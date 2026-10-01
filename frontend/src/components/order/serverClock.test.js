import { describe, expect, it, vi, beforeEach } from "vitest";

const { getFromOpenElisServer } = vi.hoisted(() => ({
  getFromOpenElisServer: vi.fn(),
}));
vi.mock("../utils/Utils", () => ({ getFromOpenElisServer }));

import { fetchServerNow } from "./serverClock";
import { currentLocalTime, todayLocalIso } from "./dateUtils";

describe("fetchServerNow", () => {
  beforeEach(() => getFromOpenElisServer.mockReset());

  // OGC-1266: environmental and vector samples were stamped from the browser's
  // clock and stored three hours ahead of the server's.
  it("stamps from the server's clock", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) =>
      callback?.({ date: "2026-09-27", time: "08:56" }),
    );

    await expect(fetchServerNow()).resolves.toEqual({
      date: "2026-09-27",
      time: "08:56",
    });
    expect(getFromOpenElisServer.mock.calls[0][0]).toBe("/rest/server-time");
  });

  it("falls back to the browser's clock when the server cannot answer", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) =>
      callback?.(undefined),
    );

    const now = await fetchServerNow();

    expect(now.date).toBe(todayLocalIso());
    expect(now.time).toMatch(/^\d{2}:\d{2}$/);
    expect([currentLocalTime(), now.time]).toContain(now.time);
  });
});
