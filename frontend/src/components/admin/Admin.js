import React, { useState, useEffect } from "react";
import config from "../../config.json";
import { FormattedMessage, useIntl, injectIntl } from "react-intl";
import { Switch, Route, useRouteMatch, useHistory } from "react-router-dom";
import "../Style.css";
import ReflexTestManagement from "./reflexTests/ReflexTestManagement";
import ProgramManagement from "./program/ProgramManagement";
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
import ProviderMenu from "./ProviderMenu/ProviderMenu";
import BarcodeConfiguration from "./barcodeConfiguration/BarcodeConfiguration";
import AnalyzerTestName from "./analyzerTestName/AnalyzerTestName.js";
import PluginList from "./pluginFile/PluginFile.js";
import ResultReportingConfiguration from "./ResultReportingConfiguration/ResultReportingConfiguration.js";
import TestCatalog from "./testManagement/ViewTestCatalog.js";
import PushNotificationPage from "../notifications/PushNotificationPage.jsx";
import OrganizationManagement from "./OrganizationManagement/OrganizationManagement";
import OrganizationAddModify from "./OrganizationManagement/OrganizationAddModify";
import UserManagement from "./userManagement/UserManagement";
import UserAddModify from "./userManagement/UserAddModify";
import ManageMethod from "./testManagement/ManageMethod.js";
import BatchTestReassignmentAndCancelation from "./BatchTestReassignmentAndCancellation/BatchTestReassignmentAndCancelation.js";
import TestNotificationConfigMenu from "./testNotificationConfigMenu/TestNotificationConfigMenu.js";
import TestNotificationConfigEdit from "./testNotificationConfigMenu/TestNotificationConfigEdit.js";
import SearchIndexManagement from "./searchIndexManagement/SearchIndexManagement";
import TestManagementConfigMenu from "./testManagementConfigMenu/TestManagementConfigMenu.js";
import ResultSelectListAdd from "./testManagementConfigMenu/ResultSelectListAdd.js";
import TestAdd from "./testManagementConfigMenu/TestAdd.js";
import TestModifyEntry from "./testManagementConfigMenu/TestModifyEntry.js";
import TestOrderability from "./testManagementConfigMenu/TestOrderability.js";
import MethodCreate from "./testManagementConfigMenu/MethodCreate.js";
import TestSectionManagement from "./testManagementConfigMenu/TestSectionManagement.js";
import TestSectionCreate from "./testManagementConfigMenu/TestSectionCreate.js";
import TestSectionOrder from "./testManagementConfigMenu/TestSectionOrder.js";
import SampleTypeManagement from "./testManagementConfigMenu/SampleTypeManagement.js";
import TestSectionTestAssign from "./testManagementConfigMenu/TestSectionTestAssign.js";
import SampleTypeOrder from "./testManagementConfigMenu/SampleTypeOrder.js";
import SampleTypeCreate from "./testManagementConfigMenu/SampleTypeCreate.js";
import SampleTypeTestAssign from "./testManagementConfigMenu/SampleTypeTestAssign.js";
import UomManagement from "./testManagementConfigMenu/UomManagement.js";
import UomCreate from "./testManagementConfigMenu/UomCreate.js";
import PanelManagement from "./testManagementConfigMenu/PanelManagement.js";
import PanelCreate from "./testManagementConfigMenu/PanelCreate.js";
import PanelOrder from "./testManagementConfigMenu/PanelOrder.js";
import PanelTestAssign from "./testManagementConfigMenu/PanelTestAssign.js";
import TestActivation from "./testManagementConfigMenu/TestActivation.js";
import TestRenameEntry from "./testManagementConfigMenu/TestRenameEntry.js";
import PanelRenameEntry from "./testManagementConfigMenu/PanelRenameEntry.js";
import SampleTypeRenameEntry from "./testManagementConfigMenu/SampleTypeRenameEntry.js";
import TestSectionRenameEntry from "./testManagementConfigMenu/TestSectionRenameEntry.js";
import UomRenameEntry from "./testManagementConfigMenu/UomRenameEntry.js";
import SelectListRenameEntry from "./testManagementConfigMenu/SelectListRenameEntry.js";
import MethodRenameEntry from "./testManagementConfigMenu/MethodRenameEntry.js";

