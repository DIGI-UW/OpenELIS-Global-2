import React, { useContext, useEffect, useMemo, useState } from "react";
import {
  Button,
  Checkbox,
  FilterableMultiSelect,
  Pagination,
  Select,
  SelectItem,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  TextInput,
} from "@carbon/react";
import { Add } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import CustomDatePicker from "../../common/CustomDatePicker";
import { ConfigurationContext } from "../../layout/Layout";
import { convertAlphaNumLabNumForDisplay } from "../../utils/Utils";
import {
  MAX_BATCH_NAME_LENGTH,
  displayDateToIso,
  priorityTagType,
} from "./batchWorkplanUtils";

const COLUMNS = [
  { key: "accessionNumber", label: "workplan.batch.column.labNumber" },
  { key: "patientName", label: "workplan.batch.column.patient" },
  { key: "testName", label: "workplan.batch.column.test" },
  { key: "panelName", label: "workplan.batch.column.panel" },
  { key: "testSectionName", label: "workplan.batch.column.unit" },
  { key: "priority", label: "workplan.batch.column.priority" },
  { key: "orderDate", label: "workplan.batch.column.orderDate" },
  { key: "sampleType", label: "workplan.batch.column.sampleType" },
];

const distinctOptions = (tests, idKey, nameKey) => {
  const byId = new Map();
  tests.forEach((test) => {
    if (test[idKey] && !byId.has(test[idKey])) {
      byId.set(test[idKey], { id: test[idKey], name: test[nameKey] || "" });
    }
  });
  return Array.from(byId.values()).sort((a, b) => a.name.localeCompare(b.name));
};

const nextSort = (current, key) => {
  if (current.key !== key) return { key, direction: "ASC" };
  if (current.direction === "ASC") return { key, direction: "DESC" };
  return { key: null, direction: "NONE" };
};

/**
 * Pending Tests (OGC-427 FR-WP-002..007, FR-BA-001/002): every pending test the
 * user may work, filtered by test, panel, lab unit, priority and order date,
 * searchable and sortable, and grouped into a batch from the selection.
 */
