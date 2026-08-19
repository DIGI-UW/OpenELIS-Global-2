import React from "react";
import { render, screen, within, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";
import MyCyclesPage from "../MyCyclesPage";
import { MOCK_CYCLES } from "../mockCycles";
import {
  getFromOpenElisServer,
  patchToOpenElisServerJsonResponse,
} from "../../../utils/Utils";

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  patchToOpenElisServerJsonResponse: vi.fn(),
}));

vi.mock("../../../common/PageBreadCrumb", () => ({
  default: function MockBreadCrumb() {
    return <div data-testid="breadcrumb">breadcrumb</div>;
  },
}));

const UNCYCLED_ORDERS = [
  {
    id: 41,
    labNumber: "DEV01260000000000014",
    programName: "WHO AFRO HIV Viral Load EQA",
    status: "PENDING",
    deadline: "2026-05-02",
  },
];

const renderPage = (orders = UNCYCLED_ORDERS, cycles = MOCK_CYCLES) => {
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url.startsWith("/rest/eqa/cycles/mine")) cb(cycles);
    if (url.startsWith("/rest/eqa/orders")) cb(orders);
  });
  return render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter>
        <MyCyclesPage />
      </MemoryRouter>
    </IntlProvider>,
  );
};

describe("MyCyclesPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test("KPI tiles derive counts from fixture data", () => {
    renderPage();
    // Fixtures: 4 active (planned, panel_received, testing, ready_to_submit),
    // 1 ready_to_submit, 1 submitted, 1 with an open NCE.
    expect(
      within(screen.getByTestId("kpi-active")).getByText("4"),
    ).toBeTruthy();
    expect(within(screen.getByTestId("kpi-ready")).getByText("1")).toBeTruthy();
    expect(
      within(screen.getByTestId("kpi-awaiting")).getByText("1"),
    ).toBeTruthy();
    expect(within(screen.getByTestId("kpi-nce")).getByText("1")).toBeTruthy();
  });

  test("default Active bucket shows only in-flight cycles", () => {
    renderPage();
    expect(screen.getByTestId("cycle-row-1")).toBeInTheDocument();
    expect(screen.getByTestId("cycle-row-2")).toBeInTheDocument();
    expect(screen.getByTestId("cycle-row-5")).toBeInTheDocument();
    expect(screen.getByTestId("cycle-row-6")).toBeInTheDocument();
    // submitted / scored / closed stay out of the Active bucket
    expect(screen.queryByTestId("cycle-row-3")).not.toBeInTheDocument();
    expect(screen.queryByTestId("cycle-row-4")).not.toBeInTheDocument();
  });

  test("row expansion reveals sample progress with result-entry deep links", () => {
    renderPage();
    fireEvent.click(screen.getByTestId("cycle-row-1"));
    const expanded = screen.getByTestId("cycle-expanded-1");
    expect(within(expanded).getByText("Sample progress")).toBeInTheDocument();
    const link = within(expanded).getByText("2026-00018421");
    expect(link.closest("a")).toHaveAttribute(
      "href",
      "/result?type=order&doRange=false&accessionNumber=2026-00018421",
    );
    // per-analyst column absent for a non-per-analyst scheme
    expect(within(expanded).queryByText("Assigned analyst")).toBeNull();
    // no review gate on this scheme, so no pre-submission summary columns
    expect(within(expanded).queryByText("Reported value")).toBeNull();
    expect(within(expanded).queryByText("Pre-submission summary")).toBeNull();
  });

  test("review-gated cycle at ready_to_submit shows the pre-submission summary", () => {
    renderPage();
    fireEvent.click(screen.getByTestId("cycle-row-2"));
    const expanded = screen.getByTestId("cycle-expanded-2");

    expect(
      within(expanded).getByText("Pre-submission summary"),
    ).toBeInTheDocument();
    expect(within(expanded).getByText("Reported value")).toBeInTheDocument();
    expect(within(expanded).getByText("Validated")).toBeInTheDocument();
    // one row per analyte, so each reported value is checkable on its own line
    expect(within(expanded).getAllByText("MTB Detection").length).toBe(3);
    expect(within(expanded).getAllByText("Detected").length).toBe(3);
    expect(within(expanded).getAllByText("Not detected").length).toBe(3);
    // identity columns are not repeated on an analyte's continuation row
    expect(within(expanded).getAllByText("NHRL-TB-01").length).toBe(1);
  });

  test("summary is hidden once the gated cycle leaves ready_to_submit", () => {
    // same scheme flag, but a closed cycle — the gate is state-dependent
    renderPage(UNCYCLED_ORDERS, [
      { ...MOCK_CYCLES.find((c) => c.id === 7), samples: [] },
    ]);
    fireEvent.change(screen.getByLabelText("Status"), {
      target: { value: "completed" },
    });
    fireEvent.click(screen.getByTestId("cycle-row-7"));
    const expanded = screen.getByTestId("cycle-expanded-7");
    expect(within(expanded).queryByText("Pre-submission summary")).toBeNull();
    expect(within(expanded).queryByText("Review & submit")).toBeNull();
  });

  test("per-analyst scheme shows analyst column in the sample table", () => {
    renderPage();
    fireEvent.click(screen.getByTestId("cycle-row-2"));
    const expanded = screen.getByTestId("cycle-expanded-2");
    expect(within(expanded).getByText("Assigned analyst")).toBeInTheDocument();
    expect(within(expanded).getAllByText("J. Otieno").length).toBe(2);
  });

  test("uncycled EQA orders bucket renders from /rest/eqa/orders", () => {
    renderPage();
    const table = screen.getByTestId("uncycled-table");
    const link = within(table).getByText("DEV01260000000000014");
    expect(link.closest("a")).toHaveAttribute(
      "href",
      "/result?type=order&doRange=false&accessionNumber=DEV01260000000000014",
    );
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/eqa/orders",
      expect.any(Function),
    );
  });

  test("uncycled section hidden when no orders", () => {
    renderPage([]);
    expect(screen.queryByTestId("uncycled-table")).toBeNull();
  });

  test("filters produce empty state and clear restores", () => {
    renderPage();
    fireEvent.change(screen.getByRole("searchbox"), {
      target: { value: "no-such-scheme" },
    });
    expect(
      screen.getByText("No cycles match these filters."),
    ).toBeInTheDocument();
    fireEvent.click(screen.getByText("Clear filters"));
    expect(screen.getByTestId("cycle-row-1")).toBeInTheDocument();
  });

  test("Review & submit PATCHes the transition endpoint and flips the row", () => {
    patchToOpenElisServerJsonResponse.mockImplementation((url, payload, cb) =>
      cb({ id: 2, status: "SUBMITTED" }),
    );
    renderPage();
    fireEvent.click(screen.getByTestId("cycle-row-2"));
    fireEvent.click(screen.getByText("Review & submit"));

    expect(patchToOpenElisServerJsonResponse).toHaveBeenCalledWith(
      "/rest/eqa/cycles/2/transition",
      JSON.stringify({
        newState: "SUBMITTED",
        stateMachine: "PARTICIPANT",
        reason: "Participant review & submit from My Cycles",
      }),
      expect.any(Function),
    );
    // leaves the Active bucket, awaiting KPI now counts it
    expect(screen.queryByTestId("cycle-row-2")).not.toBeInTheDocument();
    expect(
      within(screen.getByTestId("kpi-awaiting")).getByText("2"),
    ).toBeTruthy();
    expect(
      screen.getByText("Cycle submitted to provider — awaiting scores."),
    ).toBeInTheDocument();
  });

  test("failed submit (409/422) shows an error and leaves the row in place", () => {
    patchToOpenElisServerJsonResponse.mockImplementation((url, payload, cb) =>
      cb(undefined),
    );
    renderPage();
    fireEvent.click(screen.getByTestId("cycle-row-2"));
    fireEvent.click(screen.getByText("Review & submit"));

    expect(screen.getByTestId("cycle-row-2")).toBeInTheDocument();
    expect(
      screen.getByText(
        "Submit failed — the cycle was not advanced. Check that all results are validated and try again.",
      ),
    ).toBeInTheDocument();
  });
});
