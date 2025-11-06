import React, { useState } from "react";
import { FormattedMessage, injectIntl } from "react-intl";
import "../Style.css";
import { Heading, Grid, Column, Section, Button } from "@carbon/react";
import SearchPatientForm from "./SearchPatientForm";
import CreatePatientForm from "./CreatePatientForm";
import PageBreadCrumb from "../common/PageBreadCrumb";
let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "patient.label.modify", link: "/PatientManagement" },
];

function PatientManagement() {
  const [selectedPatient, setSelectedPatient] = useState({});
  const [searchPatientTab, setSearchPatientTab] = useState({
    kind: "primary",
    active: true,
  });
  const [newPatientTab, setNewPatientTab] = useState({
    kind: "tertiary",
    active: false,
  });

  const handleSearchPatientTab = () => {
    setNewPatientTab({ kind: "tertiary", active: false });
    setSearchPatientTab({ kind: "primary", active: true });
  };

  const handleNewPatientTab = () => {
    setNewPatientTab({ kind: "primary", active: true });
    setSearchPatientTab({ kind: "tertiary", active: false });
  };

  const getSelectedPatient = (patient) => {
    setSelectedPatient(patient);
    setNewPatientTab({ kind: "primary", active: true });
    setSearchPatientTab({ kind: "tertiary", active: false });
  };

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
              kind={searchPatientTab.kind}
              onClick={handleSearchPatientTab}
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
              kind={newPatientTab.kind}
              onClick={handleNewPatientTab}
            >
              <FormattedMessage
                id="new.patient.label"
                defaultMessage="New Patient"
              />
            </Button>
          </Column>

          {searchPatientTab.active && (
            <Column lg={16} md={8} sm={4}>
              <SearchPatientForm
                getSelectedPatient={getSelectedPatient}
              ></SearchPatientForm>
            </Column>
          )}

          <br></br>
          {newPatientTab.active && (
            <Column lg={16} md={8} sm={4}>
              <CreatePatientForm
                showActionsButton={true}
                selectedPatient={selectedPatient}
              ></CreatePatientForm>
            </Column>
          )}
        </Grid>
      </div>
    </>
  );
}

export default injectIntl(PatientManagement);
