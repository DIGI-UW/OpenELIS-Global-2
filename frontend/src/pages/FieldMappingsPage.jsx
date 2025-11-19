/**
 * FieldMappingsPage Route Component
 *
 * Modal wrapper for FieldMapping component with URL updates
 * Opens as modal overlay when route is /analyzers/:id/mappings
 * Task Reference: T067
 */

import React, { useEffect } from "react";
import { useParams, useHistory } from "react-router-dom";
import { ComposedModal, ModalHeader, ModalBody, Button } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import FieldMapping from "../components/analyzers/FieldMapping/FieldMapping";
import "./FieldMappingsPage.css";

const FieldMappingsPage = () => {
  const intl = useIntl();
  const history = useHistory();
  const { id: analyzerId } = useParams();

  const handleClose = () => {
    // Navigate back to analyzers list when modal closes
    history.push("/analyzers");
  };

  // Handle browser back button
  useEffect(() => {
    const handlePopState = () => {
      // If user navigates back, close modal
      if (window.location.pathname !== `/analyzers/${analyzerId}/mappings`) {
        handleClose();
      }
    };

    window.addEventListener("popstate", handlePopState);
    return () => {
      window.removeEventListener("popstate", handlePopState);
    };
  }, [analyzerId]);

  return (
    <ComposedModal
      open={true}
      onClose={handleClose}
      className="field-mappings-modal"
      size="lg"
      preventCloseOnClickOutside={false}
    >
      <ModalHeader
        title={intl.formatMessage({ id: "analyzer.navigation.fieldMappings" })}
        closeModal={handleClose}
      />
      <ModalBody className="field-mappings-modal-body">
        <FieldMapping />
      </ModalBody>
    </ComposedModal>
  );
};

export default FieldMappingsPage;
