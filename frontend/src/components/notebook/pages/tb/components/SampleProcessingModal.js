import React, { useState, useCallback, useContext } from "react";
import {
  Modal,
  TextArea,
  Select,
  SelectItem,
  Grid,
  Column,
  InlineNotification,
  Checkbox,
  TextInput,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import CustomDatePicker from "../../../../common/CustomDatePicker";
import { ConfigurationContext } from "../../../../layout/Layout";
import { convertToISODate } from "../../../../utils/Utils";

/**
 * Decontamination Method Options for TB sample processing.
 */
const DECONTAMINATION_METHODS = [
  { id: "NALC_NAOH", text: "NALC-NaOH (Standard)" },
  { id: "NAOH_ONLY", text: "NaOH Only" },
  { id: "OTHER", text: "Other" },
];

/**
 * Type of Assay Options - defines what test the sample is being prepared for.
 * Per SRS FR-016: "Type of assay documented"
 */
const ASSAY_TYPES = [
  { id: "CULTURE", text: "Culture (LJ/MGIT)" },
  { id: "SMEAR_MICROSCOPY", text: "Smear Microscopy (AFB)" },
  { id: "GENEXPERT", text: "GeneXpert (MTB/RIF)" },
  { id: "DST_1ST_LINE", text: "DST - 1st Line" },
  { id: "DST_2ND_LINE", text: "DST - 2nd Line" },
  { id: "MULTIPLE", text: "Multiple Assays" },
];

/**
 * SampleProcessingModal - Modal for recording sample decontamination.
 *
 * @param {boolean} open - Whether modal is open
 * @param {function} onClose - Close handler
 * @param {function} onSave - Save handler (receives processing data)
 * @param {number} selectedCount - Number of samples selected
 * @param {Array} selectedSamples - Selected sample data
 */
function SampleProcessingModal({
  open,
  onClose,
  onSave,
  selectedCount = 0,
  selectedSamples = [],
}) {
  const intl = useIntl();
  const { configurationProperties } = useContext(ConfigurationContext);

  const [formData, setFormData] = useState({
    decontaminationMethod: "NALC_NAOH",
    methodNotes: "",
    processingDate: configurationProperties?.currentDateAsText || "",
    technicianInitials: "", // SRS FR-016: "technician initials recorded"
    assayType: "", // SRS FR-016: "Type of assay documented"
    equipmentUsed: "", // SRS FR-016: "Equipment used logged"
    markReadyForInoculation: false,
  });

  const [error, setError] = useState(null);

  const handleInputChange = useCallback((field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    setError(null);
  }, []);

  const validateForm = useCallback(() => {
    if (!formData.decontaminationMethod) {
      setError(
        intl.formatMessage({
          id: "notebook.tb.sampleProc.methodRequired",
          defaultMessage: "Decontamination method is required",
        }),
      );
      return false;
    }
    if (!formData.processingDate) {
      setError(
        intl.formatMessage({
          id: "notebook.tb.sampleProc.dateRequired",
          defaultMessage: "Processing date is required",
        }),
      );
      return false;
    }
    if (
      !formData.technicianInitials ||
      formData.technicianInitials.trim() === ""
    ) {
      setError(
        intl.formatMessage({
          id: "notebook.tb.sampleProc.technicianRequired",
          defaultMessage: "Technician initials are required",
        }),
      );
      return false;
    }
    if (!formData.assayType) {
      setError(
        intl.formatMessage({
          id: "notebook.tb.sampleProc.assayTypeRequired",
          defaultMessage: "Type of assay is required",
        }),
      );
      return false;
    }
    if (!formData.equipmentUsed || formData.equipmentUsed.trim() === "") {
      setError(
        intl.formatMessage({
          id: "notebook.tb.sampleProc.equipmentRequired",
          defaultMessage: "Equipment used is required",
        }),
      );
      return false;
    }
    return true;
  }, [formData, intl]);

  const handleSave = useCallback(() => {
    if (!validateForm()) return;

    const dataToSave = {
      method: formData.decontaminationMethod,
      methodNotes:
        formData.decontaminationMethod === "OTHER" ? formData.methodNotes : "",
      processingDate: convertToISODate(formData.processingDate),
      technicianInitials: formData.technicianInitials, // SRS FR-016
      assayType: formData.assayType, // SRS FR-016
      equipmentUsed: formData.equipmentUsed, // SRS FR-016
      markReadyForInoculation: formData.markReadyForInoculation,
    };

    onSave(dataToSave);
  }, [formData, validateForm, onSave]);

  const handleClose = useCallback(() => {
    setError(null);
    setFormData({
      decontaminationMethod: "NALC_NAOH",
      methodNotes: "",
      processingDate: configurationProperties?.currentDateAsText || "",
      technicianInitials: "",
      assayType: "",
      equipmentUsed: "",
      markReadyForInoculation: false,
    });
    onClose();
  }, [onClose, configurationProperties]);

  return (
    <Modal
      open={open}
      modalHeading={intl.formatMessage({
        id: "notebook.tb.sampleProc.title",
        defaultMessage: "Sample Processing (Decontamination)",
      })}
      primaryButtonText={intl.formatMessage({
        id: "label.save",
        defaultMessage: "Save",
      })}
      secondaryButtonText={intl.formatMessage({
        id: "label.cancel",
        defaultMessage: "Cancel",
      })}
      onRequestClose={handleClose}
      onRequestSubmit={handleSave}
      size="md"
    >
      <div style={{ marginBottom: "1rem" }}>
        <p style={{ color: "#525252", marginBottom: "1rem" }}>
          <FormattedMessage
            id="notebook.tb.sampleProc.description"
            defaultMessage="Record decontamination processing for {count} selected sample(s). All samples will be processed with the same method."
            values={{ count: selectedCount }}
          />
        </p>

        {/* Selected samples summary */}
        {selectedSamples.length > 0 && selectedSamples.length <= 5 && (
          <div
            style={{
              backgroundColor: "#f4f4f4",
              padding: "0.75rem",
              borderRadius: "4px",
              marginBottom: "1rem",
            }}
          >
            <strong>
              <FormattedMessage
                id="notebook.tb.sampleProc.selectedSamples"
                defaultMessage="Selected Samples:"
              />
            </strong>
            <ul style={{ margin: "0.5rem 0 0 1rem", padding: 0 }}>
              {selectedSamples.map((sample) => (
                <li key={sample.id} style={{ fontSize: "0.875rem" }}>
                  {sample.accessionNumber || sample.id}
                </li>
              ))}
            </ul>
          </div>
        )}

        {error && (
          <InlineNotification
            kind="error"
            title={error}
            hideCloseButton
            lowContrast
            style={{ marginBottom: "1rem" }}
          />
        )}

        <Grid fullWidth>
          {/* Decontamination Method */}
          <Column lg={8} md={4} sm={4}>
            <Select
              id="decontaminationMethod"
              labelText={intl.formatMessage({
                id: "notebook.tb.sampleProc.method",
                defaultMessage: "Decontamination Method *",
              })}
              value={formData.decontaminationMethod}
              onChange={(e) =>
                handleInputChange("decontaminationMethod", e.target.value)
              }
            >
              {DECONTAMINATION_METHODS.map((method) => (
                <SelectItem
                  key={method.id}
                  value={method.id}
                  text={method.text}
                />
              ))}
            </Select>
          </Column>

          {/* Processing Date */}
          <Column lg={8} md={4} sm={4}>
            <CustomDatePicker
              id="processingDate"
              labelText={intl.formatMessage({
                id: "notebook.tb.sampleProc.date",
                defaultMessage: "Processing Date *",
              })}
              value={formData.processingDate}
              onChange={(date) => handleInputChange("processingDate", date)}
            />
          </Column>

          {/* Technician Initials - SRS FR-016 */}
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="technicianInitials"
              labelText={intl.formatMessage({
                id: "notebook.tb.sampleProc.technicianInitials",
                defaultMessage: "Technician Initials *",
              })}
              value={formData.technicianInitials}
              onChange={(e) =>
                handleInputChange("technicianInitials", e.target.value)
              }
              placeholder={intl.formatMessage({
                id: "notebook.tb.sampleProc.technicianPlaceholder",
                defaultMessage: "e.g., JD, ABC",
              })}
              maxLength={10}
            />
          </Column>

          {/* Type of Assay - SRS FR-016 */}
          <Column lg={8} md={4} sm={4}>
            <Select
              id="assayType"
              labelText={intl.formatMessage({
                id: "notebook.tb.sampleProc.assayType",
                defaultMessage: "Type of Assay *",
              })}
              value={formData.assayType}
              onChange={(e) => handleInputChange("assayType", e.target.value)}
            >
              <SelectItem value="" text="Select assay type..." />
              {ASSAY_TYPES.map((assay) => (
                <SelectItem key={assay.id} value={assay.id} text={assay.text} />
              ))}
            </Select>
          </Column>

          {/* Equipment Used - SRS FR-016 */}
          <Column lg={16} md={8} sm={4}>
            <TextInput
              id="equipmentUsed"
              labelText={intl.formatMessage({
                id: "notebook.tb.sampleProc.equipmentUsed",
                defaultMessage: "Equipment Used *",
              })}
              value={formData.equipmentUsed}
              onChange={(e) =>
                handleInputChange("equipmentUsed", e.target.value)
              }
              placeholder={intl.formatMessage({
                id: "notebook.tb.sampleProc.equipmentPlaceholder",
                defaultMessage: "e.g., BSC-01, Centrifuge-02, Incubator-A",
              })}
            />
          </Column>

          {/* Method Notes (for OTHER) */}
          {formData.decontaminationMethod === "OTHER" && (
            <Column lg={16} md={8} sm={4}>
              <TextArea
                id="methodNotes"
                labelText={intl.formatMessage({
                  id: "notebook.tb.sampleProc.methodNotes",
                  defaultMessage: "Method Details *",
                })}
                value={formData.methodNotes}
                onChange={(e) =>
                  handleInputChange("methodNotes", e.target.value)
                }
                rows={2}
                placeholder={intl.formatMessage({
                  id: "notebook.tb.sampleProc.methodNotesPlaceholder",
                  defaultMessage: "Describe the decontamination method used...",
                })}
              />
            </Column>
          )}

          {/* Ready for Inoculation Checkbox */}
          <Column lg={16} md={8} sm={4}>
            <div
              style={{
                marginTop: "1rem",
                padding: "1rem",
                backgroundColor: formData.markReadyForInoculation
                  ? "#defbe6"
                  : "#f4f4f4",
                borderRadius: "4px",
                borderLeft: `4px solid ${formData.markReadyForInoculation ? "#24a148" : "#8d8d8d"}`,
              }}
            >
              <Checkbox
                id="markReadyForInoculation"
                labelText={
                  <span style={{ fontWeight: 600 }}>
                    <FormattedMessage
                      id="notebook.tb.sampleProc.markReady"
                      defaultMessage="Mark as Ready for Inoculation"
                    />
                  </span>
                }
                checked={formData.markReadyForInoculation}
                onChange={(e, { checked }) =>
                  handleInputChange("markReadyForInoculation", checked)
                }
              />
              <p
                style={{
                  marginTop: "0.5rem",
                  marginLeft: "1.75rem",
                  fontSize: "0.875rem",
                  color: "#525252",
                }}
              >
                <FormattedMessage
                  id="notebook.tb.sampleProc.markReadyHelp"
                  defaultMessage="Check this if samples are ready to be inoculated to culture media."
                />
              </p>
            </div>
          </Column>
        </Grid>
      </div>
    </Modal>
  );
}

export default SampleProcessingModal;
