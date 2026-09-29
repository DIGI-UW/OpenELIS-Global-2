import React from "react";
import { act, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";
import { ReportNonConformingEvent } from "../ReportNonConformingEvent";
import { NotificationContext } from "../../../layout/Layout";
import { getFromOpenElisServer } from "../../../utils/Utils";

vi.mock("../../../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
    postToOpenElisServerFormData: vi.fn(),
  };
});

const SERVER_DATE = "2031-03-05";

const renderForm = async () => {
  await act(async () =>
    render(
      <IntlProvider locale="en" messages={messages}>
        <NotificationContext.Provider
          value={{
            notificationVisible: false,
            setNotificationVisible: vi.fn(),
            addNotification: vi.fn(),
          }}
        >
          <MemoryRouter initialEntries={["/ReportNonConformingEvent"]}>
            <ReportNonConformingEvent />
          </MemoryRouter>
        </NotificationContext.Provider>
      </IntlProvider>,
    ),
  );
};

describe("ReportNonConformingEvent event date", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/server-time") {
        callback({ date: SERVER_DATE, time: "08:00" });
      } else if (url === "/rest/nce/generate-number") {
        callback({ nceNumber: "NCE-1" });
      } else {
        callback([]);
      }
    });
  });

  it("defaults the date of event to the lab server's today, not the browser's", async () => {
    await renderForm();
    expect(document.getElementById("date-of-event")).toHaveValue("03/05/2031");
  });
});
