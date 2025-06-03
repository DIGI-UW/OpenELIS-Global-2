import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Heading,
  Button,
  Loading,
  Grid,
  Column,
  Section,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableBody,
  TableHeader,
  TableCell,
  TableSelectRow,
  TableSelectAll,
  TableContainer,
  Pagination,
  Search,
  Select,
  SelectItem,
  Stack,
  TextInput,
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerFormData,
  postToOpenElisServerFullResponse,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils.js";
import { NotificationContext } from "../../layout/Layout.js";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification.js";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb.js";
import CustomCheckBox from "../../common/CustomCheckBox.js";
import ActionPaginationButtonType from "../../common/ActionPaginationButtonType.js";
import { Formik, Form } from "formik";
import * as Yup from "yup";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "master.lists.page.test.management",
    link: "/MasterListsPage#testManagementConfigMenu",
  },
  {
    label: "configuration.testUnit.manage",
    link: "/MasterListsPage#TestSectionManagement",
  },
  {
    label: "configuration.testUnit.create",
    link: "/MasterListsPage#TestSectionCreate",
  },
];

function TestSectionCreate() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();
  const [isLoading, setIsLoading] = useState(true);
  const [bothFilled, setBothFilled] = useState(false);
  const [testSectionCreateList, setTestSectionCreateList] = useState({});

  const componentMounted = useRef(false);

  const handleTestSectionCreateList = (res) => {
    if (!res) {
      setIsLoading(true);
    } else {
      setTestSectionCreateList(res);
    }
  };

  const handleTestSectionCreateListCall = ({
    englishLangPost,
    frenchLangPost,
  }) => {
    postToOpenElisServerJsonResponse(
      "/rest/TestSectionCreate",
      JSON.stringify({
        testUnitEnglishName: englishLangPost,
        testUnitFrenchName: frenchLangPost,
      }),
      (res) => {
        handlePostTestSectionCreateListCallBack(res);
      },
    );
  };

  const handlePostTestSectionCreateListCallBack = (res) => {
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
    ...(testSectionCreateList?.existingTestUnitList || []).map((item) =>
      item?.value?.trim().toLowerCase(),
    ),
    ...(testSectionCreateList?.inactiveTestUnitList || []).map((item) =>
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
    getFromOpenElisServer(
      `/rest/TestSectionCreate`,
      handleTestSectionCreateList,
    );
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
        intl.formatMessage({ id: "configuration.testUnit.create.duplicate" }),
        (value) => !validateSampleType(value),
      )
      .trim(),
    frenchLangPost: Yup.string()
      .required("fill this field")
      .test(
        "duplicate-check",
        intl.formatMessage({ id: "configuration.testUnit.create.duplicate" }),
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
        <div className="orderLegendBody">
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Heading>
                  <FormattedMessage id="banner.menu.patientEdit" />
                </Heading>
              </Section>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Heading>
                  <FormattedMessage id="configuration.testUnit.create" />
                </Heading>
              </Section>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Section>
                  <Section>
                    <Heading>
                      <FormattedMessage id="testUnit.new" />
                    </Heading>
                  </Section>
                </Section>
              </Section>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
          <Formik
            initialValues={{ englishLangPost: "", frenchLangPost: "" }}
            validationSchema={validationSchema}
            onSubmit={(values, actions) => {
              if (bothFilled) {
                handleTestSectionCreateListCall(values);
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
                <Grid fullWidth={true}>
                  <Column lg={8} md={4} sm={4}>
                    <>
                      <FormattedMessage id="english.label" />
                      <span className="requiredlabel">*</span> :
                    </>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      id={`eng`}
                      name="englishLangPost"
                      labelText=""
                      hideLabel
                      disabled={bothFilled}
                      value={values.englishLangPost}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      required
                      invalid={
                        touched.englishLangPost && !!errors.englishLangPost
                      }
                      invalidText={
                        touched.englishLangPost && errors.englishLangPost
                      }
                    />
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <>
                      <FormattedMessage id="french.label" />
                      <span className="requiredlabel">*</span> :
                    </>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      id={`fr`}
                      name="frenchLangPost"
                      labelText=""
                      hideLabel
                      disabled={bothFilled}
                      value={values.frenchLangPost}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      required
                      invalid={
                        touched.frenchLangPost && !!errors.frenchLangPost
                      }
                      invalidText={
                        touched.frenchLangPost && errors.frenchLangPost
                      }
                    />
                  </Column>
                </Grid>
                {bothFilled && (
                  <>
                    <br />
                    <Grid fullWidth={true}>
                      <Column lg={16} md={8} sm={4}>
                        <Section>
                          <Section>
                            <Section>
                              <Section>
                                <Heading>
                                  <FormattedMessage id="configuration.testUnit.confirmation.explain" />
                                </Heading>
                              </Section>
                            </Section>
                          </Section>
                        </Section>
                      </Column>
                    </Grid>
                  </>
                )}
                <br />
                <Grid fullWidth={true}>
                  <Column lg={8} md={8} sm={4}>
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
                    </Button>{" "}
                    <Button
                      type="button"
                      kind="tertiary"
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
                  </Column>
                </Grid>
              </Form>
            )}
          </Formik>
          <br />
          <hr />
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Section>
                  <Section>
                    <Heading>
                      <FormattedMessage id="testUnit.existing" />
                    </Heading>
                  </Section>
                </Section>
              </Section>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
          <Grid fullWidth={true}>
            {testSectionCreateList &&
              testSectionCreateList?.existingTestUnitList?.map((tu, index) => {
                return (
                  <Column lg={4} md={4} sm={4} key={index}>
                    {tu?.value}
                  </Column>
                );
              })}
          </Grid>
          <br />
          <hr />
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Section>
                  <Section>
                    <Heading>
                      <FormattedMessage id="testUnit.existing.inactive" />
                    </Heading>
                  </Section>
                </Section>
              </Section>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
          <Grid fullWidth={true}>
            {testSectionCreateList &&
              testSectionCreateList?.inactiveTestUnitList?.map((tu, index) => {
                return (
                  <Column lg={4} md={4} sm={4} key={index}>
                    {tu?.value}
                  </Column>
                );
              })}
          </Grid>
        </div>
      </div>
    </>
  );
}

export default injectIntl(TestSectionCreate);
