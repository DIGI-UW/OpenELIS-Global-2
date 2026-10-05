import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../languages/en.json";
import TestManagementConfigMenu from "./TestManagementConfigMenu";
import { NotificationContext } from "../../layout/Layout";

/**
 * The Test Management menu links only to the domain- and component-aware
 * editors. The legacy pages (Add test, Modify test, the rename screens, the
 * old manage and view screens) are gone: each editor covers its entity's
 * whole life, so there is one tile per entity and no rename section.
 */
const LEGACY_PATHS = [
  "TestAdd",
  "TestModifyEntry",
  "TestActivation",
  "TestOrderability",
  "TestRenameEntry",
  "PanelRenameEntry",
  "SampleTypeRenameEntry",
  "TestSectionRenameEntry",
  "UomRenameEntry",
  "SelectListRenameEntry",
  "MethodRenameEntry",
  "TestSectionManagement",
  "SampleTypeManagement",
  "UomManagement",
  "PanelManagement",
  "ResultSelectListAdd",
  "TestCatalog",
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

  it("offers no legacy page and no rename section", () => {
    renderMenu();
    const hrefs = screen
      .getAllByRole("link")
      .map((a) => a.getAttribute("href"));
    LEGACY_PATHS.forEach((legacy) => {
      expect(hrefs, legacy).not.toContain(`/MasterListsPage/${legacy}`);
    });
    expect(screen.queryByText(/Rename/)).toBeNull();
  });
});
