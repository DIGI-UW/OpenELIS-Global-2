import React, { useContext, useState } from "react";
import {
  Heading,
  Button,
  Grid,
  Column,
  Section,
  Select,
  SelectItem,
  ListItem,
  TextInput,
} from "@carbon/react";
import { postToOpenElisServerJsonResponse } from "../../utils/Utils";
import {
  useInvalidateServerData,
  useServerData,
} from "../../utils/useServerData";
import { NotificationContext } from "../../layout/contexts";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { Formik, Form } from "formik";
import * as Yup from "yup";

const PANEL_CREATE_ENDPOINT = "/rest/PanelCreate";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "master.lists.page.test.management",
    link: "/MasterListsPage/testManagementConfigMenu",
  },
  {
    label: "configuration.panel.manage",
    link: "/MasterListsPage/PanelManagement",
  },
  {
    label: "configuration.panel.create",
    link: "/MasterListsPage/PanelCreate",
  },
];

function PanelCreate() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();
  const [bothFilled, setBothFilled] = useState(false);

  const { data: panelCreateList } = useServerData(PANEL_CREATE_ENDPOINT);
  const invalidateServerData = useInvalidateServerData();

  const handlePanelCreateListCall = (
    actions,
    { englishLangPost, frenchLangPost, selectedSampleTypeId, loincPost },
  ) => {
    postToOpenElisServerJsonResponse(
      "/rest/PanelCreate",
      JSON.stringify({
        panelEnglishName: englishLangPost,
        panelFrenchName: frenchLangPost,
        sampleTypeId: selectedSampleTypeId,
        panelLoinc: loincPost,
      }),
      (res) => {
        handlePostPanelCreateListCallBack(res, actions);
      },
    );
  };

  const handlePostPanelCreateListCallBack = (res, actions) => {
    if (res) {
      if (res) {
        addNotification({
          title: intl.formatMessage({
            id: "notification.title",
          }),
          message: intl.formatMessage({
            id: "notification.user.post.delete.success",
          }),
          kind: NotificationKinds.success,
        });
        actions.resetForm();
        setBothFilled(false);
        invalidateServerData();
        setNotificationVisible(true);
      }
    } else {
      actions.setSubmitting(false);
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
    }
  };

  const validationSchema = Yup.object({
    englishLangPost: Yup.string()
      .required("fill this field")
      .test(
        "duplicate-check",
        intl.formatMessage({ id: "input.error.same.panel.type" }),
        (value) => !validatePanelType(value),
      )
      .trim(),
    frenchLangPost: Yup.string()
      .required("fill this field")
      .test(
        "duplicate-check",
        intl.formatMessage({ id: "input.error.same.panel.type" }),
        (value) => !validatePanelType(value),
      )
      .trim(),
    loincPost: Yup.string()
      .required("fill this field")
      .trim()
      .matches(
        /^(?!-)(?:\d+-)*\d+$/,
        "Invalid format. Use digits separated by single dashes (e.g. 1-2-3)",
      ),
  });

  const allPanels = [
    ...(panelCreateList?.existingPanelList
      ? panelCreateList.existingPanelList.flatMap((epl) => epl?.panels || [])
      : []),
    ...(panelCreateList?.inactivePanelList
      ? panelCreateList.inactivePanelList.flatMap((epl) => epl?.panels || [])
      : []),
  ];

  const validatePanelType = (name) => {
    return allPanels.some((panel) => panel?.panelName === name);
  };

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
                <Section>
                  <Heading>
                    <FormattedMessage id="configuration.panel.create" />
                  </Heading>
                </Section>
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
                      <FormattedMessage id="panel.new" />
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
            initialValues={{
              englishLangPost: "",
              frenchLangPost: "",
              selectedSampleTypeId: "",
              loincPost: "",
            }}
            validationSchema={validationSchema}
            onSubmit={(values, actions) => {
              if (bothFilled) {
                handlePanelCreateListCall(actions, values);
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
              resetForm,
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
                  <Column lg={8} md={4} sm={4}>
                    <>
                      <FormattedMessage id="sample.type" />
                      <span className="requiredlabel">*</span> :
                    </>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Select
                      id="smapleTypeSelect"
                      name="selectedSampleTypeId"
                      value={values.selectedSampleTypeId}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      labelText={intl.formatMessage({
                        id: "sample.select.type",
                      })}
                      invalid={
                        touched.selectedSampleTypeId &&
                        !!errors.selectedSampleTypeId
                      }
                      invalidText={
                        touched.selectedSampleTypeId &&
                        errors.selectedSampleTypeId
                      }
                      required
                    >
                      <SelectItem value={"0"} text={"Select Sample Type"} />
                      {panelCreateList?.existingSampleTypeList?.map(
                        (st, index) => (
                          <SelectItem
                            key={index}
                            text={st.value}
                            value={st.id}
                          />
                        ),
                      )}
                    </Select>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <>
                      <FormattedMessage id="field.loinc" />
                      <span className="requiredlabel">*</span> :
                    </>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      id={`loincPost`}
                      name="loincPost"
                      labelText=""
                      hideLabel
                      disabled={bothFilled}
                      value={values.loincPost}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      required
                      //invalid={touched.loincPost && !!errors.loincPost}
                      //invalidText={touched.loincPost && errors.loincPost}
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
                                  <FormattedMessage id="configuration.panel.confirmation.explain" />
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
                        resetForm();
                        setBothFilled(false);
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
                      <FormattedMessage id="panel.existing" />
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
            {panelCreateList &&
              panelCreateList?.existingPanelList?.map((epl, index) => {
                return (
                  <Column lg={4} md={4} sm={4} key={index}>
                    <span style={{ fontWeight: "bold" }}>
                      {epl?.typeOfSampleName}
                    </span>
                    {epl?.panels?.map((panel, index) => {
                      return (
                        <Column lg={4} md={4} sm={4} key={index}>
                          <ListItem>{panel?.panelName}</ListItem>
                        </Column>
                      );
                    })}
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
                      <FormattedMessage id="panel.existing.inactive" />
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
            {panelCreateList &&
              panelCreateList?.inactivePanelList?.map((epl, index) => {
                return (
                  <Column lg={4} md={4} sm={4} key={index}>
                    <span style={{ fontWeight: "bold" }}>
                      {epl?.typeOfSampleName}
                    </span>
                    {epl?.panels?.map((panel, index) => {
                      return (
                        <Column lg={4} md={4} sm={4} key={index}>
                          <ListItem>{panel?.panelName}</ListItem>
                        </Column>
                      );
                    })}
                  </Column>
                );
              })}
          </Grid>
        </div>
      </div>
    </>
  );
}

export default injectIntl(PanelCreate);
