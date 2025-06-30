import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Heading,
  Button,
  Loading,
  Grid,
  Column,
  Section,
  Checkbox,
  Modal,
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
    label: "configuration.test.orderable",
    link: "/MasterListsPage#TestOrderability",
  },
];

function TestOrderability() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const componentMounted = useRef(false);
  const [isLoading, setIsLoading] = useState(false);
  const [isConfirmModalOpen, setIsConfirmModalOpen] = useState(false);
  const [testOrderabilityData, setTestOrderabilityData] = useState({});
  const [changedTestOrderabilityData, setChangedTestOrderabilityData] =
    useState({});
  const [jsonChangeList, setJsonChangeList] = useState({
    activateTest: [],
    deactivateTest: [],
  });

  const handleActiveTestsCheckboxChange = (test, sampleTypeId, isChecked) => {
    setChangedTestOrderabilityData((prev) => {
      let orderableTestList = [...prev.orderableTestList];

      orderableTestList = orderableTestList.map((sample) => {
        if (sample.sampleType.id === sampleTypeId) {
          let activeTests = [...sample.activeTests];
          let inactiveTests = [...sample.inactiveTests];

          const originalState = testOrderabilityData.orderableTestList.find(
            (sample) => sample.sampleType.id === sampleTypeId,
          );

          if (
            activeTests.some((t) => t.id === test.id) ||
            inactiveTests.some((t) => t.id === test.id)
          ) {
            if (isChecked === true) {
              inactiveTests = inactiveTests.filter(
                (item) => item.id !== test.id,
              );
              if (!activeTests.some((item) => item.id === test.id)) {
                const originalIndex = originalState.activeTests.findIndex(
                  (t) => t.id === test.id,
                );
                if (originalIndex !== -1) {
                  activeTests.splice(originalIndex, 0, test);
                } else {
                  activeTests.push(test);
                }
              }
            } else {
              activeTests = activeTests.filter((item) => item.id !== test.id);
              if (!inactiveTests.some((item) => item.id === test.id)) {
                const originalIndex = originalState.inactiveTests.findIndex(
                  (t) => t.id === test.id,
                );
                if (originalIndex !== -1) {
                  inactiveTests.splice(originalIndex, 0, test);
                } else {
                  inactiveTests.push(test);
                }
              }
            }
          }

          return { ...sample, activeTests, inactiveTests };
        }
        return sample;
      });

      return { ...prev, orderableTestList };
    });

    setJsonChangeList((prev) => {
      let activateTest = [...prev.activateTest];
      let deactivateTest = [...prev.deactivateTest];

      const originalState = testOrderabilityData.orderableTestList.find(
        (sample) => sample.sampleType.id === sampleTypeId,
      );

      const isOriginallyActive = originalState.activeTests.some(
        (t) => t.id === test.id,
      );

      if (isChecked === true) {
        if (!isOriginallyActive) {
          if (!activateTest.some((item) => item.id === test.id)) {
            activateTest.push({ id: test.id });
          }
        } else {
          activateTest = activateTest.filter((item) => item.id !== test.id);
          deactivateTest = deactivateTest.filter((item) => item.id !== test.id);
        }
      } else {
        if (isOriginallyActive) {
          if (!deactivateTest.some((item) => item.id === test.id)) {
            deactivateTest.push({ id: test.id });
          }
        } else {
          activateTest = activateTest.filter((item) => item.id !== test.id);
          deactivateTest = deactivateTest.filter((item) => item.id !== test.id);
        }
      }

      return { activateTest, deactivateTest };
    });
  };

  const handleInactiveTestsCheckboxChange = (test, sampleTypeId, isChecked) => {
    setChangedTestOrderabilityData((prev) => {
      let orderableTestList = [...prev.orderableTestList];

      orderableTestList = orderableTestList.map((sample) => {
        if (sample.sampleType.id === sampleTypeId) {
          let activeTests = [...sample.activeTests];
          let inactiveTests = [...sample.inactiveTests];

          const originalState = testOrderabilityData.orderableTestList.find(
            (sample) => sample.sampleType.id === sampleTypeId,
          );

          if (
            activeTests.some((t) => t.id === test.id) ||
            inactiveTests.some((t) => t.id === test.id)
          ) {
            if (isChecked === false) {
              activeTests = activeTests.filter((item) => item.id !== test.id);
              if (!inactiveTests.some((item) => item.id === test.id)) {
                const originalIndex = originalState.inactiveTests.findIndex(
                  (t) => t.id === test.id,
                );
                if (originalIndex !== -1) {
                  inactiveTests.splice(originalIndex, 0, test);
                } else {
                  inactiveTests.push(test);
                }
              }
            } else {
              inactiveTests = inactiveTests.filter(
                (item) => item.id !== test.id,
              );
              if (!activeTests.some((item) => item.id === test.id)) {
                const originalIndex = originalState.activeTests.findIndex(
                  (t) => t.id === test.id,
                );
                if (originalIndex !== -1) {
                  activeTests.splice(originalIndex, 0, test);
                } else {
                  activeTests.push(test);
                }
              }
            }
          }

          return { ...sample, activeTests, inactiveTests };
        }
        return sample;
      });

      return { ...prev, orderableTestList };
    });

    setJsonChangeList((prev) => {
      let activateTest = [...prev.activateTest];
      let deactivateTest = [...prev.deactivateTest];

      const originalState = testOrderabilityData.orderableTestList.find(
        (sample) => sample.sampleType.id === sampleTypeId,
      );

      const isOriginallyInactive = originalState.inactiveTests.some(
        (t) => t.id === test.id,
      );

      if (isChecked === false) {
        if (!isOriginallyInactive) {
          if (!deactivateTest.some((item) => item.id === test.id)) {
            deactivateTest.push({ id: test.id });
          }
        } else {
          activateTest = activateTest.filter((item) => item.id !== test.id);
          deactivateTest = deactivateTest.filter((item) => item.id !== test.id);
        }
      } else {
        if (isOriginallyInactive) {
          if (!activateTest.some((item) => item.id === test.id)) {
            activateTest.push({ id: test.id });
          }
        } else {
          activateTest = activateTest.filter((item) => item.id !== test.id);
          deactivateTest = deactivateTest.filter((item) => item.id !== test.id);
        }
      }

      return { activateTest, deactivateTest };
    });
  };

  function handleTestOrderabilityData(res) {
    if (!res) {
      setIsLoading(true);
    } else {
      setTestOrderabilityData(res);
      setChangedTestOrderabilityData(res);
    }
  }

  function testOrderabilityPostCall() {
    setIsLoading(true);
    postToOpenElisServerJsonResponse(
      `/rest/TestOrderability`,
      JSON.stringify({
        jsonChangeList: JSON.stringify({
          activateTest: JSON.stringify(jsonChangeList.activateTest),
          deactivateTest: JSON.stringify(jsonChangeList.deactivateTest),
        }),
      }),
      (res) => testOrderabilityPostCallback(res),
    );
  }

  function testOrderabilityPostCallback(res) {
    if (res) {
      setIsLoading(false);
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
      setTimeout(() => {
        window.location.reload();
      }, 200);
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
      setTimeout(() => {
        window.location.reload();
      }, 200);
    }
  }

  useEffect(() => {
    componentMounted.current = true;
    setIsLoading(true);
    getFromOpenElisServer(`/rest/TestOrderability`, (res) => {
      handleTestOrderabilityData(res);
    });
    return () => {
      componentMounted.current = false;
      setIsLoading(false);
    };
  }, []);

  if (!isLoading) {
    return (
      <>
        <Loading />
      </>
    );
  }

  return (
    <>
      {notificationVisible && <AlertDialog />}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <div className="orderLegendBody">
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Heading>
                  <FormattedMessage id="configuration.test.orderable" />
                </Heading>
              </Section>
            </Column>
          </Grid>
          <br />
          <hr />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Section>
                  <Section>
                    <Section>
                      <Heading>
                        <FormattedMessage id="instructions.test.order" />
                      </Heading>
                    </Section>
                  </Section>
                </Section>
              </Section>
            </Column>
          </Grid>
          <hr />
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Button
                disabled={
                  jsonChangeList?.activateTest?.length === 0 &&
                  jsonChangeList?.deactivateTest?.length === 0
                }
                onClick={() => {
                  setIsConfirmModalOpen(true);
                }}
                type="button"
              >
                <FormattedMessage id="label.button.submit" />
              </Button>{" "}
              <Button
                onClick={() =>
                  window.location.assign("/admin#TestOrderability")
                }
                kind="tertiary"
                type="button"
              >
                <FormattedMessage id="label.button.cancel" />
              </Button>
            </Column>
          </Grid>
          <br />
          {changedTestOrderabilityData?.orderableTestList &&
          changedTestOrderabilityData?.orderableTestList.length > 0 ? (
            <Grid fullWidth={true}>
              {changedTestOrderabilityData?.orderableTestList?.map((sample) => (
                <>
                  <Column lg={16} md={8} sm={4} key={sample.sampleType.id}>
                    <Section>
                      <Section>
                        <Section>
                          <Heading>{sample.sampleType.value}</Heading>
                        </Section>
                      </Section>
                    </Section>
                  </Column>
                  {[
                    ...testOrderabilityData.orderableTestList
                      .filter((s) => s.sampleType.id === sample.sampleType.id)
                      .flatMap((s) => [...s.activeTests, ...s.inactiveTests]),
                  ].map((test) => {
                    const isActive = sample.activeTests.some(
                      (t) => t.id === test.id,
                    );
                    return (
                      <Column
                        lg={4}
                        md={4}
                        sm={4}
                        key={`${sample.sampleType.id}-${test.id}`}
                      >
                        <Checkbox
                          id={`${sample.sampleType.id}-${test.id}`}
                          labelText={test.value}
                          checked={isActive}
                          onChange={(_, { checked }) => {
                            checked
                              ? handleActiveTestsCheckboxChange(
                                  test,
                                  sample.sampleType.id,
                                  checked,
                                )
                              : handleInactiveTestsCheckboxChange(
                                  test,
                                  sample.sampleType.id,
                                  checked,
                                );
                          }}
                        />
                      </Column>
                    );
                  })}
                </>
              ))}
            </Grid>
          ) : (
            <></>
          )}
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Button
                disabled={
                  jsonChangeList?.activateTest?.length === 0 &&
                  jsonChangeList?.deactivateTest?.length === 0
                }
                onClick={() => {
                  setIsConfirmModalOpen(true);
                }}
                type="button"
              >
                <FormattedMessage id="label.button.submit" />
              </Button>{" "}
              <Button
                onClick={() =>
                  window.location.assign("/admin#TestOrderability")
                }
                kind="tertiary"
                type="button"
              >
                <FormattedMessage id="label.button.cancel" />
              </Button>
            </Column>
          </Grid>
        </div>
      </div>

      <Modal
        open={isConfirmModalOpen}
        size="md"
        modalHeading={<FormattedMessage id="label.test.order.confirm" />}
        primaryButtonText={<FormattedMessage id="column.name.accept" />}
        secondaryButtonText={<FormattedMessage id="back.action.button" />}
        onRequestSubmit={() => {
          setIsConfirmModalOpen(false);
          testOrderabilityPostCall();
        }}
        onRequestClose={() => {
          setIsConfirmModalOpen(false);
        }}
      >
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <FormattedMessage id="instructions.test.activation.confirm" />
            <Section>
              <Section>
                <Section>
                  <Section>
                    <Heading>
                      <FormattedMessage id="label.test.orderable" />
                    </Heading>
                  </Section>
                </Section>
              </Section>
            </Section>
            <br />
            <Section>
              {(() => {
                const groupedTests = {};

                jsonChangeList.activateTest.forEach((test) => {
                  changedTestOrderabilityData?.orderableTestList.forEach(
                    (sample) => {
                      const foundTest = sample.activeTests.find(
                        (t) => t.id === test.id,
                      );
                      if (foundTest) {
                        if (!groupedTests[sample.sampleType.value]) {
                          groupedTests[sample.sampleType.value] = [];
                        }
                        groupedTests[sample.sampleType.value].push(
                          foundTest.value,
                        );
                      }
                    },
                  );
                });

                return Object.entries(groupedTests).map(
                  ([sampleType, tests]) => (
                    <Grid key={sampleType}>
                      <Column lg={16} md={8} sm={4}>
                        <Section>
                          <Section>
                            <Section>
                              <Heading>{sampleType}</Heading>
                            </Section>
                          </Section>
                        </Section>
                      </Column>
                      {tests.map((testName, index) => (
                        <Column lg={4} md={4} sm={4} key={index}>
                          <span key={index}>{testName}</span>
                        </Column>
                      ))}
                    </Grid>
                  ),
                );
              })()}
            </Section>
            <br />
            <Section>
              <Section>
                <Section>
                  <Section>
                    <Heading>
                      <FormattedMessage id="label.test.unorderable" />
                    </Heading>
                  </Section>
                </Section>
              </Section>
            </Section>
            <br />
            <Section>
              {(() => {
                const groupedInactiveTests = {};

                jsonChangeList.deactivateTest.forEach((test) => {
                  changedTestOrderabilityData?.orderableTestList.forEach(
                    (sample) => {
                      const foundTest = sample.inactiveTests.find(
                        (t) => t.id === test.id,
                      );
                      if (foundTest) {
                        if (!groupedInactiveTests[sample.sampleType.value]) {
                          groupedInactiveTests[sample.sampleType.value] = [];
                        }
                        groupedInactiveTests[sample.sampleType.value].push(
                          foundTest.value,
                        );
                      }
                    },
                  );
                });

                return Object.entries(groupedInactiveTests).map(
                  ([sampleType, tests]) => (
                    <Grid key={sampleType}>
                      <Column lg={16} md={8} sm={4}>
                        <Section>
                          <Section>
                            <Section>
                              <Heading>{sampleType}</Heading>
                            </Section>
                          </Section>
                        </Section>
                      </Column>
                      {tests.map((testName, index) => (
                        <Column lg={4} md={4} sm={4} key={index}>
                          <span key={index}>{testName}</span>
                        </Column>
                      ))}
                    </Grid>
                  ),
                );
              })()}
            </Section>
          </Column>
        </Grid>
      </Modal>
    </>
  );
}

export default injectIntl(TestOrderability);
