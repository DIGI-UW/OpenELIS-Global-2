import { beforeEach, describe, expect, it, vi } from "vitest";

const { getFromOpenElisServer } = vi.hoisted(() => ({
  getFromOpenElisServer: vi.fn(),
}));

vi.mock("../components/utils/Utils", () => ({
  getAcceptLanguageHeader: vi.fn(),
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse: vi.fn(),
  Roles: { RESULTS: "Results" },
}));

import { getAnalyzers } from "./analyzerService";

describe("analyzerService.getAnalyzers", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockClear();
  });

  it("serializes every URL-backed list filter", () => {
    const callback = vi.fn();
    const signal = new AbortController().signal;

    getAnalyzers(
      {
        search: "xpert",
        status: "SETUP",
        testUnit: "7",
        analyzerType: "MOLECULAR",
      },
      callback,
      signal,
    );

    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/analyzer/analyzers?status=SETUP&search=xpert&testUnit=7&analyzerType=MOLECULAR",
      callback,
      signal,
    );
  });
});
