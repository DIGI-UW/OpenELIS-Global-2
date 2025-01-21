import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Grid,
  Column,
  Section,
  Heading,
  Form,
  TextInput,
  UnorderedList,
  ListItem,
  RadioButton,
  Button,
  Loading,
  Select,
  SelectItem,
  PasswordInput,
  Checkbox,
  FormGroup,
  Tag,
} from "@carbon/react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb.js";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification.js";
import {
  ConfigurationContext,
  NotificationContext,
} from "../../layout/Layout.js";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils.js";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "configuration.batch.test.reassignment",
    link: "/MasterListsPage#batchTestReassignment",
  },
];

function BatchTestReassignmentAndCancelation() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext);

  const componentMounted = useRef(false);
  const intl = useIntl();

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
    const handleBatchTestReassignmentSampleTypeHandle = (res) => {
      if (!res) {
        setIsLoading(true);
      } else {
        const extraCurrentTestObject = {
          name: "Select Current Test",
          id: "",
          isActive: "",
        };
        setSampleTypeToGetIdData((prevSampleTypeToGetIdData) => ({
          ...prevSampleTypeToGetIdData,
          tests: [extraCurrentTestObject, ...(res.tests || [])],
        }));
        const extraObject = {
          name: "Select Multi Tests",
          id: "",
          isActive: "",
        };
        setSampleTypeToGetIdDataTag((prevSampleTypeToGetIdDataTag) => ({
          ...prevSampleTypeToGetIdDataTag,
          tests: [extraObject, ...(res.tests || [])],
        }));
      }
    };

    getFromOpenElisServer(
      `/rest/AllTestsForSampleTypeProvider?sampleTypeId=${sampleTypeToGetId}`,
      handleBatchTestReassignmentSampleTypeHandle,
    );
  }, [sampleTypeToGetId]);

  useEffect(() => {
    const handleBatchTestReassignmentSampleTypeTestHandle = (res) => {
      if (!res) {
        setIsLoading(true);
      } else {
        setSampleTypeTestIdToGetIdPendingData(res);
      }
    };

    getFromOpenElisServer(
      `/rest/getPendingAnalysisForTestProvider?testId=${sampleTypeTestIdToGetIdPending}`,
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
      setSampleTypeListShow((prevSampleTypeListShow) => [
        ...prevSampleTypeListShow,
        { id: "0", value: "Select SampleType" },
        ...batchTestGet.sampleList,
      ]);
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

  function handleSampleTypeListSelectId(e) {
    setSampleTypeToGetId(e.target.value);
    setBatchTestPost((prevUserDataPost) => ({
      ...prevUserDataPost,
      statusChangedSampleType: e.target.value,
    }));
  }

  function handleSampleTypeListSelectIdTest(e) {
    setSaveButton(false);
    setSampleTestTypeToGetPending(e.target.value);
    setBatchTestPost((prevUserDataPost) => ({
      ...prevUserDataPost,
      statusChangedCurrentTest: e.target.value,
    }));
  }

  const handleSampleTypeListSelectIdTestTag = (e) => {
    const selectedTestId = e.target.value;
    const testName = e.target.options[e.target.selectedIndex].text;

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
              <Select
                id={`selectSampleType`}
                labelText={intl.formatMessage({ id: "sample.type" })}
                defaultValue={sampleTypeListShow ? sampleTypeListShow[0] : ""}
                onChange={(e) => handleSampleTypeListSelectId(e)}
              >
                {sampleTypeListShow && sampleTypeListShow.length > 0 ? (
                  sampleTypeListShow.map((section) => (
                    <SelectItem
                      key={section.id}
                      value={section.id}
                      text={section.value}
                    />
                  ))
                ) : (
                  <SelectItem key={``} value="" text="No options available" />
                )}
              </Select>
            </Column>
          </Grid>
          <br />
          <Grid fullWidth={true}>
            <Column lg={8} md={4} sm={4}>
              <FormattedMessage id="label.currentTest" />
              <br />
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
              <br />
              <Select
                key={`selectSampleType1`}
                id={`selectSampleType1`}
                hideLabel={true}
                defaultValue={
                  sampleTypeToGetIdData && sampleTypeToGetIdData.tests
                    ? sampleTypeToGetIdData.tests[0]
                    : ""
                }
                onChange={(e) => handleSampleTypeListSelectIdTest(e)}
              >
                {sampleTypeToGetIdData &&
                sampleTypeToGetIdData.tests &&
                sampleTypeToGetIdData.tests.length > 0 ? (
                  sampleTypeToGetIdData.tests.map((section) => (
                    <SelectItem
                      key={section.id}
                      value={section.id}
                      text={section.name}
                    />
                  ))
                ) : (
                  <SelectItem
                    key="no-option-available-selectSampleType"
                    value=""
                    text="No options available"
                  />
                )}
              </Select>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <FormattedMessage id="label.replaceWith" />
              <br />
              <Checkbox
                id={`replaceWith`}
                labelText={intl.formatMessage({
                  id: "label.cancel.test.no.replace",
                })}
                checked={replaceWith}
                onChange={() => {
                  setReplaceWith(!replaceWith);
                  if (replaceWith) {
                    setJsonWad((prevJsonWad) => ({
                      ...prevJsonWad,
                      replace: [],
                    }));
                  } else {
                    const selectedTestIds = sampleTestTypeToGetTagList
                      .map((item) => item.id)
                      .join(",");
                    setJsonWad((prevJsonWad) => ({
                      ...prevJsonWad,
                      replace: selectedTestIds,
                    }));
                  }
                }}
              />
              <br />
              <Select
                key={`selectSampleType0`}
                id={`selectSampleType0`}
                hideLabel={true}
                defaultValue={
                  sampleTypeToGetIdDataTag && sampleTypeToGetIdDataTag.tests
                    ? sampleTypeToGetIdDataTag.tests[0]
                    : ""
                }
                disabled={replaceWith}
                onChange={(e) => handleSampleTypeListSelectIdTestTag(e)}
              >
                {sampleTypeToGetIdDataTag &&
                sampleTypeToGetIdDataTag.tests &&
                sampleTypeToGetIdDataTag.tests.length > 0 ? (
                  sampleTypeToGetIdDataTag.tests.map((section) => (
                    <SelectItem
                      key={section.id}
                      value={section.id}
                      text={section.name}
                    />
                  ))
                ) : (
                  <SelectItem
                    key="no-option-available-selectSampleType0"
                    value=""
                    text="No options available"
                  />
                )}
              </Select>
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
                onClick={() =>
                  window.location.assign(
                    "/MasterListsPage#batchTestReassignment",
                  )
                }
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
                  <FormattedMessage id="label.test.batch.cancel.start" />{" "}
                  {jsonWad.current}{" "}
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
                    onClick={() =>
                      window.location.assign(
                        "/MasterListsPage#batchTestReassignment",
                      )
                    }
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
