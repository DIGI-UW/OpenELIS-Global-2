import React, { useContext, useState, useEffect } from "react";
import { AlertDialog } from "../../common/CustomNotification";
import { NotificationContext } from "../../layout/Layout";
import { injectIntl, FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import AuditTrailReport from "./AuditTrailReport.js";
import { Loading } from "@carbon/react";

const AuditTrailReportIndex = () => {
  return (
    <>
      <br />
      <PageBreadCrumb breadcrumbs={[{ label: "home.label", link: "/" }]} />
      <div className="orderLegendBody">
        <>
          <AuditTrailReport report={"auditTrail"} id={"reports.auditTrail"} />
        </>
      </div>
    </>
  );
};

export default injectIntl(AuditTrailReportIndex);
