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
  TextInput,
} from "@carbon/react";
import {
  Renew,
  CheckmarkFilled,
  Edit,
  Download,
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
import { Permissions } from "../../../../constants/roles";
import PermissionGate from "../../../security/PermissionGate";
import "../../workflow/NotebookWorkflow.css";

/**
 * TraditionalMedicineArchivalPage - Page 9 of the Traditional Medicine workflow.
 *
 * SRS Requirements - STAGE 9: Reporting & Archival
 * - Data archival tracking (digital, physical, hybrid)
 * - Herbarium specimen archival with location tracking
 * - Metadata locking for regulatory compliance
 * - Long-term data preservation
 *
 * Note: Disposal is handled on the Formulation page using the bulk update endpoint.
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 */
function TraditionalMedicineArchivalPage({
  entryId,
  pageData,
  progress: _progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const componentMounted = useRef(false);

  // All hooks and state must be declared before any conditional returns (React Hooks Rule)
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);

  const [archivalModalOpen, setArchivalModalOpen] = useState(false);
  const [isApplying, setIsApplying] = useState(false);

  const [archiveType, setArchiveType] = useState(null);
  const [archiveLocation, setArchiveLocation] = useState("");
  const [retentionYears, setRetentionYears] = useState("");
  const [archivalNotes, setArchivalNotes] = useState("");
  const [generateReport, setGenerateReport] = useState(false);

  const archiveTypeOptions = [
    { id: "digital", label: "Digital Archive" },
    { id: "physical", label: "Physical Archive" },
    { id: "hybrid", label: "Hybrid (Digital + Physical)" },
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
                  data: s.data || {},
                  // Archival fields
                  archiveType: s.data?.archiveType,
                  archiveTypeLabel: s.data?.archiveTypeLabel,
                  archiveLocation: s.data?.archiveLocation,
                  retentionYears: s.data?.retentionYears,
                  archivalNotes: s.data?.archivalNotes,
                  archivedAt: s.data?.archivedAt,
                  // Herbarium archival fields
                  herbariumArchival: s.data?.herbariumArchival,
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
    setArchiveType(null);
    setArchiveLocation("");
    setRetentionYears("");
    setArchivalNotes("");
    setGenerateReport(false);
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
    setArchivalModalOpen(true);
  }, [selectedSampleIds, intl, resetForm, notify]);

  const applyArchival = useCallback(() => {
    if (!archiveType) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.archival.error.typeRequired",
          defaultMessage: "Please select archive type.",
        }),
      });
      return;
    }

    if (!archiveLocation) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.archival.error.locationRequired",
          defaultMessage: "Please enter archive location.",
        }),
      });
      return;
    }

    if (!retentionYears) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.archival.error.retentionRequired",
          defaultMessage: "Please specify retention period.",
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

    postToOpenElisServerJsonResponse(
      `/rest/notebook/tradmed/page/${pageData.id}/archival`,
      JSON.stringify({
        sampleIds,
        archiveType: archiveType.id,
        archiveLocation,
        retentionYears,
        archivalNotes,
        generateReport,
      }),
      (response) => {
        setIsApplying(false);
        if (response?.success) {
          // Update sample status using bulk endpoint after archival (mark as COMPLETED since this is final step)
          postToOpenElisServer(
            `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
            JSON.stringify({
              sampleIds,
              status: "COMPLETED",
            }),
            (statusCode) => {
              if (statusCode === 200) {
                notify({
                  kind: NotificationKinds.success,
                  title: intl.formatMessage(
                    {
                      id: "notebook.page.tradmed.archival.success",
                      defaultMessage: "Archived {count} sample(s).",
                    },
                    {
                      count: selectedSampleIds.length,
                    },
                  ),
                });
                setArchivalModalOpen(false);
                setSelectedSampleIds([]);
                loadPageSamples();
                if (onProgressUpdate) onProgressUpdate();
              } else {
                notify({
                  kind: NotificationKinds.error,
                  title: intl.formatMessage({
                    id: "notebook.page.tradmed.error.statusUpdate",
                    defaultMessage:
                      "Archival recorded but failed to update sample status.",
                  }),
                });
              }
            },
          );
        } else {
          notify({
            kind: NotificationKinds.error,
            title: intl.formatMessage({
              id: "notebook.page.tradmed.archival.error.failed",
              defaultMessage: "Failed to archive samples.",
            }),
          });
        }
      },
    );
  }, [
    archiveType,
    archiveLocation,
    retentionYears,
    archivalNotes,
    generateReport,
    hasRealPageId,
    pageData?.id,
    selectedSampleIds,
    intl,
    loadPageSamples,
    onProgressUpdate,
    notify,
  ]);

  const pendingSamples = useMemo(
    () =>
      samples.filter(
        (s) => s.status === "PENDING" || s.status === "IN_PROGRESS",
      ),
    [samples],
  );

  const archivedSamples = useMemo(
    () => samples.filter((s) => s.archiveType && s.status === "COMPLETED"),
    [samples],
  );

  // Helper to render sample status
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

  // Helper to render archive type label
  const renderArchiveType = (sample) => {
    if (!sample.archiveTypeLabel) return "-";
    return (
      <Tag type="blue" size="sm">
        {sample.archiveTypeLabel}
      </Tag>
    );
  };

  return (
    <div className="tradmed-archival-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.tradmed.archival.title"
            defaultMessage="Reporting & Archival"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.tradmed.archival.description"
            defaultMessage="Archive sample data and specimens for long-term preservation and regulatory compliance."
          />
        </p>
      </div>

      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.archival.pending"
                  defaultMessage="Awaiting Archival"
                />
              </span>
              <span className="progress-value">{pendingSamples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.archival.archived"
                  defaultMessage="Archived"
                />
              </span>
              <span className="progress-value">{archivedSamples.length}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      <div className="page-actions-bar">
        <PermissionGate
          roles={Permissions.UPDATE_SAMPLES}
          disabledTooltip={intl.formatMessage({
            id: "notebook.tradmed.tooltip.recordArchivalPermission",
            defaultMessage: "Insufficient permissions to record archival",
          })}
        >
          <Button
            kind="primary"
            size="sm"
            renderIcon={Edit}
            onClick={openModal}
            disabled={selectedSampleIds.length === 0 || !hasRealPageId}
          >
            <FormattedMessage
              id="notebook.page.tradmed.archival.recordArchival"
              defaultMessage="Record Archival ({count})"
              values={{ count: selectedSampleIds.length }}
            />
          </Button>
        </PermissionGate>

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
              id="notebook.page.tradmed.archival.pending.title"
              defaultMessage="Samples Awaiting Archival"
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
                  id="notebook.page.tradmed.archival.pending.empty"
                  defaultMessage="No samples awaiting archival."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="pending-archival"
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

      {archivedSamples.length > 0 && (
        <div className="sample-table-section">
          <div className="table-section-header">
            <h5>
              <FormattedMessage
                id="notebook.page.tradmed.archival.archived.title"
                defaultMessage="Archived Samples"
              />
              <Tag type="green" size="sm" className="count-tag">
                {archivedSamples.length}
              </Tag>
            </h5>
          </div>
          <div className="sample-grid-container">
            <SampleGrid
              gridId="archived-samples"
              samples={archivedSamples}
              showSelection={false}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "localName", header: "Local Name" },
                { key: "scientificName", header: "Scientific Name" },
                {
                  key: "archiveTypeLabel",
                  header: "Archive Type",
                  render: (_value, sample) => renderArchiveType(sample),
                },
                { key: "archiveLocation", header: "Location" },
                { key: "retentionYears", header: "Retention" },
                { key: "archivalNotes", header: "Notes" },
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
        open={archivalModalOpen}
        onRequestClose={() => setArchivalModalOpen(false)}
        onRequestSubmit={applyArchival}
        modalHeading={intl.formatMessage({
          id: "notebook.page.tradmed.archival.modal.title",
          defaultMessage: "Record Archival Details",
        })}
        primaryButtonText={
          isApplying
            ? intl.formatMessage({
                id: "label.recording",
                defaultMessage: "Recording...",
              })
            : intl.formatMessage({
                id: "notebook.page.tradmed.archival.modal.record",
                defaultMessage: "Archive Samples",
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
          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="archive-type"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.archival.modal.type",
                defaultMessage: "Archive Type *",
              })}
              label="Select..."
              items={archiveTypeOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={archiveType}
              onChange={({ selectedItem }) => setArchiveType(selectedItem)}
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="archive-location"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.archival.modal.location",
                defaultMessage: "Archive Location *",
              })}
              value={archiveLocation}
              onChange={(e) => setArchiveLocation(e.target.value)}
              placeholder="e.g., Cloud Storage, Lab Freezer A2"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="retention-years"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.archival.modal.retention",
                defaultMessage: "Retention Period *",
              })}
              label="Select..."
              items={[
                { id: "5", label: "5 Years" },
                { id: "7", label: "7 Years" },
                { id: "10", label: "10 Years" },
                { id: "Permanent", label: "Permanent" },
              ]}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={
                [
                  { id: "5", label: "5 Years" },
                  { id: "7", label: "7 Years" },
                  { id: "10", label: "10 Years" },
                  { id: "Permanent", label: "Permanent" },
                ].find((t) => t.id === retentionYears) || null
              }
              onChange={({ selectedItem }) =>
                setRetentionYears(selectedItem?.id || "")
              }
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="archival-notes"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.archival.modal.notes",
                defaultMessage: "Archival Notes",
              })}
              value={archivalNotes}
              onChange={(e) => setArchivalNotes(e.target.value)}
              rows={2}
              placeholder="Any additional notes about archival procedure or storage conditions"
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default TraditionalMedicineArchivalPage;
