import React, { useEffect, useState } from "react";
import {
  Modal,
  Stack,
  TextInput,
  Dropdown,
  RadioButtonGroup,
  RadioButton,
  Checkbox,
  Toggle,
  InlineNotification,
} from "@carbon/react";
import { useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  putToOpenElisServer,
} from "../../../utils/Utils";

const TRIGGER_TYPES = [
  "ALL",
  "ABNORMAL",
  "CRITICAL",
  "SPECIFIC_VALUE",
  "COMPLIANCE_BREACH",
];

/** Scope value standing for "every component" / "every specimen". */
const ANY = "";

const blankRule = () => ({
  name: "",
  componentId: ANY,
  sampleTypeId: ANY,
  triggerType: "ALL",
  triggerValue: "",
  notifySms: false,
  notifyEmail: false,
  notifyOrderingPhysician: false,
  notifyPatient: false,
  notifyReferringFacility: false,
  notifyCustomPhone: "",
  notifyCustomEmail: "",
  notifyRoleId: "",
  acknowledgmentRequired: false,
  enabled: true,
});

/**
 * OGC-949 / OGC-994..997 (epic OGC-763) — Add/Edit Alert Rule modal.
 *
 * Authors a rule's trigger condition (5 types incl. COMPLIANCE_BREACH for
 * ENV/VECTOR, with an adaptive Specific Value field — OGC-995), notification
 * channels + recipients (OGC-996), and the per-rule acknowledgment-required
 * toggle (OGC-997). Persists via POST/PUT to the OGC-763 alert endpoints.
 */
