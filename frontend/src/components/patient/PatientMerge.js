import React, { useState, useContext } from "react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import {
  ProgressIndicator,
  ProgressStep,
  Button,
  Loading,
  InlineNotification,
  Stack,
} from "@carbon/react";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog, NotificationKinds } from "../common/CustomNotification";
import PatientSelectionStep from "./patientMerge/PatientSelectionStep";
import PrimarySelectionStep from "./patientMerge/PrimarySelectionStep";
import ConfirmationStep from "./patientMerge/ConfirmationStep";
import {
  getPatientMergeDetails,
  executePatientMerge,
  getErrorMessage,
} from "./patientMerge/patientMergeService";
import "./patientMerge/PatientMerge.scss";
import "../../components/Style.css";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "patient.merge.title", link: "/PatientMerge" },
];

// Step constants
const STEP_SELECT = 0;
const STEP_COMPARE = 1;
const STEP_CONFIRM = 2;

function PatientMerge() {
  const intl = useIntl();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  // Wizard state
  const [currentStep, setCurrentStep] = useState(STEP_SELECT);

  // Patient data state
  const [patient1, setPatient1] = useState(null);
  const [patient2, setPatient2] = useState(null);
  const [patient1Details, setPatient1Details] = useState(null);
  const [patient2Details, setPatient2Details] = useState(null);
  const [primaryPatientId, setPrimaryPatientId] = useState(null);

  // Merge state
  const [mergeReason, setMergeReason] = useState("");
  const [confirmed, setConfirmed] = useState(false);
  const [mergeResult, setMergeResult] = useState(null);

  // UI state
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  // Fetch patient merge details when moving to compare step
  const fetchPatientDetails = async () => {
    if (!patient1 || !patient2) return false;

    setIsLoading(true);
    setError(null);

    try {
      const [details1, details2] = await Promise.all([
        getPatientMergeDetails(patient1.patientPK),
        getPatientMergeDetails(patient2.patientPK),
      ]);
      setPatient1Details(details1);
      setPatient2Details(details2);
      return true;
    } catch (err) {
      const errorMsg = getErrorMessage(err, intl);
      setError(errorMsg);
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: errorMsg,
        kind: NotificationKinds.error,
      });
      setNotificationVisible(true);
      return false;
    } finally {
      setIsLoading(false);
    }
  };

  // Execute the merge
  const executeMerge = async () => {
    if (!confirmed || !mergeReason.trim()) return;

    setIsLoading(true);
    setError(null);

    try {
      const result = await executePatientMerge({
        patient1Id: patient1Details.patientId,
        patient2Id: patient2Details.patientId,
        primaryPatientId: primaryPatientId,
        reason: mergeReason,
      });

      setMergeResult(result);
      addNotification({
        title: intl.formatMessage({ id: "patient.merge.success" }),
        message: intl.formatMessage(
          { id: "patient.merge.successMessage" },
          {
            mergedId: result.mergedPatientId,
            primaryId: result.primaryPatientId,
            auditId: result.auditId,
          },
        ),
        kind: NotificationKinds.success,
      });
      setNotificationVisible(true);

      // Reset to initial state after successful merge
      setTimeout(() => {
        resetWizard();
      }, 3000);
    } catch (err) {
      const errorMsg = getErrorMessage(err, intl);
      setError(errorMsg);
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: errorMsg,
        kind: NotificationKinds.error,
      });
      setNotificationVisible(true);
    } finally {
      setIsLoading(false);
    }
  };

  // Reset wizard to initial state
  const resetWizard = () => {
    setCurrentStep(STEP_SELECT);
    setPatient1(null);
    setPatient2(null);
    setPatient1Details(null);
    setPatient2Details(null);
    setPrimaryPatientId(null);
    setMergeReason("");
    setConfirmed(false);
    setMergeResult(null);
    setError(null);
  };

  // Navigation handlers
  const handleNext = async () => {
    if (currentStep === STEP_SELECT) {
      if (!patient1 || !patient2) {
        setError(intl.formatMessage({ id: "patient.merge.error.selectBoth" }));
        return;
      }
      const success = await fetchPatientDetails();
      if (success) {
        setCurrentStep(STEP_COMPARE);
      }
    } else if (currentStep === STEP_COMPARE) {
      if (!primaryPatientId) {
        setError(
          intl.formatMessage({ id: "patient.merge.error.selectPrimary" }),
        );
        return;
      }
      // No need for server-side validation here - we already have the patient details
      // and the user has selected both patients from search results.
      // The confirmation step will show any conflicts based on the details we have.
      setCurrentStep(STEP_CONFIRM);
    }
  };

  const handleBack = () => {
    if (currentStep > STEP_SELECT) {
      setCurrentStep(currentStep - 1);
      setError(null);
    }
  };

  const handleCancel = () => {
    resetWizard();
    window.location.href = "/";
  };

  // Handle patient selection
  const handlePatient1Select = (patient) => {
    setPatient1(patient);
    setError(null);
  };

  const handlePatient2Select = (patient) => {
    setPatient2(patient);
    setError(null);
  };

  // Check if next button should be disabled
  const isNextDisabled = () => {
    if (isLoading) return true;
    if (currentStep === STEP_SELECT) {
      // Require both patients to be selected and they must be different
      if (!patient1 || !patient2) return true;
      if (patient1.patientID === patient2.patientID) return true;
      return false;
    }
    if (currentStep === STEP_COMPARE) {
      return !primaryPatientId;
    }
    return false;
  };

  // Check if the same patient was selected twice
  const isSamePatientSelected = () => {
    return patient1 && patient2 && patient1.patientID === patient2.patientID;
  };

  // Check if confirm button should be disabled
  const isConfirmDisabled = () => {
    return isLoading || !confirmed || !mergeReason.trim();
  };

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Stack gap={10}>
        <div className="pageContent">
          {notificationVisible && <AlertDialog />}
          {isLoading && <Loading />}

          <div className="orderWorkFlowDiv">
            <h2>
              <FormattedMessage id="patient.merge.title" />
            </h2>

            {/* Progress Indicator */}
            <ProgressIndicator
              currentIndex={currentStep}
              className="patientMergeProgress"
              spaceEqually={true}
            >
              <ProgressStep
                label={intl.formatMessage({ id: "patient.merge.step.select" })}
              />
              <ProgressStep
                label={intl.formatMessage({ id: "patient.merge.step.compare" })}
              />
              <ProgressStep
                label={intl.formatMessage({ id: "patient.merge.step.confirm" })}
              />
            </ProgressIndicator>

            {/* Error Display */}
            {error && (
              <InlineNotification
                kind="error"
                title={intl.formatMessage({ id: "error.title" })}
                subtitle={error}
                onCloseButtonClick={() => setError(null)}
                className="patientMergeNotification"
              />
            )}

            {/* Same Patient Warning */}
            {isSamePatientSelected() && (
              <InlineNotification
                kind="warning"
                title={intl.formatMessage({
                  id: "patient.merge.warning.samePatient",
                })}
                subtitle={intl.formatMessage({
                  id: "patient.merge.warning.samePatientDescription",
                })}
                hideCloseButton
                className="patientMergeNotification"
              />
            )}

            {/* Step Content */}
            <div className="orderLegendBody">
              {currentStep === STEP_SELECT && (
                <PatientSelectionStep
                  patient1={patient1}
                  patient2={patient2}
                  onPatient1Select={handlePatient1Select}
                  onPatient2Select={handlePatient2Select}
                />
              )}

              {currentStep === STEP_COMPARE && (
                <PrimarySelectionStep
                  patient1Details={patient1Details}
                  patient2Details={patient2Details}
                  primaryPatientId={primaryPatientId}
                  onPrimarySelect={setPrimaryPatientId}
                />
              )}

              {currentStep === STEP_CONFIRM && (
                <ConfirmationStep
                  patient1Details={patient1Details}
                  patient2Details={patient2Details}
                  primaryPatientId={primaryPatientId}
                  mergeReason={mergeReason}
                  confirmed={confirmed}
                  onReasonChange={setMergeReason}
                  onConfirmedChange={setConfirmed}
                />
              )}
            </div>

            {/* Navigation Buttons */}
            <div className="patientMergeNavigation">
              <div className="navigationLeft">
                {currentStep > STEP_SELECT && (
                  <Button
                    kind="secondary"
                    onClick={handleBack}
                    disabled={isLoading}
                  >
                    <FormattedMessage id="patient.merge.back" />
                  </Button>
                )}
              </div>
              <div className="navigationRight">
                <Button
                  kind="ghost"
                  onClick={handleCancel}
                  disabled={isLoading}
                >
                  <FormattedMessage id="patient.merge.cancelButton" />
                </Button>

                {currentStep < STEP_CONFIRM && (
                  <Button
                    kind="primary"
                    onClick={handleNext}
                    disabled={isNextDisabled()}
                  >
                    <FormattedMessage id="patient.merge.nextStep" />
                  </Button>
                )}

                {currentStep === STEP_CONFIRM && (
                  <Button
                    kind="danger"
                    onClick={executeMerge}
                    disabled={isConfirmDisabled()}
                  >
                    <FormattedMessage id="patient.merge.confirmButton" />
                  </Button>
                )}
              </div>
            </div>
          </div>
        </div>
      </Stack>
    </>
  );
}

export default injectIntl(PatientMerge);
