/**
 * MethodsSection — OGC-949 M6 / OGC-750.
 *
 * Verifies the editor's Methods section: ported in M0, mounted into the editor
 * shell in M6. The network seam (Utils) is mocked; assertions are on rendered
 * DOM and captured request payloads.
 *
 * Scope split (deliberate, not theater): the link / inline-create / copy SUBMIT
 * payloads are contract-covered against a real DB by
 * TestMethodRestControllerIntegrationTest (13 tests). Those flows are gated
 * behind Carbon's flatpickr DatePicker / downshift ComboBox, which are not
 * reliably drivable in jsdom — driving them would produce flaky tests, not real
 * coverage. So this suite covers the genuinely frontend-reachable behavior:
 * rendering of linked methods (OGC-954), the set-default PATCH and remove DELETE
 * (OGC-956), and the inline-create form reveal (OGC-955).
 */

// ========== MOCKS (before imports) ==========
vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
  deleteFromOpenElisServer: vi.fn(),
  patchToOpenElisServerJsonResponse: vi.fn(),
}));

// ========== IMPORTS ==========
import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import MethodsSection from "./MethodsSection";
import {
  getFromOpenElisServer,
  deleteFromOpenElisServer,
  patchToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import messages from "../../../../languages/en.json";

const TEST_ID = "7";

// Two linked methods: L1 is the default (PCR), L2 is not (Culture).
const LINKS = [
  {
    id: "L1",
    methodId: "M1",
    methodName: "PCR",
    methodCode: "PCR01",
    isDefault: true,
    effectiveDate: "2026-01-01",
  },
  {
    id: "L2",
    methodId: "M2",
    methodName: "Culture",
    methodCode: "CUL01",
    isDefault: false,
    effectiveDate: "2026-02-02",
  },
];

// M3 is unlinked → available to link.
const ALL_METHODS = [
  { id: "M1", value: "PCR" },
  { id: "M2", value: "Culture" },
  { id: "M3", value: "Microscopy" },
];

const ALL_TESTS = [
  { id: "7", value: "This Test" },
  { id: "99", value: "Other Test" },
];

// Dispatch the three mount-time GETs by URL. `linksOverride` lets a test seed an
// empty list to exercise the empty state.
const seedServer = (linksOverride) => {
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url === `/rest/test/${TEST_ID}/methods`) {
      cb(linksOverride !== undefined ? linksOverride : LINKS);
    } else if (url === "/rest/displayList/METHODS") {
      cb(ALL_METHODS);
    } else if (url === "/rest/test-list") {
      cb(ALL_TESTS);
    }
  });
};

const renderSection = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <MethodsSection testId={TEST_ID} />
    </IntlProvider>,
  );

beforeEach(() => {
  vi.clearAllMocks();
  seedServer();
});

describe("MethodsSection", () => {
  it("renders the linked methods table with name, code, effective date, and default (OGC-954)", async () => {
    const { container } = renderSection();

    // Both linked methods render their name + code + effective date.
    expect(await screen.findByText("PCR")).toBeInTheDocument();
    expect(screen.getByText("PCR01")).toBeInTheDocument();
    expect(screen.getByText("2026-01-01")).toBeInTheDocument();
    expect(screen.getByText("Culture")).toBeInTheDocument();
    expect(screen.getByText("CUL01")).toBeInTheDocument();

    // The default radio reflects each link's isDefault flag.
    expect(container.querySelector("#default-L1")).toBeChecked();
    expect(container.querySelector("#default-L2")).not.toBeChecked();
  });

  it("shows the empty message when no methods are linked", async () => {
    seedServer([]);
    renderSection();
    expect(
      await screen.findByText(messages["admin.testCatalog.methods.empty"]),
    ).toBeInTheDocument();
  });

  it("loads links, the method master list, and the test list on mount", () => {
    renderSection();
    const urls = getFromOpenElisServer.mock.calls.map((c) => c[0]);
    expect(urls).toContain(`/rest/test/${TEST_ID}/methods`);
    expect(urls).toContain("/rest/displayList/METHODS");
    expect(urls).toContain("/rest/test-list");
  });

  it("sets a non-default method as default via PATCH, carrying its effective date (OGC-956)", async () => {
    const { container } = renderSection();
    await screen.findByText("Culture");

    // Click the non-default link's radio.
    fireEvent.click(container.querySelector("#default-L2"));

    expect(patchToOpenElisServerJsonResponse).toHaveBeenCalledTimes(1);
    const [url, body] = patchToOpenElisServerJsonResponse.mock.calls[0];
    expect(url).toBe(`/rest/test/${TEST_ID}/methods/L2`);
    const payload = JSON.parse(body);
    expect(payload.isDefault).toBe(true);
    // The existing effective date is preserved (the API requires it on PATCH).
    expect(payload.effectiveDate).toBe("2026-02-02");
  });

  it("removes a link via DELETE to the link's URL", async () => {
    renderSection();
    await screen.findByText("PCR");

    const removeButtons = screen.getAllByRole("button", {
      name: messages["admin.testCatalog.methods.action.remove"],
    });
    fireEvent.click(removeButtons[0]); // first row → L1

    expect(deleteFromOpenElisServer).toHaveBeenCalledTimes(1);
    expect(deleteFromOpenElisServer.mock.calls[0][0]).toBe(
      `/rest/test/${TEST_ID}/methods/L1`,
    );
  });

  it("reveals the inline create-method form when 'Create New Method' is clicked (OGC-955)", async () => {
    renderSection();
    await screen.findByText("PCR");

    // The inline form is conditionally rendered — absent until requested.
    expect(
      screen.queryByLabelText(
        messages["admin.testCatalog.methods.inline.nameEnglish"],
      ),
    ).not.toBeInTheDocument();

    fireEvent.click(
      screen.getByRole("button", {
        name: messages["admin.testCatalog.methods.btn.createMethod"],
      }),
    );

    expect(
      screen.getByLabelText(
        messages["admin.testCatalog.methods.inline.nameEnglish"],
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByLabelText(messages["admin.testCatalog.methods.inline.code"]),
    ).toBeInTheDocument();
  });
});
