import React from "react";
import {Tag, Heading, Section } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";

const PatientHeaderReDesign = (props) => {
    const {
        id,
        patient,
        dob,
        gender,
        processing
    } = props;
    const intl = useIntl();

    return (
        <Section>
            <Heading>
                <FormattedMessage id={id} />
            </Heading>
            <div style={{ display: "flex", gap: "2rem", marginTop: "0.5rem" }}>
                <p>
                    <strong><FormattedMessage id="patient.patient" defaultMessage="Patient" />:</strong> {patient}
                </p>
                <p>
                    <strong><FormattedMessage id="patient.dob" defaultMessage="DOB" />:</strong> {dob}
                </p>
                <p>
                    <strong><FormattedMessage id="patient.gender" defaultMessage="Gender" />:</strong> {gender}
                </p>
                <p>
                    <strong><FormattedMessage id="pathology.processing" defaultMessage="Processing" />:</strong> {processing}
                </p>
            </div>
        </Section>
    ); 
}

export default PatientHeaderReDesign;