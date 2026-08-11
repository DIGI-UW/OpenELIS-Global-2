import React, { useCallback, useEffect, useState } from "react";
import { useHistory, useParams } from "react-router-dom";
import { Button, Column, Grid, Loading, Section, Tag } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import {
  DEFAULT_PANEL_SECTION,
  isValidPanelSection,
} from "./panelSectionConfig";
import PanelBasicInfoSection from "./sections/PanelBasicInfoSection";
import { domainTagType } from "./PanelsList";

/**
 * OGC-224 — the Panel editor shell (FRS v2.2). Same editor-shell pattern as a
 * test, with a PANEL entity badge; sections are SideNav submenus (rendered by
 * AdminSideNav from panelSectionConfig), never in-page tabs. Routes:
 * /MasterListsPage/TestCatalogEditor/panel/<id>/<section> — `new` opens a
 * blank panel (create-in-place, Basic Info only until it exists).
 */
const PanelEditor = () => {
  const intl = useIntl();
  const history = useHistory();
  const { panelId, section } = useParams();
  const basePath = history.location.pathname.startsWith("/admin")
    ? "/admin"
    : "/MasterListsPage";
  const isCreate = panelId === "new";

  const [panel, setPanel] = useState(null);
  const [loading, setLoading] = useState(!isCreate);

  const loadPanel = useCallback(() => {
    if (!panelId || isCreate) {
      return;
    }
    setLoading(true);
    getFromOpenElisServer(`/rest/test-catalog/panels/${panelId}`, (res) => {
      setLoading(false);
      setPanel(res || null);
    });
  }, [panelId, isCreate]);

  useEffect(loadPanel, [loadPanel]);

  // Canonicalize a missing/unknown section to basic-info, keeping deep links
  // fully formed (same contract as the test editor).
  useEffect(() => {
    if (!section || !isValidPanelSection(section)) {
      history.replace(
        `${basePath}/TestCatalogEditor/panel/${panelId}/${DEFAULT_PANEL_SECTION}`,
      );
    }
  }, [section, panelId, basePath, history]);

  const backToList = () =>
    history.push(`${basePath}/TestCatalogList?entity=panels`);

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
    {
      label: "label.testCatalog.entity.panels",
      link: "/MasterListsPage/TestCatalogList?entity=panels",
    },
  ];

  const activeSection = isValidPanelSection(section)
    ? section
    : DEFAULT_PANEL_SECTION;

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: "0.75rem",
                flexWrap: "wrap",
              }}
            >
              <Button kind="ghost" size="sm" onClick={backToList}>
                ← <FormattedMessage id="label.panel.backToList" />
              </Button>
              <h3 style={{ margin: 0 }} data-testid="panel-editor-title">
                {isCreate
                  ? intl.formatMessage({ id: "title.panel.new" })
                  : panel?.name || ""}
              </h3>
              <Tag type="purple" size="sm">
                PANEL
              </Tag>
              {!isCreate && panel?.domain && (
                <Tag type={domainTagType(panel.domain)} size="sm">
                  {intl.formatMessage({
                    id: `label.domain.${panel.domain}`,
                    defaultMessage: panel.domain,
                  })}
                </Tag>
              )}
              {!isCreate && panel?.loinc && (
                <code data-testid="panel-editor-loinc">{panel.loinc}</code>
              )}
            </div>
          </Section>
          {loading ? (
            <Loading small withOverlay={false} />
          ) : (
            activeSection === "basic-info" && (
              <PanelBasicInfoSection
                panel={panel}
                isCreate={isCreate}
                onSaved={(saved) => {
                  if (isCreate && saved?.id) {
                    history.replace(
                      `${basePath}/TestCatalogEditor/panel/${saved.id}/${DEFAULT_PANEL_SECTION}`,
                    );
                  } else {
                    setPanel(saved);
                  }
                }}
              />
            )
          )}
        </Column>
      </Grid>
    </>
  );
};

export default PanelEditor;
