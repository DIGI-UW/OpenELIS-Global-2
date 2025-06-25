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
  Checkbox,
  Row,
  FlexGrid,
  Tag,
  UnorderedList,
  ListItem,
  NumberInput,
  RadioButtonGroup,
  RadioButton,
  Toggle,
  ClickableTile,
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
import { Formik, Form } from "formik";
import * as Yup from "yup";
import { CustomShowGuide } from "./customComponents/CustomShowGuide.js";
import { CustomCommonSortableOrderList } from "./sortableListComponent/SortableList.js";

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
  const [multiSelectDictionaryList, setMultiSelectDictionaryList] = useState(
    [],
  );
  const [multiSelectDictionaryListTag, setMultiSelectDictionaryListTag] =
    useState([]);
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
      inLabOnly: "N",
      antimicrobialResistance: "N",
      active: "Y",
      sampleTypes: [],
      lowValid: "",
      highValid: "",
      lowReportingRange: "",
      highReportingRange: "",
      lowCritical: "",
      highCritical: "",
      significantDigits: "",
      resultLimits: [
        {
          highAgeRange: 30,
          gender: false,
          lowNormal: -Infinity,
          highNormal: Infinity,
        },
      ],
      // '[{"highAgeRange": "30", "gender": false, "lowNormal": "-Infinity", "highNormal": "Infinity"}, {"highAgeRange": "365", "gender": false, "lowNormal": "-Infinity", "highNormal": "Infinity"}, {"highAgeRange": "1825", "gender": false, "lowNormal": "-Infinity", "highNormal": "Infinity"}, {"highAgeRange": "5110", "gender": false, "lowNormal": "-Infinity", "highNormal": "Infinity"}, {"highAgeRange": "Infinity", "gender": false, "lowNormal": "-Infinity", "highNormal": "Infinity"}]'
    },
  );

  const [currentStep, setCurrentStep] = useState(0);
  const [ageRangeFields, setAgeRangeFields] = useState([0]);

  const [formData, setFormData] = useState({
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
    inLabOnly: "N",
    antimicrobialResistance: "N",
    active: "Y",
    dictionary: [],
    dictionaryReference: "",
    defaultTestResult: "",
    sampleTypes: [],
    lowValid: "-Infinity",
    highValid: "Infinity",
    lowReportingRange: "-Infinity",
    highReportingRange: "Infinity",
    lowCritical: "-Infinity",
    highCritical: "Infinity",
    significantDigits: "",
    resultLimits: [
      {
        ageRange: "0",
        highAgeRange: "0",
        gender: false,
        lowNormal: "-Infinity",
        highNormal: "Infinity",
        lowNormalFemale: "-Infinity",
        highNormalFemale: "Infinity",
      },
    ],
  });

  const [showGuide, setShowGuide] = useState(false);

  const handleToggleShowGuide = () => {
    setShowGuide(!showGuide);
  };

  const handleNextStep = (newData, final = false) => {
    setFormData((prev) => ({ ...prev, ...newData }));

    if (!final) {
      handleTestAddPostCall(formData);
    }

    const selectedResultTypeId = newData?.resultType || formData.resultType;

    setCurrentStep((prev) => {
      if (prev === 3) {
        if (["1", "4", "5"].includes(selectedResultTypeId)) {
          return prev + 2;
        }

        if (["2", "6", "7"].includes(selectedResultTypeId)) {
          return prev + 1;
        }
      }

      if (prev === 4 && ["2", "6", "7"].includes(selectedResultTypeId)) {
        return prev + 2;
      }

      if (prev === 5 && selectedResultTypeId === "4") {
        return prev + 1;
      }

      return prev + 1;
    });
  };

  const handlePreviousStep = (newData) => {
    setFormData((prev) => ({ ...prev, ...newData }));
    const selectedResultTypeId = newData?.resultType || formData.resultType;

    setCurrentStep((prevStep) => {
      if (
        prevStep === 5 &&
        (selectedResultTypeId === "1" ||
          selectedResultTypeId === "4" ||
          selectedResultTypeId === "5")
      ) {
        return prevStep - 2;
      }

      if (
        prevStep === 6 &&
        (selectedResultTypeId === "2" ||
          selectedResultTypeId === "6" ||
          selectedResultTypeId === "7")
      ) {
        return prevStep - 2;
      }

      if (
        prevStep === 6 &&
        (selectedResultTypeId === "1" ||
          selectedResultTypeId === "4" ||
          selectedResultTypeId === "5")
      ) {
        return prevStep - 3;
      }

      return prevStep - 1;
    });
  };

  const validationSchema = Yup.object({
    testSection: Yup.string()
      .required("Test section is required")
      .notOneOf(["0"], "Please select a valid test section"),
    testNameEnglish: Yup.string()
      .matches(/^[A-Za-z\s]+$/, "Only letters and spaces are allowed")
      .trim()
      .required("English test name is required"),
    testNameFrench: Yup.string()
      .matches(/^[A-Za-z\s]+$/, "Only letters and spaces are allowed")
      .trim()
      .required("French test name is required"),
    testReportNameEnglish: Yup.string()
      .matches(/^[A-Za-z\s]+$/, "Only letters and spaces are allowed")
      .trim()
      .required("English report name is required"),
    testReportNameFrench: Yup.string()
      .matches(/^[A-Za-z\s]+$/, "Only letters and spaces are allowed")
      .trim()
      .required("French report name is required"),
    panels: Yup.array().of(
      Yup.object().shape({
        id: Yup.string().required("Panel ID is required"),
      }),
    ),
    uom: Yup.string().required("Unit of Measurement is required"),
    loinc: Yup.string()
      .matches(/^(?!-)(?:\d+-)*\d*$/, "Loinc must be a valid format")
      .required("Loinc is required"),
    resultType: Yup.string().required("Result Type is required"),
    orderable: Yup.string().oneOf(["Y", "N"], "Orderable must be Y or N"),
    notifyResults: Yup.string().oneOf(
      ["Y", "N"],
      "Notify Results must be Y or N",
    ),
    inLabOnly: Yup.string().oneOf(["Y", "N"], "In Lab Only must be Y or N"),
    antimicrobialResistance: Yup.string().oneOf(
      ["Y", "N"],
      "Antimicrobial Resistance must be Y or N",
    ),
    active: Yup.string().oneOf(["Y", "N"], "Active must be Y or N"),
    sampleTypes: Yup.array().of(
      Yup.object().shape({
        typeId: Yup.string().required("Sample Type ID is required"),
        tests: Yup.array()
          .of(
            Yup.object().shape({
              id: Yup.number().required("Test ID is required"),
            }),
          )
          .required("Tests are required"),
      }),
    ),
    lowValid: Yup.string().required("Low Valid is required"),
    highValid: Yup.string().required("High Valid is required"),
    lowReportingRange: Yup.string().required("Low Reporting Range is required"),
    highReportingRange: Yup.string().required(
      "High Reporting Range is required",
    ),
    lowCritical: Yup.string().required("Low Critical is required"),
    highCritical: Yup.string().required("High Critical is required"),
    significantDigits: Yup.string().required("Significant Digits is required"),
    resultLimits: Yup.string().required("Result Limits are required"),
  });

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

  const handleTestAddData = (res) => {
    if (!res) {
      setIsLoading(true);
    } else {
      setTestAdd(res);
    }
  };

  useEffect(() => {
    if (testAdd) {
      setLabUnitList(testAdd.labUnitList || []);
      setPanelList(testAdd.panelList || []);
      setUomList(testAdd.uomList || []);
      setResultTypeList(testAdd.resultTypeList || []);
      setSampleTypeList(testAdd.sampleTypeList || []);
      setGroupedDictionaryList(testAdd.groupedDictionaryList || []);
      setDictionaryList(testAdd.dictionaryList || []);
      setAgeRangeList(testAdd.ageRangeList || []);
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

  const handleSampleType = (res) => {
    const selectedSampleTypeIds = selectedSampleType.map((type) => type.id);

    const isInSelectedSampleType = selectedSampleTypeIds.includes(
      res.sampleTypeId,
    );

    const extraTestItem = {
      id: "0",
      name: formData.testNameEnglish,
      userBenchChoice: false,
    };

    setSelectedSampleTypeResp((prev) => {
      const isAlreadyPresent = prev.some(
        (item) => item.sampleTypeId === res.sampleTypeId,
      );

      let updated;

      if (isInSelectedSampleType && !isAlreadyPresent) {
        updated = [
          ...prev,
          {
            ...res,
            tests: [...(res.tests || []), extraTestItem],
          },
        ];
      } else if (!isInSelectedSampleType) {
        updated = prev.filter((item) => item.sampleTypeId !== res.sampleTypeId);
      } else {
        updated = prev;
      }
      return updated;
    });
  };

  const handleTestAddPostCall = (values) => {
    if (!values) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: "Form submission failed due to missing data.",
      });
      setNotificationVisible(true);
      setTimeout(() => {
        window.location.reload();
      }, 500);
    }
    setIsLoading(true);
    postToOpenElisServerJsonResponse(
      `/rest/TestAdd`,
      JSON.stringify({ jsonWad: JSON.stringify(values) }),
      (res) => {
        handelTestAddPostCallback(res);
      },
    );
  };

  const handelTestAddPostCallback = (res) => {
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
  };

  const steps = [
    <StepOneTestNameAndTestSection
      key="step-1"
      formData={formData}
      setFormData={setFormData}
      validationSchema={validationSchema}
      handleNextStep={handleNextStep}
      labUnitList={labUnitList}
      setLabUnitList={setLabUnitList}
      selectedLabUnitId={selectedLabUnitList}
      setSelectedLabUnitList={setSelectedLabUnitList}
      jsonWad={jsonWad}
      setJsonWad={setJsonWad}
    />,
    <StepTwoTestPanelAndUom
      key="step-2"
      formData={formData}
      setFormData={setFormData}
      validationSchema={validationSchema}
      handleNextStep={handleNextStep}
      handlePreviousStep={handlePreviousStep}
      panelList={panelList}
      setPanelList={setPanelList}
      uomList={uomList}
      setUomList={setUomList}
      panelListTag={panelListTag}
      setPanelListTag={setPanelListTag}
      selectedUomList={selectedUomList}
      setSelectedUomList={setSelectedUomList}
      jsonWad={jsonWad}
      setJsonWad={setJsonWad}
    />,
    <StepThreeTestResultTypeAndLoinc
      key="step-3"
      formData={formData}
      setFormData={setFormData}
      validationSchema={validationSchema}
      handleNextStep={handleNextStep}
      handlePreviousStep={handlePreviousStep}
      resultTypeList={resultTypeList}
      setResultTypeList={setResultTypeList}
      selectedResultTypeList={selectedResultTypeList}
      setSelectedResultTypeList={setSelectedResultTypeList}
      intl={intl}
      addNotification={addNotification}
      setNotificationVisible={setNotificationVisible}
      lonic={lonic}
      setLonic={setLonic}
      jsonWad={jsonWad}
      setJsonWad={setJsonWad}
    />,
    <StepFourSelectSampleTypeAndTestDisplayOrder
      key="step-4"
      formData={formData}
      setFormData={setFormData}
      validationSchema={validationSchema}
      handleNextStep={handleNextStep}
      handlePreviousStep={handlePreviousStep}
      sampleTypeList={sampleTypeList}
      setSampleTypeList={setSampleTypeList}
      selectedSampleTypeList={selectedSampleTypeList}
      setSelectedSampleTypeList={setSelectedSampleTypeList}
      sampleTestTypeToGetTagList={sampleTestTypeToGetTagList}
      setSampleTestTypeToGetTagList={setSampleTestTypeToGetTagList}
      selectedSampleType={selectedSampleType}
      setSelectedSampleType={setSelectedSampleType}
      selectedSampleTypeResp={selectedSampleTypeResp}
      setSelectedSampleTypeResp={setSelectedSampleTypeResp}
      jsonWad={jsonWad}
      setJsonWad={setJsonWad}
      currentStep={currentStep}
    />,
    <StepFiveSelectListOptionsAndResultOrder
      key="step-5"
      formData={formData}
      setFormData={setFormData}
      validationSchema={validationSchema}
      handleNextStep={handleNextStep}
      handlePreviousStep={handlePreviousStep}
      groupedDictionaryList={groupedDictionaryList}
      setGroupedDictionaryList={setGroupedDictionaryList}
      selectedGroupedDictionaryList={selectedGroupedDictionaryList}
      setSelectedGroupedDictionaryList={setSelectedGroupedDictionaryList}
      dictionaryList={dictionaryList}
      setDictionaryList={setDictionaryList}
      dictionaryListTag={dictionaryListTag}
      resultTypeList={resultTypeList}
      setDictionaryListTag={setDictionaryListTag}
      selectedResultTypeList={selectedResultTypeList}
      setSelectedResultTypeList={setSelectedResultTypeList}
      singleSelectDictionaryList={singleSelectDictionaryList}
      setSingleSelectDictionaryList={setSingleSelectDictionaryList}
      multiSelectDictionaryList={multiSelectDictionaryList}
      setMultiSelectDictionaryList={setMultiSelectDictionaryList}
      multiSelectDictionaryListTag={multiSelectDictionaryListTag}
      setMultiSelectDictionaryListTag={setMultiSelectDictionaryListTag}
      jsonWad={jsonWad}
      setJsonWad={setJsonWad}
      currentStep={currentStep}
      setCurrentStep={setCurrentStep}
    />,
    <StepSixSelectRangeAgeRangeAndSignificantDigits
      key="step-6"
      formData={formData}
      setFormData={setFormData}
      validationSchema={validationSchema}
      handleNextStep={handleNextStep}
      handlePreviousStep={handlePreviousStep}
      selectedResultTypeList={selectedResultTypeList}
      ageRangeList={ageRangeList}
      setAgeRangeList={setAgeRangeList}
      gotSelectedAgeRangeList={gotSelectedAgeRangeList}
      setGotSelectedAgeRangeList={setGotSelectedAgeRangeList}
      jsonWad={jsonWad}
      setJsonWad={setJsonWad}
      currentStep={currentStep}
      setCurrentStep={setCurrentStep}
      ageRangeFields={ageRangeFields}
      setAgeRangeFields={setAgeRangeFields}
    />,
    <StepSevenFinalDisplayAndSaveConfirmation
      key="step-7"
      formData={formData}
      setFormData={setFormData}
      validationSchema={validationSchema}
      handleNextStep={handleNextStep}
      handlePreviousStep={handlePreviousStep}
      jsonWad={jsonWad}
      setJsonWad={setJsonWad}
      panelListTag={panelListTag}
      setPanelListTag={setPanelListTag}
      selectedUomList={selectedUomList}
      setSelectedUomList={setSelectedUomList}
      selectedLabUnitList={selectedLabUnitList}
      setSelectedLabUnitList={setSelectedLabUnitList}
      selectedResultTypeList={selectedResultTypeList}
      setSelectedResultTypeList={setSelectedResultTypeList}
      selectedSampleTypeList={selectedSampleTypeList}
      setSelectedSampleTypeList={setSelectedSampleTypeList}
      sampleTestTypeToGetTagList={sampleTestTypeToGetTagList}
      setSampleTestTypeToGetTagList={setSampleTestTypeToGetTagList}
      selectedSampleType={selectedSampleType}
      setSelectedSampleType={setSelectedSampleType}
      selectedSampleTypeResp={selectedSampleTypeResp}
      setSelectedSampleTypeResp={setSelectedSampleTypeResp}
      dictionaryListTag={dictionaryListTag}
      setDictionaryListTag={setDictionaryListTag}
      singleSelectDictionaryList={singleSelectDictionaryList}
      setSingleSelectDictionaryList={setSingleSelectDictionaryList}
      multiSelectDictionaryList={multiSelectDictionaryList}
      setMultiSelectDictionaryList={setMultiSelectDictionaryList}
      multiSelectDictionaryListTag={multiSelectDictionaryListTag}
      setMultiSelectDictionaryListTag={setMultiSelectDictionaryListTag}
      ageRangeList={ageRangeList}
      setAgeRangeList={setAgeRangeList}
      gotSelectedAgeRangeList={gotSelectedAgeRangeList}
      setGotSelectedAgeRangeList={setGotSelectedAgeRangeList}
      currentStep={currentStep}
    />,
  ];

  const rows = [
    {
      id: "name",
      field: intl.formatMessage({ id: "field.name" }),
      description: <FormattedMessage id="test.description.name" />,
    },
    {
      id: "reportName",
      field: intl.formatMessage({ id: "field.reportName" }),
      description: <FormattedMessage id="test.description.reportName" />,
    },
    {
      id: "testSection",
      field: intl.formatMessage({ id: "field.testSection" }),
      description: <FormattedMessage id="test.description.testSection" />,
    },
    {
      id: "panel",
      field: intl.formatMessage({ id: "field.panel" }),
      description: <FormattedMessage id="test.description.panel" />,
    },
    {
      id: "uom",
      field: intl.formatMessage({ id: "field.uom" }),
      description: <FormattedMessage id="test.description.uom" />,
    },
    {
      id: "resultType",
      field: intl.formatMessage({ id: "field.resultType" }),
      description: (
        <>
          <p>
            <FormattedMessage id="description.resultType.kind" />
          </p>
          <ul>
            <li>
              <strong>
                <FormattedMessage id="description.resultType.numeric" />
              </strong>
              <FormattedMessage id="description.resultType.numericDesc" />
            </li>
            <li>
              <strong>
                <FormattedMessage id="description.resultType.alphanumeric" />
              </strong>
              <FormattedMessage id="description.resultType.alphanumericDesc" />
            </li>
            <li>
              <strong>
                <FormattedMessage id="description.resultType.textArea" />
              </strong>
              <FormattedMessage id="description.resultType.textAreaDesc" />
            </li>
            <li>
              <strong>
                <FormattedMessage id="description.resultType.selectList" />
              </strong>
              <FormattedMessage id="description.resultType.selectListDesc" />
            </li>
            <li>
              <strong>
                <FormattedMessage id="description.resultType.multiSelectList" />
              </strong>
              <FormattedMessage id="description.resultType.multiSelectListDesc" />
            </li>
            <li>
              <strong>
                <FormattedMessage id="description.resultType.cascadingMultiSelectList" />
              </strong>
              <FormattedMessage id="description.resultType.cascadingMultiSelectListDesc" />
            </li>
          </ul>
        </>
      ),
    },
    {
      id: "active",
      field: intl.formatMessage({ id: "test.field.active" }),
      description: <FormattedMessage id="test.description.active" />,
    },
    {
      id: "orderable",
      field: intl.formatMessage({ id: "test.field.orderable" }),
      description: <FormattedMessage id="test.description.orderable" />,
    },
  ];

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
            <Column lg={16} md={8} sm={4}>
              <Toggle
                id="toggle"
                labelText="Show Guide"
                onClick={handleToggleShowGuide}
              />
            </Column>
          </Grid>
          {showGuide && <CustomShowGuide rows={rows} />}
          <br />
          <hr />
          <br />
          <div>{steps[currentStep]}</div>
          <br />
          <hr />
          <br />
        </div>
      </div>
    </>
  );
}

