import React, { useEffect, useRef } from "react";
import { Checkbox, Column, Grid } from "@carbon/react";
import { useIntl } from "react-intl";

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

const EQASampleEntry = ({
  orderFormValues,
  setOrderFormValues,
  autoEnable = false,
}) => {
  const intl = useIntl();

  const isEQA = orderFormValues?.sampleOrderItems?.isEQASample || false;
  const autoTriggered = useRef(false);

  const handleEQAToggle = (checked) => {
    setOrderFormValues((prev) => ({
      ...prev,
      sampleOrderItems: checked
        ? { ...prev.sampleOrderItems, isEQASample: true }
        : {
            ...prev.sampleOrderItems,
            isEQASample: false,
            eqaProgramId: "",
            eqaProviderSampleId: "",
            eqaDeadline: "",
            eqaPriority: "STANDARD",
          },
      patientProperties: blankPatientDefaults,
    }));
  };

  // When autoEnable (from ?isEQA=true URL), trigger the toggle once on mount
  useEffect(() => {
    if (autoEnable && !autoTriggered.current) {
      autoTriggered.current = true;
      handleEQAToggle(true);
    }
  }, [autoEnable]);

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
