import React, { useContext, useEffect, useState } from "react";
import {
  Stack,
  Select,
  SelectItem,
  TextInput,
  TextArea,
  Checkbox,
  Toggle,
  Button,
  Loading,
  InlineNotification,
} from "@carbon/react";
import { RecentlyViewed } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  putToOpenElisServer,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";
import StorageHistoryModal from "./StorageHistoryModal";

/**
 * OGC-949 M8 / OGC-977..979 — Sample Storage section.
 *
 * Edits a test's storage conditions + max duration + stability notes (OGC-977),
 * special-handling flags + disposal (OGC-978), and the override-restricted flag
 * (OGC-979 — v1 records the flag; in-progress-order behavior is order-entry's
 * concern). Persisted as a singleton via PUT /rest/test-catalog/tests/{id}/storage.
 */
const CONDITIONS = ["REFRIGERATED", "FROZEN", "DEEP_FROZEN", "AMBIENT"];
const DISPOSALS = ["INCINERATION", "AUTOCLAVE", "BIOHAZARD", "STANDARD"];
const UNITS = ["hours", "days", "weeks", "months"];

const toInt = (v) => {
  if (v === "" || v === null || v === undefined) {
    return null;
  }
  const n = parseInt(v, 10);
  return Number.isNaN(n) ? null : n;
};

