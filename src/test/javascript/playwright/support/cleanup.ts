import { appendFileSync, existsSync, mkdirSync, readFileSync, readdirSync, rmSync } from 'node:fs';
import { join } from 'node:path';

/**
 * Throwaway teams are deleted once, after every spec has finished (cleanup.teardown.ts), not in
 * each spec's afterAll: deleting a team while other workers are still running makes the backend
 * fail unrelated requests of that team's members (TeamAccessService.currentUserMemberships loads
 * the caller's memberships, then each team; a team deleted in between is an ObjectNotFoundException
 * → HTTP 500 — this broke TEAMS-008's "add member" in parallel runs).
 *
 * Specs register the teams they create; the registry is a directory of per-worker files, so a run
 * that stops early is cleaned up by the next one.
 */
const REGISTRY = join('target', 'playwright', 'cleanup');

/** Records a team this run created, for deletion (with its products) at the end of the run. */
export function registerTeamForCleanup(teamId: number): void {
  mkdirSync(REGISTRY, { recursive: true });
  appendFileSync(join(REGISTRY, `worker-${process.pid}.txt`), `${teamId}\n`);
}

/** The registered team ids and a function that forgets them (once they are deleted). */
export function registeredTeams(): { ids: number[]; forget: () => void } {
  if (!existsSync(REGISTRY)) return { ids: [], forget: () => undefined };
  const files = readdirSync(REGISTRY).map(name => join(REGISTRY, name));
  const ids = files
    .flatMap(file => readFileSync(file, 'utf8').split('\n'))
    .map(Number)
    .filter(id => Number.isInteger(id) && id > 0);
  return {
    ids: [...new Set(ids)],
    forget: () => files.forEach(file => rmSync(file, { force: true })),
  };
}
