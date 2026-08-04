import React, { useEffect, useMemo, useState } from "react";
import {
  Button,
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
const IDENTIFICATION_STATUS_OPTIONS = ["PRELIMINARY", "CONFIRMED"];

const IsolatePanel = ({
  caseId,
  isolates = [],
  onCreateIsolate,
  onUpdateIdentification,
  saving,
  readOnly = false,
  amendmentOpen = false,
  service = MicrobiologyService,
}) => {
  const intl = useIntl();
  const [isolateLabel, setIsolateLabel] = useState("ISO-1");
  const [organismId, setOrganismId] = useState("");
  const [preliminaryOrganismText, setPreliminaryOrganismText] = useState("");
  const [significance, setSignificance] = useState("CLINICALLY_SIGNIFICANT");
  const [identificationStatus, setIdentificationStatus] =
    useState("PRELIMINARY");
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
      setIdentificationHistory({});
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
    setOrganismId("");
    setPreliminaryOrganismText("");
    setSignificance("CLINICALLY_SIGNIFICANT");
    setIdentificationStatus("PRELIMINARY");
    setIdentificationReason("");
  };

  const submit = () => {
    const payload = {
      caseId,
      isolateLabel,
      preliminaryOrganismText,
      significance,
    };
    if (organismId) {
      payload.organismId = organismId;
    }
    if (editingIsolateId) {
      onUpdateIdentification(editingIsolateId, {
        organismId,
        preliminaryOrganismText,
        significance,
        identificationStatus,
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
    setOrganismId(isolate.organismId || "");
    setPreliminaryOrganismText(isolate.preliminaryOrganismText || "");
    setSignificance(isolate.significance || "UNKNOWN");
    setIdentificationStatus(isolate.identificationStatus || "PRELIMINARY");
  };

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
              <li className="microbiology-list__row" key={isolate.id}>
                <div>
                  <strong>{isolate.isolateLabel}</strong>
                  {organismLabels[isolate.organismId] ||
                  isolate.preliminaryOrganismText
                    ? `: ${
                        organismLabels[isolate.organismId] ||
                        isolate.preliminaryOrganismText
                      }`
                    : ""}
                  <div className="microbiology-list__meta">
                    {formatMicrobiologyEnum(isolate.significance, intl)} ·{" "}
                    {formatMicrobiologyEnum(isolate.identificationStatus, intl)}
                  </div>
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
                <Button
                  kind="ghost"
                  size="sm"
                  onClick={() => editIsolate(isolate)}
                  disabled={readOnly}
                >
                  {intl.formatMessage({ id: "microbiology.isolate.edit" })}
                </Button>
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
                id: "microbiology.isolate.organism.freeText",
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
          <TextInput
            id="microbiology-preliminary-organism"
            labelText={intl.formatMessage({
              id: "microbiology.case.preliminaryOrganism",
            })}
            value={preliminaryOrganismText}
            disabled={readOnly}
            onChange={(event) => setPreliminaryOrganismText(event.target.value)}
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
              id="microbiology-isolate-identification-status"
              labelText={intl.formatMessage({
                id: "microbiology.isolate.identificationStatus",
              })}
              value={identificationStatus}
              disabled={readOnly}
              onChange={(event) => setIdentificationStatus(event.target.value)}
            >
              {IDENTIFICATION_STATUS_OPTIONS.map((status) => (
                <SelectItem
                  key={status}
                  value={status}
                  text={formatMicrobiologyEnum(status, intl)}
                />
              ))}
            </Select>
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
            <Button
              onClick={submit}
              disabled={
                saving ||
                readOnly ||
                !isolateLabel.trim() ||
                (!preliminaryOrganismText.trim() && !organismId) ||
                (editingIsolateId &&
                  amendmentOpen &&
                  !identificationReason.trim())
              }
            >
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
