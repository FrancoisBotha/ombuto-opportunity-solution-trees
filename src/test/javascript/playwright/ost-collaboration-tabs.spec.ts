import { type Locator, type Page, expect, test } from '@playwright/test';

import { registerTeamForCleanup } from './support/cleanup';
import { ADMIN_PASSWORD, ADMIN_USERNAME, type Session, USER_PASSWORD, USER_USERNAME, openSession } from './support/session';

/**
 * OST step 11: the Chat, Open Qs and History tabs and the chat modal, driven by two people at once.
 * `user` owns a throwaway team and `admin` joins it as an EDITOR (later demoted to VIEWER): both
 * post, own bubbles sit on the right, only own messages can be edited / deleted, edits are marked,
 * the node chip and the tab badge follow the count, the modal opens from the chip; open questions
 * add / tick / remove with the summary and badge; the history lists the recorded events newest
 * first and nothing for title / notes edits; a viewer reads the chat but cannot post; the
 * jump-to-latest button in a long thread.
 *
 * Cleanup: the product (and its subtree) is deleted in afterAll; the team is registered with
 * support/cleanup.ts and deleted as admin once every spec has finished (deleting it mid-run breaks
 * other workers' requests). No seeded team is touched.
 */

interface TreeNode {
  key: string;
  id: number;
  title: string;
}

interface Comment {
  id: number;
  body: string;
  mine: boolean;
}

const stamp = Date.now();
const errorsOf = new WeakMap<Page, string[]>();

function watchErrors(page: Page) {
  const list: string[] = [];
  errorsOf.set(page, list);
  page.on('pageerror', e => list.push(`pageerror: ${e.message}`));
  page.on('console', m => {
    if (m.type() === 'error') list.push(`console.error: ${m.text()}`);
  });
}

function takeErrors(page: Page) {
  const list = errorsOf.get(page) ?? [];
  return list.splice(0, list.length);
}

const node = (page: Page, key: string) => page.getByTestId(`ost-node-${key}`);
const chip = (page: Page, key: string) => page.getByTestId(`ost-node-chat-${key}`);
const panel = (page: Page) => page.getByTestId('ost-panel');
const input = (scope: Page | Locator) => scope.getByTestId('ost-chat-input');

/** Waits for the API call `action` triggers and checks its status. */
async function api(page: Page, method: string, path: string | RegExp, status: number, action: () => Promise<unknown>) {
  const response = page.waitForResponse(
    r => r.request().method() === method && (typeof path === 'string' ? r.url().includes(path) : path.test(r.url())),
  );
  await action();
  expect((await response).status(), `${method} ${path}`).toBe(status);
  return response;
}

/** Horizontal placement of a bubble inside its scroll box: 'right' or 'left'. */
async function side(page: Page, id: number): Promise<'left' | 'right'> {
  const bubble = page.getByTestId(`ost-chat-msg-${id}`).first();
  const box = await bubble.boundingBox();
  const scroller = await bubble.locator('xpath=ancestor::*[@data-cy="ost-chat-scroll"]').boundingBox();
  expect(box && scroller).toBeTruthy();
  const leftGap = box!.x - scroller!.x;
  const rightGap = scroller!.x + scroller!.width - (box!.x + box!.width);
  return rightGap < leftGap ? 'right' : 'left';
}

test.describe.configure({ mode: 'serial' });

