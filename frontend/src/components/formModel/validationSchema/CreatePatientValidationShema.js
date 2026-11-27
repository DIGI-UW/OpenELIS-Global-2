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

/**
 * Messages are React Intl keys; CreatePatientForm shows each one once, on its
 * field.
 */
export const createPatientValidationSchema = (configurationProperties = {}) => {
  const nationalIdValidator =
    configurationProperties.PATIENT_NATIONAL_ID_REQUIRED === "false"
      ? Yup.string()
      : Yup.string().required("patient.validation.nationalIdRequired");
  const sexRequired = configurationProperties.PATIENT_SEX_REQUIRED !== "false";
  const ageRequired = configurationProperties.PATIENT_AGE_REQUIRED !== "false";

  return Yup.object().shape({
    nationalId: nationalIdValidator,
    birthDateForDisplay: ageRequired
      ? Yup.string()
          .required("patient.validation.dobOrAgeRequired")
          .test(
            "valid-date",
            "patient.validation.dobInvalid",
            isValidDisplayDate,
          )
      : Yup.string()
          .nullable()
          .test(
            "valid-date",
            "patient.validation.dobInvalid",
            (value) => !value || isValidDisplayDate(value),
          ),
    email: Yup.string().email("patient.validation.emailInvalid"),
    patientContact: Yup.object().shape({
      person: Yup.object().shape({
        email: Yup.string().email("patient.validation.contactEmailInvalid"),
      }),
    }),
    gender: sexRequired
      ? Yup.string().required("patient.validation.sexRequired")
      : Yup.string().nullable(),
  });
};
