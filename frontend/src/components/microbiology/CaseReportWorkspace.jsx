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

export function MicrobiologyReportBlock({ report }) {
  const intl = useIntl(),
    t = (key) => intl.formatMessage({ id: `microbiology.m6.${key}` });
  const testRows = (tests) =>
    tests.map((test) => (
      <div key={test.analysisId}>
        <h5>{test.name}</h5>
        {test.values.map((value, index) => (
          <p key={index}>
            {value.component}: {value.value}
          </p>
        ))}
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
            <p key={row.id}>
              {row.label}: {t(`outcome.${row.outcome}`)} · {t("incubation")}:{" "}
              {intl.formatNumber(row.incubationHours)} {t("hours")}
            </p>
          ))
        ) : (
          <p>{t("none")}</p>
        )}
        {report.isolates.map((isolate) => (
          <p key={isolate.id}>
            {isolate.label}: {isolate.organism}
          </p>
        ))}
      </section>
      <section>
        <h4>{t("ast")}</h4>
        {report.classification !== "NOT_APPLICABLE" && (
          <p>{t(`classification.${report.classification}`)}</p>
        )}
        {report.isolates.map((isolate) => (
          <TableContainer
            key={isolate.id}
            title={`${isolate.label}: ${isolate.organism}`}
          >
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
                    <TableCell>{row.agent}</TableCell>
                    <TableCell>
                      {row.raw} {row.units}
                    </TableCell>
                    <TableCell>
                      {t(`interpretation.${row.interpretation}`)}
                    </TableCell>
                    <TableCell>
                      {row.standard} {row.version}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
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
      {report.notes.length > 0 && (
        <section>
          <h4>{t("sendWithResult")}</h4>
          {report.notes.map((note, index) => (
            <p key={index}>
              {note.text} · {note.author} · {intl.formatDate(new Date(note.at))}
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
  }, [detail.id, detail.canWrite, detail.canValidate]);
  const release = async (type) => {
    if (
      !(type === "preliminary"
        ? current.current.canWrite
        : current.current.canValidate)
    )
      return;
    setBusy(true);
    setError(false);
    try {
      await service.releaseReport(detail.id, type);
      const next = await load();
      setState(next);
      setVersionId(next.versions[next.versions.length - 1].id);
      onChanged?.();
    } catch {
      setError(true);
    } finally {
      setBusy(false);
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
                text={`${intl.formatNumber(v.number)} · ${t(`release.${v.type}`)} · ${intl.formatDate(new Date(v.at))} ${intl.formatTime(new Date(v.at))}`}
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
            <InlineNotification
              kind="warning"
              title={t("finalBlocked")}
              subtitle={state.readiness.blockers
                .map((blocker) => {
                  const [code, field] = blocker.split(":");
                  return (
                    t(`blocker.${code}`) +
                    (code === "CLINICAL_FIELD_REQUIRED"
                      ? `: ${t(`field.${field}`)}`
                      : state.readiness.blockerLabels?.[blocker]
                        ? `: ${state.readiness.blockerLabels[blocker]}`
                        : "")
                  );
                })
                .join("; ")}
              hideCloseButton
            />
          )}
          <Tile>
            <MicrobiologyReportBlock report={report} />
          </Tile>
          <div>
            {detail.canWrite && !detail.amendmentOpen && (
              <Button
                kind="tertiary"
                disabled={busy || state.report.validatedResults === 0}
                onClick={() => release("preliminary")}
              >
                {t("partialRelease")}
              </Button>
            )}
            {detail.canValidate && (
              <Button
                disabled={busy || !state.readiness.finalReleaseReady}
                onClick={() =>
                  release(detail.amendmentOpen ? "amended" : "final")
                }
              >
                {t(detail.amendmentOpen ? "amendedRelease" : "finalRelease")}
              </Button>
            )}
          </div>
        </>
      )}
    </Stack>
  );
}
