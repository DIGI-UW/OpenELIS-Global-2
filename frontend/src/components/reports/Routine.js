import React from "react";
import GlobalSideBar from "../common/GlobalSideBar";
import { FormattedMessage, injectIntl } from "react-intl";
import {
  IbmWatsonDiscovery,
  IbmWatsonNaturalLanguageUnderstanding,
  Microscope,
} from "@carbon/icons-react";
import config from "../../config.json";
import PageBreadCrumb from "../common/PageBreadCrumb";

let breadcrumbs = [{ label: "home.label", link: "/" }];
export const RoutineReportsMenu = {
  className: "resultSideNav",
  sideNavMenuItems: [
    {
      title: <FormattedMessage id="sidenav.title.statusreport" />,
      icon: IbmWatsonDiscovery,
      SideNavMenuItem: [
        {
          link: "/RoutineReport?type=patient&report=patientCILNSP_vreduit",
          label: <FormattedMessage id="sidenav.label.statusreport" />,
        },
      ],
    },
    {
      title: <FormattedMessage id="sidenav.title.aggregatereport" />,
      icon: Microscope,
      SideNavMenuItem: [
        {
          link: "/RoutineReport?type=indicator&report=statisticsReport",
          label: <FormattedMessage id="sidenav.label.statisticsreport" />,
        },
        {
          link: "/RoutineReport?type=indicator&report=indicatorHaitiLNSPAllTests",
          label: <FormattedMessage id="sidenav.label.testsummary" />,
        },
        {
          link: "/RoutineReport?type=indicator&report=indicatorCDILNSPHIV",
          label: <FormattedMessage id="sideNav.label.hivtestsummary" />,
        },
      ],
    },
    {
      title: <FormattedMessage id="sideNav.title.rejectionreport" />,
      icon: IbmWatsonNaturalLanguageUnderstanding,
      SideNavMenuItem: [
        {
          link: "/RoutineReport?type=indicator&report=sampleRejectionReport",
          label: <FormattedMessage id="sideNav.label.rejectionreport" />,
        },
      ],
    },
    {
      title: <FormattedMessage id="sideNav.title.activityreport" />,
      icon: IbmWatsonNaturalLanguageUnderstanding,
      SideNavMenuItem: [
        {
          link: "/RoutineReport?type=indicator&report=activityReportByTest",
          label: <FormattedMessage id="sideNav.label.bytesttype" />,
        },
        {
          link: "/RoutineReport?type=indicator&report=activityReportByPanel",
          label: <FormattedMessage id="sideNav.label.bypaneltype" />,
        },
        {
          link: "/RoutineReport?type=indicator&report=activityReportByTestSection",
          label: <FormattedMessage id="sideNav.label.byunit" />,
        },
      ],
    },
    {
      title: (
        <FormattedMessage
          id="sideNav.title.referredtestreport"
          defaultMessage="Referred Out Tests Report"
        />
      ),
      icon: IbmWatsonNaturalLanguageUnderstanding,
      SideNavMenuItem: [
        {
          link: "/RoutineReport?type=patient&report=referredOut",
          label: <FormattedMessage id="sideNav.label.referredtestreport" />,
        },
      ],
    },
    {
      title: <FormattedMessage id="sideNav.title.noncomformityreports" />,
      icon: IbmWatsonNaturalLanguageUnderstanding,
      SideNavMenuItem: [
        {
          link: "/RoutineReport?type=patient&report=haitiNonConformityByDate",
          label: (
            <FormattedMessage id="sideNav.label.noncomformityreportsbydate" />
          ),
        },
        {
          link: "/RoutineReport?type=patient&report=haitiNonConformityBySectionReason",
          label: (
            <FormattedMessage id="sideNav.label.noncomformityreportsbyunit" />
          ),
        },
      ],
    },
    {
      title: <FormattedMessage id="sideNav.title.delayedvalidation" />,
      icon: IbmWatsonNaturalLanguageUnderstanding,
      SideNavMenuItem: [
        {
          link:
            config.serverBaseUrl +
            "/ReportPrint?type=indicator&report=validationBacklog",
          label: <FormattedMessage id="sideNav.label.delayedvalidation" />,
          icon: IbmWatsonNaturalLanguageUnderstanding,
        },
      ],
    },
    {
      title: <FormattedMessage id="sideNav.title.audittrail" />,
      icon: IbmWatsonNaturalLanguageUnderstanding,
      SideNavMenuItem: [
        {
          link: "/RoutineReport?type=routine&report=auditTrail",
          label: <FormattedMessage id="sideNav.label.audittrail" />,
        },
      ],
    },
    {
      title: <FormattedMessage id="sideNav.title.exportcsvfile" />,
      icon: IbmWatsonNaturalLanguageUnderstanding,
      SideNavMenuItem: [
        {
          link: "/RoutineReport?type=routine&report=CISampleRoutineExport",
          label: <FormattedMessage id="sideNav.label.exportcsvfile" />,
        },
      ],
    },
  ],
  contentRoutes: [],
};
const Routine = () => {
  return (
    <>
      <div style={{ marginLeft: "1%" }}>
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
      </div>
      <GlobalSideBar sideNav={RoutineReportsMenu} />
    </>
  );
};

export default injectIntl(Routine);
