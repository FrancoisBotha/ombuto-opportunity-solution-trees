import { existsSync, readFileSync, readdirSync } from 'node:fs';
import { dirname, join } from 'node:path';

import { describe, expect, it } from 'vitest';

/**
 * Every package the lazily loaded OST module imports must be pre-bundled at dev-server start
 * (vite.config.ts optimizeDeps.include). Otherwise Vite discovers it on the first visit, re-optimises
 * and full-reloads every open page — in the e2e suite that landed while "Open branch" was loading
 * the canvas and sent the page back to /trees/:id.
 */

/** Packages the eager app bundle already imports (main.ts, router): Vite finds these at start-up. */
const EAGER = new Set(['vue', 'vue-router', 'pinia', 'axios']);

function projectRoot() {
  let dir = process.cwd();
  while (!existsSync(join(dir, 'vite.config.ts')) && dirname(dir) !== dir) dir = dirname(dir);
  return dir;
}

function sources(dir: string): string[] {
  return readdirSync(dir, { withFileTypes: true }).flatMap(entry => {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) return sources(path);
    return /\.(vue|ts)$/.test(entry.name) && !/\.spec\.ts$|test-util\.ts$/.test(entry.name) ? [path] : [];
  });
}

/** Bare JS imports (not CSS side-effect imports), reduced to the package name. */
function importedPackages(code: string): string[] {
  const specifiers = [...code.matchAll(/\bfrom\s+'([^'.@/][^']*|@[^/'][^']*)'/g)].map(m => m[1]);
  return specifiers
    .filter(s => !s.startsWith('@/') && !s.endsWith('.css'))
    .map(s =>
      s
        .split('/')
        .slice(0, s.startsWith('@') ? 2 : 1)
        .join('/'),
    );
}

describe('vite optimizeDeps', () => {
  const root = projectRoot();
  const config = readFileSync(join(root, 'vite.config.ts'), 'utf8');
  const includeList = /optimizeDeps:\s*\{[\s\S]*?include:\s*\[([^\]]*)\]/.exec(config)?.[1] ?? '';
  const included = new Set([...includeList.matchAll(/'([^']+)'/g)].map(m => m[1]));

  it('pre-bundles every package the lazy OST module imports', () => {
    const ostDir = join(root, 'src/main/webapp/app/ost');
    const packages = new Set(sources(ostDir).flatMap(file => importedPackages(readFileSync(file, 'utf8'))));
    expect(packages.size).toBeGreaterThan(0);
    const missing = [...packages].filter(p => !EAGER.has(p) && !included.has(p)).sort();
    expect(missing, 'add these to optimizeDeps.include in vite.config.ts').toEqual([]);
  });

  it('reads the include list', () => {
    expect(included.has('@vue-flow/core')).toBe(true);
  });
});
