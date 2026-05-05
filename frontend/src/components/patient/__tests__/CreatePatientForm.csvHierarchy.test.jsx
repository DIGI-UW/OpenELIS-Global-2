import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";

/**
 * Regression: CSV-driven address hierarchy renders the right control per
 * `level.inputType` from `/rest/address-hierarchy/levels`.
 *
 * Distro `madagascar-levels.csv` declares 5 levels (3 dropdown + 2 freetext);
 * the OE2 handler exposes per-level `inputType` via the levels API; the
 * renderer in `CreatePatientForm.jsx` branches on `level.inputType`:
 *   - "freetext" → <TextInput> bound by typeName→bindKey lookup
 *     (`FREETEXT_HIERARCHY_BIND_KEYS` at module scope)
 *   - "dropdown" (default) → cascading <Select>
 *
 * If the renderer regresses (e.g. someone reverts to unconditional Select
 * for all levels, or hardcodes Fokontany/HamletOrLot back), this test fails.
 */

const mockedDisplayLists = {
  "/rest/displayList/PATIENT_HEALTH_REGIONS": [],
  "/rest/displayList/PATIENT_EDUCATION": [],
  "/rest/displayList/PATIENT_MARITAL_STATUS": [],
  "/rest/displayList/PATIENT_DISEASE_PROGRAMME": [],
};

// Madagascar shape per distro PR #9 madagascar-levels.csv: 3 dropdown, 2 freetext.
const mockedAddressHierarchyLevels = [
  { level: 1, typeId: "13", typeName: "Province", inputType: "dropdown" },
  { level: 2, typeId: "8", typeName: "Health Region", inputType: "dropdown" },
  {
    level: 3,
    typeId: "7",
    typeName: "Health District",
    inputType: "dropdown",
  },
  { level: 4, typeId: "14", typeName: "Fokontany", inputType: "freetext" },
  { level: 5, typeId: "15", typeName: "Hamlet/Lot", inputType: "freetext" },
];

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn((url, callback) => {
    if (typeof callback !== "function") return;
    if (url === "/rest/address-hierarchy/levels") {
      callback(mockedAddressHierarchyLevels);
      return;
    }
    if (url.startsWith("/rest/address-hierarchy/level/")) {
      // Top-level dropdown values (used by the cascade init for level 1).
      callback([
        { id: "1", value: "Antananarivo" },
        { id: "2", value: "Toamasina" },
      ]);
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
    configurationProperties: {
      USE_NEW_ADDRESS_HIERARCHY: "true",
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

// Wait one microtask so useEffect callbacks fire and populate state.
const flush = () => new Promise((r) => setTimeout(r, 0));

describe("CreatePatientForm CSV-driven address hierarchy (OGC-669 regression)", () => {
  test("renders 3 cascading Selects for dropdown levels", async () => {
    renderForm();
    await flush();

    // Dropdown levels render with id `address_hierarchy_${levelIndex}` per
    // the `.map((level, levelIndex) => ...)` block in CreatePatientForm.jsx.
    for (let i = 0; i < 3; i++) {
      const select = document.getElementById(`address_hierarchy_${i}`);
      expect(
        select,
        `address_hierarchy_${i} (dropdown level ${i + 1}) must render`,
      ).not.toBeNull();
      expect(select.tagName).toBe("SELECT");
    }
  });

  test("renders TextInput for Fokontany freetext level (bindKey=fokontany)", async () => {
    renderForm();
    await flush();

    const fokontany = document.getElementById("fokontany");
    expect(fokontany, "fokontany TextInput must render").not.toBeNull();
    expect(fokontany.tagName).toBe("INPUT");
    expect(fokontany.getAttribute("type") || "text").toMatch(/text/i);
  });

  test("renders TextInput for Hamlet/Lot freetext level (bindKey=hamletOrLot)", async () => {
    renderForm();
    await flush();

    const hamletOrLot = document.getElementById("hamletOrLot");
    expect(hamletOrLot, "hamletOrLot TextInput must render").not.toBeNull();
    expect(hamletOrLot.tagName).toBe("INPUT");
  });

  test("freetext field updates on user typing (regression for hardcoded reversion)", async () => {
    const user = userEvent.setup();
    renderForm();
    await flush();

    const fokontany = document.getElementById("fokontany");
    expect(fokontany).not.toBeNull();

    await user.type(fokontany, "Ankorondrano");
    expect(fokontany).toHaveValue("Ankorondrano");
  });

  test("does NOT render TextInput for an unknown freetext typeName (defensive guard)", async () => {
    // Override the levels API for this test to include an unknown freetext
    // level — the renderer's `if (!bindKey) return null` guard should skip it.
    const { getFromOpenElisServer } = await import("../../utils/Utils");
    getFromOpenElisServer.mockImplementationOnce((url, callback) => {});
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (typeof callback !== "function") return;
      if (url === "/rest/address-hierarchy/levels") {
        callback([
          ...mockedAddressHierarchyLevels,
          {
            level: 6,
            typeId: "99",
            typeName: "UnknownTypeName",
            inputType: "freetext",
          },
        ]);
        return;
      }
      if (url.startsWith("/rest/address-hierarchy/level/")) {
        callback([]);
        return;
      }
      if (url.startsWith("/rest/address-hierarchy/children")) {
        callback([]);
        return;
      }
      callback(mockedDisplayLists[url] ?? []);
    });

    renderForm();
    await flush();

    // Known freetext bindKeys still render.
    expect(document.getElementById("fokontany")).not.toBeNull();
    expect(document.getElementById("hamletOrLot")).not.toBeNull();
    // Unknown typeName "UnknownTypeName" has no entry in
    // FREETEXT_HIERARCHY_BIND_KEYS so the renderer returns null. There's no
    // input/select with that id.
    expect(document.getElementById("UnknownTypeName")).toBeNull();
  });
});
