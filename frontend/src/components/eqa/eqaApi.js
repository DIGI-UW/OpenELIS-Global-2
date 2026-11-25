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

// Names come from the whole catalog: the lab-unit list is empty for a QA officer.
// A removed scheme assignment keeps its row with isActive false.
export const fetchTests = (schemeId, callback) => {
  getFromOpenElisServer(`/rest/eqa/programs/${schemeId}/tests`, (rows) => {
    const assigned = new Set(
      asList(rows)
        .filter((row) => row.isActive !== false)
        .map((row) => String(row.testId)),
    );
    getFromOpenElisServer("/rest/eqa/testable-tests", (testable) => {
      const usable = new Set(
        asList(testable)
          .map(String)
          .filter((id) => assigned.has(id)),
      );
      getFromOpenElisServer("/rest/displayList/ALL_TESTS", (tests) =>
        callback(asList(tests).filter((test) => usable.has(String(test.id)))),
      );
    });
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
