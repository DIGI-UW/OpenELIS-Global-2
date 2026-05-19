import React from "react";
import { FormattedMessage, injectIntl } from "react-intl";
import { useHistory } from "react-router-dom";
import "../Style.css";
import { Heading, Grid, Column, Section } from "@carbon/react";
import SearchPatientForm from "./SearchPatientForm";
import PageBreadCrumb from "../common/PageBreadCrumb";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "label.page.patientHistory", link: "/PatientHistory" },
];
const PatientHistory = () => {
  const history = useHistory();

  const getSelectedPatient = (patient) => {
    if (patient?.patientPK) {
      history.push("/PatientResults/" + patient.patientPK);
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
