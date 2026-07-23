import React, { useContext, useState, useEffect, useCallback } from "react";
import { useHistory } from "react-router-dom";
import { useIntl, FormattedMessage } from "react-intl";
import {
  Tile,
  Accordion,
  AccordionItem,
  StructuredListWrapper,
  StructuredListBody,
  StructuredListRow,
  StructuredListCell,
  Checkbox,
  InlineNotification,
  Tag,
  Loading,
} from "@carbon/react";
import { Checkmark } from "@carbon/icons-react";
import OrderWorkflowLayout from "../OrderWorkflowLayout";
import { useOrderContext } from "../OrderContext";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";

/**
 * OrderQA - Step 4: QA Review
 *
 * Final quality assurance review before order submission.
 * Shows complete order summary and QA checklist.
 * Checklist items are configured via Dictionary (category: QAChecklistItem).
 */

const OrderQA = () => {
  const intl = useIntl();
  const history = useHistory();
  const {
    orderData,
    samples,
    saveOrder,
    resetOrder,
    labNumber,
    markStepComplete,
  } = useOrderContext();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  // Checklist items from Dictionary
  const [checklistItems, setChecklistItems] = useState([]);
  // Map of itemKey -> boolean for verification status
  const [verifiedItems, setVerifiedItems] = useState({});
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);

  const displayLabNumber =
    labNumber || orderData?.sampleOrderItems?.labNo || "";

  // Load QA checklist config and status from backend on mount
  const loadChecklist = useCallback(() => {
    if (!displayLabNumber) {
      // If no lab number, just load the config
      getFromOpenElisServer("/rest/qa-checklist/config", (response) => {
        if (response && Array.isArray(response)) {
          setChecklistItems(response);
          // Initialize all items as unchecked
          const initialState = {};
          response.forEach((item) => {
            initialState[item.itemKey] = false;
          });
          setVerifiedItems(initialState);
        }
        setIsLoading(false);
      });
      return;
    }

    getFromOpenElisServer(
      `/rest/qa-checklist/by-lab-number/${displayLabNumber}`,
      (response) => {
        if (response && !response.error) {
          // Set checklist items from config
          if (
            response.checklistItems &&
            Array.isArray(response.checklistItems)
          ) {
            setChecklistItems(response.checklistItems);
          }
          // Set verified items state
          if (response.verifiedItems) {
            setVerifiedItems(response.verifiedItems);
          } else {
            // Initialize all items as unchecked
            const initialState = {};
            (response.checklistItems || []).forEach((item) => {
              initialState[item.itemKey] = false;
            });
            setVerifiedItems(initialState);
          }
        }
        setIsLoading(false);
      },
    );
  }, [displayLabNumber]);

  useEffect(() => {
    loadChecklist();
  }, [loadChecklist]);

  const handleChecklistChange = (itemKey) => {
    setVerifiedItems((prev) => ({
      ...prev,
      [itemKey]: !prev[itemKey],
    }));
  };

  // Check if all items are verified
  const allItemsComplete = checklistItems.every(
    (item) => verifiedItems[item.itemKey] === true,
  );

  // Save checklist to backend
  const saveChecklist = async () => {
    if (!displayLabNumber) {
      return Promise.resolve();
    }

    return new Promise((resolve, reject) => {
      postToOpenElisServerJsonResponse(
        "/rest/qa-checklist",
        JSON.stringify({
          labNumber: displayLabNumber,
          verifiedItems: verifiedItems,
        }),
        (response) => {
          if (response && response.success) {
            resolve(response);
          } else {
            reject(new Error(response?.error || "Failed to save checklist"));
          }
        },
      );
    });
  };

  const handleSave = async () => {
    setIsSaving(true);
    try {
      // Save order data first
      await saveOrder();
      // Then save checklist
      await saveChecklist();
      // Mark QA step complete if all checks are done
      if (allItemsComplete) {
        markStepComplete("qa");
      }
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "save.order.success.msg" }),
      });
      setNotificationVisible(true);
    } catch (error) {
      console.error("Error saving QA checklist:", error);
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
    } finally {
      setIsSaving(false);
    }
  };

  const handleSubmit = async () => {
    setIsSaving(true);
    try {
      await saveOrder();
      await saveChecklist();
      markStepComplete("qa");
      setIsSubmitted(true);
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "order.submitted.success.msg",
          defaultMessage: "Order submitted successfully",
        }),
      });
      setNotificationVisible(true);
    } catch (error) {
      console.error("Error submitting order:", error);
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
    } finally {
      setIsSaving(false);
    }
  };

  const handleStartNewOrder = () => {
    resetOrder();
    history.push("/order/enter");
  };

  const patientName = orderData?.patientProperties
    ? `${orderData.patientProperties.firstName || ""} ${orderData.patientProperties.lastName || ""}`.trim()
    : "---";

  // Get label for checklist item - use localizedName or label from dictionary
  const getItemLabel = (item) => {
    // Prefer localizedName, fall back to label (localAbbreviation), then itemKey
    return item.localizedName || item.label || item.itemKey;
  };

  if (isLoading) {
    return (
      <OrderWorkflowLayout
        currentStep={3}
        title="order.step.qa"
        showSaveButtons={false}
      >
        <Loading withOverlay={false} description="Loading checklist..." />
      </OrderWorkflowLayout>
    );
  }

  if (isSubmitted) {
    return (
      <OrderWorkflowLayout
        currentStep={3}
        title="order.step.qa"
        showSaveButtons={false}
      >
        <Tile className="qa-success-tile">
          <div className="success-content">
            <Checkmark size={48} className="success-icon" />
            <h3>
              <FormattedMessage
                id="order.submit.success"
                defaultMessage="Order Submitted Successfully"
              />
            </h3>
            <p>
              <FormattedMessage
                id="order.submit.labNumber"
                defaultMessage="Lab Number: {labNumber}"
                values={{ labNumber: displayLabNumber || "---" }}
              />
            </p>
            <button
              className="cds--btn cds--btn--primary"
              onClick={handleStartNewOrder}
            >
              <FormattedMessage
                id="order.start.new"
                defaultMessage="Start New Order"
              />
            </button>
          </div>
        </Tile>
      </OrderWorkflowLayout>
    );
  }

  return (
    <OrderWorkflowLayout
      currentStep={3}
      title="order.step.qa"
      canProceed={allItemsComplete}
      onSave={handleSave}
      onSaveAndNext={handleSubmit}
    >
      {notificationVisible && <AlertDialog />}
      {isSaving && <Loading withOverlay description="Saving..." />}

      <div className="qa-review-container">
        {/* QA Checklist */}
        <Tile className="qa-checklist-tile">
          <h4>
            <FormattedMessage
              id="qa.checklist.title"
              defaultMessage="QA Checklist"
            />
          </h4>
          <p className="qa-checklist-instructions">
            <FormattedMessage
              id="qa.checklist.instructions"
              defaultMessage="Verify all items before submitting the order"
            />
          </p>

          <div className="qa-checklist-items">
            {checklistItems.map((item) => (
              <Checkbox
                key={item.itemKey}
                id={`qa-${item.itemKey}`}
                labelText={getItemLabel(item)}
                checked={verifiedItems[item.itemKey] || false}
                onChange={() => handleChecklistChange(item.itemKey)}
                disabled={isSaving}
              />
            ))}
          </div>

          {!allItemsComplete && (
            <InlineNotification
              kind="warning"
              title={intl.formatMessage({
                id: "qa.checklist.incomplete",
                defaultMessage:
                  "Please complete all QA checks before submitting",
              })}
              hideCloseButton
              lowContrast
            />
          )}
        </Tile>

        {/* Order Summary */}
        <Accordion>
          <AccordionItem
            title={intl.formatMessage({
              id: "qa.summary.patient",
              defaultMessage: "Patient Information",
            })}
            open
          >
            <StructuredListWrapper isCondensed>
              <StructuredListBody>
                <StructuredListRow>
                  <StructuredListCell>
                    <FormattedMessage id="order.summary.patientName" />
                  </StructuredListCell>
                  <StructuredListCell>{patientName}</StructuredListCell>
                </StructuredListRow>
                <StructuredListRow>
                  <StructuredListCell>
                    <FormattedMessage
                      id="patient.dob"
                      defaultMessage="Date of Birth"
                    />
                  </StructuredListCell>
                  <StructuredListCell>
                    {orderData?.patientProperties?.birthDateForDisplay || "---"}
                  </StructuredListCell>
                </StructuredListRow>
                <StructuredListRow>
                  <StructuredListCell>
                    <FormattedMessage
                      id="patient.gender"
                      defaultMessage="Gender"
                    />
                  </StructuredListCell>
                  <StructuredListCell>
                    {orderData?.patientProperties?.gender || "---"}
                  </StructuredListCell>
                </StructuredListRow>
              </StructuredListBody>
            </StructuredListWrapper>
          </AccordionItem>

          <AccordionItem
            title={intl.formatMessage({
              id: "qa.summary.samples",
              defaultMessage: "Samples & Tests",
            })}
            open
          >
            {samples && samples.length > 0 ? (
              samples.map((sample, index) => (
                <div key={index} className="qa-sample-item">
                  <Tag type="blue" size="sm">
                    {sample.name ||
                      sample.sampleTypeName ||
                      `Sample ${index + 1}`}
                  </Tag>
                  <ul className="qa-test-list">
                    {sample.tests?.map((test, testIndex) => (
                      <li key={testIndex}>{test.name}</li>
                    ))}
                  </ul>
                </div>
              ))
            ) : (
              <p>
                <FormattedMessage
                  id="qa.summary.noSamples"
                  defaultMessage="No samples added"
                />
              </p>
            )}
          </AccordionItem>

          <AccordionItem
            title={intl.formatMessage({
              id: "qa.summary.order",
              defaultMessage: "Order Details",
            })}
          >
            <StructuredListWrapper isCondensed>
              <StructuredListBody>
                <StructuredListRow>
                  <StructuredListCell>
                    <FormattedMessage id="order.summary.accessionNumber" />
                  </StructuredListCell>
                  <StructuredListCell>
                    {displayLabNumber || "---"}
                  </StructuredListCell>
                </StructuredListRow>
                <StructuredListRow>
                  <StructuredListCell>
                    <FormattedMessage
                      id="sample.collection.date"
                      defaultMessage="Collection Date"
                    />
                  </StructuredListCell>
                  <StructuredListCell>
                    {samples?.[0]?.sampleXML?.collectionDate ||
                      orderData?.sampleOrderItems?.collectionDate ||
                      "---"}
                  </StructuredListCell>
                </StructuredListRow>
                <StructuredListRow>
                  <StructuredListCell>
                    <FormattedMessage
                      id="order.requester"
                      defaultMessage="Requester"
                    />
                  </StructuredListCell>
                  <StructuredListCell>
                    {orderData?.sampleOrderItems?.providerFirstName || ""}{" "}
                    {orderData?.sampleOrderItems?.providerLastName || "---"}
                  </StructuredListCell>
                </StructuredListRow>
              </StructuredListBody>
            </StructuredListWrapper>
          </AccordionItem>
        </Accordion>
      </div>
    </OrderWorkflowLayout>
  );
};

export default OrderQA;
