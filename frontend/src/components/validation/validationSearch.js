import { labNumberForSearch } from "../utils/Utils";

/**
 * OGC-1418 — the Validation page's one search: a lab number or a lab number
 * range typed into the search box, a patient, a Lab Unit and a date range,
 * all combined. The page keeps the search in its address so a refresh or a
 * bookmark reopens the same queue.
 */
export const SEARCH_PARAMS = [
  "labNumber",
  "testSectionId",
  "fromDate",
  "toDate",
  "patientId",
];

export const EMPTY_SEARCH = {
  labNumber: "",
  testSectionId: "",
  fromDate: "",
  toDate: "",
  patientId: "",
};

const RANGE_SEPARATOR = /\s*(?:\.\.|\s-\s|\sto\s)\s*/i;

/**
 * The lab numbers the search box names. One lab number is a range of one;
 * "A..B", "A - B" and "A to B" are a range; "A.." is every lab number from A
 * on (the old "By Range of Order Numbers" search).
 */
export function parseLabNumberSearch(text) {
  const raw = String(text || "").trim();
  if (!raw) {
    return { labNumberFrom: "", labNumberTo: "" };
  }
  const openEnded = /\.\.\s*$/.test(raw);
  const parts = raw
    .replace(/\.\.\s*$/, "")
    .split(RANGE_SEPARATOR)
    .map((part) => labNumberForSearch(part))
    .filter(Boolean);
  if (openEnded) {
    return { labNumberFrom: parts[0] || "", labNumberTo: "" };
  }
  if (parts.length >= 2) {
    return { labNumberFrom: parts[0], labNumberTo: parts[1] };
  }
  return { labNumberFrom: parts[0] || "", labNumberTo: parts[0] || "" };
}

/** Whether the search names exactly one lab number. */
export function isSingleLabNumber(search) {
  const { labNumberFrom, labNumberTo } = parseLabNumberSearch(
    search?.labNumber,
  );
  return Boolean(labNumberFrom) && labNumberFrom === labNumberTo;
}

export function isEmptySearch(search) {
  return SEARCH_PARAMS.every((key) => !String(search?.[key] || "").trim());
}

/**
 * The search an address asks for. The old menu entries' addresses keep
 * working: ?type=routine|order|range|testDate and the four legacy paths open
 * the one page with the equivalent filter.
 */
export function searchFromLocation(pathname, query) {
  const params = new URLSearchParams((query || "").replace(/^\?/, ""));
  const search = { ...EMPTY_SEARCH };
  SEARCH_PARAMS.forEach((key) => {
    search[key] = params.get(key) || "";
  });
  const path = (pathname || "").replace(/\/$/, "");
  const type =
    params.get("type") ||
    {
      "/ResultValidation": "routine",
      "/AccessionValidation": "order",
      "/AccessionValidationRange": "range",
      "/ResultValidationByTestDate": "testDate",
    }[path] ||
    "";
  const accession = params.get("accessionNumber") || "";
  if (type === "routine" && !search.testSectionId) {
    search.testSectionId = params.get("testSectionId") || "";
  }
  if (type === "order" && accession && !search.labNumber) {
    search.labNumber = accession;
  }
  if (type === "range" && accession && !search.labNumber) {
    search.labNumber = `${accession}..`;
  }
  if (type === "testDate" && params.get("date") && !search.fromDate) {
    search.fromDate = params.get("date");
    search.toDate = params.get("date");
  }
  return search;
}

/** The page's own address for a search. */
export function searchQueryString(search) {
  const params = new URLSearchParams();
  SEARCH_PARAMS.forEach((key) => {
    const value = String(search?.[key] || "").trim();
    if (value) {
      params.set(key, value);
    }
  });
  const query = params.toString();
  return query ? `?${query}` : "";
}

/** The queue request for a search. */
export function validationEndpoint(search) {
  const { labNumberFrom, labNumberTo } = parseLabNumberSearch(
    search?.labNumber,
  );
  const params = new URLSearchParams({
    labNumberFrom,
    labNumberTo,
    testSectionId: search?.testSectionId || "",
    fromDate: search?.fromDate || "",
    toDate: search?.toDate || "",
    patientId: search?.patientId || "",
  });
  return `/rest/AccessionValidation?${params.toString()}`;
}

/**
 * The scope of a bulk release: the search the page shows, so the server
 * reloads exactly those rows. `params` is the page's query string.
 */
export function releaseScope(params) {
  const search = searchFromLocation("/validation", params);
  const { labNumberFrom, labNumberTo } = parseLabNumberSearch(search.labNumber);
  return {
    labNumberFrom,
    labNumberTo,
    testSectionId: search.testSectionId,
    fromDate: search.fromDate,
    toDate: search.toDate,
    patientId: search.patientId,
  };
}
