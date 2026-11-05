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
});
