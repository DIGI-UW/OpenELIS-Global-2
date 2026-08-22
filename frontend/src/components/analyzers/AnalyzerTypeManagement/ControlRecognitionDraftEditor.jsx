import React, { useEffect, useMemo, useRef, useState } from "react";
import {
  Button,
  Checkbox,
  InlineNotification,
  Loading,
  RadioButton,
  RadioButtonGroup,
  Select,
  SelectItem,
  TextInput,
} from "@carbon/react";
import { Add, TrashCan } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import {
  getAnalyzerTypeControlRecognition,
  updateAnalyzerTypeControlRecognition,
} from "../../../services/analyzerService";
import {
  formatRecognitionCondition,
  formatRecognitionMode,
} from "./recognitionText";

const hasError = (response) => !response || Boolean(response.error);

const toForm = (recognition) => ({
  mode: recognition?.mode || null,
  affirmedNoControlResults: Boolean(recognition?.affirmedNoControlResults),
  conditions: (recognition?.conditions || []).map((condition, index) => ({
    ...condition,
    clientKey: condition.key || `loaded-${index + 1}`,
  })),
  availableSources: recognition?.availableSources || [],
});

const toUpdate = (form) => ({
  mode: form.mode,
  affirmedNoControlResults:
    form.mode === "NONE" && form.affirmedNoControlResults,
  conditions:
    form.mode === "RULES"
      ? form.conditions.map((condition) => ({
          key: condition.key || null,
          kind: condition.kind,
          sourceKey: condition.sourceKey || null,
          value: condition.value || null,
          controlLevel: condition.controlLevel || null,
          controlType: condition.controlType || null,
        }))
      : [],
});

const validForm = (form) => {
  if (form.mode === "NONE") {
    return form.affirmedNoControlResults;
  }
  if (form.mode !== "RULES" || form.conditions.length === 0) {
    return false;
  }
  return form.conditions.every(
    (condition) =>
      !condition.editable || Boolean((condition.value || "").trim()),
  );
};

