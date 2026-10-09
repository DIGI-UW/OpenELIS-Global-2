import React, { useCallback, useContext, useEffect, useState } from "react";
import { useHistory, useLocation } from "react-router-dom";
import {
  Stack,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  RadioButtonGroup,
  RadioButton,
  Toggle,
  Button,
  ComboBox,
  FilterableMultiSelect,
  Tag,
  Loading,
  InlineNotification,
  Modal,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
  postToOpenElisServerJsonResponse,
  putToOpenElisServerJsonResponse,
  resolveApiErrorMessage,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";
import useDomains from "../../../common/useDomains";
import ActivationAckModal from "./ActivationAckModal";

/**
 * OGC-949 / OGC-1112 — Basic Info section.
 *
 * Edits Domain / AMR / status plus (OGC-1112 dependency 8) the Code and
 * Description; the display name is edited in the Localization section. When
 * opened as `testId === "new"` it renders a blank create form (FR-2): Name,
 * Reporting name, Code (auto-suggested), Lab Unit, Sample type, Domain, toggles,
 * Description — Save creates the test Inactive (FR-3) and lands on its editor.
 */
// OGC-1145 FR-3 (D-030 domain guard). Since the OGC-296 domain migration a
// sample type's domain is the enum value (CLINICAL/ENVIRONMENTAL/VECTOR), but
// legacy one-character codes (sample_domain: H uman / N ewborn / E nvironmental
// / A nimal) can still arrive from un-migrated or plugin-inserted rows, so we
// normalize before comparing — the single mirror of the backend
// SampleTypeDomainMapper. Unknown/blank domains stay offerable everywhere so
// legacy data never blocks the editor.
const normalizeSampleDomain = (raw) => {
  if (!raw) {
    return null;
  }
  switch (String(raw).trim().toUpperCase()) {
    case "E":
    case "ENVIRONMENTAL":
      return "ENVIRONMENTAL";
    case "A":
    case "VECTOR":
      return "VECTOR";
    case "H":
    case "N":
    case "CLINICAL":
      return "CLINICAL";
    default:
      return null;
  }
};

const sampleTypeMatchesDomain = (type, domain) => {
  if (!domain || !type?.domain) {
    return true;
  }
  const normalized = normalizeSampleDomain(type.domain);
  return normalized === null || normalized === domain;
};

const QC_THRESHOLD_FIELDS = [
  { field: "qcBlankThreshold", labelKey: "test.qc.blankThreshold" },
  { field: "qcRpdThreshold", labelKey: "test.qc.rpdThreshold" },
  { field: "qcRecoveryWindowPct", labelKey: "test.qc.recoveryWindowPct" },
];

const isBlankValue = (value) =>
  value === undefined || value === null || String(value).trim() === "";

// Mirrors the server: whole minutes, and non-negative numbers for QC limits.
const holdingTimeValid = (value) =>
  isBlankValue(value) || /^\d{1,9}$/.test(String(value).trim());

const thresholdValid = (value) =>
  isBlankValue(value) || /^\d{1,10}(\.\d{1,5})?$/.test(String(value).trim());

const BasicInfoSection = ({ testId }) => {
  const domains = useDomains();
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const base = location.pathname.startsWith("/admin")
    ? "/admin"
    : "/MasterListsPage";
  const isCreate = testId === "new";
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  const [loading, setLoading] = useState(!isCreate);
  const [error, setError] = useState(false);
  const [saving, setSaving] = useState(false);
  const [staleSave, setStaleSave] = useState(null);
  const [form, setForm] = useState(null);
  const [pendingDomain, setPendingDomain] = useState(null);
  const [domainRadioKey, setDomainRadioKey] = useState(0);
  const [ackModalOpen, setAckModalOpen] = useState(false);
  const [coverageReport, setCoverageReport] = useState(null);
  // FR-57/FR-58 — the completeness checklist returned (422) when activation is
  // refused because the test isn't in an activatable state.
  const [completenessReport, setCompletenessReport] = useState(null);
  // FR-58 — the same gaps, fetched proactively on load, shown as a persistent
  // checklist beside the status toggle for an inactive test.
  const [completenessGaps, setCompletenessGaps] = useState([]);
  // FR-18 (OGC-1119) — the LOINC integrity warnings activation re-surfaces:
  // shown beside the toggle right after the test goes Active, never a block.
  const [activationWarnings, setActivationWarnings] = useState(null);
  // The field a 422 save named as unstorable.
  const [serverInvalidField, setServerInvalidField] = useState(null);

  // Create-mode state (FR-2).
  const [createForm, setCreateForm] = useState({
    name: "",
    reportingName: "",
    code: "",
    labUnitId: "",
    sampleTypeIds: [],
    domain: "CLINICAL",
    antimicrobialResistance: false,
    orderable: false,
    description: "",
  });
  const [codeEdited, setCodeEdited] = useState(false);
  const [codeError, setCodeError] = useState(false);
  const [labUnits, setLabUnits] = useState([]);
  const [sampleTypes, setSampleTypes] = useState([]);

  const cancelDomainChange = () => {
    setPendingDomain(null);
    setDomainRadioKey((k) => k + 1);
  };

  useEffect(() => {
    if (!testId || isCreate) {
      return;
    }
    setLoading(true);
    setError(false);
    getFromOpenElisServer(
      `/rest/test-catalog/tests/${testId}/basic-info`,
      (res) => {
        setLoading(false);
        if (!res) {
          setError(true);
          return;
        }
        setForm(res);
      },
    );
    // FR-58 — proactively fetch what still blocks activation so the editor can
    // show the checklist before the user tries to activate.
    getFromOpenElisServer(
      `/rest/test-catalog/tests/${testId}/completeness`,
      (res) => {
        if (res && !res.complete) {
          setCompletenessGaps(res.messages || []);
        } else {
          setCompletenessGaps([]);
        }
      },
    );
  }, [testId, isCreate]);

  // Both create and edit need the Lab Unit + Sample type reference lists (they are
  // editable in both modes).
  useEffect(() => {
    getFromOpenElisServer("/rest/test-catalog/lab-units", (res) =>
      setLabUnits(Array.isArray(res) ? res : []),
    );
    getFromOpenElisServer("/rest/test-catalog/sample-types", (res) =>
      setSampleTypes(Array.isArray(res) ? res : []),
    );
  }, []);

  const update = (patch) => {
    setServerInvalidField(null);
    setForm((prev) => ({ ...prev, ...patch }));
  };
  const updateCreate = (patch) =>
    setCreateForm((prev) => ({ ...prev, ...patch }));

  // OGC-189 (M2) — the Lab Unit picker is a *chooser*, so it offers only active
  // units. The grandfathered-select rule: a test already assigned to a
  // deactivated unit keeps showing that unit, marked inactive, so the control
  // never renders blank and never writes that blank back on save. Losing the
  // assignment this way is the OGC-1191 data-loss class.
  const labUnitOptionsFor = useCallback(
    (currentId) => {
      const active = labUnits.filter((unit) => unit.isActive !== false);
      const current = labUnits.find((unit) => unit.id === currentId);
      if (current && current.isActive === false) {
        return [current, ...active];
      }
      return active;
    },
    [labUnits],
  );

  // The grandfathered value is labelled so the inactive state is visible rather
  // than implied by its absence from the rest of the list.
  const labUnitItemToString = useCallback(
    (item) => {
      if (!item) {
        return "";
      }
      return item.isActive === false
        ? intl.formatMessage(
            { id: "label.testCatalog.basicInfo.labUnit.inactive" },
            { name: item.name },
          )
        : item.name;
    },
    [intl],
  );

  // OGC-1145 FR-1/2/3 — shared sample-types multi-select with removable chips.
  // Only domain-compatible types are offered; already-selected incompatible ones
  // (a domain switch) stay visible as red chips so the manager can remove them.
  const renderSampleTypesControl = (
    idPrefix,
    selectedIds,
    domain,
    onChange,
  ) => {
    const offered = sampleTypes.filter(
      (t) => sampleTypeMatchesDomain(t, domain) || selectedIds.includes(t.id),
    );
    const selectedItems = selectedIds
      .map((id) => sampleTypes.find((t) => t.id === id))
      .filter(Boolean);
    return (
      <div>
        <FilterableMultiSelect
          id={`${idPrefix}-sample-types`}
          titleText={intl.formatMessage({
            id: "label.testCatalog.basicInfo.sampleTypes",
          })}
          helperText={intl.formatMessage({
            id: "helper.testCatalog.basicInfo.sampleTypesMulti",
          })}
          placeholder={intl.formatMessage({
            id: "label.testCatalog.specimenType",
          })}
          items={offered}
          itemToString={(item) => (item ? item.name : "")}
          selectedItems={selectedItems}
          onChange={({ selectedItems: items }) =>
            onChange((items || []).map((t) => t.id))
          }
        />
        {selectedItems.length > 0 && (
          <div style={{ marginTop: "0.5rem" }}>
            {selectedItems.map((t) => (
              <Tag
                key={t.id}
                type={sampleTypeMatchesDomain(t, domain) ? "blue" : "red"}
                filter
                onClose={() =>
                  onChange(selectedIds.filter((id) => id !== t.id))
                }
                title={intl.formatMessage({ id: "label.button.remove" })}
              >
                {t.name}
              </Tag>
            ))}
          </div>
        )}
      </div>
    );
  };

  const editSampleTypeIds = form?.sampleTypeIds || [];
  const editIncompatibleTypes = form
    ? editSampleTypeIds
        .map((id) => sampleTypes.find((t) => t.id === id))
        .filter((t) => t && !sampleTypeMatchesDomain(t, form.domain))
    : [];
  const editSampleTypesMissing = form
    ? editSampleTypeIds.length === 0 && (!!form.active || !!form.orderable)
    : false;

  // ── Create (FR-2/FR-3/FR-4) ───────────────────────────────────────────────

  const createValid =
    createForm.name.trim() &&
    createForm.reportingName.trim() &&
    createForm.code.trim() &&
    createForm.sampleTypeIds.length > 0 &&
    createForm.domain;

  const handleCreate = () => {
    if (!createValid) {
      return;
    }
    setSaving(true);
    setCodeError(false);
    // The create endpoint expects `amr` (not `antimicrobialResistance`); map the
    // form's field names to the request body.
    const payload = {
      name: createForm.name,
      reportingName: createForm.reportingName,
      code: createForm.code,
      labUnitId: createForm.labUnitId,
      sampleTypeIds: createForm.sampleTypeIds,
      domain: createForm.domain,
      amr: createForm.antimicrobialResistance,
      orderable: createForm.orderable,
      description: createForm.description,
    };
    postToOpenElisServerFullResponse(
      "/rest/test-catalog/tests",
      JSON.stringify(payload),
      (response) => {
        setSaving(false);
        if (response && response.status === 201) {
          response.json().then((created) => {
            setNotificationVisible(true);
            addNotification({
              kind: "success",
              title: intl.formatMessage({
                id: "label.testCatalog.section.basic-info",
              }),
              message: intl.formatMessage(
                { id: "notification.testCatalog.testCreated" },
                { name: createForm.name },
              ),
            });
            history.push(
              `${base}/TestCatalogEditor/${created.testId}/basic-info`,
            );
          });
        } else if (response && response.status === 409) {
          // Code-in-use answers a bodyless 409; a description conflict names
          // itself in the body (OGC-1180) — the name doubles as the description,
          // so "code is taken" would point the user at the wrong field.
          response
            .json()
            .then((body) => body && body.conflict === "description")
            .catch(() => false)
            .then((isDescriptionConflict) => {
              if (isDescriptionConflict) {
                setNotificationVisible(true);
                addNotification({
                  kind: "error",
                  title: intl.formatMessage({
                    id: "label.testCatalog.section.basic-info",
                  }),
                  message: intl.formatMessage({
                    id: "error.testCatalog.description.inUse",
                  }),
                });
              } else {
                setCodeError(true);
              }
            });
        } else {
          setNotificationVisible(true);
          addNotification({
            kind: "error",
            title: intl.formatMessage({ id: "error.title" }),
            message: intl.formatMessage({ id: "server.error.msg" }),
          });
        }
      },
    );
  };

  const handleSave = () => {
    setSaving(true);
    setServerInvalidField(null);
    putToOpenElisServerJsonResponse(
      `/rest/test-catalog/tests/${testId}/basic-info`,
      JSON.stringify(form),
      (res) => {
        setSaving(false);
        setNotificationVisible(true);
        // A successful save echoes the BasicInfo body, which has no status
        // field; the helper folds an error response's status into the JSON.
        if (res && res.testId && !res.status) {
          setForm((prev) => ({ ...prev, lastupdated: res.lastupdated }));
          addNotification({
            kind: "success",
            title: intl.formatMessage({
              id: "label.testCatalog.section.basic-info",
            }),
            message: intl.formatMessage({
              id: "label.testCatalog.basicInfo.saved",
            }),
          });
        } else if (res && res.status === 409 && res.conflict === "stale") {
          setNotificationVisible(false);
          setStaleSave(resolveApiErrorMessage(intl, res, "server.error.msg"));
        } else if (res && res.status === 422 && res.invalidField) {
          setServerInvalidField(res.invalidField);
          addNotification({
            kind: "error",
            title: intl.formatMessage({
              id: "label.testCatalog.section.basic-info",
            }),
            message: intl.formatMessage({
              id: "error.testCatalog.basicInfo.invalidValue",
            }),
          });
        } else if (
          res &&
          res.status === 409 &&
          res.conflict === "description"
        ) {
          // test_desc_uk — the description belongs to another test (OGC-1180).
          addNotification({
            kind: "error",
            title: intl.formatMessage({
              id: "label.testCatalog.section.basic-info",
            }),
            message: intl.formatMessage({
              id: "error.testCatalog.description.inUse",
            }),
          });
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

  const handleActivate = (gapsAcknowledged) => {
    postToOpenElisServerJsonResponse(
      `/rest/test-catalog/tests/${testId}/activate`,
      JSON.stringify(gapsAcknowledged ? { gapsAcknowledged } : {}),
      (res) => {
        if (res && (res.status === 422 || res.statusCode === 422)) {
          // FR-57/FR-59 — incomplete test: surface the checklist, never silently
          // succeed or flip the toggle.
          setCompletenessReport(res);
        } else if (res && (res.status === 409 || res.statusCode === 409)) {
          setCoverageReport(res);
          setAckModalOpen(true);
        } else if (res && !res.error) {
          setAckModalOpen(false);
          setCoverageReport(null);
          // Activation also makes the test orderable, so take both flags from the
          // response rather than assuming.
          update({
            active: res.active !== undefined ? res.active : true,
            ...(res.orderable !== undefined
              ? { orderable: res.orderable }
              : {}),
            ...(res.lastupdated ? { lastupdated: res.lastupdated } : {}),
          });
          const integrity = res.loincIntegrity;
          setActivationWarnings(
            integrity &&
              (integrity.noLoinc ||
                (integrity.duplicates && integrity.duplicates.length > 0))
              ? integrity
              : null,
          );
          setNotificationVisible(true);
          addNotification({
            kind: "success",
            title: intl.formatMessage({
              id: "label.testCatalog.section.basic-info",
            }),
            message: intl.formatMessage({
              id: "label.testCatalog.ranges.activated",
            }),
          });
        } else {
          setNotificationVisible(true);
          addNotification({
            kind: "error",
            title: intl.formatMessage({ id: "error.title" }),
            message: intl.formatMessage({ id: "server.error.msg" }),
          });
        }
      },
    );
  };

  const cancelAck = () => {
    setAckModalOpen(false);
    setCoverageReport(null);
  };

  if (isCreate) {
    return (
      <Stack gap={6}>
        <TextInput
          id="basic-info-name"
          labelText={intl.formatMessage({ id: "label.testCatalog.testName" })}
          value={createForm.name}
          onChange={(e) => {
            const name = e.target.value;
            // Auto-suggest the code from the name until the admin edits it (FR-2).
            updateCreate(codeEdited ? { name } : { name, code: name });
          }}
        />
        <TextInput
          id="basic-info-reporting-name"
          labelText={intl.formatMessage({
            id: "label.testCatalog.basicInfo.reportingName",
          })}
          value={createForm.reportingName}
          onChange={(e) => updateCreate({ reportingName: e.target.value })}
        />
        <TextInput
          id="basic-info-code"
          labelText={intl.formatMessage({ id: "label.testCatalog.testCode" })}
          value={createForm.code}
          invalid={codeError}
          invalidText={intl.formatMessage({
            id: "error.testCatalog.codeExists",
          })}
          onChange={(e) => {
            setCodeEdited(true);
            setCodeError(false);
            updateCreate({ code: e.target.value });
          }}
        />
        <ComboBox
          id="basic-info-lab-unit"
          titleText={intl.formatMessage({
            id: "label.testCatalog.basicInfo.labUnit",
          })}
          items={labUnitOptionsFor(createForm.labUnitId)}
          itemToString={labUnitItemToString}
          selectedItem={labUnits.find((u) => u.id === createForm.labUnitId)}
          onChange={({ selectedItem }) =>
            updateCreate({ labUnitId: selectedItem ? selectedItem.id : "" })
          }
        />
        {renderSampleTypesControl(
          "basic-info",
          createForm.sampleTypeIds,
          createForm.domain,
          (ids) => updateCreate({ sampleTypeIds: ids }),
        )}
        {createForm.sampleTypeIds.length === 0 && (
          <InlineNotification
            kind="info"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({
              id: "error.testCatalog.basicInfo.sampleTypeRequired",
            })}
          />
        )}
        <RadioButtonGroup
          name="basic-info-create-domain"
          legendText={intl.formatMessage({ id: "label.testCatalog.domain" })}
          valueSelected={createForm.domain}
          onChange={(value) => updateCreate({ domain: value })}
        >
          {domains.map((d) => (
            <RadioButton
              key={d.id}
              id={`create-domain-${d.id}`}
              value={d.id}
              labelText={intl.formatMessage({ id: d.labelKey })}
            />
          ))}
        </RadioButtonGroup>
        <Toggle
          id="basic-info-create-amr"
          labelText={intl.formatMessage({
            id: "label.testCatalog.basicInfo.amr",
          })}
          labelA={intl.formatMessage({ id: "label.no" })}
          labelB={intl.formatMessage({ id: "label.yes" })}
          toggled={createForm.antimicrobialResistance}
          onToggle={(checked) =>
            updateCreate({ antimicrobialResistance: checked })
          }
        />
        <Toggle
          id="basic-info-create-orderable"
          labelText={intl.formatMessage({
            id: "label.testCatalog.basicInfo.orderable",
          })}
          labelA={intl.formatMessage({ id: "label.no" })}
          labelB={intl.formatMessage({ id: "label.yes" })}
          toggled={createForm.orderable}
          onToggle={(checked) => updateCreate({ orderable: checked })}
        />
        <TextArea
          id="basic-info-create-description"
          labelText={intl.formatMessage({
            id: "label.testCatalog.basicInfo.description",
          })}
          value={createForm.description}
          onChange={(e) => updateCreate({ description: e.target.value })}
          rows={2}
        />
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "label.testCatalog.basicInfo.createInactiveHint",
          })}
        />
        <div style={{ display: "flex", gap: "0.5rem" }}>
          <Button
            kind="primary"
            disabled={saving || !createValid}
            onClick={handleCreate}
          >
            <FormattedMessage id="label.button.save" />
          </Button>
          <Button
            kind="ghost"
            onClick={() => history.push(`${base}/TestCatalogList`)}
          >
            <FormattedMessage id="label.button.cancel" />
          </Button>
        </div>
      </Stack>
    );
  }

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
          id: "label.testCatalog.editor.loadError",
        })}
      />
    );
  }

  const timeHoldingInvalid =
    !holdingTimeValid(form.timeHolding) || serverInvalidField === "timeHolding";
  const invalidThresholds = QC_THRESHOLD_FIELDS.map(
    ({ field }) => field,
  ).filter(
    (field) => !thresholdValid(form[field]) || serverInvalidField === field,
  );
  const showQcThresholds =
    (form.domain && form.domain !== "CLINICAL") ||
    QC_THRESHOLD_FIELDS.some(({ field }) => !isBlankValue(form[field]));

  return (
    <Stack gap={6}>
      <TextInput
        id="basic-info-name"
        labelText={intl.formatMessage({
          id: "label.testCatalog.basicInfo.name",
        })}
        value={form.name || ""}
        readOnly
        helperText={intl.formatMessage({
          id: "label.testCatalog.basicInfo.name.helper",
        })}
      />
      <TextInput
        id="basic-info-code"
        labelText={intl.formatMessage({
          id: "label.testCatalog.basicInfo.code",
        })}
        value={form.code || ""}
        onChange={(e) => update({ code: e.target.value })}
      />
      <TextArea
        id="basic-info-description"
        labelText={intl.formatMessage({
          id: "label.testCatalog.basicInfo.description",
        })}
        value={form.description || ""}
        onChange={(e) => update({ description: e.target.value })}
        rows={2}
      />

      <ComboBox
        id="basic-info-edit-lab-unit"
        titleText={intl.formatMessage({
          id: "label.testCatalog.basicInfo.labUnit",
        })}
        items={labUnitOptionsFor(form.labUnitId)}
        itemToString={labUnitItemToString}
        selectedItem={labUnits.find((u) => u.id === form.labUnitId) || null}
        onChange={({ selectedItem }) =>
          update({ labUnitId: selectedItem ? selectedItem.id : "" })
        }
      />
      <div id="basic-info-edit-sample-type">
        {renderSampleTypesControl(
          "basic-info-edit",
          editSampleTypeIds,
          form.domain,
          (ids) => update({ sampleTypeIds: ids }),
        )}
      </div>
      {editSampleTypesMissing && (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "error.testCatalog.basicInfo.sampleTypeRequired",
          })}
          data-testid="sample-type-required-error"
        />
      )}
      {editIncompatibleTypes.length > 0 && (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "error.testCatalog.basicInfo.sampleTypeDomain",
          })}
          subtitle={editIncompatibleTypes.map((t) => t.name).join(", ")}
          data-testid="sample-type-domain-error"
        />
      )}

      <RadioButtonGroup
        key={domainRadioKey}
        name="basic-info-domain"
        legendText={intl.formatMessage({
          id: "label.testCatalog.basicInfo.domain",
        })}
        valueSelected={form.domain || "CLINICAL"}
        onChange={(value) => {
          if (value !== form.domain) {
            setPendingDomain(value);
          }
        }}
      >
        {domains.map((d) => (
          <RadioButton
            key={d.id}
            id={`domain-${d.id}`}
            value={d.id}
            labelText={intl.formatMessage({ id: d.labelKey })}
          />
        ))}
      </RadioButtonGroup>

      <Toggle
        id="basic-info-amr"
        labelText={intl.formatMessage({
          id: "label.testCatalog.basicInfo.amr",
        })}
        labelA={intl.formatMessage({ id: "label.no" })}
        labelB={intl.formatMessage({ id: "label.yes" })}
        toggled={!!form.antimicrobialResistance}
        onToggle={(checked) => update({ antimicrobialResistance: checked })}
      />
      <Toggle
        id="basic-info-opens-microbiology-case"
        labelText={intl.formatMessage({
          id: "label.testCatalog.basicInfo.opensMicrobiologyCase",
        })}
        labelA={intl.formatMessage({ id: "label.no" })}
        labelB={intl.formatMessage({ id: "label.yes" })}
        toggled={!!form.opensMicrobiologyCase}
        onToggle={(checked) =>
          update({
            opensMicrobiologyCase: checked,
            ...(!checked ? { collectedInSets: false } : {}),
          })
        }
      />
      {form.opensMicrobiologyCase && (
        <Select
          id="basic-info-microbiology-role"
          labelText={intl.formatMessage({
            id: "label.testCatalog.basicInfo.microbiologyCaseRole",
          })}
          value={form.microbiologyCaseRole || "DIRECT"}
          disabled={!form.opensMicrobiologyCase}
          onChange={(event) =>
            update({
              microbiologyCaseRole: event.target.value,
              ...(event.target.value !== "CULTURE"
                ? { collectedInSets: false }
                : {}),
            })
          }
        >
          {["CULTURE", "DIRECT", "CASE"].map((role) => (
            <SelectItem
              key={role}
              value={role}
              text={intl.formatMessage({
                id: `label.testCatalog.basicInfo.microbiologyCaseRole.${role}`,
              })}
            />
          ))}
        </Select>
      )}
      <Toggle
        id="basic-info-collected-in-sets"
        labelText={intl.formatMessage({
          id: "label.testCatalog.basicInfo.collectedInSets",
        })}
        labelA={intl.formatMessage({ id: "label.no" })}
        labelB={intl.formatMessage({ id: "label.yes" })}
        toggled={!!form.collectedInSets}
        disabled={
          !form.opensMicrobiologyCase || form.microbiologyCaseRole !== "CULTURE"
        }
        onToggle={(checked) => update({ collectedInSets: checked })}
      />
      <TextInput
        id="basic-info-time-holding"
        labelText={intl.formatMessage({ id: "test.timeHolding" })}
        value={form.timeHolding || ""}
        onChange={(e) => update({ timeHolding: e.target.value })}
        invalid={timeHoldingInvalid}
        invalidText={intl.formatMessage({
          id: "error.testCatalog.basicInfo.timeHolding",
        })}
      />
      <Toggle
        id="basic-info-in-lab-only"
        labelText={intl.formatMessage({ id: "test.inLabOnly" })}
        labelA={intl.formatMessage({ id: "label.no" })}
        labelB={intl.formatMessage({ id: "label.yes" })}
        toggled={!!form.inLabOnly}
        onToggle={(checked) => update({ inLabOnly: checked })}
      />
      <Toggle
        id="basic-info-notify-results"
        labelText={intl.formatMessage({ id: "test.notifyResults" })}
        labelA={intl.formatMessage({ id: "label.no" })}
        labelB={intl.formatMessage({ id: "label.yes" })}
        toggled={!!form.notifyResults}
        onToggle={(checked) => update({ notifyResults: checked })}
      />
      {showQcThresholds && (
        <fieldset
          className="cds--fieldset"
          data-testid="basic-info-qc-thresholds"
        >
          <legend className="cds--label">
            <FormattedMessage id="test.qc.thresholds.heading" />
          </legend>
          <p className="cds--form__helper-text">
            <FormattedMessage id="test.qc.thresholds.description" />
          </p>
          <Stack gap={5}>
            {QC_THRESHOLD_FIELDS.map(({ field, labelKey }) => (
              <TextInput
                key={field}
                id={`basic-info-${field}`}
                labelText={intl.formatMessage({ id: labelKey })}
                value={form[field] || ""}
                onChange={(e) => update({ [field]: e.target.value })}
                invalid={invalidThresholds.includes(field)}
                invalidText={intl.formatMessage({
                  id: "error.testCatalog.basicInfo.qcThreshold",
                })}
              />
            ))}
          </Stack>
        </fieldset>
      )}
      <Toggle
        id="basic-info-active"
        labelText={intl.formatMessage({
          id: "label.testCatalog.basicInfo.active",
        })}
        labelA={intl.formatMessage({ id: "label.no" })}
        labelB={intl.formatMessage({ id: "label.yes" })}
        toggled={!!form.active}
        onToggle={(checked) => {
          if (checked && !form.active) {
            handleActivate(null);
          } else {
            // Activation sets orderable, so deactivation clears it again.
            update({ active: checked, orderable: false });
            setActivationWarnings(null);
          }
        }}
      />
      {activationWarnings && activationWarnings.noLoinc && (
        <InlineNotification
          kind="warning"
          lowContrast
          hideCloseButton
          data-testid="activation-no-loinc-warning"
          title={intl.formatMessage({ id: "warning.testCatalog.noLoinc" })}
        />
      )}
      {activationWarnings &&
        activationWarnings.duplicates &&
        activationWarnings.duplicates.length > 0 && (
          <InlineNotification
            kind="warning"
            lowContrast
            hideCloseButton
            data-testid="activation-duplicate-loinc-warning"
            title={intl.formatMessage(
              { id: "warning.testCatalog.duplicateLoinc" },
              {
                code: activationWarnings.loinc,
                testName: activationWarnings.duplicates
                  .map((d) => d.name)
                  .join(", "),
              },
            )}
          />
        )}
      {!form.active && completenessGaps.length > 0 && (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "label.testCatalog.activation.incomplete.heading",
          })}
          subtitle={completenessGaps.join(" ")}
          data-testid="completeness-checklist"
        />
      )}
      <Toggle
        id="basic-info-orderable"
        labelText={intl.formatMessage({
          id: "label.testCatalog.basicInfo.orderable",
        })}
        labelA={intl.formatMessage({ id: "label.no" })}
        labelB={intl.formatMessage({ id: "label.yes" })}
        toggled={!!form.orderable}
        onToggle={(checked) => update({ orderable: checked })}
      />

      {staleSave && (
        <div data-testid="basic-info-stale-save">
          <InlineNotification
            kind="error"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({ id: "error.title" })}
            subtitle={staleSave}
          />
          <Button
            kind="secondary"
            size="sm"
            onClick={() => window.location.reload()}
          >
            <FormattedMessage id="label.results.refresh" />
          </Button>
        </div>
      )}

      <div>
        <Button
          kind="primary"
          disabled={
            saving ||
            Boolean(staleSave) ||
            timeHoldingInvalid ||
            invalidThresholds.length > 0 ||
            editSampleTypesMissing ||
            editIncompatibleTypes.length > 0
          }
          onClick={handleSave}
        >
          <FormattedMessage id="label.button.save" />
        </Button>
      </div>

      {pendingDomain !== null && (
        <Modal
          open
          modalHeading={intl.formatMessage({
            id: "label.testCatalog.basicInfo.domainModal.title",
          })}
          primaryButtonText={intl.formatMessage({ id: "label.button.confirm" })}
          secondaryButtonText={intl.formatMessage({
            id: "label.button.cancel",
          })}
          onRequestClose={cancelDomainChange}
          onSecondarySubmit={cancelDomainChange}
          onRequestSubmit={() => {
            update({ domain: pendingDomain });
            setPendingDomain(null);
          }}
        >
          <p>
            {intl.formatMessage(
              { id: "label.testCatalog.basicInfo.domainModal.body" },
              {
                domain: pendingDomain
                  ? intl.formatMessage({
                      id: `label.testCatalog.basicInfo.domain.${pendingDomain}`,
                    })
                  : "",
              },
            )}
          </p>
        </Modal>
      )}

      {ackModalOpen && (
        <ActivationAckModal
          open={ackModalOpen}
          report={coverageReport}
          onAcknowledge={() => handleActivate(JSON.stringify(coverageReport))}
          onCancel={cancelAck}
        />
      )}

      {completenessReport && (
        <Modal
          open={!!completenessReport}
          passiveModal
          modalHeading={intl.formatMessage({
            id: "label.testCatalog.activation.incomplete.heading",
          })}
          onRequestClose={() => setCompletenessReport(null)}
        >
          <p style={{ marginBottom: "1rem" }}>
            <FormattedMessage id="label.testCatalog.activation.incomplete.body" />
          </p>
          <ul style={{ listStyle: "disc", paddingLeft: "1.5rem" }}>
            {(completenessReport.messages || []).map((msg, idx) => (
              <li key={idx} style={{ marginBottom: "0.25rem" }}>
                {msg}
              </li>
            ))}
          </ul>
        </Modal>
      )}
    </Stack>
  );
};

export default BasicInfoSection;
