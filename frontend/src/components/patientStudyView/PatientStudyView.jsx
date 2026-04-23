import React, { useRef, useState } from "react";
import { Grid, Column, Section, Heading } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
import PatientStudySearch from "./PatientStudySearch";
import PatientStudyForm from "./PatientStudyForm";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "banner.menu.patient", link: "#" },
  { label: "banner.menu.patient.Create", link: "#" },
  { label: "banner.menu.patientConsult", link: "/PatientStudyView" },
];

const PatientStudyView = () => {
  const formRef = useRef(null);
  const [selectedPatient, setSelectedPatient] = useState(null);
  const [formData, setFormData] = useState(null);
  const [referenceLists, setReferenceLists] = useState(null);
  const [loading, setLoading] = useState(false);

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage
                  id="banner.menu.editPatient.ReadOnly"
                  defaultMessage="View Patient"
                />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>

      <div className="orderLegendBody">
        <Grid fullWidth={true}>
          <PatientStudySearch
            setSelectedPatient={setSelectedPatient}
            setFormData={setFormData}
            setReferenceLists={setReferenceLists}
            setLoading={setLoading}
            formRef={formRef}
          />
        </Grid>

        <PatientStudyForm
          formRef={formRef}
          selectedPatient={selectedPatient}
          formData={formData}
          referenceLists={referenceLists}
          loading={loading}
        />
      </div>
    </>
  );
};

export default PatientStudyView;