export default injectIntl(TestAdd);

const StepOneTestNameAndTestSection = ({
  formData,
  setFormData,
  validationSchema,
  handleNextStep,
  labUnitList,
  setLabUnitList,
  selectedLabUnitId,
  setSelectedLabUnitList,
  jsonWad,
  setJsonWad,
}) => {
  const handleSubmit = (values) => {
    handleNextStep(values, true);
  };

  return (
    <>
      <Formik
        initialValues={formData}
        enableReinitialize={true}
        validationSchema={Yup.object({
          testSection: Yup.string()
            .required("Test section is required")
            .notOneOf(["0"], "Please select a valid test section"),
          testNameEnglish: Yup.string()
            .matches(/^[A-Za-z\s]+$/, "Only letters and spaces are allowed")
            .trim()
            .required("English test name is required"),
          testNameFrench: Yup.string()
            .matches(/^[A-Za-z\s]+$/, "Only letters and spaces are allowed")
            .trim()
            .required("French test name is required"),
          testReportNameEnglish: Yup.string()
            .matches(/^[A-Za-z\s]+$/, "Only letters and spaces are allowed")
            .trim()
            .required("English report name is required"),
          testReportNameFrench: Yup.string()
            .matches(/^[A-Za-z\s]+$/, "Only letters and spaces are allowed")
            .trim()
            .required("French report name is required"),
        })}
        validateOnChange={true}
        validateOnBlur={true}
        onSubmit={(values, actions) => {
          handleSubmit(values);
          actions.setSubmitting(false);
        }}
      >
        {({
          values,
          handleChange,
          handleBlur,
          touched,
          errors,
          setFieldValue,
        }) => {
          const testNameEn = (e) => {
            setJsonWad((prev) => ({
              ...prev,
              testNameEnglish: e.target.value,
            }));
          };

          const testNameFr = (e) => {
            setJsonWad((prev) => ({
              ...prev,
              testNameFrench: e.target.value,
            }));
          };

          const reportingTestNameEn = (e) => {
            setJsonWad((prev) => ({
              ...prev,
              testReportNameEnglish: e.target.value,
            }));
          };

          const reportingTestNameFr = (e) => {
            setJsonWad((prev) => ({
              ...prev,
              testReportNameFrench: e.target.value,
            }));
          };

          const copyInputValuesFromTestNameEnFr = (values) => {
            setJsonWad((prev) => ({
              ...prev,
              testReportNameEnglish: values.testNameEnglish,
              testReportNameFrench: values.testNameFrench,
            }));
            setFieldValue("testReportNameEnglish", values.testNameEnglish);
            setFieldValue("testReportNameFrench", values.testNameFrench);
          };

          const handelTestSectionSelect = (e) => {
            setJsonWad((prev) => ({
              ...prev,
              testSection: e.target.value,
            }));

            setFieldValue("testSection", e.target.value);

            const selectedLabUnitObject = labUnitList.find(
              (item) => item.id === e.target.value,
            );

            if (selectedLabUnitObject) {
              setSelectedLabUnitList(selectedLabUnitObject);
            }
          };
          return (
            <Form>
              <Grid fullWidth={true}>
                <Column lg={16} md={8} sm={4}>
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
                      invalid={touched.testSection && !!errors.testSection}
                      invalidText={touched.testSection && errors.testSection}
                      name="testSection"
                      onChange={handelTestSectionSelect}
                      onBlur={handleBlur}
                      value={values.testSection}
                    >
                      <SelectItem value="0" text="Select Test Section" />
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
                      name="testNameEnglish"
                      value={values.testNameEnglish}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      required
                      invalid={
                        touched.testNameEnglish && !!errors.testNameEnglish
                      }
                      invalidText={
                        touched.testNameEnglish && errors.testNameEnglish
                      }
                    />
                    <br />
                    <FormattedMessage id="french.label" />
                    <br />
                    <TextInput
                      labelText=""
                      id="testNameFr"
                      name="testNameFrench"
                      value={values.testNameFrench}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      required
                      invalid={
                        touched.testNameFrench && !!errors.testNameFrench
                      }
                      invalidText={
                        touched.testNameFrench && errors.testNameFrench
                      }
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
                      onClick={() => {
                        copyInputValuesFromTestNameEnFr(values);
                      }}
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
                      name="testReportNameEnglish"
                      value={values.testReportNameEnglish}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      required
                      invalid={
                        touched.testReportNameEnglish &&
                        !!errors.testReportNameEnglish
                      }
                      invalidText={
                        touched.testReportNameEnglish &&
                        errors.testReportNameEnglish
                      }
                    />
                    <br />
                    <FormattedMessage id="french.label" />
                    <br />
                    <TextInput
                      labelText=""
                      id="reportingTestNameFr"
                      name="testReportNameFrench"
                      value={values.testReportNameFrench}
                      onChange={handleChange}
                      onBlur={handleBlur}
                      required
                      invalid={
                        touched.testReportNameFrench &&
                        !!errors.testReportNameFrench
                      }
                      invalidText={
                        touched.testReportNameFrench &&
                        errors.testReportNameFrench
                      }
                    />
                  </div>
                </Column>
              </Grid>
              <br />
              <Grid fullWidth={true}>
                <Column lg={16} md={8} sm={4}>
                  <Button type="submit">
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
            </Form>
          );
        }}
      </Formik>
    </>
  );
};

