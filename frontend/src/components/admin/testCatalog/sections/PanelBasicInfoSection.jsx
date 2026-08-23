import React, { useContext, useEffect, useState } from "react";
import {
  Button,
  RadioButton,
  RadioButtonGroup,
  Stack,
  Tag,
  TextArea,
  TextInput,
  Tile,
  Toggle,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  postToOpenElisServerJsonResponse,
  putToOpenElisServerFullResponse,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";

/**
 * OGC-224 — Panel editor · Basic Info (FRS v2.2).
 *
 * Panel Name (required) · Domain (radio; only Clinical enabled at launch —
 * Environmental / Vector disabled with the later-phase note) · Sample Types
 * (read-only, DERIVED from the member tests) · Description · Active toggle.
 * Activation rule: with zero tests the toggle is disabled (not clickable)
 * with helper text; editing never auto-flips the state. There is no code
 * field — the panel's LOINC (Terminology section) is its identifier.
 */
const PanelBasicInfoSection = ({ panel, isCreate, onSaved }) => {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  const [name, setName] = useState(isCreate ? "" : panel?.name || "");
  const [description, setDescription] = useState(
    isCreate ? "" : panel?.description || "",
  );
  const [domain, setDomain] = useState(
    isCreate ? "CLINICAL" : panel?.domain || "CLINICAL",
  );
  const [active, setActive] = useState(isCreate ? false : !!panel?.active);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!isCreate && panel) {
      setName(panel.name || "");
      setDescription(panel.description || "");
      setDomain(panel.domain || "CLINICAL");
      setActive(!!panel.active);
    }
  }, [panel, isCreate]);

  const memberCount = isCreate ? 0 : panel?.testCount || 0;
  const canActivate = memberCount > 0;
  const derivedTypes = isCreate ? [] : panel?.sampleTypes || [];

  const notify = (kind, messageId) => {
    addNotification({
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage({ id: messageId }),
      kind,
    });
    setNotificationVisible(true);
  };

  const saveExisting = (panelId) => {
    putToOpenElisServerFullResponse(
      `/rest/test-catalog/panels/${panelId}/basic-info`,
      JSON.stringify({ name: name.trim(), description, domain, active }),
      async (response) => {
        setSaving(false);
        if (response && response.ok) {
          const saved = await response.json();
          notify(NotificationKinds.success, "success.add.edited.msg");
          onSaved(saved);
        } else {
          notify(NotificationKinds.error, "error.panel.save");
        }
      },
    );
  };

  const handleSave = () => {
    if (!name.trim()) {
      notify(NotificationKinds.error, "error.panel.nameRequired");
      return;
    }
    setSaving(true);
    if (isCreate) {
      // create-in-place: the panel starts INACTIVE (zero tests can never be
      // active); it defaults to Active when its first test is added (FRS)
      postToOpenElisServerJsonResponse(
        "/rest/test-catalog/panels",
        JSON.stringify({ name: name.trim(), active: false }),
        (created) => {
          if (created && created.id) {
            saveExisting(created.id);
          } else {
            setSaving(false);
            notify(NotificationKinds.error, "error.panel.save");
          }
        },
      );
    } else {
      saveExisting(panel.id);
    }
  };

  return (
    <Stack gap={5} style={{ maxWidth: "40rem" }}>
      <TextInput
        id="panel-name"
        labelText={intl.formatMessage({ id: "label.panel.name" })}
        value={name}
        placeholder={
          isCreate
            ? intl.formatMessage({ id: "placeholder.panel.name" })
            : undefined
        }
        maxLength={20}
        onChange={(e) => setName(e.target.value)}
      />
      <div>
        <RadioButtonGroup
          legendText={intl.formatMessage({ id: "label.panel.domain" })}
          name="panel-domain"
          valueSelected={domain}
          orientation="horizontal"
          onChange={(value) => setDomain(value)}
        >
          <RadioButton
            labelText={intl.formatMessage({ id: "label.domain.CLINICAL" })}
            value="CLINICAL"
            id="panel-domain-clinical"
          />
          <RadioButton
            labelText={intl.formatMessage({ id: "label.domain.ENVIRONMENTAL" })}
            value="ENVIRONMENTAL"
            id="panel-domain-environmental"
            disabled
          />
          <RadioButton
            labelText={intl.formatMessage({ id: "label.domain.VECTOR" })}
            value="VECTOR"
            id="panel-domain-vector"
            disabled
          />
        </RadioButtonGroup>
        <p className="panel-helper-text" style={helperStyle}>
          {intl.formatMessage(
            { id: "helper.panel.domainGuard" },
            {
              domain: intl.formatMessage({
                id: `label.domain.${domain}`,
                defaultMessage: domain,
              }),
            },
          )}{" "}
          {intl.formatMessage({ id: "note.panel.domainLaterPhase" })}
        </p>
      </div>
      <div>
        <div className="cds--label">
          <FormattedMessage id="label.panel.col.sampleTypes" />
        </div>
        <Tile
          style={{
            padding: "0.75rem",
            border: "1px dashed var(--cds-border-subtle, #c6c6c6)",
          }}
          data-testid="panel-derived-sample-types"
        >
          {derivedTypes.length
            ? derivedTypes.map((typeName) => (
                <Tag key={typeName} type="gray" size="sm">
                  {typeName}
                </Tag>
              ))
            : "—"}
        </Tile>
        <p style={helperStyle}>
          {intl.formatMessage({ id: "note.panel.sampleTypesDerived" })}
        </p>
      </div>
      <TextArea
        id="panel-description"
        labelText={intl.formatMessage({ id: "label.panel.description" })}
        rows={2}
        maxLength={60}
        value={description}
        onChange={(e) => setDescription(e.target.value)}
      />
      <div>
        <Toggle
          id="panel-active"
          labelText={intl.formatMessage({ id: "label.panel.active" })}
          labelA={intl.formatMessage({
            id: "label.testCatalog.list.filter.inactive",
          })}
          labelB={intl.formatMessage({
            id: "label.testCatalog.basicInfo.active",
          })}
          toggled={active}
          disabled={!canActivate && !active}
          onToggle={(checked) => setActive(checked)}
        />
        {!canActivate && (
          <p style={helperStyle} data-testid="panel-needs-test-helper">
            {intl.formatMessage({ id: "helper.panel.needsTest" })}
          </p>
        )}
      </div>
      <div>
        <Button kind="primary" onClick={handleSave} disabled={saving}>
          <FormattedMessage id="label.button.save" />
        </Button>
      </div>
    </Stack>
  );
};

const helperStyle = {
  fontSize: "0.75rem",
  color: "var(--cds-text-secondary, #6f6f6f)",
  marginTop: "0.25rem",
};

export default PanelBasicInfoSection;
