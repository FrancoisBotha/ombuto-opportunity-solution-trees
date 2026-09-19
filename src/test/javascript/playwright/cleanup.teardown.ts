import { expect, test as teardown } from '@playwright/test';

import { registeredTeams } from './support/cleanup';

/** Seed data (ost-seed.spec.ts): refused even if a spec registered one by mistake. */
const SEEDED = new Set(['Team Jupiter', 'Team Venus', 'Best Team', 'Team Mars']);

interface AdminTeam {
  id: number;
  name: string;
  productCount: number;
}

/**
 * Runs once after all specs (the `cleanup` project, teardown of `setup`), signed in as admin
 * (setup's stored session). Deletes every team the specs registered with registerTeamForCleanup:
 * first each product (DELETE /api/products/{id}, which removes the product's whole subtree), then
 * the team (DELETE /api/admin/teams/{id}, refused while products remain). Seeded teams are never
 * registered, and never touched here.
 */
teardown('delete the throwaway teams created by the specs', async ({ request }) => {
  const { ids, forget } = registeredTeams();
  if (!ids.length) return;

  const cookies = (await request.storageState()).cookies;
  const xsrf = cookies.find(cookie => cookie.name === 'XSRF-TOKEN')?.value ?? '';
  const headers = { 'X-XSRF-TOKEN': xsrf };

  const listed = await request.get('/api/admin/teams?size=5000');
  expect(listed.ok(), `list teams as admin: HTTP ${listed.status()}`).toBe(true);
  const existing = new Map(((await listed.json()) as AdminTeam[]).map(team => [team.id, team]));

  const failures: string[] = [];
  for (const id of ids) {
    const team = existing.get(id);
    if (!team) continue; // already gone
    if (SEEDED.has(team.name)) {
      failures.push(`${team.name} (${id}) is seed data and was not deleted`);
      continue;
    }
    if (team.productCount > 0) {
      const products = await request.get(`/api/teams/${id}/products`);
      if (!products.ok()) {
        failures.push(`list products of ${team.name} (${id}): HTTP ${products.status()}`);
        continue;
      }
      for (const product of (await products.json()) as { id: number }[]) {
        const deleted = await request.delete(`/api/products/${product.id}`, { headers });
        if (deleted.status() !== 204) failures.push(`product ${product.id} of ${team.name}: HTTP ${deleted.status()}`);
      }
    }
    const deleted = await request.delete(`/api/admin/teams/${id}`, { headers });
    if (![200, 204].includes(deleted.status())) failures.push(`${team.name} (${id}): HTTP ${deleted.status()} ${await deleted.text()}`);
  }
  expect(failures, 'throwaway teams left behind').toEqual([]);
  forget();
});
