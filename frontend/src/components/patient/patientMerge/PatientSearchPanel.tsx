import React, { useState } from "react";
import { FormattedMessage } from "react-intl";
import { Button, Loading } from "@carbon/react";
import SearchPatientForm from "../SearchPatientForm";
import { getPatientMergeDetails } from "./patientMergeService";
import type { Nullable, PatientRecord, PatientSelectHandler } from "../types";

interface PatientSearchPanelProps {
  panelId: string;
  title: React.ReactNode;
  selectedPatient: Nullable<PatientRecord>;
  onPatientSelect: PatientSelectHandler;
  otherSelectedPatient: Nullable<PatientRecord>;
}

/**
 * One side of the merge: the shared patient search (server paged, with the
 * same arrows and Carbon pagination as every other patient search), minus the
 * patient already chosen on the other side. The search form goes away once a
 * patient is chosen and comes back fresh from "Search for different patient".
 */
function PatientSearchPanel({
  panelId,
  title,
  selectedPatient,
  onPatientSelect,
  otherSelectedPatient,
}: PatientSearchPanelProps) {
  const [loadingDetails, setLoadingDetails] = useState(false);
  const [searchInstance, setSearchInstance] = useState(0);

  const handlePatientSelect = async (patient: PatientRecord) => {
    const patientPK = String(patient.patientPK || patient.patientID || "");
    let selected: PatientRecord = {
      ...patient,
      id: patientPK,
      patientPK,
      patientID: patientPK,
      dob: patient.dob || patient.birthDateForDisplay,
    };
    setLoadingDetails(true);
    try {
      const details = await getPatientMergeDetails(patientPK);
      if (details) {
        selected = { ...selected, dataSummary: details.dataSummary };
      }
    } catch (error) {
      console.error("Failed to fetch patient details:", error);
    } finally {
      setLoadingDetails(false);
    }
    onPatientSelect(selected);
  };

  const excludePatientIds = otherSelectedPatient?.patientID
    ? [String(otherSelectedPatient.patientID)]
    : [];

  return (
    <div className="patientSelectionSection">
      <h4>{title}</h4>

      {loadingDetails && <Loading />}

      {!selectedPatient && (
        <SearchPatientForm
          key={searchInstance}
          idPrefix={panelId}
          excludePatientIds={excludePatientIds}
          getSelectedPatient={handlePatientSelect}
        />
      )}

      {selectedPatient && (
        <div className="changeSelectionButton">
          <Button
            kind="ghost"
            size="sm"
            onClick={() => {
              onPatientSelect(null);
              setSearchInstance((instance) => instance + 1);
            }}
          >
            <FormattedMessage id="patient.merge.searchDifferent" />
          </Button>
        </div>
      )}
    </div>
  );
}

export default PatientSearchPanel;
