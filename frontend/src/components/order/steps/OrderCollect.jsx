import React, { useContext, useState, useEffect, useRef } from "react";
import { useHistory } from "react-router-dom";
import { useWorkflowPrefix } from "../OrderContext";
import { useIntl, FormattedMessage } from "react-intl";
import { Stack, InlineNotification, Button } from "@carbon/react";
import { Warning } from "@carbon/icons-react";
import InlineNceForm from "../../nonconform/common/InlineNceForm";
import OrderWorkflowLayout from "../OrderWorkflowLayout";
import SaveFailureNotice from "../SaveFailureNotice";
import { useOrderContext } from "../OrderContext";
import {
  ConfigurationContext,
  NotificationContext,
} from "../../layout/contexts";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import { getFromOpenElisServer } from "../../utils/Utils";
import {
  getPendingRequests,
  convertRequestsToSamples,
} from "../api/sampleTypeRequestApi";
import SampleAcceptanceReview from "./sections/SampleAcceptanceReview";
import { getEnforcement } from "../api/sampleAcceptanceApi";
import RequestedTestsSection from "./sections/RequestedTestsSection";
import CollectTestPickerSection from "./sections/CollectTestPickerSection";
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
  const { configurationProperties = {} } =
    useContext(ConfigurationContext) || {};

  // Sample types from API
  const [showNceForm, setShowNceForm] = useState(false);
  // Intake acceptance is hidden when this order's domain enforcement is OFF,
  // matching QA Review. Default false → fail open.
  const [acceptanceOff, setAcceptanceOff] = useState(false);

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

  const workflowType =
    orderData?.sampleOrderItems?.environmentalFields?.workflowType ||
    "clinical";

  useEffect(() => {
    let active = true;
    getEnforcement().then((modes) => {
      if (!active) return;
      setAcceptanceOff((modes?.[workflowType] || "").toUpperCase() === "OFF");
    });
    return () => {
      active = false;
    };
  }, [workflowType]);

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
  // Informed consent stays advisory by default, which is what FRS FR-5-001/
  // FR-5-002 describes, but a site whose regulator requires consent before
  // collection can turn consentRequiredForCollection on and have it gate.
  // Environmental and vector samples have no human subject, so they capture
  // no consent and the gate never applies to them.
  const admissionDate = orderData?.microbiologyOrderDetail?.admissionDate || "";
  const hasCollectionDateConflict = samples.some((sample) =>
    isCollectionDateBeforeAdmissionDate(sample.collectionDate, admissionDate),
  );
  // Published under the Property enum's name, the way REQUESTER_REQUIRED is.
  const consentRequired =
    configurationProperties.CONSENT_REQUIRED_FOR_COLLECTION === "true";
  const consentSatisfied = !consentRequired || consentData.consentGiven;
  const canProceed =
    samples?.length > 0 &&
    samples.some((s) => s.sampleTypeId) &&
    !hasCollectionDateConflict &&
    consentSatisfied;

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
      <SaveFailureNotice />

      <Stack gap={7}>
        {consentRequired && !consentData.consentGiven && (
          <InlineNotification
            kind="warning"
            title={intl.formatMessage({
              id: "collect.consentRequired.title",
              defaultMessage: "Informed consent is required",
            })}
            subtitle={intl.formatMessage({
              id: "collect.consentRequired.subtitle",
              defaultMessage:
                "This laboratory requires consent to be recorded before a collection can proceed.",
            })}
            hideCloseButton
            lowContrast
          />
        )}

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

        {/* A: the collector could see the ordered tests but not add one. */}
        <CollectTestPickerSection
          samples={samples}
          setSamples={setSamples}
          isReadOnly={isReadOnly && !isEditMode}
        />

        {/* Section 2: Informed Consent */}
        <ConsentAccordionSection
          consentData={consentData}
          onConsentChange={handleConsentChange}
          isReadOnly={isReadOnly && !isEditMode}
        />

        {/* A collector holding a hemolyzed specimen could log an NCE here but
            had to walk to QA Review to reject or resample it. The same
            per-specimen acceptance table is mounted here, without the submit
            gate that belongs to QA. Acceptance is recorded against
            sample_items, so it appears once the collection has been saved. */}
        {!acceptanceOff && samples.some((s) => s.sampleItemId) && (
          <SampleAcceptanceReview
            orderId={orderId}
            labNumber={labNumber}
            samples={samples}
          />
        )}

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
