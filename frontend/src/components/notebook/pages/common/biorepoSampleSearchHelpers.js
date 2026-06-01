export const EMPTY_SAMPLE_SEARCH_FILTERS = {
  accessionNumber: "",
  barcode: "",
  sampleType: "",
  originLab: "",
  projectId: "",
  collectionDateFrom: "",
  collectionDateTo: "",
};

const FILTER_PARAM_MAP = {
  identity: "identity",
  accessionNumber: "accessionNumber",
  barcode: "barcode",
  sampleType: "sampleType",
  originLab: "originLab",
  projectId: "projectId",
  collectionDateFrom: "collectionDateFrom",
  collectionDateTo: "collectionDateTo",
};

export const hasActiveSearchFilters = (filters = EMPTY_SAMPLE_SEARCH_FILTERS) =>
  Object.keys(FILTER_PARAM_MAP).some((key) => String(filters[key] || "").trim());

export const buildIdentityAwareFulfillmentFilters = (
  filters = EMPTY_SAMPLE_SEARCH_FILTERS,
) => {
  const accessionNumber = String(filters.accessionNumber || "").trim();
  const barcode = String(filters.barcode || "").trim();

  const hasAccession = Boolean(accessionNumber);
  const hasBarcode = Boolean(barcode);
  const sameIdentity =
    hasAccession &&
    hasBarcode &&
    accessionNumber.toLowerCase() === barcode.toLowerCase();

  if (!hasAccession && !hasBarcode) {
    return { ...filters, identity: "" };
  }

  if (sameIdentity || (hasAccession && !hasBarcode) || (!hasAccession && hasBarcode)) {
    return {
      ...filters,
      identity: accessionNumber || barcode,
      accessionNumber: hasAccession && hasBarcode && !sameIdentity ? accessionNumber : "",
      barcode: hasAccession && hasBarcode && !sameIdentity ? barcode : "",
    };
  }

  return {
    ...filters,
    identity: "",
    accessionNumber,
    barcode,
  };
};

export const buildSampleSearchQuery = (
  filters = EMPTY_SAMPLE_SEARCH_FILTERS,
  { browse = false, status = "STORED", limit = 50, omitSampleTypeWhenIdentity = false } = {},
) => {
  const params = new URLSearchParams();
  params.set("status", status);
  params.set("limit", String(limit));

  if (browse) {
    params.set("browse", "true");
  }

  const hasIdentity = Boolean(String(filters.identity || "").trim());

  Object.entries(FILTER_PARAM_MAP).forEach(([filterKey, paramKey]) => {
    if (omitSampleTypeWhenIdentity && hasIdentity && filterKey === "sampleType") {
      return;
    }
    const value = String(filters[filterKey] || "").trim();
    if (value) {
      params.set(paramKey, value);
    }
  });

  return params.toString();
};

export const buildFulfillmentSearchQuery = (
  filters = EMPTY_SAMPLE_SEARCH_FILTERS,
  options = {},
) =>
  buildSampleSearchQuery(buildIdentityAwareFulfillmentFilters(filters), {
    ...options,
    omitSampleTypeWhenIdentity: true,
  });

export const sortSearchResults = (results, filters = EMPTY_SAMPLE_SEARCH_FILTERS) => {
  const barcodeTerm = String(filters.barcode || "").trim().toLowerCase();
  const accessionTerm = String(filters.accessionNumber || "").trim().toLowerCase();

  const score = (sample) => {
    let value = 0;
    const barcode = String(sample.barcode || sample.sampleNumber || "").toLowerCase();
    const accession = String(sample.accessionNumber || "").toLowerCase();

    if (barcodeTerm) {
      if (barcode === barcodeTerm) {
        value += 2;
      } else if (barcode.includes(barcodeTerm)) {
        value += 1;
      }
    }
    if (accessionTerm) {
      if (accession === accessionTerm) {
        value += 2;
      } else if (accession.includes(accessionTerm)) {
        value += 1;
      }
    }
    return value;
  };

  return [...results].sort((a, b) => {
    const scoreDiff = score(b) - score(a);
    if (scoreDiff !== 0) {
      return scoreDiff;
    }
    const dateA = a.collectionDate ? new Date(a.collectionDate).getTime() : 0;
    const dateB = b.collectionDate ? new Date(b.collectionDate).getTime() : 0;
    return dateB - dateA;
  });
};

export const formatCollectionDate = (value) => {
  if (!value) {
    return "-";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return String(value);
  }
  return date.toLocaleDateString();
};

export { FILTER_PARAM_MAP };
