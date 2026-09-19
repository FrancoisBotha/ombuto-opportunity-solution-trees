const childProcess = require('node:child_process');
const { syncBuiltinESMExports } = require('node:module');
const path = require('node:path');
const { pathToFileURL } = require('node:url');

childProcess.exec = (_command, options, callback) => {
  const done = typeof options === 'function' ? options : callback;
  queueMicrotask(() => done(new Error('Child processes are unavailable in this test sandbox.'), '', ''));
  return {};
};
syncBuiltinESMExports();

const vitestCli = path.join(path.dirname(require.resolve('vitest/package.json')), 'vitest.mjs');
const config = path.join(__dirname, 'vitest-no-spawn.config.mjs');
process.argv = [process.argv0, vitestCli, 'run', '--config', config, '--configLoader', 'native', ...process.argv.slice(2)];
void import(pathToFileURL(vitestCli).href);
