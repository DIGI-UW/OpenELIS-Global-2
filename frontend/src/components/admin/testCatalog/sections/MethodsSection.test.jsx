/**
 * MethodsSection — OGC-949 M6 / OGC-750.
 *
 * Verifies the editor's Methods section: ported in M0, mounted into the editor
 * shell in M6. The network seam (Utils) is mocked; assertions are on rendered
 * DOM and captured request payloads.
 *
 * The link / inline-create / copy SUBMIT payloads are contract-covered against a
 * real DB by TestMethodRestControllerIntegrationTest. Carbon's flatpickr
 * DatePicker is not drivable in jsdom, so it is replaced below by a plain input
 * that reports the typed date the way DatePicker's onChange does; that lets this
 * suite assert what each submit does with the server's answer (OGC-1234: a
 * refusal must never read as success).
 */

// ========== MOCKS (before imports) ==========
vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
  deleteFromOpenElisServer: vi.fn(),
  patchToOpenElisServerJsonResponse: vi.fn(),
}));

vi.mock("@carbon/react", async (importOriginal) => {
  const React = await import("react");
  const actual = await importOriginal();
  const DatePicker = ({ onChange, children }) =>
    React.Children.map(children, (child) =>
      React.cloneElement(child, { onDateChange: onChange }),
    );
  const DatePickerInput = ({ id, labelText, onDateChange }) => (
    <>
      <label htmlFor={id}>{labelText}</label>
      <input
        id={id}
        onChange={(e) => onDateChange([new Date(`${e.target.value}T00:00:00`)])}
      />
    </>
  );
  return { ...actual, DatePicker, DatePickerInput };
});

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
  postToOpenElisServerJsonResponse,
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

  // ── OGC-1234: every submit reports what the server actually did ──────────

  const openInlineCreate = async (container) => {
    await screen.findByText("PCR");
    fireEvent.click(
      screen.getByRole("button", {
        name: messages["admin.testCatalog.methods.btn.createMethod"],
      }),
    );
    fireEvent.change(
      screen.getByLabelText(
        messages["admin.testCatalog.methods.inline.nameEnglish"],
      ),
      { target: { value: "QA Method" } },
    );
    fireEvent.change(
      screen.getByLabelText(
        messages["admin.testCatalog.methods.inline.nameFrench"],
      ),
      { target: { value: "QA Methode" } },
    );
    fireEvent.change(
      screen.getByLabelText(messages["admin.testCatalog.methods.inline.code"]),
      { target: { value: "QAM0923" } },
    );
    fireEvent.change(container.querySelector("#inline-effective-date"), {
      target: { value: "2026-09-24" },
    });
  };
  const createAndLinkButton = () =>
    screen.getByRole("button", {
      name: messages["admin.testCatalog.methods.inline.createAndLink"],
    });

  it("a duplicate method code (409) shows an error, keeps the form open and never shows success", async () => {
    postToOpenElisServerJsonResponse.mockImplementation((url, body, cb) =>
      cb({ error: "Request failed (HTTP 409 )", status: 409 }),
    );
    const { container } = renderSection();
    await openInlineCreate(container);
    fireEvent.click(createAndLinkButton());

    expect(postToOpenElisServerJsonResponse.mock.calls[0][0]).toBe(
      `/rest/test/${TEST_ID}/methods/inline-create`,
    );
    expect(
      JSON.parse(postToOpenElisServerJsonResponse.mock.calls[0][1]).code,
    ).toBe("QAM0923");
    const duplicate = messages["admin.testCatalog.methods.error.duplicateCode"];
    expect(screen.getAllByText(duplicate).length).toBe(2);
    expect(createAndLinkButton()).toBeInTheDocument();
    expect(
      screen.getByLabelText(messages["admin.testCatalog.methods.inline.code"]),
    ).toHaveValue("QAM0923");
    expect(
      screen.queryByText(messages["admin.testCatalog.methods.created"]),
    ).toBeNull();
  });

  it("a server failure on create shows the server error, not success", async () => {
    postToOpenElisServerJsonResponse.mockImplementation((url, body, cb) =>
      cb({ error: "Request failed (HTTP 500 )", status: 500 }),
    );
    const { container } = renderSection();
    await openInlineCreate(container);
    fireEvent.click(createAndLinkButton());

    expect(
      screen.getByText(messages["admin.testCatalog.methods.error.server"]),
    ).toBeInTheDocument();
    expect(createAndLinkButton()).toBeInTheDocument();
  });

  it("a created method (201) closes the form, reloads the links and says it was created", async () => {
    postToOpenElisServerJsonResponse.mockImplementation((url, body, cb) =>
      cb({ id: "L9", methodId: "M9", methodName: "QA Method" }),
    );
    const { container } = renderSection();
    await openInlineCreate(container);
    getFromOpenElisServer.mockClear();
    fireEvent.click(createAndLinkButton());

    expect(
      screen.getByText(messages["admin.testCatalog.methods.created"]),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", {
        name: messages["admin.testCatalog.methods.inline.createAndLink"],
      }),
    ).toBeNull();
    expect(getFromOpenElisServer.mock.calls.map((c) => c[0])).toContain(
      `/rest/test/${TEST_ID}/methods`,
    );
  });

  const submitLinkModal = async (container) => {
    await screen.findByText("PCR");
    fireEvent.click(
      screen.getAllByRole("button", {
        name: messages["admin.testCatalog.methods.btn.linkMethod"],
      })[0],
    );
    fireEvent.change(container.querySelector("#link-method-select"), {
      target: { value: "Micro" },
    });
    fireEvent.click(await screen.findByText("Microscopy"));
    fireEvent.change(container.querySelector("#link-effective-date"), {
      target: { value: "2026-09-24" },
    });
    const dialog = screen.getByRole("dialog");
    fireEvent.click(
      Array.from(dialog.querySelectorAll("button")).find(
        (b) =>
          b.textContent ===
          messages["admin.testCatalog.methods.btn.linkMethod"],
      ),
    );
  };

  it("a refused link (409) reports the method is already linked, not success", async () => {
    postToOpenElisServerJsonResponse.mockImplementation((url, body, cb) =>
      cb({ error: "Request failed (HTTP 409 )", status: 409 }),
    );
    const { container } = renderSection();
    await submitLinkModal(container);

    expect(
      JSON.parse(postToOpenElisServerJsonResponse.mock.calls[0][1]).methodId,
    ).toBe("M3");
    expect(
      screen.getByText(
        messages["admin.testCatalog.methods.error.duplicateLink"],
      ),
    ).toBeInTheDocument();
    expect(
      screen.queryByText(messages["admin.testCatalog.methods.linked"]),
    ).toBeNull();
  });

  it("a successful link (201) says the method was linked", async () => {
    postToOpenElisServerJsonResponse.mockImplementation((url, body, cb) =>
      cb({ id: "L3", methodId: "M3", methodName: "Microscopy" }),
    );
    const { container } = renderSection();
    await submitLinkModal(container);

    expect(
      screen.getByText(messages["admin.testCatalog.methods.linked"]),
    ).toBeInTheDocument();
  });

  const copyFromOtherTest = async (container) => {
    await screen.findByText("PCR");
    fireEvent.change(container.querySelector("#copy-from-test"), {
      target: { value: "Other" },
    });
    fireEvent.click(await screen.findByText("Other Test"));
    fireEvent.click(
      screen.getByRole("button", {
        name: messages["admin.testCatalog.methods.btn.copyFromTest"],
      }),
    );
  };

  it("copying from a test with nothing new to copy says so, as info, not success", async () => {
    postToOpenElisServerJsonResponse.mockImplementation((url, body, cb) =>
      cb(LINKS),
    );
    const { container } = renderSection();
    await copyFromOtherTest(container);

    expect(postToOpenElisServerJsonResponse.mock.calls[0][0]).toBe(
      `/rest/test/${TEST_ID}/methods/copyFrom/99`,
    );
    expect(
      screen.getByText(
        "No methods copied: Other Test has no methods that are not already linked to this test.",
      ),
    ).toBeInTheDocument();
    expect(container.querySelector("#copy-from-test")).toHaveValue("");
  });

  it("copying new methods reports how many were copied and shows them", async () => {
    postToOpenElisServerJsonResponse.mockImplementation((url, body, cb) =>
      cb([
        ...LINKS,
        {
          id: "L3",
          methodId: "M3",
          methodName: "Microscopy",
          methodCode: "MIC01",
          isDefault: false,
          effectiveDate: "2026-09-24",
        },
      ]),
    );
    const { container } = renderSection();
    await copyFromOtherTest(container);

    expect(
      screen.getByText("1 method copied from Other Test."),
    ).toBeInTheDocument();
    expect(screen.getByText("MIC01")).toBeInTheDocument();
  });

  it("the same source test can be picked and copied again after a copy", async () => {
    postToOpenElisServerJsonResponse.mockImplementation((url, body, cb) =>
      cb(LINKS),
    );
    const { container } = renderSection();
    await copyFromOtherTest(container);
    expect(container.querySelector("#copy-from-test")).toHaveValue("");

    fireEvent.change(container.querySelector("#copy-from-test"), {
      target: { value: "Other" },
    });
    fireEvent.click(await screen.findByText("Other Test"));
    const copyButton = screen.getByRole("button", {
      name: messages["admin.testCatalog.methods.btn.copyFromTest"],
    });
    expect(copyButton).toBeEnabled();
    fireEvent.click(copyButton);

    expect(postToOpenElisServerJsonResponse).toHaveBeenCalledTimes(2);
  });

  it("a failed copy reports an error, not success", async () => {
    postToOpenElisServerJsonResponse.mockImplementation((url, body, cb) =>
      cb({ error: "Request failed (HTTP 500 )", status: 500 }),
    );
    const { container } = renderSection();
    await copyFromOtherTest(container);

    expect(
      screen.getByText(messages["admin.testCatalog.methods.error.server"]),
    ).toBeInTheDocument();
  });
});
