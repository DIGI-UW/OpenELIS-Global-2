import React, { useRef, useState } from "react";
import { Grid, Column, Section, Heading } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
import PatientStudyEditSearch from "./PatientStudyEditSearch";
import PatientStudyEditForm from "./PatientStudyEditForm";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "banner.menu.patient", link: "#" },
  { label: "banner.menu.patient.Create", link: "#" },
  { label: "banner.menu.patientStudyEdit", link: "/PatientStudyEdit" },
];

const PatientStudyEditView = () => {
  const formRef = useRef(null);
  const [selectedPatient, setSelectedPatient] = useState(null);
  const [formData, setFormData] = useState(null);
  const [referenceLists, setReferenceLists] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleCancel = () => {
    setSelectedPatient(null);
    setFormData(null);
  };

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage
                  id="banner.menu.editPatient.ReadWrite"
                  defaultMessage="Edit Patient"
                />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>

      <div className="orderLegendBody">
        <Grid fullWidth={true}>
          <PatientStudyEditSearch
            setSelectedPatient={setSelectedPatient}
            setFormData={setFormData}
            setReferenceLists={setReferenceLists}
            setLoading={setLoading}
            formRef={formRef}
          />
        </Grid>

        <PatientStudyEditForm
          formRef={formRef}
          selectedPatient={selectedPatient}
          formData={formData}
          referenceLists={referenceLists}
          loading={loading}
          onCancel={handleCancel}
        />
      </div>
    </>
  );
};

export default PatientStudyEditView;
