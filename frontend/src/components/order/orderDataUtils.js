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
