/**
 * BasicInfoSection — OGC-949 M4 / OGC-748.
 *
 * Validates the Domain-switch confirmation modal (US4 AC#1, fix M-04): changing
 * the Domain radio does not apply immediately — it opens a confirmation modal;
 * confirming applies the change, cancelling reverts to the current domain.
 */

// ========== MOCKS (before imports) ==========
// Factory must be self-contained (hoisted above imports) — no outer refs.
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
import BasicInfoSection from "./BasicInfoSection";
import {
  getFromOpenElisServer,
  putToOpenElisServer,
} from "../../../utils/Utils";
import messages from "../../../../languages/en.json";

const renderSection = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <BasicInfoSection testId="42" />
    </IntlProvider>,
  );

// The domain the section persists on Save is the source of truth for whether
// the modal applied or reverted the change — assert on that rather than on
// Carbon's controlled-radio checked state (unreliable to read in jsdom).
const savedDomain = () =>
  JSON.parse(putToOpenElisServer.mock.calls[0][1]).domain;

beforeEach(() => {
  vi.clearAllMocks();
  getFromOpenElisServer.mockImplementation((url, cb) =>
    cb({
      name: "Glucose",
      code: "GLU",
      description: "",
      domain: "CLINICAL",
      antimicrobialResistance: false,
      active: true,
      orderable: true,
    }),
  );
  putToOpenElisServer.mockImplementation((url, payload, cb) => cb(200));
});

describe("BasicInfoSection domain-switch modal", () => {
  it("confirming a domain change persists the new domain", async () => {
    renderSection();
    await screen.findByLabelText("Clinical");

    fireEvent.click(screen.getByLabelText("Environmental"));
    fireEvent.click(screen.getByRole("button", { name: "Confirm" }));
    fireEvent.click(screen.getByRole("button", { name: "Save" }));

    await waitFor(() => expect(putToOpenElisServer).toHaveBeenCalled());
    expect(savedDomain()).toBe("ENVIRONMENTAL");
  });

  it("cancelling a domain change reverts the radio and keeps the saved domain", async () => {
    renderSection();
    await screen.findByLabelText("Clinical");

    fireEvent.click(screen.getByLabelText("Environmental"));
    fireEvent.click(screen.getByRole("button", { name: "Cancel" }));

    // The radio must snap back to the current domain — not stay visually stuck
    // on the rejected choice (Carbon RadioButtonGroup internal-state desync).
    await waitFor(() =>
      expect(screen.getByLabelText("Clinical")).toBeChecked(),
    );
    expect(screen.getByLabelText("Environmental")).not.toBeChecked();

    // ...and a subsequent Save persists the unchanged domain.
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await waitFor(() => expect(putToOpenElisServer).toHaveBeenCalled());
    expect(savedDomain()).toBe("CLINICAL");
  });

  it("persists the AMR toggle", async () => {
    renderSection();
    await screen.findByLabelText("Clinical");
    // AMR starts false in the loaded form; flip it on.
    fireEvent.click(screen.getByRole("switch", { name: /AMR surveillance/ }));
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await waitFor(() => expect(putToOpenElisServer).toHaveBeenCalled());
    expect(
      JSON.parse(putToOpenElisServer.mock.calls[0][1]).antimicrobialResistance,
    ).toBe(true);
  });

  it("persists the Active toggle (boolean → Y/N)", async () => {
    renderSection();
    await screen.findByLabelText("Clinical");
    // Active starts true in the loaded form; flip it off.
    fireEvent.click(screen.getByRole("switch", { name: /Active/ }));
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await waitFor(() => expect(putToOpenElisServer).toHaveBeenCalled());
    expect(JSON.parse(putToOpenElisServer.mock.calls[0][1]).active).toBe(false);
  });

  it("shows an error state when the fetch fails", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb(undefined));
    renderSection();
    expect(
      await screen.findByText(messages["label.testCatalog.editor.loadError"]),
    ).toBeInTheDocument();
  });
});
