// Calls more than one EQA page makes. Anything only one page needs stays in that
// page's own api module (inHouseApi, workbenchApi).
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";

/**
 * A failed read answers a truthy {error: ...} object, not an array, so `data || []`
 * lets a refusal reach .map and white-screen the page. Every list read goes
 * through this instead.
 */
export const asList = (data) => (Array.isArray(data) ? data : []);

/**
 * These endpoints answer their refusals as {error: "..."} JSON with a 4xx, so a
 * truthy body is not success on its own — checking the status here is what keeps
 * a "saved!" toast over an empty table from coming back.
 */
export const failed = (response) =>
  !response || response.error || (response.status && response.status >= 400);

// The standard test list, narrowed to the tests that carry an analyte: a panel
// target is stored against an analyte, so offering the rest is a dead end the
// wizard would only discover at write time.
export const fetchTests = (callback) => {
  getFromOpenElisServer("/rest/eqa/testable-tests", (testable) => {
    const usable = new Set(asList(testable).map(String));
    getFromOpenElisServer("/rest/test-list", (tests) =>
      callback(asList(tests).filter((test) => usable.has(String(test.id)))),
    );
  });
};

// Omit cycleNumber and the service takes the scheme's next one.
export const createCycle = (payload, callback) => {
  postToOpenElisServerJsonResponse(
    "/rest/eqa/cycles",
    JSON.stringify(payload),
    callback,
  );
};

// Panel and its samples in one write; blind codes are generated server-side.
export const createPanel = (payload, callback) => {
  postToOpenElisServerJsonResponse(
    "/rest/eqa/panels",
    JSON.stringify(payload),
    callback,
  );
};
