import { beforeEach, describe, expect, it, vi } from "vitest";

const utils = vi.hoisted(() => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
  putToOpenElisServerFullResponse: vi.fn(),
}));

vi.mock("../../utils/Utils", () => utils);

import {
  logCriticalCommunication,
  releaseFinalReport,
  releasePreliminaryReport,
} from "../MicrobiologyService";

describe("MicrobiologyService", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("rejects failed critical communication responses", async () => {
    utils.postToOpenElisServerJsonResponse.mockImplementation(
      (_url, _payload, callback) =>
        callback({
          error: "Communication was not saved",
          statusCode: 500,
        }),
    );

    await expect(
      logCriticalCommunication("case-1", { recipient: "Provider on call" }),
    ).rejects.toThrow("Communication was not saved");
  });

  it.each([
    ["preliminary", releasePreliminaryReport],
    ["final", releaseFinalReport],
  ])("rejects a failed %s release response", async (_releaseType, release) => {
    utils.postToOpenElisServerJsonResponse.mockImplementation(
      (_url, _payload, callback) =>
        callback({
          error: "Report release was blocked",
          statusCode: 409,
        }),
    );

    await expect(release("case-1")).rejects.toThrow(
      "Report release was blocked",
    );
  });
});
