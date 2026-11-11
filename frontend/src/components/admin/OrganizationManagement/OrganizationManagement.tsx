import React, { useContext, useState, useEffect } from "react";
import {
  DEFAULT_SERVER_PAGE_SIZE,
  serverPageSizeFrom,
  startingRecNoFor,
} from "../../utils/offsetPaging";
import { serverPageArrowsProps } from "../../utils/serverPaging";
import ServerPageArrows from "../../common/ServerPageArrows";
import type { ChangeEvent, FormEvent, ReactNode } from "react";
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

interface OrganizationMenuItem {
  id: string;
  organizationName: string;
  organization?: { organizationName?: string };
  shortName?: string;
  isActive?: boolean | string;
  internetAddress?: string;
  streetAddress?: string;
  city?: string;
  cliaNum?: string;
}

interface OrganizationMenuResponse {
  menuList: OrganizationMenuItem[];
  fromRecordCount: string;
  toRecordCount: string;
  totalRecordCount: string;
}

interface OrganizationTableRow {
  id: string;
  orgName: string;
  parentOrg: string;
  orgPrefix: string;
  active: boolean | string;
  internetAddress: string;
  streetAddress: string;
  city: string;
  cliaNumber: string;
}

interface CarbonTableCell {
  id: string;
  value: ReactNode;
  info: { header: string };
}

interface CarbonTableRow {
  id: string;
}

interface NotificationContextValue {
  notificationVisible: boolean;
  setNotificationVisible: (visible: boolean) => void;
  addNotification: (notification: {
    kind: string;
    title: string;
    message: string;
  }) => void;
}

// eslint-disable-next-line prefer-const -- preserve the original JavaScript runtime declaration
let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "organization.main.title",
    link: "/MasterListsPage/organizationManagement",
  },
];

