import React, { useState } from "react";
import {
  Modal,
  NumberInput,
  TextArea,
  Dropdown,
  FormLabel,
  Stack,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { InventoryLotAPI } from "./InventoryService";

const LotAdjustmentModal = ({ open, onClose, onSave, lot }) => {
  const intl = useIntl();

  const adjustmentReasons = [
    { id: "INVENTORY_COUNT", text: "Physical Inventory Count" },
    { id: "DAMAGED", text: "Damaged Items" },
    { id: "FOUND", text: "Items Found" },
    { id: "ERROR_CORRECTION", text: "Data Entry Error Correction" },
    { id: "EXPIRED_DISPOSAL", text: "Expired Item Disposal" },
    { id: "OTHER", text: "Other" },
  ];

  const [formData, setFormData] = useState({
    newQuantity: lot?.currentQuantity || 0,
    reason: "INVENTORY_COUNT",
    notes: "",
  });

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const handleChange = (field, value) => {
    setFormData((prev) => {
      if (prev[field] === value) {
        return prev;
      }
      return { ...prev, [field]: value };
    });
    setError(null);
  };

  const validate = () => {
    if (formData.newQuantity < 0) {
      setError("Quantity cannot be negative");
      return false;
    }

    if (!formData.reason) {
      setError("Please select a reason for adjustment");
      return false;
    }

    if (formData.reason === "OTHER" && !formData.notes?.trim()) {
      setError("Please provide notes when selecting 'Other' as reason");
      return false;
    }

    return true;
  };

  const handleSubmit = async () => {
    if (!validate()) return;

    setSaving(true);
    setError(null);

    try {
      await InventoryLotAPI.adjust(
        lot.id,
        formData.newQuantity,
        formData.reason,
      );

      setFormData({
        newQuantity: lot?.currentQuantity || 0,
        reason: "INVENTORY_COUNT",
        notes: "",
      });

      onSave();
    } catch (err) {
      console.error("Error adjusting lot:", err);
      setError(err.message || "Error adjusting lot quantity");
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    setFormData({
      newQuantity: lot?.currentQuantity || 0,
      reason: "INVENTORY_COUNT",
      notes: "",
    });
    setError(null);
    onClose();
  };

  if (!lot) return null;

  const quantityDifference = formData.newQuantity - (lot.currentQuantity || 0);

  return (
    <Modal
      open={open}
      onRequestClose={handleCancel}
      onRequestSubmit={handleSubmit}
      modalHeading={intl.formatMessage({ id: "adjustment.title" })}
      primaryButtonText={intl.formatMessage({ id: "button.adjust" })}
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
            <strong>{lot.lotNumber}</strong> - {lot.inventoryItem?.name}
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

        {/* New Quantity */}
        <NumberInput
          id="newQuantity"
          label={intl.formatMessage({ id: "adjustment.newQuantity" })}
          min={0}
          max={999999999}
          value={formData.newQuantity}
          onChange={(e, { value }) => handleChange("newQuantity", value)}
          invalidText={error}
          invalid={!!error}
          helperText={
            quantityDifference !== 0
              ? `${quantityDifference > 0 ? "+" : ""}${quantityDifference} ${lot.inventoryItem?.units || "units"}`
              : ""
          }
        />

        {/* Reason */}
        <Dropdown
          id="reason"
          titleText={intl.formatMessage({ id: "adjustment.reason" })}
          label={intl.formatMessage({ id: "adjustment.reason.select" })}
          items={adjustmentReasons}
          itemToString={(item) => (item ? item.text : "")}
          selectedItem={adjustmentReasons.find((r) => r.id === formData.reason)}
          onChange={({ selectedItem }) =>
            handleChange("reason", selectedItem.id)
          }
        />

        {/* Notes */}
        <TextArea
          id="notes"
          labelText={intl.formatMessage({ id: "adjustment.notes" })}
          value={formData.notes}
          onChange={(e) => handleChange("notes", e.target.value)}
          placeholder={intl.formatMessage({
            id: "adjustment.notes.placeholder",
          })}
          rows={3}
          required={formData.reason === "OTHER"}
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

export default LotAdjustmentModal;
