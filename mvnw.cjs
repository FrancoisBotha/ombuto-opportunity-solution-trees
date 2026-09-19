#!/usr/bin/env node
/**
 * Cross-platform launcher for the Maven wrapper, used by the npm scripts.
 *
 * npm runs scripts through cmd.exe on Windows, where `./mvnw` fails with
 * "'.' is not recognized as an internal or external command". This picks
 * mvnw.cmd on Windows and ./mvnw everywhere else, and passes every argument on.
 */
const { spawnSync } = require('node:child_process');
const path = require('node:path');

const isWindows = process.platform === 'win32';
const args = process.argv.slice(2);

const result = isWindows
  ? spawnSync(
      // cmd.exe splits unquoted arguments on , ; = and spaces, so quote anything that is not plainly safe.
      [`"${path.join(__dirname, 'mvnw.cmd')}"`, ...args.map(a => (/^[\w./:\\-]+$/.test(a) ? a : `"${a.replace(/"/g, '""')}"`))].join(' '),
      { stdio: 'inherit', shell: true, cwd: __dirname },
    )
  : spawnSync(path.join(__dirname, 'mvnw'), args, { stdio: 'inherit', cwd: __dirname });

if (result.error) {
  console.error(result.error.message);
  process.exit(1);
}
process.exit(result.status ?? 1);
