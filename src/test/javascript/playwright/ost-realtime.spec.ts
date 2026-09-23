import { type Page, type WebSocket, expect, test } from '@playwright/test';

import { registerTeamForCleanup } from './support/cleanup';
import { ADMIN_PASSWORD, ADMIN_USERNAME, type Session, USER_PASSWORD, USER_USERNAME, openSession } from './support/session';

/**
 * RTC-007: live collaboration proved with two genuine sessions.
 *
 * `user` owns a throwaway team and `admin` joins it as an EDITOR (support/session.ts gives each
 * their own browser context, so the two STOMP connections are genuinely separate). One session
 * commits a change through the API with its own cookies; the OTHER session's rendered canvas or
 * panel has to follow without a reload (FR-029..FR-032), a field being typed in is never
 * overwritten (FR-033), a dropped connection recovers with exactly one full tree read (FR-034/035),
 * delivery stays under a second (NFR-010) and a burst leaves the receiving session responsive
 * (NFR-014).
 *
 * Cleanup: the products are deleted in afterAll; the team is registered with support/cleanup.ts and
 * removed by the cleanup teardown project once every spec has finished. No seeded team is touched.
 */

interface TreeNode {
  key: string;
  type: string;
  id: number;
  parentKey: string | null;
  title: string;
}

const stamp = Date.now();

/** NFR-010: a committed change must be on the other screen within a second. */
const LATENCY_BUDGET_MS = 1000;
/** NFR-014: after a burst the receiving session must still answer an interaction promptly. */
const RESPONSIVE_BUDGET_MS = 3000;
/** Size of the tree built for the burst scenario (nodes created, then cascaded away in one delete). */
const BURST_OPPORTUNITIES = 40;

const errorsOf = new WeakMap<Page, string[]>();

function watchErrors(page: Page) {
  const list: string[] = [];
  errorsOf.set(page, list);
  page.on('pageerror', e => list.push(`pageerror: ${e.message}`));
  page.on('console', m => {
    if (m.type() === 'error') list.push(`console.error: ${m.text()}`);
  });
}

/** Drains the recorded page / console errors, dropping the ones a scenario expects. */
function takeErrors(page: Page, ignore: RegExp[] = []): string[] {
  const list = errorsOf.get(page) ?? [];
  const all = list.splice(0, list.length);
  return all.filter(entry => !ignore.some(pattern => pattern.test(entry)));
}

const node = (page: Page, key: string) => page.getByTestId(`ost-node-${key}`);
const panel = (page: Page) => page.getByTestId('ost-panel');

/**
 * Resolves once this page has sent the STOMP SUBSCRIBE for the team's topic — the point from which
 * it is guaranteed to receive events. Nothing replays what was published before that, so every
 * navigation waits for it instead of sleeping.
 */
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
 * A node's laid-out box in canvas coordinates (its Vue Flow wrapper's transform and size), or null
 * when it is not rendered. The canvas only renders what is in view, so a caller that needs a node
 * off to the side fits the view first.
 */
function flowBox(page: Page, key: string) {
  return page.evaluate(k => {
    const el = document.querySelector<HTMLElement>(`.vue-flow__node[data-id="${CSS.escape(k)}"]`);
    if (!el) return null;
    const m = new DOMMatrixReadOnly(getComputedStyle(el).transform);
    return { x: Math.round(m.m41), y: Math.round(m.m42), width: el.offsetWidth, height: el.offsetHeight };
  }, key);
}

/**
 * Where the edge into `key` starts — its parent's bottom centre, in the same canvas coordinates as
 * {@link flowBox}. This is how the drawn tree says who a node's parent is.
 */
function edgeStart(page: Page, key: string) {
  return page.evaluate(k => {
    const path = document.querySelector<SVGPathElement>(`[data-cy="ost-edge-${k}"]`);
    if (!path) return null;
    const point = path.getPointAtLength(0);
    return { x: Math.round(point.x), y: Math.round(point.y) };
  }, key);
}

