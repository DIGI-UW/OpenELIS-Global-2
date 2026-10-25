/**
 * TranslationManagement — each locale's progress bar is named after its
 * locale, so a screen reader can tell the bars apart (Carbon requires the
 * label; OGC-1457 walk).
 */
vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerFullResponse: vi.fn(),
  putToOpenElisServerFullResponse: vi.fn(),
}));

vi.mock("../../common/PageBreadCrumb", () => ({ default: () => null }));

import React from "react";
import { fireEvent, render, screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import TranslationManagement from "./TranslationManagement";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
  putToOpenElisServerFullResponse,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import messages from "../../../languages/en.json";

beforeEach(() => {
  vi.clearAllMocks();
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url === "/rest/localizations/stats") {
      cb({
        totalEntries: 10,
        localeStats: {
          fr: {
            displayName: "Français",
            translated: 7,
            missing: 3,
            percentage: 70,
          },
          es: {
            displayName: "Español",
            translated: 2,
            missing: 8,
            percentage: 20,
          },
        },
      });
    } else if (url === "/rest/supportedlocales/active") {
      cb([]);
    } else {
      cb([]);
    }
  });
});

it("names each locale's progress bar after the locale", async () => {
  render(
    <IntlProvider locale="en" messages={messages}>
      <NotificationContext.Provider value={{ addNotification: vi.fn() }}>
        <TranslationManagement />
      </NotificationContext.Provider>
    </IntlProvider>,
  );

  expect(
    await screen.findByRole("progressbar", { name: "Français (fr)" }),
  ).toBeInTheDocument();
  expect(
    screen.getByRole("progressbar", { name: "Español (es)" }),
  ).toBeInTheDocument();
});

/** OGC-1463: another admin's translation, saved after the list loaded, stands. */
it("saves only the translations that were changed", async () => {
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url === "/rest/supportedlocales/active") {
      cb([
        { localeCode: "en", displayName: "English" },
        { localeCode: "fr", displayName: "Français" },
      ]);
    } else if (url.startsWith("/rest/localizations/stats")) {
      cb({ totalEntries: 1, localeStats: {} });
    } else if (url.startsWith("/rest/localizations")) {
      cb([
        {
          id: "7",
          description: "test name",
          translations: { en: "Glucose", fr: "Glucose FR" },
        },
      ]);
    } else {
      cb([]);
    }
  });
  putToOpenElisServerFullResponse.mockImplementation((url, body, cb) =>
    cb({ ok: true, json: () => Promise.resolve({}) }),
  );
  render(
    <IntlProvider locale="en" messages={messages}>
      <NotificationContext.Provider
        value={{ addNotification: vi.fn(), setNotificationVisible: vi.fn() }}
      >
        <TranslationManagement />
      </NotificationContext.Provider>
    </IntlProvider>,
  );

  fireEvent.click((await screen.findAllByRole("button", { name: "Edit" }))[0]);
  fireEvent.change(document.getElementById("translation-en"), {
    target: { value: "Glucose EN" },
  });
  const dialog = screen
    .getAllByRole("dialog")
    .find((d) => within(d).queryByLabelText("English (en)"));
  fireEvent.click(within(dialog).getAllByRole("button").pop());

  const [url, body] = putToOpenElisServerFullResponse.mock.calls[0];
  expect(url).toBe("/rest/localizations/7/translations");
  expect(JSON.parse(body)).toEqual({ en: "Glucose EN" });
});

/** OGC-1507: a value OpenELIS ships, with its label's origin per language. */
const shipped = (overrides = {}) => ({
  id: "9",
  description: "NCE Type: Sample collection error",
  displayKey: "nce.type.sample_collection_error",
  translations: { en: "Sample collection error", fr: "Erreur (fichier)" },
  origins: { en: "SHIPPED", fr: "SHIPPED" },
  shippedLabels: {
    en: "Sample collection error",
    fr: "Erreur de prélèvement de l'échantillon",
  },
  ...overrides,
});

const ENGLISH_AND_FRENCH = [
  { localeCode: "en", displayName: "English", fallback: true },
  { localeCode: "fr", displayName: "Français" },
];

const renderWith = (rows, locales = ENGLISH_AND_FRENCH) => {
  getFromOpenElisServer.mockImplementation((url, cb) => {
    if (url === "/rest/supportedlocales/active") {
      cb(locales);
    } else if (url.startsWith("/rest/localizations/stats")) {
      cb({ totalEntries: 1, localeStats: {} });
    } else if (url.startsWith("/rest/localizations")) {
      cb(rows);
    } else {
      cb([]);
    }
  });
  const addNotification = vi.fn();
  render(
    <IntlProvider locale="en" messages={messages}>
      <NotificationContext.Provider
        value={{ addNotification, setNotificationVisible: vi.fn() }}
      >
        <TranslationManagement />
      </NotificationContext.Provider>
    </IntlProvider>,
  );
  return addNotification;
};

const editDialog = () =>
  screen
    .getAllByRole("dialog")
    .find((d) => within(d).queryByLabelText("Français (fr)"));

it("lists a shipped value with its shipped translation and origin", async () => {
  renderWith([shipped()]);

  const row = (
    await screen.findByText("NCE Type: Sample collection error")
  ).closest("tr");
  expect(
    within(row).getByText("Erreur de prélèvement de l'échantillon"),
  ).toBeInTheDocument();
  expect(within(row).queryByText("Erreur (fichier)")).not.toBeInTheDocument();
  expect(within(row).getByText("Shipped")).toBeInTheDocument();
});

