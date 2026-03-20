import React, { useContext, useState, useEffect, useRef } from "react";
import { useHistory } from "react-router-dom";
import { useIntl, FormattedMessage } from "react-intl";
import { Stack, InlineNotification } from "@carbon/react";
import OrderWorkflowLayout from "../OrderWorkflowLayout";
import { useOrderContext } from "../OrderContext";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import { getFromOpenElisServer } from "../../utils/Utils";
import RequestedTestsSection from "./sections/RequestedTestsSection";
import SamplesCollectionSection from "./sections/SamplesCollectionSection";
import "../order-workflow.scss";

/**
 * OrderCollect - Step 2: Collect Sample
 *
 * Full implementation based on FRS and UI mockups.
 *
 * Sections:
 * 1. Requested Tests - Shows ordered tests with sample type assignment
 * 2. Samples - Collection details for each sample
 */

const OrderCollect = () => {
  const intl = useIntl();
  const history = useHistory();
  const componentMounted = useRef(true);

  const {
    orderData,
    samples,
    setSamples,
    saveOrder,
    markStepComplete,
    isReadOnly,
    isEditMode,
    testSampleAssignments,
    assignTestToSample,
    removeTestFromSample,
    updateSampleCollectionDetails,
  } = useOrderContext();

  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  // Sample types from API
  const [sampleTypes, setSampleTypes] = useState([]);
  const [isLoadingSampleTypes, setIsLoadingSampleTypes] = useState(true);

  // Units of measure for sample collection
  const [unitOfMeasures, setUnitOfMeasures] = useState([]);

  // Fetch sample types and UOMs on mount
  useEffect(() => {
    componentMounted.current = true;
    setIsLoadingSampleTypes(true);

    getFromOpenElisServer("/rest/user-sample-types", (response) => {
      if (componentMounted.current && response) {
        setSampleTypes(response);
        setIsLoadingSampleTypes(false);
      }
    });

    // Fetch sample collection UOMs (type=SAMPLE_COLLECTION)
    getFromOpenElisServer("/rest/uom?type=SAMPLE_COLLECTION", (response) => {
      if (componentMounted.current && response) {
        setUnitOfMeasures(response);
      }
    });

    return () => {
      componentMounted.current = false;
    };
  }, []);

  // Validate that at least one sample with a sample type is present
  const canProceed =
    samples &&
    samples.length > 0 &&
    samples.some((sample) => sample.sampleTypeId);

  // Check if we have any tests ordered
  const hasOrderedTests = samples.some(
    (s) => (s.tests && s.tests.length > 0) || (s.panels && s.panels.length > 0),
  );

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

  const handleSaveAndNext = async () => {
    try {
      await saveOrder();
      markStepComplete("collect");
      history.push("/order/label");
    } catch (error) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
    }
  };

  return (
    <OrderWorkflowLayout
      currentStep={1}
      title="order.step.collect"
      canProceed={canProceed}
      onSave={handleSave}
      onSaveAndNext={handleSaveAndNext}
    >
      {notificationVisible && <AlertDialog />}

      <Stack gap={7}>
        {/* Warning if no tests ordered */}
        {!hasOrderedTests && (
          <InlineNotification
            kind="warning"
            title={intl.formatMessage({
              id: "collect.noTestsWarning.title",
              defaultMessage: "No tests ordered",
            })}
            subtitle={intl.formatMessage({
              id: "collect.noTestsWarning.subtitle",
              defaultMessage:
                "Go back to Step 1 (Enter Order) to add tests and panels before collecting samples.",
            })}
            hideCloseButton
            lowContrast
          />
        )}

        {/* Section 1: Requested Tests */}
        <RequestedTestsSection
          samples={samples}
          setSamples={setSamples}
          testSampleAssignments={testSampleAssignments}
          assignTestToSample={assignTestToSample}
          removeTestFromSample={removeTestFromSample}
          sampleTypes={sampleTypes}
          isReadOnly={isReadOnly && !isEditMode}
        />

        {/* Section 2: Samples Collection */}
        <SamplesCollectionSection
          samples={samples}
          setSamples={setSamples}
          sampleTypes={sampleTypes}
          unitOfMeasures={unitOfMeasures}
          updateSampleCollectionDetails={updateSampleCollectionDetails}
          isReadOnly={isReadOnly && !isEditMode}
        />
      </Stack>
    </OrderWorkflowLayout>
  );
};

export default OrderCollect;
