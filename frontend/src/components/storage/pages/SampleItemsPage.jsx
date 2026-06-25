import React, { useMemo, useState } from "react";
import { useHistory, useLocation } from "react-router-dom";
import {
  DataTable,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Pagination,
  Search,
  Tag,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import BreadcrumbNav from "../components/BreadcrumbNav";
import SampleActionsContainer from "../SampleStorage/SampleActionsContainer";
import DisposeSampleModal from "../SampleStorage/DisposeSampleModal";
import ViewAuditModal from "../SampleStorage/ViewAuditModal";
import useStorageTableData from "../hooks/useStorageTableData";
import { postToOpenElisServerJsonResponse } from "../../utils/Utils";

/**
 * SampleItemsPage — /Storage/sample-items.
 *
 * Breadcrumb + h1 + search + paginated DataTable of sample items.
 * Per-row overflow menu navigates to
 * /Storage/sample-items/:id/manage-location.
 */
export default function SampleItemsPage() {
  const history = useHistory();
  const location = useLocation();
  const intl = useIntl();
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);
  const [searchTerm, setSearchTerm] = useState("");
  const [disposeTarget, setDisposeTarget] = useState(null);
  const [auditTarget, setAuditTarget] = useState(null);

  // URL-driven refresh: when the Manage Location page navigates back with
  // a `?t=<timestamp>` query, this changes and triggers a refetch.
  const refreshKey = useMemo(
    () => new URLSearchParams(location.search).get("t") || "initial",
    [location.search],
  );

  const { items, totalItems, loading } = useStorageTableData({
    listUrl: "/rest/storage/sample-items",
    searchUrl: "/rest/storage/sample-items/search",
    page,
    pageSize,
    searchTerm,
    refreshKey,
  });

  const crumbs = [
    {
      label: intl.formatMessage({
        id: "storage.breadcrumb.storage",
        defaultMessage: "Storage",
      }),
      href: "/Storage",
    },
    {
      label: intl.formatMessage({
        id: "storage.breadcrumb.sampleitems",
        defaultMessage: "Sample Items",
      }),
      href: "/Storage/sample-items",
    },
  ];

  const headers = [
    {
      key: "sampleItemId",
      header: intl.formatMessage({
        id: "storage.sampleitem.id",
        defaultMessage: "SampleItem ID",
      }),
    },
    {
      key: "sampleAccessionNumber",
      header: intl.formatMessage({
        id: "storage.sampleitem.accession",
        defaultMessage: "Sample Accession",
      }),
    },
    {
      key: "type",
      header: intl.formatMessage({
        id: "storage.sampleitem.type",
        defaultMessage: "Type",
      }),
    },
    {
      key: "status",
      header: intl.formatMessage({
        id: "storage.sampleitem.status",
        defaultMessage: "Status",
      }),
    },
    {
      key: "location",
      header: intl.formatMessage({
        id: "storage.sampleitem.location",
        defaultMessage: "Storage location",
      }),
    },
    { key: "actions", header: "" },
  ];

  const handleManageLocation = (sample) => {
    const id = sample.sampleItemId || sample.id;
    // Pass the full sample row via router state so ManageLocationPage
    // doesn't need a separate GET — the list already has everything.
    history.push({
      pathname: `/Storage/sample-items/${id}/manage-location`,
      state: { sample },
    });
  };

  const refreshList = () => {
    history.replace({
      pathname: location.pathname,
      search: `?t=${Date.now()}`,
    });
  };

  const handleDispose = (sample) => {
    setDisposeTarget(sample);
  };

  const handleViewAudit = (sample) => {
    setAuditTarget(sample);
  };

  const handleConfirmDispose = ({ sample, reason, method, notes }) => {
    const payload = {
      sampleItemId: String(
        sample?.sampleItemExternalId || sample?.sampleItemId || "",
      ),
      reason,
      method,
      notes: notes || null,
    };
    postToOpenElisServerJsonResponse(
      "/rest/storage/sample-items/dispose",
      JSON.stringify(payload),
      (response) => {
        if (response && !response.error && !response.statusCode) {
          setDisposeTarget(null);
          refreshList();
        }
      },
    );
  };

  const rows = useMemo(() => {
    if (!items) return [];
    return items.map((it) => {
      const sampleItemId = String(it.sampleItemId || it.id || "");
      const externalId = it.sampleItemExternalId || null;
      const displayId = externalId || sampleItemId;
      const isDisposed = it.status === "disposed" || it.status === "Disposed";
      const isStorageSkipped = it.storageSkipped === true;
      const locationPath =
        isStorageSkipped && !it.location && !it.hierarchicalPath
          ? intl.formatMessage({
              id: "storage.location.skipped",
              defaultMessage: "Storage Skipped",
            })
          : it.location || it.hierarchicalPath || "";
      return {
        id: sampleItemId,
        sampleItemId: displayId,
        sampleAccessionNumber: it.sampleAccessionNumber || "",
        type: it.type || it.sampleType || "",
        status: (
          <Tag type={isDisposed ? "red" : "green"}>
            {isDisposed ? (
              <FormattedMessage
                id="storage.status.disposed"
                defaultMessage="Disposed"
              />
            ) : (
              <FormattedMessage id="label.active" defaultMessage="Active" />
            )}
          </Tag>
        ),
        location: locationPath,
        actions: (
          <SampleActionsContainer
            sample={{
              id: sampleItemId,
              sampleId: sampleItemId,
              sampleItemId,
              sampleItemExternalId: externalId,
              sampleAccessionNumber: it.sampleAccessionNumber || "",
              type: it.type || it.sampleType || "",
              status: it.status || "Active",
              location: locationPath,
              positionCoordinate: it.positionCoordinate || "",
              notes: it.notes || "",
            }}
            onManageLocation={handleManageLocation}
            onDispose={handleDispose}
            onViewAudit={handleViewAudit}
          />
        ),
      };
    });
  }, [items]);

  return (
    <div className="storage-sample-items-page pageContent">
      <BreadcrumbNav crumbs={crumbs} />
      <h1>
        <FormattedMessage
          id="storage.tab.samples"
          defaultMessage="Sample Items"
        />
      </h1>

      <div
        className="storage-sample-items-page-toolbar"
        style={{ margin: "1rem 0" }}
      >
        <Search
          id="storage-sample-items-search"
          size="md"
          placeHolderText={intl.formatMessage({
            id: "storage.search.samples.placeholder",
            defaultMessage: "Search sample items…",
          })}
          labelText={intl.formatMessage({
            id: "storage.search.samples.placeholder",
            defaultMessage: "Search sample items",
          })}
          value={searchTerm}
          onChange={(e) => {
            setSearchTerm(e.target.value);
            setPage(1);
          }}
        />
      </div>

      <DataTable rows={rows} headers={headers} isSortable>
        {({
          rows: r,
          headers: h,
          getTableProps,
          getHeaderProps,
          getRowProps,
        }) => (
          <TableContainer>
            <Table {...getTableProps()}>
              <TableHead>
                <TableRow>
                  {h.map((header) => (
                    <TableHeader
                      key={header.key}
                      {...getHeaderProps({ header })}
                    >
                      {header.header}
                    </TableHeader>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {r.map((row) => (
                  <TableRow key={row.id} {...getRowProps({ row })}>
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

      {!loading && (
        <Pagination
          data-testid="sample-items-pagination"
          page={page}
          pageSize={pageSize}
          pageSizes={[25, 50, 100]}
          totalItems={totalItems}
          onChange={({ page: p, pageSize: s }) => {
            setPage(p);
            setPageSize(s);
          }}
        />
      )}

      <DisposeSampleModal
        open={Boolean(disposeTarget)}
        sample={disposeTarget}
        currentLocation={
          disposeTarget
            ? {
                path:
                  disposeTarget.location ||
                  intl.formatMessage({
                    id: "storage.location.unassigned",
                    defaultMessage: "Unassigned",
                  }),
              }
            : null
        }
        onClose={() => setDisposeTarget(null)}
        onConfirm={handleConfirmDispose}
      />

      <ViewAuditModal
        open={Boolean(auditTarget)}
        sample={auditTarget}
        onClose={() => setAuditTarget(null)}
      />
    </div>
  );
}
