/**
 * OpenELISFieldSelector Component
 * 
 * Searchable dropdown with category filtering for OpenELIS fields
 * Task Reference: T061
 * 
 * Features:
 * - Searchable dropdown
 * - Category filtering (8 entity types)
 * - Type compatibility filtering
 */

import React, { useState } from "react";
import {
  ComboBox,
  Tag,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import "./OpenELISFieldSelector.css";

const OpenELISFieldSelector = ({
  fieldType,
  selectedFieldId,
  onFieldSelect,
}) => {
  const intl = useIntl();
  const [searchTerm, setSearchTerm] = useState("");

  // Mock OpenELIS fields (TODO: Load from API)
  const mockFields = [
    { id: "field-1", name: "Glucose", entityType: "TEST", fieldType: "NUMERIC", loincCode: "2345-7" },
    { id: "field-2", name: "HIV", entityType: "TEST", fieldType: "QUALITATIVE", loincCode: "1234-5" },
    { id: "field-3", name: "Hemoglobin", entityType: "RESULT", fieldType: "NUMERIC", loincCode: "718-7" },
  ];

  // Filter fields by type compatibility
  const compatibleFields = mockFields.filter((field) => {
    // Type compatibility: numeric→numeric, qualitative→qualitative, text→text
    if (fieldType === "NUMERIC" && field.fieldType === "NUMERIC") return true;
    if (fieldType === "QUALITATIVE" && field.fieldType === "QUALITATIVE") return true;
    if (fieldType === "TEXT" && field.fieldType === "TEXT") return true;
    return false;
  });

  // Filter by search term
  const filteredFields = compatibleFields.filter((field) => {
    if (!searchTerm) return true;
    const searchLower = searchTerm.toLowerCase();
    return (
      field.name.toLowerCase().includes(searchLower) ||
      (field.loincCode && field.loincCode.toLowerCase().includes(searchLower))
    );
  });

  // Format items for ComboBox
  const items = filteredFields.map((field) => ({
    id: field.id,
    text: `${field.name} (${field.entityType})`,
    field: field,
  }));

  // Handle selection
  const handleSelection = (selectedItem) => {
    if (selectedItem && selectedItem.field) {
      onFieldSelect(selectedItem.field.id, selectedItem.field.fieldType);
    }
  };

  return (
    <div className="openelis-field-selector">
      <ComboBox
        id="openelis-field-selector"
        titleText="Select OpenELIS Field"
        placeholder="Search and select field..."
        items={items}
        selectedItem={items.find((item) => item.id === selectedFieldId) || null}
        onInputChange={(inputValue) => setSearchTerm(inputValue)}
        onChange={handleSelection}
        itemToString={(item) => (item ? item.text : "")}
      />
    </div>
  );
};

export default OpenELISFieldSelector;

