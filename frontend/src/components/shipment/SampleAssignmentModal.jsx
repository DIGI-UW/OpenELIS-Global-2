import React, { useState, useEffect, useContext } from "react";
import {
  Modal,
  Search,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableSelectRow,
  Loading,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";
import "./SampleAssignmentModal.css";

const SampleAssignmentModal = ({
  open,
  onClose,
  onAddSample,
  destinationFacilityId,
}) => {
  const intl = useIntl();
  const { addNotification } = useContext(NotificationContext);

  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [unassignedSamples, setUnassignedSamples] = useState([]);
  const [selectedSamples, setSelectedSamples] = useState([]);

  useEffect(() => {
    if (open) {
      fetchUnassignedSamples();
    }
  }, [open, destinationFacilityId]);

  const fetchUnassignedSamples = async () => {
    setLoading(true);
    try {
      let url = "/rest/unassigned-samples/dashboard";
      if (destinationFacilityId) {
        url = `/rest/unassigned-samples/by-facility/${destinationFacilityId}`;
      }

      const response = await getFromOpenElisServer(url);
      if (response) {
        setUnassignedSamples(response);
      }
    } catch (error) {
      console.error("Error fetching unassigned samples:", error);
      addNotification({
        kind: "error",
        title: intl.formatMessage({ id: "notification.error" }),
        message: intl.formatMessage({ id: "error.fetch.unassignedSamples" }),
      });
    } finally {
      setLoading(false);
    }
  };

  const filterSamples = () => {
    if (!searchTerm) {
      return unassignedSamples;
    }

    const lowerSearch = searchTerm.toLowerCase();
    return unassignedSamples.filter(
      (sample) =>
        sample.accessionNumber?.toLowerCase().includes(lowerSearch) ||
        sample.referralTestName?.toLowerCase().includes(lowerSearch),
    );
  };

  const headers = [
    {
      key: "accessionNumber",
      header: intl.formatMessage({ id: "sample.label.accessionNumber" }),
    },
    {
      key: "referralTestName",
      header: intl.formatMessage({ id: "shipment.label.referralTest" }),
    },
    {
      key: "priority",
      header: intl.formatMessage({ id: "shipment.label.priority" }),
    },
    {
      key: "createdDate",
      header: intl.formatMessage({ id: "shipment.label.createdDate" }),
    },
  ];

  const renderRows = () => {
    const filtered = filterSamples();
    return filtered.map((sample) => ({
      id: sample.sampleId.toString(),
      accessionNumber: sample.accessionNumber,
      referralTestName: sample.referralTestName || "-",
      priority: sample.priority || "NORMAL",
      createdDate: sample.createdDate
        ? new Date(sample.createdDate).toLocaleDateString()
        : "-",
    }));
  };

  const handleSelectionChange = (selectedRows) => {
    const selectedIds = Object.keys(selectedRows).filter(
      (key) => selectedRows[key],
    );
    setSelectedSamples(selectedIds);
  };

  const handleSubmit = () => {
    if (selectedSamples.length === 0) {
      addNotification({
        kind: "warning",
        title: intl.formatMessage({ id: "notification.warning" }),
        message: intl.formatMessage({ id: "shipment.error.noSampleSelected" }),
      });
      return;
    }

    // Add samples one by one
    selectedSamples.forEach((sampleId) => {
      onAddSample(parseInt(sampleId));
    });
  };

  const handleClose = () => {
    setSelectedSamples([]);
    setSearchTerm("");
    onClose();
  };

  return (
    <Modal
      open={open}
      modalHeading={intl.formatMessage({ id: "shipment.box.addSample" })}
      primaryButtonText={intl.formatMessage({ id: "label.add" })}
      secondaryButtonText={intl.formatMessage({ id: "label.cancel" })}
      onRequestSubmit={handleSubmit}
      onRequestClose={handleClose}
      size="lg"
      primaryButtonDisabled={selectedSamples.length === 0}
    >
      <div className="sample-assignment-modal">
        <p className="modal-description">
          <FormattedMessage id="shipment.box.addSampleDescription" />
        </p>

        <Search
          size="lg"
          placeholder={intl.formatMessage({ id: "search.placeholder" })}
          labelText={intl.formatMessage({ id: "search.label" })}
          onChange={(e) => setSearchTerm(e.target.value)}
          value={searchTerm}
          className="sample-search"
        />

        {loading ? (
          <div className="loading-container">
            <Loading />
          </div>
        ) : (
          <div className="sample-table-container">
            {renderRows().length === 0 ? (
              <div className="empty-state">
                <p>
                  <FormattedMessage id="shipment.box.noUnassignedSamples" />
                </p>
              </div>
            ) : (
              <DataTable
                rows={renderRows()}
                headers={headers}
                radio={false}
                render={({
                  rows,
                  headers,
                  getHeaderProps,
                  getRowProps,
                  getSelectionProps,
                  getTableProps,
                  selectedRows,
                }) => {
                  // Update selected samples when selection changes
                  React.useEffect(() => {
                    const selectedIds = selectedRows.map((row) => row.id);
                    setSelectedSamples(selectedIds);
                  }, [selectedRows]);

                  return (
                    <TableContainer>
                      <Table {...getTableProps()}>
                        <TableHead>
                          <TableRow>
                            <th scope="col" />
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
                              <TableSelectRow {...getSelectionProps({ row })} />
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
                  );
                }}
              />
            )}
          </div>
        )}

        {selectedSamples.length > 0 && (
          <div className="selection-summary">
            <FormattedMessage
              id="shipment.box.samplesSelected"
              values={{ count: selectedSamples.length }}
            />
          </div>
        )}
      </div>
    </Modal>
  );
};

export default SampleAssignmentModal;
