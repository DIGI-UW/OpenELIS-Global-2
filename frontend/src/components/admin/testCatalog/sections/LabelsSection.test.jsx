/**
 * LabelsSection — OGC-949 M14 / OGC-988 + OGC-989.
 *
 * Per-test label presets table + allow-override toggle, backed by the OGC-285
 * label-config API. Covers render with linked presets, the empty state, the
 * error state, and the system-preset Add picker.
 */

// ========== MOCKS (before imports) ==========
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
import LabelsSection from "./LabelsSection";
import {
  getFromOpenElisServer,
  putToOpenElisServer,
} from "../../../utils/Utils";
import messages from "../../../../languages/en.json";

const SYSTEM_PRESETS = [
  { id: 1, name: "Specimen Label", isSystem: true, printsPerSample: true },
  { id: 2, name: "Block Label", isSystem: true, printsPerSample: true },
  { id: 3, name: "Slide Label", isSystem: true, printsPerSample: true },
  { id: 99, name: "Custom Label", isSystem: false, printsPerSample: true },
];

// Branch the GET mock on URL: labelConfig vs labelPresets.
const mockGets = (config) => {
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url.includes("/labelConfig")) {
      cb(config);
    } else if (url.includes("/labelPresets")) {
      cb(SYSTEM_PRESETS);
    } else {
      cb(undefined);
    }
  });
};

const renderSection = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <LabelsSection testId="42" />
    </IntlProvider>,
  );

beforeEach(() => {
  vi.clearAllMocks();
});

describe("LabelsSection", () => {
  it("renders the linked presets with quantities and the override toggle", async () => {
    mockGets({
      allowOrderEntryOverride: true,
      links: [
        {
          id: 10,
          presetId: 1,
          presetName: "Specimen Label",
          defaultQty: 2,
          maxQty: 5,
          allowOverride: true,
        },
      ],
    });
    renderSection();

    // The preset name shows both in the config table and the order-entry preview
    // (FR-67), so match all occurrences.
    expect(
      (await screen.findAllByText("Specimen Label")).length,
    ).toBeGreaterThan(0);
    expect(document.getElementById("default-1").value).toBe("2");
    expect(document.getElementById("max-1").value).toBe("5");
    expect(
      screen.getByText(
        messages["label.testCatalog.labels.allowOverride.label"],
      ),
    ).toBeInTheDocument();
  });

  it("shows the empty state when no presets are linked", async () => {
    mockGets({ allowOrderEntryOverride: true, links: [] });
    renderSection();
    expect(
      await screen.findByText(messages["label.testCatalog.labels.empty"]),
    ).toBeInTheDocument();
  });

  it("shows an error state when the config fetch fails", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.includes("/labelConfig")) {
        cb(undefined);
      } else {
        cb(SYSTEM_PRESETS);
      }
    });
    renderSection();
    expect(
      await screen.findByText(messages["label.testCatalog.labels.loadError"]),
    ).toBeInTheDocument();
  });

  it("offers the Add Label Type picker", async () => {
    mockGets({ allowOrderEntryOverride: true, links: [] });
    renderSection();
    expect(
      await screen.findByText(
        messages["label.testCatalog.labels.addLabelType"],
      ),
    ).toBeInTheDocument();
  });

  // Label config is committed by Save, not by editing a quantity, toggling a
  // checkbox or adding a preset — an admin has to be able to commit deliberately.
  describe("explicit save", () => {
    const CONFIG_ONE = {
      allowOrderEntryOverride: true,
      links: [
        {
          presetId: 1,
          presetName: "Specimen Label",
          defaultQty: 1,
          maxQty: 2,
          allowOverride: true,
        },
      ],
    };

    it("does not write until Save is clicked", async () => {
      mockGets(CONFIG_ONE);
      renderSection();
      await waitFor(() =>
        expect(document.getElementById("default-1")).not.toBeNull(),
      );

      expect(screen.getByTestId("labels-save")).toBeDisabled();

      const qty = document.getElementById("default-1");
      fireEvent.change(qty, { target: { value: "2" } });

      expect(putToOpenElisServer).not.toHaveBeenCalled();
      expect(screen.getByTestId("labels-save")).toBeEnabled();
      expect(
        screen.getByText(messages["label.testCatalog.labels.unsaved"]),
      ).toBeInTheDocument();

      putToOpenElisServer.mockImplementation((url, body, cb) => cb(200));
      fireEvent.click(screen.getByTestId("labels-save"));

      expect(putToOpenElisServer).toHaveBeenCalledTimes(1);
      const [url, body] = putToOpenElisServer.mock.calls[0];
      expect(url).toBe("/rest/api/tests/42/labelConfig");
      expect(Number(JSON.parse(body).links[0].defaultQty)).toBe(2);
    });

    it("discards a pending change on Cancel", async () => {
      mockGets(CONFIG_ONE);
      renderSection();
      await waitFor(() =>
        expect(document.getElementById("default-1")).not.toBeNull(),
      );

      fireEvent.change(document.getElementById("default-1"), {
        target: { value: "2" },
      });
      fireEvent.click(screen.getByTestId("labels-cancel"));

      expect(putToOpenElisServer).not.toHaveBeenCalled();
      expect(screen.getByTestId("labels-save")).toBeDisabled();
    });

    /** max < default is refused for the whole set at Save time. */
    it("refuses to save a row whose maximum is below its default", async () => {
      mockGets(CONFIG_ONE);
      renderSection();
      await waitFor(() =>
        expect(document.getElementById("max-1")).not.toBeNull(),
      );

      fireEvent.change(document.getElementById("max-1"), {
        target: { value: "0" },
      });
      fireEvent.click(screen.getByTestId("labels-save"));

      expect(putToOpenElisServer).not.toHaveBeenCalled();
      expect(
        screen.getByText(messages["label.testCatalog.labels.maxLtDefault"]),
      ).toBeInTheDocument();
    });
  });
});
