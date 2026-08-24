import React, { useMemo, useState } from "react";
import {
  Button,
  InlineNotification,
  RadioButton,
  RadioButtonGroup,
  TextInput,
} from "@carbon/react";
import { useIntl } from "react-intl";

import {
  testConnection,
  updateAnalyzer,
} from "../../../services/analyzerService";

const RESULTS_ONLY = "RESULTS_ONLY";
const TWO_WAY = "TWO_WAY";

const initialSettings = (candidate, analyzerType) => ({
  ipAddress: candidate?.ipAddress || "",
  port: candidate?.port == null ? "" : String(candidate.port),
  importDirectory: candidate?.importDirectory || "",
  communicationMode:
    candidate?.communicationMode ||
    candidate?.effectiveCommunicationMode ||
    analyzerType?.instanceDefaults?.communicationMode ||
    "",
  transportMode: candidate?.transportMode || "",
  connectionRole: candidate?.connectionRole || "",
});

const isApiError = (response) =>
  !response ||
  Boolean(response.error) ||
  Boolean(response.messageKey) ||
  Number(response.statusCode) >= 400;

const hasMessage = (intl, id) =>
  Object.prototype.hasOwnProperty.call(intl.messages, id);

const formatCheckKind = (intl, kind) => {
  const id = `analyzer.setup.connect.checkKind.${kind.toLowerCase()}`;
  return intl.formatMessage({
    id: hasMessage(intl, id) ? id : "analyzer.setup.connect.checkKind.other",
  });
};

const formatCheckMessage = (intl, check) => {
  const id = `analyzer.setup.connect.check.${check.code}`;
  if (hasMessage(intl, id)) {
    return intl.formatMessage({ id }, check.args);
  }
  const statusId = `analyzer.setup.connect.checkStatus.${check.status.toLowerCase()}`;
  return intl.formatMessage({
    id: hasMessage(intl, statusId)
      ? statusId
      : "analyzer.setup.connect.checkStatus.failed",
  });
};

