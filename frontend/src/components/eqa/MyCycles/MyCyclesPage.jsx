import React, { useEffect, useState } from "react";
import {
  Grid,
  Column,
  Section,
  Heading,
  Tile,
  Select,
  SelectItem,
  Search,
  Button,
  Tag,
  InlineNotification,
  ActionableNotification,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Loading,
} from "@carbon/react";
import { ChevronDown, ChevronUp } from "@carbon/react/icons";
import { useIntl } from "react-intl";
import { Link as RouterLink, useHistory } from "react-router-dom";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { getFromOpenElisServer } from "../../utils/Utils";
import { fetchMyCycles, submitCycle } from "./cyclesApi";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "eqa.participant.myCycles.title", link: "/qa/eqa/my-cycles" },
];

const BUCKETS = {
  active: ["planned", "panel_received", "testing", "ready_to_submit"],
  awaiting: ["submitted"],
  completed: ["scored", "closed"],
  all: [
    "planned",
    "panel_received",
    "testing",
    "ready_to_submit",
    "submitted",
    "scored",
    "closed",
  ],
};

const STATUS_TAG = {
  planned: "gray",
  panel_received: "teal",
  testing: "blue",
  ready_to_submit: "purple",
  submitted: "cyan",
  scored: "green",
  closed: "gray",
};

const TYPE_TAG = {
  international_pt: "blue",
  regional_pt: "teal",
  inter_lab_split: "purple",
  in_house: "gray",
};

const SCHEME_TYPES = [
  "international_pt",
  "regional_pt",
  "inter_lab_split",
  "in_house",
];

const ENTRY_TAG = {
  entered: "green",
  in_progress: "blue",
  empty: "gray",
};

const resultEntryUrl = (labNo) =>
  `/result?type=order&doRange=false&accessionNumber=${encodeURIComponent(labNo)}`;

// FR-V2.2-07: the pre-submission summary and its Submit action appear together,
// only while a review-gated scheme sits at ready_to_submit.
const reviewGateOpen = (cycle) =>
  cycle.requiresCycleReview && cycle.status === "ready_to_submit";

// A validation is a real instant, so it renders in the viewer's timezone —
// unlike deadlines, which are end-of-day values read as UTC (formatDateOnly).
const validatedAtLabel = (value) =>
  value ? new Date(value).toLocaleString() : "—";

/**
 * Table rows for a cycle's samples. Normally one row per sample with the
 * analytes collapsed into a cell; under the review gate one row per analyte, so
 * each reported value can be checked on its own line (FR-V2.2-07). Only the
 * first row of a sample repeats its identity columns.
 */
const sampleRows = (cycle) => {
  if (!reviewGateOpen(cycle)) {
    return cycle.samples.map((sample) => ({
      key: sample.id,
      sample,
      firstOfSample: true,
      analyteLabel: sample.analytes.map((a) => a.name).join(", "),
    }));
  }
  return cycle.samples.flatMap((sample) =>
    sample.analytes.length === 0
      ? [{ key: sample.id, sample, firstOfSample: true, analyteLabel: "—" }]
      : sample.analytes.map((analyte, i) => ({
          key: `${sample.id}-${analyte.name}-${i}`,
          sample,
          analyte,
          firstOfSample: i === 0,
          analyteLabel: analyte.name,
        })),
  );
};

const kpiValueStyle = { fontSize: "1.75rem", fontWeight: 600 };
const kpiLabelStyle = { fontSize: "0.75rem", color: "#525252" };
const hintStyle = { fontSize: "0.75rem", color: "#525252" };

