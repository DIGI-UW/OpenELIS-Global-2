import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";

/**
 * LO-01-01 (UAT Round 2): the format for contact phone "does not conform to
 * prescribe format." Primary and contact phone must route through the same
 * configured validator and gate save in the same way; one accepting an
 * invalid value while the other rejects it is a regression.
 *
 * Display parity (both helperText hints reflect the same config) is locked
 * in CreatePatientForm.ogc671.test.jsx. This file locks validation parity:
 * a contact-phone blur fires the same backend endpoint as a primary-phone
 * blur, and an invalid contact-phone response disables Save.
 */

const mockedDisplayLists = {
  "/rest/displayList/PATIENT_HEALTH_REGIONS": [],
  "/rest/displayList/PATIENT_EDUCATION": [],
  "/rest/displayList/PATIENT_MARITAL_STATUS": [],
  "/rest/displayList/PATIENT_DISEASE_PROGRAMME": [],
};

const testState = vi.hoisted(() => ({
  phoneValidationResponse: { status: true, body: "" },
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
}));

testState.getFromOpenElisServer.mockImplementation((url, callback) => {
  if (typeof callback !== "function") return;
  if (url.startsWith("/rest/PhoneNumberValidationProvider")) {
    callback(testState.phoneValidationResponse);
    return;
  }
  if (url.startsWith("/rest/subjectNumberValidationProvider")) {
    callback({ status: true });
    return;
  }
  callback(mockedDisplayLists[url] ?? []);
});

testState.postToOpenElisServerJsonResponse.mockImplementation(
  (_, __, callback) => callback({ status: "success" }),
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
      PHONE_FORMAT:
        "+261 (37|38) XX XXX XX | +(37|38) XX XXX XX | (37|38) XX XXX XX",
      PHONE_FORMAT_LABEL: "37 XX XXX XX | 38 XX XXX XX",
      PHONE_INTERNATIONAL_VALIDATION: "E164",
      PHONE_INTERNATIONAL_FORMAT_LABEL: "+CC XXXXXXXX",
    },
  }),
}));

vi.mock("../../common/CustomNotification", () => ({
  AlertDialog: () => null,
  NotificationKinds: { success: "success", error: "error" },
}));
vi.mock("../AddressSearch", () => ({ default: () => null }));
vi.mock("../photoManagement/uploadPhoto/PatientImageSelector", () => ({
  default: () => null,
}));
vi.mock("../IdentificationDocuments", () => ({ default: () => null }));
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
  testState.phoneValidationResponse = { status: true, body: "" };
  testState.getFromOpenElisServer.mockClear();
  testState.postToOpenElisServerJsonResponse.mockClear();
});

describe("CreatePatientForm — primary + contact phone validation parity", () => {
  test("blurring contact phone fires the same backend validator as primary phone", async () => {
    const user = userEvent.setup();
    renderForm();
    await flush();

    const contactPhone = document.getElementById("contactPhone");
    expect(contactPhone, "contact phone field must render").not.toBeNull();

    await user.click(contactPhone);
    await user.type(contactPhone, "+261 37 12 345 67");
    contactPhone.blur();
    await flush();

    const validationCalls = testState.getFromOpenElisServer.mock.calls.filter(
      ([url]) => url.startsWith("/rest/PhoneNumberValidationProvider"),
    );
    expect(
      validationCalls.length,
      "contact phone blur must hit the phone validator endpoint",
    ).toBeGreaterThan(0);
    expect(validationCalls[0][0]).toContain("value=");
  });

  test("an invalid contact phone disables the Save button (same gate as primary)", async () => {
    const user = userEvent.setup();
    testState.phoneValidationResponse = {
      status: false,
      body: "Phone number does not match the configured format",
    };

    renderForm();
    await flush();

    const contactPhone = document.getElementById("contactPhone");
    await user.click(contactPhone);
    await user.type(contactPhone, "not-a-phone");
    contactPhone.blur();
    await flush();

    const saveBtn = document.getElementById("submit");
    expect(
      saveBtn.disabled,
      "Save must be blocked while a phone field is invalid — same protection as primary phone",
    ).toBe(true);
  });

  test("both phone fields render the same configured format hint (display parity)", async () => {
    renderForm();
    await flush();

    // ogc671 already asserts this end-to-end; this micro-assert keeps the
    // contract local to phone-validation regressions: primary + contact
    // helperText must come from the same getPhoneFormatHint output.
    const localHints = screen.getAllByText(
      "Local: 37 XX XXX XX | 38 XX XXX XX",
    );
    expect(
      localHints.length,
      "both phone fields must show the same local format hint",
    ).toBe(2);
  });
});
