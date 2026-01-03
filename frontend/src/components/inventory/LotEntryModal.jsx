import React, { useState, useEffect, useCallback } from "react";
import {
  Modal,
  TextInput,
  Dropdown,
  NumberInput,
  DatePicker,
  DatePickerInput,
  FormLabel,
  Stack,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  InventoryItemAPI,
  InventoryLotAPI,
  InventoryManagementAPI,
} from "./InventoryService";
import StorageHierarchySelector from "../notebook/workflow/StorageHierarchySelector";

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
    qcStatus: "PENDING",
    status: "ACTIVE",
    barcode: "",
    // Reagent/Equipment specific fields for lot entry
    receivedBy: "",
    storageLocation: "",
    storageBoxNumber: "",
  });

  // Unified storage hierarchy selection state
  const [storageSelection, setStorageSelection] = useState({
    room: null,
    device: null,
    shelf: null,
    rack: null,
    box: null,
  });

  const [items, setItems] = useState([]);

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

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
        qcStatus: lot.qcStatus || "PENDING",
        status: lot.status || "ACTIVE",
        barcode: lot.barcode || "",
        // Load lot-specific fields
        receivedBy: lot.receivedBy || "",
        storageLocation: lot.specificStorageLocation || "",
        storageBoxNumber: lot.storageBoxNumber || "",
      });
      // Note: For editing, we would need to load the storage hierarchy
      // based on lot.locationId and lot.locationType if available
    }
  }, [lot]);

  const fetchItems = async () => {
    try {
      // Use non-paginated endpoint to show all catalog items in dropdown
      const response = await InventoryItemAPI.getAll({
        isActive: true,
      });

      // getAll returns items directly, not wrapped in a pagination response
      const allItems = response || [];
      const validItems = Array.isArray(allItems) ? allItems : [];

      // Sort items by name for better UX
      const sortedItems = validItems.sort((a, b) =>
        a.name.localeCompare(b.name)
      );

      setItems(
        sortedItems.map((item) => ({
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

  // Handle storage hierarchy selection change
  const handleStorageSelectionChange = useCallback((selection) => {
    setStorageSelection(selection);
    setError(null);

    // Auto-populate manual storage fields from hierarchy selection
    const storagePath = buildStoragePathFromSelection(selection);
    const boxInfo = getBoxInfoFromSelection(selection);

    // Update form data with auto-populated values
    setFormData(prev => ({
      ...prev,
      storageLocation: boxInfo.freezerInfo || "", // Main storage device info
      storageBoxNumber: boxInfo.boxNumber || "",  // Box/container number
    }));
  }, []);

  // Helper function to build storage path from selection object
  const buildStoragePathFromSelection = (selection) => {
    const parts = [];
    if (selection.room) parts.push(selection.room.label);
    if (selection.device) parts.push(selection.device.label);
    if (selection.shelf) parts.push(selection.shelf.label);
    if (selection.rack) parts.push(selection.rack.label);
    if (selection.box) parts.push(selection.box.label);
    return parts.join(" > ");
  };

  // Helper function to extract box and freezer info from selection
  const getBoxInfoFromSelection = (selection) => {
    let freezerInfo = "";
    let boxNumber = "";

    // Extract freezer/device info for "Storage Location (Freezer)"
    if (selection.device) {
      freezerInfo = selection.device.label;
      if (selection.shelf) {
        freezerInfo += ` - ${selection.shelf.label}`;
      }
      if (selection.rack) {
        freezerInfo += ` - ${selection.rack.label}`;
      }
    } else if (selection.room) {
      freezerInfo = selection.room.label;
    }

    // Extract box number for "Storage Location (Box No)"
    if (selection.box) {
      boxNumber = selection.box.label;
    }

    return { freezerInfo, boxNumber };
  };

  // Build storage path from selection
  const buildStoragePath = () => {
    const parts = [];
    if (storageSelection.room) parts.push(storageSelection.room.label);
    if (storageSelection.device) parts.push(storageSelection.device.label);
    if (storageSelection.shelf) parts.push(storageSelection.shelf.label);
    if (storageSelection.rack) parts.push(storageSelection.rack.label);
    if (storageSelection.box) parts.push(storageSelection.box.label);
    return parts.join(" > ");
  };

  // Get the most specific location selected
  const getLocationInfo = () => {
    if (storageSelection.box && storageSelection.box.id) {
      return { locationId: storageSelection.box.id, locationType: "box" };
    }
    if (storageSelection.rack && storageSelection.rack.id) {
      return { locationId: storageSelection.rack.id, locationType: "rack" };
    }
    if (storageSelection.shelf && storageSelection.shelf.id) {
      return { locationId: storageSelection.shelf.id, locationType: "shelf" };
    }
    if (storageSelection.device && storageSelection.device.id) {
      return { locationId: storageSelection.device.id, locationType: "device" };
    }
    if (storageSelection.room && storageSelection.room.id) {
      return { locationId: storageSelection.room.id, locationType: "room" };
    }
    return { locationId: null, locationType: null };
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

    // Validate storage selection - at minimum a room must be selected
    if (!storageSelection.room) {
      setError("Please select a storage location (at least a room)");
      return false;
    }

    return true;
  };

  const handleSave = async () => {
    if (!validate()) return;

    setSaving(true);
    setError(null);

    try {
      const locationInfo = getLocationInfo();
      const storagePath = buildStoragePath();

      if (isEdit) {
        // Update lot with unified storage location
        await InventoryLotAPI.update(lot.id, {
          ...formData,
          inventoryItem: formData.inventoryItem,
          unitSize: formData.unitSize,
          barcode: formData.barcode || null,
          initialQuantity: lot.initialQuantity,
          version: lot.version,
          // New unified storage fields
          locationId: locationInfo.locationId,
          locationType: locationInfo.locationType,
          storagePath: storagePath,
          // Clear legacy storage location
          storageLocation: null,
          // Lot-specific fields
          receivedBy: formData.receivedBy || null,
          specificStorageLocation: formData.storageLocation || null,
          storageBoxNumber: formData.storageBoxNumber || null,
        });
      } else {
        // Create new lot with unified storage location
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
          qcStatus: formData.qcStatus,
          status: formData.status,
          barcode: formData.barcode || null,
          // New unified storage fields
          locationId: locationInfo.locationId,
          locationType: locationInfo.locationType,
          storagePath: storagePath,
          // Lot-specific fields
          receivedBy: formData.receivedBy || null,
          specificStorageLocation: formData.storageLocation || null,
          storageBoxNumber: formData.storageBoxNumber || null,
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
    <Modal
      open={open}
      onRequestClose={onClose}
      onRequestSubmit={handleSave}
      modalHeading={intl.formatMessage({
        id: isEdit ? "lot.form.title.edit" : "lot.form.title.add",
        defaultMessage: isEdit ? "Edit Lot" : "Add New Lot",
      })}
      primaryButtonText={intl.formatMessage({
        id: "button.save",
        defaultMessage: "Save",
      })}
      secondaryButtonText={intl.formatMessage({
        id: "button.cancel",
        defaultMessage: "Cancel",
      })}
      primaryButtonDisabled={saving}
      size="lg"
    >
      <Stack gap={5}>
        {error && (
          <div style={{ color: "red", marginBottom: "1rem" }}>{error}</div>
        )}

        <Dropdown
          id="inventoryItem"
          titleText={
            <FormattedMessage
              id="lot.selectItem"
              defaultMessage="Catalog Item"
            />
          }
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
          labelText={
            <FormattedMessage id="lot.number" defaultMessage="Lot Number" />
          }
          value={formData.lotNumber}
          onChange={(e) => handleChange("lotNumber", e.target.value)}
          required
        />

        <NumberInput
          id="currentQuantity"
          label={
            <FormattedMessage
              id="lot.initialQuantity"
              defaultMessage="Initial Quantity"
            />
          }
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
            labelText={
              <FormattedMessage
                id="lot.expirationDate"
                defaultMessage="Expiration Date"
              />
            }
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
            labelText={
              <FormattedMessage
                id="lot.receiptDate"
                defaultMessage="Receipt Date"
              />
            }
            placeholder="mm/dd/yyyy"
          />
        </DatePicker>

        {/* Unified Storage Hierarchy Selector */}
        <div
          style={{
            backgroundColor: "#f4f4f4",
            padding: "1rem",
            borderRadius: "4px",
            border: "1px solid #e0e0e0",
          }}
        >
          <FormLabel
            style={{
              marginBottom: "0.75rem",
              display: "block",
              fontWeight: "600",
            }}
          >
            <FormattedMessage
              id="lot.selectLocation"
              defaultMessage="Storage Location"
            />
            <span style={{ color: "#da1e28" }}> *</span>
          </FormLabel>
          <p
            style={{
              fontSize: "0.75rem",
              color: "#525252",
              marginBottom: "1rem",
            }}
          >
            <FormattedMessage
              id="lot.selectLocation.helper"
              defaultMessage="Select the storage hierarchy where this lot will be stored. You can assign at any level (room, device, shelf, rack, or box)."
            />
          </p>
          <StorageHierarchySelector
            onSelectionChange={handleStorageSelectionChange}
            boxRequired={false}
            showPath={true}
          />
        </div>

        <Dropdown
          id="qcStatus"
          titleText={
            <FormattedMessage id="lot.qcStatus" defaultMessage="QC Status" />
          }
          label="Select QC status"
          items={qcStatusOptions}
          itemToString={(item) => (item ? item.text : "")}
          selectedItem={qcStatusOptions.find((s) => s.id === formData.qcStatus)}
          onChange={({ selectedItem }) =>
            handleChange("qcStatus", selectedItem.id)
          }
        />

        <Dropdown
          id="status"
          titleText={
            <FormattedMessage id="lot.status" defaultMessage="Status" />
          }
          label="Select status"
          items={statusOptions}
          itemToString={(item) => (item ? item.text : "")}
          selectedItem={statusOptions.find((s) => s.id === formData.status)}
          onChange={({ selectedItem }) =>
            handleChange("status", selectedItem.id)
          }
        />

        {/* Reagent-specific fields */}
        {formData.inventoryItem?.itemType === "REAGENT" && (
          <>
            <TextInput
              id="receivedBy"
              labelText="Received By"
              value={formData.receivedBy}
              onChange={(e) => handleChange("receivedBy", e.target.value)}
              placeholder="e.g., Dr. Smith, Lab Tech Johnson"
              helperText="Person who received this reagent lot"
            />

            <TextInput
              id="storageLocation"
              labelText="Storage Location (Freezer)"
              value={formData.storageLocation}
              readOnly
              placeholder="Auto-populated from storage hierarchy above"
              helperText="Auto-filled from selected storage hierarchy (device + shelf + rack)"
            />

            <TextInput
              id="storageBoxNumber"
              labelText="Storage Location (Box No)"
              value={formData.storageBoxNumber}
              readOnly
              placeholder="Auto-populated from storage hierarchy above"
              helperText="Auto-filled from selected box in storage hierarchy"
            />
          </>
        )}

        <TextInput
          id="barcode"
          labelText={
            <FormattedMessage id="lot.barcode" defaultMessage="Barcode" />
          }
          value={formData.barcode}
          onChange={(e) => handleChange("barcode", e.target.value)}
          placeholder="Optional"
        />
      </Stack>
    </Modal>
  );
};

export default LotEntryModal;
