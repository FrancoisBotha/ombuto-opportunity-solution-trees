import { type Page, expect, test } from '@playwright/test';

import { ADMIN_PASSWORD, ADMIN_USERNAME, type Session, openSession } from './support/session';

/**
 * TREE-008 — end-to-end coverage of the build-a-tree flow from epic 2 §9.
 *
 * Signs in through Keycloak, walks the sidebar to the team's tree, then builds,
 * edits, deletes, focuses and reloads it. Uses a fresh team + admin session so
 * repeated runs do not fight each other, and cleans the team up on exit.
 */

const stamp = Date.now();
const teamName = `Tree ${stamp}`;
const productA = `Alpha ${stamp}`;
const productB = `Bravo ${stamp}`;
const outcomeTitle = `Outcome ${stamp}`;
const opp1Title = `Opp L1 ${stamp}`;
const opp2Title = `Opp L2 ${stamp}`;
const opp3Title = `Opp L3 ${stamp}`;
const solutionTitle = `Solution ${stamp}`;
const throwawayOutcomeTitle = `Doomed ${stamp}`;
const throwawayOppTitle = `Doomed child ${stamp}`;
const throwawaySolutionTitle = `Doomed grandchild ${stamp}`;
const editedOpportunityTitle = `Edited L1 ${stamp}`;
const editedOpportunityStatus = 'PRIORITISED';

let owner: Session;
let teamId: number;

async function createTeam(session: Session): Promise<number> {
  const resp = await session.api('post', '/api/team-management/teams', { name: teamName, description: 'TREE-008 e2e' }, 'application/json');
  expect(resp.ok(), `create team -> HTTP ${resp.status()}`).toBe(true);
  const body = await resp.json();
  expect(typeof body.id).toBe('number');
  return body.id as number;
}

async function deleteTeam(session: Session, id: number): Promise<void> {
  await session.api('delete', `/api/admin/teams/${id}`);
}

async function openTreeThroughSidebar(page: Page, id: number): Promise<string[]> {
  // Track the tree-load requests that fire between navigation and network idle.
  const treeUrl = `/api/teams/${id}/tree`;
  const loads: string[] = [];
  const listener = (req: { url: () => string; method: () => string }) => {
    if (req.method() === 'GET' && req.url().includes(treeUrl)) loads.push(req.url());
  };
  page.on('request', listener);

  await page.goto('/');
  await expect(page.getByTestId('sidebar')).toBeVisible();
  await page.getByTestId('treesMenu').click();
  await expect(page.getByTestId('TreesHeading')).toBeVisible();

  await page.getByTestId(`treeCard-${id}`).click();
  await expect(page.getByTestId('treeEditorShell')).toBeVisible();
  await expect(page.getByTestId('treeEditorTeamName')).toHaveText(teamName);

  // Wait for the initial tree fetch to settle before we snapshot the count.
  await page.waitForLoadState('networkidle');
  page.off('request', listener);
  return loads;
}

async function addProduct(page: Page, name: string): Promise<void> {
  const empty = page.getByTestId('treeEditorAddFirstProduct');
  const toolbar = page.getByTestId('treeEditorAddProduct');
  if (await empty.isVisible().catch(() => false)) {
    await empty.click();
  } else {
    await toolbar.click();
  }
  await expect(page.getByTestId('treeEditorAddProductModal')).toBeVisible();
  await page.getByTestId('addProductName').fill(name);
  await page.getByTestId('addProductDescription').fill('e2e');
  await page.getByTestId('addProductSubmit').click();
  await expect(page.getByTestId('treeEditorAddProductModal')).toHaveCount(0);
  await expect(productCard(page, name)).toBeVisible();
}

function productCard(page: Page, name: string) {
  return page.locator('[data-cy^="treeNode-product-"]').filter({ hasText: name });
}

function nodeCard(page: Page, type: 'outcome' | 'opportunity' | 'solution', title: string) {
  return page.locator(`[data-cy^="treeNode-${type}-"]`).filter({ hasText: title });
}

