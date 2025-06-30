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
  Modal,
  TextInput,
  Dropdown,
  TextArea,
  Checkbox,
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils.js";
import {
  ConfigurationContext,
  NotificationContext,
} from "../../layout/Layout.js";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification.js";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb.js";
import { ArrowLeft, ArrowRight, Cost } from "@carbon/icons-react";
import ActionPaginationButtonType from "../../common/ActionPaginationButtonType.js";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "testnotificationconfig.browse.title",
    link: "/MasterListsPage#testNotificationConfigMenu",
  },
];

function TestNotificationConfigEdit() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const ID = (() => {
    const hash = window.location.hash;
    if (hash.includes("?")) {
      const queryParams = hash.split("?")[1];
      const urlParams = new URLSearchParams(queryParams);
      return urlParams.get("testId");
    }
    return "0";
  })();

  const componentMounted = useRef(false);
  const [indMsg, setIndMsg] = useState("0");
  const [loading, setLoading] = useState(true);
  const [saveButton, setSaveButton] = useState(false);
  const [sysDefaultMsg, setSysDefaultMsg] = useState(true);
  const [testNotificationConfigEditData, setTestNotificationConfigEditData] =
    useState({});
  const [
    testNotificationConfigEditDataPost,
    setTestNotificationConfigEditDataPost,
  ] = useState({});
  const [testNamesList, setTestNamesList] = useState([]);
  const [testName, setTestName] = useState("");
  const [testNotificationConfigMenuList, setTestNotificationConfigMenuList] =
    useState([]);

  useEffect(() => {
    if (testNotificationConfigEditData) {
      setTestNotificationConfigEditDataPost(
        (prevSetTestNotificationConfigDataPost) => ({
          ...prevSetTestNotificationConfigDataPost,
          formName: testNotificationConfigEditData.formName,
          formMethod: testNotificationConfigEditData.formMethod,
          cancelAction: testNotificationConfigEditData.cancelAction,
          submitOnCancel: testNotificationConfigEditData.submitOnCancel,
          cancelMethod: testNotificationConfigEditData.cancelMethod,
          config: testNotificationConfigEditData.config,
          systemDefaultPayloadTemplate:
            testNotificationConfigEditData.systemDefaultPayloadTemplate,
          editSystemDefaultPayloadTemplate:
            testNotificationConfigEditData.editSystemDefaultPayloadTemplate,
        }),
      );
    }
  }, [testNotificationConfigEditData]);

  const handleMenuItems = (res) => {
    if (res) {
      setTestNotificationConfigEditData(res);
    }
    setLoading(false);
  };

  const handleTestNamesList = (res) => {
    if (res) {
      setTestNamesList(res);
    }
    setLoading(false);
  };

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer(
      `/rest/TestNotificationConfig?testId=${ID}`,
      handleMenuItems,
    );
    getFromOpenElisServer(`/rest/test-list`, handleTestNamesList);
    return () => {
      componentMounted.current = false;
    };
  }, [ID]);

  useEffect(() => {
    const testId = testNotificationConfigEditData?.config?.testId;
    if (testNamesList && testId) {
      const test = testNamesList.find((item) => item.id === testId);
      if (test) {
        setTestName(test.value);
      }
    }
  }, [testNamesList, testNotificationConfigEditData]);

  function handleSubjectTemplateChange(e) {
    setTestNotificationConfigEditDataPost((prev) => ({
      ...prev,
      editSystemDefaultPayloadTemplate: true,
    }));
    setTestNotificationConfigEditDataPost((prev) => ({
      ...prev,
      systemDefaultPayloadTemplate: {
        ...prev.systemDefaultPayloadTemplate,
        subjectTemplate: e.target.value,
      },
    }));
  }

  function handleMessageTemplateChange(e) {
    setTestNotificationConfigEditDataPost((prev) => ({
      ...prev,
      editSystemDefaultPayloadTemplate: true,
    }));
    setTestNotificationConfigEditDataPost((prev) => ({
      ...prev,
      systemDefaultPayloadTemplate: {
        ...prev.systemDefaultPayloadTemplate,
        messageTemplate: e.target.value,
      },
    }));
  }

  const handleCheckboxChange = (e) => {
    const { id, checked } = e.target;

    setTestNotificationConfigEditDataPost((prev) => {
      const updatedConfig = { ...prev.config };

      switch (id) {
        case "providerEmail":
          updatedConfig.providerEmail.active = checked;
          break;
        case "patientEmail":
          updatedConfig.patientEmail.active = checked;
          break;
        case "patientSMS":
          updatedConfig.patientSMS.active = checked;
          break;
        case "providerSMS":
          updatedConfig.providerSMS.active = checked;
          break;
        default:
          break;
      }

      return {
        ...prev,
        config: updatedConfig,
      };
    });
  };

  function testNotificationConfigEditSavePostCall() {
    setLoading(true);
    postToOpenElisServerJsonResponse(
      `/rest/TestNotificationConfig`,
      JSON.stringify(testNotificationConfigEditDataPost),
      (res) => {
        testNotificationConfigEditSavePostCallBack(res);
      },
    );
  }

  function testNotificationConfigEditSavePostCallBack(res) {
    if (res) {
      addNotification({
        title: intl.formatMessage({
          id: "notification.title",
        }),
        message: intl.formatMessage({
          id: "notification.user.post.save.success",
        }),
        kind: NotificationKinds.success,
      });
      setNotificationVisible(true);
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
    }
    setLoading(false);
  }

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {loading && <Loading></Loading>}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Section>
                <Heading>
                  <FormattedMessage id="testnotificationconfig.browse.title" />
                </Heading>
              </Section>
            </Section>
          </Column>
        </Grid>
        <div className="orderLegendBody">
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Section>
                  <Heading>
                    {testName && <FormattedMessage id={`${testName}`} />}
                  </Heading>
                </Section>
              </Section>
            </Column>
          </Grid>
          <hr />
          <br />
          {testNotificationConfigEditDataPost?.config && (
            <Grid fullWidth={true}>
              <Column lg={4} md={4} sm={2}>
                <Checkbox
                  id="patientEmail"
                  labelText={
                    <FormattedMessage id="testnotification.patient.email" />
                  }
                  checked={
                    testNotificationConfigEditDataPost.config.patientEmail
                      ?.active ?? false
                  }
                  onChange={handleCheckboxChange}
                />
              </Column>
              <Column lg={4} md={4} sm={2}>
                <Checkbox
                  id="patientSMS"
                  labelText={
                    <FormattedMessage id="testnotification.patient.sms" />
                  }
                  checked={
                    testNotificationConfigEditDataPost.config.patientSMS
                      ?.active ?? false
                  }
                  onChange={handleCheckboxChange}
                />
              </Column>
              <Column lg={4} md={4} sm={2}>
                <Checkbox
                  id="providerSMS"
                  labelText={
                    <FormattedMessage id="testnotification.provider.sms" />
                  }
                  checked={
                    testNotificationConfigEditDataPost.config.providerSMS
                      ?.active ?? false
                  }
                  onChange={handleCheckboxChange}
                />
              </Column>
              <Column lg={4} md={4} sm={2}>
                <Checkbox
                  // key={section.elementID}
                  // id={section.elementID}
                  // value={section.roleId}
                  // labelText={section.roleName}
                  // checked={selectedGlobalLabUnitRoles.includes(section.roleId)}
                  id="providerEmail"
                  labelText={
                    <FormattedMessage id="testnotification.provider.email" />
                  }
                  checked={
                    testNotificationConfigEditDataPost.config.providerEmail
                      ?.active ?? false
                  }
                  onChange={handleCheckboxChange}
                />
              </Column>
            </Grid>
          )}
          <br />
          <hr />
          <br />
          <Grid fullWidth={true} className="gridBoundary">
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Section>
                  <Heading>
                    <FormattedMessage id="testnotification.instructions.header" />
                  </Heading>
                </Section>
              </Section>
              <br />
              <FormattedMessage id="testnotification.instructions.body" />
              <br />
              <br />
              <FormattedMessage id="testnotification.instructions.body.0" />
              <br />
              <br />
              <FormattedMessage id="testnotification.instructions.body.1" />
              <br />
              <br />
              <FormattedMessage id="testnotification.instructions.body.2" />
              <br />
              <br />
              <FormattedMessage id="testnotification.instructions.body.3" />
              <br />
              <br />
              <Section>
                <Section>
                  <Heading>
                    <FormattedMessage id="testnotification.instructionis.variables.header" />
                  </Heading>
                </Section>
              </Section>
              <br />
              <FormattedMessage id="testnotification.instructionis.variables.body" />
              <br />
              <br />
              <FormattedMessage id="testnotification.instructionis.variables.body.0" />
              <br />
              <br />
              <FormattedMessage id="testnotification.instructionis.variables.body.1" />
              <br />
              <br />
              <FormattedMessage id="testnotification.instructionis.variables.body.2" />
              <br />
              <br />
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
          <div>
            <Grid fullWidth={true}>
              <Column lg={14} md={8} sm={4}>
                <Section>
                  <Section>
                    <Section>
                      <Heading>
                        <FormattedMessage id="testnotification.systemdefault.template" />
                      </Heading>
                    </Section>
                  </Section>
                </Section>
              </Column>
              <Column lg={2} md={8} sm={4}>
                <Button
                  onClick={() => {
                    setSysDefaultMsg(!sysDefaultMsg);
                  }}
                >
                  Edit
                </Button>
              </Column>
            </Grid>
            <br />
            <Grid fullWidth={true}>
              <Column lg={8} md={4} sm={2}>
                <FormattedMessage id="testnotification.subjecttemplate" />
              </Column>
              <Column lg={8} md={4} sm={2}>
                <TextInput
                  id="subject"
                  type="text"
                  labelText=""
                  hideLabel={true}
                  disabled={sysDefaultMsg}
                  placeholder={intl.formatMessage({
                    id: "systemDefaultPayloadTemplate.subjectTemplate",
                  })}
                  // invalid={
                  //   userDataShow &&
                  //   userDataShow.userLoginName &&
                  //   !loginNameRegex.test(userDataShow.userLoginName)
                  // }
                  // // invalidText={errors.order}
                  // required={true}
                  value={
                    testNotificationConfigEditDataPost &&
                    testNotificationConfigEditDataPost.systemDefaultPayloadTemplate &&
                    testNotificationConfigEditDataPost
                      .systemDefaultPayloadTemplate.subjectTemplate
                      ? testNotificationConfigEditDataPost
                          .systemDefaultPayloadTemplate.subjectTemplate
                      : ""
                  }
                  onChange={(e) => handleSubjectTemplateChange(e)}
                />
              </Column>
            </Grid>
            <br />
            <Grid fullWidth={true}>
              <Column lg={16} md={8} sm={4}>
                <FormattedMessage id="testnotification.messagetemplate" />
              </Column>
            </Grid>
            <br />
            <Grid fullWidth={true}>
              <Column lg={16} md={8} sm={4}>
                <TextArea
                  id="message"
                  type="text"
                  labelText=""
                  hideLabel={true}
                  disabled={sysDefaultMsg}
                  placeholder={intl.formatMessage({
                    id: "systemDefaultPayloadTemplate.messageTemplate",
                  })}
                  // invalid={
                  //   userDataShow &&
                  //   userDataShow.userLoginName &&
                  //   !loginNameRegex.test(userDataShow.userLoginName)
                  // }
                  // // invalidText={errors.order}
                  // required={true}
                  value={
                    testNotificationConfigEditDataPost &&
                    testNotificationConfigEditDataPost.systemDefaultPayloadTemplate &&
                    testNotificationConfigEditDataPost
                      .systemDefaultPayloadTemplate.messageTemplate
                      ? testNotificationConfigEditDataPost
                          .systemDefaultPayloadTemplate.messageTemplate
                      : ""
                  }
                  onChange={(e) => handleMessageTemplateChange(e)}
                />
              </Column>
            </Grid>
          </div>
          <br />
          <hr />
          <br />
          <div>
            <Grid fullWidth={true}>
              <Column lg={16} md={8} sm={4}>
                <Section>
                  <Section>
                    <Section>
                      <Heading>
                        <FormattedMessage id="testnotification.testdefault.template" />
                      </Heading>
                    </Section>
                  </Section>
                </Section>
              </Column>
            </Grid>
            <br />
            <Grid fullWidth={true}>
              <Column lg={8} md={4} sm={2}>
                <FormattedMessage id="testnotification.subjecttemplate" />
              </Column>
              <Column lg={8} md={4} sm={2}>
                <TextInput
                  id="subject"
                  type="text"
                  labelText=""
                  // invalid={
                  //   userDataShow &&
                  //   userDataShow.userLoginName &&
                  //   !loginNameRegex.test(userDataShow.userLoginName)
                  // }
                  // // invalidText={errors.order}
                  // required={true}
                  // value={
                  //   userDataShow && userDataShow.userLoginName
                  //     ? userDataShow.userLoginName
                  //     : ""
                  // }
                  // onChange={(e) => handleUserLoginNameChange(e)}
                />
              </Column>
            </Grid>
            <br />
            <Grid fullWidth={true}>
              <Column lg={16} md={8} sm={4}>
                <FormattedMessage id="testnotification.messagetemplate" />
              </Column>
            </Grid>
            <br />
            <Grid fullWidth={true}>
              <Column lg={16} md={8} sm={4}>
                <TextArea
                  id="message"
                  type="text"
                  labelText=""
                  // invalid={
                  //   userDataShow &&
                  //   userDataShow.userLoginName &&
                  //   !loginNameRegex.test(userDataShow.userLoginName)
                  // }
                  // // invalidText={errors.order}
                  // required={true}
                  // value={
                  //   userDataShow && userDataShow.userLoginName
                  //     ? userDataShow.userLoginName
                  //     : ""
                  // }
                  // onChange={(e) => handleUserLoginNameChange(e)}
                />
              </Column>
            </Grid>
          </div>
          <br />
          <hr />
          <br />
          <div>
            <Grid fullWidth={true}>
              <Column lg={14} md={8} sm={4}>
                <Section>
                  <Section>
                    <Section>
                      <Heading>
                        <FormattedMessage id="testnotification.options" />
                      </Heading>
                    </Section>
                  </Section>
                </Section>
              </Column>
            </Grid>
            <br />
            <Grid fullWidth={true}>
              <Column lg={4} md={8} sm={4}>
                <Button
                  onClick={() => {
                    setIndMsg("0");
                  }}
                  kind="tertiary"
                >
                  <FormattedMessage id="testnotification.provider.email" />
                </Button>
              </Column>{" "}
              <Column lg={4} md={8} sm={4}>
                <Button
                  onClick={() => {
                    setIndMsg("1");
                  }}
                  kind="tertiary"
                >
                  <FormattedMessage id="testnotification.provider.sms" />
                </Button>
              </Column>{" "}
              <Column lg={4} md={8} sm={4}>
                <Button
                  onClick={() => {
                    setIndMsg("2");
                  }}
                  kind="tertiary"
                >
                  <FormattedMessage id="testnotification.patient.email" />
                </Button>
              </Column>{" "}
              <Column lg={4} md={8} sm={4}>
                <Button
                  onClick={() => {
                    setIndMsg("3");
                  }}
                  kind="tertiary"
                >
                  <FormattedMessage id="testnotification.patient.sms" />
                </Button>
              </Column>
            </Grid>
            <br />
            <hr />
            <br />
            {indMsg === "0" || indMsg === "2" ? (
              <>
                <Grid fullWidth={true}>
                  <Column lg={16} md={8} sm={4}>
                    <Section>
                      <Section>
                        <Section>
                          <Heading>
                            {indMsg === "0" ? (
                              <>
                                <FormattedMessage id="testnotification.provider.email" />
                              </>
                            ) : (
                              <>
                                <FormattedMessage id="testnotification.patient.email" />
                              </>
                            )}
                          </Heading>
                        </Section>
                      </Section>
                    </Section>
                  </Column>
                </Grid>
                <br />
                <Grid fullWidth={true}>
                  <Column lg={8} md={4} sm={2}>
                    <FormattedMessage id="testnotification.bcc" />
                  </Column>
                  <Column lg={8} md={4} sm={2}>
                    <TextInput
                      id="subject"
                      type="text"
                      labelText=""
                      // invalid={
                      //   userDataShow &&
                      //   userDataShow.userLoginName &&
                      //   !loginNameRegex.test(userDataShow.userLoginName)
                      // }
                      // // invalidText={errors.order}
                      // required={true}
                      // value={
                      //   userDataShow && userDataShow.userLoginName
                      //     ? userDataShow.userLoginName
                      //     : ""
                      // }
                      // onChange={(e) => handleUserLoginNameChange(e)}
                    />
                  </Column>
                </Grid>
                <br />
                <Grid fullWidth={true}>
                  <Column lg={8} md={4} sm={2}>
                    <FormattedMessage id="testnotification.subjecttemplate" />
                  </Column>
                  <Column lg={8} md={4} sm={2}>
                    <TextInput
                      id="subject"
                      type="text"
                      labelText=""
                      // invalid={
                      //   userDataShow &&
                      //   userDataShow.userLoginName &&
                      //   !loginNameRegex.test(userDataShow.userLoginName)
                      // }
                      // // invalidText={errors.order}
                      // required={true}
                      // value={
                      //   userDataShow && userDataShow.userLoginName
                      //     ? userDataShow.userLoginName
                      //     : ""
                      // }
                      // onChange={(e) => handleUserLoginNameChange(e)}
                    />
                  </Column>
                </Grid>
                <br />
                <Grid fullWidth={true}>
                  <Column lg={16} md={8} sm={4}>
                    <FormattedMessage id="testnotification.messagetemplate" />
                  </Column>
                </Grid>
                <br />
                <Grid fullWidth={true}>
                  <Column lg={16} md={8} sm={4}>
                    <TextArea
                      id="message"
                      type="text"
                      labelText=""
                      // invalid={
                      //   userDataShow &&
                      //   userDataShow.userLoginName &&
                      //   !loginNameRegex.test(userDataShow.userLoginName)
                      // }
                      // // invalidText={errors.order}
                      // required={true}
                      // value={
                      //   userDataShow && userDataShow.userLoginName
                      //     ? userDataShow.userLoginName
                      //     : ""
                      // }
                      // onChange={(e) => handleUserLoginNameChange(e)}
                    />
                  </Column>
                </Grid>
              </>
            ) : (
              <>
                <Grid fullWidth={true}>
                  <Column lg={16} md={8} sm={4}>
                    <Section>
                      <Section>
                        <Section>
                          <Heading>
                            {indMsg === "1" ? (
                              <>
                                <FormattedMessage id="testnotification.provider.sms" />
                              </>
                            ) : (
                              <>
                                <FormattedMessage id="testnotification.patient.sms" />
                              </>
                            )}
                          </Heading>
                        </Section>
                      </Section>
                    </Section>
                  </Column>
                </Grid>
                <br />
                <Grid fullWidth={true}>
                  <Column lg={16} md={8} sm={4}>
                    <FormattedMessage id="testnotification.messagetemplate" />
                  </Column>
                </Grid>
                <br />
                <Grid fullWidth={true}>
                  <Column lg={16} md={8} sm={4}>
                    <TextArea
                      id="message"
                      type="text"
                      labelText=""
                      // invalid={
                      //   userDataShow &&
                      //   userDataShow.userLoginName &&
                      //   !loginNameRegex.test(userDataShow.userLoginName)
                      // }
                      // // invalidText={errors.order}
                      // required={true}
                      // value={
                      //   userDataShow && userDataShow.userLoginName
                      //     ? userDataShow.userLoginName
                      //     : ""
                      // }
                      // onChange={(e) => handleUserLoginNameChange(e)}
                    />
                  </Column>
                </Grid>
              </>
            )}
            <br />
            <hr />
            <br />
            <Grid fullWidth={true}>
              <Column lg={16} md={8} sm={4}>
                <Button
                  disabled={saveButton}
                  onClick={testNotificationConfigEditSavePostCall}
                  type="button"
                >
                  <FormattedMessage id="label.button.save" />
                </Button>{" "}
                <Button
                  onClick={() =>
                    window.location.assign(
                      "/MasterListsPage#testNotificationConfigMenu",
                    )
                  }
                  kind="tertiary"
                  type="button"
                >
                  <FormattedMessage id="label.button.exit" />
                </Button>
              </Column>
            </Grid>
          </div>
        </div>
      </div>
    </>
  );
}

export default injectIntl(TestNotificationConfigEdit);
