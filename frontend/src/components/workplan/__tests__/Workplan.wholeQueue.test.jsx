import React, { useEffect } from "react";
import { vi } from "vitest";
import { act, fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import Workplan from "../Workplan";
import { ConfigurationContext, NotificationContext } from "../../layout/Layout";

const { pages, printed, show } = vi.hoisted(() => {
  const paging = { currentPage: "1", totalPages: "2", totalResults: "3" };
  const row = (analysisId, accessionNumber, testName) => ({
    analysisId,
    accessionNumber,
    testName,
    receivedDate: "01/09/2026",
  });
  return {
    pages: {
      1: {
        workplanTests: [
          row("11", "DEV0100000000000001", "Haemoglobin"),
          row("12", "DEV0100000000000001", "Glucose"),
        ],
        paging,
      },
      2: {
        workplanTests: [row("21", "DEV0100000000000002", "Creatinine")],
        paging: { ...paging, currentPage: "2" },
      },
    },
    printed: vi.fn(),
    show: { page: null, failPage: null },
  };
});

vi.mock("../WorkplanSearchForm", () => ({
  default: function SearchFormStub({
    createTestsList,
    registerPageLoader,
    registerPageSource,
  }) {
    useEffect(() => {
      registerPageSource("/rest/WorkPlanByTest?test_id=5");
      registerPageLoader((n) => createTestsList(pages[n]));
      show.page = (n) => createTestsList(pages[n]);
      createTestsList(pages[1]);
    }, []);
    return null;
  },
}));

vi.mock("../../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  getFromOpenElisServer: (url, callback) => {
    const page = Number(new URL(url, "http://x").searchParams.get("page"));
    callback(page === show.failPage ? undefined : pages[page]);
  },
  postToOpenElisServerForPDF: (url, body) => printed(url, JSON.parse(body)),
}));

vi.mock("../../common/PageBreadCrumb", () => ({ default: () => null }));

const addNotification = vi.fn();

const renderWorkplan = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <ConfigurationContext.Provider value={{ configurationProperties: {} }}>
        <NotificationContext.Provider
          value={{
            notificationVisible: false,
            setNotificationVisible: vi.fn(),
            addNotification,
          }}
        >
          <Workplan type="test" />
        </NotificationContext.Provider>
      </ConfigurationContext.Provider>
    </IntlProvider>,
  );

const printedRows = () => printed.mock.calls.at(-1)[1].workplanTests;

// OGC-1647: Print sent only the page on screen and "Total Tests" counted it.
describe("Workplan across pages", () => {
  beforeEach(() => {
    printed.mockClear();
    addNotification.mockClear();
    show.failPage = null;
  });

  it("counts every test in the workplan", async () => {
    renderWorkplan();

    expect(await screen.findByText(/Total Tests\s*=\s*3/)).toBeInTheDocument();
  });

  it("prints the tests of every page", async () => {
    renderWorkplan();
    fireEvent.click(
      (await screen.findAllByRole("button", { name: "Print Workplan" }))[0],
    );

    await waitFor(() => expect(printed).toHaveBeenCalled());
    expect(printedRows().map((row) => row.testName)).toEqual([
      "Haemoglobin",
      "Glucose",
      "Creatinine",
    ]);
  });

  it("keeps a removed test removed on another page and in the print", async () => {
    const { container } = renderWorkplan();
    const glucose = await waitFor(() => {
      const box = container.querySelector("#includedCheck_1");
      expect(box).not.toBeNull();
      return box;
    });
    fireEvent.click(glucose);
    expect(glucose).toBeChecked();

    act(() => show.page(2));
    expect(container.querySelector("#includedCheck_0")).not.toBeChecked();
    act(() => show.page(1));
    expect(container.querySelector("#includedCheck_1")).toBeChecked();

    fireEvent.click(
      screen.getAllByRole("button", { name: "Print Workplan" })[0],
    );
    await waitFor(() => expect(printed).toHaveBeenCalled());
    expect(
      printedRows().map((row) => [row.testName, row.notIncludedInWorkplan]),
    ).toEqual([
      ["Haemoglobin", false],
      ["Glucose", true],
      ["Creatinine", false],
    ]);
  });

  it("prints nothing and says why when a page cannot be loaded", async () => {
    show.failPage = 2;
    renderWorkplan();
    fireEvent.click(
      (await screen.findAllByRole("button", { name: "Print Workplan" }))[0],
    );

    await waitFor(() => expect(addNotification).toHaveBeenCalled());
    expect(addNotification.mock.calls.at(-1)[0].message).toBe(
      "Not every page of the workplan could be loaded, so nothing was printed. Try again.",
    );
    expect(printed).not.toHaveBeenCalled();
  });
});
