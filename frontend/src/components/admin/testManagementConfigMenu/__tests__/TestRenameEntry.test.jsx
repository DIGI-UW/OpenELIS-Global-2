import React from "react";
import { render, screen, fireEvent, act } from "@testing-library/react";
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
import TestRenameEntry from "../TestRenameEntry";

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

const tests = (values) => ({
  testList: values.map((value, index) => ({ id: String(index + 1), value })),
});

const STORED = {
  name: { english: "Glucose", french: "Glucose FR" },
  reportingName: { english: "GLU", french: "GLU FR" },
};

const english = () => document.getElementById("eng");

describe("TestRenameEntry", () => {
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
              <TestRenameEntry />
            </NotificationContext.Provider>
          </QueryClientProvider>
        </IntlProvider>
      </MemoryRouter>,
    );

  // The names arrive from a second read; the list already shows the test name
  // before it lands, so the wait is on the read itself.
  const openTest = async (name) => {
    await userEvent.click(screen.getByRole("button", { name }));
    await waitFor(() =>
      expect(
        getFromOpenElisServer.mock.calls.some(([url]) =>
          url.includes("TestNamesProvider"),
        ),
      ).toBe(true),
    );
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0));
    });
  };

  beforeEach(() => {
    onServer = { list: tests(["Glucose", "Urea"]), names: STORED };
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/TestRenameEntry"))
        return callback(onServer.list);
      if (url.startsWith("/rest/TestNamesProvider"))
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

  it("lists the tests that can be renamed", async () => {
    renderScreen();

    expect(
      await screen.findByRole("button", { name: "Glucose" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Urea" })).toBeInTheDocument();
  });

  it("sends the picked test's id with the edited names", async () => {
    renderScreen();
    await screen.findByRole("button", { name: "Glucose" });
    await openTest("Glucose");

    fireEvent.change(english(), { target: { value: "Serum glucose" } });
    await userEvent.click(screen.getByRole("button", { name: "Save" }));
    await userEvent.click(screen.getByRole("button", { name: "Accept" }));

    const [, payload] = postToOpenElisServerJsonResponse.mock.calls[0];
    expect(JSON.parse(payload)).toMatchObject({
      testId: "1",
      nameEnglish: "Serum glucose",
      nameFrench: "Glucose FR",
      reportNameEnglish: "GLU",
    });
  });

  it("reads the tests again once a rename is accepted, without reloading", async () => {
    renderScreen();
    await screen.findByRole("button", { name: "Glucose" });
    await openTest("Glucose");

    postToOpenElisServerJsonResponse.mockImplementation(
      (url, payload, callback) => {
        // The other test changed too, which only a reread can show.
        onServer.list = tests(["Serum glucose", "Blood urea"]);
        callback(true);
      },
    );
    await userEvent.click(screen.getByRole("button", { name: "Save" }));
    await userEvent.click(screen.getByRole("button", { name: "Accept" }));

    await waitFor(() =>
      expect(
        screen.getByRole("button", { name: "Blood urea" }),
      ).toBeInTheDocument(),
    );
    // Renaming used to reload the document, which threw away the whole app.
    expect(reload).not.toHaveBeenCalled();
  });
});
