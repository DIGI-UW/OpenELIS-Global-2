import React, { useState, useCallback } from "react";
import {
  Modal,
  TextArea,
  Select,
  SelectItem,
  DatePicker,
  DatePickerInput,
  Grid,
  Column,
  InlineNotification,
  Checkbox,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";

/**
 * Decontamination Method Options for TB sample processing.
 */
const DECONTAMINATION_METHODS = [
  { id: "NALC_NAOH", text: "NALC-NaOH (Standard)" },
  { id: "NAOH_ONLY", text: "NaOH Only" },
  { id: "OTHER", text: "Other" },
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

  const [formData, setFormData] = useState({
    decontaminationMethod: "NALC_NAOH",
    methodNotes: "",
    processingDate: new Date().toISOString().split("T")[0],
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
    return true;
  }, [formData, intl]);

  const handleSave = useCallback(() => {
    if (!validateForm()) return;

    const dataToSave = {
      method: formData.decontaminationMethod,
      methodNotes:
        formData.decontaminationMethod === "OTHER" ? formData.methodNotes : "",
      processingDate: formData.processingDate,
      markReadyForInoculation: formData.markReadyForInoculation,
    };

    onSave(dataToSave);
  }, [formData, validateForm, onSave]);

  const handleClose = useCallback(() => {
    setError(null);
    setFormData({
      decontaminationMethod: "NALC_NAOH",
      methodNotes: "",
      processingDate: new Date().toISOString().split("T")[0],
      markReadyForInoculation: false,
    });
    onClose();
  }, [onClose]);

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
            <DatePicker
              datePickerType="single"
              value={formData.processingDate}
              onChange={([date]) =>
                handleInputChange(
                  "processingDate",
                  date?.toISOString().split("T")[0] || "",
                )
              }
            >
              <DatePickerInput
                id="processingDate"
                labelText={intl.formatMessage({
                  id: "notebook.tb.sampleProc.date",
                  defaultMessage: "Processing Date *",
                })}
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
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
