import { expect, test } from '@playwright/test';

import { ADMIN_PASSWORD, ADMIN_USERNAME, type Session, USER_PASSWORD, USER_USERNAME, openSession } from './support/session';

/**
 * OST step 2: the dev seed (DevDataSeeder) as seen through the running app.
 *
 * Expects a dev backend started on a database seeded by DevDataSeeder
 * (application.seed.enabled=true). This is the only spec that reads the seeded
 * teams; it never changes them.
 *
 * Other specs create throw-away teams and may add `user` to them, so the
 * "exactly" checks are made over the four seeded team names.
 */

const JUPITER = 'Team Jupiter';
const VENUS = 'Team Venus';
const BEST = 'Best Team';
const MARS = 'Team Mars';
const SEEDED = [JUPITER, VENUS, BEST, MARS];

interface MyTeam {
  id: number;
  name: string;
  role: string;
}

interface AdminTeam {
  id: number;
  name: string;
  ownerLogins: string[];
}

interface Member {
  login: string;
  role: string;
}

test.describe.configure({ mode: 'serial' });

test.describe('OST dev seed', () => {
  test.setTimeout(90_000);

  let user: Session;
  let admin: Session;

  test.beforeAll(async ({ browser }) => {
    user = await openSession(browser, USER_USERNAME, USER_PASSWORD);
    admin = await openSession(browser, ADMIN_USERNAME, ADMIN_PASSWORD);
  });

  test.afterAll(async () => {
    await user?.context.close();
    await admin?.context.close();
  });

  test('user belongs to exactly Team Jupiter and Team Venus, as owner', async () => {
    const response = await user.api('get', '/api/team-management/my-teams');
    expect(response.ok()).toBe(true);
    const seeded = ((await response.json()) as MyTeam[]).filter(team => SEEDED.includes(team.name));

    expect(seeded.map(team => team.name).sort()).toEqual([JUPITER, VENUS]);
    for (const team of seeded) {
      expect(team.role, team.name).toBe('OWNER');
    }
  });

  test('/trees lists Team Jupiter and Team Venus for user, not the admin-only teams', async () => {
    await user.page.goto('/trees');
    const list = user.page.getByTestId('treesList');
    await expect(list).toBeVisible();

    const cards = list.locator('[data-cy^="treeCard-"]');
    const titles = (await list.locator('.card-title').allInnerTexts()).map(title => title.trim());
    expect(titles.filter(title => SEEDED.includes(title)).sort()).toEqual([JUPITER, VENUS]);
    await expect(cards.filter({ hasText: BEST })).toHaveCount(0);
    await expect(cards.filter({ hasText: MARS })).toHaveCount(0);
  });

  test('admin sees the four seeded teams with the right roles', async () => {
    const response = await admin.api('get', '/api/admin/teams?size=1000');
    expect(response.ok()).toBe(true);
    const teams = (await response.json()) as AdminTeam[];

    const byName = new Map<string, AdminTeam>();
    for (const name of SEEDED) {
      const matches = teams.filter(team => team.name === name);
      expect(matches, `teams named '${name}'`).toHaveLength(1);
      byName.set(name, matches[0]);
    }

    const expectedMembers: Record<string, Record<string, string>> = {
      [JUPITER]: { [USER_USERNAME]: 'OWNER', [ADMIN_USERNAME]: 'VIEWER' },
      [VENUS]: { [USER_USERNAME]: 'OWNER', [ADMIN_USERNAME]: 'VIEWER' },
      [BEST]: { [ADMIN_USERNAME]: 'OWNER' },
      [MARS]: { [ADMIN_USERNAME]: 'OWNER' },
    };
    for (const [name, expected] of Object.entries(expectedMembers)) {
      const team = byName.get(name)!;
      const ownerLogin = Object.keys(expected).find(login => expected[login] === 'OWNER');
      expect(team.ownerLogins, `${name} owners`).toEqual([ownerLogin]);

      const membersResponse = await admin.api('get', `/api/admin/teams/${team.id}/members`);
      expect(membersResponse.ok()).toBe(true);
      const members = (await membersResponse.json()) as Member[];
      expect(Object.fromEntries(members.map(member => [member.login, member.role])), `${name} members`).toEqual(expected);
    }
  });

  test('Team Jupiter holds the two prototype products; the other teams are empty', async () => {
    const teams = (await (await admin.api('get', '/api/admin/teams?size=1000')).json()) as (AdminTeam & { productCount: number })[];
    const jupiter = teams.find(team => team.name === JUPITER)!;
    expect(jupiter.productCount).toBe(2);
    for (const name of [VENUS, BEST, MARS]) {
      expect(teams.find(team => team.name === name)!.productCount, name).toBe(0);
    }

    // /api/products lists the caller's teams' products (admin is a Jupiter viewer); filter to Team Jupiter.
    const response = await admin.api('get', '/api/products?size=1000');
    expect(response.ok()).toBe(true);
    const products = (await response.json()) as { name: string; team?: { id: number } }[];
    const names = products.filter(product => product.team?.id === jupiter.id).map(product => product.name);
    expect(names.sort()).toEqual(['Discovery Canvas', 'Insight Library']);
  });
});
