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
  const [itemsLoading, setItemsLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  const [locationModalOpen, setLocationModalOpen] = useState(false);

  const qcStatusOptions = [
    {
      id: "PENDING",
      text: intl.formatMessage({ id: "lot.qcStatus.pending" }),
    },
    { id: "PASSED", text: intl.formatMessage({ id: "lot.qcStatus.passed" }) },
    { id: "FAILED", text: intl.formatMessage({ id: "lot.qcStatus.failed" }) },
    {
      id: "QUARANTINED",
      text: intl.formatMessage({ id: "lot.qcStatus.quarantined" }),
    },
  ];

  const statusOptions = [
    { id: "ACTIVE", text: intl.formatMessage({ id: "lot.status.active" }) },
    { id: "IN_USE", text: intl.formatMessage({ id: "lot.status.inUse" }) },
    {
      id: "QUARANTINED",
      text: intl.formatMessage({ id: "lot.status.quarantined" }),
    },
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
    setItemsLoading(true);
    try {
      const allItems = await InventoryItemAPI.getAll({ isActive: true });
      console.log("LotEntryModal: fetchItems response:", allItems);

      let validItems = [];
      if (Array.isArray(allItems)) {
        validItems = allItems;
      } else if (allItems && Array.isArray(allItems.items)) {
        validItems = allItems.items;
      } else {
        console.warn(
          "LotEntryModal: Unexpected items response format",
          allItems,
        );
      }

      setItems(
        validItems.map((item) => ({
          id: item.id,
          text: `${item.name} (${item.itemType})`,
          item: item,
        })),
      );
      if (validItems.length === 0) {
        console.warn("LotEntryModal: No items found in response");
      }
    } catch (err) {
      console.error("Error fetching items:", err);
      setError(
        intl.formatMessage(
          { id: "lot.error.fetchItems" },
          { error: err.message },
        ),
      );
      setItems([]);
    } finally {
      setItemsLoading(false);
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
    setFormData((prev) => ({ ...prev, [field]: value }));
    setError(null);
  };

  const validate = () => {
    if (!formData.inventoryItem) {
      setError(intl.formatMessage({ id: "lot.validation.itemRequired" }));
      return false;
    }

    if (!formData.lotNumber?.trim()) {
      setError(intl.formatMessage({ id: "lot.validation.lotNumberRequired" }));
      return false;
    }

    if (!formData.currentQuantity || formData.currentQuantity <= 0) {
      setError(intl.formatMessage({ id: "lot.validation.quantityInvalid" }));
      return false;
    }

    if (!formData.storageLocation) {
      setError(intl.formatMessage({ id: "lot.validation.locationRequired" }));
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
      setError(
        intl.formatMessage(
          { id: "lot.save.error" },
          { error: err.message || "" },
        ),
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <Modal
        open={open && !locationModalOpen}
        onRequestClose={onClose}
        onRequestSubmit={handleSave}
        modalHeading={intl.formatMessage({
          id: isEdit ? "lot.form.title.edit" : "lot.form.title.add",
        })}
        primaryButtonText={intl.formatMessage({ id: "button.save" })}
        secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
        primaryButtonDisabled={saving}
        size="md"
        hasScrollingContent
      >
        <Stack gap={5}>
          {error && (
            <div style={{ color: "red", marginBottom: "1rem" }}>{error}</div>
          )}

          <Dropdown
            id="inventoryItem"
            titleText={<FormattedMessage id="lot.selectItem" />}
            label={
              itemsLoading
                ? intl.formatMessage({ id: "lot.selectItem.loading" })
                : items.length === 0
                  ? intl.formatMessage({ id: "lot.selectItem.noItems" })
                  : intl.formatMessage({ id: "lot.selectItem.placeholder" })
            }
            items={items}
            itemToString={(item) => (item ? item.text : "")}
            selectedItem={
              formData.inventoryItem
                ? items.find((i) => i.id === formData.inventoryItem.id)
                : null
            }
            onChange={({ selectedItem }) =>
              handleChange("inventoryItem", selectedItem?.item)
            }
            required
            disabled={isEdit || itemsLoading}
            warn={!itemsLoading && items.length === 0}
            warnText={intl.formatMessage({
              id: "lot.selectItem.warning.noItems",
            })}
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
              placeholder={intl.formatMessage({ id: "lot.date.placeholder" })}
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
              placeholder={intl.formatMessage({ id: "lot.date.placeholder" })}
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
              label={intl.formatMessage({
                id: "lot.selectLocation.placeholder",
              })}
              items={locations}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={
                formData.storageLocation
                  ? locations.find((l) => l.id === formData.storageLocation.id)
                  : null
              }
              onChange={({ selectedItem }) =>
                handleChange("storageLocation", selectedItem?.location)
              }
              required
            />
          </div>

          <Dropdown
            id="qcStatus"
            titleText={<FormattedMessage id="lot.qcStatus" />}
            label={intl.formatMessage({ id: "lot.selectQCStatus.placeholder" })}
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
            label={intl.formatMessage({ id: "lot.selectStatus.placeholder" })}
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
            placeholder={intl.formatMessage({ id: "label.optional" })}
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
