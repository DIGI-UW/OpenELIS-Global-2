import React from "react";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";

/**
 * Search by patient on the unified worklist: a button beside the Test date
 * opens the same patient panel as the audit trail page, selecting a patient
 * loads that patient's results (patientPK only — the results endpoint ignores
 * the patient when a lab unit or date is sent alongside), and clearing the
 * patient returns to the filter worklist.
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

vi.mock("../../layout/contexts", () => ({
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

const PATIENT = {
  patientPK: "123",
  firstName: "Ada",
  lastName: "Lovelace",
  subjectNumber: "S-1",
};

vi.mock("../../patient/SearchPatientForm", () => ({
  __esModule: true,
  default: ({ getSelectedPatient }: any) => (
    <button
      type="button"
      data-testid="mock-pick-patient"
      onClick={() => getSelectedPatient(PATIENT)}
    >
      pick
    </button>
  ),
}));

import UnifiedResults from "./UnifiedResults";

const LAB_UNITS = [{ id: "56", value: "Biochemistry", domain: "CLINICAL" }];
const STATUSES = [{ id: "4", value: "Not started" }];

const wire = () => {
  utilsMock.getFromOpenElisServer.mockImplementation(
    (endPoint: string, callback: any) => {
      if (endPoint === "/rest/results-entry/lab-units")
        return callback(LAB_UNITS);
      if (endPoint === "/rest/analysis-status-types") return callback(STATUSES);
      if (endPoint.startsWith("/rest/LogbookResults"))
        return callback({ testResult: [] });
      if (endPoint.startsWith("/rest/patient-details?patientID=123"))
        return callback(PATIENT);
      return callback([]);
    },
  );
};

const renderPage = () => {
  wire();
  render(
    <IntlProvider locale="en" messages={messages}>
      <UnifiedResults />
    </IntlProvider>,
  );
};

const chooseLabUnit = () =>
  fireEvent.change(screen.getByLabelText(/Lab Unit/i), {
    target: { value: "56" },
  });

const worklistCalls = () =>
  utilsMock.getFromOpenElisServer.mock.calls
    .map((c: any[]) => String(c[0]))
    .filter((url) => url.startsWith("/rest/LogbookResults"));

const lastWorklistParams = () =>
  new URLSearchParams(worklistCalls().slice(-1)[0].split("?")[1]);

const searchByPatient = () => screen.getByTestId("search-by-patient");
const loadResults = () =>
  screen.getByRole("button", { name: /^Load results$/i });

describe("Search by patient on the unified worklist", () => {
  beforeEach(() => {
    cleanup();
    utilsMock.getFromOpenElisServer.mockReset();
    utilsMock.postToOpenElisServerJsonResponse.mockReset();
    window.localStorage.clear();
    window.history.replaceState(null, "", "/Results");
  });

  it("offers the button beside Test date, before Load results, and opens the patient panel on demand", () => {
    renderPage();

    expect(screen.queryByTestId("patient-search-panel")).toBeNull();
    const button = searchByPatient();
    expect(button).toHaveTextContent(/Search by patient/i);
    const date = screen.getByLabelText(/Test date/i);
    expect(
      date.compareDocumentPosition(button) & Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy();
    expect(
      button.compareDocumentPosition(loadResults()) &
        Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy();

    fireEvent.click(button);

    expect(screen.getByTestId("patient-search-panel")).toBeVisible();
    expect(screen.getByTestId("selected-patient")).toHaveTextContent(/None/);
    expect(screen.getByTestId("mock-pick-patient")).toBeVisible();
  });

  it("loads the selected patient's results with only patientPK and keeps them on Load results", () => {
    renderPage();
    chooseLabUnit();
    expect(lastWorklistParams().get("testSectionId")).toBe("56");

    fireEvent.click(searchByPatient());
    fireEvent.click(screen.getByTestId("mock-pick-patient"));

    let params = lastWorklistParams();
    expect(params.get("patientPK")).toBe("123");
    expect(params.has("testSectionId")).toBe(false);
    expect(params.has("collectionDate")).toBe(false);
    expect(params.get("finished")).toBe("false");
    expect(screen.getByTestId("selected-patient")).toHaveTextContent(
      "Ada Lovelace (S-1)",
    );
    expect(screen.queryByTestId("mock-pick-patient")).toBeNull();
    expect(new URLSearchParams(window.location.search).get("patientId")).toBe(
      "123",
    );
    expect(screen.getByLabelText(/Lab Unit/i)).toBeDisabled();
    expect(screen.getByLabelText(/Test date/i)).toBeDisabled();

    const before = worklistCalls().length;
    fireEvent.click(loadResults());
    expect(worklistCalls().length).toBe(before + 1);
    params = lastWorklistParams();
    expect(params.get("patientPK")).toBe("123");
    expect(params.has("testSectionId")).toBe(false);
  });

  it("clearing the patient returns to the lab-unit worklist", () => {
    renderPage();
    chooseLabUnit();
    fireEvent.click(searchByPatient());
    fireEvent.click(screen.getByTestId("mock-pick-patient"));

    fireEvent.click(screen.getByTestId("clear-patient"));

    const params = lastWorklistParams();
    expect(params.has("patientPK")).toBe(false);
    expect(params.get("testSectionId")).toBe("56");
    expect(screen.queryByTestId("patient-search-panel")).toBeNull();
    expect(new URLSearchParams(window.location.search).has("patientId")).toBe(
      false,
    );
    expect(screen.getByLabelText(/Lab Unit/i)).toBeEnabled();
  });

  it("reopening the search drops the patientId from the URL so the form does not reselect it", () => {
    renderPage();
    fireEvent.click(searchByPatient());
    fireEvent.click(screen.getByTestId("mock-pick-patient"));
    expect(window.location.search).toContain("patientId=123");

    fireEvent.click(screen.getByTestId("select-another-patient"));

    expect(window.location.search).not.toContain("patientId");
    expect(screen.getByTestId("mock-pick-patient")).toBeVisible();
  });

  it("restores a patient search from the URL", () => {
    window.history.replaceState(null, "", "/Results?patientId=123");

    renderPage();

    expect(lastWorklistParams().get("patientPK")).toBe("123");
    expect(screen.getByTestId("selected-patient")).toHaveTextContent(
      "Ada Lovelace (S-1)",
    );
  });
});
