import {
  capasCompletedThisWeek,
  nceActivityRows,
  ncesResolvedThisWeek,
  newNcesThisWeek,
  severityBreakdown,
  weekStart,
} from "../overviewData";

// Fixed boundary: the week began Monday 2026-07-06 (server-provided in prod;
// these tests pass it explicitly the way ThisWeek.jsx does).
const WEEK_START_DATE = "2026-07-06";
const WEEK_START_MS = Date.parse("2026-07-06T00:00:00Z");

describe("weekStart (local fallback)", () => {
  test("midweek resolves to the preceding Monday", () => {
    const ws = weekStart(new Date("2026-07-08T12:00:00"));
    expect(ws.getDay()).toBe(1);
    expect(ws.getDate()).toBe(6);
    expect(ws.getHours()).toBe(0);
  });

  test("Monday is its own week start; Sunday belongs to the prior Monday", () => {
    expect(weekStart(new Date("2026-07-06T00:30:00")).getDate()).toBe(6);
    expect(weekStart(new Date("2026-07-12T23:00:00")).getDate()).toBe(6);
  });
});

describe("NCE weekly counters", () => {
  test("newNcesThisWeek keeps only reportDates on/after the boundary date", () => {
    const list = [
      { reportDate: "2026-07-06" },
      { reportDate: "2026-07-08" },
      { reportDate: "2026-07-05" }, // Sunday — prior week
      { reportDate: null },
    ];
    expect(newNcesThisWeek(list, WEEK_START_DATE)).toHaveLength(2);
  });

  test("severityBreakdown buckets CRITICAL/MAJOR/MINOR and treats LOW as minor", () => {
    const counts = severityBreakdown([
      { severity: "CRITICAL" },
      { severity: "MAJOR" },
      { severity: "MINOR" },
      { severity: "LOW" },
      { severity: null },
    ]);
    expect(counts).toEqual({ critical: 1, major: 1, minor: 2 });
  });

  test("ncesResolvedThisWeek accepts RESOLVED, structured newValue, and closed descriptions, in-week only", () => {
    const list = [
      {
        history: [{ activity: "RESOLVED", timestamp: "2026-07-07T09:00:00Z" }],
      },
      {
        // structured transition target, prose says nothing useful
        history: [
          {
            activity: "STATUS_CHANGED",
            description: "Status updated",
            newValue: "Closed",
            timestamp: "2026-07-08T08:00:00Z",
          },
        ],
      },
      {
        // legacy row: no newValue, description carries the transition
        history: [
          {
            activity: "STATUS_CHANGED",
            description: "Status changed to Closed",
            timestamp: "2026-07-08T08:00:00Z",
          },
        ],
      },
      {
        // CAPA transition is not a resolution
        history: [
          {
            activity: "STATUS_CHANGED",
            description: "Status changed to CAPA for follow-up",
            newValue: "CAPA",
            timestamp: "2026-07-08T08:00:00Z",
          },
        ],
      },
      {
        // resolved before this week
        history: [{ activity: "RESOLVED", timestamp: "2026-07-03T09:00:00Z" }],
      },
      { history: [] },
    ];
    expect(ncesResolvedThisWeek(list, WEEK_START_MS)).toBe(3);
  });

  test("capasCompletedThisWeek requires a resolution this week plus a CAPA trail", () => {
    const resolvedWithCapa = {
      history: [
        { activity: "CORRECTIVE_ACTION", timestamp: "2026-06-20T09:00:00Z" },
        { activity: "RESOLVED", timestamp: "2026-07-07T09:00:00Z" },
      ],
    };
    const resolvedNoCapa = {
      history: [{ activity: "RESOLVED", timestamp: "2026-07-07T10:00:00Z" }],
    };
    const capaStillOpen = {
      history: [
        { activity: "CORRECTIVE_ACTION", timestamp: "2026-07-07T09:00:00Z" },
      ],
    };
    expect(
      capasCompletedThisWeek(
        [resolvedWithCapa, resolvedNoCapa, capaStillOpen],
        WEEK_START_MS,
      ),
    ).toBe(1);
  });
});

describe("nceActivityRows", () => {
  test("flattens histories newer than the cutoff and carries actor + NCE number", () => {
    const since = Date.parse("2026-07-07T00:00:00Z");
    const rows = nceActivityRows(
      [
        {
          nceNumber: "NCE-0004",
          history: [
            {
              activity: "RESOLVED",
              userName: "Sara Chen",
              timestamp: "2026-07-07T09:00:00Z",
            },
            { activity: "CREATED", timestamp: "2026-06-30T09:00:00Z" },
          ],
        },
        { nceNumber: "NCE-0005" },
      ],
      since,
    );
    expect(rows).toEqual([
      {
        type: "NCE",
        activity: "RESOLVED",
        actor: "Sara Chen",
        nceNumber: "NCE-0004",
        timestamp: "2026-07-07T09:00:00Z",
      },
    ]);
  });
});
