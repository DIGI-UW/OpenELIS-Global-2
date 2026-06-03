import * as Yup from "yup";
import CreatePatientValidationSchema from "./CreatePatientValidationShema";

const sampleOrderItemsSchema = Yup.object()
  .shape({
    labNo: Yup.string().required("Sample Lab Number is required"),
    referringSiteName: Yup.string(),
    referringSiteId: Yup.string(),
    providerLastName: Yup.string().required("Requester Last Name is required"),
    providerFirstName: Yup.string().required(
      "Requester First Name is required",
    ),
    providerEmail: Yup.string().email("Invalid Email"),
  })
  .test("referringSiteName", "Referring Site is required", function (value) {
    const { referringSiteName, referringSiteId } = value || {};
    return !!referringSiteName || !!referringSiteId;
  });

export const createOrderEntryValidationSchema = (domain) => {
  const isNonClinical = domain === "E" || domain === "V";

  const shape = {
    sampleXML: Yup.string().required("Sample is required"),
    sampleOrderItems: sampleOrderItemsSchema,
  };

  if (!isNonClinical) {
    shape.patientProperties = CreatePatientValidationSchema;
  }

  return Yup.object().shape(shape);
};

// Backward-compatible default export (clinical — includes patient validation)
const OrderEntryValidationSchema = createOrderEntryValidationSchema();

export default OrderEntryValidationSchema;
