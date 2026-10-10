/**
 * Unsaved review choices travel with the user to a held result's mapping page
 * and back. Each return reloads the worklist, and the choices must survive any
 * number of such visits until the user saves.
 */
import React from "react";
import { vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { createMemoryHistory, parsePath } from "history";
import { IntlProvider } from "react-intl";
import { Link, Route, Router, Switch, useLocation } from "react-router-dom";
import messages from "../../languages/en.json";
import Index from "./Index";
import { ConfigurationContext, NotificationContext } from "../layout/Layout";

vi.mock("../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerFullResponse: vi.fn(),
  };
});

import { getFromOpenElisServer } from "../utils/Utils";

const heldResult = {
  id: "1004",
  analyzerId: "2001",
  accessionNumber: "ACC654321",
  testName: "QUAL_RESULT",
  result: "POSITIVE",
  rawTestCode: "QUAL_RESULT",
  rawResultValue: "POSITIVE",
  importIssueReason: "unknown_analyzer_result_value",
  sourceProfileId: "genexpert-astm",
  sourceProfileRevision: 3,
  readOnly: true,
  isControl: false,
  sampleGroupingNumber: 1,
};

const mappedResult = {
  id: "1005",
  analyzerId: "2001",
  accessionNumber: "ACC654321",
  testName: "MTB-RIF",
  result: "1379",
  testResultType: "D",
  dictionaryResultList: [
    { id: "1378", displayValue: "MTB DETECTED" },
    { id: "1379", displayValue: "NOT DETECTED" },
  ],
  readOnly: false,
  isControl: false,
  sampleGroupingNumber: 1,
};

/** Returns the way the mapping editor does: to `returnTo`, carrying its state. */
const MappingPage = () => {
  const location = useLocation();
  const returnTo = new URLSearchParams(location.search).get("returnTo");
  return (
    <Link to={{ ...parsePath(returnTo), state: location.state }}>
      Back to worklist
    </Link>
  );
};

const renderWorklist = () => {
  const history = createMemoryHistory({
    initialEntries: ["/AnalyzerResults?id=2001"],
  });
  render(
    <Router history={history}>
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
          <IntlProvider locale="en" messages={messages}>
            <Switch>
              <Route path="/AnalyzerResults">
                <Index />
              </Route>
              <Route path="/analyzers/:analyzerId/mapping">
                <MappingPage />
              </Route>
            </Switch>
          </IntlProvider>
        </NotificationContext.Provider>
      </ConfigurationContext.Provider>
    </Router>,
  );
  return history;
};

const acceptBox = () => document.getElementById("resultList1005.isAccepted");

const visitMappingAndReturn = async () => {
  fireEvent.click(
    await screen.findByRole("link", { name: "Review analyzer mapping" }),
  );
  fireEvent.click(
    await screen.findByRole("link", { name: "Back to worklist" }),
  );
  await waitFor(() => expect(acceptBox()).toBeInTheDocument());
};

describe("Analyzer worklist review choices across mapping visits", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((_url, callback) =>
      callback({
        type: "GeneXpert",
        resultList: [{ ...heldResult }, { ...mappedResult }],
        paging: { currentPage: 1, totalPages: 1 },
      }),
    );
  });

  it("keeps an unsaved acceptance through two visits to the mapping page", async () => {
    renderWorklist();
    await waitFor(() => expect(acceptBox()).toBeInTheDocument());
    fireEvent.click(acceptBox());
    expect(acceptBox()).toBeChecked();

    await visitMappingAndReturn();
    expect(acceptBox()).toBeChecked();

    await visitMappingAndReturn();
    expect(acceptBox()).toBeChecked();
  });

  it("shows a restored acceptance on the grouping's checkbox after its held row is recovered", async () => {
    renderWorklist();
    await waitFor(() => expect(acceptBox()).toBeInTheDocument());
    fireEvent.click(acceptBox());

    fireEvent.click(
      await screen.findByRole("link", { name: "Review analyzer mapping" }),
    );
    // Correcting the mapping recovers the held row; it now leads its grouping.
    getFromOpenElisServer.mockImplementation((_url, callback) =>
      callback({
        type: "GeneXpert",
        resultList: [
          { ...heldResult, importIssueReason: null, readOnly: false },
          { ...mappedResult },
        ],
        paging: { currentPage: 1, totalPages: 1 },
      }),
    );
    fireEvent.click(
      await screen.findByRole("link", { name: "Back to worklist" }),
    );

    const groupingAccept = await waitFor(() => {
      const box = document.getElementById("resultList1004.isAccepted");
      expect(box).toBeInTheDocument();
      return box;
    });
    expect(groupingAccept).toBeChecked();
    expect(acceptBox()).not.toBeInTheDocument();
  });
});
