import React, { useEffect, useState } from "react";
import {
  Button,
  Callout,
  CopyButton,
  InlineNotification,
  Layer,
  Modal,
  TextInput,
} from "@carbon/react";
import { CheckmarkFilled } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import {
  getBridgePairing,
  pairBridge,
  type BridgePairingStatus,
} from "../../../services/analyzerService";
import "./BridgePairing.css";

// Errors about the code itself belong on the field; the rest are about the
// Bridge or OpenELIS and show as a notification.
const CODE_ERRORS = new Set([
  "analyzer.bridgePairing.error.codeRequired",
  "analyzer.bridgePairing.error.wrongCode",
]);
const KNOWN_ERRORS = new Set([
  ...CODE_ERRORS,
  "analyzer.bridgePairing.error.certificateMismatch",
  "analyzer.bridgePairing.error.closed",
  "analyzer.bridgePairing.error.identity",
  "analyzer.bridgePairing.error.invalidAnswer",
  "analyzer.bridgePairing.error.notConfigured",
  "analyzer.bridgePairing.error.refused",
  "analyzer.bridgePairing.error.unreachable",
]);

/** The pairing-code field with the one explanation of where the code comes from. */
const PairingForm = ({
  code,
  errorKey,
  submitting,
  onCode,
  onSubmit,
  withSubmit,
}: {
  code: string;
  errorKey: string | null;
  submitting: boolean;
  onCode: (code: string) => void;
  onSubmit: (event: React.FormEvent) => void;
  withSubmit: boolean;
}) => {
  const intl = useIntl();
  const codeError = errorKey && CODE_ERRORS.has(errorKey) ? errorKey : null;
  return (
    <form
      className="bridge-pairing__form"
      data-testid="bridge-pairing-form"
      onSubmit={onSubmit}
    >
      <Layer>
        <TextInput
          id="bridge-pairing-code"
          labelText={intl.formatMessage({
            id: "analyzer.bridgePairing.code.label",
          })}
          helperText={intl.formatMessage({
            id: "analyzer.bridgePairing.code.help",
          })}
          value={code}
          autoComplete="off"
          invalid={Boolean(codeError)}
          invalidText={codeError ? intl.formatMessage({ id: codeError }) : ""}
          onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
            onCode(event.target.value)
          }
        />
      </Layer>
      {errorKey && !codeError && (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          data-testid="bridge-pairing-error"
          title={intl.formatMessage({ id: errorKey })}
        />
      )}
      {withSubmit && (
        <Button
          type="submit"
          kind="primary"
          size="md"
          disabled={submitting || !code.trim()}
          data-testid="bridge-pairing-submit"
        >
          {intl.formatMessage({ id: "analyzer.bridgePairing.pair" })}
        </Button>
      )}
    </form>
  );
};

/**
 * Whether OpenELIS is paired with the Analyzer Bridge, as a status row, and
 * the way to pair it with the code the Bridge shows or was configured with.
 */