function OrganizationManagement() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext) as NotificationContextValue;

  const intl = useIntl();

  const [page, setPage] = useState(1);
  const [deactivateButton, setDeactivateButton] = useState(true);
  const [modifyButton, setModifyButton] = useState(true);
  const [selectedRowIds, setSelectedRowIds] = useState<string[]>([]);
  const [selectedRowIdsPost, setSelectedRowIdsPost] = useState<
    string[] | { selectedIDs: string[] }
  >([]);
  const [isSearching, setIsSearching] = useState(false);
  const [panelSearchTerm, setPanelSearchTerm] = useState("");
  const [totalRecordCount, setTotalRecordCount] = useState("");
  const [fromRecordCount, setFromRecordCount] = useState("");
  const [toRecordCount, setToRecordCount] = useState("");
  const [serverPageSize, setServerPageSize] = useState(
    DEFAULT_SERVER_PAGE_SIZE,
  );
  const startingRecNo = startingRecNoFor(page, serverPageSize);
  const [organizationsManagmentListShow, setOrganizationsManagmentListShow] =
    useState<OrganizationTableRow[]>([]);

  function deleteDeactivateOrganizationManagament(
    event: FormEvent<HTMLFormElement>,
  ) {
    event.preventDefault();
    postToOpenElisServer(
      `/rest/DeleteOrganization?ID=${selectedRowIds.join(",")}&startingRecNo=1`,
      JSON.stringify(selectedRowIdsPost),
      deleteDeactivateOrganizationManagamentCallback,
    );
  }

  const handlePanelSearchChange = (event: ChangeEvent<HTMLInputElement>) => {
    setIsSearching(true);
    setPage(1);
    const query = event.target.value;
    setPanelSearchTerm(query);
    setSelectedRowIds([]);
  };

  const deleteDeactivateOrganizationManagamentCallback = (status: number) => {
    const succeeded = status >= 200 && status < 300;
    setNotificationVisible(true);
    addNotification({
      title: intl.formatMessage({
        id: "notification.title",
      }),
      message: intl.formatMessage({
        id: succeeded
          ? "notification.organization.post.delete.success"
          : "server.error.msg",
      }),
      kind: succeeded ? NotificationKinds.success : NotificationKinds.error,
    });
    if (!succeeded) return;
    setSelectedRowIds([]);
    invalidateServerData();
  };

  const handlePageChange = ({ page: newPage }: { page: number }) => {
    if (newPage !== page) {
      setPage(newPage);
      setSelectedRowIds([]);
    }
  };
  const arrows = serverPageArrowsProps({
    paging: {
      currentPage: page,
      totalPages: Math.max(
        Math.ceil((Number(totalRecordCount) || 0) / serverPageSize),
        1,
      ),
    },
    onPageRequest: (pageNumber) => handlePageChange({ page: pageNumber }),
  });

  // Browsing and searching are the same list from two endpoints, so which one
  // is read follows the search box rather than both being read at once.
  const { data: organizationsManagmentList } =
    useServerData<OrganizationMenuResponse>(
      panelSearchTerm
        ? `/rest/SearchOrganizationMenu?search=Y&startingRecNo=${startingRecNo}&searchString=${panelSearchTerm}`
        : `/rest/OrganizationMenu?startingRecNo=${startingRecNo}`,
    );
  const invalidateServerData = useInvalidateServerData();

  useEffect(() => {
    if (organizationsManagmentList?.menuList) {
      const newOrganizationsManagementList =
        organizationsManagmentList.menuList.map((item) => {
          return {
            id: item.id,
            orgName: item.organizationName,
            parentOrg: item.organization
              ? item.organization.organizationName
              : "",
            orgPrefix: item.shortName || "",
            active: item.isActive || "",
            internetAddress: item.internetAddress || "",
            streetAddress: item.streetAddress || "",
            city: item.city || "",
            cliaNumber: item.cliaNum || "",
          };
        });
      const newOrganizationsManagementListArray = Object.values(
        newOrganizationsManagementList,
      );
      setFromRecordCount(organizationsManagmentList.fromRecordCount);
      setToRecordCount(organizationsManagmentList.toRecordCount);
      setTotalRecordCount(organizationsManagmentList.totalRecordCount);
      setServerPageSize((previous) =>
        serverPageSizeFrom(
          organizationsManagmentList.fromRecordCount,
          organizationsManagmentList.toRecordCount,
          organizationsManagmentList.totalRecordCount,
          previous,
        ),
      );
      setOrganizationsManagmentListShow(newOrganizationsManagementListArray);
    }
  }, [organizationsManagmentList]);

  useEffect(() => {
    const selectedIDsObject = {
      selectedIDs: selectedRowIds,
    };

    setSelectedRowIdsPost(selectedIDsObject);
  }, [selectedRowIds, organizationsManagmentListShow]);

  useEffect(() => {
    if (selectedRowIds.length == 0) {
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
    if (isSearching && panelSearchTerm === "") {
      setIsSearching(false);
      setPage(1);
    }
  }, [isSearching, panelSearchTerm]);

  const renderCell = (cell: CarbonTableCell, row: CarbonTableRow) => {
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
    } else if (cell.info.header === "active") {
      return <TableCell key={cell.id}>{cell.value!.toString()}</TableCell>;
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
                <FormattedMessage id="organization.main.title" />
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
          deleteDeactivate={deleteDeactivateOrganizationManagament}
          id={selectedRowIds[0]}
          otherParmsInLink={`&startingRecNo=1`}
          addButtonRedirectLink={`/MasterListsPage/organizationEdit?ID=0`}
          modifyButtonRedirectLink={`/MasterListsPage/organizationEdit?ID=`}
          type="type2"
        />
        <br />
        <div className="orderLegendBody">
          <Grid>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Search
                  size="lg"
                  id="org-name-search-bar"
                  labelText={
                    <FormattedMessage id="organization.search.byorgname" />
                  }
                  placeholder={intl.formatMessage({
                    id: "organization.search.placeHolder",
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
                {arrows.show && <ServerPageArrows {...arrows} />}
                <DataTable
                  rows={organizationsManagmentListShow}
                  headers={[
                    {
                      key: "select",
                      header: intl.formatMessage({
                        id: "organization.select",
                      }),
                    },
                    {
                      key: "orgName",
                      header: intl.formatMessage({
                        id: "organization.organizationName",
                      }),
                    },

                    {
                      key: "parentOrg",
                      header: intl.formatMessage({
                        id: "organization.parent",
                      }),
                    },

                    {
                      key: "orgPrefix",
                      header: intl.formatMessage({
                        id: "organization.short.CI",
                      }),
                    },
                    {
                      key: "active",
                      header: intl.formatMessage({
                        id: "organization.isActive",
                      }),
                    },
                    {
                      key: "internetAddress",
                      header: intl.formatMessage({
                        id: "organization.internetaddress",
                      }),
                    },
                    {
                      key: "streetAddress",
                      header: intl.formatMessage({
                        id: "organization.streetAddress",
                      }),
                    },
                    {
                      key: "city",
                      header: intl.formatMessage({
                        id: "organization.city",
                      }),
                    },
                    {
                      key: "cliaNumber",
                      header: intl.formatMessage({
                        id: "organization.clia.number",
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
          </>
        </div>
      </div>
    </>
  );
}

export default injectIntl(OrganizationManagement);
