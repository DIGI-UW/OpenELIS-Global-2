export const safeInternalPath = (
  value: string | null | undefined,
  fallback: string | null = null,
): string | null =>
  value && value.startsWith("/") && !value.startsWith("//") ? value : fallback;
