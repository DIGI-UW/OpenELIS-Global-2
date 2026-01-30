import React, { useState, useCallback, useContext } from "react";
import {
  Modal,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  Grid,
  Column,
  InlineNotification,
  Button,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import CustomDatePicker from "../../../../common/CustomDatePicker";
import { ConfigurationContext } from "../../../../layout/Layout";
import {
  getFromOpenElisServer,
  convertToISODate,
} from "../../../../utils/Utils";

/**
 * Media Type Options for TB culture.
 */
const MEDIA_TYPES = [
  { id: "LJ", text: "LJ (Lowenstein-Jensen)" },
  { id: "MGIT", text: "MGIT (Liquid Culture)" },
];

/**
 * QC Status Options.
 */
const QC_STATUS_OPTIONS = [
  { id: "PENDING", text: "Pending" },
  { id: "PASSED", text: "Passed" },
  { id: "FAILED", text: "Failed" },
];

/**
 * MediaPreparationModal - Modal for creating/editing media batches.
 *
 * @param {boolean} open - Whether modal is open
 * @param {function} onClose - Close handler
 * @param {function} onSave - Save handler (receives batch data)
 * @param {object} initialData - Initial data for editing (optional)
 */
function MediaPreparationModal({ open, onClose, onSave, initialData = null }) {
  const intl = useIntl();
  const { configurationProperties } = useContext(ConfigurationContext);

  const [formData, setFormData] = useState({
    batchId: initialData?.batchId || "",
    mediaType: initialData?.mediaType || "LJ",
    preparationDate:
      initialData?.preparationDate ||
      configurationProperties?.currentDateAsText ||
      "",
    expiryDate: initialData?.expiryDate || "",
    tubesCount: initialData?.tubesCount || "",
    qcStatus: initialData?.qcStatus || "PENDING",
    qcNotes: initialData?.qcNotes || "",
  });

  const [error, setError] = useState(null);
  const [generating, setGenerating] = useState(false);

  const handleInputChange = useCallback((field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    setError(null);
  }, []);

  const handleGenerateBatchId = useCallback(() => {
    if (!formData.mediaType) {
      setError("Please select media type first");
      return;
    }

    setGenerating(true);
    getFromOpenElisServer(
      `/rest/tb/processing/media/generate-batch-id/${formData.mediaType}`,
      (response) => {
        setGenerating(false);
        if (response && response.batchId) {
          setFormData((prev) => ({ ...prev, batchId: response.batchId }));
        } else {
          setError("Failed to generate batch ID");
        }
      },
    );
  }, [formData.mediaType]);

  const validateForm = useCallback(() => {
    if (!formData.batchId) {
      setError(
        intl.formatMessage({
          id: "notebook.tb.mediaPrep.batchIdRequired",
          defaultMessage: "Batch ID is required",
        }),
      );
      return false;
    }
    if (!formData.mediaType) {
      setError(
        intl.formatMessage({
          id: "notebook.tb.mediaPrep.mediaTypeRequired",
          defaultMessage: "Media type is required",
        }),
      );
      return false;
    }
    if (!formData.preparationDate) {
      setError(
        intl.formatMessage({
          id: "notebook.tb.mediaPrep.prepDateRequired",
          defaultMessage: "Preparation date is required",
        }),
      );
      return false;
    }
    return true;
  }, [formData, intl]);

  const handleSave = useCallback(() => {
    if (!validateForm()) return;

    const dataToSave = {
      ...formData,
      preparationDate: convertToISODate(formData.preparationDate),
      expiryDate: convertToISODate(formData.expiryDate),
      tubesCount: formData.tubesCount
        ? parseInt(formData.tubesCount, 10)
        : null,
    };

    onSave(dataToSave);
  }, [formData, validateForm, onSave]);

  const handleClose = useCallback(() => {
    setError(null);
    setFormData({
      batchId: "",
      mediaType: "LJ",
      preparationDate: configurationProperties?.currentDateAsText || "",
      expiryDate: "",
      tubesCount: "",
      qcStatus: "PENDING",
      qcNotes: "",
    });
    onClose();
  }, [onClose, configurationProperties]);

  return (
    <Modal
      open={open}
      modalHeading={intl.formatMessage({
        id: "notebook.tb.mediaPrep.title",
        defaultMessage: "Media Preparation",
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
            id="notebook.tb.mediaPrep.description"
            defaultMessage="Create a new media batch for TB culture inoculation."
          />
        </p>

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
          {/* Media Type */}
          <Column lg={8} md={4} sm={4}>
            <Select
              id="mediaType"
              labelText={intl.formatMessage({
                id: "notebook.tb.mediaPrep.mediaType",
                defaultMessage: "Media Type *",
              })}
              value={formData.mediaType}
              onChange={(e) => handleInputChange("mediaType", e.target.value)}
            >
              {MEDIA_TYPES.map((type) => (
                <SelectItem key={type.id} value={type.id} text={type.text} />
              ))}
            </Select>
          </Column>

          {/* Batch ID with auto-generate */}
          <Column lg={8} md={4} sm={4}>
            <div
              style={{ display: "flex", alignItems: "flex-end", gap: "8px" }}
            >
              <TextInput
                id="batchId"
                labelText={intl.formatMessage({
                  id: "notebook.tb.mediaPrep.batchId",
                  defaultMessage: "Batch ID *",
                })}
                value={formData.batchId}
                onChange={(e) => handleInputChange("batchId", e.target.value)}
                placeholder="e.g., LJ-20251224-001"
                style={{ flex: 1 }}
              />
              <Button
                kind="tertiary"
                size="sm"
                onClick={handleGenerateBatchId}
                disabled={generating}
              >
                {generating ? "..." : "Generate"}
              </Button>
            </div>
          </Column>

          {/* Preparation Date */}
          <Column lg={8} md={4} sm={4}>
            <CustomDatePicker
              id="preparationDate"
              labelText={intl.formatMessage({
                id: "notebook.tb.mediaPrep.prepDate",
                defaultMessage: "Preparation Date *",
              })}
              value={formData.preparationDate}
              onChange={(date) => handleInputChange("preparationDate", date)}
            />
          </Column>

          {/* Expiry Date */}
          <Column lg={8} md={4} sm={4}>
            <CustomDatePicker
              id="expiryDate"
              labelText={intl.formatMessage({
                id: "notebook.tb.mediaPrep.expiryDate",
                defaultMessage: "Expiry Date",
              })}
              value={formData.expiryDate}
              onChange={(date) => handleInputChange("expiryDate", date)}
            />
          </Column>

          {/* Tubes Count */}
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="tubesCount"
              type="number"
              labelText={intl.formatMessage({
                id: "notebook.tb.mediaPrep.tubesCount",
                defaultMessage: "Number of Tubes/Slants",
              })}
              value={formData.tubesCount}
              onChange={(e) => handleInputChange("tubesCount", e.target.value)}
              placeholder="e.g., 24"
            />
          </Column>

          {/* QC Status */}
          <Column lg={8} md={4} sm={4}>
            <Select
              id="qcStatus"
              labelText={intl.formatMessage({
                id: "notebook.tb.mediaPrep.qcStatus",
                defaultMessage: "QC Status",
              })}
              value={formData.qcStatus}
              onChange={(e) => handleInputChange("qcStatus", e.target.value)}
            >
              {QC_STATUS_OPTIONS.map((status) => (
                <SelectItem
                  key={status.id}
                  value={status.id}
                  text={status.text}
                />
              ))}
            </Select>
          </Column>

          {/* QC Notes */}
          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="qcNotes"
              labelText={intl.formatMessage({
                id: "notebook.tb.mediaPrep.qcNotes",
                defaultMessage: "QC Notes",
              })}
              value={formData.qcNotes}
              onChange={(e) => handleInputChange("qcNotes", e.target.value)}
              rows={2}
              placeholder={intl.formatMessage({
                id: "notebook.tb.mediaPrep.qcNotesPlaceholder",
                defaultMessage:
                  "Any notes about media quality or preparation...",
              })}
            />
          </Column>
        </Grid>
      </div>
    </Modal>
  );
}

export default MediaPreparationModal;
