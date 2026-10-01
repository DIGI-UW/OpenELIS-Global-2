/**
 * A deep link to one order's results (/result?type=order&accessionNumber=…)
 * that omits doRange must still search with a valid doRange; sending the text
 * "null" made the first results request fail with a 400.
 */
import React from "react";
import { vi } from "vitest";
import { render } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../languages/en.json";
import ResultSearchPage from "./SearchResultForm";
import { ConfigurationContext, NotificationContext } from "../layout/Layout";

vi.mock("../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
  };
});

import { getFromOpenElisServer } from "../utils/Utils";

describe("Results deep link without doRange", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (typeof callback === "function") {
        callback(url.startsWith("/rest/LogbookResults") ? {} : []);
      }
    });
    window.history.pushState(
      {},
      "",
      "/result?type=order&accessionNumber=DEV0126",
    );
  });

  afterEach(() => {
    window.history.pushState({}, "", "/");
  });

  it("searches the order with doRange=false instead of doRange=null", async () => {
    render(
      <MemoryRouter
        initialEntries={["/result?type=order&accessionNumber=DEV0126"]}
      >
        <ConfigurationContext.Provider
          value={{ configurationProperties: { AccessionFormat: "" } }}
        >
          <NotificationContext.Provider
            value={{
              notificationVisible: false,
              setNotificationVisible: vi.fn(),
              addNotification: vi.fn(),
            }}
          >
            <IntlProvider locale="en" messages={messages}>
              <ResultSearchPage />
            </IntlProvider>
          </NotificationContext.Provider>
        </ConfigurationContext.Provider>
      </MemoryRouter>,
    );
    await waitFor(() => {
      const searches = getFromOpenElisServer.mock.calls
        .map(([url]) => url)
        .filter((url) => url.startsWith("/rest/LogbookResults"));
      expect(searches.length).toBeGreaterThan(0);
      expect(searches.every((url) => url.includes("doRange=false"))).toBe(true);
    });
  });
});
