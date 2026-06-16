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
    getFromOpenElisServer.mockImplementation((url, cb) => cb(undefined));
    renderEditor();
    expect(
      await screen.findByText(messages["label.testCatalog.editor.loadError"]),
    ).toBeInTheDocument();
  });

  it("renders the test name + mounts the Basic Info section from the envelope", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb(envelope));
    renderEditor();
    expect(await screen.findByText("Glucose Panel")).toBeInTheDocument();
    // basic-info is the first section → its component mounts.
    expect(screen.getByTestId("basic-info-section")).toBeInTheDocument();
  });

  it("switches the panel when a different SideNav section is clicked", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb(envelope));
    const { container } = renderEditor();
    await screen.findByTestId("basic-info-section");
    fireEvent.click(container.querySelector('[data-cy="section-methods"]'));
    // Basic Info unmounts; the not-yet-built placeholder shows.
    expect(screen.queryByTestId("basic-info-section")).toBeNull();
    expect(
      screen.getByText(messages["label.testCatalog.section.pending"]),
    ).toBeInTheDocument();
  });

  it("navigates back to the master lists page on Cancel", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb(envelope));
    renderEditor();
    await screen.findByText("Glucose Panel");
    fireEvent.click(screen.getByRole("button", { name: "Cancel" }));
    expect(mockHistory.push).toHaveBeenCalledWith("/MasterListsPage");
  });
});
