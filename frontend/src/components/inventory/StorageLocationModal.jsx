import React, { useState, useEffect, useCallback } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  Form,
  Stack,
  TextInput,
  Select,
  SelectItem,
  NumberInput,
  TextArea,
  Button,
  Tree,
  TreeNode,
  InlineNotification,
  InlineLoading,
  IconButton,
  OverflowMenu,
  OverflowMenuItem,
} from "@carbon/react";
import {
  Add,
  Edit,
  TrashCan,
  ChevronRight,
  Folder,
  FolderOpen,
  Temperature,
  Location as LocationIcon,
  ChevronDown,
} from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { StorageLocationAPI } from "./InventoryService";
import "./StorageLocationModal.css";

const LOCATION_TYPE_OPTIONS = [
  { value: "ROOM", label: "Room", icon: "🏢" },
  { value: "REFRIGERATOR", label: "Refrigerator", icon: "❄️" },
  { value: "FREEZER", label: "Freezer", icon: "🧊" },
  { value: "CABINET", label: "Cabinet", icon: "🗄️" },
  { value: "SHELF", label: "Shelf", icon: "📚" },
  { value: "DRAWER", label: "Drawer", icon: "🗃️" },
];

// Hierarchy rules: child type -> allowed parent types
const HIERARCHY_RULES = {
  ROOM: [], // No parent allowed (root level)
  REFRIGERATOR: ["ROOM"],
  FREEZER: ["ROOM"],
  CABINET: ["ROOM"],
  SHELF: ["REFRIGERATOR", "FREEZER", "CABINET"],
  DRAWER: ["REFRIGERATOR", "FREEZER", "CABINET"],
};

const INITIAL_FORM_DATA = {
  name: "",
  locationCode: "",
  locationType: "ROOM",
  description: "",
  temperatureMin: 2,
  temperatureMax: 8,
  parentLocation: null,
};

/**
 * StorageLocationModal - Tree-based hierarchical location management
 *
 * Features:
 * - Interactive tree view showing full location hierarchy
 * - Drag-and-drop to reorganize (with validation)
 * - Right-click context menu (Edit, Add Child, Delete)
 * - Add child button on each tree node
 * - Split view: tree on left, form on right
 * - Visual hierarchy with icons and indentation
 * - Smart parent selection based on location type
 */
