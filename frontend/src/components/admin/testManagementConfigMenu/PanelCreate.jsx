import React, { useContext, useState } from "react";
import {
  Heading,
  Button,
  Grid,
  Column,
  RadioButton,
  RadioButtonGroup,
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
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import useDomains from "../../common/useDomains";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { Formik, Form } from "formik";
import * as Yup from "yup";

const PANEL_CREATE_ENDPOINT = "/rest/PanelCreate";

/**
 * The legacy create answers with the form on success and with a status code
 * on failure (400 validation, 409 duplicate name, 500 insert failure); the
 * JSON helper folds a non-2xx status into the body it hands back (OGC-1232).
 */
const failureMessageId = (res) => {
  const status = res?.status ?? res?.statusCode;
  if (!res || res.error || (typeof status === "number" && status >= 400)) {
    if (status === 409) {
      return "configuration.panel.create.duplicate";
    }
    if (status === 400) {
      return "error.panel.create.invalid";
    }
    return "server.error.msg";
  }
  return null;
};

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
  const domains = useDomains();

  const { data: panelCreateList } = useServerData(PANEL_CREATE_ENDPOINT);
  const invalidateServerData = useInvalidateServerData();

  const domainLabel = (domain) =>
    intl.formatMessage({
      id: `label.domain.${domain}`,
      defaultMessage: domain,
    });

  const handlePanelCreateListCall = (
    actions,
    {
      englishLangPost,
      frenchLangPost,
      selectedSampleTypeId,
      loincPost,
      domain,
    },
  ) => {
    postToOpenElisServerJsonResponse(
      "/rest/PanelCreate",
      JSON.stringify({
        panelEnglishName: englishLangPost,
        panelFrenchName: frenchLangPost,
        sampleTypeId: selectedSampleTypeId,
        panelLoinc: loincPost,
        domain,
      }),
      (res) => {
        handlePostPanelCreateListCallBack(res, actions);
      },
    );
  };

  const handlePostPanelCreateListCallBack = (res, actions) => {
    actions.setSubmitting(false);
    const failureId = failureMessageId(res);
    if (failureId === null) {
      addNotification({
        title: intl.formatMessage({
          id: "notification.title",
        }),
        message: intl.formatMessage({
          id: "success.panel.created",
        }),
        kind: NotificationKinds.success,
      });
      actions.resetForm();
      setBothFilled(false);
      invalidateServerData();
      setNotificationVisible(true);
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: failureId }),
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
              domain: "CLINICAL",
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
              setFieldValue,
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
                      invalid={touched.loincPost && !!errors.loincPost}
                      invalidText={touched.loincPost && errors.loincPost}
                    />
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <>
                      <FormattedMessage id="label.panel.domain" />
                      <span className="requiredlabel">*</span> :
                    </>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <RadioButtonGroup
                      id="panelDomain"
                      name="domain"
                      legendText=""
                      orientation="horizontal"
                      valueSelected={values.domain}
                      disabled={bothFilled}
                      onChange={(value) => setFieldValue("domain", value)}
                    >
                      {domains.map((d) => (
                        <RadioButton
                          key={d.id}
                          id={`panel-domain-${d.id}`}
                          value={d.id}
                          labelText={intl.formatMessage({
                            id: d.labelKey,
                            defaultMessage: d.id,
                          })}
                        />
                      ))}
                    </RadioButtonGroup>
                    <p
                      style={{
                        fontSize: "0.75rem",
                        color: "var(--cds-text-secondary, #6f6f6f)",
                        marginTop: "0.25rem",
                      }}
                      data-testid="panel-create-domain-helper"
                    >
                      {intl.formatMessage(
                        { id: "helper.panel.domainGuard" },
                        { domain: domainLabel(values.domain) },
                      )}
                    </p>
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
