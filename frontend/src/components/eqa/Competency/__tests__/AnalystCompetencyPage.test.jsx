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
  kpis: {
    analystCount: 3,
    competentCount: 1,
    underReviewCount: 1,
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
      analytes: [
        {
          analyteId: 1,
          analyteName: "HIV viral load",
          status: "COMPETENT",
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
      analytes: [
        {
          analyteId: 2,
          analyteName: "CD4 count",
          status: "NOT_COMPETENT",
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
      analytes: [],
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

    expect(await screen.findByTestId("kpi-analysts")).toHaveTextContent("3");
    expect(screen.getByTestId("kpi-analysts")).toHaveTextContent(
      "14 PT samples",
    );
    expect(screen.getByTestId("kpi-competent")).toHaveTextContent("1");
    expect(screen.getByTestId("kpi-under-review")).toHaveTextContent("1");
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
