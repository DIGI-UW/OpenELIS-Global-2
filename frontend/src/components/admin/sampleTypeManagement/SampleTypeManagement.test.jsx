import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import { vi } from "vitest";
import messages from "../../../languages/en.json";
import SampleTypeManagement from "./SampleTypeManagement";

const api = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  delete: vi.fn(),
  postRaw: vi.fn(),
}));

vi.mock("../../common/useDomains", () => ({
  default: () => [
    { id: "CLINICAL", labelKey: "label.sampleType.domain.clinical" },
  ],
}));

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: api.get,
  postToOpenElisServerJsonResponse: api.post,
  putToOpenElisServer: api.put,
  deleteFromOpenElisServer: api.delete,
  postToOpenElisServer: api.postRaw,
}));
const mockIntl = {
  formatMessage: ({ id, defaultMessage }) => defaultMessage || id,
};

// The editor is URL-driven: /MasterListsPage/SampleTypeEditor/:sampleTypeId?/:section?
// Wrap in a matching Route so useParams() sees the current segment when the
// user navigates from the list to the editor.
const sampleType = {
  id: "sample-type-2",
  name: "Blood culture",
  description: "Blood culture specimen",
  domain: "CLINICAL",
  isActive: true,
  testCount: 1,
  whonetCode: "",
};

const renderPage = (initialEntry = "/MasterListsPage/SampleTypeEditor") =>
  render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <IntlProvider locale="en" messages={messages}>
        <Route
          path="/MasterListsPage/SampleTypeEditor/:sampleTypeId?/:section?"
          render={() => <SampleTypeManagement intl={mockIntl} />}
        />
        <Route
          render={({ location }) => (
            <output data-testid="sample-type-current-url">
              {location.pathname}
              {location.search}
            </output>
          )}
        />
      </IntlProvider>
    </MemoryRouter>,
  );