export default function PendingTestsPanel({
  tests,
  loading,
  canCreate,
  onCreateBatch,
  draftBatches = [],
  onAddToBatch,
}) {
  const intl = useIntl();
  const { configurationProperties = {} } =
    useContext(ConfigurationContext) || {};
  const dateLocale = configurationProperties.DEFAULT_DATE_LOCALE;

  const [selectedIds, setSelectedIds] = useState([]);
  const [search, setSearch] = useState("");
  const [testFilter, setTestFilter] = useState([]);
  const [panelFilter, setPanelFilter] = useState([]);
  const [unitFilter, setUnitFilter] = useState([]);
  const [priorityFilter, setPriorityFilter] = useState([]);
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");
  const [sort, setSort] = useState({ key: null, direction: "NONE" });
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [batchName, setBatchName] = useState("");
  const [targetBatchId, setTargetBatchId] = useState("");

  const testOptions = useMemo(
    () => distinctOptions(tests, "testId", "testName"),
    [tests],
  );
  const panelOptions = useMemo(
    () => distinctOptions(tests, "panelId", "panelName"),
    [tests],
  );
  const unitOptions = useMemo(
    () => distinctOptions(tests, "testSectionId", "testSectionName"),
    [tests],
  );
  const priorityOptions = useMemo(
    () =>
      Array.from(new Set(tests.map((test) => test.priority).filter(Boolean)))
        .sort()
        .map((priority) => ({
          id: priority,
          name: intl.formatMessage({ id: `sample.priority.${priority}` }),
        })),
    [tests, intl],
  );

  const fromIso = displayDateToIso(fromDate, dateLocale);
  const toIso = displayDateToIso(toDate, dateLocale);

  const filtered = useMemo(() => {
    const ids = (items) => new Set(items.map((item) => item.id));
    const tests_ = ids(testFilter);
    const panels = ids(panelFilter);
    const units = ids(unitFilter);
    const priorities = ids(priorityFilter);
    const needle = search.trim().toLowerCase();
    const rows = tests.filter((test) => {
      if (tests_.size && !tests_.has(test.testId)) return false;
      if (panels.size && !panels.has(test.panelId)) return false;
      if (units.size && !units.has(test.testSectionId)) return false;
      if (priorities.size && !priorities.has(test.priority)) return false;
      if (fromIso && (!test.orderDate || test.orderDate < fromIso))
        return false;
      if (toIso && (!test.orderDate || test.orderDate > toIso)) return false;
      if (needle) {
        const haystack = [
          test.accessionNumber,
          test.patientName,
          test.testName,
          test.panelName,
          test.testSectionName,
          test.sampleType,
        ]
          .filter(Boolean)
          .join(" ")
          .toLowerCase();
        if (!haystack.includes(needle)) return false;
      }
      return true;
    });
    if (sort.key) {
      const factor = sort.direction === "DESC" ? -1 : 1;
      rows.sort(
        (a, b) =>
          factor * String(a[sort.key] || "").localeCompare(b[sort.key] || ""),
      );
    }
    return rows;
  }, [
    tests,
    testFilter,
    panelFilter,
    unitFilter,
    priorityFilter,
    fromIso,
    toIso,
    search,
    sort,
  ]);

  const paged = useMemo(
    () => filtered.slice((page - 1) * pageSize, page * pageSize),
    [filtered, page, pageSize],
  );

  // A list that shrinks under the current page (a filter, a created batch)
  // would otherwise leave the viewer on a page past the end.
  useEffect(() => {
    const lastPage = Math.max(1, Math.ceil(filtered.length / pageSize));
    if (page > lastPage) setPage(lastPage);
  }, [filtered.length, pageSize, page]);

  // Rows that left the list (batched by this user or someone else) leave the
  // selection too, so the count on the button is what would be batched.
  useEffect(() => {
    const present = new Set(tests.map((test) => test.analysisId));
    setSelectedIds((current) => current.filter((id) => present.has(id)));
  }, [tests]);

  const selectedSet = useMemo(() => new Set(selectedIds), [selectedIds]);
  const pageSelected =
    paged.length > 0 && paged.every((test) => selectedSet.has(test.analysisId));

  const toggle = (analysisId, checked) =>
    setSelectedIds((current) =>
      checked
        ? current.includes(analysisId)
          ? current
          : [...current, analysisId]
        : current.filter((id) => id !== analysisId),
    );

  const togglePage = (checked) => {
    const visible = paged.map((test) => test.analysisId);
    setSelectedIds((current) =>
      checked
        ? Array.from(new Set([...current, ...visible]))
        : current.filter((id) => !visible.includes(id)),
    );
  };

  const nameTooLong = batchName.trim().length > MAX_BATCH_NAME_LENGTH;

  const create = () => {
    if (selectedIds.length === 0 || nameTooLong) return;
    onCreateBatch(batchName.trim(), selectedIds, () => {
      setSelectedIds([]);
      setBatchName("");
    });
  };

  const addToBatch = () => {
    if (selectedIds.length === 0 || !targetBatchId) return;
    onAddToBatch(Number(targetBatchId), selectedIds, () => {
      setSelectedIds([]);
      setTargetBatchId("");
    });
  };

  const clearFilters = () => {
    setTestFilter([]);
    setPanelFilter([]);
    setUnitFilter([]);
    setPriorityFilter([]);
    setFromDate("");
    setToDate("");
    setSearch("");
  };

  const multiSelect = (id, labelId, items, selected, setSelected) => (
    <FilterableMultiSelect
      id={id}
      titleText={intl.formatMessage({ id: labelId })}
      placeholder={intl.formatMessage({ id: "workplan.batch.filter.any" })}
      items={items}
      selectedItems={selected}
      itemToString={(item) => item?.name || ""}
      onChange={({ selectedItems }) => {
        setSelected(selectedItems || []);
        setPage(1);
      }}
      size="sm"
    />
  );

  return (
    <div className="batch-workplan__pending">
      <div className="batch-workplan__filters">
        {multiSelect(
          "batch-filter-test",
          "workplan.batch.filter.test",
          testOptions,
          testFilter,
          setTestFilter,
        )}
        {multiSelect(
          "batch-filter-panel",
          "workplan.batch.filter.panel",
          panelOptions,
          panelFilter,
          setPanelFilter,
        )}
        {multiSelect(
          "batch-filter-unit",
          "workplan.batch.filter.unit",
          unitOptions,
          unitFilter,
          setUnitFilter,
        )}
        {multiSelect(
          "batch-filter-priority",
          "workplan.batch.filter.priority",
          priorityOptions,
          priorityFilter,
          setPriorityFilter,
        )}
        <CustomDatePicker
          id="batch-filter-from"
          key={`from-${fromDate === "" ? "empty" : "set"}`}
          labelText={intl.formatMessage({ id: "workplan.batch.filter.from" })}
          value={fromDate}
          onChange={(value) => {
            setFromDate(value);
            setPage(1);
          }}
        />
        <CustomDatePicker
          id="batch-filter-to"
          key={`to-${toDate === "" ? "empty" : "set"}`}
          labelText={intl.formatMessage({ id: "workplan.batch.filter.to" })}
          value={toDate}
          onChange={(value) => {
            setToDate(value);
            setPage(1);
          }}
        />
        <TextInput
          id="batch-filter-search"
          size="sm"
          labelText={intl.formatMessage({ id: "workplan.batch.search" })}
          placeholder={intl.formatMessage({
            id: "workplan.batch.search.placeholder",
          })}
          value={search}
          onChange={(event) => {
            setSearch(event.target.value);
            setPage(1);
          }}
        />
        <Button kind="ghost" size="sm" onClick={clearFilters}>
          <FormattedMessage id="workplan.batch.filter.clear" />
        </Button>
      </div>

      {canCreate && (
        <div className="batch-workplan__create">
          <TextInput
            id="batch-name"
            size="sm"
            labelText={intl.formatMessage({ id: "workplan.batch.name" })}
            placeholder={intl.formatMessage({
              id: "workplan.batch.name.placeholder",
            })}
            value={batchName}
            invalid={nameTooLong}
            invalidText={intl.formatMessage(
              { id: "workplan.batch.name.tooLong" },
              { max: MAX_BATCH_NAME_LENGTH },
            )}
            onChange={(event) => setBatchName(event.target.value)}
          />
          <Button
            size="sm"
            renderIcon={Add}
            onClick={create}
            disabled={selectedIds.length === 0 || loading || nameTooLong}
          >
            <FormattedMessage
              id="workplan.batch.create"
              values={{ count: selectedIds.length }}
            />
          </Button>
          {draftBatches.length > 0 && (
            <>
              <Select
                id="batch-add-target"
                size="sm"
                labelText={intl.formatMessage({
                  id: "workplan.batch.addTo.label",
                })}
                value={targetBatchId}
                onChange={(event) => setTargetBatchId(event.target.value)}
              >
                <SelectItem
                  value=""
                  text={intl.formatMessage({
                    id: "workplan.batch.addTo.select",
                  })}
                />
                {draftBatches.map((batch) => (
                  <SelectItem
                    key={batch.id}
                    value={String(batch.id)}
                    text={batch.name}
                  />
                ))}
              </Select>
              <Button
                size="sm"
                kind="secondary"
                onClick={addToBatch}
                disabled={selectedIds.length === 0 || !targetBatchId || loading}
              >
                <FormattedMessage
                  id="workplan.batch.addTo"
                  values={{ count: selectedIds.length }}
                />
              </Button>
            </>
          )}
        </div>
      )}

      <Table size="sm" useZebraStyles>
        <TableHead>
          <TableRow>
            <TableHeader className="batch-workplan__select-col">
              <Checkbox
                id="batch-workplan-select-page"
                hideLabel
                labelText={intl.formatMessage({
                  id: "workplan.batch.selectPage",
                })}
                checked={pageSelected}
                onChange={(_, { checked }) => togglePage(checked)}
              />
            </TableHeader>
            {COLUMNS.map((column) => (
              <TableHeader
                key={column.key}
                isSortable
                sortDirection={
                  sort.key === column.key ? sort.direction : "NONE"
                }
                isSortHeader={sort.key === column.key}
                onClick={() =>
                  setSort((current) => nextSort(current, column.key))
                }
              >
                {intl.formatMessage({ id: column.label })}
              </TableHeader>
            ))}
            <TableHeader>
              <FormattedMessage id="workplan.batch.column.status" />
            </TableHeader>
          </TableRow>
        </TableHead>
        <TableBody>
          {paged.map((test) => (
            <TableRow key={test.analysisId}>
              <TableCell>
                <Checkbox
                  id={`batch-workplan-${test.analysisId}`}
                  hideLabel
                  labelText={intl.formatMessage(
                    { id: "workplan.batch.selectTest" },
                    { accessionNumber: test.accessionNumber },
                  )}
                  checked={selectedSet.has(test.analysisId)}
                  onChange={(_, { checked }) =>
                    toggle(test.analysisId, checked)
                  }
                />
              </TableCell>
              <TableCell>
                {convertAlphaNumLabNumForDisplay(test.accessionNumber)}
              </TableCell>
              <TableCell>{test.patientName}</TableCell>
              <TableCell>{test.testName}</TableCell>
              <TableCell>{test.panelName}</TableCell>
              <TableCell>{test.testSectionName}</TableCell>
              <TableCell>
                {test.priority && (
                  <Tag type={priorityTagType(test.priority)} size="sm">
                    <FormattedMessage id={`sample.priority.${test.priority}`} />
                  </Tag>
                )}
              </TableCell>
              <TableCell>{test.orderDateForDisplay}</TableCell>
              <TableCell>{test.sampleType}</TableCell>
              <TableCell>
                <Tag type={test.nonconforming ? "red" : "gray"} size="sm">
                  {test.statusName}
                </Tag>
              </TableCell>
            </TableRow>
          ))}
          {paged.length === 0 && (
            <TableRow>
              <TableCell colSpan={COLUMNS.length + 2}>
                <FormattedMessage id="workplan.batch.pending.empty" />
              </TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>
      <Pagination
        key={`pending-page-size-${pageSize}`}
        onChange={({ page: nextPage, pageSize: nextSize }) => {
          setPage(nextPage);
          setPageSize(nextSize);
        }}
        page={page}
        pageSize={pageSize}
        pageSizes={[10, 20, 50, 100]}
        totalItems={filtered.length}
        forwardText={intl.formatMessage({ id: "pagination.forward" })}
        backwardText={intl.formatMessage({ id: "pagination.backward" })}
        itemsPerPageText={intl.formatMessage({
          id: "pagination.items-per-page",
        })}
        pageNumberText={intl.formatMessage({ id: "pagination.page-number" })}
      />
    </div>
  );
}
