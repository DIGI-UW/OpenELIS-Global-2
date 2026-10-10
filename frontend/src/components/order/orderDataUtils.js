import { createSampleOrderFormValues } from "../formModel/innitialValues/OrderEntryFormValues";

export const buildLoadedOrderData = (response, prior = {}) => {
  const defaults = createSampleOrderFormValues();
  return {
    ...defaults,
    sampleTypes: prior.sampleTypes,
    testSectionList: prior.testSectionList,
    rejectReasonList: prior.rejectReasonList,
    referralOrganizations: prior.referralOrganizations,
    referralReasons: prior.referralReasons,
    ...(response.orderData || {}),
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
    },
  };
};

export const buildSubmissionSampleOrderItems = (sampleOrderItems = {}) => {
  const serializableItems = { ...sampleOrderItems };
  [
    "questionnaire",
    "vlProgramFields",
    "paymentStatus",
    "program",
    "programCode",
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
