const CreatePatientFormValues = {
  patientUpdateStatus: "ADD",
  nationalId: "",
  subjectNumber: "",
  lastName: "",
  firstName: "",
  aka: "",
  streetAddress: "",
  city: "",
  primaryPhone: "",
  email: "",
  gender: "",
  birthDateForDisplay: "",
  // Display-only age decomposition. Derived from birthDateForDisplay and
  // stripped from the submit payload in handleSubmit.
  years: "",
  months: "",
  days: "",
  commune: "",
  education: "",
  maritialStatus: "",
  nationality: "",
  healthDistrict: "",
  healthRegion: "",
  otherNationality: "",
  occupation: "",
  customNotes: "",
  targetDiseaseProgramme: "",
  photo: "",
  idDocuments: [],
  patientContact: {
    person: {
      firstName: "",
      lastName: "",
      primaryPhone: "",
      email: "",
    },
  },
};

export default CreatePatientFormValues;
