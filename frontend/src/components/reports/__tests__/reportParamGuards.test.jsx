import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../languages/en.json";
import { NotificationContext } from "../../layout/Layout";
import ReportIndex from "../Index";
import RoutineIndex from "../routine/Index";
import StudyIndex from "../study/index";
import AuditTrailReportIndex from "../auditTrailReport/Index";

vi.mock("../../utils/Utils", async () => {
  const actual = await vi.importActual("../../utils/Utils");
  return {
    ...actual,
    getFromOpenElisServer: vi.fn((url, callback) => callback(undefined)),
  };
});

const notifications = {
  notificationVisible: false,
  setNotificationVisible: vi.fn(),
  addNotification: vi.fn(),
};

/**
 * Each report screen is reached with the report named in the query string. When
 * it is missing the screen sends the browser to its own default, which used to
 * mean downloading the whole app again to reach a route the router serves.
 */
const SCREENS = [
  {
    name: "RoutineIndex",
    Screen: RoutineIndex,
    at: "/RoutineReport",
    target: "/RoutineReports",
  },
  {
    name: "StudyIndex",
    Screen: StudyIndex,
    at: "/StudyReport",
    target: "/StudyReports",
  },
  {
    name: "AuditTrailReportIndex",
    Screen: AuditTrailReportIndex,
    at: "/AuditTrailReport",
    target: "/AuditTrailReport?type=system",
  },
];

describe.each(SCREENS)("$name", ({ Screen, at, target }) => {
  let hrefWrittenTo;
  let assign;

  beforeEach(() => {
    hrefWrittenTo = null;
    assign = vi.fn();
    Object.defineProperty(window, "location", {
      configurable: true,
      value: {
        ...window.location,
        search: "",
        assign,
        reload: vi.fn(),
        get href() {
          return "http://localhost/";
        },
        set href(value) {
          hrefWrittenTo = value;
        },
      },
    });
  });

  it("reaches its default through the router when the report is missing", async () => {
    const path = target.split("?")[0];
    render(
      <MemoryRouter initialEntries={[at]}>
        <IntlProvider locale="en" messages={messages}>
          <NotificationContext.Provider value={notifications}>
            <Route path={at}>
              <Screen />
            </Route>
            <Route path={path}>
              <div>arrived at {path}</div>
            </Route>
          </NotificationContext.Provider>
        </IntlProvider>
      </MemoryRouter>,
    );

    expect(await screen.findByText(`arrived at ${path}`)).toBeInTheDocument();
    expect(hrefWrittenTo).toBeNull();
    expect(assign).not.toHaveBeenCalled();
  });
});

/**
 * OGC-1053 — /Report needs both `type` and `report`. A link missing either used
 * to bounce to the Dashboard with nothing said, indistinguishable from a session
 * or permission problem. The page now stays put, names what is missing and
 * offers the report lists.
 */
describe("ReportIndex", () => {
  const renderAt = (entry) =>
    render(
      <MemoryRouter initialEntries={[entry]}>
        <IntlProvider locale="en" messages={messages}>
          <NotificationContext.Provider value={notifications}>
            <Route path="/Report">
              <ReportIndex />
            </Route>
            <Route exact path="/">
              <div>arrived at the Dashboard</div>
            </Route>
          </NotificationContext.Provider>
        </IntlProvider>
      </MemoryRouter>,
    );

  it("names the missing parameter and stays instead of going to the Dashboard", async () => {
    renderAt("/Report?type=patient");

    expect(
      await screen.findByText(messages["error.report.linkIncomplete.title"]),
    ).toBeInTheDocument();
    expect(screen.getByText(/which report to open/)).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: messages["routine.reports"] }),
    ).toHaveAttribute("href", "/RoutineReports");
    expect(
      screen.getByRole("link", { name: messages["label.study.Reports"] }),
    ).toHaveAttribute("href", "/StudyReports");
    expect(screen.queryByText("arrived at the Dashboard")).toBeNull();
  });

  it("names both parameters when the link carries neither", async () => {
    renderAt("/Report");

    expect(await screen.findByText(/which type, report to open/)).toBeInTheDocument();
    expect(screen.queryByText("arrived at the Dashboard")).toBeNull();
  });

  it("shows no such message for a complete link", async () => {
    renderAt("/Report?type=patient&report=TBPatientReport");

    expect(
      await screen.findByText(messages["routine.reports"]),
    ).toBeInTheDocument();
    expect(
      screen.queryByText(messages["error.report.linkIncomplete.title"]),
    ).toBeNull();
    expect(screen.queryByText("arrived at the Dashboard")).toBeNull();
  });
});
