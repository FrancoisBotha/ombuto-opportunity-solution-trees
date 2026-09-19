import { mkdirSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

import { type Page, expect, test } from '@playwright/test';

import { registerTeamForCleanup } from './support/cleanup';
import { type Session, USER_PASSWORD, USER_USERNAME, openSession } from './support/session';

/**
 * OST step 13b (C21): performance smoke on a 300+ node tree. Measures, on a throwaway team:
 * - the first canvas render (navigation → nodes on screen → layout settled);
 * - wheel-zoom frame times (one wheel notch per animation frame, out to 35% and back in);
 * - what opening / closing an inline rename and a + menu costs. Both keep the edited node rendered
 *   however far it is panned or zoomed away (TreeCanvas.vue, `keepRendered`), and must stay cheap.
 *
 * The numbers are written to target/playwright/ost-perf.json and attached to the test result; the
 * assertions are generous ceilings (a smoke test, not a benchmark) so the run stays deterministic.
 * The team, with its product, is left to the run-end cleanup (support/cleanup.ts). `admin` is a
 * VIEWER of it, which exercises the cleanup deleting products as a member whose role allows it.
 */

interface TreeNode {
  key: string;
  id: number;
}

const stamp = Date.now();
const results: Record<string, unknown> = {};

const stats = (values: number[]) => {
  const sorted = [...values].sort((a, b) => a - b);
  const pick = (q: number) => sorted[Math.min(sorted.length - 1, Math.floor(q * sorted.length))];
  const round = (v: number) => Math.round(v * 10) / 10;
  return {
    frames: values.length,
    avg: round(values.reduce((s, v) => s + v, 0) / values.length),
    p50: round(pick(0.5)),
    p95: round(pick(0.95)),
    max: round(sorted[sorted.length - 1]),
  };
};

/** Waits until the viewport and every node's transform stop changing. */
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
      { intervals: [150], timeout: 20_000 },
    )
    .toBe(true);
}

const rendered = (page: Page) => page.locator('.vue-flow__node').count();

/**
 * In the page: dispatches the event that opens / closes the rename field or + menu of `key`, then
 * waits until the DOM shows it and for that frame to be painted (two more animation frames).
 * Returns the elapsed ms and the longest long task meanwhile.
 */
async function timeInteraction(page: Page, action: 'rename' | 'rename-close' | 'menu' | 'menu-close', key: string) {
  return page.evaluate(
    async ({ action, key }) => {
      const node = document.querySelector<HTMLElement>(`.ost-node[data-node-key="${key}"]`)!;
      const longTasks: number[] = [];
      const observer = new PerformanceObserver(list => list.getEntries().forEach(e => longTasks.push(e.duration)));
      try {
        observer.observe({ type: 'longtask', buffered: false });
      } catch {
        /* long tasks unsupported */
      }
      const input = () => document.querySelector('[data-cy="ost-rename-input"]');
      const menu = () => document.querySelector('[data-cy="ost-add-menu"]');
      const frame = () => new Promise(resolve => requestAnimationFrame(resolve));
      const t0 = performance.now();
      let done: () => boolean;
      if (action === 'rename') {
        node.querySelector('[data-cy="ost-node-title"]')!.dispatchEvent(new MouseEvent('dblclick', { bubbles: true, cancelable: true }));
        done = () => !!input();
      } else if (action === 'rename-close') {
        input()!.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true, cancelable: true }));
        done = () => !input();
      } else if (action === 'menu') {
        (document.querySelector(`[data-cy="ost-node-add-${key}"]`) as HTMLElement).click();
        done = () => !!menu();
      } else {
        menu()!.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true, cancelable: true }));
        done = () => !menu();
      }
      while (!done()) await frame();
      await frame();
      await frame();
      const ms = performance.now() - t0;
      observer.disconnect();
      return { ms: Math.round(ms * 10) / 10, longestTask: Math.round(Math.max(0, ...longTasks)) };
    },
    { action, key },
  );
}

test.describe.configure({ mode: 'serial' });

