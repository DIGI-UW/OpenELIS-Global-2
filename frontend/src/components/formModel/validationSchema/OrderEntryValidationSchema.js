import * as Yup from "yup";
import { createPatientValidationSchema } from "./CreatePatientValidationShema";

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

// domain (E/V) skips patient validation entirely; clinical orders still honour
// the configuration-driven patient schema from demo-silnas.
export const createOrderEntryValidationSchema = (
  domain,
  configurationProperties = {},
) => {
  const isNonClinical = domain === "E" || domain === "V";

  const shape = {
    sampleXML: Yup.string().required("Sample is required"),
    sampleOrderItems: sampleOrderItemsSchema,
  };

  if (!isNonClinical) {
    shape.patientProperties = createPatientValidationSchema(
      configurationProperties,
    );
  }

  return Yup.object().shape(shape);
};

// Backward-compatible default export (clinical — includes patient validation)
const OrderEntryValidationSchema = createOrderEntryValidationSchema();

export default OrderEntryValidationSchema;
