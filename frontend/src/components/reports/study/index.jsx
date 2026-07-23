import React, { useContext, useState, useEffect } from "react";
import { AlertDialog } from "../../common/CustomNotification";
import { NotificationContext } from "../../layout/Layout";
import {
  Heading,
  Grid,
  Column,
  Section,
  Breadcrumb,
  BreadcrumbItem,
  Loading,
} from "@carbon/react";
import { injectIntl, FormattedMessage, useIntl } from "react-intl";
import PatientStatusReport from "../common/PatientStatusReport";
import ReportByID from "../common/ReportByID";
import ReportByDate from "../common/ReportByDate";
import ReportByLabNo from "../common/ReportByLabNo";
import NonConformityNotification from "./NonConformityNotification";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import AuditTrailReport from "../auditTrailReport/AuditTrailReport";
import ReportByDateCSV from "../common/ReportByDateCSV";
import IntermediateByService from "./IntermediateByService";

export const StudyReports = (props) => {
  const { type, report } = props;

  return (
    <>
      {type === "patient" && report === "patientARVInitial1" && (
        <ReportByLabNo report="patientARVInitial1" id="header.label.ARV" />
      )}

      {type === "patient" && report === "patientARVInitial2" && (
        <ReportByLabNo report="patientARVInitial2" id="header.label.ARV" />
      )}

      {type === "patient" && report === "CIStudyExport" && (
        <ReportByDateCSV
          report="CIStudyExport"
          id="header.label.study.ciexport"
        />
      )}

      {type === "patient" && report === "Trends" && (
        <ReportByDateCSV report="Trends" id="header.label.study.vlloadtrends" />
      )}

      {type === "patient" && report === "patientARVFollowup1" && (
        <ReportByLabNo
          report="patientARVFollowup1"
          id="header.label.followup"
        />
      )}

      {type === "patient" && report === "patientARVFollowup2" && (
        <ReportByLabNo
          report="patientARVFollowup2"
          id="header.label.followup"
        />
      )}

      {type === "patient" && report === "patientARV1" && (
        <ReportByLabNo report="patientARV1" id="header.label.intialFollowup" />
      )}

      {type === "patient" && report === "patientEID1" && (
        <PatientStatusReport report="patientEID1" id="header.label.EID" />
      )}

      {type === "patient" && report === "patientEID2" && (
        <ReportByLabNo report="patientEID2" id="header.label.EID" />
      )}

      {type === "patient" && report === "patientVL1" && (
        <PatientStatusReport
          report="patientVL1"
          id="banner.menu.resultvalidation.viralload"
        />
      )}

      {type === "patient" && report === "patientIndeterminate1" && (
        <ReportByLabNo
          report="patientIndeterminate1"
          id="project.IndeterminateStudy.name"
        />
      )}

      {type === "patient" && report === "patientIndeterminate2" && (
        <ReportByLabNo
          report="patientIndeterminate2"
          id="project.IndeterminateStudy.name"
        />
      )}

      {type === "patient" && report === "patientIndeterminateByLocation" && (
        <IntermediateByService
          report="patientIndeterminateByLocation"
          id="project.IndeterminateStudy.name"
        />
      )}

      {type === "patient" && report === "patientSpecialReport" && (
        <ReportByLabNo
          report="patientSpecialReport"
          id="header.label.specialRequest"
        />
      )}

      {type === "patient" && report === "patientCollection" && (
        <ReportByID
          report="patientCollection"
          id="patient.report.collection.name"
        />
      )}
      {type === "patient" && report === "patientAssociated" && (
        <ReportByID
          report="patientAssociated"
          id="patient.report.associated.name"
        />
      )}
      {type === "patient" && report === "retroCINonConformityByDate" && (
        <ReportByDate
          report="retroCINonConformityByDate"
          id="header.label.nonconformityByDate"
        />
      )}

      {type === "patient" &&
        report === "retroCInonConformityBySectionReason" && (
          <ReportByDate
            report="retroCInonConformityBySectionReason"
            id="reports.nonConformity.bySectionReason.title"
          />
        )}

      {type === "patient" && report === "retroCINonConformityByLabno" && (
        <ReportByLabNo
          report="retroCINonConformityByLabno"
          id="header.label.intialFollowup"
        />
      )}
      {type === "patient" && report === "retroCInonConformityNotification" && (
        <NonConformityNotification
          report="retroCInonConformityNotification"
          id="reports.nonConformity.notification.report"
        />
      )}
      {type === "patient" && report === "retroCIFollowupRequiredByLocation" && (
        <ReportByDate
          report="retroCIFollowupRequiredByLocation"
          id="reports.followupRequired.byLocation"
        />
      )}

      {type === "study" && report === "auditTrail" && (
        <AuditTrailReport report={"auditTrail"} id={"reports.auditTrail"} />
      )}
    </>
  );
};

