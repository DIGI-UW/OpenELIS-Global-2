/**
 * TranslationManagement — each locale's progress bar is named after its
 * locale, so a screen reader can tell the bars apart (Carbon requires the
 * label; OGC-1457 walk).
 */
vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
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
