/**
 * TestCatalogEditor — OGC-949 M2 / OGC-927 editor shell.
 *
 * Covers the previously-untested shell: empty + error states, the envelope
 * happy-path (heading + the first section mounted), SideNav section-switch, and
 * Cancel navigation. The network seam (getFromOpenElisServer) and the leaf
 * BasicInfoSection are mocked — the shell's own wiring is under test.
 */

// ========== MOCKS (before imports) ==========
const mockHistory = {
  push: vi.fn(),
  replace: vi.fn(),
  location: { search: "" },
};
let mockParams = {};

vi.mock("react-router-dom", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    useHistory: () => mockHistory,
    useParams: () => mockParams,
    // base is derived from the pathname; a non-/admin path → /MasterListsPage.
    useLocation: () => ({
      pathname: "/MasterListsPage/TestCatalogEditor/7",
      search: "",
    }),
  };
});

vi.mock("../../utils/Utils", () => ({ getFromOpenElisServer: vi.fn() }));

vi.mock("../../common/PageBreadCrumb", () => ({ default: () => null }));

vi.mock("../../layout/Layout", async () => {
  const React = await import("react");
  return {
    NotificationContext: React.createContext({
      addNotification: () => {},
      setNotificationVisible: () => {},
    }),
  };
});

vi.mock("./sections/BasicInfoSection", async () => {
  const React = await import("react");
  return {
    default: () =>
      React.createElement("div", { "data-testid": "basic-info-section" }),
  };
});

vi.mock("./sections/MethodsSection", async () => {
  const React = await import("react");
  return {
    default: () =>
      React.createElement("div", { "data-testid": "methods-section" }),
  };
});

vi.mock("./sections/SampleResultsSection", async () => {
  const React = await import("react");
  return {
    default: () =>
      React.createElement("div", { "data-testid": "sample-results-section" }),
  };
});

vi.mock("./sections/RangesSection", async () => {
  const React = await import("react");
  return {
    default: () =>
      React.createElement("div", { "data-testid": "ranges-section" }),
  };
});

vi.mock("./sections/StorageSection", async () => {
  const React = await import("react");
  return {
    default: () =>
      React.createElement("div", { "data-testid": "storage-section" }),
  };
});

vi.mock("./sections/AnalyzersSection", async () => {
  const React = await import("react");
  return {
    default: () =>
      React.createElement("div", { "data-testid": "analyzers-section" }),
  };
});

vi.mock("./sections/DisplayOrderSection", async () => {
  const React = await import("react");
  return {
    default: () =>
      React.createElement("div", { "data-testid": "display-order-section" }),
  };
});

vi.mock("./sections/TerminologySection", async () => {
  const React = await import("react");
  return {
    default: () =>
      React.createElement("div", { "data-testid": "terminology-section" }),
  };
});

vi.mock("./sections/PanelsSection", async () => {
  const React = await import("react");
  return {
    default: () =>
      React.createElement("div", { "data-testid": "panels-section" }),
  };
});

// ========== IMPORTS ==========
import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import TestCatalogEditor from "./TestCatalogEditor";
import { getFromOpenElisServer } from "../../utils/Utils";
import messages from "../../../languages/en.json";

const renderEditor = () =>
  render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        <TestCatalogEditor />
      </IntlProvider>
    </BrowserRouter>,
  );

const envelope = {
  testId: "7",
  name: "Glucose Panel",
  code: "GLU",
  domain: "CLINICAL",
  applicableSections: [
    "basic-info",
    "sample-results",
    "methods",
    "ranges",
    "storage",
    "panels",
    "terminology",
    "analyzers",
    "display-order",
  ],
};

beforeEach(() => {
  vi.clearAllMocks();
  mockParams = { testId: "7" };
});

describe("TestCatalogEditor shell", () => {
  it("shows the empty state when no test is selected", () => {
    mockParams = {}; // no testId in the route
    renderEditor();
    expect(
      screen.getByText(messages["label.testCatalog.editor.empty"]),
    ).toBeInTheDocument();
    expect(getFromOpenElisServer).not.toHaveBeenCalled();
  });

  it("shows an error state when the envelope fetch fails", async () => {
    mockParams = { testId: "7", section: "basic-info" };
    getFromOpenElisServer.mockImplementation((url, cb) => cb(undefined));
    renderEditor();
    expect(
      await screen.findByText(messages["label.testCatalog.editor.loadError"]),
    ).toBeInTheDocument();
  });

  it("renders the test name + mounts the section named in the URL", async () => {
    mockParams = { testId: "7", section: "basic-info" };
    getFromOpenElisServer.mockImplementation((url, cb) => cb(envelope));
    renderEditor();
    expect(await screen.findByText("Glucose Panel")).toBeInTheDocument();
    expect(screen.getByTestId("basic-info-section")).toBeInTheDocument();
  });

  // Section is driven entirely by the URL :section param — the editor owns no
  // nav (that lives in AdminSideNav). Each param mounts its section.
  // All nine v1 sections are built; each URL :section param mounts its section.
  it.each([
    ["methods", "methods-section"],
    ["sample-results", "sample-results-section"],
    ["ranges", "ranges-section"],
    ["storage", "storage-section"],
    ["analyzers", "analyzers-section"],
    ["display-order", "display-order-section"],
    ["terminology", "terminology-section"],
    ["panels", "panels-section"],
  ])(
    "mounts the %s section from the URL section param",
    async (sec, testid) => {
      mockParams = { testId: "7", section: sec };
      getFromOpenElisServer.mockImplementation((url, cb) => cb(envelope));
      renderEditor();
      expect(await screen.findByTestId(testid)).toBeInTheDocument();
      expect(screen.queryByTestId("basic-info-section")).toBeNull();
    },
  );

  it.each([["bogus"], [undefined]])(
    "canonicalizes a missing/invalid section into the URL (section=%s)",
    (sec) => {
      mockParams = sec ? { testId: "7", section: sec } : { testId: "7" };
      getFromOpenElisServer.mockImplementation((url, cb) => cb(envelope));
      renderEditor();
      expect(mockHistory.replace).toHaveBeenCalledWith(
        "/MasterListsPage/TestCatalogEditor/7/basic-info",
      );
    },
  );

  it("navigates to the test list on Cancel", async () => {
    mockParams = { testId: "7", section: "basic-info" };
    getFromOpenElisServer.mockImplementation((url, cb) => cb(envelope));
    renderEditor();
    await screen.findByText("Glucose Panel");
    fireEvent.click(screen.getByRole("button", { name: "Cancel" }));
    expect(mockHistory.push).toHaveBeenCalledWith(
      "/MasterListsPage/TestCatalogList",
    );
  });

  it("create mode: a non-Basic-Info section shows a 'save first' notice, not Basic Info", () => {
    mockParams = { testId: "new", section: "methods" };
    getFromOpenElisServer.mockImplementation((url, cb) => cb([]));
    renderEditor();
    expect(
      screen.getByText(
        messages["label.testCatalog.editor.createSaveFirst.title"],
      ),
    ).toBeInTheDocument();
    // The envelope is never fetched for a new test.
    expect(getFromOpenElisServer).not.toHaveBeenCalledWith(
      "/rest/test-catalog/tests/new",
      expect.anything(),
    );
  });
});
