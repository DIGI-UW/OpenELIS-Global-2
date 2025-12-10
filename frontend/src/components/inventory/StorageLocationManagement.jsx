import React, { useState, useEffect, useCallback, useContext } from "react";
import {
  Button,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
  TableToolbar,
  TableToolbarContent,
  InlineNotification,
  InlineLoading,
  Tag,
  IconButton,
  Tooltip,
} from "@carbon/react";
import {
  Add,
  Edit,
  TrashCan,
  Temperature,
  Location as LocationIcon,
  ChevronRight,
  ChevronDown,
  DragVertical,
  Folder,
  FolderOpen,
} from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { StorageLocationAPI } from "./InventoryService";
import StorageLocationFormModal from "./StorageLocationFormModal";
import { NotificationContext } from "../layout/Layout";
import { NotificationKinds } from "../common/CustomNotification";
import "./StorageLocationManagement.css";

const LOCATION_TYPE_OPTIONS = [
  { value: "ROOM", label: "Room", icon: "🏢" },
  { value: "REFRIGERATOR", label: "Refrigerator", icon: "❄️" },
  { value: "FREEZER", label: "Freezer", icon: "🧊" },
  { value: "CABINET", label: "Cabinet", icon: "🗄️" },
  { value: "SHELF", label: "Shelf", icon: "📚" },
  { value: "DRAWER", label: "Drawer", icon: "🗃️" },
];

const HIERARCHY_RULES = {
  ROOM: [],
  REFRIGERATOR: ["ROOM"],
  FREEZER: ["ROOM"],
  CABINET: ["ROOM"],
  SHELF: ["REFRIGERATOR", "FREEZER", "CABINET"],
  DRAWER: ["REFRIGERATOR", "FREEZER", "CABINET"],
};

/**
 * StorageLocationManagement - Standalone page for managing storage locations
 */
