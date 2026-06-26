import React, {
  useContext,
  useState,
  useEffect,
  useMemo,
  useRef,
  useCallback,
} from "react";
import { useHistory, useLocation } from "react-router-dom";
import { useWorkflowPrefix } from "../OrderContext";
import { useIntl, FormattedMessage } from "react-intl";
import {
  Tile,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableExpandHeader,
  TableExpandRow,
  TableExpandedRow,
  Button,
  Tag,
  NumberInput,
  Select,
  SelectItem,
  TextArea,
  InlineNotification,
  Checkbox,
} from "@carbon/react";
import { Printer, Checkmark } from "@carbon/icons-react";
import OrderWorkflowLayout from "../OrderWorkflowLayout";
import { useOrderContext } from "../OrderContext";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import LocationPickerInline from "../../storage/LocationPicker/LocationPickerInline";
import OrderReferOutSection from "./referOut/OrderReferOutSection";
import {
  getDeepestLocationSelection,
  positionToCoordinate,
  selectionToHierarchicalPath,
} from "../../storage/LocationPicker/locationSelectionMapper";
import {
  postToOpenElisServerJsonResponse,
  patchToOpenElisServerJsonResponse,
  putToOpenElisServer,
} from "../../utils/Utils";

/**
 * OrderLabel - Step 3: Label & Store
 *
 * Handles barcode label printing and storage location assignment.
 * Features:
 * - Print various label types (Order, Sample, Slide, Block, Freezer)
 * - Quick assign via barcode scanning
 * - Storage location selection with search
 * - Position coordinate entry
 * - Condition notes
 */

