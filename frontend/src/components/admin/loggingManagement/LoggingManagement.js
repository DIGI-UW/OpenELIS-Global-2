import React, { useState, useContext } from "react";
import {
  Button,
  Grid,
  Column,
  Section,
  Heading,
  Select,
  SelectItem,
  TextInput,
} from "@carbon/react";
import { FormattedMessage, useIntl, injectIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";

const LOG_LEVELS = [
  { code: "A", label: "ALL" },
  { code: "T", label: "TRACE" },
  { code: "D", label: "DEBUG" },
  { code: "I", label: "INFO" },
  { code: "W", label: "WARN" },
  { code: "E", label: "ERROR" },
  { code: "F", label: "FATAL" },
  { code: "O", label: "OFF" },
];

function LoggingManagement() {
  const intl = useIntl();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [logLevel, setLogLevel] = useState("I");
  const [logger, setLogger] = useState("org.openelisglobal");

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
    {
      label: "logging.management.label",
      link: "/MasterListsPage/loggingManagement",
    },
  ];

  const handleApply = () => {
    const endpoint = `/logging?logLevel=${encodeURIComponent(logLevel)}&logger=${encodeURIComponent(logger)}`;
    getFromOpenElisServer(endpoint, (res) => {
      setNotificationVisible(true);
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage(
          { id: "logging.management.success" },
          {
            level: LOG_LEVELS.find((l) => l.code === logLevel)?.label,
            logger: logger,
          },
        ),
      });
    });
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
                <FormattedMessage id="logging.management.label" />
              </Heading>
            </Section>
          </Column>
        </Grid>
        <div className="orderLegendBody">
          <Grid>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <FormattedMessage id="logging.management.description" />
              </Section>
              <br />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <Select
                id="log-level-select"
                labelText={intl.formatMessage({
                  id: "logging.management.level",
                })}
                value={logLevel}
                onChange={(e) => setLogLevel(e.target.value)}
              >
                {LOG_LEVELS.map((level) => (
                  <SelectItem
                    key={level.code}
                    value={level.code}
                    text={level.label}
                  />
                ))}
              </Select>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="logger-name"
                labelText={intl.formatMessage({
                  id: "logging.management.logger",
                })}
                value={logger}
                onChange={(e) => setLogger(e.target.value)}
                helperText={intl.formatMessage({
                  id: "logging.management.logger.helper",
                })}
              />
            </Column>
            <Column lg={16} md={8} sm={4}>
              <br />
              <Button onClick={handleApply}>
                <FormattedMessage id="logging.management.apply" />
              </Button>
            </Column>
          </Grid>
        </div>
      </div>
    </>
  );
}

export default injectIntl(LoggingManagement);
