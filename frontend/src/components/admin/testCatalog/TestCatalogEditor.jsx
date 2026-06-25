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
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import PageBreadCrumb from "../../common/PageBreadCrumb";
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
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(false);
  const [envelope, setEnvelope] = useState(null);

  // The active section is driven entirely by the URL.
  const activeSection = isValidSection(section) ? section : DEFAULT_SECTION;

  useEffect(() => {
    if (!testId) {
      return;
    }
    setLoading(true);
    setError(false);
    getFromOpenElisServer(`/rest/test-catalog/tests/${testId}`, handleEnvelope);
  }, [testId]);

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

  const handleSavePlaceholder = (messageId) => {
    // Section save + clone are wired in their own milestones (M4+ / OGC-944).
    setNotificationVisible(true);
    addNotification({
      kind: "info",
      title: intl.formatMessage({ id: "label.testCatalog.editor" }),
      message: intl.formatMessage({ id: messageId }),
    });
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
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              {envelope?.name || (
                <FormattedMessage id="label.testCatalog.editor" />
              )}
            </Heading>
          </Section>
        </Column>

        {/* Header CTAs (Save / Save as new test… / Cancel). Save + clone wire in M4+/OGC-944. */}
        <Column lg={16} md={8} sm={4}>
          <div style={{ display: "flex", gap: "0.5rem", margin: "1rem 0" }}>
            <Button
              kind="primary"
              onClick={() =>
                handleSavePlaceholder("label.testCatalog.editor.save.pending")
              }
            >
              <FormattedMessage id="label.button.save" />
            </Button>
            <Button
              kind="secondary"
              data-cy="save-as-new-test"
              onClick={() =>
                handleSavePlaceholder("label.testCatalog.editor.clone.pending")
              }
            >
              <FormattedMessage id="label.testCatalog.editor.saveAsNew" />
            </Button>
            <Button kind="ghost" onClick={handleCancel}>
              <FormattedMessage id="label.button.cancel" />
            </Button>
          </div>
        </Column>

        {/* Section nav lives in the global AdminSideNav (URL-routed, #3504) —
            the editor renders only the active section's content, full width. */}
        <Column lg={16} md={8} sm={4}>
          <Tile>
            <Heading>
              <FormattedMessage
                id={`label.testCatalog.section.${activeSection}`}
              />
            </Heading>
            <div style={{ marginTop: "1rem" }}>
              {activeSection === "basic-info" ? (
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
