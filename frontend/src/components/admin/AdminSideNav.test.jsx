// Tests for the Test Catalog Management entry in the admin SideNav.

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

// The nav fetches the open test's name for the "Editing: <name>" context line.
vi.mock("../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn((_endpoint, callback) =>
    callback({ name: "Hemoglobin" }),
  ),
}));

// ========== IMPORTS ==========
import React from "react";
import { render, act } from "@testing-library/react";
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
  it("lists all 9 sections but DISABLED (not navigable) off an editor route", () => {
    mockLocation = { pathname: "/MasterListsPage/reflex", search: "" };
    const { container } = renderNav();

    V1_SECTIONS.forEach((key) => {
      const item = container.querySelector(`[data-cy="section-${key}"]`);
      // present (breadth is always visible) ...
      expect(item).not.toBeNull();
      // ... but disabled and not a link to anywhere
      expect(item.getAttribute("aria-disabled")).toBe("true");
      expect(item.getAttribute("href")).toBeNull();
      expect(item.getAttribute("aria-describedby")).toBe(
        "testCatalogSectionsHelp",
      );
    });

    // the helper caption explains how to enable them
    const help = container.querySelector(
      '[data-cy="testCatalogSectionsContext"]',
    );
    expect(help).not.toBeNull();
    expect(help.textContent).toBe("Select a test to edit its sections");

    // the list item is present and labelled as the entry (not "back")
    const list = container.querySelector('[data-cy="testCatalogList"]');
    expect(list).not.toBeNull();
    expect(list.textContent).toBe("Test Catalog Editor");
  });

  it("makes the 9 sections live routed links when editing a test", () => {
    mockLocation = {
      pathname: "/MasterListsPage/TestCatalogEditor/7/methods",
      search: "",
    };
    const { container } = renderNav();

    V1_SECTIONS.forEach((key) => {
      const item = container.querySelector(`[data-cy="section-${key}"]`);
      expect(item).not.toBeNull();
      expect(item.getAttribute("aria-disabled")).toBeNull();
    });
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

    // wayfinding: list item flips to "back to list", context names the test
    expect(
      container.querySelector('[data-cy="testCatalogList"]').textContent,
    ).toBe("← All Tests");
    expect(
      container.querySelector('[data-cy="testCatalogSectionsContext"]')
        .textContent,
    ).toBe("Editing: Hemoglobin");
  });

  it("falls back to a generic context label when the test name can't load", async () => {
    const { getFromOpenElisServer } = await import("../utils/Utils");
    getFromOpenElisServer.mockImplementationOnce((_endpoint, callback) =>
      callback(null),
    );
    mockLocation = {
      pathname: "/MasterListsPage/TestCatalogEditor/7/methods",
      search: "",
    };
    const { container } = renderNav();
    expect(
      container.querySelector('[data-cy="testCatalogSectionsContext"]')
        .textContent,
    ).toBe("Editing test");
  });

  it("aborts the in-flight test-name fetch on unmount", async () => {
    const { getFromOpenElisServer } = await import("../utils/Utils");
    mockLocation = {
      pathname: "/MasterListsPage/TestCatalogEditor/7/methods",
      search: "",
    };
    const { unmount } = renderNav();
    // getFromOpenElisServer(endpoint, callback, signal) — the 3rd arg
    const signal = getFromOpenElisServer.mock.calls.at(-1)[2];
    expect(signal).toBeInstanceOf(AbortSignal);
    expect(signal.aborted).toBe(false);
    // act() so React 17 flushes the passive-effect cleanup synchronously
    act(() => {
      unmount();
    });
    expect(signal.aborted).toBe(true);
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
