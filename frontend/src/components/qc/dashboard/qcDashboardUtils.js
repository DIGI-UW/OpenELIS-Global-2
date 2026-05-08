/**
 * QC Dashboard Shared Utilities
 *
 * Common helper functions for QC dashboard components.
 */

/**
 * Maps a compliance color (GREEN/YELLOW/RED) to a Carbon Tag type.
 */
export const getComplianceTagType = (complianceColor) => {
  switch (complianceColor?.toUpperCase()) {
    case "GREEN":
      return "green";
    case "YELLOW":
      return "warm-gray";
    case "RED":
      return "red";
    default:
      return "gray";
  }
};

/**
 * Returns the display label i18n key for a compliance color.
 */
export const getComplianceLabelKey = (complianceColor) => {
  switch (complianceColor?.toUpperCase()) {
    case "GREEN":
      return "qc.dashboard.instruments.status.green";
    case "YELLOW":
      return "qc.dashboard.instruments.status.yellow";
    case "RED":
      return "qc.dashboard.instruments.status.red";
    default:
      return "qc.dashboard.instruments.status.green";
  }
};

/**
 * Maps a violation severity to a Carbon Tag type.
 */
export const getSeverityTagType = (severity) => {
  return severity === "REJECTION" ? "red" : "warm-gray";
};

/**
 * Returns a Carbon Tag type based on a z-score (sigma value).
 * >= 4σ  → green (good)
 * 3-4σ   → warm-gray (warning)
 * < 3σ   → red (poor)
 */
export const getZScoreBadgeType = (zScore) => {
  if (zScore == null) return "gray";
  const val = Math.abs(parseFloat(zScore));
  if (val >= 4) return "green";
  if (val >= 3) return "warm-gray";
  return "red";
};

/**
 * Formats an ISO timestamp to a localized date/time string.
 */
export const formatTimestamp = (isoString) => {
  if (!isoString) return "—";
  const date = new Date(isoString);
  if (isNaN(date.getTime())) return "—";
  return date.toLocaleString(undefined, {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
};

/**
 * Filters an array of items by a time period relative to now.
 * period values: "24h", "72h", "7d", "30d", "all"
 */
export const filterByTimePeriod = (items, dateField, period) => {
  if (period === "all" || !period) return items;

  const now = new Date();
  let cutoff;
  switch (period) {
    case "24h":
      cutoff = new Date(now.getTime() - 24 * 60 * 60 * 1000);
      break;
    case "72h":
      cutoff = new Date(now.getTime() - 72 * 60 * 60 * 1000);
      break;
    case "7d":
      cutoff = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
      break;
    case "30d":
      cutoff = new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
      break;
    default:
      return items;
  }

  return items.filter((item) => {
    const itemDate = new Date(item[dateField]);
    return itemDate >= cutoff;
  });
};
