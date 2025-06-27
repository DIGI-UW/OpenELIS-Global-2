import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Heading,
  Button,
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
  Modal,
  TextInput,
  Dropdown,
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
    label: "provider.browse.title",
    link: "/MasterListsPage#providerMenu",
  },
];
function ProviderMenu() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { reloadConfiguration } = useContext(ConfigurationContext);

  const intl = useIntl();

  const componentMounted = useRef(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [modifyButton, setModifyButton] = useState(true);
  const [deactivateButton, setDeactivateButton] = useState(true);
  const [selectedRowIds, setSelectedRowIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isSearching, setIsSearching] = useState(false);
  const [panelSearchTerm, setPanelSearchTerm] = useState("");
  const [startingRecNo, setStartingRecNo] = useState(1);
  const [providerMenuList, setProviderMenuList] = useState({});
  const [providerMenuListShow, setProviderMenuListShow] = useState([]);
  const [fromRecordCount, setFromRecordCount] = useState("");
  const [toRecordCount, setToRecordCount] = useState("");
  const [totalRecordCount, setTotalRecordCount] = useState("");
  const [paging, setPaging] = useState(1);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [isUpdateModalOpen, setIsUpdateModalOpen] = useState(false);
  const [currentProvider, setCurrentProvider] = useState(null);
  const [lastName, setLastName] = useState("");
  const [firstName, setFirstName] = useState("");
  const [telephone, setTelephone] = useState("");
  const [fhirUuid, setFhirUuid] = useState("");
  const [fax, setFax] = useState("");
  const [isActive, setIsActive] = useState({ id: "yes", value: "Yes" });

  const yesOrNo = [
    { id: "yes", value: "Yes" },
    { id: "no", value: "No" },
  ];

  const handleMenuItems = (res) => {
    if (!res) {
      setLoading(true);
    } else {
      setProviderMenuList(res);
    }
  };

  useEffect(() => {
    componentMounted.current = true;
    setLoading(true);
    getFromOpenElisServer(
      `/rest/ProviderMenu?paging=${paging}&startingRecNo=${startingRecNo}`,
      handleMenuItems,
    );
    return () => {
      componentMounted.current = false;
      setLoading(false);
    };
  }, [paging, startingRecNo]);

  const handleSearchedProviderMenuList = (res) => {
    if (res) {
      setProviderMenuList(res);
    }
  };

  useEffect(() => {
    getFromOpenElisServer(
      `/rest/SearchProviderMenu?search=Y&startingRecNo=${startingRecNo}&searchString=${panelSearchTerm}`,
      handleSearchedProviderMenuList,
    );
  }, [panelSearchTerm]);

  useEffect(() => {
    if (providerMenuList.providers) {
      const newProviderMenuList = providerMenuList.providers.map((item) => {
        return {
          id: item.id,
          fhirUuid: item.fhirUuid,
          lastName: item.person.lastName,
          firstName: item.person.firstName,
          active: item.active,
          telephone: item.person.workPhone,
          fax: item.person.fax,
        };
      });
      setFromRecordCount(providerMenuList.fromRecordCount);
      setToRecordCount(providerMenuList.toRecordCount);
      setTotalRecordCount(providerMenuList.totalRecordCount);
      setProviderMenuListShow(newProviderMenuList);
    }
  }, [providerMenuList]);

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

  useEffect(() => {
    if (isSearching && panelSearchTerm === "") {
      setIsSearching(false);
      setPaging(1);
      setStartingRecNo(1);
    }
  }, [isSearching, panelSearchTerm]);

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

  function deleteDeactivateProvider(event) {
    event.preventDefault();
    setLoading(true);
    postToOpenElisServerFullResponse(
      `/rest/DeleteProvider?ID=${selectedRowIds.join(",")}&${startingRecNo}=1`,
      providerMenuListShow,
      setLoading(false),
      setTimeout(() => {
        window.location.reload();
      }, 1),
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
    setSelectedRowIds([]);
  };

  const handlePreviousPage = () => {
    setPaging((pager) => Math.max(pager - 1, 1));
    setStartingRecNo(Math.max(fromRecordCount, 1));
    setSelectedRowIds([]);
  };

  const handlePanelSearchChange = (event) => {
    setIsSearching(true);
    setPaging(1);
    setStartingRecNo(1);
    const query = event.target.value.toLowerCase();
    setPanelSearchTerm(query);
    setSelectedRowIds([]);
  };

  const openAddModal = () => {
    setLastName("");
    setFirstName("");
    setTelephone("");
    setFax("");
    setIsActive({ id: "yes", value: "Yes" });
    setIsAddModalOpen(true);
  };

  const closeAddModal = () => {
    setIsAddModalOpen(false);
  };

  const openUpdateModal = (providerId) => {
    const provider = providerMenuListShow.find((p) => p.id === providerId);
    setCurrentProvider(provider);
    setLastName(provider.lastName);
    setFirstName(provider.firstName);
    setTelephone(provider.telephone);
    setFax(provider.fax);
    setIsActive(
      provider.active ? { id: "yes", value: "Yes" } : { id: "no", value: "No" },
    );
    setIsUpdateModalOpen(true);
  };

  const closeUpdateModal = () => {
    setIsUpdateModalOpen(false);
  };

  const handleAddProvider = () => {
    const newProvider = {
      person: {
        lastName,
        firstName,
        workPhone: telephone,
        fax,
      },
      active: isActive.id === "yes",
    };
    postToOpenElisServerFullResponse(
      "/rest/Provider/FhirUuid?fhirUuid=",
      JSON.stringify(newProvider),
      displayStatus,
    );

    closeAddModal();
    window.location.reload();
  };

  const handleUpdateProvider = () => {
    const updatedProvider = {
      fhirUuid: currentProvider.fhirUuid,
      person: {
        lastName,
        firstName,
        workPhone: telephone,
        fax,
      },
      active: isActive.id === "yes",
    };
    postToOpenElisServerFullResponse(
      "/rest/Provider/FhirUuid?fhirUuid=" + currentProvider.fhirUuid,
      JSON.stringify(updatedProvider),
      displayStatus,
    );

    closeUpdateModal();
    window.location.reload();
  };

  const handleLastNameChange = (event) => {
    const value = event.target.value;
    if (value === "" || /^[A-Za-z\s]+$/.test(value)) {
      setLastName(value);
    }
  };

  const handleFirstNameChange = (event) => {
    const value = event.target.value;
    if (value === "" || /^[A-Za-z\s]+$/.test(value)) {
      setFirstName(value);
    }
  };

  const handleTelephoneChange = (event) => {
    const value = event.target.value;
    if (value === "" || (/^\d+$/.test(value) && value.length <= 10)) {
      setTelephone(value);
    }
  };

  const handleFaxChange = (event) => {
    const value = event.target.value;
    if (value === "" || (/^\d+$/.test(value) && value.length <= 10)) {
      setFax(value);
    }
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
          onSelect={(e) => {
            e.stopPropagation();
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
                <FormattedMessage id="provider.browse.title" />
              </Heading>
            </Section>
          </Column>
        </Grid>
        <br />
        <ActionPaginationButtonType
          selectedRowIds={selectedRowIds}
          modifyButton={modifyButton}
          deactivateButton={deactivateButton}
          deleteDeactivate={deleteDeactivateProvider}
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
          modalHeading="Add Provider"
          primaryButtonText="Add"
          secondaryButtonText="Cancel"
          onRequestSubmit={handleAddProvider}
          onRequestClose={closeAddModal}
        >
          <TextInput
            id="lastName"
            labelText={intl.formatMessage({ id: "provider.providerLastName" })}
            value={lastName}
            onChange={(e) => handleLastNameChange(e)}
            required
          />
          <TextInput
            id="firstName"
            labelText={intl.formatMessage({ id: "provider.providerFirstName" })}
            value={firstName}
            onChange={(e) => handleFirstNameChange(e)}
            required
          />
          <TextInput
            id="telephone"
            labelText={intl.formatMessage({ id: "provider.telephone" })}
            value={telephone}
            onChange={(e) => handleTelephoneChange(e)}
          />

          <Dropdown
            className="dropdown-list"
            id="isActive"
            titleText="Active"
            label={intl.formatMessage({ id: "provider.select" })}
            items={yesOrNo}
            itemToString={(item) => (item ? item.value : "")}
            selectedItem={isActive}
            onChange={({ selectedItem }) => setIsActive(selectedItem)}
          />
          <TextInput
            id="fax"
            labelText={intl.formatMessage({ id: "provider.fax" })}
            value={fax}
            onChange={(e) => setFax(e.target.value)}
          />
        </Modal>

        <Modal
          open={isUpdateModalOpen}
          modalHeading="Update Provider"
          primaryButtonText="Update"
          secondaryButtonText="Cancel"
          onRequestSubmit={handleUpdateProvider}
          onRequestClose={closeUpdateModal}
        >
          <TextInput
            id="lastName"
            labelText={intl.formatMessage({ id: "provider.providerLastName" })}
            value={lastName}
            onChange={(e) => handleLastNameChange(e)}
            required
          />
          <TextInput
            id="firstName"
            labelText={intl.formatMessage({ id: "provider.providerFirstName" })}
            value={firstName}
            onChange={(e) => handleFirstNameChange(e)}
            required
          />
          <TextInput
            id="telephone"
            labelText={intl.formatMessage({ id: "provider.telephone" })}
            value={telephone}
            onChange={(e) => handleTelephoneChange(e)}
          />
          <Dropdown
            id="isActive"
            titleText="Active"
            label={intl.formatMessage({ id: "provider.select" })}
            items={yesOrNo}
            itemToString={(item) => (item ? item.value : "")}
            selectedItem={isActive}
            onChange={({ selectedItem }) => setIsActive(selectedItem)}
          />
          <TextInput
            id="fax"
            labelText={intl.formatMessage({ id: "provider.fax" })}
            value={fax}
            onChange={(e) => setFax(e.target.value)}
          />
        </Modal>

        <div className="orderLegendBody">
          <Grid>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Search
                  size="lg"
                  id="provider-search-bar"
                  labelText={<FormattedMessage id="provider.search" />}
                  placeholder={intl.formatMessage({
                    id: "provider.search.placeholder",
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
          <>
            <Grid fullWidth={true} className="gridBoundary">
              <Column lg={16} md={8} sm={4}>
                <DataTable
                  rows={providerMenuListShow.slice(
                    (page - 1) * pageSize,
                    page * pageSize,
                  )}
                  headers={[
                    {
                      key: "select",
                      header: intl.formatMessage({
                        id: "provider.select",
                      }),
                    },
                    {
                      key: "lastName",
                      header: intl.formatMessage({
                        id: "provider.providerLastName",
                      }),
                    },

                    {
                      key: "firstName",
                      header: intl.formatMessage({
                        id: "provider.providerFirstName",
                      }),
                    },
                    {
                      key: "active",
                      header: intl.formatMessage({
                        id: "provider.isActive",
                      }),
                    },
                    {
                      key: "telephone",
                      header: intl.formatMessage({
                        id: "provider.telephone",
                      }),
                    },
                    {
                      key: "fax",
                      header: intl.formatMessage({
                        id: "provider.fax",
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
                                  setSelectedRowIds(
                                    selectedRowIds.includes(id)
                                      ? selectedRowIds.filter(
                                          (selectedId) => selectedId !== id,
                                        )
                                      : [...selectedRowIds, id],
                                  );
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
                  totalItems={providerMenuListShow.length}
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

export default injectIntl(ProviderMenu);
