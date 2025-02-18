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
  Checkbox,
  Pagination,
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils.js";
import {
  NotificationContext,
} from "../../layout/Layout.js";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification.js";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb.js";
import { Settings } from "@carbon/icons-react";
import "./TestNotificationConfigMenu.css";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "testnotificationconfig.browse.title",
    link: "/MasterListsPage#testNotificationConfigMenu",
  },
];

function TestNotificationConfigMenu() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();
  const componentMounted = useRef(false);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);
  const [loading, setLoading] = useState(true);
  const [saveButton, setSaveButton] = useState(true);
  const [testNamesList, setTestNamesList] = useState([]);
  const [testNotificationConfigMenuData, setTestNotificationConfigMenuData] =
    useState({});
  const [
    testNotificationConfigMenuDataPost,
    setTestNotificationConfigMenuDataPost,
  ] = useState({ menuList: [] });
  const [testNamesMap, setTestNamesMap] = useState({});
  const [windowWidth, setWindowWidth] = useState(
    typeof window !== 'undefined' ? window.innerWidth : 0
  );
  
  const isMobile = windowWidth < 672;
  const isTablet = windowWidth >= 672 && windowWidth < 1056;
  const getResponsivePageSizes = () => {
    if (isMobile) {
      return [10, 25]; 
    } else if (isTablet) {
      return [25, 50]; 
    } else {
      return [25, 50, 100]; 
    }
  };

  // Window resize handler
  useEffect(() => {
    function handleResize() {
      setWindowWidth(window.innerWidth);
    }
    
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const handleMenuItems = (res) => {
    if (res) {
      setTestNotificationConfigMenuData(res);
    }
    setLoading(false);
  };

  const handleTestNamesList = (res) => {
    if (res) {
      setTestNamesList(res);
    }
    setLoading(false);
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer(`/rest/TestNotificationConfigMenu`, handleMenuItems);
    getFromOpenElisServer(`/rest/test-list`, handleTestNamesList);
    return () => {
      componentMounted.current = false;
    };
  }, []);

  useEffect(() => {
    if (
      testNotificationConfigMenuData &&
      testNotificationConfigMenuData.menuList
    ) {
      setTestNotificationConfigMenuDataPost((prevData) => ({
        ...prevData,
        formMethod: testNotificationConfigMenuData.formMethod,
        cancelAction: testNotificationConfigMenuData.cancelAction,
        submitOnCancel: testNotificationConfigMenuData.submitOnCancel,
        cancelMethod: testNotificationConfigMenuData.cancelMethod,
        adminMenuItems: testNotificationConfigMenuData.adminMenuItems,
        totalRecordCount: testNotificationConfigMenuData.totalRecordCount,
        fromRecordCount: testNotificationConfigMenuData.fromRecordCount,
        toRecordCount: testNotificationConfigMenuData.toRecordCount,
        selectedIDs: testNotificationConfigMenuData.selectedIDs,
        menuList: testNotificationConfigMenuData.menuList,
      }));
    }
  }, [testNotificationConfigMenuData]);

  useEffect(() => {
    const map = testNamesList.reduce((acc, item) => {
      acc[item.id] = item.value;
      return acc;
    }, {});
    setTestNamesMap(map);
  }, [testNamesList]);

  const handleEditButtonClick = (id) => {
    window.location.assign(
      `/MasterListsPage#testNotificationConfig?testId=${id}`,
    );
  };

  function testNotificationConfigMenuSavePostCall() {
    setLoading(true);
    postToOpenElisServerJsonResponse(
      `/rest/TestNotificationConfigMenu`,
      JSON.stringify(testNotificationConfigMenuDataPost),
      (res) => {
        if (res) {
          addNotification({
            title: intl.formatMessage({
              id: "notification.title",
            }),
            message: intl.formatMessage({
              id: "notification.user.post.save.success",
            }),
            kind: NotificationKinds.success,
          });
          setNotificationVisible(true);
        } else {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({ id: "server.error.msg" }),
          });
          setNotificationVisible(true);
        }
        setLoading(false);
      }
    );
  }

  const handleCheckboxChange = (e, rowId, header) => {
    const isChecked = e.target.checked;

    setTestNotificationConfigMenuDataPost((prevData) => {
      const updatedMenuList = prevData.menuList.map((item) => {
        if (item.testId === rowId) {
          return {
            ...item,
            [header]: { ...item[header], active: isChecked },
          };
        }
        return item;
      });

      return {
        ...prevData,
        menuList: updatedMenuList,
      };
    });
    setSaveButton(false);
  };

  const handlePageChange = ({ page, pageSize }) => {
    setPage(page);
    setPageSize(pageSize);
  };

  const renderCell = (cell, row) => {
    if (["testId", "testName"].includes(cell.info.header)) {
      return <TableCell key={cell.id} className={isMobile ? 'compact-cell' : ''}>{cell.value}</TableCell>;
    } else if (
      ["patientEmail", "patientSMS", "providerEmail", "providerSMS"].includes(
        cell.info.header
      )
    ) {
      return (
        <TableCell key={cell.id} className={isMobile ? 'compact-cell' : ''}>
          <Checkbox
            id={`checkbox-${row.id}-${cell.info.header}`}
            labelText=""
            className="responsive-checkbox"
            checked={
              testNotificationConfigMenuDataPost?.menuList.find(
                (item) => item.testId === row.id,
              )?.[cell.info.header]?.active || false
            }
            onChange={(e) => handleCheckboxChange(e, row.id, cell.info.header)}
          />
        </TableCell>
      );
    } else if (cell.info.header === "edit") {
      return (
        <TableCell key={cell.id} className={isMobile ? 'compact-cell' : ''}>
          <Button
            hasIconOnly
            iconDescription={intl.formatMessage({
              id: "testnotification.testdefault.editIcon",
            })}
            onClick={() => handleEditButtonClick(row.cells[0].value)}
            renderIcon={Settings}
            kind="tertiary"
            className="responsive-button"
          />
        </TableCell>
      );
    }
    return <TableCell key={cell.id} className={isMobile ? 'compact-cell' : ''}>{cell.value}</TableCell>;
  };

  return (
    <>
      {notificationVisible && <AlertDialog />}
      {loading && <Loading />}
      <div className="adminPageContent admin-page-content">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />

        {/* Top Buttons */}
        <Grid fullWidth>
          <Column lg={16} md={12} sm={4} xlg={16}>
            <Section>
              <Heading className="responsive-heading">
                <FormattedMessage id="testnotificationconfig.browse.title" />
              </Heading>
            </Section>
            <br />
            <Section>
              <Column
                lg={16}
                md={12}
                sm={4}
                xlg={16}
                className="button-container"
              >
                <Button
                  disabled={saveButton}
                  onClick={testNotificationConfigMenuSavePostCall}
                  type="button"
                  className="action-button"
                >
                  <FormattedMessage id="label.button.save" />
                </Button>
                <Button
                  onClick={() =>
                    window.location.assign(
                      "/MasterListsPage#testNotificationConfigMenu",
                    )
                  }
                  kind="tertiary"
                  type="button"
                  className="action-button"
                >
                  <FormattedMessage id="label.button.exit" />
                </Button>
              </Column>
            </Section>
          </Column>
        </Grid>

        {/* Table */}
        <div className="orderLegendBody">
          <Grid fullWidth>
            <Column lg={16} md={12} sm={4} xlg={16}>
              <br />
              <div className="table-container">
                <DataTable
                  rows={
                    testNotificationConfigMenuDataPost?.menuList
                      ?.slice((page - 1) * pageSize, page * pageSize)
                      ?.map((item) => ({
                        id: item.testId,
                        testId: item.testId,
                        patientEmail: item.patientEmail.active ? "true" : "false",
                        patientSMS: item.patientSMS.active ? "true" : "false",
                        providerEmail: item.providerEmail.active
                          ? "true"
                          : "false",
                        providerSMS: item.providerSMS.active ? "true" : "false",
                        testName: testNamesMap[item.testId] || item.testId,
                      })) || []
                  }
                  headers={[
                    { key: "testId", header: intl.formatMessage({ id: "column.name.testId" }) },
                    { key: "testName", header: intl.formatMessage({ id: "label.testName" }) },
                    { key: "patientEmail", header: intl.formatMessage({ id: "testnotification.patient.email" }) },
                    { key: "patientSMS", header: intl.formatMessage({ id: "testnotification.patient.sms" }) },
                    { key: "providerEmail", header: intl.formatMessage({ id: "testnotification.provider.email" }) },
                    { key: "providerSMS", header: intl.formatMessage({ id: "testnotification.provider.sms" }) },
                    { key: "edit", header: intl.formatMessage({ id: "banner.menu.patientEdit" }) },
                  ]}
                  size={isMobile ? 'sm' : isTablet ? 'md' : 'lg'}
                  className="responsive-table"
                >
                  {({ rows, headers, getHeaderProps, getTableProps }) => (
                    <Table {...getTableProps()}>
                      <TableHead>
                        <TableRow>
                          {headers.map((header) => (
                            <TableHeader 
                              key={header.key} 
                              {...getHeaderProps({ header })}
                              className={isMobile && (header.key === "testId" || header.key === "edit") ? 'narrow-column' : 
                                        isMobile ? 'medium-column' : ''}
                            >
                              {header.header}
                            </TableHeader>
                          ))}
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
                  )}
                </DataTable>
              </div>
              <div className="responsive-pagination">
                <Pagination
                  onChange={handlePageChange}
                  page={page}
                  pageSize={pageSize}
                  pageSizes={getResponsivePageSizes()}
                  totalItems={testNotificationConfigMenuDataPost?.menuList.length}
                  forwardText={intl.formatMessage({ id: "pagination.forward" })}
                  backwardText={intl.formatMessage({ id: "pagination.backward" })}
                  itemRangeText={(min, max, total) =>
                    intl.formatMessage({ id: "pagination.item-range" }, { min, max, total })
                  }
                  itemsPerPageText={intl.formatMessage({ id: "pagination.items-per-page" })}
                  itemText={(min, max) =>
                    intl.formatMessage({ id: "pagination.item" }, { min, max })
                  }
                  pageNumberText={intl.formatMessage({ id: "pagination.page-number" })}
                  pageRangeText={(_, total) =>
                    intl.formatMessage({ id: "pagination.page-range" }, { total })
                  }
                  size={isMobile ? 'sm' : isTablet ? 'md' : 'lg'}
                />
              </div>
              <br />
            </Column>
          </Grid>

          {/* Bottom Buttons */}
          <Grid fullWidth>
            <Column lg={16} md={12} sm={4} xlg={16} className="button-container">
              <Button
                disabled={saveButton}
                onClick={testNotificationConfigMenuSavePostCall}
                type="button"
                className="action-button"
              >
                <FormattedMessage id="label.button.save" />
              </Button>
              <Button
                onClick={() =>
                  window.location.assign("/MasterListsPage#testNotificationConfigMenu")
                }
                kind="tertiary"
                type="button"
                className="action-button"
              >
                <FormattedMessage id="label.button.exit" />
              </Button>
            </Column>
          </Grid>
        </div>
      </div>
    </>
  );
}

export default injectIntl(TestNotificationConfigMenu);