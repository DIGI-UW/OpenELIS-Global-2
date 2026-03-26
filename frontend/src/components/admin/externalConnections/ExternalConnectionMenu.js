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

  const componentMounted = useRef(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [deactivateButton, setDeactivateButton] = useState(true);
  const [modifyButton, setModifyButton] = useState(true);
  const [selectedRowIds, setSelectedRowIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isSearching, setIsSearching] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [totalRecordCount, setTotalRecordCount] = useState("");
  const [startingRecNo, setStartingRecNo] = useState(1);
  const [fromRecordCount, setFromRecordCount] = useState("");
  const [toRecordCount, setToRecordCount] = useState("");
  const [paging, setPaging] = useState(1);
  const [connectionList, setConnectionList] = useState();
  const [connectionListShow, setConnectionListShow] = useState([]);

  function deactivateConnection(event) {
    event.preventDefault();
    setLoading(true);
    postToOpenElisServerJsonResponse(
      `/rest/DeactivateExternalConnection?ID=${selectedRowIds.join(",")}`,
      JSON.stringify({ selectedIDs: selectedRowIds }),
      () => {
        deactivateCallback();
      },
    );
  }

  const deactivateCallback = () => {
    setLoading(false);
    setNotificationVisible(true);
    addNotification({
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage({
        id: "externalconnections.deactivate.success",
      }),
      kind: NotificationKinds.success,
    });
    setTimeout(() => {
      window.location.reload();
    }, 200);
  };

  const handleNextPage = () => {
    setPaging((pager) => Math.max(pager, 2));
    setStartingRecNo(fromRecordCount);
    setSelectedRowIds([]);
  };

  const handlePreviousPage = () => {
    setPaging((pager) => Math.max(pager - 1, 1));
    setStartingRecNo(Math.max(fromRecordCount, 1));
    setSelectedRowIds([]);
  };

  const handleSearchChange = (event) => {
    setIsSearching(true);
    setPaging(1);
    setStartingRecNo(1);
    const query = event.target.value;
    setSearchTerm(query);
    setSelectedRowIds([]);
  };

  const handlePageChange = ({ page, pageSize }) => {
    setPage(page);
    setPageSize(pageSize);
    setSelectedRowIds([]);
  };

  const handleMenuItems = (res) => {
    if (!res) {
      setLoading(true);
    } else {
      setConnectionList(res);
    }
  };

  useEffect(() => {
    componentMounted.current = true;
    setLoading(true);
    getFromOpenElisServer(
      `/rest/ExternalConnectionMenu?paging=${paging}&startingRecNo=${startingRecNo}`,
      handleMenuItems,
    );
    return () => {
      componentMounted.current = false;
      setLoading(false);
    };
  }, [paging, startingRecNo]);

  useEffect(() => {
    if (searchTerm) {
      getFromOpenElisServer(
        `/rest/SearchExternalConnectionMenu?search=Y&startingRecNo=${startingRecNo}&searchString=${searchTerm}`,
        handleMenuItems,
      );
    }
  }, [searchTerm]);

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
      setPaging(1);
      setStartingRecNo(1);
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

  if (!loading) {
    return (
      <>
        <Loading />
      </>
    );
  }

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
                rows={connectionListShow.slice(
                  (page - 1) * pageSize,
                  page * pageSize,
                )}
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
                pageSize={pageSize}
                pageSizes={[10, 20]}
                totalItems={connectionListShow.length}
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
