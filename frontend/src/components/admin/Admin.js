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
            <SideNavMenuItem href="#TestCatalog">
              <FormattedMessage id="sidenav.label.admin.testmgt.ViewtestCatalog" />
            </SideNavMenuItem>
            <SideNavMenuItem href="#MethodManagment">
              <FormattedMessage id="sidenav.label.admin.testmgt.ManageMethod" />
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
      <PathRoute path="#MethodManagment">
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