function Admin() {
  const intl = useIntl();
  const { path } = useRouteMatch();
  const history = useHistory();
  const [isSmallScreen, setIsSmallScreen] = useState(false);

  // Navigation handler to prevent page reload
  const handleNavigation = (targetPath) => (e) => {
    e.preventDefault();
    history.push(targetPath);
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
            renderIcon={Catalog}
            target="_blank"
            href={config.serverBaseUrl + "/MasterListsPage"}
          >
            <FormattedMessage id="admin.legacy" />
          </SideNavLink>
        </SideNavItems>
      </SideNav>

      <Switch>
        <Route path={`${path}/reflex`} component={ReflexTestManagement} />
        <Route path={`${path}/calculatedValue`} component={CalculatedValue} />
        <Route path={`${path}/TestCatalog`} component={TestCatalog} />
        <Route path={`${path}/MethodManagement`} component={ManageMethod} />
        <Route path={`${path}/AnalyzerTestName`} component={AnalyzerTestName} />
        <Route path={`${path}/labNumber`} component={LabNumberManagement} />
        <Route path={`${path}/program`} component={ProgramManagement} />
        <Route path={`${path}/providerMenu`} component={ProviderMenu} />
        <Route path={`${path}/NotifyUser`} component={PushNotificationPage} />
        <Route
          path={`${path}/barcodeConfiguration`}
          component={BarcodeConfiguration}
        />
        <Route
          path={`${path}/organizationManagement`}
          component={OrganizationManagement}
        />
        <Route
          path={`${path}/organizationEdit`}
          component={OrganizationAddModify}
        />
        <Route
          path={`${path}/resultReportingConfiguration`}
          component={ResultReportingConfiguration}
        />
        <Route path={`${path}/userManagement`} component={UserManagement} />
        <Route
          path={`${path}/batchTestReassignment`}
          component={BatchTestReassignmentAndCancelation}
        />
        <Route path={`${path}/userEdit`} component={UserAddModify} />
        <Route
          path={`${path}/globalMenuManagement`}
          component={GlobalMenuManagement}
        />
        <Route
          path={`${path}/billingMenuManagement`}
          component={BillingMenuManagement}
        />
        <Route
          path={`${path}/nonConformityMenuManagement`}
          component={NonConformityMenuManagement}
        />
        <Route
          path={`${path}/patientMenuManagement`}
          component={PatientMenuManagement}
        />
        <Route
          path={`${path}/studyMenuManagement`}
          component={StudyMenuManagement}
        />
        <Route path={`${path}/commonproperties`} component={CommonProperties} />
        <Route
          path={`${path}/testManagementConfigMenu`}
          component={TestManagementConfigMenu}
        />
        <Route
          path={`${path}/ResultSelectListAdd`}
          component={ResultSelectListAdd}
        />
        <Route path={`${path}/TestAdd`} component={TestAdd} />
        <Route path={`${path}/TestModifyEntry`} component={TestModifyEntry} />
        <Route path={`${path}/TestOrderability`} component={TestOrderability} />
        <Route path={`${path}/MethodCreate`} component={MethodCreate} />
        <Route
          path={`${path}/TestSectionManagement`}
          component={TestSectionManagement}
        />
        <Route
          path={`${path}/TestSectionCreate`}
          component={TestSectionCreate}
        />
        <Route path={`${path}/TestSectionOrder`} component={TestSectionOrder} />
        <Route
          path={`${path}/TestSectionTestAssign`}
          component={TestSectionTestAssign}
        />
        <Route
          path={`${path}/SampleTypeManagement`}
          component={SampleTypeManagement}
        />
        <Route path={`${path}/SampleTypeCreate`} component={SampleTypeCreate} />
        <Route path={`${path}/SampleTypeOrder`} component={SampleTypeOrder} />
        <Route
          path={`${path}/SampleTypeTestAssign`}
          component={SampleTypeTestAssign}
        />
        <Route path={`${path}/UomManagement`} component={UomManagement} />
        <Route path={`${path}/UomCreate`} component={UomCreate} />
        <Route path={`${path}/PanelManagement`} component={PanelManagement} />
        <Route path={`${path}/PanelCreate`} component={PanelCreate} />
        <Route path={`${path}/PanelOrder`} component={PanelOrder} />
        <Route path={`${path}/PanelTestAssign`} component={PanelTestAssign} />
        <Route path={`${path}/TestActivation`} component={TestActivation} />
        <Route path={`${path}/TestRenameEntry`} component={TestRenameEntry} />
        <Route path={`${path}/PanelRenameEntry`} component={PanelRenameEntry} />
        <Route
          path={`${path}/SampleTypeRenameEntry`}
          component={SampleTypeRenameEntry}
        />
        <Route
          path={`${path}/TestSectionRenameEntry`}
          component={TestSectionRenameEntry}
        />
        <Route path={`${path}/UomRenameEntry`} component={UomRenameEntry} />
        <Route
          path={`${path}/SelectListRenameEntry`}
          component={SelectListRenameEntry}
        />
        <Route
          path={`${path}/MethodRenameEntry`}
          component={MethodRenameEntry}
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
          component={TestNotificationConfigMenu}
        />
        <Route
          path={`${path}/testNotificationConfig`}
          component={TestNotificationConfigEdit}
        />
        <Route
          path={`${path}/DictionaryMenu`}
          component={DictionaryManagement}
        />
        <Route path={`${path}/PluginFile`} component={PluginList} />
        <Route
          path={`${path}/SearchIndexManagement`}
          component={SearchIndexManagement}
        />
      </Switch>
    </>
  );
}

export default injectIntl(Admin);
