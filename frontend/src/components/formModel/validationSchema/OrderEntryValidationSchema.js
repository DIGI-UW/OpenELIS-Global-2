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
      providerLastName: Yup.string().required(
        "Requester Last Name is required",
      ),
      providerFirstName: Yup.string().required(
        "Requester First Name is required",
      ),
      providerEmail: Yup.string().email("Invalid Email"),
    })
    .test("referringSiteName", "Referring Site is required", function (value) {
      const { referringSiteName, referringSiteId } = value || {};
      return !!referringSiteName || !!referringSiteId;
    }),
});

export default OrderEntryValidationSchema;
