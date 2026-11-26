import React from "react";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";

/**
 * OGC-1200 slice 1 on the worklist: a pending test whose analyzer value waits
 * in a run says so, cannot be entered around the run and opens it; the Runs
 * chip counts the runs; a save the server refuses because a value arrived in
 * a run meanwhile turns the row into a waiting one.
 */

const { utilsMock, notify } = vi.hoisted(() => ({
  utilsMock: {
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
    postToOpenElisServerFormData: vi.fn(),
    deleteFromOpenElisServer: vi.fn(),
  },
  notify: { addNotification: vi.fn(), setNotificationVisible: vi.fn() },
}));

vi.mock("../../utils/Utils", () => utilsMock);

vi.mock("../../layout/Layout", () => ({
  NotificationContext: React.createContext({
    notificationVisible: false,
    ...notify,
  }),
  ConfigurationContext: React.createContext({
    configurationProperties: { allowResultRejection: "false" },
  }),
}));

vi.mock("../../common/CustomNotification", () => ({
  AlertDialog: () => <div />,
  NotificationKinds: { success: "success", error: "error", warning: "warning" },
}));

vi.mock("../../esignature/ESignatureButton", () => ({
  __esModule: true,
  default: ({ children, onSign }: any) => (
    <button type="button" data-testid="save-button" onClick={() => onSign({})}>
      {children}
    </button>
  ),
  SignatureMeaning: { AUTHORED: "AUTHORED", MODIFIED: "MODIFIED" },
}));

vi.mock("../../common/PageBreadCrumb", () => ({ default: () => <div /> }));

import UnifiedResults from "./UnifiedResults";
import { chooseLabUnit } from "./chooseLabUnit.test-utils";

const WAITING = {
  runId: 7,
  label: "RUN-7",
  source: "ANALYZER",
  openedAt: "2026-11-04T08:00:00Z",
};

const row = (overrides = {}) => ({
  id: "r-1",
  analysisId: "61",
  accessionNumber: "DEV0126000000000400",
  testName: "Glucose",
  resultType: "N",
  resultValue: "",
  analysisStatusId: "4",
  analysisLastupdated: "1700000000000",
  normalRange: "",
  sampleType: "Serum",
  ...overrides,
});

const LAB_UNITS = [{ id: "36", value: "Biochemistry", domain: "CLINICAL" }];

const renderPage = (rows: any[], onOpenRun = vi.fn()) => {
  utilsMock.getFromOpenElisServer.mockImplementation(
    (endPoint: string, callback: any) => {
      if (endPoint === "/rest/results-entry/lab-units")
        return callback(LAB_UNITS);
      if (endPoint.startsWith("/rest/result-runs"))
        return callback({
          open: 1,
          held: 0,
          runs: [
            {
              id: 7,
              label: "RUN-7",
              source: "ANALYZER",
              state: "OPEN",
              analyzerName: "Mindray",
              openedAt: "2026-11-04T08:00:00Z",
              dispositioned: 1,
              total: 4,
            },
          ],
        });
      if (endPoint.startsWith("/rest/LogbookResults"))
        return callback({ testResult: rows });
      return callback([]);
    },
  );
  render(
    <IntlProvider locale="en" messages={messages}>
      <UnifiedResults onOpenRun={onOpenRun} />
    </IntlProvider>,
  );
  chooseLabUnit("Biochemistry");
  return onOpenRun;
};

const resultField = () =>
  document.querySelector(
    'input[id^="unifiedResultValue-"]',
  ) as HTMLInputElement | null;

