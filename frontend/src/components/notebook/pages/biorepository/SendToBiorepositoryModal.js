import React, { useState, useEffect, useCallback } from "react";
import PropTypes from "prop-types";
import { Modal, TextInput, InlineNotification } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { postToOpenElisServerJsonResponse } from "../../../utils/Utils";
import BiorepositoryTransferSampleTable, {
  buildDefaultTransferItemMetadata,
  buildTransferSamplesForValidation,
} from "./BiorepositoryTransferSampleTable";
import {
  buildBiorepositoryTransferPayload,
  extractBiorepositoryTransferError,
  validateBiorepositoryTransferRequest,
} from "./biorepositoryTransferValidation";

export function mapPageSamplesForBiorepositoryTransfer(samples, selectedSampleIds) {
  const selectedSet = new Set((selectedSampleIds || []).map(String));
  return (samples || [])
    .filter((sample) => selectedSet.has(String(sample.id)))
    .map((sample) => ({
      sampleItemId: sample.id,
      id: sample.id,
      externalId: sample.externalId || sample.sampleExternalId,
      accessionNumber: sample.accessionNumber || sample.labNo,
      sampleType:
        sample.sampleType ||
        sample.specimenType ||
        sample.typeOfSample ||
        sample.data?.sampleType,
      collectionDate: sample.collectionDate || sample.data?.collectionDate,
      quantity:
        sample.quantity ??
        sample.volume ??
        sample.data?.sampleVolume ??
        sample.data?.volume,
      unitOfMeasure: sample.unitOfMeasure || sample.data?.unitOfMeasure,
      sampleCondition: sample.sampleCondition || sample.data?.sampleCondition,
      preservationMedium:
        sample.preservative ||
        sample.preservationMedium ||
        sample.data?.preservative,
      data: sample.data,
    }));
}

export function BiorepositoryTransferFormFields({
  samples,
  projectName,
  transferReason,
  itemMetadata,
  validationErrors,
  onProjectNameChange,
  onTransferReasonChange,
  onItemMetadataChange,
}) {
  const intl = useIntl();

  return (
    <div style={{ display: "grid", gap: "1rem" }}>
      <InlineNotification
        kind="info"
        title={intl.formatMessage({
          id: "biorepository.transfer.form.infoTitle",
          defaultMessage: "Biorepository Transfer",
        })}
        subtitle={intl.formatMessage({
          id: "biorepository.transfer.form.infoSubtitle",
          defaultMessage:
            "Complete all required fields before submitting. Missing collection date or volume can be entered per sample below.",
        })}
        hideCloseButton
        lowContrast
      />

      {validationErrors?.length > 0 && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "notebook.routing.modal.validationError",
            defaultMessage: "Please correct the following",
          })}
          subtitle={validationErrors.join(" ")}
          lowContrast
          hideCloseButton
        />
      )}

      <TextInput
        id="biorepository-project-name"
        labelText={intl.formatMessage({
          id: "notebook.routing.modal.biorepositoryProject",
          defaultMessage: "Project name *",
        })}
        value={projectName}
        onChange={(e) => onProjectNameChange(e.target.value)}
      />

      <TextInput
        id="biorepository-transfer-reason"
        labelText={intl.formatMessage({
          id: "notebook.routing.modal.biorepositoryReason",
          defaultMessage: "Transfer reason *",
        })}
        value={transferReason}
        onChange={(e) => onTransferReasonChange(e.target.value)}
      />

      <BiorepositoryTransferSampleTable
        samples={samples}
        itemMetadata={itemMetadata}
        onItemMetadataChange={onItemMetadataChange}
      />
    </div>
  );
}

BiorepositoryTransferFormFields.propTypes = {
  samples: PropTypes.arrayOf(PropTypes.object),
  projectName: PropTypes.string,
  transferReason: PropTypes.string,
  itemMetadata: PropTypes.object,
  validationErrors: PropTypes.arrayOf(PropTypes.string),
  onProjectNameChange: PropTypes.func.isRequired,
  onTransferReasonChange: PropTypes.func.isRequired,
  onItemMetadataChange: PropTypes.func.isRequired,
};

