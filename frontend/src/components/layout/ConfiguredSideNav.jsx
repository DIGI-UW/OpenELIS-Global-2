import React, { useMemo } from "react";
import { Link, useLocation } from "react-router-dom";
import { useIntl } from "react-intl";
import {
  SideNavDivider,
  SideNavLink,
  SideNavMenu,
  SideNavMenuItem,
} from "@carbon/react";
import { useMenuAutoExpand } from "./useMenuAutoExpand";
import { canonicalReportingUrl } from "../reports/CustomDataExport/routes";
import { reportingMenuDestination } from "../reports/CustomDataExport/useReportingRoute";

import { navigationIcons as icons } from "./navigationIcons";

const legacyResults = new Set([
  "menu_results_logbook",
  "menu_results_patient",
  "menu_results_accession",
  "menu_results_range",
  "menu_results_status",
]);

export default function ConfiguredSideNav({ menus, unifiedResultsOn }) {
  const intl = useIntl();
  const location = useLocation();
  const visibleMenus = useMemo(() => {
    const filter = (items) =>
      items
        .filter(
          ({ menu }) =>
            menu.isActive &&
            (menu.elementId !== "menu_results_unified" || unifiedResultsOn) &&
            (!legacyResults.has(menu.elementId) || !unifiedResultsOn),
        )
        .map((item) => ({
          ...item,
          menu: {
            ...item.menu,
            actionURL: canonicalReportingUrl(item.menu.actionURL),
          },
          childMenus: filter(item.childMenus || []),
        }));
    return filter(menus || []);
  }, [menus, unifiedResultsOn]);
  const expandedMenus = useMenuAutoExpand(visibleMenus);
  const label = (key) => intl.formatMessage({ id: key, defaultMessage: key });

  const renderItem = (item, level = 0) => {
    const { menu, childMenus, routeActive, activeDescendantId, expanded } =
      item;
    const id = menu.elementId || menu.id;
    if (menu.presentationStyle === "section") {
      return (
        <React.Fragment key={id}>
          <SideNavDivider />
          <li className="configured-nav-section">
            <h2>{label(menu.displayKey)}</h2>
          </li>
          {childMenus.map((child) => renderItem(child, level))}
        </React.Fragment>
      );
    }
    if (childMenus.length) {
      return (
        <SideNavMenu
          // Carbon owns expansion internally. Reinitialize only when navigation
          // changes the active descendant, preserving manual folds in between.
          key={`${id}:${activeDescendantId || ""}`}
          ref={(button) => {
            if (button) {
              button.id = id;
              button.dataset.cy = id.replace(/[^\w\s]/gi, "_");
            }
          }}
          title={label(menu.displayKey)}
          renderIcon={icons[menu.icon]}
          defaultExpanded={expanded}
          isActive={routeActive}
        >
          {childMenus.map((child) => renderItem(child, level + 1))}
        </SideNavMenu>
      );
    }
    const internal =
      menu.actionURL?.startsWith("/") && !menu.actionURL.startsWith("//");
    const unavailable = !menu.actionURL;
    const Item = level === 0 ? SideNavLink : SideNavMenuItem;
    const destination = unavailable
      ? { as: "span", "aria-disabled": true, tabIndex: -1 }
      : internal
        ? { as: Link, to: reportingMenuDestination(menu.actionURL, location) }
        : {
            href: menu.actionURL,
            target: menu.openInNewWindow ? "_blank" : undefined,
            rel: menu.openInNewWindow ? "noopener noreferrer" : undefined,
          };
    return (
      <Item
        key={id}
        {...destination}
        {...(level === 0 ? { renderIcon: icons[menu.icon] } : {})}
        id={`${id}_nav`}
        data-cy={id.replace(/[^\w\s]/gi, "_")}
        className={unavailable ? "configured-nav-unavailable" : undefined}
        isActive={routeActive}
        aria-current={routeActive ? "page" : undefined}
      >
        <span id={id}>
          {label(menu.displayKey)}
          {unavailable && menu.toolTipKey && (
            <small>{label(menu.toolTipKey)}</small>
          )}
        </span>
      </Item>
    );
  };
  return expandedMenus.map((item) => renderItem(item));
}
