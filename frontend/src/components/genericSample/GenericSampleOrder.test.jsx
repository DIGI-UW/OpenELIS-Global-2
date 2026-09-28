/**
 * The generic sample order's notebook picker offers "no notebook" once, as its
 * empty choice, and every notebook the server lists after it.
 */
import React from "react";
import { vi } from "vitest";
import { fireEvent, render } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../languages/en.json";
import GenericSampleOrder from "./GenericSampleOrder";
import { ConfigurationContext, NotificationContext } from "../layout/Layout";

vi.mock("../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn((url, callback) => {
      if (url === "/rest/notebook/list") {
        callback([{ id: "7", title: "Water survey" }]);
      } else if (url === "/rest/UomCreate") {
        callback({ existingUomList: [] });
      } else {
        callback([]);
      }
    }),
    postToOpenElisServerJsonResponse: vi.fn(),
  };
});

describe("GenericSampleOrder — notebook picker", () => {
  it("offers 'no notebook' once as the empty choice, then the notebooks", () => {
    const { container } = render(
      <MemoryRouter>
        <ConfigurationContext.Provider
          value={{ configurationProperties: { DEFAULT_DATE_LOCALE: "en-US" } }}
        >
          <NotificationContext.Provider
            value={{
              setNotificationVisible: vi.fn(),
              addNotification: vi.fn(),
            }}
          >
            <IntlProvider locale="en" messages={messages}>
              <GenericSampleOrder />
            </IntlProvider>
          </NotificationContext.Provider>
        </ConfigurationContext.Provider>
      </MemoryRouter>,
    );

    const select = container.querySelector("#notebookSelect");
    expect(select).not.toBeNull();
    const options = Array.from(select.options).map((o) => [o.value, o.text]);
    expect(options).toEqual([
      ["", "None - default fields only"],
      ["7", "Water survey"],
    ]);

    fireEvent.change(select, { target: { value: "7" } });
    expect(select.value).toBe("7");
  });
});
