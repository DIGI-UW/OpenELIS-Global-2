import React from "react";
import { useIntl } from "react-intl";
import {
  Select,
  SelectItem,
  TextInput,
  Button,
  DatePicker,
  DatePickerInput,
} from "@carbon/react";
import { Add, TrashCan } from "@carbon/icons-react";
import "../../Style.css";

const FilterBuilder = ({
  availableFields = [],
  filters = [],
  onFiltersChange,
}) => {
  const intl = useIntl();

  const filterableFields = availableFields.filter((f) => f.filterable);

  const getOperatorsForField = (field) => {
    if (!field) return [];

    const stringOperators = [
      { value: "EQUALS", label: intl.formatMessage({ id: "adhoc.operator.equals" }) },
      { value: "NOT_EQUALS", label: intl.formatMessage({ id: "adhoc.operator.notEquals" }) },
      { value: "CONTAINS", label: intl.formatMessage({ id: "adhoc.operator.contains" }) },
      { value: "STARTS_WITH", label: intl.formatMessage({ id: "adhoc.operator.startsWith" }) },
      { value: "IS_NULL", label: intl.formatMessage({ id: "adhoc.operator.isEmpty" }) },
      { value: "IS_NOT_NULL", label: intl.formatMessage({ id: "adhoc.operator.isNotEmpty" }) },
    ];

    const dateOperators = [
      { value: "EQUALS", label: intl.formatMessage({ id: "adhoc.operator.equals" }) },
      { value: "NOT_EQUALS", label: intl.formatMessage({ id: "adhoc.operator.notEquals" }) },
      { value: "GREATER_THAN", label: intl.formatMessage({ id: "adhoc.operator.after" }) },
      { value: "LESS_THAN", label: intl.formatMessage({ id: "adhoc.operator.before" }) },
      { value: "BETWEEN", label: intl.formatMessage({ id: "adhoc.operator.between" }) },
      { value: "IS_NULL", label: intl.formatMessage({ id: "adhoc.operator.isEmpty" }) },
      { value: "IS_NOT_NULL", label: intl.formatMessage({ id: "adhoc.operator.isNotEmpty" }) },
    ];

    const enumOperators = [
      { value: "EQUALS", label: intl.formatMessage({ id: "adhoc.operator.equals" }) },
      { value: "NOT_EQUALS", label: intl.formatMessage({ id: "adhoc.operator.notEquals" }) },
      { value: "IN", label: intl.formatMessage({ id: "adhoc.operator.in" }) },
      { value: "IS_NULL", label: intl.formatMessage({ id: "adhoc.operator.isEmpty" }) },
      { value: "IS_NOT_NULL", label: intl.formatMessage({ id: "adhoc.operator.isNotEmpty" }) },
    ];

    switch (field.dataType) {
      case "DATE":
      case "DATETIME":
        return dateOperators;
      case "ENUM":
        return enumOperators;
      default:
        return stringOperators;
    }
  };

  const addFilter = () => {
    const newFilter = {
      id: Date.now(),
      fieldId: "",
      operator: "EQUALS",
      value: "",
      valueTo: "",
    };
    onFiltersChange([...filters, newFilter]);
  };

  const removeFilter = (filterId) => {
    onFiltersChange(filters.filter((f) => f.id !== filterId));
  };

  const updateFilter = (filterId, field, value) => {
    const updatedFilters = filters.map((f) => {
      if (f.id === filterId) {
        const updated = { ...f, [field]: value };
        if (field === "fieldId") {
          updated.operator = "EQUALS";
          updated.value = "";
          updated.valueTo = "";
        }
        if (field === "operator" && value !== "BETWEEN") {
          updated.valueTo = "";
        }
        return updated;
      }
      return f;
    });
    onFiltersChange(updatedFilters);
  };

  const clearAllFilters = () => {
    onFiltersChange([]);
  };

  const getFieldById = (fieldId) => {
    return filterableFields.find((f) => f.fieldId === fieldId);
  };

  const isRangeOperator = (operator) => operator === "BETWEEN";
  const isNullOperator = (operator) =>
    operator === "IS_NULL" || operator === "IS_NOT_NULL";

  const formatDate = (date) => {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
  };

  const renderValueInput = (filter, field) => {
    if (isNullOperator(filter.operator)) {
      return null;
    }

    const isDateField =
      field?.dataType === "DATE" || field?.dataType === "DATETIME";

    if (isDateField) {
      return (
        <>
          <DatePicker
            datePickerType="single"
            dateFormat="Y-m-d"
            onChange={(dates) => {
              if (dates && dates[0]) {
                updateFilter(filter.id, "value", formatDate(dates[0]));
              }
            }}
          >
            <DatePickerInput
              id={`filter-value-${filter.id}`}
              labelText={intl.formatMessage({ id: "adhoc.filter.value" })}
              placeholder="YYYY-MM-DD"
              value={filter.value}
              onChange={(e) => updateFilter(filter.id, "value", e.target.value)}
            />
          </DatePicker>
          {isRangeOperator(filter.operator) && (
            <DatePicker
              datePickerType="single"
              dateFormat="Y-m-d"
              onChange={(dates) => {
                if (dates && dates[0]) {
                  updateFilter(filter.id, "valueTo", formatDate(dates[0]));
                }
              }}
            >
              <DatePickerInput
                id={`filter-valueTo-${filter.id}`}
                labelText={intl.formatMessage({ id: "adhoc.filter.valueTo" })}
                placeholder="YYYY-MM-DD"
                value={filter.valueTo}
                onChange={(e) =>
                  updateFilter(filter.id, "valueTo", e.target.value)
                }
              />
            </DatePicker>
          )}
        </>
      );
    }

    return (
      <>
        <TextInput
          id={`filter-value-${filter.id}`}
          className="value-input"
          labelText={intl.formatMessage({ id: "adhoc.filter.value" })}
          value={filter.value}
          onChange={(e) => updateFilter(filter.id, "value", e.target.value)}
        />
        {isRangeOperator(filter.operator) && (
          <TextInput
            id={`filter-valueTo-${filter.id}`}
            className="value-input"
            labelText={intl.formatMessage({ id: "adhoc.filter.valueTo" })}
            value={filter.valueTo}
            onChange={(e) => updateFilter(filter.id, "valueTo", e.target.value)}
          />
        )}
      </>
    );
  };

  return (
    <div className="adhoc-filter-builder">
      <h3 className="adhoc-section-title">
        {intl.formatMessage({ id: "adhoc.step.addFilters" })}
      </h3>
      <p className="adhoc-section-description">
        {intl.formatMessage({ id: "adhoc.step.addFilters.description" })}
      </p>

      <div className="adhoc-filter-rows-container">
        {filters.length === 0 ? (
          <div className="adhoc-no-filters-message">
            {intl.formatMessage({ id: "adhoc.filter.noFilters" })}
          </div>
        ) : (
          filters.map((filter) => {
            const field = getFieldById(filter.fieldId);
            const operators = getOperatorsForField(field);

            return (
              <div
                key={filter.id}
                className={`adhoc-filter-row ${isRangeOperator(filter.operator) ? "has-range" : ""}`}
              >
                <Select
                  id={`filter-field-${filter.id}`}
                  className="field-select"
                  labelText={intl.formatMessage({ id: "adhoc.filter.field" })}
                  value={filter.fieldId}
                  onChange={(e) =>
                    updateFilter(filter.id, "fieldId", e.target.value)
                  }
                >
                  <SelectItem
                    value=""
                    text={intl.formatMessage({ id: "adhoc.filter.selectField" })}
                  />
                  <SelectItem disabled text="— Patient —" />
                  {filterableFields
                    .filter((f) => f.entityName === "patient")
                    .map((f) => (
                      <SelectItem
                        key={f.fieldId}
                        value={f.fieldId}
                        text={f.displayName}
                      />
                    ))}
                  <SelectItem disabled text="— Sample —" />
                  {filterableFields
                    .filter((f) => f.entityName === "sample")
                    .map((f) => (
                      <SelectItem
                        key={f.fieldId}
                        value={f.fieldId}
                        text={f.displayName}
                      />
                    ))}
                </Select>

                <Select
                  id={`filter-operator-${filter.id}`}
                  className="operator-select"
                  labelText={intl.formatMessage({ id: "adhoc.filter.operator" })}
                  value={filter.operator}
                  onChange={(e) =>
                    updateFilter(filter.id, "operator", e.target.value)
                  }
                  disabled={!filter.fieldId}
                >
                  {operators.map((op) => (
                    <SelectItem key={op.value} value={op.value} text={op.label} />
                  ))}
                </Select>

                {renderValueInput(filter, field)}

                <Button
                  kind="danger--ghost"
                  size="sm"
                  hasIconOnly
                  renderIcon={TrashCan}
                  iconDescription={intl.formatMessage({
                    id: "adhoc.filter.remove",
                  })}
                  onClick={() => removeFilter(filter.id)}
                  data-testid="remove-filter-btn"
                />
              </div>
            );
          })
        )}
      </div>

      <div className="adhoc-filter-actions">
        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Add}
          onClick={addFilter}
          data-testid="add-filter-btn"
        >
          {intl.formatMessage({ id: "adhoc.filter.add" })}
        </Button>
        {filters.length > 0 && (
          <Button kind="ghost" size="sm" onClick={clearAllFilters}>
            {intl.formatMessage({ id: "adhoc.filter.clearAll" })}
          </Button>
        )}
      </div>
    </div>
  );
};

export default FilterBuilder;
