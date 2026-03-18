import React, {
  createContext,
  useState,
  useCallback,
  useContext,
  useEffect,
  useRef,
} from "react";
import { getFromOpenElisServer, postToOpenElisServer } from "../utils/Utils";
import { SampleOrderFormValues } from "../formModel/innitialValues/OrderEntryFormValues";

/**
 * OrderContext - Shared state for the decoupled sample collection workflow.
 *
 * This context provides order state that persists across the 4 independent steps:
 * - Enter Order (/order/enter)
 * - Collect Sample (/order/collect)
 * - Label & Store (/order/label)
 * - QA Review (/order/qa)
 *
 * Features:
 * - Auto-save every 30 seconds on dirty forms
 * - Save status indicator (Saved, Saving..., Unsaved changes)
 * - Read-only mode for barcode-loaded orders with Edit toggle
 * - Browser navigation warning for unsaved changes
 */

const AUTO_SAVE_INTERVAL = 30000; // 30 seconds

export const SaveStatus = {
  SAVED: "saved",
  SAVING: "saving",
  UNSAVED: "unsaved",
  ERROR: "error",
};

export const OrderContext = createContext({
  // Order identification
  orderId: null,
  labNumber: null,

  // Order data (form values)
  orderData: null,

  // Samples associated with the order
  samples: [],

  // Read-only mode (when order is loaded via barcode scan)
  isReadOnly: false,

  // Edit mode (user clicked Edit to modify read-only order)
  isEditMode: false,

  // Current step index (0-3)
  currentStep: 0,

  // Loading and submission states
  isLoading: false,
  isSubmitting: false,

  // Save status for auto-save indicator
  saveStatus: SaveStatus.SAVED,

  // Dirty flag for unsaved changes
  isDirty: false,

  // Error state
  error: null,

  // Step progress tracking
  stepProgress: {
    enter: false,
    collect: false,
    label: false,
    qa: false,
  },

  // Actions
  loadOrder: () => {},
  saveOrder: () => {},
  setCurrentStep: () => {},
  setOrderData: () => {},
  setSamples: () => {},
  resetOrder: () => {},
  enableEditMode: () => {},
  markStepComplete: () => {},
});

export const sampleObject = {
  index: 0,
  sampleItemId: "", // ID of existing sample_item (for updates)
  sampleRejected: false,
  rejectionReason: "",
  sampleTypeId: "",
  sampleXML: null,
  panels: [],
  tests: [],
  requestReferralEnabled: false,
  referralItems: [],
};

/**
 * Get current time formatted as HH:MM
 */
const getCurrentTime = () => {
  const now = new Date();
  const hours = String(now.getHours()).padStart(2, "0");
  const minutes = String(now.getMinutes()).padStart(2, "0");
  return `${hours}:${minutes}`;
};

/**
 * Initialize order data with minimal defaults.
 * Date fields will be populated from API response.
 */
const getInitialOrderData = () => {
  return {
    ...SampleOrderFormValues,
    // currentDate will be set from API
    currentDate: "",
    sampleOrderItems: {
      ...SampleOrderFormValues.sampleOrderItems,
      // Date fields will be set from API
      requestDate: "",
      receivedDateForDisplay: "",
      receivedTime: getCurrentTime(),
      // paymentOptionSelection should be empty or a valid numeric string
      paymentOptionSelection: "",
    },
  };
};

