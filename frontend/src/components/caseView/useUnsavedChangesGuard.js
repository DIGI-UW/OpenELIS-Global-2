import { useEffect } from "react";

/**
 * Warns before the browser itself discards a dirty case-view form: a reload,
 * a closed tab, a typed URL, a back button out of the application.
 *
 * This is half of the unsaved-changes warning and cannot be the whole of it.
 * A browser only fires beforeunload when the document is being torn down, and
 * moving between screens of a single-page application never tears the
 * document down, so this listener is silent for the commonest way of all to
 * lose a dirty case: clicking something in the side navigation. That half is
 * covered by the router Prompt in CaseActionBar, which in turn cannot see a
 * reload or a closed tab because no route change happens. Each guard covers
 * precisely what the other cannot, so both are needed and neither is
 * redundant.
 *
 * The action bar is the one place on every anatomic-pathology case view that
 * knows whether the form is dirty, so the warning is wired there rather than
 * left for each screen to remember to add on its own. The listener exists
 * only while there is something to lose: it is added when the form becomes
 * dirty and removed the moment it is saved or discarded, so a clean form
 * never blocks a navigation it has no reason to block.
 */
export default function useUnsavedChangesGuard(dirty) {
  useEffect(() => {
    if (!dirty) {
      return undefined;
    }

    const handleBeforeUnload = (event) => {
      event.preventDefault();
      event.returnValue = "";
    };

    window.addEventListener("beforeunload", handleBeforeUnload);
    return () => {
      window.removeEventListener("beforeunload", handleBeforeUnload);
    };
  }, [dirty]);
}
