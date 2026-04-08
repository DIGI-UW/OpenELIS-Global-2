import React, { useContext, useState, useEffect } from "react";
import { useHistory } from "react-router-dom";
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
import StorageLocationSelector from "../../storage/StorageLocationSelector/StorageLocationSelector";
import {
  postToOpenElisServerJsonResponse,
  patchToOpenElisServerJsonResponse,
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
  const {
    orderData,
    samples,
    setSamples,
    saveOrder,
    setCurrentStep,
    labNumber: contextLabNumber,
    stepProgress,
    markStepComplete,
    orderId,
    storageSkipped,
    setStorageSkipped,
  } = useOrderContext();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  // Get labNumber from context or orderData
  const labNumber =
    contextLabNumber || orderData?.sampleOrderItems?.labNo || null;

  // Redirect to enter step if no order is loaded
  useEffect(() => {
    if (!orderId && !labNumber) {
      history.replace("/order/enter");
    }
  }, [orderId, labNumber, history]);

  // Label printing state - order label + one entry per sample
  const [labelQuantities, setLabelQuantities] = useState(() => {
    const initial = { order: 1 };
    samples.forEach((_, index) => {
      initial[`sample-${index}`] = 1;
    });
    return initial;
  });
  const [printedLabels, setPrintedLabels] = useState(new Set());

  // Update label quantities when samples change
  useEffect(() => {
    setLabelQuantities((prev) => {
      const updated = { order: prev.order || 1 };
      samples.forEach((_, index) => {
        updated[`sample-${index}`] = prev[`sample-${index}`] || 1;
      });
      return updated;
    });
  }, [samples]);

  // Storage assignment state
  const [selectedSampleIndex, setSelectedSampleIndex] = useState(0);
  const [assignedStorage, setAssignedStorage] = useState({});
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

    // If label step was completed but no samples have storage, it means storage was skipped
    if (
      stepProgress.label &&
      samples.length > 0 &&
      !hasAnyStorage &&
      !storageSkipped
    ) {
      setStorageSkipped(true);
    }
  }, [samples, stepProgress.label, storageSkipped, setStorageSkipped]);

  // Get current sample being configured
  const currentSample = samples[selectedSampleIndex] || {};

  // Build patient info for order label
  const patientName = orderData?.patientProperties
    ? `${orderData.patientProperties.lastName || ""}, ${orderData.patientProperties.firstName || ""}`.trim()
    : "---";

  // Build label rows - Order label + one row per sample
  const labelRows = [
    {
      id: "order",
      name: intl.formatMessage({
        id: "label.type.order",
        defaultMessage: "Order Label",
      }),
      content: `Lab Nr: ${labNumber || "---"} | Patient: ${patientName}`,
      barcode: labNumber || "---",
    },
    // Add a row for each sample
    ...samples.map((sample, index) => ({
      id: `sample-${index}`,
      name: `${intl.formatMessage({
        id: "label.type.sample",
        defaultMessage: "Sample Label",
      })} ${index + 1}`,
      content: `${sample.sampleTypeName || "Sample"} | ${sample.collectionDate || "---"}`,
      barcode: sample.sampleItemId || `${labNumber}-${index + 1}`,
    })),
  ];

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

    if (labelType === "order") {
      // Print order label only
      url = `/LabelMakerServlet?labNo=${encodeURIComponent(labNumber)}&type=order&quantity=${quantity}`;
    } else if (labelType.startsWith("sample-")) {
      // Print specimen label for specific sample
      // Extract sample index from labelType (e.g., "sample-0" -> 0)
      const sampleIndex = parseInt(labelType.replace("sample-", ""), 10);
      const sample = samples[sampleIndex];

      // For specimen labels, we need labNo.sortOrder format (e.g., DEV01260000000000001.1)
      // sortOrder is 1-based in backend
      const sortOrder = sample?.sortOrder || sampleIndex + 1;
      const specimenLabNo = `${labNumber}.${sortOrder}`;

      url = `/LabelMakerServlet?labNo=${encodeURIComponent(specimenLabNo)}&type=specimen&quantity=${quantity}`;
    } else {
      // Fallback to default (prints both order and all specimen labels)
      url = `/LabelMakerServlet?labNo=${encodeURIComponent(labNumber)}&type=default&quantity=${quantity}`;
    }

    // Open label PDF in new window
    window.open(url, "_blank");

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

  const handlePrintAllLabels = () => {
    // Use 'default' type which prints both order label and all specimen labels in one PDF
    // Add override=true to bypass max print checks
    const totalQuantity = Math.max(labelQuantities.order || 1, 1);
    const url = `/LabelMakerServlet?labNo=${encodeURIComponent(labNumber)}&type=default&quantity=${totalQuantity}&override=true`;
    window.open(url, "_blank");

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
   * Handle location change from StorageLocationSelector
   * Stores selection locally - actual API call happens on Save
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
   */
  const savePendingStorageAssignments = async () => {
    const pendingAssignments = Object.entries(assignedStorage).filter(
      ([, storage]) => storage.pending,
    );

    for (const [sampleIndexStr, storage] of pendingAssignments) {
      const sampleIndex = parseInt(sampleIndexStr, 10);
      const currentSampleItem = samples[sampleIndex];
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
      // First save any pending storage assignments
      await savePendingStorageAssignments();

      // Update notes for existing assignments (if notes changed)
      await updateStorageNotes();

      await saveOrder();
      // Update step progress
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
      // First save any pending storage assignments
      await savePendingStorageAssignments();

      // Update notes for existing assignments (if notes changed)
      await updateStorageNotes();

      await saveOrder();
      markStepComplete("label");
      setCurrentStep(3);
      history.push("/order/qa");
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

  // Check if we can proceed:
  // - At least one label printed AND
  // - Either all samples have storage OR user has checked "skip storage"
  const canProceed =
    (printedLabels.has("order") || printedLabels.has("sample")) &&
    (allSamplesHaveStorage || storageSkipped);

  // Don't render if no order loaded (will redirect)
  if (!orderId && !labNumber) {
    return null;
  }

  return (
    <OrderWorkflowLayout
      currentStep={2}
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
                  return (
                    <TableRow key={row.id} {...getRowProps({ row })}>
                      <TableCell>{labelRow?.name}</TableCell>
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
                    </TableRow>
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
              (s, idx) => s.storageLocationId || assignedStorage[idx],
            ).length;
            const unassignedCount = samples.length - assignedCount;
            const unassignedNames = samples
              .map((sample, idx) => {
                const isAssigned =
                  sample.storageLocationId || assignedStorage[idx];
                if (!isAssigned) {
                  return sample.sampleTypeName || `Sample ${idx + 1}`;
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
                    title={intl.formatMessage(
                      {
                        id: "storage.unassigned.title",
                        defaultMessage:
                          "{count} sample(s) without storage assignment",
                      },
                      { count: unassignedCount },
                    )}
                    subtitle={unassignedNames}
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
                      onChange={(_, { checked }) => setStorageSkipped(checked)}
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
        {samples.length > 1 && (
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
              {samples.map((sample, index) => (
                <SelectItem
                  key={index}
                  value={index}
                  text={`${sample.sampleTypeName || "Sample"} ${index + 1}${
                    assignedStorage[index] ? " (Assigned)" : ""
                  }`}
                />
              ))}
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
              {currentSample.sampleTypeName || "---"}
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

        {/* Storage Location Selector - inline mode (dropdown/autocomplete) */}
        <StorageLocationSelector
          mode="autocomplete"
          onLocationChange={handleLocationChange}
          enableInlineCreation={false}
          optional={true}
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
    </OrderWorkflowLayout>
  );
};

export default OrderLabel;
