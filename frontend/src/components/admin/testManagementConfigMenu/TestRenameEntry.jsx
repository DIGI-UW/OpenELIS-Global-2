import React, { useContext, useState, useCallback } from "react";
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
import SearchTestNames from "./SearchTestNames";

const TEST_RENAME_ENDPOINT = "/rest/TestRenameEntry";
// One array, so the list handed to the search box keeps its identity while the
// read is still on its way and the search box does not refilter every render.
const NO_TESTS = [];
const EMPTY_NAMES = {
  name: { english: "", french: "" },
  reportingName: { english: "", french: "" },
};

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "master.lists.page.test.management",
    link: "/MasterListsPage/testManagementConfigMenu",
  },
  {
    label: "label.testName",
    link: "/MasterListsPage/TestRenameEntry",
  },
];

function TestRenameEntry() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const [isLoading, setIsLoading] = useState(true);
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [confirmationStep, setConfirmationStep] = useState(false);
  const [inputError, setInputError] = useState(false);
  const [selectedTest, setSelectedTest] = useState({});
  const [filteredTests, setFilteredTests] = useState([]);
  const [nameEdits, setNameEdits] = useState(null);

  const invalidateServerData = useInvalidateServerData();
  const { data: testNames } = useServerData(TEST_RENAME_ENDPOINT);
  const testNamesShow = testNames?.testList ?? NO_TESTS;

  const handleFilter = useCallback((filtered) => {
    setFilteredTests(filtered);
  }, []);

  // Held off until a test is picked and keyed on it. Until that read arrives
  // the hook still serves the test picked before, so the names offered for
  // editing are the empty ones rather than the previous test's.
  const { data: readNames, isPreviousData } = useServerData(
    selectedTest?.id
      ? `/rest/TestNamesProvider?testId=${selectedTest.id}`
      : null,
  );
  const testNamesLangs = isPreviousData || !readNames ? EMPTY_NAMES : readNames;
  // Only the edits live in state, on top of what the server holds.
  const testNamesLangsPost = nameEdits ?? testNamesLangs;

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
    setIsLoading(false);
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
      setIsAddModalOpen(false);
      setConfirmationStep(false);
      setSelectedTest({});
      setNameEdits(null);
      invalidateServerData();
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
    }
  }

  const openAppModle = (test) => {
    setConfirmationStep(false);
    setIsAddModalOpen(true);
    setSelectedTest(test);
    setNameEdits(null);
  };

  const testNamePost = {
    ...testNames,
    ...(selectedTest?.id
      ? {
          testId: selectedTest.id,
          nameEnglish: testNamesLangsPost?.name?.english,
          nameFrench: testNamesLangsPost?.name?.french,
          reportNameEnglish: testNamesLangsPost?.reportingName?.english,
          reportNameFrench: testNamesLangsPost?.reportingName?.french,
        }
      : {}),
  };

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
            <Column lg={8} md={4} sm={2}>
              <Section>
                <Heading>
                  <FormattedMessage id="label.testName" />
                </Heading>
              </Section>
            </Column>
            <Column lg={8} md={4} sm={2}>
              <Section>
                <Heading>
                  <SearchTestNames
                    testNames={testNamesShow}
                    onFilter={handleFilter}
                  />
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
              {filteredTests.map((test, index) => (
                <Column
                  key={index}
                  lg={4}
                  md={4}
                  sm={3}
                  style={{ minWidth: 0 }}
                >
                  <Button
                    id={`button-${index}`}
                    kind="ghost"
                    type="button"
                    onClick={() => openAppModle(test)}
                    style={{
                      color: "#000000",
                      width: "auto",
                      whiteSpace: "pre-line",
                      textAlign: "left",
                    }}
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
                          setNameEdits({
                            ...testNamesLangsPost,
                            name: {
                              ...testNamesLangsPost.name,
                              english: englishName,
                            },
                          });
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
                          setNameEdits({
                            ...testNamesLangsPost,
                            name: {
                              ...testNamesLangsPost.name,
                              french: frenchName,
                            },
                          });
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
                          setNameEdits({
                            ...testNamesLangsPost,
                            reportingName: {
                              ...testNamesLangsPost.reportingName,
                              english: englishName,
                            },
                          });
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
                          setNameEdits({
                            ...testNamesLangsPost,
                            reportingName: {
                              ...testNamesLangsPost.reportingName,
                              french: frenchName,
                            },
                          });
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
