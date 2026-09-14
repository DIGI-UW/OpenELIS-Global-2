/**
 * Cytology, Pathology, Immunohistochemistry and the program dashboard each open
 * a row's detail screen the same way: window.location.href to a route the
 * in-app router already serves. Confirms each now navigates through the
 * router instead of downloading the app again.
 */
import React from "react";
import { vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import messages from "../../languages/en.json";
import { NotificationContext } from "../layout/contexts";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import CytologyDashBoard from "../cytology/CytologyDashBoard";
import PathologyDashboard from "../pathology/PathologyDashboard";
import ImmunohistochemistryDashboard from "../immunohistochemistry/ImmunohistochemistryDashboard";
import ProgramDashboard from "../program/programDashboard";

vi.mock("../utils/Utils", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerFullResponse: vi.fn(),
  };
});

import { getFromOpenElisServer } from "../utils/Utils";

const SCREENS = [
  {
    name: "CytologyDashBoard",
    Screen: CytologyDashBoard,
    at: "/CytologyDashboard",
    dashboardEndpoint: "/rest/cytology/dashboard?",
    entries: [
      { pathologySampleId: 9, labNumber: "ACC9", patientName: "A", status: "" },
    ],
    target: "/CytologyCaseView/9",
    targetText: "arrived at cytology case view",
  },
  {
    name: "PathologyDashboard",
    Screen: PathologyDashboard,
    at: "/PathologyDashboard",
    dashboardEndpoint: "/rest/pathology/dashboard?",
    entries: [
      { pathologySampleId: 9, labNumber: "ACC9", patientName: "A", status: "" },
    ],
    target: "/PathologyCaseView/9",
    targetText: "arrived at pathology case view",
  },
  {
    name: "ImmunohistochemistryDashboard",
    Screen: ImmunohistochemistryDashboard,
    at: "/ImmunohistochemistryDashboard",
    dashboardEndpoint: "/rest/immunohistochemistry/dashboard?",
    entries: [
      {
        immunohistochemistrySampleId: 9,
        labNumber: "ACC9",
        patientName: "A",
        status: "",
      },
    ],
    target: "/ImmunohistochemistryCaseView/9",
    targetText: "arrived at immunohistochemistry case view",
  },
];

const renderScreen = (Screen, at) =>
  render(
    <MemoryRouter initialEntries={[at]}>
      <IntlProvider locale="en" messages={messages}>
        <NotificationContext.Provider value={{ notificationVisible: false }}>
          <UserSessionDetailsContext.Provider
            value={{ userSessionDetails: {} }}
          >
            <Route path={at}>
              <Screen />
            </Route>
            <Route path="/CytologyCaseView/:id">
              <div>arrived at cytology case view</div>
            </Route>
            <Route path="/PathologyCaseView/:id">
              <div>arrived at pathology case view</div>
            </Route>
            <Route path="/ImmunohistochemistryCaseView/:id">
              <div>arrived at immunohistochemistry case view</div>
            </Route>
            <Route path="/programView/:id">
              <div>arrived at program view</div>
            </Route>
          </UserSessionDetailsContext.Provider>
        </NotificationContext.Provider>
      </IntlProvider>
    </MemoryRouter>,
  );

describe.each(SCREENS)(
  "$name opens the case view through the router",
  ({ Screen, at, dashboardEndpoint, entries, targetText }) => {
    let hrefWrittenTo;

    beforeEach(() => {
      hrefWrittenTo = null;
      Object.defineProperty(window, "location", {
        configurable: true,
        value: {
          ...window.location,
          get href() {
            return "http://localhost" + at;
          },
          set href(value) {
            hrefWrittenTo = value;
          },
        },
      });
      getFromOpenElisServer.mockReset();
      getFromOpenElisServer.mockImplementation((url, callback) => {
        if (url.startsWith(dashboardEndpoint)) return callback(entries);
        return callback([]);
      });
    });

    it("navigates to the case view without downloading the app again", async () => {
      renderScreen(Screen, at);

      const row = await screen.findByText("ACC9");
      fireEvent.click(row.closest("tr"));

      expect(await screen.findByText(targetText)).toBeInTheDocument();
      expect(hrefWrittenTo).toBeNull();
    });
  },
);

describe("ProgramDashboard opens the program view through the router", () => {
  let hrefWrittenTo;

  beforeEach(() => {
    hrefWrittenTo = null;
    Object.defineProperty(window, "location", {
      configurable: true,
      value: {
        ...window.location,
        get href() {
          return "http://localhost/ProgramDashboard";
        },
        set href(value) {
          hrefWrittenTo = value;
        },
      },
    });
    getFromOpenElisServer.mockReset();
    getFromOpenElisServer.mockImplementation((url, callback) => {
      if (url.startsWith("/rest/programSamplesList")) {
        return callback({
          totalEntries: 1,
          orderProgramsDashboardForm: {
            paging: { totalPages: 1, currentPage: 1 },
            orderPrograms: [
              {
                programSampleId: 9,
                patientPK: "1",
                firstName: "A",
                lastName: "B",
                programName: "Routine Testing",
                programCode: "RT",
                accessionNumber: "ACC9",
                receivedDate: "2026-01-01",
              },
            ],
          },
        });
      }
      return callback([]);
    });
  });

  it("navigates to the program view without downloading the app again", async () => {
    renderScreen(ProgramDashboard, "/ProgramDashboard");

    const row = await screen.findByText("ACC9");
    fireEvent.click(row.closest("tr"));

    expect(
      await screen.findByText("arrived at program view"),
    ).toBeInTheDocument();
    expect(hrefWrittenTo).toBeNull();
  });
});
