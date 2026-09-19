import { afterEach, describe, expect, it, vi } from 'vitest';

import { enableAutoUnmount, flushPromises } from '@vue/test-utils';

import { dto } from '../../domain/fixtures.test-util';
import { priorityColor } from '../../domain/rules';
import { PANEL_TREE, apiError, mountWith, setupStores } from '../panel.test-util';

import DetailTab from './DetailTab.vue';

enableAutoUnmount(afterEach);

async function mountTab(nodeKey: string, extra: Parameters<typeof setupStores>[1] = {}) {
  const ctx = await setupStores(PANEL_TREE, extra);
  ctx.ui.select(nodeKey);
  const wrapper = await mountWith(DetailTab, ctx.pinia, { nodeKey });
  return { ...ctx, wrapper };
}

const statusChips = (wrapper: Awaited<ReturnType<typeof mountTab>>['wrapper']) =>
  wrapper.findAll('[data-cy^="ost-status-"]').map(c => c.attributes('data-cy')!.replace('ost-status-', ''));

/** Gives the slider track a 100px-wide box at x=0 (happy-dom has no layout). */
function sizeTrack(el: Element) {
  Object.defineProperty(el, 'getBoundingClientRect', {
    value: () => ({ left: 0, top: 0, width: 100, height: 26, right: 100, bottom: 26, x: 0, y: 0, toJSON: () => ({}) }),
  });
}

