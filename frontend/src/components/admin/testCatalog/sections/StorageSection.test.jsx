/**
 * StorageSection — OGC-949 M8 / OGC-977..979.
 *
 * Covers: loading an existing storage config, editing + saving with the payload
 * captured (numeric coercion + flags), and the fetch-error state.
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
import StorageSection from "./StorageSection";
import {
  getFromOpenElisServer,
  putToOpenElisServer,
} from "../../../utils/Utils";
import messages from "../../../../languages/en.json";

const renderSection = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <StorageSection testId="42" />
    </IntlProvider>,
  );

const emptyConfig = {
  testId: "42",
  protectFromLight: false,
  doNotFreeze: false,
  doNotRefrigerate: false,
  overrideRestricted: false,
};

beforeEach(() => {
  vi.clearAllMocks();
  putToOpenElisServer.mockImplementation((url, payload, cb) => cb(200));
});

describe("StorageSection", () => {
  it("loads and renders an existing storage config", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb({
        testId: "42",
        storageCondition: "REFRIGERATED",
        storageDuration: 7,
        storageDurationUnit: "days",
        protectFromLight: true,
        doNotFreeze: false,
        doNotRefrigerate: false,
        disposalMethod: "INCINERATION",
        overrideRestricted: false,
      }),
    );
    renderSection();
    await screen.findByTestId("storage-section");

    expect(
      screen.getByLabelText(messages["label.testCatalog.storage.condition"])
        .value,
    ).toBe("REFRIGERATED");
    expect(
      screen.getByLabelText(
        messages["label.testCatalog.storage.protectFromLight"],
      ),
    ).toBeChecked();
    expect(
      screen.getByLabelText(
        messages["label.testCatalog.storage.disposalMethod"],
      ).value,
    ).toBe("INCINERATION");
  });

  it("edits and saves, capturing a coerced payload", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb({ ...emptyConfig }),
    );
    renderSection();
    await screen.findByTestId("storage-section");

    fireEvent.change(
      screen.getByLabelText(messages["label.testCatalog.storage.condition"]),
      { target: { value: "FROZEN" } },
    );
    fireEvent.change(
      screen.getByLabelText(messages["label.testCatalog.storage.duration"]),
      { target: { value: "14" } },
    );
    fireEvent.click(
      screen.getByLabelText(messages["label.testCatalog.storage.doNotFreeze"]),
    );

    fireEvent.click(screen.getByRole("button", { name: "Save" }));
    await waitFor(() => expect(putToOpenElisServer).toHaveBeenCalled());
    const body = JSON.parse(putToOpenElisServer.mock.calls[0][1]);
    expect(body.storageCondition).toBe("FROZEN");
    expect(body.storageDuration).toBe(14); // string → int
    expect(body.doNotFreeze).toBe(true);
  });

  it("shows an error state when the fetch fails", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb(undefined));
    renderSection();
    expect(
      await screen.findByText(messages["label.testCatalog.storage.loadError"]),
    ).toBeInTheDocument();
  });
});
