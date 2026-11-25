import React, { useEffect } from "react";
import { vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import Workplan from "../Workplan";
import { ConfigurationContext, NotificationContext } from "../../layout/Layout";

const { page, printed } = vi.hoisted(() => ({
  page: {
    workplanTests: [
      {
        analysisId: "11",
        accessionNumber: "DEV0100000000000001",
        testName: "Haemoglobin",
        receivedDate: "01/09/2026",
        nextVisitDate: "15/12/2026",
      },
    ],
    paging: { currentPage: "1", totalPages: "1", totalResults: "1" },
  },
  printed: vi.fn(),
}));

vi.mock("../WorkplanSearchForm", () => ({
  default: function SearchFormStub({ createTestsList }) {
    useEffect(() => {
      createTestsList(page);
    }, []);
    return null;
  },
}));

vi.mock("../../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  getFromOpenElisServer: (url, callback) => callback(page),
  postToOpenElisServerForPDF: (url, body) => printed(url, JSON.parse(body)),
}));

vi.mock("../../common/PageBreadCrumb", () => ({ default: () => null }));

const workplan = (configurationProperties) => (
  <IntlProvider locale="en" messages={messages}>
    <ConfigurationContext.Provider value={{ configurationProperties }}>
      <NotificationContext.Provider
        value={{
          notificationVisible: false,
          setNotificationVisible: vi.fn(),
          addNotification: vi.fn(),
        }}
      >
        <Workplan type="test" />
      </NotificationContext.Provider>
    </ConfigurationContext.Provider>
  </IntlProvider>
);

// OGC-1669 review: on a page load the site configuration arrives after the
// workplan mounts, and the print dropped the next visit date.
describe("Workplan next visit date", () => {
  beforeEach(() => printed.mockClear());

  it("shows the column once the configuration arrives after the page loads", async () => {
    const { rerender } = render(workplan({}));
    await screen.findByText("DEV0100000000000001");
    expect(screen.queryByText("Date of next visit")).not.toBeInTheDocument();

    rerender(workplan({ NEXT_VISIT_DATE_ON_WORKPLAN: "true" }));

    expect(await screen.findByText("Date of next visit")).toBeInTheDocument();
    expect(screen.getByText("15/12/2026")).toBeInTheDocument();
  });

  it("sends the next visit date to the printed workplan", async () => {
    render(workplan({ NEXT_VISIT_DATE_ON_WORKPLAN: "true" }));
    fireEvent.click(
      (await screen.findAllByRole("button", { name: "Print Workplan" }))[0],
    );

    await waitFor(() => expect(printed).toHaveBeenCalled());
    expect(printed.mock.calls.at(-1)[1].workplanTests[0].nextVisitDate).toBe(
      "15/12/2026",
    );
  });
});
