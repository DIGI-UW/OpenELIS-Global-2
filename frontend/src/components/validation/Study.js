import React from "react";
import GlobalSideBar from "../common/GlobalSideBar";
import {
  IbmWatsonDiscovery,
  IbmWatsonNaturalLanguageUnderstanding,
  Microscope,
} from "@carbon/icons-react";
import config from "../../config.json";
export const RoutineReportsMenu = {
  className: "resultSideNav",
  sideNavMenuItems: [
    {
      title: "Immunology - Hematology",
      icon: IbmWatsonDiscovery,
      SideNavMenuItem: [
        {
          link: "/ResultValidationRetroC?type=Immunology&test=",
          label: "Immunology - Hematology",
        },
      ],
    },
    {
      title: "Biochemistry",
      icon: Microscope,
      SideNavMenuItem: [
        {
          link: "/ResultValidationRetroC?type=Biochemistry&test=",
          label: "Biochemistry",
        },
      ],
    },
    {
      title: "Serology",
      icon: IbmWatsonNaturalLanguageUnderstanding,
      SideNavMenuItem: [
        {
          link: "/ResultValidationRetroC?type=serology",
          label: "Serology",
        },
      ],
    },
    {
      title: "Virology",
      icon: IbmWatsonNaturalLanguageUnderstanding,
      SideNavMenuItem: [
        {
          link: "/ResultValidationRetroC?type=virology&test=DNA PCR",
          label: "DNA PCR",
        },
        {
          link: "/ResultValidationRetroC?type=virology&test=Viral Load",
          label: "Viral Load",
        },
        {
          link: "/ResultValidationRetroC?type=virology&test=Genotyping",
          label: "Genotyping",
        },
      ],
    },
  ],
  contentRoutes: [],
};
const Study = () => {
  return (
    <>
      <GlobalSideBar sideNav={RoutineReportsMenu} />
    </>
  );
};

export default Study;