const StepTwoTestPanelAndUom = ({
  handleNextStep,
  handlePreviousStep,
  panelList,
  setPanelList,
  uomList,
  setUomList,
  panelListTag,
  setPanelListTag,
  formData,
  validationSchema,
  jsonWad,
  setJsonWad,
  selectedUomList,
  setSelectedUomList,
}) => {
  const handleSubmit = (values) => {
    handleNextStep(values, true);
  };

  return (
    <>
      <Formik
        initialValues={formData}
        validationSchema={Yup.object({
          panels: Yup.array()
            .min(1, "At least one panel must be selected")
            .of(
              Yup.object().shape({
                id: Yup.string()
                  .required("Panel ID is required")
                  .oneOf(
                    panelList.map((item) => item.id),
                    "Please select a valid panel",
                  ),
              }),
            )
            .notOneOf(
              [Yup.array().of(Yup.object().shape({ id: "0" }))],
              "Please select a valid panel",
            ),
          uom: Yup.string()
            .notOneOf(["0", ""], "Please select a valid unit of measurement")
            .required("Unit of measurement is required"),
        })}
        enableReinitialize={true}
        validateOnChange={true}
        validateOnBlur={true}
        onSubmit={(values, actions) => {
          handleSubmit(values);
          actions.setSubmitting(false);
        }}
      >
        {({
          values,
          handleChange,
          handleBlur,
          touched,
          errors,
          setFieldValue,
        }) => {
          const handelPanelSelectSetTag = (e) => {
            const selectedId = e.target.value;
            const selectedValue = e.target.options[e.target.selectedIndex].text;

            setPanelListTag((prevTags) => {
              const isTagPresent = prevTags.some(
                (tag) => tag.id === selectedId,
              );
              if (isTagPresent) return prevTags;

              const newTag = { id: selectedId, value: selectedValue };
              const updatedTags = [...prevTags, newTag];

              const updatedPanels = [
                ...updatedTags.map((tag) => ({ id: tag.id })),
              ];
              setJsonWad((prevJsonWad) => ({
                ...prevJsonWad,
                panels: updatedPanels,
              }));

              return updatedTags;
            });

            setFieldValue("panels", [...values.panels, { id: selectedId }]);

            setJsonWad((prev) => ({ ...prev, panel: selectedId }));
            const selectedPanelObject = panelList.find(
              (item) => item.id === selectedId,
            );
            if (selectedPanelObject) {
              setPanelList((prev) => [...prev, selectedPanelObject]);
            }
          };

          const handlePanelRemoveTag = (idToRemove) => {
            setPanelListTag((prevTags) => {
              const updatedTags = prevTags.filter(
                (tag) => tag.id !== idToRemove,
              );

              const updatedPanels = updatedTags.map((tag) => ({ id: tag.id }));
              setJsonWad((prevJsonWad) => ({
                ...prevJsonWad,
                panels: updatedPanels,
              }));

              return updatedTags;
            });
          };

          const handelUomSelect = (e) => {
            setJsonWad((prev) => ({ ...prev, uom: e.target.value }));

            const selectedUomObject = uomList.find(
              (item) => item.id === e.target.value,
            );

            setFieldValue("uom", e.target.value);

            if (selectedUomObject) {
              setSelectedUomList(selectedUomObject);
            }
          };

          return (
            <Form>
              <Grid fullWidth={true}>
                <Column lg={16} md={8} sm={4}>
                  <FormattedMessage id="field.panel" />
                  <Select
                    onBlur={handleBlur}
                    id={`select-panel`}
                    name="panels"
                    onChange={(e) => {
                      handelPanelSelectSetTag(e);
                    }}
                    hideLabel
                    required
                    invalid={touched.panels && !!errors.panels}
                    invalidText={touched.panels && errors.panels}
                  >
                    <SelectItem value="0" text="Select Panel" />
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
                    onBlur={handleBlur}
                    onChange={(e) => {
                      handelUomSelect(e);
                    }}
                    id={`select-uom`}
                    name="uom"
                    hideLabel
                    required
                    invalid={touched.uom && !!errors.uom}
                    invalidText={touched.uom && errors.uom}
                    value={values.uom}
                  >
                    <SelectItem value="0" text="Select Unit Of Measurement" />
                    {uomList?.map((test) => (
                      <SelectItem
                        key={test.id}
                        value={test.id}
                        text={`${test.value}`}
                      />
                    ))}
                  </Select>
                </Column>
              </Grid>
              <br />
              <Grid fullWidth={true}>
                <Column lg={16} md={8} sm={4}>
                  <Button type="submit">
                    <FormattedMessage id="next.action.button" />
                  </Button>{" "}
                  <Button
                    onClick={() => handlePreviousStep(values)}
                    kind="tertiary"
                    type="button"
                  >
                    <FormattedMessage id="back.action.button" />
                  </Button>
                </Column>
              </Grid>
            </Form>
          );
        }}
      </Formik>
    </>
  );
};

const StepThreeTestResultTypeAndLoinc = ({
  formData,
  validationSchema,
  handleNextStep,
  handlePreviousStep,
  resultTypeList,
  setResultTypeList,
  selectedResultTypeList,
  setSelectedResultTypeList,
  intl,
  addNotification,
  setNotificationVisible,
  lonic,
  setLonic,
  jsonWad,
  setJsonWad,
}) => {
  const handleSubmit = (values) => {
    handleNextStep(values, true);
  };

  return (
    <>
      <Formik
        initialValues={formData}
        validationSchema={Yup.object({
          resultType: Yup.string()
            .notOneOf(["0", ""], "Please select a valid Result Type")
            .required("Result Type is required"),
          loinc: Yup.string().matches(
            /^(?!-)(?:\d+-)*\d*$/,
            "Loinc must contain only numbers",
          ),
          // .required("Loinc is required"),
          orderable: Yup.string().oneOf(["Y", "N"], "Orderable must be Y or N"),
          notifyResults: Yup.string().oneOf(
            ["Y", "N"],
            "Notify Results must be Y or N",
          ),
          inLabOnly: Yup.string().oneOf(
            ["Y", "N"],
            "In Lab Only must be Y or N",
          ),
          antimicrobialResistance: Yup.string().oneOf(
            ["Y", "N"],
            "Antimicrobial Resistance must be Y or N",
          ),
          active: Yup.string().oneOf(["Y", "N"], "Active must be Y or N"),
        })}
        enableReinitialize={true}
        validateOnChange={true}
        validateOnBlur={true}
        onSubmit={(values, actions) => {
          handleSubmit(values);
          actions.setSubmitting(false);
        }}
      >
        {({
          values,
          handleChange,
          handleBlur,
          touched,
          errors,
          setFieldValue,
        }) => {
          const handelLonicChange = (e) => {
            const regex = /^(?!-)(?:\d+-)*\d+$/;

            const value = e.target.value;

            setFieldValue("loinc", value);

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
          };

          const handelResultType = (e) => {
            setJsonWad((prev) => ({ ...prev, resultType: e.target.value }));

            const selectedResultTypeObject = resultTypeList.find(
              (item) => item.id == e.target.value,
            );

            setFieldValue("resultType", e.target.value);

            if (selectedResultTypeObject) {
              setSelectedResultTypeList(selectedResultTypeObject);
            }
          };

          const handleAntimicrobialResistance = (e) => {
            setJsonWad((prev) => ({
              ...prev,
              antimicrobialResistance: e.target.checked ? "Y" : "N",
            }));

            setFieldValue(
              "antimicrobialResistance",
              e.target.checked ? "Y" : "N",
            );
          };
          const handleIsActive = (e) => {
            setJsonWad((prev) => ({
              ...prev,
              active: e.target.checked ? "Y" : "N",
            }));
            setFieldValue("active", e.target.checked ? "Y" : "N");
          };
          const handleOrderable = (e) => {
            setJsonWad((prev) => ({
              ...prev,
              orderable: e.target.checked ? "Y" : "N",
            }));
            setFieldValue("orderable", e.target.checked ? "Y" : "N");
          };
          const handleNotifyPatientofResults = (e) => {
            setJsonWad((prev) => ({
              ...prev,
              notifyResults: e.target.checked ? "Y" : "N",
            }));
            setFieldValue("notifyResults", e.target.checked ? "Y" : "N");
          };
          const handleInLabOnly = (e) => {
            setJsonWad((prev) => ({
              ...prev,
              inLabOnly: e.target.checked ? "Y" : "N",
            }));
            setFieldValue("inLabOnly", e.target.checked ? "Y" : "N");
          };

          return (
            <Form>
              <Grid fullWidth={true}>
                <Column lg={16} md={8} sm={4}>
                  <div>
                    <>
                      <FormattedMessage id="field.resultType" />
                      <span style={{ color: "red" }}>*</span>
                    </>
                    <br />
                    <Select
                      onBlur={handleBlur}
                      id={`select-result-type`}
                      name="resultType"
                      hideLabel
                      required
                      onChange={(e) => {
                        handelResultType(e);
                      }}
                      value={values.resultType}
                      invalid={touched.resultType && !!errors.resultType}
                      invalidText={touched.resultType && errors.resultType}
                    >
                      <SelectItem value="0" text="Select Result Type" />
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
                      id="loinc"
                      name="loinc"
                      value={values.loinc}
                      placeholder={`Example : 430-0, 43166-0, 43167-8`}
                      onChange={(e) => {
                        handelLonicChange(e);
                        handleChange(e);
                      }}
                      invalid={touched.loinc && !!errors.loinc}
                      invalidText={touched.loinc && errors.loinc}
                    />
                  </div>
                  <br />
                  <div>
                    <Checkbox
                      labelText={
                        <FormattedMessage id="test.antimicrobialResistance" />
                      }
                      id="antimicrobial-resistance"
                      name="antimicrobialResistance"
                      onChange={handleAntimicrobialResistance}
                      checked={values?.antimicrobialResistance === "Y"}
                    />
                    <Checkbox
                      labelText={
                        <FormattedMessage id="dictionary.category.isActive" />
                      }
                      id="is-active"
                      name="active"
                      onChange={handleIsActive}
                      checked={values?.active === "Y"}
                    />
                    <Checkbox
                      labelText={<FormattedMessage id="label.orderable" />}
                      id="orderable"
                      name="orderable"
                      onChange={handleOrderable}
                      checked={values?.orderable === "Y"}
                    />
                    <Checkbox
                      labelText={<FormattedMessage id="test.notifyResults" />}
                      id="notify-patient-of-results"
                      name="notifyResults"
                      onChange={handleNotifyPatientofResults}
                      checked={values?.notifyResults === "Y"}
                    />
                    <Checkbox
                      labelText={<FormattedMessage id="test.inLabOnly" />}
                      id="in-lab-only"
                      name="inLabOnly"
                      onChange={handleInLabOnly}
                      checked={values?.inLabOnly === "Y"}
                    />
                  </div>
                </Column>
              </Grid>
              <br />
              <Grid fullWidth={true}>
                <Column lg={16} md={8} sm={4}>
                  <Button type="submit">
                    <FormattedMessage id="next.action.button" />
                  </Button>{" "}
                  <Button
                    onClick={() => handlePreviousStep(values)}
                    kind="tertiary"
                    type="button"
                  >
                    <FormattedMessage id="back.action.button" />
                  </Button>
                </Column>
              </Grid>
            </Form>
          );
        }}
      </Formik>
    </>
  );
};

