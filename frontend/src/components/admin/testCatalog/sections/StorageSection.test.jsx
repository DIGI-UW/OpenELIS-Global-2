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

describe("StorageSection in group mode (OGC-1238)", () => {
  const renderGroup = () =>
    render(
      <IntlProvider locale="en" messages={messages}>
        <StorageSection groupTestIds={["31", "32"]} />
      </IntlProvider>,
    );

  const stored = {
    31: {
      ...emptyConfig,
      testId: "31",
      storageCondition: "FROZEN",
      protectFromLight: true,
    },
    32: {
      ...emptyConfig,
      testId: "32",
      storageCondition: "REFRIGERATED",
      protectFromLight: false,
    },
  };

  const saveButton = () => screen.getByRole("button", { name: "Save" });

  beforeEach(() => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb(stored[url.match(/tests\/(\d+)\/storage/)[1]]),
    );
  });

  it("loads every test and names the fields that differ", async () => {
    renderGroup();

    const warning = await screen.findByTestId("storage-differ-warning");
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/test-catalog/tests/32/storage",
      expect.any(Function),
    );
    expect(warning).toHaveTextContent(
      messages["label.testCatalog.storage.condition"],
    );
    expect(warning).toHaveTextContent(
      messages["label.testCatalog.storage.protectFromLight"],
    );
    expect(warning).not.toHaveTextContent(
      messages["label.testCatalog.storage.disposalMethod"],
    );
  });

  it("shows no warning when every test has the same storage", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb({ ...stored[31], testId: url.match(/tests\/(\d+)/)[1] }),
    );
    renderGroup();
    await screen.findByTestId("storage-section");

    expect(screen.queryByTestId("storage-differ-warning")).toBeNull();
  });

  it("keeps Save disabled until a field changes", async () => {
    renderGroup();
    await screen.findByTestId("storage-section");

    expect(saveButton()).toBeDisabled();
    fireEvent.click(saveButton());
    expect(putToOpenElisServer).not.toHaveBeenCalled();

    fireEvent.change(
      screen.getByLabelText(
        messages["label.testCatalog.storage.stabilityNotes"],
      ),
      { target: { value: "Keep upright" } },
    );
    expect(saveButton()).toBeEnabled();

    fireEvent.change(
      screen.getByLabelText(
        messages["label.testCatalog.storage.stabilityNotes"],
      ),
      { target: { value: "" } },
    );
    expect(saveButton()).toBeDisabled();
  });

  it("sends only the changed fields to the group save", async () => {
    renderGroup();
    await screen.findByTestId("storage-section");

    fireEvent.change(
      screen.getByLabelText(
        messages["label.testCatalog.storage.stabilityNotes"],
      ),
      { target: { value: "Keep upright" } },
    );
    fireEvent.change(
      screen.getByLabelText(messages["label.testCatalog.storage.duration"]),
      { target: { value: "3" } },
    );
    fireEvent.click(saveButton());

    await waitFor(() => expect(putToOpenElisServer).toHaveBeenCalled());
    const [url, payload] = putToOpenElisServer.mock.calls[0];
    expect(url).toBe("/rest/test-catalog/group/storage");
    const body = JSON.parse(payload);
    expect(body.testIds).toEqual(["31", "32"]);
    expect(body.fields.sort()).toEqual(["stabilityNotes", "storageDuration"]);
    expect(body.storage.stabilityNotes).toBe("Keep upright");
    expect(body.storage.storageDuration).toBe(3);
  });

  it("applies the first test's value of a differing field once it is edited back to it", async () => {
    renderGroup();
    await screen.findByTestId("storage-differ-warning");
    const condition = screen.getByLabelText(
      messages["label.testCatalog.storage.condition"],
    );

    fireEvent.change(condition, { target: { value: "AMBIENT" } });
    fireEvent.change(condition, { target: { value: "FROZEN" } });
    expect(saveButton()).toBeEnabled();
    fireEvent.click(saveButton());

    await waitFor(() => expect(putToOpenElisServer).toHaveBeenCalled());
    const body = JSON.parse(putToOpenElisServer.mock.calls[0][1]);
    expect(body.fields).toEqual(["storageCondition"]);
    expect(body.storage.storageCondition).toBe("FROZEN");
  });
});
