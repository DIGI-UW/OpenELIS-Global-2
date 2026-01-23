import React, {
  useState,
  useEffect,
  useRef,
  useCallback,
  useContext,
  useMemo,
} from "react";
import {
  Button,
  Modal,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  Grid,
  Column,
  Tile,
  Tag,
  Loading,
  FileUploader,
} from "@carbon/react";
import { Save, Checkmark } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * VirologyDarkRoomImagingPage - Page 6 of the Virology & Vaccine Unit workflow.
 * Imaging or fluorescence analysis for virus culture samples.
 *
 * Features:
 * - Display samples from previous page (Virus Culture)
 * - Capture image data and analysis results
 * - Record CPE (Cytopathic Effect) observations
 * - Track fluorescence intensity measurements
 * - Store imaging notes and observations
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The page configuration
 * @param {Object} props.progress - Current workflow progress
 * @param {Function} props.onProgressUpdate - Callback to update progress
 */
function VirologyDarkRoomImagingPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  // State management
  const [samples, setSamples] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);

  // Form fields
  const [imageId, setImageId] = useState("");
  const [cpeObservation, setCpeObservation] = useState("");
  const [fluorescenceIntensity, setFluorescenceIntensity] = useState("");
  const [imagingNotes, setImagingNotes] = useState("");
  const [uploadedFiles, setUploadedFiles] = useState([]);
  const [imagePreviews, setImagePreviews] = useState([]);

  // Image viewer state
  const [imageViewerOpen, setImageViewerOpen] = useState(false);
  const [viewingImages, setViewingImages] = useState([]);
  const [currentImageIndex, setCurrentImageIndex] = useState(0);

  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, subtitle }) => {
      setNotificationVisible(true);
      addNotification({ kind, title, subtitle });
    },
    [addNotification, setNotificationVisible],
  );

  // Load samples for this page
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();

    return () => {
      componentMounted.current = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [entryId, pageData?.id]);

  // Keyboard navigation for image viewer
  useEffect(() => {
    if (!imageViewerOpen || viewingImages.length <= 1) return;

    const handleKeyDown = (e) => {
      if (e.key === "ArrowLeft") {
        setCurrentImageIndex((prev) =>
          prev > 0 ? prev - 1 : viewingImages.length - 1,
        );
      } else if (e.key === "ArrowRight") {
        setCurrentImageIndex((prev) =>
          prev < viewingImages.length - 1 ? prev + 1 : 0,
        );
      } else if (e.key === "Escape") {
        setImageViewerOpen(false);
        setViewingImages([]);
        setCurrentImageIndex(0);
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [imageViewerOpen, viewingImages.length]);

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
    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (fetchedSamples) => {
        if (!componentMounted.current) return;
        setSamples(fetchedSamples || []);
        setLoading(false);
      },
    );
  }, [pageData?.id]);

  const resetForm = useCallback(() => {
    setImageId("");
    setCpeObservation("");
    setFluorescenceIntensity("");
    setImagingNotes("");
    setUploadedFiles([]);
    // Clean up image preview URLs
    imagePreviews.forEach((url) => URL.revokeObjectURL(url));
    setImagePreviews([]);
  }, [imagePreviews]);

  const handleSaveImagingData = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({
          id: "notification.title",
          defaultMessage: "Notification",
        }),
        subtitle: intl.formatMessage({
          id: "virology.imaging.error.noSelection",
          defaultMessage:
            "Please select at least one sample to save imaging data.",
        }),
      });
      return;
    }

    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notification.error",
          defaultMessage: "Error",
        }),
        subtitle: intl.formatMessage({
          id: "virology.imaging.error.noPage",
          defaultMessage:
            "Cannot save imaging data until the page is saved. Please save the page first.",
        }),
      });
      return;
    }

    setLoading(true);

    // Convert uploaded files to base64 for storage in JSONB
    const convertFilesToBase64 = async () => {
      const base64Files = [];
      for (const file of uploadedFiles) {
        const base64 = await new Promise((resolve, reject) => {
          const reader = new FileReader();
          reader.onloadend = () => resolve(reader.result);
          reader.onerror = reject;
          reader.readAsDataURL(file);
        });
        base64Files.push({
          name: file.name,
          size: file.size,
          type: file.type,
          data: base64,
        });
      }
      return base64Files;
    };

    // Build payload
    const buildPayload = async () => {
      const payload = {
        notebookPageId: pageData.id,
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      };

      if (imageId && imageId.trim() !== "") {
        payload.imageId = imageId.trim();
      }

      if (cpeObservation && cpeObservation !== "") {
        payload.cpeObservation = cpeObservation;
      }

      if (fluorescenceIntensity && fluorescenceIntensity.trim() !== "") {
        payload.fluorescenceIntensity = fluorescenceIntensity.trim();
      }

      if (imagingNotes && imagingNotes.trim() !== "") {
        payload.notes = imagingNotes.trim();
      }

      // Add uploaded images as base64
      if (uploadedFiles.length > 0) {
        payload.uploadedImages = await convertFilesToBase64();
      }

      return payload;
    };

    // Send request
    buildPayload()
      .then((payload) => {
        console.log("Sending imaging data:", payload);
        postToOpenElisServerJsonResponse(
          "/rest/virology/dark-room-imaging",
          JSON.stringify(payload),
          (response) => {
            setLoading(false);
            console.log("Response data:", response);

            if (response.success) {
              notify({
                kind: NotificationKinds.success,
                title: intl.formatMessage({
                  id: "notification.success",
                  defaultMessage: "Success",
                }),
                subtitle: intl.formatMessage(
                  {
                    id: "virology.imaging.success.saved",
                    defaultMessage:
                      "Dark room imaging data saved for {count} sample(s).",
                  },
                  {
                    count: response.samplesUpdated || selectedSampleIds.length,
                  },
                ),
              });

              setModalOpen(false);
              resetForm();
              setSelectedSampleIds([]);
              loadPageSamples();

              if (onProgressUpdate) {
                onProgressUpdate();
              }
            } else {
              notify({
                kind: NotificationKinds.error,
                title: intl.formatMessage({
                  id: "notification.error",
                  defaultMessage: "Error",
                }),
                subtitle: intl.formatMessage({
                  id: "virology.imaging.error.save",
                  defaultMessage:
                    "Failed to save imaging data. Please try again.",
                }),
              });
            }
          },
        );
      })
      .catch((error) => {
        setLoading(false);
        notify({
          kind: NotificationKinds.error,
          title: intl.formatMessage({
            id: "notification.error",
            defaultMessage: "Error",
          }),
          subtitle: intl.formatMessage({
            id: "virology.imaging.error.save",
            defaultMessage: "Failed to save imaging data. Please try again.",
          }),
        });
        console.error("Error saving imaging data:", error);
      });
  }, [
    selectedSampleIds,
    pageData?.id,
    imageId,
    cpeObservation,
    fluorescenceIntensity,
    imagingNotes,
    uploadedFiles,
    intl,
    notify,
    loadPageSamples,
    onProgressUpdate,
    resetForm,
  ]);

  const handleCompleteImaging = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({
          id: "notification.title",
          defaultMessage: "Notification",
        }),
        subtitle: intl.formatMessage({
          id: "virology.imaging.error.noSelectionComplete",
          defaultMessage: "Please select at least one sample to complete.",
        }),
      });
      return;
    }

    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notification.error",
          defaultMessage: "Error",
        }),
        subtitle: intl.formatMessage({
          id: "virology.imaging.error.noPageComplete",
          defaultMessage:
            "Cannot complete samples: Page not properly initialized.",
        }),
      });
      return;
    }

    setLoading(true);

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        status: "COMPLETED",
      }),
      (response) => {
        setLoading(false);

        if (response && (response.success || response === 200)) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage({
              id: "notification.success",
              defaultMessage: "Success",
            }),
            subtitle: intl.formatMessage(
              {
                id: "virology.imaging.success.completed",
                defaultMessage:
                  "Completed dark room imaging for {count} sample(s).",
              },
              { count: selectedSampleIds.length },
            ),
          });
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          notify({
            kind: NotificationKinds.error,
            title: intl.formatMessage({
              id: "notification.error",
              defaultMessage: "Error",
            }),
            subtitle: intl.formatMessage({
              id: "virology.imaging.error.complete",
              defaultMessage: "Failed to complete imaging. Please try again.",
            }),
          });
        }
      },
    );
  }, [
    selectedSampleIds,
    pageData?.id,
    intl,
    notify,
    loadPageSamples,
    onProgressUpdate,
  ]);

  const getAdditionalColumns = (intl) => [
    {
      key: "imageId",
      header: intl.formatMessage({
        id: "virology.imaging.column.imageId",
        defaultMessage: "Image ID",
      }),
      render: (value, sample) => {
        const imgId = value || sample?.imageId || sample?.data?.imageId;
        return imgId || "-";
      },
    },
    {
      key: "cpeObservation",
      header: intl.formatMessage({
        id: "virology.imaging.column.cpe",
        defaultMessage: "CPE Observation",
      }),
      render: (value, sample) => {
        const cpe =
          value || sample?.cpeObservation || sample?.data?.cpeObservation;
        if (!cpe) return "-";
        return <Tag type="blue">{cpe}</Tag>;
      },
    },
    {
      key: "fluorescenceIntensity",
      header: intl.formatMessage({
        id: "virology.imaging.column.fluorescence",
        defaultMessage: "Fluorescence Intensity",
      }),
      render: (value, sample) => {
        const intensity =
          value ||
          sample?.fluorescenceIntensity ||
          sample?.data?.fluorescenceIntensity;
        return intensity || "-";
      },
    },
    {
      key: "imagingNotes",
      header: intl.formatMessage({
        id: "virology.imaging.column.notes",
        defaultMessage: "Imaging Notes",
      }),
      render: (value, sample) => {
        const notes =
          value ||
          sample?.imagingNotes ||
          sample?.data?.imagingNotes ||
          sample?.notes ||
          sample?.data?.notes;
        return notes || "-";
      },
    },
    {
      key: "uploadedImages",
      header: intl.formatMessage({
        id: "virology.imaging.column.images",
        defaultMessage: "Images",
      }),
      render: (value, sample) => {
        const images =
          value || sample?.uploadedImages || sample?.data?.uploadedImages;
        if (!images || !Array.isArray(images) || images.length === 0) {
          return "-";
        }
        return (
          <Button
            kind="ghost"
            size="sm"
            onClick={() => {
              setViewingImages(images);
              setCurrentImageIndex(0);
              setImageViewerOpen(true);
            }}
          >
            <FormattedMessage
              id="virology.imaging.viewImages"
              defaultMessage="View {count} image(s)"
              values={{ count: images.length }}
            />
          </Button>
        );
      },
    },
  ];

  const additionalColumns = useMemo(() => getAdditionalColumns(intl), [intl]);

  // Split samples into pending/in-progress and completed
  const pendingSamples = useMemo(
    () =>
      samples.filter(
        (sample) =>
          sample.pageStatus === "PENDING" ||
          sample.pageStatus === "IN_PROGRESS",
      ),
    [samples],
  );

  const completedSamples = useMemo(
    () => samples.filter((sample) => sample.pageStatus === "COMPLETED"),
    [samples],
  );

  const pendingCount = useMemo(() => pendingSamples.length, [pendingSamples]);
  const completedCount = useMemo(
    () => completedSamples.length,
    [completedSamples],
  );

  // CPE observation options
  const cpeOptions = [
    {
      id: "",
      text: intl.formatMessage({
        id: "select.placeholder",
        defaultMessage: "Select...",
      }),
    },
    {
      id: "None",
      text: intl.formatMessage({
        id: "virology.imaging.cpe.none",
        defaultMessage: "None",
      }),
    },
    {
      id: "Minimal",
      text: intl.formatMessage({
        id: "virology.imaging.cpe.minimal",
        defaultMessage: "Minimal",
      }),
    },
    {
      id: "Moderate",
      text: intl.formatMessage({
        id: "virology.imaging.cpe.moderate",
        defaultMessage: "Moderate",
      }),
    },
    {
      id: "Extensive",
      text: intl.formatMessage({
        id: "virology.imaging.cpe.extensive",
        defaultMessage: "Extensive",
      }),
    },
    {
      id: "Complete",
      text: intl.formatMessage({
        id: "virology.imaging.cpe.complete",
        defaultMessage: "Complete",
      }),
    },
  ];

  return (
    <div className="virology-dark-room-imaging-page">
      {loading && <Loading />}

      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <div className="page-header-section">
            <Tile className="instructions-tile">
              <h4>
                <FormattedMessage
                  id="virology.imaging.instructions.title"
                  defaultMessage="Dark Room Imaging Instructions"
                />
              </h4>
              <p>
                <FormattedMessage
                  id="virology.imaging.instructions.body"
                  defaultMessage="Perform imaging or fluorescence analysis on virus culture samples. Store image data (Image ID), record CPE (Cytopathic Effect) observations, measure fluorescence intensity, and document any relevant observations."
                />
              </p>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="action-buttons-section">
        <Button
          kind="primary"
          size="md"
          renderIcon={Save}
          onClick={() => setModalOpen(true)}
          disabled={loading || selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="virology.imaging.logData"
            defaultMessage="Log Imaging Data"
          />
        </Button>
        <Button
          kind="tertiary"
          size="md"
          renderIcon={Checkmark}
          onClick={handleCompleteImaging}
          disabled={loading || selectedSampleIds.length === 0}
          style={{ marginLeft: "0.5rem" }}
        >
          <FormattedMessage
            id="virology.imaging.complete"
            defaultMessage="Complete Dark Room Imaging ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>
      </div>

      {/* Pending/In-Progress Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="virology.imaging.pendingSamples.title"
              defaultMessage="Pending / In Progress"
            />
            <Tag type="gray" size="sm" className="count-tag">
              {pendingCount}
            </Tag>
          </h5>
        </div>
        <SampleGrid
          samples={pendingSamples}
          onSelectionChange={setSelectedSampleIds}
          selectedSampleIds={selectedSampleIds}
          additionalColumns={additionalColumns}
        />
      </div>

      {/* Completed Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="virology.imaging.completedSamples.title"
              defaultMessage="Completed"
            />
            <Tag type="green" size="sm" className="count-tag">
              {completedCount}
            </Tag>
          </h5>
        </div>
        <SampleGrid
          samples={completedSamples}
          additionalColumns={additionalColumns}
          readOnly
        />
      </div>

      {/* Imaging Data Entry Modal */}
      <Modal
        open={modalOpen}
        onRequestClose={() => {
          setModalOpen(false);
          resetForm();
        }}
        modalHeading={intl.formatMessage({
          id: "virology.imaging.modal.title",
          defaultMessage: "Log Dark Room Imaging Data",
        })}
        primaryButtonText={intl.formatMessage({
          id: "virology.imaging.modal.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "virology.imaging.modal.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleSaveImagingData}
        size="md"
      >
        <div className="modal-form-content">
          <p className="modal-selected-count">
            <FormattedMessage
              id="virology.imaging.modal.selectedCount"
              defaultMessage="Applying to {count} selected sample(s)"
              values={{ count: selectedSampleIds.length }}
            />
          </p>

          <TextInput
            id="imageId"
            labelText={intl.formatMessage({
              id: "virology.imaging.field.imageId",
              defaultMessage: "Image ID",
            })}
            placeholder={intl.formatMessage({
              id: "virology.imaging.field.imageId.placeholder",
              defaultMessage: "e.g., IMG-2024-001",
            })}
            value={imageId}
            onChange={(e) => setImageId(e.target.value)}
          />

          <Select
            id="cpeObservation"
            labelText={intl.formatMessage({
              id: "virology.imaging.field.cpe",
              defaultMessage: "CPE (Cytopathic Effect) Observation",
            })}
            value={cpeObservation}
            onChange={(e) => setCpeObservation(e.target.value)}
          >
            {cpeOptions.map((option) => (
              <SelectItem
                key={option.id}
                value={option.id}
                text={option.text}
              />
            ))}
          </Select>

          <TextInput
            id="fluorescenceIntensity"
            labelText={intl.formatMessage({
              id: "virology.imaging.field.fluorescence",
              defaultMessage: "Fluorescence Intensity",
            })}
            placeholder={intl.formatMessage({
              id: "virology.imaging.field.fluorescence.placeholder",
              defaultMessage: "e.g., High, Medium, Low, or numerical value",
            })}
            value={fluorescenceIntensity}
            onChange={(e) => setFluorescenceIntensity(e.target.value)}
          />

          <TextArea
            id="imagingNotes"
            labelText={intl.formatMessage({
              id: "virology.imaging.field.notes",
              defaultMessage: "Imaging Notes",
            })}
            placeholder={intl.formatMessage({
              id: "virology.imaging.field.notes.placeholder",
              defaultMessage: "Additional observations or notes...",
            })}
            value={imagingNotes}
            onChange={(e) => setImagingNotes(e.target.value)}
            rows={4}
          />

          <div style={{ marginTop: "1rem" }}>
            <FileUploader
              labelTitle={intl.formatMessage({
                id: "virology.imaging.field.images",
                defaultMessage: "Upload Images",
              })}
              labelDescription={intl.formatMessage({
                id: "virology.imaging.field.images.description",
                defaultMessage:
                  "Upload multiple imaging files (PNG, JPG, TIFF, etc.)",
              })}
              buttonLabel={intl.formatMessage({
                id: "virology.imaging.field.images.button",
                defaultMessage: "Add files",
              })}
              filenameStatus="edit"
              accept={[
                ".jpg",
                ".jpeg",
                ".png",
                ".tiff",
                ".tif",
                ".gif",
                ".bmp",
              ]}
              multiple
              onChange={(e) => {
                const files = Array.from(e.target.files || []);
                setUploadedFiles((prev) => [...prev, ...files]);

                // Generate previews for new files
                files.forEach((file) => {
                  if (file.type.startsWith("image/")) {
                    const reader = new FileReader();
                    reader.onloadend = () => {
                      setImagePreviews((prev) => [...prev, reader.result]);
                    };
                    reader.readAsDataURL(file);
                  }
                });
              }}
              onDelete={(e) => {
                const fileIndex = parseInt(e.currentTarget.dataset.index, 10);
                setUploadedFiles((prev) =>
                  prev.filter((_, index) => index !== fileIndex),
                );
              }}
            />
            {uploadedFiles.length > 0 && (
              <div style={{ marginTop: "1rem" }}>
                <p
                  style={{
                    fontSize: "0.875rem",
                    color: "#525252",
                    marginBottom: "0.5rem",
                  }}
                >
                  <FormattedMessage
                    id="virology.imaging.field.images.count"
                    defaultMessage="{count} file(s) selected"
                    values={{ count: uploadedFiles.length }}
                  />
                </p>
                <div
                  style={{
                    display: "grid",
                    gridTemplateColumns:
                      "repeat(auto-fill, minmax(150px, 1fr))",
                    gap: "1rem",
                  }}
                >
                  {uploadedFiles.map((file, index) => (
                    <div
                      key={index}
                      style={{
                        border: "1px solid #e0e0e0",
                        borderRadius: "4px",
                        padding: "0.5rem",
                        position: "relative",
                      }}
                    >
                      {imagePreviews[index] && (
                        <img
                          src={imagePreviews[index]}
                          alt={file.name}
                          style={{
                            width: "100%",
                            height: "120px",
                            objectFit: "cover",
                            borderRadius: "4px",
                            marginBottom: "0.5rem",
                          }}
                        />
                      )}
                      <p
                        style={{
                          fontSize: "0.75rem",
                          margin: "0 0 0.25rem 0",
                          overflow: "hidden",
                          textOverflow: "ellipsis",
                          whiteSpace: "nowrap",
                        }}
                      >
                        {file.name}
                      </p>
                      <p
                        style={{
                          fontSize: "0.75rem",
                          color: "#6f6f6f",
                          margin: "0 0 0.5rem 0",
                        }}
                      >
                        {(file.size / 1024).toFixed(2)} KB
                      </p>
                      <Button
                        kind="danger"
                        size="sm"
                        onClick={() => {
                          // Revoke the preview URL
                          if (imagePreviews[index]) {
                            URL.revokeObjectURL(imagePreviews[index]);
                          }
                          setUploadedFiles((prev) =>
                            prev.filter((_, i) => i !== index),
                          );
                          setImagePreviews((prev) =>
                            prev.filter((_, i) => i !== index),
                          );
                        }}
                        style={{ width: "100%" }}
                      >
                        Remove
                      </Button>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      </Modal>

      {/* Image Viewer Modal */}
      <Modal
        open={imageViewerOpen}
        onRequestClose={() => {
          setImageViewerOpen(false);
          setViewingImages([]);
          setCurrentImageIndex(0);
        }}
        modalHeading={intl.formatMessage(
          {
            id: "virology.imaging.viewer.title",
            defaultMessage: "Viewing Image {current} of {total}",
          },
          { current: currentImageIndex + 1, total: viewingImages.length },
        )}
        passiveModal
        size="lg"
      >
        {viewingImages.length > 0 && viewingImages[currentImageIndex] && (
          <div style={{ textAlign: "center" }}>
            <img
              src={viewingImages[currentImageIndex].data}
              alt={
                viewingImages[currentImageIndex].name ||
                `Image ${currentImageIndex + 1}`
              }
              style={{
                maxWidth: "100%",
                maxHeight: "70vh",
                objectFit: "contain",
                marginBottom: "1rem",
              }}
            />
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                marginTop: "1rem",
                padding: "0 1rem",
              }}
            >
              <div style={{ textAlign: "left", flex: 1 }}>
                <p style={{ fontWeight: "bold", marginBottom: "0.25rem" }}>
                  {viewingImages[currentImageIndex].name}
                </p>
                <p style={{ fontSize: "0.875rem", color: "#6f6f6f" }}>
                  Size:{" "}
                  {(
                    (viewingImages[currentImageIndex].size || 0) / 1024
                  ).toFixed(2)}{" "}
                  KB
                  {viewingImages[currentImageIndex].type &&
                    ` | Type: ${viewingImages[currentImageIndex].type}`}
                </p>
              </div>
              {viewingImages.length > 1 && (
                <div style={{ display: "flex", gap: "0.5rem" }}>
                  <Button
                    kind="secondary"
                    size="sm"
                    onClick={() => {
                      setCurrentImageIndex((prev) =>
                        prev > 0 ? prev - 1 : viewingImages.length - 1,
                      );
                    }}
                  >
                    Previous
                  </Button>
                  <Button
                    kind="secondary"
                    size="sm"
                    onClick={() => {
                      setCurrentImageIndex((prev) =>
                        prev < viewingImages.length - 1 ? prev + 1 : 0,
                      );
                    }}
                  >
                    Next
                  </Button>
                </div>
              )}
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}

export default VirologyDarkRoomImagingPage;
