import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { createMemoryHistory } from "history";
import { IntlProvider } from "react-intl";
import { Route, Router, Switch } from "react-router-dom";

import messages from "../../languages/en.json";
import { ConfigurationContext, NotificationContext } from "../layout/Layout";
import Index from "./Index";

const { getFromOpenElisServer } = vi.hoisted(() => ({
  getFromOpenElisServer: vi.fn(),
}));

vi.mock("../utils/Utils", () => ({
  convertAlphaNumLabNumForDisplay: (value) => value,
  getFromOpenElisServer,
}));

vi.mock("./AnalyserResults", () => ({
  default: () => <div>Analyzer result table</div>,
}));

const renderPage = (initialEntry) => {
  const history = createMemoryHistory({ initialEntries: [initialEntry] });

  render(
    <Router history={history}>
      <IntlProvider locale="en" messages={messages}>
        <ConfigurationContext.Provider
          value={{ configurationProperties: { AccessionFormat: "" } }}
        >
          <NotificationContext.Provider
            value={{
              notificationVisible: false,
              setNotificationVisible: vi.fn(),
              addNotification: vi.fn(),
            }}
          >
            <Switch>
              <Route path="/AnalyzerResults">
                <Index />
              </Route>
              <Route path="/analyzers">
                <div>Analyzer dashboard</div>
              </Route>
            </Switch>
          </NotificationContext.Provider>
        </ConfigurationContext.Provider>
      </IntlProvider>
    </Router>,
  );

  return history;
};

describe("Analyzer results page", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
  });

  it("returns to the analyzer dashboard when no analyzer is selected", async () => {
    const history = renderPage("/AnalyzerResults");

    expect(await screen.findByText("Analyzer dashboard")).toBeInTheDocument();
    expect(history.location.pathname).toBe("/analyzers");
    expect(getFromOpenElisServer).not.toHaveBeenCalled();
  });

  it("finds an accession on a one-page analyzer worklist", async () => {
    const response = {
      type: "FluoroCycler XT",
      resultList: [{ accessionNumber: "ACC-001", sampleGroupingNumber: 1 }],
      paging: {
        totalPages: 1,
        currentPage: 1,
        searchTermToPage: [{ id: "ACC-001", value: "1" }],
      },
    };
    getFromOpenElisServer.mockImplementation((_url, callback) =>
      callback(response),
    );

    renderPage("/AnalyzerResults?id=5");

    expect(
      await screen.findByRole("heading", {
        level: 1,
        name: "Analyzer: FluoroCycler XT",
      }),
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Analyzers" })).toHaveAttribute(
      "href",
      "/analyzers",
    );
    expect(screen.getByText("FluoroCycler XT")).toHaveAttribute(
      "aria-current",
      "page",
    );

    await waitFor(() =>
      expect(getFromOpenElisServer).toHaveBeenCalledWith(
        "/rest/AnalyzerResults?id=5",
        expect.any(Function),
      ),
    );
    await userEvent.type(
      screen.getByRole("textbox", { name: "Enter Lab Number" }),
      "ACC-001",
    );
    await userEvent.click(screen.getByRole("button", { name: "Search" }));

    expect(getFromOpenElisServer).toHaveBeenLastCalledWith(
      "/rest/AnalyzerResults?id=5&page=1",
      expect.any(Function),
    );
  });
});
