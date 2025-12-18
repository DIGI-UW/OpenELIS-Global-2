import React from "react";
import { FormattedMessage } from "react-intl";
import { Grid, Column } from "@carbon/react";
import PatientSearchPanel from "./PatientSearchPanel";
import PatientCard from "./PatientCard";

function PatientSelectionStep({
  patient1,
  patient2,
  onPatient1Select,
  onPatient2Select,
}) {
  return (
    <div className="patientSelectionContainer">
      <Grid fullWidth={true}>
        {/* Select First Patient Section */}
        <Column lg={16} md={8} sm={4}>
          <PatientSearchPanel
            panelId="patient1"
            title={<FormattedMessage id="patient.merge.selectFirstPatient" />}
            selectedPatient={patient1}
            onPatientSelect={onPatient1Select}
            otherSelectedPatient={patient2}
          />
          <PatientCard
            patient={patient1}
            onClear={() => onPatient1Select(null)}
          />
        </Column>

        {/* Divider */}
        <Column lg={16} md={8} sm={4}>
          <hr style={{ margin: "2rem 0", borderColor: "#e0e0e0" }} />
        </Column>

        {/* Select Second Patient Section */}
        <Column lg={16} md={8} sm={4}>
          <PatientSearchPanel
            panelId="patient2"
            title={<FormattedMessage id="patient.merge.selectSecondPatient" />}
            selectedPatient={patient2}
            onPatientSelect={onPatient2Select}
            otherSelectedPatient={patient1}
          />
          <PatientCard
            patient={patient2}
            onClear={() => onPatient2Select(null)}
          />
        </Column>
      </Grid>
    </div>
  );
}

export default PatientSelectionStep;
