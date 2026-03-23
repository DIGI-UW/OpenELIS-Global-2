import React, { useState, useEffect } from "react";
import {
  Modal,
  TextInput,
  Dropdown,
  NumberInput,
  DatePicker,
  DatePickerInput,
  FormLabel,
  Stack,
  Button,
} from "@carbon/react";
import { Add } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  InventoryItemAPI,
  InventoryLotAPI,
  InventoryManagementAPI,
  StorageLocationAPI,
} from "./InventoryService";
import StorageLocationModal from "./StorageLocationModal";

const LotEntryModal = ({ open, onClose, onSave, lot = null }) => {
  const intl = useIntl();
  const isEdit = !!lot;

  const [formData, setFormData] = useState({
    inventoryItem: null,
    lotNumber: "",
    currentQuantity: 0,
    expirationDate: null,
    receiptDate: new Date(),
    storageLocation: null,
    qcStatus: "PENDING",
    status: "ACTIVE",
    barcode: "",
  });

  const [items, setItems] = useState([]);
  const [locations, setLocations] = useState([]);

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const [locationModalOpen, setLocationModalOpen] = useState(false);

  const qcStatusOptions = [
    { id: "PENDING", text: "Pending" },
    { id: "PASSED", text: "Passed" },
    { id: "FAILED", text: "Failed" },
    { id: "QUARANTINED", text: "Quarantined" },
  ];

  const statusOptions = [
    { id: "ACTIVE", text: "Active" },
    { id: "IN_USE", text: "In Use" },
    { id: "QUARANTINED", text: "Quarantined" },
  ];

  useEffect(() => {
    fetchItems();
    fetchLocations();
  }, []);

  useEffect(() => {
    if (lot) {
      setFormData({
        inventoryItem: lot.inventoryItem,
        lotNumber: lot.lotNumber || "",
        currentQuantity: lot.currentQuantity || 0,
        expirationDate: lot.expirationDate
          ? new Date(lot.expirationDate)
          : null,
        receiptDate: lot.receiptDate ? new Date(lot.receiptDate) : new Date(),
        storageLocation: lot.storageLocation,
        qcStatus: lot.qcStatus || "PENDING",
        status: lot.status || "ACTIVE",
        barcode: lot.barcode || "",
      });
    }
  }, [lot]);

  const fetchItems = async () => {
    try {
      const allItems = await InventoryItemAPI.getAll({ isActive: true });
      const validItems = Array.isArray(allItems) ? allItems : [];
      setItems(
        validItems.map((item) => ({
          id: item.id,
          text: `${item.name} (${item.itemType})`,
          item: item,
        })),
      );
    } catch (err) {
      console.error("Error fetching items:", err);
      setItems([]);
    }
  };

  const fetchLocations = async () => {
    try {
      const allLocations = await StorageLocationAPI.getAll();
      const validLocations = Array.isArray(allLocations) ? allLocations : [];
      setLocations(
        validLocations.map((loc) => ({
          id: loc.id,
          text: loc.name,
          location: loc,
        })),
      );
    } catch (err) {
      console.error("Error fetching locations:", err);
      setLocations([]);
    }
  };

  const handleLocationCreated = (newLocation) => {
    setLocationModalOpen(false);
    fetchLocations();
    handleChange("storageLocation", newLocation);
  };

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
    if (!formData.inventoryItem) {
      setError("Please select a catalog item");
      return false;
    }

    if (!formData.lotNumber?.trim()) {
      setError("Lot number is required");
      return false;
    }

    if (!formData.currentQuantity || formData.currentQuantity <= 0) {
      setError("Quantity must be greater than 0");
      return false;
    }

    if (!formData.storageLocation) {
      setError("Please select a storage location");
      return false;
    }

    return true;
  };

  const handleSave = async () => {
    if (!validate()) return;

    setSaving(true);
    setError(null);

    try {
      if (isEdit) {
        await InventoryLotAPI.update(lot.id, {
          ...formData,
          inventoryItem: formData.inventoryItem,
          storageLocation: formData.storageLocation,
          initialQuantity: lot.initialQuantity,
          version: lot.version,
        });
      } else {
        await InventoryManagementAPI.receive({
          inventoryItem: { id: formData.inventoryItem.id },
          lotNumber: formData.lotNumber,
          currentQuantity: formData.currentQuantity,
          initialQuantity: formData.currentQuantity,
          expirationDate: formData.expirationDate
            ? formData.expirationDate.toISOString()
            : null,
          receiptDate: formData.receiptDate.toISOString(),
          storageLocation: { id: formData.storageLocation.id },
          qcStatus: formData.qcStatus,
          status: formData.status,
          barcode: formData.barcode || null,
        });
      }
      onSave();
    } catch (err) {
      console.error("Error saving lot:", err);
      setError(err.message || "Error saving lot");
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <Modal
        open={open}
        onRequestClose={onClose}
        onRequestSubmit={handleSave}
        modalHeading={intl.formatMessage({
          id: isEdit ? "lot.form.title.edit" : "lot.form.title.add",
        })}
        primaryButtonText={intl.formatMessage({ id: "button.save" })}
        secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
        primaryButtonDisabled={saving}
        size="md"
      >
        <Stack gap={5}>
          {error && (
            <div style={{ color: "red", marginBottom: "1rem" }}>{error}</div>
          )}

          <Dropdown
            id="inventoryItem"
            titleText={<FormattedMessage id="lot.selectItem" />}
            label="Select catalog item"
            items={items}
            itemToString={(item) => (item ? item.text : "")}
            selectedItem={
              formData.inventoryItem
                ? items.find((i) => i.id === formData.inventoryItem.id)
                : null
            }
            onChange={({ selectedItem }) =>
              handleChange("inventoryItem", selectedItem.item)
            }
            required
            disabled={isEdit}
          />

          <TextInput
            id="lotNumber"
            labelText={<FormattedMessage id="lot.number" />}
            value={formData.lotNumber}
            onChange={(e) => handleChange("lotNumber", e.target.value)}
            required
          />

          <NumberInput
            id="currentQuantity"
            label={<FormattedMessage id="lot.initialQuantity" />}
            value={formData.currentQuantity}
            onChange={(e, { value }) => handleChange("currentQuantity", value)}
            min={0}
            max={999999999}
            step={1}
            required
          />

          <DatePicker
            datePickerType="single"
            value={formData.expirationDate}
            onChange={([date]) => handleChange("expirationDate", date)}
          >
            <DatePickerInput
              id="expirationDate"
              labelText={<FormattedMessage id="lot.expirationDate" />}
              placeholder="mm/dd/yyyy"
            />
          </DatePicker>

          <DatePicker
            datePickerType="single"
            value={formData.receiptDate}
            onChange={([date]) => handleChange("receiptDate", date)}
          >
            <DatePickerInput
              id="receiptDate"
              labelText={<FormattedMessage id="lot.receiptDate" />}
              placeholder="mm/dd/yyyy"
            />
          </DatePicker>

          <div>
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "flex-end",
                marginBottom: "0.5rem",
              }}
            >
              <FormLabel>
                <FormattedMessage id="lot.selectLocation" />
                <span style={{ color: "#da1e28" }}> *</span>
              </FormLabel>
              <Button
                kind="ghost"
                size="sm"
                renderIcon={Add}
                onClick={() => setLocationModalOpen(true)}
              >
                <FormattedMessage id="storage.location.add.button" />
              </Button>
            </div>
            <Dropdown
              id="storageLocation"
              label="Select storage location"
              items={locations}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={
                formData.storageLocation
                  ? locations.find((l) => l.id === formData.storageLocation.id)
                  : null
              }
              onChange={({ selectedItem }) =>
                handleChange("storageLocation", selectedItem.location)
              }
              required
            />
          </div>

          <Dropdown
            id="qcStatus"
            titleText={<FormattedMessage id="lot.qcStatus" />}
            label="Select QC status"
            items={qcStatusOptions}
            itemToString={(item) => (item ? item.text : "")}
            selectedItem={qcStatusOptions.find(
              (s) => s.id === formData.qcStatus,
            )}
            onChange={({ selectedItem }) =>
              handleChange("qcStatus", selectedItem.id)
            }
          />

          <Dropdown
            id="status"
            titleText={<FormattedMessage id="lot.status" />}
            label="Select status"
            items={statusOptions}
            itemToString={(item) => (item ? item.text : "")}
            selectedItem={statusOptions.find((s) => s.id === formData.status)}
            onChange={({ selectedItem }) =>
              handleChange("status", selectedItem.id)
            }
          />

          <TextInput
            id="barcode"
            labelText={<FormattedMessage id="lot.barcode" />}
            value={formData.barcode}
            onChange={(e) => handleChange("barcode", e.target.value)}
            placeholder="Optional"
          />
        </Stack>
      </Modal>

      {/* Storage Location Creation Modal */}
      <StorageLocationModal
        open={locationModalOpen}
        onClose={() => setLocationModalOpen(false)}
        onSave={handleLocationCreated}
      />
    </>
  );
};

export default LotEntryModal;
