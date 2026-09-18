const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('fs');
const path = require('path');

const storePath = path.join(__dirname, '../src/renderer/stores/agentToolsStore.js');
const viewPath = path.join(__dirname, '../src/renderer/components/AgentsToolsView.vue');

// The store is an ES module bound to Pinia and window.electron, so lift the
// pure reorder function out of its source and run it against stubs.
function loadMoveModel(tools, saved) {
  const source = fs.readFileSync(storePath, 'utf-8');
  const match = source.match(/function moveModel\(toolId, modelId, direction\) \{[\s\S]*?\n {2}\}/);
  assert.ok(match, 'agentToolsStore should define moveModel(toolId, modelId, direction)');
  const _tools = { value: tools };
  const updateTool = (toolId, updates) => {
    const tool = _tools.value.find((entry) => entry.id === toolId);
    tool.models = updates.models;
    saved.push(updates.models.map((model) => model.id));
  };
  return new Function('_tools', 'updateTool', `${match[0]}; return moveModel;`)(_tools, updateTool);
}

function fixture() {
  return [{ id: 'claude', models: [{ id: 'opus-4.7' }, { id: 'sonnet-4.6' }, { id: 'opus-5' }] }];
}

test('moveModel swaps a model with its neighbour and persists the new order', () => {
  const saved = [];
  const moveModel = loadMoveModel(fixture(), saved);

  assert.equal(moveModel('claude', 'opus-5', 'up'), true);
  assert.deepEqual(saved.at(-1), ['opus-4.7', 'opus-5', 'sonnet-4.6']);

  assert.equal(moveModel('claude', 'opus-5', 'up'), true);
  assert.deepEqual(saved.at(-1), ['opus-5', 'opus-4.7', 'sonnet-4.6']);

  assert.equal(moveModel('claude', 'opus-5', 'down'), true);
  assert.deepEqual(saved.at(-1), ['opus-4.7', 'opus-5', 'sonnet-4.6']);
});

test('moveModel is a no-op at the ends of the list and for unknown ids', () => {
  const saved = [];
  const moveModel = loadMoveModel(fixture(), saved);

  assert.equal(moveModel('claude', 'opus-4.7', 'up'), false);
  assert.equal(moveModel('claude', 'opus-5', 'down'), false);
  assert.equal(moveModel('claude', 'missing', 'up'), false);
  assert.equal(moveModel('codex', 'opus-5', 'up'), false);
  assert.equal(saved.length, 0, 'nothing should be persisted for a no-op');
});

test('agents view exposes move up/down controls bound to the store', () => {
  const view = fs.readFileSync(viewPath, 'utf-8');
  const store = fs.readFileSync(storePath, 'utf-8');

  assert.ok(view.includes(`@click="moveModel(tool.id, model, 'up')"`), 'view should have a move-up button');
  assert.ok(view.includes(`@click="moveModel(tool.id, model, 'down')"`), 'view should have a move-down button');
  assert.ok(view.includes(':disabled="modelIndex === 0"'), 'move-up should be disabled on the first model');
  assert.ok(
    view.includes(':disabled="modelIndex === tool.models.length - 1"'),
    'move-down should be disabled on the last model'
  );
  assert.ok(view.includes('toolsStore.moveModel(toolId, model.id, direction)'), 'view should delegate to the store');
  assert.ok(/\n {4}moveModel,\r?\n/.test(store), 'store should export moveModel');
});
