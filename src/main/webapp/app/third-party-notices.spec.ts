import { existsSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';

import { describe, expect, it } from 'vitest';

/**
 * Guards THIRD-PARTY-NOTICES.md against drifting behind package.json: every runtime dependency
 * must be listed (regenerate with `node scripts/generate-third-party-notices.cjs`).
 */
function projectRoot() {
  let dir = process.cwd();
  while (!existsSync(join(dir, 'THIRD-PARTY-NOTICES.md')) && dirname(dir) !== dir) dir = dirname(dir);
  return dir;
}
const read = (relative: string) => readFileSync(join(projectRoot(), relative), 'utf8');

describe('THIRD-PARTY-NOTICES.md', () => {
  const notices = read('THIRD-PARTY-NOTICES.md');
  const pkg = JSON.parse(read('package.json')) as { dependencies: Record<string, string> };

  it('lists every production dependency as a direct package at its declared version', () => {
    const missing = Object.entries(pkg.dependencies).filter(([name, version]) => {
      const escaped = name.replace(/[.*+?^${}()|[\]\\/]/g, '\\$&');
      const row = new RegExp(
        `^\\| \\[?${escaped}\\]?(\\([^)]*\\))?\\s*\\| ${version.replace(/^[~^]/, '').replace(/\./g, '\\.')}\\s*\\|.*\\| direct \\|$`,
        'm',
      );
      return !row.test(notices);
    });
    expect(missing).toEqual([]);
  });

  it('carries the OFL-1.1 notice for the bundled Inter font', () => {
    expect(notices).toMatch(/### Inter — font under the SIL Open Font License 1\.1/);
    expect(notices).toMatch(/The Inter Project Authors/);
  });
});
