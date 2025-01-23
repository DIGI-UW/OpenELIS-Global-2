import React, { useState, useEffect } from "react";
import { BrowserRouter as Router, Route, Switch } from "react-router-dom";
import { IntlProvider } from "react-intl";
import { confirmAlert } from "react-confirm-alert";
import Layout from "./components/layout/Layout";
import Home from "./components/Home";
import Login from "./components/Login";
import LandingPage from "./components/home/LandingPage";
import { Admin } from "./components";
import ResultSearch from "./components/resultPage/ResultSearch";
import UserSessionDetailsContext from "./UserSessionDetailsContext";
import { getFromOpenElisServer } from "./components/utils/Utils";
import "./App.css";
import messages_en from "./languages/en.json";
import messages_fr from "./languages/fr.json";
import config from "./config.json";
import { SecureRoute } from "./components/security";
import "./index.scss";
import RedirectOldUI from "./RedirectOldUI";
import PatientManagement from "./components/patient/PatientManagement";
import PatientHistory from "./components/patient/PatientHistory";
import Workplan from "./components/workplan/Workplan";
import AddOrder from "./components/addOrder/Index";
import FindOrder from "./components/modifyOrder/Index";
import ModifyOrder from "./components/modifyOrder/ModifyOrder";
import RoutineReports from "./components/reports/Routine";
import StudyReports from "./components/reports/Study";
import StudyValidation from "./components/validation/Index";
import PathologyDashboard from "./components/pathology/PathologyDashboard";
import CytologyDashboard from "./components/cytology/CytologyDashBoard";
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

