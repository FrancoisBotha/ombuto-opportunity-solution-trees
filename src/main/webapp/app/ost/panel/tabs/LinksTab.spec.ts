import { afterEach, describe, expect, it } from 'vitest';

import { enableAutoUnmount, flushPromises } from '@vue/test-utils';

import DetailPanel from '../DetailPanel.vue';
import { PANEL_TREE, apiError, mountWith, setupStores } from '../panel.test-util';

import LinksTab from './LinksTab.vue';

enableAutoUnmount(afterEach);

async function mountTab(nodeKey: string, extra: Parameters<typeof setupStores>[1] = {}) {
  const ctx = await setupStores(PANEL_TREE, extra);
  ctx.ui.select(nodeKey);
  const wrapper = await mountWith(LinksTab, ctx.pinia, { nodeKey });
  return { ...ctx, wrapper };
}

const restoreButtons = (wrapper: Awaited<ReturnType<typeof mountTab>>['wrapper']) =>
  wrapper
    .findAll('[data-cy^="ost-link-restore-"]')
    .map(b => [b.attributes('data-cy')!.replace('ost-link-restore-', ''), b.attributes('disabled') !== undefined]);

describe('LinksTab', () => {
  it('renders one row per link: name + URL inputs, an open link in a new tab, a remove button', async () => {
    const { wrapper } = await mountTab('opportunity-1');
    const row = wrapper.get('[data-cy="ost-link-row-11"]');
    expect((row.get('[data-cy="ost-link-name"]').element as HTMLInputElement).value).toBe('Confluence');
    expect((row.get('[data-cy="ost-link-url"]').element as HTMLInputElement).value).toBe(
      'https://ombuto.atlassian.net/wiki/discovery/opportunity-1',
    );
    const open = row.get('[data-cy="ost-link-open"]');
    expect(open.attributes('href')).toBe('https://ombuto.atlassian.net/wiki/discovery/opportunity-1');
    expect(open.attributes('target')).toBe('_blank');
    expect(open.attributes('rel')).toBe('noopener noreferrer');
    expect(row.find('[data-cy="ost-link-remove"]').exists()).toBe(true);
    expect(wrapper.find('[data-cy="ost-link-row-12"]').exists()).toBe(true);
  });

  describe('restore defaults', () => {
    it.each([
      ['product-1', [['product-space', true]]],
      ['outcome-1', [['confluence', false]]],
      [
        'opportunity-1',
        [
          ['confluence', true],
          ['jira-initiative', false],
          ['jira-epic', true],
        ],
      ],
      ['assumption-1', [['confluence', false]]],
      [
        'evidence-1',
        [
          ['confluence', false],
          ['jira-ticket', false],
        ],
      ],
    ])('%s: one button per default link, disabled when present → %j', async (key, expected) => {
      const { wrapper } = await mountTab(key);
      expect(restoreButtons(wrapper)).toEqual(expected);
    });

    it('re-adds a missing default and then disables its button', async () => {
      const { wrapper, service, tree } = await mountTab('opportunity-1');
      service.addLink.resolves({ id: 13, name: 'Jira Initiative', url: 'https://ombuto.atlassian.net/browse/INIT-000' });
      await wrapper.get('[data-cy="ost-link-restore-jira-initiative"]').trigger('click');
      await flushPromises();
      expect(
        service.addLink.calledOnceWith('opportunity', 1, { name: 'Jira Initiative', url: 'https://ombuto.atlassian.net/browse/INIT-000' }),
      ).toBe(true);
      expect(tree.byId('opportunity-1')?.links.map(l => l.id)).toEqual([11, 12, 13]);
      expect(wrapper.get('[data-cy="ost-link-restore-jira-initiative"]').attributes('disabled')).toBeDefined();
    });

    it('treats a renamed-case default as present', async () => {
      const { wrapper, tree } = await mountTab('outcome-1');
      tree.byId('outcome-1')!.links.push({ id: 50, name: 'confluence', url: 'https://x.test' });
      await flushPromises();
      expect(restoreButtons(wrapper)).toEqual([['confluence', true]]);
    });
  });

  describe('add link', () => {
    it('validates the name and the URL before calling the server', async () => {
      const { wrapper, service } = await mountTab('outcome-1');
      await wrapper.get('[data-cy="ost-link-add"]').trigger('click');
      await wrapper.get('[data-cy="ost-link-new-name"]').setValue('Spec');
      await wrapper.get('[data-cy="ost-link-new-url"]').setValue('ftp://nope');
      await wrapper.get('[data-cy="ost-link-form"]').trigger('submit');
      expect(wrapper.get('[data-cy="ost-link-error"]').text()).toContain('http:// or https://');
      await wrapper.get('[data-cy="ost-link-new-url"]').setValue('https://');
      await wrapper.get('[data-cy="ost-link-form"]').trigger('submit');
      expect(wrapper.get('[data-cy="ost-link-error"]').text()).toContain('http:// or https://');
      await wrapper.get('[data-cy="ost-link-new-name"]').setValue('  ');
      await wrapper.get('[data-cy="ost-link-new-url"]').setValue('https://example.com/spec');
      await wrapper.get('[data-cy="ost-link-form"]').trigger('submit');
      expect(wrapper.get('[data-cy="ost-link-error"]').text()).toContain('name');
      expect(service.addLink.called).toBe(false);
    });

    it('adds a valid link and closes the form', async () => {
      const { wrapper, service, tree } = await mountTab('outcome-1');
      service.addLink.resolves({ id: 21, name: 'Spec', url: 'https://example.com/spec' });
      await wrapper.get('[data-cy="ost-link-add"]').trigger('click');
      await wrapper.get('[data-cy="ost-link-new-name"]').setValue(' Spec ');
      await wrapper.get('[data-cy="ost-link-new-url"]').setValue('https://example.com/spec');
      await wrapper.get('[data-cy="ost-link-form"]').trigger('submit');
      await flushPromises();
      expect(service.addLink.calledOnceWith('outcome', 1, { name: 'Spec', url: 'https://example.com/spec' })).toBe(true);
      expect(tree.byId('outcome-1')?.links).toHaveLength(1);
      expect(wrapper.find('[data-cy="ost-link-form"]').exists()).toBe(false);
      expect(wrapper.find('[data-cy="ost-link-row-21"]').exists()).toBe(true);
    });

    it('Escape / Cancel closes the form without saving', async () => {
      const { wrapper } = await mountTab('outcome-1');
      await wrapper.get('[data-cy="ost-link-add"]').trigger('click');
      await wrapper.get('[data-cy="ost-link-new-name"]').trigger('keydown', { key: 'Escape' });
      expect(wrapper.find('[data-cy="ost-link-form"]').exists()).toBe(false);
    });
  });

  describe('edit and remove', () => {
    it('commits a renamed link on blur', async () => {
      const { wrapper, service, tree } = await mountTab('opportunity-1');
      service.updateLink.resolves({ id: 12, name: 'Delivery epic', url: 'https://ombuto.atlassian.net/browse/DISC-000' });
      const name = wrapper.get('[data-cy="ost-link-row-12"] [data-cy="ost-link-name"]');
      await name.setValue('Delivery epic');
      await name.trigger('blur');
      await flushPromises();
      expect(service.updateLink.calledOnceWith(12, { name: 'Delivery epic' })).toBe(true);
      expect(tree.byId('opportunity-1')?.links[1].name).toBe('Delivery epic');
    });

    it('rejects an invalid URL edit inline and keeps the stored URL', async () => {
      const { wrapper, service } = await mountTab('opportunity-1');
      const url = wrapper.get('[data-cy="ost-link-row-12"] [data-cy="ost-link-url"]');
      await url.setValue('not a url');
      await url.trigger('blur');
      expect(wrapper.get('[data-cy="ost-link-row-12"] [data-cy="ost-link-error"]').text()).toContain('http');
      expect((url.element as HTMLInputElement).value).toBe('https://ombuto.atlassian.net/browse/DISC-000');
      expect(service.updateLink.called).toBe(false);
    });

    it('commits a valid URL edit', async () => {
      const { wrapper, service } = await mountTab('opportunity-1');
      service.updateLink.resolves({ id: 12, name: 'Jira Epic', url: 'https://ombuto.atlassian.net/browse/DISC-42' });
      const url = wrapper.get('[data-cy="ost-link-row-12"] [data-cy="ost-link-url"]');
      await url.setValue('https://ombuto.atlassian.net/browse/DISC-42');
      await url.trigger('blur');
      await flushPromises();
      expect(service.updateLink.calledOnceWith(12, { url: 'https://ombuto.atlassian.net/browse/DISC-42' })).toBe(true);
    });

    it('removes a link, which re-enables its restore button', async () => {
      const { wrapper, service } = await mountTab('opportunity-1');
      service.deleteLink.resolves();
      await wrapper.get('[data-cy="ost-link-row-12"] [data-cy="ost-link-remove"]').trigger('click');
      await flushPromises();
      expect(service.deleteLink.calledOnceWith(12)).toBe(true);
      expect(wrapper.find('[data-cy="ost-link-row-12"]').exists()).toBe(false);
      expect(wrapper.get('[data-cy="ost-link-restore-jira-epic"]').attributes('disabled')).toBeUndefined();
    });
  });

  it('surfaces a server error inline in the panel', async () => {
    const ctx = await setupStores();
    ctx.ui.select('outcome-1');
    ctx.ui.setPanelTab('links');
    ctx.service.addLink.rejects(apiError(400, 'error.linkurlinvalid'));
    const wrapper = await mountWith(DetailPanel, ctx.pinia);
    await wrapper.get('[data-cy="ost-link-restore-confluence"]').trigger('click');
    await flushPromises();
    expect(wrapper.get('[data-cy="ost-panel-error"]').text()).toBe('Links must start with http:// or https://.');
    expect(ctx.tree.error).toBeNull();
  });

  describe('viewer', () => {
    it('is read-only: no add, no remove, no restore; links still open', async () => {
      const { wrapper, service } = await mountTab('opportunity-1', { canEdit: false, currentUserRole: 'VIEWER' });
      expect(wrapper.find('[data-cy="ost-link-add"]').exists()).toBe(false);
      expect(wrapper.find('[data-cy="ost-link-remove"]').exists()).toBe(false);
      expect(wrapper.findAll('[data-cy^="ost-link-restore-"]')).toHaveLength(0);
      expect(wrapper.find('[data-cy="ost-link-open"]').exists()).toBe(true);
      const name = wrapper.get('[data-cy="ost-link-row-11"] [data-cy="ost-link-name"]');
      expect(name.attributes('readonly')).toBeDefined();
      await name.setValue('Changed');
      await name.trigger('blur');
      await flushPromises();
      expect(service.updateLink.called).toBe(false);
    });
  });

  describe('step-10 review follow-ups (C15)', () => {
    it('a stored name change resets the name draft only, never the URL being typed', async () => {
      const { wrapper, tree } = await mountTab('opportunity-1');
      const row = wrapper.get('[data-cy="ost-link-row-11"]');
      const url = row.get('[data-cy="ost-link-url"]');
      (url.element as HTMLInputElement).focus();
      await url.setValue('https://typing.example/in-progress');
      tree.byId('opportunity-1')!.links[0].name = 'Renamed elsewhere';
      await flushPromises();
      expect((row.get('[data-cy="ost-link-name"]').element as HTMLInputElement).value).toBe('Renamed elsewhere');
      expect((url.element as HTMLInputElement).value).toBe('https://typing.example/in-progress');
    });

    it('a stored URL change does not overwrite a focused URL draft, but does once it is not focused', async () => {
      const { wrapper, tree } = await mountTab('opportunity-1');
      const row = wrapper.get('[data-cy="ost-link-row-11"]');
      const url = row.get('[data-cy="ost-link-url"]');
      (url.element as HTMLInputElement).focus();
      await url.setValue('https://typing.example/');
      tree.byId('opportunity-1')!.links[0].url = 'https://server.example/a';
      await flushPromises();
      expect((url.element as HTMLInputElement).value).toBe('https://typing.example/');
      await url.setValue('https://server.example/a'); // nothing to save on blur
      (url.element as HTMLInputElement).blur();
      tree.byId('opportunity-1')!.links[0].url = 'https://server.example/b';
      await flushPromises();
      expect((url.element as HTMLInputElement).value).toBe('https://server.example/b');
    });

    it('Escape in the add-link form returns focus to "+ Add link"', async () => {
      const { wrapper } = await mountTab('opportunity-1');
      await wrapper.get('[data-cy="ost-link-add"]').trigger('click');
      await flushPromises();
      expect(document.activeElement?.getAttribute('data-cy')).toBe('ost-link-new-name');
      await wrapper.get('[data-cy="ost-link-new-name"]').trigger('keydown', { key: 'Escape' });
      await flushPromises();
      expect(document.activeElement?.getAttribute('data-cy')).toBe('ost-link-add');
    });
  });
});
