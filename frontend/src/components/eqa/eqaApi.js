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

// A QA officer's lab-unit test list is empty, so filter against the whole catalog.
// A removed scheme assignment keeps its row with isActive false.
// Answers null when any read fails.
export const fetchTests = (schemeId, callback) => {
  const urls = [
    `/rest/eqa/programs/${schemeId}/tests`,
    "/rest/eqa/testable-tests",
    "/rest/displayList/ALL_TESTS",
  ];
  const replies = [];
  let pending = urls.length;
  urls.forEach((url, index) =>
    getFromOpenElisServer(url, (data) => {
      replies[index] = data;
      if (--pending > 0) {
        return;
      }
      const [rows, testable, tests] = replies;
      if (!replies.every(Array.isArray)) {
        callback(null);
        return;
      }
      const labels = new Map(
        rows
          .filter((row) => row.isActive !== false)
          .map((row) => [String(row.testId), row.label]),
      );
      const usable = new Set(
        testable.map(String).filter((id) => labels.has(id)),
      );
      callback(
        tests
          .filter((test) => usable.has(String(test.id)))
          .map((test) => ({
            ...test,
            name: labels.get(String(test.id)) || test.name || test.value,
          })),
      );
    }),
  );
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
