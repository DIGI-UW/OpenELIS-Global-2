import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";

const mockedDisplayLists = {
  "/rest/displayList/PATIENT_HEALTH_REGIONS": [],
  "/rest/displayList/PATIENT_EDUCATION": [],
  "/rest/displayList/PATIENT_MARITAL_STATUS": [],
  "/rest/displayList/PATIENT_DISEASE_PROGRAMME": [],
};

const testState = vi.hoisted(() => ({
  lastPostRequest: null,
  postResponse: { status: "success" },
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

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: testState.getFromOpenElisServer,
  postToOpenElisServerJsonResponse: testState.postToOpenElisServerJsonResponse,
  resolveApiErrorMessage: vi.fn(() => "Save failed"),
}));

vi.mock("../../layout/Layout", () => ({
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

vi.mock("../../common/CustomNotification", () => ({
  AlertDialog: () => null,
  NotificationKinds: { success: "success", error: "error" },
}));

vi.mock("../AddressSearch", () => ({ default: () => null }));
vi.mock("../photoManagement/uploadPhoto/PatientImageSelector", () => ({
  default: () => <div data-testid="patient-image-selector-mock" />,
}));
vi.mock("../IdentificationDocuments", () => ({
  default: () => <div data-testid="identification-documents-mock" />,
}));
vi.mock("../PatientFormObserver", () => ({ default: () => null }));
vi.mock("../../common/CustomDatePicker", () => ({
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

import CreatePatientForm from "../CreatePatientForm";

const renderForm = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <CreatePatientForm
        showActionsButton={true}
        selectedPatient={{}}
        onClear={() => {}}
      />
    </IntlProvider>,
  );

const flush = () => new Promise((r) => setTimeout(r, 0));

beforeEach(() => {
  testState.postResponse = { status: "success" };
  testState.lastPostRequest = null;
  testState.getFromOpenElisServer.mockClear();
  testState.postToOpenElisServerJsonResponse.mockClear();
});

describe("CreatePatientForm — Enter does not submit, only explicit Save does", () => {
  test("pressing Enter in a filled form does not submit", async () => {
    const user = userEvent.setup();
    renderForm();
    await flush();

    await user.type(document.getElementById("lastName"), "Smith");
    await user.type(document.getElementById("firstName"), "John");
    await user.click(document.getElementById("radio-1"));
    await user.type(screen.getByTestId("dob-input"), "01/15/1990");

    document.getElementById("firstName").focus();
    await user.keyboard("{Enter}");
    await flush();

    expect(
      testState.postToOpenElisServerJsonResponse,
      "Enter must not trigger /rest/PatientManagement",
    ).not.toHaveBeenCalled();

    document.getElementById("lastName").focus();
    await user.keyboard("{Enter}");
    await flush();

    expect(
      testState.postToOpenElisServerJsonResponse,
      "Enter on any input must remain inert",
    ).not.toHaveBeenCalled();
  });

  test("clicking Save submits the form", async () => {
    const user = userEvent.setup();
    renderForm();
    await flush();

    await user.type(document.getElementById("lastName"), "Smith");
    await user.type(document.getElementById("firstName"), "John");
    await user.click(document.getElementById("radio-1"));
    await user.type(screen.getByTestId("dob-input"), "01/15/1990");

    await user.click(document.getElementById("submit"));
    await flush();

    expect(testState.postToOpenElisServerJsonResponse).toHaveBeenCalledTimes(1);
    const body = JSON.parse(testState.lastPostRequest.body);
    expect(body.lastName).toBe("Smith");
    expect(body.firstName).toBe("John");
  });
});
