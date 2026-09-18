/*
 * Regenerates THIRD-PARTY-NOTICES.md from the real dependency data:
 *   - frontend: production packages in package-lock.json (dev-only packages are not shipped)
 *   - backend:  runtime dependencies of the `prod` Maven profile, resolved by license-maven-plugin
 *
 * Usage:  node scripts/generate-third-party-notices.cjs
 */
const { execFileSync } = require('node:child_process');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const outFile = path.join(root, 'THIRD-PARTY-NOTICES.md');
const mavenReportDir = path.join(root, 'target', 'generated-licenses');

// Maven POMs spell the same licence many ways; fold them into SPDX identifiers.
const SPDX = [
  [/apache/i, 'Apache-2.0'],
  [/^mit\b|mit license/i, 'MIT'],
  [/bsd-3|bsd 3|new bsd|3-clause/i, 'BSD-3-Clause'],
  [/bsd-2|bsd 2|2-clause/i, 'BSD-2-Clause'],
  [/eclipse distribution|^edl/i, 'BSD-3-Clause (EDL-1.0)'],
  [/eclipse public license.*2|^epl 2|epl-2/i, 'EPL-2.0'],
  [/eclipse public license.*1|^epl 1|epl-1/i, 'EPL-1.0'],
  [/lesser general public|lgpl/i, 'LGPL-2.1'],
  [/gpl2 w\/ cpe|classpath/i, 'GPL-2.0-with-classpath-exception'],
  [/mpl|mozilla/i, 'MPL-2.0'],
  [/cddl/i, 'CDDL-1.1'],
  [/cc0|public domain/i, 'CC0-1.0'],
  [/fsl/i, 'FSL-1.1-ALv2'],
];
const spdx = name => (SPDX.find(([pattern]) => pattern.test(name)) ?? [null, name])[1];
const cell = text => String(text ?? '').replace(/\|/g, '\\|');

function frontendPackages() {
  const lock = JSON.parse(fs.readFileSync(path.join(root, 'package-lock.json'), 'utf8'));
  const direct = new Set(Object.keys(lock.packages[''].dependencies ?? {}));
  const rows = [];
  for (const [location, info] of Object.entries(lock.packages)) {
    if (!location || info.dev || info.devOptional || info.optional || info.link) continue;
    const name = location.slice(location.lastIndexOf('node_modules/') + 'node_modules/'.length);
    let license = info.license;
    let homepage = '';
    try {
      const manifest = JSON.parse(fs.readFileSync(path.join(root, location, 'package.json'), 'utf8'));
      license ??= typeof manifest.license === 'string' ? manifest.license : manifest.license?.type;
      homepage = manifest.homepage ?? '';
    } catch {
      // not installed locally; the lockfile data is enough
    }
    rows.push({ name, version: info.version, license: license ?? 'UNKNOWN', homepage, direct: direct.has(name) });
  }
  const unique = new Map(rows.map(row => [`${row.name}@${row.version}`, row]));
  return [...unique.values()].sort((a, b) => a.name.localeCompare(b.name));
}

function backendPackages() {
  // mvnw.cmd is a batch file, which Windows can only start through cmd.exe.
  const windows = process.platform === 'win32';
  execFileSync(
    windows ? 'cmd.exe' : path.join(root, 'mvnw'),
    [
      ...(windows ? ['/c', path.join(root, 'mvnw.cmd')] : []),
      '-ntp',
      '-q',
      '--batch-mode',
      '-Pprod',
      '-Dskip.installnodenpm',
      '-Dskip.npm',
      'org.codehaus.mojo:license-maven-plugin:2.4.0:add-third-party',
      `-Dlicense.outputDirectory=${mavenReportDir}`,
      '-Dlicense.excludedScopes=test,provided',
    ],
    { cwd: root, stdio: 'inherit' },
  );
  const report = fs.readFileSync(path.join(mavenReportDir, 'THIRD-PARTY.txt'), 'utf8');
  const rows = [];
  for (const line of report.split(/\r?\n/)) {
    const match = line.match(/^\s+((?:\([^)]*\)\s+)+)(.+?)\s+\(([^\s:]+:[^\s:]+):([^\s:]+) - ([^)]*)\)\s*$/);
    if (!match) continue;
    const licenses = [...match[1].matchAll(/\(([^)]*)\)/g)].map(found => spdx(found[1]));
    rows.push({
      name: match[2],
      coordinates: match[3],
      version: match[4],
      homepage: match[5],
      license: [...new Set(licenses)].join(' OR '),
    });
  }
  return rows.sort((a, b) => a.coordinates.localeCompare(b.coordinates));
}

function tally(rows) {
  const counts = new Map();
  for (const row of rows) counts.set(row.license, (counts.get(row.license) ?? 0) + 1);
  return [...counts.entries()].sort((a, b) => b[1] - a[1]);
}

const frontend = frontendPackages();
const backend = backendPackages();
const header = fs.readFileSync(path.join(__dirname, 'third-party-notices.header.md'), 'utf8').trimEnd();

const lines = [
  header,
  '',
  '## Licence summary',
  '',
  `**Frontend** — ${frontend.length} packages in the web client's production dependency tree:`,
  '',
  ...tally(frontend).map(([license, count]) => `- ${license}: ${count}`),
  '',
  `**Backend** — ${backend.length} runtime libraries in the production build:`,
  '',
  ...tally(backend).map(([license, count]) => `- ${license}: ${count}`),
  '',
  '## Frontend packages',
  '',
  'Packages marked **direct** are declared in `package.json`; the rest are pulled in by them.',
  '',
  '| Package | Version | Licence | |',
  '| --- | --- | --- | --- |',
  ...frontend.map(
    row =>
      `| ${row.homepage ? `[${cell(row.name)}](${row.homepage})` : cell(row.name)} | ${row.version} | ${cell(row.license)} | ${row.direct ? 'direct' : ''} |`,
  ),
  '',
  '## Backend libraries',
  '',
  'Where a library is offered under several licences, this project uses it under the first one listed.',
  '',
  '| Library | Coordinates | Version | Licence |',
  '| --- | --- | --- | --- |',
  ...backend.map(
    row =>
      `| ${row.homepage ? `[${cell(row.name)}](${row.homepage})` : cell(row.name)} | \`${row.coordinates}\` | ${row.version} | ${cell(row.license)} |`,
  ),
  '',
];

fs.writeFileSync(outFile, lines.join('\n'));
console.log(`Wrote ${path.relative(root, outFile)}: ${frontend.length} frontend packages, ${backend.length} backend libraries.`);
