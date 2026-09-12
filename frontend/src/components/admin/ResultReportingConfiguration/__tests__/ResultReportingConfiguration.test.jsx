import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
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
import ResultReportingConfiguration from "../ResultReportingConfiguration";

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

const config = (enabled) => ({
  cancelMethod: "GET",
  cancelAction: "",
  formMethod: "POST",
  formName: "reporting",
  hourList: [{ id: "1", value: "01" }],
  minList: [{ id: "0", value: "00" }],
  reports: [
    { title: "label.button.save", enabled, url: "", hour: "1", min: "0" },
  ],
});

describe("ResultReportingConfiguration", () => {
  let reload;
  let onServer;

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
              <ResultReportingConfiguration />
            </NotificationContext.Provider>
          </QueryClientProvider>
        </IntlProvider>
      </MemoryRouter>,
    );

  beforeEach(() => {
    onServer = config("disable");
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) =>
      url.startsWith("/rest/ResultReportingConfiguration")
        ? callback(onServer)
        : callback(undefined),
    );
    postToOpenElisServerJsonResponse.mockReset();
    reload = vi.fn();
    Object.defineProperty(window, "location", {
      configurable: true,
      value: { ...window.location, reload, assign: vi.fn() },
    });
  });

  it("shows each report as stored", async () => {
    renderScreen();

    await waitFor(() =>
      expect(document.getElementById("enabled-0-no")).toBeChecked(),
    );
  });

  it("reads the configuration again once it is saved, without reloading", async () => {
    renderScreen();
    await waitFor(() =>
      expect(document.getElementById("enabled-0-no")).toBeChecked(),
    );

    // Turning the report on is what enables saving.
    fireEvent.click(document.getElementById("enabled-0-yes"));
    let answer;
    postToOpenElisServerJsonResponse.mockImplementation(
      (url, payload, callback) => {
        answer = () => {
          onServer = config("enable");
          callback(true);
        };
      },
    );

    await userEvent.click(screen.getByRole("button", { name: "Save" }));

    // The write is still in flight: nothing has been reloaded, which the old
    // timer did a second after sending whatever the server said.
    expect(reload).not.toHaveBeenCalled();

    answer();
    await waitFor(() =>
      expect(screen.getByRole("button", { name: "Save" })).toBeDisabled(),
    );
    expect(document.getElementById("enabled-0-yes")).toBeChecked();
    expect(reload).not.toHaveBeenCalled();
  });
});
