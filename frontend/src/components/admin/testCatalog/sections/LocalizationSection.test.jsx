/**
 * LocalizationSection — OGC-949 / OGC-767, OGC-1153.
 *
 * In-context per-locale editor for a test's name / reporting name, backed by the
 * existing /rest/localizations endpoints. Covers render with a fallback field,
 * the locale-specific (no-fallback) case, the error state, and OGC-1153: the
 * picker opening on the session locale plus the explicit Save control that
 * replaced saving on blur.
 */

// ========== MOCKS (before imports) ==========
vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  putToOpenElisServerFullResponse: vi.fn(),
}));

// ========== IMPORTS ==========
import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import LocalizationSection from "./LocalizationSection";
import {
  getFromOpenElisServer,
  putToOpenElisServerFullResponse,
} from "../../../utils/Utils";
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

  // The two localization records resolve independently, so the render order must
  // come from the server's field list rather than whichever request landed first.
  it("renders the fields in the order the server declared them", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => {
      if (url.includes("supportedlocales")) {
        cb(LOCALES);
      } else if (url.endsWith("/rest/localizations/100")) {
        setTimeout(() => cb(LOC[100]), 0);
      } else if (url.includes("/rest/localizations/")) {
        cb(LOC[url.split("/rest/localizations/")[1]]);
      } else if (url.includes("/rest/test-catalog/")) {
        cb(REFS);
      }
    });
    renderSection("en");

    await screen.findByText(messages["label.testCatalog.localization.locale"]);
    const rendered = Array.from(
      document.querySelectorAll("[data-testid^='localization-field-']"),
    ).map((el) => el.getAttribute("data-testid"));
    expect(rendered).toEqual([
      "localization-field-name",
      "localization-field-reportingName",
    ]);
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

  describe("saving (OGC-1153)", () => {
    const saveButton = () =>
      screen.getByRole("button", { name: messages["label.button.save"] });
    const cancelButton = () =>
      screen.getByRole("button", { name: messages["label.button.cancel"] });
    const nameInput = () => document.getElementById("localization-input-name");

    // React installs its own `value` setter on controlled inputs to dedupe change
    // events. Passing `target` to fireEvent assigns through that setter, so React
    // records the new value before seeing the event and treats it as a no-op —
    // write through the native setter instead and let React spot the difference.
    const nativeValueSetter = Object.getOwnPropertyDescriptor(
      window.HTMLInputElement.prototype,
      "value",
    ).set;
    const type = (input, value) => {
      nativeValueSetter.call(input, value);
      fireEvent.change(input);
    };

    const renderLoaded = async (sessionLocale = "en") => {
      mockHappyPath();
      renderSection(sessionLocale);
      await screen.findByText(
        messages["label.testCatalog.localization.locale"],
      );
    };

    it("offers a Save button that is disabled until something changes", async () => {
      await renderLoaded();
      expect(saveButton()).toBeDisabled();

      type(nameInput(), "Glucose EN");
      expect(saveButton()).toBeEnabled();

      // Re-typing the stored value is not a change.
      type(nameInput(), "Glucose");
      expect(saveButton()).toBeDisabled();
    });

    it("does not save on blur — only the Save button commits", async () => {
      await renderLoaded();

      type(nameInput(), "Glucose EN");
      fireEvent.blur(nameInput());

      expect(putToOpenElisServerFullResponse).not.toHaveBeenCalled();
      expect(saveButton()).toBeEnabled();
    });

    it("PUTs the edited translations and confirms on Save", async () => {
      await renderLoaded();
      putToOpenElisServerFullResponse.mockImplementation((url, body, cb) =>
        cb({ ok: true }),
      );

      type(nameInput(), "Glucose EN");
      fireEvent.click(saveButton());

      expect(putToOpenElisServerFullResponse).toHaveBeenCalledTimes(1);
      const [url, body] = putToOpenElisServerFullResponse.mock.calls[0];
      expect(url).toBe("/rest/localizations/100/translations");
      expect(JSON.parse(body)).toEqual({ en: "Glucose EN" });

      expect(
        await screen.findByText(
          messages["label.testCatalog.localization.saved"],
        ),
      ).toBeInTheDocument();
      // The drafts are cleared on success, so nothing is left to save.
      expect(saveButton()).toBeDisabled();
    });

    it("keeps the draft so nothing is retyped when the save fails", async () => {
      await renderLoaded();
      putToOpenElisServerFullResponse.mockImplementation((url, body, cb) =>
        cb({ ok: false }),
      );

      type(nameInput(), "Glucose EN");
      fireEvent.click(saveButton());

      expect(
        await screen.findByText(
          messages["label.testCatalog.localization.saveError"],
        ),
      ).toBeInTheDocument();
      expect(nameInput().value).toBe("Glucose EN");
      expect(saveButton()).toBeEnabled();
    });

    // Drafts are keyed by locale, so checking another language mid-edit is safe.
    it("preserves a pending edit across a locale switch and saves both in one PUT", async () => {
      await renderLoaded("en");
      putToOpenElisServerFullResponse.mockImplementation((url, body, cb) =>
        cb({ ok: true }),
      );

      type(nameInput(), "Glucose EN");
      fireEvent.change(localePicker(), { target: { value: "fr" } });
      expect(nameInput().value).toBe("");

      type(nameInput(), "Glucose FR");
      fireEvent.change(localePicker(), { target: { value: "en" } });
      expect(nameInput().value).toBe("Glucose EN");

      fireEvent.click(saveButton());
      expect(putToOpenElisServerFullResponse).toHaveBeenCalledTimes(1);
      expect(
        JSON.parse(putToOpenElisServerFullResponse.mock.calls[0][1]),
      ).toEqual({ en: "Glucose EN", fr: "Glucose FR" });
    });

    it("warns while changes are pending and clears them on Cancel", async () => {
      await renderLoaded();

      type(nameInput(), "Glucose EN");
      expect(
        screen.getByText(messages["label.testCatalog.localization.unsaved"]),
      ).toBeInTheDocument();

      fireEvent.click(cancelButton());
      expect(nameInput().value).toBe("Glucose");
      expect(saveButton()).toBeDisabled();
      expect(putToOpenElisServerFullResponse).not.toHaveBeenCalled();
    });
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
