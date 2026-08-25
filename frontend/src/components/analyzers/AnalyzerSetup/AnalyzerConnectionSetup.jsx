import React, { useEffect, useMemo, useState } from "react";
import { Button, InlineNotification, Loading } from "@carbon/react";
import { useIntl } from "react-intl";

import {
  activateAnalyzer,
  getAnalyzerActivationReadiness,
  testConnection,
  updateAnalyzer,
} from "../../../services/analyzerService";
import AnalyzerConnectionFields, {
  initializeConnectionValues,
  invalidConnectionFields,
  serializeConnectionValues,
} from "./AnalyzerConnectionFields";

const isApiError = (response) =>
  !response ||
  Boolean(response.error) ||
  Boolean(response.messageKey) ||
  Boolean(response.connectionErrorKey) ||
  Number(response.statusCode) >= 400;

const hasMessage = (intl, id) =>
  Boolean(id) && Object.prototype.hasOwnProperty.call(intl.messages, id);

const normalizedCheckKey = (key) =>
  String(key || "other")
    .toLowerCase()
    .replaceAll("-", "_");

const formatCheckKind = (intl, check) => {
  const id = `analyzer.setup.connect.checkKind.${normalizedCheckKey(check.key)}`;
  return intl.formatMessage({
    id: hasMessage(intl, id) ? id : "analyzer.setup.connect.checkKind.other",
  });
};

const formatCheckMessage = (intl, check) => {
  const id = `analyzer.setup.connect.check.${check.messageKey}`;
  if (hasMessage(intl, id)) {
    return intl.formatMessage({ id }, check.details);
  }
  const statusId = `analyzer.setup.connect.checkStatus.${String(
    check.status || "failed",
  ).toLowerCase()}`;
  return intl.formatMessage({
    id: hasMessage(intl, statusId)
      ? statusId
      : "analyzer.setup.connect.checkStatus.failed",
  });
};

const probeOutcomeMessage = (status) => {
  switch (status) {
    case "SUCCEEDED":
      return "analyzer.setup.connect.outcome.success";
    case "TIMEOUT":
      return "analyzer.setup.connect.outcome.timeout";
    case "BLOCKED":
      return "analyzer.setup.connect.outcome.missing_configuration";
    default:
      return "analyzer.setup.connect.outcome.failure";
  }
};

const isActivationResult = (response, analyzerId) =>
  Boolean(response) &&
  !response.error &&
  String(response.analyzerId) === String(analyzerId) &&
  typeof response.ready === "boolean" &&
  typeof response.activated === "boolean" &&
  Array.isArray(response.blockers);

const isProbeResult = (response, saved) => {
  const connection = saved?.connection;
  return (
    !isApiError(response) &&
    Boolean(connection) &&
    response.schemaVersion === "1.0" &&
    String(response.connectionId) === String(saved.bridgeConnectionId) &&
    response.profileRef?.profileId === saved.profileId &&
    Number(response.profileRef?.revision) === Number(saved.profileRevision) &&
    response.profileRef?.fingerprint === saved.profileFingerprint &&
    Number(response.configRevision) === Number(connection.configRevision) &&
    response.configFingerprint === connection.configFingerprint &&
    response.nonMutating === true &&
    Array.isArray(response.checks)
  );
};

const formatActivationBlocker = (intl, blocker) => {
  const id = blocker?.code;
  return intl.formatMessage(
    {
      id:
        id && hasMessage(intl, id)
          ? id
          : "analyzer.setup.connect.activation.blockerUnknown",
    },
    blocker?.args,
  );
};

