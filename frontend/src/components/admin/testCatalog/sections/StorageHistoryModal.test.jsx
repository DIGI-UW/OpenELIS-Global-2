/**
 * StorageHistoryModal — OGC-949 / OGC-1005.
 *
 * Expands sample-storage audit snapshots into per-changed-field rows. Covers the
 * diff rendering, the empty state, and the error state.
 */

// ========== MOCKS (before imports) ==========
vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

// ========== IMPORTS ==========
import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import StorageHistoryModal from "./StorageHistoryModal";
import { getFromOpenElisServer } from "../../../utils/Utils";
import messages from "../../../../languages/en.json";

const renderModal = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <StorageHistoryModal open onClose={() => {}} testId="42" />
    </IntlProvider>,
  );

beforeEach(() => {
  vi.clearAllMocks();
});

describe("StorageHistoryModal", () => {
  it("expands a snapshot diff into changed-field rows", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb([
        {
          id: "h1",
          changedAt: "2026-06-25 10:00:00",
          changedBy: "1",
          changeType: "UPDATE",
          previousValues: JSON.stringify({
            storageCondition: "REFRIGERATED",
            storageDuration: 7,
          }),
          newValues: JSON.stringify({
            storageCondition: "FROZEN",
            storageDuration: 7,
          }),
        },
      ]),
    );
    renderModal();

    // Only the changed field (storageCondition) is shown, humanized.
    expect(await screen.findByText("Storage Condition")).toBeInTheDocument();
    expect(screen.getByText("REFRIGERATED")).toBeInTheDocument();
    expect(screen.getByText("FROZEN")).toBeInTheDocument();
    // The unchanged field is not rendered.
    expect(screen.queryByText("Storage Duration")).not.toBeInTheDocument();
  });

  it("shows the empty state when there are no changes", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb([]));
    renderModal();
    expect(
      await screen.findByText(
        messages["label.testCatalog.storage.history.empty"],
      ),
    ).toBeInTheDocument();
  });

  it("shows an error state when the fetch fails", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb(undefined));
    renderModal();
    expect(
      await screen.findByText(
        messages["label.testCatalog.storage.history.loadError"],
      ),
    ).toBeInTheDocument();
  });
});
