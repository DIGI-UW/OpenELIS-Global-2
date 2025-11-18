/**
 * MappingPanel Component
 * 
 * Right panel with View Mode and Edit Mode for field mappings
 * Task Reference: T062
 */

import React, { useState } from "react";
import {
  Button,
  Dropdown,
  TextInput,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import OpenELISFieldSelector from "./OpenELISFieldSelector";
import "./MappingPanel.css";

const MappingPanel = ({
  field,
  mapping,
  onCreateMapping,
  onUpdateMapping,
}) => {
  const intl = useIntl();
  const [editMode, setEditMode] = useState(!mapping);
  const [formData, setFormData] = useState({
    openelisFieldId: mapping?.openelisFieldId || "",
    openelisFieldType: mapping?.openelisFieldType || "",
    mappingType: mapping?.mappingType || "DIRECT",
    isRequired: mapping?.isRequired || false,
    isActive: mapping?.isActive !== undefined ? mapping.isActive : true,
  });

  // Handle form field changes
  const handleFieldChange = (fieldName, value) => {
    setFormData((prev) => ({
      ...prev,
      [fieldName]: value,
    }));
  };

  // Handle save
  const handleSave = () => {
    const mappingData = {
      analyzerFieldId: field.id,
      openelisFieldId: formData.openelisFieldId,
      openelisFieldType: formData.openelisFieldType,
      mappingType: formData.mappingType,
      isRequired: formData.isRequired,
      isActive: formData.isActive,
    };

    if (mapping) {
      onUpdateMapping(mapping.id, mappingData);
    } else {
      onCreateMapping(mappingData);
    }
    
    setEditMode(false);
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
          <Button kind="ghost" onClick={() => setEditMode(true)} data-testid="mapping-panel-edit-button">
            <FormattedMessage id="analyzer.fieldMapping.panel.target.edit" />
          </Button>
        )}
      </div>

      {editMode ? (
        <div className="edit-mode">
          {/* Source Field Info (read-only) */}
          <div className="source-field-card">
            <h4>Source Field</h4>
            <p><strong>Name:</strong> {field.fieldName}</p>
            <p><strong>Type:</strong> {field.fieldType}</p>
            <p><strong>Unit:</strong> {field.unit || "-"}</p>
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
            <Button kind="secondary" onClick={handleCancel} data-testid="mapping-panel-cancel-button">
              <FormattedMessage id="analyzer.fieldMapping.panel.target.cancel" />
            </Button>
            <Button kind="primary" onClick={handleSave} data-testid="mapping-panel-save-button">
              <FormattedMessage id="analyzer.form.save" />
            </Button>
          </div>
        </div>
      ) : (
        <div className="view-mode">
          {mapping ? (
            <>
              <div className="source-field-card">
                <h4>Source Field</h4>
                <p><strong>Name:</strong> {field.fieldName}</p>
                <p><strong>Type:</strong> {field.fieldType}</p>
              </div>
              <div className="target-field-card">
                <h4>Target Field</h4>
                <p><strong>Field ID:</strong> {mapping.openelisFieldId}</p>
                <p><strong>Type:</strong> {mapping.openelisFieldType}</p>
              </div>
            </>
          ) : (
            <p>No mapping exists for this field. Click "Edit" to create one.</p>
          )}
        </div>
      )}
    </div>
  );
};

export default MappingPanel;

