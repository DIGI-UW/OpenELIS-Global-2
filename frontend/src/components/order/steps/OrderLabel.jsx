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
  TextInput,
  TextArea,
  Select,
  SelectItem,
  Grid,
  Column,
  Search,
} from "@carbon/react";
import { Printer, Checkmark, Location } from "@carbon/icons-react";
import OrderWorkflowLayout from "../OrderWorkflowLayout";
import { useOrderContext } from "../OrderContext";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import { getFromOpenElisServer, postToOpenElisServer } from "../../utils/Utils";

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
    labNumber,
    stepProgress,
    setStepProgress,
    orderId,
  } = useOrderContext();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

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
  const [storageLocations, setStorageLocations] = useState([]);
  const [selectedLocationId, setSelectedLocationId] = useState("");
  const [positionCoordinate, setPositionCoordinate] = useState("");
  const [conditionNotes, setConditionNotes] = useState("");
  const [barcodeInput, setBarcodeInput] = useState("");
  const [assignedStorage, setAssignedStorage] = useState({});

  // Load storage locations on mount
  useEffect(() => {
    loadStorageLocations();
  }, []);

  const loadStorageLocations = () => {
    // Load available storage locations (devices, shelves, racks)
    getFromOpenElisServer("/rest/storage/locations/search", (response) => {
      if (response && Array.isArray(response)) {
        setStorageLocations(response);
      }
    });
  };

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

    // TODO: Integrate with actual label printing service
    // POST /rest/labels/print with { type, labNumber, quantity }

    setPrintedLabels((prev) => new Set([...prev, labelType]));

    addNotification({
      kind: NotificationKinds.success,
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage(
        {
          id: "label.print.success.count",
          defaultMessage: "{count} label(s) printed successfully",
        },
        { count: quantity },
      ),
    });
    setNotificationVisible(true);
  };

  const handlePrintAllLabels = () => {
    // Print all labels with quantity > 0
    Object.entries(labelQuantities).forEach(([type, quantity]) => {
      if (quantity > 0) {
        handlePrintLabel(type);
      }
    });
  };

  const handleBarcodeSubmit = (e) => {
    if (e.key === "Enter" && barcodeInput.trim()) {
      // Quick assign via barcode
      handleAssignStorage(barcodeInput.trim());
      setBarcodeInput("");
    }
  };

  const handleAssignStorage = (locationIdOrBarcode) => {
    const sampleItemId = currentSample.sampleItemId;
    if (!sampleItemId) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "storage.assign.error.noSample",
          defaultMessage: "No sample selected for storage assignment",
        }),
      });
      setNotificationVisible(true);
      return;
    }

    const assignmentData = {
      sampleItemId: sampleItemId,
      locationId: locationIdOrBarcode || selectedLocationId,
      locationType: "device", // Default, could be determined by barcode prefix
      positionCoordinate: positionCoordinate,
      notes: conditionNotes,
    };

    postToOpenElisServer(
      "/rest/storage/sample-items/assign",
      JSON.stringify(assignmentData),
      (response) => {
        if (response && !response.error) {
          // Update local state
          setAssignedStorage((prev) => ({
            ...prev,
            [selectedSampleIndex]: {
              locationId: assignmentData.locationId,
              position: positionCoordinate,
              notes: conditionNotes,
            },
          }));

          // Update sample with storage info
          const updatedSamples = [...samples];
          updatedSamples[selectedSampleIndex] = {
            ...updatedSamples[selectedSampleIndex],
            storageLocationId: assignmentData.locationId,
            storagePositionCoordinate: positionCoordinate,
          };
          setSamples(updatedSamples);

          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "storage.assign.success",
              defaultMessage: "Storage location assigned successfully",
            }),
          });
          setNotificationVisible(true);

          // Clear form
          setPositionCoordinate("");
          setConditionNotes("");
        } else {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message:
              response?.message ||
              intl.formatMessage({
                id: "storage.assign.error",
                defaultMessage: "Failed to assign storage location",
              }),
          });
          setNotificationVisible(true);
        }
      },
    );
  };

  const handleSave = async () => {
    try {
      await saveOrder();
      // Update step progress
      setStepProgress((prev) => ({ ...prev, label: true }));
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
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
    }
  };

  const handleSaveAndNext = async () => {
    try {
      await saveOrder();
      setStepProgress((prev) => ({ ...prev, label: true }));
      setCurrentStep(3);
      history.push("/order/qa");
    } catch (error) {
      addNotification({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "server.error.msg" }),
      });
      setNotificationVisible(true);
    }
  };

  // Check if we can proceed (at least order label printed)
  const canProceed = printedLabels.has("order") || printedLabels.has("sample");

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
              {currentSample.sampleTypeName || "Serum"}
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
          </div>
        )}

        {/* Quick Assign via Barcode */}
        <div className="quick-assign-section">
          <h6>
            <FormattedMessage
              id="storage.quickAssign.title"
              defaultMessage="Quick Assign (Barcode)"
            />
          </h6>
          <p className="helper-text">
            <FormattedMessage
              id="storage.quickAssign.description"
              defaultMessage="Scan Barcode"
            />
          </p>
          <TextInput
            id="barcode-input"
            placeholder={intl.formatMessage({
              id: "storage.quickAssign.placeholder",
              defaultMessage: "Enter barcode...",
            })}
            value={barcodeInput}
            onChange={(e) => setBarcodeInput(e.target.value)}
            onKeyDown={handleBarcodeSubmit}
          />
        </div>

        <Grid>
          <Column lg={8} md={4} sm={4}>
            {/* Select Location */}
            <div className="form-field">
              <label className="required-label">
                <FormattedMessage
                  id="storage.selectLocation"
                  defaultMessage="Select Location"
                />
                *
              </label>
              <Search
                id="location-search"
                placeholder={intl.formatMessage({
                  id: "storage.searchLocation.placeholder",
                  defaultMessage: "Search for location...",
                })}
                labelText=""
                size="md"
              />
              <Select
                id="location-select"
                labelText=""
                value={selectedLocationId}
                onChange={(e) => setSelectedLocationId(e.target.value)}
              >
                <SelectItem value="" text="" />
                {storageLocations.map((loc) => (
                  <SelectItem
                    key={loc.id}
                    value={loc.id}
                    text={loc.hierarchicalPath || loc.name}
                  />
                ))}
              </Select>
            </div>
          </Column>

          <Column lg={4} md={2} sm={2}>
            {/* Position */}
            <TextInput
              id="position-input"
              labelText={intl.formatMessage({
                id: "storage.position",
                defaultMessage: "Position (optional)",
              })}
              placeholder="e.g. A6, 1-1, R65-12"
              value={positionCoordinate}
              onChange={(e) => setPositionCoordinate(e.target.value)}
            />
          </Column>

          <Column lg={4} md={2} sm={2}>
            {/* Location Button */}
            <Button
              kind="primary"
              renderIcon={Location}
              onClick={() => handleAssignStorage()}
              disabled={!selectedLocationId}
              className="location-button"
            >
              <FormattedMessage
                id="storage.location"
                defaultMessage="Location"
              />
            </Button>
          </Column>
        </Grid>

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
            value={conditionNotes}
            onChange={(e) => setConditionNotes(e.target.value)}
            rows={3}
          />
        </div>

        <p className="storage-info-text">
          <FormattedMessage
            id="storage.info.text"
            defaultMessage="Storage locations are assigned when you click Save or Save & Next below. For complete Assign Archive function review."
          />
        </p>
      </Tile>
    </OrderWorkflowLayout>
  );
};

export default OrderLabel;
