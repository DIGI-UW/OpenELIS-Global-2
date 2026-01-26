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
  TextArea,
  Loading,
  Checkbox,
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
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import { usePermissions } from "../../../../hooks/usePermissions";
import { useTMMRDPermissions } from "../../../../hooks/useTMMRDPermissions";
import AccessDeniedMessage from "../../../common/AccessDeniedMessage";
import "../../workflow/NotebookWorkflow.css";

/**
 * TraditionalMedicineAnalyticalPage - Page 5 of the Traditional Medicine workflow.
 *
 * SRS Requirements - STAGE 6: Analytical Pathways (Dual Path)
 * - Path A: Advanced Analysis (fractionation, identification, characterization)
 * - Path B: Direct to Production
 * - Fractionation: Chromatography (column, HPLC prep)
 * - Spectral data storage (NMR, MS, IR)
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 */
function TraditionalMedicineAnalyticalPage({
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
  const { getPagePermissionLevel, canSaveData, canAccessStage5to6 } =
    useTMMRDPermissions();

  // STAGE 5-6 allowed roles per TMMRD SRS Section 11 - Researchers lead analytics
  const allowedRoles = [
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
        page="Analytical Pathways"
        reason="This page requires specific Traditional Medicine analytical roles to access."
        requiredRoles={allowedRoles}
      />
    );
  }

  // Get user's action-level permission for this page
  const pagePermissionLevel = getPagePermissionLevel("Analytical Pathways");
  const canEditData = canSaveData(pagePermissionLevel);

  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);

  const [pathwayModalOpen, setPathwayModalOpen] = useState(false);
  const [isApplying, setIsApplying] = useState(false);
  const [isCompleting, setIsCompleting] = useState(false);

  const [selectedPath, setSelectedPath] = useState(null);
  const [fractionationMethod, setFractionationMethod] = useState(null);
  const [analysisNotes, setAnalysisNotes] = useState("");

  const pathOptions = [
    {
      id: "path_a",
      label: "Path A: Advanced Analysis (Before Production)",
    },
    { id: "path_b", label: "Path B: Direct to Production" },
  ];

  const fractionationOptions = [
    { id: "column_chromatography", label: "Column Chromatography" },
    { id: "hplc_prep", label: "HPLC Prep" },
    { id: "tlc", label: "TLC" },
    { id: "gcc_ms", label: "GC/MS" },
    { id: "lcms", label: "LC/MS" },
    { id: "nmr", label: "NMR" },
    { id: "ir", label: "IR/FTIR" },
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
                  localName: s.data?.localName,
                  scientificName: s.data?.scientificName,
                  sampleCategory: s.data?.sampleCategory,
                  plantPart: s.data?.plantPart,
                  collectionDate: s.data?.collectionDate,
                  intendedUse: s.data?.intendedUse,
                  selectedPath: s.data?.selectedPath,
                  fractionationMethod: s.data?.fractionationMethod,
                  analysisNotes: s.data?.analysisNotes,
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
    setSelectedPath(null);
    setFractionationMethod(null);
    setAnalysisNotes("");
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
    setPathwayModalOpen(true);
  }, [selectedSampleIds, intl, resetForm, notify]);

  const applyPathway = useCallback(() => {
    if (!selectedPath) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.analytical.error.pathRequired",
          defaultMessage: "Please select an analytical pathway.",
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

    // First, apply the analytical pathway data to JSONB using bulk apply endpoint
    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds,
        data: {
          selectedPath: selectedPath.id,
          selectedPathLabel: selectedPath.label,
          fractionationMethod: fractionationMethod?.id || null,
          analysisNotes,
        },
      }),
      (statusCode) => {
        if (statusCode === 200) {
          // After data is saved, update the status to IN_PROGRESS
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
                      id: "notebook.page.tradmed.analytical.success",
                      defaultMessage: "Updated pathway for {count} sample(s).",
                    },
                    {
                      count: selectedSampleIds.length,
                    },
                  ),
                });
                setPathwayModalOpen(false);
                setSelectedSampleIds([]);
                loadPageSamples();
                if (onProgressUpdate) onProgressUpdate();
              } else {
                notify({
                  kind: NotificationKinds.error,
                  title: intl.formatMessage({
                    id: "notebook.page.tradmed.error.statusUpdate",
                    defaultMessage:
                      "Pathway data saved but failed to update status.",
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
              id: "notebook.page.tradmed.error.pathwayUpdate",
              defaultMessage: "Failed to update pathway for selected samples.",
            }),
          });
        }
      },
    );
  }, [
    selectedPath,
    fractionationMethod,
    analysisNotes,
    hasRealPageId,
    pageData?.id,
    selectedSampleIds,
    intl,
    loadPageSamples,
    onProgressUpdate,
    notify,
  ]);

  // Handle marking analyzed samples complete (moving to next page)
  const handleMarkComplete = useCallback(() => {
    // Filter samples that can be marked complete: selected, have selected path, and not already completed
    const samplesToComplete = samples.filter(
      (s) =>
        selectedSampleIds.includes(s.id) &&
        s.selectedPath &&
        s.status !== "COMPLETED",
    );

    if (samplesToComplete.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.tradmed.analytical.noEligibleSamples",
          defaultMessage:
            "Selected samples must have analytical pathway selected before completing.",
        }),
      });
      return;
    }

    setIsCompleting(true);

    const sampleIds = samplesToComplete.map((s) => parseInt(s.id, 10));

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({ sampleIds: sampleIds, status: "COMPLETED" }),
      (response) => {
        setIsCompleting(false);

        if (response && response.success) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage(
              {
                id: "notebook.tradmed.analytical.completeSuccess",
                defaultMessage:
                  "Successfully marked {count} samples as complete.",
              },
              { count: response.updatedCount || sampleIds.length },
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
            title:
              response?.error ||
              intl.formatMessage({
                id: "notebook.tradmed.analytical.completeFailed",
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
    () => samples.filter((s) => !s.selectedPath),
    [samples],
  );
  const processedInProgressSamples = useMemo(
    () => samples.filter((s) => s.selectedPath && s.status !== "COMPLETED"),
    [samples],
  );
  const processedCompletedSamples = useMemo(
    () => samples.filter((s) => s.selectedPath && s.status === "COMPLETED"),
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
    <div className="tradmed-analytical-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.tradmed.analytical.title"
            defaultMessage="Analytical Pathways - Dual Path Selection"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.tradmed.analytical.description"
            defaultMessage="Select analytical pathway: Path A (Advanced Analysis with fractionation) or Path B (Direct to Production). Perform fractionation, spectroscopy, and characterization as needed."
          />
        </p>
      </div>

      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.analytical.pending"
                  defaultMessage="Awaiting Pathway Assignment"
                />
              </span>
              <span className="progress-value">{pendingSamples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.analytical.assigned"
                  defaultMessage="Pathway Assigned"
                />
              </span>
              <span className="progress-value">
                {processedInProgressSamples.length}
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
            id="notebook.page.tradmed.analytical.selectPathway"
            defaultMessage="Select Pathway ({count})"
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
            id="notebook.tradmed.analytical.markComplete"
            defaultMessage="Mark Complete ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>
      </div>

      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.tradmed.analytical.pending.title"
              defaultMessage="Samples Awaiting Pathway Assignment"
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
                  id="notebook.page.tradmed.analytical.pending.empty"
                  defaultMessage="No samples awaiting pathway assignment."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="pending-pathway"
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
                { key: "intendedUse", header: "Intended Use" },
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

      {/* Assigned Pathway Section - IN PROGRESS */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.tradmed.analytical.assigned.inProgress.title"
              defaultMessage="Pathway Assigned (Pending Completion)"
            />
            <Tag type="blue" size="sm" className="count-tag">
              {processedInProgressSamples.length}
            </Tag>
          </h5>
        </div>
        <div className="sample-grid-container">
          {!loading && processedInProgressSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.tradmed.analytical.assigned.empty"
                  defaultMessage="No samples with pathway assigned yet."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="assigned-in-progress-pathway"
              samples={processedInProgressSamples}
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
                { key: "selectedPath", header: "Pathway" },
                { key: "fractionationMethod", header: "Fractionation" },
                { key: "analysisNotes", header: "Analysis Notes" },
                { key: "plantPart", header: "Plant Part" },
                { key: "collectionDate", header: "Collection Date" },
                { key: "intendedUse", header: "Intended Use" },
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

      {/* Assigned Pathway Section - COMPLETED */}
      {processedCompletedSamples.length > 0 && (
        <div className="sample-table-section">
          <div className="table-section-header">
            <h5>
              <FormattedMessage
                id="notebook.page.tradmed.analytical.assigned.completed.title"
                defaultMessage="Analytical Pathway Completion Finalized"
              />
              <Tag type="green" size="sm" className="count-tag">
                {processedCompletedSamples.length}
              </Tag>
            </h5>
          </div>
          <div className="sample-grid-container">
            <SampleGrid
              gridId="assigned-completed-pathway"
              samples={processedCompletedSamples}
              showSelection={false}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "localName", header: "Local Name" },
                { key: "scientificName", header: "Scientific Name" },
                { key: "sampleCategory", header: "Category" },
                { key: "selectedPath", header: "Pathway" },
                { key: "fractionationMethod", header: "Fractionation" },
                { key: "analysisNotes", header: "Analysis Notes" },
                { key: "plantPart", header: "Plant Part" },
                { key: "collectionDate", header: "Collection Date" },
                { key: "intendedUse", header: "Intended Use" },
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
        open={pathwayModalOpen}
        onRequestClose={() => setPathwayModalOpen(false)}
        onRequestSubmit={applyPathway}
        modalHeading={intl.formatMessage({
          id: "notebook.page.tradmed.analytical.modal.title",
          defaultMessage: "Select Analytical Pathway",
        })}
        primaryButtonText={
          isApplying
            ? intl.formatMessage({
                id: "label.assigning",
                defaultMessage: "Assigning...",
              })
            : intl.formatMessage({
                id: "notebook.page.tradmed.analytical.modal.select",
                defaultMessage: "Select Pathway",
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
          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="pathway"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.analytical.modal.pathway",
                defaultMessage: "Analytical Pathway *",
              })}
              label="Select..."
              items={pathOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={selectedPath}
              onChange={({ selectedItem }) => setSelectedPath(selectedItem)}
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="fractionation"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.analytical.modal.fractionation",
                defaultMessage: "Fractionation / Analysis Method (if Path A)",
              })}
              label="Select..."
              items={fractionationOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={fractionationMethod}
              onChange={({ selectedItem }) =>
                setFractionationMethod(selectedItem)
              }
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="analysis-notes"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.analytical.modal.notes",
                defaultMessage: "Analysis Plan Notes",
              })}
              value={analysisNotes}
              onChange={(e) => setAnalysisNotes(e.target.value)}
              rows={3}
              placeholder="Expected analyses, spectral data to collect, etc."
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default TraditionalMedicineAnalyticalPage;
