import React, { useState, useCallback, useEffect } from "react";
import {
  Modal,
  Checkbox,
  TextArea,
  RadioButtonGroup,
  RadioButton,
  InlineNotification,
  Loading,
  Tag,
  ProgressBar,
  Accordion,
  AccordionItem,
} from "@carbon/react";
import { Checkmark, Warning, WarningAlt } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import PropTypes from "prop-types";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  putToOpenElisServerJsonResponse,
} from "../../../utils/Utils";

/**
 * DocumentationVerificationModal - 7-point verification checklist modal
 * Sub-stage 1b of the Biorepository Intake workflow (linked to shipment)
 *
 * Per SRS Section 4.2.2: Documentation verification happens BEFORE sample
 * registration to ensure accompanying paperwork is complete and accurate.
 *
 * Checklist items:
 * 1. Sample Identifiers Match (shipment manifest matches labels)
 * 2. Project Linkage (valid project reference)
 * 3. Ethics Approval (IRB/ethics reference on file)
 * 4. Biosafety Classification Match
 * 5. Packaging Integrity (manual inspection)
 * 6. Informed Consent Record (for human samples)
 * 7. MTA Documented (for external samples, N/A allowed)
 *
 * @param {Object} props
 * @param {boolean} props.open - Whether the modal is open
 * @param {Function} props.onClose - Callback to close the modal
 * @param {Object} props.shipment - The shipment being verified
 * @param {Function} props.onVerificationComplete - Callback when verification is complete
 */
