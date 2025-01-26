import React, { useEffect, useState } from "react";
import {
  Grid,
  Column,
  Toggle,
  FilterableMultiSelect,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Button,
  TextInput,
  Checkbox,
  Select,
  SelectItem,
  StructuredListWrapper,
  StructuredListHead,
  StructuredListRow,
  StructuredListCell,
  StructuredListBody,
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../utils/Utils.js";
import PageBreadCrumb from "../../common/PageBreadCrumb.js";
import { FormattedMessage, useIntl } from "react-intl";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "sidenav.label.admin.testmgt.AddNewTest",
    link: "/MasterListsPage#TestAdd",
  },
];

const AddNewTest = () => {
  const [showGuide, setShowGuide] = useState(false);
  const [sampleTypeList, setSampleTypeList] = useState([]);
  const [panelList, setPanelList] = useState([]);
  const [uomList, setUomList] = useState([]);
  const [resultTypeList, setResultTypeList] = useState([]);
  const [testUnitList, setTestUnitList] = useState([]);
  const [ageRangeList, setAgeRangeList] = useState([]);
  const [dictionaryList, setDictionaryList] = useState([]);
  const [groupedDictionaryList, setGroupedDictionaryList] = useState([]);
  const [form, setForm] = useState({
    testNameEnglish: "",
    testNameFrench: "",
    testReportNameEnglish: "",
    testReportNameFrench: "",
    testSection: "",
    panels: [],
    uom: "",
    loinc: "",
    resultType: "",
    orderable: false,
    notifyResults: false,
    inLabOnly: false,
    antimicrobialResistance: false,
    active: true,
    sampleTypes: [],
    resultLimits: [],
    dictionary: [],
    dictionaryReference: "",
    defaultTestResult: "",
    significantDigits: "",
    lowValid: "-Infinity",
    highValid: "Infinity",
    lowReportingRange: "-Infinity",
    highReportingRange: "Infinity",
    lowCritical: "-Infinity",
    highCritical: "Infinity",
  });
  const [step, setStep] = useState("step1");
  const intl = useIntl();

  useEffect(() => {
    getFromOpenElisServer(`/rest/TestAdd`, handleFormData);
  }, []);

  const handleFormData = (res) => {
    console.log("Fetched data:", res);
    setSampleTypeList(res.sampleTypeList || []);
    setPanelList(res.panelList || []);
    setUomList(res.uomList || []);
    setResultTypeList(res.resultTypeList || []);
    setTestUnitList(res.testUnitList || []);
    setAgeRangeList(res.ageRangeList || []);
    setDictionaryList(res.dictionaryList || []);
    setGroupedDictionaryList(res.groupedDictionaryList || []);
  };

  const handleToggle = () => {
    setShowGuide(!showGuide);
  };

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm((prevForm) => ({
      ...prevForm,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleMultiSelectChange = (name, selectedItems) => {
    setForm((prevForm) => ({
      ...prevForm,
      [name]: selectedItems.selectedItems,
    }));
  };

  const copyFromTestName = () => {
    setForm((prevForm) => ({
      ...prevForm,
      testReportNameEnglish: prevForm.testNameEnglish,
      testReportNameFrench: prevForm.testNameFrench,
    }));
  };

  const handleSubmit = () => {
    console.log("Submitting form:", form);
    postToOpenElisServer(`/rest/TestAdd`, form, (res) => {
      console.log("Form submitted successfully", res);
    });
  };

  const nextStep = () => {
    if (step === "step1") {
      setStep("step2");
    } else if (step === "step2") {
      if (form.resultType === "NUMERIC") {
        setStep("step3Numeric");
      } else if (
        ["DICTIONARY", "MULTISELECT", "CASCADING_MULTISELECT"].includes(
          form.resultType,
        )
      ) {
        setStep("step3Dictionary");
      } else {
        setStep("confirm");
      }
    } else if (step === "step3Numeric" || step === "step3Dictionary") {
      setStep("confirm");
    }
  };

  const previousStep = () => {
    if (step === "step2") {
      setStep("step1");
    } else if (step === "step3Numeric" || step === "step3Dictionary") {
      setStep("step2");
    } else if (step === "confirm") {
      if (form.resultType === "NUMERIC") {
        setStep("step3Numeric");
      } else if (
        ["DICTIONARY", "MULTISELECT", "CASCADING_MULTISELECT"].includes(
          form.resultType,
        )
      ) {
        setStep("step3Dictionary");
      } else {
        setStep("step2");
      }
    }
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
        {step === "step1" && (
          <div id="step1Div">
            <Grid>
              <Column lg={6}>
                <text>Test Name</text>
                <TextInput
                  id="testNameEnglish"
                  name="testNameEnglish"
                  labelText="English"
                  value={form.testNameEnglish}
                  onChange={handleInputChange}
                  required
                />
                <TextInput
                  id="testNameFrench"
                  name="testNameFrench"
                  labelText={intl.formatMessage({ id: "French" })}
                  value={form.testNameFrench}
                  onChange={handleInputChange}
                  required
                />
                <br></br>
                <text>Reporting Name</text>
                <br></br>
                <Button
                  onClick={copyFromTestName}
                  style={{
                    display: "flex",
                    justifyContent: "space-around",
                    marginTop: "10px",
                  }}
                >
                  <FormattedMessage id="Copy from Test Name" />
                </Button>
                <TextInput
                  id="testReportNameEnglish"
                  name="testReportNameEnglish"
                  labelText={intl.formatMessage({ id: "English" })}
                  value={form.testReportNameEnglish}
                  onChange={handleInputChange}
                  required
                />
                <TextInput
                  id="testReportNameFrench"
                  name="testReportNameFrench"
                  labelText={intl.formatMessage({ id: "French" })}
                  value={form.testReportNameFrench}
                  onChange={handleInputChange}
                  required
                />
              </Column>
              <Column lg={6}>
                <Select
                  id="testUnitSelection"
                  name="testSection"
                  labelText={intl.formatMessage({ id: "Test Section" })}
                  value={form.testSection}
                  onChange={handleInputChange}
                  required
                >
                  <SelectItem value="" text="Select a test section" />
                  {testUnitList.map((item) => (
                    <SelectItem
                      key={item.id}
                      value={item.id}
                      text={item.value}
                    />
                  ))}
                </Select>
                <FilterableMultiSelect
                  id="panelSelection"
                  titleText={intl.formatMessage({ id: "Panel" })}
                  items={panelList}
                  itemToString={(item) => (item ? item.value : "")}
                  onChange={(selectedItems) =>
                    handleMultiSelectChange("panels", selectedItems)
                  }
                  selectedItems={form.panels}
                />
                <Select
                  id="uomSelection"
                  name="uom"
                  labelText={intl.formatMessage({ id: "Unit of Measure" })}
                  value={form.uom}
                  onChange={handleInputChange}
                >
                  <SelectItem value="" text="Select a unit of measure" />
                  {uomList.map((item) => (
                    <SelectItem
                      key={item.id}
                      value={item.id}
                      text={item.value}
                    />
                  ))}
                </Select>
                <TextInput
                  id="loinc"
                  name="loinc"
                  labelText={intl.formatMessage({ id: "LOINC" })}
                  value={form.loinc}
                  onChange={handleInputChange}
                />
                <Select
                  id="resultTypeSelection"
                  name="resultType"
                  labelText={intl.formatMessage({ id: "Result Type" })}
                  value={form.resultType}
                  onChange={handleInputChange}
                  required
                >
                  <SelectItem value="" text="Select a result type" />
                  {resultTypeList.map((item) => (
                    <SelectItem
                      key={item.id}
                      value={item.id}
                      text={item.value}
                    />
                  ))}
                </Select>
                <Checkbox
                  id="antimicrobialResistance"
                  name="antimicrobialResistance"
                  labelText={intl.formatMessage({
                    id: "Antimicrobial Resistance",
                  })}
                  checked={form.antimicrobialResistance}
                  onChange={handleInputChange}
                />
                <Checkbox
                  id="active"
                  name="active"
                  labelText={intl.formatMessage({ id: "Is Active" })}
                  checked={form.active}
                  onChange={handleInputChange}
                />
                <Checkbox
                  id="orderable"
                  name="orderable"
                  labelText={intl.formatMessage({ id: "Orderable" })}
                  checked={form.orderable}
                  onChange={handleInputChange}
                />
                <Checkbox
                  id="notifyResults"
                  name="notifyResults"
                  labelText={intl.formatMessage({ id: "Notify Results" })}
                  checked={form.notifyResults}
                  onChange={handleInputChange}
                />
                <Checkbox
                  id="inLabOnly"
                  name="inLabOnly"
                  labelText={intl.formatMessage({ id: "In Lab Only" })}
                  checked={form.inLabOnly}
                  onChange={handleInputChange}
                />
              </Column>
            </Grid>
          </div>
        )}
        {step === "step2" && (
          <div id="sampleTypeContainer">
            <Grid>
              <Column lg={4}>
                <FilterableMultiSelect
                  id="sampleTypeSelection"
                  titleText={intl.formatMessage({ id: "Sample Type" })}
                  items={sampleTypeList}
                  itemToString={(item) => (item ? item.value : "")}
                  onChange={(selectedItems) =>
                    handleMultiSelectChange("sampleTypes", selectedItems)
                  }
                  selectedItems={form.sampleTypes}
                />
              </Column>
              <Column lg={8}>
                <div id="sortTitleDiv" align="center">
                  <FormattedMessage id="Display Order" />
                </div>
                <div id="endOrderMarker"></div>
              </Column>
            </Grid>
          </div>
        )}
        {step === "step3Numeric" && (
          <div id="normalRangeDiv">
            <h3>
              <FormattedMessage id="label.range" />
            </h3>
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableHeader colSpan={6}>
                      <FormattedMessage id="label.age.range" />
                    </TableHeader>
                    <TableHeader colSpan={2}>
                      <FormattedMessage id="configuration.test.catalog.normal.range" />
                    </TableHeader>
                    <TableHeader colSpan={2}>
                      <FormattedMessage id="label.reporting.range" />
                    </TableHeader>
                    <TableHeader colSpan={2}>
                      <FormattedMessage id="configuration.test.catalog.valid.range" />
                    </TableHeader>
                    <TableHeader colSpan={4}>
                      <FormattedMessage id="configuration.test.catalog.critical.range" />
                    </TableHeader>
                  </TableRow>
                </TableHead>
                <TableBody>
                  <TableRow>
                    <TableCell>
                      <Checkbox
                        id="genderCheck_0"
                        name="genderCheck_0"
                        labelText={intl.formatMessage({
                          id: "label.sex.dependent",
                        })}
                        onChange={handleInputChange}
                      />
                    </TableCell>
                    <TableCell>
                      <span className="sexRange_0" style={{ display: "none" }}>
                        <FormattedMessage id="label.sex" />
                      </span>
                    </TableCell>
                    <TableCell colSpan={4} align="center"></TableCell>
                    <TableCell colSpan={2} align="center"></TableCell>
                    <TableCell colSpan={2} align="center"></TableCell>
                    <TableCell colSpan={2}></TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>
                      <TextInput
                        id="lowNormal_0"
                        name="lowNormal_0"
                        value={form.lowNormal_0}
                        onChange={handleInputChange}
                      />
                    </TableCell>
                    <TableCell>
                      <TextInput
                        id="highNormal_0"
                        name="highNormal_0"
                        value={form.highNormal_0}
                        onChange={handleInputChange}
                      />
                    </TableCell>
                    <TableCell>
                      <TextInput
                        id="lowReportingRange"
                        name="lowReportingRange"
                        value={form.lowReportingRange}
                        onChange={handleInputChange}
                      />
                    </TableCell>
                    <TableCell>
                      <TextInput
                        id="highReportingRange"
                        name="highReportingRange"
                        value={form.highReportingRange}
                        onChange={handleInputChange}
                      />
                    </TableCell>
                    <TableCell>
                      <TextInput
                        id="lowValid"
                        name="lowValid"
                        value={form.lowValid}
                        onChange={handleInputChange}
                      />
                    </TableCell>
                    <TableCell>
                      <TextInput
                        id="highValid"
                        name="highValid"
                        value={form.highValid}
                        onChange={handleInputChange}
                      />
                    </TableCell>
                    <TableCell>
                      <TextInput
                        id="lowCritical"
                        name="lowCritical"
                        value={form.lowCritical}
                        onChange={handleInputChange}
                      />
                    </TableCell>
                    <TableCell>
                      <TextInput
                        id="highCritical"
                        name="highCritical"
                        value={form.highCritical}
                        onChange={handleInputChange}
                      />
                    </TableCell>
                  </TableRow>
                </TableBody>
              </Table>
            </TableContainer>
            <TextInput
              id="significantDigits"
              name="significantDigits"
              labelText={intl.formatMessage({ id: "Significant Digits" })}
              value={form.significantDigits}
              onChange={handleInputChange}
            />
          </div>
        )}
        {step === "step3Dictionary" && (
          <div id="dictionarySelectId">
            <FilterableMultiSelect
              id="dictionarySelection"
              titleText={intl.formatMessage({ id: "List Options" })}
              items={dictionaryList}
              itemToString={(item) => (item ? item.value : "")}
              onChange={(selectedItems) =>
                handleMultiSelectChange("dictionary", selectedItems)
              }
              selectedItems={form.dictionary}
            />
            <Select
              id="referenceSelection"
              name="dictionaryReference"
              labelText={intl.formatMessage({ id: "Reference Value" })}
              value={form.dictionaryReference}
              onChange={handleInputChange}
            >
              <SelectItem value="" text="Select a reference value" />
              {dictionaryList.map((item) => (
                <SelectItem key={item.id} value={item.id} text={item.value} />
              ))}
            </Select>
            <Select
              id="defaultTestResultSelection"
              name="defaultTestResult"
              labelText={intl.formatMessage({ id: "Default Result" })}
              value={form.defaultTestResult}
              onChange={handleInputChange}
            >
              <SelectItem value="" text="Select a default test result" />
              {dictionaryList.map((item) => (
                <SelectItem key={item.id} value={item.id} text={item.value} />
              ))}
            </Select>
            <FilterableMultiSelect
              id="qualifierSelection"
              titleText={intl.formatMessage({ id: "Qualifiers" })}
              items={dictionaryList}
              itemToString={(item) => (item ? item.value : "")}
              onChange={(selectedItems) =>
                handleMultiSelectChange("qualifiers", selectedItems)
              }
              selectedItems={form.qualifiers}
            />
          </div>
        )}
        {step === "confirm" && (
          <div id="confirmDiv">
            <h3>
              <FormattedMessage id="Confirmation" />
            </h3>
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableHeader>
                      <FormattedMessage id="field" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="value" />
                    </TableHeader>
                  </TableRow>
                </TableHead>
                <TableBody>
                  <TableRow>
                    <TableCell>
                      <FormattedMessage id="Test Name(English)" />
                    </TableCell>
                    <TableCell>{form.testNameEnglish}</TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>
                      <FormattedMessage id="Test Name(French)" />
                    </TableCell>
                    <TableCell>{form.testNameFrench}</TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>
                      <FormattedMessage id="Reporting Name(English)" />
                    </TableCell>
                    <TableCell>{form.testReportNameEnglish}</TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>
                      <FormattedMessage id="Reporting Name(French)" />
                    </TableCell>
                    <TableCell>{form.testReportNameFrench}</TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>
                      <FormattedMessage id="Section Name" />
                    </TableCell>
                    <TableCell>
                      {
                        testUnitList.find(
                          (item) => item.id === form.testSection,
                        )?.value
                      }
                    </TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>
                      <FormattedMessage id="Panel" />
                    </TableCell>
                    <TableCell>
                      {form.panels.map((panel) => panel.value).join(", ")}
                    </TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>
                      <FormattedMessage id="Unit of Measure" />
                    </TableCell>
                    <TableCell>
                      {uomList.find((item) => item.id === form.uom)?.value}
                    </TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>
                      <FormattedMessage id="LOINC" />
                    </TableCell>
                    <TableCell>{form.loinc}</TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>
                      <FormattedMessage id="Result Type" />
                    </TableCell>
                    <TableCell>
                      {
                        resultTypeList.find(
                          (item) => item.id === form.resultType,
                        )?.value
                      }
                    </TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>
                      <FormattedMessage id="Antimicrobial Resistance" />
                    </TableCell>
                    <TableCell>
                      {form.antimicrobialResistance ? "Yes" : "No"}
                    </TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>
                      <FormattedMessage id="Is Active" />
                    </TableCell>
                    <TableCell>{form.active ? "Yes" : "No"}</TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>
                      <FormattedMessage id="Orderable" />
                    </TableCell>
                    <TableCell>{form.orderable ? "Yes" : "No"}</TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>
                      <FormattedMessage id="Notify Results" />
                    </TableCell>
                    <TableCell>{form.notifyResults ? "Yes" : "No"}</TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>
                      <FormattedMessage id="In Lab Only" />
                    </TableCell>
                    <TableCell>{form.inLabOnly ? "Yes" : "No"}</TableCell>
                  </TableRow>
                  {form.resultType === "NUMERIC" && (
                    <>
                      <TableRow>
                        <TableCell>
                          <FormattedMessage id="Significant Digits" />
                        </TableCell>
                        <TableCell>{form.significantDigits}</TableCell>
                      </TableRow>
                      <TableRow>
                        <TableCell>
                          <FormattedMessage id="Low Valid" />
                        </TableCell>
                        <TableCell>{form.lowValid}</TableCell>
                      </TableRow>
                      <TableRow>
                        <TableCell>
                          <FormattedMessage id="High Valid" />
                        </TableCell>
                        <TableCell>{form.highValid}</TableCell>
                      </TableRow>
                      <TableRow>
                        <TableCell>
                          <FormattedMessage id="Low Reporting Range" />
                        </TableCell>
                        <TableCell>{form.lowReportingRange}</TableCell>
                      </TableRow>
                      <TableRow>
                        <TableCell>
                          <FormattedMessage id="High Reporting Range" />
                        </TableCell>
                        <TableCell>{form.highReportingRange}</TableCell>
                      </TableRow>
                      <TableRow>
                        <TableCell>
                          <FormattedMessage id="Low Critical" />
                        </TableCell>
                        <TableCell>{form.lowCritical}</TableCell>
                      </TableRow>
                      <TableRow>
                        <TableCell>
                          <FormattedMessage id="High Critical" />
                        </TableCell>
                        <TableCell>{form.highCritical}</TableCell>
                      </TableRow>
                    </>
                  )}
                  {[
                    "DICTIONARY",
                    "MULTISELECT",
                    "CASCADING_MULTISELECT",
                  ].includes(form.resultType) && (
                    <>
                      <TableRow>
                        <TableCell>
                          <FormattedMessage id="Value" />
                        </TableCell>
                        <TableCell>
                          {
                            dictionaryList.find(
                              (item) => item.id === form.dictionaryReference,
                            )?.value
                          }
                        </TableCell>
                      </TableRow>
                      <TableRow>
                        <TableCell>
                          <FormattedMessage id="Result" />
                        </TableCell>
                        <TableCell>
                          {
                            dictionaryList.find(
                              (item) => item.id === form.defaultTestResult,
                            )?.value
                          }
                        </TableCell>
                      </TableRow>
                      <TableRow>
                        <TableCell>
                          <FormattedMessage id="Qualifiers" />
                        </TableCell>
                        <TableCell>
                          {form.qualifiers
                            .map((qualifier) => qualifier.value)
                            .join(", ")}
                        </TableCell>
                      </TableRow>
                    </>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </div>
        )}
        <div
          className="selectShow"
          style={{
            display: "flex",
            justifyContent: "space-between",
            marginTop: "20px",
          }}
        >
          {step !== "confirm" && (
            <Button onClick={previousStep} disabled={step === "step1"}>
              <FormattedMessage id="Back" />
            </Button>
          )}
          {step !== "confirm" && (
            <Button onClick={nextStep}>
              <FormattedMessage id="Next" />
            </Button>
          )}
        </div>
        <div
          className="confirmShow"
          style={{ display: "flex", justifyContent: "space-between" }}
        >
          {step === "confirm" && (
            <Button onClick={previousStep}>
              <FormattedMessage id="Back" />
            </Button>
          )}
          {step === "confirm" && (
            <Button onClick={handleSubmit}>
              <FormattedMessage id="Accept" />
            </Button>
          )}
        </div>
      </div>
    </div>
  );
};

export default AddNewTest;
