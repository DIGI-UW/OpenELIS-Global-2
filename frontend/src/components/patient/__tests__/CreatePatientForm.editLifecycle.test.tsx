import React from "react";
import { render, screen, act } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";

/**
 * Edit-mode lifecycle regressions for CreatePatientForm.
 *
 * Copilot review round 3 noted that a successful save on an existing
 * patient pushes the same URL the user is already on, so React Router does
 * not remount and the local `isEditing` toggle never resets — leaving the
 * form editable after save. This file locks the contract: a successful
 * save flips the form back to read-only without depending on remount.
 */

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

const renderEdit = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <CreatePatientForm
        showActionsButton={true}
        selectedPatient={{
          patientPK: "EXISTING-1",
          firstName: "Old",
          lastName: "Patient",
          gender: "M",
          birthDateForDisplay: "01/15/1990",
          nationalId: "EX-001",
        }}
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

describe("CreatePatientForm — successful save exits edit mode", () => {
  test("Save button disappears after a successful save", async () => {
    const user = userEvent.setup();
    renderEdit();
    await flush();

    // Initial state: existing patient is read-only, no Save button.
    expect(
      document.getElementById("submit"),
      "existing patient should mount in read-only mode with no Save button",
    ).toBeNull();

    // Toggle into edit mode.
    const editToggle = document.getElementById("patient-edit-toggle");
    expect(editToggle, "edit toggle must be in the DOM").not.toBeNull();
    await user.click(editToggle);

    // Save button is now visible.
    const saveBtn = document.getElementById("submit");
    expect(
      saveBtn,
      "Save button should appear after edit toggle",
    ).not.toBeNull();

    // Edit a field.
    const firstNameInput = document.getElementById("firstName");
    await user.clear(firstNameInput);
    await user.type(firstNameInput, "UpdatedFirst");

    // Click Save.
    await user.click(saveBtn);
    await flush();

    expect(
      testState.postToOpenElisServerJsonResponse,
      "save click must POST to /rest/PatientManagement",
    ).toHaveBeenCalledTimes(1);

    // After successful save, the form re-locks: Save button is gone again.
    expect(
      document.getElementById("submit"),
      "Save button must disappear after successful save — same-URL " +
        "navigation does not remount, so isEditing must be reset explicitly",
    ).toBeNull();
  });

  test("Save failure keeps the form editable for retry", async () => {
    const user = userEvent.setup();
    testState.postResponse = { statusCode: 400, error: "validation failed" };

    renderEdit();
    await flush();

    await user.click(document.getElementById("patient-edit-toggle"));
    const saveBtn = document.getElementById("submit");

    const firstNameInput = document.getElementById("firstName");
    await user.clear(firstNameInput);
    await user.type(firstNameInput, "UpdatedFirst");

    await user.click(saveBtn);
    await flush();

    expect(testState.postToOpenElisServerJsonResponse).toHaveBeenCalledTimes(1);

    // On failure the user must still be able to fix and re-save.
    expect(
      document.getElementById("submit"),
      "Save button must still be visible after a failed save",
    ).not.toBeNull();
    expect(document.getElementById("firstName")).toHaveValue("UpdatedFirst");
  });
});

describe("CreatePatientForm — age inputs are Formik fields", () => {
  test("typing years derives birthDateForDisplay and reaches the server", async () => {
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

    await user.type(document.getElementById("lastName"), "Smith");
    await user.type(document.getElementById("firstName"), "John");
    await user.click(document.getElementById("radio-1"));
    // Typing in the years field should populate Formik's birthDateForDisplay.
    await user.type(document.getElementById("years"), "30");

    await user.click(document.getElementById("submit"));
    await flush();

    expect(testState.postToOpenElisServerJsonResponse).toHaveBeenCalledTimes(1);
    const body = JSON.parse(testState.lastPostRequest.body);
    expect(
      body.birthDateForDisplay,
      "typing years must derive a DOB into Formik state " +
        "(form-strip drops years/months/days from the wire payload, " +
        "but birthDateForDisplay must be present)",
    ).toMatch(/^\d{2}\/\d{2}\/\d{4}$/);
    expect(
      body.years,
      "years/months/days are display-only and must not reach the server",
    ).toBeUndefined();
    expect(body.months).toBeUndefined();
    expect(body.days).toBeUndefined();
  });
});
