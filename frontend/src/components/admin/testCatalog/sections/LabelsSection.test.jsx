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
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import LabelsSection from "./LabelsSection";
import { getFromOpenElisServer } from "../../../utils/Utils";
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

    expect(await screen.findByText("Specimen Label")).toBeInTheDocument();
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
});
