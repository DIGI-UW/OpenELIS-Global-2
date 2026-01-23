import React, { useState, useEffect, useRef, useCallback, useMemo, useContext } from "react";
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
 * TraditionalMedicineArchivalPage - Page 8 of the Traditional Medicine workflow.
 *
 * SRS Requirements - STAGE 9: Reporting & Archival
 * - Report generation (sample history, processing, product lineage, certificate)
 * - Data archival tracking
 * - Metadata locking for regulatory compliance
 * - Long-term data preservation
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
  const { setNotificationVisible, addNotification } = useContext(NotificationContext);
  const componentMounted = useRef(false);
  const { hasAnyRole } = usePermissions();

  // TMMRD permissions per SRS Section 11
  const {
    getPagePermissionLevel,
    canSaveData,
    canAccessStage8,
  } = useTMMRDPermissions();

  // STAGE 8 allowed roles per TMMRD SRS Section 11 - Management only for archival
  const allowedRoles = [
    "Lab Manager",
    "Principal Investigator"
  ];

  const canAccessPage = hasAnyRole(allowedRoles);

  // Check page access - show access denied if user lacks required roles
  if (!canAccessPage) {
    return (
      <AccessDeniedMessage
        page="Reporting & Archival"
        reason="This page requires management level Traditional Medicine roles to access."
        requiredRoles={allowedRoles}
      />
    );
  }

  // Get user's action-level permission for this page
  const pagePermissionLevel = getPagePermissionLevel("Reporting & Archival");
  const canEditData = canSaveData(pagePermissionLevel);

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
          setSamples(
            response && Array.isArray(response)
              ? response.map((s) => ({
                  id: String(s.id || s.sampleItemId),
                  externalId: s.externalId,
                  accessionNumber: s.accessionNumber,
                  status: s.pageStatus || s.status || "PENDING",
                  localName: s.data?.localName,
                  archiveType: s.data?.archiveType,
                  archivedAt: s.data?.archivedAt,
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
        archiveTypeLabel: archiveType.label,
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
                  title: response.message ||
                    intl.formatMessage(
                      {
                        id: "notebook.page.tradmed.archival.success",
                        defaultMessage: "Archived {count} sample record(s).",
                      },
                      { count: response.updatedCount || selectedSampleIds.length },
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
                    defaultMessage: "Archival recorded but failed to update sample status.",
                  }),
                });
              }
            }
          );
        } else {
          notify({
            kind: NotificationKinds.error,
            title: response?.error || "Operation failed",
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
    () => samples.filter((s) => !s.archiveType),
    [samples],
  );
  const archivedSamples = useMemo(
    () =>
      samples.filter(
        (s) => s.archiveType && s.status === "COMPLETED",
      ),
    [samples],
  );

  return (
    <div className="tradmed-archival-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.tradmed.archival.title"
            defaultMessage="Reporting & Data Archival"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.tradmed.archival.description"
            defaultMessage="Generate reports, archive sample records, and ensure long-term data preservation and regulatory compliance."
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
        <Button
          kind="primary"
          size="sm"
          renderIcon={Edit}
          onClick={openModal}
          disabled={selectedSampleIds.length === 0 || !hasRealPageId}
        >
          <FormattedMessage
            id="notebook.page.tradmed.archival.archive"
            defaultMessage="Archive Record ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="secondary"
          size="sm"
          renderIcon={Download}
          disabled={selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="notebook.page.tradmed.archival.generateReport"
            defaultMessage="Generate Report"
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
              id="notebook.page.tradmed.archival.pending.title"
              defaultMessage="Records Awaiting Archival"
            />
            <Tag type="blue" size="sm" className="count-tag">
              {pendingSamples.length}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.tradmed.archival.pending.description"
              defaultMessage="Completed samples ready for final archival and reporting."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          {!loading && pendingSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.tradmed.archival.pending.empty"
                  defaultMessage="No records awaiting archival."
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
                { key: "localName", header: "Local Name" },
                { key: "externalId", header: "Sample ID" },
              ]}
            />
          )}
        </div>
      </div>

      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.tradmed.archival.archived.title"
              defaultMessage="Archived Records"
            />
            <Tag type="green" size="sm" className="count-tag">
              {archivedSamples.length}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.tradmed.archival.archived.description"
              defaultMessage="Sample records that have been archived and locked for regulatory compliance."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          {!loading && archivedSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.tradmed.archival.archived.empty"
                  defaultMessage="No archived records yet."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="archived-records"
              samples={archivedSamples}
              showSelection={false}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "localName", header: "Local Name" },
                { key: "archiveType", header: "Archive Type" },
                { key: "archivedAt", header: "Archived Date" },
              ]}
            />
          )}
        </div>
      </div>

      <Modal
        open={archivalModalOpen}
        onRequestClose={() => setArchivalModalOpen(false)}
        onRequestSubmit={applyArchival}
        modalHeading={intl.formatMessage({
          id: "notebook.page.tradmed.archival.modal.title",
          defaultMessage: "Archive & Report Sample Records",
        })}
        primaryButtonText={
          isApplying
            ? intl.formatMessage({
                id: "label.archiving",
                defaultMessage: "Archiving...",
              })
            : intl.formatMessage({
                id: "notebook.page.tradmed.archival.modal.archive",
                defaultMessage: "Archive",
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

        <Grid fullWidth narrow>
          <Column lg={16} md={8} sm={4}>
            <Dropdown
              id="archive-type"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.archival.modal.archiveType",
                defaultMessage: "Archive Type *",
              })}
              label="Select..."
              items={archiveTypeOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={archiveType}
              onChange={({ selectedItem }) => setArchiveType(selectedItem)}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="archive-location"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.archival.modal.location",
                defaultMessage: "Archive Location",
              })}
              value={archiveLocation}
              onChange={(e) => setArchiveLocation(e.target.value)}
              placeholder="e.g., Server/Cloud location or Physical location"
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="retention-years"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.archival.modal.retentionYears",
                defaultMessage: "Retention Period (Years)",
              })}
              value={retentionYears}
              onChange={(e) => setRetentionYears(e.target.value)}
              placeholder="e.g., 7 or Permanent"
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="archival-notes"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.archival.modal.notes",
                defaultMessage: "Archival Notes",
              })}
              value={archivalNotes}
              onChange={(e) => setArchivalNotes(e.target.value)}
              rows={2}
              placeholder="Any additional notes about archival, regulatory compliance, etc."
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default TraditionalMedicineArchivalPage;
