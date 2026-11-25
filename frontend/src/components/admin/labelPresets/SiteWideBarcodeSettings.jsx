import React, { useContext, useEffect, useRef, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Button,
  Heading,
  InlineNotification,
  RadioButton,
  RadioButtonGroup,
  Section,
  SkeletonText,
  Tag,
  TextInput,
  Tile,
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";
import { describeSaveFailure, translateServerMessage } from "./helpers";

export const SITE_SETTINGS_ENDPOINT = "/api/siteSettings/barcode";
export const ORDER_ENTRY_POOL = "orderEntryPool";
export const SEPARATE_SERIES = "separateSeries";
const PREFIX_LENGTH = 4;
const PREFIX_PATTERN = /^[A-Za-z0-9]{4}$/;

/**
 * Maps the wire shape of GET /api/siteSettings/barcode to the form. The
 * property is `prePrintUseAltAccession`: true means a separate pre-printed
 * series with its own prefix, false means the order entry format and pool.
 * The retired screen's checkbox was the inverse ("don't use"), so the
 * mapping lives in one place.
 */
export function toForm(data) {
  if (!data || typeof data !== "object" || Array.isArray(data)) {
    return null;
  }
  const useSeparateSeries =
    data.prePrintUseAltAccession === true ||
    data.prePrintUseAltAccession === "true";
  return {
    source: useSeparateSeries ? SEPARATE_SERIES : ORDER_ENTRY_POOL,
    prefix:
      typeof data.prePrintAltAccessionPrefix === "string"
        ? data.prePrintAltAccessionPrefix
        : "",
  };
}

export function toPayload(form) {
  return {
    prePrintUseAltAccession: form.source === SEPARATE_SERIES,
    prePrintAltAccessionPrefix: form.prefix,
  };
}

export function cleanPrefix(raw) {
  return String(raw ?? "")
    .toUpperCase()
    .replace(/[^A-Z0-9]/g, "")
    .slice(0, PREFIX_LENGTH);
}

export function prefixIsValid(form) {
  return form.source !== SEPARATE_SERIES || PREFIX_PATTERN.test(form.prefix);
}

const sameForm = (a, b) =>
  Boolean(a && b) && a.source === b.source && a.prefix === b.prefix;

function SiteWideBarcodeSettings() {
  const intl = useIntl();
  const { addNotification } = useContext(NotificationContext);
  const mounted = useRef(false);

  const [form, setForm] = useState(null);
  const [saved, setSaved] = useState(null);
  const [loadFailed, setLoadFailed] = useState(false);
  const [prefixError, setPrefixError] = useState(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    mounted.current = true;
    getFromOpenElisServer(SITE_SETTINGS_ENDPOINT, (data) => {
      if (!mounted.current) {
        return;
      }
      const loaded = toForm(data);
      if (loaded) {
        setForm(loaded);
        setSaved(loaded);
      } else {
        setLoadFailed(true);
      }
    });
    return () => {
      mounted.current = false;
    };
  }, []);

  const dirty = form && !sameForm(form, saved);

  const chooseSource = (source) => {
    setPrefixError(null);
    setForm((prev) => ({ ...prev, source }));
  };

  const changePrefix = (event) => {
    const prefix = cleanPrefix(event.target.value);
    setPrefixError(null);
    setForm((prev) => ({ ...prev, prefix }));
  };

  const invalidPrefixText = () =>
    intl.formatMessage({ id: "admin.labelPresets.siteWide.prefix.invalid" });

  const notifySaveFailure = (message) => {
    addNotification({
      kind: NotificationKinds.error,
      title: intl.formatMessage({
        id: "admin.labelPresets.siteWide.saveFailed",
      }),
      message,
    });
  };

  const save = () => {
    if (!form || !prefixIsValid(form)) {
      setPrefixError(invalidPrefixText());
      return;
    }
    setSaving(true);
    const submitted = { ...form };
    postToOpenElisServerFullResponse(
      SITE_SETTINGS_ENDPOINT,
      JSON.stringify(toPayload(submitted)),
      (response) => {
        if (!mounted.current) {
          return;
        }
        setSaving(false);
        if (response && response.status === 200) {
          setSaved(submitted);
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({
              id: "admin.labelPresets.siteWide.saved",
            }),
          });
          return;
        }
        if (!response) {
          notifySaveFailure(
            intl.formatMessage({
              id: "admin.labelPresets.saveFailed.noResponse",
            }),
          );
          return;
        }
        response.text().then((text) => {
          if (!mounted.current) {
            return;
          }
          if (response.status === 422) {
            setPrefixError(prefixErrorFrom(text) || invalidPrefixText());
          }
          notifySaveFailure(describeSaveFailure(intl, response.status, text));
        });
      },
    );
  };

  const prefixErrorFrom = (rawBody) => {
    try {
      const body = JSON.parse(rawBody);
      const fieldError = (body.fieldErrors || []).find(
        (fe) => fe.field === "prePrintAltAccessionPrefix",
      );
      return fieldError && fieldError.defaultMessage
        ? translateServerMessage(intl, fieldError.defaultMessage)
        : null;
    } catch (e) {
      return null;
    }
  };

  return (
    <Section>
      <Tile
        className="site-wide-barcode-settings"
        data-testid="site-wide-barcode-settings"
        style={{ marginBottom: "1rem" }}
      >
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: "0.5rem",
            flexWrap: "wrap",
            marginBottom: "0.5rem",
          }}
        >
          <Heading style={{ fontSize: "1.125rem" }}>
            <FormattedMessage id="admin.labelPresets.siteWide.title" />
          </Heading>
          <Tag type="cool-gray" size="sm">
            <FormattedMessage id="admin.labelPresets.siteWide.scope" />
          </Tag>
        </div>

        {loadFailed && (
          <InlineNotification
            kind="error"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({
              id: "admin.labelPresets.siteWide.loadFailed",
            })}
          />
        )}
        {!form && !loadFailed && <SkeletonText paragraph lineCount={3} />}

        {form && (
          <>
            <RadioButtonGroup
              legendText={intl.formatMessage({
                id: "admin.labelPresets.siteWide.prePrint.legend",
              })}
              name="prePrintSource"
              orientation="vertical"
              valueSelected={form.source}
              onChange={(value) => chooseSource(value)}
            >
              <RadioButton
                id="preprint-source-order-entry"
                value={ORDER_ENTRY_POOL}
                labelText={intl.formatMessage({
                  id: "admin.labelPresets.siteWide.prePrint.orderEntry",
                })}
              />
              <RadioButton
                id="preprint-source-separate"
                value={SEPARATE_SERIES}
                labelText={intl.formatMessage({
                  id: "admin.labelPresets.siteWide.prePrint.separate",
                })}
              />
            </RadioButtonGroup>
            <div style={{ maxWidth: "20rem", marginTop: "0.75rem" }}>
              <TextInput
                id="preprint-prefix"
                labelText={intl.formatMessage({
                  id: "admin.labelPresets.siteWide.prefix.label",
                })}
                helperText={intl.formatMessage({
                  id: "admin.labelPresets.siteWide.prefix.helper",
                })}
                maxLength={PREFIX_LENGTH}
                value={form.prefix}
                onChange={changePrefix}
                disabled={form.source !== SEPARATE_SERIES}
                invalid={Boolean(prefixError)}
                invalidText={prefixError || ""}
              />
            </div>
          </>
        )}

        <div style={{ marginTop: "1rem" }}>
          <Button
            size="sm"
            onClick={save}
            disabled={!form || loadFailed || saving || !dirty}
            data-testid="site-wide-barcode-save"
          >
            <FormattedMessage id="admin.labelPresets.siteWide.save" />
          </Button>
        </div>
      </Tile>
    </Section>
  );
}

export default SiteWideBarcodeSettings;
