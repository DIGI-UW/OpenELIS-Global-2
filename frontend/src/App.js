import React, { useEffect, useState, Suspense } from "react";
import { confirmAlert } from "react-confirm-alert";
import { IntlProvider } from "react-intl";
import { Route, BrowserRouter as Router, Switch } from "react-router-dom";
import "./App.css";
import UserSessionDetailsContext from "./UserSessionDetailsContext";
import Layout from "./components/layout/Layout";
import { getFromOpenElisServer } from "./components/utils/Utils";
import { loadAndApplyBranding } from "./components/utils/BrandingUtils";
import { languages, languageMessages } from "./languages";
import config from "./config.json";
import { SecureRoute } from "./components/security";
import "./index.scss";
import { Roles } from "./components/utils/Utils";
import RouteErrorBoundary from "./components/common/RouteErrorBoundary";
import lazyWithRetry from "./utils/lazyWithRetry";
import PageLoading from "./components/common/PageLoading";

// --- Lazy-loaded route components ---
const RedirectOldUI = lazyWithRetry(() => import("./RedirectOldUI"));
const Admin = lazyWithRetry(() => import("./components/admin/Admin"));
const ChangePassword = lazyWithRetry(
  () => import("./components/ChangePassword"),
);
const Home = lazyWithRetry(() => import("./components/Home"));
const StorageDashboard = lazyWithRetry(
  () => import("./components/storage/StorageDashboard"),
);
const AlertsDashboard = lazyWithRetry(
  () => import("./components/alerts/AlertsDashboard"),
);
const EQAManagementDashboard = lazyWithRetry(
  () => import("./components/eqa/EQAManagementDashboard"),
);
const EQADistributionDashboard = lazyWithRetry(
  () => import("./components/eqa/EQADistributionDashboard"),
);
const CreateDistribution = lazyWithRetry(
  () => import("./components/eqa/EQADistribution/CreateDistribution"),
);
const EQAOrdersPage = lazyWithRetry(
  () => import("./components/eqa/EQAOrdersPage"),
);
const MyProgramsPage = lazyWithRetry(
  () => import("./components/eqa/MyProgramsPage"),
);
const EQAParticipantsPage = lazyWithRetry(
  () => import("./components/eqa/EQAParticipantsPage"),
);
const EQAResultsPage = lazyWithRetry(
  () => import("./components/eqa/EQAResultsPage"),
);
const InventoryManagement = lazyWithRetry(
  () => import("./components/inventory/InventoryManagement"),
);
const ShipmentDashboard = lazyWithRetry(
  () => import("./components/shipment/ShipmentDashboard"),
);
const BoxCreation = lazyWithRetry(
  () => import("./components/shipment/BoxCreation"),
);
const BoxDetails = lazyWithRetry(
  () => import("./components/shipment/BoxDetails"),
);
const ReceptionWorkflow = lazyWithRetry(
  () => import("./components/shipment/ReceptionWorkflow"),
);
const Login = lazyWithRetry(() => import("./components/Login"));
const LandingPage = lazyWithRetry(
  () => import("./components/home/LandingPage"),
);
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
const QCDashboardPlaceholder = lazyWithRetry(
  () => import("./pages/analyzers/QCDashboardPlaceholder"),
);
const QCAlertsPlaceholder = lazyWithRetry(
  () => import("./pages/analyzers/QCAlertsPlaceholder"),
);
const CorrectiveActionsPlaceholder = lazyWithRetry(
  () => import("./pages/analyzers/CorrectiveActionsPlaceholder"),
);
const ResultSearch = lazyWithRetry(
  () => import("./components/resultPage/ResultSearch"),
);
const PatientManagement = lazyWithRetry(
  () => import("./components/patient/PatientManagement"),
);
const PatientHistory = lazyWithRetry(
  () => import("./components/patient/PatientHistory"),
);
const PatientMerge = lazyWithRetry(
  () => import("./components/patient/PatientMerge"),
);
const Aliquot = lazyWithRetry(() => import("./components/sample/Aliquot"));
const Workplan = lazyWithRetry(() => import("./components/workplan/Workplan"));
const AddOrder = lazyWithRetry(() => import("./components/addOrder/Index"));
const FindOrder = lazyWithRetry(() => import("./components/modifyOrder/Index"));
const ModifyOrder = lazyWithRetry(
  () => import("./components/modifyOrder/ModifyOrder"),
);
const RoutineReports = lazyWithRetry(
  () => import("./components/reports/Routine"),
);
const StudyReports = lazyWithRetry(() => import("./components/reports/Study"));
const StudyValidation = lazyWithRetry(
  () => import("./components/validation/Index"),
);
const AnalyserResultIndex = lazyWithRetry(
  () => import("./components/analyserResults/Index"),
);
const PathologyDashboard = lazyWithRetry(
  () => import("./components/pathology/PathologyDashboard"),
);
const CytologyDashboard = lazyWithRetry(
  () => import("./components/cytology/CytologyDashBoard"),
);
const NoteBookDashBoard = lazyWithRetry(
  () => import("./components/notebook/NoteBookDashBoard"),
);
const NoteBookEntryForm = lazyWithRetry(
  () => import("./components/notebook/NoteBookEntryForm"),
);
const CytologyCaseView = lazyWithRetry(
  () => import("./components/cytology/CytologyCaseView"),
);
const PathologyCaseView = lazyWithRetry(
  () => import("./components/pathology/PathologyCaseView"),
);
const ImmunohistochemistryDashboard = lazyWithRetry(
  () =>
    import("./components/immunohistochemistry/ImmunohistochemistryDashboard"),
);
const ImmunohistochemistryCaseView = lazyWithRetry(
  () =>
    import("./components/immunohistochemistry/ImmunohistochemistryCaseView"),
);
const RoutedResultsViewer = lazyWithRetry(
  () => import("./components/patient/resultsViewer/results-viewer"),
);
const EOrderPage = lazyWithRetry(() => import("./components/eOrder/Index"));
const RoutineIndex = lazyWithRetry(
  () => import("./components/reports/routine/Index"),
);
const StudyIndex = lazyWithRetry(
  () => import("./components/reports/study/index"),
);
const ReportIndex = lazyWithRetry(() => import("./components/reports/Index"));
const PrintBarcode = lazyWithRetry(
  () => import("./components/printBarcode/Index"),
);
const NonConformIndex = lazyWithRetry(
  () => import("./components/nonconform/index"),
);
const SampleBatchEntrySetup = lazyWithRetry(
  () => import("./components/batchOrderEntry/SampleBatchEntrySetup"),
);
const AuditTrailReportIndex = lazyWithRetry(
  () => import("./components/reports/auditTrailReport/Index"),
);
const ReferredOutTests = lazyWithRetry(
  () => import("./components/resultPage/resultsReferredOut/ReferredOutTests"),
);
const NoteBookInstanceEntryForm = lazyWithRetry(
  () => import("./components/notebook/NoteBookInstanceEntryForm"),
);
const NotebookSampleOrder = lazyWithRetry(
  () => import("./components/notebook/NotebookSampleOrder"),
);
const FreezerMonitoringDashboard = lazyWithRetry(
  () => import("./components/coldStorage/FreezerMonitoringDashboard"),
);
const ProgramDashboard = lazyWithRetry(
  () => import("./components/program/programDashboard"),
);
const ProgramCaseView = lazyWithRetry(
  () => import("./components/program/programCaseView"),
);
const SampleManagement = lazyWithRetry(
  () => import("./components/sampleManagement/SampleManagement"),
);
const ShipmentReport = lazyWithRetry(
  () => import("./components/shipment/ShipmentReport"),
);
const ShipmentSettings = lazyWithRetry(
  () => import("./components/shipment/ShipmentSettings"),
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
    let counter = 0;
    while (counter < 10) {
      try {
        const response = await fetch(
          config.serverBaseUrl + `/session`,
          //includes the browser sessionId in the Header for Authentication on the backend server
          { credentials: "include" },
        );
        if (response.status === 200) {
          const jsonResp = await response.json();
          console.debug(JSON.stringify(jsonResp));
          if (jsonResp.authenticated) {
            localStorage.setItem("CSRF", jsonResp.csrf);
          }
          if (
            !Object.keys(jsonResp).every(
              (key) => jsonResp[key] === userSessionDetails[key],
            )
          ) {
            setUserSessionDetails(jsonResp);
          }
          setErrorLoadingSessionDetails(false);
          return jsonResp;
        } else {
          throw new Error(
            "Did not receive a successful response from the backend while retrieving user session details",
          );
        }
      } catch (error) {
        console.error(error);
        if (counter === 10) {
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
      ++counter;
    }
    setErrorLoadingSessionDetails(true);
    return userSessionDetails;
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
              <Suspense fallback={<PageLoading />}>
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
                    exact
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
                    component={() => <GenericSampleOrder />}
                    role=""
                  />
                  <SecureRoute
                    path="/GenericSample/Edit"
                    exact
                    component={() => <GenericSampleOrderEdit />}
                    role=""
                  />
                  <SecureRoute
                    path="/GenericSample/Import"
                    exact
                    component={() => <GenericSampleOrderImport />}
                    role=""
                  />
                  <SecureRoute
                    path="/FreezerMonitoring"
                    exact
                    component={() => <FreezerMonitoringDashboard />}
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
                    role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                  />
                  <SecureRoute
                    path="/EQAOrders"
                    exact
                    component={() => <EQAOrdersPage />}
                    role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                  />
                  <SecureRoute
                    path="/EQAMyPrograms"
                    exact
                    component={() => <MyProgramsPage />}
                    role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                  />
                  <SecureRoute
                    path="/EQAManagement"
                    exact
                    component={() => <EQAManagementDashboard />}
                    role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                  />
                  <SecureRoute
                    path="/EQAResults"
                    exact
                    component={() => <EQAResultsPage />}
                    role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                  />
                  <SecureRoute
                    path="/EQAParticipants"
                    exact
                    component={() => <EQAParticipantsPage />}
                    role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                  />
                  <SecureRoute
                    path="/EQADistribution/create"
                    exact
                    component={() => <CreateDistribution />}
                    role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                  />
                  <SecureRoute
                    path="/EQADistribution"
                    exact
                    component={() => <EQADistributionDashboard />}
                    role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
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
                    component={() => <ShipmentReport />}
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
                        <AnalyzersPage />
                      </RouteErrorBoundary>
                    )}
                    role={Roles.GLOBAL_ADMIN}
                  />
                  <SecureRoute
                    path="/analyzers/:id/mappings"
                    exact
                    component={() => (
                      <RouteErrorBoundary {...routeErrorAnalyzers}>
                        <FieldMapping />
                      </RouteErrorBoundary>
                    )}
                    role={Roles.GLOBAL_ADMIN}
                  />
                  <SecureRoute
                    path="/analyzers/errors"
                    exact
                    component={() => (
                      <RouteErrorBoundary {...routeErrorAnalyzers}>
                        <ErrorDashboardPage />
                      </RouteErrorBoundary>
                    )}
                    role={Roles.LAB_SUPERVISOR}
                  />
                  <SecureRoute
                    path="/analyzers/custom-field-types"
                    exact
                    component={() => (
                      <RouteErrorBoundary {...routeErrorAnalyzers}>
                        <CustomFieldTypeManagementPage />
                      </RouteErrorBoundary>
                    )}
                    role={Roles.GLOBAL_ADMIN}
                  />
                  <SecureRoute
                    path="/analyzers/types"
                    exact
                    component={() => (
                      <RouteErrorBoundary {...routeErrorAnalyzers}>
                        <AnalyzerTypesPage />
                      </RouteErrorBoundary>
                    )}
                    role={Roles.GLOBAL_ADMIN}
                  />
                  <SecureRoute
                    path="/analyzers/qc"
                    exact
                    component={() => (
                      <RouteErrorBoundary {...routeErrorAnalyzers}>
                        <QCDashboardPlaceholder />
                      </RouteErrorBoundary>
                    )}
                    role={Roles.LAB_SUPERVISOR}
                  />
                  <SecureRoute
                    path="/analyzers/qc/alerts"
                    exact
                    component={() => (
                      <RouteErrorBoundary {...routeErrorAnalyzers}>
                        <QCAlertsPlaceholder />
                      </RouteErrorBoundary>
                    )}
                    role={Roles.LAB_SUPERVISOR}
                  />
                  <SecureRoute
                    path="/analyzers/qc/corrective-actions"
                    exact
                    component={() => (
                      <RouteErrorBoundary {...routeErrorAnalyzers}>
                        <CorrectiveActionsPlaceholder />
                      </RouteErrorBoundary>
                    )}
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
                    role={Roles.GLOBAL_ADMIN}
                  />
                  <SecureRoute
                    path="/GenericSample/Results"
                    exact
                    component={() => <GenericSampleResults />}
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
                        <RoutedResultsViewer />
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
                        <AnalyserResultIndex />
                      </RouteErrorBoundary>
                    )}
                    role={Roles.ANALYSER_IMPORT}
                  />
                  <Route path="*" component={() => <RedirectOldUI />} />
                </Switch>
              </Suspense>
            </Layout>
          </Router>
        </>
      </UserSessionDetailsContext.Provider>
    </IntlProvider>
  );
}
