import React, { useState, useEffect } from "react";
import { Route, Routes, useNavigate } from "react-router-dom";
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
import ChangePassword from "./components/ChangePassword.js";

export default function App() {
  let i18nConfig = {
    locale: navigator.language.split(/[-_]/)[0],
    defaultLocale: "en",
    messages: messages_en,
  };

  const navigate = useNavigate();
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
                  navigate(window.location.origin);
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
          navigate(config.loginRedirect);
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
          navigate(config.loginRedirect);
        })
        .catch((error) => {
          console.error(error);
        });
    }
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
          <Layout onChangeLanguage={onChangeLanguage}>
            <Routes>
              <Route path="/login" exact element={<Login />} />
              <Route
                path="/ChangePasswordLogin"
                exact
                element={<ChangePassword />}
              />
              <Route path="/landing" exact element={<LandingPage />} />
              <Route
                path="/"
                element={<SecureRoute exact element={<Home />} role="" />}
              />
              <Route
                path="/Dashboard"
                element={<SecureRoute exact element={<Home />} role="" />}
              />
              <Route
                path="/admin"
                element={
                  <SecureRoute
                    exact
                    element={<Admin />}
                    role="Global Administrator"
                  />
                }
              />
              <Route
                path="/MasterListsPage"
                element={
                  <SecureRoute
                    exact
                    element={<Admin />}
                    role="Global Administrator"
                  />
                }
              />
              <Route
                path="/PathologyDashboard"
                element={
                  <SecureRoute
                    exact
                    element={<PathologyDashboard />}
                    role=""
                    labUnitRole={{ Pathology: ["Results"] }}
                  />
                }
              />
              <Route
                path="/PathologyCaseView/:pathologySampleId"
                element={
                  <SecureRoute
                    exact
                    element={<PathologyCaseView />}
                    role=""
                    labUnitRole={{ Pathology: ["Results"] }}
                  />
                }
              />
              <Route
                path="/ImmunohistochemistryDashboard"
                element={
                  <SecureRoute
                    exact
                    element={<ImmunohistochemistryDashboard />}
                    role=""
                    labUnitRole={{ Immunohistochemistry: ["Results"] }}
                  />
                }
              />
              <Route
                path="/ImmunohistochemistryCaseView/:immunohistochemistrySampleId"
                element={
                  <SecureRoute
                    exact
                    element={<ImmunohistochemistryCaseView />}
                    role=""
                    labUnitRole={{ Immunohistochemistry: ["Results"] }}
                  />
                }
              />
              <Route
                path="/CytologyDashboard"
                element={
                  <SecureRoute
                    exact
                    element={<CytologyDashboard />}
                    role=""
                    labUnitRole={{ Cytology: ["Results"] }}
                  />
                }
              />
              <Route
                path="/CytologyCaseView/:cytologySampleId"
                element={
                  <SecureRoute
                    exact
                    element={<CytologyCaseView />}
                    role=""
                    labUnitRole={{ Cytology: ["Results"] }}
                  />
                }
              />
              <Route
                path="/SamplePatientEntry"
                element={
                  <SecureRoute
                    exact
                    element={<AddOrder />}
                    role={["Reception"]}
                  />
                }
              />
              <Route
                path="/ModifyOrder"
                element={
                  <SecureRoute
                    exact
                    element={<ModifyOrder />}
                    role="Reception"
                  />
                }
              />
              <Route
                path="/SampleEdit"
                element={
                  <SecureRoute exact element={<FindOrder />} role="Reception" />
                }
              />
              <Route
                path="/ReportNonConformingEvent"
                element={
                  <SecureRoute
                    exact
                    element={
                      <NonConformIndex form="ReportNonConformingEvent" />
                    }
                    role="Reception"
                  />
                }
              />
              <Route
                path="/ViewNonConformingEvent"
                element={
                  <SecureRoute
                    exact
                    element={<NonConformIndex form="ViewNonConformingEvent" />}
                    role="Reception"
                  />
                }
              />
              <Route
                path="/NCECorrectiveAction"
                element={
                  <SecureRoute
                    exact
                    element={<NonConformIndex form="NCECorrectiveAction" />}
                  />
                }
              />
              <Route
                path="/SampleBatchEntrySetup"
                element={
                  <SecureRoute
                    exact
                    element={<SampleBatchEntrySetup />}
                    role="Reception"
                  />
                }
              />
              <Route
                path="/ElectronicOrders"
                element={
                  <SecureRoute
                    exact
                    element={<EOrderPage />}
                    role="Reception"
                  />
                }
              />
              <Route
                path="/PrintBarcode"
                element={
                  <SecureRoute
                    exact
                    element={<PrintBarcode />}
                    role="Reception"
                  />
                }
              />
              <Route
                path="/PatientManagement"
                element={
                  <SecureRoute
                    exact
                    element={<PatientManagement />}
                    role="Reception"
                  />
                }
              />
              <Route
                path="/PatientHistory"
                element={
                  <SecureRoute
                    exact
                    element={<PatientHistory />}
                    role="Reception"
                  />
                }
              />
              <Route
                path="/PatientResults/:patientId"
                element={
                  <SecureRoute
                    exact
                    element={<RoutedResultsViewer />}
                    role="Reception"
                  />
                }
              />
              <Route
                path="/WorkPlanByTestSection"
                element={
                  <SecureRoute
                    exact
                    element={<Workplan type="unit" />}
                    role="Results"
                  />
                }
              />
              <Route
                path="/WorkplanByTest"
                element={
                  <SecureRoute
                    exact
                    element={<Workplan type="test" />}
                    role="Results"
                  />
                }
              />
              <Route
                path="/WorkplanByPanel"
                element={
                  <SecureRoute
                    exact
                    element={<Workplan type="panel" />}
                    role="Results"
                  />
                }
              />
              <Route
                path="/WorkplanByPriority"
                element={
                  <SecureRoute
                    exact
                    element={<Workplan type="priority" />}
                    role="Results"
                  />
                }
              />
              <Route
                path="/result"
                element={
                  <SecureRoute
                    exact
                    element={<ResultSearch />}
                    role="Results"
                  />
                }
              />
              <Route
                path="/LogbookResults"
                element={
                  <SecureRoute
                    exact
                    element={<ResultSearch />}
                    role="Results"
                  />
                }
              />
              <Route
                path="/PatientResults"
                element={
                  <SecureRoute
                    exact
                    element={<ResultSearch />}
                    role="Results"
                  />
                }
              />
              <Route
                path="/AccessionResults"
                element={
                  <SecureRoute
                    exact
                    element={<ResultSearch />}
                    role="Results"
                  />
                }
              />
              <Route
                path="/StatusResults"
                element={
                  <SecureRoute
                    exact
                    element={<ResultSearch />}
                    role="Results"
                  />
                }
              />
              <Route
                path="/RangeResults"
                element={
                  <SecureRoute
                    exact
                    element={<ResultSearch />}
                    role="Results"
                  />
                }
              />
              <Route
                path="/ReferredOutTests"
                element={
                  <SecureRoute
                    exact
                    element={<ReferredOutTests />}
                    role="Results"
                  />
                }
              />
              <Route
                path="/RoutineReports"
                element={
                  <SecureRoute
                    exact
                    element={<RoutineReports />}
                    role="Reports"
                  />
                }
              />
              <Route
                path="/RoutineReport"
                element={
                  <SecureRoute
                    exact
                    element={<RoutineIndex />}
                    role="Reports"
                  />
                }
              />
              <Route
                path="/StudyReports"
                element={
                  <SecureRoute
                    exact
                    element={<StudyReports />}
                    role="Reports"
                  />
                }
              />
              <Route
                path="/StudyReport"
                element={
                  <SecureRoute exact element={<StudyIndex />} role="Reports" />
                }
              />
              <Route
                path="/Report"
                element={
                  <SecureRoute exact element={<ReportIndex />} role="Reports" />
                }
              />
              <Route
                path="/AuditTrailReport"
                element={
                  <SecureRoute
                    exact
                    element={<AuditTrailReportIndex />}
                    role="Reports"
                  />
                }
              />
              <Route
                path="/validation"
                element={
                  <SecureRoute
                    exact
                    element={<StudyValidation />}
                    role="Validation"
                  />
                }
              />
              <Route
                path="/ResultValidation"
                element={
                  <SecureRoute
                    exact
                    element={<StudyValidation />}
                    role="Validation"
                  />
                }
              />
              <Route
                path="/AccessionValidation"
                element={
                  <SecureRoute
                    exact
                    element={<StudyValidation />}
                    role="Validation"
                  />
                }
              />
              <Route
                path="/AccessionValidationRange"
                element={
                  <SecureRoute
                    exact
                    element={<StudyValidation />}
                    role="Validation"
                  />
                }
              />
              <Route
                path="/ResultValidationByTestDate"
                element={
                  <SecureRoute
                    exact
                    element={<StudyValidation />}
                    role="Validation"
                  />
                }
              />
              <Route path="*" element={<RedirectOldUI />} />
            </Routes>
          </Layout>
        </>
      </UserSessionDetailsContext.Provider>
    </IntlProvider>
  );
}
