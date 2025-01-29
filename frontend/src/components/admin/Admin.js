import React, { useState, useEffect } from "react";
import config from "../../config.json";
import { FormattedMessage, useIntl, injectIntl } from "react-intl";
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
import PathRoute from "../utils/PathRoute";
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
import MethodManagement from "./testManagementConfigMenu/MethodManagement.js";
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
  const [isSmallScreen, setIsSmallScreen] = useState(false);

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
            renderIcon={Microscope}
            title={intl.formatMessage({ id: "sidenav.label.admin.testmgt" })}
          >
            <SideNavMenuItem href="#reflex">
              <FormattedMessage id="sidenav.label.admin.testmgt.reflex" />
            </SideNavMenuItem>
            <SideNavMenuItem href="#calculatedValue">
              <FormattedMessage id="sidenav.label.admin.testmgt.calculated" />
            </SideNavMenuItem>
          </SideNavMenu>
          <SideNavLink href="#AnalyzerTestName" renderIcon={ListDropdown}>
            <FormattedMessage id="sidenav.label.admin.analyzerTest" />
          </SideNavLink>
          <SideNavLink href="#labNumber" renderIcon={CharacterWholeNumber}>
            <FormattedMessage id="sidenav.label.admin.labNumber" />
          </SideNavLink>
          <SideNavLink renderIcon={ChartBubble} href="#program">
            <FormattedMessage id="sidenav.label.admin.program" />
          </SideNavLink>
          <SideNavLink renderIcon={CicsSystemGroup} href="#providerMenu">
            <FormattedMessage id="provider.browse.title" />
          </SideNavLink>
          <SideNavLink renderIcon={QrCode} href="#barcodeConfiguration">
            <FormattedMessage id="sidenav.label.admin.barcodeconfiguration" />
          </SideNavLink>
          <SideNavLink href="#PluginFile" renderIcon={BootVolumeAlt}>
            <FormattedMessage id="sidenav.label.admin.Listplugin" />
          </SideNavLink>
          <SideNavLink
            renderIcon={ContainerSoftware}
            href="#organizationManagement"
          >
            <FormattedMessage id="organization.main.title" />
          </SideNavLink>
          <SideNavLink renderIcon={Report} href="#resultReportingConfiguration">
            <FormattedMessage id="resultreporting.browse.title" />
          </SideNavLink>
          <SideNavLink renderIcon={User} href="#userManagement">
            <FormattedMessage id="unifiedSystemUser.browser.title" />
          </SideNavLink>
          <SideNavLink renderIcon={BatchJob} href="#batchTestReassignment">
            <FormattedMessage id="configuration.batch.test.reassignment" />
          </SideNavLink>
          <SideNavLink renderIcon={ResultNew} href="#testManagementConfigMenu">
            <FormattedMessage id="master.lists.page.test.management" />
          </SideNavLink>
          <SideNavMenu
            title={intl.formatMessage({ id: "sidenav.label.admin.menu" })}
            renderIcon={TableOfContents}
          >
            <SideNavMenuItem href="#globalMenuManagement">
              <FormattedMessage id="sidenav.label.admin.menu.global" />
            </SideNavMenuItem>
            <SideNavMenuItem href="#billingMenuManagement">
              <FormattedMessage id="sidenav.label.admin.menu.billing" />
            </SideNavMenuItem>
            <SideNavMenuItem href="#nonConformityMenuManagement">
              <FormattedMessage id="sidenav.label.admin.menu.nonconform" />
            </SideNavMenuItem>
            <SideNavMenuItem href="#patientMenuManagement">
              <FormattedMessage id="sidenav.label.admin.menu.patient" />
            </SideNavMenuItem>
            <SideNavMenuItem href="#studyMenuManagement">
              <FormattedMessage id="sidenav.label.admin.menu.study" />
            </SideNavMenuItem>
          </SideNavMenu>

          <SideNavMenu
            title={intl.formatMessage({ id: "admin.formEntryConfig" })}
            renderIcon={ListDropdown}
          >
            <SideNavMenuItem href="#NonConformityConfigurationMenu">
              <FormattedMessage id="sidenav.label.admin.formEntry.nonconformityconfig" />
            </SideNavMenuItem>
            <SideNavMenuItem href="#MenuStatementConfigMenu">
              <FormattedMessage id="sidenav.label.admin.formEntry.menustatementconfig" />
            </SideNavMenuItem>
            <SideNavMenuItem href="#WorkPlanConfigurationMenu">
              <FormattedMessage id="sidenav.label.admin.formEntry.Workplanconfig" />
            </SideNavMenuItem>
            <SideNavMenuItem href="#SiteInformationMenu">
              <FormattedMessage id="sidenav.label.admin.formEntry.siteInfoconfig" />
            </SideNavMenuItem>
            <SideNavMenuItem href="#ResultConfigurationMenu">
              <FormattedMessage id="sidenav.label.admin.formEntry.resultConfig" />
            </SideNavMenuItem>
            <SideNavMenuItem href="#PatientConfigurationMenu">
              <FormattedMessage id="sidenav.label.admin.formEntry.patientconfig" />
            </SideNavMenuItem>
            <SideNavMenuItem href="#PrintedReportsConfigurationMenu">
              <FormattedMessage id="sidenav.label.admin.formEntry.PrintedReportsconfig" />
            </SideNavMenuItem>
            <SideNavMenuItem href="#SampleEntryConfigurationMenu">
              <FormattedMessage id="sidenav.label.admin.formEntry.sampleEntryconfig" />
            </SideNavMenuItem>
            <SideNavMenuItem href="#ValidationConfigurationMenu">
              <FormattedMessage id="sidenav.label.admin.formEntry.validationconfig" />
            </SideNavMenuItem>
          </SideNavMenu>

          <SideNavLink href="#commonproperties" renderIcon={Settings}>
            <FormattedMessage
              id="sidenav.label.admin.commonproperties"
              defaultMessage={"Common Properties"}
            />
          </SideNavLink>
          <SideNavLink href="#testNotificationConfigMenu" renderIcon={Popup}>
            <FormattedMessage id="testnotificationconfig.browse.title" />
          </SideNavLink>
          <SideNavLink href="#DictionaryMenu" renderIcon={CharacterWholeNumber}>
            <FormattedMessage id="dictionary.label.modify" />
          </SideNavLink>
          <SideNavLink href="#NotifyUser" renderIcon={Bullhorn}>
            <FormattedMessage id="Notify User" />
          </SideNavLink>
          <SideNavLink href="#SearchIndexManagement" renderIcon={Search}>
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

      <PathRoute path="#reflex">
        <ReflexTestManagement />
      </PathRoute>
      <PathRoute path="#calculatedValue">
        <CalculatedValue />
      </PathRoute>
      <PathRoute path="#TestCatalog">
        <TestCatalog />
      </PathRoute>
      <PathRoute path="#MethodManagement">
        <ManageMethod />
      </PathRoute>
      <PathRoute path="#AnalyzerTestName">
        <AnalyzerTestName />
      </PathRoute>
      <PathRoute path="#labNumber">
        <LabNumberManagement />
      </PathRoute>
      <PathRoute path="#program">
        <ProgramManagement />
      </PathRoute>
      <PathRoute path="#providerMenu">
        <ProviderMenu />
      </PathRoute>
      <PathRoute path="#NotifyUser">
        <PushNotificationPage />
      </PathRoute>
      <PathRoute path="#barcodeConfiguration">
        <BarcodeConfiguration />
      </PathRoute>
      <PathRoute path="#organizationManagement">
        <OrganizationManagement />
      </PathRoute>
      <PathRoute path="#organizationEdit">
        <OrganizationAddModify />
      </PathRoute>
      <PathRoute path="#resultReportingConfiguration">
        <ResultReportingConfiguration />
      </PathRoute>
      <PathRoute path="#userManagement">
        <UserManagement />
      </PathRoute>
      <PathRoute path="#batchTestReassignment">
        <BatchTestReassignmentAndCancelation />
      </PathRoute>
      <PathRoute path="#userEdit">
        <UserAddModify />
      </PathRoute>
      <PathRoute path="#globalMenuManagement">
        <GlobalMenuManagement />
      </PathRoute>
      <PathRoute path="#billingMenuManagement">
        <BillingMenuManagement />
      </PathRoute>
      <PathRoute path="#nonConformityMenuManagement">
        <NonConformityMenuManagement />
      </PathRoute>
      <PathRoute path="#patientMenuManagement">
        <PatientMenuManagement />
      </PathRoute>
      <PathRoute path="#studyMenuManagement">
        <StudyMenuManagement />
      </PathRoute>
      <PathRoute path="#commonproperties">
        <CommonProperties />
      </PathRoute>
      <PathRoute path="#testManagementConfigMenu">
        <TestManagementConfigMenu />
      </PathRoute>
      <PathRoute path="#ResultSelectListAdd">
        <ResultSelectListAdd />
      </PathRoute>
      <PathRoute path="#TestAdd">
        <TestAdd />
      </PathRoute>
      <PathRoute path="#TestModifyEntry">
        <TestModifyEntry />
      </PathRoute>
      <PathRoute path="#TestOrderability">
        <TestOrderability />
      </PathRoute>
      <PathRoute path="#MethodCreate">
        <MethodCreate />
      </PathRoute>
      <PathRoute path="#TestSectionManagement">
        <TestSectionManagement />
      </PathRoute>
      <PathRoute path="#TestSectionCreate">
        <TestSectionCreate />
      </PathRoute>
      <PathRoute path="#TestSectionOrder">
        <TestSectionOrder />
      </PathRoute>
      <PathRoute path="#TestSectionTestAssign">
        <TestSectionTestAssign />
      </PathRoute>
      <PathRoute path="#SampleTypeManagement">
        <SampleTypeManagement />
      </PathRoute>
      <PathRoute path="#SampleTypeCreate">
        <SampleTypeCreate />
      </PathRoute>
      <PathRoute path="#SampleTypeOrder">
        <SampleTypeOrder />
      </PathRoute>
      <PathRoute path="#SampleTypeTestAssign">
        <SampleTypeTestAssign />
      </PathRoute>
      <PathRoute path="#UomManagement">
        <UomManagement />
      </PathRoute>
      <PathRoute path="#UomCreate">
        <UomCreate />
      </PathRoute>
      <PathRoute path="#PanelManagement">
        <PanelManagement />
      </PathRoute>
      <PathRoute path="#PanelCreate">
        <PanelCreate />
      </PathRoute>
      <PathRoute path="#PanelOrder">
        <PanelOrder />
      </PathRoute>
      <PathRoute path="#PanelTestAssign">
        <PanelTestAssign />
      </PathRoute>
      <PathRoute path="#TestActivation">
        <TestActivation />
      </PathRoute>
      <PathRoute path="#TestRenameEntry">
        <TestRenameEntry />
      </PathRoute>
      <PathRoute path="#PanelRenameEntry">
        <PanelRenameEntry />
      </PathRoute>
      <PathRoute path="#SampleTypeRenameEntry">
        <SampleTypeRenameEntry />
      </PathRoute>
      <PathRoute path="#TestSectionRenameEntry">
        <TestSectionRenameEntry />
      </PathRoute>
      <PathRoute path="#UomRenameEntry">
        <UomRenameEntry />
      </PathRoute>
      <PathRoute path="#SelectListRenameEntry">
        <SelectListRenameEntry />
      </PathRoute>
      <PathRoute path="#MethodRenameEntry">
        <MethodRenameEntry />
      </PathRoute>

      <PathRoute path="#NonConformityConfigurationMenu">
        <ConfigMenuDisplay
          menuType="NonConformityConfigurationMenu"
          label="Non Conformity Configuration Menu"
          id="sidenav.label.admin.formEntry.nonconformityconfig"
        />
      </PathRoute>
      <PathRoute path="#MenuStatementConfigMenu">
        <ConfigMenuDisplay
          menuType="MenuStatementConfigMenu"
          label="Menu Statement Configuration Menu"
          id="sidenav.label.admin.formEntry.menustatementconfig"
        />
      </PathRoute>
      <PathRoute path="#ValidationConfigurationMenu">
        <ConfigMenuDisplay
          menuType="ValidationConfigurationMenu"
          label="Validation Configuration Menu"
          id="sidenav.label.admin.formEntry.validationconfig"
        />
      </PathRoute>
      <PathRoute path="#SampleEntryConfigurationMenu">
        <ConfigMenuDisplay
          menuType="SampleEntryConfigMenu"
          label="Sample Entry Configuration Menu"
          id="sidenav.label.admin.formEntry.sampleEntryconfig"
        />
      </PathRoute>
      <PathRoute path="#WorkPlanConfigurationMenu">
        <ConfigMenuDisplay
          menuType="WorkplanConfigurationMenu"
          label="WorkPlan Configuration Menu"
          id="sidenav.label.admin.formEntry.Workplanconfig"
        />
      </PathRoute>
      <PathRoute path="#SiteInformationMenu">
        <ConfigMenuDisplay
          menuType="SiteInformationMenu"
          label="Site Information Menu"
          id="sidenav.label.admin.formEntry.siteInfoconfig"
        />
      </PathRoute>
      <PathRoute path="#ResultConfigurationMenu">
        <ConfigMenuDisplay
          menuType="ResultConfigurationMenu"
          label="Result Configuration Menu"
          id="sidenav.label.admin.formEntry.resultConfig"
        />
      </PathRoute>
      <PathRoute path="#PatientConfigurationMenu">
        <ConfigMenuDisplay
          menuType="PatientConfigurationMenu"
          label="Patient Configuration Menu"
          id="sidenav.label.admin.formEntry.patientconfig"
        />
      </PathRoute>
      <PathRoute path="#PrintedReportsConfigurationMenu">
        <ConfigMenuDisplay
          menuType="PrintedReportsConfigurationMenu"
          label="PrintedReports Configuration Menu"
          id="sidenav.label.admin.formEntry.PrintedReportsconfig"
        />
      </PathRoute>
      <PathRoute path="#testNotificationConfigMenu">
        <TestNotificationConfigMenu />
      </PathRoute>
      <PathRoute path="#testNotificationConfig">
        <TestNotificationConfigEdit />
      </PathRoute>
      <PathRoute path="#DictionaryMenu">
        <DictionaryManagement />
      </PathRoute>
      <PathRoute path="#PluginFile">
        <PluginList />
      </PathRoute>
      <PathRoute path="#SearchIndexManagement">
        <SearchIndexManagement />
      </PathRoute>
    </>
  );
}

export default injectIntl(Admin);
