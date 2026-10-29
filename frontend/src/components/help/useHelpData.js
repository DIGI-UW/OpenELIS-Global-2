import { useCallback, useEffect, useState } from "react";

// Where the Help Refresh pipeline (frontend/scripts/help-refresh.mjs) writes
// its output. The viewer loads a per-locale manifest, then streams individual
// page HTML files on demand. Everything under /help-content/ is static so a
// disconnected deployment still serves it (FRS AC 15).
const HELP_CONTENT_ROOT = "/help-content";

const manifestUrl = (locale) => `${HELP_CONTENT_ROOT}/${locale}/manifest.json`;
const pageUrl = (locale, guide, slug) =>
  `${HELP_CONTENT_ROOT}/${locale}/${guide}/${slug}.html`;

const findSlug = (manifest, slug) => {
  if (!manifest?.guides) return null;
  for (const [guide, pages] of Object.entries(manifest.guides)) {
    const page = pages.find((p) => p.slug === slug);
    if (page) return { guide, page };
  }
  return null;
};

export function useHelpManifest(locale, fallbackLocale) {
  const [state, setState] = useState({
    status: "loading",
    manifest: null,
    fallbackManifest: null,
    locale,
    usingFallback: false,
  });

  const load = useCallback(async () => {
    setState((s) => ({ ...s, status: "loading" }));
    try {
      const res = await fetch(manifestUrl(locale));
      if (res.ok) {
        const manifest = await res.json();
        setState({
          status: "ready",
          manifest,
          fallbackManifest: null,
          locale,
          usingFallback: false,
        });
        return;
      }
    } catch {
      // fall through to fallback
    }
    if (fallbackLocale && fallbackLocale !== locale) {
      try {
        const res = await fetch(manifestUrl(fallbackLocale));
        if (res.ok) {
          const manifest = await res.json();
          setState({
            status: "ready",
            manifest,
            fallbackManifest: null,
            locale: fallbackLocale,
            usingFallback: true,
          });
          return;
        }
      } catch {
        // fall through
      }
    }
    setState({
      status: "error",
      manifest: null,
      fallbackManifest: null,
      locale,
      usingFallback: false,
    });
  }, [locale, fallbackLocale]);

  useEffect(() => {
    load();
  }, [load]);

  return { ...state, reload: load };
}

export function useHelpPage(manifest, fallbackManifest, slug, locale) {
  const [state, setState] = useState({ status: "idle", page: null });

  useEffect(() => {
    if (!manifest) return;
    const inLocale = findSlug(manifest, slug);
    const inFallback = findSlug(fallbackManifest, slug);
    const resolved = inLocale || inFallback;
    if (!resolved) {
      setState({ status: "notfound", page: null });
      return;
    }
    const usingFallback = !inLocale && Boolean(inFallback);
    const effectiveLocale = usingFallback
      ? (fallbackManifest.locale ?? "en")
      : locale;

    let cancelled = false;
    setState({ status: "loading", page: null, usingFallback });
    (async () => {
      try {
        const res = await fetch(
          pageUrl(effectiveLocale, resolved.guide, resolved.page.slug),
        );
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const html = await res.text();
        if (!cancelled) {
          setState({
            status: "ready",
            page: { ...resolved.page, guide: resolved.guide, html },
            usingFallback,
          });
        }
      } catch (err) {
        if (!cancelled) {
          setState({
            status: "error",
            page: null,
            error: err.message,
            usingFallback,
          });
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [manifest, fallbackManifest, slug, locale]);

  return state;
}

export const helpTestInternals = { findSlug, HELP_CONTENT_ROOT };
