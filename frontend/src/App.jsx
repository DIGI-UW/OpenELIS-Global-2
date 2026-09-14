import React, { Suspense, useEffect, useState } from "react";
import { confirmAlert } from "react-confirm-alert";
import { IntlProvider } from "react-intl";
import {
  Route,
  Redirect,
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
import MyProgramsPage from "./components/eqa/MyProgramsPage";
import EQAParticipantsPage from "./components/eqa/EQAParticipantsPage";
import EQAResultsPage from "./components/eqa/EQAResultsPage";
import InventoryManagement from "./components/inventory/InventoryManagement";
import ShipmentDashboard from "./components/shipment/ShipmentDashboard";
import BoxCreation from "./components/shipment/BoxCreation";
import BoxDetails from "./components/shipment/BoxDetails";
import ReceptionWorkflow from "./components/shipment/ReceptionWorkflow";
import ReferenceLabResults from "./components/referenceLabResults";
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
 * develop-CI flake on route chunk fetch; the retry wrapper
 * gives the browser three chances with backoff before giving up.
 *
 * Backoff is intentionally short (0.5s/1s/1.5s): the real failures
 * are transient TCP / Docker-network conditions that resolve in
 * milliseconds. Longer waits would harm real error reporting when the
 * chunk is genuinely missing (e.g., deploy mismatch).
 */
function lazyWithRetry(factory, retries = 3, backoffMs = 500) {
  // This helper is the one legitimate wrapper around React.lazy.
  // eslint-disable-next-line local/no-raw-react-lazy
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
const AnalyzerTypesPage = lazyWithRetry(
  () => import("./pages/AnalyzerTypesPage"),
);
const AnalyzerTypeMappingPage = lazyWithRetry(
  () => import("./pages/AnalyzerTypeMappingPage"),
);
const MicrobiologyPage = lazyWithRetry(
  () => import("./pages/MicrobiologyPage"),
);
const MicrobiologyWorklistPage = lazyWithRetry(
  () => import("./pages/MicrobiologyWorklistPage"),
);
const MicrobiologyWhonetPage = lazyWithRetry(
  () => import("./pages/MicrobiologyWhonetPage"),
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
import { resolveMessagesForLocale } from "./languages";
import {
  getMicrobiologyCaseUrl,
  getMicrobiologyWorklistUrl,
  MICROBIOLOGY_CASE_PATH,
  MICROBIOLOGY_WORKLIST_PATH,
  parseMicrobiologyCaseSearch,
  parseMicrobiologyWorklistSearch,
} from "./components/microbiology/MicrobiologyRoutes";
import { MICROBIOLOGY_WHONET_PATH } from "./components/microbiology/WhonetRoutes";
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
import VectorSurveillanceReport from "./components/reports/vectorSurveillance/Index";
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
import EnvironmentalDashboard from "./components/compliance/EnvironmentalDashboard";
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
import LaporanHasilReport from "./components/reports/compliance/LaporanHasilReport";
import ManualEntryHelper from "./components/reports/vectorSurveillance/ManualEntryHelper";
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
  ClinicalOrderEnter,
  EnvironmentalOrderEnter,
  VectorOrderEnter,
  OrderCollect,
  OrderLabel,
  OrderQA,
  VectorOrderComplete,
} from "./components/order";
import {
  VectorIdentificationWorklist,
  VectorDeconvolutionWorklist,
} from "./components/vectorIdentification";

export const ANALYZER_RESULTS_ROLES = [
  Roles.GLOBAL_ADMIN,
  Roles.ANALYSER_IMPORT,
];

export default function App() {
  // The stored preference, or the browser's full tag (region kept: fr-MG
  // resolves to its own bundle, not just fr). The resolver accepts either
  // spelling (fr_MG / fr-MG) and always returns usable messages, so a stale
  // stored value can never break startup.
  const initial = resolveMessagesForLocale(
    localStorage.getItem("locale") || navigator.language,
  );

  const [locale, setLocale] = useState(initial.code);
  const [messages, setMessages] = useState(initial.messages);

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
    // The selector hands over whatever code the locales config declared —
    // underscore or hyphen, any casing. Resolve it to the canonical code and
    // the best bundle (exact, then base language, then English) so a
    // configured locale like fr_MG lands on its own translations.
    const resolved = resolveMessagesForLocale(lang);
    setLocale(resolved.code);
    setMessages(resolved.messages);
    localStorage.setItem("locale", resolved.code);
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
                <Route path="/login" exact render={() => <Login />} />
                <Route
                  path="/ChangePasswordLogin"
                  exact
                  render={() => <ChangePassword />}
                />
                <Route path="/landing" exact render={() => <LandingPage />} />
                <SecureRoute path="/" exact render={() => <Home />} role="" />
                <SecureRoute
                  path="/Dashboard"
                  exact
                  render={() => <Home />}
                  role=""
                />
                <SecureRoute
                  path="/admin"
                  render={() => <Admin />}
                  role={Roles.GLOBAL_ADMIN}
                />
                <SecureRoute
                  path="/MasterListsPage"
                  render={() => <Admin />}
                  role={Roles.GLOBAL_ADMIN}
                />
                <SecureRoute
                  path="/PathologyDashboard"
                  exact
                  render={() => <PathologyDashboard />}
                  role=""
                  labUnitRole={{ Pathology: [Roles.RESULTS] }}
                />
                <SecureRoute
                  path="/PathologyCaseView/:pathologySampleId"
                  exact
                  render={() => <PathologyCaseView />}
                  role=""
                  labUnitRole={{ Pathology: [Roles.RESULTS] }}
                />
                <SecureRoute
                  path="/ImmunohistochemistryDashboard"
                  exact
                  render={() => <ImmunohistochemistryDashboard />}
                  role=""
                  labUnitRole={{ Immunohistochemistry: [Roles.RESULTS] }}
                />
                <SecureRoute
                  path="/ImmunohistochemistryCaseView/:immunohistochemistrySampleId"
                  exact
                  render={() => <ImmunohistochemistryCaseView />}
                  role=""
                  labUnitRole={{ Immunohistochemistry: [Roles.RESULTS] }}
                />
                <SecureRoute
                  path="/CytologyDashboard"
                  exact
                  render={() => <CytologyDashboard />}
                  role=""
                />
                <SecureRoute
                  path="/genericProgram"
                  exact
                  render={() => <ProgramDashboard />}
                  role={Roles.RECEPTION}
                />
                <SecureRoute
                  path="/programView/:programSampleId"
                  exact
                  render={() => <ProgramCaseView />}
                  role={Roles.RECEPTION}
                />
                <SecureRoute
                  path="/NoteBookDashboard"
                  exact
                  render={() => <NoteBookDashBoard />}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.VALIDATION]}
                />
                <SecureRoute
                  path="/EnvironmentalDashboard"
                  exact
                  render={() => <EnvironmentalDashboard />}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/NoteBookEntryForm/:notebookid"
                  exact
                  render={() => <NoteBookEntryForm />}
                  role={Roles.GLOBAL_ADMIN}
                />
                <SecureRoute
                  path="/NoteBookEntryForm"
                  exact
                  render={() => <NoteBookEntryForm />}
                  role={Roles.GLOBAL_ADMIN}
                />
                <SecureRoute
                  path="/NoteBookInstanceEntryForm/:notebookid"
                  exact
                  render={() => <NoteBookInstanceEntryForm />}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/NoteBookInstanceEditForm/:notebookentryid"
                  exact
                  render={() => <NoteBookInstanceEntryForm />}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/NotebookSampleOrder/:notebookId/:notebookEntryId"
                  exact
                  render={() => <NotebookSampleOrder />}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/NotebookSampleOrder/:notebookId"
                  exact
                  render={() => <NotebookSampleOrder />}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/CytologyCaseView/:cytologySampleId"
                  exact
                  render={() => <CytologyCaseView />}
                  role=""
                  labUnitRole={{ Cytology: [Roles.RESULTS] }}
                />
                <SecureRoute
                  path={`${MICROBIOLOGY_CASE_PATH}/:caseId`}
                  exact
                  component={() => (
                    <Suspense fallback={null}>
                      <MicrobiologyPage />
                    </Suspense>
                  )}
                  role={[Roles.GLOBAL_ADMIN, Roles.RESULTS, Roles.VALIDATION]}
                />
                <SecureRoute
                  path={MICROBIOLOGY_WORKLIST_PATH}
                  exact
                  component={() => (
                    <Suspense fallback={null}>
                      <MicrobiologyWorklistPage />
                    </Suspense>
                  )}
                  role={[Roles.GLOBAL_ADMIN, Roles.RESULTS, Roles.VALIDATION]}
                />
                <SecureRoute
                  path={MICROBIOLOGY_WHONET_PATH}
                  exact
                  component={() => (
                    <Suspense fallback={null}>
                      <MicrobiologyWhonetPage />
                    </Suspense>
                  )}
                  role={[Roles.GLOBAL_ADMIN, Roles.RESULTS, Roles.REPORTS]}
                />
                <Route
                  path="/MicrobiologyCaseView/:caseId"
                  exact
                  render={({ location, match }) => (
                    <Redirect
                      to={getMicrobiologyCaseUrl(
                        match.params.caseId,
                        parseMicrobiologyCaseSearch(location.search),
                      )}
                    />
                  )}
                />
                <Route
                  path="/MicrobiologyWorklist"
                  exact
                  render={({ location }) => (
                    <Redirect
                      to={getMicrobiologyWorklistUrl(
                        parseMicrobiologyWorklistSearch(location.search),
                      )}
                    />
                  )}
                />
                <SecureRoute
                  path="/GenericSample/Order"
                  exact
                  render={() => (
                    <Suspense fallback={null}>
                      <GenericSampleOrder />
                    </Suspense>
                  )}
                  role={Roles.RECEPTION}
                />
                <SecureRoute
                  path="/GenericSample/Edit"
                  exact
                  render={() => (
                    <Suspense fallback={null}>
                      <GenericSampleOrderEdit />
                    </Suspense>
                  )}
                  role={Roles.RECEPTION}
                />
                <SecureRoute
                  path="/GenericSample/Import"
                  exact
                  render={() => (
                    <Suspense fallback={null}>
                      <GenericSampleOrderImport />
                    </Suspense>
                  )}
                  role={Roles.RECEPTION}
                />
                <SecureRoute
                  path="/FreezerMonitoring"
                  exact
                  render={() => (
                    <Suspense fallback={null}>
                      <FreezerMonitoringDashboard />
                    </Suspense>
                  )}
                  role={[Roles.RECEPTION, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/SamplePatientEntry"
                  exact
                  render={() => (
                    <RouteErrorBoundary {...routeErrorSamplePatientEntry}>
                      <AddOrder />
                    </RouteErrorBoundary>
                  )}
                  role={Roles.RECEPTION}
                />
                {/* Clinical Order Workflow */}
                <Route
                  path="/order/clinical"
                  render={({ match }) => (
                    <OrderProvider workflowType="clinical">
                      <Switch>
                        <SecureRoute
                          path={`${match.path}`}
                          exact
                          render={() => <OrderDashboard />}
                          role={Roles.RECEPTION}
                        />
                        <SecureRoute
                          path={`${match.path}/enter`}
                          exact
                          render={() => <ClinicalOrderEnter />}
                          role={Roles.RECEPTION}
                        />
                        <SecureRoute
                          path={`${match.path}/collect`}
                          exact
                          render={() => <OrderCollect />}
                          role={Roles.RECEPTION}
                        />
                        <SecureRoute
                          path={`${match.path}/label`}
                          exact
                          render={() => <OrderLabel />}
                          role={Roles.RECEPTION}
                        />
                        <SecureRoute
                          path={`${match.path}/qa`}
                          exact
                          render={() => <OrderQA />}
                          role={Roles.RECEPTION}
                        />
                      </Switch>
                    </OrderProvider>
                  )}
                />
                {/* Environmental Order Workflow */}
                <Route
                  path="/order/environmental"
                  render={({ match }) => (
                    <OrderProvider workflowType="environmental">
                      <Switch>
                        <SecureRoute
                          path={`${match.path}`}
                          exact
                          render={() => <OrderDashboard />}
                          role={Roles.RECEPTION}
                        />
                        <SecureRoute
                          path={`${match.path}/enter`}
                          exact
                          render={() => <EnvironmentalOrderEnter />}
                          role={Roles.RECEPTION}
                        />
                        <SecureRoute
                          path={`${match.path}/label`}
                          exact
                          render={() => <OrderLabel />}
                          role={Roles.RECEPTION}
                        />
                        <SecureRoute
                          path={`${match.path}/qa`}
                          exact
                          render={() => <OrderQA />}
                          role={Roles.RECEPTION}
                        />
                      </Switch>
                    </OrderProvider>
                  )}
                />
                {/* Vector Surveillance Order Workflow (no Collect step) */}
                <Route
                  path="/order/vector"
                  render={({ match }) => (
                    <OrderProvider workflowType="vector">
                      <Switch>
                        <SecureRoute
                          path={`${match.path}`}
                          exact
                          render={() => <OrderDashboard />}
                          role={Roles.RECEPTION}
                        />
                        <SecureRoute
                          path={`${match.path}/enter`}
                          exact
                          render={() => <VectorOrderEnter />}
                          role={Roles.RECEPTION}
                        />
                        <SecureRoute
                          path={`${match.path}/label`}
                          exact
                          render={() => <OrderLabel />}
                          role={Roles.RECEPTION}
                        />
                        <SecureRoute
                          path={`${match.path}/qa`}
                          exact
                          render={() => <OrderQA />}
                          role={Roles.RECEPTION}
                        />
                        <SecureRoute
                          path={`${match.path}/complete`}
                          exact
                          render={() => <VectorOrderComplete />}
                          role={Roles.RECEPTION}
                        />
                      </Switch>
                    </OrderProvider>
                  )}
                />
                {/* Redirect legacy /order and /order/enter to clinical workflow */}
                <Route
                  path="/order/enter"
                  exact
                  render={() => <Redirect to="/order/clinical/enter" />}
                />
                <Route
                  path="/order"
                  exact
                  render={() => <Redirect to="/order/clinical" />}
                />
                <SecureRoute
                  path="/vector/identification"
                  exact
                  render={() => <VectorIdentificationWorklist />}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/vector/deconvolution"
                  exact
                  render={() => <VectorDeconvolutionWorklist />}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/ModifyOrder"
                  exact
                  render={() => <ModifyOrder />}
                  role={Roles.RECEPTION}
                />
                <SecureRoute
                  path="/SampleEdit"
                  exact
                  render={() => <FindOrder />}
                  role={Roles.RECEPTION}
                />
                <SecureRoute
                  path="/NceDashboard"
                  exact
                  render={() => <NonConformIndex form="NceDashboard" />}
                  role={[Roles.RECEPTION, Roles.VALIDATION]}
                />
                <SecureRoute
                  path="/ReportNonConformingEvent"
                  exact
                  render={() => (
                    <NonConformIndex form="ReportNonConformingEvent" />
                  )}
                  role={[Roles.RECEPTION, Roles.VALIDATION]}
                />
                <SecureRoute
                  path="/ViewNonConformingEvent"
                  exact
                  render={() => (
                    <NonConformIndex form="ViewNonConformingEvent" />
                  )}
                  role={[Roles.RECEPTION, Roles.VALIDATION]}
                />

                <SecureRoute
                  path="/NCECorrectiveAction"
                  exact
                  render={() => <NonConformIndex form="NCECorrectiveAction" />}
                  role={[Roles.RECEPTION, Roles.VALIDATION]}
                />

                <SecureRoute
                  path="/SampleBatchEntrySetup"
                  exact
                  render={() => <SampleBatchEntrySetup />}
                  role={Roles.RECEPTION}
                />

                <SecureRoute
                  path="/ElectronicOrders"
                  exact
                  render={() => <EOrderPage />}
                  role={Roles.RECEPTION}
                />
                <SecureRoute
                  path="/PrintBarcode"
                  exact
                  render={() => <PrintBarcode />}
                  role={Roles.RECEPTION}
                />
                <SecureRoute
                  path="/PatientManagement/:patientId?"
                  exact
                  render={() => <PatientManagement />}
                  role={Roles.RECEPTION}
                />
                <SecureRoute
                  path="/Alerts"
                  exact
                  render={() => <AlertsDashboard />}
                  role={[Roles.RECEPTION, Roles.RESULTS]}
                />
                <SecureRoute
                  path="/EQAOrders"
                  exact
                  render={() => <EQAOrdersPage />}
                  role={[Roles.RECEPTION, Roles.RESULTS]}
                />
                <SecureRoute
                  path="/EQAMyPrograms"
                  exact
                  render={() => <MyProgramsPage />}
                  role={[Roles.RECEPTION, Roles.RESULTS]}
                />
                <SecureRoute
                  path="/EQAManagement"
                  exact
                  render={() => <EQAProgramManagement />}
                  role={[Roles.RECEPTION, Roles.RESULTS]}
                />
                <SecureRoute
                  path="/EQAResults"
                  exact
                  render={() => <EQAResultsPage />}
                  role={[Roles.RECEPTION, Roles.RESULTS]}
                />
                <SecureRoute
                  path="/EQAParticipants"
                  exact
                  render={() => <EQAParticipantsPage />}
                  role={[Roles.RECEPTION, Roles.RESULTS]}
                />
                <SecureRoute
                  path="/EQADistribution/create"
                  exact
                  render={() => <CreateDistribution />}
                  role={[Roles.RECEPTION, Roles.RESULTS]}
                />
                <SecureRoute
                  path="/EQADistribution"
                  exact
                  render={() => <EQADistributionDashboard />}
                  role={[Roles.RECEPTION, Roles.RESULTS]}
                />
                <SecureRoute
                  path="/Storage"
                  exact
                  render={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <StorageDashboard />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/sample-items"
                  exact
                  render={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <SampleItemsPage />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/sample-items/:id/manage-location"
                  exact
                  render={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <ManageLocationPage />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/rooms"
                  exact
                  render={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <RoomsPage />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/devices"
                  exact
                  render={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <DevicesPage />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/shelves"
                  exact
                  render={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <ShelvesPage />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/racks"
                  exact
                  render={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <RacksPage />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/boxes"
                  exact
                  render={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <BoxesPage />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/rooms/new"
                  exact
                  render={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <AddLocationPage type="room" />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/devices/new"
                  exact
                  render={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <AddLocationPage type="device" />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/shelves/new"
                  exact
                  render={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <AddLocationPage type="shelf" />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/racks/new"
                  exact
                  render={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <AddLocationPage type="rack" />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/boxes/new"
                  exact
                  render={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <AddBoxPage />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/rooms/:id/edit"
                  exact
                  render={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <EditLocationPage type="room" />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/devices/:id/edit"
                  exact
                  render={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <EditLocationPage type="device" />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/shelves/:id/edit"
                  exact
                  render={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <EditLocationPage type="shelf" />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/racks/:id/edit"
                  exact
                  render={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <EditLocationPage type="rack" />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/boxes/:id/edit"
                  exact
                  render={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <EditBoxPage />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/inventory"
                  exact
                  render={() => <InventoryManagement />}
                  role={[Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/SampleShipment"
                  exact
                  render={() => <ShipmentDashboard />}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/SampleShipment/create-box"
                  exact
                  render={() => <BoxCreation />}
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
                  render={() => <ReceptionWorkflow />}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/SampleShipment/reports"
                  exact
                  render={() => (
                    <Suspense fallback={null}>
                      <ShipmentReport />
                    </Suspense>
                  )}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/SampleShipment/settings"
                  exact
                  render={() => <ShipmentSettings />}
                  role={[Roles.RECEPTION, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/SampleShipment/reference-lab-results"
                  exact
                  render={() => <ReferenceLabResults />}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/SampleShipment/:tab"
                  render={() => <ShipmentDashboard />}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/SampleManagement"
                  exact
                  render={() => <SampleManagement />}
                  role={[Roles.RECEPTION, Roles.RESULTS]}
                />
                <SecureRoute
                  path="/analyzers/new"
                  exact
                  render={() => (
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
                  render={() => (
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
                  render={() => (
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
                  render={() => (
                    <RouteErrorBoundary {...routeErrorAnalyzers}>
                      <Suspense fallback={null}>
                        <AnalyzersPage />
                      </Suspense>
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.ANALYSER_IMPORT, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/analyzers/:id/mappings"
                  exact
                  render={() => (
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
                  render={() => (
                    <RouteErrorBoundary {...routeErrorAnalyzers}>
                      <Suspense fallback={null}>
                        <ErrorDashboardPage />
                      </Suspense>
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.ANALYSER_IMPORT, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/analyzers/custom-field-types"
                  exact
                  render={() => (
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
                  render={() => (
                    <RouteErrorBoundary {...routeErrorAnalyzers}>
                      <Suspense fallback={null}>
                        <AnalyzerTypesPage />
                      </Suspense>
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.ANALYSER_IMPORT, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/analyzers/types/:profileId/mapping"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorAnalyzers}>
                      <Suspense fallback={null}>
                        <AnalyzerTypeMappingPage />
                      </Suspense>
                    </RouteErrorBoundary>
                  )}
                  role={Roles.ANALYSER_IMPORT}
                />
                <SecureRoute
                  path="/analyzers/qc/instruments/:instrumentId"
                  exact
                  render={() => <InstrumentDetailPage />}
                  role={Roles.LAB_SUPERVISOR}
                />
                <SecureRoute
                  path="/analyzers/qc/db"
                  exact
                  render={() => <QCDashboard />}
                  role={Roles.LAB_SUPERVISOR}
                />
                <SecureRoute
                  path="/analyzers/qc/charts/:analyzerId"
                  exact
                  render={() => <ControlChartDetail />}
                  role={Roles.LAB_SUPERVISOR}
                />
                <SecureRoute
                  path="/analyzers/qc/control-lots"
                  exact
                  render={() => <ControlLotList />}
                  role={Roles.LAB_SUPERVISOR}
                />
                <SecureRoute
                  path="/analyzers/qc/control-lots/new"
                  exact
                  render={() => <ControlLotSetup />}
                  role={Roles.LAB_SUPERVISOR}
                />
                <SecureRoute
                  path="/analyzers/qc/control-lots/:id"
                  exact
                  render={() => <ControlLotSetup />}
                  role={Roles.LAB_SUPERVISOR}
                />
                <SecureRoute
                  path="/analyzers/qc/rule-config"
                  exact
                  render={() => <RuleConfigPanel />}
                  role={Roles.LAB_SUPERVISOR}
                />
                <SecureRoute
                  path="/PatientHistory"
                  exact
                  render={() => <PatientHistory />}
                  role={Roles.RECEPTION}
                />
                <SecureRoute
                  path="/PatientMerge"
                  exact
                  render={() => <PatientMerge />}
                  role={Roles.RECEPTION}
                />
                <SecureRoute
                  path="/GenericSample/Results"
                  exact
                  render={() => (
                    <Suspense fallback={null}>
                      <GenericSampleResults />
                    </Suspense>
                  )}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/Aliquot"
                  exact
                  render={() => <Aliquot />}
                  role={Roles.RECEPTION}
                />

                <SecureRoute
                  path="/PatientResults/:patientId"
                  exact
                  render={() => (
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
                  render={() => <Workplan type="unit" />}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/WorkplanByTest"
                  exact
                  render={() => <Workplan type="test" />}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/WorkplanByPanel"
                  exact
                  render={() => <Workplan type="panel" />}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/WorkplanByPriority"
                  exact
                  render={() => <Workplan type="priority" />}
                  role={Roles.RESULTS}
                />
                {/* OGC-1020 (R1): canonical unified worklist, gated by the
                    results.entry.unifiedRoute site flag */}
                <SecureRoute
                  path="/Results"
                  exact
                  render={() => (
                    <RouteErrorBoundary {...routeErrorResultsSearch}>
                      <UnifiedResultsRoute />
                    </RouteErrorBoundary>
                  )}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/result"
                  exact
                  render={() => (
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
                  render={() => (
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
                  render={() => (
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
                  render={() => (
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
                  render={() => (
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
                  render={() => (
                    <LegacyResultsGate>
                      <RouteErrorBoundary {...routeErrorResultsSearch}>
                        <ResultSearch />
                      </RouteErrorBoundary>
                    </LegacyResultsGate>
                  )}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/RoutineReports"
                  exact
                  render={() => <RoutineReports />}
                  role={Roles.REPORTS}
                />
                <SecureRoute
                  path="/RoutineReport"
                  exact
                  render={() => <RoutineIndex />}
                  role={Roles.REPORTS}
                />
                <SecureRoute
                  path="/StudyReports"
                  exact
                  render={() => <StudyReports />}
                  role={Roles.REPORTS}
                />
                <SecureRoute
                  path="/StudyReport"
                  exact
                  render={() => <StudyIndex />}
                  role={Roles.REPORTS}
                />
                <SecureRoute
                  path="/Report"
                  exact
                  render={() => <ReportIndex />}
                  role={Roles.REPORTS}
                />
                <SecureRoute
                  path="/AuditTrailReport"
                  exact
                  render={() => <AuditTrailReportIndex />}
                  role={Roles.GLOBAL_ADMIN}
                />
                <SecureRoute
                  path="/TATReport"
                  exact
                  render={() => <TATReport />}
                  role={Roles.REPORTS}
                />
                <SecureRoute
                  path="/VectorSurveillanceReport"
                  exact
                  render={() => <VectorSurveillanceReport />}
                  role={Roles.REPORTS}
                />
                <SecureRoute
                  path="/LaporanHasil"
                  exact
                  render={() => <LaporanHasilReport />}
                  role={Roles.REPORTS}
                />
                <SecureRoute
                  path="/VectorManualEntry"
                  exact
                  render={() => <ManualEntryHelper />}
                  role={Roles.REPORTS}
                />
                <SecureRoute
                  path="/validation"
                  exact
                  render={() => <StudyValidation />}
                  role={Roles.VALIDATION}
                />
                <SecureRoute
                  path="/ResultValidation"
                  exact
                  render={() => <StudyValidation />}
                  role={Roles.VALIDATION}
                />
                <SecureRoute
                  path="/AccessionValidation"
                  exact
                  render={() => <StudyValidation />}
                  role={Roles.VALIDATION}
                />
                <SecureRoute
                  path="/AccessionValidationRange"
                  exact
                  render={() => <StudyValidation />}
                  role={Roles.VALIDATION}
                />
                <SecureRoute
                  path="/ResultValidationByTestDate"
                  exact
                  render={() => <StudyValidation />}
                  role={Roles.VALIDATION}
                />
                <SecureRoute
                  path="/AnalyzerResults"
                  exact
                  render={() => (
                    <RouteErrorBoundary {...routeErrorAnalyzerResults}>
                      <Suspense fallback={null}>
                        <AnalyserResultIndex />
                      </Suspense>
                    </RouteErrorBoundary>
                  )}
                  role={ANALYZER_RESULTS_ROLES}
                />
                <Route path="*" render={() => <RedirectOldUI />} />
              </Switch>
            </Layout>
          </Router>
        </>
      </UserSessionDetailsContext.Provider>
    </IntlProvider>
  );
}
