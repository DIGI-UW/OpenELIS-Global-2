import React, { useContext, useState, useEffect } from "react";
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
  Search,
} from "@carbon/react";
import { getFromOpenElisServer } from "../../../utils/Utils";
import { useServerData } from "../../../utils/useServerData";
import { NotificationContext } from "../../../layout/Layout";
import { AlertDialog } from "../../../common/CustomNotification";
import { FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../../../common/PageBreadCrumb";
import GenericConfigEdit from "../../generalConfig/common/GenericConfigEdit";
import ValidationPolicySection, {
  VALIDATION_POLICY_API,
} from "./ValidationPolicySection";
import { settingDescription, settingName } from "./settingLabels";
import { matchesSearch } from "../../../common/searchText";
import FacilityLocationSection from "../facilityLocation/FacilityLocationSection";

interface ConfigMenuDisplayProps {
  id: string;
  menuType: string;
}

interface ConfigLocalization {
  localesAndValuesOfLocalesWithValues?: string;
}

const RESULT_CONFIGURATION_MENU = "ResultConfigurationMenu";
const REPLACED_BY_VALIDATION_POLICY = "validate all results";

interface ValidationPolicy {
  enabled: boolean;
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

function ConfigMenuDisplayContent(props: ConfigMenuDisplayProps) {
  const { notificationVisible } = useContext(
    NotificationContext,
  ) as NotificationContextValue;

  const intl = useIntl();

  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(30);
  const [modifyButton, setModifyButton] = useState(true);
  const [selectedRowId, setSelectedRowId] = useState<string | null>(null);
  // eslint-disable-next-line @typescript-eslint/no-unused-vars -- preserve the original state tuple
  const [startingRecNo, setStartingRecNo] = useState(1);
  const [orderEntryConfigurationList, setOrderEntryConfigurationList] =
    useState<ConfigTableRow[]>([]);

  const [ConfigEdit, setConfigEdit] = useState(false);
  const [query, setQuery] = useState("");

  // eslint-disable-next-line prefer-const -- preserve the original JavaScript runtime declaration
  let breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
    { label: props.id, link: `/MasterListsPage/${props.menuType}` },
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

  const { data: formEntryConfigMenuList } = useServerData<ConfigMenuResponse>(
    `/rest/${props.menuType}`,
  );
  const { data: validationPolicy } = useServerData<ValidationPolicy>(
    props.menuType === RESULT_CONFIGURATION_MENU ? VALIDATION_POLICY_API : null,
  );
  const validationPolicyEnabled = Boolean(validationPolicy?.enabled);

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

  const shownSettings = orderEntryConfigurationList
    .filter(
      (item) =>
        !(
          validationPolicyEnabled && item.name === REPLACED_BY_VALIDATION_POLICY
        ),
    )
    .map((item) => ({
      ...item,
      name: settingName(intl, item.name),
      description: settingDescription(intl, item.name, item.description),
    }))
    .filter((item) =>
      [
        item.name,
        orderEntryConfigurationList.find((setting) => setting.id === item.id)
          ?.name,
        item.description,
        item.value,
      ].some((text) => matchesSearch(text, query)),
    );
  const selectionShown = shownSettings.some(
    (item) => String(item.id) === selectedRowId,
  );

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
          aria-label="selectRow"
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
          onDone={() => setConfigEdit(false)}
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
                {validationPolicyEnabled && (
                  <Section>
                    <ValidationPolicySection policy={validationPolicy} />
                    <br />
                  </Section>
                )}
                {props.menuType === "SiteInformationMenu" && (
                  <Section>
                    <FacilityLocationSection />
                    <br />
                  </Section>
                )}
                <Section>
                  <Form onSubmit={handleModify}>
                    <Column lg={16} md={8} sm={4}>
                      <Button
                        data-cy="modify-Button"
                        disabled={modifyButton || !selectionShown}
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
                  <Search
                    id="config-menu-search"
                    size="lg"
                    labelText={intl.formatMessage({
                      id: "admin.page.configuration.search",
                    })}
                    placeholder={intl.formatMessage({
                      id: "admin.page.configuration.search",
                    })}
                    value={query}
                    onChange={(event) => {
                      setQuery(event.target.value);
                      setPage(1);
                    }}
                  />
                  <DataTable
                    rows={shownSettings.slice(
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
                        {query && shownSettings.length === 0 && (
                          <p className="cds--label">
                            <FormattedMessage
                              id="searchableList.noMatch"
                              values={{ query }}
                            />
                          </p>
                        )}
                      </TableContainer>
                    )}
                  </DataTable>
                  <Pagination
                    onChange={handlePageChange}
                    page={page}
                    pageSize={pageSize}
                    pageSizes={[5, 20, 30, 50]}
                    totalItems={shownSettings.length}
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

export default function ConfigMenuDisplay(props: ConfigMenuDisplayProps) {
  return <ConfigMenuDisplayContent key={props.menuType} {...props} />;
}
