/**
 * PanelTerminologySection — OGC-224 C4 (FRS v2.2).
 *
 * - the identifier/routing banner is present (LOINC = the panel's identifier,
 *   live FHIR routing key);
 * - mappings render with source tags and the SAME_AS LOINC row carries the
 *   Primary tag;
 * - all five sources are offered (incl. WHONET);
 * - Save PUTs the desired set (folding in a typed-but-not-added draft);
 * - empty state.
 */

// ========== MOCKS (before imports) ==========
vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  putToOpenElisServer: vi.fn(),
}));

// ========== IMPORTS ==========
import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import PanelTerminologySection, {
  primaryLoincIndex,
} from "./PanelTerminologySection";
import {
  getFromOpenElisServer,
  putToOpenElisServer,
} from "../../../utils/Utils";
import messages from "../../../../languages/en.json";

const PANEL = { id: "3", name: "CMP", domain: "CLINICAL" };
const MAPPINGS = [
  { id: "m1", source: "LOINC", code: "24323-8", relationship: "SAME_AS" },
  { id: "m2", source: "SNOMED", code: "26604007", relationship: "SAME_AS" },
];

const mockServer = (mappings = MAPPINGS) => {
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url.includes("/terminology")) {
      cb({ panelId: "3", mappings });
    } else {
      cb(undefined);
    }
  });
};

const wrap = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <PanelTerminologySection panel={PANEL} onSaved={() => {}} />
    </IntlProvider>,
  );

beforeEach(() => {
  vi.clearAllMocks();
});

describe("PanelTerminologySection", () => {
  it("shows the identifier/routing banner and the mappings with a Primary LOINC tag", async () => {
    mockServer();
    wrap();
    expect(
      await screen.findByText(messages["helper.panel.loincIsIdentifier"]),
    ).toBeInTheDocument();
    expect(screen.getByText("24323-8")).toBeInTheDocument();
    expect(screen.getByText("26604007")).toBeInTheDocument();
    expect(screen.getByTestId("primary-tag")).toBeInTheDocument();
  });

  it("offers all five sources including WHONET", async () => {
    mockServer();
    wrap();
    await screen.findByText("24323-8");
    const select = document.querySelector("#panel-terminology-source");
    const values = Array.from(select.querySelectorAll("option")).map(
      (o) => o.value,
    );
    for (const s of ["LOINC", "SNOMED", "CIEL", "OCL", "WHONET"]) {
      expect(values).toContain(s);
    }
  });

  it("Save PUTs the desired set, folding in a typed draft row", async () => {
    mockServer();
    wrap();
    await screen.findByText("24323-8");
    fireEvent.change(document.querySelector("#panel-terminology-source"), {
      target: { value: "WHONET" },
    });
    fireEvent.change(document.querySelector("#panel-terminology-code"), {
      target: { value: "WN-9" },
    });
    fireEvent.click(screen.getByRole("button", { name: /^save$/i }));
    const [url, payload] = putToOpenElisServer.mock.calls[0];
    expect(url).toBe("/rest/test-catalog/panels/3/terminology");
    const body = JSON.parse(payload);
    expect(body.mappings).toHaveLength(3);
    expect(body.mappings[2]).toEqual({
      id: null,
      source: "WHONET",
      code: "WN-9",
      relationship: null,
    });
  });

  it("shows the empty state when no mappings exist", async () => {
    mockServer([]);
    wrap();
    expect(
      await screen.findByText(messages["label.panel.terminology.empty"]),
    ).toBeInTheDocument();
  });
});

describe("primaryLoincIndex (pure)", () => {
  it("picks the SAME_AS (or relationship-less) LOINC mapping", () => {
    expect(primaryLoincIndex(MAPPINGS)).toBe(0);
    expect(
      primaryLoincIndex([
        { source: "SNOMED", code: "1" },
        { source: "LOINC", code: "2", relationship: "BROADER_THAN" },
        { source: "LOINC", code: "3" },
      ]),
    ).toBe(2);
    expect(primaryLoincIndex([{ source: "SNOMED", code: "1" }])).toBe(-1);
  });
});
