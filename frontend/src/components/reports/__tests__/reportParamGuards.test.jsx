import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import messages from "../../../languages/en.json";
import { NotificationContext } from "../../layout/contexts";
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

/**
 * Each report screen is reached with the report named in the query string. When
 * it is missing the screen sends the browser to its own default, which used to
 * mean downloading the whole app again to reach a route the router serves.
 */
const SCREENS = [
  { name: "ReportIndex", Screen: ReportIndex, at: "/ReportIndex", target: "/" },
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
          <NotificationContext.Provider
            value={{
              notificationVisible: false,
              setNotificationVisible: vi.fn(),
              addNotification: vi.fn(),
            }}
          >
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
