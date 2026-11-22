import React, { useContext, useState, useEffect } from "react";
import { AlertDialog } from "../common/CustomNotification";
import { NotificationContext } from "../layout/Layout";
import { injectIntl, useIntl } from "react-intl";
import { Link, useLocation } from "react-router-dom";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { StudyReports, STUDY_REPORT_LABELS } from "./study/index";
import { RoutineReports, ROUTINE_REPORT_LABELS } from "./routine/Index";
import { InlineNotification, Loading } from "@carbon/react";

const ReportIndex = () => {
  const intl = useIntl();
  const location = useLocation();
  const { notificationVisible } = useContext(NotificationContext);

  const [type, setType] = useState("");
  const [report, setReport] = useState("");
  const [missing, setMissing] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  // A report link names both the type and the report. When either is absent the
  // page used to send the user to the Dashboard with no explanation, so a stale
  // bookmark looked like a session or permission problem (OGC-1053). It now
  // says what is missing and points at the report lists instead.
  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const paramType = params.get("type");
    const paramReport = params.get("report");
    setType(paramType);
    setReport(paramReport);
    setMissing(
      [!paramType && "type", !paramReport && "report"].filter(Boolean),
    );
    setIsLoading(false);
  }, [location.search]);

  // /Report renders the routine or study report body without either index's own
  // breadcrumb, so it owns the full path: Home / <section> / <report>.
  const studyLabel = STUDY_REPORT_LABELS[`${type}_${report}`];
  const reportLabel = studyLabel || ROUTINE_REPORT_LABELS[`${type}_${report}`];
  const breadcrumbs = [
    { label: "home.label", link: "/" },
    studyLabel
      ? { label: "label.study.Reports", link: "" }
      : { label: "routine.reports", link: "" },
    ...(reportLabel ? [{ label: reportLabel, link: "" }] : []),
  ];

  return (
    <>
      <br />
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <div className="orderLegendBody">
        {notificationVisible === true && <AlertDialog />}
        {isLoading && <Loading />}
        {!isLoading && missing.length > 0 && (
          <div data-testid="report-link-incomplete">
            <InlineNotification
              kind="warning"
              lowContrast
              hideCloseButton
              title={intl.formatMessage({
                id: "error.report.linkIncomplete.title",
              })}
              subtitle={intl.formatMessage(
                { id: "error.report.linkIncomplete" },
                { 0: missing.join(", ") },
              )}
            />
            <ul>
              <li>
                <Link className="cds--link" to="/RoutineReports">
                  {intl.formatMessage({ id: "routine.reports" })}
                </Link>
              </li>
              <li>
                <Link className="cds--link" to="/StudyReports">
                  {intl.formatMessage({ id: "label.study.Reports" })}
                </Link>
              </li>
            </ul>
          </div>
        )}
        {!isLoading && missing.length === 0 && (
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
