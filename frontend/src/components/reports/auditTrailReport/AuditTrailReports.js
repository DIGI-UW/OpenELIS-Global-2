import React from "react";
import GlobalSideBar from "../../common/GlobalSideBar";
import { FormattedMessage } from "react-intl";
import { IbmWatsonNaturalLanguageUnderstanding } from "@carbon/icons-react";
import PageBreadCrumb from "../../common/PageBreadCrumb";

let breadcrumbs = [{ label: "home.label", link: "/" }];
export const AuditTrailReportsMenu = {
  className: "resultSideNav",
  sideNavMenuItems: [
    {
      title: <FormattedMessage id="sideNav.title.audittrail" />,
      icon: IbmWatsonNaturalLanguageUnderstanding,
      SideNavMenuItem: [
        {
          link: "/AuditTrailReport?type=system",
          label: (
            <FormattedMessage id="sideNav.label.audittrail.systemEvents" />
          ),
        },
        {
          link: "/AuditTrailReport?type=order",
          label: (
            <FormattedMessage id="sideNav.label.audittrail.orderEvents" />
          ),
        },
      ],
    },
  ],
  contentRoutes: [],
};
const AuditTrailReports = () => {
  return (
    <>
      <div style={{ marginLeft: "1%" }}>
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
      </div>
      <GlobalSideBar sideNav={AuditTrailReportsMenu} />
    </>
  );
};

export default AuditTrailReports;
