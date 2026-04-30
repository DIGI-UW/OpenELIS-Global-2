import React, { useContext, useEffect, useState } from "react";
import {
  Button,
  Checkbox,
  Column,
  Grid,
  Loading,
  Select,
  SelectItem,
  Tag,
  TextInput,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { postToOpenElisServer } from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";
import { NotificationKinds } from "../common/CustomNotification";

// ─────────────────────────────────────────────────────────────────────────────
// Editable field helpers
// ─────────────────────────────────────────────────────────────────────────────

const EditField = ({
  id,
  labelId,
  defaultMessage,
  value,
  onChange,
  required = false,
  disabled = false,
}) => {
  const intl = useIntl();
  return (
    <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
      <TextInput
        id={id || labelId}
        labelText={intl.formatMessage({ id: labelId, defaultMessage })}
        value={value || ""}
        onChange={(e) => onChange(e.target.value)}
        required={required}
        disabled={disabled}
      />
    </Column>
  );
};

const EditSelect = ({
  id,
  labelId,
  defaultMessage,
  value,
  options = [],
  onChange,
  required = false,
  disabled = false,
  optionLabelKey = "value",
}) => {
  const intl = useIntl();
  return (
    <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
      <Select
        id={id || labelId + "_sel"}
        labelText={intl.formatMessage({ id: labelId, defaultMessage })}
        value={value || ""}
        onChange={(e) => onChange(e.target.value)}
        required={required}
        disabled={disabled}
      >
        <SelectItem value="" text="" />
        {options.map((opt) => (
          <SelectItem
            key={opt.id}
            value={String(opt.id)}
            text={
              opt[optionLabelKey] ||
              opt.organizationName ||
              opt.doubleName ||
              opt.value ||
              String(opt.id)
            }
          />
        ))}
      </Select>
    </Column>
  );
};

const EditCheckbox = ({
  id,
  labelId,
  defaultMessage,
  checked,
  onChange,
  disabled = false,
}) => {
  const intl = useIntl();
  return (
    <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
      <Checkbox
        id={id || labelId + "_cb"}
        labelText={intl.formatMessage({ id: labelId, defaultMessage })}
        checked={!!checked}
        onChange={(_, { checked: val }) => onChange(val)}
        disabled={disabled}
      />
    </Column>
  );
};

const SectionHeading = ({ labelId, defaultMessage }) => (
  <Column
    lg={16}
    md={8}
    sm={4}
    style={{ marginTop: "1.5rem", marginBottom: "0.5rem" }}
  >
    <hr />
    <strong>
      <FormattedMessage id={labelId} defaultMessage={defaultMessage} />
    </strong>
  </Column>
);

// ─────────────────────────────────────────────────────────────────────────────
// Shared demographics block (editable)
// ─────────────────────────────────────────────────────────────────────────────

const EditableDemographicsBlock = ({ formData, updateField, lists }) => (
  <>
    <SectionHeading
      labelId="patient.project.patientInfo"
      defaultMessage="Patient Information"
    />
    <EditField
      id="lastName"
      labelId="patient.project.patientFamilyName"
      defaultMessage="Family Name"
      value={formData.lastName}
      onChange={(v) => updateField("lastName", v)}
    />
    <EditField
      id="firstName"
      labelId="patient.project.patientFirstNames"
      defaultMessage="First Names"
      value={formData.firstName}
      onChange={(v) => updateField("firstName", v)}
    />
    <EditSelect
      id="gender"
      labelId="patient.project.gender"
      defaultMessage="Gender"
      value={formData.gender}
      options={lists.genders || []}
      onChange={(v) => updateField("gender", v)}
      required
    />
    <EditField
      id="birthDateForDisplay"
      labelId="patient.project.dateOfBirth"
      defaultMessage="Date of Birth"
      value={formData.birthDateForDisplay}
      onChange={(v) => updateField("birthDateForDisplay", v)}
      required
    />
    <EditField
      id="subjectNumber"
      labelId="patient.subject.number"
      defaultMessage="Subject Number"
      value={formData.subjectNumber}
      onChange={(v) => updateField("subjectNumber", v)}
    />
    <EditField
      id="siteSubjectNumber"
      labelId="patient.site.subject.number"
      defaultMessage="Site Subject Number"
      value={formData.siteSubjectNumber}
      onChange={(v) => updateField("siteSubjectNumber", v)}
    />
    <EditField
      id="labNo"
      labelId="patient.project.labNo"
      defaultMessage="Lab No."
      value={formData.labNo}
      onChange={(v) => updateField("labNo", v)}
      required
    />
    <EditField
      id="receivedDateForDisplay"
      labelId="sample.entry.project.receivedDate"
      defaultMessage="Received Date"
      value={formData.receivedDateForDisplay}
      onChange={(v) => updateField("receivedDateForDisplay", v)}
      required
    />
    <EditField
      id="interviewDate"
      labelId="patient.project.interviewDate"
      defaultMessage="Interview Date"
      value={formData.interviewDate}
      onChange={(v) => updateField("interviewDate", v)}
      required
    />
  </>
);

// ─────────────────────────────────────────────────────────────────────────────
// Initial ARV editable sub-form
// ─────────────────────────────────────────────────────────────────────────────

const InitialARVEditForm = ({
  formData,
  obs,
  projectData,
  lists,
  updateField,
  updateObs,
  updateProject,
}) => {
  const [showNationalityOther, setShowNationalityOther] = useState(
    !!obs.nationalityOther,
  );
  const [showArvProphylaxis, setShowArvProphylaxis] = useState(false);
  const [showPriorDiseases, setShowPriorDiseases] = useState(false);
  const [showCurrentDiseases, setShowCurrentDiseases] = useState(false);
  const [showPriorARVInns, setShowPriorARVInns] = useState(false);
  const [showUnderInvNote, setShowUnderInvNote] = useState(false);

  useEffect(() => {
    if (obs.nationalityOther) setShowNationalityOther(true);
    if (obs.arvProphylaxisBenefit) {
      const opt = (lists.yesNo || []).find(
        (o) => String(o.id) === String(obs.arvProphylaxisBenefit),
      );
      setShowArvProphylaxis(
        opt && (opt.value || "").toLowerCase().startsWith("yes"),
      );
    }
    if (obs.anyPriorDiseases) {
      const opt = (lists.yesNo || []).find(
        (o) => String(o.id) === String(obs.anyPriorDiseases),
      );
      setShowPriorDiseases(
        opt && (opt.value || "").toLowerCase().startsWith("yes"),
      );
    }
    if (obs.anyCurrentDiseases) {
      const opt = (lists.yesNo || []).find(
        (o) => String(o.id) === String(obs.anyCurrentDiseases),
      );
      setShowCurrentDiseases(
        opt && (opt.value || "").toLowerCase().startsWith("yes"),
      );
    }
    if (obs.priorARVTreatment) {
      const opt = (lists.yesNoUnknownNaNotSpec || []).find(
        (o) => String(o.id) === String(obs.priorARVTreatment),
      );
      setShowPriorARVInns(
        opt && (opt.value || "").toLowerCase().startsWith("yes"),
      );
    }
    if (obs.underInvestigation) {
      const opt = (lists.yesNo || []).find(
        (o) => String(o.id) === String(obs.underInvestigation),
      );
      setShowUnderInvNote(
        opt && (opt.value || "").toLowerCase().startsWith("yes"),
      );
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleNationalityChange = (val) => {
    updateObs("nationality", val);
    const opt = (lists.nationalities || []).find(
      (o) => String(o.id) === String(val),
    );
    setShowNationalityOther(
      opt &&
        ((opt.value || "").toLowerCase().includes("autre") ||
          (opt.value || "").toLowerCase().includes("other")),
    );
  };

  const handleArvProphylaxisBenefitChange = (val) => {
    updateObs("arvProphylaxisBenefit", val);
    const opt = (lists.yesNo || []).find((o) => String(o.id) === String(val));
    setShowArvProphylaxis(
      opt && (opt.value || "").toLowerCase().startsWith("yes"),
    );
  };

  const handleAnyPriorDiseasesChange = (val) => {
    updateObs("anyPriorDiseases", val);
    const opt = (lists.yesNo || []).find((o) => String(o.id) === String(val));
    setShowPriorDiseases(
      opt && (opt.value || "").toLowerCase().startsWith("yes"),
    );
  };

  const handleAnyCurrentDiseasesChange = (val) => {
    updateObs("anyCurrentDiseases", val);
    const opt = (lists.yesNo || []).find((o) => String(o.id) === String(val));
    setShowCurrentDiseases(
      opt && (opt.value || "").toLowerCase().startsWith("yes"),
    );
  };

  const handlePriorARVTreatmentChange = (val) => {
    updateObs("priorARVTreatment", val);
    const opt = (lists.yesNoUnknownNaNotSpec || []).find(
      (o) => String(o.id) === String(val),
    );
    setShowPriorARVInns(
      opt && (opt.value || "").toLowerCase().startsWith("yes"),
    );
  };

  const handleUnderInvestigationChange = (val) => {
    updateObs("underInvestigation", val);
    const opt = (lists.yesNo || []).find((o) => String(o.id) === String(val));
    setShowUnderInvNote(
      opt && (opt.value || "").toLowerCase().startsWith("yes"),
    );
  };

  const priorINNs = obs.priorARVTreatmentINNsList || ["", "", "", ""];

  return (
    <>
      <EditableDemographicsBlock
        formData={formData}
        updateField={updateField}
        lists={lists}
      />

      <EditSelect
        id="centerName"
        labelId="patient.project.centerName"
        defaultMessage="Center Name"
        value={formData.centerName}
        options={lists.arvOrgsByName || []}
        onChange={(v) => updateField("centerName", v)}
      />
      <EditSelect
        id="centerCode"
        labelId="patient.project.centerCode"
        defaultMessage="Center Code"
        value={formData.centerCode}
        options={lists.arvOrgs || []}
        onChange={(v) => updateField("centerCode", v)}
        required
      />

      <SectionHeading
        labelId="patient.project.medicalHistory"
        defaultMessage="Medical History"
      />
      <EditSelect
        id="educationLevel"
        labelId="patient.project.educationLevel"
        defaultMessage="Education Level"
        value={obs.educationLevel}
        options={lists.educationLevels || []}
        onChange={(v) => updateObs("educationLevel", v)}
      />
      <EditSelect
        id="maritalStatus"
        labelId="patient.project.maritalStatus"
        defaultMessage="Marital Status"
        value={obs.maritalStatus}
        options={lists.maritalStatuses || []}
        onChange={(v) => updateObs("maritalStatus", v)}
      />
      <EditSelect
        id="nationality"
        labelId="patient.project.nationality"
        defaultMessage="Nationality"
        value={obs.nationality}
        options={lists.nationalities || []}
        onChange={handleNationalityChange}
      />
      {showNationalityOther && (
        <EditField
          id="nationalityOther"
          labelId="patient.project.nationalityOther"
          defaultMessage="Other Nationality"
          value={obs.nationalityOther}
          onChange={(v) => updateObs("nationalityOther", v)}
        />
      )}
      <EditField
        id="legalResidence"
        labelId="patient.project.legalResidence"
        defaultMessage="Legal Residence"
        value={obs.legalResidence}
        onChange={(v) => updateObs("legalResidence", v)}
      />
      <EditField
        id="nameOfDoctor"
        labelId="patient.project.nameOfDoctor"
        defaultMessage="Name of Doctor"
        value={obs.nameOfDoctor}
        onChange={(v) => updateObs("nameOfDoctor", v)}
      />

      <SectionHeading
        labelId="patient.project.antecedents"
        defaultMessage="Antecedents"
      />
      <EditSelect
        id="anyPriorDiseases"
        labelId="patient.project.antecedents"
        defaultMessage="Prior Diseases"
        value={obs.anyPriorDiseases}
        options={lists.yesNo || []}
        onChange={handleAnyPriorDiseasesChange}
      />
      {showPriorDiseases &&
        (lists.priorDiseasesList || []).map((disease) => (
          <EditSelect
            key={disease.name}
            id={disease.name}
            labelId={disease.name}
            defaultMessage={disease.label}
            value={obs[disease.name]}
            options={lists.yesNo || []}
            onChange={(v) => updateObs(disease.name, v)}
          />
        ))}
      <EditSelect
        id="arvProphylaxisBenefit"
        labelId="patient.project.arvProphylaxisBenefit"
        defaultMessage="ARV Prophylaxis Benefit"
        value={obs.arvProphylaxisBenefit}
        options={lists.yesNo || []}
        onChange={handleArvProphylaxisBenefitChange}
      />
      {showArvProphylaxis && (
        <EditSelect
          id="arvProphylaxis"
          labelId="patient.project.arvProphylaxis"
          defaultMessage="ARV Prophylaxis"
          value={obs.arvProphylaxis}
          options={lists.arvProphylaxis || []}
          onChange={(v) => updateObs("arvProphylaxis", v)}
        />
      )}
      <EditSelect
        id="currentARVTreatment"
        labelId="patient.project.currentARVTreatment"
        defaultMessage="Current ARV Treatment"
        value={obs.currentARVTreatment}
        options={lists.yesNo || []}
        onChange={(v) => updateObs("currentARVTreatment", v)}
      />
      <EditSelect
        id="priorARVTreatment"
        labelId="patient.project.priorARVTreatment"
        defaultMessage="Prior ARV Treatment"
        value={obs.priorARVTreatment}
        options={lists.yesNoUnknownNaNotSpec || []}
        onChange={handlePriorARVTreatmentChange}
      />
      {showPriorARVInns &&
        priorINNs.map((inn, i) => (
          <EditField
            key={i}
            id={"priorARVTreatmentINN_" + i}
            labelId="patient.project.priorARVInn"
            defaultMessage="Prior ARV INN"
            value={inn}
            onChange={(v) => {
              const updated = [...priorINNs];
              updated[i] = v;
              updateObs("priorARVTreatmentINNsList", updated);
            }}
          />
        ))}
      <EditSelect
        id="cotrimoxazoleTreatment"
        labelId="patient.project.cotrimoxazoleTreatment"
        defaultMessage="Cotrimoxazole Treatment"
        value={obs.cotrimoxazoleTreatment}
        options={lists.yesNo || []}
        onChange={(v) => updateObs("cotrimoxazoleTreatment", v)}
      />
      <EditSelect
        id="aidsStage"
        labelId="patient.project.aidsStage"
        defaultMessage="AIDS Stage"
        value={obs.aidsStage}
        options={lists.aidsStages || []}
        onChange={(v) => updateObs("aidsStage", v)}
      />
      <EditSelect
        id="anyCurrentDiseases"
        labelId="patient.project.anyCurrentDiseases"
        defaultMessage="Any Current Diseases"
        value={obs.anyCurrentDiseases}
        options={lists.yesNo || []}
        onChange={handleAnyCurrentDiseasesChange}
      />
      {showCurrentDiseases &&
        (lists.currentDiseasesList || []).map((disease) => (
          <EditSelect
            key={disease.name}
            id={disease.name}
            labelId={disease.name}
            defaultMessage={disease.label}
            value={obs[disease.name]}
            options={lists.yesNo || []}
            onChange={(v) => updateObs(disease.name, v)}
          />
        ))}
      <EditSelect
        id="currentOITreatment"
        labelId="patient.project.currentOITreatment"
        defaultMessage="Current OI Treatment"
        value={obs.currentOITreatment}
        options={lists.yesNoUnknownNaNotSpec || []}
        onChange={(v) => updateObs("currentOITreatment", v)}
      />
      <EditField
        id="patientWeight"
        labelId="patient.project.patientWeight"
        defaultMessage="Patient Weight"
        value={obs.patientWeight}
        onChange={(v) => updateObs("patientWeight", v)}
      />
      <EditField
        id="karnofskyScore"
        labelId="patient.project.karnofskyScore"
        defaultMessage="Karnofsky Score"
        value={obs.karnofskyScore}
        onChange={(v) => updateObs("karnofskyScore", v)}
      />
      <EditSelect
        id="underInvestigation"
        labelId="patient.project.underInvestigation"
        defaultMessage="Under Investigation"
        value={obs.underInvestigation}
        options={lists.yesNo || []}
        onChange={handleUnderInvestigationChange}
      />
      {showUnderInvNote && (
        <EditField
          id="underInvestigationNote"
          labelId="patient.project.underInvestigationComment"
          defaultMessage="Under Investigation Note"
          value={projectData.underInvestigationNote}
          onChange={(v) => updateProject("underInvestigationNote", v)}
        />
      )}
    </>
  );
};

// ─────────────────────────────────────────────────────────────────────────────
// Follow-up ARV editable sub-form
// ─────────────────────────────────────────────────────────────────────────────

const FollowupARVEditForm = ({
  formData,
  obs,
  projectData,
  lists,
  updateField,
  updateObs,
  updateProject,
}) => {
  const [showCurrentDiseases, setShowCurrentDiseases] = useState(false);
  const [showPriorARVInns, setShowPriorARVInns] = useState(false);
  const [showUnderInvNote, setShowUnderInvNote] = useState(false);

  useEffect(() => {
    if (obs.anyCurrentDiseases) {
      const opt = (lists.yesNo || []).find(
        (o) => String(o.id) === String(obs.anyCurrentDiseases),
      );
      setShowCurrentDiseases(
        opt && (opt.value || "").toLowerCase().startsWith("yes"),
      );
    }
    if (obs.priorARVTreatment) {
      const opt = (lists.yesNoUnknownNaNotSpec || []).find(
        (o) => String(o.id) === String(obs.priorARVTreatment),
      );
      setShowPriorARVInns(
        opt && (opt.value || "").toLowerCase().startsWith("yes"),
      );
    }
    if (obs.underInvestigation) {
      const opt = (lists.yesNo || []).find(
        (o) => String(o.id) === String(obs.underInvestigation),
      );
      setShowUnderInvNote(
        opt && (opt.value || "").toLowerCase().startsWith("yes"),
      );
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const priorINNs = obs.priorARVTreatmentINNsList || ["", "", "", ""];
  const futureINNs = obs.futureARVTreatmentINNsList || ["", "", "", ""];

  return (
    <>
      <EditableDemographicsBlock
        formData={formData}
        updateField={updateField}
        lists={lists}
      />

      <EditSelect
        id="centerName_followup"
        labelId="patient.project.centerName"
        defaultMessage="Center Name"
        value={formData.centerName}
        options={lists.arvOrgsByName || []}
        onChange={(v) => updateField("centerName", v)}
      />
      <EditSelect
        id="centerCode_followup"
        labelId="patient.project.centerCode"
        defaultMessage="Center Code"
        value={formData.centerCode}
        options={lists.arvOrgs || []}
        onChange={(v) => updateField("centerCode", v)}
        required
      />

      <SectionHeading
        labelId="patient.project.clinicalInfo"
        defaultMessage="Clinical Information"
      />
      <EditField
        id="patientWeight_fu"
        labelId="patient.project.patientWeight"
        defaultMessage="Patient Weight"
        value={obs.patientWeight}
        onChange={(v) => updateObs("patientWeight", v)}
      />
      <EditField
        id="karnofskyScore_fu"
        labelId="patient.project.karnofskyScore"
        defaultMessage="Karnofsky Score"
        value={obs.karnofskyScore}
        onChange={(v) => updateObs("karnofskyScore", v)}
      />
      <EditSelect
        id="hivStatus_fu"
        labelId="patient.project.hivStatus"
        defaultMessage="HIV Status"
        value={obs.hivStatus}
        options={lists.hivStatuses || []}
        onChange={(v) => updateObs("hivStatus", v)}
      />
      <EditField
        id="cd4Count"
        labelId="patient.project.cd4Count"
        defaultMessage="CD4 Count"
        value={obs.cd4Count}
        onChange={(v) => updateObs("cd4Count", v)}
      />
      <EditField
        id="cd4Percent"
        labelId="patient.project.cd4Percent"
        defaultMessage="CD4 Percent"
        value={obs.cd4Percent}
        onChange={(v) => updateObs("cd4Percent", v)}
      />
      <EditField
        id="priorCd4Date"
        labelId="patient.project.priorCd4Date"
        defaultMessage="Prior CD4 Date"
        value={obs.priorCd4Date}
        onChange={(v) => updateObs("priorCd4Date", v)}
      />
      <EditField
        id="nameOfDoctor_fu"
        labelId="patient.project.nameOfDoctor"
        defaultMessage="Name of Doctor"
        value={obs.nameOfDoctor}
        onChange={(v) => updateObs("nameOfDoctor", v)}
      />

      <SectionHeading
        labelId="patient.project.anyDiseasesSinceLast"
        defaultMessage="Current Diseases"
      />
      <EditSelect
        id="anyCurrentDiseases_fu"
        labelId="patient.project.anyDiseasesSinceLast"
        defaultMessage="Any Diseases Since Last Visit"
        value={obs.anyCurrentDiseases}
        options={lists.yesNo || []}
        onChange={(val) => {
          updateObs("anyCurrentDiseases", val);
          const opt = (lists.yesNo || []).find(
            (o) => String(o.id) === String(val),
          );
          setShowCurrentDiseases(
            opt && (opt.value || "").toLowerCase().startsWith("yes"),
          );
        }}
      />
      {showCurrentDiseases &&
        (lists.currentDiseasesList || []).map((disease) => (
          <EditSelect
            key={disease.name}
            id={disease.name + "_fu"}
            labelId={disease.name}
            defaultMessage={disease.label}
            value={obs[disease.name]}
            options={lists.yesNo || []}
            onChange={(v) => updateObs(disease.name, v)}
          />
        ))}

      <SectionHeading
        labelId="patient.project.arvTreatment"
        defaultMessage="ARV Treatment"
      />
      <EditSelect
        id="interruptedARVTreatment"
        labelId="patient.project.interruptedARVTreatment"
        defaultMessage="Interrupted ARV Treatment"
        value={obs.interruptedARVTreatment}
        options={lists.yesNoNa || []}
        onChange={(v) => updateObs("interruptedARVTreatment", v)}
      />
      <EditSelect
        id="arvTreatmentChange"
        labelId="patient.project.arvTreatmentChange"
        defaultMessage="ARV Treatment Change"
        value={obs.arvTreatmentChange}
        options={lists.yesNoNa || []}
        onChange={(v) => updateObs("arvTreatmentChange", v)}
      />
      <EditSelect
        id="arvTreatmentNew"
        labelId="patient.project.arvTreatmentNew"
        defaultMessage="New ARV Treatment"
        value={obs.arvTreatmentNew}
        options={lists.yesNoNa || []}
        onChange={(v) => updateObs("arvTreatmentNew", v)}
      />
      <EditSelect
        id="arvTreatmentRegime_fu"
        labelId="patient.project.arvTreatmentRegime"
        defaultMessage="ARV Treatment Regime"
        value={obs.arvTreatmentRegime}
        options={lists.arvRegimes || []}
        onChange={(v) => updateObs("arvTreatmentRegime", v)}
      />
      <EditField
        id="arvTreatmentInitDate_fu"
        labelId="patient.project.arvTreatmentInitDate"
        defaultMessage="ARV Treatment Init Date"
        value={obs.arvTreatmentInitDate}
        onChange={(v) => updateObs("arvTreatmentInitDate", v)}
      />
      {futureINNs.map((inn, i) => (
        <EditField
          key={i}
          id={"futureARVTreatmentINN_" + i}
          labelId="patient.project.prescribedARVTreatmentINNs"
          defaultMessage="Prescribed ARV INN"
          value={inn}
          onChange={(v) => {
            const updated = [...futureINNs];
            updated[i] = v;
            updateObs("futureARVTreatmentINNsList", updated);
          }}
        />
      ))}
      <EditSelect
        id="arvTreatmentAnyAdverseEffects_fu"
        labelId="patient.project.treatmentAnyAdverseEffects"
        defaultMessage="Any Adverse Effects"
        value={obs.arvTreatmentAnyAdverseEffects}
        options={lists.yesNo || []}
        onChange={(v) => updateObs("arvTreatmentAnyAdverseEffects", v)}
      />
      <EditSelect
        id="cotrimoxazoleTreatment_fu"
        labelId="patient.project.cotrimoxazoleTreatment"
        defaultMessage="Cotrimoxazole Treatment"
        value={obs.cotrimoxazoleTreatment}
        options={lists.yesNoNa || []}
        onChange={(v) => updateObs("cotrimoxazoleTreatment", v)}
      />
      <EditSelect
        id="anySecondaryTreatment"
        labelId="patient.project.anySecondaryTreatment"
        defaultMessage="Any Secondary Treatment"
        value={obs.anySecondaryTreatment}
        options={lists.yesNo || []}
        onChange={(v) => updateObs("anySecondaryTreatment", v)}
      />
      <EditField
        id="secondaryTreatment"
        labelId="patient.project.secondaryTreatment"
        defaultMessage="Secondary Treatment"
        value={obs.secondaryTreatment}
        onChange={(v) => updateObs("secondaryTreatment", v)}
      />
      <EditField
        id="clinicVisits"
        labelId="patient.project.clinicVisits"
        defaultMessage="Clinic Visits"
        value={obs.clinicVisits}
        onChange={(v) => updateObs("clinicVisits", v)}
      />
      <EditSelect
        id="priorARVTreatment_fu"
        labelId="patient.project.priorARVTreatment"
        defaultMessage="Prior ARV Treatment"
        value={obs.priorARVTreatment}
        options={lists.yesNoUnknownNaNotSpec || []}
        onChange={(val) => {
          updateObs("priorARVTreatment", val);
          const opt = (lists.yesNoUnknownNaNotSpec || []).find(
            (o) => String(o.id) === String(val),
          );
          setShowPriorARVInns(
            opt && (opt.value || "").toLowerCase().startsWith("yes"),
          );
        }}
      />
      {showPriorARVInns &&
        priorINNs.map((inn, i) => (
          <EditField
            key={i}
            id={"priorARVTreatmentINN_fu_" + i}
            labelId="patient.project.priorARVInn"
            defaultMessage="Prior ARV INN"
            value={inn}
            onChange={(v) => {
              const updated = [...priorINNs];
              updated[i] = v;
              updateObs("priorARVTreatmentINNsList", updated);
            }}
          />
        ))}
      <EditSelect
        id="antiTbTreatment"
        labelId="patient.project.antiTbTreatment"
        defaultMessage="Anti-TB Treatment"
        value={obs.antiTbTreatment}
        options={lists.yesNo || []}
        onChange={(v) => updateObs("antiTbTreatment", v)}
      />
      <EditSelect
        id="cotrimoxazoleTreatmentAnyAdverseEffects"
        labelId="patient.project.cotrimoxazoleTreatAnyAdvEff"
        defaultMessage="Cotrimoxazole Any Adverse Effects"
        value={obs.cotrimoxazoleTreatmentAnyAdverseEffects}
        options={lists.yesNoNa || []}
        onChange={(v) =>
          updateObs("cotrimoxazoleTreatmentAnyAdverseEffects", v)
        }
      />
      <EditSelect
        id="underInvestigation_fu"
        labelId="patient.project.underInvestigation"
        defaultMessage="Under Investigation"
        value={obs.underInvestigation}
        options={lists.yesNo || []}
        onChange={(val) => {
          updateObs("underInvestigation", val);
          const opt = (lists.yesNo || []).find(
            (o) => String(o.id) === String(val),
          );
          setShowUnderInvNote(
            opt && (opt.value || "").toLowerCase().startsWith("yes"),
          );
        }}
      />
      {showUnderInvNote && (
        <EditField
          id="underInvestigationNote_fu"
          labelId="patient.project.underInvestigationComment"
          defaultMessage="Under Investigation Note"
          value={projectData.underInvestigationNote}
          onChange={(v) => updateProject("underInvestigationNote", v)}
        />
      )}
    </>
  );
};

// ─────────────────────────────────────────────────────────────────────────────
// EID editable sub-form
// ─────────────────────────────────────────────────────────────────────────────

const EIDEditForm = ({
  formData,
  obs,
  projectData,
  lists,
  updateField,
  updateObs,
  updateProject,
}) => {
  const [showClinicOther, setShowClinicOther] = useState(
    !!obs.eidTypeOfClinicOther,
  );
  const [showUnderInvNote, setShowUnderInvNote] = useState(false);

  useEffect(() => {
    if (obs.eidTypeOfClinicOther) setShowClinicOther(true);
    if (obs.underInvestigation) {
      const opt = (lists.yesNo || []).find(
        (o) => String(o.id) === String(obs.underInvestigation),
      );
      setShowUnderInvNote(
        opt && (opt.value || "").toLowerCase().startsWith("yes"),
      );
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <>
      <EditableDemographicsBlock
        formData={formData}
        updateField={updateField}
        lists={lists}
      />

      <EditSelect
        id="EIDsiteName"
        labelId="sample.entry.project.EID.siteName"
        defaultMessage="Site Name"
        value={projectData.EIDsiteName}
        options={lists.eidOrgsByName || []}
        onChange={(v) => updateProject("EIDsiteName", v)}
        required
      />
      <EditSelect
        id="EIDsiteCode"
        labelId="sample.entry.project.EID.siteCode"
        defaultMessage="Site Code"
        value={projectData.EIDsiteCode}
        options={lists.eidOrgs || []}
        onChange={(v) => updateProject("EIDsiteCode", v)}
        required
      />
      <EditField
        id="nameOfRequestor"
        labelId="patient.project.nameOfRequestor"
        defaultMessage="Name of Requester"
        value={obs.nameOfRequestor}
        onChange={(v) => updateObs("nameOfRequestor", v)}
      />
      <EditField
        id="nameOfSampler_eid"
        labelId="patient.project.nameOfSampler"
        defaultMessage="Name of Sampler"
        value={obs.nameOfSampler}
        onChange={(v) => updateObs("nameOfSampler", v)}
      />
      <EditField
        id="receivedTimeForDisplay"
        labelId="sample.entry.project.receivedTime"
        defaultMessage="Received Time"
        value={formData.receivedTimeForDisplay}
        onChange={(v) => updateField("receivedTimeForDisplay", v)}
      />
      <EditField
        id="interviewTime"
        labelId="sample.entry.project.timeTaken"
        defaultMessage="Interview Time"
        value={formData.interviewTime}
        onChange={(v) => updateField("interviewTime", v)}
      />

      <SectionHeading
        labelId="patient.project.eidInfo"
        defaultMessage="EID Information"
      />
      <EditSelect
        id="eidTypeOfClinic"
        labelId="patient.project.eidTypeOfClinic"
        defaultMessage="Type of Clinic"
        value={obs.eidTypeOfClinic}
        options={lists.eidTypeOfClinic || []}
        onChange={(val) => {
          updateObs("eidTypeOfClinic", val);
          const opt = (lists.eidTypeOfClinic || []).find(
            (o) => String(o.id) === String(val),
          );
          setShowClinicOther(
            opt &&
              ((opt.value || "").toLowerCase().includes("autre") ||
                (opt.value || "").toLowerCase().includes("other")),
          );
        }}
      />
      {showClinicOther && (
        <EditField
          id="eidTypeOfClinicOther"
          labelId="patient.project.eidTypeOfClinicOther"
          defaultMessage="Type of Clinic (Other)"
          value={obs.eidTypeOfClinicOther}
          onChange={(v) => updateObs("eidTypeOfClinicOther", v)}
        />
      )}
      <EditSelect
        id="eidHowChildFed"
        labelId="patient.project.eidHowChildFed"
        defaultMessage="How Child Fed"
        value={obs.eidHowChildFed}
        options={lists.eidHowChildFed || []}
        onChange={(v) => updateObs("eidHowChildFed", v)}
      />
      <EditSelect
        id="eidStoppedBreastfeeding"
        labelId="patient.project.eidStoppedBreastfeeding"
        defaultMessage="Stopped Breastfeeding"
        value={obs.eidStoppedBreastfeeding}
        options={lists.eidStoppedBreastfeeding || []}
        onChange={(v) => updateObs("eidStoppedBreastfeeding", v)}
      />
      <EditSelect
        id="eidInfantSymptomatic"
        labelId="patient.project.eidInfantSymptomatic"
        defaultMessage="Infant Symptomatic"
        value={obs.eidInfantSymptomatic}
        options={lists.yesNo || []}
        onChange={(v) => updateObs("eidInfantSymptomatic", v)}
      />
      <EditSelect
        id="eidMothersHIVStatus"
        labelId="patient.project.eidMothersStatus"
        defaultMessage="Mother's HIV Status"
        value={obs.eidMothersHIVStatus}
        options={lists.eidMothersHivStatus || []}
        onChange={(v) => updateObs("eidMothersHIVStatus", v)}
      />
      <EditSelect
        id="eidMothersARV"
        labelId="patient.project.eidMothersARV"
        defaultMessage="Mother's ARV"
        value={obs.eidMothersARV}
        options={lists.eidMothersArvTreatment || []}
        onChange={(v) => updateObs("eidMothersARV", v)}
      />
      <EditSelect
        id="eidInfantsARV"
        labelId="patient.project.eidInfantProphy"
        defaultMessage="Infant ARV"
        value={obs.eidInfantsARV}
        options={lists.eidInfantProphylaxisArv || []}
        onChange={(v) => updateObs("eidInfantsARV", v)}
      />
      <EditSelect
        id="eidInfantCotrimoxazole"
        labelId="patient.project.eidInfantCotrimoxazole"
        defaultMessage="Infant Cotrimoxazole"
        value={obs.eidInfantCotrimoxazole}
        options={lists.yesNoUnknown || []}
        onChange={(v) => updateObs("eidInfantCotrimoxazole", v)}
      />
      <EditSelect
        id="eidInfantPTME"
        labelId="patient.project.eidBenefitPTME"
        defaultMessage="Infant Benefit PTME"
        value={obs.eidInfantPTME}
        options={lists.yesNo || []}
        onChange={(v) => updateObs("eidInfantPTME", v)}
      />

      <SectionHeading
        labelId="sample.entry.project.EID.whichPCR"
        defaultMessage="PCR"
      />
      <EditSelect
        id="whichPCR"
        labelId="patient.project.eidWhichPCR"
        defaultMessage="Which PCR"
        value={obs.whichPCR}
        options={lists.eidWhichPcr || []}
        onChange={(v) => updateObs("whichPCR", v)}
      />
      <EditSelect
        id="reasonForSecondPCRTest"
        labelId="sample.entry.project.EID.reasonForPCRTest"
        defaultMessage="Reason for Second PCR"
        value={obs.reasonForSecondPCRTest}
        options={lists.eidSecondPcrReason || []}
        onChange={(v) => updateObs("reasonForSecondPCRTest", v)}
      />

      <SectionHeading
        labelId="sample.entry.project.sampleType"
        defaultMessage="Sample Type"
      />
      <EditCheckbox
        id="dbsTaken_eid"
        labelId="sample.entry.project.EID.DBS"
        defaultMessage="DBS Taken"
        checked={projectData.dbsTaken}
        onChange={(v) => updateProject("dbsTaken", v)}
      />
      <EditSelect
        id="underInvestigation_eid"
        labelId="patient.project.underInvestigation"
        defaultMessage="Under Investigation"
        value={obs.underInvestigation}
        options={lists.yesNo || []}
        onChange={(val) => {
          updateObs("underInvestigation", val);
          const opt = (lists.yesNo || []).find(
            (o) => String(o.id) === String(val),
          );
          setShowUnderInvNote(
            opt && (opt.value || "").toLowerCase().startsWith("yes"),
          );
        }}
      />
      {showUnderInvNote && (
        <EditField
          id="underInvestigationNote_eid"
          labelId="patient.project.underInvestigationComment"
          defaultMessage="Under Investigation Note"
          value={projectData.underInvestigationNote}
          onChange={(v) => updateProject("underInvestigationNote", v)}
        />
      )}
    </>
  );
};

// ─────────────────────────────────────────────────────────────────────────────
// VL editable sub-form
// ─────────────────────────────────────────────────────────────────────────────

const VLEditForm = ({
  formData,
  obs,
  projectData,
  lists,
  updateField,
  updateObs,
  updateProject,
}) => {
  const [showARVTreatmentFields, setShowARVTreatmentFields] = useState(false);
  const [showVlOtherReason, setShowVlOtherReason] = useState(false);
  const [showPregnancy, setShowPregnancy] = useState(false);
  const [showUnderInvNote, setShowUnderInvNote] = useState(false);

  useEffect(() => {
    if (obs.currentARVTreatment) {
      const opt = (lists.yesNo || []).find(
        (o) => String(o.id) === String(obs.currentARVTreatment),
      );
      setShowARVTreatmentFields(
        opt && (opt.value || "").toLowerCase().startsWith("yes"),
      );
    }
    if (obs.vlOtherReasonForRequest) setShowVlOtherReason(true);
    const gender = formData.gender || "";
    setShowPregnancy(gender.toUpperCase() === "F");
    if (obs.underInvestigation) {
      const opt = (lists.yesNo || []).find(
        (o) => String(o.id) === String(obs.underInvestigation),
      );
      setShowUnderInvNote(
        opt && (opt.value || "").toLowerCase().startsWith("yes"),
      );
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const currentINNs = obs.currentARVTreatmentINNsList || ["", "", "", ""];

  return (
    <>
      <EditableDemographicsBlock
        formData={formData}
        updateField={(key, val) => {
          updateField(key, val);
          if (key === "gender") {
            setShowPregnancy(val.toUpperCase() === "F");
          }
        }}
        lists={lists}
      />

      <EditField
        id="upidCode"
        labelId="patient.upid.code"
        defaultMessage="UPID Code"
        value={formData.upidCode}
        onChange={(v) => updateField("upidCode", v)}
      />
      <EditField
        id="receivedTimeForDisplay_vl"
        labelId="sample.entry.project.receivedTime"
        defaultMessage="Received Time"
        value={formData.receivedTimeForDisplay}
        onChange={(v) => updateField("receivedTimeForDisplay", v)}
      />
      <EditField
        id="interviewTime_vl"
        labelId="sample.entry.project.timeTaken"
        defaultMessage="Interview Time"
        value={formData.interviewTime}
        onChange={(v) => updateField("interviewTime", v)}
      />
      <EditSelect
        id="ARVcenterName"
        labelId="sample.entry.project.ARV.centerName"
        defaultMessage="Center Name"
        value={projectData.ARVcenterName}
        options={lists.arvOrgsByName || []}
        onChange={(v) => updateProject("ARVcenterName", v)}
        required
      />
      <EditSelect
        id="ARVcenterCode"
        labelId="patient.project.centerCode"
        defaultMessage="Center Code"
        value={projectData.ARVcenterCode}
        options={lists.arvOrgs || []}
        onChange={(v) => updateProject("ARVcenterCode", v)}
        required
      />
      <EditField
        id="nameOfDoctor_vl"
        labelId="patient.project.nameOfClinician"
        defaultMessage="Name of Clinician"
        value={obs.nameOfDoctor}
        onChange={(v) => updateObs("nameOfDoctor", v)}
      />
      <EditField
        id="nameOfSampler_vl"
        labelId="patient.project.nameOfSampler"
        defaultMessage="Name of Sampler"
        value={obs.nameOfSampler}
        onChange={(v) => updateObs("nameOfSampler", v)}
      />

      <SectionHeading
        labelId="patient.project.clinicalInfo"
        defaultMessage="Clinical Information"
      />
      {showPregnancy && (
        <EditSelect
          id="vlPregnancy"
          labelId="sample.project.vlPregnancy"
          defaultMessage="VL Pregnancy"
          value={obs.vlPregnancy}
          options={lists.yesNo || []}
          onChange={(v) => updateObs("vlPregnancy", v)}
        />
      )}
      {showPregnancy && (
        <EditSelect
          id="vlSuckle"
          labelId="sample.project.vlSuckle"
          defaultMessage="Breastfeeding"
          value={obs.vlSuckle}
          options={lists.yesNo || []}
          onChange={(v) => updateObs("vlSuckle", v)}
        />
      )}
      <EditSelect
        id="hivStatus_vl"
        labelId="patient.project.hivType"
        defaultMessage="HIV Type"
        value={obs.hivStatus}
        options={lists.hivTypes || []}
        onChange={(v) => updateObs("hivStatus", v)}
        required
      />
      <EditSelect
        id="currentARVTreatment_vl"
        labelId="patient.project.currentARVTreatment"
        defaultMessage="Current ARV Treatment"
        value={obs.currentARVTreatment}
        options={lists.yesNo || []}
        onChange={(val) => {
          updateObs("currentARVTreatment", val);
          const opt = (lists.yesNo || []).find(
            (o) => String(o.id) === String(val),
          );
          setShowARVTreatmentFields(
            opt && (opt.value || "").toLowerCase().startsWith("yes"),
          );
        }}
      />
      {showARVTreatmentFields && (
        <>
          <EditField
            id="arvTreatmentInitDate_vl"
            labelId="patient.project.arvTreatmentInitDate"
            defaultMessage="ARV Treatment Init Date"
            value={obs.arvTreatmentInitDate}
            onChange={(v) => updateObs("arvTreatmentInitDate", v)}
          />
          <EditSelect
            id="arvTreatmentRegime_vl"
            labelId="patient.project.arvTreatmentRegime"
            defaultMessage="ARV Treatment Regime"
            value={obs.arvTreatmentRegime}
            options={lists.arvRegimes || []}
            onChange={(v) => updateObs("arvTreatmentRegime", v)}
          />
          {currentINNs.map((inn, i) => (
            <EditField
              key={i}
              id={"currentARVTreatmentINN_" + i}
              labelId="patient.project.priorARVInn"
              defaultMessage="Current ARV INN"
              value={inn}
              onChange={(v) => {
                const updated = [...currentINNs];
                updated[i] = v;
                updateObs("currentARVTreatmentINNsList", updated);
              }}
            />
          ))}
        </>
      )}

      <SectionHeading
        labelId="sample.entry.project.vl.reason"
        defaultMessage="Reason for VL"
      />
      <EditSelect
        id="vlReasonForRequest"
        labelId="sample.entry.project.vl.reason"
        defaultMessage="Reason for VL Request"
        value={obs.vlReasonForRequest}
        options={lists.arvReasonForVlDemand || []}
        onChange={(val) => {
          updateObs("vlReasonForRequest", val);
          const opt = (lists.arvReasonForVlDemand || []).find(
            (o) => String(o.id) === String(val),
          );
          setShowVlOtherReason(
            opt &&
              ((opt.value || "").toLowerCase().includes("autre") ||
                (opt.value || "").toLowerCase().includes("other")),
          );
        }}
      />
      {showVlOtherReason && (
        <EditField
          id="vlOtherReasonForRequest"
          labelId="patient.project.specify"
          defaultMessage="Other Reason"
          value={obs.vlOtherReasonForRequest}
          onChange={(v) => updateObs("vlOtherReasonForRequest", v)}
        />
      )}

      <SectionHeading
        labelId="sample.project.initialCD4"
        defaultMessage="Initial CD4"
      />
      <EditField
        id="initcd4Count"
        labelId="sample.project.cd4Count"
        defaultMessage="Initial CD4 Count"
        value={obs.initcd4Count}
        onChange={(v) => updateObs("initcd4Count", v)}
      />
      <EditField
        id="initcd4Percent"
        labelId="sample.project.cd4Percent"
        defaultMessage="Initial CD4 Percent"
        value={obs.initcd4Percent}
        onChange={(v) => updateObs("initcd4Percent", v)}
      />
      <EditField
        id="initcd4Date"
        labelId="sample.project.Cd4Date"
        defaultMessage="Initial CD4 Date"
        value={obs.initcd4Date}
        onChange={(v) => updateObs("initcd4Date", v)}
      />

      <SectionHeading
        labelId="sample.project.demandCD4"
        defaultMessage="Demand CD4"
      />
      <EditField
        id="demandcd4Count"
        labelId="sample.project.cd4Count"
        defaultMessage="Demand CD4 Count"
        value={obs.demandcd4Count}
        onChange={(v) => updateObs("demandcd4Count", v)}
      />
      <EditField
        id="demandcd4Percent"
        labelId="sample.project.cd4Percent"
        defaultMessage="Demand CD4 Percent"
        value={obs.demandcd4Percent}
        onChange={(v) => updateObs("demandcd4Percent", v)}
      />
      <EditField
        id="demandcd4Date"
        labelId="sample.project.Cd4Date"
        defaultMessage="Demand CD4 Date"
        value={obs.demandcd4Date}
        onChange={(v) => updateObs("demandcd4Date", v)}
      />

      <SectionHeading
        labelId="patient.project.priorVL"
        defaultMessage="Prior VL"
      />
      <EditSelect
        id="vlBenefit"
        labelId="patient.project.vlBenefit"
        defaultMessage="VL Benefit"
        value={obs.vlBenefit}
        options={lists.yesNo || []}
        onChange={(v) => updateObs("vlBenefit", v)}
      />
      <EditField
        id="priorVLLab"
        labelId="patient.project.priorVLLab"
        defaultMessage="Prior VL Lab"
        value={obs.priorVLLab}
        onChange={(v) => updateObs("priorVLLab", v)}
      />
      <EditField
        id="priorVLValue"
        labelId="patient.project.priorVLValue"
        defaultMessage="Prior VL Value"
        value={obs.priorVLValue}
        onChange={(v) => updateObs("priorVLValue", v)}
      />
      <EditField
        id="priorVLDate"
        labelId="patient.project.priorVLDate"
        defaultMessage="Prior VL Date"
        value={obs.priorVLDate}
        onChange={(v) => updateObs("priorVLDate", v)}
      />

      <SectionHeading
        labelId="sample.entry.project.sampleType"
        defaultMessage="Sample Type"
      />
      <EditCheckbox
        id="edtaTubeTaken"
        labelId="sample.entry.project.ARV.edtaTubeTaken"
        defaultMessage="EDTA Tube Taken"
        checked={projectData.edtaTubeTaken}
        onChange={(v) => updateProject("edtaTubeTaken", v)}
      />
      <EditCheckbox
        id="dbsvlTaken"
        labelId="sample.entry.project.ARV.dbsvlTaken"
        defaultMessage="DBS Taken"
        checked={projectData.dbsvlTaken}
        onChange={(v) => updateProject("dbsvlTaken", v)}
      />
      <EditCheckbox
        id="pscvlTaken"
        labelId="sample.entry.project.ARV.pscvlTaken"
        defaultMessage="PSC Taken"
        checked={projectData.pscvlTaken}
        onChange={(v) => updateProject("pscvlTaken", v)}
      />

      <SectionHeading
        labelId="sample.entry.project.tests"
        defaultMessage="Tests"
      />
      <EditCheckbox
        id="viralLoadTest"
        labelId="sample.entry.project.ARV.viralLoadTest"
        defaultMessage="Viral Load Test"
        checked={projectData.viralLoadTest}
        onChange={(v) => updateProject("viralLoadTest", v)}
      />
      <EditSelect
        id="underInvestigation_vl"
        labelId="patient.project.underInvestigation"
        defaultMessage="Under Investigation"
        value={obs.underInvestigation}
        options={lists.yesNo || []}
        onChange={(val) => {
          updateObs("underInvestigation", val);
          const opt = (lists.yesNo || []).find(
            (o) => String(o.id) === String(val),
          );
          setShowUnderInvNote(
            opt && (opt.value || "").toLowerCase().startsWith("yes"),
          );
        }}
      />
      {showUnderInvNote && (
        <EditField
          id="underInvestigationNote_vl"
          labelId="patient.project.underInvestigationComment"
          defaultMessage="Under Investigation Note"
          value={projectData.underInvestigationNote}
          onChange={(v) => updateProject("underInvestigationNote", v)}
        />
      )}
    </>
  );
};

// ─────────────────────────────────────────────────────────────────────────────
// RTN editable sub-form
// ─────────────────────────────────────────────────────────────────────────────

const RTNEditForm = ({
  formData,
  obs,
  projectData,
  lists,
  updateField,
  updateObs,
  updateProject,
}) => {
  const [showNationalityOther, setShowNationalityOther] = useState(
    !!obs.nationalityOther,
  );
  const [showUnderInvNote, setShowUnderInvNote] = useState(false);

  useEffect(() => {
    if (obs.nationalityOther) setShowNationalityOther(true);
    if (obs.underInvestigation) {
      const opt = (lists.yesNo || []).find(
        (o) => String(o.id) === String(obs.underInvestigation),
      );
      setShowUnderInvNote(
        opt && (opt.value || "").toLowerCase().startsWith("yes"),
      );
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <>
      <EditableDemographicsBlock
        formData={formData}
        updateField={updateField}
        lists={lists}
      />

      <EditField
        id="nameOfDoctor_rtn"
        labelId="patient.project.nameOfDoctor"
        defaultMessage="Name of Doctor"
        value={obs.nameOfDoctor}
        onChange={(v) => updateObs("nameOfDoctor", v)}
        required
      />
      <EditSelect
        id="centerCode_rtn"
        labelId="patient.project.hospitals"
        defaultMessage="Hospital"
        value={formData.centerCode}
        options={lists.rtnHospitals || []}
        onChange={(v) => updateField("centerCode", v)}
        required
      />
      <EditSelect
        id="service"
        labelId="patient.project.service"
        defaultMessage="Service"
        value={obs.service}
        options={lists.rtnServices || []}
        onChange={(v) => updateObs("service", v)}
        required
      />
      <EditSelect
        id="hospitalPatient"
        labelId="patient.project.hospitalPatient"
        defaultMessage="Hospitalized Patient"
        value={obs.hospitalPatient}
        options={lists.yesNo || []}
        onChange={(v) => updateObs("hospitalPatient", v)}
      />
      <EditSelect
        id="nationality_rtn"
        labelId="patient.project.nationality"
        defaultMessage="Nationality"
        value={obs.nationality}
        options={lists.nationalitiesFull || []}
        onChange={(val) => {
          updateObs("nationality", val);
          const opt = (lists.nationalitiesFull || []).find(
            (o) => String(o.id) === String(val),
          );
          setShowNationalityOther(
            opt &&
              ((opt.value || "").toLowerCase().includes("autre") ||
                (opt.value || "").toLowerCase().includes("other")),
          );
        }}
      />
      {showNationalityOther && (
        <EditField
          id="nationalityOther_rtn"
          labelId="patient.project.nationalityOther"
          defaultMessage="Other Nationality"
          value={obs.nationalityOther}
          onChange={(v) => updateObs("nationalityOther", v)}
        />
      )}
      <EditField
        id="reason"
        labelId="patient.project.serologyReason"
        defaultMessage="Reason"
        value={obs.reason}
        onChange={(v) => updateObs("reason", v)}
      />

      <SectionHeading
        labelId="patient.project.medicalHistory"
        defaultMessage="Medical History"
      />
      {(lists.rtnPriorDiseasesList || []).map((disease) => (
        <EditSelect
          key={disease.name}
          id={disease.name + "_rtn_prior"}
          labelId={disease.name}
          defaultMessage={disease.label}
          value={obs[disease.name]}
          options={lists.yesNoUnknownNaNotSpec || []}
          onChange={(v) => updateObs(disease.name, v)}
        />
      ))}

      <SectionHeading
        labelId="patient.project.physicalExam"
        defaultMessage="Physical Exam"
      />
      {(lists.rtnCurrentDiseasesList || []).map((disease) => (
        <EditSelect
          key={disease.name}
          id={disease.name + "_rtn_current"}
          labelId={disease.name}
          defaultMessage={disease.label}
          value={obs[disease.name]}
          options={lists.yesNoUnknownNaNotSpec || []}
          onChange={(v) => updateObs(disease.name, v)}
        />
      ))}

      <EditSelect
        id="underInvestigation_rtn"
        labelId="patient.project.underInvestigation"
        defaultMessage="Under Investigation"
        value={obs.underInvestigation}
        options={lists.yesNo || []}
        onChange={(val) => {
          updateObs("underInvestigation", val);
          const opt = (lists.yesNo || []).find(
            (o) => String(o.id) === String(val),
          );
          setShowUnderInvNote(
            opt && (opt.value || "").toLowerCase().startsWith("yes"),
          );
        }}
      />
      {showUnderInvNote && (
        <EditField
          id="underInvestigationNote_rtn"
          labelId="patient.project.underInvestigationComment"
          defaultMessage="Under Investigation Note"
          value={projectData.underInvestigationNote}
          onChange={(v) => updateProject("underInvestigationNote", v)}
        />
      )}
    </>
  );
};

// ─────────────────────────────────────────────────────────────────────────────
// Recency editable sub-form
// ─────────────────────────────────────────────────────────────────────────────

const RecencyEditForm = ({
  formData,
  obs,
  projectData,
  lists,
  updateField,
  updateObs,
  updateProject,
}) => {
  const [showPregnancy, setShowPregnancy] = useState(
    (formData.gender || "").toUpperCase() === "F",
  );

  useEffect(() => {
    setShowPregnancy((formData.gender || "").toUpperCase() === "F");
  }, [formData.gender]);

  return (
    <>
      <EditableDemographicsBlock
        formData={formData}
        updateField={(key, val) => {
          updateField(key, val);
          if (key === "gender") {
            setShowPregnancy(val.toUpperCase() === "F");
          }
        }}
        lists={lists}
      />

      <EditSelect
        id="ARVcenterName_recency"
        labelId="sample.entry.project.ARV.centerName"
        defaultMessage="Center Name"
        value={projectData.ARVcenterName}
        options={lists.arvOrgsByName || []}
        onChange={(v) => updateProject("ARVcenterName", v)}
        required
      />
      <EditSelect
        id="ARVcenterCode_recency"
        labelId="patient.project.centerCode"
        defaultMessage="Center Code"
        value={projectData.ARVcenterCode}
        options={lists.arvOrgs || []}
        onChange={(v) => updateProject("ARVcenterCode", v)}
      />
      <EditField
        id="receivedTimeForDisplay_recency"
        labelId="sample.entry.project.receivedTime"
        defaultMessage="Received Time"
        value={formData.receivedTimeForDisplay}
        onChange={(v) => updateField("receivedTimeForDisplay", v)}
      />
      <EditField
        id="interviewTime_recency"
        labelId="sample.entry.project.timeTaken"
        defaultMessage="Interview Time"
        value={formData.interviewTime}
        onChange={(v) => updateField("interviewTime", v)}
      />

      <SectionHeading
        labelId="patient.project.clinicalInfo"
        defaultMessage="Clinical Information"
      />
      <EditSelect
        id="hivStatus_recency"
        labelId="patient.project.hivType"
        defaultMessage="HIV Type"
        value={obs.hivStatus}
        options={lists.hivTypes || []}
        onChange={(v) => updateObs("hivStatus", v)}
        required
      />
      {showPregnancy && (
        <EditSelect
          id="vlPregnancy_recency"
          labelId="sample.project.vlPregnancy"
          defaultMessage="VL Pregnancy"
          value={obs.vlPregnancy}
          options={lists.yesNo || []}
          onChange={(v) => updateObs("vlPregnancy", v)}
        />
      )}
      {showPregnancy && (
        <EditSelect
          id="vlSuckle_recency"
          labelId="sample.project.vlSuckle"
          defaultMessage="Breastfeeding"
          value={obs.vlSuckle}
          options={lists.yesNo || []}
          onChange={(v) => updateObs("vlSuckle", v)}
        />
      )}
      <EditField
        id="nameOfDoctor_recency"
        labelId="patient.project.nameOfClinician"
        defaultMessage="Name of Clinician"
        value={obs.nameOfDoctor}
        onChange={(v) => updateObs("nameOfDoctor", v)}
      />
      <EditField
        id="nameOfSampler_recency"
        labelId="patient.project.nameOfSampler"
        defaultMessage="Name of Sampler"
        value={obs.nameOfSampler}
        onChange={(v) => updateObs("nameOfSampler", v)}
      />

      <SectionHeading
        labelId="sample.entry.project.sampleType"
        defaultMessage="Sample Type"
      />
      <EditCheckbox
        id="plasmaTaken"
        labelId="sample.entry.project.recency.plasma"
        defaultMessage="Plasma Taken"
        checked={projectData.plasmaTaken}
        onChange={(v) => updateProject("plasmaTaken", v)}
      />
      <EditCheckbox
        id="serumTaken"
        labelId="sample.entry.project.recency.serum"
        defaultMessage="Serum Taken"
        checked={projectData.serumTaken}
        onChange={(v) => updateProject("serumTaken", v)}
      />

      <SectionHeading
        labelId="sample.entry.project.tests"
        defaultMessage="Tests"
      />
      <EditCheckbox
        id="asanteTest"
        labelId="sample.entry.project.recency.asanteKit"
        defaultMessage="Asante Test"
        checked={projectData.asanteTest}
        onChange={(v) => updateProject("asanteTest", v)}
        required
      />
    </>
  );
};

// ─────────────────────────────────────────────────────────────────────────────
// Study type constants
// ─────────────────────────────────────────────────────────────────────────────

const STUDY_TYPES = [
  {
    id: "InitialARV_Id",
    labelId: "sample.entry.project.initialARV.title",
    defaultMessage: "Initial ARV",
  },
  {
    id: "FollowUpARV_Id",
    labelId: "sample.entry.project.followupARV.title",
    defaultMessage: "Follow-up ARV",
  },
  {
    id: "EID_Id",
    labelId: "sample.entry.project.EID.title",
    defaultMessage: "EID",
  },
  {
    id: "VL_Id",
    labelId: "sample.entry.project.VL.title",
    defaultMessage: "ARV - Viral Load",
  },
  {
    id: "RTN_Id",
    labelId: "sample.entry.project.RTN.title",
    defaultMessage: "RTN",
  },
  {
    id: "Recency_Id",
    labelId: "sample.entry.project.RT.title",
    defaultMessage: "Recency Testing",
  },
];

const PROJECT_FORM_NAME_TO_ID = {
  InitialARV_Id: "InitialARV_Id",
  FollowUpARV_Id: "FollowUpARV_Id",
  EID_Id: "EID_Id",
  VL_Id: "VL_Id",
  RTN_Id: "RTN_Id",
  Recency_Id: "Recency_Id",
};

// ─────────────────────────────────────────────────────────────────────────────
// Main exported component
// ─────────────────────────────────────────────────────────────────────────────

const PatientStudyEditForm = ({
  formRef,
  selectedPatient,
  formData: initialFormData,
  referenceLists,
  loading,
  onCancel,
}) => {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  const [selectedStudyType, setSelectedStudyType] = useState("");
  const [editFormData, setEditFormData] = useState(null);
  const [saving, setSaving] = useState(false);

  // Sync edit form data whenever new patient data loads
  useEffect(() => {
    if (initialFormData && Object.keys(initialFormData).length > 0) {
      setEditFormData(JSON.parse(JSON.stringify(initialFormData)));
      // Auto-select study type from loaded data
      const pfn =
        initialFormData.observations &&
        initialFormData.observations.projectFormName;
      if (pfn && PROJECT_FORM_NAME_TO_ID[pfn]) {
        setSelectedStudyType(PROJECT_FORM_NAME_TO_ID[pfn]);
      } else {
        setSelectedStudyType("");
      }
    } else {
      setEditFormData(null);
      setSelectedStudyType("");
    }
  }, [initialFormData]);

  // ── Field update helpers ───────────────────────────────────────────────────

  const updateField = (key, val) =>
    setEditFormData((prev) => ({ ...prev, [key]: val }));

  const updateObs = (key, val) =>
    setEditFormData((prev) => ({
      ...prev,
      observations: { ...(prev.observations || {}), [key]: val },
    }));

  const updateProject = (key, val) =>
    setEditFormData((prev) => ({
      ...prev,
      projectData: { ...(prev.projectData || {}), [key]: val },
    }));

  // ── Save handler ───────────────────────────────────────────────────────────

  const handleSave = () => {
    if (!editFormData) return;
    setSaving(true);

    const payload = {
      patientPK: editFormData.patientPK || "",
      samplePK: editFormData.samplePK || "",
      projectFormName: selectedStudyType,
      lastName: editFormData.lastName || "",
      firstName: editFormData.firstName || "",
      gender: editFormData.gender || "",
      birthDateForDisplay: editFormData.birthDateForDisplay || "",
      nationalId: editFormData.nationalId || "",
      subjectNumber: editFormData.subjectNumber || "",
      siteSubjectNumber: editFormData.siteSubjectNumber || "",
      labNo: editFormData.labNo || "",
      receivedDateForDisplay: editFormData.receivedDateForDisplay || "",
      receivedTimeForDisplay: editFormData.receivedTimeForDisplay || "",
      interviewDate: editFormData.interviewDate || "",
      interviewTime: editFormData.interviewTime || "",
      centerCode: editFormData.centerCode || "",
      centerName: editFormData.centerName || "",
      upidCode: editFormData.upidCode || "",
      observations: editFormData.observations || {},
      projectData: editFormData.projectData || {},
    };

    postToOpenElisServer(
      "/rest/patient-study-edit",
      JSON.stringify(payload),
      handleSaveResponse,
    );
  };

  const handleSaveResponse = (response) => {
    setSaving(false);
    if (response && response.success === true) {
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "patient.study.edit.save.success",
          defaultMessage: "Patient study record saved successfully.",
        }),
      });
      setNotificationVisible(true);
    } else {
      const errMsg =
        (response && response.error) ||
        intl.formatMessage({
          id: "patient.study.edit.save.error",
          defaultMessage: "Error saving patient study record.",
        });
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: errMsg,
      });
      setNotificationVisible(true);
    }
  };

  const handleCancel = () => {
    setEditFormData(null);
    setSelectedStudyType("");
    if (typeof onCancel === "function") onCancel();
  };

  // ── Loading / empty states ─────────────────────────────────────────────────

  if (loading) {
    return (
      <div ref={formRef} style={{ padding: "2rem" }}>
        <Loading description="Loading patient study data..." />
      </div>
    );
  }

  if (!editFormData) {
    return null;
  }

  const obs = editFormData.observations || {};
  const projectData = editFormData.projectData || {};
  const lists = referenceLists || {};

  const availableStudyTypes = editFormData.availableStudyTypes || null;

  // ── Sub-form renderer ──────────────────────────────────────────────────────

  const renderStudyForm = () => {
    const commonProps = {
      formData: editFormData,
      obs,
      projectData,
      lists,
      updateField,
      updateObs,
      updateProject,
    };

    switch (selectedStudyType) {
      case "InitialARV_Id":
        return <InitialARVEditForm {...commonProps} />;
      case "FollowUpARV_Id":
        return <FollowupARVEditForm {...commonProps} />;
      case "EID_Id":
        return <EIDEditForm {...commonProps} />;
      case "VL_Id":
        return <VLEditForm {...commonProps} />;
      case "RTN_Id":
        return <RTNEditForm {...commonProps} />;
      case "Recency_Id":
        return <RecencyEditForm {...commonProps} />;
      default:
        return (
          <Column lg={16} md={8} sm={4} style={{ marginTop: "1rem" }}>
            <p>
              <FormattedMessage
                id="patient.study.edit.select.type"
                defaultMessage="Please select a study type to edit the patient record."
              />
            </p>
          </Column>
        );
    }
  };

  // ── Render ─────────────────────────────────────────────────────────────────

  return (
    <div ref={formRef} style={{ marginTop: "2rem" }}>
      <Grid fullWidth>
        {/* Patient summary banner */}
        {selectedPatient && (
          <Column lg={16} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <div
              data-testid="patient-edit-banner"
              style={{
                padding: "1rem",
                backgroundColor: "#f4f4f4",
                borderLeft: "4px solid #0f62fe",
              }}
            >
              <strong>
                <FormattedMessage id="patient.label" defaultMessage="Patient" />
                :&nbsp;
              </strong>
              {selectedPatient.lastName || ""} {selectedPatient.firstName || ""}
              {selectedPatient.gender ? (
                <Tag type="blue" style={{ marginLeft: "0.5rem" }}>
                  {selectedPatient.gender}
                </Tag>
              ) : null}
              {selectedPatient.dateOfBirth ? (
                <span style={{ marginLeft: "0.75rem", color: "#525252" }}>
                  {intl.formatMessage({
                    id: "patient.birthDate",
                    defaultMessage: "DOB",
                  })}
                  : {selectedPatient.dateOfBirth}
                </span>
              ) : null}
            </div>
          </Column>
        )}

        {/* Study type selector */}
        <Column lg={6} md={4} sm={4} style={{ marginBottom: "1rem" }}>
          <Select
            id="studyTypeSelector"
            labelText={intl.formatMessage({
              id: "sample.entry.project.form",
              defaultMessage: "Study Form",
            })}
            value={selectedStudyType}
            onChange={(e) => setSelectedStudyType(e.target.value)}
          >
            <SelectItem value="" text="" />
            {STUDY_TYPES.filter(
              (st) =>
                !availableStudyTypes || availableStudyTypes.includes(st.id),
            ).map((st) => (
              <SelectItem
                key={st.id}
                value={st.id}
                text={intl.formatMessage({
                  id: st.labelId,
                  defaultMessage: st.defaultMessage,
                })}
              />
            ))}
          </Select>
        </Column>

        <Column lg={16} md={8} sm={4}>
          <hr />
        </Column>

        {/* The active editable study sub-form */}
        {renderStudyForm()}

        {/* Action buttons */}
        {selectedStudyType && (
          <Column
            lg={16}
            md={8}
            sm={4}
            style={{
              marginTop: "2rem",
              display: "flex",
              gap: "1rem",
              paddingBottom: "2rem",
            }}
          >
            <Button
              id="savePatientStudyButton"
              kind="primary"
              onClick={handleSave}
              disabled={saving}
            >
              {saving ? (
                <Loading small withOverlay={false} />
              ) : (
                <FormattedMessage
                  id="label.button.save"
                  defaultMessage="Save"
                />
              )}
            </Button>
            <Button
              id="cancelPatientStudyButton"
              kind="secondary"
              onClick={handleCancel}
              disabled={saving}
            >
              <FormattedMessage
                id="label.button.cancel"
                defaultMessage="Cancel"
              />
            </Button>
          </Column>
        )}
      </Grid>
    </div>
  );
};

export default PatientStudyEditForm;
