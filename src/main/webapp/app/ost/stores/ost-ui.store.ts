import { defineStore } from 'pinia';

import type { PanelTab } from '../domain/derive';
import type { NodeType } from '../domain/types';

/** localStorage key for the collapsed map of one user in one team. */
export const collapsedStorageKey = (login: string, teamId: number) => `ost.collapsed.${login}.${teamId}`;
/** localStorage key for the last team a user opened (used by /trees). */
export const lastTeamStorageKey = (login: string | null | undefined) => `ost.lastTeam.${login ?? 'anonymous'}`;

export function readLastTeam(login: string | null | undefined): number | null {
  try {
    const raw = localStorage.getItem(lastTeamStorageKey(login));
    const id = raw ? Number(raw) : NaN;
    return Number.isInteger(id) && id > 0 ? id : null;
  } catch {
    return null;
  }
}

export function writeLastTeam(login: string | null | undefined, teamId: number): void {
  try {
    localStorage.setItem(lastTeamStorageKey(login), String(teamId));
  } catch {
    // storage unavailable (private mode, quota) — the preference is a convenience only
  }
}

export interface OstUiState {
  selectedId: string | null;
  panelTab: PanelTab;
  leftOpen: boolean;
  rightOpen: boolean;
  /** 'all' or a product node key */
  productId: string | 'all';
  collapsed: Record<string, boolean>;
  /** storage key the collapsed map persists to; null until a tree is loaded */
  collapsedKey: string | null;
  query: string;
  hiddenTypes: Partial<Record<NodeType, boolean>>;
  /** palette type armed by click (click-to-arm-then-click) */
  tool: NodeType | null;
  /** palette type being dragged, with the pointer position for the ghost chip */
  paletteDrag: { type: NodeType; x: number; y: number } | null;
  dropTargetId: string | null;
  addMenuId: string | null;
  editingId: string | null;
  chatId: string | null;
  confirmId: string | null;
  /** Asks the canvas to centre a node (panel breadcrumb / child list); seq makes repeats observable. */
  centreRequest: { key: string; seq: number } | null;
}

const initialState = (): OstUiState => ({
  selectedId: null,
  panelTab: 'detail',
  leftOpen: true,
  rightOpen: true,
  productId: 'all',
  collapsed: {},
  collapsedKey: null,
  query: '',
  hiddenTypes: {},
  tool: null,
  paletteDrag: null,
  dropTargetId: null,
  addMenuId: null,
  editingId: null,
  chatId: null,
  confirmId: null,
  centreRequest: null,
});

/** View state of the OST screens. Never holds tree data (see ost-tree.store.ts). */
export const useOstUiStore = defineStore('ostUi', {
  state: initialState,
  actions: {
    /** Clears everything tied to one team (selection, scope, transient tool state). */
    reset() {
      Object.assign(this, initialState());
    },

    // ---- selection / panel ------------------------------------------------------------------
    select(key: string | null) {
      this.selectedId = key;
      if (key) this.rightOpen = true;
      this.addMenuId = null;
    },
    requestCentre(key: string) {
      this.centreRequest = { key, seq: (this.centreRequest?.seq ?? 0) + 1 };
    },
    setPanelTab(tab: PanelTab) {
      this.panelTab = tab;
    },
    setLeftOpen(open: boolean) {
      this.leftOpen = open;
    },
    toggleLeft() {
      this.leftOpen = !this.leftOpen;
    },
    setRightOpen(open: boolean) {
      this.rightOpen = open;
    },
    toggleRight() {
      this.rightOpen = !this.rightOpen;
    },

    // ---- scope / filters ------------------------------------------------------------------
    setProduct(productId: string | 'all') {
      this.productId = productId || 'all';
    },
    setQuery(query: string) {
      this.query = query;
    },
    toggleType(type: NodeType) {
      this.hiddenTypes = { ...this.hiddenTypes, [type]: !this.hiddenTypes[type] };
    },
    showAllTypes() {
      this.hiddenTypes = {};
    },

    // ---- collapse (persisted per user + team) ------------------------------------------------
    restoreCollapsed(login: string, teamId: number) {
      this.collapsedKey = collapsedStorageKey(login, teamId);
      let restored: Record<string, boolean> = {};
      try {
        const raw = localStorage.getItem(this.collapsedKey);
        const parsed = raw ? JSON.parse(raw) : {};
        if (parsed && typeof parsed === 'object' && !Array.isArray(parsed)) restored = parsed;
      } catch {
        restored = {};
      }
      this.collapsed = restored;
    },
    persistCollapsed() {
      if (!this.collapsedKey) return;
      try {
        const compact = Object.fromEntries(Object.entries(this.collapsed).filter(([, v]) => v));
        localStorage.setItem(this.collapsedKey, JSON.stringify(compact));
      } catch {
        // storage unavailable — collapse still works for this session
      }
    },
    setCollapsed(key: string, collapsed: boolean) {
      this.collapsed = { ...this.collapsed, [key]: collapsed };
      this.persistCollapsed();
    },
    toggleCollapse(key: string) {
      this.setCollapsed(key, !this.collapsed[key]);
    },
    /** Drops collapse entries for nodes that no longer exist. */
    pruneCollapsed(existing: Set<string>) {
      const next = Object.fromEntries(Object.entries(this.collapsed).filter(([k]) => existing.has(k)));
      if (Object.keys(next).length !== Object.keys(this.collapsed).length) {
        this.collapsed = next;
        this.persistCollapsed();
      }
    },

    // ---- palette / drag -----------------------------------------------------------------------
    armTool(type: NodeType | null) {
      this.tool = this.tool === type ? null : type;
    },
    startPaletteDrag(type: NodeType, x: number, y: number) {
      this.paletteDrag = { type, x, y };
    },
    movePaletteDrag(x: number, y: number) {
      if (this.paletteDrag) this.paletteDrag = { ...this.paletteDrag, x, y };
    },
    endPaletteDrag() {
      this.paletteDrag = null;
      this.dropTargetId = null;
    },
    setDropTarget(key: string | null) {
      this.dropTargetId = key;
    },

    // ---- menus / editing / overlays --------------------------------------------------------
    openAddMenu(key: string | null) {
      this.addMenuId = key;
    },
    startEditing(key: string) {
      this.editingId = key;
    },
    stopEditing() {
      this.editingId = null;
    },
    openChat(key: string) {
      this.chatId = key;
    },
    closeChat() {
      this.chatId = null;
    },
    askDelete(key: string) {
      this.confirmId = key;
    },
    cancelDelete() {
      this.confirmId = null;
    },

    /** Forget every reference to nodes that were removed (after a cascade delete). */
    forgetNodes(keys: Set<string>) {
      if (this.selectedId && keys.has(this.selectedId)) this.selectedId = null;
      if (this.editingId && keys.has(this.editingId)) this.editingId = null;
      if (this.chatId && keys.has(this.chatId)) this.chatId = null;
      if (this.addMenuId && keys.has(this.addMenuId)) this.addMenuId = null;
      if (this.confirmId && keys.has(this.confirmId)) this.confirmId = null;
      if (this.dropTargetId && keys.has(this.dropTargetId)) this.dropTargetId = null;
      if (this.productId !== 'all' && keys.has(this.productId)) this.productId = 'all';
      if (Object.keys(this.collapsed).some(k => keys.has(k))) {
        this.collapsed = Object.fromEntries(Object.entries(this.collapsed).filter(([k]) => !keys.has(k)));
        this.persistCollapsed();
      }
    },
  },
});