export default function StorageLocationManagement() {
  const intl = useIntl();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, subtitle, message }) => {
      setNotificationVisible(true);
      addNotification({
        kind,
        title,
        subtitle,
        message,
      });
    },
    [addNotification, setNotificationVisible],
  );

  const [locations, setLocations] = useState([]);
  const [treeData, setTreeData] = useState([]);
  const [expandedNodes, setExpandedNodes] = useState(new Set());
  const [loading, setLoading] = useState(false);

  const [formModalOpen, setFormModalOpen] = useState(false);
  const [formMode, setFormMode] = useState("create");
  const [editingLocation, setEditingLocation] = useState(null);
  const [parentLocation, setParentLocation] = useState(null);

  const [draggedNode, setDraggedNode] = useState(null);
  const [dragOverNode, setDragOverNode] = useState(null);

  const loadLocations = useCallback(async () => {
    setLoading(true);
    try {
      const allLocations = await StorageLocationAPI.getAll();
      setLocations(allLocations);
      const tree = buildTreeStructure(allLocations);
      setTreeData(tree);

      const rootIds = tree.map((node) => node.id);
      setExpandedNodes(new Set(rootIds));
    } catch (err) {
      console.error("Error loading locations:", err);
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: "Failed to load storage locations",
      });
    } finally {
      setLoading(false);
    }
  }, [notify, intl]);

  useEffect(() => {
    loadLocations();
  }, [loadLocations]);

  const buildTreeStructure = (locations) => {
    const locationMap = {};
    const roots = [];

    locations.forEach((loc) => {
      locationMap[loc.id] = { ...loc, children: [] };
    });

    locations.forEach((loc) => {
      if (loc.parentLocation?.id) {
        const parent = locationMap[loc.parentLocation.id];
        if (parent) {
          parent.children.push(locationMap[loc.id]);
        }
      } else {
        roots.push(locationMap[loc.id]);
      }
    });

    return roots;
  };

  const getLocationIcon = (locationType) => {
    const option = LOCATION_TYPE_OPTIONS.find(
      (opt) => opt.value === locationType,
    );
    return option?.icon || "📍";
  };

  const getLocationLabel = (locationType) => {
    const option = LOCATION_TYPE_OPTIONS.find(
      (opt) => opt.value === locationType,
    );
    return option?.label || locationType;
  };

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

  const handleCreate = () => {
    setFormMode("create");
    setEditingLocation(null);
    setParentLocation(null);
    setFormModalOpen(true);
  };

  const handleEdit = (location) => {
    setFormMode("edit");
    setEditingLocation(location);
    setParentLocation(null);
    setFormModalOpen(true);
  };

  const handleAddChild = (parent) => {
    setFormMode("create");
    setEditingLocation(null);
    setParentLocation(parent);
    setFormModalOpen(true);
  };

  const handleDelete = async (location) => {
    if (
      !window.confirm(
        `Are you sure you want to delete "${location.name}"? This cannot be undone.`,
      )
    ) {
      return;
    }

    try {
      const { hasActiveLots } = await StorageLocationAPI.hasActiveLots(
        location.id,
      );
      if (hasActiveLots) {
        notify({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.error" }),
          subtitle: "Cannot delete location with active inventory lots",
        });
        return;
      }

      await StorageLocationAPI.deactivate(location.id);
      await loadLocations();
      notify({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.success" }),
        subtitle: `Location "${location.name}" deleted successfully`,
      });
    } catch (err) {
      console.error("Error deleting location:", err);
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: err.message || "Failed to delete location",
      });
    }
  };

  const handleFormSubmit = async () => {
    await loadLocations();
    setFormModalOpen(false);
    notify({
      kind: NotificationKinds.success,
      title: intl.formatMessage({ id: "notification.success" }),
      subtitle:
        formMode === "create"
          ? "Location created successfully"
          : "Location updated successfully",
    });
  };

  // Drag and drop handlers
  const handleDragStart = (e, node) => {
    if (!node.parentLocation) {
      e.preventDefault();
      return;
    }
    setDraggedNode(node);
    e.dataTransfer.effectAllowed = "move";
  };

  const handleDragOver = (e, node) => {
    e.preventDefault();
    e.stopPropagation();

    if (!draggedNode || draggedNode.id === node.id) return;

    const allowedParentTypes = HIERARCHY_RULES[draggedNode.locationType] || [];
    if (allowedParentTypes.includes(node.locationType)) {
      e.dataTransfer.dropEffect = "move";
      setDragOverNode(node);
    } else {
      e.dataTransfer.dropEffect = "none";
    }
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    setDragOverNode(null);
  };

  const handleDrop = async (e, targetNode) => {
    e.preventDefault();
    e.stopPropagation();
    setDragOverNode(null);

    if (!draggedNode || draggedNode.id === targetNode.id) {
      setDraggedNode(null);
      return;
    }

    const allowedParentTypes = HIERARCHY_RULES[draggedNode.locationType] || [];
    if (!allowedParentTypes.includes(targetNode.locationType)) {
      notify({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({ id: "notification.warning" }),
        subtitle: `${getLocationLabel(draggedNode.locationType)} cannot be placed under ${getLocationLabel(targetNode.locationType)}`,
      });
      setDraggedNode(null);
      return;
    }

    try {
      await StorageLocationAPI.update(draggedNode.id, {
        ...draggedNode,
        parentLocation: { id: targetNode.id },
      });
      await loadLocations();
      setDraggedNode(null);
      setExpandedNodes((prev) => new Set([...prev, targetNode.id]));
      notify({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.success" }),
        subtitle: `Moved "${draggedNode.name}" to "${targetNode.name}"`,
      });
    } catch (err) {
      console.error("Error moving location:", err);
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: err.message || "Failed to move location",
      });
      setDraggedNode(null);
    }
  };

  const renderTreeNode = (node, depth = 0) => {
    const isExpanded = expandedNodes.has(node.id);
    const isDragOver = dragOverNode?.id === node.id;
    const hasChildren = node.children && node.children.length > 0;
    const isRoot = !node.parentLocation;

    return (
      <div key={node.id} className="tree-node-wrapper">
        <div
          className={`tree-node-item ${isDragOver ? "drag-over" : ""} ${isRoot ? "root-node" : ""}`}
          style={{ paddingLeft: `${depth * 24}px` }}
          draggable={!isRoot}
          onDragStart={(e) => handleDragStart(e, node)}
          onDragOver={(e) => handleDragOver(e, node)}
          onDragLeave={handleDragLeave}
          onDrop={(e) => handleDrop(e, node)}
        >
          {!isRoot && (
            <span className="drag-handle" title="Drag to reorganize">
              <DragVertical size={16} />
            </span>
          )}

          {hasChildren ? (
            <IconButton
              kind="ghost"
              size="sm"
              label={isExpanded ? "Collapse" : "Expand"}
              onClick={() => toggleNodeExpansion(node.id)}
              className="expand-button"
            >
              {isExpanded ? (
                <ChevronDown size={16} />
              ) : (
                <ChevronRight size={16} />
              )}
            </IconButton>
          ) : (
            <span className="expand-spacer" />
          )}

          <span className="node-icon">
            {hasChildren ? (
              isExpanded ? (
                <FolderOpen size={20} />
              ) : (
                <Folder size={20} />
              )
            ) : (
              <span style={{ fontSize: "1.125rem" }}>
                {getLocationIcon(node.locationType)}
              </span>
            )}
          </span>

          <div className="node-content">
            <div className="node-title">
              <span className="node-name">{node.name}</span>
              <Tag type="blue" size="sm" className="node-type-tag">
                {getLocationLabel(node.locationType)}
              </Tag>
            </div>
            <div className="node-details">
              {node.locationCode && (
                <span className="node-code">Code: {node.locationCode}</span>
              )}
              {node.temperatureMin != null && node.temperatureMax != null && (
                <span className="node-temp">
                  <Temperature size={14} />
                  {node.temperatureMin}°C - {node.temperatureMax}°C
                </span>
              )}
            </div>
          </div>

          <div className="node-actions">
            <Tooltip label="Add child location" align="top">
              <IconButton
                kind="ghost"
                size="sm"
                label="Add child"
                onClick={() => handleAddChild(node)}
              >
                <Add size={16} />
              </IconButton>
            </Tooltip>
            <Tooltip label="Edit location" align="top">
              <IconButton
                kind="ghost"
                size="sm"
                label="Edit"
                onClick={() => handleEdit(node)}
              >
                <Edit size={16} />
              </IconButton>
            </Tooltip>
            <Tooltip label="Delete location" align="top">
              <IconButton
                kind="ghost"
                size="sm"
                label="Delete"
                onClick={() => handleDelete(node)}
              >
                <TrashCan size={16} />
              </IconButton>
            </Tooltip>
          </div>
        </div>

        {isExpanded && hasChildren && (
          <div className="tree-node-children">
            {node.children.map((child) => renderTreeNode(child, depth + 1))}
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="storage-location-management">
      <div className="page-header">
        <div className="header-content">
          <h4>Storage Location Management</h4>
          <p>
            Manage your laboratory storage locations. Organize in a hierarchical
            structure and drag to reorganize.
          </p>
        </div>
        <Button
          kind="primary"
          size="md"
          renderIcon={Add}
          onClick={handleCreate}
        >
          Add Location
        </Button>
      </div>

      {loading ? (
        <div className="loading-container">
          <InlineLoading description="Loading locations..." />
        </div>
      ) : treeData.length === 0 ? (
        <div className="empty-state">
          <LocationIcon size={48} />
          <h5>No Locations Yet</h5>
          <p>
            Create your first storage location to get started organizing your
            laboratory inventory.
          </p>
          <Button kind="primary" renderIcon={Add} onClick={handleCreate}>
            Create First Location
          </Button>
        </div>
      ) : (
        <div className="tree-content-page">
          <div className="tree-legend">
            <span className="legend-item">
              <DragVertical size={16} /> Drag to reorganize (child locations
              only)
            </span>
            <span className="legend-item">
              <ChevronRight size={16} /> Expand/collapse to view children
            </span>
          </div>
          <div className="tree-nodes-page">
            {treeData.map((node) => renderTreeNode(node, 0))}
          </div>
        </div>
      )}

      <StorageLocationFormModal
        isOpen={formModalOpen}
        onClose={() => setFormModalOpen(false)}
        onSubmit={handleFormSubmit}
        mode={formMode}
        location={editingLocation}
        parentLocation={parentLocation}
      />
    </div>
  );
}