const StepFourSelectSampleTypeAndTestDisplayOrder = ({
  formData,
  setFormData,
  validationSchema,
  handleNextStep,
  handlePreviousStep,
  sampleTypeList,
  setSampleTypeList,
  selectedSampleTypeList,
  setSelectedSampleTypeList,
  sampleTestTypeToGetTagList,
  setSampleTestTypeToGetTagList,
  selectedSampleType,
  setSelectedSampleType,
  selectedSampleTypeResp,
  setSelectedSampleTypeResp,
  jsonWad,
  setJsonWad,
  currentStep,
}) => {
  const handleSubmit = (values) => {
    handleNextStep(values, true);
  };

  useEffect(() => {
    if (!selectedSampleTypeResp.length) return;

    const existingTypeIds = new Set(
      (formData.sampleTypes || []).map((st) => st.typeId),
    );

    const newOnes = selectedSampleTypeResp.filter(
      (resp) => !existingTypeIds.has(String(resp.sampleTypeId)),
    );

    if (newOnes.length === 0) return;

    const newTransformed = newOnes.map((resp) => ({
      typeId: String(resp.sampleTypeId),
      tests: (resp.tests || []).map((t) => ({ id: Number(t.id) })),
    }));

    // setFieldValue("sampleTypes", [
    //   ...(values.sampleTypes || []),
    //   ...newTransformed,
    // ]);

    const updatedSampleTypes = [
      ...(formData.sampleTypes || []),
      ...newTransformed,
    ];

    setFormData((prev) => ({
      ...prev,
      sampleTypes: updatedSampleTypes,
    }));
  }, [selectedSampleTypeResp]);

  return (
    <>
      {currentStep === 3 ? (
        <>
          <Formik
            initialValues={formData}
            validationSchema={Yup.object({
              sampleTypes: Yup.array()
                .min(1, "At least one sample type must be selected")
                .of(
                  Yup.object().shape({
                    // id: Yup.string().required("Sample Type ID is required"),
                    // value: Yup.string().required(
                    //   "Sample Type Value is required",
                    // ),
                    typeId: Yup.string().required("Sample Type ID is required"),
                    tests: Yup.array().of(
                      Yup.object().shape({
                        id: Yup.number().required(),
                      }),
                    ),
                  }),
                ),
            })}
            enableReinitialize={true}
            validateOnChange={true}
            validateOnBlur={true}
            onSubmit={(values, actions) => {
              handleSubmit(values);
              actions.setSubmitting(false);
            }}
          >
            {({
              values,
              handleChange,
              handleBlur,
              touched,
              errors,
              setFieldValue,
            }) => {
              const handleSampleTypeListSelectIdTestTag = (e) => {
                const selectedId = e.target.value;
                const selectedSampleTypeObject = sampleTypeList.find(
                  (type) => type.id === selectedId,
                );

                if (!selectedSampleTypeObject) return;

                const isAlreadySelected = selectedSampleType.some(
                  (type) => type.id === selectedSampleTypeObject.id,
                );

                if (!isAlreadySelected) {
                  // const matchedResp = selectedSampleTypeResp.find(
                  //   (resp) => String(resp.sampleTypeId) === selectedId,
                  // );

                  // const testList =
                  //   matchedResp?.tests?.map((t) => ({ id: Number(t.id) })) ??
                  //   [];

                  // const transformedSampleType = {
                  //   typeId: selectedId,
                  //   tests: testList,
                  // };

                  const updatedList = [
                    ...selectedSampleTypeList,
                    selectedSampleTypeObject,
                  ];

                  setSelectedSampleTypeList(updatedList);
                  setSampleTestTypeToGetTagList((prev) => [
                    ...prev,
                    selectedSampleTypeObject,
                  ]);
                  setSelectedSampleType((prev) => [
                    ...prev,
                    selectedSampleTypeObject,
                  ]);
                  // setFieldValue("sampleTypes", [
                  //   ...(values.sampleTypes || []),
                  //   transformedSampleType,
                  // ]);
                }
              };

              const handleRemoveSampleTypeListSelectIdTestTag = (
                indexToRemove,
              ) => {
                const filterByIndex = (_, index) => index !== indexToRemove;

                setFieldValue(
                  "sampleTypes",
                  selectedSampleTypeList.filter(filterByIndex),
                );
                setSelectedSampleTypeList((prev) => prev.filter(filterByIndex));
                setSelectedSampleType((prev) => prev.filter(filterByIndex));
                setSelectedSampleTypeResp((prev) => prev.filter(filterByIndex));

                setSampleTestTypeToGetTagList((prevTags) => {
                  const updatedTags = prevTags.filter(filterByIndex);
                  setJsonWad((prev) => ({
                    ...prev,
                    replace: updatedTags.map((item) => item.id),
                  }));
                  return updatedTags;
                });
              };

              return (
                <Form>
                  <Grid fullWidth={true}>
                    <Column lg={6} md={2} sm={4}>
                      <FormattedMessage id="sample.type" />
                      <br />
                      <Select
                        onBlur={handleBlur}
                        id={`select-sample-type`}
                        name="sampleTypes"
                        hideLabel
                        required
                        onChange={(e) => handleSampleTypeListSelectIdTestTag(e)}
                        invalid={touched.sampleTypes && !!errors.sampleTypes}
                        invalidText={touched.sampleTypes && errors.sampleTypes}
                      >
                        <SelectItem value="0" text="Select Sample Type" />
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
                            {sampleTestTypeToGetTagList.map(
                              (section, index) => (
                                <Tag
                                  filter
                                  key={`testTags_${index}`}
                                  onClose={() =>
                                    handleRemoveSampleTypeListSelectIdTestTag(
                                      index,
                                    )
                                  }
                                  style={{ marginRight: "0.5rem" }}
                                  type={"green"}
                                >
                                  {String(section.value)}
                                </Tag>
                              ),
                            )}
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
                                <CustomCommonSortableOrderList
                                  test={item.tests}
                                  onSort={(updatedList) => {
                                    const newList = selectedSampleTypeResp.map(
                                      (sampleType) => {
                                        if (
                                          sampleType.sampleTypeId ===
                                          item.sampleTypeId
                                        ) {
                                          return {
                                            ...sampleType,
                                            tests: updatedList,
                                          };
                                        }
                                        return sampleType;
                                      },
                                    );
                                    setSelectedSampleTypeResp(newList);
                                  }}
                                  disableSorting={false}
                                />
                              </Section>
                            </div>
                            <br />
                          </>
                        ))
                      ) : (
                        <></>
                      )}
                    </Column>
                  </Grid>
                  <br />
                  <Grid fullWidth={true}>
                    <Column lg={16} md={8} sm={4}>
                      <Button type="submit">
                        <FormattedMessage id="next.action.button" />
                      </Button>{" "}
                      <Button
                        onClick={() => handlePreviousStep(values)}
                        kind="tertiary"
                        type="button"
                      >
                        <FormattedMessage id="back.action.button" />
                      </Button>
                    </Column>
                  </Grid>
                </Form>
              );
            }}
          </Formik>
        </>
      ) : (
        <></>
      )}
    </>
  );
};

