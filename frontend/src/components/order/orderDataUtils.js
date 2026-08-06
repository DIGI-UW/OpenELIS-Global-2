import { SampleOrderFormValues } from "../formModel/innitialValues/OrderEntryFormValues";

export const buildLoadedOrderData = (response, prior = {}) => ({
  ...SampleOrderFormValues,
  sampleTypes: prior.sampleTypes,
  testSectionList: prior.testSectionList,
  rejectReasonList: prior.rejectReasonList,
  referralOrganizations: prior.referralOrganizations,
  referralReasons: prior.referralReasons,
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
    environmentalFields: {
      ...(prior.sampleOrderItems?.environmentalFields || {}),
      ...(response.sampleOrderItems?.environmentalFields || {}),
    },
    labNo: response.labNumber,
  },
});
