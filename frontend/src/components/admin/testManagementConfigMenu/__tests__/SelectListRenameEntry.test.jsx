import React from "react";
import {
  render,
  screen,
  fireEvent,
  cleanup,
  act,
} from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";
import { QueryClientProvider } from "@tanstack/react-query";
import { createQueryClient } from "../../../utils/queryClient";
import SelectListRenameEntry from "../SelectListRenameEntry";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";

vi.mock("../../../common/PageBreadCrumb", () => ({
  default: function MockBreadCrumb() {
    return <div data-testid="breadcrumb" />;
  },
}));

vi.mock("../../../utils/Utils", () => {
  const getFromOpenElisServer = vi.fn();
  return {
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

vi.mock("../../../layout/contexts", () => ({
  NotificationContext: React.createContext({
    notificationVisible: false,
    setNotificationVisible: vi.fn(),
    addNotification: vi.fn(),
  }),
}));

vi.mock("../../../common/CustomNotification", () => ({
  AlertDialog: () => <div data-testid="alert" />,
  NotificationKinds: { success: "success", error: "error" },
}));

/**
 * Rename Existing Result List Options.
 *
 * The option list carries one name per option — whichever locale it was read in
 * — so the screen sent that value as every language: renaming in English
 * replaced the French translation with the English text. It now reads the stored
 * translations and offers a field for each.
 */

const OPTIONS = {
  resultSelectOptionList: [
    { id: "101", displayValue: "Positive" },
    { id: "102", displayValue: "Negative" },
  ],
};

const STORED = { name: { english: "Positive", french: "Positif" } };

/** Answer each endpoint the page asks for. */
const serverWith = ({ names = STORED, options = OPTIONS } = {}) =>
  getFromOpenElisServer.mockImplementation((url, callback) => {
    callback(url.includes("EntityNamesProvider") ? names : options);
  });

const renderPage = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <QueryClientProvider client={createQueryClient()}>
        <SelectListRenameEntry />
      </QueryClientProvider>
    </IntlProvider>,
  );

const english = () => document.getElementById("eng");
const french = () => document.getElementById("fr");

/** Save then Accept — the modal confirms before it posts. */
const submit = () => {
  fireEvent.click(screen.getByText("Save"));
  fireEvent.click(screen.getByText("Accept"));
  return JSON.parse(postToOpenElisServerJsonResponse.mock.calls[0][1]);
};

/** The options arrive from a read, so they are waited for rather than assumed. */
const showOptions = async () => {
  renderPage();
  await screen.findByText("Positive");
};

/**
 * Opening an option reads its stored translations. The English field already
 * shows the option name before that read lands, so waiting on it would let an
 * edit be made and then overwritten; the wait is on the read itself.
 */
const openOption = async (name) => {
  fireEvent.click(screen.getByText(name));
  await waitFor(() =>
    expect(
      getFromOpenElisServer.mock.calls.some(([url]) =>
        url.includes("EntityNamesProvider"),
      ),
    ).toBe(true),
  );
  await act(async () => {
    await new Promise((resolve) => setTimeout(resolve, 0));
  });
};

describe("SelectListRenameEntry", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    serverWith();
  });

  afterEach(cleanup);

  it("lists the options to rename", async () => {
    await showOptions();

    expect(screen.getByText("Positive")).toBeInTheDocument();
    expect(screen.getByText("Negative")).toBeInTheDocument();
  });

  it("reads the stored translations when an option is opened", async () => {
    await showOptions();

    await openOption("Positive");

    expect(getFromOpenElisServer.mock.calls.map(([url]) => url)).toContain(
      "/rest/EntityNamesProvider?entityId=101&entityName=resultSelectOption",
    );
  });

  it("offers a field per language, prefilled from what is stored", async () => {
    await showOptions();

    await openOption("Positive");

    expect(french()).toHaveValue("Positif");
  });

  it("sends the stored French when only the English was edited", async () => {
    await showOptions();
    await openOption("Positive");

    fireEvent.change(english(), { target: { value: "Reactive" } });

    expect(submit()).toMatchObject({
      resultSelectOptionId: "101",
      nameEnglish: "Reactive",
      nameFrench: "Positif",
    });
  });

  it("sends the stored English when only the French was edited", async () => {
    await showOptions();
    await openOption("Positive");

    fireEvent.change(french(), { target: { value: "Reactif" } });

    expect(submit()).toMatchObject({
      nameEnglish: "Positive",
      nameFrench: "Reactif",
    });
  });

  it("sends both when both were edited", async () => {
    await showOptions();
    await openOption("Positive");

    fireEvent.change(english(), { target: { value: "Detected" } });
    fireEvent.change(french(), { target: { value: "Detecte" } });

    expect(submit()).toMatchObject({
      nameEnglish: "Detected",
      nameFrench: "Detecte",
    });
  });

  it("never submits the displayed name as the French one", async () => {
    // An option with no French stored: the field is empty rather than seeded with
    // the English text, so saving cannot invent a translation.
    serverWith({ names: { name: { english: "Negative" } } });
    await showOptions();

    await openOption("Negative");

    expect(french()).toHaveValue("");
    expect(submit().nameFrench).toBe("");
  });

  it("reads the options again once a rename is saved, without reloading", async () => {
    const reload = vi.fn();
    Object.defineProperty(window, "location", {
      configurable: true,
      value: { ...window.location, reload },
    });
    await showOptions();
    await openOption("Positive");

    fireEvent.change(english(), { target: { value: "Reactive" } });
    // Saving is what changes the stored name.
    postToOpenElisServerJsonResponse.mockImplementation(
      (url, payload, callback) => {
        // The second option changed too, which only a reread can show: the
        // edit on screen touches the first one alone.
        serverWith({
          options: {
            resultSelectOptionList: [
              { id: "101", displayValue: "Reactive" },
              { id: "102", displayValue: "Nonreactive" },
            ],
          },
        });
        callback(true);
      },
    );
    submit();

    await waitFor(() =>
      expect(screen.getByText("Nonreactive")).toBeInTheDocument(),
    );
    // Renaming used to reload the document, which threw away the whole app.
    expect(reload).not.toHaveBeenCalled();
  });

  it("still opens when the option has no stored translations at all", async () => {
    // The endpoint 404s with an error body for an option that was never
    // localized. The modal has to show the name it does know, not a spinner.
    serverWith({ names: { status: "invalid", message: "not found" } });
    await showOptions();

    await openOption("Negative");

    expect(french()).toHaveValue("");
  });
});