const StepFiveSelectListOptionsAndResultOrder = ({
  formData,
  validationSchema,
  handleNextStep,
  handlePreviousStep,
  groupedDictionaryList,
  setGroupedDictionaryList,
  selectedGroupedDictionaryList,
  setSelectedGroupedDictionaryList,
  resultTypeList,
  dictionaryList,
  setDictionaryList,
  dictionaryListTag,
  setDictionaryListTag,
  selectedResultTypeList,
  setSelectedResultTypeList,
  singleSelectDictionaryList,
  setSingleSelectDictionaryList,
  multiSelectDictionaryList,
  setMultiSelectDictionaryList,
  multiSelectDictionaryListTag,
  setMultiSelectDictionaryListTag,
  jsonWad,
  setJsonWad,
  currentStep,
  setCurrentStep,
}) => {
  const handleSubmit = (values) => {
    handleNextStep(values, true);
  };
  return (
    <>
      {currentStep === 4 &&
      ["2", "6", "7"].includes(selectedResultTypeList?.id) ? (
        <>
          <Formik
            initialValues={formData}
            validationSchema={Yup.object({
              dictionary: Yup.array()
                .min(1, "At least one dictionary option must be selected")
                .of(
                  Yup.object().shape({
                    id: Yup.string().required(),
                    // qualified: Yup.string().oneOf(["Y", "N"]),
                    qualified: Yup.string().required(),
                  }),
                ),
              // .test(
              //   "at-least-one-qualified",
              //   "At least one dictionary item must be qualified as 'Y'",
              //   (arr) => arr && arr.some((item) => item.qualified === "Y"),
              // ),
              dictionaryReference: Yup.string(),
              // .required(
              //   "Dictionary Reference is required",
              // ),
              defaultTestResult: Yup.string(),
              // .required(
              //   "Dictionary Default is required",
              // ),
            })}
            enableReinitialize={true}
            validateOnChange={true}
            validateOnBlur={true}
            onSubmit={(values, actions) => {
              const transformedDictionary = (values.dictionary || []).map(
                (item) => ({
                  // value: item.id,
                  id: item.id, // maybe a fix
                  qualified: item.qualified,
                }),
              );

              const payload = {
                ...values,
                dictionary: transformedDictionary,
              };
              handleSubmit(payload);
              actions.setSubmitting(false);
            }}
          >
            {({
              values,
              handleChange,
              handleBlur,
              touched,
              errors,
              setFieldValue,
            }) => {
              const handelSelectListOptions = (e) => {
                const selectedId = e.target.value;

                const selectedObject = dictionaryList.find(
                  (item) => item.id === selectedId,
                );

                if (!selectedObject) return;

                setSingleSelectDictionaryList((prev) => [
                  ...prev,
                  selectedObject,
                ]);

                setMultiSelectDictionaryList((prev) => [
                  ...prev,
                  selectedObject,
                ]);

                setDictionaryListTag((prev) => [...prev, selectedObject]);

                if (
                  selectedObject &&
                  !values.dictionary?.some(
                    (item) => item.id === selectedObject.id,
                  )
                ) {
                  setFieldValue("dictionary", [
                    ...(values.dictionary || []),
                    { id: selectedObject.id, qualified: "N" },
                  ]);
                }

                //set the data object in jsonWad
              };

              const handleSelectQualifiersTag = (e) => {
                const selectedId = e.target.value;

                const selectedObject = multiSelectDictionaryList.find(
                  (item) => item.id === selectedId,
                );

                if (!selectedObject) return;

                setMultiSelectDictionaryListTag((prev) =>
                  prev.some((item) => item.id === selectedObject.id)
                    ? prev
                    : [...prev, selectedObject],
                );

                setFieldValue(
                  "dictionary",
                  (values.dictionary || []).map((item) => ({
                    ...item,
                    qualified:
                      item.id === selectedObject.id ? "Y" : item.qualified,
                  })),
                );

                //set the data object in jsonWad
              };

              const handleRemoveMultiSelectDictionaryListTagSelectIdTestTag = (
                index,
              ) => {
                setMultiSelectDictionaryListTag((prevTags) => {
                  const updatedTags = prevTags.filter(
                    (_, idx) => idx !== index,
                  );

                  const updatedMultiSelectList = updatedTags.map((tag) => ({
                    id: tag.id,
                    value: tag.value,
                  }));

                  setJsonWad((prevJsonWad) => ({
                    ...prevJsonWad,
                    dictionary: updatedMultiSelectList,
                  }));

                  return updatedTags;
                });

                setMultiSelectDictionaryList((prevList) =>
                  prevList.filter((_, idx) => idx !== index),
                );

                setFieldValue(
                  "dictionary",
                  values.dictionary.filter((_, idx) => idx !== index),
                );
              };

              const handleRemoveDictionaryListSelectIdTestTag = (index) => {
                setDictionaryListTag((prevTags) => {
                  const updatedTags = prevTags.filter(
                    (_, idx) => idx !== index,
                  );

                  const updatedDictionaryList = updatedTags.map((tag) => ({
                    id: tag.id,
                    value: tag.value,
                  }));

                  setJsonWad((prevJsonWad) => ({
                    ...prevJsonWad,
                    dictionary: updatedDictionaryList,
                  }));

                  return updatedTags;
                });

                setSingleSelectDictionaryList((prevList) =>
                  prevList.filter((_, idx) => idx !== index),
                );

                setMultiSelectDictionaryList((prevList) =>
                  prevList.filter((_, idx) => idx !== index),
                );

                setFieldValue(
                  "dictionary",
                  values.dictionary.filter((_, idx) => idx !== index),
                );
              };

              return (
                <Form>
                  <Grid>
                    <Column lg={8} md={8} sm={4}>
                      <FormattedMessage id="label.select.list.options" />
                      <br />
                      <Select
                        onBlur={handleBlur}
                        id={`select-list-options`}
                        name="dictionary"
                        hideLabel
                        required
                        onChange={(e) => handelSelectListOptions(e)}
                        invalid={touched.dictionary && !!errors.dictionary}
                        invalidText={touched.dictionary && errors.dictionary}
                        value={values.dictionary.map((item) => item.id)}
                      >
                        <SelectItem value="0" text="Select List Option" />
                        {dictionaryList?.map((test) => (
                          <SelectItem
                            key={test.id}
                            value={test.id}
                            text={`${test.value}`}
                          />
                        ))}
                      </Select>
                      <br />
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
                                onClose={() =>
                                  handleRemoveDictionaryListSelectIdTestTag(
                                    index,
                                  )
                                }
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
                      {singleSelectDictionaryList &&
                        singleSelectDictionaryList?.length > 0 && (
                          <CustomCommonSortableOrderList
                            test={singleSelectDictionaryList}
                            disableSorting={false}
                            onSort={(updatedList) => {
                              setSingleSelectDictionaryList(updatedList);

                              const updatedFormikValues = updatedList.map(
                                (item) => ({
                                  id: item.id,
                                  qualified:
                                    values.dictionary.find(
                                      (d) => d.id === item.id,
                                    )?.qualified || "N",
                                }),
                              );

                              setFieldValue("dictionary", updatedFormikValues);

                              setJsonWad((prev) => ({
                                ...prev,
                                dictionary: updatedList.map(
                                  ({ id, value }) => ({ id, value }),
                                ),
                              }));
                            }}
                          />
                        )}
                      <br />
                      <br />
                      <FormattedMessage id="label.reference.value" />
                      <br />
                      <Select
                        onBlur={handleBlur}
                        id={`select-reference-value`}
                        name="dictionaryReference"
                        hideLabel
                        onChange={(e) =>
                          setFieldValue("dictionaryReference", e.target.value)
                        }
                        invalid={
                          touched.dictionaryReference &&
                          !!errors.dictionaryReference
                        }
                        invalidText={
                          touched.dictionaryReference &&
                          errors.dictionaryReference
                        }
                        value={values.dictionaryReference}
                      >
                        <SelectItem value="0" text="Select Reference Value" />
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
                      <Select
                        onBlur={handleBlur}
                        id={`select-default-result`}
                        name="defaultTestResult"
                        hideLabel
                        onChange={(e) =>
                          setFieldValue("defaultTestResult", e.target.value)
                        }
                        invalid={
                          touched.defaultTestResult &&
                          !!errors.defaultTestResult
                        }
                        invalidText={
                          touched.defaultTestResult && errors.defaultTestResult
                        }
                        value={values.defaultTestResult}
                      >
                        <SelectItem
                          value="0"
                          text="Select Single Dictionary List"
                        />
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
                        onBlur={handleBlur}
                        id={`select-qualifiers`}
                        hideLabel
                        onChange={(e) => {
                          handleSelectQualifiersTag(e);
                        }}
                        invalid={
                          touched.dictionary &&
                          (typeof errors.dictionary === "string" ||
                            (Array.isArray(errors.dictionary) &&
                              errors.dictionary.some(
                                (item) => item.qualified === "Y",
                              )))
                        }
                        invalidText={
                          touched.dictionary &&
                          (typeof errors.dictionary === "string"
                            ? errors.dictionary
                            : Array.isArray(errors.dictionary) &&
                                errors.dictionary.some(
                                  (item) => item.qualified === "Y",
                                )
                              ? "At least one dictionary item must be qualified as 'Y'"
                              : "")
                        }
                        value={values.dictionary
                          .filter((item) => item.qualified === "Y")
                          .map((item) => item.id)}
                        name="dictionary"
                      >
                        <SelectItem
                          value="0"
                          text="Select Multi Dictionary List"
                        />
                        {multiSelectDictionaryList?.map((test) => (
                          <SelectItem
                            key={test.id}
                            value={test.id}
                            text={`${test.value}`}
                          />
                        ))}
                      </Select>
                      <br />
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
                                onClose={() =>
                                  handleRemoveMultiSelectDictionaryListTagSelectIdTestTag(
                                    index,
                                  )
                                }
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
                    {groupedDictionaryList &&
                      groupedDictionaryList.map((gdl, index) => {
                        return (
                          <Column
                            style={{ margin: "2px" }}
                            key={`grouped-dictionary-list-${index}`}
                            lg={4}
                            md={4}
                            sm={4}
                          >
                            <ClickableTile
                              onClick={() => {
                                setDictionaryListTag([]);
                                setMultiSelectDictionaryListTag([]);

                                setSelectedGroupedDictionaryList(
                                  gdl.map((gdlVal) => gdlVal.id),
                                );

                                setMultiSelectDictionaryList(
                                  gdl.map((gdlVal) => ({
                                    id: gdlVal.id,
                                    value: gdlVal.value,
                                  })),
                                );

                                setSingleSelectDictionaryList(
                                  gdl.map((gdlVal) => ({
                                    id: gdlVal.id,
                                    value: gdlVal.value,
                                  })),
                                );

                                setDictionaryListTag(
                                  gdl.map((gdlVal) => ({
                                    id: gdlVal.id,
                                    value: gdlVal.value,
                                  })),
                                );

                                setFieldValue(
                                  "dictionary",
                                  gdl.map((gdlVal) => ({
                                    id: gdlVal.id,
                                    qualified: "N",
                                  })),
                                );
                              }}
                            >
                              <Section>
                                <Section>
                                  <Section>
                                    <Section>
                                      <Heading
                                        style={{
                                          textDecoration: "underline",
                                        }}
                                      >
                                        Select
                                      </Heading>
                                    </Section>
                                  </Section>
                                </Section>
                              </Section>
                              {gdl &&
                                gdl.map((gdlVal) => {
                                  return (
                                    <div key={gdlVal.id}>{gdlVal.value}</div>
                                  );
                                })}
                            </ClickableTile>
                          </Column>
                        );
                      })}
                  </Grid>
                  <br />
                  <Grid fullWidth={true}>
                    <Column lg={16} md={8} sm={4}>
                      <Button type="submit">
                        <FormattedMessage id="next.action.button" />
                      </Button>{" "}
                      <Button
                        onClick={() => handlePreviousStep(values)}
                        kind="tertiary"
                        type="button"
                      >
                        <FormattedMessage id="back.action.button" />
                      </Button>
                    </Column>
                  </Grid>
                </Form>
              );
            }}
          </Formik>
        </>
      ) : (
        <>
          <>
            <Grid fullWidth={true}>
              <Column lg={16} md={8} sm={4}>
                <Section>
                  <Section>
                    <Section>
                      <Heading>
                        <FormattedMessage id="process.testAdd.pressNext.selectGroupDictionary" />
                      </Heading>
                    </Section>
                  </Section>
                </Section>
              </Column>
            </Grid>
            <br />
            <Grid fullWidth={true}>
              <Column lg={16} md={8} sm={4}>
                <Button
                  onClick={() => setCurrentStep(currentStep + 1)}
                  type="button"
                >
                  <FormattedMessage id="next.action.button" />
                </Button>
                <Button
                  onClick={() => setCurrentStep(currentStep - 1)}
                  kind="tertiary"
                  type="button"
                >
                  <FormattedMessage id="back.action.button" />
                </Button>
              </Column>
            </Grid>
          </>
        </>
      )}
    </>
  );
};

