import React, { useState, useEffect } from "react";
import {
  Grid,
  Column,
  Toggle,
  TextInput,
  Select,
  SelectItem,
  MultiSelect,
  Button,
  Checkbox,
  StructuredListWrapper,
  StructuredListHead,
  StructuredListRow,
  StructuredListCell,
  StructuredListBody,
  Section,
  Heading,
} from "@carbon/react";
import { getFromOpenElisServer, postToOpenElisServer } from "../../utils/Utils.js";
import PageBreadCrumb from "../../common/PageBreadCrumb.js";
import { FormattedMessage, useIntl } from "react-intl";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "sidenav.label.admin.testmgt.AddNewTest",
    link: "/MasterListsPage#AddNewTest",
  },
];

const AddNewTest = () => {
  const [step, setStep] = useState(1);
  const [showGuide, setShowGuide] = useState(false);
  const [formData, setFormData] = useState({
    testNameEnglish: "",
    testNameFrench: "",
    testReportNameEnglish: "",
    testReportNameFrench: "",
    testUnitSelection: "",
    panelSelection: [],
    uomSelection: "",
    resultTypeSelection: "",
    loinc: "",
    antimicrobialResistance: false,
    active: true,
    orderable: true,
    notifyResults: false,
    inLabOnly: false,
    sampleTypes: [],
    dictionary: [],
    normalRange: {
      lowNormal: "-Infinity",
      highNormal: "Infinity",
      lowValid: "-Infinity",
      highValid: "Infinity",
      lowReportingRange: "-Infinity",
      highReportingRange: "Infinity",
      lowCritical: "-Infinity",
      highCritical: "-Infinity",
      significantDigits: "",
    },
  });

  const [testUnits, setTestUnits] = useState([]);
  const [panels, setPanels] = useState([]);
  const [uoms, setUoms] = useState([]);
  const [resultTypes, setResultTypes] = useState([]);
  const [sampleTypes, setSampleTypes] = useState([]);
  const [dictionaries, setDictionaries] = useState([]);
  const [ageRangeList, setAgeRangeList] = useState([]);
  const [error, setError] = useState(null);
  const intl = useIntl();

  const fetchAllData = async () => {
    try {
      const response = await getFromOpenElisServer("/api/test/add");
      setTestUnits(response.labUnitList || []);
      setPanels(response.panelList || []);
      setUoms(response.uomList || []);
      setResultTypes(response.resultTypeList || []);
      setSampleTypes(response.sampleTypeList || []);
      setDictionaries(response.dictionaryList || []);
      setAgeRangeList(response.ageRangeList || []);
    } catch (error) {
      console.error("Error fetching data:", error);
      setError("Failed to fetch data.");
    }
  };

  useEffect(() => {
    fetchAllData();
  }, []);

  const handleTestAdd = (response) => {
    if (response && (response.status === 200 || response.status === 201)) {
      console.log("Test added successfully:", response.data);
      alert("Test added successfully!");
    } else {
      setError("Failed to add test. Please try again.");
      console.error("Error adding test:", response);
    }
  };

  const nextStep = () => setStep(step + 1);
  const prevStep = () => setStep(step - 1);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    setFormData({
      ...formData,
      [name]: type === "checkbox" ? checked : value,
    });
  };

  const handleMultiSelectChange = (selectedItems) => {
    setFormData({
      ...formData,
      panelSelection: selectedItems.selectedItems || selectedItems,
    });
  };

  const handleSubmit = async () => {
    try {
      const response = await postToOpenElisServer("/rest/TestAdd", formData);
      handleTestAdd(response); 
    } catch (error) {
      setError("Failed to add test. Please try again.");
      console.error("Error adding test:", error);
    }
  };

  const handleToggle = () => {
    setShowGuide(!showGuide);
  };

  const rows = [
     {
       id: "name",
       field: intl.formatMessage({ id: "field.name" }),
       description: <FormattedMessage id="description.name" />,
     },
     {
       id: "reportName",
       field: intl.formatMessage({ id: "field.reportName" }),
       description: <FormattedMessage id="description.reportName" />,
     },
     {
       id: "active",
       field: intl.formatMessage({ id: "field.active" }),
       description: <FormattedMessage id="description.active" />,
     },
     {
       id: "orderable",
       field: intl.formatMessage({ id: "field.orderable" }),
       description: <FormattedMessage id="description.orderable" />,
     },
     {
       id: "testUnit",
       field: intl.formatMessage({ id: "field.testUnit" }),
       description: <FormattedMessage id="description.testUnit" />,
     },
     {
       id: "sampleType",
       field: intl.formatMessage({ id: "field.sampleType" }),
       description: <FormattedMessage id="description.sampleType" />,
     },
     {
       id: "panel",
       field: intl.formatMessage({ id: "field.panel" }),
       description: <FormattedMessage id="description.panel" />,
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
       id: "uom",
       field: intl.formatMessage({ id: "field.uom" }),
       description: <FormattedMessage id="description.uom" />,
     },
     {
       id: "significantDigits",
       field: intl.formatMessage({ id: "field.significantDigits" }),
       description: <FormattedMessage id="description.significantDigits" />,
     },
     {
       id: "selectValues",
       field: intl.formatMessage({ id: "field.selectValues" }),
       description: <FormattedMessage id="description.selectValues" />,
     },
     {
       id: "referenceValue",
       field: intl.formatMessage({ id: "field.referenceValue" }),
       description: <FormattedMessage id="description.referenceValue" />,
     },
     {
       id: "resultLimits",
       field: intl.formatMessage({ id: "field.resultLimits" }),
       description: <FormattedMessage id="description.resultLimits" />,
     },
     {
       id: "sex",
       field: intl.formatMessage({ id: "field.sex" }),
       description: <FormattedMessage id="description.sex" />,
     },
     {
       id: "ageRange",
       field: intl.formatMessage({ id: "field.ageRange" }),
       description: <FormattedMessage id="description.ageRange" />,
     },
     {
       id: "normalRange",
       field: intl.formatMessage({ id: "field.normalRange" }),
       description: <FormattedMessage id="description.normalRange" />,
     },
     {
       id: "validRange",
       field: intl.formatMessage({ id: "field.validRange" }),
       description: <FormattedMessage id="description.validRange" />,
     },
     {
       id: "note",
       field: intl.formatMessage({ id: "field.note" }),
       description: <FormattedMessage id="description.note" />,
     },
   ];

  return (
    <div className="adminPageContent">
      <br />
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <br />
      <div className="orderLegendBody">
        <Grid fullWidth={true}>
          <Column lg={12}>
            <h1>
              {" "}
              <FormattedMessage id="Add New Test" />
            </h1>
          </Column>
          <Column lg={4} md={8} sm={12}>
            <Toggle id="toggle" labelText="Show Guide" onClick={handleToggle} />
          </Column>
        </Grid>
        <hr />

        {showGuide && (
          <>
            <StructuredListWrapper ariaLabel="Structured list">
              <StructuredListHead>
                <StructuredListRow head>
                  <StructuredListCell head>Field</StructuredListCell>
                  <StructuredListCell head>Description</StructuredListCell>
                </StructuredListRow>
              </StructuredListHead>
              <StructuredListBody>
                {rows.map((row) => (
                  <StructuredListRow key={row.id}>
                    <StructuredListCell>{row.field}</StructuredListCell>
                    <StructuredListCell>{row.description}</StructuredListCell>
                  </StructuredListRow>
                ))}
              </StructuredListBody>
            </StructuredListWrapper>
            <hr />
            <br />
          </>
        )}

        {step === 1 && (
          <TestForm
            formData={formData}
            handleChange={handleChange}
            nextStep={nextStep}
            testUnits={testUnits}
            panels={panels}
            uoms={uoms}
            resultTypes={resultTypes}
            handleMultiSelectChange={handleMultiSelectChange}
          />
        )}
        {step === 2 && (
          <SampleTypeSelection
            formData={formData}
            handleChange={handleChange}
            nextStep={nextStep}
            prevStep={prevStep}
            sampleTypes={sampleTypes}
          />
        )}
        {step === 3 && (
          <DictionarySelection
            formData={formData}
            handleChange={handleChange}
            nextStep={nextStep}
            prevStep={prevStep}
            dictionaries={dictionaries}
          />
        )}
        {step === 4 && (
          <NormalRangeComponent
            formData={formData}
            handleChange={handleChange}
            handleSubmit={handleSubmit}
            prevStep={prevStep}
            ageRangeList={ageRangeList}
          />
        )}
      </div>
    </div>
  );
};