async function nodeIdOf(card: ReturnType<typeof productCard>): Promise<number> {
  const dataCy = await card.first().getAttribute('data-cy');
  const id = Number((dataCy ?? '').split('-').pop());
  expect(Number.isFinite(id) && id > 0, `parse id from ${dataCy}`).toBe(true);
  return id;
}

async function addChild(
  page: Page,
  parentType: 'product' | 'outcome' | 'opportunity',
  parentId: number,
  childType: 'outcome' | 'opportunity' | 'solution',
  title: string,
): Promise<void> {
  // Guard: no lingering modals from previous interactions.
  await expect(page.getByTestId('treeEditorAddChildModal')).toHaveCount(0);
  await expect(page.getByTestId('treeEditorAddProductModal')).toHaveCount(0);

  // Reveal the hover-only add-child affordance by selecting the parent card first.
  const parent = page.getByTestId(`treeNode-${parentType}-${parentId}`);
  await parent.scrollIntoViewIfNeeded();
  await parent.click();

  const addBtn = page.getByTestId(`treeNodeAddChild-${parentType}-${parentId}-${childType}`);
  await addBtn.waitFor({ state: 'visible' });
  await addBtn.click();

  await expect(page.getByTestId('treeEditorAddChildModal')).toBeVisible();
  await page.getByTestId('addChildTitle').fill(title);
  await page.getByTestId('addChildDescription').fill('e2e');
  await page.getByTestId('addChildSubmit').click();
  await expect(page.getByTestId('treeEditorAddChildModal')).toHaveCount(0);
  await expect(nodeCard(page, childType, title)).toBeVisible();
}