/** Waits until the viewport and every node's (possibly mid-transition) transform stop changing. */
async function settle(page: Page) {
  let last = '';
  await expect
    .poll(
      async () => {
        const now = await page.evaluate(() => {
          const pane = document.querySelector<HTMLElement>('.vue-flow__transformationpane');
          const nodes = Array.from(document.querySelectorAll<HTMLElement>('.vue-flow__node'), n => getComputedStyle(n).transform);
          return `${pane ? getComputedStyle(pane).transform : ''}|${nodes.join('|')}`;
        });
        const still = now === last;
        last = now;
        return still;
      },
      { intervals: [120], timeout: 30_000 },
    )
    .toBe(true);
}

test.describe.configure({ mode: 'serial' });

test.describe('OST realtime collaboration — two sessions', () => {
  test.setTimeout(240_000);

  let user: Session;
  let admin: Session;
  let teamId: number;
  /** the admin's user id in the throwaway team (RTC-006: the role is changed through it) */
  let adminUserId: string;
  /** the acting user's initials, as the team meta reports them (the pulse badge shows exactly these) */
  let userInitials: string;
  const productIds: number[] = [];
  const k: Record<string, string> = {};
  const n: Record<string, TreeNode> = {};
  /** measurements logged at the end of the run, so a CI log shows the numbers behind NFR-010/014 */
  const measured: string[] = [];

  async function mk(session: Session, type: string, parentType: string, parentId: number, title: string): Promise<TreeNode> {
    const res = await session.api('post', '/api/tree/nodes', { type, parentType, parentId, title });
    expect(res.status(), `create ${type} "${title}"`).toBe(201);
    return (await res.json()) as TreeNode;
  }

  const patch = (session: Session, key: string, body: Record<string, unknown>) =>
    session.api('patch', `/api/tree/nodes/${key.replace('-', '/')}`, body, 'application/merge-patch+json');

  /**
   * Opens the canvas (optionally on a node) and waits until this page has sent its SUBSCRIBE.
   * `gate` is turned off where the socket is proxied by routeWebSocket, which the page's own
   * websocket events do not describe.
   */
  async function openCanvas(page: Page, query = '', gate = true) {
    const ready = subscribed(page, teamId);
    await page.goto(`/trees/${teamId}/canvas${query}`);
    await expect(page.getByTestId('ost-canvas')).toBeVisible();
    if (gate) await ready;
  }

  /**
   * Proves the observing page really receives events before a scenario relies on it: `user` renames
   * a probe node until the observer renders the new title. Nothing replays events published before
   * a subscription is registered, so a round trip is the only hard evidence that it is.
   */
  async function proveLive(observer: Page) {
    const shown = observer.locator(`[data-cy="ost-node-${n.probe.key}"] [data-cy="ost-node-title"]`);
    for (let attempt = 1; attempt <= 10; attempt++) {
      const title = `Probe ${Date.now()}-${attempt}`;
      expect((await patch(user, n.probe.key, { title })).status()).toBe(200);
      try {
        await expect(shown).toHaveText(title, { timeout: 2000 });
        return;
      } catch {
        // The subscription was not registered yet; the next rename is the retry.
      }
    }
    throw new Error('the observing session never received a realtime event');
  }

  test.beforeAll(async ({ browser }) => {
    user = await openSession(browser, USER_USERNAME, USER_PASSWORD);
    admin = await openSession(browser, ADMIN_USERNAME, ADMIN_PASSWORD);
    for (const session of [user, admin]) {
      await session.page.setViewportSize({ width: 1600, height: 1000 });
      watchErrors(session.page);
    }

    const team = await user.api('post', '/api/team-management/teams', {
      name: `e2e realtime ${stamp}`,
      description: 'ost-realtime',
    });
    expect(team.status()).toBe(201);
    teamId = (await team.json()).id;
    registerTeamForCleanup(teamId);

    const found = (await (await user.api('get', `/api/team-management/teams/${teamId}/user-search?q=admin`)).json()) as {
      id: string;
      login: string;
    }[];
    adminUserId = found.find(u => u.login === ADMIN_USERNAME)!.id;
    expect((await user.api('post', `/api/team-management/teams/${teamId}/members`, { userId: adminUserId, role: 'EDITOR' })).status()).toBe(
      201,
    );

    const product = await user.api('post', '/api/products', {
      name: `Realtime product ${stamp}`,
      description: null,
      archived: false,
      createdDate: new Date().toISOString(),
      team: { id: teamId },
    });
    expect(product.status()).toBe(201);
    const productId = (await product.json()).id as number;
    productIds.push(productId);
    k.product = `product-${productId}`;

    n.o1 = await mk(user, 'outcome', 'product', productId, 'Realtime outcome A');
    n.o2 = await mk(user, 'outcome', 'product', productId, 'Realtime outcome B');
    // Renamed by proveLive() to check that a session really is receiving events.
    n.probe = await mk(user, 'opportunity', 'outcome', n.o2.id, 'Liveness probe');
    for (const [name, value] of Object.entries(n)) k[name] = value.key;

    const meta = (await (await user.api('get', `/api/teams/${teamId}/tree`)).json()) as {
      members: { login: string; initials: string }[];
    };
    userInitials = meta.members.find(m => m.login === USER_USERNAME)!.initials;
  });

  test.afterAll(async () => {
    for (const id of productIds) {
      const res = await user.api('delete', `/api/products/${id}`);
      expect(res.status(), 'product cleanup').toBe(204);
    }
    await user?.context.close();
    await admin?.context.close();
    if (measured.length) console.log(`RTC-007 measurements:\n  ${measured.join('\n  ')}`);
  });

  test('every kind of change reaches the other session without a reload', async () => {
    const acting = user.page;
    const observing = admin.page;
    await openCanvas(acting);
    await openCanvas(observing);
    await proveLive(observing);

    // --- create ---------------------------------------------------------------------------------
    const op = await mk(user, 'opportunity', 'outcome', n.o1.id, 'Live created opportunity');
    await expect(node(observing, op.key)).toBeVisible();
    await expect(node(observing, op.key).getByTestId('ost-node-title')).toHaveText('Live created opportunity');

    // The observer now watches this node through the panel as well.
    await openCanvas(observing, `?node=${op.key}`);
    await expect(panel(observing)).toBeVisible();
    await expect(observing.getByTestId('ost-panel-title')).toHaveValue('Live created opportunity');

    // --- patch (title / status / value) ---------------------------------------------------------
    expect((await patch(user, op.key, { title: 'Live renamed opportunity', status: 'validated', valueRating: 5 })).status()).toBe(200);
    await expect(observing.getByTestId('ost-panel-title')).toHaveValue('Live renamed opportunity');
    await expect(observing.getByTestId('ost-status-validated')).toHaveAttribute('aria-pressed', 'true');
    await expect(observing.getByTestId('ost-value-label')).toHaveText('Outsized');
    await expect(node(observing, op.key).getByTestId('ost-node-title')).toHaveText('Live renamed opportunity');
    await expect(node(observing, op.key).getByTestId('ost-node-status')).toHaveText('validated');

    // --- add a link -----------------------------------------------------------------------------
    // A new node starts with the default links (rules.ts defaultLinks), so the badge counts up from there.
    const linksBefore = Number(await observing.getByTestId('ost-tab-badge-links').textContent());
    const linkRes = await user.api('post', `/api/tree/nodes/opportunity/${op.id}/links`, {
      name: 'Live link',
      url: 'https://example.com/live',
    });
    expect(linkRes.status()).toBe(201);
    const link = (await linkRes.json()) as { id: number };
    // The tab badge follows the store, so the change is visible before the tab is even opened.
    await expect(observing.getByTestId('ost-tab-badge-links')).toHaveText(String(linksBefore + 1));
    await observing.getByTestId('ost-tab-links').click();
    await expect(observing.getByTestId(`ost-link-row-${link.id}`).getByTestId('ost-link-name')).toHaveValue('Live link');

    // --- add an open question -------------------------------------------------------------------
    const questionRes = await user.api('post', `/api/tree/opportunities/${op.id}/questions`, { text: 'Does this arrive live?' });
    expect(questionRes.status()).toBe(201);
    const question = (await questionRes.json()) as { id: number };
    await expect(observing.getByTestId('ost-tab-badge-questions')).toHaveText('1');
    await observing.getByTestId('ost-tab-questions').click();
    await expect(observing.getByTestId(`ost-question-${question.id}`)).toContainText('Does this arrive live?');

    // --- post a chat message --------------------------------------------------------------------
    const commentsLoaded = observing.waitForResponse(
      r => r.url().includes(`/api/tree/nodes/opportunity/${op.id}/comments`) && r.request().method() === 'GET',
    );
    await observing.getByTestId('ost-tab-detail').click();
    await observing.getByTestId('ost-detail-chat').click();
    expect((await commentsLoaded).status()).toBe(200);
    const commentRes = await user.api('post', `/api/tree/nodes/opportunity/${op.id}/comments`, { body: 'Live chat message' });
    expect(commentRes.status()).toBe(201);
    const comment = (await commentRes.json()) as { id: number };
    const bubble = observing.getByTestId(`ost-chat-msg-${comment.id}`);
    await expect(bubble).toContainText('Live chat message');
    await expect(observing.getByTestId('ost-detail-chat-count')).toHaveText('1');
    // CHAT-001: a message live-delivered from `user` must be attributed to `user` on the
    // observing `admin` session — not styled as the viewer's own, no Edit/Delete offered,
    // author initials shown on the run-start bubble.
    await expect(bubble).toHaveAttribute('data-mine', 'false');
    const item = bubble.locator('..');
    await expect(item).not.toHaveClass(/\bis-mine\b/);
    await expect(item.getByTestId('ost-chat-who')).toHaveText(userInitials);
    await expect(observing.getByTestId(`ost-chat-edit-${comment.id}`)).toHaveCount(0);
    await expect(observing.getByTestId(`ost-chat-delete-${comment.id}`)).toHaveCount(0);

    await observing.getByTestId('ost-chat-modal-close').click();

    // --- move (re-parent) -----------------------------------------------------------------------
    // Fit first: the canvas only renders what is in view (only-render-visible-elements), so the
    // measurements below need every node and edge on screen. Fitting is a local view action.
    await observing.getByTestId('ost-fit').click();
    await settle(observing);
    const before = await flowBox(observing, op.key);
    expect(before, 'the node is laid out before the move').not.toBeNull();
    const outcomeA = (await flowBox(observing, k.o1))!;
    expect(await edgeStart(observing, op.key), 'the edge into the node starts at outcome A').toEqual({
      x: Math.round(outcomeA.x + outcomeA.width / 2),
      y: Math.round(outcomeA.y + outcomeA.height),
    });

    const moved = await user.api('post', '/api/tree/nodes/move', {
      nodeType: 'opportunity',
      nodeId: op.id,
      parentType: 'outcome',
      parentId: n.o2.id,
    });
    expect(moved.status()).toBe(200);
    await expect(observing.getByTestId(`ost-breadcrumb-${k.o2}`)).toBeVisible();
    await expect(observing.getByTestId(`ost-breadcrumb-${k.o1}`)).toHaveCount(0);

    // The canvas re-lays the node out and redraws the edge from outcome B, live.
    await expect.poll(() => flowBox(observing, op.key), { timeout: 20_000 }).not.toEqual(before);
    await observing.getByTestId('ost-fit').click();
    await settle(observing);
    await expect(node(observing, op.key)).toBeVisible();
    const outcomeB = (await flowBox(observing, k.o2))!;
    await expect
      .poll(() => edgeStart(observing, op.key), { timeout: 20_000 })
      .toEqual({ x: Math.round(outcomeB.x + outcomeB.width / 2), y: Math.round(outcomeB.y + outcomeB.height) });

    // --- delete ---------------------------------------------------------------------------------
    expect((await user.api('delete', `/api/tree/nodes/opportunity/${op.id}`)).status()).toBe(204);
    await expect(node(observing, op.key)).toHaveCount(0);

    for (const page of [acting, observing]) expect(takeErrors(page), 'page / console errors').toEqual([]);
  });

  test('concurrent edits of different fields both land, and a field being typed in is not overwritten', async () => {
    const target = await mk(user, 'opportunity', 'outcome', n.o1.id, 'Conflict node');
    await openCanvas(user.page, `?node=${target.key}`);
    await openCanvas(admin.page, `?node=${target.key}`);
    await expect(panel(admin.page)).toBeVisible();
    await proveLive(admin.page);

    // --- different fields, committed at the same moment ------------------------------------------
    const [byUser, byAdmin] = await Promise.all([
      patch(user, target.key, { title: 'Title by user' }),
      patch(admin, target.key, { status: 'validated' }),
    ]);
    expect(byUser.status(), 'user patch of the title').toBe(200);
    expect(byAdmin.status(), 'admin patch of the status').toBe(200);

    // Both sessions converge on the last committed value of each field.
    for (const page of [user.page, admin.page]) {
      await expect(page.getByTestId('ost-panel-title')).toHaveValue('Title by user');
      await expect(page.getByTestId('ost-status-validated')).toHaveAttribute('aria-pressed', 'true');
    }

    // --- FR-033: what session B is typing survives a remote patch of the same node ---------------
    const typed = 'Admin is still typing this';
    const titleField = admin.page.getByTestId('ost-panel-title');
    await titleField.focus();
    await titleField.fill('');
    await titleField.pressSequentially(typed);
    await expect(titleField).toHaveValue(typed);

    expect((await patch(user, target.key, { title: 'Remote overwrite attempt', valueRating: 4 })).status()).toBe(200);

    // The other field lands, which proves the event was applied at all …
    await expect(admin.page.getByTestId('ost-value-label')).toHaveText('Strong');
    // … while the field under the cursor keeps exactly what was typed, and the dropped remote value
    // is surfaced instead of being applied.
    await expect(titleField).toHaveValue(typed);
    await expect(admin.page.getByTestId('ost-panel-title-dropped')).toContainText('Remote overwrite attempt');

    // Escape discards the draft so nothing of this scenario is committed on blur. It reverts to the
    // value this client still holds ('Title by user'): the remote title was dropped rather than
    // applied, so until the next full tree read this session's copy of the field is knowingly
    // behind the server. That is FR-033's trade-off, and the dropped-value hint above is what tells
    // the user about it.
    await titleField.press('Escape');
    await expect(titleField).toHaveValue('Title by user');
    expect(
      ((await (await user.api('get', `/api/teams/${teamId}/tree`)).json()) as { nodes: TreeNode[] }).nodes.find(x => x.key === target.key)
        ?.title,
    ).toBe('Remote overwrite attempt');

    expect((await user.api('delete', `/api/tree/nodes/opportunity/${target.id}`)).status()).toBe(204);
    for (const page of [user.page, admin.page]) expect(takeErrors(page), 'page / console errors').toEqual([]);
  });

  test('a node changed by the other member pulses with their initials, without moving the view or focus (FR-036)', async () => {
    const target = await mk(user, 'opportunity', 'outcome', n.o1.id, 'Pulse target');
    await openCanvas(user.page);
    await openCanvas(admin.page);
    await proveLive(admin.page);
    await admin.page.getByTestId('ost-fit').click();
    await settle(admin.page);
    await expect(node(admin.page, target.key)).toBeVisible();

    const badge = admin.page.getByTestId(`ost-node-pulse-${target.key}`);
    const viewport = () => admin.page.evaluate(() => getComputedStyle(document.querySelector('.vue-flow__transformationpane')!).transform);
    const focused = () => admin.page.evaluate(() => document.activeElement?.getAttribute('data-cy') ?? document.activeElement?.tagName);

    // Focus something of the observer's own: a remote change must not take it away.
    await admin.page.getByTestId('ost-search').focus();
    const viewportBefore = await viewport();
    expect(await focused()).toBe('ost-search');

    // The pulse lasts ~1.6 s, so a slow moment could expire it before the first poll — each retry
    // is simply another remote change to the same node.
    for (let attempt = 1; ; attempt++) {
      expect((await patch(user, target.key, { title: `Pulse ${attempt}` })).status()).toBe(200);
      try {
        await expect(badge).toHaveText(userInitials, { timeout: 2000 });
        break;
      } catch (error) {
        if (attempt === 5) throw error;
      }
    }
    await expect(badge).toHaveAttribute('title', new RegExp('just changed this'));
    await expect(node(admin.page, target.key)).toHaveClass(/is-pulsing/);

    // Neither the viewport nor the focus moved while it pulsed …
    expect(await viewport(), 'the viewport did not move').toBe(viewportBefore);
    expect(await focused(), 'focus was not stolen').toBe('ost-search');
    // … and the pulse ends by itself, leaving the node exactly as it was.
    await expect(badge).toHaveCount(0, { timeout: 10_000 });
    await expect(node(admin.page, target.key)).toBeVisible();
    expect(await viewport(), 'the viewport did not move when the pulse ended').toBe(viewportBefore);

    expect((await user.api('delete', `/api/tree/nodes/opportunity/${target.id}`)).status()).toBe(204);
    for (const page of [user.page, admin.page]) expect(takeErrors(page), 'page / console errors').toEqual([]);
  });

  test('a dropped connection is restored with exactly one full tree reload', async () => {
    const treeReads: string[] = [];
    const onRequest = (request: { method(): string; url(): string }) => {
      if (request.method() === 'GET' && request.url().includes(`/api/teams/${teamId}/tree`)) treeReads.push(request.url());
    };
    admin.page.on('request', onRequest);

    // The transport is cut by proxying the tracker socket and refusing it while `blocked` is set.
    // context.setOffline() is not enough here: Chromium leaves an established websocket to localhost
    // open, so the client never notices. Refusing the SockJS HTTP endpoints as well stops it from
    // falling back to an XHR transport, which would be a different scenario.
    let blocked = false;
    let current: { close(options?: { code?: number; reason?: string }): Promise<void> } | null = null;
    await admin.page.routeWebSocket(/websocket\/tracker/, ws => {
      if (blocked) {
        void ws.close({ code: 1006 });
        return;
      }
      ws.connectToServer();
      current = ws;
    });
    await admin.page.route('**/websocket/tracker/**', route => (blocked ? route.abort() : route.fallback()));

    try {
      await openCanvas(user.page);
      await openCanvas(admin.page, '', false);
      await proveLive(admin.page);
      treeReads.length = 0;

      // FR-035: the indicator says the connection is up before it is cut …
      const indicator = admin.page.getByTestId('ost-connection');
      await expect(indicator).toHaveAttribute('data-state', 'live');
      await expect(indicator).toHaveAccessibleName('Connection: Live');

      blocked = true;
      await current!.close({ code: 1006, reason: 'e2e drop' });

      // … and says so as soon as it is gone (reconnecting, or offline once it gives up).
      await expect(indicator).toHaveAttribute('data-state', /reconnecting|offline/, { timeout: 20_000 });
      await expect(indicator).toHaveAccessibleName(/Connection: (Reconnecting|Offline)/);

      // A change the dropped session cannot possibly have received.
      const missed = await mk(user, 'opportunity', 'outcome', n.o2.id, 'Created while offline');
      await expect(node(admin.page, missed.key)).toHaveCount(0);
      expect(treeReads.length, 'no tree read while the connection is down').toBe(0);

      blocked = false;

      // The reconnect is followed by one authoritative tree read, which brings the missed node in.
      await expect(node(admin.page, missed.key)).toBeVisible({ timeout: 60_000 });
      expect(treeReads.length, 'exactly one full tree reload after the reconnect').toBe(1);
      await expect(node(admin.page, missed.key).getByTestId('ost-node-title')).toHaveText('Created while offline');
      await expect(node(admin.page, k.o1)).toBeVisible();
      await expect(node(admin.page, k.o2)).toBeVisible();

      // The session is live again, and still only one reload has happened.
      const afterwards = await mk(user, 'opportunity', 'outcome', n.o2.id, 'Created after the reconnect');
      await expect(node(admin.page, afterwards.key)).toBeVisible();
      expect(treeReads.length, 'no second reload').toBe(1);

      // RTC-006 / FR-035: and back to live once the transport is restored.
      await expect(indicator).toHaveAttribute('data-state', 'live', { timeout: 60_000 });
      await expect(indicator).toHaveAccessibleName('Connection: Live');

      for (const id of [missed.id, afterwards.id])
        expect((await user.api('delete', `/api/tree/nodes/opportunity/${id}`)).status()).toBe(204);
    } finally {
      blocked = false;
      admin.page.off('request', onRequest);
      await admin.page.unrouteAll({ behavior: 'ignoreErrors' });
    }

    // Losing the transport makes the browser log the refused requests; nothing else is tolerated.
    const offlineNoise = [
      /ERR_INTERNET_DISCONNECTED/,
      /ERR_FAILED/,
      /ERR_NETWORK_CHANGED/,
      /Failed to load resource/,
      /websocket/i,
      /Network Error/,
    ];
    expect(takeErrors(admin.page, offlineNoise), 'page / console errors').toEqual([]);
    expect(takeErrors(user.page), 'page / console errors').toEqual([]);
  });

  test('a committed change is on the other screen within a second (NFR-010)', async () => {
    await openCanvas(user.page);
    await openCanvas(admin.page);
    await proveLive(admin.page);

    const created = await user.api('post', '/api/tree/nodes', {
      type: 'opportunity',
      parentType: 'outcome',
      parentId: n.o1.id,
      title: 'Latency probe',
    });
    expect(created.status()).toBe(201);
    const probe = (await created.json()) as TreeNode;
    const createdAt = Date.now();
    await admin.page.waitForSelector(`[data-cy="ost-node-${probe.key}"]`, { state: 'visible', timeout: LATENCY_BUDGET_MS * 10 });
    const createLatency = Date.now() - createdAt;

    expect((await patch(user, probe.key, { title: 'Latency probe renamed' })).status()).toBe(200);
    const patchedAt = Date.now();
    await admin.page.waitForFunction(
      key =>
        document.querySelector(`[data-cy="ost-node-${key}"] [data-cy="ost-node-title"]`)?.textContent?.trim() === 'Latency probe renamed',
      probe.key,
      { timeout: LATENCY_BUDGET_MS * 10 },
    );
    const patchLatency = Date.now() - patchedAt;

    expect((await user.api('delete', `/api/tree/nodes/opportunity/${probe.id}`)).status()).toBe(204);
    const deletedAt = Date.now();
    await admin.page.waitForSelector(`[data-cy="ost-node-${probe.key}"]`, { state: 'detached', timeout: LATENCY_BUDGET_MS * 10 });
    const deleteLatency = Date.now() - deletedAt;

    measured.push(
      `NFR-010 create ${createLatency} ms, patch ${patchLatency} ms, delete ${deleteLatency} ms (budget ${LATENCY_BUDGET_MS} ms)`,
    );
    console.log(`NFR-010 latency — create ${createLatency} ms, patch ${patchLatency} ms, delete ${deleteLatency} ms`);
    expect(createLatency, 'create latency').toBeLessThan(LATENCY_BUDGET_MS);
    expect(patchLatency, 'patch latency').toBeLessThan(LATENCY_BUDGET_MS);
    expect(deleteLatency, 'delete latency').toBeLessThan(LATENCY_BUDGET_MS);

    for (const page of [user.page, admin.page]) expect(takeErrors(page), 'page / console errors').toEqual([]);
  });

  test('a burst on a large tree leaves the receiving session responsive (NFR-014)', async () => {
    await openCanvas(user.page);
    await openCanvas(admin.page);
    await proveLive(admin.page);

    // The canvas only renders what is in view, so the whole tree is counted through the toolbar's
    // type chips instead — a rendered number that follows every applied event.
    const chipCount = async (type: string) =>
      Number(await admin.page.getByTestId(`ost-filter-${type}`).locator('.ost-toolbar__chip-count').textContent());
    const baseline = { opportunity: await chipCount('opportunity'), solution: await chipCount('solution') };

    // Build a big subtree while the other session is watching: the creates are a burst of their own.
    const bulk = await mk(user, 'outcome', 'product', productIds[0], 'Burst outcome');
    const builtAt = Date.now();
    for (let i = 0; i < BURST_OPPORTUNITIES; i++) {
      const opportunity = await mk(user, 'opportunity', 'outcome', bulk.id, `Burst opportunity ${i}`);
      await mk(user, 'solution', 'opportunity', opportunity.id, `Burst solution ${i}`);
    }
    const built = BURST_OPPORTUNITIES * 2 + 1;
    await expect.poll(() => chipCount('opportunity'), { timeout: 60_000 }).toBe(baseline.opportunity + BURST_OPPORTUNITIES);
    await expect.poll(() => chipCount('solution'), { timeout: 60_000 }).toBe(baseline.solution + BURST_OPPORTUNITIES);
    const buildMs = Date.now() - builtAt;
    await settle(admin.page);

    // The burst proper: one delete that cascades the whole subtree away.
    const deleted = await user.api('delete', `/api/tree/nodes/outcome/${bulk.id}`);
    expect(deleted.status()).toBe(204);
    const deletedAt = Date.now();
    await expect.poll(() => chipCount('opportunity'), { timeout: 60_000 }).toBe(baseline.opportunity);
    await expect.poll(() => chipCount('solution'), { timeout: 60_000 }).toBe(baseline.solution);
    const applyMs = Date.now() - deletedAt;
    await settle(admin.page);
    const settledMs = Date.now() - deletedAt;

    // Still responsive: fitting the view and selecting a node opens the panel on it.
    const interactedAt = Date.now();
    await admin.page.getByTestId('ost-fit').click({ timeout: RESPONSIVE_BUDGET_MS });
    await node(admin.page, k.o1).click({ timeout: RESPONSIVE_BUDGET_MS });
    await expect(panel(admin.page)).toBeVisible({ timeout: RESPONSIVE_BUDGET_MS });
    await expect(admin.page.getByTestId('ost-panel-title')).toHaveValue('Realtime outcome A', { timeout: RESPONSIVE_BUDGET_MS });
    const interactionMs = Date.now() - interactedAt;

    measured.push(
      `NFR-014 ${built} nodes built in ${buildMs} ms (includes the REST calls), cascade delete applied in ${applyMs} ms, settled in ${settledMs} ms, interaction ${interactionMs} ms`,
    );
    console.log(
      `NFR-014 burst — ${built} nodes, delete applied in ${applyMs} ms, settled in ${settledMs} ms, interaction ${interactionMs} ms`,
    );
    expect(interactionMs, 'interaction after the burst').toBeLessThan(RESPONSIVE_BUDGET_MS);

    for (const page of [user.page, admin.page]) expect(takeErrors(page), 'page / console errors').toEqual([]);
  });

  test('a role change updates the other member’s edit affordances live (FR-037)', async () => {
    await openCanvas(user.page);
    await openCanvas(admin.page);
    await proveLive(admin.page);
    await admin.page.getByTestId('ost-fit').click();
    await settle(admin.page);

    // The admin is an EDITOR: the node palette and the per-node + are there.
    const palette = admin.page.getByTestId('ost-palette');
    await expect(palette).toHaveCount(1);
    await expect(admin.page.getByTestId(`ost-node-add-${k.o1}`)).toBeVisible();

    const role = (next: 'VIEWER' | 'EDITOR') =>
      user.api('put', `/api/team-management/teams/${teamId}/members/${adminUserId}`, { role: next });

    // "Without a reload" is asserted literally: no tree read may happen while the role changes.
    let treeReads = 0;
    const onRequest = (request: { method(): string; url(): string }) => {
      if (request.method() === 'GET' && request.url().includes(`/api/teams/${teamId}/tree`)) treeReads++;
    };
    admin.page.on('request', onRequest);

    // Demoted to viewer: the affordances go, and the session is told why.
    expect((await role('VIEWER')).status()).toBe(200);
    await expect(palette).toHaveCount(0);
    await expect(admin.page.getByTestId(`ost-node-add-${k.o1}`)).toHaveCount(0);
    await expect(admin.page.getByTestId('ostError')).toContainText('viewer');
    await admin.page.getByTestId('ostErrorDismiss').click();

    // Promoted back: they come back, still without a reload.
    expect((await role('EDITOR')).status()).toBe(200);
    await expect(palette).toHaveCount(1);
    await expect(admin.page.getByTestId(`ost-node-add-${k.o1}`)).toBeVisible();

    admin.page.off('request', onRequest);
    expect(treeReads, 'the role change was applied live, with no tree re-read').toBe(0);

    for (const page of [user.page, admin.page]) expect(takeErrors(page), 'page / console errors').toEqual([]);
  });
});
