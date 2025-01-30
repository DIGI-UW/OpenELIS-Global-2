import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Heading,
  Button,
  Loading,
  Grid,
  Column,
  Section,
  Modal,
  TextInput,
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils.js";
import { NotificationContext } from "../../layout/Layout.js";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification.js";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb.js";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "master.lists.page.test.management",
    link: "/MasterListsPage#testManagementConfigMenu",
  },
  {
    label: "label.testName",
    link: "/MasterListsPage#TestRenameEntry",
  },
];

function TestRenameEntry() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const componentMounted = useRef(false);
  const [isLoading, setIsLoading] = useState(true);
  const [finished, setFinished] = useState(true);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [confirmationStep, setConfirmationStep] = useState(false);
  const [inputError, setInputError] = useState(false);
  const [testNames, setTestNames] = useState({});
  const [testNamePost, setTestNamesPost] = useState({});
  const [testNamesShow, setTestNamesShow] = useState([]);
  const [selectedTest, setSelectedTest] = useState({});
  const [testNamesLangs, setTestNamesLangs] = useState({
    name: { english: "", french: "" },
    reportingName: { english: "", french: "" },
  });
  const [testNamesLangsPost, setTestNamesLangsPost] = useState({
    name: { english: "", french: "" },
    reportingName: { english: "", french: "" },
  });

  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/TestRenameEntry", handleTestNames);
    return () => {
      componentMounted.current = false;
    };
  }, []);

  const handleTestNames = (res) => {
    if (!res) {
      setIsLoading(true);
    } else {
      setTestNames(res);
      setTestNamesPost(res);
      setTestNamesShow(res?.testList);
    }
  };

  useEffect(() => {
    if (selectedTest && selectedTest.id) {
      getFromOpenElisServer(
        `/rest/TestNamesProvider?testId=${selectedTest?.id}`,
        handleTestNamesLangs,
      );
    }
  }, [selectedTest]);

  const handleTestNamesLangs = (res) => {
    if (!res) {
      setIsLoading(true);
    } else {
      setTestNamesLangs(res);
      setTestNamesLangsPost(res);
    }
  };

  function testRenameEntryPost() {
    setIsLoading(true);
    if (confirmationStep) {
      postToOpenElisServerJsonResponse(
        `/rest/TestRenameEntry`,
        JSON.stringify(testNamePost),
        (res) => {
          testRenameEntryPostCallback(res);
        },
      );
    } else {
      setConfirmationStep(true);
    }
  }

  function testRenameEntryPostCallback(res) {
    if (res) {
      setIsLoading(false);
      setFinished(false);
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
      setIsAddModalOpen(false);
      setTimeout(() => {
        window.location.reload();
      }, 10);
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
      setTimeout(() => {
        window.location.reload();
      }, 10);
    }
  }

  const openAppModle = (test) => {
    setConfirmationStep(false);
    setIsAddModalOpen(true);
    setSelectedTest(test);
  };

  useEffect(() => {
    if (selectedTest && testNamesLangsPost && testNamesLangsPost.name) {
      setTestNamesPost((prev) => ({
        ...prev,
        testId: selectedTest?.id,
        nameEnglish: testNamesLangsPost.name.english,
        nameFrench: testNamesLangsPost.name.french,
        reportNameEnglish: testNamesLangsPost.reportingName.english,
        reportNameFrench: testNamesLangsPost.reportingName.french,
      }));
    }
  }, [testNamesLangsPost, selectedTest]);

  const closeAddModal = () => {
    setIsAddModalOpen(false);
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
                  <FormattedMessage id="label.testName" />
                </Heading>
              </Section>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
          <br />
          {testNamesShow ? (
            <Grid fullWidth={true}>
              {testNamesShow.map((test, index) => (
                <Column key={index} lg={5} md={5} sm={5}>
                  <Button
                    id={`button-${index}`}
                    kind="ghost"
                    type="button"
                    onClick={() => {
                      openAppModle(test);
                    }}
                    style={{ color: "#000000" }}
                  >
                    {test.value}
                  </Button>
                </Column>
              ))}
              <Modal
                open={isAddModalOpen}
                size="md"
                modalHeading={`Test : ${selectedTest?.value}`}
                primaryButtonText={
                  confirmationStep ? (
                    <>
                      <FormattedMessage id="column.name.accept" />
                    </>
                  ) : (
                    <>
                      <FormattedMessage id="column.name.save" />
                    </>
                  )
                }
                secondaryButtonText={
                  confirmationStep ? (
                    <>
                      <FormattedMessage id="header.reject" />
                    </>
                  ) : (
                    <>
                      <FormattedMessage id="label.button.cancel" />
                    </>
                  )
                }
                onRequestSubmit={testRenameEntryPost}
                onRequestClose={closeAddModal}
              >
                {testNamesLangs && testNamesLangs.name ? (
                  <Grid fullWidth={true}>
                    <Column lg={16} md={8} sm={4}>
                      <FormattedMessage id="column.name.testName" />
                      <br />
                      <br />
                      <>
                        <FormattedMessage id="english.current" /> :{" "}
                        {testNamesLangs?.name.english}
                      </>
                      <TextInput
                        id={`eng`}
                        labelText=""
                        hideLabel
                        value={testNamesLangsPost?.name?.english || ""}
                        onChange={(e) => {
                          const englishName = e.target.value;
                          setTestNamesLangsPost((prev) => ({
                            ...prev,
                            name: {
                              ...prev.name,
                              english: englishName,
                            },
                          }));
                          setInputError(false);
                        }}
                        required
                        invalid={inputError}
                        invalidText={
                          <FormattedMessage id="required.invalidtext" />
                        }
                      />
                      <br />
                      <>
                        <FormattedMessage id="french.current" /> :{" "}
                        {testNamesLangs?.name?.french}
                      </>
                      <TextInput
                        id={`fr`}
                        labelText=""
                        hideLabel
                        value={testNamesLangsPost?.name?.french || ""}
                        onChange={(e) => {
                          const frenchName = e.target.value;
                          setTestNamesLangsPost((prev) => ({
                            ...prev,
                            name: {
                              ...prev.name,
                              french: frenchName,
                            },
                          }));
                          setInputError(false);
                        }}
                        required
                        invalid={inputError}
                        invalidText={
                          <FormattedMessage id="required.invalidtext" />
                        }
                      />
                      <br />
                      <br />
                      <FormattedMessage id="reporting.label.testName" />
                      <br />
                      <br />
                      <>
                        <FormattedMessage id="english.current" /> :{" "}
                        {testNamesLangs?.reportingName?.english}
                      </>
                      <TextInput
                        id={`eng`}
                        labelText=""
                        hideLabel
                        value={testNamesLangsPost?.reportingName?.english || ""}
                        onChange={(e) => {
                          const englishName = e.target.value;
                          setTestNamesLangsPost((prev) => ({
                            ...prev,
                            reportingName: {
                              ...prev.reportingName,
                              english: englishName,
                            },
                          }));
                          setInputError(false);
                        }}
                        required
                        invalid={inputError}
                        invalidText={
                          <FormattedMessage id="required.invalidtext" />
                        }
                      />
                      <br />
                      <>
                        <FormattedMessage id="french.current" /> :{" "}
                        {testNamesLangs?.reportingName?.french}
                      </>
                      <TextInput
                        id={`fr`}
                        labelText=""
                        hideLabel
                        value={testNamesLangsPost?.reportingName?.french || ""}
                        onChange={(e) => {
                          const frenchName = e.target.value;
                          setTestNamesLangsPost((prev) => ({
                            ...prev,
                            reportingName: {
                              ...prev.reportingName,
                              french: frenchName,
                            },
                          }));
                          setInputError(false);
                        }}
                        required
                        invalid={inputError}
                        invalidText={
                          <FormattedMessage id="required.invalidtext" />
                        }
                      />
                    </Column>
                  </Grid>
                ) : (
                  <>
                    <div>
                      <Loading />
                    </div>
                  </>
                )}
                <br />
                {confirmationStep && (
                  <>
                    <Section>
                      <Section>
                        <Section>
                          <Heading>
                            <FormattedMessage id="confirmation.rename" />
                          </Heading>
                        </Section>
                      </Section>
                    </Section>
                  </>
                )}
              </Modal>
            </Grid>
          ) : (
            <>
              <Loading active={isLoading} />
            </>
          )}
        </div>
      </div>
    </>
  );
}

export default injectIntl(TestRenameEntry);
