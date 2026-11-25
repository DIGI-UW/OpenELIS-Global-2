import React, { useCallback, useContext, useEffect, useState } from "react";
import {
  Button,
  Column,
  Grid,
  InlineLoading,
  InlineNotification,
  Select,
  SelectItem,
  TextInput,
  Toggle,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  putToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";

export default function DeliverySettingsTab() {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const [form, setForm] = useState(null);
  const [loadError, setLoadError] = useState(false);
  const [saving, setSaving] = useState(false);
  const [badField, setBadField] = useState(null);

  const load = useCallback(() => {
    setLoadError(false);
    getFromOpenElisServer("/rest/delivery/settings", (response) => {
      if (response && Array.isArray(response.retryDelays)) {
        setForm({
          ...response,
          retryDelaysText: response.retryDelays.join(","),
        });
      } else {
        setLoadError(true);
      }
    });
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const save = () => {
    const delays = form.retryDelaysText
      .split(",")
      .map((part) => part.trim())
      .filter((part) => part)
      .map((part) => Number(part));
    if (
      delays.length === 0 ||
      delays.some((delay) => !Number.isInteger(delay) || delay < 1)
    ) {
      setBadField("retryDelays");
      return;
    }
    if (
      form.location === "CENTRAL" &&
      !window.confirm(
        intl.formatMessage(
          { id: "delivery.settings.centralConfirm" },
          { address: form.centralAddress },
        ),
      )
    ) {
      return;
    }
    setSaving(true);
    setBadField(null);
    const body = {
      retryDelays: delays,
      defaultFormat: form.defaultFormat,
      holdMinutes: Number(form.holdMinutes),
      sendPartials: !!form.sendPartials,
      labContact: form.labContact || "",
      location: form.location,
      centralAddress: form.centralAddress || "",
      secureLinks: !!form.secureLinks,
      linkDays: Number(form.linkDays),
      countryCode: form.countryCode || "",
    };
    putToOpenElisServerJsonResponse(
      "/rest/delivery/settings",
      JSON.stringify(body),
      (saved) => {
        setSaving(false);
        if (saved && Array.isArray(saved.retryDelays)) {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({ id: "delivery.settings.saved" }),
          });
          setNotificationVisible(true);
          load();
        } else {
          const field = saved && saved.field ? saved.field : null;
          setBadField(field);
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage(
              { id: "delivery.settings.invalid" },
              {
                field: field
                  ? intl.formatMessage({
                      id: `delivery.settings.${field}`,
                      defaultMessage: field,
                    })
                  : "",
              },
            ),
          });
          setNotificationVisible(true);
        }
      },
    );
  };

  const text = (key, extra = {}) => ({
    id: `delivery-settings-${key}`,
    value: form[key] == null ? "" : String(form[key]),
    onChange: (event) => setForm({ ...form, [key]: event.target.value }),
    invalid: badField === key,
    ...extra,
  });

  return (
    <div>
      <h4 style={{ marginTop: "1rem" }}>
        <FormattedMessage id="delivery.settings.title" />
      </h4>
      <p style={{ color: "#525252", marginBottom: "1rem" }}>
        <FormattedMessage id="delivery.settings.subtitle" />
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
      {!form && !loadError && <InlineLoading description="" />}
      {form && (
        <Grid narrow>
          <Column lg={6} md={4} sm={4}>
            <TextInput
              labelText={intl.formatMessage({
                id: "delivery.settings.retryDelays",
              })}
              helperText={intl.formatMessage({
                id: "delivery.settings.retryDelays.helper",
              })}
              {...text("retryDelaysText")}
              invalid={badField === "retryDelays"}
            />
          </Column>
          <Column lg={5} md={4} sm={4}>
            <Select
              id="delivery-settings-defaultFormat"
              labelText={intl.formatMessage({
                id: "delivery.settings.defaultFormat",
              })}
              value={form.defaultFormat}
              onChange={(event) =>
                setForm({ ...form, defaultFormat: event.target.value })
              }
            >
              <SelectItem
                value="MASKED"
                text={intl.formatMessage({ id: "delivery.format.masked" })}
              />
              <SelectItem
                value="PROTECTED"
                text={intl.formatMessage({ id: "delivery.format.protected" })}
              />
            </Select>
          </Column>
          <Column lg={5} md={4} sm={4}>
            <TextInput
              labelText={intl.formatMessage({ id: "delivery.settings.hold" })}
              {...text("holdMinutes", { type: "number" })}
            />
          </Column>
          <Column lg={16} md={8} sm={4} style={{ marginTop: "0.5rem" }}>
            <Toggle
              id="delivery-settings-sendPartials"
              labelText={intl.formatMessage({
                id: "delivery.settings.partials",
              })}
              toggled={!!form.sendPartials}
              onToggle={(checked) =>
                setForm({ ...form, sendPartials: checked })
              }
              size="sm"
            />
          </Column>
          <Column lg={16} md={8} sm={4}>
            <TextInput
              labelText={intl.formatMessage({
                id: "delivery.settings.contact",
              })}
              helperText={intl.formatMessage({
                id: "delivery.settings.contact.helper",
              })}
              {...text("labContact")}
            />
          </Column>
          <Column lg={5} md={4} sm={4}>
            <Select
              id="delivery-settings-location"
              labelText={intl.formatMessage({
                id: "delivery.settings.location",
              })}
              value={form.location}
              onChange={(event) =>
                setForm({ ...form, location: event.target.value })
              }
            >
              <SelectItem
                value="LOCAL"
                text={intl.formatMessage({
                  id: "delivery.settings.location.local",
                })}
              />
              <SelectItem
                value="CENTRAL"
                text={intl.formatMessage({
                  id: "delivery.settings.location.central",
                })}
              />
            </Select>
          </Column>
          <Column lg={11} md={4} sm={4}>
            <TextInput
              labelText={intl.formatMessage({
                id: "delivery.settings.centralAddress",
              })}
              disabled={form.location !== "CENTRAL"}
              {...text("centralAddress")}
            />
          </Column>
          <Column lg={5} md={4} sm={4} style={{ marginTop: "0.5rem" }}>
            <Toggle
              id="delivery-settings-secureLinks"
              labelText={intl.formatMessage({ id: "delivery.settings.links" })}
              toggled={!!form.secureLinks}
              onToggle={(checked) => setForm({ ...form, secureLinks: checked })}
              size="sm"
            />
          </Column>
          <Column lg={5} md={4} sm={4}>
            <TextInput
              labelText={intl.formatMessage({
                id: "delivery.settings.linkDays",
              })}
              {...text("linkDays", { type: "number" })}
            />
          </Column>
          <Column lg={6} md={4} sm={4}>
            <TextInput
              labelText={intl.formatMessage({
                id: "delivery.settings.countryCode",
              })}
              {...text("countryCode")}
            />
          </Column>
          <Column lg={16} md={8} sm={4} style={{ marginTop: "1rem" }}>
            <Button
              kind="primary"
              size="md"
              onClick={save}
              disabled={saving}
              data-testid="delivery-settings-save"
            >
              {saving ? (
                <InlineLoading description="" />
              ) : (
                <FormattedMessage id="delivery.settings.save" />
              )}
            </Button>
          </Column>
        </Grid>
      )}
    </div>
  );
}
