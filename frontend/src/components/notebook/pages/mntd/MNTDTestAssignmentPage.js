import React, {
  useState,
  useEffect,
  useRef,
  useCallback,
  useMemo,
} from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  TextInput,
  TextArea,
  Dropdown,
  DatePicker,
  DatePickerInput,
  Modal,
  Tag,
} from "@carbon/react";
import {
  Add,
  CheckmarkFilled,
  Renew,
  Chemistry,
  Calendar,
  User,
  Settings,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * MNTDTestAssignmentPage - Page 7 of the MNTD workflow.
 * Handles test assignment and machine scheduling.
 *
 * Purpose: Link samples to experiments and machines.
 *
 * Who uses it:
 * - Lab manager
 * - Technician
 *
 * Data Points:
 * - Test Assignment: Experiment category, Specific assay/protocol
 * - Machine Scheduling: Instrument selected, Date & time slot, Operator name
 *
 * System Actions:
 * - Send test order to machine (if integrated)
 * - Block machine calendar
 * - Log user activity
 *
 * Leads to: Test Execution & Raw Data Page
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 * @param {number} props.notebookId - The notebook ID
 */
function MNTDTestAssignmentPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  notebookId,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State for samples
  const [samples, setSamples] = useState([]);
  const [selectedIds, setSelectedIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Test assignment modal state
  const [showAssignmentModal, setShowAssignmentModal] = useState(false);
  const [assignmentData, setAssignmentData] = useState({
    experimentCategory: "",
    specificAssay: "",
    notes: "",
  });

  // Machine scheduling modal state
  const [showSchedulingModal, setShowSchedulingModal] = useState(false);
  const [schedulingData, setSchedulingData] = useState({
    instrument: "",
    scheduledDate: new Date().toISOString().split("T")[0],
    scheduledTime: "",
    operatorName: "",
    schedulingNotes: "",
  });

  // Experiment category options
  const experimentCategoryOptions = [
    {
      id: "MOLECULAR_QPCR",
      text: intl.formatMessage({
        id: "notebook.mntd.testassignment.category.molecularQpcr",
        defaultMessage: "Molecular (qPCR)",
      }),
    },
    {
      id: "MOLECULAR_PCR",
      text: intl.formatMessage({
        id: "notebook.mntd.testassignment.category.molecularPcr",
        defaultMessage: "Molecular (PCR)",
      }),
    },
    {
      id: "GENOMICS",
      text: intl.formatMessage({
        id: "notebook.mntd.testassignment.category.genomics",
        defaultMessage: "Genomics",
      }),
    },
    {
      id: "DIGITAL_PCR",
      text: intl.formatMessage({
        id: "notebook.mntd.testassignment.category.digitalPcr",
        defaultMessage: "Digital PCR",
      }),
    },
    {
      id: "SEROLOGY_ELISA",
      text: intl.formatMessage({
        id: "notebook.mntd.testassignment.category.serologyElisa",
        defaultMessage: "Serology (ELISA)",
      }),
    },
    {
      id: "SEROLOGY_MULTIPLEX",
      text: intl.formatMessage({
        id: "notebook.mntd.testassignment.category.serologyMultiplex",
        defaultMessage: "Serology (Multiplex)",
      }),
    },
    {
      id: "CULTURE",
      text: intl.formatMessage({
        id: "notebook.mntd.testassignment.category.culture",
        defaultMessage: "Culture",
      }),
    },
    {
      id: "FLOW_CYTOMETRY",
      text: intl.formatMessage({
        id: "notebook.mntd.testassignment.category.flowCytometry",
        defaultMessage: "Flow Cytometry",
      }),
    },
  ];

  // Instrument options (can be extended to load from backend)
  const instrumentOptions = [
    { id: "QPCR_1", text: "qPCR System 1" },
    { id: "QPCR_2", text: "qPCR System 2" },
    { id: "PCR_THERMAL_1", text: "PCR Thermal Cycler 1" },
    { id: "SEQUENCER_1", text: "Sequencer 1" },
    { id: "ELISA_READER_1", text: "ELISA Reader 1" },
    { id: "ELISA_WASHER_1", text: "ELISA Washer 1" },
    { id: "FLOW_CYTOMETER_1", text: "Flow Cytometer 1" },
    { id: "DIGITAL_PCR_1", text: "Digital PCR System 1" },
    { id: "INCUBATOR_1", text: "Incubator 1" },
    { id: "CENTRIFUGE_1", text: "Centrifuge 1" },
  ];

  // Load samples for this page - only QC Passed samples from page 6
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id]);

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id) {
      setLoading(false);
      return;
    }

    // Skip loading for synthetic page IDs
    if (String(pageData.id).startsWith("default-")) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

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
              status: sample.pageStatus || "PENDING",
              patientName: sample.patientName,
              // Previous page data (QC from page 6)
              qcResult: sample.data?.qcResult,
              protocol: sample.data?.protocol,
              // Test assignment data
              experimentCategory: sample.data?.experimentCategory,
              specificAssay: sample.data?.specificAssay,
              // Machine scheduling data
              instrument: sample.data?.instrument,
              scheduledDate: sample.data?.scheduledDate,
              scheduledTime: sample.data?.scheduledTime,
              operatorName: sample.data?.operatorName,
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

  // Check if page has a real database ID
  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Calculate stats
  const stats = useMemo(() => {
    const assigned = samples.filter((s) => s.experimentCategory).length;
    const scheduled = samples.filter((s) => s.instrument).length;
    const fullyReady = samples.filter(
      (s) => s.experimentCategory && s.instrument,
    ).length;
    const pending = samples.filter(
      (s) => !s.experimentCategory && !s.instrument,
    ).length;
    return { total: samples.length, assigned, scheduled, fullyReady, pending };
  }, [samples]);

  // Handle opening test assignment modal
  const handleOpenAssignmentModal = useCallback(() => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.testassignment.selectSamples",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    setShowAssignmentModal(true);
  }, [selectedIds, intl]);

  // Handle saving test assignment data
  const handleSaveAssignmentData = useCallback(() => {
    if (!assignmentData.experimentCategory) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.testassignment.categoryRequired",
          defaultMessage: "Experiment category is required.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setShowAssignmentModal(false);
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    const dataToSave = {
      experimentCategory: assignmentData.experimentCategory,
      specificAssay: assignmentData.specificAssay,
      assignmentNotes: assignmentData.notes,
      assignmentDate: new Date().toISOString().split("T")[0],
    };

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: numericIds,
        data: dataToSave,
      }),
      (response) => {
        if (componentMounted.current) {
          if (response && !response.error) {
            // Update status to IN_PROGRESS
            postToOpenElisServer(
              `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
              JSON.stringify({
                sampleIds: numericIds,
                status: "IN_PROGRESS",
              }),
              () => {
                setSuccess(
                  intl.formatMessage(
                    {
                      id: "notebook.mntd.testassignment.assignmentSaved",
                      defaultMessage:
                        "Test assignment saved for {count} samples.",
                    },
                    { count: selectedIds.length },
                  ),
                );
                setShowAssignmentModal(false);
                setSelectedIds([]);
                // Reset form
                setAssignmentData({
                  experimentCategory: "",
                  specificAssay: "",
                  notes: "",
                });
                loadPageSamples();
                if (onProgressUpdate) {
                  onProgressUpdate();
                }
              },
            );
          } else {
            setError(response?.error || "Failed to save test assignment.");
          }
        }
      },
    );
  }, [
    assignmentData,
    selectedIds,
    hasRealPageId,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // Handle opening machine scheduling modal
  const handleOpenSchedulingModal = useCallback(() => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.testassignment.selectSamples",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    setShowSchedulingModal(true);
  }, [selectedIds, intl]);

  // Handle saving machine scheduling data
  const handleSaveSchedulingData = useCallback(() => {
    if (!schedulingData.instrument) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.testassignment.instrumentRequired",
          defaultMessage: "Instrument selection is required.",
        }),
      );
      return;
    }

    if (!schedulingData.scheduledDate) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.testassignment.dateRequired",
          defaultMessage: "Scheduled date is required.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setShowSchedulingModal(false);
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    const dataToSave = {
      instrument: schedulingData.instrument,
      scheduledDate: schedulingData.scheduledDate,
      scheduledTime: schedulingData.scheduledTime,
      operatorName: schedulingData.operatorName,
      schedulingNotes: schedulingData.schedulingNotes,
    };

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: numericIds,
        data: dataToSave,
      }),
      (response) => {
        if (componentMounted.current) {
          if (response && !response.error) {
            setSuccess(
              intl.formatMessage(
                {
                  id: "notebook.mntd.testassignment.schedulingSaved",
                  defaultMessage:
                    "Machine scheduling saved for {count} samples.",
                },
                { count: selectedIds.length },
              ),
            );
            setShowSchedulingModal(false);
            setSelectedIds([]);
            // Reset form
            setSchedulingData({
              instrument: "",
              scheduledDate: new Date().toISOString().split("T")[0],
              scheduledTime: "",
              operatorName: "",
              schedulingNotes: "",
            });
            loadPageSamples();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            setError(response?.error || "Failed to save machine scheduling.");
          }
        }
      },
    );
  }, [
    schedulingData,
    selectedIds,
    hasRealPageId,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // Handle status change
  const handleStatusChange = useCallback(
    (sampleId, newStatus) => {
      if (!hasRealPageId) {
        setError(
          intl.formatMessage({
            id: "notebook.mntd.testassignment.pageNotInitialized",
            defaultMessage:
              "Cannot update status: Page not properly initialized.",
          }),
        );
        return;
      }

      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
        JSON.stringify({
          sampleIds: [parseInt(sampleId, 10)],
          status: newStatus,
        }),
        (status) => {
          if (status === 200) {
            loadPageSamples();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            setError("Failed to update sample status. Please try again.");
          }
        },
      );
    },
    [pageData?.id, hasRealPageId, loadPageSamples, onProgressUpdate, intl],
  );

  // Bulk mark as ready for testing (complete this page)
  const handleBulkMarkReady = useCallback(() => {
    if (selectedIds.length === 0) return;

    // Check that selected samples have both assignment and scheduling
    const selectedSamples = samples.filter((s) => selectedIds.includes(s.id));
    const incompleteCount = selectedSamples.filter(
      (s) => !s.experimentCategory || !s.instrument,
    ).length;

    if (incompleteCount > 0) {
      setError(
        intl.formatMessage(
          {
            id: "notebook.mntd.testassignment.incompleteWarning",
            defaultMessage:
              "{count} sample(s) are missing test assignment or machine scheduling. Please complete both before marking as ready.",
          },
          { count: incompleteCount },
        ),
      );
      return;
    }

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.testassignment.pageNotInitialized",
          defaultMessage:
            "Cannot update status: Page not properly initialized.",
        }),
      );
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: numericIds,
        status: "COMPLETED",
      }),
      (status) => {
        if (status === 200) {
          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.mntd.testassignment.markedReady",
                defaultMessage:
                  "Marked {count} samples as ready for test execution.",
              },
              { count: selectedIds.length },
            ),
          );
          loadPageSamples();
          setSelectedIds([]);
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError("Failed to update sample status. Please try again.");
        }
      },
    );
  }, [
    selectedIds,
    samples,
    pageData?.id,
    hasRealPageId,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // Get experiment category label
  const getCategoryLabel = (categoryId) => {
    const category = experimentCategoryOptions.find((c) => c.id === categoryId);
    return category ? category.text : categoryId;
  };

  // Get instrument label
  const getInstrumentLabel = (instrumentId) => {
    const instrument = instrumentOptions.find((i) => i.id === instrumentId);
    return instrument ? instrument.text : instrumentId;
  };

  // Render test assignment info column
  const renderAssignmentInfo = (sample) => {
    if (sample.experimentCategory) {
      return (
        <div style={{ fontSize: "12px" }}>
          <Tag type="blue" size="sm">
            {getCategoryLabel(sample.experimentCategory)}
          </Tag>
          {sample.specificAssay && (
            <div style={{ marginTop: "2px", color: "#525252" }}>
              {sample.specificAssay}
            </div>
          )}
        </div>
      );
    }
    return (
      <span style={{ color: "#8d8d8d", fontSize: "12px" }}>
        <FormattedMessage
          id="notebook.mntd.testassignment.notAssigned"
          defaultMessage="Not assigned"
        />
      </span>
    );
  };

  // Render machine scheduling info column
  const renderSchedulingInfo = (sample) => {
    if (sample.instrument) {
      return (
        <div style={{ fontSize: "12px" }}>
          <Tag type="teal" size="sm">
            <Settings size={12} style={{ marginRight: "4px" }} />
            {getInstrumentLabel(sample.instrument)}
          </Tag>
          {sample.scheduledDate && (
            <div style={{ marginTop: "2px", color: "#525252" }}>
              <Calendar size={12} style={{ marginRight: "4px" }} />
              {sample.scheduledDate}
              {sample.scheduledTime && ` ${sample.scheduledTime}`}
            </div>
          )}
          {sample.operatorName && (
            <div style={{ color: "#525252" }}>
              <User size={12} style={{ marginRight: "4px" }} />
              {sample.operatorName}
            </div>
          )}
        </div>
      );
    }
    return (
      <span style={{ color: "#8d8d8d", fontSize: "12px" }}>
        <FormattedMessage
          id="notebook.mntd.testassignment.notScheduled"
          defaultMessage="Not scheduled"
        />
      </span>
    );
  };

  return (
    <div className="mntd-test-assignment-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.mntd.testassignment.title"
            defaultMessage="Test Assignment & Machine Scheduling"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.mntd.testassignment.description"
            defaultMessage="Link samples to experiments and schedule machine time. Assign experiment category and specific assay, then schedule instrument and operator for test execution."
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
                  id="notebook.mntd.testassignment.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{stats.total}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.mntd.testassignment.testAssigned"
                  defaultMessage="Test Assigned"
                />
              </span>
              <span className="progress-value">{stats.assigned}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.mntd.testassignment.machineScheduled"
                  defaultMessage="Machine Scheduled"
                />
              </span>
              <span className="progress-value">{stats.scheduled}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.mntd.testassignment.fullyReady"
                  defaultMessage="Fully Ready"
                />
              </span>
              <span className="progress-value">{stats.fullyReady}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          hideCloseButton={false}
          lowContrast
          onClose={() => setError(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {success && (
        <InlineNotification
          kind="success"
          title={success}
          hideCloseButton={false}
          lowContrast
          onClose={() => setSuccess(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Chemistry}
          onClick={handleOpenAssignmentModal}
          disabled={selectedIds.length === 0}
        >
          <FormattedMessage
            id="notebook.mntd.testassignment.assignTest"
            defaultMessage="Assign Test ({count} selected)"
            values={{ count: selectedIds.length }}
          />
        </Button>

        <Button
          kind="secondary"
          size="sm"
          renderIcon={Calendar}
          onClick={handleOpenSchedulingModal}
          disabled={selectedIds.length === 0}
        >
          <FormattedMessage
            id="notebook.mntd.testassignment.scheduleMachine"
            defaultMessage="Schedule Machine ({count} selected)"
            values={{ count: selectedIds.length }}
          />
        </Button>

        {selectedIds.length > 0 && (
          <Button
            kind="tertiary"
            size="sm"
            renderIcon={CheckmarkFilled}
            onClick={handleBulkMarkReady}
          >
            <FormattedMessage
              id="notebook.mntd.testassignment.markReady"
              defaultMessage="Mark Ready for Testing ({count})"
              values={{ count: selectedIds.length }}
            />
          </Button>
        )}

        <Button
          kind="ghost"
          size="sm"
          renderIcon={Renew}
          onClick={loadPageSamples}
        >
          <FormattedMessage
            id="notebook.mntd.testassignment.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      {/* Sample Grid */}
      <div className="sample-grid-container">
        <SampleGrid
          gridId="mntd-test-assignment"
          samples={samples}
          selectedIds={selectedIds}
          onSelectionChange={setSelectedIds}
          onStatusChange={handleStatusChange}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          showSelection={true}
          loading={loading}
          additionalColumns={[
            {
              key: "assignmentInfo",
              header: intl.formatMessage({
                id: "notebook.mntd.testassignment.testAssignment",
                defaultMessage: "Test Assignment",
              }),
              render: renderAssignmentInfo,
            },
            {
              key: "schedulingInfo",
              header: intl.formatMessage({
                id: "notebook.mntd.testassignment.machineScheduling",
                defaultMessage: "Machine Scheduling",
              }),
              render: renderSchedulingInfo,
            },
          ]}
        />
      </div>

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.mntd.testassignment.empty"
              defaultMessage="No samples available for test assignment. Please complete the Processing & QC step first (samples must pass QC)."
            />
          </p>
        </div>
      )}

      {/* Test Assignment Modal */}
      <Modal
        open={showAssignmentModal}
        modalHeading={intl.formatMessage({
          id: "notebook.mntd.testassignment.modal.assignmentTitle",
          defaultMessage: "Assign Test",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.mntd.testassignment.modal.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setShowAssignmentModal(false)}
        onRequestSubmit={handleSaveAssignmentData}
        size="md"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage
              id="notebook.mntd.testassignment.modal.assignmentDescription"
              defaultMessage="Assign test experiment category and assay for {count} selected sample(s)."
              values={{ count: selectedIds.length }}
            />
          </p>

          {/* Experiment Category Selection */}
          <Dropdown
            id="experiment-category"
            titleText={intl.formatMessage({
              id: "notebook.mntd.testassignment.experimentCategory",
              defaultMessage: "Experiment Category",
            })}
            label={intl.formatMessage({
              id: "notebook.mntd.testassignment.selectCategory",
              defaultMessage: "Select category",
            })}
            items={experimentCategoryOptions}
            itemToString={(item) => (item ? item.text : "")}
            selectedItem={experimentCategoryOptions.find(
              (c) => c.id === assignmentData.experimentCategory,
            )}
            onChange={({ selectedItem }) =>
              setAssignmentData({
                ...assignmentData,
                experimentCategory: selectedItem?.id || "",
              })
            }
            style={{ marginBottom: "1rem" }}
          />

          {/* Specific Assay / Protocol */}
          <TextInput
            id="specific-assay"
            labelText={intl.formatMessage({
              id: "notebook.mntd.testassignment.specificAssay",
              defaultMessage: "Specific Assay / Protocol",
            })}
            value={assignmentData.specificAssay}
            onChange={(e) =>
              setAssignmentData({
                ...assignmentData,
                specificAssay: e.target.value,
              })
            }
            placeholder={intl.formatMessage({
              id: "notebook.mntd.testassignment.specificAssayPlaceholder",
              defaultMessage: "e.g., Malaria Pf/Pv qPCR, Leishmania ELISA",
            })}
            style={{ marginBottom: "1rem" }}
          />

          {/* Notes */}
          <TextArea
            id="assignment-notes"
            labelText={intl.formatMessage({
              id: "notebook.mntd.testassignment.notes",
              defaultMessage: "Notes",
            })}
            value={assignmentData.notes}
            onChange={(e) =>
              setAssignmentData({ ...assignmentData, notes: e.target.value })
            }
            rows={3}
          />
        </div>
      </Modal>

      {/* Machine Scheduling Modal */}
      <Modal
        open={showSchedulingModal}
        modalHeading={intl.formatMessage({
          id: "notebook.mntd.testassignment.modal.schedulingTitle",
          defaultMessage: "Schedule Machine",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.mntd.testassignment.modal.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setShowSchedulingModal(false)}
        onRequestSubmit={handleSaveSchedulingData}
        size="md"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage
              id="notebook.mntd.testassignment.modal.schedulingDescription"
              defaultMessage="Schedule machine time for {count} selected sample(s)."
              values={{ count: selectedIds.length }}
            />
          </p>

          {/* Instrument Selection */}
          <Dropdown
            id="instrument"
            titleText={intl.formatMessage({
              id: "notebook.mntd.testassignment.instrument",
              defaultMessage: "Instrument",
            })}
            label={intl.formatMessage({
              id: "notebook.mntd.testassignment.selectInstrument",
              defaultMessage: "Select instrument",
            })}
            items={instrumentOptions}
            itemToString={(item) => (item ? item.text : "")}
            selectedItem={instrumentOptions.find(
              (i) => i.id === schedulingData.instrument,
            )}
            onChange={({ selectedItem }) =>
              setSchedulingData({
                ...schedulingData,
                instrument: selectedItem?.id || "",
              })
            }
            style={{ marginBottom: "1rem" }}
          />

          {/* Scheduled Date & Time */}
          <div
            style={{
              padding: "1rem",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
              marginBottom: "1rem",
            }}
          >
            <h5 style={{ marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.mntd.testassignment.scheduledDateTime"
                defaultMessage="Scheduled Date & Time"
              />
            </h5>
            <div style={{ display: "flex", gap: "1rem" }}>
              <DatePicker
                datePickerType="single"
                onChange={([date]) =>
                  setSchedulingData({
                    ...schedulingData,
                    scheduledDate: date?.toISOString().split("T")[0] || "",
                  })
                }
              >
                <DatePickerInput
                  id="scheduled-date"
                  labelText={intl.formatMessage({
                    id: "notebook.mntd.testassignment.date",
                    defaultMessage: "Date",
                  })}
                  placeholder="mm/dd/yyyy"
                />
              </DatePicker>
              <TextInput
                id="scheduled-time"
                labelText={intl.formatMessage({
                  id: "notebook.mntd.testassignment.time",
                  defaultMessage: "Time Slot",
                })}
                value={schedulingData.scheduledTime}
                onChange={(e) =>
                  setSchedulingData({
                    ...schedulingData,
                    scheduledTime: e.target.value,
                  })
                }
                placeholder="HH:MM or time range"
              />
            </div>
          </div>

          {/* Operator Name */}
          <TextInput
            id="operator-name"
            labelText={intl.formatMessage({
              id: "notebook.mntd.testassignment.operatorName",
              defaultMessage: "Operator Name",
            })}
            value={schedulingData.operatorName}
            onChange={(e) =>
              setSchedulingData({
                ...schedulingData,
                operatorName: e.target.value,
              })
            }
            style={{ marginBottom: "1rem" }}
          />

          {/* Scheduling Notes */}
          <TextArea
            id="scheduling-notes"
            labelText={intl.formatMessage({
              id: "notebook.mntd.testassignment.schedulingNotes",
              defaultMessage: "Scheduling Notes",
            })}
            value={schedulingData.schedulingNotes}
            onChange={(e) =>
              setSchedulingData({
                ...schedulingData,
                schedulingNotes: e.target.value,
              })
            }
            rows={3}
          />
        </div>
      </Modal>
    </div>
  );
}

export default MNTDTestAssignmentPage;