const TestForm = ({
  formData,
  handleChange,
  nextStep,
  testUnits,
  panels,
  uoms,
  resultTypes,
  handleMultiSelectChange,
}) => {
  return (
    <Grid>
      <Column lg={8}>
        <TextInput
          id="testNameEnglish"
          name="testNameEnglish"
          labelText="Test Name (English)"
          value={formData.testNameEnglish}
          onChange={handleChange}
        />
        <TextInput
          id="testNameFrench"
          name="testNameFrench"
          labelText="Test Name (French)"
          value={formData.testNameFrench}
          onChange={handleChange}
        />
        <TextInput
          id="testReportNameEnglish"
          name="testReportNameEnglish"
          labelText="Test Report Name (English)"
          value={formData.testReportNameEnglish}
          onChange={handleChange}
        />
        <TextInput
          id="testReportNameFrench"
          name="testReportNameFrench"
          labelText="Test Report Name (French)"
          value={formData.testReportNameFrench}
          onChange={handleChange}
        />
        <Select
          id="testUnitSelection"
          name="testUnitSelection"
          labelText="Test Section"
          value={formData.testUnitSelection}
          onChange={handleChange}
        >
          <SelectItem value="" text="Select" />
          {testUnits.map((unit) => (
            <SelectItem key={unit?.id} value={unit?.id} text={unit?.value} />
          ))}
        </Select>
        <MultiSelect
          id="panelSelection"
          name="panelSelection"
          labelText="Panels"
          items={panels}
          itemToString={(item) => item?.value}
          onChange={handleMultiSelectChange}
          selectedItems={formData.panelSelection}
        />
        <Select
          id="uomSelection"
          name="uomSelection"
          labelText="Unit of Measure"
          value={formData.uomSelection}
          onChange={handleChange}
        >
          <SelectItem value="" text="Select" />
          {uoms.map((uom) => (
            <SelectItem key={uom?.id} value={uom?.id} text={uom?.value} />
          ))}
        </Select>
        <Select
          id="resultTypeSelection"
          name="resultTypeSelection"
          labelText="Result Type"
          value={formData.resultTypeSelection}
          onChange={handleChange}
        >
          <SelectItem value="" text="Select" />
          {resultTypes.map((type) => (
            <SelectItem key={type?.id} value={type?.id} text={type?.value} />
          ))}
        </Select>
        <TextInput
          id="loinc"
          name="loinc"
          labelText="LOINC"
          value={formData.loinc}
          onChange={handleChange}
        />
        <Checkbox
          id="active"
          name="active"
          labelText="Is Active"
          checked={formData.active}
          onChange={handleChange}
        />
        <Checkbox
          id="orderable"
          name="orderable"
          labelText="Orderable"
          checked={formData.orderable}
          onChange={handleChange}
        />
        <Checkbox
          id="notifyResults"
          name="notifyResults"
          labelText="Notify Results"
          checked={formData.notifyResults}
          onChange={handleChange}
        />
        <Checkbox
          id="inLabOnly"
          name="inLabOnly"
          labelText="In Lab Only"
          checked={formData.inLabOnly}
          onChange={handleChange}
        />
        <Checkbox
          id="antimicrobialResistance"
          name="antimicrobialResistance"
          labelText="Antimicrobial Resistance"
          checked={formData.antimicrobialResistance}
          onChange={handleChange}
        />
        <Button onClick={nextStep}>Next</Button>
      </Column>
    </Grid>
  );
};

