import React, { useMemo, useState, useEffect, useRef } from "react";
import {
  Heading,
  Grid,
  Column,
  Section,
  Loading,
  Breadcrumb,
  BreadcrumbItem,
  Button,
} from "@carbon/react";
import { EmptyState, ErrorState } from "./commons";
import "./results-viewer.styles.scss";
import { useParams } from "react-router-dom";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../../config.json";
import { getFromOpenElisServer } from "../../utils/Utils";
import PatientHeader from "../../common/PatientHeader.js";
import { getFullPath } from "../../utils/Navigation";
import PatientSummaryReadonly from "./patient-summary-readonly";
import usePatientResultsData from "./loadPatientTestData/usePatientResultsData";
import ReadonlyResultsTable, {
  flattenPatientResults,
} from "./readonly-results-table";

interface Patient {
  firstName: string;
  lastName: string;
  gender: string;
  birthDateForDisplay: string;
  subjectNumber: string;
  nationalId: string;
  patientPK: number | null;
  primaryPhone?: string;
  streetAddress?: string;
  city?: string;
  commune?: string;
  education?: string;
  maritialStatus?: string;
  nationality?: string;
  healthDistrict?: string;
  healthRegion?: string;
  otherNationality?: string;
  patientContact?: {
    person?: {
      firstName?: string;
      lastName?: string;
      primaryPhone?: string;
      email?: string;
    };
  };
}

interface ResultsViewerProps {
  patientId?: string;
}

const RoutedResultsViewer: React.FC = () => {
  const patientObj: Patient = {
    firstName: "",
    lastName: "",
    gender: "",
    birthDateForDisplay: "",
    subjectNumber: "",
    nationalId: "",
    patientPK: null,
    primaryPhone: "",
    streetAddress: "",
    city: "",
    commune: "",
    education: "",
    maritialStatus: "",
    nationality: "",
    healthDistrict: "",
    healthRegion: "",
    otherNationality: "",
    patientContact: {
      person: {
        firstName: "",
        lastName: "",
        primaryPhone: "",
        email: "",
      },
    },
  };

  const { patientId } = useParams<{ patientId: string }>();
  const [patient, setPatient] = useState(patientObj);
  const componentMounted = useRef(false);
  const intl = useIntl();

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer(
      "/rest/patient-details?patientID=" + patientId,
      (data) => {
        if (componentMounted.current) {
          setPatient(data);
        }
      },
    );
    return () => {
      componentMounted.current = false;
    };
  }, [patientId]);

  return (
    <>
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Breadcrumb>
            <BreadcrumbItem href={getFullPath("/")}>
              {intl.formatMessage({ id: "home.label" })}
            </BreadcrumbItem>
            <BreadcrumbItem href={getFullPath("/PatientHistory")}>
              {intl.formatMessage({ id: "label.search.patient" })}
            </BreadcrumbItem>
          </Breadcrumb>
        </Column>
      </Grid>
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage id="label.test.results" />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <PatientHeader
            id={patient.patientPK}
            lastName={patient.lastName}
            firstName={patient.firstName}
            gender={patient.gender}
            dob={patient.birthDateForDisplay}
            subjectNumber={patient.subjectNumber}
            nationalId={patient.nationalId}
            primaryPhone={patient.primaryPhone}
            className="patient-header2"
          >
            {" "}
          </PatientHeader>
        </Column>
      </Grid>
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <PatientSummaryReadonly patient={patient} />
        </Column>
      </Grid>
      <Grid fullWidth={true} className="orderLegendBody">
        <Column lg={16} md={8} sm={4}>
          <ResultsViewer patientId={patientId} />
        </Column>
      </Grid>
    </>
  );
};

const ResultsViewer: React.FC<ResultsViewerProps> = ({ patientId }) => {
  const intl = useIntl();
  const { sortedObs, loaded, error } = usePatientResultsData(patientId || "");
  const flattenedRows = useMemo(
    () => flattenPatientResults(sortedObs),
    [sortedObs],
  );
  const totalResultsCount = flattenedRows.length;

  const handleReportPrint = () => {
    const reportUrl =
      `${config.serverBaseUrl}/ReportPrint` +
      `?report=patientCILNSP_vreduit` +
      `&type=patient` +
      `&selPatient=${patientId}` +
      `&onlyResults=true` +
      `&dateType=RESULT_DATE`;

    window.open(reportUrl, "_blank");
  };

  if (error) {
    return (
      <ErrorState
        error={error}
        headerTitle={intl.formatMessage({
          id: "dataLoadError",
          defaultMessage: "Data Load Error",
        })}
      />
    );
  }

  return (
    <div className="resultsContainer">
      <div className="resultsHeader">
        <div className="resultsHeaderTitle">
          <h4 style={{ flexGrow: 1 }}>{`${intl.formatMessage({
            id: "sidenav.label.results",
          })} ${totalResultsCount ? `(${totalResultsCount})` : ""}`}</h4>
        </div>
        <Button
          data-cy="printableVersion"
          type="button"
          onClick={handleReportPrint}
        >
          Print Result
        </Button>
      </div>

      {!loaded && <Loading />}

      {loaded && !flattenedRows.length ? (
        <EmptyState
          headerTitle={intl.formatMessage({ id: "label.test.results" })}
          displayText={intl.formatMessage({
            id: "label.test.resultsData",
          })}
        />
      ) : (
        <ReadonlyResultsTable loading={!loaded} rows={flattenedRows} />
      )}
    </div>
  );
};

export default RoutedResultsViewer;
