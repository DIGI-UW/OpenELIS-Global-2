import React, { useContext, useEffect, useState } from "react";
import { useParams, useHistory, useLocation } from "react-router-dom";
import {
  Grid,
  Column,
  Section,
  Heading,
  Button,
  Loading,
  InlineNotification,
  Tile,
} from "@carbon/react";
import { ArrowLeft } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { AlertDialog } from "../../common/CustomNotification";
import { NotificationContext } from "../../layout/Layout";
import BasicInfoSection from "./sections/BasicInfoSection";
import SampleResultsSection from "./sections/SampleResultsSection";
import MethodsSection from "./sections/MethodsSection";
import RangesSection from "./sections/RangesSection";
import StorageSection from "./sections/StorageSection";
import AnalyzersSection from "./sections/AnalyzersSection";
import DisplayOrderSection from "./sections/DisplayOrderSection";
import TerminologySection from "./sections/TerminologySection";
import PanelsSection from "./sections/PanelsSection";
import ReagentsSection from "./sections/ReagentsSection";
import LabelsSection from "./sections/LabelsSection";
import AlertsSection from "./sections/AlertsSection";
import ReflexCalcSection from "./sections/ReflexCalcSection";
import LocalizationSection from "./sections/LocalizationSection";
import { DEFAULT_SECTION, isValidSection } from "./sectionConfig";

/**
 * OGC-949 M2 / OGC-927 — unified Test Catalog editor shell.
 *
 * SideNav-routed shell (#3504): the active section is a URL segment
 * (.../TestCatalogEditor/:testId/:section), so sections are deep-linkable and
 * back-button-friendly. The section navigation itself lives in the global
 * AdminSideNav (one sidenav, no editor-owned nav). All nine v1 sections are
 * built and URL-routed (M4–M12); an unknown/invalid section canonicalizes to
 * the default, and the final ternary branch is a defensive fallback. ADMIN-gated
 * by the SecureRoute (and the REST API 403s non-admins — see
 * TestCatalogEditorRestController).
 */
