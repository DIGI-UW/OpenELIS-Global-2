import React, { Suspense, useEffect, useState } from "react";
import { confirmAlert } from "react-confirm-alert";
import { IntlProvider } from "react-intl";
import {
  Redirect,
  Route,
  BrowserRouter as Router,
  Switch,
} from "react-router-dom";
import RedirectOldUI from "./RedirectOldUI";
import UserSessionDetailsContext from "./UserSessionDetailsContext";
import { Admin } from "./components";
import ChangePassword from "./components/ChangePassword";
import Home from "./components/Home";
import Layout from "./components/layout/Layout";
import StorageDashboard from "./components/storage/StorageDashboard";
import SampleItemsPage from "./components/storage/pages/SampleItemsPage";
import ManageLocationPage from "./components/storage/pages/ManageLocationPage";
import RoomsPage from "./components/storage/pages/RoomsPage";
import DevicesPage from "./components/storage/pages/DevicesPage";
import ShelvesPage from "./components/storage/pages/ShelvesPage";
import RacksPage from "./components/storage/pages/RacksPage";
import BoxesPage from "./components/storage/pages/BoxesPage";
import EditLocationPage from "./components/storage/pages/EditLocationPage";
import EditBoxPage from "./components/storage/pages/EditBoxPage";
import AddLocationPage from "./components/storage/pages/AddLocationPage";
import AddBoxPage from "./components/storage/pages/AddBoxPage";
import AlertsDashboard from "./components/alerts/AlertsDashboard";
import EQAProgramManagement from "./components/eqa/EQAProgram/ProgramManagement";
import EQADistributionDashboard from "./components/eqa/EQADistributionDashboard";
import CreateDistribution from "./components/eqa/EQADistribution/CreateDistribution";
import EQAOrdersPage from "./components/eqa/EQAOrdersPage";
import MyCyclesPage from "./components/eqa/MyCycles/MyCyclesPage";
import ProviderWorkbenchPage from "./components/eqa/Provider/Workbench/ProviderWorkbenchPage";
import InHousePanelsPage from "./components/eqa/InHouse/InHousePanelsPage";
import BlindingWizard from "./components/eqa/InHouse/BlindingWizard";
import MyProgramsPage from "./components/eqa/MyProgramsPage";
import EQAParticipantsPage from "./components/eqa/EQAParticipantsPage";
import EQAResultsPage from "./components/eqa/EQAResultsPage";
import QAPlaceholder from "./components/qa/QAPlaceholder";
import QAOverview from "./components/qa/overview/QAOverview";
import QIDashboard from "./components/qa/qi/QIDashboard";
import QIConfigList from "./components/qa/qi/QIConfigList";
import QIEnabledRoute from "./components/qa/qi/QIEnabledRoute";
import AmendmentReport from "./components/qa/qi/AmendmentReport";
import RejectionReport from "./components/qa/qi/RejectionReport";
import CallbackReport from "./components/qa/qi/CallbackReport";
import ESignatureLog from "./components/qa/qms/ESignatureLog";
import CapaRegister from "./components/qa/qms/CapaRegister";
import Accreditation from "./components/qa/qms/Accreditation";
import InventoryManagement from "./components/inventory/InventoryManagement";
import ShipmentDashboard from "./components/shipment/ShipmentDashboard";
import BoxCreation from "./components/shipment/BoxCreation";
import BoxDetails from "./components/shipment/BoxDetails";
import ReceptionWorkflow from "./components/shipment/ReceptionWorkflow";
import Login from "./components/Login";
import LandingPage from "./components/home/LandingPage";

/**
 * Wraps `React.lazy` with retry-on-failure semantics for the dynamic
 * `import()` factory. Handles transient chunk-fetch failures — e.g.
 * Chrome's `ERR_NETWORK_CHANGED` when the browser's network state
 * flickers during a chunk request, or any single failed resource fetch
 * that leaves the lazy component permanently broken until page reload.
 *
 * Without retry, a single chunk-fetch blip crashes the route and
 * surfaces as an E2E failure: the RouteErrorBoundary catches the
 * `TypeError: Failed to fetch dynamically imported module` and shows
 * its "module could not be loaded" fallback. Seen as a recurring
 * develop-CI flake on AnalyzerForm chunk fetch; the retry wrapper
 * gives the browser three chances with backoff before giving up.
 *
 * Backoff is intentionally short (0.5s/1s/1.5s): the real failures
 * are transient TCP / Docker-network conditions that resolve in
 * milliseconds. Longer waits would harm real error reporting when the
 * chunk is genuinely missing (e.g., deploy mismatch).
 */
function lazyWithRetry(factory, retries = 3, backoffMs = 500) {
  // eslint-disable-next-line local/no-raw-react-lazy --
  // This IS the lazyWithRetry helper: it legitimately wraps React.lazy
  // with retry semantics. The rule flags direct callers elsewhere.
  return React.lazy(async () => {
    let lastError;
    for (let attempt = 0; attempt < retries; attempt += 1) {
      try {
        return await factory();
      } catch (err) {
        lastError = err;
        if (attempt < retries - 1) {
          await new Promise((resolve) =>
            setTimeout(resolve, backoffMs * (attempt + 1)),
          );
        }
      }
    }
    throw lastError;
  });
}