const ControlRecognitionDraftEditor = ({ draftId, onStateChange }) => {
  const intl = useIntl();
  const nextClientKey = useRef(1);
  const [draft, setDraft] = useState(null);
  const [form, setForm] = useState(toForm(null));
  const [baseline, setBaseline] = useState(null);
  const [newCondition, setNewCondition] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    getAnalyzerTypeControlRecognition(draftId, (response) => {
      setLoading(false);
      if (hasError(response) || !response.recognition) {
        setError(
          response?.error ||
            intl.formatMessage({
              id: "analyzerType.recognition.error.load",
            }),
        );
        return;
      }
      const nextForm = toForm(response.recognition);
      setDraft(response);
      setForm(nextForm);
      setBaseline(JSON.stringify(toUpdate(nextForm)));
    });
  }, [draftId, intl]);

  const update = toUpdate(form);
  const dirty = baseline !== null && JSON.stringify(update) !== baseline;
  const valid = validForm(form);
  const publishable =
    Boolean(draft) &&
    !loading &&
    !dirty &&
    valid &&
    (draft.validationIssues || []).length === 0;

  useEffect(() => {
    onStateChange?.({
      loaded: Boolean(draft) && !loading,
      dirty,
      valid,
      publishable,
      validationIssues: draft?.validationIssues || [],
    });
  }, [dirty, draft, loading, onStateChange, publishable, valid]);

  const changeMode = (mode) => {
    setSaved(false);
    setForm((current) => ({
      ...current,
      mode,
      affirmedNoControlResults:
        mode === "NONE" ? false : current.affirmedNoControlResults,
    }));
  };

  const changeCondition = (clientKey, value) => {
    setSaved(false);
    setForm((current) => ({
      ...current,
      conditions: current.conditions.map((condition) =>
        condition.clientKey === clientKey ? { ...condition, value } : condition,
      ),
    }));
  };

  const removeCondition = (clientKey) => {
    setSaved(false);
    setForm((current) => ({
      ...current,
      conditions: current.conditions.filter(
        (condition) => condition.clientKey !== clientKey,
      ),
    }));
  };

  const conditionChoices = useMemo(
    () => [
      {
        value: "SPECIMEN_ID_STARTS_WITH|",
        label: intl.formatMessage({
          id: "analyzerType.recognition.condition.specimenPrefix",
        }),
      },
      ...form.availableSources.flatMap((source) => [
        {
          value: `FIELD_VALUE_EQUALS|${source.key}`,
          label: intl.formatMessage(
            { id: "analyzerType.recognition.condition.sourceEquals" },
            { source: source.label },
          ),
        },
        {
          value: `FIELD_VALUE_CONTAINS|${source.key}`,
          label: intl.formatMessage(
            { id: "analyzerType.recognition.condition.sourceContains" },
            { source: source.label },
          ),
        },
      ]),
    ],
    [form.availableSources, intl],
  );

  const addCondition = () => {
    if (!newCondition) {
      return;
    }
    const [kind, sourceKey] = newCondition.split("|");
    const source = form.availableSources.find(
      (candidate) => candidate.key === sourceKey,
    );
    const clientKey = `new-${nextClientKey.current++}`;
    setSaved(false);
    setForm((current) => ({
      ...current,
      conditions: [
        ...current.conditions,
        {
          clientKey,
          key: null,
          kind,
          sourceKey: sourceKey || null,
          sourceLabel:
            kind === "SPECIMEN_ID_STARTS_WITH"
              ? intl.formatMessage({
                  id: "analyzerType.recognition.source.specimenId",
                })
              : source?.label,
          description: null,
          value: "",
          editable: true,
          controlLevel: null,
          controlType: null,
        },
      ],
    }));
    setNewCondition("");
  };

  const save = () => {
    if (!dirty || !valid || saving) {
      return;
    }
    setSaving(true);
    setError(null);
    setSaved(false);
    updateAnalyzerTypeControlRecognition(draftId, update, (response) => {
      setSaving(false);
      if (hasError(response) || !response.recognition) {
        setError(
          response?.error ||
            intl.formatMessage({
              id: "analyzerType.recognition.error.save",
            }),
        );
        return;
      }
      const nextForm = toForm(response.recognition);
      setDraft(response);
      setForm(nextForm);
      setBaseline(JSON.stringify(toUpdate(nextForm)));
      setSaved(true);
    });
  };

  if (loading) {
    return (
      <div className="analyzer-type-recognition__loading">
        <Loading
          withOverlay={false}
          small
          description={intl.formatMessage({
            id: "analyzerType.recognition.loading",
          })}
        />
      </div>
    );
  }

  if (!draft || !draft.recognition) {
    return (
      <InlineNotification
        kind="error"
        lowContrast
        hideCloseButton
        title={intl.formatMessage({
          id: "analyzerType.recognition.error.title",
        })}
        subtitle={error}
      />
    );
  }

  return (
    <section
      className="analyzer-type-recognition"
      aria-labelledby="analyzer-type-recognition-heading"
    >
      <div>
        <h3 id="analyzer-type-recognition-heading">
          {intl.formatMessage({
            id: "analyzerType.recognition.heading",
          })}
        </h3>
        <p>{formatRecognitionMode(intl, form.mode)}</p>
      </div>

      <RadioButtonGroup
        name={`control-recognition-mode-${draftId}`}
        legendText={intl.formatMessage({
          id: "analyzerType.recognition.mode",
        })}
        valueSelected={form.mode || ""}
        onChange={changeMode}
        orientation="vertical"
      >
        <RadioButton
          id={`control-recognition-rules-${draftId}`}
          value="RULES"
          labelText={intl.formatMessage({
            id: "analyzerType.recognition.mode.rules",
          })}
        />
        <RadioButton
          id={`control-recognition-none-${draftId}`}
          value="NONE"
          labelText={intl.formatMessage({
            id: "analyzerType.recognition.mode.none",
          })}
        />
      </RadioButtonGroup>

      {form.mode === "RULES" && (
        <div className="analyzer-type-recognition__conditions">
          <p>
            {intl.formatMessage({
              id: "analyzerType.recognition.rules.help",
            })}
          </p>
          {form.conditions.map((condition) => {
            const conditionText = condition.key
              ? formatRecognitionCondition(intl, condition)
              : null;
            return (
              <div
                className="analyzer-type-recognition__condition"
                key={condition.clientKey}
                role="group"
                aria-label={
                  conditionText ||
                  intl.formatMessage({
                    id: "analyzerType.recognition.condition.new",
                  })
                }
              >
                <div className="analyzer-type-recognition__condition-content">
                  {conditionText && <p>{conditionText}</p>}
                  {condition.editable && (
                    <TextInput
                      id={`control-recognition-${draftId}-${condition.clientKey}`}
                      labelText={intl.formatMessage(
                        {
                          id:
                            condition.kind === "SPECIMEN_ID_STARTS_WITH"
                              ? "analyzerType.recognition.condition.prefixValue"
                              : "analyzerType.recognition.condition.fieldValue",
                        },
                        { source: condition.sourceLabel },
                      )}
                      value={condition.value || ""}
                      onChange={(event) =>
                        changeCondition(condition.clientKey, event.target.value)
                      }
                    />
                  )}
                </div>
                <Button
                  kind="ghost"
                  size="sm"
                  hasIconOnly
                  renderIcon={TrashCan}
                  iconDescription={intl.formatMessage({
                    id: "analyzerType.recognition.condition.remove",
                  })}
                  onClick={() => removeCondition(condition.clientKey)}
                />
              </div>
            );
          })}
          <div className="analyzer-type-recognition__add">
            <Select
              id={`control-recognition-new-${draftId}`}
              aria-label={intl.formatMessage({
                id: "analyzerType.recognition.condition.select",
              })}
              labelText={intl.formatMessage({
                id: "analyzerType.recognition.condition.select",
              })}
              value={newCondition}
              onChange={(event) => setNewCondition(event.target.value)}
            >
              <SelectItem
                value=""
                text={intl.formatMessage({
                  id: "analyzerType.recognition.condition.select.placeholder",
                })}
              />
              {conditionChoices.map((choice) => (
                <SelectItem
                  key={choice.value}
                  value={choice.value}
                  text={choice.label}
                />
              ))}
            </Select>
            <Button
              kind="tertiary"
              size="sm"
              renderIcon={Add}
              disabled={!newCondition}
              onClick={addCondition}
            >
              {intl.formatMessage({
                id: "analyzerType.recognition.condition.add",
              })}
            </Button>
          </div>
        </div>
      )}

      {form.mode === "NONE" && (
        <div className="analyzer-type-recognition__none">
          <InlineNotification
            kind="warning"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({
              id: "analyzerType.recognition.none.title",
            })}
            subtitle={intl.formatMessage({
              id: "analyzerType.recognition.none.subtitle",
            })}
          />
          <Checkbox
            id={`control-recognition-none-affirmation-${draftId}`}
            aria-label={intl.formatMessage({
              id: "analyzerType.recognition.none.affirmation",
            })}
            labelText={intl.formatMessage({
              id: "analyzerType.recognition.none.affirmation",
            })}
            checked={form.affirmedNoControlResults}
            onChange={(_, state) =>
              setForm((current) => ({
                ...current,
                affirmedNoControlResults: state.checked,
              }))
            }
          />
        </div>
      )}

      {error && (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "analyzerType.recognition.error.title",
          })}
          subtitle={error}
        />
      )}
      {saved && (
        <InlineNotification
          kind="success"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "analyzerType.recognition.saved",
          })}
        />
      )}
      {(draft.validationIssues || []).length > 0 && (
        <InlineNotification
          kind="warning"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "analyzerType.recognition.validation.title",
          })}
          subtitle={(draft.validationIssues || []).join(" ")}
        />
      )}
      <Button
        kind="secondary"
        size="sm"
        disabled={!dirty || !valid || saving}
        onClick={save}
      >
        {intl.formatMessage({
          id: "analyzerType.recognition.save",
        })}
      </Button>
    </section>
  );
};

export default ControlRecognitionDraftEditor;