function SendToBiorepositoryModal({
  open,
  onClose,
  sourceLab,
  notebookId,
  entryId,
  selectedSamples,
  onSuccess,
  onError,
}) {
  const intl = useIntl();
  const [projectName, setProjectName] = useState("");
  const [transferReason, setTransferReason] = useState("");
  const [itemMetadata, setItemMetadata] = useState({});
  const [validationErrors, setValidationErrors] = useState([]);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!open) return;
    const metadata = {};
    (selectedSamples || []).forEach((sample) => {
      const sampleItemId = String(sample.sampleItemId || sample.id);
      metadata[sampleItemId] = buildDefaultTransferItemMetadata(sample);
    });
    setItemMetadata(metadata);
    setValidationErrors([]);
  }, [open, selectedSamples]);

  const resetForm = useCallback(() => {
    setProjectName("");
    setTransferReason("");
    setItemMetadata({});
    setValidationErrors([]);
  }, []);

  const handleClose = useCallback(() => {
    resetForm();
    onClose();
  }, [onClose, resetForm]);

  const handleSubmit = useCallback(() => {
    const samplesForValidation = buildTransferSamplesForValidation(
      selectedSamples,
      itemMetadata,
    );
    const errors = validateBiorepositoryTransferRequest({
      projectName,
      transferReason,
      samples: samplesForValidation,
    });

    if (errors.length > 0) {
      setValidationErrors(errors);
      return;
    }

    setValidationErrors([]);
    setSubmitting(true);

    const sampleItemIds = (selectedSamples || []).map((sample) =>
      parseInt(sample.sampleItemId || sample.id, 10),
    );

    const payload = buildBiorepositoryTransferPayload({
      sourceLab,
      sampleItemIds,
      projectName,
      transferReason,
      itemMetadata,
      sourceNotebookId: notebookId || entryId,
      sourceNotebookEntryId: entryId,
    });

    postToOpenElisServerJsonResponse(
      "/rest/biorepository/transfer",
      JSON.stringify(payload),
      (response) => {
        setSubmitting(false);
        if (response && response.id) {
          resetForm();
          if (onSuccess) {
            onSuccess(response);
          }
          onClose();
          return;
        }

        const message = extractBiorepositoryTransferError(
          response,
          "Failed to create biorepository transfer request.",
        );
        setValidationErrors([message]);
        if (onError) {
          onError(message);
        }
      },
    );
  }, [
    selectedSamples,
    itemMetadata,
    projectName,
    transferReason,
    sourceLab,
    notebookId,
    entryId,
    onSuccess,
    onError,
    onClose,
    resetForm,
  ]);

  return (
    <Modal
      open={open}
      size="lg"
      modalHeading={intl.formatMessage({
        id: "notebook.routing.modal.biorepositoryTitle",
        defaultMessage: "Transfer to Biorepository",
      })}
      primaryButtonText={
        submitting
          ? intl.formatMessage({
              id: "label.processing",
              defaultMessage: "Processing...",
            })
          : intl.formatMessage({
              id: "biorepository.transfer.submit",
              defaultMessage: "Send to Biorepository",
            })
      }
      secondaryButtonText={intl.formatMessage({
        id: "label.cancel",
        defaultMessage: "Cancel",
      })}
      onRequestClose={handleClose}
      onRequestSubmit={handleSubmit}
      primaryButtonDisabled={submitting || (selectedSamples || []).length === 0}
    >
      <p style={{ marginBottom: "1rem" }}>
        <FormattedMessage
          id="biorepository.transfer.sampleCount"
          defaultMessage="Transfer {count} selected sample(s) to the biorepository review queue."
          values={{ count: (selectedSamples || []).length }}
        />
      </p>

      <BiorepositoryTransferFormFields
        samples={selectedSamples}
        projectName={projectName}
        transferReason={transferReason}
        itemMetadata={itemMetadata}
        validationErrors={validationErrors}
        onProjectNameChange={(value) => {
          setProjectName(value);
          setValidationErrors([]);
        }}
        onTransferReasonChange={(value) => {
          setTransferReason(value);
          setValidationErrors([]);
        }}
        onItemMetadataChange={(metadata) => {
          setItemMetadata(metadata);
          setValidationErrors([]);
        }}
      />
    </Modal>
  );
}

SendToBiorepositoryModal.propTypes = {
  open: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  sourceLab: PropTypes.string.isRequired,
  notebookId: PropTypes.number,
  entryId: PropTypes.number,
  selectedSamples: PropTypes.arrayOf(PropTypes.object),
  onSuccess: PropTypes.func,
  onError: PropTypes.func,
};

export default SendToBiorepositoryModal;
