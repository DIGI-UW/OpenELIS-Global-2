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
import Questionnaire from "../common/Questionnaire";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";
import config from "../../config.json";

/**
 * GenericSampleOrder - Configurable sample order entry component
 *
 * @param {Object} props - Component configuration
 * @param {string} props.title - Page title (i18n key or string)
 * @param {string} props.titleDefault - Default page title
 * @param {Array} props.breadcrumbs - Custom breadcrumb array [{label, link}]
 * @param {string} props.saveEndpoint - API endpoint for saving (default: "/rest/GenericSampleOrder")
 * @param {boolean} props.showNotebookSelection - Show notebook selection (default: true)
 * @param {boolean} props.showLabNoGenerate - Show lab number generate button (default: true)
 * @param {boolean} props.labNoRequired - Lab number required before save (default: true)
 * @param {boolean} props.showSampleType - Show sample type field (default: true)
 * @param {boolean} props.showQuantity - Show quantity field (default: true)
 * @param {boolean} props.showUom - Show unit of measure field (default: true)
 * @param {boolean} props.showFrom - Show from field (default: true)
 * @param {boolean} props.showCollector - Show collector field (default: true)
 * @param {boolean} props.showCollectionDate - Show collection date field (default: true)
 * @param {boolean} props.showCollectionTime - Show collection time field (default: true)
 * @param {boolean} props.showQuestionnaire - Show FHIR questionnaire section (default: true)
 * @param {boolean} props.showBreadcrumbs - Show breadcrumbs (default: true)
 * @param {boolean} props.showSuccessScreen - Show success screen after save (default: true)
 * @param {Function} props.onSaveSuccess - Callback after successful save (data) => void
 * @param {Function} props.onSaveError - Callback after save error (error) => void
 * @param {Object} props.initialValues - Initial form values
 * @param {Array} props.additionalFields - Additional custom fields to render [{id, labelText, type, options}]
 * @param {Function} props.renderCustomContent - Render function for custom content (formData, updateField) => React.Node
 */
