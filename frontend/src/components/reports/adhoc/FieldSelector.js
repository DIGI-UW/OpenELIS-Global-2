import React from "react";
import { useIntl } from "react-intl";
import {
  Checkbox,
  Tile,
  StructuredListWrapper,
  StructuredListHead,
  StructuredListRow,
  StructuredListCell,
  StructuredListBody,
} from "@carbon/react";
import "../../Style.css";

const FieldSelector = ({
  patientFields = [],
  sampleFields = [],
  selectedFields = [],
  onFieldToggle,
  onSelectAll,
  onDeselectAll,
}) => {
  const intl = useIntl();

  const isFieldSelected = (fieldId) => selectedFields.includes(fieldId);

  const isAllSelected = (fields) =>
    fields.length > 0 &&
    fields.every((f) => selectedFields.includes(f.fieldId));

  const isSomeSelected = (fields) =>
    fields.some((f) => selectedFields.includes(f.fieldId)) &&
    !isAllSelected(fields);

  const handleSelectAllToggle = (entity, fields, isChecked) => {
    if (isChecked) {
      onSelectAll(
        entity,
        fields.map((f) => f.fieldId),
      );
    } else {
      onDeselectAll(
        entity,
        fields.map((f) => f.fieldId),
      );
    }
  };

  const renderFieldGroup = (title, fields, entityName) => {
    if (!fields || fields.length === 0) {
      return null;
    }

    return (
      <Tile className="adhoc-field-group-tile">
        <h4 className="adhoc-field-group-title">{title}</h4>
        <StructuredListWrapper selection>
          <StructuredListHead>
            <StructuredListRow head>
              <StructuredListCell head>
                <Checkbox
                  id={`select-all-${entityName}`}
                  labelText={intl.formatMessage({ id: "adhoc.field.selectAll" })}
                  checked={isAllSelected(fields)}
                  indeterminate={isSomeSelected(fields)}
                  onChange={(_, { checked }) =>
                    handleSelectAllToggle(entityName, fields, checked)
                  }
                />
              </StructuredListCell>
              <StructuredListCell head>
                {intl.formatMessage({ id: "adhoc.field.type" })}
              </StructuredListCell>
            </StructuredListRow>
          </StructuredListHead>
          <StructuredListBody>
            {fields.map((field) => (
              <StructuredListRow key={field.fieldId}>
                <StructuredListCell>
                  <Checkbox
                    id={`field-${field.fieldId}`}
                    labelText={field.displayName}
                    checked={isFieldSelected(field.fieldId)}
                    onChange={(_, { checked }) =>
                      onFieldToggle(field.fieldId, checked)
                    }
                  />
                </StructuredListCell>
                <StructuredListCell>
                  <span
                    className={`adhoc-data-type-badge adhoc-data-type-${field.dataType.toLowerCase()}`}
                  >
                    {field.dataType}
                  </span>
                </StructuredListCell>
              </StructuredListRow>
            ))}
          </StructuredListBody>
        </StructuredListWrapper>
      </Tile>
    );
  };

  return (
    <div className="adhoc-field-selector">
      <h3 className="adhoc-section-title">
        {intl.formatMessage({ id: "adhoc.step.selectFields" })}
      </h3>
      <p className="adhoc-section-description">
        {intl.formatMessage({ id: "adhoc.step.selectFields.description" })}
      </p>

      <div className="adhoc-field-groups-container">
        {renderFieldGroup(
          intl.formatMessage({ id: "adhoc.entity.patient" }),
          patientFields,
          "patient",
        )}
        {renderFieldGroup(
          intl.formatMessage({ id: "adhoc.entity.sample" }),
          sampleFields,
          "sample",
        )}
      </div>

      {selectedFields.length > 0 && (
        <div className="adhoc-selected-fields-summary">
          <span className="adhoc-selected-count">
            {intl.formatMessage(
              { id: "adhoc.field.selectedCount" },
              { count: selectedFields.length },
            )}
          </span>
        </div>
      )}
    </div>
  );
};

export default FieldSelector;
