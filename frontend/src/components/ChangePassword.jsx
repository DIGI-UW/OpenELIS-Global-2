import React, { useContext, useState } from "react";
import config from "../config.json";
import qs from "qs";
import {
  Button,
  Column,
  Form,
  FormLabel,
  Grid,
  Heading,
  ListItem,
  Loading,
  PasswordInput,
  Section,
  Stack,
  TextInput,
  UnorderedList,
} from "@carbon/react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import { Formik } from "formik";
import * as Yup from "yup";
import { AlertDialog, NotificationKinds } from "./common/CustomNotification";
import { NotificationContext } from "./layout/Layout";
import UserSessionDetailsContext from "../UserSessionDetailsContext";

function ChangePassword() {
  const intl = useIntl();
  const { notificationVisible, addNotification, setNotificationVisible } =
    useContext(NotificationContext);
  const { userSessionDetails, logout } = useContext(UserSessionDetailsContext);
  const [submitting, setSubmitting] = useState(false);

  const changePassword = (values) => {
    setSubmitting(true);
    // apiCall=true makes the backend answer with an explicit JSON status; the
    // legacy redirect response is indistinguishable from a security bounce
    fetch(config.serverBaseUrl + "/ChangePasswordLogin?apiCall=true", {
      method: "POST",
      credentials: "include",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
      },
      body: qs.stringify(values),
    })
      .then(async (response) => {
        const data = await response.json();
        setSubmitting(false);
        if (response.status === 200) {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "notification.password.change.success",
            }),
          });
          setNotificationVisible(true);
          setTimeout(() => {
            if (userSessionDetails.authenticated) {
              logout();
            } else {
              window.location.href = "/login";
            }
          }, 2000);
        } else {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id:
                data.error && intl.messages[data.error]
                  ? data.error
                  : "notification.password.change.fail",
            }),
          });
          setNotificationVisible(true);
        }
      })
      .catch((error) => {
        addNotification({
          kind: NotificationKinds.error,
          title: intl.formatMessage({ id: "notification.title" }),
          message: error.message,
        });
        setNotificationVisible(true);
        setSubmitting(false);
      });
  };

  const LoginComlexityMessage = () => {
    return (
      <>
        <h5>
          <FormattedMessage id="login.complexity.new.message" />
        </h5>
        <br />
        <h6>
          <UnorderedList nested={true}>
            <ListItem>
              <FormattedMessage id="login.complexity.message.1" />
            </ListItem>
            <ListItem>
              <FormattedMessage id="login.complexity.message.2" />
            </ListItem>
            <ListItem>
              <FormattedMessage id="login.complexity.message.3" />
            </ListItem>
            <ListItem>
              <FormattedMessage id="login.complexity.message.4" />
            </ListItem>
            <ListItem>
              <FormattedMessage id="login.complexity.new.message.1" />
            </ListItem>
          </UnorderedList>
        </h6>
      </>
    );
  };

  return (
    <>
      <div className="changePasswordPage">
        {notificationVisible === true ? <AlertDialog /> : ""}
        <Grid fullWidth={true}>
          <Column lg={0} md={0} sm={4}>
            <LoginComlexityMessage />
          </Column>
          <Column lg={6} md={4} sm={4}>
            <Section>
              <Formik
                initialValues={{
                  loginName: "",
                  password: "",
                  newPassword: "",
                  confirmPassword: "",
                }}
                onSubmit={(values) => {
                  changePassword(values);
                }}
                validationSchema={Yup.object().shape({
                  loginName: Yup.string().required(
                    intl.formatMessage({ id: "validation.field.required" }),
                  ),
                  password: Yup.string().required(
                    intl.formatMessage({ id: "validation.field.required" }),
                  ),
                  newPassword: Yup.string()
                    .required(
                      intl.formatMessage({ id: "validation.field.required" }),
                    )
                    .min(
                      7,
                      intl.formatMessage({
                        id: "validation.password.minLength",
                      }),
                    )
                    .matches(
                      // matches backend complexity (HaitiPasswordValidation):
                      // 7+ chars of letters/digits/*$#! with >=1 special char
                      /^(?=.*[*$#!])[A-Za-z0-9*$#!]{7,}$/,
                      intl.formatMessage({
                        id: "validation.password.specialChar",
                      }),
                    )
                    .test(
                      "not-same-as-old",
                      intl.formatMessage({
                        id: "validation.password.notSameAsOld",
                      }),
                      function (value) {
                        return value !== this.parent.password; // compare newPassword to old password
                      },
                    ),
                  confirmPassword: Yup.string()
                    .required(
                      intl.formatMessage({ id: "validation.field.required" }),
                    )
                    .oneOf(
                      [Yup.ref("newPassword"), null],
                      intl.formatMessage({ id: "validation.password.match" }),
                    )
                    .min(
                      7,
                      intl.formatMessage({
                        id: "validation.password.minLength",
                      }),
                    )
                    .matches(
                      /^(?=.*[*$#!])[A-Za-z0-9*$#!]{7,}$/,
                      intl.formatMessage({
                        id: "validation.password.specialChar",
                      }),
                    )
                    .test(
                      "not-same-as-old",
                      intl.formatMessage({
                        id: "validation.password.notSameAsOld",
                      }),
                      function (value) {
                        return value !== this.parent.password; // compare newPassword to old password
                      },
                    ),
                })}
              >
                {(formik) => (
                  <Form
                    onSubmit={formik.handleSubmit}
                    onChange={formik.handleChange}
                  >
                    <Stack gap={5}>
                      <FormLabel>
                        <Heading>
                          <FormattedMessage id="label.button.changepassword" />
                        </Heading>
                      </FormLabel>
                      <TextInput
                        id="loginName"
                        name="loginName"
                        autoComplete="username"
                        labelText={intl.formatMessage({
                          id: "login.msg.username",
                        })}
                        hideLabel={true}
                        placeholder={intl.formatMessage({
                          id: "login.msg.username",
                        })}
                        required={true}
                        onBlur={formik.handleBlur}
                        invalid={
                          formik.touched.loginName && !!formik.errors.loginName
                        }
                        invalidText={formik.errors.loginName}
                      />
                      <PasswordInput
                        id="current-password"
                        name="password"
                        autoComplete="current-password"
                        labelText={intl.formatMessage({
                          id: "login.login.current.password",
                        })}
                        hideLabel={true}
                        placeholder={intl.formatMessage({
                          id: "login.login.current.password",
                        })}
                        required={true}
                        onBlur={formik.handleBlur}
                        invalid={
                          formik.touched.password && !!formik.errors.password
                        }
                        invalidText={formik.errors.password}
                      />
                      <br />
                      <PasswordInput
                        id="new-password"
                        name="newPassword"
                        autoComplete="new-password"
                        labelText={intl.formatMessage({
                          id: "login.login.new.password",
                        })}
                        hideLabel={true}
                        placeholder={intl.formatMessage({
                          id: "login.login.new.password",
                        })}
                        required={true}
                        onBlur={formik.handleBlur}
                        invalid={
                          formik.touched.newPassword &&
                          !!formik.errors.newPassword
                        }
                        invalidText={formik.errors.newPassword}
                      />
                      <PasswordInput
                        id="repeat-new-password"
                        name="confirmPassword"
                        autoComplete="new-password"
                        labelText={intl.formatMessage({
                          id: "login.login.repeat.password",
                        })}
                        hideLabel={true}
                        placeholder={intl.formatMessage({
                          id: "login.login.repeat.password",
                        })}
                        required={true}
                        onBlur={formik.handleBlur}
                        invalid={
                          formik.touched.confirmPassword &&
                          !!formik.errors.confirmPassword
                        }
                        invalidText={formik.errors.confirmPassword}
                      />
                      <Stack orientation="horizontal">
                        <Button
                          data-cy="submitNewPassword"
                          type="submit"
                          disabled={!formik.isValid}
                        >
                          <FormattedMessage id="label.button.submit" />
                          <Loading
                            small={true}
                            withOverlay={false}
                            className={submitting ? "show" : "hidden"}
                          />
                        </Button>
                        <Button
                          data-cy="exitPasswordReset"
                          kind="secondary"
                          onClick={() => {
                            window.location.href = "/";
                          }}
                        >
                          <FormattedMessage id="label.button.exit" />
                        </Button>
                      </Stack>
                    </Stack>
                  </Form>
                )}
              </Formik>
            </Section>
          </Column>
          <Column lg={10} md={4} sm={0}>
            <LoginComlexityMessage />
          </Column>
        </Grid>
      </div>
    </>
  );
}

export default injectIntl(ChangePassword);
