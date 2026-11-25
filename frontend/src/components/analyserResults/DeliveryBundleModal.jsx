import React, { useEffect, useState } from "react";
import { InlineLoading, InlineNotification, Modal } from "@carbon/react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";

/** The FHIR bundle the Bridge delivered, exactly as OpenELIS kept it. */
const DeliveryBundleModal = ({ receiptId, onClose }) => {
  const intl = useIntl();
  const [state, setState] = useState({ status: "loading", text: "" });

  useEffect(() => {
    if (!receiptId) {
      return undefined;
    }
    const controller = new AbortController();
    setState({ status: "loading", text: "" });
    getFromOpenElisServer(
      `/rest/analyzer/deliveries/${encodeURIComponent(receiptId)}/bundle`,
      (bundle) =>
        setState(
          bundle
            ? { status: "ready", text: JSON.stringify(bundle, null, 2) }
            : { status: "failed", text: "" },
        ),
      controller.signal,
    );
    return () => controller.abort();
  }, [receiptId]);

  return (
    <Modal
      open={Boolean(receiptId)}
      passiveModal
      size="lg"
      modalHeading={intl.formatMessage({
        id: "analyzer.placement.bundleTitle",
      })}
      onRequestClose={onClose}
    >
      {state.status === "loading" && (
        <InlineLoading
          description={intl.formatMessage({
            id: "analyzer.placement.bundleLoading",
          })}
        />
      )}
      {state.status === "failed" && (
        <InlineNotification
          kind="error"
          hideCloseButton
          lowContrast
          title={intl.formatMessage({ id: "analyzer.placement.bundleFailed" })}
        />
      )}
      {state.status === "ready" && (
        <pre
          className="deliveryBundle"
          data-testid="delivery-bundle"
          tabIndex={0}
          style={{ maxHeight: "60vh", overflow: "auto" }}
        >
          {state.text}
        </pre>
      )}
    </Modal>
  );
};

export default DeliveryBundleModal;