test.describe('OST performance smoke (300+ nodes)', () => {
  test.setTimeout(240_000);

  let user: Session;
  let teamId: number;
  let productId: number;
  const keys: string[] = [];
  let deepKey = '';
  const errors: string[] = [];

  async function mk(type: string, parentType: string, parentId: number, title: string): Promise<TreeNode> {
    const res = await user.api('post', '/api/tree/nodes', { type, parentType, parentId, title });
    expect(res.status(), `create ${type} "${title}"`).toBe(201);
    const created = (await res.json()) as TreeNode;
    keys.push(created.key);
    return created;
  }

  /** Creates `count` children of each parent, a few requests at a time (the server serialises per team). */
  async function level(parents: TreeNode[], type: string, parentType: string, count: number, label: string) {
    const jobs = parents.flatMap((p, i) =>
      Array.from({ length: count }, (_, j) => () => mk(type, parentType, p.id, `${label} ${i + 1}.${j + 1}`)),
    );
    const out: TreeNode[] = [];
    for (let i = 0; i < jobs.length; i += 6) out.push(...(await Promise.all(jobs.slice(i, i + 6).map(job => job()))));
    return out;
  }

  test.beforeAll(async ({ browser }) => {
    test.setTimeout(240_000);
    user = await openSession(browser, USER_USERNAME, USER_PASSWORD);
    await user.page.setViewportSize({ width: 1600, height: 1000 });
    user.page.on('pageerror', e => errors.push(`pageerror: ${e.message}`));
    user.page.on('console', m => {
      if (m.type() === 'error') errors.push(`console.error: ${m.text()}`);
    });

    const team = await user.api('post', '/api/team-management/teams', { name: `e2e perf ${stamp}`, description: 'ost-perf' });
    expect(team.status()).toBe(201);
    teamId = (await team.json()).id;
    registerTeamForCleanup(teamId);
    // admin joins as a VIEWER: it may list the team's products but not delete them, so the run-end
    // cleanup must delete this team's products as `user` (the OWNER) — see cleanup.teardown.ts.
    const found = (await (await user.api('get', `/api/team-management/teams/${teamId}/user-search?q=admin`)).json()) as {
      id: string;
      login: string;
    }[];
    const adminId = found.find(u => u.login === 'admin')!.id;
    expect((await user.api('post', `/api/team-management/teams/${teamId}/members`, { userId: adminId, role: 'VIEWER' })).status()).toBe(
      201,
    );
    const product = await user.api('post', '/api/products', {
      name: `Perf product ${stamp}`,
      description: null,
      archived: false,
      createdDate: new Date().toISOString(),
      team: { id: teamId },
    });
    expect(product.status()).toBe(201);
    productId = (await product.json()).id;
    keys.push(`product-${productId}`);

    // 1 product + 5 outcomes + 30 opportunities + 90 solutions + 180 assumptions = 306 nodes.
    const outcomes = await level([{ key: `product-${productId}`, id: productId }], 'outcome', 'product', 5, 'Outcome');
    const opportunities = await level(outcomes, 'opportunity', 'outcome', 6, 'Opportunity');
    const solutions = await level(opportunities, 'solution', 'opportunity', 3, 'Solution');
    const assumptions = await level(solutions, 'assumption', 'solution', 2, 'Assumption');
    deepKey = assumptions[assumptions.length - 1].key;
    expect(keys.length).toBeGreaterThanOrEqual(300);
  });

  test.afterEach(() => {
    expect(errors.splice(0), 'page / console errors').toEqual([]);
  });

  test.afterAll(async () => {
    // The product (306 nodes) is left for the run-end cleanup on purpose (see beforeAll).
    await user?.context.close();
    mkdirSync(join('target', 'playwright'), { recursive: true });
    writeFileSync(join('target', 'playwright', 'ost-perf.json'), `${JSON.stringify(results, null, 2)}\n`);
  });

  test('first canvas render of a 306-node tree', async () => {
    const page = user.page;
    // Warm the lazy OST chunks once, so the measurement is the render, not the dev server's first compile.
    await page.goto(`/trees/${teamId}`);
    await expect(page.getByTestId('ostDashboardPage')).toBeVisible();

    const t0 = Date.now();
    const treeRead = page.waitForResponse(r => r.url().endsWith(`/api/teams/${teamId}/tree`) && r.request().method() === 'GET');
    await page.goto(`/trees/${teamId}/canvas`);
    const treeMs = Date.now() - t0;
    await treeRead;
    const loaded = Date.now() - t0;
    await expect(page.getByTestId(`ost-node-product-${productId}`)).toBeVisible();
    const firstNodes = Date.now() - t0;
    await settle(page);
    const settled = Date.now() - t0;
    const count = await rendered(page);
    results.initialRender = {
      nodes: keys.length,
      renderedNodes: count,
      navigationMs: treeMs,
      treeLoadedMs: loaded,
      firstNodeVisibleMs: firstNodes,
      settledMs: settled,
      zoom: await page.getByTestId('ost-zoom-level').textContent(),
    };
    test.info().annotations.push({ type: 'initialRender', description: JSON.stringify(results.initialRender) });
    // Visibility culling: far fewer than all nodes are in the DOM at the fitted zoom.
    expect(count).toBeLessThan(keys.length);
    expect(firstNodes).toBeLessThan(10_000);
  });

  test('wheel zoom frame times (one notch per frame, out to 35% and back in)', async () => {
    const page = user.page;
    await page.goto(`/trees/${teamId}/canvas`);
    await expect(page.getByTestId(`ost-node-product-${productId}`)).toBeVisible();
    await settle(page);
    const run = (deltaY: number, notches: number) =>
      page.evaluate(
        async ({ deltaY, notches }) => {
          const canvas = document.querySelector<HTMLElement>('[data-cy="ost-canvas"]')!;
          const r = canvas.getBoundingClientRect();
          const frame = () => new Promise<number>(resolve => requestAnimationFrame(resolve));
          const times: number[] = [];
          let last = await frame();
          for (let i = 0; i < notches; i++) {
            canvas.dispatchEvent(
              new WheelEvent('wheel', {
                deltaY,
                clientX: r.left + r.width / 2,
                clientY: r.top + r.height / 2,
                bubbles: true,
                cancelable: true,
              }),
            );
            const now = await frame();
            times.push(now - last);
            last = now;
          }
          return { times, rendered: document.querySelectorAll('.vue-flow__node').length };
        },
        { deltaY, notches },
      );
    const out = await run(100, 30);
    await expect(page.getByTestId('ost-zoom-level')).toHaveText('35%');
    const back = await run(-100, 30);
    results.wheelZoom = {
      zoomOut: { ...stats(out.times), renderedAt35: out.rendered },
      zoomIn: { ...stats(back.times), renderedAtEnd: back.rendered },
    };
    test.info().annotations.push({ type: 'wheelZoom', description: JSON.stringify(results.wheelZoom) });
    expect(stats([...out.times, ...back.times]).p50).toBeLessThan(250);
  });

  test('a rename or + menu keeps culling on for every other node (cheap) and survives its node leaving the view', async () => {
    const page = user.page;
    await page.goto(`/trees/${teamId}/canvas?node=${deepKey}`);
    await expect(page.getByTestId(`ost-node-${deepKey}`)).toBeVisible();
    await settle(page);
    const before = await rendered(page);

    const rename = await timeInteraction(page, 'rename', deepKey);
    const duringRename = await rendered(page);
    const input = page.getByTestId('ost-rename-input');
    await expect(input).toBeFocused();

    // Zoom in about the far corner until the node is out of view: its field (and draft) stays.
    await input.fill('Draft kept off-screen');
    await page.evaluate(key => {
      const canvas = document.querySelector<HTMLElement>('[data-cy="ost-canvas"]')!;
      const r = canvas.getBoundingClientRect();
      const n = document.querySelector<HTMLElement>(`.ost-node[data-node-key="${key}"]`)!.getBoundingClientRect();
      const x = n.left + n.width / 2 > r.left + r.width / 2 ? r.left + 20 : r.right - 20;
      for (let i = 0; i < 24; i++)
        canvas.dispatchEvent(new WheelEvent('wheel', { deltaY: -100, clientX: x, clientY: r.top + 20, bubbles: true, cancelable: true }));
    }, deepKey);
    await expect(page.getByTestId('ost-zoom-level')).toHaveText('160%');
    await expect
      .poll(() =>
        page.evaluate(key => {
          const r = document.querySelector('[data-cy="ost-canvas"]')!.getBoundingClientRect();
          const n = document.querySelector(`.ost-node[data-node-key="${key}"]`)!.getBoundingClientRect();
          return n.right < r.left || n.left > r.right || n.bottom < r.top || n.top > r.bottom;
        }, deepKey),
      )
      .toBe(true);
    await expect(input).toHaveValue('Draft kept off-screen');
    expect(await rendered(page)).toBeLessThan(keys.length / 4);
    const renameClose = await timeInteraction(page, 'rename-close', deepKey);
    await expect(input).toHaveCount(0);
    await expect(page.getByTestId(`ost-node-${deepKey}`)).toHaveCount(0); // culled again once the edit ends
    await page.goto(`/trees/${teamId}/canvas?node=${deepKey}`);
    await expect(page.getByTestId(`ost-node-${deepKey}`)).toBeVisible();
    await settle(page);

    const menu = await timeInteraction(page, 'menu', deepKey);
    const duringMenu = await rendered(page);
    await expect(page.getByTestId('ost-add-menu')).toBeVisible();
    const menuClose = await timeInteraction(page, 'menu-close', deepKey);
    await expect(page.getByTestId('ost-add-menu')).toHaveCount(0);

    results.editOpen = {
      renderedBefore: before,
      rename,
      renameClose,
      renderedDuringRename: duringRename,
      menu,
      menuClose,
      renderedDuringMenu: duringMenu,
    };
    test.info().annotations.push({ type: 'editOpen', description: JSON.stringify(results.editOpen) });

    // Only the edited node joins the rendered set (culling stays on for the rest).
    expect(duringRename).toBeLessThanOrEqual(before + 1);
    expect(duringMenu).toBeLessThanOrEqual(before + 1);
    expect(rename.ms).toBeLessThan(1_000);
    expect(menu.ms).toBeLessThan(1_000);
  });
});
