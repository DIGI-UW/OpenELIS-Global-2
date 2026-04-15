import React, { useContext, useMemo } from "react";
import {
  Accordion,
  AccordionItem,
  Column,
  Grid,
  RadioButton,
  RadioButtonGroup,
  TextInput,
  Tile,
} from "@carbon/react";
import {
  addMonths,
  addYears,
  differenceInDays,
  differenceInMonths,
  differenceInYears,
} from "date-fns";
import { FormattedMessage, useIntl } from "react-intl";
import { ConfigurationContext } from "../../layout/Layout";
import AsyncAvatar from "../photoManagement/photoAvatar/AyncAvatar";

type PatientContactPerson = {
  firstName?: string;
  lastName?: string;
  primaryPhone?: string;
  email?: string;
};

type PatientSummary = {
  patientPK?: number | null;
  nationalId?: string;
  subjectNumber?: string;
  lastName?: string;
  firstName?: string;
  streetAddress?: string;
  city?: string;
  primaryPhone?: string;
  gender?: string;
  birthDateForDisplay?: string;
  commune?: string;
  education?: string;
  maritialStatus?: string;
  nationality?: string;
  healthDistrict?: string;
  healthRegion?: string;
  otherNationality?: string;
  patientContact?: {
    person?: PatientContactPerson;
  };
};

type PatientSummaryReadonlyProps = {
  patient: PatientSummary;
};

type ConfigurationContextValue = {
  configurationProperties: Record<string, string | undefined>;
  reloadConfiguration?: () => void;
} | null;

type AgeParts = {
  years: string;
  months: string;
  days: string;
};

const parseBirthDate = (
  birthDateForDisplay: string | undefined,
  locale: string | undefined,
) => {
  if (!birthDateForDisplay) {
    return null;
  }

  const parts = birthDateForDisplay.split("/");
  if (parts.length !== 3) {
    return null;
  }

  const year = Number(parts[2]);
  const month = Number(locale === "fr-FR" ? parts[1] : parts[0]);
  const day = Number(locale === "fr-FR" ? parts[0] : parts[1]);

  const parsedDate = new Date(year, month - 1, day);

  return Number.isNaN(parsedDate.getTime()) ? null : parsedDate;
};

const getAgeParts = (
  birthDateForDisplay: string | undefined,
  locale: string | undefined,
): AgeParts => {
  const birthDate = parseBirthDate(birthDateForDisplay, locale);

  if (!birthDate) {
    return { years: "", months: "", days: "" };
  }

  const today = new Date();
  const years = differenceInYears(today, birthDate);
  const months = differenceInMonths(today, addYears(birthDate, years));
  const days = differenceInDays(
    today,
    addMonths(addYears(birthDate, years), months),
  );

  return {
    years: String(years),
    months: String(months),
    days: String(days),
  };
};

type ReadonlyFieldProps = {
  id: string;
  labelText: React.ReactNode;
  value?: string;
};

const ReadonlyField = ({ id, labelText, value = "" }: ReadonlyFieldProps) => (
  <TextInput
    id={id}
    className="patientReadonlyField"
    labelText={labelText}
    value={value}
    readOnly
  />
);

