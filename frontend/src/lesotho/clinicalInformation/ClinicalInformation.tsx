// LESOTHO: Clinical Information section of Add Order (above Attachments).
//
// One section that holds the clinical fields of every Lesotho MOH request
// form. These details change from one request to the next (viral load, CD4,
// pregnancy, PCR reason...), so they belong to the order; the patient form
// keeps only details that do not change, such as the passport number.
//
// Fields shared by all forms are always shown. A form's own fields appear only
// when the order has a test linked to that form (testForms.ts), so there is
// nothing to tick; follow-up fields appear only when the answer they depend on
// is given.
//
// Add Order keeps the values in its own state and, after the order is saved,
// posts them for the order's lab number (clinicalInformationApi.ts).
import React from "react";
import { useIntl } from "react-intl";
import {
  Checkbox,
  Column,
  FormGroup,
  Grid,
  RadioButton,
  RadioButtonGroup,
  Select,
  SelectItem,
  TextArea,
  TextInput,
} from "@carbon/react";
import CustomDatePicker from "../../components/common/CustomDatePicker";
import "./ClinicalInformation.css";

export const REQUEST_FORMS = [
  "VIRAL_LOAD",
  "DNA_PCR",
  "TB",
  "CERVICAL_CYTOLOGY",
  "BLOOD_TRANSFUSION",
] as const;
export type RequestForm = (typeof REQUEST_FORMS)[number];

const CERVIX_STATES = [
  "NORMAL",
  "ERODED",
  "SUSPICIOUS",
  "FIRST_SMEAR",
  "ROUTINE",
  "PREGNANT",
  "POSTNATAL",
  "POSTMENOPAUSAL",
  "CONTRACEPTION",
  "THERAPY",
];
const CURRENT_HIV_DRUGS = [
  "3TC",
  "TDF",
  "NVP",
  "ABC",
  "AZT",
  "EFV",
  "DDI",
  "LPV_R",
  "ATV_R",
  "RAL",
  "ETR",
  "DRV_R",
];
const PREVIOUS_HIV_DRUGS = [
  "3TC",
  "TDF",
  "NVP",
  "ABC",
  "AZT",
  "EFV",
  "DDI",
  "LPV_R",
  "ATV_R",
  "D4T",
];
const BLOOD_GROUPS = [
  "A+",
  "A-",
  "B+",
  "B-",
  "AB+",
  "AB-",
  "O+",
  "O-",
  "UNKNOWN",
];
const BLOOD_COMPONENTS = ["WHOLE_BLOOD", "PACKED_CELLS", "FFP", "OTHER"];
const VL_REASONS = [
  "ROUTINE",
  "CLINICAL_FAILURE",
  "IMMUNOLOGICAL_FAILURE",
  "REPEAT_AFTER_EAC",
];
const PCR_REASONS = [
  "WEEKS_6",
  "WEEKS_14",
  "OTHER_REASON",
  "AFTER_WEANING",
  "CONFIRMATORY",
];
const MOTHER_ART = [
  "BEFORE_PREGNANCY",
  "DURING_PREGNANCY",
  "DURING_LABOUR",
  "DURING_BREASTFEEDING",
  "NO_ART",
  "UNKNOWN",
];
const TB_PURPOSES = ["DIAGNOSTIC", "FOLLOW_UP"];
const YES_NO = ["Y", "N"];
const YES_NO_UNKNOWN = ["Y", "N", "U"];

export interface ComponentRequest {
  units?: string;
  requiredDate?: string;
  requiredTime?: string;
  specify?: string;
}

export interface ClinicalInformationValues {
  forms?: RequestForm[];
  clinicalHistory?: string;
  currentTreatment?: string;
  previousTreatment?: string;
  tb?: {
    tbNumber?: string;
    suspectNumber?: string;
    previousLabNumber?: string;
    purpose?: string;
    firstSampleDate?: string;
    referredToLab?: string;
    sampleNumber?: string;
    dateProcessed?: string;
  };
  cervical?: {
    states?: string[];
    contraceptionSpecify?: string;
    therapySpecify?: string;
    lmp?: string;
    parity?: string;
    lastPregnancy?: string;
  };
  viralLoad?: {
    artNumber?: string;
    pregnant?: string;
    breastfeeding?: string;
    regimenStartDate?: string;
    reason?: string;
    missedDoses?: string;
    currentRegimen?: string[];
    previousRegimen?: string[];
    firstCd4?: string;
    firstCd4Date?: string;
    lastCd4?: string;
    lastCd4Date?: string;
    previousViralLoad?: string;
    previousViralLoadDate?: string;
    viralLoadLog?: string;
  };
  pcr?: {
    reason?: string;
    under5Number?: string;
    ancNumber?: string;
    motherArtNumber?: string;
    motherName?: string;
    village?: string;
    clinicContact?: string;
    facilityCode?: string;
    collectedBy?: string;
    motherArt?: string;
    gestationalAge?: string;
    nvpInitiated?: string;
    everBreastfed?: string;
    breastfeedingNow?: string;
    lastBreastfeedDate?: string;
  };
  transfusion?: {
    operationName?: string;
    previousTransfusion?: "" | "Y" | "N";
    reaction?: "" | "Y" | "N";
    reactionType?: string;
    patientGroup?: string;
    pregnancies?: string;
    components?: string[];
    requests?: Record<string, ComponentRequest>;
  };
}

