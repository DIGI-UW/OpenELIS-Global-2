import React, {
  createContext,
  useState,
  useCallback,
  useContext,
  useEffect,
  useRef,
} from "react";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  putToOpenElisServer,
} from "../utils/Utils";
import {
  createRequestsForSamples,
  getRequestsBySample,
  convertRequestsToSamples,
} from "./api/sampleTypeRequestApi";
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

  // Test-to-sample assignments (Step 2: Collect)
  testSampleAssignments: {},

  // Actions
  loadOrder: () => {},
  saveOrder: () => {},
  setCurrentStep: () => {},
  setOrderData: () => {},
  setSamples: () => {},
  resetOrder: () => {},
  enableEditMode: () => {},
  markStepComplete: () => {},
  // Test assignment actions (Step 2)
  assignTestToSample: () => {},
  removeTestFromSample: () => {},
  updateSampleCollectionDetails: () => {},
});

export const sampleObject = {
  index: 0,
  sampleItemId: "",
  sampleRejected: false,
  rejectionReason: "",
  sampleTypeId: "",
  sampleTypeName: "",
  sampleXML: null,
  panels: [],
  tests: [],
  requestReferralEnabled: false,
  referralItems: [],
  quantity: "",
  quantityUnit: "",
  collectionConditions: "",
  collectionDate: "",
  collectionTime: "",
  collectorId: "",
  receivedDate: "",
  receivedTime: "",
  receivedBy: "",
  hasNCE: false,
  nceId: "",
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
 * Convert ISO date (YYYY-MM-DD) to backend format (MM/dd/yyyy)
 */
const convertIsoToBackendDate = (isoDate) => {
  if (!isoDate) return "";
  // Check if already in MM/dd/yyyy format
  if (isoDate.includes("/")) return isoDate;
  // Convert from YYYY-MM-DD to MM/dd/yyyy
  const parts = isoDate.split("-");
  if (parts.length === 3) {
    return `${parts[1]}/${parts[2]}/${parts[0]}`;
  }
  return isoDate;
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

  // Storage assignment skipped flag (Label step)
  // Persisted to backend via /rest/order/storage-skipped endpoint
  const [storageSkipped, setStorageSkippedState] = useState(false);

  // Wrapper for setStorageSkipped - persists to backend
  const setStorageSkipped = useCallback(
    (value) => {
      setStorageSkippedState(value);

      if (labNumber) {
        const endpoint = `/rest/order/storage-skipped?labNumber=${encodeURIComponent(labNumber)}&storageSkipped=${value}`;
        putToOpenElisServer(endpoint, null, Function.prototype);
      }
    },
    [labNumber],
  );

  // Test-to-sample assignments for Step 2
  // Structure: { [testId]: { testId, testName, isPanel, assignedToSamples: [sampleIndex, ...] } }
  const [testSampleAssignments, setTestSampleAssignments] = useState({});

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
                // Keep patient status from response or default to NO_ACTION for subsequent saves
                // Only set UPDATE when patient data has actually been modified
                patientUpdateStatus:
                  response.patientProperties?.patientUpdateStatus ||
                  "NO_ACTION",
              },
              sampleOrderItems: {
                ...SampleOrderFormValues.sampleOrderItems,
                ...(response.sampleOrderItems || {}),
                labNo: response.labNumber,
              },
            };

            setOrderDataState(loadedOrderData);

            // Load sample type requests if no sample_items exist (decoupled workflow)
            // This handles Step 1 edit where samples are stored as requests, not items
            const hasSampleItems =
              response.samples &&
              response.samples.length > 0 &&
              response.samples.some((s) => s.sampleItemId);

            if (!hasSampleItems && response.id) {
              // Try to load sample type requests
              getRequestsBySample(response.id)
                .then((requests) => {
                  if (requests && requests.length > 0) {
                    const samplesFromRequests =
                      convertRequestsToSamples(requests);
                    setSamplesState(samplesFromRequests);
                  } else {
                    setSamplesState(response.samples || [sampleObject]);
                  }
                })
                .catch(() => {
                  setSamplesState(response.samples || [sampleObject]);
                });
            } else {
              setSamplesState(response.samples || [sampleObject]);
            }

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

            // Load storageSkipped from backend response
            const savedStorageSkipped = response.storageSkipped === true;
            setStorageSkippedState(savedStorageSkipped);

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
   * @param samplesArray - Array of sample objects
   * @param envFields - Optional environmentalFields from orderData (for GPS fallback)
   */
  const buildSampleXML = useCallback((samplesArray, envFields = {}) => {
    if (!samplesArray || samplesArray.length === 0) {
      return "";
    }

    // Check if any sample has a sample type (required for saving)
    const hasSampleType = samplesArray.some((s) => s.sampleTypeId);
    if (!hasSampleType) {
      return "";
    }

    let sampleXmlString = '<?xml version="1.0" encoding="utf-8"?>';
    sampleXmlString += "<samples>";

    samplesArray.forEach((sampleItem) => {
      // Include sample if it has a sample type (tests are optional for collection step)
      if (sampleItem.sampleTypeId) {
        const tests =
          sampleItem.tests && sampleItem.tests.length > 0
            ? sampleItem.tests.map((t) => t.id).join(",")
            : "";
        const panels =
          sampleItem.panels && sampleItem.panels.length > 0
            ? sampleItem.panels.map((p) => p.id).join(",")
            : "";

        // Get collection data - check both top-level fields (new) and sampleXML (legacy)
        const sampleXMLData = sampleItem.sampleXML || {};
        // Convert ISO date (YYYY-MM-DD) to backend format (MM/dd/yyyy)
        const collectionDate = convertIsoToBackendDate(
          sampleItem.collectionDate || sampleXMLData.collectionDate || "",
        );
        const collectionTime =
          sampleItem.collectionTime || sampleXMLData.collectionTime || "";
        const collector =
          sampleItem.collectorId || sampleXMLData.collector || "";
        const collectionConditions =
          sampleItem.collectionConditions ||
          sampleXMLData.collectionConditions ||
          "";
        const quantity = sampleItem.quantity || sampleXMLData.quantity || "";
        const uom = sampleItem.quantityUnit || sampleXMLData.uom || "";
        const rejected = sampleItem.sampleRejected ? "true" : "false";
        const rejectReasonId = sampleItem.rejectionReason || "";

        const receivedDate = convertIsoToBackendDate(
          sampleItem.receivedDate || sampleXMLData.receivedDate || "",
        );
        const receivedTime =
          sampleItem.receivedTime || sampleXMLData.receivedTime || "";

        // Storage location data - check both top-level sample properties (from OrderLabel)
        // and nested sampleXML.storageLocation (legacy format)
        const storageLocation = sampleXMLData.storageLocation || {};
        const storageLocationId =
          sampleItem.storageLocationId || storageLocation.id || "";
        const storageLocationType =
          sampleItem.storageLocationType || storageLocation.type || "";
        const storagePositionCoordinate =
          sampleItem.storagePositionCoordinate ||
          storageLocation.positionCoordinate ||
          "";

        // GPS data - fallback to environmentalFields for environmental workflow
        const gpsLatitude =
          sampleXMLData.gpsLatitude || envFields.gpsLatitude || "";
        const gpsLongitude =
          sampleXMLData.gpsLongitude || envFields.gpsLongitude || "";
        const gpsAccuracy = sampleXMLData.gpsAccuracy || "";
        const gpsCaptureMethod = sampleXMLData.gpsCaptureMethod || "";

        // Include sampleItemId for updates - this identifies which existing sample_item to update
        const sampleItemId = sampleItem.sampleItemId || "";

        sampleXmlString += `<sample sampleID='${sampleItem.sampleTypeId}' sampleItemId='${sampleItemId}' date='${collectionDate}' time='${collectionTime}' collector='${collector}' collectionConditions='${collectionConditions}' quantity='${quantity}' uom='${uom}' receivedDate='${receivedDate}' receivedTime='${receivedTime}' tests='${tests}' testSectionMap='' testSampleTypeMap='' panels='${panels}' rejected='${rejected}' rejectReasonId='${rejectReasonId}' initialConditionIds='' storageLocationId='${storageLocationId}' storageLocationType='${storageLocationType}' storagePositionCoordinate='${storagePositionCoordinate}' gpsLatitude='${gpsLatitude}' gpsLongitude='${gpsLongitude}' gpsAccuracy='${gpsAccuracy}' gpsCaptureMethod='${gpsCaptureMethod}'/>`;
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
      // Pass environmentalFields for GPS fallback in environmental workflow
      const envFields = orderData?.sampleOrderItems?.environmentalFields || {};
      const sampleXML = buildSampleXML(samples, envFields);
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

            // Reload order to get created sampleItemIds and orderId for subsequent saves
            const labNo = orderData?.sampleOrderItems?.labNo;
            if (labNo) {
              getFromOpenElisServer(
                `/rest/order/search?labNumber=${encodeURIComponent(labNo)}`,
                (response) => {
                  if (response) {
                    // CRITICAL: Update orderId from the response - needed for Step 2 to work correctly
                    // The orderId is used to set sampleOrderItems.sampleId which tells the backend
                    // this is an UPDATE (not insert), so it loads the existing sample and skips
                    // accession number validation
                    if (response.id) {
                      setOrderId(response.id);
                    }
                    if (response.samples) {
                      setSamplesState(response.samples);
                    }
                    // CRITICAL: Update patientUpdateStatus to NO_ACTION after first save
                    // This prevents "stale state" errors when saving again (patient already exists)
                    setOrderDataState((prev) => ({
                      ...prev,
                      patientProperties: {
                        ...prev.patientProperties,
                        patientUpdateStatus: "NO_ACTION",
                        // Also update patientPK if available
                        patientPK:
                          response.patientProperties?.patientPK ||
                          prev.patientProperties?.patientPK,
                      },
                    }));
                  }
                  resolve({ success: true });
                },
              );
            } else {
              resolve({ success: true });
            }
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
   * Save order entry only (Step 1) - creates order metadata and sample_type_requests,
   * but NOT sample_item records. Sample items are created in Step 2 (Collect Sample).
   *
   * This enables the decoupled workflow where:
   * - Step 1: Order metadata + requested sample types
   * - Step 2: Physical sample collection (creates sample_item records)
   *
   * @param {boolean} silent - If true, no loading indicator is shown
   */
  const saveOrderEntry = useCallback(
    async (silent = false) => {
      if (isReadOnly && !isEditMode) {
        return Promise.reject(new Error("Cannot save in read-only mode"));
      }

      if (!silent) {
        setIsSubmitting(true);
      }
      setSaveStatus(SaveStatus.SAVING);
      setError(null);

      // For Step 1, we send empty sampleXML - sample types will be saved as requests
      const envFields = orderData?.sampleOrderItems?.environmentalFields || {};

      // Prepare order data WITHOUT sample items
      const submitData = {
        ...orderData,
        sampleXML: "", // Empty - no sample_item records created
        referralItems: [],
        useReferral: false,
        orderEntryOnly: true, // Flag for backend to skip sample validation
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

      // Remove extra fields that fail validation
      if (submitData.sampleOrderItems.questionnaire) {
        delete submitData.sampleOrderItems.questionnaire;
      }
      if (submitData.sampleOrderItems.vlProgramFields) {
        delete submitData.sampleOrderItems.vlProgramFields;
      }
      if (submitData.sampleOrderItems.paymentStatus) {
        delete submitData.sampleOrderItems.paymentStatus;
      }
      if (submitData.sampleOrderItems.program) {
        delete submitData.sampleOrderItems.program;
      }

      return new Promise((resolve, reject) => {
        const endpoint = "/rest/SamplePatientEntry";

        if (orderId) {
          submitData.sampleOrderItems = {
            ...submitData.sampleOrderItems,
            sampleId: orderId,
          };
        }

        postToOpenElisServer(
          endpoint,
          JSON.stringify(submitData),
          async (status) => {
            if (status === 200 || status === 201) {
              // Reload order to get the created sample ID
              const labNo = orderData?.sampleOrderItems?.labNo;
              if (labNo) {
                getFromOpenElisServer(
                  `/rest/order/search?labNumber=${encodeURIComponent(labNo)}`,
                  async (response) => {
                    if (response) {
                      const sampleId = response.id;
                      setOrderId(sampleId);

                      // Create sample_type_requests for each selected sample type
                      const samplesWithTypes = samples.filter(
                        (s) => s.sampleTypeId,
                      );
                      if (samplesWithTypes.length > 0 && sampleId) {
                        try {
                          await createRequestsForSamples(
                            sampleId,
                            samplesWithTypes,
                          );
                        } catch (err) {
                          // Reject with error so UI shows failure
                          if (!silent) {
                            setIsSubmitting(false);
                          }
                          setSaveStatus(SaveStatus.ERROR);
                          setError("Failed to save sample type requests");
                          reject(
                            new Error(
                              "Failed to save sample type requests: " +
                                err.message,
                            ),
                          );
                          return;
                        }
                      }

                      // Update state
                      setIsDirty(false);
                      setSaveStatus(SaveStatus.SAVED);
                      setOrderDataState((prev) => ({
                        ...prev,
                        patientProperties: {
                          ...prev.patientProperties,
                          patientUpdateStatus: "NO_ACTION",
                          patientPK:
                            response.patientProperties?.patientPK ||
                            prev.patientProperties?.patientPK,
                        },
                      }));

                      if (!silent) {
                        setIsSubmitting(false);
                      }
                      resolve({ success: true, sampleId });
                    } else {
                      if (!silent) {
                        setIsSubmitting(false);
                      }
                      resolve({ success: true });
                    }
                  },
                );
              } else {
                if (!silent) {
                  setIsSubmitting(false);
                }
                setIsDirty(false);
                setSaveStatus(SaveStatus.SAVED);
                resolve({ success: true });
              }
            } else {
              if (!silent) {
                setIsSubmitting(false);
              }
              setSaveStatus(SaveStatus.ERROR);
              const errorMsg = "Failed to save order";
              setError(errorMsg);
              reject(new Error(errorMsg));
            }
          },
        );
      });
    },
    [orderId, orderData, samples, isReadOnly, isEditMode],
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
   * Assign a test to a sample (Step 2: Collect)
   * @param {string} testId - The test ID to assign
   * @param {string} testName - The test name
   * @param {boolean} isPanel - Whether this is a panel
   * @param {number} sampleIndex - The sample index to assign to
   */
  const assignTestToSample = useCallback(
    (testId, testName, isPanel, sampleIndex) => {
      // Update test assignments
      setTestSampleAssignments((prev) => {
        const existing = prev[testId] || {
          testId,
          testName,
          isPanel,
          assignedToSamples: [],
        };
        const assignedToSamples = existing.assignedToSamples.includes(
          sampleIndex,
        )
          ? existing.assignedToSamples
          : [...existing.assignedToSamples, sampleIndex];
        return {
          ...prev,
          [testId]: { ...existing, assignedToSamples },
        };
      });

      // Also add the test to the sample's tests array
      setSamplesState((prevSamples) => {
        const updated = [...prevSamples];
        const sample = updated[sampleIndex];
        if (sample) {
          const existingTests = sample.tests || [];
          if (!existingTests.some((t) => t.id === testId)) {
            updated[sampleIndex] = {
              ...sample,
              tests: [...existingTests, { id: testId, name: testName }],
            };
          }
        }
        return updated;
      });

      setIsDirty(true);
      setSaveStatus(SaveStatus.UNSAVED);
    },
    [],
  );

  /**
   * Remove a test from a sample (Step 2: Collect)
   * @param {string} testId - The test ID to remove
   * @param {number} sampleIndex - The sample index to remove from
   */
  const removeTestFromSample = useCallback((testId, sampleIndex) => {
    // Update test assignments
    setTestSampleAssignments((prev) => {
      const existing = prev[testId];
      if (!existing) return prev;
      const assignedToSamples = existing.assignedToSamples.filter(
        (idx) => idx !== sampleIndex,
      );
      if (assignedToSamples.length === 0) {
        const { [testId]: removed, ...rest } = prev;
        return rest;
      }
      return {
        ...prev,
        [testId]: { ...existing, assignedToSamples },
      };
    });

    // Also remove the test from the sample's tests array
    setSamplesState((prevSamples) => {
      const updated = [...prevSamples];
      const sample = updated[sampleIndex];
      if (sample && sample.tests) {
        updated[sampleIndex] = {
          ...sample,
          tests: sample.tests.filter((t) => t.id !== testId),
        };
      }
      return updated;
    });

    setIsDirty(true);
    setSaveStatus(SaveStatus.UNSAVED);
  }, []);

  /**
   * Update collection details for a sample (Step 2: Collect)
   * @param {number} sampleIndex - The sample index to update
   * @param {object} details - The collection details to update
   */
  const updateSampleCollectionDetails = useCallback((sampleIndex, details) => {
    setSamplesState((prevSamples) => {
      const updated = [...prevSamples];
      if (updated[sampleIndex]) {
        updated[sampleIndex] = {
          ...updated[sampleIndex],
          ...details,
        };
      }
      return updated;
    });

    setIsDirty(true);
    setSaveStatus(SaveStatus.UNSAVED);
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
    setStorageSkipped(false);
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
   * Auto-save effect - saves every 30 seconds if form is dirty and has minimum required data.
   * A lab number alone is not sufficient — patient (clinical) or site (environmental) plus
   * at least one sample type must be present before we persist.
   */
  useEffect(() => {
    const hasLabNumber = orderData?.sampleOrderItems?.labNo;
    const envFields = orderData?.sampleOrderItems?.environmentalFields || {};
    const workflowType = envFields.workflowType || "clinical";
    const hasPatientOrSite =
      workflowType === "environmental"
        ? !!(envFields.samplingSiteId || envFields.samplingSiteName)
        : !!(
            orderData?.patientProperties?.lastName ||
            orderData?.patientProperties?.nationalId
          );
    const hasSampleTypes = samples.some((s) => s.sampleTypeId);
    const canAutoSave = hasLabNumber && hasPatientOrSite && hasSampleTypes;

    if (isDirty && !isReadOnly && canAutoSave) {
      autoSaveTimerRef.current = setInterval(() => {
        if (isDirty && !isSubmitting) {
          saveOrder(true).catch(() => {});
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
    orderData?.sampleOrderItems?.environmentalFields?.workflowType,
    orderData?.sampleOrderItems?.environmentalFields?.samplingSiteId,
    orderData?.sampleOrderItems?.environmentalFields?.samplingSiteName,
    orderData?.patientProperties?.lastName,
    orderData?.patientProperties?.nationalId,
    samples,
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
    storageSkipped,
    testSampleAssignments,

    // Actions
    loadOrder,
    saveOrder,
    saveOrderEntry, // Step 1: saves order + creates sample_type_requests (no sample_items)
    setCurrentStep,
    setOrderData,
    setSamples,
    resetOrder,
    enableEditMode,
    markStepComplete,
    setStorageSkipped,
    // Test assignment actions (Step 2)
    assignTestToSample,
    removeTestFromSample,
    updateSampleCollectionDetails,
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
