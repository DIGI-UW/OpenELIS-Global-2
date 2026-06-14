/**
 * TestCatalogList — OGC-949 M3 / OGC-928 (RM10/RM11 polish).
 *
 * Validates the list screen's resilience + filter behavior:
 * - rows render from a successful fetch;
 * - a FAILED fetch shows an error state (not a silent empty list — the bug RM10 fixes);
 * - an empty result shows an empty state;
 * - filter state restores from the URL and is sent to the server (RM11 URL-sync + AMR).
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
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import TestCatalogList from "./TestCatalogList";
import { getFromOpenElisServer } from "../../utils/Utils";
import messages from "../../../languages/en.json";

// ========== HELPERS ==========
const renderList = () =>
  render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        <TestCatalogList />
      </IntlProvider>
    </BrowserRouter>,
  );

const pageOf = (rows) => ({ rows, total: rows.length });

beforeEach(() => {
  vi.clearAllMocks();
  mockHistory.location = { search: "" };
});

describe("TestCatalogList", () => {
  it("renders the rows returned by the server", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb(
        pageOf([
          {
            testId: "7",
            name: "Glucose",
            code: "GLU",
            domain: "CLINICAL",
            active: true,
          },
        ]),
      ),
    );
    renderList();
    expect(await screen.findByText("Glucose")).toBeInTheDocument();
  });

  it("shows an error state when the fetch fails (not a silent empty list)", async () => {
    // getFromOpenElisServer calls back with undefined on a failed fetch.
    getFromOpenElisServer.mockImplementation((url, cb) => cb(undefined));
    renderList();
    expect(
      await screen.findByText(messages["label.testCatalog.list.error"]),
    ).toBeInTheDocument();
  });

  it("shows an empty state when no tests match", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb(pageOf([])));
    renderList();
    expect(
      await screen.findByText(messages["label.testCatalog.list.empty"]),
    ).toBeInTheDocument();
  });

  it("restores filter state from the URL and sends it to the server", async () => {
    mockHistory.location = { search: "?amr=true&domain=CLINICAL" };
    let requestedUrl = "";
    getFromOpenElisServer.mockImplementation((url, cb) => {
      requestedUrl = url;
      cb(pageOf([]));
    });
    renderList();
    await waitFor(() => expect(getFromOpenElisServer).toHaveBeenCalled());
    expect(requestedUrl).toContain("amr=true");
    expect(requestedUrl).toContain("domain=CLINICAL");
  });
});
