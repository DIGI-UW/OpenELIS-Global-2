import React, { useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Button,
  Checkbox,
  InlineNotification,
  Select,
  SelectItem,
  StructuredListBody,
  StructuredListCell,
  StructuredListRow,
  StructuredListWrapper,
  Tag,
} from "@carbon/react";
import { ArrowDown, ArrowUp, TrashCan } from "@carbon/icons-react";
import {
  LAB_NUMBER,
  SELECTABLE_FIELD_KEYS,
  addField,
  fieldLabelId,
  fieldsLikelyOverflow,
  estimateFittingRows,
  selectableCount,
  moveField,
  removeField,
  toggleRequired,
} from "./labelFieldCatalog";

/**
 * Content fields of a label preset (OGC-1218): Lab Number locked at position
 * 1, up to fifteen selectable fields in display order, each optionally
 * required. Reordering uses buttons, so it works from the keyboard as well as
 * with a pointer. A soft hint warns when the count is unlikely to fit the
 * preset's height; it never blocks the save.
 */
function LabelPresetFieldsEditor({ fields, heightMm, onChange, disabled }) {
  const intl = useIntl();
  const [pendingKey, setPendingKey] = useState("");

  const nameOf = (key) => {
    const id = fieldLabelId(key);
    return id ? intl.formatMessage({ id }) : key;
  };
  const available = SELECTABLE_FIELD_KEYS.filter(
    (key) => !fields.some((f) => f.fieldKey === key),
  );
  const overflow = fieldsLikelyOverflow(fields, heightMm);

  const add = () => {
    if (!pendingKey) {
      return;
    }
    onChange(addField(fields, pendingKey));
    setPendingKey("");
  };

  return (
    <div data-testid="label-preset-fields">
      <p className="helper-text">
        <FormattedMessage id="admin.labelPresets.fields.helper" />
      </p>
      <StructuredListWrapper isCondensed isFlush>
        <StructuredListBody>
          {fields.map((field, index) => {
            const locked = field.fieldKey === LAB_NUMBER;
            const name = nameOf(field.fieldKey);
            return (
              <StructuredListRow
                key={field.fieldKey}
                data-testid={`label-field-row-${field.fieldKey}`}
              >
                <StructuredListCell>
                  <span className="label-field-position">
                    {intl.formatMessage(
                      { id: "admin.labelPresets.fields.position" },
                      { position: field.displayOrder },
                    )}
                  </span>
                </StructuredListCell>
                <StructuredListCell>
                  {name}
                  {locked && (
                    <Tag type="blue" size="sm" style={{ marginLeft: "0.5rem" }}>
                      <FormattedMessage id="admin.labelPresets.fields.locked" />
                    </Tag>
                  )}
                </StructuredListCell>
                <StructuredListCell>
                  <Checkbox
                    id={`label-field-required-${field.fieldKey}`}
                    labelText={intl.formatMessage({
                      id: "admin.labelPresets.fields.required",
                    })}
                    checked={Boolean(field.isRequired)}
                    disabled={disabled || locked}
                    onChange={() =>
                      onChange(toggleRequired(fields, field.fieldKey))
                    }
                  />
                </StructuredListCell>
                <StructuredListCell>
                  {!locked && (
                    <>
                      <Button
                        kind="ghost"
                        size="sm"
                        hasIconOnly
                        renderIcon={ArrowUp}
                        iconDescription={intl.formatMessage(
                          { id: "admin.labelPresets.fields.moveUp" },
                          { field: name },
                        )}
                        disabled={disabled || index <= 1}
                        onClick={() =>
                          onChange(moveField(fields, field.fieldKey, -1))
                        }
                        data-testid={`label-field-up-${field.fieldKey}`}
                      />
                      <Button
                        kind="ghost"
                        size="sm"
                        hasIconOnly
                        renderIcon={ArrowDown}
                        iconDescription={intl.formatMessage(
                          { id: "admin.labelPresets.fields.moveDown" },
                          { field: name },
                        )}
                        disabled={disabled || index >= fields.length - 1}
                        onClick={() =>
                          onChange(moveField(fields, field.fieldKey, 1))
                        }
                        data-testid={`label-field-down-${field.fieldKey}`}
                      />
                      <Button
                        kind="ghost"
                        size="sm"
                        hasIconOnly
                        renderIcon={TrashCan}
                        iconDescription={intl.formatMessage(
                          { id: "admin.labelPresets.fields.remove" },
                          { field: name },
                        )}
                        disabled={disabled}
                        onClick={() =>
                          onChange(removeField(fields, field.fieldKey))
                        }
                        data-testid={`label-field-remove-${field.fieldKey}`}
                      />
                    </>
                  )}
                </StructuredListCell>
              </StructuredListRow>
            );
          })}
        </StructuredListBody>
      </StructuredListWrapper>

      {available.length === 0 ? (
        <p className="helper-text">
          <FormattedMessage id="admin.labelPresets.fields.allAdded" />
        </p>
      ) : (
        <div style={{ display: "flex", alignItems: "flex-end", gap: "0.5rem" }}>
          <Select
            id="label-field-add"
            labelText={intl.formatMessage({
              id: "admin.labelPresets.fields.add.label",
            })}
            value={pendingKey}
            disabled={disabled}
            onChange={(event) => setPendingKey(event.target.value)}
          >
            <SelectItem
              value=""
              text={intl.formatMessage({
                id: "admin.labelPresets.fields.add.placeholder",
              })}
            />
            {available.map((key) => (
              <SelectItem key={key} value={key} text={nameOf(key)} />
            ))}
          </Select>
          <Button
            kind="tertiary"
            size="md"
            disabled={disabled || !pendingKey}
            onClick={add}
            data-testid="label-field-add-button"
          >
            <FormattedMessage id="admin.labelPresets.fields.add.button" />
          </Button>
        </div>
      )}

      {overflow && (
        <InlineNotification
          kind="warning"
          lowContrast
          hideCloseButton
          role="status"
          title={intl.formatMessage(
            { id: "admin.labelPresets.fields.fitHint" },
            {
              count: selectableCount(fields),
              height: heightMm,
              rows: estimateFittingRows(heightMm),
            },
          )}
          data-testid="label-fields-fit-hint"
        />
      )}
    </div>
  );
}

export default LabelPresetFieldsEditor;
