import React, { useEffect, useState } from "react";
import {
  Button,
  Callout,
  InlineNotification,
  TextInput,
  Tile,
} from "@carbon/react";
import { useIntl } from "react-intl";
import {
  getBridgePairing,
  pairBridge,
  type BridgePairingStatus,
} from "../../../services/analyzerService";

const KNOWN_ERRORS = new Set([
  "analyzer.bridgePairing.error.certificateMismatch",
  "analyzer.bridgePairing.error.closed",
  "analyzer.bridgePairing.error.codeRequired",
  "analyzer.bridgePairing.error.identity",
  "analyzer.bridgePairing.error.invalidAnswer",
  "analyzer.bridgePairing.error.notConfigured",
  "analyzer.bridgePairing.error.refused",
  "analyzer.bridgePairing.error.unreachable",
  "analyzer.bridgePairing.error.wrongCode",
]);

/**
 * Shows whether OpenELIS is paired with the Analyzer Bridge, and pairs it with
 * the code the Bridge shows or was configured with.
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

  if (!status) {
    return null;
  }

  if (!status.bridgeUrl) {
    return (
      <div data-testid="bridge-pairing">
        <Callout
          kind="warning"
          lowContrast
          data-testid="bridge-pairing-not-configured"
          title={intl.formatMessage({
            id: "analyzer.bridgePairing.error.notConfigured",
          })}
        />
      </div>
    );
  }

  const submit = (event: React.FormEvent) => {
    event.preventDefault();
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

  const stalePin =
    status.paired &&
    (status.pinCheck === "OTHER_CERTIFICATE" ||
      status.pinCheck === "BRIDGE_UNPAIRED");

  const form = (
    <form
      className="bridge-pairing-form"
      data-testid="bridge-pairing-form"
      onSubmit={submit}
    >
      <TextInput
        id="bridge-pairing-code"
        labelText={intl.formatMessage({
          id: "analyzer.bridgePairing.code.label",
        })}
        value={code}
        autoComplete="off"
        onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
          setCode(event.target.value)
        }
      />
      <Button
        type="submit"
        kind="primary"
        size="md"
        disabled={submitting || !code.trim()}
        data-testid="bridge-pairing-submit"
      >
        {intl.formatMessage({ id: "analyzer.bridgePairing.pair" })}
      </Button>
      {status.paired && !stalePin && (
        <Button
          kind="ghost"
          size="md"
          onClick={() => {
            setPairingAgain(false);
            setErrorKey(null);
          }}
        >
          {intl.formatMessage({ id: "analyzer.bridgePairing.cancel" })}
        </Button>
      )}
      {errorKey && (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          data-testid="bridge-pairing-error"
          title={intl.formatMessage({ id: errorKey })}
        />
      )}
    </form>
  );

  if (status.paired && status.pinCheck === "UNREACHABLE") {
    return (
      <div data-testid="bridge-pairing">
        <Callout
          kind="error"
          lowContrast
          data-testid="bridge-pairing-unreachable"
          title={intl.formatMessage({
            id: "analyzer.bridgePairing.error.unreachable",
          })}
          subtitle={status.bridgeUrl}
        />
      </div>
    );
  }

  if (stalePin) {
    const key =
      status.pinCheck === "OTHER_CERTIFICATE"
        ? "analyzer.bridgePairing.stale.otherCertificate"
        : "analyzer.bridgePairing.stale.bridgeUnpaired";
    return (
      <div data-testid="bridge-pairing">
        <Callout
          kind="warning"
          lowContrast
          data-testid="bridge-pairing-stale"
          title={intl.formatMessage({ id: `${key}.title` })}
          subtitle={`${intl.formatMessage(
            { id: `${key}.subtitle` },
            {
              url: status.bridgeUrl,
              fingerprint: (status.bridgeCertificateSha256 || "").slice(0, 16),
            },
          )} ${intl.formatMessage({
            id: status.pairsAutomatically
              ? "analyzer.bridgePairing.automatic"
              : "analyzer.bridgePairing.unpaired.subtitle",
          })}`}
        />
        {form}
      </div>
    );
  }

  if (!status.paired) {
    return (
      <div data-testid="bridge-pairing">
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
        {form}
      </div>
    );
  }

  return (
    <Tile data-testid="bridge-pairing">
      <div data-testid="bridge-pairing-paired">
        <strong>
          {intl.formatMessage({ id: "analyzer.bridgePairing.paired.title" })}
        </strong>
        <p>
          {intl.formatMessage(
            { id: "analyzer.bridgePairing.paired.detail" },
            {
              url: status.bridgeUrl,
              fingerprint: (status.bridgeCertificateSha256 || "").slice(0, 16),
              pairedAt: status.pairedAt
                ? intl.formatDate(status.pairedAt, {
                    dateStyle: "medium",
                    timeStyle: "short",
                  })
                : "",
            },
          )}
        </p>
      </div>
      {pairingAgain ? (
        form
      ) : (
        <Button
          kind="tertiary"
          size="sm"
          data-testid="bridge-pairing-again"
          onClick={() => setPairingAgain(true)}
        >
          {intl.formatMessage({ id: "analyzer.bridgePairing.pairAgain" })}
        </Button>
      )}
    </Tile>
  );
};

export default BridgePairing;