interface Props {
  values?: ClinicalInformationValues;
  /** The request forms whose fields to show, from the order's tests. */
  forms?: RequestForm[];
  /** The order's patient sex: "M", "F" or empty. */
  patientSex?: string;
  onChange: (values: ClinicalInformationValues) => void;
  disabled?: boolean;
}

const K = "lesotho.clinicalInfo.";
const ANSWER_LABELS: Record<string, string> = {
  Y: "yes",
  N: "no",
  U: "unknown",
};

/** Forms that apply to this patient: cervical cytology is hidden for men. */
export const formsForSex = (sex?: string): RequestForm[] =>
  REQUEST_FORMS.filter(
    (form) => !(form === "CERVICAL_CYTOLOGY" && sex === "M"),
  );

const toggle = (list: string[] = [], item: string, on: boolean) =>
  on ? Array.from(new Set([...list, item])) : list.filter((x) => x !== item);

const ClinicalInformation: React.FC<Props> = ({
  values = {},
  forms = [],
  patientSex,
  onChange,
  disabled = false,
}) => {
  const intl = useIntl();
  const t = (id: string) => intl.formatMessage({ id: K + id });
  const available = formsForSex(patientSex);
  // A form's fields that are no longer shown (its test was removed, or the
  // patient's sex was set to male) keep what was typed, so nothing is lost if
  // the test or the sex is changed back.
  const shown = (form: RequestForm) =>
    forms.includes(form) && available.includes(form);
  const isMale = patientSex === "M";

  const set = (patch: Partial<ClinicalInformationValues>) =>
    onChange({ ...values, ...patch });
  const setIn = <
    G extends "tb" | "cervical" | "viralLoad" | "pcr" | "transfusion",
  >(
    group: G,
    patch: Partial<NonNullable<ClinicalInformationValues[G]>>,
  ) => onChange({ ...values, [group]: { ...(values[group] || {}), ...patch } });

  const text = (
    id: string,
    value: string | undefined,
    onValue: (v: string) => void,
    type = "text",
    suffix = "",
  ) => (
    <TextInput
      id={`clinical-info-${id}${suffix ? "-" + suffix : ""}`}
      labelText={t(id)}
      type={type}
      value={value || ""}
      disabled={disabled}
      onChange={(e) => onValue(e.target.value)}
    />
  );
  const area = (
    id: string,
    value: string | undefined,
    onValue: (v: string) => void,
  ) => (
    <TextArea
      id={`clinical-info-${id}`}
      labelText={t(id)}
      value={value || ""}
      disabled={disabled}
      rows={3}
      maxCount={1000}
      enableCounter
      onChange={(e) => onValue(e.target.value)}
    />
  );
  const date = (
    id: string,
    value: string | undefined,
    onValue: (v: string) => void,
  ) => (
    <CustomDatePicker
      id={`clinical-info-${id}`}
      labelText={t(id)}
      value={value || ""}
      disabled={disabled}
      disallowFutureDate={true}
      onChange={(v: string) => onValue(v)}
    />
  );
  const checklist = (
    groupId: string,
    keyPrefix: string,
    items: string[],
    selected: string[] = [],
    onValue: (next: string[]) => void,
  ) => (
    <FormGroup legendText={t(groupId)} className="clinical-info-checklist">
      {items.map((item) => (
        <Checkbox
          key={item}
          id={`clinical-info-${groupId}-${item}`}
          labelText={t(keyPrefix + item)}
          checked={selected.includes(item)}
          disabled={disabled}
          onChange={(_, { checked }) =>
            onValue(toggle(selected, item, checked))
          }
        />
      ))}
    </FormGroup>
  );
  // One answer from a short list, as radio buttons. Labels come from
  // `<keyPrefix><option>`; Y/N/U use the shared yes/no/unknown labels.
  const choice = (
    id: string,
    options: string[],
    value: string | undefined,
    onValue: (v: string) => void,
    keyPrefix = "",
  ) => (
    <RadioButtonGroup
      legendText={t(id)}
      name={`clinical-info-${id}`}
      valueSelected={value || ""}
      disabled={disabled}
      onChange={(v) => onValue(String(v))}
    >
      {options.map((option) => (
        <RadioButton
          key={option}
          id={`clinical-info-${id}-${option}`}
          labelText={t(keyPrefix ? keyPrefix + option : ANSWER_LABELS[option])}
          value={option}
        />
      ))}
    </RadioButtonGroup>
  );
  const yesNo = (
    id: string,
    value: string | undefined,
    onValue: (v: "Y" | "N") => void,
  ) => choice(id, YES_NO, value, (v) => onValue(v as "Y" | "N"));
  const yesNoUnknown = (
    id: string,
    value: string | undefined,
    onValue: (v: string) => void,
  ) => choice(id, YES_NO_UNKNOWN, value, onValue);

  const tb = values.tb || {};
  const cervical = values.cervical || {};
  const vl = values.viralLoad || {};
  const pcr = values.pcr || {};
  const transfusion = values.transfusion || {};
  const requests = transfusion.requests || {};

  return (
    <div className="clinical-info" data-testid="clinical-information">
      {/* Shared by every request form. */}
      <h5 className="clinical-info-heading">{t("section.common")}</h5>
      <Grid fullWidth={true} className="clinical-info-grid">
        <Column lg={16} md={8} sm={4}>
          {area("clinicalHistory", values.clinicalHistory, (v) =>
            set({ clinicalHistory: v }),
          )}
        </Column>
        <Column lg={8} md={4} sm={4}>
          {area("currentTreatment", values.currentTreatment, (v) =>
            set({ currentTreatment: v }),
          )}
        </Column>
        <Column lg={8} md={4} sm={4}>
          {area("previousTreatment", values.previousTreatment, (v) =>
            set({ previousTreatment: v }),
          )}
        </Column>
      </Grid>

      {shown("TB") && (
        <section data-testid="clinical-info-tb">
          <h5 className="clinical-info-heading">{t("form.TB")}</h5>
          <Grid fullWidth={true} className="clinical-info-grid">
            <Column lg={5} md={4} sm={4}>
              {text("tbNumber", tb.tbNumber, (v) =>
                setIn("tb", { tbNumber: v }),
              )}
            </Column>
            <Column lg={5} md={4} sm={4}>
              {text("suspectNumber", tb.suspectNumber, (v) =>
                setIn("tb", { suspectNumber: v }),
              )}
            </Column>
            <Column lg={6} md={4} sm={4}>
              {text("previousLabNumber", tb.previousLabNumber, (v) =>
                setIn("tb", { previousLabNumber: v }),
              )}
            </Column>
            <Column lg={8} md={4} sm={4}>
              {choice(
                "tbPurpose",
                TB_PURPOSES,
                tb.purpose,
                (v) => setIn("tb", { purpose: v }),
                "tbPurpose.",
              )}
            </Column>
            <Column lg={8} md={4} sm={4}>
              {date("firstSampleDate", tb.firstSampleDate, (v) =>
                setIn("tb", { firstSampleDate: v }),
              )}
            </Column>
            <Column lg={8} md={4} sm={4}>
              {text("referredToLab", tb.referredToLab, (v) =>
                setIn("tb", { referredToLab: v }),
              )}
            </Column>
            <Column lg={4} md={4} sm={4}>
              {text("sampleNumber", tb.sampleNumber, (v) =>
                setIn("tb", { sampleNumber: v }),
              )}
            </Column>
            <Column lg={4} md={4} sm={4}>
              {date("dateProcessed", tb.dateProcessed, (v) =>
                setIn("tb", { dateProcessed: v }),
              )}
            </Column>
          </Grid>
        </section>
      )}

      {shown("CERVICAL_CYTOLOGY") && (
        <section data-testid="clinical-info-cervical">
          <h5 className="clinical-info-heading">
            {t("form.CERVICAL_CYTOLOGY")}
          </h5>
          {checklist(
            "cervixState",
            "cervix.",
            CERVIX_STATES,
            cervical.states,
            (states) => setIn("cervical", { states }),
          )}
          <Grid fullWidth={true} className="clinical-info-grid">
            {(cervical.states || []).includes("CONTRACEPTION") && (
              <Column lg={8} md={4} sm={4}>
                {text(
                  "contraceptionSpecify",
                  cervical.contraceptionSpecify,
                  (v) => setIn("cervical", { contraceptionSpecify: v }),
                )}
              </Column>
            )}
            {(cervical.states || []).includes("THERAPY") && (
              <Column lg={8} md={4} sm={4}>
                {text("therapySpecify", cervical.therapySpecify, (v) =>
                  setIn("cervical", { therapySpecify: v }),
                )}
              </Column>
            )}
            <Column lg={5} md={4} sm={4}>
              {date("lmp", cervical.lmp, (v) => setIn("cervical", { lmp: v }))}
            </Column>
            <Column lg={5} md={4} sm={4}>
              {text(
                "parity",
                cervical.parity,
                (v) => setIn("cervical", { parity: v }),
                "number",
              )}
            </Column>
            <Column lg={6} md={4} sm={4}>
              {date("lastPregnancy", cervical.lastPregnancy, (v) =>
                setIn("cervical", { lastPregnancy: v }),
              )}
            </Column>
          </Grid>
        </section>
      )}

      {shown("VIRAL_LOAD") && (
        <section data-testid="clinical-info-viral-load">
          <h5 className="clinical-info-heading">{t("form.VIRAL_LOAD")}</h5>
          <Grid fullWidth={true} className="clinical-info-grid">
            <Column lg={8} md={4} sm={4}>
              {text("artNumber", vl.artNumber, (v) =>
                setIn("viralLoad", { artNumber: v }),
              )}
            </Column>
            <Column lg={8} md={4} sm={4}>
              {date("regimenStartDate", vl.regimenStartDate, (v) =>
                setIn("viralLoad", { regimenStartDate: v }),
              )}
            </Column>
            {!isMale && (
              <Column lg={8} md={4} sm={4}>
                {yesNo("pregnant", vl.pregnant, (v) =>
                  setIn("viralLoad", { pregnant: v }),
                )}
              </Column>
            )}
            {!isMale && (
              <Column lg={8} md={4} sm={4}>
                {yesNo("breastfeeding", vl.breastfeeding, (v) =>
                  setIn("viralLoad", { breastfeeding: v }),
                )}
              </Column>
            )}
            <Column lg={16} md={8} sm={4}>
              {choice(
                "vlReason",
                VL_REASONS,
                vl.reason,
                (v) => setIn("viralLoad", { reason: v }),
                "vlReason.",
              )}
            </Column>
            <Column lg={16} md={8} sm={4}>
              {yesNo("missedDoses", vl.missedDoses, (v) =>
                setIn("viralLoad", { missedDoses: v }),
              )}
            </Column>
          </Grid>
          {checklist(
            "currentHivTreatment",
            "drug.",
            CURRENT_HIV_DRUGS,
            vl.currentRegimen,
            (currentRegimen) => setIn("viralLoad", { currentRegimen }),
          )}
          {checklist(
            "previousHivTreatment",
            "drug.",
            PREVIOUS_HIV_DRUGS,
            vl.previousRegimen,
            (previousRegimen) => setIn("viralLoad", { previousRegimen }),
          )}
          <Grid fullWidth={true} className="clinical-info-grid">
            <Column lg={4} md={4} sm={4}>
              {text(
                "firstCd4",
                vl.firstCd4,
                (v) => setIn("viralLoad", { firstCd4: v }),
                "number",
              )}
            </Column>
            <Column lg={4} md={4} sm={4}>
              {date("firstCd4Date", vl.firstCd4Date, (v) =>
                setIn("viralLoad", { firstCd4Date: v }),
              )}
            </Column>
            <Column lg={4} md={4} sm={4}>
              {text(
                "lastCd4",
                vl.lastCd4,
                (v) => setIn("viralLoad", { lastCd4: v }),
                "number",
              )}
            </Column>
            <Column lg={4} md={4} sm={4}>
              {date("lastCd4Date", vl.lastCd4Date, (v) =>
                setIn("viralLoad", { lastCd4Date: v }),
              )}
            </Column>
            <Column lg={4} md={4} sm={4}>
              {text("previousViralLoad", vl.previousViralLoad, (v) =>
                setIn("viralLoad", { previousViralLoad: v }),
              )}
            </Column>
            <Column lg={4} md={4} sm={4}>
              {date("previousViralLoadDate", vl.previousViralLoadDate, (v) =>
                setIn("viralLoad", { previousViralLoadDate: v }),
              )}
            </Column>
            <Column lg={8} md={8} sm={4}>
              {text(
                "viralLoadLog",
                vl.viralLoadLog,
                (v) => setIn("viralLoad", { viralLoadLog: v }),
                "number",
              )}
            </Column>
          </Grid>
        </section>
      )}

      {shown("DNA_PCR") && (
        <section data-testid="clinical-info-pcr">
          <h5 className="clinical-info-heading">{t("form.DNA_PCR")}</h5>
          {choice(
            "pcrReason",
            PCR_REASONS,
            pcr.reason,
            (v) => setIn("pcr", { reason: v }),
            "pcrReason.",
          )}
          <p className="clinical-info-subheading">{t("pcrInfantMother")}</p>
          <Grid fullWidth={true} className="clinical-info-grid">
            <Column lg={4} md={4} sm={4}>
              {text("under5Number", pcr.under5Number, (v) =>
                setIn("pcr", { under5Number: v }),
              )}
            </Column>
            <Column lg={4} md={4} sm={4}>
              {text("ancNumber", pcr.ancNumber, (v) =>
                setIn("pcr", { ancNumber: v }),
              )}
            </Column>
            <Column lg={4} md={4} sm={4}>
              {text("motherArtNumber", pcr.motherArtNumber, (v) =>
                setIn("pcr", { motherArtNumber: v }),
              )}
            </Column>
            <Column lg={4} md={4} sm={4}>
              {text("village", pcr.village, (v) =>
                setIn("pcr", { village: v }),
              )}
            </Column>
            <Column lg={8} md={4} sm={4}>
              {text("motherName", pcr.motherName, (v) =>
                setIn("pcr", { motherName: v }),
              )}
            </Column>
          </Grid>
          <p className="clinical-info-subheading">{t("pcrCollection")}</p>
          <Grid fullWidth={true} className="clinical-info-grid">
            <Column lg={5} md={4} sm={4}>
              {text("collectedBy", pcr.collectedBy, (v) =>
                setIn("pcr", { collectedBy: v }),
              )}
            </Column>
            <Column lg={5} md={4} sm={4}>
              {text(
                "clinicContact",
                pcr.clinicContact,
                (v) => setIn("pcr", { clinicContact: v }),
                "tel",
              )}
            </Column>
            <Column lg={6} md={4} sm={4}>
              {text("facilityCode", pcr.facilityCode, (v) =>
                setIn("pcr", { facilityCode: v }),
              )}
            </Column>
          </Grid>
          <p className="clinical-info-subheading">{t("pcrPmtct")}</p>
          {choice(
            "motherArt",
            MOTHER_ART,
            pcr.motherArt,
            (v) => setIn("pcr", { motherArt: v }),
            "motherArt.",
          )}
          <Grid fullWidth={true} className="clinical-info-grid">
            <Column lg={4} md={4} sm={4}>
              {text("gestationalAge", pcr.gestationalAge, (v) =>
                setIn("pcr", { gestationalAge: v }),
              )}
            </Column>
            <Column lg={12} md={4} sm={4}>
              {yesNoUnknown("nvpInitiated", pcr.nvpInitiated, (v) =>
                setIn("pcr", { nvpInitiated: v }),
              )}
            </Column>
            <Column lg={8} md={4} sm={4}>
              {yesNoUnknown("everBreastfed", pcr.everBreastfed, (v) =>
                // A baby never breastfed has no breastfeeding to report.
                setIn("pcr", {
                  everBreastfed: v,
                  ...(v === "Y"
                    ? {}
                    : { breastfeedingNow: "", lastBreastfeedDate: "" }),
                }),
              )}
            </Column>
            {pcr.everBreastfed === "Y" && (
              <Column lg={8} md={4} sm={4}>
                {yesNoUnknown("breastfeedingNow", pcr.breastfeedingNow, (v) =>
                  setIn("pcr", {
                    breastfeedingNow: v,
                    ...(v === "N" ? {} : { lastBreastfeedDate: "" }),
                  }),
                )}
              </Column>
            )}
            {pcr.everBreastfed === "Y" && pcr.breastfeedingNow === "N" && (
              <Column lg={8} md={4} sm={4}>
                {date("lastBreastfeedDate", pcr.lastBreastfeedDate, (v) =>
                  setIn("pcr", { lastBreastfeedDate: v }),
                )}
              </Column>
            )}
          </Grid>
        </section>
      )}

      {shown("BLOOD_TRANSFUSION") && (
        <section data-testid="clinical-info-transfusion">
          <h5 className="clinical-info-heading">
            {t("form.BLOOD_TRANSFUSION")}
          </h5>
          <Grid fullWidth={true} className="clinical-info-grid">
            <Column lg={8} md={4} sm={4}>
              {text("operationName", transfusion.operationName, (v) =>
                setIn("transfusion", { operationName: v }),
              )}
            </Column>
            <Column lg={4} md={4} sm={4}>
              <Select
                id="clinical-info-patientGroup"
                labelText={t("patientGroup")}
                value={transfusion.patientGroup || ""}
                disabled={disabled}
                onChange={(e) =>
                  setIn("transfusion", { patientGroup: e.target.value })
                }
              >
                <SelectItem value="" text="" />
                {BLOOD_GROUPS.map((g) => (
                  <SelectItem
                    key={g}
                    value={g}
                    text={g === "UNKNOWN" ? t("unknown") : g}
                  />
                ))}
              </Select>
            </Column>
            {!isMale && (
              <Column lg={4} md={4} sm={4}>
                {text(
                  "pregnancies",
                  transfusion.pregnancies,
                  (v) => setIn("transfusion", { pregnancies: v }),
                  "number",
                )}
              </Column>
            )}
            <Column lg={8} md={4} sm={4}>
              {yesNo(
                "previousTransfusion",
                transfusion.previousTransfusion,
                (v) =>
                  // Answering "No" clears the reaction it no longer applies to.
                  setIn("transfusion", {
                    previousTransfusion: v,
                    ...(v === "N" ? { reaction: "", reactionType: "" } : {}),
                  }),
              )}
            </Column>
            {transfusion.previousTransfusion === "Y" && (
              <Column lg={8} md={4} sm={4}>
                {yesNo("reaction", transfusion.reaction, (v) =>
                  setIn("transfusion", {
                    reaction: v,
                    ...(v === "N" ? { reactionType: "" } : {}),
                  }),
                )}
              </Column>
            )}
            {transfusion.previousTransfusion === "Y" &&
              transfusion.reaction === "Y" && (
                <Column lg={16} md={8} sm={4}>
                  {text("reactionType", transfusion.reactionType, (v) =>
                    setIn("transfusion", { reactionType: v }),
                  )}
                </Column>
              )}
          </Grid>
          {checklist(
            "componentsRequested",
            "component.",
            BLOOD_COMPONENTS,
            transfusion.components,
            (components) => setIn("transfusion", { components }),
          )}
          {(transfusion.components || []).map((component) => {
            const request = requests[component] || {};
            const setRequest = (patch: Partial<ComponentRequest>) =>
              setIn("transfusion", {
                requests: {
                  ...requests,
                  [component]: { ...request, ...patch },
                },
              });
            return (
              <Grid
                fullWidth={true}
                className="clinical-info-grid"
                key={component}
                data-testid={`clinical-info-component-${component}`}
              >
                <Column lg={16} md={8} sm={4}>
                  <p className="clinical-info-subheading">
                    {t("component." + component)}
                  </p>
                </Column>
                {component === "OTHER" && (
                  <Column lg={4} md={4} sm={4}>
                    {text(
                      "componentSpecify",
                      request.specify,
                      (v) => setRequest({ specify: v }),
                      "text",
                      component,
                    )}
                  </Column>
                )}
                <Column lg={4} md={4} sm={4}>
                  {text(
                    "unitsRequired",
                    request.units,
                    (v) => setRequest({ units: v }),
                    "number",
                    component,
                  )}
                </Column>
                <Column lg={4} md={4} sm={4}>
                  <CustomDatePicker
                    id={`clinical-info-requiredDate-${component}`}
                    labelText={t("requiredDate")}
                    value={request.requiredDate || ""}
                    disabled={disabled}
                    disallowPastDate={true}
                    onChange={(v: string) => setRequest({ requiredDate: v })}
                  />
                </Column>
                <Column lg={4} md={4} sm={4}>
                  {text(
                    "requiredTime",
                    request.requiredTime,
                    (v) => setRequest({ requiredTime: v }),
                    "time",
                    component,
                  )}
                </Column>
              </Grid>
            );
          })}
        </section>
      )}
    </div>
  );
};

export default ClinicalInformation;
