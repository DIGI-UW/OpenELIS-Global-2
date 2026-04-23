import React from "react";
import {
  Grid,
  Column,
  Select,
  SelectItem,
  TextInput,
  Checkbox,
  Loading,
  Tag,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";

// ─────────────────────────────────────────────────────────────────────────────
// Helper – render a read-only labelled field row
// ─────────────────────────────────────────────────────────────────────────────
const ReadOnlyField = ({ labelId, defaultMessage, value }) => {
  const intl = useIntl();
  return (
    <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
      <TextInput
        id={labelId}
        labelText={intl.formatMessage({
          id: labelId,
          defaultMessage: defaultMessage,
        })}
        value={value || ""}
        readOnly
      />
    </Column>
  );
};

// ─────────────────────────────────────────────────────────────────────────────
// Helper – render a read-only select field (shows the matching label)
// ─────────────────────────────────────────────────────────────────────────────
const ReadOnlySelect = ({ labelId, defaultMessage, value, options = [] }) => {
  const intl = useIntl();
  const matched = options.find((o) => String(o.id) === String(value));
  const displayValue = matched
    ? matched.value || matched.organizationName || matched.doubleName || ""
    : value || "";
  return (
    <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
      <TextInput
        id={labelId + "_display"}
        labelText={intl.formatMessage({
          id: labelId,
          defaultMessage: defaultMessage,
        })}
        value={displayValue}
        readOnly
      />
    </Column>
  );
};

// ─────────────────────────────────────────────────────────────────────────────
// Helper – read-only checkbox (shown as a disabled checked/unchecked box)
// ─────────────────────────────────────────────────────────────────────────────
const ReadOnlyCheckbox = ({ labelId, defaultMessage, checked }) => {
  const intl = useIntl();
  return (
    <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
      <Checkbox
        id={labelId + "_cb"}
        labelText={intl.formatMessage({
          id: labelId,
          defaultMessage: defaultMessage,
        })}
        checked={!!checked}
        disabled
      />
    </Column>
  );
};

// ─────────────────────────────────────────────────────────────────────────────
// Section heading
// ─────────────────────────────────────────────────────────────────────────────
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
// Common patient demographics block (shared by all 6 sub-forms)
// ─────────────────────────────────────────────────────────────────────────────
const DemographicsBlock = ({ formData }) => (
  <>
    <SectionHeading
      labelId="patient.project.patientInfo"
      defaultMessage="Patient Information"
    />
    <ReadOnlyField
      labelId="patient.project.patientFamilyName"
      defaultMessage="Family Name"
      value={formData.lastName}
    />
    <ReadOnlyField
      labelId="patient.project.patientFirstNames"
      defaultMessage="First Names"
      value={formData.firstName}
    />
    <ReadOnlyField
      labelId="patient.project.gender"
      defaultMessage="Gender"
      value={formData.gender}
    />
    <ReadOnlyField
      labelId="patient.project.dateOfBirth"
      defaultMessage="Date of Birth"
      value={formData.birthDateForDisplay}
    />
    <ReadOnlyField
      labelId="patient.subject.number"
      defaultMessage="Subject Number"
      value={formData.subjectNumber}
    />
    <ReadOnlyField
      labelId="patient.site.subject.number"
      defaultMessage="Site Subject Number"
      value={formData.siteSubjectNumber}
    />
    <ReadOnlyField
      labelId="patient.project.labNo"
      defaultMessage="Lab No."
      value={formData.labNo}
    />
    <ReadOnlyField
      labelId="sample.entry.project.receivedDate"
      defaultMessage="Received Date"
      value={formData.receivedDateForDisplay}
    />
    <ReadOnlyField
      labelId="patient.project.interviewDate"
      defaultMessage="Interview Date"
      value={formData.interviewDate}
    />
  </>
);

// ─────────────────────────────────────────────────────────────────────────────
// Initial ARV sub-form
// ─────────────────────────────────────────────────────────────────────────────
const InitialARVForm = ({ formData, obs, projectData, lists }) => (
  <>
    <DemographicsBlock formData={formData} />

    <ReadOnlySelect
      labelId="patient.project.centerName"
      defaultMessage="Center Name"
      value={formData.centerName}
      options={lists.arvOrgsByName || []}
    />
    <ReadOnlySelect
      labelId="patient.project.centerCode"
      defaultMessage="Center Code"
      value={formData.centerCode}
      options={lists.arvOrgs || []}
    />

    <SectionHeading
      labelId="patient.project.medicalHistory"
      defaultMessage="Medical History"
    />
    <ReadOnlySelect
      labelId="patient.project.hivStatus"
      defaultMessage="HIV Status"
      value={obs.hivStatus}
      options={lists.hivStatuses || []}
    />
    <ReadOnlySelect
      labelId="patient.project.educationLevel"
      defaultMessage="Education Level"
      value={obs.educationLevel}
      options={lists.educationLevels || []}
    />
    <ReadOnlySelect
      labelId="patient.project.maritalStatus"
      defaultMessage="Marital Status"
      value={obs.maritalStatus}
      options={lists.maritalStatuses || []}
    />
    <ReadOnlySelect
      labelId="patient.project.nationality"
      defaultMessage="Nationality"
      value={obs.nationality}
      options={lists.nationalities || []}
    />
    <ReadOnlyField
      labelId="patient.project.nationalityOther"
      defaultMessage="Other Nationality"
      value={obs.nationalityOther}
    />
    <ReadOnlyField
      labelId="patient.project.legalResidence"
      defaultMessage="Legal Residence"
      value={obs.legalResidence}
    />
    <ReadOnlyField
      labelId="patient.project.nameOfDoctor"
      defaultMessage="Name of Doctor"
      value={obs.nameOfDoctor}
    />

    <SectionHeading
      labelId="patient.project.antecedents"
      defaultMessage="Antecedents"
    />
    <ReadOnlySelect
      labelId="patient.project.antecedents"
      defaultMessage="Prior Diseases"
      value={obs.anyPriorDiseases}
      options={lists.yesNo || []}
    />
    {(lists.priorDiseasesList || []).map((disease) => (
      <ReadOnlySelect
        key={disease.name}
        labelId={disease.name}
        defaultMessage={disease.label}
        value={obs[disease.name]}
        options={lists.yesNo || []}
      />
    ))}
    <ReadOnlySelect
      labelId="patient.project.arvProphylaxisBenefit"
      defaultMessage="ARV Prophylaxis Benefit"
      value={obs.arvProphylaxisBenefit}
      options={lists.yesNo || []}
    />
    <ReadOnlySelect
      labelId="patient.project.arvProphylaxis"
      defaultMessage="ARV Prophylaxis"
      value={obs.arvProphylaxis}
      options={lists.arvProphylaxis || []}
    />
    <ReadOnlySelect
      labelId="patient.project.currentARVTreatment"
      defaultMessage="Current ARV Treatment"
      value={obs.currentARVTreatment}
      options={lists.yesNo || []}
    />
    <ReadOnlySelect
      labelId="patient.project.priorARVTreatment"
      defaultMessage="Prior ARV Treatment"
      value={obs.priorARVTreatment}
      options={lists.yesNoUnknownNaNotSpec || []}
    />
    {(obs.priorARVTreatmentINNsList || []).filter(Boolean).map((inn, i) => (
      <ReadOnlyField
        key={i}
        labelId="patient.project.priorARVInn"
        defaultMessage="Prior ARV INN"
        value={inn}
      />
    ))}
    <ReadOnlySelect
      labelId="patient.project.priorDiseases"
      defaultMessage="Prior Diseases (Other)"
      value={obs.priorDiseases}
      options={lists.yesNo || []}
    />
    <ReadOnlyField
      labelId="patient.project.specify"
      defaultMessage="Specify"
      value={obs.priorDiseasesValue}
    />
    <ReadOnlySelect
      labelId="patient.project.cotrimoxazoleTreatment"
      defaultMessage="Cotrimoxazole Treatment"
      value={obs.cotrimoxazoleTreatment}
      options={lists.yesNo || []}
    />
    <ReadOnlySelect
      labelId="patient.project.aidsStage"
      defaultMessage="AIDS Stage"
      value={obs.aidsStage}
      options={lists.aidsStages || []}
    />
    <ReadOnlySelect
      labelId="patient.project.anyCurrentDiseases"
      defaultMessage="Any Current Diseases"
      value={obs.anyCurrentDiseases}
      options={lists.yesNo || []}
    />
    {(lists.currentDiseasesList || []).map((disease) => (
      <ReadOnlySelect
        key={disease.name}
        labelId={disease.name}
        defaultMessage={disease.label}
        value={obs[disease.name]}
        options={lists.yesNo || []}
      />
    ))}
    <ReadOnlySelect
      labelId="patient.project.currentDiseases"
      defaultMessage="Current Diseases (Other)"
      value={obs.currentDiseases}
      options={lists.yesNo || []}
    />
    <ReadOnlyField
      labelId="patient.project.specify"
      defaultMessage="Specify"
      value={obs.currentDiseasesValue}
    />
    <ReadOnlySelect
      labelId="patient.project.currentOITreatment"
      defaultMessage="Current OI Treatment"
      value={obs.currentOITreatment}
      options={lists.yesNoUnknownNaNotSpec || []}
    />
    <ReadOnlyField
      labelId="patient.project.patientWeight"
      defaultMessage="Patient Weight"
      value={obs.patientWeight}
    />
    <ReadOnlyField
      labelId="patient.project.karnofskyScore"
      defaultMessage="Karnofsky Score"
      value={obs.karnofskyScore}
    />
    <ReadOnlyField
      labelId="patient.age"
      defaultMessage="Age (years)"
      value={formData.age}
    />
    <ReadOnlySelect
      labelId="patient.project.underInvestigation"
      defaultMessage="Under Investigation"
      value={obs.underInvestigation}
      options={lists.yesNo || []}
    />
    <ReadOnlyField
      labelId="patient.project.underInvestigationComment"
      defaultMessage="Under Investigation Note"
      value={projectData.underInvestigationNote}
    />
  </>
);

// ─────────────────────────────────────────────────────────────────────────────
// Follow-up ARV sub-form
// ─────────────────────────────────────────────────────────────────────────────
const FollowupARVForm = ({ formData, obs, projectData, lists }) => (
  <>
    <DemographicsBlock formData={formData} />

    <ReadOnlySelect
      labelId="patient.project.centerName"
      defaultMessage="Center Name"
      value={formData.centerName}
      options={lists.arvOrgsByName || []}
    />
    <ReadOnlySelect
      labelId="patient.project.centerCode"
      defaultMessage="Center Code"
      value={formData.centerCode}
      options={lists.arvOrgs || []}
    />

    <SectionHeading
      labelId="patient.project.clinicalInfo"
      defaultMessage="Clinical Information"
    />
    <ReadOnlyField
      labelId="patient.project.patientWeight"
      defaultMessage="Patient Weight"
      value={obs.patientWeight}
    />
    <ReadOnlyField
      labelId="patient.project.karnofskyScore"
      defaultMessage="Karnofsky Score"
      value={obs.karnofskyScore}
    />
    <ReadOnlyField
      labelId="patient.age"
      defaultMessage="Age (years)"
      value={formData.age}
    />
    <ReadOnlySelect
      labelId="patient.project.hivStatus"
      defaultMessage="HIV Status"
      value={obs.hivStatus}
      options={lists.hivStatuses || []}
    />
    <ReadOnlyField
      labelId="patient.project.cd4Count"
      defaultMessage="CD4 Count"
      value={obs.cd4Count}
    />
    <ReadOnlyField
      labelId="patient.project.cd4Percent"
      defaultMessage="CD4 Percent"
      value={obs.cd4Percent}
    />
    <ReadOnlyField
      labelId="patient.project.priorCd4Date"
      defaultMessage="Prior CD4 Date"
      value={obs.priorCd4Date}
    />
    <ReadOnlyField
      labelId="patient.project.nameOfDoctor"
      defaultMessage="Name of Doctor"
      value={obs.nameOfDoctor}
    />

    <SectionHeading
      labelId="patient.project.anyDiseasesSinceLast"
      defaultMessage="Current Diseases"
    />
    <ReadOnlySelect
      labelId="patient.project.anyDiseasesSinceLast"
      defaultMessage="Any Diseases Since Last Visit"
      value={obs.anyCurrentDiseases}
      options={lists.yesNo || []}
    />
    {(lists.currentDiseasesList || []).map((disease) => (
      <ReadOnlySelect
        key={disease.name}
        labelId={disease.name}
        defaultMessage={disease.label}
        value={obs[disease.name]}
        options={lists.yesNo || []}
      />
    ))}

    <SectionHeading
      labelId="patient.project.arvTreatment"
      defaultMessage="ARV Treatment"
    />
    <ReadOnlySelect
      labelId="patient.project.interruptedARVTreatment"
      defaultMessage="Interrupted ARV Treatment"
      value={obs.interruptedARVTreatment}
      options={lists.yesNoNa || []}
    />
    <ReadOnlySelect
      labelId="patient.project.arvTreatmentChange"
      defaultMessage="ARV Treatment Change"
      value={obs.arvTreatmentChange}
      options={lists.yesNoNa || []}
    />
    <ReadOnlySelect
      labelId="patient.project.arvTreatmentNew"
      defaultMessage="New ARV Treatment"
      value={obs.arvTreatmentNew}
      options={lists.yesNoNa || []}
    />
    <ReadOnlySelect
      labelId="patient.project.arvTreatmentRegime"
      defaultMessage="ARV Treatment Regime"
      value={obs.arvTreatmentRegime}
      options={lists.arvRegimes || []}
    />
    <ReadOnlyField
      labelId="patient.project.arvTreatmentInitDate"
      defaultMessage="ARV Treatment Init Date"
      value={obs.arvTreatmentInitDate}
    />
    {(obs.futureARVTreatmentINNsList || []).filter(Boolean).map((inn, i) => (
      <ReadOnlyField
        key={i}
        labelId="patient.project.prescribedARVTreatmentINNs"
        defaultMessage="Prescribed ARV INN"
        value={inn}
      />
    ))}
    <ReadOnlySelect
      labelId="patient.project.treatmentAnyAdverseEffects"
      defaultMessage="Any Adverse Effects"
      value={obs.arvTreatmentAnyAdverseEffects}
      options={lists.yesNo || []}
    />
    <ReadOnlySelect
      labelId="patient.project.cotrimoxazoleTreatment"
      defaultMessage="Cotrimoxazole Treatment"
      value={obs.cotrimoxazoleTreatment}
      options={lists.yesNoNa || []}
    />
    <ReadOnlySelect
      labelId="patient.project.anySecondaryTreatment"
      defaultMessage="Any Secondary Treatment"
      value={obs.anySecondaryTreatment}
      options={lists.yesNo || []}
    />
    <ReadOnlyField
      labelId="patient.project.secondaryTreatment"
      defaultMessage="Secondary Treatment"
      value={obs.secondaryTreatment}
    />
    <ReadOnlyField
      labelId="patient.project.clinicVisits"
      defaultMessage="Clinic Visits"
      value={obs.clinicVisits}
    />
    <ReadOnlySelect
      labelId="patient.project.priorARVTreatment"
      defaultMessage="Prior ARV Treatment"
      value={obs.priorARVTreatment}
      options={lists.yesNoUnknownNaNotSpec || []}
    />
    {(obs.priorARVTreatmentINNsList || []).filter(Boolean).map((inn, i) => (
      <ReadOnlyField
        key={i}
        labelId="patient.project.priorARVInn"
        defaultMessage="Prior ARV INN"
        value={inn}
      />
    ))}
    <ReadOnlySelect
      labelId="patient.project.antiTbTreatment"
      defaultMessage="Anti-TB Treatment"
      value={obs.antiTbTreatment}
      options={lists.yesNo || []}
    />
    <ReadOnlySelect
      labelId="patient.project.cotrimoxazoleTreatAnyAdvEff"
      defaultMessage="Cotrimoxazole Any Adverse Effects"
      value={obs.cotrimoxazoleTreatmentAnyAdverseEffects}
      options={lists.yesNoNa || []}
    />
    <ReadOnlySelect
      labelId="patient.project.currentDiseases"
      defaultMessage="Current Diseases (Other)"
      value={obs.currentDiseases}
      options={lists.yesNo || []}
    />
    <ReadOnlyField
      labelId="patient.project.specify"
      defaultMessage="Specify"
      value={obs.currentDiseasesValue}
    />
    <ReadOnlySelect
      labelId="patient.project.underInvestigation"
      defaultMessage="Under Investigation"
      value={obs.underInvestigation}
      options={lists.yesNo || []}
    />
    <ReadOnlyField
      labelId="patient.project.underInvestigationComment"
      defaultMessage="Under Investigation Note"
      value={projectData.underInvestigationNote}
    />
  </>
);

// ─────────────────────────────────────────────────────────────────────────────
// EID sub-form
// ─────────────────────────────────────────────────────────────────────────────
const EIDForm = ({ formData, obs, projectData, lists }) => (
  <>
    <DemographicsBlock formData={formData} />

    <ReadOnlySelect
      labelId="sample.entry.project.EID.siteName"
      defaultMessage="Site Name"
      value={projectData.EIDsiteName}
      options={lists.eidOrgsByName || []}
    />
    <ReadOnlySelect
      labelId="sample.entry.project.EID.siteCode"
      defaultMessage="Site Code"
      value={projectData.EIDsiteCode}
      options={lists.eidOrgs || []}
    />
    <ReadOnlyField
      labelId="patient.project.nameOfRequestor"
      defaultMessage="Name of Requester"
      value={obs.nameOfRequestor}
    />
    <ReadOnlyField
      labelId="patient.project.nameOfSampler"
      defaultMessage="Name of Sampler"
      value={obs.nameOfSampler}
    />
    <ReadOnlyField
      labelId="sample.entry.project.receivedTime"
      defaultMessage="Received Time"
      value={formData.receivedTimeForDisplay}
    />
    <ReadOnlyField
      labelId="sample.entry.project.timeTaken"
      defaultMessage="Interview Time"
      value={formData.interviewTime}
    />

    <SectionHeading
      labelId="patient.project.eidInfo"
      defaultMessage="EID Information"
    />
    <ReadOnlySelect
      labelId="patient.project.eidTypeOfClinic"
      defaultMessage="Type of Clinic"
      value={obs.eidTypeOfClinic}
      options={lists.eidTypeOfClinic || []}
    />
    <ReadOnlyField
      labelId="patient.project.eidTypeOfClinicOther"
      defaultMessage="Type of Clinic (Other)"
      value={obs.eidTypeOfClinicOther}
    />
    <ReadOnlySelect
      labelId="patient.project.eidHowChildFed"
      defaultMessage="How Child Fed"
      value={obs.eidHowChildFed}
      options={lists.eidHowChildFed || []}
    />
    <ReadOnlySelect
      labelId="patient.project.eidStoppedBreastfeeding"
      defaultMessage="Stopped Breastfeeding"
      value={obs.eidStoppedBreastfeeding}
      options={lists.eidStoppedBreastfeeding || []}
    />
    <ReadOnlySelect
      labelId="patient.project.eidInfantSymptomatic"
      defaultMessage="Infant Symptomatic"
      value={obs.eidInfantSymptomatic}
      options={lists.yesNo || []}
    />
    <ReadOnlySelect
      labelId="patient.project.eidMothersStatus"
      defaultMessage="Mother's HIV Status"
      value={obs.eidMothersHIVStatus}
      options={lists.eidMothersHivStatus || []}
    />
    <ReadOnlySelect
      labelId="patient.project.eidMothersARV"
      defaultMessage="Mother's ARV"
      value={obs.eidMothersARV}
      options={lists.eidMothersArvTreatment || []}
    />
    <ReadOnlySelect
      labelId="patient.project.eidInfantProphy"
      defaultMessage="Infant ARV"
      value={obs.eidInfantsARV}
      options={lists.eidInfantProphylaxisArv || []}
    />
    <ReadOnlySelect
      labelId="patient.project.eidInfantCotrimoxazole"
      defaultMessage="Infant Cotrimoxazole"
      value={obs.eidInfantCotrimoxazole}
      options={lists.yesNoUnknown || []}
    />
    <ReadOnlySelect
      labelId="patient.project.eidBenefitPTME"
      defaultMessage="Infant Benefit PTME"
      value={obs.eidInfantPTME}
      options={lists.yesNo || []}
    />

    <SectionHeading
      labelId="sample.entry.project.EID.whichPCR"
      defaultMessage="PCR"
    />
    <ReadOnlySelect
      labelId="patient.project.eidWhichPCR"
      defaultMessage="Which PCR"
      value={obs.whichPCR}
      options={lists.eidWhichPcr || []}
    />
    <ReadOnlySelect
      labelId="sample.entry.project.EID.reasonForPCRTest"
      defaultMessage="Reason for Second PCR"
      value={obs.reasonForSecondPCRTest}
      options={lists.eidSecondPcrReason || []}
    />

    <SectionHeading
      labelId="sample.entry.project.sampleType"
      defaultMessage="Sample Type"
    />
    <ReadOnlyCheckbox
      labelId="sample.entry.project.EID.DBS"
      defaultMessage="DBS Taken"
      checked={projectData.dbsTaken}
    />
    <ReadOnlySelect
      labelId="patient.project.underInvestigation"
      defaultMessage="Under Investigation"
      value={obs.underInvestigation}
      options={lists.yesNo || []}
    />
    <ReadOnlyField
      labelId="patient.project.underInvestigationComment"
      defaultMessage="Under Investigation Note"
      value={projectData.underInvestigationNote}
    />
  </>
);

// ─────────────────────────────────────────────────────────────────────────────
// Viral Load sub-form
// ─────────────────────────────────────────────────────────────────────────────
const VLForm = ({ formData, obs, projectData, lists }) => (
  <>
    <DemographicsBlock formData={formData} />

    <ReadOnlyField
      labelId="patient.upid.code"
      defaultMessage="UPID Code"
      value={formData.upidCode}
    />
    <ReadOnlyField
      labelId="sample.entry.project.receivedTime"
      defaultMessage="Received Time"
      value={formData.receivedTimeForDisplay}
    />
    <ReadOnlyField
      labelId="sample.entry.project.timeTaken"
      defaultMessage="Interview Time"
      value={formData.interviewTime}
    />
    <ReadOnlySelect
      labelId="sample.entry.project.ARV.centerName"
      defaultMessage="Center Name"
      value={projectData.ARVcenterName}
      options={lists.arvOrgsByName || []}
    />
    <ReadOnlySelect
      labelId="patient.project.centerCode"
      defaultMessage="Center Code"
      value={projectData.ARVcenterCode}
      options={lists.arvOrgs || []}
    />
    <ReadOnlyField
      labelId="patient.project.nameOfClinician"
      defaultMessage="Name of Clinician"
      value={obs.nameOfDoctor}
    />
    <ReadOnlyField
      labelId="patient.project.nameOfSampler"
      defaultMessage="Name of Sampler"
      value={obs.nameOfSampler}
    />

    <SectionHeading
      labelId="patient.project.clinicalInfo"
      defaultMessage="Clinical Information"
    />
    <ReadOnlySelect
      labelId="sample.project.vlPregnancy"
      defaultMessage="VL Pregnancy"
      value={obs.vlPregnancy}
      options={lists.yesNo || []}
    />
    <ReadOnlySelect
      labelId="sample.project.vlSuckle"
      defaultMessage="Breastfeeding"
      value={obs.vlSuckle}
      options={lists.yesNo || []}
    />
    <ReadOnlySelect
      labelId="patient.project.hivType"
      defaultMessage="HIV Type"
      value={obs.hivStatus}
      options={lists.hivTypes || []}
    />
    <ReadOnlySelect
      labelId="patient.project.currentARVTreatment"
      defaultMessage="Current ARV Treatment"
      value={obs.currentARVTreatment}
      options={lists.yesNo || []}
    />
    <ReadOnlyField
      labelId="patient.project.arvTreatmentInitDate"
      defaultMessage="ARV Treatment Init Date"
      value={obs.arvTreatmentInitDate}
    />
    <ReadOnlySelect
      labelId="patient.project.arvTreatmentRegime"
      defaultMessage="ARV Treatment Regime"
      value={obs.arvTreatmentRegime}
      options={lists.arvRegimes || []}
    />

    <SectionHeading
      labelId="sample.entry.project.vl.reason"
      defaultMessage="Reason for VL"
    />
    <ReadOnlySelect
      labelId="sample.entry.project.vl.reason"
      defaultMessage="Reason for VL Request"
      value={obs.vlReasonForRequest}
      options={lists.arvReasonForVlDemand || []}
    />
    <ReadOnlyField
      labelId="patient.project.specify"
      defaultMessage="Other Reason"
      value={obs.vlOtherReasonForRequest}
    />

    <SectionHeading
      labelId="sample.project.initialCD4"
      defaultMessage="Initial CD4"
    />
    <ReadOnlyField
      labelId="sample.project.cd4Count"
      defaultMessage="Initial CD4 Count"
      value={obs.initcd4Count}
    />
    <ReadOnlyField
      labelId="sample.project.cd4Percent"
      defaultMessage="Initial CD4 Percent"
      value={obs.initcd4Percent}
    />
    <ReadOnlyField
      labelId="sample.project.Cd4Date"
      defaultMessage="Initial CD4 Date"
      value={obs.initcd4Date}
    />

    <SectionHeading
      labelId="sample.project.demandCD4"
      defaultMessage="Demand CD4"
    />
    <ReadOnlyField
      labelId="sample.project.cd4Count"
      defaultMessage="Demand CD4 Count"
      value={obs.demandcd4Count}
    />
    <ReadOnlyField
      labelId="sample.project.cd4Percent"
      defaultMessage="Demand CD4 Percent"
      value={obs.demandcd4Percent}
    />
    <ReadOnlyField
      labelId="sample.project.Cd4Date"
      defaultMessage="Demand CD4 Date"
      value={obs.demandcd4Date}
    />

    <SectionHeading
      labelId="patient.project.priorVL"
      defaultMessage="Prior VL"
    />
    <ReadOnlySelect
      labelId="patient.project.vlBenefit"
      defaultMessage="VL Benefit"
      value={obs.vlBenefit}
      options={lists.yesNo || []}
    />
    <ReadOnlyField
      labelId="patient.project.priorVLLab"
      defaultMessage="Prior VL Lab"
      value={obs.priorVLLab}
    />
    <ReadOnlyField
      labelId="patient.project.priorVLValue"
      defaultMessage="Prior VL Value"
      value={obs.priorVLValue}
    />
    <ReadOnlyField
      labelId="patient.project.priorVLDate"
      defaultMessage="Prior VL Date"
      value={obs.priorVLDate}
    />

    <SectionHeading
      labelId="sample.entry.project.sampleType"
      defaultMessage="Sample Type"
    />
    <ReadOnlyCheckbox
      labelId="sample.entry.project.ARV.edtaTubeTaken"
      defaultMessage="EDTA Tube Taken"
      checked={projectData.edtaTubeTaken}
    />
    <ReadOnlyCheckbox
      labelId="sample.entry.project.ARV.dbsvlTaken"
      defaultMessage="DBS Taken"
      checked={projectData.dbsvlTaken}
    />
    <ReadOnlyCheckbox
      labelId="sample.entry.project.ARV.pscvlTaken"
      defaultMessage="PSC Taken"
      checked={projectData.pscvlTaken}
    />

    <SectionHeading
      labelId="sample.entry.project.tests"
      defaultMessage="Tests"
    />
    <ReadOnlyCheckbox
      labelId="sample.entry.project.ARV.viralLoadTest"
      defaultMessage="Viral Load Test"
      checked={projectData.viralLoadTest}
    />
    <ReadOnlySelect
      labelId="patient.project.underInvestigation"
      defaultMessage="Under Investigation"
      value={obs.underInvestigation}
      options={lists.yesNo || []}
    />
    <ReadOnlyField
      labelId="patient.project.underInvestigationComment"
      defaultMessage="Under Investigation Note"
      value={projectData.underInvestigationNote}
    />
  </>
);

// ─────────────────────────────────────────────────────────────────────────────
// RTN sub-form
// ─────────────────────────────────────────────────────────────────────────────
const RTNForm = ({ formData, obs, projectData, lists }) => (
  <>
    <DemographicsBlock formData={formData} />

    <ReadOnlyField
      labelId="patient.project.nameOfDoctor"
      defaultMessage="Name of Doctor"
      value={obs.nameOfDoctor}
    />
    <ReadOnlySelect
      labelId="patient.project.hospitals"
      defaultMessage="Hospital"
      value={obs.hospitalPatient}
      options={lists.rtnHospitals || []}
    />
    <ReadOnlySelect
      labelId="patient.project.service"
      defaultMessage="Service"
      value={obs.service}
      options={lists.rtnServices || []}
    />
    <ReadOnlySelect
      labelId="patient.project.hospitalPatient"
      defaultMessage="Hospitalized Patient"
      value={obs.hospitalPatient}
      options={lists.yesNo || []}
    />
    <ReadOnlyField
      labelId="patient.project.nameOfDoctor"
      defaultMessage="Name of Doctor"
      value={obs.nameOfDoctor}
    />
    <ReadOnlySelect
      labelId="patient.project.nationality"
      defaultMessage="Nationality"
      value={obs.nationality}
      options={lists.nationalitiesFull || []}
    />
    <ReadOnlyField
      labelId="patient.project.nationalityOther"
      defaultMessage="Other Nationality"
      value={obs.nationalityOther}
    />
    <ReadOnlyField
      labelId="patient.project.serologyReason"
      defaultMessage="Reason"
      value={obs.reason}
    />

    <SectionHeading
      labelId="patient.project.medicalHistory"
      defaultMessage="Medical History"
    />
    {(lists.rtnPriorDiseasesList || []).map((disease) => (
      <ReadOnlySelect
        key={disease.name}
        labelId={disease.name}
        defaultMessage={disease.label}
        value={obs[disease.name]}
        options={lists.yesNoUnknownNaNotSpec || []}
      />
    ))}

    <SectionHeading
      labelId="patient.project.physicalExam"
      defaultMessage="Physical Exam"
    />
    {(lists.rtnCurrentDiseasesList || []).map((disease) => (
      <ReadOnlySelect
        key={disease.name}
        labelId={disease.name}
        defaultMessage={disease.label}
        value={obs[disease.name]}
        options={lists.yesNoUnknownNaNotSpec || []}
      />
    ))}

    <ReadOnlySelect
      labelId="patient.project.underInvestigation"
      defaultMessage="Under Investigation"
      value={obs.underInvestigation}
      options={lists.yesNo || []}
    />
    <ReadOnlyField
      labelId="patient.project.underInvestigationComment"
      defaultMessage="Under Investigation Note"
      value={projectData.underInvestigationNote}
    />
  </>
);

// ─────────────────────────────────────────────────────────────────────────────
// Recency (RT) sub-form
// ─────────────────────────────────────────────────────────────────────────────
const RecencyForm = ({ formData, obs, projectData, lists }) => (
  <>
    <DemographicsBlock formData={formData} />

    <ReadOnlySelect
      labelId="sample.entry.project.ARV.centerName"
      defaultMessage="Center Name"
      value={projectData.ARVcenterName}
      options={lists.arvOrgsByName || []}
    />
    <ReadOnlySelect
      labelId="patient.project.centerCode"
      defaultMessage="Center Code"
      value={projectData.ARVcenterCode}
      options={lists.arvOrgs || []}
    />
    <ReadOnlyField
      labelId="sample.entry.project.receivedTime"
      defaultMessage="Received Time"
      value={formData.receivedTimeForDisplay}
    />
    <ReadOnlyField
      labelId="sample.entry.project.timeTaken"
      defaultMessage="Interview Time"
      value={formData.interviewTime}
    />

    <SectionHeading
      labelId="patient.project.clinicalInfo"
      defaultMessage="Clinical Information"
    />
    <ReadOnlySelect
      labelId="patient.project.hivType"
      defaultMessage="HIV Type"
      value={obs.hivStatus}
      options={lists.hivTypes || []}
    />
    <ReadOnlySelect
      labelId="sample.project.vlPregnancy"
      defaultMessage="VL Pregnancy"
      value={obs.vlPregnancy}
      options={lists.yesNo || []}
    />
    <ReadOnlySelect
      labelId="sample.project.vlSuckle"
      defaultMessage="Breastfeeding"
      value={obs.vlSuckle}
      options={lists.yesNo || []}
    />
    <ReadOnlyField
      labelId="patient.project.nameOfClinician"
      defaultMessage="Name of Clinician"
      value={obs.nameOfDoctor}
    />
    <ReadOnlyField
      labelId="patient.project.nameOfSampler"
      defaultMessage="Name of Sampler"
      value={obs.nameOfSampler}
    />

    <SectionHeading
      labelId="sample.entry.project.sampleType"
      defaultMessage="Sample Type"
    />
    <ReadOnlyCheckbox
      labelId="sample.entry.project.recency.plasma"
      defaultMessage="Plasma Taken"
      checked={projectData.plasmaTaken}
    />
    <ReadOnlyCheckbox
      labelId="sample.entry.project.recency.serum"
      defaultMessage="Serum Taken"
      checked={projectData.serumTaken}
    />

    <SectionHeading
      labelId="sample.entry.project.tests"
      defaultMessage="Tests"
    />
    <ReadOnlyCheckbox
      labelId="sample.entry.project.recency.asanteKit"
      defaultMessage="Asante Test"
      checked={projectData.asanteTest}
    />
  </>
);

// ─────────────────────────────────────────────────────────────────────────────
// Study type selector and form switcher
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

// Map the stored projectFormName to the study type selector id
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
const PatientStudyForm = ({
  formRef,
  selectedPatient,
  formData,
  referenceLists,
  loading,
}) => {
  const intl = useIntl();
  const [selectedStudyType, setSelectedStudyType] = React.useState("");

  // Auto-select the study type from the loaded projectFormName
  React.useEffect(() => {
    if (formData && formData.observations) {
      const pfn = formData.observations.projectFormName;
      if (pfn && PROJECT_FORM_NAME_TO_ID[pfn]) {
        setSelectedStudyType(PROJECT_FORM_NAME_TO_ID[pfn]);
      } else {
        setSelectedStudyType("");
      }
    }
  }, [formData]);

  // Derive available study types from the list returned by the backend
  const availableStudyTypes = React.useMemo(() => {
    if (!formData || !formData.availableStudyTypes) return null;
    return formData.availableStudyTypes;
  }, [formData]);

  if (loading) {
    return (
      <div ref={formRef} style={{ padding: "2rem" }}>
        <Loading description="Loading patient study data..." />
      </div>
    );
  }

  if (!formData) {
    return null;
  }

  const obs = formData.observations || {};
  const projectData = formData.projectData || {};
  const lists = referenceLists || {};

  const renderStudyForm = () => {
    switch (selectedStudyType) {
      case "InitialARV_Id":
        return (
          <InitialARVForm
            formData={formData}
            obs={obs}
            projectData={projectData}
            lists={lists}
          />
        );
      case "FollowUpARV_Id":
        return (
          <FollowupARVForm
            formData={formData}
            obs={obs}
            projectData={projectData}
            lists={lists}
          />
        );
      case "EID_Id":
        return (
          <EIDForm
            formData={formData}
            obs={obs}
            projectData={projectData}
            lists={lists}
          />
        );
      case "VL_Id":
        return (
          <VLForm
            formData={formData}
            obs={obs}
            projectData={projectData}
            lists={lists}
          />
        );
      case "RTN_Id":
        return (
          <RTNForm
            formData={formData}
            obs={obs}
            projectData={projectData}
            lists={lists}
          />
        );
      case "Recency_Id":
        return (
          <RecencyForm
            formData={formData}
            obs={obs}
            projectData={projectData}
            lists={lists}
          />
        );
      default:
        return (
          <Column lg={16} md={8} sm={4} style={{ marginTop: "1rem" }}>
            <p>
              <FormattedMessage
                id="patient.study.view.select.type"
                defaultMessage="Please select a study type to view the patient record."
              />
            </p>
          </Column>
        );
    }
  };

  return (
    <div ref={formRef} style={{ marginTop: "2rem" }}>
      <Grid fullWidth>
        {/* Patient summary banner */}
        {selectedPatient && (
          <Column lg={16} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <div
              data-testid="patient-banner"
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

        {/* Study type selector — read-only once patient data is loaded (mirrors JSP readonly mode) */}
        <Column lg={6} md={4} sm={4} style={{ marginBottom: "1rem" }}>
          <Select
            id="studyTypeSelector"
            labelText={intl.formatMessage({
              id: "sample.entry.project.form",
              defaultMessage: "Study Form",
            })}
            value={selectedStudyType}
            disabled={false}
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

        {/* The active study sub-form */}
        {renderStudyForm()}
      </Grid>
    </div>
  );
};

export default PatientStudyForm;
