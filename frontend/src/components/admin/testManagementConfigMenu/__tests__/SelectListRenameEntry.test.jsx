import React from "react";
import { render, screen, fireEvent, cleanup } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";
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

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
}));

vi.mock("../../../layout/Layout", () => ({
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
      <SelectListRenameEntry />
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

describe("SelectListRenameEntry", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    serverWith();
  });

  afterEach(cleanup);

  it("lists the options to rename", () => {
    renderPage();

    expect(screen.getByText("Positive")).toBeInTheDocument();
    expect(screen.getByText("Negative")).toBeInTheDocument();
  });

  it("reads the stored translations when an option is opened", () => {
    renderPage();

    fireEvent.click(screen.getByText("Positive"));

    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/EntityNamesProvider?entityId=101&entityName=resultSelectOption",
      expect.any(Function),
    );
  });

  it("offers a field per language, prefilled from what is stored", () => {
    renderPage();

    fireEvent.click(screen.getByText("Positive"));

    expect(english()).toHaveValue("Positive");
    expect(french()).toHaveValue("Positif");
  });

  it("sends the stored French when only the English was edited", () => {
    renderPage();
    fireEvent.click(screen.getByText("Positive"));

    fireEvent.change(english(), { target: { value: "Reactive" } });

    expect(submit()).toMatchObject({
      resultSelectOptionId: "101",
      nameEnglish: "Reactive",
      nameFrench: "Positif",
    });
  });

  it("sends the stored English when only the French was edited", () => {
    renderPage();
    fireEvent.click(screen.getByText("Positive"));

    fireEvent.change(french(), { target: { value: "Reactif" } });

    expect(submit()).toMatchObject({
      nameEnglish: "Positive",
      nameFrench: "Reactif",
    });
  });

  it("sends both when both were edited", () => {
    renderPage();
    fireEvent.click(screen.getByText("Positive"));

    fireEvent.change(english(), { target: { value: "Detected" } });
    fireEvent.change(french(), { target: { value: "Detecte" } });

    expect(submit()).toMatchObject({
      nameEnglish: "Detected",
      nameFrench: "Detecte",
    });
  });

  it("never submits the displayed name as the French one", () => {
    // An option with no French stored: the field is empty rather than seeded with
    // the English text, so saving cannot invent a translation.
    serverWith({ names: { name: { english: "Negative" } } });
    renderPage();

    fireEvent.click(screen.getByText("Negative"));

    expect(french()).toHaveValue("");
    expect(submit().nameFrench).toBe("");
  });

  it("still opens when the option has no stored translations at all", () => {
    // The endpoint 404s with an error body for an option that was never
    // localized. The modal has to show the name it does know, not a spinner.
    serverWith({ names: { status: "invalid", message: "not found" } });
    renderPage();

    fireEvent.click(screen.getByText("Negative"));

    expect(english()).toHaveValue("Negative");
    expect(french()).toHaveValue("");
  });
});
