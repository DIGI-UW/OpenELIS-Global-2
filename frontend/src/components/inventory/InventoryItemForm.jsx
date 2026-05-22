import React, { useState, useEffect, useContext, useCallback } from "react";
import {
  Modal,
  TextInput,
  Dropdown,
  NumberInput,
  Stack,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../layout/Layout";
import { NotificationKinds } from "../common/CustomNotification";
import { InventoryItemAPI } from "./InventoryService";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { usePermissions } from "../../hooks/usePermissions";
import { inventorySaveRoles } from "../../security/rbacActions";
import {
  formatItemTypesForDropdown,
} from "./catalog/inventoryItemTypeLabels";
import { convertFromISODateTime } from "./catalog/inventoryDateUtils";
import {
  validateCatalogForm,
  buildCatalogPayload,
} from "./catalog/inventoryCatalogValidation";
import {
  formatUnitOptionsFromUomResponse,
  DEFAULT_EQUIPMENT_UNIT,
} from "./catalog/inventoryUnitOptions";
import CatalogFieldsReagent from "./catalog/CatalogFieldsReagent";
import CatalogFieldsCartridge from "./catalog/CatalogFieldsCartridge";
import CatalogFieldsEquipment from "./catalog/CatalogFieldsEquipment";
import CatalogFieldsRdt from "./catalog/CatalogFieldsRdt";
import CatalogFieldsKit from "./catalog/CatalogFieldsKit";
import CatalogFieldsEnzyme from "./catalog/CatalogFieldsEnzyme";

const emptyFormData = () => ({
  name: "",
  itemType: "REAGENT",
  category: "",
  manufacturer: "",
  units: "",
  lowStockThreshold: 0,
  projectName: "",
  stabilityAfterOpening: 0,
  dilutionNotes: "",
  storageRequirements: "",
  concentration: "",
  compatibleAnalyzers: "",
  calibrationRequired: "N",
  equipmentCondition: "functional",
  modelNumber: "",
  serialNumber: "",
  ahriTag: "",
  analyzerId: "",
  installationDate: "",
  lastServiceDate: "",
  lastMaintenanceDate: "",
  nextMaintenanceDate: "",
  testsPerKit: 0,
  individualTracking: "N",
  sourceOrganization: "",
  kitTestType: "",
  enzymeType: "",
});

const mapItemToFormData = (item) => ({
  ...emptyFormData(),
  name: item.name || "",
  itemType: item.itemType || "REAGENT",
  category: item.category || "",
  manufacturer: item.manufacturer || "",
  units: item.units || "",
  lowStockThreshold: item.lowStockThreshold || 0,
  projectName: item.projectName || "",
  stabilityAfterOpening: item.stabilityAfterOpening || 0,
  dilutionNotes: item.dilutionNotes || "",
  storageRequirements: item.storageRequirements || "",
  concentration: item.concentration || "",
  compatibleAnalyzers: item.compatibleAnalyzers || "",
  calibrationRequired: item.calibrationRequired || "N",
  equipmentCondition: item.equipmentCondition || "functional",
  modelNumber: item.modelNumber || "",
  serialNumber: item.serialNumber || "",
  ahriTag: item.ahriTag || "",
  analyzerId: item.analyzerId || "",
  installationDate: convertFromISODateTime(item.installationDate) || "",
  lastServiceDate: convertFromISODateTime(item.lastServiceDate) || "",
  lastMaintenanceDate: convertFromISODateTime(item.lastMaintenanceDate) || "",
  nextMaintenanceDate: convertFromISODateTime(item.nextMaintenanceDate) || "",
  testsPerKit: item.testsPerKit || 0,
  individualTracking: item.individualTracking || "N",
  sourceOrganization: item.sourceOrganization || "",
  kitTestType: item.kitTestType || "",
  enzymeType: item.enzymeType || "",
});

const InventoryItemForm = ({ open, onClose, onSave, item = null }) => {
  const intl = useIntl();
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const notify = useCallback(
    ({ kind, title, subtitle }) => {
      setNotificationVisible(true);
      addNotification({ kind, title, subtitle });
    },
    [addNotification, setNotificationVisible],
  );
  const isEdit = !!item;
  const { hasAnyRole } = usePermissions();
  const canSaveInventory = hasAnyRole(inventorySaveRoles);

  const [formData, setFormData] = useState(emptyFormData());
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [itemTypes, setItemTypes] = useState([]);
  const [unitOptions, setUnitOptions] = useState([]);
  const [projects, setProjects] = useState([]);
  const [analyzers, setAnalyzers] = useState([]);
  const [analyzersLoading, setAnalyzersLoading] = useState(false);
  const [showNewUnitModal, setShowNewUnitModal] = useState(false);
  const [newUnitName, setNewUnitName] = useState("");
  const [assignableDepartments, setAssignableDepartments] = useState([]);
  const [inventoryDepartmentId, setInventoryDepartmentId] = useState("");
  const [assignableDepartmentsLoading, setAssignableDepartmentsLoading] =
    useState(false);
  const [projectsLoading, setProjectsLoading] = useState(false);

  const hasUnrestrictedDepartmentAccess = useCallback(() => {
    const ud = userSessionDetails;
    if (!ud?.authenticated) {
      return false;
    }
    if (ud.roles?.includes("Global Administrator")) {
      return true;
    }
    const allLab = ud.userLabRolesMap?.AllLabUnits;
    return Array.isArray(allLab) && allLab.length > 0;
  }, [userSessionDetails]);

  useEffect(() => {
    const loadItemTypes = async () => {
      try {
        const types = await InventoryItemAPI.getItemTypes();
        setItemTypes(formatItemTypesForDropdown(types));
      } catch (err) {
        console.error("Error loading item types:", err);
        notify({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.error" }),
          subtitle: "Failed to load item types",
        });
      }
    };

    const loadUnitOptions = async () => {
      try {
        const units = await InventoryItemAPI.getUnitOptions();
        setUnitOptions(units);
      } catch {
        setUnitOptions(formatUnitOptionsFromUomResponse(null));
      }
    };

    loadItemTypes();
    loadUnitOptions();
  }, [notify, intl]);

  useEffect(() => {
    if (!open) {
      return;
    }
    setAnalyzersLoading(true);
    InventoryItemAPI.getLinkableAnalyzers()
      .then((list) => setAnalyzers(Array.isArray(list) ? list : []))
      .catch(() => setAnalyzers([]))
      .finally(() => setAnalyzersLoading(false));
  }, [open]);

  useEffect(() => {
    if (!open) {
      setAssignableDepartmentsLoading(false);
      return;
    }
    let cancelled = false;
    setAssignableDepartmentsLoading(true);
    InventoryItemAPI.getAssignableDepartments()
      .then((list) => {
        if (cancelled || !Array.isArray(list)) {
          return;
        }
        setAssignableDepartments(list);
        const itemDepartmentId = item?.departmentTestSectionId;
        const loginId = userSessionDetails?.loginLabUnitId;
        if (
          itemDepartmentId &&
          list.some((d) => String(d.id) === String(itemDepartmentId))
        ) {
          setInventoryDepartmentId(String(itemDepartmentId));
        } else if (
          loginId &&
          list.some((d) => String(d.id) === String(loginId))
        ) {
          setInventoryDepartmentId(String(loginId));
        } else if (list.length === 1) {
          setInventoryDepartmentId(String(list[0].id));
        } else {
          setInventoryDepartmentId("");
        }
      })
      .catch(() => {
        if (!cancelled) {
          setAssignableDepartments([]);
          setInventoryDepartmentId("");
        }
      })
      .finally(() => {
        if (!cancelled) {
          setAssignableDepartmentsLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [open, item, userSessionDetails]);

  useEffect(() => {
    if (!open) {
      return;
    }
    if (hasUnrestrictedDepartmentAccess() && !inventoryDepartmentId) {
      setProjects([]);
      setProjectsLoading(false);
      return;
    }
    let cancelled = false;
    setProjectsLoading(true);
    InventoryItemAPI.getLinkedProjects(inventoryDepartmentId || undefined)
      .then((list) => {
        if (!cancelled && Array.isArray(list)) {
          const projectRows = [...list];
          if (
            isEdit &&
            formData.projectName &&
            !projectRows.some(
              (project) =>
                String(project.id) === String(formData.projectName) ||
                project.value === formData.projectName,
            )
          ) {
            projectRows.unshift({
              id: String(formData.projectName),
              value: formData.projectName,
            });
          }
          setProjects(projectRows);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setProjects([]);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setProjectsLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [
    open,
    inventoryDepartmentId,
    isEdit,
    formData.projectName,
    hasUnrestrictedDepartmentAccess,
  ]);

  useEffect(() => {
    if (item) {
      setFormData(mapItemToFormData(item));
    } else {
      setFormData(emptyFormData());
    }
  }, [item, open]);

  const handleCreateNewUnit = async () => {
    if (!newUnitName.trim()) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: "Unit name cannot be empty",
      });
      return;
    }

    try {
      await InventoryItemAPI.createUnitOfMeasure(newUnitName.trim());
      const units = await InventoryItemAPI.getUnitOptions();
      setUnitOptions(units);
      const created = units.find(
        (u) => u.id === newUnitName.trim() || u.text === newUnitName.trim(),
      );
      handleChange("units", created?.id ?? newUnitName.trim());
      setShowNewUnitModal(false);
      setNewUnitName("");
      notify({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.success" }),
        subtitle: `Unit "${newUnitName.trim()}" created successfully`,
      });
    } catch (err) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: err.message || "Failed to create new unit",
      });
    }
  };

  const handleChange = (field, value) => {
    const numericFields = [
      "lowStockThreshold",
      "stabilityAfterOpening",
      "testsPerKit",
    ];
    let processedValue = value;
    if (numericFields.includes(field)) {
      if (value === "" || value === null || value === undefined || isNaN(value)) {
        processedValue = 0;
      }
    }

    setFormData((prev) => {
      if (prev[field] === processedValue) {
        return prev;
      }
      return { ...prev, [field]: processedValue };
    });
    setError(null);
  };

  const handleSave = async () => {
    const validationError = validateCatalogForm(formData, {
      inventoryDepartmentId,
      assignableDepartmentsLoading,
      assignableDepartments,
    });
    if (validationError) {
      setError(validationError);
      return;
    }

    setSaving(true);
    setError(null);

    try {
      const sanitizedData = buildCatalogPayload(
        formData,
        inventoryDepartmentId,
      );
      if (isEdit) {
        await InventoryItemAPI.update(item.id, sanitizedData);
      } else {
        await InventoryItemAPI.create(sanitizedData);
      }
      setSaving(false);
      onSave();
    } catch (err) {
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

  const renderTypeFields = () => {
    switch (formData.itemType) {
      case "REAGENT":
        return (
          <CatalogFieldsReagent formData={formData} onChange={handleChange} />
        );
      case "CARTRIDGE":
        return (
          <CatalogFieldsCartridge formData={formData} onChange={handleChange} />
        );
      case "EQUIPMENT":
        return (
          <CatalogFieldsEquipment
            formData={formData}
            onChange={handleChange}
            analyzers={analyzers}
            analyzersLoading={analyzersLoading}
          />
        );
      case "RDT":
        return <CatalogFieldsRdt formData={formData} onChange={handleChange} />;
      case "HIV_KIT":
      case "SYPHILIS_KIT":
        return <CatalogFieldsKit formData={formData} onChange={handleChange} />;
      case "ENZYME":
        return (
          <CatalogFieldsEnzyme formData={formData} onChange={handleChange} />
        );
      default:
        return null;
    }
  };

  return (
    <>
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
        primaryButtonDisabled={
          saving || !canSaveInventory || (!isEdit && assignableDepartmentsLoading)
        }
        size="md"
      >
        <Stack gap={5}>
          {error && (
            <div style={{ color: "red", marginBottom: "1rem" }}>{error}</div>
          )}

          {assignableDepartmentsLoading ? (
            <p>Loading departments…</p>
          ) : assignableDepartments.length > 0 ? (
            <Dropdown
              id="inventory-catalog-department"
              titleText="Department (lab unit)"
              label="Select department"
              items={assignableDepartments}
              selectedItem={
                assignableDepartments.find(
                  (d) => String(d.id) === String(inventoryDepartmentId),
                ) || null
              }
              onChange={({ selectedItem }) => {
                const nextDepartmentId = selectedItem
                  ? String(selectedItem.id)
                  : "";
                const departmentChanged =
                  nextDepartmentId !== String(inventoryDepartmentId || "");
                setInventoryDepartmentId(nextDepartmentId);
                if (departmentChanged && formData.projectName) {
                  handleChange("projectName", "");
                }
              }}
              itemToString={(item) => (item ? item.value || item.id : "")}
              helperText={
                hasUnrestrictedDepartmentAccess()
                  ? "Choose the owning department for this catalog item."
                  : "This catalog item will only be visible and manageable within your active department."
              }
              disabled={!hasUnrestrictedDepartmentAccess()}
              required
            />
          ) : null}

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
            onChange={({ selectedItem }) => {
              const nextType = selectedItem?.id || "REAGENT";
              handleChange("itemType", nextType);
              if (nextType === "EQUIPMENT") {
                handleChange("units", DEFAULT_EQUIPMENT_UNIT);
              }
            }}
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

          <Dropdown
            id="projectName"
            titleText="Linked notebook / project (optional)"
            label={
              hasUnrestrictedDepartmentAccess() && !inventoryDepartmentId
                ? "Select department first"
                : projectsLoading
                  ? "Loading linked notebooks..."
                  : "Select a linked notebook / project"
            }
            items={projects}
            itemToString={(item) => (item ? item.value : "")}
            selectedItem={
              projects.find(
                (p) =>
                  String(p.id) === String(formData.projectName) ||
                  p.value === formData.projectName,
              ) || null
            }
            onChange={({ selectedItem }) =>
              handleChange("projectName", String(selectedItem?.id || ""))
            }
            disabled={
              (hasUnrestrictedDepartmentAccess() && !inventoryDepartmentId) ||
              projectsLoading
            }
          />

          {formData.itemType !== "EQUIPMENT" && (
            <Dropdown
              id="units"
              titleText={intl.formatMessage({ id: "catalog.item.units" })}
              label="Select a unit"
              items={unitOptions}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={
                unitOptions.find(
                  (u) =>
                    u.id === formData.units ||
                    u.text === formData.units,
                ) || null
              }
              onChange={({ selectedItem }) => {
                if (selectedItem?.id === "__add_new__") {
                  setShowNewUnitModal(true);
                } else {
                  handleChange("units", selectedItem?.id || "");
                }
              }}
            />
          )}

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

          {renderTypeFields()}
        </Stack>
      </Modal>

      <Modal
        open={showNewUnitModal}
        onRequestClose={() => {
          setShowNewUnitModal(false);
          setNewUnitName("");
        }}
        modalHeading="Add New Unit of Measure"
        primaryButtonText="Create Unit"
        secondaryButtonText="Cancel"
        onRequestSubmit={handleCreateNewUnit}
        size="sm"
      >
        <TextInput
          id="newUnitName"
          labelText="Unit Name"
          placeholder="e.g., mg, liters, vials"
          value={newUnitName}
          onChange={(e) => setNewUnitName(e.target.value)}
        />
      </Modal>
    </>
  );
};

export default InventoryItemForm;
