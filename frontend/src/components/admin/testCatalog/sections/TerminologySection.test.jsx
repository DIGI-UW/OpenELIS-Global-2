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
import { fireEvent, render, screen, within } from "@testing-library/react";
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
  it("loads and renders existing mappings", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb({
        testId: "42",
        mappings: [
          { id: "a", source: "LOINC", code: "1558-6", relationship: "SAME_AS" },
        ],
      }),
    );
    renderSection();
    expect(await screen.findByText("1558-6")).toBeInTheDocument();
    // Source renders as a Tag in the row (LOINC also appears in the add-form
    // Select, so scope to the row to disambiguate).
    const row = screen.getByTestId("mapping-row-a");
    expect(within(row).getByText("LOINC")).toBeInTheDocument();
  });

  it("adds a mapping via the form and saves it with the payload captured", async () => {
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
    // Row now present.
    expect(screen.getByText("1558-6")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await waitFor(() => expect(putToOpenElisServer).toHaveBeenCalled());
    const mappings = JSON.parse(putToOpenElisServer.mock.calls[0][1]).mappings;
    expect(mappings).toEqual([
      { id: null, source: "LOINC", code: "1558-6", relationship: "SAME_AS" },
    ]);
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
    await screen.findByText("1558-6");
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
