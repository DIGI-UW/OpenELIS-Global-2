import React from "react";
import { FormattedMessage, injectIntl } from "react-intl";
import "../Style.css";
import { Heading, Grid, Column, Section } from "@carbon/react";
import SearchPatientForm from "./SearchPatientForm";
import { useState, useEffect, useRef } from "react";
import PageBreadCrumb from "../common/PageBreadCrumb";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "label.page.patientHistory", link: "/PatientHistory" },
];
const PatientHistory = () => {
  const [selectedPatient, setSelectedPatient] = useState({});
  const componentMounted = useRef(false);

  const getSelectedPatient = (patient) => {
    if (componentMounted.current) {
      setSelectedPatient(patient);
    }
  };
  useEffect(() => {
    componentMounted.current = true;
    openPatientResults(selectedPatient.patientPK);

    return () => {
      componentMounted.current = false;
    };
  }, [selectedPatient]);

  const openPatientResults = (patientId) => {
    if (patientId) {
      window.location.href = "/PatientResults/" + patientId;
    }
  };

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
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
      <br></br>

      <div className="orderLegendBody">
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <SearchPatientForm getSelectedPatient={getSelectedPatient} />
          </Column>
        </Grid>
      </div>
    </>
  );
};
export default injectIntl(PatientHistory);