test.describe('TREE-008 — build-a-tree end to end', () => {
  test.setTimeout(180_000);

  test.beforeAll(async ({ browser }) => {
    owner = await openSession(browser, ADMIN_USERNAME, ADMIN_PASSWORD);
    teamId = await createTeam(owner);
  });

  test.afterAll(async () => {
    if (owner) {
      if (teamId) await deleteTeam(owner, teamId);
      await owner.context.close();
    }
  });

  test('builds, edits, focuses, deletes and persists a full tree', async () => {
    const page = owner.page;

    // 1. Sign in is done by beforeAll (openSession); open the tree via the sidebar
    //    and assert the initial load fired exactly one GET /api/teams/{id}/tree.
    const initialLoads = await openTreeThroughSidebar(page, teamId);
    expect(initialLoads, `expected one tree load, got ${initialLoads.length}`).toHaveLength(1);

    // 2. Add two products, then build one full branch under product A.
    await expect(page.getByTestId('treeEditorEmpty')).toBeVisible();
    await addProduct(page, productA);
    await addProduct(page, productB);
    await expect(productCard(page, productA)).toBeVisible();
    await expect(productCard(page, productB)).toBeVisible();

    const productAId = await nodeIdOf(productCard(page, productA));

    await addChild(page, 'product', productAId, 'outcome', outcomeTitle);
    const outcomeId = await nodeIdOf(nodeCard(page, 'outcome', outcomeTitle));

    await addChild(page, 'outcome', outcomeId, 'opportunity', opp1Title);
    const opp1Id = await nodeIdOf(nodeCard(page, 'opportunity', opp1Title));

    await addChild(page, 'opportunity', opp1Id, 'opportunity', opp2Title);
    const opp2Id = await nodeIdOf(nodeCard(page, 'opportunity', opp2Title));

    await addChild(page, 'opportunity', opp2Id, 'opportunity', opp3Title);
    const opp3Id = await nodeIdOf(nodeCard(page, 'opportunity', opp3Title));

    await addChild(page, 'opportunity', opp3Id, 'solution', solutionTitle);
    await expect(nodeCard(page, 'solution', solutionTitle)).toBeVisible();

    // 3. Edit a node's title and status through the detail panel; the card must
    //    reflect both changes.
    await page.getByTestId(`treeNode-opportunity-${opp1Id}`).click();
    const detail = page.getByTestId('treeDetailPanel');
    await expect(detail).toBeVisible();
    await detail.getByTestId('detailTitle').fill(editedOpportunityTitle);
    await detail.getByTestId('detailStatus').selectOption(editedOpportunityStatus);
    await detail.getByTestId('detailSave').click();

    const editedCard = page.getByTestId(`treeNode-opportunity-${opp1Id}`);
    await expect(editedCard.getByTestId('treeNodeTitle')).toHaveText(editedOpportunityTitle);
    await expect(editedCard.getByTestId('treeNodeStatus')).toContainText(editedOpportunityStatus);

    // 4. Delete a node with descendants; the confirmation must state how many
    //    descendants will disappear, and the whole subtree must be gone after.
    await addChild(page, 'product', productAId, 'outcome', throwawayOutcomeTitle);
    const throwawayOutcomeId = await nodeIdOf(nodeCard(page, 'outcome', throwawayOutcomeTitle));
    await addChild(page, 'outcome', throwawayOutcomeId, 'opportunity', throwawayOppTitle);
    const throwawayOppId = await nodeIdOf(nodeCard(page, 'opportunity', throwawayOppTitle));
    await addChild(page, 'opportunity', throwawayOppId, 'solution', throwawaySolutionTitle);

    await page.getByTestId(`treeNode-outcome-${throwawayOutcomeId}`).click();
    await page.getByTestId(`treeNodeDelete-outcome-${throwawayOutcomeId}`).click();
    await expect(page.getByTestId('treeEditorDeleteModal')).toBeVisible();
    await expect(page.getByTestId('deleteConfirmCount')).toHaveText('2');
    await expect(page.getByTestId('deleteConfirmMessage')).toContainText(throwawayOutcomeTitle);
    await page.getByTestId('deleteConfirm').click();
    await expect(page.getByTestId('treeEditorDeleteModal')).toHaveCount(0);
    await expect(nodeCard(page, 'outcome', throwawayOutcomeTitle)).toHaveCount(0);
    await expect(nodeCard(page, 'opportunity', throwawayOppTitle)).toHaveCount(0);
    await expect(nodeCard(page, 'solution', throwawaySolutionTitle)).toHaveCount(0);

    // 5. Focus product A: product B's card should vanish; clearing focus brings
    //    it back.
    await page.getByTestId('treeEditorFocusSelect').selectOption(String(productAId));
    await expect(productCard(page, productB)).toHaveCount(0);
    await expect(productCard(page, productA)).toBeVisible();
    await page.getByTestId('treeEditorClearFocus').click();
    await expect(productCard(page, productB)).toBeVisible();
    await expect(productCard(page, productA)).toBeVisible();

    // 6. Reload the page and assert the built tree survives: both products, the
    //    three-level opportunity nesting, the edited title and its status.
    await page.reload();
    await expect(page.getByTestId('treeEditorShell')).toBeVisible();
    await expect(productCard(page, productA)).toBeVisible();
    await expect(productCard(page, productB)).toBeVisible();
    await expect(nodeCard(page, 'outcome', outcomeTitle)).toBeVisible();
    await expect(nodeCard(page, 'opportunity', editedOpportunityTitle)).toBeVisible();
    await expect(nodeCard(page, 'opportunity', opp2Title)).toBeVisible();
    await expect(nodeCard(page, 'opportunity', opp3Title)).toBeVisible();
    await expect(nodeCard(page, 'solution', solutionTitle)).toBeVisible();
    const persistedEditedCard = page.getByTestId(`treeNode-opportunity-${opp1Id}`);
    await expect(persistedEditedCard.getByTestId('treeNodeTitle')).toHaveText(editedOpportunityTitle);
    await expect(persistedEditedCard.getByTestId('treeNodeStatus')).toContainText(editedOpportunityStatus);
  });
});
