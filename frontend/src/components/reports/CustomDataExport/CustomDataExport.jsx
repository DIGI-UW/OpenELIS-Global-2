import React, { useContext, useEffect, useMemo, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { InlineNotification, Modal, TextInput } from "@carbon/react";
import ReportingView from "./ReportingView";
import { useIntl } from "react-intl";
import useReportingRoute from "./useReportingRoute";
import UserSessionDetailsContext from "../../../UserSessionDetailsContext";
import { serverQuery } from "../../utils/queryClient";
import {
  createSavedReport,
  deleteSavedReport,
  reportingPath,
  recoverReport,
  submitReport,
  updateSavedReport,
} from "./api";
import "./CustomDataExport.scss";

// Reports links can reload the page. Keep only the draft in this browser session.
const draftStorageKey = "openelis-reporting-draft";
let sessionDraft;
export const clearReportingDraft = () => {
  sessionDraft = undefined;
  try {
    window.sessionStorage.removeItem(draftStorageKey);
  } catch {
    /* In-memory drafts still work when browser storage is unavailable. */
  }
};
const emptyDraft = () => ({
  id: crypto.randomUUID(),
  reportType: null,
  started: false,
  step: 1,
  layout: "SPREADSHEET",
  columns: {},
  dateFrom: "",
  dateTo: "",
  labSectionIds: [],
  testIds: [],
  resultStatuses: ["FINALIZED"],
  jobId: null,
  review: false,
  savedReport: null,
});
function readDraft(owner) {
  if (!owner) return emptyDraft();
  try {
    const stored = JSON.parse(window.sessionStorage.getItem(draftStorageKey));
    if (
      stored?.version === 1 &&
      stored.draft?.columns &&
      Array.isArray(stored.draft.labSectionIds) &&
      Array.isArray(stored.draft.testIds) &&
      Array.isArray(stored.draft.resultStatuses)
    )
      sessionDraft = stored;
  } catch {
    /* Use the current page's draft if storage cannot be read. */
  }
  if (sessionDraft?.owner === owner)
    return {
      ...emptyDraft(),
      ...sessionDraft.draft,
      started: sessionDraft.draft.started ?? !!sessionDraft.draft.reportType,
    };
  clearReportingDraft();
  return emptyDraft();
}
function saveDraft(owner, draft) {
  if (!owner) return;
  sessionDraft = { version: 1, owner, draft };
  try {
    window.sessionStorage.setItem(
      draftStorageKey,
      JSON.stringify(sessionDraft),
    );
  } catch {
    /* Navigation within the page remains available without browser storage. */
  }
}
const calendarDay = (value) => {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return NaN;
  const day = Date.parse(`${value}T00:00:00Z`);
  return Number.isFinite(day) &&
    new Date(day).toISOString().slice(0, 10) === value
    ? day
    : NaN;
};
const active = (job) => job && ["QUEUED", "GENERATING"].includes(job.state);

export default function CustomDataExport() {
  const { userSessionDetails = {} } = useContext(UserSessionDetailsContext);
  const owner = String(
    userSessionDetails.userId || userSessionDetails.loginName || "",
  );
  return <ReportingBuilder key={owner} owner={owner} />;
}

function ReportingBuilder({ owner }) {
  const intl = useIntl();
  const t = (id, values) => intl.formatMessage({ id }, values);
  const route = useReportingRoute();
  const [storedDraft, setDraft] = useState(() => ({
    ...readDraft(owner),
    ...(route.panel === "builder" ? { started: true } : {}),
  }));
  const resumeRoute = useRef({
    reportType: storedDraft.reportType,
    layout: storedDraft.layout,
    step: storedDraft.step,
    started: storedDraft.started,
  });
  const {
    panel,
    step,
    navigate,
    page: queuePage,
    savedId,
    jobId: expandedJob,
    search: savedSearch,
  } = route;
  const currentPanel = useRef(panel);
  useEffect(() => {
    currentPanel.current = panel;
  }, [panel]);
  // URL values drive the current screen immediately, including POP navigation.
  // Stored navigation metadata is used only by Continue and session recovery.
  const draft =
    panel === "builder"
      ? {
          ...storedDraft,
          reportType: route.reportType,
          layout: route.layout,
          step,
          review: step === 3,
        }
      : storedDraft;
  const setPanel = (next) =>
    navigate({
      panel: next,
      ...resumeRoute.current,
      page: 0,
      jobId: null,
      savedId: storedDraft.savedReport?.id,
      search: "",
    });
  const setQueuePage = (page) =>
    navigate({ panel: "queue", page, jobId: null });
  const setSavedSearch = (search) => navigate({ search }, true);
  const [searchTerm, setSearchTerm] = useState(savedSearch);
  useEffect(() => {
    const timer = setTimeout(() => setSearchTerm(savedSearch), 250);
    return () => clearTimeout(timer);
  }, [savedSearch]);
  const [datesTouched, setDatesTouched] = useState({ from: false, to: false });
  const [saveOpen, setSaveOpen] = useState(false);
  const [saveName, setSaveName] = useState("");
  const [savedNotice, setSavedNotice] = useState("");
  const [freshDatePrompt, setFreshDatePrompt] = useState(false);
  const [deleteCandidate, setDeleteCandidate] = useState(null);
  const [updateOpen, setUpdateOpen] = useState(false);
  const [cancelCandidate, setCancelCandidate] = useState(null);
  const retryIds = useRef(new Map());
  const submitted = useRef(null);
  const draftRevision = useRef(0);
  const queryClient = useQueryClient();
  const columnKey = `${draft.reportType}:${draft.layout}`;
  const types = useQuery(
    serverQuery(["reporting-types", owner], `${reportingPath}/report-types`),
  );
  const catalog = useQuery({
    ...serverQuery(
      ["reporting-catalog", owner, draft.reportType, draft.layout],
      `${reportingPath}/variables?reportType=${encodeURIComponent(draft.reportType)}&layout=${encodeURIComponent(draft.layout)}`,
    ),
    enabled: panel === "builder" && !!draft.reportType,
  });
  const job = useQuery({
    ...serverQuery(
      ["reporting-job", owner, draft.jobId],
      `${reportingPath}/jobs/${draft.jobId}`,
    ),
    enabled: !!draft.jobId,
    refetchInterval: (data) => (active(data) ? 1500 : false),
  });
  const queue = useQuery({
    ...serverQuery(
      ["reporting-queue", owner, queuePage],
      `${reportingPath}/jobs?page=${queuePage}`,
    ),
    enabled: panel === "queue",
    refetchInterval: (data) => (data?.activeCount > 0 ? 2000 : false),
  });
  const savedReports = useQuery({
    ...serverQuery(
      ["reporting-saved", owner, panel === "saved" ? searchTerm : ""],
      `${reportingPath}/saved-configs?page=0&size=100&search=${encodeURIComponent(panel === "saved" ? searchTerm : "")}`,
    ),
    enabled: panel === "saved" || panel === "overview",
  });
  const linkedSaved = useQuery({
    ...serverQuery(
      ["reporting-saved-detail", owner, savedId],
      `${reportingPath}/saved-configs/${encodeURIComponent(savedId || "")}`,
    ),
    enabled:
      panel === "builder" &&
      !!savedId &&
      savedId !== storedDraft.savedReport?.id,
    onSuccess: (saved) => {
      if (saved.id === savedId) openSaved(saved, false, true);
    },
  });
  const restoringSaved =
    panel === "builder" && !!savedId && savedId !== storedDraft.savedReport?.id;
  const linkedJob = useQuery({
    ...serverQuery(
      ["reporting-job", owner, expandedJob],
      `${reportingPath}/jobs/${encodeURIComponent(expandedJob || "")}`,
    ),
    enabled: panel === "queue" && !!expandedJob,
    refetchInterval: (data) => (active(data) ? 1500 : false),
  });
  const submission = useMutation({
    mutationFn: ({ request }) => submitReport(request),
    onSuccess: (result) => {
      queryClient.setQueryData(["reporting-job", owner, result.id], result);
      queryClient.invalidateQueries({ queryKey: ["reporting-queue", owner] });
    },
  });
  const recovery = useMutation({
    mutationFn: recoverReport,
    onSuccess: (result) => {
      queryClient.setQueryData(["reporting-job", owner, result.id], result);
      queryClient.setQueriesData(
        { queryKey: ["reporting-queue", owner] },
        (page) =>
          page
            ? {
                ...page,
                jobs: page.jobs.map((item) =>
                  item.id === result.id ? result : item,
                ),
              }
            : page,
      );
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ["reporting-queue", owner] });
      queryClient.invalidateQueries({ queryKey: ["reporting-job", owner] });
    },
  });
  const retryJob = (item) => {
    if (recovery.isLoading) return;
    if (!retryIds.current.has(item.id))
      retryIds.current.set(item.id, crypto.randomUUID());
    recovery.mutate(
      {
        id: item.id,
        action: "retry",
        clientRequestId: retryIds.current.get(item.id),
      },
      {
        onSuccess: (result) => {
          retryIds.current.delete(item.id);
          if (currentPanel.current === "queue")
            navigate({ panel: "queue", page: 0, jobId: result.id });
        },
      },
    );
  };
  const supportsFilter = (name) =>
    catalog.data?.definition.filters?.includes(name);
  const filterNames = ["labSectionIds", "testIds", "resultStatuses"];
  const effectiveFilters = Object.fromEntries(
    filterNames.map((name) => [name, supportsFilter(name) ? draft[name] : []]),
  );
  const unavailableFilters =
    !!catalog.data &&
    filterNames.some(
      (name) =>
        !supportsFilter(name) &&
        draft[name].length > 0 &&
        !(
          name === "resultStatuses" &&
          draft[name].length === 1 &&
          draft[name][0] === "FINALIZED"
        ),
    );
  const savedDefinition = () => ({
    schemaVersion: 1,
    reportType: draft.reportType,
    layout: draft.layout,
    selectedVariables: selected,
    filters: effectiveFilters,
  });
  const createSaved = useMutation({
    mutationFn: createSavedReport,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["reporting-saved", owner] });
    },
  });
  const updateSaved = useMutation({
    mutationFn: updateSavedReport,
    onSuccess: (result) => {
      queryClient.setQueriesData(
        { queryKey: ["reporting-saved", owner] },
        (page) =>
          page?.reports
            ? {
                ...page,
                reports: page.reports.map((saved) =>
                  saved.id === result.id ? result : saved,
                ),
              }
            : page,
      );
      queryClient.invalidateQueries({ queryKey: ["reporting-saved", owner] });
    },
  });
  const removeSaved = useMutation({
    mutationFn: deleteSavedReport,
    onSuccess: (_result, { id: removedId }) => {
      queryClient.setQueriesData(
        { queryKey: ["reporting-saved", owner] },
        (page) =>
          page?.reports
            ? {
                ...page,
                reports: page.reports.filter((saved) => saved.id !== removedId),
              }
            : page,
      );
      queryClient.invalidateQueries({ queryKey: ["reporting-saved", owner] });
    },
  });

  useEffect(() => {
    if (panel === "builder")
      resumeRoute.current = {
        started: true,
        step,
        review: step === 3,
        reportType: route.reportType,
        layout: route.layout,
      };
    saveDraft(owner, { ...storedDraft, ...resumeRoute.current });
  }, [owner, storedDraft, panel, step, route.reportType, route.layout]);

  const fields = catalog.data?.variables || [];
  const byId = useMemo(
    () =>
      new Map(
        (catalog.data?.variables || []).map((field) => [field.id, field]),
      ),
    [catalog.data],
  );
  const selected = draft.columns[columnKey] ?? catalog.data?.defaultColumns ?? [];
  const stale = catalog.data ? selected.filter((id) => !byId.has(id)) : [];
  const firstDay = calendarDay(draft.dateFrom);
  const lastDay = calendarDay(draft.dateTo);
  const periodDays = (lastDay - firstDay) / 86400000 + 1;
  const fromError = !draft.dateFrom
    ? datesTouched.from
      ? "reporting.dates.requiredFrom"
      : null
    : !Number.isFinite(firstDay)
      ? "reporting.dates.invalid"
      : null;
  const toError = !draft.dateTo
    ? datesTouched.to
      ? "reporting.dates.requiredTo"
      : null
    : !Number.isFinite(lastDay)
      ? "reporting.dates.invalid"
      : periodDays < 1
        ? "reporting.dates.reversed"
        : periodDays > catalog.data?.maxDays
          ? "reporting.dates.tooLong"
          : null;
  const validPeriod =
    Number.isFinite(periodDays) &&
    periodDays >= 1 &&
    periodDays <= catalog.data?.maxDays;
  const update = (changes) => {
    draftRevision.current += 1;
    setDraft((value) => ({
      ...value,
      ...changes,
      ...("reportType" in changes ? { savedReport: null } : {}),
    }));
    submission.reset();
    if ("reportType" in changes || "layout" in changes)
      navigate({
        reportType:
          "reportType" in changes ? changes.reportType : draft.reportType,
        layout: changes.layout || draft.layout,
        savedId: "reportType" in changes ? null : savedId,
        step: 1,
      });
  };
  const openSaved = (saved, copy = false, replace = false) => {
    draftRevision.current += 1;
    submitted.current = null;
    submission.reset();
    createSaved.reset();
    updateSaved.reset();
    const definition = saved.definition;
    const key = `${definition.reportType}:${definition.layout}`;
    setDraft((value) => ({
      ...value,
      id: crypto.randomUUID(),
      reportType: definition.reportType,
      layout: definition.layout,
      columns: {
        ...value.columns,
        [key]: definition.selectedVariables,
      },
      dateFrom: "",
      dateTo: "",
      labSectionIds: definition.filters.labSectionIds,
      testIds: definition.filters.testIds,
      resultStatuses: definition.filters.resultStatuses,
      jobId: null,
      review: false,
      step: 2,
      started: true,
      savedReport: copy ? null : saved,
    }));
    setDatesTouched({ from: false, to: false });
    setFreshDatePrompt(true);
    navigate(
      {
        panel: "builder",
        step: 2,
        reportType: definition.reportType,
        layout: definition.layout,
        savedId: copy ? null : saved.id,
        page: 0,
        jobId: null,
      },
      replace,
    );
    setSavedNotice("");
    if (copy) {
      setSaveName(
        intl.formatMessage(
          { id: "reporting.saved.copyName" },
          { name: saved.name },
        ),
      );
      setSaveOpen(true);
    }
  };
  const rerunJob = (item) => {
    const request = item.request;
    openSaved({
      definition: {
        reportType: request.definition.id,
        layout: request.layout,
        selectedVariables: request.variables.map((field) => field.id),
        filters: request.filterSpec,
      },
    });
    setDraft((value) => ({ ...value, savedReport: null }));
  };
  useEffect(() => {
    if (panel !== "builder" || restoringSaved || !types.data) return;
    const type = types.data.find((item) => item.id === route.reportType);
    if (route.reportType && !type) {
      navigate({ reportType: null, step: 1 }, true);
      return;
    }
    if (type && !type.layouts.includes(route.layout)) {
      navigate({ layout: type.layouts[0], step: 1 }, true);
      return;
    }
    if (!route.reportType && step !== 1) navigate({ step: 1 }, true);
    else if (catalog.data && step > 1 && (!selected.length || stale.length))
      navigate({ step: 1 }, true);
    else if (catalog.data && step === 3 && !validPeriod)
      navigate({ step: 2 }, true);
  }, [
    panel,
    restoringSaved,
    types.data,
    route.reportType,
    route.layout,
    step,
    catalog.data,
    selected.length,
    stale.length,
    validPeriod,
    navigate,
  ]);
  const saveCurrent = () => {
    const revision = draftRevision.current;
    createSaved.mutate(
      { name: saveName, definition: savedDefinition() },
      {
        onSuccess: (result) => {
          if (revision !== draftRevision.current) return;
          setDraft((value) => ({ ...value, savedReport: result }));
          navigate({ savedId: result.id }, true);
          setSavedNotice(t("reporting.saved.created", { name: result.name }));
          updateSaved.reset();
          setSaveOpen(false);
        },
      },
    );
  };
  const updateCurrent = () => {
    setUpdateOpen(false);
    const revision = draftRevision.current;
    updateSaved.mutate(
      {
        id: draft.savedReport.id,
        name: draft.savedReport.name,
        expectedVersion: draft.savedReport.version,
        definition: savedDefinition(),
      },
      {
        onSuccess: (result) => {
          if (revision !== draftRevision.current) return;
          setDraft((value) => ({ ...value, savedReport: result }));
          setSavedNotice(t("reporting.saved.updated", { name: result.name }));
        },
      },
    );
  };
  const setColumns = (columns) =>
    update({ columns: { ...draft.columns, [columnKey]: columns } });
  const goStep = (step) => {
    navigate({ panel: "builder", step });
  };
  const startFresh = () => {
    draftRevision.current += 1;
    submitted.current = null;
    setDraft({ ...emptyDraft(), started: true });
    navigate({
      panel: "builder",
      step: 1,
      reportType: null,
      layout: "SPREADSHEET",
      savedId: null,
      page: 0,
      jobId: null,
    });
    setDatesTouched({ from: false, to: false });
    setFreshDatePrompt(false);
    setSavedNotice("");
    setSaveName("");
    submission.reset();
    createSaved.reset();
    updateSaved.reset();
  };
  const chooseType = (type) => {
    draftRevision.current += 1;
    submitted.current = null;
    const layout = type.layouts[0];
    const key = `${type.id}:${layout}`;
    setDraft((value) => {
      const columns = { ...value.columns };
      columns[key] = [];
      return {
        ...value,
        reportType: type.id,
        layout,
        columns,
        savedReport: null,
        step: 1,
        review: false,
      };
    });
    navigate({
      panel: "builder",
      step: 1,
      reportType: type.id,
      layout,
      savedId: null,
    });
  };
  const saveCopy = () => {
    setSaveName(
      t("reporting.saved.copyName", { name: draft.savedReport.name }),
    );
    setSaveOpen(true);
  };
  const run = () => {
    if (
      !selected.length ||
      stale.length ||
      !validPeriod ||
      submission.isLoading ||
      restoringSaved
    )
      return;
    const request = {
      schemaVersion: 1,
      reportType: draft.reportType,
      layout: draft.layout,
      selectedVariables: selected,
      filterSpec: {
        dateFrom: draft.dateFrom,
        dateTo: draft.dateTo,
        ...effectiveFilters,
      },
    };
    const fingerprint = JSON.stringify(request);
    if (
      !submitted.current ||
      submitted.current.fingerprint !== fingerprint ||
      job.data?.state === "READY"
    ) {
      submitted.current = { fingerprint, id: crypto.randomUUID() };
    }
    // A new run must not expose the previous file as its current download.
    // Keep draft inputs and the request identity for a failed-submit retry.
    setDraft((value) => ({ ...value, jobId: null }));
    const revision = draftRevision.current;
    submission.mutate(
      {
        request: { ...request, clientRequestId: submitted.current.id },
      },
      {
        onSuccess: (result) => {
          // Per-call callbacks stop on unmount; the cache callback above still runs.
          if (revision === draftRevision.current)
            setDraft((value) => ({ ...value, jobId: result.id }));
        },
      },
    );
  };
  const errorText = (error) =>
    intl.messages[error?.message]
      ? t(error.message)
      : t("reporting.requestError");
  const lookup = (items, ids) =>
    ids.length
      ? items
          .filter((item) => ids.includes(item.id))
          .map((item) => item.label)
          .join(", ")
      : t("reporting.all");
  return (
    <>
      <ReportingView
        key={storedDraft.id}
        {...{
          draft,
          panel,
          setPanel,
          catalog,
          types,
          fields,
          selected,
          byId,
          stale,
          step,
          goStep,
          update,
          startFresh,
          chooseType,
          columnKey,
          setColumns,
          savedReports,
          savedSearch,
          setSavedSearch,
          openSaved,
          setDeleteCandidate,
          queue,
          queuePage,
          setQueuePage,
          submission,
          job,
          savedNotice,
          freshDatePrompt,
          unavailableFilters,
          fromError,
          toError,
          setDatesTouched,
          validPeriod,
          periodDays,
          supportsFilter,
          effectiveFilters,
          lookup,
          run,
          errorText,
          saveName,
          setSaveName,
          saveCurrent,
          setUpdateOpen,
          saveCopy,
          recovery,
          retryJob,
          rerunJob,
        }}
        requestCancel={(item) => {
          recovery.reset();
          setCancelCandidate(item);
        }}
        linkedSavedError={restoringSaved ? linkedSaved.error : null}
        restoringSaved={restoringSaved}
        linkedJob={linkedJob}
        expandedJob={expandedJob}
        setExpandedJob={(id) => navigate({ jobId: id })}
        savedError={createSaved.error || updateSaved.error}
        createSavedBusy={createSaved.isLoading}
        updateSavedBusy={updateSaved.isLoading}
      />
      {!!cancelCandidate && (
        <Modal
          danger
          open
          modalHeading={t("reporting.recovery.cancelTitle")}
          primaryButtonText={t("reporting.recovery.cancelTitle")}
          secondaryButtonText={t("reporting.recovery.keepQueued")}
          primaryButtonDisabled={recovery.isLoading}
          onRequestClose={() => !recovery.isLoading && setCancelCandidate(null)}
          onRequestSubmit={() =>
            recovery.mutate(
              { id: cancelCandidate.id, action: "cancel" },
              {
                onSuccess: () => setCancelCandidate(null),
              },
            )
          }
        >
          <p>{t("reporting.recovery.cancelHelp")}</p>
          {recovery.error && (
            <InlineNotification
              kind="error"
              title={errorText(recovery.error)}
              hideCloseButton
            />
          )}
        </Modal>
      )}
      {saveOpen && (
        <Modal
          open={saveOpen}
          modalHeading={t("reporting.saved.save")}
          primaryButtonText={t("reporting.saved.confirmSave")}
          secondaryButtonText={t("reporting.saved.cancel")}
          primaryButtonDisabled={!saveName.trim() || createSaved.isLoading}
          onRequestSubmit={saveCurrent}
          onRequestClose={() => setSaveOpen(false)}
        >
          {createSaved.error && (
            <InlineNotification
              kind="error"
              title={errorText(createSaved.error)}
              hideCloseButton
            />
          )}
          <TextInput
            id="reporting-saved-name"
            labelText={t("reporting.saved.name")}
            value={saveName}
            maxLength={200}
            onChange={(event) => setSaveName(event.target.value)}
          />
        </Modal>
      )}
      {updateOpen && (
        <Modal
          open={updateOpen}
          modalHeading={t("reporting.saved.update")}
          primaryButtonText={t("reporting.saved.confirmUpdate")}
          secondaryButtonText={t("reporting.saved.cancel")}
          onRequestSubmit={updateCurrent}
          onRequestClose={() => setUpdateOpen(false)}
        >
          <p>
            {t("reporting.saved.updateHelp", { name: draft.savedReport?.name })}
          </p>
        </Modal>
      )}
      {!!deleteCandidate && (
        <Modal
          danger
          open={!!deleteCandidate}
          modalHeading={t("reporting.saved.delete")}
          primaryButtonText={t("reporting.saved.confirmDelete")}
          secondaryButtonText={t("reporting.saved.cancel")}
          primaryButtonDisabled={removeSaved.isLoading}
          onRequestSubmit={() =>
            removeSaved.mutate(
              {
                id: deleteCandidate.id,
                expectedVersion: deleteCandidate.version,
              },
              {
                onSuccess: (_result, { id }) => {
                  setDraft((value) =>
                    value.savedReport?.id === id
                      ? { ...value, savedReport: null }
                      : value,
                  );
                  setDeleteCandidate(null);
                },
              },
            )
          }
          onRequestClose={() => setDeleteCandidate(null)}
        >
          {removeSaved.error && (
            <InlineNotification
              kind="error"
              title={errorText(removeSaved.error)}
              hideCloseButton
            />
          )}
          <p>
            {t("reporting.saved.deleteHelp", { name: deleteCandidate?.name })}
          </p>
        </Modal>
      )}
    </>
  );
}