export default function StorageLocationModal({ isOpen, onClose, onSubmit }) {
  const intl = useIntl();

  // Tree state
  const [treeData, setTreeData] = useState([]);
  const [expandedNodes, setExpandedNodes] = useState(new Set());
  const [selectedNode, setSelectedNode] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Form state
  const [formData, setFormData] = useState(INITIAL_FORM_DATA);
  const [formMode, setFormMode] = useState("view"); // "view" | "create" | "edit"
  const [availableParents, setAvailableParents] = useState([]);

  // Drag and drop state
  const [draggedNode, setDraggedNode] = useState(null);
  const [dragOverNode, setDragOverNode] = useState(null);

  // Fetch and build tree structure
  const loadTreeData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const allLocations = await StorageLocationAPI.getAll();
      const tree = buildTreeStructure(allLocations);
      setTreeData(tree);

      // Auto-expand root nodes
      const rootIds = tree.map((node) => node.id);
      setExpandedNodes(new Set(rootIds));
    } catch (err) {
      console.error("Error loading tree:", err);
      setError("Failed to load storage locations");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (isOpen) {
      loadTreeData();
      // Always start in create mode for better UX
      setFormMode("create");
      setSelectedNode(null);
      setFormData(INITIAL_FORM_DATA);
    }
  }, [isOpen, loadTreeData]);

  // Build hierarchical tree structure from flat array
  const buildTreeStructure = (locations) => {
    const locationMap = {};
    const roots = [];

    // Create map of all locations
    locations.forEach((loc) => {
      locationMap[loc.id] = { ...loc, children: [] };
    });

    // Build parent-child relationships
    locations.forEach((loc) => {
      if (loc.parentLocation?.id) {
        const parent = locationMap[loc.parentLocation.id];
        if (parent) {
          parent.children.push(locationMap[loc.id]);
        }
      } else {
        // Root level location
        roots.push(locationMap[loc.id]);
      }
    });

    return roots;
  };

  // Get icon for location type
  const getLocationIcon = (locationType) => {
    const option = LOCATION_TYPE_OPTIONS.find(
      (opt) => opt.value === locationType,
    );
    return option?.icon || "📍";
  };

  // Handle tree node selection
  const handleNodeSelect = (node) => {
    setSelectedNode(node);
    setFormMode("view");
    setFormData({
      name: node.name,
      locationCode: node.locationCode || "",
      locationType: node.locationType,
      description: node.description || "",
      temperatureMin: node.temperatureMin ?? 2,
      temperatureMax: node.temperatureMax ?? 8,
      parentLocation: node.parentLocation,
    });
  };

  // Handle edit button click
  const handleEditNode = (node) => {
    setSelectedNode(node);
    setFormMode("edit");
    setFormData({
      name: node.name,
      locationCode: node.locationCode || "",
      locationType: node.locationType,
      description: node.description || "",
      temperatureMin: node.temperatureMin ?? 2,
      temperatureMax: node.temperatureMax ?? 8,
      parentLocation: node.parentLocation,
    });
    loadAvailableParents(node.locationType, node.id);
  };

  // Handle add child button click
  const handleAddChild = (parentNode) => {
    setSelectedNode(null);
    setFormMode("create");

    // Determine default child type based on parent
    let defaultChildType = "SHELF";
    if (parentNode.locationType === "ROOM") {
      defaultChildType = "REFRIGERATOR";
    }

    setFormData({
      ...INITIAL_FORM_DATA,
      locationType: defaultChildType,
      parentLocation: {
        id: parentNode.id,
        name: parentNode.name,
        locationType: parentNode.locationType,
      },
    });
    loadAvailableParents(defaultChildType, null);
  };

  // Handle create new root location
  const handleCreateRoot = () => {
    setSelectedNode(null);
    setFormMode("create");
    setFormData(INITIAL_FORM_DATA);
    loadAvailableParents("ROOM", null);
  };

  // Handle delete node
  const handleDeleteNode = async (node) => {
    if (!window.confirm(`Are you sure you want to delete "${node.name}"?`)) {
      return;
    }

    try {
      // Check if location has active lots
      const { hasActiveLots } = await StorageLocationAPI.hasActiveLots(node.id);
      if (hasActiveLots) {
        setError("Cannot delete location with active inventory lots");
        return;
      }

      await StorageLocationAPI.deactivate(node.id);
      await loadTreeData();
      setSelectedNode(null);
      setFormMode("view");
    } catch (err) {
      console.error("Error deleting location:", err);
      setError(err.message || "Failed to delete location");
    }
  };

  // Load available parent locations based on type
  const loadAvailableParents = async (locationType, excludeId = null) => {
    const allowedParentTypes = HIERARCHY_RULES[locationType] || [];

    if (allowedParentTypes.length === 0) {
      setAvailableParents([]);
      return;
    }

    try {
      const allLocations = await StorageLocationAPI.getAll();
      const validParents = allLocations.filter(
        (loc) =>
          allowedParentTypes.includes(loc.locationType) && loc.id !== excludeId,
      );
      setAvailableParents(validParents);
    } catch (err) {
      console.error("Error loading parents:", err);
      setAvailableParents([]);
    }
  };

  // Handle form field changes
  const handleFormChange = (field, value) => {
    setFormData((prev) => {
      const updated = { ...prev, [field]: value };

      // Clear parent if type changed to ROOM
      if (field === "locationType" && value === "ROOM") {
        updated.parentLocation = null;
      }

      // Reload available parents if type changed
      if (field === "locationType") {
        loadAvailableParents(value, selectedNode?.id);
      }

      return updated;
    });
  };

  // Handle form submit
  const handleFormSubmit = async () => {
    const payload = {
      name: formData.name.trim(),
      locationCode: formData.locationCode?.trim() || null,
      locationType: formData.locationType,
      description: formData.description?.trim() || null,
      temperatureMin:
        formData.temperatureMin !== null && formData.temperatureMin !== ""
          ? parseFloat(formData.temperatureMin)
          : null,
      temperatureMax:
        formData.temperatureMax !== null && formData.temperatureMax !== ""
          ? parseFloat(formData.temperatureMax)
          : null,
      parentLocation: formData.parentLocation,
      isActive: true,
    };

    try {
      if (formMode === "create") {
        await StorageLocationAPI.create(payload);
      } else if (formMode === "edit") {
        await StorageLocationAPI.update(selectedNode.id, payload);
      }

      await loadTreeData();
      setFormMode("view");
      setSelectedNode(null);
      setFormData(INITIAL_FORM_DATA);

      if (onSubmit) {
        onSubmit();
      }
    } catch (err) {
      console.error("Error saving location:", err);
      setError(err.message || "Failed to save location");
    }
  };

  // Handle form cancel
  const handleFormCancel = () => {
    setFormMode("view");
    if (selectedNode) {
      setFormData({
        name: selectedNode.name,
        locationCode: selectedNode.locationCode || "",
        locationType: selectedNode.locationType,
        description: selectedNode.description || "",
        temperatureMin: selectedNode.temperatureMin ?? 2,
        temperatureMax: selectedNode.temperatureMax ?? 8,
        parentLocation: selectedNode.parentLocation,
      });
    } else {
      setFormData(INITIAL_FORM_DATA);
    }
  };

  // Toggle node expansion
  const toggleNodeExpansion = (nodeId) => {
    setExpandedNodes((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(nodeId)) {
        newSet.delete(nodeId);
      } else {
        newSet.add(nodeId);
      }
      return newSet;
    });
  };

  // Drag and drop handlers
  const handleDragStart = (e, node) => {
    setDraggedNode(node);
    e.dataTransfer.effectAllowed = "move";
  };

  const handleDragOver = (e, node) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = "move";
    setDragOverNode(node);
  };

  const handleDragLeave = () => {
    setDragOverNode(null);
  };

  const handleDrop = async (e, targetNode) => {
    e.preventDefault();
    setDragOverNode(null);

    if (!draggedNode || draggedNode.id === targetNode.id) {
      setDraggedNode(null);
      return;
    }

    // Validate: can draggedNode be a child of targetNode?
    const allowedParentTypes = HIERARCHY_RULES[draggedNode.locationType] || [];
    if (allowedParentTypes.length === 0) {
      setError(
        `${draggedNode.locationType} cannot have a parent (root level only)`,
      );
      setDraggedNode(null);
      return;
    }

    if (!allowedParentTypes.includes(targetNode.locationType)) {
      setError(
        `${draggedNode.locationType} cannot be placed under ${targetNode.locationType}`,
      );
      setDraggedNode(null);
      return;
    }

    // Prevent making a location its own ancestor
    if (isDescendant(targetNode, draggedNode)) {
      setError("Cannot move location under its own descendant");
      setDraggedNode(null);
      return;
    }

    try {
      await StorageLocationAPI.update(draggedNode.id, {
        ...draggedNode,
        parentLocation: { id: targetNode.id },
      });
      await loadTreeData();
      setDraggedNode(null);
    } catch (err) {
      console.error("Error moving location:", err);
      setError(err.message || "Failed to move location");
      setDraggedNode(null);
    }
  };

  // Check if node1 is a descendant of node2
  const isDescendant = (node1, node2) => {
    let current = node1;
    while (current?.parentLocation?.id) {
      if (current.parentLocation.id === node2.id) {
        return true;
      }
      // Find parent in tree
      current = findNodeById(treeData, current.parentLocation.id);
    }
    return false;
  };

  // Find node by ID in tree
  const findNodeById = (nodes, id) => {
    for (const node of nodes) {
      if (node.id === id) return node;
      if (node.children?.length) {
        const found = findNodeById(node.children, id);
        if (found) return found;
      }
    }
    return null;
  };

  // Render tree node recursively
  const renderTreeNode = (node, depth = 0) => {
    const isExpanded = expandedNodes.has(node.id);
    const isSelected = selectedNode?.id === node.id;
    const isDragOver = dragOverNode?.id === node.id;
    const hasChildren = node.children && node.children.length > 0;

    return (
      <div key={node.id} className="tree-node-container">
        <div
          className={`tree-node ${isSelected ? "selected" : ""} ${isDragOver ? "drag-over" : ""}`}
          style={{ paddingLeft: `${depth * 20}px` }}
          onClick={() => handleNodeSelect(node)}
          draggable
          onDragStart={(e) => handleDragStart(e, node)}
          onDragOver={(e) => handleDragOver(e, node)}
          onDragLeave={handleDragLeave}
          onDrop={(e) => handleDrop(e, node)}
        >
          {hasChildren && (
            <IconButton
              kind="ghost"
              size="sm"
              label={isExpanded ? "Collapse" : "Expand"}
              onClick={(e) => {
                e.stopPropagation();
                toggleNodeExpansion(node.id);
              }}
              className="tree-toggle"
            >
              {isExpanded ? (
                <ChevronDown size={16} />
              ) : (
                <ChevronRight size={16} />
              )}
            </IconButton>
          )}

          {!hasChildren && <span className="tree-spacer" />}

          <span className="tree-icon">
            {getLocationIcon(node.locationType)}
          </span>

          <span className="tree-label">
            {node.name}
            <span className="tree-type">({node.locationType})</span>
          </span>

          {node.temperatureMin != null && node.temperatureMax != null && (
            <span className="tree-temp">
              <Temperature size={14} />
              {node.temperatureMin}°C to {node.temperatureMax}°C
            </span>
          )}

          <div className="tree-actions">
            <IconButton
              kind="ghost"
              size="sm"
              label="Add Child"
              onClick={(e) => {
                e.stopPropagation();
                handleAddChild(node);
              }}
            >
              <Add size={16} />
            </IconButton>

            <IconButton
              kind="ghost"
              size="sm"
              label="Edit"
              onClick={(e) => {
                e.stopPropagation();
                handleEditNode(node);
              }}
            >
              <Edit size={16} />
            </IconButton>

            <IconButton
              kind="ghost"
              size="sm"
              label="Delete"
              onClick={(e) => {
                e.stopPropagation();
                handleDeleteNode(node);
              }}
            >
              <TrashCan size={16} />
            </IconButton>
          </div>
        </div>

        {isExpanded && hasChildren && (
          <div className="tree-children">
            {node.children.map((child) => renderTreeNode(child, depth + 1))}
          </div>
        )}
      </div>
    );
  };

  // Render form based on mode
  const renderForm = () => {
    if (formMode === "view" && !selectedNode) {
      return (
        <div className="empty-state">
          <LocationIcon size={48} />
          <h4>No Location Selected</h4>
          <p>
            Select a location from the tree to view details, or create a new
            location.
          </p>
          <Button kind="primary" renderIcon={Add} onClick={handleCreateRoot}>
            Create Root Location
          </Button>
        </div>
      );
    }

    const isReadOnly = formMode === "view";
    const isValid = formData.name && formData.locationType;

    return (
      <Form className="location-form">
        <Stack gap={6}>
          {error && (
            <InlineNotification
              kind="error"
              title="Error"
              subtitle={error}
              onCloseButtonClick={() => setError(null)}
            />
          )}

          {/* Breadcrumb if editing */}
          {selectedNode?.parentLocation && (
            <div className="breadcrumb">
              <LocationIcon size={16} />
              <span>Parent: {selectedNode.parentLocation.name}</span>
            </div>
          )}

          <TextInput
            id="name"
            labelText={<FormattedMessage id="storage.location.name" />}
            placeholder="e.g., Main Lab Refrigerator"
            value={formData.name}
            onChange={(e) => handleFormChange("name", e.target.value)}
            required
            {...(isReadOnly && { readOnly: true })}
          />

          <TextInput
            id="locationCode"
            labelText={<FormattedMessage id="storage.location.code" />}
            placeholder="e.g., LAB-FRIDGE-01"
            value={formData.locationCode}
            onChange={(e) => handleFormChange("locationCode", e.target.value)}
            {...(isReadOnly && { readOnly: true })}
          />

          <Select
            id="locationType"
            labelText={<FormattedMessage id="storage.location.type" />}
            value={formData.locationType}
            onChange={(e) => handleFormChange("locationType", e.target.value)}
            required
            disabled={isReadOnly}
          >
            {LOCATION_TYPE_OPTIONS.map((opt) => (
              <SelectItem
                key={opt.value}
                value={opt.value}
                text={`${opt.icon} ${opt.label}`}
              />
            ))}
          </Select>

          {/* Parent location selector */}
          {formData.locationType !== "ROOM" && !isReadOnly && (
            <Select
              id="parentLocation"
              labelText={<FormattedMessage id="storage.location.parent" />}
              value={formData.parentLocation?.id || ""}
              onChange={(e) => {
                const parentId = parseInt(e.target.value);
                const parent = availableParents.find((p) => p.id === parentId);
                handleFormChange("parentLocation", parent || null);
              }}
              disabled={availableParents.length === 0}
            >
              <SelectItem value="" text="Select parent location" />
              {availableParents.map((parent) => (
                <SelectItem
                  key={parent.id}
                  value={parent.id}
                  text={`${parent.name} (${parent.locationType})`}
                />
              ))}
            </Select>
          )}

          {formData.locationType !== "ROOM" &&
            availableParents.length === 0 &&
            !isReadOnly && (
              <InlineNotification
                kind="warning"
                title="No valid parent locations"
                subtitle={`Create a ${HIERARCHY_RULES[formData.locationType]?.join(" or ")} first`}
                hideCloseButton
                lowContrast
              />
            )}

          <div className="temperature-section">
            <h5>Temperature Settings</h5>
            <div className="temperature-inputs">
              <NumberInput
                id="temperatureMin"
                label="Min (°C)"
                value={formData.temperatureMin}
                onChange={(e, { value }) =>
                  handleFormChange("temperatureMin", value)
                }
                min={-200}
                max={200}
                step={1}
                {...(isReadOnly && { readOnly: true })}
              />
              <NumberInput
                id="temperatureMax"
                label="Max (°C)"
                value={formData.temperatureMax}
                onChange={(e, { value }) =>
                  handleFormChange("temperatureMax", value)
                }
                min={-200}
                max={200}
                step={1}
                {...(isReadOnly && { readOnly: true })}
              />
            </div>
          </div>

          <TextArea
            id="description"
            labelText={<FormattedMessage id="storage.location.description" />}
            placeholder="Optional description or notes"
            value={formData.description}
            onChange={(e) => handleFormChange("description", e.target.value)}
            rows={3}
            {...(isReadOnly && { readOnly: true })}
          />

          {/* Action buttons */}
          {!isReadOnly && (
            <div className="form-actions">
              <Button kind="secondary" onClick={handleFormCancel}>
                Cancel
              </Button>
              <Button
                kind="primary"
                onClick={handleFormSubmit}
                disabled={!isValid}
              >
                {formMode === "create" ? "Create" : "Update"}
              </Button>
            </div>
          )}

          {isReadOnly && (
            <div className="form-actions">
              <Button
                kind="secondary"
                onClick={() => handleEditNode(selectedNode)}
              >
                Edit Location
              </Button>
              <Button
                kind="danger"
                onClick={() => handleDeleteNode(selectedNode)}
              >
                Delete Location
              </Button>
            </div>
          )}
        </Stack>
      </Form>
    );
  };

  return (
    <ComposedModal
      open={isOpen}
      onClose={onClose}
      size="lg"
      className="storage-location-modal"
    >
      <ModalHeader title="Manage Storage Locations" closeModal={onClose} />

      <ModalBody>
        <div className="modal-content-split">
          {/* Left Panel: Tree View */}
          <div className="tree-panel">
            <div className="tree-panel-header">
              <h5>Location Hierarchy</h5>
              <Button
                kind="primary"
                size="sm"
                renderIcon={Add}
                onClick={handleCreateRoot}
              >
                New Location
              </Button>
            </div>

            <div className="tree-container">
              {loading && <InlineLoading description="Loading locations..." />}

              {!loading && treeData.length === 0 && (
                <div className="empty-tree">
                  <p>
                    No locations yet. Create your first root location to get
                    started.
                  </p>
                </div>
              )}

              {!loading && treeData.length > 0 && (
                <div className="tree-view">
                  {treeData.map((node) => renderTreeNode(node))}
                </div>
              )}
            </div>

            <div className="tree-help">
              <InlineNotification
                kind="info"
                title="Drag and Drop"
                subtitle="Drag locations to reorganize the hierarchy. Hierarchy rules will be enforced."
                hideCloseButton
                lowContrast
              />
            </div>
          </div>

          {/* Right Panel: Form */}
          <div className="form-panel">{renderForm()}</div>
        </div>
      </ModalBody>
    </ComposedModal>
  );
}
