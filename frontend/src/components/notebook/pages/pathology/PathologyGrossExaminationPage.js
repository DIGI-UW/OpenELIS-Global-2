import { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  Tag,
  InlineNotification,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  DatePicker,
  DatePickerInput,
  Checkbox,
  Modal,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableSelectAll,
  TableSelectRow,
  NumberInput,
  FileUploaderDropContainer,
  FileUploaderItem,
  Loading,
  TableContainer,
} from "@carbon/react";
import {
  Add,
  Checkmark,
  Renew,
  Camera,
  TrashCan,
  View,
  Edit,
  CheckmarkFilled,
  CloseFilled,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import "../../workflow/NotebookWorkflow.css";

/**
 * PathologyGrossExaminationPage - Gross Examination workflow step.
 * Purpose: Perform gross/macroscopic examination of specimens with photo documentation.
 * Who uses it: Pathologists / Technicians
 *
 * Key activities:
 * - Document specimen appearance, dimensions, weight
 * - Capture gross images (up to 96 photos)
 * - Record abnormalities, margins, orientation
 * - Document sectioning decisions
 */
function PathologyGrossExaminationPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  notebookId,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // Sample list state
  const [samples, setSamples] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Selection state
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);

  // Grossing Modal state
  const [grossingModalOpen, setGrossingModalOpen] = useState(false);
  const [selectedSample, setSelectedSample] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [grossingLoading, setGrossingLoading] = useState(false);
  const [grossingViewMode, setGrossingViewMode] = useState(false);

  // Gross images state
  const [grossImages, setGrossImages] = useState([]);

  // Grossing form data
  const [grossingData, setGrossingData] = useState({
    // Specimen description
    specimenReceived: "",
    specimenDescription: "",
    // Dimensions
    dimensionLength: "",
    dimensionWidth: "",
    dimensionHeight: "",
    dimensionUnit: "cm",
    // Weight
    specimenWeight: "",
    weightUnit: "g",
    // Appearance
    color: "",
    texture: "",
    consistency: "",
    margins: "",
    marginsInked: false,
    inkColors: "",
    // Orientation
    landmarks: "",
    orientation: "",
    orientationMarkers: "",
    // Abnormalities
    abnormalities: "",
    lesionSize: "",
    lesionLocation: "",
    distanceToMargins: "",
    // Sectioning
    numberOfSections: 1,
    sectioningMethod: "",
    sectionsToSubmit: "",
    representativeSections: false,
    entirelySubmitted: false,
    // Free text
    grossDescription: "",
    grossDictation: "",
    // Staff
    examinerName: "",
    examinerInitials: "",
    grossingDate: "",
    grossingStartTime: "",
    grossingEndTime: "",
    // Quality Control
    qcStatus: "PASS", // PASS, FLAG, or FAIL
    qcSpecimenIntegrity: true,
    qcLabelingCorrect: true,
    qcFixationAdequate: true,
    qcDocumentationComplete: true,
    qcIssues: "",
    qcCorrectiveAction: "",
    qcReviewedBy: "",
    qcReviewDate: "",
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
            const transformedSamples = response.map((sample) => {
              const sampleData = sample.data || {};
              return {
                id: String(sample.id || sample.sampleItemId),
                externalId: sample.externalId,
                accessionNumber: sample.accessionNumber,
                sampleType:
                  sample.sampleType || sample.typeOfSample?.description,
                specimenCategory: sample.specimenCategory || "histopathology",
                collectionDate: sample.collectionDate,
                status: sample.pageStatus || "PENDING",
                patientName: sample.patientName,
                // Grossing status (backend saves as grossingCompleted)
                grossingComplete:
                  sampleData.grossingCompleted === true ||
                  sampleData.grossingComplete === true,
                grossDescription: sampleData.grossDescription || "",
                examinerName: sampleData.examinerName || "",
                grossingDate: sampleData.grossingDate || "",
                imageCount: sampleData.grossImageCount || 0,
                // QC status
                qcStatus: sampleData.qcStatus || "PENDING",
              };
            });
            setSamples(transformedSamples);
          } else {
            setSamples([]);
          }
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  // Check if page has a real database ID
  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  const handleGrossingInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setGrossingData((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleGrossingDateChange = (dates, fieldName) => {
    if (dates && dates.length > 0) {
      const date = dates[0];
      const formattedDate = date.toISOString().split("T")[0];
      setGrossingData((prev) => ({
        ...prev,
        [fieldName]: formattedDate,
      }));
    }
  };

  // Open grossing modal
  const openGrossingModal = (sample) => {
    setSelectedSample(sample);
    setGrossingViewMode(false);
    setGrossingLoading(true);
    setGrossImages([]);

    // Reset form data with defaults
    setGrossingData({
      specimenReceived: "",
      specimenDescription: "",
      dimensionLength: "",
      dimensionWidth: "",
      dimensionHeight: "",
      dimensionUnit: "cm",
      specimenWeight: "",
      weightUnit: "g",
      color: "",
      texture: "",
      consistency: "",
      margins: "",
      marginsInked: false,
      inkColors: "",
      landmarks: "",
      orientation: "",
      orientationMarkers: "",
      abnormalities: "",
      lesionSize: "",
      lesionLocation: "",
      distanceToMargins: "",
      numberOfSections: 1,
      sectioningMethod: "",
      sectionsToSubmit: "",
      representativeSections: false,
      entirelySubmitted: false,
      grossDescription: "",
      grossDictation: "",
      examinerName: "",
      examinerInitials: "",
      grossingDate: new Date().toISOString().split("T")[0],
      grossingStartTime: "",
      grossingEndTime: "",
      // QC fields
      qcStatus: "PASS",
      qcSpecimenIntegrity: true,
      qcLabelingCorrect: true,
      qcFixationAdequate: true,
      qcDocumentationComplete: true,
      qcIssues: "",
      qcCorrectiveAction: "",
      qcReviewedBy: "",
      qcReviewDate: new Date().toISOString().split("T")[0],
    });

    setGrossingModalOpen(true);

    // Fetch existing grossing data if available
    if (pageData?.id && sample?.id) {
      getFromOpenElisServer(
        `/rest/notebook/pathology/grossing/${sample.id}?pageId=${pageData.id}`,
        (response) => {
          setGrossingLoading(false);
          if (response && response.success && response.hasData) {
            setGrossingViewMode(true);
            setGrossingData((prev) => ({
              ...prev,
              specimenReceived: response.specimenReceived || "",
              specimenDescription: response.specimenDescription || "",
              dimensionLength: response.dimensionLength || "",
              dimensionWidth: response.dimensionWidth || "",
              dimensionHeight: response.dimensionHeight || "",
              dimensionUnit: response.dimensionUnit || "cm",
              specimenWeight: response.specimenWeight || "",
              weightUnit: response.weightUnit || "g",
              color: response.color || "",
              texture: response.texture || "",
              consistency: response.consistency || "",
              margins: response.margins || "",
              marginsInked: response.marginsInked === true,
              inkColors: response.inkColors || "",
              landmarks: response.landmarks || "",
              orientation: response.orientation || "",
              orientationMarkers: response.orientationMarkers || "",
              abnormalities: response.abnormalities || "",
              lesionSize: response.lesionSize || "",
              lesionLocation: response.lesionLocation || "",
              distanceToMargins: response.distanceToMargins || "",
              numberOfSections: response.numberOfSections || 1,
              sectioningMethod: response.sectioningMethod || "",
              sectionsToSubmit: response.sectionsToSubmit || "",
              representativeSections: response.representativeSections === true,
              entirelySubmitted: response.entirelySubmitted === true,
              grossDescription: response.grossDescription || "",
              grossDictation: response.grossDictation || "",
              examinerName: response.examinerName || "",
              examinerInitials: response.examinerInitials || "",
              grossingDate: response.grossingDate || "",
              grossingStartTime: response.grossingStartTime || "",
              grossingEndTime: response.grossingEndTime || "",
              // QC fields
              qcStatus: response.qcStatus || "PASS",
              qcSpecimenIntegrity: response.qcSpecimenIntegrity !== false,
              qcLabelingCorrect: response.qcLabelingCorrect !== false,
              qcFixationAdequate: response.qcFixationAdequate !== false,
              qcDocumentationComplete:
                response.qcDocumentationComplete !== false,
              qcIssues: response.qcIssues || "",
              qcCorrectiveAction: response.qcCorrectiveAction || "",
              qcReviewedBy: response.qcReviewedBy || "",
              qcReviewDate: response.qcReviewDate || "",
            }));

            // Load existing images
            if (response.grossImages && Array.isArray(response.grossImages)) {
              const loadedImages = response.grossImages.map((img, index) => ({
                id: `existing-${index}`,
                fileName: img.fileName || `Image ${index + 1}`,
                description: img.description || "",
                imageNumber: index + 1,
                status: "complete",
                preview: img.base64Data || img.imageUrl || null,
                base64Data: img.base64Data || null,
                isExisting: true,
              }));
              setGrossImages(loadedImages);
            }
          }
        },
      );
    } else {
      setGrossingLoading(false);
    }
  };

  // Handle image upload
  const handleImageUpload = useCallback(
    (event, { addedFiles }) => {
      const accession = selectedSample?.accessionNumber || "SAMPLE";
      const newFiles = addedFiles.map((file, index) => {
        const imageNum = grossImages.length + index + 1;
        const extension = file.name.split(".").pop() || "jpg";
        const generatedFileName = `${accession}_GROSS_${String(imageNum).padStart(2, "0")}.${extension}`;
        return {
          id: `new-${Date.now()}-${index}`,
          file: file,
          fileName: generatedFileName,
          originalFileName: file.name,
          description: "",
          imageNumber: imageNum,
          status: "uploading",
          preview: null,
          base64Data: null,
          isExisting: false,
        };
      });

      setGrossImages((prev) => [...prev, ...newFiles]);

      // Convert files to base64
      newFiles.forEach((fileObj) => {
        const reader = new FileReader();
        reader.onload = () => {
          setGrossImages((prev) =>
            prev.map((img) =>
              img.id === fileObj.id
                ? {
                    ...img,
                    status: "complete",
                    preview: reader.result,
                    base64Data: reader.result,
                  }
                : img,
            ),
          );
        };
        reader.readAsDataURL(fileObj.file);
      });
    },
    [grossImages.length, selectedSample?.accessionNumber],
  );

  // Remove image
  const handleRemoveImage = useCallback((imageId) => {
    setGrossImages((prev) => prev.filter((img) => img.id !== imageId));
  }, []);

  // Update image description
  const handleImageDescriptionChange = useCallback((imageId, description) => {
    setGrossImages((prev) =>
      prev.map((img) => (img.id === imageId ? { ...img, description } : img)),
    );
  }, []);

  // Submit grossing data
  const handleSubmitGrossing = () => {
    if (submitting) return;

    setSubmitting(true);
    setError(null);

    const payload = {
      sampleId: selectedSample.id,
      pageId: pageData?.id,
      ...grossingData,
      grossingComplete: true,
      grossImages: grossImages.map((img) => ({
        base64Data: img.base64Data,
        fileName: img.fileName,
        description: img.description,
      })),
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/pathology/grossing/submit`,
      JSON.stringify(payload),
      (response) => {
        setSubmitting(false);
        if (response && response.success) {
          setGrossingModalOpen(false);
          setSuccessMessage(
            intl.formatMessage({
              id: "pathology.grossing.success",
              defaultMessage: "Gross examination saved successfully.",
            }),
          );
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(response?.error || "Failed to save gross examination.");
        }
      },
    );
  };

  // Mark samples as complete
  const handleMarkComplete = () => {
    if (submitting || selectedSampleIds.length === 0) return;

    const samplesToComplete = samples.filter(
      (s) => selectedSampleIds.includes(s.id) && s.grossingComplete,
    );

    if (samplesToComplete.length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.grossing.error.noComplete",
          defaultMessage:
            "Please complete gross examination before marking samples as complete.",
        }),
      );
      return;
    }

    setSubmitting(true);
    setError(null);

    const sampleIds = samplesToComplete.map((s) => parseInt(s.id, 10));

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData?.id}/samples/status`,
      JSON.stringify({
        sampleIds: sampleIds,
        status: "COMPLETED",
      }),
      (response) => {
        setSubmitting(false);
        if (response && response.success) {
          setSelectedSampleIds([]);
          setSuccessMessage(
            intl.formatMessage(
              {
                id: "pathology.grossing.success.complete",
                defaultMessage:
                  "Successfully marked {count} samples as complete.",
              },
              { count: samplesToComplete.length },
            ),
          );
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(response?.error || "Failed to update status.");
        }
      },
    );
  };

  // Handle selection changes
  const handleSelectionChange = (selectedRows) => {
    const ids = selectedRows.map((row) => row.id);
    setSelectedSampleIds(ids);
  };

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
      key: "sampleType",
      header: intl.formatMessage({
        id: "pathology.table.specimenType",
        defaultMessage: "Specimen Type",
      }),
    },
    {
      key: "grossingStatus",
      header: intl.formatMessage({
        id: "pathology.table.grossingStatus",
        defaultMessage: "Grossing Status",
      }),
    },
    {
      key: "examinerName",
      header: intl.formatMessage({
        id: "pathology.table.examiner",
        defaultMessage: "Examiner",
      }),
    },
    {
      key: "imageCount",
      header: intl.formatMessage({
        id: "pathology.table.images",
        defaultMessage: "Images",
      }),
    },
    {
      key: "qcStatus",
      header: intl.formatMessage({
        id: "pathology.table.qcStatus",
        defaultMessage: "QC",
      }),
    },
    {
      key: "status",
      header: intl.formatMessage({
        id: "pathology.table.status",
        defaultMessage: "Status",
      }),
    },
  ];

  // Computed values
  const completedCount = samples.filter((s) => s.grossingComplete).length;
  const pendingCount = samples.filter(
    (s) => s.status === "PENDING" || !s.grossingComplete,
  ).length;

  return (
    <div className="pathology-page gross-examination-page">
      {/* Header */}
      <Grid className="page-header">
        <Column lg={12} md={8} sm={4}>
          <h3>
            <FormattedMessage
              id="pathology.page.grossExamination.title"
              defaultMessage="Gross Examination"
            />
          </h3>
          <p className="page-description">
            <FormattedMessage
              id="pathology.page.grossExamination.description"
              defaultMessage="Perform macroscopic examination of specimens with photo documentation."
            />
          </p>
        </Column>
        <Column lg={4} md={8} sm={4} className="header-actions">
          <Button
            kind="ghost"
            size="sm"
            renderIcon={Renew}
            onClick={loadPageSamples}
            disabled={loading}
          >
            <FormattedMessage
              id="pathology.page.refresh"
              defaultMessage="Refresh"
            />
          </Button>
        </Column>
      </Grid>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "notification.error",
            defaultMessage: "Error",
          })}
          subtitle={error}
          onCloseButtonClick={() => setError(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {successMessage && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({
            id: "notification.success",
            defaultMessage: "Success",
          })}
          subtitle={successMessage}
          onCloseButtonClick={() => setSuccessMessage(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Summary Stats */}
      <Grid className="summary-stats" style={{ marginBottom: "1rem" }}>
        <Column lg={4} md={4} sm={4}>
          <Tile className="stat-tile">
            <span className="stat-value">{samples.length}</span>
            <span className="stat-label">
              <FormattedMessage
                id="pathology.stats.total"
                defaultMessage="Total Samples"
              />
            </span>
          </Tile>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Tile className="stat-tile">
            <span className="stat-value" style={{ color: "#24a148" }}>
              {completedCount}
            </span>
            <span className="stat-label">
              <FormattedMessage
                id="pathology.stats.grossed"
                defaultMessage="Grossed"
              />
            </span>
          </Tile>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Tile className="stat-tile">
            <span className="stat-value" style={{ color: "#f1c21b" }}>
              {pendingCount}
            </span>
            <span className="stat-label">
              <FormattedMessage
                id="pathology.stats.pending"
                defaultMessage="Pending"
              />
            </span>
          </Tile>
        </Column>
      </Grid>

      {/* Action Buttons */}
      {hasRealPageId && samples.length > 0 && (
        <div
          className="action-buttons"
          style={{ marginBottom: "1rem", display: "flex", gap: "0.5rem" }}
        >
          <Button
            kind="primary"
            size="md"
            renderIcon={Edit}
            onClick={() => {
              if (selectedSampleIds.length === 1) {
                const sample = samples.find(
                  (s) => s.id === selectedSampleIds[0],
                );
                if (sample) openGrossingModal(sample);
              }
            }}
            disabled={selectedSampleIds.length !== 1 || submitting}
          >
            <FormattedMessage
              id="pathology.page.performGrossing"
              defaultMessage="Perform Grossing"
            />
          </Button>
          <Button
            kind="secondary"
            size="md"
            renderIcon={Checkmark}
            onClick={handleMarkComplete}
            disabled={selectedSampleIds.length === 0 || submitting}
          >
            <FormattedMessage
              id="pathology.page.markComplete"
              defaultMessage="Mark Complete ({count})"
              values={{ count: selectedSampleIds.length }}
            />
          </Button>
        </div>
      )}

      {/* Sample Table */}
      <div className="sample-table">
        {loading ? (
          <Loading description="Loading samples..." withOverlay={false} />
        ) : !hasRealPageId ? (
          <div className="empty-state">
            <p>
              <FormattedMessage
                id="pathology.page.noPageId"
                defaultMessage="No page data available. Samples will appear here once the workflow is started."
              />
            </p>
          </div>
        ) : samples.length === 0 ? (
          <div className="empty-state">
            <p>
              <FormattedMessage
                id="pathology.page.grossExamination.empty"
                defaultMessage="No samples available for gross examination. Samples must pass QC first."
              />
            </p>
          </div>
        ) : (
          <DataTable
            rows={samples.map((s) => ({
              ...s,
              grossingStatus: s.grossingComplete ? "Complete" : "Pending",
            }))}
            headers={headers}
            isSortable
            render={({
              rows,
              headers,
              getTableProps,
              getHeaderProps,
              getRowProps,
              getSelectionProps,
              selectedRows,
            }) => {
              if (
                selectedRows.length !== selectedSampleIds.length ||
                !selectedRows.every((r) => selectedSampleIds.includes(r.id))
              ) {
                setTimeout(() => handleSelectionChange(selectedRows), 0);
              }

              return (
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
                      {rows.map((row) => {
                        const sample = samples.find((s) => s.id === row.id);
                        return (
                          <TableRow key={row.id} {...getRowProps({ row })}>
                            <TableSelectRow {...getSelectionProps({ row })} />
                            {row.cells.map((cell) => (
                              <TableCell key={cell.id}>
                                {cell.info.header === "grossingStatus" ? (
                                  <span
                                    style={{
                                      display: "flex",
                                      alignItems: "center",
                                      gap: "0.25rem",
                                      cursor: "pointer",
                                    }}
                                    onClick={() => openGrossingModal(sample)}
                                    title="Click to view/edit gross examination"
                                  >
                                    {sample?.grossingComplete ? (
                                      <>
                                        <CheckmarkFilled
                                          size={16}
                                          style={{ color: "#24a148" }}
                                        />
                                        <span>Complete</span>
                                      </>
                                    ) : (
                                      <Tag type="gray" size="sm">
                                        Pending
                                      </Tag>
                                    )}
                                  </span>
                                ) : cell.info.header === "status" ? (
                                  <Tag
                                    type={
                                      sample?.status === "COMPLETED"
                                        ? "green"
                                        : sample?.status === "IN_PROGRESS"
                                          ? "blue"
                                          : "gray"
                                    }
                                    size="sm"
                                  >
                                    {sample?.status || "PENDING"}
                                  </Tag>
                                ) : cell.info.header === "imageCount" ? (
                                  <span>
                                    {sample?.imageCount > 0 ? (
                                      <>
                                        <Camera size={16} /> {sample.imageCount}
                                      </>
                                    ) : (
                                      "-"
                                    )}
                                  </span>
                                ) : cell.info.header === "qcStatus" ? (
                                  <Tag
                                    type={
                                      sample?.qcStatus === "PASS"
                                        ? "green"
                                        : sample?.qcStatus === "FLAG"
                                          ? "gold"
                                          : sample?.qcStatus === "FAIL"
                                            ? "red"
                                            : "gray"
                                    }
                                    size="sm"
                                  >
                                    {sample?.qcStatus === "PASS"
                                      ? "✓ Pass"
                                      : sample?.qcStatus === "FLAG"
                                        ? "⚠ Flag"
                                        : sample?.qcStatus === "FAIL"
                                          ? "✗ Fail"
                                          : "-"}
                                  </Tag>
                                ) : (
                                  cell.value || "-"
                                )}
                              </TableCell>
                            ))}
                          </TableRow>
                        );
                      })}
                    </TableBody>
                  </Table>
                </TableContainer>
              );
            }}
          />
        )}
      </div>

      {/* Grossing Modal */}
      <Modal
        open={grossingModalOpen}
        modalHeading={intl.formatMessage(
          {
            id: "pathology.modal.grossing.title",
            defaultMessage: "Gross Examination - {accession}",
          },
          { accession: selectedSample?.accessionNumber || "" },
        )}
        primaryButtonText={
          grossingViewMode
            ? intl.formatMessage({
                id: "pathology.modal.edit",
                defaultMessage: "Edit",
              })
            : intl.formatMessage({
                id: "pathology.modal.save",
                defaultMessage: "Save",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "pathology.modal.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setGrossingModalOpen(false);
          setSelectedSample(null);
          setGrossImages([]);
          setGrossingViewMode(false);
          setError(null);
        }}
        onRequestSubmit={
          grossingViewMode
            ? () => setGrossingViewMode(false)
            : handleSubmitGrossing
        }
        primaryButtonDisabled={submitting || grossingLoading}
        size="lg"
        hasScrollingContent
        preventCloseOnClickOutside
      >
        {grossingLoading ? (
          <div style={{ textAlign: "center", padding: "2rem" }}>
            <Loading description="Loading data..." withOverlay={false} />
          </div>
        ) : (
          <div style={{ maxHeight: "70vh", overflowY: "auto" }}>
            {/* View Mode Banner */}
            {grossingViewMode && (
              <InlineNotification
                kind="info"
                title={intl.formatMessage({
                  id: "pathology.grossing.viewMode",
                  defaultMessage: "Viewing existing data",
                })}
                subtitle={intl.formatMessage({
                  id: "pathology.grossing.viewMode.description",
                  defaultMessage: "Click 'Edit' to make changes.",
                })}
                lowContrast
                hideCloseButton
                style={{ marginBottom: "1rem" }}
              />
            )}

            {/* Specimen Description */}
            <h5 style={{ marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.grossing.section.specimen"
                defaultMessage="Specimen Description"
              />
            </h5>
            <Grid>
              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="specimenReceived"
                  name="specimenReceived"
                  labelText={intl.formatMessage({
                    id: "pathology.grossing.specimenReceived",
                    defaultMessage: "Specimen Received",
                  })}
                  value={grossingData.specimenReceived}
                  onChange={handleGrossingInputChange}
                  disabled={grossingViewMode}
                />
              </Column>
              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="specimenDescription"
                  name="specimenDescription"
                  labelText={intl.formatMessage({
                    id: "pathology.grossing.specimenDescription",
                    defaultMessage: "Specimen Description",
                  })}
                  value={grossingData.specimenDescription}
                  onChange={handleGrossingInputChange}
                  disabled={grossingViewMode}
                />
              </Column>
            </Grid>

            {/* Dimensions */}
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.grossing.section.dimensions"
                defaultMessage="Dimensions & Weight"
              />
            </h5>
            <Grid>
              <Column lg={4} md={2} sm={2}>
                <TextInput
                  id="dimensionLength"
                  name="dimensionLength"
                  labelText={intl.formatMessage({
                    id: "pathology.grossing.length",
                    defaultMessage: "Length",
                  })}
                  value={grossingData.dimensionLength}
                  onChange={handleGrossingInputChange}
                  disabled={grossingViewMode}
                />
              </Column>
              <Column lg={4} md={2} sm={2}>
                <TextInput
                  id="dimensionWidth"
                  name="dimensionWidth"
                  labelText={intl.formatMessage({
                    id: "pathology.grossing.width",
                    defaultMessage: "Width",
                  })}
                  value={grossingData.dimensionWidth}
                  onChange={handleGrossingInputChange}
                  disabled={grossingViewMode}
                />
              </Column>
              <Column lg={4} md={2} sm={2}>
                <TextInput
                  id="dimensionHeight"
                  name="dimensionHeight"
                  labelText={intl.formatMessage({
                    id: "pathology.grossing.height",
                    defaultMessage: "Height",
                  })}
                  value={grossingData.dimensionHeight}
                  onChange={handleGrossingInputChange}
                  disabled={grossingViewMode}
                />
              </Column>
              <Column lg={4} md={2} sm={2}>
                <Select
                  id="dimensionUnit"
                  name="dimensionUnit"
                  labelText={intl.formatMessage({
                    id: "pathology.grossing.unit",
                    defaultMessage: "Unit",
                  })}
                  value={grossingData.dimensionUnit}
                  onChange={handleGrossingInputChange}
                  disabled={grossingViewMode}
                >
                  <SelectItem value="cm" text="cm" />
                  <SelectItem value="mm" text="mm" />
                </Select>
              </Column>
            </Grid>

            <Grid style={{ marginTop: "1rem" }}>
              <Column lg={4} md={2} sm={2}>
                <TextInput
                  id="specimenWeight"
                  name="specimenWeight"
                  labelText={intl.formatMessage({
                    id: "pathology.grossing.weight",
                    defaultMessage: "Weight",
                  })}
                  value={grossingData.specimenWeight}
                  onChange={handleGrossingInputChange}
                  disabled={grossingViewMode}
                />
              </Column>
              <Column lg={4} md={2} sm={2}>
                <Select
                  id="weightUnit"
                  name="weightUnit"
                  labelText={intl.formatMessage({
                    id: "pathology.grossing.weightUnit",
                    defaultMessage: "Weight Unit",
                  })}
                  value={grossingData.weightUnit}
                  onChange={handleGrossingInputChange}
                  disabled={grossingViewMode}
                >
                  <SelectItem value="g" text="g" />
                  <SelectItem value="mg" text="mg" />
                  <SelectItem value="kg" text="kg" />
                </Select>
              </Column>
            </Grid>

            {/* Appearance */}
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.grossing.section.appearance"
                defaultMessage="Appearance"
              />
            </h5>
            <Grid>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="color"
                  name="color"
                  labelText={intl.formatMessage({
                    id: "pathology.grossing.color",
                    defaultMessage: "Color",
                  })}
                  value={grossingData.color}
                  onChange={handleGrossingInputChange}
                  disabled={grossingViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="texture"
                  name="texture"
                  labelText={intl.formatMessage({
                    id: "pathology.grossing.texture",
                    defaultMessage: "Texture",
                  })}
                  value={grossingData.texture}
                  onChange={handleGrossingInputChange}
                  disabled={grossingViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="consistency"
                  name="consistency"
                  labelText={intl.formatMessage({
                    id: "pathology.grossing.consistency",
                    defaultMessage: "Consistency",
                  })}
                  value={grossingData.consistency}
                  onChange={handleGrossingInputChange}
                  disabled={grossingViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="margins"
                  name="margins"
                  labelText={intl.formatMessage({
                    id: "pathology.grossing.margins",
                    defaultMessage: "Margins",
                  })}
                  value={grossingData.margins}
                  onChange={handleGrossingInputChange}
                  disabled={grossingViewMode}
                />
              </Column>
            </Grid>

            <Grid style={{ marginTop: "1rem" }}>
              <Column lg={4} md={4} sm={4}>
                <Checkbox
                  id="marginsInked"
                  name="marginsInked"
                  labelText={intl.formatMessage({
                    id: "pathology.grossing.marginsInked",
                    defaultMessage: "Margins Inked",
                  })}
                  checked={grossingData.marginsInked}
                  onChange={(e) =>
                    setGrossingData((prev) => ({
                      ...prev,
                      marginsInked: e.target.checked,
                    }))
                  }
                  disabled={grossingViewMode}
                />
              </Column>
              {grossingData.marginsInked && (
                <Column lg={4} md={4} sm={4}>
                  <TextInput
                    id="inkColors"
                    name="inkColors"
                    labelText={intl.formatMessage({
                      id: "pathology.grossing.inkColors",
                      defaultMessage: "Ink Colors",
                    })}
                    value={grossingData.inkColors}
                    onChange={handleGrossingInputChange}
                    disabled={grossingViewMode}
                  />
                </Column>
              )}
            </Grid>

            {/* Gross Description */}
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.grossing.section.description"
                defaultMessage="Gross Description"
              />
            </h5>
            <Grid>
              <Column lg={16} md={8} sm={4}>
                <TextArea
                  id="grossDescription"
                  name="grossDescription"
                  labelText={intl.formatMessage({
                    id: "pathology.grossing.grossDescription",
                    defaultMessage: "Gross Description",
                  })}
                  value={grossingData.grossDescription}
                  onChange={handleGrossingInputChange}
                  disabled={grossingViewMode}
                  rows={4}
                />
              </Column>
            </Grid>

            {/* Staff Info */}
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.grossing.section.staff"
                defaultMessage="Staff & Date"
              />
            </h5>
            <Grid>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="examinerName"
                  name="examinerName"
                  labelText={intl.formatMessage({
                    id: "pathology.grossing.examinerName",
                    defaultMessage: "Examiner Name",
                  })}
                  value={grossingData.examinerName}
                  onChange={handleGrossingInputChange}
                  disabled={grossingViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="examinerInitials"
                  name="examinerInitials"
                  labelText={intl.formatMessage({
                    id: "pathology.grossing.examinerInitials",
                    defaultMessage: "Initials",
                  })}
                  value={grossingData.examinerInitials}
                  onChange={handleGrossingInputChange}
                  disabled={grossingViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <DatePicker
                  datePickerType="single"
                  onChange={(dates) =>
                    handleGrossingDateChange(dates, "grossingDate")
                  }
                  value={grossingData.grossingDate}
                >
                  <DatePickerInput
                    id="grossingDate"
                    labelText={intl.formatMessage({
                      id: "pathology.grossing.date",
                      defaultMessage: "Date",
                    })}
                    placeholder="mm/dd/yyyy"
                    disabled={grossingViewMode}
                  />
                </DatePicker>
              </Column>
            </Grid>

            {/* Quality Control Section */}
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.qc.section.title"
                defaultMessage="Quality Control & Assessment"
              />
            </h5>

            {/* QC Status Indicator */}
            <div
              style={{
                padding: "1rem",
                marginBottom: "1rem",
                backgroundColor:
                  grossingData.qcStatus === "PASS"
                    ? "#defbe6"
                    : grossingData.qcStatus === "FLAG"
                      ? "#fff8e1"
                      : "#fff1f1",
                borderRadius: "4px",
                border: `1px solid ${
                  grossingData.qcStatus === "PASS"
                    ? "#24a148"
                    : grossingData.qcStatus === "FLAG"
                      ? "#f1c21b"
                      : "#da1e28"
                }`,
              }}
            >
              <Grid>
                <Column lg={8} md={4} sm={4}>
                  <Select
                    id="qcStatus"
                    name="qcStatus"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.status",
                      defaultMessage: "QC Status",
                    })}
                    value={grossingData.qcStatus}
                    onChange={handleGrossingInputChange}
                    disabled={grossingViewMode}
                  >
                    <SelectItem value="PASS" text="✓ Pass - All criteria met" />
                    <SelectItem
                      value="FLAG"
                      text="⚠ Flag - Proceed with caution"
                    />
                    <SelectItem
                      value="FAIL"
                      text="✗ Fail - Requires corrective action"
                    />
                  </Select>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  {grossingData.qcStatus === "FLAG" && (
                    <InlineNotification
                      kind="warning"
                      title={intl.formatMessage({
                        id: "pathology.qc.flagged",
                        defaultMessage: "Sample Flagged",
                      })}
                      subtitle={intl.formatMessage({
                        id: "pathology.qc.flagged.description",
                        defaultMessage:
                          "Processing can continue but issue is documented.",
                      })}
                      lowContrast
                      hideCloseButton
                    />
                  )}
                  {grossingData.qcStatus === "FAIL" && (
                    <InlineNotification
                      kind="error"
                      title={intl.formatMessage({
                        id: "pathology.qc.failed",
                        defaultMessage: "QC Failed",
                      })}
                      subtitle={intl.formatMessage({
                        id: "pathology.qc.failed.description",
                        defaultMessage:
                          "Corrective action required. Sample is still processed to preserve specimen.",
                      })}
                      lowContrast
                      hideCloseButton
                    />
                  )}
                </Column>
              </Grid>
            </div>

            {/* QC Checkpoints */}
            <Grid>
              <Column lg={4} md={4} sm={4}>
                <Checkbox
                  id="qcSpecimenIntegrity"
                  name="qcSpecimenIntegrity"
                  labelText={intl.formatMessage({
                    id: "pathology.qc.specimenIntegrity",
                    defaultMessage: "Specimen integrity verified",
                  })}
                  checked={grossingData.qcSpecimenIntegrity}
                  onChange={(e) =>
                    setGrossingData((prev) => ({
                      ...prev,
                      qcSpecimenIntegrity: e.target.checked,
                    }))
                  }
                  disabled={grossingViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <Checkbox
                  id="qcLabelingCorrect"
                  name="qcLabelingCorrect"
                  labelText={intl.formatMessage({
                    id: "pathology.qc.labelingCorrect",
                    defaultMessage: "Labeling correct & complete",
                  })}
                  checked={grossingData.qcLabelingCorrect}
                  onChange={(e) =>
                    setGrossingData((prev) => ({
                      ...prev,
                      qcLabelingCorrect: e.target.checked,
                    }))
                  }
                  disabled={grossingViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <Checkbox
                  id="qcFixationAdequate"
                  name="qcFixationAdequate"
                  labelText={intl.formatMessage({
                    id: "pathology.qc.fixationAdequate",
                    defaultMessage: "Fixation adequate",
                  })}
                  checked={grossingData.qcFixationAdequate}
                  onChange={(e) =>
                    setGrossingData((prev) => ({
                      ...prev,
                      qcFixationAdequate: e.target.checked,
                    }))
                  }
                  disabled={grossingViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <Checkbox
                  id="qcDocumentationComplete"
                  name="qcDocumentationComplete"
                  labelText={intl.formatMessage({
                    id: "pathology.qc.documentationComplete",
                    defaultMessage: "Documentation complete",
                  })}
                  checked={grossingData.qcDocumentationComplete}
                  onChange={(e) =>
                    setGrossingData((prev) => ({
                      ...prev,
                      qcDocumentationComplete: e.target.checked,
                    }))
                  }
                  disabled={grossingViewMode}
                />
              </Column>
            </Grid>

            {/* QC Issues and Corrective Actions */}
            {(grossingData.qcStatus === "FLAG" ||
              grossingData.qcStatus === "FAIL") && (
              <Grid style={{ marginTop: "1rem" }}>
                <Column lg={8} md={4} sm={4}>
                  <TextArea
                    id="qcIssues"
                    name="qcIssues"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.issues",
                      defaultMessage: "QC Issues Identified",
                    })}
                    placeholder={intl.formatMessage({
                      id: "pathology.qc.issues.placeholder",
                      defaultMessage: "Describe any quality issues observed...",
                    })}
                    value={grossingData.qcIssues}
                    onChange={handleGrossingInputChange}
                    disabled={grossingViewMode}
                    rows={3}
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <TextArea
                    id="qcCorrectiveAction"
                    name="qcCorrectiveAction"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.correctiveAction",
                      defaultMessage: "Corrective Action Taken",
                    })}
                    placeholder={intl.formatMessage({
                      id: "pathology.qc.correctiveAction.placeholder",
                      defaultMessage:
                        "Describe corrective actions or mitigation steps...",
                    })}
                    value={grossingData.qcCorrectiveAction}
                    onChange={handleGrossingInputChange}
                    disabled={grossingViewMode}
                    rows={3}
                  />
                </Column>
              </Grid>
            )}

            {/* QC Reviewer */}
            <Grid style={{ marginTop: "1rem" }}>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="qcReviewedBy"
                  name="qcReviewedBy"
                  labelText={intl.formatMessage({
                    id: "pathology.qc.reviewedBy",
                    defaultMessage: "QC Reviewed By",
                  })}
                  value={grossingData.qcReviewedBy}
                  onChange={handleGrossingInputChange}
                  disabled={grossingViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <DatePicker
                  datePickerType="single"
                  onChange={(dates) =>
                    handleGrossingDateChange(dates, "qcReviewDate")
                  }
                  value={grossingData.qcReviewDate}
                >
                  <DatePickerInput
                    id="qcReviewDate"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.reviewDate",
                      defaultMessage: "QC Review Date",
                    })}
                    placeholder="mm/dd/yyyy"
                    disabled={grossingViewMode}
                  />
                </DatePicker>
              </Column>
            </Grid>

            {/* Image Upload */}
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.grossing.section.images"
                defaultMessage="Gross Images"
              />
            </h5>
            {!grossingViewMode && (
              <FileUploaderDropContainer
                labelText={intl.formatMessage({
                  id: "pathology.grossing.dropImages",
                  defaultMessage:
                    "Drag and drop images here or click to upload (up to 96 images)",
                })}
                accept={["image/jpeg", "image/png", "image/gif"]}
                multiple
                onAddFiles={handleImageUpload}
                disabled={grossImages.length >= 96}
              />
            )}

            {/* Image Gallery */}
            {grossImages.length > 0 && (
              <div
                style={{
                  marginTop: "1rem",
                  display: "grid",
                  gridTemplateColumns: "repeat(auto-fill, minmax(150px, 1fr))",
                  gap: "1rem",
                }}
              >
                {grossImages.map((image) => (
                  <div
                    key={image.id}
                    style={{
                      border: "1px solid #e0e0e0",
                      borderRadius: "4px",
                      padding: "0.5rem",
                    }}
                  >
                    {image.preview ? (
                      <img
                        src={image.preview}
                        alt={image.fileName}
                        style={{
                          width: "100%",
                          height: "100px",
                          objectFit: "cover",
                          borderRadius: "4px",
                        }}
                      />
                    ) : (
                      <div
                        style={{
                          width: "100%",
                          height: "100px",
                          background: "#f4f4f4",
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                        }}
                      >
                        <Camera size={24} />
                      </div>
                    )}
                    <p
                      style={{
                        fontSize: "0.75rem",
                        marginTop: "0.5rem",
                        overflow: "hidden",
                        textOverflow: "ellipsis",
                        whiteSpace: "nowrap",
                      }}
                    >
                      {image.fileName}
                    </p>
                    {!grossingViewMode && (
                      <Button
                        kind="ghost"
                        size="sm"
                        renderIcon={TrashCan}
                        onClick={() => handleRemoveImage(image.id)}
                        hasIconOnly
                        iconDescription="Remove"
                      />
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}

export default PathologyGrossExaminationPage;
