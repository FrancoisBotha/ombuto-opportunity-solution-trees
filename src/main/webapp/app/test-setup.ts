import { beforeAll } from 'vitest';

import axios from 'axios';

// Node 22+ defines its own global localStorage/sessionStorage, which shadow happy-dom's and have no
// working methods unless Node is started with --localstorage-file. Swap in an in-memory Storage.
const createMemoryStorage = (): Storage => {
  const items = new Map<string, string>();
  return {
    get length() {
      return items.size;
    },
    clear: () => items.clear(),
    getItem: key => items.get(key) ?? null,
    key: index => [...items.keys()][index] ?? null,
    removeItem: key => void items.delete(key),
    setItem: (key, value) => void items.set(key, String(value)),
  };
};
for (const name of ['localStorage', 'sessionStorage'] as const) {
  if (typeof globalThis[name]?.clear !== 'function') {
    Object.defineProperty(globalThis, name, { value: createMemoryStorage(), configurable: true, writable: true });
  }
}

beforeAll(() => {
  globalThis.location.href = 'https://jhipster.tech/';

  // Make sure axios is never executed.
  axios.interceptors.request.use(request => {
    throw new Error(`Error axios should be mocked ${request.url}`);
  });
});
