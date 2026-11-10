import { labTimestamp } from "./MicrobiologyTime";
import React, { useEffect, useRef, useState } from "react";
import { useIntl } from "react-intl";
import {
  Button,
  Checkbox,
  InlineLoading,
  InlineNotification,
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
  Tile,
} from "@carbon/react";
import CaseReleaseActions from "./CaseReleaseActions";

export function MicrobiologyReportBlock({ report }) {
  const intl = useIntl(),
    t = (key) => intl.formatMessage({ id: `microbiology.m6.${key}` });
  const notesFor = (type, id) =>
    (report.notes || [])
      .filter(
        (note) =>
          (note.targetType || null) === type && (note.targetId || null) === id,
      )
      .map((note, index) => (
        <p key={index}>
          {note.fromRule
            ? `${note.text} · ${t("fromRule")}`
            : `${note.text} · ${note.author} · ${labTimestamp(intl, note.at)}`}
        </p>
      ));
  const testRows = (tests) =>
    tests.map((test) => (
      <div key={test.analysisId}>
        <h5>{test.name}</h5>
        {test.values.map((value, index) => (
          <p key={index}>
            {value.component}: {value.value}
          </p>
        ))}
        {notesFor("RESULT", test.analysisId)}
      </div>
    ));
  return (
    <Stack gap={5}>
      <h3>{report.labUnitName}</h3>
      <section>
        <h4>{t("initial")}</h4>
        {report.initial.length ? testRows(report.initial) : <p>{t("none")}</p>}
      </section>
      <section>
        <h4>{t("culture")}</h4>
        {report.cultures.length ? (
          report.cultures.map((row) => (
            <div key={row.id}>
              <p>
                {row.label}: {t(`outcome.${row.outcome}`)} · {t("incubation")}:{" "}
                {intl.formatNumber(row.incubationHours)} {t("hours")}
              </p>
              {notesFor("CULTURE", row.id)}
            </div>
          ))
        ) : (
          <p>{t("none")}</p>
        )}
        {report.isolates.map((isolate) => (
          <div key={isolate.id}>
            <p>
              {isolate.label}: {isolate.organism}
            </p>
            {notesFor("ISOLATE", isolate.id)}
          </div>
        ))}
      </section>
      <section>
        <h4>{t("ast")}</h4>
        {report.classification !== "NOT_APPLICABLE" && (
          <p>{t(`classification.${report.classification}`)}</p>
        )}
        {report.isolates.map((isolate) => (
          <React.Fragment key={isolate.id}>
            {isolate.tested > 0 && (
              <p>
                {intl.formatMessage(
                  { id: "microbiology.m6.reportedOfTested" },
                  {
                    reported: isolate.susceptibilities.length,
                    tested: isolate.tested,
                  },
                )}
              </p>
            )}
            <TableContainer title={`${isolate.label}: ${isolate.organism}`}>
              <Table aria-label={`${isolate.label} ${t("ast")}`}>
                <TableHead>
                  <TableRow>
                    {["agent", "raw", "interpretation", "breakpoint"].map(
                      (key) => (
                        <TableHeader key={key}>{t(key)}</TableHeader>
                      ),
                    )}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {isolate.susceptibilities.map((row) => (
                    <TableRow key={row.antibioticId}>
                      <TableCell>
                        {row.agent}
                        {notesFor("DRUG", row.readingId)}
                      </TableCell>
                      <TableCell>
                        {row.raw} {row.units}
                      </TableCell>
                      <TableCell>
                        {t(`interpretation.${row.interpretation}`)}
                      </TableCell>
                      <TableCell>
                        {row.standard} {row.version}
                        {row.clinicalContext === "NON_MENINGITIS" && (
                          <p>{t("clinicalContext.nonMeningitis")}</p>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </React.Fragment>
        ))}
      </section>
      <section>
        <h4>{t("additional")}</h4>
        {report.additional.length ? (
          testRows(report.additional)
        ) : (
          <p>{t("none")}</p>
        )}
      </section>
      {report.notes.some((note) => !note.targetType) && (
        <section>
          <h4>{t("sendWithResult")}</h4>
          {report.notes
            .filter((note) => !note.targetType)
            .map((note, index) => (
              <p key={index}>
                {note.text} · {note.author} · {labTimestamp(intl, note.at)}
              </p>
            ))}
        </section>
      )}
    </Stack>
  );
}
export default function CaseReportWorkspace({ detail, service, onChanged }) {
  const intl = useIntl(),
    t = (key) => intl.formatMessage({ id: `microbiology.m6.${key}` });
  const [state, setState] = useState(null),
    [versionId, setVersionId] = useState(""),
    [error, setError] = useState(false),
    [busy, setBusy] = useState(false);
  const current = useRef(detail);
  current.current = detail;
  const load = async () => {
    const [report, readiness, versions, tests] = await Promise.all([
      service.getReport(detail.id),
      service.getReadiness(detail.id),
      service.getReportVersions(detail.id),
      service.getTests(detail.id),
    ]);
    return { report, readiness, versions, tests };
  };
  useEffect(() => {
    let live = true;
    load()
      .then((value) => {
        if (live) {
          setState(value);
          setError(false);
        }
      })
      .catch(() => {
        if (live) setError(true);
      });
    return () => {
      live = false;
    };
  }, [detail, service]);
  const released = async () => {
    try {
      const next = await load();
      setState(next);
      setVersionId(next.versions[next.versions.length - 1].id);
      onChanged?.();
    } catch {
      setError(true);
    }
  };
  const select = async (analysisId, selected) => {
    if (!current.current.canValidate) return;
    setBusy(true);
    setError(false);
    try {
      await service.selectReportResult(detail.id, analysisId, selected);
      setState(await load());
    } catch {
      setError(true);
    } finally {
      setBusy(false);
    }
  };
  const report =
    state?.versions.find((v) => v.id === versionId)?.report || state?.report;
  return (
    <Stack gap={5}>
      <h3>{t("report")}</h3>
      {detail.domain === "ENVIRONMENTAL" &&
        detail.finalReleaseState === "FINAL_RELEASED" && (
          <Button
            disabled={busy}
            onClick={async () => {
              setBusy(true);
              try {
                await service.downloadEnvironmentalCertificate(detail.orderId);
              } catch {
                setError(true);
              } finally {
                setBusy(false);
              }
            }}
          >
            {intl.formatMessage({
              id: "microbiology.environmental.certificate",
            })}
          </Button>
        )}

      {error && (
        <InlineNotification
          kind="error"
          title={t("saveError")}
          hideCloseButton
        />
      )}
      {!state && !error && <InlineLoading description={t("loading")} />}
      {state && (
        <>
          <Select
            id="micro-report-version"
            labelText={t("version")}
            value={versionId}
            onChange={(e) => setVersionId(e.target.value)}
          >
            <SelectItem value="" text={t("preview")} />
            {state.versions.map((v) => (
              <SelectItem
                key={v.id}
                value={v.id}
                text={`${intl.formatNumber(v.number)} · ${t(`release.${v.type}`)} · ${labTimestamp(intl, v.at)}`}
              />
            ))}
          </Select>
          {detail.canValidate && (
            <Tile>
              <h4>{t("selectedResults")}</h4>
              {state.tests
                .filter((test) => test.reportable)
                .map((test) => (
                  <Checkbox
                    key={test.analysisId}
                    id={`report-selection-${test.analysisId}`}
                    labelText={test.testName}
                    checked={test.selectedForReport}
                    disabled={busy}
                    onChange={(_, { checked }) =>
                      select(test.analysisId, checked)
                    }
                  />
                ))}
            </Tile>
          )}
          {!versionId && state.readiness.blockers.length > 0 && (
            <Tile>
              <h4>{t("finalBlocked")}</h4>
              {
                <ul>
                  {state.readiness.blockers.map((blocker) => {
                    const [code, field] = blocker.split(":");
                    // i18n-keys: microbiology.m6.field.*
                    const label =
                      code === "CLINICAL_FIELD_REQUIRED"
                        ? intl.formatMessage(
                            { id: "microbiology.m6.blocker.fieldRequired" },
                            { field: t(`field.${field}`) },
                          )
                        : t(`blocker.${code}`) +
                          (state.readiness.blockerLabels?.[blocker]
                            ? `: ${state.readiness.blockerLabels[blocker]}`
                            : "");
                    const context = [
                      "CLINICAL_FIELD_REQUIRED",
                      "PROGRAM_REQUIRED",
                      "QUESTIONNAIRE_FIELD_REQUIRED",
                      "QUESTIONNAIRE_UNAVAILABLE",
                    ].includes(code);
                    return (
                      <li key={blocker}>
                        {context ? (
                          <a
                            href="#case-information"
                            onClick={() =>
                              document
                                .getElementById("case-information")
                                ?.focus()
                            }
                          >
                            {label}
                          </a>
                        ) : (
                          label
                        )}
                      </li>
                    );
                  })}
                </ul>
              }
            </Tile>
          )}
          <Tile>
            <MicrobiologyReportBlock report={report} />
          </Tile>
          <CaseReleaseActions
            detail={detail}
            service={service}
            readiness={state.readiness}
            onReleased={released}
            partialReady={state.report.validatedResults > 0}
          />
        </>
      )}
    </Stack>
  );
}
