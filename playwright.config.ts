import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright end-to-end tests.
 *
 * Expects the app to be running already:
 *   docker compose -f src/main/docker/keycloak.yml up -d
 *   ./mvnw                      (backend on :8080)
 *   npm start                   (vite dev server on :9000)
 *
 * Override the target with E2E_BASE_URL, and the login with E2E_USERNAME / E2E_PASSWORD.
 */
export const BASE_URL = process.env.E2E_BASE_URL ?? 'http://localhost:9000';
export const STORAGE_STATE = 'target/playwright/.auth/user.json';

export default defineConfig({
  testDir: 'src/test/javascript/playwright',
  outputDir: 'target/playwright/test-results',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [['list'], ['html', { outputFolder: 'target/playwright/report', open: 'never' }]],
  timeout: 30_000,
  expect: { timeout: 10_000 },
  use: {
    baseURL: BASE_URL,
    // JHipster marks elements with data-cy; make getByTestId use it.
    testIdAttribute: 'data-cy',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    // Logs in once through Keycloak and stores the session for the other projects.
    { name: 'setup', testMatch: /auth\.setup\.ts/, teardown: 'cleanup' },
    // After every spec has finished: deletes the throwaway teams they registered (support/cleanup.ts).
    { name: 'cleanup', testMatch: /cleanup\.teardown\.ts/, use: { storageState: STORAGE_STATE } },
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'], storageState: STORAGE_STATE },
      dependencies: ['setup'],
      testIgnore: /anonymous\.spec\.ts/,
    },
    // Tests that must run without a session (sign-in page etc.).
    {
      name: 'chromium-anonymous',
      use: { ...devices['Desktop Chrome'] },
      testMatch: /anonymous\.spec\.ts/,
    },
  ],
});
