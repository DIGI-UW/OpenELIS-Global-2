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
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import TranslationManagement from "./TranslationManagement";
import { getFromOpenElisServer } from "../../utils/Utils";
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
