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
  Modal,
  TextInput,
  Dropdown,
  Select,
  SelectItem,
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils.js";
import {
  ConfigurationContext,
  NotificationContext,
} from "../../layout/Layout.js";
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
    label: "sidenav.label.admin.analyzerTest",
    link: "/MasterListsPage#analyzerMenu",
  },
];
function AnalyzerTestName() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { reloadConfiguration } = useContext(ConfigurationContext);

  const intl = useIntl();

  const componentMounted = useRef(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [modifyButton, setModifyButton] = useState(true);
  const [deactivateButton, setDeactivateButton] = useState(true);
  const [selectedRowIds, setSelectedRowIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [startingRecNo, setStartingRecNo] = useState(1);
  const [AnalyzerTestName, setAnalyzerTestName] = useState({});
  const [AnalyzerTestNameShow, setAnalyzerTestNameShow] = useState([]);
  const [fromRecordCount, setFromRecordCount] = useState("1");
  const [toRecordCount, setToRecordCount] = useState("");
  const [totalRecordCount, setTotalRecordCount] = useState("");
  const [paging, setPaging] = useState(null);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [isUpdateModalOpen, setIsUpdateModalOpen] = useState(false);
  const [testName, setTestName] = useState("");
  const [analyzerList, setAnalyzerList] = useState([]);
  const [testList, setTestList] = useState([]);
  const [selectedAnalyzer, setSelectedAnalyzer] = useState(null);
  const [selectedAnalyzerId, setSelectedAnalyzerId] = useState(null);
  const [selectedTest, setSelectedTest] = useState(null);
  const [selectedTestId, setSelectedTestId] = useState(null);
  const [filterAnalyzer, setFilterAnalyser] = useState("");

  const handleMenuItems = (res) => {
    if (!res) {
      setLoading(true);
    } else {
      setAnalyzerTestName(res);
    }
  };

  useEffect(() => {
    componentMounted.current = true;
    setLoading(true);
    getFromOpenElisServer(
      `/rest/AnalyzerTestNameMenu?paging=${paging}&startingRecNo=${startingRecNo}&analyzerId=${filterAnalyzer}`,
      handleMenuItems,
    );
    fetchDropdownData();
    fetchDropdownDatatestlist();
    return () => {
      componentMounted.current = false;
      setLoading(false);
    };
  }, [paging, startingRecNo]);

  useEffect(() => {
    if (AnalyzerTestName.menuList) {
      const newAnalyzerTestName = AnalyzerTestName.menuList.map((item) => {
        return {
          id: item.uniqueId,
          analyzerName: `${item.analyzerName} - ${item.analyzerTestName}`,
          actualTestName: item.actualTestName,
        };
      });
      setFromRecordCount(AnalyzerTestName.fromRecordCount);
      setToRecordCount(AnalyzerTestName.toRecordCount);
      setTotalRecordCount(AnalyzerTestName.totalRecordCount);
      setAnalyzerTestNameShow(newAnalyzerTestName);
    }
  }, [AnalyzerTestName]);

  const fetchDropdownData = async () => {
    getFromOpenElisServer(
      "/rest/AnalyzerTestName?ID=0&startingRecNo=1",
      handleDropDown,
    );
  };

  const fetchDropdownDatatestlist = async () => {
    getFromOpenElisServer("/rest/test-list", handleDropDownTestList);
  };

  function handleDropDownTestList(response) {
    setTestList(response);
  }

  function handleDropDown(response) {
    if (response) {
      setAnalyzerList(response.analyzerList || []);
      // setTestList(response.testList || []);
      if (response.analyzerList.length == 0) {
        setNotificationVisible(true);
        addNotification({
          kind: NotificationKinds.warning,
          title: intl.formatMessage({ id: "notification.title" }),
          message: intl.formatMessage({ id: "message.noPluginFound" }),
        });
      }
    }
  }

  useEffect(() => {
    if (selectedRowIds.length === 1) {
      setModifyButton(false);
    } else {
      setModifyButton(true);
    }
  }, [selectedRowIds]);

  useEffect(() => {
    setLoading(true);
    setFromRecordCount("");
    setToRecordCount("");
    setTotalRecordCount("");
    getFromOpenElisServer(
      `/rest/AnalyzerTestNameMenu?analyzerId=${filterAnalyzer}`,
      handleMenuItems,
    );
  }, [filterAnalyzer]);

  useEffect(() => {
    if (selectedRowIds.length === 0) {
      setDeactivateButton(true);
    } else {
      setDeactivateButton(false);
    }
  }, [selectedRowIds]);

  async function displayStatus(res) {
    setNotificationVisible(true);
    if (res.status == "201" || res.status == "200") {
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "save.config.success.msg" }),
      });
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
    }
    reloadConfiguration();
  }

  function deleteDeactivateAnalyzer(event) {
    event.preventDefault();
    setLoading(true);
    const selectedIds = { selectedIDs: selectedRowIds };
    postToOpenElisServerFullResponse(
      `/rest/DeleteAnalyzerTestName?ID=${selectedRowIds.join(",")}&${startingRecNo}=1`,
      JSON.stringify(selectedIds),
      setLoading(false),
      setTimeout(() => {
        window.location.reload();
      }, 1000),
    );
  }

  const handlePageChange = ({ page, pageSize }) => {
    setPage(page);
    setPageSize(pageSize);
    setSelectedRowIds([]);
  };

  const handleNextPage = () => {
    setPaging((pager) => Math.max(pager, 2));
    setStartingRecNo(fromRecordCount);
  };

  const handlePreviousPage = () => {
    setPaging((pager) => Math.max(pager - 1, 1));
    setStartingRecNo(Math.max(fromRecordCount, 1));
  };

  const openAddModal = () => {
    setTestName("");
    setSelectedAnalyzer(null);
    setSelectedAnalyzerId(null);
    setSelectedTest(null);
    setSelectedTestId(null);
    setIsAddModalOpen(true);
  };

  const closeAddModal = () => {
    setIsAddModalOpen(false);
  };

  const openUpdateModal = (AnalyzerId) => {
    setIsUpdateModalOpen(true);
  };

  const closeUpdateModal = () => {
    setIsUpdateModalOpen(false);
  };
  const checkIfCombinationExists = () => {
    return AnalyzerTestNameShow.some(
      (item) => item.analyzerName === `${selectedAnalyzer.name} - ${testName}`,
    );
  };

  const handleAddAnalyzer = () => {
    if (checkIfCombinationExists()) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "analyzer.combinationName.notification",
        }),
      });
      setNotificationVisible(true);
      return;
    }

    const newAnalyzer = {
      analyzerId: selectedAnalyzerId,
      analyzerTestName: testName,
      testId: selectedTestId,
    };

    postToOpenElisServerFullResponse(
      "/rest/AnalyzerTestName",
      JSON.stringify(newAnalyzer),
      displayStatus,
    );

    closeAddModal();
    window.location.reload();
  };

  const handleUpdateAnalyzer = () => {
    if (checkIfCombinationExists()) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "analyzer.combinationName.notification",
        }),
      });
      setNotificationVisible(true);
      return;
    }
    const newAnalyzer = {
      analyzerId: selectedAnalyzerId,
      analyzerTestName: testName,
      testId: selectedTestId,
    };

    postToOpenElisServerFullResponse(
      "/rest/AnalyzerTestName",
      JSON.stringify(newAnalyzer),
      displayStatus,
    );

    closeUpdateModal();

    window.location.reload();
  };

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
            setDeactivateButton(false);
            if (selectedRowIds.includes(row.id)) {
              setSelectedRowIds(selectedRowIds.filter((id) => id !== row.id));
            } else {
              setSelectedRowIds([...selectedRowIds, row.id]);
            }
          }}
        />
      );
    } else if (cell.info.header === "active") {
      return <TableCell key={cell.id}>{cell.value.toString()}</TableCell>;
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
                <FormattedMessage id="sidenav.label.admin.analyzerTest" />
              </Heading>
            </Section>
          </Column>
        </Grid>
        <br />
        <ActionPaginationButtonType
          selectedRowIds={selectedRowIds}
          modifyButton={modifyButton}
          deactivateButton={deactivateButton}
          deleteDeactivate={deleteDeactivateAnalyzer}
          openUpdateModal={openUpdateModal}
          openAddModal={openAddModal}
          handlePreviousPage={handlePreviousPage}
          handleNextPage={handleNextPage}
          fromRecordCount={fromRecordCount}
          toRecordCount={toRecordCount}
          totalRecordCount={totalRecordCount}
          type="type1"
        />
        <br />
        <Modal
          open={isAddModalOpen}
          size="sm"
          modalHeading="Add Analyzer Test Name"
          primaryButtonText="Add"
          secondaryButtonText="Cancel"
          primaryButtonDisabled={analyzerList.length == 0}
          onRequestSubmit={handleAddAnalyzer}
          onRequestClose={closeAddModal}
        >
          <Dropdown
            id="analyzer-dropdown"
            titleText={intl.formatMessage({
              id: "banner.menu.results.analyzer",
            })}
            items={analyzerList}
            itemToString={(item) => (item ? item.name : "")}
            selectedItem={selectedAnalyzer}
            onChange={({ selectedItem }) => {
              setSelectedAnalyzer(selectedItem);
              setSelectedAnalyzerId(selectedItem ? selectedItem.id : null);
            }}
          />
          <br />
          <Dropdown
            id="test-dropdown"
            titleText={intl.formatMessage({ id: "label.actualTestName" })}
            items={testList}
            itemToString={(item) => (item ? item.value : "")}
            selectedItem={selectedTest}
            onChange={({ selectedItem }) => {
              setSelectedTest(selectedItem);
              setSelectedTestId(selectedItem ? selectedItem.id : null);
            }}
          />
          <br />
          <TextInput
            id="testName"
            labelText={intl.formatMessage({
              id: "sidenav.label.admin.analyzerTest",
            })}
            value={testName}
            onChange={(e) => setTestName(e.target.value)}
            required
          />
          <br />
        </Modal>

        <Modal
          open={isUpdateModalOpen}
          size="sm"
          modalHeading="Update AnalyzerTestName"
          primaryButtonText="Update"
          secondaryButtonText="Cancel"
          onRequestSubmit={handleUpdateAnalyzer}
          onRequestClose={closeUpdateModal}
        >
          <Dropdown
            id="analyzer-dropdown"
            titleText={intl.formatMessage({
              id: "banner.menu.results.analyzer",
            })}
            items={analyzerList}
            itemToString={(item) => (item ? item.name : "")}
            selectedItem={selectedAnalyzer}
            onChange={({ selectedItem }) => {
              setSelectedAnalyzer(selectedItem);
              setSelectedAnalyzerId(selectedItem ? selectedItem.id : null);
            }}
          />
          <Dropdown
            id="test-dropdown"
            titleText={intl.formatMessage({ id: "label.actualTestName" })}
            items={testList}
            itemToString={(item) => (item ? item.value : "")}
            selectedItem={selectedTest}
            onChange={({ selectedItem }) => {
              setSelectedTest(selectedItem);
              setSelectedTestId(selectedItem ? selectedItem.id : null);
            }}
          />
          <br />
          <TextInput
            id="testName"
            labelText={intl.formatMessage({
              id: "sidenav.label.admin.analyzerTest",
            })}
            value={testName}
            onChange={(e) => setTestName(e.target.value)}
            required
          />
          <br />
        </Modal>

        <div className="orderLegendBody">
          <>
            <Grid fullWidth={true} className="gridBoundary">
              <Column lg={4} md={8} sm={4}>
                <Select
                  labelText={intl.formatMessage({
                    id: "select.label.analyzer",
                    defaultMessage: "Select Analyzer",
                  })}
                  value={filterAnalyzer}
                  onChange={(e) => {
                    setFilterAnalyser(e.target.value);
                  }}
                >
                  <SelectItem
                    value=""
                    text={intl.formatMessage({
                      id: "all.label",
                    })}
                  />
                  {analyzerList.map((analyzer, analyzer_index) => (
                    <SelectItem
                      text={analyzer.name}
                      value={analyzer.id}
                      key={analyzer_index}
                    />
                  ))}
                </Select>
              </Column>
              <Column lg={12} md={8} sm={4}></Column>
              <Column lg={16} md={8} sm={4}>
                <DataTable
                  rows={AnalyzerTestNameShow.slice(
                    (page - 1) * pageSize,
                    page * pageSize,
                  )}
                  headers={[
                    {
                      key: "select",
                      header: "select",
                    },
                    {
                      key: "analyzerName",
                      header: "Analyzer - Analyzer test name",
                    },

                    {
                      key: "actualTestName",
                      header: "Actual test Name",
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
                            <TableSelectAll
                              id="table-select-all"
                              {...getSelectionProps()}
                              checked={
                                selectedRowIds.length === pageSize &&
                                AnalyzerTestNameShow.slice(
                                  (page - 1) * pageSize,
                                  page * pageSize,
                                ).filter(
                                  (row) =>
                                    !row.disabled &&
                                    selectedRowIds.includes(row.id),
                                ).length === pageSize
                              }
                              indeterminate={
                                selectedRowIds.length > 0 &&
                                selectedRowIds.length <
                                  AnalyzerTestNameShow.slice(
                                    (page - 1) * pageSize,
                                    page * pageSize,
                                  ).filter((row) => !row.disabled).length
                              }
                              onSelect={() => {
                                setDeactivateButton(false);
                                const currentPageIds =
                                  AnalyzerTestNameShow.slice(
                                    (page - 1) * pageSize,
                                    page * pageSize,
                                  )
                                    .filter((row) => !row.disabled)
                                    .map((row) => row.id);
                                if (
                                  selectedRowIds.length === pageSize &&
                                  currentPageIds.every((id) =>
                                    selectedRowIds.includes(id),
                                  )
                                ) {
                                  setSelectedRowIds([]);
                                } else {
                                  setSelectedRowIds(
                                    currentPageIds.filter(
                                      (id) => !selectedRowIds.includes(id),
                                    ),
                                  );
                                }
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
                          <>
                            {rows.map((row) => (
                              <TableRow
                                key={row.id}
                                onClick={() => {
                                  const id = row.id;
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
                  totalItems={AnalyzerTestNameShow.length}
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

export default injectIntl(AnalyzerTestName);
