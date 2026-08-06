import { SampleOrderFormValues } from "../formModel/innitialValues/OrderEntryFormValues";

export const buildLoadedOrderData = (response) => ({
  ...SampleOrderFormValues,
  ...(response.orderData || {}),
  microbiologyOrderDetail: {
    ...SampleOrderFormValues.microbiologyOrderDetail,
    ...(response.orderData?.microbiologyOrderDetail || {}),
    ...(response.microbiologyOrderDetail || {}),
  },
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
    labNo: response.labNumber,
  },
});

export const buildSubmissionSampleOrderItems = (sampleOrderItems = {}) => {
  const serializableItems = { ...sampleOrderItems };
  [
    "questionnaire",
    "vlProgramFields",
    "paymentStatus",
    "program",
    "microbiologyProgramId",
    "microbiologyPreviousProgramId",
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
