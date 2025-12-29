import React, { useState, useCallback, useEffect } from "react";
import {
  Modal,
  Select,
  SelectItem,
  DatePicker,
  DatePickerInput,
  Grid,
  Column,
  InlineNotification,
  Tag,
  Dropdown,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../../../utils/Utils";

/**
 * Culture Method Options.
 */
const CULTURE_METHODS = [
  { id: "LJ", text: "LJ (Lowenstein-Jensen)" },
  { id: "MGIT", text: "MGIT (Liquid Culture)" },
  { id: "BOTH", text: "Both LJ and MGIT" },
];

/**
 * InoculationModal - Modal for inoculating processed samples to culture media.
 *
 * Supports both single sample and bulk inoculation modes.
 *
 * @param {boolean} open - Whether modal is open
 * @param {function} onClose - Close handler
 * @param {function} onSave - Save handler (receives inoculation data)
 * @param {object|array} sample - Sample being inoculated (single object or array for bulk)
 * @param {object} processingRecord - Sample processing record (optional)
 * @param {boolean} bulkMode - Whether this is a bulk inoculation (multiple samples)
 */
function InoculationModal({
  open,
  onClose,
  onSave,
  sample = null,
  processingRecord = null,
  bulkMode = false,
}) {
  const intl = useIntl();

  const [formData, setFormData] = useState({
    mediaBatchId: "",
    cultureMethod: "LJ",
    inoculationDate: new Date().toISOString().split("T")[0],
  });

  const [availableMedia, setAvailableMedia] = useState([]);
  const [loadingMedia, setLoadingMedia] = useState(false);
  const [error, setError] = useState(null);

  // Fetch available media batches when modal opens or culture method changes
  useEffect(() => {
    if (open) {
      fetchAvailableMedia(formData.cultureMethod);
    }
  }, [open, formData.cultureMethod]);

  const fetchAvailableMedia = useCallback((mediaType) => {
    setLoadingMedia(true);
    // For "BOTH", we need LJ media primarily
    const typeToFetch = mediaType === "BOTH" ? "LJ" : mediaType;
    getFromOpenElisServer(
      `/rest/tb/processing/media/available?mediaType=${typeToFetch}`,
      (response) => {
        if (response && Array.isArray(response)) {
          setAvailableMedia(response);
        } else {
          setAvailableMedia([]);
        }
        setLoadingMedia(false);
      },
    );
  }, []);

  const handleInputChange = useCallback((field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    setError(null);
  }, []);

  const validateForm = useCallback(() => {
    if (!formData.mediaBatchId) {
      setError(
        intl.formatMessage({
          id: "notebook.tb.inoculation.mediaBatchRequired",
          defaultMessage: "Please select a media batch",
        }),
      );
      return false;
    }
    if (!formData.inoculationDate) {
      setError(
        intl.formatMessage({
          id: "notebook.tb.inoculation.dateRequired",
          defaultMessage: "Inoculation date is required",
        }),
      );
      return false;
    }
    return true;
  }, [formData, intl]);

  const handleSave = useCallback(() => {
    if (!validateForm()) return;

    const dataToSave = {
      sampleItemId: sample?.sampleItemId || sample?.id,
      mediaBatchId: parseInt(formData.mediaBatchId, 10),
      processingId: processingRecord?.id || null,
      cultureMethod: formData.cultureMethod,
      inoculationDate: formData.inoculationDate,
    };

    onSave(dataToSave);
  }, [formData, sample, processingRecord, validateForm, onSave]);

  const handleClose = useCallback(() => {
    setError(null);
    setFormData({
      mediaBatchId: "",
      cultureMethod: "LJ",
      inoculationDate: new Date().toISOString().split("T")[0],
    });
    onClose();
  }, [onClose]);

  const selectedBatch = availableMedia.find(
    (m) => String(m.id) === String(formData.mediaBatchId),
  );

  // Determine if we're in bulk mode (array of samples)
  const isBulk = bulkMode && Array.isArray(sample);
  const sampleCount = isBulk ? sample.length : 1;
  const firstSample = isBulk ? sample[0] : sample;

  return (
    <Modal
      open={open}
      modalHeading={
        isBulk
          ? intl.formatMessage(
              {
                id: "notebook.tb.inoculation.bulkTitle",
                defaultMessage: "Bulk Inoculate {count} Samples",
              },
              { count: sampleCount },
            )
          : intl.formatMessage({
              id: "notebook.tb.inoculation.title",
              defaultMessage: "Inoculate Sample to Culture Media",
            })
      }
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
        {/* Bulk Mode Info */}
        {isBulk && (
          <div
            style={{
              backgroundColor: "#d0e2ff",
              padding: "1rem",
              borderRadius: "4px",
              marginBottom: "1rem",
              borderLeft: "4px solid #0f62fe",
            }}
          >
            <strong>
              <FormattedMessage
                id="notebook.tb.inoculation.bulkInfo"
                defaultMessage="Bulk Inoculation:"
              />
            </strong>{" "}
            <FormattedMessage
              id="notebook.tb.inoculation.bulkDescription"
              defaultMessage="{count} samples will be inoculated with the same settings."
              values={{ count: sampleCount }}
            />
            <div style={{ marginTop: "0.5rem", fontSize: "0.875rem" }}>
              <strong>
                <FormattedMessage
                  id="notebook.tb.inoculation.samples"
                  defaultMessage="Samples:"
                />
              </strong>{" "}
              {sample
                .slice(0, 5)
                .map((s) => s.accessionNumber || s.id)
                .join(", ")}
              {sample.length > 5 && ` (+${sample.length - 5} more)`}
            </div>
          </div>
        )}

        {/* Single Sample Info */}
        {!isBulk && firstSample && (
          <div
            style={{
              backgroundColor: "#e5f6ff",
              padding: "1rem",
              borderRadius: "4px",
              marginBottom: "1rem",
              borderLeft: "4px solid #0f62fe",
            }}
          >
            <strong>
              <FormattedMessage
                id="notebook.tb.inoculation.sample"
                defaultMessage="Sample:"
              />
            </strong>{" "}
            {firstSample.accessionNumber || firstSample.id}
            {firstSample.sampleType && (
              <span style={{ marginLeft: "1rem", color: "#525252" }}>
                ({firstSample.sampleType})
              </span>
            )}
            {processingRecord && (
              <div style={{ marginTop: "0.5rem", fontSize: "0.875rem" }}>
                <Tag type="blue" size="sm">
                  {processingRecord.decontaminationMethod || "Processed"}
                </Tag>
              </div>
            )}
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
          {/* Culture Method */}
          <Column lg={8} md={4} sm={4}>
            <Select
              id="cultureMethod"
              labelText={intl.formatMessage({
                id: "notebook.tb.inoculation.cultureMethod",
                defaultMessage: "Culture Method *",
              })}
              value={formData.cultureMethod}
              onChange={(e) =>
                handleInputChange("cultureMethod", e.target.value)
              }
            >
              {CULTURE_METHODS.map((method) => (
                <SelectItem
                  key={method.id}
                  value={method.id}
                  text={method.text}
                />
              ))}
            </Select>
          </Column>

          {/* Inoculation Date */}
          <Column lg={8} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              value={formData.inoculationDate}
              onChange={([date]) =>
                handleInputChange(
                  "inoculationDate",
                  date?.toISOString().split("T")[0] || "",
                )
              }
            >
              <DatePickerInput
                id="inoculationDate"
                labelText={intl.formatMessage({
                  id: "notebook.tb.inoculation.date",
                  defaultMessage: "Inoculation Date *",
                })}
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
          </Column>

          {/* Media Batch Selection */}
          <Column lg={16} md={8} sm={4}>
            <Dropdown
              id="mediaBatchId"
              titleText={intl.formatMessage({
                id: "notebook.tb.inoculation.mediaBatch",
                defaultMessage: "Select Media Batch *",
              })}
              label={
                loadingMedia
                  ? "Loading..."
                  : intl.formatMessage({
                      id: "notebook.tb.inoculation.selectBatch",
                      defaultMessage: "Select a media batch...",
                    })
              }
              items={availableMedia.map((batch) => ({
                id: String(batch.id),
                text: `${batch.batchId} (${batch.mediaType}) - ${batch.tubesCount || "?"} tubes`,
                batch,
              }))}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={
                selectedBatch
                  ? {
                      id: String(selectedBatch.id),
                      text: `${selectedBatch.batchId} (${selectedBatch.mediaType})`,
                      batch: selectedBatch,
                    }
                  : null
              }
              onChange={({ selectedItem }) =>
                handleInputChange("mediaBatchId", selectedItem?.id || "")
              }
              disabled={loadingMedia}
            />
          </Column>

          {/* Selected Batch Info */}
          {selectedBatch && (
            <Column lg={16} md={8} sm={4}>
              <div
                style={{
                  marginTop: "1rem",
                  padding: "1rem",
                  backgroundColor: "#defbe6",
                  borderRadius: "4px",
                  borderLeft: "4px solid #24a148",
                }}
              >
                <strong>
                  <FormattedMessage
                    id="notebook.tb.inoculation.batchInfo"
                    defaultMessage="Batch Info:"
                  />
                </strong>
                <Grid fullWidth style={{ marginTop: "0.5rem" }}>
                  <Column lg={4} md={2} sm={2}>
                    <div style={{ fontSize: "0.875rem" }}>
                      <span style={{ color: "#525252" }}>Batch ID:</span>
                      <br />
                      <strong>{selectedBatch.batchId}</strong>
                    </div>
                  </Column>
                  <Column lg={4} md={2} sm={2}>
                    <div style={{ fontSize: "0.875rem" }}>
                      <span style={{ color: "#525252" }}>Media Type:</span>
                      <br />
                      <strong>{selectedBatch.mediaType}</strong>
                    </div>
                  </Column>
                  <Column lg={4} md={2} sm={2}>
                    <div style={{ fontSize: "0.875rem" }}>
                      <span style={{ color: "#525252" }}>QC Status:</span>
                      <br />
                      <Tag
                        type={
                          selectedBatch.qcStatus === "PASSED" ? "green" : "gray"
                        }
                        size="sm"
                      >
                        {selectedBatch.qcStatus}
                      </Tag>
                    </div>
                  </Column>
                  <Column lg={4} md={2} sm={2}>
                    <div style={{ fontSize: "0.875rem" }}>
                      <span style={{ color: "#525252" }}>Expiry:</span>
                      <br />
                      <strong>{selectedBatch.expiryDate || "N/A"}</strong>
                    </div>
                  </Column>
                </Grid>
              </div>
            </Column>
          )}

          {/* No media available warning */}
          {!loadingMedia && availableMedia.length === 0 && (
            <Column lg={16} md={8} sm={4}>
              <InlineNotification
                kind="warning"
                title={intl.formatMessage({
                  id: "notebook.tb.inoculation.noMediaAvailable",
                  defaultMessage:
                    "No QC-passed media batches available for the selected type. Please create a media batch first.",
                })}
                hideCloseButton
                lowContrast
                style={{ marginTop: "1rem" }}
              />
            </Column>
          )}
        </Grid>
      </div>
    </Modal>
  );
}

export default InoculationModal;
