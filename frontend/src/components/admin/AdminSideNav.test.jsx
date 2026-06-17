/**
 * AdminSideNav — OGC-949 / #3504.
 *
 * The Test Catalog editor's sections live HERE (URL-routed) under the dedicated
 * "Test Catalog Management" entry, and only while editing a test. These tests
 * guard the shared-component blast radius: a non-editor admin route must render
 * exactly as before (no section items), and an editor route must render all 9
 * sections as routed links with the active one marked.
 */

// ========== MOCKS (before imports) ==========
const mockHistory = { push: vi.fn() };
let mockLocation = { pathname: "/MasterListsPage", search: "" };

vi.mock("react-router-dom", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    useHistory: () => mockHistory,
    useLocation: () => mockLocation,
  };
});

// ========== IMPORTS ==========
import React from "react";
import { render } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import AdminSideNav from "./AdminSideNav";
import { V1_SECTIONS } from "./testCatalog/sectionConfig";
import messages from "../../languages/en.json";

const renderNav = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <AdminSideNav />
    </IntlProvider>,
  );

beforeEach(() => vi.clearAllMocks());

describe("AdminSideNav — Test Catalog Management entry", () => {
  it("renders NO editor section items on a non-editor admin route", () => {
    mockLocation = { pathname: "/MasterListsPage/reflex", search: "" };
    const { container } = renderNav();
    V1_SECTIONS.forEach((key) =>
      expect(container.querySelector(`[data-cy="section-${key}"]`)).toBeNull(),
    );
    // the list item is always present in the dedicated entry
    expect(
      container.querySelector('[data-cy="testCatalogList"]'),
    ).not.toBeNull();
  });

  it("renders all 9 URL-routed sections when editing a test", () => {
    mockLocation = {
      pathname: "/MasterListsPage/TestCatalogEditor/7/methods",
      search: "",
    };
    const { container } = renderNav();
    V1_SECTIONS.forEach((key) =>
      expect(
        container.querySelector(`[data-cy="section-${key}"]`),
      ).not.toBeNull(),
    );
    // each links to the routed section URL
    expect(
      container
        .querySelector('[data-cy="section-ranges"]')
        .getAttribute("href"),
    ).toBe("/MasterListsPage/TestCatalogEditor/7/ranges");
    // the active section (methods) is aria-current; others are not
    expect(
      container
        .querySelector('[data-cy="section-methods"]')
        .getAttribute("aria-current"),
    ).toBe("page");
    expect(
      container
        .querySelector('[data-cy="section-basic-info"]')
        .getAttribute("aria-current"),
    ).toBeNull();
  });

  it("uses the /admin base prefix when on an /admin editor route", () => {
    mockLocation = {
      pathname: "/admin/TestCatalogEditor/7/storage",
      search: "",
    };
    const { container } = renderNav();
    expect(
      container
        .querySelector('[data-cy="section-storage"]')
        .getAttribute("href"),
    ).toBe("/admin/TestCatalogEditor/7/storage");
  });
});
