import React, { useEffect, useState } from "react";
import {
  Grid,
  Column,
  Section,
  Heading,
  TextInput,
  Button,
} from "@carbon/react";
import { FormattedMessage } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
import CustomDatePicker from "../common/CustomDatePicker";
import CustomTimePicker from "../common/CustomTimePicker";
import CustomSelect from "../common/CustomSelect";
import CustomLabNumberInput from "../common/CustomLabNumberInput";
import Questionnaire from "../common/Questionnaire"; // Import the Questionnaire component
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";

export default function GenericSampleOrder() {
  // Default fields as specified
  const [defaultForm, setDefaultForm] = useState({
    labNo: "",
    sampleTypeId: "",
    quantity: "",
    sampleUnitOfMeasure: "",
    from: "",
    collector: "",
    collectionDate: "",
    collectionTime: "",
  });

  // FHIR Questionnaire data and state
  const [fhirQuestionnaire, setFhirQuestionnaire] = useState(null);
  const [fhirResponses, setFhirResponses] = useState({});
  const [questionnaireLoading, setQuestionnaireLoading] = useState(false);

  // Notebook selection
  const [notebooks, setNotebooks] = useState([]);
  const [selectedNotebookId, setSelectedNotebookId] = useState(null);

  // Dropdown lists
  const [sampleTypes, setSampleTypes] = useState([]);
  const [uoms, setUoms] = useState([]);
  const [labNoLoading, setLabNoLoading] = useState(false);

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "menu.genericSample" },
    { label: "menu.genericSample.order" },
  ];

  // Load default data and notebooks
  useEffect(() => {
    // Load default dropdown data
    getFromOpenElisServer("/rest/user-sample-types", (res) => {
      setSampleTypes(res || []);
    });
    getFromOpenElisServer("/rest/UomCreate", (res) => {
      setUoms(res.existingUomList || []);
    });

    // Load available notebooks
    getFromOpenElisServer("/rest/notebook/list", (res) => {
      setNotebooks(Array.isArray(res) ? res : []);
    });

    // Generate LabNo
    handleLabNoGeneration();
  }, []);

  // Load FHIR Questionnaire when notebook is selected
  useEffect(() => {
    if (selectedNotebookId) {
      loadFhirQuestionnaireForNotebook(selectedNotebookId);
    } else {
      setFhirQuestionnaire(null);
      setFhirResponses({});
    }
  }, [selectedNotebookId]);

  // Load FHIR Questionnaire for selected notebook
  const loadFhirQuestionnaireForNotebook = (notebookId) => {
    setQuestionnaireLoading(true);

    // Find the selected notebook to get its questionnaire UUID
    const notebook = notebooks.find((n) => n.id === parseInt(notebookId));
    if (notebook && notebook.questionnaireFhirUuid) {
      getFromOpenElisServer(
        "/rest/fhir/Questionnaire/" + notebook.questionnaireFhirUuid,
        (res) => {
          setFhirQuestionnaire(res || null);
          setQuestionnaireLoading(false);
        },
      );
    } else {
      setFhirQuestionnaire(null);
      setQuestionnaireLoading(false);
    }
  };

  // Handle notebook selection
  const handleNotebookChange = (notebookId) => {
    setSelectedNotebookId(notebookId);
  };

  // Lab number generation
  const handleLabNoGeneration = () => {
    setLabNoLoading(true);
    getFromOpenElisServer("/rest/SampleEntryGenerateScanProvider", (res) => {
      setDefaultForm((prev) => ({ ...prev, labNo: res?.body || "" }));
      setLabNoLoading(false);
    });
  };

  const updateDefaultField = (key, value) => {
    setDefaultForm((prev) => ({ ...prev, [key]: value }));
  };

  // Handler for FHIR questionnaire answers
  const handleAnswerChange = (e) => {
    const { id, value } = e.target;

    // Handle multi-select values - extract just the value field if it's an array of objects
    let processedValue = value;
    if (Array.isArray(value)) {
      // Check if it's an array of objects with value property (from FilterableMultiSelect)
      if (
        value.length > 0 &&
        typeof value[0] === "object" &&
        "value" in value[0]
      ) {
        processedValue = value.map((item) => item.value);
      }
    }

    setFhirResponses((prev) => ({ ...prev, [id]: processedValue }));
  };

  // Get answer for FHIR questionnaire
  const getAnswer = (questionId) => {
    return fhirResponses[questionId] || "";
  };

  const onSubmit = (e) => {
    e.preventDefault();

    const submissionData = {
      defaultFields: defaultForm,
      notebookId: selectedNotebookId ? parseInt(selectedNotebookId) : null,
      fhirQuestionnaire: fhirQuestionnaire,
      fhirResponses: fhirResponses,
    };

    // Post to backend
    postToOpenElisServerJsonResponse(
      "/rest/GenericSampleOrder",
      JSON.stringify(submissionData),
      (data) => {
        if (data && data.success) {
          alert(
            "Sample order saved successfully! Accession Number: " +
              (data.accessionNumber || ""),
          );
          // Optionally redirect or reset form
          // window.location.href = "/";
        } else {
          alert(
            "Error saving sample order: " + (data?.error || "Unknown error"),
          );
        }
      },
    );
  };

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              <FormattedMessage
                id="genericSample.order.title"
                defaultMessage="Generic Sample - Order"
              />
            </Heading>
          </Section>
        </Column>
      </Grid>

      <form onSubmit={onSubmit}>
        {/* NOTEBOOK SELECTION SECTION */}
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                <FormattedMessage
                  id="genericSample.notebook.selection.title"
                  defaultMessage="Notebook Selection (Optional)"
                />
              </Heading>
            </Section>
          </Column>
        </Grid>

        <Grid fullWidth={true}>
          <Column lg={8} md={8} sm={4}>
            <CustomSelect
              id="notebookSelect"
              labelText={
                <FormattedMessage
                  id="notebook.select.label"
                  defaultMessage="Select Notebook"
                />
              }
              value={selectedNotebookId || ""}
              onChange={(value) => setSelectedNotebookId(value)}
              options={[
                { id: "", value: "None - Default Fields Only" },
                ...notebooks.map((notebook) => ({
                  id: notebook.id,
                  value: notebook.title,
                })),
              ]}
              placeholder="Select a notebook"
            />
          </Column>
        </Grid>

        {/* DEFAULT FIELDS SECTION */}
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                <FormattedMessage
                  id="genericSample.default.fields.title"
                  defaultMessage="Sample Information"
                />
              </Heading>
            </Section>
          </Column>
        </Grid>

        {/* Row 1: Lab number, Sample Type */}
        <Grid fullWidth={true}>
          <Column lg={8} md={8} sm={4}>
            <CustomLabNumberInput
              id="labNo"
              name="labNo"
              labelText={
                <FormattedMessage
                  id="sample.label.labnumber"
                  defaultMessage="Lab Number"
                />
              }
              value={defaultForm.labNo}
              readOnly
            />
            <Button
              type="button"
              style={{ marginTop: 10 }}
              onClick={handleLabNoGeneration}
              disabled={labNoLoading}
              size="sm"
            >
              {labNoLoading ? "Generating..." : "Regenerate Lab Number"}
            </Button>
          </Column>
          <Column lg={8} md={8} sm={4}>
            <CustomSelect
              id="sampleType"
              labelText={
                <FormattedMessage
                  id="sample.type"
                  defaultMessage="Sample Type"
                />
              }
              value={defaultForm.sampleTypeId}
              onChange={(v) => updateDefaultField("sampleTypeId", v)}
              options={sampleTypes.map((s) => ({ id: s.id, value: s.value }))}
              placeholder="Select sample type"
            />
          </Column>
        </Grid>

        {/* Row 2: Quantity, Sample Unit Of Measure */}
        <Grid fullWidth={true}>
          <Column lg={8} md={8} sm={4}>
            <TextInput
              id="quantity"
              labelText={
                <FormattedMessage
                  id="sample.quantity.label"
                  defaultMessage="Quantity"
                />
              }
              type="number"
              value={defaultForm.quantity}
              onChange={(e) => updateDefaultField("quantity", e.target.value)}
            />
          </Column>
          <Column lg={8} md={8} sm={4}>
            <CustomSelect
              id="sampleUnitOfMeasure"
              labelText={
                <FormattedMessage
                  id="sample.uom.label"
                  defaultMessage="Sample Unit Of Measure"
                />
              }
              value={defaultForm.sampleUnitOfMeasure}
              onChange={(v) => updateDefaultField("sampleUnitOfMeasure", v)}
              options={uoms.map((u) => ({ id: u.id, value: u.value }))}
              placeholder="Select units"
            />
          </Column>
        </Grid>

        {/* Row 3: From, Collector */}
        <Grid fullWidth={true}>
          <Column lg={8} md={8} sm={4}>
            <TextInput
              id="from"
              labelText={
                <FormattedMessage
                  id="genericSample.field.from"
                  defaultMessage="From"
                />
              }
              value={defaultForm.from}
              onChange={(e) => updateDefaultField("from", e.target.value)}
            />
          </Column>
          <Column lg={8} md={8} sm={4}>
            <TextInput
              id="collector"
              labelText={
                <FormattedMessage
                  id="collector.label"
                  defaultMessage="Collector"
                />
              }
              value={defaultForm.collector}
              onChange={(e) => updateDefaultField("collector", e.target.value)}
            />
          </Column>
        </Grid>

        {/* Row 4: Collection date, Collection time */}
        <Grid fullWidth={true}>
          <Column lg={8} md={8} sm={4}>
            <CustomDatePicker
              id="collectionDate"
              labelText={
                <FormattedMessage
                  id="sample.collection.date"
                  defaultMessage="Collection Date"
                />
              }
              value={defaultForm.collectionDate}
              onChange={(v) => updateDefaultField("collectionDate", v)}
            />
          </Column>
          <Column lg={8} md={8} sm={4}>
            <CustomTimePicker
              id="collectionTime"
              labelText={
                <FormattedMessage
                  id="sample.collection.time"
                  defaultMessage="Collection Time"
                />
              }
              value={defaultForm.collectionTime}
              onChange={(v) => updateDefaultField("collectionTime", v)}
            />
          </Column>
        </Grid>

        {/* FHIR QUESTIONNAIRE SECTION */}
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                <FormattedMessage
                  id="fhir.questionnaire.title"
                  defaultMessage="Additional Information"
                />
              </Heading>
            </Section>

            {questionnaireLoading ? (
              <div>Loading questionnaire...</div>
            ) : (
              <Questionnaire
                questionnaire={fhirQuestionnaire}
                onAnswerChange={handleAnswerChange}
                getAnswer={getAnswer}
              />
            )}
          </Column>
        </Grid>

        {/* Action buttons */}
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <div style={{ display: "flex", gap: "0.75rem" }}>
              <Button type="submit">
                <FormattedMessage id="button.save" defaultMessage="Save" />
              </Button>
              <Button
                kind="secondary"
                type="button"
                onClick={() => window.history.back()}
              >
                <FormattedMessage id="button.cancel" defaultMessage="Cancel" />
              </Button>
            </div>
          </Column>
        </Grid>
      </form>
    </>
  );
}