const AnalyzerConnectionSetup = ({ candidate, onCandidateChange, onClose }) => {
  const intl = useIntl();
  const fields = useMemo(
    () => candidate?.connection?.fields || [],
    [candidate?.connection?.fields],
  );
  const connectionIdentity = [
    candidate?.bridgeConnectionId,
    candidate?.connection?.configRevision,
    candidate?.connection?.configFingerprint,
  ].join(":");
  const [settings, setSettings] = useState(() =>
    initializeConnectionValues(fields),
  );
  const [changedSecrets, setChangedSecrets] = useState(() => new Set());
  const [submitAttempted, setSubmitAttempted] = useState(false);
  const [action, setAction] = useState(null);
  const [probe, setProbe] = useState(null);
  const [error, setError] = useState(false);
  const [readiness, setReadiness] = useState(null);
  const [readinessLoading, setReadinessLoading] = useState(
    Boolean(candidate?.id),
  );
  const [activationError, setActivationError] = useState(false);

  const submitting = action !== null;

  useEffect(() => {
    setSettings(initializeConnectionValues(fields));
    setChangedSecrets(new Set());
    setSubmitAttempted(false);
    setProbe(null);
    setError(false);
  }, [connectionIdentity, fields]);

  useEffect(() => {
    const controller = new AbortController();
    if (candidate?.id) {
      getAnalyzerActivationReadiness(
        candidate.id,
        (result) => {
          setReadinessLoading(false);
          if (!isActivationResult(result, candidate.id)) {
            setReadiness(null);
            setActivationError(true);
            return;
          }
          setReadiness(result);
        },
        controller.signal,
      );
    }
    return () => controller.abort();
  }, [candidate?.id]);

  const refreshReadiness = (analyzerId = candidate.id) => {
    setReadinessLoading(true);
    setActivationError(false);
    getAnalyzerActivationReadiness(analyzerId, (result) => {
      setReadinessLoading(false);
      if (!isActivationResult(result, analyzerId)) {
        setReadiness(null);
        setActivationError(true);
        return;
      }
      setReadiness(result);
    });
  };

  const updateSetting = (field, value) => {
    setSettings((previous) => ({ ...previous, [field.key]: value }));
    if (field.inputKind === "SECRET") {
      setChangedSecrets((previous) => {
        const next = new Set(previous);
        next.add(field.key);
        return next;
      });
    }
    setProbe(null);
    setError(false);
    setReadiness(null);
    setActivationError(false);
  };

  const validate = () =>
    Boolean(candidate?.id && candidate?.connection) &&
    invalidConnectionFields(fields, settings, changedSecrets).length === 0;

  const connectionPayload = () => ({
    name: candidate.name,
    profileId: candidate.profileId,
    profileRevision: candidate.profileRevision,
    testUnitIds: candidate.testUnitIds,
    connectionValues: serializeConnectionValues(
      fields,
      settings,
      changedSecrets,
    ),
  });

  const saveCandidate = (nextAction, onSaved) => {
    setSubmitAttempted(true);
    setError(false);
    if (!validate()) {
      return;
    }

    setAction(nextAction);
    updateAnalyzer(candidate.id, connectionPayload(), (saved) => {
      if (isApiError(saved) || !saved.connection) {
        setAction(null);
        setError(true);
        return;
      }
      onCandidateChange?.(saved);
      onSaved(saved);
    });
  };

  const runProbe = () => {
    setProbe(null);
    saveCandidate("probe", (saved) => {
      testConnection(saved.id, (result) => {
        setAction(null);
        if (!isProbeResult(result, saved)) {
          setError(true);
          return;
        }
        setProbe(result);
        refreshReadiness(saved.id);
      });
    });
  };

  const finishAndActivate = () => {
    setReadiness(null);
    setActivationError(false);
    saveCandidate("activate", (saved) => {
      activateAnalyzer(saved.id, (result) => {
        setAction(null);
        if (!isActivationResult(result, saved.id)) {
          setActivationError(true);
          return;
        }
        setReadiness(result);
        if (result.activated && result.status === "ACTIVE") {
          onCandidateChange?.({ ...saved, status: "ACTIVE" });
          onClose?.();
        }
      });
    });
  };

  const saveAndFinishLater = () => {
    saveCandidate("save", () => {
      setAction(null);
      onClose?.();
    });
  };

  if (!candidate?.id || !candidate?.connection) {
    return (
      <InlineNotification
        kind="error"
        lowContrast
        hideCloseButton
        title={intl.formatMessage({ id: "analyzer.setup.connect.loadError" })}
      />
    );
  }

  return (
    <div className="analyzer-setup__connect">
      <AnalyzerConnectionFields
        fields={fields}
        values={settings}
        changedSecrets={changedSecrets}
        submitAttempted={submitAttempted}
        onChange={updateSetting}
      />

      <div className="analyzer-setup__connect-actions">
        <Button type="button" disabled={submitting} onClick={runProbe}>
          {intl.formatMessage({
            id:
              action === "probe"
                ? "analyzer.setup.connect.testing"
                : "analyzer.setup.connect.test",
          })}
        </Button>
      </div>

      {error && (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({ id: "analyzer.setup.connect.error" })}
        />
      )}

      {probe && (
        <section
          className="analyzer-setup__connect-evidence"
          aria-labelledby="analyzer-setup-connect-evidence-title"
        >
          <InlineNotification
            kind={probe.status === "SUCCEEDED" ? "success" : "error"}
            lowContrast
            hideCloseButton
            title={intl.formatMessage({
              id: probeOutcomeMessage(probe.status),
            })}
          />
          <h4 id="analyzer-setup-connect-evidence-title">
            {intl.formatMessage({ id: "analyzer.setup.connect.evidence" })}
          </h4>
          <dl>
            {probe.checks.map((check) => (
              <div key={check.key}>
                <dt>{formatCheckKind(intl, check)}</dt>
                <dd>{formatCheckMessage(intl, check)}</dd>
              </div>
            ))}
          </dl>
        </section>
      )}

      <section
        className="analyzer-setup__activation-readiness"
        aria-labelledby="analyzer-setup-activation-readiness-title"
      >
        <h4 id="analyzer-setup-activation-readiness-title">
          {intl.formatMessage({
            id: "analyzer.setup.connect.activation.readiness",
          })}
        </h4>
        {readinessLoading && (
          <Loading
            small
            withOverlay={false}
            description={intl.formatMessage({
              id: "analyzer.setup.connect.activation.loading",
            })}
          />
        )}
        {activationError && (
          <InlineNotification
            kind="error"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({
              id: "analyzer.setup.connect.activation.error",
            })}
          />
        )}
        {readiness?.ready && (
          <InlineNotification
            kind="success"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({
              id: readiness.activated
                ? "analyzer.setup.connect.activation.active"
                : "analyzer.setup.connect.activation.ready",
            })}
          />
        )}
        {readiness?.blockers.map((blocker, index) => (
          <InlineNotification
            key={`${blocker.code}-${index}`}
            kind="warning"
            lowContrast
            hideCloseButton
            title={formatActivationBlocker(intl, blocker)}
          />
        ))}
      </section>

      <div className="analyzer-setup__completion-actions">
        <Button type="button" disabled={submitting} onClick={finishAndActivate}>
          {intl.formatMessage({
            id:
              action === "activate"
                ? "analyzer.setup.connect.activation.activating"
                : "analyzer.setup.connect.activation.finish",
          })}
        </Button>
        <Button
          type="button"
          kind="secondary"
          disabled={submitting}
          onClick={saveAndFinishLater}
        >
          {intl.formatMessage({
            id:
              action === "save"
                ? "analyzer.setup.connect.activation.saving"
                : "analyzer.setup.connect.activation.saveLater",
          })}
        </Button>
        <Button
          type="button"
          kind="ghost"
          disabled={submitting}
          onClick={onClose}
        >
          {intl.formatMessage({
            id: "analyzer.setup.connect.activation.cancel",
          })}
        </Button>
      </div>
    </div>
  );
};

export default AnalyzerConnectionSetup;
