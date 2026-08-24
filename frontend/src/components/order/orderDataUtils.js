import { SampleOrderFormValues } from "../formModel/innitialValues/OrderEntryFormValues";

export const buildLoadedOrderData = (response, prior = {}) => {
  const loadedMicrobiologyOrderDetail = {
    ...(response.orderData?.microbiologyOrderDetail || {}),
    ...(response.microbiologyOrderDetail || {}),
  };
  const microbiologyOrderDetail = {
    ...SampleOrderFormValues.microbiologyOrderDetail,
    ...loadedMicrobiologyOrderDetail,
    culturePurpose: Object.prototype.hasOwnProperty.call(
      loadedMicrobiologyOrderDetail,
      "culturePurpose",
    )
      ? loadedMicrobiologyOrderDetail.culturePurpose || ""
      : SampleOrderFormValues.microbiologyOrderDetail.culturePurpose,
  };
  delete microbiologyOrderDetail.criticalNotificationPreference;

  return {
    ...SampleOrderFormValues,
    sampleTypes: prior.sampleTypes,
    testSectionList: prior.testSectionList,
    rejectReasonList: prior.rejectReasonList,
    referralOrganizations: prior.referralOrganizations,
    referralReasons: prior.referralReasons,
    ...(response.orderData || {}),
    microbiologyOrderDetail,
    patientProperties: {
      ...SampleOrderFormValues.patientProperties,
      ...(response.patientProperties || {}),
      ...(response.orderData?.patientProperties || {}),
      patientUpdateStatus:
        response.patientProperties?.patientUpdateStatus || "NO_ACTION",
    },
    sampleOrderItems: {
      ...SampleOrderFormValues.sampleOrderItems,
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

export const isMicrobiologyOrderReady = (orderData, samples) => {
  const hasCultureWorkflow = samples.some((sample) =>
    (sample.tests || []).some((test) => test.cultureWorkflowType),
  );
  const sampleOrderItems = orderData?.sampleOrderItems || {};
  const microbiologyProgramSelected =
    Boolean(sampleOrderItems.microbiologyProgramId) &&
    String(sampleOrderItems.programId || "") ===
      String(sampleOrderItems.microbiologyProgramId);

  if (!hasCultureWorkflow && !microbiologyProgramSelected) {
    return true;
  }

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
