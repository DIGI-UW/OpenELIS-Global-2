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
  Modal,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  putToOpenElisServer,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";
import ActivationAckModal from "./ActivationAckModal";

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
  // Domain change is confirmed via a modal before it is applied (US4 AC#1).
  const [pendingDomain, setPendingDomain] = useState(null);
  // Carbon's RadioButtonGroup caches its own selection and only re-reads
  // valueSelected when that prop changes. Cancelling a domain change leaves
  // valueSelected unchanged, so bumping this key remounts the group to re-sync
  // it to the current (unchanged) domain — otherwise the radio would stay
  // visually stuck on the rejected choice.
  const [domainRadioKey, setDomainRadioKey] = useState(0);
  // Activation coverage gate (OGC-973): toggling Active on routes through the
  // coverage check; uncovered ranges surface an acknowledgment modal.
  const [ackModalOpen, setAckModalOpen] = useState(false);
  const [coverageReport, setCoverageReport] = useState(null);

  const cancelDomainChange = () => {
    setPendingDomain(null);
    setDomainRadioKey((k) => k + 1);
  };

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

  const handleActivate = (gapsAcknowledged) => {
    postToOpenElisServerJsonResponse(
      `/rest/test-catalog/tests/${testId}/activate`,
      JSON.stringify(gapsAcknowledged ? { gapsAcknowledged } : {}),
      (res) => {
        if (res && (res.status === 409 || res.statusCode === 409)) {
          // Uncovered age windows → require acknowledgment before activating.
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
        key={domainRadioKey}
        name="basic-info-domain"
        legendText={intl.formatMessage({
          id: "label.testCatalog.basicInfo.domain",
        })}
        valueSelected={form.domain || "CLINICAL"}
        onChange={(value) => {
          // Hold the change until the modal confirms it. The selection is
          // reconciled via valueSelected (on confirm) or the remount key (on
          // cancel) — see domainRadioKey.
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
            // Activation is coverage-gated (OGC-973) — never set directly.
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