const PatientSummaryReadonly: React.FC<PatientSummaryReadonlyProps> = ({
  patient,
}) => {
  const intl = useIntl();
  const configurationContext =
    useContext<ConfigurationContextValue>(ConfigurationContext);
  const configurationProperties =
    configurationContext?.configurationProperties ?? {};

  const ageParts = useMemo(
    () =>
      getAgeParts(
        patient.birthDateForDisplay,
        configurationProperties.DEFAULT_DATE_LOCALE,
      ),
    [patient.birthDateForDisplay, configurationProperties.DEFAULT_DATE_LOCALE],
  );

  const patientName =
    `${patient.lastName || ""} ${patient.firstName || ""}`.trim() || "Patient";

  return (
    <Tile className="patientSummaryTile">
      <div className="patientSummaryHeading">
        <h4>
          <FormattedMessage id="patient.label.info" />
        </h4>
      </div>

      <Grid fullWidth>
        <Column lg={4} md={4} sm={4}>
          <div className="patientSummaryPhotoCard">
            <AsyncAvatar
              patientId={patient.patientPK ? String(patient.patientPK) : null}
              hasPhoto={Boolean(patient.patientPK)}
              patientName={patientName}
              size={112}
              gender={patient.gender}
            />
          </div>
        </Column>

        <Column lg={6} md={4} sm={4}>
          <ReadonlyField
            id="patient-summary-subject-number"
            labelText={intl.formatMessage({ id: "patient.subject.number" })}
            value={patient.subjectNumber}
          />
        </Column>

        <Column lg={6} md={4} sm={4}>
          <ReadonlyField
            id="patient-summary-national-id"
            labelText={
              <>
                {intl.formatMessage({ id: "patient.natioanalid" })}
                <span className="requiredlabel">*</span>
              </>
            }
            value={patient.nationalId}
          />
        </Column>

        <Column lg={6} md={4} sm={4}>
          <ReadonlyField
            id="patient-summary-last-name"
            labelText={intl.formatMessage({ id: "patient.last.name" })}
            value={patient.lastName}
          />
        </Column>

        <Column lg={6} md={4} sm={4}>
          <ReadonlyField
            id="patient-summary-first-name"
            labelText={intl.formatMessage({ id: "patient.first.name" })}
            value={patient.firstName}
          />
        </Column>

        <Column lg={4} md={4} sm={4}>
          <ReadonlyField
            id="patient-summary-primary-phone"
            labelText={intl.formatMessage(
              {
                id: "patient.label.primaryphone",
                defaultMessage: "Primary phone: {PHONE_FORMAT}",
              },
              {
                PHONE_FORMAT: configurationProperties.PHONE_FORMAT || "",
              },
            )}
            value={patient.primaryPhone}
          />
        </Column>

        <Column lg={8} md={4} sm={4}>
          <RadioButtonGroup
            className="patientReadonlyRadioGroup"
            legendText={
              <>
                {intl.formatMessage({ id: "patient.gender" })}
                <span className="requiredlabel">*</span>
              </>
            }
            name="patient-summary-gender"
            valueSelected={patient.gender || ""}
          >
            <RadioButton
              id="patient-summary-male"
              labelText={intl.formatMessage({ id: "patient.male" })}
              value="M"
              disabled
            />
            <RadioButton
              id="patient-summary-female"
              labelText={intl.formatMessage({ id: "patient.female" })}
              value="F"
              disabled
            />
          </RadioButtonGroup>
        </Column>

        <Column lg={4} md={4} sm={4}>
          <ReadonlyField
            id="patient-summary-dob"
            labelText={
              <>
                {intl.formatMessage({ id: "patient.dob" })}
                <span className="requiredlabel">*</span>
              </>
            }
            value={patient.birthDateForDisplay}
          />
        </Column>

        <Column lg={4} md={4} sm={4}>
          <ReadonlyField
            id="patient-summary-years"
            labelText={intl.formatMessage({ id: "patient.age.years" })}
            value={ageParts.years}
          />
        </Column>

        <Column lg={4} md={4} sm={4}>
          <ReadonlyField
            id="patient-summary-months"
            labelText={intl.formatMessage({ id: "patient.age.months" })}
            value={ageParts.months}
          />
        </Column>

        <Column lg={4} md={4} sm={4}>
          <ReadonlyField
            id="patient-summary-days"
            labelText={intl.formatMessage({ id: "patient.age.days" })}
            value={ageParts.days}
          />
        </Column>
      </Grid>

      <Accordion className="patientSummaryAccordion">
        <AccordionItem
          title={intl.formatMessage({ id: "emergencyContactInfo.title" })}
        >
          <Grid fullWidth>
            <Column lg={6} md={4} sm={4}>
              <ReadonlyField
                id="patient-summary-contact-last-name"
                labelText={intl.formatMessage({
                  id: "patientcontact.person.lastname",
                })}
                value={patient.patientContact?.person?.lastName}
              />
            </Column>

            <Column lg={6} md={4} sm={4}>
              <ReadonlyField
                id="patient-summary-contact-first-name"
                labelText={intl.formatMessage({
                  id: "patientcontact.person.firstname",
                })}
                value={patient.patientContact?.person?.firstName}
              />
            </Column>

            <Column lg={4} md={4} sm={4}>
              <ReadonlyField
                id="patient-summary-contact-phone"
                labelText={intl.formatMessage(
                  {
                    id: "patient.label.contactphone",
                    defaultMessage: "Contact Phone: {PHONE_FORMAT}",
                  },
                  {
                    PHONE_FORMAT: configurationProperties.PHONE_FORMAT || "",
                  },
                )}
                value={patient.patientContact?.person?.primaryPhone}
              />
            </Column>

            <Column lg={8} md={4} sm={4}>
              <ReadonlyField
                id="patient-summary-contact-email"
                labelText={intl.formatMessage({
                  id: "patientcontact.person.email",
                })}
                value={patient.patientContact?.person?.email}
              />
            </Column>
          </Grid>
        </AccordionItem>

        <AccordionItem
          title={intl.formatMessage({ id: "patient.label.additionalInfo" })}
        >
          <Grid fullWidth>
            <Column lg={4} md={4} sm={4}>
              <ReadonlyField
                id="patient-summary-city"
                labelText={intl.formatMessage({ id: "patient.address.town" })}
                value={patient.city}
              />
            </Column>

            <Column lg={4} md={4} sm={4}>
              <ReadonlyField
                id="patient-summary-street-address"
                labelText={intl.formatMessage({ id: "patient.address.street" })}
                value={patient.streetAddress}
              />
            </Column>

            <Column lg={4} md={4} sm={4}>
              <ReadonlyField
                id="patient-summary-commune"
                labelText={intl.formatMessage({ id: "patient.address.camp" })}
                value={patient.commune}
              />
            </Column>

            <Column lg={4} md={4} sm={4}>
              <ReadonlyField
                id="patient-summary-health-region"
                labelText={intl.formatMessage({
                  id: "patient.address.healthregion",
                })}
                value={patient.healthRegion}
              />
            </Column>

            <Column lg={4} md={4} sm={4}>
              <ReadonlyField
                id="patient-summary-health-district"
                labelText={intl.formatMessage({
                  id: "patient.address.healthdistrict",
                })}
                value={patient.healthDistrict}
              />
            </Column>

            <Column lg={4} md={4} sm={4}>
              <ReadonlyField
                id="patient-summary-education"
                labelText={intl.formatMessage({ id: "patient.eduction" })}
                value={patient.education}
              />
            </Column>

            <Column lg={4} md={4} sm={4}>
              <ReadonlyField
                id="patient-summary-maritial-status"
                labelText={intl.formatMessage({ id: "patient.maritalstatus" })}
                value={patient.maritialStatus}
              />
            </Column>

            <Column lg={4} md={4} sm={4}>
              <ReadonlyField
                id="patient-summary-nationality"
                labelText={intl.formatMessage({ id: "patient.nationality" })}
                value={patient.nationality}
              />
            </Column>

            <Column lg={4} md={4} sm={4}>
              <ReadonlyField
                id="patient-summary-other-nationality"
                labelText={intl.formatMessage({
                  id: "patient.nationality.other",
                })}
                value={patient.otherNationality}
              />
            </Column>
          </Grid>
        </AccordionItem>
      </Accordion>
    </Tile>
  );
};

export default PatientSummaryReadonly;
