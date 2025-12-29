import React, { useState, useEffect, useCallback } from "react";
import { TreeView, TreeNode, Button } from "@carbon/react";
import { Template, Document, Edit, WarningAlt } from "@carbon/icons-react";
import { getFromOpenElisServer } from "../utils/Utils";
import { FormattedMessage, useIntl } from "react-intl";
import { Loading } from "@carbon/react";

/**
 * NotebookTreeView - Hierarchical tree view for notebook templates and their child instances.
 *
 * Parent templates (isParentTemplate=true) are shown as expandable nodes with Template icon.
 * Child instances (isChildInstance=true) are shown as leaf nodes with Document icon.
 * Entry counts are displayed at both parent (aggregate) and child levels.
 *
 * @param {Function} onSelectNotebook - Callback when a notebook is selected. Receives (notebookId, isParentTemplate, notebookData)
 * @param {Number} selectedId - Currently selected notebook ID
 * @param {Function} onRefresh - Optional callback to trigger refresh
 */
const NotebookTreeView = ({ onSelectNotebook, selectedId, onRefresh }) => {
  const [hierarchy, setHierarchy] = useState([]);
  const [loading, setLoading] = useState(true);
  const [expandedNodes, setExpandedNodes] = useState([]);
  const intl = useIntl();

  const loadHierarchy = useCallback(() => {
    setLoading(true);
    getFromOpenElisServer("/rest/notebook/hierarchy", (data) => {
      if (Array.isArray(data)) {
        setHierarchy(data);
        // Start with all nodes collapsed
        setExpandedNodes([]);
      } else {
        setHierarchy([]);
      }
      setLoading(false);
    });
  }, []);

  useEffect(() => {
    loadHierarchy();
  }, [loadHierarchy]);

  // Expose refresh function
  useEffect(() => {
    if (onRefresh) {
      onRefresh(loadHierarchy);
    }
  }, [onRefresh, loadHierarchy]);

  const handleSelect = (event, node) => {
    if (node && node.id) {
      // Parse the node ID to get the actual notebook ID
      const idParts = node.id.split("-");
      const nodeType = idParts[0];
      const notebookId = parseInt(idParts[1], 10);

      // Handle orphan entries node - select parent with special flag
      if (nodeType === "orphan") {
        const parentData = hierarchy.find((p) => p.id === notebookId);
        if (parentData) {
          // Pass parent data with orphan flag for showing orphan entries
          onSelectNotebook(notebookId, true, {
            ...parentData,
            showOrphanEntries: true,
          });
        }
        return;
      }

      const isParent = nodeType === "parent";

      // Toggle expansion when clicking on a parent node
      if (isParent) {
        handleToggle(event, { id: node.id });
      }

      // Find the notebook data
      let notebookData = null;
      if (isParent) {
        notebookData = hierarchy.find((p) => p.id === notebookId);
      } else {
        for (const parent of hierarchy) {
          const child = parent.children?.find((c) => c.id === notebookId);
          if (child) {
            notebookData = child;
            break;
          }
        }
      }

      onSelectNotebook(notebookId, isParent, notebookData);
    }
  };

  const handleToggle = (event, node) => {
    const nodeId = node?.id;
    if (nodeId) {
      setExpandedNodes((prev) => {
        if (prev.includes(nodeId)) {
          return prev.filter((id) => id !== nodeId);
        } else {
          return [...prev, nodeId];
        }
      });
    }
  };

  const isSelected = (nodeId) => {
    if (!selectedId) return false;
    return (
      nodeId === `parent-${selectedId}` || nodeId === `child-${selectedId}`
    );
  };

  const handleEdit = (e, notebookId) => {
    e.stopPropagation();
    window.location.href = `/NoteBookEntryForm/${notebookId}`;
  };

  if (loading) {
    return (
      <Loading
        description={intl.formatMessage({ id: "loading.description" })}
        withOverlay={false}
        small
      />
    );
  }

  if (hierarchy.length === 0) {
    return (
      <div style={{ padding: "1rem", color: "#6f6f6f", fontSize: "0.875rem" }}>
        <FormattedMessage
          id="notebook.tree.empty"
          defaultMessage="No notebooks found"
        />
      </div>
    );
  }

  return (
    <div className="notebook-tree-view">
      <TreeView
        label={intl.formatMessage({
          id: "notebook.tree.label",
          defaultMessage: "Notebooks",
        })}
        onSelect={handleSelect}
        selected={
          selectedId ? [`parent-${selectedId}`, `child-${selectedId}`] : []
        }
      >
        {hierarchy.map((parent) => (
          <TreeNode
            key={`parent-${parent.id}`}
            id={`parent-${parent.id}`}
            value={`parent-${parent.id}`}
            label={
              <span className="tree-node-label">
                <Button
                  kind="ghost"
                  size="sm"
                  hasIconOnly
                  renderIcon={Edit}
                  iconDescription={intl.formatMessage({
                    id: "notebook.icon.edit",
                    defaultMessage: "Edit",
                  })}
                  onClick={(e) => handleEdit(e, parent.id)}
                  className="tree-node-edit-btn"
                />
                <Template size={16} className="tree-node-icon parent-icon" />
                <span className="tree-node-title">{parent.title}</span>
                <span className="tree-node-count">
                  ({parent.totalEntries || 0})
                </span>
              </span>
            }
            isExpanded={expandedNodes.includes(`parent-${parent.id}`)}
            onToggle={(e) => handleToggle(e, { id: `parent-${parent.id}` })}
            className={
              isSelected(`parent-${parent.id}`) ? "tree-node-selected" : ""
            }
          >
            {/* Orphan/Unassigned Entries node - only show if there are orphan entries */}
            {parent.orphanEntryCount > 0 && (
              <TreeNode
                key={`orphan-${parent.id}`}
                id={`orphan-${parent.id}`}
                value={`orphan-${parent.id}`}
                label={
                  <span className="tree-node-label tree-node-orphan">
                    <WarningAlt
                      size={16}
                      className="tree-node-icon orphan-icon"
                    />
                    <span className="tree-node-title">
                      <FormattedMessage
                        id="notebook.tree.unassignedEntries"
                        defaultMessage="Unassigned Entries"
                      />
                    </span>
                    <span className="tree-node-count tree-node-orphan-count">
                      ({parent.orphanEntryCount})
                    </span>
                  </span>
                }
                className={
                  isSelected(`orphan-${parent.id}`) ? "tree-node-selected" : ""
                }
              />
            )}
            {/* Child instances */}
            {parent.children && parent.children.length > 0
              ? parent.children.map((child) => (
                  <TreeNode
                    key={`child-${child.id}`}
                    id={`child-${child.id}`}
                    value={`child-${child.id}`}
                    label={
                      <span className="tree-node-label">
                        <Button
                          kind="ghost"
                          size="sm"
                          hasIconOnly
                          renderIcon={Edit}
                          iconDescription={intl.formatMessage({
                            id: "notebook.icon.edit",
                            defaultMessage: "Edit",
                          })}
                          onClick={(e) => handleEdit(e, child.id)}
                          className="tree-node-edit-btn"
                        />
                        <Document
                          size={16}
                          className="tree-node-icon child-icon"
                        />
                        <span className="tree-node-title">{child.title}</span>
                        <span className="tree-node-count">
                          ({child.entryCount || 0})
                        </span>
                      </span>
                    }
                    className={
                      isSelected(`child-${child.id}`)
                        ? "tree-node-selected"
                        : ""
                    }
                  />
                ))
              : // Only show "No lab instances" if there are also no orphan entries
                parent.orphanEntryCount === 0 && (
                  <TreeNode
                    key={`empty-${parent.id}`}
                    id={`empty-${parent.id}`}
                    value={`empty-${parent.id}`}
                    disabled
                    label={
                      <span className="tree-node-label tree-node-empty">
                        <FormattedMessage
                          id="notebook.tree.noChildren"
                          defaultMessage="No lab instances"
                        />
                      </span>
                    }
                  />
                )}
          </TreeNode>
        ))}
      </TreeView>
    </div>
  );
};

export default NotebookTreeView;
