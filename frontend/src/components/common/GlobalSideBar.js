import React from "react";
import {
  Content,
  SideNav,
  SideNavItems,
  SideNavMenu,
  SideNavMenuItem,
} from "@carbon/react";
import PathRoute from "../utils/PathRoute";
import { getFullPath } from "../utils/Navigation";

const GlobalSideBar = (props) => {
  const { sideNav } = props;

  // Helper to resolve link: use getFullPath for internal links, pass through external links
  const resolveLink = (link) => {
    if (!link) return link;
    // External links (http/https) should not be prefixed
    if (link.startsWith("http://") || link.startsWith("https://")) {
      return link;
    }
    // Internal links get the base path prefix
    return getFullPath(link);
  };

  return (
    <>
      <SideNav
        aria-label="Side navigation"
        isPersistent={true}
        defaultExpanded={true}
        isRail
      >
        <SideNavItems className={sideNav.className}>
          {sideNav.sideNavMenuItems.map((item, index) => {
            return (
              <div key={"sideNav_" + index}>
                <SideNavMenu renderIcon={item.icon} title={item.title}>
                  {item.SideNavMenuItem.map((subItem, subIndex) => {
                    return (
                      <div key={index + "_" + subIndex}>
                        <SideNavMenuItem
                          key={index + "_" + subIndex}
                          href={resolveLink(subItem.link)}
                        >
                          {subItem.label}
                        </SideNavMenuItem>
                      </div>
                    );
                  })}
                </SideNavMenu>
              </div>
            );
          })}
        </SideNavItems>
      </SideNav>
      <Content>
        {sideNav.contentRoutes.map((route, index) => {
          return (
            <PathRoute key={"routePath_" + index} path={route.path}>
              {route.pageComponent}
            </PathRoute>
          );
        })}
      </Content>
    </>
  );
};

export default GlobalSideBar;
