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
  Tile,
  Tag,
  Modal,
  TextInput,
  TextArea,
  Dropdown,
  RadioButtonGroup,
  RadioButton,
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
 * VirologyQualityControlPage - Page 4 of the Virology & Vaccine Unit workflow.
 * Validate cell viability and sterility.
 *
 * Features:
 * - Display samples from previous page (Cell Culture)
 * - Log QC results: viability percentage, sterility pass/fail
 * - Track test methods and acceptance criteria
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function VirologyQualityControlPage({
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

  // Form data for QC modal
  const [viabilityPercentage, setViabilityPercentage] = useState("");
  const [sterilityResult, setSterilityResult] = useState(null);
  const [testMethod, setTestMethod] = useState("");
  const [acceptanceCriteria, setAcceptanceCriteria] = useState("");
  const [deviationNotes, setDeviationNotes] = useState("");
  const [qcNotes, setQcNotes] = useState("");

  // Notification helper function
  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, subtitle }) => {
      setNotificationVisible(true);
      addNotification({ kind, title, subtitle });
    },
    [addNotification, setNotificationVisible],
  );

  // Load samples for this page
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();

    return () => {
      componentMounted.current = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [entryId, pageData?.id]);

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id) {
      setLoading(false);
      return;
    }

    if (String(pageData.id).startsWith("default-")) {
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
              sampleType: sample.sampleType || sample.typeOfSample?.description,
              collectionDate: sample.collectionDate,
              status: sample.pageStatus || sample.status || "PENDING",
              // Previous page data
              testType: sample.data?.testType,
              cellLine: sample.data?.cellLine,
              passageNumber: sample.data?.passageNumber,
              // QC data
              viabilityPercentage: sample.data?.viabilityPercentage,
              sterilityResult: sample.data?.sterilityResult,
              testMethod: sample.data?.testMethod,
              acceptanceCriteria: sample.data?.acceptanceCriteria,
              deviationNotes: sample.data?.deviationNotes,
              qcNotes: sample.data?.qcNotes,
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

  // Sterility result options
  const sterilityResultOptions = [
    {
      id: "Pass",
      text: intl.formatMessage({
        id: "virology.qc.sterility.pass",
        defaultMessage: "Pass",
      }),
    },
    {
      id: "Fail",
      text: intl.formatMessage({
        id: "virology.qc.sterility.fail",
        defaultMessage: "Fail",
      }),
    },
  ];

  // Handle save QC data
  const handleSaveQC = useCallback(() => {
    // Validation
    if (!viabilityPercentage || viabilityPercentage === "") {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: "Please enter viability percentage",
      });
      return;
    }

    const viability = parseFloat(viabilityPercentage);
    if (isNaN(viability) || viability < 0 || viability > 100) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: "Viability percentage must be between 0 and 100",
      });
      return;
    }

    if (!sterilityResult) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: "Please select sterility result (Pass/Fail)",
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
      viabilityPercentage: viability,
      sterilityResult: sterilityResult.id,
      testMethod,
      acceptanceCriteria,
      deviationNotes,
      notes: qcNotes,
    };

    postToOpenElisServerJsonResponse(
      "/rest/virology/quality-control",
      JSON.stringify(payload),
      (response) => {
        setLoading(false);

        if (response && response.success) {
          const samplesUpdated = response.samplesUpdated || 0;
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.success" }),
            subtitle: `Quality control data logged successfully. ${samplesUpdated} sample(s) updated.`,
          });

          // Clear form and selection
          setViabilityPercentage("");
          setSterilityResult(null);
          setTestMethod("");
          setAcceptanceCriteria("");
          setDeviationNotes("");
          setQcNotes("");
          setSelectedSampleIds([]);
          setModalOpen(false);

          // Reload samples
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          const errorMessage =
            response?.error || response?.message || "Unknown error";
          notify({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.error" }),
            subtitle: errorMessage,
          });
        }
      },
    );
  }, [
    viabilityPercentage,
    sterilityResult,
    testMethod,
    acceptanceCriteria,
    deviationNotes,
    qcNotes,
    selectedSampleIds,
    pageData?.id,
    intl,
    notify,
    loadPageSamples,
    onProgressUpdate,
  ]);

  const handleCompleteQC = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({
          id: "virology.qc.complete.noSamplesSelected.title",
          defaultMessage: "No Samples Selected",
        }),
        subtitle: intl.formatMessage({
          id: "virology.qc.complete.noSamplesSelected.body",
          defaultMessage: "Please select at least one sample to complete.",
        }),
      });
      return;
    }

    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "virology.qc.complete.pageNotSaved.title",
          defaultMessage: "Page Not Saved",
        }),
        subtitle: intl.formatMessage({
          id: "virology.qc.complete.pageNotSaved.body",
          defaultMessage:
            "Cannot complete samples until the page is saved. Please save the page first.",
        }),
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

        if (response.success) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage({
              id: "virology.qc.complete.success.title",
              defaultMessage: "Quality Control Completed",
            }),
            subtitle: intl.formatMessage(
              {
                id: "virology.qc.complete.success.body",
                defaultMessage:
                  "{count} sample(s) marked as completed and ready for Virus Culture.",
              },
              { count: response.samplesUpdated || selectedSampleIds.length },
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
              id: "virology.qc.complete.error.title",
              defaultMessage: "Completion Failed",
            }),
            subtitle: intl.formatMessage(
              {
                id: "virology.qc.complete.error.body",
                defaultMessage:
                  "Failed to complete samples: {error}. Please try again.",
              },
              { error: response.error || "Unknown error" },
            ),
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

  // Custom columns for QC display
  const getAdditionalColumns = (intl) => [
    {
      key: "cellLine",
      header: intl.formatMessage({
        id: "virology.qc.column.cellLine",
        defaultMessage: "Cell Line",
      }),
      render: (value, sample) => {
        const cellLine = value || sample?.cellLine || sample?.data?.cellLine;
        return cellLine || "-";
      },
    },
    {
      key: "viabilityPercentage",
      header: intl.formatMessage({
        id: "virology.qc.column.viability",
        defaultMessage: "Viability (%)",
      }),
      render: (value, sample) => {
        const viability =
          value ||
          sample?.viabilityPercentage ||
          sample?.data?.viabilityPercentage;
        if (!viability) return "-";
        return `${viability}%`;
      },
    },
    {
      key: "sterilityResult",
      header: intl.formatMessage({
        id: "virology.qc.column.sterility",
        defaultMessage: "Sterility",
      }),
      render: (value, sample) => {
        const sterility =
          value || sample?.sterilityResult || sample?.data?.sterilityResult;
        if (!sterility) return "-";

        const tagType = sterility.toLowerCase() === "pass" ? "green" : "red";
        const display =
          sterility.charAt(0).toUpperCase() + sterility.slice(1).toLowerCase();
        return (
          <Tag type={tagType} size="sm">
            {display}
          </Tag>
        );
      },
    },
    {
      key: "testMethod",
      header: intl.formatMessage({
        id: "virology.qc.column.method",
        defaultMessage: "Test Method",
      }),
      render: (value, sample) => {
        const method = value || sample?.testMethod || sample?.data?.testMethod;
        return method || "-";
      },
    },
  ];

  // Split samples into pending/in-progress and completed
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

  const pendingCount = pendingSamples.length;
  const completedCount = completedSamples.length;

  return (
    <div className="virology-quality-control-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="virology.qc.title"
            defaultMessage="Quality Control - Cell Viability & Sterility"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="virology.qc.description"
            defaultMessage="Validate cell viability and sterility. Log QC results (viability %, sterility pass/fail) with test methods and acceptance criteria."
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
                  id="virology.qc.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="virology.qc.pendingInProgress"
                  defaultMessage="Pending / In Progress"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="virology.qc.completed"
                  defaultMessage="Completed"
                />
              </span>
              <span className="progress-value">{completedCount}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

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
            id="virology.qc.logQC"
            defaultMessage="Log Quality Control Results"
          />
        </Button>
        <Button
          kind="tertiary"
          size="md"
          renderIcon={Checkmark}
          onClick={handleCompleteQC}
          disabled={loading || selectedSampleIds.length === 0}
          style={{ marginLeft: "0.5rem" }}
        >
          <FormattedMessage
            id="virology.qc.complete"
            defaultMessage="Complete Quality Control ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>
      </div>

      {/* Pending / In Progress Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="virology.qc.pendingSamples.title"
              defaultMessage="Pending / In Progress"
            />
            <Tag type="gray" size="sm" className="count-tag">
              {pendingCount}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="virology.qc.pendingSamples.description"
              defaultMessage="Samples ready for quality control testing. Select samples and log QC results."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          {!loading && pendingSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="virology.qc.pendingSamples.empty"
                  defaultMessage="No pending samples. Complete cell culture on Page 3 first."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="pending-qc"
              samples={pendingSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              showSelection={true}
              loading={loading}
              additionalColumns={getAdditionalColumns(intl)}
            />
          )}
        </div>
      </div>

      {/* Completed Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="virology.qc.completedSamples.title"
              defaultMessage="Completed"
            />
            <Tag type="green" size="sm" className="count-tag">
              {completedCount}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="virology.qc.completedSamples.description"
              defaultMessage="Samples with completed quality control testing."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          {!loading && completedSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="virology.qc.completedSamples.empty"
                  defaultMessage="No completed samples yet."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="completed-qc"
              samples={completedSamples}
              selectedIds={[]}
              showSelection={false}
              loading={loading}
              additionalColumns={getAdditionalColumns(intl)}
            />
          )}
        </div>
      </div>

      {/* Quality Control Data Modal */}
      <Modal
        open={modalOpen}
        onRequestClose={() => setModalOpen(false)}
        modalHeading={
          <FormattedMessage
            id="virology.qc.modal.title"
            defaultMessage="Log Quality Control Results"
          />
        }
        modalLabel={
          <FormattedMessage
            id="virology.qc.modal.subtitle"
            defaultMessage="Validate cell viability and sterility"
          />
        }
        primaryButtonText={
          <FormattedMessage
            id="virology.qc.save"
            defaultMessage="Save QC Results"
          />
        }
        secondaryButtonText={
          <FormattedMessage id="button.cancel" defaultMessage="Cancel" />
        }
        onRequestSubmit={handleSaveQC}
        primaryButtonDisabled={
          loading || !viabilityPercentage || !sterilityResult
        }
        size="md"
      >
        <Grid fullWidth>
          {/* Viability Percentage */}
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="viability-percentage"
              labelText={
                <span>
                  <FormattedMessage
                    id="virology.qc.viability"
                    defaultMessage="Cell Viability (%)"
                  />
                  {" *"}
                </span>
              }
              value={viabilityPercentage}
              onChange={(e) => setViabilityPercentage(e.target.value)}
              placeholder="e.g., 95.5"
              type="number"
              min="0"
              max="100"
              step="0.1"
              invalid={
                viabilityPercentage &&
                (isNaN(parseFloat(viabilityPercentage)) ||
                  parseFloat(viabilityPercentage) < 0 ||
                  parseFloat(viabilityPercentage) > 100)
              }
              invalidText="Must be between 0 and 100"
            />
          </Column>

          {/* Sterility Result */}
          <Column lg={8} md={4} sm={4}>
            <Dropdown
              id="sterility-result"
              titleText={
                <span>
                  <FormattedMessage
                    id="virology.qc.sterility"
                    defaultMessage="Sterility Test Result"
                  />
                  {" *"}
                </span>
              }
              label="Select result..."
              items={sterilityResultOptions}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={sterilityResult}
              onChange={({ selectedItem }) => setSterilityResult(selectedItem)}
              invalid={!sterilityResult}
              invalidText="Required"
            />
          </Column>

          {/* Test Method */}
          <Column lg={16} md={8} sm={4}>
            <TextInput
              id="test-method"
              labelText={
                <FormattedMessage
                  id="virology.qc.testMethod"
                  defaultMessage="Test Method"
                />
              }
              value={testMethod}
              onChange={(e) => setTestMethod(e.target.value)}
              placeholder="e.g., Trypan Blue Exclusion, Flow Cytometry"
            />
          </Column>

          {/* Acceptance Criteria */}
          <Column lg={16} md={8} sm={4}>
            <TextInput
              id="acceptance-criteria"
              labelText={
                <FormattedMessage
                  id="virology.qc.acceptanceCriteria"
                  defaultMessage="Acceptance Criteria"
                />
              }
              value={acceptanceCriteria}
              onChange={(e) => setAcceptanceCriteria(e.target.value)}
              placeholder="e.g., Viability ≥ 90%, Sterility Pass"
            />
          </Column>

          {/* Deviation Notes */}
          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="deviation-notes"
              labelText={
                <FormattedMessage
                  id="virology.qc.deviations"
                  defaultMessage="Deviations / Issues"
                />
              }
              placeholder="Document any deviations from acceptance criteria or quality issues..."
              value={deviationNotes}
              onChange={(e) => setDeviationNotes(e.target.value)}
              rows={3}
            />
          </Column>

          {/* QC Notes */}
          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="qc-notes"
              labelText={
                <FormattedMessage
                  id="virology.qc.notes"
                  defaultMessage="QC Notes"
                />
              }
              placeholder="Add any additional observations or notes about the quality control testing..."
              value={qcNotes}
              onChange={(e) => setQcNotes(e.target.value)}
              rows={3}
            />
          </Column>

          {/* Info about selected samples */}
          <Column lg={16} md={8} sm={4}>
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

export default VirologyQualityControlPage;
