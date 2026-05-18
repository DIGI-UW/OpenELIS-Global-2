import React, { useState, useCallback, useEffect } from "react";
import {
  Modal,
  TextInput,
  NumberInput,
  Select,
  SelectItem,
  Checkbox,
  DatePicker,
  DatePickerInput,
  TimePicker,
  Grid,
  Column,
  InlineNotification,
  Loading,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../../config.json";
import "./NotebookWorkflow.css";

const INITIAL_FORM_STATE = {
  // A. Sample Identity
  sampleId: "",
  // B. Specimen Information
  specimenType: "",
  specimenQuality: "",
  numOfSamples: 1,
  // C. Request Paper Details
  documentNumber: "",
  referringFacility: "",
  // D. Patient / Participant Metadata
  patientName: "",
  patientAge: "",
  patientSex: "",
  patientId: "",
  studyId: "",
  patientAddress: "",
  patientPhone: "",
  physicianPhone: "",
  consentStatus: "",
  // E. Clinical Context
  treatmentHistory: "",
  // F. Requested Tests
  culture: false,
  smearMicroscopy: false,
  genexpert: false,
  identification: false,
  dstFirstLine: false,
  dstSecondLine: false,
  intendedMethod: "",
  // G. Receipt Details
  receivedSite: "",
  receivedDate: "",
  receivedTime: "",
};

/**
 * TBIndividualSampleRegistrationModal - Form modal for registering a single
 * TB sample with all manifest fields (FR-014).
 */
function TBIndividualSampleRegistrationModal({
  open,
  onClose,
  entryId,
  onRegisterSuccess,
}) {
  const intl = useIntl();

  const [formData, setFormData] = useState({ ...INITIAL_FORM_STATE });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [validationErrors, setValidationErrors] = useState({});
  const [specimenTypes, setSpecimenTypes] = useState([]);

  useEffect(() => {
    if (open) {
      fetch(`${config.serverBaseUrl}/rest/notebook/tb/sample-types`, {
        credentials: "include",
        headers: { "X-CSRF-Token": localStorage.getItem("CSRF") },
      })
        .then((res) => res.json())
        .then((data) => {
          if (data.sampleTypes) {
            setSpecimenTypes(data.sampleTypes);
          }
        })
        .catch(() => {});
    }
  }, [open]);

  const handleFieldChange = useCallback((field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    setValidationErrors((prev) => ({ ...prev, [field]: undefined }));
  }, []);

  const handleCheckboxChange = useCallback((field, checked) => {
    setFormData((prev) => ({ ...prev, [field]: checked }));
  }, []);

  const validate = useCallback(() => {
    const errors = {};
    if (!formData.specimenType.trim()) {
      errors.specimenType = intl.formatMessage({
        id: "notebook.tb.register.error.specimenTypeRequired",
        defaultMessage: "Specimen type is required",
      });
    }
    if (formData.numOfSamples < 1) {
      errors.numOfSamples = intl.formatMessage({
        id: "notebook.tb.register.error.numOfSamplesMin",
        defaultMessage: "Number of samples must be at least 1",
      });
    }
    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  }, [formData, intl]);

  const handleSubmit = useCallback(async () => {
    if (!validate()) return;

    setIsSubmitting(true);
    setError(null);

    try {
      const endpoint = `${config.serverBaseUrl}/rest/notebook/tb/entry/${entryId}/samples/register`;
      const response = await fetch(endpoint, {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
        body: JSON.stringify(formData),
      });

      const data = await response.json();

      if (response.ok && data.success) {
        if (onRegisterSuccess) {
          onRegisterSuccess(data);
        }
        handleClose();
      } else {
        setError(
          data.error ||
            intl.formatMessage({
              id: "notebook.tb.register.error.failed",
              defaultMessage: "Failed to register sample. Please try again.",
            }),
        );
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setIsSubmitting(false);
    }
  }, [formData, entryId, validate, onRegisterSuccess, intl]);

  const handleClose = useCallback(() => {
    setFormData({ ...INITIAL_FORM_STATE });
    setError(null);
    setValidationErrors({});
    onClose();
  }, [onClose]);

  return (
    <Modal
      open={open}
      onRequestClose={handleClose}
      modalHeading={intl.formatMessage({
        id: "notebook.tb.register.title",
        defaultMessage: "Register Individual TB Sample",
      })}
      primaryButtonText={
        isSubmitting
          ? intl.formatMessage({
              id: "notebook.tb.register.submitting",
              defaultMessage: "Registering...",
            })
          : intl.formatMessage({
              id: "notebook.tb.register.submit",
              defaultMessage: "Register Sample",
            })
      }
      secondaryButtonText={intl.formatMessage({
        id: "label.button.cancel",
        defaultMessage: "Cancel",
      })}
      onRequestSubmit={handleSubmit}
      onSecondarySubmit={handleClose}
      primaryButtonDisabled={isSubmitting}
      size="lg"
    >
      <div className="tb-individual-registration-form">
        {isSubmitting && <Loading withOverlay description="Registering..." />}

        {error && (
          <InlineNotification
            kind="error"
            title={error}
            lowContrast
            hideCloseButton
            style={{ marginBottom: "1rem" }}
          />
        )}

        {/* B. Specimen Information (Required) */}
        <div className="form-section">
          <h5 className="form-section-title">
            <FormattedMessage
              id="notebook.tb.register.section.specimen"
              defaultMessage="Specimen Information"
            />
            <span className="required-indicator">*</span>
          </h5>
          <Grid condensed>
            <Column lg={8} md={4} sm={4}>
              <Select
                id="specimenType"
                labelText={intl.formatMessage({
                  id: "notebook.tb.register.specimenType",
                  defaultMessage: "Specimen Type",
                })}
                value={formData.specimenType}
                onChange={(e) =>
                  handleFieldChange("specimenType", e.target.value)
                }
                invalid={!!validationErrors.specimenType}
                invalidText={validationErrors.specimenType}
              >
                <SelectItem value="" text="" />
                {specimenTypes.map((type) => (
                  <SelectItem
                    key={type.id}
                    value={type.description}
                    text={type.description}
                  />
                ))}
              </Select>
            </Column>
            <Column lg={4} md={2} sm={2}>
              <Select
                id="specimenQuality"
                labelText={intl.formatMessage({
                  id: "notebook.tb.register.specimenQuality",
                  defaultMessage: "Specimen Quality",
                })}
                value={formData.specimenQuality}
                onChange={(e) =>
                  handleFieldChange("specimenQuality", e.target.value)
                }
              >
                <SelectItem value="" text="" />
                <SelectItem value="Good" text="Good" />
                <SelectItem value="Fair" text="Fair" />
                <SelectItem value="Poor" text="Poor" />
              </Select>
            </Column>
            <Column lg={4} md={2} sm={2}>
              <NumberInput
                id="numOfSamples"
                label={intl.formatMessage({
                  id: "notebook.tb.register.numOfSamples",
                  defaultMessage: "Number of Samples",
                })}
                value={formData.numOfSamples}
                min={1}
                max={50}
                onChange={(e, { value }) =>
                  handleFieldChange("numOfSamples", value)
                }
                invalid={!!validationErrors.numOfSamples}
                invalidText={validationErrors.numOfSamples}
              />
            </Column>
          </Grid>
        </div>

        {/* A. Sample Identity */}
        <div className="form-section">
          <h5 className="form-section-title">
            <FormattedMessage
              id="notebook.tb.register.section.identity"
              defaultMessage="Sample Identity"
            />
          </h5>
          <Grid condensed>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="sampleId"
                labelText={intl.formatMessage({
                  id: "notebook.tb.register.sampleId",
                  defaultMessage: "Sample ID",
                })}
                value={formData.sampleId}
                onChange={(e) => handleFieldChange("sampleId", e.target.value)}
                placeholder="TB-2024-001"
              />
            </Column>
          </Grid>
        </div>

        {/* C. Request Paper Details */}
        <div className="form-section">
          <h5 className="form-section-title">
            <FormattedMessage
              id="notebook.tb.register.section.request"
              defaultMessage="Request Paper Details"
            />
          </h5>
          <Grid condensed>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="documentNumber"
                labelText={intl.formatMessage({
                  id: "notebook.tb.register.documentNumber",
                  defaultMessage: "Document Number",
                })}
                value={formData.documentNumber}
                onChange={(e) =>
                  handleFieldChange("documentNumber", e.target.value)
                }
                placeholder="REQ-2024-001"
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="referringFacility"
                labelText={intl.formatMessage({
                  id: "notebook.tb.register.referringFacility",
                  defaultMessage: "Referring Facility",
                })}
                value={formData.referringFacility}
                onChange={(e) =>
                  handleFieldChange("referringFacility", e.target.value)
                }
              />
            </Column>
          </Grid>
        </div>

        {/* D. Patient / Participant Metadata */}
        <div className="form-section">
          <h5 className="form-section-title">
            <FormattedMessage
              id="notebook.tb.register.section.patient"
              defaultMessage="Patient / Participant Metadata"
            />
          </h5>
          <Grid condensed>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="patientName"
                labelText={intl.formatMessage({
                  id: "notebook.tb.register.patientName",
                  defaultMessage: "Patient Name",
                })}
                value={formData.patientName}
                onChange={(e) =>
                  handleFieldChange("patientName", e.target.value)
                }
              />
            </Column>
            <Column lg={4} md={2} sm={2}>
              <TextInput
                id="patientAge"
                labelText={intl.formatMessage({
                  id: "notebook.tb.register.patientAge",
                  defaultMessage: "Age",
                })}
                value={formData.patientAge}
                onChange={(e) =>
                  handleFieldChange("patientAge", e.target.value)
                }
                placeholder="45"
              />
            </Column>
            <Column lg={4} md={2} sm={2}>
              <Select
                id="patientSex"
                labelText={intl.formatMessage({
                  id: "notebook.tb.register.patientSex",
                  defaultMessage: "Sex",
                })}
                value={formData.patientSex}
                onChange={(e) =>
                  handleFieldChange("patientSex", e.target.value)
                }
              >
                <SelectItem value="" text="" />
                <SelectItem value="M" text="M" />
                <SelectItem value="F" text="F" />
              </Select>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="patientId"
                labelText={intl.formatMessage({
                  id: "notebook.tb.register.patientId",
                  defaultMessage: "Patient ID",
                })}
                value={formData.patientId}
                onChange={(e) => handleFieldChange("patientId", e.target.value)}
                placeholder="PAT-12345"
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="studyId"
                labelText={intl.formatMessage({
                  id: "notebook.tb.register.studyId",
                  defaultMessage: "Study ID",
                })}
                value={formData.studyId}
                onChange={(e) => handleFieldChange("studyId", e.target.value)}
              />
            </Column>
            <Column lg={16} md={8} sm={4}>
              <TextInput
                id="patientAddress"
                labelText={intl.formatMessage({
                  id: "notebook.tb.register.patientAddress",
                  defaultMessage: "Patient Address",
                })}
                value={formData.patientAddress}
                onChange={(e) =>
                  handleFieldChange("patientAddress", e.target.value)
                }
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="patientPhone"
                labelText={intl.formatMessage({
                  id: "notebook.tb.register.patientPhone",
                  defaultMessage: "Patient Phone",
                })}
                value={formData.patientPhone}
                onChange={(e) =>
                  handleFieldChange("patientPhone", e.target.value)
                }
                placeholder="+251-911-123456"
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="physicianPhone"
                labelText={intl.formatMessage({
                  id: "notebook.tb.register.physicianPhone",
                  defaultMessage: "Physician Phone",
                })}
                value={formData.physicianPhone}
                onChange={(e) =>
                  handleFieldChange("physicianPhone", e.target.value)
                }
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <Select
                id="consentStatus"
                labelText={intl.formatMessage({
                  id: "notebook.tb.register.consentStatus",
                  defaultMessage: "Consent Status",
                })}
                value={formData.consentStatus}
                onChange={(e) =>
                  handleFieldChange("consentStatus", e.target.value)
                }
              >
                <SelectItem value="" text="" />
                <SelectItem value="Yes" text="Yes" />
                <SelectItem value="No" text="No" />
              </Select>
            </Column>
          </Grid>
        </div>

        {/* E. Clinical Context */}
        <div className="form-section">
          <h5 className="form-section-title">
            <FormattedMessage
              id="notebook.tb.register.section.clinical"
              defaultMessage="Clinical Context"
            />
          </h5>
          <Grid condensed>
            <Column lg={8} md={4} sm={4}>
              <Select
                id="treatmentHistory"
                labelText={intl.formatMessage({
                  id: "notebook.tb.register.treatmentHistory",
                  defaultMessage: "Treatment History",
                })}
                value={formData.treatmentHistory}
                onChange={(e) =>
                  handleFieldChange("treatmentHistory", e.target.value)
                }
              >
                <SelectItem value="" text="" />
                <SelectItem value="New" text="New" />
                <SelectItem value="Retreatment" text="Retreatment" />
                <SelectItem value="MDR-TB" text="MDR-TB" />
                <SelectItem value="XDR" text="XDR" />
                <SelectItem value="Relapse" text="Relapse" />
              </Select>
            </Column>
          </Grid>
        </div>

        {/* F. Requested Tests */}
        <div className="form-section">
          <h5 className="form-section-title">
            <FormattedMessage
              id="notebook.tb.register.section.tests"
              defaultMessage="Requested Tests"
            />
          </h5>
          <Grid condensed>
            <Column lg={5} md={4} sm={4}>
              <Checkbox
                id="culture"
                labelText={intl.formatMessage({
                  id: "notebook.tb.register.culture",
                  defaultMessage: "Culture",
                })}
                checked={formData.culture}
                onChange={(_, { checked }) =>
                  handleCheckboxChange("culture", checked)
                }
              />
            </Column>
            <Column lg={5} md={4} sm={4}>
              <Checkbox
                id="smearMicroscopy"
                labelText={intl.formatMessage({
                  id: "notebook.tb.register.smearMicroscopy",
                  defaultMessage: "Smear Microscopy (AFB)",
                })}
                checked={formData.smearMicroscopy}
                onChange={(_, { checked }) =>
                  handleCheckboxChange("smearMicroscopy", checked)
                }
              />
            </Column>
            <Column lg={6} md={4} sm={4}>
              <Checkbox
                id="genexpert"
                labelText={intl.formatMessage({
                  id: "notebook.tb.register.genexpert",
                  defaultMessage: "GeneXpert (MTB/RIF)",
                })}
                checked={formData.genexpert}
                onChange={(_, { checked }) =>
                  handleCheckboxChange("genexpert", checked)
                }
              />
            </Column>
            <Column lg={5} md={4} sm={4}>
              <Checkbox
                id="identification"
                labelText={intl.formatMessage({
                  id: "notebook.tb.register.identification",
                  defaultMessage: "Identification",
                })}
                checked={formData.identification}
                onChange={(_, { checked }) =>
                  handleCheckboxChange("identification", checked)
                }
              />
            </Column>
            <Column lg={5} md={4} sm={4}>
              <Checkbox
                id="dstFirstLine"
                labelText={intl.formatMessage({
                  id: "notebook.tb.register.dstFirstLine",
                  defaultMessage: "DST First Line",
                })}
                checked={formData.dstFirstLine}
                onChange={(_, { checked }) =>
                  handleCheckboxChange("dstFirstLine", checked)
                }
              />
            </Column>
            <Column lg={6} md={4} sm={4}>
              <Checkbox
                id="dstSecondLine"
                labelText={intl.formatMessage({
                  id: "notebook.tb.register.dstSecondLine",
                  defaultMessage: "DST Second Line",
                })}
                checked={formData.dstSecondLine}
                onChange={(_, { checked }) =>
                  handleCheckboxChange("dstSecondLine", checked)
                }
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <Select
                id="intendedMethod"
                labelText={intl.formatMessage({
                  id: "notebook.tb.register.intendedMethod",
                  defaultMessage: "Intended Culture Method",
                })}
                value={formData.intendedMethod}
                onChange={(e) =>
                  handleFieldChange("intendedMethod", e.target.value)
                }
              >
                <SelectItem value="" text="" />
                <SelectItem value="LJ" text="LJ (Lowenstein-Jensen)" />
                <SelectItem value="MGIT" text="MGIT" />
                <SelectItem value="Both" text="Both" />
              </Select>
            </Column>
          </Grid>
        </div>

        {/* G. Receipt Details */}
        <div className="form-section">
          <h5 className="form-section-title">
            <FormattedMessage
              id="notebook.tb.register.section.receipt"
              defaultMessage="Receipt Details"
            />
          </h5>
          <Grid condensed>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="receivedSite"
                labelText={intl.formatMessage({
                  id: "notebook.tb.register.receivedSite",
                  defaultMessage: "Received Site",
                })}
                value={formData.receivedSite}
                onChange={(e) =>
                  handleFieldChange("receivedSite", e.target.value)
                }
              />
            </Column>
            <Column lg={4} md={4} sm={4}>
              <DatePicker
                datePickerType="single"
                dateFormat="Y-m-d"
                value={formData.receivedDate}
                onChange={(dates) => {
                  if (dates && dates.length > 0) {
                    const d = dates[0];
                    const formatted = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
                    handleFieldChange("receivedDate", formatted);
                  }
                }}
              >
                <DatePickerInput
                  id="receivedDate"
                  labelText={intl.formatMessage({
                    id: "notebook.tb.register.receivedDate",
                    defaultMessage: "Received Date",
                  })}
                  placeholder="yyyy-mm-dd"
                />
              </DatePicker>
            </Column>
            <Column lg={4} md={4} sm={4}>
              <TimePicker
                id="receivedTime"
                labelText={intl.formatMessage({
                  id: "notebook.tb.register.receivedTime",
                  defaultMessage: "Received Time",
                })}
                value={formData.receivedTime}
                onChange={(e) =>
                  handleFieldChange("receivedTime", e.target.value)
                }
                placeholder="HH:MM"
              />
            </Column>
          </Grid>
        </div>
      </div>
    </Modal>
  );
}

export default TBIndividualSampleRegistrationModal;
