import React, { useContext, useEffect, useState, createRef } from "react";
import config from "../config.json";
import "./Style.css";
import qs from "qs";
import { FormattedMessage, injectIntl, IntlShape } from "react-intl";
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
import { getBranding } from "./utils/BrandingUtils";

interface LoginProps {
  intl: IntlShape;
}

const Login: React.FC<LoginProps> = (props) => {
  const { notificationVisible, addNotification, setNotificationVisible } =
    useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext);

  const { userSessionDetails, refresh } = useContext(UserSessionDetailsContext);
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [samlRedirectInitiated, setSamlRedirectInitiated] =
    useState<boolean>(false);
  const [loginLogoUrl, setLoginLogoUrl] = useState<string | null>(null);
  const [logoVersion, setLogoVersion] = useState<number>(0);
  const firstInput = createRef<HTMLInputElement>();

  useEffect(() => {
    if (
      configurationProperties?.useSaml === "true" &&
      configurationProperties?.useSamlLoginPage === "false" &&
      !samlRedirectInitiated &&
      !userSessionDetails.authenticated
    ) {
      setSamlRedirectInitiated(true);
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

    const interval = setInterval(() => {
      checkLogin();
    }, 1000 * 3);

    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    getBranding((response: any) => {
      if (response) {
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
    const logoSrc = loginLogoUrl
      ? `${config.serverBaseUrl}${loginLogoUrl}?v=${logoVersion}`
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
              onError={(e: any) => {
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

  const doLogin = (data: any) => {
    setSubmitting(true);
    fetch(config.serverBaseUrl + "/ValidateLogin?apiCall=true", {
      credentials: "include",
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
      },
      body: qs.stringify(data),
    })
      .then(async (response) => {
        setSubmitting(false);
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
        {configurationProperties?.oauthUrls?.map((url: any) => (
          <Button
            key={url.key}
            type="button"
            renderIcon={HardwareSecurityModule as any}
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
    <>
      <div
        data-cy="login-Page-Content"
        className="loginPageContent oe-loginPageContent"
      >
        {notificationVisible === true ? <AlertDialog /> : ""}
        <div className="oe-loginPageCenter">
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
                      username: "",
                      password: "",
                    }}
                    onSubmit={(values) => {
                      doLogin(values);
                      fetch(config.serverBaseUrl + "/LoginPage", {
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
                                renderIcon={HardwareSecurityModule as any}
                                onClick={() => {
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
      </div>
    </>
  );
};

export default injectIntl(Login as React.ComponentType<any>);