const StepSixSelectRangeAgeRangeAndSignificantDigits = ({
  formData,
  validationSchema,
  handleNextStep,
  handlePreviousStep,
  selectedResultTypeList,
  ageRangeList,
  setAgeRangeList,
  gotSelectedAgeRangeList,
  setGotSelectedAgeRangeList,
  jsonWad,
  setJsonWad,
  currentStep,
  setCurrentStep,
  ageRangeFields,
  setAgeRangeFields,
}) => {
  const handleSubmit = (values) => {
    handleNextStep(values, true);
  };

  const [ageRanges, setAgeRanges] = useState([{ raw: 0, unit: "Y" }]);

  return (
    <>
      {currentStep === 5 && selectedResultTypeList?.id === "4" ? (
        <>
          <Formik
            initialValues={formData}
            validationSchema={Yup.object().shape({
              resultLimits: Yup.array().of(
                Yup.object().shape({
                  ageRange: Yup.string().required("Age range is required"),
                  highAgeRange: Yup.string().required(
                    "High age range is required",
                  ),
                  // gender: Yup.boolean().when("lowNormalFemale", {
                  //   is: (val) => val !== undefined,
                  //   then: (schema) => schema.required("Required"),
                  //   otherwise: (schema) => schema.notRequired(),
                  // }),
                  gender: Yup.boolean().oneOf(
                    [true, false],
                    "Gender is required",
                  ),
                  lowNormal: Yup.mixed()
                    .test(
                      "is-valid-number-or-infinity",
                      "Low Normal must be a number or '-Infinity'",
                      (value) => {
                        return (
                          value === "-Infinity" ||
                          value === "Infinity" ||
                          typeof value === "number" ||
                          !isNaN(Number(value))
                        );
                      },
                    )
                    .required("Low Normal is required"),
                  highNormal: Yup.mixed()
                    .test(
                      "is-valid-number-or-infinity",
                      "High Normal must be a equal, higher number then Low Normal or 'Infinity'",
                      function (value) {
                        const { lowNormal } = this.parent;

                        const isValidType =
                          typeof value === "number" ||
                          value === "Infinity" ||
                          value === "-Infinity";

                        if (!isValidType) return false;

                        if (
                          typeof lowNormal === "number" &&
                          typeof value === "number"
                        ) {
                          return value >= lowNormal;
                        }

                        if (value === "Infinity") return true;

                        if (
                          typeof value === "number" &&
                          lowNormal !== undefined &&
                          !isNaN(Number(lowNormal))
                        ) {
                          return value >= Number(lowNormal);
                        }

                        return typeof value === "number";
                      },
                    )
                    .required("High Normal is required"),
                  lowNormalFemale: Yup.mixed().when("gender", {
                    is: true,
                    then: Yup.mixed()
                      .test(
                        "is-valid-number-or-infinity",
                        "Low Normal Female must be a number or '-Infinity'",
                        (value) => {
                          return (
                            value === "-Infinity" ||
                            value === "Infinity" ||
                            typeof value === "number" ||
                            !isNaN(Number(value))
                          );
                        },
                      )
                      .required("Low Normal Female is required"),
                    otherwise: Yup.mixed().notRequired(),
                  }),
                  highNormalFemale: Yup.mixed().when("gender", {
                    is: true,
                    then: Yup.mixed()
                      .test(
                        "is-valid-number-or-infinity",
                        "High Normal Female must be equal, number or 'Infinity'",
                        (value) =>
                          value === "-Infinity" ||
                          value === "Infinity" ||
                          typeof value === "number" ||
                          !isNaN(Number(value)),
                      )
                      .test(
                        "greater-than-lowNormalFemale",
                        "High Normal Female must be equal, greater than Low Normal Female or Infinity",
                        function (value) {
                          const { lowNormalFemale } = this.parent;

                          const numValue = parseFloat(value);
                          const numLow = parseFloat(lowNormalFemale);

                          const isValidNumber = (v) =>
                            v === "Infinity" ||
                            v === "-Infinity" ||
                            typeof v === "number" ||
                            !isNaN(Number(v));

                          if (
                            !isValidNumber(value) ||
                            !isValidNumber(lowNormalFemale)
                          ) {
                            return true;
                          }

                          return (
                            (numValue === Infinity || numValue >= numLow) &&
                            numValue !== -Infinity
                          );
                        },
                      )
                      .required("High Normal Female is required"),
                    otherwise: Yup.mixed().notRequired(),
                  }),
                  // lowCritical: Yup.number()
                  //   .min(0)
                  //   .max(100)
                  //   .required("Required")
                  //   .test(
                  //     "lowCritical-range",
                  //     "Low critical must be between lowValid and lowNormal",
                  //     function (value) {
                  //       const { lowValid, lowNormal } = this.parent;
                  //       if (
                  //         value == null ||
                  //         lowValid == null ||
                  //         lowNormal == null
                  //       )
                  //         return true;
                  //       return value >= lowValid && value <= lowNormal;
                  //     },
                  //   ),
                  // highCritical: Yup.number()
                  //   .min(0)
                  //   .max(100)
                  //   .required("Required")
                  //   .test(
                  //     "highCritical-range",
                  //     "High critical must be between highNormal and highValid",
                  //     function (value) {
                  //       const { highValid, highNormal } = this.parent;
                  //       if (
                  //         value == null ||
                  //         highValid == null ||
                  //         highNormal == null
                  //       )
                  //         return true;
                  //       return value >= highNormal && value <= highValid;
                  //     },
                  //   ),
                }),
              ),
              lowReportingRange: Yup.mixed().test(
                "is-valid-number-or-infinity",
                "Low Reporting Range must be a number or 'Infinity'",
                (value) =>
                  value === "-Infinity" ||
                  value === "Infinity" ||
                  typeof value === "number" ||
                  !isNaN(Number(value)),
              ),
              // .min(0, "Minimum value is 0")
              // .max(100, "Maximum value is 100")
              // .required("Required"),

              highReportingRange: Yup.mixed()
                .test(
                  "is-valid-number-or-infinity",
                  "High Reporting Range must be a number or 'Infinity'",
                  (value) =>
                    value === "-Infinity" ||
                    value === "Infinity" ||
                    typeof value === "number" ||
                    !isNaN(Number(value)),
                )
                .test(
                  "greater-than-lowReportingRange",
                  "Must be greater or equal to lower reporting range",
                  function (value) {
                    const { lowReportingRange } = this.parent;
                    const high = parseFloat(value);
                    const low = parseFloat(lowReportingRange);

                    if (isNaN(high) || isNaN(low)) return true;
                    return high >= low;
                  },
                ),
              // .max(100, "Maximum value is 100")
              // .required("Required"),

              lowValid: Yup.mixed().test(
                "is-valid-number-or-infinity",
                "Low Valid must be a number or 'Infinity'",
                (value) =>
                  value === "-Infinity" ||
                  value === "Infinity" ||
                  typeof value === "number" ||
                  !isNaN(Number(value)),
              ),
              // .min(0, "Minimum value is 0")
              // .max(100, "Maximum value is 100")
              // .required("Required"),

              highValid: Yup.mixed()
                .test(
                  "is-valid-number-or-infinity",
                  "High Valid must be a number or 'Infinity'",
                  (value) =>
                    value === "-Infinity" ||
                    value === "Infinity" ||
                    typeof value === "number" ||
                    !isNaN(Number(value)),
                )
                .test(
                  "greater-than-lowValid",
                  "Must be greater or equal to lower valid range",
                  function (value) {
                    const { lowValid } = this.parent;
                    const high = parseFloat(value);
                    const low = parseFloat(lowValid);

                    if (isNaN(high) || isNaN(low)) return true;
                    return high >= low;
                  },
                ),
              // .max(100, "Maximum value is 100")
              // .required("Required"),

              lowCritical: Yup.mixed()
                .test(
                  "is-valid-number-or-infinity",
                  "Low Critical must be a number or 'Infinity'",
                  (value) =>
                    value === "-Infinity" ||
                    value === "Infinity" ||
                    typeof value === "number" ||
                    !isNaN(Number(value)),
                )
                .test(
                  "lowCritical-between-valid-and-normal",
                  "Low critical must be between lowValid and lowNormal",
                  function (value) {
                    const { lowValid, resultLimits } = this.parent;
                    const lowCritical = parseFloat(value);
                    const valid = parseFloat(lowValid);
                    const lowNormal =
                      resultLimits && resultLimits.length > 0
                        ? parseFloat(resultLimits[0].lowNormal)
                        : NaN;

                    if ([lowCritical, valid, lowNormal].some(isNaN))
                      return true;
                    return lowCritical >= valid && lowCritical <= lowNormal;
                  },
                ),

              highCritical: Yup.mixed()
                .test(
                  "is-valid-number-or-infinity",
                  "High Critical must be a number or 'Infinity'",
                  (value) =>
                    value === "-Infinity" ||
                    value === "Infinity" ||
                    typeof value === "number" ||
                    !isNaN(Number(value)),
                )
                .test(
                  "highCritical-between-normal-and-valid",
                  "High critical must be between highNormal and highValid",
                  function (value) {
                    const { highValid, resultLimits } = this.parent;
                    const highCritical = parseFloat(value);
                    const valid = parseFloat(highValid);
                    const highNormal =
                      resultLimits && resultLimits.length > 0
                        ? parseFloat(resultLimits[0].highNormal)
                        : NaN;

                    if ([highCritical, valid, highNormal].some(isNaN))
                      return true;
                    return highCritical >= highNormal && highCritical <= valid;
                  },
                ),

              significantDigits: Yup.number().min(0).max(4),
              // .required("Required"),
            })}
            enableReinitialize={true}
            validateOnChange={true}
            validateOnBlur={true}
            onSubmit={(values, actions) => {
              const processedLimits = (values.resultLimits || []).map(
                (limit, index) => {
                  const raw = parseFloat(ageRanges[index]?.raw || "0");
                  const unit = ageRanges[index]?.unit || "Y";
                  const multiplier = unit === "Y" ? 365 : unit === "M" ? 30 : 1;

                  return {
                    ...limit,
                    highAgeRange: String(raw * multiplier),
                  };
                },
              );

              const payload = {
                ...values,
                resultLimits: processedLimits,
              };
              handleSubmit(payload);
              actions.setSubmitting(false);
            }}
          >
            {({
              values,
              handleChange,
              handleBlur,
              touched,
              errors,
              setFieldValue,
            }) => {
              const handleAddAgeRangeFillUp = (index, currentLimits) => {
                setAgeRangeFields((prev) => {
                  if (index === prev.length - 1) {
                    return [...prev, prev.length];
                  }
                  return prev;
                });

                const prevLimit = currentLimits?.[index];
                const nextLowAge = prevLimit?.highAgeRange || "0";

                const newLimit = {
                  ageRange: String(nextLowAge),
                  highAgeRange: "0",
                  gender: false,
                  lowNormal: "-Infinity",
                  highNormal: "Infinity",
                  lowNormalFemale: "-Infinity",
                  highNormalFemale: "Infinity",
                };

                const updatedLimits = [...(currentLimits || []), newLimit];
                setFieldValue("resultLimits", updatedLimits);
                setAgeRanges((prev) => [...prev, { raw: 0, unit: "Y" }]);
              };

              const handleRemoveAgeRangeFillUp = (indexToRemove) => {
                setAgeRangeFields((prev) =>
                  prev.filter((_, i) => i !== indexToRemove),
                );

                const updatedLimits = (values.resultLimits || []).filter(
                  (_, index) => index !== indexToRemove,
                );
                setFieldValue("resultLimits", updatedLimits);

                setAgeRanges((prev) =>
                  prev.filter((_, i) => i !== indexToRemove),
                );
              };

              const handleRangeChange = (index, field, value) => {
                setFieldValue(`resultLimits[${index}].${field}`, value);
              };

              return (
                <Form>
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
                    </Column>
                    {ageRangeFields.map((_, index) => {
                      const selectedAgeRanges = (values.resultLimits || [])
                        .map((item) => item.ageRange)
                        .filter((val, idx) => idx !== index && val);
                      const availableAgeRanges = ageRangeList.filter(
                        (age) => !selectedAgeRanges.includes(age.id),
                      );
                      return (
                        <React.Fragment key={index}>
                          <Column
                            key={index}
                            lg={4}
                            md={4}
                            sm={4}
                            style={{ marginTop: "1rem" }}
                          >
                            <Checkbox
                              id={`gender-${index}`}
                              name={`resultLimits[${index}].gender`}
                              labelText={
                                <FormattedMessage id="label.sex.dependent" />
                              }
                              checked={
                                values.resultLimits?.[index]?.gender || false
                              }
                              onChange={(e) => {
                                if (!values.resultLimits?.[index]) {
                                  const updatedLimits = [
                                    ...(values.resultLimits || []),
                                  ];
                                  updatedLimits[index] = { gender: false };
                                  setFieldValue("resultLimits", updatedLimits);
                                }
                                handleRangeChange(
                                  index,
                                  "gender",
                                  e.target.checked,
                                );
                              }}
                            />
                          </Column>
                          <Column
                            key={index}
                            lg={4}
                            md={4}
                            sm={4}
                            style={{ marginTop: "1rem" }}
                          >
                            <RadioButtonGroup
                              id={`fieldAgeRangeRadioGroup-${index}`}
                              name={`fieldAgeRangeRadioGroup-${index}`}
                              value={ageRanges[index]?.unit || "Y"}
                              onChange={(val) => {
                                setAgeRanges((prev) => {
                                  const updated = [...prev];
                                  updated[index] = {
                                    ...updated[index],
                                    unit: val,
                                  };
                                  return updated;
                                });
                              }}
                            >
                              <RadioButton
                                labelText={"Y"}
                                value={"Y"}
                                id={`Y-${index}`}
                              />
                              <RadioButton
                                labelText={"M"}
                                value={"M"}
                                id={`M-${index}`}
                              />
                              <RadioButton
                                labelText={"D"}
                                value={"D"}
                                id={`D-${index}`}
                              />
                            </RadioButtonGroup>
                          </Column>
                          <Column
                            key={index}
                            lg={4}
                            md={4}
                            sm={4}
                            style={{ marginTop: "1rem" }}
                          >
                            <TextInput
                              id={`resultLimits[${index}].ageRange`}
                              name={`resultLimits[${index}].ageRange`}
                              onBlur={handleBlur}
                              label="Age Range (Low)"
                              size={"md"}
                              // min={0}
                              // max={1000}
                              // step={1}
                              value={values.resultLimits?.[index]?.ageRange}
                              disabled
                              invalid={
                                touched?.resultLimits?.[index]?.ageRange &&
                                !!errors?.resultLimits?.[index]?.ageRange
                              }
                              invalidText={
                                touched?.resultLimits?.[index]?.ageRange &&
                                errors?.resultLimits?.[index]?.ageRange
                              }
                              // onChange={(_, { value }) =>
                              //   handleRangeChange(index, "ageRange", value)
                              // }
                            />
                          </Column>
                          <Column
                            key={index}
                            lg={4}
                            md={4}
                            sm={4}
                            style={{ marginTop: "1rem" }}
                          >
                            <TextInput
                              id={`resultLimits[${index}].highAgeRange`}
                              name={`resultLimits[${index}].highAgeRange`}
                              onBlur={handleBlur}
                              label="Age Range (High)"
                              size={"md"}
                              // min={0}
                              // max={1000}
                              // step={1}
                              // value={values.resultLimits?.[index]?.highAgeRange}
                              value={String(ageRanges[index]?.raw) || "0"}
                              invalid={
                                touched?.resultLimits?.[index]?.highAgeRange &&
                                !!errors?.resultLimits?.[index]?.highAgeRange
                              }
                              invalidText={
                                touched?.resultLimits?.[index]?.highAgeRange &&
                                errors?.resultLimits?.[index]?.highAgeRange
                              }
                              onChange={(e) => {
                                const val = e.target.value;
                                setAgeRanges((prev) => {
                                  const updated = [...prev];
                                  updated[index] = {
                                    ...updated[index],
                                    raw: val,
                                  };
                                  return updated;
                                });
                              }}
                            />
                          </Column>
                          <Column
                            key={index}
                            lg={4}
                            md={4}
                            sm={4}
                            style={{ marginTop: "1rem" }}
                          >
                            {/* ageRange removal from the end data */}
                            <Select
                              onBlur={handleBlur}
                              id={`resultLimits[${index}].ageRange`}
                              name={`resultLimits[${index}].ageRange`}
                              labelText=""
                              hideLabel
                              size={"md"}
                              value={
                                values.resultLimits?.[index]?.ageRange || ""
                              }
                              onChange={(e) => {
                                setFieldValue(
                                  `resultLimits[${index}].ageRange`,
                                  e.target.value,
                                );
                              }}
                              invalid={
                                touched?.resultLimits?.[index]?.ageRange &&
                                !!errors?.resultLimits?.[index]?.ageRange
                              }
                              invalidText={
                                touched?.resultLimits?.[index]?.ageRange &&
                                errors?.resultLimits?.[index]?.ageRange
                              }
                            >
                              <SelectItem
                                value={"0"}
                                text={`Select Age Range`}
                              />
                              {availableAgeRanges.map((age) => (
                                <SelectItem
                                  key={age.id}
                                  value={age.id}
                                  text={`${age.value}`}
                                />
                              ))}
                            </Select>
                          </Column>
                          <Column
                            key={index}
                            lg={8}
                            md={4}
                            sm={4}
                            style={{ marginTop: "1rem" }}
                          >
                            <FormattedMessage id="field.normalRange" />{" "}
                            {values.resultLimits?.[index]?.gender ? (
                              <>
                                <FormattedMessage id="patient.male" />
                              </>
                            ) : (
                              <></>
                            )}
                            <hr />
                            <div style={{ display: "flex", gap: "4px" }}>
                              <TextInput
                                id={`resultLimits[${index}].lowNormal`}
                                name={`resultLimits[${index}].lowNormal`}
                                onBlur={handleBlur}
                                label="Lower Range"
                                size={"md"}
                                // min={0}
                                // max={1000}
                                // step={1}
                                value={values.resultLimits?.[index]?.lowNormal}
                                invalid={
                                  touched?.resultLimits?.[index]?.lowNormal &&
                                  !!errors?.resultLimits?.[index]?.lowNormal
                                }
                                invalidText={
                                  touched?.resultLimits?.[index]?.lowNormal &&
                                  errors?.resultLimits?.[index]?.lowNormal
                                }
                                onChange={(e) =>
                                  handleRangeChange(
                                    index,
                                    "lowNormal",
                                    e.target.value,
                                  )
                                }
                              />
                              <TextInput
                                id={`resultLimits[${index}].highNormal`}
                                name={`resultLimits[${index}].highNormal`}
                                onBlur={handleBlur}
                                label="Higher Range"
                                size={"md"}
                                // min={0}
                                // max={1000}
                                // step={1}
                                value={values.resultLimits?.[index]?.highNormal}
                                invalid={
                                  touched?.resultLimits?.[index]?.highNormal &&
                                  !!errors?.resultLimits?.[index]?.highNormal
                                }
                                invalidText={
                                  touched?.resultLimits?.[index]?.highNormal &&
                                  errors?.resultLimits?.[index]?.highNormal
                                }
                                onChange={(e) =>
                                  handleRangeChange(
                                    index,
                                    "highNormal",
                                    e.target.value,
                                  )
                                }
                              />
                            </div>
                          </Column>
                          {values.resultLimits?.[index]?.gender ? (
                            <>
                              <Column
                                key={index}
                                lg={8}
                                md={4}
                                sm={4}
                                style={{ marginTop: "1rem" }}
                              >
                                <FormattedMessage id="field.normalRange" />{" "}
                                <FormattedMessage id="patient.female" />
                                <hr />
                                <div style={{ display: "flex", gap: "4px" }}>
                                  <TextInput
                                    id={`resultLimits[${index}].lowNormalFemale`}
                                    name={`resultLimits[${index}].lowNormalFemale`}
                                    onBlur={handleBlur}
                                    label="Lower Range"
                                    size={"md"}
                                    // min={0}
                                    // max={1000}
                                    // step={1}
                                    value={
                                      values.resultLimits?.[index]
                                        ?.lowNormalFemale
                                    }
                                    invalid={
                                      touched?.resultLimits?.[index]
                                        ?.lowNormalFemale &&
                                      !!errors?.resultLimits?.[index]
                                        ?.lowNormalFemale
                                    }
                                    invalidText={
                                      touched?.resultLimits?.[index]
                                        ?.lowNormalFemale &&
                                      errors?.resultLimits?.[index]
                                        ?.lowNormalFemale
                                    }
                                    onChange={(e) =>
                                      handleRangeChange(
                                        index,
                                        "lowNormalFemale",
                                        e.target.value,
                                      )
                                    }
                                  />
                                  <TextInput
                                    id={`resultLimits[${index}].highNormalFemale`}
                                    name={`resultLimits[${index}].highNormalFemale`}
                                    onBlur={handleBlur}
                                    label="Higher Range"
                                    size={"md"}
                                    // min={0}
                                    // max={1000}
                                    // step={1}
                                    value={
                                      values.resultLimits?.[index]
                                        ?.highNormalFemale
                                    }
                                    invalid={
                                      touched?.resultLimits?.[index]
                                        ?.highNormalFemale &&
                                      !!errors?.resultLimits?.[index]
                                        ?.highNormalFemale
                                    }
                                    invalidText={
                                      touched?.resultLimits?.[index]
                                        ?.highNormalFemale &&
                                      errors?.resultLimits?.[index]
                                        ?.highNormalFemale
                                    }
                                    onChange={(e) =>
                                      handleRangeChange(
                                        index,
                                        "highNormalFemale",
                                        e.target.value,
                                      )
                                    }
                                  />
                                </div>
                              </Column>
                            </>
                          ) : (
                            <></>
                          )}
                          {ageRangeFields.length > 1 ? (
                            <Column
                              key={index}
                              lg={16}
                              md={8}
                              sm={4}
                              style={{ marginTop: "1rem" }}
                            >
                              <div
                                key={`remove-age-range-fill-up-${index}`}
                                style={{
                                  display: "flex",
                                  justifyContent: "flex-end",
                                }}
                              >
                                <Button
                                  id={`remove-age-range-fill-up-${index}`}
                                  name={`remove-age-range-fill-up-${index}`}
                                  kind="danger"
                                  type="button"
                                  onClick={() =>
                                    handleRemoveAgeRangeFillUp(index)
                                  }
                                >
                                  Remove
                                </Button>
                              </div>
                            </Column>
                          ) : (
                            <></>
                          )}
                          <Column
                            key={index}
                            lg={16}
                            md={8}
                            sm={4}
                            style={{ marginTop: "1rem" }}
                          >
                            <Button
                              id={`add-age-range-fill-up-${index}`}
                              name={`add-age-range-fill-up-${index}`}
                              kind="secondary"
                              type="button"
                              onClick={() =>
                                handleAddAgeRangeFillUp(
                                  index,
                                  values.resultLimits,
                                )
                              }
                            >
                              <FormattedMessage id="Add Another Age Range +" />
                            </Button>
                          </Column>
                        </React.Fragment>
                      );
                    })}
                  </Grid>
                  <br />
                  <hr />
                  <Grid fullWidth={true}>
                    <Column lg={8} md={4} sm={4} style={{ marginTop: "1rem" }}>
                      <FormattedMessage id="label.reporting.range" />
                      <hr />
                      <div style={{ display: "flex", gap: "4px" }}>
                        <TextInput
                          id={`lowReportingRange`}
                          name={`lowReportingRange`}
                          onBlur={handleBlur}
                          label="Lower Range"
                          size={"md"}
                          // min={0}
                          // max={1000}
                          // step={1}
                          value={values.lowReportingRange}
                          invalid={
                            touched?.lowReportingRange &&
                            !!errors?.lowReportingRange
                          }
                          invalidText={
                            touched?.lowReportingRange &&
                            errors?.lowReportingRange
                          }
                          onChange={(e) =>
                            setFieldValue("lowReportingRange", e.target.value)
                          }
                        />
                        <TextInput
                          id={`highReportingRange`}
                          name={`highReportingRange`}
                          onBlur={handleBlur}
                          label="Higher Range"
                          size={"md"}
                          // min={0}
                          // max={1000}
                          // step={1}
                          value={values.highReportingRange}
                          invalid={
                            touched?.highReportingRange &&
                            !!errors?.highReportingRange
                          }
                          invalidText={
                            touched?.highReportingRange &&
                            errors?.highReportingRange
                          }
                          onChange={(e) =>
                            setFieldValue("highReportingRange", e.target.value)
                          }
                        />
                      </div>
                    </Column>
                    <Column lg={8} md={4} sm={4} style={{ marginTop: "1rem" }}>
                      <FormattedMessage id="field.validRange" />
                      <hr />
                      <div style={{ display: "flex", gap: "4px" }}>
                        <TextInput
                          id={`lowValid`}
                          name={`lowValid`}
                          onBlur={handleBlur}
                          label="Lower Range"
                          size={"md"}
                          // min={0}
                          // max={1000}
                          // step={1}
                          value={values.lowValid}
                          invalid={touched?.lowValid && !!errors?.lowValid}
                          invalidText={touched?.lowValid && errors?.lowValid}
                          onChange={(e) =>
                            setFieldValue("lowValid", e.target.value)
                          }
                        />
                        <TextInput
                          id={`highValid`}
                          name={`highValid`}
                          onBlur={handleBlur}
                          label="Higher Range"
                          size={"md"}
                          // min={0}
                          // max={1000}
                          // step={1}
                          value={values.highValid}
                          invalid={touched?.highValid && !!errors?.highValid}
                          invalidText={touched?.highValid && errors?.highValid}
                          onChange={(e) =>
                            setFieldValue("highValid", e.target.value)
                          }
                        />
                      </div>
                    </Column>
                    <Column lg={8} md={4} sm={4} style={{ marginTop: "1rem" }}>
                      <FormattedMessage id="label.critical.range" />
                      <hr />
                      <div style={{ display: "flex", gap: "4px" }}>
                        <TextInput
                          id={`lowCritical`}
                          name={`lowCritical`}
                          onBlur={handleBlur}
                          label="Lower Range"
                          size={"md"}
                          // min={0}
                          // max={1000}
                          // step={1}
                          value={values.lowCritical}
                          invalid={
                            touched?.lowCritical && !!errors?.lowCritical
                          }
                          invalidText={
                            touched?.lowCritical && errors?.lowCritical
                          }
                          onChange={(e) =>
                            setFieldValue("lowCritical", e.target.value)
                          }
                        />
                        <TextInput
                          id={`highCritical`}
                          name={`highCritical`}
                          onBlur={handleBlur}
                          label="Higher Range"
                          size={"md"}
                          // min={0}
                          // max={1000}
                          // step={1}
                          value={values.highCritical}
                          invalid={
                            touched?.highCritical && !!errors?.highCritical
                          }
                          invalidText={
                            touched?.highCritical && errors?.highCritical
                          }
                          onChange={(e) =>
                            setFieldValue("highCritical", e.target.value)
                          }
                        />
                      </div>
                    </Column>
                  </Grid>
                  <br />
                  <hr />
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
                          name={"significantDigits"}
                          max={4}
                          min={0}
                          size={"md"}
                          step={1}
                          invalid={
                            touched.significantDigits &&
                            !!errors.significantDigits
                          }
                          invalidText={
                            touched.significantDigits &&
                            errors.significantDigits
                          }
                          onBlur={handleBlur}
                          onChange={(_, { value }) =>
                            setFieldValue("significantDigits", value)
                          }
                          value={values.significantDigits || 0}
                        />
                      </Column>
                    </Row>
                  </FlexGrid>
                  <br />
                  <Grid fullWidth={true}>
                    <Column lg={16} md={8} sm={4}>
                      <Button type="submit">
                        <FormattedMessage id="next.action.button" />
                      </Button>{" "}
                      <Button
                        onClick={() => handlePreviousStep(values)}
                        kind="tertiary"
                        type="button"
                      >
                        <FormattedMessage id="back.action.button" />
                      </Button>
                    </Column>
                  </Grid>
                </Form>
              );
            }}
          </Formik>
        </>
      ) : (
        <>
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Section>
                  <Section>
                    <Heading>
                      <FormattedMessage id="process.testAdd.pressNext.ageRangeSelectiong" />
                    </Heading>
                  </Section>
                </Section>
              </Section>
            </Column>
          </Grid>
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Button
                onClick={() => setCurrentStep(currentStep + 1)}
                type="button"
              >
                <FormattedMessage id="next.action.button" />
              </Button>
              <Button
                onClick={() => setCurrentStep(currentStep - 1)}
                kind="tertiary"
                type="button"
              >
                <FormattedMessage id="back.action.button" />
              </Button>
            </Column>
          </Grid>
        </>
      )}
    </>
  );
};

