import React, { useState } from "react";
import {
  Button,
  ComposedModal,
  Dropdown,
  Form,
  Heading,
  InlineNotification,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
  NumberInput,
  Section,
  Stack,
  Toggle,
} from "@carbon/react";
import { Add, TrashCan } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { putToOpenElisServerFullResponse } from "../../utils/Utils";
import { useServerData } from "../../utils/useServerData";
import { unitFor } from "./qiThresholds";
import "../common/QAStyles.css";

// The lab's active test sections, the same list every other selector reads.
const TEST_SECTIONS_ENDPOINT = "/rest/displayList/TEST_SECTION_ACTIVE";

/**
 * OGC-709 — two-level editor for one indicator: the lab-wide default
 * (enable/disable + target/action) plus per-test-section overrides. Disabling
 * asks for confirmation (it hides the tile / suppresses alerts / stops auto-NCE
 * once 710/711/712 land). Validation mirrors the server (range 0–100, direction
 * ordering, no duplicate section); the server is the trust boundary and returns
 * 400 on anything forced past this.
 */

// NCE ships without numeric bands until OGC-712 defines them; every other
// indicator requires both thresholds. Mirrors the QiIndicator enum server-side.
function thresholdsRequired(indicatorKey) {
  return indicatorKey !== "NCE";
}

function toNumberOrNull(v) {
  if (v === "" || v === null || v === undefined) {
    return null;
  }
  const n = Number(v);
  return Number.isNaN(n) ? null : n;
}

