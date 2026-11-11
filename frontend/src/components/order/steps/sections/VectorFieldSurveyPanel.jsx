import React, { useState } from "react";
import { useIntl } from "react-intl";
import {
  Grid,
  Column,
  Tile,
  TextInput,
  Select,
  SelectItem,
  Accordion,
  AccordionItem,
} from "@carbon/react";

/**
 * VectorFieldSurveyPanel - custom renderer for the "Vector Field Survey" program.
 *
 * Implements:
 * - AC3: Larval & Pupal data captured in two collapsible sections.
 * - AC4: live-computed indices preview (HI / CI / BI / ABJ / PPI) as the operator types.
 *
 * Design note: this is presentation-only. It reads and writes the SAME
 * questionnaireResponse via the getAnswer / onAnswerChange callbacks owned by
 * ProgramSection, so persistence (QuestionnaireResponse) and FHIR serialization are
 * unchanged, and the generic Questionnaire.jsx component is left untouched.
 *
 * The indices are NOT persisted - they are derived on the fly for preview only. The
 * authoritative index computation and exact 95% CIs live on the LHU (OGC-552).
 */

const LARVAL_FIELDS = [
  {
    linkId: "houses_examined",
    id: "vfs.housesExamined",
    label: "Houses examined",
  },
  {
    linkId: "houses_positive",
    id: "vfs.housesPositive",
    label: "Houses positive",
  },
  {
    linkId: "containers_examined",
    id: "vfs.containersExamined",
    label: "Containers examined",
  },
  {
    linkId: "containers_positive",
    id: "vfs.containersPositive",
    label: "Containers positive",
  },
];

const PUPAL_FIELDS = [
  { linkId: "pupae_count", id: "vfs.pupaeCount", label: "Pupae count" },
  {
    linkId: "population_at_risk",
    id: "vfs.populationAtRisk",
    label: "Population at risk",
  },
];

// Kemenkes national target: Larva-Free Rate (ABJ) >= 95%.
const ABJ_TARGET = 95;

