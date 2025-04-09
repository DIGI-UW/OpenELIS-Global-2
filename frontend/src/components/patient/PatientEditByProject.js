import React from "react";
import { FormattedMessage, injectIntl } from "react-intl";
import "../Style.css";
import { Heading, Grid, Column, Section } from "@carbon/react";
import PatientEditByProjectForm from "./PatientEditByProjectForm";
import PageBreadCrumb from "../common/PageBreadCrumb";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "patient.label.modify", link: "/PatientEditByProject" },
];

function PatientEditByProject() {
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
          <Column lg={16} md={8} sm={4}>
            <PatientEditByProjectForm />
          </Column>
        </Grid>
      </div>
    </>
  );
}

export default injectIntl(PatientEditByProject);
