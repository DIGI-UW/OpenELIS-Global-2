/**
 * Text as a search compares it: without accents, in lower case and trimmed,
 * so "Hématologie" and "hemato" meet.
 */
export const foldForSearch = (text) =>
  String(text ?? "")
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .toLowerCase()
    .trim();

/** Whether the text contains the typed query; a blank query matches all. */
export const matchesSearch = (text, query) => {
  const typed = foldForSearch(query);
  return !typed || foldForSearch(text).includes(typed);
};
