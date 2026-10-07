/**
 * OGC-1418 — Study Menu Configuration saves to the endpoint the server serves
 * (the trailing-slash path answered 404, so nothing here could be saved), and
 * no longer offers the retired Validation study entries.
 */
import React from "react";
import { vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../languages/en.json";
import { NotificationContext } from "../../layout/Layout";

vi.mock("../../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn((url, callback) =>
      callback({
        menu: { elementId: url.split("/").pop(), isActive: true },
        childMenus: [],
      }),
    ),
    postToOpenElisServerFullResponse: vi.fn(),
  };
});

import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";
import StudyMenuManagement from "./StudyMenuManagement";

describe("Study Menu Configuration (OGC-1418)", () => {
  it("saves to /rest/menu and leaves the retired Validation entries out", async () => {
    render(
      <MemoryRouter>
        <NotificationContext.Provider
          value={{
            notificationVisible: false,
            setNotificationVisible: vi.fn(),
            addNotification: vi.fn(),
          }}
        >
          <IntlProvider locale="en" messages={messages}>
            <StudyMenuManagement />
          </IntlProvider>
        </NotificationContext.Provider>
      </MemoryRouter>,
    );

    fireEvent.click(screen.getByRole("button", { name: /submit/i }));

    await waitFor(() =>
      expect(postToOpenElisServerFullResponse).toHaveBeenCalled(),
    );
    const [url, body] = postToOpenElisServerFullResponse.mock.calls[0];
    expect(url).toBe("/rest/menu");
    const ids = JSON.parse(body).map((item) => item.menu.elementId);
    expect(ids).not.toContain("menu_resultvalidation_study");
    expect(ids).not.toContain("menu_resultvalidation_virology");
    expect(
      getFromOpenElisServer.mock.calls.map(([fetched]) => fetched),
    ).not.toContain("/rest/menu/menu_resultvalidation_study");
  });
});
