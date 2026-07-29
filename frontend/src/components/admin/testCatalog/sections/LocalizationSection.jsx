import React, { useCallback, useEffect, useState } from "react";
import {
  Stack,
  Select,
  SelectItem,
  TextInput,
  Tag,
  Tooltip,
  Loading,
  InlineNotification,
} from "@carbon/react";
import { useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  putToOpenElisServerFullResponse,
} from "../../../utils/Utils";

const FALLBACK_LOCALE = "en";

/**
 * OGC-949 / OGC-767 — Localization section. Edits a test's name / reporting-name
 * translations in-context. These live in the generic `localization` tables (the
 * test already FK-links to them), so this reads/writes through the existing
 * /rest/localizations/{id} endpoints; the editor controller only bridges
 * testId → the backing localization ids. No per-test translation store.
 *
 * For the chosen locale each field shows its value with a fallback indicator
 * when the value is coming from English rather than a locale-specific
 * translation, and saves on blur.
 */
const LocalizationSection = ({ testId }) => {
  const intl = useIntl();

  const [locales, setLocales] = useState([]);
  const [locale, setLocale] = useState("");
  // fields: [{ field, localizationId, translations: {locale: value} }]
  const [fields, setFields] = useState([]);
  const [drafts, setDrafts] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [notification, setNotification] = useState(null);

  // Resolve the active supported locales once, defaulting the picker to the
  // first non-fallback locale (so an admin lands on something to translate).
  useEffect(() => {
    getFromOpenElisServer("/rest/supportedlocales/active", (res) => {
      const list = Array.isArray(res) ? res : [];
      setLocales(list);
      if (list.length > 0) {
        const target = list.find((l) => !l.fallback) || list[0];
        setLocale(target.localeCode);
      }
    });
  }, []);

  // Load the test's localization refs, then hydrate each one's translations.
  const load = useCallback(() => {
    if (!testId) {
      return;
    }
    setLoading(true);
    setError(false);
    getFromOpenElisServer(
      `/rest/test-catalog/tests/${testId}/localization`,
      (refs) => {
        if (!refs || !Array.isArray(refs.fields)) {
          setLoading(false);
          setError(true);
          return;
        }
        if (refs.fields.length === 0) {
          setFields([]);
          setLoading(false);
          return;
        }
        let pending = refs.fields.length;
        const resolved = [];
        refs.fields.forEach((ref) => {
          getFromOpenElisServer(
            `/rest/localizations/${ref.localizationId}`,
            (loc) => {
              resolved.push({
                field: ref.field,
                localizationId: ref.localizationId,
                translations: (loc && loc.translations) || {},
              });
              pending -= 1;
              if (pending === 0) {
                setFields(resolved);
                setLoading(false);
              }
            },
          );
        });
      },
    );
  }, [testId]);

  useEffect(() => {
    load();
  }, [load]);

  // Reset the per-field drafts whenever the fields or the chosen locale change,
  // so each input reflects the translation (or empty) for the current locale.
  useEffect(() => {
    const next = {};
    fields.forEach((f) => {
      next[f.field] = f.translations[locale] || "";
    });
    setDrafts(next);
  }, [fields, locale]);

  const saveField = (entry) => {
    const value = drafts[entry.field] || "";
    if (value === (entry.translations[locale] || "")) {
      return;
    }
    const merged = { ...entry.translations, [locale]: value };
    putToOpenElisServerFullResponse(
      `/rest/localizations/${entry.localizationId}/translations`,
      JSON.stringify(merged),
      (response) => {
        if (response && response.ok) {
          setNotification({
            kind: "success",
            text: intl.formatMessage({
              id: "label.testCatalog.localization.saved",
            }),
          });
          load();
        } else {
          setNotification({
            kind: "error",
            text: intl.formatMessage({
              id: "label.testCatalog.localization.saveError",
            }),
          });
        }
      },
    );
  };

  if (loading) {
    return (
      <Loading
        description={intl.formatMessage({ id: "label.loading" })}
        withOverlay={false}
      />
    );
  }

  if (error) {
    return (
      <InlineNotification
        kind="error"
        lowContrast
        hideCloseButton
        title={intl.formatMessage({ id: "error.title" })}
        subtitle={intl.formatMessage({
          id: "label.testCatalog.localization.loadError",
        })}
      />
    );
  }

  const isFallback = (entry) =>
    locale !== FALLBACK_LOCALE && !entry.translations[locale];

  const fallbackTag = (entry) => {
    if (!isFallback(entry)) {
      return null;
    }
    return (
      <Tooltip
        align="top"
        label={intl.formatMessage(
          { id: "label.testCatalog.localization.fallback.tooltip" },
          { locale },
        )}
      >
        <Tag type="warm-gray" size="sm">
          {intl.formatMessage({
            id: "label.testCatalog.localization.fallback.en",
          })}
        </Tag>
      </Tooltip>
    );
  };

  return (
    <Stack gap={6} data-testid="localization-section">
      {notification && (
        <InlineNotification
          kind={notification.kind}
          lowContrast
          title={notification.text}
          onCloseButtonClick={() => setNotification(null)}
        />
      )}

      <p>
        {intl.formatMessage({ id: "label.testCatalog.localization.intro" })}
      </p>

      {fields.length === 0 ? (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "label.testCatalog.localization.empty",
          })}
        />
      ) : (
        <>
          <div style={{ maxWidth: "16rem" }}>
            <Select
              id="localization-locale"
              labelText={intl.formatMessage({
                id: "label.testCatalog.localization.locale",
              })}
              value={locale}
              onChange={(e) => setLocale(e.target.value)}
            >
              {locales.map((l) => (
                <SelectItem
                  key={l.localeCode}
                  value={l.localeCode}
                  text={`${l.displayName} (${l.localeCode})`}
                />
              ))}
            </Select>
          </div>

          {fields.map((entry) => (
            <div
              key={entry.field}
              data-testid={`localization-field-${entry.field}`}
            >
              <div
                style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
              >
                <span className="cds--label">
                  {intl.formatMessage({
                    id: `label.testCatalog.localization.field.${entry.field}`,
                  })}
                </span>
                {fallbackTag(entry)}
              </div>
              <TextInput
                id={`localization-input-${entry.field}`}
                labelText=""
                placeholder={entry.translations[FALLBACK_LOCALE] || ""}
                value={drafts[entry.field] ?? ""}
                onChange={(e) =>
                  setDrafts((prev) => ({
                    ...prev,
                    [entry.field]: e.target.value,
                  }))
                }
                onBlur={() => saveField(entry)}
              />
            </div>
          ))}
        </>
      )}
    </Stack>
  );
};

export default LocalizationSection;
