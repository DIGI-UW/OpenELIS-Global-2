import React from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Grid,
  Column,
  InlineNotification,
  RadioButton,
  Accordion,
  AccordionItem,
} from "@carbon/react";

function PrimarySelectionStep({
  patient1Details,
  patient2Details,
  primaryPatientId,
  onPrimarySelect,
}) {
  const intl = useIntl();

  // Get display name from firstName and lastName
  const getPatientName = (patient) => {
    if (!patient) return "-";
    const parts = [];
    if (patient.firstName) parts.push(patient.firstName);
    if (patient.lastName) parts.push(patient.lastName);
    return parts.length > 0 ? parts.join(" ") : "-";
  };

  // Get display ID - prioritize nationalId or first identifier over DB ID
  const getDisplayId = (patient) => {
    if (!patient) return "";
    // Check nationalId first
    if (patient.nationalId) return patient.nationalId;
    // Check identifiers for subject number or other IDs
    if (patient.identifiers && patient.identifiers.length > 0) {
      const subjectNumber = patient.identifiers.find(
        (id) =>
          id.identityType === "Subject Number" ||
          id.identityType === "Unique Health ID",
      );
      if (subjectNumber) return subjectNumber.identityValue;
      // Return first identifier if no subject number
      return patient.identifiers[0].identityValue;
    }
    // Fallback to patientId (DB ID)
    return patient.patientId;
  };

  const renderDemographics = (patient) => {
    if (!patient) return null;

    return (
      <div className="demographicsGrid">
        <div className="demographicsItem">
          <span className="label">
            <FormattedMessage id="patient.merge.name" />
          </span>
          <span className="value">{getPatientName(patient)}</span>
        </div>
        <div className="demographicsItem">
          <span className="label">
            <FormattedMessage id="patient.merge.dob" />
          </span>
          <span className="value">{patient.birthDate || "-"}</span>
        </div>
        <div className="demographicsItem">
          <span className="label">
            <FormattedMessage id="patient.merge.gender" />
          </span>
          <span className="value">
            {patient.gender === "M"
              ? intl.formatMessage({ id: "patient.male" })
              : patient.gender === "F"
                ? intl.formatMessage({ id: "patient.female" })
                : patient.gender || "-"}
          </span>
        </div>
        <div className="demographicsItem">
          <span className="label">
            <FormattedMessage id="patient.merge.phone" />
          </span>
          <span className="value">
            {patient.phoneNumber ||
              intl.formatMessage({ id: "patient.merge.notRecorded" })}
          </span>
        </div>
        <div className="demographicsItem">
          <span className="label">
            <FormattedMessage id="patient.merge.email" />
          </span>
          <span className="value">
            {patient.email ||
              intl.formatMessage({ id: "patient.merge.notRecorded" })}
          </span>
        </div>
        <div className="demographicsItem" style={{ gridColumn: "1 / -1" }}>
          <span className="label">
            <FormattedMessage id="patient.merge.address" />
          </span>
          <span className="value">
            {patient.address ||
              intl.formatMessage({ id: "patient.merge.notRecorded" })}
          </span>
        </div>
      </div>
    );
  };

  const renderClinicalSummary = (patient) => {
    if (!patient) return null;

    const summary = patient.dataSummary || {};

    return (
      <div className="clinicalSummary">
        <div className="clinicalSummaryGrid">
          <div className="demographicsItem">
            <span className="label">
              <FormattedMessage id="patient.merge.activeOrders" />
            </span>
            <span className="value">{summary.activeOrders || 0}</span>
          </div>
          <div className="demographicsItem">
            <span className="label">
              <FormattedMessage id="patient.merge.totalResults" />
            </span>
            <span className="value">{summary.totalResults || 0}</span>
          </div>
          <div className="demographicsItem">
            <span className="label">
              <FormattedMessage id="patient.merge.totalSamples" />
            </span>
            <span className="value">{summary.totalSamples || 0}</span>
          </div>
        </div>
      </div>
    );
  };

  const renderIdentifiers = (patient) => {
    if (!patient || !patient.identifiers || patient.identifiers.length === 0) {
      return (
        <div className="identifiersList">
          <p>{intl.formatMessage({ id: "patient.merge.notRecorded" })}</p>
        </div>
      );
    }

    return (
      <div className="identifiersList">
        {patient.identifiers.map((identifier, index) => (
          <div key={index} className="identifierItem">
            <span className="label">{identifier.identityType}</span>
            <span className="value">{identifier.identityValue}</span>
          </div>
        ))}
      </div>
    );
  };

  const renderPatientOption = (patient, patientNumber) => {
    if (!patient) return null;

    const isSelected = primaryPatientId === patient.patientId;
    const displayId = getDisplayId(patient);
    const patientName = getPatientName(patient);
    const labelText = `${intl.formatMessage({ id: "patient.label" })} ${patientNumber}: ${displayId} - ${patientName}`;

    return (
      <div className="patientOption">
        <RadioButton
          id={`patient-${patientNumber}`}
          name="primaryPatient"
          labelText={labelText}
          value={patient.patientId}
          checked={isSelected}
          onClick={() => onPrimarySelect(patient.patientId)}
        />

        <Accordion className="patientAccordion">
          <AccordionItem
            title={intl.formatMessage({ id: "patient.merge.demographics" })}
            open
          >
            {renderDemographics(patient)}
          </AccordionItem>
          <AccordionItem
            title={intl.formatMessage({ id: "patient.merge.clinicalSummary" })}
          >
            {renderClinicalSummary(patient)}
          </AccordionItem>
          <AccordionItem
            title={intl.formatMessage({ id: "patient.merge.identifiers" })}
          >
            {renderIdentifiers(patient)}
          </AccordionItem>
        </Accordion>
      </div>
    );
  };

  return (
    <div className="primarySelectionContainer">
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          {/* Warning Banner */}
          <InlineNotification
            kind="warning"
            title={intl.formatMessage({ id: "label.warning" })}
            subtitle={intl.formatMessage({
              id: "patient.merge.warning.dataLink",
            })}
            hideCloseButton
            className="warningBanner"
          />
        </Column>

        <Column lg={16} md={8} sm={4}>
          <p className="primarySelectionQuestion">
            <FormattedMessage id="patient.merge.selectPrimaryDescription" />
          </p>
        </Column>

        <Column lg={16} md={8} sm={4}>
          <div className="primarySelectionOptions">
            {renderPatientOption(patient1Details, 1)}
            {renderPatientOption(patient2Details, 2)}
          </div>
        </Column>
      </Grid>
    </div>
  );
}

export default PrimarySelectionStep;
