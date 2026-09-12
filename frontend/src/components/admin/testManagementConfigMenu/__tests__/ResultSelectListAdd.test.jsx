import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../../languages/en.json";
import { postToOpenElisServerJsonResponse } from "../../../utils/Utils";
import { createQueryClient } from "../../../utils/queryClient";
import { NotificationContext } from "../../../layout/contexts";
import ResultSelectListAdd from "../ResultSelectListAdd";

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

const english = () => document.getElementById("eng");
// The french name and the LOINC code are both rendered with id "fr", so the
// name is reached as the second textbox rather than by id.
const frenchName = () => screen.getAllByRole("textbox")[1];

describe("ResultSelectListAdd", () => {
  let reload;

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
              <ResultSelectListAdd />
            </NotificationContext.Provider>
          </QueryClientProvider>
        </IntlProvider>
      </MemoryRouter>,
    );

  const nameTheListAndContinue = async () => {
    renderScreen();
    await userEvent.type(english(), "Growth");
    await userEvent.type(frenchName(), "Croissance");
    postToOpenElisServerJsonResponse.mockImplementation(
      (url, payload, callback) =>
        callback({
          tests: [{ id: "3", description: "Culture" }],
          testDictionary: { 3: [] },
        }),
    );
    await userEvent.click(screen.getAllByRole("button", { name: "Next" })[0]);
    return screen.findByLabelText("Culture");
  };

  beforeEach(() => {
    postToOpenElisServerJsonResponse.mockReset();
    reload = vi.fn();
    Object.defineProperty(window, "location", {
      configurable: true,
      value: { ...window.location, reload, assign: vi.fn() },
    });
  });

  it("offers the tests to pick from once the list is named", async () => {
    await nameTheListAndContinue();

    const [, payload] = postToOpenElisServerJsonResponse.mock.calls[0];
    expect(JSON.parse(payload)).toMatchObject({
      nameEnglish: "Growth",
      nameFrench: "Croissance",
    });
    expect(screen.getByLabelText("Culture")).toBeInTheDocument();
  });

  it("empties the form when the entry is abandoned", async () => {
    await nameTheListAndContinue();

    await userEvent.click(screen.getAllByRole("button", { name: "Cancel" })[0]);

    await waitFor(() => expect(english()).toHaveValue(""));
    expect(screen.queryByLabelText("Culture")).not.toBeInTheDocument();
    expect(reload).not.toHaveBeenCalled();
  });

  it("empties the form once the list is saved, without reloading", async () => {
    await nameTheListAndContinue();

    await userEvent.click(screen.getByLabelText("Culture"));
    postToOpenElisServerJsonResponse.mockImplementation(
      (url, payload, callback) => callback(true),
    );
    await userEvent.click(screen.getAllByRole("button", { name: "Next" })[1]);
    await userEvent.click(screen.getByRole("button", { name: "Save" }));

    await waitFor(() => expect(english()).toHaveValue(""));
    // Saving used to reload the document, which threw away the whole app.
    expect(reload).not.toHaveBeenCalled();
  });
});
