import { type APIResponse, expect, test } from '@playwright/test';

import { BASE_URL } from '../../../../playwright.config';

import { registerTeamForCleanup } from './support/cleanup';
import { ADMIN_PASSWORD, ADMIN_USERNAME, type Session, USER_PASSWORD, USER_USERNAME, openSession } from './support/session';

/**
 * Epic 1 (TEAMS-001..007) integration test: team-scoped access control, checked
 * against the real backend, database and Keycloak.
 *
 * The dev realm has two accounts, so `admin` owns the team and `user` is walked
 * through every membership state in turn: non-member -> viewer -> editor ->
 * removed. Each state is asserted at the API (what the server allows) and, where
 * a role changes what the page offers, in the UI.
 *
 * The scenarios build on each other, so the file runs serially and stops at the
 * first failure. Every team it creates (and their products) is deleted after the
 * run (support/cleanup.ts).
 */

const DENIED = [403, 404]; // NFR-002: "not a member" and "does not exist" look the same.

function expectDenied(response: APIResponse, what: string): void {
  expect(DENIED, `${what} -> HTTP ${response.status()}`).toContain(response.status());
}

function expectOk(response: APIResponse, what: string): void {
  expect(response.ok(), `${what} -> HTTP ${response.status()}`).toBe(true);
}

test.describe.configure({ mode: 'serial' });

