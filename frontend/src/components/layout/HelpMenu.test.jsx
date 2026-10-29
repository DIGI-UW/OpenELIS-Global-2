import React from "react";
import { fireEvent, render } from "@testing-library/react";
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

  describe("manual button routing (FRS AC 12 kill switch)", () => {
    const mockProperties = (properties) => {
      getFromOpenElisServer.mockImplementation((url, callback) => {
        if (url === "/rest/properties") callback(properties);
      });
    };

    const renderOpen = () => {
      const toggle = vi.fn();
      const { getByText } = renderWithIntl(
        <HelpMenu helpOpen={true} handlePanelToggle={toggle} />,
      );
      return { getByText, toggle };
    };

    const clickManual = (getByText) => {
      fireEvent.click(getByText("User Manual"));
    };

    let openSpy;
    beforeEach(() => {
      openSpy = vi.spyOn(window, "open").mockImplementation(() => null);
    });
    afterEach(() => {
      openSpy.mockRestore();
    });

    test("opens /help in a new tab when useInApp is absent (default true)", () => {
      mockProperties({
        "org.openelisglobal.help.manual.url": "https://example.com/manual",
      });
      const { getByText, toggle } = renderOpen();
      clickManual(getByText);
      expect(openSpy).toHaveBeenCalledWith("/help", "_blank");
      expect(toggle).toHaveBeenCalledWith("");
    });

    test("opens /help when useInApp is explicitly true", () => {
      mockProperties({
        "org.openelisglobal.help.manual.url": "https://example.com/manual",
        "org.openelisglobal.help.manual.useInApp": "true",
      });
      const { getByText } = renderOpen();
      clickManual(getByText);
      expect(openSpy).toHaveBeenCalledWith("/help", "_blank");
    });

    test("falls back to the legacy manual URL when useInApp=false", () => {
      mockProperties({
        "org.openelisglobal.help.manual.url": "https://example.com/manual",
        "org.openelisglobal.help.manual.useInApp": "false",
      });
      const { getByText } = renderOpen();
      clickManual(getByText);
      expect(openSpy).toHaveBeenCalledWith(
        "https://example.com/manual",
        "_blank",
      );
    });

    test("accepts upper/mixed case 'FALSE' as the kill-switch value", () => {
      mockProperties({
        "org.openelisglobal.help.manual.url": "https://example.com/manual",
        "org.openelisglobal.help.manual.useInApp": "FALSE",
      });
      const { getByText } = renderOpen();
      clickManual(getByText);
      expect(openSpy).toHaveBeenCalledWith(
        "https://example.com/manual",
        "_blank",
      );
    });

    test("does not open anything when useInApp=false and the legacy URL is empty", () => {
      mockProperties({
        "org.openelisglobal.help.manual.useInApp": "false",
      });
      const { getByText, toggle } = renderOpen();
      clickManual(getByText);
      expect(openSpy).not.toHaveBeenCalled();
      expect(toggle).not.toHaveBeenCalled();
    });
  });
});