const AnalyzersPage = lazyWithRetry(() => import("./pages/AnalyzersPage"));
const FieldMapping = lazyWithRetry(
  () => import("./components/analyzers/FieldMapping/FieldMapping"),
);
const ErrorDashboardPage = lazyWithRetry(
  () => import("./pages/ErrorDashboardPage"),
);
const CustomFieldTypeManagementPage = lazyWithRetry(
  () => import("./pages/CustomFieldTypeManagementPage"),
);
const AnalyzerTypesPage = lazyWithRetry(
  () => import("./pages/AnalyzerTypesPage"),
);
const AnalyzerFormPage = lazyWithRetry(
  () => import("./components/analyzers/AnalyzerForm/AnalyzerForm"),
);
const QcRulePage = lazyWithRetry(
  () => import("./components/analyzers/QcRules/QcRuleBuilderModal"),
);
import {
  QCDashboard,
  ControlChartDetail,
  ControlLotList,
  InstrumentDetailPage,
  ControlLotSetup,
  RuleConfigPanel,
} from "./components/qc";
import ResultSearch from "./components/resultPage/ResultSearch";
import {
  LegacyResultsGate,
  UnifiedResultsRoute,
} from "./components/resultPage/unified/routeGates";
import { getFromOpenElisServer } from "./components/utils/Utils";
import { loadAndApplyBranding } from "./components/utils/BrandingUtils";
import { languages, languageMessages } from "./languages";
import config from "./config.json";
import { SecureRoute } from "./components/security";
import "./index.scss";
import PatientManagement from "./components/patient/PatientManagement";
import PatientHistory from "./components/patient/PatientHistory";
import PatientMerge from "./components/patient/PatientMerge";
import Aliquot from "./components/sample/Aliquot";
import Workplan from "./components/workplan/Workplan";
import AddOrder from "./components/addOrder/Index";
import FindOrder from "./components/modifyOrder/Index";
import ModifyOrder from "./components/modifyOrder/ModifyOrder";
import RoutineReports from "./components/reports/Routine";
import StudyReports from "./components/reports/Study";
import TATReport from "./components/reports/tat";
import StudyValidation from "./components/validation/Index";
const AnalyserResultIndex = lazyWithRetry(
  () => import("./components/analyserResults/Index"),
);
import PathologyDashboard from "./components/pathology/PathologyDashboard";
import CytologyDashboard from "./components/cytology/CytologyDashBoard";
import NoteBookDashBoard from "./components/notebook/NoteBookDashBoard";
import NoteBookEntryForm from "./components/notebook/NoteBookEntryForm";
import CytologyCaseView from "./components/cytology/CytologyCaseView";
import PathologyCaseView from "./components/pathology/PathologyCaseView";
import ImmunohistochemistryDashboard from "./components/immunohistochemistry/ImmunohistochemistryDashboard";
import ImmunohistochemistryCaseView from "./components/immunohistochemistry/ImmunohistochemistryCaseView";
const RoutedResultsViewer = lazyWithRetry(
  () => import("./components/patient/resultsViewer/results-viewer.tsx"),
);
import EOrderPage from "./components/eOrder/Index";
import RoutineIndex from "./components/reports/routine/Index";
import StudyIndex from "./components/reports/study/index";
import ReportIndex from "./components/reports/Index";
import PrintBarcode from "./components/printBarcode/Index";
import NonConformIndex from "./components/nonconform/index";
import SampleBatchEntrySetup from "./components/batchOrderEntry/SampleBatchEntrySetup";
import AuditTrailReportIndex from "./components/reports/auditTrailReport/Index";
import ReferredOutTests from "./components/resultPage/resultsReferredOut/ReferredOutTests";
import { Roles } from "./components/utils/Utils";
import NoteBookInstanceEntryForm from "./components/notebook/NoteBookInstanceEntryForm";
import NotebookSampleOrder from "./components/notebook/NotebookSampleOrder";
const FreezerMonitoringDashboard = lazyWithRetry(
  () => import("./components/coldStorage/FreezerMonitoringDashboard"),
);
import ProgramDashboard from "./components/program/programDashboard.jsx";
import ProgramCaseView from "./components/program/programCaseView.jsx";
import SampleManagement from "./components/sampleManagement/SampleManagement";
const ShipmentReport = lazyWithRetry(
  () => import("./components/shipment/ShipmentReport"),
);

const GenericSampleOrder = lazyWithRetry(
  () => import("./components/genericSample/GenericSampleOrder"),
);
const GenericSampleOrderEdit = lazyWithRetry(
  () => import("./components/genericSample/GenericSampleOrderEdit"),
);
const GenericSampleOrderImport = lazyWithRetry(
  () => import("./components/genericSample/GenericSampleOrderImport"),
);
const GenericSampleResults = lazyWithRetry(
  () => import("./components/genericSample/GenericSampleResults"),
);

import ShipmentSettings from "./components/shipment/ShipmentSettings";
import RouteErrorBoundary from "./components/common/RouteErrorBoundary";
import {
  OrderProvider,
  OrderDashboard,
  OrderEnter,
  OrderCollect,
  OrderLabel,
  OrderQA,
} from "./components/order";

// QA-context breadcrumb for the TAT report mounted at /qa/qi/tat (OGC-696).
// Labels are i18n keys resolved by PageBreadCrumb.
const qaTatBreadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "sideNav.label.qa", link: "/qa/overview" },
  { label: "sideNav.label.qa.qi.dashboard", link: "/qa/qi/dashboard" },
  { label: "reports.tat.title", link: "" },
];