const StepSevenFinalDisplayAndSaveConfirmation = ({
  formData,
  validationSchema,
  handlePreviousStep,
  handleNextStep,
  jsonWad,
  setJsonWad,
  panelListTag,
  selectedLabUnitList,
  selectedUomList,
  selectedResultTypeList,
  selectedSampleTypeList,
  selectedSampleTypeResp,
  currentStep,
  setCurrentStep,
}) => {
  const handleSubmit = (values) => {
    handleNextStep(values, false);
  };
  return (
    <>
      {currentStep === 7 - 1 ? (
        <>
          <Formik
            initialValues={formData}
            // validationSchema={validationSchema}
            enableReinitialize={true}
            validateOnChange={true}
            validateOnBlur={true}
            onSubmit={(values, actions) => {
              handleSubmit(values);
              actions.setSubmitting(false);
            }}
          >
            {({
              values,
              handleChange,
              handleBlur,
              touched,
              errors,
              setFieldValue,
            }) => {
              return (
                <Form>
                  <Grid fullWidth={true}>
                    <Column lg={6} md={8} sm={4}>
                      <Section>
                        <Section>
                          <Section>
                            <Heading>
                              <FormattedMessage id="sample.entry.project.testName" />
                            </Heading>
                          </Section>
                        </Section>
                      </Section>
                      <br />
                      <FormattedMessage id="english.label" />
                      {" : "}
                      {values?.testNameEnglish}
                      <br />
                      <FormattedMessage id="french.label" />
                      {" : "}
                      {values?.testNameFrench}
                      <br />
                      <br />
                      <Section>
                        <Section>
                          <Section>
                            <Heading>
                              <FormattedMessage id="reporting.label.testName" />
                            </Heading>
                          </Section>
                        </Section>
                      </Section>
                      <br />
                      <FormattedMessage id="english.label" />
                      {" : "}
                      {values?.testReportNameEnglish}
                      <br />
                      <FormattedMessage id="french.label" />
                      {" : "}
                      {values?.testReportNameFrench}
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
                      {values?.loinc}
                      <br />
                      <br />
                      <FormattedMessage id="field.resultType" />
                      {" : "}
                      {selectedResultTypeList.value}
                      <br />
                      <br />
                      <FormattedMessage id="test.antimicrobialResistance" />
                      {" : "}
                      {values?.antimicrobialResistance}
                      <br />
                      <br />
                      <FormattedMessage id="dictionary.category.isActive" />
                      {" : "}
                      {values?.active}
                      <br />
                      <br />
                      <FormattedMessage id="label.orderable" />
                      {" : "}
                      {values?.orderable}
                      <br />
                      <br />
                      <FormattedMessage id="test.notifyResults" />
                      {" : "}
                      {values?.notifyResults}
                      <br />
                      <br />
                      <FormattedMessage id="test.inLabOnly" />
                      {" : "}
                      {values?.inLabOnly}
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
                      {/* {values.sampleTypes.length > 0 ? (
                        <UnorderedList nested={true}>
                          {values.sampleTypes.map((type, index) => (
                            <div key={`sampleType_${index}`}>
                              <ListItem>
                                {selectedSampleTypeList.find(
                                  (item) => item.id === type.id,
                                )?.value ?? `Sample Type ${type.id}`}
                              </ListItem>
                              <br />
                              {type.tests?.length > 0 && (
                                <div className="gridBoundary">
                                  <Section>
                                    <UnorderedList nested>
                                      {type.tests.map((test) => (
                                        <ListItem key={`test_${test.id}`}>
                                          {test.name}
                                        </ListItem>
                                      ))}
                                    </UnorderedList>
                                  </Section>
                                </div>
                              )}
                            </div>
                          ))}
                        </UnorderedList>
                      ) : (
                        <></>
                      )} */}
                      <br />
                      <FormattedMessage id="field.referenceValue" />
                      {" : "}
                      {values?.dictionaryReference}
                      <br />
                      <FormattedMessage id="label.default.result" />
                      {" : "}
                      {values?.defaultTestResult}
                    </Column>
                  </Grid>
                  <br />
                  <Grid fullWidth={true}>
                    <Column lg={16} md={8} sm={4}>
                      <Button type="submit">
                        <FormattedMessage id="accept.action.button" />
                      </Button>{" "}
                      <Button
                        onClick={() => handlePreviousStep(values)}
                        kind="tertiary"
                        type="button"
                      >
                        <FormattedMessage id="back.action.button" />
                      </Button>
                    </Column>
                  </Grid>
                </Form>
              );
            }}
          </Formik>
        </>
      ) : (
        <></>
      )}
    </>
  );
};

// compitative selection
// validation schema flow fix for each step
// func moving to form ground
// fucntion buildup for 5-6-7 step
// better formating at the end of the step 7
