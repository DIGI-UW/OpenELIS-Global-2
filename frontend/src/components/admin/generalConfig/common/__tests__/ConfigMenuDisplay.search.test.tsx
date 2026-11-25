import React from "react";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../../../languages/en.json";
import { getFromOpenElisServer } from "../../../../utils/Utils";
import { createQueryClient } from "../../../../utils/queryClient";
import {
  ConfigurationContext,
  NotificationContext,
} from "../../../../layout/Layout";
import ConfigMenuDisplay from "../ConfigMenuDisplay";

vi.mock("../../../../utils/Utils", async () => {
  const actual = await vi.importActual("../../../../utils/Utils");
  const getFromOpenElisServer = vi.fn();
  return {
    ...actual,
    getFromOpenElisServer,
    fetchFromOpenElisServer: vi.fn(
      (url: string) =>
        new Promise((resolve, reject) =>
          getFromOpenElisServer(url, (response: unknown) =>
            response === undefined
              ? reject(new Error("read failed"))
              : resolve(response),
          ),
        ),
    ),
  };
});

const MENU = "SiteInformationMenu";

const setting = (
  id: string,
  name: string,
  description: string,
  value: string,
) => ({ id, name, description, value, valueType: "text" });

const SETTINGS = [
  setting("1", "siteName", "Nom du site", "Hôpital Central"),
  setting("2", "phone", "Phone number", "+256 700 000"),
  setting("3", "resultsOnly", "Results only", "false"),
];

/**
 * A settings page is searched by typing part of a setting's name, description
 * or value (OGC-1515), and its paging counts only the settings that match.
 */
describe("ConfigMenuDisplay search", () => {
  beforeEach(() => {
    (getFromOpenElisServer as ReturnType<typeof vi.fn>).mockReset();
    (getFromOpenElisServer as ReturnType<typeof vi.fn>).mockImplementation(
      (url: string, callback: (r: unknown) => void) =>
        callback(url === `/rest/${MENU}` ? { menuList: SETTINGS } : undefined),
    );
  });

  const renderScreen = () =>
    render(
      <MemoryRouter>
        <IntlProvider locale="en" messages={messages}>
          <QueryClientProvider client={createQueryClient()}>
            <NotificationContext.Provider
              value={{
                notificationVisible: false,
                setNotificationVisible: vi.fn(),
                addNotification: vi.fn(),
              }}
            >
              <ConfigurationContext.Provider
                value={{ reloadConfiguration: vi.fn() }}
              >
                <ConfigMenuDisplay id="admin.formEntryConfig" menuType={MENU} />
              </ConfigurationContext.Provider>
            </NotificationContext.Provider>
          </QueryClientProvider>
        </IntlProvider>
      </MemoryRouter>,
    );

  const listedDescriptions = () =>
    within(screen.getByRole("table"))
      .queryAllByRole("row")
      .slice(1)
      .map((row) => within(row).getAllByRole("cell")[2].textContent);

  const search = () =>
    screen.getByRole("searchbox", {
      name: messages["admin.page.configuration.search"],
    });

  it("narrows the settings to those whose name, description or value contains what is typed", async () => {
    renderScreen();
    await screen.findByText("Nom du site");

    await userEvent.type(search(), "hopital");
    expect(listedDescriptions()).toEqual(["Nom du site"]);

    await userEvent.clear(search());
    await userEvent.type(search(), "PHONE");
    expect(listedDescriptions()).toEqual(["Phone number"]);

    await userEvent.clear(search());
    await userEvent.type(search(), "resultsonly");
    expect(listedDescriptions()).toEqual(["Results only"]);

    await userEvent.clear(search());
    expect(listedDescriptions()).toEqual([
      "Nom du site",
      "Phone number",
      "Results only",
    ]);
  });

  it("counts only the matching settings and says when none match", async () => {
    renderScreen();
    await screen.findByText("Nom du site");

    await userEvent.type(search(), "site");
    expect(screen.getByText("1-1 of 1 items")).toBeInTheDocument();

    await userEvent.clear(search());
    await userEvent.type(search(), "zzz");
    expect(listedDescriptions()).toEqual([]);
    expect(screen.getByText('Nothing matches "zzz".')).toBeInTheDocument();
  });

  it("modifies a setting chosen from the search results", async () => {
    renderScreen();
    await screen.findByText("Nom du site");

    await userEvent.type(search(), "phone");
    await userEvent.click(screen.getByLabelText("selectRow"));

    expect(screen.getByRole("button", { name: "Modify" })).toBeEnabled();
  });

  it("does not modify a chosen setting while the search hides it", async () => {
    renderScreen();
    await screen.findByText("Nom du site");

    await userEvent.type(search(), "phone");
    await userEvent.click(screen.getByLabelText("selectRow"));
    await userEvent.clear(search());
    await userEvent.type(search(), "site");

    expect(screen.getByRole("button", { name: "Modify" })).toBeDisabled();

    await userEvent.clear(search());
    expect(screen.getByRole("button", { name: "Modify" })).toBeEnabled();
  });
});
