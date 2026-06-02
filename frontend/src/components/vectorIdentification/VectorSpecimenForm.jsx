import React, { useContext, useEffect, useRef, useState } from "react";
import {
  Accordion,
  AccordionItem,
  Button,
  ComboBox,
  Column,
  Grid,
  InlineNotification,
  Select,
  SelectItem,
  TextArea,
  TextInput,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../layout/Layout";
import { NotificationKinds } from "../common/CustomNotification";
import {
  VectorIdentificationAPI,
  VectorSpeciesAPI,
} from "./VectorIdentificationService";

const METHODS = ["MORPHOLOGICAL", "MOLECULAR", "BOTH"];
const CONFIDENCES = ["CONFIRMED", "PRESUMPTIVE"];
const GENBANK_PATTERN = /^[A-Z]{1,2}[0-9]{5,8}$/;

const speciesLabel = (sp) =>
  [sp?.genus, sp?.species, sp?.subspecies].filter(Boolean).join(" ");

const VectorSpecimenForm = ({
  specimen,
  lotId,
  lotLifecycleStage,
  speciesById,
  onSaved,
  onCancel,
}) => {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [speciesCatalog, setSpeciesCatalog] = useState([]);
  const [selectedSpecies, setSelectedSpecies] = useState(null);
  const [method, setMethod] = useState(specimen?.identificationMethod || "");
  const [confidence, setConfidence] = useState(specimen?.confidence || "");
  const [notes, setNotes] = useState("");
  const [physState, setPhysState] = useState(
    specimen?.physiologicalState || "PHYS_UNKNOWN",
  );
  const [targetGene, setTargetGene] = useState("");
  const [assayName, setAssayName] = useState("");
  const [genbankAccession, setGenbankAccession] = useState("");
  const [genbankError, setGenbankError] = useState(null);
  const [saving, setSaving] = useState(false);
  const [bloodMealAdded, setBloodMealAdded] = useState(false);
  const [bloodMealDismissed, setBloodMealDismissed] = useState(false);
  const [resultCandidates, setResultCandidates] = useState([]);
  const [linkedResult, setLinkedResult] = useState(null);
  const [physStates, setPhysStates] = useState([]);
  const [lifecycleStages, setLifecycleStages] = useState([]);
  const [lifecycleStage, setLifecycleStage] = useState(
    specimen?.lifecycleStage || lotLifecycleStage || "ADULT",
  );

  const mountedRef = useRef(true);
  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  const showMolecular = method === "MOLECULAR" || method === "BOTH";

  const isMosquito =
    (specimen?.typeOfSampleName || "").toLowerCase() === "mosquito";
  const showPhysState = lifecycleStage === "ADULT" && isMosquito;
  const isBloodFed = (() => {
    if (!physState) return false;
    if (physState === "BLOOD_FED") return true;
    const entry = physStates.find((p) => p.code === physState);
    if (!entry || !entry.label) return false;
    return /blood[\s-]?fed/i.test(entry.label);
  })();
  const showBloodMealPrompt =
    showPhysState && isBloodFed && !bloodMealAdded && !bloodMealDismissed;

  useEffect(() => {
    if (speciesById && Object.keys(speciesById).length) {
      const list = Object.values(speciesById).filter((s) => s.active !== false);
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setSpeciesCatalog(list);
      return;
    }
    VectorSpeciesAPI.getAll()
      .then((data) => {
        if (!mountedRef.current) return;
        setSpeciesCatalog(
          (Array.isArray(data) ? data : []).filter((s) => s.active !== false),
        );
      })
      .catch(() => {
        if (mountedRef.current) setSpeciesCatalog([]);
      });
  }, [speciesById]);

  useEffect(() => {
    if (specimen?.vectorSpeciesId && speciesCatalog.length) {
      const match = speciesCatalog.find(
        (s) => s.id === specimen.vectorSpeciesId,
      );
      // eslint-disable-next-line react-hooks/set-state-in-effect
      if (match) setSelectedSpecies(match);
    }
  }, [specimen, speciesCatalog]);

  useEffect(() => {
    if (!lotId) return;
    VectorIdentificationAPI.getResultCandidates(lotId)
      .then((data) => {
        if (!mountedRef.current) return;
        setResultCandidates(Array.isArray(data) ? data : []);
      })
      .catch(() => {
        if (mountedRef.current) setResultCandidates([]);
      });
  }, [lotId]);

  useEffect(() => {
    VectorIdentificationAPI.getDictionaryEntries("vecPhysiologicalState")
      .then((data) => {
        if (!mountedRef.current) return;
        if (Array.isArray(data) && data.length > 0) setPhysStates(data);
      })
      .catch(() => {});
  }, []);

  useEffect(() => {
    VectorIdentificationAPI.getDictionaryEntries("vecLifecycleStages")
      .then((data) => {
        if (!mountedRef.current) return;
        if (Array.isArray(data) && data.length > 0) setLifecycleStages(data);
      })
      .catch(() => {});
  }, []);

  const validate = () => {
    if (!selectedSpecies) {
      return intl.formatMessage({ id: "vectorId.error.speciesRequired" });
    }
    if (!METHODS.includes(method)) {
      return intl.formatMessage({ id: "vectorId.error.methodRequired" });
    }
    if (!CONFIDENCES.includes(confidence)) {
      return intl.formatMessage({ id: "vectorId.error.confidenceRequired" });
    }
    if (genbankAccession && !GENBANK_PATTERN.test(genbankAccession.trim())) {
      return intl.formatMessage({ id: "vectorId.error.accessionFormat" });
    }
    if (notes && notes.length > 500) {
      return intl.formatMessage({ id: "vectorId.error.notesTooLong" });
    }
    return null;
  };

  const handleSave = () => {
    const validationError = validate();
    if (validationError) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: validationError,
      });
      setNotificationVisible(true);
      return;
    }
    setSaving(true);
    const body = {
      sampleItemId: specimen.sampleItemId,
      vectorSpeciesId: selectedSpecies.id,
      identificationMethod: method,
      confidence,
      notes: notes || null,
      lifecycleStage: lifecycleStage === "UNKNOWN" ? null : lifecycleStage,
      physiologicalState:
        showPhysState && physState !== "PHYS_UNKNOWN" && physState !== "UNKNOWN"
          ? physState
          : null,
      targetGene: showMolecular ? targetGene || null : null,
      assayName: showMolecular ? assayName || null : null,
      genbankAccession: showMolecular ? genbankAccession || null : null,
      linkedResultId:
        showMolecular && linkedResult ? linkedResult.resultId : null,
    };
    VectorIdentificationAPI.identify(specimen.sampleItemId, body)
      .then((result) => {
        addNotification({
          kind: NotificationKinds.success,
          title: intl.formatMessage({ id: "notification.title" }),
          message: intl.formatMessage({ id: "vectorId.message.saveSuccess" }),
        });
        setNotificationVisible(true);
        setTimeout(() => {
          if (onSaved) onSaved(result);
        }, 0);
      })
      .catch((err) => {
        addNotification({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.title" }),
          message:
            err?.payload?.message ||
            err?.message ||
            intl.formatMessage({ id: "vectorId.error.saveFailed" }),
        });
        setNotificationVisible(true);
      })
      .finally(() => {
        if (mountedRef.current) setSaving(false);
      });
  };

  return (
    <div
      style={{ padding: "1rem", background: "var(--cds-layer-01, #f4f4f4)" }}
    >
      <Grid fullWidth narrow>
        <Column lg={6} md={4} sm={4}>
          <ComboBox
            id={`species-${specimen.sampleItemId}`}
            titleText={
              <span>
                <FormattedMessage id="vectorId.form.species" /> *
              </span>
            }
            items={speciesCatalog}
            itemToString={speciesLabel}
            selectedItem={selectedSpecies}
            onChange={({ selectedItem }) => setSelectedSpecies(selectedItem)}
            placeholder={intl.formatMessage({
              id: "vectorId.form.speciesPlaceholder",
            })}
          />
        </Column>
        <Column lg={5} md={4} sm={4}>
          <Select
            id={`method-${specimen.sampleItemId}`}
            labelText={
              <span>
                <FormattedMessage id="vectorId.form.method" /> *
              </span>
            }
            value={method}
            onChange={(e) => setMethod(e.target.value)}
          >
            <SelectItem
              value=""
              text={intl.formatMessage({ id: "vectorId.form.select" })}
            />
            {METHODS.map((m) => (
              <SelectItem
                key={m}
                value={m}
                text={intl.formatMessage({
                  id: `vectorId.method.${m.toLowerCase()}`,
                })}
              />
            ))}
          </Select>
        </Column>
        <Column lg={5} md={4} sm={4}>
          <Select
            id={`confidence-${specimen.sampleItemId}`}
            labelText={
              <span>
                <FormattedMessage id="vectorId.form.confidence" /> *
              </span>
            }
            value={confidence}
            onChange={(e) => setConfidence(e.target.value)}
          >
            <SelectItem
              value=""
              text={intl.formatMessage({ id: "vectorId.form.select" })}
            />
            {CONFIDENCES.map((c) => (
              <SelectItem
                key={c}
                value={c}
                text={intl.formatMessage({
                  id: `vectorId.confidence.${c.toLowerCase()}`,
                })}
              />
            ))}
          </Select>
        </Column>
      </Grid>

      <Grid fullWidth narrow style={{ marginTop: "0.75rem" }}>
        <Column lg={6} md={4} sm={4}>
          <Select
            id={`lifecycleStage-${specimen.sampleItemId}`}
            labelText={<FormattedMessage id="vectorId.form.lifecycleStage" />}
            value={lifecycleStage}
            onChange={(e) => setLifecycleStage(e.target.value)}
          >
            {lifecycleStages.map((l) => (
              <SelectItem key={l.code} value={l.code} text={l.label} />
            ))}
          </Select>
        </Column>
        {showPhysState && (
          <Column lg={6} md={4} sm={4}>
            <Select
              id={`physState-${specimen.sampleItemId}`}
              labelText={<FormattedMessage id="vectorId.form.physState" />}
              value={physState}
              onChange={(e) => {
                setPhysState(e.target.value);
                setBloodMealDismissed(false);
              }}
            >
              {physStates.map((p) => (
                <SelectItem key={p.code} value={p.code} text={p.label} />
              ))}
            </Select>
          </Column>
        )}
        <Column lg={showPhysState ? 4 : 10} md={showPhysState ? 4 : 8} sm={4}>
          <TextArea
            id={`notes-${specimen.sampleItemId}`}
            labelText={<FormattedMessage id="vectorId.form.notes" />}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            maxCount={500}
            enableCounter
            rows={2}
          />
        </Column>
      </Grid>

      {showBloodMealPrompt && (
        <div
          style={{
            marginTop: "0.75rem",
            background: "var(--cds-blue-10, #edf5ff)",
            border: "1px solid #78a9ff",
            padding: "0.75rem 0.875rem",
            display: "flex",
            gap: "0.625rem",
          }}
        >
          <span style={{ fontSize: "1.125rem" }}>🩸</span>
          <div style={{ flex: 1 }}>
            <div
              style={{
                color: "var(--cds-link-primary-hover, #0043ce)",
                fontWeight: 600,
                marginBottom: "0.25rem",
              }}
            >
              <FormattedMessage id="vectorId.bloodMeal.title" />
            </div>
            <div
              style={{
                color: "var(--cds-text-primary, #393939)",
                marginBottom: "0.5rem",
                fontSize: "0.8125rem",
              }}
            >
              <FormattedMessage id="vectorId.bloodMeal.body" />
            </div>
            <div style={{ display: "flex", gap: "0.5rem" }}>
              <Button
                kind="primary"
                size="sm"
                onClick={() => {
                  VectorIdentificationAPI.addBloodmealPanel(
                    specimen.sampleItemId,
                  )
                    .then((result) => {
                      setBloodMealAdded(true);
                      if (
                        result &&
                        result.created === 0 &&
                        result.skipped > 0
                      ) {
                        addNotification({
                          kind: NotificationKinds.info,
                          title: intl.formatMessage({
                            id: "notification.title",
                          }),
                          message: intl.formatMessage(
                            {
                              id: "vectorId.bloodMeal.alreadyAttached",
                              defaultMessage:
                                "Blood-meal panel already attached ({skipped} tests).",
                            },
                            { skipped: result.skipped },
                          ),
                        });
                        setNotificationVisible(true);
                      }
                    })
                    .catch((err) => {
                      addNotification({
                        kind: NotificationKinds.error,
                        title: intl.formatMessage({ id: "notification.title" }),
                        message:
                          err.message ||
                          intl.formatMessage({
                            id: "vectorId.bloodMeal.error.add",
                          }),
                      });
                      setNotificationVisible(true);
                    });
                }}
              >
                <FormattedMessage id="vectorId.bloodMeal.add" />
              </Button>
              <Button
                kind="ghost"
                size="sm"
                onClick={() => {
                  VectorIdentificationAPI.dismissBloodmealSuggestion(
                    specimen.sampleItemId,
                  )
                    .then(() => setBloodMealDismissed(true))
                    .catch((err) => {
                      addNotification({
                        kind: NotificationKinds.error,
                        title: intl.formatMessage({ id: "notification.title" }),
                        message:
                          err.message ||
                          intl.formatMessage({
                            id: "vectorId.bloodMeal.error.dismiss",
                          }),
                      });
                      setNotificationVisible(true);
                    });
                }}
              >
                <FormattedMessage id="vectorId.bloodMeal.dismiss" />
              </Button>
            </div>
          </div>
        </div>
      )}
      {bloodMealAdded && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({ id: "vectorId.bloodMeal.added" })}
          subtitle={intl.formatMessage({ id: "vectorId.bloodMeal.addedSub" })}
          lowContrast
          hideCloseButton
          style={{ marginTop: "0.75rem", maxWidth: "100%" }}
        />
      )}

      {showMolecular && (
        <Accordion style={{ marginTop: "0.75rem" }}>
          <AccordionItem
            title={intl.formatMessage({ id: "vectorId.form.molecularDetails" })}
            open
          >
            <Grid fullWidth narrow>
              <Column lg={5} md={4} sm={4}>
                <TextInput
                  id={`targetGene-${specimen.sampleItemId}`}
                  labelText={<FormattedMessage id="vectorId.form.targetGene" />}
                  value={targetGene}
                  onChange={(e) => setTargetGene(e.target.value)}
                  placeholder={intl.formatMessage({
                    id: "vectorId.form.targetGene.placeholder",
                  })}
                />
              </Column>
              <Column lg={5} md={4} sm={4}>
                <TextInput
                  id={`assayName-${specimen.sampleItemId}`}
                  labelText={<FormattedMessage id="vectorId.form.assayName" />}
                  value={assayName}
                  onChange={(e) => setAssayName(e.target.value)}
                  placeholder={intl.formatMessage({
                    id: "vectorId.form.assayName.placeholder",
                  })}
                />
              </Column>
              <Column lg={6} md={4} sm={4}>
                <TextInput
                  id={`accession-${specimen.sampleItemId}`}
                  labelText={
                    <FormattedMessage id="vectorId.form.genbankAccession" />
                  }
                  value={genbankAccession}
                  onChange={(e) => {
                    setGenbankAccession(e.target.value);
                    setGenbankError(null);
                  }}
                  onBlur={(e) => {
                    const v = e.target.value;
                    if (v && !GENBANK_PATTERN.test(v.trim())) {
                      setGenbankError(
                        intl.formatMessage({
                          id: "vectorId.error.accessionFormat",
                        }),
                      );
                    } else {
                      setGenbankError(null);
                    }
                  }}
                  placeholder={intl.formatMessage({
                    id: "vectorId.form.genbankAccession.placeholder",
                  })}
                  invalid={Boolean(genbankError)}
                  invalidText={genbankError}
                />
              </Column>
            </Grid>

            <Grid fullWidth narrow style={{ marginTop: "0.75rem" }}>
              <Column lg={16} md={8} sm={4}>
                <ComboBox
                  id={`linkedResult-${specimen.sampleItemId}`}
                  titleText={
                    <FormattedMessage id="vectorId.form.linkedResult" />
                  }
                  items={resultCandidates}
                  itemToString={(c) =>
                    c
                      ? `${c.testName || "—"} — ${c.value || ""}${
                          c.sampleItemExternalId
                            ? " (" + c.sampleItemExternalId + ")"
                            : ""
                        }`
                      : ""
                  }
                  selectedItem={linkedResult}
                  onChange={({ selectedItem }) => setLinkedResult(selectedItem)}
                  placeholder={intl.formatMessage({
                    id: "vectorId.form.linkedResultPlaceholder",
                  })}
                />
              </Column>
            </Grid>
          </AccordionItem>
        </Accordion>
      )}

      <div style={{ marginTop: "1rem", display: "flex", gap: "0.5rem" }}>
        <Button kind="primary" onClick={handleSave} disabled={saving}>
          <FormattedMessage id="vectorId.button.save" />
        </Button>
        <Button kind="ghost" onClick={onCancel} disabled={saving}>
          <FormattedMessage id="vectorId.button.cancel" />
        </Button>
      </div>
    </div>
  );
};

export default VectorSpecimenForm;
