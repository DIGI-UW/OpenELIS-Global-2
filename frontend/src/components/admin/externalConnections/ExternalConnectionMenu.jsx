import React, { useContext, useState, useEffect } from "react";
import {
  DEFAULT_SERVER_PAGE_SIZE,
  serverPageSizeFrom,
  startingRecNoFor,
} from "../../utils/offsetPaging";
import {
  Heading,
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
  TableContainer,
  Pagination,
  Search,
} from "@carbon/react";
import { postToOpenElisServer } from "../../utils/Utils";
import {
  useInvalidateServerData,
  useServerData,
} from "../../utils/useServerData";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import ActionPaginationButtonType from "../../common/ActionPaginationButtonType";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "externalconnections.browse.title",
    link: "/MasterListsPage/externalConnections",
  },
];

function ExternalConnectionMenu() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const [page, setPage] = useState(1);
  const [deactivateButton, setDeactivateButton] = useState(true);
  const [modifyButton, setModifyButton] = useState(true);
  const [selectedRowIds, setSelectedRowIds] = useState([]);
  const [isSearching, setIsSearching] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [totalRecordCount, setTotalRecordCount] = useState("");
  const [fromRecordCount, setFromRecordCount] = useState("");
  const [toRecordCount, setToRecordCount] = useState("");
  const [serverPageSize, setServerPageSize] = useState(
    DEFAULT_SERVER_PAGE_SIZE,
  );
  const startingRecNo = startingRecNoFor(page, serverPageSize);
  const [connectionListShow, setConnectionListShow] = useState([]);

  function deactivateConnection(event) {
    event.preventDefault();
    postToOpenElisServer(
      `/rest/DeactivateExternalConnection?ID=${selectedRowIds.join(",")}`,
      JSON.stringify({ selectedIDs: selectedRowIds }),
      deactivateCallback,
    );
  }

  const deactivateCallback = (status) => {
    const succeeded = status >= 200 && status < 300;
    setNotificationVisible(true);
    addNotification({
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage({
        id: succeeded
          ? "externalconnections.deactivate.success"
          : "server.error.msg",
      }),
      kind: succeeded ? NotificationKinds.success : NotificationKinds.error,
    });
    if (!succeeded) return;
    setSelectedRowIds([]);
    invalidateServerData();
  };

  const handleNextPage = () => {
    setPage((current) => current + 1);
    setSelectedRowIds([]);
  };

  const handlePreviousPage = () => {
    setPage((current) => Math.max(current - 1, 1));
    setSelectedRowIds([]);
  };

  const handleSearchChange = (event) => {
    setIsSearching(true);
    setPage(1);
    const query = event.target.value;
    setSearchTerm(query);
    setSelectedRowIds([]);
  };

  const handlePageChange = ({ page: newPage }) => {
    if (newPage !== page) {
      setPage(newPage);
      setSelectedRowIds([]);
    }
  };

  // Browsing and searching are the same list from two endpoints, so which one
  // is read follows the search box.
  const { data: connectionList } = useServerData(
    searchTerm
      ? `/rest/SearchExternalConnectionMenu?search=Y&startingRecNo=${startingRecNo}&searchString=${searchTerm}`
      : `/rest/ExternalConnectionMenu?startingRecNo=${startingRecNo}`,
  );
  const invalidateServerData = useInvalidateServerData();

  useEffect(() => {
    if (connectionList) {
      const list = connectionList.menuList.map((item) => {
        return {
          id: String(item.id),
          name:
            item.nameLocalization && item.nameLocalization.localizedValue
              ? item.nameLocalization.localizedValue
              : "",
          programmedConnection: item.programmedConnection || "",
          uri: item.uri || "",
          authType: item.activeAuthenticationType || "",
          active: item.active != null ? String(item.active) : "",
        };
      });
      setFromRecordCount(connectionList.fromRecordCount);
      setToRecordCount(connectionList.toRecordCount);
      setTotalRecordCount(connectionList.totalRecordCount);
      setServerPageSize((previous) =>
        serverPageSizeFrom(
          connectionList.fromRecordCount,
          connectionList.toRecordCount,
          connectionList.totalRecordCount,
          previous,
        ),
      );
      setConnectionListShow(list);
    }
  }, [connectionList]);

  useEffect(() => {
    if (selectedRowIds.length === 0) {
      setDeactivateButton(true);
    } else {
      setDeactivateButton(false);
    }
    if (selectedRowIds.length === 1) {
      setModifyButton(false);
    } else {
      setModifyButton(true);
    }
  }, [selectedRowIds]);

  useEffect(() => {
    if (isSearching && searchTerm === "") {
      setIsSearching(false);
      setPage(1);
    }
  }, [isSearching, searchTerm]);

  const renderCell = (cell, row) => {
    if (cell.info.header === "select") {
      return (
        <TableSelectRow
          key={cell.id}
          id={cell.id}
          checked={selectedRowIds.includes(row.id)}
          name="selectRowCheckbox"
          ariaLabel="selectRows"
          onSelect={() => {
            if (selectedRowIds.includes(row.id)) {
              setSelectedRowIds(selectedRowIds.filter((id) => id !== row.id));
            } else {
              setSelectedRowIds([...selectedRowIds, row.id]);
            }
          }}
        />
      );
    } else {
      return <TableCell key={cell.id}>{cell.value}</TableCell>;
    }
  };

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                <FormattedMessage id="externalconnections.browse.title" />
              </Heading>
            </Section>
          </Column>
        </Grid>
        <br />
        <ActionPaginationButtonType
          selectedRowIds={selectedRowIds}
          modifyButton={modifyButton}
          deactivateButton={deactivateButton}
          fromRecordCount={fromRecordCount}
          toRecordCount={toRecordCount}
          totalRecordCount={totalRecordCount}
          handlePreviousPage={handlePreviousPage}
          handleNextPage={handleNextPage}
          deleteDeactivate={deactivateConnection}
          id={selectedRowIds[0]}
          otherParmsInLink={`&startingRecNo=1`}
          addButtonRedirectLink={`/MasterListsPage/externalConnectionEdit?ID=0`}
          modifyButtonRedirectLink={`/MasterListsPage/externalConnectionEdit?ID=`}
          type="type2"
        />
        <br />
        <div className="orderLegendBody">
          <Grid>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Search
                  size="lg"
                  id="ext-conn-search-bar"
                  labelText={
                    <FormattedMessage id="externalconnections.search" />
                  }
                  placeholder={intl.formatMessage({
                    id: "externalconnections.search.placeholder",
                  })}
                  onChange={handleSearchChange}
                  value={searchTerm || ""}
                />
              </Section>
            </Column>
          </Grid>
          <br />
          <Grid fullWidth={true} className="gridBoundary">
            <Column lg={16} md={8} sm={4}>
              <DataTable
                rows={connectionListShow}
                headers={[
                  {
                    key: "select",
                    header: intl.formatMessage({
                      id: "externalconnections.select",
                    }),
                  },
                  {
                    key: "name",
                    header: intl.formatMessage({
                      id: "externalconnections.name",
                    }),
                  },
                  {
                    key: "programmedConnection",
                    header: intl.formatMessage({
                      id: "externalconnections.programmedconnection",
                    }),
                  },
                  {
                    key: "uri",
                    header: intl.formatMessage({
                      id: "externalconnections.uri",
                    }),
                  },
                  {
                    key: "authType",
                    header: intl.formatMessage({
                      id: "externalconnections.authtype",
                    }),
                  },
                  {
                    key: "active",
                    header: intl.formatMessage({
                      id: "externalconnections.active",
                    }),
                  },
                ]}
              >
                {({ rows, headers, getHeaderProps, getTableProps }) => (
                  <TableContainer>
                    <Table {...getTableProps()}>
                      <TableHead>
                        <TableRow>
                          {headers.map((header) => (
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
                        {rows.map((row) => (
                          <TableRow
                            key={row.id}
                            onClick={() => {
                              const id = row.id;
                              const isSelected = selectedRowIds.includes(id);
                              if (isSelected) {
                                setSelectedRowIds(
                                  selectedRowIds.filter(
                                    (selectedId) => selectedId !== id,
                                  ),
                                );
                              } else {
                                setSelectedRowIds([...selectedRowIds, id]);
                              }
                            }}
                          >
                            {row.cells.map((cell) => renderCell(cell, row))}
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                )}
              </DataTable>
              <Pagination
                onChange={handlePageChange}
                page={page}
                pageSize={serverPageSize}
                pageSizes={[serverPageSize]}
                pageSizeInputDisabled
                totalItems={Number(totalRecordCount) || 0}
                forwardText={intl.formatMessage({
                  id: "pagination.forward",
                })}
                backwardText={intl.formatMessage({
                  id: "pagination.backward",
                })}
                itemRangeText={(min, max, total) =>
                  intl.formatMessage(
                    { id: "pagination.item-range" },
                    { min: min, max: max, total: total },
                  )
                }
                itemsPerPageText={intl.formatMessage({
                  id: "pagination.items-per-page",
                })}
                itemText={(min, max) =>
                  intl.formatMessage(
                    { id: "pagination.item" },
                    { min: min, max: max },
                  )
                }
                pageNumberText={intl.formatMessage({
                  id: "pagination.page-number",
                })}
                pageRangeText={(_current, total) =>
                  intl.formatMessage(
                    { id: "pagination.page-range" },
                    { total: total },
                  )
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

export default injectIntl(ExternalConnectionMenu);
