/**
 * How a provider's name reads when a screen has room for a single string:
 * the title's abbreviation, then the given name, then the family name, each
 * part dropped cleanly when absent so nothing renders a leading space or a
 * stray separator (OGC-1223, FR-11).
 *
 * The backend has the same helper in ProviderDisplayName.java; both exist
 * because some names are assembled server-side for reports and dropdowns and
 * some client-side for the order screens.
 */

const parts = (...values: (string | null | undefined)[]): string[] =>
  values
    .map((value) => (value == null ? "" : String(value).trim()))
    .filter((value) => value.length > 0);

/** `Dr John Kila` — the form a person reads. */
export const titledProviderName = (
  title?: string | null,
  firstName?: string | null,
  lastName?: string | null,
): string => parts(title, firstName, lastName).join(" ");

/** `Kila, Dr John` — the form an administrator scans in a sorted list. */
export const titledProviderNameFamilyFirst = (
  title?: string | null,
  firstName?: string | null,
  lastName?: string | null,
): string => {
  const given = parts(title, firstName).join(" ");
  const family = parts(lastName).join("");
  if (!family) {
    return given;
  }
  return given ? `${family}, ${given}` : family;
};

/**
 * The titled name of a provider-shaped object from the API, which carries
 * either a resolved abbreviation or the raw code.
 */
export interface TitledProvider {
  titleCode?: string | null;
  titleAbbreviation?: string | null;
  firstName?: string | null;
  lastName?: string | null;
}

export const providerDisplayName = (
  provider?: TitledProvider | null,
): string => {
  if (!provider) {
    return "";
  }
  return titledProviderName(
    provider.titleAbbreviation || provider.titleCode,
    provider.firstName,
    provider.lastName,
  );
};

/**
 * Whether a name field looks like it carries a rank rather than a name, so the
 * form can suggest moving it to the Title field (OGC-1223, FR-7). This only
 * warns: a clinician really called "Doctor" must still be able to save.
 */
const TITLE_TOKENS = [
  "dr",
  "dr.",
  "doctor",
  "prof",
  "prof.",
  "professor",
  "sr",
  "sr.",
  "sister",
  "mr",
  "mr.",
  "mrs",
  "mrs.",
  "ms",
  "ms.",
  "heo",
  "rmo",
];

export const looksLikeATitle = (value?: string | null): boolean => {
  if (!value) {
    return false;
  }
  const first = String(value).trim().split(/\s+/)[0] || "";
  return TITLE_TOKENS.includes(first.toLowerCase());
};
