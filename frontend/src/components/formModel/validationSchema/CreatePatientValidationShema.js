import * as Yup from "yup";

export const createPatientValidationSchema = (configurationProperties = {}) => {
  const nationalIdValidator =
    configurationProperties.PATIENT_NATIONAL_ID_REQUIRED === "false"
      ? Yup.string()
      : Yup.string().required("National ID Required");

  return Yup.object().shape({
    nationalId: nationalIdValidator,
    birthDateForDisplay: Yup.string()
      .required("Patient Birth date Required")
      .test("valid-date", "Invalid date format", function (value) {
        const dateFormat = /^\d{2}\/\d{2}\/\d{4}$/;
        if (!value || !value.match(dateFormat)) {
          return false;
        }
        const [day, month, year] = value.split("/");
        const date = new Date(`${year}-${month}-${day}`);
        const date2 = new Date(`${year}-${day}-${month}`);

        const validDate1 = date instanceof Date && !isNaN(date);
        const validDate2 = date2 instanceof Date && !isNaN(date2);

        return validDate1 || validDate2;
      }),
    email: Yup.string().email("Patient Email Must Be Valid"),
    patientContact: Yup.object().shape({
      person: Yup.object().shape({
        email: Yup.string().email("Contact Email Must Be Valid"),
      }),
    }),
    gender: Yup.string().required("Gender is Required"),
  });
};
