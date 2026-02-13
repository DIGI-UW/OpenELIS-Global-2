/**
 * DeleteAnalyzerModal Component
 *
 * Confirmation modal for deleting an analyzer
 * Displays warning message about data loss
 * Uses danger/destructive styling for delete action
 *
 * Reference: Figma node 1-1489
 */

import React, { useState } from "react";
import {
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  InlineNotification,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { deleteAnalyzer } from "../../../services/analyzerService";
import PropTypes from "prop-types";
import "./DeleteAnalyzerModal.css";

const DeleteAnalyzerModal = ({ analyzer, open, onClose, onConfirm }) => {
  const intl = useIntl();
  const [isDeleting, setIsDeleting] = useState(false);
  const [notification, setNotification] = useState(null);

  const handleDelete = () => {
    if (!analyzer || !analyzer.id) {
      setNotification({
        kind: "error",
        title: intl.formatMessage({ id: "analyzer.delete.error" }),
        subtitle: intl.formatMessage({
          id: "analyzer.delete.error.noId",
        }),
      });
      return;
    }

    setIsDeleting(true);
    setNotification(null);

    deleteAnalyzer(analyzer.id, (success, error) => {
      setIsDeleting(false);

      if (success) {
        // Call onConfirm callback if provided
        if (onConfirm) {
          onConfirm(analyzer.id);
        }
        // Close modal
        onClose();
      } else {
        setNotification({
          kind: "error",
          title: intl.formatMessage({ id: "analyzer.delete.error" }),
          subtitle:
            error?.error ||
            error?.message ||
            intl.formatMessage({ id: "analyzer.delete.error.unknown" }),
        });
      }
    });
  };

  const analyzerName =
    analyzer?.name || intl.formatMessage({ id: "analyzer.delete.unknown" });

  return (
    <ComposedModal
      open={open}
      onClose={onClose}
      preventCloseOnClickOutside={isDeleting}
      danger
      data-testid="delete-analyzer-modal"
      className="delete-analyzer-modal"
    >
      <ModalHeader
        title={intl.formatMessage({ id: "analyzer.delete.title" })}
        data-testid="delete-analyzer-modal-header"
      />
      <ModalBody>
        {notification && (
          <InlineNotification
            kind={notification.kind}
            title={notification.title}
            subtitle={notification.subtitle}
            onClose={() => setNotification(null)}
            data-testid="delete-analyzer-notification"
          />
        )}

        <p data-testid="delete-analyzer-message">
          {intl.formatMessage(
            { id: "analyzer.delete.message" },
            { name: analyzerName },
          )}
        </p>
      </ModalBody>
      <ModalFooter>
        <Button
          kind="secondary"
          onClick={onClose}
          disabled={isDeleting}
          data-testid="delete-analyzer-cancel-button"
        >
          {intl.formatMessage({ id: "analyzer.delete.cancel" })}
        </Button>
        <Button
          kind="danger"
          onClick={handleDelete}
          disabled={isDeleting}
          data-testid="delete-analyzer-delete-button"
        >
          {intl.formatMessage({ id: "analyzer.delete.delete" })}
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
};

DeleteAnalyzerModal.propTypes = {
  analyzer: PropTypes.shape({
    id: PropTypes.string, // Optional - component handles null/missing id with error notification
    name: PropTypes.string,
  }),
  open: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  onConfirm: PropTypes.func, // Optional callback after successful delete
};

DeleteAnalyzerModal.defaultProps = {
  onConfirm: null,
};

export default DeleteAnalyzerModal;
