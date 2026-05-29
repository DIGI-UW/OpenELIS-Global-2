import React, { useContext, useEffect, useState, createRef } from "react";
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
  Button,
  Stack,
  Loading,
} from "@carbon/react";
import { Formik } from "formik";
import { AlertDialog, NotificationKinds } from "./common/CustomNotification";
import UserSessionDetailsContext from "../UserSessionDetailsContext";
import { ConfigurationContext, NotificationContext } from "./layout/Layout";

function Login(props) {
  const { notificationVisible, addNotification, setNotificationVisible } =
    useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext);

  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const [submitting, setSubmitting] = useState(false);
  const [samlRedirectInitiated, setSamlRedirectInitiated] = useState(false);
  const firstInput = createRef();
  const showFormLogin = configurationProperties?.useFormLogin !== "false";

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
  }, []);

  useEffect(() => {
    if (userSessionDetails.authenticated) {
      window.location.href = "/";
    }
  }, [userSessionDetails]);

  const loginMessage = () => {
    return (
      <>
        <Column lg={6} md={0} sm={0} />
        <Column lg={4} md={8} sm={4}>
          <picture>
            <img
              src="images/ahri_logo_full.png"
              alt="AHRI LIMS"
              width="320"
              style={{ maxWidth: "100%", height: "auto" }}
            />
          </picture>
        </Column>
        <Column lg={6} md={0} sm={0} />
        <Column lg={6} md={0} sm={0} />
        <Column lg={4} md={8} sm={4}>
          <FormattedMessage id="login.notice.message" />
        </Column>
        <Column lg={6} md={0} sm={0} />
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
        let responseData = null;

        // Some backend/proxy setups return redirects/HTML instead of JSON.
        // Keep the login flow resilient by only parsing JSON when possible.
        try {
          responseData = await response.json();
        } catch (jsonError) {
          responseData = null;
        }

        if (response.status === 200) {
          window.location.href = "/";
          return;
        }

        // Legacy behavior: backend may return 302 for login POST.
        // Verify current session before deciding it is an error.
        if (response.status === 302 || response.redirected || !responseData) {
          const sessionResponse = await fetch(config.serverBaseUrl + "/session", {
            credentials: "include",
          });

          if (sessionResponse.status === 200) {
            const sessionData = await sessionResponse.json();
            if (sessionData.authenticated) {
              window.location.href = "/";
              return;
            }
          }
        }

        if (responseData?.error) {
          addNotification({
            title: props.intl.formatMessage({
              id: "notification.title",
            }),
            message: props.intl.formatMessage({
              id: responseData.error,
            }),
            kind: NotificationKinds.error,
          });
          setNotificationVisible(true);
          return;
        }

        addNotification({
          title: props.intl.formatMessage({
            id: "notification.title",
          }),
          message: props.intl.formatMessage({
            id: "error.invalidcredentials",
          }),
          kind: NotificationKinds.error,
        });
        setNotificationVisible(true);
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
              console.log(url);
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
    <>
      <div className="loginTopBar" data-cy="login-top-bar">
        <img
          src="images/ahri_logo.png"
          alt="AHRI LIMS"
          className="loginTopBarLogo"
        />
        <span className="loginTopBarTitle">AHRI LIMS</span>
      </div>
      <div data-cy="login-Page-Content" className="loginPageContent">
        {notificationVisible === true ? <AlertDialog /> : ""}
        <Grid fullWidth={true}>{loginMessage()}</Grid>
        <Grid fullWidth={false}>
          <Column lg={16}>
            <br />
            <br />
          </Column>
          <Column lg={6} md={0} sm={0} />
          <Column lg={4} md={8} sm={4}>
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
                    loginName: "",
                    password: "",
                  }}
                  onSubmit={(values) => {
                    fetch(config.serverBaseUrl + "/LoginPage", {
                      //includes the browser sessionId in the Header for Authentication on the backend server
                      credentials: "include",
                      method: "GET",
                    })
                      .then((response) => response.status)
                      .then(() => {
                        doLogin(values);
                      })
                      .catch((error) => {
                        console.error(error);
                      });
                  }}
                >
                  {({ isValid, values, handleChange, handleSubmit }) => (
                    <Form onSubmit={handleSubmit}>
                      <Stack gap={5}>
                        <FormLabel>
                          <Heading>
                            <FormattedMessage id="login.title" />
                          </Heading>
                        </FormLabel>
                        {showFormLogin && (
                          <>
                            <TextInput
                              id="loginName"
                              name="loginName"
                              value={values.loginName}
                              onChange={handleChange}
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
                            />
                            <TextInput.PasswordInput
                              id="password"
                              name="password"
                              value={values.password}
                              onChange={handleChange}
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
                                  window.location.href = "/ChangePasswordLogin";
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
                  )}
                </Formik>
              )}
            </Section>
          </Column>
          <Column lg={6} md={0} sm={0} />
          <Column lg={0} md={0} sm={0}>
            {loginMessage()}
          </Column>
        </Grid>
      </div>
    </>
  );
}

export default injectIntl(Login);
