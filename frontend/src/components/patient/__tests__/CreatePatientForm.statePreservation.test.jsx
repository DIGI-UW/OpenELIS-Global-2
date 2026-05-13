import React from "react";
import { render, screen, act } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";

/**
 * State-preservation regressions for the patient create/edit form.
 *
 * UAT (May 2026) reproduction: "I filled the first part of the form and
 * somehow lost the fields during the rest of it." The form historically
 * combined `<Formik enableReinitialize>` with a local `patientDetails`
 * React state that was mutated from many paths (DOB handlers, async photo
 * fetch, address-hierarchy defaults). Any such mutation reseeded Formik
 * and clobbered user input typed since the last seed.
 *
 * These three tests describe the user's task as a user sees it:
 *  1. Fill the new-patient form, save, see the right data submitted.
 *  2. Save fails (server returns 400): the user's typed fields are still
 *     there so they can fix and retry.
 *  3. Editing an existing patient: a field the user typed survives the
 *     async photo fetch completing in the background.
 *
 * Children that aren't part of state behavior (PatientImageSelector,
 * IdentificationDocuments, AddressSearch, PatientFormObserver) are mocked
 * to inert shells. CustomDatePicker is mocked to a real-shape shim that
 * fires `onChange(value)` like the production component, so the DOB-driven
 * state clobber path is reachable from tests. Only the network and shell
 * contexts are mocked — the form, Formik, and the validation schema are
 * all real.
 */

const mockedDisplayLists = {
  "/rest/displayList/PATIENT_HEALTH_REGIONS": [],
  "/rest/displayList/PATIENT_EDUCATION": [],
  "/rest/displayList/PATIENT_MARITAL_STATUS": [],
  "/rest/displayList/PATIENT_DISEASE_PROGRAMME": [],
};

// vi.mock factories run before module imports, before normal `const`
// initializers. Use vi.hoisted to share state (deferred callbacks, captured
// request, response stub) between the test body and the mock factory.
const testState = vi.hoisted(() => ({
  pendingPhotoCallbacks: [],
  lastPostRequest: null,
  postResponse: { status: "success" },
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
}));

