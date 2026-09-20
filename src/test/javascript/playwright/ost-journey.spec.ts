import { type Browser, type Locator, type Page, type Response, expect, test } from '@playwright/test';

import { registerTeamForCleanup } from './support/cleanup';
import { ADMIN_PASSWORD, ADMIN_USERNAME, type Session, USER_PASSWORD, USER_USERNAME, openSession } from './support/session';

/**
 * OST step 13b: the prototype flow end to end, as one person would walk it.
 *
 * `admin` owns a throwaway team (API only) and adds `user` as an EDITOR. `user` then, in the UI:
 * lands on /trees and switches to the team → dashboard → "Open branch" to the canvas; builds a
 * branch with the + menu and the palette (outcome, nested opportunities, solution, assumption,
 * evidence under an opportunity and under an assumption); renames inline (the new title shows in the
 * frame the field closes; a rename ended by clicking the search box leaves focus and typing there);
 * sets status / value / priority / confidence / owner / notes in the panel; adds, removes and
 * restores links; adds and ticks open questions; posts, edits and deletes chat messages; checks the
 * history (and that title / notes left no entry); drags a node to re-parent it; collapses and
 * expands; jumps to a hidden node with search + Enter; filters by type; scopes to a product; opens
 * an assumption from the Experiments tracker; edits confidence / value / priority on the full-page
 * detail, leaves a typed note by navigating (it is saved) and deletes a product there; deletes a
 * subtree with its descendant count; reloads and finds everything persisted. On a touch screen the
 * small chat / question buttons are 44px, and every compact control (view tabs, team and product
 * combos, palette toggle, filter / status chips, breadcrumbs, $ steps, link restore, node chips)
 * has a 44px+ hit area that no neighbour's covers. Finally `user` is demoted to VIEWER and a second session
 * checks that everything is read-only.
 *
 * Every page / console error fails the test (except the browser's log line for an expected 403).
 * No fixed sleeps. The team is registered with support/cleanup.ts; products are deleted in afterAll.
 */

interface TreeNode {
  key: string;
  type: string;
  id: number;
  parentKey: string | null;
  title: string;
  notes: string | null;
  status: string | null;
  confidence: number | null;
  priority: number | null;
  valueRating: number | null;
  ownerLogin: string | null;
  links: { id: number; name: string; url: string }[];
  questions: { id: number; text: string; done: boolean }[];
}

const stamp = Date.now();
const P1 = `Journey product ${stamp}`;
const P2 = `Second product ${stamp}`;

const errorsOf = new WeakMap<Page, string[]>();

function watchErrors(page: Page) {
  const list: string[] = [];
  errorsOf.set(page, list);
  page.on('pageerror', e => list.push(`pageerror: ${e.message}`));
  page.on('console', m => {
    if (m.type() === 'error' && !/status of 403/.test(m.text())) list.push(`console.error: ${m.text()}`);
  });
}

const takeErrors = (page: Page) => {
  const list = errorsOf.get(page) ?? [];
  return list.splice(0, list.length);
};

const node = (page: Page, key: string) => page.getByTestId(`ost-node-${key}`);
const title = (page: Page, key: string) => node(page, key).getByTestId('ost-node-title');
const panel = (page: Page) => page.getByTestId('ost-panel');
const renameInput = (page: Page) => page.getByTestId('ost-rename-input');

async function box(locator: Locator) {
  const b = await locator.boundingBox();
  expect(b, 'element has a box').not.toBeNull();
  return b!;
}

const centre = (b: { x: number; y: number; width: number; height: number }) => ({ x: b.x + b.width / 2, y: b.y + b.height / 2 });

/** Waits for the API call `action` triggers and checks its status. */
async function api(page: Page, method: string, path: string | RegExp, status: number, action: () => Promise<unknown>): Promise<Response> {
  const response = page.waitForResponse(
    r => r.request().method() === method && (typeof path === 'string' ? r.url().includes(path) : path.test(r.url())),
  );
  await action();
  const res = await response;
  expect(res.status(), `${method} ${path}`).toBe(status);
  return res;
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
      { intervals: [120] },
    )
    .toBe(true);
}

/** Fit the view to the branch and wait for it to stop moving. */
async function fit(page: Page) {
  await page.getByTestId('ost-fit').click();
  await settle(page);
}

/** Presses on a node's kicker (never a button) and moves in steps to `to`; the caller releases. */
async function pickUp(page: Page, from: Locator, to: { x: number; y: number }) {
  const start = centre(await box(from.locator('.ost-node__kicker')));
  await page.mouse.move(start.x, start.y);
  await page.mouse.down();
  await page.mouse.move(start.x + 12, start.y + 12, { steps: 3 });
  await page.mouse.move(to.x, to.y, { steps: 12 });
}

/** Drags a priority slider from 30% to `fraction` of its width: one PATCH, on release. */
async function dragPriority(page: Page, fraction: number) {
  const slider = page.getByTestId('ost-priority');
  const b = await box(slider);
  await page.mouse.move(b.x + b.width * 0.3, b.y + b.height / 2);
  await page.mouse.down();
  await page.mouse.move(b.x + b.width * fraction, b.y + b.height / 2, { steps: 6 });
  await api(page, 'PATCH', '/api/tree/nodes/', 200, () => page.mouse.up());
  return Number(await slider.getAttribute('aria-valuenow'));
}

/**
 * Types `text` into the open rename field of `key` and presses Enter, recording the node's title
 * every time the DOM changes: the field must close straight onto the new title (never a frame with
 * the old one while the save is in flight).
 */
