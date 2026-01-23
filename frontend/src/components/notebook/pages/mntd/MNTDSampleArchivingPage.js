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
  Loading,
  Modal,
  TextInput,
  TextArea,
  NumberInput,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableSelectRow,
  TableSelectAll,
  Tag,
  RadioButtonGroup,
  RadioButton,
  DatePicker,
  DatePickerInput,
  Dropdown,
} from "@carbon/react";
import {
  Archive,
  TrashCan,
  Box,
  Renew,
  Calendar,
  Location,
  Automatic,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import StorageHierarchySelector from "../../workflow/StorageHierarchySelector";
import BoxLayoutViewer from "../../workflow/BoxLayoutViewer";
import "../../workflow/NotebookWorkflow.css";

/**
 * MNTDSampleArchivingPage - Page 9 of the MNTD workflow.
 * Handles final sample archiving: either Retention (with storage assignment) or Disposal.
 * This is the end of the sample lifecycle - no manipulation after this page.
 *
 * Purpose: Archive samples after test execution.
 *
 * Who uses it:
 * - Technician
 * - Lab Manager
 *
 * Sample Outcomes:
 * 1. Retention: Sample is assigned to storage with a retention period
 * 2. Disposal: Sample is marked as disposed (not stored)
 *
 * Data Points:
 * - Archive Type: RETENTION or DISPOSAL
 * - For RETENTION: Storage location, Retention period (months), Retention end date
 * - For DISPOSAL: Disposal method, Disposal date, Disposal reason
 * - Archived by, Archive date, Notes
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 * @param {number} props.notebookId - The notebook ID
 */
function MNTDSampleArchivingPage({ entryId, pageData, onProgressUpdate }) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State for samples
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Archive modal state
  const [archiveModalOpen, setArchiveModalOpen] = useState(false);
  const [archiveType, setArchiveType] = useState("RETENTION");
  const [isArchiving, setIsArchiving] = useState(false);

  // Retention-specific state
  const [retentionData, setRetentionData] = useState({
    retentionPeriodMonths: 12,
    retentionEndDate: "",
    storageNotes: "",
  });

  // Storage hierarchy state (for retention)
  const [storageSelection, setStorageSelection] = useState({
    room: null,
    device: null,
    shelf: null,
    rack: null,
    box: null,
  });
  const [boxLayout, setBoxLayout] = useState({});
  const [wellAssignments, setWellAssignments] = useState({});

  // Disposal-specific state
  const [disposalData, setDisposalData] = useState({
    disposalMethod: "AUTOCLAVE",
    disposalDate: new Date().toISOString().split("T")[0],
    disposalReason: "",
    disposalNotes: "",
  });

  // Disposal method options
  const disposalMethodOptions = [
    { id: "AUTOCLAVE", text: "Autoclave" },
    { id: "INCINERATION", text: "Incineration" },
    { id: "CHEMICAL_TREATMENT", text: "Chemical Treatment" },
    { id: "BIOHAZARD_WASTE", text: "Biohazard Waste Container" },
    { id: "OTHER", text: "Other" },
  ];

  // Load samples for this page
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
              // Archive data
              archiveType: sample.data?.archiveType,
              archiveDate: sample.data?.archiveDate,
              archivedBy: sample.data?.archivedBy,
              // Retention data
              retentionPeriodMonths: sample.data?.retentionPeriodMonths,
              retentionEndDate: sample.data?.retentionEndDate,
              storageLocation: sample.data?.storageLocation,
              storageWell: sample.data?.archiveStorageWell,
              // Disposal data
              disposalMethod: sample.data?.disposalMethod,
              disposalDate: sample.data?.disposalDate,
              disposalReason: sample.data?.disposalReason,
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

  // Load box occupancy from storage API
  const loadBoxOccupancy = useCallback((boxId) => {
    if (!boxId) return;

    getFromOpenElisServer(
      `/rest/storage/boxes/${boxId}/occupancy`,
      (response) => {
        if (componentMounted.current && response) {
          const occupiedCoordinates = response.occupiedCoordinates || {};
          setBoxLayout(occupiedCoordinates);
        }
      },
    );
  }, []);

  // Handle storage hierarchy selection change
  const handleStorageSelectionChange = useCallback(
    (selection) => {
      setStorageSelection(selection);
      setWellAssignments({});
      if (selection.box?.id) {
        loadBoxOccupancy(selection.box.id);
      } else {
        setBoxLayout({});
      }
    },
    [loadBoxOccupancy],
  );

  // Handle well click from BoxLayoutViewer
  const handleWellClick = useCallback(
    (wellCoord, wellInfo) => {
      if (wellInfo && !wellInfo.pending) {
        setError(
          intl.formatMessage(
            {
              id: "notebook.mntd.archiving.wellOccupied",
              defaultMessage:
                "Well {well} is already occupied. Choose another position.",
            },
            { well: wellCoord },
          ),
        );
        return;
      }

      // Assign to next unassigned sample
      const unassignedSamples = selectedSampleIds.filter(
        (id) => !wellAssignments[id],
      );
      if (unassignedSamples.length > 0) {
        setWellAssignments((prev) => ({
          ...prev,
          [unassignedSamples[0]]: wellCoord,
        }));
      }
    },
    [selectedSampleIds, wellAssignments, intl],
  );

  // Auto-populate wells with selected samples (same as Temporary Storage)
  const handleAutoPopulate = useCallback(() => {
    if (!storageSelection.box) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.archiving.selectBoxFirst",
          defaultMessage: "Please select a storage box first.",
        }),
      );
      return;
    }

    const rows = storageSelection.box.rows || 8;
    const columns = storageSelection.box.columns || 12;
    const rowLetters = Array.from({ length: rows }, (_, i) =>
      String.fromCharCode("A".charCodeAt(0) + i),
    );

    const newAssignments = {};
    let sampleIndex = 0;

    for (let row of rowLetters) {
      for (let col = 1; col <= columns; col++) {
        if (sampleIndex >= selectedSampleIds.length) break;

        const wellCoord = `${row}${col}`;
        if (!boxLayout[wellCoord]) {
          newAssignments[selectedSampleIds[sampleIndex]] = wellCoord;
          sampleIndex++;
        }
      }
      if (sampleIndex >= selectedSampleIds.length) break;
    }

    setWellAssignments(newAssignments);

    if (sampleIndex < selectedSampleIds.length) {
      setError(
        intl.formatMessage(
          {
            id: "notebook.mntd.archiving.notEnoughWells",
            defaultMessage:
              "Not enough empty wells. {assigned} of {total} samples assigned.",
          },
          { assigned: sampleIndex, total: selectedSampleIds.length },
        ),
      );
    } else {
      setError(null);
      setSuccessMessage(
        intl.formatMessage(
          {
            id: "notebook.mntd.archiving.autoPopulateSuccess",
            defaultMessage: "Auto-assigned {count} samples to wells.",
          },
          { count: sampleIndex },
        ),
      );
    }
  }, [storageSelection.box, selectedSampleIds, boxLayout, intl]);

  // Build combined layout for visualization (existing + pending assignments)
  const getCombinedLayout = useCallback(() => {
    const combined = { ...boxLayout };

    Object.entries(wellAssignments).forEach(([sampleId, wellCoord]) => {
      if (!combined[wellCoord]) {
        const sample = samples.find((s) => s.id === sampleId);
        combined[wellCoord] = {
          sampleItemId: sampleId,
          externalId: sample?.externalId || sampleId,
          pending: true,
        };
      }
    });

    return combined;
  }, [boxLayout, wellAssignments, samples]);

  // Calculate retention end date based on months
  const calculateRetentionEndDate = useCallback((months) => {
    const date = new Date();
    date.setMonth(date.getMonth() + months);
    return date.toISOString().split("T")[0];
  }, []);

  // Handle retention period change
  const handleRetentionPeriodChange = useCallback(
    (months) => {
      setRetentionData((prev) => ({
        ...prev,
        retentionPeriodMonths: months,
        retentionEndDate: calculateRetentionEndDate(months),
      }));
    },
    [calculateRetentionEndDate],
  );

  // Check if page has a real database ID
  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Handle open archive modal
  const handleOpenArchiveModal = () => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.archiving.noSamplesSelected",
          defaultMessage: "Please select samples to archive.",
        }),
      );
      return;
    }

    // Reset modal state
    setArchiveType("RETENTION");
    setRetentionData({
      retentionPeriodMonths: 12,
      retentionEndDate: calculateRetentionEndDate(12),
      storageNotes: "",
    });
    setDisposalData({
      disposalMethod: "AUTOCLAVE",
      disposalDate: new Date().toISOString().split("T")[0],
      disposalReason: "",
      disposalNotes: "",
    });
    setStorageSelection({
      room: null,
      device: null,
      shelf: null,
      rack: null,
      box: null,
    });
    setWellAssignments({});
    setBoxLayout({});
    setArchiveModalOpen(true);
  };

  // Handle archive submission
  const handleArchiveSamples = useCallback(() => {
    if (!hasRealPageId) {
      setArchiveModalOpen(false);
      return;
    }

    // Validate based on archive type
    if (archiveType === "RETENTION") {
      if (!storageSelection.box?.id) {
        setError(
          intl.formatMessage({
            id: "notebook.mntd.archiving.selectStorage",
            defaultMessage:
              "Please select a storage location for retention samples.",
          }),
        );
        return;
      }

      // Check all samples have well assignments
      const unassignedCount = selectedSampleIds.filter(
        (id) => !wellAssignments[id],
      ).length;
      if (unassignedCount > 0) {
        setError(
          intl.formatMessage(
            {
              id: "notebook.mntd.archiving.assignAllWells",
              defaultMessage:
                "{count} sample(s) do not have well assignments. Click wells to assign.",
            },
            { count: unassignedCount },
          ),
        );
        return;
      }
    }

    setIsArchiving(true);
    setError(null);

    const numericIds = selectedSampleIds.map((id) => parseInt(id, 10));

    // Build storage path for retention
    let storagePath = "";
    if (archiveType === "RETENTION" && storageSelection.box) {
      const parts = [];
      if (storageSelection.room) parts.push(storageSelection.room.name);
      if (storageSelection.device) parts.push(storageSelection.device.name);
      if (storageSelection.shelf) parts.push(storageSelection.shelf.name);
      if (storageSelection.rack) parts.push(storageSelection.rack.name);
      if (storageSelection.box) parts.push(storageSelection.box.name);
      storagePath = parts.join(" > ");
    }

    // Build data to save
    const dataToSave = {
      archiveType: archiveType,
      archiveDate: new Date().toISOString(),
      archivedBy: localStorage.getItem("userName") || "System",
    };

    if (archiveType === "RETENTION") {
      dataToSave.retentionPeriodMonths = retentionData.retentionPeriodMonths;
      dataToSave.retentionEndDate = retentionData.retentionEndDate;
      dataToSave.storageLocation = storagePath;
      dataToSave.archiveStorageBoxId = storageSelection.box?.id;
      dataToSave.archiveStorageNotes = retentionData.storageNotes;
    } else {
      dataToSave.disposalMethod = disposalData.disposalMethod;
      dataToSave.disposalDate = disposalData.disposalDate;
      dataToSave.disposalReason = disposalData.disposalReason;
      dataToSave.disposalNotes = disposalData.disposalNotes;
    }

    // If retention with well assignments, include them
    if (
      archiveType === "RETENTION" &&
      Object.keys(wellAssignments).length > 0
    ) {
      // Convert well assignments to sample-specific data
      const sampleWellData = {};
      Object.entries(wellAssignments).forEach(([sampleId, wellCoord]) => {
        sampleWellData[sampleId] = wellCoord;
      });
      dataToSave.wellAssignments = sampleWellData;
    }

    // First, apply the archive data
    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: numericIds,
        data: dataToSave,
      }),
      (response) => {
        if (componentMounted.current) {
          if (response && !response.error) {
            // Now update status to COMPLETED (end of lifecycle)
            postToOpenElisServer(
              `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
              JSON.stringify({
                sampleIds: numericIds,
                status: "COMPLETED",
              }),
              (statusResponse) => {
                if (componentMounted.current) {
                  setIsArchiving(false);
                  if (
                    statusResponse === 200 ||
                    (statusResponse && !statusResponse.error)
                  ) {
                    const typeLabel =
                      archiveType === "RETENTION" ? "retained" : "disposed";
                    setSuccessMessage(
                      intl.formatMessage(
                        {
                          id: "notebook.mntd.archiving.success",
                          defaultMessage:
                            "{count} sample(s) successfully {type}.",
                        },
                        { count: selectedSampleIds.length, type: typeLabel },
                      ),
                    );
                    setArchiveModalOpen(false);
                    setSelectedSampleIds([]);
                    loadPageSamples();
                    if (onProgressUpdate) {
                      onProgressUpdate();
                    }
                  } else {
                    setError(
                      statusResponse?.error ||
                        "Failed to update sample status.",
                    );
                  }
                }
              },
            );

            // If retention, also create storage assignments using same endpoint as Temporary Storage
            if (
              archiveType === "RETENTION" &&
              storageSelection.box?.id &&
              Object.keys(wellAssignments).length > 0
            ) {
              // Build storage path
              const storagePath = [
                storageSelection.room?.label || storageSelection.room?.name,
                storageSelection.device?.label || storageSelection.device?.name,
                storageSelection.shelf?.label || storageSelection.shelf?.name,
                storageSelection.rack?.label || storageSelection.rack?.name,
                storageSelection.box?.label || storageSelection.box?.name,
              ]
                .filter(Boolean)
                .join(" > ");

              const assignData = {
                sampleIds: numericIds,
                boxId: storageSelection.box.id,
                wellAssignments: wellAssignments,
                reassign: false,
                data: {
                  storageRoom:
                    storageSelection.room?.label || storageSelection.room?.name,
                  storageFreezer:
                    storageSelection.device?.label ||
                    storageSelection.device?.name,
                  storageRack:
                    storageSelection.rack?.label || storageSelection.rack?.name,
                  storageBox:
                    storageSelection.box?.label || storageSelection.box?.name,
                  storagePath: storagePath,
                  storageType: "ARCHIVE",
                  retentionEndDate: retentionData.retentionEndDate,
                  assignedDateTime: new Date().toISOString(),
                },
              };

              postToOpenElisServerJsonResponse(
                `/rest/notebook/bulk/page/${pageData.id}/samples/storage`,
                JSON.stringify(assignData),
                (response) => {
                  if (
                    response &&
                    response.errors &&
                    response.errors.length > 0
                  ) {
                    console.warn(
                      "Storage assignment warnings:",
                      response.errors,
                    );
                  }
                },
              );
            }
          } else {
            setIsArchiving(false);
            setError(response?.error || "Failed to archive samples.");
          }
        }
      },
    );
  }, [
    hasRealPageId,
    archiveType,
    selectedSampleIds,
    storageSelection,
    wellAssignments,
    retentionData,
    disposalData,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // Calculate stats
  const stats = useMemo(() => {
    const retained = samples.filter(
      (s) => s.archiveType === "RETENTION",
    ).length;
    const disposed = samples.filter((s) => s.archiveType === "DISPOSAL").length;
    const pending = samples.filter((s) => !s.archiveType).length;
    const completed = samples.filter((s) => s.status === "COMPLETED").length;
    return {
      total: samples.length,
      retained,
      disposed,
      pending,
      completed,
    };
  }, [samples]);

  // Filter samples
  const filteredSamples = useMemo(() => {
    if (statusFilter === "ALL") return samples;
    if (statusFilter === "PENDING")
      return samples.filter((s) => !s.archiveType);
    if (statusFilter === "RETENTION")
      return samples.filter((s) => s.archiveType === "RETENTION");
    if (statusFilter === "DISPOSAL")
      return samples.filter((s) => s.archiveType === "DISPOSAL");
    return samples;
  }, [samples, statusFilter]);

  // Filter options
  const filterOptions = [
    { id: "ALL", text: "All Samples" },
    { id: "PENDING", text: "Pending Archive" },
    { id: "RETENTION", text: "Retained" },
    { id: "DISPOSAL", text: "Disposed" },
  ];

  // Render archive status tag
  const renderArchiveStatus = (sample) => {
    if (sample.archiveType === "RETENTION") {
      return (
        <Tag type="blue" size="sm">
          <Box size={12} style={{ marginRight: "4px" }} />
          <FormattedMessage
            id="notebook.mntd.archiving.retained"
            defaultMessage="Retained"
          />
        </Tag>
      );
    }
    if (sample.archiveType === "DISPOSAL") {
      return (
        <Tag type="red" size="sm">
          <TrashCan size={12} style={{ marginRight: "4px" }} />
          <FormattedMessage
            id="notebook.mntd.archiving.disposed"
            defaultMessage="Disposed"
          />
        </Tag>
      );
    }
    return (
      <Tag type="gray" size="sm">
        <FormattedMessage
          id="notebook.mntd.archiving.pendingArchive"
          defaultMessage="Pending"
        />
      </Tag>
    );
  };

  // Render archive details
  const renderArchiveDetails = (sample) => {
    if (sample.archiveType === "RETENTION") {
      return (
        <div style={{ fontSize: "12px" }}>
          {sample.storageLocation && (
            <div style={{ color: "#525252" }}>
              <Location size={12} style={{ marginRight: "4px" }} />
              {sample.storageLocation}
            </div>
          )}
          {sample.storageWell && (
            <div style={{ color: "#525252" }}>Well: {sample.storageWell}</div>
          )}
          {sample.retentionEndDate && (
            <div style={{ color: "#525252" }}>
              <Calendar size={12} style={{ marginRight: "4px" }} />
              Until: {sample.retentionEndDate}
            </div>
          )}
        </div>
      );
    }
    if (sample.archiveType === "DISPOSAL") {
      return (
        <div style={{ fontSize: "12px" }}>
          {sample.disposalMethod && (
            <div style={{ color: "#525252" }}>
              Method: {sample.disposalMethod}
            </div>
          )}
          {sample.disposalDate && (
            <div style={{ color: "#525252" }}>Date: {sample.disposalDate}</div>
          )}
          {sample.disposalReason && (
            <div
              style={{
                color: "#525252",
                fontStyle: "italic",
                maxWidth: "200px",
              }}
            >
              {sample.disposalReason.substring(0, 50)}
              {sample.disposalReason.length > 50 ? "..." : ""}
            </div>
          )}
        </div>
      );
    }
    return (
      <span style={{ color: "#8d8d8d", fontSize: "12px" }}>
        <FormattedMessage
          id="notebook.mntd.archiving.notArchived"
          defaultMessage="Not archived"
        />
      </span>
    );
  };

  if (loading) {
    return (
      <div className="mntd-sample-archiving-page">
        <Loading withOverlay={false} />
      </div>
    );
  }

  return (
    <div className="mntd-sample-archiving-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <Archive size={20} style={{ marginRight: "0.5rem" }} />
          <FormattedMessage
            id="notebook.page.mntd.archiving.title"
            defaultMessage="Sample Archiving"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.mntd.archiving.description"
            defaultMessage="Archive samples after test execution. Samples can be retained in storage with a retention period or disposed. This is the final step in the sample lifecycle."
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
                  id="notebook.mntd.archiving.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{stats.total}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <Box size={16} style={{ marginRight: "4px" }} />
                <FormattedMessage
                  id="notebook.mntd.archiving.retained"
                  defaultMessage="Retained"
                />
              </span>
              <span className="progress-value">{stats.retained}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <TrashCan size={16} style={{ marginRight: "4px" }} />
                <FormattedMessage
                  id="notebook.mntd.archiving.disposed"
                  defaultMessage="Disposed"
                />
              </span>
              <span className="progress-value">{stats.disposed}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.mntd.archiving.pending"
                  defaultMessage="Pending"
                />
              </span>
              <span className="progress-value">{stats.pending}</span>
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

      {successMessage && (
        <InlineNotification
          kind="success"
          title={successMessage}
          hideCloseButton={false}
          lowContrast
          onClose={() => setSuccessMessage(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Dropdown
          id="status-filter"
          titleText=""
          label="Filter"
          items={filterOptions}
          itemToString={(item) => (item ? item.text : "")}
          selectedItem={filterOptions.find((f) => f.id === statusFilter)}
          onChange={({ selectedItem }) =>
            setStatusFilter(selectedItem?.id || "ALL")
          }
          size="sm"
        />

        <Button
          kind="primary"
          size="sm"
          renderIcon={Archive}
          onClick={handleOpenArchiveModal}
          disabled={selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="notebook.mntd.archiving.archiveSelected"
            defaultMessage="Archive Selected ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="ghost"
          size="sm"
          renderIcon={Renew}
          onClick={loadPageSamples}
        >
          <FormattedMessage
            id="notebook.mntd.archiving.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      {/* Sample Table */}
      <div className="sample-grid-container">
        {filteredSamples.length > 0 ? (
          <DataTable
            rows={filteredSamples.map((sample) => ({
              id: sample.id,
              externalId: sample.externalId || "-",
              accessionNumber: sample.accessionNumber || "-",
              sampleType: sample.sampleType || "-",
              archiveStatus: sample.archiveType,
              archiveDetails: sample,
            }))}
            headers={[
              {
                key: "externalId",
                header: intl.formatMessage({
                  id: "notebook.mntd.archiving.column.externalId",
                  defaultMessage: "External ID",
                }),
              },
              {
                key: "accessionNumber",
                header: intl.formatMessage({
                  id: "notebook.mntd.archiving.column.accessionNumber",
                  defaultMessage: "Accession #",
                }),
              },
              {
                key: "sampleType",
                header: intl.formatMessage({
                  id: "notebook.mntd.archiving.column.sampleType",
                  defaultMessage: "Sample Type",
                }),
              },
              {
                key: "archiveStatus",
                header: intl.formatMessage({
                  id: "notebook.mntd.archiving.column.archiveStatus",
                  defaultMessage: "Archive Status",
                }),
              },
              {
                key: "archiveDetails",
                header: intl.formatMessage({
                  id: "notebook.mntd.archiving.column.details",
                  defaultMessage: "Details",
                }),
              },
            ]}
            size="sm"
          >
            {({
              rows,
              headers,
              getTableProps,
              getHeaderProps,
              getRowProps,
            }) => {
              const allRowIds = rows.map((row) => row.id);
              const allSelected =
                allRowIds.length > 0 &&
                allRowIds.every((id) => selectedSampleIds.includes(id));
              const someSelected =
                !allSelected &&
                allRowIds.some((id) => selectedSampleIds.includes(id));

              const handleSelectAll = () => {
                if (allSelected) {
                  setSelectedSampleIds((prev) =>
                    prev.filter((id) => !allRowIds.includes(id)),
                  );
                } else {
                  setSelectedSampleIds((prev) => {
                    const newSet = new Set(prev);
                    allRowIds.forEach((id) => newSet.add(id));
                    return Array.from(newSet);
                  });
                }
              };

              const handleRowSelect = (rowId) => {
                setSelectedSampleIds((prev) =>
                  prev.includes(rowId)
                    ? prev.filter((id) => id !== rowId)
                    : [...prev, rowId],
                );
              };

              return (
                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
                      <TableSelectAll
                        id="archiving-select-all"
                        name="archiving-select-all"
                        checked={allSelected}
                        indeterminate={someSelected}
                        onSelect={handleSelectAll}
                        ariaLabel={intl.formatMessage({
                          id: "notebook.mntd.archiving.selectAll",
                          defaultMessage: "Select all samples",
                        })}
                      />
                      {headers.map((header) => (
                        <TableHeader
                          key={header.key}
                          {...getHeaderProps({ header })}
                        >
                          {header.header}
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {rows.map((row) => {
                      const sample = filteredSamples.find(
                        (s) => s.id === row.id,
                      );
                      const isSelected = selectedSampleIds.includes(row.id);
                      const isArchived = sample?.archiveType;

                      return (
                        <TableRow
                          key={row.id}
                          {...getRowProps({ row })}
                          className={isArchived ? "archived-row" : ""}
                        >
                          <TableSelectRow
                            id={`archiving-select-row-${row.id}`}
                            name={`archiving-select-row-${row.id}`}
                            checked={isSelected}
                            onSelect={() => handleRowSelect(row.id)}
                            disabled={isArchived}
                            ariaLabel={intl.formatMessage(
                              {
                                id: "notebook.mntd.archiving.selectRow",
                                defaultMessage: "Select sample {id}",
                              },
                              { id: row.id },
                            )}
                          />
                          {row.cells.map((cell) => (
                            <TableCell key={cell.id}>
                              {cell.info.header === "archiveStatus"
                                ? renderArchiveStatus(sample)
                                : cell.info.header === "archiveDetails"
                                  ? renderArchiveDetails(sample)
                                  : cell.value}
                            </TableCell>
                          ))}
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              );
            }}
          </DataTable>
        ) : (
          <div className="empty-state">
            <FormattedMessage
              id="notebook.mntd.archiving.noSamples"
              defaultMessage="No samples available for archiving. Complete the test execution step first."
            />
          </div>
        )}
      </div>

      {/* Archive Modal */}
      <Modal
        open={archiveModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.mntd.archiving.modal.title",
          defaultMessage: "Archive Samples",
        })}
        primaryButtonText={
          isArchiving
            ? intl.formatMessage({
                id: "notebook.mntd.archiving.processing",
                defaultMessage: "Processing...",
              })
            : intl.formatMessage({
                id: "notebook.mntd.archiving.modal.archive",
                defaultMessage: "Archive",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setArchiveModalOpen(false)}
        onRequestSubmit={handleArchiveSamples}
        primaryButtonDisabled={isArchiving}
        size="lg"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage
              id="notebook.mntd.archiving.modal.description"
              defaultMessage="Archive {count} selected sample(s). Choose whether to retain in storage or dispose."
              values={{ count: selectedSampleIds.length }}
            />
          </p>

          {/* Archive Type Selection */}
          <RadioButtonGroup
            legendText={intl.formatMessage({
              id: "notebook.mntd.archiving.archiveType",
              defaultMessage: "Archive Type",
            })}
            name="archive-type"
            valueSelected={archiveType}
            onChange={(value) => setArchiveType(value)}
            style={{ marginBottom: "1.5rem" }}
          >
            <RadioButton
              id="archive-retention"
              labelText={intl.formatMessage({
                id: "notebook.mntd.archiving.retention",
                defaultMessage: "Retention (Store with retention period)",
              })}
              value="RETENTION"
            />
            <RadioButton
              id="archive-disposal"
              labelText={intl.formatMessage({
                id: "notebook.mntd.archiving.disposal",
                defaultMessage: "Disposal (Mark as disposed)",
              })}
              value="DISPOSAL"
            />
          </RadioButtonGroup>

          {/* Retention Options */}
          {archiveType === "RETENTION" && (
            <div
              style={{
                padding: "1rem",
                backgroundColor: "#e0f0ff",
                borderRadius: "4px",
                marginBottom: "1rem",
              }}
            >
              <h5 style={{ marginBottom: "1rem" }}>
                <Box size={16} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="notebook.mntd.archiving.retentionDetails"
                  defaultMessage="Retention Details"
                />
              </h5>

              <Grid fullWidth>
                <Column lg={8} md={4} sm={4}>
                  <NumberInput
                    id="retention-period"
                    label={intl.formatMessage({
                      id: "notebook.mntd.archiving.retentionPeriod",
                      defaultMessage: "Retention Period (months)",
                    })}
                    min={1}
                    max={120}
                    value={retentionData.retentionPeriodMonths}
                    onChange={(e, { value }) =>
                      handleRetentionPeriodChange(value)
                    }
                    style={{ marginBottom: "1rem" }}
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <TextInput
                    id="retention-end-date"
                    labelText={intl.formatMessage({
                      id: "notebook.mntd.archiving.retentionEndDate",
                      defaultMessage: "Retention End Date",
                    })}
                    value={retentionData.retentionEndDate}
                    readOnly
                    style={{ marginBottom: "1rem" }}
                  />
                </Column>
              </Grid>

              {/* Storage Location Selection */}
              <h6 style={{ marginBottom: "0.5rem", marginTop: "1rem" }}>
                <FormattedMessage
                  id="notebook.mntd.archiving.selectStorage"
                  defaultMessage="Select Storage Location"
                />
              </h6>

              <StorageHierarchySelector
                onSelectionChange={handleStorageSelectionChange}
                selectedBox={storageSelection.box}
                storageType="archival"
              />

              {/* Box Layout Viewer */}
              {storageSelection.box?.id && (
                <div style={{ marginTop: "1rem" }}>
                  <div
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                      marginBottom: "0.5rem",
                    }}
                  >
                    <h6>
                      <FormattedMessage
                        id="notebook.mntd.archiving.assignWells"
                        defaultMessage="Click wells to assign samples ({assigned}/{total})"
                        values={{
                          assigned: Object.keys(wellAssignments).length,
                          total: selectedSampleIds.length,
                        }}
                      />
                    </h6>
                    <Button
                      kind="tertiary"
                      size="sm"
                      renderIcon={Automatic}
                      onClick={handleAutoPopulate}
                      disabled={selectedSampleIds.length === 0}
                    >
                      <FormattedMessage
                        id="notebook.mntd.storage.autoPopulate"
                        defaultMessage="Auto-Populate"
                      />
                    </Button>
                  </div>
                  <BoxLayoutViewer
                    boxId={storageSelection.box.id}
                    layout={getCombinedLayout()}
                    rows={storageSelection.box.rows || 8}
                    columns={storageSelection.box.columns || 12}
                    onWellClick={handleWellClick}
                  />
                </div>
              )}

              <TextArea
                id="storage-notes"
                labelText={intl.formatMessage({
                  id: "notebook.mntd.archiving.storageNotes",
                  defaultMessage: "Storage Notes",
                })}
                value={retentionData.storageNotes}
                onChange={(e) =>
                  setRetentionData((prev) => ({
                    ...prev,
                    storageNotes: e.target.value,
                  }))
                }
                rows={2}
                style={{ marginTop: "1rem" }}
              />
            </div>
          )}

          {/* Disposal Options */}
          {archiveType === "DISPOSAL" && (
            <div
              style={{
                padding: "1rem",
                backgroundColor: "#fff0f0",
                borderRadius: "4px",
                marginBottom: "1rem",
              }}
            >
              <h5 style={{ marginBottom: "1rem" }}>
                <TrashCan size={16} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="notebook.mntd.archiving.disposalDetails"
                  defaultMessage="Disposal Details"
                />
              </h5>

              <Grid fullWidth>
                <Column lg={8} md={4} sm={4}>
                  <Dropdown
                    id="disposal-method"
                    titleText={intl.formatMessage({
                      id: "notebook.mntd.archiving.disposalMethod",
                      defaultMessage: "Disposal Method",
                    })}
                    label="Select method..."
                    items={disposalMethodOptions}
                    itemToString={(item) => (item ? item.text : "")}
                    selectedItem={disposalMethodOptions.find(
                      (m) => m.id === disposalData.disposalMethod,
                    )}
                    onChange={({ selectedItem }) =>
                      setDisposalData((prev) => ({
                        ...prev,
                        disposalMethod: selectedItem?.id || "",
                      }))
                    }
                    style={{ marginBottom: "1rem" }}
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <DatePicker
                    datePickerType="single"
                    onChange={([date]) =>
                      setDisposalData((prev) => ({
                        ...prev,
                        disposalDate: date?.toISOString().split("T")[0] || "",
                      }))
                    }
                  >
                    <DatePickerInput
                      id="disposal-date"
                      labelText={intl.formatMessage({
                        id: "notebook.mntd.archiving.disposalDate",
                        defaultMessage: "Disposal Date",
                      })}
                      placeholder="mm/dd/yyyy"
                    />
                  </DatePicker>
                </Column>
                <Column lg={16} md={8} sm={4}>
                  <TextArea
                    id="disposal-reason"
                    labelText={intl.formatMessage({
                      id: "notebook.mntd.archiving.disposalReason",
                      defaultMessage: "Reason for Disposal",
                    })}
                    value={disposalData.disposalReason}
                    onChange={(e) =>
                      setDisposalData((prev) => ({
                        ...prev,
                        disposalReason: e.target.value,
                      }))
                    }
                    rows={2}
                    placeholder="e.g., Testing complete, sample no longer needed..."
                    style={{ marginTop: "1rem" }}
                  />
                </Column>
                <Column lg={16} md={8} sm={4}>
                  <TextArea
                    id="disposal-notes"
                    labelText={intl.formatMessage({
                      id: "notebook.mntd.archiving.disposalNotes",
                      defaultMessage: "Additional Notes",
                    })}
                    value={disposalData.disposalNotes}
                    onChange={(e) =>
                      setDisposalData((prev) => ({
                        ...prev,
                        disposalNotes: e.target.value,
                      }))
                    }
                    rows={2}
                    style={{ marginTop: "1rem" }}
                  />
                </Column>
              </Grid>
            </div>
          )}
        </div>
      </Modal>
    </div>
  );
}

export default MNTDSampleArchivingPage;
