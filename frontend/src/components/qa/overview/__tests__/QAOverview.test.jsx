import React from "react";
import { act, fireEvent, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import QAOverview from "../QAOverview";
import { weekStart } from "../overviewData";
import { renderQa } from "../../testUtils";
import { isoDaysFromToday } from "../../common/qaDates";
import { fetchFromOpenElisServer, toLocalIsoDate } from "../../../utils/Utils";

vi.mock("../../../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    fetchFromOpenElisServer: vi.fn(),
  };
});

// Dates relative to the real clock so the week/24h predicates stay stable
// whichever day the suite runs.
const isoDay = toLocalIsoDate;
const TODAY = isoDay(new Date());
const LAST_MONTH = isoDay(new Date(Date.now() - 10 * 864e5)); // before any Monday
const NOW_ISO = new Date().toISOString();
const HOUR_AGO_ISO = new Date(Date.now() - 3600e3).toISOString();

// 3 critical+Pending (pulse), 1 resolved-with-CAPA this week, plus noise
const NCE_LIST = [
  { id: "1", severity: "CRITICAL", status: "Pending", reportDate: TODAY },
  { id: "2", severity: "CRITICAL", status: "Pending", reportDate: TODAY },
  { id: "3", severity: "CRITICAL", status: "Pending", reportDate: TODAY },
  {
    id: "4",
    nceNumber: "NCE-0004",
    severity: "CRITICAL",
    status: "Closed",
    reportDate: LAST_MONTH,
    history: [
      {
        activity: "CORRECTIVE_ACTION",
        description: "Corrective action updated",
        timestamp: new Date(Date.now() - 9 * 864e5).toISOString(),
        userName: "Sara Chen",
      },
      {
        activity: "RESOLVED",
        description: "NCE resolved and marked as Completed",
        timestamp: NOW_ISO,
        userName: "Sara Chen",
      },
    ],
  },
  { id: "5", severity: "MAJOR", status: "Pending", reportDate: TODAY },
  // In the CAPA stage with a recorded verdict => counts toward "in corrective
  // action" (all CAPA-stage) but not "effectiveness reviews due" (verdict given)
  {
    id: "6",
    severity: "MINOR",
    status: "CAPA",
    effective: "No",
    reportDate: LAST_MONTH,
  },
  // CAPA recorded, verdict pending => effectiveness review due
  { id: "7", severity: "MAJOR", status: "CAPA", reportDate: LAST_MONTH },
  // CAPA already reviewed ineffective => not due
  {
    id: "8",
    severity: "MINOR",
    status: "CAPA",
    effective: "No",
    reportDate: LAST_MONTH,
  },
];

const SUMMARY = {
  qc: {
    compliantInstruments: 4,
    warningInstruments: 1,
    nonCompliantInstruments: 1,
    totalInstruments: 6,
    violations24h: 2,
    violationsThisWeek: 2,
    weekRuleBreakdown: { "1-3s": 1, "1-2s": 1 },
  },
  eqa: { open: 5, overdue: 1, dueSoon14d: 3 },
  week: {
    weekStart: isoDay(weekStart(new Date())),
    weekStartInstant: weekStart(new Date()).toISOString(),
    auditEntries: 8732,
    signatureEvents: 1247,
  },
  activity: [
    {
      type: "ESIG",
      timestamp: HOUR_AGO_ISO,
      actor: "L. Tran",
      meaning: "VALIDATED_AND_RELEASED",
      recordType: "RESULT",
      recordId: 44196,
    },
    {
      type: "QC_VIOLATION",
      timestamp: new Date(Date.now() - 2 * 3600e3).toISOString(),
      ruleCode: "1-2s",
      severity: "WARNING",
      instrumentName: "Architect ci8200",
    },
  ],
};

const TAT_CURRENT = { mean: 33.3333, totalCount: 12 };
const TAT_PRIOR = { mean: 40, totalCount: 9 };

const renderPage = async () => {
  let view;
  fetchFromOpenElisServer.mockClear();
  await act(async () => {
    view = renderQa(<QAOverview />, { entries: ["/qa/overview"] });
  });
  // This week's confirmed-callback count is the last thing on the page to
  // arrive: its window is only asked for once the overview summary has landed.
  // Seeing it means every read has settled.
  await waitFor(() =>
    expect(screen.getByText("3 confirmed")).toBeInTheDocument(),
  );
  return view;
};

const callsTo = (fragment) =>
  fetchFromOpenElisServer.mock.calls.filter(([url]) => url.includes(fragment));

