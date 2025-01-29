import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Form,
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
  Checkbox,
  Row,
  FlexGrid,
  Tag,
  UnorderedList,
  ListItem,
  NumberInput,
  RadioButtonGroup,
  RadioButton,
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
import { id } from "date-fns/locale";
import { value } from "jsonpath";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "master.lists.page.test.management",
    link: "/MasterListsPage#testManagementConfigMenu",
  },
  {
    label: "configuration.test.add",
    link: "/MasterListsPage#TestAdd",
  },
];

function TestAdd() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const componentMounted = useRef(false);
  const [isLoading, setIsLoading] = useState(false);
  const [lonic, setLonic] = useState("");
  const [testAdd, setTestAdd] = useState({});
  const [ageRangeList, setAgeRangeList] = useState([]);
  const [gotSelectedAgeRangeList, setGotSelectedAgeRangeList] = useState([]);
  const [labUnitList, setLabUnitList] = useState([]);
  const [selectedLabUnitList, setSelectedLabUnitList] = useState({});
  const [panelList, setPanelList] = useState([]);
  const [panelListTag, setPanelListTag] = useState([]);
  const [uomList, setUomList] = useState([]);
  const [selectedUomList, setSelectedUomList] = useState({});
  const [resultTypeList, setResultTypeList] = useState([]);
  const [selectedResultTypeList, setSelectedResultTypeList] = useState({});
  const [sampleTypeList, setSampleTypeList] = useState([]);
  const [selectedSampleTypeList, setSelectedSampleTypeList] = useState([]);
  const [sampleTestTypeToGetTagList, setSampleTestTypeToGetTagList] = useState(
    [],
  );
  const [selectedSampleType, setSelectedSampleType] = useState([]);
  const [selectedSampleTypeResp, setSelectedSampleTypeResp] = useState([]);
  const [groupedDictionaryList, setGroupedDictionaryList] = useState([]);
  const [selectedGroupedDictionaryList, setSelectedGroupedDictionaryList] =
    useState([]);
  const [dictionaryList, setDictionaryList] = useState([]);
  const [dictionaryListTag, setDictionaryListTag] = useState([]);
  const [singleSelectDictionaryList, setSingleSelectDictionaryList] = useState(
    [],
  );
  const [multiSelectDictionaryList, setMultiSelectDictionaryList] = useState([
    { id: "0", value: "Multiple" },
  ]);
  const [multiSelectDictionaryListTag, setMultiSelectDictionaryListTag] =
    useState([]);
  const [sampleTypeSetupPage, setSampleTypeSetupPage] = useState(true);
  const [rangeSetupPage, setRangeSetupPage] = useState(true);
  const [onResultType, setOnResultType] = useState(true);
  const [existingTestSetupPage, setExistingTestSetupPage] = useState(true);
  const [finalSaveConfirmation, setFinalSaveConfirmation] = useState(true);
  const [jsonWad, setJsonWad] = useState(
    // {
    //   testNameEnglish: "aasdf",
    //   testNameFrench: "asdf",
    //   testReportNameEnglish: "aasdf",
    //   testReportNameFrench: "asdf",
    //   testSection: "56",
    //   panels: [{ id: "1" }, { id: "2" }],
    //   uom: "1",
    //   loinc: "asdf",
    //   resultType: "4",
    //   orderable: "Y",
    //   notifyResults: "Y",
    //   inLabOnly: "Y",
    //   antimicrobialResistance: "Y",
    //   active: "Y",
    //   sampleTypes:
    //     '[{"typeId": "31", "tests": [{"id": 301}, {"id": 0}]}, {"typeId": "3", "tests": [{"id": 306}, {"id": 304}, {"id": 308}, {"id": 319}, {"id": 317}, {"id": 311}, {"id": 314}, {"id": 3}, {"id": 32}, {"id": 40}, {"id": 41}, {"id": 56}, {"id": 47}, {"id": 49}, {"id": 51}, {"id": 0}]}]',
    //   lowValid: "-Infinity",
    //   highValid: "Infinity",
    //   lowReportingRange: "-Infinity",
    //   highReportingRange: "Infinity",
    //   lowCritical: "-Infinity",
    //   highCritical: "-Infinity",
    //   significantDigits: "",
    //   resultLimits:
    //     '[{"highAgeRange": "30", "gender": false, "lowNormal": "-Infinity", "highNormal": "Infinity"}, {"highAgeRange": "365", "gender": false, "lowNormal": "-Infinity", "highNormal": "Infinity"}, {"highAgeRange": "1825", "gender": false, "lowNormal": "-Infinity", "highNormal": "Infinity"}, {"highAgeRange": "5110", "gender": false, "lowNormal": "-Infinity", "highNormal": "Infinity"}, {"highAgeRange": "Infinity", "gender": false, "lowNormal": "-Infinity", "highNormal": "Infinity"}]',
    //   dictionary:
    //     '[{"value": "824", "qualified": "N"}, {"value": "826", "qualified": "N"}, {"value": "825", "qualified": "N"}, {"value": "822", "qualified": "N"}, {"value": "829", "qualified": "N"}, {"value": "821", "qualified": "N"}]',
    //   dictionaryReference: "824",
    //   defaultTestResult: "825",
    // },
    {
      testNameEnglish: "",
      testNameFrench: "",
      testReportNameEnglish: "",
      testReportNameFrench: "",
      testSection: "",
      panels: [],
      uom: "",
      loinc: "",
      resultType: "",
      orderable: "Y",
      notifyResults: "",
      inLabOnly: "",
      antimicrobialResistance: "",
      active: "Y",
      sampleTypes: [],
      lowValid: "",
      highValid: "",
      lowReportingRange: "",
      highReportingRange: "",
      lowCritical: "",
      highCritical: "",
      significantDigits: "",
      resultLimits:
        '[{"highAgeRange": "30", "gender": false, "lowNormal": "-Infinity", "highNormal": "Infinity"}, {"highAgeRange": "365", "gender": false, "lowNormal": "-Infinity", "highNormal": "Infinity"}, {"highAgeRange": "1825", "gender": false, "lowNormal": "-Infinity", "highNormal": "Infinity"}, {"highAgeRange": "5110", "gender": false, "lowNormal": "-Infinity", "highNormal": "Infinity"}, {"highAgeRange": "Infinity", "gender": false, "lowNormal": "-Infinity", "highNormal": "Infinity"}]',
    },
  );

  useEffect(() => {
    componentMounted.current = true;
    setIsLoading(true);
    getFromOpenElisServer(`/rest/TestAdd`, (res) => {
      handleTestAddData(res);
    });
    return () => {
      componentMounted.current = false;
      setIsLoading(false);
    };
  }, []);

  function handleTestAddData(res) {
    if (!res) {
      setIsLoading(true);
    } else {
      setTestAdd(res);
    }
  }

  useEffect(() => {
    if (testAdd) {
      setLabUnitList([{ id: "0", value: "" }, ...(testAdd.labUnitList || [])]);
      setPanelList([
        { id: "0", value: "Select Multiple" },
        ...(testAdd.panelList || []),
      ]);
      setUomList([{ id: "0", value: "" }, ...(testAdd.uomList || [])]);
      setResultTypeList([
        { id: "0", value: "" },
        ...(testAdd.resultTypeList || []),
      ]);
      setSampleTypeList([
        { id: "0", value: "Select Multiple" },
        ...(testAdd.sampleTypeList || []),
      ]);
      setGroupedDictionaryList([
        // { id: "0", value: "Select Multiple" },
        ...(testAdd.groupedDictionaryList || []),
      ]);
      setDictionaryList([
        { id: "0", value: "Select Multiple" },
        ...(testAdd.dictionaryList || []),
      ]);
      setAgeRangeList([
        { id: "0", value: "" },
        ...(testAdd.ageRangeList || []),
      ]);
    }
  }, [testAdd]);

  useEffect(() => {
    if (selectedSampleType.length === 0) return;

    const fetchSampleTypeData = async (id) => {
      return new Promise((resolve, reject) => {
        try {
          getFromOpenElisServer(
            `/rest/sample-type-tests?sampleType=${id}`,
            (res) => {
              if (res) {
                handleSampleType(res);
                resolve(res);
              } else {
                reject(new Error("No response received"));
              }
            },
          );
        } catch (error) {
          console.error(`Error fetching data for sample type ${id}:`, error);
          reject(error);
        }
      });
    };

    const fetchAllSampleTypesData = async () => {
      try {
        await Promise.all(
          selectedSampleType.map((sampleType) =>
            fetchSampleTypeData(sampleType.id),
          ),
        );
      } catch (error) {
        console.error("Error fetching all sample types:", error);
      }
    };

    fetchAllSampleTypesData();
  }, [selectedSampleType]);

  function handleSampleType(res) {
    setSelectedSampleTypeResp((prev) => {
      const selectedSampleTypeIds = selectedSampleType.map((type) => type.id);

      const isInSelectedSampleType = selectedSampleTypeIds.includes(
        res.sampleTypeId,
      );

      if (isInSelectedSampleType) {
        const isAlreadyPresent = prev.some(
          (item) => item.sampleTypeId === res.sampleTypeId,
        );
        if (!isAlreadyPresent) {
          return [...prev, res];
        }
      } else {
        return prev.filter((item) => item.sampleTypeId !== res.sampleTypeId);
      }
      return prev;
    });
  }

  function testNameEn(e) {
    setJsonWad((prev) => ({
      ...prev,
      testNameEnglish: e.target.value,
    }));
  }

  function testNameFr(e) {
    setJsonWad((prev) => ({
      ...prev,
      testNameFrench: e.target.value,
    }));
  }

  function reportingTestNameEn(e) {
    setJsonWad((prev) => ({
      ...prev,
      testReportNameEnglish: e.target.value,
    }));
  }

  function reportingTestNameFr(e) {
    setJsonWad((prev) => ({
      ...prev,
      testReportNameFrench: e.target.value,
    }));
  }

  function copyInputValuesFromTestNameEnFr() {
    setJsonWad((prev) => ({
      ...prev,
      testReportNameEnglish: prev.testNameEnglish,
      testReportNameFrench: prev.testNameFrench,
    }));
  }

  function handelTestSectionSelect(e) {
    setJsonWad((prev) => ({
      ...prev,
      testSection: e.target.value,
    }));

    const selectedLabUnitObject = labUnitList.find(
      (item) => item.id === e.target.value,
    );

    if (selectedLabUnitObject) {
      setSelectedLabUnitList(selectedLabUnitObject);
    }
  }

  function handelUomSelect(e) {
    setJsonWad((prev) => ({ ...prev, uom: e.target.value }));

    const selectedUomObject = uomList.find(
      (item) => item.id === e.target.value,
    );

    if (selectedUomObject) {
      setSelectedUomList(selectedUomObject);
    }
  }

  function handelLonicChange(e) {
    const regex = /^(?!-)(?:\d+-)*\d*$/;

    const value = e.target.value;

    if (regex.test(value)) {
      setLonic(value);
      setJsonWad((prev) => ({ ...prev, loinc: value }));
    } else {
      addNotification({
        title: intl.formatMessage({
          id: "notification.title",
        }),
        message: intl.formatMessage({
          id: "notification.user.post.save.success",
        }),
        kind: NotificationKinds.error,
      });
      setNotificationVisible(true);
    }
  }

  function handelResultType(e) {
    setJsonWad((prev) => ({ ...prev, resultType: e.target.value }));

    const selectedResultTypeObject = resultTypeList.find(
      (item) => item.id == e.target.value,
    );

    if (selectedResultTypeObject) {
      setSelectedResultTypeList(selectedResultTypeObject);
    }
  }

  function handleAntimicrobialResistance(e) {
    setJsonWad((prev) => ({
      ...prev,
      antimicrobialResistance: e.target.checked ? "Y" : "N",
    }));
  }
  function handleIsActive(e) {
    setJsonWad((prev) => ({ ...prev, active: e.target.checked ? "Y" : "N" }));
  }
  function handleOrderable(e) {
    setJsonWad((prev) => ({
      ...prev,
      orderable: e.target.checked ? "Y" : "N",
    }));
  }
  function handleNotifyPatientofResults(e) {
    setJsonWad((prev) => ({
      ...prev,
      notifyResults: e.target.checked ? "Y" : "N",
    }));
  }
  function handleInLabOnly(e) {
    setJsonWad((prev) => ({
      ...prev,
      inLabOnly: e.target.checked ? "Y" : "N",
    }));
  }

  function handleSampleTypeSetup() {
    setSampleTypeSetupPage(true);
  }

  function handleRangeSetup() {
    setRangeSetupPage(true);
  }

  function handleOnResultType() {
    setOnResultType(true);
  }

  function handleExistingTestSetup() {
    setExistingTestSetupPage(true);
  }

  function handleFinalSaveConfirmation() {
    setFinalSaveConfirmation(true);
  }

  const handelPanelSelectSetTag = (e) => {
    const selectedId = e.target.value;
    const selectedValue = e.target.options[e.target.selectedIndex].text;

    setPanelListTag((prevTags) => {
      const isTagPresent = prevTags.some((tag) => tag.id === selectedId);
      if (isTagPresent) return prevTags;

      const newTag = { id: selectedId, value: selectedValue };
      const updatedTags = [...prevTags, newTag];

      const updatedPanels = [...updatedTags.map((tag) => ({ id: tag.id }))];
      setJsonWad((prevJsonWad) => ({
        ...prevJsonWad,
        panels: updatedPanels,
      }));

      return updatedTags;
    });
  };

  const handlePanelRemoveTag = (idToRemove) => {
    setPanelListTag((prevTags) => {
      const updatedTags = prevTags.filter((tag) => tag.id !== idToRemove);

      const updatedPanels = updatedTags.map((tag) => ({ id: tag.id }));
      setJsonWad((prevJsonWad) => ({
        ...prevJsonWad,
        panels: updatedPanels,
      }));

      return updatedTags;
    });
  };

  // const handleSampleTypeListSelectIdTestTag = (e) => {
  //   const selectedTestId = e.target.value;
  //   const testName = e.target.options[e.target.selectedIndex].text;

  //   const existingIndex = sampleTestTypeToGetTagList.findIndex(
  //     (item) => item.id === selectedTestId,
  //   );

  //   let updatedList;
  //   if (existingIndex !== -1) {
  //     updatedList = [...sampleTestTypeToGetTagList];
  //     updatedList.splice(existingIndex, 1);
  //     setSampleTestTypeToGetTagList(updatedList);
  //   } else {
  //     const selectedTest = {
  //       id: selectedTestId,
  //       name: testName,
  //     };
  //     updatedList = [...sampleTestTypeToGetTagList, selectedTest];
  //     setSampleTestTypeToGetTagList(updatedList);
  //   }

  //   const updatedReplace = updatedList.map((item) => item.id);
  //   setJsonWad((prevJsonWad) => ({
  //     ...prevJsonWad,
  //     replace: updatedReplace,
  //   }));
  // };

  const handleSampleTypeListSelectIdTestTag = (e) => {
    const selectedId = e.target.value;
    const selectedSampleTypeObject = sampleTypeList.find(
      (type) => type.id === selectedId,
    );

    if (selectedSampleTypeObject) {
      const isAlreadySelected = selectedSampleType.some(
        (type) => type.id === selectedSampleTypeObject.id,
      );

      if (!isAlreadySelected) {
        setSelectedSampleTypeList([
          ...selectedSampleTypeList,
          selectedSampleTypeObject,
        ]);

        setSampleTestTypeToGetTagList([
          ...sampleTestTypeToGetTagList,
          selectedSampleTypeObject,
        ]);

        setSelectedSampleType((prev) => [...prev, selectedSampleTypeObject]);
      }
    }
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

    setSelectedSampleTypeList((prevList) => {
      const updatedList = prevList.filter(
        (_, index) => index !== indexToRemove,
      );
      return updatedList;
    });

    setSelectedSampleType((prevList) => {
      const updatedList = prevList.filter(
        (_, index) => index !== indexToRemove,
      );
      return updatedList;
    });

    setSelectedSampleTypeResp((prevState) =>
      prevState.filter((_, index) => index !== indexToRemove),
    );
  }

  function testAddPostCall() {
    setIsLoading(true);
    postToOpenElisServerJsonResponse(
      `/rest/TestAdd`,
      JSON.stringify(jsonWad),
      (res) => {
        testAddPostCallback(res);
      },
    );
  }

  function testAddPostCallback(res) {
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

  const handelSelectListOptions = (e) => {
    const selectedId = e.target.value;

    const selectedObject = dictionaryList.find(
      (item) => item.id === selectedId,
    );

    if (selectedObject) {
      setSingleSelectDictionaryList((prev) => [...prev, selectedObject]);
      setMultiSelectDictionaryList((prev) => [...prev, selectedObject]);

      setDictionaryListTag((prev) => [...prev, selectedObject]);
    }

    //set the data object in jsonWad
  };

  const handleSelectQualifiersTag = (e) => {
    const selectedId = e.target.value;

    const selectedObject = multiSelectDictionaryList.find(
      (item) => item.id === selectedId,
    );

    if (selectedObject) {
      setMultiSelectDictionaryListTag((prev) => [...prev, selectedObject]);
    }

    //set the data object in jsonWad
  };

  const handleLabUnitSelect = (e) => {
    const selectedLabUnitId = e.target.value;

    setJsonWad((prev) => ({ ...prev, testSection: selectedLabUnitId }));
  };

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
                  <FormattedMessage id="configuration.test.add" />
                </Heading>
              </Section>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
          <Grid fullWidth={true}>
            <Column lg={8} md={4} sm={4}>
              <div>
                <>
                  <FormattedMessage id="test.section.label" />
                  <span style={{ color: "red" }}>*</span>
                </>
                <br />
                <Select
                  id={`select-test-section`}
                  hideLabel
                  required
                  onChange={(e) => {
                    handelTestSectionSelect(e);
                  }}
                >
                  {labUnitList?.map((test) => (
                    <SelectItem
                      key={test.id}
                      value={test.id}
                      text={`${test.value}`}
                    />
                  ))}
                </Select>
              </div>
              <br />
              <div>
                <>
                  <FormattedMessage id="sample.entry.project.testName" />
                  <span style={{ color: "red" }}>*</span>
                </>
                <br />
                <br />
                <FormattedMessage id="english.label" />
                <br />
                <TextInput
                  labelText=""
                  id="testNameEn"
                  value={jsonWad?.testNameEnglish}
                  onChange={testNameEn}
                  required
                />
                <br />
                <FormattedMessage id="french.label" />
                <br />
                <TextInput
                  labelText=""
                  id="testNameFr"
                  value={jsonWad?.testNameFrench}
                  onChange={testNameFr}
                  required
                />
              </div>
              <br />
              <div>
                <>
                  <FormattedMessage id="reporting.label.testName" />
                  <span style={{ color: "red" }}>*</span>
                </>
                <br />
                <br />
                <Button
                  kind="tertiary"
                  onClick={copyInputValuesFromTestNameEnFr}
                  type="button"
                >
                  <FormattedMessage id="test.add.copy.name" />
                </Button>
                <br />
                <br />
                <FormattedMessage id="english.label" />
                <br />
                <TextInput
                  labelText=""
                  id="reportingTestNameEn"
                  value={jsonWad?.reportingTestNameEn}
                  required
                  onChange={reportingTestNameEn}
                />
                <br />
                <FormattedMessage id="french.label" />
                <br />
                <TextInput
                  labelText=""
                  id="reportingTestNameFr"
                  value={jsonWad?.reportingTestNameFr}
                  required
                  onChange={reportingTestNameFr}
                />
              </div>
            </Column>
            <Column lg={4} md={4} sm={4}>
              <FormattedMessage id="field.panel" />
              <Select
                id={`select-panel`}
                onChange={(e) => {
                  handelPanelSelectSetTag(e);
                }}
                hideLabel
                required
              >
                {panelList?.map((test) => (
                  <SelectItem
                    key={test.id}
                    value={test.id}
                    text={`${test.value}`}
                  />
                ))}
              </Select>
              <br />
              {panelListTag && panelListTag.length ? (
                <div
                  className={"select-panel"}
                  style={{ marginBottom: "1.188rem" }}
                >
                  <>
                    {panelListTag.map((panel) => (
                      <Tag
                        filter
                        key={`panelTags_${panel.id}`}
                        onClose={() => handlePanelRemoveTag(panel.id)}
                        style={{ marginRight: "0.5rem" }}
                        type={"green"}
                      >
                        {panel.value}
                      </Tag>
                    ))}
                  </>
                </div>
              ) : (
                <></>
              )}
              <br />
              <FormattedMessage id="field.uom" />
              <Select
                onChange={(e) => {
                  handelUomSelect(e);
                }}
                id={`select-uom`}
                hideLabel
                required
              >
                {uomList?.map((test) => (
                  <SelectItem
                    key={test.id}
                    value={test.id}
                    text={`${test.value}`}
                  />
                ))}
              </Select>
            </Column>
            <Column lg={4} md={4} sm={4}>
              <div>
                <>
                  <FormattedMessage id="field.resultType" />
                  <span style={{ color: "red" }}>*</span>
                </>
                <br />
                <Select
                  id={`select-result-type`}
                  hideLabel
                  required
                  onChange={(e) => {
                    handelResultType(e);
                  }}
                >
                  {resultTypeList?.map((test) => (
                    <SelectItem
                      key={test.id}
                      value={test.id}
                      text={`${test.value}`}
                    />
                  ))}
                </Select>
              </div>
              <br />
              <div>
                <FormattedMessage id="label.loinc" />
                <br />
                <TextInput
                  labelText=""
                  required
                  id="loinc"
                  value={lonic}
                  onChange={(e) => {
                    handelLonicChange(e);
                  }}
                />
              </div>
              <br />
              <div>
                <Checkbox
                  labelText={
                    <FormattedMessage id="test.antimicrobialResistance" />
                  }
                  id="antimicrobial-resistance"
                  onChange={handleAntimicrobialResistance}
                  checked={jsonWad?.antimicrobialResistance === "Y"}
                />
                <Checkbox
                  labelText={
                    <FormattedMessage id="dictionary.category.isActive" />
                  }
                  id="is-active"
                  onChange={handleIsActive}
                  checked={jsonWad?.active === "Y"}
                />
                <Checkbox
                  labelText={<FormattedMessage id="label.orderable" />}
                  id="orderable"
                  onChange={handleOrderable}
                  checked={jsonWad?.orderable === "Y"}
                />
                <Checkbox
                  labelText={<FormattedMessage id="test.notifyResults" />}
                  id="notify-patient-of-results"
                  onChange={handleNotifyPatientofResults}
                  checked={jsonWad?.notifyResults === "Y"}
                />
                <Checkbox
                  labelText={<FormattedMessage id="test.inLabOnly" />}
                  id="in-lab-only"
                  onChange={handleInLabOnly}
                  checked={jsonWad?.inLabOnly === "Y"}
                />
              </div>
            </Column>
            <br />
            <br />
            <Column lg={16} md={8} sm={4}>
              <Button onClick={handleSampleTypeSetup} type="button">
                <FormattedMessage id="next.action.button" />
              </Button>{" "}
              <Button
                onClick={() => {
                  window.location.assign(
                    "/MasterListsPage#testManagementConfigMenu",
                  );
                }}
                kind="tertiary"
                type="button"
              >
                <FormattedMessage id="back.action.button" />
              </Button>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
          {sampleTypeSetupPage ? (
            <>
              <Grid fullWidth={true}>
                <Column lg={6} md={2} sm={4}>
                  <FormattedMessage id="sample.type" />
                  <br />
                  <Select
                    id={`select-sample-type`}
                    hideLabel
                    required
                    onChange={(e) => handleSampleTypeListSelectIdTestTag(e)}
                  >
                    {sampleTypeList?.map((test) => (
                      <SelectItem
                        key={test.id}
                        value={test.id}
                        text={`${test.value}`}
                      />
                    ))}
                  </Select>
                  <br />
                  {sampleTestTypeToGetTagList &&
                  sampleTestTypeToGetTagList.length ? (
                    <div
                      className={"select-sample-type"}
                      style={{ marginBottom: "1.188rem" }}
                    >
                      <>
                        {sampleTestTypeToGetTagList.map((section, index) => (
                          <Tag
                            filter
                            key={`testTags_${index}`}
                            onClose={() =>
                              handleRemoveSampleTypeListSelectIdTestTag(index)
                            }
                            style={{ marginRight: "0.5rem" }}
                            type={"green"}
                          >
                            {section.value}
                          </Tag>
                        ))}
                      </>
                    </div>
                  ) : (
                    <></>
                  )}
                  <br />
                </Column>
                <Column lg={10} md={6} sm={4}>
                  <Section>
                    <Section>
                      <Section>
                        <Heading>
                          <FormattedMessage id="label.test.display.order" />
                        </Heading>
                      </Section>
                    </Section>
                  </Section>
                  <br />
                  {selectedSampleTypeResp.length > 0 ? (
                    selectedSampleTypeResp.map((item, index) => (
                      <>
                        <div className="gridBoundary">
                          <Section key={index}>
                            <UnorderedList>
                              {item.tests.map((test) => (
                                <ListItem key={test.id}>{test.name}</ListItem>
                              ))}
                            </UnorderedList>
                          </Section>
                        </div>
                        <br />
                      </>
                    ))
                  ) : (
                    <></>
                  )}
                </Column>
                <br />
                <br />
                <Column lg={16} md={8} sm={4}>
                  <Button onClick={handleRangeSetup} type="button">
                    <FormattedMessage id="next.action.button" />
                  </Button>{" "}
                  <Button
                    onClick={() => {
                      window.location.reload();
                    }}
                    kind="tertiary"
                    type="button"
                  >
                    <FormattedMessage id="label.button.cancel" />
                  </Button>
                </Column>
              </Grid>
              <br />
              <hr />
              <br />
            </>
          ) : (
            <></>
          )}
          {onResultType ? (
            <>
              <Grid>
                <Column lg={8} md={8} sm={4}>
                  <FormattedMessage id="label.select.list.options" />
                  {/* map the Select list options */}
                  <br />
                  <Select
                    id={`select-list-options`}
                    hideLabel
                    required
                    onChange={(e) => handelSelectListOptions(e)} // need a fix
                  >
                    {dictionaryList?.map((test) => (
                      <SelectItem
                        key={test.id}
                        value={test.id}
                        text={`${test.value}`}
                      />
                    ))}
                  </Select>
                  {/* tags need to display */}
                  <br />
                  {/* need to add tags */}
                  {dictionaryListTag && dictionaryListTag.length ? (
                    <div
                      className={"select-list-options-tag"}
                      style={{ marginBottom: "1.188rem" }}
                    >
                      <>
                        {dictionaryListTag.map((dict, index) => (
                          <Tag
                            filter
                            key={`list-options_${index}`}
                            // onClose={() =>
                            //   handleRemoveSampleTypeListSelectIdTestTag(index)
                            // }
                            style={{ marginRight: "0.5rem" }}
                            type={"green"}
                          >
                            {dict.value}
                          </Tag>
                        ))}
                      </>
                    </div>
                  ) : (
                    <></>
                  )}
                  <br />
                </Column>
                <Column lg={8} md={8} sm={4}>
                  <Section>
                    <Section>
                      <Section>
                        <Heading>
                          <FormattedMessage id="label.result.order" />
                        </Heading>
                      </Section>
                    </Section>
                  </Section>
                  {/* remeder dragable & Select list options */}
                  <br />
                  <br />
                  <FormattedMessage id="label.reference.value" />
                  <br />
                  {/* single Select */}
                  <Select
                    id={`select-reference-value`}
                    hideLabel
                    required
                    // onChange={(e) => handleSampleTypeListSelectIdTestTag(e)} // need to fix
                  >
                    {singleSelectDictionaryList?.map((test) => (
                      <SelectItem
                        key={test.id}
                        value={test.id}
                        text={`${test.value}`}
                      />
                    ))}
                  </Select>
                  <br />
                  <br />
                  <FormattedMessage id="label.default.result" />
                  <br />
                  {/* single Select */}
                  <Select
                    id={`select-default-result`}
                    hideLabel
                    required
                    // onChange={(e) => handleSampleTypeListSelectIdTestTag(e)} // need to fix
                  >
                    {singleSelectDictionaryList?.map((test) => (
                      <SelectItem
                        key={test.id}
                        value={test.id}
                        text={`${test.value}`}
                      />
                    ))}
                  </Select>
                  <br />
                  <br />
                  <FormattedMessage id="label.qualifiers" />
                  <br />
                  <Select
                    id={`select-qualifiers`}
                    hideLabel
                    required
                    onChange={(e) => handleSelectQualifiersTag(e)} // need to fix
                  >
                    {multiSelectDictionaryList?.map((test) => (
                      <SelectItem
                        key={test.id}
                        value={test.id}
                        text={`${test.value}`}
                      />
                    ))}
                  </Select>
                  <br />
                  {/* need to add tags */}
                  {multiSelectDictionaryListTag &&
                  multiSelectDictionaryListTag.length ? (
                    <div
                      className={"select-qualifiers-tag"}
                      style={{ marginBottom: "1.188rem" }}
                    >
                      <>
                        {multiSelectDictionaryListTag.map((dict, index) => (
                          <Tag
                            filter
                            key={`qualifiers_${index}`}
                            // onClose={() =>
                            //   handleRemoveSampleTypeListSelectIdTestTag(index)
                            // }
                            style={{ marginRight: "0.5rem" }}
                            type={"green"}
                          >
                            {dict.value}
                          </Tag>
                        ))}
                      </>
                    </div>
                  ) : (
                    <></>
                  )}
                  <br />
                </Column>
                <br />
                <br />
                <Column lg={16} md={8} sm={4}>
                  <Button onClick={handleOnResultType} type="button">
                    <FormattedMessage id="next.action.button" />
                  </Button>{" "}
                  <Button
                    onClick={() => {
                      window.location.assign(
                        "/MasterListsPage#testManagementConfigMenu",
                      );
                    }}
                    kind="tertiary"
                    type="button"
                  >
                    <FormattedMessage id="back.action.button" />
                  </Button>
                </Column>
              </Grid>
              <br />
              <hr />
              <br />
            </>
          ) : (
            <></>
          )}
          {rangeSetupPage ? (
            <>
              <Grid fullWidth={true}>
                <Column lg={16} md={8} sm={4}>
                  <Section>
                    <Section>
                      <Section>
                        <Heading>
                          <FormattedMessage id="label.button.range" />
                        </Heading>
                      </Section>
                    </Section>
                  </Section>
                </Column>
              </Grid>
              <br />
              <hr />
              <br />
              <Grid fullWidth={true} className="gridBoundary">
                <Column lg={16} md={8} sm={4}>
                  <FormattedMessage id="field.ageRange" />
                  <hr />
                  <div style={{ display: "flex", gap: "4px" }}>
                    <Checkbox
                      id={"gender"}
                      labelText={<FormattedMessage id="label.sex.dependent" />}
                      // onChange={() => {}}
                    />
                    {/* render male & female on checkbox*/}
                    <RadioButtonGroup name={"fieldAgeRangeRadioGroup"}>
                      <RadioButton labelText={"Y"} />
                      <RadioButton labelText={"M"} />
                      <RadioButton labelText={"D"} />
                    </RadioButtonGroup>
                    <TextInput
                      id="field.ageRange0"
                      labelText=""
                      hideLabel
                      required
                    />
                    <Select
                      id="field.ageRange1"
                      labelText=""
                      hideLabel
                      required
                    >
                      {/* map agerangeList values from objects inside array */}
                      {ageRangeList.map((age) => (
                        <SelectItem
                          key={age.id}
                          value={age.id}
                          text={`${age.value}`}
                        />
                      ))}
                    </Select>
                  </div>
                  <hr />
                  <br />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <FormattedMessage id="field.normalRange" />
                  <hr />
                  <div style={{ display: "flex", gap: "4px" }}>
                    <TextInput
                      id="field.normalRange0"
                      labelText=""
                      hideLabel
                      required
                    />
                    <TextInput
                      id="field.normalRange1"
                      labelText=""
                      hideLabel
                      required
                    />
                    {/* render  two extra fields for TextInput on Click of Check box */}
                  </div>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <FormattedMessage id="label.reporting.range" />
                  <hr />
                  <div style={{ display: "flex", gap: "4px" }}>
                    <TextInput
                      id="label.reporting.range0"
                      labelText=""
                      hideLabel
                      required
                    />
                    <TextInput
                      id="label.reporting.range1"
                      labelText=""
                      hideLabel
                      required
                    />
                  </div>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <FormattedMessage id="field.validRange" />
                  <hr />
                  <div style={{ display: "flex", gap: "4px" }}>
                    <TextInput
                      id="field.validRange0"
                      labelText=""
                      hideLabel
                      required
                    />
                    <TextInput
                      id="field.validRange1"
                      labelText=""
                      hideLabel
                      required
                    />
                  </div>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <FormattedMessage id="label.critical.range" />
                  <hr />
                  <div style={{ display: "flex", gap: "4px" }}>
                    <TextInput
                      id="label.critical.range0"
                      labelText=""
                      hideLabel
                      required
                    />
                    <TextInput
                      id="label.critical.range1"
                      labelText=""
                      hideLabel
                      required
                    />
                  </div>
                </Column>
              </Grid>
              <br />
              <FlexGrid fullWidth={true}>
                <Row>
                  <Column lg={4} md={4} sm={4}>
                    <Section>
                      <Section>
                        <Section>
                          <Heading>
                            <FormattedMessage id="field.significantDigits" />
                            {" : "}
                          </Heading>
                        </Section>
                      </Section>
                    </Section>
                  </Column>
                  <Column lg={4} md={4} sm={4}>
                    <NumberInput
                      id={"significant_digits_num_input"}
                      max={99}
                      min={0}
                      size={"md"}
                      allowEmpty={true}
                    />
                  </Column>
                </Row>
              </FlexGrid>
              <br />
              <Grid fullWidth={true}>
                <Column lg={16} md={8} sm={4}>
                  <Button onClick={handleExistingTestSetup} type="button">
                    <FormattedMessage id="next.action.button" />
                  </Button>{" "}
                  <Button
                    onClick={() => {
                      window.location.assign(
                        "/MasterListsPage#testManagementConfigMenu",
                      );
                    }}
                    kind="tertiary"
                    type="button"
                  >
                    <FormattedMessage id="back.action.button" />
                  </Button>
                </Column>
              </Grid>
              <br />
              <hr />
              <br />
            </>
          ) : (
            <></>
          )}
          {existingTestSetupPage ? (
            <>
              <Grid fullWidth={true}>
                <Column lg={16} md={8} sm={4}>
                  <Section>
                    <Section>
                      <Section>
                        <Heading>
                          <FormattedMessage id="label.existing.test.sets" />
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
                {groupedDictionaryList.map((innerArray, outerIndex) => (
                  <>
                    <Column
                      key={`list-${outerIndex}`}
                      lg={4}
                      md={4}
                      sm={4}
                      onClick={() => {
                        setSelectedGroupedDictionaryList([
                          ...selectedGroupedDictionaryList,
                          innerArray,
                        ]);
                      }}
                    >
                      <Section>
                        <UnorderedList>
                          {innerArray.map((item) => (
                            <ListItem key={`listItem-${outerIndex}-${item.id}`}>
                              {item.value}
                            </ListItem>
                          ))}
                          {/* need to fix console log here */}
                        </UnorderedList>
                      </Section>
                      <br />
                    </Column>
                  </>
                ))}
              </Grid>
              <br />
              <Grid fullWidth={true}>
                <Column lg={16} md={8} sm={4}>
                  <Button onClick={handleFinalSaveConfirmation} type="button">
                    <FormattedMessage id="next.action.button" />
                  </Button>{" "}
                  <Button
                    onClick={() => {
                      window.location.assign(
                        "/MasterListsPage#testManagementConfigMenu",
                      );
                    }}
                    kind="tertiary"
                    type="button"
                  >
                    <FormattedMessage id="back.action.button" />
                  </Button>
                </Column>
              </Grid>
              <br />
              <hr />
              <br />
            </>
          ) : (
            <></>
          )}
          {finalSaveConfirmation ? (
            <>
              <Grid fullWidth={true}>
                <Column lg={6} md={8} sm={4}>
                  <FormattedMessage id="sample.entry.project.testName" />
                  <br />
                  <FormattedMessage id="english.label" />
                  {" : "}
                  {jsonWad?.testNameEnglish}
                  <br />
                  <FormattedMessage id="french.label" />
                  {" : "}
                  {jsonWad?.testNameFrench}
                  <br />
                  <br />
                  <FormattedMessage id="reporting.label.testName" />
                  <br />
                  <FormattedMessage id="english.label" />
                  {" : "}
                  {jsonWad?.reportingTestNameEn}
                  <br />
                  <FormattedMessage id="french.label" />
                  {" : "}
                  {jsonWad?.reportingTestNameFr}
                  <br />
                  <br />
                  <FormattedMessage id="test.section.label" />
                  {" : "}
                  {selectedLabUnitList?.value}
                  <br />
                  <br />
                  <FormattedMessage id="field.panel" />
                  {" : "}
                  {/* map the  {panelList[0].value} in and there values in line*/}
                  {panelListTag.length > 0 ? (
                    <UnorderedList>
                      {panelListTag.map((tag) => (
                        <div key={tag.id} style={{ marginRight: "0.5rem" }}>
                          <ListItem>{tag.value}</ListItem>
                        </div>
                      ))}
                    </UnorderedList>
                  ) : (
                    <></>
                  )}
                  <br />
                  <br />
                  <FormattedMessage id="field.uom" />
                  {" : "}
                  {selectedUomList?.value}
                  <br />
                  <br />
                  <FormattedMessage id="label.loinc" />
                  {" : "}
                  {jsonWad?.loinc}
                  <br />
                  <br />
                  <FormattedMessage id="field.resultType" />
                  {" : "}
                  {selectedResultTypeList.value}
                  <br />
                  <br />
                  <FormattedMessage id="test.antimicrobialResistance" />
                  {" : "}
                  {jsonWad?.antimicrobialResistance}
                  <br />
                  <br />
                  <FormattedMessage id="dictionary.category.isActive" />
                  {" : "}
                  {jsonWad?.active}
                  <br />
                  <br />
                  <FormattedMessage id="label.orderable" />
                  {" : "}
                  {jsonWad?.orderable}
                  <br />
                  <br />
                  <FormattedMessage id="test.notifyResults" />
                  {" : "}
                  {jsonWad?.notifyResults}
                  <br />
                  <br />
                  <FormattedMessage id="test.inLabOnly" />
                  {" : "}
                  {jsonWad?.inLabOnly}
                  <br />
                </Column>
                <Column lg={10} md={8} sm={4}>
                  <FormattedMessage id="sample.type.and.test.sort.order" />
                  {/* Mapp the combbination of the selecte[sampleType] & tests of [sampleType] in sorted order */}
                  <br />
                  {selectedSampleTypeList.length > 0 ? (
                    <UnorderedList nested={true}>
                      {selectedSampleTypeList.map((type, index) => (
                        <div key={`selectedSampleType_${index}`}>
                          <ListItem>{type.value}</ListItem>
                          <br />
                          {selectedSampleTypeResp
                            .filter((resp) => resp.sampleTypeId === type.id)
                            .map((item, respIndex) => (
                              <div
                                key={`selectedSampleTypeResp_${respIndex}`}
                                className="gridBoundary"
                              >
                                <Section>
                                  <UnorderedList nested>
                                    {item.tests.map((test) => (
                                      <ListItem key={`test_${test.id}`}>
                                        {test.name}
                                      </ListItem>
                                    ))}
                                  </UnorderedList>
                                </Section>
                              </div>
                            ))}
                        </div>
                      ))}
                    </UnorderedList>
                  ) : (
                    <></>
                  )}
                </Column>
              </Grid>
              <br />
              <hr />
              <br />
            </>
          ) : (
            <></>
          )}
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Button
                disabled={!finalSaveConfirmation}
                onClick={() => {
                  setJsonWad(JSON.stringify(jsonWad));
                  testAddPostCall();
                }}
                type="button"
              >
                <FormattedMessage id="label.button.submit" />
              </Button>{" "}
              <Button
                onClick={() =>
                  window.location.assign(
                    "/MasterListsPage#testManagementConfigMenu",
                  )
                }
                kind="tertiary"
                type="button"
              >
                <FormattedMessage id="label.button.cancel" />
              </Button>
            </Column>
          </Grid>
          <button
            onClick={() => {
              console.log(testAdd);
            }}
          >
            testAdd
          </button>
          <button
            onClick={() => {
              console.log(jsonWad);
            }}
          >
            jsonWad
          </button>
          <button
            onClick={() => {
              console.log(sampleTestTypeToGetTagList);
            }}
          >
            sampleTestTypeToGetTagList
          </button>
          <button
            onClick={() => {
              console.log(selectedSampleType);
            }}
          >
            selectedSampleType
          </button>
          <button
            onClick={() => {
              console.log(selectedSampleTypeResp);
            }}
          >
            selectedSampleTypeResp
          </button>
        </div>
      </div>
    </>
  );
}

export default injectIntl(TestAdd);