it("marks a shipped value that has no translation yet as missing", async () => {
  renderWith([
    shipped({
      shippedLabels: { en: "Barcode Issue", fr: "Barcode Issue" },
      translations: { en: "Barcode Issue" },
    }),
  ]);

  const row = (
    await screen.findByText("NCE Type: Sample collection error")
  ).closest("tr");
  expect(within(row).getByText("Missing")).toBeInTheDocument();
});

it("shows the lab's English where a language has no translation", async () => {
  renderWith([
    shipped({
      translations: { en: "Barcode unreadable" },
      origins: { en: "SITE_OVERRIDE", fr: "SHIPPED" },
      shippedLabels: { en: "Barcode Issue", fr: "Barcode Issue" },
      labels: { en: "Barcode unreadable", fr: "Barcode unreadable" },
    }),
  ]);

  const row = (
    await screen.findByText("NCE Type: Sample collection error")
  ).closest("tr");
  expect(within(row).getAllByText("Barcode unreadable")).toHaveLength(2);
  expect(within(row).queryByText("Barcode Issue")).not.toBeInTheDocument();
  expect(within(row).getByText("Missing")).toBeInTheDocument();
});

it("edits a shipped value from its shipped translation and refuses a blank override", async () => {
  renderWith([shipped()]);

  fireEvent.click((await screen.findAllByRole("button", { name: "Edit" }))[0]);
  const french = document.getElementById("translation-fr");
  expect(french.value).toBe("Erreur de prélèvement de l'échantillon");
  expect(
    within(editDialog()).getByText(
      "Shipped translation: Erreur de prélèvement de l'échantillon",
    ),
  ).toBeInTheDocument();

  fireEvent.change(french, { target: { value: "  " } });
  expect(within(editDialog()).getByText(/cannot be blank/)).toBeInTheDocument();
  const save = within(editDialog()).getByRole("button", { name: "Save" });
  expect(save).toBeDisabled();

  fireEvent.change(french, { target: { value: "Prélèvement mal fait" } });
  putToOpenElisServerFullResponse.mockImplementation((url, body, cb) =>
    cb({ ok: true, json: () => Promise.resolve({}) }),
  );
  fireEvent.click(within(editDialog()).getByRole("button", { name: "Save" }));
  const [url, body] = putToOpenElisServerFullResponse.mock.calls[0];
  expect(url).toBe("/rest/localizations/9/translations");
  expect(JSON.parse(body)).toEqual({ fr: "Prélèvement mal fait" });
});

it("edits a shipped value while a regional language is active", async () => {
  renderWith(
    [
      shipped({
        origins: { en: "SHIPPED", fr: "SHIPPED", "fr-HT": "SHIPPED" },
        shippedLabels: {
          en: "Sample collection error",
          fr: "Erreur de prélèvement de l'échantillon",
          "fr-HT": "Erreur de prélèvement de l'échantillon",
        },
      }),
    ],
    [...ENGLISH_AND_FRENCH, { localeCode: "fr-HT", displayName: "Haïti" }],
  );

  fireEvent.click((await screen.findAllByRole("button", { name: "Edit" }))[0]);
  expect(document.getElementById("translation-fr-HT").value).toBe(
    "Erreur de prélèvement de l'échantillon",
  );
  fireEvent.change(document.getElementById("translation-fr"), {
    target: { value: "Prélèvement mal fait" },
  });
  expect(
    within(editDialog()).getByRole("button", { name: "Save" }),
  ).toBeEnabled();
});

it("returns an override to the shipped translation after a confirmation", async () => {
  const addNotification = renderWith([
    shipped({
      translations: { en: "Sample collection error", fr: "Mon libellé" },
      origins: { en: "SHIPPED", fr: "SITE_OVERRIDE" },
    }),
  ]);
  postToOpenElisServerFullResponse.mockImplementation((url, body, cb) =>
    cb({ ok: true, json: () => Promise.resolve(shipped()) }),
  );

  fireEvent.click((await screen.findAllByRole("button", { name: "Edit" }))[0]);
  expect(document.getElementById("translation-fr").value).toBe("Mon libellé");
  const frenchRow = screen.getByTestId("translation-row-fr");
  expect(within(frenchRow).getByText("Site override")).toBeInTheDocument();
  expect(
    within(screen.getByTestId("translation-row-en")).queryByRole("button", {
      name: "Reset to shipped translation",
    }),
  ).not.toBeInTheDocument();

  fireEvent.click(
    within(frenchRow).getByRole("button", {
      name: "Reset to shipped translation",
    }),
  );
  expect(postToOpenElisServerFullResponse).not.toHaveBeenCalled();
  fireEvent.click(
    within(screen.getByTestId("translation-row-fr")).getByRole("button", {
      name: "Reset",
    }),
  );

  expect(postToOpenElisServerFullResponse.mock.calls[0][0]).toBe(
    "/rest/localizations/9/translations/fr/reset",
  );
  expect(
    await within(screen.getByTestId("translation-row-fr")).findByText(
      "Shipped",
    ),
  ).toBeInTheDocument();
  expect(document.getElementById("translation-fr").value).toBe(
    "Erreur de prélèvement de l'échantillon",
  );
  expect(addNotification).toHaveBeenCalledWith(
    expect.objectContaining({ kind: "success" }),
  );
});
