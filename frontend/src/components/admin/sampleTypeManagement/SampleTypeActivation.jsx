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
  InlineNotification,
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

          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <InlineNotification
                kind="info"
                title={intl.formatMessage({ id: "notification.title" })}
                subtitle={intl.formatMessage({ id: "configuration.sampleType.confirmation.explain" })}
                hideCloseButton
              />
            </Column>
          </Grid>

          <br />

          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <DataTable
                rows={tableRows}
                headers={headers}
                render={({ rows, headers, getHeaderProps, getTableProps }) => (
                  <TableContainer>
                    <Table {...getTableProps()}>
                      <TableHead>
                        <TableRow>
                          {headers.map((header) => (
                            <TableHeader key={header.key} {...getHeaderProps({ header })}>
                              {header.header}
                            </TableHeader>
                          ))}
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {rows.map((row) => (
                          <TableRow key={row.id}>
                            {row.cells.map((cell) => (
                              <TableCell key={cell.id}>{cell.value}</TableCell>
                            ))}
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </TableContainer>
                )}
              />
            </Column>
          </Grid>

          <br />

          {hasChanges && (
            <Grid fullWidth={true}>
              <Column lg={16} md={8} sm={4}>
                <Button
                  onClick={handleSubmit}
                  disabled={isSubmitting}
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
                  style={{ marginLeft: "1rem" }}
                  disabled={isSubmitting}
                >
                  <FormattedMessage id="label.button.cancel" />
                </Button>
              </Column>
            </Grid>
          )}
        </div>
      </div>
    </>
  );
}

export default injectIntl(SampleTypeActivation);