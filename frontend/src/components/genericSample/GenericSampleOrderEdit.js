import React, { useEffect, useState } from "react";
import {
  Grid,
  Column,
  Section,
  Heading,
  TextInput,
  Button,
  InlineLoading,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
import CustomDatePicker from "../common/CustomDatePicker";
import CustomTimePicker from "../common/CustomTimePicker";
import CustomSelect from "../common/CustomSelect";
import CustomLabNumberInput from "../common/CustomLabNumberInput";
import Questionnaire from "../common/Questionnaire";
import { getFromOpenElisServer } from "../utils/Utils";
import config from "../../config.json";

/**
 * GenericSampleOrderEdit - Configurable sample order edit component
 *
 * @param {Object} props - Component configuration
 * @param {string} props.title - Page title (i18n key)
 * @param {string} props.titleDefault - Default page title
 * @param {Array} props.breadcrumbs - Custom breadcrumb array [{label, link}]
 * @param {string} props.searchEndpoint - API endpoint for search (default: "/rest/GenericSampleOrder")
 * @param {string} props.saveEndpoint - API endpoint for saving (default: "/rest/GenericSampleOrder")
 * @param {boolean} props.showBreadcrumbs - Show breadcrumbs (default: true)
 * @param {boolean} props.showNotebookSelection - Show notebook selection (default: true)
 * @param {boolean} props.showSampleType - Show sample type field (default: true)
 * @param {boolean} props.showQuantity - Show quantity field (default: true)
 * @param {boolean} props.showUom - Show unit of measure field (default: true)
 * @param {boolean} props.showFrom - Show from field (default: true)
 * @param {boolean} props.showCollector - Show collector field (default: true)
 * @param {boolean} props.showCollectionDate - Show collection date field (default: true)
 * @param {boolean} props.showCollectionTime - Show collection time field (default: true)
 * @param {boolean} props.showQuestionnaire - Show FHIR questionnaire section (default: true)
 * @param {Function} props.onSearchComplete - Callback after search completes (data) => void
 * @param {Function} props.onSaveSuccess - Callback after successful save (data) => void
 * @param {Function} props.onSaveError - Callback after save error (error) => void
 * @param {Array} props.additionalFields - Additional custom fields to render [{id, labelText, type, options}]
 * @param {Function} props.renderCustomContent - Render function for custom content (formData, updateField) => React.Node
 */
export default function GenericSampleOrderEdit({
  title = "genericSample.edit.title",
  titleDefault = "Generic Sample - Edit Order",
  breadcrumbs: customBreadcrumbs,
  searchEndpoint = "/rest/GenericSampleOrder",
  saveEndpoint = "/rest/GenericSampleOrder",
  showBreadcrumbs = true,
  showNotebookSelection = true,
  showSampleType = true,
  showQuantity = true,
  showUom = true,
  showFrom = true,
  showCollector = true,
  showCollectionDate = true,
  showCollectionTime = true,
  showQuestionnaire = true,
  onSearchComplete,
  onSaveSuccess,
  onSaveError,
  additionalFields = [],
  renderCustomContent,
}) {
  const intl = useIntl();
  // Search state
  const [searchAccessionNumber, setSearchAccessionNumber] = useState("");
  const [searching, setSearching] = useState(false);
  const [orderFound, setOrderFound] = useState(false);
  const [searchError, setSearchError] = useState("");

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

  // Notebook state
  const [notebooks, setNotebooks] = useState([]);
  const [selectedNotebookId, setSelectedNotebookId] = useState(null);

  // Dropdown lists
  const [sampleTypes, setSampleTypes] = useState([]);
  const [uoms, setUoms] = useState([]);
  const [saving, setSaving] = useState(false);

  // Breadcrumbs
  const defaultBreadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "menu.genericSample" },
    { label: "menu.genericSample.edit" },
  ];

  const breadcrumbs = customBreadcrumbs || defaultBreadcrumbs;

  // Load default data
  useEffect(() => {
    // Load default dropdown data conditionally
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
        setNotebooks(res || []);
      });
    }

    // Check if accession number is in URL params
    const urlParams = new URLSearchParams(window.location.search);
    const accessionNumberParam = urlParams.get("accessionNumber");
    if (accessionNumberParam) {
      setSearchAccessionNumber(accessionNumberParam);
      handleSearch(accessionNumberParam);
    }
  }, [showSampleType, showUom, showNotebookSelection]);

  // Load questionnaire when notebook is selected (but not on initial load from backend)
  const [notebookChangedByUser, setNotebookChangedByUser] = useState(false);

  useEffect(() => {
    if (!showQuestionnaire) return;

    if (selectedNotebookId && notebookChangedByUser) {
      // User manually changed the notebook, so clear responses and load new questionnaire
      setFhirResponses({});
      loadFhirQuestionnaireForNotebook(selectedNotebookId);
    } else if (!selectedNotebookId) {
      // No notebook selected, clear questionnaire
      setFhirQuestionnaire(null);
      setFhirResponses({});
    }
    // When notebook loaded from backend (!notebookChangedByUser), do NOT reload
    // the questionnaire - it was already set from the search response
  }, [selectedNotebookId, notebookChangedByUser, showQuestionnaire]);

  const loadFhirQuestionnaireForNotebook = (notebookId) => {
    setQuestionnaireLoading(true);
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

  const handleSearch = (accessionNumber) => {
    if (!accessionNumber || accessionNumber.trim() === "") {
      setSearchError("Please enter an accession number");
      return;
    }

    setSearching(true);
    setSearchError("");
    setOrderFound(false);

    // Extract base accession number (remove any suffix like "-1")
    const baseAccessionNumber = accessionNumber.split("-")[0];

    getFromOpenElisServer(
      `${searchEndpoint}?accessionNumber=${encodeURIComponent(baseAccessionNumber)}`,
      (data) => {
        setSearching(false);
        if (data && data.defaultFields && data.defaultFields.labNo) {
          // Populate form with retrieved data
          setDefaultForm({
            labNo: data.defaultFields.labNo || "",
            sampleTypeId: data.defaultFields.sampleTypeId || "",
            quantity: data.defaultFields.quantity || "",
            sampleUnitOfMeasure: data.defaultFields.sampleUnitOfMeasure || "",
            from: data.defaultFields.from || "",
            collector: data.defaultFields.collector || "",
            collectionDate: data.defaultFields.collectionDate || "",
            collectionTime: data.defaultFields.collectionTime || "",
          });

          // Set notebook if available (reset the changed flag since this is from backend)
          if (data.notebookId) {
            setNotebookChangedByUser(false);
            setSelectedNotebookId(data.notebookId);
          }

          // Set questionnaire and responses if available
          if (data.fhirQuestionnaire) {
            setFhirQuestionnaire(data.fhirQuestionnaire);
            setQuestionnaireLoading(false);
          }

          if (data.fhirResponses) {
            setFhirResponses(data.fhirResponses || {});
          }

          setOrderFound(true);
          setSearchAccessionNumber(baseAccessionNumber);

          // Call callback if provided
          if (onSearchComplete) {
            onSearchComplete(data);
          }
        } else {
          setSearchError(
            data?.error || "No sample found with this accession number",
          );
          setOrderFound(false);
        }
      },
    );
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

    if (!orderFound || !searchAccessionNumber) {
      alert("Please search for an order first");
      return;
    }

    setSaving(true);

    const submissionData = {
      defaultFields: defaultForm,
      fhirQuestionnaire: fhirQuestionnaire,
      fhirResponses: fhirResponses,
      notebookId: selectedNotebookId,
    };

    // Put to backend - use fetch directly for JSON response
    const options = {
      credentials: "include",
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF"),
      },
      body: JSON.stringify(submissionData),
    };

    fetch(
      config.serverBaseUrl +
        `${saveEndpoint}/${encodeURIComponent(searchAccessionNumber)}`,
      options,
    )
      .then((response) => response.json())
      .then((data) => {
        setSaving(false);
        if (data && data.success) {
          if (onSaveSuccess) {
            onSaveSuccess(data);
          } else {
            alert(
              intl.formatMessage(
                { id: "genericSample.edit.success" },
                {
                  accessionNumber:
                    data.accessionNumber || searchAccessionNumber,
                },
              ),
            );
          }
          // Optionally reload the data
          handleSearch(searchAccessionNumber);
        } else {
          const errorMsg = data?.error || "Unknown error";
          if (onSaveError) {
            onSaveError(errorMsg);
          } else {
            alert(
              intl.formatMessage({ id: "error.save.sample" }) + ": " + errorMsg,
            );
          }
        }
      })
      .catch((error) => {
        setSaving(false);
        const errorMsg = error.message || "Unknown error";
        if (onSaveError) {
          onSaveError(errorMsg);
        } else {
          alert(
            intl.formatMessage({ id: "error.save.sample" }) + ": " + errorMsg,
          );
        }
      });
  };

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    handleSearch(searchAccessionNumber);
  };

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
        {/* Search Section */}
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                <FormattedMessage
                  id="genericSample.search.title"
                  defaultMessage="Search by Accession Number"
                />
              </Heading>
              <form onSubmit={handleSearchSubmit}>
                <Grid fullWidth={true}>
                  <Column lg={12} md={6} sm={4}>
                    <CustomLabNumberInput
                      id="searchAccessionNumber"
                      name="searchAccessionNumber"
                      labelText={
                        <FormattedMessage
                          id="search.label.accession"
                          defaultMessage="Accession Number"
                        />
                      }
                      value={searchAccessionNumber}
                      onChange={(e, rawVal) =>
                        setSearchAccessionNumber(
                          rawVal ? rawVal : e?.target?.value,
                        )
                      }
                      placeholder="Enter accession number"
                    />
                  </Column>
                  <Column lg={4} md={2} sm={4}>
                    <div
                      style={{
                        display: "flex",
                        alignItems: "flex-end",
                        height: "100%",
                      }}
                    >
                      <Button type="submit" disabled={searching}>
                        {searching ? (
                          <InlineLoading description="Searching..." />
                        ) : (
                          <FormattedMessage
                            id="label.button.search"
                            defaultMessage="Search"
                          />
                        )}
                      </Button>
                    </div>
                  </Column>
                </Grid>
                {searchError && (
                  <Grid fullWidth={true}>
                    <Column lg={16} md={8} sm={4}>
                      <div style={{ color: "red", marginTop: "0.5rem" }}>
                        {searchError}
                      </div>
                    </Column>
                  </Grid>
                )}
              </form>
            </Section>
          </Column>
        </Grid>

        {/* Edit Form Section - Only show if order found */}
        {orderFound && (
          <form onSubmit={onSubmit}>
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
                      onChange={(e) =>
                        updateDefaultField("from", e.target.value)
                      }
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

            {/* Row 5: Notebook Selection */}
            {showNotebookSelection && (
              <Grid fullWidth={true}>
                <Column lg={8} md={8} sm={4}>
                  <CustomSelect
                    id="notebookSelect"
                    labelText={
                      <FormattedMessage
                        id="genericSample.notebook.label"
                        defaultMessage="Select Notebook"
                      />
                    }
                    value={selectedNotebookId || ""}
                    onChange={(v) => {
                      setNotebookChangedByUser(true);
                      setSelectedNotebookId(v);
                    }}
                    options={notebooks.map((n) => ({
                      id: n.id,
                      value: n.title,
                    }))}
                    placeholder="Select a notebook (optional)"
                  />
                </Column>
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
                  <Button type="submit" disabled={saving}>
                    {saving ? (
                      <InlineLoading description="Saving..." />
                    ) : (
                      <FormattedMessage
                        id="button.save"
                        defaultMessage="Save"
                      />
                    )}
                  </Button>
                  <Button
                    kind="secondary"
                    type="button"
                    onClick={() => {
                      setOrderFound(false);
                      setSearchAccessionNumber("");
                      setSearchError("");
                    }}
                  >
                    <FormattedMessage
                      id="button.new.search"
                      defaultMessage="New Search"
                    />
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
        )}
      </div>
    </>
  );
}
