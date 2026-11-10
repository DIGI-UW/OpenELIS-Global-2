import React, { useCallback, useContext, useEffect, useState } from "react";
import {
  Button,
  Column,
  ComposedModal,
  Grid,
  InlineLoading,
  InlineNotification,
  ModalBody,
  ModalFooter,
  ModalHeader,
  Select,
  SelectItem,
  Tag,
  TextInput,
  Toggle,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  putToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";

const STATUS_TAG = { READY: "green", NOT_SET_UP: "gray", FAILING: "red" };
const STATUS_KEY = {
  READY: "ready",
  NOT_SET_UP: "notSetUp",
  FAILING: "failing",
};

function formatDateTime(iso) {
  if (!iso) {
    return "";
  }
  const date = new Date(iso);
  return Number.isNaN(date.getTime()) ? iso : date.toLocaleString();
}

function TestModal({ channel, open, onClose }) {
  const intl = useIntl();
  const [address, setAddress] = useState("");
  const [sending, setSending] = useState(false);
  const [outcome, setOutcome] = useState(null);

  useEffect(() => {
    if (open) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setOutcome(null);
    }
  }, [open]);

  const send = () => {
    setSending(true);
    setOutcome(null);
    postToOpenElisServerJsonResponse(
      `/rest/delivery/channels/${channel.toLowerCase()}/test`,
      JSON.stringify({ address: address.trim() }),
      (response) => {
        setSending(false);
        if (response && typeof response.success === "boolean") {
          setOutcome(response);
        } else {
          setOutcome({
            success: false,
            message: intl.formatMessage({ id: "delivery.channel.test.failed" }),
          });
        }
      },
    );
  };

  return (
    <ComposedModal open={open} onClose={onClose} size="sm">
      <ModalHeader
        title={intl.formatMessage({ id: "delivery.channel.test.title" })}
        label={intl.formatMessage({
          id: `delivery.channel.${channel.toLowerCase()}`,
        })}
      />
      <ModalBody>
        <TextInput
          id={`channel-test-address-${channel}`}
          labelText={intl.formatMessage({
            id: "delivery.channel.test.address",
          })}
          value={address}
          onChange={(event) => setAddress(event.target.value)}
        />
        {outcome && (
          <InlineNotification
            kind={outcome.success ? "success" : "error"}
            lowContrast
            hideCloseButton
            title={
              outcome.success
                ? intl.formatMessage({ id: "delivery.channel.test.sent" })
                : outcome.message
            }
            subtitle={outcome.success ? outcome.message : ""}
            style={{ marginTop: "1rem" }}
            data-testid="channel-test-outcome"
          />
        )}
      </ModalBody>
      <ModalFooter>
        <Button kind="secondary" onClick={onClose}>
          <FormattedMessage id="label.button.close" />
        </Button>
        <Button
          kind="primary"
          onClick={send}
          disabled={sending || !address.trim()}
        >
          {sending ? (
            <InlineLoading description="" />
          ) : (
            <FormattedMessage id="delivery.channel.test.send" />
          )}
        </Button>
      </ModalFooter>
    </ComposedModal>
  );
}

