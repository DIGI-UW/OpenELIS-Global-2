import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Form,
  Heading,
  Toggle,
  Button,
  Loading,
  Grid,
  Column,
  Section,
  Checkbox,
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
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../../utils/Utils.js";
import { NotificationContext } from "../../../layout/Layout.js";
import {
  AlertDialog,
  NotificationKinds,
} from "../../../common/CustomNotification.js";
import config from "../../../../config.json";
import { FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../../../common/PageBreadCrumb.js";
import GenericConfigEdit from "../../generalConfig/common/GenericConfigEdit.js";

function ConfigMenuDisplay(props) {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const componentMounted = useRef(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(30);
  const [modifyButton, setModifyButton] = useState(true);
  const [selectedRowId, setSelectedRowId] = useState(null);
  const [startingRecNo, setStartingRecNo] = useState(1);
  const [formEntryConfigMenuList, setformEntryConfigMenuList] = useState([]);
  const [orderEntryConfigurationList, setOrderEntryConfigurationList] =
    useState([]);

  const [ConfigEdit, setConfigEdit] = useState(false);

  let breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
    { label: `${props.label}`, link: `/MasterListsPage#${props.menuType}` },
  ];

  function handleModify(event) {
    event.preventDefault();
    setConfigEdit(true);
  }

  const handlePageChange = ({ page, pageSize }) => {
    setPage(page);
    setPageSize(pageSize);
  };

  const handleMenuItems = (res) => {
    if (res) {
      setformEntryConfigMenuList(res);
    }
  };

  const handleLogoResponse = (res, item) => {
    const value = res.value;
    const updatedItem = {
      id: item.id,
      startingRecNo: startingRecNo,
      name: item.name,
      description: item.description,
      value: value,
      valueType: item.valueType,
    };

    setOrderEntryConfigurationList((prevList) => {
      const index = prevList.findIndex((prevItem) => prevItem.id === item.id);

      if (index !== -1) {
        const newList = [...prevList];
        newList[index] = updatedItem;
        return newList;
      } else {
        return [...prevList, updatedItem];
      }
    });
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer(`/rest/${props.menuType}`, handleMenuItems);
    return () => {
      componentMounted.current = false;
    };
  }, []);

  useEffect(() => {
    const updateConfigList = () => {
      if (formEntryConfigMenuList && formEntryConfigMenuList.menuList) {
        const updatedList = formEntryConfigMenuList.menuList
          .map((item) => {
            let value = item.value;
            if (item.valueType === "text" && item.tag === "localization") {
              value =
                item.localization.localesAndValuesOfLocalesWithValues || value;
            } else if (item.valueType === "logoUpload") {
              getFromOpenElisServer(
                `/dbImage/siteInformation/${item.name}`,
                (res) => {
                  handleLogoResponse(res, item);
                },
              );

              return null;
            }
            return {
              id: item.id,
              startingRecNo: startingRecNo,
              name: item.name,
              description: item.description,
              value: value,
              valueType: item.valueType,
            };
          })
          .filter(Boolean);

        setOrderEntryConfigurationList(updatedList);
      }
    };

    updateConfigList();
  }, [formEntryConfigMenuList]);

  const renderCell = (cell, row) => {
    if (cell.info.header === "select") {
      return (
        <TableSelectRow
          radio
          key={cell.id}
          id={cell.id}
          checked={selectedRowId === row.id}
          name="selectRowRadio"
          ariaLabel="selectRow"
          onSelect={() => {
            setModifyButton(false);
            setSelectedRowId(row.id);
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

  return (
    <>
      {ConfigEdit ? (
        <GenericConfigEdit
          menuType={props.menuType.substring(0, props.menuType.indexOf("Menu"))}
          ID={selectedRowId}
        />
      ) : (
        <>
          {notificationVisible === true ? <AlertDialog /> : ""}
          <div className="adminPageContent">
            <PageBreadCrumb breadcrumbs={breadcrumbs} />
            <Grid fullWidth={true}>
              <Column lg={16} md={8} sm={4}>
                <Section>
                  <Heading>
                    <FormattedMessage id={props.id} />
                  </Heading>
                </Section>
                <br />
                <Section>
                  <Form onSubmit={handleModify}>
                    <Column lg={16} md={8} sm={4}>
                      <Button disabled={modifyButton} type="submit">
                        <FormattedMessage id="admin.page.configuration.formEntryConfigMenu.button.modify" />
                      </Button>{" "}
                      <Button kind="tertiary" disabled={true} type="submit">
                        <FormattedMessage id="admin.page.configuration.formEntryConfigMenu.button.deactivate" />
                      </Button>{" "}
                      <Button kind="tertiary" disabled={true} type="submit">
                        <FormattedMessage id="admin.page.configuration.formEntryConfigMenu.button.add" />
                      </Button>
                    </Column>
                  </Form>
                </Section>
              </Column>
            </Grid>
            <div className="orderLegendBody">
              <Grid fullWidth={true} className="gridBoundary">
                <Column lg={16} md={8} sm={4}>
                  <DataTable
                    rows={orderEntryConfigurationList.slice(
                      (page - 1) * pageSize,
                      page * pageSize,
                    )}
                    headers={[
                      {
                        key: "select",
                        header: intl.formatMessage({
                          id: "admin.page.configuration.formEntryConfigMenu.select",
                        }),
                      },
                      {
                        key: "name",
                        header: intl.formatMessage({
                          id: "admin.page.configuration.formEntryConfigMenu.name",
                        }),
                      },

                      {
                        key: "description",
                        header: intl.formatMessage({
                          id: "admin.page.configuration.formEntryConfigMenu.description",
                        }),
                      },
                      {
                        key: "value",
                        header: intl.formatMessage({
                          id: "admin.page.configuration.formEntryConfigMenu.value",
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
                            <>
                              {rows.map((row) => (
                                <TableRow
                                  key={row.id}
                                  onClick={() => {
                                    setSelectedRowId(row.id);
                                  }}
                                >
                                  {row.cells.map((cell) =>
                                    renderCell(cell, row),
                                  )}
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
                    pageSizes={[5, 20, 30, 50]}
                    totalItems={orderEntryConfigurationList.length}
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
      )}
    </>
  );
}

export default ConfigMenuDisplay;
