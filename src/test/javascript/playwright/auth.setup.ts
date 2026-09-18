import { test as setup } from '@playwright/test';

import { STORAGE_STATE } from '../../../../playwright.config';

import { loginViaKeycloak } from './support/login';

setup('authenticate', async ({ page }) => {
  await loginViaKeycloak(page);
  await page.context().storageState({ path: STORAGE_STATE });
});
