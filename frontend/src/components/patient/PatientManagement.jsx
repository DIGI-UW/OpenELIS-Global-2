import React from "react";
import { FormattedMessage, injectIntl } from "react-intl";
import { useHistory, useParams } from "react-router-dom";
import "../Style.css";
import {
  Heading,
  Grid,
  Column,
  Section,
  Button,
  Loading,
  InlineNotification,
} from "@carbon/react";
import SearchPatientForm from "./SearchPatientForm";
import CreatePatientForm from "./CreatePatientForm";
import PageBreadCrumb from "../common/PageBreadCrumb";
import usePatientDetails from "./usePatientDetails";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "patient.label.modify", link: "/PatientManagement" },
];

function PatientManagement() {
  const history = useHistory();
  const { patientId } = useParams();

  const isNewMode = patientId === "new";
  const isEditMode = !!patientId && !isNewMode;
  const isSearchMode = !patientId;

  // Only fetch when an actual id is in the URL. New-mode and search-mode
  // render without a fetch.
  const { patient, loading, error } = usePatientDetails(
    isEditMode ? patientId : null,
  );

  const goToSearch = () => history.push("/PatientManagement");
  const goToNewPatient = () => history.push("/PatientManagement/new");
  const goToEditPatient = (selected) =>
    history.push(`/PatientManagement/${selected.patientPK}`);

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage id="patient.label.modify" />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      <br></br>
      <div className="orderLegendBody">
        <Grid>
          <Column lg={4} md={3} sm={2}>
            <Button
              id="searchPatient"
              kind={isSearchMode ? "primary" : "tertiary"}
              onClick={goToSearch}
            >
              <FormattedMessage
                id="search.patient.label"
                defaultMessage="Search for Patient"
              />
            </Button>
          </Column>
          <Column lg={4} md={3} sm={2}>
            <Button
              id="newPatient"
              kind={isNewMode || isEditMode ? "primary" : "tertiary"}
              onClick={goToNewPatient}
              disabled={isNewMode}
            >
              <FormattedMessage
                id="new.patient.label"
                defaultMessage="New Patient"
              />
            </Button>
          </Column>

          {isSearchMode && (
            <Column lg={16} md={8} sm={4}>
              <SearchPatientForm getSelectedPatient={goToEditPatient} />
            </Column>
          )}

          {isNewMode && (
            <Column lg={16} md={8} sm={4}>
              <CreatePatientForm
                key="new"
                showActionsButton={true}
                selectedPatient={{}}
              />
            </Column>
          )}

          {isEditMode && loading && (
            <Column lg={16} md={8} sm={4}>
              <Loading
                description={<FormattedMessage id="loading.label" />}
                withOverlay={false}
              />
            </Column>
          )}

          {isEditMode && !loading && error && (
            <Column lg={16} md={8} sm={4}>
              <InlineNotification
                kind="error"
                title={<FormattedMessage id="notification.title" />}
                subtitle={
                  <FormattedMessage
                    id="patient.fetch.error"
                    defaultMessage="Could not load patient. The id may be invalid or the server is unreachable."
                  />
                }
                hideCloseButton
              />
              <br />
              <Button kind="tertiary" onClick={goToSearch}>
                <FormattedMessage
                  id="search.patient.label"
                  defaultMessage="Search for Patient"
                />
              </Button>
            </Column>
          )}

          {isEditMode && !loading && !error && patient && (
            <Column lg={16} md={8} sm={4}>
              <CreatePatientForm
                key={patient.patientPK}
                showActionsButton={true}
                selectedPatient={patient}
              />
            </Column>
          )}
        </Grid>
      </div>
    </>
  );
}

export default injectIntl(PatientManagement);
