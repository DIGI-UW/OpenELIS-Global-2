import React, { useState, useEffect } from "react";
import config from "../../config.json";
import { FormattedMessage, useIntl } from "react-intl";
import { Routes, Route, useRouteMatch, useNavigate } from "react-router-dom";
import "../Style.css";
import ReflexTestManagement from "./reflexTests/ReflexTestManagement";
import CalendarManagement from "./calendarManagement";
import ProgramManagement from "./program/ProgramManagement";
import EQAProgramManagement from "../eqa/EQAProgram/ProgramManagement";
import LabNumberManagement from "./labNumber/LabNumberManagement";
import {
  GlobalMenuManagement,
  BillingMenuManagement,
  NonConformityMenuManagement,
  PatientMenuManagement,
  StudyMenuManagement,
  DictionaryManagement,
} from "./menu";
import {
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
  DataCheck,
  ConnectionSignal,
  Calendar,
} from "@carbon/icons-react";
import CalculatedValue from "./calculatedValue/CalculatedValueForm";
import {
  SideNav,
  SideNavItems,
  SideNavLink,
  SideNavMenu,
  SideNavMenuItem,
} from "@carbon/react";
import { CommonProperties } from "./menu/CommonProperties";
import ConfigMenuDisplay from "./generalConfig/common/ConfigMenuDisplay";
import SiteBrandingConfig from "./generalConfig/siteBranding/SiteBrandingConfig";
import ProviderMenu from "./ProviderMenu/ProviderMenu";
import BarcodeConfiguration from "./barcodeConfiguration/BarcodeConfiguration";
import AnalyzerTestName from "./analyzerTestName/AnalyzerTestName";
import PluginList from "./pluginFile/PluginFile";
import ResultReportingConfiguration from "./ResultReportingConfiguration/ResultReportingConfiguration";
import TestCatalog from "./testManagement/ViewTestCatalog";
import PushNotificationPage from "../notifications/PushNotificationPage.jsx";
import OrganizationManagement from "./OrganizationManagement/OrganizationManagement";
import OrganizationAddModify from "./OrganizationManagement/OrganizationAddModify";
import UserManagement from "./userManagement/UserManagement";
import UserAddModify from "./userManagement/UserAddModify";
import ManageMethod from "./testManagement/ManageMethod";
import BatchTestReassignmentAndCancelation from "./BatchTestReassignmentAndCancellation/BatchTestReassignmentAndCancelation";
import TestNotificationConfigMenu from "./testNotificationConfigMenu/TestNotificationConfigMenu";
import TestNotificationConfigEdit from "./testNotificationConfigMenu/TestNotificationConfigEdit";
import SearchIndexManagement from "./searchIndexManagement/SearchIndexManagement";
import LoggingManagement from "./loggingManagement/LoggingManagement";
import TestManagementConfigMenu from "./testManagementConfigMenu/TestManagementConfigMenu";
import ResultSelectListAdd from "./testManagementConfigMenu/ResultSelectListAdd";
import TestAdd from "./testManagementConfigMenu/TestAdd";
import TestModifyEntry from "./testManagementConfigMenu/TestModifyEntry";
import TestOrderability from "./testManagementConfigMenu/TestOrderability";
import MethodCreate from "./testManagementConfigMenu/MethodCreate";
import TestSectionManagement from "./testManagementConfigMenu/TestSectionManagement";
import TestSectionCreate from "./testManagementConfigMenu/TestSectionCreate";
import TestSectionOrder from "./testManagementConfigMenu/TestSectionOrder";
import SampleTypeManagement from "./testManagementConfigMenu/SampleTypeManagement";
import TestSectionTestAssign from "./testManagementConfigMenu/TestSectionTestAssign";
import SampleTypeOrder from "./testManagementConfigMenu/SampleTypeOrder";
import SampleTypeCreate from "./testManagementConfigMenu/SampleTypeCreate";
import SampleTypeTestAssign from "./testManagementConfigMenu/SampleTypeTestAssign";
import UomManagement from "./testManagementConfigMenu/UomManagement";
import UomCreate from "./testManagementConfigMenu/UomCreate";
import PanelManagement from "./testManagementConfigMenu/PanelManagement";
import PanelCreate from "./testManagementConfigMenu/PanelCreate";
import PanelOrder from "./testManagementConfigMenu/PanelOrder";
import PanelTestAssign from "./testManagementConfigMenu/PanelTestAssign";
import TestActivation from "./testManagementConfigMenu/TestActivation";
import TestRenameEntry from "./testManagementConfigMenu/TestRenameEntry";
import PanelRenameEntry from "./testManagementConfigMenu/PanelRenameEntry";
import SampleTypeRenameEntry from "./testManagementConfigMenu/SampleTypeRenameEntry";
import TestSectionRenameEntry from "./testManagementConfigMenu/TestSectionRenameEntry";
import UomRenameEntry from "./testManagementConfigMenu/UomRenameEntry";
import SelectListRenameEntry from "./testManagementConfigMenu/SelectListRenameEntry";
import MethodRenameEntry from "./testManagementConfigMenu/MethodRenameEntry";
import {
  LanguageManagement,
  TranslationManagement,
} from "./localizationManagement";
import ExternalConnectionMenu from "./externalConnections/ExternalConnectionMenu";
import ExternalConnectionAddModify from "./externalConnections/ExternalConnectionAddModify";
import DatabaseCleaning from "./databaseCleaning/DatabaseCleaning";
import { TrashCan } from "@carbon/icons-react";
import { getFromOpenElisServer } from "../utils/Utils";

