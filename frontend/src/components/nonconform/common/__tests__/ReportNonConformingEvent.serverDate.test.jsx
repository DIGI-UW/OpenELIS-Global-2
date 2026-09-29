import React from "react";
import { act, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";
import { ReportNonConformingEvent } from "../ReportNonConformingEvent";
import { NotificationContext } from "../../../layout/Layout";
import { getFromOpenElisServer } from "../../../utils/Utils";
import { loadLabClock, resetLabClock } from "../../../utils/labClock";

vi.mock("../../../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
    postToOpenElisServerFormData: vi.fn(),
  };
});

// 11:30 UTC is already 6 March in Kiritimati (UTC+14) and still 5 March in
// every browser zone from UTC-11 to UTC+12, including the CI runner's.
const INSTANT = Date.parse("2031-03-05T11:30:00Z");

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
  beforeEach(async () => {
    vi.useFakeTimers({ toFake: ["Date"] });
    vi.setSystemTime(INSTANT);
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === "/rest/server-time") {
        callback({ timezone: "Pacific/Kiritimati" });
      } else if (url === "/rest/nce/generate-number") {
        callback({ nceNumber: "NCE-1" });
      } else {
        callback([]);
      }
    });
    await loadLabClock();
  });

  afterEach(() => {
    vi.useRealTimers();
    resetLabClock();
  });

  it("defaults the date of event to the lab server's today, not the browser's", async () => {
    await renderForm();
    expect(document.getElementById("date-of-event")).toHaveValue("03/06/2031");
  });
});
