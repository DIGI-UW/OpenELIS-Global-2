/**
 * OGC-1507: the dictionary list shows whether each entry's label is OpenELIS's
 * shipped translation, the lab's override of it, or the site's own value.
 */
import React from "react";
import { render, screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import messages from "../../../languages/en.json";
import { getFromOpenElisServer } from "../../utils/Utils";
import { ConfigurationContext, NotificationContext } from "../../layout/Layout";
import DictionaryManagement from "./DictionaryManagement";

vi.mock("../../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return { ...actual, getFromOpenElisServer: vi.fn() };
});

const entry = (id, dictEntry, displayValue, labelOrigin) => ({
  id: String(id),
  dictEntry,
  displayValue,
  labelOrigin,
  localAbbreviation: "",
  isActive: "Y",
  loincCode: "",
  dictionaryCategory: { categoryName: "Test Results" },
});

describe("Dictionary list label origin", () => {
  it("shows where each label comes from", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) =>
      url.startsWith("/rest/DictionaryMenu")
        ? callback({
            menuList: [
              entry(1, "Positive", "Positive", "SHIPPED"),
              entry(2, "Negative", "Negative", "SITE_OVERRIDE"),
              entry(3, "Lab own", "Lab own", undefined),
            ],
          })
        : callback([]),
    );
    render(
      <MemoryRouter>
        <ConfigurationContext.Provider
          value={{ configurationProperties: {}, reloadConfiguration: vi.fn() }}
        >
          <NotificationContext.Provider
            value={{
              notificationVisible: false,
              setNotificationVisible: vi.fn(),
              addNotification: vi.fn(),
            }}
          >
            <IntlProvider locale="en" messages={messages}>
              <DictionaryManagement />
            </IntlProvider>
          </NotificationContext.Provider>
        </ConfigurationContext.Provider>
      </MemoryRouter>,
    );

    const overridden = (await screen.findByText("Negative")).closest("tr");
    expect(screen.getByText("Origin")).toBeInTheDocument();
    expect(within(overridden).getByText("Site override")).toBeInTheDocument();
    expect(
      within(screen.getByText("Positive").closest("tr")).getByText("Shipped"),
    ).toBeInTheDocument();
    expect(
      within(screen.getByText("Lab own").closest("tr")).getByText("Site value"),
    ).toBeInTheDocument();
  });
});
