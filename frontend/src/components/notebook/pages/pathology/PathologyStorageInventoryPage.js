import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  Tag,
  InlineNotification,
  Loading,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  DatePicker,
  DatePickerInput,
  Modal,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  NumberInput,
} from "@carbon/react";
import { DataBase, Temperature, Location, Undo } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import "../../workflow/NotebookWorkflow.css";

/**
 * PathologyStorageInventoryPage - Page 5 of the pathology workflow.
 * Purpose: Track physical storage and retrieval of pathology materials.
 * Who uses it: Store manager / lab staff
 */
function PathologyStorageInventoryPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  const [samples, setSamples] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [storageModalOpen, setStorageModalOpen] = useState(false);
  const [tempLogModalOpen, setTempLogModalOpen] = useState(false);
  const [selectedSample, setSelectedSample] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const [storageData, setStorageData] = useState({
    storageType: "",
    expectedDuration: "",
    storageUnit: "",
    rack: "",
    box: "",
    position: "",
    dateStored: "",
    storedBy: "",
  });

  // Retrieval modal state
  const [retrievalModalOpen, setRetrievalModalOpen] = useState(false);
  const [retrievalData, setRetrievalData] = useState({
    dateRetrieved: "",
    retrievedBy: "",
    recipientSignature: "",
  });

  const [tempLogData, setTempLogData] = useState({
    storageUnit: "",
    temperatureCheckAM: 4,
    temperatureCheckPM: 4,
    checkedBy: "",
    checkDate: "",
  });

  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id]);

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      setLoading(false);
      return;
    }

    setLoading(true);
    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            setSamples(
              response.map((sample) => ({
                id: String(sample.id || sample.sampleItemId),
                accessionNumber: sample.accessionNumber,
                specimenType:
                  sample.sampleType || sample.typeOfSample?.description,
                storageLocation: sample.storageLocation || "-",
                storageType: sample.storageType || "-",
                status: sample.pageStatus || "PENDING",
              })),
            );
          } else {
            setSamples([]);
          }
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  const handleInputChange = (e, setState) => {
    const { name, value } = e.target;
    setState((prev) => ({ ...prev, [name]: value }));
  };

  const handleDateChange = (dates, fieldName, setState) => {
    if (dates?.[0]) {
      setState((prev) => ({
        ...prev,
        [fieldName]: dates[0].toISOString().split("T")[0],
      }));
    }
  };

  const openStorageModal = (sample) => {
    setSelectedSample(sample);
    setStorageData({
      storageType: "",
      expectedDuration: "",
      storageUnit: "",
      rack: "",
      box: "",
      position: "",
      dateStored: new Date().toISOString().split("T")[0],
      storedBy: "",
      dateRetrieved: "",
      retrievedBy: "",
      recipientSignature: "",
    });
    setStorageModalOpen(true);
  };

  const handleSubmitStorage = () => {
    if (submitting) return;
    if (
      !storageData.storageUnit ||
      !storageData.rack ||
      !storageData.box ||
      !storageData.position
    ) {
      setError("Please fill in all location fields");
      return;
    }

    setSubmitting(true);
    postToOpenElisServer(
      `/rest/notebook/pathology/storage/assign`,
      JSON.stringify({
        sampleId: selectedSample?.id,
        pageId: pageData?.id,
        ...storageData,
      }),
      (status) => {
        setSubmitting(false);
        if (status === 200) {
          setStorageModalOpen(false);
          loadPageSamples();
          onProgressUpdate?.();
        } else {
          setError("Failed to assign storage. Please try again.");
        }
      },
    );
  };

  const handleSubmitTempLog = () => {
    if (submitting) return;
    setSubmitting(true);
    postToOpenElisServer(
      `/rest/notebook/pathology/storage/temperature-log`,
      JSON.stringify({ pageId: pageData?.id, ...tempLogData }),
      (status) => {
        setSubmitting(false);
        if (status === 200) {
          setTempLogModalOpen(false);
        } else {
          setError("Failed to log temperature. Please try again.");
        }
      },
    );
  };

  const openRetrievalModal = (sample) => {
    setSelectedSample(sample);
    setRetrievalData({
      dateRetrieved: new Date().toISOString().split("T")[0],
      retrievedBy: "",
      recipientSignature: "",
    });
    setRetrievalModalOpen(true);
  };

  const handleSubmitRetrieval = () => {
    if (submitting) return;
    if (!retrievalData.retrievedBy || !retrievalData.recipientSignature) {
      setError("Please fill in Retrieved By and Recipient Signature");
      return;
    }

    setSubmitting(true);
    postToOpenElisServer(
      `/rest/notebook/pathology/storage/retrieve`,
      JSON.stringify({
        sampleId: selectedSample?.id,
        pageId: pageData?.id,
        ...retrievalData,
      }),
      (status) => {
        setSubmitting(false);
        if (status === 200) {
          setRetrievalModalOpen(false);
          loadPageSamples();
          onProgressUpdate?.();
        } else {
          setError("Failed to record retrieval. Please try again.");
        }
      },
    );
  };

  const storedCount = samples.filter((s) => s.status === "COMPLETED").length;
  const pendingCount = samples.filter((s) => s.status === "PENDING").length;

  const headers = [
    { key: "accessionNumber", header: "Accession Number" },
    { key: "specimenType", header: "Specimen Type" },
    { key: "storageType", header: "Storage Type" },
    { key: "storageLocation", header: "Location" },
    { key: "status", header: "Status" },
    { key: "actions", header: "Actions" },
  ];

  const storageTypeOptions = [
    { id: "room_temp_cabinet", text: "Room temperature cabinet" },
    { id: "4c_refrigerator", text: "4°C refrigerator" },
    { id: "-20c_freezer", text: "-20°C freezer" },
    { id: "-80c_freezer", text: "-80°C freezer" },
    { id: "ln2_vapor", text: "LN2 vapor" },
    { id: "slide_box", text: "Slide box" },
  ];

  return (
    <div className="pathology-storage-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="pathology.page.storage.title"
            defaultMessage="Storage & Inventory Management"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="pathology.page.storage.description"
            defaultMessage="Track physical storage and retrieval of pathology materials. Assign locations and log environmental monitoring."
          />
        </p>
      </div>

      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">Total Samples</span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">Stored</span>
              <span className="progress-value">{storedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">Pending Storage</span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      <div className="page-actions-bar">
        <Button
          kind="secondary"
          size="sm"
          renderIcon={Temperature}
          onClick={() => setTempLogModalOpen(true)}
        >
          <FormattedMessage
            id="pathology.page.storage.logTemperature"
            defaultMessage="Log Temperature"
          />
        </Button>
      </div>

      {error && (
        <InlineNotification
          kind="error"
          title={error}
          onCloseButtonClick={() => setError(null)}
          lowContrast
        />
      )}

      <div className="sample-grid-container">
        {loading ? (
          <Loading withOverlay={false} description="Loading samples..." />
        ) : samples.length === 0 ? (
          <div className="empty-state">
            <p>No samples available for storage assignment.</p>
          </div>
        ) : (
          <DataTable
            rows={samples}
            headers={headers}
            isSortable
            render={({
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
                      const sample = samples.find((s) => s.id === row.id);
                      return (
                        <TableRow key={row.id} {...getRowProps({ row })}>
                          {row.cells.map((cell) => (
                            <TableCell key={cell.id}>
                              {cell.info.header === "status" ? (
                                <Tag
                                  type={
                                    cell.value === "COMPLETED"
                                      ? "green"
                                      : "gray"
                                  }
                                  size="sm"
                                >
                                  {cell.value}
                                </Tag>
                              ) : cell.info.header === "actions" ? (
                                <div
                                  style={{ display: "flex", gap: "0.25rem" }}
                                >
                                  <Button
                                    kind="ghost"
                                    size="sm"
                                    renderIcon={Location}
                                    onClick={() => openStorageModal(sample)}
                                  >
                                    Assign
                                  </Button>
                                  {sample?.status === "COMPLETED" && (
                                    <Button
                                      kind="ghost"
                                      size="sm"
                                      renderIcon={Undo}
                                      onClick={() => openRetrievalModal(sample)}
                                    >
                                      Retrieve
                                    </Button>
                                  )}
                                </div>
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
          />
        )}
      </div>

      {/* Storage Assignment Modal */}
      <Modal
        open={storageModalOpen}
        modalHeading={`Assign Storage - ${selectedSample?.accessionNumber || ""}`}
        primaryButtonText="Save"
        secondaryButtonText="Cancel"
        onRequestClose={() => setStorageModalOpen(false)}
        onRequestSubmit={handleSubmitStorage}
        primaryButtonDisabled={submitting}
        size="lg"
      >
        <Grid fullWidth>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="storageType"
              name="storageType"
              labelText="Storage Type *"
              value={storageData.storageType}
              onChange={(e) => handleInputChange(e, setStorageData)}
            >
              <SelectItem value="" text="" />
              {storageTypeOptions.map((opt) => (
                <SelectItem key={opt.id} value={opt.id} text={opt.text} />
              ))}
            </Select>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="expectedDuration"
              name="expectedDuration"
              labelText="Expected Duration"
              value={storageData.expectedDuration}
              onChange={(e) => handleInputChange(e, setStorageData)}
            />
          </Column>
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              Location Tracking
            </h5>
          </Column>
          <Column lg={4} md={4} sm={4}>
            <TextInput
              id="storageUnit"
              name="storageUnit"
              labelText="Storage Unit *"
              value={storageData.storageUnit}
              onChange={(e) => handleInputChange(e, setStorageData)}
              placeholder="Cabinet/Freezer ID"
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <TextInput
              id="rack"
              name="rack"
              labelText="Rack *"
              value={storageData.rack}
              onChange={(e) => handleInputChange(e, setStorageData)}
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <TextInput
              id="box"
              name="box"
              labelText="Box *"
              value={storageData.box}
              onChange={(e) => handleInputChange(e, setStorageData)}
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <TextInput
              id="position"
              name="position"
              labelText="Position *"
              value={storageData.position}
              onChange={(e) => handleInputChange(e, setStorageData)}
              placeholder="e.g., A1, B3"
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              onChange={(dates) =>
                handleDateChange(dates, "dateStored", setStorageData)
              }
            >
              <DatePickerInput
                id="dateStored"
                labelText="Date Stored *"
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="storedBy"
              name="storedBy"
              labelText="Stored By *"
              value={storageData.storedBy}
              onChange={(e) => handleInputChange(e, setStorageData)}
            />
          </Column>
        </Grid>
      </Modal>

      {/* Temperature Log Modal */}
      <Modal
        open={tempLogModalOpen}
        modalHeading="Log Temperature"
        primaryButtonText="Save"
        secondaryButtonText="Cancel"
        onRequestClose={() => setTempLogModalOpen(false)}
        onRequestSubmit={handleSubmitTempLog}
        primaryButtonDisabled={submitting}
      >
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <TextInput
              id="tempStorageUnit"
              name="storageUnit"
              labelText="Storage Unit"
              value={tempLogData.storageUnit}
              onChange={(e) => handleInputChange(e, setTempLogData)}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <NumberInput
              id="temperatureCheckAM"
              label="Temperature AM (°C)"
              value={tempLogData.temperatureCheckAM}
              onChange={(e, { value }) =>
                setTempLogData((prev) => ({
                  ...prev,
                  temperatureCheckAM: value,
                }))
              }
              min={-200}
              max={200}
              step={0.1}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <NumberInput
              id="temperatureCheckPM"
              label="Temperature PM (°C)"
              value={tempLogData.temperatureCheckPM}
              onChange={(e, { value }) =>
                setTempLogData((prev) => ({
                  ...prev,
                  temperatureCheckPM: value,
                }))
              }
              min={-200}
              max={200}
              step={0.1}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="tempCheckedBy"
              name="checkedBy"
              labelText="Checked By"
              value={tempLogData.checkedBy}
              onChange={(e) => handleInputChange(e, setTempLogData)}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              onChange={(dates) =>
                handleDateChange(dates, "checkDate", setTempLogData)
              }
            >
              <DatePickerInput
                id="checkDate"
                labelText="Check Date"
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
          </Column>
        </Grid>
      </Modal>

      {/* Retrieval Modal */}
      <Modal
        open={retrievalModalOpen}
        modalHeading={`Retrieve Sample - ${selectedSample?.accessionNumber || ""}`}
        primaryButtonText="Record Retrieval"
        secondaryButtonText="Cancel"
        onRequestClose={() => setRetrievalModalOpen(false)}
        onRequestSubmit={handleSubmitRetrieval}
        primaryButtonDisabled={submitting}
      >
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <DatePicker
              datePickerType="single"
              onChange={(dates) =>
                handleDateChange(dates, "dateRetrieved", setRetrievalData)
              }
            >
              <DatePickerInput
                id="dateRetrieved"
                labelText="Date Retrieved *"
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="retrievedBy"
              name="retrievedBy"
              labelText="Retrieved By *"
              value={retrievalData.retrievedBy}
              onChange={(e) => handleInputChange(e, setRetrievalData)}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="recipientSignature"
              name="recipientSignature"
              labelText="Recipient Signature *"
              value={retrievalData.recipientSignature}
              onChange={(e) => handleInputChange(e, setRetrievalData)}
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default PathologyStorageInventoryPage;
