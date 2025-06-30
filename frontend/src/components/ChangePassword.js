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

function ChangePassword() {
  const intl = useIntl();
  const { notificationVisible, addNotification, setNotificationVisible } =
    useContext(NotificationContext);
  const [submitting, setSubmitting] = useState(false);

  const changePassword = (values) => {
    setSubmitting(true);
    // Call the API to change the password show success or error notification on success redirect to the login page
    fetch(config.serverBaseUrl + "/ChangePasswordLogin", {
      method: "POST",
      credentials: "include",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
      },
      body: qs.stringify(values),
    })
      .then((response) => {
        console.log(response);
        if (response.redirected === true) {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "notification.password.change.success",
            }),
          });
          setNotificationVisible(true);
          setTimeout(() => {
            window.location.href = "/login";
          }, 2000);
        } else {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "notification.password.change.fail",
            }),
          });
          setNotificationVisible(true);
        }
        setSubmitting(false);
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
                  loginName: Yup.string().required(),
                  password: Yup.string().required(),
                  newPassword: Yup.string()
                    .required()
                    .min(7, "Password must be at least 7 characters")
                    .matches(
                      /^(?=.*[*$#!])(?=.*[A-Z])[A-Za-z0-9*$#!]{7,}$/,
                      "Password must contain at least one special character",
                    )
                    .test(
                      "not-same-as-old",
                      "New password must not be the same as the old password",
                      function (value) {
                        return value !== this.parent.password; // compare newPassword to old password
                      },
                    ),
                  confirmPassword: Yup.string()
                    .required()
                    .oneOf(
                      [Yup.ref("newPassword"), null],
                      "Passwords must match",
                    )
                    .min(7, "Password must be at least 7 characters")
                    .matches(
                      /^(?=.*[*$#!])(?=.*[A-Z])[A-Za-z0-9*$#!]{7,}$/,
                      "Password must contain at least one special character",
                    )
                    .test(
                      "not-same-as-old",
                      "New password must not be the same as the old password",
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
                        labelText={intl.formatMessage({
                          id: "login.msg.username",
                        })}
                        hideLabel={true}
                        placeholder={intl.formatMessage({
                          id: "login.msg.username",
                        })}
                        required={true}
                      />
                      <PasswordInput
                        id="current-password"
                        name="password"
                        labelText={intl.formatMessage({
                          id: "login.login.current.password",
                        })}
                        hideLabel={true}
                        placeholder={intl.formatMessage({
                          id: "login.login.current.password",
                        })}
                        required={true}
                      />
                      <br />
                      <PasswordInput
                        id="new-password"
                        name="newPassword"
                        labelText={intl.formatMessage({
                          id: "login.login.new.password",
                        })}
                        hideLabel={true}
                        placeholder={intl.formatMessage({
                          id: "login.login.new.password",
                        })}
                        required={true}
                        // If only new password is not valid according to validation schema, make the vield invalid
                        onBlur={formik.handleBlur}
                        invalid={
                          formik.errors.newPassword &&
                          formik.touched.newPassword
                        }
                      />
                      <PasswordInput
                        id="repeat-new-password"
                        name="confirmPassword"
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
                          formik.values.newPassword !==
                            formik.values.confirmPassword
                        }
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
