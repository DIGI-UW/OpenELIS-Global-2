import React from "react";
import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
} from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";

/**
 * The worklist is fetched one server page at a time, and each fetch takes
 * long enough on a busy lab unit that an unchanged table reads as "nothing
 * happened". The page shows a loader from the moment a worklist or a page of
 * it is requested until the rows have been applied.
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

const row = (analysisId: string) => ({
  id: `r${analysisId}`,
  analysisId,
  testResultComponentId: `c${analysisId}`,
  testName: "Glucose",
  resultType: "N",
  resultValue: "",
  analysisStatusId: "4",
});

const PAGE_ONE = {
  testResult: [row("60"), row("61")],
  paging: { currentPage: "1", totalPages: "2" },
};
const PAGE_TWO = {
  testResult: [row("70")],
  paging: { currentPage: "2", totalPages: "2" },
};

/** Holds every worklist callback until the test releases it. */
const pendingWorklists: Array<(body: unknown) => void> = [];

const wire = () => {
  utilsMock.getFromOpenElisServer.mockImplementation(
    (endPoint: string, callback: any) => {
      if (endPoint === "/rest/results-entry/lab-units")
        return callback(LAB_UNITS);
      if (endPoint === "/rest/analysis-status-types") return callback(STATUSES);
      if (endPoint.startsWith("/rest/LogbookResults")) {
        pendingWorklists.push(callback);
        return;
      }
      return callback([]);
    },
  );
};

const loader = () => document.querySelector(".cds--loading-overlay");

const releaseWorklist = (body: unknown) => {
  const callback = pendingWorklists.shift();
  expect(callback).toBeDefined();
  act(() => callback!(body));
};

describe("Unified Results loader", () => {
  beforeEach(() => {
    cleanup();
    pendingWorklists.length = 0;
    utilsMock.getFromOpenElisServer.mockReset();
    window.localStorage.clear();
    wire();
    render(
      <IntlProvider locale="en" messages={messages}>
        <UnifiedResults />
      </IntlProvider>,
    );
  });

  it("shows a loader while the worklist is being fetched and hides it once the rows arrive", () => {
    expect(loader()).toBeNull();

    fireEvent.change(screen.getByLabelText(/Lab Unit/i), {
      target: { value: "56" },
    });

    expect(loader()).not.toBeNull();
    expect(screen.getByText("Loading results...")).toBeInTheDocument();

    releaseWorklist(PAGE_ONE);

    expect(loader()).toBeNull();
    expect(document.querySelectorAll("tbody tr").length).toBe(2);
  });

  it("shows the loader again while another server page is being fetched", () => {
    fireEvent.change(screen.getByLabelText(/Lab Unit/i), {
      target: { value: "56" },
    });
    releaseWorklist(PAGE_ONE);
    expect(loader()).toBeNull();

    fireEvent.click(document.querySelector("#loadnextresults")!);

    expect(loader()).not.toBeNull();
    expect(
      utilsMock.getFromOpenElisServer.mock.calls.some((c: any[]) =>
        String(c[0]).endsWith("&page=2"),
      ),
    ).toBe(true);

    releaseWorklist(PAGE_TWO);

    expect(loader()).toBeNull();
    expect(document.querySelectorAll("tbody tr").length).toBe(1);
  });

  it("hides the loader when the fetch fails, so the failure notice is not covered", () => {
    fireEvent.change(screen.getByLabelText(/Lab Unit/i), {
      target: { value: "56" },
    });
    expect(loader()).not.toBeNull();

    releaseWorklist({ status: 500, error: "Internal Server Error" });

    expect(loader()).toBeNull();
    expect(
      document.querySelector(".cds--actionable-notification"),
    ).not.toBeNull();
  });
});