testState.getFromOpenElisServer.mockImplementation((url, callback) => {
  if (typeof callback !== "function") return;
  if (url.startsWith("/rest/patient-photos/")) {
    testState.pendingPhotoCallbacks.push(() =>
      callback({ data: "base64-photo-bytes" }),
    );
    return;
  }
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
      // Permissive name regex — these tests only assert state preservation,
      // not the regex gate (covered separately in ogc671 tests).
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

vi.mock("../AddressSearch", () => ({
  default: () => null,
}));

// The photo upload widget itself isn't exercised — the photo fetch callback
// (network mock above) is the clobber trigger we care about.
vi.mock("../photoManagement/uploadPhoto/PatientImageSelector", () => ({
  default: () => <div data-testid="patient-image-selector-mock" />,
}));

vi.mock("../IdentificationDocuments", () => ({
  default: () => <div data-testid="identification-documents-mock" />,
}));

vi.mock("../PatientFormObserver", () => ({
  default: () => null,
}));

// Real-shape CustomDatePicker shim: production component calls onChange(value)
// (a string) on user selection. The form's `handleDatePickerChange` reads
// that string and writes it into Formik state via setPatientDetails — which
// is one of the state clobber paths these tests exercise.
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

import CreatePatientForm from "../CreatePatientForm";

const renderForm = (selectedPatient = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <CreatePatientForm
        showActionsButton={true}
        selectedPatient={selectedPatient}
        onClear={() => {}}
      />
    </IntlProvider>,
  );

const flush = () => new Promise((r) => setTimeout(r, 0));

beforeEach(() => {
  testState.pendingPhotoCallbacks = [];
  testState.postResponse = { status: "success" };
  testState.lastPostRequest = null;
  testState.getFromOpenElisServer.mockClear();
  testState.postToOpenElisServerJsonResponse.mockClear();
});

describe("CreatePatientForm — patient creation submits typed values", () => {
  test("a filled new-patient form sends every entered field on save", async () => {
    const user = userEvent.setup();
    renderForm({});
    await flush();

    await user.type(document.getElementById("lastName"), "Smith");
    await user.type(document.getElementById("firstName"), "John");
    // Carbon RadioButtonGroup wires onChange(value) — clicking the input is
    // the click event Carbon translates into the value callback.
    await user.click(document.getElementById("radio-1"));
    // CustomDatePicker shim — typing fills the input and fires onChange.
    await user.type(screen.getByTestId("dob-input"), "01/15/1990");

    await user.click(document.getElementById("submit"));
    await flush();

    expect(testState.postToOpenElisServerJsonResponse).toHaveBeenCalledTimes(1);
    const body = JSON.parse(testState.lastPostRequest.body);
    expect(body.lastName, "lastName must reach the server").toBe("Smith");
    expect(body.firstName, "firstName must reach the server").toBe("John");
    expect(body.gender, "gender must reach the server").toBe("M");
    expect(body.birthDateForDisplay, "DOB must reach the server").toBe(
      "01/15/1990",
    );
  });
});

describe("CreatePatientForm — backend save error preserves entries", () => {
  test("user's typed values are still in the form after a 400 response", async () => {
    const user = userEvent.setup();
    testState.postResponse = { statusCode: 400, error: "validation failed" };

    renderForm({});
    await flush();

    await user.type(document.getElementById("lastName"), "Smith");
    await user.type(document.getElementById("firstName"), "John");
    await user.click(document.getElementById("radio-1"));
    await user.type(screen.getByTestId("dob-input"), "01/15/1990");

    await user.click(document.getElementById("submit"));
    await flush();

    expect(testState.postToOpenElisServerJsonResponse).toHaveBeenCalledTimes(1);

    // The user can see their work — no reset/clobber on the error path.
    expect(document.getElementById("lastName")).toHaveValue("Smith");
    expect(document.getElementById("firstName")).toHaveValue("John");
    expect(document.getElementById("radio-1").checked).toBe(true);
    expect(screen.getByTestId("dob-input")).toHaveValue("01/15/1990");
  });
});

describe("CreatePatientForm — editing a patient survives async photo arrival", () => {
  test("a field typed during edit is not clobbered when /rest/patient-photos resolves", async () => {
    const user = userEvent.setup();

    renderForm({
      patientPK: "EXISTING-1",
      firstName: "Old",
      lastName: "Patient",
      gender: "M",
      birthDateForDisplay: "01/15/1990",
      nationalId: "EX-001",
    });
    await flush();

    // Edit toggle flips read-only off so the inputs accept input.
    const editToggle = document.getElementById("patient-edit-toggle");
    expect(editToggle, "edit toggle must be in the DOM").not.toBeNull();
    await user.click(editToggle);

    const firstNameInput = document.getElementById("firstName");
    await user.clear(firstNameInput);
    await user.type(firstNameInput, "UpdatedFirst");
    expect(firstNameInput).toHaveValue("UpdatedFirst");

    // The photo fetch was queued on mount (see network mock). Fire it now,
    // simulating the server response landing while the user types.
    expect(
      testState.pendingPhotoCallbacks.length,
      "photo fetch should have been queued during edit-mode mount",
    ).toBeGreaterThan(0);
    await act(async () => {
      testState.pendingPhotoCallbacks.forEach((cb) => cb());
      testState.pendingPhotoCallbacks = [];
    });
    await flush();

    // The user's edit must still be in the form — the previously-broken
    // enableReinitialize + setPatientDetails path would reset firstName
    // back to "Old" here.
    expect(
      document.getElementById("firstName"),
      "user's typed first-name must survive async photo arrival",
    ).toHaveValue("UpdatedFirst");
  });
});
