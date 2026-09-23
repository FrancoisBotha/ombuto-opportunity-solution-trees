import { expect, test } from '@playwright/test';

import { helpTopics } from '../../../main/webapp/app/help/help-topics';

import { registerTeamForCleanup } from './support/cleanup';
import { ADMIN_PASSWORD, ADMIN_USERNAME, type Session, USER_PASSWORD, USER_USERNAME, openSession } from './support/session';

/**
 * HELPGD closeout: end-to-end walkthrough of BDD UC 'View the Help Guide'
 * (docs/BDD Use Cases/bdd_VIEW_HELP_GUIDE.md).
 *
 * Reads the seeded Team Jupiter tree, focuses the canvas on one seeded product and never
 * changes it. Scenario 3 needs a viewer-role session, so this spec creates one throwaway
 * team (owned by `user`, with `admin` added as VIEWER) and registers it for cleanup.
 */

interface MyTeam {
  id: number;
  name: string;
}

interface TreeNode {
  key: string;
  type: string;
  id: number;
  parentKey: string | null;
}

const JUPITER = 'Team Jupiter';
const stamp = Date.now();

test.describe.configure({ mode: 'serial' });

test.describe('help guide', () => {
  test.setTimeout(90_000);

  let user: Session;
  let viewer: Session;
  let jupiter: MyTeam;
  let firstProductKey: string;
  let viewerTeamId: number;

  test.beforeAll(async ({ browser }) => {
    user = await openSession(browser, USER_USERNAME, USER_PASSWORD);
    viewer = await openSession(browser, ADMIN_USERNAME, ADMIN_PASSWORD);

    const teams = (await (await user.api('get', '/api/team-management/my-teams')).json()) as MyTeam[];
    jupiter = teams.find(t => t.name === JUPITER)!;
    expect(jupiter, JUPITER).toBeTruthy();

    const tree = (await (await user.api('get', `/api/teams/${jupiter.id}/tree`)).json()) as { nodes: TreeNode[] };
    const firstProduct = tree.nodes.find(n => n.type.toLowerCase() === 'product');
    expect(firstProduct, 'seeded Team Jupiter has at least one product').toBeTruthy();
    firstProductKey = firstProduct!.key;

    // Scenario 3 needs a viewer-role user. Create a throwaway team with `user` as owner and
    // add `admin` as VIEWER, mirroring the pattern in ost-canvas-view.spec.ts.
    const team = await user.api('post', '/api/team-management/teams', {
      name: `HELPGD-003 viewer ${stamp}`,
      description: 'help-guide viewer scenario',
    });
    expect(team.status()).toBe(201);
    viewerTeamId = (await team.json()).id;
    registerTeamForCleanup(viewerTeamId);
    const found = (await (await user.api('get', `/api/team-management/teams/${viewerTeamId}/user-search?q=admin`)).json()) as {
      id: string;
      login: string;
    }[];
    const adminId = found.find(u => u.login === 'admin')!.id;
    expect(
      (await user.api('post', `/api/team-management/teams/${viewerTeamId}/members`, { userId: adminId, role: 'VIEWER' })).status(),
    ).toBe(201);
  });

  test.afterAll(async () => {
    await user?.context.close();
    await viewer?.context.close();
  });

  test('Scenario 1+2+5: open Help from the tree canvas, open Logging interviews, go back, search, close', async () => {
    const page = user.page;
    await page.goto(`/trees/${jupiter.id}/canvas`);
    await expect(page.getByTestId('ost-canvas')).toBeVisible();

    // Focus the canvas on the first seeded product (Scenario 4 precondition — used to check
    // that closing Help does not lose the focused product).
    await page.getByTestId('ost-product-combo').click();
    await page.getByTestId(`ost-product-option-${firstProductKey}`).click();
    await expect(page).toHaveURL(new RegExp(`product=${firstProductKey}`));
    const urlBefore = page.url();
    const productLabelBefore = await page.getByTestId('ost-product-combo-label').textContent();

    // Scenario 1 — open Help from the navbar and see the topic list.
    await page.getByTestId('helpLink').click();
    const panel = page.getByTestId('helpPanel');
    await expect(panel).toBeVisible();
    const items = page.getByTestId('helpTopicItem');
    await expect(items).toHaveCount(helpTopics.length);
    for (const topic of helpTopics) {
      await expect(panel).toContainText(topic.title);
    }

    // Scenario 2 — click "Logging interviews", read the steps, and go back to the list.
    await page.getByRole('button', { name: /Logging interviews/ }).click();
    const stepList = page.getByTestId('helpTopicSteps');
    await expect(stepList).toBeVisible();
    const loggingInterviews = helpTopics.find(t => t.title === 'Logging interviews')!;
    const stepLis = stepList.locator('li');
    await expect(stepLis).toHaveCount(loggingInterviews.steps.length);
    for (let i = 0; i < loggingInterviews.steps.length; i++) {
      await expect(stepLis.nth(i)).toHaveText(loggingInterviews.steps[i]);
    }
    await expect(page.getByTestId('helpRoleNote')).toContainText(loggingInterviews.roleNote!);

    await page.getByTestId('helpBack').click();
    await expect(page.getByTestId('helpTopicSteps')).toHaveCount(0);
    await expect(page.getByTestId('helpTopicItem')).toHaveCount(helpTopics.length);

    // Scenario 5 — search for "status" narrows the list; a non-matching term shows the empty state.
    const searchBox = page.getByTestId('helpSearch');
    await searchBox.fill('status');
    const matching = helpTopics.filter(t => [t.title, t.summary, ...t.steps].join(' ').toLowerCase().includes('status'));
    expect(matching.length, 'seeded topics include at least one matching "status"').toBeGreaterThan(0);
    await expect(page.getByTestId('helpTopicItem')).toHaveCount(matching.length);
    await expect(page.getByTestId('helpEmpty')).toHaveCount(0);

    await searchBox.fill('zzznothingmatchesthisxyz');
    await expect(page.getByTestId('helpTopicItem')).toHaveCount(0);
    await expect(page.getByTestId('helpEmpty')).toContainText('No matching topics');

    // Scenario 4 — close the panel, and confirm the URL and focused product are unchanged.
    await page.getByTestId('helpClose').click();
    await expect(page.getByTestId('helpPanel')).toHaveCount(0);
    expect(page.url()).toBe(urlBefore);
    await expect(page.getByTestId('ost-product-combo-label')).toHaveText(productLabelBefore!);
    await expect(page.getByTestId('ost-canvas')).toBeVisible();
  });

  test('Scenario 3: a viewer sees the same topics and every tree-changing topic states the role requirement', async () => {
    const page = viewer.page;
    // Land on the throwaway team where `admin` is a VIEWER.
    await page.goto(`/trees/${viewerTeamId}`);
    await expect(page.getByTestId('helpLink')).toBeVisible();
    await page.getByTestId('helpLink').click();
    const panel = page.getByTestId('helpPanel');
    await expect(panel).toBeVisible();

    // Same topics as every other user.
    await expect(page.getByTestId('helpTopicItem')).toHaveCount(helpTopics.length);

    // Every tree-changing topic states an editor / owner requirement in its role note.
    for (const topic of helpTopics.filter(t => t.changesTree)) {
      await page.getByRole('button', { name: new RegExp(topic.title.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')) }).click();
      const note = page.getByTestId('helpRoleNote');
      await expect(note, `${topic.title} shows a role note`).toBeVisible();
      await expect(note, `${topic.title} note mentions editor or owner`).toContainText(/editor|owner/i);
      await page.getByTestId('helpBack').click();
    }

    await page.getByTestId('helpClose').click();
    await expect(page.getByTestId('helpPanel')).toHaveCount(0);
  });
});
