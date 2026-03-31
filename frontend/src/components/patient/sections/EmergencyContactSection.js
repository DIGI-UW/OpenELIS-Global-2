import React from "react";
import { Field, ErrorMessage } from "formik";
import { Grid, Column, TextInput } from "@carbon/react";
import { useIntl } from "react-intl";

const EmergencyContactSection = ({
  values,
  phoneValidation,
  handlePhoneValidation,
  handleFirstContactNameChange,
  handleLastContactNameChange,
  configurationProperties,
}) => {
  const intl = useIntl();

  return (
    <Grid>
      <Column lg={16} md={8} sm={4}><br /></Column>
      {/* Last Name */}
      <Column lg={8} md={4} sm={4}>
        <Field name="patientContact.person.lastName">
          {({ field }) => (
            <TextInput
              {...field}
              value={values.patientContact?.person?.lastName || ""}
              labelText={intl.formatMessage({ id: "patientcontact.person.lastname" })}
              id={field.name}
              onChange={handleLastContactNameChange}
              placeholder={intl.formatMessage({ id: "patient.emergency.lastname" })}
            />
          )}
        </Field>
      </Column>
      {/* First Name */}
      <Column lg={8} md={4} sm={4}>
        <Field name="patientContact.person.firstName">
          {({ field }) => (
            <TextInput
              {...field}
              value={values.patientContact?.person?.firstName || ""}
              labelText={intl.formatMessage({ id: "patientcontact.person.firstname" })}
              id={field.name}
              onChange={handleFirstContactNameChange}
              placeholder={intl.formatMessage({ id: "patient.emergency.firstname" })}
            />
          )}
        </Field>
      </Column>
      {/* Email */}
      <Column lg={8} md={4} sm={4}>
        <Field name="patientContact.person.email">
          {({ field }) => (
            <TextInput
              {...field}
              value={values.patientContact?.person?.email || ""}
              labelText={intl.formatMessage({ id: "patientcontact.person.email" })}
              id={field.name}
              placeholder={intl.formatMessage({ id: "patient.emergency.email" })}
            />
          )}
        </Field>
        <div className="error">
          <ErrorMessage name="patientContact.person.email" />
        </div>
      </Column>
      {/* Primary Phone */}
      <Column lg={8} md={4} sm={4}>
        <Field name="patientContact.person.primaryPhone">
          {({ field }) => (
            <TextInput
              {...field}
              value={values.patientContact?.person?.primaryPhone || ""}
              id="contactPhone"
              onBlur={handlePhoneValidation}
              labelText={intl.formatMessage(
                { id: "patient.label.contactphone", defaultMessage: "Contact Phone: {PHONE_FORMAT}" },
                { PHONE_FORMAT: configurationProperties.PHONE_FORMAT }
              )}
              invalid={!phoneValidation.contactPhone.status}
              invalidText={phoneValidation.contactPhone.status ? "" : phoneValidation.contactPhone.body}
              placeholder={intl.formatMessage({ id: "patient.emergency.phone" })}
            />
          )}
        </Field>
      </Column>
      <Column lg={16} md={8} sm={4}><br /></Column>
    </Grid>
  );
};

export default EmergencyContactSection;