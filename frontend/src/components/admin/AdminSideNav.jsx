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
} from "@carbon/icons-react";
import {
  SideNavItems,
  SideNavLink,
  SideNavMenu,
  SideNavMenuItem,
} from "@carbon/react";
import { V1_SECTIONS } from "./testCatalog/sectionConfig";

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
  const history = useHistory();
  const location = useLocation();
  const path = getAdminBasePath(location.pathname);

  const editorMatch = location.pathname.match(/\/TestCatalogEditor\/([^/]+)/);
  const editorTestId = editorMatch ? editorMatch[1] : null;

  // Keyed by id so the label never shows a prior test's name while the next loads.
  const [editorTest, setEditorTest] = useState({ id: null, name: null });
  useEffect(() => {
    if (!editorTestId) {
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
      {/* key flips with editor context to force a remount — Carbon SideNavMenu
          reads defaultExpanded only at mount. */}
      <SideNavMenu
        key={editorTestId ? "testcatalog-editor" : "testcatalog"}
        data-cy="testCatalogManagement"
        renderIcon={Catalog}
        isActive={!!editorTestId}
        defaultExpanded={!!editorTestId}
        title={intl.formatMessage({ id: "sidenav.label.admin.testCatalog" })}
      >
        <SideNavMenuItem
          data-cy="testCatalogList"
          {...navProps(`${path}/TestCatalogList`)}
        >
          <FormattedMessage
            id={
              editorTestId
                ? "sidenav.label.admin.testCatalog.backToList"
                : "sidenav.label.admin.testmgt.testCatalogEditor"
            }
          />
        </SideNavMenuItem>
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
            <FormattedMessage id={`label.testCatalog.section.${sectionKey}`} />
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