test.describe('Epic 1 — team-scoped access control', () => {
  test.setTimeout(120_000);

  const stamp = Date.now();
  const teamName = `Access ${stamp}`;

  let owner: Session;
  let user: Session;
  let teamId: number;
  let userId: string;
  let ownerUserId: string;
  let productId: number;

  const team = () => `/api/team-management/teams/${teamId}`;
  const member = () => `${team()}/members/${userId}`;
  const newProduct = (name: string) => ({
    name,
    description: 'teams-access e2e',
    archived: false,
    createdDate: new Date().toISOString(),
    team: { id: teamId },
  });

  test.beforeAll(async ({ browser }) => {
    // `user` signs in first: the app only knows a user (and the member picker can
    // only find them) once they have logged in.
    user = await openSession(browser, USER_USERNAME, USER_PASSWORD);
    owner = await openSession(browser, ADMIN_USERNAME, ADMIN_PASSWORD);
  });

  test.afterAll(async () => {
    await owner?.context.close();
    await user?.context.close();
  });

  test('creating a team makes the creator its owner', async () => {
    const created = await owner.api('post', '/api/team-management/teams', { name: teamName, description: 'Access-control e2e' });
    expect(created.status()).toBe(201);
    const body = await created.json();
    teamId = body.id;
    registerTeamForCleanup(teamId);
    expect(body.role).toBe('OWNER');
    expect(body.memberCount).toBe(1);

    const myTeams = await (await owner.api('get', '/api/team-management/my-teams')).json();
    expect(myTeams.find((entry: { id: number }) => entry.id === teamId)?.role).toBe('OWNER');

    const members = await (await owner.api('get', `${team()}/members`)).json();
    expect(members).toHaveLength(1);
    expect(members[0].login).toBe(ADMIN_USERNAME);
    ownerUserId = members[0].userId;
  });

  test('server sets createdDate itself and ignores a client-supplied one', async () => {
    const forged = await owner.api('post', '/api/team-management/teams', {
      name: `Forged ${stamp}`,
      description: 'client tries to backdate',
      createdDate: '2001-01-01T00:00:00Z',
    });
    expect(forged.status()).toBe(201);
    registerTeamForCleanup((await forged.json()).id);
    const createdDate = new Date((await forged.json()).createdDate).getTime();
    expect(Math.abs(Date.now() - createdDate)).toBeLessThan(5 * 60_000);
  });

  test('non-member can neither see nor touch the team', async () => {
    const myTeams = await (await user.api('get', '/api/team-management/my-teams')).json();
    expect(myTeams.some((entry: { id: number }) => entry.id === teamId)).toBe(false);

    expectDenied(await user.api('get', team()), 'non-member reads team');
    expectDenied(await user.api('get', `${team()}/members`), 'non-member lists members');
    expectDenied(await user.api('get', `/api/teams/${teamId}/products`), 'non-member lists products');
    expectDenied(await user.api('put', team(), { name: 'hijacked', description: 'x' }), 'non-member renames team');
    expectDenied(await user.api('post', '/api/products', newProduct('Outsider product')), 'non-member creates product');
  });

  test('non-member cannot add themselves to the team', async () => {
    const search = await owner.api('get', `${team()}/user-search?q=${USER_USERNAME}`);
    expectOk(search, 'owner searches users');
    const match = (await search.json()).find((entry: { login: string }) => entry.login === USER_USERNAME);
    expect(match, `'${USER_USERNAME}' must be findable once they have logged in`).toBeTruthy();
    userId = match.id;

    expectDenied(await user.api('post', `${team()}/members`, { userId, role: 'OWNER' }), 'non-member self-enrols');
  });

  test('generated CRUD endpoints are closed to non-admin users', async () => {
    for (const path of [
      'teams',
      'team-members',
      'outcomes',
      'opportunities',
      'solutions',
      'assumptions',
      'evidences',
      'comments',
      'node-links',
      'open-questions',
      'node-histories',
      'tags',
    ]) {
      const response = await user.api('get', `/api/${path}`);
      expect(response.status(), `GET /api/${path} as a plain user`).toBe(403);
    }
  });

  test('owner adds the user as viewer; a duplicate add is rejected', async () => {
    const added = await owner.api('post', `${team()}/members`, { userId, role: 'VIEWER' });
    expect(added.status()).toBe(201);
    expect((await added.json()).role).toBe('VIEWER');

    const again = await owner.api('post', `${team()}/members`, { userId, role: 'EDITOR' });
    expect(again.status(), 'adding the same user twice').toBe(400);
  });

  test('viewer can read everything and change nothing', async () => {
    expectOk(await user.api('get', team()), 'viewer reads team');
    expectOk(await user.api('get', `${team()}/members`), 'viewer lists members');
    expectOk(await user.api('get', `/api/teams/${teamId}/products`), 'viewer lists products');

    expectDenied(await user.api('put', team(), { name: 'viewer rename', description: 'x' }), 'viewer renames team');
    expectDenied(await user.api('post', '/api/products', newProduct('Viewer product')), 'viewer creates product');
    expectDenied(await user.api('put', member(), { role: 'OWNER' }), 'viewer promotes themselves');
    expectDenied(await user.api('delete', `${team()}/members/${ownerUserId}`), 'viewer removes the owner');
  });

  test('promoting the viewer to editor unlocks product controls in the UI, not member controls', async () => {
    expectOk(await owner.api('put', member(), { role: 'EDITOR' }), 'owner promotes user to editor');

    await user.page.goto(`/teams/${teamId}`);
    await expect(user.page.getByTestId('teamName')).toHaveText(teamName);
    await expect(user.page.getByTestId('teamRole')).toHaveText(/EDITOR/i);
    await expect(user.page.getByTestId('newProductButton')).toBeVisible();
    await expect(user.page.getByTestId('addMemberButton')).toHaveCount(0);
    await expect(user.page.locator('[data-cy^="removeMemberButton-"]')).toHaveCount(0);
  });

  test('editor can create and archive products but cannot manage members', async () => {
    const created = await user.api('post', '/api/products', newProduct(`Editor product ${stamp}`));
    expect(created.status()).toBe(201);
    productId = (await created.json()).id;

    const archived = await user.api(
      'patch',
      `/api/products/${productId}`,
      { id: productId, archived: true },
      'application/merge-patch+json',
    );
    expectOk(archived, 'editor archives product');

    // Archived products stay listed, flagged as archived (TEAMS-003).
    const listed = await (await owner.api('get', `/api/teams/${teamId}/products`)).json();
    expect(listed.find((entry: { id: number }) => entry.id === productId)?.archived).toBe(true);

    expectDenied(await user.api('put', member(), { role: 'OWNER' }), 'editor promotes themselves');
    // An authorised caller would get 400 'memberexists' here, so a 403/404 proves the role check ran.
    expectDenied(await user.api('post', `${team()}/members`, { userId: ownerUserId, role: 'VIEWER' }), 'editor adds a member');
    expectDenied(await user.api('delete', `${team()}/members/${ownerUserId}`), 'editor removes the owner');
  });

  test("/api/products only lists products from the caller's teams", async () => {
    const mine = await (await user.api('get', '/api/products?size=200')).json();
    expect(mine.some((entry: { id: number }) => entry.id === productId)).toBe(true);

    const memberTeams = new Set(
      (await (await user.api('get', '/api/team-management/my-teams')).json()).map((entry: { id: number }) => entry.id),
    );
    const leaked = mine.filter((entry: { team?: { id: number } }) => entry.team && !memberTeams.has(entry.team.id));
    expect(leaked, 'products from teams the user does not belong to').toEqual([]);
  });

  test('a team must keep at least one owner', async () => {
    const demote = await owner.api('put', `${team()}/members/${ownerUserId}`, { role: 'VIEWER' });
    expect(demote.status(), 'last owner demotes themselves').toBe(400);

    const leave = await owner.api('delete', `${team()}/members/${ownerUserId}`);
    expect(leave.status(), 'last owner removes themselves').toBe(400);
  });

  test('removing a member revokes access immediately, in the API and the UI', async () => {
    expect((await owner.api('delete', member())).status()).toBe(204);

    expectDenied(await user.api('get', team()), 'removed member reads team');
    expectDenied(await user.api('get', `/api/products/${productId}`), 'removed member reads a product they created');

    await user.page.goto('/teams');
    await expect(user.page.getByTestId('MyTeamsHeading')).toBeVisible();
    await expect(user.page.locator(`[data-cy="teamCard-${teamId}"]`)).toHaveCount(0);

    await user.page.goto(`/teams/${teamId}`);
    await expect(user.page.getByTestId('teamForbidden').or(user.page.getByTestId('teamNotFound'))).toBeVisible();
  });

  test('ROLE_ADMIN gets no implicit access to a team it is not a member of', async () => {
    const created = await user.api('post', '/api/team-management/teams', {
      name: `User-owned ${stamp}`,
      description: 'admin is not a member',
    });
    expect(created.status()).toBe(201);
    const userTeamId = (await created.json()).id;
    registerTeamForCleanup(userTeamId);

    expectDenied(await owner.api('get', `/api/team-management/teams/${userTeamId}`), 'admin reads a team they are not in');
    expectDenied(await owner.api('get', `/api/teams/${userTeamId}/products`), "admin lists that team's products");

    const adminTeams = await (await owner.api('get', '/api/team-management/my-teams')).json();
    expect(adminTeams.some((entry: { id: number }) => entry.id === userTeamId)).toBe(false);
  });

  test('anonymous callers are rejected outright', async ({ playwright }) => {
    // Explicitly cookie-less: request contexts otherwise inherit the project's signed-in storageState.
    const anonymous = await playwright.request.newContext({ baseURL: BASE_URL, storageState: { cookies: [], origins: [] } });
    try {
      expect((await anonymous.get('/api/team-management/my-teams')).status()).toBe(401);
      expect((await anonymous.get(`/api/teams/${teamId}/products`)).status()).toBe(401);
    } finally {
      await anonymous.dispose();
    }
  });
});