export const OrderProvider = ({ children }) => {
  const [orderId, setOrderId] = useState(null);
  const [labNumber, setLabNumber] = useState(null);
  const [orderData, setOrderDataState] = useState(getInitialOrderData);
  const [samples, setSamplesState] = useState([sampleObject]);
  const [isReadOnly, setIsReadOnly] = useState(false);
  const [isEditMode, setIsEditMode] = useState(false);
  const [currentStep, setCurrentStep] = useState(0);
  const [isLoading, setIsLoading] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [saveStatus, setSaveStatus] = useState(SaveStatus.SAVED);
  const [isDirty, setIsDirty] = useState(false);
  const [error, setError] = useState(null);
  const [stepProgress, setStepProgress] = useState({
    enter: false,
    collect: false,
    label: false,
    qa: false,
  });

  const autoSaveTimerRef = useRef(null);
  const lastSavedDataRef = useRef(null);

  /**
   * Wrapper for setOrderData that marks form as dirty
   */
  const setOrderData = useCallback((newData) => {
    setOrderDataState(newData);
    setIsDirty(true);
    setSaveStatus(SaveStatus.UNSAVED);
  }, []);

  /**
   * Wrapper for setSamples that marks form as dirty
   */
  const setSamples = useCallback((newSamples) => {
    setSamplesState(newSamples);
    setIsDirty(true);
    setSaveStatus(SaveStatus.UNSAVED);
  }, []);

  /**
   * Load an existing order by lab number (accession number).
   * Used when user scans a barcode or enters a lab number.
   * Loads in read-only mode by default (user must click Edit to modify).
   */
  const loadOrder = useCallback(async (searchLabNumber, readOnly = true) => {
    setIsLoading(true);
    setError(null);

    return new Promise((resolve, reject) => {
      getFromOpenElisServer(
        `/rest/order/search?labNumber=${encodeURIComponent(searchLabNumber)}`,
        (response) => {
          setIsLoading(false);

          if (response && response.labNumber) {
            setOrderId(response.id);
            setLabNumber(response.labNumber);

            // Build order data by merging response fields with defaults
            // The backend returns patientProperties at top level and inside orderData
            const loadedOrderData = {
              ...SampleOrderFormValues,
              ...(response.orderData || {}),
              patientProperties: {
                ...SampleOrderFormValues.patientProperties,
                ...(response.patientProperties || {}),
                ...(response.orderData?.patientProperties || {}),
              },
              patientUpdateStatus: "UPDATE",
              sampleOrderItems: {
                ...SampleOrderFormValues.sampleOrderItems,
                ...(response.sampleOrderItems || {}),
                labNo: response.labNumber,
              },
            };

            setOrderDataState(loadedOrderData);
            setSamplesState(response.samples || [sampleObject]);
            setIsReadOnly(readOnly);
            setIsEditMode(false);
            setIsDirty(false);
            setSaveStatus(SaveStatus.SAVED);
            setStepProgress(
              response.stepProgress || {
                enter: false,
                collect: false,
                label: false,
                qa: false,
              },
            );
            setError(null);
            lastSavedDataRef.current = JSON.stringify({
              orderData: loadedOrderData,
              samples: response.samples,
            });
            resolve(response);
          } else {
            const errorMsg = "Order not found";
            setError(errorMsg);
            reject(new Error(errorMsg));
          }
        },
      );
    });
  }, []);

  /**
   * Convert samples array to XML format expected by backend
   */
  const buildSampleXML = useCallback((samplesArray) => {
    if (!samplesArray || samplesArray.length === 0) {
      return "";
    }

    // Check if any sample has tests
    const hasTests = samplesArray.some((s) => s.tests && s.tests.length > 0);
    if (!hasTests) {
      return "";
    }

    let sampleXmlString = '<?xml version="1.0" encoding="utf-8"?>';
    sampleXmlString += "<samples>";

    samplesArray.forEach((sampleItem) => {
      if (sampleItem.tests && sampleItem.tests.length > 0) {
        const tests = sampleItem.tests.map((t) => t.id).join(",");
        const panels =
          sampleItem.panels && sampleItem.panels.length > 0
            ? sampleItem.panels.map((p) => p.id).join(",")
            : "";

        // Get sample XML data or defaults
        const sampleXMLData = sampleItem.sampleXML || {};
        const collectionDate = sampleXMLData.collectionDate || "";
        const collectionTime = sampleXMLData.collectionTime || "";
        const collector = sampleXMLData.collector || "";
        const quantity = sampleXMLData.quantity || "";
        const uom = sampleXMLData.uom || "";
        const rejected = sampleItem.sampleRejected ? "true" : "false";
        const rejectReasonId = sampleItem.rejectionReason || "";

        // Storage location data
        const storageLocation = sampleXMLData.storageLocation || {};
        const storageLocationId = storageLocation.id || "";
        const storageLocationType = storageLocation.type || "";
        const storagePositionCoordinate =
          storageLocation.positionCoordinate || "";

        // GPS data
        const gpsLatitude = sampleXMLData.gpsLatitude || "";
        const gpsLongitude = sampleXMLData.gpsLongitude || "";
        const gpsAccuracy = sampleXMLData.gpsAccuracy || "";
        const gpsCaptureMethod = sampleXMLData.gpsCaptureMethod || "";

        // Include sampleItemId for updates - this identifies which existing sample_item to update
        const sampleItemId = sampleItem.sampleItemId || "";

        sampleXmlString += `<sample sampleID='${sampleItem.sampleTypeId}' sampleItemId='${sampleItemId}' date='${collectionDate}' time='${collectionTime}' collector='${collector}' quantity='${quantity}' uom='${uom}' tests='${tests}' testSectionMap='' testSampleTypeMap='' panels='${panels}' rejected='${rejected}' rejectReasonId='${rejectReasonId}' initialConditionIds='' storageLocationId='${storageLocationId}' storageLocationType='${storageLocationType}' storagePositionCoordinate='${storagePositionCoordinate}' gpsLatitude='${gpsLatitude}' gpsLongitude='${gpsLongitude}' gpsAccuracy='${gpsAccuracy}' gpsCaptureMethod='${gpsCaptureMethod}'/>`;
      }
    });

    sampleXmlString += "</samples>";
    return sampleXmlString;
  }, []);

  /**
   * Build referral items from samples
   */
  const buildReferralItems = useCallback((samplesArray) => {
    const referralItems = [];

    samplesArray.forEach((sampleItem) => {
      if (sampleItem.referralItems && sampleItem.referralItems.length > 0) {
        const tests = sampleItem.tests
          ? sampleItem.tests.map((t) => t.id).join(",")
          : "";
        const referredInstitutes = sampleItem.referralItems
          .map((r) => r.institute)
          .join(",");
        const sentDates = sampleItem.referralItems
          .map((r) => r.sentDate)
          .join(",");
        const referralReasonIds = sampleItem.referralItems
          .map((r) => r.reasonForReferral)
          .join(",");
        const referrers = sampleItem.referralItems
          .map((r) => r.referrer)
          .join(",");

        referralItems.push({
          referrer: referrers,
          referredInstituteId: referredInstitutes,
          referredTestId: tests,
          referredSendDate: sentDates,
          referralReasonId: referralReasonIds,
        });
      }
    });

    return referralItems;
  }, []);

  /**
   * Save the current order state.
   * Can be called at any step to persist progress.
   *
   * @param {boolean} silent - If true, no loading indicator is shown
   * @param {boolean} orderEntryOnly - If true, samples are not required (decoupled workflow)
   */
  const saveOrder = useCallback(
    async (silent = false, orderEntryOnly = false) => {
      if (isReadOnly && !isEditMode) {
        return Promise.reject(new Error("Cannot save in read-only mode"));
      }

      if (!silent) {
        setIsSubmitting(true);
      }
      setSaveStatus(SaveStatus.SAVING);
      setError(null);

      // Build sample XML and referral items
      const sampleXML = buildSampleXML(samples);
      const referralItems = buildReferralItems(samples);
      const useReferral = referralItems.length > 0;

      // Prepare order data for submission in the format expected by SamplePatientEntry
      const submitData = {
        ...orderData,
        sampleXML: sampleXML,
        referralItems: referralItems,
        useReferral: useReferral,
        // Flag for decoupled workflow: samples not required when orderEntryOnly=true
        orderEntryOnly: orderEntryOnly,
        // Clean up display lists that shouldn't be sent
        sampleOrderItems: {
          ...orderData.sampleOrderItems,
          priorityList: [],
          programList: [],
          referringSiteList: [],
          providersList: [],
          paymentOptions: [],
          testLocationCodeList: [],
        },
        initialSampleConditionList: [],
        testSectionList: [],
      };

      // Remove extra fields from sampleOrderItems that backend doesn't expect or that fail validation
      if (submitData.sampleOrderItems.questionnaire) {
        delete submitData.sampleOrderItems.questionnaire;
      }
      if (submitData.sampleOrderItems.vlProgramFields) {
        delete submitData.sampleOrderItems.vlProgramFields;
      }
      if (submitData.sampleOrderItems.paymentStatus) {
        delete submitData.sampleOrderItems.paymentStatus;
      }
      // Remove 'program' field - it contains the name (e.g., "Histopathology") but validation
      // expects a numeric ID. The backend uses 'programId' instead.
      if (submitData.sampleOrderItems.program) {
        delete submitData.sampleOrderItems.program;
      }

      return new Promise((resolve, reject) => {
        // Always use SamplePatientEntry endpoint - the backend handles both insert and update
        // based on whether sampleOrderItems.sampleId is present
        const endpoint = "/rest/SamplePatientEntry";

        // Include sampleId in the payload for updates
        if (orderId) {
          submitData.sampleOrderItems = {
            ...submitData.sampleOrderItems,
            sampleId: orderId,
          };
        }

        postToOpenElisServer(endpoint, JSON.stringify(submitData), (status) => {
          if (!silent) {
            setIsSubmitting(false);
          }

          if (status === 200 || status === 201) {
            setIsDirty(false);
            setSaveStatus(SaveStatus.SAVED);
            setError(null);
            lastSavedDataRef.current = JSON.stringify({
              orderData,
              samples,
            });
            resolve({ success: true });
          } else {
            setSaveStatus(SaveStatus.ERROR);
            const errorMsg = "Failed to save order";
            setError(errorMsg);
            reject(new Error(errorMsg));
          }
        });
      });
    },
    [
      orderId,
      orderData,
      samples,
      stepProgress,
      isReadOnly,
      isEditMode,
      buildSampleXML,
      buildReferralItems,
    ],
  );

  /**
   * Enable edit mode for a read-only order
   */
  const enableEditMode = useCallback(() => {
    setIsEditMode(true);
  }, []);

  /**
   * Mark a step as complete
   */
  const markStepComplete = useCallback((step) => {
    setStepProgress((prev) => ({
      ...prev,
      [step]: true,
    }));
  }, []);

  /**
   * Reset the order context to initial state.
   * Used when starting a new order.
   */
  const resetOrder = useCallback(() => {
    setOrderId(null);
    setLabNumber(null);
    setOrderDataState(getInitialOrderData());
    setSamplesState([sampleObject]);
    setIsReadOnly(false);
    setIsEditMode(false);
    setCurrentStep(0);
    setIsDirty(false);
    setSaveStatus(SaveStatus.SAVED);
    setError(null);
    setStepProgress({
      enter: false,
      collect: false,
      label: false,
      qa: false,
    });
    lastSavedDataRef.current = null;

    // Re-fetch form defaults from API to get correct date format
    getFromOpenElisServer("/rest/SamplePatientEntry", (response) => {
      if (response && response.currentDate) {
        setOrderDataState((prev) => ({
          ...prev,
          currentDate: response.currentDate,
          sampleOrderItems: {
            ...prev.sampleOrderItems,
            requestDate: response.currentDate,
            receivedDateForDisplay: response.currentDate,
            receivedTime: getCurrentTime(),
            paymentOptions: response.sampleOrderItems?.paymentOptions || [],
            paymentOptionSelection: "",
            referringSiteList:
              response.sampleOrderItems?.referringSiteList || [],
            providersList: response.sampleOrderItems?.providersList || [],
            testLocationCodeList:
              response.sampleOrderItems?.testLocationCodeList || [],
            priorityList: response.sampleOrderItems?.priorityList || [],
            programList: response.sampleOrderItems?.programList || [],
          },
          sampleTypes: response.sampleTypes || [],
          testSectionList: response.testSectionList || [],
          rejectReasonList: response.rejectReasonList || [],
          referralOrganizations: response.referralOrganizations || [],
          referralReasons: response.referralReasons || [],
        }));
      }
    });
  }, []);

  /**
   * Initialize form defaults from API on mount.
   * This ensures we get the correct date format from the server.
   */
  useEffect(() => {
    getFromOpenElisServer("/rest/SamplePatientEntry", (response) => {
      if (response && response.currentDate) {
        setOrderDataState((prev) => ({
          ...prev,
          currentDate: response.currentDate,
          sampleOrderItems: {
            ...prev.sampleOrderItems,
            requestDate: response.currentDate,
            receivedDateForDisplay: response.currentDate,
            receivedTime:
              prev.sampleOrderItems?.receivedTime || getCurrentTime(),
            // Use payment options from API if available
            paymentOptions: response.sampleOrderItems?.paymentOptions || [],
            // Keep paymentOptionSelection empty (not "free")
            paymentOptionSelection: "",
            // Copy other reference data from API
            referringSiteList:
              response.sampleOrderItems?.referringSiteList || [],
            providersList: response.sampleOrderItems?.providersList || [],
            testLocationCodeList:
              response.sampleOrderItems?.testLocationCodeList || [],
            priorityList: response.sampleOrderItems?.priorityList || [],
            programList: response.sampleOrderItems?.programList || [],
          },
          // Copy other lists from API response
          sampleTypes: response.sampleTypes || [],
          testSectionList: response.testSectionList || [],
          rejectReasonList: response.rejectReasonList || [],
          referralOrganizations: response.referralOrganizations || [],
          referralReasons: response.referralReasons || [],
        }));
      }
    });
  }, []);

  /**
   * Auto-save effect - saves every 30 seconds if form is dirty and has a lab number
   */
  useEffect(() => {
    const hasLabNumber = orderData?.sampleOrderItems?.labNo;
    if (isDirty && !isReadOnly && hasLabNumber) {
      autoSaveTimerRef.current = setInterval(() => {
        if (isDirty && !isSubmitting) {
          saveOrder(true).catch((err) => {
            console.error("Auto-save failed:", err);
          });
        }
      }, AUTO_SAVE_INTERVAL);
    }

    return () => {
      if (autoSaveTimerRef.current) {
        clearInterval(autoSaveTimerRef.current);
      }
    };
  }, [
    isDirty,
    isReadOnly,
    isSubmitting,
    saveOrder,
    orderData?.sampleOrderItems?.labNo,
  ]);

  /**
   * Browser navigation warning for unsaved changes
   */
  useEffect(() => {
    const handleBeforeUnload = (e) => {
      if (isDirty) {
        e.preventDefault();
        e.returnValue = "";
        return "";
      }
    };

    window.addEventListener("beforeunload", handleBeforeUnload);
    return () => {
      window.removeEventListener("beforeunload", handleBeforeUnload);
    };
  }, [isDirty]);

  const value = {
    // State
    orderId,
    labNumber,
    orderData,
    samples,
    isReadOnly,
    isEditMode,
    currentStep,
    isLoading,
    isSubmitting,
    saveStatus,
    isDirty,
    error,
    stepProgress,

    // Actions
    loadOrder,
    saveOrder,
    setCurrentStep,
    setOrderData,
    setSamples,
    resetOrder,
    enableEditMode,
    markStepComplete,
  };

  return (
    <OrderContext.Provider value={value}>{children}</OrderContext.Provider>
  );
};

/**
 * Custom hook for accessing the OrderContext.
 * Throws an error if used outside of OrderProvider.
 */
export const useOrderContext = () => {
  const context = useContext(OrderContext);
  if (!context) {
    throw new Error("useOrderContext must be used within an OrderProvider");
  }
  return context;
};

export default OrderContext;
