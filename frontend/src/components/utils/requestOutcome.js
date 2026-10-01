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

/**
 * The server's own wording for a refusal, to show the user. A request that got
 * no answer (status 0) carries the browser's "Failed to fetch" instead, which
 * is not a message for a lab user, so it reads as empty and the caller falls
 * back to its own wording (OGC-1408).
 */
export const serverMessage = (res) =>
  res && typeof res === "object" && res.status !== 0
    ? res.message || res.error || ""
    : "";
