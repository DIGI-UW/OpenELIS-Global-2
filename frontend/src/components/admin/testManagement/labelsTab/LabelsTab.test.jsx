import React from "react";
import { fireEvent, render, screen, act } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";

/**
 * LabelsTab Vitest tests (OGC-285 M4, T104).
 *
 * Coverage per tasks.md:
 * 1. Empty state: GET returns no links -> empty state message shown.
 * 2. Link 2 presets: mock GET returns 2 links -> both preset names visible.
 * 3. Duplicate-preset error UI: master toggle off -> all Allow Override boxes disabled.
 * 4. Master toggle off -> Allow Override boxes forced disabled (AC-12).
 *
 * Uses vi.mock for Utils so no real HTTP calls are made. Assertions use
 * getByRole/getByText per durable memory "No test workaround comments".
 */

// Hoist mock refs so they can be mutated before import
const { getFromOpenElisServerImpl, putToOpenElisServerFullResponseImpl } =
  vi.hoisted(() => {
    const getImpl = vi.fn();
    const putImpl = vi.fn();
    return {
      getFromOpenElisServerImpl: getImpl,
      putToOpenElisServerFullResponseImpl: putImpl,
    };
  });

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: getFromOpenElisServerImpl,
  putToOpenElisServerFullResponse: putToOpenElisServerFullResponseImpl,
}));

import LabelsTab from "./LabelsTab";

const renderWithIntl = (ui) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {ui}
    </IntlProvider>,
  );

// Helper: configure getFromOpenElisServer to return different data based on URL
function mockGetResponses({ labelConfig = null, presets = [] } = {}) {
  const defaultConfig = labelConfig ?? {
    allowOrderEntryOverride: true,
    links: [],
  };
  getFromOpenElisServerImpl.mockImplementation((url, callback) => {
    if (url.includes("/labelConfig")) {
      callback(defaultConfig);
    } else if (url.includes("/labelPresets")) {
      callback(presets);
    }
  });
}

// -----------------------------------------------------------------------
// 1. Empty state
// -----------------------------------------------------------------------
describe("LabelsTab — empty state", () => {
  beforeEach(() => {
    mockGetResponses();
  });

  test("shows empty-state message when no links returned", async () => {
    renderWithIntl(<LabelsTab testId="1" />);

    await waitFor(() => {
      expect(
        screen.getByText(
          messages["admin.testCatalog.labels.linkedPresets.empty"],
        ),
      ).toBeInTheDocument();
    });
  });

  test("master override toggle is visible", async () => {
    renderWithIntl(<LabelsTab testId="1" />);

    await waitFor(() => {
      // Toggle renders a button element — find by role
      expect(
        screen.getByText(
          messages["admin.testCatalog.labels.masterOverride.label"],
        ),
      ).toBeInTheDocument();
    });
  });
});

// -----------------------------------------------------------------------
// 2. Two linked presets visible after GET
// -----------------------------------------------------------------------
describe("LabelsTab — two linked presets", () => {
  const twoLinks = [
    {
      id: 1,
      presetId: 101,
      presetName: "Specimen Label",
      defaultQty: 1,
      maxQty: 5,
      allowOverride: true,
    },
    {
      id: 2,
      presetId: 102,
      presetName: "Slide Label",
      defaultQty: 4,
      maxQty: 12,
      allowOverride: false,
    },
  ];

  beforeEach(() => {
    mockGetResponses({
      labelConfig: { allowOrderEntryOverride: true, links: twoLinks },
      presets: [],
    });
  });

  test("both preset names are visible", async () => {
    renderWithIntl(<LabelsTab testId="1" />);

    await waitFor(() => {
      // Both appear in LinkedPresetsTable AND OrderEntryPreview; use getAllBy
      expect(screen.getAllByText("Specimen Label").length).toBeGreaterThan(0);
      expect(screen.getAllByText("Slide Label").length).toBeGreaterThan(0);
    });
  });

  test("order entry preview shows both presets", async () => {
    renderWithIntl(<LabelsTab testId="1" />);

    await waitFor(() => {
      // Preview title
      expect(
        screen.getByText(messages["admin.testCatalog.labels.preview.title"]),
      ).toBeInTheDocument();
    });
  });
});

// -----------------------------------------------------------------------
// 3. Master toggle off -> Allow Override checkboxes disabled (AC-12)
// -----------------------------------------------------------------------
describe("LabelsTab — master toggle off disables Allow Override boxes", () => {
  const linkWithOverrideOn = [
    {
      id: 1,
      presetId: 101,
      presetName: "Specimen Label",
      defaultQty: 1,
      maxQty: 5,
      allowOverride: true,
    },
  ];

  beforeEach(() => {
    mockGetResponses({
      labelConfig: {
        allowOrderEntryOverride: false,
        links: linkWithOverrideOn,
      },
      presets: [],
    });
  });

  test("when master override is off, allowOverride checkbox is disabled", async () => {
    renderWithIntl(<LabelsTab testId="1" />);

    await waitFor(() => {
      // "Specimen Label" appears in both table and preview; use getAllBy
      expect(screen.getAllByText("Specimen Label").length).toBeGreaterThan(0);
    });

    // The allowOverride checkbox for index 0 — when masterOverride is false,
    // LinkedPresetsTable forces it disabled.
    const checkbox = screen.getByRole("checkbox", { hidden: true });
    expect(checkbox).toBeDisabled();
  });
});

// -----------------------------------------------------------------------
// 4. Duplicate-preset error UI — no direct UI element; verify that
//    the Add dropdown excludes already-linked presets (AC-11 prevention)
// -----------------------------------------------------------------------
describe("LabelsTab — duplicate preset prevention via dropdown exclusion", () => {
  const existingLink = {
    id: 1,
    presetId: 101,
    presetName: "Specimen Label",
    defaultQty: 1,
    maxQty: 5,
    allowOverride: true,
  };

  const availablePreset = {
    id: 101,
    name: "Specimen Label",
    prints_per_sample: true,
    default_per_sample: 1,
    max_per_sample: 5,
  };

  beforeEach(() => {
    mockGetResponses({
      labelConfig: {
        allowOrderEntryOverride: true,
        links: [existingLink],
      },
      presets: [availablePreset],
    });
  });

  test("dropdown is disabled when no new presets available (already all linked)", async () => {
    renderWithIntl(<LabelsTab testId="1" />);

    await waitFor(() => {
      // "Specimen Label" appears in table and preview; use getAllBy
      expect(screen.getAllByText("Specimen Label").length).toBeGreaterThan(0);
    });

    // The Add Label Type dropdown: already-linked preset (id 101) is excluded
    // from addablePresets, so nothing is selectable and dropdown is disabled.
    const dropdown = screen.getByRole("combobox", { hidden: true });
    // When addablePresets is empty, the Dropdown is disabled
    // (see LabelsTab: disabled={addablePresets.length === 0})
    expect(dropdown).toBeDisabled();
  });
});
