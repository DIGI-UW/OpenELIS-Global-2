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
  type AnalyzerDeactivationResultView,
} from "../../../services/analyzerService";
import { analyzerErrorText } from "../analyzerErrors";
import type { Analyzer } from "../types";

interface AnalyzerLifecycleModalProps {
  analyzer: Analyzer;
  open: boolean;
  onClose: () => void;
  onConfirm: (analyzerId: string) => void;
}

/**
 * Confirms deactivating an analyzer. Activating and reactivating one happen
 * in setup's Connect step, which shows what still blocks it.
 */
const AnalyzerLifecycleModal = ({
  analyzer,
  open,
  onClose,
  onConfirm,
}: AnalyzerLifecycleModalProps) => {
  const intl = useIntl();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const message = (suffix: string) =>
    intl.formatMessage({ id: `analyzer.lifecycle.deactivate.${suffix}` });

  const handleDeactivation = (
    response: AnalyzerDeactivationResultView | undefined,
  ) => {
    setSubmitting(false);
    if (response?.deactivated) {
      onConfirm(response.analyzerId);
      onClose();
      return;
    }
    setError(
      analyzerErrorText(intl, response, "analyzer.lifecycle.deactivate.error"),
    );
  };

  const handleConfirm = () => {
    if (!analyzer.id) {
      setError(message("error"));
      return;
    }
    setSubmitting(true);
    setError(null);
    deactivateAnalyzer(analyzer.id, handleDeactivation);
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
      danger
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
        <p>
          {intl.formatMessage(
            { id: "analyzer.lifecycle.deactivate.message" },
            { name: analyzer.name },
          )}
        </p>
      </ModalBody>
      <ModalFooter>
        <Button kind="secondary" onClick={handleClose} disabled={submitting}>
          {message("cancel")}
        </Button>
        <Button kind="danger" onClick={handleConfirm} disabled={submitting}>
          {message("confirm")}
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

export default AnalyzerLifecycleModal;
