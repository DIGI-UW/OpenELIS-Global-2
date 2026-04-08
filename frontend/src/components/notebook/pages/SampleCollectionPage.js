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
  Loading,
  Modal,
  TextInput,
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
} from "@carbon/react";
import {
  Upload,
  Chemistry,
  CheckmarkFilled,
  Link as LinkIcon,
  Barcode,
  DataShare,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";
import MedLabManifestImportModal from "../workflow/MedLabManifestImportModal";
import LinkPatientModal from "../workflow/LinkPatientModal";
import LinkOrderModal from "../workflow/LinkOrderModal";
import BulkLinkOrderModal from "../workflow/BulkLinkOrderModal";
import BiorepoSampleImportPage from "./common/BiorepoSampleImportPage";
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
 * @param {number} props.orderEntryPageId - The Patient Order Entry page ID (for fetching available orders)
 */
function SampleCollectionPage({
  entryId,
  pageData,
  onProgressUpdate,
  orderEntryPageId,
  notebookId,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  // State for samples
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Modal state
  const [importModalOpen, setImportModalOpen] = useState(false);
  const [linkPatientModalOpen, setLinkPatientModalOpen] = useState(false);
  const [linkOrderModalOpen, setLinkOrderModalOpen] = useState(false);
  const [bulkLinkOrderModalOpen, setBulkLinkOrderModalOpen] = useState(false);
  const [biorepoImportOpen, setBiorepoImportOpen] = useState(false);
  const [sampleForLinking, setSampleForLinking] = useState(null);
  const [samplesForBulkLinking, setSamplesForBulkLinking] = useState([]);
  const [collectionModalOpen, setCollectionModalOpen] = useState(false);
  const [samplesForCollection, setSamplesForCollection] = useState([]);
  const [collectionDateInput, setCollectionDateInput] = useState("");
  const [collectionTimeInput, setCollectionTimeInput] = useState("");
  const [isCollecting, setIsCollecting] = useState(false);

  // Barcode generation state
  const [barcodeSource, setBarcodeSource] = useState("about:blank");
  const [renderBarcode, setRenderBarcode] = useState(false);
  const [barcodeLabNo, setBarcodeLabNo] = useState("");

  const formatLocalDate = useCallback((date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  }, []);

  const formatLocalTime = useCallback((date) => {
    const hours = String(date.getHours()).padStart(2, "0");
    const minutes = String(date.getMinutes()).padStart(2, "0");
    return `${hours}:${minutes}`;
  }, []);

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
              id: String(sample.sampleId || sample.sampleItemId),
              sampleId: sample.sampleId || sample.sampleItemId,
              sampleItemId: sample.sampleItemId || sample.id,
              externalId: sample.externalId,
              accessionNumber: sample.accessionNumber,
              sampleType: sample.sampleType || sample.typeOfSample?.description,
              collectionDate: sample.collectionDate,
              status: sample.pageStatus || "PENDING",
              // Display "Participant" for anonymous samples (no patientId)
              patientName:
                sample.data?.patientName ||
                sample.patientName ||
                (!sample.data?.patientId && !sample.patientId
                  ? intl.formatMessage({
                      id: "medlab.collection.participant",
                      defaultMessage: "Participant",
                    })
                  : ""),
              patientId: sample.data?.patientId || sample.patientId || "",
              patientNationalId: sample.data?.patientNationalId || "",
              volume: sample.volume,
              containerType: sample.containerType || sample.data?.containerType,
              sampleTypeId:
                sample.sampleTypeId || sample.data?.sampleTypeId || "",
              labNo: sample.labNo || sample.accessionNumber,
              linkedOrderId:
                sample.linkedOrderId || sample.data?.linkedOrderId || "",
              linkedOrderLabNo:
                sample.linkedOrderLabNo || sample.data?.linkedOrderLabNo || "",
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
  }, [pageData?.id, intl]);

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

  // Handle bulk mark as collected - direct status update without modal
  const handleBulkMarkCollected = useCallback(
    async (sampleIds, collectionOverrides = {}) => {
      if (!sampleIds || sampleIds.length === 0) return;

      // Require real page ID for bulk actions
      if (!hasRealPageId) {
        setError(
          "Cannot mark samples: Page not properly initialized. Please refresh the page.",
        );
        return;
      }

      setIsCollecting(true);
      const selectedSamples = samples.filter((sample) =>
        sampleIds.includes(String(sample.id)),
      );
      const virtualOrderSamples = selectedSamples.filter(
        (sample) =>
          sample.linkedOrderLabNo &&
          !/^\d+$/.test(String(sample.sampleItemId || "")),
      );
      const regularNotebookSamples = selectedSamples.filter(
        (sample) =>
          !sample.linkedOrderLabNo ||
          /^\d+$/.test(String(sample.sampleItemId || "")),
      );

      let successCount = 0;
      let hadError = false;

      for (const sample of virtualOrderSamples) {
        const response = await new Promise((resolve) => {
          postToOpenElisServerJsonResponse(
            "/rest/medlab/sample-collection",
            JSON.stringify({
              labNo: sample.linkedOrderLabNo || sample.labNo,
              sampleTypeId: sample.sampleTypeId || "",
              containerType: sample.containerType || "",
              collectionDate:
                collectionOverrides.collectionDate ||
                sample.collectionDate ||
                "",
              collectionTime: collectionOverrides.collectionTime || "",
              notebookPageId: pageData.id,
            }),
            (json) => resolve(json),
          );
        });

        if (response?.success && !response?.statusCode) {
          successCount += 1;
        } else {
          hadError = true;
        }
      }

      if (regularNotebookSamples.length > 0) {
        const regularSampleIds = regularNotebookSamples
          .map((sample) => parseInt(sample.sampleItemId, 10))
          .filter((id) => !Number.isNaN(id));

        if (regularSampleIds.length > 0) {
          const status = await new Promise((resolve) => {
            postToOpenElisServer(
              `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
              JSON.stringify({
                sampleIds: regularSampleIds,
                status: "COMPLETED",
              }),
              (responseStatus) => resolve(responseStatus),
            );
          });

          if (status === 200) {
            successCount += regularSampleIds.length;
          } else {
            hadError = true;
          }
        }
      }

      if (!hadError && successCount === sampleIds.length) {
        addNotification({
          title: intl.formatMessage({ id: "notification.title" }),
          message: intl.formatMessage(
            {
              id: "medlab.collection.bulk.success",
              defaultMessage: "{count} samples marked as collected",
            },
            { count: successCount },
          ),
          kind: NotificationKinds.success,
        });
      } else {
        addNotification({
          title: intl.formatMessage({ id: "notification.title" }),
          message: intl.formatMessage(
            {
              id: "medlab.collection.bulk.partialError",
              defaultMessage:
                "{count} sample(s) collected. Some samples could not be collected.",
            },
            { count: successCount },
          ),
          kind:
            successCount > 0
              ? NotificationKinds.warning
              : NotificationKinds.error,
        });
      }

      setNotificationVisible(true);
      setSelectedSampleIds([]);
      setCollectionModalOpen(false);
      setSamplesForCollection([]);
      loadPageSamples();
      if (onProgressUpdate) {
        onProgressUpdate();
      }
      setIsCollecting(false);
    },
    [
      hasRealPageId,
      pageData,
      samples,
      intl,
      addNotification,
      setNotificationVisible,
      loadPageSamples,
      onProgressUpdate,
    ],
  );

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

      postToOpenElisServer(
        `/rest/medlab/samples/bulk-link-patient`,
        JSON.stringify({
          sampleItemIds: selectedSampleIds.map((id) => parseInt(id, 10)),
          patientId: patientId,
          notebookPageId: pageData.id,
        }),
        (status) => {
          if (status === 200) {
            addNotification({
              title: intl.formatMessage({ id: "notification.title" }),
              message: intl.formatMessage(
                {
                  id: "medlab.linkPatient.success",
                  defaultMessage:
                    "{count} sample(s) linked to patient / participant {name}",
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
                defaultMessage:
                  "Error linking samples to patient / participant",
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

  // Split pending samples into two groups: unlinked samples and samples ready for collection
  const unlinkedSamples = pendingSamples.filter(
    (s) => !s.linkedOrderId && !s.linkedOrderLabNo,
  );
  const readyForCollection = pendingSamples.filter(
    (s) => s.linkedOrderId || s.linkedOrderLabNo,
  );

  const collectedSamples = samples.filter((s) => s.status === "COMPLETED");

  const openCollectionModal = useCallback(
    (sampleIds) => {
      if (!sampleIds || sampleIds.length === 0) return;

      const selectedSamples = readyForCollection.filter((sample) =>
        sampleIds.includes(String(sample.id)),
      );

      const now = new Date();
      setSamplesForCollection(selectedSamples);
      setCollectionDateInput(formatLocalDate(now));
      setCollectionTimeInput(formatLocalTime(now));
      setCollectionModalOpen(true);
    },
    [readyForCollection, formatLocalDate, formatLocalTime],
  );

  const handleCollectionSubmit = useCallback(() => {
    const sampleIds = samplesForCollection.map((sample) => String(sample.id));
    handleBulkMarkCollected(sampleIds, {
      collectionDate: collectionDateInput,
      collectionTime: collectionTimeInput,
    });
  }, [
    samplesForCollection,
    handleBulkMarkCollected,
    collectionDateInput,
    collectionTimeInput,
  ]);

  // Table headers for unlinked samples (no actions column)
  const unlinkedHeaders = [
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
  ];

  // Table headers for ready-for-collection samples (with actions)
  const readyHeaders = [
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
      key: "linkedOrder",
      header: intl.formatMessage({
        id: "medlab.collection.linkedOrder",
        defaultMessage: "Linked Order",
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

  // Handle link order success
  const handleLinkOrderSuccess = useCallback(() => {
    setLinkOrderModalOpen(false);
    setSampleForLinking(null);
    loadPageSamples();
    if (onProgressUpdate) {
      onProgressUpdate();
    }
    addNotification({
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage({
        id: "medlab.collection.linkOrder.success",
        defaultMessage: "Sample linked to order successfully",
      }),
      kind: NotificationKinds.success,
    });
    setNotificationVisible(true);
  }, [
    loadPageSamples,
    onProgressUpdate,
    intl,
    addNotification,
    setNotificationVisible,
  ]);

  // Handle bulk link order success
  const handleBulkLinkOrderSuccess = useCallback(
    (result) => {
      setBulkLinkOrderModalOpen(false);
      setSamplesForBulkLinking([]);
      setSelectedSampleIds([]);
      loadPageSamples();
      if (onProgressUpdate) {
        onProgressUpdate();
      }
      addNotification({
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage(
          {
            id: "medlab.collection.bulkLinkOrder.success",
            defaultMessage: "{count} sample(s) linked to order(s) successfully",
          },
          { count: result?.samplesLinked || result?.linksCreated || 0 },
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

      {/* Sample Import Section */}
      <Grid fullWidth className="import-section">
        <Column lg={16} md={8} sm={4}>
          <div style={{ padding: "1rem 0" }}>
            <p style={{ marginBottom: "1rem", color: "#525252" }}>
              <FormattedMessage
                id="medlab.page.sampleCollection.import.description"
                defaultMessage="Bulk import samples from a CSV manifest file. Samples will be created with pre-labeled identifiers and can be linked to orders afterward."
              />
            </p>
            <div style={{ display: "flex", gap: "0.5rem" }}>
              <Button
                kind="secondary"
                size="md"
                renderIcon={DataShare}
                onClick={() => setBiorepoImportOpen(true)}
              >
                <FormattedMessage
                  id="medlab.page.sampleCollection.importFromBiorepo"
                  defaultMessage="Import from Biorepository"
                />
              </Button>
              <Button
                kind="primary"
                size="md"
                renderIcon={Upload}
                onClick={() => setImportModalOpen(true)}
              >
                <FormattedMessage
                  id="medlab.page.sampleCollection.importManifest"
                  defaultMessage="Import from Manifest"
                />
              </Button>
            </div>
          </div>
        </Column>
      </Grid>

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

      {/* Table 1: Samples Awaiting Order Link (no Collect buttons) */}
      {!loading && unlinkedSamples.length > 0 && (
        <div className="orders-section">
          <h5>
            <FormattedMessage
              id="medlab.page.sampleCollection.awaitingOrderLink"
              defaultMessage="Samples Awaiting Order Link"
            />
          </h5>
          <p
            style={{
              fontSize: "0.875rem",
              color: "#525252",
              marginBottom: "1rem",
            }}
          >
            <FormattedMessage
              id="medlab.page.sampleCollection.awaitingOrderLink.description"
              defaultMessage="These samples must be linked to orders before they can be collected."
            />
          </p>
          <DataTable
            rows={unlinkedSamples.map((s) => ({
              ...s,
              id: String(s.id),
              labNo: s.labNo || s.accessionNumber || "-",
              externalId: s.externalId || "-",
              sampleType: s.sampleType || "-",
              patientName: s.patientName || "-",
              collectionDate: s.collectionDate || "-",
            }))}
            headers={unlinkedHeaders}
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
                      <TableBatchAction
                        tabIndex={
                          batchActionProps.shouldShowBatchActions ? 0 : -1
                        }
                        renderIcon={LinkIcon}
                        onClick={() => {
                          if (selectedRows.length === 0) return;

                          // Get selected samples from unlinked samples
                          const selectedSamplesData = unlinkedSamples.filter(
                            (s) =>
                              selectedRows.some((r) => String(s.id) === r.id),
                          );

                          if (selectedSamplesData.length === 1) {
                            // Single sample: use existing LinkOrderModal
                            setSampleForLinking(selectedSamplesData[0]);
                            setLinkOrderModalOpen(true);
                          } else {
                            // Multiple samples: use new BulkLinkOrderModal
                            setSamplesForBulkLinking(selectedSamplesData);
                            setBulkLinkOrderModalOpen(true);
                          }
                        }}
                      >
                        <FormattedMessage
                          id="medlab.collection.linkOrder"
                          defaultMessage="Link to Order"
                        />
                      </TableBatchAction>
                    </TableBatchActions>
                    <TableToolbarContent>
                      <p style={{ fontSize: "0.875rem", color: "#525252" }}>
                        <FormattedMessage
                          id="medlab.collection.awaitingOrderLink.hint"
                          defaultMessage="Select samples to link to patients or orders"
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
                      {rows.map((row) => (
                        <TableRow key={row.id} {...getRowProps({ row })}>
                          <TableSelectRow {...getSelectionProps({ row })} />
                          {row.cells.map((cell) => (
                            <TableCell key={cell.id}>{cell.value}</TableCell>
                          ))}
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              );
            }}
          </DataTable>
        </div>
      )}

      {/* Table 2: Ready for Collection (with Collect buttons) */}
      {!loading && readyForCollection.length > 0 && (
        <div className="orders-section" style={{ marginTop: "2rem" }}>
          <h5>
            <FormattedMessage
              id="medlab.page.sampleCollection.readyForCollection"
              defaultMessage="Ready for Collection"
            />
          </h5>
          <p
            style={{
              fontSize: "0.875rem",
              color: "#525252",
              marginBottom: "1rem",
            }}
          >
            <FormattedMessage
              id="medlab.page.sampleCollection.readyForCollection.description"
              defaultMessage="These samples are linked to orders and ready to be collected."
            />
          </p>
          <DataTable
            rows={readyForCollection.map((s) => ({
              ...s,
              id: String(s.id),
              labNo: s.labNo || s.accessionNumber || "-",
              externalId: s.externalId || "-",
              sampleType: s.sampleType || "-",
              patientName: s.patientName || "-",
              linkedOrder:
                s.linkedOrderLabNo ||
                intl.formatMessage({
                  id: "medlab.collection.unlinked",
                  defaultMessage: "Not Linked",
                }),
              collectionDate: s.collectionDate || "-",
            }))}
            headers={readyHeaders}
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
                          const ids = selectedRows.map((r) => r.id);
                          openCollectionModal(ids);
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
                        renderIcon={Barcode}
                        onClick={() => {
                          // Get the first selected sample directly from selectedRows
                          if (selectedRows.length === 0) return;

                          const selectedSample = readyForCollection.find(
                            (s) => String(s.id) === selectedRows[0].id,
                          );

                          if (!selectedSample) {
                            addNotification({
                              title: intl.formatMessage({
                                id: "notification.title",
                              }),
                              message: intl.formatMessage({
                                id: "medlab.collection.barcode.noSample",
                                defaultMessage: "Selected sample not found",
                              }),
                              kind: NotificationKinds.error,
                            });
                            setNotificationVisible(true);
                            return;
                          }

                          // Get identifier from sample
                          // Priority: accessionNumber > labNo > externalId
                          const identifier =
                            selectedSample.accessionNumber ||
                            selectedSample.labNo ||
                            selectedSample.externalId;

                          if (!identifier) {
                            addNotification({
                              title: intl.formatMessage({
                                id: "notification.title",
                              }),
                              message: intl.formatMessage({
                                id: "medlab.collection.barcode.noIdentifier",
                                defaultMessage:
                                  "Sample does not have a valid identifier for barcode generation",
                              }),
                              kind: NotificationKinds.error,
                            });
                            setNotificationVisible(true);
                            return;
                          }

                          // Use LabelMakerServlet to generate barcode
                          setBarcodeLabNo(identifier);
                          setBarcodeSource(
                            `/LabelMakerServlet?labNo=${identifier}&type=specimen&quantity=1`,
                          );
                          setRenderBarcode(true);
                        }}
                      >
                        <FormattedMessage
                          id="medlab.collection.generateBarcode"
                          defaultMessage="Generate Barcode"
                        />
                      </TableBatchAction>
                    </TableBatchActions>
                    <TableToolbarContent>
                      <p style={{ fontSize: "0.875rem", color: "#525252" }}>
                        <FormattedMessage
                          id="medlab.collection.readyForCollection.hint"
                          defaultMessage="Select samples to collect"
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
                        const sample = readyForCollection.find(
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
                                    onClick={() =>
                                      openCollectionModal([String(sample.id)])
                                    }
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

      {/* Manifest Import Modal - Use MedLab version with 13-field support */}
      <MedLabManifestImportModal
        open={importModalOpen}
        onClose={() => setImportModalOpen(false)}
        entryId={entryId}
        pageData={pageData}
        onImportSuccess={handleImportSuccess}
      />

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

      {/* Link Order Modal */}
      <LinkOrderModal
        open={linkOrderModalOpen}
        onClose={() => {
          setLinkOrderModalOpen(false);
          setSampleForLinking(null);
        }}
        sample={sampleForLinking}
        orderEntryPageId={orderEntryPageId}
        onLinkSuccess={handleLinkOrderSuccess}
      />

      {/* Bulk Link Order Modal */}
      <BulkLinkOrderModal
        open={bulkLinkOrderModalOpen}
        onClose={() => {
          setBulkLinkOrderModalOpen(false);
          setSamplesForBulkLinking([]);
        }}
        samples={samplesForBulkLinking}
        orderEntryPageId={orderEntryPageId}
        onLinkSuccess={handleBulkLinkOrderSuccess}
      />

      <Modal
        open={collectionModalOpen}
        modalHeading={intl.formatMessage({
          id: "medlab.collection.modal.title",
          defaultMessage: "Record Collection",
        })}
        primaryButtonText={intl.formatMessage({
          id: "medlab.collection.modal.submit",
          defaultMessage: "Collect",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.button.cancel",
          defaultMessage: "Cancel",
        })}
        primaryButtonDisabled={
          isCollecting || samplesForCollection.length === 0
        }
        onRequestClose={() => {
          if (!isCollecting) {
            setCollectionModalOpen(false);
            setSamplesForCollection([]);
          }
        }}
        onRequestSubmit={handleCollectionSubmit}
      >
        <p style={{ marginBottom: "1rem", color: "#525252" }}>
          <FormattedMessage
            id="medlab.collection.modal.description"
            defaultMessage="Collection date and time default to now, but you can adjust them before recording collection."
          />
        </p>
        <p style={{ marginBottom: "1rem", fontSize: "0.875rem" }}>
          <FormattedMessage
            id="medlab.collection.modal.count"
            defaultMessage="{count} sample(s) will be collected."
            values={{ count: samplesForCollection.length }}
          />
        </p>
        <Grid condensed>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="medlab-collection-date"
              type="date"
              labelText={intl.formatMessage({
                id: "medlab.collection.date",
                defaultMessage: "Collection Date",
              })}
              value={collectionDateInput}
              onChange={(event) => setCollectionDateInput(event.target.value)}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="medlab-collection-time"
              type="time"
              labelText={intl.formatMessage({
                id: "medlab.collection.time",
                defaultMessage: "Collection Time",
              })}
              value={collectionTimeInput}
              onChange={(event) => setCollectionTimeInput(event.target.value)}
            />
          </Column>
        </Grid>
      </Modal>

      {/* Barcode Display Modal */}
      <Modal
        open={renderBarcode}
        onRequestClose={() => {
          setRenderBarcode(false);
          setBarcodeSource("about:blank");
          setBarcodeLabNo("");
        }}
        modalHeading={
          <>
            <FormattedMessage id="barcode.header" defaultMessage="Barcode" />
            {barcodeLabNo && ` - ${barcodeLabNo}`}
          </>
        }
        passiveModal
        size="lg"
      >
        <div style={{ minHeight: "500px" }}>
          <iframe
            src={barcodeSource}
            width="100%"
            height="500px"
            title="Barcode Preview"
            style={{ border: "1px solid #e0e0e0" }}
          />
        </div>
      </Modal>

      {/* Biorepository Sample Import Modal */}
      {biorepoImportOpen && (
        <Modal
          open
          modalHeading={intl.formatMessage({
            id: "biorepo.import.title",
            defaultMessage: "Biorepository Sample Request / Withdrawal Form",
          })}
          passiveModal
          onRequestClose={() => setBiorepoImportOpen(false)}
          size="lg"
        >
          <BiorepoSampleImportPage
            entryId={entryId}
            pageData={pageData}
            progress={progress}
            onProgressUpdate={() => {
              setBiorepoImportOpen(false);
              if (onProgressUpdate) onProgressUpdate();
            }}
            notebookId={notebookId}
          />
        </Modal>
      )}
    </div>
  );
}

export default SampleCollectionPage;
