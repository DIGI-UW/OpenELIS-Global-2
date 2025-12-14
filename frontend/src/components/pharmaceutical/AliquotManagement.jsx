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
  NumberInput,
  Select,
  SelectItem,
  Tag,
  Loading,
  InlineNotification,
  ProgressBar,
  Tile,
} from "@carbon/react";
import { Add, WarningAlt, Renew } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer, postToOpenElisServer } from "../utils/Utils";

const AliquotManagement = () => {
  const intl = useIntl();
  const [aliquots, setAliquots] = useState([]);
  const [samples, setSamples] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isFreezeThawModalOpen, setIsFreezeThawModalOpen] = useState(false);
  const [selectedAliquot, setSelectedAliquot] = useState(null);
  const [exceededAliquots, setExceededAliquots] = useState([]);
  const [formData, setFormData] = useState({
    parentSampleId: "",
    volumeWeight: "",
    freezeThawLimit: 5,
    storageLocationId: "",
  });

  const fetchAliquots = useCallback(() => {
    setLoading(true);
    getFromOpenElisServer("/rest/pharmaceutical/aliquots", (response) => {
      if (response) {
        setAliquots(response);
      }
      setLoading(false);
    });
  }, []);

  const fetchSamples = useCallback(() => {
    getFromOpenElisServer("/rest/pharmaceutical/samples", (response) => {
      if (response) {
        setSamples(response);
      }
    });
  }, []);

  const fetchExceededAliquots = useCallback(() => {
    getFromOpenElisServer(
      "/rest/pharmaceutical/aliquots/freeze-thaw-exceeded",
      (response) => {
        if (response) {
          setExceededAliquots(response);
        }
      },
    );
  }, []);

  useEffect(() => {
    fetchAliquots();
    fetchSamples();
    fetchExceededAliquots();
  }, [fetchAliquots, fetchSamples, fetchExceededAliquots]);

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleNumberChange = (e, { name, value }) => {
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleCreateAliquot = () => {
    postToOpenElisServer(
      `/rest/pharmaceutical/aliquots/sample/${formData.parentSampleId}`,
      JSON.stringify({
        volumeWeight: parseFloat(formData.volumeWeight),
        freezeThawLimit: formData.freezeThawLimit,
        storageLocationId: formData.storageLocationId
          ? parseInt(formData.storageLocationId)
          : null,
      }),
      (response) => {
        if (response) {
          setIsCreateModalOpen(false);
          setFormData({
            parentSampleId: "",
            volumeWeight: "",
            freezeThawLimit: 5,
            storageLocationId: "",
          });
          fetchAliquots();
          fetchExceededAliquots();
        } else {
          setError("Failed to create aliquot");
        }
      },
    );
  };

  const handleRecordFreezeThaw = () => {
    if (!selectedAliquot) return;

    postToOpenElisServer(
      `/rest/pharmaceutical/aliquots/${selectedAliquot.id}/freeze-thaw`,
      null,
      (response) => {
        if (response) {
          setIsFreezeThawModalOpen(false);
          setSelectedAliquot(null);
          fetchAliquots();
          fetchExceededAliquots();
        } else {
          setError("Failed to record freeze-thaw cycle");
        }
      },
    );
  };

  const openFreezeThawModal = (aliquot) => {
    setSelectedAliquot(aliquot);
    setIsFreezeThawModalOpen(true);
  };

  const getStatusTag = (status) => {
    const statusColors = {
      AVAILABLE: "green",
      IN_USE: "blue",
      RESERVED: "cyan",
      EXHAUSTED: "gray",
      DISPOSED: "gray",
    };
    return <Tag type={statusColors[status] || "gray"}>{status}</Tag>;
  };

  const getFreezeThawProgress = (count, limit) => {
    const percentage = (count / limit) * 100;
    let status = "active";
    if (percentage >= 100) {
      status = "error";
    } else if (percentage >= 80) {
      status = "warning";
    }
    return (
      <div className="freeze-thaw-indicator">
        <ProgressBar
          value={percentage}
          max={100}
          size="sm"
          status={status === "error" ? "error" : undefined}
        />
        <span
          className={
            percentage >= 100
              ? "freeze-thaw-indicator--exceeded"
              : percentage >= 80
                ? "freeze-thaw-indicator--warning"
                : ""
          }
        >
          {count}/{limit}
        </span>
      </div>
    );
  };

  const headers = [
    {
      key: "aliquotCode",
      header: intl.formatMessage({ id: "pharmaceutical.aliquot.code" }),
    },
    {
      key: "parentSampleId",
      header: intl.formatMessage({ id: "pharmaceutical.aliquot.parentSample" }),
    },
    {
      key: "volumeWeight",
      header: intl.formatMessage({ id: "pharmaceutical.aliquot.volume" }),
    },
    {
      key: "freezeThaw",
      header: intl.formatMessage({ id: "pharmaceutical.aliquot.freezeThaw" }),
    },
    {
      key: "status",
      header: intl.formatMessage({ id: "pharmaceutical.aliquot.status" }),
    },
    {
      key: "actions",
      header: intl.formatMessage({ id: "label.actions" }),
    },
  ];

  const rows = aliquots.map((aliquot) => ({
    id: String(aliquot.id),
    aliquotCode: aliquot.aliquotCode,
    parentSampleId: aliquot.parentSampleId,
    volumeWeight: aliquot.volumeWeight,
    freezeThaw: aliquot,
    status: aliquot.status,
    actions: aliquot,
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

        {exceededAliquots.length > 0 && (
          <Tile className="pharmaceutical-alert pharmaceutical-alert--warning">
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <WarningAlt size={20} />
              <strong>
                <FormattedMessage id="pharmaceutical.aliquot.freezeThaw.warning" />
              </strong>
            </div>
            <p>
              {exceededAliquots.length}{" "}
              <FormattedMessage id="pharmaceutical.aliquot.freezeThaw.exceeded.count" />
            </p>
          </Tile>
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
                  <Button
                    renderIcon={Add}
                    onClick={() => setIsCreateModalOpen(true)}
                  >
                    <FormattedMessage id="pharmaceutical.aliquot.create" />
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
                          ) : cell.info.header === "freezeThaw" ? (
                            getFreezeThawProgress(
                              cell.value.freezeThawCount,
                              cell.value.freezeThawLimit,
                            )
                          ) : cell.info.header === "actions" ? (
                            <div className="pharmaceutical-actions">
                              <Button
                                kind="ghost"
                                size="sm"
                                renderIcon={Renew}
                                iconDescription={intl.formatMessage({
                                  id: "pharmaceutical.aliquot.recordFreezeThaw",
                                })}
                                hasIconOnly
                                onClick={() => openFreezeThawModal(cell.value)}
                                disabled={cell.value.status === "EXHAUSTED"}
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

        {/* Create Aliquot Modal */}
        <Modal
          open={isCreateModalOpen}
          onRequestClose={() => setIsCreateModalOpen(false)}
          modalHeading={intl.formatMessage({
            id: "pharmaceutical.aliquot.create.title",
          })}
          primaryButtonText={intl.formatMessage({ id: "label.save" })}
          secondaryButtonText={intl.formatMessage({ id: "label.cancel" })}
          onRequestSubmit={handleCreateAliquot}
        >
          <Form className="pharmaceutical-form">
            <Select
              id="parentSampleId"
              name="parentSampleId"
              labelText={intl.formatMessage({
                id: "pharmaceutical.aliquot.parentSample",
              })}
              value={formData.parentSampleId}
              onChange={handleInputChange}
              required
            >
              <SelectItem value="" text="Select a sample" />
              {samples.map((sample) => (
                <SelectItem
                  key={sample.id}
                  value={sample.id}
                  text={`${sample.uniqueSampleId} - ${sample.sampleName}`}
                />
              ))}
            </Select>
            <TextInput
              id="volumeWeight"
              name="volumeWeight"
              labelText={intl.formatMessage({
                id: "pharmaceutical.aliquot.volume",
              })}
              value={formData.volumeWeight}
              onChange={handleInputChange}
              required
            />
            <NumberInput
              id="freezeThawLimit"
              name="freezeThawLimit"
              label={intl.formatMessage({
                id: "pharmaceutical.aliquot.freezeThawLimit",
              })}
              value={formData.freezeThawLimit}
              onChange={(e, { value }) =>
                handleNumberChange(e, { name: "freezeThawLimit", value })
              }
              min={1}
              max={20}
            />
            <TextInput
              id="storageLocationId"
              name="storageLocationId"
              labelText={intl.formatMessage({
                id: "pharmaceutical.aliquot.storageLocation",
              })}
              value={formData.storageLocationId}
              onChange={handleInputChange}
            />
          </Form>
        </Modal>

        {/* Record Freeze-Thaw Modal */}
        <Modal
          open={isFreezeThawModalOpen}
          onRequestClose={() => setIsFreezeThawModalOpen(false)}
          modalHeading={intl.formatMessage({
            id: "pharmaceutical.aliquot.recordFreezeThaw.title",
          })}
          primaryButtonText={intl.formatMessage({ id: "label.confirm" })}
          secondaryButtonText={intl.formatMessage({ id: "label.cancel" })}
          onRequestSubmit={handleRecordFreezeThaw}
          danger={
            selectedAliquot &&
            selectedAliquot.freezeThawCount + 1 >=
              selectedAliquot.freezeThawLimit
          }
        >
          {selectedAliquot && (
            <div>
              <p>
                <FormattedMessage id="pharmaceutical.aliquot.recordFreezeThaw.confirm" />
              </p>
              <p>
                <strong>
                  <FormattedMessage id="pharmaceutical.aliquot.code" />:
                </strong>{" "}
                {selectedAliquot.aliquotCode}
              </p>
              <p>
                <strong>
                  <FormattedMessage id="pharmaceutical.aliquot.currentCount" />:
                </strong>{" "}
                {selectedAliquot.freezeThawCount} /{" "}
                {selectedAliquot.freezeThawLimit}
              </p>
              {selectedAliquot.freezeThawCount + 1 >=
                selectedAliquot.freezeThawLimit && (
                <InlineNotification
                  kind="warning"
                  title={intl.formatMessage({
                    id: "pharmaceutical.aliquot.freezeThaw.limitReached",
                  })}
                  subtitle={intl.formatMessage({
                    id: "pharmaceutical.aliquot.freezeThaw.limitReached.message",
                  })}
                  hideCloseButton
                  lowContrast
                />
              )}
            </div>
          )}
        </Modal>
      </Column>
    </Grid>
  );
};

export default AliquotManagement;
