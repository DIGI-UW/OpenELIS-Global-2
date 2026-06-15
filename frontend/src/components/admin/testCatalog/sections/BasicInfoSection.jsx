import React, { useContext, useEffect, useState } from "react";
import {
  Stack,
  TextInput,
  TextArea,
  RadioButtonGroup,
  RadioButton,
  Toggle,
  Button,
  Loading,
  InlineNotification,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  putToOpenElisServer,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";

/**
 * OGC-949 M4 / OGC-748 — Basic Info section.
 *
 * This slice persists the v2.5-new fields (Domain, AMR) plus the status flags
 * against the M1 schema. Name/Code/Description render read-only here (editing
 * them is OGC-950, which touches localization); the coverage-gated activation
 * modal is OGC-953, wired when Ranges (M7) lands.
 */
const DOMAINS = ["CLINICAL", "ENVIRONMENTAL", "VECTOR"];

const BasicInfoSection = ({ testId }) => {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState(null);

  useEffect(() => {
    if (!testId) {
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
  }, [testId]);

  const update = (patch) => setForm((prev) => ({ ...prev, ...patch }));

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

  if (loading) {
    return <Loading description="Loading" withOverlay={false} />;
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
        readOnly
      />
      <TextArea
        id="basic-info-description"
        labelText={intl.formatMessage({
          id: "label.testCatalog.basicInfo.description",
        })}
        value={form.description || ""}
        readOnly
        rows={2}
      />

      <RadioButtonGroup
        name="basic-info-domain"
        legendText={intl.formatMessage({
          id: "label.testCatalog.basicInfo.domain",
        })}
        valueSelected={form.domain || "CLINICAL"}
        onChange={(value) => update({ domain: value })}
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
        onToggle={(checked) => update({ active: checked })}
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
    </Stack>
  );
};

export default BasicInfoSection;
