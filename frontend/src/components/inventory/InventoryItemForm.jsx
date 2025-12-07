import React, { useState, useEffect } from "react";
import {
  Modal,
  TextInput,
  Dropdown,
  NumberInput,
  TextArea,
  FormLabel,
  Stack,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { InventoryItemAPI } from "./InventoryService";

const InventoryItemForm = ({ open, onClose, onSave, item = null }) => {
  const intl = useIntl();
  const isEdit = !!item;

  // Form state
  const [formData, setFormData] = useState({
    name: "",
    itemType: "REAGENT",
    category: "",
    manufacturer: "",
    units: "",
    minimumStockLevel: 0,
    reorderQuantity: 0,
    stabilityAfterOpening: 0,
    storageRequirements: "",
    compatibleAnalyzers: "",
    testsPerCartridge: 0,
    testsPerKit: 0,
    testTypes: "",
  });

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  // Item types
  const itemTypes = [
    { id: "REAGENT", text: "Reagent" },
    { id: "RDT", text: "RDT (Rapid Diagnostic Test)" },
    { id: "CARTRIDGE", text: "Analyzer Cartridge" },
  ];

  // Load item data if editing, reset if adding new
  useEffect(() => {
    if (item) {
      setFormData({
        name: item.name || "",
        itemType: item.itemType || "REAGENT",
        category: item.category || "",
        manufacturer: item.manufacturer || "",
        units: item.units || "",
        minimumStockLevel: item.minimumStockLevel || 0,
        reorderQuantity: item.reorderQuantity || 0,
        stabilityAfterOpening: item.stabilityAfterOpening || 0,
        storageRequirements: item.storageRequirements || "",
        compatibleAnalyzers: item.compatibleAnalyzers || "",
        testsPerCartridge: item.testsPerCartridge || 0,
        testsPerKit: item.testsPerKit || 0,
        testTypes: item.testTypes || "",
      });
    } else {
      // Reset to initial state when adding new item
      setFormData({
        name: "",
        itemType: "REAGENT",
        category: "",
        manufacturer: "",
        units: "",
        minimumStockLevel: 0,
        reorderQuantity: 0,
        stabilityAfterOpening: 0,
        storageRequirements: "",
        compatibleAnalyzers: "",
        testsPerCartridge: 0,
        testsPerKit: 0,
        testTypes: "",
      });
    }
  }, [item, open]);

  // Handle input changes
  const handleChange = (field, value) => {
    // Convert empty string or NaN to 0 for numeric fields
    const numericFields = [
      "minimumStockLevel",
      "reorderQuantity",
      "stabilityAfterOpening",
      "testsPerCartridge",
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
      if (isEdit) {
        await InventoryItemAPI.update(item.id, formData);
      } else {
        await InventoryItemAPI.create(formData);
      }
      onSave();
    } catch (err) {
      console.error("Error saving item:", err);
      setError(err.message || "Error saving catalog item");
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
          id="minimumStockLevel"
          label={<FormattedMessage id="catalog.item.minimumStock" />}
          value={formData.minimumStockLevel ?? 0}
          onChange={(e, { value }) =>
            handleChange("minimumStockLevel", value ?? 0)
          }
          min={0}
        />

        <NumberInput
          id="reorderQuantity"
          label={<FormattedMessage id="catalog.item.reorderQuantity" />}
          value={formData.reorderQuantity ?? 0}
          onChange={(e, { value }) =>
            handleChange("reorderQuantity", value ?? 0)
          }
          min={0}
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
          <>
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

            <NumberInput
              id="testsPerCartridge"
              label={<FormattedMessage id="catalog.item.testsPerCartridge" />}
              value={formData.testsPerCartridge ?? 0}
              onChange={(e, { value }) =>
                handleChange("testsPerCartridge", value ?? 0)
              }
              min={1}
            />
          </>
        )}

        {formData.itemType === "RDT" && (
          <>
            <NumberInput
              id="testsPerKit"
              label={<FormattedMessage id="catalog.item.testsPerKit" />}
              value={formData.testsPerKit ?? 0}
              onChange={(e, { value }) =>
                handleChange("testsPerKit", value ?? 0)
              }
              min={1}
              required
            />

            <TextInput
              id="testTypes"
              labelText={<FormattedMessage id="catalog.item.testTypes" />}
              value={formData.testTypes}
              onChange={(e) => handleChange("testTypes", e.target.value)}
              placeholder="e.g., Malaria, HIV, COVID-19"
            />
          </>
        )}
      </Stack>
    </Modal>
  );
};

export default InventoryItemForm;
