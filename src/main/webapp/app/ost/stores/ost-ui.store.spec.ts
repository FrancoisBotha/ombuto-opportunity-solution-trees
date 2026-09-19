import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { createPinia, setActivePinia } from 'pinia';

import { collapsedStorageKey, lastTeamStorageKey, readLastTeam, useOstUiStore, writeLastTeam } from './ost-ui.store';

describe('OST UI store', () => {
  let ui: ReturnType<typeof useOstUiStore>;

  beforeEach(() => {
    localStorage.clear();
    setActivePinia(createPinia());
    ui = useOstUiStore();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('collapse persistence', () => {
    it('writes the collapsed map per user and team, keeping only collapsed entries', () => {
      ui.restoreCollapsed('user', 7);
      ui.setCollapsed('outcome-1', true);
      ui.setCollapsed('solution-2', true);
      ui.setCollapsed('solution-2', false);
      expect(JSON.parse(localStorage.getItem(collapsedStorageKey('user', 7))!)).toEqual({ 'outcome-1': true });
      expect(localStorage.getItem(collapsedStorageKey('user', 8))).toBeNull();
      expect(localStorage.getItem(collapsedStorageKey('admin', 7))).toBeNull();

      ui.restoreCollapsed('admin', 7);
      expect(ui.collapsed).toEqual({});
      ui.toggleCollapse('opportunity-3');
      expect(JSON.parse(localStorage.getItem(collapsedStorageKey('admin', 7))!)).toEqual({ 'opportunity-3': true });

      ui.restoreCollapsed('user', 7);
      expect(ui.collapsed).toEqual({ 'outcome-1': true });
    });

    it('does not persist before a tree is loaded', () => {
      const setItem = vi.spyOn(Storage.prototype, 'setItem');
      ui.setCollapsed('outcome-1', true);
      expect(ui.collapsed).toEqual({ 'outcome-1': true });
      expect(setItem).not.toHaveBeenCalled();
    });

    it('survives storage that throws on read', () => {
      vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
        throw new Error('SecurityError');
      });
      ui.restoreCollapsed('user', 7);
      expect(ui.collapsed).toEqual({});
      expect(readLastTeam('user')).toBeNull();
    });

    it('survives storage that throws on write, keeping the in-memory state', () => {
      vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
        throw new Error('QuotaExceededError');
      });
      ui.restoreCollapsed('user', 7);
      expect(() => ui.setCollapsed('outcome-1', true)).not.toThrow();
      expect(ui.collapsed).toEqual({ 'outcome-1': true });
      expect(() => writeLastTeam('user', 7)).not.toThrow();
    });

    it('ignores persisted values that are not an object map', () => {
      localStorage.setItem(collapsedStorageKey('user', 7), JSON.stringify(['outcome-1']));
      ui.restoreCollapsed('user', 7);
      expect(ui.collapsed).toEqual({});
    });

    it('prunes entries for nodes that no longer exist and persists the result', () => {
      ui.restoreCollapsed('user', 7);
      ui.setCollapsed('outcome-1', true);
      ui.setCollapsed('gone-1', true);
      ui.pruneCollapsed(new Set(['outcome-1']));
      expect(ui.collapsed).toEqual({ 'outcome-1': true });
      expect(JSON.parse(localStorage.getItem(collapsedStorageKey('user', 7))!)).toEqual({ 'outcome-1': true });
    });
  });

  describe('forgetNodes', () => {
    it('drops every reference to removed nodes and persists the collapse change', () => {
      ui.restoreCollapsed('user', 7);
      ui.setCollapsed('solution-1', true);
      ui.setCollapsed('outcome-1', true);
      ui.select('assumption-1');
      ui.startEditing('assumption-1');
      ui.openChat('solution-1');
      ui.openAddMenu('solution-1');
      ui.askDelete('solution-1');
      ui.setDropTarget('assumption-1');
      ui.setProduct('product-1');

      ui.forgetNodes(new Set(['solution-1', 'assumption-1', 'product-1']));

      expect(ui.selectedId).toBeNull();
      expect(ui.editingId).toBeNull();
      expect(ui.chatId).toBeNull();
      expect(ui.addMenuId).toBeNull();
      expect(ui.confirmId).toBeNull();
      expect(ui.dropTargetId).toBeNull();
      expect(ui.productId).toBe('all');
      expect(ui.collapsed).toEqual({ 'outcome-1': true });
      expect(JSON.parse(localStorage.getItem(collapsedStorageKey('user', 7))!)).toEqual({ 'outcome-1': true });
    });

    it('leaves unrelated state alone', () => {
      ui.select('outcome-1');
      ui.setProduct('product-2');
      const setItem = vi.spyOn(Storage.prototype, 'setItem');
      ui.forgetNodes(new Set(['solution-9']));
      expect(ui.selectedId).toBe('outcome-1');
      expect(ui.productId).toBe('product-2');
      expect(setItem).not.toHaveBeenCalled();
    });
  });

  describe('last team', () => {
    it('reads back what was written, per login', () => {
      writeLastTeam('user', 7);
      writeLastTeam(undefined, 9);
      expect(readLastTeam('user')).toBe(7);
      expect(readLastTeam('admin')).toBeNull();
      expect(localStorage.getItem(lastTeamStorageKey(null))).toBe('9');
      localStorage.setItem(lastTeamStorageKey('user'), 'abc');
      expect(readLastTeam('user')).toBeNull();
    });
  });

  it('reset clears team-scoped state', () => {
    ui.restoreCollapsed('user', 7);
    ui.setCollapsed('outcome-1', true);
    ui.select('outcome-1');
    ui.reset();
    expect(ui.collapsed).toEqual({});
    expect(ui.collapsedKey).toBeNull();
    expect(ui.selectedId).toBeNull();
  });
});