const AlertRuleModal = ({ open, onClose, testId, rule, onSaved }) => {
  const intl = useIntl();
  const isEdit = !!(rule && rule.id);

  const [form, setForm] = useState(blankRule());
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [roles, setRoles] = useState([]);
  // A rule names a measurement, and on a multi-component test the test name
  // does not: Ct Value and the coded PCR Result beside it are different things
  // to alert on.
  const [components, setComponents] = useState([]);
  const [sampleTypes, setSampleTypes] = useState([]);

  useEffect(() => {
    if (!open) {
      return;
    }
    setError(null);
    setForm(rule && rule.id ? { ...blankRule(), ...rule } : blankRule());
    getFromOpenElisServer(`/rest/test-catalog/${testId}/alerts/roles`, (data) =>
      setRoles(Array.isArray(data) ? data : []),
    );
    getFromOpenElisServer(
      `/rest/test-catalog/${testId}/alerts/components`,
      (data) => setComponents(Array.isArray(data) ? data : []),
    );
    getFromOpenElisServer(
      `/rest/test-catalog/${testId}/alerts/sample-types`,
      (data) => setSampleTypes(Array.isArray(data) ? data : []),
    );
  }, [open, rule, testId]);

  // A test with one component has nothing to disambiguate, so the picker stays
  // out of the way; the rule is unscoped and means what it always meant.
  const multiComponent = components.length > 1;
  const multiSpecimen = sampleTypes.length > 1;

  const set = (field, value) =>
    setForm((prev) => ({ ...prev, [field]: value }));

  const handleSubmit = () => {
    if (!form.name || !form.name.trim()) {
      setError(
        intl.formatMessage({ id: "label.testCatalog.alerts.nameRequired" }),
      );
      return;
    }
    if (
      form.triggerType === "SPECIFIC_VALUE" &&
      (!form.triggerValue || !form.triggerValue.trim())
    ) {
      setError(
        intl.formatMessage({ id: "label.testCatalog.alerts.valueRequired" }),
      );
      return;
    }
    setSaving(true);
    setError(null);
    const cb = (status) => {
      setSaving(false);
      if (status >= 200 && status < 300) {
        onSaved();
        onClose();
      } else {
        setError(
          intl.formatMessage({ id: "label.testCatalog.alerts.saveError" }),
        );
      }
    };
    // explicit whitelist — the form state may carry server-managed fields
    // (id, testId, lastupdated) from the GET representation on edit
    const payload = JSON.stringify({
      name: form.name,
      componentId: form.componentId || null,
      sampleTypeId: form.sampleTypeId || null,
      triggerType: form.triggerType,
      triggerValue: form.triggerValue,
      notifySms: form.notifySms,
      notifyEmail: form.notifyEmail,
      notifyOrderingPhysician: form.notifyOrderingPhysician,
      notifyPatient: form.notifyPatient,
      notifyReferringFacility: form.notifyReferringFacility,
      notifyCustomPhone: form.notifyCustomPhone,
      notifyCustomEmail: form.notifyCustomEmail,
      notifyRoleId: form.notifyRoleId,
      acknowledgmentRequired: form.acknowledgmentRequired,
      enabled: form.enabled,
    });
    if (isEdit) {
      putToOpenElisServer(
        `/rest/test-catalog/${testId}/alerts/${rule.id}`,
        payload,
        cb,
      );
    } else {
      postToOpenElisServer(`/rest/test-catalog/${testId}/alerts`, payload, cb);
    }
  };

  return (
    <Modal
      open={open}
      onRequestClose={onClose}
      onRequestSubmit={handleSubmit}
      modalHeading={intl.formatMessage({
        id: isEdit
          ? "label.testCatalog.alerts.modal.editTitle"
          : "label.testCatalog.alerts.modal.addTitle",
      })}
      primaryButtonText={intl.formatMessage({ id: "button.save" })}
      secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
      primaryButtonDisabled={saving}
      size="md"
    >
      <Stack gap={5}>
        <TextInput
          id="alert-name"
          labelText={intl.formatMessage({
            id: "label.testCatalog.alerts.field.name",
          })}
          value={form.name}
          onChange={(e) => set("name", e.target.value)}
          invalid={!!error && !form.name.trim()}
        />

        {multiComponent && (
          <Dropdown
            id="alert-component"
            titleText={intl.formatMessage({
              id: "label.testCatalog.alerts.field.component",
            })}
            helperText={intl.formatMessage({
              id: "label.testCatalog.alerts.field.componentHelp",
            })}
            label={intl.formatMessage({
              id: "label.testCatalog.alerts.scope.anyComponent",
            })}
            items={[
              { id: ANY, value: ANY },
              ...components.map((c) => ({ id: c.id, value: c.value })),
            ]}
            itemToString={(item) =>
              item && item.id
                ? item.value
                : intl.formatMessage({
                    id: "label.testCatalog.alerts.scope.anyComponent",
                  })
            }
            selectedItem={
              [
                { id: ANY, value: ANY },
                ...components.map((c) => ({ id: c.id, value: c.value })),
              ].find((i) => i.id === (form.componentId || ANY)) || null
            }
            onChange={({ selectedItem }) =>
              set("componentId", selectedItem ? selectedItem.id : ANY)
            }
          />
        )}

        {multiSpecimen && (
          <Dropdown
            id="alert-sample-type"
            titleText={intl.formatMessage({
              id: "label.testCatalog.alerts.field.sampleType",
            })}
            helperText={intl.formatMessage({
              id: "label.testCatalog.alerts.field.sampleTypeHelp",
            })}
            label={intl.formatMessage({
              id: "label.testCatalog.alerts.scope.anySampleType",
            })}
            items={[
              { id: ANY, value: ANY },
              ...sampleTypes.map((t) => ({ id: t.id, value: t.value })),
            ]}
            itemToString={(item) =>
              item && item.id
                ? item.value
                : intl.formatMessage({
                    id: "label.testCatalog.alerts.scope.anySampleType",
                  })
            }
            selectedItem={
              [
                { id: ANY, value: ANY },
                ...sampleTypes.map((t) => ({ id: t.id, value: t.value })),
              ].find((i) => i.id === (form.sampleTypeId || ANY)) || null
            }
            onChange={({ selectedItem }) =>
              set("sampleTypeId", selectedItem ? selectedItem.id : ANY)
            }
          />
        )}

        <RadioButtonGroup
          legendText={intl.formatMessage({
            id: "label.testCatalog.alerts.field.trigger",
          })}
          name="alert-trigger"
          orientation="vertical"
          valueSelected={form.triggerType}
          onChange={(value) => set("triggerType", value)}
        >
          {TRIGGER_TYPES.map((t) => (
            <RadioButton
              key={t}
              id={`trigger-${t}`}
              value={t}
              labelText={intl.formatMessage({
                id: `label.testCatalog.alerts.trigger.${t}`,
              })}
            />
          ))}
        </RadioButtonGroup>

        {form.triggerType === "SPECIFIC_VALUE" && (
          <TextInput
            id="alert-trigger-value"
            labelText={intl.formatMessage({
              id: "label.testCatalog.alerts.field.specificValue",
            })}
            value={form.triggerValue || ""}
            onChange={(e) => set("triggerValue", e.target.value)}
            invalid={!!error && !form.triggerValue}
          />
        )}

        <fieldset className="cds--fieldset">
          <legend className="cds--label">
            {intl.formatMessage({
              id: "label.testCatalog.alerts.field.channels",
            })}
          </legend>
          <Checkbox
            id="channel-sms"
            labelText={intl.formatMessage({
              id: "label.testCatalog.alerts.channel.sms",
            })}
            checked={!!form.notifySms}
            onChange={(_e, { checked }) => set("notifySms", checked)}
          />
          <Checkbox
            id="channel-email"
            labelText={intl.formatMessage({
              id: "label.testCatalog.alerts.channel.email",
            })}
            checked={!!form.notifyEmail}
            onChange={(_e, { checked }) => set("notifyEmail", checked)}
          />
        </fieldset>

        <fieldset className="cds--fieldset">
          <legend className="cds--label">
            {intl.formatMessage({
              id: "label.testCatalog.alerts.field.recipients",
            })}
          </legend>
          <Checkbox
            id="recipient-physician"
            labelText={intl.formatMessage({
              id: "label.testCatalog.alerts.recipient.physician",
            })}
            checked={!!form.notifyOrderingPhysician}
            onChange={(_e, { checked }) =>
              set("notifyOrderingPhysician", checked)
            }
          />
          <Checkbox
            id="recipient-patient"
            labelText={intl.formatMessage({
              id: "label.testCatalog.alerts.recipient.patient",
            })}
            checked={!!form.notifyPatient}
            onChange={(_e, { checked }) => set("notifyPatient", checked)}
          />
          <Checkbox
            id="recipient-facility"
            labelText={intl.formatMessage({
              id: "label.testCatalog.alerts.recipient.facility",
            })}
            checked={!!form.notifyReferringFacility}
            onChange={(_e, { checked }) =>
              set("notifyReferringFacility", checked)
            }
          />
          <TextInput
            id="recipient-custom-phone"
            labelText={intl.formatMessage({
              id: "label.testCatalog.alerts.recipient.customPhone",
            })}
            value={form.notifyCustomPhone || ""}
            onChange={(e) => set("notifyCustomPhone", e.target.value)}
          />
          <TextInput
            id="recipient-custom-email"
            labelText={intl.formatMessage({
              id: "label.testCatalog.alerts.recipient.customEmail",
            })}
            value={form.notifyCustomEmail || ""}
            onChange={(e) => set("notifyCustomEmail", e.target.value)}
          />
          <Dropdown
            id="recipient-role"
            titleText={intl.formatMessage({
              id: "label.testCatalog.alerts.recipient.roleId",
            })}
            label={intl.formatMessage({
              id: "label.testCatalog.alerts.recipient.rolePlaceholder",
            })}
            items={roles}
            itemToString={(item) => (item ? item.value : "")}
            selectedItem={
              roles.find((r) => String(r.id) === String(form.notifyRoleId)) ||
              null
            }
            onChange={({ selectedItem }) =>
              set("notifyRoleId", selectedItem ? selectedItem.id : "")
            }
          />
        </fieldset>

        <Toggle
          id="alert-ack-required"
          labelText={intl.formatMessage({
            id: "label.testCatalog.alerts.field.ackRequired",
          })}
          labelA={intl.formatMessage({ id: "label.no" })}
          labelB={intl.formatMessage({ id: "label.yes" })}
          toggled={!!form.acknowledgmentRequired}
          onToggle={(checked) => set("acknowledgmentRequired", checked)}
        />

        {error && (
          <InlineNotification
            kind="error"
            lowContrast
            onCloseButtonClick={() => setError(null)}
            title={intl.formatMessage({ id: "error.title" })}
            subtitle={error}
          />
        )}
      </Stack>
    </Modal>
  );
};

export default AlertRuleModal;
