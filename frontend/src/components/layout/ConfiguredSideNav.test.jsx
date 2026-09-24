import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
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
