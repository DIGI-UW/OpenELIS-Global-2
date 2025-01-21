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
  StructuredListWrapper,
  StructuredListHead,
  StructuredListRow,
  StructuredListCell,
  StructuredListBody,
  Section,
} from "@carbon/react";

// Utility functions for API calls
const getFromOpenElisServer = async (endpoint) => {
  try {
    const response = await fetch(`http://your-backend-server${endpoint}`);
    if (!response.ok) {
      throw new Error("Network response was not ok");
    }
    return await response.json();
  } catch (error) {
    console.error("Error fetching data:", error);
    return [];
  }
};

const postToOpenElisServer = async (endpoint, data) => {
  try {
    const response = await fetch(`http://your-backend-server${endpoint}`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(data),
    });
    if (!response.ok) {
      throw new Error("Network response was not ok");
    }
    return await response.json();
  } catch (error) {
    console.error("Error posting data:", error);
    throw error;
  }
};

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
      highCritical: "Infinity",
      significantDigits: "",
    },
  });

  const [testUnits, setTestUnits] = useState([]);
  const [panels, setPanels] = useState([]);
  const [uoms, setUoms] = useState([]);
  const [resultTypes, setResultTypes] = useState([]);
  const [sampleTypes, setSampleTypes] = useState([]);
  const [dictionaries, setDictionaries] = useState([]);

  // Fetch data from the backend on component mount
  useEffect(() => {
    console.log("Fetching data...");
    fetchTestUnits();
    fetchPanels();
    fetchUoms();
    fetchResultTypes();
    fetchSampleTypes();
    fetchDictionaries();
  }, []);

  const fetchTestUnits = async () => {
    console.log("Fetching test units...");
    const response = await getFromOpenElisServer("/rest/TestUnits");
    console.log("Test Units:", response);
    setTestUnits(response);
  };

  const fetchPanels = async () => {
    console.log("Fetching panels...");
    const response = await getFromOpenElisServer("/rest/Panels");
    console.log("Panels:", response);
    setPanels(response);
  };

  const fetchUoms = async () => {
    console.log("Fetching UOMs...");
    const response = await getFromOpenElisServer("/rest/Uoms");
    console.log("UOMs:", response);
    setUoms(response);
  };

  const fetchResultTypes = async () => {
    console.log("Fetching result types...");
    const response = await getFromOpenElisServer("/rest/ResultTypes");
    console.log("Result Types:", response);
    setResultTypes(response);
  };

  const fetchSampleTypes = async () => {
    console.log("Fetching sample types...");
    const response = await getFromOpenElisServer("/rest/SampleTypes");
    console.log("Sample Types:", response);
    setSampleTypes(response);
  };

  const fetchDictionaries = async () => {
    console.log("Fetching dictionaries...");
    const response = await getFromOpenElisServer("/rest/Dictionaries");
    console.log("Dictionaries:", response);
    setDictionaries(response);
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
      panelSelection: selectedItems.selectedItems,
    });
  };

  const handleSubmit = async () => {
    try {
      console.log("Submitting form data:", formData);
      const response = await postToOpenElisServer("/rest/TestAdd", formData);
      console.log("Test added successfully:", response);
      alert("Test added successfully!");
    } catch (error) {
      console.error("Error adding test:", error);
      alert("Failed to add test.");
    }
  };

  const handleToggle = () => {
    setShowGuide(!showGuide);
  };

  return (
    <div className="adminPageContent">
      <Grid fullWidth>
        <Column lg={16}>
          <h1>Add New Test</h1>
        </Column>
        <Column lg={4}>
          <Toggle
            id="toggle-guide"
            labelText="Show Guide"
            toggled={showGuide}
            onToggle={handleToggle}
          />
        </Column>
      </Grid>
      <hr />

      {showGuide && (
        <Section>
          <StructuredListWrapper ariaLabel="Form Guide">
            <StructuredListHead>
              <StructuredListRow head>
                <StructuredListCell head>Field</StructuredListCell>
                <StructuredListCell head>Description</StructuredListCell>
              </StructuredListRow>
            </StructuredListHead>
            <StructuredListBody>
              <StructuredListRow>
                <StructuredListCell>Test Name</StructuredListCell>
                <StructuredListCell>
                  Enter the name of the test in English and French.
                </StructuredListCell>
              </StructuredListRow>
              {/* Add more guide rows as needed */}
            </StructuredListBody>
          </StructuredListWrapper>
        </Section>
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
        />
      )}
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
          labelText="Test Name (English)"
          value={formData.testNameEnglish}
          onChange={handleChange}
        />
        <TextInput
          id="testNameFrench"
          labelText="Test Name (French)"
          value={formData.testNameFrench}
          onChange={handleChange}
        />
        <TextInput
          id="testReportNameEnglish"
          labelText="Test Report Name (English)"
          value={formData.testReportNameEnglish}
          onChange={handleChange}
        />
        <TextInput
          id="testReportNameFrench"
          labelText="Test Report Name (French)"
          value={formData.testReportNameFrench}
          onChange={handleChange}
        />
        <Select
          id="testUnitSelection"
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
          labelText="Panels"
          items={panels}
          itemToString={(item) => item?.value}
          onChange={handleMultiSelectChange}
          selectedItems={formData.panelSelection}
        />
        <Select
          id="uomSelection"
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
          labelText="LOINC"
          value={formData.loinc}
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
}) => {
  return (
    <Grid>
      <Column lg={8}>
        <TextInput
          id="lowNormal"
          labelText="Low Normal"
          value={formData.normalRange.lowNormal}
          onChange={handleChange}
        />
        <TextInput
          id="highNormal"
          labelText="High Normal"
          value={formData.normalRange.highNormal}
          onChange={handleChange}
        />
        <TextInput
          id="significantDigits"
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
