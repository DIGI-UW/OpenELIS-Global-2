import React, { useCallback, useEffect, useState } from "react";
import {
  Button,
  DataTable,
  DataTableSkeleton,
  DatePicker,
  DatePickerInput,
  Dropdown,
  Modal,
  Pagination,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
} from "@carbon/react";
import { Download, DocumentPdf } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../../config.json";
import { getFromOpenElisServer } from "../../utils/Utils";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import QAEmptyState from "../common/QAEmptyState";
import "./ESignatureLog.css";

/**
 * Electronic Signature Log (OGC-702) at /qa/qms/e-signature-log: filterable,
 * paginated read over the unified electronic_signature table. Read-only;
 * export is OGC-703.
 */

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "sideNav.label.qa", link: "" },
  { label: "sideNav.label.qa.qms", link: "" },
  { label: "sideNav.label.qa.qms.esigLog", link: "" },
];

const HEADERS = [
  { key: "signedAt", labelKey: "qa.qms.esigLog.column.signedAt" },
  { key: "signer", labelKey: "qa.qms.esigLog.column.signer" },
  { key: "action", labelKey: "qa.qms.esigLog.column.action" },
  { key: "subject", labelKey: "qa.qms.esigLog.column.subject" },
  { key: "reason", labelKey: "qa.qms.esigLog.column.reason" },
];

const MEANINGS = ["AUTHORED", "VALIDATED_AND_RELEASED", "REJECTED"];

const RECORD_TYPES = [
  "RESULT",
  "RESULT_BATCH",
  "ANALYSIS",
  "VALIDATION_BATCH",
  "QC_RESULT",
  "REPORT",
];

