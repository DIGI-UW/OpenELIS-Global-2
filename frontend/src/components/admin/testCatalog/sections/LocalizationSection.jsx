import React, { useCallback, useEffect, useRef, useState } from "react";
import {
  Stack,
  Select,
  SelectItem,
  TextInput,
  Tag,
  Tooltip,
  Loading,
  Button,
  InlineNotification,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
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
 * Pick the locale the picker opens on: the session locale (exact match first, then
 * the same language in another region, e.g. session `en-GB` → supported `en`),
 * falling back to the configured fallback locale and then the first entry.
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

/** Draft key, so an edit is scoped to the locale it was typed in. */
const draftKey = (localeCode, field) => `${localeCode}|${field}`;

/**
 * OGC-949 / OGC-767 — Localization section. Edits a test's name / reporting-name
 * translations in-context. These live in the generic `localization` tables (the
 * test already FK-links to them), so this reads/writes through the existing
 * /rest/localizations/{id} endpoints; the editor controller only bridges
 * an entity id → the backing localization ids. No per-entity translation store.
 *
 * <p>Reused by the Panel and Sample Type editors, which pass their own bridge
 * endpoint via `refsUrl` and their id via `entityId`; a test needs neither and
 * keeps calling it with `testId` alone.
 *
 * The picker opens on the session locale so the admin knows which record they are
 * looking at. For the chosen locale each field shows its value with a fallback
 * indicator when the value is coming from English rather than a locale-specific
 * translation.
 *
 * Drafts are keyed by locale as well as field, so switching the picker to check
 * another language does not drop what was typed; one Save flushes every pending
 * locale of a field in a single PUT.
 */
const LocalizationSection = ({
  testId,
  refsUrl,
  entityId,
  entity = "test",
}) => {
  // Panels and sample types keep their display names in the same generic
  // localization tables a test does, and each has its own endpoint bridging an id
  // to the backing localization row. The rest of this section is entity-agnostic
  // already, so they pass that endpoint in rather than duplicating any of it.
  const id = entityId !== undefined ? entityId : testId;
  const localizationRefsUrl =
    refsUrl || `/rest/test-catalog/tests/${id}/localization`;
  // Only the strings that name the thing being translated differ between
  // entities; everything else here reads the same for all three.
  const copy = (suffix) =>
    intl.formatMessage({
      id: `label.${entity}.localization.${suffix}`,
      defaultMessage: intl.formatMessage({
        id: `label.testCatalog.localization.${suffix}`,
      }),
    });
  const intl = useIntl();

  const [locales, setLocales] = useState([]);
  const [locale, setLocale] = useState("");
  // fields: [{ field, localizationId, translations: {locale: value} }]
  const [fields, setFields] = useState([]);
  // drafts: { "<locale>|<field>": value } — only what the admin actually typed
  const [drafts, setDrafts] = useState({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(false);
  const [notification, setNotification] = useState(null);

  // A request can land after the admin has switched tabs, so the async callbacks
  // below check this before setting state.
  const mounted = useRef(true);
  useEffect(
    () => () => {
      mounted.current = false;
    },
    [],
  );

  // Resolve the active supported locales, defaulting the picker to the locale the
  // session is running in (see resolveInitialLocale).
  useEffect(() => {
    getFromOpenElisServer("/rest/supportedlocales/active", (res) => {
      if (!mounted.current) {
        return;
      }
      const list = Array.isArray(res) ? res : [];
      setLocales(list);
      if (list.length > 0) {
        setLocale(resolveInitialLocale(list, intl.locale));
      }
    });
  }, [intl.locale]);

  // Load the test's localization refs, then hydrate each one's translations.
  const load = useCallback(() => {
    if (!id) {
      return;
    }
    setLoading(true);
    setError(false);
    getFromOpenElisServer(localizationRefsUrl, (refs) => {
      if (!mounted.current) {
        return;
      }
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
      // Indexed, not appended: these requests finish in any order, and the
      // fields must keep the order the server declared them in.
      const resolved = new Array(refs.fields.length);
      refs.fields.forEach((ref, index) => {
        getFromOpenElisServer(
          `/rest/localizations/${ref.localizationId}`,
          (loc) => {
            if (!mounted.current) {
              return;
            }
            resolved[index] = {
              field: ref.field,
              localizationId: ref.localizationId,
              translations: (loc && loc.translations) || {},
            };
            pending -= 1;
            if (pending === 0) {
              setFields(resolved);
              setLoading(false);
            }
          },
        );
      });
    });
  }, [id, localizationRefsUrl]);

  useEffect(() => {
    load();
  }, [load]);

  /** The value shown for a field: the admin's draft for this locale, else stored. */
  const valueFor = (entry) => {
    const draft = drafts[draftKey(locale, entry.field)];
    return draft !== undefined ? draft : entry.translations[locale] || "";
  };

  /**
   * Collapse the drafts into one PUT per localization record, dropping any that
   * match what is already stored.
   */
  const pendingSaves = () => {
    const byId = new Map();
    Object.keys(drafts).forEach((key) => {
      const separator = key.indexOf("|");
      const localeCode = key.slice(0, separator);
      const field = key.slice(separator + 1);
      const entry = fields.find((f) => f.field === field);
      if (!entry || drafts[key] === (entry.translations[localeCode] || "")) {
        return;
      }
      const batched = byId.get(entry.localizationId) || {
        entry,
        translations: { ...entry.translations },
      };
      batched.translations[localeCode] = drafts[key];
      byId.set(entry.localizationId, batched);
    });
    return Array.from(byId.values());
  };

  const unsaved = pendingSaves();

  const handleSave = () => {
    if (unsaved.length === 0 || saving) {
      return;
    }
    setSaving(true);
    setNotification(null);
    let pending = unsaved.length;
    let failed = 0;
    unsaved.forEach(({ entry, translations }) => {
      putToOpenElisServerFullResponse(
        `/rest/localizations/${entry.localizationId}/translations`,
        JSON.stringify(translations),
        (response) => {
          if (!response || !response.ok) {
            failed += 1;
          }
          pending -= 1;
          if (pending > 0 || !mounted.current) {
            return;
          }
          setSaving(false);
          if (failed > 0) {
            setNotification({
              kind: "error",
              text: intl.formatMessage({
                id: "label.testCatalog.localization.saveError",
              }),
            });
            return;
          }
          setNotification({
            kind: "success",
            text: intl.formatMessage({
              id: "label.testCatalog.localization.saved",
            }),
          });
          setDrafts({});
          load();
        },
      );
    });
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

  // True exactly when the input is empty and so showing the English placeholder.
  const isFallback = (entry) => locale !== FALLBACK_LOCALE && !valueFor(entry);

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

      <p>{copy("intro")}</p>

      {fields.length === 0 ? (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={copy("empty")}
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
                    id: `label.${entity}.localization.field.${entry.field}`,
                    defaultMessage: intl.formatMessage({
                      id: `label.testCatalog.localization.field.${entry.field}`,
                    }),
                  })}
                </span>
                {fallbackTag(entry)}
              </div>
              <TextInput
                id={`localization-input-${entry.field}`}
                labelText=""
                placeholder={entry.translations[FALLBACK_LOCALE] || ""}
                value={valueFor(entry)}
                onChange={(e) => {
                  // Read the value before the updater runs: by then React has
                  // reset this controlled input to its previous value.
                  const { value } = e.target;
                  const key = draftKey(locale, entry.field);
                  setDrafts((prev) => ({ ...prev, [key]: value }));
                }}
              />
            </div>
          ))}

          {unsaved.length > 0 && (
            <InlineNotification
              kind="warning"
              lowContrast
              hideCloseButton
              title={intl.formatMessage({
                id: "label.testCatalog.localization.unsaved",
              })}
            />
          )}

          <div style={{ display: "flex", gap: "0.5rem" }}>
            <Button
              kind="primary"
              disabled={saving || unsaved.length === 0}
              onClick={handleSave}
            >
              <FormattedMessage id="label.button.save" />
            </Button>
            <Button
              kind="ghost"
              disabled={saving || unsaved.length === 0}
              onClick={() => setDrafts({})}
            >
              <FormattedMessage id="label.button.cancel" />
            </Button>
          </div>
        </>
      )}
    </Stack>
  );
};

export default LocalizationSection;
