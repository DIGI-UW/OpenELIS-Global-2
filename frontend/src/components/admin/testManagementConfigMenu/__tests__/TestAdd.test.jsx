import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
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
import TestAdd from "../TestAdd";

vi.mock("../../../utils/Utils", async () => {
  const actual = await vi.importActual("../../../utils/Utils");
  return {
    ...actual,
    getFromOpenElisServer: vi.fn((url, callback) =>
      callback(
        url.startsWith("/rest/TestAdd")
          ? {
              labUnitList: [],
              panelList: [],
              uomList: [],
              resultTypeList: [],
              sampleTypeList: [],
              environmentalSampleTypeIds: [],
              groupedDictionaryList: [],
              dictionaryList: [],
              ageRangeList: [],
            }
          : [],
      ),
    ),
    postToOpenElisServerJsonResponse: vi.fn(),
  };
});

const testNameEn = () => document.getElementById("testNameEn");

describe("TestAdd", () => {
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
              <TestAdd />
            </NotificationContext.Provider>
          </QueryClientProvider>
        </IntlProvider>
      </MemoryRouter>,
    );

  beforeEach(() => {
    postToOpenElisServerJsonResponse.mockReset();
    reload = vi.fn();
    Object.defineProperty(window, "location", {
      configurable: true,
      value: { ...window.location, reload, assign: vi.fn() },
    });
  });

  it("empties the form when the entry is abandoned", async () => {
    renderScreen();
    await waitFor(() => expect(testNameEn()).toBeInTheDocument());

    fireEvent.change(testNameEn(), { target: { value: "Serum glucose" } });
    expect(testNameEn()).toHaveValue("Serum glucose");

    await userEvent.click(screen.getByRole("button", { name: "Cancel" }));

    // Cancelling used to reload the document, which threw away the whole app.
    await waitFor(() => expect(testNameEn()).toHaveValue(""));
    expect(reload).not.toHaveBeenCalled();
  });
});
