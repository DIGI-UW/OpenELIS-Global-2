import React from "react";
import { render, screen, fireEvent, cleanup } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import HelpMenu from "./HelpMenu";
import messages from "../../languages/en.json";
import { getFromOpenElisServer } from "../utils/Utils";

vi.mock("../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

// Replaced inline utils require

const renderWithIntl = (component) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );

describe("HelpMenu", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });
  test.each([
    "javascript:alert(1)",
    "java\nscript:alert(1)",
    "data:text/html,test",
    "file:///tmp/help",
    "blob:https://example.org/id",
    {},
    "https://[",
  ])("does not open unsafe configured URLs: %s", (value) => {
    getFromOpenElisServer.mockImplementation((_, callback) =>
      callback({ "org.openelisglobal.help.manual.url": value }),
    );
    const open = vi.spyOn(window, "open").mockImplementation(() => null);
    renderWithIntl(<HelpMenu helpOpen handlePanelToggle={() => {}} />);
    const button = screen.getByRole("button", {
      name: messages["banner.menu.help.usermanual"],
    });
    expect(button).toBeDisabled();
    fireEvent.click(button);
    expect(open).not.toHaveBeenCalled();
  });
  test.each([
    "https://example.org/manual",
    "http://example.org/manual",
    "/manual",
  ])("opens a web help URL with an isolated window: %s", (value) => {
    getFromOpenElisServer.mockImplementation((_, callback) =>
      callback({ "org.openelisglobal.help.manual.url": value }),
    );
    const open = vi.spyOn(window, "open").mockImplementation(() => null);
    renderWithIntl(<HelpMenu helpOpen handlePanelToggle={() => {}} />);
    fireEvent.click(
      screen.getByRole("button", {
        name: messages["banner.menu.help.usermanual"],
      }),
    );
    expect(open).toHaveBeenCalledWith(
      new URL(value, window.location.origin).href,
      "_blank",
      "noopener,noreferrer",
    );
  });
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test("does not crash when /rest/properties returns non-JSON (undefined)", () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/properties") {
        callback(undefined);
      }
    });

    expect(() =>
      renderWithIntl(
        <HelpMenu helpOpen={false} handlePanelToggle={() => {}} />,
      ),
    ).not.toThrow();
  });

  test("does not crash when /rest/properties returns a valid object", () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/properties") {
        callback({
          "org.openelisglobal.help.manual.url": "https://example.com/manual",
          "org.openelisglobal.help.tutorials.url":
            "https://example.com/tutorials",
          "org.openelisglobal.help.release-notes.url":
            "https://example.com/release-notes",
        });
      }
    });

    expect(() =>
      renderWithIntl(
        <HelpMenu helpOpen={false} handlePanelToggle={() => {}} />,
      ),
    ).not.toThrow();
  });
});