beforeEach(() => {
  sessionStorage.clear();
  vi.clearAllMocks();
  // The TAT rollup asks for the 30-day window and the equal one before it.
  const currentWindow = `fromDate=${isoDaysFromToday(-30)}`;
  fetchFromOpenElisServer.mockImplementation((url) => {
    if (url.includes("/rest/nce/dashboard")) {
      return Promise.resolve({ nceList: NCE_LIST });
    }
    if (url.includes("/rest/qa/overview/summary")) {
      return Promise.resolve(SUMMARY);
    }
    if (url.includes("/rest/reports/tat/summary")) {
      return Promise.resolve(
        url.includes(currentWindow) ? TAT_CURRENT : TAT_PRIOR,
      );
    }
    if (url.includes("/rest/reports/amendment/summary")) {
      return Promise.resolve({
        amendedCount: 8,
        releasedCount: 2580,
        ratePercent: 0.31,
      });
    }
    if (url.includes("/rest/reports/rejection/summary")) {
      return Promise.resolve({
        rejectedCount: 3,
        totalCount: 120,
        ratePercent: 2.5,
      });
    }
    if (url.includes("/rest/critical-callback/summary")) {
      return Promise.resolve({
        enabled: true,
        criticalCount: 4,
        confirmedCount: 3,
        compliancePercent: 75.0,
        target: 100,
      });
    }
    if (url.includes("/rest/accreditation/summary")) {
      return Promise.resolve({
        totalBodies: 3,
        activeBodies: 2,
        expiringBodies: 1,
        expiredBodies: 0,
        inForceBodyNames: ["ISO 15189", "SANAS"],
        worstStatus: "EXPIRING",
      });
    }
    if (url.includes("/rest/nce/capa-register")) {
      return Promise.resolve([
        { id: 1, nceStatus: "Pending", dueDate: LAST_MONTH }, // overdue
        { id: 2, nceStatus: "completed", dateCompleted: TODAY },
      ]);
    }
    if (url.includes("/rest/qi-config/resolve")) {
      // full thresholds where the tiles judge tones (OGC-710)
      if (url.includes("indicator=REJECTION")) {
        return Promise.resolve({
          enabled: true,
          target: 2,
          action: 5,
          direction: "LOWER_BETTER",
        });
      }
      if (url.includes("indicator=AMENDMENT")) {
        return Promise.resolve({
          enabled: true,
          target: 0.5,
          action: 2,
          direction: "LOWER_BETTER",
        });
      }
      if (url.includes("indicator=CALLBACK")) {
        // the one HIGHER_BETTER indicator (qa/009 seed: 100 target / 95 action)
        return Promise.resolve({
          enabled: true,
          target: 100,
          action: 95,
          direction: "HIGHER_BETTER",
        });
      }
      return Promise.resolve({ enabled: true });
    }
    return Promise.reject(new Error(`unexpected read: ${url}`));
  });
});

