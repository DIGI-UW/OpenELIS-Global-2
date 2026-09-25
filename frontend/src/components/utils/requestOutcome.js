/**
 * OGC-1234: postToOpenElisServerJsonResponse never hands its callback a falsy
 * value: a refusal arrives as an object carrying the HTTP status (0 for a
 * network failure), a success as the parsed body, which has no numeric status.
 * So `if (res)` reads every refusal as success; this is the check to use.
 */
export const requestFailed = (res) =>
  !res ||
  (typeof res === "object" &&
    typeof res.status === "number" &&
    (res.status === 0 || res.status >= 400));
