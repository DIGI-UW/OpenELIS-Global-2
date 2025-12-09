import React, { useState } from "react";
import {
  Modal,
  TextArea,
  Dropdown,
  FormLabel,
  Stack,
  InlineNotification,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { InventoryLotAPI } from "./InventoryService";

const DisposeLotModal = ({ open, onClose, onSave, lot, lots }) => {
  const intl = useIntl();
  const isBatchMode = lots && lots.length > 1;
  const singleLot = lot || (lots && lots.length === 1 ? lots[0] : null);

  const disposalReasons = [
    { id: "EXPIRED", text: "Expired" },
    { id: "DAMAGED", text: "Damaged" },
    { id: "CONTAMINATED", text: "Contaminated" },
    { id: "RECALLED", text: "Manufacturer Recall" },
    { id: "QC_FAILED", text: "Failed Quality Control" },
    { id: "OTHER", text: "Other" },
  ];

  const [formData, setFormData] = useState({
    reason: "EXPIRED",
    notes: "",
  });

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const handleChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    setError(null);
  };

  const validate = () => {
    if (!formData.reason) {
      setError("Please select a disposal reason");
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
      if (isBatchMode) {
        // Batch disposal
        const lotIds = lots.map((l) => l.id);
        const response = await InventoryLotAPI.batchDispose(
          lotIds,
          formData.reason,
          formData.notes,
        );

        if (response.failedCount > 0) {
          setError(
            `Disposed ${response.successCount} lots successfully. ${response.failedCount} failed: ${response.errors}`,
          );
          // Don't return - still close and refresh
        }
      } else {
        // Single disposal
        await InventoryLotAPI.dispose(
          singleLot.id,
          formData.reason,
          formData.notes,
        );
      }

      setFormData({
        reason: "EXPIRED",
        notes: "",
      });

      onSave();
    } catch (err) {
      console.error("Error disposing lot:", err);
      setError(err.message || "Error disposing lot");
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    setFormData({
      reason: "EXPIRED",
      notes: "",
    });
    setError(null);
    onClose();
  };

  if (!singleLot && !isBatchMode) return null;

  return (
    <Modal
      open={open}
      onRequestClose={handleCancel}
      onRequestSubmit={handleSubmit}
      modalHeading={
        isBatchMode
          ? intl.formatMessage(
              { id: "disposal.batch.title" },
              { count: lots.length },
            )
          : intl.formatMessage({ id: "disposal.title" })
      }
      primaryButtonText={intl.formatMessage({ id: "button.dispose" })}
      secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
      primaryButtonDisabled={saving}
      danger
      size="sm"
    >
      <Stack gap={6}>
        <InlineNotification
          kind="warning"
          title={intl.formatMessage({ id: "disposal.warning.title" })}
          subtitle={
            isBatchMode
              ? intl.formatMessage(
                  { id: "disposal.batch.warning.message" },
                  { count: lots.length },
                )
              : intl.formatMessage({ id: "disposal.warning.message" })
          }
          hideCloseButton
          lowContrast
        />

        {isBatchMode ? (
          <div>
            <FormLabel>
              <FormattedMessage
                id="disposal.batch.lots"
                defaultMessage="Selected Lots"
              />
            </FormLabel>
            <div
              style={{
                maxHeight: "200px",
                overflowY: "auto",
                padding: "0.5rem",
                border: "1px solid #e0e0e0",
                borderRadius: "4px",
              }}
            >
              {lots.map((l, idx) => (
                <div key={l.id} style={{ marginBottom: "0.5rem" }}>
                  <strong>{l.lotNumber}</strong> - {l.inventoryItem?.name} (
                  {l.currentQuantity} {l.inventoryItem?.units || "units"})
                </div>
              ))}
            </div>
          </div>
        ) : (
          <>
            <div>
              <FormLabel>
                <FormattedMessage id="lot.number" />
              </FormLabel>
              <p>
                <strong>{singleLot.lotNumber}</strong> -{" "}
                {singleLot.inventoryItem?.name}
              </p>
            </div>

            <div>
              <FormLabel>
                <FormattedMessage id="lot.currentQuantity" />
              </FormLabel>
              <p>
                <strong>
                  {singleLot.currentQuantity}{" "}
                  {singleLot.inventoryItem?.units || "units"}
                </strong>
              </p>
            </div>

            {singleLot.expirationDate && (
              <div>
                <FormLabel>
                  <FormattedMessage id="lot.expirationDate" />
                </FormLabel>
                <p>
                  <strong>
                    {new Date(singleLot.expirationDate).toLocaleDateString()}
                  </strong>
                </p>
              </div>
            )}
          </>
        )}

        <Dropdown
          id="reason"
          titleText={intl.formatMessage({ id: "disposal.reason" })}
          label={intl.formatMessage({ id: "disposal.reason.select" })}
          items={disposalReasons}
          itemToString={(item) => (item ? item.text : "")}
          selectedItem={disposalReasons.find((r) => r.id === formData.reason)}
          onChange={({ selectedItem }) =>
            handleChange("reason", selectedItem.id)
          }
        />

        <TextArea
          id="notes"
          labelText={intl.formatMessage({ id: "disposal.notes" })}
          value={formData.notes}
          onChange={(e) => handleChange("notes", e.target.value)}
          placeholder={intl.formatMessage({ id: "disposal.notes.placeholder" })}
          rows={4}
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

export default DisposeLotModal;
