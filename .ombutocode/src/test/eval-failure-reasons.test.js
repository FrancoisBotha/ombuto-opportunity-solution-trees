const test = require('node:test');
const assert = require('node:assert/strict');

const { resolveEvalOutcomeAfterRun } = require('../src/main/runLifecycle');

// The reasons stored on a failed ticket are what a person reads first. They
// must describe what the evaluator actually said: an EPIC_REFERENCE_CHECK line
// that is present and says FAIL is a verdict, not a missing line.

const EPIC = 'docs/Epics/epic_02_TREE_EDITOR_CORE.md';
const MISSING_LINE = /missing explicit epic spec verification/;

function evaluate(lines, epicRef = EPIC) {
  return resolveEvalOutcomeAfterRun({
    runState: 'completed',
    currentStatus: 'eval',
    epicRef,
    stdout: lines.join('\n')
  });
}

test('an explicit EPIC_REFERENCE_CHECK: FAIL is reported as a failed check, not as a missing line', () => {
  // Shape of the real TREE-006 evaluator output that exposed the bug.
  const outcome = evaluate([
    'EVALUATION_RESULT: FAIL',
    'ACCEPTANCE_CRITERIA_CHECKS:',
    '- PASS: An "Add product" toolbar action opens a form | evidence: tree-editor.vue',
    '- FAIL: Deleting a node deletes it via the API and removes its descendants | failure_reason: deleteNode calls the generated non-cascading endpoints | suggestion: call api/tree/...',
    'EPIC_REFERENCE_CHECK: FAIL | evidence: The frontend bypasses the cascade and authorisation endpoints delivered by TREE-003.',
    'SUMMARY: delete is wired to the wrong endpoints'
  ]);

  assert.equal(outcome.nextStatus, 'todo');
  assert.equal(outcome.evalSummary.epic_reference_check, 'FAIL');

  const reasons = outcome.reasons.join('\n');
  assert.doesNotMatch(reasons, MISSING_LINE, 'the line was present, so it must not be reported as missing');
  assert.match(reasons, /EPIC_REFERENCE_CHECK: FAIL/);
  assert.ok(reasons.includes(EPIC), 'the reason should name the epic');
  assert.match(reasons, /Acceptance criterion failed: Deleting a node/);
  assert.deepEqual(outcome.evalSummary.failure_reasons, outcome.reasons);
});

test('a genuinely absent EPIC_REFERENCE_CHECK line is still reported as missing', () => {
  const outcome = evaluate([
    'EVALUATION_RESULT: PASS',
    'ACCEPTANCE_CRITERIA_CHECKS:',
    '- PASS: everything works | evidence: looked at it',
    'SUMMARY: fine'
  ]);

  assert.equal(outcome.nextStatus, 'todo');
  assert.match(outcome.reasons.join('\n'), MISSING_LINE);
});

test('a failing criterion alone is named instead of the generic FAIL message', () => {
  const outcome = evaluate([
    'EVALUATION_RESULT: FAIL',
    'ACCEPTANCE_CRITERIA_CHECKS:',
    '- FAIL: The list is sorted by name | failure_reason: it is sorted by id',
    'EPIC_REFERENCE_CHECK: PASS | evidence: matches the epic',
    'SUMMARY: wrong sort order'
  ]);

  assert.equal(outcome.nextStatus, 'todo');
  assert.deepEqual(outcome.reasons, ['Acceptance criterion failed: The list is sorted by name']);
});

test('naming failed criteria does not change the pass/fail decision', () => {
  // Everything passes -> review, with no reasons.
  const passing = evaluate([
    'EVALUATION_RESULT: PASS',
    'ACCEPTANCE_CRITERIA_CHECKS:',
    '- PASS: it works | evidence: spec',
    'EPIC_REFERENCE_CHECK: PASS | evidence: matches',
    'SUMMARY: good'
  ]);
  assert.equal(passing.nextStatus, 'review');
  assert.deepEqual(passing.reasons, []);

  // An overall PASS with one criterion marked FAIL went to review before this
  // change; describing failures must not tighten that gate as a side effect.
  const contradictory = evaluate([
    'EVALUATION_RESULT: PASS',
    'ACCEPTANCE_CRITERIA_CHECKS:',
    '- PASS: it works | evidence: spec',
    '- FAIL: minor wording | failure_reason: label differs',
    'EPIC_REFERENCE_CHECK: PASS | evidence: matches',
    'SUMMARY: good enough'
  ]);
  assert.equal(contradictory.nextStatus, 'review');
});

test('ad-hoc tickets without an epic never get an epic reference reason', () => {
  const outcome = evaluate(
    ['EVALUATION_RESULT: FAIL', 'ACCEPTANCE_CRITERIA_CHECKS:', '- FAIL: it works | failure_reason: it does not', 'SUMMARY: broken'],
    ''
  );
  assert.equal(outcome.nextStatus, 'todo');
  assert.doesNotMatch(outcome.reasons.join('\n'), /EPIC_REFERENCE_CHECK|epic spec/);
});