const TestCatalogEditor = () => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const { testId, section } = useParams();
  const base = location.pathname.startsWith("/admin")
    ? "/admin"
    : "/MasterListsPage";
  const { addNotification, setNotificationVisible, notificationVisible } =
    useContext(NotificationContext);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(false);
  const [envelope, setEnvelope] = useState(null);

  // Create-in-place (FR-2): testId "new" opens a blank Basic Info, no fetch.
  const isCreate = testId === "new";
  // The active section is driven entirely by the URL.
  const activeSection = isValidSection(section) ? section : DEFAULT_SECTION;

  useEffect(() => {
    if (!testId || isCreate) {
      return;
    }
    setLoading(true);
    setError(false);
    getFromOpenElisServer(`/rest/test-catalog/tests/${testId}`, handleEnvelope);
  }, [testId, isCreate]);

  // Canonicalize the section into the URL so deep-links + the SideNav agree.
  useEffect(() => {
    if (testId && (!section || !isValidSection(section))) {
      history.replace(`${base}/TestCatalogEditor/${testId}/${DEFAULT_SECTION}`);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [testId, section]);

  const handleEnvelope = (res) => {
    setLoading(false);
    if (!res) {
      setError(true);
      return;
    }
    setEnvelope(res);
  };

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "breadcrums.admin.managment", link: base },
    {
      label: "label.testCatalog.editor",
      link: `${base}/TestCatalogList`,
    },
  ];

  const handleCancel = () => {
    history.push(`${base}/TestCatalogList`);
  };

  // FR-7: open the combined editor over this test's specimen siblings (tests
  // sharing its name stem). Falls back to a notice when there are none.
  const editRelatedTests = () => {
    getFromOpenElisServer(
      `/rest/test-catalog/tests/${testId}/siblings`,
      (res) => {
        const ids = Array.isArray(res) ? res.map((r) => r.testId) : [];
        if (ids.length >= 2) {
          history.push(
            `${base}/TestCatalogEditor/group/${ids.join(",")}/ranges`,
          );
        } else {
          setNotificationVisible(true);
          addNotification({
            kind: "info",
            title: intl.formatMessage({ id: "label.testCatalog.editor" }),
            message: intl.formatMessage({
              id: "label.testCatalog.editor.noRelated",
            }),
          });
        }
      },
    );
  };

  // Empty state: no test selected (the list view, M3/OGC-928, links here with a testId).
  if (!testId) {
    return (
      <>
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                <FormattedMessage id="label.testCatalog.editor" />
              </Heading>
            </Section>
            <InlineNotification
              kind="info"
              lowContrast
              hideCloseButton
              title={intl.formatMessage({
                id: "label.testCatalog.editor.empty",
              })}
              subtitle={intl.formatMessage({
                id: "label.testCatalog.editor.empty.helper",
              })}
            />
          </Column>
        </Grid>
      </>
    );
  }

  if (loading) {
    return <Loading description="Loading" withOverlay={false} />;
  }

  if (error) {
    return (
      <>
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <InlineNotification
              kind="error"
              lowContrast
              hideCloseButton
              title={intl.formatMessage({ id: "error.title" })}
              subtitle={intl.formatMessage({
                id: "label.testCatalog.editor.loadError",
              })}
            />
          </Column>
        </Grid>
      </>
    );
  }

  return (
    <>
      {/* Sections raise toasts via NotificationContext; the page must render
          the AlertDialog for them to be visible (app-wide pattern). */}
      {notificationVisible === true && <AlertDialog />}
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth>
        <Column lg={12} md={6} sm={4}>
          <Section>
            <Heading>
              {isCreate ? (
                <FormattedMessage id="title.testCatalog.createTest" />
              ) : (
                envelope?.name || (
                  <FormattedMessage id="label.testCatalog.editor" />
                )
              )}
            </Heading>
          </Section>
        </Column>
        <Column
          lg={4}
          md={2}
          sm={4}
          style={{
            display: "flex",
            justifyContent: "flex-end",
            alignItems: "flex-start",
          }}
        >
          <Button
            kind="ghost"
            size="sm"
            data-testid="test-editor-back-to-list"
            renderIcon={ArrowLeft}
            onClick={handleCancel}
          >
            <FormattedMessage id="sidenav.label.admin.testCatalog.backToList" />
          </Button>
        </Column>

        {/* Header actions. Saving is per-section (each section owns its own Save),
            so the header exposes only cross-cutting navigation actions — no header
            Save. "Save as new test" belongs to the Add-specimen-variant flow (FR-52)
            and is surfaced there, not as a header placeholder (FR-78). */}
        {!isCreate && (
          <Column lg={16} md={8} sm={4}>
            <div style={{ display: "flex", gap: "0.5rem", margin: "1rem 0" }}>
              <Button
                kind="ghost"
                data-testid="edit-related-tests"
                onClick={editRelatedTests}
              >
                <FormattedMessage id="button.testCatalog.editRelatedFromEditor" />
              </Button>
              <Button kind="ghost" onClick={handleCancel}>
                <FormattedMessage id="label.button.cancel" />
              </Button>
            </div>
          </Column>
        )}

        {/* Section nav lives in the global AdminSideNav (URL-routed, #3504) —
            the editor renders only the active section's content, full width. */}
        <Column lg={16} md={8} sm={4}>
          <Tile>
            <Heading>
              <FormattedMessage
                id={`label.testCatalog.section.${
                  isCreate ? "basic-info" : activeSection
                }`}
              />
            </Heading>
            <div style={{ marginTop: "1rem" }}>
              {isCreate ? (
                // Create-in-place: only Basic Info is usable; the other sections
                // need a persisted test, so guide the user to save first (FR-3)
                // rather than showing Basic Info under another section's title.
                activeSection === "basic-info" ? (
                  <BasicInfoSection testId={testId} />
                ) : (
                  <InlineNotification
                    kind="info"
                    lowContrast
                    hideCloseButton
                    title={intl.formatMessage({
                      id: "label.testCatalog.editor.createSaveFirst.title",
                    })}
                    subtitle={intl.formatMessage({
                      id: "label.testCatalog.editor.createSaveFirst",
                    })}
                  />
                )
              ) : activeSection === "basic-info" ? (
                <BasicInfoSection testId={testId} />
              ) : activeSection === "sample-results" ? (
                <SampleResultsSection testId={testId} />
              ) : activeSection === "methods" ? (
                <MethodsSection testId={testId} />
              ) : activeSection === "ranges" ? (
                <RangesSection testId={testId} />
              ) : activeSection === "storage" ? (
                <StorageSection testId={testId} />
              ) : activeSection === "analyzers" ? (
                <AnalyzersSection testId={testId} />
              ) : activeSection === "display-order" ? (
                <DisplayOrderSection testId={testId} />
              ) : activeSection === "terminology" ? (
                <TerminologySection testId={testId} />
              ) : activeSection === "panels" ? (
                <PanelsSection testId={testId} />
              ) : activeSection === "reagents" ? (
                <ReagentsSection testId={testId} />
              ) : activeSection === "labels" ? (
                <LabelsSection testId={testId} />
              ) : activeSection === "alerts" ? (
                <AlertsSection testId={testId} />
              ) : activeSection === "reflex-calc" ? (
                <ReflexCalcSection testId={testId} />
              ) : activeSection === "localization" ? (
                <LocalizationSection testId={testId} />
              ) : (
                <p>
                  <FormattedMessage id="label.testCatalog.section.pending" />
                </p>
              )}
            </div>
          </Tile>
        </Column>
      </Grid>
    </>
  );
};

export default TestCatalogEditor;
