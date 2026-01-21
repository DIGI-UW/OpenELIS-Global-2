import React, {
  useState,
  useEffect,
  useRef,
  useCallback,
  useContext,
  useMemo,
} from "react";
import {
  Grid,
  Column,
  Button,
  Tag,
  Modal,
  TextInput,
  TextArea,
  DatePicker,
  DatePickerInput,
  FileUploader,
} from "@carbon/react";
import { Save, Checkmark } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * VirologyGenomeSequencingPage - Page for viral genome analysis.
 * Records sequence data (FASTA files, GenBank accession).
 */
function VirologyGenomeSequencingPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);
  const componentMounted = useRef(false);

  // State
  const [loading, setLoading] = useState(true);
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [modalOpen, setModalOpen] = useState(false);

  // Form data
  const [sequencingDate, setSequencingDate] = useState("");
  const [fastaFileReference, setFastaFileReference] = useState("");
  const [genbankAccession, setGenbankAccession] = useState("");
  const [sequencingNotes, setSequencingNotes] = useState("");

  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, subtitle }) => {
      setNotificationVisible(true);
      addNotification({ kind, title, subtitle });
    },
    [addNotification, setNotificationVisible],
  );

  // Load samples
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    return () => {
      componentMounted.current = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [entryId, pageData?.id]);

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
          if (response && Array.isArray(response)) {
            const transformedSamples = response.map((sample) => ({
              id: String(sample.id || sample.sampleItemId),
              externalId: sample.externalId,
              accessionNumber: sample.accessionNumber,
              sampleType:
                sample.sampleType || sample.typeOfSample?.description,
              collectionDate: sample.collectionDate,
              status: sample.pageStatus || sample.status || "PENDING",
              fastaFileReference: sample.data?.fastaFileReference,
              genbankAccession: sample.data?.genbankAccession,
              data: sample.data,
            }));
            setSamples(transformedSamples);
          } else {
            setSamples([]);
          }
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  const handleSaveSequencing = useCallback(() => {
    if (!fastaFileReference && !genbankAccession) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: "Please provide either FASTA file reference or GenBank accession",
      });
      return;
    }

    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({ id: "notification.warning" }),
        subtitle: "Please select at least one sample",
      });
      return;
    }

    setLoading(true);

    const payload = {
      notebookPageId: pageData?.id,
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      sequencingDate: sequencingDate || null,
      fastaFileReference,
      genbankAccession: genbankAccession || null,
      notes: sequencingNotes,
    };

    postToOpenElisServerJsonResponse(
      "/rest/virology/genome-sequencing",
      JSON.stringify(payload),
      (response) => {
        setLoading(false);

        if (response.success) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.success" }),
            subtitle: `Genome sequencing data saved successfully. ${response.samplesUpdated || 0} sample(s) updated.`,
          });

          setSequencingDate("");
          setFastaFileReference("");
          setGenbankAccession("");
          setSequencingNotes("");
          setSelectedSampleIds([]);
          setModalOpen(false);

          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          notify({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.error" }),
            subtitle: response.error || response.message || "Unknown error",
          });
        }
      },
    );
  }, [
    fastaFileReference,
    genbankAccession,
    sequencingDate,
    sequencingNotes,
    selectedSampleIds,
    pageData?.id,
    intl,
    notify,
    loadPageSamples,
    onProgressUpdate,
  ]);

  const handleCompleteSequencing = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({ id: "notification.warning" }),
        subtitle: "Please select at least one sample to complete.",
      });
      return;
    }

    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: "Cannot complete samples: Page not properly initialized.",
      });
      return;
    }

    setLoading(true);

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        status: "COMPLETED",
      }),
      (response) => {
        setLoading(false);

        if (response && (response.success || response === 200)) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.success" }),
            subtitle: `Completed genome sequencing for ${selectedSampleIds.length} sample(s).`,
          });
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          notify({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.error" }),
            subtitle: "Failed to complete genome sequencing. Please try again.",
          });
        }
      },
    );
  }, [
    selectedSampleIds,
    pageData?.id,
    intl,
    notify,
    loadPageSamples,
    onProgressUpdate,
  ]);

  const pendingSamples = useMemo(
    () =>
      samples.filter(
        (s) => s.status === "PENDING" || s.status === "IN_PROGRESS",
      ),
    [samples],
  );

  const completedSamples = useMemo(
    () => samples.filter((s) => s.status === "COMPLETED"),
    [samples],
  );

  const additionalColumns = useMemo(
    () => [
      {
        key: "fastaFileReference",
        header: "FASTA File",
        render: (value) => (value ? <Tag size="sm">{value}</Tag> : "-"),
      },
      {
        key: "genbankAccession",
        header: "GenBank Accession",
        render: (value) =>
          value ? <Tag type="cyan" size="sm">{value}</Tag> : "-",
      },
    ],
    [],
  );

  return (
    <div className="virology-genome-sequencing-page">
      {/* Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="virology.sequencing.title"
            defaultMessage="Genome Sequencing"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="virology.sequencing.description"
            defaultMessage="Record viral genome sequence data including FASTA files and GenBank accessions."
          />
        </p>
      </div>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="md"
          renderIcon={Save}
          onClick={() => setModalOpen(true)}
          disabled={loading || selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="virology.sequencing.log"
            defaultMessage="Log Sequencing Data"
          />
        </Button>
        <Button
          kind="tertiary"
          size="md"
          renderIcon={Checkmark}
          onClick={handleCompleteSequencing}
          disabled={loading || selectedSampleIds.length === 0}
          style={{ marginLeft: "0.5rem" }}
        >
          <FormattedMessage
            id="virology.sequencing.complete"
            defaultMessage="Complete Sequencing ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>
      </div>

      {/* Pending Samples */}
      {pendingSamples.length > 0 && (
        <div className="sample-table-section">
          <div className="table-section-header">
            <h5>
              <FormattedMessage
                id="virology.sequencing.pending"
                defaultMessage="Samples Pending Genome Sequencing"
              />
              <Tag type="blue" size="sm" className="count-tag">
                {pendingSamples.length}
              </Tag>
              {selectedSampleIds.length > 0 && (
                <Tag type="purple" size="sm" className="count-tag">
                  {selectedSampleIds.length} selected
                </Tag>
              )}
            </h5>
          </div>
          <div className="sample-grid-container">
            <SampleGrid
              gridId="sequencing-pending-samples"
              samples={pendingSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              showSelection={true}
              loading={loading}
              additionalColumns={additionalColumns}
            />
          </div>
        </div>
      )}

      {/* Completed Samples */}
      {completedSamples.length > 0 && (
        <div className="sample-table-section">
          <div className="table-section-header">
            <h5>
              <FormattedMessage
                id="virology.sequencing.completed"
                defaultMessage="Completed Genome Sequencing"
              />
              <Tag type="green" size="sm" className="count-tag">
                {completedSamples.length}
              </Tag>
            </h5>
          </div>
          <div className="sample-grid-container">
            <SampleGrid
              gridId="sequencing-completed-samples"
              samples={completedSamples}
              additionalColumns={additionalColumns}
              showSelection={false}
            />
          </div>
        </div>
      )}

      {/* Empty State */}
      {!loading && samples.length === 0 && (
        <div className="empty-table-state">
          <p>
            <FormattedMessage
              id="virology.sequencing.empty"
              defaultMessage="No samples available for genome sequencing."
            />
          </p>
        </div>
      )}

      {/* Modal */}
      <Modal
        open={modalOpen}
        onRequestClose={() => setModalOpen(false)}
        modalHeading="Log Genome Sequencing Data"
        modalLabel="Viral Genome Analysis"
        primaryButtonText="Save Sequencing Data"
        secondaryButtonText="Cancel"
        onRequestSubmit={handleSaveSequencing}
        primaryButtonDisabled={
          loading || (!fastaFileReference && !genbankAccession)
        }
        size="md"
      >
        <Grid fullWidth>
          <Column lg={8}>
            <DatePicker
              datePickerType="single"
              dateFormat="Y-m-d"
              value={sequencingDate}
              onChange={(dates) =>
                setSequencingDate(
                  dates[0] ? dates[0].toISOString().split("T")[0] : "",
                )
              }
            >
              <DatePickerInput
                id="sequencing-date"
                placeholder="YYYY-MM-DD"
                labelText="Sequencing Date"
              />
            </DatePicker>
          </Column>

          <Column lg={16}>
            <TextInput
              id="fasta-file-reference"
              labelText="FASTA File Reference *"
              placeholder="e.g., genome_seq_001.fasta or file path"
              value={fastaFileReference}
              onChange={(e) => setFastaFileReference(e.target.value)}
            />
          </Column>

          <Column lg={16}>
            <TextInput
              id="genbank-accession"
              labelText="GenBank Accession"
              placeholder="e.g., MN908947 (optional)"
              value={genbankAccession}
              onChange={(e) => setGenbankAccession(e.target.value)}
            />
          </Column>

          <Column lg={16}>
            <TextArea
              id="sequencing-notes"
              labelText="Notes"
              placeholder="Additional notes about the sequencing analysis..."
              value={sequencingNotes}
              onChange={(e) => setSequencingNotes(e.target.value)}
              rows={4}
            />
          </Column>

          <Column lg={16}>
            <div style={{ marginTop: "1rem" }}>
              <Tag type="blue">
                {selectedSampleIds.length} sample(s) selected
              </Tag>
            </div>
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default VirologyGenomeSequencingPage;
