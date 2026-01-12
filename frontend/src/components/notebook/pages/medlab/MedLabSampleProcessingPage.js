import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  NumberInput,
  TextInput,
  Dropdown,
  Checkbox,
  Modal,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Tag,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  TextArea,
} from "@carbon/react";
import {
  Add,
  ArrowRight,
  Renew,
  View,
  Chemistry,
  Microscope,
  DataShare,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * SampleProcessingPage - Sample processing page for MedLab workflow.
 * Handles discipline-specific processing, derived materials, aliquoting,
 * and special handling flags.
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function MedLabSampleProcessingPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [activeTab, setActiveTab] = useState(0);

  // Processing modal state
  const [processModalOpen, setProcessModalOpen] = useState(false);
  const [processingType, setProcessingType] = useState("");
  const [derivedMaterial, setDerivedMaterial] = useState("");
  const [processingNotes, setProcessingNotes] = useState("");
  const [isBioequivalence, setIsBioequivalence] = useState(false);
  const [transferToBioanalytical, setTransferToBioanalytical] = useState(false);
  const [processing, setProcessing] = useState(false);

  // Aliquoting modal state
  const [aliquotModalOpen, setAliquotModalOpen] = useState(false);
  const [aliquotCount, setAliquotCount] = useState(1);
  const [containerType, setContainerType] = useState("cryovial");
  const [labelingConfirmed, setLabelingConfirmed] = useState(false);
  const [externalIdPrefix, setExternalIdPrefix] = useState("MLB-A");
  const [creating, setCreating] = useState(false);

  // View children modal
  const [viewChildrenModalOpen, setViewChildrenModalOpen] = useState(false);
  const [selectedParentForView, setSelectedParentForView] = useState(null);
  const [parentChildren, setParentChildren] = useState([]);
  const [loadingChildren, setLoadingChildren] = useState(false);

  // Processing type options by discipline
  const processingTypeOptions = [
    {
      id: "chemistry-centrifugation",
      label: "Chemistry: Centrifugation",
      discipline: "CHEMISTRY",
    },
    {
      id: "hematology-smear",
      label: "Hematology: Blood Smear",
      discipline: "HEMATOLOGY",
    },
    {
      id: "hematology-staining",
      label: "Hematology: Staining",
      discipline: "HEMATOLOGY",
    },
    {
      id: "parasitology-concentration",
      label: "Parasitology: Concentration",
      discipline: "PARASITOLOGY",
    },
    {
      id: "parasitology-thick-smear",
      label: "Parasitology: Thick Smear",
      discipline: "PARASITOLOGY",
    },
    {
      id: "microbiology-inoculation",
      label: "Microbiology: Culture Inoculation",
      discipline: "MICROBIOLOGY",
    },
    {
      id: "microbiology-gram-stain",
      label: "Microbiology: Gram Stain",
      discipline: "MICROBIOLOGY",
    },
    {
      id: "serology-dilution",
      label: "Serology: Serial Dilution",
      discipline: "SEROLOGY",
    },
    {
      id: "urinalysis-spin",
      label: "Urinalysis: Spin",
      discipline: "URINALYSIS",
    },
    { id: "other", label: "Other Processing", discipline: "OTHER" },
  ];

  // Derived material options
  const derivedMaterialOptions = [
    { id: "serum", label: "Serum" },
    { id: "plasma", label: "Plasma (EDTA)" },
    { id: "plasma-citrate", label: "Plasma (Citrate)" },
    { id: "plasma-heparin", label: "Plasma (Heparin)" },
    { id: "buffy-coat", label: "Buffy Coat" },
    { id: "red-cells", label: "Red Blood Cells" },
    { id: "urine-sediment", label: "Urine Sediment" },
    { id: "urine-supernatant", label: "Urine Supernatant" },
    { id: "csf-supernatant", label: "CSF Supernatant" },
    { id: "csf-sediment", label: "CSF Sediment" },
    { id: "stool-concentrate", label: "Stool Concentrate" },
    { id: "culture-isolate", label: "Culture Isolate" },
    { id: "dna-extract", label: "DNA Extract" },
    { id: "rna-extract", label: "RNA Extract" },
    { id: "other", label: "Other" },
  ];

  // Container type options
  const containerTypeOptions = [
    { id: "cryovial", label: "Cryovial (1.5mL)" },
    { id: "cryovial-2ml", label: "Cryovial (2.0mL)" },
    { id: "microtube", label: "Microtube (0.5mL)" },
    { id: "microtube-1.5ml", label: "Microtube (1.5mL)" },
    { id: "aliquot-tube", label: "Aliquot Tube" },
    { id: "slide", label: "Glass Slide" },
    { id: "culture-plate", label: "Culture Plate" },
    { id: "culture-tube", label: "Culture Tube" },
  ];

  // Define loadSamplesForProcessing before useEffect that uses it
  const loadSamplesForProcessing = useCallback(() => {
    if (!entryId) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    getFromOpenElisServer(
      `/rest/medlab/entry/${entryId}/samples-for-processing`,
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            setSamples(response);
          } else if (response && response.error) {
            setError(response.error);
            setSamples([]);
          } else {
            setSamples([]);
          }
          setLoading(false);
        }
      },
    );
  }, [entryId]);

  // Load samples on mount and when entryId changes
  useEffect(() => {
    componentMounted.current = true;
    loadSamplesForProcessing();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, loadSamplesForProcessing]);

  // Check if page has a real database ID
  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Handle process samples modal open
  const handleOpenProcessModal = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError("Please select at least one sample to process.");
      return;
    }
    setProcessingType("");
    setDerivedMaterial("");
    setProcessingNotes("");
    setIsBioequivalence(false);
    setTransferToBioanalytical(false);
    setProcessModalOpen(true);
  }, [selectedSampleIds]);

  // Handle record processing
  const handleRecordProcessing = useCallback(() => {
    if (!processingType) {
      setError("Please select a processing type.");
      return;
    }

    setProcessing(true);
    setError(null);

    const requestBody = {
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      processingType: processingType,
      derivedMaterial: derivedMaterial || null,
      notes: processingNotes || null,
      isBioequivalence: isBioequivalence,
      transferToBioanalytical: transferToBioanalytical,
      notebookPageId: pageData?.id || null,
    };

    postToOpenElisServerJsonResponse(
      `/rest/medlab/entry/${entryId}/record-processing`,
      JSON.stringify(requestBody),
      (response) => {
        setProcessing(false);
        setProcessModalOpen(false);

        if (response && response.success) {
          setSuccess(
            `Successfully recorded processing for ${response.processedCount || selectedSampleIds.length} sample(s).`,
          );
          setSelectedSampleIds([]);
          loadSamplesForProcessing();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(response?.error || "Failed to record processing.");
        }
      },
    );
  }, [
    selectedSampleIds,
    processingType,
    derivedMaterial,
    processingNotes,
    isBioequivalence,
    transferToBioanalytical,
    pageData?.id,
    entryId,
    loadSamplesForProcessing,
    onProgressUpdate,
  ]);

  // Handle aliquot modal open
  const handleOpenAliquotModal = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError("Please select at least one sample to create aliquots.");
      return;
    }
    setAliquotCount(1);
    setContainerType("cryovial");
    setLabelingConfirmed(false);
    setExternalIdPrefix("MLB-A");
    setAliquotModalOpen(true);
  }, [selectedSampleIds]);

  // Handle create aliquots (child samples)
  const handleCreateAliquots = useCallback(() => {
    if (!labelingConfirmed) {
      setError("Please confirm that labeling requirements are met.");
      return;
    }

    setCreating(true);
    setError(null);

    const requestBody = {
      parentSampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      childCountPerParent: aliquotCount,
      externalIdPrefix: externalIdPrefix,
      containerType: containerType,
      notebookPageId: pageData?.id || null,
    };

    postToOpenElisServerJsonResponse(
      `/rest/medlab/entry/${entryId}/create-aliquots`,
      JSON.stringify(requestBody),
      (response) => {
        setCreating(false);
        setAliquotModalOpen(false);

        if (response && response.success) {
          setSuccess(
            `Successfully created ${response.createdCount || aliquotCount * selectedSampleIds.length} aliquot(s).`,
          );
          setSelectedSampleIds([]);
          loadSamplesForProcessing();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(response?.error || "Failed to create aliquots.");
        }
      },
    );
  }, [
    selectedSampleIds,
    aliquotCount,
    externalIdPrefix,
    containerType,
    labelingConfirmed,
    pageData?.id,
    entryId,
    loadSamplesForProcessing,
    onProgressUpdate,
  ]);

  // Handle view children for a parent
  const handleViewChildren = useCallback((parentSampleId) => {
    setSelectedParentForView(parentSampleId);
    setViewChildrenModalOpen(true);
    setLoadingChildren(true);

    getFromOpenElisServer(
      `/rest/notebook/samples/${parentSampleId}/children`,
      (response) => {
        setLoadingChildren(false);
        if (response && Array.isArray(response)) {
          setParentChildren(response);
        } else {
          setParentChildren([]);
        }
      },
    );
  }, []);

  // Calculate stats
  const totalSamples = samples.length;
  const processedSamples = samples.filter(
    (s) => s.pageStatus === "COMPLETED" || s.processingType,
  ).length;
  const pendingSamples = totalSamples - processedSamples;
  const withAliquots = samples.filter(
    (s) => s.hasChildren || s.childAliquotCount > 0,
  ).length;

  // Transform samples for SampleGrid
  const transformedSamples = samples.map((sample) => ({
    id: String(sample.sampleItemId),
    sampleItemId: sample.sampleItemId,
    externalId: sample.externalId || "-",
    accessionNumber: sample.labNo || sample.accessionNumber || "-",
    patientName: sample.patientName || "-",
    sampleType: sample.sampleType || "-",
    processingType: sample.processingType || null,
    processingTypeLabel: sample.processingType
      ? processingTypeOptions.find((p) => p.id === sample.processingType)
          ?.label || sample.processingType
      : "-",
    derivedMaterial: sample.derivedMaterial || null,
    derivedMaterialLabel: sample.derivedMaterial
      ? derivedMaterialOptions.find((d) => d.id === sample.derivedMaterial)
          ?.label || sample.derivedMaterial
      : "-",
    childAliquotCount: sample.childAliquotCount || 0,
    status: sample.pageStatus || "PENDING",
    hasChildren: sample.hasChildren || sample.childAliquotCount > 0,
    isBioequivalence: sample.isBioequivalence,
    transferToBioanalytical: sample.transferToBioanalytical,
    // Hierarchy fields from backend
    isAliquot: sample.isAliquot || false,
    nestingLevel: sample.nestingLevel || 0,
    parentSampleItemId: sample.parentSampleItemId || null,
    parentExternalId: sample.parentExternalId || null,
  }));

  // Sort samples to group parents with their children
  // Backend returns all parents first, then all children - we need to interleave them
  const sortedSamples = [];
  const parents = transformedSamples.filter((s) => !s.isAliquot);
  const children = transformedSamples.filter((s) => s.isAliquot);

  // Build a map of children by parent ID for quick lookup
  const childrenByParentId = {};
  children.forEach((child) => {
    const parentId = String(child.parentSampleItemId);
    if (!childrenByParentId[parentId]) {
      childrenByParentId[parentId] = [];
    }
    childrenByParentId[parentId].push(child);
  });

  // Add each parent followed immediately by its children
  parents.forEach((parent) => {
    sortedSamples.push(parent);
    const parentChildren =
      childrenByParentId[String(parent.sampleItemId)] || [];
    sortedSamples.push(...parentChildren);
  });

  // Split into pending and processed lists, keeping parent-child hierarchy together
  // First, identify which parents are pending vs processed
  const parentSamples = sortedSamples.filter((s) => !s.isAliquot);
  const childSamples = sortedSamples.filter((s) => s.isAliquot);

  // Build maps of parent status
  const pendingParentIds = new Set(
    parentSamples
      .filter((s) => s.status !== "COMPLETED" && s.processingType === null)
      .map((s) => String(s.sampleItemId)),
  );
  const processedParentIds = new Set(
    parentSamples
      .filter((s) => s.status === "COMPLETED" || s.processingType !== null)
      .map((s) => String(s.sampleItemId)),
  );

  // Split samples, keeping children with their parents
  const pendingSamplesList = sortedSamples.filter((s) => {
    if (!s.isAliquot) {
      // Parent: use its own status
      return pendingParentIds.has(String(s.sampleItemId));
    } else {
      // Child: use parent's status
      return pendingParentIds.has(String(s.parentSampleItemId));
    }
  });

  const processedSamplesList = sortedSamples.filter((s) => {
    if (!s.isAliquot) {
      // Parent: use its own status
      return processedParentIds.has(String(s.sampleItemId));
    } else {
      // Child: use parent's status
      return processedParentIds.has(String(s.parentSampleItemId));
    }
  });

  return (
    <div className="sample-processing-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="medlab.page.sampleProcessing.title"
            defaultMessage="Sample Processing"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="medlab.page.sampleProcessing.description"
            defaultMessage="Process samples for testing, record derived materials, and create aliquots. Select samples to record processing or create child samples."
          />
        </p>
      </div>

      {/* Progress Summary */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="medlab.page.sampleProcessing.total"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{totalSamples}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="medlab.page.sampleProcessing.processed"
                  defaultMessage="Processed"
                />
              </span>
              <span className="progress-value">{processedSamples}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="medlab.page.sampleProcessing.pending"
                  defaultMessage="Pending"
                />
              </span>
              <span className="progress-value">{pendingSamples}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="medlab.page.sampleProcessing.withAliquots"
                  defaultMessage="With Aliquots"
                />
              </span>
              <span className="progress-value">{withAliquots}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Chemistry}
          onClick={handleOpenProcessModal}
          disabled={selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="medlab.page.sampleProcessing.recordProcessing"
            defaultMessage="Record Processing ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="secondary"
          size="sm"
          renderIcon={Add}
          onClick={handleOpenAliquotModal}
          disabled={selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="medlab.page.sampleProcessing.createAliquots"
            defaultMessage="Create Aliquots ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Renew}
          onClick={loadSamplesForProcessing}
        >
          <FormattedMessage
            id="medlab.page.sampleProcessing.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          hideCloseButton={false}
          lowContrast
          onClose={() => setError(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {success && (
        <InlineNotification
          kind="success"
          title={success}
          hideCloseButton={false}
          lowContrast
          onClose={() => setSuccess(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Tabs for Pending vs Processed */}
      <Tabs
        selectedIndex={activeTab}
        onChange={({ selectedIndex }) => setActiveTab(selectedIndex)}
      >
        <TabList aria-label="Sample processing tabs">
          <Tab>
            <FormattedMessage
              id="medlab.page.sampleProcessing.tab.pending"
              defaultMessage="Pending Processing ({count})"
              values={{ count: pendingSamplesList.length }}
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="medlab.page.sampleProcessing.tab.processed"
              defaultMessage="Processed ({count})"
              values={{ count: processedSamplesList.length }}
            />
          </Tab>
        </TabList>

        <TabPanels>
          {/* Pending Samples Tab */}
          <TabPanel>
            {loading ? (
              <p>Loading samples...</p>
            ) : pendingSamplesList.length === 0 ? (
              <div className="empty-state">
                <p>
                  <FormattedMessage
                    id="medlab.page.sampleProcessing.noPending"
                    defaultMessage="No samples pending processing. All samples have been processed."
                  />
                </p>
              </div>
            ) : (
              <SampleGrid
                gridId="medlab-sample-processing-pending"
                samples={pendingSamplesList}
                selectedIds={selectedSampleIds}
                onSelectionChange={setSelectedSampleIds}
                showSelection={true}
                showHierarchy={true}
                showPatient={true}
                loading={loading}
                additionalColumns={[
                  {
                    key: "processingTypeLabel",
                    header: intl.formatMessage({
                      id: "medlab.sample.processingType",
                      defaultMessage: "Processing",
                    }),
                    render: (sample) => sample.processingTypeLabel || "-",
                  },
                  {
                    key: "derivedMaterialLabel",
                    header: intl.formatMessage({
                      id: "medlab.sample.derivedMaterial",
                      defaultMessage: "Derived Material",
                    }),
                    render: (sample) => sample.derivedMaterialLabel || "-",
                  },
                  {
                    key: "aliquots",
                    header: intl.formatMessage({
                      id: "medlab.sample.aliquots",
                      defaultMessage: "Aliquots",
                    }),
                    render: (sample) => {
                      return sample.childAliquotCount > 0 ? (
                        <Tag type="cyan">{sample.childAliquotCount}</Tag>
                      ) : (
                        "-"
                      );
                    },
                  },
                  {
                    key: "actions",
                    header: "",
                    render: (sample) => {
                      return sample.hasChildren ? (
                        <Button
                          kind="ghost"
                          size="sm"
                          hasIconOnly
                          iconDescription="View Aliquots"
                          renderIcon={View}
                          onClick={() => handleViewChildren(sample.id)}
                        />
                      ) : null;
                    },
                  },
                ]}
              />
            )}
          </TabPanel>

          {/* Processed Samples Tab */}
          <TabPanel>
            {loading ? (
              <p>Loading samples...</p>
            ) : processedSamplesList.length === 0 ? (
              <div className="empty-state">
                <p>
                  <FormattedMessage
                    id="medlab.page.sampleProcessing.noProcessed"
                    defaultMessage="No samples have been processed yet."
                  />
                </p>
              </div>
            ) : (
              <SampleGrid
                gridId="medlab-sample-processing-processed"
                samples={processedSamplesList}
                selectedIds={[]}
                showSelection={false}
                showHierarchy={true}
                showPatient={true}
                loading={loading}
                additionalColumns={[
                  {
                    key: "processingTypeLabel",
                    header: intl.formatMessage({
                      id: "medlab.sample.processingType",
                      defaultMessage: "Processing",
                    }),
                    render: (sample) => sample.processingTypeLabel || "-",
                  },
                  {
                    key: "derivedMaterialLabel",
                    header: intl.formatMessage({
                      id: "medlab.sample.derivedMaterial",
                      defaultMessage: "Derived Material",
                    }),
                    render: (sample) => sample.derivedMaterialLabel || "-",
                  },
                  {
                    key: "aliquots",
                    header: intl.formatMessage({
                      id: "medlab.sample.aliquots",
                      defaultMessage: "Aliquots",
                    }),
                    render: (sample) => {
                      return sample.childAliquotCount > 0 ? (
                        <Tag type="cyan">{sample.childAliquotCount}</Tag>
                      ) : (
                        "-"
                      );
                    },
                  },
                  {
                    key: "flags",
                    header: intl.formatMessage({
                      id: "medlab.sample.flags",
                      defaultMessage: "Flags",
                    }),
                    render: (sample) => {
                      return (
                        <>
                          {sample.isBioequivalence && (
                            <Tag type="purple" size="sm">
                              BE Study
                            </Tag>
                          )}
                          {sample.transferToBioanalytical && (
                            <Tag type="teal" size="sm">
                              Bioanalytical
                            </Tag>
                          )}
                        </>
                      );
                    },
                  },
                  {
                    key: "actions",
                    header: "",
                    render: (sample) => {
                      return sample.hasChildren ? (
                        <Button
                          kind="ghost"
                          size="sm"
                          hasIconOnly
                          iconDescription="View Aliquots"
                          renderIcon={View}
                          onClick={() => handleViewChildren(sample.id)}
                        />
                      ) : null;
                    },
                  },
                ]}
              />
            )}
          </TabPanel>
        </TabPanels>
      </Tabs>

      {/* Record Processing Modal */}
      <Modal
        open={processModalOpen}
        modalHeading={intl.formatMessage({
          id: "medlab.page.sampleProcessing.modal.title",
          defaultMessage: "Record Sample Processing",
        })}
        primaryButtonText={intl.formatMessage({
          id: "medlab.page.sampleProcessing.modal.save",
          defaultMessage: "Record Processing",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "medlab.page.sampleProcessing.modal.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setProcessModalOpen(false)}
        onRequestSubmit={handleRecordProcessing}
        primaryButtonDisabled={processing || !processingType}
        size="md"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p>
            <FormattedMessage
              id="medlab.page.sampleProcessing.modal.description"
              defaultMessage="Record processing details for {count} selected sample(s)."
              values={{ count: selectedSampleIds.length }}
            />
          </p>
        </div>

        <Dropdown
          id="processingType"
          titleText={intl.formatMessage({
            id: "medlab.page.sampleProcessing.modal.processingType",
            defaultMessage: "Processing Type *",
          })}
          label="Select processing type"
          items={processingTypeOptions}
          itemToString={(item) => (item ? item.label : "")}
          selectedItem={processingTypeOptions.find(
            (p) => p.id === processingType,
          )}
          onChange={({ selectedItem }) =>
            setProcessingType(selectedItem?.id || "")
          }
          style={{ marginBottom: "1rem" }}
        />

        <Dropdown
          id="derivedMaterial"
          titleText={intl.formatMessage({
            id: "medlab.page.sampleProcessing.modal.derivedMaterial",
            defaultMessage: "Derived Material",
          })}
          label="Select derived material (optional)"
          items={derivedMaterialOptions}
          itemToString={(item) => (item ? item.label : "")}
          selectedItem={derivedMaterialOptions.find(
            (d) => d.id === derivedMaterial,
          )}
          onChange={({ selectedItem }) =>
            setDerivedMaterial(selectedItem?.id || "")
          }
          style={{ marginBottom: "1rem" }}
        />

        <TextArea
          id="processingNotes"
          labelText={intl.formatMessage({
            id: "medlab.page.sampleProcessing.modal.notes",
            defaultMessage: "Processing Notes",
          })}
          placeholder="Enter any processing notes..."
          value={processingNotes}
          onChange={(e) => setProcessingNotes(e.target.value)}
          rows={3}
          style={{ marginBottom: "1rem" }}
        />

        <div
          style={{
            padding: "1rem",
            backgroundColor: "#f4f4f4",
            borderRadius: "4px",
            marginBottom: "1rem",
          }}
        >
          <h6 style={{ marginBottom: "0.5rem" }}>Special Handling</h6>
          <Checkbox
            id="isBioequivalence"
            labelText="Bioequivalence Study Sample"
            checked={isBioequivalence}
            onChange={(e, { checked }) => setIsBioequivalence(checked)}
          />
          <Checkbox
            id="transferToBioanalytical"
            labelText="Transfer to Bioanalytical Lab"
            checked={transferToBioanalytical}
            onChange={(e, { checked }) => setTransferToBioanalytical(checked)}
          />
        </div>
      </Modal>

      {/* Create Aliquots Modal */}
      <Modal
        open={aliquotModalOpen}
        modalHeading={intl.formatMessage({
          id: "medlab.page.sampleProcessing.aliquotModal.title",
          defaultMessage: "Create Aliquots (Child Samples)",
        })}
        primaryButtonText={intl.formatMessage({
          id: "medlab.page.sampleProcessing.aliquotModal.create",
          defaultMessage: "Create Aliquots",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "medlab.page.sampleProcessing.aliquotModal.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setAliquotModalOpen(false)}
        onRequestSubmit={handleCreateAliquots}
        primaryButtonDisabled={creating || !labelingConfirmed}
        size="md"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p>
            <FormattedMessage
              id="medlab.page.sampleProcessing.aliquotModal.description"
              defaultMessage="Create aliquots (child samples) from {count} selected parent sample(s)."
              values={{ count: selectedSampleIds.length }}
            />
          </p>
        </div>

        <NumberInput
          id="aliquotCount"
          label={intl.formatMessage({
            id: "medlab.page.sampleProcessing.aliquotModal.count",
            defaultMessage: "Number of Aliquots per Sample",
          })}
          value={aliquotCount}
          onChange={(e, { value }) => setAliquotCount(value)}
          min={1}
          max={20}
          step={1}
          style={{ marginBottom: "1rem" }}
        />

        <Dropdown
          id="containerType"
          titleText={intl.formatMessage({
            id: "medlab.page.sampleProcessing.aliquotModal.containerType",
            defaultMessage: "Container Type",
          })}
          label="Select container type"
          items={containerTypeOptions}
          itemToString={(item) => (item ? item.label : "")}
          selectedItem={containerTypeOptions.find(
            (c) => c.id === containerType,
          )}
          onChange={({ selectedItem }) =>
            setContainerType(selectedItem?.id || "cryovial")
          }
          style={{ marginBottom: "1rem" }}
        />

        <TextInput
          id="externalIdPrefix"
          labelText={intl.formatMessage({
            id: "medlab.page.sampleProcessing.aliquotModal.prefix",
            defaultMessage: "External ID Prefix",
          })}
          value={externalIdPrefix}
          onChange={(e) => setExternalIdPrefix(e.target.value)}
          helperText={intl.formatMessage(
            {
              id: "medlab.page.sampleProcessing.aliquotModal.prefixHelp",
              defaultMessage:
                "Aliquots will be named: {prefix}-{year}-{sequence}",
            },
            {
              prefix: externalIdPrefix || "PREFIX",
              year: new Date().getFullYear(),
              sequence: "001",
            },
          )}
          style={{ marginBottom: "1rem" }}
        />

        <div
          style={{
            padding: "1rem",
            backgroundColor: "#fff8e1",
            borderRadius: "4px",
            border: "1px solid #ffc107",
            marginBottom: "1rem",
          }}
        >
          <Checkbox
            id="labelingConfirmed"
            labelText={intl.formatMessage({
              id: "medlab.page.sampleProcessing.aliquotModal.labelingConfirm",
              defaultMessage:
                "I confirm that all aliquot containers have been properly labeled with matching identifiers",
            })}
            checked={labelingConfirmed}
            onChange={(e, { checked }) => setLabelingConfirmed(checked)}
          />
        </div>

        <div
          style={{
            padding: "0.5rem 1rem",
            backgroundColor: "#e0f7fa",
            borderRadius: "4px",
          }}
        >
          <p>
            <strong>
              <FormattedMessage
                id="medlab.page.sampleProcessing.aliquotModal.total"
                defaultMessage="Total aliquots to create: {total}"
                values={{ total: selectedSampleIds.length * aliquotCount }}
              />
            </strong>
          </p>
        </div>
      </Modal>

      {/* View Aliquots Modal */}
      <Modal
        open={viewChildrenModalOpen}
        modalHeading={intl.formatMessage({
          id: "medlab.page.sampleProcessing.viewModal.title",
          defaultMessage: "Aliquots (Child Samples)",
        })}
        passiveModal
        onRequestClose={() => {
          setViewChildrenModalOpen(false);
          setParentChildren([]);
        }}
      >
        {loadingChildren ? (
          <p>Loading aliquots...</p>
        ) : parentChildren.length === 0 ? (
          <p>
            <FormattedMessage
              id="medlab.page.sampleProcessing.viewModal.noChildren"
              defaultMessage="No aliquots found for this sample."
            />
          </p>
        ) : (
          <DataTable
            rows={parentChildren.map((child) => ({
              id: String(child.id),
              externalId: child.externalId || "-",
              sampleType: child.sampleType || "-",
              containerType: child.containerType || "-",
              status: child.status || "PENDING",
            }))}
            headers={[
              { key: "externalId", header: "External ID" },
              { key: "sampleType", header: "Sample Type" },
              { key: "containerType", header: "Container" },
              { key: "status", header: "Status" },
            ]}
          >
            {({
              rows,
              headers,
              getTableProps,
              getHeaderProps,
              getRowProps,
            }) => (
              <Table {...getTableProps()}>
                <TableHead>
                  <TableRow>
                    {headers.map((header) => (
                      <TableHeader
                        key={header.key}
                        {...getHeaderProps({ header })}
                      >
                        {header.header}
                      </TableHeader>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {rows.map((row) => (
                    <TableRow key={row.id} {...getRowProps({ row })}>
                      {row.cells.map((cell) => (
                        <TableCell key={cell.id}>
                          {cell.info.header === "status" ? (
                            <Tag
                              type={
                                cell.value === "COMPLETED"
                                  ? "green"
                                  : cell.value === "IN_PROGRESS"
                                    ? "blue"
                                    : "gray"
                              }
                            >
                              {cell.value}
                            </Tag>
                          ) : (
                            cell.value
                          )}
                        </TableCell>
                      ))}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </DataTable>
        )}
      </Modal>
    </div>
  );
}

export default MedLabSampleProcessingPage;
