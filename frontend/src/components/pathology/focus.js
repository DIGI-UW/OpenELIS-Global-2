/**
 * Focuses the first enabled control the selectors name. With `onlyFrom`, focus
 * moves only from the body, an unmounted control or those initiators, and with
 * `unlessHeld` only from the first two.
 */
export function focusFirstOf(
  targets,
  { unlessHeld = false, onlyFrom = null } = {},
) {
  setTimeout(() => {
    const held = document.activeElement;
    const nothingHeld = !held || held === document.body || !held.isConnected;
    if (unlessHeld && !nothingHeld) {
      return;
    }
    if (onlyFrom && !nothingHeld && !onlyFrom.includes(held)) {
      return;
    }
    for (const target of [].concat(targets)) {
      const element = document.querySelector(target);
      if (element && !element.disabled) {
        element.focus();
        return;
      }
    }
  });
}
