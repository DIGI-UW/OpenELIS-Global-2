import React, { useState } from "react";
import {
  Button,
  ComposedModal,
  InlineNotification,
  ModalBody,
  ModalFooter,
  ModalHeader,
} from "@carbon/react";
import { useIntl } from "react-intl";

import {
  deactivateAnalyzer,
  reactivateAnalyzer,
  type AnalyzerActivationResultView,
  type AnalyzerDeactivationResultView,
} from "../../../services/analyzerService";
import type { Analyzer } from "../types";

export type AnalyzerLifecycleAction = "deactivate" | "reactivate";

interface AnalyzerLifecycleModalProps {
  action: AnalyzerLifecycleAction;
  analyzer: Analyzer;
  open: boolean;
  onClose: () => void;
  onConfirm: (analyzerId: string) => void;
}

interface LifecycleBlocker {
  code: string;
  args?: Record<string, unknown>;
}

const AnalyzerLifecycleModal = ({
  action,
  analyzer,
  open,
  onClose,
  onConfirm,
}: AnalyzerLifecycleModalProps) => {
  const intl = useIntl();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [blockers, setBlockers] = useState<LifecycleBlocker[]>([]);
  const isDeactivation = action === "deactivate";

  const message = (suffix: string) =>
    intl.formatMessage({ id: `analyzer.lifecycle.${action}.${suffix}` });

  const formatBlocker = (blocker: LifecycleBlocker) => {
    const id = blocker.code;
    const known = Boolean(id && Object.hasOwn(intl.messages, id));
    return intl.formatMessage(
      {
        id: known ? id : "analyzer.setup.connect.activation.blockerUnknown",
      },
      blocker.args,
    );
  };

  const finish = (analyzerId: string) => {
    setSubmitting(false);
    onConfirm(analyzerId);
    onClose();
  };

  const handleDeactivation = (
    response: AnalyzerDeactivationResultView | undefined,
  ) => {
    if (response?.deactivated) {
      finish(response.analyzerId);
      return;
    }
    setSubmitting(false);
    setError(
      response?.failure ||
        response?.error ||
        response?.message ||
        message("error"),
    );
  };

  const handleReactivation = (
    response: AnalyzerActivationResultView | undefined,
  ) => {
    if (response?.activated) {
      finish(response.analyzerId);
      return;
    }
    setSubmitting(false);
    if (response?.blockers?.length) {
      setBlockers(response.blockers);
      return;
    }
    setError(response?.error || response?.message || message("error"));
  };

  const handleConfirm = () => {
    if (!analyzer.id) {
      setError(message("error"));
      return;
    }

    setSubmitting(true);
    setError(null);
    setBlockers([]);
    if (isDeactivation) {
      deactivateAnalyzer(analyzer.id, handleDeactivation);
    } else {
      reactivateAnalyzer(analyzer.id, handleReactivation);
    }
  };

  const handleClose = () => {
    if (!submitting) {
      onClose();
    }
  };

  return (
    <ComposedModal
      open={open}
      onClose={handleClose}
      preventCloseOnClickOutside
      danger={isDeactivation}
      data-testid="analyzer-lifecycle-modal"
    >
      <ModalHeader title={message("title")} />
      <ModalBody>
        {error && (
          <InlineNotification
            kind="error"
            title={message("errorTitle")}
            subtitle={error}
            lowContrast
            hideCloseButton
          />
        )}
        {blockers.length > 0 && (
          <>
            <InlineNotification
              kind="error"
              title={message("blockedTitle")}
              subtitle={message("blockedMessage")}
              lowContrast
              hideCloseButton
            />
            <ul>
              {blockers.map((blocker, index) => (
                <li key={`${blocker.code}-${index}`}>
                  {formatBlocker(blocker)}
                </li>
              ))}
            </ul>
          </>
        )}
        <p>
          {intl.formatMessage(
            { id: `analyzer.lifecycle.${action}.message` },
            { name: analyzer.name },
          )}
        </p>
      </ModalBody>
      <ModalFooter>
        <Button kind="secondary" onClick={handleClose} disabled={submitting}>
          {message("cancel")}
        </Button>
        <Button
          kind={isDeactivation ? "danger" : "primary"}
          onClick={handleConfirm}
          disabled={submitting}
        >
          {message("confirm")}
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

export default AnalyzerLifecycleModal;
