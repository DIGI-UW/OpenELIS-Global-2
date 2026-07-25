import React, { useCallback, useEffect, useState } from "react";
import {
  Button,
  InlineLoading,
  InlineNotification,
  Link,
  Stack,
  Tag,
} from "@carbon/react";
import { CheckmarkFilled } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import * as analyzerService from "../../../services/analyzerService";

const blockerMessageIds = {
  NO_TEST_MAPPINGS: "analyzer.setupVerification.blocker.noTestMappings",
  PENDING_ANALYZER_CODES:
    "analyzer.setupVerification.blocker.pendingAnalyzerCodes",
  PENDING_RESULT_VALUES:
    "analyzer.setupVerification.blocker.pendingResultValues",
  UNBOUND_RESULT_VALUES:
    "analyzer.setupVerification.blocker.unboundResultValues",
  NO_ACTIVE_QC_RULE: "analyzer.setupVerification.blocker.noActiveQcRule",
  NO_ACTIVE_CONTROL_LOT:
    "analyzer.setupVerification.blocker.noActiveControlLot",
  SETUP_NOT_VERIFIED: "analyzer.setupVerification.blocker.notVerified",
  MAPPINGS_CHANGED: "analyzer.setupVerification.blocker.mappingsChanged",
  QC_CHANGED: "analyzer.setupVerification.blocker.qcChanged",
};

const SetupVerificationPanel = ({ analyzerId }) => {
  const intl = useIntl();
  const [verification, setVerification] = useState(null);
  const [loading, setLoading] = useState(true);
  const [verifying, setVerifying] = useState(false);
  const [error, setError] = useState(null);

  const loadVerification = useCallback(() => {
    setLoading(true);
    analyzerService.getSetupVerification(analyzerId, (response) => {
      if (response?.error || response?.statusCode >= 400) {
        setError(
          response?.error ||
            intl.formatMessage({
              id: "analyzer.setupVerification.error.load",
            }),
        );
      } else {
        setVerification(response);
        setError(null);
      }
      setLoading(false);
    });
  }, [analyzerId, intl]);

  useEffect(() => {
    loadVerification();
  }, [loadVerification]);

  const verifyCurrentSetup = () => {
    setVerifying(true);
    setError(null);
    analyzerService.verifyAnalyzerSetup(
      analyzerId,
      {
        mappingIds: verification?.mappingIds || [],
        qcIds: verification?.qcIds || [],
      },
      (response) => {
        setVerifying(false);
        if (response?.error || response?.statusCode >= 400) {
          setError(
            response?.error ||
              intl.formatMessage({
                id: "analyzer.setupVerification.error.verify",
              }),
          );
          return;
        }
        setVerification(response);
      },
    );
  };

  if (loading) {
    return (
      <InlineLoading
        description={intl.formatMessage({
          id: "analyzer.setupVerification.loading",
        })}
      />
    );
  }

  const current = verification?.currentlyVerified === true;
  const canVerify =
    verification?.mappingReady === true && verification?.qcReady === true;

  return (
    <div
      className="setup-verification-panel"
      data-testid="setup-verification-panel"
    >
      <div className="setup-verification-heading">
        <div>
          <h4>
            <FormattedMessage id="analyzer.setupVerification.title" />
          </h4>
          <p>
            <FormattedMessage id="analyzer.setupVerification.subtitle" />
          </p>
        </div>
        <Tag type={current ? "green" : "warm-gray"} size="sm">
          <FormattedMessage
            id={
              current
                ? "analyzer.setupVerification.state.current"
                : `analyzer.setupVerification.state.${(
                    verification?.verificationState || "INCOMPLETE"
                  ).toLowerCase()}`
            }
          />
        </Tag>
      </div>

      {error && (
        <InlineNotification
          kind="error"
          title={error}
          lowContrast
          hideCloseButton
        />
      )}

      {verification?.blockers?.length > 0 && (
        <ul className="setup-verification-blockers">
          {verification.blockers.map((blocker) => (
            <li key={blocker}>
              {intl.formatMessage({
                id:
                  blockerMessageIds[blocker] ||
                  "analyzer.setupVerification.blocker.unknown",
              })}
            </li>
          ))}
        </ul>
      )}

      {verification?.verifiedBy && verification?.verifiedAt && (
        <p data-testid="setup-verification-audit">
          {intl.formatMessage(
            { id: "analyzer.setupVerification.audit" },
            {
              actor: verification.verifiedBy,
              time: intl.formatDate(new Date(verification.verifiedAt), {
                dateStyle: "medium",
                timeStyle: "short",
              }),
            },
          )}
        </p>
      )}

      <Stack
        className="setup-verification-actions"
        orientation="horizontal"
        gap={4}
      >
        <Button
          size="sm"
          kind="primary"
          renderIcon={CheckmarkFilled}
          disabled={!canVerify || verifying}
          onClick={verifyCurrentSetup}
        >
          <FormattedMessage id="analyzer.setupVerification.verify" />
        </Button>
        <Link href={`/analyzers/${analyzerId}/qc-rules`}>
          <FormattedMessage id="analyzer.setupVerification.manageRules" />
        </Link>
        <Link href={`/analyzers/qc/control-lots/new?analyzerId=${analyzerId}`}>
          <FormattedMessage id="analyzer.setupVerification.manageLots" />
        </Link>
      </Stack>
    </div>
  );
};

export default SetupVerificationPanel;
