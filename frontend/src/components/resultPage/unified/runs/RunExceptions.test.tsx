import React from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";
import { NotificationContext } from "../../../layout/Layout";

const { utilsMock } = vi.hoisted(() => ({
  utilsMock: {
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
  },
}));

vi.mock("../../../utils/Utils", () => utilsMock);

import RunExceptions, { RunExceptionRecord, mapNowUrl } from "./RunExceptions";

const UNMATCHED: RunExceptionRecord = {
  key: "UNMATCHED|24-01214",
  kind: "UNMATCHED",
  accessionNumber: "24-01214",
  testId: "11",
  testName: "Glucose",
  rowIds: ["501", "502"],
};

const UNORDERED: RunExceptionRecord = {
  key: "UNORDERED|DEV01|11",
  kind: "UNORDERED",
  accessionNumber: "DEV01",
  testId: "11",
  testName: "Glucose",
  rowIds: ["503"],
};

const UNMAPPED: RunExceptionRecord = {
  key: "UNMAPPED|unknown_analyzer_test|TBIL-2|7.1",
  kind: "UNMAPPED",
  accessionNumber: "DEV02",
  code: "TBIL-2",
  value: "7.1",
  issue: "unknown_analyzer_test",
  rowIds: ["504"],
  mapping: { analyzerId: "9", focusTest: "TBIL-2", focusValue: "7.1" },
};

const notifications = {
  addNotification: vi.fn(),
  setNotificationVisible: vi.fn(),
};

const renderBlock = (exceptions: RunExceptionRecord[], onResolved = vi.fn()) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <NotificationContext.Provider value={notifications}>
        <MemoryRouter>
          <RunExceptions
            runId={7}
            exceptions={exceptions}
            onResolved={onResolved}
          />
        </MemoryRouter>
      </NotificationContext.Provider>
    </IntlProvider>,
  );

