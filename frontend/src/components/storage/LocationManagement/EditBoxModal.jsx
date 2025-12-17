import React, { useEffect, useMemo, useState } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  TextInput,
  Dropdown,
  Toggle,
  InlineNotification,
} from "@carbon/react";
import { useIntl } from "react-intl";
import config from "../../../config.json";

const DEFAULT_FORM = {
  label: "",
  code: "",
  type: "",
  rows: "",
  columns: "",
  positionSchemaHint: "letter-number",
  active: true,
};

/**
 * Create/Edit modal for StorageBox (Box/Plate)
 *
 * Props:
 * - open: boolean
 * - mode: "create" | "edit"
 * - box: existing box object when editing
 * - parentRack: selected rack object (required for create)
 * - onClose: () => void
 * - onSave: (savedBox) => void
 */
const EditBoxModal = ({ open, mode, box, parentRack, onClose, onSave }) => {
  const intl = useIntl();
  const isEdit = mode === "edit";

  const [formData, setFormData] = useState(DEFAULT_FORM);
  const [error, setError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const typeItems = useMemo(
    () => [
      {
        id: "",
        label: intl.formatMessage({
          id: "label.select",
          defaultMessage: "Select",
        }),
      },
      {
        id: "plate",
        label: intl.formatMessage({
          id: "storage.box.type.plate",
          defaultMessage: "Plate",
        }),
      },
      {
        id: "box",
        label: intl.formatMessage({
          id: "storage.box.type.box",
          defaultMessage: "Box",
        }),
      },
      {
        id: "96-well",
        label: intl.formatMessage({
          id: "storage.box.type.96well",
          defaultMessage: "96-well",
        }),
      },
      {
        id: "24-well",
        label: intl.formatMessage({
          id: "storage.box.type.24well",
          defaultMessage: "24-well",
        }),
      },
      {
        id: "other",
        label: intl.formatMessage({
          id: "storage.box.type.other",
          defaultMessage: "Other",
        }),
      },
    ],
    [intl],
  );

  const schemaItems = useMemo(
    () => [
      {
        id: "letter-number",
        label: intl.formatMessage({
          id: "storage.box.schema.letterNumber",
          defaultMessage: "Letter-Number (A1)",
        }),
      },
      {
        id: "number-number",
        label: intl.formatMessage({
          id: "storage.box.schema.numberNumber",
          defaultMessage: "Number-Number (1-1)",
        }),
      },
    ],
    [intl],
  );

  useEffect(() => {
    if (!open) {
      setError(null);
      setIsSubmitting(false);
      setFormData(DEFAULT_FORM);
      return;
    }

    if (isEdit && box) {
      setFormData({
        label: box.label ?? "",
        code: box.code ?? "",
        type: box.type ?? "",
        rows: box.rows != null ? String(box.rows) : "",
        columns: box.columns != null ? String(box.columns) : "",
        positionSchemaHint: box.positionSchemaHint ?? "letter-number",
        active: !!box.active,
      });
    } else {
      setFormData(DEFAULT_FORM);
    }
  }, [open, isEdit, box]);

  const handleFieldChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async () => {
    if (!formData.label || !formData.code) {
      setError(
        intl.formatMessage({
          id: "storage.box.validation.required",
          defaultMessage: "Label and Code are required.",
        }),
      );
      return;
    }

    if (!isEdit && !parentRack?.id) {
      setError(
        intl.formatMessage({
          id: "storage.box.validation.parentRack.required",
          defaultMessage: "Please select a rack first.",
        }),
      );
      return;
    }

    const endpoint = isEdit
      ? `/rest/storage/boxes/${box.id}`
      : `/rest/storage/boxes`;
    const method = isEdit ? "PUT" : "POST";

    const payload = {
      label: formData.label,
      code: formData.code,
      type: formData.type || null,
      rows: parseInt(formData.rows, 10),
      columns: parseInt(formData.columns, 10),
      positionSchemaHint: formData.positionSchemaHint || null,
      active: !!formData.active,
      parentRackId: isEdit
        ? String(box.parentRackId ?? parentRack?.id ?? "")
        : String(parentRack.id),
    };

    setIsSubmitting(true);
    setError(null);

    try {
      const response = await fetch(config.serverBaseUrl + endpoint, {
        method,
        headers: {
          "Content-Type": "application/json",
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
        credentials: "include",
        body: JSON.stringify(payload),
      });

      if (response.status >= 200 && response.status < 300) {
        const saved = await response.json().catch(() => payload);
        if (onSave) {
          onSave(saved);
        }
        if (onClose) {
          onClose();
        }
        return;
      }

      let message = intl.formatMessage({
        id: "storage.box.save.error",
        defaultMessage: "Unable to save box.",
      });

      const contentType = response.headers.get("content-type");
      if (contentType && contentType.includes("application/json")) {
        try {
          const errorData = await response.json();
          message = errorData?.error || errorData?.message || message;
        } catch (e) {
          // ignore JSON parsing errors
        }
      }

      setError(message);
    } catch (e) {
      setError(
        e?.message ||
          intl.formatMessage({
            id: "storage.box.save.error",
            defaultMessage: "Unable to save box.",
          }),
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <ComposedModal open={open} onClose={onClose} size="md">
      <ModalHeader
        title={intl.formatMessage({
          id: isEdit ? "storage.box.edit.title" : "storage.box.create.title",
          defaultMessage: isEdit ? "Edit Box/Plate" : "Create Box/Plate",
        })}
      />
      <ModalBody>
        {error && (
          <InlineNotification
            kind="error"
            lowContrast
            title={intl.formatMessage({
              id: "storage.box.save.error.title",
              defaultMessage: "Error",
            })}
            subtitle={error}
          />
        )}

        <TextInput
          id="box-label"
          data-testid="box-label"
          labelText={intl.formatMessage({
            id: "storage.box.label",
            defaultMessage: "Label",
          })}
          value={formData.label}
          onChange={(e) => handleFieldChange("label", e.target.value)}
        />

        <TextInput
          id="box-code"
          data-testid="box-code"
          labelText={intl.formatMessage({
            id: "storage.box.code",
            defaultMessage: "Code",
          })}
          helperText={intl.formatMessage({
            id: "storage.box.code.helper",
            defaultMessage: "Max 10 characters.",
          })}
          value={formData.code}
          onChange={(e) => handleFieldChange("code", e.target.value)}
        />

        <Dropdown
          id="box-type"
          data-testid="box-type"
          label={intl.formatMessage({
            id: "storage.box.type",
            defaultMessage: "Type",
          })}
          titleText={intl.formatMessage({
            id: "storage.box.type",
            defaultMessage: "Type",
          })}
          items={typeItems}
          selectedItem={
            typeItems.find((t) => t.id === formData.type) || typeItems[0]
          }
          itemToString={(item) => item?.label || ""}
          onChange={({ selectedItem }) =>
            handleFieldChange("type", selectedItem?.id || "")
          }
        />

        <div
          style={{
            display: "grid",
            gridTemplateColumns: "1fr 1fr",
            gap: "1rem",
          }}
        >
          <TextInput
            id="box-rows"
            data-testid="box-rows"
            labelText={intl.formatMessage({
              id: "storage.box.rows",
              defaultMessage: "Rows",
            })}
            type="number"
            value={formData.rows}
            onChange={(e) => handleFieldChange("rows", e.target.value)}
          />
          <TextInput
            id="box-columns"
            data-testid="box-columns"
            labelText={intl.formatMessage({
              id: "storage.box.columns",
              defaultMessage: "Columns",
            })}
            type="number"
            value={formData.columns}
            onChange={(e) => handleFieldChange("columns", e.target.value)}
          />
        </div>

        <Dropdown
          id="box-position-schema"
          data-testid="box-position-schema"
          label={intl.formatMessage({
            id: "storage.box.positionSchemaHint",
            defaultMessage: "Position schema",
          })}
          titleText={intl.formatMessage({
            id: "storage.box.positionSchemaHint",
            defaultMessage: "Position schema",
          })}
          items={schemaItems}
          selectedItem={
            schemaItems.find((s) => s.id === formData.positionSchemaHint) ||
            schemaItems[0]
          }
          itemToString={(item) => item?.label || ""}
          onChange={({ selectedItem }) =>
            handleFieldChange(
              "positionSchemaHint",
              selectedItem?.id || "letter-number",
            )
          }
        />

        <Toggle
          id="box-active"
          data-testid="box-active"
          labelText={intl.formatMessage({
            id: "storage.box.active",
            defaultMessage: "Active",
          })}
          toggled={!!formData.active}
          onToggle={(checked) => handleFieldChange("active", checked)}
        />
      </ModalBody>
      <ModalFooter>
        <Button kind="secondary" onClick={onClose}>
          {intl.formatMessage({
            id: "button.cancel",
            defaultMessage: "Cancel",
          })}
        </Button>
        <Button kind="primary" disabled={isSubmitting} onClick={handleSubmit}>
          {intl.formatMessage({ id: "button.save", defaultMessage: "Save" })}
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

export default EditBoxModal;
