import React from "react";
import { render } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import HelpMenu from "./HelpMenu";
import messages from "../../languages/en.json";

jest.mock("../utils/Utils", () => ({
  getFromOpenElisServer: jest.fn(),
}));

const { getFromOpenElisServer } = require("../utils/Utils");

const renderWithIntl = (component) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );

describe("HelpMenu", () => {
  beforeEach(() => {
    jest.clearAllMocks();
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