function DocumentationVerificationModal({
  open,
  onClose,
  shipment,
  onVerificationComplete,
}) {
  const intl = useIntl();

  const [verification, setVerification] = useState(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  // Checklist items configuration
  const checklistItems = [
    {
      id: "sampleIdentifiers",
      label: intl.formatMessage({
        id: "biorepository.verification.item.sampleIdentifiers",
        defaultMessage: "Sample Identifiers Match",
      }),
      description: intl.formatMessage({
        id: "biorepository.verification.item.sampleIdentifiers.description",
        defaultMessage:
          "Verify that sample identifiers on the tube match the manifest and system records",
      }),
      allowNA: false,
    },
    {
      id: "projectLinkage",
      label: intl.formatMessage({
        id: "biorepository.verification.item.projectLinkage",
        defaultMessage: "Project Linkage",
      }),
      description: intl.formatMessage({
        id: "biorepository.verification.item.projectLinkage.description",
        defaultMessage: "Confirm sample is correctly linked to a valid project",
      }),
      allowNA: false,
      autoVerify: true,
    },
    {
      id: "ethicsApproval",
      label: intl.formatMessage({
        id: "biorepository.verification.item.ethicsApproval",
        defaultMessage: "Ethics Approval",
      }),
      description: intl.formatMessage({
        id: "biorepository.verification.item.ethicsApproval.description",
        defaultMessage:
          "Verify ethics approval reference is documented and valid",
      }),
      allowNA: false,
    },
    {
      id: "biosafetyMatch",
      label: intl.formatMessage({
        id: "biorepository.verification.item.biosafetyMatch",
        defaultMessage: "Biosafety Classification Match",
      }),
      description: intl.formatMessage({
        id: "biorepository.verification.item.biosafetyMatch.description",
        defaultMessage: "Confirm biosafety level matches sample documentation",
      }),
      allowNA: false,
    },
    {
      id: "packagingIntegrity",
      label: intl.formatMessage({
        id: "biorepository.verification.item.packagingIntegrity",
        defaultMessage: "Packaging Integrity",
      }),
      description: intl.formatMessage({
        id: "biorepository.verification.item.packagingIntegrity.description",
        defaultMessage: "Manually verify sample packaging is intact and secure",
      }),
      allowNA: false,
      manual: true,
    },
    {
      id: "consentRecord",
      label: intl.formatMessage({
        id: "biorepository.verification.item.consentRecord",
        defaultMessage: "Informed Consent Record",
      }),
      description: intl.formatMessage({
        id: "biorepository.verification.item.consentRecord.description",
        defaultMessage: "Verify informed consent is documented for this sample",
      }),
      allowNA: true,
    },
    {
      id: "mtaDocumented",
      label: intl.formatMessage({
        id: "biorepository.verification.item.mtaDocumented",
        defaultMessage: "MTA Documented",
      }),
      description: intl.formatMessage({
        id: "biorepository.verification.item.mtaDocumented.description",
        defaultMessage:
          "Verify Material Transfer Agreement is in place if sample is from external source",
      }),
      allowNA: true,
    },
  ];

  // Load verification data for shipment
  useEffect(() => {
    if (open && shipment?.id) {
      setLoading(true);
      getFromOpenElisServer(
        `/rest/biorepository/verification/by-shipment/${shipment.id}`,
        (data) => {
          if (data && !data.error) {
            setVerification(data);
          } else {
            // Create new verification if none exists
            postToOpenElisServerJsonResponse(
              `/rest/biorepository/verification/create-for-shipment/${shipment.id}`,
              JSON.stringify({}),
            ).then((response) => {
              if (response && !response.error) {
                // Reload to get full verification object
                getFromOpenElisServer(
                  `/rest/biorepository/verification/by-shipment/${shipment.id}`,
                  (newData) => {
                    setVerification(newData);
                    setLoading(false);
                  },
                );
              } else {
                setError(
                  response?.error || "Failed to create verification record",
                );
                setLoading(false);
              }
            });
            return;
          }
          setLoading(false);
        },
      );
    }
  }, [open, shipment?.id]);

  const getItemStatus = useCallback(
    (itemId) => {
      if (!verification) return "PENDING";
      const statusField = `status${itemId.charAt(0).toUpperCase() + itemId.slice(1)}`;
      return verification[statusField] || "PENDING";
    },
    [verification],
  );

  const getItemChecked = useCallback(
    (itemId) => {
      if (!verification) return false;
      const checkField = `check${itemId.charAt(0).toUpperCase() + itemId.slice(1)}`;
      return verification[checkField] || false;
    },
    [verification],
  );

  const handleItemChange = useCallback(
    async (itemId, verified, notApplicable = false, naJustification = "") => {
      if (!verification?.id) return;

      setSaving(true);
      setError(null);

      try {
        const response = await putToOpenElisServerJsonResponse(
          `/rest/biorepository/verification/${verification.id}/item`,
          JSON.stringify({
            itemName: itemId,
            verified,
            notApplicable,
            naJustification,
          }),
        );

        if (response.error) {
          setError(response.error);
        } else {
          // Update local state
          setVerification((prev) => ({
            ...prev,
            [`check${itemId.charAt(0).toUpperCase() + itemId.slice(1)}`]:
              verified,
            [`status${itemId.charAt(0).toUpperCase() + itemId.slice(1)}`]:
              notApplicable ? "N_A" : verified ? "VERIFIED" : "PENDING",
            overallStatus: response.status,
            completedCount: response.completedCount,
          }));
        }
      } catch (err) {
        setError("Failed to update verification item");
      } finally {
        setSaving(false);
      }
    },
    [verification?.id],
  );

  const handleComplete = useCallback(async () => {
    if (!verification?.id) return;

    setSaving(true);
    setError(null);

    try {
      const response = await postToOpenElisServerJsonResponse(
        `/rest/biorepository/verification/${verification.id}/complete`,
        JSON.stringify({}),
      );

      if (response.error) {
        setError(response.error);
      } else {
        if (onVerificationComplete) {
          onVerificationComplete(response);
        }
        onClose();
      }
    } catch (err) {
      setError("Failed to complete verification");
    } finally {
      setSaving(false);
    }
  }, [verification?.id, onVerificationComplete, onClose]);

  const handleQuarantine = useCallback(
    async (reason) => {
      if (!verification?.id) return;

      setSaving(true);
      setError(null);

      try {
        const response = await postToOpenElisServerJsonResponse(
          `/rest/biorepository/verification/${verification.id}/quarantine`,
          JSON.stringify({ reason }),
        );

        if (response.error) {
          setError(response.error);
        } else {
          if (onVerificationComplete) {
            onVerificationComplete(response);
          }
          onClose();
        }
      } catch (err) {
        setError("Failed to quarantine sample");
      } finally {
        setSaving(false);
      }
    },
    [verification?.id, onVerificationComplete, onClose],
  );

  const completedCount = verification?.completedCount || 0;
  const totalItems = checklistItems.length;
  const progress = (completedCount / totalItems) * 100;
  const isComplete = completedCount === totalItems;

  return (
    <Modal
      open={open}
      onRequestClose={onClose}
      modalHeading={intl.formatMessage({
        id: "biorepository.verification.modal.title",
        defaultMessage: "Documentation Verification",
      })}
      primaryButtonText={
        isComplete
          ? intl.formatMessage({
              id: "biorepository.verification.button.complete",
              defaultMessage: "Complete Verification",
            })
          : undefined
      }
      primaryButtonDisabled={!isComplete || saving}
      onRequestSubmit={handleComplete}
      secondaryButtonText={intl.formatMessage({
        id: "biorepository.button.cancel",
        defaultMessage: "Cancel",
      })}
      size="lg"
      passiveModal={!isComplete}
    >
      {loading && <Loading withOverlay description="Loading verification..." />}
      {saving && <Loading withOverlay description="Saving..." />}

      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "biorepository.verification.error.title",
            defaultMessage: "Error",
          })}
          subtitle={error}
          lowContrast
          onClose={() => setError(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {shipment && (
        <div style={{ marginBottom: "1rem" }}>
          <p>
            <strong>
              <FormattedMessage
                id="biorepository.verification.shipment"
                defaultMessage="Shipment:"
              />
            </strong>{" "}
            {shipment.deliveryReference || shipment.id}
          </p>
          {shipment.senderName && (
            <p style={{ fontSize: "0.875rem", color: "#525252" }}>
              <FormattedMessage
                id="biorepository.verification.sender"
                defaultMessage="From:"
              />{" "}
              {shipment.senderName}
            </p>
          )}
        </div>
      )}

      <ProgressBar
        label={intl.formatMessage(
          {
            id: "biorepository.verification.progress",
            defaultMessage: "Verification Progress: {completed} of {total}",
          },
          { completed: completedCount, total: totalItems },
        )}
        value={progress}
        status={isComplete ? "finished" : "active"}
        style={{ marginBottom: "1.5rem" }}
      />

      <Accordion>
        {checklistItems.map((item) => {
          const status = getItemStatus(item.id);
          const checked = getItemChecked(item.id);

          return (
            <AccordionItem
              key={item.id}
              title={
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: "0.5rem",
                  }}
                >
                  {status === "VERIFIED" && (
                    <Checkmark size={16} style={{ color: "green" }} />
                  )}
                  {status === "PENDING" && (
                    <WarningAlt size={16} style={{ color: "orange" }} />
                  )}
                  {status === "N_A" && <Tag size="sm">N/A</Tag>}
                  <span>{item.label}</span>
                </div>
              }
            >
              <p style={{ marginBottom: "1rem", color: "#525252" }}>
                {item.description}
              </p>

              {item.allowNA ? (
                <RadioButtonGroup
                  name={`verification-${item.id}`}
                  valueSelected={status}
                  onChange={(value) => {
                    if (value === "VERIFIED") {
                      handleItemChange(item.id, true, false);
                    } else if (value === "N_A") {
                      handleItemChange(item.id, false, true, "Not applicable");
                    } else {
                      handleItemChange(item.id, false, false);
                    }
                  }}
                  orientation="horizontal"
                >
                  <RadioButton
                    id={`${item.id}-verified`}
                    labelText={intl.formatMessage({
                      id: "biorepository.verification.status.verified",
                      defaultMessage: "Verified",
                    })}
                    value="VERIFIED"
                  />
                  <RadioButton
                    id={`${item.id}-na`}
                    labelText={intl.formatMessage({
                      id: "biorepository.verification.status.na",
                      defaultMessage: "N/A",
                    })}
                    value="N_A"
                  />
                  <RadioButton
                    id={`${item.id}-pending`}
                    labelText={intl.formatMessage({
                      id: "biorepository.verification.status.pending",
                      defaultMessage: "Pending",
                    })}
                    value="PENDING"
                  />
                </RadioButtonGroup>
              ) : (
                <Checkbox
                  id={`verification-${item.id}`}
                  labelText={intl.formatMessage({
                    id: "biorepository.verification.checkbox.verified",
                    defaultMessage: "Verified",
                  })}
                  checked={checked}
                  onChange={(e, { checked }) =>
                    handleItemChange(item.id, checked)
                  }
                />
              )}
            </AccordionItem>
          );
        })}
      </Accordion>

      {!isComplete && (
        <div style={{ marginTop: "1.5rem" }}>
          <InlineNotification
            kind="warning"
            title={intl.formatMessage({
              id: "biorepository.verification.incomplete.title",
              defaultMessage: "Verification Incomplete",
            })}
            subtitle={intl.formatMessage({
              id: "biorepository.verification.incomplete.message",
              defaultMessage:
                "All checklist items must be verified before completing. If verification cannot be completed, consider quarantining the shipment.",
            })}
            lowContrast
            hideCloseButton
          />
        </div>
      )}
    </Modal>
  );
}

DocumentationVerificationModal.propTypes = {
  open: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  shipment: PropTypes.object,
  onVerificationComplete: PropTypes.func,
};

export default DocumentationVerificationModal;
