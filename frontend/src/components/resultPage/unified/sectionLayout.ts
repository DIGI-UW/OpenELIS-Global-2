/**
 * OGC-1021 (R2) — sticky reference-zone layout (FR-C4, decision D9).
 *
 * The expanded panel remembers which reference sections the user opened or
 * closed and reapplies that layout on every row and after reload. Stored
 * browser-local (per workstation) — no schema, no server round-trip.
 *
 * Precedence per FR-C4: remembered user choice > per-result auto-open >
 * collapsed. "Reset layout" clears every remembered choice.
 */

const STORAGE_KEY = "oe.results.sectionLayout.v1";

export type SectionLayout = Record<string, boolean>;

function readLayout(storage: Storage): SectionLayout {
  try {
    const raw = storage.getItem(STORAGE_KEY);
    const parsed = raw ? JSON.parse(raw) : {};
    return parsed && typeof parsed === "object" ? parsed : {};
  } catch {
    return {};
  }
}

export function loadSectionLayout(
  storage: Storage = window.localStorage,
): SectionLayout {
  return readLayout(storage);
}

/** Persist one explicit user choice (open or closed) for a section. */
export function rememberSectionChoice(
  sectionId: string,
  open: boolean,
  storage: Storage = window.localStorage,
): SectionLayout {
  const layout = readLayout(storage);
  layout[sectionId] = open;
  try {
    storage.setItem(STORAGE_KEY, JSON.stringify(layout));
  } catch {
    // storage full/unavailable — the session still works, just not sticky
  }
  return layout;
}

/** "Reset layout" (FR-C4): drop every remembered choice. */
export function resetSectionLayout(
  storage: Storage = window.localStorage,
): SectionLayout {
  try {
    storage.removeItem(STORAGE_KEY);
  } catch {
    // ignore
  }
  return {};
}

/**
 * Whether a section renders open. Remembered choice wins; otherwise a
 * per-result auto-open (this row has notable content); otherwise collapsed.
 */
export function isSectionOpen(
  layout: SectionLayout,
  sectionId: string,
  autoOpen: boolean,
): boolean {
  if (Object.prototype.hasOwnProperty.call(layout, sectionId)) {
    return Boolean(layout[sectionId]);
  }
  return autoOpen;
}