const SampleTypeSelection = ({
  formData,
  handleChange,
  nextStep,
  prevStep,
  sampleTypes,
}) => {
  return (
    <Grid>
      <Column lg={8}>
        <MultiSelect
          id="sampleTypeSelection"
          name="sampleTypes"
          labelText="Sample Type"
          items={sampleTypes}
          itemToString={(item) => item?.value}
          onChange={handleChange}
          selectedItems={formData.sampleTypes}
        />
        <Button onClick={prevStep}>Back</Button>
        <Button onClick={nextStep}>Next</Button>
      </Column>
    </Grid>
  );
};

const DictionarySelection = ({
  formData,
  handleChange,
  nextStep,
  prevStep,
  dictionaries,
}) => {
  return (
    <Grid>
      <Column lg={8}>
        <MultiSelect
          id="dictionarySelection"
          name="dictionary"
          labelText="Dictionary"
          items={dictionaries}
          itemToString={(item) => item?.value}
          onChange={handleChange}
          selectedItems={formData.dictionary}
        />
        <Button onClick={prevStep}>Back</Button>
        <Button onClick={nextStep}>Next</Button>
      </Column>
    </Grid>
  );
};

const NormalRangeComponent = ({
  formData,
  handleChange,
  handleSubmit,
  prevStep,
  ageRangeList,
}) => {
  return (
    <Grid>
      <Column lg={8}>
        <TextInput
          id="lowNormal"
          name="normalRange.lowNormal"
          labelText="Low Normal"
          value={formData.normalRange.lowNormal}
          onChange={handleChange}
        />
        <TextInput
          id="highNormal"
          name="normalRange.highNormal"
          labelText="High Normal"
          value={formData.normalRange.highNormal}
          onChange={handleChange}
        />
        <TextInput
          id="significantDigits"
          name="normalRange.significantDigits"
          labelText="Significant Digits"
          value={formData.normalRange.significantDigits}
          onChange={handleChange}
        />
        <Button onClick={prevStep}>Back</Button>
        <Button onClick={handleSubmit}>Submit</Button>
      </Column>
    </Grid>
  );
};

export default AddNewTest;