describe("QAOverview", () => {
  test("renders the six sections; remaining placeholders match the light-up plan", async () => {
    await renderPage();

    expect(
      screen.getByRole("heading", { name: "QA Overview" }),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/Daily snapshot for the QA Officer/),
    ).toBeInTheDocument();

    // This Week / Pillars / Activity, the QC/EQA attention rows, the Today
    // Amendment tile and the Average TAT tile are all live now (NCE Pulse
    // came first). These placeholders remain.
    const slotCounts = {
      "Attention Required": 1,
      Today: 0,
      "This Week": 0,
      "Pillar Status": 1,
      "Recent Activity": 0,
    };
    Object.entries(slotCounts).forEach(([name, slots]) => {
      const region = screen.getByRole("region", { name });
      expect(within(region).queryAllByText("Coming soon")).toHaveLength(slots);
    });
  });

  test("slots sharing an endpoint read it once", async () => {
    await renderPage();

    // Five slots want the NCE register, two want the TAT rollup, four want
    // the overview summary, and three want the NCE indicator's config — each
    // is one cached read, not one per slot.
    expect(callsTo("/rest/nce/dashboard")).toHaveLength(1);
    expect(callsTo("/rest/qa/overview/summary")).toHaveLength(1);
    expect(callsTo("indicator=NCE")).toHaveLength(1);
    // The callback summary is asked for three genuinely different windows:
    // the tile's 30 days, the attention queue's 24 hours, and this week.
    expect(
      new Set(callsTo("/rest/critical-callback/summary").map(([url]) => url))
        .size,
    ).toBe(3);
  });

  test("Today tiles carry the KPI titles, tickets, and the live TAT/Amendment/NCE values", async () => {
    await renderPage();
    const today = screen.getByRole("region", { name: "Today" });

    [
      "Average TAT",
      "Rejection Rate",
      "Amendment Rate",
      "Critical Callback",
      "NCE Pulse",
    ].forEach((title) => {
      expect(within(today).getByText(title)).toBeInTheDocument();
    });
    // All five tiles are live — no ticket badges remain (OGC-697/714 lit up)
    ["OGC-696", "OGC-697", "OGC-714"].forEach((ticket) => {
      expect(within(today).queryByText(ticket)).not.toBeInTheDocument();
    });
    // Average TAT is live: value + prior-window delta, no ticket
    expect(within(today).queryByText("OGC-696")).not.toBeInTheDocument();
    const tile = (indicator) =>
      within(today).getByTestId(`qa-overview-tile-${indicator}`);
    expect(tile("tat")).toHaveTextContent("33h 20m");
    expect(tile("tat")).toHaveTextContent("↓ 6h 40m");
    expect(tile("tat")).toHaveTextContent("vs prior 30 days");
    // NCE Pulse is live: no ticket tag, real counts from the mocked payload
    expect(within(today).queryByText("OGC-699")).not.toBeInTheDocument();
    expect(tile("nce")).toHaveTextContent("3");
    expect(tile("nce").className).toContain("qi-tile--amber");
    expect(tile("nce")).toHaveTextContent("3 in corrective action");
    // Amendment Rate is live: rate under the green threshold
    expect(within(today).queryByText("OGC-698")).not.toBeInTheDocument();
    expect(tile("amendment")).toHaveTextContent("0.31%");
    expect(tile("amendment").className).toContain("qi-tile--green");
    expect(tile("amendment")).toHaveTextContent("8 of 2580 released");
    // Rejection Rate is live (OGC-697/710): between target 2 and action 5
    expect(tile("rejection")).toHaveTextContent("2.50%");
    expect(tile("rejection").className).toContain("qi-tile--amber");
    expect(tile("rejection")).toHaveTextContent("3 of 120 started");
    // Callback compliance is live (OGC-714/715): 75% below the 100% target
    expect(tile("callback")).toHaveTextContent("75.00%");
    expect(tile("callback").className).toContain("qi-tile--red");
    expect(tile("callback")).toHaveTextContent("3 of 4 confirmed");
  });

  test("attention queue: live NCE, QC-violation, and EQA-due rows with drill-throughs", async () => {
    await renderPage();
    const attention = screen.getByRole("region", {
      name: "Attention Required",
    });

    const nceRow = within(attention).getByRole("button", {
      name: /Critical NCEs pending acknowledgment/,
    });
    expect(within(nceRow).getByText("3")).toBeInTheDocument();
    expect(nceRow).toHaveClass("qa-live-row-alert");

    const qcRow = within(attention).getByRole("button", {
      name: /QC violations in last 24 hours/,
    });
    expect(within(qcRow).getByText("2")).toBeInTheDocument();
    expect(qcRow).toHaveClass("qa-live-row-alert");

    const eqaRow = within(attention).getByRole("button", {
      name: /EQA submissions due in next 14 days/,
    });
    expect(within(eqaRow).getByText("3")).toBeInTheDocument();
    expect(eqaRow).not.toHaveClass("qa-live-row-alert");

    // Critical results (last 24h) is live: 4 critical, 1 unconfirmed => alert
    const criticalRow = within(attention).getByRole("button", {
      name: /Critical results in last 24 hours/,
    });
    expect(within(criticalRow).getByText("4")).toBeInTheDocument();
    expect(criticalRow).toHaveClass("qa-live-row-alert");

    // Overdue CAPAs is live: one open row past its due date
    const capaRow = within(attention).getByRole("button", {
      name: /Overdue CAPAs/,
    });
    expect(within(capaRow).getByText("1")).toBeInTheDocument();
    expect(capaRow).toHaveClass("qa-live-row-alert");

    // Effectiveness reviews due is live: one CAPA awaiting a verdict (the
    // reviewed-"No" CAPA doesn't count); only the NCE-v2 SLA row remains
    const reviewRow = within(attention).getByRole("button", {
      name: /CAPA effectiveness reviews due/,
    });
    expect(within(reviewRow).getByText("1")).toBeInTheDocument();
    expect(reviewRow).toHaveClass("qa-live-row-alert");
    expect(within(attention).queryByText("OGC-707")).not.toBeInTheDocument();
    expect(within(attention).getByText("NCE v2")).toBeInTheDocument();
  });

  test("This Week counters aggregate NCE, QC, EQA, audit, and signature numbers", async () => {
    await renderPage();
    const week = screen.getByRole("region", { name: "This Week" });

    const stat = (label) =>
      within(week).getByText(label).closest(".qa-live-stat");

    expect(within(stat("New NCEs")).getByText("4")).toBeInTheDocument();
    expect(
      within(stat("New NCEs")).getByText("3 critical · 1 major · 0 minor"),
    ).toBeInTheDocument();
    expect(within(stat("NCEs resolved")).getByText("1")).toBeInTheDocument();
    expect(within(stat("CAPAs completed")).getByText("1")).toBeInTheDocument();
    expect(within(stat("QC violations")).getByText("2")).toBeInTheDocument();
    expect(
      within(stat("QC violations")).getByText("1 × 1-3s · 1 × 1-2s"),
    ).toBeInTheDocument();
    expect(within(stat("EQA submissions")).getByText("5")).toBeInTheDocument();
    expect(
      within(stat("EQA submissions")).getByText("currently open · 1 overdue"),
    ).toBeInTheDocument();
    expect(
      within(stat("Audit log entries")).getByText("8,732"),
    ).toBeInTheDocument();
    expect(
      within(stat("Signature events")).getByText("1,247"),
    ).toBeInTheDocument();

    // Critical results is live (OGC-714/715): week window with confirmations
    expect(within(stat("Critical results")).getByText("4")).toBeInTheDocument();
    expect(
      within(stat("Critical results")).getByText("3 confirmed"),
    ).toBeInTheDocument();
  });

  test("pillar chips roll up QC, QI, and QMS status; EQA stays a placeholder", async () => {
    await renderPage();
    const pillars = screen.getByRole("region", { name: "Pillar Status" });

    // Name labels stay bound to the right chips
    [
      "Statistical QC",
      "EQA",
      "Quality Indicators",
      "QMS & Improvement",
    ].forEach((pillar) => {
      expect(within(pillars).getByText(pillar)).toBeInTheDocument();
    });

    const qcChip = within(pillars)
      .getByText("4 of 6 instruments in control")
      .closest(".qa-pillar-chip");
    expect(qcChip).toHaveClass("qa-pillar-red");
    expect(within(qcChip).getByText("Statistical QC")).toBeInTheDocument();

    // 33.3333h mean, improved by 6h40m vs prior window
    const qiChip = within(pillars)
      .getByText(/Avg TAT 33h 20m/)
      .closest(".qa-pillar-chip");
    expect(qiChip).toHaveClass("qa-pillar-green");
    expect(qiChip).toHaveTextContent("↓ 6h 40m");

    const qmsChip = within(pillars)
      .getByText("3 critical pending · 3 in corrective action")
      .closest(".qa-pillar-chip");
    expect(qmsChip).toHaveClass("qa-pillar-red");

    expect(within(pillars).getByText("OGC-721")).toBeInTheDocument();
  });

  test("recent activity merges NCE history, e-signature, and QC alert rows newest-first", async () => {
    await renderPage();
    const activity = screen.getByRole("region", { name: "Recent Activity" });

    const rows = activity.querySelectorAll(".qa-activity-row");
    expect(rows.length).toBe(3);
    expect(rows[0]).toHaveTextContent("Sara Chen resolved NCE-0004");
    expect(rows[1]).toHaveTextContent(
      "L. Tran validated & released RESULT #44196",
    );
    expect(rows[2]).toHaveTextContent(
      "QC alert: 1-2s (WARNING) on Architect ci8200",
    );

    expect(
      within(activity).getByRole("link", { name: /View full audit trail/ }),
    ).toHaveAttribute("href", "/qa/qms/audit-trail");
  });

  test("inspector readiness answers Q1/Q3/Q4/Q5 and keeps the Q2 placeholder; open state sticks", async () => {
    const view = await renderPage();
    const heading = screen.getByRole("button", {
      name: /Inspector readiness/,
    });
    expect(heading).toHaveAttribute("aria-expanded", "false");

    fireEvent.click(heading);
    expect(heading).toHaveAttribute("aria-expanded", "true");
    expect(sessionStorage.getItem("qa.overview.inspectorOpen")).toBe("1");

    const inspector = screen.getByRole("region", {
      name: /Inspector readiness/,
    });
    expect(within(inspector).getAllByText("Coming soon")).toHaveLength(1);
    expect(
      within(inspector).getByText("4 of 6 instruments in control (30d)"),
    ).toBeInTheDocument();
    expect(
      within(inspector).getByText("Avg TAT 33h 20m vs prior 30d"),
    ).toBeInTheDocument();
    expect(
      within(inspector).getByText("3 critical NCEs pending acknowledgment"),
    ).toBeInTheDocument();
    expect(
      within(inspector).getByText("2 of 3 in force, 1 expiring, 0 expired"),
    ).toBeInTheDocument();

    view.unmount();
    await renderPage();
    expect(
      screen.getByRole("button", { name: /Inspector readiness/ }),
    ).toHaveAttribute("aria-expanded", "true");
  });
});
