import React, { useMemo, useState } from "react";
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
import useStorageTableData from "../hooks/useStorageTableData";

/**
 * InventoryLotsPage — /Storage/inventory-lots.
 *
 * The lot counterpart of SampleItemsPage. Storage Management could show
 * which samples occupied a location but never which inventory lots did,
 * even though both share sample_storage_assignment and both count toward
 * occupancy.
 */
export default function InventoryLotsPage({ embedded = false }) {
  const intl = useIntl();
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);
  const [searchTerm, setSearchTerm] = useState("");

  const { items, totalItems, loading } = useStorageTableData({
    listUrl: "/rest/storage/inventory-lots",
    page,
    pageSize,
    searchTerm,
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
        id: "storage.breadcrumb.inventorylots",
        defaultMessage: "Inventory Lots",
      }),
      href: "/Storage/inventory-lots",
    },
  ];

  const headers = [
    {
      key: "lotNumber",
      header: intl.formatMessage({ id: "lot.number", defaultMessage: "Lot" }),
    },
    {
      key: "barcode",
      header: intl.formatMessage({
        id: "lot.barcode",
        defaultMessage: "Barcode",
      }),
    },
    {
      key: "itemName",
      header: intl.formatMessage({
        id: "catalog.item.name",
        defaultMessage: "Item",
      }),
    },
    {
      key: "quantity",
      header: intl.formatMessage({
        id: "lot.currentQuantity",
        defaultMessage: "Quantity",
      }),
    },
    {
      key: "status",
      header: intl.formatMessage({
        id: "lot.status",
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
  ];

  // The client filters because the listing endpoint has no search twin;
  // lot counts are small enough that a round trip per keystroke would cost
  // more than it saves.
  const filtered = useMemo(() => {
    const term = (searchTerm || "").trim().toLowerCase();
    if (!term) return items;
    return items.filter((lot) =>
      [lot.lotNumber, lot.barcode, lot.itemName, lot.location].some(
        (field) => field && String(field).toLowerCase().includes(term),
      ),
    );
  }, [items, searchTerm]);

  const paginated = useMemo(
    () => filtered.slice((page - 1) * pageSize, page * pageSize),
    [filtered, page, pageSize],
  );

  const rows = useMemo(
    () =>
      paginated.map((lot) => {
        const locationPath = lot.location || "";
        return {
          id: String(lot.id),
          lotNumber: lot.lotNumber || "",
          barcode: lot.barcode || "",
          itemName: lot.itemName || "",
          quantity: lot.quantity ?? "",
          status: lot.status || "",
          location: locationPath ? (
            <Tag type="blue">
              {lot.positionCoordinate
                ? `${locationPath} · ${lot.positionCoordinate}`
                : locationPath}
            </Tag>
          ) : (
            <Tag type="gray">
              {intl.formatMessage({
                id: "storage.location.notAssigned",
                defaultMessage: "Not assigned",
              })}
            </Tag>
          ),
        };
      }),
    [paginated, intl],
  );

  return (
    <div
      className={
        embedded
          ? "storage-inventory-lots-page"
          : "storage-inventory-lots-page pageContent"
      }
    >
      {!embedded && (
        <>
          <BreadcrumbNav crumbs={crumbs} />
          <h1>
            <FormattedMessage
              id="storage.tab.inventoryLots"
              defaultMessage="Inventory Lots"
            />
          </h1>
        </>
      )}

      <div
        className="storage-inventory-lots-page-toolbar"
        style={{ margin: "1rem 0" }}
      >
        <Search
          id="storage-inventory-lots-search"
          size="md"
          placeHolderText={intl.formatMessage({
            id: "storage.search.lots.placeholder",
            defaultMessage: "Search lots…",
          })}
          labelText={intl.formatMessage({
            id: "storage.search.lots.placeholder",
            defaultMessage: "Search lots",
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
          data-testid="inventory-lots-pagination"
          page={page}
          pageSize={pageSize}
          pageSizes={[10, 25, 50, 100]}
          totalItems={filtered.length || totalItems}
          onChange={({ page: nextPage, pageSize: nextSize }) => {
            setPage(nextPage);
            setPageSize(nextSize);
          }}
        />
      )}
    </div>
  );
}
