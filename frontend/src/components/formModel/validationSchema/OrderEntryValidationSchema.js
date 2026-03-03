import * as Yup from "yup";
import CreatePatientValidationSchema from "./CreatePatientValidationShema";

const OrderEntryValidationSchema = Yup.object().shape({
  sampleXML: Yup.string().required("Sample is required"),
  patientProperties: CreatePatientValidationSchema,
  sampleOrderItems: Yup.object()
    .shape({
      labNo: Yup.string().required("Sample Lab Number is required"),
      referringSiteName: Yup.string(),
      referringSiteId: Yup.string(),
      providerLastName: Yup.string()
        .required("Requester Last Name is required")
        .test(
          "name-letters-only-last",
          "Enter only letters. Name should not contain numbers.",
          (value) => !value || /^[A-Za-z\s]+$/.test(value),
        ),
      providerFirstName: Yup.string()
        .required("Requester First Name is required")
        .test(
          "name-letters-only-first",
          "Enter only letters. Name should not contain numbers.",
          (value) => !value || /^[A-Za-z\s]+$/.test(value),
        ),
      providerEmail: Yup.string().email("Invalid Email"),
      providerWorkPhone: Yup.string()
        .test(
          "phone-digits-only",
          "Phone number must contain digits only.",
          (value) => !value || /^\d+$/.test(value),
        )
        .test(
          "phone-length-10",
          "Phone number must be exactly 10 digits.",
          (value) => !value || value.length === 10,
        ),
    })
    .test("referringSiteName", "Referring Site is required", function (value) {
      const { referringSiteName, referringSiteId } = value || {};
      return !!referringSiteName || !!referringSiteId;
    }),
});

export default OrderEntryValidationSchema;
