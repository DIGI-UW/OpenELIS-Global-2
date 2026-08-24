import React, { useEffect, useState } from "react";
import config from "../../config.json";
import { FormattedMessage, useIntl } from "react-intl";
import { useHistory, useLocation } from "react-router-dom";
import { getFromOpenElisServer } from "../utils/Utils";
import {
  ArrowLeft,
  Microscope,
  CharacterWholeNumber,
  TableOfContents,
  ChartBubble,
  Catalog,
  Settings,
  ListDropdown,
  CicsSystemGroup,
  QrCode,
  ContainerSoftware,
  BootVolumeAlt,
  Report,
  Bullhorn,
  User,
  BatchJob,
  ResultNew,
  Popup,
  Search,
  ConnectionSignal,
  Calendar,
  TrashCan,
  Sprout,
  ListChecked,
} from "@carbon/icons-react";
import {
  SideNavItems,
  SideNavLink,
  SideNavMenu,
  SideNavMenuItem,
} from "@carbon/react";
import { V1_SECTIONS } from "./testCatalog/sectionConfig";
import { SAMPLE_TYPE_SECTIONS } from "./sampleTypeManagement/sectionConfig";
import { PANEL_SECTIONS } from "./testCatalog/panelSectionConfig";
import { LAB_UNIT_SECTIONS } from "./labUnitManagement/sectionConfig";

const getAdminBasePath = (pathname) =>
  pathname.startsWith("/admin") ? "/admin" : "/MasterListsPage";