async function commitRename(page: Page, key: string, text: string) {
  const input = renameInput(page);
  await expect(input).toBeFocused();
  await input.fill(text);
  await page.evaluate(k => {
    const host = document.querySelector(`.vue-flow__node[data-id="${CSS.escape(k)}"]`)!;
    const seen: string[] = [];
    const observer = new MutationObserver(() => {
      const shown = host.querySelector('[data-cy="ost-node-title"]');
      if (shown && !host.querySelector('[data-cy="ost-rename-input"]')) seen.push((shown.textContent ?? '').trim());
    });
    observer.observe(host, { subtree: true, childList: true, characterData: true });
    (window as unknown as { __titles: () => string[] }).__titles = () => {
      observer.disconnect();
      return seen;
    };
  }, key);
  await input.press('Enter');
  await expect(input).toHaveCount(0);
  await expect(title(page, key)).toHaveText(text);
  const seen = await page.evaluate(() => (window as unknown as { __titles: () => string[] }).__titles());
  expect(seen.length, 'the title was rendered after the field closed').toBeGreaterThan(0);
  expect(
    seen.filter(t => t !== text),
    'titles shown after the field closed',
  ).toEqual([]);
}

test.describe.configure({ mode: 'serial' });

test.describe('OST journey (editor, then viewer)', () => {
  let user: Session;
  let admin: Session;
  let teamId: number;
  let userId: string;
  const productIds: Record<'p1' | 'p2', number> = { p1: 0, p2: 0 };
  const k: Record<string, string> = {};
  const deleted = new Set<number>();

  async function treeNodes(): Promise<TreeNode[]> {
    const res = await admin.api('get', `/api/teams/${teamId}/tree`);
    expect(res.status()).toBe(200);
    return ((await res.json()) as { nodes: TreeNode[] }).nodes;
  }

  const serverNode = async (key: string) => (await treeNodes()).find(n => n.key === key);

  async function mkProduct(name: string) {
    const res = await admin.api('post', '/api/products', {
      name,
      description: null,
      archived: false,
      createdDate: new Date().toISOString(),
      team: { id: teamId },
    });
    expect(res.status(), `create product ${name}`).toBe(201);
    return (await res.json()).id as number;
  }

  /** Opens `parent`'s + menu, picks `type`; returns the created node's key (it is selected, in rename mode). */
  async function addViaMenu(page: Page, parent: string, type: string) {
    await page.getByTestId(`ost-node-add-${parent}`).click();
    const res = await api(page, 'POST', /\/api\/tree\/nodes$/, 201, () => node(page, parent).getByTestId(`ost-add-menu-${type}`).click());
    const created = (await res.json()) as TreeNode;
    await expect(node(page, created.key)).toHaveClass(/\bis-selected\b/);
    await expect(renameInput(page)).toBeFocused();
    return created.key;
  }

  async function select(page: Page, key: string) {
    await node(page, key).locator('.ost-node__kicker').click();
    await expect(node(page, key)).toHaveClass(/\bis-selected\b/);
    await expect(panel(page)).toBeVisible();
    await expect(page.getByTestId('ost-panel-title')).toHaveValue((await serverNode(key))!.title);
  }

  async function openSessionAt(browser: Browser, username: string, password: string) {
    const s = await openSession(browser, username, password);
    await s.page.setViewportSize({ width: 1600, height: 1000 });
    watchErrors(s.page);
    return s;
  }

  test.beforeAll(async ({ browser }) => {
    test.setTimeout(120_000);
    user = await openSessionAt(browser, USER_USERNAME, USER_PASSWORD);
    admin = await openSessionAt(browser, ADMIN_USERNAME, ADMIN_PASSWORD);

    const team = await admin.api('post', '/api/team-management/teams', { name: `e2e journey ${stamp}`, description: 'ost-journey' });
    expect(team.status()).toBe(201);
    teamId = (await team.json()).id;
    registerTeamForCleanup(teamId);
    const found = (await (await admin.api('get', `/api/team-management/teams/${teamId}/user-search?q=user`)).json()) as {
      id: string;
      login: string;
    }[];
    userId = found.find(u => u.login === 'user')!.id;
    expect((await admin.api('post', `/api/team-management/teams/${teamId}/members`, { userId, role: 'EDITOR' })).status()).toBe(201);

    productIds.p1 = await mkProduct(P1);
    productIds.p2 = await mkProduct(P2);
    k.p1 = `product-${productIds.p1}`;
    k.p2 = `product-${productIds.p2}`;
    const p2Outcome = await admin.api('post', '/api/tree/nodes', {
      type: 'outcome',
      parentType: 'product',
      parentId: productIds.p2,
      title: 'P2 outcome',
    });
    expect(p2Outcome.status()).toBe(201);
  });

  test.afterEach(async () => {
    for (const s of [user, admin]) if (s) expect(takeErrors(s.page), 'page / console errors').toEqual([]);
  });

  test.afterAll(async () => {
    for (const id of Object.values(productIds)) {
      if (!id || deleted.has(id)) continue;
      expect((await admin.api('delete', `/api/tree/nodes/product/${id}`)).status(), `product ${id} cleanup`).toBe(204);
    }
    await user?.context.close();
    await admin?.context.close();
  });

  test('an editor walks the whole flow', async () => {
    test.setTimeout(300_000);
    const page = user.page;

    await test.step('/trees → the team → dashboard → Open branch → canvas', async () => {
      await page.goto('/trees');
      await expect(page).toHaveURL(/\/trees\/\d+$/);
      await expect(page.getByTestId('ostDashboardPage')).toBeVisible();
      await page.getByTestId('ostTeamComboButton').click();
      await page.getByTestId(`ostTeamOption-${teamId}`).click();
      await expect(page).toHaveURL(new RegExp(`/trees/${teamId}$`));
      await expect(page.getByTestId('ostTeamComboLabel')).toHaveText(`e2e journey ${stamp}`);
      await expect(page.getByTestId(`ost-product-card-${k.p1}`)).toContainText(P1);
      await expect(page.getByTestId(`ost-product-card-${k.p2}`)).toContainText(P2);
      await page.getByTestId(`ost-open-branch-${k.p1}`).click();
      await expect(page).toHaveURL(new RegExp(`/trees/${teamId}/canvas\\?product=${k.p1}$`));
      await expect(node(page, k.p1)).toBeVisible();
      await expect(node(page, k.p2)).toHaveCount(0);
      await settle(page);
    });

    await test.step('build a branch with the + menu and the palette', async () => {
      k.outcome = await addViaMenu(page, k.p1, 'outcome');
      await expect(renameInput(page)).toHaveValue('New outcome');
      await commitRename(page, k.outcome, 'Journey outcome');

      k.op1 = await addViaMenu(page, k.outcome, 'opportunity');
      await commitRename(page, k.op1, 'Parent opportunity');
      k.op2 = await addViaMenu(page, k.op1, 'opportunity');
      await commitRename(page, k.op2, 'Nested opportunity');
      k.ev1 = await addViaMenu(page, k.op1, 'evidence');
      await expect(renameInput(page)).toHaveValue('New snippet');
      await commitRename(page, k.ev1, 'Interview snippet');
      k.op3 = await addViaMenu(page, k.outcome, 'opportunity');
      await commitRename(page, k.op3, 'Movable opportunity');
      k.doomed = await addViaMenu(page, k.op3, 'solution');
      await commitRename(page, k.doomed, 'Doomed solution');

      // Palette drag: a solution onto the nested opportunity.
      await fit(page);
      const tool = page.getByTestId('ost-palette-solution');
      const start = centre(await box(tool));
      await page.mouse.move(start.x, start.y);
      await page.mouse.down();
      await page.mouse.move(start.x + 40, start.y + 10, { steps: 4 });
      await expect(page.getByTestId('ost-palette-ghost')).toBeVisible();
      await page.mouse.move(centre(await box(node(page, k.op2))).x, centre(await box(node(page, k.op2))).y, { steps: 12 });
      await expect(node(page, k.op2)).toHaveAttribute('data-drop-hover', 'true');
      const dropped = await api(page, 'POST', /\/api\/tree\/nodes$/, 201, () => page.mouse.up());
      k.s1 = ((await dropped.json()) as TreeNode).key;
      await commitRename(page, k.s1, 'Journey solution');

      // Palette click-to-arm: an assumption onto the solution.
      await fit(page);
      await page.getByTestId('ost-palette-assumption').click();
      await expect(node(page, k.s1)).toHaveAttribute('data-drop-target', 'true');
      const armed = await api(page, 'POST', /\/api\/tree\/nodes$/, 201, () => node(page, k.s1).locator('.ost-node__kicker').click());
      k.a1 = ((await armed.json()) as TreeNode).key;
      await commitRename(page, k.a1, 'Journey assumption');
      await expect(page.getByTestId('ost-palette-assumption')).toHaveAttribute('aria-pressed', 'false');

      // Evidence under the assumption (the only other parent evidence may have).
      k.ev2 = await addViaMenu(page, k.a1, 'evidence');
      await commitRename(page, k.ev2, 'Test result');

      const nodes = await treeNodes();
      const parentOf = (key: string) => nodes.find(n => n.key === key)?.parentKey;
      expect([k.outcome, k.op1, k.op2, k.s1, k.a1, k.ev1, k.ev2, k.op3, k.doomed].map(parentOf)).toEqual([
        k.p1,
        k.outcome,
        k.op1,
        k.op2,
        k.s1,
        k.op1,
        k.a1,
        k.outcome,
        k.op3,
      ]);
    });

    await test.step('rename inline; a rename ended by a click into the search box leaves focus and typing there', async () => {
      await fit(page);
      await title(page, k.op2).dblclick();
      await expect(renameInput(page)).toBeFocused();
      await renameInput(page).fill('Nested opportunity renamed');
      const search = page.getByTestId('ost-search');
      await api(page, 'PATCH', `/api/tree/nodes/opportunity/`, 200, async () => {
        await search.click();
        await page.keyboard.type('abc');
      });
      await expect(renameInput(page)).toHaveCount(0);
      await expect(search).toBeFocused();
      await expect(search).toHaveValue('abc');
      await expect(title(page, k.op2)).toHaveText('Nested opportunity renamed');
      await search.fill('');
    });

    await test.step('panel: status, value, priority, notes (opportunity); status, confidence, owner (assumption)', async () => {
      await fit(page);
      await select(page, k.op1);
      const oppPath = `/api/tree/nodes/opportunity/${k.op1.split('-')[1]}`;
      await api(page, 'PATCH', oppPath, 200, () => page.getByTestId('ost-status-exploring').click());
      await expect(node(page, k.op1).getByTestId('ost-node-status')).toHaveText('exploring');
      await api(page, 'PATCH', oppPath, 200, () => page.getByTestId('ost-value-5').click());
      await expect(page.getByTestId('ost-value-label')).toHaveText('Outsized');
      const priority = await dragPriority(page, 0.9);
      expect(priority).toBeGreaterThanOrEqual(85);
      const notes = page.getByTestId('ost-notes');
      await notes.fill('Heard in most interviews.');
      await api(page, 'PATCH', oppPath, 200, () => notes.blur());

      await select(page, k.a1);
      const aPath = `/api/tree/nodes/assumption/${k.a1.split('-')[1]}`;
      await api(page, 'PATCH', aPath, 200, () => page.getByTestId('ost-status-testing').click());
      await api(page, 'PATCH', aPath, 200, () => page.getByTestId('ost-confidence-80').click());
      await expect(node(page, k.a1).getByTestId('ost-node-metric')).toHaveText('80% confidence');
      await api(page, 'PATCH', aPath, 200, () => page.getByTestId('ost-owner').selectOption('user'));

      expect(await serverNode(k.op1)).toMatchObject({ status: 'EXPLORING', valueRating: 5, priority, notes: 'Heard in most interviews.' });
      expect(await serverNode(k.a1)).toMatchObject({ status: 'TESTING', confidence: 80, ownerLogin: 'user' });
    });

    await test.step('links: add, remove a default, restore it', async () => {
      await select(page, k.op1);
      await page.getByTestId('ost-tab-links').click();
      await expect(page.getByTestId('ost-tab-badge-links')).toHaveText('3');
      await page.getByTestId('ost-link-add').click();
      await page.getByTestId('ost-link-new-name').fill('Research board');
      await page.getByTestId('ost-link-new-url').fill('https://example.com/research');
      await api(page, 'POST', /\/links$/, 201, () => page.getByTestId('ost-link-new-save').click());
      await expect(page.getByTestId('ost-tab-badge-links')).toHaveText('4');

      const confluence = (await serverNode(k.op1))!.links.find(l => l.name === 'Confluence')!;
      await api(page, 'DELETE', `/api/tree/links/${confluence.id}`, 204, () =>
        page.getByTestId(`ost-link-row-${confluence.id}`).getByTestId('ost-link-remove').click(),
      );
      await expect(page.getByTestId('ost-link-restore-confluence')).toBeEnabled();
      await api(page, 'POST', /\/links$/, 201, () => page.getByTestId('ost-link-restore-confluence').click());
      await expect(page.getByTestId('ost-link-restore-confluence')).toBeDisabled();
      expect((await serverNode(k.op1))!.links.map(l => l.name).sort()).toEqual([
        'Confluence',
        'Jira Epic',
        'Jira Initiative',
        'Research board',
      ]);
    });

    await test.step('open questions: add two, tick one', async () => {
      await page.getByTestId('ost-tab-questions').click();
      const add = page.getByTestId('ost-question-add');
      const ids: number[] = [];
      for (const text of ['Who else has said this?', 'How often does it happen?']) {
        await add.fill(text);
        const res = await api(page, 'POST', /\/questions$/, 201, () => add.press('Enter'));
        ids.push(((await res.json()) as { id: number }).id);
        await expect(add).toHaveValue('');
      }
      await api(page, 'PATCH', `/api/tree/questions/${ids[0]}`, 200, () => page.getByTestId(`ost-question-toggle-${ids[0]}`).click());
      await expect(page.getByTestId('ost-questions-summary')).toHaveText('1 open · 1 answered');
      await expect(page.getByTestId('ost-tab-badge-questions')).toHaveText('1');
    });

    await test.step('chat: post, edit, delete', async () => {
      await api(page, 'GET', /\/comments$/, 200, () => page.getByTestId('ost-tab-chat').click());
      const input = page.getByTestId('ost-chat-input');
      const posted: number[] = [];
      for (const text of ['Seven of nine interviews mention this.', 'Oops, wrong node.']) {
        await input.fill(text);
        const res = await api(page, 'POST', /\/comments$/, 201, () => input.press('Enter'));
        posted.push(((await res.json()) as { id: number }).id);
        await expect(input).toHaveValue('');
      }
      k.keptComment = String(posted[0]);
      await page.getByTestId(`ost-chat-edit-${posted[0]}`).click();
      await input.fill('Seven of nine interviews mention this (edited).');
      await api(page, 'PATCH', `/api/tree/comments/${posted[0]}`, 200, () => input.press('Enter'));
      await expect(page.getByTestId(`ost-chat-msg-${posted[0]}`).getByTestId('ost-chat-edited')).toBeVisible();
      await api(page, 'DELETE', `/api/tree/comments/${posted[1]}`, 204, () => page.getByTestId(`ost-chat-delete-${posted[1]}`).click());
      await expect(page.getByTestId(`ost-chat-msg-${posted[1]}`)).toHaveCount(0);
      await expect(page.getByTestId(`ost-node-chat-${k.op1}`)).toHaveText('1');
    });

    await test.step('history lists what happened, newest first, and nothing for title / notes edits', async () => {
      await page.getByTestId('ost-tab-history').click();
      await expect(page.getByTestId('ost-history-what')).toHaveText([
        'Comment deleted',
        'Comment added',
        'Comment added',
        'Open question added',
        'Open question added',
        'Link added',
        'Link removed',
        'Link added',
        /^Priority set to \w+$/,
        'Value set to $$$$$',
        'Status changed to “exploring”',
        'Node created as opportunity',
      ]);
    });

    await test.step('drag a node onto another to re-parent it', async () => {
      await fit(page);
      await pickUp(page, node(page, k.op3), centre(await box(node(page, k.op1))));
      await expect(node(page, k.op1)).toHaveAttribute('data-drop-hover', 'true');
      await api(page, 'POST', '/api/tree/nodes/move', 200, () => page.mouse.up());
      await expect(page.locator('[data-drop-target]')).toHaveCount(0);
      expect((await serverNode(k.op3))!.parentKey).toBe(k.op1);
      await settle(page);
      expect((await box(node(page, k.op3))).y).toBeGreaterThan((await box(node(page, k.op1))).y);

      await select(page, k.op3);
      await page.getByTestId('ost-tab-history').click();
      await expect(page.getByTestId('ost-history-what')).toHaveText(['Moved under “Parent opportunity”', 'Node created as opportunity']);
    });

    await test.step('collapse and expand', async () => {
      const chip = page.getByTestId(`ost-collapse-${k.op1}`);
      await chip.click();
      await expect(chip).toHaveText(/^\+\d+$/);
      for (const key of [k.op2, k.s1, k.a1, k.ev1, k.ev2, k.op3]) await expect(node(page, key)).toHaveCount(0);
      await chip.click();
      await expect(chip).toHaveText('–');
      await expect(node(page, k.op2)).toBeVisible();
    });

    await test.step('search + Enter jumps to a node hidden under a collapsed branch', async () => {
      await page.getByTestId(`ost-collapse-${k.op1}`).click();
      await expect(node(page, k.ev2)).toHaveCount(0);
      const search = page.getByTestId('ost-search');
      await search.fill('test result');
      await search.press('Enter');
      await expect(node(page, k.ev2)).toHaveClass(/\bis-selected\b/);
      await expect(page).toHaveURL(new RegExp(`node=${k.ev2}`));
      await expect(page.getByTestId('ost-search-status')).toHaveText('Match 1 of 1: Test result');
      await expect(page.getByTestId(`ost-collapse-${k.op1}`)).toHaveText('–');
      await search.fill('');
    });

    await test.step('type filters dim a type', async () => {
      const chip = page.getByTestId('ost-filter-evidence');
      await chip.click();
      await expect(chip).toHaveAttribute('aria-pressed', 'false');
      await expect(node(page, k.ev1)).toHaveClass(/\bis-dimmed\b/);
      await expect(node(page, k.op1)).not.toHaveClass(/\bis-dimmed\b/);
      await chip.click();
      await expect(page.locator('.ost-node.is-dimmed')).toHaveCount(0);
    });

    await test.step('product scope: all products, then one', async () => {
      await page.getByTestId('ost-product-combo').click();
      await page.getByTestId('ost-product-option-all').click();
      await expect(page).not.toHaveURL(/product=/);
      await expect(node(page, k.p2)).toBeVisible();
      await page.getByTestId('ost-product-combo').click();
      await page.getByTestId(`ost-product-option-${k.p1}`).click();
      await expect(page).toHaveURL(new RegExp(`product=${k.p1}`));
      await expect(page.getByTestId('ost-product-combo-label')).toHaveText(P1);
      await expect(node(page, k.p2)).toHaveCount(0);
    });

    await test.step('Experiments tracker row → the assumption on the canvas', async () => {
      await page.getByTestId('ostTabExperiments').click();
      await expect(page.getByTestId('ostExperimentsPage')).toBeVisible();
      const row = page.getByTestId(`ost-experiment-row-${k.a1}`);
      await expect(row).toContainText('Journey assumption');
      await expect(row.getByTestId('ost-experiment-confidence')).toContainText('80');
      await row.click();
      await expect(page).toHaveURL(new RegExp(`/trees/${teamId}/canvas\\?.*node=${k.a1}`));
      await expect(node(page, k.a1)).toHaveClass(/\bis-selected\b/);
      await expect(page.getByTestId('ost-panel-title')).toHaveValue('Journey assumption');
    });

    await test.step('full-page detail: confidence, value, priority; a typed note is saved on navigating away; back', async () => {
      await page.getByTestId('ost-open-detail').click();
      await expect(page).toHaveURL(new RegExp(`/trees/${teamId}/nodes/${k.a1}$`));
      await expect(page.getByTestId('ost-node-detail-title')).toHaveValue('Journey assumption');
      await api(page, 'PATCH', `/api/tree/nodes/assumption/`, 200, () => page.getByTestId('ost-confidence-60').click());
      await expect(page.getByTestId('ost-node-detail-metric-confidence')).toHaveText('Med-high · 60%');

      await page.getByTestId(`ost-node-detail-crumb-${k.op1}`).click();
      await expect(page.getByTestId('ost-node-detail-title')).toHaveValue('Parent opportunity');
      await api(page, 'PATCH', '/api/tree/nodes/opportunity/', 200, () => page.getByTestId('ost-value-2').click());
      await expect(page.getByTestId('ost-node-detail-metric-value')).toContainText('$$');
      k.detailPriority = String(await dragPriority(page, 0.15));

      // A note being typed is saved when the user navigates away (breadcrumb) without blurring first.
      await page.getByTestId('ost-notes').fill('Typed on the page, then navigated.');
      await api(page, 'PATCH', `/api/tree/nodes/opportunity/${k.op1.split('-')[1]}`, 200, () =>
        page.getByTestId(`ost-node-detail-crumb-${k.outcome}`).click(),
      );
      await expect(page.getByTestId('ost-node-detail-title')).toHaveValue('Journey outcome');
      expect(await serverNode(k.op1)).toMatchObject({ valueRating: 2, notes: 'Typed on the page, then navigated.' });
      expect(await serverNode(k.a1)).toMatchObject({ confidence: 60 });

      await expect(page.getByTestId('ost-node-detail-open-canvas')).toHaveText('← Back to canvas');
      await page.getByTestId('ost-node-detail-open-canvas').click();
      await expect(page).toHaveURL(new RegExp(`/trees/${teamId}/canvas`));
      await expect(node(page, k.outcome)).toHaveClass(/\bis-selected\b/);
    });

    await test.step('full-page detail: delete a product (with its outcome)', async () => {
      await page.goto(`/trees/${teamId}/nodes/${k.p2}`);
      await expect(page.getByTestId('ost-node-detail-kicker')).toHaveText('Product');
      await page.getByTestId('ost-node-detail-delete').click();
      await expect(page.getByTestId('ostConfirmDeleteText')).toContainText(`“${P2}” and its 1 descendant`);
      await api(page, 'DELETE', `/api/tree/nodes/product/${productIds.p2}`, 204, () => page.getByTestId('ostConfirmDeleteConfirm').click());
      deleted.add(productIds.p2);
      await expect(page).toHaveURL(new RegExp(`/trees/${teamId}/canvas`));
      await expect(node(page, k.p1)).toBeVisible();
      await expect(node(page, k.p2)).toHaveCount(0);
      expect((await treeNodes()).some(n => n.key === k.p2)).toBe(false);
    });

    await test.step('delete a subtree: the dialog names the descendants', async () => {
      await fit(page);
      await select(page, k.op3);
      await page.keyboard.press('Delete');
      await expect(page.getByTestId('ostConfirmDeleteText')).toContainText('“Movable opportunity” and its 1 descendant');
      await api(page, 'DELETE', `/api/tree/nodes/opportunity/${k.op3.split('-')[1]}`, 204, () =>
        page.getByTestId('ostConfirmDeleteConfirm').click(),
      );
      for (const key of [k.op3, k.doomed]) await expect(node(page, key)).toHaveCount(0);
    });

    await test.step('reload: everything persisted', async () => {
      await page.goto(`/trees/${teamId}/canvas?node=${k.op1}`);
      await expect(panel(page)).toBeVisible();
      const expected: [string, string][] = [
        [k.outcome, 'Journey outcome'],
        [k.op1, 'Parent opportunity'],
        [k.op2, 'Nested opportunity renamed'],
        [k.s1, 'Journey solution'],
        [k.a1, 'Journey assumption'],
        [k.ev1, 'Interview snippet'],
        [k.ev2, 'Test result'],
      ];
      for (const [key, text] of expected) await expect(title(page, key)).toHaveText(text);
      await expect(node(page, k.op3)).toHaveCount(0);
      await expect(page.getByTestId('ost-status-exploring')).toHaveAttribute('aria-pressed', 'true');
      await expect(page.getByTestId('ost-value-label')).toHaveText('Modest');
      await expect(page.getByTestId('ost-priority')).toHaveAttribute('aria-valuenow', k.detailPriority);
      await expect(page.getByTestId('ost-notes')).toHaveValue('Typed on the page, then navigated.');
      await expect(page.getByTestId(`ost-node-chat-${k.op1}`)).toHaveText('1');
      await expect(page.getByTestId('ost-tab-badge-questions')).toHaveText('1');

      const nodes = await treeNodes();
      expect(nodes.map(n => n.key).sort()).toEqual([k.p1, k.outcome, k.op1, k.op2, k.s1, k.a1, k.ev1, k.ev2].sort());
      expect(nodes.find(n => n.key === k.a1)).toMatchObject({ status: 'TESTING', confidence: 60, ownerLogin: 'user' });
      expect(nodes.find(n => n.key === k.op1)).toMatchObject({ status: 'EXPLORING', valueRating: 2, priority: Number(k.detailPriority) });
      expect(
        nodes
          .find(n => n.key === k.op1)!
          .questions.map(q => q.done)
          .sort(),
      ).toEqual([false, true]);
    });
  });

  test('on a touch screen the small controls are 44px targets (hit area; the look stays compact)', async ({ browser }) => {
    const context = await browser.newContext({
      storageState: await user.context.storageState(),
      hasTouch: true,
      isMobile: true,
      viewport: { width: 1280, height: 900 },
    });
    const page = await context.newPage();
    watchErrors(page);
    try {
      await page.goto(`/trees/${teamId}/canvas?node=${k.op1}`);
      await expect(panel(page)).toBeVisible();
      expect(await page.evaluate(() => matchMedia('(pointer: coarse)').matches)).toBe(true);
      const atLeast44 = async (locator: Locator) => {
        const b = await box(locator);
        expect(Math.round(b.width), 'width').toBeGreaterThanOrEqual(44);
        expect(Math.round(b.height), 'height').toBeGreaterThanOrEqual(44);
      };
      await api(page, 'GET', /\/comments$/, 200, () => page.getByTestId('ost-tab-chat').click());
      await atLeast44(page.getByTestId(`ost-chat-edit-${k.keptComment}`));
      await atLeast44(page.getByTestId(`ost-chat-delete-${k.keptComment}`));
      await page.getByTestId('ost-tab-questions').click();
      for (const q of (await serverNode(k.op1))!.questions) {
        await atLeast44(page.getByTestId(`ost-question-toggle-${q.id}`));
        await atLeast44(page.getByTestId(`ost-question-remove-${q.id}`));
      }

      // Controls that keep their compact look: a tap 21px from their centre, in each direction, still
      // lands on them (an invisible ::before widens the hit area to 44 x 44px).
      const hitArea44 = async (locator: Locator, name: string) => {
        await expect(locator, name).toBeVisible();
        // Whole hit area on screen (e.g. the restore buttons below the fold of the panel body).
        await locator.evaluate(el => {
          const r = el.getBoundingClientRect();
          if (r.top < 24 || r.bottom > window.innerHeight - 24) el.scrollIntoView({ block: 'center', inline: 'nearest' });
        });
        const misses = await locator.evaluate(el => {
          const r = el.getBoundingClientRect();
          const cx = r.left + r.width / 2;
          const cy = r.top + r.height / 2;
          return [
            [cx - 21, cy],
            [cx + 21, cy],
            [cx, cy - 21],
            [cx, cy + 21],
          ]
            .map(([x, y]) => ({ x, y, hit: document.elementFromPoint(x, y) }))
            .filter(({ hit }) => !hit || !(hit === el || el.contains(hit)))
            .map(
              ({ x, y, hit }) =>
                `${Math.round(x - cx)},${Math.round(y - cy)} → ${hit?.getAttribute('data-cy') ?? hit?.className ?? 'nothing'}`,
            );
        });
        expect(misses, `${name}: taps that miss it`).toEqual([]);
      };
      await page.getByTestId('ost-tab-detail').click();
      await hitArea44(page.getByTestId('ost-tab-detail'), 'panel tab');
      await hitArea44(page.getByTestId('ost-filter-outcome'), 'filter chip');
      expect((await box(page.getByTestId('ost-filter-outcome'))).height, 'filter chip look unchanged').toBeLessThan(44);

      // Top nav and toolbar: view tabs, team combo, product combo.
      for (const cy of ['ostTabTrees', 'ostTabCanvas', 'ostTabExperiments']) await hitArea44(page.getByTestId(cy), `view tab ${cy}`);
      await hitArea44(page.getByTestId('ostTeamComboButton'), 'team combo');
      await hitArea44(page.getByTestId('ost-product-combo'), 'product combo');
      expect((await box(page.getByTestId('ost-product-combo'))).height, 'product combo look unchanged').toBeLessThan(44);

      // Palette hide, then the rail's show button.
      const paletteToggle = page.getByTestId('ost-palette-toggle');
      await hitArea44(paletteToggle, 'palette hide');
      expect((await box(paletteToggle)).width, 'palette hide look unchanged').toBeLessThan(44);
      await paletteToggle.click();
      await expect(page.getByTestId('ost-palette')).toHaveAttribute('data-state', 'closed');
      await hitArea44(paletteToggle, 'palette show');
      await paletteToggle.click();
      await expect(page.getByTestId('ost-palette')).toHaveAttribute('data-state', 'open');

      // Panel: breadcrumb, every status chip (a wrapped second row must not cover the first), every $ step.
      for (const key of [k.p1, k.outcome]) await hitArea44(page.getByTestId(`ost-breadcrumb-${key}`), `panel breadcrumb ${key}`);
      const chips = await page.getByTestId('ost-panel').locator('[data-cy^="ost-status-"]').all();
      expect(chips.length, 'opportunity status chips').toBeGreaterThan(3);
      const chipTops = new Set<number>();
      for (const chip of chips) {
        await hitArea44(chip, `status chip ${await chip.getAttribute('data-cy')}`);
        chipTops.add(Math.round((await box(chip)).y));
      }
      expect(chipTops.size, 'the status chips wrap onto a second row').toBeGreaterThan(1);
      for (let v = 1; v <= 5; v++) await hitArea44(page.getByTestId(`ost-value-${v}`), `value step ${v}`);
      expect((await box(page.getByTestId('ost-value-1'))).height, 'value step look unchanged').toBeLessThan(44);

      // Links tab: the default-link restore buttons.
      await page.getByTestId('ost-tab-links').click();
      const restore = await page.locator('[data-cy^="ost-link-restore-"]').all();
      expect(restore.length, 'restore buttons').toBe(3);
      for (const button of restore) await hitArea44(button, `link restore ${await button.getAttribute('data-cy')}`);
      await page.getByTestId('ost-tab-detail').click();

      // Canvas controls scale with the zoom; at 100% their hit area is 44+ screen px.
      const zoomLevel = async () => Number((await page.getByTestId('ost-zoom-level').textContent())!.replace('%', ''));
      for (let i = 0; i < 10 && (await zoomLevel()) < 100; i++) await page.getByTestId('ost-zoom-in').click();
      expect(await zoomLevel()).toBeGreaterThanOrEqual(100);
      await settle(page);
      await hitArea44(page.getByTestId(`ost-node-add-${k.op1}`), 'node +');
      expect((await box(page.getByTestId(`ost-node-add-${k.op1}`))).width, 'node + look unchanged').toBeLessThan(44);
      await hitArea44(page.getByTestId(`ost-collapse-${k.op1}`), 'collapse chip');
      await hitArea44(page.getByTestId(`ost-node-chat-${k.op1}`), 'chat chip');
      await api(page, 'GET', /\/comments$/, 200, () => page.getByTestId('ost-tab-chat').click());
      await hitArea44(page.getByTestId('ost-chat-send'), 'chat send');

      // Full-page detail: breadcrumb links, status chips and $ steps.
      await page.goto(`/trees/${teamId}/nodes/${k.op1}`);
      await expect(page.getByTestId('ost-node-detail-title')).toHaveValue('Parent opportunity');
      for (const key of [k.p1, k.outcome]) await hitArea44(page.getByTestId(`ost-node-detail-crumb-${key}`), `page breadcrumb ${key}`);
      for (const chip of await page.getByTestId('ost-node-detail').locator('[data-cy^="ost-status-"]').all()) {
        await hitArea44(chip, `page status chip ${await chip.getAttribute('data-cy')}`);
      }
      for (let v = 1; v <= 5; v++) await hitArea44(page.getByTestId(`ost-value-${v}`), `page value step ${v}`);
      expect(takeErrors(page)).toEqual([]);
    } finally {
      await context.close();
    }
  });

  test('a viewer (second session) can look at everything and change nothing', async ({ browser }) => {
    test.setTimeout(150_000);
    // The editor's page is open (loaded as an EDITOR) when the owner demotes them.
    const stale = user.page;
    await stale.goto(`/trees/${teamId}/canvas`);
    await expect(node(stale, k.op2)).toBeVisible();
    await expect(stale.getByTestId(`ost-node-add-${k.op2}`)).toBeVisible();
    const demoted = await admin.api('put', `/api/team-management/teams/${teamId}/members/${userId}`, { role: 'VIEWER' });
    expect(demoted.status()).toBe(200);

    await test.step('the open editor page says the role changed and turns read-only, live (FR-037)', async () => {
      // Since RTC-006 the demotion arrives as a MEMBERSHIP_CHANGED event, so the page turns
      // read-only at once — the user never gets as far as a write the server would refuse (that
      // fallback still guards a page whose socket is down; ost-tree.store.spec.ts covers it).
      await expect(stale.getByTestId('ostError')).toContainText('Your role changed to viewer — changes are no longer possible.');
      await expect(stale.locator('[data-cy^="ost-node-add-"]')).toHaveCount(0);
      await expect(stale.getByTestId('ost-palette')).toHaveCount(0);
      await title(stale, k.op2).dblclick();
      await expect(renameInput(stale)).toHaveCount(0);
      await expect(title(stale, k.op2)).toHaveText('Nested opportunity renamed');
    });

    const viewer = await openSessionAt(browser, USER_USERNAME, USER_PASSWORD);
    const page = viewer.page;
    try {
      const before = await treeNodes();
      let writes = 0;
      page.on('request', r => {
        if (r.url().includes('/api/tree/') && r.method() !== 'GET') writes++;
      });

      await test.step('dashboard and experiments read', async () => {
        await page.goto(`/trees/${teamId}`);
        await expect(page.getByTestId(`ost-product-card-${k.p1}`)).toContainText(P1);
        await page.getByTestId('ostTabExperiments').click();
        await expect(page.getByTestId(`ost-experiment-row-${k.a1}`)).toContainText('Journey assumption');
      });

      await test.step('canvas: no palette, no +, no drag, no rename, no delete', async () => {
        await page.goto(`/trees/${teamId}/canvas`);
        await expect(node(page, k.op1)).toBeVisible();
        await settle(page);
        await expect(page.getByTestId('ost-legend-hint')).toContainText('View only');
        await expect(page.getByTestId('ost-palette')).toHaveCount(0);
        await expect(page.locator('[data-cy^="ost-node-add-"]')).toHaveCount(0);
        await expect(page.locator('.vue-flow__node.draggable')).toHaveCount(0);
        await title(page, k.op2).dblclick();
        await expect(renameInput(page)).toHaveCount(0);
        await node(page, k.op2).focus();
        await page.keyboard.press('F2');
        await expect(renameInput(page)).toHaveCount(0);
        await page.keyboard.press('Delete');
        await expect(page.getByTestId('ostConfirmDelete')).toHaveCount(0);
        await pickUp(page, node(page, k.op2), centre(await box(node(page, k.outcome))));
        await expect(page.locator('[data-drop-target]')).toHaveCount(0);
        await page.mouse.up();
      });

      await test.step('panel: every field read-only; links, questions and chat read-only', async () => {
        await page.goto(`/trees/${teamId}/canvas?node=${k.op1}`);
        await expect(panel(page)).toBeVisible();
        await expect(page.getByTestId('ost-panel-title')).toHaveAttribute('readonly', '');
        await expect(page.getByTestId('ost-status-parked')).toBeDisabled();
        await expect(page.getByTestId('ost-value-1')).toBeDisabled();
        await expect(page.getByTestId('ost-priority')).toHaveAttribute('aria-readonly', 'true');
        await expect(page.getByTestId('ost-notes')).toHaveAttribute('readonly', '');
        await expect(page.locator('[data-cy^="ost-quick-add-"]')).toHaveCount(0);
        await expect(page.getByTestId('ost-delete')).toHaveCount(0);
        await page.getByTestId('ost-tab-links').click();
        await expect(page.getByTestId('ost-link-add')).toHaveCount(0);
        await expect(page.getByTestId('ost-link-remove')).toHaveCount(0);
        await page.getByTestId('ost-tab-questions').click();
        await expect(page.getByTestId('ost-question-add')).toHaveCount(0);
        await expect(page.locator('[data-cy^="ost-question-remove-"]')).toHaveCount(0);
        await expect(page.locator('[data-cy^="ost-question-toggle-"]').first()).toBeDisabled();
        await api(page, 'GET', /\/comments$/, 200, () => page.getByTestId('ost-tab-chat').click());
        await expect(page.getByTestId(`ost-chat-msg-${k.keptComment}`)).toHaveAttribute('data-mine', 'true');
        await expect(page.getByTestId('ost-chat-input')).toBeDisabled();
        await expect(page.getByTestId('ost-chat-readonly')).toBeVisible();
        await expect(page.getByTestId(`ost-chat-edit-${k.keptComment}`)).toHaveCount(0);
        await expect(page.getByTestId(`ost-chat-delete-${k.keptComment}`)).toHaveCount(0);

        await page.goto(`/trees/${teamId}/canvas?node=${k.a1}`);
        await expect(page.getByTestId('ost-confidence-20')).toBeDisabled();
        await expect(page.getByTestId('ost-owner')).toBeDisabled();
      });

      await test.step('full-page detail read-only', async () => {
        await page.goto(`/trees/${teamId}/nodes/${k.op1}`);
        await expect(page.getByTestId('ost-node-detail-title')).toHaveText('Parent opportunity');
        await expect(page.getByTestId('ost-node-detail-delete')).toHaveCount(0);
        await expect(page.locator('[data-cy^="ost-node-detail-add-"]')).toHaveCount(0);
        await expect(page.getByTestId('ost-status-validated')).toBeDisabled();
        await expect(page.getByTestId('ost-notes')).toHaveAttribute('readonly', '');
        await expect(page.getByTestId('ost-node-detail-chat').getByTestId('ost-chat-input')).toBeDisabled();
      });

      await test.step('the server refuses a viewer’s writes too', async () => {
        const opId = Number(k.op1.split('-')[1]);
        expect((await viewer.api('patch', `/api/tree/nodes/opportunity/${opId}`, { title: 'viewer' })).status()).toBe(403);
        expect(
          (await viewer.api('post', '/api/tree/nodes', { type: 'solution', parentType: 'opportunity', parentId: opId })).status(),
        ).toBe(403);
        expect((await viewer.api('post', `/api/tree/nodes/opportunity/${opId}/comments`, { body: 'viewer' })).status()).toBe(403);
      });

      expect(writes, 'no write requests from the viewer UI').toBe(0);
      expect(await treeNodes()).toEqual(before);
      expect(takeErrors(page), 'viewer page / console errors').toEqual([]);
    } finally {
      await viewer.context.close();
    }
  });
});
