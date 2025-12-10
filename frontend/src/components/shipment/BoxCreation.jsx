import { Document, Scan, TrashCan } from "@carbon/icons-react";
import {
  Button,
  Column,
  DataTable,
  Dropdown,
  Grid,
  InlineNotification,
  Loading,
  Modal,
  NumberInput,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  TextArea,
  TextInput,
  Tile,
} from "@carbon/react";
import { useContext, useEffect, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import { useHistory } from "react-router-dom";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog } from "../common/CustomNotification";
import PageBreadCrumb from "../common/PageBreadCrumb";
import ShipmentNavigation from "./ShipmentNavigation";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";
import config from "../../config.json";
import "./BoxCreation.css";

const BoxCreation = () => {
  const intl = useIntl();
  const history = useHistory();
  const { addNotification } = useContext(NotificationContext);

  // Auto-generated box number
  const [boxNumber, setBoxNumber] = useState("");
  const [loading, setLoading] = useState(false);
  const [facilities, setFacilities] = useState([]);

  // Form state
  const [selectedFacility, setSelectedFacility] = useState(null);
  const [selectedTemperature, setSelectedTemperature] = useState(null);
  const [capacity, setCapacity] = useState(25);
  const [notes, setNotes] = useState("");

  // Sample management
  const [sampleSearchTerm, setSampleSearchTerm] = useState("");
  const [addedSamples, setAddedSamples] = useState([]);
  const [searchResults, setSearchResults] = useState([]);
  const [searching, setSearching] = useState(false);

  // Reject sample modal
  const [showRejectModal, setShowRejectModal] = useState(false);
  const [sampleToReject, setSampleToReject] = useState(null);
  const [rejectionReason, setRejectionReason] = useState(null);

  // Validation messages
  const [validationMessages, setValidationMessages] = useState([]);

  // Temperature options
  const temperatureOptions = [
    {
      id: "AMBIENT",
      label: intl.formatMessage({ id: "shipment.temperature.ambient" }),
    },
    {
      id: "REFRIGERATED",
      label: intl.formatMessage({ id: "shipment.temperature.refrigerated" }),
    },
    {
      id: "FROZEN",
      label: intl.formatMessage({ id: "shipment.temperature.frozen" }),
    },
    {
      id: "DEEP_FROZEN",
      label: intl.formatMessage({ id: "shipment.temperature.deepFrozen" }),
    },
    {
      id: "DRY_ICE",
      label: intl.formatMessage({ id: "shipment.temperature.dryIce" }),
    },
  ];

  // Rejection reasons
  const rejectionReasons = [
    {
      id: "DAMAGED",
      label: intl.formatMessage({ id: "shipment.rejection.damaged" }),
    },
    {
      id: "INSUFFICIENT_VOLUME",
      label: intl.formatMessage({
        id: "shipment.rejection.insufficientVolume",
      }),
    },
    {
      id: "WRONG_SAMPLE_TYPE",
      label: intl.formatMessage({ id: "shipment.rejection.wrongType" }),
    },
    {
      id: "EXPIRED",
      label: intl.formatMessage({ id: "shipment.rejection.expired" }),
    },
    {
      id: "OTHER",
      label: intl.formatMessage({ id: "shipment.rejection.other" }),
    },
  ];

  useEffect(() => {
    fetchFacilities();
    generateBoxNumber();
  }, []);

  useEffect(() => {
    validateForm();
  }, [selectedFacility, addedSamples]);

  const generateBoxNumber = () => {
    const year = new Date().getFullYear();
    const random = Math.floor(Math.random() * 1000)
      .toString()
      .padStart(3, "0");
    setBoxNumber(`BOX-${year}-${random}`);
  };

  const fetchFacilities = () => {
    getFromOpenElisServer(
      "/rest/displayList/REFERRAL_ORGANIZATIONS",
      (response) => {
        if (response) {
          setFacilities(response);
        }
      },
    );
  };

  const validateForm = () => {
    const messages = [];

    if (!selectedFacility) {
      messages.push(
        intl.formatMessage({ id: "shipment.validation.selectDestination" }),
      );
    }

    if (addedSamples.length === 0) {
      messages.push(
        intl.formatMessage({ id: "shipment.validation.addSample" }),
      );
    }

    setValidationMessages(messages);
    return messages.length === 0;
  };

  const handleSearchSample = async () => {
    if (!sampleSearchTerm.trim()) {
      return;
    }

    setSearching(true);

    try {
      // Search for sample by accession number
      const response = await fetch(
        `${config.serverBaseUrl}/rest/sample/by-accession/${sampleSearchTerm}`,
        {
          credentials: "include",
          method: "GET",
          headers: {
            "Content-Type": "application/json",
          },
        },
      );

      setSearching(false);

      // Handle 404 - Sample not found
      if (response.status === 404) {
        const message = intl.formatMessage({ id: "shipment.error.sampleNotFound" });
        console.log("404 detected - showing notification:", message);
        addNotification({
          kind: "error",
          title: intl.formatMessage({ id: "notification.error" }),
          message: `${message}: ${sampleSearchTerm}`,
        });
        return;
      }

      // Handle other errors
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const data = await response.json();

      if (!data) {
        addNotification({
          kind: "error",
          title: intl.formatMessage({ id: "notification.error" }),
          message: intl.formatMessage({ id: "shipment.error.sampleNotFound" }),
        });
        return;
      }

      // Check if sample is already added
      if (
        addedSamples.find((s) => s.accessionNumber === data.accessionNumber)
      ) {
        addNotification({
          kind: "warning",
          title: intl.formatMessage({ id: "notification.warning" }),
          message: intl.formatMessage({
            id: "shipment.error.sampleAlreadyInBox",
          }),
        });
        return;
      }

      // Add sample to the box
      setAddedSamples([
        ...addedSamples,
        {
          id: data.id,
          accessionNumber: data.accessionNumber,
          sampleType: data.sampleType || "-",
          referralTest: data.referralTest || "-",
        },
      ]);

      setSampleSearchTerm("");

      addNotification({
        kind: "success",
        title: intl.formatMessage({ id: "notification.success" }),
        message: intl.formatMessage({
          id: "shipment.notification.sampleAdded",
        }),
      });
    } catch (error) {
      console.error("Error searching sample:", error);
      setSearching(false);
      addNotification({
        kind: "error",
        title: intl.formatMessage({ id: "notification.error" }),
        message: intl.formatMessage({ id: "shipment.error.searchFailed" }),
      });
    }
  };

  const handleRemoveSample = (sampleId) => {
    setAddedSamples(addedSamples.filter((s) => s.id !== sampleId));
  };

  const handleRejectSample = (sample) => {
    setSampleToReject(sample);
    setShowRejectModal(true);
  };

  const confirmRejectSample = () => {
    if (sampleToReject && rejectionReason) {
      handleRemoveSample(sampleToReject.id);

      addNotification({
        kind: "info",
        title: intl.formatMessage({ id: "notification.info" }),
        message: intl.formatMessage(
          { id: "shipment.notification.sampleRejected" },
          {
            accession: sampleToReject.accessionNumber,
            reason: rejectionReason.label,
          },
        ),
      });

      setShowRejectModal(false);
      setSampleToReject(null);
      setRejectionReason(null);
    }
  };

  const handleSaveBox = async () => {
    if (!validateForm()) {
      return;
    }

    setLoading(true);

    try {
      const boxData = {
        boxId: boxNumber,
        destinationFacilityId: Number.parseInt(selectedFacility.id),
        temperatureRequirement: selectedTemperature?.id || "AMBIENT",
        capacity: capacity,
        notes: notes,
        state: "DRAFT",
      };

      const response = await postToOpenElisServerJsonResponse(
        "/rest/shipping-box",
        JSON.stringify(boxData),
      );

      if (response && response.id) {
        // Add samples to the box
        for (const sample of addedSamples) {
          await postToOpenElisServerJsonResponse(
            "/rest/box-sample",
            JSON.stringify({
              shippingBoxId: response.id,
              sampleId: sample.id,
            }),
          );
        }

        addNotification({
          kind: "success",
          title: intl.formatMessage({ id: "notification.success" }),
          message: intl.formatMessage({
            id: "shipment.notification.boxCreated",
          }),
        });

        // Navigate to box details
        history.push(`/SampleShipment/box/${response.id}`);
      }
    } catch (error) {
      console.error("Error creating box:", error);
      addNotification({
        kind: "error",
        title: intl.formatMessage({ id: "notification.error" }),
        message: intl.formatMessage({ id: "shipment.error.createBox" }),
      });
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    history.push("/SampleShipment/boxes");
  };

  const handlePrintManifest = () => {
    // Will be enabled only when box is saved
    console.log("Print manifest");
  };

  // Calculate remaining capacity
  const remainingCapacity = capacity - addedSamples.length;
  const isFormValid = validationMessages.length === 0;

  // Sample table headers
  const sampleHeaders = [
    {
      key: "accessionNumber",
      header: intl.formatMessage({ id: "sample.label.accessionNumber" }),
    },
    {
      key: "sampleType",
      header: intl.formatMessage({ id: "shipment.sample.type" }),
    },
    {
      key: "referralTest",
      header: intl.formatMessage({ id: "shipment.label.referralTest" }),
    },
    { key: "actions", header: intl.formatMessage({ id: "label.actions" }) },
  ];

  const renderSampleRows = () => {
    return addedSamples.map((sample) => ({
      id: sample.id.toString(),
      accessionNumber: sample.accessionNumber,
      sampleType: sample.sampleType,
      referralTest: sample.referralTest,
      actions: (
        <div className="sample-actions">
          <Button
            kind="ghost"
            size="sm"
            renderIcon={TrashCan}
            iconDescription={intl.formatMessage({
              id: "shipment.sample.remove",
            })}
            onClick={() => handleRemoveSample(sample.id)}
            hasIconOnly
          />
          <Button
            kind="danger--ghost"
            size="sm"
            onClick={() => handleRejectSample(sample)}
          >
            <FormattedMessage id="shipment.action.rejectSample" />
          </Button>
        </div>
      ),
    }));
  };

  if (loading) {
    return (
      <div className="loading-container">
        <Loading />
      </div>
    );
  }

  return (
    <div className="box-creation">
      <AlertDialog />
      <PageBreadCrumb
        breadcrumbs={[
          { label: "home.label", link: "/" },
          { label: "shipment.breadcrumb", link: "/SampleShipment" },
          { label: "shipment.box.create", link: "/SampleShipment/box/create" },
        ]}
      />
      <ShipmentNavigation />

      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <h2 className="page-title">
            <FormattedMessage id="shipment.box.create" />
          </h2>
          <p className="page-description">
            <FormattedMessage id="shipment.box.createDescription" />
          </p>
        </Column>
      </Grid>

      <Grid fullWidth className="box-creation-content">
        {/* Left Column - Form */}
        <Column lg={10} md={5} sm={4}>
          {/* Validation Messages */}
          {validationMessages.length > 0 && (
            <div className="validation-messages">
              {validationMessages.map((message, index) => (
                <InlineNotification
                  key={index}
                  kind="warning"
                  title={intl.formatMessage({ id: "label.warning" })}
                  subtitle={message}
                  lowContrast
                  hideCloseButton
                />
              ))}
            </div>
          )}

          {/* Box Details Form */}
          <div className="form-section">
            <TextInput
              id="boxNumber"
              labelText={intl.formatMessage({ id: "shipment.box.number" })}
              value={boxNumber}
              readOnly
              disabled
            />

            <NumberInput
              id="capacity"
              label={intl.formatMessage({ id: "shipment.box.capacity" })}
              value={capacity}
              onChange={(e, { value }) => setCapacity(value)}
              min={1}
              max={100}
              step={1}
            />

            <Dropdown
              id="destination"
              titleText={intl.formatMessage({ id: "shipment.box.destination" })}
              label={intl.formatMessage({ id: "label.select" })}
              items={facilities}
              itemToString={(item) => (item ? item.value : "")}
              selectedItem={selectedFacility}
              onChange={({ selectedItem }) => setSelectedFacility(selectedItem)}
            />

            <Dropdown
              id="temperature"
              titleText={intl.formatMessage({ id: "shipment.box.temperature" })}
              label={intl.formatMessage({ id: "label.select" })}
              items={temperatureOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={selectedTemperature}
              onChange={({ selectedItem }) =>
                setSelectedTemperature(selectedItem)
              }
            />

            <TextArea
              id="notes"
              labelText={intl.formatMessage({ id: "shipment.box.notes" })}
              placeholder={intl.formatMessage({
                id: "shipment.box.notesPlaceholder",
              })}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={3}
            />
          </div>

          {/* Sample Search Section */}
          <div className="sample-search-section">
            <h4>
              <FormattedMessage id="shipment.box.addSamples" />
            </h4>
            <p className="sample-progress">
              <FormattedMessage
                id="shipment.box.sampleProgress"
                values={{
                  added: addedSamples.length,
                  capacity,
                  remaining: remainingCapacity,
                }}
              />
            </p>

            <div className="sample-search-input">
              <TextInput
                id="sampleSearch"
                labelText={intl.formatMessage({ id: "shipment.sample.search" })}
                placeholder={intl.formatMessage({
                  id: "shipment.sample.searchPlaceholder",
                })}
                value={sampleSearchTerm}
                onChange={(e) => setSampleSearchTerm(e.target.value)}
                onKeyPress={(e) => e.key === "Enter" && handleSearchSample()}
                disabled={searching || addedSamples.length >= capacity}
              />
              <Button
                renderIcon={Scan}
                onClick={handleSearchSample}
                disabled={
                  searching ||
                  !sampleSearchTerm.trim() ||
                  addedSamples.length >= capacity
                }
              >
                <FormattedMessage id="shipment.action.scanSample" />
              </Button>
            </div>
          </div>

          {/* Samples Table */}
          {addedSamples.length > 0 && (
            <div className="samples-table-section">
              <DataTable rows={renderSampleRows()} headers={sampleHeaders}>
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
                        {rows.map((row) => (
                          <TableRow {...getRowProps({ row })} key={row.id}>
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
        </Column>

        {/* Right Column - Summary Panel */}
        <Column lg={6} md={3} sm={4}>
          <Tile className="summary-panel">
            <h4 className="summary-title">
              <FormattedMessage id="shipment.box.summary" />
            </h4>

            <div className="summary-details">
              <div className="summary-row">
                <span className="summary-label">
                  <FormattedMessage id="shipment.box.number" />:
                </span>
                <span className="summary-value">{boxNumber}</span>
              </div>

              <div className="summary-row">
                <span className="summary-label">
                  <FormattedMessage id="shipment.box.capacity" />:
                </span>
                <span className="summary-value">
                  <FormattedMessage
                    id="shipment.box.capacityValue"
                    values={{ capacity }}
                  />
                </span>
              </div>

              <div className="summary-row">
                <span className="summary-label">
                  <FormattedMessage id="shipment.box.samplesAdded" />:
                </span>
                <span className="summary-value">{addedSamples.length}</span>
              </div>

              <div className="summary-row">
                <span className="summary-label">
                  <FormattedMessage id="shipment.box.destination" />:
                </span>
                <span className="summary-value">
                  {selectedFacility?.label || "-"}
                </span>
              </div>

              <div className="summary-row">
                <span className="summary-label">
                  <FormattedMessage id="shipment.box.temperature" />:
                </span>
                <span className="summary-value">
                  {selectedTemperature?.label || "Ambient"}
                </span>
              </div>
            </div>

            <div className="summary-actions">
              <Button
                kind="tertiary"
                renderIcon={Document}
                onClick={handlePrintManifest}
                disabled
              >
                <FormattedMessage id="shipment.action.printManifest" />
              </Button>

              <Button
                kind="primary"
                onClick={handleSaveBox}
                disabled={!isFormValid || loading}
              >
                <FormattedMessage id="shipment.action.saveBox" />
              </Button>

              <Button kind="secondary" onClick={handleCancel}>
                <FormattedMessage id="label.cancel" />
              </Button>
            </div>
          </Tile>
        </Column>
      </Grid>

      {/* Reject Sample Modal */}
      <Modal
        open={showRejectModal}
        modalHeading={intl.formatMessage({ id: "shipment.modal.rejectSample" })}
        primaryButtonText={intl.formatMessage({
          id: "shipment.action.confirmRejection",
        })}
        secondaryButtonText={intl.formatMessage({ id: "label.cancel" })}
        onRequestSubmit={confirmRejectSample}
        onRequestClose={() => {
          setShowRejectModal(false);
          setSampleToReject(null);
          setRejectionReason(null);
        }}
        primaryButtonDisabled={!rejectionReason}
      >
        <p>
          <FormattedMessage
            id="shipment.modal.rejectSampleMessage"
            values={{ accession: sampleToReject?.accessionNumber }}
          />
        </p>

        <Dropdown
          id="rejectionReason"
          titleText={intl.formatMessage({ id: "shipment.rejection.reason" })}
          label={intl.formatMessage({ id: "label.select" })}
          items={rejectionReasons}
          itemToString={(item) => (item ? item.label : "")}
          selectedItem={rejectionReason}
          onChange={({ selectedItem }) => setRejectionReason(selectedItem)}
        />
      </Modal>
    </div>
  );
};

export default BoxCreation;
