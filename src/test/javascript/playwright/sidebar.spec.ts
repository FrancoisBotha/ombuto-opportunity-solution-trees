import { expect, test } from '@playwright/test';

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
});
