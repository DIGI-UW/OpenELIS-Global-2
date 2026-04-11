import React, { useEffect, useRef } from "react";
import { Checkbox, Column, Grid } from "@carbon/react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";

const EQA_PLACEHOLDER = "NULL";

const eqaPatientDefaults = {
  patientUpdateStatus: "ADD",
  nationalId: EQA_PLACEHOLDER,
  subjectNumber: EQA_PLACEHOLDER,
  lastName: EQA_PLACEHOLDER,
  firstName: EQA_PLACEHOLDER,
  streetAddress: "",
  city: "",
  primaryPhone: "",
  gender: "M",
  birthDateForDisplay: "01/01/1900",
  commune: "",
  education: "",
  maritialStatus: "",
  nationality: "",
  healthDistrict: "",
  healthRegion: "",
  otherNationality: "",
  photo: "",
  patientContact: {
    person: { firstName: "", lastName: "", primaryPhone: "", email: "" },
  },
};

const blankPatientDefaults = {
  patientUpdateStatus: "ADD",
  nationalId: "",
  subjectNumber: "",
  lastName: "",
  firstName: "",
  streetAddress: "",
  city: "",
  primaryPhone: "",
  gender: "",
  birthDateForDisplay: "",
  commune: "",
  education: "",
  maritialStatus: "",
  nationality: "",
  healthDistrict: "",
  healthRegion: "",
  otherNationality: "",
  patientContact: {
    person: { firstName: "", lastName: "", primaryPhone: "", email: "" },
  },
  readOnly: false,
};

const EQASampleEntry = ({ orderFormValues, setOrderFormValues }) => {
  const intl = useIntl();

  const isEQA = orderFormValues?.sampleOrderItems?.isEQASample || false;
  const initializedRef = useRef(false);

  // Auto-trigger toggle logic when isEQA is set externally (e.g. ?isEQA=true URL param)
  useEffect(() => {
    if (isEQA && !initializedRef.current) {
      initializedRef.current = true;
      handleEQAToggle(true);
    }
  }, [isEQA]);

  const handleEQAToggle = (checked) => {
    if (checked) {
      // Search for an existing NULL placeholder patient to reuse
      const searchUrl =
        "/rest/patient-search-results?" +
        "lastName=" +
        EQA_PLACEHOLDER +
        "&firstName=" +
        EQA_PLACEHOLDER +
        "&STNumber=&subjectNumber=&nationalID=" +
        encodeURIComponent(EQA_PLACEHOLDER) +
        "&labNumber=&guid=&dateOfBirth=&gender=&suppressExternalSearch=true";

      getFromOpenElisServer(searchUrl, (res) => {
        const results = res?.patientSearchResults || [];
        const existingPatient = results.find(
          (p) =>
            p.lastName === EQA_PLACEHOLDER && p.firstName === EQA_PLACEHOLDER,
        );

        if (existingPatient) {
          // Reuse existing NULL patient
          getFromOpenElisServer(
            "/rest/patient-details?patientID=" + existingPatient.patientID,
            (patientDetails) => {
              setOrderFormValues({
                ...orderFormValues,
                sampleOrderItems: {
                  ...orderFormValues.sampleOrderItems,
                  isEQASample: true,
                },
                patientUpdateStatus: "UPDATE",
                patientProperties: {
                  ...patientDetails,
                  patientUpdateStatus: "UPDATE",
                  readOnly: true,
                },
              });
            },
          );
        } else {
          // No existing NULL patient — create a new one
          setOrderFormValues({
            ...orderFormValues,
            sampleOrderItems: {
              ...orderFormValues.sampleOrderItems,
              isEQASample: true,
            },
            patientProperties: {
              ...eqaPatientDefaults,
              patientUpdateStatus: "ADD",
            },
          });
        }
      });
    } else {
      setOrderFormValues({
        ...orderFormValues,
        sampleOrderItems: {
          ...orderFormValues.sampleOrderItems,
          isEQASample: false,
          eqaProgramId: "",
          eqaProviderOrganizationId: "",
          eqaProviderSampleId: "",
          eqaParticipantId: "",
          eqaDeadline: "",
          eqaPriority: "STANDARD",
        },
        patientProperties: blankPatientDefaults,
      });
    }
  };

  return (
    <Grid fullWidth={true}>
      <Column lg={16} md={8} sm={4}>
        <Checkbox
          id="eqa-sample-checkbox"
          labelText={intl.formatMessage({ id: "eqa.sample.checkbox" })}
          checked={isEQA}
          onChange={(_, { checked }) => handleEQAToggle(checked)}
          data-testid="eqa-sample-checkbox"
        />
      </Column>
    </Grid>
  );
};

export default EQASampleEntry;
