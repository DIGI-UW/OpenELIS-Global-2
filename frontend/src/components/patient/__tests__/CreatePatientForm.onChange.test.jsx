import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";

/**
 * Regression: education / maritialStatus / nationality Selects must update
 * Formik state on change.
 *
 * Each Select previously had `onChange={() => {}}` which overrode Carbon's
 * default handler and prevented Formik's setFieldValue from firing. The
 * dropdown *appeared* responsive (Carbon manages display state internally)
 * but `values.<field>` stayed empty — the column persisted as null on save.
 *
 * Since the Selects are controlled (`value={values.<field> || ""}`), the
 * DOM `value` reflects Formik state. With the bug, picking an option
 * causes a render where the value snaps back to "". With the fix, the
 * picked value persists.
 */

const mockedDisplayLists = {
  "/rest/displayList/PATIENT_HEALTH_REGIONS": [],
  "/rest/displayList/PATIENT_EDUCATION": [
    { id: "PRIMARY", value: "Primary" },
    { id: "SECONDARY", value: "Secondary" },
    { id: "UNIVERSITY", value: "University" },
  ],
  "/rest/displayList/PATIENT_MARITAL_STATUS": [
    { id: "SINGLE", value: "Single" },
    { id: "MARRIED", value: "Married" },
  ],
  "/rest/displayList/PATIENT_DISEASE_PROGRAMME": [],
};

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn((url, callback) => {
    if (typeof callback === "function") {
      callback(mockedDisplayLists[url] ?? []);
    }
  }),
  postToOpenElisServerJsonResponse: vi.fn(),
  resolveApiErrorMessage: vi.fn((err) => String(err)),
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
      PHONE_FORMAT: "+261-xx-xxx-xx-xx",
      DEFAULT_DATE_LOCALE: "en-US",
    },
  }),
}));

vi.mock("../../common/CustomNotification", () => ({
  AlertDialog: () => null,
  NotificationKinds: { success: "success", error: "error" },
}));

vi.mock("../AddressSearch", () => ({
  default: () => <div data-testid="address-search-mock" />,
}));

vi.mock("../photoManagement/uploadPhoto/PatientImageSelector", () => ({
  default: () => <div data-testid="patient-image-selector-mock" />,
}));

vi.mock("../IdentificationDocuments", () => ({
  default: () => <div data-testid="identification-documents-mock" />,
}));

vi.mock("../PatientFormObserver", () => ({
  default: () => null,
}));

vi.mock("../../common/CustomDatePicker", () => ({
  default: () => <div data-testid="custom-date-picker-mock" />,
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

// Carbon's <Select> renders a native <select> with role="combobox".
// Use the id attribute as the most stable anchor — labels can collide with
// helper text on the same field, and translated labels can drift.
const findSelectById = (id) => document.getElementById(id);

describe("CreatePatientForm Select onChange wiring (OGC-669 regression)", () => {
  test("education Select preserves user pick (regression for empty-onChange)", async () => {
    const user = userEvent.setup();
    renderForm();

    // Wait one microtask so the form's useEffect callbacks fire and populate
    // the educationList state.
    await new Promise((r) => setTimeout(r, 0));

    const select = findSelectById("education");
    expect(select, "education Select must be in DOM").not.toBeNull();
    expect(select.tagName).toBe("SELECT");
    expect(select.querySelectorAll("option").length).toBeGreaterThan(1);

    // <SelectItem value=...> binds to the displayList's `.value` field, so
    // the option value is "Secondary", not the "SECONDARY" id.
    await user.selectOptions(select, "Secondary");

    // Controlled <select>: with the bug, value snaps back to "" because
    // Formik's values.education never updated; with the fix, value persists.
    expect(select).toHaveValue("Secondary");
  });

  test("maritialStatus Select preserves user pick (regression for empty-onChange)", async () => {
    const user = userEvent.setup();
    renderForm();
    await new Promise((r) => setTimeout(r, 0));

    const select = findSelectById("maritialStatus");
    expect(select, "maritialStatus Select must be in DOM").not.toBeNull();
    expect(select.querySelectorAll("option").length).toBeGreaterThan(1);

    await user.selectOptions(select, "Married");
    expect(select).toHaveValue("Married");
  });

  test("nationality Select preserves user pick (regression for empty-onChange)", async () => {
    const user = userEvent.setup();
    renderForm();
    await new Promise((r) => setTimeout(r, 0));

    const select = findSelectById("nationality");
    expect(select, "nationality Select must be in DOM").not.toBeNull();
    // nationalityList is imported from data/countries.js (static), so options
    // are always > 1 even without server data.
    expect(select.querySelectorAll("option").length).toBeGreaterThan(1);

    // Pick a value we know exists in nationalityList.
    const optionValues = Array.from(select.querySelectorAll("option"))
      .map((o) => o.value)
      .filter(Boolean);
    expect(optionValues.length).toBeGreaterThan(0);
    const target = optionValues.includes("MG") ? "MG" : optionValues[0];

    await user.selectOptions(select, target);
    expect(select).toHaveValue(target);
  });
});