function Admin() {
  const intl = useIntl();
  const intl = useIntl();
  const { path } = useRouteMatch();
  const navigate = useNavigate();
  const [isSmallScreen, setIsSmallScreen] = useState(false);
  const [isTrainingInstallation, setIsTrainingInstallation] = useState(false);

  useEffect(() => {
    getFromOpenElisServer("/rest/database-cleaning/status", (response) => {
      if (response) {
        setIsTrainingInstallation(response.trainingInstallation);
      }
    });
  }, []);

  // Navigation handler to prevent page reload
  const handleNavigation = (targetPath) => (e) => {
    e.preventDefault();
    navigate(targetPath);
  };

  useEffect(() => {
    const mediaQuery = window.matchMedia("(max-width: 1024px)"); //applicable for medium screen and below  for only small screen set max-width: 768px
    const handleMediaQueryChange = () => setIsSmallScreen(mediaQuery.matches);

    handleMediaQueryChange();
    mediaQuery.addEventListener("change", handleMediaQueryChange);

    return () =>
      mediaQuery.removeEventListener("change", handleMediaQueryChange);
  }, []);

  return (
    <>
      <SideNav
        aria-label="Side navigation"
        defaultExpanded={true}
        isRail={isSmallScreen}
      >
        <SideNavItems className="adminSideNav">
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
            data-cy="eqaProgramEntry"
            renderIcon={DataCheck}
            onClick={handleNavigation(`${path}/eqaProgram`)}
          >
            <FormattedMessage id="sidenav.label.admin.eqaProgram" />
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
              onClick={handleNavigation(
                `${path}/NonConformityConfigurationMenu`,
              )}
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
              onClick={handleNavigation(
                `${path}/PrintedReportsConfigurationMenu`,
              )}
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
      </SideNav>

      <Routes>
        <Route
          path={`${path}/calendarManagement`}
          element={<CalendarManagement />}
        />
        <Route path={`${path}/reflex`} element={<ReflexTestManagement />} />
        <Route path={`${path}/calculatedValue`} element={<CalculatedValue />} />
        <Route path={`${path}/TestCatalog`} element={<TestCatalog />} />
        <Route path={`${path}/MethodManagement`} element={<ManageMethod />} />
        <Route
          path={`${path}/AnalyzerTestName`}
          element={<AnalyzerTestName />}
        />
        <Route path={`${path}/labNumber`} element={<LabNumberManagement />} />
        <Route path={`${path}/program`} element={<ProgramManagement />} />
        <Route path={`${path}/eqaProgram`} element={<EQAProgramManagement />} />
        <Route path={`${path}/providerMenu`} element={<ProviderMenu />} />
        <Route path={`${path}/NotifyUser`} element={<PushNotificationPage />} />
        <Route
          path={`${path}/barcodeConfiguration`}
          element={<BarcodeConfiguration />}
        />
        <Route
          path={`${path}/organizationManagement`}
          element={<OrganizationManagement />}
        />
        <Route
          path={`${path}/organizationEdit`}
          element={<OrganizationAddModify />}
        />
        <Route
          path={`${path}/resultReportingConfiguration`}
          element={<ResultReportingConfiguration />}
        />
        <Route path={`${path}/userManagement`} element={<UserManagement />} />
        <Route
          path={`${path}/batchTestReassignment`}
          element={<BatchTestReassignmentAndCancelation />}
        />
        <Route path={`${path}/userEdit`} element={<UserAddModify />} />
        <Route
          path={`${path}/globalMenuManagement`}
          element={<GlobalMenuManagement />}
        />
        <Route
          path={`${path}/billingMenuManagement`}
          element={<BillingMenuManagement />}
        />
        <Route
          path={`${path}/SiteBrandingMenu`}
          element={<SiteBrandingConfig />}
        />
        <Route
          path={`${path}/nonConformityMenuManagement`}
          element={<NonConformityMenuManagement />}
        />
        <Route
          path={`${path}/patientMenuManagement`}
          element={<PatientMenuManagement />}
        />
        <Route
          path={`${path}/studyMenuManagement`}
          element={<StudyMenuManagement />}
        />
        <Route
          path={`${path}/commonproperties`}
          element={<CommonProperties />}
        />
        <Route
          path={`${path}/testManagementConfigMenu`}
          element={<TestManagementConfigMenu />}
        />
        <Route
          path={`${path}/ResultSelectListAdd`}
          element={<ResultSelectListAdd />}
        />
        <Route path={`${path}/TestAdd`} element={<TestAdd />} />
        <Route path={`${path}/TestModifyEntry`} element={<TestModifyEntry />} />
        <Route
          path={`${path}/TestOrderability`}
          element={<TestOrderability />}
        />
        <Route path={`${path}/MethodCreate`} element={<MethodCreate />} />
        <Route
          path={`${path}/TestSectionManagement`}
          element={<TestSectionManagement />}
        />
        <Route
          path={`${path}/TestSectionCreate`}
          element={<TestSectionCreate />}
        />
        <Route
          path={`${path}/TestSectionOrder`}
          element={<TestSectionOrder />}
        />
        <Route
          path={`${path}/TestSectionTestAssign`}
          element={<TestSectionTestAssign />}
        />
        <Route
          path={`${path}/SampleTypeManagement`}
          element={<SampleTypeManagement />}
        />
        <Route
          path={`${path}/SampleTypeCreate`}
          element={<SampleTypeCreate />}
        />
        <Route path={`${path}/SampleTypeOrder`} element={<SampleTypeOrder />} />
        <Route
          path={`${path}/SampleTypeTestAssign`}
          element={<SampleTypeTestAssign />}
        />
        <Route path={`${path}/UomManagement`} element={<UomManagement />} />
        <Route path={`${path}/UomCreate`} element={<UomCreate />} />
        <Route path={`${path}/PanelManagement`} element={<PanelManagement />} />
        <Route path={`${path}/PanelCreate`} element={<PanelCreate />} />
        <Route path={`${path}/PanelOrder`} element={<PanelOrder />} />
        <Route path={`${path}/PanelTestAssign`} element={<PanelTestAssign />} />
        <Route path={`${path}/TestActivation`} element={<TestActivation />} />
        <Route path={`${path}/TestRenameEntry`} element={<TestRenameEntry />} />
        <Route
          path={`${path}/PanelRenameEntry`}
          element={<PanelRenameEntry />}
        />
        <Route
          path={`${path}/SampleTypeRenameEntry`}
          element={<SampleTypeRenameEntry />}
        />
        <Route
          path={`${path}/TestSectionRenameEntry`}
          element={<TestSectionRenameEntry />}
        />
        <Route path={`${path}/UomRenameEntry`} element={<UomRenameEntry />} />
        <Route
          path={`${path}/SelectListRenameEntry`}
          element={<SelectListRenameEntry />}
        />
        <Route
          path={`${path}/MethodRenameEntry`}
          element={<MethodRenameEntry />}
        />
        <Route
          path={`${path}/languageManagement`}
          element={<LanguageManagement />}
        />
        <Route
          path={`${path}/translationManagement`}
          element={<TranslationManagement />}
        />
        <Route
          path={`${path}/NonConformityConfigurationMenu`}
          component={() => (
            <ConfigMenuDisplay
              menuType="NonConformityConfigurationMenu"
              label="Non Conformity Configuration Menu"
              id="sidenav.label.admin.formEntry.nonconformityconfig"
            />
          )}
        />
        <Route
          path={`${path}/MenuStatementConfigMenu`}
          component={() => (
            <ConfigMenuDisplay
              menuType="MenuStatementConfigMenu"
              label="Menu Statement Configuration Menu"
              id="sidenav.label.admin.formEntry.menustatementconfig"
            />
          )}
        />
        <Route
          path={`${path}/ValidationConfigurationMenu`}
          component={() => (
            <ConfigMenuDisplay
              menuType="ValidationConfigurationMenu"
              label="Validation Configuration Menu"
              id="sidenav.label.admin.formEntry.validationconfig"
            />
          )}
        />
        <Route
          path={`${path}/SampleEntryConfigurationMenu`}
          component={() => (
            <ConfigMenuDisplay
              menuType="SampleEntryConfigMenu"
              label="Sample Entry Configuration Menu"
              id="sidenav.label.admin.formEntry.sampleEntryconfig"
            />
          )}
        />
        <Route
          path={`${path}/WorkPlanConfigurationMenu`}
          component={() => (
            <ConfigMenuDisplay
              menuType="WorkplanConfigurationMenu"
              label="WorkPlan Configuration Menu"
              id="sidenav.label.admin.formEntry.Workplanconfig"
            />
          )}
        />
        <Route
          path={`${path}/SiteInformationMenu`}
          component={() => (
            <ConfigMenuDisplay
              menuType="SiteInformationMenu"
              label="Site Information Menu"
              id="sidenav.label.admin.formEntry.siteInfoconfig"
            />
          )}
        />
        <Route
          path={`${path}/ResultConfigurationMenu`}
          component={() => (
            <ConfigMenuDisplay
              menuType="ResultConfigurationMenu"
              label="Result Configuration Menu"
              id="sidenav.label.admin.formEntry.resultConfig"
            />
          )}
        />
        <Route
          path={`${path}/PatientConfigurationMenu`}
          component={() => (
            <ConfigMenuDisplay
              menuType="PatientConfigurationMenu"
              label="Patient Configuration Menu"
              id="sidenav.label.admin.formEntry.patientconfig"
            />
          )}
        />
        <Route
          path={`${path}/PrintedReportsConfigurationMenu`}
          component={() => (
            <ConfigMenuDisplay
              menuType="PrintedReportsConfigurationMenu"
              label="PrintedReports Configuration Menu"
              id="sidenav.label.admin.formEntry.PrintedReportsconfig"
            />
          )}
        />
        <Route
          path={`${path}/testNotificationConfigMenu`}
          element={<TestNotificationConfigMenu />}
        />
        <Route
          path={`${path}/testNotificationConfig`}
          element={<TestNotificationConfigEdit />}
        />
        <Route
          path={`${path}/DictionaryMenu`}
          element={<DictionaryManagement />}
        />
        <Route path={`${path}/PluginFile`} element={<PluginList />} />
        <Route
          path={`${path}/SearchIndexManagement`}
          element={<SearchIndexManagement />}
        />
        <Route
          path={`${path}/loggingManagement`}
          element={<LoggingManagement />}
        />
        <Route
          path={`${path}/externalConnections`}
          element={<ExternalConnectionMenu />}
        />
        <Route
          path={`${path}/externalConnectionEdit`}
          element={<ExternalConnectionAddModify />}
        />
        <Route
          path={`${path}/DatabaseCleaning`}
          element={<DatabaseCleaning />}
        />
      </Routes>
    </>
  );
}

export default Admin;
