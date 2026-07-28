import { useRef, useLayoutEffect } from "react";

/**
 * Keep the latest callback reachable through a stable ref so effects and
 * event handlers can fire it without listing the callback in their
 * dependency arrays.
 *
 * The scenario this solves: a parent renders the consumer with an
 * inline arrow (`onChange={(state) => ...}`) every render. If the
 * consumer's effect depends on `onChange`, the effect runs on every
 * parent render, which — if the effect itself causes a parent
 * re-render — creates a render loop. Returning a stable ref breaks
 * the loop: the effect depends only on meaningful state, and still
 * calls the most recent callback via `ref.current`.
 *
 * Uses useLayoutEffect so the ref is updated synchronously before any
 * effect that consumes it reads it. useEffect would let an effect fire
 * with a stale callback on the very first render after a prop change.
 */
export default function useLatestCallback(callback) {
  const ref = useRef(callback);
  useLayoutEffect(() => {
    ref.current = callback;
  });
  return ref;
}
