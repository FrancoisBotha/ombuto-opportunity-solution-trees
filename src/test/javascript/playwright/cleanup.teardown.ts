import { type APIRequestContext, type APIResponse, expect, test as teardown } from '@playwright/test';

import { registeredTeams } from './support/cleanup';
import { ADMIN_PASSWORD, ADMIN_USERNAME, type Session, USER_PASSWORD, USER_USERNAME, openSession } from './support/session';

/** Seed data (ost-seed.spec.ts): refused even if a spec registered one by mistake. */
const SEEDED = new Set(['Team Jupiter', 'Team Venus', 'Best Team', 'Team Mars']);

/** Test users whose credentials the suite knows; a team's products are deleted as one of them. */
const KNOWN_USERS: Record<string, string> = { [ADMIN_USERNAME]: ADMIN_PASSWORD, [USER_USERNAME]: USER_PASSWORD };

interface AdminTeam {
  id: number;
  name: string;
  productCount: number;
}

interface AdminMember {
  login: string;
  role: 'OWNER' | 'EDITOR' | 'VIEWER';
}

type Api = (verb: 'get' | 'delete', url: string) => Promise<APIResponse>;

/**
 * Runs once after all specs (the `cleanup` project, teardown of `setup`), signed in as admin
 * (setup's stored session). Deletes every team the specs registered with registerTeamForCleanup:
 * first each product (DELETE /api/products/{id}, which removes the product's whole subtree), then
 * the team (DELETE /api/admin/teams/{id}, refused while products remain). Seeded teams are never
 * registered, and never touched here.
 *
 * Products can only be deleted by an OWNER or EDITOR of their team (any member may list them), and
 * the admin is not always one: the products are deleted as a member whose role allows it (found
 * through the admin members API, signed in with the suite's known credentials). Whatever cannot be
 * deleted is reported per team, with the reason.
 */
teardown('delete the throwaway teams created by the specs', async ({ request, browser }) => {
  teardown.setTimeout(120_000);
  const { ids, forget } = registeredTeams();
  if (!ids.length) return;

  const cookies = (await request.storageState()).cookies;
  const xsrf = cookies.find(cookie => cookie.name === 'XSRF-TOKEN')?.value ?? '';
  const headers = { 'X-XSRF-TOKEN': xsrf };
  const asAdmin: Api = (verb, url) => (request as APIRequestContext)[verb](url, verb === 'delete' ? { headers } : {});

  const listed = await request.get('/api/admin/teams?size=5000');
  expect(listed.ok(), `list teams as admin: HTTP ${listed.status()}`).toBe(true);
  const existing = new Map(((await listed.json()) as AdminTeam[]).map(team => [team.id, team]));

  /** Sessions of other known users, opened on demand and closed at the end. */
  const sessions = new Map<string, Session>();
  const sessionFor = async (login: string): Promise<Api | null> => {
    if (login === ADMIN_USERNAME) return asAdmin;
    const password = KNOWN_USERS[login];
    if (!password) return null;
    let session = sessions.get(login);
    if (!session) {
      session = await openSession(browser, login, password);
      sessions.set(login, session);
    }
    return session.api;
  };

  /**
   * Lists the team's products as a member who may also DELETE them: an OWNER or EDITOR (owners
   * first) we can sign in as — the admin only when that is its role. Any member may list products,
   * so "admin can list them" is not enough: a VIEWER admin gets 403 on the delete. When the members
   * cannot be listed, the admin is tried as a last resort.
   */
  async function productApi(team: AdminTeam): Promise<{ api: Api; products: { id: number }[] } | string> {
    const tried: string[] = [];
    const members = await asAdmin('get', `/api/admin/teams/${team.id}/members`);
    const candidates = members.ok()
      ? ((await members.json()) as AdminMember[])
          .filter(m => m.role !== 'VIEWER')
          .sort((a, b) => Number(b.role === 'OWNER') - Number(a.role === 'OWNER'))
          .map(m => m.login)
      : [ADMIN_USERNAME];
    if (!members.ok()) tried.push(`members not listed (HTTP ${members.status()})`);
    for (const login of candidates) {
      const api = await sessionFor(login);
      if (!api) {
        tried.push(`${login}: no known credentials`);
        continue;
      }
      const list = await api('get', `/api/teams/${team.id}/products`);
      if (list.ok()) return { api, products: await list.json() };
      tried.push(`${login}: HTTP ${list.status()}`);
    }
    return `no OWNER/EDITOR could list its products (${tried.join(', ') || 'no editors'})`;
  }

  const failures: string[] = [];
  try {
    for (const id of ids) {
      const team = existing.get(id);
      if (!team) continue; // already gone
      const label = `${team.name} (${id})`;
      if (SEEDED.has(team.name)) {
        failures.push(`${label} is seed data and was not deleted`);
        continue;
      }
      if (team.productCount > 0) {
        const found = await productApi(team);
        if (typeof found === 'string') {
          failures.push(`${label}: ${team.productCount} product(s) left — ${found}`);
          continue;
        }
        for (const product of found.products) {
          const deleted = await found.api('delete', `/api/products/${product.id}`);
          if (deleted.status() !== 204) failures.push(`${label}: product ${product.id} not deleted — HTTP ${deleted.status()}`);
        }
      }
      const deleted = await request.delete(`/api/admin/teams/${id}`, { headers });
      if (![200, 204].includes(deleted.status()))
        failures.push(`${label}: team not deleted — HTTP ${deleted.status()} ${await deleted.text()}`);
    }
  } finally {
    for (const session of sessions.values()) await session.context.close();
  }
  expect(failures, 'throwaway teams left behind').toEqual([]);
  forget();
});
