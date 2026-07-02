import React from "react";
import { injectIntl } from "react-intl";
import { Switch, Route, useRouteMatch } from "react-router-dom";
import "../Style.css";
import ReflexTestManagement from "./reflexTests/ReflexTestManagement";
import CalendarManagement from "./calendarManagement";
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
import CalculatedValue from "./calculatedValue/CalculatedValueForm";
import { CommonProperties } from "./menu/CommonProperties";
import ConfigMenuDisplay from "./generalConfig/common/ConfigMenuDisplay";
import SiteBrandingConfig from "./generalConfig/siteBranding/SiteBrandingConfig";
import ProviderMenu from "./ProviderMenu/ProviderMenu";
import DataExportStatus from "./DataExportStatus/DataExportStatus";
import LabelPresetList from "./labelPresets/LabelPresetList";
import { Redirect } from "react-router-dom";
import AnalyzerTestName from "./analyzerTestName/AnalyzerTestName";
import PluginList from "./pluginFile/PluginFile";
import ResultReportingConfiguration from "./ResultReportingConfiguration/ResultReportingConfiguration";
import TestCatalog from "./testManagement/ViewTestCatalog";
import TestCatalogEditor from "./testCatalog/TestCatalogEditor";
import CombinedTestEditor from "./testCatalog/CombinedTestEditor";
import TestCatalogList from "./testCatalog/TestCatalogList";
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
import AdminDashboard from "./AdminDashboard";

function Admin() {
  const { path } = useRouteMatch();

  return (
    <Switch>
      <Route
        path={`${path}/calendarManagement`}
        component={CalendarManagement}
      />
      <Route path={`${path}/reflex`} component={ReflexTestManagement} />
      <Route path={`${path}/calculatedValue`} component={CalculatedValue} />
      <Route path={`${path}/TestCatalog`} component={TestCatalog} />
      <Route path={`${path}/TestCatalogList`} component={TestCatalogList} />
      <Route
        path={`${path}/TestCatalogEditor/group/:ids/:section?`}
        component={CombinedTestEditor}
      />
      <Route
        path={`${path}/TestCatalogEditor/:testId?/:section?`}
        component={TestCatalogEditor}
      />
      <Route path={`${path}/MethodManagement`} component={ManageMethod} />
      <Route path={`${path}/AnalyzerTestName`} component={AnalyzerTestName} />
      <Route path={`${path}/labNumber`} component={LabNumberManagement} />
      <Route path={`${path}/labelPresets`} component={LabelPresetList} />
      <Route path={`${path}/program`} component={ProgramManagement} />
      <Route path={`${path}/providerMenu`} component={ProviderMenu} />
      <Route path={`${path}/dataExportStatus`} component={DataExportStatus} />
      <Route path={`${path}/NotifyUser`} component={PushNotificationPage} />
      <Redirect
        from={`${path}/barcodeConfiguration`}
        to={`${path}/labelPresets`}
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
      <Route path={`${path}/SiteBrandingMenu`} component={SiteBrandingConfig} />
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
      {/* OGC-1112 FR-38: the legacy 7-step create wizard is retired in favor of
          create-in-place; any link to it lands on the unified New test flow. */}
      <Route
        path={`${path}/TestAdd`}
        render={() => (
          <Redirect to={`${path}/TestCatalogEditor/new/basic-info`} />
        )}
      />
      <Route path={`${path}/TestModifyEntry`} component={TestModifyEntry} />
      <Route path={`${path}/TestOrderability`} component={TestOrderability} />
      <Route path={`${path}/MethodCreate`} component={MethodCreate} />
      <Route
        path={`${path}/TestSectionManagement`}
        component={TestSectionManagement}
      />
      <Route path={`${path}/TestSectionCreate`} component={TestSectionCreate} />
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
      <Route path={`${path}/MethodRenameEntry`} component={MethodRenameEntry} />
      <Route
        path={`${path}/languageManagement`}
        component={LanguageManagement}
      />
      <Route
        path={`${path}/translationManagement`}
        component={TranslationManagement}
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
      <Route path={`${path}/DictionaryMenu`} component={DictionaryManagement} />
      <Route path={`${path}/PluginFile`} component={PluginList} />
      <Route
        path={`${path}/SearchIndexManagement`}
        component={SearchIndexManagement}
      />
      <Route path={`${path}/loggingManagement`} component={LoggingManagement} />
      <Route
        path={`${path}/externalConnections`}
        component={ExternalConnectionMenu}
      />
      <Route
        path={`${path}/externalConnectionEdit`}
        component={ExternalConnectionAddModify}
      />
      <Route path={`${path}/DatabaseCleaning`} component={DatabaseCleaning} />
      <Route
        path={path}
        exact
        render={() => <AdminDashboard basePath={path} />}
      />
    </Switch>
  );
}

export default injectIntl(Admin);