export default function App() {
  let i18nConfig = {
    locale: navigator.language.split(/[-_]/)[0],
    defaultLocale: "en",
    messages: messages_en,
  };

  const [userSessionDetails, setUserSessionDetails] = useState({});
  const [errorLoadingSessionDetails, setErrorLoadingSessionDetails] =
    useState(false);
  const [locale, setLocale] = useState("en");

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

  i18nConfig.locale =
    localStorage.getItem("locale") || navigator.language.split(/[-_]/)[0];
  switch (i18nConfig.locale) {
    case "en":
      i18nConfig.messages = messages_en;
      break;
    case "fr":
      i18nConfig.messages = messages_fr;
      break;
    default:
      i18nConfig.messages = messages_en;
      break;
  }

  const logout = () => {
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
  };

  const changeLanguageReact = (lang) => {
    switch (lang) {
      case "en":
        i18nConfig.messages = messages_en;
        break;
      case "fr":
        i18nConfig.messages = messages_fr;
        break;
      default:
        i18nConfig.messages = messages_en;
        break;
    }
    i18nConfig.locale = lang;
    localStorage.setItem("locale", lang);
    //rerender the component on changing locale
    setLocale(lang);
  };

  const changeLanguageBackend = async (lang) => {
    if (userSessionDetails.authenticated) {
      getFromOpenElisServer("/Home?lang=" + lang, () => {});
    } else {
      getFromOpenElisServer("/LoginPage?lang=" + lang, () => {});
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
      locale={i18nConfig.locale}
      key={i18nConfig.locale}
      defaultLocale={i18nConfig.defaultLocale}
      messages={i18nConfig.messages}
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
                  role="Global Administrator"
                />
                <SecureRoute
                  path="/MasterListsPage"
                  exact
                  component={() => <Admin />}
                  role="Global Administrator"
                />
                <SecureRoute
                  path="/PathologyDashboard"
                  exact
                  component={() => <PathologyDashboard />}
                  role=""
                  labUnitRole={{ Pathology: ["Results"] }}
                />
                <SecureRoute
                  path="/PathologyCaseView/:pathologySampleId"
                  exact
                  component={() => <PathologyCaseView />}
                  role=""
                  labUnitRole={{ Pathology: ["Results"] }}
                />
                <SecureRoute
                  path="/ImmunohistochemistryDashboard"
                  exact
                  component={() => <ImmunohistochemistryDashboard />}
                  role=""
                  labUnitRole={{ Immunohistochemistry: ["Results"] }}
                />
                <SecureRoute
                  path="/ImmunohistochemistryCaseView/:immunohistochemistrySampleId"
                  exact
                  component={() => <ImmunohistochemistryCaseView />}
                  role=""
                  labUnitRole={{ Immunohistochemistry: ["Results"] }}
                />
                <SecureRoute
                  path="/CytologyDashboard"
                  exact
                  component={() => <CytologyDashboard />}
                  role=""
                  labUnitRole={{ Cytology: ["Results"] }}
                />
                <SecureRoute
                  path="/CytologyCaseView/:cytologySampleId"
                  exact
                  component={() => <CytologyCaseView />}
                  role=""
                  labUnitRole={{ Cytology: ["Results"] }}
                />
                <SecureRoute
                  path="/SamplePatientEntry"
                  exact
                  component={() => <AddOrder />}
                  role={["Reception"]}
                />
                <SecureRoute
                  path="/ModifyOrder"
                  exact
                  component={() => <ModifyOrder />}
                  role="Reception"
                />
                <SecureRoute
                  path="/SampleEdit"
                  exact
                  component={() => <FindOrder />}
                  role="Reception"
                />
                <SecureRoute
                  path="/ReportNonConformingEvent"
                  exact
                  component={() => (
                    <NonConformIndex form="ReportNonConformingEvent" />
                  )}
                  role="Reception"
                />
                <SecureRoute
                  path="/ViewNonConformingEvent"
                  exact
                  component={() => (
                    <NonConformIndex form="ViewNonConformingEvent" />
                  )}
                  role="Reception"
                />

                <SecureRoute
                  path="/NCECorrectiveAction"
                  exact
                  component={() => (
                    <NonConformIndex form="NCECorrectiveAction" />
                  )}
                />

                <SecureRoute
                  path="/SampleBatchEntrySetup"
                  exact
                  component={() => <SampleBatchEntrySetup />}
                  role="Reception"
                />

                <SecureRoute
                  path="/ElectronicOrders"
                  exact
                  component={() => <EOrderPage />}
                  role="Reception"
                />
                <SecureRoute
                  path="/PrintBarcode"
                  exact
                  component={() => <PrintBarcode />}
                  role="Reception"
                />
                <SecureRoute
                  path="/PatientManagement"
                  exact
                  component={() => <PatientManagement />}
                  role="Reception"
                />
                <SecureRoute
                  path="/PatientHistory"
                  exact
                  component={() => <PatientHistory />}
                  role="Reception"
                />
                <SecureRoute
                  path="/PatientResults/:patientId"
                  exact
                  component={() => <RoutedResultsViewer />}
                  role="Reception"
                />

                <SecureRoute
                  path="/WorkPlanByTestSection"
                  exact
                  component={() => <Workplan type="unit" />}
                  role="Results"
                />
                <SecureRoute
                  path="/WorkplanByTest"
                  exact
                  component={() => <Workplan type="test" />}
                  role="Results"
                />
                <SecureRoute
                  path="/WorkplanByPanel"
                  exact
                  component={() => <Workplan type="panel" />}
                  role="Results"
                />
                <SecureRoute
                  path="/WorkplanByPriority"
                  exact
                  component={() => <Workplan type="priority" />}
                  role="Results"
                />
                <SecureRoute
                  path="/result"
                  exact
                  component={() => <ResultSearch />}
                  role="Results"
                />
                <SecureRoute
                  path="/LogbookResults"
                  exact
                  component={() => <ResultSearch />}
                  role="Results"
                />
                <SecureRoute
                  path="/PatientResults"
                  exact
                  component={() => <ResultSearch />}
                  role="Results"
                />
                <SecureRoute
                  path="/AccessionResults"
                  exact
                  component={() => <ResultSearch />}
                  role="Results"
                />
                <SecureRoute
                  path="/StatusResults"
                  exact
                  component={() => <ResultSearch />}
                  role="Results"
                />
                <SecureRoute
                  path="/RangeResults"
                  exact
                  component={() => <ResultSearch />}
                  role="Results"
                />
                <SecureRoute
                  path="/ReferredOutTests"
                  exact
                  component={() => <ReferredOutTests />}
                  role="Results"
                />
                <SecureRoute
                  path="/RoutineReports"
                  exact
                  component={() => <RoutineReports />}
                  role="Reports"
                />
                <SecureRoute
                  path="/RoutineReport"
                  exact
                  component={() => <RoutineIndex />}
                  role="Reports"
                />
                <SecureRoute
                  path="/StudyReports"
                  exact
                  component={() => <StudyReports />}
                  role="Reports"
                />
                <SecureRoute
                  path="/StudyReport"
                  exact
                  component={() => <StudyIndex />}
                  role="Reports"
                />
                <SecureRoute
                  path="/Report"
                  exact
                  component={() => <ReportIndex />}
                  role="Reports"
                />
                <SecureRoute
                  path="/AuditTrailReport"
                  exact
                  component={() => <AuditTrailReportIndex />}
                  role="Reports"
                />
                <SecureRoute
                  path="/validation"
                  exact
                  component={() => <StudyValidation />}
                  role="Validation"
                />
                <SecureRoute
                  path="/ResultValidation"
                  exact
                  component={() => <StudyValidation />}
                  role="Validation"
                />
                <SecureRoute
                  path="/AccessionValidation"
                  exact
                  component={() => <StudyValidation />}
                  role="Validation"
                />
                <SecureRoute
                  path="/AccessionValidationRange"
                  exact
                  component={() => <StudyValidation />}
                  role="Validation"
                />
                <SecureRoute
                  path="/ResultValidationByTestDate"
                  exact
                  component={() => <StudyValidation />}
                  role="Validation"
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