describe("SampleTypeManagement", () => {
  beforeEach(() => {
    api.get.mockReset();
    api.post.mockReset();
    api.put.mockReset();
    api.delete.mockReset();
    api.postRaw.mockReset();
    api.get.mockImplementation((url, callback) => {
      if (url === "/rest/sample-types") {
        callback({ success: true, data: [sampleType] });
      } else if (url === "/rest/sample-types/sample-type-2") {
        callback({
          success: true,
          data: sampleType,
        });
      } else if (url.startsWith("/rest/AllTestsForSampleTypeProvider")) {
        callback({ tests: [] });
      } else {
        callback({});
      }
    });
    api.put.mockImplementation((_url, _body, callback) => callback(200));
  });

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

  test("focuses, saves, and returns from an exact WHONET specimen repair link", async () => {
    const user = userEvent.setup();
    const returnTo =
      "/Microbiology/whonet?from=2026-07-01&to=2026-07-31&significance=CLINICALLY_SIGNIFICANT&dedup=FIRST_ISOLATE_7_DAY&step=preview&page=2&pageSize=50";
    renderPage(
      `/MasterListsPage/SampleTypeEditor/sample-type-2/basic-info?focus=whonet&returnTo=${encodeURIComponent(returnTo)}`,
    );

    const codeInput = await screen.findByLabelText("WHONET specimen code");
    expect(codeInput).toHaveFocus();
    expect(
      screen.queryByRole("link", { name: "Return to WHONET preview" }),
    ).not.toBeInTheDocument();

    await user.type(codeInput, "BLD");
    await user.click(screen.getByRole("button", { name: "Save" }));

    await waitFor(() => expect(api.put).toHaveBeenCalledTimes(1));
    expect(api.put.mock.calls[0][0]).toBe("/rest/sample-types/sample-type-2");
    expect(JSON.parse(api.put.mock.calls[0][1])).toMatchObject({
      id: "sample-type-2",
      whonetCode: "BLD",
    });

    const returnLink = await screen.findByRole("link", {
      name: "Return to WHONET preview",
    });
    expect(returnLink).toHaveAttribute("href", returnTo);
    await user.click(returnLink);
    expect(screen.getByTestId("sample-type-current-url")).toHaveTextContent(
      returnTo,
    );
  });

  // OGC-1234 — a create refused by bean validation (400) must not read as a
  // successful save: the editor stays on the add form and names the field.
  test("a refused create (400) keeps the add form open and marks the name", async () => {
    api.post.mockImplementation((_url, _body, callback) =>
      callback({
        error: "validation",
        fieldErrors: [{ field: "sampleTypeEnglishName", defaultMessage: "" }],
        status: 400,
      }),
    );
    renderPage("/MasterListsPage/SampleTypeEditor/new/basic-info");

    const name = await screen.findByRole("textbox", { name: /Name/ });
    await userEvent.type(name, "QA<b>RV</b>0923");
    await userEvent.type(
      document.getElementById("st-description"),
      "QA markup probe",
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Create Sample Type" }),
    );

    await waitFor(() => expect(api.post).toHaveBeenCalledTimes(1));
    expect(api.post.mock.calls[0][0]).toBe("/rest/SampleTypeCreate");
    const refusal = messages["error.sampleType.create.invalidName"];
    expect(
      await screen.findByText(`Failed to create sample type: ${refusal}`),
    ).toBeInTheDocument();
    expect(screen.getByText(refusal)).toBeInTheDocument();
    expect(name).toHaveAttribute("aria-invalid", "true");
    expect(name).toHaveValue("QA<b>RV</b>0923");
    expect(screen.getByTestId("sample-type-current-url")).toHaveTextContent(
      "/MasterListsPage/SampleTypeEditor/new/basic-info",
    );
    expect(screen.queryByText("Sample type saved successfully.")).toBeNull();
  });

  test("after a refused create, correcting the name re-enables Create and the create goes through", async () => {
    api.post
      .mockImplementationOnce((_url, _body, callback) =>
        callback({
          error: "validation",
          fieldErrors: [{ field: "sampleTypeEnglishName", defaultMessage: "" }],
          status: 400,
        }),
      )
      .mockImplementationOnce((_url, _body, callback) => callback({}));
    renderPage("/MasterListsPage/SampleTypeEditor/new/basic-info");

    const name = await screen.findByRole("textbox", { name: /Name/ });
    await userEvent.type(name, "QA<b>RV</b>0923");
    await userEvent.type(
      document.getElementById("st-description"),
      "QA markup probe",
    );
    const create = screen.getByRole("button", { name: "Create Sample Type" });
    await userEvent.click(create);
    await screen.findByText(
      `Failed to create sample type: ${messages["error.sampleType.create.invalidName"]}`,
    );

    await userEvent.clear(name);
    await userEvent.type(name, "QA Plain 0923");
    expect(name).not.toHaveAttribute("aria-invalid", "true");
    expect(create).toBeEnabled();
    await userEvent.click(create);

    await waitFor(() => expect(api.post).toHaveBeenCalledTimes(2));
    expect(JSON.parse(api.post.mock.calls[1][1]).sampleTypeEnglishName).toBe(
      "QA Plain 0923",
    );
  });

  test("a create opened straight on /new lands on the new sample type, not the first listed one", async () => {
    const created = {
      id: "sample-type-9",
      name: "QA Plain 0923",
      description: "QA plain probe",
      domain: "CLINICAL",
      isActive: false,
      testCount: 0,
    };
    let posted = false;
    // The page's own list read is still in flight when the admin clicks Create
    // (a deep link to /new with a long list), so it never answers in time.
    const pendingListReads = [];
    let clicked = false;
    api.get.mockImplementation((url, callback) => {
      if (url === "/rest/sample-types") {
        const answer = () =>
          callback({
            success: true,
            data: posted ? [sampleType, created] : [sampleType],
          });
        if (!clicked) {
          pendingListReads.push(answer);
        } else {
          answer();
        }
      } else if (url === "/rest/sample-types/sample-type-9") {
        callback({ success: true, data: created });
      } else if (url.startsWith("/rest/AllTestsForSampleTypeProvider")) {
        callback({ tests: [] });
      } else {
        callback({});
      }
    });
    api.post.mockImplementation((_url, _body, callback) => {
      posted = true;
      callback({});
    });
    renderPage("/MasterListsPage/SampleTypeEditor/new/basic-info");

    await userEvent.type(
      await screen.findByRole("textbox", { name: /Name/ }),
      "QA Plain 0923",
    );
    await userEvent.type(
      document.getElementById("st-description"),
      "QA plain probe",
    );
    const create = screen.getByRole("button", { name: "Create Sample Type" });
    clicked = true;
    await userEvent.click(create);

    await waitFor(() =>
      expect(screen.getByTestId("sample-type-current-url")).toHaveTextContent(
        "/MasterListsPage/SampleTypeEditor/sample-type-9/",
      ),
    );
  });
});
