import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import { createPatientValidationSchema } from "../../formModel/validationSchema/CreatePatientValidationShema";

const mockedDisplayLists = {
  "/rest/displayList/PATIENT_HEALTH_REGIONS": [],
  "/rest/displayList/PATIENT_EDUCATION": [],
  "/rest/displayList/PATIENT_MARITAL_STATUS": [],
  "/rest/displayList/PATIENT_DISEASE_PROGRAMME": [],
};

const mockedAddressHierarchyLevels = [
  {
    level: 1,
    typeId: "13",
    typeName: "Province",
    inputType: "dropdown",
    displayKey: "patient.address.province",
    sortOrder: 3,
  },
  {
    level: 2,
    typeId: "8",
    typeName: "Health Region",
    inputType: "dropdown",
    displayKey: "patient.address.healthregion",
    sortOrder: 4,
  },
  {
    level: 3,
    typeId: "7",
    typeName: "Health District",
    inputType: "dropdown",
    displayKey: "patient.address.healthdistrict",
    sortOrder: 5,
  },
  {
    level: 4,
    typeId: "14",
    typeName: "Fokontany",
    inputType: "freetext",
    displayKey: "patient.address.fokontany",
    sortOrder: 1,
    bindKey: "addressHierarchy_3",
  },
  {
    level: 5,
    typeId: "15",
    typeName: "Hamlet/Lot",
    inputType: "freetext",
    displayKey: "patient.address.hamletOrLot",
    sortOrder: 2,
    bindKey: "addressHierarchy_4",
  },
];

