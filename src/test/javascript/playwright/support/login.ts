import { type Page, expect } from '@playwright/test';

export const E2E_USERNAME = process.env.E2E_USERNAME ?? 'admin';
export const E2E_PASSWORD = process.env.E2E_PASSWORD ?? 'admin';

/**
 * Signs in through the OAuth2 redirect to Keycloak and waits until the app
 * reports an authenticated session.
 */
export async function loginViaKeycloak(page: Page, username = E2E_USERNAME, password = E2E_PASSWORD): Promise<void> {
  await page.goto('/');
  await page.getByTestId('login').click();

  // Keycloak's default login theme.
  await page.locator('#username').fill(username);
  await page.locator('#password').fill(password);
  await page.locator('#kc-login').click();

  await page.waitForURL(url => !url.pathname.includes('/realms/'));
  await expect(page.getByTestId('sidebar')).toBeVisible();
}
