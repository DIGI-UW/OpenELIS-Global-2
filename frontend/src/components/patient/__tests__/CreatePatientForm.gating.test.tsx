import React from "react";
import { render } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";

/**
 * Two conditional-render rules on CreatePatientForm:
 *
 *   1. Target type (`targetDiseaseProgramme`) renders as <TextArea> with
 *      a character counter (maxCount=255, enableCounter), not <Select>.
 *
 *   2. GPS Latitude / Longitude inputs render only when
 *      `configurationProperties.PATIENT_GPS_CAPTURE_ENABLED === "true"`.
 *      The gate is on the all-caps key (matches Property.toString() in
 *      DisplayListController). The camelCase form `patientGpsCaptureEnabled`
 *      must NOT enable rendering.
 */

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn((url, callback) => {
    if (typeof callback === "function") {
      callback([]);
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

// Render the form with a per-test configurationProperties override. We can't
// just mutate the module-scope context default, so we wrap in a Provider —
// React's useContext(ConfigurationContext) picks up the nearest Provider's
// value, overriding createContext's default.
const renderWithConfig = (configurationProperties) =>
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

describe("CreatePatientForm Target type renders as TextArea with counter (OGC-669 regression)", () => {
  test("targetDiseaseProgramme is a TEXTAREA, not a SELECT", async () => {
    renderWithConfig({
      USE_NEW_ADDRESS_HIERARCHY: "false",
      PATIENT_GPS_CAPTURE_ENABLED: "false",
    });
    await flush();

    const target = document.getElementById("targetDiseaseProgramme");
    expect(target, "targetDiseaseProgramme must be in DOM").not.toBeNull();
    expect(target.tagName, "Target type must render as TEXTAREA").toBe(
      "TEXTAREA",
    );
  });

  test("Carbon's enableCounter prop renders the maxLength counter", async () => {
    renderWithConfig({
      USE_NEW_ADDRESS_HIERARCHY: "false",
      PATIENT_GPS_CAPTURE_ENABLED: "false",
    });
    await flush();

    const target = document.getElementById("targetDiseaseProgramme");
    expect(target).not.toBeNull();
    expect(target.getAttribute("maxlength")).toBe("255");
    // Carbon renders the counter as a sibling div.cds--text-area__counter
    // inside the same form-item wrapper. Confirm the wrapper is annotated.
    const wrapper = target.closest(".cds--text-area__wrapper");
    expect(wrapper, "Carbon TextArea wrapper must exist").not.toBeNull();
  });
});

describe("CreatePatientForm GPS lat/long render gate (OGC-650 regression)", () => {
  test("with PATIENT_GPS_CAPTURE_ENABLED=true, both gpsLatitude + gpsLongitude render", async () => {
    renderWithConfig({
      USE_NEW_ADDRESS_HIERARCHY: "false",
      PATIENT_GPS_CAPTURE_ENABLED: "true",
    });
    await flush();

    expect(
      document.getElementById("gpsLatitude"),
      "gpsLatitude must render when toggle is on",
    ).not.toBeNull();
    expect(
      document.getElementById("gpsLongitude"),
      "gpsLongitude must render when toggle is on",
    ).not.toBeNull();
  });

  test("with PATIENT_GPS_CAPTURE_ENABLED=false, GPS fields are absent", async () => {
    renderWithConfig({
      USE_NEW_ADDRESS_HIERARCHY: "false",
      PATIENT_GPS_CAPTURE_ENABLED: "false",
    });
    await flush();

    expect(
      document.getElementById("gpsLatitude"),
      "gpsLatitude must NOT render when toggle is off",
    ).toBeNull();
    expect(
      document.getElementById("gpsLongitude"),
      "gpsLongitude must NOT render when toggle is off",
    ).toBeNull();
  });

  test("with PATIENT_GPS_CAPTURE_ENABLED unset (undefined), GPS fields are absent (vanilla OE2 default)", async () => {
    renderWithConfig({
      USE_NEW_ADDRESS_HIERARCHY: "false",
      // PATIENT_GPS_CAPTURE_ENABLED intentionally omitted — `undefined === "true"` is false.
    });
    await flush();

    expect(document.getElementById("gpsLatitude")).toBeNull();
    expect(document.getElementById("gpsLongitude")).toBeNull();
  });

  test("the camelCase key alone does NOT enable GPS (regression for the original key-mismatch bug)", async () => {
    renderWithConfig({
      USE_NEW_ADDRESS_HIERARCHY: "false",
      // The original buggy gate read this camelCase key. Setting only the
      // camelCase form must NOT enable GPS — only the all-caps key counts.
      patientGpsCaptureEnabled: "true",
    });
    await flush();

    expect(
      document.getElementById("gpsLatitude"),
      "GPS must not render from camelCase key alone — gate is on PATIENT_GPS_CAPTURE_ENABLED",
    ).toBeNull();
  });
});
