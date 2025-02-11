import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Heading,
  Loading,
  Grid,
  Column,
  Section,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableBody,
  TableHeader,
  TableCell,
  TableSelectRow,
  TableSelectAll,
  TableContainer,
  Pagination,
  Search,
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils.js";
import { NotificationContext } from "../../layout/Layout.js";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification.js";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb.js";
import ActionPaginationButtonType from "../../common/ActionPaginationButtonType.js";

// Constants
let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "organization.main.title",
    link: "/MasterListsPage#organizationManagement",
  },
];

const TABLE_HEADERS = (intl) => [
  {
    key: "select",
    header: intl.formatMessage({ id: "organization.select" }),
  },
  {
    key: "orgName",
    header: intl.formatMessage({ id: "organization.organizationName" }),
  },
  {
    key: "parentOrg",
    header: intl.formatMessage({ id: "organization.parent" }),
  },
  {
    key: "orgPrefix",
    header: intl.formatMessage({ id: "organization.short.CI" }),
  },
  {
    key: "active",
    header: intl.formatMessage({ id: "organization.isActive" }),
  },
  {
    key: "internetAddress",
    header: intl.formatMessage({ id: "organization.internetaddress" }),
  },
  {
    key: "streetAddress",
    header: intl.formatMessage({ id: "organization.streetAddress" }),
  },
  {
    key: "city",
    header: intl.formatMessage({ id: "organization.city" }),
  },
  {
    key: "cliaNumber",
    header: intl.formatMessage({ id: "organization.clia.number" }),
  },
];

// Custom Hooks
const usePagination = (initialPage = 1, initialPageSize = 10) => {
  const [page, setPage] = useState(initialPage);
  const [pageSize, setPageSize] = useState(initialPageSize);

  const handlePageChange = useCallback(({ page, pageSize }) => {
    setPage(page);
    setPageSize(pageSize);
  }, []);

  return { page, pageSize, handlePageChange };
};

