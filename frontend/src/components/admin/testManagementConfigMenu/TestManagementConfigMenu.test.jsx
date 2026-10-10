import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import messages from "../../../languages/en.json";
import TestManagementConfigMenu from "./TestManagementConfigMenu";
import { NotificationContext } from "../../layout/Layout";

/**
 * The Test Management menu links to the domain- and component-aware editors.
 * The legacy pages they replace (Add test, Modify test, the rename screens,
 * the old manage and view screens) are gone, so there is one tile per entity.
 * Result select lists have no replacement, so their add and rename pages stay
 * and are the only rename entry.
 */
const LEGACY_PATHS = [
  "TestAdd",
  "TestModifyEntry",
  "TestRenameEntry",
  "PanelRenameEntry",
  "SampleTypeRenameEntry",
  "TestSectionRenameEntry",
  "UomRenameEntry",
  "MethodRenameEntry",
  "TestSectionManagement",
  "SampleTypeManagement",
  "UomManagement",
  "PanelManagement",
];

const renderMenu = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <NotificationContext.Provider
        value={{
          notificationVisible: false,
          setNotificationVisible: vi.fn(),
          addNotification: vi.fn(),
        }}
      >
        <MemoryRouter>
          <TestManagementConfigMenu />
        </MemoryRouter>
      </NotificationContext.Provider>
    </IntlProvider>,
  );

describe("Test Management menu", () => {
  it("links each catalogue entity to its editor", () => {
    renderMenu();
    const hrefs = screen
      .getAllByRole("link")
      .map((a) => a.getAttribute("href"));
    expect(hrefs).toEqual(
      expect.arrayContaining([
        "/MasterListsPage/TestCatalogList",
        "/MasterListsPage/TestCatalogList?entity=panels",
        "/MasterListsPage/SampleTypeEditor",
        "/MasterListsPage/LabUnitManagement",
        "/MasterListsPage/MethodManagement",
        "/MasterListsPage/UnitsOfMeasure",
        "/MasterListsPage/TestCatalog",
        "/MasterListsPage/TestActivation",
        "/MasterListsPage/TestOrderability",
        "/MasterListsPage/PanelOrder",
        "/MasterListsPage/ResultSelectListAdd",
        "/MasterListsPage/SelectListRenameEntry",
        "/MasterListsPage/CatalogImport",
        "/MasterListsPage/reflex",
        "/MasterListsPage/calculatedValue",
        "/MasterListsPage/program",
        "/MasterListsPage/ComplianceStandardsAdmin",
      ]),
    );
    expect(screen.getByText("Test Catalogue Editor")).toBeInTheDocument();
    expect(screen.getByText("Panel Editor")).toBeInTheDocument();
  });

  it("offers no replaced legacy page, and renames only result select lists", () => {
    renderMenu();
    const hrefs = screen
      .getAllByRole("link")
      .map((a) => a.getAttribute("href"));
    LEGACY_PATHS.forEach((legacy) => {
      expect(hrefs, legacy).not.toContain(`/MasterListsPage/${legacy}`);
    });
    expect(
      screen
        .getAllByText(/Rename/)
        .every((node) => /result list/.test(node.textContent)),
    ).toBe(true);
  });

  it("opens a tile inside the app instead of reloading the page", () => {
    let location;
    render(
      <IntlProvider locale="en" messages={messages}>
        <NotificationContext.Provider
          value={{
            notificationVisible: false,
            setNotificationVisible: vi.fn(),
            addNotification: vi.fn(),
          }}
        >
          <MemoryRouter
            initialEntries={["/MasterListsPage/testManagementConfigMenu"]}
          >
            <TestManagementConfigMenu />
            <Route
              path="*"
              render={(props) => {
                location = props.location;
                return null;
              }}
            />
          </MemoryRouter>
        </NotificationContext.Provider>
      </IntlProvider>,
    );
    const tile = document.getElementById("UnitsOfMeasure");

    const modified = fireEvent.click(tile, { ctrlKey: true });
    expect(modified).toBe(true);
    expect(location.pathname).toBe("/MasterListsPage/testManagementConfigMenu");

    const plain = fireEvent.click(tile);
    expect(plain).toBe(false);
    expect(location.pathname).toBe("/MasterListsPage/UnitsOfMeasure");
  });
});
