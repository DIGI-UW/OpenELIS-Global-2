import React, { useContext, useMemo, useState } from "react";
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
import SampleActionsContainer from "../SampleStorage/SampleActionsContainer";
import DisposeSampleModal from "../SampleStorage/DisposeSampleModal";
import ViewAuditModal from "../SampleStorage/ViewAuditModal";
import LocationPickerModal from "../LocationPicker/LocationPickerModal";
import { LEVEL_ORDER } from "../LocationPicker/useLocationPicker";
import {
  getDeepestLocationSelection,
  positionToCoordinate,
} from "../LocationPicker/locationSelectionMapper";
import useSampleStorage from "../hooks/useSampleStorage";
import useStorageTableData from "../hooks/useStorageTableData";
import { NotificationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";
import { postToOpenElisServerJsonResponse } from "../../utils/Utils";

/**
 * SampleItemsPage — /Storage/sample-items.
 *
 * Search + paginated DataTable of sample items.
 * Per-row overflow menu opens the shared LocationPickerModal, the same
 * picker the results and inventory surfaces use.
 */
export default function SampleItemsPage() {
  const history = useHistory();
  const location = useLocation();
  const intl = useIntl();
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);
  const [searchTerm, setSearchTerm] = useState("");
  const [disposeTarget, setDisposeTarget] = useState(null);
  const [auditTarget, setAuditTarget] = useState(null);
  const [locationTarget, setLocationTarget] = useState(null);
  const { assignSampleItem, moveSampleItem } = useSampleStorage();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  // URL-driven refresh: `refreshList` stamps a `?t=<timestamp>` query,
  // which changes this and triggers a refetch.
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

  // The list endpoint is paged by the server, but the search endpoint returns
  // every match and takes no page or size, so a search is cut to a page here.
  const searching = (searchTerm || "").trim().length > 0;
  const visibleItems = useMemo(() => {
    if (!searching) return items || [];
    return (items || []).slice((page - 1) * pageSize, page * pageSize);
  }, [items, searching, page, pageSize]);

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
    setLocationTarget(sample);
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

  const notifyError = (message) => {
    setNotificationVisible(true);
    addNotification({
      kind: NotificationKinds.error,
      title: intl.formatMessage({ id: "notification.error" }),
      message,
    });
  };

  // A sample that already sits somewhere is a move: the modal then shows
  // the current location and asks for a reason, and the save posts to
  // /move rather than /assign.
  const locationTargetCurrent = useMemo(() => {
    if (!locationTarget) return null;
    const hasAnyLevel = LEVEL_ORDER.some((lvl) => locationTarget[`${lvl}Id`]);
    const locationPath =
      locationTarget.location || locationTarget.hierarchicalPath || "";
    if (!hasAnyLevel && !locationPath) return null;
    const selection = {};
    LEVEL_ORDER.forEach((lvl) => {
      if (locationTarget[`${lvl}Id`]) {
        selection[lvl] = {
          id: locationTarget[`${lvl}Id`],
          name: locationTarget[`${lvl}Name`] || "",
        };
      }
    });
    return {
      selection,
      hierarchicalPath: locationPath,
      position: locationTarget.positionCoordinate
        ? { mode: "text", value: locationTarget.positionCoordinate }
        : null,
    };
  }, [locationTarget]);

  const handleLocationConfirm = async ({
    selection,
    position,
    reason,
    notes,
  }) => {
    if (!locationTarget) return;
    const deepest = getDeepestLocationSelection(selection, {
      requireAssignable: true,
    });
    if (!deepest) {
      notifyError(
        intl.formatMessage({
          id: "storage.manageLocation.error.selectTarget",
          defaultMessage: "Select a storage location before saving",
        }),
      );
      return;
    }

    const payload = {
      sampleItemId: locationTarget.sampleItemId || locationTarget.id,
      locationId: String(deepest.value.id),
      locationType: deepest.type,
      positionCoordinate: positionToCoordinate(position, {
        emptyValue: null,
      }),
      notes: notes || null,
    };

    try {
      if (locationTargetCurrent) {
        await moveSampleItem({ ...payload, reason: reason || null });
      } else {
        await assignSampleItem(payload);
      }
      setLocationTarget(null);
      refreshList();
    } catch (e) {
      notifyError(
        e.message ||
          intl.formatMessage({
            id: "storage.manageLocation.error.saveFailed",
            defaultMessage: "Save failed",
          }),
      );
    }
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
          return;
        }
        // Without this the modal stays open and silent, so Confirm reads as a
        // click that did nothing rather than as a disposal that was refused.
        notifyError(
          response?.message ||
            response?.error ||
            intl.formatMessage({
              id: "storage.dispose.error",
              defaultMessage: "Disposal failed",
            }),
        );
      },
    );
  };

  const rows = useMemo(() => {
    return visibleItems.map((it) => {
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
  }, [visibleItems]);

  return (
    <div className="storage-sample-items-page">
      <div
        className="storage-sample-items-page-toolbar"
        style={{ margin: "1rem 0" }}
      >
        <Search
          id="storage-sample-items-search"
          size="md"
          placeholder={intl.formatMessage({
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
          pageSizes={[5, 25, 50, 100]}
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

      <LocationPickerModal
        isOpen={Boolean(locationTarget)}
        occupantType="SAMPLE_ITEM"
        occupant={{
          label:
            locationTarget?.sampleAccessionNumber ||
            locationTarget?.sampleItemId ||
            "",
          type: locationTarget?.type || "",
          status: locationTarget?.status || "Active",
        }}
        currentLocation={locationTargetCurrent}
        onConfirm={handleLocationConfirm}
        onCancel={() => setLocationTarget(null)}
      />
    </div>
  );
}