const StorageSection = ({ testId }) => {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState(null);
  const [historyOpen, setHistoryOpen] = useState(false);

  const load = () => {
    setLoading(true);
    setError(false);
    getFromOpenElisServer(
      `/rest/test-catalog/tests/${testId}/storage`,
      (res) => {
        setLoading(false);
        if (!res) {
          setError(true);
          return;
        }
        setForm(res);
      },
    );
  };

  useEffect(() => {
    if (!testId) {
      return;
    }
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [testId]);

  const update = (patch) => setForm((prev) => ({ ...prev, ...patch }));

  const handleSave = () => {
    setSaving(true);
    const payload = {
      ...form,
      storageDuration: toInt(form.storageDuration),
      disposalTimeframe: toInt(form.disposalTimeframe),
    };
    putToOpenElisServer(
      `/rest/test-catalog/tests/${testId}/storage`,
      JSON.stringify(payload),
      (status) => {
        setSaving(false);
        setNotificationVisible(true);
        if (status === 200) {
          addNotification({
            kind: "success",
            title: intl.formatMessage({
              id: "label.testCatalog.section.storage",
            }),
            message: intl.formatMessage({
              id: "label.testCatalog.storage.saved",
            }),
          });
          load();
        } else {
          addNotification({
            kind: "error",
            title: intl.formatMessage({ id: "error.title" }),
            message: intl.formatMessage({ id: "server.error.msg" }),
          });
        }
      },
    );
  };

  if (loading) {
    return (
      <Loading
        description={intl.formatMessage({ id: "label.loading" })}
        withOverlay={false}
      />
    );
  }
  if (error || !form) {
    return (
      <InlineNotification
        kind="error"
        lowContrast
        hideCloseButton
        title={intl.formatMessage({ id: "error.title" })}
        subtitle={intl.formatMessage({
          id: "label.testCatalog.storage.loadError",
        })}
      />
    );
  }

  const noneOption = (
    <SelectItem
      value=""
      text={intl.formatMessage({ id: "label.testCatalog.storage.none" })}
    />
  );

  return (
    <Stack gap={6} data-testid="storage-section">
      <div style={{ display: "flex", justifyContent: "flex-end" }}>
        <Button
          kind="ghost"
          size="sm"
          renderIcon={RecentlyViewed}
          onClick={() => setHistoryOpen(true)}
          data-testid="storage-history-button"
        >
          {intl.formatMessage({ id: "label.testCatalog.storage.history.view" })}
        </Button>
      </div>
      <StorageHistoryModal
        open={historyOpen}
        onClose={() => setHistoryOpen(false)}
        testId={testId}
      />
      <h5>
        <FormattedMessage id="label.testCatalog.storage.storageHeading" />
      </h5>
      <Select
        id="storage-condition"
        labelText={intl.formatMessage({
          id: "label.testCatalog.storage.condition",
        })}
        value={form.storageCondition || ""}
        onChange={(e) => update({ storageCondition: e.target.value })}
      >
        {noneOption}
        {CONDITIONS.map((c) => (
          <SelectItem
            key={c}
            value={c}
            text={intl.formatMessage({
              id: `label.testCatalog.storage.condition.${c}`,
            })}
          />
        ))}
      </Select>
      <TextInput
        id="storage-condition-custom"
        labelText={intl.formatMessage({
          id: "label.testCatalog.storage.conditionCustom",
        })}
        value={form.storageConditionCustom || ""}
        onChange={(e) => update({ storageConditionCustom: e.target.value })}
      />
      <div style={{ display: "flex", gap: "1rem" }}>
        <TextInput
          id="storage-duration"
          type="number"
          labelText={intl.formatMessage({
            id: "label.testCatalog.storage.duration",
          })}
          value={form.storageDuration ?? ""}
          onChange={(e) => update({ storageDuration: e.target.value })}
        />
        <Select
          id="storage-duration-unit"
          labelText={intl.formatMessage({
            id: "label.testCatalog.storage.durationUnit",
          })}
          value={form.storageDurationUnit || ""}
          onChange={(e) => update({ storageDurationUnit: e.target.value })}
        >
          {noneOption}
          {UNITS.map((u) => (
            <SelectItem
              key={u}
              value={u}
              text={intl.formatMessage({
                id: `label.testCatalog.storage.unit.${u}`,
              })}
            />
          ))}
        </Select>
      </div>
      <TextArea
        id="storage-stability-notes"
        labelText={intl.formatMessage({
          id: "label.testCatalog.storage.stabilityNotes",
        })}
        value={form.stabilityNotes || ""}
        rows={2}
        onChange={(e) => update({ stabilityNotes: e.target.value })}
      />

      <h5>
        <FormattedMessage id="label.testCatalog.storage.handlingHeading" />
      </h5>
      <Checkbox
        id="storage-protect-from-light"
        labelText={intl.formatMessage({
          id: "label.testCatalog.storage.protectFromLight",
        })}
        checked={!!form.protectFromLight}
        onChange={(_e, { checked }) => update({ protectFromLight: checked })}
      />
      <Checkbox
        id="storage-do-not-freeze"
        labelText={intl.formatMessage({
          id: "label.testCatalog.storage.doNotFreeze",
        })}
        checked={!!form.doNotFreeze}
        onChange={(_e, { checked }) => update({ doNotFreeze: checked })}
      />
      <Checkbox
        id="storage-do-not-refrigerate"
        labelText={intl.formatMessage({
          id: "label.testCatalog.storage.doNotRefrigerate",
        })}
        checked={!!form.doNotRefrigerate}
        onChange={(_e, { checked }) => update({ doNotRefrigerate: checked })}
      />

      <h5>
        <FormattedMessage id="label.testCatalog.storage.disposalHeading" />
      </h5>
      <Select
        id="storage-disposal-method"
        labelText={intl.formatMessage({
          id: "label.testCatalog.storage.disposalMethod",
        })}
        value={form.disposalMethod || ""}
        onChange={(e) => update({ disposalMethod: e.target.value })}
      >
        {noneOption}
        {DISPOSALS.map((d) => (
          <SelectItem
            key={d}
            value={d}
            text={intl.formatMessage({
              id: `label.testCatalog.storage.disposal.${d}`,
            })}
          />
        ))}
      </Select>
      <div style={{ display: "flex", gap: "1rem" }}>
        <TextInput
          id="storage-disposal-timeframe"
          type="number"
          labelText={intl.formatMessage({
            id: "label.testCatalog.storage.disposalTimeframe",
          })}
          value={form.disposalTimeframe ?? ""}
          onChange={(e) => update({ disposalTimeframe: e.target.value })}
        />
        <Select
          id="storage-disposal-unit"
          labelText={intl.formatMessage({
            id: "label.testCatalog.storage.disposalUnit",
          })}
          value={form.disposalUnit || ""}
          onChange={(e) => update({ disposalUnit: e.target.value })}
        >
          {noneOption}
          {UNITS.map((u) => (
            <SelectItem
              key={u}
              value={u}
              text={intl.formatMessage({
                id: `label.testCatalog.storage.unit.${u}`,
              })}
            />
          ))}
        </Select>
      </div>
      <TextArea
        id="storage-special-instructions"
        labelText={intl.formatMessage({
          id: "label.testCatalog.storage.specialInstructions",
        })}
        value={form.specialInstructions || ""}
        rows={2}
        onChange={(e) => update({ specialInstructions: e.target.value })}
      />

      <Toggle
        id="storage-override-restricted"
        labelText={intl.formatMessage({
          id: "label.testCatalog.storage.overrideRestricted",
        })}
        labelA={intl.formatMessage({ id: "label.no" })}
        labelB={intl.formatMessage({ id: "label.yes" })}
        toggled={!!form.overrideRestricted}
        onToggle={(checked) => update({ overrideRestricted: checked })}
      />

      <div>
        <Button kind="primary" disabled={saving} onClick={handleSave}>
          <FormattedMessage id="label.button.save" />
        </Button>
      </div>
    </Stack>
  );
};

export default StorageSection;
