import React, { useContext, useState } from "react";
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
} from "@carbon/react";
import { Checkmark, Warning } from "@carbon/icons-react";
import OrderWorkflowLayout from "../OrderWorkflowLayout";
import { useOrderContext } from "../OrderContext";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";

/**
 * OrderQA - Step 4: QA Review
 *
 * Final quality assurance review before order submission.
 * Shows complete order summary and QA checklist.
 */

const OrderQA = () => {
  const intl = useIntl();
  const history = useHistory();
  const { orderData, samples, saveOrder, resetOrder, labNumber } =
    useOrderContext();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [qaChecklist, setQaChecklist] = useState({
    patientInfoVerified: false,
    samplesVerified: false,
    labelsVerified: false,
    storageVerified: false,
  });
  const [isSubmitted, setIsSubmitted] = useState(false);

  const handleChecklistChange = (field) => {
    setQaChecklist((prev) => ({
      ...prev,
      [field]: !prev[field],
    }));
  };

  const allChecksComplete = Object.values(qaChecklist).every(Boolean);

  const handleSave = async () => {
    try {
      await saveOrder();
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "save.order.success.msg" }),
      });
      setNotificationVisible(true);
    } catch (error) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
    }
  };

  const handleSubmit = async () => {
    try {
      await saveOrder();
      setIsSubmitted(true);
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "save.order.success.msg" }),
      });
      setNotificationVisible(true);
    } catch (error) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
    }
  };

  const handleStartNewOrder = () => {
    resetOrder();
    history.push("/order/enter");
  };

  const patientName = orderData?.patientProperties
    ? `${orderData.patientProperties.firstName || ""} ${orderData.patientProperties.lastName || ""}`.trim()
    : "---";

  const displayLabNumber =
    labNumber || orderData?.sampleOrderItems?.labNo || "---";

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
                values={{ labNumber: displayLabNumber }}
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
      canProceed={allChecksComplete}
      onSave={handleSave}
      onSaveAndNext={handleSubmit}
    >
      {notificationVisible && <AlertDialog />}

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
            <Checkbox
              id="qa-patient-info"
              labelText={intl.formatMessage({
                id: "qa.checklist.patientInfo",
                defaultMessage: "Patient information is correct",
              })}
              checked={qaChecklist.patientInfoVerified}
              onChange={() => handleChecklistChange("patientInfoVerified")}
            />
            <Checkbox
              id="qa-samples"
              labelText={intl.formatMessage({
                id: "qa.checklist.samples",
                defaultMessage: "Sample types and tests are correct",
              })}
              checked={qaChecklist.samplesVerified}
              onChange={() => handleChecklistChange("samplesVerified")}
            />
            <Checkbox
              id="qa-labels"
              labelText={intl.formatMessage({
                id: "qa.checklist.labels",
                defaultMessage: "Labels have been printed and applied",
              })}
              checked={qaChecklist.labelsVerified}
              onChange={() => handleChecklistChange("labelsVerified")}
            />
            <Checkbox
              id="qa-storage"
              labelText={intl.formatMessage({
                id: "qa.checklist.storage",
                defaultMessage: "Storage locations have been assigned",
              })}
              checked={qaChecklist.storageVerified}
              onChange={() => handleChecklistChange("storageVerified")}
            />
          </div>

          {!allChecksComplete && (
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
                  <StructuredListCell>{displayLabNumber}</StructuredListCell>
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