// Local-timezone yyyy-mm-dd (toISOString shifts to UTC and can be off by a day)
function formatDate(d) {
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${month}-${day}`;
}

function defaultFilters() {
  const to = new Date();
  const from = new Date();
  from.setDate(from.getDate() - 30);
  return {
    fromDate: formatDate(from),
    toDate: formatDate(to),
    signerId: "",
    meaning: "",
    recordType: "",
  };
}

function formatTimestamp(value) {
  return value ? new Date(value).toLocaleString() : "—";
}

const ESignatureLog = () => {
  const intl = useIntl();
  const [draft, setDraft] = useState(defaultFilters);
  const [applied, setApplied] = useState(defaultFilters);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(25);
  const [users, setUsers] = useState([]);
  // "csv" | "pdf" while the empty-filter export confirm is open
  const [pendingExport, setPendingExport] = useState(null);
  // undefined = loading, null = fetch yielded no data
  const [data, setData] = useState();

  const allLabel = intl.formatMessage({ id: "qa.qms.esigLog.filter.all" });

  useEffect(() => {
    getFromOpenElisServer("/rest/users", (response) => {
      if (Array.isArray(response)) {
        setUsers(response);
      }
    });
  }, []);

  const buildFilterParams = useCallback(
    () =>
      new URLSearchParams(
        Object.entries(applied).filter(([, value]) => value !== ""),
      ),
    [applied],
  );

  const fetchLog = useCallback(() => {
    setData(undefined);
    const params = buildFilterParams();
    params.set("page", page);
    params.set("pageSize", pageSize);
    getFromOpenElisServer(`/rest/esig/log?${params}`, (res) =>
      setData(res ?? null),
    );
  }, [buildFilterParams, page, pageSize]);

  useEffect(() => {
    fetchLog();
  }, [fetchLog]);

  const handleDates = (dates) => {
    if (dates.length === 2) {
      setDraft({
        ...draft,
        fromDate: formatDate(dates[0]),
        toDate: formatDate(dates[1]),
      });
    }
  };

  const applyFilters = () => {
    setPage(0);
    setApplied(draft);
  };

  const clearFilters = () => {
    const defaults = defaultFilters();
    setDraft(defaults);
    setPage(0);
    setApplied(defaults);
  };

  const openExport = (format) => {
    const endpoint = format === "pdf" ? "exportPdf" : "export";
    window.open(
      `${config.serverBaseUrl}/rest/esig/log/${endpoint}?${buildFilterParams()}`,
      "_blank",
    );
  };

  // Empty-filter export warns first (OGC-703): only the date range is set,
  // so the export may cover everything up to the 10,000-row cap.
  const handleExport = (format) => {
    if (applied.signerId || applied.meaning || applied.recordType) {
      openExport(format);
    } else {
      setPendingExport(format);
    }
  };

  const meaningLabel = (meaning) =>
    intl.formatMessage({ id: `qa.qms.esigLog.meaning.${meaning}` });

  const meaningItems = [
    { id: "", label: allLabel },
    ...MEANINGS.map((m) => ({ id: m, label: meaningLabel(m) })),
  ];
  const recordTypeItems = [
    { id: "", label: allLabel },
    ...RECORD_TYPES.map((t) => ({ id: t, label: t })),
  ];
  const userItems = [
    { id: "", label: allLabel },
    ...users.map((u) => ({ id: String(u.id), label: u.value })),
  ];

  const rows = (data?.items || []).map((item) => ({
    id: String(item.signatureId),
    signedAt: formatTimestamp(item.signedAt),
    signer: item.signerNamePrinted || "—",
    action: item.signatureMeaning ? meaningLabel(item.signatureMeaning) : "—",
    subject: item.recordType ? `${item.recordType} #${item.recordId}` : "—",
    reason: item.rejectionReason || "—",
  }));

  return (
    <div className="adminPageContent esig-log">
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <h2>
        <FormattedMessage id="qa.qms.esigLog.title" />
      </h2>
      <p className="esig-log__subtitle">
        <FormattedMessage id="qa.qms.esigLog.subtitle" />
      </p>
      <div className="esig-log__filters" data-testid="esig-log-filters">
        <DatePicker
          datePickerType="range"
          dateFormat="Y-m-d"
          value={[draft.fromDate, draft.toDate]}
          onChange={handleDates}
        >
          <DatePickerInput
            id="esig-log-from"
            labelText={intl.formatMessage({ id: "qa.qms.esigLog.filter.from" })}
            placeholder="yyyy-mm-dd"
          />
          <DatePickerInput
            id="esig-log-to"
            labelText={intl.formatMessage({ id: "qa.qms.esigLog.filter.to" })}
            placeholder="yyyy-mm-dd"
          />
        </DatePicker>
        <Dropdown
          id="esig-log-meaning"
          className="esig-log__filter-dropdown"
          titleText={intl.formatMessage({
            id: "qa.qms.esigLog.filter.action",
          })}
          label={allLabel}
          items={meaningItems}
          itemToString={(item) => item?.label || ""}
          selectedItem={meaningItems.find((i) => i.id === draft.meaning)}
          onChange={({ selectedItem }) =>
            setDraft({ ...draft, meaning: selectedItem?.id || "" })
          }
        />
        <Dropdown
          id="esig-log-record-type"
          className="esig-log__filter-dropdown"
          titleText={intl.formatMessage({
            id: "qa.qms.esigLog.filter.subjectType",
          })}
          label={allLabel}
          items={recordTypeItems}
          itemToString={(item) => item?.label || ""}
          selectedItem={recordTypeItems.find((i) => i.id === draft.recordType)}
          onChange={({ selectedItem }) =>
            setDraft({ ...draft, recordType: selectedItem?.id || "" })
          }
        />
        <Dropdown
          id="esig-log-user"
          className="esig-log__filter-dropdown"
          titleText={intl.formatMessage({ id: "qa.qms.esigLog.filter.user" })}
          label={allLabel}
          items={userItems}
          itemToString={(item) => item?.label || ""}
          selectedItem={userItems.find((i) => i.id === draft.signerId)}
          onChange={({ selectedItem }) =>
            setDraft({ ...draft, signerId: selectedItem?.id || "" })
          }
        />
        <Button
          size="md"
          onClick={applyFilters}
          data-testid="esig-log-apply-filters"
        >
          {intl.formatMessage({ id: "qa.qms.esigLog.filter.apply" })}
        </Button>
        <Button
          kind="ghost"
          size="md"
          onClick={clearFilters}
          data-testid="esig-log-clear-filters"
        >
          {intl.formatMessage({ id: "qa.qms.esigLog.filter.clear" })}
        </Button>
        <Button
          kind="ghost"
          size="md"
          renderIcon={Download}
          onClick={() => handleExport("csv")}
          data-testid="esig-log-export-csv"
        >
          {intl.formatMessage({ id: "qa.qms.esigLog.export.csv" })}
        </Button>
        <Button
          kind="ghost"
          size="md"
          renderIcon={DocumentPdf}
          onClick={() => handleExport("pdf")}
          data-testid="esig-log-export-pdf"
        >
          {intl.formatMessage({ id: "qa.qms.esigLog.export.pdf" })}
        </Button>
      </div>
      <Modal
        open={!!pendingExport}
        size="sm"
        modalHeading={intl.formatMessage({
          id: "qa.qms.esigLog.export.confirm.title",
        })}
        primaryButtonText={intl.formatMessage({
          id: "qa.qms.esigLog.export.confirm.confirm",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "qa.qms.esigLog.export.confirm.cancel",
        })}
        onRequestSubmit={() => {
          openExport(pendingExport);
          setPendingExport(null);
        }}
        onRequestClose={() => setPendingExport(null)}
      >
        <FormattedMessage id="qa.qms.esigLog.export.confirm.body" />
      </Modal>
      {data === undefined ? (
        <DataTableSkeleton columnCount={HEADERS.length} rowCount={5} />
      ) : data === null ? (
        <p className="esig-log__message">
          <FormattedMessage id="qa.qms.esigLog.error" />
        </p>
      ) : rows.length === 0 ? (
        <QAEmptyState
          titleKey="qa.empty.esigLog.title"
          subheadKey="qa.empty.esigLog.subhead"
        />
      ) : (
        <>
          <DataTable
            rows={rows}
            headers={HEADERS.map((h) => ({
              key: h.key,
              header: intl.formatMessage({ id: h.labelKey }),
            }))}
          >
            {({ rows: tableRows, headers, getHeaderProps, getRowProps }) => (
              <TableContainer>
                <Table size="sm">
                  <TableHead>
                    <TableRow>
                      {headers.map((header) => (
                        <TableHeader
                          {...getHeaderProps({ header })}
                          key={header.key}
                        >
                          {header.header}
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {tableRows.map((row) => (
                      <TableRow {...getRowProps({ row })} key={row.id}>
                        {row.cells.map((cell) => (
                          <TableCell key={cell.id}>{cell.value}</TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
          <Pagination
            page={page + 1}
            pageSize={pageSize}
            pageSizes={[25, 50, 100]}
            totalItems={data.totalCount}
            onChange={({ page: newPage, pageSize: newPageSize }) => {
              setPage(newPage - 1);
              setPageSize(newPageSize);
            }}
          />
        </>
      )}
    </div>
  );
};

export default ESignatureLog;
