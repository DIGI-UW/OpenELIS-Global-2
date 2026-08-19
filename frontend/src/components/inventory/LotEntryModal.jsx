import React, { useState, useEffect, useRef } from "react";
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
import { FormattedMessage, useIntl } from "react-intl";
import {
  InventoryItemAPI,
  InventoryLotAPI,
  InventoryManagementAPI,
  InventoryLotStorageAPI,
} from "./InventoryService";
import LocationPickerModal from "../storage/LocationPicker/LocationPickerModal";
import {
  selectionToHierarchicalPath,
  getDeepestLocationSelection,
  positionToCoordinate,
} from "../storage/LocationPicker/locationSelectionMapper";

const LotEntryModal = ({ open, onClose, onSave, lot = null }) => {
  const intl = useIntl();
  const isEdit = !!lot;

  // Guards setState after awaits — fetches and saves can resolve after the
  // parent has unmounted this modal (e.g. onSave() closes it before the
  // finally block runs).
  const isMountedRef = useRef(true);
  useEffect(() => {
    isMountedRef.current = true;
    return () => {
      isMountedRef.current = false;
    };
  }, []);

  const [formData, setFormData] = useState({
    inventoryItem: null,
    lotNumber: "",
    currentQuantity: 0,
    expirationDate: null,
    receiptDate: new Date(),
    qcStatus: "PENDING",
    status: "ACTIVE",
    barcode: "",
  });

  const [items, setItems] = useState([]);

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [locationError, setLocationError] = useState(null);

  const [locationPickerOpen, setLocationPickerOpen] = useState(false);
  // Edit mode: the lot's currently-assigned location, shaped for
  // LocationPickerModal's currentLocation prop; null when unassigned.
  const [currentLocation, setCurrentLocation] = useState(null);
  // Create mode: a location picked before the lot exists in the DB,
  // applied right after the lot is saved (see handleSave).
  const [pendingAssignment, setPendingAssignment] = useState(null);

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
        expirationDate: lot.expirationDate
          ? new Date(lot.expirationDate)
          : null,
        receiptDate: lot.receiptDate ? new Date(lot.receiptDate) : new Date(),
        qcStatus: lot.qcStatus || "PENDING",
        status: lot.status || "ACTIVE",
        barcode: lot.barcode || "",
      });
      fetchCurrentLocation(lot.id);
    }
  }, [lot]);

  const fetchItems = async () => {
    try {
      const allItems = await InventoryItemAPI.getAll({ isActive: true });
      if (!isMountedRef.current) return;
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
      if (isMountedRef.current) setItems([]);
    }
  };

  const fetchCurrentLocation = async (lotId) => {
    try {
      const location = await InventoryLotStorageAPI.getLocation(lotId);
      if (!isMountedRef.current) return;
      if (location && location.hierarchicalPath) {
        setCurrentLocation({
          selection: {},
          hierarchicalPath: location.hierarchicalPath,
          position: location.positionCoordinate
            ? { mode: "text", value: location.positionCoordinate }
            : null,
        });
      } else {
        setCurrentLocation(null);
      }
    } catch (err) {
      console.error("Error fetching lot location:", err);
      if (isMountedRef.current) setCurrentLocation(null);
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

    if (isEdit && !formData.lotNumber?.trim()) {
      setError("Lot number is required");
      return false;
    }

    if (!formData.currentQuantity || formData.currentQuantity <= 0) {
      setError("Quantity must be greater than 0");
      return false;
    }

    if (!isEdit && !pendingAssignment) {
      setError("Please assign a storage location");
      return false;
    }

    return true;
  };

  const buildLocationPayload = (inventoryLotId, assignment) => {
    const deepest = getDeepestLocationSelection(assignment.selection, {
      requireAssignable: true,
    });
    return {
      inventoryLotId: String(inventoryLotId),
      locationId: deepest ? String(deepest.value.id) : null,
      locationType: deepest ? deepest.type : null,
      positionCoordinate: positionToCoordinate(assignment.position, {
        emptyValue: null,
      }),
      notes: assignment.notes || "",
    };
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
          initialQuantity: lot.initialQuantity,
          version: lot.version,
          // barcode is UNIQUE: blank has to be null, or a second blank collides
          barcode: formData.barcode?.trim() || null,
        });
      } else {
        const savedLot = await InventoryManagementAPI.receive({
          inventoryItem: { id: formData.inventoryItem.id },
          // Leave blank to let the server auto-generate one from the item
          // code + today's date.
          lotNumber: formData.lotNumber?.trim() || null,
          currentQuantity: formData.currentQuantity,
          initialQuantity: formData.currentQuantity,
          expirationDate: formData.expirationDate
            ? formData.expirationDate.toISOString()
            : null,
          receiptDate: formData.receiptDate.toISOString(),
          qcStatus: formData.qcStatus,
          status: formData.status,
          barcode: formData.barcode?.trim() || null,
        });

        if (pendingAssignment && savedLot?.id) {
          await InventoryLotStorageAPI.assignLocation(
            buildLocationPayload(savedLot.id, pendingAssignment),
          );
        }
      }
      onSave();
    } catch (err) {
      console.error("Error saving lot:", err);
      if (isMountedRef.current) setError(err.message || "Error saving lot");
    } finally {
      // onSave() above may have unmounted this modal already.
      if (isMountedRef.current) setSaving(false);
    }
  };

  const handleLocationConfirm = async ({ selection, position, notes }) => {
    if (!isEdit) {
      // Lot doesn't exist yet — defer the assignment call until handleSave.
      setPendingAssignment({ selection, position, notes });
      setLocationPickerOpen(false);
      return;
    }

    setLocationError(null);
    try {
      const payload = buildLocationPayload(lot.id, {
        selection,
        position,
        notes,
      });
      if (currentLocation) {
        await InventoryLotStorageAPI.moveLocation({
          ...payload,
          reason: notes || "",
        });
      } else {
        await InventoryLotStorageAPI.assignLocation(payload);
      }
      await fetchCurrentLocation(lot.id);
      if (isMountedRef.current) setLocationPickerOpen(false);
    } catch (err) {
      console.error("Error assigning lot location:", err);
      if (isMountedRef.current)
        setLocationError(err.message || "Error assigning storage location");
    }
  };

  const locationSummary = isEdit
    ? currentLocation?.hierarchicalPath || ""
    : pendingAssignment
      ? selectionToHierarchicalPath(pendingAssignment.selection)
      : "";

  return (
    <>
      <Modal
        open={open && !locationPickerOpen}
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
                ? (items.find((i) => i.id === formData.inventoryItem.id) ??
                  null)
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
            required={isEdit}
            placeholder={
              isEdit
                ? undefined
                : intl.formatMessage({
                    id: "lot.number.placeholder",
                    defaultMessage: "Leave blank to auto-generate",
                  })
            }
            helperText={
              isEdit
                ? undefined
                : intl.formatMessage({
                    id: "lot.number.hint",
                    defaultMessage:
                      "Stable identifier for this lot. Leave blank and we'll generate one from the item code and today's date.",
                  })
            }
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
                onClick={() => {
                  setLocationError(null);
                  setLocationPickerOpen(true);
                }}
              >
                <FormattedMessage
                  id={
                    locationSummary
                      ? "storage.location.move"
                      : "storage.location.assign"
                  }
                  defaultMessage={
                    locationSummary
                      ? "Move storage location"
                      : "Assign storage location"
                  }
                />
              </Button>
            </div>
            <div>
              {locationSummary || (
                <FormattedMessage
                  id="storage.location.notAssigned"
                  defaultMessage="Not assigned"
                />
              )}
            </div>
            {locationError && (
              <div style={{ color: "red" }}>{locationError}</div>
            )}
          </div>

          <Dropdown
            id="qcStatus"
            titleText={<FormattedMessage id="lot.qcStatus" />}
            label="Select QC status"
            items={qcStatusOptions}
            itemToString={(item) => (item ? item.text : "")}
            selectedItem={
              qcStatusOptions.find((s) => s.id === formData.qcStatus) ?? null
            }
            onChange={({ selectedItem }) =>
              handleChange("qcStatus", selectedItem.id)
            }
            helperText={intl.formatMessage({ id: "lot.qcStatus.hint" })}
          />

          <Dropdown
            id="status"
            titleText={<FormattedMessage id="lot.status" />}
            label="Select status"
            items={statusOptions}
            itemToString={(item) => (item ? item.text : "")}
            selectedItem={
              statusOptions.find((s) => s.id === formData.status) ?? null
            }
            onChange={({ selectedItem }) =>
              handleChange("status", selectedItem.id)
            }
          />

          <TextInput
            id="barcode"
            labelText={<FormattedMessage id="lot.barcode" />}
            value={formData.barcode}
            disabled={isEdit}
            onChange={(e) => handleChange("barcode", e.target.value)}
            placeholder={
              isEdit
                ? ""
                : intl.formatMessage({
                    id: "lot.barcode.placeholder",
                  })
            }
            helperText={
              isEdit
                ? intl.formatMessage({ id: "lot.barcode.locked" })
                : intl.formatMessage({ id: "lot.barcode.hint" })
            }
          />
        </Stack>
      </Modal>

      <LocationPickerModal
        isOpen={locationPickerOpen}
        occupantType="INVENTORY_LOT"
        occupant={{
          label: formData.lotNumber,
          type: formData.inventoryItem?.name || "",
          status: formData.status,
        }}
        currentLocation={isEdit ? currentLocation : null}
        onConfirm={handleLocationConfirm}
        onCancel={() => setLocationPickerOpen(false)}
      />
    </>
  );
};

export default LotEntryModal;
