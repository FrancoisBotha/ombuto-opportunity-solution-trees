/*
 * Inline error reporting for the detail panel. Store actions surface failures in the store's
 * global `error` (shown as a toast); a failure started from the panel is moved to the panel
 * instead, next to what the user was editing, so it is shown once and in context.
 *
 * The slot remembers which node the error belongs to: the panel shows it only while that node is
 * selected. A failure that comes back after the user selected another node stays on the global
 * toast, labelled with the node it was about, so it is never shown against the wrong node.
 */
import { type InjectionKey, type Ref, inject, provide, ref } from 'vue';

import { useOstTreeStore } from '../stores/ost-tree.store';
import { useOstUiStore } from '../stores/ost-ui.store';

export interface PanelErrors {
  message: Ref<string | null>;
  /** node key the message is about (null: not tied to a node) */
  nodeKey: Ref<string | null>;
  report: (message: string | null, nodeKey?: string | null) => void;
}

const KEY: InjectionKey<PanelErrors> = Symbol('ostPanelErrors');

const createErrors = (): PanelErrors => {
  const message = ref<string | null>(null);
  const nodeKey = ref<string | null>(null);
  return {
    message,
    nodeKey,
    report: (m, key = null) => {
      message.value = m;
      nodeKey.value = m === null ? null : key;
    },
  };
};

/** Called by DetailPanel: one error slot for the whole panel. */
export function providePanelErrors(): PanelErrors {
  const errors = createErrors();
  provide(KEY, errors);
  return errors;
}

/**
 * `run(action)` awaits a store action; when it reports failure (false / null) the store's error
 * message is shown in the panel and taken off the global toast — if the node selected when the
 * action started is still selected. The panel itself passes its own slot (a component cannot
 * inject what it provides).
 */
export function usePanelAction(own?: PanelErrors) {
  const errors = own ?? inject(KEY, null) ?? createErrors();
  const tree = useOstTreeStore();
  const ui = useOstUiStore();
  async function run<T>(action: () => Promise<T>, fallback = 'Something went wrong. Your change was not saved.'): Promise<T> {
    const key = ui.selectedId;
    errors.report(null);
    const result = await action();
    if (result === false || result === null) {
      const message = tree.error ?? fallback;
      if (ui.selectedId === key) {
        errors.report(message, key);
        tree.clearError();
      } else {
        // The user moved on: leave it on the toast, saying which node it was about.
        const title = tree.byId(key)?.title;
        tree.error = title ? `“${title}”: ${message}` : message;
      }
    }
    return result;
  }
  return { run, errors };
}
