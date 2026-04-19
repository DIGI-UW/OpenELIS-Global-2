import React, { Suspense, useEffect, useState } from "react";
import { confirmAlert } from "react-confirm-alert";
import { IntlProvider } from "react-intl";
import { Route, BrowserRouter as Router, Switch } from "react-router-dom";
import "./App.css";
import RedirectOldUI from "./RedirectOldUI";
import UserSessionDetailsContext from "./UserSessionDetailsContext";
import { Admin } from "./components";
import ChangePassword from "./components/ChangePassword.js";
import Home from "./components/Home";
import Layout from "./components/layout/Layout";
import StorageDashboard from "./components/storage/StorageDashboard";
import AlertsDashboard from "./components/alerts/AlertsDashboard";
import EQAManagementDashboard from "./components/eqa/EQAManagementDashboard";
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
import Login from "./components/Login";
import LandingPage from "./components/home/LandingPage";
const AnalyzersPage = React.lazy(() => import("./pages/AnalyzersPage"));
const FieldMapping = React.lazy(
  () => import("./components/analyzers/FieldMapping/FieldMapping"),
);
const ErrorDashboardPage = React.lazy(
  () => import("./pages/ErrorDashboardPage"),
);
const CustomFieldTypeManagementPage = React.lazy(
  () => import("./pages/CustomFieldTypeManagementPage"),
);
const AnalyzerTypesPage = React.lazy(() => import("./pages/AnalyzerTypesPage"));
const QCDashboardPlaceholder = React.lazy(
  () => import("./pages/analyzers/QCDashboardPlaceholder"),
);
const QCAlertsPlaceholder = React.lazy(
  () => import("./pages/analyzers/QCAlertsPlaceholder"),
);
const CorrectiveActionsPlaceholder = React.lazy(
  () => import("./pages/analyzers/CorrectiveActionsPlaceholder"),
);
import ResultSearch from "./components/resultPage/ResultSearch";
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
const AnalyserResultIndex = React.lazy(
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
const RoutedResultsViewer = React.lazy(
  () => import("./components/patient/resultsViewer/results-viewer.tsx"),
);
import EOrderPage from "./components/eOrder/Index";
import RoutineIndex from "./components/reports/routine/Index.js";
import StudyIndex from "./components/reports/study/index.js";
import ReportIndex from "./components/reports/Index.js";
import PrintBarcode from "./components/printBarcode/Index";
import NonConformIndex from "./components/nonconform/index";
import SampleBatchEntrySetup from "./components/batchOrderEntry/SampleBatchEntrySetup.js";
import AuditTrailReportIndex from "./components/reports/auditTrailReport/Index.js";
import ReferredOutTests from "./components/resultPage/resultsReferredOut/ReferredOutTests.js";
import { Roles } from "./components/utils/Utils";
import NoteBookInstanceEntryForm from "./components/notebook/NoteBookInstanceEntryForm.js";
import NotebookSampleOrder from "./components/notebook/NotebookSampleOrder.js";
const FreezerMonitoringDashboard = React.lazy(
  () => import("./components/coldStorage/FreezerMonitoringDashboard"),
);
import ProgramDashboard from "./components/program/programDashboard.jsx";
import ProgramCaseView from "./components/program/programCaseView.jsx";
import SampleManagement from "./components/sampleManagement/SampleManagement";
const ShipmentReport = React.lazy(
  () => import("./components/shipment/ShipmentReport"),
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
                  component={() => {
                    const GenericSampleOrder =
                      require("./components/genericSample/GenericSampleOrder").default;
                    return <GenericSampleOrder />;
                  }}
                  role={Roles.RECEPTION}
                />
                <SecureRoute
                  path="/GenericSample/Edit"
                  exact
                  component={() => {
                    const GenericSampleOrderEdit =
                      require("./components/genericSample/GenericSampleOrderEdit").default;
                    return <GenericSampleOrderEdit />;
                  }}
                  role={Roles.RECEPTION}
                />
                <SecureRoute
                  path="/GenericSample/Import"
                  exact
                  component={() => {
                    const GenericSampleOrderImport =
                      require("./components/genericSample/GenericSampleOrderImport").default;
                    return <GenericSampleOrderImport />;
                  }}
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
                  path="/PatientManagement"
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
                <SecureRoute
                  path="/EQAOrders"
                  exact
                  component={() => <EQAOrdersPage />}
                  role={[Roles.RECEPTION, Roles.RESULTS]}
                />
                <SecureRoute
                  path="/EQAMyPrograms"
                  exact
                  component={() => <MyProgramsPage />}
                  role={[Roles.RECEPTION, Roles.RESULTS]}
                />
                <SecureRoute
                  path="/EQAManagement"
                  exact
                  component={() => <EQAProgramManagement />}
                  role={[Roles.RECEPTION, Roles.RESULTS]}
                />
                <SecureRoute
                  path="/EQAResults"
                  exact
                  component={() => <EQAResultsPage />}
                  role={[Roles.RECEPTION, Roles.RESULTS]}
                />
                <SecureRoute
                  path="/EQAParticipants"
                  exact
                  component={() => <EQAParticipantsPage />}
                  role={[Roles.RECEPTION, Roles.RESULTS]}
                />
                <SecureRoute
                  path="/EQADistribution/create"
                  exact
                  component={() => <CreateDistribution />}
                  role={[Roles.RECEPTION, Roles.RESULTS]}
                />
                <SecureRoute
                  path="/EQADistribution"
                  exact
                  component={() => <EQADistributionDashboard />}
                  role={[Roles.RECEPTION, Roles.RESULTS]}
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
                  path="/Storage/:tab"
                  component={() => (
                    <RouteErrorBoundary {...routeErrorStorage}>
                      <StorageDashboard />
                    </RouteErrorBoundary>
                  )}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
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
                  path="/analyzers/qc"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorAnalyzers}>
                      <Suspense fallback={null}>
                        <QCDashboardPlaceholder />
                      </Suspense>
                    </RouteErrorBoundary>
                  )}
                  role={Roles.ANALYSER_IMPORT}
                />
                <SecureRoute
                  path="/analyzers/qc/alerts"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorAnalyzers}>
                      <Suspense fallback={null}>
                        <QCAlertsPlaceholder />
                      </Suspense>
                    </RouteErrorBoundary>
                  )}
                  role={Roles.ANALYSER_IMPORT}
                />
                <SecureRoute
                  path="/analyzers/qc/corrective-actions"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorAnalyzers}>
                      <Suspense fallback={null}>
                        <CorrectiveActionsPlaceholder />
                      </Suspense>
                    </RouteErrorBoundary>
                  )}
                  role={Roles.ANALYSER_IMPORT}
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
                  component={() => {
                    const GenericSampleResults =
                      require("./components/genericSample/GenericSampleResults").default;
                    return <GenericSampleResults />;
                  }}
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
                <SecureRoute
                  path="/result"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorResultsSearch}>
                      <ResultSearch />
                    </RouteErrorBoundary>
                  )}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/LogbookResults"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorResultsSearch}>
                      <ResultSearch />
                    </RouteErrorBoundary>
                  )}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/PatientResults"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorResultsSearch}>
                      <ResultSearch />
                    </RouteErrorBoundary>
                  )}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/AccessionResults"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorResultsSearch}>
                      <ResultSearch />
                    </RouteErrorBoundary>
                  )}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/StatusResults"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorResultsSearch}>
                      <ResultSearch />
                    </RouteErrorBoundary>
                  )}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/RangeResults"
                  exact
                  component={() => (
                    <RouteErrorBoundary {...routeErrorResultsSearch}>
                      <ResultSearch />
                    </RouteErrorBoundary>
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
                <SecureRoute
                  path="/AuditTrailReport"
                  exact
                  component={() => <AuditTrailReportIndex />}
                  role={Roles.GLOBAL_ADMIN}
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