const BridgePairing = () => {
  const intl = useIntl();
  const [status, setStatus] = useState<BridgePairingStatus | undefined>();
  const [pairingAgain, setPairingAgain] = useState(false);
  const [code, setCode] = useState("");
  const [errorKey, setErrorKey] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    getBridgePairing(setStatus, controller.signal);
    return () => controller.abort();
  }, []);

  if (!status || !status.bridgeUrl) {
    return null;
  }

  const submit = (event?: React.FormEvent) => {
    event?.preventDefault();
    if (!code.trim()) {
      setErrorKey("analyzer.bridgePairing.error.codeRequired");
      return;
    }
    setSubmitting(true);
    setErrorKey(null);
    pairBridge(code, (response) => {
      setSubmitting(false);
      if (response && response.paired && !response.errorKey) {
        setStatus(response);
        setPairingAgain(false);
        setCode("");
      } else {
        setErrorKey(
          response?.errorKey && KNOWN_ERRORS.has(response.errorKey)
            ? response.errorKey
            : "analyzer.bridgePairing.error.refused",
        );
      }
    });
  };

  const onCode = (value: string) => {
    setCode(value);
    if (errorKey && CODE_ERRORS.has(errorKey)) setErrorKey(null);
  };

  if (!status.paired) {
    return (
      <div className="bridge-pairing" data-testid="bridge-pairing">
        <Callout
          kind="warning"
          lowContrast
          data-testid="bridge-pairing-unpaired"
          title={intl.formatMessage({
            id: "analyzer.bridgePairing.unpaired.title",
          })}
          subtitle={intl.formatMessage({
            id: status.pairsAutomatically
              ? "analyzer.bridgePairing.automatic"
              : "analyzer.bridgePairing.unpaired.subtitle",
          })}
        />
        <PairingForm
          code={code}
          errorKey={errorKey}
          submitting={submitting}
          onCode={onCode}
          onSubmit={submit}
          withSubmit
        />
      </div>
    );
  }

  const fingerprint = (status.bridgeCertificateSha256 || "").slice(0, 16);
  return (
    <div className="bridge-pairing" data-testid="bridge-pairing">
      <div className="bridge-pairing__row" data-testid="bridge-pairing-paired">
        <div className="bridge-pairing__state">
          <CheckmarkFilled
            className="bridge-pairing__icon bridge-pairing__icon--paired"
            aria-hidden="true"
          />
          <span>
            {intl.formatMessage({ id: "analyzer.bridgePairing.paired.title" })}
          </span>
        </div>
        <dl className="bridge-pairing__facts">
          <div>
            <dt>
              {intl.formatMessage({ id: "analyzer.bridgePairing.bridge" })}
            </dt>
            <dd>{status.bridgeUrl}</dd>
          </div>
          <div>
            <dt>
              {intl.formatMessage({ id: "analyzer.bridgePairing.certificate" })}
            </dt>
            <dd>
              <code>{fingerprint}</code>
              {status.bridgeCertificateSha256 && (
                <CopyButton
                  size="sm"
                  align="top"
                  iconDescription={intl.formatMessage({
                    id: "analyzer.bridgePairing.copyCertificate",
                  })}
                  feedback={intl.formatMessage({
                    id: "analyzer.bridgePairing.copied",
                  })}
                  onClick={() =>
                    navigator.clipboard?.writeText(
                      status.bridgeCertificateSha256 || "",
                    )
                  }
                />
              )}
            </dd>
          </div>
          <div>
            <dt>
              {intl.formatMessage({ id: "analyzer.bridgePairing.pairedOn" })}
            </dt>
            <dd>
              {status.pairedAt
                ? intl.formatDate(status.pairedAt, {
                    dateStyle: "medium",
                    timeStyle: "short",
                  })
                : "-"}
            </dd>
          </div>
        </dl>
        <Button
          kind="ghost"
          size="sm"
          data-testid="bridge-pairing-again"
          onClick={() => {
            setErrorKey(null);
            setPairingAgain(true);
          }}
        >
          {intl.formatMessage({ id: "analyzer.bridgePairing.pairAgain" })}
        </Button>
      </div>
      {pairingAgain && (
        <Modal
          open
          size="sm"
          modalHeading={intl.formatMessage({
            id: "analyzer.bridgePairing.modal.title",
          })}
          primaryButtonText={intl.formatMessage({
            id: "analyzer.bridgePairing.pair",
          })}
          secondaryButtonText={intl.formatMessage({
            id: "analyzer.bridgePairing.cancel",
          })}
          primaryButtonDisabled={submitting || !code.trim()}
          onRequestSubmit={() => submit()}
          onRequestClose={() => {
            setPairingAgain(false);
            setErrorKey(null);
            setCode("");
          }}
        >
          <PairingForm
            code={code}
            errorKey={errorKey}
            submitting={submitting}
            onCode={onCode}
            onSubmit={submit}
            withSubmit={false}
          />
        </Modal>
      )}
    </div>
  );
};

export default BridgePairing;
