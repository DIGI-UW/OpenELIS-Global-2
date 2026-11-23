export const getMacroToken = (value, caret) => {
  const beforeCaret = value.slice(0, caret);
  const match = beforeCaret.match(/(?:^|\s)(\.[A-Za-z0-9_-]*)$/);
  if (!match) return null;
  const token = match[1];
  return { token, start: caret - token.length, end: caret };
};

export const filterMacroSuggestions = (macros, token) => {
  if (!token?.startsWith(".")) return [];
  const query = token.toLowerCase();
  const phraseQuery = query.slice(1);
  return macros.filter(
    (macro) =>
      macro.code.toLowerCase().startsWith(query) ||
      macro.expansionText.toLowerCase().includes(phraseQuery),
  );
};

export const expandMacroToken = (value, token, macro, requestedSuffix = "") => {
  if (!token || !macro) return null;
  const suffix =
    requestedSuffix === " " && value[token.end] === " " ? "" : requestedSuffix;
  const nextValue =
    value.slice(0, token.start) +
    macro.expansionText +
    suffix +
    value.slice(token.end);
  return {
    value: nextValue,
    caret: token.start + macro.expansionText.length + suffix.length,
  };
};
