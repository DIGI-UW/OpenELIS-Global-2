import React, { useState, useEffect } from "react";
import { ChevronRight, ChevronDown } from "@carbon/react/icons";
import { getFromOpenElisServer } from "../../utils/Utils";
import "./LocationTreeView.css";

/**
 * LocationTreeView component for hierarchical browsing with expand/collapse
 * Displays Room → Device → Shelf → Rack hierarchy (Position level excluded)
 *
 * Props:
 * - onLocationSelect: function - Callback when location is selected
 * - allowInactive: boolean - Allow selection of inactive locations (default: false)
 */
const LocationTreeView = ({ onLocationSelect, allowInactive = false }) => {
  const [rooms, setRooms] = useState([]);
  const [expandedNodes, setExpandedNodes] = useState(new Set());
  const [loadedChildren, setLoadedChildren] = useState({}); // { nodeId: { devices: [], shelves: [], racks: [] } }

  // Load rooms on mount
  useEffect(() => {
    getFromOpenElisServer(
      "/rest/storage/rooms",
      (response) => {
        setRooms(response || []);
      },
      () => {
        setRooms([]);
      },
    );
  }, []);

  // Load children when node is expanded
  const loadChildren = (nodeId, nodeType, parentId) => {
    const cacheKey = `${nodeType}-${nodeId}`;
    if (loadedChildren[cacheKey]) {
      return; // Already loaded
    }

    let url = "";
    if (nodeType === "room") {
      // Ensure roomId is sent as string (backend parses it)
      url = `/rest/storage/devices?roomId=${String(nodeId)}`;
    } else if (nodeType === "device") {
      url = `/rest/storage/shelves?deviceId=${String(nodeId)}`;
    } else if (nodeType === "shelf") {
      url = `/rest/storage/racks?shelfId=${String(nodeId)}`;
    }
    // Rack level doesn't load children (Position excluded per FR-065b)

    if (url) {
      getFromOpenElisServer(
        url,
        (response) => {
          setLoadedChildren((prev) => ({
            ...prev,
            [cacheKey]: response || [],
          }));
        },
        () => {
          setLoadedChildren((prev) => ({
            ...prev,
            [cacheKey]: [],
          }));
        },
      );
    }
  };

  const toggleNode = (nodeId, nodeType, parentId) => {
    const key = `${nodeType}-${nodeId}`;
    const newExpanded = new Set(expandedNodes);

    if (newExpanded.has(key)) {
      newExpanded.delete(key);
    } else {
      newExpanded.add(key);
      loadChildren(nodeId, nodeType, parentId);
    }

    setExpandedNodes(newExpanded);
  };

  const handleNodeClick = (node, nodeType) => {
    // Prevent selection of inactive locations unless allowInactive is true
    if (!allowInactive && node.active === false) {
      return;
    }
    // Call onLocationSelect with location info including type for downward inclusive filtering
    if (onLocationSelect) {
      onLocationSelect({
        id: node.id,
        name: node.name || node.label,
        code: node.code,
        type: nodeType,
        ...node,
      });
    }
  };

  const renderNode = (node, nodeType, level = 0, parentId = null) => {
    const key = `${nodeType}-${node.id}`;
    const isExpanded = expandedNodes.has(key);
    const children = loadedChildren[key] || [];
    const hasChildren = nodeType !== "rack"; // Rack is the last level (Position excluded)

    const getDisplayName = () => {
      if (nodeType === "room") return node.name;
      if (nodeType === "device") return node.name;
      if (nodeType === "shelf") return node.label;
      if (nodeType === "rack") return node.label;
      return "";
    };

    const getChildType = () => {
      if (nodeType === "room") return "device";
      if (nodeType === "device") return "shelf";
      if (nodeType === "shelf") return "rack";
      return null;
    };

    const displayName = getDisplayName();
    const childType = getChildType();
    const isInactive = node.active === false;

    return (
      <li
        key={key}
        className={`tree-node ${isInactive ? "inactive" : ""}`}
        style={{ paddingLeft: level > 0 ? `${level * 0.75}rem` : "0" }}
      >
        <div className="tree-node-content">
          {hasChildren && (
            <button
              type="button"
              onClick={() => toggleNode(node.id, nodeType, parentId)}
              className="tree-expand-button"
              aria-expanded={isExpanded}
              aria-label={isExpanded ? "Collapse" : "Expand"}
              title=""
            >
              {isExpanded ? (
                <ChevronDown size={16} />
              ) : (
                <ChevronRight size={16} />
              )}
            </button>
          )}
          {!hasChildren && <span className="tree-spacer" />}
          <button
            type="button"
            className={`tree-node-label ${isInactive ? "inactive" : ""}`}
            onClick={() => handleNodeClick(node, nodeType)}
            disabled={!allowInactive && isInactive}
          >
            {displayName}
            {isInactive && <span className="inactive-badge"> (Inactive)</span>}
          </button>
        </div>
        {isExpanded && hasChildren && childType && (
          <ul className="tree-children">
            {children.map((child) =>
              renderNode(child, childType, level + 1, node.id),
            )}
          </ul>
        )}
      </li>
    );
  };

  return (
    <div className="location-tree-view" data-testid="location-tree-view">
      <ul className="tree-root">
        {rooms.map((room) => renderNode(room, "room", 0))}
      </ul>
    </div>
  );
};

export default LocationTreeView;
