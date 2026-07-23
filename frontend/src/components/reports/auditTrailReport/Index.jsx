import React, { useState, useEffect, useContext } from "react";
import { AlertDialog } from "../../common/CustomNotification";
import { NotificationContext } from "../../layout/Layout";
import { useLocation } from "react-router-dom";
import { Loading } from "@carbon/react";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import AuditTrailReport from "./AuditTrailReport";
import SystemAuditEvents from "./SystemAuditEvents";

const AuditTrailReportIndex = () => {
  const { notificationVisible } = useContext(NotificationContext);
  const location = useLocation();

  const [type, setType] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const paramType = params.get("type");
    if (paramType) {
      setType(paramType);
      setIsLoading(false);
    } else {
      window.location.href = "/AuditTrailReport?type=system";
    }
  }, [location.search]);

  return (
    <>
      <br />
      <PageBreadCrumb
        breadcrumbs={[
          { label: "home.label", link: "/" },
          {
            label: "sideNav.title.audittrail",
            link: "/AuditTrailReport?type=system",
          },
        ]}
      />
      <div className="orderLegendBody">
        {notificationVisible === true && <AlertDialog />}
        {isLoading && <Loading />}
        {!isLoading && type === "system" && <SystemAuditEvents />}
        {!isLoading && type === "order" && (
          <AuditTrailReport report={"auditTrail"} id={"reports.auditTrail"} />
        )}
      </div>
    </>
  );
};

export default AuditTrailReportIndex;
