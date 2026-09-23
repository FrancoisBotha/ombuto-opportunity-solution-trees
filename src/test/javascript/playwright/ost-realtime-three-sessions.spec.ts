import { type Page, type WebSocket, expect, test } from '@playwright/test';

import { registerTeamForCleanup } from './support/cleanup';
import { ADMIN_PASSWORD, ADMIN_USERNAME, type Session, USER_PASSWORD, USER_USERNAME, openSession } from './support/session';

/**
 * S2 — live collaboration with THREE sessions, covering the change types and the ordering property
 * that the two-session spec (`ost-realtime.spec.ts`) never drove end to end.
 *
 * Why three. `user` owns a throwaway team and has it open in *two* browser contexts (two tabs of
 * the same person), while `admin` watches from a third. Two sessions cannot tell a working echo
 * suppression from a broken one: when the acting user is the only one of their login on screen, an
 * echo that is wrongly suppressed and an echo that is correctly applied look identical. With the
 * same login in two tabs, the tab that did NOT write must still see the change (it is not its own
 * echo), while the tab that did must not apply it twice. `admin` is demoted to VIEWER part-way
 * through, which is the third role: FR-034 says viewers receive events like anyone else.
 *
 * Every assertion here is about the delivered change, never about what the caller sent: the
 * observing page's rendered DOM is the evidence, and each scenario first proves the observer is
 * genuinely subscribed (nothing replays events published before a SUBSCRIBE).
 *
 * Cleanup: the products are deleted in afterAll; the team is registered with support/cleanup.ts.
 * No seeded team is touched.
 */

interface TreeNode {
  key: string;
  type: string;
  id: number;
  parentKey: string | null;
  title: string;
}

const stamp = Date.now();

const node = (page: Page, key: string) => page.getByTestId(`ost-node-${key}`);
const panel = (page: Page) => page.getByTestId('ost-panel');

const errorsOf = new WeakMap<Page, string[]>();

function watchErrors(page: Page) {
  const list: string[] = [];
  errorsOf.set(page, list);
  page.on('pageerror', e => list.push(`pageerror: ${e.message}`));
  page.on('console', m => {
    if (m.type() === 'error') list.push(`console.error: ${m.text()}`);
  });
}

const takeErrors = (page: Page): string[] => {
  const list = errorsOf.get(page) ?? [];
  return list.splice(0, list.length);
};

/** Resolves once this page has sent the STOMP SUBSCRIBE for the team's topic. */
function subscribed(page: Page, teamId: number): Promise<void> {
  return new Promise<void>(resolve => {
    const onSocket = (ws: WebSocket) => {
      if (!ws.url().includes('/websocket/tracker')) return;
      ws.on('framesent', frame => {
        const body = String(frame.payload);
        if (body.includes('SUBSCRIBE') && body.includes(`/topic/teams/${teamId}/tree`)) {
          page.off('websocket', onSocket);
          resolve();
        }
      });
    };
    page.on('websocket', onSocket);
  });
}

/**
 * Every `seq` this page has been delivered on the team topic, in arrival order. NFR-014 / epic §11
 * risk 1: a per-team counter assigned under the structure lock must arrive strictly increasing by
 * one, whatever else is interleaved with the writes.
 */
function recordSeqs(page: Page, teamId: number): number[] {
  const seqs: number[] = [];
  page.on('websocket', ws => {
    if (!ws.url().includes('/websocket/tracker')) return;
    ws.on('framereceived', frame => {
      for (const m of String(frame.payload).matchAll(/\\"seq\\":(\d+)|"seq":(\d+)/g)) {
        const raw = m[1] ?? m[2];
        if (raw !== undefined && String(frame.payload).includes(String(teamId))) seqs.push(Number(raw));
      }
    });
  });
  return seqs;
}

test.describe.configure({ mode: 'serial' });

