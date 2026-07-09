import React from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Grid,
  Column,
  InlineNotification,
  TextArea,
  Checkbox,
} from "@carbon/react";

function ConfirmationStep({
  patient1Details,
  patient2Details,
  primaryPatientId,
  mergeReason,
  confirmed,
  onReasonChange,
  onConfirmedChange,
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
          id.identityType === "Unique Health ID" ||
          id.identityType === "SUBJECT NUMBER",
      );
      if (subjectNumber) return subjectNumber.identityValue;
      // Return first identifier if no subject number
      return patient.identifiers[0].identityValue;
    }
    // Fallback to patientId (DB ID)
    return patient.patientId;
  };

  // Determine which patient is primary and which is being merged
  const primaryPatient =
    patient1Details?.patientId === primaryPatientId
      ? patient1Details
      : patient2Details;
  const mergedPatient =
    patient1Details?.patientId === primaryPatientId
      ? patient2Details
      : patient1Details;

  // Combine data summaries from both patients
  const primarySummary = primaryPatient?.dataSummary || {};
  const mergedSummary = mergedPatient?.dataSummary || {};
  const dataSummary = {
    totalOrders:
      (primarySummary.totalOrders || 0) + (mergedSummary.totalOrders || 0),
    activeOrders:
      (primarySummary.activeOrders || 0) + (mergedSummary.activeOrders || 0),
    totalResults:
      (primarySummary.totalResults || 0) + (mergedSummary.totalResults || 0),
    totalSamples:
      (primarySummary.totalSamples || 0) + (mergedSummary.totalSamples || 0),
  };

  // Detect conflicting fields by comparing patient details
  const detectConflictingFields = () => {
    const conflicts = [];
    if (
      primaryPatient?.phoneNumber &&
      mergedPatient?.phoneNumber &&
      primaryPatient.phoneNumber !== mergedPatient.phoneNumber
    ) {
      conflicts.push("phone");
    }
    if (
      primaryPatient?.email &&
      mergedPatient?.email &&
      primaryPatient.email !== mergedPatient.email
    ) {
      conflicts.push("email");
    }
    if (
      primaryPatient?.address &&
      mergedPatient?.address &&
      primaryPatient.address !== mergedPatient.address
    ) {
      conflicts.push("address");
    }
    return conflicts;
  };

  const conflictingFields = detectConflictingFields();
  const hasConflicts = conflictingFields.length > 0;

  // Build conflict display messages
  const getConflictMessages = () => {
    const messages = [];
    if (conflictingFields.includes("phone")) {
      messages.push(
        intl.formatMessage(
          { id: "patient.merge.phonesDiffer" },
          {
            primary: primaryPatient?.phoneNumber || "-",
            secondary: mergedPatient?.phoneNumber || "-",
          },
        ),
      );
    }
    if (conflictingFields.includes("email")) {
      messages.push(
        intl.formatMessage(
          { id: "patient.merge.emailsDiffer" },
          {
            primary: primaryPatient?.email || "-",
            secondary: mergedPatient?.email || "-",
          },
        ),
      );
    }
    if (conflictingFields.includes("address")) {
      messages.push(
        intl.formatMessage(
          { id: "patient.merge.addressesDiffer" },
          {
            primary: primaryPatient?.address || "-",
            secondary: mergedPatient?.address || "-",
          },
        ),
      );
    }
    return messages;
  };

  // Collect all identifiers from both patients, grouped by type
  const getAllIdentifiers = () => {
    const identifiersByType = {};

    // Helper to add identifier to the grouped object
    const addIdentifier = (type, value, patientLabel) => {
      if (!type || !value) return;
      if (!identifiersByType[type]) {
        identifiersByType[type] = [];
      }
      // Avoid duplicates
      const existing = identifiersByType[type].find((id) => id.value === value);
      if (!existing) {
        identifiersByType[type].push({ value, patientLabel });
      }
    };

    // Add identifiers from primary patient
    if (primaryPatient?.identifiers) {
      primaryPatient.identifiers.forEach((id) => {
        addIdentifier(
          id.identityType,
          id.identityValue,
          intl.formatMessage({ id: "patient.merge.primaryLabel" }),
        );
      });
    }

    // Add identifiers from merged patient
    if (mergedPatient?.identifiers) {
      mergedPatient.identifiers.forEach((id) => {
        addIdentifier(
          id.identityType,
          id.identityValue,
          intl.formatMessage({ id: "patient.merge.mergingLabel" }),
        );
      });
    }

    return identifiersByType;
  };

  const allIdentifiers = getAllIdentifiers();
  const conflictMessages = getConflictMessages();

  return (
    <div className="confirmationContainer">
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          {/* Critical Warning Banner */}
          <InlineNotification
            kind="error"
            title={intl.formatMessage({ id: "label.warning" })}
            subtitle={intl.formatMessage({
              id: "patient.merge.warning.irreversible",
            })}
            hideCloseButton
            className="criticalWarningBanner"
          />
        </Column>

        {/* Merge Summary */}
        <Column lg={16} md={8} sm={4}>
          <div className="mergeSummarySection">
            <h4>
              <FormattedMessage id="patient.merge.mergeSummary" />
            </h4>
            <div className="summaryGrid">
              <span className="summaryLabel">
                <FormattedMessage id="patient.merge.primaryPatient" />:
              </span>
              <span className="summaryValue">
                {getDisplayId(primaryPatient)} -{" "}
                {getPatientName(primaryPatient)}
              </span>
              <span className="summaryLabel">
                <FormattedMessage id="patient.merge.mergingFrom" />:
              </span>
              <span className="summaryValue">
                {getDisplayId(mergedPatient)} - {getPatientName(mergedPatient)}
              </span>
            </div>
          </div>
        </Column>

        {/* Data to be Consolidated */}
        <Column lg={16} md={8} sm={4}>
          <div className="dataConsolidationSection">
            <h5>
              <FormattedMessage id="patient.merge.dataToConsolidate" />
            </h5>
            <ul className="dataConsolidationList">
              <li>
                <FormattedMessage id="patient.merge.orders" />:{" "}
                {dataSummary?.totalOrders || 0}
                {dataSummary?.activeOrders > 0 &&
                  ` (${dataSummary.activeOrders} active)`}
              </li>
              <li>
                <FormattedMessage id="patient.merge.testResults" />:{" "}
                {dataSummary?.totalResults || 0}
              </li>
              <li>
                <FormattedMessage id="patient.merge.samples" />:{" "}
                {dataSummary?.totalSamples || 0}
              </li>
              <li>
                <FormattedMessage id="patient.merge.auditEntries" />
              </li>
            </ul>
          </div>
        </Column>

        {/* Identifiers to Preserve */}
        <Column lg={16} md={8} sm={4}>
          <div className="identifiersPreserveSection">
            <h5>
              <FormattedMessage id="patient.merge.identifiersToPreserve" />
            </h5>
            <ul className="dataConsolidationList">
              {Object.entries(allIdentifiers).map(([type, values]) => (
                <li key={type}>
                  {type}: {values.map((v) => v.value).join(", ")}
                </li>
              ))}
              {Object.keys(allIdentifiers).length === 0 && (
                <li>
                  <FormattedMessage id="patient.merge.noIdentifiers" />
                </li>
              )}
            </ul>
          </div>
        </Column>

        {/* Conflicting Information */}
        {hasConflicts && (
          <Column lg={16} md={8} sm={4}>
            <div className="conflictingInfoSection">
              <h5>
                <FormattedMessage id="patient.merge.conflictingInfo" />
              </h5>
              <ul className="conflictingInfoList">
                {conflictMessages.map((message, index) => (
                  <li key={index}>{message}</li>
                ))}
              </ul>
              <p className="conflictNote">
                <FormattedMessage id="patient.merge.conflictNote" />
              </p>
            </div>
          </Column>
        )}

        {/* Reason for Merge */}
        <Column lg={16} md={8} sm={4}>
          <div className="reasonSection">
            <TextArea
              id="mergeReason"
              labelText={intl.formatMessage({ id: "patient.merge.reason" })}
              placeholder={intl.formatMessage({
                id: "patient.merge.reasonPlaceholder",
              })}
              value={mergeReason}
              onChange={(e) => onReasonChange(e.target.value)}
              rows={4}
              maxCount={1000}
              enableCounter
              required
            />
          </div>
        </Column>

        {/* Confirmation Checkbox */}
        <Column lg={16} md={8} sm={4}>
          <div className="confirmationCheckbox">
            <Checkbox
              id="confirmMerge"
              labelText={intl.formatMessage({
                id: "patient.merge.confirmCheckbox",
              })}
              checked={confirmed}
              onChange={(_, { checked }) => onConfirmedChange(checked)}
            />
          </div>
        </Column>
      </Grid>
    </div>
  );
}

export default ConfirmationStep;
