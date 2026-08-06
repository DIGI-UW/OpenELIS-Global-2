import React, { useEffect, useMemo, useState } from "react";
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

const initialForm = {
  categoryId: "",
  typeId: "",
  reportingUnitId: "",
  severity: "",
  title: "",
  description: "",
  immediateAction: "",
  disposition: "FLAG_ONLY",
};

const CaseNonconformancePanel = ({
  caseId,
  mode,
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
        if (lostMode) {
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

  const selectedCategory = useMemo(
    () =>
      categories.find(
        (category) => String(category.id) === String(form.categoryId),
      ),
    [categories, form.categoryId],
  );
  const types = selectedCategory?.types || [];
  const lostConfigurationMissing = lostMode && !loading && !form.typeId;
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
    if (requiredMissing || lostConfigurationMissing) {
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
      })
      .then(onComplete)
      .catch(() => {
        setError(intl.formatMessage({ id: "microbiology.nce.submitError" }));
      })
      .finally(() => setSaving(false));
  };

  return (
    <section className="microbiology-card" aria-busy={loading || saving}>
      <Stack gap={5}>
        <div>
          <h3>
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
          onChange={(value) => update("disposition", value)}
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
        </RadioButtonGroup>

        <div className="microbiology-form-actions">
          <Button kind="secondary" type="button" onClick={onCancel}>
            {intl.formatMessage({ id: "button.cancel" })}
          </Button>
          <Button
            type="button"
            disabled={loading || saving || lostConfigurationMissing}
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
