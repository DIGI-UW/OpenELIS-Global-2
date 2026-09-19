import React, { useContext, useMemo, useState } from "react";
import { Link, useHistory, useLocation } from "react-router-dom";
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
import BreadcrumbNav from "../components/BreadcrumbNav";
import AddLocationModal from "../components/AddLocationModal";
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
 *   - level: storageLevels key, which drives the Add and Delete modals
 *   - listUrl: backend endpoint (e.g. /rest/storage/rooms)
 *   - editHref?: builder `(row) => "/Storage/.../edit"` to render a
 *     per-row Edit link
 */
export default function StorageResourcePage({
  level,
  crumbs,
  heading,
  listUrl,
  searchUrl,
  headers,
  mapRow,
  page,
  setPage,
  pageSize,
  setPageSize,
  editHref,
  searchPlaceholderId,
  // Rendered inside the Storage Management dashboard tab, where the container
  // already supplies the breadcrumb and heading.
  embedded = false,
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
    page,
    pageSize,
    refreshKey,
  });

  const rows = useMemo(() => {
    const mapped = (items || []).map(mapRow);
    return mapped.map((row, idx) => {
      const rawItem = items[idx];
      const nextRow = { ...row };
      if (editHref) {
        nextRow.actions = (
          <Link to={editHref(rawItem)}>
            <FormattedMessage id="label.edit" defaultMessage="Edit" />
          </Link>
        );
      }
      if (editHref || isGlobalAdmin) {
        nextRow.menuActions = (
          <OverflowMenu
            size="sm"
            ariaLabel="Row actions"
            data-testid={`storage-row-actions-${rawItem?.id}`}
          >
            {editHref && (
              <OverflowMenuItem
                itemText={
                  <FormattedMessage id="label.edit" defaultMessage="Edit" />
                }
                onClick={() => history.push(editHref(rawItem))}
              />
            )}
            {isGlobalAdmin && (
              <OverflowMenuItem
                isDelete
                itemText={
                  <FormattedMessage id="label.delete" defaultMessage="Delete" />
                }
                onClick={() => setDeleteTarget(rawItem)}
              />
            )}
          </OverflowMenu>
        );
      }
      return {
        ...nextRow,
      };
    });
  }, [items, mapRow, editHref, history, isGlobalAdmin]);

  const effectiveHeaders = useMemo(() => {
    const nextHeaders = [...headers];
    if (editHref) {
      nextHeaders.push({ key: "actions", header: "" });
    }
    if (editHref || isGlobalAdmin) {
      nextHeaders.push({ key: "menuActions", header: "" });
    }
    return nextHeaders;
  }, [headers, editHref, isGlobalAdmin]);

  return (
    <div
      className={
        embedded ? "storage-resource-page" : "storage-resource-page pageContent"
      }
    >
      {!embedded && (
        <>
          <BreadcrumbNav crumbs={crumbs} />
          <h1>{heading}</h1>
        </>
      )}

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
          pageSizes={[25, 50, 100]}
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
