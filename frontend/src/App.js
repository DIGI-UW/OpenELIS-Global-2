import React, { useState, useEffect } from "react";
import { BrowserRouter as Router, Route, Switch } from "react-router-dom";
import { IntlProvider } from "react-intl";
import { confirmAlert } from "react-confirm-alert";
import Layout from "./components/layout/Layout";
import Home from "./components/Home";
import StorageDashboard from "./components/storage/StorageDashboard";
import InventoryManagement from "./components/inventory/InventoryManagement";
import Login from "./components/Login";
import LandingPage from "./components/home/LandingPage";
import { Admin } from "./components";
import ResultSearch from "./components/resultPage/ResultSearch";
import UserSessionDetailsContext from "./UserSessionDetailsContext";
import { getFromOpenElisServer } from "./components/utils/Utils";
import "./App.css";
import { languages } from "./languages";
import config from "./config.json";
import { SecureRoute } from "./components/security";
import "./index.scss";
import RedirectOldUI from "./RedirectOldUI";
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
import StudyValidation from "./components/validation/Index";
import AnalyserResultIndex from "./components/analyserResults/Index";
import PathologyDashboard from "./components/pathology/PathologyDashboard";
import CytologyDashboard from "./components/cytology/CytologyDashBoard";
import NoteBookDashBoard from "./components/notebook/NoteBookDashBoard";
import NoteBookEntryForm from "./components/notebook/NoteBookEntryForm";
import CytologyCaseView from "./components/cytology/CytologyCaseView";
import PathologyCaseView from "./components/pathology/PathologyCaseView";
import ImmunohistochemistryDashboard from "./components/immunohistochemistry/ImmunohistochemistryDashboard";
import ImmunohistochemistryCaseView from "./components/immunohistochemistry/ImmunohistochemistryCaseView";
import RoutedResultsViewer from "./components/patient/resultsViewer/results-viewer.tsx";
import EOrderPage from "./components/eOrder/Index";
import RoutineIndex from "./components/reports/routine/Index.js";
import StudyIndex from "./components/reports/study/index.js";
import ReportIndex from "./components/reports/Index.js";
import PrintBarcode from "./components/printBarcode/Index";
import NonConformIndex from "./components/nonconform/index";
import SampleBatchEntrySetup from "./components/batchOrderEntry/SampleBatchEntrySetup.js";
import AuditTrailReportIndex from "./components/reports/auditTrailReport/Index.js";
import ReferredOutTests from "./components/resultPage/resultsReferredOut/ReferredOutTests.js";
import ChangePassword from "./components/ChangePassword.js";
import { Roles } from "./components/utils/Utils";
import NoteBookInstanceEntryForm from "./components/notebook/NoteBookInstanceEntryForm.js";
import NotebookSampleOrder from "./components/notebook/NotebookSampleOrder.js";
import FreezerMonitoringDashboard from "./components/coldStorage/FreezerMonitoringDashboard";
import ProgramDashboard from "./components/program/programDashboard.jsx";
import ProgramCaseView from "./components/program/programCaseView.jsx";
import SampleManagement from "./components/sampleManagement/SampleManagement";

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
          const POPUP_HEIGHT = 700;
          const POPUP_WIDTH = 600;
          const top =
            window.outerHeight / 2 + window.screenY - POPUP_HEIGHT / 2;
          const left = window.outerWidth / 2 + window.screenX - POPUP_WIDTH / 2;
          const newWindow = window.open(
            "",
            "SAML Popup",
            `height=${POPUP_HEIGHT},width=${POPUP_WIDTH},top=${top},left=${left}`,
          );
          newWindow.document.write(html);
          newWindow.document.close();
          getUserSessionDetails();
          window.location.href = config.loginRedirect;
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
    if (!languages[lang]) {
      lang = "en";
    }
    setLocale(lang);
    setMessages(languages[lang].messages);
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
                  path="/GenericSample/Order"
                  exact
                  component={() => {
                    const GenericSampleOrder =
                      require("./components/genericSample/GenericSampleOrder").default;
                    return <GenericSampleOrder />;
                  }}
                  role=""
                />
                <SecureRoute
                  path="/GenericSample/Edit"
                  exact
                  component={() => {
                    const GenericSampleOrderEdit =
                      require("./components/genericSample/GenericSampleOrderEdit").default;
                    return <GenericSampleOrderEdit />;
                  }}
                  role=""
                />
                <SecureRoute
                  path="/GenericSample/Import"
                  exact
                  component={() => {
                    const GenericSampleOrderImport =
                      require("./components/genericSample/GenericSampleOrderImport").default;
                    return <GenericSampleOrderImport />;
                  }}
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
                  path="/FreezerMonitoring"
                  exact
                  component={() => <FreezerMonitoringDashboard />}
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
                  path="/SamplePatientEntry"
                  exact
                  component={() => <AddOrder />}
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
                  path="/Storage"
                  exact
                  component={() => <StorageDashboard />}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/Storage/:tab"
                  component={() => <StorageDashboard />}
                  role={[Roles.RECEPTION, Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/inventory"
                  exact
                  component={() => <InventoryManagement />}
                  role={[Roles.RESULTS, Roles.GLOBAL_ADMIN]}
                />
                <SecureRoute
                  path="/SampleManagement"
                  exact
                  component={() => <SampleManagement />}
                  role={[Roles.RECEPTION, Roles.RESULTS]}
                />
                <SecureRoute
                  path="/GenericSample/Results"
                  exact
                  component={() => {
                    const GenericSampleResults =
                      require("./components/genericSample/GenericSampleResults").default;
                    return <GenericSampleResults />;
                  }}
                  role={[Roles.RESULTS]}
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
                  path="/Aliquot"
                  exact
                  component={() => <Aliquot />}
                  role={Roles.RECEPTION}
                />

                <SecureRoute
                  path="/PatientResults/:patientId"
                  exact
                  component={() => <RoutedResultsViewer />}
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
                  component={() => <ResultSearch />}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/LogbookResults"
                  exact
                  component={() => <ResultSearch />}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/PatientResults"
                  exact
                  component={() => <ResultSearch />}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/AccessionResults"
                  exact
                  component={() => <ResultSearch />}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/StatusResults"
                  exact
                  component={() => <ResultSearch />}
                  role={Roles.RESULTS}
                />
                <SecureRoute
                  path="/RangeResults"
                  exact
                  component={() => <ResultSearch />}
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
                  component={() => <AnalyserResultIndex />}
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
