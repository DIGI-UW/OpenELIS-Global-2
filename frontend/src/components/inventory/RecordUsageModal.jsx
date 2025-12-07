import React, { useState } from "react";
import {
  Modal,
  NumberInput,
  TextArea,
  TextInput,
  FormLabel,
  Stack,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { InventoryManagementAPI } from "./InventoryService";

const RecordUsageModal = ({ open, onClose, onSave, lot }) => {
  const intl = useIntl();

  // Form state
  const [formData, setFormData] = useState({
    quantityUsed: 1,
    testResultId: "",
    notes: "",
  });

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  // Handle input changes
  const handleChange = (field, value) => {
    setFormData((prev) => {
      // Prevent unnecessary state updates if value hasn't changed
      if (prev[field] === value) {
        return prev;
      }
      return { ...prev, [field]: value };
    });
    setError(null);
  };

  // Validate form
  const validate = () => {
    if (!formData.quantityUsed || formData.quantityUsed <= 0) {
      setError("Quantity must be greater than 0");
      return false;
    }

    if (
      lot &&
      lot.currentQuantity &&
      formData.quantityUsed > lot.currentQuantity
    ) {
      setError(
        `Cannot use ${formData.quantityUsed} units. Only ${lot.currentQuantity} units available.`,
      );
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
      await InventoryManagementAPI.consume({
        lotId: lot.id,
        quantity: formData.quantityUsed,
        testResultId: formData.testResultId || null,
        notes: formData.notes,
      });

      // Reset form
      setFormData({
        quantityUsed: 1,
        testResultId: "",
        notes: "",
      });

      onSave();
    } catch (err) {
      console.error("Error recording usage:", err);
      setError(err.message || "Error recording usage");
    } finally {
      setSaving(false);
    }
  };

  // Handle cancel
  const handleCancel = () => {
    setFormData({
      quantityUsed: 1,
      testResultId: "",
      notes: "",
    });
    setError(null);
    onClose();
  };

  if (!lot) return null;

  return (
    <Modal
      open={open}
      onRequestClose={handleCancel}
      onRequestSubmit={handleSubmit}
      modalHeading={intl.formatMessage({ id: "usage.record.title" })}
      primaryButtonText={intl.formatMessage({ id: "button.record" })}
      secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
      primaryButtonDisabled={saving}
      size="sm"
    >
      <Stack gap={6}>
        {/* Lot Information */}
        <div>
          <FormLabel>
            <FormattedMessage id="lot.number" />
          </FormLabel>
          <p>
            <strong>{lot.lotNumber}</strong>
          </p>
        </div>

        <div>
          <FormLabel>
            <FormattedMessage id="lot.currentQuantity" />
          </FormLabel>
          <p>
            <strong>
              {lot.currentQuantity} {lot.inventoryItem?.units || "units"}
            </strong>
          </p>
        </div>

        {/* Quantity Used */}
        <NumberInput
          id="quantityUsed"
          label={intl.formatMessage({ id: "usage.quantityUsed" })}
          min={1}
          max={lot.currentQuantity}
          value={formData.quantityUsed}
          onChange={(e, { value }) => handleChange("quantityUsed", value)}
          invalidText={error}
          invalid={!!error}
          helperText={intl.formatMessage({ id: "usage.quantityUsed.helper" })}
        />

        {/* Test Result ID (Optional) */}
        <TextInput
          id="testResultId"
          labelText={intl.formatMessage({ id: "usage.testResultId" })}
          value={formData.testResultId}
          onChange={(e) => handleChange("testResultId", e.target.value)}
          placeholder={intl.formatMessage({
            id: "usage.testResultId.placeholder",
          })}
        />

        {/* Notes */}
        <TextArea
          id="notes"
          labelText={intl.formatMessage({ id: "usage.notes" })}
          value={formData.notes}
          onChange={(e) => handleChange("notes", e.target.value)}
          placeholder={intl.formatMessage({ id: "usage.notes.placeholder" })}
          rows={3}
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

export default RecordUsageModal;
