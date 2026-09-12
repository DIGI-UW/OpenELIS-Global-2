import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { QueryClientProvider } from "@tanstack/react-query";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../../languages/en.json";
import { NotificationContext } from "../../../layout/contexts";
import { fetchFromOpenElisServer } from "../../../utils/Utils";
import { createQueryClient } from "../../../utils/queryClient";
import UomCreate from "../UomCreate";
import PanelTestAssign from "../PanelTestAssign";
import SampleTypeTestAssign from "../SampleTypeTestAssign";
import TestSectionTestAssign from "../TestSectionTestAssign";
import TestActivation from "../TestActivation";
import TestOrderability from "../TestOrderability";

vi.mock("../../../utils/Utils", async () => ({
  ...(await vi.importActual("../../../utils/Utils")),
  fetchFromOpenElisServer: vi.fn(),
}));

describe("Required administration reads", () => {
  beforeEach(() => {
    fetchFromOpenElisServer
      .mockReset()
      .mockRejectedValue(new Error("HTTP 500"));
  });

  it.each([
    ["units", UomCreate],
    ["panel assignments", PanelTestAssign],
    ["sample type assignments", SampleTypeTestAssign],
    ["section assignments", TestSectionTestAssign],
    ["activation", TestActivation],
    ["orderability", TestOrderability],
  ])(
    "shows an actionable error instead of a spinner for %s",
    async (_, Component) => {
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
                <Component />
              </NotificationContext.Provider>
            </QueryClientProvider>
          </IntlProvider>
        </MemoryRouter>,
      );
      const retry = await screen.findByRole("button", { name: "Retry" });
      expect(
        screen.getByText(messages["server.error.msg"]),
      ).toBeInTheDocument();
      expect(document.querySelector(".cds--loading")).not.toBeInTheDocument();
      const reads = fetchFromOpenElisServer.mock.calls.length;
      await userEvent.click(retry);
      await waitFor(() =>
        expect(fetchFromOpenElisServer).toHaveBeenCalledTimes(reads + 1),
      );
      expect(
        await screen.findByRole("button", { name: "Retry" }),
      ).toBeInTheDocument();
    },
  );
});
