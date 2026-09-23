/**
 * Which series the trend graph is showing.
 *
 * A graph plots one line, and the test name no longer identifies one: the
 * unified test catalogue lets a test hold a component per analyte and run on
 * several specimens, so "Albumin" can be four different measurements for the
 * same patient. The key that picks one out is the same triple the timeline
 * rows are built from — test, specimen, component.
 *
 * It travels in the URL hash so the graph survives a refresh and can be linked
 * to, the way `#trendline/<testId>` did before the timeline was rewritten and
 * the link that produced it was dropped.
 */
export interface TrendKey {
  testId: string;
  sampleTypeId?: string;
  componentId?: string;
}

const PREFIX = "#trendline/";

/** The hash a link to this series should carry. */
export const trendHash = (key: TrendKey): string => {
  const params = new URLSearchParams({ testId: key.testId });
  if (key.sampleTypeId) {
    params.set("sampleTypeId", key.sampleTypeId);
  }
  if (key.componentId) {
    params.set("componentId", key.componentId);
  }
  return PREFIX + params.toString();
};

/**
 * The series a hash names, or null when it names none. A bare
 * `#trendline/<testId>` — the shape links used before this key existed — is
 * still read as that test, unscoped.
 */
export const parseTrendHash = (hash: string): TrendKey | null => {
  const at = (hash || "").indexOf(PREFIX);
  if (at === -1) {
    return null;
  }
  const raw = hash.slice(at + PREFIX.length);
  if (!raw) {
    return null;
  }
  if (!raw.includes("=")) {
    return { testId: decodeURIComponent(raw) };
  }
  const params = new URLSearchParams(raw);
  const testId = params.get("testId");
  if (!testId) {
    return null;
  }
  return {
    testId,
    sampleTypeId: params.get("sampleTypeId") || undefined,
    componentId: params.get("componentId") || undefined,
  };
};

/** The query the result-tree endpoint needs to return exactly this series. */
export const trendQuery = (patientUuid: string, key: TrendKey): string => {
  const params = new URLSearchParams({
    patientId: patientUuid,
    testId: key.testId,
  });
  if (key.sampleTypeId) {
    params.set("sampleTypeId", key.sampleTypeId);
  }
  if (key.componentId) {
    params.set("componentId", key.componentId);
  }
  return `/rest/test-result-tree?${params.toString()}`;
};
