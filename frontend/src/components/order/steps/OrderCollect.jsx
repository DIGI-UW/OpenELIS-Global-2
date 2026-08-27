import React, { useContext, useState, useEffect, useRef } from "react";
import { useHistory } from "react-router-dom";
import { useWorkflowPrefix } from "../OrderContext";
import { useIntl, FormattedMessage } from "react-intl";
import { Stack, InlineNotification, Button } from "@carbon/react";
import { Warning } from "@carbon/icons-react";
import InlineNceForm from "../../nonconform/common/InlineNceForm";
import OrderWorkflowLayout from "../OrderWorkflowLayout";
import { useOrderContext } from "../OrderContext";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import { getFromOpenElisServer } from "../../utils/Utils";
import {
  getPendingRequests,
  convertRequestsToSamples,
} from "../api/sampleTypeRequestApi";
import RequestedTestsSection from "./sections/RequestedTestsSection";
import SamplesCollectionSection from "./sections/SamplesCollectionSection";
import ConsentAccordionSection from "./sections/ConsentAccordionSection";
import "../order-workflow.scss";
import { isCollectionDateBeforeAdmissionDate } from "../dateUtils";

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
  const workflowPrefix = useWorkflowPrefix();
  const componentMounted = useRef(true);

  const {
    orderId,
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
    setOrderData,
    labNumber,
  } = useOrderContext();

  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  // Sample types from API
  const [showNceForm, setShowNceForm] = useState(false);

  // Sample types from API
  const [sampleTypes, setSampleTypes] = useState([]);
  // Units of measure for sample collection
  const [unitOfMeasures, setUnitOfMeasures] = useState([]);

  // Consent is already part of canonical order state; do not mirror it locally.
  const consentData = {
    consentGiven: orderData?.sampleOrderItems?.consentGiven || false,
    consentFormReference:
      orderData?.sampleOrderItems?.consentFormReference || "",
    consentRecordedAt: orderData?.sampleOrderItems?.consentRecordedAt || "",
    consentRecordedBy: orderData?.sampleOrderItems?.consentRecordedBy || "",
  };

  // Fetch sample types and UOMs on mount
  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/user-sample-types", (response) => {
      if (componentMounted.current && response) {
        setSampleTypes(response);
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

  // Load pending sample type requests when orderId is available
  useEffect(() => {
    const loadPendingRequests = async () => {
      if (!orderId || !componentMounted.current) return;

      // Only load if samples don't already have sampleItemIds (not yet collected)
      const hasSampleItemIds = samples.some((s) => s.sampleItemId);
      if (hasSampleItemIds) return;

      try {
        const requests = await getPendingRequests(orderId);
        if (componentMounted.current && requests && requests.length > 0) {
          // Convert pending requests to samples array for the UI
          const samplesFromRequests = convertRequestsToSamples(requests);
          // Merge with any existing sample data.
          // collectionDate/Time are intentionally NOT preserved from existing:
          // the backend stores the order entry date there, not an actual
          // collection date. SampleCollectionCard will auto-fill them to
          // today when they are empty. Only Step-2-specific fields (collector,
          // conditions, receivedDate/Time) are preserved.
          const mergedSamples = samplesFromRequests.map((reqSample, idx) => {
            const existing = samples[idx];
            if (existing && existing.sampleTypeId === reqSample.sampleTypeId) {
              return {
                ...reqSample,
                collectorId: existing.collectorId || reqSample.collectorId,
                collectionConditions:
                  existing.collectionConditions ||
                  reqSample.collectionConditions,
                receivedDate: existing.receivedDate || reqSample.receivedDate,
                receivedTime: existing.receivedTime || reqSample.receivedTime,
              };
            }
            return reqSample;
          });
          setSamples(mergedSamples);
        }
      } catch {
        // Failed to load pending requests
      }
    };

    loadPendingRequests();
  }, [orderId]);

  // Validate that at least one sample with a sample type is present.
  // Informed consent is advisory only (FRS FR-5-001/FR-5-002) — does not gate submission.
  const admissionDate = orderData?.microbiologyOrderDetail?.admissionDate || "";
  const hasCollectionDateConflict = samples.some((sample) =>
    isCollectionDateBeforeAdmissionDate(sample.collectionDate, admissionDate),
  );
  const canProceed =
    samples?.length > 0 &&
    samples.some((s) => s.sampleTypeId) &&
    !hasCollectionDateConflict;

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
    } catch {
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
      history.push(`${workflowPrefix}/label`);
    } catch {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
    }
  };

  const handleConsentChange = (updatedConsent) => {
    // Sync consent data with orderData.sampleOrderItems for backend persistence
    setOrderData({
      ...orderData,
      sampleOrderItems: {
        ...orderData.sampleOrderItems,
        consentGiven: updatedConsent.consentGiven,
        consentFormReference: updatedConsent.consentFormReference,
        consentRecordedAt: updatedConsent.consentRecordedAt,
        consentRecordedBy: updatedConsent.consentRecordedBy,
      },
    });
  };

  return (
    <OrderWorkflowLayout
      title="order.step.collect"
      canProceed={canProceed}
      canSave={!hasCollectionDateConflict}
      onSave={handleSave}
      onSaveAndNext={handleSaveAndNext}
      extraButtons={
        labNumber && (
          <Button
            kind="danger--tertiary"
            size="md"
            renderIcon={Warning}
            onClick={() => setShowNceForm((v) => !v)}
          >
            <FormattedMessage
              id="nce.button.reportNce"
              defaultMessage="Report NCE"
            />
          </Button>
        )
      }
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

        {/* Section 2: Informed Consent */}
        <ConsentAccordionSection
          consentData={consentData}
          onConsentChange={handleConsentChange}
          isReadOnly={isReadOnly && !isEditMode}
        />

        {/* Section 3: Samples Collection */}
        <SamplesCollectionSection
          samples={samples}
          setSamples={setSamples}
          sampleTypes={sampleTypes}
          unitOfMeasures={unitOfMeasures}
          updateSampleCollectionDetails={updateSampleCollectionDetails}
          isReadOnly={isReadOnly && !isEditMode}
          admissionDate={admissionDate}
        />

        {showNceForm && labNumber && (
          <InlineNceForm
            accessionNumber={labNumber}
            onClose={() => setShowNceForm(false)}
            onSubmitSuccess={() => setShowNceForm(false)}
          />
        )}
      </Stack>
    </OrderWorkflowLayout>
  );
};

export default OrderCollect;
