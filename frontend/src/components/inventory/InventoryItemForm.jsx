import React, { useState, useEffect, useContext, useCallback } from "react";
import {
  Modal,
  TextInput,
  Dropdown,
  NumberInput,
  TextArea,
  Stack,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../layout/Layout";
import { NotificationKinds } from "../common/CustomNotification";
import { InventoryItemAPI } from "./InventoryService";

const InventoryItemForm = ({ open, onClose, onSave, item = null }) => {
  const intl = useIntl();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const notify = useCallback(
    ({ kind, title, subtitle }) => {
      setNotificationVisible(true);
      addNotification({
        kind,
        title,
        subtitle,
      });
    },
    [addNotification, setNotificationVisible],
  );
  const isEdit = !!item;

  // Form state
  const [formData, setFormData] = useState({
    code: "",
    name: "",
    itemType: "REAGENT",
    category: "",
    manufacturer: "",
    units: "",
    lowStockThreshold: 0,
    stabilityAfterOpening: 0,
    storageRequirements: "",
    compatibleAnalyzers: "",
    testsPerKit: 0,
  });

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [itemTypes, setItemTypes] = useState([]);

  // Load item types from backend
  useEffect(() => {
    const loadItemTypes = async () => {
      try {
        const types = await InventoryItemAPI.getItemTypes();
        const formattedTypes = types.map((type) => ({
          id: type.code,
          text: type.label,
        }));
        setItemTypes(formattedTypes);
      } catch (err) {
        console.error("Error loading item types:", err);
        notify({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.error" }),
          subtitle: "Failed to load item types",
        });
      }
    };
    loadItemTypes();
  }, [notify, intl]);

  // Load item data if editing, reset if adding new
  useEffect(() => {
    if (item) {
      setFormData({
        code: item.id || "",
        name: item.name || "",
        itemType: item.itemType || "REAGENT",
        category: item.category || "",
        manufacturer: item.manufacturer || "",
        units: item.units || "",
        lowStockThreshold: item.lowStockThreshold || 0,
        stabilityAfterOpening: item.stabilityAfterOpening || 0,
        storageRequirements: item.storageRequirements || "",
        compatibleAnalyzers: item.compatibleAnalyzers || "",
        testsPerKit: item.testsPerKit || 0,
      });
    } else {
      // Reset to initial state when adding new item
      setFormData({
        code: "",
        name: "",
        itemType: "REAGENT",
        category: "",
        manufacturer: "",
        units: "",
        lowStockThreshold: 0,
        stabilityAfterOpening: 0,
        storageRequirements: "",
        compatibleAnalyzers: "",
        testsPerKit: 0,
      });
    }
  }, [item, open]);

  // Handle input changes
  const handleChange = (field, value) => {
    // Convert empty string or NaN to 0 for numeric fields
    const numericFields = [
      "lowStockThreshold",
      "stabilityAfterOpening",
      "testsPerKit",
    ];

    let processedValue = value;
    if (numericFields.includes(field)) {
      if (value === "" || value === null || value === undefined) {
        processedValue = 0;
      } else if (isNaN(value)) {
        processedValue = 0;
      }
    }

    setFormData((prev) => {
      // Prevent unnecessary state updates if value hasn't changed
      if (prev[field] === processedValue) {
        return prev;
      }
      return { ...prev, [field]: processedValue };
    });
    setError(null);
  };

  // Validate form
  const validate = () => {
    if (!formData.name?.trim()) {
      setError("Item name is required");
      return false;
    }

    if (!formData.itemType) {
      setError("Item type is required");
      return false;
    }

    // Type-specific validation
    if (formData.itemType === "REAGENT" && !formData.stabilityAfterOpening) {
      setError("Stability after opening is required for reagents");
      return false;
    }

    if (
      formData.itemType === "CARTRIDGE" &&
      !formData.compatibleAnalyzers?.trim()
    ) {
      setError("Compatible analyzers are required for cartridges");
      return false;
    }

    if (formData.itemType === "RDT" && !formData.testsPerKit) {
      setError("Tests per kit is required for RDTs");
      return false;
    }

    return true;
  };

  // Handle save
  const handleSave = async () => {
    if (!validate()) return;

    setSaving(true);
    setError(null);

    try {
      // Build sanitized data with only type-relevant fields
      const sanitizedData = {
        name: formData.name,
        itemType: formData.itemType,
        category: formData.category,
        manufacturer: formData.manufacturer,
        units: formData.units,
        lowStockThreshold: Number(formData.lowStockThreshold) || 0,
      };

      // Add type-specific fields only for relevant item types
      if (formData.itemType === "REAGENT") {
        sanitizedData.stabilityAfterOpening =
          Number(formData.stabilityAfterOpening) || 0;
        sanitizedData.storageRequirements = formData.storageRequirements;
      } else if (formData.itemType === "CARTRIDGE") {
        sanitizedData.compatibleAnalyzers = formData.compatibleAnalyzers;
      } else if (formData.itemType === "RDT") {
        sanitizedData.testsPerKit = Number(formData.testsPerKit) || 0;
      }

      if (isEdit) {
        await InventoryItemAPI.update(item.id, sanitizedData);
      } else {
        // Optional user-supplied code; blank means the server auto-generates
        // one from the name. Locked once saved, so never sent on update.
        sanitizedData.id = formData.code?.trim() || null;
        await InventoryItemAPI.create(sanitizedData);
      }
      setSaving(false);
      onSave();
    } catch (err) {
      console.error("Error saving item:", err);
      const errorMessage = err.message || "Error saving catalog item";
      setError(errorMessage);
      setSaving(false);
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: errorMessage,
      });
    }
  };

  return (
    <Modal
      open={open}
      onRequestClose={onClose}
      onRequestSubmit={handleSave}
      modalHeading={intl.formatMessage({
        id: isEdit
          ? "catalog.item.form.title.edit"
          : "catalog.item.form.title.add",
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

        <TextInput
          id="name"
          labelText={<FormattedMessage id="catalog.item.name" />}
          value={formData.name}
          onChange={(e) => handleChange("name", e.target.value)}
          required
        />

        <TextInput
          id="code"
          labelText={
            <FormattedMessage id="catalog.item.code" defaultMessage="Code" />
          }
          value={formData.code}
          disabled={isEdit}
          placeholder={
            isEdit
              ? ""
              : intl.formatMessage({
                  id: "catalog.item.code.placeholder",
                  defaultMessage: "Leave blank to auto-generate from name",
                })
          }
          helperText={
            isEdit
              ? intl.formatMessage({
                  id: "catalog.item.code.locked",
                  defaultMessage:
                    "Code is locked once saved so integrations and existing references keep working.",
                })
              : intl.formatMessage({
                  id: "catalog.item.code.hint",
                  defaultMessage:
                    "Stable identifier used by integrations. Leave blank and we'll generate one from the name.",
                })
          }
          onChange={(e) => handleChange("code", e.target.value.toUpperCase())}
        />

        <Dropdown
          id="itemType"
          titleText={<FormattedMessage id="catalog.item.type" />}
          label="Select item type"
          items={itemTypes}
          itemToString={(item) => (item ? item.text : "")}
          selectedItem={itemTypes.find((t) => t.id === formData.itemType)}
          onChange={({ selectedItem }) =>
            handleChange("itemType", selectedItem.id)
          }
          required
        />

        <TextInput
          id="category"
          labelText={<FormattedMessage id="catalog.item.category" />}
          value={formData.category}
          onChange={(e) => handleChange("category", e.target.value)}
        />

        <TextInput
          id="manufacturer"
          labelText={<FormattedMessage id="catalog.item.manufacturer" />}
          value={formData.manufacturer}
          onChange={(e) => handleChange("manufacturer", e.target.value)}
        />

        <TextInput
          id="units"
          labelText={<FormattedMessage id="catalog.item.units" />}
          value={formData.units}
          onChange={(e) => handleChange("units", e.target.value)}
          placeholder="e.g., mL, tests, kits"
        />

        <NumberInput
          id="lowStockThreshold"
          label={<FormattedMessage id="catalog.item.lowStockThreshold" />}
          value={formData.lowStockThreshold ?? 0}
          onChange={(e, { value }) =>
            handleChange("lowStockThreshold", value ?? 0)
          }
          min={0}
          max={999999}
        />

        {/* Type-specific fields */}
        {formData.itemType === "REAGENT" && (
          <>
            <NumberInput
              id="stabilityAfterOpening"
              label={
                <FormattedMessage id="catalog.item.stabilityAfterOpening" />
              }
              value={formData.stabilityAfterOpening ?? 0}
              onChange={(e, { value }) =>
                handleChange("stabilityAfterOpening", value ?? 0)
              }
              min={0}
              max={365}
              required
            />

            <TextArea
              id="storageRequirements"
              labelText={
                <FormattedMessage id="catalog.item.storageRequirements" />
              }
              value={formData.storageRequirements}
              onChange={(e) =>
                handleChange("storageRequirements", e.target.value)
              }
              placeholder="e.g., Store at 2-8°C, protect from light"
            />
          </>
        )}

        {formData.itemType === "CARTRIDGE" && (
          <TextInput
            id="compatibleAnalyzers"
            labelText={
              <FormattedMessage id="catalog.item.compatibleAnalyzers" />
            }
            value={formData.compatibleAnalyzers}
            onChange={(e) =>
              handleChange("compatibleAnalyzers", e.target.value)
            }
            placeholder="e.g., GeneXpert, Cobas"
            required
          />
        )}

        {formData.itemType === "RDT" && (
          <NumberInput
            id="testsPerKit"
            label={<FormattedMessage id="catalog.item.testsPerKit" />}
            value={formData.testsPerKit ?? 0}
            onChange={(e, { value }) => handleChange("testsPerKit", value ?? 0)}
            min={1}
            max={1000}
            required
          />
        )}
      </Stack>
    </Modal>
  );
};

export default InventoryItemForm;
