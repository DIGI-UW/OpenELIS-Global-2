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
import ProviderTitleMenu from "./providerTitle/ProviderTitleMenu";
import DataExportStatus from "./DataExportStatus/DataExportStatus";
import LabelPresetList from "./labelPresets/LabelPresetList";
import { Redirect } from "react-router-dom";
import ResultReportingConfiguration from "./ResultReportingConfiguration/ResultReportingConfiguration";
import TestCatalogEditor from "./testCatalog/TestCatalogEditor";
import PanelEditor from "./testCatalog/PanelEditor";
import CombinedTestEditor from "./testCatalog/CombinedTestEditor";
import TestCatalogList from "./testCatalog/TestCatalogList";
import CatalogImport from "./catalogImport/CatalogImport";
import PushNotificationPage from "../notifications/PushNotificationPage.jsx";
import LocationsPage, {
  legacyOrganizationEditTarget,
} from "./locations/LocationsPage";
import UserManagement from "./userManagement/UserManagement";
import RoleManagement from "./roleManagement/RoleManagement";
import UserAddModify from "./userManagement/UserAddModify";
import ManageMethod from "./testManagement/ManageMethod";
import BatchTestReassignmentAndCancelation from "./BatchTestReassignmentAndCancellation/BatchTestReassignmentAndCancelation";
import TestNotificationConfigMenu from "./testNotificationConfigMenu/TestNotificationConfigMenu";
import TestNotificationConfigEdit from "./testNotificationConfigMenu/TestNotificationConfigEdit";
import NotificationTriggerConfig from "./notificationTriggerConfig/NotificationTriggerConfig";
import SearchIndexManagement from "./searchIndexManagement/SearchIndexManagement";
import LoggingManagement from "./loggingManagement/LoggingManagement";
import TestManagementConfigMenu from "./testManagementConfigMenu/TestManagementConfigMenu";
import SampleTypeEditor from "./sampleTypeManagement/SampleTypeManagement.jsx";
import LabUnitManagement from "./labUnitManagement/LabUnitManagement.jsx";
import ComplianceStandardsAdmin from "./complianceStandards/ComplianceStandardsAdmin";
import {
  LanguageManagement,
  TranslationManagement,
} from "./localizationManagement";
import ExternalConnectionMenu from "./externalConnections/ExternalConnectionMenu";
import ExternalConnectionAddModify from "./externalConnections/ExternalConnectionAddModify";
import DatabaseCleaning from "./databaseCleaning/DatabaseCleaning";
import VectorSurveillanceSetup from "./vectorSurveillance/VectorSurveillanceSetup";
import SampleAcceptanceChecklistSetup from "./sampleAcceptance/SampleAcceptanceChecklistSetup";
import AdminDashboard from "./AdminDashboard";
import StuckAnalyzerEvents from "./StuckAnalyzerEvents";
import MicrobiologyReferenceAdmin from "./microbiologyReference/MicrobiologyReferenceAdmin";

