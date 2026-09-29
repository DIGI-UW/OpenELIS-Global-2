import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";
import AnalystCompetencyPage from "../AnalystCompetencyPage";
import { getFromOpenElisServer } from "../../../utils/Utils";

vi.mock("../../../utils/Utils", async () => {
  const actual = await vi.importActual("../../../utils/Utils");
  return { ...actual, getFromOpenElisServer: vi.fn() };
});

vi.mock("../../../common/PageBreadCrumb", () => ({
  default: function MockBreadCrumb() {
    return <div data-testid="breadcrumb">breadcrumb</div>;
  },
}));

const ROLLUP = {
  windowStart: "2025-09-10",
  windowEnd: "2026-09-10",
  windowMonths: 12,
  evidenceFloor: 4,
  kpis: {
    analystCount: 4,
    competentCount: 1,
    underReviewCount: 2,
    notCompetentCount: 1,
    assessedSampleCount: 14,
  },
  analysts: [
    {
      analystId: 11,
      analystName: "Aisha Nakato",
      status: "COMPETENT",
      sampleCount: 6,
      sampleCountThisYear: 4,
      evaluableCount: 6,
      failureCount: 0,
      mostRecentPerformance: "acceptable",
      mostRecentDate: "2026-07-30",
      statusReasons: [
        {
          reason: "MEETS_EVIDENCE",
          analyteName: "HIV viral load",
          evaluableCount: 6,
          failureCount: 0,
        },
      ],
      analytes: [
        {
          analyteId: 1,
          analyteName: "HIV viral load",
          status: "COMPETENT",
          reason: "MEETS_EVIDENCE",
          evaluableCount: 6,
          failureCount: 0,
          latestPerformance: "acceptable",
          latestDate: "2026-07-30",
          openEscalation: false,
        },
      ],
      history: [
        {
          date: "2026-07-30",
          schemeName: "National HIV PT",
          analyteName: "HIV viral load",
          eventType: null,
          outcome: "acceptable",
          counted: true,
          failure: false,
          nceId: null,
        },
      ],
    },
    {
      analystId: 12,
      analystName: "Brian Okello",
      status: "NOT_COMPETENT",
      sampleCount: 5,
      sampleCountThisYear: 5,
      evaluableCount: 5,
      failureCount: 1,
      mostRecentPerformance: "unacceptable",
      mostRecentDate: "2026-08-02",
      statusReasons: [
        {
          reason: "OPEN_ESCALATION",
          analyteName: "CD4 count",
          evaluableCount: 5,
          failureCount: 1,
        },
      ],
      analytes: [
        {
          analyteId: 2,
          analyteName: "CD4 count",
          status: "NOT_COMPETENT",
          reason: "OPEN_ESCALATION",
          evaluableCount: 5,
          failureCount: 1,
          latestPerformance: "unacceptable",
          latestDate: "2026-08-02",
          openEscalation: true,
        },
      ],
      history: [
        {
          date: "2026-08-02",
          schemeName: "National CD4 PT",
          analyteName: "CD4 count",
          eventType: "ESCALATED_TO_NCE",
          outcome: "unacceptable",
          counted: false,
          failure: true,
          nceId: 44,
        },
        {
          date: "2026-05-11",
          schemeName: "National CD4 PT",
          analyteName: "CD4 count",
          eventType: "DISMISSED_EQUIPMENT",
          outcome: "dismissed",
          counted: false,
          failure: false,
          nceId: null,
        },
      ],
    },
    {
      analystId: 13,
      analystName: "Carol Achieng",
      status: "UNDER_REVIEW",
      sampleCount: 3,
      sampleCountThisYear: 3,
      evaluableCount: 3,
      failureCount: 0,
      mostRecentPerformance: "acceptable",
      mostRecentDate: "2026-06-01",
      statusReasons: [
        {
          reason: "INSUFFICIENT_EVIDENCE",
          analyteName: "Syphilis RPR",
          evaluableCount: 3,
          failureCount: 0,
        },
      ],
      analytes: [
        {
          analyteId: 3,
          analyteName: "Syphilis RPR",
          status: "UNDER_REVIEW",
          reason: "INSUFFICIENT_EVIDENCE",
          evaluableCount: 3,
          failureCount: 0,
          latestPerformance: "acceptable",
          latestDate: "2026-06-01",
          openEscalation: false,
        },
      ],
      history: [],
    },
    {
      analystId: 14,
      analystName: "Daniel Mwangi",
      status: "UNDER_REVIEW",
      sampleCount: 5,
      sampleCountThisYear: 5,
      evaluableCount: 5,
      failureCount: 2,
      mostRecentPerformance: "unacceptable",
      mostRecentDate: "2026-08-20",
      statusReasons: [
        {
          reason: "REPEATED_FAILURE",
          analyteName: "Hepatitis B surface antigen",
          evaluableCount: 5,
          failureCount: 2,
        },
      ],
      analytes: [
        {
          analyteId: 4,
          analyteName: "Hepatitis B surface antigen",
          status: "UNDER_REVIEW",
          reason: "REPEATED_FAILURE",
          evaluableCount: 5,
          failureCount: 2,
          latestPerformance: "unacceptable",
          latestDate: "2026-08-20",
          openEscalation: false,
        },
      ],
      history: [],
    },
  ],
};