const madagascarRegistrationConfig = {
  USE_NEW_ADDRESS_HIERARCHY: "true",
  PATIENT_GPS_CAPTURE_ENABLED: "false",
  DEFAULT_NATIONALITY: "",
  DEFAULT_DATE_LOCALE: "en-US",
  PATIENT_NATIONAL_ID_REQUIRED: "false",
  PATIENT_ALIAS_ENABLED: "true",
  PATIENT_ALIAS_LABEL: "Alias",
  PHONE_FORMAT:
    "+261 (37|38) XX XXX XX | +(37|38) XX XXX XX | (37|38) XX XXX XX",
  PHONE_FORMAT_LABEL: "37 XX XXX XX | 38 XX XXX XX",
  PHONE_INTERNATIONAL_VALIDATION: "E164",
  PHONE_INTERNATIONAL_FORMAT_LABEL: "+CC XXXXXXXX",
  PATIENT_ID_DOCUMENTS_LABEL: "Identification Documents (e.g. CIN)",
};

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn((url, callback) => {
    if (typeof callback !== "function") return;
    if (url === "/rest/address-hierarchy/levels") {
      callback(mockedAddressHierarchyLevels);
      return;
    }
    if (url.startsWith("/rest/address-hierarchy/level/")) {
      callback([{ id: "1", value: "Antananarivo" }]);
      return;
    }
    if (url.startsWith("/rest/address-hierarchy/children")) {
      callback([]);
      return;
    }
    callback(mockedDisplayLists[url] ?? []);
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
    configurationProperties: {},
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
import { ConfigurationContext } from "../../layout/Layout";

const renderForm = (configurationProperties = madagascarRegistrationConfig) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <ConfigurationContext.Provider value={{ configurationProperties }}>
        <CreatePatientForm
          showActionsButton={true}
          selectedPatient={{}}
          onClear={() => {}}
        />
      </ConfigurationContext.Provider>
    </IntlProvider>,
  );

const flush = () => new Promise((r) => setTimeout(r, 0));

const position = (element) =>
  Array.from(document.querySelectorAll("input, select, textarea")).indexOf(
    element,
  );

const countOccurrences = (text, value) =>
  text.match(new RegExp(value, "g"))?.length ?? 0;

describe("CreatePatientForm OGC-671 config-driven registration requirements", () => {
  test("National ID frontend validation follows PATIENT_NATIONAL_ID_REQUIRED=false", async () => {
    const schema = createPatientValidationSchema({
      PATIENT_NATIONAL_ID_REQUIRED: "false",
    });

    await expect(
      schema.isValid({
        nationalId: "",
        firstName: "Solo",
        lastName: "Rabe",
        gender: "M",
        birthDateForDisplay: "05/15/1990",
        email: "",
        patientContact: { person: { email: "" } },
      }),
    ).resolves.toBe(true);
  });

  test("National ID frontend validation still requires the field by default", async () => {
    const schema = createPatientValidationSchema({});

    await expect(
      schema.isValid({
        nationalId: "",
        firstName: "Solo",
        lastName: "Rabe",
        gender: "M",
        birthDateForDisplay: "05/15/1990",
        email: "",
        patientContact: { person: { email: "" } },
      }),
    ).resolves.toBe(false);
  });

  test("National ID label required marker follows config", async () => {
    renderForm();
    await flush();

    expect(
      document.querySelector('label[for="nationalId"] .requiredlabel'),
    ).toBeNull();
  });

  test("Alias field is config-enabled immediately after patient name fields", async () => {
    renderForm();
    await flush();

    const lastName = document.getElementById("lastName");
    const firstName = document.getElementById("firstName");
    const alias = document.getElementById("aka");

    expect(
      alias,
      "Alias input must render when PATIENT_ALIAS_ENABLED=true",
    ).not.toBeNull();
    expect(position(lastName)).toBeLessThan(position(firstName));
    expect(position(firstName)).toBeLessThan(position(alias));
    expect(screen.getByLabelText("Alias")).toBe(alias);
  });

  test("Alias field is absent when config-disabled", async () => {
    renderForm({
      ...madagascarRegistrationConfig,
      PATIENT_ALIAS_ENABLED: "false",
    });
    await flush();

    expect(document.getElementById("aka")).toBeNull();
  });

  test("phone labels show local and international format hints from config", async () => {
    renderForm();
    await flush();

    const primaryPhoneLabel = document.querySelector(
      'label[for="primaryPhone"]',
    );
    const contactPhoneLabel = document.querySelector(
      'label[for="contactPhone"]',
    );
    expect(primaryPhoneLabel).toHaveTextContent("Primary phone:");
    expect(primaryPhoneLabel).not.toHaveTextContent("Local:");
    expect(contactPhoneLabel).toHaveTextContent("Contact Phone:");
    expect(contactPhoneLabel).not.toHaveTextContent("Local:");

    expect(
      screen.getAllByText("Local: 37 XX XXX XX | 38 XX XXX XX"),
    ).toHaveLength(2);
    expect(screen.getAllByText("International: +CC XXXXXXXX")).toHaveLength(2);
    expect(document.body).not.toHaveTextContent("+261 xx-xxx-xx-xx");
    expect(document.body).not.toHaveTextContent("+261 (37|38) XX XXX XX");
    expect(countOccurrences(document.body.textContent, "Local:")).toBe(2);
    expect(countOccurrences(document.body.textContent, "International:")).toBe(
      2,
    );
  });

  test("phone labels omit international hint when international validation is empty", async () => {
    renderForm({
      ...madagascarRegistrationConfig,
      PHONE_INTERNATIONAL_VALIDATION: "",
    });
    await flush();

    const primaryPhoneLabel = document.querySelector(
      'label[for="primaryPhone"]',
    );
    const contactPhoneLabel = document.querySelector(
      'label[for="contactPhone"]',
    );

    expect(primaryPhoneLabel).not.toHaveTextContent("Local:");
    expect(primaryPhoneLabel).not.toHaveTextContent("International:");
    expect(contactPhoneLabel).not.toHaveTextContent("Local:");
    expect(contactPhoneLabel).not.toHaveTextContent("International:");
    expect(
      screen.getAllByText("Local: 37 XX XXX XX | 38 XX XXX XX"),
    ).toHaveLength(2);
    expect(screen.queryByText(/International:/)).not.toBeInTheDocument();
  });

  test("identification document section label follows config", async () => {
    renderForm();
    await flush();

    expect(
      screen.getByText("Identification Documents (e.g. CIN)"),
    ).toBeInTheDocument();
  });
});