function Admin() {
  const { path } = useRouteMatch();

  return (
    <Switch>
      <Route
        path={`${path}/MicrobiologyReference/:section/:detailId?`}
        component={MicrobiologyReferenceAdmin}
      />
      <Route
        path={`${path}/calendarManagement`}
        component={CalendarManagement}
      />
      <Route path={`${path}/reflex`} component={ReflexTestManagement} />
      <Route path={`${path}/calculatedValue`} component={CalculatedValue} />
      {/* The legacy View Test Catalog page is gone; its address opens the list. */}
      <Redirect
        exact
        from={`${path}/TestCatalog`}
        to={`${path}/TestCatalogList`}
      />
      <Route path={`${path}/TestCatalogList`} component={TestCatalogList} />
      <Route path={`${path}/CatalogImport`} component={CatalogImport} />
      <Route
        path={`${path}/stuckAnalyzerEvents`}
        render={() => <StuckAnalyzerEvents basePath={path} />}
      />
      <Route
        path={`${path}/TestCatalogEditor/group/:ids/:section?`}
        component={CombinedTestEditor}
      />
      {/* OGC-224 — panel entity segment must precede the generic :testId
          route, which would otherwise swallow "panel" as a test id. */}
      <Route
        path={`${path}/TestCatalogEditor/panel/:panelId/:section?`}
        component={PanelEditor}
      />
      <Route
        path={`${path}/TestCatalogEditor/:testId?/:section?`}
        component={TestCatalogEditor}
      />
      <Route path={`${path}/MethodManagement`} component={ManageMethod} />
      <Route path={`${path}/labNumber`} component={LabNumberManagement} />
      <Route path={`${path}/labelPresets`} component={LabelPresetList} />
      {/* OGC-781: the Programs rework keeps the live /program URL so bookmarks
          and deep links survive; /programV2 was its pre-release alias. */}
      <Redirect from={`${path}/programV2`} to={`${path}/program`} />
      <Route path={`${path}/program`} component={ProgramManagement} />
      <Route path={`${path}/providerMenu`} component={ProviderMenu} />
      <Route path={`${path}/providerTitleMenu`} component={ProviderTitleMenu} />
      <Route path={`${path}/dataExportStatus`} component={DataExportStatus} />
      <Route path={`${path}/NotifyUser`} component={PushNotificationPage} />
      <Redirect
        from={`${path}/barcodeConfiguration`}
        to={`${path}/labelPresets`}
      />
      {/* OGC-1363: Locations & Organizations replaces Organization Management
          and absorbs the vector Sampling Sites page; the old routes redirect. */}
      <Route path={`${path}/locations`} component={LocationsPage} />
      <Redirect
        from={`${path}/organizationManagement`}
        to={`${path}/locations`}
      />
      <Route
        path={`${path}/organizationEdit`}
        render={({ location }) => (
          <Redirect to={legacyOrganizationEditTarget(path, location)} />
        )}
      />
      <Redirect
        from={`${path}/vectorSurveillanceSetup/sampling-sites`}
        to={`${path}/locations/sites`}
      />
      <Route
        path={`${path}/resultReportingConfiguration`}
        component={ResultReportingConfiguration}
      />
      <Route path={`${path}/userManagement`} component={UserManagement} />
      <Route path={`${path}/roleManagement`} component={RoleManagement} />
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
      {/* The legacy test-management pages are gone. Every address they had
          opens the editor that replaced it: tests, panels, sample types and
          lab units have editors of their own; result select lists are
          dictionary entries; methods are listed under Methods. */}
      <Route
        path={`${path}/TestAdd`}
        render={() => (
          <Redirect to={`${path}/TestCatalogEditor/new/basic-info`} />
        )}
      />
      <Route
        path={[
          `${path}/TestModifyEntry`,
          `${path}/TestActivation`,
          `${path}/TestOrderability`,
          `${path}/TestRenameEntry`,
          `${path}/UomManagement`,
          `${path}/UomCreate`,
          `${path}/UomRenameEntry`,
        ]}
        render={() => <Redirect to={`${path}/TestCatalogList`} />}
      />
      <Route
        path={[
          `${path}/PanelManagement`,
          `${path}/PanelCreate`,
          `${path}/PanelOrder`,
          `${path}/PanelTestAssign`,
          `${path}/PanelRenameEntry`,
        ]}
        render={() => <Redirect to={`${path}/TestCatalogList?entity=panels`} />}
      />
      <Route
        path={[
          `${path}/SampleTypeManagement`,
          `${path}/SampleTypeCreate`,
          `${path}/SampleTypeOrder`,
          `${path}/SampleTypeTestAssign`,
          `${path}/SampleTypeRenameEntry`,
        ]}
        render={() => <Redirect to={`${path}/SampleTypeEditor`} />}
      />
      <Route
        path={`${path}/SampleTypeEditor/:sampleTypeId?/:section?`}
        component={SampleTypeEditor}
      />
      <Route
        path={[
          `${path}/TestSectionManagement`,
          `${path}/TestSectionCreate`,
          `${path}/TestSectionOrder`,
          `${path}/TestSectionTestAssign`,
          `${path}/TestSectionEdit`,
          `${path}/TestSectionRenameEntry`,
        ]}
        render={() => <Redirect to={`${path}/LabUnitManagement`} />}
      />
      <Route
        path={`${path}/LabUnitManagement/:labUnitId?/:section?`}
        component={LabUnitManagement}
      />
      <Route
        path={[`${path}/ResultSelectListAdd`, `${path}/SelectListRenameEntry`]}
        render={() => <Redirect to={`${path}/DictionaryMenu`} />}
      />
      <Route
        path={[`${path}/MethodCreate`, `${path}/MethodRenameEntry`]}
        render={() => <Redirect to={`${path}/MethodManagement`} />}
      />
      <Route
        path={`${path}/ComplianceStandardsAdmin`}
        component={ComplianceStandardsAdmin}
      />
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
        render={() => (
          <ConfigMenuDisplay
            menuType="NonConformityConfigurationMenu"
            id="sidenav.label.admin.formEntry.nonconformityconfig"
          />
        )}
      />
      <Route
        path={`${path}/MenuStatementConfigMenu`}
        render={() => (
          <ConfigMenuDisplay
            menuType="MenuStatementConfigMenu"
            id="sidenav.label.admin.formEntry.menustatementconfig"
          />
        )}
      />
      <Route
        path={`${path}/ValidationConfigurationMenu`}
        render={() => (
          <ConfigMenuDisplay
            menuType="ValidationConfigurationMenu"
            id="sidenav.label.admin.formEntry.validationconfig"
          />
        )}
      />
      <Route
        path={`${path}/SampleEntryConfigurationMenu`}
        render={() => (
          <ConfigMenuDisplay
            menuType="SampleEntryConfigMenu"
            id="sidenav.label.admin.formEntry.sampleEntryconfig"
          />
        )}
      />
      <Route
        path={`${path}/WorkPlanConfigurationMenu`}
        render={() => (
          <ConfigMenuDisplay
            menuType="WorkplanConfigurationMenu"
            id="sidenav.label.admin.formEntry.Workplanconfig"
          />
        )}
      />
      <Route
        path={`${path}/SiteInformationMenu`}
        render={() => (
          <ConfigMenuDisplay
            menuType="SiteInformationMenu"
            id="sidenav.label.admin.formEntry.siteInfoconfig"
          />
        )}
      />
      <Route
        path={`${path}/ResultConfigurationMenu`}
        render={() => (
          <ConfigMenuDisplay
            menuType="ResultConfigurationMenu"
            id="sidenav.label.admin.formEntry.resultConfig"
          />
        )}
      />
      <Route
        path={`${path}/PatientConfigurationMenu`}
        render={() => (
          <ConfigMenuDisplay
            menuType="PatientConfigurationMenu"
            id="sidenav.label.admin.formEntry.patientconfig"
          />
        )}
      />
      <Route
        path={`${path}/PrintedReportsConfigurationMenu`}
        render={() => (
          <ConfigMenuDisplay
            menuType="PrintedReportsConfigurationMenu"
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
        path={`${path}/notificationTriggerConfig`}
        component={NotificationTriggerConfig}
      />
      <Route path={`${path}/DictionaryMenu`} component={DictionaryManagement} />
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
        path={`${path}/vectorSurveillanceSetup`}
        component={VectorSurveillanceSetup}
      />
      <Route
        path={`${path}/SampleAcceptanceChecklist`}
        component={SampleAcceptanceChecklistSetup}
      />
      <Route
        path={path}
        exact
        render={() => <AdminDashboard basePath={path} />}
      />
    </Switch>
  );
}

export default injectIntl(Admin);
