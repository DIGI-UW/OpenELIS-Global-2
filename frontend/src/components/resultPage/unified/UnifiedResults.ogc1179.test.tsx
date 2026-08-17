import React from "react";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  within,
} from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";

/**
 * OGC-1179 — the unified /Results worklist, at the level the findings were
 * reported at: what the page does when a technician opens a saved row, changes
 * nothing, changes something, and is told their save lost a race.
 *
 * The server layer is mocked so the page's own behaviour is what is under
 * test; the payloads are the shapes the real endpoints return.
 */

const { utilsMock } = vi.hoisted(() => ({
  utilsMock: {
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
    postToOpenElisServerFormData: vi.fn(),
    deleteFromOpenElisServer: vi.fn(),
  },
}));

vi.mock("../../utils/Utils", () => utilsMock);

vi.mock("../../layout/Layout", () => ({
  NotificationContext: React.createContext({
    notificationVisible: false,
    setNotificationVisible: vi.fn(),
    addNotification: vi.fn(),
  }),
  ConfigurationContext: React.createContext({
    configurationProperties: { allowResultRejection: "false" },
  }),
}));

vi.mock("../../common/CustomNotification", () => ({
  AlertDialog: () => <div />,
  NotificationKinds: { success: "success", error: "error", warning: "warning" },
}));

// The e-signature ceremony has its own tests; here it must only stand in for
// the Save control and report the meaning it was handed.
vi.mock("../../esignature/ESignatureButton", () => ({
  __esModule: true,
  default: ({ children, onSign, disabled, meaning }: any) => (
    <button
      type="button"
      data-testid="save-button"
      data-meaning={meaning}
      disabled={disabled}
      onClick={() => onSign({})}
    >
      {children}
    </button>
  ),
  SignatureMeaning: {
    AUTHORED: "AUTHORED",
    MODIFIED: "MODIFIED",
    VALIDATED_AND_RELEASED: "VALIDATED_AND_RELEASED",
    REJECTED: "REJECTED",
  },
}));

vi.mock("../../common/PageBreadCrumb", () => ({ default: () => <div /> }));

import UnifiedResults from "./UnifiedResults";

/** A numeric result stored more precisely than its test reports to. */
const worklistRow = (overrides = {}) => ({
  id: "r-71",
  analysisId: "60",
  testResultComponentId: "c-n2",
  accessionNumber: "DEV0126000000000032",
  testName: "COVID-19 PCR — N2 (Ct)",
  resultType: "N",
  resultValue: "33",
  rawResultValue: "33.7",
  significantDigits: 0,
  analysisStatusId: "4",
  analysisLastupdated: "1700000000000",
  normalRange: "",
  sampleType: "Respiratory Swab",
  ...overrides,
});

const LAB_UNITS = [
  { id: "165", value: "Molecular Biology", domain: "CLINICAL" },
];
const STATUSES = [
  { id: "4", value: "Not started" },
  { id: "15", value: "Accepted by technician" },
];

const wireServer = (rows: any[]) => {
  utilsMock.getFromOpenElisServer.mockImplementation(
    (endPoint: string, callback: any) => {
      if (endPoint === "/rest/results-entry/lab-units")
        return callback(LAB_UNITS);
      if (endPoint === "/rest/analysis-status-types") return callback(STATUSES);
      if (endPoint.startsWith("/rest/LogbookResults"))
        return callback({ testResult: rows });
      return callback([]);
    },
  );
};

const renderPage = async (rows = [worklistRow()]) => {
  wireServer(rows);
  const utils = render(
    <IntlProvider locale="en" messages={messages}>
      <UnifiedResults />
    </IntlProvider>,
  );
  // the page loads its worklist once a Lab Unit is selected
  fireEvent.change(screen.getByLabelText(/Lab Unit/i), {
    target: { value: "165" },
  });
  expect(
    screen.getByRole("table", { name: "Results worklist" }),
  ).toBeInTheDocument();
  expect(document.querySelector("tbody tr")).not.toBeNull();
  return utils;
};

const resultFields = () =>
  Array.from(
    document.querySelectorAll('input[id^="unifiedResultValue-"]'),
  ) as HTMLInputElement[];