describe('DetailTab', () => {
  describe('status chips', () => {
    it.each([
      ['opportunity-1', ['unexplored', 'exploring', 'validated', 'parked']],
      ['solution-1', ['candidate', 'building', 'shipped', 'dropped']],
      ['assumption-1', ['untested', 'testing', 'supported', 'refuted']],
      ['outcome-1', []],
      ['product-1', []],
      ['evidence-1', []],
    ])('%s → %j', async (key, chips) => {
      const { wrapper } = await mountTab(key);
      expect(statusChips(wrapper)).toEqual(chips);
    });

    it('marks the current status and patches a new one', async () => {
      const { wrapper, service, tree } = await mountTab('opportunity-1');
      expect(wrapper.get('[data-cy="ost-status-exploring"]').attributes('aria-pressed')).toBe('true');
      service.patchNode.resolves(dto('opportunity-1', 'outcome-1', { status: 'VALIDATED', priority: 50, valueRating: 3 }));
      await wrapper.get('[data-cy="ost-status-validated"]').trigger('click');
      expect(tree.byId('opportunity-1')?.status).toBe('validated'); // optimistic
      await flushPromises();
      expect(service.patchNode.calledOnceWith('opportunity', 1, { status: 'VALIDATED' })).toBe(true);
      expect(wrapper.get('[data-cy="ost-status-validated"]').classes()).toContain('is-on');
    });
  });

  describe('per-type fields', () => {
    it('confidence and owner only on assumptions; evidence bar only on solutions; value + priority only on opportunities', async () => {
      const present = async (key: string) => {
        const { wrapper } = await mountTab(key);
        return ['ost-confidence', 'ost-owner', 'ost-evidence-bar', 'ost-value-1', 'ost-priority'].filter(cy =>
          wrapper.find(`[data-cy="${cy}"]`).exists(),
        );
      };
      expect(await present('assumption-1')).toEqual(['ost-confidence', 'ost-owner']);
      expect(await present('solution-1')).toEqual(['ost-evidence-bar']);
      expect(await present('opportunity-1')).toEqual(['ost-value-1', 'ost-priority']);
      expect(await present('outcome-1')).toEqual([]);
      expect(await present('evidence-1')).toEqual([]);
    });

    it('every type has notes and a child list', async () => {
      const { wrapper } = await mountTab('evidence-1');
      expect(wrapper.find('[data-cy="ost-notes"]').exists()).toBe(true);
      expect(wrapper.text()).toContain('Children (0)');
    });
  });

  describe('confidence', () => {
    it('fills the steps up to the value, labels it and patches a step', async () => {
      const { wrapper, service } = await mountTab('assumption-2');
      const on = wrapper.findAll('[data-cy^="ost-confidence-"]').filter(s => s.classes('is-on'));
      expect(on.map(s => s.attributes('data-cy'))).toEqual(['ost-confidence-20', 'ost-confidence-40', 'ost-confidence-60']);
      expect(wrapper.get('[data-cy="ost-confidence-label"]').text()).toBe('Med-high');
      service.patchNode.resolves(dto('assumption-2', 'solution-1', { status: 'TESTING', confidence: 80 }));
      await wrapper.get('[data-cy="ost-confidence-80"]').trigger('click');
      await flushPromises();
      expect(service.patchNode.calledOnceWith('assumption', 2, { confidence: 80 })).toBe(true);
    });

    it('announces the highest step not above a value that is not a multiple of 20 as pressed', async () => {
      const { wrapper, tree } = await mountTab('assumption-2');
      const pressed = () =>
        wrapper
          .findAll('[data-cy^="ost-confidence-"]')
          .filter(s => s.attributes('aria-pressed') === 'true')
          .map(s => s.attributes('data-cy'));
      expect(pressed()).toEqual(['ost-confidence-60']);
      tree.byId('assumption-2')!.conf = 50;
      await flushPromises();
      expect(pressed()).toEqual(['ost-confidence-40']);
      tree.byId('assumption-2')!.conf = 10;
      await flushPromises();
      expect(pressed()).toEqual([]);
    });
  });

  describe('evidence strength', () => {
    it('is derived from the assumptions below: supported → 100, refuted → 0, else confidence', async () => {
      const { wrapper, tree } = await mountTab('solution-1');
      // assumption-1 supported (100), assumption-2 testing at 60 → 80
      expect(wrapper.get('[data-cy="ost-evidence-bar"]').attributes('data-score')).toBe('80');
      expect(wrapper.get('[data-cy="ost-evidence-score"]').text()).toBe('80%');
      expect(wrapper.get('[data-cy="ost-evidence-note"]').text()).toContain('2 assumption tests · 1 supported · 0 refuted');
      tree.byId('assumption-2')!.status = 'refuted';
      await flushPromises();
      expect(wrapper.get('[data-cy="ost-evidence-score"]').text()).toBe('50%');
      // read-only: nothing to click
      expect(wrapper.get('[data-cy="ost-evidence-bar"]').findAll('button, input')).toHaveLength(0);
    });

    it('says untested when the solution has no assumptions', async () => {
      const { wrapper } = await mountTab('solution-2');
      expect(wrapper.get('[data-cy="ost-evidence-score"]').text()).toBe('untested');
      expect(wrapper.get('[data-cy="ost-evidence-note"]').text()).toContain('No assumption tests under this solution yet');
    });
  });

  describe('value $ scale', () => {
    it('shows five $ steps with the word label and commits on click', async () => {
      const { wrapper, service } = await mountTab('opportunity-1');
      expect(wrapper.findAll('[data-cy^="ost-value-"]').filter(b => /^\$+$/.test(b.text()))).toHaveLength(5);
      expect(wrapper.get('[data-cy="ost-value-5"]').text()).toBe('$$$$$');
      expect(wrapper.get('[data-cy="ost-value-label"]').text()).toBe('Solid');
      expect(wrapper.findAll('.ost-value__step.is-on')).toHaveLength(3);
      service.patchNode.resolves(dto('opportunity-1', 'outcome-1', { status: 'EXPLORING', priority: 50, valueRating: 5 }));
      await wrapper.get('[data-cy="ost-value-5"]').trigger('click');
      expect(wrapper.get('[data-cy="ost-value-label"]').text()).toBe('Outsized');
      await flushPromises();
      expect(service.patchNode.calledOnceWith('opportunity', 1, { valueRating: 5 })).toBe(true);
    });
  });

  describe('priority slider', () => {
    it('drags a local draft and commits once, on release', async () => {
      const { wrapper, service, tree } = await mountTab('opportunity-1');
      service.patchNode.resolves(dto('opportunity-1', 'outcome-1', { status: 'EXPLORING', priority: 70, valueRating: 3 }));
      const slider = wrapper.get('[data-cy="ost-priority"]');
      sizeTrack(slider.element);
      await slider.trigger('pointerdown', { clientX: 30, button: 0, pointerId: 1 });
      expect(slider.attributes('aria-valuenow')).toBe('30');
      await slider.trigger('pointermove', { clientX: 70, pointerId: 1 });
      expect(slider.attributes('aria-valuenow')).toBe('70');
      expect(wrapper.get('[data-cy="ost-priority-label"]').text()).toBe('High · 70');
      expect(service.patchNode.called).toBe(false);
      expect(tree.byId('opportunity-1')?.priority).toBe(50);
      await slider.trigger('pointerup', { clientX: 70, pointerId: 1 });
      await flushPromises();
      expect(service.patchNode.calledOnceWith('opportunity', 1, { priority: 70 })).toBe(true);
      expect(tree.byId('opportunity-1')?.priority).toBe(70);
    });

    it('clamps to 1–100 and colours the handle on the cold → warm ramp', async () => {
      const { wrapper, service } = await mountTab('opportunity-1');
      service.patchNode.resolves(dto('opportunity-1', 'outcome-1', { status: 'EXPLORING', priority: 1, valueRating: 3 }));
      const slider = wrapper.get('[data-cy="ost-priority"]');
      expect(wrapper.get('[data-cy="ost-priority-handle"]').attributes('data-color')).toBe(priorityColor(50));
      sizeTrack(slider.element);
      await slider.trigger('pointerdown', { clientX: -40, button: 0, pointerId: 1 });
      expect(slider.attributes('aria-valuenow')).toBe('1');
      expect(wrapper.get('[data-cy="ost-priority-handle"]').attributes('data-color')).toBe(priorityColor(1));
      await slider.trigger('pointermove', { clientX: 400, pointerId: 1 });
      expect(slider.attributes('aria-valuenow')).toBe('100');
      await slider.trigger('pointercancel', { pointerId: 1 });
      expect(slider.attributes('aria-valuenow')).toBe('50');
      expect(service.patchNode.called).toBe(false);
    });

    it('key presses move the value at once but commit one PATCH once the keys are idle (C15)', async () => {
      const { wrapper, service } = await mountTab('opportunity-1');
      service.patchNode.resolves(dto('opportunity-1', 'outcome-1', { status: 'EXPLORING', priority: 63, valueRating: 3 }));
      vi.useFakeTimers();
      try {
        const slider = wrapper.get('[data-cy="ost-priority"]');
        await slider.trigger('keydown', { key: 'PageUp' });
        for (let i = 0; i < 3; i++) await slider.trigger('keydown', { key: 'ArrowRight', repeat: true });
        expect(slider.attributes('aria-valuenow')).toBe('63');
        vi.advanceTimersByTime(300);
        expect(service.patchNode.called).toBe(false);
        vi.advanceTimersByTime(200);
      } finally {
        vi.useRealTimers();
      }
      await flushPromises();
      expect(service.patchNode.calledOnceWith('opportunity', 1, { priority: 63 })).toBe(true);
    });

    it('blur commits a pending keyboard value immediately; back to the start sends nothing', async () => {
      const { wrapper, service } = await mountTab('opportunity-1');
      service.patchNode.resolves(dto('opportunity-1', 'outcome-1', { status: 'EXPLORING', priority: 100, valueRating: 3 }));
      const slider = wrapper.get('[data-cy="ost-priority"]');
      await slider.trigger('keydown', { key: 'ArrowUp' });
      await slider.trigger('keydown', { key: 'ArrowDown' });
      await slider.trigger('blur');
      await flushPromises();
      expect(service.patchNode.called).toBe(false);
      await slider.trigger('keydown', { key: 'End' });
      await slider.trigger('blur');
      await flushPromises();
      expect(service.patchNode.calledOnceWith('opportunity', 1, { priority: 100 })).toBe(true);
    });
  });

  describe('owner', () => {
    it('offers Unassigned plus the team members and patches the login', async () => {
      const { wrapper, service } = await mountTab('assumption-2');
      const select = wrapper.get('[data-cy="ost-owner"]');
      expect(select.findAll('option').map(o => [o.attributes('value'), o.text()])).toEqual([
        ['', 'Unassigned'],
        ['user', 'Kira P'],
        ['admin', 'Ana R'],
      ]);
      expect((select.element as HTMLSelectElement).value).toBe('');
      service.patchNode.resolves(dto('assumption-2', 'solution-1', { status: 'TESTING', confidence: 60, ownerLogin: 'admin' }));
      await select.setValue('admin');
      await flushPromises();
      expect(service.patchNode.calledOnceWith('assumption', 2, { ownerLogin: 'admin' })).toBe(true);
    });

    it('shows a server refusal inline', async () => {
      const { wrapper, service, tree } = await mountTab('assumption-2');
      service.patchNode.rejects(apiError(400, 'error.ownernotmember'));
      await wrapper.get('[data-cy="ost-owner"]').setValue('admin');
      await flushPromises();
      expect(tree.byId('assumption-2')?.owner).toBe('');
      // DetailTab alone has no panel to show it in — the message is still taken off the toast
      expect(tree.error).toBeNull();
    });
  });

  describe('notes', () => {
    it('commits on blur only', async () => {
      const { wrapper, service } = await mountTab('outcome-1');
      service.patchNode.resolves(dto('outcome-1', 'product-1', { notes: 'Learned a lot' }));
      const notes = wrapper.get('[data-cy="ost-notes"]');
      await notes.trigger('focus');
      await notes.setValue('Learned a lot');
      expect(service.patchNode.called).toBe(false);
      await notes.trigger('blur');
      await flushPromises();
      expect(service.patchNode.calledOnceWith('outcome', 1, { notes: 'Learned a lot' })).toBe(true);
    });
  });

  describe('children and quick-add', () => {
    it('lists the children; a click selects and centres the child', async () => {
      const { wrapper, ui } = await mountTab('opportunity-1');
      expect(wrapper.findAll('[data-cy^="ost-child-"]').map(c => c.attributes('data-cy'))).toEqual([
        'ost-child-solution-1',
        'ost-child-solution-2',
      ]);
      expect(wrapper.get('[data-cy="ost-child-solution-1"]').text()).toContain('Solution');
      await wrapper.get('[data-cy="ost-child-solution-2"]').trigger('click');
      expect(ui.selectedId).toBe('solution-2');
      expect(ui.centreRequest?.key).toBe('solution-2');
    });

    it.each([
      ['product-1', ['outcome']],
      ['outcome-1', ['opportunity']],
      ['opportunity-1', ['opportunity', 'solution', 'evidence']],
      ['solution-1', ['assumption']],
      ['assumption-1', ['evidence']],
      ['evidence-1', []],
    ])('%s quick-adds %j', async (key, types) => {
      const { wrapper } = await mountTab(key);
      expect(wrapper.findAll('[data-cy^="ost-quick-add-"]').map(b => b.attributes('data-cy')!.replace('ost-quick-add-', ''))).toEqual(
        types,
      );
    });

    it('creates the child, selects it and centres it', async () => {
      const { wrapper, service, ui, tree } = await mountTab('solution-2');
      service.createNode.resolves(dto('assumption-9', 'solution-2', { title: 'New assumption', status: 'UNTESTED', confidence: 40 }));
      await wrapper.get('[data-cy="ost-quick-add-assumption"]').trigger('click');
      await flushPromises();
      expect(service.createNode.calledOnceWith({ type: 'ASSUMPTION', parentType: 'SOLUTION', parentId: 2 })).toBe(true);
      expect(tree.byId('assumption-9')).toBeDefined();
      expect(ui.selectedId).toBe('assumption-9');
      expect(ui.centreRequest?.key).toBe('assumption-9');
    });
  });

  describe('viewer', () => {
    it('is read-only: no quick-add, disabled chips / confidence / value / owner, read-only notes and slider', async () => {
      const viewer = { canEdit: false, currentUserRole: 'VIEWER' as const };
      const opp = await mountTab('opportunity-1', viewer);
      expect(opp.wrapper.findAll('[data-cy^="ost-quick-add-"]')).toHaveLength(0);
      expect(opp.wrapper.get('[data-cy="ost-status-validated"]').attributes('disabled')).toBeDefined();
      expect(opp.wrapper.get('[data-cy="ost-value-5"]').attributes('disabled')).toBeDefined();
      expect(opp.wrapper.get('[data-cy="ost-notes"]').attributes('readonly')).toBeDefined();
      const slider = opp.wrapper.get('[data-cy="ost-priority"]');
      expect(slider.attributes('aria-readonly')).toBe('true');
      sizeTrack(slider.element);
      await slider.trigger('pointerdown', { clientX: 90, button: 0, pointerId: 1 });
      await slider.trigger('pointerup', { clientX: 90, pointerId: 1 });
      await slider.trigger('keydown', { key: 'End' });
      await opp.wrapper.get('[data-cy="ost-value-5"]').trigger('click');
      await flushPromises();
      expect(opp.service.patchNode.called).toBe(false);
      // children still navigate
      expect(opp.wrapper.find('[data-cy="ost-child-solution-1"]').exists()).toBe(true);

      const as = await mountTab('assumption-2', viewer);
      expect(as.wrapper.get('[data-cy="ost-confidence-80"]').attributes('disabled')).toBeDefined();
      expect(as.wrapper.get('[data-cy="ost-owner"]').attributes('disabled')).toBeDefined();
    });
  });

  describe('accessibility and layout (C15)', () => {
    it('confidence: only the step equal to the value is pressed', async () => {
      const { wrapper } = await mountTab('assumption-2');
      const pressed = [20, 40, 60, 80, 100].map(v => wrapper.get(`[data-cy="ost-confidence-${v}"]`).attributes('aria-pressed'));
      expect(pressed).toEqual(['false', 'false', 'true', 'false', 'false']);
      // steps up to the value still look filled
      expect(wrapper.get('[data-cy="ost-confidence-40"]').classes()).toContain('is-on');
    });

    it('evidence strength: the meter says "untested" instead of 0%', async () => {
      const lonely = await mountTab('solution-2');
      const bar = lonely.wrapper.get('[data-cy="ost-evidence-bar"]');
      expect(bar.attributes('aria-valuetext')).toMatch(/^Untested/);
      const tested = await mountTab('solution-1');
      const score = tested.wrapper.get('[data-cy="ost-evidence-bar"]').attributes('data-score');
      expect(tested.wrapper.get('[data-cy="ost-evidence-bar"]').attributes('aria-valuetext')).toBe(`${score}%`);
    });

    it('labels use the prototype spacing per field', async () => {
      const { wrapper } = await mountTab('opportunity-1');
      const label = (text: string) => wrapper.findAll('.ost-field__label').find(l => l.text().startsWith(text))!;
      expect(label('Status').classes()).toContain('ost-field__label--6');
      expect(label('Opportunity value').classes()).toContain('ost-field__label--7');
      expect(label('Priority').classes()).toContain('ost-field__label--8');
      expect(label('Notes').classes()).toContain('ost-field__label--6');
      expect(label('Children').classes()).toContain('ost-field__label--7');
    });
  });

  describe('notes', () => {
    it('a failed save keeps the typed notes in the field (rolled back in the tree only)', async () => {
      const { wrapper, service, tree } = await mountTab('solution-1');
      service.patchNode.rejects(apiError(403));
      const notes = wrapper.get('[data-cy="ost-notes"]');
      await notes.trigger('focus');
      await notes.setValue('Typed notes worth keeping');
      await notes.trigger('blur');
      await flushPromises();
      expect(service.patchNode.calledOnceWith('solution', 1, { notes: 'Typed notes worth keeping' })).toBe(true);
      expect(tree.byId('solution-1')?.note).toBe('');
      expect((notes.element as HTMLTextAreaElement).value).toBe('Typed notes worth keeping');
      // Blurring again retries with the kept text.
      service.patchNode.resolves(dto('solution-1', 'opportunity-1', { notes: 'Typed notes worth keeping' }));
      await notes.trigger('focus');
      await notes.trigger('blur');
      await flushPromises();
      expect(service.patchNode.callCount).toBe(2);
      expect(tree.byId('solution-1')?.note).toBe('Typed notes worth keeping');
      expect((notes.element as HTMLTextAreaElement).value).toBe('Typed notes worth keeping');
    });

    it('a successful save follows the server copy', async () => {
      const { wrapper, service, tree } = await mountTab('solution-1');
      service.patchNode.resolves(dto('solution-1', 'opportunity-1', { notes: 'Saved' }));
      const notes = wrapper.get('[data-cy="ost-notes"]');
      await notes.trigger('focus');
      await notes.setValue('Saved');
      await notes.trigger('blur');
      await flushPromises();
      expect(tree.byId('solution-1')?.note).toBe('Saved');
      tree.byId('solution-1')!.note = 'Changed elsewhere';
      await flushPromises();
      expect((notes.element as HTMLTextAreaElement).value).toBe('Changed elsewhere');
    });
  });
});
