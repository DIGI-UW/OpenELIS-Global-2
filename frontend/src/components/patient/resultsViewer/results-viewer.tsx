import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Heading,
  Grid,
  Column,
  Section,
  Loading,
  Breadcrumb,
  BreadcrumbItem,
  Tag,
} from "@carbon/react";
import { useTranslation } from "react-i18next";
import { EmptyState, ErrorState } from "./commons";
import { FilterContext, FilterProvider } from "./filter";
import { useGetManyObstreeData } from "./grouped-timeline";
import "./results-viewer.styles.scss";
import { useParams } from "react-router-dom";
import TreeViewWrapper from "./tree-view";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import config from "../../../config.json";
import { getFromOpenElisServer } from "../../utils/Utils";
import PatientHeader from "../../common/PatientHeader.js";

interface ResultsViewerProps {
  basePath: string;
  patientId?: string;
  loading?: boolean;
}

interface Patient {
  firstName: string;
  lastName: string;
  gender: string;
  birthDateForDisplay: string;
  subjectNumber: string;
  nationalId: string;
  patientPK: number;
}
const RoutedResultsViewer: React.FC<ResultsViewerProps> = () => {
  const patientObj: Patient = {
    firstName: "",
    lastName: "",
    gender: "",
    birthDateForDisplay: "",
    subjectNumber: "",
    nationalId: "",
    patientPK: null,
  };

  const { patientId } = useParams();
  const [patient, setPatient] = useState(patientObj);

  const componentMounted = useRef(false);

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer(
      "/rest/patient-details?patientID=" + patientId,
      loadPatient,
    );
    return () => {
      componentMounted.current = false;
    };
  }, [patientId]);

  const loadPatient = (patient) => {
    if (componentMounted.current) {
      setPatient(patient);
    }
  };
  const intl = useIntl();

  const { roots, loading, error } = useGetManyObstreeData(patientId);

  const { t } = useTranslation();

  if (error) {
    return (
      <ErrorState
        error={error}
        headerTitle={t("dataLoadError", "Data Load Error")}
      />
    );
  }

  if (loading) {
    return (
      <>
        <Loading></Loading>
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <EmptyState
              headerTitle={intl.formatMessage({ id: "label.test.results" })}
              displayText={intl.formatMessage({
                id: "label.test.resultsData",
              })}
            />
          </Column>
        </Grid>
      </>
    );
  }

  return (
    <>
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Breadcrumb>
            <BreadcrumbItem href="/">
              {intl.formatMessage({ id: "home.label" })}
            </BreadcrumbItem>
            <BreadcrumbItem href="/PatientHistory">
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
                <FormattedMessage id="label.page.patientHistory" />
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
            className="patient-header2"
          >
            {" "}
          </PatientHeader>
        </Column>
      </Grid>

      {roots?.length ? (
        <Grid fullWidth={true} className="orderLegendBody">
          <Column lg={16} md={8} sm={4}>
            <FilterProvider roots={loading ? roots : []}>
              <ResultsViewer
                patientId={patientId}
                basePath={config.serverBaseUrl}
                loading={loading}
              />
            </FilterProvider>
          </Column>
        </Grid>
      ) : (
        <Grid fullWidth={true} className="orderLegendBody">
          <Column lg={16}>
            <EmptyState
              headerTitle={intl.formatMessage({ id: "label.test.results" })}
              displayText={intl.formatMessage({
                id: "label.test.resultsData",
              })}
            />
          </Column>
        </Grid>
      )}
    </>
  );
};

const ResultsViewer: React.FC<ResultsViewerProps> = ({
  patientId,
  basePath,
}) => {
  const { t } = useTranslation();
  const { totalResultsCount } = useContext(FilterContext);
  const { type, testUuid } = useParams();
  const intl = useIntl();
  return (
    <div className="resultsContainer">
      <div className="resultsHeader">
        <div className="leftSection leftHeaderSection desktopHeading">
          <h4 style={{ flexGrow: 1 }}>{`${intl.formatMessage({
            id: "sidenav.label.results",
          })} ${totalResultsCount ? `(${totalResultsCount})` : ""}`}</h4>
        </div>
      </div>

      <div className="flex">
        <TreeViewWrapper
          patientUuid={patientId}
          basePath={basePath}
          type={type}
          expanded={true}
          testUuid={testUuid}
        />
      </div>
    </div>
  );
};

export default RoutedResultsViewer;
