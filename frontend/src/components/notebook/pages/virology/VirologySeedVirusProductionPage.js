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
 * VirologySeedVirusProductionPage - Select strain for vaccine production.
 * Records seed virus batch ID and selection criteria.
 */
function VirologySeedVirusProductionPage({
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
  const [productionDate, setProductionDate] = useState("");
  const [seedVirusBatchId, setSeedVirusBatchId] = useState("");
  const [selectedStrain, setSelectedStrain] = useState("");
  const [selectionCriteria, setSelectionCriteria] = useState("");
  const [productionNotes, setProductionNotes] = useState("");

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
              seedVirusBatchId: sample.data?.seedVirusBatchId,
              selectedStrain: sample.data?.selectedStrain,
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

  const handleSaveProduction = useCallback(() => {
    if (!seedVirusBatchId || !selectionCriteria) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle:
          "Please provide seed virus batch ID and selection criteria",
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
      productionDate: productionDate || null,
      seedVirusBatchId,
      selectedStrain: selectedStrain || null,
      selectionCriteria,
      notes: productionNotes,
    };

    postToOpenElisServerJsonResponse(
      "/rest/virology/seed-virus-production",
      JSON.stringify(payload),
      (response) => {
        setLoading(false);

        if (response.success) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.success" }),
            subtitle: `Seed virus production data saved successfully. ${response.samplesUpdated || 0} sample(s) updated.`,
          });

          setProductionDate("");
          setSeedVirusBatchId("");
          setSelectedStrain("");
          setSelectionCriteria("");
          setProductionNotes("");
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
    seedVirusBatchId,
    selectionCriteria,
    selectedStrain,
    productionDate,
    productionNotes,
    selectedSampleIds,
    pageData?.id,
    intl,
    notify,
    loadPageSamples,
    onProgressUpdate,
  ]);

  const handleCompleteProduction = useCallback(() => {
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
            subtitle: `Completed seed virus production for ${selectedSampleIds.length} sample(s).`,
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
            subtitle:
              "Failed to complete seed virus production. Please try again.",
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
        key: "seedVirusBatchId",
        header: "Seed Virus Batch ID",
        render: (value) =>
          value ? <Tag type="magenta" size="sm">{value}</Tag> : "-",
      },
      {
        key: "selectedStrain",
        header: "Selected Strain",
        render: (value) =>
          value ? <Tag type="purple" size="sm">{value}</Tag> : "-",
      },
    ],
    [],
  );

  return (
    <div className="virology-seed-virus-production-page">
      {/* Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="virology.seedProduction.title"
            defaultMessage="Seed Virus Production"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="virology.seedProduction.description"
            defaultMessage="Select strain for vaccine production and document seed virus batch details."
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
            id="virology.seedProduction.log"
            defaultMessage="Log Seed Virus Production"
          />
        </Button>
        <Button
          kind="tertiary"
          size="md"
          renderIcon={Checkmark}
          onClick={handleCompleteProduction}
          disabled={loading || selectedSampleIds.length === 0}
          style={{ marginLeft: "0.5rem" }}
        >
          <FormattedMessage
            id="virology.seedProduction.complete"
            defaultMessage="Complete Production ({count})"
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
                id="virology.seedProduction.pending"
                defaultMessage="Samples Pending Seed Virus Production"
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
              gridId="seed-production-pending-samples"
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
                id="virology.seedProduction.completed"
                defaultMessage="Completed Seed Virus Production"
              />
              <Tag type="green" size="sm" className="count-tag">
                {completedSamples.length}
              </Tag>
            </h5>
          </div>
          <div className="sample-grid-container">
            <SampleGrid
              gridId="seed-production-completed-samples"
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
              id="virology.seedProduction.empty"
              defaultMessage="No samples available for seed virus production."
            />
          </p>
        </div>
      )}

      {/* Modal */}
      <Modal
        open={modalOpen}
        onRequestClose={() => setModalOpen(false)}
        modalHeading="Log Seed Virus Production"
        modalLabel="Vaccine Strain Selection"
        primaryButtonText="Save Production Data"
        secondaryButtonText="Cancel"
        onRequestSubmit={handleSaveProduction}
        primaryButtonDisabled={
          loading || !seedVirusBatchId || !selectionCriteria
        }
        size="md"
      >
        <Grid fullWidth>
          <Column lg={8}>
            <DatePicker
              datePickerType="single"
              dateFormat="Y-m-d"
              value={productionDate}
              onChange={(dates) =>
                setProductionDate(
                  dates[0] ? dates[0].toISOString().split("T")[0] : "",
                )
              }
            >
              <DatePickerInput
                id="production-date"
                placeholder="YYYY-MM-DD"
                labelText="Production Date"
              />
            </DatePicker>
          </Column>

          <Column lg={16}>
            <TextInput
              id="seed-virus-batch-id"
              labelText={
                <span>
                  Seed Virus Batch ID
                  {" *"}
                </span>
              }
              placeholder="e.g., SEED-2026-001"
              value={seedVirusBatchId}
              onChange={(e) => setSeedVirusBatchId(e.target.value)}
              invalid={!seedVirusBatchId}
              invalidText="Required"
            />
          </Column>

          <Column lg={16}>
            <TextInput
              id="selected-strain"
              labelText="Selected Strain"
              placeholder="e.g., A/H1N1, SARS-CoV-2 Delta, etc."
              value={selectedStrain}
              onChange={(e) => setSelectedStrain(e.target.value)}
            />
          </Column>

          <Column lg={16}>
            <TextArea
              id="selection-criteria"
              labelText={
                <span>
                  Selection Criteria
                  {" *"}
                </span>
              }
              placeholder="Document the criteria used for strain selection (e.g., antigenic properties, growth characteristics, epidemiological data)..."
              value={selectionCriteria}
              onChange={(e) => setSelectionCriteria(e.target.value)}
              rows={4}
              invalid={!selectionCriteria}
              invalidText="Required"
            />
          </Column>

          <Column lg={16}>
            <TextArea
              id="production-notes"
              labelText="Additional Notes"
              placeholder="Any additional notes about the seed virus production..."
              value={productionNotes}
              onChange={(e) => setProductionNotes(e.target.value)}
              rows={3}
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

export default VirologySeedVirusProductionPage;