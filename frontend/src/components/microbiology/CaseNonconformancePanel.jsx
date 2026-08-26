import React, { useEffect, useMemo, useRef, useState } from "react";
import {
  Button,
  InlineNotification,
  RadioButton,
  RadioButtonGroup,
  Select,
  SelectItem,
  Stack,
  TextArea,
  TextInput,
} from "@carbon/react";
import { useIntl } from "react-intl";
import MicrobiologyService from "./MicrobiologyService";
import { formatMicrobiologyEnum } from "./MicrobiologyLabels";

const EMPTY_ISOLATES = [];

const initialForm = {
  categoryId: "",
  typeId: "",
  reportingUnitId: "",
  severity: "",
  title: "",
  description: "",
  immediateAction: "",
  disposition: "FLAG_ONLY",
  sourceAstRunId: "",
  retestScope: "WHOLE_PANEL",
  retestAntibioticId: "",
};

const CaseNonconformancePanel = ({
  caseId,
  mode,
  isolates = EMPTY_ISOLATES,
  workflowType,
  service = MicrobiologyService,
  onComplete,
  onCancel,
}) => {
  const intl = useIntl();
  const lostMode = mode === "mark-lost";
  const [categories, setCategories] = useState([]);
  const [reportingUnits, setReportingUnits] = useState([]);
  const [form, setForm] = useState(() => ({
    ...initialForm,
    disposition: lostMode ? "REJECT_TEST" : "FLAG_ONLY",
  }));
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [astRuns, setAstRuns] = useState([]);
  const [antibiotics, setAntibiotics] = useState([]);
  const [loadingRetest, setLoadingRetest] = useState(false);
  const titleRef = useRef(null);

  useEffect(() => {
    const frame = window.requestAnimationFrame(() => titleRef.current?.focus());
    return () => window.cancelAnimationFrame(frame);
  }, [mode]);

  useEffect(() => {
    let active = true;
    Promise.all([service.getNceCategories(), service.getNceReportingUnits()])
      .then(([categoryRows, unitRows]) => {
        if (!active) {
          return;
        }
        const safeCategories = Array.isArray(categoryRows) ? categoryRows : [];
        setCategories(safeCategories);
        setReportingUnits(Array.isArray(unitRows) ? unitRows : []);
        if (!lostMode) {
          const preAnalytical = safeCategories.find((category) =>
            String(category.name || "")
              .toLowerCase()
              .replace(/[^a-z]/g, "")
              .includes("preanalytical"),
          );
          if (preAnalytical) {
            setForm((current) => ({
              ...current,
              categoryId: String(preAnalytical.id),
            }));
          }
        } else {
          const match = safeCategories
            .flatMap((category) =>
              (category.types || []).map((type) => ({ category, type })),
            )
            .find(({ type }) =>
              String(type.name || "")
                .toLowerCase()
                .includes("specimen lost"),
            );
          if (match) {
            setForm((current) => ({
              ...current,
              categoryId: String(match.category.id),
              typeId: String(match.type.id),
            }));
          }
        }
      })
      .catch(() => {
        if (active) {
          setError(
            intl.formatMessage({
              id: "microbiology.nce.configurationLoadError",
            }),
          );
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, [intl, lostMode, service]);

  useEffect(() => {
    if (lostMode || form.disposition !== "RETEST") {
      return;
    }
    let active = true;
    Promise.all([
      Promise.all(
        isolates.map((isolate) =>
          service.getAstRunsForIsolate(isolate.id).then((runs = []) =>
            runs.map((run) => ({
              ...run,
              isolateLabel: isolate.isolateLabel || isolate.id,
            })),
          ),
        ),
      ),
      service.getAntibiotics(workflowType),
    ])
      .then(([runsByIsolate, antibioticRows]) => {
        if (!active) {
          return;
        }
        setAstRuns(
          runsByIsolate.flat().filter((run) => run.status === "REVIEWED"),
        );
        setAntibiotics(Array.isArray(antibioticRows) ? antibioticRows : []);
      })
      .catch(() => {
        if (active) {
          setError(
            intl.formatMessage({ id: "microbiology.nce.retestLoadError" }),
          );
        }
      })
      .finally(() => {
        if (active) {
          setLoadingRetest(false);
        }
      });
    return () => {
      active = false;
    };
  }, [form.disposition, intl, isolates, lostMode, service, workflowType]);

  const selectedCategory = useMemo(
    () =>
      categories.find(
        (category) => String(category.id) === String(form.categoryId),
      ),
    [categories, form.categoryId],
  );
  const types = selectedCategory?.types || [];
  const lostConfigurationMissing = lostMode && !loading && !form.typeId;
  const selectedAstRun = astRuns.find((run) => run.id === form.sourceAstRunId);
  const retestSelectionMissing =
    form.disposition === "RETEST" &&
    (!selectedAstRun ||
      (form.retestScope === "SINGLE_ANTIBIOTIC" && !form.retestAntibioticId));
  const requiredMissing =
    !form.categoryId ||
    !form.reportingUnitId ||
    !form.severity ||
    !form.description.trim();

  const update = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }));
    setError("");
  };

  const submit = () => {
    if (requiredMissing || lostConfigurationMissing || retestSelectionMissing) {
      setError(intl.formatMessage({ id: "microbiology.nce.required" }));
      return;
    }
    setSaving(true);
    service
      .reportCaseNonconformance(caseId, {
        categoryId: form.categoryId,
        typeId: form.typeId,
        reportingUnitId: Number(form.reportingUnitId),
        severity: form.severity,
        title: form.title.trim(),
        description: form.description.trim(),
        immediateAction: form.immediateAction.trim(),
        disposition: lostMode ? "REJECT_TEST" : form.disposition,
        eventType: lostMode ? "SPECIMEN_LOST" : "NONCONFORMANCE",
        ...(form.disposition === "RETEST"
          ? {
              sourceAstRunId: selectedAstRun.id,
              astTechnique: selectedAstRun.technique,
              orderedAntibioticIds:
                form.retestScope === "SINGLE_ANTIBIOTIC"
                  ? [form.retestAntibioticId]
                  : [],
            }
          : {}),
      })
      .then(onComplete)
      .catch(() => {
        setError(intl.formatMessage({ id: "microbiology.nce.submitError" }));
      })
      .finally(() => setSaving(false));
  };

  return (
    <section
      className="microbiology-card"
      data-testid="microbiology-nce-panel"
      aria-labelledby="microbiology-nce-panel-title"
      aria-busy={loading || saving}
    >
      <Stack gap={5}>
        <div>
          <h3 ref={titleRef} id="microbiology-nce-panel-title" tabIndex={-1}>
            {intl.formatMessage({
              id: lostMode
                ? "microbiology.nce.markLostTitle"
                : "microbiology.nce.reportTitle",
            })}
          </h3>
          <p>
            {intl.formatMessage({
              id: lostMode
                ? "microbiology.nce.markLostHint"
                : "microbiology.nce.reportHint",
            })}
          </p>
        </div>

        {lostConfigurationMissing && (
          <InlineNotification
            kind="error"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({
              id: "microbiology.nce.configurationBlockerTitle",
            })}
            subtitle={intl.formatMessage({
              id: "microbiology.nce.lostTypeMissing",
            })}
          />
        )}
        {error && (
          <InlineNotification
            kind="error"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({ id: "microbiology.nce.errorTitle" })}
            subtitle={error}
          />
        )}

        <div className="microbiology-form-grid">
          <Select
            id="microbiology-nce-category"
            labelText={intl.formatMessage({ id: "microbiology.nce.category" })}
            value={form.categoryId}
            disabled={loading || lostMode}
            onChange={(event) => {
              update("categoryId", event.target.value);
              update("typeId", "");
            }}
          >
            <SelectItem value="" text="" />
            {categories.map((category) => (
              <SelectItem
                key={category.id}
                value={String(category.id)}
                text={category.name}
              />
            ))}
          </Select>
          <Select
            id="microbiology-nce-type"
            labelText={intl.formatMessage({ id: "microbiology.nce.type" })}
            value={form.typeId}
            disabled={loading || lostMode || !form.categoryId}
            onChange={(event) => update("typeId", event.target.value)}
          >
            <SelectItem value="" text="" />
            {types.map((type) => (
              <SelectItem
                key={type.id}
                value={String(type.id)}
                text={type.name}
              />
            ))}
          </Select>
          <Select
            id="microbiology-nce-reporting-unit"
            labelText={intl.formatMessage({
              id: "microbiology.nce.reportingUnit",
            })}
            value={form.reportingUnitId}
            disabled={loading}
            onChange={(event) => update("reportingUnitId", event.target.value)}
          >
            <SelectItem value="" text="" />
            {reportingUnits.map((unit) => (
              <SelectItem
                key={unit.id}
                value={String(unit.id)}
                text={unit.value}
              />
            ))}
          </Select>
          <TextInput
            id="microbiology-nce-title"
            labelText={intl.formatMessage({ id: "microbiology.nce.title" })}
            value={form.title}
            onChange={(event) => update("title", event.target.value)}
          />
        </div>

        <RadioButtonGroup
          legendText={intl.formatMessage({ id: "microbiology.nce.severity" })}
          name="microbiology-nce-severity"
          valueSelected={form.severity}
          onChange={(value) => update("severity", value)}
        >
          <RadioButton
            id="microbiology-nce-severity-minor"
            labelText={intl.formatMessage({ id: "microbiology.nce.minor" })}
            value="MINOR"
          />
          <RadioButton
            id="microbiology-nce-severity-major"
            labelText={intl.formatMessage({ id: "microbiology.nce.major" })}
            value="MAJOR"
          />
          <RadioButton
            id="microbiology-nce-severity-critical"
            labelText={intl.formatMessage({ id: "microbiology.nce.critical" })}
            value="CRITICAL"
          />
        </RadioButtonGroup>

        <TextArea
          id="microbiology-nce-description"
          labelText={intl.formatMessage({ id: "microbiology.nce.description" })}
          value={form.description}
          onChange={(event) => update("description", event.target.value)}
        />
        <TextArea
          id="microbiology-nce-immediate-action"
          labelText={intl.formatMessage({
            id: "microbiology.nce.immediateAction",
          })}
          value={form.immediateAction}
          onChange={(event) => update("immediateAction", event.target.value)}
        />

        <RadioButtonGroup
          legendText={intl.formatMessage({
            id: "microbiology.nce.disposition",
          })}
          name="microbiology-nce-disposition"
          valueSelected={lostMode ? "REJECT_TEST" : form.disposition}
          onChange={(value) => {
            update("disposition", value);
            update("sourceAstRunId", "");
            update("retestAntibioticId", "");
            setAstRuns([]);
            setAntibiotics([]);
            setLoadingRetest(value === "RETEST");
          }}
        >
          {!lostMode && (
            <RadioButton
              id="microbiology-nce-flag-only"
              labelText={intl.formatMessage({
                id: "microbiology.nce.flagOnly",
              })}
              value="FLAG_ONLY"
            />
          )}
          <RadioButton
            id="microbiology-nce-reject-tests"
            labelText={intl.formatMessage({
              id: "microbiology.nce.rejectTests",
            })}
            value="REJECT_TEST"
          />
          {!lostMode && (
            <RadioButton
              id="microbiology-nce-retest"
              labelText={intl.formatMessage({
                id: "microbiology.nce.retest",
              })}
              value="RETEST"
            />
          )}
        </RadioButtonGroup>

        {!lostMode && form.disposition === "RETEST" ? (
          <Stack gap={4}>
            <Select
              id="microbiology-nce-source-ast-run"
              labelText={intl.formatMessage({
                id: "microbiology.nce.sourceAstRun",
              })}
              value={form.sourceAstRunId}
              disabled={loadingRetest}
              onChange={(event) => {
                update("sourceAstRunId", event.target.value);
                update("retestAntibioticId", "");
              }}
            >
              <SelectItem value="" text="" />
              {astRuns.map((run) => (
                <SelectItem
                  key={run.id}
                  value={run.id}
                  text={`${run.isolateLabel} · ${formatMicrobiologyEnum(
                    run.technique,
                    intl,
                  )} · ${run.id}`}
                />
              ))}
            </Select>
            <RadioButtonGroup
              legendText={intl.formatMessage({
                id: "microbiology.ast.attemptScope",
              })}
              name="microbiology-nce-retest-scope"
              valueSelected={form.retestScope}
              onChange={(value) => {
                update("retestScope", value);
                update("retestAntibioticId", "");
              }}
            >
              <RadioButton
                id="microbiology-nce-retest-whole-panel"
                labelText={intl.formatMessage({
                  id: "microbiology.ast.wholePanel",
                })}
                value="WHOLE_PANEL"
              />
              <RadioButton
                id="microbiology-nce-retest-single-antibiotic"
                labelText={intl.formatMessage({
                  id: "microbiology.ast.singleAntibiotic",
                })}
                value="SINGLE_ANTIBIOTIC"
              />
            </RadioButtonGroup>
            {form.retestScope === "SINGLE_ANTIBIOTIC" ? (
              <Select
                id="microbiology-nce-retest-antibiotic"
                labelText={intl.formatMessage({
                  id: "microbiology.ast.antibioticToRepeat",
                })}
                value={form.retestAntibioticId}
                disabled={!selectedAstRun}
                onChange={(event) =>
                  update("retestAntibioticId", event.target.value)
                }
              >
                <SelectItem value="" text="" />
                {(selectedAstRun?.orderedAntibiotics || []).map((ordered) => {
                  const antibiotic = antibiotics.find(
                    (item) => item.id === ordered.antibioticId,
                  );
                  return (
                    <SelectItem
                      key={ordered.antibioticId}
                      value={ordered.antibioticId}
                      text={antibiotic?.label || ordered.antibioticId}
                    />
                  );
                })}
              </Select>
            ) : null}
          </Stack>
        ) : null}

        <div className="microbiology-form-actions">
          <Button kind="secondary" type="button" onClick={onCancel}>
            {intl.formatMessage({ id: "button.cancel" })}
          </Button>
          <Button
            type="button"
            disabled={
              loading ||
              saving ||
              loadingRetest ||
              lostConfigurationMissing ||
              retestSelectionMissing
            }
            onClick={submit}
          >
            {intl.formatMessage({
              id: lostMode
                ? "microbiology.nce.markLost"
                : "microbiology.nce.report",
            })}
          </Button>
        </div>
      </Stack>
    </section>
  );
};

export default CaseNonconformancePanel;
