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
  Checkbox,
  Modal,
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
} from "@carbon/react";
import { TrashCan, Archive, DocumentBlank, Locked } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import "../../workflow/NotebookWorkflow.css";

/**
 * PathologyDisposalArchivingPage - Page 7 of the pathology workflow.
 * Purpose: Close the sample lifecycle in compliance with policy.
 * Who uses it: Lab manager / quality officer
 */
function PathologyDisposalArchivingPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [disposalModalOpen, setDisposalModalOpen] = useState(false);
  const [archiveModalOpen, setArchiveModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const [disposalData, setDisposalData] = useState({
    disposalReason: "",
    retentionPolicy: "",
    disposalMethod: "",
    disposalDate: "",
    staffSignature: "",
    unitHeadApproval: "",
  });

  const [archiveData, setArchiveData] = useState({
    archiveTypes: [],
    archiveLocation: "",
    digitalBackupLocation: "",
    archiveDate: "",
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
                sampleCategory: sample.sampleCategory || "Clinical diagnostic",
                status: sample.pageStatus || "PENDING",
                disposalStatus: sample.disposalStatus,
                archiveStatus: sample.archiveStatus,
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

  const handleCheckboxChange = (value, checked) => {
    setArchiveData((prev) => ({
      ...prev,
      archiveTypes: checked
        ? [...prev.archiveTypes, value]
        : prev.archiveTypes.filter((v) => v !== value),
    }));
  };

  const openDisposalModal = () => {
    if (selectedSampleIds.length === 0) {
      setError("Please select samples for disposal");
      return;
    }
    setDisposalData({
      disposalReason: "",
      retentionPolicy: "",
      disposalMethod: "",
      disposalDate: new Date().toISOString().split("T")[0],
      staffSignature: "",
      unitHeadApproval: "",
    });
    setDisposalModalOpen(true);
  };

  const openArchiveModal = () => {
    setArchiveData({
      archiveTypes: [],
      archiveLocation: "",
      digitalBackupLocation: "",
      archiveDate: new Date().toISOString().split("T")[0],
    });
    setArchiveModalOpen(true);
  };

  const handleSubmitDisposal = () => {
    if (submitting) return;
    if (
      !disposalData.disposalReason ||
      !disposalData.disposalMethod ||
      !disposalData.staffSignature ||
      !disposalData.unitHeadApproval
    ) {
      setError("Please fill in all required disposal fields");
      return;
    }

    setSubmitting(true);
    postToOpenElisServer(
      `/rest/notebook/pathology/disposal/submit`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        pageId: pageData?.id,
        ...disposalData,
      }),
      (status) => {
        setSubmitting(false);
        if (status === 200) {
          setDisposalModalOpen(false);
          setSelectedSampleIds([]);
          loadPageSamples();
          onProgressUpdate?.();
        } else {
          setError("Failed to record disposal. Please try again.");
        }
      },
    );
  };

  const handleSubmitArchive = () => {
    if (submitting) return;
    setSubmitting(true);
    postToOpenElisServer(
      `/rest/notebook/pathology/archive/submit`,
      JSON.stringify({
        entryId,
        pageId: pageData?.id,
        ...archiveData,
      }),
      (status) => {
        setSubmitting(false);
        if (status === 200) {
          setArchiveModalOpen(false);
          onProgressUpdate?.();
        } else {
          setError("Failed to record archive. Please try again.");
        }
      },
    );
  };

  const disposedCount = samples.filter(
    (s) => s.disposalStatus === "DISPOSED",
  ).length;
  const archivedCount = samples.filter(
    (s) => s.archiveStatus === "ARCHIVED",
  ).length;
  const activeCount = samples.filter((s) => s.status === "PENDING").length;

  const headers = [
    { key: "accessionNumber", header: "Accession Number" },
    { key: "specimenType", header: "Specimen Type" },
    { key: "sampleCategory", header: "Category" },
    { key: "disposalStatus", header: "Disposal Status" },
    { key: "status", header: "Status" },
  ];

  const disposalReasonOptions = [
    { id: "retention_expired", text: "Retention period expired" },
    { id: "sample_depleted", text: "Sample depleted" },
    { id: "quality_degradation", text: "Quality degradation" },
    { id: "study_completion", text: "Study completion" },
    { id: "other", text: "Other" },
  ];

  const retentionPolicyOptions = [
    { id: "national_clinical", text: "National clinical guidelines" },
    { id: "research_protocol", text: "Research protocol" },
    { id: "institutional", text: "Institutional policy" },
  ];

  const disposalMethodOptions = [
    { id: "incineration", text: "Incineration" },
    { id: "autoclaving", text: "Autoclaving" },
    { id: "chemical", text: "Chemical treatment" },
    { id: "other", text: "Other" },
  ];

  const archiveTypeOptions = [
    { id: "physical_logbooks", label: "Physical - Logbooks" },
    { id: "physical_ledgers", label: "Physical - Ledgers" },
    { id: "physical_reports", label: "Physical - Reports" },
    { id: "digital_backup", label: "Digital backup" },
  ];

  return (
    <div className="pathology-disposal-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="pathology.page.disposal.title"
            defaultMessage="Disposal & Archiving"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="pathology.page.disposal.description"
            defaultMessage="Close the sample lifecycle in compliance with policy. Record disposal and archive documentation."
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
            <Tile
              className="progress-tile"
              style={{ backgroundColor: "#fff1f1" }}
            >
              <span className="progress-label">Disposed</span>
              <span className="progress-value">{disposedCount}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">Archived</span>
              <span className="progress-value">{archivedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">Active</span>
              <span className="progress-value">{activeCount}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      <div className="page-actions-bar">
        <Button
          kind="danger--tertiary"
          size="sm"
          renderIcon={TrashCan}
          onClick={openDisposalModal}
          disabled={selectedSampleIds.length === 0}
        >
          Dispose Selected ({selectedSampleIds.length})
        </Button>
        <Button
          kind="secondary"
          size="sm"
          renderIcon={Archive}
          onClick={openArchiveModal}
        >
          Archive Records
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
            <p>No samples available for disposal or archiving.</p>
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
              getSelectionProps,
            }) => (
              <TableContainer>
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
                        <TableSelectRow
                          {...getSelectionProps({ row })}
                          onChange={() => {
                            const isSelected = selectedSampleIds.includes(
                              row.id,
                            );
                            if (isSelected) {
                              setSelectedSampleIds(
                                selectedSampleIds.filter((id) => id !== row.id),
                              );
                            } else {
                              setSelectedSampleIds([
                                ...selectedSampleIds,
                                row.id,
                              ]);
                            }
                          }}
                        />
                        {row.cells.map((cell) => (
                          <TableCell key={cell.id}>
                            {cell.info.header === "status" ||
                            cell.info.header === "disposalStatus" ? (
                              <Tag
                                type={
                                  cell.value === "COMPLETED" ||
                                  cell.value === "DISPOSED"
                                    ? "red"
                                    : cell.value === "ARCHIVED"
                                      ? "green"
                                      : "gray"
                                }
                                size="sm"
                              >
                                {cell.value || "Active"}
                              </Tag>
                            ) : (
                              cell.value
                            )}
                          </TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          />
        )}
      </div>

      {/* Disposal Modal */}
      <Modal
        open={disposalModalOpen}
        modalHeading={`Dispose Samples (${selectedSampleIds.length})`}
        primaryButtonText="Confirm Disposal"
        secondaryButtonText="Cancel"
        onRequestClose={() => setDisposalModalOpen(false)}
        onRequestSubmit={handleSubmitDisposal}
        primaryButtonDisabled={submitting}
        danger
        size="lg"
      >
        <Grid fullWidth>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="disposalReason"
              name="disposalReason"
              labelText="Disposal Reason *"
              value={disposalData.disposalReason}
              onChange={(e) => handleInputChange(e, setDisposalData)}
            >
              <SelectItem value="" text="" />
              {disposalReasonOptions.map((opt) => (
                <SelectItem key={opt.id} value={opt.id} text={opt.text} />
              ))}
            </Select>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="retentionPolicy"
              name="retentionPolicy"
              labelText="Retention Policy Applied"
              value={disposalData.retentionPolicy}
              onChange={(e) => handleInputChange(e, setDisposalData)}
            >
              <SelectItem value="" text="" />
              {retentionPolicyOptions.map((opt) => (
                <SelectItem key={opt.id} value={opt.id} text={opt.text} />
              ))}
            </Select>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="disposalMethod"
              name="disposalMethod"
              labelText="Disposal Method *"
              value={disposalData.disposalMethod}
              onChange={(e) => handleInputChange(e, setDisposalData)}
            >
              <SelectItem value="" text="" />
              {disposalMethodOptions.map((opt) => (
                <SelectItem key={opt.id} value={opt.id} text={opt.text} />
              ))}
            </Select>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              onChange={(dates) =>
                handleDateChange(dates, "disposalDate", setDisposalData)
              }
            >
              <DatePickerInput
                id="disposalDate"
                labelText="Disposal Date *"
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="staffSignature"
              name="staffSignature"
              labelText="Staff Signature *"
              value={disposalData.staffSignature}
              onChange={(e) => handleInputChange(e, setDisposalData)}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="unitHeadApproval"
              name="unitHeadApproval"
              labelText="Unit Head Approval *"
              value={disposalData.unitHeadApproval}
              onChange={(e) => handleInputChange(e, setDisposalData)}
            />
          </Column>
        </Grid>
      </Modal>

      {/* Archive Modal */}
      <Modal
        open={archiveModalOpen}
        modalHeading="Archive Records"
        primaryButtonText="Save"
        secondaryButtonText="Cancel"
        onRequestClose={() => setArchiveModalOpen(false)}
        onRequestSubmit={handleSubmitArchive}
        primaryButtonDisabled={submitting}
        size="lg"
      >
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginBottom: "1rem" }}>Archive Type</h5>
            <div style={{ display: "flex", flexWrap: "wrap", gap: "1rem" }}>
              {archiveTypeOptions.map((opt) => (
                <Checkbox
                  key={opt.id}
                  id={opt.id}
                  labelText={opt.label}
                  checked={archiveData.archiveTypes.includes(opt.id)}
                  onChange={(e) =>
                    handleCheckboxChange(opt.id, e.target.checked)
                  }
                />
              ))}
            </div>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="archiveLocation"
              name="archiveLocation"
              labelText="Physical Archive Location"
              placeholder="e.g., Fire-proof cabinet A3"
              value={archiveData.archiveLocation}
              onChange={(e) => handleInputChange(e, setArchiveData)}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="digitalBackupLocation"
              name="digitalBackupLocation"
              labelText="Digital Backup Location"
              placeholder="e.g., Secure server /archives/2025"
              value={archiveData.digitalBackupLocation}
              onChange={(e) => handleInputChange(e, setArchiveData)}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              onChange={(dates) =>
                handleDateChange(dates, "archiveDate", setArchiveData)
              }
            >
              <DatePickerInput
                id="archiveDate"
                labelText="Archive Date"
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default PathologyDisposalArchivingPage;
