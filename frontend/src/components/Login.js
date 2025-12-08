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
import { getBranding } from "../services/siteBrandingService";

function Login(props) {
  const { notificationVisible, addNotification, setNotificationVisible } =
    useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext);

  const { userSessionDetails, refresh } = useContext(UserSessionDetailsContext);
  const [submitting, setSubmitting] = useState(false);
  const [loginLogoUrl, setLoginLogoUrl] = useState(null);
  const [logoVersion, setLogoVersion] = useState(0); // Version counter for cache-busting
  const firstInput = createRef();

  useEffect(() => {
    firstInput?.current?.focus();

    const interval = setInterval(() => {
      checkLogin();
    }, 1000 * 3);

    return () => clearInterval(interval); // clear your interval to prevent memory leaks.
  }, []);

  // Load branding configuration for login logo
  // Task Reference: T080 - Fetch SiteBranding configuration on login page load
  useEffect(() => {
    getBranding((response) => {
      if (response) {
        // Task Reference: T041 - Check useHeaderLogoForLogin flag
        if (response.useHeaderLogoForLogin && response.headerLogoUrl) {
          // Use header logo for login page
          setLoginLogoUrl(response.headerLogoUrl);
          setLogoVersion(prev => prev + 1); // Increment version to force logo reload
        } else if (response.loginLogoUrl) {
          // Use dedicated login logo
          setLoginLogoUrl(response.loginLogoUrl);
          setLogoVersion(prev => prev + 1); // Increment version to force logo reload
        }
        // If neither exists, loginLogoUrl remains null and default logo will be used

        // Task Reference: T080 - Apply custom primary color to login page UI elements
        if (response.primaryColor) {
          document.documentElement.style.setProperty('--cds-interactive-01', response.primaryColor);
          document.documentElement.style.setProperty('--site-branding-primary', response.primaryColor);
        }
        if (response.secondaryColor) {
          document.documentElement.style.setProperty('--cds-interactive-02', response.secondaryColor);
          document.documentElement.style.setProperty('--site-branding-secondary', response.secondaryColor);
        }
        if (response.accentColor) {
          document.documentElement.style.setProperty('--cds-support-01', response.accentColor);
          document.documentElement.style.setProperty('--site-branding-accent', response.accentColor);
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
    // Task Reference: T041 - Use custom login logo if available, otherwise default
    // Add cache-busting parameter to prevent stale logo display after upload
    const logoSrc = loginLogoUrl 
      ? `../api${loginLogoUrl}?v=${logoVersion}` 
      : `images/openelis_logo_full.png`;
    
    return (
      <>
        <Column lg={6} md={0} sm={0} />
        <Column lg={4} md={8} sm={4}>
          <picture>
            <img
              src={logoSrc}
              alt="fullsize logo"
              width="300"
              height="56"
              style={{ objectFit: "contain" }}
              onError={(e) => {
                // Fallback to default logo if custom logo fails to load
                e.target.src = `images/openelis_logo_full.png`;
              }}
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
                    .catch((error) => {
                      console.error(error);
                    });
                }}
              >
                {({ isValid, handleChange, handleSubmit }) => (
                  <Form onSubmit={handleSubmit} onChange={handleChange}>
                    <Stack gap={5}>
                      <FormLabel>
                        <Heading>
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
                          />
                          <TextInput.PasswordInput
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
                      {configurationProperties?.useSaml == "true" && (
                        <Button
                          type="button"
                          renderIcon={HardwareSecurityModule}
                          onClick={() => {
                            const POPUP_HEIGHT = 700;
                            const POPUP_WIDTH = 600;
                            const top =
                              window.outerHeight / 2 +
                              window.screenY -
                              POPUP_HEIGHT / 2;
                            const left =
                              window.outerWidth / 2 +
                              window.screenX -
                              POPUP_WIDTH / 2;
                            window.open(
                              config.serverBaseUrl + "/LoginPage?useSAML=true",
                              "SAML Popup",
                              `height=${POPUP_HEIGHT},width=${POPUP_WIDTH},top=${top},left=${left}`,
                            );
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
