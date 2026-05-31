/**
 * Shared helpers for Biorepository storage assignment display and API response handling.
 */

const STORAGE_LOCATION_KEYS = [
  "storageWell",
  "storagePath",
  "storageRoom",
  "storageFreezer",
  "storageShelf",
  "storageRack",
  "storageBox",
];

const readField = (sample, key) =>
  sample?.data?.[key] ?? sample?.[key] ?? null;

export const hasStorageLocation = (sample) =>
  STORAGE_LOCATION_KEYS.some((key) => {
    const value = readField(sample, key);
    return value !== null && value !== undefined && String(value).trim() !== "";
  });

export const deriveStoragePageStatus = (sample) => {
  const rawStatus = sample?.pageStatus || sample?.status || "PENDING";

  if (rawStatus === "COMPLETED" || rawStatus === "SKIPPED") {
    return rawStatus;
  }

  return hasStorageLocation(sample) ? "IN_PROGRESS" : rawStatus;
};

export const getStorageLocationLabel = (sample) => {
  const well = readField(sample, "storageWell");
  const path = readField(sample, "storagePath");

  if (well && path) {
    return `${path} (${well})`;
  }
  return well || path || null;
};

export function buildBiorepositoryStorageUrl(basePath, notebookId) {
  if (!notebookId) {
    return basePath;
  }
  const separator = basePath.includes("?") ? "&" : "?";
  return `${basePath}${separator}biorepositoryOnly=true&notebookId=${encodeURIComponent(notebookId)}`;
}

/**
 * Interpret bulk storage assignment API response without treating zero assignments as success.
 */
export const interpretStorageAssignmentResponse = (
  response,
  requestedCount = 0,
) => {
  const assignedCount = Number(response?.assignedCount ?? 0);
  const errors = Array.isArray(response?.errors) ? response.errors : [];
  const success = Boolean(response?.success) && assignedCount > 0;

  return {
    assignedCount,
    requestedCount: Number(response?.requestedCount ?? requestedCount),
    errors,
    success,
    errorMessage:
      response?.error ||
      (errors.length > 0 ? errors.join("; ") : null) ||
      (assignedCount === 0
        ? "No samples were assigned to storage. Verify the sample is on this page and the location is available."
        : null),
  };
};
