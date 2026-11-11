import React, { useState, useContext, useEffect, useRef } from "react";
import {
  Button,
  Grid,
  Column,
  Section,
  Heading,
  Select,
  SelectItem,
  TextInput,
  TextArea,
  Toggle,
} from "@carbon/react";
import { FormattedMessage, useIntl, injectIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import config from "../../../config.json";
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

const MAX_DISPLAYED_LINES = 500;

function LoggingManagement() {
  const intl = useIntl();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [logLevel, setLogLevel] = useState("I");
  const [logger, setLogger] = useState("org.openelisglobal");

  const [streaming, setStreaming] = useState(true);
  const [streamError, setStreamError] = useState(null);
  const [tailLines, setTailLines] = useState([]);
  const tailRef = useRef(null);

  useEffect(() => {
    if (!streaming) return undefined;

    const es = new EventSource(`${config.serverBaseUrl}/logging/stream`, {
      withCredentials: true,
    });

    es.onopen = () => setStreamError(null);
    es.onmessage = (event) => {
      setTailLines((prev) => {
        const next = prev.concat(event.data);
        return next.length > MAX_DISPLAYED_LINES
          ? next.slice(next.length - MAX_DISPLAYED_LINES)
          : next;
      });
    };
    es.onerror = () => {
      setStreamError(
        intl.formatMessage({
          id: "logging.management.tail.disconnected",
          defaultMessage:
            "Stream disconnected. Toggle off and on to reconnect.",
        }),
      );
    };

    return () => es.close();
  }, [streaming, intl]);

  useEffect(() => {
    if (tailRef.current) {
      tailRef.current.scrollTop = tailRef.current.scrollHeight;
    }
  }, [tailLines]);

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
    getFromOpenElisServer(endpoint, () => {
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

            <Column lg={16} md={8} sm={4}>
              <br />
              <hr />
              <Section>
                <Heading>
                  <FormattedMessage
                    id="logging.management.tail.label"
                    defaultMessage="Live application log"
                  />
                </Heading>
              </Section>
              <br />
            </Column>
            <Column lg={8} md={4} sm={2}>
              <Toggle
                id="tail-stream"
                labelText={intl.formatMessage({
                  id: "logging.management.tail.stream",
                  defaultMessage: "Live stream",
                })}
                labelA={intl.formatMessage({
                  id: "logging.management.tail.off",
                  defaultMessage: "Off",
                })}
                labelB={intl.formatMessage({
                  id: "logging.management.tail.on",
                  defaultMessage: "On",
                })}
                toggled={streaming}
                onToggle={setStreaming}
              />
            </Column>
            <Column lg={8} md={4} sm={2}>
              <div style={{ marginTop: "1.5rem" }}>
                <Button
                  kind="tertiary"
                  size="sm"
                  onClick={() => setTailLines([])}
                >
                  <FormattedMessage
                    id="logging.management.tail.clear"
                    defaultMessage="Clear"
                  />
                </Button>
              </div>
            </Column>
            <Column lg={16} md={8} sm={4}>
              {streamError && (
                <div style={{ color: "#c62828", margin: "0.5rem 0" }}>
                  {streamError}
                </div>
              )}
              <TextArea
                id="log-tail"
                ref={tailRef}
                labelText=""
                value={tailLines.join("\n")}
                readOnly
                rows={24}
                style={{
                  fontFamily:
                    "ui-monospace, SFMono-Regular, Menlo, Consolas, monospace",
                  fontSize: "0.75rem",
                  whiteSpace: "pre",
                }}
              />
            </Column>
          </Grid>
        </div>
      </div>
    </>
  );
}

export default injectIntl(LoggingManagement);