const StudyIndex = () => {
  const intl = useIntl();
  const { setNotificationVisible, addNotification, notificationVisible } =
    useContext(NotificationContext);

  const [type, setType] = useState("");
  const [report, setReport] = useState("");
  const [source, setSource] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [breadcrumbs, setBreadcrumbs] = useState([]);
  const breadcrumbMap = {
    patient_patientCollection: "patient.report.collection.name",
    patient_patientAssociated: "patient.report.associated.name",
    patient_retroCINonConformityByDate: "header.label.nonconformityByDate",
    patient_retroCInonConformityBySectionReason:
      "reports.nonConformity.bySectionReason.title",
    patient_retroCINonConformityByLabno: "header.label.intialFollowup",
    patient_retroCInonConformityNotification:
      "reports.nonConformity.notification.report",
    patient_retroCIFollowupRequiredByLocation:
      "reports.followupRequired.byLocation",
    patient_patientARVInitial1: "header.label.ARV",
    patient_patientARVInitial2: "header.label.ARV",
    patient_patientARVFollowup1: "header.label.followup",
    patient_patientARVFollowup2: "header.label.followup",
    patient_patientARV1: "header.label.intialFollowup",
    patient_patientEID1: "header.label.EID",
    patient_patientEID2: "header.label.EID",
    patient_patientVL1: "banner.menu.resultvalidation.viralload",
    patient_patientIndeterminate1: "project.IndeterminateStudy.name",
    patient_patientIndeterminate2: "project.IndeterminateStudy.name",
    patient_patientIndeterminateByLocation: "project.IndeterminateStudy.name",
    patient_patientSpecialReport: "header.label.specialRequest",
    study_auditTrail: "reports.auditTrail",
    patient_CIStudyExport: "reports.label.cistudyexport",
    patient_Trends: "reports.label.trends",
  };

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const paramType = params.get("type");
    const paramReport = params.get("report");

    setType(paramType);
    setReport(paramReport);

    // Updating breadcrumbs based on paramType and paramReport
    if (paramType && paramReport) {
      const breadcrumbId = breadcrumbMap[`${paramType}_${paramReport}`];
      if (breadcrumbId) {
        const breadcrumbLabel = intl.formatMessage({ id: breadcrumbId });
        setBreadcrumbs([
          { label: intl.formatMessage({ id: "home.label" }), link: "/" },
          {
            label: intl.formatMessage({ id: "label.study.Reports" }),
            link: "/StudyReports",
          },
          {
            label: breadcrumbLabel,
            link: `/StudyReport?type=${paramType}&report=${paramReport}`,
          },
        ]);
      }
      setIsLoading(false);
    } else {
      window.location.href = "/StudyReports";
    }
  }, [type, report]);

  return (
    <>
      <br />
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage id="selectReportValues.title" />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      <div className="orderLegendBody">
        {notificationVisible === true && <AlertDialog />}
        {isLoading && <Loading />}
        {!isLoading && <StudyReports type={type} report={report} />}
      </div>
    </>
  );
};

export default injectIntl(StudyIndex);
