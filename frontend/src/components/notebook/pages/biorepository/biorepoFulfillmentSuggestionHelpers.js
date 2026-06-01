/**
 * Shared helpers for Stage 5 fulfillment workbench suggestion review.
 */

export const SUGGESTION_STATUS = {
  EXACT_MATCH: "EXACT_MATCH",
  EXACT_MATCH_TYPE_MISMATCH: "EXACT_MATCH_TYPE_MISMATCH",
  REVIEW_SUGGESTIONS: "REVIEW_SUGGESTIONS",
  NO_CANDIDATE: "NO_CANDIDATE",
  NO_CRITERIA: "NO_CRITERIA",
};

export const SUGGESTIONS_LOAD_STATE = {
  IDLE: "idle",
  LOADING: "loading",
  LOADED: "loaded",
  ERROR: "error",
};

/** Stable string key for suggestionsByItemId map lookups. */
export const normalizeRetrievalItemId = (id) =>
  id == null || id === "" ? null : String(id);

export const lookupSuggestion = (suggestionsByItemId, itemId) => {
  const key = normalizeRetrievalItemId(itemId);
  if (!key || !suggestionsByItemId) {
    return null;
  }
  return suggestionsByItemId[key] ?? null;
};

export const isValidSuggestionApiMap = (data) => {
  if (data == null || typeof data !== "object" || Array.isArray(data) || data.error) {
    return false;
  }
  const status = data.status ?? data.statusCode;
  if (status != null && Number(status) >= 400) {
    return false;
  }
  return true;
};

export const mergeSuggestionMaps = (primary, fallback) => {
  const merged = { ...(fallback || {}) };
  Object.entries(primary || {}).forEach(([key, value]) => {
    const normalizedKey = normalizeRetrievalItemId(key);
    if (normalizedKey && value) {
      merged[normalizedKey] = value;
    }
  });
  return merged;
};

export const buildBulkSuggestionRequest = (itemIds, identityLookups) => ({
  itemIds: Array.isArray(itemIds) ? itemIds.filter((id) => id != null) : [],
  identityLookups: Array.isArray(identityLookups) ? identityLookups : [],
});

export const buildSuggestionSummaryFromResults = (results) => {
  const normalized = Array.isArray(results) ? results : [];
  const exactMatches = normalized.filter((sample) => sample?.exactIdentityMatch);
  if (exactMatches.length > 0) {
    const topSuggestion = exactMatches[0];
    const hasMismatch =
      topSuggestion?.sampleTypeMatchesRequested === false ||
      topSuggestion?.mismatchReason === "TYPE_MISMATCH";
    return {
      status: hasMismatch
        ? SUGGESTION_STATUS.EXACT_MATCH_TYPE_MISMATCH
        : SUGGESTION_STATUS.EXACT_MATCH,
      results: exactMatches,
      topSuggestion,
      exactMatchFound: true,
      fallbackUsed: false,
      noExactMatch: false,
      summary: {
        sampleIdentity: formatTopCandidateIdentity(topSuggestion),
        availableQuantity:
          topSuggestion?.remainingQuantity ?? topSuggestion?.quantity ?? null,
        availableUnitOfMeasure: topSuggestion?.unitOfMeasure || null,
        samplePath: formatSamplePath(topSuggestion),
        matchReason: topSuggestion?.matchReason || null,
        matchScore: topSuggestion?.matchScore ?? null,
        sampleTypeMatchesRequested: topSuggestion?.sampleTypeMatchesRequested ?? null,
        mismatchReason: topSuggestion?.mismatchReason || null,
      },
    };
  }
  if (normalized.length > 0) {
    const fallbackUsed = normalized.some((sample) => sample?.fallbackUsed);
    const topSuggestion = normalized[0];
    return {
      status: SUGGESTION_STATUS.REVIEW_SUGGESTIONS,
      results: normalized,
      topSuggestion,
      exactMatchFound: false,
      fallbackUsed,
      noExactMatch: fallbackUsed,
      summary: {
        sampleIdentity: formatTopCandidateIdentity(topSuggestion),
        availableQuantity:
          topSuggestion?.remainingQuantity ?? topSuggestion?.quantity ?? null,
        availableUnitOfMeasure: topSuggestion?.unitOfMeasure || null,
        samplePath: formatSamplePath(topSuggestion),
        matchReason: topSuggestion?.matchReason || null,
        matchScore: topSuggestion?.matchScore ?? null,
        sampleTypeMatchesRequested: topSuggestion?.sampleTypeMatchesRequested ?? null,
        mismatchReason: topSuggestion?.mismatchReason || null,
      },
    };
  }
  return {
    status: SUGGESTION_STATUS.NO_CANDIDATE,
    results: [],
    topSuggestion: null,
    exactMatchFound: false,
    fallbackUsed: false,
    noExactMatch: false,
    summary: null,
  };
};