test.describe('OST collaboration tabs', () => {
  test.setTimeout(150_000);

  let user: Session;
  let admin: Session;
  let teamId: number;
  let productId: number;
  let adminUserId: string;
  const k: Record<string, string> = {};
  const n: Record<string, TreeNode> = {};
  const ids: Record<string, number> = {};
  /** initials the server derives for `user` (members of the tree read) */
  let userInitials = '';

  async function mk(type: string, parentType: string, parentId: number, title: string) {
    const res = await user.api('post', '/api/tree/nodes', { type, parentType, parentId, title });
    expect(res.status(), `create ${type} "${title}"`).toBe(201);
    return (await res.json()) as TreeNode;
  }

  async function open(page: Page, key: string, tab?: 'chat' | 'questions' | 'history' | 'links') {
    await page.goto(`/trees/${teamId}/canvas?node=${key}`);
    await expect(panel(page)).toBeVisible();
    await expect(node(page, key)).toHaveClass(/\bis-selected\b/);
    if (tab) {
      if (tab === 'chat') {
        await api(page, 'GET', `/nodes/${key.split('-')[0]}/${key.split('-')[1]}/comments`, 200, () =>
          page.getByTestId(`ost-tab-${tab}`).click(),
        );
      } else {
        await page.getByTestId(`ost-tab-${tab}`).click();
      }
      await expect(page.getByTestId(`ostTab-${tab}`)).toBeVisible();
    }
  }

  async function send(page: Page, scope: Page | Locator, text: string): Promise<Comment> {
    await input(scope).fill(text);
    const response = await api(page, 'POST', /\/comments$/, 201, () => input(scope).press('Enter'));
    await expect(input(scope)).toHaveValue('');
    return (await (await response).json()) as Comment;
  }

  test.beforeAll(async ({ browser }) => {
    user = await openSession(browser, USER_USERNAME, USER_PASSWORD);
    admin = await openSession(browser, ADMIN_USERNAME, ADMIN_PASSWORD);
    for (const s of [user, admin]) {
      await s.page.setViewportSize({ width: 1600, height: 1000 });
      watchErrors(s.page);
    }

    const team = await user.api('post', '/api/team-management/teams', {
      name: `e2e collab ${stamp}`,
      description: 'ost-collaboration-tabs',
    });
    expect(team.status()).toBe(201);
    teamId = (await team.json()).id;
    registerTeamForCleanup(teamId);
    const found = (await (await user.api('get', `/api/team-management/teams/${teamId}/user-search?q=admin`)).json()) as {
      id: string;
      login: string;
    }[];
    adminUserId = found.find(u => u.login === 'admin')!.id;
    const added = await user.api('post', `/api/team-management/teams/${teamId}/members`, { userId: adminUserId, role: 'EDITOR' });
    expect(added.status()).toBe(201);

    const product = await user.api('post', '/api/products', {
      name: `Collab product ${stamp}`,
      description: null,
      archived: false,
      createdDate: new Date().toISOString(),
      team: { id: teamId },
    });
    expect(product.status()).toBe(201);
    productId = (await product.json()).id;
    k.product = `product-${productId}`;
    n.o1 = await mk('outcome', 'product', productId, 'Collab outcome');
    n.op1 = await mk('opportunity', 'outcome', n.o1.id, 'Collab opportunity');
    n.s1 = await mk('solution', 'opportunity', n.op1.id, 'Modal solution');
    n.hist = await mk('opportunity', 'outcome', n.o1.id, 'History opportunity');
    n.long = await mk('solution', 'opportunity', n.op1.id, 'Long thread solution');
    for (const [name, value] of Object.entries(n)) k[name] = value.key;
    const tree = await (await user.api('get', `/api/teams/${teamId}/tree`)).json();
    userInitials = (tree.members as { login: string; initials: string }[]).find(m => m.login === 'user')!.initials;
    expect(userInitials).toMatch(/^\S+$/);
  });

  test.afterEach(async () => {
    for (const s of [user, admin]) expect(takeErrors(s.page), 'page / console errors').toEqual([]);
  });

  test.afterAll(async () => {
    if (productId) {
      const res = await user.api('delete', `/api/products/${productId}`);
      expect(res.status(), 'product cleanup').toBe(204);
    }
    await user?.context.close();
    await admin?.context.close();
  });

  test('both post; own bubbles right; edit / delete own only; edited marker; chip and badge counts', async () => {
    const u = user.page;
    const a = admin.page;

    await open(u, k.op1, 'chat');
    await expect(u.getByTestId('ost-chat-empty')).toBeVisible();
    await expect(u.getByTestId('ost-tab-badge-chat')).toHaveCount(0);
    const first = await send(u, u, 'Heard this in 7 of 9 interviews.');
    ids.userFirst = first.id;
    await expect(u.getByTestId(`ost-chat-msg-${first.id}`)).toHaveAttribute('data-mine', 'true');
    expect(await side(u, first.id)).toBe('right');
    await expect(u.getByTestId('ost-tab-badge-chat')).toHaveText('1');
    await expect(chip(u, k.op1)).toHaveText('1');

    // Shift+Enter is a new line, not a send
    await input(u).fill('Line one');
    await input(u).press('Shift+Enter');
    await input(u).pressSequentially('line two');
    await expect(input(u)).toHaveValue('Line one\nline two');
    await input(u).fill('');

    await open(a, k.op1, 'chat');
    await expect(a.getByTestId(`ost-chat-msg-${first.id}`)).toHaveAttribute('data-mine', 'false');
    expect(await side(a, first.id)).toBe('left');
    await expect(a.getByTestId(`ost-chat-edit-${first.id}`)).toHaveCount(0);
    await expect(a.getByTestId(`ost-chat-delete-${first.id}`)).toHaveCount(0);
    await expect(a.getByTestId('ost-chat-who').first()).toHaveText(userInitials);

    const reply = await send(a, a, 'Same in enterprise, but procurement.');
    ids.adminReply = reply.id;
    expect(await side(a, reply.id)).toBe('right');
    await expect(a.getByTestId('ost-tab-badge-chat')).toHaveText('2');
    await expect(chip(a, k.op1)).toHaveText('2');

    // admin edits their own message: Escape cancels, Enter saves, then it is marked edited
    await a.getByTestId(`ost-chat-edit-${reply.id}`).click();
    await expect(a.getByTestId('ost-chat-editing')).toContainText('Editing message');
    await expect(input(a)).toHaveValue('Same in enterprise, but procurement.');
    await input(a).fill('Cancelled edit');
    await input(a).press('Escape');
    await expect(a.getByTestId('ost-chat-editing')).toHaveCount(0);
    await expect(a.getByTestId(`ost-chat-msg-${reply.id}`)).not.toContainText('(edited)');
    await a.getByTestId(`ost-chat-edit-${reply.id}`).click();
    await input(a).fill('Same in enterprise — procurement, not calendars.');
    await api(a, 'PATCH', `/api/tree/comments/${reply.id}`, 200, () => input(a).press('Enter'));
    await expect(a.getByTestId(`ost-chat-msg-${reply.id}`)).toContainText('Same in enterprise — procurement, not calendars.');
    await expect(a.getByTestId(`ost-chat-msg-${reply.id}`).getByTestId('ost-chat-edited')).toHaveText('(edited)');

    // the server refuses edits and deletes of someone else's message
    expect((await admin.api('patch', `/api/tree/comments/${first.id}`, { body: 'hijack' })).status()).toBe(403);
    expect((await admin.api('delete', `/api/tree/comments/${first.id}`)).status()).toBe(403);

    // user sees the edit (after a reload), cannot touch it, and deletes an own message
    await open(u, k.op1, 'chat');
    await expect(u.getByTestId(`ost-chat-msg-${reply.id}`).getByTestId('ost-chat-edited')).toBeVisible();
    expect(await side(u, reply.id)).toBe('left');
    await expect(u.getByTestId(`ost-chat-edit-${reply.id}`)).toHaveCount(0);
    await expect(u.getByTestId(`ost-chat-delete-${reply.id}`)).toHaveCount(0);
    const oops = await send(u, u, 'Oops, wrong node.');
    await expect(u.getByTestId('ost-tab-badge-chat')).toHaveText('3');
    await expect(chip(u, k.op1)).toHaveText('3');
    await api(u, 'DELETE', `/api/tree/comments/${oops.id}`, 204, () => u.getByTestId(`ost-chat-delete-${oops.id}`).click());
    await expect(u.getByTestId(`ost-chat-msg-${oops.id}`)).toHaveCount(0);
    await expect(u.getByTestId('ost-tab-badge-chat')).toHaveText('2');
    await expect(chip(u, k.op1)).toHaveText('2');
    const stamps = await u.getByTestId('ost-chat-stamp').allTextContents();
    expect(stamps.length).toBeGreaterThan(0);
    expect(stamps[0]).toMatch(/^Today \d\d:\d\d$/);

    // counts survive a reload
    await u.reload();
    await expect(chip(u, k.op1)).toHaveText('2');
  });

  test('the chat modal opens from the node chip; products have no chip', async () => {
    const u = user.page;
    await u.goto(`/trees/${teamId}/canvas`);
    await expect(node(u, k.s1)).toBeVisible();
    await expect(chip(u, k.product)).toHaveCount(0);
    await expect(chip(u, k.s1)).toHaveText('0');

    await api(u, 'GET', `/nodes/solution/${n.s1.id}/comments`, 200, () => chip(u, k.s1).click());
    const modal = u.getByTestId('ost-chat-modal');
    await expect(modal).toBeVisible();
    await expect(modal.getByRole('heading')).toHaveText('Modal solution');
    await expect(u.getByTestId('ost-chat-modal-sub')).toHaveText('Solution · no messages');
    await expect(input(modal)).toBeFocused();
    const msg = await send(u, modal, 'Let us hold this until the interviews are back.');
    expect(await side(u, msg.id)).toBe('right');
    await expect(u.getByTestId('ost-chat-modal-sub')).toHaveText('Solution · 1 message');
    await expect(chip(u, k.s1)).toHaveText('1');

    // Escape closes; the backdrop and the close button too
    await input(modal).press('Escape');
    await expect(modal).toHaveCount(0);
    await chip(u, k.s1).click();
    await expect(modal).toBeVisible();
    await u.getByTestId('ost-chat-modal-close').click();
    await expect(modal).toHaveCount(0);
    await chip(u, k.s1).click();
    await expect(modal).toBeVisible();
    await u.mouse.click(320, 700); // the backdrop, beside the dialog
    await expect(modal).toHaveCount(0);

    // the same thread in the panel's Chat tab
    await open(u, k.s1, 'chat');
    await expect(u.getByTestId(`ost-chat-msg-${msg.id}`)).toBeVisible();
    await expect(u.getByTestId('ost-tab-badge-chat')).toHaveText('1');
  });

  test('open questions: add, tick, remove, summary and badge', async () => {
    const u = user.page;
    await open(u, k.op1, 'questions');
    await expect(u.getByTestId('ost-questions-hint')).toContainText('Delivery work belongs in Jira');
    await expect(u.getByTestId('ost-questions-summary')).toHaveText('0 open · 0 answered');
    await expect(u.getByTestId('ost-tab-badge-questions')).toHaveCount(0);

    const add = u.getByTestId('ost-question-add');
    const created: number[] = [];
    for (const text of ['Who else has said this?', 'Is this sized?']) {
      await add.fill(text);
      const res = await api(u, 'POST', `/opportunities/${n.op1.id}/questions`, 201, () => add.press('Enter'));
      created.push(((await (await res).json()) as { id: number }).id);
      await expect(add).toHaveValue('');
    }
    await expect(u.getByTestId('ost-questions-summary')).toHaveText('2 open · 0 answered');
    await expect(u.getByTestId('ost-tab-badge-questions')).toHaveText('2');

    await api(u, 'PATCH', `/api/tree/questions/${created[0]}`, 200, () => u.getByTestId(`ost-question-toggle-${created[0]}`).click());
    await expect(u.getByTestId(`ost-question-toggle-${created[0]}`)).toHaveAttribute('aria-checked', 'true');
    await expect(u.getByTestId('ost-questions-summary')).toHaveText('1 open · 1 answered');
    await expect(u.getByTestId('ost-tab-badge-questions')).toHaveText('1');

    await api(u, 'DELETE', `/api/tree/questions/${created[1]}`, 204, () => u.getByTestId(`ost-question-remove-${created[1]}`).click());
    await expect(u.getByTestId(`ost-question-${created[1]}`)).toHaveCount(0);
    await expect(u.getByTestId('ost-questions-summary')).toHaveText('0 open · 1 answered');
    await expect(u.getByTestId('ost-tab-badge-questions')).toHaveCount(0);

    // untick brings the badge back; the state survives a reload
    await api(u, 'PATCH', `/api/tree/questions/${created[0]}`, 200, () => u.getByTestId(`ost-question-toggle-${created[0]}`).click());
    await expect(u.getByTestId('ost-tab-badge-questions')).toHaveText('1');
    await open(u, k.op1, 'questions');
    await expect(u.getByTestId('ost-questions-summary')).toHaveText('1 open · 0 answered');
    await expect(u.getByTestId(`ost-question-${created[0]}`)).toContainText('Who else has said this?');
  });

  test('history: recorded events newest first, nothing for title or notes', async () => {
    const u = user.page;
    const opp = n.hist;
    await open(u, k.hist, 'history');
    await expect(u.locator('[data-cy^="ost-history-"][data-event]')).toHaveCount(1);
    await expect(u.getByTestId('ost-history-what')).toHaveText(['Node created as opportunity']);
    const byUserToday = new RegExp(`^${userInitials} · Today [0-9]{2}:[0-9]{2}$`);
    await expect(u.getByTestId('ost-history-meta').first()).toHaveText(byUserToday);

    await u.getByTestId('ost-tab-detail').click();
    await api(u, 'PATCH', `/nodes/opportunity/${opp.id}`, 200, () => u.getByTestId('ost-status-validated').click());
    // title and notes are saved but never recorded
    const title = u.getByTestId('ost-panel-title');
    await title.fill('History opportunity renamed');
    await api(u, 'PATCH', `/nodes/opportunity/${opp.id}`, 200, () => title.press('Enter'));
    const notes = u.getByTestId('ost-notes');
    await notes.fill('Notes typing must not be logged.');
    await api(u, 'PATCH', `/nodes/opportunity/${opp.id}`, 200, () => notes.blur());
    await api(u, 'PATCH', `/nodes/opportunity/${opp.id}`, 200, () => u.getByTestId('ost-value-5').click());

    // the History tab refreshes after edits made in the same session
    await u.getByTestId('ost-tab-history').click();
    await expect(u.getByTestId('ost-history-what')).toHaveText([
      /^Value set to \$+$/,
      'Status changed to “validated”',
      'Node created as opportunity',
    ]);

    await u.getByTestId('ost-tab-links').click();
    await u.getByTestId('ost-link-add').click();
    await u.getByTestId('ost-link-new-name').fill('Research board');
    await u.getByTestId('ost-link-new-url').fill('https://example.com/research');
    await api(u, 'POST', `/nodes/opportunity/${opp.id}/links`, 201, () => u.getByTestId('ost-link-new-save').click());

    await u.getByTestId('ost-tab-questions').click();
    await u.getByTestId('ost-question-add').fill('Which segment feels it most?');
    await api(u, 'POST', `/opportunities/${opp.id}/questions`, 201, () => u.getByTestId('ost-question-add').press('Enter'));

    await api(u, 'GET', `/nodes/opportunity/${opp.id}/comments`, 200, () => u.getByTestId('ost-tab-chat').click());
    await send(u, u, 'Logged in the history too.');

    // re-parent under the other opportunity (through the API; drag is covered by the canvas spec)
    const moved = await user.api('post', '/api/tree/nodes/move', {
      nodeType: 'OPPORTUNITY',
      nodeId: opp.id,
      parentType: 'OPPORTUNITY',
      parentId: n.op1.id,
    });
    expect(moved.status()).toBe(200);

    await open(u, k.hist, 'history');
    await expect(u.getByTestId('ost-history-what')).toHaveText([
      'Moved under “Collab opportunity”',
      'Comment added',
      'Open question added',
      'Link added',
      /^Value set to \$+$/,
      'Status changed to “validated”',
      'Node created as opportunity',
    ]);
    await expect(u.locator('[data-cy^="ost-history-"][data-event]').first()).toHaveAttribute('data-event', 'MOVED');
    for (const meta of await u.getByTestId('ost-history-meta').allTextContents()) expect(meta).toMatch(byUserToday);
  });

  test('a viewer reads the chat but cannot post, edit or delete', async () => {
    const demoted = await user.api('put', `/api/team-management/teams/${teamId}/members/${adminUserId}`, { role: 'VIEWER' });
    expect(demoted.status()).toBe(200);
    const a = admin.page;
    await open(a, k.op1, 'chat');
    await expect(a.getByTestId(`ost-chat-msg-${ids.userFirst}`)).toBeVisible();
    await expect(a.getByTestId(`ost-chat-msg-${ids.adminReply}`)).toHaveAttribute('data-mine', 'true');
    await expect(input(a)).toBeDisabled();
    await expect(a.getByTestId('ost-chat-send')).toBeDisabled();
    await expect(a.getByTestId('ost-chat-readonly')).toContainText('only owners and editors can post');
    await expect(a.getByTestId(`ost-chat-edit-${ids.adminReply}`)).toHaveCount(0);
    await expect(a.getByTestId(`ost-chat-delete-${ids.adminReply}`)).toHaveCount(0);

    // the server agrees
    const nodePath = `/api/tree/nodes/opportunity/${n.op1.id}/comments`;
    expect((await admin.api('post', nodePath, { body: 'viewer post' })).status()).toBe(403);
    expect((await admin.api('patch', `/api/tree/comments/${ids.adminReply}`, { body: 'viewer edit' })).status()).toBe(403);
    expect((await admin.api('delete', `/api/tree/comments/${ids.adminReply}`)).status()).toBe(403);

    // open questions and the modal are read-only too
    await a.getByTestId('ost-tab-questions').click();
    await expect(a.getByTestId('ost-question-add')).toHaveCount(0);
    await expect(a.locator('[data-cy^="ost-question-remove-"]')).toHaveCount(0);
    await expect(a.locator('[data-cy^="ost-question-toggle-"]').first()).toBeDisabled();
    await chip(a, k.s1).click();
    const modal = a.getByTestId('ost-chat-modal');
    await expect(modal).toBeVisible();
    await expect(input(modal)).toBeDisabled();
    await a.keyboard.press('Escape');
    await expect(modal).toHaveCount(0);
  });

  test('a long thread opens at the latest message; jump-to-latest after scrolling up', async () => {
    const path = `/api/tree/nodes/solution/${n.long.id}/comments`;
    let last = 0;
    for (let i = 1; i <= 30; i++) {
      const res = await user.api('post', path, { body: `Message number ${i} in a long thread about the solution.` });
      expect(res.status()).toBe(201);
      last = ((await res.json()) as Comment).id;
    }
    const u = user.page;
    await open(u, k.long, 'chat');
    await expect(u.getByTestId('ost-tab-badge-chat')).toHaveText('30');
    const scroller = u.getByTestId('ost-chat-scroll');
    await expect(u.getByTestId(`ost-chat-msg-${last}`)).toBeInViewport();
    await expect(u.getByTestId('ost-chat-jump')).toHaveCount(0);
    expect(await scroller.evaluate(el => el.scrollHeight > el.clientHeight)).toBe(true);

    await scroller.evaluate(el => el.scrollTo({ top: 0 }));
    await expect(u.getByTestId('ost-chat-jump')).toBeVisible();
    await expect(u.getByTestId(`ost-chat-msg-${last}`)).not.toBeInViewport();
    await u.getByTestId('ost-chat-jump').click();
    await expect(u.getByTestId(`ost-chat-msg-${last}`)).toBeInViewport();
    await expect(u.getByTestId('ost-chat-jump')).toHaveCount(0);

    // a new message while scrolled up still lands in view
    await scroller.evaluate(el => el.scrollTo({ top: 0 }));
    await expect(u.getByTestId('ost-chat-jump')).toBeVisible();
    const fresh = await send(u, u, 'And one more.');
    await expect(u.getByTestId(`ost-chat-msg-${fresh.id}`)).toBeInViewport();
    await expect(u.getByTestId('ost-chat-jump')).toHaveCount(0);
  });
});
