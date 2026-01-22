import React, { useState, useEffect, useRef, useCallback, useMemo, useContext } from "react";
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
  NumberInput,
} from "@carbon/react";
import {
  Renew,
  CheckmarkFilled,
  Pending,
  WarningAltFilled,
  Edit,
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
import "../../workflow/NotebookWorkflow.css";

/**
 * TraditionalMedicinePreparationPage - Page 3 of the Traditional Medicine workflow.
 *
 * SRS Requirements - STAGE 4: Sample Preparation for Analysis
 * - Physical processing: Freshly processed, drying (air, oven, freeze)
 * - Preparation methods: Grinding, chopping, powdering
 * - Weight tracking and yield calculation
 * - Documentation: processing method, before/after weight, yield %, operator, date
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function TraditionalMedicinePreparationPage({
  entryId,
  pageData,
  progress: _progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } = useContext(NotificationContext);
  const componentMounted = useRef(false);

  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);

  // Preparation modal state
  const [preparationModalOpen, setPreparationModalOpen] = useState(false);
  const [isApplyingPrep, setIsApplyingPrep] = useState(false);

  // Preparation form fields
  const [processingMethod, setProcessingMethod] = useState(null);
  const [dryingMethod, setDryingMethod] = useState(null);
  const [weightBefore, setWeightBefore] = useState("");
  const [weightAfter, setWeightAfter] = useState("");
  const [dryingTemperature, setDryingTemperature] = useState("");
  const [dryingDuration, setDryingDuration] = useState("");
  const [prepNotes, setPrepNotes] = useState("");

  // Processing method options (per SRS)
  const processingMethodOptions = [
    { id: "fresh", label: "Freshly Processed (Used Immediately)" },
    { id: "grinding", label: "Grinding to Powder" },
    { id: "chopping", label: "Chopping to Coarse Pieces" },
    { id: "powdering", label: "Fine Powdering" },
    { id: "other", label: "Other Method" },
  ];

  // Drying method options (per SRS)
  const dryingMethodOptions = [
    { id: "air_drying", label: "Air Drying" },
    { id: "oven_drying", label: "Oven Drying (Controlled Temperature)" },
    { id: "freeze_drying", label: "Freeze Drying (Heat-Sensitive)" },
    { id: "no_drying", label: "No Drying Required" },
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
          if (response && Array.isArray(response)) {
            const transformedSamples = response.map((sample) => ({
              id: String(sample.id || sample.sampleItemId),
              externalId: sample.externalId,
              accessionNumber: sample.accessionNumber,
              status: sample.pageStatus || sample.status || "PENDING",
              localName: sample.data?.localName,
              scientificName: sample.data?.scientificName,
              // Preparation data
              processingMethod: sample.data?.processingMethod,
              dryingMethod: sample.data?.dryingMethod,
              weightBefore: sample.data?.weightBefore,
              weightAfter: sample.data?.weightAfter,
              yieldPercent: sample.data?.yieldPercent,
              dryingTemperature: sample.data?.dryingTemperature,
              dryingDuration: sample.data?.dryingDuration,
              preparedAt: sample.data?.preparedAt,
              preparedBy: sample.data?.preparedBy,
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

  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id, loadPageSamples]);

  const resetPrepForm = useCallback(() => {
    setProcessingMethod(null);
    setDryingMethod(null);
    setWeightBefore("");
    setWeightAfter("");
    setDryingTemperature("");
    setDryingDuration("");
    setPrepNotes("");
  }, []);

  const openPrepModal = useCallback(() => {
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
    resetPrepForm();
    setPreparationModalOpen(true);
  }, [selectedSampleIds, intl, resetPrepForm, notify]);

  const applyPreparation = useCallback(() => {
    if (!processingMethod) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.prep.error.methodRequired",
          defaultMessage: "Please select processing method.",
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

    setIsApplyingPrep(true);

    const sampleIds = selectedSampleIds.map((id) => parseInt(id, 10));
    const yieldPercent =
      weightBefore && weightAfter
        ? (((parseFloat(weightBefore) - parseFloat(weightAfter)) /
            parseFloat(weightBefore)) *
            100).toFixed(2)
        : null;

    postToOpenElisServerJsonResponse(
      `/rest/notebook/tradmed/page/${pageData.id}/preparation`,
      JSON.stringify({
        sampleIds: sampleIds,
        processingMethod: processingMethod.id,
        processingMethodLabel: processingMethod.label,
        dryingMethod: dryingMethod?.id || null,
        dryingMethodLabel: dryingMethod?.label || null,
        weightBefore: weightBefore || null,
        weightAfter: weightAfter || null,
        yieldPercent: yieldPercent,
        dryingTemperature: dryingTemperature || null,
        dryingDuration: dryingDuration || null,
        prepNotes: prepNotes || null,
      }),
      (response) => {
        setIsApplyingPrep(false);

        if (response && response.success) {
          // Update sample status using bulk endpoint after preparation
          postToOpenElisServer(
            `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
            JSON.stringify({
              sampleIds: sampleIds,
              status: "IN_PROGRESS",
            }),
            (statusCode) => {
              if (statusCode === 200) {
                notify({
                  kind: NotificationKinds.success,
                  title: response.message ||
                    intl.formatMessage(
                      {
                        id: "notebook.page.tradmed.prep.success",
                        defaultMessage: "Prepared {count} sample(s).",
                      },
                      { count: response.updatedCount || selectedSampleIds.length },
                    ),
                });
                setPreparationModalOpen(false);
                setSelectedSampleIds([]);
                loadPageSamples();
                if (onProgressUpdate) onProgressUpdate();
              } else {
                notify({
                  kind: NotificationKinds.error,
                  title: intl.formatMessage({
                    id: "notebook.page.tradmed.error.statusUpdate",
                    defaultMessage: "Preparation recorded but failed to update sample status.",
                  }),
                });
              }
            }
          );
        } else {
          notify({
            kind: NotificationKinds.error,
            title: response?.error ||
              intl.formatMessage({
                id: "notebook.page.tradmed.prep.error.failed",
                defaultMessage:
                  "Failed to prepare samples. Please try again.",
              }),
          });
        }
      },
    );
  }, [
    processingMethod,
    dryingMethod,
    weightBefore,
    weightAfter,
    dryingTemperature,
    dryingDuration,
    prepNotes,
    hasRealPageId,
    pageData?.id,
    selectedSampleIds,
    intl,
    loadPageSamples,
    onProgressUpdate,
    notify,
  ]);

  const unpreparedSamples = useMemo(
    () => samples.filter((s) => !s.processingMethod),
    [samples],
  );
  const preparedSamples = useMemo(
    () => samples.filter((s) => s.processingMethod && s.status === "COMPLETED"),
    [samples],
  );

  const renderYieldTag = (sample) => {
    if (!sample.yieldPercent) return "-";
    const yieldNum = parseFloat(sample.yieldPercent);
    const type = yieldNum > 50 ? "green" : yieldNum > 30 ? "blue" : "red";
    return (
      <Tag type={type} size="sm">
        {sample.yieldPercent}%
      </Tag>
    );
  };

  return (
    <div className="tradmed-preparation-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.tradmed.prep.title"
            defaultMessage="Sample Preparation for Analysis"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.tradmed.prep.description"
            defaultMessage="Process and prepare samples through grinding, chopping, drying, or powdering with weight tracking and yield calculation."
          />
        </p>
      </div>

      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.prep.readyForPrep"
                  defaultMessage="Ready for Preparation"
                />
              </span>
              <span className="progress-value">{unpreparedSamples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.prep.prepared"
                  defaultMessage="Prepared"
                />
              </span>
              <span className="progress-value">{preparedSamples.length}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Edit}
          onClick={openPrepModal}
          disabled={selectedSampleIds.length === 0 || !hasRealPageId}
        >
          <FormattedMessage
            id="notebook.page.tradmed.prep.recordPrep"
            defaultMessage="Record Preparation ({count})"
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
              id="notebook.page.tradmed.prep.unprepared.title"
              defaultMessage="Samples Awaiting Preparation"
            />
            <Tag type="blue" size="sm" className="count-tag">
              {unpreparedSamples.length}
            </Tag>
          </h5>
        </div>
        <div className="sample-grid-container">
          {!loading && unpreparedSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.tradmed.prep.unprepared.empty"
                  defaultMessage="No samples awaiting preparation."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="unprepared-samples"
              samples={unpreparedSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              showSelection={true}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "localName", header: "Local Name" },
                { key: "scientificName", header: "Scientific Name" },
              ]}
            />
          )}
        </div>
      </div>

      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.tradmed.prep.prepared.title"
              defaultMessage="Prepared Samples"
            />
            <Tag type="green" size="sm" className="count-tag">
              {preparedSamples.length}
            </Tag>
          </h5>
        </div>
        <div className="sample-grid-container">
          {!loading && preparedSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.tradmed.prep.prepared.empty"
                  defaultMessage="No samples prepared yet."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="prepared-samples"
              samples={preparedSamples}
              showSelection={false}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "localName", header: "Local Name" },
                { key: "processingMethod", header: "Processing Method" },
                { key: "weightBefore", header: "Weight Before (g)" },
                { key: "weightAfter", header: "Weight After (g)" },
                {
                  key: "yieldPercent",
                  header: "Yield %",
                  render: (_value, row) => renderYieldTag(row),
                },
              ]}
            />
          )}
        </div>
      </div>

      <Modal
        open={preparationModalOpen}
        onRequestClose={() => setPreparationModalOpen(false)}
        onRequestSubmit={applyPreparation}
        modalHeading={intl.formatMessage({
          id: "notebook.page.tradmed.prep.modal.title",
          defaultMessage: "Record Sample Preparation",
        })}
        primaryButtonText={
          isApplyingPrep
            ? intl.formatMessage({
                id: "label.recording",
                defaultMessage: "Recording...",
              })
            : intl.formatMessage({
                id: "notebook.page.tradmed.prep.modal.record",
                defaultMessage: "Record Preparation",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        primaryButtonDisabled={isApplyingPrep}
        size="md"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p>
            <FormattedMessage
              id="notebook.page.tradmed.prep.modal.description"
              defaultMessage="Record preparation details for {count} sample(s)."
              values={{ count: selectedSampleIds.length }}
            />
          </p>
        </div>

        {isApplyingPrep && <Loading withOverlay={false} small />}

        <Grid fullWidth narrow>
          <Column lg={16} md={8} sm={4}>
            <Dropdown
              id="processing-method"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.prep.modal.method",
                defaultMessage: "Processing Method *",
              })}
              label={intl.formatMessage({
                id: "label.select",
                defaultMessage: "Select...",
              })}
              items={processingMethodOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={processingMethod}
              onChange={({ selectedItem }) => setProcessingMethod(selectedItem)}
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <Dropdown
              id="drying-method"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.prep.modal.drying",
                defaultMessage: "Drying Method",
              })}
              label={intl.formatMessage({
                id: "label.select",
                defaultMessage: "Select...",
              })}
              items={dryingMethodOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={dryingMethod}
              onChange={({ selectedItem }) => setDryingMethod(selectedItem)}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <NumberInput
              id="weight-before"
              label={intl.formatMessage({
                id: "notebook.page.tradmed.prep.modal.weightBefore",
                defaultMessage: "Weight Before (g)",
              })}
              value={weightBefore}
              onChange={(e) => setWeightBefore(e.imaginaryTarget?.value || e.target?.value || "")}
              step={0.1}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <NumberInput
              id="weight-after"
              label={intl.formatMessage({
                id: "notebook.page.tradmed.prep.modal.weightAfter",
                defaultMessage: "Weight After (g)",
              })}
              value={weightAfter}
              onChange={(e) => setWeightAfter(e.imaginaryTarget?.value || e.target?.value || "")}
              step={0.1}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <NumberInput
              id="drying-temp"
              label={intl.formatMessage({
                id: "notebook.page.tradmed.prep.modal.dryingTemp",
                defaultMessage: "Drying Temperature (°C)",
              })}
              value={dryingTemperature}
              onChange={(e) => setDryingTemperature(e.imaginaryTarget?.value || e.target?.value || "")}
              step={1}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="drying-duration"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.prep.modal.dryingDuration",
                defaultMessage: "Drying Duration (hours)",
              })}
              value={dryingDuration}
              onChange={(e) => setDryingDuration(e.target.value)}
              placeholder="e.g., 24"
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="prep-notes"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.prep.modal.notes",
                defaultMessage: "Preparation Notes",
              })}
              value={prepNotes}
              onChange={(e) => setPrepNotes(e.target.value)}
              rows={2}
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default TraditionalMedicinePreparationPage;