const OrderLabel = () => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const workflowPrefix = useWorkflowPrefix();
  const {
    orderData,
    samples,
    setSamples,
    setCurrentStep,
    labNumber: contextLabNumber,
    markStepComplete,
    orderId,
    storageSkipped,
    setStorageSkipped,
    loadOrder,
    isLoading,
  } = useOrderContext();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  // Get labNumber from context or orderData
  const labNumber =
    contextLabNumber || orderData?.sampleOrderItems?.labNo || null;

  // Deep-link support: if the URL carries ?labNumber and no order is loaded,
  // fetch it before falling back to the Step 1 redirect. Lets external links
  // (dashboard widgets, tests) land directly on Step 3 without walking the
  // wizard.
  const urlLabNumber = new URLSearchParams(location.search).get("labNumber");

  // Persist storageSkipped using the resolved labNumber (context labNumber may be
  // null on fresh creation flow before loadOrder has been called)
  const handleStorageSkippedChange = useCallback(
    (checked) => {
      setStorageSkipped(checked);
      if (labNumber) {
        putToOpenElisServer(
          `/rest/order/storage-skipped?labNumber=${encodeURIComponent(labNumber)}&storageSkipped=${checked}`,
          null,
          Function.prototype,
        );
      }
    },
    [labNumber, setStorageSkipped],
  );

  // Redirect to enter step if no order is loaded
  useEffect(() => {
    if (orderId || labNumber || isLoading) return;
    if (urlLabNumber) {
      loadOrder(urlLabNumber).catch(() => {
        history.replace(`${workflowPrefix}/enter`);
      });
      return;
    }
    history.replace(`${workflowPrefix}/enter`);
  }, [orderId, labNumber, isLoading, urlLabNumber, loadOrder, history]);

  // Label printing state - order label + one entry per sample
  const [labelQuantities, setLabelQuantities] = useState(() => {
    const initial = { order: 1 };
    samples.forEach((_, index) => {
      initial[`sample-${index}`] = 1;
    });
    return initial;
  });
  const [printedLabels, setPrintedLabels] = useState(new Set());

  // For vector orders, the label table can grow into thousands of rows if a
  // pool has many organisms. Collapse the per-organism rows into one row per
  // pool (grouped by sample/animal type) with a Carbon-DataTable
  // expandable row exposing the paginated specimen list inline.
  const isVectorWorkflow = workflowPrefix === "/order/vector";
  const POOL_PAGE_SIZE = 25;
  const [poolPage, setPoolPage] = useState({});

  const labelFallbackSampleName = intl.formatMessage({
    id: "sample.fallback.name",
    defaultMessage: "Sample",
  });

  // Group unvoided samples into pools for the pool view. Each group collects
  // its specimens (sample_items) sorted by sort_order so the barcode range and
  // pagination show in submission order. Group key is vectorPoolId (the stable
  // pool identifier) so two pools of the same animal stay distinct rows.
  const poolGroups = useMemo(() => {
    if (!isVectorWorkflow) return [];
    const visible = (samples || []).filter(
      (s) => !s?.voided && !s?.sampleRejected,
    );
    const groups = new Map();
    visible.forEach((sample, index) => {
      const key =
        sample?.vectorPoolId ||
        sample?.typeOfSampleId ||
        sample?.sampleTypeId ||
        `unknown-${index}`;
      if (!groups.has(key)) {
        groups.set(key, {
          key,
          name:
            sample?.sampleTypeName || sample?.name || labelFallbackSampleName,
          specimens: [],
        });
      }
      groups.get(key).specimens.push({ ...sample, _originalIndex: index });
    });
    return Array.from(groups.values()).map((g) => ({
      ...g,
      specimens: g.specimens.sort((a, b) => {
        const sa = parseInt(a.sortOrder, 10);
        const sb = parseInt(b.sortOrder, 10);
        if (Number.isFinite(sa) && Number.isFinite(sb)) return sa - sb;
        return 0;
      }),
    }));
  }, [samples, isVectorWorkflow, labelFallbackSampleName]);

  // Update label quantities when samples change.
  // For vector orders, allocate one slot per pool group (the row in the main
  // table) — individual specimens that show up in the expanded panel still
  // use the existing sample-<index> slots so their Print buttons keep
  // working without special-casing.
  useEffect(() => {
    setLabelQuantities((prev) => {
      const updated = { order: prev.order || 1 };
      if (isVectorWorkflow) {
        poolGroups.forEach((pool) => {
          updated[`pool-${pool.key}`] = prev[`pool-${pool.key}`] || 1;
        });
      }
      samples.forEach((_, index) => {
        updated[`sample-${index}`] = prev[`sample-${index}`] || 1;
      });
      return updated;
    });
  }, [samples, isVectorWorkflow, poolGroups]);

  // Storage assignment state
  const [selectedSampleIndex, setSelectedSampleIndex] = useState(0);
  const [assignedStorage, setAssignedStorage] = useState({});
  // Mirror of assignedStorage in a ref so the save loop reads the latest
  // pending entries even if it runs from a closure captured at an earlier
  // render (e.g. during the saveOrder→reload→savePending cascade).
  const assignedStorageRef = useRef({});
  useEffect(() => {
    assignedStorageRef.current = assignedStorage;
  }, [assignedStorage]);
  // Per-sample condition notes
  const [conditionNotes, setConditionNotes] = useState({});

  // Initialize assignedStorage and conditionNotes from loaded samples (when navigating back or reloading)
  useEffect(() => {
    const initialStorage = {};
    const initialNotes = {};
    let hasAnyStorage = false;

    samples.forEach((sample, index) => {
      if (sample.storageLocationId) {
        hasAnyStorage = true;
        initialStorage[index] = {
          locationId: sample.storageLocationId,
          locationType: sample.storageLocationType || "device",
          hierarchicalPath: sample.storageHierarchicalPath || "",
          position: sample.storagePositionCoordinate || "",
          pending: false,
        };
      }
      if (sample.storageNotes !== undefined && sample.storageNotes !== null) {
        initialNotes[index] = sample.storageNotes;
      }
    });

    if (Object.keys(initialStorage).length > 0) {
      setAssignedStorage(initialStorage);
    }
    setConditionNotes((prev) => ({ ...prev, ...initialNotes }));
  }, [samples]);

  // Keep the storage selector off rejected/voided specimens. They're excluded
  // from the dropdown options (and from labels/refer-out), so a stale index —
  // e.g. the default 0 landing on a rejected specimen that sorts first — would
  // desync the detail card (showing the rejected sample) from the selector.
  useEffect(() => {
    const isLive = (i) =>
      samples[i] && !samples[i].sampleRejected && !samples[i].voided;
    if (samples.length > 0 && !isLive(selectedSampleIndex)) {
      const firstLive = samples.findIndex(
        (s) => s && !s.sampleRejected && !s.voided,
      );
      if (firstLive >= 0 && firstLive !== selectedSampleIndex) {
        setSelectedSampleIndex(firstLive);
      }
    }
  }, [samples, selectedSampleIndex]);

  // Get current sample being configured
  const currentSample = samples[selectedSampleIndex] || {};

  // Count specimens that are actually storable (rejected/voided are excluded
  // from the selector); drives whether the multi-sample picker is worth showing.
  const liveSampleCount = samples.filter(
    (s) => s && !s.sampleRejected && !s.voided,
  ).length;

  // Build patient info for order label
  const patientName = orderData?.patientProperties
    ? `${orderData.patientProperties.lastName || ""}, ${orderData.patientProperties.firstName || ""}`.trim()
    : "---";

  // Build label rows - Order label + one row per printable sample.
  //
  // Vector pool fan-out hard-deletes the "pool of N" parent sample_item, so
  // only the N per-organism siblings survive — no voided placeholder remains
  // to skip. The voided filter below is defensive for any non-vector workflow
  // that may soft-delete a sample_item. The id encodes the row's index in
  // the *full* samples array so handlePrintLabel / assignedStorage /
  // setSamples lookups stay aligned with the unfiltered list everywhere else
  // in this component.
  const envFields = orderData?.sampleOrderItems?.environmentalFields || {};
  const siteName =
    envFields.samplingSiteName ||
    envFields.vecCollectionSiteName ||
    envFields.collectionSiteName ||
    "";

  const labNrPrefix = intl.formatMessage({
    id: "label.order.labNrPrefix",
    defaultMessage: "Lab Nr",
  });
  const patientPrefix = intl.formatMessage({
    id: "label.order.patientPrefix",
    defaultMessage: "Patient",
  });
  const orderLabelRow = {
    id: "order",
    name: intl.formatMessage({
      id: "label.type.order",
      defaultMessage: "Order Label",
    }),
    content: `${labNrPrefix}: ${labNumber || "---"} | ${patientPrefix}: ${patientName}`,
    barcode: labNumber || "---",
  };

  // Build a per-specimen row from a (sample, original-index, display-index)
  // triple. Shared by the flat path (clinical/environmental) and by the
  // expanded-pool panel under vector.
  const buildSpecimenRow = (sample, index, displayIndex) => {
    const specimenBarcode =
      labNumber && sample.sortOrder
        ? `${labNumber}.${sample.sortOrder}`
        : `${labNumber || "---"}-${index + 1}`;
    const typeName =
      sample.sampleTypeName || sample.name || labelFallbackSampleName;
    const contentParts = [typeName];
    if (siteName) contentParts.push(siteName);
    contentParts.push(sample.collectionDate || "---");
    return {
      id: `sample-${index}`,
      name: `${intl.formatMessage({
        id: "label.type.sample",
        defaultMessage: "Sample Label",
      })} ${displayIndex + 1}`,
      content: contentParts.join(" | "),
      barcode: specimenBarcode,
      qcMetadata: sample.qcMetadata || null,
    };
  };

  const flatSpecimenRows = samples
    .map((sample, index) => ({ sample, index }))
    // Exclude voided and rejected/resampled specimens — a rejected specimen is
    // shown read-only in the QA intake-acceptance table, not labelled/stored here.
    .filter(({ sample }) => !sample?.voided && !sample?.sampleRejected)
    .map(({ sample, index }, displayIndex) =>
      buildSpecimenRow(sample, index, displayIndex),
    );

  // Vector orders collapse to one row per pool (grouped by sample type) so
  // a "pool of 1000" doesn't render 1001 table rows. The individual rows
  // are still reachable via the per-pool expand toggle, paginated.
  const poolSummaryRows = poolGroups.map((pool) => {
    const first = pool.specimens[0];
    const last = pool.specimens[pool.specimens.length - 1];
    const barcodeRange =
      pool.specimens.length === 0
        ? "---"
        : labNumber && first?.sortOrder && last?.sortOrder
          ? pool.specimens.length === 1
            ? `${labNumber}.${first.sortOrder}`
            : `${labNumber}.${first.sortOrder} – ${labNumber}.${last.sortOrder}`
          : labNumber || "---";
    const poolContentParts = [pool.name];
    if (siteName) poolContentParts.push(siteName);
    poolContentParts.push(first?.collectionDate || "---");
    return {
      id: `pool-${pool.key}`,
      kind: "pool",
      poolKey: pool.key,
      name: intl.formatMessage(
        {
          id: "label.type.pool",
          defaultMessage: "Pool of {count} {animal}",
        },
        { count: pool.specimens.length, animal: pool.name },
      ),
      content: poolContentParts.join(" | "),
      barcode: barcodeRange,
      pool,
    };
  });

  const labelRows = [
    orderLabelRow,
    ...(isVectorWorkflow ? poolSummaryRows : flatSpecimenRows),
  ];

  const renderQcTag = (qcMeta) => {
    if (!qcMeta?.qcType) return null;
    const tagType =
      qcMeta.qcType === "BLANK"
        ? "blue"
        : qcMeta.qcType === "DUPLICATE"
          ? "teal"
          : "purple";
    return (
      <Tag type={tagType} size="sm">
        <FormattedMessage
          id={`qc.type.${qcMeta.qcType.toLowerCase()}`}
          defaultMessage={`QC: ${qcMeta.qcType}`}
        />
      </Tag>
    );
  };

  const handleQuantityChange = (labelType, value) => {
    setLabelQuantities((prev) => ({
      ...prev,
      [labelType]: value,
    }));
  };

  const handlePrintLabel = (labelType) => {
    const quantity = labelQuantities[labelType];
    if (quantity <= 0) return;

    let url;

    // Quantity flows through; the BarcodeLabelInfo.numPrinted cap stays in
    // effect so the servlet's Override prompt fires when reached.
    if (labelType === "order") {
      url = `/LabelMakerServlet?labNo=${encodeURIComponent(labNumber)}&type=order&quantity=${quantity}`;
    } else if (labelType.startsWith("pool-")) {
      // Pool row: fire one specimenOrder request and let the servlet emit a
      // multi-page PDF with one barcode per organism. For single-pool
      // vector orders this is exactly right; for multi-pool orders the
      // servlet currently doesn't filter by sample type so it'll print
      // every pool's specimens — a known limitation until a sampleTypeId
      // filter is added on the backend.
      url = `/LabelMakerServlet?labNo=${encodeURIComponent(labNumber)}&type=specimenOrder&quantity=${quantity}`;
    } else if (labelType.startsWith("sample-")) {
      // Specimen URL uses labNo.<sortOrder> (1-based) so the servlet targets
      // a single sample item rather than every item on the order.
      const sampleIndex = parseInt(labelType.replace("sample-", ""), 10);
      const sample = samples[sampleIndex];
      const sortOrder = sample?.sortOrder || sampleIndex + 1;
      const specimenLabNo = `${labNumber}.${sortOrder}`;

      url = `/LabelMakerServlet?labNo=${encodeURIComponent(specimenLabNo)}&type=specimen&quantity=${quantity}`;
    } else {
      url = `/LabelMakerServlet?labNo=${encodeURIComponent(labNumber)}&type=default&quantity=${quantity}`;
    }

    if (!openPrintWindow(url)) {
      return;
    }

    setPrintedLabels((prev) => new Set([...prev, labelType]));

    addNotification({
      kind: NotificationKinds.success,
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage(
        {
          id: "label.print.success.count",
          defaultMessage: "{count} label(s) sent to print",
        },
        { count: quantity },
      ),
    });
    setNotificationVisible(true);
  };

  // Returns true on success; false (with an error toast) when the popup is
  // blocked. Without the null-check, a blocked popup would still raise the
  // green "sent to print" toast even though no PDF actually opened.
  const openPrintWindow = (url) => {
    const printWindow = window.open(url, "_blank");
    if (!printWindow) {
      console.warn("OrderLabel: window.open returned null for", url);
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "label.print.error.popupBlocked",
          defaultMessage:
            "Popup blocked. Please allow popups for this site to print labels.",
        }),
      });
      setNotificationVisible(true);
      return false;
    }
    return true;
  };

  const handlePrintAllLabels = () => {
    const orderQty = Math.max(labelQuantities.order || 1, 1);
    // type=default prints order label + all specimen labels in one PDF.
    // The servlet's environmental specimen handling uses getSampleItemsBySampleId
    // (no status filter) for env/vector via isEnvOrVectorSample, so all
    // specimens are included regardless of status.
    const url = `/LabelMakerServlet?labNo=${encodeURIComponent(labNumber)}&type=default&quantity=${orderQty}`;
    if (!openPrintWindow(url)) {
      return;
    }

    // Mark all as printed
    const allPrinted = new Set(["order"]);
    samples.forEach((_, index) => {
      allPrinted.add(`sample-${index}`);
    });
    setPrintedLabels(allPrinted);

    addNotification({
      kind: NotificationKinds.success,
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage({
        id: "label.printAll.success",
        defaultMessage: "All labels sent to print",
      }),
    });
    setNotificationVisible(true);
  };

  /**
   * Handle location change from the LocationPicker.
   * Stores selection locally; the actual /assign or /move API call
   * happens later in savePendingStorageAssignments() when the user
   * submits the order form.
   */
  const handleLocationChange = (location) => {
    if (location) {
      const locationId = String(location.id || location.locationId || "");
      const locationType = location.type || location.locationType || "device";
      const hierarchicalPath = location.hierarchicalPath || location.path || "";

      // Update local state to track pending storage assignment
      setAssignedStorage((prev) => ({
        ...prev,
        [selectedSampleIndex]: {
          locationId: locationId,
          locationType: locationType,
          hierarchicalPath: hierarchicalPath,
          position: location.positionCoordinate || "",
          pending: true, // Mark as pending save
        },
      }));
    }
  };

  /**
   * Save pending storage assignments via API
   * Uses /assign for new assignments, /move for reassignments
   *
   * Accepts an optional `samplesOverride` argument so callers can pass the
   * freshly-saved samples returned from saveOrder — the closure-captured
   * `samples` is stale immediately after a save (state hasn't re-rendered yet),
   * so without an override new samples have no sampleItemId and the assign
   * loop silently skips them.
   */
  const savePendingStorageAssignments = async (samplesOverride) => {
    const samplesForLookup = samplesOverride || samples;
    // Read assignedStorage from the ref so we have the latest pending entries
    // even if the save-order→reload→savePending cascade caused intervening
    // re-renders that the closure wouldn't see.
    const latestAssignedStorage = assignedStorageRef.current || assignedStorage;
    const pendingAssignments = Object.entries(latestAssignedStorage).filter(
      ([, storage]) => storage.pending,
    );

    for (const [sampleIndexStr, storage] of pendingAssignments) {
      const sampleIndex = parseInt(sampleIndexStr, 10);
      const currentSampleItem = samplesForLookup[sampleIndex];
      const sampleItemId =
        currentSampleItem?.sampleItemId || currentSampleItem?.id;

      if (!sampleItemId) {
        console.warn(
          `Skipping storage assignment for sample ${sampleIndex} - no sampleItemId`,
        );
        continue;
      }

      // Check if sample already has a storage assignment (use move instead of assign)
      const hasExistingAssignment = currentSampleItem.storageLocationId;

      const requestData = {
        sampleItemId: String(sampleItemId),
        locationId: storage.locationId,
        locationType: storage.locationType,
        positionCoordinate: storage.position || "",
        notes: conditionNotes[sampleIndex] || "",
      };

      // Add reason field for move endpoint
      if (hasExistingAssignment) {
        requestData.reason = "Reassignment from order workflow";
      }

      const endpoint = hasExistingAssignment
        ? "/rest/storage/sample-items/move"
        : "/rest/storage/sample-items/assign";

      await new Promise((resolve, reject) => {
        postToOpenElisServerJsonResponse(
          endpoint,
          JSON.stringify(requestData),
          (response) => {
            if (response && !response.error && !response.message) {
              // Mark as no longer pending
              setAssignedStorage((prev) => ({
                ...prev,
                [sampleIndex]: {
                  ...prev[sampleIndex],
                  pending: false,
                  hierarchicalPath:
                    response.hierarchicalPath || storage.hierarchicalPath,
                },
              }));
              resolve(response);
            } else {
              reject(
                new Error(
                  response?.message ||
                    response?.error ||
                    "Failed to assign storage",
                ),
              );
            }
          },
        );
      });
    }
  };

  /**
   * Update notes for samples that already have storage assignments (no location change)
   */
  const updateStorageNotes = async () => {
    for (let sampleIndex = 0; sampleIndex < samples.length; sampleIndex++) {
      const sample = samples[sampleIndex];
      const sampleItemId = sample?.sampleItemId || sample?.id;
      const storage = assignedStorage[sampleIndex];

      // Skip if no storage assignment or if pending (will be handled by savePendingStorageAssignments)
      if (!sampleItemId || !sample.storageLocationId || storage?.pending) {
        continue;
      }

      const currentNotes = conditionNotes[sampleIndex] || "";
      const savedNotes = sample.storageNotes || "";

      // Only update if notes changed
      if (currentNotes !== savedNotes) {
        await new Promise((resolve, reject) => {
          patchToOpenElisServerJsonResponse(
            `/rest/storage/sample-items/${sampleItemId}`,
            JSON.stringify({ notes: currentNotes }),
            (response) => {
              if (response && !response.error && !response.message) {
                // Update the sample's storageNotes via setSamples for proper React state update
                setSamples((prevSamples) =>
                  prevSamples.map((s, idx) =>
                    idx === sampleIndex
                      ? { ...s, storageNotes: currentNotes }
                      : s,
                  ),
                );
                resolve(response);
              } else {
                reject(
                  new Error(
                    response?.message ||
                      response?.error ||
                      "Failed to update notes",
                  ),
                );
              }
            },
          );
        });
      }
    }
  };

  const handleSave = async () => {
    try {
      await savePendingStorageAssignments();
      await updateStorageNotes();
      markStepComplete("label");
      addNotification({
        kind: NotificationKinds.success,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "save.order.success.msg" }),
      });
      setNotificationVisible(true);
    } catch (error) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message:
          error.message || intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
    }
  };

  const handleSaveAndNext = async () => {
    try {
      await savePendingStorageAssignments();
      await updateStorageNotes();
      markStepComplete("label");
      setCurrentStep(3);
      history.push(
        labNumber
          ? `${workflowPrefix}/qa?order=${encodeURIComponent(labNumber)}`
          : `${workflowPrefix}/qa`,
      );
    } catch (error) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message:
          error.message || intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
    }
  };

  // Check if all samples have storage assigned
  const allSamplesHaveStorage =
    samples.length > 0 &&
    samples.every((s, idx) => s.storageLocationId || assignedStorage[idx]);

  // Vector workflow has no collection step and no storage requirement
  const canProceed = isVectorWorkflow
    ? true
    : (printedLabels.has("order") || printedLabels.has("sample")) &&
      (allSamplesHaveStorage || storageSkipped);

  // Don't render if no order loaded (will redirect)
  if (!orderId && !labNumber) {
    return null;
  }

  return (
    <OrderWorkflowLayout
      title="order.step.label"
      canProceed={canProceed}
      onSave={handleSave}
      onSaveAndNext={handleSaveAndNext}
    >
      {notificationVisible && <AlertDialog />}

      {/* Print Labels Section */}
      <Tile className="order-section print-labels-section">
        <h4>
          <FormattedMessage
            id="label.print.title"
            defaultMessage="Print Labels"
          />
        </h4>
        <p className="section-description">
          <FormattedMessage
            id="label.print.description"
            defaultMessage="Labels are used to track accession/order from lab registration. Labels can also be printed from step 1."
          />
        </p>

        <DataTable
          rows={labelRows.map((lr) => ({
            id: lr.id,
            labelType: lr.name,
            content: lr.content,
            barcode: lr.barcode,
            quantity: lr.id,
            print: lr.id,
          }))}
          headers={[
            {
              key: "labelType",
              header: intl.formatMessage({
                id: "label.type",
                defaultMessage: "Label Type",
              }),
            },
            {
              key: "content",
              header: intl.formatMessage({
                id: "label.content",
                defaultMessage: "Content",
              }),
            },
            {
              key: "barcode",
              header: intl.formatMessage({
                id: "label.barcode",
                defaultMessage: "Barcode",
              }),
            },
            {
              key: "quantity",
              header: intl.formatMessage({
                id: "label.quantity",
                defaultMessage: "Quantity",
              }),
            },
            {
              key: "print",
              header: intl.formatMessage({
                id: "label.print",
                defaultMessage: "Print",
              }),
            },
          ]}
        >
          {({ rows, headers, getTableProps, getHeaderProps, getRowProps }) => (
            <Table {...getTableProps()} size="md">
              <TableHead>
                <TableRow>
                  {/* Empty header cell aligned with TableExpandRow's
                      chevron column. Only pool rows expand, but every
                      row needs the column to keep alignment. */}
                  <TableExpandHeader
                    aria-label={intl.formatMessage({
                      id: "label.expand.poolDetails",
                      defaultMessage: "Expand pool details",
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
                  const labelId = row.id;
                  const labelRow = labelRows.find((lr) => lr.id === labelId);
                  const isPoolRow = labelRow?.kind === "pool";
                  const cells = (
                    <>
                      <TableCell>
                        <span className="label-type-cell">
                          {labelRow?.name}
                          {renderQcTag(labelRow?.qcMetadata)}
                        </span>
                      </TableCell>
                      <TableCell>{labelRow?.content}</TableCell>
                      <TableCell>
                        <code>{labelRow?.barcode}</code>
                      </TableCell>
                      <TableCell>
                        <NumberInput
                          id={`quantity-${labelId}`}
                          min={0}
                          max={10}
                          value={labelQuantities[labelId] || 1}
                          onChange={(e, { value }) =>
                            handleQuantityChange(labelId, value)
                          }
                          size="sm"
                          hideSteppers={false}
                          className="quantity-input"
                        />
                      </TableCell>
                      <TableCell>
                        <Button
                          kind="primary"
                          size="sm"
                          renderIcon={
                            printedLabels.has(labelId) ? Checkmark : Printer
                          }
                          onClick={() => handlePrintLabel(labelId)}
                          disabled={(labelQuantities[labelId] || 1) <= 0}
                        >
                          <FormattedMessage
                            id="label.print"
                            defaultMessage="Print"
                          />
                        </Button>
                      </TableCell>
                    </>
                  );

                  if (!isPoolRow) {
                    // Order-label row (and clinical/environmental sample
                    // rows) aren't expandable — render plain TableRow with
                    // an empty placeholder cell to match the chevron column.
                    return (
                      <TableRow key={row.id} {...getRowProps({ row })}>
                        <TableCell />
                        {cells}
                      </TableRow>
                    );
                  }

                  // Pool rows: use Carbon's expandable variant. Carbon
                  // manages row.isExpanded internally via the chevron
                  // click; we read it here to gate the expanded panel.
                  const pool = labelRow.pool;
                  const totalPages = Math.max(
                    1,
                    Math.ceil(pool.specimens.length / POOL_PAGE_SIZE),
                  );
                  const currentPage = Math.min(
                    poolPage[labelRow.poolKey] || 0,
                    totalPages - 1,
                  );
                  const start = currentPage * POOL_PAGE_SIZE;
                  const visible = pool.specimens.slice(
                    start,
                    start + POOL_PAGE_SIZE,
                  );
                  return (
                    <React.Fragment key={row.id}>
                      <TableExpandRow
                        isExpanded={row.isExpanded}
                        ariaLabel={
                          row.isExpanded
                            ? intl.formatMessage({
                                id: "label.pool.collapse",
                                defaultMessage: "Hide individual specimens",
                              })
                            : intl.formatMessage({
                                id: "label.pool.expand",
                                defaultMessage: "Show individual specimens",
                              })
                        }
                        {...getRowProps({ row })}
                      >
                        {cells}
                      </TableExpandRow>
                      {row.isExpanded && (
                        <TableExpandedRow colSpan={headers.length + 1}>
                          <div className="pool-expansion-panel">
                            <div className="pool-expansion-header">
                              <h5>
                                <FormattedMessage
                                  id="label.pool.expansion.title"
                                  defaultMessage="{animal} specimens ({count})"
                                  values={{
                                    animal: pool.name,
                                    count: pool.specimens.length,
                                  }}
                                />
                              </h5>
                              <span className="pool-expansion-page">
                                <FormattedMessage
                                  id="label.pool.expansion.page"
                                  defaultMessage="Page {page} of {total}"
                                  values={{
                                    page: currentPage + 1,
                                    total: totalPages,
                                  }}
                                />
                              </span>
                            </div>
                            <Table size="sm" className="pool-expansion-table">
                              <TableHead>
                                <TableRow>
                                  <TableHeader>#</TableHeader>
                                  <TableHeader>
                                    <FormattedMessage
                                      id="label.barcode"
                                      defaultMessage="Barcode"
                                    />
                                  </TableHeader>
                                  <TableHeader>
                                    <FormattedMessage
                                      id="label.print"
                                      defaultMessage="Print"
                                    />
                                  </TableHeader>
                                </TableRow>
                              </TableHead>
                              <TableBody>
                                {visible.map((sample) => {
                                  const index = sample._originalIndex;
                                  const specimenRow = buildSpecimenRow(
                                    sample,
                                    index,
                                    index,
                                  );
                                  const subLabelId = specimenRow.id;
                                  return (
                                    <TableRow key={subLabelId}>
                                      <TableCell>
                                        {sample.sortOrder || "—"}
                                      </TableCell>
                                      <TableCell>
                                        <code>{specimenRow.barcode}</code>
                                      </TableCell>
                                      <TableCell>
                                        <Button
                                          kind="ghost"
                                          size="sm"
                                          renderIcon={
                                            printedLabels.has(subLabelId)
                                              ? Checkmark
                                              : Printer
                                          }
                                          onClick={() =>
                                            handlePrintLabel(subLabelId)
                                          }
                                        >
                                          <FormattedMessage
                                            id="label.print"
                                            defaultMessage="Print"
                                          />
                                        </Button>
                                      </TableCell>
                                    </TableRow>
                                  );
                                })}
                              </TableBody>
                            </Table>
                            {totalPages > 1 && (
                              <div className="pool-expansion-pager">
                                <Button
                                  kind="ghost"
                                  size="sm"
                                  onClick={() =>
                                    setPoolPage((prev) => ({
                                      ...prev,
                                      [labelRow.poolKey]: Math.max(
                                        0,
                                        currentPage - 1,
                                      ),
                                    }))
                                  }
                                  disabled={currentPage === 0}
                                >
                                  <FormattedMessage
                                    id="label.pool.expansion.prev"
                                    defaultMessage="Previous"
                                  />
                                </Button>
                                <Button
                                  kind="ghost"
                                  size="sm"
                                  onClick={() =>
                                    setPoolPage((prev) => ({
                                      ...prev,
                                      [labelRow.poolKey]: Math.min(
                                        totalPages - 1,
                                        currentPage + 1,
                                      ),
                                    }))
                                  }
                                  disabled={currentPage >= totalPages - 1}
                                >
                                  <FormattedMessage
                                    id="label.pool.expansion.next"
                                    defaultMessage="Next"
                                  />
                                </Button>
                              </div>
                            )}
                          </div>
                        </TableExpandedRow>
                      )}
                    </React.Fragment>
                  );
                })}
              </TableBody>
            </Table>
          )}
        </DataTable>

        <div className="print-all-actions">
          <p className="helper-text">
            <FormattedMessage
              id="label.print.helper"
              defaultMessage="* Labels are used to label pre-printed labels."
            />
          </p>
          <Button
            kind="secondary"
            renderIcon={Printer}
            onClick={handlePrintAllLabels}
          >
            <FormattedMessage
              id="label.printAll"
              defaultMessage="Print All Labels"
            />
          </Button>
        </div>
      </Tile>

      {/* Assign Storage Location Section */}
      <Tile className="order-section storage-assignment-section">
        <h4>
          <FormattedMessage
            id="storage.assign.title"
            defaultMessage="Assign Storage Location"
          />
        </h4>

        {/* Storage Assignment Summary */}
        {samples.length > 0 &&
          (() => {
            const assignedCount = samples.filter(
              (s, idx) =>
                !s.sampleRejected &&
                (s.storageLocationId || assignedStorage[idx]),
            ).length;
            const unassignedCount =
              samples.filter((s) => !s.sampleRejected).length - assignedCount;
            const unassignedNames = samples
              .map((sample, idx) => {
                const isAssigned =
                  sample.storageLocationId || assignedStorage[idx];
                if (!sample.sampleRejected && !isAssigned) {
                  const baseName =
                    sample.sampleTypeName || sample.name || `Sample ${idx + 1}`;
                  const qcType = sample.qcMetadata?.qcType;
                  return qcType ? `${baseName} (QC: ${qcType})` : baseName;
                }
                return null;
              })
              .filter(Boolean)
              .join(", ");

            if (unassignedCount > 0) {
              return (
                <>
                  <InlineNotification
                    kind={storageSkipped ? "info" : "warning"}
                    lowContrast
                    hideCloseButton
                    title={
                      storageSkipped
                        ? intl.formatMessage(
                            {
                              id: "storage.skipped.title",
                              defaultMessage:
                                "Storage skipped for {count} sample(s)",
                            },
                            { count: unassignedCount },
                          )
                        : intl.formatMessage({
                            id: "storage.unassigned.title",
                            defaultMessage: "Unassigned Samples",
                          })
                    }
                    subtitle={
                      storageSkipped
                        ? intl.formatMessage({
                            id: "storage.skipAssignment",
                            defaultMessage:
                              "No storage required - samples will be processed immediately",
                          })
                        : unassignedNames
                    }
                    style={{ marginBottom: "1rem" }}
                  />
                  <div style={{ marginTop: "1rem", marginBottom: "1rem" }}>
                    <Checkbox
                      id="skip-storage-checkbox"
                      labelText={intl.formatMessage(
                        {
                          id: "storage.skipRemaining",
                          defaultMessage:
                            "Skip storage for unassigned samples ({count}) - will be processed immediately",
                        },
                        { count: unassignedCount },
                      )}
                      checked={storageSkipped}
                      onChange={(_, { checked }) =>
                        handleStorageSkippedChange(checked)
                      }
                    />
                  </div>
                </>
              );
            }
            return (
              <InlineNotification
                kind="success"
                lowContrast
                hideCloseButton
                title={intl.formatMessage(
                  {
                    id: "storage.allAssigned.title",
                    defaultMessage:
                      "All {count} sample(s) have storage assigned",
                  },
                  { count: assignedCount },
                )}
                style={{ marginBottom: "1rem" }}
              />
            );
          })()}

        {/* Sample Selector for multi-sample orders */}
        {liveSampleCount > 1 && (
          <div className="sample-selector">
            <Select
              id="sample-selector"
              labelText={intl.formatMessage({
                id: "storage.selectSample",
                defaultMessage: "Select Sample",
              })}
              value={selectedSampleIndex}
              onChange={(e) => setSelectedSampleIndex(Number(e.target.value))}
            >
              {samples.map((sample, index) => {
                if (sample.sampleRejected) return null;
                const baseName =
                  sample.sampleTypeName || sample.name || "Sample";
                const qcType = sample.qcMetadata?.qcType;
                const qcSuffix = qcType ? ` [QC: ${qcType}]` : "";
                const assignedSuffix = assignedStorage[index]
                  ? " (Assigned)"
                  : "";
                return (
                  <SelectItem
                    key={index}
                    value={index}
                    text={`${baseName} ${index + 1}${qcSuffix}${assignedSuffix}`}
                  />
                );
              })}
            </Select>
          </div>
        )}

        {/* Sample Info */}
        {samples.length > 0 && (
          <div className="sample-info-bar">
            <span>
              <strong>
                <FormattedMessage
                  id="sample.item.id"
                  defaultMessage="Sample Item ID"
                />
                :
              </strong>{" "}
              {currentSample.sampleItemId || labNumber + "-01"}
            </span>
            <span>
              <strong>
                <FormattedMessage
                  id="sample.type"
                  defaultMessage="Sample Type"
                />
                :
              </strong>{" "}
              {currentSample.sampleTypeName || currentSample.name || "---"}{" "}
              {renderQcTag(currentSample.qcMetadata)}
            </span>
            <span>
              <strong>
                <FormattedMessage id="status" defaultMessage="Status" />:
              </strong>{" "}
              <Tag
                type={assignedStorage[selectedSampleIndex] ? "green" : "gray"}
                size="sm"
              >
                {assignedStorage[selectedSampleIndex] ? (
                  <FormattedMessage
                    id="storage.assigned"
                    defaultMessage="Assigned"
                  />
                ) : (
                  <FormattedMessage
                    id="storage.active"
                    defaultMessage="Active"
                  />
                )}
              </Tag>
            </span>
            {assignedStorage[selectedSampleIndex]?.hierarchicalPath && (
              <span>
                <strong>
                  <FormattedMessage
                    id="storage.currentLocation"
                    defaultMessage="Location"
                  />
                  :
                </strong>{" "}
                {assignedStorage[selectedSampleIndex].hierarchicalPath}
              </span>
            )}
          </div>
        )}

        {/* Storage location. Selection is held locally in
            assignedStorage (keyed by sample index); the actual
            /assign or /move POST fires when the user saves the order
            form — see savePendingStorageAssignments. */}
        <LocationPickerInline
          allowCreate={false}
          onChange={(state) => {
            const deepest = getDeepestLocationSelection(state.selection, {
              requireAssignable: true,
            });
            if (!deepest) return;
            handleLocationChange({
              id: deepest.value.id,
              type: deepest.type,
              hierarchicalPath: selectionToHierarchicalPath(state.selection),
              positionCoordinate: positionToCoordinate(state.position),
            });
          }}
        />

        {/* Condition Notes */}
        <div className="condition-notes-section">
          <TextArea
            id="condition-notes"
            labelText={intl.formatMessage({
              id: "storage.conditionNotes",
              defaultMessage: "Condition Notes (optional)",
            })}
            placeholder={intl.formatMessage({
              id: "storage.conditionNotes.placeholder",
              defaultMessage: "Enter any condition notes...",
            })}
            value={conditionNotes[selectedSampleIndex] || ""}
            onChange={(e) =>
              setConditionNotes((prev) => ({
                ...prev,
                [selectedSampleIndex]: e.target.value,
              }))
            }
            rows={3}
          />
        </div>
      </Tile>

      <OrderReferOutSection />
    </OrderWorkflowLayout>
  );
};

export default OrderLabel;
