import React, { useEffect, useState } from "react";
import { useIntl } from "react-intl";
import { useHistory, useLocation } from "react-router-dom";
import {
  Stack,
  Grid,
  Column,
  TableContainer,
  DataTable,
  Table,
  TableHead,
  TableHeader,
  TableBody,
  TableRow,
  TableCell,
  TableSelectAll,
  TableSelectRow,
  Button,
  Select,
  SelectItem,
  Checkbox,
  TextInput,
  TextArea,
  MultiSelect,
  InlineNotification,
  InlineLoading,
  Pagination,
  Tag,
} from "@carbon/react";
import config from "../../config.json";
import { MICROBIOLOGY_CASE_PATH } from "./MicrobiologyRoutes";
export default function CultureWorklist({ service }) {
  const intl = useIntl(),
    history = useHistory(),
    location = useLocation(),
    t = (id) => intl.formatMessage({ id });
  const [data, setData] = useState(null),
    [error, setError] = useState(false),
    [loading, setLoading] = useState(true),
    [filter, setFilter] = useState(""),
    [attention, setAttention] = useState(false),
    [units, setUnits] = useState([]),
    [search, setSearch] = useState(
      () => new URLSearchParams(location.search).get("q") || "",
    ),
    [page, setPage] = useState(1),
    [size, setSize] = useState(20),
    [refresh, setRefresh] = useState(0),
    [action, setAction] = useState(null),
    [selection, setSelection] = useState([]),
    [outcome, setOutcome] = useState([]),
    [saving, setSaving] = useState(false),
    [options, setOptions] = useState(null),
    [input, setInput] = useState({}),
    [sheet, setSheet] = useState(null);
  useEffect(() => {
    let live = true;
    setLoading(true);
    const q = new URLSearchParams({
      cultureFilter: filter,
      needsAttention: attention,
      q: search,
      page,
      pageSize: size,
    });
    units.forEach((u) => q.append("labUnitIds", u.id));
    service
      .searchBench(q.toString())
      .then((d) => {
        if (live) {
          setData(d);
          setError(false);
        }
      })
      .catch(() => {
        if (live) setError(true);
      })
      .finally(() => {
        if (live) setLoading(false);
      });
    return () => {
      live = false;
    };
  }, [service, filter, attention, units, search, page, size, refresh]);
  const begin = async (kind, selected) => {
    const selectedRows = selected.map((r) =>
      data.rows.find((item) => item.id === r.id),
    );
    setSelection(selectedRows);
    setAction(kind);
    setInput({
      // This input represents an instant in the browser zone, converted back to UTC on save.
      inoculatedAt: new Date(
        Date.now() - new Date().getTimezoneOffset() * 60000,
      )
        .toISOString()
        .slice(0, 16),
      duration: "24",
      durationUnit: "HOURS",
    });
    setError(false);
    try {
      setOptions(await service.getCultureOptions(selectedRows[0].caseId));
    } catch {
      setError(true);
    }
  };
  const save = async () => {
    setSaving(true);
    setError(false);
    const grouped = new Map();
    selection.forEach((row) => {
      if (!grouped.has(row.caseId))
        grouped.set(row.caseId, {
          caseId: row.caseId,
          sampleItemId: row.sampleItemId,
          cultureIds: [],
        });
      if (row.cultureId) grouped.get(row.caseId).cultureIds.push(row.cultureId);
    });
    try {
      const { cultureTestId, ...cultureInput } = input;
      const result = await service.benchAction({
        action,
        cases: [...grouped.values()],
        sampleTypeId: selection[0].sampleTypeId,
        cultureTestId,
        culture: {
          ...cultureInput,
          mediumItemId: input.mediumItemId ? Number(input.mediumItemId) : null,
          lotId: input.lotId ? Number(input.lotId) : null,
          inoculatedAt: input.inoculatedAt
            ? new Date(input.inoculatedAt).toISOString()
            : null,
        },
      });
      setOutcome(result);
      setAction(null);
      setRefresh((n) => n + 1);
    } catch {
      setError(true);
    } finally {
      setSaving(false);
    }
  };
  const print = async (selected) => {
    setError(false);
    const selectedRows = selected.map((r) =>
      data.rows.find((item) => item.id === r.id),
    );
    try {
      const s = await service.printBench({
        labUnitId: selectedRows[0].labUnitId,
        caseIds: [...new Set(selectedRows.map((r) => r.caseId))],
        cultureIds: selectedRows.map((r) => r.cultureId).filter(Boolean),
      });
      setSheet(s);
    } catch {
      setError(true);
    }
  };
  const field = (name, label, type = "text") => (
    <TextInput
      id={`bench-${name}`}
      type={type}
      labelText={t(label)}
      value={input[name] || ""}
      onChange={(e) => setInput({ ...input, [name]: e.target.value })}
    />
  );
  const choice = (name, label, items) => (
    <Select
      id={`bench-${name}`}
      labelText={t(label)}
      value={input[name] || ""}
      onChange={(e) => setInput({ ...input, [name]: e.target.value })}
    >
      <SelectItem value="" text={t("common.select")} />
      {(items || []).map((item) => (
        <SelectItem
          key={item.id}
          value={String(item.id)}
          text={item.value || item.name || item.lotNumber}
        />
      ))}
    </Select>
  );
  // i18n-keys: microbiology.bench.attention.*, microbiology.bench.filter.*, microbiology.bench.action.*
  return (
    <Stack gap={5}>
      <TextInput
        id="bench-search"
        labelText={t("microbiology.case.search")}
        value={search}
        onChange={(e) => {
          setSearch(e.target.value);
          setPage(1);
        }}
      />
      <MultiSelect
        id="bench-units"
        titleText={t("microbiology.case.labUnit")}
        label={t("microbiology.case.allUnits")}
        items={data?.labUnits || []}
        itemToString={(u) => u?.value || ""}
        onChange={({ selectedItems }) => {
          setUnits(selectedItems);
          setPage(1);
        }}
      />
      <Select
        id="bench-filter"
        labelText={t("microbiology.culture.title")}
        value={filter}
        onChange={(e) => {
          setFilter(e.target.value);
          setPage(1);
        }}
      >
        {["", "AWAITING_INOCULATION", "CHECK_DUE", "FINAL_READ_DUE"].map(
          (f) => (
            <SelectItem
              key={f}
              value={f}
              text={t(`microbiology.bench.filter.${f || "ALL"}`)}
            />
          ),
        )}
      </Select>
      <Checkbox
        id="bench-attention"
        labelText={t("microbiology.bench.attentionOnly")}
        checked={attention}
        onChange={(_, { checked }) => {
          setAttention(checked);
          setPage(1);
        }}
      />
      {loading && <InlineLoading description={t("common.loading")} />}
      {error && (
        <InlineNotification
          kind="error"
          title={t("microbiology.bench.error")}
          hideCloseButton
        />
      )}
      <DataTable
        key={`${filter}-${refresh}-${(data?.rows || []).map((row) => row.id).join(",")}`}
        rows={(data?.rows || []).map((r) => ({ ...r, disabled: false }))}
        headers={[
          { key: "accessionNumber", header: t("common.labNumber") },
          { key: "patientName", header: t("common.patient") },
          { key: "cultureId", header: t("microbiology.culture.title") },
          { key: "attention", header: t("microbiology.bench.attentionOnly") },
          { key: "dueAt", header: t("microbiology.bench.due") },
        ]}
      >
        {({
          rows,
          headers,
          getHeaderProps,
          getSelectionProps,
          selectedRows,
        }) => (
          <TableContainer>
            <Stack gap={3}>
              <Grid fullWidth condensed>
                {["NO_GROWTH", "INOCULATE", "EXTEND"].map((kind) => (
                  <Column key={kind} lg={4} md={4} sm={4}>
                    <Button
                      size="sm"
                      disabled={
                        !selectedRows.length ||
                        selectedRows.some(
                          (r) =>
                            !data.rows.find((item) => item.id === r.id)
                              ?.canWrite,
                        ) ||
                        navigator.onLine === false ||
                        (kind === "INOCULATE"
                          ? filter !== "AWAITING_INOCULATION"
                          : !filter || filter === "AWAITING_INOCULATION")
                      }
                      onClick={() => begin(kind, selectedRows)}
                    >
                      {t(`microbiology.bench.action.${kind}`)}
                    </Button>
                  </Column>
                ))}
                <Column lg={4} md={4} sm={4}>
                  <Button
                    kind="tertiary"
                    disabled={
                      !selectedRows.length ||
                      new Set(
                        selectedRows.map(
                          (r) =>
                            data.rows.find((item) => item.id === r.id)
                              ?.labUnitId,
                        ),
                      ).size > 1
                    }
                    onClick={() => print(selectedRows)}
                  >
                    {t("microbiology.bench.print")}
                  </Button>
                </Column>
              </Grid>
              <Table aria-label={t("microbiology.worklist.title")}>
                <TableHead>
                  <TableRow>
                    <TableSelectAll {...getSelectionProps()} />
                    {headers.map((h) => (
                      <TableHeader
                        {...getHeaderProps({ header: h })}
                        key={h.key}
                      >
                        {h.header}
                      </TableHeader>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {rows.map((row) => {
                    const item = data.rows.find((r) => r.id === row.id);
                    return (
                      <TableRow key={row.id}>
                        <TableSelectRow {...getSelectionProps({ row })} />
                        <TableCell>
                          <Button
                            kind="ghost"
                            onClick={() =>
                              history.push(
                                `${MICROBIOLOGY_CASE_PATH}/${item.caseId}?scope=CULTURE${item.cultureId ? `&row=${item.cultureId}` : ""}`,
                              )
                            }
                          >
                            {item.accessionNumber}
                          </Button>
                        </TableCell>
                        <TableCell>{item.patientName}</TableCell>
                        <TableCell>
                          {item.culture?.containerIdentifier ||
                            item.specimenType}
                        </TableCell>
                        <TableCell>
                          {item.attention.map((a) => (
                            <Tag key={a}>
                              {t(`microbiology.bench.attention.${a}`)}
                            </Tag>
                          ))}
                        </TableCell>
                        <TableCell>
                          {item.dueAt
                            ? `${intl.formatDate(new Date(item.dueAt))} ${intl.formatTime(new Date(item.dueAt))}`
                            : ""}
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </Stack>
          </TableContainer>
        )}
      </DataTable>
      <Pagination
        page={page}
        pageSize={size}
        pageSizes={[20, 50, 100]}
        totalItems={data?.total || 0}
        backwardText={t("common.previous")}
        forwardText={t("common.next")}
        itemsPerPageText={t("microbiology.case.itemsPerPage")}
        itemRangeText={(min, max, total) =>
          intl.formatMessage(
            { id: "microbiology.case.range" },
            { min, max, total },
          )
        }
        pageRangeText={(current, total) =>
          intl.formatMessage(
            { id: "microbiology.case.pages" },
            { current, total },
          )
        }
        onChange={({ page, pageSize }) => {
          setPage(page);
          setSize(pageSize);
        }}
      />
      {action && options && (
        <Stack gap={3}>
          <h3>{t(`microbiology.bench.action.${action}`)}</h3>
          {action === "INOCULATE" && (
            <>
              {choice("cultureTestId", "test.name", selection[0].cultureTests)}
              {choice(
                "mediumItemId",
                "microbiology.bench.medium",
                options.media,
              )}
              {choice(
                "lotId",
                "microbiology.bench.lot",
                options.media.find((m) => String(m.id) === input.mediumItemId)
                  ?.lots,
              )}
              <Checkbox
                id="bench-not-tracked"
                labelText={t("microbiology.bench.notTracked")}
                checked={Boolean(input.notTracked)}
                onChange={(_, { checked }) =>
                  setInput({ ...input, notTracked: checked, lotId: "" })
                }
              />
              {choice(
                "atmosphereId",
                "microbiology.bench.atmosphere",
                options.atmospheres,
              )}
              {field("temperature", "microbiology.bench.temperature", "number")}
              {field("duration", "microbiology.bench.duration", "number")}
              {field(
                "inoculatedAt",
                "microbiology.bench.inoculated",
                "datetime-local",
              )}
            </>
          )}
          {action === "EXTEND" &&
            choice(
              "reasonId",
              "microbiology.bench.reason",
              options.extensionReasons,
            )}
          <TextArea
            id="bench-note"
            labelText={t("microbiology.bench.note")}
            value={input.note || ""}
            onChange={(e) => setInput({ ...input, note: e.target.value })}
          />
          <Button
            disabled={saving || navigator.onLine === false}
            onClick={save}
          >
            {t("common.save")}
          </Button>
          <Button kind="secondary" onClick={() => setAction(null)}>
            {t("common.cancel")}
          </Button>
        </Stack>
      )}
      {outcome.map((r) => (
        <Stack key={r.caseId} gap={2}>
          <p>
            {selection.find((s) => s.caseId === r.caseId)?.accessionNumber ||
              r.caseId}
            :{" "}
            {r.error
              ? t("microbiology.bench.error")
              : t("microbiology.bench.saved")}
          </p>
          {r.skipped?.map((s) => (
            <p key={s.cultureId}>
              {s.container}:{" "}
              {intl.formatMessage(
                { id: "microbiology.bench.skipped" },
                { reason: t(`microbiology.bench.skip.${s.reason}`) },
              )}
            </p>
          ))}
          {r.actionId && !r.error && (
            <Button
              kind="ghost"
              onClick={async () => {
                try {
                  await service.undoBench(r.caseId, r.actionId);
                  setOutcome((previous) =>
                    previous.filter((x) => x.actionId !== r.actionId),
                  );
                  setRefresh((n) => n + 1);
                } catch {
                  setError(true);
                }
              }}
            >
              {t("microbiology.bench.undo")}
            </Button>
          )}
        </Stack>
      ))}
      {sheet && (
        <a
          target="_blank"
          rel="noreferrer"
          href={`${config.serverBaseUrl}/rest/microbiology/bench/sheets/${encodeURIComponent(sheet.id)}`}
        >
          {t("microbiology.bench.openSheet")} {sheet.id}
        </a>
      )}
    </Stack>
  );
}
