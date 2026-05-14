import React, {
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  createRef,
} from "react";
import config from "../config.json";
import "./Style.css";
import qs from "qs";
import { FormattedMessage, injectIntl } from "react-intl";
import { HardwareSecurityModule } from "@carbon/icons-react";
import {
  Form,
  Section,
  Heading,
  FormLabel,
  Grid,
  Column,
  TextInput,
  PasswordInput,
  Button,
  Stack,
  Loading,
} from "@carbon/react";
import { Formik } from "formik";
import { AlertDialog, NotificationKinds } from "./common/CustomNotification";
import UserSessionDetailsContext from "../UserSessionDetailsContext";
import { ConfigurationContext, NotificationContext } from "./layout/Layout";
import { getBranding } from "./utils/BrandingUtils";

function Login(props) {
  const { notificationVisible, addNotification, setNotificationVisible } =
    useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext);

  const { userSessionDetails, refresh } = useContext(UserSessionDetailsContext);
  const [submitting, setSubmitting] = useState(false);
  const [samlRedirectInitiated, setSamlRedirectInitiated] = useState(false);
  const [loginLogoUrl, setLoginLogoUrl] = useState(null);
  const [logoVersion, setLogoVersion] = useState(0); // Version counter for cache-busting
  const userIsActiveRef = useRef(false); // Track if user is actively typing without triggering re-renders
  const activityResetTimerRef = useRef(null);
  const markUserActive = useCallback(() => {
    userIsActiveRef.current = true;
    if (activityResetTimerRef.current) {
      clearTimeout(activityResetTimerRef.current);
    }
    activityResetTimerRef.current = setTimeout(() => {
      userIsActiveRef.current = false;
    }, 5000);
  }, []);
  const firstInput = createRef();

  // Auto-redirect to SAML if configured to bypass login page
  useEffect(() => {
    if (
      configurationProperties?.useSaml === "true" &&
      configurationProperties?.useSamlLoginPage === "false" &&
      !samlRedirectInitiated &&
      !userSessionDetails.authenticated
    ) {
      // Mark as initiated to prevent multiple redirects
      setSamlRedirectInitiated(true);

      // Use full-page redirect instead of popup to avoid popup blockers
      // Add 'redirect=true' parameter to tell backend to redirect to dashboard after auth
      window.location.href =
        config.serverBaseUrl + "/LoginPage?useSAML=true&redirect=true";
    }
  }, [
    configurationProperties,
    samlRedirectInitiated,
    userSessionDetails.authenticated,
  ]);

  useEffect(() => {
    firstInput?.current?.focus();

    // Poll every 10s, but skip polling while the user is actively typing.
    // Using a ref (not state) keeps a single stable interval alive across renders.
    const interval = setInterval(() => {
      if (!userIsActiveRef.current) {
        checkLogin();
      }
    }, 1000 * 10);

    return () => {
      clearInterval(interval);
      if (activityResetTimerRef.current) {
        clearTimeout(activityResetTimerRef.current);
      }
    };
  }, []);

  // Load branding configuration for login logo
  // Colors are handled by App.js
  useEffect(() => {
    getBranding((response) => {
      if (response) {
        // Check useHeaderLogoForLogin flag
        if (response.useHeaderLogoForLogin && response.headerLogoUrl) {
          setLoginLogoUrl(response.headerLogoUrl);
          setLogoVersion((prev) => prev + 1);
        } else if (response.loginLogoUrl) {
          setLoginLogoUrl(response.loginLogoUrl);
          setLogoVersion((prev) => prev + 1);
        }
      }
    });
  }, []);

  useEffect(() => {
    if (userSessionDetails.authenticated) {
      window.location.href = "/";
    }
  }, [userSessionDetails]);

  const checkLogin = () => {
    refresh();
  };

  const loginMessage = () => {
    // Add cache-busting parameter to prevent stale logo display after upload
    const logoSrc = loginLogoUrl
      ? `${config.serverBaseUrl}${loginLogoUrl}?v=${logoVersion}`
      : `images/openelis_logo_full.png`;

    return (
      <>
        <div
          className="absolute top-1 left-0 
         rounded-3xl h-90 w-full
         drop
        shadow-3xl border-collapse
        "
        >
          <div
            className=" 
        fixed top-8 
        left-0 
        rounded-2xl 
        sm:top-36 sm:left-20
        p-1 gap-4 mt-16 
        bg-slate-300 drop
        shadow-2xl border 
        lg:left-32 lg:top-20
         lg:p-4 
        md:top-40 md:left-4
         "
          >
            <picture className="opacity-100">
              <img
                src={logoSrc}
                alt="fullsize logo"
                className=" h-9 w-96 rounded-s-3xl"
                style={{ objectFit: "contain" }}
                onError={(e) => {
                  // Fallback to default logo if custom logo fails to load
                  e.target.src = `images/openelis_logo_full.png`;
                }}
              />
            </picture>
          </div>

          <div className="fixed inset-0 -z-10 ">
            <picture className="block w-full h-full opacity-60">
              <img
                src="images/laboratory-microscope.png"
                className="w-full h-full object-cover "
                alt="Laboratory microscope background"
              />
            </picture>
          </div>

          <div
            className=" 
fixed bottom-0
left-0 text-bold 
 flex-col lg:left-0 
  border bg-slate-300 drop-shadow-xl
  "
          >
            <p
              className=" 
  p-0 px-3 mt-2 lg:px-4 border  
   "
            >
              <FormattedMessage id="login.notice.message" />
            </p>
          </div>
        </div>
      </>
    );
  };

  const doLogin = (data) => {
    setSubmitting(true);
    fetch(config.serverBaseUrl + "/ValidateLogin?apiCall=true", {
      //includes the browser sessionId in the Header for Authentication on the backend server
      credentials: "include",
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
      },
      body: qs.stringify(data),
    })
      .then(async (response) => {
        setSubmitting(false);
        // get json response here
        let data = await response.json();
        if (response.status === 200) {
          window.location.href = "/";
        } else {
          addNotification({
            title: props.intl.formatMessage({
              id: "notification.title",
            }),
            message: props.intl.formatMessage({
              id: data.error,
            }),
            kind: NotificationKinds.error,
          });
          setNotificationVisible(true);
        }
      })
      .catch((error) => {
        setSubmitting(false);
        console.error(error);
        if (error instanceof SyntaxError) {
          addNotification({
            title: props.intl.formatMessage({
              id: "notification.title",
            }),
            message: props.intl.formatMessage({
              id: "notification.login.syntax.error",
            }),
            kind: NotificationKinds.error,
          });
          setNotificationVisible(true);
        } else {
          addNotification({
            title: props.intl.formatMessage({
              id: "notification.title",
            }),
            message: props.intl.formatMessage({
              id: "notification.login.generic.error",
            }),
            kind: NotificationKinds.error,
          });
          setNotificationVisible(true);
        }
      });
  };

  const renderOauthButtons = () => {
    return (
      <span id="oauth-buttons">
        {configurationProperties?.oauthUrls?.map((url) => (
          <Button
            key={url.key}
            type="button"
            renderIcon={HardwareSecurityModule}
            onClick={() => {
              window.location.href = config.serverBaseUrl + "/" + url.value;
            }}
          >
            <FormattedMessage id="label.button.login.sso" />
          </Button>
        ))}
      </span>
    );
  };

  return (
    <div
      className=" absolute content-evenly 
     top-36 px-0 lg:left-32 p-4 
     lg:p-4 lg:top-60 
     sm:rounded-2xl sm:top-80
      sm:left-20 
      md:top-80 
      md:left-4 "
    >
      <div
        className="absolute "
        data-cy="login-Page-Content"
        // className="  loginPageContent oe-loginPageContent  "
      >
        {notificationVisible === true ? <AlertDialog /> : ""}
        <div
          className=" container mx-auto
        left-4 p-4  mt-5 bg-slate-200
        rounded-3xl lg:rounded-3xl 
        w-96 drop-shadow-3xl
        border-collapse
        lg:left-80 lg:p-4 
         lg:h-64 lg:w-96"
        >
          <Grid fullWidth={true}>{loginMessage()}</Grid>
          <Grid fullWidth={false}>
            <Section>
              {samlRedirectInitiated ? (
                <Stack gap={5}>
                  <FormLabel>
                    <Heading>
                      <FormattedMessage id="login.title" />
                    </Heading>
                  </FormLabel>
                  <div style={{ textAlign: "center", padding: "2rem" }}>
                    <Loading
                      description={props.intl.formatMessage({
                        id: "login.redirecting.sso",
                      })}
                      withOverlay={false}
                    />
                    <p style={{ marginTop: "1rem" }}>
                      <FormattedMessage id="login.redirecting.sso" />
                    </p>
                  </div>
                </Stack>
              ) : (
                <Formik
                  initialValues={{
                    username: "",
                    password: "",
                  }}
                  onSubmit={(values) => {
                    doLogin(values);
                    fetch(config.serverBaseUrl + "/LoginPage", {
                      //includes the browser sessionId in the Header for Authentication on the backend server
                      credentials: "include",
                      method: "GET",
                    })
                      .then((response) => response.status)
                      .then(() => {
                        doLogin(values);
                      })
                      .catch(() => {});
                  }}
                >
                  {({ isValid, handleChange, handleSubmit }) => (
                    <div className=" lg:p-0 p-4 lg:mt-1 sm:p-0   ">
                      <Form onSubmit={handleSubmit} onChange={handleChange}>
                        <Stack gap={5}>
                          <FormLabel>
                            <Heading
                              className=" 
                             items-center
                             text-center 
                            font-bold
                             bg-slate-100
                              rounded-2xl
                               drop-shadow-2xl "
                            >
                              <FormattedMessage id="login.title" />
                            </Heading>
                          </FormLabel>
                          {configurationProperties?.useFormLogin == "true" && (
                            <>
                              <TextInput
                                id="loginName"
                                invalidText={props.intl.formatMessage({
                                  id: "login.msg.username.missing",
                                })}
                                labelText={props.intl.formatMessage({
                                  id: "login.msg.username",
                                })}
                                hideLabel={true}
                                placeholder={props.intl.formatMessage({
                                  id: "login.msg.username",
                                })}
                                autoComplete="off"
                                ref={firstInput}
                                onFocus={markUserActive}
                                onChange={markUserActive}
                              />
                              <PasswordInput
                                id="password"
                                invalidText={props.intl.formatMessage({
                                  id: "login.msg.password.missing",
                                })}
                                labelText={props.intl.formatMessage({
                                  id: "login.msg.password",
                                })}
                                hideLabel={true}
                                placeholder={props.intl.formatMessage({
                                  id: "login.msg.password",
                                })}
                                onFocus={markUserActive}
                                onChange={markUserActive}
                              />
                              <Stack orientation="horizontal">
                                <Button
                                  type="submit"
                                  disabled={!isValid}
                                  data-cy="loginButton"
                                >
                                  <FormattedMessage id="label.button.login" />
                                  <Loading
                                    small={true}
                                    withOverlay={false}
                                    className={submitting ? "show" : "hidden"}
                                  />
                                </Button>

                                <Button
                                  data-cy="changePassword"
                                  type="button"
                                  onClick={() => {
                                    window.location.href =
                                      "/ChangePasswordLogin";
                                  }}
                                >
                                  <FormattedMessage id="label.button.changepassword" />
                                </Button>
                              </Stack>
                            </>
                          )}
                          {configurationProperties?.useSaml == "true" &&
                            configurationProperties?.useSamlLoginPage !==
                              "false" && (
                              <Button
                                type="button"
                                renderIcon={HardwareSecurityModule}
                                onClick={() => {
                                  // Use full-page redirect instead of popup to avoid popup blockers
                                  window.location.href =
                                    config.serverBaseUrl +
                                    "/LoginPage?useSAML=true&redirect=true";
                                }}
                              >
                                <FormattedMessage id="label.button.login.sso" />
                              </Button>
                            )}
                          {configurationProperties?.useOauth == "true" &&
                            renderOauthButtons()}
                        </Stack>
                      </Form>
                    </div>
                  )}
                </Formik>
              )}
            </Section>

            {loginMessage()}
          </Grid>
        </div>
      </div>
    </div>
  );
}

export default injectIntl(Login);
