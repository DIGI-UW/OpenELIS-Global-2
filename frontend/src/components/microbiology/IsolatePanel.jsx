import React, { useEffect, useMemo, useState } from "react";
import {
  Button,
  NumberInput,
  Select,
  SelectItem,
  Tag,
  TextArea,
  TextInput,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { formatMicrobiologyEnum } from "./MicrobiologyLabels";
import MicrobiologyService from "./MicrobiologyService";

const SIGNIFICANCE_OPTIONS = [
  { value: "UNKNOWN", labelId: "microbiology.isolate.unknown" },
  {
    value: "CLINICALLY_SIGNIFICANT",
    labelId: "microbiology.isolate.significant",
  },
  { value: "CONTAMINANT", labelId: "microbiology.isolate.contaminant" },
  { value: "NORMAL_FLORA", labelId: "microbiology.isolate.normalFlora" },
];
const IDENTIFICATION_METHOD_OPTIONS = [
  "MALDI_TOF",
  "VITEK_2",
  "MANUAL_BIOCHEMISTRY",
  "PCR",
];

const IsolatePanel = ({
  caseId,
  isolates = [],
  onCreateIsolate,
  onUpdateIdentification,
  saving,
  readOnly = false,
  amendmentOpen = false,
  onLogCritical,
  service = MicrobiologyService,
}) => {
  const intl = useIntl();
  const [isolateLabel, setIsolateLabel] = useState("ISO-1");
  const [gramStain, setGramStain] = useState("");
  const [colonyMorphology, setColonyMorphology] = useState("");
  const [organismId, setOrganismId] = useState("");
  const [preliminaryOrganismText, setPreliminaryOrganismText] = useState("");
  const [significance, setSignificance] = useState("CLINICALLY_SIGNIFICANT");
  const [identificationMethod, setIdentificationMethod] = useState("");
  const [identificationConfidence, setIdentificationConfidence] =
    useState(99.5);
  const [editingIsolateId, setEditingIsolateId] = useState("");
  const [identificationReason, setIdentificationReason] = useState("");
  const [organisms, setOrganisms] = useState([]);
  const [identificationHistory, setIdentificationHistory] = useState({});

  useEffect(() => {
    if (!service.getOrganisms) {
      return;
    }
    service
      .getOrganisms()
      .then((items) => setOrganisms(Array.isArray(items) ? items : []));
  }, [service]);

  useEffect(() => {
    if (!service.getIdentificationHistory || isolates.length === 0) {
      return;
    }
    let active = true;
    Promise.all(
      isolates.map((isolate) =>
        service
          .getIdentificationHistory(isolate.id)
          .then((history) => [
            isolate.id,
            Array.isArray(history) ? history : [],
          ]),
      ),
    )
      .then((entries) => {
        if (active) {
          setIdentificationHistory(Object.fromEntries(entries));
        }
      })
      .catch(() => {
        if (active) {
          setIdentificationHistory({});
        }
      });
    return () => {
      active = false;
    };
  }, [isolates, service]);

  const organismLabels = useMemo(
    () =>
      Object.fromEntries(
        organisms.map((organism) => [organism.id, organism.label]),
      ),
    [organisms],
  );

  const resetForm = () => {
    setEditingIsolateId("");
    setIsolateLabel("ISO-1");
    setGramStain("");
    setColonyMorphology("");
    setOrganismId("");
    setPreliminaryOrganismText("");
    setSignificance("CLINICALLY_SIGNIFICANT");
    setIdentificationMethod("");
    setIdentificationConfidence(99.5);
    setIdentificationReason("");
  };

  const submit = () => {
    const payload = {
      caseId,
      isolateLabel,
      gramStain,
      colonyMorphology,
      significance,
    };
    if (editingIsolateId) {
      onUpdateIdentification(editingIsolateId, {
        organismId,
        preliminaryOrganismText:
          organismLabels[organismId] || preliminaryOrganismText,
        significance,
        identificationStatus: "CONFIRMED",
        identificationMethod,
        identificationConfidence: Number(identificationConfidence),
        ...(amendmentOpen
          ? { identificationReason: identificationReason.trim() }
          : {}),
      });
    } else {
      onCreateIsolate(payload);
    }
    resetForm();
  };

  const editIsolate = (isolate) => {
    setEditingIsolateId(isolate.id);
    setIsolateLabel(isolate.isolateLabel);
    setGramStain(isolate.gramStain || "");
    setColonyMorphology(isolate.colonyMorphology || "");
    setOrganismId(isolate.organismId || "");
    setPreliminaryOrganismText(isolate.preliminaryOrganismText || "");
    setSignificance(isolate.significance || "UNKNOWN");
    setIdentificationMethod(isolate.identificationMethod || "");
    setIdentificationConfidence(isolate.identificationConfidence ?? 99.5);
  };

  const submitDisabled = Boolean(
    saving ||
    readOnly ||
    !isolateLabel.trim() ||
    (!editingIsolateId && !gramStain.trim()) ||
    (editingIsolateId &&
      (!organismId ||
        !identificationMethod ||
        identificationConfidence === "")) ||
    (editingIsolateId && amendmentOpen && !identificationReason.trim()),
  );

  return (
    <section
      className="microbiology-card"
      data-testid="microbiology-isolates-card"
      aria-labelledby="microbiology-isolates-heading"
    >
      <div className="microbiology-card__header">
        <div>
          <h3 id="microbiology-isolates-heading">
            {intl.formatMessage({ id: "microbiology.case.isolates" })}
          </h3>
          <p className="microbiology-card__hint">
            {intl.formatMessage({ id: "microbiology.case.isolates.hint" })}
          </p>
        </div>
        <Tag type={isolates.length > 0 ? "green" : "cool-gray"}>
          {isolates.length}
        </Tag>
      </div>
      <div>
        {isolates.length === 0 ? (
          <p>
            {intl.formatMessage({ id: "microbiology.case.isolates.empty" })}
          </p>
        ) : (
          <ul className="microbiology-list">
            {isolates.map((isolate) => (
              <li
                className="microbiology-list__row microbiology-isolate-row"
                key={isolate.id}
              >
                <div>
                  <strong>{isolate.isolateLabel}</strong>
                  {organismLabels[isolate.organismId] ||
                  isolate.preliminaryOrganismText
                    ? `: ${
                        organismLabels[isolate.organismId] ||
                        isolate.preliminaryOrganismText
                      }`
                    : ""}
                  <Tag
                    type={
                      isolate.identificationStatus === "CONFIRMED"
                        ? "green"
                        : "warm-gray"
                    }
                  >
                    {intl.formatMessage({
                      id:
                        isolate.identificationStatus === "CONFIRMED"
                          ? "microbiology.isolate.identified"
                          : "microbiology.isolate.pending",
                    })}
                  </Tag>
                  <dl className="microbiology-isolate-details">
                    <div>
                      <dt>
                        {intl.formatMessage({
                          id: "microbiology.isolate.gramStain",
                        })}
                      </dt>
                      <dd>{isolate.gramStain || "-"}</dd>
                    </div>
                    <div>
                      <dt>
                        {intl.formatMessage({
                          id: "microbiology.isolate.colonyMorphology",
                        })}
                      </dt>
                      <dd>{isolate.colonyMorphology || "-"}</dd>
                    </div>
                    {isolate.identificationStatus === "CONFIRMED" && (
                      <div>
                        <dt>
                          {intl.formatMessage({
                            id: "microbiology.isolate.identification",
                          })}
                        </dt>
                        <dd>
                          {formatMicrobiologyEnum(
                            isolate.identificationMethod,
                            intl,
                          )}{" "}
                          · {isolate.identificationConfidence}% ·{" "}
                          {formatMicrobiologyEnum(isolate.significance, intl)}
                        </dd>
                      </div>
                    )}
                  </dl>
                  {(identificationHistory[isolate.id] || []).length > 0 && (
                    <div className="microbiology-identification-history">
                      <strong>
                        {intl.formatMessage({
                          id: "microbiology.isolate.identificationHistory",
                        })}
                      </strong>
                      <ol>
                        {identificationHistory[isolate.id].map((event) => (
                          <li key={event.id}>
                            {intl.formatMessage(
                              {
                                id: "microbiology.isolate.identificationChange",
                              },
                              {
                                previous:
                                  event.previousOrganismText ||
                                  event.previousOrganismId ||
                                  intl.formatMessage({
                                    id: "microbiology.isolate.unidentified",
                                  }),
                                next:
                                  event.newOrganismText ||
                                  event.newOrganismId ||
                                  intl.formatMessage({
                                    id: "microbiology.isolate.unidentified",
                                  }),
                                reason: event.reason,
                              },
                            )}
                            {event.changedBy && (
                              <span className="microbiology-list__meta">
                                {event.changedBy}
                              </span>
                            )}
                          </li>
                        ))}
                      </ol>
                    </div>
                  )}
                </div>
                <div className="microbiology-isolate-row__actions">
                  {onLogCritical && (
                    <Button
                      kind="ghost"
                      size="sm"
                      onClick={() => onLogCritical(isolate)}
                    >
                      {intl.formatMessage(
                        {
                          id: "microbiology.critical.logForIsolate",
                        },
                        { isolate: isolate.isolateLabel },
                      )}
                    </Button>
                  )}
                  <Button
                    kind={
                      isolate.identificationStatus === "CONFIRMED"
                        ? "ghost"
                        : "tertiary"
                    }
                    size="sm"
                    onClick={() => editIsolate(isolate)}
                    disabled={readOnly}
                  >
                    {intl.formatMessage({
                      id: amendmentOpen
                        ? "microbiology.isolate.reidentify"
                        : isolate.identificationStatus === "CONFIRMED"
                          ? "microbiology.isolate.edit"
                          : "microbiology.isolate.identify",
                    })}
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        )}
        <div className="microbiology-form-grid microbiology-form-grid--three">
          <TextInput
            id="microbiology-isolate-label"
            labelText={intl.formatMessage({
              id: "microbiology.case.isolateLabel",
            })}
            value={isolateLabel}
            disabled={readOnly || Boolean(editingIsolateId)}
            onChange={(event) => setIsolateLabel(event.target.value)}
          />
          <TextInput
            id="microbiology-isolate-gram-stain"
            labelText={intl.formatMessage({
              id: "microbiology.isolate.gramStain",
            })}
            value={gramStain}
            disabled={readOnly || Boolean(editingIsolateId)}
            onChange={(event) => setGramStain(event.target.value)}
          />
          <TextInput
            id="microbiology-isolate-colony-morphology"
            labelText={intl.formatMessage({
              id: "microbiology.isolate.colonyMorphology",
            })}
            value={colonyMorphology}
            disabled={readOnly || Boolean(editingIsolateId)}
            onChange={(event) => setColonyMorphology(event.target.value)}
          />
          <Select
            id="microbiology-isolate-significance"
            labelText={intl.formatMessage({
              id: "microbiology.case.significance",
            })}
            value={significance}
            disabled={readOnly}
            onChange={(event) => setSignificance(event.target.value)}
          >
            {SIGNIFICANCE_OPTIONS.map((option) => (
              <SelectItem
                key={option.value}
                value={option.value}
                text={intl.formatMessage({ id: option.labelId })}
              />
            ))}
          </Select>
          {editingIsolateId && (
            <Select
              id="microbiology-isolate-organism"
              labelText={intl.formatMessage({
                id: "microbiology.isolate.organism",
              })}
              value={organismId}
              disabled={readOnly}
              onChange={(event) => setOrganismId(event.target.value)}
            >
              <SelectItem
                value=""
                text={intl.formatMessage({
                  id: "microbiology.isolate.organism.select",
                })}
              />
              {organisms.map((organism) => (
                <SelectItem
                  key={organism.id}
                  value={organism.id}
                  text={organism.label}
                />
              ))}
            </Select>
          )}
          {editingIsolateId && (
            <Select
              id="microbiology-isolate-identification-method"
              labelText={intl.formatMessage({
                id: "microbiology.isolate.identificationMethod",
              })}
              value={identificationMethod}
              disabled={readOnly}
              onChange={(event) => setIdentificationMethod(event.target.value)}
            >
              <SelectItem value="" text="" />
              {IDENTIFICATION_METHOD_OPTIONS.map((methodOption) => (
                <SelectItem
                  key={methodOption}
                  value={methodOption}
                  text={formatMicrobiologyEnum(methodOption, intl)}
                />
              ))}
            </Select>
          )}
          {editingIsolateId && (
            <NumberInput
              id="microbiology-isolate-identification-confidence"
              label={intl.formatMessage({
                id: "microbiology.isolate.identificationConfidence",
              })}
              min={0}
              max={100}
              step={0.1}
              value={identificationConfidence}
              disabled={readOnly}
              onChange={(event, state) =>
                setIdentificationConfidence(state?.value ?? event.target.value)
              }
            />
          )}
          {editingIsolateId && amendmentOpen && (
            <div className="microbiology-form-grid__wide">
              <TextArea
                id="microbiology-isolate-identification-reason"
                labelText={intl.formatMessage({
                  id: "microbiology.isolate.identificationReason",
                })}
                value={identificationReason}
                disabled={readOnly}
                onChange={(event) =>
                  setIdentificationReason(event.target.value)
                }
              />
            </div>
          )}
          <div className="microbiology-isolate-actions">
            <Button onClick={submit} disabled={submitDisabled}>
              {intl.formatMessage({
                id: editingIsolateId
                  ? "microbiology.isolate.update"
                  : "microbiology.case.createIsolate",
              })}
            </Button>
            {editingIsolateId && (
              <Button kind="ghost" onClick={resetForm} disabled={saving}>
                {intl.formatMessage({ id: "button.cancel" })}
              </Button>
            )}
          </div>
        </div>
      </div>
    </section>
  );
};

export default IsolatePanel;
