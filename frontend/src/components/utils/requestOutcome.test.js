import { requestFailed, serverMessage } from "./requestOutcome";

/**
 * OGC-1234 / OGC-1408 — the fetch helpers never hand their callback a falsy
 * value: a refusal is an object carrying the HTTP status, a request that got
 * no answer carries status 0, and a success is the parsed body.
 */
describe("requestFailed", () => {
  it("reads a request that got no answer (status 0) as a failure", () => {
    expect(
      requestFailed({
        error: "Failed to fetch",
        message: "Failed to fetch",
        status: 0,
      }),
    ).toBe(true);
  });

  it("reads a refusal as a failure", () => {
    expect(requestFailed({ status: 400, error: "abc is not a number." })).toBe(
      true,
    );
    expect(requestFailed({ status: 500 })).toBe(true);
  });

  it("reads a missing response as a failure", () => {
    expect(requestFailed(undefined)).toBe(true);
    expect(requestFailed(null)).toBe(true);
  });

  it("reads a parsed success body as a success, whatever its fields", () => {
    expect(requestFailed({ resultId: "71", analysisStatusId: "15" })).toBe(
      false,
    );
    expect(requestFailed({ status: "ACTIVE" })).toBe(false);
    expect(requestFailed([])).toBe(false);
  });
});

describe("serverMessage", () => {
  it("hands back the server's own wording for a refusal", () => {
    expect(serverMessage({ status: 400, error: "abc is not a number." })).toBe(
      "abc is not a number.",
    );
    expect(
      serverMessage({ status: 409, message: "Insufficient stock", error: "x" }),
    ).toBe("Insufficient stock");
  });

  it("is empty for a request that got no answer, so the caller uses its own wording", () => {
    expect(
      serverMessage({
        error: "Failed to fetch",
        message: "Failed to fetch",
        status: 0,
      }),
    ).toBe("");
    expect(serverMessage(undefined)).toBe("");
  });
});
