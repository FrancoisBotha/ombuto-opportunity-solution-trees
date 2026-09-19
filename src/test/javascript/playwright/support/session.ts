import { type APIResponse, type Browser, type BrowserContext, type Page } from '@playwright/test';

import { BASE_URL } from '../../../../../playwright.config';

import { loginViaKeycloak } from './login';

export const ADMIN_USERNAME = process.env.E2E_ADMIN_USERNAME ?? 'admin';
export const ADMIN_PASSWORD = process.env.E2E_ADMIN_PASSWORD ?? 'admin';
export const USER_USERNAME = process.env.E2E_USER_USERNAME ?? 'user';
export const USER_PASSWORD = process.env.E2E_USER_PASSWORD ?? 'user';

type Verb = 'get' | 'post' | 'put' | 'patch' | 'delete';

export interface Session {
  context: BrowserContext;
  page: Page;
  /** Calls the backend with this user's session cookie and CSRF token. */
  api: (verb: Verb, url: string, data?: unknown, contentType?: string) => Promise<APIResponse>;
}

/**
 * Signs a user in through Keycloak in a browser context of their own, so two
 * users can be driven side by side without sharing cookies.
 */
export async function openSession(browser: Browser, username: string, password: string): Promise<Session> {
  const context = await browser.newContext({ storageState: { cookies: [], origins: [] } });
  const page = await context.newPage();
  await loginViaKeycloak(page, username, password);

  const api: Session['api'] = async (verb, url, data, contentType) => {
    // Spring's CookieCsrfTokenRepository: mutating calls must echo the XSRF-TOKEN cookie.
    const cookies = await context.cookies(BASE_URL);
    const xsrf = cookies.find(cookie => cookie.name === 'XSRF-TOKEN')?.value ?? '';
    const headers: Record<string, string> = { 'X-XSRF-TOKEN': xsrf };
    if (contentType) headers['Content-Type'] = contentType;
    return context.request[verb](url, { data, headers });
  };

  return { context, page, api };
}
