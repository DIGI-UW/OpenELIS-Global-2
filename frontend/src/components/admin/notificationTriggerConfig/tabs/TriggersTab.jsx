import React, { useContext, useEffect, useMemo, useState } from "react";
import {
  Button,
  Checkbox,
  Column,
  DataTable,
  Grid,
  InlineNotification,
  Link,
  Loading,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  Toggle,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../../common/CustomNotification";
import { TRIGGER_DESCRIPTORS } from "../descriptors";

export default function TriggersTab() {
  const intl = useIntl();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [triggers, setTriggers] = useState([]);
  const [availableChannels, setAvailableChannels] = useState([]);
  const [availableRecipientTypes, setAvailableRecipientTypes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [dirty, setDirty] = useState(false);

  useEffect(() => {
    let mounted = true;
    getFromOpenElisServer("/rest/NotificationTriggerConfig", (res) => {
      if (!mounted) return;
      if (res) {
        setTriggers(Array.isArray(res.triggers) ? res.triggers : []);
        setAvailableChannels(
          Array.isArray(res.availableChannels) ? res.availableChannels : [],
        );
        setAvailableRecipientTypes(
          Array.isArray(res.availableRecipientTypes)
            ? res.availableRecipientTypes
            : [],
        );
      }
      setLoading(false);
    });
    return () => {
      mounted = false;
    };
  }, []);

  const channelLabel = (channel) =>
    intl.formatMessage({
      id: `notificationtrigger.channel.${channel.toLowerCase()}`,
      defaultMessage: channel,
    });

  const recipientLabel = (recipientType) => {
    const key = recipientType
      .toLowerCase()
      .replace(/_/g, "")
      .replace(/[^a-z]/g, "");
    return intl.formatMessage({
      id: `notificationtrigger.recipient.${key}`,
      defaultMessage: recipientType,
    });
  };

  const triggerName = (eventCode) => {
    const descriptor = TRIGGER_DESCRIPTORS[eventCode];
    if (descriptor) {
      return intl.formatMessage({
        id: descriptor.nameKey,
        defaultMessage: eventCode,
      });
    }
    return eventCode;
  };

  const triggerDescription = (eventCode) => {
    const descriptor = TRIGGER_DESCRIPTORS[eventCode];
    return descriptor
      ? intl.formatMessage({
          id: descriptor.descriptionKey,
          defaultMessage: "",
        })
      : "";
  };

  const triggerStatusLabel = (eventCode) => {
    const descriptor = TRIGGER_DESCRIPTORS[eventCode];
    return descriptor
      ? intl.formatMessage({ id: descriptor.statusKey, defaultMessage: "" })
      : "";
  };

  const updateTrigger = (eventCode, mutator) => {
    setTriggers((prev) =>
      prev.map((t) => (t.eventCode === eventCode ? mutator({ ...t }) : t)),
    );
    setDirty(true);
  };

  const toggleEnabled = (eventCode, enabled) => {
    updateTrigger(eventCode, (t) => ({ ...t, enabled }));
  };

  const toggleChannel = (eventCode, channel, checked) => {
    updateTrigger(eventCode, (t) => {
      const next = new Set(t.channels || []);
      if (checked) next.add(channel);
      else next.delete(channel);
      return { ...t, channels: Array.from(next) };
    });
  };

  const toggleRecipient = (eventCode, recipientType, checked) => {
    updateTrigger(eventCode, (t) => {
      const next = new Set(t.recipientTypes || []);
      if (checked) next.add(recipientType);
      else next.delete(recipientType);
      return { ...t, recipientTypes: Array.from(next) };
    });
  };

  const handleSave = () => {
    setSaving(true);
    postToOpenElisServerJsonResponse(
      "/rest/NotificationTriggerConfig",
      JSON.stringify({ triggers }),
      (res) => {
        setSaving(false);
        if (res && Array.isArray(res.triggers)) {
          setTriggers(res.triggers);
          setDirty(false);
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "notification.user.post.save.success",
            }),
          });
        } else {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({ id: "server.error.msg" }),
          });
        }
        setNotificationVisible(true);
      },
    );
  };

  const handleCancel = () => {
    setLoading(true);
    setDirty(false);
    getFromOpenElisServer("/rest/NotificationTriggerConfig", (res) => {
      if (res) {
        setTriggers(Array.isArray(res.triggers) ? res.triggers : []);
      }
      setLoading(false);
    });
  };

  const headers = useMemo(
    () => [
      {
        key: "trigger",
        header: intl.formatMessage({
          id: "notificationtrigger.column.trigger",
        }),
      },
      {
        key: "eventCode",
        header: intl.formatMessage({
          id: "notificationtrigger.column.eventcode",
        }),
      },
      {
        key: "enabled",
        header: intl.formatMessage({
          id: "notificationtrigger.column.enabled",
        }),
      },
      {
        key: "channels",
        header: intl.formatMessage({
          id: "notificationtrigger.column.channels",
        }),
      },
      {
        key: "recipients",
        header: intl.formatMessage({
          id: "notificationtrigger.column.recipients",
        }),
      },
      {
        key: "override",
        header: intl.formatMessage({
          id: "notificationtrigger.column.override",
        }),
      },
    ],
    [intl],
  );

  const rows = useMemo(
    () =>
      triggers.map((t) => ({
        id: t.eventCode,
        eventCode: t.eventCode,
      })),
    [triggers],
  );

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {(loading || saving) && <Loading />}
      <Grid fullWidth={true} style={{ marginTop: "1rem" }}>
        <Column lg={16} md={8} sm={4}>
          <h4>
            <FormattedMessage id="notificationtrigger.activetriggers.heading" />
          </h4>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage id="notificationtrigger.activetriggers.subtitle" />
          </p>
        </Column>
      </Grid>
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <DataTable rows={rows} headers={headers}>
            {({ getTableProps, getHeaderProps }) => (
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
                    {triggers.map((trigger) => {
                      const channelSet = new Set(trigger.channels || []);
                      const recipientSet = new Set(
                        trigger.recipientTypes || [],
                      );
                      const rowDisabled = !trigger.enabled;
                      return (
                        <TableRow key={trigger.eventCode}>
                          <TableCell>
                            <div
                              style={{
                                display: "flex",
                                flexDirection: "column",
                              }}
                            >
                              <div
                                style={{
                                  display: "flex",
                                  gap: "0.5rem",
                                  alignItems: "center",
                                }}
                              >
                                <strong>
                                  {triggerName(trigger.eventCode)}
                                </strong>
                                {triggerStatusLabel(trigger.eventCode) && (
                                  <Tag size="sm" type="green">
                                    {triggerStatusLabel(trigger.eventCode)}
                                  </Tag>
                                )}
                              </div>
                              <span
                                style={{
                                  color: "#6f6f6f",
                                  fontSize: "0.875rem",
                                }}
                              >
                                {triggerDescription(trigger.eventCode)}
                              </span>
                            </div>
                          </TableCell>
                          <TableCell>
                            <Tag type="gray">{trigger.eventCode}</Tag>
                          </TableCell>
                          <TableCell>
                            <div
                              style={{
                                display: "flex",
                                flexDirection: "column",
                                gap: "0.25rem",
                              }}
                            >
                              <Toggle
                                id={`toggle-${trigger.eventCode}`}
                                aria-label={intl.formatMessage({
                                  id: "notificationtrigger.column.enabled",
                                })}
                                labelA=""
                                labelB=""
                                hideLabel
                                toggled={!!trigger.enabled}
                                onToggle={(checked) =>
                                  toggleEnabled(trigger.eventCode, checked)
                                }
                              />
                              {!trigger.enabled && (
                                <span
                                  style={{
                                    color: "#6f6f6f",
                                    fontSize: "0.75rem",
                                  }}
                                >
                                  <FormattedMessage
                                    id={
                                      TRIGGER_DESCRIPTORS[trigger.eventCode]
                                        ? "notificationtrigger.referralout.defaultoff"
                                        : "notificationtrigger.disabled.hint"
                                    }
                                    defaultMessage=""
                                  />
                                </span>
                              )}
                            </div>
                          </TableCell>
                          <TableCell>
                            <div
                              style={{
                                display: "flex",
                                flexDirection: "column",
                              }}
                            >
                              {availableChannels.map((channel) => (
                                <Checkbox
                                  key={`${trigger.eventCode}-channel-${channel}`}
                                  id={`${trigger.eventCode}-channel-${channel}`}
                                  labelText={channelLabel(channel)}
                                  checked={channelSet.has(channel)}
                                  disabled={rowDisabled}
                                  onChange={(_, { checked }) =>
                                    toggleChannel(
                                      trigger.eventCode,
                                      channel,
                                      checked,
                                    )
                                  }
                                />
                              ))}
                            </div>
                          </TableCell>
                          <TableCell>
                            <div
                              style={{
                                display: "flex",
                                flexDirection: "column",
                              }}
                            >
                              {availableRecipientTypes.map((recipientType) => (
                                <Checkbox
                                  key={`${trigger.eventCode}-recipient-${recipientType}`}
                                  id={`${trigger.eventCode}-recipient-${recipientType}`}
                                  labelText={recipientLabel(recipientType)}
                                  checked={recipientSet.has(recipientType)}
                                  disabled={rowDisabled}
                                  onChange={(_, { checked }) =>
                                    toggleRecipient(
                                      trigger.eventCode,
                                      recipientType,
                                      checked,
                                    )
                                  }
                                />
                              ))}
                            </div>
                          </TableCell>
                          <TableCell>
                            <Link
                              href="#"
                              data-testid="per-lab-unit-override-stub"
                              onClick={(e) => {
                                e.preventDefault();
                                addNotification({
                                  kind: NotificationKinds.info,
                                  title: intl.formatMessage({
                                    id: "notification.title",
                                  }),
                                  message: intl.formatMessage({
                                    id: "notificationtrigger.override.comingsoon",
                                  }),
                                });
                                setNotificationVisible(true);
                              }}
                            >
                              <FormattedMessage id="notificationtrigger.override.configure" />
                            </Link>
                          </TableCell>
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
        </Column>
      </Grid>
      <Grid fullWidth={true} style={{ marginTop: "1rem" }}>
        <Column lg={16} md={8} sm={4}>
          <InlineNotification
            kind="info"
            lowContrast
            hideCloseButton
            title=""
            subtitle={intl.formatMessage({
              id: "notificationtrigger.referralout.firesoncenote",
            })}
          />
        </Column>
      </Grid>
      <Grid fullWidth={true} style={{ marginTop: "1rem" }}>
        <Column
          lg={16}
          md={8}
          sm={4}
          style={{ display: "flex", justifyContent: "flex-end", gap: "0.5rem" }}
        >
          <Button kind="secondary" onClick={handleCancel} disabled={saving}>
            <FormattedMessage id="label.button.cancel" />
          </Button>
          <Button onClick={handleSave} disabled={!dirty || saving}>
            <FormattedMessage id="label.button.saveconfig" />
          </Button>
        </Column>
      </Grid>
    </>
  );
}
