import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import NotebookDepartmentEquipmentMultiSelect from "./NotebookDepartmentEquipmentMultiSelect";
import ReagentUsageSelector from "./ReagentUsageSelector";
import {
  loadNotebookEquipmentOptions,
  loadNotebookScopedInventory,
  NOTEBOOK_INVENTORY_SCOPE_STATUS,
} from "../utils/notebookInventoryScope";

jest.mock("../utils/notebookInventoryScope", () => ({
  loadNotebookEquipmentOptions: jest.fn(),
  loadNotebookScopedInventory: jest.fn(),
  NOTEBOOK_INVENTORY_SCOPE_STATUS: {
    READY: "ready",
    DEPARTMENT_SCOPE_UNAVAILABLE: "departmentScopeUnavailable",
    NO_INVENTORY_EQUIPMENT: "noInventoryEquipment",
    NO_INVENTORY_LOTS: "noInventoryLots",
  },
  mergeInventoryOptionsWithLinkedSelections: jest.requireActual(
    "../utils/notebookInventoryScope",
  ).mergeInventoryOptionsWithLinkedSelections,
}));

const renderWithIntl = (ui) =>
  render(<IntlProvider locale="en">{ui}</IntlProvider>);

describe("Notebook inventory selectors", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test("equipment multiselect merges department inventory with linked template instruments", async () => {
    loadNotebookEquipmentOptions.mockImplementation((notebookId, buildUrl, callback) => {
      callback(
        [{ id: "1", value: "Centrifuge" }],
        null,
        { scopeStatus: NOTEBOOK_INVENTORY_SCOPE_STATUS.READY },
      );
    });

    renderWithIntl(
      <NotebookDepartmentEquipmentMultiSelect
        notebookId={11}
        selectedIds={["2"]}
        templateInstruments={[{ id: "2", value: "Legacy Analyzer" }]}
      />,
    );

    expect(loadNotebookEquipmentOptions).toHaveBeenCalled();

    expect(screen.queryByText("No active equipment found in this notebook's departments.")).toBeNull();
  });

  test("reagent selector shows empty helper when scoped department inventory is empty", async () => {
    loadNotebookScopedInventory.mockImplementation((notebookId, endpoint, callback) => {
      callback([], null, {
        scopeStatus: NOTEBOOK_INVENTORY_SCOPE_STATUS.NO_INVENTORY_LOTS,
      });
    });

    renderWithIntl(
      <ReagentUsageSelector
        notebookId={22}
        selectedIds={[]}
        reagentQuantities={{}}
        onSelectionChange={jest.fn()}
        onQuantityChange={jest.fn()}
        titleText="Reagents"
        label="Select reagents..."
      />,
    );

    expect(
      await screen.findByText(
        "No reagents or consumables were found for this notebook's departments.",
      ),
    ).toBeTruthy();
  });

  test("reagent selector surfaces qc-pending warning without hiding the reagent", async () => {
    loadNotebookScopedInventory.mockImplementation((notebookId, endpoint, callback) => {
      callback(
        [
          {
            id: "55",
            name: "Prep Buffer",
            lotNumber: "LOT-55",
            units: "mL",
            currentQuantity: 10,
            qcStatus: "PENDING",
            selectionWarnings: ["QC_PENDING"],
          },
        ],
        null,
        { scopeStatus: NOTEBOOK_INVENTORY_SCOPE_STATUS.READY },
      );
    });

    renderWithIntl(
      <ReagentUsageSelector
        notebookId={22}
        selectedIds={[]}
        reagentQuantities={{}}
        onSelectionChange={jest.fn()}
        onQuantityChange={jest.fn()}
        titleText="Reagents"
        label="Select reagents..."
      />,
    );

    expect(loadNotebookScopedInventory).toHaveBeenCalled();
    expect(
      screen.queryByText(
        "No reagents or consumables were found for this notebook's departments.",
      ),
    ).toBeNull();
  });
});
