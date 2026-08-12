import React, { useContext, useState, useEffect } from "react";
import { AlertDialog } from "../../common/CustomNotification";
import { NotificationContext } from "../../layout/Layout";
import { Loading } from "@carbon/react";
import { injectIntl, useIntl } from "react-intl";
import PatientStatusReport from "../common/PatientStatusReport";
import StatisticsReport from "./StatisticsReport";
import ReferredOut from "./ReferredOut";
import ReportByDate from "../common/ReportByDate";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { RoutineReportsMenu } from "../Routine";

// The routine side nav already pairs every report URL with its title message,
// so derive the leaf breadcrumb from it rather than duplicating the mapping.
export const ROUTINE_REPORT_LABELS = RoutineReportsMenu.sideNavMenuItems.reduce(
  (labels, group) => {
    (group.SideNavMenuItem || []).forEach((item) => {
      const query = item.link.split("?")[1];
      const messageId = item.label?.props?.id;
      if (query && messageId) {
        const params = new URLSearchParams(query);
        labels[`${params.get("type")}_${params.get("report")}`] = messageId;
      }
    });
    return labels;
  },
  {},
);

export const RoutineReports = (props) => {
  const { type, report } = props;

  return (
    <>
      {type === "patient" && report === "patientCILNSP_vreduit" && (
        <PatientStatusReport
          report={"patientCILNSP_vreduit"}
          id={"openreports.patientTestStatus"}
        />
      )}

      {type === "patient" && report === "referredOut" && <ReferredOut />}

      {type === "patient" && report === "haitiNonConformityBySectionReason" && (
        <ReportByDate
          report={"haitiNonConformityBySectionReason"}
          id={"openreports.mgt.nonconformity.section"}
        />
      )}

      {type === "patient" && report === "haitiNonConformityByDate" && (
        <ReportByDate
          report={"haitiNonConformityByDate"}
          id={"openreports.mgt.nonconformity.date"}
        />
      )}

      {type === "routine" && report === "CISampleRoutineExport" && (
        <ReportByDate
          report={"CISampleRoutineExport"}
          id={"sideNav.label.exportcsvfile"}
        />
      )}

      {type === "indicator" &&
        (report === "activityReportByTest" ||
          report === "activityReportByPanel" ||
          report === "activityReportByTestSection") && (
          <ReportByDate key={report} report={report} />
        )}

      {type === "indicator" && report === "statisticsReport" && (
        <StatisticsReport />
      )}

      {type === "indicator" && report === "indicatorHaitiLNSPAllTests" && (
        <ReportByDate
          report={"indicatorHaitiLNSPAllTests"}
          id={"openreports.all.test.summary.title"}
        />
      )}

      {type === "indicator" && report === "indicatorCDILNSPHIV" && (
        <ReportByDate
          report={"indicatorHaitiLNSPAllTests"}
          id={"openreports.all.test.summary.title"}
        />
      )}

      {type === "indicator" && report === "sampleRejectionReport" && (
        <ReportByDate
          report={"sampleRejectionReport"}
          id={"openreports.mgt.rejection"}
        />
      )}

      {type === "patient" && report === "ExportWHONETReportByDate" && (
        <ReportByDate
          report={"ExportWHONETReportByDate"}
          id={"header.label.study.ciexport"}
        />
      )}

    </>
  );
};

const RoutineIndex = () => {
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
      window.location.href = "/RoutineReports";
    }
  }, []);

  return (
    <>
      <br />
      <PageBreadCrumb
        breadcrumbs={[
          { label: "home.label", link: "/" },
          { label: "routine.reports", link: "/RoutineReports" },
          ...(ROUTINE_REPORT_LABELS[`${type}_${report}`]
            ? [
                {
                  label: ROUTINE_REPORT_LABELS[`${type}_${report}`],
                  link: "",
                },
              ]
            : []),
        ]}
      />
      <div className="orderLegendBody">
        {notificationVisible === true && <AlertDialog />}
        {isLoading && <Loading />}
        {!isLoading && <RoutineReports type={type} report={report} />}
      </div>
    </>
  );
};

export default injectIntl(RoutineIndex);