const AnalyzerConnectionSetup = ({
  candidate,
  analyzerType,
  onCandidateChange,
}) => {
  const intl = useIntl();
  const [settings, setSettings] = useState(() =>
    initialSettings(candidate, analyzerType),
  );
  const [submitAttempted, setSubmitAttempted] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [probe, setProbe] = useState(null);
  const [error, setError] = useState(false);

  const isFile =
    candidate?.transportMode === "FILE" || analyzerType?.protocol === "FILE";
  const dataFlow =
    settings.communicationMode === "ANALYZER_INITIATED"
      ? RESULTS_ONLY
      : TWO_WAY;
  const supportsTwoWay =
    analyzerType?.instanceDefaults?.supportsLisInitiated === true;
  const needsRemotePort =
    !isFile &&
    (settings.connectionRole === "INITIATOR" || dataFlow === TWO_WAY);

  const endpoint = useMemo(() => {
    if (!probe?.configureEndpoint) {
      return null;
    }
    const value = probe.configureEndpoint;
    if (value.host && value.port != null) {
      return `${value.host}:${value.port}`;
    }
    return value.path || value.url || null;
  }, [probe]);

  const updateSetting = (field, value) => {
    setSettings((previous) => ({ ...previous, [field]: value }));
    setProbe(null);
    setError(false);
  };

  const changeDataFlow = (nextDataFlow) => {
    if (nextDataFlow === RESULTS_ONLY) {
      updateSetting("communicationMode", "ANALYZER_INITIATED");
      return;
    }
    const declaredDefault = analyzerType?.instanceDefaults?.communicationMode;
    updateSetting(
      "communicationMode",
      declaredDefault === "LIS_INITIATED" || declaredDefault === "BOTH"
        ? declaredDefault
        : "BOTH",
    );
  };

  const validate = () => {
    if (!candidate?.id || !analyzerType) {
      return false;
    }
    if (isFile) {
      return Boolean(settings.importDirectory.trim());
    }
    const port = Number(settings.port);
    return (
      Boolean(settings.ipAddress.trim()) &&
      (!needsRemotePort ||
        (Number.isInteger(port) && port >= 1 && port <= 65535))
    );
  };

  const runProbe = () => {
    setSubmitAttempted(true);
    setError(false);
    setProbe(null);
    if (!validate()) {
      return;
    }

    setSubmitting(true);
    const payload = {
      ipAddress: isFile ? null : settings.ipAddress.trim(),
      port: needsRemotePort ? Number(settings.port) : null,
      communicationMode: isFile ? null : settings.communicationMode,
      transportMode: settings.transportMode,
      connectionRole: settings.connectionRole,
      importDirectory: isFile ? settings.importDirectory.trim() : null,
    };

    updateAnalyzer(candidate.id, payload, (saved) => {
      if (isApiError(saved)) {
        setSubmitting(false);
        setError(true);
        return;
      }
      onCandidateChange?.(saved);
      testConnection(candidate.id, (result) => {
        setSubmitting(false);
        const matchesCandidate =
          !isApiError(result) &&
          String(result.analyzerId) === String(candidate.id) &&
          result.profileRef?.profileId === candidate.profileId &&
          Number(result.profileRef?.revision) ===
            Number(candidate.profileRevision) &&
          Array.isArray(result.checks);
        if (!matchesCandidate) {
          setError(true);
          return;
        }
        setProbe(result);
      });
    });
  };

  if (!candidate?.id || !analyzerType) {
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
      {!isFile && (
        <RadioButtonGroup
          name="analyzer-setup-data-flow"
          aria-label={intl.formatMessage({
            id: "analyzer.setup.connect.dataFlow",
          })}
          legendText={intl.formatMessage({
            id: "analyzer.setup.connect.dataFlow",
          })}
          valueSelected={dataFlow}
          onChange={changeDataFlow}
          orientation="vertical"
        >
          <RadioButton
            id="analyzer-setup-results-only"
            value={RESULTS_ONLY}
            labelText={intl.formatMessage({
              id: "analyzer.setup.connect.dataFlow.resultsOnly",
            })}
          />
          {supportsTwoWay && (
            <RadioButton
              id="analyzer-setup-two-way"
              value={TWO_WAY}
              labelText={intl.formatMessage({
                id: "analyzer.setup.connect.dataFlow.twoWay",
              })}
            />
          )}
        </RadioButtonGroup>
      )}

      <div className="analyzer-setup__connect-fields">
        {isFile ? (
          <TextInput
            id="analyzer-setup-import-directory"
            labelText={intl.formatMessage({
              id: "analyzer.setup.connect.directory",
            })}
            value={settings.importDirectory}
            onChange={(event) =>
              updateSetting("importDirectory", event.target.value)
            }
            invalid={submitAttempted && !settings.importDirectory.trim()}
            invalidText={intl.formatMessage({
              id: "analyzer.setup.connect.directory.required",
            })}
          />
        ) : (
          <>
            <TextInput
              id="analyzer-setup-ip-address"
              labelText={intl.formatMessage({
                id:
                  settings.connectionRole === "RECEIVER" &&
                  dataFlow === RESULTS_ONLY
                    ? "analyzer.setup.connect.sourceAddress"
                    : "analyzer.setup.connect.analyzerAddress",
              })}
              value={settings.ipAddress}
              onChange={(event) =>
                updateSetting("ipAddress", event.target.value)
              }
              invalid={submitAttempted && !settings.ipAddress.trim()}
              invalidText={intl.formatMessage({
                id: "analyzer.setup.connect.address.required",
              })}
            />
            {needsRemotePort && (
              <TextInput
                id="analyzer-setup-port"
                type="number"
                min={1}
                max={65535}
                labelText={intl.formatMessage({
                  id: "analyzer.setup.connect.port",
                })}
                value={settings.port}
                onChange={(event) => updateSetting("port", event.target.value)}
                invalid={
                  submitAttempted &&
                  (!Number.isInteger(Number(settings.port)) ||
                    Number(settings.port) < 1 ||
                    Number(settings.port) > 65535)
                }
                invalidText={intl.formatMessage({
                  id: "analyzer.setup.connect.port.invalid",
                })}
              />
            )}
          </>
        )}
      </div>

      <div className="analyzer-setup__connect-actions">
        <Button type="button" disabled={submitting} onClick={runProbe}>
          {intl.formatMessage({
            id: submitting
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
            kind={probe.outcome === "SUCCESS" ? "success" : "error"}
            lowContrast
            hideCloseButton
            title={intl.formatMessage({
              id: `analyzer.setup.connect.outcome.${probe.outcome.toLowerCase()}`,
            })}
          />
          <h4 id="analyzer-setup-connect-evidence-title">
            {intl.formatMessage({
              id: "analyzer.setup.connect.evidence",
            })}
          </h4>
          <dl>
            {probe.checks.map((check) => (
              <div key={`${check.kind}-${check.code}`}>
                <dt>{formatCheckKind(intl, check.kind)}</dt>
                <dd>{formatCheckMessage(intl, check)}</dd>
              </div>
            ))}
            {endpoint && (
              <div>
                <dt>
                  {intl.formatMessage({
                    id: "analyzer.setup.connect.configureEndpoint",
                  })}
                </dt>
                <dd>{endpoint}</dd>
              </div>
            )}
          </dl>
          {probe.resultsOnlyAvailable && dataFlow === TWO_WAY && (
            <Button
              type="button"
              kind="tertiary"
              onClick={() => changeDataFlow(RESULTS_ONLY)}
            >
              {intl.formatMessage({
                id: "analyzer.setup.connect.useResultsOnly",
              })}
            </Button>
          )}
        </section>
      )}
    </div>
  );
};

export default AnalyzerConnectionSetup;