const VectorFieldSurveyPanel = ({
  questionnaire,
  getAnswer = () => "",
  onAnswerChange = () => {},
  isReadOnly = false,
}) => {
  const intl = useIntl();
  const [larvalOpen, setLarvalOpen] = useState(true);
  const [pupalOpen, setPupalOpen] = useState(true);

  const t = (id, defaultMessage) => intl.formatMessage({ id, defaultMessage });

  // survey_method options come from the questionnaire (single source of truth for the enum)
  const surveyMethodOptions =
    questionnaire?.item
      ?.find((it) => it.linkId === "survey_method")
      ?.answerOption?.map((opt) => opt.valueCoding)
      .filter(Boolean) || [];

  // Read a numeric answer; null when blank/invalid so indices stay "—" until usable.
  const num = (linkId) => {
    const v = getAnswer(linkId);
    if (v === "" || v === null || v === undefined) return null;
    const n = Number(v);
    return Number.isFinite(n) ? n : null;
  };

  const pct = (numer, denom) =>
    numer != null && denom != null && denom > 0 ? (numer / denom) * 100 : null;
  const ratio = (numer, denom) =>
    numer != null && denom != null && denom > 0 ? numer / denom : null;

  const housesExamined = num("houses_examined");
  const housesPositive = num("houses_positive");
  const containersExamined = num("containers_examined");
  const containersPositive = num("containers_positive");
  const pupaeCount = num("pupae_count");
  const populationAtRisk = num("population_at_risk");

  const houseIndex = pct(housesPositive, housesExamined);
  const containerIndex = pct(containersPositive, containersExamined);
  const breteauIndex = pct(containersPositive, housesExamined);
  const abj = houseIndex != null ? 100 - houseIndex : null;
  const ppi = ratio(pupaeCount, populationAtRisk);

  const fmt = (v, digits = 1) => (v == null ? "—" : v.toFixed(digits));

  const indices = [
    {
      key: "hi",
      label: t("vfs.hi", "House Index (HI)"),
      value: fmt(houseIndex),
      unit: "%",
    },
    {
      key: "ci",
      label: t("vfs.ci", "Container Index (CI)"),
      value: fmt(containerIndex),
      unit: "%",
    },
    {
      key: "bi",
      label: t("vfs.bi", "Breteau Index (per 100 houses)"),
      value: fmt(breteauIndex),
      unit: "",
    },
    {
      key: "abj",
      label: t("vfs.abj", "ABJ (Larva-Free Rate)"),
      value: fmt(abj),
      unit: "%",
    },
    {
      key: "ppi",
      label: t("vfs.ppi", "Pupae Per Person (PPI)"),
      value: fmt(ppi, 2),
      unit: "",
    },
  ];

  // AC6 - inline input validation. Map of linkId -> error message.
  // A denominator (examined / population) is "in play" when it or its sibling
  // count has data, and must then be >= 1 (prevents the zero/empty denominators
  // and the >100% indices that would otherwise flow into the LHU).
  const errors = {};
  const numericFields = {
    houses_examined: housesExamined,
    houses_positive: housesPositive,
    containers_examined: containersExamined,
    containers_positive: containersPositive,
    pupae_count: pupaeCount,
    population_at_risk: populationAtRisk,
  };
  Object.keys(numericFields).forEach((k) => {
    if (numericFields[k] != null && numericFields[k] < 0) {
      errors[k] = t("vfs.err.negative", "Cannot be negative.");
    }
  });
  if (
    housesPositive != null &&
    housesExamined != null &&
    housesPositive > housesExamined
  ) {
    errors.houses_positive = t(
      "vfs.err.housesExceeded",
      "Cannot exceed houses examined.",
    );
  }
  if (
    containersPositive != null &&
    containersExamined != null &&
    containersPositive > containersExamined
  ) {
    errors.containers_positive = t(
      "vfs.err.containersExceeded",
      "Cannot exceed containers examined.",
    );
  }
  const examinedMin = t(
    "vfs.err.examinedMin",
    "Must be ≥ 1 when larval data is entered.",
  );
  if (
    (housesExamined != null || housesPositive != null) &&
    !errors.houses_examined &&
    (housesExamined == null || housesExamined < 1)
  ) {
    errors.houses_examined = examinedMin;
  }
  if (
    (containersExamined != null || containersPositive != null) &&
    !errors.containers_examined &&
    (containersExamined == null || containersExamined < 1)
  ) {
    errors.containers_examined = examinedMin;
  }
  if (
    (populationAtRisk != null || pupaeCount != null) &&
    !errors.population_at_risk &&
    (populationAtRisk == null || populationAtRisk < 1)
  ) {
    errors.population_at_risk = t(
      "vfs.err.populationMin",
      "Must be ≥ 1 when pupal data is entered.",
    );
  }

  const renderNumberField = ({ linkId, id, label }) => (
    <Column lg={8} md={4} sm={4} key={linkId}>
      <TextInput
        id={linkId}
        labelText={t(id, label)}
        type="number"
        min="0"
        step="1"
        value={getAnswer(linkId) || ""}
        onChange={onAnswerChange}
        disabled={isReadOnly}
        invalid={!!errors[linkId]}
        invalidText={errors[linkId] || ""}
      />
    </Column>
  );

  return (
    <div className="vector-field-survey-panel">
      <Accordion>
        <AccordionItem
          title={t("vfs.larval.title", "Larval Survey — Mode C")}
          open={larvalOpen}
          onHeadingClick={() => setLarvalOpen((o) => !o)}
        >
          <Grid>{LARVAL_FIELDS.map(renderNumberField)}</Grid>
        </AccordionItem>
        <AccordionItem
          title={t("vfs.pupal.title", "Pupal Survey — Mode D")}
          open={pupalOpen}
          onHeadingClick={() => setPupalOpen((o) => !o)}
        >
          <Grid>{PUPAL_FIELDS.map(renderNumberField)}</Grid>
        </AccordionItem>
      </Accordion>

      <Grid style={{ marginTop: "1rem" }}>
        <Column lg={8} md={4} sm={4}>
          <Select
            id="survey_method"
            labelText={t("vfs.surveyMethod", "Survey method")}
            value={getAnswer("survey_method") || ""}
            onChange={onAnswerChange}
            disabled={isReadOnly}
          >
            <SelectItem
              value=""
              text={t("vfs.surveyMethod.placeholder", "Choose an option")}
            />
            {surveyMethodOptions.map((opt) => (
              <SelectItem key={opt.code} value={opt.code} text={opt.display} />
            ))}
          </Select>
        </Column>
      </Grid>

      <Tile className="vfs-indices-tile" style={{ marginTop: "1rem" }}>
        <h6 style={{ marginBottom: "0.75rem" }}>
          {t("vfs.indices.title", "Computed indices (preview)")}
        </h6>
        <Grid>
          {indices.map((ix) => (
            <Column lg={3} md={4} sm={2} key={ix.key}>
              <div style={{ display: "flex", flexDirection: "column" }}>
                <span style={{ fontSize: "0.75rem", color: "#525252" }}>
                  {ix.label}
                </span>
                <span style={{ fontSize: "1.25rem", fontWeight: 600 }}>
                  {ix.value}
                  {ix.value !== "—" ? ix.unit : ""}
                </span>
              </div>
            </Column>
          ))}
        </Grid>
        {abj != null && (
          <p style={{ marginTop: "0.75rem", fontSize: "0.875rem" }}>
            {abj >= ABJ_TARGET
              ? t("vfs.abj.target.met", "✓ ABJ meets the 95% Kemenkes target")
              : t(
                  "vfs.abj.target.below",
                  "✗ ABJ below the 95% Kemenkes target",
                )}
          </p>
        )}
      </Tile>
    </div>
  );
};

export default VectorFieldSurveyPanel;
