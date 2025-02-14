import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Button,
  Loading,
  Grid,
  Column,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableBody,
  TableHeader,
  TableCell,
  TableContainer,
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils.js";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification.js";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb.js";
import { Settings } from "@carbon/icons-react";
import { NotificationContext } from "../../common/NotificationContext";
import '../testNotificationConfigMenu/TestNotificationConfigMenu.css';

function TestNotificationConfigMenu() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const intl = useIntl();
  const isMounted = useRef(true);

  const [loading, setLoading] = useState(true);
  const [saveButtonDisabled, setSaveButtonDisabled] = useState(true);
  const [testNamesMap, setTestNamesMap] = useState({});
  const [menuList, setMenuList] = useState([]);

  const breadcrumbs = [
    { title: intl.formatMessage({ id: "breadcrumb.home" }), href: "/" },
    {
      title: intl.formatMessage({ id: "breadcrumb.testNotification" }),
      href: "#",
    },
  ];

  useEffect(() => {
    fetchInitialData();
    return () => {
      isMounted.current = false;
    };
  }, []);

  const fetchInitialData = async () => {
    try {
      const menuData = await getFromOpenElisServer(
        "/rest/TestNotificationConfigMenu",
      );
      const testListData = await getFromOpenElisServer("/rest/test-list");

      if (isMounted.current) {
        if (menuData?.menuList) setMenuList(menuData.menuList);
        if (testListData) mapTestNames(testListData);
        setLoading(false);
      }
    } catch (error) {
      console.error("Error fetching initial data:", error);
      setLoading(false);
    }
  };

  const mapTestNames = (testList) => {
    const map = testList.reduce((acc, { id, value }) => {
      acc[id] = value;
      return acc;
    }, {});
    setTestNamesMap(map);
  };

  useEffect(() => {
    if (menuList.length) setSaveButtonDisabled(false);
  }, [menuList]);

  const handleEditButtonClick = (id) => {
    window.location.assign(
      `/MasterListsPage#testNotificationConfig?testId=${id}`,
    );
  };

  const handleSave = async () => {
    setLoading(true);
    try {
      const response = await postToOpenElisServerJsonResponse(
        "/rest/TestNotificationConfigMenu",
        JSON.stringify({ menuList }),
      );

      const notificationProps = response
        ? {
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "notification.user.post.save.success",
            }),
            kind: NotificationKinds.success,
          }
        : {
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({ id: "server.error.msg" }),
            kind: NotificationKinds.error,
          };

      addNotification(notificationProps);
    } catch (error) {
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
        kind: NotificationKinds.error,
      });
    } finally {
      if (isMounted.current) {
        setNotificationVisible(true);
        setLoading(false);
      }
    }
  };

  const dataTableRows = menuList.map(
    ({ testId, patientEmail, patientSMS, providerEmail, providerSMS }) => ({
      id: testId,
      testName: testNamesMap[testId] || testId,
      patientEmail: patientEmail.active ? "true" : "false",
      patientSMS: patientSMS.active ? "true" : "false",
      providerEmail: providerEmail.active ? "true" : "false",
      providerSMS: providerSMS.active ? "true" : "false",
    }),
  );

  const headers = [
    { key: "testId", header: intl.formatMessage({ id: "column.name.testId" }) },
    { key: "testName", header: intl.formatMessage({ id: "label.testName" }) },
    {
      key: "patientEmail",
      header: intl.formatMessage({ id: "testnotification.patient.email" }),
    },
    {
      key: "patientSMS",
      header: intl.formatMessage({ id: "testnotification.patient.sms" }),
    },
    {
      key: "providerEmail",
      header: intl.formatMessage({ id: "testnotification.provider.email" }),
    },
    {
      key: "providerSMS",
      header: intl.formatMessage({ id: "testnotification.provider.sms" }),
    },
    {
      key: "edit",
      header: intl.formatMessage({ id: "banner.menu.patientEdit" }),
    },
  ];

  return (
    <>
      {notificationVisible && <AlertDialog />}
      {loading && <Loading />}
      <div style={styles.adminPageContent}>
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <div style={styles.buttonContainer}>
              <Button
                style={styles.responsiveButton}
                disabled={saveButtonDisabled}
                onClick={handleSave}
                type="button"
              >
                <FormattedMessage id="label.button.save" />
              </Button>
              <Button
                style={styles.responsiveButton}
                onClick={() =>
                  window.location.assign(
                    "/MasterListsPage#testNotificationConfigMenu",
                  )
                }
                kind="tertiary"
                type="button"
              >
                <FormattedMessage id="label.button.exit" />
              </Button>
            </div>
          </Column>
        </Grid>

        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <DataTable rows={dataTableRows} headers={headers}>
              {({ rows, headers, getHeaderProps }) => (
                <TableContainer>
                  <Table>
                    <TableHead>
                      <TableRow>
                        {headers.map((header) => (
                          <TableHeader
                            key={header.key}
                            {...getHeaderProps({ header })}
                            style={styles.tableHeader}
                          >
                            {header.header}
                          </TableHeader>
                        ))}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {rows.map((row) => (
                        <TableRow key={row.id}>
                          {row.cells.map((cell) => (
                            <TableCell key={cell.id} style={styles.tableCell}>
                              {cell.info.header === "edit" ? (
                                <Button
                                  hasIconOnly
                                  style={styles.responsiveIconButton}
                                  iconDescription="Edit"
                                  onClick={() => handleEditButtonClick(row.id)}
                                  renderIcon={Settings}
                                  kind="tertiary"
                                />
                              ) : (
                                cell.value
                              )}
                            </TableCell>
                          ))}
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </DataTable>
          </Column>
        </Grid>
      </div>
    </>
  );
}

export default injectIntl(TestNotificationConfigMenu);
