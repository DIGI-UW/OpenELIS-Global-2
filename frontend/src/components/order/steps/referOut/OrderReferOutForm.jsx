import React, { useState } from "react";
import { useIntl, FormattedMessage } from "react-intl";
import { format } from "date-fns";
import {
  Button,
  ComboBox,
  TextInput,
  TextArea,
  TimePicker,
  Stack,
  Form,
  Grid,
  Column,
} from "@carbon/react";
import CustomDatePicker from "../../../common/CustomDatePicker";

const EMPTY_VALUES = {
  referredInstituteId: "",
  agreementReference: "",
  handoffDate: "",
  handoffTime: "",
  expectedReturnDate: "",
  cocContactName: "",
  cocContactPhone: "",
  cocContactEmail: "",
  subcontractNotes: "",
};

const OrderReferOutForm = ({
  initialValues = {},
  referralOrganizations = [],
  onSave,
  onCancel,
  isSaving = false,
}) => {
  const intl = useIntl();

  const splitHandoff = (datetime) => {
    if (!datetime) return { handoffDate: "", handoffTime: "" };
    // Accept "MM/dd/yyyy HH:mm" or ISO; split for the two inputs.
    const m = String(datetime).match(
      /^(\d{2}\/\d{2}\/\d{4})\s+(\d{1,2}:\d{2})/,
    );
    if (m) return { handoffDate: m[1], handoffTime: m[2] };
    const d = new Date(datetime);
    if (Number.isNaN(d.getTime())) return { handoffDate: "", handoffTime: "" };
    return {
      handoffDate: format(d, "MM/dd/yyyy"),
      handoffTime: format(d, "HH:mm"),
    };
  };

  const [values, setValues] = useState(() => ({
    ...EMPTY_VALUES,
    referredInstituteId: initialValues.referredInstituteId || "",
    agreementReference: initialValues.agreementReference || "",
    expectedReturnDate: initialValues.expectedReturnDate || "",
    cocContactName: initialValues.cocContactName || "",
    cocContactPhone: initialValues.cocContactPhone || "",
    cocContactEmail: initialValues.cocContactEmail || "",
    subcontractNotes: initialValues.subcontractNotes || "",
    ...splitHandoff(initialValues.handoffDatetime),
  }));
  const [errors, setErrors] = useState({});

  const setField = (key, value) =>
    setValues((prev) => ({ ...prev, [key]: value }));

  const selectedOrg =
    referralOrganizations.find((o) => o.id === values.referredInstituteId) ||
    null;

  // Validates an E.164-style phone: leading "+", followed by digits with
  // optional spaces, dashes, or parens for readability. Empty is accepted
  // since the field is optional.
  const PHONE_REGEX = /^\+[\d][\d\s()-]{4,}$/;
  const validatePhone = (raw) => {
    if (!raw) return null;
    if (!PHONE_REGEX.test(raw.trim())) {
      return intl.formatMessage({
        id: "error.referOut.phoneFormat",
        defaultMessage:
          "Enter a valid phone number with country code prefix (e.g. +62 812 3456 7890).",
      });
    }
    return null;
  };

  const handlePhoneBlur = () => {
    const err = validatePhone(values.cocContactPhone);
    setErrors((prev) => {
      const next = { ...prev };
      if (err) next.cocContactPhone = err;
      else delete next.cocContactPhone;
      return next;
    });
  };

  // Minimal email shape check — sender@host with at least one dot in the host.
  // Empty is accepted since the field is optional.
  const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  const validateEmail = (raw) => {
    if (!raw) return null;
    if (!EMAIL_REGEX.test(raw.trim())) {
      return intl.formatMessage({
        id: "error.referOut.emailFormat",
        defaultMessage: "Enter a valid email address (e.g. lab@example.org).",
      });
    }
    return null;
  };

  const handleEmailBlur = () => {
    const err = validateEmail(values.cocContactEmail);
    setErrors((prev) => {
      const next = { ...prev };
      if (err) next.cocContactEmail = err;
      else delete next.cocContactEmail;
      return next;
    });
  };

  const validate = () => {
    const next = {};
    if (!values.referredInstituteId) {
      next.referredInstituteId = intl.formatMessage({
        id: "error.referOut.referringLabRequired",
        defaultMessage: "Select a referring lab.",
      });
    }
    if (values.cocContactPhone && values.cocContactPhone.length > 50) {
      next.cocContactPhone = intl.formatMessage(
        {
          id: "error.referOut.maxLength",
          defaultMessage: "Maximum {max} characters.",
        },
        { max: 50 },
      );
    }
    const phoneErr = validatePhone(values.cocContactPhone);
    if (phoneErr) next.cocContactPhone = phoneErr;
    if (values.cocContactEmail && values.cocContactEmail.length > 255) {
      next.cocContactEmail = intl.formatMessage(
        {
          id: "error.referOut.maxLength",
          defaultMessage: "Maximum {max} characters.",
        },
        { max: 255 },
      );
    }
    const emailErr = validateEmail(values.cocContactEmail);
    if (emailErr) next.cocContactEmail = emailErr;
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!validate()) return;

    const handoffDatetime =
      values.handoffDate && values.handoffTime
        ? `${values.handoffDate} ${values.handoffTime}`
        : "";

    onSave({
      referredInstituteId: values.referredInstituteId,
      referredSendDate: handoffDatetime ? values.handoffDate : "",
      agreementReference: values.agreementReference.trim(),
      handoffDatetime,
      expectedReturnDate: values.expectedReturnDate,
      cocContactName: values.cocContactName.trim(),
      cocContactPhone: values.cocContactPhone.trim(),
      cocContactEmail: values.cocContactEmail.trim(),
      subcontractNotes: values.subcontractNotes.trim(),
    });
  };

  return (
    <Form onSubmit={handleSubmit} className="refer-out-form">
      <Stack gap={5}>
        <Grid fullWidth narrow>
          <Column lg={8} md={4} sm={4}>
            <ComboBox
              id="referOut-referringLab"
              titleText={intl.formatMessage({
                id: "label.referOut.field.referringLab",
                defaultMessage: "Referring Lab",
              })}
              items={referralOrganizations}
              itemToString={(item) => (item ? item.value : "")}
              selectedItem={selectedOrg}
              onChange={({ selectedItem }) =>
                setField(
                  "referredInstituteId",
                  selectedItem ? selectedItem.id : "",
                )
              }
              invalid={!!errors.referredInstituteId}
              invalidText={errors.referredInstituteId}
              placeholder={intl.formatMessage({
                id: "label.referOut.field.referringLab.placeholder",
                defaultMessage: "Select a referring lab",
              })}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="referOut-agreementReference"
              labelText={intl.formatMessage({
                id: "label.referOut.field.agreementReference",
                defaultMessage: "Agreement Reference",
              })}
              maxLength={100}
              value={values.agreementReference}
              onChange={(e) => setField("agreementReference", e.target.value)}
            />
          </Column>
        </Grid>

        <Grid fullWidth narrow>
          <Column lg={6} md={4} sm={4}>
            <CustomDatePicker
              id="referOut-handoffDate"
              value={values.handoffDate || ""}
              onChange={(formatted) => setField("handoffDate", formatted)}
              labelText={intl.formatMessage({
                id: "label.referOut.field.handoffDate",
                defaultMessage: "Handoff Date",
              })}
              updateStateValue
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <TimePicker
              id="referOut-handoffTime"
              labelText={intl.formatMessage({
                id: "label.referOut.field.handoffTime",
                defaultMessage: "Handoff Time",
              })}
              value={values.handoffTime}
              onChange={(e) => setField("handoffTime", e.target.value)}
              placeholder={intl.formatMessage({
                id: "label.referOut.field.handoffTime.placeholder",
                defaultMessage: "HH:mm",
              })}
              pattern="([01][0-9]|2[0-3]):[0-5][0-9]"
            />
          </Column>
          <Column lg={6} md={4} sm={4}>
            <CustomDatePicker
              id="referOut-expectedReturnDate"
              value={values.expectedReturnDate || ""}
              onChange={(formatted) =>
                setField("expectedReturnDate", formatted)
              }
              labelText={intl.formatMessage({
                id: "label.referOut.field.expectedReturnDate",
                defaultMessage: "Expected Return Date",
              })}
              updateStateValue
            />
          </Column>
        </Grid>

        <Grid fullWidth narrow>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="referOut-cocContactName"
              labelText={intl.formatMessage({
                id: "label.referOut.field.cocContactName",
                defaultMessage: "Chain-of-Custody Contact Name",
              })}
              maxLength={100}
              value={values.cocContactName}
              onChange={(e) => setField("cocContactName", e.target.value)}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="referOut-cocContactPhone"
              labelText={intl.formatMessage({
                id: "label.referOut.field.cocContactPhone",
                defaultMessage: "Chain-of-Custody Contact Phone",
              })}
              maxLength={50}
              value={values.cocContactPhone}
              onChange={(e) => setField("cocContactPhone", e.target.value)}
              onBlur={handlePhoneBlur}
              type="tel"
              inputMode="tel"
              placeholder={intl.formatMessage({
                id: "label.referOut.field.cocContactPhone.placeholder",
                defaultMessage: "+62 812 3456 7890",
              })}
              invalid={!!errors.cocContactPhone}
              invalidText={errors.cocContactPhone}
            />
          </Column>
        </Grid>

        <Grid fullWidth narrow>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="referOut-cocContactEmail"
              labelText={intl.formatMessage({
                id: "label.referOut.field.cocContactEmail",
                defaultMessage: "Chain-of-Custody Contact Email",
              })}
              maxLength={255}
              value={values.cocContactEmail}
              onChange={(e) => setField("cocContactEmail", e.target.value)}
              onBlur={handleEmailBlur}
              type="email"
              inputMode="email"
              placeholder={intl.formatMessage({
                id: "label.referOut.field.cocContactEmail.placeholder",
                defaultMessage: "lab@example.org",
              })}
              invalid={!!errors.cocContactEmail}
              invalidText={errors.cocContactEmail}
            />
          </Column>
        </Grid>

        <TextArea
          id="referOut-notes"
          labelText={intl.formatMessage({
            id: "label.referOut.field.notes",
            defaultMessage: "Notes",
          })}
          maxLength={500}
          rows={3}
          value={values.subcontractNotes}
          onChange={(e) => setField("subcontractNotes", e.target.value)}
        />

        <div className="refer-out-form__actions">
          <Button kind="primary" type="submit" disabled={isSaving}>
            <FormattedMessage
              id="label.referOut.action.save"
              defaultMessage="Save Referral"
            />
          </Button>
          <Button kind="ghost" onClick={onCancel} disabled={isSaving}>
            <FormattedMessage
              id="label.button.cancel"
              defaultMessage="Cancel"
            />
          </Button>
        </div>
      </Stack>
    </Form>
  );
};

export default OrderReferOutForm;
