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
  MultiSelect,
  NumberInput,
} from "@carbon/react";
import { Add, Checkmark, ChevronRight } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import "../../workflow/NotebookWorkflow.css";

/**
 * PathologySampleProcessingPage - Page 3 of the pathology workflow.
 * Purpose: Create slides, blocks, aliquots while maintaining traceability.
 * Who uses it: Technicians / pathologists
 */
function PathologySampleProcessingPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // Sample list state
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Processing Modal state
  const [processingModalOpen, setProcessingModalOpen] = useState(false);
  const [selectedSample, setSelectedSample] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  // Processing form state
  const [processingData, setProcessingData] = useState({
    processingAction: "",
    // Histopathology
    grossExamDone: false,
    grossDescription: "",
    sectioningDone: false,
    tissueProcessingSteps: [],
    embeddingDone: false,
    microtomyThickness: 4,
    // Cytopathology
    centrifugationDone: false,
    smearTypes: [],
    stainUsed: [],
    // Blood
    wedgeSmearDone: false,
    bloodStain: "",
    // Research
    sopFollowed: "",
    processingMethods: [],
    // Common
    processingDate: "",
    staffInitials: "",
    processingNotes: "",
  });

  // Load samples
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id]);

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id) {
      setLoading(false);
      return;
    }

    if (String(pageData.id).startsWith("default-")) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            const transformedSamples = response.map((sample) => ({
              id: String(sample.id || sample.sampleItemId),
              accessionNumber: sample.accessionNumber,
              specimenType:
                sample.sampleType || sample.typeOfSample?.description,
              specimenCategory: sample.specimenCategory || "histopathology",
              status: sample.pageStatus || "PENDING",
              childSamplesCount: sample.childSamplesCount || 0,
            }));
            setSamples(transformedSamples);
          } else {
            setSamples([]);
          }
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setProcessingData((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleMultiSelectChange = (fieldName, selectedItems) => {
    setProcessingData((prev) => ({
      ...prev,
      [fieldName]: selectedItems.map((item) => item.id || item),
    }));
  };

  const handleDateChange = (dates, fieldName) => {
    if (dates && dates.length > 0) {
      const date = dates[0];
      const formattedDate = date.toISOString().split("T")[0];
      setProcessingData((prev) => ({
        ...prev,
        [fieldName]: formattedDate,
      }));
    }
  };

  const openProcessingModal = (sample) => {
    setSelectedSample(sample);
    setProcessingData({
      processingAction: "",
      grossExamDone: false,
      grossDescription: "",
      sectioningDone: false,
      tissueProcessingSteps: [],
      embeddingDone: false,
      microtomyThickness: 4,
      centrifugationDone: false,
      smearTypes: [],
      stainUsed: [],
      wedgeSmearDone: false,
      bloodStain: "",
      sopFollowed: "",
      processingMethods: [],
      processingDate: new Date().toISOString().split("T")[0],
      staffInitials: "",
      processingNotes: "",
    });
    setProcessingModalOpen(true);
  };

  const handleSubmitProcessing = () => {
    if (submitting) return;

    if (
      !processingData.processingAction ||
      !processingData.staffInitials ||
      !processingData.processingDate
    ) {
      setError(
        intl.formatMessage({
          id: "pathology.processing.error.requiredFields",
          defaultMessage:
            "Please fill in Processing Action, Staff Initials, and Processing Date",
        }),
      );
      return;
    }

    setSubmitting(true);
    setError(null);

    const payload = {
      sampleId: selectedSample?.id,
      pageId: pageData?.id,
      entryId: entryId,
      ...processingData,
    };

    postToOpenElisServer(
      `/rest/notebook/pathology/processing/submit`,
      JSON.stringify(payload),
      (status) => {
        setSubmitting(false);
        if (status === 200) {
          setProcessingModalOpen(false);
          setSelectedSample(null);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            intl.formatMessage({
              id: "pathology.processing.error.submitFailed",
              defaultMessage:
                "Failed to submit processing data. Please try again.",
            }),
          );
        }
      },
    );
  };

  // Calculate stats
  const processedCount = samples.filter((s) => s.status === "COMPLETED").length;
  const inProgressCount = samples.filter(
    (s) => s.status === "IN_PROGRESS",
  ).length;
  const pendingCount = samples.filter((s) => s.status === "PENDING").length;

  // Table headers
  const headers = [
    {
      key: "accessionNumber",
      header: intl.formatMessage({
        id: "pathology.table.accessionNumber",
        defaultMessage: "Accession Number",
      }),
    },
    {
      key: "specimenType",
      header: intl.formatMessage({
        id: "pathology.table.specimenType",
        defaultMessage: "Specimen Type",
      }),
    },
    {
      key: "childSamplesCount",
      header: intl.formatMessage({
        id: "pathology.table.childSamples",
        defaultMessage: "Child Samples",
      }),
    },
    {
      key: "status",
      header: intl.formatMessage({
        id: "pathology.table.status",
        defaultMessage: "Status",
      }),
    },
    {
      key: "actions",
      header: intl.formatMessage({
        id: "pathology.table.actions",
        defaultMessage: "Actions",
      }),
    },
  ];

  const processingActionOptions = [
    { id: "section_slides", text: "Section to slides" },
    { id: "aliquot_lbc", text: "Aliquot for LBC" },
    { id: "aliquot_cell_block", text: "Aliquot for cell block" },
    { id: "aliquot_molecular", text: "Aliquot for molecular testing" },
    { id: "aliquot_biobank", text: "Aliquot for biobanking" },
  ];

  const tissueProcessingOptions = [
    { id: "alcohols", label: "Alcohols" },
    { id: "xylene", label: "Xylene" },
    { id: "paraffin", label: "Paraffin" },
  ];

  const smearTypeOptions = [
    { id: "direct_smear", label: "Direct smear" },
    { id: "lbc_thinprep", label: "LBC (ThinPrep)" },
    { id: "cell_block", label: "Cell block" },
  ];

  const stainOptions = [
    { id: "pap", label: "Pap stain" },
    { id: "romanowsky", label: "Romanowsky" },
    { id: "giemsa", label: "Giemsa" },
    { id: "wright", label: "Wright" },
  ];

  return (
    <div className="pathology-processing-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="pathology.page.processing.title"
            defaultMessage="Sample Processing & Aliquoting"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="pathology.page.processing.description"
            defaultMessage="Create slides, blocks, and aliquots while maintaining traceability. Process samples by type and create child samples."
          />
        </p>
      </div>

      {/* Progress Summary */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.page.processing.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.page.processing.processed"
                  defaultMessage="Processed"
                />
              </span>
              <span className="progress-value">{processedCount}</span>
            </Tile>
            <Tile
              className="progress-tile"
              style={{ backgroundColor: "#e0f0ff" }}
            >
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.page.processing.inProgress"
                  defaultMessage="In Progress"
                />
              </span>
              <span className="progress-value">{inProgressCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.page.processing.pending"
                  defaultMessage="Pending"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Error Display */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          onCloseButtonClick={() => setError(null)}
          lowContrast
        />
      )}

      {/* Sample Table */}
      <div className="sample-grid-container">
        {loading ? (
          <Loading withOverlay={false} description="Loading samples..." />
        ) : samples.length === 0 ? (
          <div className="empty-state">
            <p>
              <FormattedMessage
                id="pathology.page.processing.empty"
                defaultMessage="No samples available for processing. Samples must pass QC on the previous page first."
              />
            </p>
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
                                      : cell.value === "IN_PROGRESS"
                                        ? "blue"
                                        : "gray"
                                  }
                                  size="sm"
                                >
                                  {cell.value}
                                </Tag>
                              ) : cell.info.header === "actions" ? (
                                <Button
                                  kind="ghost"
                                  size="sm"
                                  renderIcon={ChevronRight}
                                  onClick={() => openProcessingModal(sample)}
                                >
                                  <FormattedMessage
                                    id="pathology.page.processing.process"
                                    defaultMessage="Process"
                                  />
                                </Button>
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

      {/* Processing Modal */}
      <Modal
        open={processingModalOpen}
        modalHeading={intl.formatMessage(
          {
            id: "pathology.modal.processing.title",
            defaultMessage: "Process Sample - {accession}",
          },
          { accession: selectedSample?.accessionNumber || "" },
        )}
        primaryButtonText={intl.formatMessage({
          id: "label.button.submit",
          defaultMessage: "Submit",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.button.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setProcessingModalOpen(false);
          setSelectedSample(null);
          setError(null);
        }}
        onRequestSubmit={handleSubmitProcessing}
        primaryButtonDisabled={submitting}
        size="lg"
      >
        <Grid fullWidth>
          {/* Processing Action */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.processing.action"
                defaultMessage="Processing Action"
              />
            </h5>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Select
              id="processingAction"
              name="processingAction"
              labelText={intl.formatMessage({
                id: "pathology.processing.processingAction",
                defaultMessage: "Processing Action *",
              })}
              value={processingData.processingAction}
              onChange={handleInputChange}
            >
              <SelectItem value="" text="" />
              {processingActionOptions.map((opt) => (
                <SelectItem key={opt.id} value={opt.id} text={opt.text} />
              ))}
            </Select>
          </Column>

          {/* Histopathology Processing */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.processing.histopathology"
                defaultMessage="Histopathology Processing"
              />
            </h5>
          </Column>

          <Column lg={4} md={4} sm={4}>
            <Checkbox
              id="grossExamDone"
              name="grossExamDone"
              labelText={intl.formatMessage({
                id: "pathology.processing.grossExamDone",
                defaultMessage: "Gross Examination Done",
              })}
              checked={processingData.grossExamDone}
              onChange={handleInputChange}
            />
          </Column>

          <Column lg={4} md={4} sm={4}>
            <Checkbox
              id="sectioningDone"
              name="sectioningDone"
              labelText={intl.formatMessage({
                id: "pathology.processing.sectioningDone",
                defaultMessage: "Sectioning Done",
              })}
              checked={processingData.sectioningDone}
              onChange={handleInputChange}
            />
          </Column>

          <Column lg={4} md={4} sm={4}>
            <Checkbox
              id="embeddingDone"
              name="embeddingDone"
              labelText={intl.formatMessage({
                id: "pathology.processing.embeddingDone",
                defaultMessage: "Embedding Done",
              })}
              checked={processingData.embeddingDone}
              onChange={handleInputChange}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <NumberInput
              id="microtomyThickness"
              name="microtomyThickness"
              label={intl.formatMessage({
                id: "pathology.processing.microtomyThickness",
                defaultMessage: "Microtomy Thickness (um)",
              })}
              value={processingData.microtomyThickness}
              onChange={(e, { value }) =>
                setProcessingData((prev) => ({
                  ...prev,
                  microtomyThickness: value,
                }))
              }
              min={1}
              max={10}
              step={1}
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="grossDescription"
              name="grossDescription"
              labelText={intl.formatMessage({
                id: "pathology.processing.grossDescription",
                defaultMessage: "Gross Description",
              })}
              value={processingData.grossDescription}
              onChange={handleInputChange}
              rows={3}
            />
          </Column>

          {/* Common Fields */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.processing.log"
                defaultMessage="Processing Log"
              />
            </h5>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              onChange={(dates) => handleDateChange(dates, "processingDate")}
            >
              <DatePickerInput
                id="processingDate"
                labelText={intl.formatMessage({
                  id: "pathology.processing.processingDate",
                  defaultMessage: "Processing Date *",
                })}
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="staffInitials"
              name="staffInitials"
              labelText={intl.formatMessage({
                id: "pathology.processing.staffInitials",
                defaultMessage: "Staff Initials *",
              })}
              value={processingData.staffInitials}
              onChange={handleInputChange}
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="processingNotes"
              name="processingNotes"
              labelText={intl.formatMessage({
                id: "pathology.processing.notes",
                defaultMessage: "Processing Notes",
              })}
              value={processingData.processingNotes}
              onChange={handleInputChange}
              rows={3}
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default PathologySampleProcessingPage;
