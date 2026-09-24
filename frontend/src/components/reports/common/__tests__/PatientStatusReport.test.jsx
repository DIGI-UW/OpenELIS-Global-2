import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";

/**
 * The patient report holds the patient the way the results entry page does:
 * choosing one puts the search away and leaves a tag naming them, with a way
 * back to the search and a way to clear. The long result list used to stay on
 * screen under the chosen patient, so nothing said who the report was for.
 */

const { utilsMock } = vi.hoisted(() => ({
  utilsMock: {
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
    postToOpenElisServerFormData: vi.fn(),
    encodeDate: (date) => date,
  },
}));

vi.mock("../../../utils/Utils", () => utilsMock);

vi.mock("../../../layout/Layout", () => ({
  NotificationContext: React.createContext({
    notificationVisible: false,
    setNotificationVisible: vi.fn(),
    addNotification: vi.fn(),
  }),
  ConfigurationContext: React.createContext({
    configurationProperties: { restrictFreeTextRefSiteEntry: "false" },
  }),
}));

const PATIENT = {
  patientPK: "123",
  firstName: "Ada",
  lastName: "Lovelace",
  subjectNumber: "S-1",
};

vi.mock("../../../patient/SearchPatientForm", () => ({
  __esModule: true,
  default: ({ getSelectedPatient }) => (
    <button
      type="button"
      data-testid="mock-patient-search"
      onClick={() => getSelectedPatient(PATIENT)}
    >
      pick a patient
    </button>
  ),
}));

import PatientStatusReport from "../PatientStatusReport";

const renderReport = () => {
  utilsMock.getFromOpenElisServer.mockImplementation((url, callback) =>
    callback([]),
  );
  return render(
    <IntlProvider locale="en" messages={messages}>
      <PatientStatusReport
        report="patientCILNSP_vreduit"
        id="openreports.patientTestStatus"
      />
    </IntlProvider>,
  );
};

describe("Patient report patient selection", () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  it("puts the search away behind a tag naming the patient, and brings it back", async () => {
    const user = userEvent.setup();
    renderReport();

    expect(screen.getByTestId("selected-patient")).toHaveTextContent("None");
    expect(screen.getByTestId("mock-patient-search")).toBeInTheDocument();

    await user.click(screen.getByTestId("mock-patient-search"));

    expect(screen.getByTestId("selected-patient")).toHaveTextContent(
      "Ada Lovelace (S-1)",
    );
    expect(screen.queryByTestId("mock-patient-search")).not.toBeInTheDocument();

    await user.click(screen.getByTestId("select-another-patient"));

    expect(screen.getByTestId("mock-patient-search")).toBeInTheDocument();
    expect(screen.getByTestId("selected-patient")).toHaveTextContent(
      "Ada Lovelace",
    );
  });

  it("clears the patient and shows the search again", async () => {
    const user = userEvent.setup();
    renderReport();

    await user.click(screen.getByTestId("mock-patient-search"));
    await user.click(screen.getByTestId("clear-patient"));

    expect(screen.getByTestId("selected-patient")).toHaveTextContent("None");
    expect(screen.getByTestId("mock-patient-search")).toBeInTheDocument();
    expect(screen.queryByTestId("clear-patient")).not.toBeInTheDocument();
  });
});
