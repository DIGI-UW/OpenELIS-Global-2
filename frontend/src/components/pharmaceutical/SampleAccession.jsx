import React, { useState, useEffect, useCallback } from "react";
import {
  Grid,
  Column,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableToolbar,
  TableToolbarContent,
  TableToolbarSearch,
  Button,
  Modal,
  Form,
  TextInput,
  DatePicker,
  DatePickerInput,
  Select,
  SelectItem,
  TextArea,
  Tag,
  Loading,
  InlineNotification,
} from "@carbon/react";
import { Add, View, Edit } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer, postToOpenElisServer } from "../utils/Utils";

const SampleAccession = () => {
  const intl = useIntl();
  const [samples, setSamples] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isViewModalOpen, setIsViewModalOpen] = useState(false);
  const [selectedSample, setSelectedSample] = useState(null);
  const [formData, setFormData] = useState({
    sampleName: "",
    iupacName: "",
    gradeSpec: "",
    lotBatch: "",
    manufactureDate: null,
    expiryRetestDate: null,
    storageCondition: "",
    ownerRequester: "",
    labType: "QC",
  });

  const fetchSamples = useCallback(() => {
    setLoading(true);
    getFromOpenElisServer("/rest/pharmaceutical/samples", (response) => {
      if (response) {
        setSamples(response);
      }
      setLoading(false);
    });
  }, []);

  useEffect(() => {
    fetchSamples();
  }, [fetchSamples]);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleDateChange = (dates, fieldName) => {
    if (dates && dates.length > 0) {
      setFormData((prev) => ({ ...prev, [fieldName]: dates[0] }));
    }
  };

  const handleSubmit = () => {
    const payload = {
      ...formData,
      manufactureDate: formData.manufactureDate
        ? formData.manufactureDate.getTime()
        : null,
      expiryRetestDate: formData.expiryRetestDate
        ? formData.expiryRetestDate.getTime()
        : null,
    };

    postToOpenElisServer(
      "/rest/pharmaceutical/samples/register",
      JSON.stringify(payload),
      (response) => {
        if (response) {
          setIsModalOpen(false);
          setFormData({
            sampleName: "",
            iupacName: "",
            gradeSpec: "",
            lotBatch: "",
            manufactureDate: null,
            expiryRetestDate: null,
            storageCondition: "",
            ownerRequester: "",
            labType: "QC",
          });
          fetchSamples();
        } else {
          setError("Failed to register sample");
        }
      },
    );
  };

  const handleViewDetails = (sample) => {
    getFromOpenElisServer(
      `/rest/pharmaceutical/samples/${sample.id}/details`,
      (response) => {
        if (response) {
          setSelectedSample(response);
          setIsViewModalOpen(true);
        }
      },
    );
  };

  const getStatusTag = (status) => {
    const statusColors = {
      REGISTERED: "blue",
      IN_TESTING: "orange",
      COMPLETED: "green",
      IN_STORAGE: "cyan",
      PENDING_DISPOSAL: "purple",
      DISPOSED: "gray",
    };
    return <Tag type={statusColors[status] || "gray"}>{status}</Tag>;
  };

  const headers = [
    {
      key: "uniqueSampleId",
      header: intl.formatMessage({ id: "pharmaceutical.sample.id" }),
    },
    {
      key: "sampleName",
      header: intl.formatMessage({ id: "pharmaceutical.sample.name" }),
    },
    {
      key: "lotBatch",
      header: intl.formatMessage({ id: "pharmaceutical.sample.lot" }),
    },
    {
      key: "labType",
      header: intl.formatMessage({ id: "pharmaceutical.sample.labType" }),
    },
    {
      key: "status",
      header: intl.formatMessage({ id: "pharmaceutical.sample.status" }),
    },
    {
      key: "barcode",
      header: intl.formatMessage({ id: "pharmaceutical.sample.barcode" }),
    },
    {
      key: "actions",
      header: intl.formatMessage({ id: "label.actions" }),
    },
  ];

  const rows = samples.map((sample) => ({
    id: String(sample.id),
    uniqueSampleId: sample.uniqueSampleId,
    sampleName: sample.sampleName,
    lotBatch: sample.lotBatch,
    labType: sample.labType,
    status: sample.status,
    barcode: sample.barcode,
    actions: sample,
  }));

  if (loading) {
    return <Loading />;
  }

  return (
    <Grid>
      <Column lg={16} md={8} sm={4}>
        {error && (
          <InlineNotification
            kind="error"
            title={intl.formatMessage({ id: "error.title" })}
            subtitle={error}
            onCloseButtonClick={() => setError(null)}
            className="pharmaceutical-alert"
          />
        )}

        <DataTable rows={rows} headers={headers}>
          {({
            rows,
            headers,
            getTableProps,
            getHeaderProps,
            getRowProps,
            onInputChange,
          }) => (
            <>
              <TableToolbar>
                <TableToolbarContent>
                  <TableToolbarSearch onChange={onInputChange} />
                  <Button renderIcon={Add} onClick={() => setIsModalOpen(true)}>
                    <FormattedMessage id="pharmaceutical.sample.register" />
                  </Button>
                </TableToolbarContent>
              </TableToolbar>
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
                    <TableRow key={row.id} {...getRowProps({ row })}>
                      {row.cells.map((cell) => (
                        <TableCell key={cell.id}>
                          {cell.info.header === "status" ? (
                            getStatusTag(cell.value)
                          ) : cell.info.header === "barcode" ? (
                            <span className="sample-barcode">{cell.value}</span>
                          ) : cell.info.header === "actions" ? (
                            <div className="pharmaceutical-actions">
                              <Button
                                kind="ghost"
                                size="sm"
                                renderIcon={View}
                                iconDescription="View"
                                hasIconOnly
                                onClick={() => handleViewDetails(cell.value)}
                              />
                              <Button
                                kind="ghost"
                                size="sm"
                                renderIcon={Edit}
                                iconDescription="Edit"
                                hasIconOnly
                              />
                            </div>
                          ) : (
                            cell.value
                          )}
                        </TableCell>
                      ))}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </>
          )}
        </DataTable>

        {/* Register Sample Modal */}
        <Modal
          open={isModalOpen}
          onRequestClose={() => setIsModalOpen(false)}
          modalHeading={intl.formatMessage({
            id: "pharmaceutical.sample.register.title",
          })}
          primaryButtonText={intl.formatMessage({ id: "label.save" })}
          secondaryButtonText={intl.formatMessage({ id: "label.cancel" })}
          onRequestSubmit={handleSubmit}
        >
          <Form className="pharmaceutical-form">
            <TextInput
              id="sampleName"
              name="sampleName"
              labelText={intl.formatMessage({
                id: "pharmaceutical.sample.name",
              })}
              value={formData.sampleName}
              onChange={handleInputChange}
              required
            />
            <TextInput
              id="iupacName"
              name="iupacName"
              labelText={intl.formatMessage({
                id: "pharmaceutical.sample.iupacName",
              })}
              value={formData.iupacName}
              onChange={handleInputChange}
            />
            <TextInput
              id="gradeSpec"
              name="gradeSpec"
              labelText={intl.formatMessage({
                id: "pharmaceutical.sample.gradeSpec",
              })}
              value={formData.gradeSpec}
              onChange={handleInputChange}
            />
            <TextInput
              id="lotBatch"
              name="lotBatch"
              labelText={intl.formatMessage({
                id: "pharmaceutical.sample.lot",
              })}
              value={formData.lotBatch}
              onChange={handleInputChange}
              required
            />
            <DatePicker
              datePickerType="single"
              onChange={(dates) => handleDateChange(dates, "manufactureDate")}
            >
              <DatePickerInput
                id="manufactureDate"
                labelText={intl.formatMessage({
                  id: "pharmaceutical.sample.manufactureDate",
                })}
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
            <DatePicker
              datePickerType="single"
              onChange={(dates) => handleDateChange(dates, "expiryRetestDate")}
            >
              <DatePickerInput
                id="expiryRetestDate"
                labelText={intl.formatMessage({
                  id: "pharmaceutical.sample.expiryDate",
                })}
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
            <TextArea
              id="storageCondition"
              name="storageCondition"
              labelText={intl.formatMessage({
                id: "pharmaceutical.sample.storageCondition",
              })}
              value={formData.storageCondition}
              onChange={handleInputChange}
            />
            <TextInput
              id="ownerRequester"
              name="ownerRequester"
              labelText={intl.formatMessage({
                id: "pharmaceutical.sample.owner",
              })}
              value={formData.ownerRequester}
              onChange={handleInputChange}
            />
            <Select
              id="labType"
              name="labType"
              labelText={intl.formatMessage({
                id: "pharmaceutical.sample.labType",
              })}
              value={formData.labType}
              onChange={handleInputChange}
            >
              <SelectItem value="QC" text="Quality Control (QC)" />
              <SelectItem value="RND" text="Research & Development (R&D)" />
              <SelectItem value="STABILITY" text="Stability Testing" />
              <SelectItem value="DISSOLUTION" text="Dissolution Testing" />
              <SelectItem value="REGULATORY" text="Regulatory Testing" />
            </Select>
          </Form>
        </Modal>

        {/* View Sample Details Modal */}
        <Modal
          open={isViewModalOpen}
          onRequestClose={() => setIsViewModalOpen(false)}
          modalHeading={intl.formatMessage({
            id: "pharmaceutical.sample.details.title",
          })}
          passiveModal
        >
          {selectedSample && (
            <div>
              <h4>
                <FormattedMessage id="pharmaceutical.sample.info" />
              </h4>
              <p>
                <strong>
                  <FormattedMessage id="pharmaceutical.sample.id" />:
                </strong>{" "}
                {selectedSample.sample?.uniqueSampleId}
              </p>
              <p>
                <strong>
                  <FormattedMessage id="pharmaceutical.sample.name" />:
                </strong>{" "}
                {selectedSample.sample?.sampleName}
              </p>
              <p>
                <strong>
                  <FormattedMessage id="pharmaceutical.sample.status" />:
                </strong>{" "}
                {getStatusTag(selectedSample.sample?.status)}
              </p>
              <p>
                <strong>
                  <FormattedMessage id="pharmaceutical.sample.barcode" />:
                </strong>{" "}
                <span className="sample-barcode">
                  {selectedSample.sample?.barcode}
                </span>
              </p>

              <h4 style={{ marginTop: "1rem" }}>
                <FormattedMessage id="pharmaceutical.aliquots" /> (
                {selectedSample.aliquotCount || 0})
              </h4>
              {selectedSample.aliquots?.map((aliquot) => (
                <p key={aliquot.id}>
                  {aliquot.aliquotCode} - {aliquot.status}
                </p>
              ))}

              <h4 style={{ marginTop: "1rem" }}>
                <FormattedMessage id="pharmaceutical.qcChecks" />
              </h4>
              {selectedSample.qcChecks?.map((qc) => (
                <p key={qc.id}>
                  {qc.checklistName} - {qc.outcome}
                </p>
              ))}
            </div>
          )}
        </Modal>
      </Column>
    </Grid>
  );
};

export default SampleAccession;