const MyCyclesPage = () => {
  const intl = useIntl();
  const history = useHistory();
  const t = (id, defaultMessage, values) =>
    intl.formatMessage({ id, defaultMessage }, values);

  const [cycles, setCycles] = useState([]);
  const [uncycledOrders, setUncycledOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [expanded, setExpanded] = useState(null);
  const [bucket, setBucket] = useState("active");
  const [typeFilter, setTypeFilter] = useState("all");
  const [search, setSearch] = useState("");
  const [submitNotice, setSubmitNotice] = useState(null);

  useEffect(() => {
    fetchMyCycles((data) => {
      setCycles(data);
      setLoading(false);
    });
    getFromOpenElisServer("/rest/eqa/orders", (data) => {
      // G2 (decided 2026-08-13): ad-hoc EQA orders stay visible in their own
      // bucket. cycleId lands on the order DTO with T-15; until then every
      // EQA order is uncycled by definition.
      setUncycledOrders((data || []).filter((o) => !o.cycleId));
    });
  }, []);

  const statusLabel = (status) =>
    t(`eqa.cycle.status.${status}`, status.replace(/_/g, " "));

  const statusTag = (status) => (
    <Tag type={STATUS_TAG[status] || "gray"} size="sm">
      {statusLabel(status)}
    </Tag>
  );

  const schemeTypeLabel = (type) =>
    t(`eqa.schemeType.${type}`, type.replace(/_/g, " "));

  const handleSubmit = (cycle) => {
    submitCycle(cycle.id, (updated) => {
      if (updated) {
        // transition response is the bare cycle DTO — keep the row's
        // progress/samples, take the new status
        setCycles((prev) =>
          prev.map((c) =>
            c.id === updated.id ? { ...c, status: updated.status } : c,
          ),
        );
        setSubmitNotice({
          kind: "success",
          text: t(
            "eqa.cycle.submitted.success",
            "Cycle submitted to provider — awaiting scores.",
          ),
        });
      } else {
        setSubmitNotice({
          kind: "error",
          text: t(
            "eqa.cycle.submitted.error",
            "Submit failed — the cycle was not advanced. Check that all results are validated and try again.",
          ),
        });
      }
    });
  };

  // Lab-wide KPIs — deliberately NOT filtered; they describe the whole lab.
  const activeCount = cycles.filter((c) =>
    BUCKETS.active.includes(c.status),
  ).length;
  const readyCount = cycles.filter(
    (c) => c.status === "ready_to_submit",
  ).length;
  const awaitingCount = cycles.filter((c) => c.status === "submitted").length;
  const openNce = cycles.filter((c) => c.hasNce).length;

  const filteredCycles = cycles.filter((c) => {
    if (!BUCKETS[bucket].includes(c.status)) return false;
    if (typeFilter !== "all" && c.schemeType !== typeFilter) return false;
    if (
      search &&
      !`${c.schemeName} ${c.provider} ${c.cycleNumber}`
        .toLowerCase()
        .includes(search.toLowerCase())
    ) {
      return false;
    }
    return true;
  });

  const filtersApplied =
    bucket !== "active" || typeFilter !== "all" || search !== "";

  const clearFilters = () => {
    setBucket("active");
    setTypeFilter("all");
    setSearch("");
  };

  const renderExpanded = (cycle) => (
    <div style={{ padding: "0.5rem 0" }}>
      {cycle.hasNce && (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={cycle.nceRef || "NCE"}
          subtitle={t(
            "eqa.cycle.nceBanner",
            "Unacceptable score on {scheme}. Follow up in the NCE register.",
            { scheme: cycle.schemeName },
          )}
        />
      )}
      {cycle.perAnalyst && (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={t("eqa.cycle.perAnalyst.title", "Per-analyst required")}
          subtitle={t(
            "eqa.cycle.perAnalyst.body",
            "Analysts are mapped at scheme enrollment. The per-analyst column is visible on the standard result entry page.",
          )}
        />
      )}
      {reviewGateOpen(cycle) && (
        <ActionableNotification
          kind="success"
          lowContrast
          hideCloseButton
          inline
          title={t(
            "eqa.cycle.review.title",
            "Optional cycle-level review enabled for this scheme.",
          )}
          subtitle={t(
            "eqa.cycle.review.body",
            "All {total} analytes validated in the standard pipeline. Submit is a single click — validation is the authoritative gate, no re-authentication.",
            { total: cycle.progress.total },
          )}
          actionButtonLabel={t("eqa.btn.reviewSubmit", "Review & submit")}
          onActionButtonClick={() => handleSubmit(cycle)}
        />
      )}
      {cycle.samples.length > 0 ? (
        <>
          <h5 style={{ margin: "0.75rem 0 0.5rem" }}>
            {reviewGateOpen(cycle)
              ? t("eqa.review.title", "Pre-submission summary")
              : t("eqa.progress.title", "Sample progress")}
          </h5>
          {reviewGateOpen(cycle) && (
            <p style={{ ...hintStyle, margin: "0 0 0.5rem" }}>
              {t(
                "eqa.review.subtitle",
                "Read-only summary of every validated result, grouped by sample. This is what will be submitted to the provider.",
              )}
            </p>
          )}
          <Table size="sm">
            <TableHead>
              <TableRow>
                <TableHeader>{t("eqa.sample.labNo", "Lab No")}</TableHeader>
                <TableHeader>
                  {t("eqa.sample.providerId", "Provider sample ID")}
                </TableHeader>
                <TableHeader>
                  {reviewGateOpen(cycle)
                    ? t("eqa.sample.analyte", "Analyte")
                    : t("eqa.sample.analytes", "Analytes")}
                </TableHeader>
                {reviewGateOpen(cycle) && (
                  <>
                    <TableHeader>
                      {t("eqa.sample.reportedValue", "Reported value")}
                    </TableHeader>
                    <TableHeader>
                      {t("eqa.sample.validatedAt", "Validated")}
                    </TableHeader>
                  </>
                )}
                {cycle.perAnalyst && (
                  <TableHeader>
                    {t("eqa.sample.analyst", "Assigned analyst")}
                  </TableHeader>
                )}
                <TableHeader>
                  {t("eqa.sample.entryStatus", "Entry status")}
                </TableHeader>
              </TableRow>
            </TableHead>
            <TableBody>
              {sampleRows(cycle).map((row) => (
                <TableRow key={row.key}>
                  <TableCell>
                    {row.firstOfSample ? (
                      <RouterLink
                        to={resultEntryUrl(row.sample.labNo)}
                        title={t(
                          "eqa.labNo.tooltip",
                          "Open results entry for this lab number",
                        )}
                        style={{ fontFamily: "monospace" }}
                      >
                        {row.sample.labNo}
                      </RouterLink>
                    ) : null}
                  </TableCell>
                  <TableCell>
                    {row.firstOfSample ? row.sample.id : null}
                  </TableCell>
                  <TableCell>{row.analyteLabel}</TableCell>
                  {reviewGateOpen(cycle) && (
                    <>
                      <TableCell>{row.analyte?.value || "—"}</TableCell>
                      <TableCell>
                        {validatedAtLabel(row.analyte?.validatedAt)}
                      </TableCell>
                    </>
                  )}
                  {cycle.perAnalyst && (
                    <TableCell>
                      {row.firstOfSample ? row.sample.analyst || "—" : null}
                    </TableCell>
                  )}
                  <TableCell>
                    {row.firstOfSample ? (
                      <Tag
                        type={ENTRY_TAG[row.sample.entryStatus] || "gray"}
                        size="sm"
                      >
                        {t(
                          `eqa.entry.${row.sample.entryStatus}`,
                          row.sample.entryStatus.replace(/_/g, " "),
                        )}
                      </Tag>
                    ) : null}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          {reviewGateOpen(cycle) && (
            <p style={{ ...hintStyle, marginTop: "0.5rem" }}>
              {t(
                "eqa.review.auditFootnote",
                "Every value above was validated in the standard pipeline, which captured the validating user in the audit trail. Submitting records the acknowledgement only — no separate sign-off.",
              )}
            </p>
          )}
        </>
      ) : (
        <div style={{ fontStyle: "italic", color: "#525252" }}>
          {cycle.status === "scored" &&
            t(
              "eqa.cycle.scoredCopy",
              "Scoring complete. Scored results appear under Lab Performance.",
            )}
          {cycle.status === "submitted" &&
            t(
              "eqa.cycle.submittedCopy",
              "Submitted to provider — awaiting scores.",
            )}
          {cycle.status === "closed" &&
            t("eqa.cycle.closedCopy", "Cycle closed.")}
          {cycle.status === "panel_received" &&
            t(
              "eqa.cycle.panelReceivedCopy",
              "Panel received. Proceed to standard result entry to begin testing.",
            )}
          {cycle.status === "planned" && (
            <>
              <p style={{ margin: "0 0 0.5rem" }}>
                {t(
                  "eqa.cycle.plannedCopy",
                  "Panel has not yet been received at this lab. Open Add Order with the EQA box checked to record the receipt.",
                )}
              </p>
              <Button
                size="sm"
                kind="tertiary"
                onClick={() => history.push("/SamplePatientEntry?isEQA=true")}
              >
                {t("eqa.btn.receivePanel", "Receive panel — open Add Order")}
              </Button>
            </>
          )}
        </div>
      )}
    </div>
  );

  if (loading) {
    return <Loading />;
  }

  const columnCount = 7;

  return (
    <div className="pageContent">
      <PageBreadCrumb breadcrumbs={breadcrumbs} />

      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              {t("eqa.participant.myCycles.title", "My EQA Cycles")}
            </Heading>
            <p style={{ color: "#525252", marginBottom: "1rem" }}>
              {t(
                "eqa.participant.myCycles.subtitle",
                "Cycles your lab is participating in. Result entry and validation happen in the standard OpenELIS result pipeline — this page tracks progress and routes you there.",
              )}
            </p>
          </Section>

          <Grid condensed style={{ marginBottom: "1.5rem" }}>
            <Column lg={4} md={2} sm={4}>
              <Tile data-testid="kpi-active">
                <h4 style={kpiLabelStyle}>
                  {t("eqa.kpi.active", "Active cycles")}
                </h4>
                <p style={kpiValueStyle}>{activeCount}</p>
                <span style={hintStyle}>
                  {t("eqa.kpi.active.hint", "in flight")}
                </span>
              </Tile>
            </Column>
            <Column lg={4} md={2} sm={4}>
              <Tile data-testid="kpi-ready">
                <h4 style={kpiLabelStyle}>
                  {t("eqa.kpi.readyToSubmit", "Ready to submit")}
                </h4>
                <p style={kpiValueStyle}>{readyCount}</p>
                <span style={hintStyle}>
                  {t("eqa.kpi.readyToSubmit.hint", "with review flag on")}
                </span>
              </Tile>
            </Column>
            <Column lg={4} md={2} sm={4}>
              <Tile data-testid="kpi-awaiting">
                <h4 style={kpiLabelStyle}>
                  {t("eqa.kpi.awaiting", "Awaiting scores")}
                </h4>
                <p style={kpiValueStyle}>{awaitingCount}</p>
                <span style={hintStyle}>
                  {t("eqa.kpi.awaiting.hint", "submitted to provider")}
                </span>
              </Tile>
            </Column>
            <Column lg={4} md={2} sm={4}>
              <Tile data-testid="kpi-nce">
                <h4 style={kpiLabelStyle}>
                  {t("eqa.kpi.openNce", "Open EQA-linked NCE")}
                </h4>
                <p
                  style={{
                    ...kpiValueStyle,
                    color: openNce > 0 ? "#da1e28" : "inherit",
                  }}
                >
                  {openNce}
                </p>
                <span style={hintStyle}>
                  {openNce > 0
                    ? t("eqa.kpi.openNce.attention", "needs attention")
                    : t("eqa.kpi.openNce.none", "none open")}
                </span>
              </Tile>
            </Column>
          </Grid>

          {submitNotice && (
            <InlineNotification
              kind={submitNotice.kind}
              lowContrast
              title={submitNotice.text}
              onCloseButtonClick={() => setSubmitNotice(null)}
              style={{ marginBottom: "1rem" }}
            />
          )}

          <InlineNotification
            kind="info"
            lowContrast
            hideCloseButton
            title={t(
              "eqa.participant.infoTitle",
              "EQA results flow through standard validation.",
            )}
            subtitle={t(
              "eqa.participant.infoBody",
              "Click a Lab No to open standard result entry for that sample. Once validation completes, the cycle advances to Submitted (or Ready to submit if the scheme requires cycle-level review).",
            )}
            style={{ marginBottom: "1rem", maxWidth: "none" }}
          />

          <Grid condensed style={{ marginBottom: "0.5rem" }}>
            <Column lg={4} md={2} sm={4}>
              <Select
                id="cycle-bucket-filter"
                labelText={t("eqa.bucket.label", "Status")}
                value={bucket}
                onChange={(e) => {
                  setBucket(e.target.value);
                  setExpanded(null);
                }}
              >
                {Object.keys(BUCKETS).map((k) => {
                  const count = cycles.filter((c) =>
                    BUCKETS[k].includes(c.status),
                  ).length;
                  return (
                    <SelectItem
                      key={k}
                      value={k}
                      text={`${t(`eqa.bucket.${k}`, k)} (${count})`}
                    />
                  );
                })}
              </Select>
            </Column>
            <Column lg={4} md={2} sm={4}>
              <Select
                id="cycle-type-filter"
                labelText={t("eqa.filter.schemeType", "Scheme type")}
                value={typeFilter}
                onChange={(e) => setTypeFilter(e.target.value)}
              >
                <SelectItem
                  value="all"
                  text={t("eqa.schemeType.all", "All types")}
                />
                {SCHEME_TYPES.map((type) => (
                  <SelectItem
                    key={type}
                    value={type}
                    text={schemeTypeLabel(type)}
                  />
                ))}
              </Select>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <Search
                id="cycle-search"
                labelText={t(
                  "eqa.filter.searchCycles",
                  "Search scheme, provider, or cycle",
                )}
                placeholder={t(
                  "eqa.filter.searchCycles",
                  "Search scheme, provider, or cycle",
                )}
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </Column>
          </Grid>

          <div style={{ marginBottom: "0.75rem", ...hintStyle }}>
            {t("eqa.filter.showing", "Showing {shown} of {total} cycles", {
              shown: filteredCycles.length,
              total: cycles.length,
            })}
            {" · "}
            {t(`eqa.bucket.${bucket}.hint`, "")}
            {filtersApplied && (
              <Button kind="ghost" size="sm" onClick={clearFilters}>
                {t("eqa.filter.clear", "Clear filters")}
              </Button>
            )}
          </div>

          <Table data-testid="cycles-table">
            <TableHead>
              <TableRow>
                <TableHeader>{t("eqa.col.scheme", "Scheme")}</TableHeader>
                <TableHeader>{t("eqa.col.type", "Type")}</TableHeader>
                <TableHeader>{t("eqa.col.cycle", "Cycle")}</TableHeader>
                <TableHeader>{t("eqa.col.status", "Status")}</TableHeader>
                <TableHeader>{t("eqa.col.deadline", "Deadline")}</TableHeader>
                <TableHeader>{t("eqa.col.progress", "Progress")}</TableHeader>
                <TableHeader aria-label="expand" />
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredCycles.length === 0 && (
                <TableRow>
                  <TableCell colSpan={columnCount}>
                    {t(
                      "eqa.cycle.emptyFiltered",
                      "No cycles match these filters.",
                    )}
                  </TableCell>
                </TableRow>
              )}
              {filteredCycles.map((c) => (
                <React.Fragment key={c.id}>
                  <TableRow
                    onClick={() => setExpanded(expanded === c.id ? null : c.id)}
                    style={{ cursor: "pointer" }}
                    data-testid={`cycle-row-${c.id}`}
                  >
                    <TableCell>
                      <strong>{c.schemeName}</strong>
                      <br />
                      <span style={hintStyle}>{c.provider}</span>
                      {c.hasNce && (
                        <>
                          {" "}
                          <Tag type="red" size="sm">
                            {c.nceRef || "NCE"}
                          </Tag>
                        </>
                      )}
                    </TableCell>
                    <TableCell>
                      <Tag type={TYPE_TAG[c.schemeType] || "gray"} size="sm">
                        {schemeTypeLabel(c.schemeType)}
                      </Tag>
                    </TableCell>
                    <TableCell>{c.cycleNumber}</TableCell>
                    <TableCell>{statusTag(c.status)}</TableCell>
                    <TableCell>{c.deadline}</TableCell>
                    <TableCell>
                      {c.progress.entered} / {c.progress.total}
                    </TableCell>
                    <TableCell>
                      <Button
                        kind="ghost"
                        size="sm"
                        hasIconOnly
                        renderIcon={expanded === c.id ? ChevronUp : ChevronDown}
                        iconDescription={t(
                          "eqa.cycle.toggleDetails",
                          "Toggle cycle details",
                        )}
                        onClick={(e) => {
                          e.stopPropagation();
                          setExpanded(expanded === c.id ? null : c.id);
                        }}
                      />
                    </TableCell>
                  </TableRow>
                  {expanded === c.id && (
                    <TableRow data-testid={`cycle-expanded-${c.id}`}>
                      <TableCell colSpan={columnCount}>
                        {renderExpanded(c)}
                      </TableCell>
                    </TableRow>
                  )}
                </React.Fragment>
              ))}
            </TableBody>
          </Table>

          {uncycledOrders.length > 0 && (
            <Section style={{ marginTop: "2rem" }}>
              <h4>{t("eqa.uncycled.title", "Uncycled EQA orders")}</h4>
              <p style={{ ...hintStyle, marginBottom: "0.5rem" }}>
                {t(
                  "eqa.uncycled.subtitle",
                  "EQA orders not linked to any cycle. They stay visible here so nothing is lost.",
                )}
              </p>
              <Table size="sm" data-testid="uncycled-table">
                <TableHead>
                  <TableRow>
                    <TableHeader>{t("eqa.sample.labNo", "Lab No")}</TableHeader>
                    <TableHeader>
                      {t("eqa.tests.program", "Program")}
                    </TableHeader>
                    <TableHeader>{t("eqa.col.status", "Status")}</TableHeader>
                    <TableHeader>
                      {t("eqa.col.deadline", "Deadline")}
                    </TableHeader>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {uncycledOrders.map((o) => (
                    <TableRow key={o.id}>
                      <TableCell>
                        {o.labNumber ? (
                          <RouterLink
                            to={resultEntryUrl(o.labNumber)}
                            style={{ fontFamily: "monospace" }}
                          >
                            {o.labNumber}
                          </RouterLink>
                        ) : (
                          "—"
                        )}
                      </TableCell>
                      <TableCell>{o.programName || ""}</TableCell>
                      <TableCell>{o.status || ""}</TableCell>
                      <TableCell>
                        {o.deadline
                          ? new Date(o.deadline).toLocaleDateString()
                          : ""}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </Section>
          )}
        </Column>
      </Grid>
    </div>
  );
};

export default MyCyclesPage;
