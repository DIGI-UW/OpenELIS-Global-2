import React from "react";
import { injectIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import AuditTrailReport from "./AuditTrailReport.js";

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
