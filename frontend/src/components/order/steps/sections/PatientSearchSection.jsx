import React, { useContext, useState } from "react";
import { FormattedMessage } from "react-intl";
import { Tile, Button, Tag, Link } from "@carbon/react";
import SearchPatientForm from "../../../patient/SearchPatientForm";
import CreatePatientForm from "../../../patient/CreatePatientForm";
import { OrderContext, SaveStatus } from "../../OrderContext";

/**
 * PatientSearchSection - Patient search with results table and selection card
 *
 * The search itself is the shared SearchPatientForm, so the order lanes and
 * Patient Management search the same way. Clearing the selected patient
 * starts a new, empty search.
 *
 * Implements:
 * - ORD-2: Patient search (local + Client Registry)
 * - ORD-9: Selected patient summary card
 * - XC-2: Unified search pattern
 */

/**
 * The blank record a New Patient form starts from. One shared object: the form
 * treats a new `selectedPatient` as a different patient and rewrites the order
 * from its fields, so a fresh literal on every render would let the form
 * overwrite the patient the order has just saved.
 */
const NEW_PATIENT = {
  id: "",
  healthRegion: [],
  nationalId: "",
  subjectNumber: "",
};

const PatientSearchSection = ({
  orderData,
  setOrderData,
  setPhoneValidation,
  isReadOnly,
  required = false,
}) => {
  const [activeTab, setActiveTab] = useState("search");
  const [locallySelectedPatient, setSelectedPatient] = useState(null);
  const [searchInstance, setSearchInstance] = useState(0);

  // The patient the order holds, as it was when it became the order's patient
  // (chosen from the search, loaded with the order) or as it was last saved.
  // The patient form compares its fields against this record to tell an
  // untouched patient from an edited one, so it must not follow the form's
  // own writes; it is taken again only for another patient or after a save.
  const { saveStatus } = useContext(OrderContext);
  const heldPatientPK = orderData?.patientProperties?.patientPK || "";
  const [held, setHeld] = useState({
    patientPK: "",
    saveStatus,
    patient: null,
  });
  const takeHeldPatient =
    held.patientPK !== heldPatientPK ||
    (saveStatus === SaveStatus.SAVED && held.saveStatus !== SaveStatus.SAVED);
  if (takeHeldPatient || held.saveStatus !== saveStatus) {
    setHeld({
      patientPK: heldPatientPK,
      saveStatus,
      patient: takeHeldPatient
        ? (heldPatientPK && orderData.patientProperties) || null
        : held.patient,
    });
  }
  const heldPatient = takeHeldPatient
    ? (heldPatientPK && orderData.patientProperties) || null
    : held.patient;

  const selectedPatient = heldPatient || locallySelectedPatient;

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
    setSearchInstance((instance) => instance + 1);
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
        {required && <span className="required-indicator"> *</span>}
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
            key={searchInstance}
            idPrefix="order-patient-search"
            getSelectedPatient={handleSelectPatient}
            renderNotifications={false}
            followUrlLabNumber={false}
          />
        </div>
      )}

      {activeTab === "new" && (
        <div className="new-patient-content">
          <CreatePatientForm
            key={(selectedPatient && selectedPatient.patientPK) || "new"}
            showActionsButton={false}
            selectedPatient={selectedPatient || NEW_PATIENT}
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
