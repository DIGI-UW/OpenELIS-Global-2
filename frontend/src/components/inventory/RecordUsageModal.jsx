import React, { useState, useCallback, useEffect, useRef } from "react";
import {
  Modal,
  NumberInput,
  TextArea,
  ComboBox,
  FormLabel,
  Stack,
  InlineNotification,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { InventoryManagementAPI } from "./InventoryService";
import { getFromOpenElisServer } from "../utils/Utils";

// Consumption is item-level FEFO (POST /management/consume has no lot id),
// so this modal speaks about the item, not the lot row it was opened from.
const RecordUsageModal = ({ open, onClose, onSave, lot }) => {
  const intl = useIntl();

  const [formData, setFormData] = useState({
    quantityUsed: 1,
    testResultId: "",
    notes: "",
  });

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [searchResults, setSearchResults] = useState([]);
  const [searchLoading, setSearchLoading] = useState(false);

  // onSave() unmounts this modal before the finally block runs.
  const isMountedRef = useRef(true);
  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  const handleChange = (field, value) => {
    setFormData((prev) => {
      if (prev[field] === value) {
        return prev;
      }
      return { ...prev, [field]: value };
    });
    setError(null);
  };

  const searchAccessionNumbers = useCallback((query) => {
    if (!query || query.length < 2) {
      setSearchResults([]);
      return;
    }

    setSearchLoading(true);
    getFromOpenElisServer(
      `/rest/samples/search?accessionNumber=${encodeURIComponent(query)}&includeTests=true`,
      (response) => {
        setSearchLoading(false);
        if (response && response.samples) {
          const items = response.samples.map((sample) => ({
            id: sample.accessionNumber,
            text: `${sample.accessionNumber} - ${sample.patientName || "Unknown"}`,
          }));
          setSearchResults(items);
        } else {
          setSearchResults([]);
        }
      },
    );
  }, []);

  // Sufficiency is the server's call: it sums the item's usable lots.
  const validate = () => {
    if (!formData.quantityUsed || formData.quantityUsed <= 0) {
      setError(intl.formatMessage({ id: "usage.error.quantityPositive" }));
      return false;
    }
    return true;
  };

  const handleSubmit = async () => {
    if (!validate()) return;

    setSaving(true);
    setError(null);

    try {
      await InventoryManagementAPI.consume({
        itemId: String(lot.inventoryItem.id),
        quantity: formData.quantityUsed,
        testResultId: formData.testResultId || null,
        analysisId: null,
      });

      setFormData({
        quantityUsed: 1,
        testResultId: "",
        notes: "",
      });

      onSave();
    } catch (err) {
      console.error("Error recording usage:", err);
      if (!isMountedRef.current) return;
      // errorCode is an en.json id; message is the raw backend string.
      setError(
        err.errorCode
          ? intl.formatMessage({ id: err.errorCode }, err.params)
          : err.message || intl.formatMessage({ id: "usage.record.error" }),
      );
    } finally {
      if (isMountedRef.current) setSaving(false);
    }
  };

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
        <div>
          <FormLabel>
            <FormattedMessage id="usage.item" />
          </FormLabel>
          <p>
            <strong>{lot.inventoryItem?.name}</strong>
          </p>
        </div>

        <InlineNotification
          kind="info"
          title=""
          subtitle={intl.formatMessage({ id: "usage.fefo.note" })}
          hideCloseButton
          lowContrast
        />

        <NumberInput
          id="quantityUsed"
          label={intl.formatMessage({ id: "usage.quantityUsed" })}
          min={1}
          value={formData.quantityUsed}
          onChange={(e, { value }) => handleChange("quantityUsed", value)}
          helperText={intl.formatMessage({ id: "usage.quantityUsed.helper" })}
        />

        <ComboBox
          id="testResultId"
          titleText={intl.formatMessage({ id: "usage.testResultId" })}
          placeholder={intl.formatMessage({
            id: "usage.testResultId.placeholder",
          })}
          items={searchResults}
          itemToString={(item) => (item ? item.text : "")}
          onInputChange={(query) => searchAccessionNumbers(query)}
          onChange={({ selectedItem }) => {
            handleChange("testResultId", selectedItem ? selectedItem.id : "");
          }}
          helperText="Start typing an accession number to search, or type manually"
        />

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
