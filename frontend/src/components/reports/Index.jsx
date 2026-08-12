import React, { useContext, useState, useEffect } from "react";
import { AlertDialog } from "../common/CustomNotification";
import { NotificationContext } from "../layout/Layout";
import { injectIntl, useIntl } from "react-intl";
import { useLocation } from "react-router-dom";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { StudyReports, STUDY_REPORT_LABELS } from "./study/index";
import { RoutineReports, ROUTINE_REPORT_LABELS } from "./routine/Index";
import { Loading } from "@carbon/react";

const ReportIndex = () => {
  const intl = useIntl();
  const location = useLocation();
  const { setNotificationVisible, addNotification, notificationVisible } =
    useContext(NotificationContext);

  const [type, setType] = useState("");
  const [report, setReport] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const paramType = params.get("type");
    const paramReport = params.get("report");
    setType(paramType);
    setReport(paramReport);

    if (paramType && paramReport) {
      setIsLoading(false);
    } else {
      window.location.href = "/";
    }
  }, [location.search]);

  // /Report renders the routine or study report body without either index's own
  // breadcrumb, so it owns the full path: Home / <section> / <report>.
  const studyLabel = STUDY_REPORT_LABELS[`${type}_${report}`];
  const reportLabel = studyLabel || ROUTINE_REPORT_LABELS[`${type}_${report}`];
  const breadcrumbs = [
    { label: "home.label", link: "/" },
    studyLabel
      ? { label: "label.study.Reports", link: "/StudyReports" }
      : { label: "routine.reports", link: "/RoutineReports" },
    ...(reportLabel ? [{ label: reportLabel, link: "" }] : []),
  ];

  return (
    <>
      <br />
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
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
