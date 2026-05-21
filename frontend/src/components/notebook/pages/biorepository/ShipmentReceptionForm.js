import React, { useState, useCallback, useEffect } from "react";
import {
  Form,
  FormGroup,
  TextInput,
  TextArea,
  NumberInput,
  RadioButtonGroup,
  RadioButton,
  FileUploader,
  Button,
  InlineNotification,
  Loading,
  Grid,
  Column,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Tag,
  Modal,
} from "@carbon/react";
import { Add, ArrowRight } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import PropTypes from "prop-types";
import {
  postToOpenElisServerJsonResponse,
  getFromOpenElisServer,
} from "../../../utils/Utils";

/**
 * ShipmentReceptionForm - Form for receiving incoming shipments
 * Sub-stage 1a of the Biorepository Intake workflow
 *
 * Features:
 * - View existing shipments in a table
 * - Receive new shipments
 * - Select existing shipment to continue workflow
 *
 * @param {Object} props
 * @param {Function} props.onShipmentCreated - Callback when shipment is successfully created
 * @param {Function} props.onShipmentSelected - Callback when an existing shipment is selected
 * @param {Function} props.onCancel - Callback to cancel the form
 */
function ShipmentReceptionForm({
  onShipmentCreated,
  onShipmentSelected,
  onCancel,
}) {
  const intl = useIntl();

  // View state: 'list' or 'form'
  const [viewMode, setViewMode] = useState("list");

  // Existing shipments
  const [shipments, setShipments] = useState([]);
  const [loadingShipments, setLoadingShipments] = useState(true);

  // Form state
  const [formData, setFormData] = useState({
    deliveryReference: "",
    senderName: "",
    senderOrganization: "",
    packagingCondition: "INTACT",
    packagingConditionNotes: "",
    transportTemperature: null,
    expectedSampleCount: null,
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [photoFile, setPhotoFile] = useState(null);

  // Validation state
  const [errors, setErrors] = useState({});

  // Load existing shipments
  useEffect(() => {
    loadShipments();
  }, []);

  const loadShipments = useCallback(() => {
    setLoadingShipments(true);
    getFromOpenElisServer(
      "/rest/biorepository/shipment?limit=50",
      (data) => {
        if (data && Array.isArray(data)) {
          setShipments(data);
        } else if (data && data.shipments) {
          setShipments(data.shipments);
        }
        setLoadingShipments(false);
      },
      (err) => {
        console.error("Failed to load shipments:", err);
        setLoadingShipments(false);
      },
    );
  }, []);

  const validateForm = useCallback(() => {
    const newErrors = {};

    if (!formData.deliveryReference.trim()) {
      newErrors.deliveryReference = intl.formatMessage({
        id: "biorepository.shipment.error.deliveryReference.required",
        defaultMessage: "Delivery reference is required",
      });
    }

    if (!formData.senderName.trim()) {
      newErrors.senderName = intl.formatMessage({
        id: "biorepository.shipment.error.senderName.required",
        defaultMessage: "Sender name is required",
      });
    }

    if (
      formData.packagingCondition === "DAMAGED" &&
      !formData.packagingConditionNotes.trim()
    ) {
      newErrors.packagingConditionNotes = intl.formatMessage({
        id: "biorepository.shipment.error.packagingNotes.required",
        defaultMessage: "Please describe the packaging damage",
      });
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  }, [formData, intl]);

  const handleInputChange = useCallback(
    (field, value) => {
      setFormData((prev) => ({
        ...prev,
        [field]: value,
      }));
      if (errors[field]) {
        setErrors((prev) => {
          const newErrors = { ...prev };
          delete newErrors[field];
          return newErrors;
        });
      }
    },
    [errors],
  );

  const handleSubmit = useCallback(
    (e) => {
      e.preventDefault();

      if (!validateForm()) {
        return;
      }

      setLoading(true);
      setError(null);

      const shipmentData = {
        deliveryReference: formData.deliveryReference.trim(),
        senderName: formData.senderName.trim(),
        senderOrganization: formData.senderOrganization.trim() || null,
        packagingCondition: formData.packagingCondition,
        packagingConditionNotes:
          formData.packagingConditionNotes.trim() || null,
        transportTemperature: formData.transportTemperature,
        expectedSampleCount: formData.expectedSampleCount,
      };

      postToOpenElisServerJsonResponse(
        "/rest/biorepository/shipment/receive",
        JSON.stringify(shipmentData),
        (response) => {
          setLoading(false);
          if (response.error) {
            setError(response.error);
          } else {
            // Notify parent first — this triggers a tab switch which
            // unmounts this component, making local state updates unnecessary
            if (onShipmentCreated) {
              onShipmentCreated(response);
              return;
            }
            // Only do local cleanup if there's no parent callback
            loadShipments();
            setViewMode("list");
            setFormData({
              deliveryReference: "",
              senderName: "",
              senderOrganization: "",
              packagingCondition: "INTACT",
              packagingConditionNotes: "",
              transportTemperature: null,
              expectedSampleCount: null,
            });
          }
        },
      );
    },
    [formData, validateForm, onShipmentCreated, intl, loadShipments],
  );

  const handleFileChange = useCallback((event) => {
    const file = event.target.files?.[0];
    if (file) {
      setPhotoFile(file);
    }
  }, []);

  const handleSelectShipment = useCallback(
    (shipment) => {
      if (onShipmentSelected) {
        onShipmentSelected(shipment);
      }
    },
    [onShipmentSelected],
  );

  const getStatusTag = (status) => {
    const statusColors = {
      RECEIVED: "blue",
      PROCESSING: "cyan",
      COMPLETED: "green",
      CANCELLED: "red",
    };
    return (
      <Tag type={statusColors[status] || "gray"} size="sm">
        {status}
      </Tag>
    );
  };

  const getConditionTag = (condition) => {
    return (
      <Tag type={condition === "INTACT" ? "green" : "red"} size="sm">
        {condition}
      </Tag>
    );
  };

  const formatDate = (dateString) => {
    if (!dateString) return "-";
    try {
      return new Date(dateString).toLocaleString();
    } catch {
      return dateString;
    }
  };

  // Table headers
  const headers = [
    {
      key: "deliveryReference",
      header: intl.formatMessage({
        id: "biorepository.shipment.table.deliveryRef",
        defaultMessage: "Delivery Ref",
      }),
    },
    {
      key: "senderName",
      header: intl.formatMessage({
        id: "biorepository.shipment.table.sender",
        defaultMessage: "Sender",
      }),
    },
    {
      key: "senderOrganization",
      header: intl.formatMessage({
        id: "biorepository.shipment.table.organization",
        defaultMessage: "Organization",
      }),
    },
    {
      key: "packagingCondition",
      header: intl.formatMessage({
        id: "biorepository.shipment.table.condition",
        defaultMessage: "Condition",
      }),
    },
    {
      key: "expectedSampleCount",
      header: intl.formatMessage({
        id: "biorepository.shipment.table.samples",
        defaultMessage: "Expected Samples",
      }),
    },
    {
      key: "status",
      header: intl.formatMessage({
        id: "biorepository.shipment.table.status",
        defaultMessage: "Status",
      }),
    },
    {
      key: "receptionTimestamp",
      header: intl.formatMessage({
        id: "biorepository.shipment.table.received",
        defaultMessage: "Received",
      }),
    },
    {
      key: "actions",
      header: intl.formatMessage({
        id: "biorepository.shipment.table.actions",
        defaultMessage: "Actions",
      }),
    },
  ];

  // Transform shipments for DataTable
  const rows = shipments.map((shipment) => ({
    id: String(shipment.id),
    deliveryReference: shipment.deliveryReference,
    senderName: shipment.senderName,
    senderOrganization: shipment.senderOrganization || "-",
    packagingCondition: shipment.packagingCondition,
    expectedSampleCount: shipment.expectedSampleCount ?? "-",
    status: shipment.status,
    receptionTimestamp: shipment.receptionTimestamp,
    _original: shipment,
  }));

  // List view with existing shipments
  if (viewMode === "list") {
    return (
      <div className="shipment-reception-list">
        {loadingShipments && <Loading withOverlay description="Loading..." />}

        <Grid>
          <Column lg={16} md={8} sm={4}>
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                marginBottom: "1rem",
              }}
            >
              <h4>
                <FormattedMessage
                  id="biorepository.shipment.list.title"
                  defaultMessage="Recent Shipments"
                />
              </h4>
              <Button
                kind="primary"
                size="md"
                renderIcon={Add}
                onClick={() => setViewMode("form")}
              >
                <FormattedMessage
                  id="biorepository.shipment.button.newShipment"
                  defaultMessage="Receive New Shipment"
                />
              </Button>
            </div>
          </Column>

          <Column lg={16} md={8} sm={4}>
            {shipments.length === 0 && !loadingShipments ? (
              <InlineNotification
                kind="info"
                title={intl.formatMessage({
                  id: "biorepository.shipment.list.empty.title",
                  defaultMessage: "No Shipments",
                })}
                subtitle={intl.formatMessage({
                  id: "biorepository.shipment.list.empty.message",
                  defaultMessage:
                    "No shipments have been received yet. Click 'Receive New Shipment' to get started.",
                })}
                lowContrast
                hideCloseButton
              />
            ) : (
              <DataTable rows={rows} headers={headers} isSortable>
                {({
                  rows,
                  headers,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                }) => (
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
                      {rows.map((row) => {
                        const originalShipment = shipments.find(
                          (s) => String(s.id) === row.id,
                        );
                        return (
                          <TableRow key={row.id} {...getRowProps({ row })}>
                            {row.cells.map((cell) => {
                              if (cell.info.header === "packagingCondition") {
                                return (
                                  <TableCell key={cell.id}>
                                    {getConditionTag(cell.value)}
                                  </TableCell>
                                );
                              }
                              if (cell.info.header === "status") {
                                return (
                                  <TableCell key={cell.id}>
                                    {getStatusTag(cell.value)}
                                  </TableCell>
                                );
                              }
                              if (cell.info.header === "receptionTimestamp") {
                                return (
                                  <TableCell key={cell.id}>
                                    {formatDate(cell.value)}
                                  </TableCell>
                                );
                              }
                              if (cell.info.header === "actions") {
                                return (
                                  <TableCell key={cell.id}>
                                    <Button
                                      kind="ghost"
                                      size="sm"
                                      renderIcon={ArrowRight}
                                      onClick={() =>
                                        handleSelectShipment(originalShipment)
                                      }
                                    >
                                      <FormattedMessage
                                        id="biorepository.shipment.button.continue"
                                        defaultMessage="Continue"
                                      />
                                    </Button>
                                  </TableCell>
                                );
                              }
                              return (
                                <TableCell key={cell.id}>
                                  {cell.value}
                                </TableCell>
                              );
                            })}
                          </TableRow>
                        );
                      })}
                    </TableBody>
                  </Table>
                )}
              </DataTable>
            )}
          </Column>
        </Grid>
      </div>
    );
  }

  // Form view for receiving new shipment
  return (
    <Form onSubmit={handleSubmit} className="shipment-reception-form">
      {loading && <Loading withOverlay description="Processing..." />}

      <Grid>
        <Column lg={16} md={8} sm={4}>
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              marginBottom: "1rem",
            }}
          >
            <h4>
              <FormattedMessage
                id="biorepository.shipment.form.title"
                defaultMessage="Receive New Shipment"
              />
            </h4>
            <Button kind="ghost" size="sm" onClick={() => setViewMode("list")}>
              <FormattedMessage
                id="biorepository.shipment.button.backToList"
                defaultMessage="Back to List"
              />
            </Button>
          </div>
        </Column>
      </Grid>

      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "biorepository.shipment.error.title",
            defaultMessage: "Error",
          })}
          subtitle={error}
          lowContrast
          onClose={() => setError(null)}
        />
      )}

      <Grid>
        <Column lg={8} md={4} sm={4}>
          <FormGroup legendText="">
            <TextInput
              id="deliveryReference"
              labelText={intl.formatMessage({
                id: "biorepository.shipment.field.deliveryReference",
                defaultMessage: "Delivery Reference *",
              })}
              placeholder={intl.formatMessage({
                id: "biorepository.shipment.field.deliveryReference.placeholder",
                defaultMessage: "Enter tracking number or delivery reference",
              })}
              value={formData.deliveryReference}
              onChange={(e) =>
                handleInputChange("deliveryReference", e.target.value)
              }
              invalid={!!errors.deliveryReference}
              invalidText={errors.deliveryReference}
            />
          </FormGroup>
        </Column>

        <Column lg={8} md={4} sm={4}>
          <FormGroup legendText="">
            <TextInput
              id="senderName"
              labelText={intl.formatMessage({
                id: "biorepository.shipment.field.senderName",
                defaultMessage: "Sender Name *",
              })}
              placeholder={intl.formatMessage({
                id: "biorepository.shipment.field.senderName.placeholder",
                defaultMessage: "Enter sender's name",
              })}
              value={formData.senderName}
              onChange={(e) => handleInputChange("senderName", e.target.value)}
              invalid={!!errors.senderName}
              invalidText={errors.senderName}
            />
          </FormGroup>
        </Column>

        <Column lg={8} md={4} sm={4}>
          <FormGroup legendText="">
            <TextInput
              id="senderOrganization"
              labelText={intl.formatMessage({
                id: "biorepository.shipment.field.senderOrganization",
                defaultMessage: "Sender Organization",
              })}
              placeholder={intl.formatMessage({
                id: "biorepository.shipment.field.senderOrganization.placeholder",
                defaultMessage: "Enter sending organization (optional)",
              })}
              value={formData.senderOrganization}
              onChange={(e) =>
                handleInputChange("senderOrganization", e.target.value)
              }
            />
          </FormGroup>
        </Column>

        <Column lg={8} md={4} sm={4}>
          <FormGroup legendText="">
            <NumberInput
              id="expectedSampleCount"
              label={intl.formatMessage({
                id: "biorepository.shipment.field.expectedSampleCount",
                defaultMessage: "Expected Sample Count",
              })}
              min={0}
              value={formData.expectedSampleCount || ""}
              onChange={(e, { value }) =>
                handleInputChange("expectedSampleCount", value)
              }
              allowEmpty
            />
          </FormGroup>
        </Column>

        <Column lg={8} md={4} sm={4}>
          <FormGroup legendText="">
            <NumberInput
              id="transportTemperature"
              label={intl.formatMessage({
                id: "biorepository.shipment.field.transportTemperature",
                defaultMessage: "Transport Temperature (°C)",
              })}
              step={0.1}
              value={formData.transportTemperature || ""}
              onChange={(e, { value }) =>
                handleInputChange("transportTemperature", value)
              }
              allowEmpty
            />
          </FormGroup>
        </Column>

        <Column lg={16} md={8} sm={4}>
          <FormGroup
            legendText={intl.formatMessage({
              id: "biorepository.shipment.field.packagingCondition",
              defaultMessage: "Packaging Condition *",
            })}
          >
            <RadioButtonGroup
              name="packagingCondition"
              valueSelected={formData.packagingCondition}
              onChange={(value) =>
                handleInputChange("packagingCondition", value)
              }
              orientation="horizontal"
            >
              <RadioButton
                id="packaging-intact"
                labelText={intl.formatMessage({
                  id: "biorepository.shipment.packagingCondition.intact",
                  defaultMessage: "Intact",
                })}
                value="INTACT"
              />
              <RadioButton
                id="packaging-damaged"
                labelText={intl.formatMessage({
                  id: "biorepository.shipment.packagingCondition.damaged",
                  defaultMessage: "Damaged",
                })}
                value="DAMAGED"
              />
            </RadioButtonGroup>
          </FormGroup>
        </Column>

        {formData.packagingCondition === "DAMAGED" && (
          <>
            <Column lg={16} md={8} sm={4}>
              <FormGroup legendText="">
                <TextArea
                  id="packagingConditionNotes"
                  labelText={intl.formatMessage({
                    id: "biorepository.shipment.field.packagingNotes",
                    defaultMessage: "Packaging Damage Description *",
                  })}
                  placeholder={intl.formatMessage({
                    id: "biorepository.shipment.field.packagingNotes.placeholder",
                    defaultMessage: "Describe the packaging damage in detail",
                  })}
                  value={formData.packagingConditionNotes}
                  onChange={(e) =>
                    handleInputChange("packagingConditionNotes", e.target.value)
                  }
                  invalid={!!errors.packagingConditionNotes}
                  invalidText={errors.packagingConditionNotes}
                  rows={3}
                />
              </FormGroup>
            </Column>

            <Column lg={16} md={8} sm={4}>
              <FormGroup legendText="">
                <FileUploader
                  labelTitle={intl.formatMessage({
                    id: "biorepository.shipment.field.packagingPhoto",
                    defaultMessage: "Packaging Photo",
                  })}
                  labelDescription={intl.formatMessage({
                    id: "biorepository.shipment.field.packagingPhoto.description",
                    defaultMessage:
                      "Upload a photo of the damaged packaging (optional)",
                  })}
                  buttonLabel={intl.formatMessage({
                    id: "biorepository.shipment.field.packagingPhoto.button",
                    defaultMessage: "Add file",
                  })}
                  accept={[".jpg", ".jpeg", ".png"]}
                  multiple={false}
                  onChange={handleFileChange}
                />
              </FormGroup>
            </Column>
          </>
        )}

        <Column lg={16} md={8} sm={4}>
          <div
            className="form-actions"
            style={{ marginTop: "1rem", display: "flex", gap: "1rem" }}
          >
            <Button type="submit" disabled={loading}>
              <FormattedMessage
                id="biorepository.shipment.button.receive"
                defaultMessage="Receive Shipment"
              />
            </Button>
            <Button
              kind="secondary"
              onClick={() => setViewMode("list")}
              disabled={loading}
            >
              <FormattedMessage
                id="biorepository.button.cancel"
                defaultMessage="Cancel"
              />
            </Button>
          </div>
        </Column>
      </Grid>
    </Form>
  );
}

ShipmentReceptionForm.propTypes = {
  onShipmentCreated: PropTypes.func,
  onShipmentSelected: PropTypes.func,
  onCancel: PropTypes.func,
};

export default ShipmentReceptionForm;
