/**
 * LocalizationSection — OGC-949 / OGC-767.
 *
 * In-context per-locale editor for a test's name / reporting name, backed by the
 * existing /rest/localizations endpoints. Covers render with a fallback field,
 * the locale-specific (no-fallback) case, and the error state.
 */

// ========== MOCKS (before imports) ==========
vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  putToOpenElisServerFullResponse: vi.fn(),
}));

// ========== IMPORTS ==========
import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import LocalizationSection from "./LocalizationSection";
import { getFromOpenElisServer } from "../../../utils/Utils";
import messages from "../../../../languages/en.json";

const LOCALES = [
  { localeCode: "en", displayName: "English", fallback: true },
  { localeCode: "fr", displayName: "French", fallback: false },
];

const REFS = {
  testId: "42",
  fields: [
    { field: "name", localizationId: "100" },
    { field: "reportingName", localizationId: "101" },
  ],
};

// name: only English -> falls back for fr. reportingName: has a fr translation.
const LOC = {
  100: { id: "100", translations: { en: "Glucose" } },
  101: { id: "101", translations: { en: "Glucose Report", fr: "Glucose FR" } },
};

const mockHappyPath = () =>
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url.includes("supportedlocales")) {
      cb(LOCALES);
    } else if (url.includes("/rest/localizations/")) {
      const id = url.split("/rest/localizations/")[1];
      cb(LOC[id]);
    } else if (url.includes("/rest/test-catalog/")) {
      cb(REFS);
    }
  });

const renderSection = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <LocalizationSection testId="42" />
    </IntlProvider>,
  );

beforeEach(() => {
  vi.clearAllMocks();
});

describe("LocalizationSection", () => {
  it("shows a fallback indicator for an untranslated field and the locale value for a translated one", async () => {
    mockHappyPath();
    renderSection();

    // Defaults to the first non-fallback locale (fr). 'name' has no fr value ->
    // fallback tag shown, the English value surfaces as the placeholder.
    expect(
      await screen.findByText(
        messages["label.testCatalog.localization.fallback.en"],
      ),
    ).toBeInTheDocument();
    const nameInput = document.getElementById("localization-input-name");
    expect(nameInput.value).toBe("");
    expect(nameInput.placeholder).toBe("Glucose");

    // 'reportingName' has a fr translation -> populated, no fallback tag.
    expect(
      document.getElementById("localization-input-reportingName").value,
    ).toBe("Glucose FR");
  });

  it("shows an error state when the refs fetch fails", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.includes("supportedlocales")) {
        cb(LOCALES);
      } else {
        cb(undefined);
      }
    });
    renderSection();
    expect(
      await screen.findByText(
        messages["label.testCatalog.localization.loadError"],
      ),
    ).toBeInTheDocument();
  });
});
