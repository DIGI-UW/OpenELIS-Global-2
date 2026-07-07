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
  postToOpenElisServerJsonResponse: vi.fn(),
  postToOpenElisServerFullResponse: vi.fn(),
}));

// ========== IMPORTS ==========
import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import BasicInfoSection from "./BasicInfoSection";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  putToOpenElisServer,
} from "../../../utils/Utils";
import messages from "../../../../languages/en.json";

const renderSection = (testId = "42") =>
  render(
    <MemoryRouter
      initialEntries={[
        `/MasterListsPage/TestCatalogEditor/${testId}/basic-info`,
      ]}
    >
      <IntlProvider locale="en" messages={messages}>
        <BasicInfoSection testId={testId} />
      </IntlProvider>
    </MemoryRouter>,
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

  it("activating with coverage gaps requires acknowledgment (the safety gate)", async () => {
    // Load the test INACTIVE so toggling Active on triggers the activation gate.
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb({
        name: "Glucose",
        code: "GLU",
        description: "",
        domain: "CLINICAL",
        antimicrobialResistance: false,
        active: false,
        orderable: true,
      }),
    );
    const gapReport = {
      status: 409,
      male: {
        sex: "M",
        status: "GAP",
        gaps: [{ fromAge: 0, toAge: 1 }],
        overlaps: [],
      },
      female: { sex: "F", status: "EMPTY", gaps: [], overlaps: [] },
    };
    // First activate (no ack) → 409 with the gap report; second (with ack) → 200.
    postToOpenElisServerJsonResponse
      .mockImplementationOnce((url, body, cb) => cb(gapReport))
      .mockImplementationOnce((url, body, cb) => cb({ male: gapReport.male }));

    renderSection();
    await screen.findByLabelText("Clinical");

    fireEvent.click(screen.getByRole("switch", { name: /Active/ }));
    await waitFor(() =>
      expect(postToOpenElisServerJsonResponse).toHaveBeenCalledTimes(1),
    );
    expect(postToOpenElisServerJsonResponse.mock.calls[0][0]).toBe(
      "/rest/test-catalog/tests/42/activate",
    );
    // The 409 surfaces the acknowledgment modal.
    expect(
      await screen.findByText(
        messages["label.testCatalog.ranges.ackModal.warning"],
      ),
    ).toBeInTheDocument();

    // Acknowledge → re-POST carrying the acknowledged gap report.
    fireEvent.click(
      screen.getByText(messages["label.testCatalog.ranges.ackModal.confirm"]),
    );
    await waitFor(() =>
      expect(postToOpenElisServerJsonResponse).toHaveBeenCalledTimes(2),
    );
    const secondBody = JSON.parse(
      postToOpenElisServerJsonResponse.mock.calls[1][1],
    );
    expect(secondBody.gapsAcknowledged).toBeTruthy();

    // The acknowledged activation succeeded → the modal closes and Active turns on.
    await waitFor(() =>
      expect(
        screen.queryByText(
          messages["label.testCatalog.ranges.ackModal.warning"],
        ),
      ).not.toBeInTheDocument(),
    );
    expect(screen.getByRole("switch", { name: /Active/ })).toBeChecked();
  });

  it("shows an error state when the fetch fails", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb(undefined));
    renderSection();
    expect(
      await screen.findByText(messages["label.testCatalog.editor.loadError"]),
    ).toBeInTheDocument();
  });
});

describe("BasicInfoSection create mode (testId=new)", () => {
  beforeEach(() => {
    // Create mode fetches the Lab Unit + Sample type reference lists only.
    getFromOpenElisServer.mockImplementation((url, cb) => cb([]));
  });

  it("renders a blank create form and gates Save until required fields are filled", async () => {
    renderSection("new");
    // Test name field is present (create-only label).
    expect(
      await screen.findByLabelText(messages["label.testCatalog.testName"]),
    ).toBeInTheDocument();
    // Save is disabled with an empty form (name/reportingName/code/sampleType required).
    const save = screen.getByRole("button", { name: "Save" });
    expect(save).toBeDisabled();
    // It does not fetch the edit-mode basic-info payload.
    expect(getFromOpenElisServer).not.toHaveBeenCalledWith(
      "/rest/test-catalog/tests/new/basic-info",
      expect.anything(),
    );
  });
});
