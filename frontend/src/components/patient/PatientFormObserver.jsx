import { useEffect } from "react";
import { useFormikContext } from "formik";

const normalizeAddressHierarchy = (patient = {}) => {
  const normalized = {};

  Object.entries(patient.addressHierarchy || {}).forEach(([key, value]) => {
    normalized[key] = value || "";
  });

  Object.keys(patient)
    .filter((key) => key.startsWith("addressHierarchy_"))
    .sort()
    .forEach((key) => {
      normalized[key] = patient[key] || "";
    });

  return normalized;
};

const normalizePatientForComparison = (patient = {}) => ({
  patientPK: patient.patientPK || "",
  STnumber: patient.STnumber || "",
  subjectNumber: patient.subjectNumber || "",
  nationalId: patient.nationalId || "",
  guid: patient.guid || "",
  lastName: patient.lastName || "",
  firstName: patient.firstName || "",
  aka: patient.aka || "",
  mothersName: patient.mothersName || "",
  mothersInitial: patient.mothersInitial || "",
  streetAddress: patient.streetAddress || "",
  city: patient.city || "",
  commune: patient.commune || "",
  addressDepartment: patient.addressDepartment || "",
  gender: patient.gender || "",
  birthDateForDisplay: patient.birthDateForDisplay || "",
  patientType: patient.patientType || "",
  insuranceNumber: patient.insuranceNumber || "",
  occupation: patient.occupation || "",
  primaryPhone: patient.primaryPhone || "",
  email: patient.email || "",
  healthRegion: patient.healthRegion || "",
  education: patient.education || "",
  maritialStatus: patient.maritialStatus || "",
  nationality: patient.nationality || "",
  healthDistrict: patient.healthDistrict || "",
  otherNationality: patient.otherNationality || "",
  patientContact: {
    id: patient.patientContact?.id || "",
    person: {
      firstName: patient.patientContact?.person?.firstName || "",
      lastName: patient.patientContact?.person?.lastName || "",
      primaryPhone: patient.patientContact?.person?.primaryPhone || "",
      email: patient.patientContact?.person?.email || "",
    },
  },
  addressHierarchy: normalizeAddressHierarchy(patient),
});

export const mergePatientIntoOrderFormValues = (
  orderFormValues = {},
  values = {},
  patientUpdateStatus,
) => {
  const nextPatientProperties = {
    ...values,
    patientUpdateStatus,
  };

  if (
    orderFormValues.patientUpdateStatus === patientUpdateStatus &&
    JSON.stringify(orderFormValues.patientProperties || {}) ===
      JSON.stringify(nextPatientProperties)
  ) {
    return orderFormValues;
  }

  return {
    ...orderFormValues,
    patientUpdateStatus,
    patientProperties: nextPatientProperties,
  };
};

export const derivePatientUpdateStatus = (
  values,
  selectedPatient,
  fallbackAction,
) => {
  if (!selectedPatient?.patientPK) {
    return fallbackAction;
  }

  return JSON.stringify(normalizePatientForComparison(values)) ===
    JSON.stringify(normalizePatientForComparison(selectedPatient))
    ? "NO_ACTION"
    : "UPDATE";
};

const PatientFormObserver = (props) => {
  const { values } = useFormikContext();
  const { setOrderFormValues, formAction, selectedPatient } = props;

  useEffect(() => {
    const patientUpdateStatus = derivePatientUpdateStatus(
      values,
      selectedPatient,
      formAction,
    );

    setOrderFormValues((previousOrderFormValues) =>
      mergePatientIntoOrderFormValues(
        previousOrderFormValues,
        values,
        patientUpdateStatus,
      ),
    );
  }, [formAction, selectedPatient, setOrderFormValues, values]);

  return null;
};

export default PatientFormObserver;
