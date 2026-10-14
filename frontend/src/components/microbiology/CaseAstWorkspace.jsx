import ContainerLabelButton from "./ContainerLabelButton";
import React, { useEffect, useRef, useState } from "react";
import { useIntl } from "react-intl";
import {
  Accordion,
  AccordionItem,
  Button,
  InlineLoading,
  InlineNotification,
  Modal,
  Select,
  SelectItem,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  TextArea,
  TextInput,
  Tile,
} from "@carbon/react";

const validationActions = [
  "review",
  "override",
  "revert",
  "select",
  "acknowledge",
  "qc-override",
];
const reasons = [
  "override",
  "revert",
  "select",
  "repeat",
  "retest",
  "invalidate",
  "acknowledge",
  "qc-override",
  "start",
  "resolve",
];
const techniques = ["BROTH_MICRODILUTION", "DISK_DIFFUSION", "ETEST"];
export default function CaseAstWorkspace({ detail, service, onChanged }) {
  const intl = useIntl();
  const t = (key) => intl.formatMessage({ id: `microbiology.m6.${key}` });
  const [data, setData] = useState(null),
    [error, setError] = useState(false),
    [busy, setBusy] = useState(false),
    [draft, setDraft] = useState(null);
  const current = useRef(detail);
  current.current = detail;
  useEffect(() => {
    let live = true;
    service
      .getAstWorkspace(detail.id)
      .then((value) => {
        if (live) {
          setData(value);
          setError(false);
        }
      })
      .catch(() => {
        if (live) setError(true);
      });
    return () => {
      live = false;
    };
  }, [detail.id, detail.canWrite, detail.canValidate, service]);
  const canEnter = detail.canWrite && data?.canEnter;
  const canValidate = detail.canValidate && data?.canValidate;
  const open = (action, isolate, run, reading, antibioticId) =>
    setDraft({
      action,
      isolateId: isolate?.id,
      runId: run?.id,
      readingId: reading?.id,
      antibioticId: antibioticId || reading?.antibioticId,
      panelId: isolate?.assignments?.[0]?.panelId || "",
      breakpointStandardId: data.standards?.[0]?.id || "",
      technique: run?.technique || techniques[0],
      rawValue: "",
      analyzerInstrumentId: run?.analyzerInstrumentId,
      analyzerCardId: "",
      interpretation: "RESISTANT",
      reason: "",
    });
  const submit = async () => {
    const validating =
      validationActions.includes(draft.action) || draft.action === "resolve";
    if (!(validating ? current.current.canValidate : current.current.canWrite))
      return;
    setBusy(true);
    setError(false);
    try {
      if (draft.action === "resolve") {
        await service.resolveDst(detail.id, draft);
        setData(await service.getAstWorkspace(detail.id));
      } else
        setData(
          await service.astAction(
            detail.id,
            {
              ...draft,
              rawValue:
                draft.action === "reading" ? Number(draft.rawValue) : null,
            },
            validating,
          ),
        );
      setDraft(null);
      onChanged?.();
    } catch {
      setError(true);
    } finally {
      setBusy(false);
    }
  };
  const button = (action, isolate, run, reading, antibioticId) => (
    <Button
      key={action}
      size="sm"
      kind="ghost"
      onClick={() => open(action, isolate, run, reading, antibioticId)}
    >
      {t(`action.${action}`)}
    </Button>
  );
  const activeIsolate = data?.isolates?.find((i) => i.id === draft?.isolateId);
  const invalid =
    draft &&
    ((reasons.includes(draft.action) && !draft.reason.trim()) ||
      (draft.action === "reading" &&
        (draft.rawValue === "" ||
          !Number.isFinite(Number(draft.rawValue)) ||
          Number(draft.rawValue) < 0)) ||
      (draft.action === "start" &&
        (!draft.panelId || !draft.breakpointStandardId)) ||
      (draft.action === "assign" && !draft.panelId) ||
      (draft.action === "invalidate" &&
        draft.analyzerInstrumentId &&
        !draft.analyzerCardId.trim()));
  return (
    <Stack gap={5}>
      <h3>{t("ast")}</h3>
      {error && (
        <InlineNotification
          kind="error"
          title={t("saveError")}
          hideCloseButton
        />
      )}
      {!data && !error && <InlineLoading description={t("loading")} />}
      {data?.isolates?.length === 0 && <p>{t("identifyFirst")}</p>}
      {data?.isolates?.map((isolate) => (
        <Tile key={isolate.id} data-testid={`ast-isolate-${isolate.id}`}>
          <Stack gap={4}>
            <h4>
              {isolate.label}: {isolate.organism}
            </h4>
            {isolate.protocol === "NTM" && (
              <InlineNotification
                kind="info"
                title={t("ntm")}
                hideCloseButton
              />
            )}
            <p>
              {t("assignedPanels")}:{" "}
              {isolate.assignments.map((a) => a.label).join(", ") || t("none")}
            </p>
            {canEnter && (
              <div>
                {button("assign", isolate)}
                {button("start", isolate)}
              </div>
            )}
            <Accordion>
              {isolate.runs.map((run) => (
                <AccordionItem
                  key={run.id}
                  data-testid={`ast-run-${run.id}`}
                  title={`${t(`run.${run.status}`)} · ${t(`technique.${run.technique}`)} · ${intl.formatDate(new Date(run.startedAt))}`}
                  open
                >
                  <Stack gap={4}>
                    <ContainerLabelButton
                      detail={detail}
                      kind="AST"
                      rowId={run.id}
                      service={service}
                    />
                    <TableContainer>
                      <Table
                        size="sm"
                        aria-label={`${isolate.label} ${t("ast")}`}
                      >
                        <TableHead>
                          <TableRow>
                            {[
                              "agent",
                              "raw",
                              "interpretation",
                              "selected",
                              "actions",
                            ].map((key) => (
                              <TableHeader key={key}>{t(key)}</TableHeader>
                            ))}
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {run.orderedAntibiotics.map((agent) => {
                            const entries = run.readings.filter(
                              (r) => r.antibioticId === agent.antibioticId,
                            );
                            const reading = entries[entries.length - 1];
                            const selected =
                              reading &&
                              isolate.choices.some(
                                (c) => c.readingId === reading.id,
                              );
                            const editable = ![
                              "REVIEWED",
                              "INVALIDATED",
                              "CANCELLED",
                              "RERUN_REQUIRED",
                            ].includes(run.status);
                            return (
                              <TableRow key={agent.antibioticId}>
                                <TableCell>{agent.antibioticName}</TableCell>
                                <TableCell>
                                  {reading
                                    ? `${reading.rawText ?? reading.rawValue ?? ""} ${reading.units || ""}`
                                    : t("none")}
                                </TableCell>
                                <TableCell>
                                  {reading ? (
                                    <>
                                      {t(
                                        `interpretation.${reading.overrideInterpretation || reading.interpretation}`,
                                      )}
                                      {reading.overrideInterpretation && (
                                        <Tag type="purple">{t("manual")}</Tag>
                                      )}
                                      <br />
                                      {t("breakpoint")}: {run.breakpointVersion}{" "}
                                      {reading.matchedBy &&
                                        t(`match.${reading.matchedBy}`)}
                                    </>
                                  ) : (
                                    t("none")
                                  )}
                                </TableCell>
                                <TableCell>
                                  {selected ? (
                                    <Tag type="green">{t("selected")}</Tag>
                                  ) : (
                                    t("none")
                                  )}
                                </TableCell>
                                <TableCell>
                                  {canEnter &&
                                    editable &&
                                    button(
                                      "reading",
                                      isolate,
                                      run,
                                      reading,
                                      agent.antibioticId,
                                    )}
                                  {canValidate &&
                                    reading &&
                                    editable &&
                                    button("override", isolate, run, reading)}
                                  {canValidate &&
                                    reading?.overrideInterpretation &&
                                    editable &&
                                    button("revert", isolate, run, reading)}
                                  {canValidate &&
                                    reading &&
                                    run.status === "REVIEWED" &&
                                    !selected &&
                                    button("select", isolate, run, reading)}
                                </TableCell>
                              </TableRow>
                            );
                          })}
                        </TableBody>
                      </Table>
                    </TableContainer>
                    <div>
                      {canValidate &&
                        ["IN_PROGRESS", "RESULTS_IN"].includes(run.status) &&
                        button("review", isolate, run)}
                      {canEnter && run.status === "REVIEWED" && (
                        <>
                          {button("repeat", isolate, run)}
                          {button("retest", isolate, run)}
                        </>
                      )}
                      {canEnter &&
                        ["QC_FAILED", "RESULTS_IN"].includes(run.status) &&
                        button("invalidate", isolate, run)}
                      {canValidate &&
                        run.status === "QC_FAILED" &&
                        button("qc-override", isolate, run)}
                      {canValidate &&
                        run.analyzerExpertFlags &&
                        !run.analyzerFlagsAcknowledgedAt &&
                        button("acknowledge", isolate, run)}
                    </div>
                    <Accordion>
                      <AccordionItem title={t("history")}>
                        {run.attemptReason && <p>{run.attemptReason}</p>}
                        {run.readings.map((reading) => (
                          <div key={reading.id}>
                            <p>
                              {
                                run.orderedAntibiotics.find(
                                  (a) =>
                                    a.antibioticId === reading.antibioticId,
                                )?.antibioticName
                              }
                              : {reading.rawText ?? reading.rawValue}{" "}
                              {reading.units} ·{" "}
                              {t(`interpretation.${reading.interpretation}`)} ·{" "}
                              {intl.formatDate(new Date(reading.createdAt))}{" "}
                              {intl.formatTime(new Date(reading.createdAt))}
                            </p>
                            {reading.overrideHistory.map((event) => (
                              <p key={event.id}>
                                {event.performedByDisplay} ·{" "}
                                {t(
                                  `interpretation.${event.fromInterpretation}`,
                                )}{" "}
                                →{" "}
                                {t(`interpretation.${event.toInterpretation}`)}:{" "}
                                {event.reason}
                              </p>
                            ))}
                          </div>
                        ))}
                      </AccordionItem>
                    </Accordion>
                  </Stack>
                </AccordionItem>
              ))}
            </Accordion>
          </Stack>
        </Tile>
      ))}
      {data?.dst && data.dst.classification !== "NOT_APPLICABLE" && (
        <Tile>
          <h4>
            {t("dst")}: {t(`classification.${data.dst.classification}`)}
          </h4>
          {data.dst.drugs.map((drug) => (
            <Stack gap={3} key={drug.antibioticId}>
              <p>
                {drug.label}: {t(`interpretation.${drug.interpretation}`)}
              </p>
              {drug.evidence.map((item) => (
                <p key={`${item.source}-${item.sourceId}`}>
                  {t(`source.${item.source}`)}:{" "}
                  {t(`interpretation.${item.interpretation}`)} ·{" "}
                  {t(item.validated ? "validated" : "pending")}
                </p>
              ))}
              {drug.resolutionReason && (
                <p>
                  {t("resolution")}: {drug.resolutionReason}
                </p>
              )}
              {canValidate && drug.discordant && !drug.resolutionReason && (
                <Button
                  kind="tertiary"
                  size="sm"
                  onClick={() =>
                    setDraft({
                      action: "resolve",
                      antibioticId: drug.antibioticId,
                      evidenceFingerprint: drug.evidenceFingerprint,
                      interpretation: "RESISTANT",
                      reason: "",
                    })
                  }
                >
                  {t("action.resolve")}
                </Button>
              )}
            </Stack>
          ))}
        </Tile>
      )}
      {draft && (
        <Modal
          open
          modalHeading={t(`action.${draft.action}`)}
          primaryButtonText={t("save")}
          secondaryButtonText={t("cancel")}
          primaryButtonDisabled={
            busy ||
            invalid ||
            (validationActions.includes(draft.action) ||
            draft.action === "resolve"
              ? !canValidate
              : !canEnter)
          }
          onRequestClose={() => !busy && setDraft(null)}
          onRequestSubmit={submit}
        >
          <Stack gap={4}>
            {["start", "assign"].includes(draft.action) && (
              <Select
                id="ast-panel"
                labelText={t("panel")}
                value={draft.panelId}
                onChange={(e) =>
                  setDraft({ ...draft, panelId: e.target.value })
                }
              >
                <SelectItem value="" text={t("choose")} />
                {activeIsolate.panels.map((p) => (
                  <SelectItem key={p.id} value={p.id} text={p.label} />
                ))}
              </Select>
            )}
            {["start", "repeat", "retest"].includes(draft.action) && (
              <Select
                id="ast-technique"
                labelText={t("technique")}
                value={draft.technique}
                onChange={(e) =>
                  setDraft({ ...draft, technique: e.target.value })
                }
              >
                {techniques.map((value) => (
                  <SelectItem
                    key={value}
                    value={value}
                    text={t(`technique.${value}`)}
                  />
                ))}
              </Select>
            )}
            {draft.action === "start" && (
              <Select
                id="ast-standard"
                labelText={t("breakpoint")}
                value={draft.breakpointStandardId}
                onChange={(e) =>
                  setDraft({ ...draft, breakpointStandardId: e.target.value })
                }
              >
                <SelectItem value="" text={t("choose")} />
                {data.standards.map((p) => (
                  <SelectItem key={p.id} value={p.id} text={p.label} />
                ))}
              </Select>
            )}
            {draft.action === "invalidate" && draft.analyzerInstrumentId && (
              <TextInput
                id="ast-replacement-card"
                labelText={t("replacementCard")}
                value={draft.analyzerCardId}
                onChange={(e) =>
                  setDraft({ ...draft, analyzerCardId: e.target.value })
                }
              />
            )}
            {draft.action === "reading" && (
              <TextInput
                id="ast-raw"
                type="number"
                min="0"
                step="any"
                labelText={t("raw")}
                value={draft.rawValue}
                onChange={(e) =>
                  setDraft({ ...draft, rawValue: e.target.value })
                }
              />
            )}
            {["override", "resolve"].includes(draft.action) && (
              <Select
                id="ast-interpretation"
                labelText={t("interpretation")}
                value={draft.interpretation}
                onChange={(e) =>
                  setDraft({ ...draft, interpretation: e.target.value })
                }
              >
                {(draft.action === "resolve"
                  ? ["SUSCEPTIBLE", "RESISTANT"]
                  : ["SUSCEPTIBLE", "INTERMEDIATE", "RESISTANT"]
                ).map((value) => (
                  <SelectItem
                    key={value}
                    value={value}
                    text={t(`interpretation.${value}`)}
                  />
                ))}
              </Select>
            )}
            {reasons.includes(draft.action) && (
              <TextArea
                id="ast-reason"
                labelText={t("reason")}
                value={draft.reason}
                maxLength={4000}
                onChange={(e) => setDraft({ ...draft, reason: e.target.value })}
              />
            )}
            {error && (
              <InlineNotification
                kind="error"
                title={t("saveError")}
                hideCloseButton
              />
            )}
          </Stack>
        </Modal>
      )}
    </Stack>
  );
}