function QIConfigEditor({ indicator, onClose }) {
  const intl = useIntl();
  const key = indicator.indicatorKey;
  const direction = indicator.direction;
  // TAT thresholds are mean-TAT hours; rate indicators are percentages.
  const unit = unitFor(key);
  const withUnit = (labelId) =>
    unit
      ? `${intl.formatMessage({ id: labelId })} (${unit})`
      : intl.formatMessage({ id: labelId });

  const [enabled, setEnabled] = useState(indicator.enabled !== false);
  const [target, setTarget] = useState(indicator.target ?? "");
  const [action, setAction] = useState(indicator.action ?? "");
  const [overrides, setOverrides] = useState(
    (indicator.overrides || []).map((o) => ({
      testCategoryId: o.testCategoryId ?? "",
      target: o.target ?? "",
      action: o.action ?? "",
    })),
  );
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [confirmDisable, setConfirmDisable] = useState(false);

  const sectionsQuery = useServerData(TEST_SECTIONS_ENDPOINT);
  const sections = Array.isArray(sectionsQuery.data) ? sectionsQuery.data : [];

  const sectionName = (id) => {
    const match = sections.find((s) => String(s.id) === String(id));
    return match ? match.value : id;
  };

  const addOverride = () =>
    setOverrides((prev) => [
      ...prev,
      { testCategoryId: "", target: target, action: action },
    ]);

  const removeOverride = (idx) =>
    setOverrides((prev) => prev.filter((_, i) => i !== idx));

  const setOverrideField = (idx, field, value) =>
    setOverrides((prev) =>
      prev.map((o, i) => (i === idx ? { ...o, [field]: value } : o)),
    );

  const handleToggle = (checked) => {
    if (!checked && enabled) {
      setConfirmDisable(true); // ask before turning off
    } else {
      setEnabled(checked);
    }
  };

  // Client-side mirror of the server rule; returns an i18n message id or null.
  const thresholdError = (t, a, required) => {
    const tn = toNumberOrNull(t);
    const an = toNumberOrNull(a);
    if (tn === null && an === null) {
      return required ? "qa.qiConfig.validation.required" : null;
    }
    if (tn === null || an === null) {
      return "qa.qiConfig.validation.required";
    }
    if (tn < 0 || tn > 100 || an < 0 || an > 100) {
      return "qa.qiConfig.validation.range";
    }
    if (direction === "HIGHER_BETTER" && tn <= an) {
      return "qa.qiConfig.validation.direction";
    }
    if (direction === "LOWER_BETTER" && tn >= an) {
      return "qa.qiConfig.validation.direction";
    }
    return null;
  };

  const validate = () => {
    const defErr = thresholdError(target, action, thresholdsRequired(key));
    if (defErr) {
      return defErr;
    }
    const seen = new Set();
    for (const o of overrides) {
      if (!o.testCategoryId) {
        return "qa.qiConfig.validation.category";
      }
      if (seen.has(o.testCategoryId)) {
        return "qa.qiConfig.validation.duplicateCategory";
      }
      seen.add(o.testCategoryId);
      const oErr = thresholdError(o.target, o.action, true);
      if (oErr) {
        return oErr;
      }
    }
    return null;
  };

  const handleSave = () => {
    const errId = validate();
    if (errId) {
      setError(intl.formatMessage({ id: errId }));
      return;
    }
    setSubmitting(true);
    setError(null);
    const payload = {
      enabled,
      target: toNumberOrNull(target),
      action: toNumberOrNull(action),
      overrides: overrides.map((o) => ({
        testCategoryId: o.testCategoryId,
        target: toNumberOrNull(o.target),
        action: toNumberOrNull(o.action),
      })),
    };
    putToOpenElisServerFullResponse(
      `/rest/qi-config/indicator/${key}`,
      JSON.stringify(payload),
      (response) => {
        setSubmitting(false);
        if (response && (response.status === 200 || response.status === 204)) {
          onClose(true);
        } else if (response && typeof response.json === "function") {
          response
            .json()
            .then((body) =>
              setError(
                (body && body.error) ||
                  intl.formatMessage({ id: "qa.qiConfig.saveFailed" }),
              ),
            )
            .catch(() =>
              setError(intl.formatMessage({ id: "qa.qiConfig.saveFailed" })),
            );
        } else {
          setError(intl.formatMessage({ id: "qa.qiConfig.saveFailed" }));
        }
      },
    );
  };

  const indicatorName = intl.formatMessage({
    id: `qa.qiConfig.indicator.${key.toLowerCase()}`,
  });

  return (
    <>
      <ComposedModal open onClose={() => onClose(false)} size="lg">
        <ModalHeader>
          <Heading>
            {intl.formatMessage(
              { id: "qa.qiConfig.editor.title" },
              { indicator: indicatorName },
            )}
          </Heading>
        </ModalHeader>
        <ModalBody hasScrollingContent>
          {error && (
            <InlineNotification
              kind="error"
              title={error}
              lowContrast
              onCloseButtonClick={() => setError(null)}
            />
          )}
          <Form>
            <Stack gap={6}>
              <Section>
                <Heading>
                  <FormattedMessage id="qa.qiConfig.editor.defaults" />
                </Heading>
                <Toggle
                  id="qi-config-enabled"
                  labelText={intl.formatMessage({
                    id: "qa.qiConfig.field.enabled",
                  })}
                  labelA={intl.formatMessage({ id: "label.no" })}
                  labelB={intl.formatMessage({ id: "label.yes" })}
                  toggled={enabled}
                  onToggle={handleToggle}
                />
                <Stack
                  orientation="horizontal"
                  gap={5}
                  className="qi-config-row"
                >
                  <NumberInput
                    id="qi-config-target"
                    label={withUnit("qa.qiConfig.field.target")}
                    value={target}
                    min={0}
                    max={100}
                    step={0.5}
                    onChange={(e, { value }) => setTarget(value)}
                  />
                  <NumberInput
                    id="qi-config-action"
                    label={withUnit("qa.qiConfig.field.action")}
                    value={action}
                    min={0}
                    max={100}
                    step={0.5}
                    onChange={(e, { value }) => setAction(value)}
                  />
                </Stack>
                {thresholdsRequired(key) && (
                  <p className="qi-dashboard__subtitle">
                    <FormattedMessage id="qa.qiConfig.editor.autoNce" />
                  </p>
                )}
              </Section>

              <Section>
                <Heading>
                  <FormattedMessage id="qa.qiConfig.editor.overrides" />
                </Heading>
                <p className="qi-dashboard__subtitle">
                  <FormattedMessage id="qa.qiConfig.editor.usesDefault" />
                </p>
                {overrides.length === 0 && (
                  <p>
                    <FormattedMessage id="qa.qiConfig.editor.noOverrides" />
                  </p>
                )}
                {overrides.map((o, idx) => (
                  <Stack
                    orientation="horizontal"
                    gap={5}
                    className="qi-config-row"
                    // eslint-disable-next-line react/no-array-index-key
                    key={idx}
                  >
                    <Dropdown
                      id={`qi-config-override-section-${idx}`}
                      titleText={intl.formatMessage({
                        id: "qa.qiConfig.field.category",
                      })}
                      label={intl.formatMessage({
                        id: "qa.qiConfig.field.category",
                      })}
                      items={sections.map((s) => String(s.id))}
                      selectedItem={
                        o.testCategoryId ? String(o.testCategoryId) : null
                      }
                      itemToString={(id) => (id ? sectionName(id) : "")}
                      onChange={({ selectedItem }) =>
                        setOverrideField(idx, "testCategoryId", selectedItem)
                      }
                    />
                    <NumberInput
                      id={`qi-config-override-target-${idx}`}
                      label={withUnit("qa.qiConfig.field.target")}
                      value={o.target}
                      min={0}
                      max={100}
                      step={0.5}
                      onChange={(e, { value }) =>
                        setOverrideField(idx, "target", value)
                      }
                    />
                    <NumberInput
                      id={`qi-config-override-action-${idx}`}
                      label={withUnit("qa.qiConfig.field.action")}
                      value={o.action}
                      min={0}
                      max={100}
                      step={0.5}
                      onChange={(e, { value }) =>
                        setOverrideField(idx, "action", value)
                      }
                    />
                    <Button
                      kind="danger--ghost"
                      size="md"
                      renderIcon={TrashCan}
                      hasIconOnly
                      iconDescription={intl.formatMessage({
                        id: "label.button.delete",
                      })}
                      onClick={() => removeOverride(idx)}
                    />
                  </Stack>
                ))}
                <Button
                  kind="tertiary"
                  size="sm"
                  renderIcon={Add}
                  onClick={addOverride}
                >
                  <FormattedMessage id="qa.qiConfig.editor.addOverride" />
                </Button>
              </Section>
            </Stack>
          </Form>
        </ModalBody>
        <ModalFooter>
          <Button kind="secondary" onClick={() => onClose(false)}>
            <FormattedMessage id="label.button.cancel" />
          </Button>
          <Button kind="primary" onClick={handleSave} disabled={submitting}>
            <FormattedMessage id="label.button.save" />
          </Button>
        </ModalFooter>
      </ComposedModal>

      {confirmDisable && (
        <Modal
          open
          danger
          size="sm"
          modalHeading={intl.formatMessage({
            id: "qa.qiConfig.confirmDisableTitle",
          })}
          primaryButtonText={intl.formatMessage({ id: "label.yes" })}
          secondaryButtonText={intl.formatMessage({ id: "label.no" })}
          onRequestClose={() => setConfirmDisable(false)}
          onSecondarySubmit={() => setConfirmDisable(false)}
          onRequestSubmit={() => {
            setEnabled(false);
            setConfirmDisable(false);
          }}
        >
          <FormattedMessage id="qa.qiConfig.confirmDisableBody" />
        </Modal>
      )}
    </>
  );
}

export default QIConfigEditor;
