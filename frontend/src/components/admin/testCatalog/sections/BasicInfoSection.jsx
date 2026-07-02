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

  // Create-mode state (FR-2).
  const [createForm, setCreateForm] = useState({
    name: "",
    reportingName: "",
    code: "",
    labUnitId: "",
    sampleTypeId: "",
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
  }, [testId, isCreate]);

  // Create form needs the Lab Unit + Sample type reference lists.
  useEffect(() => {
    if (!isCreate) {
      return;
    }
    getFromOpenElisServer("/rest/test-catalog/lab-units", (res) =>
      setLabUnits(Array.isArray(res) ? res : []),
    );
    getFromOpenElisServer("/rest/test-catalog/sample-types", (res) =>
      setSampleTypes(Array.isArray(res) ? res : []),
    );
  }, [isCreate]);

  const update = (patch) => setForm((prev) => ({ ...prev, ...patch }));
  const updateCreate = (patch) =>
    setCreateForm((prev) => ({ ...prev, ...patch }));

  // ── Create (FR-2/FR-3/FR-4) ───────────────────────────────────────────────

  const createValid =
    createForm.name.trim() &&
    createForm.reportingName.trim() &&
    createForm.code.trim() &&
    createForm.sampleTypeId &&
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
      sampleTypeId: createForm.sampleTypeId,
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
        if (res && (res.status === 409 || res.statusCode === 409)) {
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
        <ComboBox
          id="basic-info-sample-type"
          titleText={intl.formatMessage({
            id: "label.testCatalog.specimenType",
          })}
          items={sampleTypes}
          itemToString={(item) => (item ? item.name : "")}
          selectedItem={
            sampleTypes.find((s) => s.id === createForm.sampleTypeId) || null
          }
          onChange={({ selectedItem }) =>
            updateCreate({ sampleTypeId: selectedItem ? selectedItem.id : "" })
          }
        />
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
        <Button kind="primary" disabled={saving} onClick={handleSave}>
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
    </Stack>
  );
};

export default BasicInfoSection;
