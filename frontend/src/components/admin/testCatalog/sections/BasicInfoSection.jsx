import React, { useContext, useEffect, useState } from "react";
import { useHistory, useLocation } from "react-router-dom";
import {
  Stack,
  TextInput,
  TextArea,
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
  putToOpenElisServer,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";
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
const DOMAINS = ["CLINICAL", "ENVIRONMENTAL", "VECTOR"];

// OGC-1145 FR-3 (D-030 domain guard): test domain → compatible legacy
// sample-type domain chars (sample_domain: Human/Newborn/Environmental/Animal).
// Sample types without a domain stay offerable everywhere (legacy data).
const COMPATIBLE_SAMPLE_DOMAINS = {
  CLINICAL: ["H", "N"],
  ENVIRONMENTAL: ["E"],
  VECTOR: ["A"],
};

const sampleTypeMatchesDomain = (type, domain) => {
  if (!domain || !type?.domain) {
    return true;
  }
  const allowed = COMPATIBLE_SAMPLE_DOMAINS[domain];
  return !allowed || allowed.includes(type.domain);
};

const BasicInfoSection = ({ testId }) => {
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

  const update = (patch) => setForm((prev) => ({ ...prev, ...patch }));
  const updateCreate = (patch) =>
    setCreateForm((prev) => ({ ...prev, ...patch }));

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
          setCodeError(true);
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
    putToOpenElisServer(
      `/rest/test-catalog/tests/${testId}/basic-info`,
      JSON.stringify(form),
      (status) => {
        setSaving(false);
        setNotificationVisible(true);
        if (status === 200) {
          addNotification({
            kind: "success",
            title: intl.formatMessage({
              id: "label.testCatalog.section.basic-info",
            }),
            message: intl.formatMessage({
              id: "label.testCatalog.basicInfo.saved",
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
          update({ active: true });
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
          items={labUnits}
          itemToString={(item) => (item ? item.name : "")}
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
          {DOMAINS.map((d) => (
            <RadioButton
              key={d}
              id={`create-domain-${d}`}
              value={d}
              labelText={intl.formatMessage({
                id: `label.testCatalog.basicInfo.domain.${d}`,
              })}
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
        items={labUnits}
        itemToString={(item) => (item ? item.name : "")}
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
        {DOMAINS.map((d) => (
          <RadioButton
            key={d}
            id={`domain-${d}`}
            value={d}
            labelText={intl.formatMessage({
              id: `label.testCatalog.basicInfo.domain.${d}`,
            })}
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
            update({ active: checked });
          }
        }}
      />
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

      <div>
        <Button
          kind="primary"
          disabled={
            saving || editSampleTypesMissing || editIncompatibleTypes.length > 0
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
