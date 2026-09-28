import * as Yup from "yup";

const isValidDisplayDate = (value) => {
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
};

export const createPatientValidationSchema = (configurationProperties = {}) => {
  const nationalIdValidator =
    configurationProperties.PATIENT_NATIONAL_ID_REQUIRED === "false"
      ? Yup.string()
      : Yup.string().required("National ID Required");
  const sexRequired = configurationProperties.PATIENT_SEX_REQUIRED !== "false";
  const ageRequired = configurationProperties.PATIENT_AGE_REQUIRED !== "false";

  return Yup.object().shape({
    nationalId: nationalIdValidator,
    birthDateForDisplay: ageRequired
      ? Yup.string()
          .required("Patient Birth date Required")
          .test("valid-date", "Invalid date format", isValidDisplayDate)
      : Yup.string().test(
          "valid-date",
          "Invalid date format",
          (value) => !value || isValidDisplayDate(value),
        ),
    email: Yup.string().email("Patient Email Must Be Valid"),
    patientContact: Yup.object().shape({
      person: Yup.object().shape({
        email: Yup.string().email("Contact Email Must Be Valid"),
      }),
    }),
    gender: sexRequired
      ? Yup.string().required("Sex is Required")
      : Yup.string(),
  });
};
