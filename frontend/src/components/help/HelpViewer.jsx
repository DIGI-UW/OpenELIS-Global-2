import React, { useEffect, useMemo } from "react";
import { Link, useHistory, useLocation, useParams } from "react-router-dom";
import { useIntl } from "react-intl";
import {
  Button,
  InlineLoading,
  InlineNotification,
  SideNav,
  SideNavItems,
  SideNavLink,
  SideNavMenu,
  Tag,
} from "@carbon/react";

import { useHelpManifest, useHelpPage } from "./useHelpData";

const normaliseLocale = (locale) =>
  (locale || "en").split("-")[0].toLowerCase();

const landingSlug = (manifest) => {
  const userGuide = manifest?.guides?.user;
  return userGuide?.[0]?.slug ?? null;
};

const guideKeyFor = (guide) =>
  guide === "admin" ? "help.viewer.guide.admin" : "help.viewer.guide.user";

export default function HelpViewer() {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const { slug } = useParams();

  const activeLocale = normaliseLocale(intl.locale);
  const manifestState = useHelpManifest(activeLocale, "en");
  const { manifest, locale: effectiveLocale, usingFallback } = manifestState;

  const resolvedSlug = slug || (manifest ? landingSlug(manifest) : null);
  const pageState = useHelpPage(manifest, null, resolvedSlug, effectiveLocale);

  // Deep links: scroll to #heading-id once the page HTML is in the DOM.
  useEffect(() => {
    if (pageState.status !== "ready") return;
    const hash = location.hash?.replace(/^#/, "");
    if (!hash) return;
    const el = document.getElementById(hash);
    if (el) el.scrollIntoView({ behavior: "auto", block: "start" });
  }, [pageState.status, location.hash]);

  const sideNavItems = useMemo(() => {
    if (!manifest?.guides) return [];
    return Object.entries(manifest.guides).map(([guide, pages]) => ({
      guide,
      label: intl.formatMessage({ id: guideKeyFor(guide) }),
      pages,
    }));
  }, [manifest, intl]);

  if (manifestState.status === "loading") {
    return (
      <InlineLoading
        description={intl.formatMessage({ id: "help.viewer.loading" })}
      />
    );
  }

  if (manifestState.status === "error") {
    return (
      <InlineNotification
        kind="error"
        title={intl.formatMessage({ id: "help.viewer.error.title" })}
        actions={
          <Button kind="ghost" onClick={manifestState.reload}>
            {intl.formatMessage({ id: "help.viewer.error.retry" })}
          </Button>
        }
      />
    );
  }

  return (
    <div className="help-viewer" data-testid="help-viewer">
      <SideNav
        aria-label={intl.formatMessage({ id: "help.viewer.title" })}
        isPersistent
      >
        <SideNavItems>
          {sideNavItems.map(({ guide, label, pages }) => (
            <SideNavMenu key={guide} title={label} defaultExpanded>
              {pages.map((p) => (
                <SideNavLink
                  key={p.slug}
                  element={Link}
                  to={`/help/${p.slug}`}
                  isActive={p.slug === resolvedSlug}
                >
                  {p.title}
                </SideNavLink>
              ))}
            </SideNavMenu>
          ))}
        </SideNavItems>
      </SideNav>

      <main className="help-viewer__content" data-testid="help-viewer-content">
        <HelpViewerHeader
          onReturn={() => {
            if (window.opener) window.close();
            else history.push("/");
          }}
        />
        {usingFallback || pageState.usingFallback ? (
          <InlineNotification
            kind="info"
            hideCloseButton
            lowContrast
            title={intl.formatMessage({ id: "help.viewer.fallback.english" })}
            subtitle={intl.formatMessage({ id: "help.viewer.fallback.notice" })}
          />
        ) : null}
        <PageBody
          state={pageState}
          intl={intl}
          usingFallback={usingFallback || pageState.usingFallback}
        />
      </main>
    </div>
  );
}

function HelpViewerHeader({ onReturn }) {
  const intl = useIntl();
  return (
    <div className="help-viewer__header" data-testid="help-viewer-header">
      <h1>{intl.formatMessage({ id: "help.viewer.title" })}</h1>
      <Button kind="ghost" size="sm" onClick={onReturn}>
        {intl.formatMessage({ id: "help.viewer.returnLabel" })}
      </Button>
    </div>
  );
}

function PageBody({ state, intl, usingFallback }) {
  if (state.status === "loading" || state.status === "idle") {
    return (
      <InlineLoading
        description={intl.formatMessage({ id: "help.viewer.loading" })}
      />
    );
  }
  if (state.status === "notfound") {
    return (
      <InlineNotification
        kind="warning"
        hideCloseButton
        lowContrast
        title={intl.formatMessage({ id: "help.viewer.notfound.title" })}
        subtitle={intl.formatMessage({ id: "help.viewer.notfound.body" })}
      />
    );
  }
  if (state.status === "error") {
    return (
      <InlineNotification
        kind="error"
        hideCloseButton
        title={intl.formatMessage({ id: "help.viewer.error.title" })}
        subtitle={state.error}
      />
    );
  }

  return (
    <article>
      <header className="help-viewer__page-head">
        <h2>{state.page.title}</h2>
        {usingFallback ? (
          <Tag type="blue">
            {intl.formatMessage({ id: "help.viewer.fallback.english" })}
          </Tag>
        ) : null}
      </header>
      {/*
        Pipeline writes sanitize-html output (sanitize-html allowlist in
        frontend/scripts/help-refresh.mjs, FRS §7). We render that as-is; no
        user-provided HTML reaches here.
      */}
      <div
        className="help-viewer__page-body"
        dangerouslySetInnerHTML={{ __html: state.page.html }}
      />
    </article>
  );
}
