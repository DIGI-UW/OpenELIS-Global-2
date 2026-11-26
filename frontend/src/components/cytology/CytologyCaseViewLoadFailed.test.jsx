import React from "react";
import { vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import messages from "../../languages/en.json";
import { NotificationContext } from "../layout/Layout";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import CytologyCaseView from "./CytologyCaseView";

// The real request helper runs: only fetch is stubbed, so a 404 and a 500
// reach the view the way a browser would hand them over.
vi.mock("../common/QuestionnaireResponse", () => ({ default: () => null }));
vi.mock("../common/PriorResultsPanel", () => ({
  default: () => <div data-testid="prior-results-marker" />,
}));

const CASE_ID = "424242";
const CASE_PATH = "/rest/cytology/caseView/" + CASE_ID;
const NOTICE = messages["caseView.error.loadFailed"];
const NO_PATIENT = messages["patient.label.nopatientid"];

const GOOD_CASE = {
  labNumber: "CY6",
  patientPK: "1",
  lastName: "B",
  firstName: "A",
  status: "PREPARING_SLIDES",
  assignedTechnicianId: "1",
  slides: [],
  reports: [],
};

const jsonResponse = (status, body) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json" },
  });

const stubFetch = (caseStatus, caseBody = {}) =>
  vi.fn((url) =>
    Promise.resolve(
      String(url).endsWith(CASE_PATH)
        ? jsonResponse(caseStatus, caseBody)
        : jsonResponse(200, []),
    ),
  );

// The case answers each GET in turn from caseReads and every POST with 200.
const stubFetchInTurn = (...caseReads) =>
  vi.fn((url, init) => {
    if (String(url).endsWith(CASE_PATH)) {
      if (init?.method === "POST") {
        return Promise.resolve(jsonResponse(200, {}));
      }
      const [status, body] = caseReads.shift();
      return Promise.resolve(jsonResponse(status, body));
    }
    return stubFetch(200)(url);
  });

const renderCaseView = () =>
  render(
    <MemoryRouter initialEntries={["/CytologyCaseView/" + CASE_ID]}>
      <IntlProvider locale="en" messages={messages}>
        <NotificationContext.Provider
          value={{
            notificationVisible: false,
            setNotificationVisible: vi.fn(),
            addNotification: vi.fn(),
          }}
        >
          <UserSessionDetailsContext.Provider
            value={{
              userSessionDetails: {
                userId: "1",
                firstName: "Sam",
                lastName: "Session",
                roles: [],
              },
            }}
          >
            <Route path="/CytologyCaseView/:cytologySampleId">
              <CytologyCaseView />
            </Route>
          </UserSessionDetailsContext.Provider>
        </NotificationContext.Provider>
      </IntlProvider>
    </MemoryRouter>,
  );

describe("the cytology case view when the case cannot be read", () => {
  let originalFetch;
  let errorSpy;

  beforeEach(() => {
    originalFetch = global.fetch;
    errorSpy = vi.spyOn(console, "error").mockImplementation(() => {});
  });

  afterEach(() => {
    global.fetch = originalFetch;
    errorSpy.mockRestore();
  });

  const crashLogged = () =>
    errorSpy.mock.calls.some((args) =>
      args.some(
        (arg) =>
          arg instanceof Error ||
          String(arg).includes("Cannot read properties"),
      ),
    );

  const expectTheNoticeAndNoForm = async (status) => {
    global.fetch = stubFetch(status, { error: "x" });
    renderCaseView();

    expect(await screen.findByText(NOTICE)).toBeInTheDocument();
    expect(errorSpy).toHaveBeenCalledWith(
      `GET ${CASE_PATH} failed: HTTP ${status}`,
    );
    expect(screen.queryAllByRole("button", { name: "Save" })).toHaveLength(0);
    expect(screen.queryByText(NO_PATIENT)).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("prior-results-marker"),
    ).not.toBeInTheDocument();
    expect(crashLogged()).toBe(false);
  };

  it("anUnknownCase_showsTheNoticeAndNoForm", async () => {
    await expectTheNoticeAndNoForm(404);
  });

  it("aServerError_showsTheNoticeAndNoForm", async () => {
    await expectTheNoticeAndNoForm(500);
  });

  it("aGoodRead_showsTheFormAndNoNotice", async () => {
    global.fetch = stubFetch(200, GOOD_CASE);
    renderCaseView();

    await waitFor(() =>
      expect(
        screen.getAllByRole("button", { name: "Save" }).length,
      ).toBeGreaterThan(0),
    );
    expect(await screen.findAllByText(/CY6/)).not.toHaveLength(0);
    expect(screen.getByTestId("prior-results-marker")).toBeInTheDocument();
    expect(screen.queryByText(NOTICE)).not.toBeInTheDocument();
    expect(crashLogged()).toBe(false);
  });

  it("aFailedReReadAfterSave_keepsTheForm", async () => {
    global.fetch = stubFetchInTurn([200, GOOD_CASE], [500, { error: "x" }]);
    renderCaseView();
    expect(await screen.findAllByText(/CY6/)).not.toHaveLength(0);

    fireEvent.click(screen.getAllByRole("button", { name: "Save" })[0]);

    await waitFor(() =>
      expect(errorSpy).toHaveBeenCalledWith(
        `GET ${CASE_PATH} failed: HTTP 500`,
      ),
    );
    expect(
      screen.getAllByRole("button", { name: "Save" }).length,
    ).toBeGreaterThan(0);
    expect(screen.queryByText(NOTICE)).not.toBeInTheDocument();
    expect(crashLogged()).toBe(false);
  });
});
