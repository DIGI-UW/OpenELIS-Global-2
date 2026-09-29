import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import { getFromOpenElisServer, toLocalIsoDate } from "./Utils";
import {
  labNow,
  labTimeToInstant,
  loadLabClock,
  resetLabClock,
} from "./labClock";

vi.mock("./Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  getFromOpenElisServer: vi.fn(),
}));

// 20:00 UTC is already the next day in Kiritimati (UTC+14) and still the
// morning in Pago Pago (UTC-11), whatever zone the test runner is in.
const INSTANT = Date.parse("2031-03-05T20:00:00Z");

const serverIn = (timezone) =>
  getFromOpenElisServer.mockImplementation((url, callback) =>
    callback({ timezone }),
  );

describe("labClock", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(INSTANT);
  });
  afterEach(() => {
    vi.useRealTimers();
    resetLabClock();
    getFromOpenElisServer.mockReset();
  });

  test("today is the lab's calendar day, not the browser's", async () => {
    serverIn("Pacific/Kiritimati");
    await loadLabClock();
    expect(toLocalIsoDate(labNow())).toBe("2031-03-06");

    serverIn("Pacific/Pago_Pago");
    await loadLabClock();
    expect(toLocalIsoDate(labNow())).toBe("2031-03-05");
  });

  test("now reads the lab's wall clock in its local fields", async () => {
    serverIn("Pacific/Kiritimati");
    await loadLabClock();
    const now = labNow();
    expect([now.getFullYear(), now.getMonth(), now.getDate()]).toEqual([
      2031, 2, 6,
    ]);
    expect([now.getHours(), now.getMinutes()]).toEqual([10, 0]);
  });

  test("keeps the browser's clock when the server names no usable zone", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) =>
      callback({ timezone: "Not/AZone" }),
    );
    await loadLabClock();
    expect(labNow().getTime()).toBe(INSTANT);
  });

  test("a lab wall-clock time converts back to the instant it names", async () => {
    serverIn("Pacific/Kiritimati");
    await loadLabClock();
    expect(labTimeToInstant(new Date(2031, 2, 6, 0, 0)).toISOString()).toBe(
      "2031-03-05T10:00:00.000Z",
    );
  });

  test("the conversion uses the offset of the target day across a DST change", async () => {
    // Now is 20 March (New York on EDT, UTC-4); 1 March is still EST, UTC-5.
    vi.setSystemTime(Date.parse("2031-03-20T12:00:00Z"));
    serverIn("America/New_York");
    await loadLabClock();
    expect(labTimeToInstant(new Date(2031, 2, 1, 0, 0)).toISOString()).toBe(
      "2031-03-01T05:00:00.000Z",
    );
  });

  test("a browser clock that is wrong is corrected by the server's own time", async () => {
    // The browser believes it is 20:00 UTC; the server's clock reads 16:30.
    getFromOpenElisServer.mockImplementation((url, callback) =>
      callback({ date: "2031-03-05", time: "16:30", timezone: "UTC" }),
    );
    await loadLabClock();
    const now = labNow();
    expect([now.getDate(), now.getHours(), now.getMinutes()]).toEqual([
      5, 16, 30,
    ]);
  });
});
