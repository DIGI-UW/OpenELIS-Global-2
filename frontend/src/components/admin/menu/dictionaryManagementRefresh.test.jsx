/**
 * Adding, editing and deactivating a dictionary entry used to reload the
 * document, which is how the table stopped showing stale data. Removing the
 * reload without a replacement left the old rows on screen after a
 * successful write.
 */
import React from "react";
import { fireEvent, render, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../languages/en.json";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";
import {
  ConfigurationContext,
  NotificationContext,
} from "../../layout/contexts";
import DictionaryManagement from "./DictionaryManagement";

vi.mock("../../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServer: vi.fn(),
    postToOpenElisServerFullResponse: vi.fn(),
  };
});

const entry = (id, dictEntry) => ({
  id: String(id),
  dictEntry,
  localAbbreviation: "",
  isActive: "Y",
  loincCode: "",
  dictionaryCategory: { categoryName: "General" },
  lastupdated: "2026-01-01",
});

/** What the server would answer the menu's browse read right now. */
let menuList;

const renderScreen = () =>
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

describe("Dictionary menu refresh", () => {
  beforeEach(() => {
    menuList = { menuList: [entry(1, "Entry A")] };
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/DictionaryMenu")) return callback(menuList);
      if (url.startsWith("/rest/SearchDictionaryMenu"))
        return callback(menuList);
      return callback([]);
    });
    postToOpenElisServer.mockReset();
    postToOpenElisServerFullResponse.mockReset();
  });

  it("stops showing a deactivated entry after the write, without a page load", async () => {
    renderScreen();
    await screen.findByText("Entry A");

    fireEvent.click(screen.getByLabelText("selectRow"));
    // Only the server knows Entry A is gone now.
    menuList = { menuList: [] };
    postToOpenElisServer.mockImplementation((url, body, callback) =>
      callback("200"),
    );
    fireEvent.click(document.querySelector('[data-cy="deactivateButton"]'));

    await waitFor(() =>
      expect(screen.queryByText("Entry A")).not.toBeInTheDocument(),
    );
    expect(
      document.querySelector('[data-cy="deactivateButton"]'),
    ).toBeDisabled();
    expect(document.querySelector('[data-cy="modifyButton"]')).toBeDisabled();
    fireEvent.click(document.querySelector('[data-cy="deactivateButton"]'));
    expect(postToOpenElisServer).toHaveBeenCalledOnce();
  });

  it("does not bring a deleted search result back when search is cleared", async () => {
    renderScreen();
    await screen.findByText("Entry A");
    fireEvent.change(screen.getByRole("searchbox"), {
      target: { value: "Entry" },
    });
    fireEvent.click(screen.getByLabelText("selectRow"));
    menuList = { menuList: [] };
    postToOpenElisServer.mockImplementation((url, body, callback) =>
      callback("200"),
    );
    fireEvent.click(document.querySelector('[data-cy="deactivateButton"]'));
    await waitFor(() =>
      expect(screen.queryByText("Entry A")).not.toBeInTheDocument(),
    );
    fireEvent.change(screen.getByRole("searchbox"), { target: { value: "" } });
    await waitFor(() =>
      expect(screen.queryByText("Entry A")).not.toBeInTheDocument(),
    );
    expect(getFromOpenElisServer.mock.calls.at(-1)[0]).toContain(
      "/rest/DictionaryMenu?",
    );
  });

  it("shows a newly added entry after the save, without a page load", async () => {
    renderScreen();
    await screen.findByText("Entry A");

    fireEvent.click(document.querySelector('[data-cy="addButton"]'));
    const dialog = screen.getByRole("dialog");

    // Only the server knows about Entry B.
    menuList = { menuList: [entry(1, "Entry A"), entry(2, "Entry B")] };
    postToOpenElisServerFullResponse.mockImplementation((url, body, callback) =>
      callback({ status: "201" }),
    );
    fireEvent.click(within(dialog).getByText("Add"));

    await waitFor(() =>
      expect(screen.queryByText("Entry B")).toBeInTheDocument(),
    );
  });
});