export const mapApiSuggestionEntry = (apiEntry) => {
  if (!apiEntry) {
    return {
      status: SUGGESTION_STATUS.NO_CANDIDATE,
      results: [],
      topSuggestion: null,
      exactMatchFound: false,
      fallbackUsed: false,
      noExactMatch: false,
      summary: apiEntry?.summary || null,
    };
  }

  const status = apiEntry.suggestionStatus || SUGGESTION_STATUS.NO_CANDIDATE;
  const candidates = Array.isArray(apiEntry.candidates) ? apiEntry.candidates : [];
  const topSuggestion = apiEntry.topCandidate || candidates[0] || null;

  if (status === SUGGESTION_STATUS.NO_CRITERIA) {
    return {
      status: SUGGESTION_STATUS.NO_CRITERIA,
      results: [],
      topSuggestion: null,
      exactMatchFound: false,
      fallbackUsed: false,
      noExactMatch: false,
    };
  }

  if (
    status === SUGGESTION_STATUS.EXACT_MATCH ||
    status === SUGGESTION_STATUS.EXACT_MATCH_TYPE_MISMATCH
  ) {
    return {
      status,
      results: candidates.length > 0 ? candidates : topSuggestion ? [topSuggestion] : [],
      topSuggestion,
      exactMatchFound: true,
      fallbackUsed: Boolean(apiEntry.fallbackUsed),
      noExactMatch: Boolean(apiEntry.noExactMatch),
      summary: apiEntry.summary || null,
    };
  }

  if (status === SUGGESTION_STATUS.REVIEW_SUGGESTIONS) {
    return {
      status: SUGGESTION_STATUS.REVIEW_SUGGESTIONS,
      results: candidates,
      topSuggestion,
      exactMatchFound: false,
      fallbackUsed: Boolean(apiEntry.fallbackUsed),
      noExactMatch: Boolean(apiEntry.noExactMatch),
      summary: apiEntry.summary || null,
    };
  }

  return {
    status: SUGGESTION_STATUS.NO_CANDIDATE,
    results: [],
    topSuggestion: null,
    exactMatchFound: false,
    fallbackUsed: Boolean(apiEntry.fallbackUsed),
    noExactMatch: Boolean(apiEntry.noExactMatch),
    summary: apiEntry.summary || null,
  };
};

export const mapApiSuggestionResponse = (apiMap) => {
  if (!isValidSuggestionApiMap(apiMap)) {
    return {};
  }
  return Object.entries(apiMap).reduce((acc, [itemId, entry]) => {
    const key = normalizeRetrievalItemId(entry?.retrievalItemId ?? itemId);
    if (key) {
      acc[key] = mapApiSuggestionEntry(entry);
    }
    return acc;
  }, {});
};

export const formatTopCandidateIdentity = (sample) => {
  if (!sample) {
    return null;
  }
  const accession = sample.accessionNumber?.trim();
  const barcode = sample.barcode?.trim();
  if (accession && barcode && accession !== barcode) {
    return `${accession} · ${barcode}`;
  }
  return accession || barcode || sample.externalId || null;
};

export const getSuggestionTagProps = (status) => {
  switch (status) {
  case SUGGESTION_STATUS.EXACT_MATCH:
    return { type: "green" };
  case SUGGESTION_STATUS.EXACT_MATCH_TYPE_MISMATCH:
    return { type: "magenta" };
  case SUGGESTION_STATUS.REVIEW_SUGGESTIONS:
    return { type: "blue" };
  case SUGGESTION_STATUS.NO_CRITERIA:
    return { type: "purple" };
  case SUGGESTION_STATUS.NO_CANDIDATE:
  default:
    return { type: "gray" };
  }
};

export const getSuggestionStatusMessageId = (status) => {
  switch (status) {
  case SUGGESTION_STATUS.EXACT_MATCH:
    return "biorepository.retrieval.workbench.suggestionState.exact";
  case SUGGESTION_STATUS.EXACT_MATCH_TYPE_MISMATCH:
    return "biorepository.retrieval.workbench.suggestionState.exactMismatch";
  case SUGGESTION_STATUS.REVIEW_SUGGESTIONS:
    return "biorepository.retrieval.workbench.suggestionState.review";
  case SUGGESTION_STATUS.NO_CRITERIA:
    return "biorepository.retrieval.workbench.suggestionState.noCriteria";
  case SUGGESTION_STATUS.NO_CANDIDATE:
  default:
    return "biorepository.retrieval.workbench.suggestionState.noCandidate";
  }
};

export const getSuggestionStatusDefaultMessage = (status, count) => {
  switch (status) {
  case SUGGESTION_STATUS.EXACT_MATCH:
    return "Exact match found";
  case SUGGESTION_STATUS.EXACT_MATCH_TYPE_MISMATCH:
    return "Exact match with type mismatch";
  case SUGGESTION_STATUS.REVIEW_SUGGESTIONS:
    return count > 1 ? `Review suggestions (${count})` : "Review suggestions";
  case SUGGESTION_STATUS.NO_CRITERIA:
    return "Add accession, barcode, type, origin, or project";
  case SUGGESTION_STATUS.NO_CANDIDATE:
  default:
    return "No candidate found";
  }
};

