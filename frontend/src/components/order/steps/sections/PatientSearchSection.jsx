import React, { useState } from "react";
import { FormattedMessage } from "react-intl";
import { Tile, Button, Tag, Link } from "@carbon/react";
import SearchPatientForm from "../../../patient/SearchPatientForm";
import CreatePatientForm from "../../../patient/CreatePatientForm";

/**
 * PatientSearchSection - Patient search with results table and selection card
 *
 * The search itself is the shared SearchPatientForm, so the order lanes and
 * Patient Management search the same way. It stays mounted while a patient is
 * selected or the New Patient tab is open, so clearing the selection brings
 * back the criteria and results the user had.
 *
 * Implements:
 * - ORD-2: Patient search (local + Client Registry)
 * - ORD-9: Selected patient summary card
 * - XC-2: Unified search pattern
 */

const PatientSearchSection = ({
  orderData,
  setOrderData,
  setPhoneValidation,
  isReadOnly,
}) => {
  const [activeTab, setActiveTab] = useState("search");
  const [locallySelectedPatient, setSelectedPatient] = useState(null);

  const selectedPatient = orderData?.patientProperties?.patientPK
    ? orderData.patientProperties
    : locallySelectedPatient;

  const handleSelectPatient = (patient) => {
    setSelectedPatient(patient);
    setOrderData((prev) => ({
      ...prev,
      patientUpdateStatus: "UPDATE",
      patientProperties: {
        ...patient,
        patientUpdateStatus: "UPDATE",
      },
    }));
  };

  const handleClearSelection = () => {
    setSelectedPatient(null);
    setOrderData((prev) => ({
      ...prev,
      patientUpdateStatus: "",
      patientProperties: {
        patientPK: "",
        guid: "",
        firstName: "",
        lastName: "",
        birthDateForDisplay: "",
        gender: "",
        nationalId: "",
      },
    }));
  };

  const showSearchForm =
    activeTab === "search" && !selectedPatient && !isReadOnly;

  return (
    <Tile
      className="order-section patient-search-section"
      data-testid="patient-search-section"
    >
      <h4 className="section-title">
        <FormattedMessage id="banner.menu.patient" defaultMessage="Patient" />
      </h4>
      <p className="helper-text">
        <FormattedMessage
          id="patient.search.section.helper"
          defaultMessage="Search by any combination of fields — partial matches accepted. 'External Search' queries the Client Registry and requires at minimum a name and date of birth."
        />
      </p>

      <div className="section-tabs">
        <Button
          kind={activeTab === "search" ? "primary" : "tertiary"}
          size="md"
          onClick={() => setActiveTab("search")}
        >
          <FormattedMessage
            id="search.patient.label"
            defaultMessage="Search for Patient"
          />
        </Button>
        <Button
          kind={activeTab === "new" ? "primary" : "tertiary"}
          size="md"
          onClick={() => setActiveTab("new")}
          disabled={isReadOnly}
        >
          <FormattedMessage
            id="new.patient.label"
            defaultMessage="New Patient"
          />
        </Button>
      </div>

      {activeTab === "search" && selectedPatient && (
        <div className="search-content">
          <div className="selected-entity-card">
            <div className="selected-card-header">
              <Tag type="green" size="sm">
                <FormattedMessage id="selected" defaultMessage="Selected" />
              </Tag>
              {!isReadOnly && (
                <Link onClick={handleClearSelection}>
                  <FormattedMessage
                    id="label.button.clear"
                    defaultMessage="Clear"
                  />
                </Link>
              )}
            </div>
            <div className="selected-card-content">
              <h5>
                {selectedPatient.firstName} {selectedPatient.lastName}
              </h5>
              <p>
                {selectedPatient.birthDateForDisplay &&
                  `DOB: ${selectedPatient.birthDateForDisplay}`}
                {selectedPatient.gender && ` · ${selectedPatient.gender}`}
                {selectedPatient.nationalId &&
                  ` · ID: ${selectedPatient.nationalId}`}
              </p>
            </div>
          </div>
        </div>
      )}

      {!isReadOnly && (
        <div
          className="search-content"
          data-testid="order-patient-search"
          hidden={!showSearchForm}
        >
          <SearchPatientForm
            idPrefix="order-patient-search"
            getSelectedPatient={handleSelectPatient}
            renderNotifications={false}
          />
        </div>
      )}

      {activeTab === "new" && (
        <div className="new-patient-content">
          <CreatePatientForm
            key={(selectedPatient && selectedPatient.patientPK) || "new"}
            showActionsButton={false}
            selectedPatient={
              selectedPatient || {
                id: "",
                healthRegion: [],
                nationalId: "",
                subjectNumber: "",
              }
            }
            orderFormValues={orderData}
            setOrderFormValues={setOrderData}
            error={() => null}
            setPhoneValidation={setPhoneValidation}
          />
        </div>
      )}
    </Tile>
  );
};

export default PatientSearchSection;
