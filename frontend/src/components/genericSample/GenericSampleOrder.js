import React, { useEffect, useState } from "react";
import {
  Grid,
  Column,
  Section,
  Heading,
  TextInput,
  Button,
  InlineNotification,
} from "@carbon/react";
import { Printer } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
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
import config from "../../config.json";

export default function GenericSampleOrder() {
  const intl = useIntl();

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

  // Success state for showing success message with print barcode option
  const [successData, setSuccessData] = useState(null);

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

    // Note: Lab number is NOT auto-generated - user must click "Generate Lab Number" button
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

  /**
   * Handle printing barcode for the created sample.
   * Opens the LabelMakerServlet in a new window to generate and print the barcode PDF.
   * For generic samples, passes sample type, quantity, and from fields to display on label.
   */
  const handlePrintBarCode = (sampleData) => {
    const params = new URLSearchParams({
      labNo: sampleData.accessionNumber,
      type: "generic",
      quantity: "1",
      sampleType: sampleData.sampleType || "",
      sampleQuantity: sampleData.quantity
        ? `${sampleData.quantity}${sampleData.unitOfMeasure ? " " + sampleData.unitOfMeasure : ""}`
        : "",
      from: sampleData.from || "",
    });
    const barcodesPdf =
      config.serverBaseUrl + `/LabelMakerServlet?${params.toString()}`;
    window.open(barcodesPdf);
  };

  /**
   * Reset form and start a new sample order.
   */
  const handleNewOrder = () => {
    setSuccessData(null);
    setDefaultForm({
      labNo: "",
      sampleTypeId: "",
      quantity: "",
      sampleUnitOfMeasure: "",
      from: "",
      collector: "",
      collectionDate: "",
      collectionTime: "",
    });
    setFhirResponses({});
    setSelectedNotebookId(null);
    // Note: Lab number is NOT auto-generated - user must click "Generate Lab Number" button
  };

  const onSubmit = (e) => {
    e.preventDefault();

    // Validate that lab number has been generated
    if (!defaultForm.labNo || defaultForm.labNo.trim() === "") {
      alert(
        intl.formatMessage({
          id: "genericSample.order.error.labNoRequired",
          defaultMessage: "Please generate a Lab Number before saving.",
        }),
      );
      return;
    }

    const submissionData = {
      defaultFields: defaultForm,
      notebookId: selectedNotebookId ? parseInt(selectedNotebookId) : null,
      fhirQuestionnaire: fhirQuestionnaire,
      fhirResponses: fhirResponses,
    };

    // Find the sample type name for display
    const selectedSampleType = sampleTypes.find(
      (s) => s.id === defaultForm.sampleTypeId,
    );
    const selectedUom = uoms.find(
      (u) => u.id === defaultForm.sampleUnitOfMeasure,
    );

    // Post to backend
    postToOpenElisServerJsonResponse(
      "/rest/GenericSampleOrder",
      JSON.stringify(submissionData),
      (data) => {
        if (data && data.success) {
          // Show success state with print barcode option and sample details
          setSuccessData({
            accessionNumber: data.accessionNumber || defaultForm.labNo,
            sampleType: selectedSampleType?.value || "",
            quantity: defaultForm.quantity,
            unitOfMeasure: selectedUom?.value || "",
            from: defaultForm.from,
            collector: defaultForm.collector,
            collectionDate: defaultForm.collectionDate,
            collectionTime: defaultForm.collectionTime,
          });
        } else {
          alert(
            intl.formatMessage({ id: "error.save.sample" }) +
              ": " +
              (data?.error || "Unknown error"),
          );
        }
      },
    );
  };

  // If sample was successfully created, show success message with print option
  if (successData) {
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

        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <div
              style={{
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                padding: "2rem",
                marginTop: "2rem",
              }}
            >
              <InlineNotification
                kind="success"
                title={intl.formatMessage({ id: "save.success" })}
                subtitle={intl.formatMessage(
                  { id: "genericSample.order.success.message" },
                  { accessionNumber: successData.accessionNumber },
                )}
                lowContrast
                hideCloseButton
                style={{ maxWidth: "600px", marginBottom: "1.5rem" }}
              />

              {/* Sample Details Summary */}
              <div
                style={{
                  backgroundColor: "#f4f4f4",
                  padding: "1.5rem",
                  borderRadius: "4px",
                  width: "100%",
                  maxWidth: "600px",
                  marginBottom: "1.5rem",
                }}
              >
                <h4 style={{ marginBottom: "1rem" }}>
                  <FormattedMessage
                    id="genericSample.order.success.details"
                    defaultMessage="Sample Details"
                  />
                </h4>
                <div
                  style={{
                    display: "grid",
                    gridTemplateColumns: "1fr 1fr",
                    gap: "0.75rem",
                  }}
                >
                  <div>
                    <strong>
                      <FormattedMessage id="sample.label.labnumber" />:
                    </strong>
                  </div>
                  <div>{successData.accessionNumber}</div>

                  {successData.sampleType && (
                    <>
                      <div>
                        <strong>
                          <FormattedMessage id="sample.type" />:
                        </strong>
                      </div>
                      <div>{successData.sampleType}</div>
                    </>
                  )}

                  {successData.quantity && (
                    <>
                      <div>
                        <strong>
                          <FormattedMessage id="sample.quantity.label" />:
                        </strong>
                      </div>
                      <div>
                        {successData.quantity}
                        {successData.unitOfMeasure &&
                          ` ${successData.unitOfMeasure}`}
                      </div>
                    </>
                  )}

                  {successData.from && (
                    <>
                      <div>
                        <strong>
                          <FormattedMessage id="genericSample.field.from" />:
                        </strong>
                      </div>
                      <div>{successData.from}</div>
                    </>
                  )}

                  {successData.collector && (
                    <>
                      <div>
                        <strong>
                          <FormattedMessage id="collector.label" />:
                        </strong>
                      </div>
                      <div>{successData.collector}</div>
                    </>
                  )}

                  {successData.collectionDate && (
                    <>
                      <div>
                        <strong>
                          <FormattedMessage id="sample.collection.date" />:
                        </strong>
                      </div>
                      <div>
                        {successData.collectionDate}
                        {successData.collectionTime &&
                          ` ${successData.collectionTime}`}
                      </div>
                    </>
                  )}
                </div>
              </div>

              <div
                style={{ display: "flex", gap: "1rem", marginTop: "0.5rem" }}
              >
                <Button
                  kind="primary"
                  renderIcon={Printer}
                  onClick={() => handlePrintBarCode(successData)}
                >
                  <FormattedMessage id="print.barcode" />
                </Button>
                <Button kind="secondary" onClick={handleNewOrder}>
                  <FormattedMessage
                    id="genericSample.order.newOrder"
                    defaultMessage="Create Another Sample"
                  />
                </Button>
                <Button
                  kind="tertiary"
                  onClick={() => (window.location.href = "/")}
                >
                  <FormattedMessage id="button.home" defaultMessage="Home" />
                </Button>
              </div>
            </div>
          </Column>
        </Grid>
      </>
    );
  }

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
                <>
                  <FormattedMessage
                    id="sample.label.labnumber"
                    defaultMessage="Lab Number"
                  />
                  <span style={{ color: "red" }}> *</span>
                </>
              }
              value={defaultForm.labNo}
              readOnly
              placeholder={intl.formatMessage({
                id: "genericSample.order.labNo.placeholder",
                defaultMessage: "Click 'Generate Lab Number' to create",
              })}
            />
            <Button
              type="button"
              kind={defaultForm.labNo ? "tertiary" : "primary"}
              style={{ marginTop: 10 }}
              onClick={handleLabNoGeneration}
              disabled={labNoLoading}
              size="sm"
            >
              {labNoLoading
                ? intl.formatMessage({
                    id: "generating",
                    defaultMessage: "Generating...",
                  })
                : intl.formatMessage({
                    id: "genericSample.order.generateLabNo",
                    defaultMessage: "Generate Lab Number",
                  })}
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
