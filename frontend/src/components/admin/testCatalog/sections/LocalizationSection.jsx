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

/** Base language subtag of a locale code, tolerating both `fr_FR` and `fr-FR`. */
const baseLanguage = (code) =>
  String(code || "")
    .toLowerCase()
    .split(/[-_]/)[0];

/**
 * OGC-1153: pick the locale the picker opens on. The session locale wins so the
 * admin lands on the record they are actually reading (an exact match first, then
 * the same language in another region, e.g. session `en-GB` → supported `en`);
 * only when the session locale is not supported does the configured fallback —
 * then the first entry — take over.
 */
const resolveInitialLocale = (list, sessionLocale) => {
  const exact = list.find((l) => l.localeCode === sessionLocale);
  if (exact) {
    return exact.localeCode;
  }
  const sameLanguage = list.find(
    (l) => baseLanguage(l.localeCode) === baseLanguage(sessionLocale),
  );
  if (sameLanguage) {
    return sameLanguage.localeCode;
  }
  const fallback = list.find((l) => l.fallback);
  return (fallback || list[0]).localeCode;
};

/**
 * OGC-949 / OGC-767 — Localization section. Edits a test's name / reporting-name
 * translations in-context. These live in the generic `localization` tables (the
 * test already FK-links to them), so this reads/writes through the existing
 * /rest/localizations/{id} endpoints; the editor controller only bridges
 * testId → the backing localization ids. No per-test translation store.
 *
 * The picker opens on the session locale so the admin knows which record they are
 * looking at. For the chosen locale each field shows its value with a fallback
 * indicator when the value is coming from English rather than a locale-specific
 * translation, and saves on blur — which the section says explicitly, since there
 * is no Save control to imply it.
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

  // Resolve the active supported locales, defaulting the picker to the locale the
  // session is running in (see resolveInitialLocale).
  useEffect(() => {
    getFromOpenElisServer("/rest/supportedlocales/active", (res) => {
      const list = Array.isArray(res) ? res : [];
      setLocales(list);
      if (list.length > 0) {
        setLocale(resolveInitialLocale(list, intl.locale));
      }
    });
  }, [intl.locale]);

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

      {/* OGC-1153: there is no Save button — the fields persist on blur — so the
          section states that outright, the way the Display Order section does. */}
      <p>
        {intl.formatMessage({ id: "label.testCatalog.localization.intro" })}{" "}
        {intl.formatMessage({ id: "label.testCatalog.localization.autoSave" })}
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
