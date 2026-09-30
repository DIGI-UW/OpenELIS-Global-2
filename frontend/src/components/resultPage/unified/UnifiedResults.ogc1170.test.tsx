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
 * OGC-1170 (defect B) — a worklist that failed to load must not look like a
 * worklist with nothing in it.
 *
 * When the fetch failed the page rendered the column header, the chip `All (0)`
 * and `0–0 of 0 items` — and nothing else. No alert, no notification, no
 * occurrence of error/failed/unable anywhere in the page text. A technician was
 * shown an empty queue, pixel-identical to "no pending work", while the work
 * sat there undone.
 *
 * getFromOpenElisServer does not check response.ok: it parses whatever JSON came
 * back and hands it to the callback, so a 500's error body arrives looking like
 * a response. The page has to tell the two apart itself.
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

vi.mock("../../esignature/ESignatureButton", () => ({
  __esModule: true,
  default: ({ children }: any) => <button type="button">{children}</button>,
  SignatureMeaning: { AUTHORED: "AUTHORED", MODIFIED: "MODIFIED" },
}));

vi.mock("../../common/PageBreadCrumb", () => ({ default: () => <div /> }));

import UnifiedResults from "./UnifiedResults";

const LAB_UNITS = [{ id: "56", value: "Biochemistry", domain: "CLINICAL" }];
const STATUSES = [{ id: "4", value: "Not started" }];

/** The body a 500 actually delivers through this callback. */
const SERVER_ERROR = {
  timestamp: 1786600543858,
  status: 500,
  error: "Internal Server Error",
};

const wire = (worklistResponse: unknown) => {
  utilsMock.getFromOpenElisServer.mockImplementation(
    (endPoint: string, callback: any) => {
      if (endPoint === "/rest/results-entry/lab-units")
        return callback(LAB_UNITS);
      if (endPoint === "/rest/analysis-status-types") return callback(STATUSES);
      if (endPoint.startsWith("/rest/LogbookResults"))
        return callback(worklistResponse);
      return callback([]);
    },
  );
};

const renderPage = (worklistResponse: unknown) => {
  wire(worklistResponse);
  render(
    <IntlProvider locale="en" messages={messages}>
      <UnifiedResults />
    </IntlProvider>,
  );
  fireEvent.change(screen.getByLabelText(/Lab Unit/i), {
    target: { value: "56" },
  });
};

const notification = () =>
  document.querySelector(".cds--actionable-notification") as HTMLElement | null;

const worklistLoads = () =>
  utilsMock.getFromOpenElisServer.mock.calls.filter((c: any[]) =>
    String(c[0]).startsWith("/rest/LogbookResults"),
  ).length;

describe("OGC-1170 — a worklist that failed to load", () => {
  beforeEach(() => {
    cleanup();
    utilsMock.getFromOpenElisServer.mockReset();
    utilsMock.postToOpenElisServerJsonResponse.mockReset();
    window.localStorage.clear();
  });

  it("says so, instead of rendering a silent empty table", () => {
    renderPage(SERVER_ERROR);

    const notice = notification();
    expect(notice).not.toBeNull();
    expect(notice).toHaveTextContent(/could not be loaded/i);
    expect(notice).toHaveTextContent(/not an empty worklist/i);
  });

  it("offers a retry that re-requests the worklist", () => {
    renderPage(SERVER_ERROR);
    const before = worklistLoads();

    fireEvent.click(
      within(notification()!).getByRole("button", { name: /Try again/i }),
    );

    expect(worklistLoads()).toBeGreaterThan(before);
  });

  it("says the same when nothing came back at all", () => {
    // getFromOpenElisServer calls back with undefined for a network failure or
    // a non-JSON response.
    renderPage(undefined);

    expect(notification()).not.toBeNull();
  });

  /**
   * The distinction the whole ticket rests on: the units that genuinely hold no
   * work answer 200 with an empty list, and those must go on looking empty.
   */
  it("stays quiet for a worklist that is genuinely empty", () => {
    renderPage({ testResult: [] });

    expect(notification()).toBeNull();
    expect(document.querySelectorAll("tbody tr").length).toBe(0);
  });

  it("clears the failure once a later load succeeds", () => {
    renderPage(SERVER_ERROR);
    expect(notification()).not.toBeNull();

    wire({
      testResult: [
        {
          id: "r1",
          analysisId: "60",
          testResultComponentId: "c1",
          testName: "Glucose",
          resultType: "N",
          resultValue: "",
          analysisStatusId: "4",
        },
      ],
    });
    fireEvent.click(
      within(notification()!).getByRole("button", { name: /Try again/i }),
    );

    expect(notification()).toBeNull();
    expect(document.querySelectorAll("tbody tr").length).toBe(1);
  });
});
