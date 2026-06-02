import {
  Button,
  Column,
  ComboBox,
  Grid,
  InlineNotification,
  Select,
  SelectItem,
  TextArea,
  Tile,
} from "@carbon/react";
import { useContext, useEffect, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationKinds } from "../common/CustomNotification";
import { NotificationContext } from "../layout/Layout";
import {
  VectorIdentificationAPI,
  VectorSpeciesAPI,
} from "./VectorIdentificationService";

const METHODS = ["MORPHOLOGICAL", "MOLECULAR", "BOTH"];
const CONFIDENCES = ["CONFIRMED", "PRESUMPTIVE"];

const speciesLabel = (sp) =>
  [sp?.genus, sp?.species, sp?.subspecies].filter(Boolean).join(" ");

const VectorBulkApplyTile = ({
  sampleItemIds,
  prefillSpecimen,
  onClose,
  onApplied,
}) => {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [speciesCatalog, setSpeciesCatalog] = useState([]);
  const [selectedSpecies, setSelectedSpecies] = useState(null);
  const [method, setMethod] = useState(
    prefillSpecimen?.identificationMethod || "",
  );
  const [confidence, setConfidence] = useState(
    prefillSpecimen?.confidence || "",
  );
  const [physState, setPhysState] = useState(
    prefillSpecimen?.physiologicalState || "PHYS_UNKNOWN",
  );
  const [lifecycleStage, setLifecycleStage] = useState(
    prefillSpecimen?.lifecycleStage || "ADULT",
  );
  const [physStates, setPhysStates] = useState([]);
  const [lifecycleStages, setLifecycleStages] = useState([]);

  const isMosquito =
    (prefillSpecimen?.typeOfSampleName || "").toLowerCase() === "mosquito";
  const showPhysState = lifecycleStage === "ADULT" && isMosquito;
  const [notes, setNotes] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const [bloodMealAdded, setBloodMealAdded] = useState(false);
  const [bloodMealDismissed, setBloodMealDismissed] = useState(false);
  const [bloodMealBusy, setBloodMealBusy] = useState(false);

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
    VectorSpeciesAPI.getAll()
      .then((data) => {
        const list = (Array.isArray(data) ? data : []).filter(
          (s) => s.active !== false,
        );
        setSpeciesCatalog(list);
        if (prefillSpecimen?.vectorSpeciesId && !selectedSpecies) {
          const match = list.find(
            (s) => s.id === prefillSpecimen.vectorSpeciesId,
          );
          if (match) setSelectedSpecies(match);
        }
      })
      .catch(() => setSpeciesCatalog([]));
  }, [prefillSpecimen]);

  useEffect(() => {
    VectorIdentificationAPI.getDictionaryEntries("vecPhysiologicalState")
      .then((data) => {
        if (Array.isArray(data) && data.length > 0) setPhysStates(data);
      })
      .catch(() => {});
  }, []);

  useEffect(() => {
    VectorIdentificationAPI.getDictionaryEntries("vecLifecycleStages")
      .then((data) => {
        if (Array.isArray(data) && data.length > 0) setLifecycleStages(data);
      })
      .catch(() => {});
  }, []);

  const handleAddBloodmealPanel = () => {
    if (!sampleItemIds || sampleItemIds.length === 0) return;
    setBloodMealBusy(true);
    const calls = sampleItemIds.map((id) =>
      VectorIdentificationAPI.addBloodmealPanel(id)
        .then((result) => ({ id, status: "ok", result }))
        .catch((err) => ({ id, status: "error", err })),
    );
    Promise.all(calls).then((results) => {
      let addedCount = 0;
      let skippedCount = 0;
      let rejectedCount = 0;
      results.forEach((r) => {
        if (r.status !== "ok") {
          rejectedCount++;
          return;
        }
        if (r.result && (r.result.created || 0) > 0) addedCount++;
        else skippedCount++;
      });
      setBloodMealAdded(true);
      setBloodMealBusy(false);
      addNotification({
        kind:
          rejectedCount > 0 && addedCount === 0
            ? NotificationKinds.error
            : NotificationKinds.info,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage(
          {
            id: "vectorId.bloodMeal.bulkSummary",
            defaultMessage:
              "Blood-meal panel: {added} added, {skipped} already attached, {rejected} rejected.",
          },
          { added: addedCount, skipped: skippedCount, rejected: rejectedCount },
        ),
      });
      setNotificationVisible(true);
    });
  };

  const handleDismissBloodmealPrompt = () => {
    setBloodMealDismissed(true);
    if (sampleItemIds && sampleItemIds.length > 0) {
      sampleItemIds.forEach((id) => {
        VectorIdentificationAPI.dismissBloodmealSuggestion(id).catch(() => {});
      });
    }
  };

  const handleApply = () => {
    if (
      !selectedSpecies ||
      !METHODS.includes(method) ||
      !CONFIDENCES.includes(confidence)
    ) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "vectorId.error.requiredFields" }),
      });
      setNotificationVisible(true);
      return;
    }
    setSubmitting(true);
    const body = {
      sampleItemIds,
      vectorSpeciesId: selectedSpecies.id,
      identificationMethod: method,
      confidence,
      notes: notes || null,
      lifecycleStage: lifecycleStage === "UNKNOWN" ? null : lifecycleStage,
      physiologicalState:
        showPhysState && physState !== "PHYS_UNKNOWN" && physState !== "UNKNOWN"
          ? physState
          : null,
    };
    VectorIdentificationAPI.bulkIdentify(body)
      .then((result) => {
        addNotification({
          kind: NotificationKinds.success,
          title: intl.formatMessage({ id: "notification.title" }),
          message: intl.formatMessage(
            { id: "vectorId.message.bulkSuccess" },
            { count: result?.count ?? sampleItemIds.length },
          ),
        });
        setNotificationVisible(true);
        if (onApplied) onApplied(result);
      })
      .catch((err) => {
        addNotification({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.title" }),
          message:
            err.message ||
            intl.formatMessage({ id: "vectorId.error.saveFailed" }),
        });
        setNotificationVisible(true);
      })
      .finally(() => setSubmitting(false));
  };

  return (
    <Tile
      style={{
        marginBottom: "0.75rem",
        background: "var(--cds-background, #fff)",
        border: "2px solid #161616",
        padding: "1rem",
      }}
    >
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: "0.75rem",
        }}
      >
        <strong style={{ fontSize: "0.875rem" }}>
          <FormattedMessage id="vectorId.bulk.heading" />
        </strong>
        <span
          style={{
            fontSize: "0.75rem",
            color: "var(--cds-text-secondary, #525252)",
          }}
        >
          <FormattedMessage
            id="vectorId.bulk.applyToCount"
            values={{ count: sampleItemIds.length }}
          />
        </span>
      </div>

      <InlineNotification
        kind="info"
        title={intl.formatMessage({ id: "vectorId.bulk.notice.title" })}
        subtitle={intl.formatMessage({ id: "vectorId.bulk.notice.body" })}
        lowContrast
        hideCloseButton
        style={{ marginBottom: "1rem", maxWidth: "100%" }}
      />

      <Grid fullWidth narrow>
        <Column lg={16} md={8} sm={4}>
          <ComboBox
            id="bulk-species"
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
      </Grid>

      <Grid fullWidth narrow style={{ marginTop: "0.75rem" }}>
        <Column lg={8} md={4} sm={4}>
          <Select
            id="bulk-method"
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
        <Column lg={8} md={4} sm={4}>
          <Select
            id="bulk-confidence"
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
        {/* Switching to a non-ADULT stage collapses the PhysState field below. */}
        <Column lg={6} md={4} sm={4}>
          <Select
            id="bulk-lifecycleStage"
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
              id="bulk-physState"
              labelText={<FormattedMessage id="vectorId.form.physState" />}
              value={physState}
              onChange={(e) => setPhysState(e.target.value)}
            >
              {physStates.map((p) => (
                <SelectItem key={p.code} value={p.code} text={p.label} />
              ))}
            </Select>
          </Column>
        )}
        <Column lg={showPhysState ? 4 : 10} md={showPhysState ? 4 : 8} sm={4}>
          <TextArea
            id="bulk-notes"
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
              <FormattedMessage
                id="vectorId.bloodMeal.bulkTitle"
                defaultMessage="Blood-fed females detected ({count} specimens)"
                values={{ count: sampleItemIds.length }}
              />
            </div>
            <div
              style={{
                color: "var(--cds-text-primary, #393939)",
                marginBottom: "0.5rem",
                fontSize: "0.8125rem",
              }}
            >
              <FormattedMessage
                id="vectorId.bloodMeal.bulkBody"
                defaultMessage="Add the Mosquito Blood-Meal Identification Panel to every selected specimen so the lab can identify which vertebrate host the mosquitoes fed on."
              />
            </div>
            <div style={{ display: "flex", gap: "0.5rem" }}>
              <Button
                kind="primary"
                size="sm"
                onClick={handleAddBloodmealPanel}
                disabled={bloodMealBusy}
              >
                <FormattedMessage
                  id="vectorId.bloodMeal.bulkAdd"
                  defaultMessage="Add Panel to All ({count})"
                  values={{ count: sampleItemIds.length }}
                />
              </Button>
              <Button
                kind="ghost"
                size="sm"
                onClick={handleDismissBloodmealPrompt}
                disabled={bloodMealBusy}
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
          subtitle={intl.formatMessage(
            {
              id: "vectorId.bloodMeal.bulkAddedSub",
              defaultMessage:
                "Blood-meal panel processed for {count} specimens.",
            },
            { count: sampleItemIds.length },
          )}
          lowContrast
          hideCloseButton
          style={{ marginTop: "0.75rem", maxWidth: "100%" }}
        />
      )}

      <div style={{ marginTop: "1rem", display: "flex", gap: "0.5rem" }}>
        <Button kind="primary" onClick={handleApply} disabled={submitting}>
          <FormattedMessage
            id="vectorId.bulk.applyToCount"
            values={{ count: sampleItemIds.length }}
          />
        </Button>
        <Button kind="ghost" onClick={onClose} disabled={submitting}>
          <FormattedMessage id="vectorId.button.cancel" />
        </Button>
      </div>
    </Tile>
  );
};

export default VectorBulkApplyTile;