function ChannelForm({ channel, onSaved }) {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const [form, setForm] = useState({ ...channel, secret: "" });
  const [saving, setSaving] = useState(false);
  const [badField, setBadField] = useState(null);

  const field = (key) => ({
    id: `channel-${channel.channel}-${key}`,
    value: form[key] == null ? "" : String(form[key]),
    onChange: (event) => setForm({ ...form, [key]: event.target.value }),
    invalid: badField === key,
  });

  const refuse = (field) => {
    setBadField(field || null);
    addNotification({
      kind: NotificationKinds.error,
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage(
        { id: "delivery.channel.saveFailed" },
        {
          field: field
            ? intl.formatMessage({
                id: `delivery.channel.${field}`,
                defaultMessage: field,
              })
            : "",
        },
      ),
    });
    setNotificationVisible(true);
  };

  const save = () => {
    setBadField(null);
    if (form.port && !/^\d+$/.test(String(form.port).trim())) {
      refuse("port");
      return;
    }
    setSaving(true);
    const body = { ...form, port: form.port ? Number(form.port) : null };
    putToOpenElisServerJsonResponse(
      `/rest/delivery/channels/${channel.channel.toLowerCase()}`,
      JSON.stringify(body),
      (saved) => {
        setSaving(false);
        if (
          saved &&
          saved.channel &&
          !saved.status?.toString().match(/^\d+$/)
        ) {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({ id: "delivery.channel.saved" }),
          });
          setNotificationVisible(true);
          setForm({ ...form, secret: "" });
          onSaved();
        } else {
          refuse(saved && saved.field);
        }
      },
    );
  };

  const secretHelper = channel.secretSet
    ? intl.formatMessage({ id: "delivery.channel.secretHelper" })
    : "";

  return (
    <div style={{ padding: "1rem 0" }}>
      <Grid narrow>
        <Column lg={16} md={8} sm={4} style={{ marginBottom: "0.5rem" }}>
          <Toggle
            id={`channel-${channel.channel}-active`}
            labelText={intl.formatMessage({ id: "delivery.channel.active" })}
            toggled={form.active}
            onToggle={(checked) => setForm({ ...form, active: checked })}
            size="sm"
          />
        </Column>
        {channel.channel === "EMAIL" && (
          <>
            <Column lg={6} md={4} sm={4}>
              <TextInput
                labelText={intl.formatMessage({ id: "delivery.channel.host" })}
                {...field("host")}
              />
            </Column>
            <Column lg={2} md={2} sm={2}>
              <TextInput
                labelText={intl.formatMessage({ id: "delivery.channel.port" })}
                {...field("port")}
              />
            </Column>
            <Column lg={4} md={2} sm={4}>
              <Select
                id={`channel-${channel.channel}-security`}
                labelText={intl.formatMessage({
                  id: "delivery.channel.security",
                })}
                value={form.security || "STARTTLS"}
                onChange={(event) =>
                  setForm({ ...form, security: event.target.value })
                }
              >
                <SelectItem
                  value="STARTTLS"
                  text={intl.formatMessage({
                    id: "delivery.channel.security.starttls",
                  })}
                />
                <SelectItem
                  value="SSL_TLS"
                  text={intl.formatMessage({
                    id: "delivery.channel.security.ssl",
                  })}
                />
                <SelectItem
                  value="NONE"
                  text={intl.formatMessage({
                    id: "delivery.channel.security.none",
                  })}
                />
              </Select>
            </Column>
            <Column lg={4} md={4} sm={4}>
              <TextInput
                labelText={intl.formatMessage({
                  id: "delivery.channel.username",
                })}
                {...field("username")}
              />
            </Column>
            <Column lg={4} md={4} sm={4}>
              <TextInput
                type="password"
                labelText={`${intl.formatMessage({ id: "delivery.channel.password" })}${
                  channel.secretSet
                    ? ` (${intl.formatMessage({ id: "delivery.channel.secretSet" })})`
                    : ""
                }`}
                helperText={secretHelper}
                placeholder={channel.secretSet ? "••••••••" : ""}
                {...field("secret")}
              />
            </Column>
            <Column lg={6} md={4} sm={4}>
              <TextInput
                labelText={intl.formatMessage({
                  id: "delivery.channel.senderAddress",
                })}
                {...field("senderAddress")}
              />
            </Column>
            <Column lg={5} md={4} sm={4}>
              <TextInput
                labelText={intl.formatMessage({
                  id: "delivery.channel.senderName",
                })}
                {...field("senderName")}
              />
            </Column>
            <Column lg={5} md={4} sm={4}>
              <TextInput
                labelText={intl.formatMessage({
                  id: "delivery.channel.replyTo",
                })}
                {...field("replyTo")}
              />
            </Column>
          </>
        )}
        {channel.channel === "WHATSAPP" && (
          <>
            <Column lg={6} md={4} sm={4}>
              <TextInput
                labelText={intl.formatMessage({
                  id: "delivery.channel.whatsapp.accountSid",
                })}
                {...field("username")}
              />
            </Column>
            <Column lg={5} md={4} sm={4}>
              <TextInput
                type="password"
                labelText={`${intl.formatMessage({ id: "delivery.channel.whatsapp.token" })}${
                  channel.secretSet
                    ? ` (${intl.formatMessage({ id: "delivery.channel.secretSet" })})`
                    : ""
                }`}
                helperText={secretHelper}
                placeholder={channel.secretSet ? "••••••••" : ""}
                {...field("secret")}
              />
            </Column>
            <Column lg={5} md={4} sm={4}>
              <TextInput
                labelText={intl.formatMessage({
                  id: "delivery.channel.whatsapp.from",
                })}
                {...field("senderAddress")}
              />
            </Column>
            <Column lg={8} md={8} sm={4}>
              <TextInput
                labelText={intl.formatMessage({
                  id: "delivery.channel.whatsapp.address",
                })}
                {...field("host")}
              />
            </Column>
          </>
        )}
        {channel.channel === "SMS" && (
          <>
            <Column lg={16} md={8} sm={4}>
              <InlineNotification
                kind="info"
                lowContrast
                hideCloseButton
                title=""
                subtitle={intl.formatMessage({
                  id: "delivery.channel.sms.note",
                })}
              />
            </Column>
            <Column lg={4} md={4} sm={4}>
              <TextInput
                labelText={intl.formatMessage({
                  id: "delivery.channel.countryCode",
                })}
                {...field("countryCode")}
              />
            </Column>
          </>
        )}
      </Grid>
      <div style={{ marginTop: "1rem" }}>
        <Button
          kind="primary"
          size="md"
          onClick={save}
          disabled={saving}
          data-testid={`channel-save-${channel.channel}`}
        >
          {saving ? (
            <InlineLoading description="" />
          ) : (
            <FormattedMessage id="delivery.channel.save" />
          )}
        </Button>
      </div>
    </div>
  );
}

