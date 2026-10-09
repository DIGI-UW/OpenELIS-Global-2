import React, { useContext, useState, useEffect, useRef } from "react";
import { useHistory } from "react-router-dom";
import { useWorkflowPrefix } from "../OrderContext";
import { useIntl, FormattedMessage } from "react-intl";
import { Stack, InlineNotification, Button } from "@carbon/react";
import { Warning } from "@carbon/icons-react";
import InlineNceForm from "../../nonconform/common/InlineNceForm";
import OrderWorkflowLayout from "../OrderWorkflowLayout";
import SaveFailureNotice, { saveFailureMessage } from "../SaveFailureNotice";
import { useOrderContext } from "../OrderContext";
import PrepareStorageSection from "./sections/PrepareStorageSection";
import OrderReferOutSection from "./referOut/OrderReferOutSection";
import { isFullyReferred } from "./referralState";
import { ConfigurationContext, NotificationContext } from "../../layout/Layout";
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
import PrepareLabelsSection from "./sections/PrepareLabelsSection";
import ConsentAccordionSection from "./sections/ConsentAccordionSection";
import "../order-workflow.scss";
import { prepareSamplesToContinue } from "./prepareSamplesChecklist";

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
  // The Labels section registers its row printer here so a sample card's
  // Print Labels button prints that tube's labels (FR-C6).
  const printLabelsRowRef = useRef(null);

  const {
    orderId,
    orderData,
    samples,
    setSamples,
    seedSamples,
    saveOrder,
    markStepComplete,
    isReadOnly,
    isEditMode,
    isLoading,
    testSampleAssignments,
    assignTestToSample,
    removeTestFromSample,
    updateSampleCollectionDetails,
    setOrderData,
    labNumber,
    storageSkipped,
    stageStorageSkipped,
    sampleCheckEnabled,
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
          seedSamples(mergedSamples);
        }
      } catch {
        // Failed to load pending requests
      }
    };

    loadPendingRequests();
  }, [orderId]);

  // Two levels of required (FR-A7, FR-D7). Save and exit needs the save
  // level: a sample with a sample type. Save and next needs the complete
  // level as well (see
  // prepareSamplesToContinue). Informed consent stays advisory by default (FRS
  // FR-5-001/FR-5-002); a site whose regulator requires it turns
  // consentRequiredForCollection on. Environmental and vector samples have no
  // human subject, so consent never applies to them.
  // Published under the Property enum's name, the way REQUESTER_REQUIRED is.
  const consentRequired =
    configurationProperties.CONSENT_REQUIRED_FOR_COLLECTION === "true";
  const consentSatisfied = !consentRequired || consentData.consentGiven;
  const canSave = samples?.length > 0 && samples.some((s) => s.sampleTypeId);
  const toContinue = prepareSamplesToContinue({
    samples,
    labNumber,
    consentSatisfied,
    intl,
  });
  const canProceed = canSave && toContinue.length === 0;

  // Check if we have any tests ordered
  const hasOrderedTests = samples.some(
    (s) => (s.tests && s.tests.length > 0) || (s.panels && s.panels.length > 0),
  );

  // The step's completion travels with its save (FR-F5): a save made while
  // the complete level is met marks the order Samples prepared.
  const progressStep = canProceed ? "SAMPLES_PREPARED" : null;

  const handleSave = async () => {
    try {
      await saveOrder(false, false, null, false, progressStep);
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "save.order.success.msg" }),
      });
      setNotificationVisible(true);
      return true;
    } catch (error) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: saveFailureMessage(intl, error),
      });
      setNotificationVisible(true);
      return false;
    }
  };

  // Save and next opens Sample check when the laboratory uses it; otherwise
  // this save finishes order entry (FR-K15) and the dashboard says so. An
  // order whose every tube is referred out has nothing for the in-house
  // Sample check, so it finishes here too (OGC-1423).
  const fullyReferred = isFullyReferred(samples);
  const handleSaveAndNext = async () => {
    try {
      await saveOrder(false, false, null, false, progressStep);
      markStepComplete("collect");
      if (sampleCheckEnabled && !fullyReferred) {
        history.push(
          labNumber
            ? `${workflowPrefix}/qa?order=${encodeURIComponent(labNumber)}`
            : `${workflowPrefix}/qa`,
        );
      } else {
        history.push(
          `${workflowPrefix}?done=${encodeURIComponent(labNumber || "")}`,
        );
      }
    } catch (error) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: saveFailureMessage(intl, error),
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
      title="order.step.prepare"
      canProceed={canProceed}
      canSave={canSave}
      onSave={handleSave}
      onSaveAndNext={handleSaveAndNext}
      toContinue={toContinue}
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
          labNumber={orderData?.sampleOrderItems?.labNo || labNumber || ""}
          referringSite={
            orderData?.sampleOrderItems?.referringSiteId
              ? {
                  id: orderData.sampleOrderItems.referringSiteId,
                  name: orderData.sampleOrderItems.referringSiteName || "",
                }
              : null
          }
        />

        {/* A: the collector could see the ordered tests but not add one. */}
        <CollectTestPickerSection
          samples={samples}
          setSamples={setSamples}
          isReadOnly={isReadOnly && !isEditMode}
        />

        {/* Section 2: Informed Consent */}
        <div id="consent-section">
          <ConsentAccordionSection
            consentData={consentData}
            onConsentChange={handleConsentChange}
            isReadOnly={isReadOnly && !isEditMode}
          />
        </div>

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
          printDisabled={isLoading}
          workflowType={workflowType}
          labNumber={orderData?.sampleOrderItems?.labNo || labNumber || ""}
          onPrintLabels={(sampleIndex) => {
            if (printLabelsRowRef.current) {
              printLabelsRowRef.current(sampleIndex);
            }
          }}
        />

        {/* Labels for the order and every tube, from the presets and the test
            catalog (FR-I2). The quantities travel with this step's save and
            printing reads the saved rows (FR-I6, FR-I7). */}
        <PrepareLabelsSection
          isReadOnly={isReadOnly && !isEditMode}
          onSaveBeforePrint={handleSave}
          registerPrintRow={(printRow) => {
            printLabelsRowRef.current = printRow;
          }}
        />

        {/* Storage and referral, per sample, saved with this step (FR-E1,
            FR-E2, FR-E5). Formerly the Label & Store step. */}
        <PrepareStorageSection
          samples={samples}
          updateSampleCollectionDetails={updateSampleCollectionDetails}
          storageSkipped={storageSkipped}
          onStorageSkippedChange={stageStorageSkipped}
          labNumber={labNumber}
          isReadOnly={isReadOnly && !isEditMode}
        />

        {orderId && <OrderReferOutSection />}

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
