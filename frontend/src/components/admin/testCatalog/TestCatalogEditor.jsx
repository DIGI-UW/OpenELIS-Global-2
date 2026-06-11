import React, { useContext, useEffect, useState } from "react";
import { useParams, useHistory } from "react-router-dom";
import {
  Grid,
  Column,
  Section,
  Heading,
  Button,
  Loading,
  InlineNotification,
  SideNav,
  SideNavItems,
  SideNavLink,
  Tile,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { NotificationContext } from "../../layout/Layout";
import BasicInfoSection from "./sections/BasicInfoSection";

/**
 * OGC-949 M2 / OGC-927 — unified Test Catalog editor shell.
 *
 * SideNav-routed shell that loads a test's editor envelope (identity + which
 * sections apply for its domain) and mounts each section. Per-section UIs land
 * in their own milestones (M4+); until then a section renders a placeholder.
 * The whole surface is ADMIN-gated by the SecureRoute that renders it (and the
 * REST API returns 403 for non-admins — see TestCatalogEditorRestController).
 */

// v1 section keys in SideNav order; labels resolved via i18n.
const V1_SECTIONS = [
  "basic-info",
  "sample-results",
  "methods",
  "ranges",
  "storage",
  "panels",
  "terminology",
  "analyzers",
  "display-order",
];

const TestCatalogEditor = () => {
  const intl = useIntl();
  const history = useHistory();
  const { testId } = useParams();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(false);
  const [envelope, setEnvelope] = useState(null);
  const [activeSection, setActiveSection] = useState(V1_SECTIONS[0]);

  useEffect(() => {
    if (!testId) {
      return;
    }
    setLoading(true);
    setError(false);
    getFromOpenElisServer(`/rest/test-catalog/tests/${testId}`, handleEnvelope);
  }, [testId]);

  const handleEnvelope = (res) => {
    setLoading(false);
    if (!res) {
      setError(true);
      return;
    }
    setEnvelope(res);
    const sections = res.applicableSections || V1_SECTIONS;
    setActiveSection(sections[0]);
  };

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
    {
      label: "label.testCatalog.editor",
      link: "/MasterListsPage/TestCatalogEditor",
    },
  ];

  const handleCancel = () => {
    history.push("/MasterListsPage");
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

  const sections = envelope?.applicableSections || V1_SECTIONS;

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

        <Column lg={4} md={2} sm={4}>
          <SideNav
            isFixedNav
            expanded
            isChildOfHeader={false}
            aria-label="Test Catalog sections"
          >
            <SideNavItems>
              {sections.map((sectionKey) => (
                <SideNavLink
                  key={sectionKey}
                  isActive={activeSection === sectionKey}
                  onClick={() => setActiveSection(sectionKey)}
                  href="#"
                  data-cy={`section-${sectionKey}`}
                >
                  <FormattedMessage
                    id={`label.testCatalog.section.${sectionKey}`}
                  />
                </SideNavLink>
              ))}
            </SideNavItems>
          </SideNav>
        </Column>

        <Column lg={12} md={6} sm={4}>
          <Tile>
            <Heading>
              <FormattedMessage
                id={`label.testCatalog.section.${activeSection}`}
              />
            </Heading>
            <div style={{ marginTop: "1rem" }}>
              {activeSection === "basic-info" ? (
                <BasicInfoSection testId={testId} />
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
