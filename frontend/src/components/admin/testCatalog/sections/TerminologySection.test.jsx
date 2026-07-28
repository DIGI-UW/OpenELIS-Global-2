/**
 * TerminologySection — OGC-949 M10 / OGC-957..958.
 *
 * Covers: loading existing mappings, adding a mapping via the inline form +
 * saving with the payload captured, removing a mapping, and the error state.
 */

// ========== MOCKS (before imports) ==========
vi.mock("../../../layout/Layout", async () => {
  const React = await import("react");
  return {
    NotificationContext: React.createContext({
      addNotification: () => {},
      setNotificationVisible: () => {},
    }),
  };
});

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  putToOpenElisServer: vi.fn(),
}));

// ========== IMPORTS ==========
import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import TerminologySection from "./TerminologySection";
import {
  getFromOpenElisServer,
  putToOpenElisServer,
} from "../../../utils/Utils";
import messages from "../../../../languages/en.json";

const renderSection = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <TerminologySection testId="42" />
    </IntlProvider>,
  );

beforeEach(() => {
  vi.clearAllMocks();
  putToOpenElisServer.mockImplementation((url, payload, cb) => cb(200));
});

describe("TerminologySection", () => {
  it("offers the per-specimen override picker only for multi-specimen tests (OGC-1145 FR-13)", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb({
        testId: "42",
        mappings: [
          {
            id: "a",
            source: "LOINC",
            code: "2345-7",
            relationship: "SAME_AS",
            sampleTypeId: null,
          },
        ],
        sampleTypes: [
          { id: "2", name: "Serum" },
          { id: "9", name: "CSF" },
        ],
      }),
    );
    const { container } = renderSection();
    await screen.findByTestId("mapping-row-a");
    // The Specimen column renders with the shared default on the row.
    expect(
      screen.getAllByText(messages["label.testCatalog.override.shared"]).length,
    ).toBeGreaterThan(0);

    // Scope the mapping to CSF and save — the payload carries the scope.
    fireEvent.click(screen.getByTestId("edit-mapping-0"));
    fireEvent.change(container.querySelector("#mapping-sample-type-0"), {
      target: { value: "9" },
    });
    fireEvent.click(
      screen.getByRole("button", { name: messages["label.button.save"] }),
    );
    await waitFor(() => expect(putToOpenElisServer).toHaveBeenCalled());
    const payload = JSON.parse(putToOpenElisServer.mock.calls[0][1]);
    expect(payload.mappings[0].sampleTypeId).toBe("9");
  });

  it("loads existing mappings read-only, editable only after clicking Edit", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb({
        testId: "42",
        mappings: [
          { id: "a", source: "LOINC", code: "1558-6", relationship: "SAME_AS" },
        ],
      }),
    );
    const { container } = renderSection();
    await screen.findByTestId("mapping-row-a");
    // Read-only by default: the code renders as text, not an input.
    expect(screen.getByText("1558-6")).toBeInTheDocument();
    expect(container.querySelector("#mapping-code-0")).toBeNull();

    // Clicking Edit reveals the editable controls for that row.
    fireEvent.click(screen.getByTestId("edit-mapping-0"));
    expect(container.querySelector("#mapping-source-0").value).toBe("LOINC");
    expect(container.querySelector("#mapping-code-0").value).toBe("1558-6");
    expect(container.querySelector("#mapping-rel-0").value).toBe("SAME_AS");
  });

  it("adds a mapping via the form + Add mapping and saves it", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb({ testId: "42", mappings: [] }),
    );
    renderSection();
    await screen.findByText(messages["label.testCatalog.terminology.empty"]);

    fireEvent.change(
      screen.getByLabelText(messages["label.testCatalog.terminology.source"]),
      { target: { value: "LOINC" } },
    );
    fireEvent.change(
      screen.getByLabelText(messages["label.testCatalog.terminology.code"]),
      { target: { value: "1558-6" } },
    );
    fireEvent.change(
      screen.getByLabelText(
        messages["label.testCatalog.terminology.relationship"],
      ),
      { target: { value: "SAME_AS" } },
    );
    fireEvent.click(
      screen.getByRole("button", {
        name: messages["label.testCatalog.terminology.addMapping"],
      }),
    );
    // Row now present (read-only display).
    expect(screen.getByText("1558-6")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await waitFor(() => expect(putToOpenElisServer).toHaveBeenCalled());
    const mappings = JSON.parse(putToOpenElisServer.mock.calls[0][1]).mappings;
    expect(mappings).toEqual([
      {
        id: null,
        source: "LOINC",
        code: "1558-6",
        relationship: "SAME_AS",
        displayName: null,
        componentId: null,
        sampleTypeId: null,
      },
    ]);
  });

  it("saves a filled draft on Save even without clicking Add mapping", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb({ testId: "42", mappings: [] }),
    );
    renderSection();
    await screen.findByText(messages["label.testCatalog.terminology.empty"]);

    fireEvent.change(
      screen.getByLabelText(messages["label.testCatalog.terminology.source"]),
      { target: { value: "SNOMED" } },
    );
    fireEvent.change(
      screen.getByLabelText(messages["label.testCatalog.terminology.code"]),
      { target: { value: "12345" } },
    );
    // Straight to Save — no "Add mapping" click first.
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await waitFor(() => expect(putToOpenElisServer).toHaveBeenCalled());
    expect(JSON.parse(putToOpenElisServer.mock.calls[0][1]).mappings).toEqual([
      {
        id: null,
        source: "SNOMED",
        code: "12345",
        relationship: "SAME_AS",
        displayName: null,
        componentId: null,
        sampleTypeId: null,
      },
    ]);
  });

  it("edits an existing mapping in place and persists the change", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb({
        testId: "42",
        mappings: [
          { id: "a", source: "LOINC", code: "1558-6", relationship: "SAME_AS" },
        ],
      }),
    );
    const { container } = renderSection();
    await screen.findByTestId("mapping-row-a");

    // Fields are editable only after clicking Edit.
    fireEvent.click(screen.getByTestId("edit-mapping-0"));
    fireEvent.change(container.querySelector("#mapping-code-0"), {
      target: { value: "9999-9" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await waitFor(() => expect(putToOpenElisServer).toHaveBeenCalled());
    expect(JSON.parse(putToOpenElisServer.mock.calls[0][1]).mappings).toEqual([
      {
        id: "a",
        source: "LOINC",
        code: "9999-9",
        relationship: "SAME_AS",
        displayName: null,
        componentId: null,
        sampleTypeId: null,
      },
    ]);
  });

  it("refuses to save a duplicate (source, code) mapping instead of PUTting", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb({
        testId: "42",
        mappings: [
          { id: "a", source: "LOINC", code: "1558-6", relationship: "SAME_AS" },
        ],
      }),
    );
    renderSection();
    await screen.findByTestId("mapping-row-a");

    // Draft the same (source, code) again and try to save straight away.
    fireEvent.change(
      screen.getByLabelText(messages["label.testCatalog.terminology.source"]),
      { target: { value: "LOINC" } },
    );
    fireEvent.change(
      screen.getByLabelText(messages["label.testCatalog.terminology.code"]),
      { target: { value: "1558-6" } },
    );
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    // The duplicate is caught client-side with a specific warning — no PUT.
    expect(putToOpenElisServer).not.toHaveBeenCalled();
  });

  it("removes a mapping so it drops out of the saved payload", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb({
        testId: "42",
        mappings: [
          { id: "a", source: "LOINC", code: "1558-6", relationship: "SAME_AS" },
        ],
      }),
    );
    renderSection();
    await screen.findByTestId("mapping-row-a");
    fireEvent.click(
      screen.getByRole("button", {
        name: messages["label.testCatalog.terminology.remove"],
      }),
    );
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await waitFor(() => expect(putToOpenElisServer).toHaveBeenCalled());
    expect(JSON.parse(putToOpenElisServer.mock.calls[0][1]).mappings).toEqual(
      [],
    );
  });

  it("shows an error state when the fetch fails", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb(undefined));
    renderSection();
    expect(
      await screen.findByText(
        messages["label.testCatalog.terminology.loadError"],
      ),
    ).toBeInTheDocument();
  });

  it("shows the LOINC integrity warnings (no-LOINC + duplicate) from the endpoint", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.includes("/loinc-integrity")) {
        cb({
          loinc: "1558-6",
          active: true,
          noLoinc: true,
          duplicates: [{ testId: "9", name: "Glucose (Serum)" }],
        });
      } else {
        cb({ testId: "42", mappings: [] });
      }
    });
    renderSection();
    expect(await screen.findByTestId("no-loinc-warning")).toBeInTheDocument();
    expect(screen.getByTestId("duplicate-loinc-warning")).toBeInTheDocument();
  });
});
