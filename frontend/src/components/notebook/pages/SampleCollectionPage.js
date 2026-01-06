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
  Tag,
  InlineNotification,
  Loading,
  Modal,
  TextInput,
  Select,
  SelectItem,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableSelectAll,
  TableSelectRow,
  TableToolbar,
  TableToolbarContent,
  TableBatchActions,
  TableBatchAction,
  TextArea,
} from "@carbon/react";
import {
  Upload,
  Chemistry,
  CheckmarkFilled,
  Link as LinkIcon,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer, postToOpenElisServer } from "../../utils/Utils";
import { NotificationContext, ConfigurationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";
import CustomDatePicker from "../../common/CustomDatePicker";
import ManifestImportModal from "../workflow/ManifestImportModal";
import LinkPatientModal from "../workflow/LinkPatientModal";
import "../workflow/NotebookWorkflow.css";

/**
 * SampleCollectionPage - Page 2 of the MedLab workflow.
 * Handles sample collection: importing samples from manifest or collecting from existing orders.
 * Similar to SampleReceptionPage with manifest import and bulk actions support.
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function SampleCollectionPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext);

  // State for samples
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Modal state
  const [importModalOpen, setImportModalOpen] = useState(false);
  const [collectionModalOpen, setCollectionModalOpen] = useState(false);
  const [bulkCollectionModalOpen, setBulkCollectionModalOpen] = useState(false);
  const [linkPatientModalOpen, setLinkPatientModalOpen] = useState(false);
  const [selectedOrder, setSelectedOrder] = useState(null);

  // Collection form state
  const [collectionForm, setCollectionForm] = useState({
    sampleType: "",
    containerType: "",
    collectionTime: "",
    collectionDate: "",
    collectorId: "",
    volume: "",
    notes: "",
  });

  // Available container types
  const [containerTypes] = useState([
    { id: "EDTA", name: "EDTA Tube (Purple)" },
    { id: "SST", name: "SST Tube (Gold/Red)" },
    { id: "HEPARIN", name: "Heparin Tube (Green)" },
    { id: "CITRATE", name: "Citrate Tube (Blue)" },
    { id: "PLAIN", name: "Plain Tube (Red)" },
    { id: "URINE_CUP", name: "Urine Cup" },
    { id: "STOOL_CONTAINER", name: "Stool Container" },
    { id: "SWAB", name: "Swab" },
    { id: "CRYOVIAL", name: "Cryovial" },
  ]);

  // Load data on mount
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

    // Use page-specific endpoint to get samples with their page status
    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            // Transform samples for the grid
            // Patient info is stored in sample.data field from Link to Patient feature
            const transformedSamples = response.map((sample) => ({
              id: String(sample.id || sample.sampleItemId),
              externalId: sample.externalId,
              accessionNumber: sample.accessionNumber,
              sampleType: sample.sampleType || sample.typeOfSample?.description,
              collectionDate: sample.collectionDate,
              status: sample.pageStatus || "PENDING",
              patientName: sample.data?.patientName || sample.patientName || "",
              patientId: sample.data?.patientId || "",
              patientNationalId: sample.data?.patientNationalId || "",
              volume: sample.volume,
              containerType: sample.containerType,
              labNo: sample.labNo || sample.accessionNumber,
              data: sample.data, // Preserve full data for other uses
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

  // Handle manifest import success
  const handleImportSuccess = useCallback(
    (result) => {
      setImportModalOpen(false);
      loadPageSamples();
      if (onProgressUpdate) {
        onProgressUpdate();
      }
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage(
          {
            id: "medlab.collection.import.success",
            defaultMessage: "Successfully imported {count} samples",
          },
          { count: result?.samplesCreated || 0 },
        ),
        kind: NotificationKinds.success,
      });
      setNotificationVisible(true);
    },
    [
      loadPageSamples,
      onProgressUpdate,
      intl,
      addNotification,
      setNotificationVisible,
    ],
  );

  // Check if page has a real database ID (not a default synthetic ID)
  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Handle bulk mark as collected
  const handleBulkMarkCollected = useCallback(() => {
    if (selectedSampleIds.length === 0) return;

    // Require real page ID for bulk actions
    if (!hasRealPageId) {
      setError(
        "Cannot mark samples: Page not properly initialized. Please refresh the page.",
      );
      return;
    }

    // Open bulk collection modal to gather collection details
    setBulkCollectionModalOpen(true);
  }, [selectedSampleIds, hasRealPageId]);

  // Submit bulk collection
  const handleSubmitBulkCollection = useCallback(() => {
    if (selectedSampleIds.length === 0) return;

    const bulkCollectionData = {
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      status: "COMPLETED",
      containerType: collectionForm.containerType,
      collectionDate:
        collectionForm.collectionDate ||
        configurationProperties?.currentDateAsText,
      collectionTime: collectionForm.collectionTime,
      collectorId: collectionForm.collectorId,
      volume: collectionForm.volume,
      notes: collectionForm.notes,
    };

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify(bulkCollectionData),
      (status) => {
        if (status === 200) {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage(
              {
                id: "medlab.collection.bulk.success",
                defaultMessage: "{count} samples marked as collected",
              },
              { count: selectedSampleIds.length },
            ),
            kind: NotificationKinds.success,
          });
          setNotificationVisible(true);
          setBulkCollectionModalOpen(false);
          setSelectedSampleIds([]);
          setCollectionForm({
            sampleType: "",
            containerType: "",
            collectionTime: "",
            collectionDate: "",
            collectorId: "",
            volume: "",
            notes: "",
          });
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "medlab.collection.bulk.error",
              defaultMessage: "Error marking samples as collected",
            }),
            kind: NotificationKinds.error,
          });
          setNotificationVisible(true);
        }
      },
    );
  }, [
    selectedSampleIds,
    collectionForm,
    pageData,
    configurationProperties,
    intl,
    addNotification,
    setNotificationVisible,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Handle individual sample status change
  const handleStatusChange = useCallback(
    (sampleId, newStatus) => {
      // Require real page ID for status changes
      if (!hasRealPageId) {
        setError(
          "Cannot update status: Page not properly initialized. Please refresh the page.",
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
    [pageData?.id, hasRealPageId, loadPageSamples, onProgressUpdate],
  );

  // Open collection modal for an individual sample
  const handleCollectSample = useCallback((sample) => {
    setSelectedOrder(sample);
    setCollectionForm({
      sampleType: sample.sampleType || "",
      containerType: sample.containerType || "",
      collectionTime: new Date().toLocaleTimeString("en-US", {
        hour: "2-digit",
        minute: "2-digit",
        hour12: false,
      }),
      collectionDate: "",
      collectorId: "",
      volume: sample.volume || "",
      notes: "",
    });
    setCollectionModalOpen(true);
  }, []);

  // Submit individual sample collection
  const handleSubmitCollection = useCallback(() => {
    if (!selectedOrder) return;

    const collectionData = {
      sampleIds: [parseInt(selectedOrder.id, 10)],
      status: "COMPLETED",
      containerType: collectionForm.containerType,
      collectionDate:
        collectionForm.collectionDate ||
        configurationProperties?.currentDateAsText,
      collectionTime: collectionForm.collectionTime,
      collectorId: collectionForm.collectorId,
      volume: collectionForm.volume,
      notes: collectionForm.notes,
    };

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify(collectionData),
      (status) => {
        if (status === 200) {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "medlab.collection.success",
              defaultMessage: "Sample collected successfully",
            }),
            kind: NotificationKinds.success,
          });
          setNotificationVisible(true);
          setCollectionModalOpen(false);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "medlab.collection.error",
              defaultMessage: "Error collecting sample",
            }),
            kind: NotificationKinds.error,
          });
          setNotificationVisible(true);
        }
      },
    );
  }, [
    selectedOrder,
    collectionForm,
    pageData,
    configurationProperties,
    intl,
    addNotification,
    setNotificationVisible,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Handle linking samples to patient
  const handleLinkPatient = useCallback(
    (patientId, patientInfo) => {
      if (selectedSampleIds.length === 0 || !patientId) return;

      // Require real page ID for linking
      if (!hasRealPageId) {
        setError(
          "Cannot link samples: Page not properly initialized. Please refresh the page.",
        );
        return;
      }

      // Store both patientId and patientName for display across all workflow pages
      const patientName = patientInfo
        ? `${patientInfo.firstName || ""} ${patientInfo.lastName || ""}`.trim()
        : "";

      const linkData = {
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        data: {
          patientId: patientId,
          patientName: patientName,
          patientNationalId: patientInfo?.nationalId || "",
        },
      };

      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
        JSON.stringify(linkData),
        (status) => {
          if (status === 200) {
            addNotification({
              title: intl.formatMessage({ id: "notification.title" }),
              message: intl.formatMessage(
                {
                  id: "medlab.linkPatient.success",
                  defaultMessage: "{count} sample(s) linked to patient {name}",
                },
                {
                  count: selectedSampleIds.length,
                  name: patientName || patientId,
                },
              ),
              kind: NotificationKinds.success,
            });
            setNotificationVisible(true);
            setLinkPatientModalOpen(false);
            setSelectedSampleIds([]);
            loadPageSamples();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            addNotification({
              title: intl.formatMessage({ id: "notification.title" }),
              message: intl.formatMessage({
                id: "medlab.linkPatient.error",
                defaultMessage: "Error linking samples to patient",
              }),
              kind: NotificationKinds.error,
            });
            setNotificationVisible(true);
          }
        },
      );
    },
    [
      selectedSampleIds,
      pageData,
      hasRealPageId,
      intl,
      addNotification,
      setNotificationVisible,
      loadPageSamples,
      onProgressUpdate,
    ],
  );

  // Calculate stats
  const collectedCount = samples.filter((s) => s.status === "COMPLETED").length;
  // Include IN_PROGRESS samples in pending count - these are samples that have been partially processed
  // (e.g., linked to a patient) but not yet collected
  const pendingCount = samples.filter(
    (s) => s.status === "PENDING" || s.status === "IN_PROGRESS",
  ).length;

  // Filter samples by status for display
  // Include IN_PROGRESS in pending - when data is applied (e.g., patient linked), status transitions
  // from PENDING to IN_PROGRESS, but sample still needs to be collected
  const pendingSamples = samples.filter(
    (s) => s.status === "PENDING" || s.status === "IN_PROGRESS",
  );
  const collectedSamples = samples.filter((s) => s.status === "COMPLETED");

  // Table headers for pending samples
  const pendingHeaders = [
    {
      key: "labNo",
      header: intl.formatMessage({ id: "sample.label.labnumber" }),
    },
    {
      key: "externalId",
      header: intl.formatMessage({
        id: "notebook.sample.externalId",
        defaultMessage: "External ID",
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
        id: "medlab.sample.patient",
        defaultMessage: "Patient",
      }),
    },
    {
      key: "collectionDate",
      header: intl.formatMessage({
        id: "notebook.sample.collectionDate",
        defaultMessage: "Collection Date",
      }),
    },
    {
      key: "actions",
      header: intl.formatMessage({ id: "label.button.actions" }),
    },
  ];

  // Render collection form fields (shared between individual and bulk modals)
  const renderCollectionFormFields = () => (
    <Grid>
      <Column lg={8} md={4} sm={4}>
        <Select
          id="container-type"
          labelText={intl.formatMessage({
            id: "medlab.collection.containerType",
            defaultMessage: "Container Type",
          })}
          value={collectionForm.containerType}
          onChange={(e) =>
            setCollectionForm((prev) => ({
              ...prev,
              containerType: e.target.value,
            }))
          }
        >
          <SelectItem value="" text="" />
          {containerTypes.map((type) => (
            <SelectItem key={type.id} value={type.id} text={type.name} />
          ))}
        </Select>
      </Column>
      <Column lg={8} md={4} sm={4}>
        <CustomDatePicker
          id="collection-date"
          labelText={intl.formatMessage({
            id: "medlab.collection.date",
            defaultMessage: "Collection Date",
          })}
          value={
            collectionForm.collectionDate ||
            configurationProperties?.currentDateAsText
          }
          onChange={(date) =>
            setCollectionForm((prev) => ({ ...prev, collectionDate: date }))
          }
          disallowFutureDate={true}
        />
      </Column>
      <Column lg={8} md={4} sm={4}>
        <TextInput
          id="collection-time"
          labelText={intl.formatMessage({
            id: "medlab.collection.time",
            defaultMessage: "Collection Time",
          })}
          value={collectionForm.collectionTime}
          onChange={(e) =>
            setCollectionForm((prev) => ({
              ...prev,
              collectionTime: e.target.value,
            }))
          }
          placeholder="HH:MM"
        />
      </Column>
      <Column lg={8} md={4} sm={4}>
        <TextInput
          id="collector-id"
          labelText={intl.formatMessage({
            id: "medlab.collection.collectorId",
            defaultMessage: "Collector ID",
          })}
          value={collectionForm.collectorId}
          onChange={(e) =>
            setCollectionForm((prev) => ({
              ...prev,
              collectorId: e.target.value,
            }))
          }
        />
      </Column>
      <Column lg={8} md={4} sm={4}>
        <TextInput
          id="volume"
          labelText={intl.formatMessage({
            id: "medlab.collection.volume",
            defaultMessage: "Volume (mL)",
          })}
          value={collectionForm.volume}
          onChange={(e) =>
            setCollectionForm((prev) => ({
              ...prev,
              volume: e.target.value,
            }))
          }
          type="number"
        />
      </Column>
      <Column lg={16} md={8} sm={4}>
        <TextArea
          id="notes"
          labelText={intl.formatMessage({
            id: "medlab.collection.notes",
            defaultMessage: "Notes",
          })}
          value={collectionForm.notes}
          onChange={(e) =>
            setCollectionForm((prev) => ({
              ...prev,
              notes: e.target.value,
            }))
          }
          rows={2}
        />
      </Column>
    </Grid>
  );

  return (
    <div className="sample-collection-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="medlab.page.sampleCollection.title"
            defaultMessage="Sample Collection"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="medlab.page.sampleCollection.description"
            defaultMessage="Import samples from a manifest or collect samples for pending orders. Mark samples as collected when complete."
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
                  id="medlab.page.sampleCollection.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="medlab.page.sampleCollection.collected"
                  defaultMessage="Collected"
                />
              </span>
              <span className="progress-value">{collectedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="medlab.page.sampleCollection.pending"
                  defaultMessage="Pending Collection"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Upload}
          onClick={() => setImportModalOpen(true)}
        >
          <FormattedMessage
            id="medlab.page.sampleCollection.importManifest"
            defaultMessage="Import from Manifest"
          />
        </Button>

        {selectedSampleIds.length > 0 && (
          <Button
            kind="secondary"
            size="sm"
            renderIcon={CheckmarkFilled}
            onClick={handleBulkMarkCollected}
          >
            <FormattedMessage
              id="medlab.page.sampleCollection.markCollected"
              defaultMessage="Mark Selected as Collected ({count})"
              values={{ count: selectedSampleIds.length }}
            />
          </Button>
        )}
      </div>

      {/* Error Display */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          hideCloseButton
          lowContrast
        />
      )}

      {/* Loading State */}
      {loading && <Loading />}

      {/* Pending Samples Table with Selection */}
      {!loading && pendingSamples.length > 0 && (
        <div className="orders-section">
          <h5>
            <FormattedMessage
              id="medlab.page.sampleCollection.pendingSamples"
              defaultMessage="Samples Pending Collection"
            />
          </h5>
          <DataTable
            rows={pendingSamples.map((s) => ({
              ...s,
              id: String(s.id),
              labNo: s.labNo || s.accessionNumber || "-",
              externalId: s.externalId || "-",
              sampleType: s.sampleType || "-",
              patientName: s.patientName || "-",
              collectionDate: s.collectionDate || "-",
            }))}
            headers={pendingHeaders}
            isSortable
          >
            {({
              rows,
              headers,
              getHeaderProps,
              getTableProps,
              getSelectionProps,
              getRowProps,
              selectedRows,
              getBatchActionProps,
            }) => {
              const batchActionProps = getBatchActionProps();
              return (
                <TableContainer>
                  <TableToolbar>
                    <TableBatchActions {...batchActionProps}>
                      <TableBatchAction
                        tabIndex={
                          batchActionProps.shouldShowBatchActions ? 0 : -1
                        }
                        renderIcon={CheckmarkFilled}
                        onClick={() => {
                          setSelectedSampleIds(selectedRows.map((r) => r.id));
                          setBulkCollectionModalOpen(true);
                        }}
                      >
                        <FormattedMessage
                          id="medlab.collection.bulk.collect"
                          defaultMessage="Mark as Collected"
                        />
                      </TableBatchAction>
                      <TableBatchAction
                        tabIndex={
                          batchActionProps.shouldShowBatchActions ? 0 : -1
                        }
                        renderIcon={LinkIcon}
                        onClick={() => {
                          setSelectedSampleIds(selectedRows.map((r) => r.id));
                          setLinkPatientModalOpen(true);
                        }}
                      >
                        <FormattedMessage
                          id="medlab.collection.bulk.linkPatient"
                          defaultMessage="Link to Patient"
                        />
                      </TableBatchAction>
                    </TableBatchActions>
                    <TableToolbarContent>
                      <p style={{ fontSize: "0.875rem", color: "#525252" }}>
                        <FormattedMessage
                          id="medlab.collection.bulk.hint"
                          defaultMessage="Select samples to perform bulk collection"
                        />
                      </p>
                    </TableToolbarContent>
                  </TableToolbar>
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
                      {rows.map((row) => {
                        const sample = pendingSamples.find(
                          (s) => String(s.id) === row.id,
                        );
                        return (
                          <TableRow key={row.id} {...getRowProps({ row })}>
                            <TableSelectRow {...getSelectionProps({ row })} />
                            {row.cells.map((cell) => (
                              <TableCell key={cell.id}>
                                {cell.info.header === "actions" ? (
                                  <Button
                                    kind="primary"
                                    size="sm"
                                    renderIcon={Chemistry}
                                    onClick={() => handleCollectSample(sample)}
                                  >
                                    <FormattedMessage
                                      id="medlab.collection.collect"
                                      defaultMessage="Collect"
                                    />
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
              );
            }}
          </DataTable>
        </div>
      )}

      {/* Collected Samples Table */}
      {!loading && collectedSamples.length > 0 && (
        <div className="orders-section" style={{ marginTop: "2rem" }}>
          <h5>
            <FormattedMessage
              id="medlab.page.sampleCollection.collectedSamples"
              defaultMessage="Collected Samples"
            />
          </h5>
          <DataTable
            rows={collectedSamples.map((s) => ({
              ...s,
              id: String(s.id),
              labNo: s.labNo || s.accessionNumber || "-",
              externalId: s.externalId || "-",
              sampleType: s.sampleType || "-",
              patientName: s.patientName || "-",
              containerType: s.containerType || "-",
              collectionDate: s.collectionDate || "-",
            }))}
            headers={[
              {
                key: "labNo",
                header: intl.formatMessage({ id: "sample.label.labnumber" }),
              },
              {
                key: "externalId",
                header: intl.formatMessage({
                  id: "notebook.sample.externalId",
                  defaultMessage: "External ID",
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
                  id: "medlab.sample.patient",
                  defaultMessage: "Patient",
                }),
              },
              {
                key: "containerType",
                header: intl.formatMessage({
                  id: "medlab.collection.containerType",
                  defaultMessage: "Container",
                }),
              },
              {
                key: "collectionDate",
                header: intl.formatMessage({
                  id: "medlab.collection.date",
                  defaultMessage: "Collection Date",
                }),
              },
            ]}
            isSortable
          >
            {({ rows, headers, getHeaderProps, getTableProps }) => (
              <TableContainer>
                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
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
                      <TableRow key={row.id}>
                        {row.cells.map((cell) => (
                          <TableCell key={cell.id}>{cell.value}</TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
        </div>
      )}

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="medlab.page.sampleCollection.empty"
              defaultMessage="No samples have been added to this notebook yet. Use the 'Import from Manifest' button above to import samples."
            />
          </p>
        </div>
      )}

      {/* Manifest Import Modal */}
      <ManifestImportModal
        open={importModalOpen}
        onClose={() => setImportModalOpen(false)}
        entryId={entryId}
        onImportSuccess={handleImportSuccess}
      />

      {/* Individual Collection Modal */}
      <Modal
        open={collectionModalOpen}
        onRequestClose={() => setCollectionModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "medlab.page.sampleCollection.collectModal",
          defaultMessage: "Collect Sample",
        })}
        primaryButtonText={intl.formatMessage({ id: "label.button.save" })}
        secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
        onRequestSubmit={handleSubmitCollection}
        size="md"
      >
        {selectedOrder && (
          <Tile className="order-info-tile" style={{ marginBottom: "1rem" }}>
            <strong>
              <FormattedMessage id="sample.label.labnumber" />:
            </strong>{" "}
            {selectedOrder.labNo || selectedOrder.accessionNumber}
            <br />
            <strong>
              <FormattedMessage
                id="notebook.sample.externalId"
                defaultMessage="External ID"
              />
              :
            </strong>{" "}
            {selectedOrder.externalId || "-"}
            <br />
            <strong>
              <FormattedMessage
                id="sample.sampleType"
                defaultMessage="Sample Type"
              />
              :
            </strong>{" "}
            {selectedOrder.sampleType || "-"}
          </Tile>
        )}
        {renderCollectionFormFields()}
      </Modal>

      {/* Bulk Collection Modal */}
      <Modal
        open={bulkCollectionModalOpen}
        onRequestClose={() => {
          setBulkCollectionModalOpen(false);
          setSelectedSampleIds([]);
        }}
        modalHeading={intl.formatMessage({
          id: "medlab.page.sampleCollection.bulkCollectModal",
          defaultMessage: "Bulk Sample Collection",
        })}
        primaryButtonText={intl.formatMessage({
          id: "medlab.collection.bulk.collect",
          defaultMessage: "Mark as Collected",
        })}
        secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
        onRequestSubmit={handleSubmitBulkCollection}
        size="md"
      >
        <Tile className="order-info-tile" style={{ marginBottom: "1rem" }}>
          <Tag type="blue">
            <FormattedMessage
              id="medlab.collection.bulk.selectedCount"
              defaultMessage="{count} samples selected"
              values={{ count: selectedSampleIds.length }}
            />
          </Tag>
        </Tile>
        <p style={{ marginBottom: "1rem", color: "#525252" }}>
          <FormattedMessage
            id="medlab.collection.bulk.description"
            defaultMessage="The following collection details will be applied to all selected samples."
          />
        </p>
        {renderCollectionFormFields()}
      </Modal>

      {/* Link Patient Modal */}
      <LinkPatientModal
        open={linkPatientModalOpen}
        onClose={() => {
          setLinkPatientModalOpen(false);
          setSelectedSampleIds([]);
        }}
        selectedSampleIds={selectedSampleIds}
        onLinkPatient={handleLinkPatient}
      />
    </div>
  );
}

export default SampleCollectionPage;
