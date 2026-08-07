import * as Yup from "yup";
import { createPatientValidationSchema } from "./CreatePatientValidationShema";

export const createOrderEntryValidationSchema = (
  configurationProperties = {},
) => {
  const requesterRequired =
    configurationProperties.REQUESTER_REQUIRED === "true";
  const providerFirstNameSchema = requesterRequired
    ? Yup.string().required("Requester First Name is required")
    : Yup.string();
  const providerLastNameSchema = requesterRequired
    ? Yup.string().required("Requester Last Name is required")
    : Yup.string();

  return Yup.object().shape({
    sampleXML: Yup.string().required("Sample is required"),
    patientProperties: createPatientValidationSchema(configurationProperties),
    sampleOrderItems: Yup.object()
      .shape({
        labNo: Yup.string().required("Sample Lab Number is required"),
        referringSiteName: Yup.string(),
        referringSiteId: Yup.string(),
        providerLastName: providerLastNameSchema,
        providerFirstName: providerFirstNameSchema,
        providerEmail: Yup.string().email("Invalid Email"),
      })
      .test(
        "referringSiteName",
        "Referring Site is required",
        function (value) {
          const { referringSiteName, referringSiteId } = value || {};
          return !!referringSiteName || !!referringSiteId;
        },
      ),
  });
};
