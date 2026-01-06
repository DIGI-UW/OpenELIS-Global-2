import React, {
  useState,
  useEffect,
  useRef,
  useCallback,
  useContext,
} from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Modal,
  Dropdown,
  TextInput,
  TextArea,
  Tag,
  Loading,
  ProgressBar,
  Checkbox,
  StructuredListWrapper,
  StructuredListHead,
  StructuredListBody,
  StructuredListRow,
  StructuredListCell,
  RadioButtonGroup,
  RadioButton,
  Select,
  SelectItem,
  DatePicker,
  DatePickerInput,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableSelectRow,
  TableSelectAll,
  TableContainer,
  Accordion,
  AccordionItem,
} from "@carbon/react";
import {
  Archive,
  CheckmarkFilled,
  DocumentExport,
  TrashCan,
  Temperature,
  Document,
  Catalog,
  Time,
  User,
  Warning,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";
import TraceabilityChecklist from "../workflow/TraceabilityChecklist";
import "../workflow/NotebookWorkflow.css";

/**
 * EndOfProjectArchivingPage - Shared page for both Immunology and MedLab workflows.
 * Handles end-of-project archiving, biorepository transfer, sample disposal, and notebook finalization.
 *
 * For Immunology workflow:
 * - Transfer remaining Parent and Child Samples to Biorepository Laboratory
 * - Verify complete traceability links
 *
 * For MedLab workflow:
 * - Sample disposal with method selection (incineration, autoclaving, chemical treatment)
 * - Retention period and biobank transfer management
 * - Accreditation support with audit trails and SOP compliance
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function EndOfProjectArchivingPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  // Detect if this is MedLab workflow based on pageData
  const isMedLabWorkflow =
    pageData?.workflowId === "medlab" ||
    pageData?.category === "validation" ||
    pageData?.pageId === "disposal-archiving" ||
    pageData?.pageId === "end-of-project-archiving";

  // State
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Archiving progress state
  const [archivingProgress, setArchivingProgress] = useState(null);
  const [archivableSamples, setArchivableSamples] = useState({
    parent: [],
    child: [],
  });

  // Traceability state
  const [traceabilityResult, setTraceabilityResult] = useState(null);
  const [verifyingTraceability, setVerifyingTraceability] = useState(false);

  // Transfer modal state
  const [transferModalOpen, setTransferModalOpen] = useState(false);
  const [transferring, setTransferring] = useState(false);

  // Finalization modal state
  const [finalizeModalOpen, setFinalizeModalOpen] = useState(false);
  const [finalizing, setFinalizing] = useState(false);
  const [confirmFinalize, setConfirmFinalize] = useState(false);

  // Storage location selection (simplified for biorepository)
  const [rooms, setRooms] = useState([]);
  const [devices, setDevices] = useState([]);
  const [selectedRoom, setSelectedRoom] = useState(null);
  const [selectedDevice, setSelectedDevice] = useState(null);
  const [transferNotes, setTransferNotes] = useState("");

  // ==================== MedLab-specific state ====================
  // Disposal state
  const [disposalSamples, setDisposalSamples] = useState([]);
  const [selectedDisposalSampleIds, setSelectedDisposalSampleIds] = useState(
    [],
  );
  const [disposalModalOpen, setDisposalModalOpen] = useState(false);
  const [disposing, setDisposing] = useState(false);
  const [disposalReason, setDisposalReason] = useState("expiry");
  const [disposalMethod, setDisposalMethod] = useState("INCINERATION");
  const [disposalDate, setDisposalDate] = useState(
    new Date().toISOString().split("T")[0],
  );
  const [responsiblePerson, setResponsiblePerson] = useState("");
  const [facilityDetails, setFacilityDetails] = useState("");
  const [disposalNotes, setDisposalNotes] = useState("");
  const [disposalRecords, setDisposalRecords] = useState([]);

  // Archiving (MedLab) state
  const [archivingModalOpen, setArchivingModalOpen] = useState(false);
  const [archiving, setArchiving] = useState(false);
  const [retentionYears, setRetentionYears] = useState(2);
  const [storageCondition, setStorageCondition] = useState("-80°C");
  const [transferToBiobank, setTransferToBiobank] = useState(false);
  const [biobankDetails, setBiobankDetails] = useState("");
  const [archiveNotes, setArchiveNotes] = useState("");
  const [archivingRecords, setArchivingRecords] = useState([]);

  // Summary state
  const [summary, setSummary] = useState(null);

  // Accreditation state
  const [auditTrail, setAuditTrail] = useState([]);
  const [sopCompliance, setSopCompliance] = useState(null);
  const [activeTab, setActiveTab] = useState(0);

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Disposal method options based on sample type rules
  const disposalMethods = [
    {
      value: "INCINERATION",
      label: intl.formatMessage({
        id: "medlab.disposal.method.incineration",
        defaultMessage: "Incineration",
      }),
    },
    {
      value: "AUTOCLAVING",
      label: intl.formatMessage({
        id: "medlab.disposal.method.autoclaving",
        defaultMessage: "Autoclaving",
      }),
    },
    {
      value: "CHEMICAL_TREATMENT",
      label: intl.formatMessage({
        id: "medlab.disposal.method.chemical",
        defaultMessage: "Chemical Treatment",
      }),
    },
  ];

  // Disposal reasons
  const disposalReasons = [
    {
      value: "expiry",
      label: intl.formatMessage({
        id: "medlab.disposal.reason.expiry",
        defaultMessage: "Expiry",
      }),
    },
    {
      value: "exhaustion",
      label: intl.formatMessage({
        id: "medlab.disposal.reason.exhaustion",
        defaultMessage: "Sample Exhaustion",
      }),
    },
    {
      value: "qc_fail",
      label: intl.formatMessage({
        id: "medlab.disposal.reason.qcFail",
        defaultMessage: "QC Failure",
      }),
    },
  ];

  // Storage conditions
  const storageConditions = [
    { value: "-80°C", label: "-80°C (Bioequivalence)" },
    { value: "-20°C", label: "-20°C" },
    { value: "2-8°C", label: "2-8°C (Refrigerated)" },
    { value: "Room Temperature", label: "Room Temperature" },
  ];

  // Load Immunology-specific data
  const loadImmunologyData = useCallback(() => {
    // Load archiving progress
    getFromOpenElisServer(
      `/rest/notebook/${entryId}/archive/progress`,
      (response) => {
        if (componentMounted.current) {
          setArchivingProgress(response);
        }
      },
    );

    // Load archivable samples
    getFromOpenElisServer(
      `/rest/notebook/${entryId}/archive/samples`,
      (response) => {
        if (componentMounted.current) {
          setArchivableSamples(response || { parent: [], child: [] });
          // Build sample list for grid
          const allSamples = [
            ...(response?.parent || []).map((id) => ({
              sampleItemId: id,
              type: "parent",
            })),
            ...(response?.child || []).map((id) => ({
              sampleItemId: id,
              type: "child",
            })),
          ];
          setSamples(allSamples);
          setLoading(false);
        }
      },
    );
  }, [entryId]);

  // Load MedLab-specific data
  const loadMedLabData = useCallback(() => {
    // Load samples for disposal
    getFromOpenElisServer(
      `/rest/medlab/entry/${entryId}/samples-for-disposal`,
      (response) => {
        if (componentMounted.current) {
          setDisposalSamples(Array.isArray(response) ? response : []);
        }
      },
    );

    // Load disposal records
    getFromOpenElisServer(
      `/rest/medlab/entry/${entryId}/disposal-records`,
      (response) => {
        if (componentMounted.current) {
          setDisposalRecords(Array.isArray(response) ? response : []);
        }
      },
    );

    // Load archiving records
    getFromOpenElisServer(
      `/rest/medlab/entry/${entryId}/archiving-records`,
      (response) => {
        if (componentMounted.current) {
          setArchivingRecords(Array.isArray(response) ? response : []);
        }
      },
    );

    // Load summary
    getFromOpenElisServer(
      `/rest/medlab/entry/${entryId}/disposal-archiving-summary`,
      (response) => {
        if (componentMounted.current) {
          setSummary(response || {});
          setLoading(false);
        }
      },
    );

    // Load audit trail
    getFromOpenElisServer(
      `/rest/medlab/entry/${entryId}/audit-trail`,
      (response) => {
        if (componentMounted.current) {
          setAuditTrail(Array.isArray(response) ? response : []);
        }
      },
    );

    // Load SOP compliance
    getFromOpenElisServer(
      `/rest/medlab/entry/${entryId}/sop-compliance`,
      (response) => {
        if (componentMounted.current) {
          setSopCompliance(response || {});
        }
      },
    );
  }, [entryId]);

  // Define loadPageData before the useEffect that uses it
  const loadPageData = useCallback(() => {
    if (!entryId) {
      setLoading(false);
      return;
    }

    setLoading(true);

    // Check if this is MedLab workflow - use MedLab endpoints
    if (isMedLabWorkflow) {
      loadMedLabData();
    } else {
      loadImmunologyData();
    }
  }, [entryId, isMedLabWorkflow, loadMedLabData, loadImmunologyData]);

  // Load data on mount
  useEffect(() => {
    componentMounted.current = true;
    loadPageData();
    loadRooms();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id, loadPageData]);

  const loadRooms = () => {
    getFromOpenElisServer("/rest/storage/rooms", (response) => {
      if (componentMounted.current) {
        const roomOptions =
          response?.map((room) => ({
            id: room.id.toString(),
            label: room.name,
          })) || [];
        setRooms(roomOptions);
      }
    });
  };

  const loadDevices = (roomId) => {
    if (!roomId) {
      setDevices([]);
      return;
    }
    getFromOpenElisServer(
      `/rest/storage/devices?roomId=${roomId}`,
      (response) => {
        if (componentMounted.current) {
          const deviceOptions =
            response?.map((device) => ({
              id: device.id.toString(),
              label: device.name,
            })) || [];
          setDevices(deviceOptions);
        }
      },
    );
  };

  const handleRoomChange = ({ selectedItem }) => {
    setSelectedRoom(selectedItem);
    setSelectedDevice(null);
    if (selectedItem) {
      loadDevices(selectedItem.id);
    } else {
      setDevices([]);
    }
  };

  const handleVerifyTraceability = async () => {
    setVerifyingTraceability(true);
    setError(null);

    postToOpenElisServerJsonResponse(
      `/rest/notebook/${entryId}/archive/verify-traceability`,
      {},
      (response) => {
        if (componentMounted.current) {
          setTraceabilityResult(response);
          setVerifyingTraceability(false);
        }
      },
      () => {
        if (componentMounted.current) {
          setError(
            intl.formatMessage({
              id: "notebook.archive.verifyError",
              defaultMessage: "Failed to verify traceability",
            }),
          );
          setVerifyingTraceability(false);
        }
      },
    );
  };

  const handleTransferToBiorepository = async () => {
    if (!selectedDevice) {
      setError(
        intl.formatMessage({
          id: "notebook.archive.selectLocation",
          defaultMessage: "Please select a biorepository location",
        }),
      );
      return;
    }

    setTransferring(true);
    setError(null);

    const sampleIds =
      selectedSampleIds.length > 0
        ? selectedSampleIds
        : [...archivableSamples.parent, ...archivableSamples.child];

    const requestBody = {
      sampleItemIds: sampleIds,
      locationId: selectedDevice.id,
      locationType: "device",
      notes: transferNotes || "End of project transfer to biorepository",
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/${entryId}/archive/transfer`,
      JSON.stringify(requestBody),
      (response) => {
        if (componentMounted.current) {
          if (response.success) {
            setSuccess(
              intl.formatMessage(
                {
                  id: "notebook.archive.transferSuccess",
                  defaultMessage:
                    "{count} samples transferred to biorepository",
                },
                { count: response.transferredCount },
              ),
            );
            setTransferModalOpen(false);
            loadPageData(); // Reload to update progress
            if (onProgressUpdate) onProgressUpdate();
          } else {
            setError(response.error || "Transfer failed");
          }
          setTransferring(false);
        }
      },
      () => {
        if (componentMounted.current) {
          setError(
            intl.formatMessage({
              id: "notebook.archive.transferError",
              defaultMessage: "Failed to transfer samples",
            }),
          );
          setTransferring(false);
        }
      },
    );
  };

  const handleFinalize = async () => {
    if (!confirmFinalize) {
      setError(
        intl.formatMessage({
          id: "notebook.archive.confirmRequired",
          defaultMessage:
            "Please confirm that you want to finalize this notebook",
        }),
      );
      return;
    }

    setFinalizing(true);
    setError(null);

    postToOpenElisServerJsonResponse(
      `/rest/notebook/${entryId}/archive/finalize`,
      {},
      (response) => {
        if (componentMounted.current) {
          if (response.success) {
            setSuccess(
              intl.formatMessage({
                id: "notebook.archive.finalizeSuccess",
                defaultMessage: "Notebook has been finalized successfully",
              }),
            );
            setFinalizeModalOpen(false);
            if (onProgressUpdate) onProgressUpdate();
          } else {
            setError(response.error || "Finalization failed");
          }
          setFinalizing(false);
        }
      },
      () => {
        if (componentMounted.current) {
          setError(
            intl.formatMessage({
              id: "notebook.archive.finalizeError",
              defaultMessage: "Failed to finalize notebook",
            }),
          );
          setFinalizing(false);
        }
      },
    );
  };

  const canFinalize =
    traceabilityResult?.passed && archivingProgress?.readyForFinalization;

  // ==================== MedLab Handler Functions ====================

  // Handle disposal submission
  const handleRecordDisposal = useCallback(() => {
    if (selectedDisposalSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "medlab.disposal.selectSamples",
          defaultMessage: "Please select samples for disposal",
        }),
      );
      return;
    }

    setDisposing(true);
    setError(null);

    const requestBody = {
      sampleIds: selectedDisposalSampleIds,
      disposalReason,
      disposalMethod,
      disposalDate,
      responsiblePerson,
      facilityDetails,
      notes: disposalNotes,
      notebookPageId: pageData?.id,
    };

    postToOpenElisServer(
      "/rest/medlab/record-disposal",
      JSON.stringify(requestBody),
      (status) => {
        if (!componentMounted.current) return;
        if (status === 200) {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "medlab.disposal.success",
              defaultMessage: "Samples disposed successfully",
            }),
            kind: NotificationKinds.success,
          });
          setNotificationVisible(true);
          setDisposalModalOpen(false);
          setSelectedDisposalSampleIds([]);
          loadMedLabData();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          setError(
            intl.formatMessage({
              id: "medlab.disposal.error",
              defaultMessage: "Error recording disposal",
            }),
          );
        }
        setDisposing(false);
      },
    );
  }, [
    selectedDisposalSampleIds,
    disposalReason,
    disposalMethod,
    disposalDate,
    responsiblePerson,
    facilityDetails,
    disposalNotes,
    pageData,
    intl,
    addNotification,
    setNotificationVisible,
    loadMedLabData,
    onProgressUpdate,
  ]);

  // Handle archiving submission
  const handleRecordArchiving = useCallback(() => {
    if (selectedDisposalSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "medlab.archiving.selectSamples",
          defaultMessage: "Please select samples for archiving",
        }),
      );
      return;
    }

    setArchiving(true);
    setError(null);

    const requestBody = {
      sampleIds: selectedDisposalSampleIds,
      retentionYears,
      storageCondition,
      transferToBiobank,
      biobankDetails,
      notes: archiveNotes,
      notebookPageId: pageData?.id,
    };

    postToOpenElisServer(
      "/rest/medlab/record-archiving",
      JSON.stringify(requestBody),
      (status) => {
        if (!componentMounted.current) return;
        if (status === 200) {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "medlab.archiving.success",
              defaultMessage: "Samples archived successfully",
            }),
            kind: NotificationKinds.success,
          });
          setNotificationVisible(true);
          setArchivingModalOpen(false);
          setSelectedDisposalSampleIds([]);
          loadMedLabData();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          setError(
            intl.formatMessage({
              id: "medlab.archiving.error",
              defaultMessage: "Error recording archiving",
            }),
          );
        }
        setArchiving(false);
      },
    );
  }, [
    selectedDisposalSampleIds,
    retentionYears,
    storageCondition,
    transferToBiobank,
    biobankDetails,
    archiveNotes,
    pageData,
    intl,
    addNotification,
    setNotificationVisible,
    loadMedLabData,
    onProgressUpdate,
  ]);

  // Handle MedLab finalization
  const handleMedLabFinalize = useCallback(() => {
    if (!confirmFinalize) {
      setError(
        intl.formatMessage({
          id: "notebook.archive.confirmRequired",
          defaultMessage:
            "Please confirm that you want to finalize this notebook",
        }),
      );
      return;
    }

    setFinalizing(true);
    setError(null);

    postToOpenElisServer(
      `/rest/medlab/entry/${entryId}/finalize`,
      JSON.stringify({}),
      (status) => {
        if (!componentMounted.current) return;
        if (status === 200) {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "notebook.archive.finalizeSuccess",
              defaultMessage: "Notebook has been finalized successfully",
            }),
            kind: NotificationKinds.success,
          });
          setNotificationVisible(true);
          setFinalizeModalOpen(false);
          if (onProgressUpdate) onProgressUpdate();
        } else {
          setError(
            intl.formatMessage({
              id: "notebook.archive.finalizeError",
              defaultMessage: "Failed to finalize notebook",
            }),
          );
        }
        setFinalizing(false);
      },
    );
  }, [
    confirmFinalize,
    entryId,
    intl,
    addNotification,
    setNotificationVisible,
    onProgressUpdate,
  ]);

  // Get tag type for disposal status
  const getDisposalStatusTagType = (status) => {
    switch (status) {
      case "DISPOSED":
        return "red";
      case "ARCHIVED":
        return "blue";
      case "PENDING":
        return "gray";
      default:
        return "gray";
    }
  };

  // MedLab can finalize check
  const canMedLabFinalize =
    summary?.pendingCount === 0 && sopCompliance?.allCompliant;

  // Render loading state
  if (loading) {
    return (
      <div className="page-loading">
        <Loading withOverlay={false} />
        <p>
          <FormattedMessage
            id="notebook.page.loading"
            defaultMessage="Loading page data..."
          />
        </p>
      </div>
    );
  }

  // ==================== MedLab Workflow Render ====================
  if (isMedLabWorkflow) {
    return (
      <div className="notebook-page disposal-archiving-page">
        {/* Page Header */}
        <div className="page-section-header">
          <h4>
            <FormattedMessage
              id="medlab.page.disposalArchiving.title"
              defaultMessage="Disposal, Archiving & Accreditation"
            />
          </h4>
          <p className="page-description">
            <FormattedMessage
              id="medlab.page.disposalArchiving.description"
              defaultMessage="Close the lifecycle of samples with proper disposal/archiving and ensure accreditation compliance."
            />
          </p>
        </div>

        {/* Notifications */}
        {error && (
          <InlineNotification
            kind="error"
            title={intl.formatMessage({ id: "error", defaultMessage: "Error" })}
            subtitle={error}
            onCloseButtonClick={() => setError(null)}
            style={{ marginBottom: "1rem" }}
          />
        )}

        {/* Progress Summary Tiles */}
        <Grid fullWidth className="progress-section">
          <Column lg={16} md={8} sm={4}>
            <div className="progress-tiles">
              <Tile className="progress-tile">
                <span className="progress-label">
                  <FormattedMessage
                    id="medlab.disposal.totalSamples"
                    defaultMessage="Total Samples"
                  />
                </span>
                <span className="progress-value">
                  {summary?.totalCount || 0}
                </span>
              </Tile>
              <Tile className="progress-tile verified">
                <span className="progress-label">
                  <FormattedMessage
                    id="medlab.disposal.disposed"
                    defaultMessage="Disposed"
                  />
                </span>
                <span className="progress-value">
                  {summary?.disposedCount || 0}
                </span>
              </Tile>
              <Tile className="progress-tile pending">
                <span className="progress-label">
                  <FormattedMessage
                    id="medlab.disposal.archived"
                    defaultMessage="Archived"
                  />
                </span>
                <span className="progress-value">
                  {summary?.archivedCount || 0}
                </span>
              </Tile>
              <Tile className="progress-tile">
                <span className="progress-label">
                  <FormattedMessage
                    id="medlab.disposal.pending"
                    defaultMessage="Pending"
                  />
                </span>
                <span className="progress-value">
                  {summary?.pendingCount || 0}
                </span>
              </Tile>
            </div>
          </Column>
        </Grid>

        {/* Tabs for Disposal, Archiving, Accreditation */}
        <Tabs
          selectedIndex={activeTab}
          onChange={({ selectedIndex }) => setActiveTab(selectedIndex)}
        >
          <TabList aria-label="Disposal and Archiving tabs">
            <Tab>
              <TrashCan size={16} style={{ marginRight: "0.5rem" }} />
              <FormattedMessage
                id="medlab.tab.disposal"
                defaultMessage="Disposal"
              />
            </Tab>
            <Tab>
              <Archive size={16} style={{ marginRight: "0.5rem" }} />
              <FormattedMessage
                id="medlab.tab.archiving"
                defaultMessage="Archiving"
              />
            </Tab>
            <Tab>
              <Document size={16} style={{ marginRight: "0.5rem" }} />
              <FormattedMessage
                id="medlab.tab.accreditation"
                defaultMessage="Accreditation"
              />
            </Tab>
          </TabList>
          <TabPanels>
            {/* Disposal Tab */}
            <TabPanel>
              <div style={{ padding: "1rem 0" }}>
                <h5>
                  <FormattedMessage
                    id="medlab.disposal.samplesForDisposal"
                    defaultMessage="Samples Available for Disposal"
                  />
                </h5>
                {disposalSamples.length > 0 ? (
                  <DataTable
                    rows={disposalSamples.map((s, idx) => ({
                      ...s,
                      id: s.sampleItemId?.toString() || idx.toString(),
                    }))}
                    headers={[
                      {
                        key: "labNo",
                        header: intl.formatMessage({
                          id: "sample.label.labnumber",
                          defaultMessage: "Lab No",
                        }),
                      },
                      {
                        key: "sampleType",
                        header: intl.formatMessage({
                          id: "sample.sampleType",
                          defaultMessage: "Sample Type",
                        }),
                      },
                      {
                        key: "patientName",
                        header: intl.formatMessage({
                          id: "patient.label",
                          defaultMessage: "Patient",
                        }),
                      },
                      {
                        key: "recommendedMethod",
                        header: intl.formatMessage({
                          id: "medlab.disposal.recommendedMethod",
                          defaultMessage: "Recommended Method",
                        }),
                      },
                      {
                        key: "disposalStatus",
                        header: intl.formatMessage({
                          id: "medlab.disposal.status",
                          defaultMessage: "Status",
                        }),
                      },
                    ]}
                    size="md"
                  >
                    {({
                      rows,
                      headers,
                      getHeaderProps,
                      getRowProps,
                      getTableProps,
                      getSelectionProps,
                    }) => (
                      <TableContainer>
                        <Table {...getTableProps()}>
                          <TableHead>
                            <TableRow>
                              <TableSelectAll {...getSelectionProps()} />
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
                            {rows.map((row) => (
                              <TableRow key={row.id} {...getRowProps({ row })}>
                                <TableSelectRow
                                  {...getSelectionProps({ row })}
                                  onChange={() => {
                                    const sampleId = parseInt(row.id);
                                    setSelectedDisposalSampleIds((prev) =>
                                      prev.includes(sampleId)
                                        ? prev.filter((id) => id !== sampleId)
                                        : [...prev, sampleId],
                                    );
                                  }}
                                  checked={selectedDisposalSampleIds.includes(
                                    parseInt(row.id),
                                  )}
                                />
                                {row.cells.map((cell) => (
                                  <TableCell key={cell.id}>
                                    {cell.info.header === "disposalStatus" ? (
                                      <Tag
                                        type={getDisposalStatusTagType(
                                          cell.value,
                                        )}
                                        size="sm"
                                      >
                                        {cell.value}
                                      </Tag>
                                    ) : cell.info.header ===
                                      "recommendedMethod" ? (
                                      <Tag type="outline" size="sm">
                                        {cell.value}
                                      </Tag>
                                    ) : (
                                      cell.value
                                    )}
                                  </TableCell>
                                ))}
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
                      </TableContainer>
                    )}
                  </DataTable>
                ) : (
                  <p className="empty-state">
                    <FormattedMessage
                      id="medlab.disposal.noSamples"
                      defaultMessage="No samples pending disposal."
                    />
                  </p>
                )}

                {/* Disposal Action Button */}
                {selectedDisposalSampleIds.length > 0 && (
                  <div style={{ marginTop: "1rem" }}>
                    <Button
                      kind="danger"
                      renderIcon={TrashCan}
                      onClick={() => setDisposalModalOpen(true)}
                    >
                      <FormattedMessage
                        id="medlab.disposal.recordDisposal"
                        defaultMessage="Record Disposal ({count})"
                        values={{ count: selectedDisposalSampleIds.length }}
                      />
                    </Button>
                  </div>
                )}

                {/* Disposal Records */}
                {disposalRecords.length > 0 && (
                  <div style={{ marginTop: "2rem" }}>
                    <h5>
                      <FormattedMessage
                        id="medlab.disposal.records"
                        defaultMessage="Disposal Records"
                      />
                    </h5>
                    <StructuredListWrapper>
                      <StructuredListHead>
                        <StructuredListRow head>
                          <StructuredListCell head>Lab No</StructuredListCell>
                          <StructuredListCell head>Method</StructuredListCell>
                          <StructuredListCell head>Reason</StructuredListCell>
                          <StructuredListCell head>Date</StructuredListCell>
                          <StructuredListCell head>
                            Responsible
                          </StructuredListCell>
                        </StructuredListRow>
                      </StructuredListHead>
                      <StructuredListBody>
                        {disposalRecords.slice(0, 10).map((record, idx) => (
                          <StructuredListRow key={idx}>
                            <StructuredListCell>
                              {record.labNo}
                            </StructuredListCell>
                            <StructuredListCell>
                              <Tag type="red" size="sm">
                                {record.disposalMethod}
                              </Tag>
                            </StructuredListCell>
                            <StructuredListCell>
                              {record.disposalReason}
                            </StructuredListCell>
                            <StructuredListCell>
                              {record.disposalDate}
                            </StructuredListCell>
                            <StructuredListCell>
                              {record.responsiblePerson}
                            </StructuredListCell>
                          </StructuredListRow>
                        ))}
                      </StructuredListBody>
                    </StructuredListWrapper>
                  </div>
                )}
              </div>
            </TabPanel>

            {/* Archiving Tab */}
            <TabPanel>
              <div style={{ padding: "1rem 0" }}>
                <h5>
                  <FormattedMessage
                    id="medlab.archiving.samplesForArchiving"
                    defaultMessage="Samples Available for Archiving"
                  />
                </h5>
                {disposalSamples.filter((s) => s.disposalStatus === "PENDING")
                  .length > 0 ? (
                  <DataTable
                    rows={disposalSamples
                      .filter((s) => s.disposalStatus === "PENDING")
                      .map((s, idx) => ({
                        ...s,
                        id: s.sampleItemId?.toString() || idx.toString(),
                      }))}
                    headers={[
                      {
                        key: "labNo",
                        header: intl.formatMessage({
                          id: "sample.label.labnumber",
                          defaultMessage: "Lab No",
                        }),
                      },
                      {
                        key: "sampleType",
                        header: intl.formatMessage({
                          id: "sample.sampleType",
                          defaultMessage: "Sample Type",
                        }),
                      },
                      {
                        key: "patientName",
                        header: intl.formatMessage({
                          id: "patient.label",
                          defaultMessage: "Patient",
                        }),
                      },
                    ]}
                    size="md"
                  >
                    {({
                      rows,
                      headers,
                      getHeaderProps,
                      getRowProps,
                      getTableProps,
                      getSelectionProps,
                    }) => (
                      <TableContainer>
                        <Table {...getTableProps()}>
                          <TableHead>
                            <TableRow>
                              <TableSelectAll {...getSelectionProps()} />
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
                            {rows.map((row) => (
                              <TableRow key={row.id} {...getRowProps({ row })}>
                                <TableSelectRow
                                  {...getSelectionProps({ row })}
                                  onChange={() => {
                                    const sampleId = parseInt(row.id);
                                    setSelectedDisposalSampleIds((prev) =>
                                      prev.includes(sampleId)
                                        ? prev.filter((id) => id !== sampleId)
                                        : [...prev, sampleId],
                                    );
                                  }}
                                  checked={selectedDisposalSampleIds.includes(
                                    parseInt(row.id),
                                  )}
                                />
                                {row.cells.map((cell) => (
                                  <TableCell key={cell.id}>
                                    {cell.value}
                                  </TableCell>
                                ))}
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
                      </TableContainer>
                    )}
                  </DataTable>
                ) : (
                  <p className="empty-state">
                    <FormattedMessage
                      id="medlab.archiving.noSamples"
                      defaultMessage="No samples pending archiving."
                    />
                  </p>
                )}

                {/* Archiving Action Button */}
                {selectedDisposalSampleIds.length > 0 && (
                  <div style={{ marginTop: "1rem" }}>
                    <Button
                      kind="primary"
                      renderIcon={Archive}
                      onClick={() => setArchivingModalOpen(true)}
                    >
                      <FormattedMessage
                        id="medlab.archiving.recordArchiving"
                        defaultMessage="Archive Samples ({count})"
                        values={{ count: selectedDisposalSampleIds.length }}
                      />
                    </Button>
                  </div>
                )}

                {/* Archiving Records */}
                {archivingRecords.length > 0 && (
                  <div style={{ marginTop: "2rem" }}>
                    <h5>
                      <FormattedMessage
                        id="medlab.archiving.records"
                        defaultMessage="Archiving Records"
                      />
                    </h5>
                    <StructuredListWrapper>
                      <StructuredListHead>
                        <StructuredListRow head>
                          <StructuredListCell head>Lab No</StructuredListCell>
                          <StructuredListCell head>
                            Retention
                          </StructuredListCell>
                          <StructuredListCell head>
                            Condition
                          </StructuredListCell>
                          <StructuredListCell head>Biobank</StructuredListCell>
                          <StructuredListCell head>Archived</StructuredListCell>
                        </StructuredListRow>
                      </StructuredListHead>
                      <StructuredListBody>
                        {archivingRecords.slice(0, 10).map((record, idx) => (
                          <StructuredListRow key={idx}>
                            <StructuredListCell>
                              {record.labNo}
                            </StructuredListCell>
                            <StructuredListCell>
                              {record.retentionYears} years
                            </StructuredListCell>
                            <StructuredListCell>
                              <Tag type="blue" size="sm">
                                {record.storageCondition}
                              </Tag>
                            </StructuredListCell>
                            <StructuredListCell>
                              {record.transferToBiobank ? (
                                <Tag type="green" size="sm">
                                  Yes
                                </Tag>
                              ) : (
                                <Tag type="gray" size="sm">
                                  No
                                </Tag>
                              )}
                            </StructuredListCell>
                            <StructuredListCell>
                              {record.archivedAt}
                            </StructuredListCell>
                          </StructuredListRow>
                        ))}
                      </StructuredListBody>
                    </StructuredListWrapper>
                  </div>
                )}
              </div>
            </TabPanel>

            {/* Accreditation Tab */}
            <TabPanel>
              <div style={{ padding: "1rem 0" }}>
                {/* SOP Compliance Summary */}
                <Tile style={{ marginBottom: "1rem" }}>
                  <h5>
                    <Document size={20} style={{ marginRight: "0.5rem" }} />
                    <FormattedMessage
                      id="medlab.accreditation.sopCompliance"
                      defaultMessage="SOP Compliance Status"
                    />
                  </h5>
                  {sopCompliance && (
                    <div
                      style={{
                        display: "flex",
                        gap: "1rem",
                        marginTop: "0.5rem",
                      }}
                    >
                      <Tag type={sopCompliance.allCompliant ? "green" : "red"}>
                        {sopCompliance.allCompliant
                          ? "All SOPs Compliant"
                          : "SOPs Pending"}
                      </Tag>
                      <span>
                        {sopCompliance.completedCount || 0} /{" "}
                        {sopCompliance.totalCount || 0} completed
                      </span>
                    </div>
                  )}
                </Tile>

                {/* Audit Trail */}
                <Accordion>
                  <AccordionItem
                    title={intl.formatMessage({
                      id: "medlab.accreditation.auditTrail",
                      defaultMessage: "Audit Trail",
                    })}
                  >
                    {auditTrail.length > 0 ? (
                      <StructuredListWrapper>
                        <StructuredListHead>
                          <StructuredListRow head>
                            <StructuredListCell head>
                              Timestamp
                            </StructuredListCell>
                            <StructuredListCell head>Action</StructuredListCell>
                            <StructuredListCell head>Page</StructuredListCell>
                            <StructuredListCell head>Sample</StructuredListCell>
                            <StructuredListCell head>User</StructuredListCell>
                          </StructuredListRow>
                        </StructuredListHead>
                        <StructuredListBody>
                          {auditTrail.slice(0, 20).map((entry, idx) => (
                            <StructuredListRow key={idx}>
                              <StructuredListCell>
                                {entry.timestamp}
                              </StructuredListCell>
                              <StructuredListCell>
                                <Tag type="outline" size="sm">
                                  {entry.action}
                                </Tag>
                              </StructuredListCell>
                              <StructuredListCell>
                                {entry.pageTitle}
                              </StructuredListCell>
                              <StructuredListCell>
                                {entry.labNo}
                              </StructuredListCell>
                              <StructuredListCell>
                                {entry.userName}
                              </StructuredListCell>
                            </StructuredListRow>
                          ))}
                        </StructuredListBody>
                      </StructuredListWrapper>
                    ) : (
                      <p className="empty-state">No audit trail entries.</p>
                    )}
                  </AccordionItem>
                </Accordion>

                {/* Finalize Button */}
                <div style={{ marginTop: "2rem" }}>
                  <Button
                    kind="danger"
                    renderIcon={DocumentExport}
                    onClick={() => setFinalizeModalOpen(true)}
                    disabled={!canMedLabFinalize}
                  >
                    <FormattedMessage
                      id="notebook.archive.finalize"
                      defaultMessage="Finalize Notebook"
                    />
                  </Button>
                  {!canMedLabFinalize && (
                    <p
                      className="action-helper-text"
                      style={{ marginTop: "0.5rem", color: "#6f6f6f" }}
                    >
                      <FormattedMessage
                        id="medlab.finalize.requirements"
                        defaultMessage="All samples must be disposed/archived and SOPs must be compliant before finalization."
                      />
                    </p>
                  )}
                </div>
              </div>
            </TabPanel>
          </TabPanels>
        </Tabs>

        {/* Disposal Modal */}
        <Modal
          open={disposalModalOpen}
          onRequestClose={() => setDisposalModalOpen(false)}
          modalHeading={intl.formatMessage({
            id: "medlab.disposal.modalTitle",
            defaultMessage: "Record Sample Disposal",
          })}
          primaryButtonText={intl.formatMessage({
            id: "medlab.disposal.confirm",
            defaultMessage: "Confirm Disposal",
          })}
          secondaryButtonText={intl.formatMessage({
            id: "label.button.cancel",
          })}
          onRequestSubmit={handleRecordDisposal}
          primaryButtonDisabled={disposing}
          size="md"
        >
          {disposing && <Loading withOverlay />}
          <Grid>
            <Column lg={8} md={4} sm={4}>
              <Select
                id="disposal-reason"
                labelText={intl.formatMessage({
                  id: "medlab.disposal.reason",
                  defaultMessage: "Disposal Reason",
                })}
                value={disposalReason}
                onChange={(e) => setDisposalReason(e.target.value)}
              >
                {disposalReasons.map((r) => (
                  <SelectItem key={r.value} value={r.value} text={r.label} />
                ))}
              </Select>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <Select
                id="disposal-method"
                labelText={intl.formatMessage({
                  id: "medlab.disposal.method",
                  defaultMessage: "Disposal Method",
                })}
                value={disposalMethod}
                onChange={(e) => setDisposalMethod(e.target.value)}
              >
                {disposalMethods.map((m) => (
                  <SelectItem key={m.value} value={m.value} text={m.label} />
                ))}
              </Select>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="disposal-date"
                labelText={intl.formatMessage({
                  id: "medlab.disposal.date",
                  defaultMessage: "Disposal Date",
                })}
                type="date"
                value={disposalDate}
                onChange={(e) => setDisposalDate(e.target.value)}
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="responsible-person"
                labelText={intl.formatMessage({
                  id: "medlab.disposal.responsiblePerson",
                  defaultMessage: "Responsible Person",
                })}
                value={responsiblePerson}
                onChange={(e) => setResponsiblePerson(e.target.value)}
              />
            </Column>
            <Column lg={16} md={8} sm={4}>
              <TextInput
                id="facility-details"
                labelText={intl.formatMessage({
                  id: "medlab.disposal.facilityDetails",
                  defaultMessage: "Licensed Disposal Facility Details",
                })}
                value={facilityDetails}
                onChange={(e) => setFacilityDetails(e.target.value)}
              />
            </Column>
            <Column lg={16} md={8} sm={4}>
              <TextArea
                id="disposal-notes"
                labelText={intl.formatMessage({
                  id: "medlab.disposal.notes",
                  defaultMessage: "Notes",
                })}
                value={disposalNotes}
                onChange={(e) => setDisposalNotes(e.target.value)}
                rows={3}
              />
            </Column>
          </Grid>

          {/* Disposal Rules Info */}
          <InlineNotification
            kind="info"
            title={intl.formatMessage({
              id: "medlab.disposal.rules.title",
              defaultMessage: "Disposal Method Rules",
            })}
            subtitle={intl.formatMessage({
              id: "medlab.disposal.rules.subtitle",
              defaultMessage:
                "Blood & stool → Incineration | Urine & analyzer waste → Chemical treatment",
            })}
            hideCloseButton
            lowContrast
            style={{ marginTop: "1rem" }}
          />
        </Modal>

        {/* Archiving Modal */}
        <Modal
          open={archivingModalOpen}
          onRequestClose={() => setArchivingModalOpen(false)}
          modalHeading={intl.formatMessage({
            id: "medlab.archiving.modalTitle",
            defaultMessage: "Archive Samples",
          })}
          primaryButtonText={intl.formatMessage({
            id: "medlab.archiving.confirm",
            defaultMessage: "Confirm Archiving",
          })}
          secondaryButtonText={intl.formatMessage({
            id: "label.button.cancel",
          })}
          onRequestSubmit={handleRecordArchiving}
          primaryButtonDisabled={archiving}
          size="md"
        >
          {archiving && <Loading withOverlay />}
          <Grid>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="retention-years"
                labelText={intl.formatMessage({
                  id: "medlab.archiving.retentionYears",
                  defaultMessage: "Retention Period (Years)",
                })}
                type="number"
                value={retentionYears}
                onChange={(e) =>
                  setRetentionYears(parseInt(e.target.value) || 2)
                }
                min={1}
                max={25}
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <Select
                id="storage-condition"
                labelText={intl.formatMessage({
                  id: "medlab.archiving.storageCondition",
                  defaultMessage: "Storage Condition",
                })}
                value={storageCondition}
                onChange={(e) => setStorageCondition(e.target.value)}
              >
                {storageConditions.map((c) => (
                  <SelectItem key={c.value} value={c.value} text={c.label} />
                ))}
              </Select>
            </Column>
            <Column lg={16} md={8} sm={4}>
              <Checkbox
                id="transfer-biobank"
                labelText={intl.formatMessage({
                  id: "medlab.archiving.transferToBiobank",
                  defaultMessage: "Transfer to Biobank/Biorepository",
                })}
                checked={transferToBiobank}
                onChange={(_, { checked }) => setTransferToBiobank(checked)}
              />
            </Column>
            {transferToBiobank && (
              <Column lg={16} md={8} sm={4}>
                <TextInput
                  id="biobank-details"
                  labelText={intl.formatMessage({
                    id: "medlab.archiving.biobankDetails",
                    defaultMessage: "Biobank/Biorepository Details",
                  })}
                  value={biobankDetails}
                  onChange={(e) => setBiobankDetails(e.target.value)}
                />
              </Column>
            )}
            <Column lg={16} md={8} sm={4}>
              <TextArea
                id="archive-notes"
                labelText={intl.formatMessage({
                  id: "medlab.archiving.notes",
                  defaultMessage: "Notes",
                })}
                value={archiveNotes}
                onChange={(e) => setArchiveNotes(e.target.value)}
                rows={3}
              />
            </Column>
          </Grid>

          {/* Bioequivalence Info */}
          <InlineNotification
            kind="info"
            title={intl.formatMessage({
              id: "medlab.archiving.bioequivalence.title",
              defaultMessage: "Bioequivalence Retention",
            })}
            subtitle={intl.formatMessage({
              id: "medlab.archiving.bioequivalence.subtitle",
              defaultMessage:
                "Default retention for bioequivalence studies: 2 years at −80°C",
            })}
            hideCloseButton
            lowContrast
            style={{ marginTop: "1rem" }}
          />
        </Modal>

        {/* Finalize Modal (MedLab) */}
        <Modal
          open={finalizeModalOpen}
          onRequestClose={() => {
            setFinalizeModalOpen(false);
            setConfirmFinalize(false);
          }}
          modalHeading={intl.formatMessage({
            id: "notebook.archive.finalizeModal.title",
            defaultMessage: "Finalize Notebook",
          })}
          primaryButtonText={intl.formatMessage({
            id: "notebook.archive.finalize",
            defaultMessage: "Finalize",
          })}
          secondaryButtonText={intl.formatMessage({
            id: "label.button.cancel",
          })}
          onRequestSubmit={handleMedLabFinalize}
          primaryButtonDisabled={!confirmFinalize || finalizing}
          danger
        >
          {finalizing && <Loading withOverlay />}
          <InlineNotification
            kind="warning"
            title={intl.formatMessage({
              id: "notebook.archive.finalizeWarning.title",
              defaultMessage: "This action is irreversible",
            })}
            subtitle={intl.formatMessage({
              id: "notebook.archive.finalizeWarning.subtitle",
              defaultMessage:
                "Once finalized, the notebook cannot be modified. Metadata will be permanently archived.",
            })}
            hideCloseButton
            lowContrast
          />
          <div style={{ marginTop: "1rem" }}>
            <h5>
              <FormattedMessage
                id="notebook.archive.finalizeSummary"
                defaultMessage="Finalization Summary"
              />
            </h5>
            <ul>
              <li>Total Samples: {summary?.totalCount || 0}</li>
              <li>Disposed: {summary?.disposedCount || 0}</li>
              <li>Archived: {summary?.archivedCount || 0}</li>
              <li>Pending: {summary?.pendingCount || 0}</li>
            </ul>
          </div>
          <Checkbox
            id="confirm-finalize"
            labelText={intl.formatMessage({
              id: "notebook.archive.confirmFinalize",
              defaultMessage:
                "I confirm that all samples have been properly disposed/archived. I understand this action cannot be undone.",
            })}
            checked={confirmFinalize}
            onChange={(_, { checked }) => setConfirmFinalize(checked)}
            style={{ marginTop: "1rem" }}
          />
        </Modal>
      </div>
    );
  }

  // ==================== Immunology Workflow Render (Original) ====================
  return (
    <div className="notebook-page archiving-page">
      <Grid>
        {/* Header */}
        <Column lg={16} md={8} sm={4}>
          <div className="page-header">
            <h3>
              <Archive size={24} />
              <FormattedMessage
                id="notebook.archive.title"
                defaultMessage="End of Project Archiving"
              />
            </h3>
            <p className="page-description">
              <FormattedMessage
                id="notebook.archive.description"
                defaultMessage="Transfer samples to biorepository and finalize the notebook with complete traceability verification."
              />
            </p>
          </div>
        </Column>

        {/* Notifications */}
        {error && (
          <Column lg={16} md={8} sm={4}>
            <InlineNotification
              kind="error"
              title={intl.formatMessage({
                id: "error",
                defaultMessage: "Error",
              })}
              subtitle={error}
              onCloseButtonClick={() => setError(null)}
            />
          </Column>
        )}

        {success && (
          <Column lg={16} md={8} sm={4}>
            <InlineNotification
              kind="success"
              title={intl.formatMessage({
                id: "success",
                defaultMessage: "Success",
              })}
              subtitle={success}
              onCloseButtonClick={() => setSuccess(null)}
            />
          </Column>
        )}

        {/* Progress Summary */}
        <Column lg={8} md={4} sm={4}>
          <Tile className="archiving-progress-tile">
            <h4>
              <FormattedMessage
                id="notebook.archive.progress"
                defaultMessage="Archiving Progress"
              />
            </h4>
            {archivingProgress && (
              <>
                <ProgressBar
                  value={archivingProgress.percentComplete || 0}
                  max={100}
                  label={`${Math.round(archivingProgress.percentComplete || 0)}%`}
                  helperText={intl.formatMessage(
                    {
                      id: "notebook.archive.progressHelper",
                      defaultMessage: "{archived} of {total} samples archived",
                    },
                    {
                      archived: archivingProgress.archivedSamples || 0,
                      total: archivingProgress.totalSamples || 0,
                    },
                  )}
                />
                <div className="progress-details">
                  <div className="progress-item">
                    <Tag type="blue">
                      <FormattedMessage
                        id="notebook.archive.parentSamples"
                        defaultMessage="Parent Samples"
                      />
                    </Tag>
                    <span>
                      {archivingProgress.archivedParents || 0} /{" "}
                      {archivingProgress.parentSamples || 0}
                    </span>
                  </div>
                  <div className="progress-item">
                    <Tag type="teal">
                      <FormattedMessage
                        id="notebook.archive.childSamples"
                        defaultMessage="Child Samples"
                      />
                    </Tag>
                    <span>
                      {archivingProgress.archivedChildren || 0} /{" "}
                      {archivingProgress.childSamples || 0}
                    </span>
                  </div>
                </div>
              </>
            )}
          </Tile>
        </Column>

        {/* Actions */}
        <Column lg={8} md={4} sm={4}>
          <Tile className="archiving-actions-tile">
            <h4>
              <FormattedMessage
                id="notebook.archive.actions"
                defaultMessage="Actions"
              />
            </h4>
            <div className="action-buttons">
              <Button
                kind="secondary"
                onClick={handleVerifyTraceability}
                disabled={verifyingTraceability}
                renderIcon={CheckmarkFilled}
              >
                <FormattedMessage
                  id="notebook.archive.verifyTraceability"
                  defaultMessage="Verify Traceability"
                />
              </Button>
              <Button
                kind="primary"
                onClick={() => setTransferModalOpen(true)}
                disabled={
                  archivableSamples.parent.length === 0 &&
                  archivableSamples.child.length === 0
                }
                renderIcon={Archive}
              >
                <FormattedMessage
                  id="notebook.archive.transferToBiorepository"
                  defaultMessage="Transfer to Biorepository"
                />
              </Button>
              <Button
                kind="danger"
                onClick={() => setFinalizeModalOpen(true)}
                disabled={!canFinalize}
                renderIcon={DocumentExport}
              >
                <FormattedMessage
                  id="notebook.archive.finalize"
                  defaultMessage="Finalize Notebook"
                />
              </Button>
            </div>
            {!canFinalize && traceabilityResult && (
              <p className="action-helper-text">
                <FormattedMessage
                  id="notebook.archive.cannotFinalize"
                  defaultMessage="Resolve traceability issues before finalizing."
                />
              </p>
            )}
          </Tile>
        </Column>

        {/* Traceability Checklist */}
        <Column lg={16} md={8} sm={4}>
          <TraceabilityChecklist
            traceabilityResult={traceabilityResult}
            loading={verifyingTraceability}
          />
        </Column>

        {/* Sample List */}
        <Column lg={16} md={8} sm={4}>
          <Tile className="samples-tile">
            <h4>
              <FormattedMessage
                id="notebook.archive.samplesList"
                defaultMessage="Samples Pending Archive"
              />
            </h4>
            {samples.length > 0 ? (
              <StructuredListWrapper>
                <StructuredListHead>
                  <StructuredListRow head>
                    <StructuredListCell head>
                      <FormattedMessage
                        id="notebook.sample.id"
                        defaultMessage="Sample ID"
                      />
                    </StructuredListCell>
                    <StructuredListCell head>
                      <FormattedMessage
                        id="notebook.sample.type"
                        defaultMessage="Type"
                      />
                    </StructuredListCell>
                  </StructuredListRow>
                </StructuredListHead>
                <StructuredListBody>
                  {samples.slice(0, 20).map((sample, index) => (
                    <StructuredListRow key={index}>
                      <StructuredListCell>
                        {sample.sampleItemId}
                      </StructuredListCell>
                      <StructuredListCell>
                        <Tag type={sample.type === "parent" ? "blue" : "teal"}>
                          {sample.type === "parent" ? "Parent" : "Child"}
                        </Tag>
                      </StructuredListCell>
                    </StructuredListRow>
                  ))}
                </StructuredListBody>
              </StructuredListWrapper>
            ) : (
              <p className="no-samples-message">
                <FormattedMessage
                  id="notebook.archive.noSamplesPending"
                  defaultMessage="All samples have been archived."
                />
              </p>
            )}
            {samples.length > 20 && (
              <p className="samples-truncated">
                <FormattedMessage
                  id="notebook.archive.moreSamples"
                  defaultMessage="... and {count} more samples"
                  values={{ count: samples.length - 20 }}
                />
              </p>
            )}
          </Tile>
        </Column>
      </Grid>

      {/* Transfer Modal */}
      <Modal
        open={transferModalOpen}
        onRequestClose={() => setTransferModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.archive.transferModal.title",
          defaultMessage: "Transfer to Biorepository",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.archive.transfer",
          defaultMessage: "Transfer",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleTransferToBiorepository}
        primaryButtonDisabled={!selectedDevice || transferring}
      >
        {transferring && <Loading withOverlay />}
        <div className="transfer-form">
          <p>
            <FormattedMessage
              id="notebook.archive.transferModal.description"
              defaultMessage="Select the biorepository location for permanent sample storage."
            />
          </p>
          <Dropdown
            id="room-select"
            titleText={intl.formatMessage({
              id: "notebook.storage.room",
              defaultMessage: "Room",
            })}
            label={intl.formatMessage({
              id: "notebook.storage.selectRoom",
              defaultMessage: "Select a room",
            })}
            items={rooms}
            itemToString={(item) => (item ? item.label : "")}
            selectedItem={selectedRoom}
            onChange={handleRoomChange}
          />
          <Dropdown
            id="device-select"
            titleText={intl.formatMessage({
              id: "notebook.storage.device",
              defaultMessage: "Device / Unit",
            })}
            label={intl.formatMessage({
              id: "notebook.storage.selectDevice",
              defaultMessage: "Select a device",
            })}
            items={devices}
            itemToString={(item) => (item ? item.label : "")}
            selectedItem={selectedDevice}
            onChange={({ selectedItem }) => setSelectedDevice(selectedItem)}
            disabled={!selectedRoom}
          />
          <TextInput
            id="transfer-notes"
            labelText={intl.formatMessage({
              id: "notebook.archive.transferNotes",
              defaultMessage: "Transfer Notes",
            })}
            placeholder={intl.formatMessage({
              id: "notebook.archive.transferNotesPlaceholder",
              defaultMessage: "Optional notes about this transfer",
            })}
            value={transferNotes}
            onChange={(e) => setTransferNotes(e.target.value)}
          />
          <p className="transfer-count">
            <FormattedMessage
              id="notebook.archive.transferCount"
              defaultMessage="{count} samples will be transferred"
              values={{
                count:
                  selectedSampleIds.length > 0
                    ? selectedSampleIds.length
                    : archivableSamples.parent.length +
                      archivableSamples.child.length,
              }}
            />
          </p>
        </div>
      </Modal>

      {/* Finalize Modal */}
      <Modal
        open={finalizeModalOpen}
        onRequestClose={() => {
          setFinalizeModalOpen(false);
          setConfirmFinalize(false);
        }}
        modalHeading={intl.formatMessage({
          id: "notebook.archive.finalizeModal.title",
          defaultMessage: "Finalize Notebook",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.archive.finalize",
          defaultMessage: "Finalize",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleFinalize}
        primaryButtonDisabled={!confirmFinalize || finalizing}
        danger
      >
        {finalizing && <Loading withOverlay />}
        <div className="finalize-form">
          <InlineNotification
            kind="warning"
            title={intl.formatMessage({
              id: "notebook.archive.finalizeWarning.title",
              defaultMessage: "This action is irreversible",
            })}
            subtitle={intl.formatMessage({
              id: "notebook.archive.finalizeWarning.subtitle",
              defaultMessage:
                "Once finalized, the notebook cannot be modified. All samples must be archived and traceability verified.",
            })}
            hideCloseButton
            lowContrast
          />
          <div className="finalize-summary">
            <h5>
              <FormattedMessage
                id="notebook.archive.finalizeSummary"
                defaultMessage="Finalization Summary"
              />
            </h5>
            <ul>
              <li>
                <FormattedMessage
                  id="notebook.archive.totalSamples"
                  defaultMessage="Total Samples: {count}"
                  values={{ count: archivingProgress?.totalSamples || 0 }}
                />
              </li>
              <li>
                <FormattedMessage
                  id="notebook.archive.archivedSamples"
                  defaultMessage="Archived Samples: {count}"
                  values={{ count: archivingProgress?.archivedSamples || 0 }}
                />
              </li>
              <li>
                <FormattedMessage
                  id="notebook.archive.traceabilityStatus"
                  defaultMessage="Traceability: {status}"
                  values={{
                    status: traceabilityResult?.passed
                      ? "Verified"
                      : "Not Verified",
                  }}
                />
              </li>
            </ul>
          </div>
          <Checkbox
            id="confirm-finalize"
            labelText={intl.formatMessage({
              id: "notebook.archive.confirmFinalize",
              defaultMessage:
                "I confirm that all samples have been properly archived and traceability has been verified. I understand this action cannot be undone.",
            })}
            checked={confirmFinalize}
            onChange={(_, { checked }) => setConfirmFinalize(checked)}
          />
        </div>
      </Modal>
    </div>
  );
}

export default EndOfProjectArchivingPage;
