/**
 * The dictionary category is chosen by typing part of its name (OGC-1515), and
 * the entry cannot be sent without the category and Is Active the server
 * requires.
 */
import React from "react";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../languages/en.json";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";
import { ConfigurationContext, NotificationContext } from "../../layout/Layout";
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

const CATEGORIES = [
  {
    id: "11",
    description: "Sample Container",
    categoryName: "Sample Container",
  },
  {
    id: "12",
    description: "Résultats qualitatifs",
    categoryName: "Qualitative",
  },
  { id: "13", description: "General", categoryName: "General" },
];

const ENTRY = {
  id: "1",
  dictEntry: "Entry A",
  localAbbreviation: "EA",
  isActive: "Y",
  loincCode: "",
  dictionaryCategory: CATEGORIES[1],
  lastupdated: "2026-01-01",
};

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

const optionNames = (combobox) =>
  within(document.getElementById(combobox.getAttribute("aria-controls")))
    .queryAllByRole("option")
    .map((option) => option.textContent);

const openAdd = async () => {
  renderScreen();
  await screen.findByText("Entry A");
  await userEvent.click(document.querySelector('[data-cy="addButton"]'));
  return screen.getByRole("dialog");
};

describe("Dictionary category search", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/dictionary-categories") return callback(CATEGORIES);
      if (url.startsWith("/rest/Dictionary?ID=")) return callback(ENTRY);
      if (url.startsWith("/rest/DictionaryMenu"))
        return callback({ menuList: [ENTRY] });
      return callback([]);
    });
    postToOpenElisServerFullResponse.mockReset();
  });

  it("narrows the categories to what is typed, ignoring case and accents", async () => {
    const dialog = await openAdd();

    const category = within(dialog).getByRole("combobox", {
      name: /Dictionary Category/,
    });
    await userEvent.type(category, "RESUL");

    expect(optionNames(category)).toEqual(["Résultats qualitatifs"]);
  });

  it("sends the chosen category and offers the container population for a sample container", async () => {
    const dialog = await openAdd();
    const category = within(dialog).getByRole("combobox", {
      name: /Dictionary Category/,
    });

    await userEvent.type(category, "contai");
    await userEvent.click(
      screen.getByRole("option", { name: "Sample Container" }),
    );
    expect(category).toHaveValue("Sample Container");
    expect(
      within(dialog).getByText(messages["dictionary.containerPopulation"]),
    ).toBeInTheDocument();

    await userEvent.type(document.getElementById("dictEntry"), "Tube B");
    await userEvent.type(document.getElementById("localAbbrev"), "TB");
    await userEvent.click(
      within(dialog).getByRole("combobox", { name: /Is Active/ }),
    );
    await userEvent.click(screen.getByRole("option", { name: "Y" }));
    await userEvent.click(within(dialog).getByRole("button", { name: "Add" }));

    expect(postToOpenElisServerFullResponse).toHaveBeenCalledOnce();
    const body = JSON.parse(postToOpenElisServerFullResponse.mock.calls[0][1]);
    expect(body.selectedDictionaryCategoryId).toBe("11");
    expect(body.isActive).toBe("Y");
  });

  it("marks the category and Is Active required and will not send an entry without them", async () => {
    const dialog = await openAdd();

    await userEvent.type(document.getElementById("dictEntry"), "Entry B");
    await userEvent.type(document.getElementById("localAbbrev"), "EB");

    const add = within(dialog).getByRole("button", { name: "Add" });
    expect(add).toBeDisabled();
    const category = within(dialog).getByRole("combobox", {
      name: /Dictionary Category/,
    });
    expect(category).toHaveAttribute("aria-required", "true");
    expect(category.labels[0]).toHaveTextContent("Dictionary Category *");
    expect(
      within(dialog).getByText("Is Active").parentElement,
    ).toHaveTextContent("Is Active *");
    await userEvent.type(category, "Gener");
    await userEvent.click(screen.getByRole("option", { name: "General" }));
    expect(add).toBeDisabled();

    await userEvent.clear(category);
    await userEvent.tab();
    expect(
      within(dialog).getByText(messages["dictionary.error.categoryRequired"]),
    ).toBeInTheDocument();

    await userEvent.click(add);
    expect(postToOpenElisServerFullResponse).not.toHaveBeenCalled();
  });

  it("shows the saved category when an entry is modified", async () => {
    renderScreen();
    await screen.findByText("Entry A");

    await userEvent.click(screen.getByLabelText("selectRow"));
    await userEvent.click(document.querySelector('[data-cy="modifyButton"]'));

    const dialog = screen.getByRole("dialog");
    expect(
      within(dialog).getByRole("combobox", { name: /Dictionary Category/ }),
    ).toHaveValue("Résultats qualitatifs");
  });
});