const normalizePath = (path) => {
  if (!path) {
    return "";
  }
  const pathOnly = path.split(/[?#]/)[0] || "";
  return pathOnly.length > 1 && pathOnly.endsWith("/")
    ? pathOnly.slice(0, -1)
    : pathOnly;
};

export default function AdminSideNav({ isTrainingInstallation = false }) {
  const intl = useIntl();
  /**
   * The caption above the sections list. Greyed sections without one leave a
   * reader no way to know what to do next.
   */
  const sectionsCaption = (id, dataCy, messageId, values) => (
    <li
      id={id}
      data-cy={dataCy}
      className="adminSideNav__sectionsContext"
      style={{
        padding: "0.25rem 1rem 0.5rem",
        fontSize: "0.75rem",
        lineHeight: 1.3,
        color: "var(--cds-text-secondary, #6f6f6f)",
      }}
    >
      <FormattedMessage id={messageId} values={values} />
    </li>
  );

  /** A section the reader cannot open yet, described by the caption above it. */
  const disabledSection = (dataCy, describedBy, label) => (
    <SideNavMenuItem
      key={dataCy}
      data-cy={dataCy}
      aria-disabled="true"
      aria-describedby={describedBy}
      tabIndex={-1}
      onClick={(e) => e.preventDefault()}
      style={{ opacity: 0.5, cursor: "not-allowed" }}
    >
      {label}
    </SideNavMenuItem>
  );

  const history = useHistory();
  const location = useLocation();
  const path = getAdminBasePath(location.pathname);

  // OGC-224 — /TestCatalogEditor/panel/:id is the PANEL editor, not a test id.
  const panelEditorMatch = location.pathname.match(
    /\/TestCatalogEditor\/panel\/([^/]+)/,
  );
  const editorPanelId = panelEditorMatch ? panelEditorMatch[1] : null;

  const editorMatch = editorPanelId
    ? null
    : location.pathname.match(/\/TestCatalogEditor\/([^/]+)/);
  const editorTestId = editorMatch ? editorMatch[1] : null;

  // Sample Type editor context: /SampleTypeEditor/:sampleTypeId/:section?
  // The plain list URL (no trailing id) leaves this null.
  const sampleTypeEditorMatch = location.pathname.match(
    /\/SampleTypeEditor\/([^/]+)/,
  );
  const editorSampleTypeId = sampleTypeEditorMatch
    ? sampleTypeEditorMatch[1]
    : null;

  // Which entity the shell is showing, whether or not one is selected yet. The
  // sections list needs this: with nothing selected the panels and sample types
  // contexts used to fall through to the tests branch, which greyed out the
  // test sections and told the reader to pick a test.
  const inPanelsContext =
    !!editorPanelId || /[?&]entity=panels(&|$)/.test(location.search);
  const inSampleTypesContext =
    !!editorSampleTypeId || /\/SampleTypeEditor(\/|$)/.test(location.pathname);

  // Lab Unit editor context: /LabUnitManagement/:labUnitId/:section?
  // The plain list URL (no trailing id) leaves this null.
  const labUnitEditorMatch = location.pathname.match(
    /\/LabUnitManagement\/([^/]+)/,
  );
  const editorLabUnitId = labUnitEditorMatch ? labUnitEditorMatch[1] : null;

  // Keyed by id so the label never shows a prior test's name while the next loads.
  const [editorTest, setEditorTest] = useState({ id: null, name: null });
  useEffect(() => {
    // "new" (create-in-place) and "group" (combined editor) are not real test
    // ids — don't fetch an envelope for them (it would 500).
    if (!editorTestId || editorTestId === "new" || editorTestId === "group") {
      return undefined;
    }
    const controller = new AbortController();
    getFromOpenElisServer(
      `/rest/test-catalog/tests/${editorTestId}`,
      (res) => {
        setEditorTest({ id: editorTestId, name: res?.name || null });
      },
      controller.signal,
    );
    return () => {
      controller.abort();
    };
  }, [editorTestId]);
  const editorTestName =
    editorTest.id === editorTestId ? editorTest.name : null;

  // Sample type name for the sidenav helper caption. "new" is create-in-place.
  const [editorSampleType, setEditorSampleType] = useState({
    id: null,
    name: null,
  });
  useEffect(() => {
    if (!editorSampleTypeId || editorSampleTypeId === "new") {
      return undefined;
    }
    const controller = new AbortController();
    getFromOpenElisServer(
      "/rest/sample-types",
      (res) => {
        const list =
          res && res.success && Array.isArray(res.data)
            ? res.data
            : Array.isArray(res)
              ? res
              : [];
        const match = list.find(
          (item) => String(item.id) === String(editorSampleTypeId),
        );
        setEditorSampleType({
          id: editorSampleTypeId,
          name: match ? match.name || match.description || null : null,
        });
      },
      controller.signal,
    );
    return () => {
      controller.abort();
    };
  }, [editorSampleTypeId]);
  const editorSampleTypeName =
    editorSampleType.id === editorSampleTypeId ? editorSampleType.name : null;

  // Lab unit name for the sidenav helper caption. "new" is create-in-place.
  const [editorLabUnit, setEditorLabUnit] = useState({ id: null, name: null });
  useEffect(() => {
    if (!editorLabUnitId || editorLabUnitId === "new") {
      return undefined;
    }
    const controller = new AbortController();
    getFromOpenElisServer(
      "/rest/lab-units-management",
      (res) => {
        const list =
          res && res.success && Array.isArray(res.data)
            ? res.data
            : Array.isArray(res)
              ? res
              : [];
        const match = list.find(
          (item) => String(item.id) === String(editorLabUnitId),
        );
        setEditorLabUnit({
          id: editorLabUnitId,
          name: match ? match.name || match.description || null : null,
        });
      },
      controller.signal,
    );
    return () => {
      controller.abort();
    };
  }, [editorLabUnitId]);
  const editorLabUnitName =
    editorLabUnit.id === editorLabUnitId ? editorLabUnit.name : null;

  // Any Test Catalog Management surface (list or editor, either entity). The
  // menu stays mounted (same key) and expanded across every in-area
  // navigation, so clicking "All Sample Types"/"All Tests" from an editor
  // doesn't collapse it.
  const inTestCatalogArea =
    !!editorTestId ||
    !!editorSampleTypeId ||
    !!editorPanelId ||
    !!editorLabUnitId ||
    /\/(TestCatalogList|SampleTypeEditor|LabUnitManagement)(\/|$)/.test(
      location.pathname,
    );

  // Panel name for the sidenav helper caption. "new" is create-in-place.
  const [editorPanel, setEditorPanel] = useState({ id: null, name: null });
  useEffect(() => {
    if (!editorPanelId || editorPanelId === "new") {
      return undefined;
    }
    const controller = new AbortController();
    getFromOpenElisServer(
      `/rest/test-catalog/panels/${editorPanelId}`,
      (res) => {
        setEditorPanel({ id: editorPanelId, name: res?.name || null });
      },
      controller.signal,
    );
    return () => {
      controller.abort();
    };
  }, [editorPanelId]);
  const editorPanelName =
    editorPanel.id === editorPanelId ? editorPanel.name : null;

  const handleNavigation = (targetPath) => (e) => {
    e.preventDefault();
    history.push(targetPath);
  };

  const navProps = (targetPath) => {
    const isActive =
      normalizePath(location.pathname) === normalizePath(targetPath);
    return {
      href: targetPath,
      isActive,
      "aria-current": isActive ? "page" : undefined,
      onClick: handleNavigation(targetPath),
    };
  };

  // OGC-224 — the list route hosts two entities (?entity=panels); the two
  // entity links disambiguate on the query string so only one lights up.
  const onPanelsList =
    new URLSearchParams(location.search).get("entity") === "panels";
  const entityListNavProps = (targetPath, isActive) => ({
    href: targetPath,
    isActive,
    "aria-current": isActive ? "page" : undefined,
    onClick: handleNavigation(targetPath),
  });
  const testsListNavProps = (targetPath) =>
    entityListNavProps(
      targetPath,
      normalizePath(location.pathname) === normalizePath(targetPath) &&
        !onPanelsList,
    );
  const panelsListNavProps = (targetPath) =>
    entityListNavProps(
      targetPath,
      /\/TestCatalogList(\/|$)/.test(location.pathname) && onPanelsList,
    );

  return (
    <SideNavItems className="adminSideNav">
      <SideNavLink
        data-testid="admin-back-to-main-nav"
        renderIcon={ArrowLeft}
        {...navProps("/Dashboard")}
      >
        <FormattedMessage id="sidenav.label.admin.backToMainMenu" />
      </SideNavLink>
      <SideNavMenu
        data-cy="reflexTestsConfig"
        renderIcon={Microscope}
        title={intl.formatMessage({ id: "sidenav.label.admin.testmgt" })}
      >
        <SideNavMenuItem data-cy="reflex" {...navProps(`${path}/reflex`)}>
          <FormattedMessage id="sidenav.label.admin.testmgt.reflex" />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="calculatedValue"
          {...navProps(`${path}/calculatedValue`)}
        >
          <FormattedMessage id="sidenav.label.admin.testmgt.calculated" />
        </SideNavMenuItem>
      </SideNavMenu>
      {/* key flips on entering/leaving the Test Catalog area to force a
          remount — Carbon SideNavMenu reads defaultExpanded only at mount.
          Within the area the key is stable, so navigating between the lists
          and either editor never collapses the menu. */}
      <SideNavMenu
        key={inTestCatalogArea ? "testcatalog-area" : "testcatalog"}
        data-cy="testCatalogManagement"
        renderIcon={Catalog}
        isActive={inTestCatalogArea}
        defaultExpanded={inTestCatalogArea}
        title={intl.formatMessage({ id: "sidenav.label.admin.testCatalog" })}
      >
        {/* Entity links always come first, in both editor contexts. */}
        <SideNavMenuItem
          data-cy="sampleTypeManagement"
          {...navProps(`${path}/SampleTypeEditor`)}
        >
          <FormattedMessage
            id={
              editorSampleTypeId
                ? "sidenav.label.admin.sampleType.backToList"
                : "sidenav.label.admin.sampleTypeManagement"
            }
          />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="testCatalogList"
          {...testsListNavProps(`${path}/TestCatalogList`)}
        >
          <FormattedMessage
            id={
              editorTestId
                ? "sidenav.label.admin.testCatalog.backToList"
                : "sidenav.label.admin.testmgt.testCatalogEditor"
            }
          />
        </SideNavMenuItem>
        {/* OGC-224 — Panels is a peer entity of Tests / Sample Types in this
            shell; the list route hosts it via ?entity=panels. */}
        <SideNavMenuItem
          data-cy="panelsList"
          {...panelsListNavProps(`${path}/TestCatalogList?entity=panels`)}
        >
          <FormattedMessage id="label.testCatalog.entity.panels" />
        </SideNavMenuItem>
        {/* OGC-189 — Lab Units is a peer entity of Tests / Panels / Sample
            Types in this shell (per the v2.0 FRS direction). */}
        <SideNavMenuItem
          data-cy="labUnitManagement"
          {...navProps(`${path}/LabUnitManagement`)}
        >
          <FormattedMessage
            id={
              editorLabUnitId
                ? "sidenav.label.admin.labUnit.backToList"
                : "sidenav.label.admin.labUnitManagement"
            }
          />
        </SideNavMenuItem>
        {editorLabUnitId ? (
          <>
            {editorLabUnitId === "new"
              ? sectionsCaption(
                  "labUnitSectionsHelp",
                  "labUnitSectionsContext",
                  "sidenav.label.admin.labUnit.addingNew",
                )
              : editorLabUnitName
                ? sectionsCaption(
                    "labUnitSectionsHelp",
                    "labUnitSectionsContext",
                    "sidenav.label.admin.labUnit.editing",
                    { name: editorLabUnitName },
                  )
                : sectionsCaption(
                    "labUnitSectionsHelp",
                    "labUnitSectionsContext",
                    "sidenav.label.admin.labUnit.editingGeneric",
                  )}
            {LAB_UNIT_SECTIONS.map((sectionKey) => (
              <SideNavMenuItem
                key={sectionKey}
                data-cy={`labUnit-section-${sectionKey}`}
                {...navProps(
                  `${path}/LabUnitManagement/${editorLabUnitId}/${sectionKey}`,
                )}
              >
                <FormattedMessage id={`label.labUnit.section.${sectionKey}`} />
              </SideNavMenuItem>
            ))}
          </>
        ) : inPanelsContext ? (
          <>
            {/* OGC-224 — panel context: caption + the panel's own sections as
                SideNav submenu items (FRS: submenus, never tabs). With no panel
                chosen the panel sections are shown greyed rather than the
                test ones. */}
            {editorPanelId
              ? sectionsCaption(
                  "panelSectionsHelp",
                  "panelSectionsContext",
                  editorPanelId === "new"
                    ? "sidenav.label.admin.panel.addingNew"
                    : "sidenav.label.admin.panel.editing",
                  { name: editorPanelName || "" },
                )
              : sectionsCaption(
                  "panelSectionsHelp",
                  "panelSectionsContext",
                  "sidenav.label.admin.panel.sectionsHelper",
                )}
            {PANEL_SECTIONS.map((sectionKey) => {
              const label = (
                <FormattedMessage id={`label.panel.section.${sectionKey}`} />
              );
              return editorPanelId ? (
                <SideNavMenuItem
                  key={sectionKey}
                  data-cy={`panel-section-${sectionKey}`}
                  {...navProps(
                    `${path}/TestCatalogEditor/panel/${editorPanelId}/${sectionKey}`,
                  )}
                >
                  {label}
                </SideNavMenuItem>
              ) : (
                disabledSection(
                  `panel-section-${sectionKey}`,
                  "panelSectionsHelp",
                  label,
                )
              );
            })}
          </>
        ) : inSampleTypesContext ? (
          <>
            {!editorSampleTypeId
              ? sectionsCaption(
                  "sampleTypeSectionsHelp",
                  "sampleTypeSectionsContext",
                  "sidenav.label.admin.sampleType.sectionsHelper",
                )
              : editorSampleTypeId === "new"
                ? sectionsCaption(
                    "sampleTypeSectionsHelp",
                    "sampleTypeSectionsContext",
                    "sidenav.label.admin.sampleType.addingNew",
                  )
                : editorSampleTypeName
                  ? sectionsCaption(
                      "sampleTypeSectionsHelp",
                      "sampleTypeSectionsContext",
                      "sidenav.label.admin.sampleType.editing",
                      { name: editorSampleTypeName },
                    )
                  : sectionsCaption(
                      "sampleTypeSectionsHelp",
                      "sampleTypeSectionsContext",
                      "sidenav.label.admin.sampleType.editingGeneric",
                    )}
            {SAMPLE_TYPE_SECTIONS.map((sectionKey) => {
              const label = (
                <FormattedMessage
                  id={`label.sampleType.section.${sectionKey}`}
                />
              );
              return editorSampleTypeId ? (
                <SideNavMenuItem
                  key={sectionKey}
                  data-cy={`sampleType-section-${sectionKey}`}
                  {...navProps(
                    `${path}/SampleTypeEditor/${editorSampleTypeId}/${sectionKey}`,
                  )}
                >
                  {label}
                </SideNavMenuItem>
              ) : (
                disabledSection(
                  `sampleType-section-${sectionKey}`,
                  "sampleTypeSectionsHelp",
                  label,
                )
              );
            })}
          </>
        ) : (
          <>
            <li
              id="testCatalogSectionsHelp"
              data-cy="testCatalogSectionsContext"
              className="adminSideNav__sectionsContext"
              style={{
                padding: "0.25rem 1rem 0.5rem",
                fontSize: "0.75rem",
                lineHeight: 1.3,
                color: "var(--cds-text-secondary, #6f6f6f)",
              }}
            >
              {editorTestId ? (
                editorTestName ? (
                  <FormattedMessage
                    id="sidenav.label.admin.testCatalog.editing"
                    values={{ name: editorTestName }}
                  />
                ) : (
                  <FormattedMessage id="sidenav.label.admin.testCatalog.editingGeneric" />
                )
              ) : (
                <FormattedMessage id="sidenav.label.admin.testCatalog.sectionsHelper" />
              )}
            </li>
            {V1_SECTIONS.map((sectionKey) => {
              const label = (
                <FormattedMessage
                  id={`label.testCatalog.section.${sectionKey}`}
                />
              );
              return editorTestId ? (
                <SideNavMenuItem
                  key={sectionKey}
                  data-cy={`section-${sectionKey}`}
                  {...navProps(
                    `${path}/TestCatalogEditor/${editorTestId}/${sectionKey}`,
                  )}
                >
                  {label}
                </SideNavMenuItem>
              ) : (
                <SideNavMenuItem
                  key={sectionKey}
                  data-cy={`section-${sectionKey}`}
                  aria-disabled="true"
                  aria-describedby="testCatalogSectionsHelp"
                  tabIndex={-1}
                  onClick={(e) => e.preventDefault()}
                  style={{ opacity: 0.5, cursor: "not-allowed" }}
                >
                  {label}
                </SideNavMenuItem>
              );
            })}
          </>
        )}
      </SideNavMenu>
      <SideNavLink
        renderIcon={ListDropdown}
        {...navProps(`${path}/AnalyzerTestName`)}
      >
        <FormattedMessage id="sidenav.label.admin.analyzerTest" />
      </SideNavLink>
      <SideNavLink
        data-cy="labNumberMgmnt"
        renderIcon={CharacterWholeNumber}
        {...navProps(`${path}/labNumber`)}
      >
        <FormattedMessage id="sidenav.label.admin.labNumber" />
      </SideNavLink>
      <SideNavLink
        data-cy="programEntry"
        renderIcon={ChartBubble}
        {...navProps(`${path}/program`)}
      >
        <FormattedMessage id="sidenav.label.admin.program" />
      </SideNavLink>
      <SideNavLink
        data-cy="providerMgmnt"
        renderIcon={CicsSystemGroup}
        {...navProps(`${path}/providerMenu`)}
      >
        <FormattedMessage id="provider.browse.title" />
      </SideNavLink>
      <SideNavLink
        data-cy="labelPresets"
        renderIcon={QrCode}
        {...navProps(`${path}/labelPresets`)}
      >
        <FormattedMessage id="sidenav.label.admin.labelPresets" />
      </SideNavLink>
      <SideNavLink
        data-cy="pluginFile"
        renderIcon={BootVolumeAlt}
        {...navProps(`${path}/PluginFile`)}
      >
        <FormattedMessage id="sidenav.label.admin.Listplugin" />
      </SideNavLink>
      <SideNavMenu
        data-cy="vectorSurveillance"
        renderIcon={Sprout}
        title={intl.formatMessage({
          id: "sidenav.label.admin.vectorSurveillance",
          defaultMessage: "Vector Surveillance",
        })}
      >
        <SideNavMenuItem
          data-cy="vectorSpecies"
          {...navProps(`${path}/vectorSurveillanceSetup/species`)}
        >
          <FormattedMessage
            id="vector.admin.species"
            defaultMessage="Species"
          />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="vectorTrapTypes"
          {...navProps(`${path}/vectorSurveillanceSetup/trap-types`)}
        >
          <FormattedMessage
            id="vector.admin.trapTypes"
            defaultMessage="Trap Types"
          />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="vectorSamplingSites"
          {...navProps(`${path}/vectorSurveillanceSetup/sampling-sites`)}
        >
          <FormattedMessage
            id="vector.admin.samplingSites"
            defaultMessage="Sampling Sites"
          />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="vectorManualEntryFields"
          {...navProps(`${path}/vectorSurveillanceSetup/manual-entry-fields`)}
        >
          <FormattedMessage
            id="vectorReport.fieldMap.title"
            defaultMessage="Manual Entry Field Map"
          />
        </SideNavMenuItem>
      </SideNavMenu>
      <SideNavLink
        data-cy="orgMgmnt"
        renderIcon={ContainerSoftware}
        {...navProps(`${path}/organizationManagement`)}
      >
        <FormattedMessage id="organization.main.title" />
      </SideNavLink>
      <SideNavLink
        data-cy="resultReportingConfiguration"
        renderIcon={Report}
        {...navProps(`${path}/resultReportingConfiguration`)}
      >
        <FormattedMessage id="resultreporting.browse.title" />
      </SideNavLink>
      <SideNavLink
        data-cy="userMgmnt"
        renderIcon={User}
        {...navProps(`${path}/userManagement`)}
      >
        <FormattedMessage id="unifiedSystemUser.browser.title" />
      </SideNavLink>
      <SideNavLink
        data-cy="batchTestReassignment"
        renderIcon={BatchJob}
        {...navProps(`${path}/batchTestReassignment`)}
      >
        <FormattedMessage id="configuration.batch.test.reassignment" />
      </SideNavLink>
      <SideNavLink
        data-cy="testManagementConfigMenu"
        renderIcon={ResultNew}
        {...navProps(`${path}/testManagementConfigMenu`)}
      >
        <FormattedMessage id="master.lists.page.test.management" />
      </SideNavLink>
      <SideNavMenu
        title={intl.formatMessage({ id: "sidenav.label.admin.menu" })}
        renderIcon={TableOfContents}
      >
        <SideNavMenuItem
          data-cy="globalMenuMgmnt"
          {...navProps(`${path}/globalMenuManagement`)}
        >
          <FormattedMessage id="sidenav.label.admin.menu.global" />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="billingMenuMgmnt"
          {...navProps(`${path}/billingMenuManagement`)}
        >
          <FormattedMessage id="sidenav.label.admin.menu.billing" />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="nonConformMenuMgmnt"
          {...navProps(`${path}/nonConformityMenuManagement`)}
        >
          <FormattedMessage id="sidenav.label.admin.menu.nonconform" />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="patientMenuMgmnt"
          {...navProps(`${path}/patientMenuManagement`)}
        >
          <FormattedMessage id="sidenav.label.admin.menu.patient" />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="studyMenuMgmnt"
          {...navProps(`${path}/studyMenuManagement`)}
        >
          <FormattedMessage id="sidenav.label.admin.menu.study" />
        </SideNavMenuItem>
      </SideNavMenu>

      <SideNavMenu
        title={intl.formatMessage({ id: "admin.formEntryConfig" })}
        renderIcon={ListDropdown}
      >
        <SideNavMenuItem
          data-cy="nonConformConfig"
          {...navProps(`${path}/NonConformityConfigurationMenu`)}
        >
          <FormattedMessage id="sidenav.label.admin.formEntry.nonconformityconfig" />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="menuStatementConfig"
          {...navProps(`${path}/MenuStatementConfigMenu`)}
        >
          <FormattedMessage id="sidenav.label.admin.formEntry.menustatementconfig" />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="workPlanConfig"
          {...navProps(`${path}/WorkPlanConfigurationMenu`)}
        >
          <FormattedMessage id="sidenav.label.admin.formEntry.Workplanconfig" />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="siteInfoMenu"
          {...navProps(`${path}/SiteInformationMenu`)}
        >
          <FormattedMessage id="sidenav.label.admin.formEntry.siteInfoconfig" />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="siteBrandingMenu"
          {...navProps(`${path}/SiteBrandingMenu`)}
        >
          <FormattedMessage id="sidenav.label.admin.formEntry.siteBranding" />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="resultConfigMenu"
          {...navProps(`${path}/ResultConfigurationMenu`)}
        >
          <FormattedMessage id="sidenav.label.admin.formEntry.resultConfig" />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="patientConfigMenu"
          {...navProps(`${path}/PatientConfigurationMenu`)}
        >
          <FormattedMessage id="sidenav.label.admin.formEntry.patientconfig" />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="printedReportsConfigMenu"
          {...navProps(`${path}/PrintedReportsConfigurationMenu`)}
        >
          <FormattedMessage id="sidenav.label.admin.formEntry.PrintedReportsconfig" />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="sampleEntryConfigMenu"
          {...navProps(`${path}/SampleEntryConfigurationMenu`)}
        >
          <FormattedMessage id="sidenav.label.admin.formEntry.sampleEntryconfig" />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="validationConfigMenu"
          {...navProps(`${path}/ValidationConfigurationMenu`)}
        >
          <FormattedMessage id="sidenav.label.admin.formEntry.validationconfig" />
        </SideNavMenuItem>
      </SideNavMenu>

      <SideNavMenu
        data-cy="sampleAcceptanceChecklist"
        renderIcon={ListChecked}
        title={intl.formatMessage({
          id: "sampleAcceptance.title",
          defaultMessage: "Sample Acceptance Checklist",
        })}
      >
        <SideNavMenuItem
          data-cy="sampleAcceptanceAll"
          {...navProps(`${path}/SampleAcceptanceChecklist/all`)}
        >
          <FormattedMessage
            id="sampleAcceptance.domain.all"
            defaultMessage="All domains"
          />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="sampleAcceptanceClinical"
          {...navProps(`${path}/SampleAcceptanceChecklist/clinical`)}
        >
          <FormattedMessage
            id="sampleAcceptance.domain.clinical"
            defaultMessage="Clinical"
          />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="sampleAcceptanceEnvironmental"
          {...navProps(`${path}/SampleAcceptanceChecklist/environmental`)}
        >
          <FormattedMessage
            id="sampleAcceptance.domain.environmental"
            defaultMessage="Environmental"
          />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="sampleAcceptanceVector"
          {...navProps(`${path}/SampleAcceptanceChecklist/vector`)}
        >
          <FormattedMessage
            id="sampleAcceptance.domain.vector"
            defaultMessage="Vector"
          />
        </SideNavMenuItem>
      </SideNavMenu>

      <SideNavLink
        renderIcon={Settings}
        {...navProps(`${path}/commonproperties`)}
      >
        <FormattedMessage
          id="sidenav.label.admin.commonproperties"
          defaultMessage={"Common Properties"}
        />
      </SideNavLink>
      <SideNavLink
        renderIcon={Popup}
        {...navProps(`${path}/testNotificationConfigMenu`)}
      >
        <FormattedMessage id="testnotificationconfig.browse.title" />
      </SideNavLink>
      <SideNavLink
        data-cy="dictMenu"
        renderIcon={CharacterWholeNumber}
        {...navProps(`${path}/DictionaryMenu`)}
      >
        <FormattedMessage id="dictionary.label.modify" />
      </SideNavLink>
      <SideNavLink
        data-cy="notifyUser"
        renderIcon={Bullhorn}
        {...navProps(`${path}/NotifyUser`)}
      >
        <FormattedMessage id="notify.main.title" />
      </SideNavLink>
      <SideNavLink
        renderIcon={Search}
        {...navProps(`${path}/SearchIndexManagement`)}
      >
        <FormattedMessage id="searchindexmanagement.label" />
      </SideNavLink>
      <SideNavLink
        renderIcon={Settings}
        {...navProps(`${path}/loggingManagement`)}
      >
        <FormattedMessage id="logging.management.label" />
      </SideNavLink>
      {isTrainingInstallation && (
        <SideNavLink
          renderIcon={TrashCan}
          {...navProps(`${path}/DatabaseCleaning`)}
        >
          <FormattedMessage id="database.clean" />
        </SideNavLink>
      )}
      <SideNavMenu
        title={intl.formatMessage({
          id: "sidenav.label.admin.localization",
        })}
        renderIcon={TableOfContents}
      >
        <SideNavMenuItem
          data-cy="languageManagement"
          {...navProps(`${path}/languageManagement`)}
        >
          <FormattedMessage
            id="locale.management.title"
            defaultMessage="Language Management"
          />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="translationManagement"
          {...navProps(`${path}/translationManagement`)}
        >
          <FormattedMessage
            id="translation.management.title"
            defaultMessage="Translation Management"
          />
        </SideNavMenuItem>
      </SideNavMenu>
      <SideNavLink
        renderIcon={ConnectionSignal}
        {...navProps(`${path}/externalConnections`)}
      >
        <FormattedMessage id="externalconnections.browse.title" />
      </SideNavLink>
      <SideNavLink
        data-cy="dataExportStatus"
        renderIcon={ConnectionSignal}
        {...navProps(`${path}/dataExportStatus`)}
      >
        <FormattedMessage id="dataexport.status.title" />
      </SideNavLink>
      <SideNavLink
        data-cy="calendarMgmnt"
        renderIcon={Calendar}
        {...navProps(`${path}/calendarManagement`)}
      >
        <FormattedMessage id="calendar.management.title" />
      </SideNavLink>
      <SideNavLink
        renderIcon={Catalog}
        target="_blank"
        rel="noopener noreferrer"
        href={config.serverBaseUrl + "/MasterListsPage"}
      >
        <FormattedMessage id="admin.legacy" />
      </SideNavLink>
    </SideNavItems>
  );
}
