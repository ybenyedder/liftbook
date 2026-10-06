/** Minimal observable store — replaces Compose's mutableStateOf recomposition. */

export interface Store<T> {
  get(): T;
  set(v: T): void;
  subscribe(fn: () => void): () => void;
}

export function makeStore<T>(initial: T): Store<T> {
  let value = initial;
  const listeners = new Set<() => void>();
  return {
    get: () => value,
    set: (v) => {
      value = v;
      listeners.forEach((l) => l());
    },
    subscribe: (fn) => {
      listeners.add(fn);
      return () => listeners.delete(fn);
    },
  };
}

/** Integer revision counter — bump to re-render subscribers. */
export function makeRev() {
  const store = makeStore(0);
  return {
    bump: () => store.set(store.get() + 1),
    subscribe: store.subscribe,
    get: () => store.get(),
    get value() {
      return store.get();
    },
  };
}

import { useSyncExternalStore } from "react";

/** Re-render the calling component whenever the store/rev changes. */
export function useStore<T>(store: { subscribe(fn: () => void): () => void; get(): T }): T {
  return useSyncExternalStore(store.subscribe, store.get, store.get);
}
