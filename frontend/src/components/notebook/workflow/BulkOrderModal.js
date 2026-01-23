import React, { useState, useEffect, useCallback, useContext } from "react";
import {
  Modal,
  TextInput,
  Checkbox,
  RadioButtonGroup,
  RadioButton,
  InlineNotification,
  Loading,
  Tag,
  Grid,
  Column,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";
import "./NotebookWorkflow.css";

/**
 * BulkOrderModal - Modal for creating lab orders for multiple patients at once.
 * Implements bulk entry workflow for busy labs.
 *
 * @param {Object} props
 * @param {boolean} props.open - Whether the modal is open
 * @param {function} props.onClose - Callback when modal is closed
 * @param {Array} props.selectedPatients - Array of patient objects to create orders for
 * @param {number} props.notebookEntryId - The notebook entry ID
 * @param {number} props.notebookPageId - The notebook page ID
 * @param {function} props.onSuccess - Callback when orders are created successfully
 */
function BulkOrderModal({
  open,
  onClose,
  selectedPatients,
  notebookEntryId,
  notebookPageId,
  onSuccess,
}) {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  // Form state
  const [labNumberPrefix, setLabNumberPrefix] = useState("MEDLAB-");
  const [previewNumbers, setPreviewNumbers] = useState([]);
  const [loadingPreview, setLoadingPreview] = useState(false);

  // Test selection
  const [availableTests, setAvailableTests] = useState([]);
  const [selectedTests, setSelectedTests] = useState([]);
  const [loadingTests, setLoadingTests] = useState(false);

  // Order settings
  const [priority, setPriority] = useState("ROUTINE");

  // Submission state
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState(null);
  const [validationErrors, setValidationErrors] = useState([]);

  // Load available tests when modal opens
  useEffect(() => {
    if (open) {
      setError(null);
      setValidationErrors([]);
      setSelectedTests([]);
      loadAvailableTests();
      // Generate default prefix with current year
      const year = new Date().getFullYear();
      setLabNumberPrefix(`MEDLAB-${year}-`);
    }
  }, [open]);

  // Load preview of lab numbers when prefix changes or patients change
  useEffect(() => {
    if (open && labNumberPrefix && selectedPatients.length > 0) {
      loadLabNumberPreview();
    }
  }, [open, labNumberPrefix, selectedPatients.length]);

  const loadAvailableTests = useCallback(() => {
    setLoadingTests(true);
    getFromOpenElisServer("/rest/test-list", (response) => {
      if (response) {
        setAvailableTests(Array.isArray(response) ? response : []);
      }
      setLoadingTests(false);
    });
  }, []);

  const loadLabNumberPreview = useCallback(() => {
    if (!labNumberPrefix.trim()) {
      setPreviewNumbers([]);
      return;
    }
    setLoadingPreview(true);
    // Get preview of next N lab numbers from server
    getFromOpenElisServer(
      `/rest/medlab/lab-number-preview?prefix=${encodeURIComponent(labNumberPrefix.trim())}&count=${selectedPatients.length}`,
      (response) => {
        if (response && Array.isArray(response)) {
          setPreviewNumbers(response);
        } else {
          // Fallback: generate client-side preview (just for display)
          const preview = selectedPatients.map((_, index) => {
            const num = String(index + 1).padStart(3, "0");
            return `${labNumberPrefix.trim()}${num}`;
          });
          setPreviewNumbers(preview);
        }
        setLoadingPreview(false);
      },
    );
  }, [labNumberPrefix, selectedPatients.length]);

  const handleTestToggle = useCallback((testId) => {
    setSelectedTests((prev) => {
      if (prev.includes(testId)) {
        return prev.filter((id) => id !== testId);
      } else {
        return [...prev, testId];
      }
    });
  }, []);

  const handleSelectAllTests = useCallback(() => {
    if (selectedTests.length === availableTests.length) {
      setSelectedTests([]);
    } else {
      setSelectedTests(availableTests.map((t) => t.id));
    }
  }, [selectedTests.length, availableTests]);

  const isValid =
    labNumberPrefix.trim() &&
    selectedTests.length > 0 &&
    selectedPatients.length > 0;

  const handleSubmit = async () => {
    if (!isValid) return;

    setIsSubmitting(true);
    setError(null);
    setValidationErrors([]);

    const bulkOrderData = {
      patients: selectedPatients.map((p) => ({
        patientId: p.id || p.patientID,
        firstName: p.firstName,
        lastName: p.lastName,
      })),
      labNumberPrefix: labNumberPrefix.trim(),
      testIds: selectedTests,
      priority: priority,
      notebookEntryId: notebookEntryId,
      notebookPageId: notebookPageId,
    };

    postToOpenElisServerJsonResponse(
      "/rest/medlab/bulk-patient-orders",
      JSON.stringify(bulkOrderData),
      (response) => {
        setIsSubmitting(false);

        const isHttpError = typeof response?.statusCode === "number";
        if (response?.success && !isHttpError) {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage(
              {
                id: "medlab.bulkOrder.success",
                defaultMessage: "{count} lab orders created successfully",
              },
              { count: response.createdCount || selectedPatients.length },
            ),
            kind: NotificationKinds.success,
          });
          setNotificationVisible(true);

          if (onSuccess) {
            onSuccess(response.orders || []);
          }
          onClose();
        } else {
          // Extract detailed validation errors if available
          if (
            response?.validationErrors &&
            Array.isArray(response.validationErrors)
          ) {
            setValidationErrors(response.validationErrors);
            setError(
              intl.formatMessage({
                id: "medlab.bulkOrder.validationFailed",
                defaultMessage:
                  "Order creation failed due to validation errors",
              }),
            );
          } else {
            setError(
              response?.error ||
                intl.formatMessage({
                  id: "medlab.bulkOrder.error",
                  defaultMessage: "Failed to create bulk orders",
                }),
            );
          }
        }
      },
    );
  };

  const handleClose = () => {
    if (!isSubmitting) {
      onClose();
    }
  };

  return (
    <Modal
      open={open}
      onRequestClose={handleClose}
      modalHeading={intl.formatMessage({
        id: "medlab.bulkOrder.modal.title",
        defaultMessage: "Create Bulk Lab Orders",
      })}
      primaryButtonText={intl.formatMessage(
        {
          id: "medlab.bulkOrder.modal.submit",
          defaultMessage: "Create {count} Orders",
        },
        { count: selectedPatients.length },
      )}
      secondaryButtonText={intl.formatMessage({
        id: "label.button.cancel",
        defaultMessage: "Cancel",
      })}
      onRequestSubmit={handleSubmit}
      primaryButtonDisabled={!isValid || isSubmitting}
      size="lg"
    >
      <div className="bulk-order-modal">
        {/* Selected patients summary */}
        <div className="bulk-order-summary">
          <Tag type="blue">
            <FormattedMessage
              id="medlab.bulkOrder.selectedPatients"
              defaultMessage="{count} {count, plural, one {patient} other {patients}} selected"
              values={{ count: selectedPatients.length }}
            />
          </Tag>
        </div>

        {/* Patient list preview */}
        <div className="bulk-order-patients-preview">
          <p className="section-label">
            <FormattedMessage
              id="medlab.bulkOrder.patientsPreview"
              defaultMessage="Orders will be created for:"
            />
          </p>
          <ul className="patient-list">
            {selectedPatients.slice(0, 5).map((patient, index) => (
              <li key={patient.id || index}>
                {patient.lastName}, {patient.firstName}
                {previewNumbers[index] && (
                  <Tag type="gray" size="sm" style={{ marginLeft: "8px" }}>
                    {previewNumbers[index]}
                  </Tag>
                )}
              </li>
            ))}
            {selectedPatients.length > 5 && (
              <li className="more-patients">
                <FormattedMessage
                  id="medlab.bulkOrder.andMore"
                  defaultMessage="...and {count} more"
                  values={{ count: selectedPatients.length - 5 }}
                />
              </li>
            )}
          </ul>
        </div>

        <Grid>
          <Column lg={8} md={4} sm={4}>
            {/* Lab Number Prefix */}
            <div className="bulk-order-field">
              <TextInput
                id="lab-number-prefix"
                labelText={intl.formatMessage({
                  id: "medlab.bulkOrder.labNumberPrefix",
                  defaultMessage: "Lab Number Prefix",
                })}
                placeholder="MEDLAB-2026-"
                value={labNumberPrefix}
                onChange={(e) => setLabNumberPrefix(e.target.value)}
                helperText={intl.formatMessage({
                  id: "medlab.bulkOrder.labNumberHelper",
                  defaultMessage:
                    "Sequential numbers will be appended (e.g., 001, 002, 003)",
                })}
              />
              {loadingPreview && (
                <Loading small withOverlay={false} description="Loading..." />
              )}
            </div>

            {/* Priority */}
            <div className="bulk-order-field">
              <RadioButtonGroup
                legendText={intl.formatMessage({
                  id: "medlab.bulkOrder.priority",
                  defaultMessage: "Priority",
                })}
                name="bulk-priority"
                valueSelected={priority}
                onChange={(value) => setPriority(value)}
                orientation="horizontal"
              >
                <RadioButton
                  id="priority-routine"
                  value="ROUTINE"
                  labelText="Routine"
                />
                <RadioButton
                  id="priority-urgent"
                  value="URGENT"
                  labelText="Urgent"
                />
                <RadioButton id="priority-stat" value="STAT" labelText="STAT" />
              </RadioButtonGroup>
            </div>
          </Column>

          <Column lg={8} md={4} sm={4}>
            {/* Test Selection */}
            <div className="bulk-order-field">
              <div className="test-selection-header">
                <span className="section-label">
                  <FormattedMessage
                    id="medlab.bulkOrder.selectTests"
                    defaultMessage="Select Tests"
                  />
                  <span className="required-marker">*</span>
                </span>
                <Checkbox
                  id="select-all-tests"
                  labelText={intl.formatMessage({
                    id: "medlab.bulkOrder.selectAll",
                    defaultMessage: "Select All",
                  })}
                  checked={
                    availableTests.length > 0 &&
                    selectedTests.length === availableTests.length
                  }
                  indeterminate={
                    selectedTests.length > 0 &&
                    selectedTests.length < availableTests.length
                  }
                  onChange={handleSelectAllTests}
                />
              </div>

              {loadingTests ? (
                <Loading
                  small
                  withOverlay={false}
                  description="Loading tests..."
                />
              ) : (
                <div className="test-checkbox-grid">
                  {availableTests.map((test) => (
                    <div
                      key={test.id}
                      className="test-checkbox-item"
                      onClick={() => handleTestToggle(test.id)}
                      style={{ cursor: "pointer" }}
                    >
                      <Checkbox
                        id={`test-${test.id}`}
                        labelText={
                          test.value ||
                          test.localizedTestName ||
                          test.testName ||
                          test.name ||
                          "Unknown Test"
                        }
                        checked={selectedTests.includes(test.id)}
                        onChange={() => {}}
                      />
                    </div>
                  ))}
                </div>
              )}

              {selectedTests.length > 0 && (
                <p className="selected-count">
                  <FormattedMessage
                    id="medlab.bulkOrder.testsSelected"
                    defaultMessage="{count} {count, plural, one {test} other {tests}} selected"
                    values={{ count: selectedTests.length }}
                  />
                </p>
              )}
            </div>
          </Column>
        </Grid>

        {/* Error notification */}
        {error && (
          <InlineNotification
            kind="error"
            title={intl.formatMessage({
              id: "label.error",
              defaultMessage: "Error",
            })}
            subtitle={error}
            hideCloseButton
            lowContrast
          />
        )}

        {/* Detailed validation errors list */}
        {validationErrors.length > 0 && (
          <div className="validation-errors-list">
            <ul>
              {validationErrors.map((err, index) => (
                <li key={index} className="validation-error-item">
                  {err}
                </li>
              ))}
            </ul>
          </div>
        )}

        {/* Loading indicator */}
        {isSubmitting && (
          <Loading
            withOverlay={false}
            description={intl.formatMessage({
              id: "medlab.bulkOrder.creating",
              defaultMessage: "Creating lab orders...",
            })}
          />
        )}
      </div>
    </Modal>
  );
}

export default BulkOrderModal;