export default function GenericSampleOrder({
  title = "genericSample.order.title",
  titleDefault = "Generic Sample - Order",
  breadcrumbs: customBreadcrumbs,
  saveEndpoint = "/rest/GenericSampleOrder",
  showNotebookSelection = true,
  showLabNoGenerate = true,
  labNoRequired = true,
  showSampleType = true,
  showQuantity = true,
  showUom = true,
  showFrom = true,
  showCollector = true,
  showCollectionDate = true,
  showCollectionTime = true,
  showQuestionnaire = true,
  showBreadcrumbs = true,
  showSuccessScreen = true,
  onSaveSuccess,
  onSaveError,
  initialValues = {},
  additionalFields = [],
  renderCustomContent,
}) {
  const intl = useIntl();

  // Extract notebook IDs from initialValues (these should NOT be in defaultForm)
  // Use useMemo to ensure these values update when initialValues changes
  const initialNotebookId = React.useMemo(
    () => initialValues.notebookId || null,
    [initialValues.notebookId],
  );
  const initialNotebookEntryId = React.useMemo(
    () => initialValues.notebookEntryId || null,
    [initialValues.notebookEntryId],
  );

  // Default fields - only include standard fields that the backend expects
  const [defaultForm, setDefaultForm] = useState({
    labNo: initialValues.labNo || "",
    sampleTypeId: initialValues.sampleTypeId || "",
    quantity: initialValues.quantity || "",
    sampleUnitOfMeasure: initialValues.sampleUnitOfMeasure || "",
    from: initialValues.from || "",
    collector: initialValues.collector || "",
    collectionDate: initialValues.collectionDate || "",
    collectionTime: initialValues.collectionTime || "",
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

  // Success state
  const [successData, setSuccessData] = useState(null);

  // Default breadcrumbs
  const defaultBreadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "menu.genericSample" },
    { label: "menu.genericSample.order" },
  ];

  const breadcrumbs = customBreadcrumbs || defaultBreadcrumbs;

  // Load default data and notebooks
  useEffect(() => {
    if (showSampleType) {
      getFromOpenElisServer("/rest/user-sample-types", (res) => {
        setSampleTypes(res || []);
      });
    }
    if (showUom) {
      getFromOpenElisServer("/rest/UomCreate", (res) => {
        setUoms(res.existingUomList || []);
      });
    }
    if (showNotebookSelection) {
      getFromOpenElisServer("/rest/notebook/list", (res) => {
        setNotebooks(Array.isArray(res) ? res : []);
      });
    }
  }, [showSampleType, showUom, showNotebookSelection]);

  // Load FHIR Questionnaire when notebook is selected (via dropdown or via initialNotebookId)
  useEffect(() => {
    const effectiveNotebookIdForQuestionnaire =
      selectedNotebookId || initialNotebookId;

    if (effectiveNotebookIdForQuestionnaire && showQuestionnaire) {
      loadFhirQuestionnaireForNotebook(effectiveNotebookIdForQuestionnaire);
    } else {
      setFhirQuestionnaire(null);
      setFhirResponses({});
    }
  }, [selectedNotebookId, initialNotebookId, showQuestionnaire]);

  const loadFhirQuestionnaireForNotebook = (notebookId) => {
    setQuestionnaireLoading(true);

    // If we have the notebooks list loaded, try to find the questionnaire UUID from it
    const notebook = notebooks.find((n) => n.id === parseInt(notebookId));
    if (notebook && notebook.questionnaireFhirUuid) {
      getFromOpenElisServer(
        "/rest/fhir/Questionnaire/" + notebook.questionnaireFhirUuid,
        (res) => {
          setFhirQuestionnaire(res || null);
          setQuestionnaireLoading(false);
        },
      );
    } else if (!showNotebookSelection && initialNotebookId) {
      // If notebook selection is hidden and we have an initialNotebookId,
      // fetch the notebook details directly to get the questionnaire
      getFromOpenElisServer(
        `/rest/notebook/view/${notebookId}`,
        (notebookData) => {
          if (notebookData && notebookData.questionnaireFhirUuid) {
            getFromOpenElisServer(
              "/rest/fhir/Questionnaire/" + notebookData.questionnaireFhirUuid,
              (res) => {
                setFhirQuestionnaire(res || null);
                setQuestionnaireLoading(false);
              },
            );
          } else {
            setFhirQuestionnaire(null);
            setQuestionnaireLoading(false);
          }
        },
      );
    } else {
      setFhirQuestionnaire(null);
      setQuestionnaireLoading(false);
    }
  };

  const handleNotebookChange = (notebookId) => {
    setSelectedNotebookId(notebookId);
  };

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

  const handleAnswerChange = (e) => {
    const { id, value } = e.target;
    let processedValue = value;
    if (Array.isArray(value)) {
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

  const getAnswer = (questionId) => {
    return fhirResponses[questionId] || "";
  };

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
  };

  const onSubmit = (e) => {
    e.preventDefault();

    if (
      labNoRequired &&
      (!defaultForm.labNo || defaultForm.labNo.trim() === "")
    ) {
      alert(
        intl.formatMessage({
          id: "genericSample.order.error.labNoRequired",
          defaultMessage: "Please generate a Lab Number before saving.",
        }),
      );
      return;
    }

    // Use selected notebook ID from dropdown, or fall back to initial notebook ID from props
    const effectiveNotebookId = selectedNotebookId
      ? parseInt(selectedNotebookId)
      : initialNotebookId
        ? parseInt(initialNotebookId)
        : null;

    const submissionData = {
      defaultFields: defaultForm,
      notebookId: effectiveNotebookId,
      notebookEntryId: initialNotebookEntryId
        ? parseInt(initialNotebookEntryId)
        : null,
      fhirQuestionnaire: fhirQuestionnaire,
      fhirResponses: fhirResponses,
    };

    const selectedSampleType = sampleTypes.find(
      (s) => s.id === defaultForm.sampleTypeId,
    );
    const selectedUom = uoms.find(
      (u) => u.id === defaultForm.sampleUnitOfMeasure,
    );

    postToOpenElisServerJsonResponse(
      saveEndpoint,
      JSON.stringify(submissionData),
      (data) => {
        if (data && data.success) {
          const resultData = {
            accessionNumber: data.accessionNumber || defaultForm.labNo,
            sampleType: selectedSampleType?.value || "",
            quantity: defaultForm.quantity,
            unitOfMeasure: selectedUom?.value || "",
            from: defaultForm.from,
            collector: defaultForm.collector,
            collectionDate: defaultForm.collectionDate,
            collectionTime: defaultForm.collectionTime,
          };

          if (onSaveSuccess) {
            onSaveSuccess(resultData);
          }

          if (showSuccessScreen) {
            setSuccessData(resultData);
          }
        } else {
          const errorMsg =
            intl.formatMessage({ id: "error.save.sample" }) +
            ": " +
            (data?.error || "Unknown error");

          if (onSaveError) {
            onSaveError(data?.error || "Unknown error");
          } else {
            alert(errorMsg);
          }
        }
      },
    );
  };

  // Success screen
  if (successData && showSuccessScreen) {
    return (
      <>
        {showBreadcrumbs && <PageBreadCrumb breadcrumbs={breadcrumbs} />}
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                <FormattedMessage id={title} defaultMessage={titleDefault} />
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
      {showBreadcrumbs && <PageBreadCrumb breadcrumbs={breadcrumbs} />}
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              <FormattedMessage id={title} defaultMessage={titleDefault} />
            </Heading>
          </Section>
        </Column>
      </Grid>

      <div className="orderLegendBody">
        <form onSubmit={onSubmit}>
          {/* NOTEBOOK SELECTION SECTION */}
          {showNotebookSelection && (
            <>
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
            </>
          )}

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
                    {labNoRequired && <span style={{ color: "red" }}> *</span>}
                  </>
                }
                value={defaultForm.labNo}
                readOnly
                placeholder={intl.formatMessage({
                  id: "genericSample.order.labNo.placeholder",
                  defaultMessage: "Click 'Generate Lab Number' to create",
                })}
              />
              {showLabNoGenerate && (
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
              )}
            </Column>
            {showSampleType && (
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
                  options={sampleTypes.map((s) => ({
                    id: s.id,
                    value: s.value,
                  }))}
                  placeholder="Select sample type"
                />
              </Column>
            )}
          </Grid>

          {/* Row 2: Quantity, Sample Unit Of Measure */}
          {(showQuantity || showUom) && (
            <Grid fullWidth={true}>
              {showQuantity && (
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
                    onChange={(e) =>
                      updateDefaultField("quantity", e.target.value)
                    }
                  />
                </Column>
              )}
              {showUom && (
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
                    onChange={(v) =>
                      updateDefaultField("sampleUnitOfMeasure", v)
                    }
                    options={uoms.map((u) => ({ id: u.id, value: u.value }))}
                    placeholder="Select units"
                  />
                </Column>
              )}
            </Grid>
          )}

          {/* Row 3: From, Collector */}
          {(showFrom || showCollector) && (
            <Grid fullWidth={true}>
              {showFrom && (
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
              )}
              {showCollector && (
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
                    onChange={(e) =>
                      updateDefaultField("collector", e.target.value)
                    }
                  />
                </Column>
              )}
            </Grid>
          )}

          {/* Row 4: Collection date, Collection time */}
          {(showCollectionDate || showCollectionTime) && (
            <Grid fullWidth={true}>
              {showCollectionDate && (
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
              )}
              {showCollectionTime && (
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
              )}
            </Grid>
          )}

          {/* Additional custom fields */}
          {additionalFields.length > 0 && (
            <Grid fullWidth={true}>
              {additionalFields.map((field) => (
                <Column key={field.id} lg={8} md={8} sm={4}>
                  {field.type === "select" ? (
                    <CustomSelect
                      id={field.id}
                      labelText={field.labelText}
                      value={defaultForm[field.id] || ""}
                      onChange={(v) => updateDefaultField(field.id, v)}
                      options={field.options || []}
                      placeholder={field.placeholder}
                    />
                  ) : (
                    <TextInput
                      id={field.id}
                      labelText={field.labelText}
                      type={field.type || "text"}
                      value={defaultForm[field.id] || ""}
                      onChange={(e) =>
                        updateDefaultField(field.id, e.target.value)
                      }
                    />
                  )}
                </Column>
              ))}
            </Grid>
          )}

          {/* Custom content render */}
          {renderCustomContent &&
            renderCustomContent(defaultForm, updateDefaultField)}

          {/* FHIR QUESTIONNAIRE SECTION */}
          {showQuestionnaire && (
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
          )}

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
                  <FormattedMessage
                    id="button.cancel"
                    defaultMessage="Cancel"
                  />
                </Button>
              </div>
            </Column>
          </Grid>
        </form>
      </div>
    </>
  );
}
