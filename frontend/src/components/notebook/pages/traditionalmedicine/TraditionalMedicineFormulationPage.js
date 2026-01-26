import React, {
  useState,
  useEffect,
  useRef,
  useCallback,
  useMemo,
  useContext,
} from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  Tag,
  Modal,
  Dropdown,
  TextInput,
  TextArea,
  Loading,
} from "@carbon/react";
import {
  Renew,
  CheckmarkFilled,
  Edit,
  Archive,
  Pending,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import { usePermissions } from "../../../../hooks/usePermissions";
import { useTMMRDPermissions } from "../../../../hooks/useTMMRDPermissions";
import AccessDeniedMessage from "../../../common/AccessDeniedMessage";
import "../../workflow/NotebookWorkflow.css";

/**
 * TraditionalMedicineFormulationPage - Page 7 of the Traditional Medicine workflow.
 *
 * SRS Requirements - STAGE 8: Formulation
 * - Formulation types (capsules, tinctures, ointments, teas, syrups)
 * - Batch number, manufacturing date
 * - QC results (stability, microbial, heavy metals)
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 */
function TraditionalMedicineFormulationPage({
  entryId,
  pageData,
  progress: _progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const componentMounted = useRef(false);
  const { hasAnyRole } = usePermissions();

  // TMMRD permissions per SRS Section 11
  const { getPagePermissionLevel, canSaveData, canAccessStage7 } =
    useTMMRDPermissions();

  // STAGE 7 allowed roles per TMMRD SRS Section 11 - Pharmacognosists lead formulation
  const allowedRoles = [
    "Lab Technician",
    "Researcher",
    "Pharmacognosist",
    "Lab Manager",
    "Principal Investigator",
  ];

  const canAccessPage = hasAnyRole(allowedRoles);

  // Check page access - show access denied if user lacks required roles
  if (!canAccessPage) {
    return (
      <AccessDeniedMessage
        page="Formulation Development"
        reason="This page requires specific Traditional Medicine formulation roles to access."
        requiredRoles={allowedRoles}
      />
    );
  }

  // Get user's action-level permission for this page
  const pagePermissionLevel = getPagePermissionLevel("Formulation Development");
  const canEditData = canSaveData(pagePermissionLevel);

  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);

  const [formulationModalOpen, setFormulationModalOpen] = useState(false);
  const [isApplying, setIsApplying] = useState(false);
  const [isCompleting, setIsCompleting] = useState(false);

  const [formulationType, setFormulationType] = useState(null);
  const [batchNumber, setBatchNumber] = useState("");
  const [manufacturingDate, setManufacturingDate] = useState("");
  const [ingredients, setIngredients] = useState("");
  const [manufacturingSteps, setManufacturingSteps] = useState("");
  const [productSpecifications, setProductSpecifications] = useState("");
  const [stabilityTesting, setStabilityTesting] = useState("");
  const [microbialTesting, setMicrobialTesting] = useState("");
  const [heavyMetalTesting, setHeavyMetalTesting] = useState("");
  const [pesticidesTestingNotes, setPesticidesTestingNotes] = useState("");
  const [activeConstituentQuantification, setActiveConstituentQuantification] =
    useState("");
  const [qcNotes, setQcNotes] = useState("");

  const formulationOptions = [
    { id: "capsule", label: "Capsules" },
    { id: "tincture", label: "Tinctures" },
    { id: "ointment", label: "Ointments/Creams" },
    { id: "tea", label: "Teas" },
    { id: "syrup", label: "Syrups" },
    { id: "other", label: "Other" },
  ];

  // Notification callback
  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, message }) => {
      setNotificationVisible(true);
      addNotification({ kind, title, message });
    },
    [addNotification, setNotificationVisible],
  );

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      setLoading(false);
      return;
    }

    setLoading(true);

    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (componentMounted.current) {
          let samplesToProcess = [];

          // Handle both array and object responses from API
          if (response) {
            if (Array.isArray(response)) {
              samplesToProcess = response;
            } else if (response.samples && Array.isArray(response.samples)) {
              samplesToProcess = response.samples;
            }
          }

          setSamples(
            samplesToProcess.length > 0
              ? samplesToProcess.map((s) => ({
                  id: String(s.id || s.sampleItemId),
                  externalId: s.externalId,
                  accessionNumber: s.accessionNumber,
                  status: s.pageStatus || s.status || "PENDING",
                  // Identification
                  localName: s.data?.localName,
                  scientificName: s.data?.scientificName,
                  sampleCategory: s.data?.sampleCategory,
                  plantPart: s.data?.plantPart,
                  collectionDate: s.data?.collectionDate,
                  // Extraction & Preparation data from previous pages
                  extractYieldPercentage: s.data?.extractYieldPercentage,
                  solventLabel: s.data?.solventLabel,
                  dryingMethodLabel: s.data?.dryingMethodLabel,
                  processingMethodLabel: s.data?.processingMethodLabel,
                  // Formulation data from Page 7
                  formulationType: s.data?.formulationType,
                  batchNumber: s.data?.batchNumber,
                  manufacturingDate: s.data?.manufacturingDate,
                  ingredients: s.data?.ingredients,
                  manufacturingSteps: s.data?.manufacturingSteps,
                  productSpecifications: s.data?.productSpecifications,
                  stabilityTesting: s.data?.stabilityTesting,
                  microbialTesting: s.data?.microbialTesting,
                  heavyMetalTesting: s.data?.heavyMetalTesting,
                  pesticidesTestingNotes: s.data?.pesticidesTestingNotes,
                  activeConstituentQuantification:
                    s.data?.activeConstituentQuantification,
                  qcNotes: s.data?.qcNotes,
                }))
              : [],
          );
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id, loadPageSamples]);

  const resetForm = useCallback(() => {
    setFormulationType(null);
    setBatchNumber("");
    setManufacturingDate("");
    setIngredients("");
    setManufacturingSteps("");
    setProductSpecifications("");
    setStabilityTesting("");
    setMicrobialTesting("");
    setHeavyMetalTesting("");
    setPesticidesTestingNotes("");
    setActiveConstituentQuantification("");
    setQcNotes("");
  }, []);

  const openModal = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      });
      return;
    }
    resetForm();
    setFormulationModalOpen(true);
  }, [selectedSampleIds, intl, resetForm, notify]);

  const applyFormulation = useCallback(() => {
    if (!formulationType) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.formulation.error.typeRequired",
          defaultMessage: "Please select formulation type.",
        }),
      });
      return;
    }

    if (!batchNumber) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.formulation.error.batchRequired",
          defaultMessage: "Please enter batch number.",
        }),
      });
      return;
    }

    if (!hasRealPageId) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      });
      return;
    }

    setIsApplying(true);

    const sampleIds = selectedSampleIds.map((id) => parseInt(id, 10));

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds,
        data: {
          formulationType: formulationType.id,
          formulationTypeLabel: formulationType.label,
          batchNumber,
          manufacturingDate,
          ingredients,
          manufacturingSteps,
          productSpecifications,
          stabilityTesting,
          microbialTesting,
          heavyMetalTesting,
          pesticidesTestingNotes,
          activeConstituentQuantification,
          qcNotes,
        },
      }),
      (statusCode) => {
        if (statusCode === 200) {
          // Update sample status using bulk endpoint after formulation
          postToOpenElisServer(
            `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
            JSON.stringify({
              sampleIds,
              status: "IN_PROGRESS",
            }),
            (statusCodeUpdate) => {
              setIsApplying(false);
              if (statusCodeUpdate === 200) {
                notify({
                  kind: NotificationKinds.success,
                  title: intl.formatMessage(
                    {
                      id: "notebook.page.tradmed.formulation.success",
                      defaultMessage:
                        "Recorded formulation for {count} sample(s).",
                    },
                    {
                      count: selectedSampleIds.length,
                    },
                  ),
                });
                setFormulationModalOpen(false);
                setSelectedSampleIds([]);
                loadPageSamples();
                if (onProgressUpdate) onProgressUpdate();
              } else {
                notify({
                  kind: NotificationKinds.error,
                  title: intl.formatMessage({
                    id: "notebook.page.tradmed.error.statusUpdate",
                    defaultMessage:
                      "Formulation recorded but failed to update sample status.",
                  }),
                });
              }
            },
          );
        } else {
          setIsApplying(false);
          notify({
            kind: NotificationKinds.error,
            title: intl.formatMessage({
              id: "notebook.page.tradmed.formulation.error.failed",
              defaultMessage: "Failed to record formulation data.",
            }),
          });
        }
      },
    );
  }, [
    formulationType,
    batchNumber,
    manufacturingDate,
    ingredients,
    manufacturingSteps,
    productSpecifications,
    stabilityTesting,
    microbialTesting,
    heavyMetalTesting,
    pesticidesTestingNotes,
    activeConstituentQuantification,
    qcNotes,
    hasRealPageId,
    pageData?.id,
    selectedSampleIds,
    intl,
    loadPageSamples,
    onProgressUpdate,
    notify,
  ]);

  // Handle marking formulated samples complete (moving to next page)
  const handleMarkComplete = useCallback(() => {
    // Filter samples that can be marked complete: selected, formulated, and not already completed
    const samplesToComplete = samples.filter(
      (s) =>
        selectedSampleIds.includes(s.id) &&
        s.formulationType &&
        s.status !== "COMPLETED",
    );

    if (samplesToComplete.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.tradmed.formulation.noEligibleSamples",
          defaultMessage:
            "Selected samples must have formulation type recorded before completing.",
        }),
      });
      return;
    }

    setIsCompleting(true);

    const sampleIds = samplesToComplete.map((s) => parseInt(s.id, 10));

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({ sampleIds, status: "COMPLETED" }),
      (statusCode) => {
        setIsCompleting(false);

        if (statusCode === 200) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage(
              {
                id: "notebook.tradmed.formulation.completeSuccess",
                defaultMessage:
                  "Successfully marked {count} samples as complete.",
              },
              { count: sampleIds.length },
            ),
          });
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          notify({
            kind: NotificationKinds.error,
            title: intl.formatMessage({
              id: "notebook.tradmed.formulation.completeFailed",
              defaultMessage: "Failed to mark samples complete.",
            }),
          });
        }
      },
    );
  }, [
    selectedSampleIds,
    samples,
    pageData?.id,
    intl,
    notify,
    loadPageSamples,
    onProgressUpdate,
  ]);

  const pendingSamples = useMemo(
    () => samples.filter((s) => !s.formulationType),
    [samples],
  );
  const formulatedInProgressSamples = useMemo(
    () => samples.filter((s) => s.formulationType && s.status !== "COMPLETED"),
    [samples],
  );
  const formulatedCompletedSamples = useMemo(
    () => samples.filter((s) => s.formulationType && s.status === "COMPLETED"),
    [samples],
  );

  // Helper to render sample status - simple status display matching API response
  const renderStatus = (sample) => {
    const status = sample.status || "PENDING";

    switch (status.toUpperCase()) {
      case "COMPLETED":
        return (
          <Tag type="green" size="sm" renderIcon={CheckmarkFilled}>
            <FormattedMessage
              id="notebook.tradmed.status.completed"
              defaultMessage="Completed"
            />
          </Tag>
        );
      case "IN_PROGRESS":
        return (
          <Tag type="blue" size="sm" renderIcon={Archive}>
            <FormattedMessage
              id="notebook.tradmed.status.inProgress"
              defaultMessage="In Progress"
            />
          </Tag>
        );
      case "SKIPPED":
        return (
          <Tag type="gray" size="sm">
            <FormattedMessage
              id="notebook.tradmed.status.skipped"
              defaultMessage="Skipped"
            />
          </Tag>
        );
      default:
        return (
          <Tag type="gray" size="sm" renderIcon={Pending}>
            <FormattedMessage
              id="notebook.tradmed.status.pending"
              defaultMessage="Pending"
            />
          </Tag>
        );
    }
  };

  return (
    <div className="tradmed-formulation-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.tradmed.formulation.title"
            defaultMessage="Formulation of Medical Product"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.tradmed.formulation.description"
            defaultMessage="Record formulation details, batch numbers, ingredients, and QC results for final medical product."
          />
        </p>
      </div>

      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.formulation.pending"
                  defaultMessage="Awaiting Formulation"
                />
              </span>
              <span className="progress-value">{pendingSamples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.formulation.formulated"
                  defaultMessage="Formulated"
                />
              </span>
              <span className="progress-value">
                {formulatedInProgressSamples.length}
              </span>
            </Tile>
          </div>
        </Column>
      </Grid>

      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Edit}
          onClick={openModal}
          disabled={selectedSampleIds.length === 0 || !hasRealPageId}
        >
          <FormattedMessage
            id="notebook.page.tradmed.formulation.recordFormulation"
            defaultMessage="Record Formulation ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={CheckmarkFilled}
          onClick={handleMarkComplete}
          disabled={
            selectedSampleIds.length === 0 || isCompleting || !hasRealPageId
          }
        >
          <FormattedMessage
            id="notebook.tradmed.formulation.markComplete"
            defaultMessage="Mark Complete ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="ghost"
          size="sm"
          renderIcon={Renew}
          onClick={loadPageSamples}
          disabled={loading}
        >
          <FormattedMessage
            id="notebook.page.tradmed.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.tradmed.formulation.pending.title"
              defaultMessage="Samples Awaiting Formulation"
            />
            <Tag type="blue" size="sm" className="count-tag">
              {pendingSamples.length}
            </Tag>
          </h5>
        </div>
        <div className="sample-grid-container">
          {!loading && pendingSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.tradmed.formulation.pending.empty"
                  defaultMessage="No samples awaiting formulation."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="pending-formulation"
              samples={pendingSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              showSelection={true}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "localName", header: "Local Name" },
                { key: "scientificName", header: "Scientific Name" },
                { key: "sampleCategory", header: "Category" },
                { key: "plantPart", header: "Plant Part" },
                { key: "collectionDate", header: "Collection Date" },
                {
                  key: "extractYieldPercentage",
                  header: "Yield %",
                },
                {
                  key: "solventLabel",
                  header: "Solvent",
                },
                {
                  key: "dryingMethodLabel",
                  header: "Drying Method",
                },
                {
                  key: "processingMethodLabel",
                  header: "Processing",
                },
                {
                  key: "status",
                  header: intl.formatMessage({
                    id: "notebook.tradmed.column.status",
                    defaultMessage: "Status",
                  }),
                  render: (_value, sample) => renderStatus(sample),
                },
              ]}
            />
          )}
        </div>
      </div>

      {/* Formulated Products Section - IN PROGRESS */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.tradmed.formulation.formulated.inProgress.title"
              defaultMessage="Formulated (Pending Completion)"
            />
            <Tag type="blue" size="sm" className="count-tag">
              {formulatedInProgressSamples.length}
            </Tag>
          </h5>
        </div>
        <div className="sample-grid-container">
          {!loading && formulatedInProgressSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.tradmed.formulation.formulated.empty"
                  defaultMessage="No formulated products yet."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="formulated-in-progress-samples"
              samples={formulatedInProgressSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              showSelection={true}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "localName", header: "Local Name" },
                { key: "scientificName", header: "Scientific Name" },
                { key: "sampleCategory", header: "Category" },
                { key: "plantPart", header: "Plant Part" },
                {
                  key: "extractYieldPercentage",
                  header: "Yield %",
                },
                {
                  key: "solventLabel",
                  header: "Solvent",
                },
                {
                  key: "dryingMethodLabel",
                  header: "Drying Method",
                },
                { key: "formulationType", header: "Formulation Type" },
                { key: "batchNumber", header: "Batch #" },
                { key: "manufacturingDate", header: "Manufacturing Date" },
                { key: "ingredients", header: "Ingredients" },
                { key: "manufacturingSteps", header: "Manufacturing Steps" },
                { key: "productSpecifications", header: "Product Specs" },
                { key: "stabilityTesting", header: "Stability Testing" },
                { key: "microbialTesting", header: "Microbial Testing" },
                { key: "heavyMetalTesting", header: "Heavy Metal Testing" },
                {
                  key: "pesticidesTestingNotes",
                  header: "Pesticide Residues",
                },
                {
                  key: "activeConstituentQuantification",
                  header: "Active Constituents",
                },
                { key: "qcNotes", header: "QC Notes" },
                {
                  key: "status",
                  header: intl.formatMessage({
                    id: "notebook.tradmed.column.status",
                    defaultMessage: "Status",
                  }),
                  render: (_value, sample) => renderStatus(sample),
                },
              ]}
            />
          )}
        </div>
      </div>

      {/* Formulated Products Section - COMPLETED */}
      {formulatedCompletedSamples.length > 0 && (
        <div className="sample-table-section">
          <div className="table-section-header">
            <h5>
              <FormattedMessage
                id="notebook.page.tradmed.formulation.formulated.completed.title"
                defaultMessage="Formulation Completion Finalized"
              />
              <Tag type="green" size="sm" className="count-tag">
                {formulatedCompletedSamples.length}
              </Tag>
            </h5>
          </div>
          <div className="sample-grid-container">
            <SampleGrid
              gridId="formulated-completed-samples"
              samples={formulatedCompletedSamples}
              showSelection={false}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "localName", header: "Local Name" },
                { key: "scientificName", header: "Scientific Name" },
                { key: "sampleCategory", header: "Category" },
                { key: "plantPart", header: "Plant Part" },
                {
                  key: "extractYieldPercentage",
                  header: "Yield %",
                },
                {
                  key: "solventLabel",
                  header: "Solvent",
                },
                {
                  key: "dryingMethodLabel",
                  header: "Drying Method",
                },
                { key: "formulationType", header: "Formulation Type" },
                { key: "batchNumber", header: "Batch #" },
                { key: "manufacturingDate", header: "Manufacturing Date" },
                { key: "ingredients", header: "Ingredients" },
                { key: "manufacturingSteps", header: "Manufacturing Steps" },
                { key: "productSpecifications", header: "Product Specs" },
                { key: "stabilityTesting", header: "Stability Testing" },
                { key: "microbialTesting", header: "Microbial Testing" },
                { key: "heavyMetalTesting", header: "Heavy Metal Testing" },
                {
                  key: "pesticidesTestingNotes",
                  header: "Pesticide Residues",
                },
                {
                  key: "activeConstituentQuantification",
                  header: "Active Constituents",
                },
                { key: "qcNotes", header: "QC Notes" },
                {
                  key: "status",
                  header: intl.formatMessage({
                    id: "notebook.tradmed.column.status",
                    defaultMessage: "Status",
                  }),
                  render: (_value, sample) => renderStatus(sample),
                },
              ]}
            />
          </div>
        </div>
      )}

      <Modal
        open={formulationModalOpen}
        onRequestClose={() => setFormulationModalOpen(false)}
        onRequestSubmit={applyFormulation}
        modalHeading={intl.formatMessage({
          id: "notebook.page.tradmed.formulation.modal.title",
          defaultMessage: "Record Formulation Details",
        })}
        primaryButtonText={
          isApplying
            ? intl.formatMessage({
                id: "label.recording",
                defaultMessage: "Recording...",
              })
            : intl.formatMessage({
                id: "notebook.page.tradmed.formulation.modal.record",
                defaultMessage: "Record Formulation",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        primaryButtonDisabled={isApplying}
        size="md"
      >
        {isApplying && <Loading withOverlay={false} small />}

        <Grid narrow>
          {/* Formulation Process Section */}
          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="formulation-type"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.modal.type",
                defaultMessage: "Formulation Type *",
              })}
              label="Select..."
              items={formulationOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={formulationType}
              onChange={({ selectedItem }) => setFormulationType(selectedItem)}
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="batch-number"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.modal.batchNumber",
                defaultMessage: "Batch Number *",
              })}
              value={batchNumber}
              onChange={(e) => setBatchNumber(e.target.value)}
              placeholder="e.g., FORM-2025-001"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="manufacturing-date"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.modal.mfgDate",
                defaultMessage: "Manufacturing Date",
              })}
              type="date"
              value={manufacturingDate}
              onChange={(e) => setManufacturingDate(e.target.value)}
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="ingredients"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.modal.ingredients",
                defaultMessage: "Ingredients & Concentrations *",
              })}
              value={ingredients}
              onChange={(e) => setIngredients(e.target.value)}
              rows={2}
              placeholder="List: Extract + excipients/carriers with quantities and concentrations"
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="manufacturing-steps"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.modal.manufacturingSteps",
                defaultMessage: "Manufacturing Steps",
              })}
              value={manufacturingSteps}
              onChange={(e) => setManufacturingSteps(e.target.value)}
              rows={2}
              placeholder="Document each manufacturing step in sequence"
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="product-specifications"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.modal.productSpecs",
                defaultMessage: "Product Specifications",
              })}
              value={productSpecifications}
              onChange={(e) => setProductSpecifications(e.target.value)}
              rows={2}
              placeholder="Physical properties, color, odor, appearance, etc."
            />
          </Column>

          {/* Product Testing Section */}
          <Column lg={16} md={16} sm={4} style={{ marginBottom: "0.5rem" }}>
            <div
              style={{
                fontSize: "0.875rem",
                fontWeight: 600,
                marginBottom: "0.5rem",
              }}
            >
              Product Testing:
            </div>
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="stability-testing"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.modal.stabilityTesting",
                defaultMessage: "Stability Testing",
              })}
              label="Select..."
              items={[
                { id: "accelerated", label: "Accelerated Conditions" },
                { id: "realtime", label: "Real-Time Conditions" },
                { id: "both", label: "Both Accelerated & Real-Time" },
                { id: "other", label: "Other" },
              ]}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={
                [
                  { id: "accelerated", label: "Accelerated Conditions" },
                  { id: "realtime", label: "Real-Time Conditions" },
                  { id: "both", label: "Both Accelerated & Real-Time" },
                  { id: "other", label: "Other" },
                ].find((t) => t.id === stabilityTesting) || null
              }
              onChange={({ selectedItem }) =>
                setStabilityTesting(selectedItem?.id || "")
              }
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="microbial-testing"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.modal.microbialTesting",
                defaultMessage: "Microbial Testing",
              })}
              label="Select..."
              items={[
                { id: "aerobic", label: "Aerobic Bacteria" },
                { id: "fungi", label: "Fungi" },
                { id: "yeast", label: "Yeast" },
                { id: "pathogens", label: "Pathogens (E. coli, Salmonella)" },
                { id: "comprehensive", label: "Comprehensive Panel" },
              ]}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={
                [
                  { id: "aerobic", label: "Aerobic Bacteria" },
                  { id: "fungi", label: "Fungi" },
                  { id: "yeast", label: "Yeast" },
                  { id: "pathogens", label: "Pathogens (E. coli, Salmonella)" },
                  { id: "comprehensive", label: "Comprehensive Panel" },
                ].find((t) => t.id === microbialTesting) || null
              }
              onChange={({ selectedItem }) =>
                setMicrobialTesting(selectedItem?.id || "")
              }
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="heavy-metal-testing"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.modal.heavyMetalTesting",
                defaultMessage: "Heavy Metal Testing",
              })}
              label="Select..."
              items={[
                { id: "pb", label: "Lead (Pb)" },
                { id: "cd", label: "Cadmium (Cd)" },
                { id: "hg", label: "Mercury (Hg)" },
                { id: "as", label: "Arsenic (As)" },
                { id: "comprehensive", label: "Comprehensive (ICP-MS)" },
              ]}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={
                [
                  { id: "pb", label: "Lead (Pb)" },
                  { id: "cd", label: "Cadmium (Cd)" },
                  { id: "hg", label: "Mercury (Hg)" },
                  { id: "as", label: "Arsenic (As)" },
                  { id: "comprehensive", label: "Comprehensive (ICP-MS)" },
                ].find((t) => t.id === heavyMetalTesting) || null
              }
              onChange={({ selectedItem }) =>
                setHeavyMetalTesting(selectedItem?.id || "")
              }
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="pesticides-testing"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.modal.pesticidesTesting",
                defaultMessage: "Pesticide Residues",
              })}
              label="Select..."
              items={[
                { id: "notdetected", label: "Not Detected" },
                { id: "detected", label: "Detected" },
                { id: "nottested", label: "Not Tested" },
              ]}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={
                [
                  { id: "notdetected", label: "Not Detected" },
                  { id: "detected", label: "Detected" },
                  { id: "nottested", label: "Not Tested" },
                ].find((t) => t.id === pesticidesTestingNotes) || null
              }
              onChange={({ selectedItem }) =>
                setPesticidesTestingNotes(selectedItem?.id || "")
              }
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="active-constituent-quantification"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.modal.activeConstituent",
                defaultMessage: "Analysis Method",
              })}
              label="Select..."
              items={[
                { id: "hplc", label: "HPLC" },
                { id: "gcms", label: "GC-MS" },
                { id: "uplc", label: "UPLC" },
                { id: "lcms", label: "LC-MS/MS" },
                { id: "other", label: "Other" },
              ]}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={
                [
                  { id: "hplc", label: "HPLC" },
                  { id: "gcms", label: "GC-MS" },
                  { id: "uplc", label: "UPLC" },
                  { id: "lcms", label: "LC-MS/MS" },
                  { id: "other", label: "Other" },
                ].find((t) => t.id === activeConstituentQuantification) || null
              }
              onChange={({ selectedItem }) =>
                setActiveConstituentQuantification(selectedItem?.id || "")
              }
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="qc-notes"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.formulation.modal.qcNotes",
                defaultMessage: "QC Results & Documentation",
              })}
              value={qcNotes}
              onChange={(e) => setQcNotes(e.target.value)}
              rows={1}
              placeholder="Summary of all QC results, batch records, and documentation status"
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default TraditionalMedicineFormulationPage;
