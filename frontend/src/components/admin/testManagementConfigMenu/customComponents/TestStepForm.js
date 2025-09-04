import React, { useState, useEffect, useRef, useContext } from "react";
import {
  Heading,
  Button,
  Grid,
  Column,
  Section,
  Select,
  SelectItem,
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
  ClickableTile,
  Loading,
} from "@carbon/react";
import { NotificationKinds } from "../../../common/CustomNotification.js";
import { FormattedMessage, useIntl } from "react-intl";
import { Formik, Form } from "formik";
import * as Yup from "yup";
import { CustomCommonSortableOrderList } from "./../sortableListComponent/SortableList.js";
import { getFromOpenElisServer } from "../../../utils/Utils.js";
import { NotificationContext } from "../../../layout/Layout.js";
import { extractAgeRangeParts } from "./TestFormData.js";

export const TestStepForm = ({ initialData, mode = "add", postCall }) => {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();
  const componentMounted = useRef(false);
  const [formData, setFormData] = useState(initialData);
  const [isLoading, setIsLoading] = useState(false);
  const [ageRangeList, setAgeRangeList] = useState([]);
  const [gotSelectedAgeRangeList, setGotSelectedAgeRangeList] = useState([]);
  const [labUnitList, setLabUnitList] = useState([]);
  const [selectedLabUnitList, setSelectedLabUnitList] = useState({});
  const [panelList, setPanelList] = useState([]);
  const [panelListTag, setPanelListTag] = useState([]);
  const [uomList, setUomList] = useState([]);
  const [selectedUomList, setSelectedUomList] = useState({});
  const [resultTypeList, setResultTypeList] = useState([]);
  const [codedResultList, setCodedResultList] = useState([]);
  const [freeResultList, setFreeResultList] = useState([]);
  const [numericResultId, setNumericResultId] = useState("");
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
  const [currentStep, setCurrentStep] = useState(0);
  const [ageRangeFields, setAgeRangeFields] = useState([0]);
  const [ageRanges, setAgeRanges] = useState([{ raw: "Infinity", unit: "Y" }]);

  useEffect(() => {
    if (resultTypeList.length > 0) {
      const codedList = resultTypeList
        .filter(
          (item) =>
            item.value === "Multiselect" ||
            item.value === "Cascading multiselect" ||
            item.value === "Select List",
        )
        .map((item) => item.id);
      setCodedResultList(codedList);

      const freeTextList = resultTypeList
        .filter(
          (item) => item.value === "Free text" || item.value === "Alphanumeric",
        )
        .map((item) => item.id);
      setFreeResultList(freeTextList);

      const numericId = resultTypeList.find(
        (item) => item.value === "Numeric",
      ).id;
      setNumericResultId(numericId);
    }
  }, [resultTypeList]);

  const handleNextStep = (newData, final = false) => {
    setFormData((prev) => ({ ...prev, ...newData }));

    if (!final) {
      postCall(formData);
    }

    const selectedResultTypeId = newData?.resultType || formData.resultType;

    setCurrentStep((prev) => {
      if (prev === 3) {
        if (freeResultList.includes(selectedResultTypeId)) {
          return prev + 3;
        }

        if (numericResultId === selectedResultTypeId) {
          return prev + 2;
        }

        if (codedResultList.includes(selectedResultTypeId)) {
          return prev + 1;
        }
      }

      if (prev === 4 && codedResultList.includes(selectedResultTypeId)) {
        return prev + 2;
      }

      if (prev === 5 && selectedResultTypeId === numericResultId) {
        return prev + 1;
      }

      return prev + 1;
    });
  };

  const handlePreviousStep = (newData) => {
    setFormData((prev) => ({ ...prev, ...newData }));
    const selectedResultTypeId = newData?.resultType || formData.resultType;

    setCurrentStep((prevStep) => {
      if (prevStep === 6) {
        if (freeResultList.includes(selectedResultTypeId)) {
          return prevStep - 3;
        }

        if (codedResultList.includes(selectedResultTypeId)) {
          return prevStep - 2;
        }

        if (selectedResultTypeId === numericResultId) {
          return prevStep - 1;
        }
      }

      if (prevStep === 5 && selectedResultTypeId === numericResultId) {
        return prevStep - 2;
      }

      return prevStep - 1;
    });
  };

  useEffect(() => {
    componentMounted.current = true;
    setIsLoading(true);
    getFromOpenElisServer(`/rest/TestAdd`, (res) => {
      setLabUnitList(res.labUnitList || []);
      setPanelList(res.panelList || []);
      setUomList(res.uomList || []);
      setResultTypeList(res.resultTypeList || []);
      setSampleTypeList(res.sampleTypeList || []);
      setGroupedDictionaryList(res.groupedDictionaryList || []);
      setDictionaryList(res.dictionaryList || []);
      setAgeRangeList(res.ageRangeList || []);
      setIsLoading(false);
    });
    return () => {
      componentMounted.current = false;
      setIsLoading(false);
    };
  }, []);

  const normalizeResultLimits = (limits = []) => {
    const keyMap = {};
    const extractedAgeRanges = [];

    limits.forEach((limit) => {
      const key = `${limit.ageRange}`;

      if (!keyMap[key]) {
        const { low, high } = extractAgeRangeParts(limit.ageRange);

        extractedAgeRanges.push(high);

        keyMap[key] = {
          ageRange: low.raw?.toString(),
          highAgeRange: high.raw?.toString(),
          gender: limit.gender,
          lowNormal: limit.lowNormal,
          highNormal: limit.highNormal,
          lowNormalFemale: limit.lowNormalFemale,
          highNormalFemale: limit.highNormalFemale,
        };
      }

      if (limit.gender || limit.gender === "M" || limit.gender === "F") {
        keyMap[key].gender = true;
        keyMap[key].lowNormal = limit.lowNormal;
        keyMap[key].highNormal = limit.highNormal;
        keyMap[key].lowNormalFemale = limit.lowNormalFemale;
        keyMap[key].highNormalFemale = limit.highNormalFemale;
      }
    });

    return {
      normalizedLimits: Object.values(keyMap),
      extractedAgeRanges,
    };
  };

  useEffect(() => {
    if (mode === "edit") {
      if (
        !initialData ||
        !labUnitList.length ||
        !panelList.length ||
        !uomList.length
      )
        return;

      const selectedLabUnit = labUnitList.find(
        (item) => item.value === initialData.testSection,
      );

      const selectedPanelObjects = panelList.filter((panel) =>
        initialData?.panels?.includes(panel.value),
      );

      const selectedUom = uomList.find(
        (item) => item.value === initialData.uom,
      );

      const selectedSampleTypeFilteredObject = sampleTypeList.filter(
        (sampleType) =>
          initialData.sampleTypes.includes(String(sampleType.value)),
      );

      selectedSampleTypeFilteredObject.forEach((sampleType) => {
        getFromOpenElisServer(
          `/rest/sample-type-tests?sampleType=${sampleType.id}`,
          (res) => {
            if (res) {
              handleSampleType(res);
            } else {
              console.error("No response received for sample type tests");
            }
          },
        );
      });

      const updatedList = [
        // ...selectedSampleTypeList,
        ...selectedSampleTypeFilteredObject,
      ];

      setSelectedSampleTypeList(updatedList);

      setSampleTestTypeToGetTagList(() =>
        // prev
        [
          // ...prev,
          ...selectedSampleTypeFilteredObject,
        ],
      );

      setSelectedSampleType((prev) => [
        ...prev,
        ...selectedSampleTypeFilteredObject,
      ]);

      if (selectedLabUnit) {
        setSelectedLabUnitList(selectedLabUnit);
      }

      setPanelListTag(
        selectedPanelObjects.map((panel) => ({
          id: panel?.id,
          value: panel?.value,
        })),
      );

      if (selectedUom) {
        setSelectedUomList(selectedUom);
      }

      const mappedResultType = resultTypeList.find(
        (type) =>
          (initialData.resultType === "R" && type.value === "Free text") ||
          (initialData.resultType === "D" && type.value === "Select List") ||
          (initialData.resultType === "N" && type.value === "Numeric") ||
          (initialData.resultType === "A" && type.value === "Alphanumeric") ||
          (initialData.resultType === "M" && type.value === "Multiselect") ||
          (initialData.resultType === "C" &&
            type.value === "Cascading multiselect"),
      );

      if (mappedResultType) {
        setSelectedResultTypeList(mappedResultType);
      }

      if (initialData.dictionary && Array.isArray(initialData.dictionary)) {
        const matchedDictFlat = initialData.dictionary
          .map((val) => {
            const isString = typeof val === "string";
            const valueRaw = isString ? val : (val?.value ?? "");

            const firstToken = valueRaw.trim().split(" ")[0];
            const qualified = valueRaw.toLowerCase().includes("qualifiable")
              ? "Y"
              : "N";

            const matched = dictionaryList.find((dictItem) => {
              return dictItem.value.trim() === firstToken;
            });

            return matched
              ? {
                  id: matched.id,
                  value: matched.value,
                  qualified,
                }
              : null;
          })
          .filter(Boolean);

        setSingleSelectDictionaryList(matchedDictFlat);
        setMultiSelectDictionaryList(matchedDictFlat);
        setDictionaryListTag(matchedDictFlat);
        setMultiSelectDictionaryListTag(
          matchedDictFlat.filter((d) => d.qualified === "Y"),
        );

        setFormData((prev) => ({
          ...prev,
          dictionary: matchedDictFlat.map(({ id, qualified }) => ({
            id,
            qualified,
          })),
        }));

        const extractFirst = (val) => val?.trim()?.split(" ")[0];

        const refValue = extractFirst(initialData?.dictionaryReference);
        const defaultVal = extractFirst(initialData?.defaultTestResult);

        const refMatch = dictionaryList.find((item) => {
          return item.value.trim() === refValue;
        });

        const defaultMatch = dictionaryList.find((item) => {
          return item.value.trim() === defaultVal;
        });

        if (refMatch) {
          setFormData((prev) => ({
            ...prev,
            dictionaryReference: refMatch.id,
          }));
        }

        if (defaultMatch) {
          setFormData((prev) => ({
            ...prev,
            defaultTestResult: defaultMatch.id,
          }));
        }
      }

      const { extractedAgeRanges, normalizedLimits } = normalizeResultLimits(
        initialData.resultLimits || [],
      );
      setAgeRanges(extractedAgeRanges);
      setAgeRangeFields(normalizedLimits.map((_, i) => i));

      setFormData((prev) => ({
        ...prev,
        testSection: selectedLabUnit?.id || "",
        panels: selectedPanelObjects.map((panel) => ({ id: panel?.id })),
        uom: selectedUom?.id || "",
        sampleTypes: [],
        resultType: mappedResultType?.id || "",
        resultLimits: normalizedLimits,
      }));
    }
  }, [
    initialData,
    mode,
    labUnitList,
    panelList,
    uomList,
    sampleTypeList,
    resultTypeList,
    dictionaryList,
  ]);

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
      id: formData.testId || "0",
      name: formData.testNameEnglish || formData.testNameFrench,
      userBenchChoice: false,
    };

    const alreadyContainsDefault = res.tests?.some(
      (t) =>
        (t.name || "").trim().toLowerCase() ===
          (formData.testNameEnglish || "").trim().toLowerCase() ||
        (t.name || "").trim().toLowerCase() ===
          (formData.testNameFrench || "").trim().toLowerCase(),
    );

    const hasTestIdMatch = res.tests?.some(
      (t) => String(t.id) === String(initialData.testId),
    );

    const updatedTests = (() => {
      if (hasTestIdMatch) {
        return res.tests.map((t) =>
          String(t.id) === String(initialData.testId)
            ? {
                ...t,
                name: formData.testNameEnglish || formData.testNameFrench,
              }
            : t,
        );
      } else {
        return mode === "edit" && alreadyContainsDefault
          ? [...(res.tests || [])]
          : [...(res.tests || []), extraTestItem];
      }
    })();

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
            tests: updatedTests,
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

  useEffect(() => {
    if (!initialData?.testId) return;

    setSelectedSampleTypeResp((prevList) => {
      return prevList.map((item) => ({
        ...item,
        tests: (item.tests || []).map((t) =>
          String(t.id) === String(initialData.testId)
            ? {
                ...t,
                name: formData.testNameEnglish || formData.testNameFrench,
              }
            : t,
        ),
      }));
    });
  }, [formData.testNameEnglish, formData.testNameFrench]);

  const steps = [
    <StepOneTestNameAndTestSection
      key="step-1"
      formData={formData}
      setFormData={setFormData}
      handleNextStep={handleNextStep}
      labUnitList={labUnitList}
      setLabUnitList={setLabUnitList}
      selectedLabUnitId={selectedLabUnitList}
      setSelectedLabUnitList={setSelectedLabUnitList}
    />,
    <StepTwoTestPanelAndUom
      key="step-2"
      formData={formData}
      setFormData={setFormData}
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
    />,
    <StepThreeTestResultTypeAndLoinc
      key="step-3"
      formData={formData}
      setFormData={setFormData}
      handleNextStep={handleNextStep}
      handlePreviousStep={handlePreviousStep}
      resultTypeList={resultTypeList}
      setSelectedResultTypeList={setSelectedResultTypeList}
    />,
    <StepFourSelectSampleTypeAndTestDisplayOrder
      key="step-4"
      formData={formData}
      setFormData={setFormData}
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
      currentStep={currentStep}
    />,
    <StepFiveSelectListOptionsAndResultOrder
      key="step-5"
      formData={formData}
      setFormData={setFormData}
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
      currentStep={currentStep}
      setCurrentStep={setCurrentStep}
    />,
    <StepSixSelectRangeAgeRangeAndSignificantDigits
      key="step-6"
      formData={formData}
      setFormData={setFormData}
      handleNextStep={handleNextStep}
      handlePreviousStep={handlePreviousStep}
      selectedResultTypeList={selectedResultTypeList}
      ageRangeList={ageRangeList}
      setAgeRangeList={setAgeRangeList}
      gotSelectedAgeRangeList={gotSelectedAgeRangeList}
      setGotSelectedAgeRangeList={setGotSelectedAgeRangeList}
      currentStep={currentStep}
      setCurrentStep={setCurrentStep}
      ageRangeFields={ageRangeFields}
      setAgeRangeFields={setAgeRangeFields}
      ageRanges={ageRanges}
      setAgeRanges={setAgeRanges}
      mode={mode}
    />,
    <StepSevenFinalDisplayAndSaveConfirmation
      key="step-7"
      formData={formData}
      setFormData={setFormData}
      handleNextStep={handleNextStep}
      handlePreviousStep={handlePreviousStep}
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

  if (isLoading) {
    return (
      <>
        <Loading />
      </>
    );
  }

  return (
    <>
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <br />
          <hr />
          <br />
          <div>{steps[currentStep]}</div>
          <br />
          <hr />
          <br />
        </Column>
      </Grid>
    </>
  );
};