export default function App() {
  const defaultLocale =
    localStorage.getItem("locale") || navigator.language.split(/[-_]/)[0];

  const initialLocale = languages[defaultLocale] ? defaultLocale : "en";

  const [locale, setLocale] = useState(initialLocale);
  const [messages, setMessages] = useState(languages[initialLocale].messages);

  const [userSessionDetails, setUserSessionDetails] = useState({});
  const [errorLoadingSessionDetails, setErrorLoadingSessionDetails] =
    useState(false);

  useEffect(() => {
    getUserSessionDetails();
  }, []);

  // Load and apply site branding (colors, favicon)
  useEffect(() => {
    loadAndApplyBranding();

    // Listen for branding updates from admin UI
    const handleBrandingUpdate = () => {
      loadAndApplyBranding();
    };
    window.addEventListener("branding-updated", handleBrandingUpdate);

    return () => {
      window.removeEventListener("branding-updated", handleBrandingUpdate);
    };
  }, []);

  const getUserSessionDetails = async () => {
    const maxRetries = 10;
    for (let attempt = 0; attempt < maxRetries; attempt++) {
      try {
        const response = await fetch(config.serverBaseUrl + `/session`, {
          credentials: "include",
        });
        if (response.status === 200) {
          const jsonResp = await response.json();
          console.debug(JSON.stringify(jsonResp));
          if (jsonResp.authenticated) {
            localStorage.setItem("CSRF", jsonResp.csrf);
          }
          setUserSessionDetails(jsonResp);
          setErrorLoadingSessionDetails(false);
          return jsonResp;
        } else {
          throw new Error(
            "Did not receive a successful response from the backend while retrieving user session details",
          );
        }
      } catch (error) {
        console.error(error);
        if (attempt < maxRetries - 1) {
          await new Promise((resolve) => setTimeout(resolve, 1000));
        } else {
          const options = {
            title: "System Error",
            message: "Error : " + error.message,
            buttons: [
              {
                label: "OK",
                onClick: () => {
                  window.location.href = window.location.origin;
                },
              },
            ],
            closeOnClickOutside: false,
            closeOnEscape: false,
          };
          confirmAlert(options);
        }
      }
    }
    setErrorLoadingSessionDetails(true);
  };

  const logout = () => {
    if (userSessionDetails.loginMethod === "SAML") {
      fetch(config.serverBaseUrl + "/Logout?useSAML=true", {
        //includes the browser sessionId in the Header for Authentication on the backend server
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
      })
        .then((response) => response.text())
        .then((html) => {
          // Parse the SAML SLO response and submit the form in the current
          // window — no popup, no iframe needed.
          const parser = new DOMParser();
          const doc = parser.parseFromString(html, "text/html");
          const samlForm = doc.querySelector("form");

          if (samlForm) {
            const form = document.createElement("form");
            form.method = samlForm.method || "POST";
            form.action = samlForm.action;
            Array.from(samlForm.querySelectorAll("input")).forEach((input) => {
              const hidden = document.createElement("input");
              hidden.type = "hidden";
              hidden.name = input.name;
              hidden.value = input.value;
              form.appendChild(hidden);
            });
            document.body.appendChild(form);
            form.submit();
          } else {
            // No SAML form in response — fall back to a direct redirect
            getUserSessionDetails();
            window.location.href = config.loginRedirect;
          }
        })
        .catch((error) => {
          console.error(error);
        });
    } else {
      fetch(config.serverBaseUrl + "/Logout", {
        //includes the browser sessionId in the Header for Authentication on the backend server
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
      })
        .then((response) => response.status)
        .then(() => {
          getUserSessionDetails();
          window.location.href = config.loginRedirect;
        })
        .catch((error) => {
          console.error(error);
        });
    }
  };

  const changeLanguageReact = (lang) => {
    // Check if we have messages for this language
    const messages = languageMessages[lang] || languages[lang]?.messages;
    if (!messages) {
      lang = "en";
    }
    setLocale(lang);
    setMessages(languageMessages[lang] || languages["en"].messages);
    localStorage.setItem("locale", lang);
  };

  const changeLanguageBackend = async (lang) => {
    if (userSessionDetails.authenticated) {
      getFromOpenElisServer("/Home?lang=" + lang, () => {
        // Language changed on backend
      });
    } else {
      getFromOpenElisServer("/LoginPage?lang=" + lang, () => {
        // Language changed on backend
      });
    }
  };

  const onChangeLanguage = (lang) => {
    changeLanguageReact(lang);
    changeLanguageBackend(lang);
  };

  const refresh = async (callback) => {
    await getUserSessionDetails();
    if (typeof callback === "function") {
      callback();
    }
  };

  const isCheckingLogin = () => {
    return !("authenticated" in userSessionDetails);
  };

  const routeErrorStorage = {
    titleKey: "errorBoundary.route.storage.title",
    messageKey: "errorBoundary.route.storage.message",
  };

  const routeErrorPatientResultsViewer = {
    titleKey: "errorBoundary.route.patientResultsViewer.title",
    messageKey: "errorBoundary.route.patientResultsViewer.message",
  };

  const routeErrorResultsSearch = {
    titleKey: "errorBoundary.route.resultsSearch.title",
    messageKey: "errorBoundary.route.resultsSearch.message",
  };

  const routeErrorSamplePatientEntry = {
    titleKey: "errorBoundary.route.samplePatientEntry.title",
    messageKey: "errorBoundary.route.samplePatientEntry.message",
  };

  const routeErrorAnalyzers = {
    titleKey: "errorBoundary.route.analyzers.title",
    messageKey: "errorBoundary.route.analyzers.message",
  };

  const routeErrorAnalyzerResults = {
    titleKey: "errorBoundary.route.analyzerResults.title",
    messageKey: "errorBoundary.route.analyzerResults.message",
  };

  return (
    <IntlProvider
      locale={locale}
      key={locale}
      defaultLocale="en"
      messages={messages}
    >
      <UserSessionDetailsContext.Provider
        value={{
          userSessionDetails,
          errorLoadingSessionDetails,
          isCheckingLogin,
          logout,
          refresh,
        }}
      >
        <>
          <Router>
            <Layout onChangeLanguage={onChangeLanguage}>
              <Switch>
                <Route path="/login" exact component={() => <Login />} />
                <Route
                  path="/ChangePasswordLogin"
                  exact
                  component={() => <ChangePassword />}
                />
                <Route
                  path="/landing"
                  exact
                  component={() => <LandingPage />}
                />
                <SecureRoute
                  path="/"
                  exact
                  component={() => <Home />}
                  role=""
                />
                <SecureRoute
                  path="/Dashboard"
                  exact
                  component={() => <Home />}
                  role=""
                />
                <SecureRoute
                  path="/admin"
                  component={() => <Admin />}
                  role={Roles.GLOBAL_ADMIN}
                />
                <SecureRoute
                  path="/MasterListsPage"
                  component={() => <Admin />}
                  role={Roles.GLOBAL_ADMIN}
                />
                <SecureRoute
                  path="/PathologyDashboard"
                  exact
                  component={() => <PathologyDashboard />}
                  role=""
                  labUnitRole={{ Pathology: [Roles.RESULTS] }}
                />
                <SecureRoute
                  path="/PathologyCaseView/:pathologySampleId"
                  exact
                  component={() => <PathologyCaseView />}
                  role=""
                  labUnitRole={{ Pathology: [Roles.RESULTS] }}
                />
                <SecureRoute
                  path="/ImmunohistochemistryDashboard"
                  exact
                  component={() => <ImmunohistochemistryDashboard />}
                  role=""
                  labUnitRole={{ Immunohistochemistry: [Roles.RESULTS] }}
                />
                <SecureRoute
                  path="/ImmunohistochemistryCaseView/:immunohistochemistrySampleId"
                  exact
                  component={() => <ImmunohistochemistryCaseView />}
                  role=""
                  labUnitRole={{ Immunohistochemistry: [Roles.RESULTS] }}
                />
                <SecureRoute
                  path="/CytologyDashboard"
                  exact
                  component={() => <CytologyDashboard />}
                  role=""
                />
                <SecureRoute
                  path="/genericProgram"
                  exact
                  component={() => <ProgramDashboard />}
                  role={Roles.RECEPTION}
                />
                <SecureRoute
                  path="/programView/:programSampleId"
                  exact
                  component={() => <ProgramCaseView />}
                  role={Roles.RECEPTION}
                />
                <SecureRoute
                  path="/NoteBookDashboard"
                  exact
                  component={() => <NoteBookDashBoard />}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.VALIDATION]}
                />
                <SecureRoute
                  path="/NoteBookEntryForm/:notebookid"
                  exact
                  component={() => <NoteBookEntryForm />}
                  role={Roles.GLOBAL_ADMIN}
                />
                <SecureRoute
                  path="/NoteBookEntryForm"
                  exact
                  component={() => <NoteBookEntryForm />}
                  role={Roles.GLOBAL_ADMIN}
                />
                <SecureRoute
                  path="/NoteBookInstanceEntryForm/:notebookid"
                  exact
                  component={() => <NoteBookInstanceEntryForm />}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/NoteBookInstanceEditForm/:notebookentryid"
                  exact
                  component={() => <NoteBookInstanceEntryForm />}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/NotebookSampleOrder/:notebookId/:notebookEntryId"
                  exact
                  component={() => <NotebookSampleOrder />}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/NotebookSampleOrder/:notebookId"
                  exact
                  component={() => <NotebookSampleOrder />}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/CytologyCaseView/:cytologySampleId"
                  exact
                  component={() => <CytologyCaseView />}
                  role=""
                  labUnitRole={{ Cytology: [Roles.RESULTS] }}
                />
                <SecureRoute
                  path="/GenericSample/Order"
                  exact
                  component={() => (
                    <Suspense fallback={null}>
                      <GenericSampleOrder />
                    </Suspense>
                  )}
                  role={Roles.RECEPTION}
                />
                <SecureRoute
                  path="/GenericSample/Edit"
                  exact
                  component={() => (
                    <Suspense fallback={null}>
                      <GenericSampleOrderEdit />
                    </Suspense>
                  )}
                  role={Roles.RECEPTION}
                />
                <SecureRoute
                  path="/GenericSample/Import"
                  exact
                  component={() => (
                    <Suspense fallback={null}>
                      <GenericSampleOrderImport />
                    </Suspense>
                  )}
                  role={Roles.RECEPTION}
                />
                <SecureRoute
                  path="/FreezerMonitoring"
                  exact
                  component={() => (
                    <Suspense fallback={null}>
                      <FreezerMonitoringDashboard />
                    </Suspense>
                  )}
                  role={Roles.RECEPTION}
                />
                <SecureRoute
                  path="/SamplePatientEntry"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorSamplePatientEntry}>
                      <AddOrder />
                    </RouteErrorBoundary>
                  )}
                  role={Roles.RECEPTION}
                />
                {/* Decoupled Sample Collection Workflow - NAV-2 */}
                {/* Use Route with render to wrap all /order/* paths in shared OrderProvider */}
                <Route
                  path="/order"
                  render={({ match }) => (
                    <OrderProvider>
                      <Switch>
                        <SecureRoute
                          path={`${match.path}`}
                          exact
                          component={() => <OrderDashboard />}
                          role={Roles.RECEPTION}
                        />
                        <SecureRoute
                          path={`${match.path}/enter`}
                          exact
                          component={() => <OrderEnter />}
                          role={Roles.RECEPTION}
                        />
                        <SecureRoute
                          path={`${match.path}/collect`}
                          exact
                          component={() => <OrderCollect />}
                          role={Roles.RECEPTION}
                        />
                        <SecureRoute
                          path={`${match.path}/label`}
                          exact
                          component={() => <OrderLabel />}
                          role={Roles.RECEPTION}
                        />
                        <SecureRoute
                          path={`${match.path}/qa`}
                          exact
                          component={() => <OrderQA />}
                          role={Roles.RECEPTION}
                        />
                      </Switch>
                    </OrderProvider>
                  )}
                />
                <SecureRoute
                  path="/ModifyOrder"
                  exact
                  component={() => <ModifyOrder />}
                  role={Roles.RECEPTION}
                />
                <SecureRoute
                  path="/SampleEdit"
                  exact
                  component={() => <FindOrder />}
                  role={Roles.RECEPTION}
                />
                <SecureRoute
                  path="/NceDashboard"
                  exact
                  component={() => <NonConformIndex form="NceDashboard" />}
                  role={[Roles.RECEPTION, Roles.VALIDATION]}
                />
                <SecureRoute
                  path="/ReportNonConformingEvent"
                  exact
                  component={() => (
                    <NonConformIndex form="ReportNonConformingEvent" />
                  )}
                  role={[Roles.RECEPTION, Roles.VALIDATION]}
                />
                <SecureRoute
                  path="/ViewNonConformingEvent"
                  exact
                  component={() => (
                    <NonConformIndex form="ViewNonConformingEvent" />
                  )}
                  role={[Roles.RECEPTION, Roles.VALIDATION]}
                />

                <SecureRoute
                  path="/NCECorrectiveAction"
                  exact
                  component={() => (
                    <NonConformIndex form="NCECorrectiveAction" />
                  )}
                  role={[Roles.RECEPTION, Roles.VALIDATION]}
                />

                <SecureRoute
                  path="/SampleBatchEntrySetup"
                  exact
                  component={() => <SampleBatchEntrySetup />}
                  role={Roles.RECEPTION}
                />

                <SecureRoute
                  path="/ElectronicOrders"
                  exact
                  component={() => <EOrderPage />}
                  role={Roles.RECEPTION}
                />
                <SecureRoute
                  path="/PrintBarcode"
                  exact
                  component={() => <PrintBarcode />}
                  role={Roles.RECEPTION}
                />
                <SecureRoute
                  path="/PatientManagement/:patientId?"
                  exact
                  component={() => <PatientManagement />}
                  role={Roles.RECEPTION}
                />
                <SecureRoute
                  path="/Alerts"
                  exact
                  component={() => <AlertsDashboard />}
                  role={[Roles.RECEPTION, Roles.RESULTS]}
                />
                {/* QA v0.5 IA rehome (OGC-691): EQA pages moved to /qa/eqa/* */}
                <Redirect exact from="/EQAOrders" to="/qa/eqa/orders" />
                <Redirect
                  exact
                  from="/EQAMyPrograms"
                  to="/qa/eqa/my-programs"
                />
                <Redirect exact from="/EQAManagement" to="/qa/eqa/management" />
                <Redirect exact from="/EQAResults" to="/qa/eqa/results" />
                <Redirect
                  exact
                  from="/EQAParticipants"
                  to="/qa/eqa/participants"
                />
                <Redirect
                  exact
                  from="/EQADistribution/create"
                  to="/qa/eqa/distribution/create"
                />
                <Redirect
                  exact
                  from="/EQADistribution"
                  to="/qa/eqa/distribution"
                />
                <SecureRoute
                  path="/qa/eqa/orders"
                  exact
                  component={() => <EQAOrdersPage />}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                  permission="qa.view.eqa"
                />
                {/* qa/019 menu row (T-12) ships the FRS path; page lives in the
                    /qa/eqa/* family with its V1 siblings (T-24 card note). */}
                <Redirect
                  exact
                  from="/eqa/participant/cycles"
                  to="/qa/eqa/my-cycles"
                />
                <SecureRoute
                  path="/qa/eqa/my-cycles"
                  exact
                  component={() => <MyCyclesPage />}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                  permission="qa.view.eqa"
                />
                <SecureRoute
                  path="/qa/eqa/my-programs"
                  exact
                  component={() => <MyProgramsPage />}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                  permission="qa.view.eqa"
                />
                <SecureRoute
                  path="/qa/eqa/management"
                  exact
                  component={() => <EQAProgramManagement />}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                  permission="qa.view.eqa"
                />
                <SecureRoute
                  path="/qa/eqa/results"
                  exact
                  component={() => <EQAResultsPage />}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                  permission="qa.view.eqa"
                />
                <SecureRoute
                  path="/qa/eqa/participants"
                  exact
                  component={() => <EQAParticipantsPage />}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                  permission="qa.view.eqa"
                />
                <SecureRoute
                  path="/qa/eqa/distribution/create"
                  exact
                  component={() => <CreateDistribution />}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                  permission="qa.view.eqa"
                />
                <SecureRoute
                  path="/qa/eqa/distribution"
                  exact
                  component={() => <EQADistributionDashboard />}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                  permission="qa.view.eqa"
                />
                {/* Provider prep + shipping workbenches (T-25). Same page with
                    and without a cycle: without one it lists provider cycles,
                    which is what makes it reachable until T-24 ships the
                    scheme list qa/019's menu row points at. */}
                <SecureRoute
                  path="/qa/eqa/provider/cycles/:cycleId/workbench"
                  exact
                  component={() => <ProviderWorkbenchPage />}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                  permission="qa.view.eqa"
                />
                <SecureRoute
                  path="/qa/eqa/provider/workbench"
                  exact
                  component={() => <ProviderWorkbenchPage />}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                  permission="qa.view.eqa"
                />
                {/* In-house blinding (T-21): landing list, then the 4-step
                    wizard. The wizard's writes carry their own qa.manage.eqa
                    guard server-side, so both routes sit on the read umbrella. */}
                <SecureRoute
                  path="/qa/eqa/in-house/new"
                  exact
                  component={() => <BlindingWizard />}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                  permission="qa.view.eqa"
                />
                <SecureRoute
                  path="/qa/eqa/in-house"
                  exact
                  component={() => <InHousePanelsPage />}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                  permission="qa.view.eqa"
                />
                {/* QA menu (OGC-688): Overview shell + placeholder leaves.
                    No pillar-landing routes: sidenav parents expand-only
                    (never navigate), so landing pages would be unreachable. */}
                <SecureRoute
                  path="/qa/overview"
                  exact
                  component={() => <QAOverview />}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.VALIDATION]}
                />
                <SecureRoute
                  path="/qa/qc/reagent-qc"
                  exact
                  component={() => <QAPlaceholder feature="reagent-qc" />}
                  role={Roles.LAB_SUPERVISOR}
                />
                <SecureRoute
                  path="/qa/qc/manual-qc"
                  exact
                  component={() => <QAPlaceholder feature="manual-qc" />}
                  role={Roles.LAB_SUPERVISOR}
                />
                {/* QA v1 MVP (OGC-695/696): QI Dashboard replaces the pillar
                    placeholder; the pillar menu entry is now expand-only. */}
                <Redirect exact from="/qa/qi" to="/qa/qi/dashboard" />
                <SecureRoute
                  path="/qa/qi/dashboard"
                  exact
                  component={() => <QIDashboard />}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.VALIDATION]}
                />
                <SecureRoute
                  path="/qa/qi/config"
                  exact
                  component={() => <QIConfigList />}
                  permission="qa.manage.qi"
                  role={Roles.GLOBAL_ADMIN}
                />
                <SecureRoute
                  path="/qa/qi/tat"
                  exact
                  component={() => (
                    <QIEnabledRoute indicator="TAT">
                      <TATReport breadcrumbs={qaTatBreadcrumbs} />
                    </QIEnabledRoute>
                  )}
                  role={[Roles.RESULTS, Roles.REPORTS]}
                />
                <SecureRoute
                  path="/qa/qi/rejection"
                  exact
                  component={() => (
                    <QIEnabledRoute indicator="REJECTION">
                      <RejectionReport />
                    </QIEnabledRoute>
                  )}
                  role={[Roles.RESULTS, Roles.REPORTS]}
                />
                <SecureRoute
                  path="/qa/qi/amendment"
                  exact
                  component={() => (
                    <QIEnabledRoute indicator="AMENDMENT">
                      <AmendmentReport />
                    </QIEnabledRoute>
                  )}
                  role={[Roles.RESULTS, Roles.REPORTS]}
                />
                <SecureRoute
                  path="/qa/qi/callback"
                  exact
                  component={() => (
                    <QIEnabledRoute indicator="CALLBACK">
                      <CallbackReport />
                    </QIEnabledRoute>
                  )}
                  role={[Roles.RESULTS, Roles.REPORTS]}
                />
                <SecureRoute
                  path="/qa/qms/nce-register"
                  exact
                  component={() => (
                    <NonConformIndex form="ViewNonConformingEvent" />
                  )}
                  role={[Roles.RECEPTION, Roles.VALIDATION]}
                />
                <SecureRoute
                  path="/qa/qms/audit-trail"
                  exact
                  component={() => <AuditTrailReportIndex />}
                  role={Roles.GLOBAL_ADMIN}
                />
                <SecureRoute
                  path="/qa/qms/e-signature-log"
                  exact
                  component={() => <ESignatureLog />}
                  permission="qa.view.qms"
                  role={Roles.GLOBAL_ADMIN}
                />
                <SecureRoute
                  path="/qa/qms/capa-register"
                  exact
                  component={() => <CapaRegister />}
                  permission="qa.view.qms"
                  role={Roles.GLOBAL_ADMIN}
                />
                <SecureRoute
                  path="/qa/qms/accreditation"
                  exact
                  component={() => <Accreditation />}
                  permission="qa.view.qms"
                  role={Roles.GLOBAL_ADMIN}
                />
                <SecureRoute
                  path="/Storage"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <StorageDashboard />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/sample-items"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <SampleItemsPage />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/sample-items/:id/manage-location"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <ManageLocationPage />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/rooms"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <RoomsPage />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/devices"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <DevicesPage />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/shelves"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <ShelvesPage />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/racks"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <RacksPage />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/boxes"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <BoxesPage />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/rooms/new"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <AddLocationPage type="room" />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/devices/new"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <AddLocationPage type="device" />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/shelves/new"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <AddLocationPage type="shelf" />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/racks/new"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <AddLocationPage type="rack" />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/boxes/new"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <AddBoxPage />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/rooms/:id/edit"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <EditLocationPage type="room" />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/devices/:id/edit"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <EditLocationPage type="device" />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/shelves/:id/edit"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <EditLocationPage type="shelf" />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/racks/:id/edit"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <EditLocationPage type="rack" />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/boxes/:id/edit"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <EditBoxPage />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/inventory"
                  exact
                  component={() => <InventoryManagement />}
                  role={[Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/SampleShipment"
                  exact
                  component={() => <ShipmentDashboard />}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/SampleShipment/create-box"
                  exact
                  component={() => <BoxCreation />}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/SampleShipment/box/:boxId"
                  exact
                  component={BoxDetails}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/SampleShipment/receive"
                  exact
                  component={() => <ReceptionWorkflow />}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/SampleShipment/reports"
                  exact
                  component={() => (
                    <Suspense fallback={null}>
                      <ShipmentReport />
                    </Suspense>
                  )}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/SampleShipment/settings"
                  exact
                  component={() => <ShipmentSettings />}
                  role={[Roles.RECEPTION, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/SampleShipment/:tab"
                  component={() => <ShipmentDashboard />}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/SampleManagement"
                  exact
                  component={() => <SampleManagement />}
                  role={[Roles.RECEPTION, Roles.RESULTS]}
                />
                <SecureRoute
                  path="/analyzers/new"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorAnalyzers}>
                      <Suspense fallback={null}>
                        <AnalyzerFormPage />
                      </Suspense>
                    </RouteErrorBoundary>
                  )}
                  role={Roles.GLOBAL_ADMIN}
                />
                <SecureRoute
                  path="/analyzers/:id/edit"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorAnalyzers}>
                      <Suspense fallback={null}>
                        <AnalyzerFormPage />
                      </Suspense>
                    </RouteErrorBoundary>
                  )}
                  role={Roles.GLOBAL_ADMIN}
                />
                <SecureRoute
                  path="/analyzers/:id/qc-rules"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorAnalyzers}>
                      <Suspense fallback={null}>
                        <QcRulePage />
                      </Suspense>
                    </RouteErrorBoundary>
                  )}
                  role={Roles.GLOBAL_ADMIN}
                />
                <SecureRoute
                  path="/analyzers"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorAnalyzers}>
                      <Suspense fallback={null}>
                        <AnalyzersPage />
                      </Suspense>
                    </RouteErrorBoundary>
                  )}
                  role={Roles.ANALYSER_IMPORT}
                />
                <SecureRoute
                  path="/analyzers/:id/mappings"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorAnalyzers}>
                      <Suspense fallback={null}>
                        <FieldMapping />
                      </Suspense>
                    </RouteErrorBoundary>
                  )}
                  role={Roles.ANALYSER_IMPORT}
                />
                <SecureRoute
                  path="/analyzers/errors"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorAnalyzers}>
                      <Suspense fallback={null}>
                        <ErrorDashboardPage />
                      </Suspense>
                    </RouteErrorBoundary>
                  )}
                  role={Roles.ANALYSER_IMPORT}
                />
                <SecureRoute
                  path="/analyzers/custom-field-types"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorAnalyzers}>
                      <Suspense fallback={null}>
                        <CustomFieldTypeManagementPage />
                      </Suspense>
                    </RouteErrorBoundary>
                  )}
                  role={Roles.ANALYSER_IMPORT}
                />
                <SecureRoute
                  path="/analyzers/types"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorAnalyzers}>
                      <Suspense fallback={null}>
                        <AnalyzerTypesPage />
                      </Suspense>
                    </RouteErrorBoundary>
                  )}
                  role={Roles.ANALYSER_IMPORT}
                />
                <SecureRoute
                  path="/analyzers/qc/instruments/:instrumentId"
                  exact
                  component={() => <InstrumentDetailPage />}
                  role={Roles.LAB_SUPERVISOR}
                />
                {/* QA v0.5 IA rehome (OGC-689): QC pages moved to /qa/qc/* */}
                <Redirect exact from="/analyzers/qc/db" to="/qa/qc/dashboard" />
                <SecureRoute
                  path="/qa/qc/dashboard"
                  exact
                  component={() => <QCDashboard />}
                  role={Roles.LAB_SUPERVISOR}
                />
                <SecureRoute
                  path="/qa/qc/alerts"
                  exact
                  component={() => <QCDashboard initialTab={1} />}
                  role={Roles.LAB_SUPERVISOR}
                />
                <SecureRoute
                  path="/analyzers/qc/charts/:analyzerId"
                  exact
                  component={() => <ControlChartDetail />}
                  role={Roles.LAB_SUPERVISOR}
                />
                <Redirect
                  exact
                  from="/analyzers/qc/control-lots"
                  to="/qa/qc/control-lots"
                />
                <SecureRoute
                  path="/qa/qc/control-lots"
                  exact
                  component={() => <ControlLotList />}
                  role={Roles.LAB_SUPERVISOR}
                />
                <SecureRoute
                  path="/analyzers/qc/control-lots/new"
                  exact
                  component={() => <ControlLotSetup />}
                  role={Roles.LAB_SUPERVISOR}
                />
                <SecureRoute
                  path="/analyzers/qc/control-lots/:id"
                  exact
                  component={() => <ControlLotSetup />}
                  role={Roles.LAB_SUPERVISOR}
                />
                <Redirect
                  exact
                  from="/analyzers/qc/rule-config"
                  to="/qa/qc/rule-config"
                />
                <SecureRoute
                  path="/qa/qc/rule-config"
                  exact
                  component={() => <RuleConfigPanel />}
                  role={Roles.LAB_SUPERVISOR}
                />
                <SecureRoute
                  path="/PatientHistory"
                  exact
                  component={() => <PatientHistory />}
                  role={Roles.RECEPTION}
                />
                <SecureRoute
                  path="/PatientMerge"
                  exact
                  component={() => <PatientMerge />}
                  role={Roles.RECEPTION}
                />
                <SecureRoute
                  path="/GenericSample/Results"
                  exact
                  component={() => (
                    <Suspense fallback={null}>
                      <GenericSampleResults />
                    </Suspense>
                  )}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/Aliquot"
                  exact
                  component={() => <Aliquot />}
                  role={Roles.RECEPTION}
                />

                <SecureRoute
                  path="/PatientResults/:patientId"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorPatientResultsViewer}>
                      <Suspense fallback={null}>
                        <RoutedResultsViewer />
                      </Suspense>
                    </RouteErrorBoundary>
                  )}
                  role={Roles.RECEPTION}
                />

                <SecureRoute
                  path="/WorkPlanByTestSection"
                  exact
                  component={() => <Workplan type="unit" />}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/WorkplanByTest"
                  exact
                  component={() => <Workplan type="test" />}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/WorkplanByPanel"
                  exact
                  component={() => <Workplan type="panel" />}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/WorkplanByPriority"
                  exact
                  component={() => <Workplan type="priority" />}
                  role={Roles.RESULTS}
                />
                {/* OGC-1020 (R1): canonical unified worklist, gated by the
                    results.entry.unifiedRoute site flag */}
                <SecureRoute
                  path="/Results"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorResultsSearch}>
                      <UnifiedResultsRoute />
                    </RouteErrorBoundary>
                  )}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/result"
                  exact
                  component={() => (
                    <LegacyResultsGate>
                      <RouteErrorBoundary {...routeErrorResultsSearch}>
                        <ResultSearch />
                      </RouteErrorBoundary>
                    </LegacyResultsGate>
                  )}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/LogbookResults"
                  exact
                  component={() => (
                    <LegacyResultsGate>
                      <RouteErrorBoundary {...routeErrorResultsSearch}>
                        <ResultSearch />
                      </RouteErrorBoundary>
                    </LegacyResultsGate>
                  )}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/PatientResults"
                  exact
                  component={() => (
                    <LegacyResultsGate>
                      <RouteErrorBoundary {...routeErrorResultsSearch}>
                        <ResultSearch />
                      </RouteErrorBoundary>
                    </LegacyResultsGate>
                  )}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/AccessionResults"
                  exact
                  component={() => (
                    <LegacyResultsGate>
                      <RouteErrorBoundary {...routeErrorResultsSearch}>
                        <ResultSearch />
                      </RouteErrorBoundary>
                    </LegacyResultsGate>
                  )}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/StatusResults"
                  exact
                  component={() => (
                    <LegacyResultsGate>
                      <RouteErrorBoundary {...routeErrorResultsSearch}>
                        <ResultSearch />
                      </RouteErrorBoundary>
                    </LegacyResultsGate>
                  )}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/RangeResults"
                  exact
                  component={() => (
                    <LegacyResultsGate>
                      <RouteErrorBoundary {...routeErrorResultsSearch}>
                        <ResultSearch />
                      </RouteErrorBoundary>
                    </LegacyResultsGate>
                  )}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/ReferredOutTests"
                  exact
                  component={() => <ReferredOutTests />}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/RoutineReports"
                  exact
                  component={() => <RoutineReports />}
                  role={Roles.REPORTS}
                />
                <SecureRoute
                  path="/RoutineReport"
                  exact
                  component={() => <RoutineIndex />}
                  role={Roles.REPORTS}
                />
                <SecureRoute
                  path="/StudyReports"
                  exact
                  component={() => <StudyReports />}
                  role={Roles.REPORTS}
                />
                <SecureRoute
                  path="/StudyReport"
                  exact
                  component={() => <StudyIndex />}
                  role={Roles.REPORTS}
                />
                <SecureRoute
                  path="/Report"
                  exact
                  component={() => <ReportIndex />}
                  role={Roles.REPORTS}
                />
                {/* QA v0.5 IA rehome (OGC-690): Audit Trail moved to QMS pillar */}
                <Route
                  path="/AuditTrailReport"
                  exact
                  render={({ location }) => (
                    <Redirect
                      to={{
                        pathname: "/qa/qms/audit-trail",
                        search: location.search,
                      }}
                    />
                  )}
                />
                <SecureRoute
                  path="/TATReport"
                  exact
                  component={() => <TATReport />}
                  role={Roles.REPORTS}
                />
                <SecureRoute
                  path="/validation"
                  exact
                  component={() => <StudyValidation />}
                  role={Roles.VALIDATION}
                />
                <SecureRoute
                  path="/ResultValidation"
                  exact
                  component={() => <StudyValidation />}
                  role={Roles.VALIDATION}
                />
                <SecureRoute
                  path="/AccessionValidation"
                  exact
                  component={() => <StudyValidation />}
                  role={Roles.VALIDATION}
                />
                <SecureRoute
                  path="/AccessionValidationRange"
                  exact
                  component={() => <StudyValidation />}
                  role={Roles.VALIDATION}
                />
                <SecureRoute
                  path="/ResultValidationByTestDate"
                  exact
                  component={() => <StudyValidation />}
                  role={Roles.VALIDATION}
                />
                <SecureRoute
                  path="/AnalyzerResults"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorAnalyzerResults}>
                      <Suspense fallback={null}>
                        <AnalyserResultIndex />
                      </Suspense>
                    </RouteErrorBoundary>
                  )}
                  role={Roles.ANALYSER_IMPORT}
                />
                <Route path="*" component={() => <RedirectOldUI />} />
              </Switch>
            </Layout>
          </Router>
        </>
      </UserSessionDetailsContext.Provider>
    </IntlProvider>
  );
}
