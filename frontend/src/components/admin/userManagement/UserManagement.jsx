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
  Select,
  SelectItem,
} from "@carbon/react";
import { postToOpenElisServerJsonResponse } from "../../utils/Utils";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { serverQuery } from "../../utils/queryClient";
import { NotificationContext } from "../../layout/contexts";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import CustomCheckBox from "../../common/CustomCheckBox";
import ActionPaginationButtonType from "../../common/ActionPaginationButtonType";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "unifiedSystemUser.browser.title",
    link: "/MasterListsPage/userManagement",
  },
];

// Every read of the user list shares this prefix, so one invalidation covers
// the filtered, searched and paged variants alike.
const USER_LIST_KEY = ["userManagement", "list"];

function UserManagement() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const queryClient = useQueryClient();
  const componentMounted = useRef(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [deactivateButton, setDeactivateButton] = useState(true);
  const [modifyButton, setModifyButton] = useState(true);
  const [selectedRowIds, setSelectedRowIds] = useState([]);
  const [selectedRowCombinedUserID, setSelectedRowCombinedUserID] = useState(
    [],
  );
  const [selectedRowCombinedUserIDPost, setSelectedRowCombinedUserIDPost] =
    useState([]);
  const [selectedRowIdsPost, setSelectedRowIdsPost] = useState();
  const [loading, setLoading] = useState(true);
  const [isSearching, setIsSearching] = useState(false);
  const [panelSearchTerm, setPanelSearchTerm] = useState("");
  const [roleFilter, setRoleFilter] = useState("");
  const [filters, setFilters] = useState([]);
  const [startingRecNo, setStartingRecNo] = useState(1);
  const [totalRecordCount, setTotalRecordCount] = useState("");
  const [paging, setPaging] = useState(1);
  const [fromRecordCount, setFromRecordCount] = useState("");
  const [toRecordCount, setToRecordCount] = useState("");
  const [userManagementList, setUserManagementList] = useState();
  const [userManagementListShow, setUserManagementListShow] = useState([]);
  const [testSectionsShow, setTestSectionsShow] = useState({});

  function deleteDeactivateUserManagement(event) {
    event.preventDefault();
    setLoading(true);
    postToOpenElisServerJsonResponse(
      `/rest/DeleteUnifiedSystemUser?ID=${selectedRowCombinedUserID.join(
        ",",
      )}&startingRecNo=1`,
      JSON.stringify(selectedRowCombinedUserIDPost),
      (res) => {
        deleteDeactivateUserManagementCallback(res);
      },
    );
  }

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

  useEffect(() => {
    const selectedIDsObject = {
      selectedIDs: selectedRowIds,
    };

    setSelectedRowIdsPost(selectedIDsObject);
  }, [selectedRowIds, userManagementListShow]);

  useEffect(() => {
    const selectedRowCombinedUserIDObject = {
      selectedIDs: selectedRowCombinedUserID,
    };

    setSelectedRowCombinedUserIDPost(selectedRowCombinedUserIDObject);
  }, [selectedRowCombinedUserID, userManagementListShow]);

  function deleteDeactivateUserManagementCallback(res) {
    if (res) {
      setLoading(false);
      setNotificationVisible(true);
      addNotification({
        title: intl.formatMessage({
          id: "notification.title",
        }),
        message: intl.formatMessage({
          id: "notification.user.post.delete.success",
        }),
        kind: NotificationKinds.success,
      });
      setSelectedRowIds([]);
      queryClient.invalidateQueries({ queryKey: USER_LIST_KEY });
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
      // A failed delete leaves the list as it was: reloading here would have
      // discarded the filters and selection the user still needs.
    }
  }

  const handlePageChange = ({ page, pageSize }) => {
    setPage(page);
    setPageSize(pageSize);
    setSelectedRowIds([]);
    setSelectedRowCombinedUserID([]);
  };

  // What the screen shows is a read of one endpoint, so the endpoint is the
  // cache key: a write invalidates USER_LIST_KEY and the list is read again,
  // which is what reloading the document used to accomplish.
  const userListEndpoint = panelSearchTerm
    ? `/rest/SearchUnifiedSystemUserMenu?search=Y&startingRecNo=${startingRecNo}&searchString=${panelSearchTerm}&filter=${filters.join(
        ",",
      )}&roleFilter=${roleFilter}`
    : `/rest/SearchUnifiedSystemUserMenu?search=N&startingRecNo=${startingRecNo}&filter=${filters.join(
        ",",
      )}&roleFilter=${roleFilter}`;

  const {
    data: fetchedUserList,
    isFetching: userListFetching,
    isError: userListFailed,
  } = useQuery({
    ...serverQuery(USER_LIST_KEY.concat(userListEndpoint), userListEndpoint),
    keepPreviousData: true,
  });

  useEffect(() => {
    if (fetchedUserList) {
      setUserManagementList(fetchedUserList);
    }
  }, [fetchedUserList]);

  // This screen calls useQuery directly rather than through useServerData
  // (its cache key needs a distinct invalidation scope), so it does not pick
  // up that hook's own notify-on-error handling and needs its own.
  const userListFailureNotified = useRef(false);
  useEffect(() => {
    if (userListFailed && !userListFailureNotified.current) {
      userListFailureNotified.current = true;
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
    } else if (!userListFailed) {
      userListFailureNotified.current = false;
    }
  }, [userListFailed]);

  useEffect(() => {
    if (userManagementListShow) {
      if (selectedRowIds.length > 0) {
        const combinedIds = selectedRowIds.map((id) => {
          const selectedRow = userManagementListShow.find(
            (row) => row.id === id,
          );
          return selectedRow.combinedUserID;
        });
        setSelectedRowCombinedUserID(combinedIds);
      } else {
        setSelectedRowCombinedUserID([]);
      }
    }
  }, [selectedRowIds, userManagementListShow]);

  useEffect(() => {
    if (userManagementList) {
      const pagination = {
        totalRecordCount: userManagementList.totalRecordCount,
        fromRecordCount: userManagementList.fromRecordCount,
        toRecordCount: userManagementList.toRecordCount,
      };
      setFromRecordCount(pagination.fromRecordCount);
      setToRecordCount(pagination.toRecordCount);
      setTotalRecordCount(pagination.totalRecordCount);

      const newUserManagementList = userManagementList.menuList.map((item) => {
        return {
          id: item.systemUserId,
          combinedUserID: item.combinedUserID,
          firstName: item.firstName,
          lastName: item.lastName,
          loginName: item.loginName,
          expDate: item.expDate,
          locked: item.locked,
          disabled: item.disabled,
          active: item.active,
          timeout: item.timeout,
        };
      });
      const newUserManagementListArray = Object.values(newUserManagementList);
      setUserManagementListShow(newUserManagementListArray);

      const testSections = userManagementList.testSections.map((item) => {
        return {
          id: item.id,
          value: item.value,
        };
      });

      setTestSectionsShow(testSections);
    }
  }, [userManagementList]);

  useEffect(() => {
    if (selectedRowIds.length === 1) {
      setModifyButton(false);
    } else {
      setModifyButton(true);
    }
    if (selectedRowIds.length === 0) {
      setDeactivateButton(true);
    } else {
      setDeactivateButton(false);
    }
  }, [selectedRowIds]);

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
            const isActiveCell = row.cells.find((cell) =>
              cell.id.endsWith(":active"),
            );

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

  const handlePanelSearchChange = (event) => {
    setIsSearching(true);
    setPaging(1);
    setStartingRecNo(1);
    const query = event.target.value;
    setPanelSearchTerm(query);
    setSelectedRowIds([]);
  };

  useEffect(() => {
    if (isSearching && panelSearchTerm === "") {
      setIsSearching(false);
      setPaging(1);
      setStartingRecNo(1);
    }
  }, [isSearching, panelSearchTerm]);

  function handleTestSectionsSelectChange(e) {
    setRoleFilter(e.target.value);
  }

  if (userListFetching && !userManagementList) {
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
                <FormattedMessage id="unifiedSystemUser.browser.title" />
              </Heading>
            </Section>
            <br />
            <Section>
              <Section>
                <Section>
                  <Section>
                    <Heading>
                      <FormattedMessage id="user.select.instruction" />
                    </Heading>
                  </Section>
                </Section>
              </Section>
            </Section>
            <br />
            <Section>
              <Column lg={16} md={8} sm={4}>
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
                  deleteDeactivate={deleteDeactivateUserManagement}
                  id={selectedRowCombinedUserID[0]}
                  otherParmsInLink={`&startingRecNo=1&roleFilter=`}
                  addButtonRedirectLink={`/MasterListsPage/userEdit?ID=0&startingRecNo=1&roleFilter=`}
                  modifyButtonRedirectLink={`/MasterListsPage/userEdit?ID=`}
                  type="type2"
                />
                <br />
              </Column>
            </Section>
          </Column>
        </Grid>
        <div className="orderLegendBody">
          <Grid>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Search
                  size="lg"
                  id="user-name-search-bar"
                  labelText={
                    <FormattedMessage id="unifiedSystemUser.browser.search" />
                  }
                  placeholder={intl.formatMessage({
                    id: "unifiedSystemUser.browser.search.placeholder",
                  })}
                  onChange={handlePanelSearchChange}
                  value={(() => {
                    if (panelSearchTerm) {
                      return panelSearchTerm;
                    }
                    return "";
                  })()}
                ></Search>
              </Section>
            </Column>
          </Grid>
          <br />
          <Grid fullWidth={true}>
            <Column lg={2} md={2} sm={1}>
              <FormattedMessage id="menu.label.filter" />
            </Column>
            <Column lg={6} md={6} sm={3}>
              <Select
                id="filters"
                labelText={<FormattedMessage id="menu.label.filter.role" />}
                value={roleFilter}
                onChange={handleTestSectionsSelectChange}
              >
                <SelectItem key="" value="" text="" />
                {testSectionsShow && testSectionsShow.length > 0 ? (
                  testSectionsShow.map((section) => (
                    <SelectItem
                      key={section.id}
                      value={section.id}
                      text={section.value}
                    />
                  ))
                ) : (
                  <SelectItem
                    key="no-option-available"
                    value=""
                    text={intl.formatMessage({
                      id: "label.no.options.available",
                    })}
                  />
                )}
              </Select>
            </Column>
            <Column lg={8} md={8} sm={4}>
              <CustomCheckBox
                id="only-active"
                checked={filters.includes("isActive")}
                label={<FormattedMessage id="menu.label.filter.active" />}
                onChange={(isChecked) => {
                  if (isChecked) {
                    setFilters([...filters, "isActive"]);
                  } else {
                    setFilters(
                      filters.filter((filter) => filter !== "isActive"),
                    );
                  }
                }}
              />
              <br />
              <CustomCheckBox
                id="only-administrator"
                checked={filters.includes("isAdmin")}
                label={<FormattedMessage id="menu.label.filter.admin" />}
                onChange={(isChecked) => {
                  if (isChecked) {
                    setFilters([...filters, "isAdmin"]);
                  } else {
                    setFilters(
                      filters.filter((filter) => filter !== "isAdmin"),
                    );
                  }
                }}
              />
            </Column>
          </Grid>
          <br />
          <>
            <Grid fullWidth={true} className="gridBoundary">
              <Column lg={16} md={8} sm={4}>
                <DataTable
                  rows={userManagementListShow.slice(
                    (page - 1) * pageSize,
                    page * pageSize,
                  )}
                  headers={[
                    {
                      key: "select",
                      header: intl.formatMessage({
                        id: "unifiedSystemUser.select",
                      }),
                    },
                    {
                      key: "firstName",
                      header: intl.formatMessage({
                        id: "systemuser.firstName",
                      }),
                    },
                    {
                      key: "lastName",
                      header: intl.formatMessage({
                        id: "systemuser.lastName",
                      }),
                    },
                    {
                      key: "loginName",
                      header: intl.formatMessage({
                        id: "systemuser.loginName",
                      }),
                    },
                    {
                      key: "expDate",
                      header: intl.formatMessage({
                        id: "login.password.expired.date",
                      }),
                    },
                    {
                      key: "locked",
                      header: intl.formatMessage({
                        id: "login.account.locked",
                      }),
                    },
                    {
                      key: "disabled",
                      header: intl.formatMessage({
                        id: "login.account.disabled",
                      }),
                    },
                    {
                      key: "active",
                      header: intl.formatMessage({
                        id: "systemuser.isActive",
                      }),
                    },
                    {
                      key: "timeout",
                      header: intl.formatMessage({
                        id: "login.timeout",
                      }),
                    },
                  ]}
                >
                  {({
                    rows,
                    headers,
                    getHeaderProps,
                    getTableProps,
                    getSelectionProps,
                  }) => (
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
                          <>
                            {rows.map((row) => (
                              <TableRow
                                key={row.id}
                                onClick={() => {
                                  const id = row.id;
                                  const CombinedUserID = row.combinedUserID;
                                  const isSelected =
                                    selectedRowIds.includes(id);
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
                          </>
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
                  totalItems={userManagementListShow.length}
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
          </>
        </div>
      </div>
    </>
  );
}

export default injectIntl(UserManagement);
