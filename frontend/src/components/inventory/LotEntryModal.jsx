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
import StorageLocationFormModal from "./StorageLocationFormModal";

const LotEntryModal = ({ open, onClose, onSave, lot = null }) => {
  const intl = useIntl();
  const isEdit = !!lot;

  const [formData, setFormData] = useState({
    inventoryItem: null,
    lotNumber: "",
    currentQuantity: 0,
    unitSize: "",
    expirationDate: null,
    receiptDate: new Date(),
    dateOpened: null,
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
        unitSize: lot.unitSize || "",
        expirationDate: lot.expirationDate
          ? new Date(lot.expirationDate)
          : null,
        receiptDate: lot.receiptDate ? new Date(lot.receiptDate) : new Date(),
        dateOpened: lot.dateOpened ? new Date(lot.dateOpened) : null,
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

  const flattenLocationTree = (locations) => {
    const rootLocations = locations.filter((loc) => !loc.parentLocation);

    const flattenNode = (location, depth = 0) => {
      const children = locations.filter(
        (loc) => loc.parentLocation?.id === location.id,
      );

      const indent = "\u00A0\u00A0".repeat(depth);
      const prefix = depth > 0 ? "├─ " : "";

      return [
        {
          id: location.id,
          text: `${indent}${prefix}${location.name}`,
          location: location,
          depth: depth,
        },
        ...children.flatMap((child) => flattenNode(child, depth + 1)),
      ];
    };

    return rootLocations.flatMap((loc) => flattenNode(loc));
  };

  const fetchLocations = async () => {
    try {
      const allLocations = await StorageLocationAPI.getAll();
      const validLocations = Array.isArray(allLocations) ? allLocations : [];

      // Build hierarchical tree with indentation
      const hierarchicalLocations = flattenLocationTree(validLocations);

      setLocations(hierarchicalLocations);
    } catch (err) {
      console.error("Error fetching locations:", err);
      setLocations([]);
    }
  };

  const handleLocationCreated = async (locationData) => {
    try {
      const newLocation = await StorageLocationAPI.create(locationData);
      setLocationModalOpen(false);
      await fetchLocations();
      handleChange("storageLocation", newLocation);
    } catch (err) {
      console.error("Error creating storage location:", err);
      setError("Failed to create storage location");
    }
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

    if (!formData.unitSize?.trim()) {
      setError(
        "Unit size is required (e.g., 50 mL, 100 tests, 1 test per strip)",
      );
      return false;
    }

    if (!formData.storageLocation) {
      setError("Please select a storage location");
      return false;
    }

    if (formData.dateOpened) {
      const openingDate = new Date(formData.dateOpened);
      const today = new Date();
      today.setHours(23, 59, 59, 999);

      if (openingDate > today) {
        setError("Opening date cannot be in the future");
        return false;
      }

      if (
        formData.receiptDate &&
        openingDate < new Date(formData.receiptDate)
      ) {
        setError("Opening date cannot be before receipt date");
        return false;
      }

      if (
        formData.expirationDate &&
        openingDate > new Date(formData.expirationDate)
      ) {
        setError("Opening date cannot be after expiration date");
        return false;
      }
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
          unitSize: formData.unitSize,
          barcode: formData.barcode || null,
          dateOpened: formData.dateOpened
            ? formData.dateOpened.toISOString()
            : null,
          initialQuantity: lot.initialQuantity,
          version: lot.version,
        });
      } else {
        await InventoryManagementAPI.receive({
          inventoryItem: { id: formData.inventoryItem.id },
          lotNumber: formData.lotNumber,
          currentQuantity: formData.currentQuantity,
          initialQuantity: formData.currentQuantity,
          unitSize: formData.unitSize,
          expirationDate: formData.expirationDate
            ? formData.expirationDate.toISOString()
            : null,
          receiptDate: formData.receiptDate.toISOString(),
          dateOpened: formData.dateOpened
            ? formData.dateOpened.toISOString()
            : null,
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
        size="sm"
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

          <TextInput
            id="unitSize"
            labelText="Unit Size *"
            helperText="Size/volume of each individual unit (e.g., 50 mL per bottle, 1 test per strip)"
            value={formData.unitSize}
            onChange={(e) => handleChange("unitSize", e.target.value)}
            placeholder="e.g., 50 mL, 100 tests, 250 μL, 1 test"
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

          <DatePicker
            datePickerType="single"
            value={formData.dateOpened}
            onChange={([date]) => handleChange("dateOpened", date)}
          >
            <DatePickerInput
              id="dateOpened"
              labelText="Opening Date"
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

      <StorageLocationFormModal
        isOpen={locationModalOpen}
        onClose={() => setLocationModalOpen(false)}
        onSubmit={handleLocationCreated}
        mode="create"
      />
    </>
  );
};

export default LotEntryModal;
