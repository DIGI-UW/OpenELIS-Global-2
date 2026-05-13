import React, { useContext, useState, useRef, useEffect } from "react";
import {
  Heading,
  Loading,
  Grid,
  Column,
  Section,
  Toggle,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableBody,
  TableHeader,
  TableCell,
  TableContainer,
  Button,
  Checkbox,
} from "@carbon/react";
import { getFromOpenElisServer, postToOpenElisServerJsonResponse } from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { CustomShowGuide } from "../testManagementConfigMenu/customComponents/CustomShowGuide";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "configuration.sampleType.manage",
    link: "/MasterListsPage/SampleTypeManagement",
  },
  {
    label: "configuration.sampleType.activate",
    link: "/MasterListsPage/SampleTypeActivation",
  },
];

function SampleTypeActivation() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const componentMounted = useRef(false);
  const [isLoading, setIsLoading] = useState(false);
  const [sampleTypeList, setSampleTypeList] = useState([]);
  const [showGuide, setShowGuide] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [pendingChanges, setPendingChanges] = useState({});

  const handleToggleShowGuide = () => {
    setShowGuide(!showGuide);
  };

  const loadSampleTypes = () => {
    setIsLoading(true);
    getFromOpenElisServer(
      `/rest/SampleTypeManagement`,
      (res) => {
        if (componentMounted.current) {
          setSampleTypeList(res.menuList || []);
          setPendingChanges({});
          setIsLoading(false);
        }
      }
    );
  };

  const handleActivationChange = (sampleTypeId, isActive) => {
    setPendingChanges(prev => ({
      ...prev,
      [sampleTypeId]: isActive
    }));
  };

  const handleSubmit = () => {
    if (Object.keys(pendingChanges).length === 0) {
      addNotification({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({ id: "notification.title" }),
        message: "No changes to save.",
      });
      setNotificationVisible(true);
      return;
    }

    setIsSubmitting(true);

    const changes = Object.entries(pendingChanges).map(([id, isActive]) => ({
      id: parseInt(id),
      isActive: isActive
    }));

    postToOpenElisServerJsonResponse(
      `/rest/SampleTypeManagement/activate`,
      { changes },
      (res) => {
        if (res && res.success) {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({ id: "notification.sample.type.deactivate.success" }),
            kind: NotificationKinds.success,
          });
          setNotificationVisible(true);

          // Reload data and clear pending changes
          loadSampleTypes();
        } else {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message: res?.message || intl.formatMessage({ id: "server.error.msg" }),
          });
          setNotificationVisible(true);
        }
        setIsSubmitting(false);
      }
    );
  };

  const handleCancel = () => {
    setPendingChanges({});
  };

  const getSampleTypeStatus = (sampleType) => {
    const sampleTypeId = sampleType.id;
    if (pendingChanges.hasOwnProperty(sampleTypeId)) {
      return pendingChanges[sampleTypeId];
    }
    return sampleType.isActive;
  };

  const hasChanges = Object.keys(pendingChanges).length > 0;

  const rows = [
    {
      id: "sampleTypeTable",
      field: intl.formatMessage({ id: "sample.type" }),
      description: <FormattedMessage id="configuration.sampleType.confirmation.explain" />,
    },
    {
      id: "activation",
      field: intl.formatMessage({ id: "label.status.active" }),
      description: <FormattedMessage id="configuration.sampleType.activate.explain" />,
    },
  ];

  const headers = [
    { key: "name", header: intl.formatMessage({ id: "sample.type.name" }) },
    { key: "displayOrder", header: intl.formatMessage({ id: "sample.type.display.order" }) },
    { key: "whonetCode", header: intl.formatMessage({ id: "sample.type.whonet.code" }) },
    { key: "testCount", header: intl.formatMessage({ id: "sample.type.test.count" }) },
    { key: "storageDefaults", header: intl.formatMessage({ id: "sample.type.storage.defaults" }) },
    { key: "status", header: intl.formatMessage({ id: "label.status" }) },
    { key: "actions", header: intl.formatMessage({ id: "label.actions" }) },
  ];

  const tableRows = sampleTypeList.map((sampleType) => {
    const currentStatus = getSampleTypeStatus(sampleType);
    const hasTests = (sampleType.testCount || 0) > 0;

    return {
      id: sampleType.id.toString(),
      name: sampleType.description || sampleType.name || `Sample Type ${sampleType.id}`,
      displayOrder: sampleType.sortOrder || sampleType.displayOrder || "-",
      whonetCode: sampleType.whonetCode || "-",
      testCount: sampleType.testCount || 0,
      storageDefaults: sampleType.storageDefaults || "-",
      status: currentStatus
        ? intl.formatMessage({ id: "label.status.active" })
        : intl.formatMessage({ id: "label.status.inactive" }),
      actions: (
        <Checkbox
          id={`activation-${sampleType.id}`}
          labelText=""
          checked={currentStatus}
          onChange={(checked) => handleActivationChange(sampleType.id, checked)}
          disabled={!hasTests && !currentStatus}
        />
      ),
    };
  });

  useEffect(() => {
    componentMounted.current = true;
    loadSampleTypes();
    return () => {
      componentMounted.current = false;
    };
  }, []);

  if (isLoading) {
    return <Loading />;
  }

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <div className="orderLegendBody">
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Heading>
                  <FormattedMessage id="configuration.sampleType.activate" />
                </Heading>
              </Section>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />

          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Toggle
                id="toggle"
                labelText={<FormattedMessage id="test.show.guide" />}
                onClick={handleToggleShowGuide}
              />
            </Column>
          </Grid>
          {showGuide && <CustomShowGuide rows={rows} />}

          <br />

          {/* Information Banner */}
          <Grid fullWidth={true} style={{ marginBottom: "2rem" }}>
            <Column lg={16} md={8} sm={4}>
              <div style={{
                backgroundColor: "#f0f8ff",
                padding: "1.5rem",
                borderRadius: "8px",
                border: "1px solid #d0e2ff",
                display: "flex",
                alignItems: "center",
                gap: "0.75rem"
              }}>
                <span style={{
                  backgroundColor: "#0066cc",
                  color: "white",
                  borderRadius: "50%",
                  width: "20px",
                  height: "20px",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  fontSize: "12px",
                  fontWeight: "bold",
                  flexShrink: 0
                }}>
                  i
                </span>
                <div>
                  <p style={{
                    color: "#0066cc",
                    margin: 0,
                    fontSize: "0.875rem",
                    lineHeight: "1.4",
                    fontWeight: "500"
                  }}>
                    <FormattedMessage id="configuration.sampleType.confirmation.explain" />
                  </p>
                </div>
              </div>
            </Column>
          </Grid>

          {/* Sample Type Activation Table */}
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section style={{
                backgroundColor: "#ffffff",
                padding: "2rem",
                borderRadius: "12px",
                border: "1px solid #e0e0e0",
                boxShadow: "0 4px 8px rgba(0, 0, 0, 0.08)"
              }}>
                <div style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "0.75rem",
                  marginBottom: "1.5rem"
                }}>
                  <div style={{
                    backgroundColor: "#0066cc",
                    color: "white",
                    borderRadius: "50%",
                    width: "28px",
                    height: "28px",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    fontSize: "14px",
                    fontWeight: "bold"
                  }}>
                    ⚙
                  </div>
                  <div>
                    <Heading size="md" style={{ margin: 0, color: "#161616" }}>
                      Sample Types Activation Management
                    </Heading>
                    <p style={{
                      margin: "0.25rem 0 0 0",
                      fontSize: "0.875rem",
                      color: "#6f6f6f"
                    }}>
                      Control which sample types are active in the system
                    </p>
                  </div>
                </div>

                <DataTable
                  rows={tableRows}
                  headers={headers}
                  render={({ rows, headers, getHeaderProps, getTableProps }) => (
                    <TableContainer style={{
                      backgroundColor: "#f8f9fa",
                      borderRadius: "8px",
                      border: "1px solid #e0e0e0"
                    }}>
                      <Table {...getTableProps()} style={{ backgroundColor: "white" }}>
                        <TableHead style={{ backgroundColor: "#f4f4f4" }}>
                          <TableRow>
                            {headers.map((header) => (
                              <TableHeader
                                key={header.key}
                                {...getHeaderProps({ header })}
                                style={{
                                  backgroundColor: "#f4f4f4",
                                  color: "#161616",
                                  fontWeight: "600",
                                  fontSize: "0.875rem",
                                  borderBottom: "2px solid #e0e0e0"
                                }}
                              >
                                {header.header}
                              </TableHeader>
                            ))}
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {rows.map((row) => {
                            const sampleType = sampleTypeList.find(st => st.id.toString() === row.id);
                            const currentStatus = getSampleTypeStatus(sampleType);
                            const hasTests = (sampleType?.testCount || 0) > 0;

                            return (
                              <TableRow
                                key={row.id}
                                style={{ backgroundColor: "white" }}
                              >
                                {row.cells.map((cell) => {
                                  if (cell.info.header === "status") {
                                    return (
                                      <TableCell key={cell.id} style={{ padding: "1rem" }}>
                                        <span style={{
                                          fontWeight: "500",
                                          color: currentStatus ? "#161616" : "#dc2626"
                                        }}>
                                          {currentStatus
                                            ? intl.formatMessage({ id: "label.status.active" })
                                            : intl.formatMessage({ id: "label.status.inactive" })
                                          }
                                        </span>
                                      </TableCell>
                                    );
                                  }

                                  if (cell.info.header === "actions") {
                                    return (
                                      <TableCell key={cell.id} style={{ padding: "1rem" }}>
                                        <div style={{
                                          display: "flex",
                                          alignItems: "center",
                                          gap: "0.5rem"
                                        }}>
                                          <Checkbox
                                            id={`activation-${sampleType.id}`}
                                            labelText=""
                                            checked={currentStatus}
                                            onChange={(checked) => handleActivationChange(sampleType.id, checked)}
                                            disabled={!hasTests && !currentStatus}
                                          />
                                          {!hasTests && !currentStatus && (
                                            <span style={{
                                              fontSize: "0.75rem",
                                              color: "#6b7280",
                                              fontStyle: "italic"
                                            }}>
                                              Needs tests
                                            </span>
                                          )}
                                        </div>
                                      </TableCell>
                                    );
                                  }

                                  if (cell.info.header === "testCount") {
                                    return (
                                      <TableCell key={cell.id} style={{ padding: "1rem" }}>
                                        <span style={{
                                          backgroundColor: hasTests ? "#e0f2fe" : "#fee2e2",
                                          color: hasTests ? "#0066cc" : "#991b1b",
                                          padding: "0.25rem 0.5rem",
                                          borderRadius: "12px",
                                          fontSize: "0.875rem",
                                          fontWeight: "500"
                                        }}>
                                          {cell.value}
                                        </span>
                                      </TableCell>
                                    );
                                  }

                                  return (
                                    <TableCell key={cell.id} style={{
                                      padding: "1rem",
                                      color: "#374151",
                                      fontSize: "0.875rem"
                                    }}>
                                      {cell.value}
                                    </TableCell>
                                  );
                                })}
                              </TableRow>
                            );
                          })}
                        </TableBody>
                      </Table>
                    </TableContainer>
                  )}
                />

                {/* Action Buttons */}
                {hasChanges && (
                  <div style={{
                    marginTop: "2rem",
                    padding: "1.5rem",
                    backgroundColor: "#f8f9fa",
                    borderRadius: "8px",
                    border: "1px solid #e0e0e0"
                  }}>
                    <div style={{
                      display: "flex",
                      alignItems: "center",
                      gap: "0.75rem",
                      marginBottom: "1rem"
                    }}>
                      <span style={{
                        backgroundColor: "#f59e0b",
                        color: "white",
                        borderRadius: "50%",
                        width: "20px",
                        height: "20px",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        fontSize: "12px",
                        fontWeight: "bold"
                      }}>
                        !
                      </span>
                      <p style={{
                        margin: 0,
                        fontSize: "0.875rem",
                        color: "#d97706",
                        fontWeight: "500"
                      }}>
                        You have unsaved changes.
                      </p>
                    </div>

                    <div style={{ display: "flex", gap: "1rem" }}>
                      <Button
                        onClick={handleSubmit}
                        disabled={isSubmitting}
                        kind="primary"
                      >
                        {isSubmitting ? (
                          <FormattedMessage id="sample.type.updating" />
                        ) : (
                          <FormattedMessage id="label.button.save" />
                        )}
                      </Button>

                      <Button
                        kind="secondary"
                        onClick={handleCancel}
                        disabled={isSubmitting}
                      >
                        <FormattedMessage id="label.button.cancel" />
                      </Button>
                    </div>
                  </div>
                )}
              </Section>
            </Column>
          </Grid>
        </div>
      </div>
    </>
  );
}

export default injectIntl(SampleTypeActivation);