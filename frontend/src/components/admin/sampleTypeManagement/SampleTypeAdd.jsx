import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Heading,
  Button,
  Loading,
  Grid,
  Column,
  Section,
  TextInput,
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { Formik, Form } from "formik";
import * as Yup from "yup";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "configuration.sampleType.manage",
    link: "/MasterListsPage/SampleTypeManagement",
  },
  {
    label: "configuration.sampleType.create",
    link: "/MasterListsPage/SampleTypeAdd",
  },
];

function SampleTypeAdd() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();
  const [isLoading, setIsLoading] = useState(true);
  const [bothFilled, setBothFilled] = useState(false);
  const [sampleTypeCreateList, setSampleTypeCreateList] = useState({});

  const componentMounted = useRef(false);

  const handleSampleTypeCreateList = (res) => {
    if (!res) {
      setIsLoading(true);
    } else {
      setSampleTypeCreateList(res);
    }
  };

  const handleSampleTypeCreateListCall = ({
    englishLangPost,
    frenchLangPost,
  }) => {
    postToOpenElisServerJsonResponse(
      "/rest/SampleTypeCreate",
      JSON.stringify({
        sampleTypeEnglishName: englishLangPost,
        sampleTypeFrenchName: frenchLangPost,
      }),
      (res) => {
        handlePostSampleTypeCreateListCallBack(res);
      },
    );
  };

  const handlePostSampleTypeCreateListCallBack = (res) => {
    if (res) {
      if (res) {
        setIsLoading(false);
        addNotification({
          title: intl.formatMessage({
            id: "notification.title",
          }),
          message: intl.formatMessage({
            id: "notification.user.post.delete.success",
          }),
          kind: NotificationKinds.success,
        });
        setTimeout(() => {
          window.location.reload();
        }, 200);
        setNotificationVisible(true);
      }
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
    }
  };

  const allSampleTypeValues = [
    ...(sampleTypeCreateList?.existingSampleTypeList || []).map((item) =>
      item?.value?.trim().toLowerCase(),
    ),
    ...(sampleTypeCreateList?.inactiveSampleTypeList || []).map((item) =>
      item?.value?.trim().toLowerCase(),
    ),
  ];

  const validateSampleType = (name) => {
    if (!name) return false;
    return allSampleTypeValues.includes(name.trim().toLowerCase());
  };

  useEffect(() => {
    componentMounted.current = true;
    setIsLoading(true);
    getFromOpenElisServer(`/rest/SampleTypeCreate`, handleSampleTypeCreateList);
    return () => {
      componentMounted.current = false;
      setIsLoading(false);
    };
  }, []);

  const validationSchema = Yup.object({
    englishLangPost: Yup.string()
      .required("fill this field")
      .test(
        "duplicate-check",
        intl.formatMessage({ id: "configuration.sampleType.create.duplicate" }),
        (value) => !validateSampleType(value),
      )
      .trim(),
    frenchLangPost: Yup.string()
      .required("fill this field")
      .test(
        "duplicate-check",
        intl.formatMessage({ id: "configuration.sampleType.create.duplicate" }),
        (value) => !validateSampleType(value),
      )
      .trim(),
  });

  if (!isLoading) {
    return (
      <>
        <Loading />
      </>
    );
  }

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <div className="orderLegendBody" style={{ padding: "2rem" }}>
          <Grid fullWidth={true} style={{ marginBottom: "2rem" }}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Heading size="lg" style={{ marginBottom: "1rem" }}>
                  <FormattedMessage id="configuration.sampleType.create" />
                </Heading>
                <p style={{ color: "#6f6f6f", marginBottom: "1.5rem" }}>
                  <FormattedMessage id="configuration.sampleType.confirmation.explain" />
                </p>
              </Section>
            </Column>
          </Grid>

          <Grid fullWidth={true} style={{ marginBottom: "2rem" }}>
            <Column lg={16} md={8} sm={4}>
              <Section style={{
                backgroundColor: "#f4f4f4",
                padding: "2rem",
                borderRadius: "8px",
                border: "1px solid #e0e0e0"
              }}>
                <Formik
                  initialValues={{ englishLangPost: "", frenchLangPost: "" }}
                  validationSchema={validationSchema}
                  onSubmit={(values, actions) => {
                    if (bothFilled) {
                      handleSampleTypeCreateListCall(values);
                    } else {
                      setBothFilled(true);
                      actions.setSubmitting(false);
                    }
                  }}
                >
                  {({
                    values,
                    errors,
                    touched,
                    handleChange,
                    handleBlur,
                    handleSubmit,
                    isSubmitting,
                  }) => (
                    <Form onSubmit={handleSubmit}>
                      <Grid fullWidth={true} style={{ marginBottom: "1.5rem" }}>
                        <Column lg={6} md={4} sm={4} style={{ display: "flex", alignItems: "center", paddingRight: "1rem" }}>
                          <label style={{ fontWeight: "500" }}>
                            <FormattedMessage id="english.label" />
                            <span className="requiredlabel">*</span> :
                          </label>
                        </Column>
                        <Column lg={10} md={4} sm={4}>
                          <TextInput
                            id="eng"
                            name="englishLangPost"
                            labelText=""
                            hideLabel
                            placeholder="Enter sample type name in English"
                            disabled={bothFilled}
                            value={values.englishLangPost}
                            onChange={handleChange}
                            onBlur={handleBlur}
                            required
                            invalid={touched.englishLangPost && !!errors.englishLangPost}
                            invalidText={touched.englishLangPost && errors.englishLangPost}
                          />
                        </Column>
                      </Grid>

                      <Grid fullWidth={true} style={{ marginBottom: "2rem" }}>
                        <Column lg={6} md={4} sm={4} style={{ display: "flex", alignItems: "center", paddingRight: "1rem" }}>
                          <label style={{ fontWeight: "500" }}>
                            <FormattedMessage id="french.label" />
                            <span className="requiredlabel">*</span> :
                          </label>
                        </Column>
                        <Column lg={10} md={4} sm={4}>
                          <TextInput
                            id="fr"
                            name="frenchLangPost"
                            labelText=""
                            hideLabel
                            placeholder="Enter sample type name in French"
                            disabled={bothFilled}
                            value={values.frenchLangPost}
                            onChange={handleChange}
                            onBlur={handleBlur}
                            required
                            invalid={touched.frenchLangPost && !!errors.frenchLangPost}
                            invalidText={touched.frenchLangPost && errors.frenchLangPost}
                          />
                        </Column>
                      </Grid>

                      {bothFilled && (
                        <div style={{
                          backgroundColor: "#f0f8ff",
                          padding: "0.75rem 1rem",
                          borderRadius: "4px",
                          border: "1px solid #d0e2ff",
                          marginBottom: "1.5rem",
                          display: "flex",
                          alignItems: "center",
                          gap: "0.5rem"
                        }}>
                          <span style={{
                            backgroundColor: "#0066cc",
                            color: "white",
                            borderRadius: "50%",
                            width: "16px",
                            height: "16px",
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            fontSize: "12px",
                            fontWeight: "bold",
                            flexShrink: 0
                          }}>
                            i
                          </span>
                          <p style={{
                            color: "#0066cc",
                            margin: 0,
                            fontSize: "0.875rem",
                            lineHeight: "1.4"
                          }}>
                            <FormattedMessage id="configuration.sampleType.confirmation.explain" />
                          </p>
                        </div>
                      )}

                      <div style={{ display: "flex", gap: "1rem" }}>
                        <Button
                          disabled={isSubmitting}
                          type="submit"
                          kind="primary"
                        >
                          {bothFilled ? (
                            <FormattedMessage id="accept.action.button" />
                          ) : (
                            <FormattedMessage id="next.action.button" />
                          )}
                        </Button>

                        <Button
                          type="button"
                          kind="secondary"
                          onClick={() => {
                            window.location.reload();
                          }}
                        >
                          {bothFilled ? (
                            <FormattedMessage id="reject.action.button" />
                          ) : (
                            <FormattedMessage id="label.button.previous" />
                          )}
                        </Button>
                      </div>
                    </Form>
                  )}
                </Formik>
              </Section>
            </Column>
          </Grid>

          {/* Existing Sample Types */}
          <Grid fullWidth={true} style={{ marginTop: "3rem", marginBottom: "2rem" }}>
            <Column lg={16} md={8} sm={4}>
              <Section style={{
                backgroundColor: "#f0f8ff",
                padding: "2rem",
                borderRadius: "12px",
                border: "1px solid #d0e2ff",
                boxShadow: "0 2px 4px rgba(0, 0, 0, 0.05)"
              }}>
                <div style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "0.75rem",
                  marginBottom: "1rem"
                }}>
                  <div style={{
                    backgroundColor: "#0066cc",
                    color: "white",
                    borderRadius: "50%",
                    width: "24px",
                    height: "24px",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    fontSize: "12px",
                    fontWeight: "bold"
                  }}>
                    ✓
                  </div>
                  <div>
                    <Heading size="md" style={{ margin: 0, color: "#0066cc" }}>
                      <FormattedMessage id="sampleType.existing" />
                    </Heading>
                    <p style={{
                      margin: "0.25rem 0 0 0",
                      fontSize: "0.8rem",
                      color: "#6b7280"
                    }}>
                      Active sample types with tests assigned
                    </p>
                  </div>
                </div>

                <div style={{
                  display: "flex",
                  flexWrap: "wrap",
                  gap: "0.5rem",
                  maxHeight: "180px",
                  overflowY: "auto",
                  padding: "1rem",
                  backgroundColor: "white",
                  borderRadius: "8px",
                  border: "1px solid #e0e0e0"
                }}>
                  {sampleTypeCreateList?.existingSampleTypeList?.length > 0 ? (
                    sampleTypeCreateList.existingSampleTypeList.map((stl, index) => (
                      <span
                        key={index}
                        style={{
                          backgroundColor: "#e0f2fe",
                          color: "#0066cc",
                          padding: "0.4rem 0.8rem",
                          borderRadius: "20px",
                          fontSize: "0.8rem",
                          border: "1px solid #b3d9ff",
                          fontWeight: "500",
                          display: "flex",
                          alignItems: "center",
                          gap: "0.25rem"
                        }}
                      >
                        <span style={{ fontSize: "10px", color: "#0066cc" }}>●</span>
                        {stl?.value}
                      </span>
                    ))
                  ) : (
                    <p style={{
                      color: "#9ca3af",
                      fontStyle: "italic",
                      margin: "1rem 0",
                      textAlign: "center",
                      width: "100%"
                    }}>
                      No active sample types found
                    </p>
                  )}
                </div>
              </Section>
            </Column>
          </Grid>

          {/* Inactive Sample Types */}
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section style={{
                backgroundColor: "#fffbeb",
                padding: "2rem",
                borderRadius: "12px",
                border: "1px solid #fed7aa",
                boxShadow: "0 2px 4px rgba(0, 0, 0, 0.05)"
              }}>
                <div style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "0.75rem",
                  marginBottom: "1rem"
                }}>
                  <div style={{
                    backgroundColor: "#f59e0b",
                    color: "white",
                    borderRadius: "50%",
                    width: "24px",
                    height: "24px",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    fontSize: "12px",
                    fontWeight: "bold"
                  }}>
                    ⚠
                  </div>
                  <div>
                    <Heading size="md" style={{ margin: 0, color: "#d97706" }}>
                      <FormattedMessage id="sampleType.existing.inactive" />
                    </Heading>
                    <p style={{
                      margin: "0.25rem 0 0 0",
                      fontSize: "0.8rem",
                      color: "#6b7280"
                    }}>
                      Assign tests to activate these sample types
                    </p>
                  </div>
                </div>

                <div style={{
                  display: "flex",
                  flexWrap: "wrap",
                  gap: "0.5rem",
                  maxHeight: "180px",
                  overflowY: "auto",
                  padding: "1rem",
                  backgroundColor: "white",
                  borderRadius: "8px",
                  border: "1px solid #e0e0e0"
                }}>
                  {sampleTypeCreateList?.inactiveSampleTypeList?.length > 0 ? (
                    sampleTypeCreateList.inactiveSampleTypeList.map((stl, index) => (
                      <span
                        key={index}
                        style={{
                          backgroundColor: "#fef3c7",
                          color: "#d97706",
                          padding: "0.4rem 0.8rem",
                          borderRadius: "20px",
                          fontSize: "0.8rem",
                          border: "1px solid #fcd34d",
                          fontWeight: "500",
                          display: "flex",
                          alignItems: "center",
                          gap: "0.25rem",
                          opacity: "0.85"
                        }}
                      >
                        <span style={{ fontSize: "10px", color: "#f59e0b" }}>○</span>
                        {stl?.value}
                      </span>
                    ))
                  ) : (
                    <p style={{
                      color: "#9ca3af",
                      fontStyle: "italic",
                      margin: "1rem 0",
                      textAlign: "center",
                      width: "100%"
                    }}>
                      No inactive sample types found
                    </p>
                  )}
                </div>
              </Section>
            </Column>
          </Grid>
        </div>
      </div>
    </>
  );
}

export default injectIntl(SampleTypeAdd);