const renderPage = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter>
        <AnalystCompetencyPage />
      </MemoryRouter>
    </IntlProvider>,
  );

describe("AnalystCompetencyPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getFromOpenElisServer.mockImplementation((_url, callback) =>
      callback(ROLLUP),
    );
  });

  it("counts the analysts in each band", async () => {
    renderPage();

    expect(await screen.findByTestId("kpi-analysts")).toHaveTextContent("4");
    expect(screen.getByTestId("kpi-analysts")).toHaveTextContent(
      "14 PT samples",
    );
    expect(screen.getByTestId("kpi-competent")).toHaveTextContent("1");
    expect(screen.getByTestId("kpi-under-review")).toHaveTextContent("2");
    expect(screen.getByTestId("kpi-not-competent")).toHaveTextContent("1");
  });

  it("shows each analyst's band and sample counts", async () => {
    const { container } = renderPage();

    await screen.findByText("Brian Okello");
    const row = [...container.querySelectorAll("tbody tr")].find((tr) =>
      tr.textContent.includes("Brian Okello"),
    );
    expect(row).toHaveTextContent("Not competent");
    expect(row).toHaveTextContent("5 this year · 1 failed");
    expect(row).toHaveTextContent("Unacceptable");
  });

  it("expands to the per-analyte bands and the events behind them", async () => {
    renderPage();

    await screen.findByText("Brian Okello");
    const buttons = screen.getAllByRole("button", { name: /View history/i });
    await userEvent.click(buttons[1]);

    const history = await screen.findByTestId("history-12");
    expect(history).toHaveTextContent("CD4 count");
    expect(history).toHaveTextContent("Escalated to non-conformity");
    // The equipment dismissal is evidence, but it is not held against them.
    expect(history).toHaveTextContent("Dismissed — equipment");
    expect(history).toHaveTextContent("Excused");
    expect(history).toHaveTextContent("Failure");
  });

  it("tells the two under-review rules apart on the row itself", async () => {
    const { container } = renderPage();

    await screen.findByText("Carol Achieng");
    const rowFor = (name) =>
      [...container.querySelectorAll("tbody tr")].find((tr) =>
        tr.textContent.includes(name),
      );

    // Same band, opposite findings. Carol has done too little to judge; Daniel
    // has failed twice. The band alone cannot separate them, so the rule does.
    const carol = rowFor("Carol Achieng");
    expect(carol).toHaveTextContent("Under review");
    expect(carol).toHaveTextContent("Insufficient evidence · Syphilis RPR");
    expect(carol).toHaveTextContent("short of the evidence floor of 4");
    expect(carol).toHaveTextContent("not a performance concern");
    expect(carol).toHaveTextContent(
      "assign this analyst more proficiency testing samples",
    );

    const daniel = rowFor("Daniel Mwangi");
    expect(daniel).toHaveTextContent("Under review");
    expect(daniel).toHaveTextContent(
      "Repeated failure · Hepatitis B surface antigen",
    );
    expect(daniel).toHaveTextContent("2 failures in 5 assessable samples");
    expect(daniel).toHaveTextContent("This is a performance concern");
    expect(daniel).not.toHaveTextContent("Insufficient evidence");
  });

  it("names the rule behind a band that is not under review", async () => {
    const { container } = renderPage();

    await screen.findByText("Brian Okello");
    const brian = [...container.querySelectorAll("tbody tr")].find((tr) =>
      tr.textContent.includes("Brian Okello"),
    );
    expect(brian).toHaveTextContent("Open non-conformity · CD4 count");
    expect(brian).toHaveTextContent(
      "Close the non-conformity to clear the band",
    );
  });

  it("says what an analyst is, and what it is not", async () => {
    renderPage();

    expect(
      await screen.findByText(/An analyst is an OpenELIS user account/i),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/separate record from whoever entered the result/i),
    ).toBeInTheDocument();
  });

  it("filters the table by analyst name", async () => {
    renderPage();

    await screen.findByText("Aisha Nakato");
    await userEvent.type(screen.getByRole("searchbox"), "Carol");

    expect(screen.getByText("Carol Achieng")).toBeInTheDocument();
    expect(screen.queryByText("Aisha Nakato")).not.toBeInTheDocument();
  });

  it("says why the page is empty rather than showing a bare table", async () => {
    getFromOpenElisServer.mockImplementation((_url, callback) =>
      callback({ kpis: { analystCount: 0 }, analysts: [] }),
    );
    renderPage();

    expect(
      await screen.findByText(/No analyst has been recorded/i),
    ).toBeInTheDocument();
  });
});
