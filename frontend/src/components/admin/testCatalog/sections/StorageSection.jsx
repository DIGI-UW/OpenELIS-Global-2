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
// FR-86: full standard-lab value lists (stored as varchar codes in
// test_sample_handling — no schema change). Review the set with the lab lead.
const CONDITIONS = [
  "AMBIENT",
  "CONTROLLED_ROOM_TEMPERATURE",
  "COOL_ROOM",
  "COLD_ROOM",
  "REFRIGERATED",
  "FROZEN",
  "DEEP_FROZEN",
  "ULTRA_LOW_FREEZER",
  "WARM_INCUBATOR",
];
const DISPOSALS = [
  "INCINERATION",
  "AUTOCLAVE",
  "CHEMICAL_DISINFECTION",
  "BIOHAZARD",
  "SHARPS_CONTAINER",
  "SEWER",
  "RETURN_TO_SENDER",
  "ARCHIVE",
  "STANDARD",
];
const UNITS = ["hours", "days", "weeks", "months"];

const toInt = (v) => {
  if (v === "" || v === null || v === undefined) {
    return null;
  }
  const n = parseInt(v, 10);
  return Number.isNaN(n) ? null : n;
};

// Every editable field, with its label, in form order. The keys are the names
// the group save accepts in its list of changed fields.
const FIELD_LABELS = {
  storageCondition: "label.testCatalog.storage.condition",
  storageConditionCustom: "label.testCatalog.storage.conditionCustom",
  storageDuration: "label.testCatalog.storage.duration",
  storageDurationUnit: "label.testCatalog.storage.durationUnit",
  stabilityNotes: "label.testCatalog.storage.stabilityNotes",
  protectFromLight: "label.testCatalog.storage.protectFromLight",
  doNotFreeze: "label.testCatalog.storage.doNotFreeze",
  doNotRefrigerate: "label.testCatalog.storage.doNotRefrigerate",
  disposalMethod: "label.testCatalog.storage.disposalMethod",
  disposalTimeframe: "label.testCatalog.storage.disposalTimeframe",
  disposalUnit: "label.testCatalog.storage.disposalUnit",
  specialInstructions: "label.testCatalog.storage.specialInstructions",
  overrideRestricted: "label.testCatalog.storage.overrideRestricted",
};
const FIELDS = Object.keys(FIELD_LABELS);
const BOOLEAN_FIELDS = [
  "protectFromLight",
  "doNotFreeze",
  "doNotRefrigerate",
  "overrideRestricted",
];
const INTEGER_FIELDS = ["storageDuration", "disposalTimeframe"];

const normalized = (config, field) => {
  const v = config ? config[field] : null;
  if (BOOLEAN_FIELDS.includes(field)) {
    return !!v;
  }
  if (INTEGER_FIELDS.includes(field)) {
    return toInt(v);
  }
  return v === undefined || v === null || v === "" ? null : v;
};

const fieldsThatDiffer = (a, b) =>
  FIELDS.filter((f) => normalized(a, f) !== normalized(b, f));

const StorageSection = ({ testId, groupTestIds }) => {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  // Group mode (FR-8): edit shared storage across N tests. Every test is loaded;
  // the form starts from the first test's values, fields that are not the same
  // on every test are named in a warning, and a save writes only the fields the
  // admin changed, so each test keeps its own value for the rest. A differing
  // field the admin edited counts as changed even when it ends on the first
  // test's value, so that value can be applied to every test.
  const isGroup = Array.isArray(groupTestIds) && groupTestIds.length > 0;
  const primaryId = isGroup ? groupTestIds[0] : testId;
  const groupKey = isGroup ? groupTestIds.join(",") : "";

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState(null);
  const [baseline, setBaseline] = useState(null);
  const [differingFields, setDifferingFields] = useState([]);
  const [touched, setTouched] = useState([]);
  const [historyOpen, setHistoryOpen] = useState(false);

  const load = () => {
    setLoading(true);
    setError(false);
    const ids = isGroup ? groupTestIds : [primaryId];
    const configs = {};
    let pending = ids.length;
    let failed = false;
    ids.forEach((id) => {
      getFromOpenElisServer(`/rest/test-catalog/tests/${id}/storage`, (res) => {
        if (!res) {
          failed = true;
        }
        configs[id] = res;
        pending -= 1;
        if (pending > 0) {
          return;
        }
        setLoading(false);
        if (failed) {
          setError(true);
          return;
        }
        const first = configs[ids[0]];
        const differing = new Set();
        ids.forEach((other) =>
          fieldsThatDiffer(first, configs[other]).forEach((f) =>
            differing.add(f),
          ),
        );
        setDifferingFields(FIELDS.filter((f) => differing.has(f)));
        setBaseline(first);
        setTouched([]);
        setForm(first);
      });
    });
  };

  useEffect(() => {
    if (!primaryId) {
      return;
    }
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [testId, groupKey]);

  const update = (patch) => {
    setForm((prev) => ({ ...prev, ...patch }));
    setTouched((prev) => [...new Set([...prev, ...Object.keys(patch)])]);
  };

  const valueChanged = form && baseline ? fieldsThatDiffer(baseline, form) : [];
  const changedFields = FIELDS.filter(
    (f) =>
      valueChanged.includes(f) ||
      (touched.includes(f) && differingFields.includes(f)),
  );

  const handleSave = () => {
    setSaving(true);
    const storage = {
      ...form,
      storageDuration: toInt(form.storageDuration),
      disposalTimeframe: toInt(form.disposalTimeframe),
    };
    const url = isGroup
      ? "/rest/test-catalog/group/storage"
      : `/rest/test-catalog/tests/${testId}/storage`;
    const payload = isGroup
      ? { testIds: groupTestIds, storage, fields: changedFields }
      : storage;
    putToOpenElisServer(url, JSON.stringify(payload), (status) => {
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
    });
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
      {isGroup && differingFields.length > 0 && (
        <InlineNotification
          kind="warning"
          lowContrast
          hideCloseButton
          data-testid="storage-differ-warning"
          title={intl.formatMessage({
            id: "state.testCatalog.differsAcrossTests",
          })}
          subtitle={intl.formatMessage(
            { id: "label.testCatalog.group.storageDiffer" },
            {
              fields: differingFields
                .map((f) => intl.formatMessage({ id: FIELD_LABELS[f] }))
                .join(", "),
            },
          )}
        />
      )}
      {!isGroup && (
        <>
          <div style={{ display: "flex", justifyContent: "flex-end" }}>
            <Button
              kind="ghost"
              size="sm"
              renderIcon={RecentlyViewed}
              onClick={() => setHistoryOpen(true)}
              data-testid="storage-history-button"
            >
              {intl.formatMessage({
                id: "label.testCatalog.storage.history.view",
              })}
            </Button>
          </div>
          <StorageHistoryModal
            open={historyOpen}
            onClose={() => setHistoryOpen(false)}
            testId={testId}
          />
        </>
      )}
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
        <Button
          kind="primary"
          disabled={saving || (isGroup && changedFields.length === 0)}
          onClick={handleSave}
        >
          <FormattedMessage id="label.button.save" />
        </Button>
      </div>
    </Stack>
  );
};

export default StorageSection;
