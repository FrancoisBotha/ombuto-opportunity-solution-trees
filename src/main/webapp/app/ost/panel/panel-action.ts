/*
 * Inline error reporting for the detail panel. Store actions surface failures in the store's
 * global `error` (shown as a toast); a failure started from the panel is moved to the panel
 * instead, next to what the user was editing, so it is shown once and in context.
 */
import { type InjectionKey, type Ref, inject, provide, ref } from 'vue';

import { useOstTreeStore } from '../stores/ost-tree.store';

export interface PanelErrors {
  message: Ref<string | null>;
  report: (message: string | null) => void;
}

const KEY: InjectionKey<PanelErrors> = Symbol('ostPanelErrors');

const createErrors = (): PanelErrors => {
  const message = ref<string | null>(null);
  return { message, report: m => (message.value = m) };
};

/** Called by DetailPanel: one error slot for the whole panel. */
export function providePanelErrors(): PanelErrors {
  const errors = createErrors();
  provide(KEY, errors);
  return errors;
}

/**
 * `run(action)` awaits a store action; when it reports failure (false / null) the store's error
 * message is shown in the panel and taken off the global toast. The panel itself passes its own
 * slot (a component cannot inject what it provides).
 */
export function usePanelAction(own?: PanelErrors) {
  const errors = own ?? inject(KEY, null) ?? createErrors();
  const tree = useOstTreeStore();
  async function run<T>(action: () => Promise<T>, fallback = 'Something went wrong. Your change was not saved.'): Promise<T> {
    errors.report(null);
    const result = await action();
    if (result === false || result === null) {
      errors.report(tree.error ?? fallback);
      tree.clearError();
    }
    return result;
  }
  return { run, errors };
}
