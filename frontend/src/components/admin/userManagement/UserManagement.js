import React, {
  useContext,
  useState,
  useEffect,
  useRef,
  useMemo,
  useCallback,
} from "react";
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
  Select,
  SelectItem,
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
import CustomCheckBox from "../../common/CustomCheckBox.js";
import ActionPaginationButtonType from "../../common/ActionPaginationButtonType.js";

const INITIAL_PAGE_SIZE = 10;
const PAGINATION_SIZES = [10, 20];

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "unifiedSystemUser.browser.title",
    link: "/MasterListsPage#userManagement",
  },
];

const transformUserManagementList = (menuList) =>
  menuList.map((item) => ({
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
  }));

function UserManagement() {
  const intl = useIntl();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const componentMounted = useRef(false);

  // Pagination state
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(INITIAL_PAGE_SIZE);
  const [startingRecNo, setStartingRecNo] = useState(1);
  const [fromRecordCount, setFromRecordCount] = useState("");
  const [toRecordCount, setToRecordCount] = useState("");
  const [totalRecordCount, setTotalRecordCount] = useState("");
  const [paging, setPaging] = useState(1);

  // Selection state
  const [selectedRowIds, setSelectedRowIds] = useState([]);
  const [selectedRowCombinedUserID, setSelectedRowCombinedUserID] = useState(
    [],
  );

  // UI state
  const [loading, setLoading] = useState(true);
  const [isSearching, setIsSearching] = useState(false);
  const [panelSearchTerm, setPanelSearchTerm] = useState("");
  const [roleFilter, setRoleFilter] = useState("");
  const [filters, setFilters] = useState([]);

  // Data state
  const [userManagementList, setUserManagementList] = useState();
  const [searchedUserManagementList, setSearchedUserManagementList] =
    useState();
  const [testSectionsShow, setTestSectionsShow] = useState({});

  // Memoized derived state
  const deactivateButton = useMemo(
    () => selectedRowIds.length === 0,
    [selectedRowIds],
  );
  const modifyButton = useMemo(
    () => selectedRowIds.length !== 1,
    [selectedRowIds],
  );

  const userManagementListShow = useMemo(
    () =>
      userManagementList?.menuList
        ? transformUserManagementList(userManagementList.menuList)
        : [],
    [userManagementList],
  );

  const searchedUserManagementListShow = useMemo(
    () =>
      searchedUserManagementList?.menuList
        ? transformUserManagementList(searchedUserManagementList.menuList)
        : [],
    [searchedUserManagementList],
  );

  // Memoized handlers
  const handleDeleteDeactivateUserManagement = useCallback(
    async (event) => {
      event.preventDefault();
      setLoading(true);

      try {
        const res = await postToOpenElisServerJsonResponse(
          `/rest/DeleteUnifiedSystemUser?ID=${selectedRowCombinedUserID.join(",")}&startingRecNo=1`,
          JSON.stringify({ selectedIDs: selectedRowCombinedUserID }),
        );

        if (res) {
          setNotificationVisible(true);
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "notification.user.post.delete.success",
            }),
            kind: NotificationKinds.success,
          });
        } else {
          throw new Error("Server error");
        }
      } catch (error) {
        addNotification({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.title" }),
          message: intl.formatMessage({ id: "server.error.msg" }),
        });
        setNotificationVisible(true);
      } finally {
        setLoading(false);
        setTimeout(() => window.location.reload(), 200);
      }
    },
    [selectedRowCombinedUserID, intl, addNotification, setNotificationVisible],
  );

  const handlePageChange = useCallback(({ page, pageSize }) => {
    setPage(page);
    setPageSize(pageSize);
    setSelectedRowIds([]);
    setSelectedRowCombinedUserID([]);
  }, []);

  const handlePanelSearchChange = useCallback((event) => {
    setIsSearching(true);
    setPaging(1);
    setStartingRecNo(1);
    setPanelSearchTerm(event.target.value);
    setSelectedRowIds([]);
  }, []);

  const handleTestSectionsSelectChange = useCallback((e) => {
    setRoleFilter(e.target.value);
  }, []);

  // Memoized table headers
  const TABLE_HEADERS = useMemo(
    () => [
      {
        key: "select",
        header: intl.formatMessage({ id: "unifiedSystemUser.select" }),
      },
      {
        key: "firstName",
        header: intl.formatMessage({ id: "systemuser.firstName" }),
      },
      {
        key: "lastName",
        header: intl.formatMessage({ id: "systemuser.lastName" }),
      },
      {
        key: "loginName",
        header: intl.formatMessage({ id: "systemuser.loginName" }),
      },
      {
        key: "expDate",
        header: intl.formatMessage({ id: "login.password.expired.date" }),
      },
      {
        key: "locked",
        header: intl.formatMessage({ id: "login.account.locked" }),
      },
      {
        key: "disabled",
        header: intl.formatMessage({ id: "login.account.disabled" }),
      },
      {
        key: "active",
        header: intl.formatMessage({ id: "systemuser.isActive" }),
      },
      { key: "timeout", header: intl.formatMessage({ id: "login.timeout" }) },
    ],
    [intl],
  );

  // Effects
  useEffect(() => {
    componentMounted.current = true;
    return () => {
      componentMounted.current = false;
    };
  }, []);

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const res = await getFromOpenElisServer(
          `/rest/SearchUnifiedSystemUserMenu?search=N&startingRecNo=${startingRecNo}&filter=${filters.join(",")}&roleFilter=${roleFilter}`,
        );
        if (componentMounted.current) {
          setUserManagementList(res);
          if (res?.testSections) {
            setTestSectionsShow(res.testSections);
          }
        }
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [roleFilter, filters, startingRecNo]);

  useEffect(() => {
    if (userManagementList) {
      const { fromRecordCount, toRecordCount, totalRecordCount } =
        userManagementList;
      setFromRecordCount(fromRecordCount);
      setToRecordCount(toRecordCount);
      setTotalRecordCount(totalRecordCount);
    }
  }, [userManagementList]);

  useEffect(() => {
    if (isSearching) {
      const fetchSearchResults = async () => {
        const res = await getFromOpenElisServer(
          `/rest/SearchUnifiedSystemUserMenu?search=Y&startingRecNo=${startingRecNo}&searchString=${panelSearchTerm}&filter=${filters.join(",")}&roleFilter=${roleFilter}`,
        );
        setSearchedUserManagementList(res);
      };
      fetchSearchResults();
    }
  }, [panelSearchTerm, roleFilter, filters, startingRecNo, isSearching]);

  if (!loading) {
    return <Loading />;
  }

  const currentList = isSearching
    ? searchedUserManagementListShow
    : userManagementListShow;
  const paginatedList = currentList.slice(
    (page - 1) * pageSize,
    page * pageSize,
  );

  return (
    <>
      {notificationVisible && <AlertDialog />}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                <FormattedMessage id="unifiedSystemUser.browser.title" />
              </Heading>
            </Section>
            <br />
            <Section>
              <Heading>
                <FormattedMessage id="user.select.instruction" />
              </Heading>
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
                  handleDeleteDeactivate={handleDeleteDeactivateUserManagement}
                  id={selectedRowCombinedUserID[0]}
                  otherParmsInLink="&startingRecNo=1&roleFilter="
                  addButtonRedirectLink="/MasterListsPage#userEdit?ID=0&startingRecNo=1&roleFilter="
                  modifyButtonRedirectLink="/MasterListsPage#userEdit?ID="
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
                  value={panelSearchTerm || ""}
                />
              </Section>
            </Column>
          </Grid>
          <br />

          <Grid fullWidth>
            <Column lg={2} md={2} sm={1}>
              <FormattedMessage id="menu.label.filter" />
            </Column>
            <Column lg={6} md={6} sm={3}>
              <Select
                id="filters"
                labelText={<FormattedMessage id="menu.label.filter.role" />}
                defaultValue={testSectionsShow?.[0]?.id || ""}
                onChange={handleTestSectionsSelectChange}
              >
                <SelectItem key="" value="" text="" />
                {testSectionsShow?.length > 0 ? (
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
                    text="No options available"
                  />
                )}
              </Select>
            </Column>
            <Column lg={8} md={8} sm={4}>
              <CustomCheckBox
                id="only-active"
                label={<FormattedMessage id="menu.label.filter.active" />}
                onChange={(isChecked) => {
                  setFilters((prev) =>
                    isChecked
                      ? [...prev, "isActive"]
                      : prev.filter((filter) => filter !== "isActive"),
                  );
                }}
              />
              <br />
              <CustomCheckBox
                id="only-administrator"
                label={<FormattedMessage id="menu.label.filter.admin" />}
                onChange={(isChecked) => {
                  setFilters((prev) =>
                    isChecked
                      ? [...prev, "isAdmin"]
                      : prev.filter((filter) => filter !== "isAdmin"),
                  );
                }}
              />
            </Column>
          </Grid>
          <br />

          <Grid fullWidth className="gridBoundary">
            <Column lg={16} md={8} sm={4}>
              <DataTable rows={paginatedList} headers={TABLE_HEADERS}>
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
                          <TableSelectAll
                            id="table-select-all"
                            {...getSelectionProps()}
                            checked={
                              selectedRowIds.length === pageSize &&
                              currentList
                                .slice((page - 1) * pageSize, page * pageSize)
                                .filter(
                                  (row) =>
                                    !row.disabled &&
                                    selectedRowIds.includes(row.id),
                                ).length === pageSize
                            }
                            indeterminate={
                              selectedRowIds.length > 0 &&
                              selectedRowIds.length <
                                currentList
                                  .slice((page - 1) * pageSize, page * pageSize)
                                  .filter((row) => !row.disabled).length
                            }
                            onSelect={() => {
                              const currentPageIds = currentList
                                .slice((page - 1) * pageSize, page * pageSize)
                                .filter((row) => !row.disabled)
                                .map((row) => row.id);

                              setSelectedRowIds((prev) =>
                                prev.length === pageSize &&
                                currentPageIds.every((id) => prev.includes(id))
                                  ? []
                                  : currentPageIds.filter(
                                      (id) => !prev.includes(id),
                                    ),
                              );
                            }}
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
                          <TableRow
                            key={row.id}
                            onClick={() => {
                              const id = row.id;
                              setSelectedRowIds((prev) =>
                                prev.includes(id)
                                  ? prev.filter(
                                      (selectedId) => selectedId !== id,
                                    )
                                  : [...prev, id],
                              );
                              setSelectedRowCombinedUserID((prev) =>
                                prev.includes(row.combinedUserID)
                                  ? prev.filter(
                                      (id) => id !== row.combinedUserID,
                                    )
                                  : [...prev, row.combinedUserID],
                              );
                            }}
                          >
                            <TableSelectRow {...getSelectionProps({ row })} />
                            {Object.entries(row)
                              .filter(
                                ([key]) =>
                                  key !== "id" && key !== "combinedUserID",
                              )
                              .map(([key, value]) => (
                                <TableCell key={key}>{value}</TableCell>
                              ))}
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
                pageSizes={PAGINATION_SIZES}
                totalItems={currentList.length}
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

export default injectIntl(UserManagement);
