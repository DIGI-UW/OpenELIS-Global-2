// Data seam for the Follow-Up Queue (OGC-611, FR-V2.3-02). Every call is live:
// the queue, escalate and dismiss endpoints all shipped with the tiered NCE
// adapter, so nothing here is mocked.
//
// A register row is one cycle per participating organization and carries a JSON
// snapshot of every result that put the lab there — so one queue row can cover
// several analytes. The view model keeps that grain (triage acts on the row)
// and derives the single-analyte columns the table shows when it holds one.
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";

const SOURCE_KEY = {
  IN_HOUSE: "in_house",
  INTER_LAB_SPLIT: "inter_lab_split",
};

/** Worst-first: an unacceptable result decides the row's reason and sort. */
const SEVERITY = { UNACCEPTABLE: 2, QUESTIONABLE: 1 };

const absZ = (value) =>
  value === null || value === undefined || value === "" || isNaN(Number(value))
    ? null
    : Math.abs(Number(value));

const worstResult = (results) =>
  results.reduce((worst, result) => {
    if (!worst) return result;
    const bySeverity =
      (SEVERITY[result.performanceStatus] || 0) -
      (SEVERITY[worst.performanceStatus] || 0);
    if (bySeverity !== 0) return bySeverity > 0 ? result : worst;
    return (absZ(result.zScore) ?? -1) > (absZ(worst.zScore) ?? -1)
      ? result
      : worst;
  }, null);

const toViewModel = (dto) => {
  const results = dto.results || [];
  const worst = worstResult(results);
  return {
    ...dto,
    results,
    sourceKey: SOURCE_KEY[dto.schemeType] || "external",
    // FR-V2.3-01 puts exactly two kinds of item in this queue, and the scheme
    // decides which: an in-house failure, or an external score in the
    // questionable band. An external result bad enough to be a plain
    // unacceptable took the auto-NCE path and never arrived here — so reading
    // the tag off the verdict would label the band wrongly. Per-result verdicts
    // are in the expanded detail.
    reason: dto.schemeType === "IN_HOUSE" ? "inHouseFailure" : "questionable",
    analyteLabel: results.map((r) => r.analyteName).filter(Boolean),
    worstZScore: worst ? worst.zScore : null,
  };
};

export const fetchFollowUpQueue = (callback) =>
  getFromOpenElisServer("/rest/eqa/followups", (data) =>
    callback((data || []).map(toViewModel)),
  );

/**
 * Both triage writes answer with {ok, body}: on refusal the body is the
 * server's own {error: "..."}, which is the whole point of the FullResponse
 * helper — a JSON-only helper reports a 409 as success.
 */
const withBody = (callback) => (response) => {
  if (!response) {
    callback({ ok: false, body: null });
    return;
  }
  response
    .json()
    .then((body) => callback({ ok: response.ok, body }))
    .catch(() => callback({ ok: response.ok, body: null }));
};

export const escalateFollowUp = (followupId, callback) =>
  postToOpenElisServerFullResponse(
    `/rest/eqa/followups/${followupId}/escalate`,
    "{}",
    withBody(callback),
  );

export const dismissFollowUp = (followupId, category, notes, callback) =>
  postToOpenElisServerFullResponse(
    `/rest/eqa/followups/${followupId}/dismiss`,
    JSON.stringify({ category, notes }),
    withBody(callback),
  );
