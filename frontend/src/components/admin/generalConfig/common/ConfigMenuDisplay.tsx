import React, { useContext, useState, useEffect, useRef } from "react";
import type { FormEvent, ReactNode } from "react";
import {
  Form,
  Heading,
  Button,
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
} from "@carbon/react";
import { getFromOpenElisServer } from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";
import { AlertDialog } from "../../../common/CustomNotification";
import { FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../../../common/PageBreadCrumb";
import GenericConfigEdit from "../../generalConfig/common/GenericConfigEdit";

interface ConfigMenuDisplayProps {
  id: string;
  label: string;
  menuType: string;
}

interface ConfigLocalization {
  localesAndValuesOfLocalesWithValues?: string;
}

interface ConfigMenuItem {
  id: string;
  name: string;
  description: string;
  value: string;
  valueType: string;
  tag?: string;
  localization?: ConfigLocalization;
}

interface ConfigMenuResponse {
  menuList: ConfigMenuItem[];
}

interface ConfigTableRow {
  id: string;
  startingRecNo: number;
  name: string;
  description: string;
  value: string;
  valueType: string;
}

interface CarbonTableCell {
  id: string;
  value: string;
  info: { header: string };
}

interface CarbonTableRow {
  id: string;
}

interface NotificationContextValue {
  notificationVisible: boolean;
}

function ConfigMenuDisplay(props: ConfigMenuDisplayProps) {
  const { notificationVisible } = useContext(
    NotificationContext,
  ) as NotificationContextValue;

  const intl = useIntl();

  const componentMounted = useRef(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(30);
  const [modifyButton, setModifyButton] = useState(true);
  const [selectedRowId, setSelectedRowId] = useState<string | null>(null);
  // eslint-disable-next-line @typescript-eslint/no-unused-vars -- preserve the original state tuple
  const [startingRecNo, setStartingRecNo] = useState(1);
  const [formEntryConfigMenuList, setformEntryConfigMenuList] = useState<
    ConfigMenuResponse | []
  >([]);
  const [orderEntryConfigurationList, setOrderEntryConfigurationList] =
    useState<ConfigTableRow[]>([]);

  const [ConfigEdit, setConfigEdit] = useState(false);

  // eslint-disable-next-line prefer-const -- preserve the original JavaScript runtime declaration
  let breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
    { label: `${props.label}`, link: `/MasterListsPage/${props.menuType}` },
  ];

  function handleModify(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setConfigEdit(true);
  }

  const handlePageChange = ({
    page,
    pageSize,
  }: {
    page: number;
    pageSize: number;
  }) => {
    setPage(page);
    setPageSize(pageSize);
  };

  const handleMenuItems = (res?: ConfigMenuResponse) => {
    if (res) {
      setformEntryConfigMenuList(res);
    }
  };

  const handleLogoResponse = (res: { value: string }, item: ConfigMenuItem) => {
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
      if (
        formEntryConfigMenuList &&
        (formEntryConfigMenuList as ConfigMenuResponse).menuList
      ) {
        const updatedList = (
          formEntryConfigMenuList as ConfigMenuResponse
        ).menuList
          .map((item) => {
            let value = item.value;
            if (item.valueType === "text" && item.tag === "localization") {
              value =
                item.localization.localesAndValuesOfLocalesWithValues || value;
            } else if (item.valueType === "logoUpload") {
              getFromOpenElisServer(
                `/dbImage/siteInformation/${item.name}`,
                (res: { value: string }) => {
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

  const renderCell = (
    cell: CarbonTableCell,
    row: CarbonTableRow,
  ): ReactNode => {
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
          ID={selectedRowId as string}
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
                      <Button
                        data-cy="modify-Button"
                        disabled={modifyButton}
                        type="submit"
                      >
                        <FormattedMessage id="admin.page.configuration.formEntryConfigMenu.button.modify" />
                      </Button>{" "}
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
                                <TableRow key={row.id}>
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
