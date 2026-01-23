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
  Checkbox,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
} from "@carbon/react";
import {
  Report,
  Download,
  CheckmarkFilled,
  Renew,
  DocumentExport,
  Archive,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import StorageHierarchySelector from "../../workflow/StorageHierarchySelector";
import config from "../../../../config.json";
import "../../workflow/NotebookWorkflow.css";

/**
 * ImmunologyReportingREDCapPage - Page 10 of the Immunology workflow.
 * Handles final report generation and REDCap integration.
 *
 * Purpose: Finalize results and integrate with external systems.
 *
 * Who uses it:
 * - Lab manager
 * - Data manager
 *
 * Data Points:
 * - Report Generation: Report type, Date range, Samples included
 * - External Integration: REDCap project, Upload confirmation, Validation status
 *
 * System Actions:
 * - Final report generated and downloaded
 * - REDCap-compatible file generated and downloaded
 * - Results archived to storage hierarchy
 * - Full audit trail maintained
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 * @param {number} props.notebookId - The notebook ID
 */
function ImmunologyReportingREDCapPage({
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

  // Active tab state
  const [activeTab, setActiveTab] = useState(0);

  // Report generation modal state
  const [showReportModal, setShowReportModal] = useState(false);
  const [reportData, setReportData] = useState({
    reportType: "SUMMARY",
    dateRangeStart: "",
    dateRangeEnd: "",
    includeAllSamples: true,
    reportFormat: "CSV",
    recipientEmails: "",
    reportNotes: "",
  });
  const [isGeneratingReport, setIsGeneratingReport] = useState(false);

  // REDCap submission modal state
  const [showREDCapModal, setShowREDCapModal] = useState(false);
  const [redcapData, setRedcapData] = useState({
    projectId: "",
    recordIdField: "record_id",
    eventName: "",
    instrumentName: "",
  });
  const [isGeneratingREDCapFile, setIsGeneratingREDCapFile] = useState(false);

  // Archive modal state
  const [showArchiveModal, setShowArchiveModal] = useState(false);
  const [archiveData, setArchiveData] = useState({
    archiveNotes: "",
    createBackup: true,
  });
  const [isArchiving, setIsArchiving] = useState(false);

  // Storage hierarchy state for archiving (like Temporary Storage page)
  const [storageSelection, setStorageSelection] = useState({
    room: null,
    device: null,
    shelf: null,
    rack: null,
    box: null,
  });
  const [boxLayout, setBoxLayout] = useState({});
  const [selectedWell, setSelectedWell] = useState(null);
  const [useAutoAssign, setUseAutoAssign] = useState(true); // Default to auto-assign mode

  // History state
  const [reportHistory, setReportHistory] = useState([]);

  // Report type options
  const reportTypeOptions = [
    { id: "SUMMARY", text: "Summary Report" },
    { id: "DETAILED", text: "Detailed Results Report" },
    { id: "QC", text: "Quality Control Report" },
    { id: "STATISTICAL", text: "Statistical Analysis Report" },
    { id: "AUDIT", text: "Audit Trail Report" },
    { id: "CUSTOM", text: "Custom Report" },
  ];

  // Report format options - PDF and Excel not yet implemented on backend
  const reportFormatOptions = [
    { id: "CSV", text: "CSV" },
    { id: "JSON", text: "JSON" },
  ];

  // Load samples for this page
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    loadReportHistory();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id]);

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
      if (selection.box?.id) {
        loadBoxOccupancy(selection.box.id);
      } else {
        setBoxLayout({});
      }
    },
    [loadBoxOccupancy],
  );

  // Calculate preview wells for auto-assignment (like Temporary Storage page)
  const getAutoAssignPreview = useCallback(() => {
    if (!storageSelection.box || selectedIds.length === 0) {
      return {
        previewWells: [],
        availableCount: 0,
        totalWells: 0,
        occupiedCount: 0,
      };
    }

    const rows = storageSelection.box?.rows || 8;
    const columns = storageSelection.box?.columns || 12;
    const occupiedWells = new Set(Object.keys(boxLayout));

    // Generate all wells in order (A1, A2, ..., A12, B1, B2, ...)
    const allWells = [];
    for (let row = 0; row < rows; row++) {
      const rowLetter = String.fromCharCode("A".charCodeAt(0) + row);
      for (let col = 1; col <= columns; col++) {
        allWells.push(`${rowLetter}${col}`);
      }
    }

    // Find available wells
    const availableWells = allWells.filter((well) => !occupiedWells.has(well));

    // Get preview wells (wells that will be assigned)
    const previewWells = availableWells.slice(0, selectedIds.length);

    return {
      previewWells,
      availableCount: availableWells.length,
      totalWells: rows * columns,
      occupiedCount: occupiedWells.size,
    };
  }, [storageSelection.box, selectedIds, boxLayout]);

  const autoAssignPreview = getAutoAssignPreview();

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
              // Validation status from previous page
              validationStatus: sample.data?.validationStatus,
              // Reporting data
              reportGenerated: sample.data?.reportGenerated,
              reportGeneratedAt: sample.data?.reportGeneratedAt,
              reportType: sample.data?.reportType,
              reportFormat: sample.data?.reportFormat,
              reportFileId: sample.data?.reportFileId,
              // REDCap data
              redcapExported: sample.data?.redcapExported,
              redcapExportedAt: sample.data?.redcapExportedAt,
              redcapProjectId: sample.data?.redcapProjectId,
              // Archive data
              archived: sample.data?.archived,
              archivedAt: sample.data?.archivedAt,
              archiveLocation: sample.data?.archiveLocation,
              storagePath: sample.data?.storagePath,
              storageWell: sample.data?.storageWell,
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

  const loadReportHistory = useCallback(() => {
    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      return;
    }

    // Load report history from API
    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/reports`,
      (response) => {
        if (componentMounted.current && response && Array.isArray(response)) {
          setReportHistory(
            response.map((r, idx) => ({
              id: r.id || `report-${idx}`,
              date: r.generatedAt
                ? new Date(r.generatedAt).toLocaleString()
                : "-",
              reportType: r.reportType || "-",
              format: r.reportFormat || "-",
              sampleCount: r.sampleCount || 0,
              generatedBy: r.generatedBy || "-",
              fileId: r.fileId,
              fileName: r.fileName,
            })),
          );
        }
      },
    );
  }, [pageData?.id]);

  // Check if page has a real database ID
  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Calculate stats
  const stats = useMemo(() => {
    const reportGenerated = samples.filter((s) => s.reportGenerated).length;
    const redcapExported = samples.filter((s) => s.redcapExported).length;
    const archived = samples.filter((s) => s.archived).length;
    const pending = samples.filter(
      (s) => !s.reportGenerated && !s.redcapExported,
    ).length;
    const completed = samples.filter(
      (s) => s.reportGenerated && s.redcapExported && s.archived,
    ).length;
    return {
      total: samples.length,
      reportGenerated,
      redcapExported,
      archived,
      pending,
      completed,
    };
  }, [samples]);

  // Handle opening report generation modal
  const handleOpenReportModal = useCallback(() => {
    if (selectedIds.length === 0 && !reportData.includeAllSamples) {
      setError(
        intl.formatMessage({
          id: "notebook.immunology.reporting.selectSamples",
          defaultMessage:
            "Please select at least one sample or choose to include all samples.",
        }),
      );
      return;
    }
    setShowReportModal(true);
  }, [selectedIds, reportData.includeAllSamples, intl]);

  // Helper function to trigger file download
  const downloadFile = useCallback((blob, fileName) => {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
  }, []);

  // Handle generating report - downloads file and saves to history
  const handleGenerateReport = useCallback(() => {
    if (!reportData.reportType) {
      setError(
        intl.formatMessage({
          id: "notebook.immunology.reporting.reportTypeRequired",
          defaultMessage: "Please select a report type.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setShowReportModal(false);
      return;
    }

    setIsGeneratingReport(true);

    const targetIds = reportData.includeAllSamples
      ? samples.map((s) => parseInt(s.id, 10))
      : selectedIds.map((id) => parseInt(id, 10));

    const requestData = {
      sampleIds: targetIds,
      reportType: reportData.reportType,
      reportFormat: reportData.reportFormat,
      dateRangeStart: reportData.dateRangeStart,
      dateRangeEnd: reportData.dateRangeEnd,
      recipientEmails: reportData.recipientEmails
        .split(",")
        .map((e) => e.trim())
        .filter((e) => e),
      notes: reportData.reportNotes,
      saveToHistory: true,
    };

    // Use fetch to handle file download
    fetch(
      `${config.serverBaseUrl}/rest/notebook/bulk/page/${pageData.id}/generate-report`,
      {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
        body: JSON.stringify(requestData),
      },
    )
      .then((response) => {
        if (!response.ok) {
          return response.json().then((err) => {
            throw new Error(err.error || "Failed to generate report");
          });
        }
        // Get filename from Content-Disposition header if available
        const contentDisposition = response.headers.get("Content-Disposition");
        let fileName = `Immunology_Report_${reportData.reportType}_${new Date().toISOString().split("T")[0]}`;
        if (reportData.reportFormat === "PDF") fileName += ".pdf";
        else if (reportData.reportFormat === "EXCEL") fileName += ".xlsx";
        else if (reportData.reportFormat === "CSV") fileName += ".csv";
        else fileName += ".json";

        if (contentDisposition) {
          const match = contentDisposition.match(/filename="?([^"]+)"?/);
          if (match) fileName = match[1];
        }

        return response.blob().then((blob) => ({ blob, fileName }));
      })
      .then(({ blob, fileName }) => {
        if (componentMounted.current) {
          // Download the file
          downloadFile(blob, fileName);

          // Update sample data to mark report generated
          const dataToSave = {
            reportGenerated: true,
            reportGeneratedAt: new Date().toISOString(),
            reportType: reportData.reportType,
            reportFormat: reportData.reportFormat,
          };

          postToOpenElisServer(
            `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
            JSON.stringify({
              sampleIds: targetIds,
              data: dataToSave,
            }),
            () => {
              setIsGeneratingReport(false);
              setSuccess(
                intl.formatMessage(
                  {
                    id: "notebook.immunology.reporting.reportGeneratedDownloaded",
                    defaultMessage:
                      "Report generated and downloaded for {count} samples. Report saved to history.",
                  },
                  { count: targetIds.length },
                ),
              );
              setShowReportModal(false);
              setSelectedIds([]);
              setReportData({
                reportType: "SUMMARY",
                dateRangeStart: "",
                dateRangeEnd: "",
                includeAllSamples: true,
                reportFormat: "CSV",
                recipientEmails: "",
                reportNotes: "",
              });
              loadPageSamples();
              loadReportHistory();
              if (onProgressUpdate) {
                onProgressUpdate();
              }
            },
          );
        }
      })
      .catch((err) => {
        if (componentMounted.current) {
          setIsGeneratingReport(false);
          setError(err.message || "Failed to generate report.");
        }
      });
  }, [
    reportData,
    samples,
    selectedIds,
    hasRealPageId,
    pageData?.id,
    loadPageSamples,
    loadReportHistory,
    onProgressUpdate,
    intl,
    downloadFile,
  ]);

  // Handle downloading a report from history
  const handleDownloadReport = useCallback(
    (fileId, fileName) => {
      if (!fileId) {
        setError("Report file not available for download.");
        return;
      }

      fetch(`${config.serverBaseUrl}/rest/notebook/files/${fileId}/download`, {
        method: "GET",
        credentials: "include",
        headers: {
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
      })
        .then((response) => {
          if (!response.ok) {
            throw new Error("Failed to download report");
          }
          return response.blob();
        })
        .then((blob) => {
          downloadFile(blob, fileName || `report_${fileId}`);
        })
        .catch((err) => {
          setError(err.message || "Failed to download report.");
        });
    },
    [downloadFile],
  );

  // Handle opening REDCap modal
  const handleOpenREDCapModal = useCallback(() => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.immunology.reporting.selectSamplesREDCap",
          defaultMessage: "Please select at least one sample to export.",
        }),
      );
      return;
    }
    setShowREDCapModal(true);
  }, [selectedIds, intl]);

  // Handle REDCap file generation and download
  const handleGenerateREDCapFile = useCallback(() => {
    if (!hasRealPageId) {
      setShowREDCapModal(false);
      return;
    }

    setIsGeneratingREDCapFile(true);

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    const requestData = {
      sampleIds: numericIds,
      projectId: redcapData.projectId,
      recordIdField: redcapData.recordIdField,
      eventName: redcapData.eventName,
      instrumentName: redcapData.instrumentName,
    };

    // Use fetch to handle CSV file download
    fetch(
      `${config.serverBaseUrl}/rest/notebook/bulk/page/${pageData.id}/redcap/export`,
      {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
        body: JSON.stringify(requestData),
      },
    )
      .then((response) => {
        if (!response.ok) {
          return response.json().then((err) => {
            throw new Error(err.error || "Failed to generate REDCap file");
          });
        }
        const fileName = `REDCap_Immunology_Export_${redcapData.projectId || "data"}_${new Date().toISOString().split("T")[0]}.csv`;
        return response.blob().then((blob) => ({ blob, fileName }));
      })
      .then(({ blob, fileName }) => {
        if (componentMounted.current) {
          // Download the CSV file
          downloadFile(blob, fileName);

          // Update sample data to mark REDCap exported
          const dataToSave = {
            redcapExported: true,
            redcapExportedAt: new Date().toISOString(),
            redcapProjectId: redcapData.projectId,
          };

          postToOpenElisServer(
            `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
            JSON.stringify({
              sampleIds: numericIds,
              data: dataToSave,
            }),
            () => {
              setIsGeneratingREDCapFile(false);
              setSuccess(
                intl.formatMessage(
                  {
                    id: "notebook.immunology.reporting.redcapFileDownloaded",
                    defaultMessage:
                      "REDCap-compatible CSV file downloaded for {count} samples. You can now import this file into REDCap.",
                  },
                  { count: selectedIds.length },
                ),
              );
              setShowREDCapModal(false);
              setSelectedIds([]);
              setRedcapData({
                projectId: "",
                recordIdField: "record_id",
                eventName: "",
                instrumentName: "",
              });
              loadPageSamples();
              if (onProgressUpdate) {
                onProgressUpdate();
              }
            },
          );
        }
      })
      .catch((err) => {
        if (componentMounted.current) {
          setIsGeneratingREDCapFile(false);
          setError(err.message || "Failed to generate REDCap file.");
        }
      });
  }, [
    redcapData,
    selectedIds,
    hasRealPageId,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
    downloadFile,
  ]);

  // Handle opening archive modal
  const handleOpenArchiveModal = useCallback(() => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.immunology.reporting.selectSamplesArchive",
          defaultMessage: "Please select at least one sample to archive.",
        }),
      );
      return;
    }
    setShowArchiveModal(true);
    // Reset storage selection
    setStorageSelection({
      room: null,
      device: null,
      shelf: null,
      rack: null,
      box: null,
    });
    setBoxLayout({});
    setSelectedWell(null);
  }, [selectedIds, intl]);

  // Handle archiving samples with storage location assignment
  const handleArchiveSamples = useCallback(() => {
    if (!hasRealPageId) {
      setShowArchiveModal(false);
      return;
    }

    if (!storageSelection.box) {
      setError(
        intl.formatMessage({
          id: "notebook.immunology.reporting.selectStorageBox",
          defaultMessage: "Please select a storage box for archiving.",
        }),
      );
      return;
    }

    // Check if auto-assign has enough wells
    if (
      useAutoAssign &&
      autoAssignPreview.previewWells.length < selectedIds.length
    ) {
      setError(
        intl.formatMessage(
          {
            id: "notebook.immunology.reporting.notEnoughWells",
            defaultMessage:
              "Not enough available wells in the selected box. Available: {available}, Required: {required}",
          },
          {
            available: autoAssignPreview.availableCount,
            required: selectedIds.length,
          },
        ),
      );
      return;
    }

    setIsArchiving(true);

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    // Build storage path
    const storagePath = [
      storageSelection.room?.label,
      storageSelection.device?.label,
      storageSelection.shelf?.label,
      storageSelection.rack?.label,
      storageSelection.box?.label,
    ]
      .filter(Boolean)
      .join(" > ");

    // Determine archive location based on mode
    const archiveLocation = useAutoAssign
      ? `${storagePath} > Auto-assigned`
      : storagePath + (selectedWell ? ` > ${selectedWell}` : "");

    const dataToSave = {
      archived: true,
      archivedAt: new Date().toISOString(),
      archiveLocation: archiveLocation,
      storagePath: storagePath,
      storageWell: useAutoAssign ? null : selectedWell, // Will be assigned per-sample in auto mode
      archiveRoomId: storageSelection.room?.id,
      archiveDeviceId: storageSelection.device?.id,
      archiveShelfId: storageSelection.shelf?.id,
      archiveRackId: storageSelection.rack?.id,
      archiveBoxId: storageSelection.box?.id,
      archiveNotes: archiveData.archiveNotes,
    };

    // First apply archive data
    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: numericIds,
        data: dataToSave,
      }),
      (response) => {
        if (componentMounted.current) {
          if (response && !response.error) {
            // Assign to storage location if box is selected
            if (storageSelection.box?.id) {
              if (useAutoAssign) {
                // Use auto-assign endpoint for sequential well assignment
                const autoAssignRequest = {
                  sampleIds: numericIds,
                  data: {
                    storageRoom: storageSelection.room?.label,
                    storageFreezer: storageSelection.device?.label,
                    storageRack: storageSelection.rack?.label,
                    storageBox: storageSelection.box?.label,
                    storagePath: storagePath,
                    assignedBy: "Archive Operation",
                    assignedDateTime: new Date().toISOString(),
                    notes:
                      archiveData.archiveNotes ||
                      "Archived from Immunology Reporting page",
                  },
                  boxId: parseInt(storageSelection.box.id, 10),
                  rows: storageSelection.box?.rows || 8,
                  columns: storageSelection.box?.columns || 12,
                  occupiedWells: Object.keys(boxLayout),
                };

                postToOpenElisServerJsonResponse(
                  `/rest/notebook/bulk/page/${pageData.id}/samples/storage/auto-assign`,
                  JSON.stringify(autoAssignRequest),
                  (autoResponse) => {
                    const assignedCount = autoResponse?.updatedCount || 0;
                    if (assignedCount > 0) {
                      // Update status to COMPLETED
                      postToOpenElisServer(
                        `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
                        JSON.stringify({
                          sampleIds: numericIds,
                          status: "COMPLETED",
                        }),
                        () => {
                          setIsArchiving(false);
                          setSuccess(
                            intl.formatMessage(
                              {
                                id: "notebook.immunology.reporting.archiveAutoSuccess",
                                defaultMessage:
                                  "Successfully archived {count} samples to {box}. Samples auto-assigned to sequential wells.",
                              },
                              {
                                count: assignedCount,
                                box: storageSelection.box?.label,
                              },
                            ),
                          );
                          setShowArchiveModal(false);
                          setSelectedIds([]);
                          setStorageSelection({
                            room: null,
                            device: null,
                            shelf: null,
                            rack: null,
                            box: null,
                          });
                          setBoxLayout({});
                          setSelectedWell(null);
                          setUseAutoAssign(true);
                          setArchiveData({
                            archiveNotes: "",
                            createBackup: true,
                          });
                          loadPageSamples();
                          if (onProgressUpdate) {
                            onProgressUpdate();
                          }
                        },
                      );
                    } else {
                      setIsArchiving(false);
                      setError(
                        autoResponse?.error ||
                          "Failed to auto-assign storage locations.",
                      );
                    }
                  },
                );
              } else {
                // Manual single well assignment
                const storageRequest = {
                  sampleIds: numericIds,
                  boxId: parseInt(storageSelection.box.id, 10),
                  wellCoordinate: selectedWell,
                  notes:
                    archiveData.archiveNotes ||
                    "Archived from Immunology Reporting page",
                };

                postToOpenElisServerJsonResponse(
                  `/rest/notebook/bulk/page/${pageData.id}/samples/storage`,
                  JSON.stringify(storageRequest),
                  () => {
                    // Update status to COMPLETED
                    postToOpenElisServer(
                      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
                      JSON.stringify({
                        sampleIds: numericIds,
                        status: "COMPLETED",
                      }),
                      () => {
                        setIsArchiving(false);
                        setSuccess(
                          intl.formatMessage(
                            {
                              id: "notebook.immunology.reporting.archiveSuccess",
                              defaultMessage:
                                "Successfully archived {count} samples to {location}. Full audit trail maintained.",
                            },
                            {
                              count: selectedIds.length,
                              location: archiveLocation,
                            },
                          ),
                        );
                        setShowArchiveModal(false);
                        setSelectedIds([]);
                        setStorageSelection({
                          room: null,
                          device: null,
                          shelf: null,
                          rack: null,
                          box: null,
                        });
                        setBoxLayout({});
                        setSelectedWell(null);
                        setUseAutoAssign(true);
                        setArchiveData({
                          archiveNotes: "",
                          createBackup: true,
                        });
                        loadPageSamples();
                        if (onProgressUpdate) {
                          onProgressUpdate();
                        }
                      },
                    );
                  },
                );
              }
            } else {
              setIsArchiving(false);
              setError("Storage box selection is required.");
            }
          } else {
            setIsArchiving(false);
            setError(response?.error || "Failed to archive samples.");
          }
        }
      },
    );
  }, [
    archiveData,
    selectedIds,
    storageSelection,
    selectedWell,
    useAutoAssign,
    autoAssignPreview,
    boxLayout,
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
            id: "notebook.immunology.reporting.pageNotInitialized",
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

  // Render validation status from Data Analysis page
  const renderValidationStatus = (sample) => {
    const status = sample.validationStatus;
    if (!status || status === "PENDING") {
      return (
        <Tag type="gray" size="sm">
          Pending
        </Tag>
      );
    }
    if (status === "VALID") {
      return (
        <Tag type="green" size="sm">
          <CheckmarkFilled size={12} style={{ marginRight: "4px" }} />
          Valid
        </Tag>
      );
    }
    if (status === "INVALID") {
      return (
        <Tag type="red" size="sm">
          Invalid
        </Tag>
      );
    }
    if (status === "INCONCLUSIVE") {
      return (
        <Tag type="orange" size="sm">
          Inconclusive
        </Tag>
      );
    }
    return <span>{status}</span>;
  };

  // Render report status column
  const renderReportStatus = (sample) => {
    if (sample.reportGenerated) {
      return (
        <div style={{ fontSize: "12px" }}>
          <Tag type="green" size="sm">
            <CheckmarkFilled size={12} style={{ marginRight: "4px" }} />
            <FormattedMessage
              id="notebook.immunology.reporting.reportGenerated"
              defaultMessage="Generated"
            />
          </Tag>
          {sample.reportType && (
            <div style={{ marginTop: "2px", color: "#525252" }}>
              {sample.reportType} ({sample.reportFormat})
            </div>
          )}
          {sample.reportGeneratedAt && (
            <div style={{ color: "#8d8d8d", fontSize: "11px" }}>
              {new Date(sample.reportGeneratedAt).toLocaleDateString()}
            </div>
          )}
        </div>
      );
    }
    return (
      <span style={{ color: "#8d8d8d", fontSize: "12px" }}>
        <FormattedMessage
          id="notebook.immunology.reporting.noReport"
          defaultMessage="No report"
        />
      </span>
    );
  };

  // Render REDCap status column
  const renderREDCapStatus = (sample) => {
    if (sample.redcapExported) {
      return (
        <div style={{ fontSize: "12px" }}>
          <Tag type="blue" size="sm">
            <Download size={12} style={{ marginRight: "4px" }} />
            <FormattedMessage
              id="notebook.immunology.reporting.exported"
              defaultMessage="Exported"
            />
          </Tag>
          {sample.redcapProjectId && (
            <div style={{ marginTop: "2px", color: "#525252" }}>
              Project: {sample.redcapProjectId}
            </div>
          )}
          {sample.redcapExportedAt && (
            <div style={{ color: "#8d8d8d", fontSize: "11px" }}>
              {new Date(sample.redcapExportedAt).toLocaleDateString()}
            </div>
          )}
        </div>
      );
    }
    return (
      <span style={{ color: "#8d8d8d", fontSize: "12px" }}>
        <FormattedMessage
          id="notebook.immunology.reporting.notExported"
          defaultMessage="Not exported"
        />
      </span>
    );
  };

  // Render archive status column
  const renderArchiveStatus = (sample) => {
    if (sample.archived) {
      return (
        <div style={{ fontSize: "12px" }}>
          <Tag type="purple" size="sm">
            <Archive size={12} style={{ marginRight: "4px" }} />
            <FormattedMessage
              id="notebook.immunology.reporting.archived"
              defaultMessage="Archived"
            />
          </Tag>
          {sample.archiveLocation && (
            <div style={{ marginTop: "2px", color: "#525252" }}>
              {sample.archiveLocation}
            </div>
          )}
          {sample.archivedAt && (
            <div style={{ color: "#8d8d8d", fontSize: "11px" }}>
              {new Date(sample.archivedAt).toLocaleDateString()}
            </div>
          )}
        </div>
      );
    }
    return (
      <span style={{ color: "#8d8d8d", fontSize: "12px" }}>
        <FormattedMessage
          id="notebook.immunology.reporting.notArchived"
          defaultMessage="Not archived"
        />
      </span>
    );
  };

  return (
    <div className="immunology-reporting-redcap-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.immunology.reporting.title"
            defaultMessage="Reporting & REDCap Integration"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.immunology.reporting.description"
            defaultMessage="Generate reports (with download), export REDCap-compatible files, and archive samples to storage locations."
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
                  id="notebook.immunology.reporting.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{stats.total}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.immunology.reporting.reportsGenerated"
                  defaultMessage="Reports Generated"
                />
              </span>
              <span className="progress-value">{stats.reportGenerated}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.immunology.reporting.redcapExported"
                  defaultMessage="REDCap Exported"
                />
              </span>
              <span className="progress-value">{stats.redcapExported}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.immunology.reporting.archived"
                  defaultMessage="Archived"
                />
              </span>
              <span className="progress-value">{stats.archived}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.immunology.reporting.completed"
                  defaultMessage="Fully Completed"
                />
              </span>
              <span className="progress-value">{stats.completed}</span>
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

      {/* Tabs for different views */}
      <Tabs
        selectedIndex={activeTab}
        onChange={({ selectedIndex }) => setActiveTab(selectedIndex)}
      >
        <TabList aria-label="Reporting tabs">
          <Tab>
            <FormattedMessage
              id="notebook.immunology.reporting.tab.samples"
              defaultMessage="Samples"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="notebook.immunology.reporting.tab.history"
              defaultMessage="Report History"
            />
          </Tab>
        </TabList>

        <TabPanels>
          {/* Samples Tab */}
          <TabPanel>
            {/* Action Buttons */}
            <div className="page-actions-bar">
              <Button
                kind="primary"
                size="sm"
                renderIcon={Report}
                onClick={handleOpenReportModal}
              >
                <FormattedMessage
                  id="notebook.immunology.reporting.generateReport"
                  defaultMessage="Generate Report"
                />
              </Button>

              <Button
                kind="secondary"
                size="sm"
                renderIcon={Download}
                onClick={handleOpenREDCapModal}
                disabled={selectedIds.length === 0}
              >
                <FormattedMessage
                  id="notebook.immunology.reporting.exportREDCap"
                  defaultMessage="Export for REDCap ({count})"
                  values={{ count: selectedIds.length }}
                />
              </Button>

              <Button
                kind="tertiary"
                size="sm"
                renderIcon={Archive}
                onClick={handleOpenArchiveModal}
                disabled={selectedIds.length === 0}
              >
                <FormattedMessage
                  id="notebook.immunology.reporting.archive"
                  defaultMessage="Archive Results ({count})"
                  values={{ count: selectedIds.length }}
                />
              </Button>

              <Button
                kind="ghost"
                size="sm"
                renderIcon={Renew}
                onClick={loadPageSamples}
              >
                <FormattedMessage
                  id="notebook.immunology.reporting.refresh"
                  defaultMessage="Refresh"
                />
              </Button>
            </div>

            {/* Sample Grid */}
            <div className="sample-grid-container">
              <SampleGrid
                gridId="immunology-reporting-redcap"
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
                    key: "validationStatus",
                    header: intl.formatMessage({
                      id: "notebook.immunology.reporting.validationStatus",
                      defaultMessage: "Validation",
                    }),
                    render: renderValidationStatus,
                  },
                  {
                    key: "reportStatus",
                    header: intl.formatMessage({
                      id: "notebook.immunology.reporting.reportStatus",
                      defaultMessage: "Report",
                    }),
                    render: renderReportStatus,
                  },
                  {
                    key: "redcapStatus",
                    header: intl.formatMessage({
                      id: "notebook.immunology.reporting.redcapStatus",
                      defaultMessage: "REDCap",
                    }),
                    render: renderREDCapStatus,
                  },
                  {
                    key: "archiveStatus",
                    header: intl.formatMessage({
                      id: "notebook.immunology.reporting.archiveStatus",
                      defaultMessage: "Archive",
                    }),
                    render: renderArchiveStatus,
                  },
                ]}
              />
            </div>

            {/* Empty state */}
            {!loading && samples.length === 0 && (
              <div className="empty-state">
                <p>
                  <FormattedMessage
                    id="notebook.immunology.reporting.empty"
                    defaultMessage="No samples available for reporting. Please complete previous workflow steps first."
                  />
                </p>
              </div>
            )}
          </TabPanel>

          {/* History Tab */}
          <TabPanel>
            <div style={{ padding: "1rem" }}>
              <h5 style={{ marginBottom: "1rem" }}>
                <FormattedMessage
                  id="notebook.immunology.reporting.reportHistory"
                  defaultMessage="Report Generation History"
                />
              </h5>

              {reportHistory.length === 0 ? (
                <Tile style={{ textAlign: "center", padding: "2rem" }}>
                  <DocumentExport
                    size={32}
                    style={{ marginBottom: "1rem", color: "#8d8d8d" }}
                  />
                  <p style={{ color: "#8d8d8d" }}>
                    <FormattedMessage
                      id="notebook.immunology.reporting.noReportHistory"
                      defaultMessage="No report history available yet. Generated reports will appear here."
                    />
                  </p>
                </Tile>
              ) : (
                <DataTable
                  rows={reportHistory}
                  headers={[
                    { key: "date", header: "Date" },
                    { key: "reportType", header: "Report Type" },
                    { key: "format", header: "Format" },
                    { key: "sampleCount", header: "Samples" },
                    { key: "generatedBy", header: "Generated By" },
                    { key: "action", header: "Action" },
                  ]}
                >
                  {({
                    rows,
                    headers,
                    getTableProps,
                    getHeaderProps,
                    getRowProps,
                  }) => (
                    <TableContainer>
                      <Table {...getTableProps()}>
                        <TableHead>
                          <TableRow>
                            {headers.map((header) => (
                              <TableHeader
                                {...getHeaderProps({ header })}
                                key={header.key}
                              >
                                {header.header}
                              </TableHeader>
                            ))}
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {rows.map((row) => {
                            const originalRow = reportHistory.find(
                              (r) => r.id === row.id,
                            );
                            return (
                              <TableRow {...getRowProps({ row })} key={row.id}>
                                {row.cells.map((cell) => (
                                  <TableCell key={cell.id}>
                                    {cell.info.header === "action" ? (
                                      <Button
                                        kind="ghost"
                                        size="sm"
                                        renderIcon={Download}
                                        onClick={() =>
                                          handleDownloadReport(
                                            originalRow?.fileId,
                                            originalRow?.fileName,
                                          )
                                        }
                                        disabled={!originalRow?.fileId}
                                      >
                                        Download
                                      </Button>
                                    ) : (
                                      cell.value
                                    )}
                                  </TableCell>
                                ))}
                              </TableRow>
                            );
                          })}
                        </TableBody>
                      </Table>
                    </TableContainer>
                  )}
                </DataTable>
              )}
            </div>
          </TabPanel>
        </TabPanels>
      </Tabs>

      {/* Generate Report Modal */}
      <Modal
        open={showReportModal}
        modalHeading={intl.formatMessage({
          id: "notebook.immunology.reporting.modal.reportTitle",
          defaultMessage: "Generate Report",
        })}
        primaryButtonText={
          isGeneratingReport
            ? intl.formatMessage({
                id: "notebook.immunology.reporting.generating",
                defaultMessage: "Generating...",
              })
            : intl.formatMessage({
                id: "notebook.immunology.reporting.generateAndDownload",
                defaultMessage: "Generate & Download",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setShowReportModal(false)}
        onRequestSubmit={handleGenerateReport}
        primaryButtonDisabled={isGeneratingReport}
        size="md"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage
              id="notebook.immunology.reporting.modal.reportDescription"
              defaultMessage="Configure report parameters. The report will be generated and automatically downloaded. It will also be saved to the Report History for future downloads."
            />
          </p>

          {/* Report Type */}
          <Dropdown
            id="report-type"
            titleText={intl.formatMessage({
              id: "notebook.immunology.reporting.reportType",
              defaultMessage: "Report Type",
            })}
            label={intl.formatMessage({
              id: "notebook.immunology.reporting.selectReportType",
              defaultMessage: "Select report type",
            })}
            items={reportTypeOptions}
            itemToString={(item) => (item ? item.text : "")}
            selectedItem={reportTypeOptions.find(
              (r) => r.id === reportData.reportType,
            )}
            onChange={({ selectedItem }) =>
              setReportData({
                ...reportData,
                reportType: selectedItem?.id || "",
              })
            }
            style={{ marginBottom: "1rem" }}
          />

          {/* Report Format */}
          <Dropdown
            id="report-format"
            titleText={intl.formatMessage({
              id: "notebook.immunology.reporting.reportFormat",
              defaultMessage: "Report Format",
            })}
            label={intl.formatMessage({
              id: "notebook.immunology.reporting.selectFormat",
              defaultMessage: "Select format",
            })}
            items={reportFormatOptions}
            itemToString={(item) => (item ? item.text : "")}
            selectedItem={reportFormatOptions.find(
              (f) => f.id === reportData.reportFormat,
            )}
            onChange={({ selectedItem }) =>
              setReportData({
                ...reportData,
                reportFormat: selectedItem?.id || "",
              })
            }
            style={{ marginBottom: "1rem" }}
          />

          {/* Date Range */}
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
                id="notebook.immunology.reporting.dateRange"
                defaultMessage="Date Range (Optional)"
              />
            </h5>
            <Grid fullWidth>
              <Column lg={8} md={4} sm={4}>
                <DatePicker
                  datePickerType="single"
                  onChange={([date]) =>
                    setReportData({
                      ...reportData,
                      dateRangeStart: date?.toISOString().split("T")[0] || "",
                    })
                  }
                >
                  <DatePickerInput
                    id="date-start"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.reporting.startDate",
                      defaultMessage: "Start Date",
                    })}
                    placeholder="mm/dd/yyyy"
                  />
                </DatePicker>
              </Column>
              <Column lg={8} md={4} sm={4}>
                <DatePicker
                  datePickerType="single"
                  onChange={([date]) =>
                    setReportData({
                      ...reportData,
                      dateRangeEnd: date?.toISOString().split("T")[0] || "",
                    })
                  }
                >
                  <DatePickerInput
                    id="date-end"
                    labelText={intl.formatMessage({
                      id: "notebook.immunology.reporting.endDate",
                      defaultMessage: "End Date",
                    })}
                    placeholder="mm/dd/yyyy"
                  />
                </DatePicker>
              </Column>
            </Grid>
          </div>

          {/* Include All Samples */}
          <Checkbox
            id="include-all-samples"
            labelText={intl.formatMessage({
              id: "notebook.immunology.reporting.includeAllSamples",
              defaultMessage: "Include all samples in report",
            })}
            checked={reportData.includeAllSamples}
            onChange={(e, { checked }) =>
              setReportData({ ...reportData, includeAllSamples: checked })
            }
            style={{ marginBottom: "1rem" }}
          />

          {!reportData.includeAllSamples && (
            <p
              style={{
                marginBottom: "1rem",
                fontSize: "12px",
                color: "#525252",
              }}
            >
              <FormattedMessage
                id="notebook.immunology.reporting.selectedSamplesCount"
                defaultMessage="Report will include {count} selected sample(s)"
                values={{ count: selectedIds.length }}
              />
            </p>
          )}

          {/* Recipient Emails */}
          <TextInput
            id="recipient-emails"
            labelText={intl.formatMessage({
              id: "notebook.immunology.reporting.recipientEmails",
              defaultMessage: "Recipient Email(s) - Optional",
            })}
            helperText={intl.formatMessage({
              id: "notebook.immunology.reporting.emailHelper",
              defaultMessage:
                "Separate multiple emails with commas (report will also be emailed)",
            })}
            value={reportData.recipientEmails}
            onChange={(e) =>
              setReportData({
                ...reportData,
                recipientEmails: e.target.value,
              })
            }
            style={{ marginBottom: "1rem" }}
          />

          {/* Notes */}
          <TextArea
            id="report-notes"
            labelText={intl.formatMessage({
              id: "notebook.immunology.reporting.reportNotes",
              defaultMessage: "Notes",
            })}
            value={reportData.reportNotes}
            onChange={(e) =>
              setReportData({ ...reportData, reportNotes: e.target.value })
            }
            rows={3}
          />
        </div>
      </Modal>

      {/* REDCap Export Modal */}
      <Modal
        open={showREDCapModal}
        modalHeading={intl.formatMessage({
          id: "notebook.immunology.reporting.modal.redcapTitle",
          defaultMessage: "Export for REDCap",
        })}
        primaryButtonText={
          isGeneratingREDCapFile
            ? intl.formatMessage({
                id: "notebook.immunology.reporting.generating",
                defaultMessage: "Generating...",
              })
            : intl.formatMessage({
                id: "notebook.immunology.reporting.downloadCSV",
                defaultMessage: "Download CSV",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setShowREDCapModal(false)}
        onRequestSubmit={handleGenerateREDCapFile}
        primaryButtonDisabled={isGeneratingREDCapFile}
        size="md"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage
              id="notebook.immunology.reporting.modal.redcapDescription"
              defaultMessage="Generate a REDCap-compatible CSV file for {count} selected sample(s). You can then import this file directly into your REDCap project."
              values={{ count: selectedIds.length }}
            />
          </p>

          {/* Project ID */}
          <TextInput
            id="redcap-project-id"
            labelText={intl.formatMessage({
              id: "notebook.immunology.reporting.projectId",
              defaultMessage: "REDCap Project ID (Optional)",
            })}
            helperText={intl.formatMessage({
              id: "notebook.immunology.reporting.projectIdHelper",
              defaultMessage: "For reference in the exported file",
            })}
            value={redcapData.projectId}
            onChange={(e) =>
              setRedcapData({ ...redcapData, projectId: e.target.value })
            }
            style={{ marginBottom: "1rem" }}
          />

          {/* Record ID Field */}
          <TextInput
            id="record-id-field"
            labelText={intl.formatMessage({
              id: "notebook.immunology.reporting.recordIdField",
              defaultMessage: "Record ID Field Name",
            })}
            helperText={intl.formatMessage({
              id: "notebook.immunology.reporting.recordIdFieldHelper",
              defaultMessage:
                "The name of the record ID field in your REDCap project",
            })}
            value={redcapData.recordIdField}
            onChange={(e) =>
              setRedcapData({ ...redcapData, recordIdField: e.target.value })
            }
            style={{ marginBottom: "1rem" }}
          />

          {/* Event Name (optional) */}
          <TextInput
            id="event-name"
            labelText={intl.formatMessage({
              id: "notebook.immunology.reporting.eventName",
              defaultMessage: "Event Name (for longitudinal projects)",
            })}
            value={redcapData.eventName}
            onChange={(e) =>
              setRedcapData({ ...redcapData, eventName: e.target.value })
            }
            style={{ marginBottom: "1rem" }}
          />

          {/* Instrument Name (optional) */}
          <TextInput
            id="instrument-name"
            labelText={intl.formatMessage({
              id: "notebook.immunology.reporting.instrumentName",
              defaultMessage: "Instrument Name (optional)",
            })}
            value={redcapData.instrumentName}
            onChange={(e) =>
              setRedcapData({ ...redcapData, instrumentName: e.target.value })
            }
          />

          <div
            style={{
              marginTop: "1rem",
              padding: "0.75rem",
              backgroundColor: "#e0f0ff",
              borderRadius: "4px",
              fontSize: "12px",
            }}
          >
            <strong>
              <FormattedMessage
                id="notebook.immunology.reporting.redcapNote"
                defaultMessage="Note:"
              />
            </strong>{" "}
            <FormattedMessage
              id="notebook.immunology.reporting.redcapNoteText"
              defaultMessage="The CSV file will be formatted for direct import into REDCap. After downloading, go to your REDCap project > Data Import Tool > Upload your CSV file."
            />
          </div>
        </div>
      </Modal>

      {/* Archive Modal with Storage Hierarchy */}
      <Modal
        open={showArchiveModal}
        modalHeading={intl.formatMessage({
          id: "notebook.immunology.reporting.modal.archiveTitle",
          defaultMessage: "Archive to Storage",
        })}
        primaryButtonText={
          isArchiving
            ? intl.formatMessage({
                id: "notebook.immunology.reporting.archiving",
                defaultMessage: "Archiving...",
              })
            : useAutoAssign
              ? intl.formatMessage({
                  id: "notebook.immunology.reporting.autoArchiveButton",
                  defaultMessage: "Auto-Archive",
                })
              : intl.formatMessage({
                  id: "notebook.immunology.reporting.archiveButton",
                  defaultMessage: "Archive",
                })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setShowArchiveModal(false);
          setStorageSelection({
            room: null,
            device: null,
            shelf: null,
            rack: null,
            box: null,
          });
          setBoxLayout({});
          setSelectedWell(null);
          setUseAutoAssign(true);
        }}
        onRequestSubmit={handleArchiveSamples}
        primaryButtonDisabled={
          isArchiving ||
          !storageSelection.box ||
          (useAutoAssign &&
            autoAssignPreview.previewWells.length < selectedIds.length)
        }
        size="lg"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage
              id="notebook.immunology.reporting.modal.archiveDescription"
              defaultMessage="Select storage location to archive {count} sample(s). Use the hierarchy selector below to choose the exact location."
              values={{ count: selectedIds.length }}
            />
          </p>

          {/* Storage Hierarchy Selector */}
          <div
            style={{
              padding: "1rem",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
              marginBottom: "1rem",
            }}
          >
            <h5 style={{ marginBottom: "1rem" }}>
              <FormattedMessage
                id="notebook.immunology.reporting.storageLocation"
                defaultMessage="Storage Location"
              />
            </h5>
            <StorageHierarchySelector
              onSelectionChange={handleStorageSelectionChange}
              boxRequired={true}
              showPath={true}
              entryId={entryId}
            />

            {/* Storage path and availability info when box is selected */}
            {storageSelection.box && (
              <div
                style={{
                  marginTop: "1rem",
                  padding: "0.75rem",
                  backgroundColor: "#fff",
                  borderRadius: "4px",
                  border: "1px solid #e0e0e0",
                }}
              >
                <strong>
                  <FormattedMessage
                    id="notebook.immunology.reporting.storagePath"
                    defaultMessage="Storage Path:"
                  />
                </strong>{" "}
                {[
                  storageSelection.room?.label,
                  storageSelection.device?.label,
                  storageSelection.shelf?.label,
                  storageSelection.rack?.label,
                  storageSelection.box?.label,
                ]
                  .filter(Boolean)
                  .join(" > ")}
                <div
                  style={{
                    marginTop: "0.5rem",
                    display: "flex",
                    gap: "0.5rem",
                  }}
                >
                  <Tag type="blue">
                    {autoAssignPreview.availableCount} wells available
                  </Tag>
                  <Tag type="green">
                    {autoAssignPreview.previewWells.length} will be assigned
                  </Tag>
                  {autoAssignPreview.availableCount < selectedIds.length && (
                    <Tag type="red">Not enough wells!</Tag>
                  )}
                </div>
              </div>
            )}
          </div>

          {/* Assignment Mode Toggle */}
          {storageSelection.box && (
            <div
              style={{
                padding: "1rem",
                backgroundColor: "#fff",
                border: "1px solid #e0e0e0",
                borderRadius: "4px",
                marginBottom: "1rem",
              }}
            >
              <Checkbox
                id="use-auto-assign"
                labelText={intl.formatMessage({
                  id: "notebook.immunology.reporting.useAutoAssign",
                  defaultMessage:
                    "Auto-assign samples to sequential wells (recommended)",
                })}
                checked={useAutoAssign}
                onChange={(_, { checked }) => {
                  setUseAutoAssign(checked);
                  if (checked) {
                    setSelectedWell(null);
                  }
                }}
              />
              <p
                style={{
                  fontSize: "12px",
                  color: "#525252",
                  marginTop: "0.5rem",
                  marginLeft: "1.5rem",
                }}
              >
                {useAutoAssign ? (
                  <FormattedMessage
                    id="notebook.immunology.reporting.autoAssignDescription"
                    defaultMessage="Samples will be automatically assigned to the next available wells starting from A1."
                  />
                ) : (
                  <FormattedMessage
                    id="notebook.immunology.reporting.manualAssignDescription"
                    defaultMessage="Click on a specific well in the box layout below to assign all samples to that position."
                  />
                )}
              </p>
            </div>
          )}

          {/* Box Layout Preview with Auto-Assign Visualization */}
          {storageSelection.box && (
            <div
              style={{
                padding: "1rem",
                backgroundColor: "#fff",
                border: "1px solid #e0e0e0",
                borderRadius: "4px",
                marginBottom: "1rem",
              }}
            >
              <h5 style={{ marginBottom: "0.5rem" }}>
                <FormattedMessage
                  id="notebook.immunology.reporting.boxLayout"
                  defaultMessage="Box Layout"
                />{" "}
                - {storageSelection.box.label}
              </h5>
              <p
                style={{
                  fontSize: "12px",
                  color: "#525252",
                  marginBottom: "1rem",
                }}
              >
                {useAutoAssign ? (
                  <FormattedMessage
                    id="notebook.immunology.reporting.autoAssignPreviewHint"
                    defaultMessage="Green wells show where samples will be auto-assigned."
                  />
                ) : (
                  <FormattedMessage
                    id="notebook.immunology.reporting.clickWell"
                    defaultMessage="Click on an empty well to select a specific position (optional)"
                  />
                )}
              </p>

              {/* Auto-Assign Preview Grid (like Temporary Storage page) */}
              <div
                className="box-grid-container"
                style={{
                  display: "grid",
                  gridTemplateColumns: `auto repeat(${storageSelection.box?.columns || 12}, 1fr)`,
                  gap: "2px",
                  fontSize: "10px",
                  maxWidth: "100%",
                  overflowX: "auto",
                }}
              >
                {/* Column headers */}
                <div style={{ padding: "2px 4px" }}></div>
                {Array.from(
                  { length: storageSelection.box?.columns || 12 },
                  (_, i) => (
                    <div
                      key={`col-${i}`}
                      style={{
                        textAlign: "center",
                        fontWeight: "bold",
                        padding: "2px",
                      }}
                    >
                      {i + 1}
                    </div>
                  ),
                )}

                {/* Grid rows */}
                {Array.from(
                  { length: storageSelection.box?.rows || 8 },
                  (_, rowIdx) => {
                    const rowLetter = String.fromCharCode(
                      "A".charCodeAt(0) + rowIdx,
                    );
                    return (
                      <React.Fragment key={`row-${rowIdx}`}>
                        <div
                          style={{
                            fontWeight: "bold",
                            padding: "2px 4px",
                            display: "flex",
                            alignItems: "center",
                          }}
                        >
                          {rowLetter}
                        </div>
                        {Array.from(
                          { length: storageSelection.box?.columns || 12 },
                          (_, colIdx) => {
                            const wellCoord = `${rowLetter}${colIdx + 1}`;
                            const isOccupied = boxLayout[wellCoord];
                            const willBeAssigned =
                              useAutoAssign &&
                              autoAssignPreview.previewWells.includes(
                                wellCoord,
                              );
                            const isManuallySelected =
                              !useAutoAssign && selectedWell === wellCoord;

                            let bgColor = "#e0e0e0"; // Empty
                            let borderColor = "#c0c0c0";
                            let cursor = "default";

                            if (isOccupied) {
                              bgColor = "#a8a8a8"; // Already occupied
                              borderColor = "#808080";
                            } else if (willBeAssigned || isManuallySelected) {
                              bgColor = "#42be65"; // Will be assigned (green)
                              borderColor = "#24a148";
                            } else if (!useAutoAssign) {
                              cursor = "pointer";
                            }

                            return (
                              <div
                                key={wellCoord}
                                title={
                                  isOccupied
                                    ? `${wellCoord} - Occupied`
                                    : willBeAssigned
                                      ? `${wellCoord} - Will be assigned`
                                      : isManuallySelected
                                        ? `${wellCoord} - Selected`
                                        : `${wellCoord} - Empty`
                                }
                                onClick={() => {
                                  if (!useAutoAssign && !isOccupied) {
                                    setSelectedWell(wellCoord);
                                  }
                                }}
                                style={{
                                  width: "20px",
                                  height: "20px",
                                  borderRadius: "50%",
                                  backgroundColor: bgColor,
                                  border: `1px solid ${borderColor}`,
                                  margin: "auto",
                                  cursor: cursor,
                                  transition: "background-color 0.2s",
                                }}
                              />
                            );
                          },
                        )}
                      </React.Fragment>
                    );
                  },
                )}
              </div>

              {/* Legend */}
              <div
                style={{
                  display: "flex",
                  gap: "1rem",
                  marginTop: "0.5rem",
                  fontSize: "12px",
                }}
              >
                <div
                  style={{ display: "flex", alignItems: "center", gap: "4px" }}
                >
                  <div
                    style={{
                      width: "12px",
                      height: "12px",
                      borderRadius: "50%",
                      backgroundColor: "#42be65",
                    }}
                  />
                  <span>{useAutoAssign ? "Will be assigned" : "Selected"}</span>
                </div>
                <div
                  style={{ display: "flex", alignItems: "center", gap: "4px" }}
                >
                  <div
                    style={{
                      width: "12px",
                      height: "12px",
                      borderRadius: "50%",
                      backgroundColor: "#a8a8a8",
                    }}
                  />
                  <span>Already occupied</span>
                </div>
                <div
                  style={{ display: "flex", alignItems: "center", gap: "4px" }}
                >
                  <div
                    style={{
                      width: "12px",
                      height: "12px",
                      borderRadius: "50%",
                      backgroundColor: "#e0e0e0",
                    }}
                  />
                  <span>Empty</span>
                </div>
              </div>

              {/* Manual selection indicator */}
              {!useAutoAssign && selectedWell && (
                <p
                  style={{
                    marginTop: "0.5rem",
                    fontSize: "12px",
                    color: "#0f62fe",
                  }}
                >
                  <FormattedMessage
                    id="notebook.immunology.reporting.selectedWell"
                    defaultMessage="Selected position: {well}"
                    values={{ well: selectedWell }}
                  />
                </p>
              )}
            </div>
          )}

          {/* Archive Notes */}
          <TextArea
            id="archive-notes"
            labelText={intl.formatMessage({
              id: "notebook.immunology.reporting.archiveNotes",
              defaultMessage: "Archive Notes",
            })}
            placeholder={intl.formatMessage({
              id: "notebook.immunology.reporting.archiveNotesPlaceholder",
              defaultMessage: "Optional notes about this archive operation",
            })}
            value={archiveData.archiveNotes}
            onChange={(e) =>
              setArchiveData({ ...archiveData, archiveNotes: e.target.value })
            }
            rows={3}
            style={{ marginBottom: "1rem" }}
          />

          {/* Create Backup */}
          <Checkbox
            id="create-backup"
            labelText={intl.formatMessage({
              id: "notebook.immunology.reporting.createBackup",
              defaultMessage: "Create backup before archiving",
            })}
            checked={archiveData.createBackup}
            onChange={(_, { checked }) =>
              setArchiveData({ ...archiveData, createBackup: checked })
            }
          />

          {storageSelection.box && (
            <p
              style={{
                marginTop: "1rem",
                padding: "0.5rem",
                backgroundColor: "#e0f0e0",
                borderRadius: "4px",
                fontSize: "12px",
              }}
            >
              <FormattedMessage
                id="notebook.immunology.reporting.archiveLocationPreview"
                defaultMessage="Samples will be archived to: {location}"
                values={{
                  location: [
                    storageSelection.room?.label,
                    storageSelection.device?.label,
                    storageSelection.shelf?.label,
                    storageSelection.rack?.label,
                    storageSelection.box?.label,
                    selectedWell,
                  ]
                    .filter(Boolean)
                    .join(" > "),
                }}
              />
            </p>
          )}
        </div>
      </Modal>
    </div>
  );
}

export default ImmunologyReportingREDCapPage;
