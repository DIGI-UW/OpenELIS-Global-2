import * as Yup from "yup";

// Requester first/last name follow the same REQUESTER_REQUIRED switch as order
// entry (#4003): AddOrder marks the fields with * only when it is on, and an
// order saved without a requester must stay editable.
const createModifyOrderEntryValidationSchema = (
  configurationProperties = {},
) => {
  const requesterRequired =
    configurationProperties.REQUESTER_REQUIRED === "true";
  return Yup.object().shape({
    sampleOrderItems: Yup.object()
      .shape({
        labNo: Yup.string().required("Sample Lab Number is required"),
        referringSiteName: Yup.string(),
        referringSiteId: Yup.string(),
        providerLastName: requesterRequired
          ? Yup.string().required("Requester Last Name is required")
          : Yup.string(),
        providerFirstName: requesterRequired
          ? Yup.string().required("Requester First Name is required")
          : Yup.string(),
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

export default createModifyOrderEntryValidationSchema;
