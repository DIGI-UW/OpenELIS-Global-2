import React, { useState, useCallback, useContext, useEffect } from "react";
import {
  Grid,
  Column,
  Button,
  InlineNotification,
  Loading,
  Tabs,
  TabList,
  Tab,
  TabPanel,
  TabPanels,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Tile,
  Tag,
  Select,
  SelectItem,
  TextInput,
  TextArea,
  Form,
  FormGroup,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../../../layout/Layout";
import BioanalyticalManifestImportModal from "../../modals/BioanalyticalManifestImportModal";
import { Upload, Edit, Save } from "@carbon/react/icons";
import config from "../../../../config.json";
import "./BioanalyticalPages.css";

/**
 * BioanalyticalSampleReceptionPage - STAGE 1 of bioanalytical workflow.
 *
 * STAGE 1: Sample Reception & Registration
 * ● Receive processed biological samples from Medical Laboratory at clinical site
 * ● Receive pharmaceutical samples directly from researchers or external clients
 * ● Assign or retain Sample Identifier with linkage to source
 * ● Register metadata: sample type, requested tests, storage condition, source laboratory/client
 * ● Link to project or bioequivalence study
 *
 * 3-Tab Design:
 * - Tab 1: Manifest Import - CSV bulk import for both biological and pharmaceutical samples
 * - Tab 2: Received Samples - View and manage imported samples with verification
 * - Tab 3: Reception Metadata - Individual sample metadata editing and study linking
 *
 * @param {Object} props
 * @param {number} props.entryId - Notebook entry ID
 * @param {Object} props.pageData - Page configuration
 * @param {Object} props.progress - Sample progress counts
 * @param {function} props.onProgressUpdate - Callback after sample changes
 */
function BioanalyticalSampleReceptionPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const { setNotificationVisible } = useContext(NotificationContext);

  const [isLoading, setIsLoading] = useState(false);
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);
  const [samples, setSamples] = useState([]);
  const [selectedTab, setSelectedTab] = useState(0);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [selectedSample, setSelectedSample] = useState(null);
  const [editingMetadata, setEditingMetadata] = useState({});

  // Stage 1 sample data state
  const [sampleMetadata, setSampleMetadata] = useState({
    // Core reception data
    sampleId: "",
    sourceOrigin: "",
    sourceType: "", // NEW: Medical Lab vs. External distinction
    sourceLaboratory: "", // NEW: Source lab tracking
    requestedTests: "",
    dateTimeOfReceipt: "",
    receivingPersonnel: "",

    // Sample classification
    sampleType: "",
    storageConditionPrior: "",
    sampleVolume: "",
    transportTemperature: "",

    // Enhanced Study integration
    projectStudyAssociation: "",
    bioequivalenceProtocol: "", // NEW: BE protocol linking
    studyPhase: "", // NEW: Study phase tracking
    subjectId: "",
    timepoint: "",
    timepointDescription: "", // NEW: Enhanced timepoint info
    pkParameter: "", // NEW: PK parameter flag
    manifestVerificationStatus: "",

    // Chain of custody
    chainOfCustody: "", // NEW: Transfer tracking

    // Administrative
    notes: "",
  });

  // NEW: Source type options for Medical Lab vs. External distinction
  const sourceTypes = [
    { id: "medical_lab", label: "Medical Laboratory", description: "Processed biological samples from clinical site lab" },
    { id: "external_client", label: "External Client", description: "Pharmaceutical samples from researchers or sponsors" },
    { id: "cro", label: "Contract Research Organization (CRO)", description: "Samples from external CRO partners" },
    { id: "biorepository", label: "Biorepository", description: "Long-term stored samples transferred for analysis" },
  ];

  // NEW: Bioequivalence study phases
  const studyPhases = [
    { id: "screening", label: "Screening", description: "Pre-study qualification samples" },
    { id: "treatment_a", label: "Treatment A (Reference)", description: "Reference formulation dosing period" },
    { id: "treatment_b", label: "Treatment B (Test)", description: "Test formulation dosing period" },
    { id: "washout", label: "Washout Period", description: "Between-period washout samples" },
    { id: "follow_up", label: "Follow-up", description: "Post-study safety samples" },
  ];

  // NEW: Enhanced pharmacokinetic timepoints
  const pkTimepoints = [
    { id: "pre_dose", label: "Pre-dose (0h)", description: "Baseline sample before dosing", pkParam: "baseline" },
    { id: "0.5h", label: "0.5 hours post-dose", description: "Early absorption phase", pkParam: "absorption" },
    { id: "1h", label: "1 hour post-dose", description: "Absorption phase", pkParam: "absorption" },
    { id: "2h", label: "2 hours post-dose", description: "Peak concentration window", pkParam: "cmax" },
    { id: "4h", label: "4 hours post-dose", description: "Post-peak distribution", pkParam: "distribution" },
    { id: "8h", label: "8 hours post-dose", description: "Elimination phase", pkParam: "elimination" },
    { id: "12h", label: "12 hours post-dose", description: "Late elimination", pkParam: "elimination" },
    { id: "24h", label: "24 hours post-dose", description: "Terminal elimination", pkParam: "terminal" },
  ];

  const handleImportModalOpen = () => {
    setIsImportModalOpen(true);
  };

  const handleImportModalClose = () => {
    setIsImportModalOpen(false);
  };

  const handleImportSuccess = useCallback(
    (results) => {
      setSuccessMessage(
        intl.formatMessage(
          {
            id: "notebook.bioanalytical.reception.importSuccess",
            defaultMessage:
              "{count} samples imported successfully for Stage 1 reception",
          },
          { count: results.totalCreated || 0 },
        ),
      );

      // Refresh sample list
      if (onProgressUpdate) {
        onProgressUpdate();
      }

      // Close modal and switch to received samples tab
      setTimeout(() => {
        setIsImportModalOpen(false);
        setSelectedTab(1); // Switch to "Received Samples" tab
      }, 1000);
    },
    [intl, onProgressUpdate],
  );

  const handleSampleSelect = (sample) => {
    setSelectedSample(sample);
    setEditingMetadata({
      sampleId: sample.uniqueSampleId || sample.externalId || "",
      sourceOrigin: sample.sourceOrigin || "",
      requestedTests: sample.requestedTests || "",
      dateTimeOfReceipt: sample.dateTimeOfReceipt || "",
      receivingPersonnel: sample.receivingPersonnel || "",
      sampleType: sample.sampleType || "",
      storageConditionPrior: sample.storageConditionPrior || "",
      sampleVolume: sample.sampleVolume || "",
      transportTemperature: sample.transportTemperature || "",
      projectStudyAssociation: sample.projectStudyAssociation || "",
      subjectId: sample.subjectId || "",
      timepoint: sample.timepoint || "",
      manifestVerificationStatus: sample.manifestVerificationStatus || "",
      notes: sample.notes || "",
    });
    setSelectedTab(2); // Switch to metadata tab
  };

  const handleMetadataChange = (field, value) => {
    setEditingMetadata((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  const saveMetadata = () => {
    if (!selectedSample) return;

    // TODO: Implement API call to save metadata
    setSuccessMessage(
      intl.formatMessage({
        id: "notebook.bioanalytical.reception.metadataSaved",
        defaultMessage: "Sample metadata updated successfully",
      }),
    );
  };

  // Load Stage 1 samples from backend API
  const loadPageSamples = useCallback(() => {
    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      setIsLoading(false);
      setSamples([]);
      return;
    }

    setIsLoading(true);
    setErrorMessage("");
    setSuccessMessage("");

    // Load samples for this bioanalytical workflow page
    fetch(`${config.serverBaseUrl}/rest/notebook/page/${pageData.id}/samples`, {
      method: "GET",
      credentials: "include",
      headers: {
        "X-CSRF-Token": localStorage.getItem("CSRF"),
        "Content-Type": "application/json",
      },
    })
      .then((response) => response.json())
      .then((data) => {
        if (Array.isArray(data)) {
          const transformedSamples = data.map((sample) => ({
            id: String(sample.id || sample.sampleItemId),
            externalId: sample.externalId,
            accessionNumber: sample.accessionNumber,
            sampleType: sample.sampleType || sample.typeOfSample?.description,
            status: sample.pageStatus || sample.status || "PENDING",

            // Stage 1 bioanalytical-specific metadata from JSONB data
            uniqueSampleId: sample.data?.uniqueSampleId || sample.externalId,
            sourceOrigin: sample.data?.sourceOrigin,
            requestedTests: sample.data?.requestedTests,
            dateTimeOfReceipt: sample.data?.dateTimeOfReceipt,
            receivingPersonnel: sample.data?.receivingPersonnel,
            projectStudyAssociation: sample.data?.projectStudyAssociation,
            storageConditionPrior: sample.data?.storageConditionPrior,
            sampleVolume: sample.data?.sampleVolume,
            transportTemperature: sample.data?.transportTemperature,
            manifestVerificationStatus: sample.data?.manifestVerificationStatus,
            subjectId: sample.data?.subjectId,
            timepoint: sample.data?.timepoint,
            notes: sample.data?.notes,
          }));
          setSamples(transformedSamples);
        } else {
          setSamples([]);
        }
        setIsLoading(false);
      })
      .catch((error) => {
        console.error("Failed to load Stage 1 samples:", error);
        setErrorMessage(
          intl.formatMessage({
            id: "notebook.bioanalytical.stage1.error.loadSamples",
            defaultMessage: "Failed to load samples. Please refresh the page.",
          }),
        );
        setSamples([]);
        setIsLoading(false);
      });
  }, [pageData?.id, intl]);

  // Load samples when component mounts or page changes
  useEffect(() => {
    loadPageSamples();
  }, [loadPageSamples]);

  return (
    <div className="bioanalytical-page">
      {/* Stage 1 Header */}
      <div className="page-instructions">
        <h3>
          <FormattedMessage
            id="notebook.bioanalytical.stage1.title"
            defaultMessage="STAGE 1: Sample Reception & Registration"
          />
        </h3>
        <div className="stage-requirements">
          <p>
            <FormattedMessage
              id="notebook.bioanalytical.stage1.description"
              defaultMessage="Complete sample reception and registration workflow covering all bioanalytical laboratory requirements:"
            />
          </p>
          <ul style={{ marginLeft: "1.5rem", marginTop: "0.5rem" }}>
            <li>
              <FormattedMessage
                id="notebook.bioanalytical.stage1.req1"
                defaultMessage="Receive processed biological samples from Medical Laboratory at clinical site"
              />
            </li>
            <li>
              <FormattedMessage
                id="notebook.bioanalytical.stage1.req2"
                defaultMessage="Receive pharmaceutical samples directly from researchers or external clients"
              />
            </li>
            <li>
              <FormattedMessage
                id="notebook.bioanalytical.stage1.req3"
                defaultMessage="Assign or retain Sample Identifier with linkage to source"
              />
            </li>
            <li>
              <FormattedMessage
                id="notebook.bioanalytical.stage1.req4"
                defaultMessage="Register metadata: sample type, requested tests, storage condition, source laboratory/client"
              />
            </li>
            <li>
              <FormattedMessage
                id="notebook.bioanalytical.stage1.req5"
                defaultMessage="Link to project or bioequivalence study"
              />
            </li>
          </ul>
        </div>
      </div>

      {/* Stage 1 Progress Overview */}
      <Grid fullWidth style={{ marginBottom: "1.5rem" }}>
        <Column lg={16} md={8} sm={4}>
          <div
            className="progress-tiles"
            style={{ display: "flex", gap: "1rem" }}
          >
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.bioanalytical.stage1.totalReceived"
                  defaultMessage="Total Received"
                />
              </span>
              <span className="progress-value">{progress?.total || 0}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.bioanalytical.stage1.biologicalSamples"
                  defaultMessage="Biological Samples"
                />
              </span>
              <span className="progress-value">
                {
                  samples.filter((s) =>
                    ["Plasma", "Serum", "Urine", "Whole Blood"].includes(
                      s.sampleType,
                    ),
                  ).length
                }
              </span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.bioanalytical.stage1.pharmaceuticalSamples"
                  defaultMessage="Pharmaceutical Samples"
                />
              </span>
              <span className="progress-value">
                {
                  samples.filter((s) =>
                    ["API", "Tablet", "Capsule", "Suspension"].includes(
                      s.sampleType,
                    ),
                  ).length
                }
              </span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.bioanalytical.stage1.studyLinked"
                  defaultMessage="Study Linked"
                />
              </span>
              <span className="progress-value">
                {samples.filter((s) => s.projectStudyAssociation).length}
              </span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Error/Success Messages */}
      {errorMessage && (
        <div style={{ marginBottom: "1rem" }}>
          <InlineNotification
            kind="error"
            title={intl.formatMessage({
              id: "notebook.bioanalytical.reception.error",
              defaultMessage: "Error",
            })}
            subtitle={errorMessage}
            lowContrast
            onCloseButtonClick={() => setErrorMessage("")}
          />
        </div>
      )}

      {successMessage && (
        <div style={{ marginBottom: "1rem" }}>
          <InlineNotification
            kind="success"
            title={intl.formatMessage({
              id: "notebook.bioanalytical.reception.success",
              defaultMessage: "Success",
            })}
            subtitle={successMessage}
            lowContrast
            onCloseButtonClick={() => setSuccessMessage("")}
          />
        </div>
      )}

      {/* 3-Tab Design for Stage 1 Components */}
      <Tabs
        selectedIndex={selectedTab}
        onChange={(evt) => setSelectedTab(evt.selectedIndex)}
      >
        <TabList aria-label="Stage 1 sample reception components">
          <Tab>
            <FormattedMessage
              id="notebook.bioanalytical.stage1.tab.import"
              defaultMessage="Manifest Import"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="notebook.bioanalytical.stage1.tab.samples"
              defaultMessage="Received Samples"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="notebook.bioanalytical.stage1.tab.metadata"
              defaultMessage="Reception Metadata"
            />
          </Tab>
        </TabList>

        <TabPanels>
          {/* Tab 1: Manifest Import - CSV bulk import for both sample types */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="page-section">
                    <div className="section-header">
                      <h3 style={{ marginBottom: "1rem" }}>
                        <FormattedMessage
                          id="notebook.bioanalytical.stage1.import.title"
                          defaultMessage="CSV Manifest Import - Stage 1 Reception"
                        />
                      </h3>
                      <p style={{ marginBottom: "1.5rem", color: "#525252" }}>
                        <FormattedMessage
                          id="notebook.bioanalytical.stage1.import.description"
                          defaultMessage="Import both biological and pharmaceutical samples with complete Stage 1 metadata. This addresses all Stage 1 requirements for sample reception and registration."
                        />
                      </p>
                    </div>

                    {/* Stage 1 Requirements Mapping */}
                    <div
                      style={{
                        marginBottom: "2rem",
                        padding: "1rem",
                        backgroundColor: "#f4f4f4",
                        borderRadius: "4px",
                      }}
                    >
                      <h4
                        style={{
                          marginBottom: "1rem",
                          fontSize: "1rem",
                          fontWeight: "600",
                        }}
                      >
                        <FormattedMessage
                          id="notebook.bioanalytical.stage1.import.requirements"
                          defaultMessage="How Manifest Import Addresses Stage 1 Requirements:"
                        />
                      </h4>

                      <div style={{ display: "grid", gap: "1rem" }}>
                        <div
                          style={{
                            display: "flex",
                            alignItems: "center",
                            gap: "0.5rem",
                          }}
                        >
                          <Tag type="blue" size="sm">
                            1
                          </Tag>
                          <span style={{ fontSize: "0.875rem" }}>
                            <FormattedMessage
                              id="notebook.bioanalytical.stage1.import.req1"
                              defaultMessage="Biological Samples: Import plasma, serum, urine from Medical Laboratory with clinical site linkage"
                            />
                          </span>
                        </div>

                        <div
                          style={{
                            display: "flex",
                            alignItems: "center",
                            gap: "0.5rem",
                          }}
                        >
                          <Tag type="green" size="sm">
                            2
                          </Tag>
                          <span style={{ fontSize: "0.875rem" }}>
                            <FormattedMessage
                              id="notebook.bioanalytical.stage1.import.req2"
                              defaultMessage="Pharmaceutical Samples: Import API, tablets, capsules from researchers/external clients"
                            />
                          </span>
                        </div>

                        <div
                          style={{
                            display: "flex",
                            alignItems: "center",
                            gap: "0.5rem",
                          }}
                        >
                          <Tag type="purple" size="sm">
                            3
                          </Tag>
                          <span style={{ fontSize: "0.875rem" }}>
                            <FormattedMessage
                              id="notebook.bioanalytical.stage1.import.req3"
                              defaultMessage="Sample Identifier: Assign new or retain original IDs with source linkage tracking"
                            />
                          </span>
                        </div>

                        <div
                          style={{
                            display: "flex",
                            alignItems: "center",
                            gap: "0.5rem",
                          }}
                        >
                          <Tag type="red" size="sm">
                            4
                          </Tag>
                          <span style={{ fontSize: "0.875rem" }}>
                            <FormattedMessage
                              id="notebook.bioanalytical.stage1.import.req4"
                              defaultMessage="Metadata Registration: Sample type, tests, storage conditions, source laboratory/client"
                            />
                          </span>
                        </div>

                        <div
                          style={{
                            display: "flex",
                            alignItems: "center",
                            gap: "0.5rem",
                          }}
                        >
                          <Tag type="cyan" size="sm">
                            5
                          </Tag>
                          <span style={{ fontSize: "0.875rem" }}>
                            <FormattedMessage
                              id="notebook.bioanalytical.stage1.import.req5"
                              defaultMessage="Study Linking: Connect to bioequivalence studies with subject IDs and timepoints"
                            />
                          </span>
                        </div>
                      </div>
                    </div>

                    {/* Import Action */}
                    <div className="import-section">
                      <h4 style={{ marginBottom: "0.75rem" }}>
                        <FormattedMessage
                          id="notebook.bioanalytical.stage1.import.action"
                          defaultMessage="Start Stage 1 Sample Import"
                        />
                      </h4>

                      <p
                        style={{
                          marginBottom: "1.5rem",
                          fontSize: "0.875rem",
                          color: "#525252",
                        }}
                      >
                        <FormattedMessage
                          id="notebook.bioanalytical.stage1.import.help"
                          defaultMessage="Upload CSV manifest to import samples with complete Stage 1 metadata. The system handles both biological samples from clinical sites and pharmaceutical samples from research clients."
                        />
                      </p>

                      <Button
                        kind="primary"
                        renderIcon={Upload}
                        onClick={handleImportModalOpen}
                      >
                        <FormattedMessage
                          id="notebook.bioanalytical.stage1.import.button"
                          defaultMessage="Import Stage 1 Manifest"
                        />
                      </Button>
                    </div>
                  </div>
                </Column>
              </Grid>
            </div>
          </TabPanel>

          {/* Tab 2: Received Samples - View and manage imported samples */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="section-header">
                    <h4>
                      <FormattedMessage
                        id="notebook.bioanalytical.stage1.samples.title"
                        defaultMessage="Stage 1 Received Samples"
                      />
                    </h4>
                    <p style={{ marginBottom: "1.5rem" }}>
                      <FormattedMessage
                        id="notebook.bioanalytical.stage1.samples.description"
                        defaultMessage="Review and manage samples received during Stage 1. Both biological samples from medical laboratories and pharmaceutical samples from researchers are shown with their reception metadata."
                      />
                    </p>
                  </div>

                  {/* Sample Type Breakdown */}
                  <div
                    style={{
                      marginBottom: "1.5rem",
                      display: "flex",
                      gap: "1rem",
                    }}
                  >
                    <Tag type="blue">
                      <FormattedMessage
                        id="notebook.bioanalytical.stage1.samples.biological"
                        defaultMessage="Biological: {count}"
                        values={{
                          count: samples.filter((s) =>
                            [
                              "Plasma",
                              "Serum",
                              "Urine",
                              "Whole Blood",
                            ].includes(s.sampleType),
                          ).length,
                        }}
                      />
                    </Tag>
                    <Tag type="green">
                      <FormattedMessage
                        id="notebook.bioanalytical.stage1.samples.pharmaceutical"
                        defaultMessage="Pharmaceutical: {count}"
                        values={{
                          count: samples.filter((s) =>
                            ["API", "Tablet", "Capsule", "Suspension"].includes(
                              s.sampleType,
                            ),
                          ).length,
                        }}
                      />
                    </Tag>
                  </div>

                  {isLoading ? (
                    <Loading description="Loading Stage 1 samples..." />
                  ) : (
                    <div>
                      <Table>
                        <TableHead>
                          <TableRow>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.stage1.samples.column.id"
                                defaultMessage="Sample ID"
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.stage1.samples.column.type"
                                defaultMessage="Sample Type"
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.stage1.samples.column.source"
                                defaultMessage="Source Origin"
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.stage1.samples.column.tests"
                                defaultMessage="Requested Tests"
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.stage1.samples.column.study"
                                defaultMessage="Study Link"
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.stage1.samples.column.status"
                                defaultMessage="Status"
                              />
                            </TableHeader>
                            <TableHeader>
                              <FormattedMessage
                                id="notebook.bioanalytical.stage1.samples.column.actions"
                                defaultMessage="Actions"
                              />
                            </TableHeader>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {samples.length > 0 ? (
                            samples.map((sample, idx) => (
                              <TableRow key={idx}>
                                <TableCell>{sample.uniqueSampleId}</TableCell>
                                <TableCell>
                                  <Tag
                                    type={
                                      sample.sampleType === "Plasma"
                                        ? "blue"
                                        : "green"
                                    }
                                    size="sm"
                                  >
                                    {sample.sampleType}
                                  </Tag>
                                </TableCell>
                                <TableCell>{sample.sourceOrigin}</TableCell>
                                <TableCell>{sample.requestedTests}</TableCell>
                                <TableCell>
                                  {sample.projectStudyAssociation}
                                </TableCell>
                                <TableCell>
                                  <Tag
                                    type={
                                      sample.status === "PENDING"
                                        ? "gray"
                                        : sample.status === "IN_PROGRESS"
                                          ? "blue"
                                          : "green"
                                    }
                                    size="sm"
                                  >
                                    {sample.status}
                                  </Tag>
                                </TableCell>
                                <TableCell>
                                  <Button
                                    kind="ghost"
                                    size="sm"
                                    renderIcon={Edit}
                                    onClick={() => handleSampleSelect(sample)}
                                  >
                                    <FormattedMessage
                                      id="notebook.bioanalytical.stage1.samples.edit"
                                      defaultMessage="Edit Metadata"
                                    />
                                  </Button>
                                </TableCell>
                              </TableRow>
                            ))
                          ) : (
                            <TableRow>
                              <TableCell
                                colSpan="7"
                                style={{ textAlign: "center", padding: "2rem" }}
                              >
                                <FormattedMessage
                                  id="notebook.bioanalytical.stage1.samples.empty"
                                  defaultMessage="No samples received yet. Import a CSV manifest from the 'Manifest Import' tab to add samples."
                                />
                              </TableCell>
                            </TableRow>
                          )}
                        </TableBody>
                      </Table>
                    </div>
                  )}
                </Column>
              </Grid>
            </div>
          </TabPanel>

          {/* Tab 3: Reception Metadata - Individual sample metadata editing */}
          <TabPanel>
            <div style={{ paddingTop: "1.5rem" }}>
              <Grid>
                <Column lg={16} md={8} sm={4}>
                  <div className="section-header">
                    <h4>
                      <FormattedMessage
                        id="notebook.bioanalytical.stage1.metadata.title"
                        defaultMessage="Stage 1 Reception Metadata Editor"
                      />
                    </h4>
                    <p style={{ marginBottom: "1.5rem" }}>
                      <FormattedMessage
                        id="notebook.bioanalytical.stage1.metadata.description"
                        defaultMessage="Edit individual sample metadata to complete Stage 1 registration. Update sample identifiers, source linkage, metadata registration, and study associations."
                      />
                    </p>
                  </div>

                  {selectedSample ? (
                    <Form>
                      {/* Core Reception Data - Stage 1 Requirements 3 & 4 */}
                      <div style={{ marginBottom: "2rem" }}>
                        <h5
                          style={{
                            marginBottom: "1rem",
                            display: "flex",
                            alignItems: "center",
                            gap: "0.5rem",
                          }}
                        >
                          <Tag type="purple" size="sm">
                            3
                          </Tag>
                          <FormattedMessage
                            id="notebook.bioanalytical.stage1.metadata.core"
                            defaultMessage="Sample Identifier & Core Reception Data"
                          />
                        </h5>

                        <div
                          style={{
                            display: "grid",
                            gridTemplateColumns:
                              "repeat(auto-fit, minmax(300px, 1fr))",
                            gap: "1rem",
                          }}
                        >
                          <TextInput
                            labelText={intl.formatMessage({
                              id: "notebook.bioanalytical.stage1.metadata.sampleId",
                              defaultMessage: "Sample ID (with source linkage)",
                            })}
                            value={editingMetadata.sampleId || ""}
                            onChange={(e) =>
                              handleMetadataChange("sampleId", e.target.value)
                            }
                            helperText="Retain original or assign new with source tracking"
                          />

                          <Select
                            id="source-type"
                            labelText={intl.formatMessage({
                              id: "notebook.bioanalytical.stage1.metadata.sourceType",
                              defaultMessage: "Source Type *",
                            })}
                            value={editingMetadata.sourceType || ""}
                            onChange={(e) =>
                              handleMetadataChange("sourceType", e.target.value)
                            }
                          >
                            <SelectItem
                              value=""
                              text="-- Select source type --"
                            />
                            {sourceTypes.map((type) => (
                              <SelectItem
                                key={type.id}
                                value={type.id}
                                text={`${type.label} - ${type.description}`}
                              />
                            ))}
                          </Select>

                          <TextInput
                            labelText={intl.formatMessage({
                              id: "notebook.bioanalytical.stage1.metadata.sourceLaboratory",
                              defaultMessage: "Source Laboratory/Client",
                            })}
                            value={editingMetadata.sourceLaboratory || ""}
                            onChange={(e) =>
                              handleMetadataChange("sourceLaboratory", e.target.value)
                            }
                            helperText="Specific laboratory or client organization name"
                          />

                          <TextInput
                            labelText={intl.formatMessage({
                              id: "notebook.bioanalytical.stage1.metadata.sourceOrigin",
                              defaultMessage: "Source Origin Details",
                            })}
                            value={editingMetadata.sourceOrigin || ""}
                            onChange={(e) =>
                              handleMetadataChange(
                                "sourceOrigin",
                                e.target.value,
                              )
                            }
                            helperText="Additional source information or reference number"
                          />

                          <TextInput
                            labelText={intl.formatMessage({
                              id: "notebook.bioanalytical.stage1.metadata.dateTimeOfReceipt",
                              defaultMessage: "Date/Time of Receipt",
                            })}
                            value={editingMetadata.dateTimeOfReceipt || ""}
                            onChange={(e) =>
                              handleMetadataChange(
                                "dateTimeOfReceipt",
                                e.target.value,
                              )
                            }
                            helperText="YYYY-MM-DD HH:MM"
                          />

                          <TextInput
                            labelText={intl.formatMessage({
                              id: "notebook.bioanalytical.stage1.metadata.receivingPersonnel",
                              defaultMessage: "Receiving Personnel",
                            })}
                            value={editingMetadata.receivingPersonnel || ""}
                            onChange={(e) =>
                              handleMetadataChange(
                                "receivingPersonnel",
                                e.target.value,
                              )
                            }
                            helperText="Person who received the sample"
                          />
                        </div>
                      </div>

                      {/* Sample Classification - Stage 1 Requirement 4 */}
                      <div style={{ marginBottom: "2rem" }}>
                        <h5
                          style={{
                            marginBottom: "1rem",
                            display: "flex",
                            alignItems: "center",
                            gap: "0.5rem",
                          }}
                        >
                          <Tag type="red" size="sm">
                            4
                          </Tag>
                          <FormattedMessage
                            id="notebook.bioanalytical.stage1.metadata.classification"
                            defaultMessage="Metadata Registration (Sample Type, Tests, Storage)"
                          />
                        </h5>

                        <div
                          style={{
                            display: "grid",
                            gridTemplateColumns:
                              "repeat(auto-fit, minmax(300px, 1fr))",
                            gap: "1rem",
                          }}
                        >
                          <Select
                            labelText={intl.formatMessage({
                              id: "notebook.bioanalytical.stage1.metadata.sampleType",
                              defaultMessage: "Sample Type",
                            })}
                            value={editingMetadata.sampleType || ""}
                            onChange={(e) =>
                              handleMetadataChange("sampleType", e.target.value)
                            }
                          >
                            <option value="">Select sample type...</option>
                            <option value="Plasma">Plasma (Biological)</option>
                            <option value="Serum">Serum (Biological)</option>
                            <option value="Urine">Urine (Biological)</option>
                            <option value="API">API (Pharmaceutical)</option>
                            <option value="Tablet">
                              Tablet (Pharmaceutical)
                            </option>
                            <option value="Capsule">
                              Capsule (Pharmaceutical)
                            </option>
                          </Select>

                          <TextInput
                            labelText={intl.formatMessage({
                              id: "notebook.bioanalytical.stage1.metadata.requestedTests",
                              defaultMessage: "Requested Tests",
                            })}
                            value={editingMetadata.requestedTests || ""}
                            onChange={(e) =>
                              handleMetadataChange(
                                "requestedTests",
                                e.target.value,
                              )
                            }
                            helperText="Assay, Dissolution, LC-MS/MS, HPLC, Bioequivalence"
                          />

                          <Select
                            labelText={intl.formatMessage({
                              id: "notebook.bioanalytical.stage1.metadata.storageConditionPrior",
                              defaultMessage: "Storage Condition Prior",
                            })}
                            value={editingMetadata.storageConditionPrior || ""}
                            onChange={(e) =>
                              handleMetadataChange(
                                "storageConditionPrior",
                                e.target.value,
                              )
                            }
                          >
                            <option value="">
                              Select storage condition...
                            </option>
                            <option value="Room Temperature">
                              Room Temperature
                            </option>
                            <option value="Refrigerated (2-8°C)">
                              Refrigerated (2-8°C)
                            </option>
                            <option value="Frozen (-20°C)">
                              Frozen (-20°C)
                            </option>
                            <option value="Ultra-frozen (-80°C)">
                              Ultra-frozen (-80°C)
                            </option>
                          </Select>

                          <TextInput
                            labelText={intl.formatMessage({
                              id: "notebook.bioanalytical.stage1.metadata.sampleVolume",
                              defaultMessage: "Sample Volume/Quantity",
                            })}
                            value={editingMetadata.sampleVolume || ""}
                            onChange={(e) =>
                              handleMetadataChange(
                                "sampleVolume",
                                e.target.value,
                              )
                            }
                            helperText="5 mL, 100 mg, 2 tablets"
                          />
                        </div>
                      </div>

                      {/* Study Integration - Stage 1 Requirement 5 */}
                      <div style={{ marginBottom: "2rem" }}>
                        <h5
                          style={{
                            marginBottom: "1rem",
                            display: "flex",
                            alignItems: "center",
                            gap: "0.5rem",
                          }}
                        >
                          <Tag type="cyan" size="sm">
                            5
                          </Tag>
                          <FormattedMessage
                            id="notebook.bioanalytical.stage1.metadata.study"
                            defaultMessage="Study Linking (Bioequivalence Studies)"
                          />
                        </h5>

                        <div
                          style={{
                            display: "grid",
                            gridTemplateColumns:
                              "repeat(auto-fit, minmax(300px, 1fr))",
                            gap: "1rem",
                          }}
                        >
                          <TextInput
                            labelText={intl.formatMessage({
                              id: "notebook.bioanalytical.stage1.metadata.projectStudyAssociation",
                              defaultMessage: "Project/Study Association",
                            })}
                            value={
                              editingMetadata.projectStudyAssociation || ""
                            }
                            onChange={(e) =>
                              handleMetadataChange(
                                "projectStudyAssociation",
                                e.target.value,
                              )
                            }
                            helperText="BE-2024-001, STAB-API-001, PK-STUDY-002"
                          />

                          <TextInput
                            labelText={intl.formatMessage({
                              id: "notebook.bioanalytical.stage1.metadata.bioequivalenceProtocol",
                              defaultMessage: "Bioequivalence Protocol ID",
                            })}
                            value={editingMetadata.bioequivalenceProtocol || ""}
                            onChange={(e) =>
                              handleMetadataChange("bioequivalenceProtocol", e.target.value)
                            }
                            helperText="BE-PROTO-001, FDA-BE-2024-XYZ (for BE studies only)"
                          />

                          <Select
                            id="study-phase"
                            labelText={intl.formatMessage({
                              id: "notebook.bioanalytical.stage1.metadata.studyPhase",
                              defaultMessage: "Study Phase",
                            })}
                            value={editingMetadata.studyPhase || ""}
                            onChange={(e) =>
                              handleMetadataChange("studyPhase", e.target.value)
                            }
                          >
                            <SelectItem
                              value=""
                              text="-- Select study phase --"
                            />
                            {studyPhases.map((phase) => (
                              <SelectItem
                                key={phase.id}
                                value={phase.id}
                                text={`${phase.label} - ${phase.description}`}
                              />
                            ))}
                          </Select>

                          <TextInput
                            labelText={intl.formatMessage({
                              id: "notebook.bioanalytical.stage1.metadata.subjectId",
                              defaultMessage: "Subject/Patient ID",
                            })}
                            value={editingMetadata.subjectId || ""}
                            onChange={(e) =>
                              handleMetadataChange("subjectId", e.target.value)
                            }
                            helperText="SUBJ-001, P-101 (for bioequivalence studies)"
                          />

                          <Select
                            id="pk-timepoint"
                            labelText={intl.formatMessage({
                              id: "notebook.bioanalytical.stage1.metadata.timepoint",
                              defaultMessage: "Pharmacokinetic Timepoint *",
                            })}
                            value={editingMetadata.timepoint || ""}
                            onChange={(e) => {
                              const selectedTimepoint = pkTimepoints.find(tp => tp.id === e.target.value);
                              handleMetadataChange("timepoint", e.target.value);
                              handleMetadataChange("timepointDescription", selectedTimepoint?.description || "");
                              handleMetadataChange("pkParameter", selectedTimepoint?.pkParam || "");
                            }}
                          >
                            <SelectItem
                              value=""
                              text="-- Select timepoint --"
                            />
                            {pkTimepoints.map((timepoint) => (
                              <SelectItem
                                key={timepoint.id}
                                value={timepoint.id}
                                text={`${timepoint.label} - ${timepoint.description}`}
                              />
                            ))}
                          </Select>

                          <TextInput
                            labelText={intl.formatMessage({
                              id: "notebook.bioanalytical.stage1.metadata.timepointDescription",
                              defaultMessage: "Timepoint Details",
                            })}
                            value={editingMetadata.timepointDescription || ""}
                            onChange={(e) =>
                              handleMetadataChange("timepointDescription", e.target.value)
                            }
                            helperText="Auto-populated from PK timepoint selection"
                            disabled={!!editingMetadata.timepoint}
                          />

                          <TextInput
                            labelText={intl.formatMessage({
                              id: "notebook.bioanalytical.stage1.metadata.pkParameter",
                              defaultMessage: "Target PK Parameter",
                            })}
                            value={editingMetadata.pkParameter || ""}
                            onChange={(e) =>
                              handleMetadataChange("pkParameter", e.target.value)
                            }
                            helperText="Cmax, AUC, Tmax, elimination phase"
                            disabled={!!editingMetadata.timepoint}
                          />

                          <Select
                            labelText={intl.formatMessage({
                              id: "notebook.bioanalytical.stage1.metadata.manifestVerificationStatus",
                              defaultMessage: "Manifest Verification Status",
                            })}
                            value={
                              editingMetadata.manifestVerificationStatus || ""
                            }
                            onChange={(e) =>
                              handleMetadataChange(
                                "manifestVerificationStatus",
                                e.target.value,
                              )
                            }
                          >
                            <option value="">Select status...</option>
                            <option value="Verified">Verified</option>
                            <option value="Pending">Pending</option>
                            <option value="Discrepancy">Discrepancy</option>
                          </Select>
                        </div>
                      </div>

                      {/* Notes */}
                      <div style={{ marginBottom: "2rem" }}>
                        <TextArea
                          labelText={intl.formatMessage({
                            id: "notebook.bioanalytical.stage1.metadata.notes",
                            defaultMessage: "Notes/Comments",
                          })}
                          value={editingMetadata.notes || ""}
                          onChange={(e) =>
                            handleMetadataChange("notes", e.target.value)
                          }
                          helperText="Additional observations and notes"
                          rows={3}
                        />
                      </div>

                      {/* Save Button */}
                      <Button
                        kind="primary"
                        renderIcon={Save}
                        onClick={saveMetadata}
                      >
                        <FormattedMessage
                          id="notebook.bioanalytical.stage1.metadata.save"
                          defaultMessage="Save Stage 1 Metadata"
                        />
                      </Button>
                    </Form>
                  ) : (
                    <div
                      style={{
                        textAlign: "center",
                        padding: "2rem",
                        color: "#525252",
                      }}
                    >
                      <FormattedMessage
                        id="notebook.bioanalytical.stage1.metadata.noSelection"
                        defaultMessage="Select a sample from the 'Received Samples' tab to edit its Stage 1 metadata."
                      />
                    </div>
                  )}
                </Column>
              </Grid>
            </div>
          </TabPanel>
        </TabPanels>
      </Tabs>

      {/* Import Modal */}
      <BioanalyticalManifestImportModal
        open={isImportModalOpen}
        onClose={handleImportModalClose}
        entryId={entryId}
        onSuccess={handleImportSuccess}
      />
    </div>
  );
}

export default BioanalyticalSampleReceptionPage;
