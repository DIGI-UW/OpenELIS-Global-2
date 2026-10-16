import React, {
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
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

const LOGIN_LOGO_CACHE_KEY = "openelis.loginLogoUrl";

const readCachedLoginLogo = () => {
  try {
    const raw = sessionStorage.getItem(LOGIN_LOGO_CACHE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    if (parsed && typeof parsed.url === "string") {
      return { url: parsed.url, version: parsed.version ?? 0 };
    }
    return null;
  } catch {
    return null;
  }
};

const writeCachedLoginLogo = (url, version) => {
  try {
    if (url) {
      sessionStorage.setItem(
        LOGIN_LOGO_CACHE_KEY,
        JSON.stringify({ url, version }),
      );
    } else {
      sessionStorage.removeItem(LOGIN_LOGO_CACHE_KEY);
    }
  } catch {
    /* storage unavailable — non-fatal */
  }
};

function Login(props) {
  const { notificationVisible, addNotification, setNotificationVisible } =
    useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext);

  const { userSessionDetails, refresh } = useContext(UserSessionDetailsContext);
  const [submitting, setSubmitting] = useState(false);
  const [loginLogoUrl, setLoginLogoUrl] = useState(
    () => readCachedLoginLogo()?.url ?? null,
  );
  const [brandingResolved, setBrandingResolved] = useState(
    () => readCachedLoginLogo() !== null,
  );
  const [logoVersion, setLogoVersion] = useState(
    () => readCachedLoginLogo()?.version ?? 0,
  );
  const samlRedirectInitiatedRef = useRef(false);
  const shouldAutoRedirectToSaml =
    configurationProperties?.useSaml === "true" &&
    configurationProperties?.useSamlLoginPage === "false" &&
    !userSessionDetails.authenticated;
  const userIsActiveRef = useRef(false);
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
  const firstInput = useRef(null);

  useEffect(() => {
    if (shouldAutoRedirectToSaml && !samlRedirectInitiatedRef.current) {
      samlRedirectInitiatedRef.current = true;
      window.location.href =
        config.serverBaseUrl + "/LoginPage?useSAML=true&redirect=true";
    }
  }, [shouldAutoRedirectToSaml]);

  useEffect(() => {
    firstInput?.current?.focus();

    const interval = setInterval(() => {
      if (!userIsActiveRef.current) {
        refresh();
      }
    }, 1000 * 10);

    return () => {
      clearInterval(interval);
      if (activityResetTimerRef.current) {
        clearTimeout(activityResetTimerRef.current);
      }
    };
  }, [refresh]);

  useEffect(() => {
    getBranding((response) => {
      if (response === undefined) {
        setBrandingResolved(true);
        return;
      }

      let newUrl = null;
      if (response?.useHeaderLogoForLogin && response.headerLogoUrl) {
        newUrl = response.headerLogoUrl;
      } else if (response?.loginLogoUrl) {
        newUrl = response.loginLogoUrl;
      }

      if (!newUrl) {
        writeCachedLoginLogo(null, 0);
        setLoginLogoUrl(null);
        setBrandingResolved(true);
        return;
      }

      const newVersion = response.logoRevision ?? Date.now();
      writeCachedLoginLogo(newUrl, newVersion);
      setLoginLogoUrl(newUrl);
      setLogoVersion(newVersion);
      setBrandingResolved(true);
    });
  }, []);

  useEffect(() => {
    if (userSessionDetails.authenticated) {
      window.location.href = "/";
    }
  }, [userSessionDetails]);

  const loginMessage = () => {
    const logoSrc = loginLogoUrl
      ? `${config.serverBaseUrl}${loginLogoUrl}?v=${logoVersion}`
      : brandingResolved
        ? `images/openelis_logo_full.png`
        : null;

    return (
      <>
        <Column lg={6} md={0} sm={0} />
        <Column lg={4} md={8} sm={4}>
          {logoSrc ? (
            <picture>
              <img
                src={logoSrc}
                alt="fullsize logo"
                width="300"
                height="56"
                style={{ objectFit: "contain" }}
                onError={(e) => {
                  e.target.src = `images/openelis_logo_full.png`;
                }}
              />
            </picture>
          ) : (
            <div style={{ width: 300, height: 56 }} aria-hidden="true" />
          )}
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
                {shouldAutoRedirectToSaml ? (
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
                    onSubmit={(values) => doLogin(values)}
                  >
                    {({ isValid, handleChange, handleSubmit }) => (
                      <Form onSubmit={handleSubmit}>
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
                                name="loginName"
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
                                onChange={(event) => {
                                  markUserActive();
                                  handleChange(event);
                                }}
                              />
                              <PasswordInput
                                id="password"
                                name="password"
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
                                onChange={(event) => {
                                  markUserActive();
                                  handleChange(event);
                                }}
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
}

export default injectIntl(Login);
