import React from "react";
import { act, render, screen, fireEvent } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Redirect, Route } from "react-router-dom";
import { SideNav, SideNavItems } from "@carbon/react";
import ConfiguredSideNav from "./ConfiguredSideNav";
import messages from "../../languages/en.json";
import { readFileSync } from "node:fs";
import path from "node:path";
const profile = JSON.parse(
  readFileSync(
    path.resolve(
      process.cwd(),
      "../projects/reporting-uat/menu/menu_config.json",
    ),
    "utf8",
  ),
);

const menus = (definitions) =>
  definitions.map(({ childMenus = [], ...menu }) => ({
    menu: { isActive: true, ...menu },
    childMenus: menus(childMenus),
  }));

test.each([false, true])(
  "the reporting instance follows the mock sections with one Results Entry link (unified=%s)",
  (unifiedResultsOn) => {
    const { container } = render(
      <MemoryRouter
        initialEntries={[
          "/reports/custom-data-export?view=queue&job=example&uat=navigation",
        ]}
      >
        <IntlProvider locale="en" messages={messages}>
          <SideNav expanded aria-label="Side navigation">
            <SideNavItems>
              <ConfiguredSideNav
                menus={menus(profile.menus)}
                unifiedResultsOn={unifiedResultsOn}
              />
            </SideNavItems>
          </SideNav>
        </IntlProvider>
      </MemoryRouter>,
    );
    expect(
      screen.getAllByRole("heading").map((heading) => heading.textContent),
    ).toEqual(["Main Menu", "Patient & Orders", "Reports", "Administration"]);
    expect(
      screen.getByRole("link", { name: "Order Test", exact: true }),
    ).toHaveAttribute("href", "/SamplePatientEntry");
    expect(
      screen.getByRole("link", { name: "Results Entry", exact: true }),
    ).toHaveAttribute(
      "href",
      unifiedResultsOn ? "/Results" : "/PatientResults",
    );
    expect(
      screen.getByRole("link", { name: "Results Validation", exact: true }),
    ).toHaveAttribute("href", "/ResultValidation?type=&test=");
    expect(
      screen.getByRole("link", { name: "My Report Queue", exact: true }),
    ).toHaveAttribute("aria-current", "page");
    expect(container.querySelectorAll('[aria-current="page"]')).toHaveLength(1);
    expect(
      screen.getByRole("link", { name: "Custom Data Export", exact: true }),
    ).toHaveAttribute("href", "/reports/custom-data-export?uat=navigation");
    expect(
      screen.getByRole("link", { name: "My Report Queue", exact: true }),
    ).toHaveAttribute(
      "href",
      "/reports/custom-data-export?uat=navigation&view=queue",
    );
    expect(container.querySelector("#menu_home_nav svg")).toBeTruthy();
    const unavailable = container.querySelector(
      "#menu_reports_patient_print_queue_nav",
    );
    expect(unavailable).toHaveAttribute("aria-disabled", "true");
    expect(unavailable).not.toHaveAttribute("href");
    expect(unavailable).toHaveTextContent("Not yet connected");
    expect(
      screen.getByRole("button", { name: "Other reports", exact: true }),
    ).toHaveAttribute("aria-expanded", "false");
    const more = screen.getByRole("button", {
      name: "More tools",
      exact: true,
    });
    expect(more).toHaveAttribute("aria-expanded", "false");
    fireEvent.click(more);
    expect(more).toHaveAttribute("aria-expanded", "true");
    expect(
      screen.getByRole("link", { name: "Alerts", exact: true }),
    ).toHaveAttribute("href", "/Alerts");
    for (const list of container.querySelectorAll(
      ".cds--side-nav__items, .cds--side-nav__menu",
    )) {
      expect([...list.children].every((child) => child.tagName === "LI")).toBe(
        true,
      );
    }
  },
);

