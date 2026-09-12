import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../../languages/en.json";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import { createQueryClient } from "../../../utils/queryClient";
import { NotificationContext } from "../../../layout/contexts";
import PanelRenameEntry from "../PanelRenameEntry";
import SampleTypeRenameEntry from "../SampleTypeRenameEntry";
import TestSectionRenameEntry from "../TestSectionRenameEntry";
import UomRenameEntry from "../UomRenameEntry";
import MethodRenameEntry from "../MethodRenameEntry";

vi.mock("../../../utils/Utils", async () => {
  const actual = await vi.importActual("../../../utils/Utils");
  const getFromOpenElisServer = vi.fn();
  return {
    ...actual,
    getFromOpenElisServer,
    fetchFromOpenElisServer: vi.fn(
      (url) =>
        new Promise((resolve, reject) =>
          getFromOpenElisServer(url, (response) =>
            response === undefined
              ? reject(new Error("read failed"))
              : resolve(response),
          ),
        ),
    ),
    postToOpenElisServerJsonResponse: vi.fn(),
  };
});

/**
 * The five rename screens are the same screen with different nouns: read a list
 * of entries, pick one, edit its names, save. Each used to reload the document
 * to show the stored name afterwards.
 */
const SCREENS = [
  {
    name: "PanelRenameEntry",
    Screen: PanelRenameEntry,
    endPoint: "/rest/PanelRenameEntry",
    listField: "panelList",
    idField: "panelId",
  },
  {
    name: "SampleTypeRenameEntry",
    Screen: SampleTypeRenameEntry,
    endPoint: "/rest/SampleTypeRenameEntry",
    listField: "sampleTypeList",
    idField: "sampleTypeId",
  },
  {
    name: "TestSectionRenameEntry",
    Screen: TestSectionRenameEntry,
    endPoint: "/rest/TestSectionRenameEntry",
    listField: "testSectionList",
    idField: "testSectionId",
  },
  {
    name: "UomRenameEntry",
    Screen: UomRenameEntry,
    endPoint: "/rest/UomRenameEntry",
    listField: "uomList",
    idField: "uomId",
  },
  {
    name: "MethodRenameEntry",
    Screen: MethodRenameEntry,
    endPoint: "/rest/MethodRenameEntry",
    listField: "methodList",
    idField: "methodId",
  },
];

const renderScreen = (Screen) =>
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
            <Screen />
          </NotificationContext.Provider>
        </QueryClientProvider>
      </IntlProvider>
    </MemoryRouter>,
  );

describe.each(SCREENS)("$name", ({ Screen, endPoint, listField, idField }) => {
  let reload;
  let onServer;

  const listOf = (names) => ({
    [listField]: names.map((name, index) => ({
      id: String(index + 1),
      value: name,
    })),
  });

  beforeEach(() => {
    onServer = {
      list: listOf(["Chemistry", "Haematology"]),
      names: { name: { english: "Chemistry", french: "Chimie" } },
    };
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.startsWith(endPoint)) return callback(onServer.list);
      if (url.startsWith("/rest/EntityNamesProvider"))
        return callback(onServer.names);
      return callback(undefined);
    });
    postToOpenElisServerJsonResponse.mockReset();
    reload = vi.fn();
    Object.defineProperty(window, "location", {
      configurable: true,
      value: { ...window.location, reload, assign: vi.fn() },
    });
  });

  const pickChemistry = async () => {
    renderScreen(Screen);
    await userEvent.click(
      await screen.findByRole("button", { name: "Chemistry" }),
    );
    // The names only reach the inputs once the second read answers, so finding
    // the stored name there is what proves the entry was picked.
    return screen.findByDisplayValue("Chemistry");
  };

  it("lists the entries that can be renamed", async () => {
    renderScreen(Screen);

    expect(
      await screen.findByRole("button", { name: "Chemistry" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Haematology" }),
    ).toBeInTheDocument();
  });

  it("sends the picked entry's id with the edited name", async () => {
    const english = await pickChemistry();

    await userEvent.clear(english);
    await userEvent.type(english, "Serology");
    await userEvent.click(screen.getByRole("button", { name: "Save" }));
    await userEvent.click(screen.getByRole("button", { name: "Accept" }));

    expect(postToOpenElisServerJsonResponse).toHaveBeenCalledTimes(1);
    const [, payload] = postToOpenElisServerJsonResponse.mock.calls[0];
    expect(JSON.parse(payload)).toMatchObject({
      [idField]: "1",
      nameEnglish: "Serology",
      nameFrench: "Chimie",
    });
  });

  it("reads the list again once a rename is accepted, without reloading", async () => {
    await pickChemistry();

    // Accepting the rename is what changes the stored name.
    postToOpenElisServerJsonResponse.mockImplementation(
      (url, payload, callback) => {
        onServer.list = listOf(["Serology", "Haematology"]);
        callback(true);
      },
    );

    await userEvent.click(screen.getByRole("button", { name: "Save" }));
    await userEvent.click(screen.getByRole("button", { name: "Accept" }));

    await waitFor(() =>
      expect(
        screen.getByRole("button", { name: "Serology" }),
      ).toBeInTheDocument(),
    );
    // Renaming used to reload the document, which threw away the whole app.
    expect(reload).not.toHaveBeenCalled();
  });
});
