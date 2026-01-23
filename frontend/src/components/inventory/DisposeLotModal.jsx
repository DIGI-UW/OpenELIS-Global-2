import React, { useState, useEffect } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  TextArea,
  Dropdown,
  FormLabel,
  Stack,
  InlineNotification,
  Checkbox,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { InventoryLotAPI } from "./InventoryService";
import "./DisposeLotModal.css";

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

  const disposalMethods = [
    { id: "AUTOCLAVE", text: "Biohazard Autoclave" },
    { id: "NEUTRALIZATION", text: "Chemical Neutralization" },
    { id: "INCINERATION", text: "Incineration" },
    { id: "OTHER", text: "Other" },
  ];

  const [formData, setFormData] = useState({
    reason: "",
    method: "",
    notes: "",
  });

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [confirmed, setConfirmed] = useState(false);

  const handleChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    setError(null);
  };

  const canConfirm = formData.reason && formData.method && confirmed;

  const validate = () => {
    if (!formData.reason) {
      setError("Please select a disposal reason");
      return false;
    }

    if (!formData.method) {
      setError("Please select a disposal method");
      return false;
    }

    if (!confirmed) {
      setError("Please confirm the disposal action");
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
        }
      } else {
        await InventoryLotAPI.dispose(
          singleLot.id,
          formData.reason,
          formData.notes,
        );
      }

      setFormData({
        reason: "",
        method: "",
        notes: "",
      });
      setConfirmed(false);

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
      reason: "",
      method: "",
      notes: "",
    });
    setConfirmed(false);
    setError(null);
    onClose();
  };

  useEffect(() => {
    const handleEscape = (event) => {
      if (event.key === "Escape" && open) {
        handleCancel();
      }
    };

    if (open) {
      document.addEventListener("keydown", handleEscape);
    }

    return () => {
      document.removeEventListener("keydown", handleEscape);
    };
  }, [open]);

  const handleKeyDown = (event) => {
    if (event.key === "Enter" && !event.shiftKey) {
      if (event.target.tagName === "TEXTAREA") {
        return;
      }
      event.preventDefault();
      if (canConfirm) {
        handleSubmit();
      }
    }
  };

  if (!singleLot && !isBatchMode) return null;

  return (
    <ComposedModal open={open} onClose={handleCancel} size="lg">
      <ModalHeader
        title={
          isBatchMode
            ? intl.formatMessage(
                { id: "disposal.batch.title" },
                { count: lots.length },
              )
            : intl.formatMessage({ id: "disposal.title" })
        }
        subtitle={
          isBatchMode
            ? intl.formatMessage(
                { id: "disposal.batch.subtitle" },
                { count: lots.length },
              )
            : intl.formatMessage(
                { id: "disposal.subtitle" },
                { lotNumber: singleLot?.lotNumber || "" },
              )
        }
      />
      <ModalBody onKeyDown={handleKeyDown}>
        <Stack gap={6}>
          <div className="dispose-modal-alert">
            <InlineNotification
              kind="error"
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
              lowContrast={false}
            />
          </div>

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

          <div className="dispose-modal-instructions">
            <InlineNotification
              kind="info"
              title={intl.formatMessage({ id: "disposal.instructions.title" })}
              subtitle={intl.formatMessage({
                id: "disposal.instructions.subtitle",
              })}
              hideCloseButton
              lowContrast={true}
            />
          </div>

          <div className="dispose-modal-separator" />

          <div className="dispose-modal-fields">
            <div className="form-group">
              <Dropdown
                id="reason"
                titleText={intl.formatMessage({ id: "disposal.reason" })}
                label={intl.formatMessage({ id: "disposal.reason.select" })}
                items={disposalReasons}
                itemToString={(item) => (item ? item.text : "")}
                selectedItem={
                  disposalReasons.find((r) => r.id === formData.reason) || null
                }
                onChange={({ selectedItem }) =>
                  handleChange("reason", selectedItem?.id || "")
                }
                required
              />
            </div>

            <div className="form-group">
              <Dropdown
                id="disposal-method"
                titleText={intl.formatMessage({ id: "disposal.method" })}
                label={intl.formatMessage({ id: "disposal.method.select" })}
                items={disposalMethods}
                itemToString={(item) => (item ? item.text : "")}
                selectedItem={
                  disposalMethods.find((m) => m.id === formData.method) || null
                }
                onChange={({ selectedItem }) =>
                  handleChange("method", selectedItem?.id || "")
                }
                required
              />
            </div>

            <div className="form-group">
              <TextArea
                id="notes"
                labelText={intl.formatMessage({ id: "disposal.notes" })}
                value={formData.notes}
                onChange={(e) => handleChange("notes", e.target.value)}
                placeholder={intl.formatMessage({
                  id: "disposal.notes.placeholder",
                })}
                rows={3}
                required={formData.reason === "OTHER"}
              />
            </div>

            <div className="form-group">
              <Checkbox
                id="disposal-confirmation"
                labelText={intl.formatMessage(
                  isBatchMode
                    ? { id: "disposal.batch.confirmation" }
                    : { id: "disposal.confirmation" },
                  isBatchMode ? { count: lots.length } : {},
                )}
                checked={confirmed}
                onChange={(_, { checked }) => setConfirmed(checked)}
              />
            </div>
          </div>

          {error && (
            <div className="error-message" style={{ color: "#da1e28" }}>
              {error}
            </div>
          )}
        </Stack>
      </ModalBody>
      <ModalFooter>
        <Button kind="secondary" onClick={handleCancel}>
          <FormattedMessage id="label.button.cancel" defaultMessage="Cancel" />
        </Button>
        <Button
          kind="danger"
          onClick={handleSubmit}
          disabled={!canConfirm || saving}
        >
          <FormattedMessage
            id="storage.confirm.disposal"
            defaultMessage="Confirm Disposal"
          />
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

export default DisposeLotModal;
