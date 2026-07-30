import React, { useCallback, useEffect, useState } from "react";
import {
  Button,
  Dropdown,
  InlineLoading,
  InlineNotification,
  Toggle,
} from "@carbon/react";
import { Add } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  putToOpenElisServerFullResponse,
} from "../../../utils/Utils";
import LinkedPresetsTable from "./LinkedPresetsTable";
import OrderEntryPreview from "./OrderEntryPreview";

/**
 * LabelsTab (OGC-285 M4, T111).
 *
 * Mounted inside ViewTestCatalog as a temporary Carbon Tabs shim
 * (see T114 / TODO OGC-746). Once OGC-746 ships, the dedicated Test Editor
 * modal replaces this shim.
 *
 * Props:
 *   testId - string PK of the test being configured
 *
 * Data flow:
 *   GET  /rest/api/tests/{testId}/labelConfig  -> populate state
 *   PUT  /rest/api/tests/{testId}/labelConfig  -> persist on Save
 *   GET  /rest/api/labelPresets?status=ACTIVE  -> populate the "Add" dropdown
 */
function LabelsTab({ testId }) {
  const intl = useIntl();

  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [saveSuccess, setSaveSuccess] = useState(false);

  // Current config state
  const [masterOverride, setMasterOverride] = useState(true);
  const [links, setLinks] = useState([]);

  // Available presets for the "Add" dropdown (active, per-sample only)
  const [availablePresets, setAvailablePresets] = useState([]);

  // Fetch current config on mount / testId change
  useEffect(() => {
    if (!testId) return;
    setLoading(true);
    setError(null);
    getFromOpenElisServer(`/rest/api/tests/${testId}/labelConfig`, (data) => {
      if (data) {
        setMasterOverride(data.allowOrderEntryOverride !== false);
        setLinks(Array.isArray(data.links) ? data.links : []);
      }
      setLoading(false);
    });
  }, [testId]);

  // Fetch available presets (active) for the dropdown
  useEffect(() => {
    getFromOpenElisServer("/rest/api/labelPresets?status=ACTIVE", (data) => {
      if (Array.isArray(data)) {
        // Filter to per-sample presets only (prints_per_sample = true)
        setAvailablePresets(data.filter((p) => p.prints_per_sample !== false));
      }
    });
  }, []);

  const linkedPresetIds = new Set(links.map((l) => l.presetId));

  // Available for adding: active per-sample presets not already linked
  const addablePresets = availablePresets.filter(
    (p) => !linkedPresetIds.has(p.id),
  );

  const handleLinkChange = useCallback((index, field, value) => {
    setLinks((prev) => {
      const updated = [...prev];
      updated[index] = { ...updated[index], [field]: value };
      return updated;
    });
  }, []);

  const handleRemove = useCallback((index) => {
    setLinks((prev) => prev.filter((_, i) => i !== index));
  }, []);

  const handleAddPreset = ({ selectedItem }) => {
    if (!selectedItem) return;
    setLinks((prev) => [
      ...prev,
      {
        presetId: selectedItem.id,
        presetName: selectedItem.name,
        defaultQty: selectedItem.default_per_sample ?? 0,
        maxQty: selectedItem.max_per_sample ?? 10,
        allowOverride: true,
      },
    ]);
  };

  const handleSave = () => {
    setSaving(true);
    setSaveSuccess(false);
    setError(null);

    const payload = {
      allowOrderEntryOverride: masterOverride,
      links: links.map((l) => ({
        presetId: l.presetId,
        defaultQty: l.defaultQty,
        maxQty: l.maxQty,
        allowOverride: l.allowOverride,
      })),
    };

    putToOpenElisServerFullResponse(
      `/rest/api/tests/${testId}/labelConfig`,
      JSON.stringify(payload),
      (response) => {
        setSaving(false);
        if (!response) {
          setError(
            intl.formatMessage({ id: "admin.testCatalog.labels.save.error" }),
          );
          return;
        }
        if (response.ok) {
          response.json().then((data) => {
            setLinks(data.links || []);
            setMasterOverride(data.allowOrderEntryOverride !== false);
            setSaveSuccess(true);
          });
        } else {
          setError(
            intl.formatMessage({ id: "admin.testCatalog.labels.save.error" }),
          );
        }
      },
    );
  };

  if (loading) {
    return (
      <InlineLoading
        description={intl.formatMessage({ id: "loading.label" })}
      />
    );
  }

  return (
    <div style={{ padding: "1rem 0" }}>
      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "admin.testCatalog.labels.save.error",
          })}
          subtitle={error}
          lowContrast
          onCloseButtonClick={() => setError(null)}
        />
      )}
      {saveSuccess && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({
            id: "admin.testCatalog.labels.save.success",
          })}
          lowContrast
          onCloseButtonClick={() => setSaveSuccess(false)}
        />
      )}

      {/* Master toggle (AC-12) */}
      <Toggle
        id="masterOverrideToggle"
        labelText={
          <FormattedMessage id="admin.testCatalog.labels.masterOverride.label" />
        }
        labelA={intl.formatMessage({
          id: "admin.testCatalog.labels.masterOverride.off",
        })}
        labelB={intl.formatMessage({
          id: "admin.testCatalog.labels.masterOverride.on",
        })}
        toggled={masterOverride}
        onToggle={(checked) => setMasterOverride(checked)}
      />

      {/* Linked presets table */}
      <div style={{ marginTop: "1.5rem" }}>
        <p
          className="cds--label"
          style={{ fontWeight: "600", marginBottom: "0.5rem" }}
        >
          <FormattedMessage id="admin.testCatalog.labels.linkedPresets.title" />
        </p>
        <LinkedPresetsTable
          links={links}
          masterOverride={masterOverride}
          onLinkChange={handleLinkChange}
          onRemove={handleRemove}
        />
      </div>

      {/* Add Label Type dropdown (AC-8/AC-11: excludes already-linked presets) */}
      <div
        style={{
          marginTop: "1rem",
          display: "flex",
          alignItems: "flex-end",
          gap: "0.5rem",
        }}
      >
        <Dropdown
          id="addPresetDropdown"
          titleText={
            <FormattedMessage id="admin.testCatalog.labels.addPreset.label" />
          }
          label={intl.formatMessage({
            id: "admin.testCatalog.labels.addPreset.placeholder",
          })}
          items={addablePresets}
          itemToString={(item) => (item ? item.name : "")}
          onChange={handleAddPreset}
          disabled={addablePresets.length === 0}
        />
        <Button
          renderIcon={Add}
          onClick={() => {
            // The Dropdown's onChange handles adding; this button is decorative
          }}
          disabled
          style={{ visibility: "hidden" }}
        >
          <FormattedMessage id="admin.testCatalog.labels.addPreset.label" />
        </Button>
      </div>

      {/* Order Entry Preview (AC-10) */}
      <OrderEntryPreview links={links} masterOverride={masterOverride} />

      {/* Save button */}
      <div style={{ marginTop: "1.5rem" }}>
        <Button onClick={handleSave} disabled={saving}>
          {saving ? (
            <InlineLoading
              description={intl.formatMessage({ id: "saving.label" })}
            />
          ) : (
            <FormattedMessage id="label.button.save" />
          )}
        </Button>
      </div>
    </div>
  );
}

export default LabelsTab;