const resultField = () => resultFields()[0];

describe("OGC-1179 — unified Results worklist", () => {
  beforeEach(() => {
    // RTL 9 under vitest does not auto-unmount between tests
    cleanup();
    utilsMock.getFromOpenElisServer.mockReset();
    utilsMock.postToOpenElisServerJsonResponse.mockReset();
    window.localStorage.clear();
  });

  /** #7 — the worklist table is named, so the History table inside a row is not confused with it. */
  it("names the results table", async () => {
    await renderPage();
    expect(
      screen.getByRole("table", { name: "Results worklist" }),
    ).toBeInTheDocument();
  });

  /** #2 — Save is not offered, so no Part 11 signature is demanded, until something changes. */
  it("offers no Save on a row opened for Edit and left untouched", async () => {
    await renderPage();

    expect(screen.queryByTestId("save-button")).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /Edit/i }));
    expect(screen.queryByTestId("save-button")).not.toBeInTheDocument();

    fireEvent.change(resultField(), { target: { value: "34" } });
    expect(screen.getByTestId("save-button")).toBeInTheDocument();
  });

  /** #1 — Edit repopulates from the stored value, not from the reported one. */
  it("edits the stored value, not the rounded one it reports", async () => {
    await renderPage();

    fireEvent.click(screen.getByRole("button", { name: /Edit/i }));

    expect(resultField().value).toBe("33.7");
  });

  /** #3 — a revision is signed as a revision, not as authorship. */
  it("signs a revision as MODIFIED", async () => {
    await renderPage();

    fireEvent.click(screen.getByRole("button", { name: /Edit/i }));
    fireEvent.change(resultField(), { target: { value: "34" } });

    expect(screen.getByTestId("save-button")).toHaveAttribute(
      "data-meaning",
      "MODIFIED",
    );
  });

  it("signs a first entry as AUTHORED", async () => {
    await renderPage([worklistRow({ resultValue: "", rawResultValue: "" })]);

    fireEvent.change(resultField(), { target: { value: "12" } });

    expect(screen.getByTestId("save-button")).toHaveAttribute(
      "data-meaning",
      "AUTHORED",
    );
  });

  /** #1 (entry half) — a newly typed out-of-precision value cannot be saved. */
  it("refuses to save a value finer than the test reports to", async () => {
    await renderPage();
    fireEvent.click(screen.getByRole("button", { name: /Edit/i }));

    fireEvent.change(resultField(), { target: { value: "34.55" } });
    expect(screen.getByTestId("save-button")).toBeDisabled();

    fireEvent.change(resultField(), { target: { value: "34" } });
    expect(screen.getByTestId("save-button")).toBeEnabled();
  });

  /** #4 — the Status chip and the counts follow the save, without a reload. */
  it("adopts the status the save reports", async () => {
    await renderPage();
    expect(screen.getByText("Not started")).toBeInTheDocument();

    utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
      (_url: string, _body: string, callback: any) =>
        callback({
          analysisStatusId: "15",
          analysisLastupdated: "1700000009999",
          resultId: "71",
          resultValue: "34",
          rawResultValue: "34",
        }),
    );

    fireEvent.click(screen.getByRole("button", { name: /Edit/i }));
    fireEvent.change(resultField(), { target: { value: "34" } });
    fireEvent.click(screen.getByTestId("save-button"));

    expect(screen.getByText("Accepted by technician")).toBeInTheDocument();
    expect(screen.queryByText("Not started")).not.toBeInTheDocument();
  });

  /**
   * #1 — a row saved and edited again, with no reload between, still edits what
   * is stored.
   *
   * <p>Reporting pads as well as truncates: a value of 110 on a test reporting
   * to two places is reported as "110.00". A row that kept the reported form
   * would write it back on its next save, so it adopts what the save says was
   * stored.
   */
  it("adopts the persisted value so a second edit is not a rewrite", async () => {
    await renderPage([
      worklistRow({
        resultValue: "110.00",
        rawResultValue: "110",
        significantDigits: 2,
      }),
    ]);
    utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
      (_url: string, _body: string, callback: any) =>
        callback({
          resultId: "71",
          analysisStatusId: "15",
          resultValue: "34.20",
          rawResultValue: "34.2",
        }),
    );

    fireEvent.click(screen.getByRole("button", { name: /Edit/i }));
    expect(resultField().value).toBe("110");
    fireEvent.change(resultField(), { target: { value: "34.2" } });
    fireEvent.click(screen.getByTestId("save-button"));

    fireEvent.click(screen.getByRole("button", { name: /Edit/i }));
    expect(resultField().value).toBe("34.2");
  });

  /** #6 — a stale save offers a refresh the user can actually press. */
  it("gives the stale-save notice a working Refresh action", async () => {
    await renderPage();
    utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
      (_url: string, _body: string, callback: any) =>
        callback({
          status: 409,
          error: "error.results.staleSave",
          modifiedBy: "ELIS,Open",
          modifiedAt: "2026-08-13 02:11:16",
        }),
    );

    fireEvent.click(screen.getByRole("button", { name: /Edit/i }));
    fireEvent.change(resultField(), { target: { value: "34" } });
    fireEvent.click(screen.getByTestId("save-button"));

    const notice = document.querySelector(
      ".cds--actionable-notification",
    ) as HTMLElement;
    expect(notice).not.toBeNull();
    const refresh = within(notice).getByRole("button", { name: /Refresh/i });
    expect(refresh).toBeInTheDocument();

    const loadsBefore = utilsMock.getFromOpenElisServer.mock.calls.filter(
      (c: any[]) => String(c[0]).startsWith("/rest/LogbookResults"),
    ).length;
    fireEvent.click(refresh);
    const loadsAfter = utilsMock.getFromOpenElisServer.mock.calls.filter(
      (c: any[]) => String(c[0]).startsWith("/rest/LogbookResults"),
    ).length;
    expect(loadsAfter).toBeGreaterThan(loadsBefore);
  });
});

