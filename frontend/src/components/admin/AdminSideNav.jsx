import React, { useEffect, useState } from "react";
import config from "../../config.json";
import { FormattedMessage, useIntl } from "react-intl";
import { useHistory, useLocation } from "react-router-dom";
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
import { getFromOpenElisServer } from "../utils/Utils";

const getAdminBasePath = (pathname) =>
  pathname.startsWith("/admin") ? "/admin" : "/MasterListsPage";

export default function AdminSideNav() {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const path = getAdminBasePath(location.pathname);
  const [isTrainingInstallation, setIsTrainingInstallation] = useState(false);

  useEffect(() => {
    getFromOpenElisServer("/rest/database-cleaning/status", (response) => {
      if (response) {
        setIsTrainingInstallation(response.trainingInstallation);
      }
    });
  }, []);

  const handleNavigation = (targetPath) => (e) => {
    e.preventDefault();
    history.push(targetPath);
  };

  return (
    <SideNavItems className="adminSideNav">
      <SideNavLink
        data-testid="admin-back-to-main-nav"
        renderIcon={ArrowLeft}
        onClick={handleNavigation("/Dashboard")}
      >
        <FormattedMessage id="sidenav.label.admin.backToMainMenu" />
      </SideNavLink>
      <SideNavMenu
        data-cy="reflexTestsConfig"
        renderIcon={Microscope}
        title={intl.formatMessage({ id: "sidenav.label.admin.testmgt" })}
      >
        <SideNavMenuItem
          data-cy="reflex"
          onClick={handleNavigation(`${path}/reflex`)}
        >
          <FormattedMessage id="sidenav.label.admin.testmgt.reflex" />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="calculatedValue"
          onClick={handleNavigation(`${path}/calculatedValue`)}
        >
          <FormattedMessage id="sidenav.label.admin.testmgt.calculated" />
        </SideNavMenuItem>
      </SideNavMenu>
      <SideNavLink
        renderIcon={ListDropdown}
        onClick={handleNavigation(`${path}/AnalyzerTestName`)}
      >
        <FormattedMessage id="sidenav.label.admin.analyzerTest" />
      </SideNavLink>
      <SideNavLink
        data-cy="labNumberMgmnt"
        renderIcon={CharacterWholeNumber}
        onClick={handleNavigation(`${path}/labNumber`)}
      >
        <FormattedMessage id="sidenav.label.admin.labNumber" />
      </SideNavLink>
      <SideNavLink
        data-cy="programEntry"
        renderIcon={ChartBubble}
        onClick={handleNavigation(`${path}/program`)}
      >
        <FormattedMessage id="sidenav.label.admin.program" />
      </SideNavLink>
      <SideNavLink
        data-cy="providerMgmnt"
        renderIcon={CicsSystemGroup}
        onClick={handleNavigation(`${path}/providerMenu`)}
      >
        <FormattedMessage id="provider.browse.title" />
      </SideNavLink>
      <SideNavLink
        data-cy="barcodeConfig"
        renderIcon={QrCode}
        onClick={handleNavigation(`${path}/barcodeConfiguration`)}
      >
        <FormattedMessage id="sidenav.label.admin.barcodeconfiguration" />
      </SideNavLink>
      <SideNavLink
        data-cy="pluginFile"
        renderIcon={BootVolumeAlt}
        onClick={handleNavigation(`${path}/PluginFile`)}
      >
        <FormattedMessage id="sidenav.label.admin.Listplugin" />
      </SideNavLink>
      <SideNavLink
        data-cy="orgMgmnt"
        renderIcon={ContainerSoftware}
        onClick={handleNavigation(`${path}/organizationManagement`)}
      >
        <FormattedMessage id="organization.main.title" />
      </SideNavLink>
      <SideNavLink
        data-cy="resultReportingConfiguration"
        renderIcon={Report}
        onClick={handleNavigation(`${path}/resultReportingConfiguration`)}
      >
        <FormattedMessage id="resultreporting.browse.title" />
      </SideNavLink>
      <SideNavLink
        data-cy="userMgmnt"
        renderIcon={User}
        onClick={handleNavigation(`${path}/userManagement`)}
      >
        <FormattedMessage id="unifiedSystemUser.browser.title" />
      </SideNavLink>
      <SideNavLink
        data-cy="batchTestReassignment"
        renderIcon={BatchJob}
        onClick={handleNavigation(`${path}/batchTestReassignment`)}
      >
        <FormattedMessage id="configuration.batch.test.reassignment" />
      </SideNavLink>
      <SideNavLink
        data-cy="testManagementConfigMenu"
        renderIcon={ResultNew}
        onClick={handleNavigation(`${path}/testManagementConfigMenu`)}
      >
        <FormattedMessage id="master.lists.page.test.management" />
      </SideNavLink>
      <SideNavMenu
        title={intl.formatMessage({ id: "sidenav.label.admin.menu" })}
        renderIcon={TableOfContents}
      >
        <SideNavMenuItem
          data-cy="globalMenuMgmnt"
          onClick={handleNavigation(`${path}/globalMenuManagement`)}
        >
          <FormattedMessage id="sidenav.label.admin.menu.global" />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="billingMenuMgmnt"
          onClick={handleNavigation(`${path}/billingMenuManagement`)}
        >
          <FormattedMessage id="sidenav.label.admin.menu.billing" />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="nonConformMenuMgmnt"
          onClick={handleNavigation(`${path}/nonConformityMenuManagement`)}
        >
          <FormattedMessage id="sidenav.label.admin.menu.nonconform" />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="patientMenuMgmnt"
          onClick={handleNavigation(`${path}/patientMenuManagement`)}
        >
          <FormattedMessage id="sidenav.label.admin.menu.patient" />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="studyMenuMgmnt"
          onClick={handleNavigation(`${path}/studyMenuManagement`)}
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
          onClick={handleNavigation(`${path}/NonConformityConfigurationMenu`)}
        >
          <FormattedMessage id="sidenav.label.admin.formEntry.nonconformityconfig" />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="menuStatementConfig"
          onClick={handleNavigation(`${path}/MenuStatementConfigMenu`)}
        >
          <FormattedMessage id="sidenav.label.admin.formEntry.menustatementconfig" />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="workPlanConfig"
          onClick={handleNavigation(`${path}/WorkPlanConfigurationMenu`)}
        >
          <FormattedMessage id="sidenav.label.admin.formEntry.Workplanconfig" />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="siteInfoMenu"
          onClick={handleNavigation(`${path}/SiteInformationMenu`)}
        >
          <FormattedMessage id="sidenav.label.admin.formEntry.siteInfoconfig" />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="siteBrandingMenu"
          onClick={handleNavigation(`${path}/SiteBrandingMenu`)}
        >
          <FormattedMessage id="sidenav.label.admin.formEntry.siteBranding" />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="resultConfigMenu"
          onClick={handleNavigation(`${path}/ResultConfigurationMenu`)}
        >
          <FormattedMessage id="sidenav.label.admin.formEntry.resultConfig" />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="patientConfigMenu"
          onClick={handleNavigation(`${path}/PatientConfigurationMenu`)}
        >
          <FormattedMessage id="sidenav.label.admin.formEntry.patientconfig" />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="printedReportsConfigMenu"
          onClick={handleNavigation(`${path}/PrintedReportsConfigurationMenu`)}
        >
          <FormattedMessage id="sidenav.label.admin.formEntry.PrintedReportsconfig" />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="sampleEntryConfigMenu"
          onClick={handleNavigation(`${path}/SampleEntryConfigurationMenu`)}
        >
          <FormattedMessage id="sidenav.label.admin.formEntry.sampleEntryconfig" />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="validationConfigMenu"
          onClick={handleNavigation(`${path}/ValidationConfigurationMenu`)}
        >
          <FormattedMessage id="sidenav.label.admin.formEntry.validationconfig" />
        </SideNavMenuItem>
      </SideNavMenu>

      <SideNavLink
        renderIcon={Settings}
        onClick={handleNavigation(`${path}/commonproperties`)}
      >
        <FormattedMessage
          id="sidenav.label.admin.commonproperties"
          defaultMessage={"Common Properties"}
        />
      </SideNavLink>
      <SideNavLink
        renderIcon={Popup}
        onClick={handleNavigation(`${path}/testNotificationConfigMenu`)}
      >
        <FormattedMessage id="testnotificationconfig.browse.title" />
      </SideNavLink>
      <SideNavLink
        data-cy="dictMenu"
        renderIcon={CharacterWholeNumber}
        onClick={handleNavigation(`${path}/DictionaryMenu`)}
      >
        <FormattedMessage id="dictionary.label.modify" />
      </SideNavLink>
      <SideNavLink
        data-cy="notifyUser"
        renderIcon={Bullhorn}
        onClick={handleNavigation(`${path}/NotifyUser`)}
      >
        <FormattedMessage id="notify.main.title" />
      </SideNavLink>
      <SideNavLink
        renderIcon={Search}
        onClick={handleNavigation(`${path}/SearchIndexManagement`)}
      >
        <FormattedMessage id="searchindexmanagement.label" />
      </SideNavLink>
      <SideNavLink
        renderIcon={Settings}
        onClick={handleNavigation(`${path}/loggingManagement`)}
      >
        <FormattedMessage id="logging.management.label" />
      </SideNavLink>
      {isTrainingInstallation && (
        <SideNavLink
          renderIcon={TrashCan}
          onClick={handleNavigation(`${path}/DatabaseCleaning`)}
        >
          <FormattedMessage id="database.clean" />
        </SideNavLink>
      )}
      <SideNavMenu
        title={intl.formatMessage({
          id: "sidenav.label.admin.localization",
          defaultMessage: "Localization",
        })}
        renderIcon={TableOfContents}
      >
        <SideNavMenuItem
          data-cy="languageManagement"
          onClick={handleNavigation(`${path}/languageManagement`)}
        >
          <FormattedMessage
            id="locale.management.title"
            defaultMessage="Language Management"
          />
        </SideNavMenuItem>
        <SideNavMenuItem
          data-cy="translationManagement"
          onClick={handleNavigation(`${path}/translationManagement`)}
        >
          <FormattedMessage
            id="translation.management.title"
            defaultMessage="Translation Management"
          />
        </SideNavMenuItem>
      </SideNavMenu>
      <SideNavLink
        renderIcon={ConnectionSignal}
        onClick={handleNavigation(`${path}/externalConnections`)}
      >
        <FormattedMessage id="externalconnections.browse.title" />
      </SideNavLink>
      <SideNavLink
        data-cy="calendarMgmnt"
        renderIcon={Calendar}
        onClick={handleNavigation(`${path}/calendarManagement`)}
      >
        <FormattedMessage id="calendar.management.title" />
      </SideNavLink>
      <SideNavLink
        renderIcon={Catalog}
        target="_blank"
        href={config.serverBaseUrl + "/MasterListsPage"}
      >
        <FormattedMessage id="admin.legacy" />
      </SideNavLink>
    </SideNavItems>
  );
}
