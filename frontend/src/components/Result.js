import React from "react";
import { injectIntl } from "react-intl";
import "./Style.css";
import ResultSearch from "./resultPage/ResultSearch";
import {
  Microscope,
  IbmWatsonDiscovery,
  IbmWatsonNaturalLanguageUnderstanding,
} from "@carbon/icons-react";
import GlobalSideBar from "./common/GlobalSideBar";

export const resultsSideMenu = {
  className: "resultSideNav",
  sideNavMenuItems: [
    {
      title: "Result Search",
      icon: IbmWatsonDiscovery,
      SideNavMenuItem: [
        {
          link: "#resultSearch",
          label: "By Patient or Lab",
        },
        {
          link: "#resultUnit",
          label: "By Unit",
        },
        {
          link: "#resultSearch",
          label: "By Test, Date or Status",
        },
      ],
    },
    {
      title: "Patient Search",
      icon: Microscope,
      SideNavMenuItem: [
        {
          link: "#1",
          label: "Link",
        },
      ],
    },
    {
      title: "Lab Unit",
      icon: IbmWatsonNaturalLanguageUnderstanding,
      SideNavMenuItem: [
        {
          link: "#1",
          label: "Link",
        },
        {
          link: "#1",
          label: "Link",
        },
      ],
    },
  ],
  contentRoutes: [
    {
      path: "#resultSearch",
      pageComponent: <ResultSearch />,
    },
    {
      path: "#resultUnit",
      pageComponent: <ResultSearch />,
    },
    {
      path: "#resultTest",
      pageComponent: <ResultSearch />,
    },
  ],
};

export const billingSideMenu = {
  className: "resultSideNav",
  sideNavMenuItems: [
    {
      title: "Result Search",
      icon: IbmWatsonDiscovery,
      SideNavMenuItem: [
        {
          link: "#resultSearch",
          label: "By Patient or Lab",
        },
      ],
    },
  ],
  contentRoutes: [
    {
      path: "#resultSearch",
      pageComponent: <ResultSearch />,
    },
  ],
};

function Result() {
  var menu = "Results";
  switch (menu) {
    case "Billing":
      return (
        <>
          <GlobalSideBar sideNav={billingSideMenu} />
        </>
      );
    case "Results":
      return (
        <>
          <GlobalSideBar sideNav={resultsSideMenu} />
        </>
      );
    default:
      return (
        <>
          <GlobalSideBar sideNav={billingSideMenu} />
        </>
      );
  }
}

export default injectIntl(Result);
