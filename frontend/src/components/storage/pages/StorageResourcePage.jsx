import React, { useContext, useMemo, useState } from "react";
import { useHistory, useLocation } from "react-router-dom";
import {
  Button,
  DataTable,
  OverflowMenu,
  OverflowMenuItem,
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
  Loading,
} from "@carbon/react";
import { Add } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import AddLocationModal from "../components/AddLocationModal";
import EditLocationModal from "../components/EditLocationModal";
import DeleteLocationConfirmModal from "../components/DeleteLocationConfirmModal";
import useStorageTableData from "../hooks/useStorageTableData";
import UserSessionDetailsContext from "../../../UserSessionDetailsContext";
import { NotificationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";
import { storageLevel } from "../storageLevels";
import { hasRole, Roles } from "../../utils/Utils";

/**
 * StorageResourcePage — shared shell for the five per-resource storage
 * pages (Rooms, Devices, Shelves, Racks, Boxes).
 *
 * Each concrete page is a thin wrapper that passes the right config:
 *   - level: storageLevels key, which drives the Add, Edit and Delete modals
 *   - listUrl: backend endpoint (e.g. /rest/storage/rooms)
 */
export default function StorageResourcePage({
  level,
  listUrl,
  searchUrl,
  headers,
  mapRow,
  page,
  setPage,
  pageSize,
  setPageSize,
  searchPlaceholderId,
}) {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const isGlobalAdmin = hasRole(userSessionDetails, Roles.GLOBAL_ADMIN);
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [searchTerm, setSearchTerm] = useState("");
  const [addOpen, setAddOpen] = useState(false);
  const [editTarget, setEditTarget] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);

  const levelMeta = storageLevel(level);

  // Name the level being acted on — "Rack created", not a generic
  // "Storage location created" that reads identically for all five.
  const notify = (messageId, defaultMessage) => {
    setNotificationVisible(true);
    addNotification({
      kind: NotificationKinds.success,
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage(
        { id: messageId, defaultMessage },
        {
          level: intl.formatMessage({
            id: levelMeta.labelId,
            defaultMessage: levelMeta.label,
          }),
        },
      ),
    });
  };

  const refreshAfterWrite = () =>
    history.replace({
      pathname: location.pathname,
      search: `?t=${Date.now()}`,
    });

  const refreshKey = useMemo(
    () => new URLSearchParams(location.search).get("t") || "initial",
    [location.search],
  );

  const { items, totalItems, loading } = useStorageTableData({
    listUrl,
    searchUrl,
    searchTerm,
    refreshKey,
  });

  // The level listings return every row and take no page or size parameter, so
  // the page is cut here. Passing them to the fetch would only refire it.
  // Highest id first: a row just added lands at the top of the first page
  // rather than at the end of the last one, and the order stops depending on
  // the order the endpoint happened to return, which an update can change.
  const ordered = useMemo(() => {
    const newestFirst = [...(items || [])];
    newestFirst.sort((a, b) => (Number(b.id) || 0) - (Number(a.id) || 0));
    return newestFirst;
  }, [items]);

  const paginated = useMemo(
    () => ordered.slice((page - 1) * pageSize, page * pageSize),
    [ordered, page, pageSize],
  );

  const rows = useMemo(() => {
    const mapped = paginated.map(mapRow);
    return mapped.map((row, idx) => {
      const rawItem = paginated[idx];
      if (!isGlobalAdmin) return row;
      return {
        ...row,
        menuActions: (
          <OverflowMenu
            size="sm"
            flipped
            aria-label="Row actions"
            data-testid={`storage-row-actions-${rawItem?.id}`}
          >
            <OverflowMenuItem
              itemText={
                <FormattedMessage id="label.edit" defaultMessage="Edit" />
              }
              onClick={() => setEditTarget(rawItem)}
            />
            <OverflowMenuItem
              isDelete
              itemText={
                <FormattedMessage id="label.delete" defaultMessage="Delete" />
              }
              onClick={() => setDeleteTarget(rawItem)}
            />
          </OverflowMenu>
        ),
      };
    });
  }, [paginated, mapRow, isGlobalAdmin]);

  // Edit and Delete both open admin-only modals; without either the column
  // would be an empty cell on every row.
  const effectiveHeaders = useMemo(
    () =>
      isGlobalAdmin
        ? [...headers, { key: "menuActions", header: "" }]
        : headers,
    [headers, isGlobalAdmin],
  );

  return (
    <div className="storage-resource-page">
      {searchUrl && (
        <div
          className="storage-resource-page-toolbar"
          style={{ margin: "1rem 0" }}
        >
          <Search
            id="storage-resource-search"
            size="md"
            labelText={intl.formatMessage({
              id: searchPlaceholderId || "label.search",
              defaultMessage: "Search",
            })}
            placeholder={intl.formatMessage({
              id: searchPlaceholderId || "label.search",
              defaultMessage: "Search",
            })}
            value={searchTerm}
            onChange={(e) => {
              setSearchTerm(e.target.value);
              setPage(1);
            }}
          />
        </div>
      )}

      {isGlobalAdmin && (
        <div style={{ margin: "1rem 0" }}>
          <Button
            kind="primary"
            renderIcon={Add}
            onClick={() => setAddOpen(true)}
          >
            <FormattedMessage id="label.add" defaultMessage="Add" />
          </Button>
        </div>
      )}

      {loading && <Loading small withOverlay={false} />}

      <DataTable rows={rows} headers={effectiveHeaders} isSortable>
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

      <AddLocationModal
        level={level}
        open={addOpen}
        onClose={() => setAddOpen(false)}
        onCreated={() => {
          setAddOpen(false);
          notify("storage.location.created", "{level} created");
          // The new row sorts to the top, so the first page is where it shows.
          setPage(1);
          refreshAfterWrite();
        }}
      />
      <EditLocationModal
        level={level}
        id={editTarget?.id}
        open={Boolean(editTarget)}
        onClose={() => setEditTarget(null)}
        onUpdated={() => {
          setEditTarget(null);
          notify("storage.location.updated", "{level} updated");
          refreshAfterWrite();
        }}
      />
      <DeleteLocationConfirmModal
        isOpen={Boolean(deleteTarget)}
        type={level}
        location={deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onDeleted={() => {
          setDeleteTarget(null);
          notify("storage.location.deleted", "{level} deleted");
          // The deleted row can be the only one on the last page, which would
          // leave the user on a page the shortened listing no longer reaches.
          setPage(1);
          refreshAfterWrite();
        }}
      />
    </div>
  );
}

/**
 * Renders the Active/Inactive Tag consistently across pages. Kept
 * here (not exported as a separate component) because only the per-
 * resource pages use it; no point widening surface area.
 */
export function ActiveTag({ active }) {
  return active ? (
    <Tag type="green">
      <FormattedMessage id="label.active" defaultMessage="Active" />
    </Tag>
  ) : (
    <Tag type="gray">
      <FormattedMessage id="label.inactive" defaultMessage="Inactive" />
    </Tag>
  );
}