export default function ChannelsTab() {
  const intl = useIntl();
  const [channels, setChannels] = useState(null);
  const [loadError, setLoadError] = useState(false);
  const [openChannel, setOpenChannel] = useState(null);
  const [testing, setTesting] = useState(null);

  const load = useCallback(() => {
    setLoadError(false);
    getFromOpenElisServer("/rest/delivery/channels", (response) => {
      if (Array.isArray(response)) {
        setChannels(response);
      } else {
        setLoadError(true);
      }
    });
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  return (
    <div>
      <h4 style={{ marginTop: "1rem" }}>
        <FormattedMessage id="delivery.channel.title" />
      </h4>
      <p style={{ color: "#525252", marginBottom: "1rem" }}>
        <FormattedMessage id="delivery.channel.subtitle" />
      </p>
      {loadError && (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({ id: "delivery.sent.loadError" })}
          actions={
            <Button kind="ghost" size="sm" onClick={load}>
              Retry
            </Button>
          }
        />
      )}
      {!channels && !loadError && <InlineLoading description="" />}
      {channels &&
        channels.map((channel) => (
          <div
            key={channel.channel}
            style={{
              border: "1px solid #e0e0e0",
              padding: "1rem",
              marginBottom: "0.75rem",
            }}
            data-testid={`channel-row-${channel.channel}`}
          >
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: "1rem",
                flexWrap: "wrap",
              }}
            >
              <strong style={{ minWidth: "6rem" }}>
                {intl.formatMessage({
                  id: `delivery.channel.${channel.channel.toLowerCase()}`,
                })}
              </strong>
              <Tag
                type={STATUS_TAG[channel.status] || "gray"}
                size="sm"
                data-testid={`channel-status-${channel.channel}`}
              >
                {intl.formatMessage({
                  id: `delivery.channel.status.${STATUS_KEY[channel.status] || "notSetUp"}`,
                })}
              </Tag>
              <span style={{ color: "#525252", fontSize: "0.875rem" }}>
                <FormattedMessage id="delivery.channel.lastSuccess" />:{" "}
                {channel.lastSuccessAt
                  ? formatDateTime(channel.lastSuccessAt)
                  : intl.formatMessage({ id: "delivery.channel.never" })}
              </span>
              {channel.status === "FAILING" && channel.lastError && (
                <span style={{ color: "#da1e28", fontSize: "0.875rem" }}>
                  {channel.lastError}
                </span>
              )}
              <span
                style={{ marginLeft: "auto", display: "flex", gap: "0.5rem" }}
              >
                <Button
                  kind="tertiary"
                  size="sm"
                  onClick={() => setTesting(channel.channel)}
                  data-testid={`channel-test-${channel.channel}`}
                >
                  <FormattedMessage id="delivery.channel.sendTest" />
                </Button>
                <Button
                  kind="ghost"
                  size="sm"
                  onClick={() =>
                    setOpenChannel(
                      openChannel === channel.channel ? null : channel.channel,
                    )
                  }
                  data-testid={`channel-edit-${channel.channel}`}
                >
                  <FormattedMessage
                    id="label.button.edit"
                    defaultMessage="Edit"
                  />
                </Button>
              </span>
            </div>
            {openChannel === channel.channel && (
              <ChannelForm channel={channel} onSaved={load} />
            )}
          </div>
        ))}
      {testing && (
        <TestModal
          channel={testing}
          open={!!testing}
          onClose={() => {
            setTesting(null);
            load();
          }}
        />
      )}
    </div>
  );
}
