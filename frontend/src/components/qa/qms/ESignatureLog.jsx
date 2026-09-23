import React, { useCallback, useEffect, useState } from "react";
import {
  Button,
  DataTableSkeleton,
  DatePicker,
  DatePickerInput,
  Dropdown,
  Modal,
  Pagination,
} from "@carbon/react";
import { Download, DocumentPdf } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../../config.json";
import {
  getFromOpenElisServer,
  toLocalIsoDate,
  toLocalIsoDateTime,
} from "../../utils/Utils";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import QASimpleTable from "../common/QASimpleTable";
import QAEmptyState from "../common/QAEmptyState";
import { lastDays } from "../common/qaDates";
import "../common/QAStyles.css";

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
  { key: "signedAt", labelKey: "qc.signature.signedAt" },
  { key: "signer", labelKey: "qa.qms.esigLog.column.signer" },
  { key: "action", labelKey: "common.action" },
  { key: "subject", labelKey: "qa.qms.esigLog.column.subject" },
  { key: "reason", labelKey: "storage.audit.reason" },
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

function defaultFilters() {
  return {
    ...lastDays(30),
    signerId: "",
    meaning: "",
    recordType: "",
  };
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

  const allLabel = intl.formatMessage({ id: "common.all" });

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
        fromDate: toLocalIsoDate(dates[0]),
        toDate: toLocalIsoDate(dates[1]),
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
    signedAt: toLocalIsoDateTime(item.signedAt),
    signer: item.signerNamePrinted || "—",
    action: item.signatureMeaning ? meaningLabel(item.signatureMeaning) : "—",
    subject: item.recordType ? `${item.recordType} #${item.recordId}` : "—",
    reason: item.rejectionReason || "—",
  }));

  return (
    <div className="pageContent">
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <h2>
        <FormattedMessage id="sideNav.label.qa.qms.esigLog" />
      </h2>
      <p className="qi-dashboard__subtitle">
        <FormattedMessage id="qa.qms.esigLog.subtitle" />
      </p>
      <div className="qi-dashboard__controls" data-testid="esig-log-filters">
        <DatePicker
          datePickerType="range"
          dateFormat="Y-m-d"
          value={[draft.fromDate, draft.toDate]}
          onChange={handleDates}
        >
          <DatePickerInput
            id="esig-log-from"
            labelText={intl.formatMessage({ id: "reports.tat.dateRangeFrom" })}
            placeholder="yyyy-mm-dd"
          />
          <DatePickerInput
            id="esig-log-to"
            labelText={intl.formatMessage({ id: "reports.tat.dateRangeTo" })}
            placeholder="yyyy-mm-dd"
          />
        </DatePicker>
        <Dropdown
          id="esig-log-meaning"
          className="qi-dashboard__filter"
          titleText={intl.formatMessage({
            id: "common.action",
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
          className="qi-dashboard__filter"
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
          className="qi-dashboard__filter"
          titleText={intl.formatMessage({ id: "common.user" })}
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
          {intl.formatMessage({ id: "label.clear" })}
        </Button>
        <Button
          kind="ghost"
          size="md"
          renderIcon={Download}
          onClick={() => handleExport("csv")}
          data-testid="esig-log-export-csv"
        >
          {intl.formatMessage({ id: "reports.tat.exportCsv" })}
        </Button>
        <Button
          kind="ghost"
          size="md"
          renderIcon={DocumentPdf}
          onClick={() => handleExport("pdf")}
          data-testid="esig-log-export-pdf"
        >
          {intl.formatMessage({ id: "common.exportPdf" })}
        </Button>
      </div>
      <Modal
        open={!!pendingExport}
        size="sm"
        modalHeading={intl.formatMessage({
          id: "qa.qms.esigLog.export.confirm.title",
        })}
        primaryButtonText={intl.formatMessage({
          id: "reports.export",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
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
        <p className="qi-tile__message">
          <FormattedMessage id="qa.qms.esigLog.error" />
        </p>
      ) : rows.length === 0 ? (
        <QAEmptyState
          titleKey="qa.empty.esigLog.title"
          subheadKey="qa.empty.esigLog.subhead"
        />
      ) : (
        <>
          <QASimpleTable rows={rows} headers={HEADERS} />
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