describe("OGC-1200 runs on the unified worklist", () => {
  beforeEach(() => {
    cleanup();
    utilsMock.getFromOpenElisServer.mockReset();
    utilsMock.postToOpenElisServerJsonResponse.mockReset();
    notify.addNotification.mockReset();
    window.localStorage.clear();
  });

  it("names the run a pending test's value waits in and offers no entry", () => {
    const onOpenRun = renderPage([row({ waitingRun: WAITING })]);

    const tag = screen.getByTestId("waiting-run-7");
    expect(tag).toHaveTextContent("Waiting in RUN-7");
    expect(tag.parentElement).toHaveTextContent("Analyzer message");
    expect(resultField()).toBeNull();
    expect(screen.queryByTestId("save-button")).not.toBeInTheDocument();

    fireEvent.click(tag);
    expect(onOpenRun).toHaveBeenCalledWith(7);
  });

  it("shows held rows by default under a Held chip that hides them when turned off", () => {
    renderPage([
      row({ waitingRun: { ...WAITING, held: true } }),
      row({ id: "r-2", analysisId: "62", testName: "Urea" }),
    ]);

    const tag = screen.getByTestId("held-run-7");
    expect(tag).toHaveTextContent("Held: QC failed in RUN-7");
    const chip = screen.getByTestId("held-chip");
    expect(chip).toHaveTextContent("Held (1)");

    fireEvent.click(chip);

    expect(screen.queryByTestId("held-run-7")).not.toBeInTheDocument();
    expect(screen.getByText("Urea")).toBeInTheDocument();
    expect(screen.getByTestId("held-chip")).toHaveTextContent("Held (1)");
  });

  it("releases a row held for a missing control as soon as a control recorded on it clears the hold", () => {
    renderPage([
      row({
        testId: "1",
        runId: 7,
        runOfOneOpen: true,
        waitingRun: {
          ...WAITING,
          source: "RESULT_ENTRY",
          held: true,
          heldCause: "QC_MISSING",
        },
      }),
    ]);
    expect(screen.getByTestId("held-run-7")).toHaveTextContent(
      "Held: QC missing in RUN-7",
    );
    expect(resultField()).toBeNull();
    const pageRoutes = utilsMock.getFromOpenElisServer.getMockImplementation();
    utilsMock.getFromOpenElisServer.mockImplementation(
      (endPoint: string, callback: any) =>
        endPoint.startsWith("/rest/qc/results/control-lots")
          ? callback([
              {
                id: "lot-1",
                lotNumber: "CTL-1",
                productName: "Control",
                controlLevel: "NORMAL",
              },
            ])
          : pageRoutes?.(endPoint, callback),
    );
    utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
      (url: string, _body: string, callback: any) =>
        url === "/rest/result-runs/of-analysis/61/controls"
          ? callback({ id: "qc-9", runId: 7, waitingRun: null })
          : callback({}),
    );

    fireEvent.click(screen.getByRole("button", { name: /expand row/i }));
    fireEvent.change(screen.getByTestId("qc-outcome"), {
      target: { value: "PASS" },
    });
    fireEvent.change(screen.getByTestId("qc-lot"), {
      target: { value: "lot-1" },
    });
    fireEvent.change(screen.getByRole("textbox", { name: "Measured value" }), {
      target: { value: "81" },
    });
    fireEvent.change(screen.getByLabelText("Expected value"), {
      target: { value: "80" },
    });
    fireEvent.change(screen.getByLabelText("Uncertainty (±)"), {
      target: { value: "8" },
    });
    fireEvent.click(screen.getByTestId("qc-record"));

    expect(screen.queryByTestId("held-run-7")).not.toBeInTheDocument();
    expect(resultField()).not.toBeNull();
  });

  it("shows a stored result held by its run as held, with its value and no Edit", () => {
    renderPage([
      row({ resultValue: "88", waitingRun: { ...WAITING, held: true } }),
    ]);

    expect(screen.getByTestId("held-run-7")).toHaveTextContent(
      "Held: QC failed in RUN-7",
    );
    expect(screen.getByText("88")).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Edit" }),
    ).not.toBeInTheDocument();
    expect(resultField()).toBeNull();
  });

  it("offers Edit on a stored result its run no longer holds", () => {
    renderPage([row({ resultValue: "88" })]);

    expect(screen.queryByTestId("held-run-7")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Edit" })).toBeInTheDocument();
  });

  it("tags a pending test batched in a workplan run and keeps it open for entry", () => {
    const onOpenRun = renderPage([
      row({
        waitingRun: {
          ...WAITING,
          runId: 9,
          label: "RUN-9",
          source: "WORKPLAN",
        },
      }),
    ]);

    const tag = screen.getByTestId("waiting-run-9");
    expect(tag).toHaveTextContent("Batched in RUN-9");
    expect(tag.parentElement).toHaveTextContent("Workplan batch");
    expect(resultField()).not.toBeNull();

    fireEvent.click(tag);
    expect(onOpenRun).toHaveBeenCalledWith(9);
  });

  it("holds a batched test its run holds, with no entry", () => {
    renderPage([
      row({
        waitingRun: {
          ...WAITING,
          runId: 9,
          label: "RUN-9",
          source: "WORKPLAN",
          held: true,
          heldCause: "QC_MISSING",
        },
      }),
    ]);

    expect(screen.getByTestId("held-run-9")).toHaveTextContent(
      "Held: QC missing in RUN-9",
    );
    expect(resultField()).toBeNull();
  });

  it("batches pending rows of one test picked on the worklist into a run and opens it", () => {
    utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
      (_url: string, _body: string, callback: any) =>
        callback({ id: 30, status: "ACTIVE", runId: 205 }),
    );
    const onOpenRun = renderPage([
      row({ testId: "5" }),
      row({
        id: "r-2",
        analysisId: "62",
        testId: "5",
        accessionNumber: "DEV0126000000000401",
      }),
    ]);

    fireEvent.click(
      screen.getByLabelText("Select DEV0126000000000400 Glucose for a batch"),
    );
    expect(screen.getByTestId("batch-these")).toBeDisabled();
    fireEvent.click(
      screen.getByLabelText("Select DEV0126000000000401 Glucose for a batch"),
    );
    fireEvent.click(screen.getByTestId("batch-these"));

    const batched = utilsMock.postToOpenElisServerJsonResponse.mock.calls.find(
      ([url]: [string]) =>
        url === "/rest/batch-workplans/batches/from-worklist",
    );
    expect(batched).toBeDefined();
    const body = batched[1];
    expect(JSON.parse(body)).toEqual({ analysisIds: ["61", "62"] });
    expect(onOpenRun).toHaveBeenCalledWith(205);
  });

  it("keeps a picked row while another lab number is searched", () => {
    renderPage([row({ testId: "5" })]);

    fireEvent.click(
      screen.getByLabelText("Select DEV0126000000000400 Glucose for a batch"),
    );
    const search = screen.getByRole("searchbox");
    fireEvent.change(search, { target: { value: "DEV0126000000000401" } });
    fireEvent.keyDown(search, { key: "Enter" });

    expect(screen.getByTestId("batch-these-count")).toHaveTextContent(
      "Rows selected for a batch: 1",
    );
  });

  it("offers no pick on a row that is resulted, waiting or batched in a run", () => {
    renderPage([
      row({ testId: "5", resultValue: "88" }),
      row({
        id: "r-2",
        analysisId: "62",
        testId: "5",
        accessionNumber: "DEV0126000000000401",
        waitingRun: WAITING,
      }),
      row({
        id: "r-3",
        analysisId: "63",
        testId: "5",
        accessionNumber: "DEV0126000000000402",
        waitingRun: {
          ...WAITING,
          runId: 9,
          label: "RUN-9",
          source: "WORKPLAN",
        },
      }),
    ]);

    expect(screen.queryByLabelText(/for a batch$/)).not.toBeInTheDocument();
  });

  it("leaves a pending test with no waiting value open for entry", () => {
    renderPage([row()]);

    expect(screen.queryByTestId("waiting-run-7")).not.toBeInTheDocument();
    expect(resultField()).not.toBeNull();
  });

  it("counts open and held runs for the lab unit and opens the one chosen", () => {
    const onOpenRun = renderPage([row()]);

    const chip = screen.getByTestId("runs-chip");
    expect(chip).toHaveTextContent("Runs: 1 open, 0 held");
    const lastRunsCall = utilsMock.getFromOpenElisServer.mock.calls
      .map((call: any[]) => String(call[0]))
      .filter((url: string) => url.startsWith("/rest/result-runs"))
      .pop();
    expect(lastRunsCall).toBe("/rest/result-runs?testSectionId=36");

    fireEvent.click(screen.getByText(/Runs: 1 open/));
    fireEvent.click(screen.getByTestId("open-run-7"));
    expect(onOpenRun).toHaveBeenCalledWith(7);
  });

  it("turns every part of a test whose save was refused for a waiting value into a waiting row", () => {
    renderPage([
      row(),
      row({
        id: "r-2",
        testResultComponentId: "c-2",
        testName: "Glucose part",
      }),
    ]);
    utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
      (_url: string, _body: string, callback: any) =>
        callback({
          status: 409,
          error: "An analyzer value for this test is waiting in RUN-7.",
          waitingRun: WAITING,
        }),
    );

    fireEvent.change(resultField() as HTMLInputElement, {
      target: { value: "5.4" },
    });
    fireEvent.click(screen.getByTestId("save-button"));

    expect(screen.getAllByTestId("waiting-run-7")).toHaveLength(2);
    expect(resultField()).toBeNull();
    expect(notify.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        message: "An analyzer value for this test is waiting in RUN-7.",
      }),
    );
  });

  it("shows a row whose save was refused for a failed control as held, with the server's reason", () => {
    renderPage([row()]);
    utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
      (_url: string, _body: string, callback: any) =>
        callback({
          status: 409,
          error:
            "A control for this test failed in RUN-7, so its results there are held.",
          waitingRun: { ...WAITING, held: true },
        }),
    );

    fireEvent.change(resultField() as HTMLInputElement, {
      target: { value: "5.4" },
    });
    fireEvent.click(screen.getByTestId("save-button"));

    expect(screen.getByTestId("held-run-7")).toHaveTextContent(
      "Held: QC failed in RUN-7",
    );
    expect(resultField()).toBeNull();
    expect(notify.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        message:
          "A control for this test failed in RUN-7, so its results there are held.",
      }),
    );
  });
});
