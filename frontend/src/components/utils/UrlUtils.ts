export const safeInternalPath = (
  value: string | null | undefined,
  fallback: string | null = null,
): string | null => {
  if (!value || !value.startsWith("/")) {
    return fallback;
  }

  const internalOrigin = "https://openelis.invalid";
  try {
    const target = new URL(value, internalOrigin);
    return target.origin === internalOrigin
      ? `${target.pathname}${target.search}${target.hash}`
      : fallback;
  } catch {
    return fallback;
  }
};
