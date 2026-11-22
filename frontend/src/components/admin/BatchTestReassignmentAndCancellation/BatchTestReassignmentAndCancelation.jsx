import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Grid,
  Column,
  Section,
  Heading,
  Button,
  Loading,
  Checkbox,
  Tag,
} from "@carbon/react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import { NotificationContext } from "../../layout/Layout";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import SearchableSelect from "../../common/SearchableSelect";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "configuration.batch.test.reassignment",
    link: "/MasterListsPage/batchTestReassignment",
  },
];

function BatchTestReassignmentAndCancelation() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const componentMounted = useRef(false);
  const intl = useIntl();
  const resetToDefault = () => {
    setChangesToShow(false);
    setSaveButton(true);
    setIsLoading(true);
    setCurrentTest(true);
    setReplaceWith(true);
    setBatchTestGet(null);
    setBatchTestPost(null);
    setSampleTypeListShow([]);
    setSampleTypeToGetId(null);
    setSampleTypeToGetIdData({});
    setSampleTypeToGetIdDataTag({});
    setSampleTestTypeToGetPending(null);
    setSampleTypeTestIdToGetIdPendingData({});
    setSampleTestTypeToGetTagList([]);
    setJsonWad({
      current: "",
      sampleType: "",
      changeNotStarted: [],
      changeTechReject: [],
      changeBioReject: [],
      changeNotValidated: [],
      noChangeNotStarted: [],
      noChangeTechReject: [],
      noChangeBioReject: [],
      noChangeNotValidated: [],
    });

    getFromOpenElisServer(
      `/rest/BatchTestReassignment`,
      handleBatchTestReassignment,
    );
  };

  const [saveButton, setSaveButton] = useState(true);
  const [isLoading, setIsLoading] = useState(true);
  const [currentTest, setCurrentTest] = useState(true);
  const [replaceWith, setReplaceWith] = useState(true);
  const [batchTestGet, setBatchTestGet] = useState(null);
  const [batchTestPost, setBatchTestPost] = useState(null);
  const [sampleTypeListShow, setSampleTypeListShow] = useState([]);
  const [sampleTypeToGetId, setSampleTypeToGetId] = useState(null);
  const [sampleTypeToGetIdData, setSampleTypeToGetIdData] = useState({});
  const [sampleTypeToGetIdDataTag, setSampleTypeToGetIdDataTag] = useState({});
  const [sampleTypeTestIdToGetIdPending, setSampleTestTypeToGetPending] =
    useState(null);
  const [
    sampleTypeTestIdToGetIdPendingData,
    setSampleTypeTestIdToGetIdPendingData,
  ] = useState({});
  const [sampleTestTypeToGetTagList, setSampleTestTypeToGetTagList] = useState(
    [],
  );
  const [jsonWad, setJsonWad] = useState({
    current: "",
    sampleType: "",
    changeNotStarted: [],
    changeTechReject: [],
    changeBioReject: [],
    changeNotValidated: [],
    noChangeNotStarted: [],
    noChangeTechReject: [],
    noChangeBioReject: [],
    noChangeNotValidated: [],
  });
  const [changesToShow, setChangesToShow] = useState(false);
  const [replacePicks, setReplacePicks] = useState(0);

  useEffect(() => {
    if (replacePicks > 0) {
      document.getElementById("selectSampleType0")?.focus();
    }
  }, [replacePicks]);

  useEffect(() => {
    componentMounted.current = true;
    setIsLoading(true);
    getFromOpenElisServer(
      `/rest/BatchTestReassignment`,
      handleBatchTestReassignment,
    );
    return () => {
      componentMounted.current = false;
      setIsLoading(false);
    };
  }, []);

  const handleBatchTestReassignment = (res) => {
    if (!res) {
      setIsLoading(true);
    } else {
      setBatchTestGet(res);
    }
  };

  useEffect(() => {
    if (!sampleTypeToGetId) {
      return;
    }
    const handleBatchTestReassignmentSampleTypeHandle = (res) => {
      if (!res) {
        setIsLoading(true);
      } else {
        setSampleTypeToGetIdData((prevSampleTypeToGetIdData) => ({
          ...prevSampleTypeToGetIdData,
          tests: res.tests || [],
        }));
        setSampleTypeToGetIdDataTag((prevSampleTypeToGetIdDataTag) => ({
          ...prevSampleTypeToGetIdDataTag,
          tests: res.tests || [],
        }));
      }
    };

    getFromOpenElisServer(
      `/rest/AllTestsForSampleTypeProvider?sampleTypeId=${encodeURIComponent(sampleTypeToGetId)}`,
      handleBatchTestReassignmentSampleTypeHandle,
    );
  }, [sampleTypeToGetId]);

  useEffect(() => {
    if (!sampleTypeTestIdToGetIdPending) {
      return;
    }
    const handleBatchTestReassignmentSampleTypeTestHandle = (res) => {
      if (!res) {
        setIsLoading(true);
      } else {
        setSampleTypeTestIdToGetIdPendingData(res);
      }
    };

    getFromOpenElisServer(
      `/rest/getPendingAnalysisForTestProvider?testId=${encodeURIComponent(sampleTypeTestIdToGetIdPending)}`,
      handleBatchTestReassignmentSampleTypeTestHandle,
    );
  }, [sampleTypeTestIdToGetIdPending]);

  useEffect(() => {
    if (sampleTypeTestIdToGetIdPendingData) {
      const {
        notStarted,
        technicianRejection,
        biologistRejection,
        notValidated,
      } = sampleTypeTestIdToGetIdPendingData;

      setJsonWad((prevJsonWad) => ({
        ...prevJsonWad,
        changeNotStarted: notStarted ? notStarted.map((item) => item.id) : [],
        noChangeNotStarted: [],
        changeTechReject: technicianRejection
          ? technicianRejection.map((item) => item.id)
          : [],
        noChangeTechReject: [],
        changeBioReject: biologistRejection
          ? biologistRejection.map((item) => item.id)
          : [],
        noChangeBioReject: [],
        changeNotValidated: notValidated
          ? notValidated.map((item) => item.id)
          : [],
        noChangeNotValidated: [],
      }));
    }
  }, [sampleTypeTestIdToGetIdPendingData]);

  useEffect(() => {
    if (batchTestGet) {
      const BatchTestReassignmentInfoToPost = {
        formName: batchTestGet.formName,
        formMethod: batchTestGet.formMethod,
        cancelAction: batchTestGet.cancelAction,
        submitOnCancel: batchTestGet.submitOnCancel,
        cancelMethod: batchTestGet.cancelMethod,
        sampleList: batchTestGet.sampleList,
        statusChangedSampleType: batchTestGet.statusChangedSampleType,
        statusChangedCurrentTest: batchTestGet.statusChangedCurrentTest,
        statusChangedNextTest: batchTestGet.statusChangedNextTest,
        jsonWad: batchTestGet.jsonWad,
      };
      setBatchTestPost(BatchTestReassignmentInfoToPost);
      setSampleTypeListShow(batchTestGet.sampleList || []);
    }
  }, [batchTestGet]);

  function batchTestReassignmentPostCall() {
    setIsLoading(true);
    postToOpenElisServerJsonResponse(
      `/rest/BatchTestReassignment`,
      JSON.stringify(batchTestPost),
      (res) => {
        sampleTypeListPostCallback(res);
      },
    );
  }

  function sampleTypeListPostCallback(res) {
    if (res && !res.error) {
      setIsLoading(false);
      addNotification({
        title: intl.formatMessage({
          id: "notification.title",
        }),
        message: intl.formatMessage({
          id: "save.success",
        }),
        kind: NotificationKinds.success,
      });
      setNotificationVisible(true);
      resetToDefault();
    } else {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: res ? "error.save.msg" : "server.error.msg",
        }),
      });
      setNotificationVisible(true);
    }
  }
  const capitalizeFirstLetter = (string) => {
    return string.charAt(0).toUpperCase() + string.slice(1);
  };

  const handleSelectAll = (sectionKey) => {
    const sectionMap = {
      bioReject: "biologistRejection",
      techReject: "technicianRejection",
    };

    const fullSectionKey = sectionMap[sectionKey] || sectionKey;
    const allIds = sampleTypeTestIdToGetIdPendingData[fullSectionKey].map(
      (item) => item.id,
    );
    const isAllChecked =
      jsonWad[`change${capitalizeFirstLetter(sectionKey)}`].length ===
      allIds.length;

    if (isAllChecked) {
      setJsonWad((prevJsonWad) => ({
        ...prevJsonWad,
        [`noChange${capitalizeFirstLetter(sectionKey)}`]: allIds,
        [`change${capitalizeFirstLetter(sectionKey)}`]: [],
      }));
    } else {
      setJsonWad((prevJsonWad) => ({
        ...prevJsonWad,
        [`change${capitalizeFirstLetter(sectionKey)}`]: allIds,
        [`noChange${capitalizeFirstLetter(sectionKey)}`]: [],
      }));
    }
    setSaveButton(false);
  };

  const handleCheckboxChange = (id, sectionKey) => {
    setJsonWad((prevJsonWad) => {
      let updatedArray =
        prevJsonWad[`change${capitalizeFirstLetter(sectionKey)}`].slice();
      const index = updatedArray.indexOf(id);
      if (index === -1) {
        updatedArray.push(id);
      } else {
        updatedArray.splice(index, 1);
      }
      let isChecked = updatedArray.includes(id);
      let noChangeArray =
        prevJsonWad[`noChange${capitalizeFirstLetter(sectionKey)}`];
      const noChangeIndex = noChangeArray.indexOf(id);

      if (isChecked && noChangeIndex !== -1) {
        noChangeArray.splice(noChangeIndex, 1);
      } else if (!isChecked && noChangeIndex === -1) {
        noChangeArray.push(id);
      }

      return {
        ...prevJsonWad,
        [`change${capitalizeFirstLetter(sectionKey)}`]: updatedArray,
        [`noChange${capitalizeFirstLetter(sectionKey)}`]: noChangeArray,
      };
    });
  };

  function handleSampleTypeListSelectId(sampleTypeId) {
    setSampleTypeToGetId(sampleTypeId || null);
    setSampleTypeToGetIdData({});
    setSampleTypeToGetIdDataTag({});
    handleSampleTypeListSelectIdTest("");
    setSampleTestTypeToGetTagList([]);
    setJsonWad((prevJsonWad) => ({ ...prevJsonWad, replace: [] }));
    setBatchTestPost((prevUserDataPost) => ({
      ...prevUserDataPost,
      statusChangedSampleType: sampleTypeId,
    }));
  }

  function handleSampleTypeListSelectIdTest(testId) {
    setSaveButton(!testId);
    setSampleTestTypeToGetPending(testId || null);
    if (!testId) {
      setSampleTypeTestIdToGetIdPendingData({});
    }
    setBatchTestPost((prevUserDataPost) => ({
      ...prevUserDataPost,
      statusChangedCurrentTest: testId,
    }));
  }

  const testOptions = (tests, includeInactive) =>
    (tests || [])
      .filter((test) => includeInactive || test.isActive === "Y")
      .map((test) => ({ id: test.id, value: test.name }));

  const handleSampleTypeListSelectIdTestTag = (selectedTestId) => {
    if (!selectedTestId) {
      return;
    }
    setReplacePicks((picks) => picks + 1);
    const testName = (sampleTypeToGetIdDataTag.tests || []).find(
      (test) => test.id === selectedTestId,
    )?.name;

    const existingIndex = sampleTestTypeToGetTagList.findIndex(
      (item) => item.id === selectedTestId,
    );

    let updatedList;
    if (existingIndex !== -1) {
      updatedList = [...sampleTestTypeToGetTagList];
      updatedList.splice(existingIndex, 1);
      setSampleTestTypeToGetTagList(updatedList);
    } else {
      const selectedTest = {
        id: selectedTestId,
        name: testName,
      };
      updatedList = [...sampleTestTypeToGetTagList, selectedTest];
      setSampleTestTypeToGetTagList(updatedList);
    }

    const updatedReplace = updatedList.map((item) => item.id);
    setJsonWad((prevJsonWad) => ({
      ...prevJsonWad,
      replace: updatedReplace,
    }));
  };

  function handleRemoveSampleTypeListSelectIdTestTag(indexToRemove) {
    setSampleTestTypeToGetTagList((prevTags) => {
      const updatedTags = prevTags.filter(
        (_, index) => index !== indexToRemove,
      );

      const updatedReplace = updatedTags.map((item) => item.id);
      setJsonWad((prevJsonWad) => ({
        ...prevJsonWad,
        replace: updatedReplace,
      }));

      return updatedTags;
    });
  }

  useEffect(() => {
    if (
      batchTestPost &&
      sampleTypeToGetIdDataTag &&
      sampleTypeToGetIdDataTag.tests &&
      sampleTypeListShow
    ) {
      const sampleTypeValue = batchTestPost.statusChangedSampleType || "";
      const currentTest = batchTestPost.statusChangedCurrentTest || "";
      const sampleTypeItem = sampleTypeListShow.find(
        (item) => item.id === sampleTypeValue.toString(),
      );
      const sampleTypeValueToSet = sampleTypeItem ? sampleTypeItem.value : "";
      const currentTestItem = sampleTypeToGetIdDataTag.tests.find(
        (test) => test.id === currentTest.toString(),
      );
      const currentTestNameToSet = currentTestItem ? currentTestItem.name : "";

      setJsonWad((prevJsonWad) => ({
        ...prevJsonWad,
        sampleType: sampleTypeValueToSet,
        current: currentTestNameToSet,
      }));
    }
  }, [batchTestPost, sampleTypeToGetIdDataTag, sampleTypeListShow]);

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
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Section>
                <Heading>
                  <FormattedMessage id="configuration.batch.test.reassignment" />
                </Heading>
              </Section>
            </Section>
          </Column>
        </Grid>
        <div className="orderLegendBody">
          <Grid fullWidth={true}>
            <Column lg={8} md={4} sm={4}>
              <Section>
                <Section>
                  <Section>
                    <Heading>
                      <FormattedMessage id="sample.select.type" />
                    </Heading>
                  </Section>
                </Section>
              </Section>
            </Column>
          </Grid>
          <hr />
          <br />
          <Grid fullWidth={true}>
            <Column lg={4} md={4} sm={4}>
              <SearchableSelect
                id="selectSampleType"
                labelText={intl.formatMessage({ id: "sample.type" })}
                placeholder={intl.formatMessage({
                  id: "configuration.batch.test.reassignment.search.sampleType",
                })}
                options={sampleTypeListShow}
                value={sampleTypeToGetId ?? ""}
                onChange={handleSampleTypeListSelectId}
              />
            </Column>
          </Grid>
          <br />
          <Grid fullWidth={true}>
            <Column lg={8} md={4} sm={4}>
              <SearchableSelect
                id="selectSampleType1"
                labelText={intl.formatMessage({ id: "label.currentTest" })}
                placeholder={intl.formatMessage({
                  id: "configuration.batch.test.reassignment.search.currentTest",
                })}
                options={testOptions(sampleTypeToGetIdData.tests, currentTest)}
                value={sampleTypeTestIdToGetIdPending ?? ""}
                onChange={handleSampleTypeListSelectIdTest}
              />
              <Checkbox
                id={`currentTest`}
                labelText={intl.formatMessage({
                  id: "label.includeInactiveTests",
                })}
                checked={currentTest}
                onChange={() => {
                  setCurrentTest(!currentTest);
                }}
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <SearchableSelect
                key={`replaceWith-${replacePicks}`}
                id="selectSampleType0"
                labelText={intl.formatMessage({ id: "label.replaceWith" })}
                placeholder={intl.formatMessage({
                  id: "configuration.batch.test.reassignment.search.replaceWith",
                })}
                options={testOptions(sampleTypeToGetIdDataTag.tests).filter(
                  (option) =>
                    !sampleTestTypeToGetTagList.some(
                      (tag) => tag.id === option.id,
                    ),
                )}
                value=""
                disabled={replaceWith}
                onChange={handleSampleTypeListSelectIdTestTag}
              />
              <Checkbox
                id={`replaceWith`}
                labelText={intl.formatMessage({
                  id: "label.cancel.test.no.replace",
                })}
                checked={replaceWith}
                onChange={() => {
                  setReplaceWith(!replaceWith);
                  setSampleTestTypeToGetTagList([]);
                  setJsonWad((prevJsonWad) => ({
                    ...prevJsonWad,
                    replace: [],
                  }));
                }}
              />

              <div
                className={"searchTestText"}
                style={{ marginBottom: "1.188rem" }}
              >
                {sampleTestTypeToGetTagList &&
                sampleTestTypeToGetTagList.length ? (
                  <>
                    {sampleTestTypeToGetTagList.map((section, index) => (
                      <Tag
                        filter
                        key={`testTags_` + index}
                        onClose={() =>
                          handleRemoveSampleTypeListSelectIdTestTag(index)
                        }
                        style={{ marginRight: "0.5rem" }}
                        type={"red"}
                      >
                        {section.name}
                      </Tag>
                    ))}
                  </>
                ) : (
                  <></>
                )}
              </div>
            </Column>
          </Grid>
          <br />
          <Grid fullWidth={true}>
            <Column lg={8} md={4} sm={4}>
              <FormattedMessage id="label.checkedWillBeModified" />
            </Column>
          </Grid>
          <br />
          <Grid fullWidth={true}>
            <Column lg={4} md={4} sm={2}>
              {sampleTypeTestIdToGetIdPendingData &&
              sampleTypeTestIdToGetIdPendingData.notStarted ? (
                <>
                  <FormattedMessage id="label.analysisNotStarted" />
                  <br />
                  <Checkbox
                    id={`selectAll0`}
                    labelText={intl.formatMessage({
                      id: "referral.print.selected.patient.reports.selectall.button",
                    })}
                    checked={sampleTypeTestIdToGetIdPendingData.notStarted.every(
                      (item) => jsonWad.changeNotStarted.includes(item.id),
                    )}
                    onChange={() => {
                      handleSelectAll("notStarted");
                    }}
                  />
                </>
              ) : (
                <></>
              )}
              {sampleTypeTestIdToGetIdPendingData &&
                sampleTypeTestIdToGetIdPendingData.notStarted &&
                sampleTypeTestIdToGetIdPendingData.notStarted.map(
                  (item, index) => (
                    <div key={index}>
                      <Checkbox
                        id={`notStartedCheckbox_${index}`}
                        value={item.id}
                        labelText={item.labNo}
                        checked={jsonWad.changeNotStarted.includes(item.id)}
                        onChange={() => {
                          handleCheckboxChange(item.id, "notStarted");
                        }}
                      />
                    </div>
                  ),
                )}
              <br />
            </Column>
            <Column lg={4} md={4} sm={2}>
              {sampleTypeTestIdToGetIdPendingData &&
              sampleTypeTestIdToGetIdPendingData.technicianRejection ? (
                <>
                  <FormattedMessage id="label.rejectedByTechnician" />
                  <br />
                  <Checkbox
                    id={`selectAll1`}
                    labelText={intl.formatMessage({
                      id: "referral.print.selected.patient.reports.selectall.button",
                    })}
                    checked={sampleTypeTestIdToGetIdPendingData.technicianRejection.every(
                      (item) => jsonWad.changeTechReject.includes(item.id),
                    )}
                    onChange={() => {
                      handleSelectAll("techReject");
                    }}
                  />
                </>
              ) : (
                <></>
              )}
              {sampleTypeTestIdToGetIdPendingData &&
                sampleTypeTestIdToGetIdPendingData.technicianRejection &&
                sampleTypeTestIdToGetIdPendingData.technicianRejection.map(
                  (item, index) => (
                    <div key={index}>
                      <Checkbox
                        id={`technicianRejectionCheckbox_${index}`}
                        value={item.id}
                        labelText={item.labNo}
                        checked={jsonWad.changeTechReject.includes(item.id)}
                        onChange={() => {
                          handleCheckboxChange(item.id, "techReject");
                        }}
                      />
                    </div>
                  ),
                )}
              <br />
            </Column>
            <Column lg={4} md={4} sm={2}>
              {sampleTypeTestIdToGetIdPendingData &&
              sampleTypeTestIdToGetIdPendingData.biologistRejection ? (
                <>
                  <FormattedMessage id="label.rejectedByBiologist" />
                  <br />
                  <Checkbox
                    id={`selectAll2`}
                    labelText={intl.formatMessage({
                      id: "referral.print.selected.patient.reports.selectall.button",
                    })}
                    checked={sampleTypeTestIdToGetIdPendingData.biologistRejection.every(
                      (item) => jsonWad.changeBioReject.includes(item.id),
                    )}
                    onChange={() => {
                      handleSelectAll("bioReject");
                    }}
                  />
                </>
              ) : (
                <></>
              )}
              {sampleTypeTestIdToGetIdPendingData &&
                sampleTypeTestIdToGetIdPendingData.biologistRejection &&
                sampleTypeTestIdToGetIdPendingData.biologistRejection.map(
                  (item, index) => (
                    <div key={index}>
                      <Checkbox
                        id={`biologistRejectionCheckbox_${index}`}
                        value={item.id}
                        labelText={item.labNo}
                        checked={jsonWad.changeBioReject.includes(item.id)}
                        onChange={() => {
                          handleCheckboxChange(item.id, "bioReject");
                        }}
                      />
                    </div>
                  ),
                )}
              <br />
            </Column>
            <Column lg={4} md={4} sm={2}>
              {sampleTypeTestIdToGetIdPendingData &&
              sampleTypeTestIdToGetIdPendingData.notValidated ? (
                <>
                  <FormattedMessage id="label.notValidated" />
                  <br />
                  <Checkbox
                    id={`selectAll3`}
                    labelText={intl.formatMessage({
                      id: "referral.print.selected.patient.reports.selectall.button",
                    })}
                    checked={sampleTypeTestIdToGetIdPendingData.notValidated.every(
                      (item) => jsonWad.changeNotValidated.includes(item.id),
                    )}
                    onChange={() => {
                      handleSelectAll("notValidated");
                    }}
                  />
                </>
              ) : (
                <></>
              )}
              {sampleTypeTestIdToGetIdPendingData &&
                sampleTypeTestIdToGetIdPendingData.notValidated &&
                sampleTypeTestIdToGetIdPendingData.notValidated.map(
                  (item, index) => (
                    <div key={index}>
                      <Checkbox
                        id={`notValidatedCheckbox_${index}`}
                        value={item.id}
                        labelText={item.labNo}
                        checked={jsonWad.changeNotValidated.includes(item.id)}
                        onChange={() => {
                          handleCheckboxChange(item.id, "notValidated");
                        }}
                      />
                    </div>
                  ),
                )}
              <br />
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Button
                data-cy="okButton"
                disabled={saveButton}
                onClick={() => {
                  setChangesToShow(true);
                  setBatchTestPost((prevBatchTestPost) => ({
                    ...prevBatchTestPost,
                    jsonWad: JSON.stringify(jsonWad),
                  }));
                }}
                type="button"
              >
                <FormattedMessage id="label.button.ok" />
              </Button>{" "}
              <Button
                data-cy="cancelButton"
                onClick={resetToDefault}
                kind="tertiary"
                type="button"
              >
                <FormattedMessage id="label.button.cancel" />
              </Button>
            </Column>
          </Grid>
          {changesToShow ? (
            <>
              <br />
              <hr />
              <br />
              <Grid fullWidth={true} className="gridBoundary">
                <Column lg={16} md={8} sm={4}>
                  <FormattedMessage id="field.sampleType" /> :{" "}
                  {jsonWad.sampleType}
                  <br />
                  <br />
                  {sampleTestTypeToGetTagList.length ? (
                    <FormattedMessage
                      id="label.test.batch.replace.with"
                      values={{
                        current: jsonWad.current,
                        replacements: sampleTestTypeToGetTagList
                          .map((tag) => tag.name)
                          .join(", "),
                      }}
                    />
                  ) : (
                    <>
                      <FormattedMessage id="label.test.batch.cancel.start" />{" "}
                      {jsonWad.current}
                    </>
                  )}{" "}
                  <FormattedMessage id="label.test.batch.cancel.finish" />
                  <br />
                  <br />
                  <hr />
                </Column>
                <Column lg={4} md={4} sm={2}>
                  <br />
                  <FormattedMessage id="label.analysisNotStarted" />
                  {jsonWad.changeNotStarted.map((id) => {
                    const item =
                      sampleTypeTestIdToGetIdPendingData.notStarted.find(
                        (data) => data.id === id,
                      );
                    return (
                      item && (
                        <div key={id}>
                          <br />
                          {item.labNo}
                          <br />
                        </div>
                      )
                    );
                  })}
                </Column>
                <Column lg={4} md={4} sm={2}>
                  <br />
                  <FormattedMessage id="label.rejectedByTechnician" />
                  {jsonWad.changeTechReject.map((id) => {
                    const item =
                      sampleTypeTestIdToGetIdPendingData.technicianRejection.find(
                        (data) => data.id === id,
                      );
                    return (
                      item && (
                        <div key={id}>
                          <br />
                          {item.labNo}
                          <br />
                        </div>
                      )
                    );
                  })}
                </Column>
                <Column lg={4} md={4} sm={2}>
                  <br />
                  <FormattedMessage id="label.rejectedByBiologist" />
                  {jsonWad.changeBioReject.map((id) => {
                    const item =
                      sampleTypeTestIdToGetIdPendingData.biologistRejection.find(
                        (data) => data.id === id,
                      );
                    return (
                      item && (
                        <div key={id}>
                          <br />
                          {item.labNo}
                          <br />
                        </div>
                      )
                    );
                  })}
                </Column>
                <Column lg={4} md={4} sm={2}>
                  <br />
                  <FormattedMessage id="label.notValidated" />
                  {jsonWad.changeNotValidated.map((id) => {
                    const item =
                      sampleTypeTestIdToGetIdPendingData.notValidated.find(
                        (data) => data.id === id,
                      );
                    return (
                      item && (
                        <div key={id}>
                          <br />
                          {item.labNo}
                          <br />
                        </div>
                      )
                    );
                  })}
                </Column>
                <Column lg={16} md={8} sm={4}>
                  <br />
                  <hr />
                  <FormattedMessage id="label.test.batch.replace.start" />{" "}
                  {jsonWad.current}{" "}
                  <FormattedMessage id="label.test.batch.no.change.finish" />
                  <br />
                  <br />
                </Column>
                <Column lg={4} md={4} sm={2}>
                  <br />
                  <FormattedMessage id="label.analysisNotStarted" />
                  {jsonWad.noChangeNotStarted.map((id) => {
                    const item =
                      sampleTypeTestIdToGetIdPendingData.notStarted.find(
                        (data) => data.id === id,
                      );
                    return (
                      item && (
                        <div key={id}>
                          <br />
                          {item.labNo}
                          <br />
                        </div>
                      )
                    );
                  })}
                </Column>
                <Column lg={4} md={4} sm={2}>
                  <br />
                  <FormattedMessage id="label.rejectedByTechnician" />
                  {jsonWad.noChangeTechReject.map((id) => {
                    const item =
                      sampleTypeTestIdToGetIdPendingData.technicianRejection.find(
                        (data) => data.id === id,
                      );
                    return (
                      item && (
                        <div key={id}>
                          <br />
                          {item.labNo}
                          <br />
                        </div>
                      )
                    );
                  })}
                </Column>
                <Column lg={4} md={4} sm={2}>
                  <br />
                  <FormattedMessage id="label.rejectedByBiologist" />
                  {jsonWad.noChangeBioReject.map((id) => {
                    const item =
                      sampleTypeTestIdToGetIdPendingData.biologistRejection.find(
                        (data) => data.id === id,
                      );
                    return (
                      item && (
                        <div key={id}>
                          <br />
                          {item.labNo}
                          <br />
                        </div>
                      )
                    );
                  })}
                </Column>
                <Column lg={4} md={4} sm={2}>
                  <br />
                  <FormattedMessage id="label.notValidated" />
                  {jsonWad.noChangeNotValidated.map((id) => {
                    const item =
                      sampleTypeTestIdToGetIdPendingData.notValidated.find(
                        (data) => data.id === id,
                      );
                    return (
                      item && (
                        <div key={id}>
                          <br />
                          {item.labNo}
                          <br />
                        </div>
                      )
                    );
                  })}
                </Column>
                <Column lg={16} md={8} sm={4}>
                  <br />
                  <br />
                  <hr />
                  <Button
                    disabled={saveButton}
                    onClick={() => {
                      setBatchTestPost((prevBatchTestPost) => ({
                        ...prevBatchTestPost,
                        jsonWad: JSON.stringify(jsonWad),
                      }));
                      batchTestReassignmentPostCall();
                    }}
                    type="button"
                  >
                    <FormattedMessage id="column.name.accept" />
                  </Button>{" "}
                  <Button
                    onClick={resetToDefault}
                    kind="tertiary"
                    type="button"
                  >
                    <FormattedMessage id="column.name.reject" />
                  </Button>
                </Column>
              </Grid>
            </>
          ) : (
            <></>
          )}
        </div>
      </div>
    </>
  );
}

export default injectIntl(BatchTestReassignmentAndCancelation);
