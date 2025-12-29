import * as Yup from "yup";

const CreatePatientValidationSchema = Yup.object().shape({
  nationalId: Yup.string().required("National ID Required"),
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

      if (!validDate1 && !validDate2) {
        return false;
      }

      // Use the valid date format
      const birthDate = validDate1 ? date : date2;
      
      // Get today's date at midnight for comparison
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      
      // Get birth date at midnight for comparison
      const birthDateMidnight = new Date(birthDate);
      birthDateMidnight.setHours(0, 0, 0, 0);
      
      // Allow dates up to and including today (not future dates)
      return birthDateMidnight <= today;
    }),
  patientContact: Yup.object().shape({
    person: Yup.object().shape({
      email: Yup.string().email("Contact Email Must Be Valid"),
    }),
  }),
  gender: Yup.string().required("Gender is Required"),
});

export default CreatePatientValidationSchema;
