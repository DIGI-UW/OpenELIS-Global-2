/**
 * PanelsList — OGC-224 (Panel Management Domain Upgrade v2.2).
 *
 * The Panels context of the Test Catalog Management shell:
 * - ?entity=panels on the list route renders the Panels list (dispatcher);
 * - rows show name / LOINC / test count / domain tag / derived sample types /
 *   status, per the FRS column spec;
 * - the domain-upgrade banner is present;
 * - search + domain + status filtering (client-side, pure function);
 * - "Add Panel" opens a blank panel in the editor shell;
 * - a failed fetch shows the error state.
 */

// ========== MOCKS (before imports) ==========
const mockHistory = {
  push: vi.fn(),
  replace: vi.fn(),
  location: { search: "" },
};

vi.mock("react-router-dom", async (importOriginal) => {
  const actual = await importOriginal();
  return { ...actual, useHistory: () => mockHistory };
});

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

vi.mock("../../common/PageBreadCrumb", () => ({ default: () => null }));

// ========== IMPORTS ==========
import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import TestCatalogList from "./TestCatalogList";
import PanelsList, { filterPanels } from "./PanelsList";
import { getFromOpenElisServer } from "../../utils/Utils";
import messages from "../../../languages/en.json";

// ========== HELPERS ==========
const PANELS = [
  {
    id: "1",
    name: "Complete Blood Count",
    loinc: "58410-2",
    domain: "CLINICAL",
    active: true,
    testCount: 8,
    sampleTypes: ["Whole Blood (EDTA)"],
  },
  {
    id: "2",
    name: "Thyroid Function Panel",
    loinc: "55204-3",
    domain: "CLINICAL",
    active: false,
    testCount: 3,
    sampleTypes: ["Serum"],
  },
];

const mockServer = (panels = PANELS) => {
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url.startsWith("/rest/test-catalog/panels")) {
      cb(panels);
    } else if (url.startsWith("/rest/domains")) {
      cb([{ id: "CLINICAL", labelKey: "label.domain.CLINICAL" }]);
    } else {
      cb([]);
    }
  });
};

const wrap = (node) =>
  render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        {node}
      </IntlProvider>
    </BrowserRouter>,
  );

beforeEach(() => {
  vi.clearAllMocks();
  mockHistory.location = { search: "" };
});

describe("TestCatalogList entity dispatch (OGC-224)", () => {
  it("?entity=panels renders the Panels context instead of the tests list", async () => {
    mockHistory.location = { search: "?entity=panels" };
    mockServer();
    wrap(<TestCatalogList />);
    expect(await screen.findByText("Complete Blood Count")).toBeInTheDocument();
    expect(
      getFromOpenElisServer.mock.calls.some(([url]) =>
        url.startsWith("/rest/test-catalog/panels?includeInactive=true"),
      ),
    ).toBe(true);
  });
});

describe("PanelsList (FRS v2.2 list spec)", () => {
  it("renders the FRS columns: name, LOINC, tests, domain tag, derived sample types, status", async () => {
    mockServer();
    wrap(<PanelsList />);
    expect(await screen.findByText("Complete Blood Count")).toBeInTheDocument();
    expect(screen.getByText("58410-2")).toBeInTheDocument();
    expect(screen.getByText("8")).toBeInTheDocument();
    expect(screen.getAllByText("Clinical").length).toBeGreaterThan(0);
    expect(screen.getByText("Whole Blood (EDTA)")).toBeInTheDocument();
    expect(screen.getAllByText("Active").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Inactive").length).toBeGreaterThan(0);
  });

  it("shows the domain-upgrade banner", async () => {
    mockServer();
    wrap(<PanelsList />);
    expect(
      await screen.findByText(messages["note.panel.domainUpgrade"]),
    ).toBeInTheDocument();
    expect(
      screen.getByText(messages["note.panel.domainLaterPhase"]),
    ).toBeInTheDocument();
  });

  it("Add Panel opens a blank panel in the editor shell", async () => {
    mockServer();
    wrap(<PanelsList />);
    await screen.findByText("Complete Blood Count");
    fireEvent.click(screen.getByTestId("panel-add"));
    expect(mockHistory.push).toHaveBeenCalledWith(
      "/MasterListsPage/TestCatalogEditor/panel/new/basic-info",
    );
  });

  it("a row opens that panel's editor", async () => {
    mockServer();
    wrap(<PanelsList />);
    fireEvent.click(await screen.findByText("Complete Blood Count"));
    expect(mockHistory.push).toHaveBeenCalledWith(
      "/MasterListsPage/TestCatalogEditor/panel/1/basic-info",
    );
  });

  it("search narrows by name or LOINC", async () => {
    mockServer();
    wrap(<PanelsList />);
    await screen.findByText("Complete Blood Count");
    const searchBox = screen.getByTestId("panel-search");
    const input =
      searchBox.tagName === "INPUT"
        ? searchBox
        : searchBox.querySelector("input");
    fireEvent.change(input, { target: { value: "55204" } });
    expect(screen.queryByText("Complete Blood Count")).not.toBeInTheDocument();
    expect(screen.getByText("Thyroid Function Panel")).toBeInTheDocument();
  });

  it("a failed fetch shows the error state, not a silent empty list", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb(undefined));
    wrap(<PanelsList />);
    expect(
      await screen.findByText(messages["label.testCatalog.list.error"]),
    ).toBeInTheDocument();
  });
});

describe("filterPanels (pure)", () => {
  const panels = PANELS;
  it("matches name and LOINC case-insensitively", () => {
    expect(
      filterPanels(panels, { search: "blood", domain: "", status: "all" }),
    ).toHaveLength(1);
    expect(
      filterPanels(panels, { search: "58410", domain: "", status: "all" }),
    ).toHaveLength(1);
  });
  it("filters by domain and status", () => {
    expect(
      filterPanels(panels, { search: "", domain: "CLINICAL", status: "all" }),
    ).toHaveLength(2);
    expect(
      filterPanels(panels, { search: "", domain: "VECTOR", status: "all" }),
    ).toHaveLength(0);
    expect(
      filterPanels(panels, { search: "", domain: "", status: "active" }),
    ).toHaveLength(1);
    expect(
      filterPanels(panels, { search: "", domain: "", status: "inactive" }),
    ).toHaveLength(1);
  });
});