const hasStoredSampleLocation = (bioSample) =>
  Boolean(
    bioSample?.hierarchicalPath ||
      bioSample?.samplePath ||
      bioSample?.storageLocation ||
      bioSample?.location,
  );

/**
 * Mirrors backend {@code resolveFulfillmentAvailableQuantity} for client-side attach checks.
 */
export const resolveAttachAvailableQuantity = (bioSample, quantityHint = null) => {
  const hint =
    quantityHint != null && !Number.isNaN(Number(quantityHint)) ? Number(quantityHint) : null;

  if (bioSample) {
    const remaining = bioSample.remainingQuantity ?? bioSample.availableQuantity;
    if (remaining != null && Number(remaining) > 0) {
      return Number(remaining);
    }
    if (hint != null && hint > 0) {
      return hint;
    }
    const quantity = bioSample.quantity;
    if (quantity != null && Number(quantity) > 0) {
      return Number(quantity);
    }
    if (hasStoredSampleLocation(bioSample)) {
      return 1;
    }
    return null;
  }

  if (hint != null && hint > 0) {
    return hint;
  }
  return null;
};

export const resolveDefaultAttachQuantity = (referenceItem, bioSample) => {
  const requested =
    referenceItem?.quantityRequested != null &&
    !Number.isNaN(Number(referenceItem.quantityRequested))
      ? Number(referenceItem.quantityRequested)
      : null;
  const available = resolveAttachAvailableQuantity(bioSample, requested);

  if (requested != null && requested > 0) {
    if (available != null && available > 0) {
      return Math.min(requested, available);
    }
    return requested;
  }
  if (available != null && available > 0) {
    return available;
  }
  return null;
};

export const validateAttachQuantity = (requested, available, entered, options = {}) => {
  const { bioSample } = options;
  const requestedNum =
    requested != null && !Number.isNaN(Number(requested)) ? Number(requested) : null;
  const enteredProvided = entered !== "" && entered != null && !Number.isNaN(Number(entered));
  const parsed = enteredProvided
    ? Number(entered)
    : requestedNum != null
      ? requestedNum
      : null;

  if (parsed == null || parsed <= 0) {
    return { valid: false, errorKey: "biorepository.retrieval.workbench.attach.invalidQuantity" };
  }

  const effectiveAvailable =
    bioSample != null
      ? resolveAttachAvailableQuantity(bioSample, requestedNum)
      : available != null && !Number.isNaN(Number(available))
        ? Number(available)
        : null;

  const usedImplicitLineDefault =
    enteredProvided && requestedNum != null && parsed === requestedNum;

  if (effectiveAvailable != null && parsed > effectiveAvailable) {
    if (!enteredProvided || usedImplicitLineDefault) {
      return { valid: true, quantity: effectiveAvailable };
    }
    return { valid: false, errorKey: "biorepository.retrieval.workbench.attach.exceedsAvailable" };
  }

  if (requestedNum != null && parsed > requestedNum) {
    return { valid: false, errorKey: "biorepository.retrieval.workbench.attach.exceedsRequested" };
  }

  return { valid: true, quantity: parsed };
};

export const getTopCandidate = (suggestion) => suggestion?.topSuggestion ?? null;

export const hasTypeMismatch = (suggestion) =>
  suggestion?.summary?.sampleTypeMatchesRequested === false ||
  suggestion?.summary?.mismatchReason === "TYPE_MISMATCH" ||
  suggestion?.topSuggestion?.sampleTypeMatchesRequested === false ||
  suggestion?.topSuggestion?.mismatchReason === "TYPE_MISMATCH";

export const hasUsableTopSuggestion = (suggestion) => {
  const top = getTopCandidate(suggestion);
  if (!top) {
    return false;
  }
  return Boolean(
    top.id != null ||
      top.bioSampleId != null ||
      top.accessionNumber ||
      top.barcode ||
      top.externalId,
  );
};

export const formatSamplePath = (sample) => {
  if (!sample) {
    return null;
  }
  if (sample.samplePath) {
    return sample.samplePath;
  }
  if (sample.hierarchicalPath) {
    return sample.hierarchicalPath;
  }
  return null;
};

export const getSuggestionSummary = (suggestion) => {
  if (!suggestion) {
    return null;
  }
  const summary = suggestion.summary || {};
  const top = getTopCandidate(suggestion);
  return {
    sampleIdentity:
      summary.sampleIdentity || formatTopCandidateIdentity(top) || null,
    availableQuantity:
      summary.availableQuantity ??
      top?.remainingQuantity ??
      top?.quantity ??
      null,
    availableUnitOfMeasure:
      summary.availableUnitOfMeasure || top?.unitOfMeasure || null,
    samplePath:
      summary.samplePath || formatSamplePath(top) || null,
    matchReason:
      summary.matchReason || top?.matchReason || null,
    matchScore:
      summary.matchScore ?? top?.matchScore ?? null,
    sampleTypeMatchesRequested:
      summary.sampleTypeMatchesRequested ??
      top?.sampleTypeMatchesRequested ??
      null,
    mismatchReason:
      summary.mismatchReason || top?.mismatchReason || null,
  };
};
