import React, { useEffect, useRef, useState } from "react";
import {
  Button,
  Checkbox,
  Dropdown,
  InlineLoading,
  InlineNotification,
  MultiSelect,
  Search,
  Select,
  SelectItem,
  TextInput,
  Tag,
} from "@carbon/react";
import {
  ArrowLeft,
  ArrowRight,
  Download,
  Information,
} from "@carbon/icons-react";
import { useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import ReportingColumns from "./ReportingColumns";
import { downloadUrl } from "./api";

const sourceTagColors = {
  SAMPLE_TESTING: "blue",
  REFERRALS: "teal",
  NON_CONFORMANCE: "magenta",
};

// The presentation follows ReportBuilder/ReportQueue in openelis-work 5b2df7e34f.
// Real data and mutations come from the existing reporting controller component.
export default function ReportingView(p) {
  const intl = useIntl();
  const t = (id, values) => intl.formatMessage({ id }, values);
  const [moreFilters, setMoreFilters] = useState(false);
  const [saveForLater, setSaveForLater] = useState(false);
  const { expandedJob, setExpandedJob } = p;
  const title = useRef(null);
  const typeButtons = useRef(new Map());
  useEffect(() => {
    title.current?.focus();
  }, [p.panel, p.step]);
  const {
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
    savedReports,
    openSaved,
    queue,
    submission,
    job,
  } = p;
  const data = catalog.data;
  const busy =
    submission.isLoading || ["QUEUED", "GENERATING"].includes(job.data?.state);
  const notification = (kind, title, subtitle) => (
    <InlineNotification
      kind={kind}
      title={title}
      subtitle={subtitle}
      hideCloseButton
    />
  );
  const pending = t("reporting.design.pending");
  const declaredTypes = ["SAMPLE_TESTING", "REFERRALS", "NON_CONFORMANCE"];
  const connectedTypes = declaredTypes.filter((id) =>
    types.data?.some((type) => type.id === id),
  );
  const labelFor = (id) =>
    types.data?.find((item) => item.id === id)?.label ||
    t(`reporting.design.type.${id}`);
  const card = (title, children, testId) => (
    <section className="card" aria-label={title} data-testid={testId}>
      <div className="card-header">
        <h2 className="card-title">{title}</h2>
      </div>
      <div className="card-body">{children}</div>
    </section>
  );
  const savedList = (management) => (
    <>
      {savedReports.isLoading && (
        <InlineLoading description={t("reporting.saved.loading")} />
      )}
      {savedReports.error &&
        notification("error", t("reporting.saved.loadError"))}
      {savedReports.data?.reports.length === 0 && (
        <p>{t("reporting.saved.empty")}</p>
      )}
      <div className="saved-report-list">
        {savedReports.data?.reports.map((saved) => (
          <article
            className="saved-report"
            aria-label={saved.name}
            key={saved.id}
          >
            <div>
              <strong>{saved.name}</strong>
              <span>
                {t("reporting.design.savedFields", {
                  count: saved.definition.selectedVariables.length,
                })}
              </span>
            </div>
            <div className="queue-actions">
              <Button
                kind="tertiary"
                size="sm"
                onClick={() => openSaved(saved)}
              >
                {t("reporting.design.useReport")}
              </Button>
              {management && (
                <>
                  <Button
                    kind="ghost"
                    size="sm"
                    onClick={() => openSaved(saved, true)}
                  >
                    {t("reporting.saved.copy")}
                  </Button>
                  <Button
                    kind="danger--ghost"
                    size="sm"
                    onClick={() => p.setDeleteCandidate(saved)}
                  >
                    {t("reporting.saved.delete")}
                  </Button>
                </>
              )}
            </div>
          </article>
        ))}
      </div>
    </>
  );
  const preview = selected.length > 0 && (
    <section
      className="column-preview"
      aria-label={t("reporting.headerPreview")}
    >
      <h2>{t("reporting.design.csvPreview")}</h2>
      <p className="column-hint">{t("reporting.design.previewHelp")}</p>
      <div
        className="column-preview-scroll"
        tabIndex="0"
        role="region"
        aria-label={t("reporting.design.scrollPreview")}
      >
        <table>
          <thead>
            <tr>
              {selected.map((id) => (
                <th scope="col" key={id}>
                  {byId.get(id)?.label || id}
                </th>
              ))}
            </tr>
          </thead>
        </table>
      </div>
    </section>
  );
  const status = (item) => (
    <Tag
      type={
        {
          READY: "green",
          FAILED: "red",
          EXPIRED: "gray",
          CANCELLED: "gray",
          QUEUED: "blue",
          GENERATING: "cyan",
        }[item.state] || "gray"
      }
    >
      {t(`reporting.state.${item.state}`)}
    </Tag>
  );
  const jobActions = (item) => (
    <div className="queue-actions">
      {item.state === "READY" && (
        <Button
          kind="primary"
          size="sm"
          as="a"
          href={downloadUrl(item.id)}
          renderIcon={Download}
        >
          {t("reporting.download")}
        </Button>
      )}
      {["QUEUED", "FAILED", "EXPIRED"].includes(item.state) && (
        <Button
          kind={item.state === "QUEUED" ? "tertiary" : "primary"}
          size="sm"
          disabled={p.recovery.isLoading}
          onClick={() =>
            ({
              QUEUED: p.requestCancel,
              FAILED: p.retryJob,
              EXPIRED: p.rerunJob,
            })[item.state](item)
          }
        >
          {t(
            {
              QUEUED: "common.cancel",
              FAILED: "common.retry",
              EXPIRED: "reporting.design.jobAction.EXPIRED",
            }[item.state],
          )}
        </Button>
      )}
      {item.state !== "GENERATING" && (
        <Button kind="ghost" size="sm" disabled title={pending}>
          {t("reporting.saved.confirmDelete")}
        </Button>
      )}
      <Button
        kind="ghost"
        size="sm"
        aria-expanded={expandedJob === item.id}
        onClick={() => setExpandedJob(expandedJob === item.id ? null : item.id)}
      >
        {t("reporting.design.details")}
      </Button>
    </div>
  );
  const jobDetails = (item) => (
    <div className="queue-detail-content">
      <p>
        {t(`reporting.layout.${item.request.layout || "SPREADSHEET"}`)} ·{" "}
        {item.request.filterSpec.dateFrom} – {item.request.filterSpec.dateTo}
      </p>
      <ol>
        {item.request.variables?.map((field) => (
          <li key={field.id}>{field.label}</li>
        ))}
      </ol>
      {item.expiresAt && (
        <p>
          {t("reporting.design.expires", {
            date: intl.formatDate(item.expiresAt),
          })}
        </p>
      )}
      {item.failureCode && (
        <p role="alert">{p.errorText({ message: item.failureCode })}</p>
      )}
    </div>
  );
  return (
    <div className="reporting-design">
      <div className="page-header">
        <div>
          <PageBreadCrumb
            breadcrumbs={[
              { label: "home.label", link: "/" },
              { label: "banner.menu.reports", link: "" },
              {
                label:
                  panel === "queue" ? "reporting.queue" : "reporting.title",
                link: "",
              },
            ]}
          />
          <h1 className="page-title" ref={title} tabIndex={-1}>
            {t(panel === "queue" ? "reporting.queue" : "reporting.title")}
          </h1>
          <p className="page-subtitle">
            {t(
              panel === "queue"
                ? "reporting.design.queueHelp"
                : "reporting.design.subtitle",
            )}
          </p>
        </div>
        <div className="queue-actions">
          {panel !== "overview" && (
            <Button
              size="md"
              kind="tertiary"
              renderIcon={ArrowLeft}
              onClick={() => setPanel("overview")}
            >
              {t("reporting.design.overview")}
            </Button>
          )}
          {panel === "queue" ? (
            <>
              <Button
                size="md"
                kind="tertiary"
                disabled={!draft.started}
                onClick={() => setPanel("builder")}
              >
                {t("reporting.design.continue")}
              </Button>
              <Button size="md" onClick={startFresh}>
                {t("reporting.design.newExport")}
              </Button>
            </>
          ) : (
            <Button
              size="md"
              kind="tertiary"
              renderIcon={ArrowRight}
              onClick={() => setPanel("queue")}
            >
              {t("reporting.queue")}
            </Button>
          )}
        </div>
      </div>
      <div className="page-body">
        {(types.error || catalog.error) &&
          notification(
            "error",
            t("reporting.loadError"),
            p.errorText(types.error || catalog.error),
          )}
        {p.linkedSavedError &&
          notification(
            "error",
            t("reporting.saved.loadError"),
            p.errorText(p.linkedSavedError),
          )}
        {panel === "overview" && (
          <div className="start-grid">
            <section
              className="start-card"
              aria-labelledby="reporting-new-title"
            >
              <h2 id="reporting-new-title">{t("reporting.design.newTitle")}</h2>
              <p>{t("reporting.design.newHelp")}</p>
              <div className="queue-actions">
                <Button size="md" onClick={startFresh}>
                  {t("reporting.design.start")}
                </Button>
                {draft.started && (
                  <Button
                    size="md"
                    kind="tertiary"
                    onClick={() => setPanel("builder")}
                  >
                    {t("reporting.design.continue")}
                  </Button>
                )}
              </div>
            </section>
            <section
              className="start-card"
              aria-labelledby="reporting-saved-title"
            >
              <h2 id="reporting-saved-title">
                {t("reporting.design.savedTitle")}
              </h2>
              <p>{t("reporting.design.savedHelp")}</p>
              {savedList(false)}
              <Button kind="ghost" size="sm" onClick={() => setPanel("saved")}>
                {t("reporting.saved.library")}
              </Button>
            </section>
          </div>
        )}
        {panel === "saved" &&
          card(
            t("reporting.saved.library"),
            <>
              <Search
                id="reporting-saved-search"
                labelText={t("reporting.saved.search")}
                placeholder={t("reporting.saved.search")}
                value={p.savedSearch}
                onChange={(e) => p.setSavedSearch(e.target.value)}
              />
              {savedList(true)}
            </>,
          )}
        {panel === "queue" && (
          <section aria-label={t("reporting.queue")}>
            {queue.isLoading && (
              <InlineLoading description={t("reporting.loading")} />
            )}
            {queue.error && notification("error", t("reporting.loadError"))}
            {p.recovery.error &&
              notification("error", p.errorText(p.recovery.error))}
            {expandedJob && p.linkedJob.isLoading && (
              <InlineLoading description={t("reporting.loading")} />
            )}
            {expandedJob &&
              p.linkedJob.error &&
              notification(
                "error",
                t("reporting.loadError"),
                p.errorText(p.linkedJob.error),
              )}
            {p.linkedJob.data &&
              !queue.data?.jobs.some((item) => item.id === expandedJob) &&
              card(
                p.linkedJob.data.request.definition.label,
                <>
                  {status(p.linkedJob.data)}
                  {jobDetails(p.linkedJob.data)}
                  {jobActions(p.linkedJob.data)}
                </>,
                `reporting-job-${p.linkedJob.data.id}`,
              )}
            {queue.data?.jobs.length === 0 && (
              <div className="empty-state">
                <p>
                  {t(
                    p.queuePage
                      ? "reporting.design.emptyQueuePage"
                      : "reporting.queueEmpty",
                  )}
                </p>
                <Button
                  size="md"
                  kind="ghost"
                  onClick={p.queuePage ? () => p.setQueuePage(0) : startFresh}
                >
                  {t(
                    p.queuePage
                      ? "reporting.design.firstQueuePage"
                      : "reporting.design.start",
                  )}
                </Button>
              </div>
            )}
            {!!queue.data?.jobs.length && (
              <div className="card queue-card">
                <table className="queue-table">
                  <thead>
                    <tr>
                      {[
                        "jobName",
                        "report",
                        "period",
                        "status",
                        "size",
                        "submitted",
                        "actions",
                      ].map((key) => (
                        <th key={key}>
                          {t(
                            {
                              period: "common.dateRange",
                              status: "common.status",
                              actions: "common.actions",
                            }[key] || `reporting.design.queue.${key}`,
                          )}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {queue.data.jobs.map((item) => (
                      <React.Fragment key={item.id}>
                        <tr data-testid={`reporting-job-${item.id}`}>
                          <td data-label={t("reporting.design.queue.jobName")}>
                            <strong>{item.request.definition.label}</strong>
                            <small>{item.id}</small>
                          </td>
                          <td data-label={t("reporting.design.queue.report")}>
                            {item.request.definition.label}
                          </td>
                          <td data-label={t("common.dateRange")}>
                            {item.request.filterSpec.dateFrom} –{" "}
                            {item.request.filterSpec.dateTo}
                          </td>
                          <td data-label={t("common.status")}>
                            {status(item)}
                          </td>
                          <td data-label={t("reporting.design.queue.size")}>
                            {item.rowCount == null
                              ? "—"
                              : t("reporting.rows", { count: item.rowCount })}
                            {item.fileSize != null &&
                              ` · ${intl.formatNumber(item.fileSize / 1024, { maximumFractionDigits: 1 })} KB`}
                          </td>
                          <td
                            data-label={t("reporting.design.queue.submitted")}
                          >
                            {item.submittedAt
                              ? intl.formatDate(item.submittedAt, {
                                  dateStyle: "short",
                                  timeStyle: "short",
                                })
                              : "—"}
                          </td>
                          <td
                            className="queue-action-cell"
                            data-label={t("common.actions")}
                          >
                            {jobActions(item)}
                          </td>
                        </tr>
                        {expandedJob === item.id && (
                          <tr className="queue-detail">
                            <td colSpan="7">{jobDetails(item)}</td>
                          </tr>
                        )}
                      </React.Fragment>
                    ))}
                  </tbody>
                </table>
                <div className="queue-pagination">
                  <span>
                    {t("reporting.design.jobsShown", {
                      count: queue.data.jobs.length,
                    })}
                  </span>
                  <div className="queue-actions">
                    <Button
                      kind="tertiary"
                      size="sm"
                      disabled={!p.queuePage}
                      onClick={() => p.setQueuePage(p.queuePage - 1)}
                    >
                      {t("reporting.previous")}
                    </Button>
                    <Button
                      kind="tertiary"
                      size="sm"
                      disabled={!queue.data.hasMore}
                      onClick={() => p.setQueuePage(p.queuePage + 1)}
                    >
                      {t("reporting.next")}
                    </Button>
                  </div>
                </div>
              </div>
            )}
            <p className="column-hint">{t("reporting.design.queuePending")}</p>
          </section>
        )}
        {panel === "builder" && p.restoringSaved && !p.linkedSavedError && (
          <InlineLoading description={t("reporting.saved.loading")} />
        )}
        {panel === "builder" && p.linkedSavedError && (
          <Button size="md" kind="tertiary" onClick={startFresh}>
            {t("reporting.design.start")}
          </Button>
        )}
        {panel === "builder" && !p.restoringSaved && (
          <>
            <div className="stepper" aria-label={t("reporting.design.steps")}>
              {["chooseColumns", "setFilters", "reviewSubmit"].map(
                (key, index) => (
                  <React.Fragment key={key}>
                    <div
                      className="step-wrap"
                      aria-current={step === index + 1 ? "step" : undefined}
                    >
                      <div
                        className={`step-circle ${index + 1 <= step ? "current" : "pending"}`}
                      >
                        {index + 1 < step ? "✓" : index + 1}
                      </div>
                      <div
                        className={`step-label ${step === index + 1 ? "current" : ""}`}
                      >
                        {t(`reporting.design.${key}`)}
                      </div>
                    </div>
                    {index < 2 && (
                      <div
                        className={`step-line ${index + 1 < step ? "done" : ""}`}
                      />
                    )}
                  </React.Fragment>
                ),
              )}
            </div>
            {p.savedNotice && notification("success", p.savedNotice)}
            {p.freshDatePrompt &&
              (!draft.dateFrom || !draft.dateTo) &&
              notification("info", t("reporting.saved.freshDates"))}
            {p.unavailableFilters &&
              notification("info", t("reporting.filters.unavailable"))}
            {p.savedError && notification("error", p.errorText(p.savedError))}
            {catalog.isLoading && draft.reportType && (
              <InlineLoading description={t("reporting.loading")} />
            )}
            {step === 1 && (
              <>
                {!selected.length && (
                  <>
                    <h2 className="type-question">
                      {t("reporting.design.typeQuestion")}
                    </h2>
                    <p className="column-hint">
                      {t("reporting.design.typeHelp")}
                    </p>
                    <div
                      className="type-grid"
                      role="radiogroup"
                      aria-label={t("reporting.reportType")}
                    >
                      {declaredTypes.map((id) => {
                        const available = types.data?.find(
                          (type) => type.id === id,
                        );
                        return (
                          <Button
                            size="md"
                            kind="tertiary"
                            key={id}
                            role="radio"
                            aria-checked={draft.reportType === id}
                            aria-disabled={!available}
                            tabIndex={
                              available &&
                              (draft.reportType === id ||
                                (!connectedTypes.includes(draft.reportType) &&
                                  connectedTypes[0] === id))
                                ? 0
                                : -1
                            }
                            ref={(element) => {
                              if (element) typeButtons.current.set(id, element);
                              else typeButtons.current.delete(id);
                            }}
                            onKeyDown={(event) => {
                              const direction = {
                                ArrowRight: 1,
                                ArrowDown: 1,
                                ArrowLeft: -1,
                                ArrowUp: -1,
                              }[event.key];
                              if (!direction || !available) return;
                              event.preventDefault();
                              const next =
                                connectedTypes[
                                  (connectedTypes.indexOf(id) +
                                    direction +
                                    connectedTypes.length) %
                                    connectedTypes.length
                                ];
                              p.chooseType(
                                types.data.find((type) => type.id === next),
                              );
                              typeButtons.current.get(next)?.focus();
                            }}
                            className={`type-card ${draft.reportType === id ? "selected" : ""}`}
                            onClick={() => {
                              if (available) p.chooseType(available);
                            }}
                          >
                            <strong>{labelFor(id)}</strong>
                            <span>{t(`reporting.design.typeHelp.${id}`)}</span>
                            {!available && (
                              <span className="pending-label">{pending}</span>
                            )}
                          </Button>
                        );
                      })}
                    </div>
                    {!!types.data?.some(
                      (type) => !declaredTypes.includes(type.id),
                    ) && (
                      <div className="configured-reports">
                        <Dropdown
                          id="reporting-configured-source"
                          titleText={t("reporting.design.configuredReports")}
                          label={t("reporting.design.chooseConfigured")}
                          items={types.data.filter(
                            (type) => !declaredTypes.includes(type.id),
                          )}
                          itemToString={(item) => item?.label || ""}
                          selectedItem={
                            types.data.find(
                              (type) =>
                                type.id === draft.reportType &&
                                !declaredTypes.includes(type.id),
                            ) || null
                          }
                          onChange={({ selectedItem }) =>
                            p.chooseType(selectedItem)
                          }
                        />
                      </div>
                    )}
                  </>
                )}
                {draft.reportType && data ? (
                  <>
                    <div className="source-banner">
                      <Information size={20} />
                      <span>
                        {t("reporting.design.building", {
                          type: data.definition.label,
                        })}
                      </span>
                      {!!selected.length && (
                        <Button
                          kind="tertiary"
                          size="sm"
                          onClick={() => update({ reportType: null })}
                        >
                          {t("reporting.design.changeType")}
                        </Button>
                      )}
                    </div>
                    {data.definition.layouts.length > 1 && (
                      <div className="layout-choice">
                        <Dropdown
                          id="reporting-layout"
                          titleText={t("reporting.layout")}
                          label={t("reporting.layout")}
                          items={data.definition.layouts}
                          selectedItem={draft.layout}
                          itemToString={(item) =>
                            item ? t(`reporting.layout.${item}`) : ""
                          }
                          onChange={({ selectedItem }) =>
                            update({ layout: selectedItem })
                          }
                        />
                        <p className="column-hint">
                          {t(`reporting.meaning.${draft.layout}`)}
                        </p>
                      </div>
                    )}
                    <ReportingColumns
                      key={p.columnKey}
                      fields={fields}
                      selected={selected}
                      onChange={p.setColumns}
                    />
                    {preview}
                  </>
                ) : !draft.reportType ? (
                  notification("info", t("reporting.design.chooseType"))
                ) : null}
                <div className="action-bar">
                  <Button
                    size="md"
                    disabled={!data || !selected.length || stale.length > 0}
                    renderIcon={ArrowRight}
                    onClick={() => goStep(2)}
                  >
                    {t("reporting.design.nextFilters")}
                  </Button>
                </div>
              </>
            )}
            {step === 2 && data && (
              <>
                {card(
                  t("common.dateRange"),
                  <>
                    <p className="column-hint">
                      {t("reporting.periodHelp", {
                        // i18n-keys: reporting.dateAnchor.*
                        dateMeaning: t(
                          `reporting.dateAnchor.${data.definition.dateAnchor}`,
                        ),
                        days: data.maxDays,
                        timezone: data.timezone,
                      })}
                    </p>
                    <div className="filter-grid">
                      <TextInput
                        id="reporting-from"
                        type="date"
                        labelText={t("reporting.dateFrom")}
                        value={draft.dateFrom}
                        invalid={!!p.fromError}
                        invalidText={p.fromError ? t(p.fromError) : ""}
                        onBlur={() =>
                          p.setDatesTouched((v) => ({ ...v, from: true }))
                        }
                        onChange={(event) =>
                          update({ dateFrom: event.target.value })
                        }
                      />
                      <TextInput
                        id="reporting-to"
                        type="date"
                        labelText={t("reporting.dateTo")}
                        value={draft.dateTo}
                        invalid={!!p.toError}
                        invalidText={
                          p.toError ? t(p.toError, { days: data.maxDays }) : ""
                        }
                        onBlur={() =>
                          p.setDatesTouched((v) => ({ ...v, to: true }))
                        }
                        onChange={(event) =>
                          update({ dateTo: event.target.value })
                        }
                      />
                    </div>
                    <p className="column-hint">
                      {p.validPeriod
                        ? t("reporting.design.periodDays", {
                            count: p.periodDays,
                          })
                        : t("reporting.dates.range")}
                    </p>
                  </>,
                )}
                {card(
                  t("reporting.design.scopeFilters"),
                  <>
                    <div className="filter-grid">
                      {[
                        [
                          "labSectionIds",
                          "labSections",
                          "allAccessibleSections",
                        ],
                        ["testIds", "tests", "allTests"],
                        ["resultStatuses", "statuses", "finalized"],
                      ]
                        .filter(([name]) => p.supportsFilter(name))
                        .map(([name, key, label]) => (
                          <MultiSelect
                            key={name}
                            id={`reporting-filter-${name}`}
                            titleText={t(
                              `reporting.${name === "resultStatuses" ? "resultStatuses" : key}`,
                            )}
                            label={t(`reporting.${label}`)}
                            items={data[key]}
                            itemToString={(item) => item?.label || ""}
                            selectedItems={data[key].filter((item) =>
                              draft[name].includes(item.id),
                            )}
                            onChange={({ selectedItems }) =>
                              update({
                                [name]: selectedItems.map((item) => item.id),
                              })
                            }
                          />
                        ))}
                    </div>
                    <div className="more-filters">
                      <Button
                        kind="ghost"
                        size="sm"
                        aria-expanded={moreFilters}
                        aria-controls="reporting-more-filters"
                        onClick={() => setMoreFilters(!moreFilters)}
                      >
                        {t(
                          moreFilters
                            ? "reporting.design.hideFilters"
                            : "reporting.design.moreFilters",
                        )}
                      </Button>
                    </div>
                    {moreFilters && (
                      <div id="reporting-more-filters">
                        <p className="column-hint">{pending}</p>
                        <div className="filter-grid">
                          {["sampleStatus", "priority"].map((key) => (
                            <Select
                              key={key}
                              id={`reporting-pending-${key}`}
                              labelText={t(
                                key === "priority"
                                  ? "common.priority"
                                  : `reporting.design.${key}`,
                              )}
                              disabled
                            >
                              <SelectItem value="" text={pending} />
                            </Select>
                          ))}
                          <TextInput
                            id="reporting-pending-referringSite"
                            labelText={t("reporting.design.referringSite")}
                            disabled
                            placeholder={pending}
                          />
                        </div>
                      </div>
                    )}
                  </>,
                )}
                <div className="action-bar">
                  <Button
                    size="md"
                    kind="tertiary"
                    renderIcon={ArrowLeft}
                    onClick={() => goStep(1)}
                  >
                    {t("common.back")}
                  </Button>
                  <Button
                    size="md"
                    renderIcon={ArrowRight}
                    disabled={
                      !!draft.dateFrom && !!draft.dateTo && !p.validPeriod
                    }
                    onClick={() => {
                      p.setDatesTouched({ from: true, to: true });
                      if (p.validPeriod) goStep(3);
                    }}
                  >
                    {t("reporting.design.nextReview")}
                  </Button>
                </div>
              </>
            )}
            {step === 3 && data && (
              <>
                <div className="estimate-banner">
                  <Information size={20} />
                  <span>{t("reporting.design.deliveryHelp")}</span>
                </div>
                {card(
                  t("reporting.design.reviewConfiguration"),
                  <>
                    <div className="review-section">
                      <h3>{t("reporting.reportType")}</h3>
                      <Tag
                        type={
                          sourceTagColors[
                            data.definition.source || data.definition.id
                          ] || "gray"
                        }
                      >
                        {data.definition.label}
                      </Tag>
                      <p>{t(`reporting.layout.${draft.layout}`)}</p>
                    </div>
                    <div className="review-section">
                      <div className="review-heading">
                        <h3>{t("reporting.design.dateFilters")}</h3>
                        <Button
                          kind="ghost"
                          size="sm"
                          onClick={() => goStep(2)}
                        >
                          {t("reporting.design.change")}
                        </Button>
                      </div>
                      <span className="var-pill">
                        {draft.dateFrom} – {draft.dateTo} ·{" "}
                        {t("reporting.design.periodDays", {
                          count: p.periodDays,
                        })}
                        {" · "}
                        {t(
                          `reporting.dateAnchor.${data.definition.dateAnchor}`,
                        )}
                      </span>
                    </div>
                    <div className="review-section">
                      <h3>
                        {t(
                          data.statuses.length
                            ? "reporting.design.testStatus"
                            : "reporting.tests",
                        )}
                      </h3>
                      <p>
                        {p.lookup(data.tests, p.effectiveFilters.testIds)}
                        {data.statuses.length > 0 && (
                          <>
                            {" · "}
                            {p.effectiveFilters.resultStatuses.length
                              ? p.lookup(
                                  data.statuses,
                                  p.effectiveFilters.resultStatuses,
                                )
                              : t("reporting.finalized")}
                          </>
                        )}
                      </p>

                      <h3>{t("reporting.design.labScope")}</h3>
                      <span className="var-pill">
                        {p.lookup(
                          data.labSections,
                          p.effectiveFilters.labSectionIds,
                        )}
                      </span>
                    </div>
                    <div className="review-section">
                      <div className="review-heading">
                        <h3>
                          {t("reporting.design.reviewFields", {
                            count: selected.length,
                          })}
                        </h3>
                        <Button
                          kind="ghost"
                          size="sm"
                          onClick={() => goStep(1)}
                        >
                          {t("reporting.edit")}
                        </Button>
                      </div>
                      <ol
                        className="review-columns"
                        aria-label={t("reporting.design.columnOrder")}
                      >
                        {selected.map((id) => (
                          <li key={id}>{byId.get(id)?.label || id}</li>
                        ))}
                      </ol>
                    </div>
                  </>,
                )}
                {card(
                  t("reporting.design.fileName"),
                  <>
                    <TextInput
                      id="reporting-file-name"
                      labelText={t("reporting.design.customFileName")}
                      disabled
                      aria-describedby="reporting-file-name-pending"
                    />
                    <p id="reporting-file-name-pending" className="column-hint">
                      {t("reporting.design.fileNamePending")}
                    </p>
                  </>,
                )}
                <section className="save-config-panel">
                  {draft.savedReport ? (
                    <>
                      <h3>{draft.savedReport.name}</h3>
                      <div className="queue-actions">
                        <Button
                          size="md"
                          kind="tertiary"
                          disabled={
                            !selected.length ||
                            stale.length > 0 ||
                            p.updateSavedBusy
                          }
                          onClick={() => p.setUpdateOpen(true)}
                        >
                          {t("reporting.saved.update")}
                        </Button>
                        <Button size="md" kind="ghost" onClick={p.saveCopy}>
                          {t("reporting.saved.copy")}
                        </Button>
                      </div>
                    </>
                  ) : (
                    <>
                      <Checkbox
                        id="reporting-save-later"
                        labelText={t("reporting.design.saveLater")}
                        checked={saveForLater}
                        onChange={(_, { checked }) => setSaveForLater(checked)}
                      />
                      <p>{t("reporting.design.savedHelp")}</p>
                      {saveForLater && (
                        <div className="save-config-row">
                          <TextInput
                            id="reporting-review-name"
                            labelText={t("reporting.saved.name")}
                            value={p.saveName}
                            maxLength={200}
                            onChange={(event) =>
                              p.setSaveName(event.target.value)
                            }
                          />
                          <Button
                            size="md"
                            kind="tertiary"
                            disabled={
                              !p.saveName.trim() ||
                              p.createSavedBusy ||
                              stale.length > 0
                            }
                            onClick={p.saveCurrent}
                          >
                            {t("reporting.design.saveSettings")}
                          </Button>
                        </div>
                      )}
                    </>
                  )}
                </section>
                <div className="action-bar">
                  <Button
                    size="md"
                    kind="tertiary"
                    renderIcon={ArrowLeft}
                    onClick={() => goStep(2)}
                  >
                    {t("common.back")}
                  </Button>
                  <Button
                    size="md"
                    disabled={
                      !selected.length ||
                      stale.length > 0 ||
                      !p.validPeriod ||
                      busy
                    }
                    onClick={p.run}
                  >
                    {t("reporting.generate")}
                  </Button>
                </div>
              </>
            )}
            {stale.length > 0 &&
              notification("warning", t("reporting.columns.stale"))}
            {submission.error &&
              notification(
                "error",
                t("reporting.submitError"),
                p.errorText(submission.error),
              )}
            {submission.isLoading && (
              <InlineLoading description={t("reporting.submitting")} />
            )}
            {job.data && (
              <section
                className="card current-report"
                aria-label={t("reporting.currentReport")}
              >
                <div className="card-header">
                  <h2>{t("reporting.currentReport")}</h2>
                  {status(job.data)}
                </div>
                <div className="card-body">
                  <p>
                    {job.data.request.definition.label} ·{" "}
                    {job.data.request.filterSpec.dateFrom} –{" "}
                    {job.data.request.filterSpec.dateTo}
                  </p>
                  {job.data.rowCount != null && (
                    <p>{t("reporting.rows", { count: job.data.rowCount })}</p>
                  )}
                  {busy && (
                    <InlineLoading description={t("reporting.generating")} />
                  )}
                  {jobActions(job.data)}
                  {expandedJob === job.data.id && jobDetails(job.data)}
                </div>
              </section>
            )}
            {job.error && notification("error", t("reporting.loadError"))}
          </>
        )}
      </div>
    </div>
  );
}