describe("groups stay open across navigation", () => {
  const qaMenus = () =>
    menus([
      {
        elementId: "menu_qa",
        displayKey: "sideNav.label.qa",
        childMenus: [
          {
            elementId: "menu_qa_qms",
            displayKey: "sideNav.label.qa.qms",
            childMenus: [
              {
                elementId: "menu_reports_audittrail",
                displayKey: "sideNav.title.audittrail",
                childMenus: [
                  {
                    elementId: "menu_reports_audittrail_system",
                    displayKey: "sideNav.label.audittrail.systemEvents",
                    actionURL: "/AuditTrailReport?type=system",
                  },
                  {
                    elementId: "menu_reports_audittrail_order",
                    displayKey: "sideNav.label.audittrail.orderEvents",
                    actionURL: "/AuditTrailReport?type=order",
                  },
                ],
              },
            ],
          },
        ],
      },
    ]);

  let history;
  const renderQa = (entry = "/Dashboard") =>
    render(
      <MemoryRouter initialEntries={[entry]}>
        <IntlProvider locale="en" messages={messages}>
          <Route
            render={(props) => {
              history = props.history;
              return null;
            }}
          />
          <Route
            path="/AuditTrailReport"
            render={({ location }) => (
              <Redirect
                to={{
                  pathname: "/qa/qms/audit-trail",
                  search: location.search,
                }}
              />
            )}
          />
          <SideNav expanded aria-label="Side navigation">
            <SideNavItems>
              <ConfiguredSideNav menus={qaMenus()} unifiedResultsOn={false} />
            </SideNavItems>
          </SideNav>
        </IntlProvider>
      </MemoryRouter>,
    );

  const group = (id) => document.getElementById(id);
  const openGroups = () =>
    ["menu_qa", "menu_qa_qms", "menu_reports_audittrail"].filter(
      (id) => group(id).getAttribute("aria-expanded") === "true",
    );

  it("links a moved audit trail row to its current route", () => {
    renderQa();
    expect(
      document.getElementById("menu_reports_audittrail_system_nav"),
    ).toHaveAttribute("href", "/qa/qms/audit-trail?type=system");
    expect(
      document.getElementById("menu_reports_audittrail_order_nav"),
    ).toHaveAttribute("href", "/qa/qms/audit-trail?type=order");
  });

  it("keeps QA > QMS > Audit Trail open and marks the clicked item", () => {
    renderQa();
    ["menu_qa", "menu_qa_qms", "menu_reports_audittrail"].forEach((id) =>
      fireEvent.click(group(id)),
    );

    fireEvent.click(
      document.getElementById("menu_reports_audittrail_system_nav"),
    );
    expect(openGroups()).toHaveLength(3);
    expect(
      document.getElementById("menu_reports_audittrail_system_nav"),
    ).toHaveAttribute("aria-current", "page");

    fireEvent.click(
      document.getElementById("menu_reports_audittrail_order_nav"),
    );
    expect(openGroups()).toHaveLength(3);
    expect(
      document.getElementById("menu_reports_audittrail_order_nav"),
    ).toHaveAttribute("aria-current", "page");
  });

  it("an old bookmark redirected by the router still opens its groups", () => {
    renderQa("/AuditTrailReport?type=order");
    expect(openGroups()).toHaveLength(3);
    expect(
      document.getElementById("menu_reports_audittrail_order_nav"),
    ).toHaveAttribute("aria-current", "page");
  });

  it("a route no menu item matches leaves the open groups as they were", () => {
    renderQa("/qa/qms/audit-trail?type=system");
    expect(openGroups()).toHaveLength(3);

    act(() => history.push("/qa/qms/page-without-a-menu-entry"));

    expect(openGroups()).toHaveLength(3);
    expect(document.querySelectorAll('[aria-current="page"]')).toHaveLength(0);
  });
});

test("a plugin menu row with a literal name renders it without a missing-translation error", () => {
  const onError = vi.fn();
  render(
    <MemoryRouter initialEntries={["/Dashboard"]}>
      <IntlProvider locale="en-US" messages={messages} onError={onError}>
        <SideNav expanded aria-label="Side navigation">
          <SideNavItems>
            <ConfiguredSideNav
              menus={menus([
                {
                  elementId: "menu_results_analyzer_2",
                  displayKey: "Mindray BS240",
                  actionURL: "/AnalyzerResults?id=2",
                },
                {
                  elementId: "menu_home",
                  displayKey: "banner.menu.home",
                  actionURL: "/Dashboard",
                },
              ])}
              unifiedResultsOn
            />
          </SideNavItems>
        </SideNav>
      </IntlProvider>
    </MemoryRouter>,
  );
  expect(
    screen.getByRole("link", { name: "Mindray BS240", exact: true }),
  ).toHaveAttribute("href", "/AnalyzerResults?id=2");
  expect(
    screen.getByRole("link", { name: messages["banner.menu.home"] }),
  ).toBeInTheDocument();
  expect(onError).not.toHaveBeenCalled();
});
