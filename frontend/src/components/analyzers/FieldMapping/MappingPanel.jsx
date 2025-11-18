/**
 * MappingPanel Component
 *
 * Right panel with View Mode and Edit Mode for field mappings
 * Task Reference: T062, T078
 *
 * Supports draft/active workflow per FR-010:
 * - "Save as Draft" button (saves with isActive=false)
 * - "Save and Activate" button (saves with isActive=true, requires confirmation for active analyzers)
 */

import React, { useState } from "react";
import { Button, Dropdown, TextInput } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import OpenELISFieldSelector from "./OpenELISFieldSelector";
import MappingActivationModal from "./MappingActivationModal";
import "./MappingPanel.css";

const MappingPanel = ({
  field,
  mapping,
  onCreateMapping,
  onUpdateMapping,
  analyzerName,
  analyzerIsActive = false,
}) => {
  const intl = useIntl();
  const [editMode, setEditMode] = useState(!mapping);
  const [showActivationModal, setShowActivationModal] = useState(false);
  const [pendingMappingData, setPendingMappingData] = useState(null);
  const [formData, setFormData] = useState({
    openelisFieldId: mapping?.openelisFieldId || "",
    openelisFieldType: mapping?.openelisFieldType || "",
    mappingType: mapping?.mappingType || "DIRECT",
    isRequired: mapping?.isRequired || false,
    isActive: mapping?.isActive !== undefined ? mapping.isActive : false, // Default to draft
  });

  // Handle form field changes
  const handleFieldChange = (fieldName, value) => {
    setFormData((prev) => ({
      ...prev,
      [fieldName]: value,
    }));
  };

  // Handle save as draft
  const handleSaveAsDraft = () => {
    const mappingData = {
      analyzerFieldId: field.id,
      openelisFieldId: formData.openelisFieldId,
      openelisFieldType: formData.openelisFieldType,
      mappingType: formData.mappingType,
      isRequired: formData.isRequired,
      isActive: false, // Save as draft
    };

    if (mapping) {
      onUpdateMapping(mapping.id, mappingData);
    } else {
      onCreateMapping(mappingData);
    }

    setEditMode(false);
  };

  // Handle save and activate
  const handleSaveAndActivate = () => {
    const mappingData = {
      analyzerFieldId: field.id,
      openelisFieldId: formData.openelisFieldId,
      openelisFieldType: formData.openelisFieldType,
      mappingType: formData.mappingType,
      isRequired: formData.isRequired,
      isActive: true, // Activate
    };

    // Check if confirmation is required (active analyzer with active mapping)
    const isUpdatingActiveMapping = mapping && mapping.isActive;
    if (analyzerIsActive && isUpdatingActiveMapping) {
      // Show confirmation modal
      setPendingMappingData(mappingData);
      setShowActivationModal(true);
    } else {
      // No confirmation needed, save directly
      if (mapping) {
        onUpdateMapping(mapping.id, mappingData);
      } else {
        onCreateMapping(mappingData);
      }
      setEditMode(false);
    }
  };

  // Handle activation confirmation
  const handleActivationConfirm = () => {
    if (pendingMappingData) {
      if (mapping) {
        onUpdateMapping(mapping.id, pendingMappingData);
      } else {
        onCreateMapping(pendingMappingData);
      }
      setEditMode(false);
    }
    setShowActivationModal(false);
    setPendingMappingData(null);
  };

  // Handle activation modal close
  const handleActivationModalClose = () => {
    setShowActivationModal(false);
    setPendingMappingData(null);
  };

  // Handle cancel
  const handleCancel = () => {
    if (mapping) {
      setEditMode(false);
      // Reset form data to mapping values
      setFormData({
        openelisFieldId: mapping.openelisFieldId || "",
        openelisFieldType: mapping.openelisFieldType || "",
        mappingType: mapping.mappingType || "DIRECT",
        isRequired: mapping.isRequired || false,
        isActive: mapping.isActive !== undefined ? mapping.isActive : true,
      });
    }
  };

  return (
    <div className="mapping-panel" data-testid="mapping-panel">
      <div className="panel-header">
        <h3>Mapping</h3>
        {!editMode && mapping && (
          <Button
            kind="ghost"
            onClick={() => setEditMode(true)}
            data-testid="mapping-panel-edit-button"
          >
            <FormattedMessage id="analyzer.fieldMapping.panel.target.edit" />
          </Button>
        )}
      </div>

      {editMode ? (
        <div className="edit-mode">
          {/* Source Field Info (read-only) */}
          <div className="source-field-card">
            <h4>Source Field</h4>
            <p>
              <strong>Name:</strong> {field.fieldName}
            </p>
            <p>
              <strong>Type:</strong> {field.fieldType}
            </p>
            <p>
              <strong>Unit:</strong> {field.unit || "-"}
            </p>
          </div>

          {/* OpenELIS Field Selector */}
          <div className="target-field-selector">
            <OpenELISFieldSelector
              fieldType={field.fieldType}
              selectedFieldId={formData.openelisFieldId}
              onFieldSelect={(fieldId, fieldType) => {
                handleFieldChange("openelisFieldId", fieldId);
                handleFieldChange("openelisFieldType", fieldType);
              }}
            />
          </div>

          {/* Action Buttons */}
          <div className="mapping-actions">
            <Button
              kind="secondary"
              onClick={handleCancel}
              data-testid="mapping-panel-cancel-button"
            >
              <FormattedMessage id="analyzer.fieldMapping.panel.target.cancel" />
            </Button>
            <Button
              kind="secondary"
              onClick={handleSaveAsDraft}
              data-testid="mapping-panel-save-draft-button"
            >
              <FormattedMessage id="analyzer.fieldMapping.panel.target.saveDraft" />
            </Button>
            <Button
              kind="primary"
              onClick={handleSaveAndActivate}
              data-testid="mapping-panel-save-and-activate-button"
            >
              <FormattedMessage id="analyzer.fieldMapping.panel.target.saveActivate" />
            </Button>
          </div>
        </div>
      ) : (
        <div className="view-mode">
          {mapping ? (
            <>
              <div className="source-field-card">
                <h4>Source Field</h4>
                <p>
                  <strong>Name:</strong> {field.fieldName}
                </p>
                <p>
                  <strong>Type:</strong> {field.fieldType}
                </p>
              </div>
              <div className="target-field-card">
                <h4>Target Field</h4>
                <p>
                  <strong>Field ID:</strong> {mapping.openelisFieldId}
                </p>
                <p>
                  <strong>Type:</strong> {mapping.openelisFieldType}
                </p>
              </div>
            </>
          ) : (
            <p>No mapping exists for this field. Click "Edit" to create one.</p>
          )}
        </div>
      )}

      {/* Activation Confirmation Modal */}
      <MappingActivationModal
        open={showActivationModal}
        onClose={handleActivationModalClose}
        analyzerName={analyzerName || ""}
        analyzerIsActive={analyzerIsActive}
        onConfirm={handleActivationConfirm}
      />
    </div>
  );
};

export default MappingPanel;
