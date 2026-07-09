/**
 * PanelsSection — OGC-949 M9 / OGC-980..982.
 *
 * Covers: loading memberships, adding a panel via the typeahead + saving the
 * captured payload, editing this test's position, removing a membership, and
 * the error state.
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
import PanelsSection from "./PanelsSection";
import {
  getFromOpenElisServer,
  putToOpenElisServer,
} from "../../../utils/Utils";
import messages from "../../../../languages/en.json";

const allPanels = [
  { id: "1", name: "Lipid Panel" },
  { id: "2", name: "Metabolic Panel" },
];

const wire = (memberships) => {
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url === "/rest/test-catalog/panels") {
      cb(allPanels);
    } else if (url.includes("/tests/")) {
      cb({ testId: "42", memberships });
    }
  });
};

const renderSection = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <PanelsSection testId="42" />
    </IntlProvider>,
  );

beforeEach(() => {
  vi.clearAllMocks();
  putToOpenElisServer.mockImplementation((url, payload, cb) => cb(200));
});

describe("PanelsSection", () => {
  it("loads and renders existing memberships", async () => {
    wire([{ panelId: "1", panelName: "Lipid Panel", position: 3 }]);
    renderSection();
    expect(await screen.findByText("Lipid Panel")).toBeInTheDocument();
    const row = screen.getByTestId("panel-membership-1");
    expect(within(row).getByRole("spinbutton")).toHaveValue(3);
  });

  it("adds a panel via the typeahead and saves the captured payload", async () => {
    wire([]);
    renderSection();
    await screen.findByText(messages["label.testCatalog.panels.empty"]);
    const combo = screen.getByPlaceholderText(
      messages["label.testCatalog.panels.addToPanel"],
    );
    fireEvent.click(combo);
    fireEvent.click(screen.getByText("Metabolic Panel"));
    // Membership row appears.
    expect(await screen.findByTestId("panel-membership-2")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await waitFor(() => expect(putToOpenElisServer).toHaveBeenCalled());
    expect(
      JSON.parse(putToOpenElisServer.mock.calls[0][1]).memberships,
    ).toEqual([{ panelId: "2", position: null }]);
  });

  it("edits this test's position and saves it", async () => {
    wire([{ panelId: "1", panelName: "Lipid Panel", position: 3 }]);
    renderSection();
    await screen.findByText("Lipid Panel");
    const row = screen.getByTestId("panel-membership-1");
    fireEvent.change(within(row).getByRole("spinbutton"), {
      target: { value: "5" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await waitFor(() => expect(putToOpenElisServer).toHaveBeenCalled());
    expect(
      JSON.parse(putToOpenElisServer.mock.calls[0][1]).memberships,
    ).toEqual([{ panelId: "1", position: 5 }]);
  });

  it("removes a membership so it drops out of the payload", async () => {
    wire([{ panelId: "1", panelName: "Lipid Panel", position: 3 }]);
    renderSection();
    await screen.findByText("Lipid Panel");
    fireEvent.click(
      screen.getByRole("button", {
        name: messages["label.testCatalog.panels.remove"],
      }),
    );
    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await waitFor(() => expect(putToOpenElisServer).toHaveBeenCalled());
    expect(
      JSON.parse(putToOpenElisServer.mock.calls[0][1]).memberships,
    ).toEqual([]);
  });

  it("shows an error state when the fetch fails", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url === "/rest/test-catalog/panels") {
        cb(allPanels);
      } else {
        cb(undefined);
      }
    });
    renderSection();
    expect(
      await screen.findByText(messages["label.testCatalog.panels.loadError"]),
    ).toBeInTheDocument();
  });
});
