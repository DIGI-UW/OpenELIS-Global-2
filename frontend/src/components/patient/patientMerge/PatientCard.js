import React from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { Button } from "@carbon/react";
import { Close, User } from "@carbon/react/icons";

function PatientCard({ patient, onClear, showClearButton = true }) {
  const intl = useIntl();

  // Get the display ID - prefer subjectNumber (Unique Health ID), then nationalId, then fall back to DB ID
  const getDisplayId = (patient) => {
    if (patient.subjectNumber) {
      return patient.subjectNumber;
    }
    if (patient.nationalId) {
      return patient.nationalId;
    }
    // Fallback to database ID if no other identifier available
    return patient.patientID || patient.patientPK;
  };

  // Build full address string
  const getFullAddress = (patient) => {
    const parts = [];
    if (patient.streetAddress) {
      parts.push(patient.streetAddress);
    } else if (patient.streetName || patient.flatNumberApartmentName) {
      parts.push(
        `${patient.flatNumberApartmentName || ""} ${patient.streetName || ""}`.trim(),
      );
    }
    if (patient.town) parts.push(patient.town);
    if (patient.county) parts.push(patient.county);
    if (patient.postalCode) parts.push(patient.postalCode);

    if (parts.length === 0 && patient.address) {
      return typeof patient.address === "object"
        ? `${patient.address.street || ""} ${patient.address.city || ""}`.trim()
        : patient.address;
    }
    return parts.join(", ");
  };

  // Check if we have any identifiers to show (excluding the one shown in header)
  const hasIdentifiers = () => {
    const displayId = getDisplayId(patient);
    // Show nationalId if it's different from display ID
    const showNationalId =
      patient.nationalId && patient.nationalId !== displayId;
    // Show subjectNumber if it's different from display ID
    const showSubjectNumber =
      patient.subjectNumber && patient.subjectNumber !== displayId;
    return (
      showNationalId ||
      showSubjectNumber ||
      patient.stNumber ||
      patient.externalId
    );
  };

  // Check if we have any contact info
  const hasContactInfo = () => {
    return patient.contactPhone || patient.contactEmail;
  };

  // Check if we have any address info
  const hasAddressInfo = () => {
    return getFullAddress(patient);
  };

  if (!patient) {
    return (
      <div className="selectedPatientCard empty">
        <FormattedMessage id="patient.merge.noPatientSelected" />
      </div>
    );
  }

  const displayId = getDisplayId(patient);
  const fullAddress = getFullAddress(patient);

  return (
    <div className="selectedPatientCard">
      {/* Header with Patient ID */}
      <div className="selectedPatientHeader">
        <h5>
          <User size={20} />
          <FormattedMessage id="patient.id" />: {displayId}
        </h5>
        {showClearButton && onClear && (
          <Button
            kind="ghost"
            size="sm"
            hasIconOnly
            renderIcon={Close}
            iconDescription="Clear selection"
            onClick={onClear}
          />
        )}
      </div>

      {/* Demographics Section */}
      <div className="patientCardSection">
        <div className="patientDetailRow">
          <span className="label">
            <FormattedMessage id="patient.merge.name" />
          </span>
          <span className="value">
            {patient.firstName} {patient.lastName}
          </span>
        </div>
        <div className="patientDetailRow">
          <span className="label">
            <FormattedMessage id="patient.merge.dob" />
          </span>
          <span className="value">
            {patient.dob || patient.birthDate || "-"}
          </span>
        </div>
        <div className="patientDetailRow">
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
      </div>

      {/* Identifiers Section - grouped together */}
      {hasIdentifiers() && (
        <div className="patientCardSection">
          <div className="sectionLabel">
            <FormattedMessage id="patient.merge.identifiers" />
          </div>
          {/* Show nationalId only if different from header display ID */}
          {patient.nationalId && patient.nationalId !== displayId && (
            <div className="patientDetailRow">
              <span className="label">
                <FormattedMessage id="patient.merge.nationalId" />
              </span>
              <span className="value">{patient.nationalId}</span>
            </div>
          )}
          {/* Show subjectNumber only if different from header display ID */}
          {patient.subjectNumber && patient.subjectNumber !== displayId && (
            <div className="patientDetailRow">
              <span className="label">
                <FormattedMessage id="patient.subject.number" />
              </span>
              <span className="value">{patient.subjectNumber}</span>
            </div>
          )}
          {patient.stNumber && (
            <div className="patientDetailRow">
              <span className="label">
                <FormattedMessage id="patient.st.number" />
              </span>
              <span className="value">{patient.stNumber}</span>
            </div>
          )}
          {patient.externalId && (
            <div className="patientDetailRow">
              <span className="label">
                <FormattedMessage id="patient.externalId" />
              </span>
              <span className="value">{patient.externalId}</span>
            </div>
          )}
        </div>
      )}

      {/* Contact Information Section */}
      {hasContactInfo() && (
        <div className="patientCardSection">
          <div className="sectionLabel">
            <FormattedMessage id="patient.merge.contactInfo" />
          </div>
          {patient.contactPhone && (
            <div className="patientDetailRow">
              <span className="label">
                <FormattedMessage id="patient.merge.phone" />
              </span>
              <span className="value">{patient.contactPhone}</span>
            </div>
          )}
          {patient.contactEmail && (
            <div className="patientDetailRow">
              <span className="label">
                <FormattedMessage id="patient.merge.email" />
              </span>
              <span className="value">{patient.contactEmail}</span>
            </div>
          )}
        </div>
      )}

      {/* Address Section */}
      {hasAddressInfo() && (
        <div className="patientCardSection">
          <div className="patientDetailRow">
            <span className="label">
              <FormattedMessage id="patient.merge.address" />
            </span>
            <span className="value">{fullAddress}</span>
          </div>
        </div>
      )}

      {/* Statistics Section - always shown */}
      <hr className="statisticsDivider" />
      <div className="patientStatistics">
        <div className="statisticItem">
          <span className="statisticValue">
            {patient.dataSummary?.activeOrders ?? "-"}
          </span>
          <span className="statisticLabel">
            <FormattedMessage id="patient.merge.activeOrders" />
          </span>
        </div>
        <div className="statisticItem">
          <span className="statisticValue">
            {patient.dataSummary?.totalResults ?? "-"}
          </span>
          <span className="statisticLabel">
            <FormattedMessage id="patient.merge.totalResults" />
          </span>
        </div>
        <div className="statisticItem">
          <span className="statisticValue">
            {patient.dataSummary?.totalSamples ?? "-"}
          </span>
          <span className="statisticLabel">
            <FormattedMessage id="patient.merge.totalSamples" />
          </span>
        </div>
      </div>
    </div>
  );
}

export default PatientCard;
