import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServer: vi.fn(),
  putToOpenElisServer: vi.fn(),
  deleteFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
}));

import { getFromOpenElisServer } from "../../utils/Utils";
import SampleTypeManagement from "./SampleTypeManagement";

/** One sample type, so the list has a row to click. */
const SAMPLE_TYPES = [
  {
    id: "5",
    name: "Serum",
    description: "Serum specimen",
    domain: "CLINICAL",
    isActive: true,
    testCount: 3,
  },
];

beforeEach(() => {
  getFromOpenElisServer.mockReset();
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (typeof cb !== "function") return;
    if (url.startsWith("/rest/sample-types")) {
      cb({ success: true, data: SAMPLE_TYPES });
      return;
    }
    cb([]);
  });
});

const mockIntl = {
  formatMessage: ({ id, defaultMessage }) => defaultMessage || id,
};

// The editor is URL-driven: /MasterListsPage/SampleTypeEditor/:sampleTypeId?/:section?
// Wrap in a matching Route so useParams() sees the current segment when the
// user navigates from the list to the editor.
const renderPage = () =>
  render(
    <MemoryRouter initialEntries={["/MasterListsPage/SampleTypeEditor"]}>
      <IntlProvider locale="en" messages={{}}>
        <Route
          path="/MasterListsPage/SampleTypeEditor/:sampleTypeId?/:section?"
          render={() => <SampleTypeManagement intl={mockIntl} />}
        />
      </IntlProvider>
    </MemoryRouter>,
  );

describe("SampleTypeManagement", () => {
  test("renders sample type management page after loading", async () => {
    renderPage();

    // Header and Add button only mount once the async fetch settles
    // (component starts with isLoading=true).
    expect(await screen.findByText("Sample Type Editor")).toBeInTheDocument();
    expect(screen.getByText("Add Sample Type")).toBeInTheDocument();
  });

  test("opens add sample type form when Add button is clicked", async () => {
    renderPage();

    const addButton = await screen.findByText("Add Sample Type");
    fireEvent.click(addButton);

    expect(screen.getByText("Add New Sample Type")).toBeInTheDocument();
  });

  test("can navigate back to list from add form", async () => {
    renderPage();

    const addButton = await screen.findByText("Add Sample Type");
    fireEvent.click(addButton);

    const backButton = screen.getByText("← Back to List");
    fireEvent.click(backButton);

    expect(screen.getByText("Sample Type Editor")).toBeInTheDocument();
    expect(screen.getByText("Add Sample Type")).toBeInTheDocument();
  });

  test("opens the editor when a sample type row is clicked", async () => {
    // Tests and panels open from the row; sample types required the Edit
    // button, which made the list behave unlike its two peers.
    renderPage();
    await screen.findByText("Sample Type Editor");

    const rows = document.querySelectorAll('[data-cy^="sampleType-row-"]');
    expect(
      rows.length,
      "the fixture must render at least one sample type row",
    ).toBeGreaterThan(0);

    const firstDataCell = rows[0].querySelector("td");
    expect(firstDataCell.style.cursor).toBe("pointer");
    fireEvent.click(firstDataCell);

    // Leaving the list is what opening the editor looks like from here.
    expect(screen.queryByText("Add Sample Type")).not.toBeInTheDocument();
  });

  test("keeps the row reachable from the keyboard", async () => {
    renderPage();
    await screen.findByText("Sample Type Editor");

    const row = document.querySelector('[data-cy^="sampleType-row-"]');
    expect(row.getAttribute("tabindex")).toBe("0");

    fireEvent.keyDown(row, { key: "Enter" });
    expect(screen.queryByText("Add Sample Type")).not.toBeInTheDocument();
  });
});
