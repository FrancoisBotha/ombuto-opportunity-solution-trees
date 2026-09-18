import { expect, test } from '@playwright/test';

test.describe('anonymous visitor', () => {
  test('sees the sign-in button and no sidebar', async ({ page }) => {
    await page.goto('/');
    await expect(page.getByTestId('login')).toBeVisible();
    await expect(page.getByTestId('sidebar')).toHaveCount(0);
  });

  test('sign in redirects to Keycloak', async ({ page }) => {
    await page.goto('/');
    await page.getByTestId('login').click();
    await page.waitForURL(/\/realms\/.*\/protocol\/openid-connect\/auth/);
    await expect(page.locator('#kc-login')).toBeVisible();
  });
});
