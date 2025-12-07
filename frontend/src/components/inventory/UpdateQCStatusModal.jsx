import React, { useState } from "react";
import {
  Modal,
  Dropdown,
  TextArea,
  FormLabel,
  Stack,
  InlineNotification,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { InventoryLotAPI } from "./InventoryService";

const UpdateQCStatusModal = ({ open, onClose, onSave, lot }) => {
  const intl = useIntl();

  // QC Status options
  const qcStatusOptions = [
    { id: "PENDING", text: "Pending QC" },
    { id: "PASSED", text: "Passed" },
    { id: "FAILED", text: "Failed" },
    { id: "NOT_REQUIRED", text: "Not Required" },
  ];

  // Form state
  const [formData, setFormData] = useState({
    qcStatus: lot?.qcStatus || "PENDING",
    notes: "",
  });

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  // Handle input changes
  const handleChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    setError(null);
  };

  // Validate form
  const validate = () => {
    if (!formData.qcStatus) {
      setError("Please select a QC status");
      return false;
    }

    if (formData.qcStatus === "FAILED" && !formData.notes?.trim()) {
      setError("Please provide notes explaining QC failure");
      return false;
    }

    return true;
  };

  // Handle form submission
  const handleSubmit = async () => {
    if (!validate()) return;

    setSaving(true);
    setError(null);

    try {
      await InventoryLotAPI.updateQCStatus(
        lot.id,
        formData.qcStatus,
        formData.notes,
      );

      // Reset form
      setFormData({
        qcStatus: lot?.qcStatus || "PENDING",
        notes: "",
      });

      onSave();
    } catch (err) {
      console.error("Error updating QC status:", err);
      setError(err.message || "Error updating QC status");
    } finally {
      setSaving(false);
    }
  };

  // Handle cancel
  const handleCancel = () => {
    setFormData({
      qcStatus: lot?.qcStatus || "PENDING",
      notes: "",
    });
    setError(null);
    onClose();
  };

  if (!lot) return null;

  const isStatusChange = formData.qcStatus !== lot.qcStatus;

  return (
    <Modal
      open={open}
      onRequestClose={handleCancel}
      onRequestSubmit={handleSubmit}
      modalHeading={intl.formatMessage({ id: "qc.status.update.title" })}
      primaryButtonText={intl.formatMessage({ id: "button.update" })}
      secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
      primaryButtonDisabled={saving || !isStatusChange}
      size="sm"
    >
      <Stack gap={6}>
        {/* Lot Information */}
        <div>
          <FormLabel>
            <FormattedMessage id="lot.number" />
          </FormLabel>
          <p>
            <strong>{lot.lotNumber}</strong> - {lot.inventoryItem?.name}
          </p>
        </div>

        <div>
          <FormLabel>
            <FormattedMessage id="qc.status.current" />
          </FormLabel>
          <p>
            <strong>{lot.qcStatus || "PENDING"}</strong>
          </p>
        </div>

        {/* Info message for FAILED status */}
        {formData.qcStatus === "FAILED" && (
          <InlineNotification
            kind="warning"
            title={intl.formatMessage({ id: "qc.status.failed.warning.title" })}
            subtitle={intl.formatMessage({
              id: "qc.status.failed.warning.message",
            })}
            hideCloseButton
            lowContrast
          />
        )}

        {/* QC Status Dropdown */}
        <Dropdown
          id="qcStatus"
          titleText={intl.formatMessage({ id: "qc.status.new" })}
          label={intl.formatMessage({ id: "qc.status.select" })}
          items={qcStatusOptions}
          itemToString={(item) => (item ? item.text : "")}
          selectedItem={qcStatusOptions.find((s) => s.id === formData.qcStatus)}
          onChange={({ selectedItem }) =>
            handleChange("qcStatus", selectedItem.id)
          }
        />

        {/* Notes */}
        <TextArea
          id="notes"
          labelText={intl.formatMessage({ id: "qc.status.notes" })}
          value={formData.notes}
          onChange={(e) => handleChange("notes", e.target.value)}
          placeholder={intl.formatMessage({
            id: "qc.status.notes.placeholder",
          })}
          rows={4}
          required={formData.qcStatus === "FAILED"}
        />

        {error && (
          <div className="error-message" style={{ color: "#da1e28" }}>
            {error}
          </div>
        )}
      </Stack>
    </Modal>
  );
};

export default UpdateQCStatusModal;