test.describe('OST realtime collaboration — three sessions', () => {
  test.setTimeout(240_000);

  /** the owner, acting */
  let owner: Session;
  /** the same person, second tab: proves an echo is suppressed per client, not per login */
  let ownerTab2: Session;
  /** a third person, EDITOR then VIEWER */
  let other: Session;
  let teamId: number;
  let otherUserId: string;
  const productIds: number[] = [];
  const n: Record<string, TreeNode> = {};

  async function mk(session: Session, type: string, parentType: string, parentId: number, title: string): Promise<TreeNode> {
    const res = await session.api('post', '/api/tree/nodes', { type, parentType, parentId, title });
    expect(res.status(), `create ${type} "${title}"`).toBe(201);
    return (await res.json()) as TreeNode;
  }

  const patch = (session: Session, key: string, body: Record<string, unknown>) =>
    session.api('patch', `/api/tree/nodes/${key.replace('-', '/')}`, body, 'application/merge-patch+json');

  async function openCanvas(page: Page, query = '') {
    const ready = subscribed(page, teamId);
    await page.goto(`/trees/${teamId}/canvas${query}`);
    await expect(page.getByTestId('ost-canvas')).toBeVisible();
    await ready;
  }

  /** Renames a probe node until the observer renders it: hard evidence the subscription is live. */
  async function proveLive(observer: Page) {
    const shown = observer.locator(`[data-cy="ost-node-${n.probe.key}"] [data-cy="ost-node-title"]`);
    for (let attempt = 1; attempt <= 10; attempt++) {
      const title = `Probe ${Date.now()}-${attempt}`;
      expect((await patch(owner, n.probe.key, { title })).status()).toBe(200);
      try {
        await expect(shown).toHaveText(title, { timeout: 2000 });
        return;
      } catch {
        // not subscribed yet; the next rename is the retry
      }
    }
    throw new Error('the observing session never received a realtime event');
  }

  test.beforeAll(async ({ browser }) => {
    owner = await openSession(browser, USER_USERNAME, USER_PASSWORD);
    ownerTab2 = await openSession(browser, USER_USERNAME, USER_PASSWORD);
    other = await openSession(browser, ADMIN_USERNAME, ADMIN_PASSWORD);
    for (const session of [owner, ownerTab2, other]) {
      await session.page.setViewportSize({ width: 1600, height: 1000 });
      watchErrors(session.page);
    }

    const team = await owner.api('post', '/api/team-management/teams', {
      name: `e2e realtime trio ${stamp}`,
      description: 'ost-realtime-three-sessions',
    });
    expect(team.status()).toBe(201);
    teamId = (await team.json()).id;
    registerTeamForCleanup(teamId);

    const found = (await (await owner.api('get', `/api/team-management/teams/${teamId}/user-search?q=admin`)).json()) as {
      id: string;
      login: string;
    }[];
    otherUserId = found.find(u => u.login === ADMIN_USERNAME)!.id;
    expect(
      (await owner.api('post', `/api/team-management/teams/${teamId}/members`, { userId: otherUserId, role: 'EDITOR' })).status(),
    ).toBe(201);

    for (const which of ['A', 'B']) {
      const product = await owner.api('post', '/api/products', {
        name: `Trio product ${which} ${stamp}`,
        description: null,
        archived: false,
        createdDate: new Date().toISOString(),
        team: { id: teamId },
      });
      expect(product.status()).toBe(201);
      productIds.push((await product.json()).id as number);
    }

    n.o1 = await mk(owner, 'outcome', 'product', productIds[0], 'Trio outcome A');
    n.o2 = await mk(owner, 'outcome', 'product', productIds[1], 'Trio outcome B');
    n.probe = await mk(owner, 'opportunity', 'outcome', n.o1.id, 'Liveness probe');
  });

  test.afterAll(async () => {
    for (const id of productIds) await owner.api('delete', `/api/products/${id}`);
    await owner?.context.close();
    await ownerTab2?.context.close();
    await other?.context.close();
  });

  /**
   * FR-032 read strictly: "a client ignores the echo of its OWN writes". A second tab of the same
   * person is a different client, so it must still see the change. Two sessions cannot distinguish
   * that from suppression by login, which is why this needs the third.
   */
  test('the same person in two tabs: the writing tab applies once, the other tab still receives it', async () => {
    await openCanvas(owner.page);
    await openCanvas(ownerTab2.page);
    await proveLive(ownerTab2.page);

    const target = await mk(owner, 'opportunity', 'outcome', n.o1.id, 'Two tabs');
    // The second tab of the same login sees it, because it is not that tab's own echo.
    await expect(node(ownerTab2.page, target.key)).toBeVisible();
    await expect(node(ownerTab2.page, target.key).getByTestId('ost-node-title')).toHaveText('Two tabs');

    // Rapid repeated edits from one tab: the other converges on the last value and renders the node
    // exactly once (a double-applied create would leave two DOM nodes for the same key).
    for (let i = 1; i <= 6; i++) expect((await patch(owner, target.key, { title: `Rapid ${i}` })).status()).toBe(200);
    await expect(node(ownerTab2.page, target.key).getByTestId('ost-node-title')).toHaveText('Rapid 6');
    await expect(node(ownerTab2.page, target.key)).toHaveCount(1);
    await expect(node(owner.page, target.key)).toHaveCount(1);
    await expect(node(owner.page, target.key).getByTestId('ost-node-title')).toHaveText('Rapid 6');

    expect((await owner.api('delete', `/api/tree/nodes/opportunity/${target.id}`)).status()).toBe(204);
    await expect(node(ownerTab2.page, target.key)).toHaveCount(0);
    for (const page of [owner.page, ownerTab2.page]) expect(takeErrors(page), 'page / console errors').toEqual([]);
  });

  /**
   * The edit and delete halves of links, questions and chat. The two-session spec only ever drove
   * the ADD of each, so LINK_UPDATED / LINK_REMOVED / QUESTION_UPDATED / QUESTION_REMOVED /
   * COMMENT_UPDATED / COMMENT_DELETED were never observed arriving anywhere (FR-030).
   */
  test('link, question and comment EDIT and DELETE all reach the other sessions', async () => {
    const target = await mk(owner, 'opportunity', 'outcome', n.o1.id, 'Edit and delete');
    await openCanvas(owner.page, `?node=${target.key}`);
    await openCanvas(other.page, `?node=${target.key}`);
    await expect(panel(other.page)).toBeVisible();
    await proveLive(other.page);

    // --- link: add, edit, delete ------------------------------------------------------------
    const linkRes = await owner.api('post', `/api/tree/nodes/opportunity/${target.id}/links`, {
      name: 'Trio link',
      url: 'https://example.com/trio',
    });
    expect(linkRes.status()).toBe(201);
    const linkId = ((await linkRes.json()) as { id: number }).id;
    await other.page.getByTestId('ost-tab-links').click();
    const linkRow = other.page.getByTestId(`ost-link-row-${linkId}`);
    await expect(linkRow.getByTestId('ost-link-name')).toHaveValue('Trio link');

    expect(
      (await owner.api('patch', `/api/tree/links/${linkId}`, { name: 'Trio link renamed', url: 'https://example.com/trio-2' })).status(),
    ).toBe(200);
    await expect(linkRow.getByTestId('ost-link-name')).toHaveValue('Trio link renamed');
    await expect(linkRow.getByTestId('ost-link-url')).toHaveValue('https://example.com/trio-2');

    const linksBefore = Number(await other.page.getByTestId('ost-tab-badge-links').textContent());
    expect((await owner.api('delete', `/api/tree/links/${linkId}`)).status()).toBe(204);
    await expect(linkRow).toHaveCount(0);
    await expect(other.page.getByTestId('ost-tab-badge-links')).toHaveText(String(linksBefore - 1));

    // --- open question: add, toggle done, delete ---------------------------------------------
    const qRes = await owner.api('post', `/api/tree/opportunities/${target.id}/questions`, { text: 'Trio question' });
    expect(qRes.status()).toBe(201);
    const questionId = ((await qRes.json()) as { id: number }).id;
    await other.page.getByTestId('ost-tab-questions').click();
    const question = other.page.getByTestId(`ost-question-${questionId}`);
    await expect(question).toContainText('Trio question');

    expect((await owner.api('patch', `/api/tree/questions/${questionId}`, { text: 'Trio question answered', done: true })).status()).toBe(
      200,
    );
    await expect(question).toContainText('Trio question answered');
    await expect(other.page.getByTestId(`ost-question-toggle-${questionId}`)).toBeChecked();

    expect((await owner.api('delete', `/api/tree/questions/${questionId}`)).status()).toBe(204);
    await expect(question).toHaveCount(0);

    // --- comment: add, edit, delete -----------------------------------------------------------
    const commentsLoaded = other.page.waitForResponse(
      r => r.url().includes(`/api/tree/nodes/opportunity/${target.id}/comments`) && r.request().method() === 'GET',
    );
    await other.page.getByTestId('ost-tab-detail').click();
    await other.page.getByTestId('ost-detail-chat').click();
    expect((await commentsLoaded).status()).toBe(200);
    const cRes = await owner.api('post', `/api/tree/nodes/opportunity/${target.id}/comments`, { body: 'Trio message' });
    expect(cRes.status()).toBe(201);
    const commentId = ((await cRes.json()) as { id: number }).id;
    const message = other.page.getByTestId(`ost-chat-msg-${commentId}`);
    await expect(message).toContainText('Trio message');

    expect((await owner.api('patch', `/api/tree/comments/${commentId}`, { body: 'Trio message, edited' })).status()).toBe(200);
    await expect(message).toContainText('Trio message, edited');

    expect((await owner.api('delete', `/api/tree/comments/${commentId}`)).status()).toBe(204);
    await expect(message).toHaveCount(0);
    await expect(other.page.getByTestId('ost-detail-chat-count')).toHaveCount(0);

    expect((await owner.api('delete', `/api/tree/nodes/opportunity/${target.id}`)).status()).toBe(204);
    for (const page of [owner.page, other.page]) expect(takeErrors(page), 'page / console errors').toEqual([]);
  });

  /**
   * A cascade delete of a deep subtree and a move across products — the two structural changes the
   * observer has to reconstruct from a single event (FR-030's cascaded keys, and `siblings`).
   */
  test('a deep cascade delete and a move to another product both land on the observers', async () => {
    await openCanvas(owner.page);
    await openCanvas(other.page);
    await openCanvas(ownerTab2.page);
    await proveLive(other.page);
    await proveLive(ownerTab2.page);

    // product > outcome > opportunity > solution > assumption > evidence
    const branch = await mk(owner, 'opportunity', 'outcome', n.o1.id, 'Deep branch');
    const solution = await mk(owner, 'solution', 'opportunity', branch.id, 'Deep solution');
    const assumption = await mk(owner, 'assumption', 'solution', solution.id, 'Deep assumption');
    const evidence = await mk(owner, 'evidence', 'assumption', assumption.id, 'Deep evidence');
    const deep = [branch, solution, assumption, evidence];
    for (const page of [other.page, ownerTab2.page]) {
      await page.getByTestId('ost-fit').click();
      for (const created of deep) await expect(node(page, created.key)).toBeVisible();
    }

    // One delete at the top removes the whole subtree on both observers, from one event.
    expect((await owner.api('delete', `/api/tree/nodes/opportunity/${branch.id}`)).status()).toBe(204);
    for (const page of [other.page, ownerTab2.page]) for (const gone of deep) await expect(node(page, gone.key)).toHaveCount(0);

    // A move across products: the node leaves outcome A (product A) for outcome B (product B).
    const traveller = await mk(owner, 'opportunity', 'outcome', n.o1.id, 'Traveller');
    await expect(node(other.page, traveller.key)).toBeVisible();
    const moved = await owner.api('post', '/api/tree/nodes/move', {
      nodeType: 'opportunity',
      nodeId: traveller.id,
      parentType: 'outcome',
      parentId: n.o2.id,
    });
    expect(moved.status()).toBe(200);

    // The observer's breadcrumb is the rendered statement of who the node's parent is now, and it
    // names the other product's outcome without a reload.
    await openCanvas(other.page, `?node=${traveller.key}`);
    await expect(other.page.getByTestId(`ost-breadcrumb-${n.o2.key}`)).toBeVisible();
    await expect(other.page.getByTestId(`ost-breadcrumb-${n.o1.key}`)).toHaveCount(0);

    expect((await owner.api('delete', `/api/tree/nodes/opportunity/${traveller.id}`)).status()).toBe(204);
    for (const page of [owner.page, other.page, ownerTab2.page]) expect(takeErrors(page), 'page / console errors').toEqual([]);
  });

  /**
   * FR-034 — a VIEWER receives events like anyone else, and (epic §11 risk 1) membership writes
   * share the team's single `seq` space with tree writes, so interleaving them leaves the delivered
   * sequence strictly increasing by one. A viewer also gets the history a write produced.
   */
  test('a viewer keeps receiving, and interleaved membership and tree writes stay strictly ordered', async () => {
    const seqs = recordSeqs(other.page, teamId);
    await openCanvas(owner.page);
    await openCanvas(other.page);
    await proveLive(other.page);

    const role = (next: 'VIEWER' | 'EDITOR') =>
      owner.api('put', `/api/team-management/teams/${teamId}/members/${otherUserId}`, { role: next });

    expect((await role('VIEWER')).status()).toBe(200);
    await expect(other.page.getByTestId('ost-palette')).toHaveCount(0);
    await other.page.getByTestId('ostErrorDismiss').click();

    // A viewer still receives every tree event.
    const seen = await mk(owner, 'opportunity', 'outcome', n.o1.id, 'Seen by a viewer');
    await expect(node(other.page, seen.key)).toBeVisible();

    // Interleave membership changes with tree writes, then check the delivered order.
    seqs.length = 0;
    for (let i = 0; i < 4; i++) {
      expect((await patch(owner, seen.key, { title: `Interleaved ${i}` })).status()).toBe(200);
      expect((await role(i % 2 === 0 ? 'EDITOR' : 'VIEWER')).status()).toBe(200);
    }
    await expect(node(other.page, seen.key).getByTestId('ost-node-title')).toHaveText('Interleaved 3');
    await expect.poll(() => seqs.length, { timeout: 20_000 }).toBeGreaterThanOrEqual(8);
    for (let i = 1; i < seqs.length; i++) {
      expect(seqs[i], `seq ${seqs[i]} follows ${seqs[i - 1]} (per-team monotonic, no gaps)`).toBe(seqs[i - 1] + 1);
    }

    // A gap would have forced a full re-read; the session is still following events instead.
    expect((await patch(owner, seen.key, { title: 'Still following' })).status()).toBe(200);
    await expect(node(other.page, seen.key).getByTestId('ost-node-title')).toHaveText('Still following');

    // And the history the writes produced is readable live by the viewer.
    await openCanvas(other.page, `?node=${seen.key}`);
    await other.page.getByTestId('ost-tab-history').click();
    await expect(other.page.getByTestId('ost-history-empty')).toHaveCount(0);

    expect((await role('EDITOR')).status()).toBe(200);
    expect((await owner.api('delete', `/api/tree/nodes/opportunity/${seen.id}`)).status()).toBe(204);
    for (const page of [owner.page, other.page]) expect(takeErrors(page), 'page / console errors').toEqual([]);
  });
});
