/**
 * Ranges section — localization guard.
 *
 * Every word the Ranges UI puts on screen must come from a message key, so a
 * site running in French sees French once the bundle is translated. The audit
 * that proves it today is easy to break tomorrow: one `<TableHeader>Actions`
 * or one `"Any age"` default slipped into a new branch reads fine in English
 * and is invisible until someone switches locale.
 *
 * So the section is rendered against a bundle where every key resolves to a
 * marker instead of English. Anything English still on screen is text the
 * component wrote itself — including the sex, age-unit, coverage and column
 * labels, which are the ones a "just print the value" shortcut tends to
 * produce. Persisted data (component labels, specimen names, the numbers) is
 * expected to survive untranslated, and is asserted to.
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
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import RangesSection from "./RangesSection";
import { getFromOpenElisServer } from "../../../utils/Utils";
import englishMessages from "../../../../languages/en.json";

/**
 * A stand-in bundle: every key keeps its own identity ("«label.x»") so a
 * missing key still shows up as react-intl falling back to the raw id, while
 * no English wording reaches the DOM through the bundle.
 */
const markerMessages = Object.fromEntries(
  Object.keys(englishMessages).map((key) => [key, `«${key}»`]),
);

/** English wording that must never be produced by the component itself. */
const ENGLISH_UI_WORDS = [
  "Component",
  "Specimen",
  "Sex",
  "Age range",
  "Normal",
  "Critical",
  "Valid",
  "Actions",
  "Male",
  "Female",
  "Both",
  "Any age",
  "days",
  "months",
  "years",
  "Fully covered",
  "Has gaps",
  "Has overlaps",
  "No ranges",
  "Coverage validation",
  "Add range",
  "All components",
  "All specimens",
  "Uncovered age windows",
];

const RESPONSE = {
  testId: "42",
  // One row per shape the table renders: both sexes, male-only with a
  // day-and-year bounded window, female-only, and a specimen override.
  ranges: [
    {
      id: "1",
      componentId: "c1",
      sampleTypeId: null,
      gender: null,
      minAge: 0,
      maxAge: null,
      lowNormal: 35,
      highNormal: 55,
      lowCritical: 25,
      highCritical: 75,
      lowValid: 0,
      highValid: 100,
    },
    {
      id: "2",
      componentId: "c1",
      sampleTypeId: "s2",
      gender: "M",
      minAge: 0,
      maxAge: 10950,
      lowNormal: 30,
      highNormal: 50,
    },
    {
      id: "3",
      componentId: "c2",
      sampleTypeId: "s1",
      gender: "F",
      minAge: 10950,
      maxAge: null,
      lowNormal: 31,
      highNormal: 51,
    },
  ],
  sampleTypes: [
    { id: "s1", name: "DBS" },
    { id: "s2", name: "Urines" },
  ],
  coverage: {
    male: {
      sex: "M",
      status: "GAP",
      gaps: [{ fromAge: 30, toAge: 45 }],
      overlaps: [],
    },
    female: { sex: "F", status: "COMPLETE", gaps: [], overlaps: [] },
  },
};

const COMPONENTS = {
  components: [
    { id: "c1", label: "Albumine recherche miction", code: "ALB" },
    { id: "c2", label: "Album-B", code: "ALB_B" },
  ],
};

const renderWithMarkers = () =>
  render(
    <IntlProvider locale="en" messages={markerMessages}>
      <RangesSection testId="42" />
    </IntlProvider>,
  );

beforeEach(() => {
  vi.clearAllMocks();
  getFromOpenElisServer.mockImplementation((url, cb) =>
    cb(url.endsWith("/sample-results") ? COMPONENTS : RESPONSE),
  );
});

describe("Ranges section localization", () => {
  it("writes no English of its own — every label comes from a message key", async () => {
    renderWithMarkers();
    await screen.findByTestId("ranges-section");
    // Drop the markers first: a key id such as `…ranges.years` contains the
    // very word being searched for, and would mask a real leak beside it.
    const rendered = screen
      .getByTestId("ranges-section")
      .textContent.replace(/«[^»]*»/g, "");

    const leaked = ENGLISH_UI_WORDS.filter((word) => rendered.includes(word));
    expect(leaked).toEqual([]);
  });

  it("resolves every label it renders — no raw message id reaches the user", async () => {
    renderWithMarkers();
    await screen.findByTestId("ranges-section");
    const rendered = screen.getByTestId("ranges-section").textContent;

    // A key absent from the bundle renders as the bare id; a resolved one
    // renders inside the markers.
    expect(rendered).toMatch(/«label\./);
    expect(rendered).not.toMatch(/(^|[^«])label\.[a-zA-Z.]+/);
  });

  it("leaves persisted range data alone", async () => {
    renderWithMarkers();
    await screen.findByTestId("ranges-section");
    const rendered = screen.getByTestId("ranges-section").textContent;

    // Component labels and specimen names are configuration, not UI copy.
    expect(rendered).toContain("Albumine recherche miction");
    expect(rendered).toContain("Album-B");
    expect(rendered).toContain("DBS");
    expect(rendered).toContain("Urines");
    // …as are the bounds themselves.
    expect(rendered).toContain("35 – 55");
    expect(rendered).toContain("25 – 75");
    expect(rendered).toContain("0 – 100");
  });
});
