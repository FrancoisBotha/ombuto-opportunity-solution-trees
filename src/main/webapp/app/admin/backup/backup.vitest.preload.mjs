import childProcess from 'node:child_process';
import { syncBuiltinESMExports } from 'node:module';

// Vite probes Windows network drives with `net use` before it loads the test
// config. The build sandbox blocks subprocesses, so provide the empty result
// that Vite expects when no network drives are mapped.
const emptyNetUseResult = callback => {
  queueMicrotask(() => callback?.(null, '', ''));
  return { on() {}, unref() {} };
};

const originalExec = childProcess.exec;
childProcess.exec = function (command, options, callback) {
  if (/^net use$/i.test(command.trim())) {
    return emptyNetUseResult(typeof options === 'function' ? options : callback);
  }
  return originalExec.apply(this, arguments);
};

const originalExecFile = childProcess.execFile;
childProcess.execFile = function (file, args, options, callback) {
  const command = [file, ...(Array.isArray(args) ? args : [])].join(' ');
  if (/\bnet use\b/i.test(command)) {
    return emptyNetUseResult(typeof options === 'function' ? options : callback);
  }
  return originalExecFile.apply(this, arguments);
};
syncBuiltinESMExports();
