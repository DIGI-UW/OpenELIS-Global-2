/**
 * LocalizationSection — OGC-949 / OGC-767, OGC-1153.
 *
 * In-context per-locale editor for a test's name / reporting name, backed by the
 * existing /rest/localizations endpoints. Covers render with a fallback field,
 * the locale-specific (no-fallback) case, the error state, and OGC-1153: the
 * picker opening on the session locale plus the stated auto-save behaviour.
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

const mockHappyPath = (locales = LOCALES) =>
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url.includes("supportedlocales")) {
      cb(locales);
    } else if (url.includes("/rest/localizations/")) {
      const id = url.split("/rest/localizations/")[1];
      cb(LOC[id]);
    } else if (url.includes("/rest/test-catalog/")) {
      cb(REFS);
    }
  });

const renderSection = (sessionLocale = "en") =>
  render(
    <IntlProvider locale={sessionLocale} messages={messages}>
      <LocalizationSection testId="42" />
    </IntlProvider>,
  );

const localePicker = () => document.getElementById("localization-locale");

beforeEach(() => {
  vi.clearAllMocks();
});

describe("LocalizationSection", () => {
  it("shows a fallback indicator for an untranslated field and the locale value for a translated one", async () => {
    mockHappyPath();
    renderSection("fr");

    // In a French session the picker opens on fr. 'name' has no fr value ->
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

  it("opens the locale picker on the session locale, not the first non-fallback locale", async () => {
    mockHappyPath();
    renderSection("en");

    await screen.findByText(messages["label.testCatalog.localization.locale"]);
    expect(localePicker().value).toBe("en");
    expect(document.getElementById("localization-input-name").value).toBe(
      "Glucose",
    );
  });

  it("matches a regional session locale to the same supported language", async () => {
    mockHappyPath();
    renderSection("en-GB");

    await screen.findByText(messages["label.testCatalog.localization.locale"]);
    expect(localePicker().value).toBe("en");
  });

  it("falls back to the configured fallback locale when the session locale is unsupported", async () => {
    mockHappyPath();
    renderSection("es");

    await screen.findByText(messages["label.testCatalog.localization.locale"]);
    expect(localePicker().value).toBe("en");
  });

  it("uses the first locale when neither the session locale nor a fallback is configured", async () => {
    mockHappyPath([
      { localeCode: "fr", displayName: "French", fallback: false },
      { localeCode: "mg", displayName: "Malagasy", fallback: false },
    ]);
    renderSection("es");

    await screen.findByText(messages["label.testCatalog.localization.locale"]);
    expect(localePicker().value).toBe("fr");
  });

  it("states that each field is saved automatically on blur", async () => {
    mockHappyPath();
    renderSection("en");

    expect(
      await screen.findByText(
        messages["label.testCatalog.localization.autoSave"],
        { exact: false, selector: "p" },
      ),
    ).toBeInTheDocument();
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
