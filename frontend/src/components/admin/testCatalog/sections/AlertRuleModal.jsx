import React, { useEffect, useState } from "react";
import {
  Modal,
  Stack,
  TextInput,
  NumberInput,
  RadioButtonGroup,
  RadioButton,
  Checkbox,
  Toggle,
  InlineNotification,
} from "@carbon/react";
import { useIntl } from "react-intl";
import {
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

const blankRule = () => ({
  name: "",
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

  useEffect(() => {
    if (!open) {
      return;
    }
    setError(null);
    setForm(rule && rule.id ? { ...blankRule(), ...rule } : blankRule());
  }, [open, rule]);

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
    const payload = JSON.stringify(form);
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
          <NumberInput
            id="recipient-role"
            label={intl.formatMessage({
              id: "label.testCatalog.alerts.recipient.roleId",
            })}
            value={form.notifyRoleId === "" ? "" : Number(form.notifyRoleId)}
            min={0}
            allowEmpty
            hideSteppers
            onChange={(e) =>
              set("notifyRoleId", e.target.value === "" ? "" : e.target.value)
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
