import { expect, test } from '@playwright/test';

import { USER_PASSWORD, USER_USERNAME, openSession } from './support/session';

test.describe('sidebar navigation', () => {
  test('is shown when signed in', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByTestId('sidebar')).toBeVisible();
    await expect(page.getByTestId('logout')).toBeVisible();
  });

  test('collapses and expands, remembering the choice', async ({ page }) => {
    await page.goto('/');
    const sidebar = page.getByTestId('sidebar');
    const toggle = page.getByTestId('sidebarToggle');

    // Start from a known state.
    if (!(await sidebar.evaluate(el => el.classList.contains('active')))) {
      await toggle.click();
    }
    await expect(sidebar).toHaveClass(/active/);

    await toggle.click();
    await expect(sidebar).not.toHaveClass(/active/);

    await page.reload();
    await expect(page.getByTestId('sidebar')).not.toHaveClass(/active/);

    await page.getByTestId('sidebarToggle').click();
    await expect(page.getByTestId('sidebar')).toHaveClass(/active/);
  });

  test('navigates to an entity page and marks it active', async ({ page }) => {
    await page.goto('/');
    const sidebar = page.getByTestId('sidebar');
    if (!(await sidebar.evaluate(el => el.classList.contains('active')))) {
      await page.getByTestId('sidebarToggle').click();
    }

    await sidebar.getByRole('link', { name: 'Opportunities' }).click();
    await expect(page).toHaveURL(/\/opportunity$/);
    await expect(page.getByTestId('OpportunityHeading')).toBeVisible();

    const activeItem = sidebar.locator('li.active');
    await expect(activeItem).toContainText('Opportunities');
  });

  test('shows the administration group for an admin', async ({ page }) => {
    await page.goto('/');
    const sidebar = page.getByTestId('sidebar');
    if (!(await sidebar.evaluate(el => el.classList.contains('active')))) {
      await page.getByTestId('sidebarToggle').click();
    }
    await expect(sidebar.getByTestId('adminMenu')).toBeVisible();
    await sidebar.getByTestId('adminMenu').click();
    await sidebar.getByRole('link', { name: 'Health' }).click();
    await expect(page).toHaveURL(/\/admin\/health$/);
  });

  test('lists Trees above Teams and shows Static Data to an admin', async ({ page }) => {
    await page.goto('/');
    const sidebar = page.getByTestId('sidebar');
    await expect(sidebar.locator('.nav-item', { hasText: 'Static Data' })).toBeVisible();
    const items = (await sidebar.locator('.nav-item').allTextContents()).map(t => t.trim());
    expect(items.indexOf('Trees')).toBeGreaterThan(-1);
    expect(items.indexOf('Trees')).toBeLessThan(items.indexOf('Teams'));
    expect(items).toContain('Static Data');
    expect(items).not.toContain('Tree');
    // The former Discovery group now lives inside Static Data.
    expect(items).not.toContain('Discovery');
    expect(items).toEqual(expect.arrayContaining(['Opportunities', 'Assumptions', 'Tags']));
  });

  test('hides Static Data from a user who is not an admin', async ({ browser }) => {
    const session = await openSession(browser, USER_USERNAME, USER_PASSWORD);
    try {
      await session.page.goto('/');
      const sidebar = session.page.getByTestId('sidebar');
      await expect(sidebar.getByTestId('treesMenu')).toBeVisible();
      await expect(sidebar.locator('.nav-item', { hasText: 'Discovery' })).toHaveCount(0);
      await expect(sidebar.locator('.nav-item', { hasText: 'Assumptions' })).toHaveCount(0);
      await expect(sidebar.locator('.nav-item', { hasText: 'Static Data' })).toHaveCount(0);
    } finally {
      await session.context.close();
    }
  });
});