export const StepOneTestNameAndTestSection = ({
  formData,
  setFormData,
  handleNextStep,
  labUnitList,
  setLabUnitList,
  selectedLabUnitId,
  setSelectedLabUnitList,
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
          testNameEnglish: Yup.string().required(
            "English test name is required",
          ),
          testNameFrench: Yup.string().required("French test name is required"),
          testReportNameEnglish: Yup.string().required(
            "English report name is required",
          ),
          testReportNameFrench: Yup.string().required(
            "French report name is required",
          ),
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
          const copyInputValuesFromTestNameEnFr = (values) => {
            setFieldValue("testReportNameEnglish", values.testNameEnglish);
            setFieldValue("testReportNameFrench", values.testNameFrench);
          };

          const handelTestSectionSelect = (e) => {
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

export const StepTwoTestPanelAndUom = ({
  handleNextStep,
  handlePreviousStep,
  panelList,
  setPanelList,
  uomList,
  setUomList,
  panelListTag,
  setPanelListTag,
  formData,
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
            // .min(1, "At least one panel must be selected")
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
            )
            .nullable(),
          uom: Yup.string().notOneOf(
            ["0", ""],
            "Please select a valid unit of measurement",
          ),
          // .required("Unit of measurement is required"),
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
            const selectedPanelObject = panelList.find(
              (item) => item?.id === selectedId,
            );
            if (!selectedPanelObject) return;

            setPanelListTag((tags) =>
              tags.some((tag) => tag.id === selectedId)
                ? tags
                : [
                    ...tags,
                    {
                      id: selectedPanelObject.id,
                      value: selectedPanelObject.value,
                    },
                  ],
            );

            const isAlreadyInValuesPanels = Array.isArray(values.panels)
              ? values.panels.find(
                  (panel) => String(panel?.id) === String(selectedId),
                )
              : false;

            if (!isAlreadyInValuesPanels) {
              setFieldValue("panels", [...values.panels, { id: selectedId }]);
            }

            // setPanelList((panels) => panels.filter((p) => p.id !== selectedId));
          };

          const handlePanelRemoveTag = (idToRemove) => {
            const isPresentInValuesPanels = values.panels.find(
              (panel) => panel?.id === idToRemove,
            );

            if (!isPresentInValuesPanels) return;

            setPanelListTag((tags) =>
              tags.filter((tag) => tag.id !== idToRemove),
            );

            setFieldValue(
              "panels",
              values.panels.filter((p) => String(p.id) !== String(idToRemove)),
            );

            // const idToReAddObject = panelList.find(
            //   (panel) => panel.id === idToRemove,
            // );

            // if (idToReAddObject) {
            //   setPanelList((prevPanels) => {
            //     const exists = prevPanels.some((p) => p.id === idToRemove);
            //     if (exists) return prevPanels;
            //     return [...prevPanels, idToReAddObject].sort((a, b) =>
            //       a.value.localeCompare(b.value),
            //     );
            //   });
            // }
          };

          const handelUomSelect = (e) => {
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
                    <div style={{ marginBottom: "1.188rem" }}>
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

export const StepThreeTestResultTypeAndLoinc = ({
  formData,
  handleNextStep,
  handlePreviousStep,
  resultTypeList,
  setSelectedResultTypeList,
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
          // loinc: Yup.string().matches(
          //   /^(?!-)(?:\d+-)*\d+$/,
          //   "Loinc must contain only numbers",
          // ),
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
          const handelResultType = (e) => {
            const selectedResultTypeObject = resultTypeList.find(
              (item) => item.id == e.target.value,
            );

            setFieldValue("resultType", e.target.value);

            if (selectedResultTypeObject) {
              setSelectedResultTypeList(selectedResultTypeObject);
            }
          };

          const handleAntimicrobialResistance = (e) => {
            setFieldValue(
              "antimicrobialResistance",
              e.target.checked ? "Y" : "N",
            );
          };
          const handleIsActive = (e) => {
            setFieldValue("active", e.target.checked ? "Y" : "N");
          };
          const handleOrderable = (e) => {
            setFieldValue("orderable", e.target.checked ? "Y" : "N");
          };
          const handleNotifyPatientofResults = (e) => {
            setFieldValue("notifyResults", e.target.checked ? "Y" : "N");
          };
          const handleInLabOnly = (e) => {
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

export const StepFourSelectSampleTypeAndTestDisplayOrder = ({
  formData,
  setFormData,
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
  currentStep,
}) => {
  const handleSubmit = (values) => {
    handleNextStep(values, true);
  };

  useEffect(() => {
    if (!selectedSampleTypeResp.length) {
      setFormData((prev) => ({
        ...prev,
        sampleTypes: [],
      }));
      return;
    }

    setFormData((prev) => {
      const transformed = selectedSampleTypeResp.map((resp) => ({
        typeId: String(resp.sampleTypeId),
        tests: (resp.tests || []).map((t) => ({ id: Number(t.id) })),
      }));

      return {
        ...prev,
        sampleTypes: transformed,
      };
    });
  }, [selectedSampleTypeResp]);

  return (
    <>
      {currentStep === 3 ? (
        <>
          <Formik
            initialValues={formData}
            validationSchema={Yup.object({
              sampleTypes: Yup.array()
                // .min(1, "At least one sample type must be selected")
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
                )
                .nullable(),
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
                }
              };

              const handleRemoveSampleTypeListSelectIdTestTag = (index) => {
                setSampleTestTypeToGetTagList((prev) => {
                  const updated = [...prev];
                  const removedItem = updated.splice(index, 1)[0];
                  const removedId = removedItem?.id;

                  setSelectedSampleType((prev) =>
                    prev.filter((item) => item.id !== removedId),
                  );
                  setSelectedSampleTypeResp((prev) =>
                    prev.filter((item) => item.sampleTypeId !== removedId),
                  );
                  setSelectedSampleTypeList((prev) =>
                    prev.filter((item) => item.id !== removedId),
                  );

                  return updated;
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
                      sampleTestTypeToGetTagList.length > 0 ? (
                        <div style={{ marginBottom: "1.188rem" }}>
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
                      {Array.isArray(selectedSampleTypeResp) &&
                      selectedSampleTypeResp.length > 0 ? (
                        selectedSampleTypeResp.map((item) => (
                          <>
                            <div
                              className="gridBoundary"
                              key={item.sampleTypeId}
                            >
                              <Section key={item.sampleTypeId}>
                                <CustomCommonSortableOrderList
                                  key={item.sampleTypeId}
                                  test={item.tests}
                                  onSort={(updatedList) => {
                                    const updatedResp =
                                      selectedSampleTypeResp.map(
                                        (sampleType) =>
                                          sampleType.sampleTypeId ===
                                          item.sampleTypeId
                                            ? {
                                                ...sampleType,
                                                tests: updatedList,
                                              }
                                            : sampleType,
                                      );

                                    setSelectedSampleTypeResp(updatedResp);
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

export const StepFiveSelectListOptionsAndResultOrder = ({
  formData,
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
              };

              const handleRemoveMultiSelectDictionaryListTagSelectIdTestTag = (
                index,
              ) => {
                setMultiSelectDictionaryListTag((prevTags) => {
                  const updatedTags = prevTags.filter(
                    (_, idx) => idx !== index,
                  );

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
                        <div style={{ marginBottom: "1.188rem" }}>
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
                        <div style={{ marginBottom: "1.188rem" }}>
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

export const StepSixSelectRangeAgeRangeAndSignificantDigits = ({
  formData,
  handleNextStep,
  handlePreviousStep,
  selectedResultTypeList,
  ageRangeList,
  setAgeRangeList,
  gotSelectedAgeRangeList,
  setGotSelectedAgeRangeList,
  currentStep,
  setCurrentStep,
  ageRangeFields,
  setAgeRangeFields,
  ageRanges,
  setAgeRanges,
  mode,
}) => {
  const handleSubmit = (values) => {
    handleNextStep(values, true);
  };

  useEffect(() => {
    if (mode === "edit" && ageRangeList.length && ageRangeFields.length) {
      setGotSelectedAgeRangeList(
        ageRangeFields.map((_, index) => {
          const current = ageRangeList?.[index];
          return current
            ? { id: current.id, value: current.value }
            : { id: "0", value: "Select Age Range" };
        }),
      );
    }
  }, [mode, ageRangeList, ageRangeFields.length]);

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
                  // highAgeRange: Yup.string().required(
                  //   "High age range is required",
                  // ),
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
                      (value) =>
                        value === "-Infinity" ||
                        value === "Infinity" ||
                        typeof value === "number" ||
                        !isNaN(Number(value)),
                    )
                    .test(
                      "greater-than-lowNormal",
                      "High Normal must be equal, greater than Low Normal or Infinity",
                      function (value) {
                        const { lowNormal } = this.parent;

                        const numValue = parseFloat(value);
                        const numLow = parseFloat(lowNormal);

                        const isValidNumber = (v) =>
                          v === "Infinity" ||
                          v === "-Infinity" ||
                          typeof v === "number" ||
                          !isNaN(Number(v));

                        if (
                          !isValidNumber(value) ||
                          !isValidNumber(lowNormal)
                        ) {
                          return true;
                        }

                        return (
                          (numValue === Infinity || numValue >= numLow) &&
                          numValue !== -Infinity
                        );
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
                        "High Normal Female must be equal, higher number then Low Normal or 'Infinity'",
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

              lowValid: Yup.mixed()
                .test(
                  "is-valid-number-or-infinity",
                  "Low Valid must be a number or 'Infinity'",
                  (value) =>
                    value === "-Infinity" ||
                    value === "Infinity" ||
                    typeof value === "number" ||
                    !isNaN(Number(value)),
                )
                .test(
                  "less-than-lowNormals",
                  "Low Valid must be less than Low Normal and Low Normal Female",
                  function (value) {
                    const { resultLimits } = this.parent;
                    const lowValid = parseFloat(value);

                    if (
                      !resultLimits ||
                      resultLimits.length === 0 ||
                      // isNaN(lowValid)
                      !Number.isFinite(lowValid)
                    ) {
                      return true;
                    }
                    const lowNormalRaw = resultLimits[0]?.lowNormal;
                    const lowNormalFemaleRaw = resultLimits[0]?.lowNormalFemale;

                    const lowNormal = Number.isFinite(parseFloat(lowNormalRaw))
                      ? parseFloat(lowNormalRaw)
                      : NaN;

                    const lowNormalFemale = Number.isFinite(
                      parseFloat(lowNormalFemaleRaw),
                    )
                      ? parseFloat(lowNormalFemaleRaw)
                      : NaN;

                    const validAgainstLowNormal =
                      isNaN(lowNormal) || lowValid < lowNormal;

                    const validAgainstLowNormalFemale =
                      isNaN(lowNormalFemale) || lowValid < lowNormalFemale;

                    return validAgainstLowNormal && validAgainstLowNormalFemale;
                  },
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
                  "greater-than-highNormals",
                  "High Valid must be greater than High Normal and High Normal Female",
                  function (value) {
                    const { resultLimits } = this.parent;
                    const highValid = parseFloat(value);

                    if (
                      !resultLimits ||
                      resultLimits.length === 0 ||
                      // isNaN(highValid)
                      !Number.isFinite(highValid)
                    ) {
                      return true;
                    }

                    const highNormalRaw = resultLimits[0]?.highNormal;
                    const highNormalFemaleRaw =
                      resultLimits[0]?.highNormalFemale;

                    const highNormal = Number.isFinite(
                      parseFloat(highNormalRaw),
                    )
                      ? parseFloat(highNormalRaw)
                      : NaN;

                    const highNormalFemale = Number.isFinite(
                      parseFloat(highNormalFemaleRaw),
                    )
                      ? parseFloat(highNormalFemaleRaw)
                      : NaN;

                    const validAgainstHighNormal =
                      isNaN(highNormal) || highValid > highNormal;

                    const validAgainstHighNormalFemale =
                      isNaN(highNormalFemale) || highValid > highNormalFemale;

                    return (
                      validAgainstHighNormal && validAgainstHighNormalFemale
                    );
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
                  const raw = parseFloat(ageRanges[index]?.raw || "Infinity");
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
                  highAgeRange: "Infinity",
                  gender: false,
                  lowNormal: "-Infinity",
                  highNormal: "Infinity",
                  lowNormalFemale: "-Infinity",
                  highNormalFemale: "Infinity",
                };

                const updatedLimits = [...(currentLimits || []), newLimit];
                setFieldValue("resultLimits", updatedLimits);
                setAgeRanges((prev) => {
                  const unit = prev[index]?.unit || "Y";
                  return [...prev, { raw: 0, unit: unit }];
                });
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
                              name={`resultLimits[${index}].unit`}
                              // defaultSelected={
                              //   `${ageRanges?.[index]?.unit}-${index}` ||
                              //   `Y-${index}`
                              // }
                              // value={String(
                              //   ageRanges?.[index]?.unit || `Y-${index}`,
                              // )}
                              value={ageRanges?.[index]?.unit || "Y"}
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
                              required
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
                              value={values.resultLimits?.[index]?.highAgeRange}
                              // required
                              // value={String(ageRanges[index]?.raw) || "0"}
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
                                handleRangeChange(index, "highAgeRange", val);
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
                            <Select
                              onBlur={handleBlur}
                              id={`resultLimits[${index}].ageRangeChange`}
                              name={`resultLimits[${index}].ageRangeChange`}
                              labelText=""
                              hideLabel
                              size={"md"}
                              // value={ageRangeList?.[index]?.id || "0"}
                              value={
                                gotSelectedAgeRangeList?.[index]?.id || "0"
                              }
                              onChange={(e) => {
                                // setFieldValue(
                                //   `resultLimits[${index}].ageRange`,
                                //   e.target.value,
                                // );
                                const selectedAge = ageRangeList.find(
                                  (a) => a.id === e.target.value,
                                );
                                if (selectedAge) {
                                  setGotSelectedAgeRangeList((prev) => {
                                    const updated = [...prev];
                                    updated[index] = selectedAge;
                                    return updated;
                                  });
                                }
                              }}
                              // disabled
                              invalid={
                                touched?.resultLimits?.[index]?.ageRange &&
                                !!errors?.resultLimits?.[index]?.ageRange
                              }
                              invalidText={
                                touched?.resultLimits?.[index]?.ageRange &&
                                errors?.resultLimits?.[index]?.ageRange
                              }
                            >
                              {mode !== "edit" && (
                                <SelectItem
                                  value={"0"}
                                  text={`Select Age Range`}
                                />
                              )}
                              {ageRangeList
                                .filter(
                                  (age) =>
                                    !gotSelectedAgeRangeList.some(
                                      (a, i) => i !== index && a?.id === age.id,
                                    ),
                                )
                                .map((age) => (
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
                              disabled={
                                index !== ageRangeFields.length - 1 ||
                                ageRangeFields.length >= ageRangeList.length
                              }
                              onClick={() =>
                                handleAddAgeRangeFillUp(
                                  index,
                                  values.resultLimits,
                                )
                              }
                            >
                              <FormattedMessage id="add.age.range" />
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

export const StepSevenFinalDisplayAndSaveConfirmation = ({
  formData,
  handlePreviousStep,
  handleNextStep,
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
