import React, { useContext, useMemo } from "react";
import {
  Button,
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
import config from "../../../config.json";

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
    disabled
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

  const patientName =
    `${patient.lastName || ""} ${patient.firstName || ""}`.trim() || "Patient";

  const [labNoOpen, setLabNoOpen] = React.useState(false);
  const [labNoVal, setLabNoVal] = React.useState("");

  const openAllReports = () => {
    const url =
      config.serverBaseUrl +
      "/ReportPrint?report=patientCILNSP_vreduit&type=patient" +
      "&accessionDirect=&highAccessionDirect=" +
      "&dateOfBirthSearchValue=&selPatient=" +
      encodeURIComponent(String(patient.patientPK ?? "")) +
      "&referringSiteId=&referringSiteDepartmentId=" +
      "&onlyResults=false&_onlyResults=on" +
      "&dateType=RESULT_DATE&lowerDateRange=&upperDateRange=";
    window.open(url, "_blank");
  };

  const openLabNoReport = () => {
    if (!labNoVal.trim()) return;
    const url =
      config.serverBaseUrl +
      "/ReportPrint?report=patientCILNSP_vreduit&type=patient" +
      "&accessionDirect=" +
      encodeURIComponent(labNoVal.trim()) +
      "&highAccessionDirect=" +
      encodeURIComponent(labNoVal.trim());
    window.open(url, "_blank");
  };

  return (
    <Tile className="patientSummaryTile">
      <Grid fullWidth>
        {/* Avatar */}
        <Column lg={2} md={2} sm={4}>
          <div className="patientSummaryPhotoCard">
            <AsyncAvatar
              patientId={patient.patientPK ? String(patient.patientPK) : null}
              hasPhoto={Boolean(patient.patientPK)}
              patientName={patientName}
              size={80}
              gender={patient.gender}
            />
          </div>
        </Column>

        {/* Full name */}
        <Column lg={4} md={3} sm={4}>
          <TextInput
            id="patient-summary-name"
            className="patientReadonlyField"
            labelText={intl.formatMessage({
              id: "patient.name",
              defaultMessage: "Full Name",
            })}
            value={patientName}
            disabled
          />
        </Column>

        {/* Phone */}
        <Column lg={3} md={3} sm={4}>
          <TextInput
            id="patient-summary-primary-phone"
            className="patientReadonlyField"
            labelText={intl.formatMessage(
              { id: "patient.label.primaryphone", defaultMessage: "Phone" },
              { PHONE_FORMAT: "" },
            )}
            value={patient.primaryPhone ?? ""}
            disabled
          />
        </Column>

        {/* Gender */}
        <Column lg={3} md={3} sm={4}>
          <RadioButtonGroup
            className="patientReadonlyRadioGroup"
            legendText={intl.formatMessage({ id: "patient.gender" })}
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

        {/* DOB */}
        <Column lg={4} md={3} sm={4}>
          <TextInput
            id="patient-summary-dob"
            className="patientReadonlyField"
            labelText={intl.formatMessage({ id: "patient.dob" })}
            value={patient.birthDateForDisplay ?? ""}
            disabled
          />
        </Column>
      </Grid>

      {/* Action buttons */}
      <div
        style={{
          display: "flex",
          flexWrap: "wrap",
          gap: "1rem",
          alignItems: "flex-start",
          marginTop: "1.25rem",
          paddingTop: "1rem",
          borderTop: "1px solid #e0e0e0",
        }}
      >
        {/* All Reports button with description */}
        <div
          style={{ display: "flex", flexDirection: "column", gap: "0.25rem" }}
        >
          <Button kind="primary" size="md" onClick={openAllReports}>
            <FormattedMessage
              id="report.all.client"
              defaultMessage="All Reports"
            />
          </Button>
          <span
            style={{
              fontSize: "0.75rem",
              color: "#525252",
              maxWidth: "180px",
              lineHeight: "1.3",
            }}
          >
            <FormattedMessage
              id="report.all.client.description"
              defaultMessage="Generate all recorded lab results for this patient"
            />
          </span>
        </div>

        {/* By Lab No button with description */}
        <div
          style={{ display: "flex", flexDirection: "column", gap: "0.25rem" }}
        >
          <Button
            kind="tertiary"
            size="md"
            onClick={() => {
              setLabNoOpen((v) => !v);
              setLabNoVal("");
            }}
          >
            <FormattedMessage id="report.by.labno" defaultMessage="By Lab No" />
          </Button>
          <span
            style={{
              fontSize: "0.75rem",
              color: "#525252",
              maxWidth: "180px",
              lineHeight: "1.3",
            }}
          >
            <FormattedMessage
              id="report.by.labno.description"
              defaultMessage="Print report for a specific accession number"
            />
          </span>
        </div>

        {/* Inline lab number input — shown only when toggled */}
        {labNoOpen && (
          <div
            style={{
              display: "flex",
              flexDirection: "column",
              gap: "0.25rem",
              alignSelf: "flex-start",
            }}
          >
            <div
              style={{ display: "flex", gap: "0.5rem", alignItems: "flex-end" }}
            >
              <TextInput
                id="patient-summary-labno-input"
                labelText={intl.formatMessage({
                  id: "report.enter.labNumber.headline",
                  defaultMessage: "Accession / Lab No",
                })}
                placeholder="e.g. mberDEV01260000000000015"
                size="md"
                value={labNoVal}
                onChange={(e) =>
                  setLabNoVal((e.target as HTMLInputElement).value)
                }
                onKeyDown={(e: React.KeyboardEvent) => {
                  if (e.key === "Enter" && labNoVal.trim()) openLabNoReport();
                }}
                style={{ width: "240px" }}
              />
              <Button
                kind="primary"
                size="md"
                disabled={!labNoVal.trim()}
                onClick={openLabNoReport}
              >
                <FormattedMessage
                  id="label.button.print"
                  defaultMessage="Print"
                />
              </Button>
            </div>
          </div>
        )}
      </div>
    </Tile>
  );
};

export default PatientSummaryReadonly;
