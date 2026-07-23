import React from "react";
import {
  Content,
  SideNav,
  SideNavItems,
  SideNavMenu,
  SideNavMenuItem,
} from "@carbon/react";
import PathRoute from "../utils/PathRoute";

const GlobalSideBar = (props) => {
  const { sideNav } = props;

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
                          href={subItem.link}
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
