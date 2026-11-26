import React from "react";
import { vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import messages from "../../languages/en.json";
import { NotificationContext } from "../layout/Layout";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import CytologyCaseView from "./CytologyCaseView";

vi.mock("../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerFullResponse: vi.fn(),
    postToOpenElisServerForPDF: vi.fn(),
    hasRole: vi.fn(() => true),
  };
});

vi.mock("../common/QuestionnaireResponse", () => ({ default: () => null }));

import { getFromOpenElisServer } from "../utils/Utils";

const CASE_ID = "6";
const CASE_URL = "/rest/cytology/caseView/" + CASE_ID;

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

describe("CytologyCaseView rollout", () => {
  test("exports a component for OGC-284 rollout", () => {
    expect(CytologyCaseView).toBeTruthy();
  });
});

describe("the prior anatomic-pathology results under the patient band", () => {
  // Every other read answers [], which the panel reads as could not load.
  it("renders the prior results panel directly after the patient header and asks this case's endpoint", async () => {
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url === CASE_URL) {
        return callback({
          labNumber: "CY6",
          patientPK: "1",
          lastName: "B",
          firstName: "A",
          status: "PREPARING_SLIDES",
          assignedTechnicianId: "1",
          slides: [],
          reports: [],
        });
      }
      return callback([]);
    });

    renderCaseView();
    await waitFor(() =>
      expect(document.querySelector(".patient-header2")).not.toBeNull(),
    );

    const panel = screen.getByTestId("prior-results").closest(".prior-results");
    expect(panel.previousElementSibling).toContainElement(
      document.querySelector(".patient-header2"),
    );
    expect(
      getFromOpenElisServer.mock.calls.filter(
        ([url]) => url === CASE_URL + "/priorResults",
      ),
    ).toHaveLength(1);
  });
});
