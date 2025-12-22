import React, { useState, useRef } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Modal,
  TextArea,
  Checkbox,
} from "@carbon/react";
import { Archive, Checkmark, Warning } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { postToOpenElisServerJsonResponse } from "../../../utils/Utils";
import "../../workflow/NotebookWorkflow.css";

/**
 * ImmunologyArchivingPage - Final stage of the Immunology workflow.
 * Provides a simple option to archive the notebook, marking the end of its lifecycle.
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {number} props.notebookId - The notebook ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function ImmunologyArchivingPage({
  entryId,
  notebookId,
  pageData,
  progress: _progress, // eslint-disable-line no-unused-vars
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(true);

  // State
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [archiveModalOpen, setArchiveModalOpen] = useState(false);
  const [archiving, setArchiving] = useState(false);
  const [archiveNotes, setArchiveNotes] = useState("");
  const [confirmArchive, setConfirmArchive] = useState(false);
  const [isArchived, setIsArchived] = useState(
    pageData?.data?.archived || false,
  );

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Handle archive notebook
  const handleArchiveNotebook = () => {
    if (!confirmArchive) {
      setError(
        intl.formatMessage({
          id: "notebook.immunology.archiving.confirmRequired",
          defaultMessage:
            "Please confirm that you want to archive this notebook.",
        }),
      );
      return;
    }

    setArchiving(true);
    setError(null);

    const nbId = notebookId || entryId;

    postToOpenElisServerJsonResponse(
      `/rest/notebook/${nbId}/archive`,
      JSON.stringify({
        pageId: pageData?.id,
        notes: archiveNotes,
        archivedAt: new Date().toISOString(),
      }),
      (response) => {
        if (!componentMounted.current) return;

        setArchiving(false);

        if (response && response.success) {
          setSuccess(
            intl.formatMessage({
              id: "notebook.immunology.archiving.success",
              defaultMessage:
                "Notebook has been archived successfully. The notebook lifecycle is now complete.",
            }),
          );
          setArchiveModalOpen(false);
          setIsArchived(true);
          setConfirmArchive(false);
          setArchiveNotes("");
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            response?.error ||
              intl.formatMessage({
                id: "notebook.immunology.archiving.error",
                defaultMessage: "Failed to archive notebook.",
              }),
          );
        }
      },
    );
  };

  return (
    <div className="immunology-archiving-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.immunology.archiving.title"
            defaultMessage="Archive Notebook"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.immunology.archiving.description"
            defaultMessage="Mark the end of this notebook's lifecycle by archiving it. This indicates that all work has been completed."
          />
        </p>
      </div>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          onCloseButtonClick={() => setError(null)}
          lowContrast
          style={{ marginBottom: "1rem" }}
        />
      )}
      {success && (
        <InlineNotification
          kind="success"
          title={success}
          onCloseButtonClick={() => setSuccess(null)}
          lowContrast
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Archive Status */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          {isArchived ? (
            <Tile className="archive-status-tile archived">
              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: "1rem",
                  padding: "1rem",
                }}
              >
                <Checkmark size={32} style={{ color: "#24a148" }} />
                <div>
                  <h5 style={{ margin: 0, color: "#24a148" }}>
                    <FormattedMessage
                      id="notebook.immunology.archiving.archived"
                      defaultMessage="Notebook Archived"
                    />
                  </h5>
                  <p style={{ margin: "0.5rem 0 0 0", color: "#525252" }}>
                    <FormattedMessage
                      id="notebook.immunology.archiving.archivedDescription"
                      defaultMessage="This notebook has been archived. The lifecycle is complete."
                    />
                  </p>
                </div>
              </div>
            </Tile>
          ) : (
            <Tile className="archive-status-tile">
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  gap: "1rem",
                  padding: "2rem",
                  textAlign: "center",
                }}
              >
                <Archive size={48} style={{ color: "#0f62fe" }} />
                <div>
                  <h5 style={{ margin: 0 }}>
                    <FormattedMessage
                      id="notebook.immunology.archiving.readyToArchive"
                      defaultMessage="Ready to Archive"
                    />
                  </h5>
                  <p
                    style={{
                      margin: "0.5rem 0 1.5rem 0",
                      color: "#525252",
                      maxWidth: "500px",
                    }}
                  >
                    <FormattedMessage
                      id="notebook.immunology.archiving.readyDescription"
                      defaultMessage="When all work is complete, archive this notebook to mark the end of its lifecycle. Archived notebooks are preserved for record-keeping."
                    />
                  </p>
                  <Button
                    kind="primary"
                    size="lg"
                    renderIcon={Archive}
                    onClick={() => setArchiveModalOpen(true)}
                    disabled={!hasRealPageId}
                  >
                    <FormattedMessage
                      id="notebook.immunology.archiving.archiveButton"
                      defaultMessage="Archive Notebook"
                    />
                  </Button>
                </div>
              </div>
            </Tile>
          )}
        </Column>
      </Grid>

      {/* Archive Confirmation Modal */}
      <Modal
        open={archiveModalOpen}
        onRequestClose={() => {
          setArchiveModalOpen(false);
          setConfirmArchive(false);
          setArchiveNotes("");
          setError(null);
        }}
        modalHeading={intl.formatMessage({
          id: "notebook.immunology.archiving.modal.title",
          defaultMessage: "Archive Notebook",
        })}
        primaryButtonText={
          archiving
            ? intl.formatMessage({
                id: "label.archiving",
                defaultMessage: "Archiving...",
              })
            : intl.formatMessage({
                id: "notebook.immunology.archiving.modal.archive",
                defaultMessage: "Archive Notebook",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleArchiveNotebook}
        primaryButtonDisabled={!confirmArchive || archiving}
        danger
        size="sm"
      >
        <p style={{ marginBottom: "1rem" }}>
          <FormattedMessage
            id="notebook.immunology.archiving.modal.description"
            defaultMessage="Archiving this notebook marks the end of its lifecycle. All data will be preserved for record-keeping."
          />
        </p>

        <TextArea
          id="archive-notes"
          labelText={intl.formatMessage({
            id: "notebook.immunology.archiving.notes",
            defaultMessage: "Archive Notes (Optional)",
          })}
          value={archiveNotes}
          onChange={(e) => setArchiveNotes(e.target.value)}
          placeholder={intl.formatMessage({
            id: "notebook.immunology.archiving.notesPlaceholder",
            defaultMessage: "Add any final notes about this notebook...",
          })}
          rows={3}
          style={{ marginBottom: "1rem" }}
        />

        <div
          style={{
            padding: "1rem",
            backgroundColor: "#fff1f1",
            borderRadius: "4px",
            borderLeft: "4px solid #da1e28",
            marginBottom: "1rem",
          }}
        >
          <p
            style={{
              margin: 0,
              fontSize: "0.875rem",
              display: "flex",
              alignItems: "flex-start",
              gap: "0.5rem",
            }}
          >
            <Warning size={16} style={{ flexShrink: 0, marginTop: "2px" }} />
            <span>
              <FormattedMessage
                id="notebook.immunology.archiving.warning"
                defaultMessage="This action marks the notebook as complete. Please ensure all work has been finished before archiving."
              />
            </span>
          </p>
        </div>

        <Checkbox
          id="confirm-archive"
          labelText={intl.formatMessage({
            id: "notebook.immunology.archiving.confirm",
            defaultMessage:
              "I confirm that all work is complete and I want to archive this notebook",
          })}
          checked={confirmArchive}
          onChange={(e, { checked }) => setConfirmArchive(checked)}
        />
      </Modal>
    </div>
  );
}

export default ImmunologyArchivingPage;
