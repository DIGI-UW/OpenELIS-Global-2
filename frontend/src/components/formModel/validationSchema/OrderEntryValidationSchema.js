import * as Yup from "yup";
import { createPatientValidationSchema } from "./CreatePatientValidationShema";

// domain is optional: E/V skip patient validation; clinical orders validate
// patients using configurationProperties. Requester first/last name are
// required only when the deployment turns REQUESTER_REQUIRED on (#4003).
export const createOrderEntryValidationSchema = (
  configurationProperties = {},
  domain,
) => {
  const isNonClinical = domain === "E" || domain === "V";
  const requesterRequired =
    configurationProperties.REQUESTER_REQUIRED === "true";
  // A blind PT sample has no referring site and no requester — demanding the
  // clinical ceremony on an EQA order blocks a save that is otherwise complete.
  const requiredUnlessEQA = (message) =>
    Yup.string().when("isEQASample", {
      is: true,
      otherwise: (schema) => schema.required(message),
    });
  const providerFirstNameSchema = requesterRequired
    ? requiredUnlessEQA("Requester First Name is required")
    : Yup.string();
  const providerLastNameSchema = requesterRequired
    ? requiredUnlessEQA("Requester Last Name is required")
    : Yup.string();

  const sampleOrderItemsSchema = Yup.object()
    .shape({
      labNo: Yup.string().required("Sample Lab Number is required"),
      referringSiteName: Yup.string(),
      referringSiteId: Yup.string(),
      providerLastName: providerLastNameSchema,
      providerFirstName: providerFirstNameSchema,
      providerEmail: Yup.string().email("Invalid Email"),
    })
    .test("referringSiteName", "Referring Site is required", function (value) {
      const { referringSiteName, referringSiteId, isEQASample } = value || {};
      return !!isEQASample || !!referringSiteName || !!referringSiteId;
    });

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
