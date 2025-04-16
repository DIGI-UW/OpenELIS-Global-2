import { ArrowLeft, ArrowRight } from "@carbon/icons-react";
import {
  Button,
  Column,
  DataTable,
  Dropdown,
  Form,
  Grid,
  Heading,
  Modal,
  Pagination,
  Search,
  Section,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  TableSelectRow,
  TextInput,
} from "@carbon/react";
import React, { useContext, useEffect, useRef, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { ConfigurationContext, NotificationContext } from "../../layout/Layout";
import "../../Style.css";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";

function DictionaryManagement() {
  const intl = useIntl();
  const componentMounted = useRef(false);
  const dirtyFieldsRef = useRef(new Set());

  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { reloadConfiguration } = useContext(ConfigurationContext);
  const [dictionaryMenuList, setDictionaryMenuList] = useState([]);

  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [open, setOpen] = useState(false);

  const [categoryDescription, setCategoryDescription] = useState([]);

  const [category, setCategory] = useState("");
  const [dictionaryNumber, setDictionaryNumber] = useState("");
  const [dictionaryEntry, setDictionaryEntry] = useState("");
  const [localAbbreviation, setLocalAbbreviation] = useState("");
  const [isActive, setIsActive] = useState("");

  const [fromRecordCount, setFromRecordCount] = useState("1");
  const [toRecordCount, setToRecordCount] = useState("");
  const [totalRecordCount, setTotalRecordCount] = useState("");

  const [selectedRowId, setSelectedRowId] = useState(null);
  const [modifyButton, setModifyButton] = useState(true);
  const [deactivateButton, setDeactivateButton] = useState(true);
  const [editMode, setEditMode] = useState(true);

  const [paging, setPaging] = useState(null);
  const [startingRecNo, setStartingRecNo] = useState(1);
  const [isSearching, setIsSearching] = useState(false);
  const [panelSearchTerm, setPanelSearchTerm] = useState("");
  const [searchedMenuList, setSearchedMenuList] = useState([]);

  const [isMobile, setIsMobile] = useState(window.innerWidth < 530);

  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth < 530);
    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, []);

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer(
      `/rest/DictionaryMenu?paging=${paging}&startingRecNo=${startingRecNo}`,
      fetchedDictionaryMenu,
    );
    return () => {
      componentMounted.current = false;
    };
  }, [paging, startingRecNo]);

  const handleNextPage = () => {
    setPaging((pager) => Math.max(pager, 2));
    setStartingRecNo(fromRecordCount);
  };

  const handlePreviousPage = () => {
    setPaging((pager) => Math.max(pager - 1, 1));
    setStartingRecNo(Math.max(fromRecordCount, 1));
  };

  const yesOrNo = [
    {
      id: "Y",
      value: "Y",
    },
    {
      id: "N",
      value: "N",
    },
  ];

  const handlePageChange = (pageInfo) => {
    if (page != pageInfo.page) {
      setPage(pageInfo.page);
    }

    if (pageSize != pageInfo.pageSize) {
      setPageSize(pageInfo.pageSize);
    }
  };

  const fetchedDictionaryMenu = (res) => {
    if (componentMounted.current) {
      if (res) {
        if (
          res.toRecordCount !== undefined &&
          res.fromRecordCount !== undefined &&
          res.totalRecordCount !== undefined
        ) {
          setToRecordCount(res.toRecordCount);
          setFromRecordCount(res.fromRecordCount);
          setTotalRecordCount(res.totalRecordCount);
        }
        if (res.menuList) {
          const menuList = res.menuList.map((item) => ({
            id: item.id,
            dictEntry: item.dictEntry,
            localAbbreviation: item.localAbbreviation,
            isActive: item.isActive,
            categoryName: item.dictionaryCategory
              ? item.dictionaryCategory.categoryName
              : "not available",
            lastupdated: item.lastupdated,
          }));
          setDictionaryMenuList(menuList);
        }
      }
    }
  };

  const fetchedDictionaryCategory = (category) => {
    if (componentMounted.current) {
      setCategoryDescription(category);
    }
  };

  useEffect(() => {
    if (panelSearchTerm) {
      getFromOpenElisServer(
        `/rest/SearchDictionaryMenu?search=Y&startingRecNo=1&searchString=${panelSearchTerm}`,
        fetchedSearchedDictionaryMenu,
      );
    } else {
      setSearchedMenuList([]);
    }
  }, [panelSearchTerm]);

  const fetchedSearchedDictionaryMenu = (res) => {
    if (componentMounted.current) {
      if (res) {
        if (
          res.toRecordCount !== undefined &&
          res.fromRecordCount !== undefined &&
          res.totalRecordCount !== undefined
        ) {
          setToRecordCount(res.toRecordCount);
          setFromRecordCount(res.fromRecordCount);
          setTotalRecordCount(res.totalRecordCount);
        }
        if (res.menuList) {
          const menuList = res.menuList.map((item) => ({
            id: item.id,
            dictEntry: item.dictEntry,
            localAbbreviation: item.localAbbreviation,
            isActive: item.isActive,
            categoryName: item.dictionaryCategory
              ? item.dictionaryCategory.categoryName
              : "not available",
            lastupdated: item.lastupdated,
          }));
          setSearchedMenuList(menuList);
        }
      }
    }
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/DictionaryMenu", fetchedDictionaryMenu);
    return () => {
      componentMounted.current = false;
    };
  }, []);

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer(
      "/rest/dictionary-categories",
      fetchedDictionaryCategory,
    );
    return () => {
      componentMounted.current = false;
    };
  }, []);

  const postData = {
    id: dictionaryNumber,
    selectedDictionaryCategoryId: category?.id,
    dictEntry: dictionaryEntry,
    localAbbreviation: localAbbreviation,
    isActive: isActive.id,
  };

  async function displayStatus(res) {
    setNotificationVisible(true);
    if (res.status == "201" || res.status == "200") {
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "success.add.edited.msg" }),
      });
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "error.add.edited.msg" }),
      });
    }
    reloadConfiguration();
  }

  const handleSubmitModal = (e) => {
    e.preventDefault();
    postToOpenElisServerFullResponse(
      "/rest/Dictionary",
      JSON.stringify(postData),
      displayStatus,
    );
    setOpen(false);
  };

  const handleUpdateModal = (e) => {
    e.preventDefault();

    if (!componentMounted.current[dictionaryEntry]) {
      dirtyFieldsRef.current.add("dictEntry");
    }

    if (!componentMounted.current[isActive]) {
      dirtyFieldsRef.current.add("isActive");
    }

    if (!componentMounted.current[localAbbreviation]) {
      dirtyFieldsRef.current.add("localAbbreviation");
    }

    const dirtyFields =
      dirtyFieldsRef.current.size > 0
        ? `;${[...dirtyFieldsRef.current].join(";")}`
        : "";

    const updateData = {
      id: dictionaryNumber,
      selectedDictionaryCategoryId: category.id,
      dictEntry: dictionaryEntry,
      localAbbreviation: localAbbreviation,
      isActive: isActive.id,
      dirtyFormFields: dirtyFields,
    };

    postToOpenElisServerFullResponse(
      `/rest/Dictionary?ID=${selectedRowId}&startingRecNo=${startingRecNo}`,
      JSON.stringify(updateData),
      displayStatus,
    );
    setOpen(false);
  };

  const renderCell = (cell, row) => {
    if (cell.info.header === "select") {
      return (
        <TableSelectRow
          key={cell.id}
          id={cell.id}
          radio={true}
          checked={selectedRowId === row.id}
          name="selectRowRadio"
          ariaLabel="selectRow"
          onSelect={() => {
            const isActiveCell = row.cells.find((cell) =>
              cell.id.endsWith(":isActive"),
            );

            let isActiveValue = "";
            if (isActiveCell) {
              isActiveValue = isActiveCell.value;
            }

            setModifyButton(false);
            setSelectedRowId(row.id);

            setDeactivateButton(isActiveValue !== "Y");
          }}
        />
      );
    } else if (
      cell.info.header === "value" &&
      typeof cell.value === "string" &&
      cell.value.startsWith("data:image")
    ) {
      return (
        <TableCell key={cell.id}>
          <img
            src={cell.value}
            alt="Config Image"
            style={{ maxWidth: "50px" }}
          />
        </TableCell>
      );
    }
    return <TableCell key={cell.id}>{cell.value}</TableCell>;
  };

  const handleDictionaryMenuItems = (res) => {
    if (componentMounted.current) {
      setDictionaryNumber(res.id);
      setCategory(res.dictionaryCategory);
      setDictionaryEntry(res.dictEntry);
      setIsActive(yesOrNo.find((item) => item.id === res.isActive));
      setLocalAbbreviation(res.localAbbreviation);
    }
  };

  const handleOnClickOnModification = async (event) => {
    event.preventDefault();
    if (selectedRowId) {
      const selectedItem = dictionaryMenuList.find(
        (item) => item.id === selectedRowId,
      );

      if (selectedItem) {
        setDictionaryNumber(selectedItem.id);
        setCategory(selectedItem.category);
        setDictionaryEntry(selectedItem.dictEntry);
        setLocalAbbreviation(selectedItem.localAbbreviation);
        setIsActive(yesOrNo.find((item) => item.id === selectedItem.isActive));
        setOpen(true);
        setEditMode(false);
      }

      getFromOpenElisServer(
        `/rest/Dictionary?ID=${selectedRowId}&startingRecNo=${startingRecNo}`,
        handleDictionaryMenuItems,
      );
      setOpen(true);
      setEditMode(false);
    }
  };

  const handleDeactivation = async (event) => {
    event.preventDefault();
    const list = [...dictionaryMenuList];
    list.splice(selectedRowId, 1);
    setDictionaryMenuList(list);
    if (selectedRowId) {
      postToOpenElisServer(
        `/rest/delete-dictionary?selectedIDs=${selectedRowId}`,
        {},
        handleDelete,
      );
    }
    reloadConfiguration();
  };

  const handleDelete = (status) => {
    setNotificationVisible(true);
    if (status == "200") {
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "dictionary.menu.deactivate.success",
        }),
      });
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "dictionary.menu.deactivate.fail" }),
      });
    }
  };

  const handlePanelSearchChange = (event) => {
    const query = event.target.value;
    setPanelSearchTerm(query);
    if (query) {
      setIsSearching(true);
    } else {
      setIsSearching(false);
    }
  };

  return (
    <div className="adminPageContent">
      {notificationVisible === true ? <AlertDialog /> : ""}
      <PageBreadCrumb
        breadcrumbs={[
          { label: "home.label", link: "/" },
          { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
          {
            label: "dictionary.label.modify",
            link: "/MasterListsPage#DictionaryManagement",
          },
        ]}
      />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              <FormattedMessage id="dictionary.label.modify" />
            </Heading>
          </Section>
          <br />
          <Section>
            <Form
              style={{
                display: "flex",
                flexDirection: isMobile ? "column" : "row",
                gap: isMobile ? "1rem" : "2rem",
                justifyContent: "space-between",
                alignItems: isMobile ? "stretch" : "center",
                flexWrap: "wrap",
              }}
            >
              <Column
                lg={16}
                md={8}
                sm={4}
                style={{
                  display: "flex",
                  gap: isMobile ? "0.75rem" : "0.5rem",
                  flexDirection: isMobile ? "column" : "row",
                  width: isMobile ? "100%" : "auto",
                  margin: "0",
                }}
              >
                <Button
                  style={{ width: isMobile ? "100%" : "auto" }}
                  disabled={!editMode}
                  onClick={() => setOpen(true)}
                >
                  {intl.formatMessage({
                    id: "admin.page.configuration.formEntryConfigMenu.button.add",
                  })}
                </Button>
                <Button
                  style={{ width: isMobile ? "100%" : "auto" }}
                  disabled={modifyButton}
                  type="submit"
                  onClick={handleOnClickOnModification}
                >
                  <FormattedMessage id="admin.page.configuration.formEntryConfigMenu.button.modify" />
                </Button>
                <Modal
                  open={open}
                  size="sm"
                  onRequestClose={() => setOpen(false)}
                  modalHeading={editMode ? "Add Dictionary" : "Edit Dictionary"}
                  primaryButtonText={editMode ? "Add" : "Update"}
                  secondaryButtonText="Cancel"
                  onRequestSubmit={
                    editMode ? handleSubmitModal : handleUpdateModal
                  }
                >
                  <TextInput
                    data-modal-primary-focus
                    id="dictNumber"
                    labelText="Dictionary Number"
                    disabled
                    onChange={(e) => setDictionaryNumber(e.target.value)}
                    style={{
                      marginBottom: "1rem",
                    }}
                  />
                  <Dropdown
                    id="description"
                    label=""
                    type="default"
                    items={categoryDescription}
                    titleText="Dictionary Category"
                    itemToString={(item) => (item ? item.description : "")}
                    onChange={({ selectedItem }) => {
                      setCategory(selectedItem);
                    }}
                    selectedItem={category}
                    size="md"
                    style={{
                      marginBottom: "1rem",
                    }}
                  />
                  <TextInput
                    id="dictEntry"
                    labelText="Dictionary Entry"
                    value={dictionaryEntry}
                    onChange={(e) => setDictionaryEntry(e.target.value)}
                    style={{
                      marginBottom: "1rem",
                    }}
                  />
                  <Dropdown
                    id="isActive"
                    type="default"
                    label=""
                    items={yesOrNo}
                    titleText="Is Active"
                    itemToString={(item) => (item ? item.id : "")}
                    onChange={({ selectedItem }) => {
                      setIsActive(selectedItem);
                    }}
                    selectedItem={isActive}
                    size="md"
                    style={{
                      marginBottom: "1rem",
                    }}
                  />
                  <TextInput
                    id="localAbbrev"
                    labelText="Local Abbreviation"
                    value={localAbbreviation}
                    onChange={(e) => setLocalAbbreviation(e.target.value)}
                    style={{
                      marginBottom: "1rem",
                    }}
                  />
                </Modal>
                <Button
                  style={{ width: isMobile ? "100%" : "auto" }}
                  disabled={modifyButton || deactivateButton}
                  onClick={handleDeactivation}
                  type="submit"
                >
                  <FormattedMessage id="admin.page.configuration.formEntryConfigMenu.button.deactivate" />
                </Button>
              </Column>
              <Column
                lg={16}
                md={8}
                sm={4}
                style={{
                  display: "flex",
                  flexDirection: isMobile ? "column" : "row",
                  alignItems: "center",
                  justifyContent: "center",
                  gap: isMobile ? "0.75rem" : "0.5rem",
                }}
              >
                <h4
                  style={{
                    margin: 0,
                    fontSize: isMobile ? "1.2rem" : "1.2rem",
                    textAlign: isMobile ? "center" : "left",
                  }}
                >
                  Showing {fromRecordCount} - {toRecordCount} of{" "}
                  {totalRecordCount}
                </h4>
                <div
                  style={{
                    display: "flex",
                    gap: "0.5rem",
                    alignItems: "center",
                    justifyContent: "center",
                  }}
                >
                  <Button
                    style={{
                      minWidth: isMobile ? "2rem" : "2.5rem",
                      minHeight: isMobile ? "2rem" : "2.5rem",
                      padding: "0.5rem",
                    }}
                    hasIconOnly
                    iconDescription="previous"
                    disabled={parseInt(fromRecordCount) <= 1}
                    onClick={handlePreviousPage}
                    renderIcon={ArrowLeft}
                  />
                  <Button
                    style={{
                      minWidth: isMobile ? "2rem" : "2.5rem",
                      minHeight: isMobile ? "2rem" : "2.5rem",
                      padding: "0.5rem",
                    }}
                    hasIconOnly
                    iconDescription="next"
                    renderIcon={ArrowRight}
                    onClick={handleNextPage}
                    disabled={
                      parseInt(toRecordCount) >= parseInt(totalRecordCount)
                    }
                  />
                </div>
              </Column>
            </Form>
          </Section>
        </Column>
      </Grid>
      <div className="orderLegendBody">
        <Grid>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Search
                size="lg"
                id="dictionary-entry-search"
                labelText={<FormattedMessage id="search.by.dictionary.entry" />}
                placeholder={intl.formatMessage({
                  id: "search.by.dictionary.entry",
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
        <Grid fullWidth={true} className="gridBoundary">
          <Column lg={16} md={8} sm={4}>
            <DataTable
              size="sm"
              rows={
                isSearching
                  ? searchedMenuList.slice(
                      (page - 1) * pageSize,
                      page * pageSize,
                    )
                  : dictionaryMenuList.slice(
                      (page - 1) * pageSize,
                      page * pageSize,
                    )
              }
              headers={[
                {
                  key: "select",
                  header: intl.formatMessage({
                    id: "admin.page.configuration.formEntryConfigMenu.select",
                  }),
                },
                {
                  key: "categoryName",
                  header: intl.formatMessage({
                    id: "dictionary.category.name",
                  }),
                },
                {
                  key: "dictEntry",
                  header: intl.formatMessage({ id: "dictionary.dictEntry" }),
                },
                {
                  key: "localAbbreviation",
                  header: intl.formatMessage({
                    id: "dictionary.category.localAbbreviation",
                  }),
                },
                {
                  key: "isActive",
                  header: intl.formatMessage({
                    id: "dictionary.category.isActive",
                  }),
                },
              ]}
              isSortable
            >
              {({ rows, headers, getHeaderProps, getTableProps }) => {
                return (
                  <TableContainer title="" description="">
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
                          <TableHeader />
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {rows.map((row) => (
                          <TableRow key={row.id}>
                            {row.cells.map((cell) => renderCell(cell, row))}
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                );
              }}
            </DataTable>
            <Pagination
              onChange={handlePageChange}
              page={page}
              pageSize={pageSize}
              pageSizes={[10, 20]}
              totalItems={
                isSearching
                  ? searchedMenuList.length
                  : dictionaryMenuList.length
              }
              forwardText={intl.formatMessage({ id: "pagination.forward" })}
              backwardText={intl.formatMessage({ id: "pagination.backward" })}
              size="sm"
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
  );
}

export default DictionaryManagement;
