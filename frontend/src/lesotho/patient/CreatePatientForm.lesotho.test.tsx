// LESOTHO: the patient form sends the passport number with the patient. It
// holds only details that do not change; clinical information belongs to each
// order (see lesotho/clinicalInformation and Add Order).
import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { beforeEach, describe, expect, test, vi } from "vitest";
import messages from "../../languages/en.json";

const mockedDisplayLists = {
  "/rest/displayList/PATIENT_HEALTH_REGIONS": [],
  "/rest/displayList/PATIENT_EDUCATION": [],
  "/rest/displayList/PATIENT_MARITAL_STATUS": [],
  "/rest/displayList/PATIENT_DISEASE_PROGRAMME": [],
};

const testState = vi.hoisted(() => ({
  postResponse: { status: "success" },
  lastPostRequest: null,
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
}));

testState.getFromOpenElisServer.mockImplementation((url, callback) => {
  if (typeof callback !== "function") return;
  if (url.startsWith("/rest/patient-photos/")) return;
  if (url.startsWith("/rest/PhoneNumberValidationProvider")) {
    callback({ status: true, body: "" });
    return;
  }
  if (url.startsWith("/rest/subjectNumberValidationProvider")) {
    callback({ status: true });
    return;
  }
  callback(mockedDisplayLists[url] ?? []);
});

testState.postToOpenElisServerJsonResponse.mockImplementation(
  (url, body, callback) => {
    testState.lastPostRequest = { url, body };
    callback(testState.postResponse);
  },
);

vi.mock("../../components/utils/Utils", () => ({
  getFromOpenElisServer: testState.getFromOpenElisServer,
  postToOpenElisServerJsonResponse: testState.postToOpenElisServerJsonResponse,
  resolveApiErrorMessage: vi.fn(() => "Save failed"),
}));

vi.mock("../../components/layout/Layout", () => ({
  NotificationContext: React.createContext({
    notificationVisible: false,
    setNotificationVisible: () => {},
    addNotification: () => {},
  }),
  ConfigurationContext: React.createContext({
    configurationProperties: {
      USE_NEW_ADDRESS_HIERARCHY: "false",
      PATIENT_GPS_CAPTURE_ENABLED: "false",
      DEFAULT_NATIONALITY: "",
      DEFAULT_DATE_LOCALE: "en-US",
      FIRST_NAME_REGEX: "^[A-Za-z\\s'-]+$",
      LAST_NAME_REGEX: "^[A-Za-z\\s'-]+$",
      PATIENT_NATIONAL_ID_REQUIRED: "false",
      PATIENT_ALIAS_ENABLED: "false",
      PHONE_FORMAT: "+261-xx-xxx-xx-xx",
    },
  }),
}));

vi.mock("../../components/common/CustomNotification", () => ({
  AlertDialog: () => null,
  NotificationKinds: { success: "success", error: "error" },
}));

vi.mock("../../components/patient/AddressSearch", () => ({
  default: () => null,
}));
vi.mock(
  "../../components/patient/photoManagement/uploadPhoto/PatientImageSelector",
  () => ({
    default: () => <div data-testid="patient-image-selector-mock" />,
  }),
);
vi.mock("../../components/patient/IdentificationDocuments", () => ({
  default: () => <div data-testid="identification-documents-mock" />,
}));
vi.mock("../../components/patient/PatientFormObserver", () => ({
  default: () => null,
}));
vi.mock("../../components/common/CustomDatePicker", () => ({
  default: ({ value, onChange, id }) => (
    <input
      id={id || "date-picker-default-id"}
      data-testid="dob-input"
      value={value || ""}
      onChange={(e) => onChange(e.target.value)}
    />
  ),
}));

vi.mock("react-router-dom", () => ({
  useHistory: () => ({ push: vi.fn() }),
}));

import CreatePatientForm from "../../components/patient/CreatePatientForm";

const flush = () => new Promise((r) => setTimeout(r, 0));

beforeEach(() => {
  testState.postResponse = { status: "success", patientId: "42" };
  testState.lastPostRequest = null;
  testState.postToOpenElisServerJsonResponse.mockClear();
});

describe("CreatePatientForm — Lesotho fields", () => {
  test("saves the patient with the passport number and nothing else", async () => {
    const user = userEvent.setup();
    render(
      <IntlProvider locale="en" messages={messages}>
        <CreatePatientForm
          showActionsButton={true}
          selectedPatient={{}}
          onClear={() => {}}
        />
      </IntlProvider>,
    );
    await flush();

    await user.type(document.getElementById("lastName"), "Mokoena");
    await user.type(document.getElementById("firstName"), "Palesa");
    await user.click(document.getElementById("radio-2"));
    await user.type(document.getElementById("years"), "30");
    await user.type(screen.getByLabelText("Passport Number"), "RA1234567");

    await user.click(document.getElementById("submit"));
    await flush();

    const calls = testState.postToOpenElisServerJsonResponse.mock.calls;
    expect(calls, "only the patient is saved here").toHaveLength(1);
    const [patientUrl, patientBody] = calls[0];
    expect(patientUrl).toBe("/rest/PatientManagement");
    expect(JSON.parse(patientBody).passportNumber).toBe("RA1234567");
  });

  test("has no clinical information section: it moved to Add Order", async () => {
    render(
      <IntlProvider locale="en" messages={messages}>
        <CreatePatientForm
          showActionsButton={true}
          selectedPatient={{}}
          onClear={() => {}}
        />
      </IntlProvider>,
    );
    await flush();
    expect(screen.queryByText("Clinical Information")).not.toBeInTheDocument();
  });
});
