import React, { useContext } from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import LayoutProvider from "./LayoutProvider";
import { ConfigurationContext, NotificationContext } from "./contexts";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { getFromOpenElisServer } from "../utils/Utils";

/**
 * LayoutProvider holds the state behind ConfigurationContext and
 * NotificationContext. It carries no chrome, which is what lets /login read the
 * configuration without pulling Layout and Header onto the first-paint path, so
 * these tests render it with no Layout at all.
 *
 * @see spec.md FR-012: Preserve ConfigurationContext and NotificationContext
 */
vi.mock("../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn((url, callback) => {
    if (url === "/rest/configuration-properties") {
      callback({ releaseNumber: "3.0.0", BANNER_TEXT: "Test Lab" });
    } else if (url === "/rest/open-configuration-properties") {
      callback({ releaseNumber: "3.0.0" });
    }
  }),
}));

const renderProvider = (ui, { authenticated = true } = {}) =>
  render(
    <IntlProvider locale="en" messages={{}}>
      <UserSessionDetailsContext.Provider
        value={{ userSessionDetails: { authenticated } }}
      >
        <LayoutProvider>{ui}</LayoutProvider>
      </UserSessionDetailsContext.Provider>
    </IntlProvider>,
  );

const configurationFetches = (url) =>
  getFromOpenElisServer.mock.calls.filter(([called]) => called === url).length;

beforeEach(() => {
  vi.clearAllMocks();
});

describe("LayoutProvider", () => {
  test("hands children the configuration it fetched", async () => {
    const ConfigConsumer = () => {
      const { configurationProperties } = useContext(ConfigurationContext);
      return (
        <div data-testid="config-consumer">
          {configurationProperties.BANNER_TEXT ?? "no-banner"}
        </div>
      );
    };

    renderProvider(<ConfigConsumer />);

    await waitFor(() =>
      expect(screen.getByTestId("config-consumer")).toHaveTextContent(
        "Test Lab",
      ),
    );
  });

  test("reads the open properties for a signed-out session", async () => {
    renderProvider(<div />, { authenticated: false });

    await waitFor(() =>
      expect(configurationFetches("/rest/open-configuration-properties")).toBe(
        1,
      ),
    );
    expect(configurationFetches("/rest/configuration-properties")).toBe(0);
  });

  test("reloadConfiguration refetches once per call", async () => {
    const ConfigReloader = () => {
      const { reloadConfiguration } = useContext(ConfigurationContext);
      return (
        <button type="button" onClick={() => reloadConfiguration()}>
          Reload configuration
        </button>
      );
    };

    renderProvider(<ConfigReloader />);
    const before = configurationFetches("/rest/configuration-properties");

    fireEvent.click(screen.getByText("Reload configuration"));

    await waitFor(() =>
      expect(configurationFetches("/rest/configuration-properties")).toBe(
        before + 1,
      ),
    );
  });

  test("addNotification puts a notification in front of children", () => {
    const NotificationConsumer = () => {
      const { notifications, addNotification } =
        useContext(NotificationContext);
      return (
        <>
          <button
            type="button"
            onClick={() => addNotification({ title: "Saved" })}
          >
            Notify
          </button>
          <div data-testid="notification-consumer">
            {notifications.map((n) => n.title).join(",")}
          </div>
        </>
      );
    };

    renderProvider(<NotificationConsumer />);
    fireEvent.click(screen.getByText("Notify"));

    expect(screen.getByTestId("notification-consumer")).toHaveTextContent(
      "Saved",
    );
  });

  test("removeNotification takes the notification at that index back out", () => {
    const NotificationConsumer = () => {
      const { notifications, addNotification, removeNotification } =
        useContext(NotificationContext);
      return (
        <>
          <button
            type="button"
            onClick={() => addNotification({ title: "Saved" })}
          >
            Notify
          </button>
          <button type="button" onClick={() => removeNotification(0)}>
            Dismiss
          </button>
          <div data-testid="notification-consumer">{notifications.length}</div>
        </>
      );
    };

    renderProvider(<NotificationConsumer />);
    fireEvent.click(screen.getByText("Notify"));
    expect(screen.getByTestId("notification-consumer")).toHaveTextContent("1");

    fireEvent.click(screen.getByText("Dismiss"));

    expect(screen.getByTestId("notification-consumer")).toHaveTextContent("0");
  });
});
