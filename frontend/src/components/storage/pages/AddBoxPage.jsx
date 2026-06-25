import React, { useEffect, useMemo, useState } from "react";
import { useHistory } from "react-router-dom";
import {
  Button,
  Checkbox,
  Dropdown,
  InlineLoading,
  InlineNotification,
  NumberInput,
  TextInput,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import BreadcrumbNav from "../components/BreadcrumbNav";
import { getFromOpenElisServer } from "../../utils/Utils";
import useCreateLocation from "./hooks/useCreateLocation";

const BOX_PRESETS = [
  { id: "9x9", label: "9x9", rows: 9, columns: 9 },
  { id: "10x10", label: "10x10", rows: 10, columns: 10 },
  { id: "8x12", label: "8x12 (96-well plate)", rows: 8, columns: 12 },
  { id: "4x6", label: "4x6", rows: 4, columns: 6 },
  { id: "6x8", label: "6x8", rows: 6, columns: 8 },
  { id: "16x24", label: "16x24 (384-well plate)", rows: 16, columns: 24 },
  { id: "custom", label: "Custom", rows: null, columns: null },
];

export default function AddBoxPage() {
  const intl = useIntl();
  const history = useHistory();
  const createLocation = useCreateLocation();
  const [rackOptions, setRackOptions] = useState([]);
  const [loadingRacks, setLoadingRacks] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [selectedPreset, setSelectedPreset] = useState(BOX_PRESETS[2]);
  const [formData, setFormData] = useState({
    label: "",
    code: "",
    parentRackId: "",
    rows: "8",
    columns: "12",
    active: true,
  });

  useEffect(() => {
    setLoadingRacks(true);
    getFromOpenElisServer("/rest/storage/racks", (response) => {
      if (Array.isArray(response)) {
        setRackOptions(response);
      }
      setLoadingRacks(false);
    });
  }, []);

  const crumbs = useMemo(
    () => [
      {
        label: intl.formatMessage({
          id: "storage.breadcrumb.storage",
          defaultMessage: "Storage",
        }),
        href: "/Storage",
      },
      {
        label: intl.formatMessage({
          id: "storage.nav.boxes",
          defaultMessage: "Boxes",
        }),
        href: "/Storage/boxes",
      },
      {
        label: intl.formatMessage({
          id: "storage.add.box",
          defaultMessage: "Add Box",
        }),
        href: "/Storage/boxes/new",
      },
    ],
    [intl],
  );

  const updateField = (key, value) => {
    setFormData((prev) => ({ ...prev, [key]: value }));
  };

  const navigateBack = () => {
    history.push(`/Storage/boxes?t=${Date.now()}`);
  };

  const handlePresetChange = ({ selectedItem }) => {
    if (!selectedItem) return;
    setSelectedPreset(selectedItem);
    if (selectedItem.id !== "custom") {
      setFormData((prev) => ({
        ...prev,
        rows: String(selectedItem.rows),
        columns: String(selectedItem.columns),
      }));
    }
  };

  const handleSubmit = async () => {
    setSaving(true);
    setError(null);

    const payload = {
      label: formData.label.trim(),
      code: formData.code.trim() || null,
      parentRackId: formData.parentRackId || null,
      rows: formData.rows ? Number.parseInt(formData.rows, 10) : null,
      columns: formData.columns ? Number.parseInt(formData.columns, 10) : null,
      active: formData.active,
    };

    try {
      await createLocation("boxes", payload);
      navigateBack();
    } catch (e) {
      setError(
        e?.message ||
          intl.formatMessage({
            id: "storage.edit.error.saveFailed",
            defaultMessage: "Save failed",
          }),
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="storage-add-page pageContent">
      <BreadcrumbNav crumbs={crumbs} />
      <h1>
        <FormattedMessage id="storage.add.box" defaultMessage="Add Box" />
      </h1>

      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "label.error",
            defaultMessage: "Error",
          })}
          subtitle={error}
          lowContrast
        />
      )}

      <div className="storage-edit-page-form" style={{ maxWidth: "32rem" }}>
        <TextInput
          id="box-add-label"
          labelText={intl.formatMessage({
            id: "label.label",
            defaultMessage: "Label",
          })}
          value={formData.label}
          onChange={(e) => updateField("label", e.target.value)}
        />
        <TextInput
          id="box-add-code"
          labelText={intl.formatMessage({
            id: "label.code",
            defaultMessage: "Code",
          })}
          value={formData.code}
          onChange={(e) => updateField("code", e.target.value)}
        />

        {loadingRacks ? (
          <InlineLoading
            description={intl.formatMessage({
              id: "label.loading",
              defaultMessage: "Loading...",
            })}
          />
        ) : (
          <Dropdown
            id="box-add-rack"
            titleText={intl.formatMessage({
              id: "storage.nav.rack",
              defaultMessage: "Rack",
            })}
            label={intl.formatMessage({
              id: "storage.editbox.selectRack",
              defaultMessage: "Select rack",
            })}
            items={rackOptions}
            itemToString={(item) => (item ? item.label || item.name || "" : "")}
            selectedItem={
              rackOptions.find(
                (rack) => String(rack.id) === String(formData.parentRackId),
              ) || null
            }
            onChange={({ selectedItem }) =>
              updateField(
                "parentRackId",
                selectedItem ? String(selectedItem.id) : "",
              )
            }
          />
        )}

        <Dropdown
          id="box-add-preset"
          titleText={intl.formatMessage({
            id: "storage.box.preset",
            defaultMessage: "Dimension preset",
          })}
          label={intl.formatMessage({
            id: "storage.box.preset.select",
            defaultMessage: "Select dimensions",
          })}
          items={BOX_PRESETS}
          itemToString={(item) => (item ? item.label : "")}
          selectedItem={selectedPreset}
          onChange={handlePresetChange}
        />

        <NumberInput
          id="box-add-rows"
          label={intl.formatMessage({
            id: "storage.box.rows",
            defaultMessage: "Rows",
          })}
          value={formData.rows}
          disabled={selectedPreset.id !== "custom"}
          onChange={(_event, { value }) =>
            updateField("rows", String(value ?? ""))
          }
          min={0}
        />
        <NumberInput
          id="box-add-columns"
          label={intl.formatMessage({
            id: "storage.box.columns",
            defaultMessage: "Columns",
          })}
          value={formData.columns}
          disabled={selectedPreset.id !== "custom"}
          onChange={(_event, { value }) =>
            updateField("columns", String(value ?? ""))
          }
          min={0}
        />

        <Checkbox
          id="box-add-active"
          labelText={intl.formatMessage({
            id: "label.active",
            defaultMessage: "Active",
          })}
          checked={formData.active}
          onChange={(_event, { checked }) => updateField("active", checked)}
        />
      </div>

      <div className="storage-edit-page-actions" style={{ marginTop: "1rem" }}>
        <Button kind="secondary" onClick={navigateBack}>
          <FormattedMessage id="label.cancel" defaultMessage="Cancel" />
        </Button>
        <Button
          kind="primary"
          onClick={handleSubmit}
          disabled={
            saving ||
            !formData.label.trim() ||
            !formData.code.trim() ||
            !formData.parentRackId
          }
        >
          <FormattedMessage id="label.add" defaultMessage="Add" />
        </Button>
      </div>
    </div>
  );
}
