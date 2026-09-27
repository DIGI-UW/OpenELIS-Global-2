import { createSampleOrderFormValues } from "../formModel/innitialValues/OrderEntryFormValues";
import { defaultMicrobiologyOrderDetail } from "../microbiology/MicrobiologyOrderDetailFields";

export const buildLoadedOrderData = (response, prior = {}) => {
  const defaults = createSampleOrderFormValues();
  // The server returns microbiology details only for an order it considers
  // microbiology, so their absence is meaningful and is preserved here.
  const loadedMicrobiologyOrderDetail = {
    ...(response.orderData?.microbiologyOrderDetail || {}),
    ...(response.microbiologyOrderDetail || {}),
  };
  let microbiologyOrderDetail;
  if (Object.keys(loadedMicrobiologyOrderDetail).length > 0) {
    microbiologyOrderDetail = {
      ...defaultMicrobiologyOrderDetail,
      ...loadedMicrobiologyOrderDetail,
      culturePurpose: Object.prototype.hasOwnProperty.call(
        loadedMicrobiologyOrderDetail,
        "culturePurpose",
      )
        ? loadedMicrobiologyOrderDetail.culturePurpose || ""
        : defaultMicrobiologyOrderDetail.culturePurpose,
    };
    delete microbiologyOrderDetail.criticalNotificationPreference;
  }

  return {
    ...defaults,
    sampleTypes: prior.sampleTypes,
    testSectionList: prior.testSectionList,
    rejectReasonList: prior.rejectReasonList,
    referralOrganizations: prior.referralOrganizations,
    referralReasons: prior.referralReasons,
    ...(response.orderData || {}),
    ...(microbiologyOrderDetail ? { microbiologyOrderDetail } : {}),
    patientProperties: {
      ...defaults.patientProperties,
      ...(response.patientProperties || {}),
      ...(response.orderData?.patientProperties || {}),
      patientUpdateStatus:
        response.patientProperties?.patientUpdateStatus || "NO_ACTION",
    },
    sampleOrderItems: {
      ...defaults.sampleOrderItems,
      ...(response.sampleOrderItems || {}),
      environmentalFields: {
        ...(prior.sampleOrderItems?.environmentalFields || {}),
        ...(response.sampleOrderItems?.environmentalFields || {}),
      },
      labNo: response.labNumber,
      microbiologyProgramId:
        response.sampleOrderItems?.programCode?.toUpperCase() === "MICROBIOLOGY"
          ? response.sampleOrderItems?.programId
          : undefined,
    },
  };
};

export const hasCultureWorkflowTest = (samples = []) =>
  samples.some((sample) =>
    (sample.tests || []).some((test) => test.cultureWorkflowType),
  );

/**
 * The single microbiology decision on the client: a selected culture-workflow
 * test, or an explicitly selected Microbiology program as the documented
 * fallback. Readiness and the submitted payload read this. Section visibility
 * also reads it, widened where a section has already resolved the program list.
 */
export const isMicrobiologyOrder = (orderData, samples = []) => {
  if (hasCultureWorkflowTest(samples)) {
    return true;
  }
  const sampleOrderItems = orderData?.sampleOrderItems || {};
  return (
    Boolean(sampleOrderItems.microbiologyProgramId) &&
    String(sampleOrderItems.programId || "") ===
      String(sampleOrderItems.microbiologyProgramId)
  );
};

/**
 * Microbiology details are submitted only for an order that qualifies, so a
 * routine order carries none even if the form still holds values.
 */
export const buildSubmittedMicrobiologyOrderDetail = (orderData, samples) =>
  isMicrobiologyOrder(orderData, samples)
    ? buildSubmissionMicrobiologyOrderDetail(orderData?.microbiologyOrderDetail)
    : undefined;

export const isMicrobiologyOrderReady = (orderData, samples) => {
  if (!isMicrobiologyOrder(orderData, samples)) {
    return true;
  }
  const sampleOrderItems = orderData?.sampleOrderItems || {};
  return (
    String(sampleOrderItems.programId || "") ===
    String(sampleOrderItems.microbiologyProgramId || "")
  );
};

export const buildSubmissionMicrobiologyOrderDetail = (detail = {}) => {
  const submission = { ...detail };
  delete submission.criticalNotificationPreference;
  submission.admissionDate =
    detail.patientOrigin === "OUTPATIENT" || !detail.admissionDate
      ? null
      : detail.admissionDate;
  return submission;
};

export const buildSubmissionSampleOrderItems = (sampleOrderItems = {}) => {
  const serializableItems = { ...sampleOrderItems };
  [
    "questionnaire",
    "vlProgramFields",
    "paymentStatus",
    "program",
    "programCode",
    "microbiologyProgramId",
    "microbiologyPreviousProgramId",
    "domain",
  ].forEach((field) => delete serializableItems[field]);

  return {
    ...serializableItems,
    priorityList: [],
    programList: [],
    referringSiteList: [],
    providersList: [],
    paymentOptions: [],
    testLocationCodeList: [],
  };
};