describe("RunExceptions (OGC-1200 FR-E4)", () => {
  beforeEach(() => {
    vi.useRealTimers();
    utilsMock.getFromOpenElisServer.mockReset();
    utilsMock.postToOpenElisServerJsonResponse.mockReset();
    notifications.addNotification.mockReset();
  });

  it("is hidden when the run has no exceptions", () => {
    renderBlock([]);
    expect(screen.queryByTestId("run-exceptions")).not.toBeInTheDocument();
  });

  it("lists each kind with what it is about and the action that resolves it", () => {
    renderBlock([UNMATCHED, UNORDERED, UNMAPPED]);

    expect(screen.getByText("Exceptions (3)")).toBeInTheDocument();
    expect(screen.getByTestId("run-exception-detail-501")).toHaveTextContent(
      "Lab number 24-01214 names no order (2 values)",
    );
    expect(screen.getByTestId("run-exception-detail-503")).toHaveTextContent(
      "Glucose is not ordered on lab number DEV01 (1 value)",
    );
    expect(screen.getByTestId("run-exception-detail-504")).toHaveTextContent(
      "Analyzer code TBIL-2 has no mapped test (1 value)",
    );
    expect(screen.getByTestId("run-exception-match-501")).toHaveTextContent(
      "Find lab number",
    );
    expect(screen.getByTestId("run-exception-match-503")).toBeInTheDocument();
    expect(
      screen.queryByTestId("run-exception-match-504"),
    ).not.toBeInTheDocument();
    expect(screen.getByTestId("run-exception-map-504")).toHaveAttribute(
      "href",
      "/analyzers/9/mapping?returnTo=%2FResults%3Frun%3D7&focusTest=TBIL-2&focusValue=7.1",
    );
  });

  it("opens the analyzer's mapping on the code and comes back to the run", () => {
    expect(mapNowUrl(7, { analyzerId: "9", focusTest: "A B" })).toBe(
      "/analyzers/9/mapping?returnTo=%2FResults%3Frun%3D7&focusTest=A+B",
    );
  });

  it("finds the order by the start of its lab number and matches the values to it with a reason", async () => {
    vi.useFakeTimers();
    const onResolved = vi.fn();
    utilsMock.getFromOpenElisServer.mockImplementation(
      (url: string, callback: (body: unknown) => void) =>
        callback([
          {
            accessionNumber: "24-01215",
            patientName: "Doe, Jane",
            receivedDate: "11/24/2026",
            testOrdered: true,
          },
        ]),
    );
    utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
      (url: string, body: string, callback: (body: unknown) => void) =>
        callback({ exceptions: [] }),
    );
    renderBlock([UNMATCHED], onResolved);

    fireEvent.click(screen.getByTestId("run-exception-match-501"));
    const input = screen.getByRole("combobox");
    fireEvent.change(input, { target: { value: "24-0" } });
    await act(async () => {
      vi.advanceTimersByTime(350);
    });
    expect(utilsMock.getFromOpenElisServer.mock.calls[0][0]).toBe(
      "/rest/result-runs/7/exceptions/orders?q=24-0&testId=11",
    );
    vi.useRealTimers();
    fireEvent.click(
      await screen.findByText(
        "24-01215 · Doe, Jane · 11/24/2026 · test ordered",
      ),
    );
    fireEvent.change(screen.getByLabelText(/Reason/), {
      target: { value: "Label misread" },
    });
    fireEvent.click(screen.getByTestId("run-exception-confirm-501"));

    const [url, body] =
      utilsMock.postToOpenElisServerJsonResponse.mock.calls[0];
    expect(url).toBe("/rest/result-runs/7/exceptions/match");
    expect(JSON.parse(body)).toEqual({
      rowIds: ["501", "502"],
      key: "UNMATCHED|24-01214",
      reason: "Label misread",
      accessionNumber: "24-01215",
    });
    expect(onResolved).toHaveBeenCalled();
    expect(notifications.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        message: "Lab number 24-01215 now holds 2 values from this run.",
      }),
    );
  });

  it("does not match without an order or ignore without a reason", () => {
    renderBlock([UNMATCHED]);

    fireEvent.click(screen.getByTestId("run-exception-match-501"));
    fireEvent.change(screen.getByLabelText(/Reason/), {
      target: { value: "Label misread" },
    });
    fireEvent.click(screen.getByTestId("run-exception-confirm-501"));
    expect(screen.getByText("Choose the order.")).toBeInTheDocument();

    fireEvent.click(screen.getByText("Cancel"));
    fireEvent.click(screen.getByTestId("run-exception-ignore-open-501"));
    fireEvent.click(screen.getByTestId("run-exception-confirm-501"));
    expect(screen.getByText("A reason is required.")).toBeInTheDocument();
    expect(utilsMock.postToOpenElisServerJsonResponse).not.toHaveBeenCalled();
  });

  it("ignores only the values of the record, with the reason, and shows why the server refused", () => {
    const onResolved = vi.fn();
    utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
      (url: string, body: string, callback: (body: unknown) => void) =>
        callback({
          message:
            "This record has changed since the run was opened. Reload the run to see what is left.",
        }),
    );
    renderBlock([UNMATCHED, UNORDERED], onResolved);

    fireEvent.click(screen.getByTestId("run-exception-ignore-open-503"));
    fireEvent.change(screen.getByLabelText(/Reason/), {
      target: { value: "Calibration tube" },
    });
    fireEvent.click(screen.getByTestId("run-exception-confirm-503"));

    const [url, body] =
      utilsMock.postToOpenElisServerJsonResponse.mock.calls[0];
    expect(url).toBe("/rest/result-runs/7/exceptions/ignore");
    expect(JSON.parse(body)).toEqual({
      rowIds: ["503"],
      key: "UNORDERED|DEV01|11",
      reason: "Calibration tube",
      accessionNumber: null,
    });
    expect(
      screen.getByText(
        "This record has changed since the run was opened. Reload the run to see what is left.",
      ),
    ).toBeInTheDocument();
    expect(onResolved).not.toHaveBeenCalled();
  });

  it("reloads the run and says why when the record changed under the page", () => {
    const onResolved = vi.fn();
    utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
      (url: string, body: string, callback: (body: unknown) => void) =>
        callback({
          message:
            "This record has changed since the run was opened. Reload the run to see what is left.",
          stale: true,
        }),
    );
    renderBlock([UNMATCHED], onResolved);

    fireEvent.click(screen.getByTestId("run-exception-ignore-open-501"));
    fireEvent.change(screen.getByLabelText(/Reason/), {
      target: { value: "Not ours" },
    });
    fireEvent.click(screen.getByTestId("run-exception-confirm-501"));

    expect(onResolved).toHaveBeenCalled();
    expect(notifications.addNotification).toHaveBeenCalledWith(
      expect.objectContaining({
        kind: "error",
        message:
          "This record has changed since the run was opened. Reload the run to see what is left.",
      }),
    );
    expect(
      screen.queryByTestId("run-exception-confirm-501"),
    ).not.toBeInTheDocument();
  });

  it("shows who held a record and why", () => {
    renderBlock([
      {
        ...UNMATCHED,
        hold: {
          reason: "Waiting for reception",
          heldBy: "Open ELIS",
          heldAt: "2031-03-06T10:00:00Z",
        },
      },
    ]);

    expect(screen.getByTestId("run-exception-hold-501")).toHaveTextContent(
      /Held by Open ELIS, .*: Waiting for reception/,
    );
  });
});