/**
 * A multi-component analysis renders one row per component sharing an
 * analysisId (FR-A′1). The save response carries analysis-level facts — the
 * version token and the status — which every row of that analysis must adopt,
 * and row-level facts, which only the saved row may. A sibling being edited at
 * the same time must come through untouched.
 */
describe("OGC-1179 — a save and its sibling component rows", () => {
  const componentRow = (componentId: string, value: string) => ({
    id: `r-${componentId}`,
    analysisId: "60",
    testResultComponentId: componentId,
    accessionNumber: "DEV0126000000000032",
    testName: `COVID-19 PCR — ${componentId}`,
    resultType: "N",
    resultValue: value,
    rawResultValue: value,
    significantDigits: 2,
    analysisStatusId: "4",
    analysisLastupdated: "1700000000000",
  });

  beforeEach(() => {
    cleanup();
    utilsMock.getFromOpenElisServer.mockReset();
    utilsMock.postToOpenElisServerJsonResponse.mockReset();
    window.localStorage.clear();
  });

  it("leaves a sibling's unsaved edit alone while adopting the analysis status", async () => {
    await renderPage([componentRow("n2", "10"), componentRow("e", "20")]);

    const field = (componentId: string) =>
      document.getElementById(
        `unifiedResultValue-60-${componentId}`,
      ) as HTMLInputElement;

    // the technician opens both components and types into each
    screen.getAllByRole("button", { name: /Edit/i }).forEach((b) => {
      fireEvent.click(b);
    });
    fireEvent.change(field("n2"), { target: { value: "11" } });
    fireEvent.change(field("e"), { target: { value: "21" } });

    utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
      (_url: string, _body: string, callback: any) =>
        callback({
          resultId: "71",
          analysisStatusId: "15",
          analysisLastupdated: "1700000009999",
          resultValue: "11.00",
          rawResultValue: "11",
        }),
    );

    // and saves only the first
    fireEvent.click(screen.getAllByTestId("save-button")[0]);

    // the sibling kept exactly what was typed into it
    expect(field("e").value).toBe("21");
    // and the analysis-level status reached both rows
    expect(screen.getAllByText("Accepted by technician").length).toBe(2);
  });
});
