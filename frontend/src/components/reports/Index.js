import React, { useContext, useState, useEffect } from "react";
import { AlertDialog } from "../common/CustomNotification";
import { NotificationContext } from "../layout/Layout";
import { injectIntl, FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { StudyReports } from "./study/index";
import { RoutineReports } from "./routine/Index";
import { Loading } from "@carbon/react";

const ReportIndex = () => {
  const intl = useIntl();
  const { setNotificationVisible, addNotification, notificationVisible } =
    useContext(NotificationContext);

  const [type, setType] = useState("");
  const [report, setReport] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const paramType = params.get("type");
    const paramReport = params.get("report");
    setType(paramType);
    setReport(paramReport);

    if (paramType && paramReport) {
      setIsLoading(false);
    } else {
      window.location.href = "/";
    }
  }, []);

  return (
    <>
      <br />
      <PageBreadCrumb breadcrumbs={[{ label: "home.label", link: "/" }]} />
      <div className="orderLegendBody">
        {notificationVisible === true && <AlertDialog />}
        {isLoading && <Loading />}
        {!isLoading && (
          <>
            <RoutineReports type={type} report={report} />
            <StudyReports type={type} report={report} />
          </>
        )}
      </div>
    </>
  );
};

export default injectIntl(ReportIndex);
