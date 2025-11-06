import * as Yup from "yup";

const ModifyOrderEntryValidationSchema = Yup.object().shape({
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

export default ModifyOrderEntryValidationSchema;