const useOrganizationData = (
  paging,
  startingRecNo,
  panelSearchTerm,
  isSearching,
) => {
  const [organizationData, setOrganizationData] = useState({
    list: [],
    fromRecordCount: 0,
    toRecordCount: 0,
    totalRecordCount: 0,
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const endpoint = isSearching
          ? `/rest/SearchOrganizationMenu?search=Y&startingRecNo=${startingRecNo}&searchString=${panelSearchTerm}`
          : `/rest/OrganizationMenu?paging=${paging}&startingRecNo=${startingRecNo}`;

        const response = await getFromOpenElisServer(endpoint);

        if (!response) throw new Error("No data received");

        const formattedData = response.modelMap.form.menuList.map((item) => ({
          id: item.id,
          orgName: item.organizationName,
          parentOrg: item.organization?.organizationName || "",
          orgPrefix: item.shortName || "",
          active: item.isActive || "",
          internetAddress: item.internetAddress || "",
          streetAddress: item.streetAddress || "",
          city: item.city || "",
          cliaNumber: item.cliaNum || "",
        }));

        setOrganizationData({
          list: formattedData,
          fromRecordCount: response.modelMap.form.fromRecordCount,
          toRecordCount: response.modelMap.form.toRecordCount,
          totalRecordCount: response.modelMap.form.totalRecordCount,
        });
      } catch (err) {
        setError(err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [paging, startingRecNo, panelSearchTerm, isSearching]);

  return { organizationData, loading, error };
};

// Components
const TableCellRenderer = memo(({ cell, row, onSelect, selectedRowIds }) => {
  if (cell.info.header === "select") {
    return (
      <TableSelectRow
        key={cell.id}
        id={cell.id}
        checked={selectedRowIds.includes(row.id)}
        name="selectRowCheckbox"
        ariaLabel={`Select row ${row.id}`}
        onSelect={() => onSelect(row.id)}
      />
    );
  }

  if (cell.info.header === "active") {
    return <TableCell key={cell.id}>{cell.value.toString()}</TableCell>;
  }

  return <TableCell key={cell.id}>{cell.value}</TableCell>;
});

const OrganizationTable = memo(
  ({
    data,
    page,
    pageSize,
    selectedRowIds,
    onRowSelect,
    onSelectAll,
    intl,
  }) => {
    const paginatedData = useMemo(
      () => data.slice((page - 1) * pageSize, page * pageSize),
      [data, page, pageSize],
    );

    return (
      <DataTable rows={paginatedData} headers={TABLE_HEADERS(intl)}>
        {({ rows, headers, getHeaderProps, getTableProps }) => (
          <TableContainer>
            <Table {...getTableProps()}>
              <TableHead>
                <TableRow>
                  <TableSelectAll
                    id="table-select-all"
                    checked={
                      selectedRowIds.length === pageSize &&
                      paginatedData.every((row) =>
                        selectedRowIds.includes(row.id),
                      )
                    }
                    indeterminate={
                      selectedRowIds.length > 0 &&
                      selectedRowIds.length < paginatedData.length
                    }
                    onSelect={onSelectAll}
                  />
                  {headers.map(
                    (header) =>
                      header.key !== "select" && (
                        <TableHeader
                          key={header.key}
                          {...getHeaderProps({ header })}
                        >
                          {header.header}
                        </TableHeader>
                      ),
                  )}
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((row) => (
                  <TableRow key={row.id} onClick={() => onRowSelect(row.id)}>
                    {row.cells.map((cell) => (
                      <TableCellRenderer
                        key={cell.id}
                        cell={cell}
                        row={row}
                        onSelect={onRowSelect}
                        selectedRowIds={selectedRowIds}
                      />
                    ))}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </DataTable>
    );
  },
);

// Main Component
function OrganizationManagement() {
  const intl = useIntl();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  // Pagination state
  const { page, pageSize, handlePageChange } = usePagination();
  const [paging, setPaging] = useState(1);
  const [startingRecNo, setStartingRecNo] = useState(1);

  // Search state
  const [isSearching, setIsSearching] = useState(false);
  const [panelSearchTerm, setPanelSearchTerm] = useState("");

  // Selection state
  const [selectedRowIds, setSelectedRowIds] = useState([]);
  const [deactivateButton, setDeactivateButton] = useState(true);
  const [modifyButton, setModifyButton] = useState(true);

  // Fetch data
  const { organizationData, loading, error } = useOrganizationData(
    paging,
    startingRecNo,
    panelSearchTerm,
    isSearching,
  );

  // Handlers
  const handlePanelSearchChange = useCallback((event) => {
    setIsSearching(true);
    setPaging(1);
    setStartingRecNo(1);
    setPanelSearchTerm(event.target.value);
    setSelectedRowIds([]);
  }, []);

  const handleDeleteDeactivate = useCallback(
    async (event) => {
      event.preventDefault();
      try {
        await postToOpenElisServerJsonResponse(
          `/rest/DeleteOrganization?ID=${selectedRowIds.join(",")}&startingRecNo=1`,
          JSON.stringify({ selectedIDs: selectedRowIds }),
        );

        setNotificationVisible(true);
        addNotification({
          title: intl.formatMessage({ id: "notification.title" }),
          message: intl.formatMessage({
            id: "notification.organization.post.delete.success",
          }),
          kind: NotificationKinds.success,
        });

        setTimeout(() => window.location.reload(), 200);
      } catch (error) {
        addNotification({
          title: intl.formatMessage({ id: "notification.error" }),
          message: error.message,
          kind: NotificationKinds.error,
        });
      }
    },
    [selectedRowIds, intl, setNotificationVisible, addNotification],
  );

  const handleRowSelect = useCallback((id) => {
    setSelectedRowIds((prev) => {
      const newSelection = prev.includes(id)
        ? prev.filter((selectedId) => selectedId !== id)
        : [...prev, id];
      return newSelection;
    });
  }, []);

  const handleSelectAll = useCallback(() => {
    const currentPageIds = organizationData.list
      .slice((page - 1) * pageSize, page * pageSize)
      .map((row) => row.id);

    setSelectedRowIds((prev) =>
      prev.length === currentPageIds.length ? [] : currentPageIds,
    );
  }, [page, pageSize, organizationData.list]);

  // Effects
  useEffect(() => {
    setModifyButton(selectedRowIds.length !== 1);
  }, [selectedRowIds]);

  useEffect(() => {
    setDeactivateButton(selectedRowIds.length === 0);
  }, [selectedRowIds]);

  useEffect(() => {
    if (isSearching && panelSearchTerm === "") {
      setIsSearching(false);
      setPaging(1);
      setStartingRecNo(1);
    }
  }, [isSearching, panelSearchTerm]);

  if (!loading) {
    return (
      <>
        <Loading />
      </>
    );
  }
  if (error) {
    return (
      <>
        <ErrorMessage error={error} />
      </>
    );
  }

  return (
    <>
      {notificationVisible && <AlertDialog />}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />

        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                <FormattedMessage id="organization.main.title" />
              </Heading>
            </Section>
          </Column>
        </Grid>

        <ActionPaginationButtonType
          selectedRowIds={selectedRowIds}
          modifyButton={modifyButton}
          deactivateButton={deactivateButton}
          fromRecordCount={organizationData.fromRecordCount}
          toRecordCount={organizationData.toRecordCount}
          totalRecordCount={organizationData.totalRecordCount}
          handlePreviousPage={() => {
            setPaging((p) => Math.max(p - 1, 1));
            setStartingRecNo(Math.max(organizationData.fromRecordCount, 1));
          }}
          handleNextPage={() => {
            setPaging((p) => Math.max(p, 2));
            setStartingRecNo(organizationData.fromRecordCount);
          }}
          deleteDeactivate={handleDeleteDeactivate}
          id={selectedRowIds[0]}
          otherParmsInLink="&startingRecNo=1"
          addButtonRedirectLink="/MasterListsPage#organizationEdit?ID=0"
          modifyButtonRedirectLink="/MasterListsPage#organizationEdit?ID="
          type="type2"
        />

        <div className="orderLegendBody">
          <Grid>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Search
                  size="lg"
                  id="org-name-search-bar"
                  labelText={
                    <FormattedMessage id="organization.search.byorgname" />
                  }
                  placeholder={intl.formatMessage({
                    id: "organization.search.placeHolder",
                  })}
                  onChange={handlePanelSearchChange}
                  value={panelSearchTerm}
                />
              </Section>
            </Column>
          </Grid>

          <Grid fullWidth className="gridBoundary">
            <Column lg={16} md={8} sm={4}>
              <OrganizationTable
                data={organizationData.list}
                page={page}
                pageSize={pageSize}
                selectedRowIds={selectedRowIds}
                onRowSelect={handleRowSelect}
                onSelectAll={handleSelectAll}
                intl={intl}
              />

              <Pagination
                onChange={handlePageChange}
                page={page}
                pageSize={pageSize}
                pageSizes={[10, 20]}
                totalItems={organizationData.list.length}
                forwardText={intl.formatMessage({ id: "pagination.forward" })}
                backwardText={intl.formatMessage({ id: "pagination.backward" })}
                itemRangeText={(min, max, total) =>
                  intl.formatMessage(
                    { id: "pagination.item-range" },
                    { min, max, total },
                  )
                }
                itemsPerPageText={intl.formatMessage({
                  id: "pagination.items-per-page",
                })}
                itemText={(min, max) =>
                  intl.formatMessage({ id: "pagination.item" }, { min, max })
                }
                pageNumberText={intl.formatMessage({
                  id: "pagination.page-number",
                })}
                pageRangeText={(_current, total) =>
                  intl.formatMessage({ id: "pagination.page-range" }, { total })
                }
                pageText={(page, pagesUnknown) =>
                  intl.formatMessage(
                    { id: "pagination.page" },
                    { page: pagesUnknown ? "" : page },
                  )
                }
              />
            </Column>
          </Grid>
        </div>
      </div>
    </>
  );
}

export default injectIntl(OrganizationManagement);
