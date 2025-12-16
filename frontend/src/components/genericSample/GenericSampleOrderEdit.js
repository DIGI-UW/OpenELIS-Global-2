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
import { FormattedMessage } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
import CustomDatePicker from "../common/CustomDatePicker";
import CustomTimePicker from "../common/CustomTimePicker";
import CustomSelect from "../common/CustomSelect";
import CustomLabNumberInput from "../common/CustomLabNumberInput";
import Questionnaire from "../common/Questionnaire";
import { getFromOpenElisServer } from "../utils/Utils";
import config from "../../config.json";

export default function GenericSampleOrderEdit() {
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

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "menu.genericSample" },
    { label: "menu.genericSample.edit" },
  ];

  // Load default data
  useEffect(() => {
    // Load default dropdown data
    getFromOpenElisServer("/rest/user-sample-types", (res) => {
      setSampleTypes(res || []);
    });
    getFromOpenElisServer("/rest/UomCreate", (res) => {
      setUoms(res.existingUomList || []);
    });
    getFromOpenElisServer("/rest/notebook/list", (res) => {
      setNotebooks(res || []);
    });

    // Check if accession number is in URL params
    const urlParams = new URLSearchParams(window.location.search);
    const accessionNumberParam = urlParams.get("accessionNumber");
    if (accessionNumberParam) {
      setSearchAccessionNumber(accessionNumberParam);
      handleSearch(accessionNumberParam);
    }
  }, []);

  // Load questionnaire when notebook is selected (but not on initial load from backend)
  const [notebookChangedByUser, setNotebookChangedByUser] = useState(false);

  useEffect(() => {
    if (selectedNotebookId && notebookChangedByUser) {
      // User manually changed the notebook, so clear responses and load new questionnaire
      setFhirResponses({});
      loadFhirQuestionnaireForNotebook(selectedNotebookId);
    } else if (selectedNotebookId && !notebookChangedByUser) {
      // Notebook loaded from backend, just load the questionnaire without clearing responses
      loadFhirQuestionnaireForNotebook(selectedNotebookId);
    } else {
      setFhirQuestionnaire(null);
      setFhirResponses({});
    }
  }, [selectedNotebookId, notebookChangedByUser]);

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
      `/rest/GenericSampleOrder?accessionNumber=${encodeURIComponent(baseAccessionNumber)}`,
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
        `/rest/GenericSampleOrder/${encodeURIComponent(searchAccessionNumber)}`,
      options,
    )
      .then((response) => response.json())
      .then((data) => {
        setSaving(false);
        if (data && data.success) {
          alert(
            "Sample order updated successfully! Accession Number: " +
              (data.accessionNumber || searchAccessionNumber),
          );
          // Optionally reload the data
          handleSearch(searchAccessionNumber);
        } else {
          alert(
            "Error updating sample order: " + (data?.error || "Unknown error"),
          );
        }
      })
      .catch((error) => {
        setSaving(false);
        alert(
          "Error updating sample order: " + (error.message || "Unknown error"),
        );
      });
  };

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    handleSearch(searchAccessionNumber);
  };

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              <FormattedMessage
                id="genericSample.edit.title"
                defaultMessage="Generic Sample - Edit Order"
              />
            </Heading>
          </Section>
        </Column>
      </Grid>

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
                onChange={(e) =>
                  updateDefaultField("collector", e.target.value)
                }
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

          {/* Row 5: Notebook Selection */}
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
                options={notebooks.map((n) => ({ id: n.id, value: n.title }))}
                placeholder="Select a notebook (optional)"
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
                <Button type="submit" disabled={saving}>
                  {saving ? (
                    <InlineLoading description="Saving..." />
                  ) : (
                    <FormattedMessage id="button.save" defaultMessage="Save" />
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
    </>
  );
}
