import { useState, useEffect, useRef, useCallback, useMemo } from "react";
import {
  Grid,
  Column,
  Button,
  InlineNotification,
  Select,
  SelectItem,
  TextArea,
  Tile,
  Modal,
  RadioButtonGroup,
  RadioButton,
  Tag,
  Checkbox,
  TextInput,
  MultiSelect,
  DatePicker,
  DatePickerInput,
  TimePicker,
  TimePickerSelect,
  Accordion,
  AccordionItem,
} from "@carbon/react";
import {
  Checkmark,
  Edit,
  InventoryManagement,
  Chemistry,
  Warning,
  Time,
  DocumentAdd,
  Task,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * ImmunologyAdditionalAssaysPage - Page 3 of the Immunology workflow.
 * STAGE 3: Additional Assays (If Required)
 *
 * Process:
 * - Perform supplementary tests prior to extraction (per protocol)
 * - May include:
 *   - Preliminary cell phenotyping
 *   - Viability assays
 *   - Preliminary functional assays
 *   - Contamination checks
 *
 * Documentation:
 * - Test type performed
 * - Operator name
 * - Reagents used (with lot numbers)
 * - Results obtained
 * - Pass/fail status
 * - Any deviations from protocol
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 * @param {Array} props.templateInstruments - Instruments from template
 */
function ImmunologyAdditionalAssaysPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  templateInstruments,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Bulk apply modal state
  const [bulkApplyModalOpen, setBulkApplyModalOpen] = useState(false);
  const [isBulkApplying, setIsBulkApplying] = useState(false);

  // Reagents and Instruments from inventory
  const [reagents, setReagents] = useState([]);
  const [instruments, setInstruments] = useState([]);
  const [loadingReagents, setLoadingReagents] = useState(false);
  const [loadingInstruments, setLoadingInstruments] = useState(false);

  // Assay form values
  const [assayValues, setAssayValues] = useState({
    // Test Type Selection
    testType: "",
    testTypeOther: "",

    // Assay Details
    assayProtocol: "",
    assayPurpose: "",

    // Operator Information
    operatorName: "",
    operatorInitials: "",

    // Reagent & Instrument Selection (from inventory)
    selectedReagents: [],
    selectedEquipment: [],

    // Timing
    assayStartTime: "",
    assayEndTime: "",
    assayDate: new Date().toISOString().split("T")[0],

    // Results
    resultsSummary: "",
    resultsQuantitative: "",
    resultsUnit: "",

    // Pass/Fail Status
    assayResult: "", // "Pass" or "Fail"
    failAction: "", // "reject" or "continue_with_caution"
    failureReason: "",

    // Deviations
    deviationObserved: false,
    deviationDescription: "",
    deviationImpact: "",
    correctiveAction: "",

    // Notes
    generalNotes: "",
  });

  // Test types for immunology additional assays
  const testTypes = useMemo(
    () => [
      {
        value: "cell_phenotyping",
        label: intl.formatMessage({
          id: "notebook.immunology.assay.cellPhenotyping",
          defaultMessage: "Preliminary Cell Phenotyping",
        }),
      },
      {
        value: "viability_assay",
        label: intl.formatMessage({
          id: "notebook.immunology.assay.viabilityAssay",
          defaultMessage: "Viability Assay",
        }),
      },
      {
        value: "functional_assay",
        label: intl.formatMessage({
          id: "notebook.immunology.assay.functionalAssay",
          defaultMessage: "Preliminary Functional Assay",
        }),
      },
      {
        value: "contamination_check",
        label: intl.formatMessage({
          id: "notebook.immunology.assay.contaminationCheck",
          defaultMessage: "Contamination Check",
        }),
      },
      {
        value: "elisa",
        label: intl.formatMessage({
          id: "notebook.immunology.assay.elisa",
          defaultMessage: "ELISA",
        }),
      },
      {
        value: "flow_cytometry",
        label: intl.formatMessage({
          id: "notebook.immunology.assay.flowCytometry",
          defaultMessage: "Flow Cytometry",
        }),
      },
      {
        value: "western_blot",
        label: intl.formatMessage({
          id: "notebook.immunology.assay.westernBlot",
          defaultMessage: "Western Blot",
        }),
      },
      {
        value: "pcr",
        label: intl.formatMessage({
          id: "notebook.immunology.assay.pcr",
          defaultMessage: "PCR",
        }),
      },
      {
        value: "other",
        label: intl.formatMessage({
          id: "notebook.immunology.assay.other",
          defaultMessage: "Other",
        }),
      },
    ],
    [intl],
  );

  // Load reagents from inventory
  const loadReagents = useCallback(() => {
    setLoadingReagents(true);
    getFromOpenElisServer(
      "/rest/inventory/reagents?status=active",
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            setReagents(
              response.map((r) => ({
                id: r.id,
                label: `${r.name} (Lot: ${r.lotNumber || "N/A"})`,
                name: r.name,
                lotNumber: r.lotNumber,
                ...r,
              })),
            );
          }
          setLoadingReagents(false);
        }
      },
    );
  }, []);

  // Load instruments from template or inventory
  const loadInstruments = useCallback(() => {
    if (templateInstruments && templateInstruments.length > 0) {
      setInstruments(
        templateInstruments.map((analyzer) => ({
          id: analyzer.id,
          label: analyzer.value,
          name: analyzer.value,
        })),
      );
      setLoadingInstruments(false);
      return;
    }

    setLoadingInstruments(true);
    getFromOpenElisServer(
      "/rest/inventory/instruments?status=active",
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            setInstruments(
              response.map((i) => ({
                id: i.id,
                label: `${i.name} (${i.serialNumber || "N/A"})`,
                name: i.name,
                serialNumber: i.serialNumber,
                ...i,
              })),
            );
          }
          setLoadingInstruments(false);
        }
      },
    );
  }, [templateInstruments]);

  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    loadReagents();
    loadInstruments();
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
    setError(null);
    setSuccessMessage(null);

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
              status: sample.pageStatus || sample.status || "PENDING",
              // Assay fields from data
              testType: sample.data?.testType,
              testTypeOther: sample.data?.testTypeOther,
              operatorName: sample.data?.operatorName,
              assayResult: sample.data?.assayResult,
              failAction: sample.data?.failAction,
              deviationObserved: sample.data?.deviationObserved,
              resultsSummary: sample.data?.resultsSummary,
              assayDate: sample.data?.assayDate,
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

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Reset form
  const resetAssayValues = () => {
    setAssayValues({
      testType: "",
      testTypeOther: "",
      assayProtocol: "",
      assayPurpose: "",
      operatorName: "",
      operatorInitials: "",
      selectedReagents: [],
      selectedEquipment: [],
      assayStartTime: "",
      assayEndTime: "",
      assayDate: new Date().toISOString().split("T")[0],
      resultsSummary: "",
      resultsQuantitative: "",
      resultsUnit: "",
      assayResult: "",
      failAction: "",
      failureReason: "",
      deviationObserved: false,
      deviationDescription: "",
      deviationImpact: "",
      correctiveAction: "",
      generalNotes: "",
    });
  };

  // Build data object from form values
  const buildAssayData = () => {
    const data = {};

    // Test Type
    if (assayValues.testType) data.testType = assayValues.testType;
    if (assayValues.testType === "other" && assayValues.testTypeOther)
      data.testTypeOther = assayValues.testTypeOther;

    // Assay Details
    if (assayValues.assayProtocol)
      data.assayProtocol = assayValues.assayProtocol;
    if (assayValues.assayPurpose) data.assayPurpose = assayValues.assayPurpose;

    // Operator
    if (assayValues.operatorName) data.operatorName = assayValues.operatorName;
    if (assayValues.operatorInitials)
      data.operatorInitials = assayValues.operatorInitials;

    // Reagents & Equipment
    if (assayValues.selectedReagents.length > 0)
      data.selectedReagents = assayValues.selectedReagents;
    if (assayValues.selectedEquipment.length > 0)
      data.selectedEquipment = assayValues.selectedEquipment;

    // Timing
    if (assayValues.assayStartTime)
      data.assayStartTime = assayValues.assayStartTime;
    if (assayValues.assayEndTime) data.assayEndTime = assayValues.assayEndTime;
    if (assayValues.assayDate) data.assayDate = assayValues.assayDate;

    // Results
    if (assayValues.resultsSummary)
      data.resultsSummary = assayValues.resultsSummary;
    if (assayValues.resultsQuantitative)
      data.resultsQuantitative = assayValues.resultsQuantitative;
    if (assayValues.resultsUnit) data.resultsUnit = assayValues.resultsUnit;

    // Pass/Fail
    if (assayValues.assayResult) data.assayResult = assayValues.assayResult;
    if (assayValues.failAction) data.failAction = assayValues.failAction;
    if (assayValues.failureReason)
      data.failureReason = assayValues.failureReason;

    // Deviations
    data.deviationObserved = assayValues.deviationObserved;
    if (assayValues.deviationObserved) {
      if (assayValues.deviationDescription)
        data.deviationDescription = assayValues.deviationDescription;
      if (assayValues.deviationImpact)
        data.deviationImpact = assayValues.deviationImpact;
      if (assayValues.correctiveAction)
        data.correctiveAction = assayValues.correctiveAction;
    }

    // Notes
    if (assayValues.generalNotes) data.generalNotes = assayValues.generalNotes;

    return data;
  };

  // Handle bulk apply
  const handleBulkApply = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.immunology.assay.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.immunology.assay.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    const data = buildAssayData();

    if (Object.keys(data).length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.immunology.assay.error.noData",
          defaultMessage: "Please complete at least one field before applying.",
        }),
      );
      return;
    }

    setIsBulkApplying(true);
    setError(null);

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        data: data,
      }),
      (status) => {
        setIsBulkApplying(false);
        if (status === 200) {
          setSuccessMessage(
            intl.formatMessage(
              {
                id: "notebook.page.immunology.assay.success.applied",
                defaultMessage:
                  "Assay data applied to {count} sample(s) successfully.",
              },
              { count: selectedSampleIds.length },
            ),
          );
          setBulkApplyModalOpen(false);
          resetAssayValues();
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            intl.formatMessage({
              id: "notebook.page.immunology.assay.error.apply",
              defaultMessage: "Failed to apply assay data. Please try again.",
            }),
          );
        }
      },
    );
  }, [
    selectedSampleIds,
    hasRealPageId,
    intl,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    assayValues,
  ]);

  // Handle mark as complete
  const handleMarkAssayComplete = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.immunology.assay.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.immunology.assay.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    const selectedSamples = samples.filter((s) =>
      selectedSampleIds.includes(s.id),
    );

    // Check if all have assay result
    const missingResult = selectedSamples.filter((s) => !s.assayResult);
    if (missingResult.length > 0) {
      setError(
        intl.formatMessage(
          {
            id: "notebook.page.immunology.assay.error.noResult",
            defaultMessage:
              "{count} sample(s) are missing assay result (Pass/Fail). Please complete assay data first.",
          },
          { count: missingResult.length },
        ),
      );
      return;
    }

    // Separate samples by result and action
    const passedSamples = selectedSamples.filter(
      (s) => s.assayResult === "Pass",
    );
    const continueWithCautionSamples = selectedSamples.filter(
      (s) =>
        s.assayResult === "Fail" && s.failAction === "continue_with_caution",
    );
    const rejectedSamples = selectedSamples.filter(
      (s) =>
        s.assayResult === "Fail" &&
        (s.failAction === "reject" || !s.failAction),
    );

    // Check if failed samples have an action selected
    const failedNoAction = selectedSamples.filter(
      (s) => s.assayResult === "Fail" && !s.failAction,
    );
    if (failedNoAction.length > 0) {
      setError(
        intl.formatMessage(
          {
            id: "notebook.page.immunology.assay.error.noFailAction",
            defaultMessage:
              "{count} failed sample(s) do not have an action selected. Please select 'Reject' or 'Continue with Caution' for failed samples.",
          },
          { count: failedNoAction.length },
        ),
      );
      return;
    }

    const proceedingSamples = [...passedSamples, ...continueWithCautionSamples];

    let completedRequests = 0;
    let failedRequests = 0;
    const totalRequests =
      (proceedingSamples.length > 0 ? 1 : 0) +
      (rejectedSamples.length > 0 ? 1 : 0);

    const handleRequestComplete = () => {
      completedRequests++;
      if (completedRequests === totalRequests) {
        if (failedRequests === 0) {
          let message = "";
          if (proceedingSamples.length > 0 && rejectedSamples.length > 0) {
            message = intl.formatMessage(
              {
                id: "notebook.page.immunology.assay.completedMixed",
                defaultMessage:
                  "{proceedCount} sample(s) completed and can proceed. {rejectCount} sample(s) rejected.",
              },
              {
                proceedCount: proceedingSamples.length,
                rejectCount: rejectedSamples.length,
              },
            );
          } else if (rejectedSamples.length > 0) {
            message = intl.formatMessage(
              {
                id: "notebook.page.immunology.assay.completedRejected",
                defaultMessage: "{count} sample(s) rejected.",
              },
              { count: rejectedSamples.length },
            );
          } else {
            message = intl.formatMessage(
              {
                id: "notebook.page.immunology.assay.completed",
                defaultMessage:
                  "Marked {count} sample(s) as Assay Complete. They can now proceed to next step.",
              },
              { count: proceedingSamples.length },
            );
          }
          setSuccessMessage(message);
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            intl.formatMessage({
              id: "notebook.page.immunology.error.status",
              defaultMessage: "Failed to update status. Please try again.",
            }),
          );
        }
      }
    };

    // Update proceeding samples to COMPLETED
    if (proceedingSamples.length > 0) {
      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
        JSON.stringify({
          sampleIds: proceedingSamples.map((s) => parseInt(s.id, 10)),
          status: "COMPLETED",
        }),
        (status) => {
          if (status !== 200) {
            failedRequests++;
          }
          handleRequestComplete();
        },
      );
    }

    // Update rejected samples to REJECTED status
    if (rejectedSamples.length > 0) {
      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
        JSON.stringify({
          sampleIds: rejectedSamples.map((s) => parseInt(s.id, 10)),
          status: "REJECTED",
        }),
        (status) => {
          if (status !== 200) {
            failedRequests++;
          }
          handleRequestComplete();
        },
      );
    }
  }, [
    selectedSampleIds,
    hasRealPageId,
    intl,
    samples,
    loadPageSamples,
    onProgressUpdate,
    pageData?.id,
  ]);

  // Calculate stats
  const passedCount = samples.filter((s) => s.assayResult === "Pass").length;
  const failedCount = samples.filter((s) => s.assayResult === "Fail").length;
  const pendingResultCount = samples.filter((s) => !s.assayResult).length;
  const completedCount = samples.filter((s) => s.status === "COMPLETED").length;
  const rejectedCount = samples.filter((s) => s.status === "REJECTED").length;
  const deviationCount = samples.filter((s) => s.deviationObserved).length;

  // Get assay result tag
  const getAssayResultTag = (result) => {
    if (!result) return <Tag type="gray">Pending</Tag>;
    if (result === "Pass") return <Tag type="green">Pass</Tag>;
    if (result === "Fail") return <Tag type="red">Fail</Tag>;
    return <Tag type="gray">{result}</Tag>;
  };

  // Get test type label
  const getTestTypeLabel = (typeValue) => {
    if (!typeValue) return "-";
    const type = testTypes.find((t) => t.value === typeValue);
    return type ? type.label : typeValue;
  };

  return (
    <div className="immunology-additional-assays-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.immunology.assay.title"
            defaultMessage="Additional Assays"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.immunology.assay.description"
            defaultMessage="Perform supplementary tests prior to extraction per protocol. Record test type, operator, reagents, results, and any protocol deviations."
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
                  id="notebook.page.immunology.assay.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.immunology.assay.pendingResult"
                  defaultMessage="Pending Result"
                />
              </span>
              <span className="progress-value">{pendingResultCount}</span>
            </Tile>
            <Tile className="progress-tile success">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.immunology.assay.passed"
                  defaultMessage="Passed"
                />
              </span>
              <span className="progress-value">{passedCount}</span>
            </Tile>
            <Tile className="progress-tile error">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.immunology.assay.failed"
                  defaultMessage="Failed"
                />
              </span>
              <span className="progress-value">{failedCount}</span>
            </Tile>
            {deviationCount > 0 && (
              <Tile className="progress-tile warning">
                <span className="progress-label">
                  <FormattedMessage
                    id="notebook.page.immunology.assay.deviations"
                    defaultMessage="Deviations"
                  />
                </span>
                <span className="progress-value">{deviationCount}</span>
              </Tile>
            )}
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.immunology.assay.completed"
                  defaultMessage="Completed"
                />
              </span>
              <span className="progress-value">{completedCount}</span>
            </Tile>
            {rejectedCount > 0 && (
              <Tile className="progress-tile error">
                <span className="progress-label">
                  <FormattedMessage
                    id="notebook.page.immunology.assay.rejected"
                    defaultMessage="Rejected"
                  />
                </span>
                <span className="progress-value">{rejectedCount}</span>
              </Tile>
            )}
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Edit}
          onClick={() => setBulkApplyModalOpen(true)}
          disabled={selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="notebook.page.immunology.assay.bulkApply"
            defaultMessage="Record Assay Data ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        {selectedSampleIds.length > 0 && (
          <Button
            kind="secondary"
            size="sm"
            renderIcon={Checkmark}
            onClick={handleMarkAssayComplete}
          >
            <FormattedMessage
              id="notebook.page.immunology.assay.markComplete"
              defaultMessage="Mark Assay Complete ({count})"
              values={{ count: selectedSampleIds.length }}
            />
          </Button>
        )}
      </div>

      {/* Errors / Success */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          hideCloseButton
          lowContrast
        />
      )}
      {successMessage && (
        <InlineNotification
          kind="success"
          title={successMessage}
          hideCloseButton
          lowContrast
        />
      )}

      {/* Pending Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.immunology.assay.pendingTable.title"
              defaultMessage="Samples Pending Assay"
            />
            <Tag type="gray" className="count-tag">
              {
                samples.filter(
                  (s) =>
                    s.status !== "COMPLETED" &&
                    s.status !== "SKIPPED" &&
                    s.status !== "REJECTED",
                ).length
              }
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.immunology.assay.pendingTable.description"
              defaultMessage="Select samples and use 'Record Assay Data' to document test results and observations."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          <SampleGrid
            gridId="pending-assay"
            samples={samples.filter(
              (s) =>
                s.status !== "COMPLETED" &&
                s.status !== "SKIPPED" &&
                s.status !== "REJECTED",
            )}
            selectedIds={selectedSampleIds}
            onSelectionChange={setSelectedSampleIds}
            statusFilter={statusFilter}
            onStatusFilterChange={setStatusFilter}
            showSelection={true}
            loading={loading}
            columns={[
              {
                key: "accessionNumber",
                header: intl.formatMessage({
                  id: "notebook.grid.accessionNumber",
                  defaultMessage: "Accession #",
                }),
              },
              {
                key: "externalId",
                header: intl.formatMessage({
                  id: "notebook.grid.externalId",
                  defaultMessage: "External ID",
                }),
              },
              {
                key: "sampleType",
                header: intl.formatMessage({
                  id: "notebook.grid.sampleType",
                  defaultMessage: "Sample Type",
                }),
              },
              {
                key: "testType",
                header: intl.formatMessage({
                  id: "notebook.grid.testType",
                  defaultMessage: "Test Type",
                }),
                render: (value) => getTestTypeLabel(value),
              },
              {
                key: "operatorName",
                header: intl.formatMessage({
                  id: "notebook.grid.operator",
                  defaultMessage: "Operator",
                }),
                render: (value) => value || "-",
              },
              {
                key: "assayResult",
                header: intl.formatMessage({
                  id: "notebook.grid.result",
                  defaultMessage: "Result",
                }),
                render: (value) => getAssayResultTag(value),
              },
              {
                key: "deviationObserved",
                header: intl.formatMessage({
                  id: "notebook.grid.deviation",
                  defaultMessage: "Deviation",
                }),
                render: (value) =>
                  value ? (
                    <Tag type="magenta" size="sm">
                      <Warning size={12} /> Yes
                    </Tag>
                  ) : (
                    "-"
                  ),
              },
              {
                key: "status",
                header: intl.formatMessage({
                  id: "notebook.grid.status",
                  defaultMessage: "Status",
                }),
              },
            ]}
          />
        </div>
        {!loading &&
          samples.filter(
            (s) =>
              s.status !== "COMPLETED" &&
              s.status !== "SKIPPED" &&
              s.status !== "REJECTED",
          ).length === 0 && (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.immunology.assay.pendingTable.empty"
                  defaultMessage="No samples pending assay. All samples have been processed."
                />
              </p>
            </div>
          )}
      </div>

      {/* Completed Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.immunology.assay.completedTable.title"
              defaultMessage="Assay Completed"
            />
            <Tag type="green" className="count-tag">
              {
                samples.filter(
                  (s) =>
                    s.status === "COMPLETED" ||
                    s.status === "SKIPPED" ||
                    s.status === "REJECTED",
                ).length
              }
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.immunology.assay.completedTable.description"
              defaultMessage="Samples that have completed assays. Passed samples can proceed to the next workflow step."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          <SampleGrid
            gridId="completed-assay"
            samples={samples.filter(
              (s) =>
                s.status === "COMPLETED" ||
                s.status === "SKIPPED" ||
                s.status === "REJECTED",
            )}
            selectedIds={[]}
            showSelection={false}
            loading={loading}
            columns={[
              {
                key: "accessionNumber",
                header: intl.formatMessage({
                  id: "notebook.grid.accessionNumber",
                  defaultMessage: "Accession #",
                }),
              },
              {
                key: "externalId",
                header: intl.formatMessage({
                  id: "notebook.grid.externalId",
                  defaultMessage: "External ID",
                }),
              },
              {
                key: "testType",
                header: intl.formatMessage({
                  id: "notebook.grid.testType",
                  defaultMessage: "Test Type",
                }),
                render: (value) => getTestTypeLabel(value),
              },
              {
                key: "operatorName",
                header: intl.formatMessage({
                  id: "notebook.grid.operator",
                  defaultMessage: "Operator",
                }),
                render: (value) => value || "-",
              },
              {
                key: "assayResult",
                header: intl.formatMessage({
                  id: "notebook.grid.result",
                  defaultMessage: "Result",
                }),
                render: (value) => getAssayResultTag(value),
              },
              {
                key: "deviationObserved",
                header: intl.formatMessage({
                  id: "notebook.grid.deviation",
                  defaultMessage: "Deviation",
                }),
                render: (value) =>
                  value ? (
                    <Tag type="magenta" size="sm">
                      <Warning size={12} /> Yes
                    </Tag>
                  ) : (
                    "-"
                  ),
              },
              {
                key: "status",
                header: intl.formatMessage({
                  id: "notebook.grid.disposition",
                  defaultMessage: "Disposition",
                }),
                render: (value) => {
                  if (value === "REJECTED") {
                    return (
                      <Tag type="red">
                        <FormattedMessage
                          id="notebook.disposition.rejected"
                          defaultMessage="Rejected"
                        />
                      </Tag>
                    );
                  }
                  if (value === "SKIPPED") {
                    return (
                      <Tag type="magenta">
                        <FormattedMessage
                          id="notebook.disposition.notProceeding"
                          defaultMessage="Not Proceeding"
                        />
                      </Tag>
                    );
                  }
                  if (value === "COMPLETED") {
                    return (
                      <Tag type="green">
                        <FormattedMessage
                          id="notebook.disposition.proceeding"
                          defaultMessage="Proceeding"
                        />
                      </Tag>
                    );
                  }
                  return "-";
                },
              },
            ]}
          />
        </div>
        {!loading &&
          samples.filter(
            (s) =>
              s.status === "COMPLETED" ||
              s.status === "SKIPPED" ||
              s.status === "REJECTED",
          ).length === 0 && (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.immunology.assay.completedTable.empty"
                  defaultMessage="No samples have completed assays yet."
                />
              </p>
            </div>
          )}
      </div>

      {/* Bulk Apply Modal */}
      <Modal
        open={bulkApplyModalOpen}
        onRequestClose={() => {
          setBulkApplyModalOpen(false);
          resetAssayValues();
        }}
        onRequestSubmit={handleBulkApply}
        modalHeading={intl.formatMessage({
          id: "notebook.page.immunology.assay.modal.title",
          defaultMessage: "Record Assay Data",
        })}
        primaryButtonText={
          isBulkApplying
            ? intl.formatMessage({
                id: "notebook.applying",
                defaultMessage: "Applying...",
              })
            : intl.formatMessage({
                id: "notebook.apply",
                defaultMessage: "Apply to Selected",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "notebook.cancel",
          defaultMessage: "Cancel",
        })}
        primaryButtonDisabled={isBulkApplying}
        size="lg"
      >
        <div className="bulk-apply-modal-content">
          <p className="modal-description">
            <FormattedMessage
              id="notebook.page.immunology.assay.modal.description"
              defaultMessage="Recording assay data for {count} selected sample(s). Complete the sections below."
              values={{ count: selectedSampleIds.length }}
            />
          </p>

          <Accordion>
            {/* Section 1: Test Type & Details */}
            <AccordionItem
              title={
                <span className="accordion-title">
                  <Task size={16} style={{ marginRight: "0.5rem" }} />
                  <FormattedMessage
                    id="notebook.immunology.assay.section.testType"
                    defaultMessage="A. Test Type & Details"
                  />
                </span>
              }
              open
            >
              <Grid narrow>
                <Column lg={8} md={4} sm={4}>
                  <Select
                    id="testType"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.assay.testType",
                      defaultMessage: "Test Type Performed",
                    })}
                    value={assayValues.testType}
                    onChange={(e) =>
                      setAssayValues((prev) => ({
                        ...prev,
                        testType: e.target.value,
                      }))
                    }
                  >
                    <SelectItem value="" text="Select test type..." />
                    {testTypes.map((type) => (
                      <SelectItem
                        key={type.value}
                        value={type.value}
                        text={type.label}
                      />
                    ))}
                  </Select>
                </Column>
                {assayValues.testType === "other" && (
                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      id="testTypeOther"
                      labelText={intl.formatMessage({
                        id: "notebook.immunology.assay.testTypeOther",
                        defaultMessage: "Specify Other Test Type",
                      })}
                      value={assayValues.testTypeOther}
                      onChange={(e) =>
                        setAssayValues((prev) => ({
                          ...prev,
                          testTypeOther: e.target.value,
                        }))
                      }
                    />
                  </Column>
                )}
                <Column lg={8} md={4} sm={4}>
                  <TextInput
                    id="assayProtocol"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.assay.protocol",
                      defaultMessage: "Protocol Reference",
                    })}
                    value={assayValues.assayProtocol}
                    onChange={(e) =>
                      setAssayValues((prev) => ({
                        ...prev,
                        assayProtocol: e.target.value,
                      }))
                    }
                    placeholder={intl.formatMessage({
                      id: "notebook.immunology.assay.protocol.placeholder",
                      defaultMessage: "e.g., SOP-IMM-003 v2.1",
                    })}
                  />
                </Column>
                <Column lg={16} md={8} sm={4}>
                  <TextArea
                    id="assayPurpose"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.assay.purpose",
                      defaultMessage: "Purpose / Objective",
                    })}
                    value={assayValues.assayPurpose}
                    onChange={(e) =>
                      setAssayValues((prev) => ({
                        ...prev,
                        assayPurpose: e.target.value,
                      }))
                    }
                    placeholder={intl.formatMessage({
                      id: "notebook.immunology.assay.purpose.placeholder",
                      defaultMessage: "Describe the purpose of this assay...",
                    })}
                    rows={2}
                  />
                </Column>
              </Grid>
            </AccordionItem>

            {/* Section 2: Operator & Timing */}
            <AccordionItem
              title={
                <span className="accordion-title">
                  <Time size={16} style={{ marginRight: "0.5rem" }} />
                  <FormattedMessage
                    id="notebook.immunology.assay.section.operatorTiming"
                    defaultMessage="B. Operator & Timing"
                  />
                </span>
              }
            >
              <Grid narrow>
                <Column lg={6} md={4} sm={4}>
                  <TextInput
                    id="operatorName"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.assay.operatorName",
                      defaultMessage: "Operator Name",
                    })}
                    value={assayValues.operatorName}
                    onChange={(e) =>
                      setAssayValues((prev) => ({
                        ...prev,
                        operatorName: e.target.value,
                      }))
                    }
                  />
                </Column>
                <Column lg={4} md={2} sm={2}>
                  <TextInput
                    id="operatorInitials"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.assay.operatorInitials",
                      defaultMessage: "Initials",
                    })}
                    value={assayValues.operatorInitials}
                    onChange={(e) =>
                      setAssayValues((prev) => ({
                        ...prev,
                        operatorInitials: e.target.value,
                      }))
                    }
                    maxLength={5}
                  />
                </Column>
                <Column lg={6} md={4} sm={4}>
                  <DatePicker
                    datePickerType="single"
                    value={assayValues.assayDate}
                    onChange={([date]) =>
                      setAssayValues((prev) => ({
                        ...prev,
                        assayDate: date ? date.toISOString().split("T")[0] : "",
                      }))
                    }
                  >
                    <DatePickerInput
                      id="assayDate"
                      labelText={intl.formatMessage({
                        id: "notebook.immunology.assay.date",
                        defaultMessage: "Date Performed",
                      })}
                      placeholder="mm/dd/yyyy"
                    />
                  </DatePicker>
                </Column>
                <Column lg={4} md={2} sm={2}>
                  <TimePicker
                    id="assayStartTime"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.assay.startTime",
                      defaultMessage: "Start Time",
                    })}
                    value={assayValues.assayStartTime}
                    onChange={(e) =>
                      setAssayValues((prev) => ({
                        ...prev,
                        assayStartTime: e.target.value,
                      }))
                    }
                  >
                    <TimePickerSelect id="startTimePeriod" labelText="AM/PM">
                      <SelectItem value="AM" text="AM" />
                      <SelectItem value="PM" text="PM" />
                    </TimePickerSelect>
                  </TimePicker>
                </Column>
                <Column lg={4} md={2} sm={2}>
                  <TimePicker
                    id="assayEndTime"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.assay.endTime",
                      defaultMessage: "End Time",
                    })}
                    value={assayValues.assayEndTime}
                    onChange={(e) =>
                      setAssayValues((prev) => ({
                        ...prev,
                        assayEndTime: e.target.value,
                      }))
                    }
                  >
                    <TimePickerSelect id="endTimePeriod" labelText="AM/PM">
                      <SelectItem value="AM" text="AM" />
                      <SelectItem value="PM" text="PM" />
                    </TimePickerSelect>
                  </TimePicker>
                </Column>
              </Grid>
            </AccordionItem>

            {/* Section 3: Reagents & Instruments */}
            <AccordionItem
              title={
                <span className="accordion-title">
                  <InventoryManagement
                    size={16}
                    style={{ marginRight: "0.5rem" }}
                  />
                  <FormattedMessage
                    id="notebook.immunology.assay.section.reagentInstrument"
                    defaultMessage="C. Reagent & Instrument Selection"
                  />
                </span>
              }
            >
              <Grid narrow>
                <Column lg={8} md={4} sm={4}>
                  <MultiSelect
                    id="selectedReagents"
                    titleText={intl.formatMessage({
                      id: "notebook.immunology.reagents",
                      defaultMessage: "Reagents Used",
                    })}
                    label={intl.formatMessage({
                      id: "notebook.immunology.reagents.placeholder",
                      defaultMessage: "Select reagents...",
                    })}
                    items={reagents}
                    itemToString={(item) => (item ? item.label : "")}
                    selectedItems={reagents.filter((r) =>
                      assayValues.selectedReagents.includes(r.id),
                    )}
                    onChange={({ selectedItems }) =>
                      setAssayValues((prev) => ({
                        ...prev,
                        selectedReagents: selectedItems.map((r) => r.id),
                      }))
                    }
                    disabled={loadingReagents}
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <MultiSelect
                    id="selectedEquipment"
                    titleText={intl.formatMessage({
                      id: "notebook.immunology.equipment",
                      defaultMessage: "Instruments / Equipment",
                    })}
                    label={intl.formatMessage({
                      id: "notebook.immunology.equipment.placeholder",
                      defaultMessage: "Select instruments...",
                    })}
                    items={instruments}
                    itemToString={(item) => (item ? item.label : "")}
                    selectedItems={instruments.filter((i) =>
                      assayValues.selectedEquipment.includes(i.id),
                    )}
                    onChange={({ selectedItems }) =>
                      setAssayValues((prev) => ({
                        ...prev,
                        selectedEquipment: selectedItems.map((i) => i.id),
                      }))
                    }
                    disabled={loadingInstruments}
                  />
                </Column>
              </Grid>
            </AccordionItem>

            {/* Section 4: Results */}
            <AccordionItem
              title={
                <span className="accordion-title">
                  <Chemistry size={16} style={{ marginRight: "0.5rem" }} />
                  <FormattedMessage
                    id="notebook.immunology.assay.section.results"
                    defaultMessage="D. Results Obtained"
                  />
                </span>
              }
            >
              <Grid narrow>
                <Column lg={16} md={8} sm={4}>
                  <TextArea
                    id="resultsSummary"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.assay.resultsSummary",
                      defaultMessage: "Results Summary",
                    })}
                    value={assayValues.resultsSummary}
                    onChange={(e) =>
                      setAssayValues((prev) => ({
                        ...prev,
                        resultsSummary: e.target.value,
                      }))
                    }
                    placeholder={intl.formatMessage({
                      id: "notebook.immunology.assay.resultsSummary.placeholder",
                      defaultMessage:
                        "Describe the results obtained from the assay...",
                    })}
                    rows={3}
                  />
                </Column>
                <Column lg={6} md={4} sm={4}>
                  <TextInput
                    id="resultsQuantitative"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.assay.resultsQuantitative",
                      defaultMessage: "Quantitative Result",
                    })}
                    value={assayValues.resultsQuantitative}
                    onChange={(e) =>
                      setAssayValues((prev) => ({
                        ...prev,
                        resultsQuantitative: e.target.value,
                      }))
                    }
                    placeholder={intl.formatMessage({
                      id: "notebook.immunology.assay.resultsQuantitative.placeholder",
                      defaultMessage: "e.g., 95.5",
                    })}
                  />
                </Column>
                <Column lg={4} md={2} sm={2}>
                  <TextInput
                    id="resultsUnit"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.assay.resultsUnit",
                      defaultMessage: "Unit",
                    })}
                    value={assayValues.resultsUnit}
                    onChange={(e) =>
                      setAssayValues((prev) => ({
                        ...prev,
                        resultsUnit: e.target.value,
                      }))
                    }
                    placeholder={intl.formatMessage({
                      id: "notebook.immunology.assay.resultsUnit.placeholder",
                      defaultMessage: "e.g., %",
                    })}
                  />
                </Column>
              </Grid>
            </AccordionItem>

            {/* Section 5: Pass/Fail Status */}
            <AccordionItem
              title={
                <span className="accordion-title">
                  <Checkmark size={16} style={{ marginRight: "0.5rem" }} />
                  <FormattedMessage
                    id="notebook.immunology.assay.section.passFail"
                    defaultMessage="E. Pass/Fail Determination"
                  />
                </span>
              }
            >
              <Grid narrow>
                <Column lg={8} md={4} sm={4}>
                  <RadioButtonGroup
                    legendText={intl.formatMessage({
                      id: "notebook.immunology.assay.result",
                      defaultMessage: "Assay Result",
                    })}
                    name="assayResult"
                    valueSelected={assayValues.assayResult}
                    onChange={(value) =>
                      setAssayValues((prev) => ({
                        ...prev,
                        assayResult: value,
                        failAction: value === "Pass" ? "" : prev.failAction,
                        failureReason:
                          value === "Pass" ? "" : prev.failureReason,
                      }))
                    }
                    orientation="horizontal"
                  >
                    <RadioButton
                      id="result-pass"
                      value="Pass"
                      labelText={intl.formatMessage({
                        id: "notebook.immunology.assay.result.pass",
                        defaultMessage: "Pass",
                      })}
                    />
                    <RadioButton
                      id="result-fail"
                      value="Fail"
                      labelText={intl.formatMessage({
                        id: "notebook.immunology.assay.result.fail",
                        defaultMessage: "Fail",
                      })}
                    />
                  </RadioButtonGroup>
                </Column>
              </Grid>

              {assayValues.assayResult === "Fail" && (
                <Grid narrow style={{ marginTop: "1rem" }}>
                  <Column lg={8} md={4} sm={4}>
                    <Select
                      id="failAction"
                      labelText={intl.formatMessage({
                        id: "notebook.immunology.assay.failAction",
                        defaultMessage: "Action to Take",
                      })}
                      value={assayValues.failAction}
                      onChange={(e) =>
                        setAssayValues((prev) => ({
                          ...prev,
                          failAction: e.target.value,
                        }))
                      }
                    >
                      <SelectItem value="" text="Select action..." />
                      <SelectItem
                        value="reject"
                        text={intl.formatMessage({
                          id: "notebook.immunology.failAction.reject",
                          defaultMessage: "Reject",
                        })}
                      />
                      <SelectItem
                        value="continue_with_caution"
                        text={intl.formatMessage({
                          id: "notebook.immunology.failAction.continueWithCaution",
                          defaultMessage: "Continue with Caution",
                        })}
                      />
                    </Select>
                  </Column>
                  <Column lg={16} md={8} sm={4}>
                    <TextArea
                      id="failureReason"
                      labelText={intl.formatMessage({
                        id: "notebook.immunology.assay.failureReason",
                        defaultMessage: "Failure Reason",
                      })}
                      value={assayValues.failureReason}
                      onChange={(e) =>
                        setAssayValues((prev) => ({
                          ...prev,
                          failureReason: e.target.value,
                        }))
                      }
                      placeholder={intl.formatMessage({
                        id: "notebook.immunology.assay.failureReason.placeholder",
                        defaultMessage:
                          "Describe why the assay failed or did not meet criteria...",
                      })}
                      rows={2}
                    />
                  </Column>
                </Grid>
              )}
            </AccordionItem>

            {/* Section 6: Deviations from Protocol */}
            <AccordionItem
              title={
                <span className="accordion-title">
                  <Warning size={16} style={{ marginRight: "0.5rem" }} />
                  <FormattedMessage
                    id="notebook.immunology.assay.section.deviations"
                    defaultMessage="F. Protocol Deviations"
                  />
                </span>
              }
            >
              <Grid narrow>
                <Column lg={16} md={8} sm={4}>
                  <Checkbox
                    id="deviationObserved"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.assay.deviationObserved",
                      defaultMessage: "Deviation from protocol observed",
                    })}
                    checked={assayValues.deviationObserved}
                    onChange={(_, { checked }) =>
                      setAssayValues((prev) => ({
                        ...prev,
                        deviationObserved: checked,
                      }))
                    }
                  />
                </Column>

                {assayValues.deviationObserved && (
                  <>
                    <Column lg={16} md={8} sm={4}>
                      <TextArea
                        id="deviationDescription"
                        labelText={intl.formatMessage({
                          id: "notebook.immunology.assay.deviationDescription",
                          defaultMessage: "Deviation Description",
                        })}
                        value={assayValues.deviationDescription}
                        onChange={(e) =>
                          setAssayValues((prev) => ({
                            ...prev,
                            deviationDescription: e.target.value,
                          }))
                        }
                        placeholder={intl.formatMessage({
                          id: "notebook.immunology.assay.deviationDescription.placeholder",
                          defaultMessage:
                            "Describe what deviated from the standard protocol...",
                        })}
                        rows={2}
                      />
                    </Column>
                    <Column lg={8} md={4} sm={4}>
                      <TextArea
                        id="deviationImpact"
                        labelText={intl.formatMessage({
                          id: "notebook.immunology.assay.deviationImpact",
                          defaultMessage: "Potential Impact",
                        })}
                        value={assayValues.deviationImpact}
                        onChange={(e) =>
                          setAssayValues((prev) => ({
                            ...prev,
                            deviationImpact: e.target.value,
                          }))
                        }
                        placeholder={intl.formatMessage({
                          id: "notebook.immunology.assay.deviationImpact.placeholder",
                          defaultMessage:
                            "Describe the potential impact of this deviation...",
                        })}
                        rows={2}
                      />
                    </Column>
                    <Column lg={8} md={4} sm={4}>
                      <TextArea
                        id="correctiveAction"
                        labelText={intl.formatMessage({
                          id: "notebook.immunology.assay.correctiveAction",
                          defaultMessage: "Corrective Action Taken",
                        })}
                        value={assayValues.correctiveAction}
                        onChange={(e) =>
                          setAssayValues((prev) => ({
                            ...prev,
                            correctiveAction: e.target.value,
                          }))
                        }
                        placeholder={intl.formatMessage({
                          id: "notebook.immunology.assay.correctiveAction.placeholder",
                          defaultMessage:
                            "Describe any corrective action taken...",
                        })}
                        rows={2}
                      />
                    </Column>
                  </>
                )}
              </Grid>
            </AccordionItem>

            {/* Section 7: General Notes */}
            <AccordionItem
              title={
                <span className="accordion-title">
                  <DocumentAdd size={16} style={{ marginRight: "0.5rem" }} />
                  <FormattedMessage
                    id="notebook.immunology.assay.section.notes"
                    defaultMessage="G. General Notes"
                  />
                </span>
              }
            >
              <Grid narrow>
                <Column lg={16} md={8} sm={4}>
                  <TextArea
                    id="generalNotes"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.assay.generalNotes",
                      defaultMessage: "Additional Notes",
                    })}
                    value={assayValues.generalNotes}
                    onChange={(e) =>
                      setAssayValues((prev) => ({
                        ...prev,
                        generalNotes: e.target.value,
                      }))
                    }
                    placeholder={intl.formatMessage({
                      id: "notebook.immunology.assay.generalNotes.placeholder",
                      defaultMessage:
                        "Any additional observations, comments, or notes...",
                    })}
                    rows={3}
                  />
                </Column>
              </Grid>
            </AccordionItem>
          </Accordion>
        </div>
      </Modal>
    </div>
  );
}

export default ImmunologyAdditionalAssaysPage;
