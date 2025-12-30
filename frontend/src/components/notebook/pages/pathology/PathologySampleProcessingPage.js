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
  NumberInput,
  ContentSwitcher,
  Switch,
  FileUploader,
  FileUploaderDropContainer,
  FileUploaderItem,
  Accordion,
  AccordionItem,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  Loading,
} from "@carbon/react";
import {
  Add,
  ArrowRight,
  Renew,
  View,
  ChevronRight,
  TreeViewAlt,
  List,
  Camera,
  TrashCan,
  DocumentDownload,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import PathologyHierarchyTable from "../../workflow/PathologyHierarchyTable";
import "../../workflow/NotebookWorkflow.css";

/**
 * PathologySampleProcessingPage - Page 3 of the pathology workflow.
 * Purpose: Create slides, blocks, aliquots while maintaining traceability.
 * Who uses it: Technicians / pathologists
 */
function PathologySampleProcessingPage({
  entryId,
  pageData,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // Sample list state
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // View mode: 'hierarchy' (tree) or 'flat' (grid)
  const [viewMode, setViewMode] = useState("hierarchy");

  // Processing Modal state
  const [processingModalOpen, setProcessingModalOpen] = useState(false);
  const [selectedSample, setSelectedSample] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  // Create Children Modal state
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [childCount, setChildCount] = useState(1);
  const [externalIdPrefix, setExternalIdPrefix] = useState("PATH-C");
  const [creating, setCreating] = useState(false);

  // View Children Modal state
  const [viewChildrenModalOpen, setViewChildrenModalOpen] = useState(false);
  const [selectedParentForView, setSelectedParentForView] = useState(null);
  const [parentChildren, setParentChildren] = useState([]);
  const [loadingChildren, setLoadingChildren] = useState(false);

  // Child type for creation modal
  const [childType, setChildType] = useState("cassette");

  // Grossing Modal state - for gross examination with image uploads
  const [grossingModalOpen, setGrossingModalOpen] = useState(false);
  const [grossingSample, setGrossingSample] = useState(null);
  const [grossingSubmitting, setGrossingSubmitting] = useState(false);
  const [grossingLoading, setGrossingLoading] = useState(false);
  const [grossingViewMode, setGrossingViewMode] = useState(false); // true = viewing existing, false = editing/creating
  const [grossImages, setGrossImages] = useState([]);
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
  });

  // Processing form state - includes cassette workflow fields
  const [processingData, setProcessingData] = useState({
    processingAction: "",
    // Cassette workflow (Specimen -> Cassette -> Block -> Slide)
    cassetteId: "",
    cassetteColor: "",
    cassetteLabel: "",
    numberOfCassettes: 1,
    tissueOrientation: "",
    // Histopathology processing
    grossExamDone: false,
    grossDescription: "",
    specimenDimensions: "",
    specimenWeight: "",
    sectionsSubmitted: 1,
    // Tissue processing
    processorId: "",
    processingProtocol: "",
    processingStartTime: "",
    processingEndTime: "",
    // Embedding
    blockId: "",
    embeddingDone: false,
    embeddingTechnician: "",
    orientationConfirmed: false,
    // Microtomy
    slideId: "",
    sectioningDone: false,
    microtomyThickness: 4,
    numberOfSlides: 1,
    microtomyTechnician: "",
    sectionQuality: "",
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
              externalId: sample.externalId,
              accessionNumber: sample.accessionNumber,
              sampleType: sample.sampleType || sample.typeOfSample?.description,
              specimenCategory: sample.specimenCategory || "histopathology",
              collectionDate: sample.collectionDate,
              status: sample.pageStatus || "PENDING",
              patientName: sample.patientName,
              // Hierarchy information from backend
              hasChildren: sample.hasChildren || false,
              childAliquotCount: sample.childAliquotCount || 0,
              isAliquot: sample.isAliquot || false,
              nestingLevel: sample.nestingLevel || 0,
              parentSampleItemId: sample.parentSampleItemId
                ? String(sample.parentSampleItemId)
                : null,
              parentExternalId: sample.parentExternalId,
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

  // Check if page has a real database ID
  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  const handleInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setProcessingData((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
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
      // Cassette workflow
      cassetteId: "",
      cassetteColor: "",
      cassetteLabel: "",
      numberOfCassettes: 1,
      tissueOrientation: "",
      // Histopathology processing
      grossExamDone: false,
      grossDescription: "",
      specimenDimensions: "",
      specimenWeight: "",
      sectionsSubmitted: 1,
      // Tissue processing
      processorId: "",
      processingProtocol: "",
      processingStartTime: "",
      processingEndTime: "",
      // Embedding
      blockId: "",
      embeddingDone: false,
      embeddingTechnician: "",
      orientationConfirmed: false,
      // Microtomy
      slideId: "",
      sectioningDone: false,
      microtomyThickness: 4,
      numberOfSlides: 1,
      microtomyTechnician: "",
      sectionQuality: "",
      // Common
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
          setSuccess(
            intl.formatMessage({
              id: "pathology.processing.success",
              defaultMessage: "Processing data submitted successfully.",
            }),
          );
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

  // Handle create children modal open
  const handleOpenCreateModal = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.processing.error.selectSample",
          defaultMessage: "Please select at least one parent sample.",
        }),
      );
      return;
    }
    setCreateModalOpen(true);
  }, [selectedSampleIds, intl]);

  // Handle create children
  const handleCreateChildren = useCallback(() => {
    if (selectedSampleIds.length === 0 || !hasRealPageId) return;

    setCreating(true);
    setError(null);

    postToOpenElisServerJsonResponse(
      `/rest/notebook/${entryId}/samples/create-children`,
      JSON.stringify({
        parentSampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        childCountPerParent: childCount,
        externalIdPrefix: externalIdPrefix,
        pageId: pageData?.id,
      }),
      (response) => {
        setCreating(false);
        setCreateModalOpen(false);

        if (response && response.success) {
          setSuccess(
            intl.formatMessage(
              {
                id: "pathology.processing.childrenCreated",
                defaultMessage: "Successfully created {count} child samples.",
              },
              { count: response.createdCount },
            ),
          );
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            response?.error ||
              intl.formatMessage({
                id: "pathology.processing.error.createChildrenFailed",
                defaultMessage: "Failed to create child samples.",
              }),
          );
        }
      },
    );
  }, [
    selectedSampleIds,
    hasRealPageId,
    entryId,
    childCount,
    externalIdPrefix,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // Handle view children for a parent
  const handleViewChildren = useCallback((parentSampleId) => {
    setSelectedParentForView(parentSampleId);
    setViewChildrenModalOpen(true);
    setLoadingChildren(true);

    getFromOpenElisServer(
      `/rest/notebook/samples/${parentSampleId}/children`,
      (response) => {
        setLoadingChildren(false);
        if (response && Array.isArray(response)) {
          setParentChildren(response);
        } else {
          setParentChildren([]);
        }
      },
    );
  }, []);

  // Handle status change
  const handleStatusChange = useCallback(
    (sampleId, newStatus) => {
      if (!hasRealPageId) {
        setError(
          intl.formatMessage({
            id: "pathology.processing.error.pageNotInitialized",
            defaultMessage:
              "Cannot update status: Page not properly initialized. Please refresh the page.",
          }),
        );
        return;
      }

      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
        JSON.stringify({
          sampleIds: [parseInt(sampleId, 10)],
          status: newStatus,
        }),
        (status) => {
          if (status === 200) {
            loadPageSamples();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            setError(
              intl.formatMessage({
                id: "pathology.processing.error.statusUpdateFailed",
                defaultMessage:
                  "Failed to update sample status. Please try again.",
              }),
            );
          }
        },
      );
    },
    [pageData?.id, hasRealPageId, loadPageSamples, onProgressUpdate, intl],
  );

  // Bulk mark as completed
  const handleBulkMarkCompleted = useCallback(() => {
    if (selectedSampleIds.length === 0) return;

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "pathology.processing.error.pageNotInitialized",
          defaultMessage:
            "Cannot update status: Page not properly initialized. Please refresh the page.",
        }),
      );
      return;
    }

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        status: "COMPLETED",
      }),
      (status) => {
        if (status === 200) {
          setSuccess(
            intl.formatMessage(
              {
                id: "pathology.processing.bulkCompleted",
                defaultMessage:
                  "Successfully marked {count} samples as completed.",
              },
              { count: selectedSampleIds.length },
            ),
          );
          loadPageSamples();
          setSelectedSampleIds([]);
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            intl.formatMessage({
              id: "pathology.processing.error.statusUpdateFailed",
              defaultMessage:
                "Failed to update sample status. Please try again.",
            }),
          );
        }
      },
    );
  }, [
    selectedSampleIds,
    pageData?.id,
    hasRealPageId,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // Calculate stats
  const processedCount = samples.filter((s) => s.status === "COMPLETED").length;
  const inProgressCount = samples.filter(
    (s) => s.status === "IN_PROGRESS",
  ).length;
  const pendingCount = samples.filter((s) => s.status === "PENDING").length;

  // ========================================
  // GROSSING MODAL HANDLERS
  // ========================================

  // Open grossing modal for a sample - loads existing data if available
  const openGrossingModal = useCallback(
    (sample) => {
      setGrossingSample(sample);
      setGrossImages([]);
      setGrossingViewMode(false);
      setGrossingLoading(true);

      // Initialize with empty data first
      const emptyData = {
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
        grossingStartTime: new Date()
          .toTimeString()
          .split(" ")[0]
          .substring(0, 5),
        grossingEndTime: "",
      };
      setGrossingData(emptyData);
      setGrossingModalOpen(true);

      // Try to load existing grossing data
      if (pageData?.id && sample?.id) {
        getFromOpenElisServer(
          `/rest/notebook/pathology/grossing/${sample.id}?pageId=${pageData.id}`,
          (response) => {
            setGrossingLoading(false);
            if (response && response.success && response.hasData) {
              // Populate with existing data
              setGrossingViewMode(true); // Start in view mode if data exists
              setGrossingData({
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
                marginsInked: response.marginsInked || false,
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
                representativeSections:
                  response.representativeSections || false,
                entirelySubmitted: response.entirelySubmitted || false,
                grossDescription: response.grossDescription || "",
                grossDictation: response.grossDictation || "",
                examinerName: response.examinerName || "",
                examinerInitials: response.examinerInitials || "",
                grossingDate: response.grossingDate || "",
                grossingStartTime: response.grossingStartTime || "",
                grossingEndTime: response.grossingEndTime || "",
              });

              // Load existing images if available
              if (response.grossImages && Array.isArray(response.grossImages)) {
                const loadedImages = response.grossImages.map((img, index) => ({
                  id: `existing-${index}`,
                  fileName: img.fileName || `Image ${index + 1}`,
                  fileSize: 0,
                  fileType: img.imageType || "image/jpeg",
                  specimenPart: img.specimenPart || "A",
                  viewDescription: img.viewDescription || "",
                  imageNumber: index + 1,
                  captureTime: img.captureTime || "",
                  notes: img.notes || "",
                  status: "complete",
                  preview: img.base64Data || img.imageUrl || null,
                  base64Data: img.base64Data || null,
                  imageUrl: img.imageUrl || null,
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
    },
    [pageData?.id],
  );

  // Handle grossing input changes
  const handleGrossingInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setGrossingData((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  // Handle image upload for grossing
  const handleGrossImageUpload = useCallback(
    (event, { addedFiles }) => {
      // Validate total image count (max 96)
      const currentCount = grossImages.length;
      const newCount = addedFiles.length;

      if (currentCount + newCount > 96) {
        setError(
          intl.formatMessage(
            {
              id: "pathology.grossing.error.maxImages",
              defaultMessage:
                "Maximum 96 images allowed per specimen. You can add {remaining} more.",
            },
            { remaining: 96 - currentCount },
          ),
        );
        return;
      }

      // Process each file
      const newImages = addedFiles.map((file, index) => {
        const imageNumber = currentCount + index + 1;
        return {
          id: `img-${Date.now()}-${index}`,
          file: file,
          fileName: file.name,
          fileSize: file.size,
          fileType: file.type,
          specimenPart: "A",
          viewDescription: "",
          imageNumber: imageNumber,
          captureTime: new Date().toISOString(),
          notes: "",
          status: "uploading",
          preview: null,
        };
      });

      // Generate previews for each image
      newImages.forEach((img) => {
        const reader = new FileReader();
        reader.onload = (e) => {
          setGrossImages((prev) =>
            prev.map((i) =>
              i.id === img.id
                ? {
                    ...i,
                    preview: e.target.result,
                    base64Data: e.target.result,
                    status: "complete",
                  }
                : i,
            ),
          );
        };
        reader.readAsDataURL(img.file);
      });

      setGrossImages((prev) => [...prev, ...newImages]);
    },
    [grossImages.length, intl],
  );

  // Remove a gross image
  const handleRemoveGrossImage = useCallback((imageId) => {
    setGrossImages((prev) => prev.filter((img) => img.id !== imageId));
  }, []);

  // Update image metadata (view description, specimen part, notes)
  const handleUpdateImageMetadata = useCallback((imageId, field, value) => {
    setGrossImages((prev) =>
      prev.map((img) =>
        img.id === imageId ? { ...img, [field]: value } : img,
      ),
    );
  }, []);

  // Submit grossing data
  const handleSubmitGrossing = useCallback(() => {
    if (grossingSubmitting) return;

    // Validate required fields
    if (!grossingData.grossDescription || !grossingData.examinerInitials) {
      setError(
        intl.formatMessage({
          id: "pathology.grossing.error.requiredFields",
          defaultMessage:
            "Please fill in Gross Description and Examiner Initials",
        }),
      );
      return;
    }

    setGrossingSubmitting(true);
    setError(null);

    // Prepare images for submission
    const imagesPayload = grossImages.map((img) => ({
      base64Data: img.base64Data,
      fileName: img.fileName,
      imageType: img.fileType,
      specimenPart: img.specimenPart,
      viewDescription: img.viewDescription,
      captureTime: img.captureTime,
      notes: img.notes,
    }));

    const payload = {
      sampleId: grossingSample?.id,
      pageId: pageData?.id,
      entryId: entryId,
      ...grossingData,
      grossImages: imagesPayload,
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/pathology/grossing/submit`,
      JSON.stringify(payload),
      (response) => {
        setGrossingSubmitting(false);
        if (response && response.success) {
          setGrossingModalOpen(false);
          setGrossingSample(null);
          setGrossImages([]);
          setSuccess(
            intl.formatMessage(
              {
                id: "pathology.grossing.success",
                defaultMessage:
                  "Gross examination saved successfully. {imageCount} images uploaded.",
              },
              { imageCount: response.imageCount || 0 },
            ),
          );
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            response?.error ||
              intl.formatMessage({
                id: "pathology.grossing.error.submitFailed",
                defaultMessage:
                  "Failed to save gross examination. Please try again.",
              }),
          );
        }
      },
    );
  }, [
    grossingSubmitting,
    grossingData,
    grossImages,
    grossingSample?.id,
    pageData?.id,
    entryId,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // Sectioning method options
  const sectioningMethodOptions = [
    { id: "bread_loaf", text: "Bread-loaf (serial cross-sections)" },
    { id: "perpendicular", text: "Perpendicular to margin" },
    { id: "tangential", text: "Tangential" },
    { id: "radial", text: "Radial sections" },
    { id: "en_face", text: "En face" },
    { id: "bisection", text: "Bisection" },
    { id: "quadrant", text: "Quadrant sectioning" },
  ];

  // Specimen part options (for multi-part specimens)
  const specimenPartOptions = [
    { id: "A", text: "Part A" },
    { id: "B", text: "Part B" },
    { id: "C", text: "Part C" },
    { id: "D", text: "Part D" },
    { id: "E", text: "Part E" },
    { id: "F", text: "Part F" },
  ];

  // View description presets for gross images
  const viewDescriptionPresets = [
    "Superior surface",
    "Inferior surface",
    "Anterior aspect",
    "Posterior aspect",
    "Lateral view",
    "Medial view",
    "Cross-section",
    "Margin (inked)",
    "Lesion close-up",
    "Ruler for scale",
    "Orientation markers",
    "After sectioning",
  ];

  // Processing action options - aligned with cassette workflow
  const processingActionOptions = [
    { id: "grossing", text: "Grossing (Examine & Document Specimen)" },
    { id: "cassetting", text: "Cassetting (Transfer to Cassette)" },
    {
      id: "tissue_processing",
      text: "Tissue Processing (Fixation/Dehydration)",
    },
    { id: "embedding", text: "Embedding (Create Paraffin Block)" },
    { id: "microtomy", text: "Microtomy (Cut Sections to Slides)" },
    { id: "aliquot_lbc", text: "Aliquot for LBC" },
    { id: "aliquot_cell_block", text: "Aliquot for Cell Block" },
    { id: "aliquot_molecular", text: "Aliquot for Molecular Testing" },
    { id: "aliquot_biobank", text: "Aliquot for Biobanking" },
  ];

  // Cassette color options (standard lab colors)
  const cassetteColorOptions = [
    { id: "white", text: "White (Standard)" },
    { id: "blue", text: "Blue" },
    { id: "pink", text: "Pink" },
    { id: "green", text: "Green" },
    { id: "yellow", text: "Yellow" },
    { id: "orange", text: "Orange" },
    { id: "lavender", text: "Lavender" },
  ];

  // Processing protocol options
  const processingProtocolOptions = [
    { id: "standard_overnight", text: "Standard Overnight" },
    { id: "rapid_biopsy", text: "Rapid (Biopsy)" },
    { id: "extended_fatty", text: "Extended (Fatty Tissue)" },
    { id: "decalcification", text: "Decalcification Required" },
  ];

  // Section quality options
  const sectionQualityOptions = [
    { id: "good", text: "Good" },
    { id: "acceptable", text: "Acceptable" },
    { id: "poor_recut", text: "Poor - Recut Needed" },
  ];

  // Child type options for creation modal
  const childTypeOptions = [
    {
      id: "cassette",
      text: "Tissue Cassette",
      prefix: "CAS",
      description: "Create cassette from specimen",
    },
    {
      id: "block",
      text: "Paraffin Block",
      prefix: "BLK",
      description: "Create block from cassette",
    },
    {
      id: "slide",
      text: "Tissue Slide",
      prefix: "SLD",
      description: "Create slide from block",
    },
  ];

  // Custom column for processing action
  const renderProcessAction = (sample) => {
    return (
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
    );
  };

  // Custom column for children actions
  const renderChildrenAction = (sample) => {
    return (
      <Button
        kind="ghost"
        size="sm"
        hasIconOnly
        iconDescription={intl.formatMessage({
          id: "pathology.page.processing.viewChildren",
          defaultMessage: "View Children",
        })}
        renderIcon={View}
        onClick={() => handleViewChildren(sample.id)}
      />
    );
  };

  return (
    <div className="pathology-processing-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="pathology.page.processing.title"
            defaultMessage="Sample Processing & Grossing/Aliquoting"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="pathology.page.processing.description"
            defaultMessage="Process specimens through histopathology workflow: Specimen → Cassette → Paraffin Block → Slide. Track each step with unique IDs for full traceability."
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

      {/* View Mode Switcher and Action Buttons */}
      <div
        className="page-actions-bar"
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          flexWrap: "wrap",
          gap: "1rem",
        }}
      >
        {/* View Mode Toggle */}
        <ContentSwitcher
          onChange={(e) => setViewMode(e.name)}
          selectedIndex={viewMode === "hierarchy" ? 0 : 1}
          size="sm"
        >
          <Switch name="hierarchy">
            <TreeViewAlt size={16} style={{ marginRight: "4px" }} />
            <FormattedMessage
              id="pathology.page.processing.hierarchyView"
              defaultMessage="Hierarchy"
            />
          </Switch>
          <Switch name="flat">
            <List size={16} style={{ marginRight: "4px" }} />
            <FormattedMessage
              id="pathology.page.processing.flatView"
              defaultMessage="Flat List"
            />
          </Switch>
        </ContentSwitcher>

        {/* Action Buttons */}
        <div style={{ display: "flex", gap: "0.5rem" }}>
          {viewMode === "flat" && (
            <>
              <Button
                kind="primary"
                size="sm"
                renderIcon={Add}
                onClick={handleOpenCreateModal}
                disabled={selectedSampleIds.length === 0}
              >
                <FormattedMessage
                  id="pathology.page.processing.createChildren"
                  defaultMessage="Create Children ({count} selected)"
                  values={{ count: selectedSampleIds.length }}
                />
              </Button>

              {selectedSampleIds.length > 0 && (
                <Button
                  kind="secondary"
                  size="sm"
                  renderIcon={ArrowRight}
                  onClick={handleBulkMarkCompleted}
                >
                  <FormattedMessage
                    id="pathology.page.processing.markCompleted"
                    defaultMessage="Mark Processing Complete ({count})"
                    values={{ count: selectedSampleIds.length }}
                  />
                </Button>
              )}
            </>
          )}

          <Button
            kind="tertiary"
            size="sm"
            renderIcon={Renew}
            onClick={loadPageSamples}
          >
            <FormattedMessage
              id="pathology.page.processing.refresh"
              defaultMessage="Refresh"
            />
          </Button>
        </div>
      </div>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          hideCloseButton={false}
          lowContrast
          onClose={() => setError(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {success && (
        <InlineNotification
          kind="success"
          title={success}
          hideCloseButton={false}
          lowContrast
          onClose={() => setSuccess(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Sample Display - Hierarchy or Flat Grid */}
      <div className="sample-grid-container">
        {viewMode === "hierarchy" ? (
          <PathologyHierarchyTable
            samples={samples}
            onCreateChild={(sample, childType) => {
              setSelectedSampleIds([String(sample.id)]);
              setChildType(childType);
              const typeOption = childTypeOptions.find(
                (opt) => opt.id === childType,
              );
              if (typeOption) {
                setExternalIdPrefix(`PATH-${typeOption.prefix}`);
              }
              setCreateModalOpen(true);
            }}
            onViewChildren={(sample) => handleViewChildren(sample.id)}
            onProcessSample={(sample) => openProcessingModal(sample)}
            onGrossingSample={(sample) => openGrossingModal(sample)}
            onRefresh={loadPageSamples}
            loading={loading}
          />
        ) : (
          <SampleGrid
            gridId="pathology-sample-processing"
            samples={samples}
            selectedIds={selectedSampleIds}
            onSelectionChange={setSelectedSampleIds}
            onStatusChange={handleStatusChange}
            statusFilter={statusFilter}
            onStatusFilterChange={setStatusFilter}
            showSelection={true}
            showHierarchy={true}
            loading={loading}
            additionalColumns={[
              {
                key: "process",
                header: intl.formatMessage({
                  id: "pathology.page.processing.processColumn",
                  defaultMessage: "Process",
                }),
                render: renderProcessAction,
              },
              {
                key: "children",
                header: intl.formatMessage({
                  id: "pathology.page.processing.childrenColumn",
                  defaultMessage: "Children",
                }),
                render: renderChildrenAction,
              },
            ]}
          />
        )}
      </div>

      {/* Empty state - only show if not in hierarchy view (hierarchy table has its own empty state) */}
      {!loading && samples.length === 0 && viewMode === "flat" && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="pathology.page.processing.empty"
              defaultMessage="No samples available for processing. Samples must pass QC on the previous page first."
            />
          </p>
        </div>
      )}

      {/* Create Children Modal - Cassette/Block/Slide Hierarchy */}
      <Modal
        open={createModalOpen}
        modalHeading={intl.formatMessage({
          id: "pathology.page.processing.createModal.title",
          defaultMessage: "Create Child Samples (Histopathology Workflow)",
        })}
        primaryButtonText={intl.formatMessage({
          id: "pathology.page.processing.createModal.create",
          defaultMessage: "Create Children",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.button.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setCreateModalOpen(false)}
        onRequestSubmit={handleCreateChildren}
        primaryButtonDisabled={creating}
      >
        {/* Workflow Hierarchy Reminder */}
        <div
          style={{
            backgroundColor: "#e0f7fa",
            padding: "0.75rem",
            marginBottom: "1rem",
            borderRadius: "4px",
          }}
        >
          <FormattedMessage
            id="pathology.processing.createModal.workflowHierarchy"
            defaultMessage="Workflow: Specimen → Cassette → Block → Slide"
          />
        </div>

        <div style={{ marginBottom: "1rem" }}>
          <p>
            <FormattedMessage
              id="pathology.page.processing.createModal.description"
              defaultMessage="Create child samples from {count} selected parent sample(s)."
              values={{ count: selectedSampleIds.length }}
            />
          </p>
        </div>

        {/* Child Type Selection */}
        <Select
          id="childType"
          labelText={intl.formatMessage({
            id: "pathology.page.processing.createModal.childType",
            defaultMessage: "Child Sample Type *",
          })}
          value={childType}
          onChange={(e) => {
            const newType = e.target.value;
            setChildType(newType);
            // Update prefix based on type
            const typeOption = childTypeOptions.find(
              (opt) => opt.id === newType,
            );
            if (typeOption) {
              setExternalIdPrefix(`PATH-${typeOption.prefix}`);
            }
          }}
          style={{ marginBottom: "1rem" }}
        >
          {childTypeOptions.map((opt) => (
            <SelectItem key={opt.id} value={opt.id} text={opt.text} />
          ))}
        </Select>

        <p
          style={{
            fontSize: "0.875rem",
            color: "#525252",
            marginBottom: "1rem",
          }}
        >
          {childTypeOptions.find((opt) => opt.id === childType)?.description}
        </p>

        <NumberInput
          id="childCount"
          label={intl.formatMessage({
            id: "pathology.page.processing.createModal.childCount",
            defaultMessage: "Children per Parent",
          })}
          value={childCount}
          onChange={(e, { value }) => setChildCount(value)}
          min={1}
          max={10}
          step={1}
          style={{ marginBottom: "1rem" }}
        />

        <TextInput
          id="externalIdPrefix"
          labelText={intl.formatMessage({
            id: "pathology.page.processing.createModal.prefix",
            defaultMessage: "External ID Prefix",
          })}
          value={externalIdPrefix}
          onChange={(e) => setExternalIdPrefix(e.target.value)}
          helperText={intl.formatMessage(
            {
              id: "pathology.page.processing.createModal.prefixHelp",
              defaultMessage:
                "Children will be named: {prefix}-{year}-{sequence}",
            },
            {
              prefix: externalIdPrefix || "PREFIX",
              year: new Date().getFullYear(),
              sequence: "001",
            },
          )}
        />

        <div style={{ marginTop: "1rem" }}>
          <p>
            <FormattedMessage
              id="pathology.page.processing.createModal.total"
              defaultMessage="Total children to create: {total}"
              values={{ total: selectedSampleIds.length * childCount }}
            />
          </p>
        </div>
      </Modal>

      {/* View Children Modal */}
      <Modal
        open={viewChildrenModalOpen}
        modalHeading={intl.formatMessage({
          id: "pathology.page.processing.viewModal.title",
          defaultMessage: "Child Samples",
        })}
        passiveModal
        onRequestClose={() => {
          setViewChildrenModalOpen(false);
          setParentChildren([]);
        }}
      >
        {loadingChildren ? (
          <p>
            <FormattedMessage
              id="pathology.page.processing.viewModal.loading"
              defaultMessage="Loading children..."
            />
          </p>
        ) : parentChildren.length === 0 ? (
          <p>
            <FormattedMessage
              id="pathology.page.processing.viewModal.noChildren"
              defaultMessage="No child samples found for this parent."
            />
          </p>
        ) : (
          <DataTable
            rows={parentChildren.map((child) => ({
              id: String(child.id),
              externalId: child.externalId || "-",
              sampleType: child.sampleType || "-",
              status: child.status || "PENDING",
            }))}
            headers={[
              {
                key: "externalId",
                header: intl.formatMessage({
                  id: "pathology.page.processing.viewModal.externalId",
                  defaultMessage: "External ID",
                }),
              },
              {
                key: "sampleType",
                header: intl.formatMessage({
                  id: "pathology.page.processing.viewModal.sampleType",
                  defaultMessage: "Sample Type",
                }),
              },
              {
                key: "status",
                header: intl.formatMessage({
                  id: "pathology.page.processing.viewModal.status",
                  defaultMessage: "Status",
                }),
              },
            ]}
          >
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
                  {rows.map((row) => (
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
                            >
                              {cell.value}
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
            )}
          </DataTable>
        )}
      </Modal>

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
          {/* Workflow Hierarchy Info */}
          <Column lg={16} md={8} sm={4}>
            <div
              style={{
                backgroundColor: "#e0f7fa",
                padding: "0.75rem",
                marginBottom: "1rem",
                borderRadius: "4px",
              }}
            >
              <FormattedMessage
                id="pathology.processing.workflowHierarchy"
                defaultMessage="Histopathology Workflow: Specimen → Cassette → Block → Slide"
              />
            </div>
          </Column>

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

          {/* Grossing Section - shown when action is grossing */}
          {(processingData.processingAction === "grossing" ||
            processingData.processingAction === "") && (
            <>
              <Column lg={16} md={8} sm={4}>
                <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
                  <FormattedMessage
                    id="pathology.processing.grossing"
                    defaultMessage="Gross Examination"
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
                <TextInput
                  id="specimenDimensions"
                  name="specimenDimensions"
                  labelText={intl.formatMessage({
                    id: "pathology.processing.specimenDimensions",
                    defaultMessage: "Specimen Dimensions (cm)",
                  })}
                  value={processingData.specimenDimensions}
                  onChange={handleInputChange}
                  placeholder="e.g., 2.5 x 1.5 x 0.5"
                />
              </Column>

              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="specimenWeight"
                  name="specimenWeight"
                  labelText={intl.formatMessage({
                    id: "pathology.processing.specimenWeight",
                    defaultMessage: "Specimen Weight (g)",
                  })}
                  value={processingData.specimenWeight}
                  onChange={handleInputChange}
                />
              </Column>

              <Column lg={4} md={4} sm={4}>
                <NumberInput
                  id="sectionsSubmitted"
                  name="sectionsSubmitted"
                  label={intl.formatMessage({
                    id: "pathology.processing.sectionsSubmitted",
                    defaultMessage: "Sections Submitted",
                  })}
                  value={processingData.sectionsSubmitted}
                  onChange={(e, { value }) =>
                    setProcessingData((prev) => ({
                      ...prev,
                      sectionsSubmitted: value,
                    }))
                  }
                  min={1}
                  max={50}
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
            </>
          )}

          {/* Cassetting Section */}
          {(processingData.processingAction === "cassetting" ||
            processingData.processingAction === "") && (
            <>
              <Column lg={16} md={8} sm={4}>
                <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
                  <FormattedMessage
                    id="pathology.processing.cassetting"
                    defaultMessage="Cassetting Information"
                  />
                </h5>
              </Column>

              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="cassetteId"
                  name="cassetteId"
                  labelText={intl.formatMessage({
                    id: "pathology.processing.cassetteId",
                    defaultMessage: "Cassette ID *",
                  })}
                  value={processingData.cassetteId}
                  onChange={handleInputChange}
                  placeholder="e.g., CAS-2024-001"
                />
              </Column>

              <Column lg={4} md={4} sm={4}>
                <Select
                  id="cassetteColor"
                  name="cassetteColor"
                  labelText={intl.formatMessage({
                    id: "pathology.processing.cassetteColor",
                    defaultMessage: "Cassette Color",
                  })}
                  value={processingData.cassetteColor}
                  onChange={handleInputChange}
                >
                  <SelectItem value="" text="" />
                  {cassetteColorOptions.map((opt) => (
                    <SelectItem key={opt.id} value={opt.id} text={opt.text} />
                  ))}
                </Select>
              </Column>

              <Column lg={4} md={4} sm={4}>
                <NumberInput
                  id="numberOfCassettes"
                  name="numberOfCassettes"
                  label={intl.formatMessage({
                    id: "pathology.processing.numberOfCassettes",
                    defaultMessage: "Number of Cassettes",
                  })}
                  value={processingData.numberOfCassettes}
                  onChange={(e, { value }) =>
                    setProcessingData((prev) => ({
                      ...prev,
                      numberOfCassettes: value,
                    }))
                  }
                  min={1}
                  max={20}
                />
              </Column>

              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="cassetteLabel"
                  name="cassetteLabel"
                  labelText={intl.formatMessage({
                    id: "pathology.processing.cassetteLabel",
                    defaultMessage: "Cassette Label",
                  })}
                  value={processingData.cassetteLabel}
                  onChange={handleInputChange}
                />
              </Column>

              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="tissueOrientation"
                  name="tissueOrientation"
                  labelText={intl.formatMessage({
                    id: "pathology.processing.tissueOrientation",
                    defaultMessage: "Tissue Orientation",
                  })}
                  value={processingData.tissueOrientation}
                  onChange={handleInputChange}
                  placeholder="e.g., Inked margins: blue=deep, black=superior"
                />
              </Column>
            </>
          )}

          {/* Tissue Processing Section */}
          {(processingData.processingAction === "tissue_processing" ||
            processingData.processingAction === "") && (
            <>
              <Column lg={16} md={8} sm={4}>
                <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
                  <FormattedMessage
                    id="pathology.processing.tissueProcessing"
                    defaultMessage="Tissue Processing"
                  />
                </h5>
              </Column>

              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="processorId"
                  name="processorId"
                  labelText={intl.formatMessage({
                    id: "pathology.processing.processorId",
                    defaultMessage: "Tissue Processor ID",
                  })}
                  value={processingData.processorId}
                  onChange={handleInputChange}
                />
              </Column>

              <Column lg={4} md={4} sm={4}>
                <Select
                  id="processingProtocol"
                  name="processingProtocol"
                  labelText={intl.formatMessage({
                    id: "pathology.processing.processingProtocol",
                    defaultMessage: "Processing Protocol",
                  })}
                  value={processingData.processingProtocol}
                  onChange={handleInputChange}
                >
                  <SelectItem value="" text="" />
                  {processingProtocolOptions.map((opt) => (
                    <SelectItem key={opt.id} value={opt.id} text={opt.text} />
                  ))}
                </Select>
              </Column>

              <Column lg={4} md={4} sm={4}>
                <DatePicker
                  datePickerType="single"
                  onChange={(dates) =>
                    handleDateChange(dates, "processingStartTime")
                  }
                >
                  <DatePickerInput
                    id="processingStartTime"
                    labelText={intl.formatMessage({
                      id: "pathology.processing.processingStartTime",
                      defaultMessage: "Processing Start",
                    })}
                    placeholder="mm/dd/yyyy"
                  />
                </DatePicker>
              </Column>

              <Column lg={4} md={4} sm={4}>
                <DatePicker
                  datePickerType="single"
                  onChange={(dates) =>
                    handleDateChange(dates, "processingEndTime")
                  }
                >
                  <DatePickerInput
                    id="processingEndTime"
                    labelText={intl.formatMessage({
                      id: "pathology.processing.processingEndTime",
                      defaultMessage: "Processing End",
                    })}
                    placeholder="mm/dd/yyyy"
                  />
                </DatePicker>
              </Column>
            </>
          )}

          {/* Embedding Section */}
          {(processingData.processingAction === "embedding" ||
            processingData.processingAction === "") && (
            <>
              <Column lg={16} md={8} sm={4}>
                <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
                  <FormattedMessage
                    id="pathology.processing.embedding"
                    defaultMessage="Embedding (Cassette → Paraffin Block)"
                  />
                </h5>
              </Column>

              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="blockId"
                  name="blockId"
                  labelText={intl.formatMessage({
                    id: "pathology.processing.blockId",
                    defaultMessage: "Block ID *",
                  })}
                  value={processingData.blockId}
                  onChange={handleInputChange}
                  placeholder="e.g., BLK-2024-001"
                />
              </Column>

              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="embeddingTechnician"
                  name="embeddingTechnician"
                  labelText={intl.formatMessage({
                    id: "pathology.processing.embeddingTechnician",
                    defaultMessage: "Embedding Technician",
                  })}
                  value={processingData.embeddingTechnician}
                  onChange={handleInputChange}
                />
              </Column>

              <Column lg={4} md={4} sm={4}>
                <Checkbox
                  id="embeddingDone"
                  name="embeddingDone"
                  labelText={intl.formatMessage({
                    id: "pathology.processing.embeddingDone",
                    defaultMessage: "Embedding Complete",
                  })}
                  checked={processingData.embeddingDone}
                  onChange={handleInputChange}
                />
              </Column>

              <Column lg={4} md={4} sm={4}>
                <Checkbox
                  id="orientationConfirmed"
                  name="orientationConfirmed"
                  labelText={intl.formatMessage({
                    id: "pathology.processing.orientationConfirmed",
                    defaultMessage: "Orientation Confirmed",
                  })}
                  checked={processingData.orientationConfirmed}
                  onChange={handleInputChange}
                />
              </Column>
            </>
          )}

          {/* Microtomy Section */}
          {(processingData.processingAction === "microtomy" ||
            processingData.processingAction === "") && (
            <>
              <Column lg={16} md={8} sm={4}>
                <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
                  <FormattedMessage
                    id="pathology.processing.microtomy"
                    defaultMessage="Microtomy (Block → Slide)"
                  />
                </h5>
              </Column>

              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="slideId"
                  name="slideId"
                  labelText={intl.formatMessage({
                    id: "pathology.processing.slideId",
                    defaultMessage: "Slide ID",
                  })}
                  value={processingData.slideId}
                  onChange={handleInputChange}
                  placeholder="e.g., SLD-2024-001"
                />
              </Column>

              <Column lg={4} md={4} sm={4}>
                <NumberInput
                  id="microtomyThickness"
                  name="microtomyThickness"
                  label={intl.formatMessage({
                    id: "pathology.processing.microtomyThickness",
                    defaultMessage: "Section Thickness (um)",
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

              <Column lg={4} md={4} sm={4}>
                <NumberInput
                  id="numberOfSlides"
                  name="numberOfSlides"
                  label={intl.formatMessage({
                    id: "pathology.processing.numberOfSlides",
                    defaultMessage: "Number of Slides",
                  })}
                  value={processingData.numberOfSlides}
                  onChange={(e, { value }) =>
                    setProcessingData((prev) => ({
                      ...prev,
                      numberOfSlides: value,
                    }))
                  }
                  min={1}
                  max={20}
                />
              </Column>

              <Column lg={4} md={4} sm={4}>
                <Select
                  id="sectionQuality"
                  name="sectionQuality"
                  labelText={intl.formatMessage({
                    id: "pathology.processing.sectionQuality",
                    defaultMessage: "Section Quality",
                  })}
                  value={processingData.sectionQuality}
                  onChange={handleInputChange}
                >
                  <SelectItem value="" text="" />
                  {sectionQualityOptions.map((opt) => (
                    <SelectItem key={opt.id} value={opt.id} text={opt.text} />
                  ))}
                </Select>
              </Column>

              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="microtomyTechnician"
                  name="microtomyTechnician"
                  labelText={intl.formatMessage({
                    id: "pathology.processing.microtomyTechnician",
                    defaultMessage: "Microtomy Technician",
                  })}
                  value={processingData.microtomyTechnician}
                  onChange={handleInputChange}
                />
              </Column>

              <Column lg={4} md={4} sm={4}>
                <Checkbox
                  id="sectioningDone"
                  name="sectioningDone"
                  labelText={intl.formatMessage({
                    id: "pathology.processing.sectioningDone",
                    defaultMessage: "Sectioning Complete",
                  })}
                  checked={processingData.sectioningDone}
                  onChange={handleInputChange}
                />
              </Column>
            </>
          )}

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

      {/* ========================================
          GROSSING MODAL - Gross Examination Entry
          ======================================== */}
      <Modal
        open={grossingModalOpen}
        modalHeading={intl.formatMessage(
          {
            id: grossingViewMode
              ? "pathology.grossing.modal.title.view"
              : "pathology.grossing.modal.title",
            defaultMessage: grossingViewMode
              ? "Gross Examination (View) - {sampleId}"
              : "Gross Examination - {sampleId}",
          },
          {
            sampleId:
              grossingSample?.externalId ||
              grossingSample?.accessionNumber ||
              "",
          },
        )}
        primaryButtonText={
          grossingViewMode
            ? intl.formatMessage({
                id: "pathology.grossing.modal.edit",
                defaultMessage: "Edit",
              })
            : intl.formatMessage({
                id: "pathology.grossing.modal.save",
                defaultMessage: "Save Gross Examination",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setGrossingModalOpen(false);
          setGrossingSample(null);
          setGrossImages([]);
          setGrossingViewMode(false);
        }}
        onRequestSubmit={
          grossingViewMode
            ? () => setGrossingViewMode(false)
            : handleSubmitGrossing
        }
        size="lg"
        hasScrollingContent
        preventCloseOnClickOutside
      >
        {/* Loading indicator */}
        {grossingLoading && (
          <div style={{ textAlign: "center", padding: "2rem" }}>
            <Loading
              description="Loading grossing data..."
              withOverlay={false}
            />
          </div>
        )}

        {/* View Mode Banner */}
        {grossingViewMode && !grossingLoading && (
          <InlineNotification
            kind="success"
            title={intl.formatMessage({
              id: "pathology.grossing.view.title",
              defaultMessage: "Gross Examination Complete",
            })}
            subtitle={intl.formatMessage(
              {
                id: "pathology.grossing.view.description",
                defaultMessage:
                  "Completed by {examiner} on {date}. Click 'Edit' to modify.",
              },
              {
                examiner: grossingData.examinerName || "Unknown",
                date: grossingData.grossingDate || "Unknown",
              },
            )}
            lowContrast
            hideCloseButton
            style={{ marginBottom: "1rem" }}
          />
        )}

        {/* Info Banner for edit mode */}
        {!grossingViewMode && !grossingLoading && (
          <InlineNotification
            kind="info"
            title={intl.formatMessage({
              id: "pathology.grossing.info.title",
              defaultMessage: "Gross Examination",
            })}
            subtitle={intl.formatMessage({
              id: "pathology.grossing.info.description",
              defaultMessage:
                "Document macroscopic findings, dimensions, and photograph the specimen (up to 96 images). Images will be named using standardized convention.",
            })}
            lowContrast
            hideCloseButton
            style={{ marginBottom: "1rem" }}
          />
        )}

        <Tabs>
          <TabList aria-label="Grossing tabs">
            <Tab>
              <FormattedMessage
                id="pathology.grossing.tab.findings"
                defaultMessage="Gross Findings"
              />
            </Tab>
            <Tab>
              <FormattedMessage
                id="pathology.grossing.tab.images"
                defaultMessage="Images ({count}/96)"
                values={{ count: grossImages.length }}
              />
            </Tab>
            <Tab>
              <FormattedMessage
                id="pathology.grossing.tab.sectioning"
                defaultMessage="Sectioning Plan"
              />
            </Tab>
          </TabList>

          <TabPanels>
            {/* Tab 1: Gross Findings */}
            <TabPanel>
              <Grid fullWidth style={{ padding: "1rem 0" }}>
                {/* Specimen Description */}
                <Column lg={16} md={8} sm={4}>
                  <h5 style={{ marginBottom: "1rem" }}>
                    <FormattedMessage
                      id="pathology.grossing.specimenDescription"
                      defaultMessage="Specimen Description"
                    />
                  </h5>
                </Column>

                <Column lg={16} md={8} sm={4}>
                  <TextInput
                    id="specimenReceived"
                    name="specimenReceived"
                    labelText={intl.formatMessage({
                      id: "pathology.grossing.specimenReceived",
                      defaultMessage: "Specimen Received (as labeled)",
                    })}
                    value={grossingData.specimenReceived}
                    onChange={handleGrossingInputChange}
                    placeholder="e.g., Right breast lumpectomy, oriented with short suture superior"
                  />
                </Column>

                {/* Dimensions */}
                <Column lg={16} md={8} sm={4}>
                  <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
                    <FormattedMessage
                      id="pathology.grossing.dimensions"
                      defaultMessage="Dimensions & Weight"
                    />
                  </h5>
                </Column>

                <Column lg={3} md={2} sm={2}>
                  <TextInput
                    id="dimensionLength"
                    name="dimensionLength"
                    labelText={intl.formatMessage({
                      id: "pathology.grossing.length",
                      defaultMessage: "Length",
                    })}
                    value={grossingData.dimensionLength}
                    onChange={handleGrossingInputChange}
                    placeholder="0.0"
                  />
                </Column>

                <Column lg={3} md={2} sm={2}>
                  <TextInput
                    id="dimensionWidth"
                    name="dimensionWidth"
                    labelText={intl.formatMessage({
                      id: "pathology.grossing.width",
                      defaultMessage: "Width",
                    })}
                    value={grossingData.dimensionWidth}
                    onChange={handleGrossingInputChange}
                    placeholder="0.0"
                  />
                </Column>

                <Column lg={3} md={2} sm={2}>
                  <TextInput
                    id="dimensionHeight"
                    name="dimensionHeight"
                    labelText={intl.formatMessage({
                      id: "pathology.grossing.height",
                      defaultMessage: "Height/Depth",
                    })}
                    value={grossingData.dimensionHeight}
                    onChange={handleGrossingInputChange}
                    placeholder="0.0"
                  />
                </Column>

                <Column lg={3} md={2} sm={2}>
                  <Select
                    id="dimensionUnit"
                    name="dimensionUnit"
                    labelText={intl.formatMessage({
                      id: "pathology.grossing.unit",
                      defaultMessage: "Unit",
                    })}
                    value={grossingData.dimensionUnit}
                    onChange={handleGrossingInputChange}
                  >
                    <SelectItem value="cm" text="cm" />
                    <SelectItem value="mm" text="mm" />
                  </Select>
                </Column>

                <Column lg={3} md={2} sm={2}>
                  <TextInput
                    id="specimenWeight"
                    name="specimenWeight"
                    labelText={intl.formatMessage({
                      id: "pathology.grossing.weight",
                      defaultMessage: "Weight",
                    })}
                    value={grossingData.specimenWeight}
                    onChange={handleGrossingInputChange}
                    placeholder="0.0"
                  />
                </Column>

                <Column lg={1} md={2} sm={2}>
                  <Select
                    id="weightUnit"
                    name="weightUnit"
                    labelText=" "
                    value={grossingData.weightUnit}
                    onChange={handleGrossingInputChange}
                  >
                    <SelectItem value="g" text="g" />
                    <SelectItem value="mg" text="mg" />
                  </Select>
                </Column>

                {/* Appearance */}
                <Column lg={16} md={8} sm={4}>
                  <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
                    <FormattedMessage
                      id="pathology.grossing.appearance"
                      defaultMessage="Appearance"
                    />
                  </h5>
                </Column>

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
                    placeholder="e.g., Tan-yellow, hemorrhagic"
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
                    placeholder="e.g., Firm, rubbery, friable"
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
                    placeholder="e.g., Homogeneous, heterogeneous"
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
                    placeholder="e.g., Well-circumscribed, infiltrative"
                  />
                </Column>

                <Column lg={4} md={4} sm={4}>
                  <Checkbox
                    id="marginsInked"
                    name="marginsInked"
                    labelText={intl.formatMessage({
                      id: "pathology.grossing.marginsInked",
                      defaultMessage: "Margins Inked",
                    })}
                    checked={grossingData.marginsInked}
                    onChange={handleGrossingInputChange}
                  />
                </Column>

                {grossingData.marginsInked && (
                  <Column lg={12} md={4} sm={4}>
                    <TextInput
                      id="inkColors"
                      name="inkColors"
                      labelText={intl.formatMessage({
                        id: "pathology.grossing.inkColors",
                        defaultMessage: "Ink Colors (specify margins)",
                      })}
                      value={grossingData.inkColors}
                      onChange={handleGrossingInputChange}
                      placeholder="e.g., Superior=blue, Inferior=black, Anterior=red"
                    />
                  </Column>
                )}

                {/* Orientation & Landmarks */}
                <Column lg={16} md={8} sm={4}>
                  <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
                    <FormattedMessage
                      id="pathology.grossing.orientation"
                      defaultMessage="Orientation & Landmarks"
                    />
                  </h5>
                </Column>

                <Column lg={8} md={4} sm={4}>
                  <TextInput
                    id="orientation"
                    name="orientation"
                    labelText={intl.formatMessage({
                      id: "pathology.grossing.orientationDesc",
                      defaultMessage: "Orientation",
                    })}
                    value={grossingData.orientation}
                    onChange={handleGrossingInputChange}
                    placeholder="e.g., Short suture superior, long suture lateral"
                  />
                </Column>

                <Column lg={8} md={4} sm={4}>
                  <TextInput
                    id="landmarks"
                    name="landmarks"
                    labelText={intl.formatMessage({
                      id: "pathology.grossing.landmarks",
                      defaultMessage: "Anatomical Landmarks",
                    })}
                    value={grossingData.landmarks}
                    onChange={handleGrossingInputChange}
                    placeholder="e.g., Nipple present, skin ellipse"
                  />
                </Column>

                {/* Abnormalities */}
                <Column lg={16} md={8} sm={4}>
                  <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
                    <FormattedMessage
                      id="pathology.grossing.abnormalities"
                      defaultMessage="Lesions & Abnormalities"
                    />
                  </h5>
                </Column>

                <Column lg={16} md={8} sm={4}>
                  <TextArea
                    id="abnormalities"
                    name="abnormalities"
                    labelText={intl.formatMessage({
                      id: "pathology.grossing.abnormalitiesDesc",
                      defaultMessage: "Abnormalities Identified",
                    })}
                    value={grossingData.abnormalities}
                    onChange={handleGrossingInputChange}
                    rows={2}
                    placeholder="e.g., 1.5 cm firm white mass at 2 o'clock, 3 cm from nipple"
                  />
                </Column>

                <Column lg={4} md={4} sm={4}>
                  <TextInput
                    id="lesionSize"
                    name="lesionSize"
                    labelText={intl.formatMessage({
                      id: "pathology.grossing.lesionSize",
                      defaultMessage: "Lesion Size",
                    })}
                    value={grossingData.lesionSize}
                    onChange={handleGrossingInputChange}
                    placeholder="e.g., 1.5 x 1.2 x 1.0 cm"
                  />
                </Column>

                <Column lg={4} md={4} sm={4}>
                  <TextInput
                    id="lesionLocation"
                    name="lesionLocation"
                    labelText={intl.formatMessage({
                      id: "pathology.grossing.lesionLocation",
                      defaultMessage: "Lesion Location",
                    })}
                    value={grossingData.lesionLocation}
                    onChange={handleGrossingInputChange}
                    placeholder="e.g., Upper outer quadrant"
                  />
                </Column>

                <Column lg={4} md={4} sm={4}>
                  <TextInput
                    id="distanceToMargins"
                    name="distanceToMargins"
                    labelText={intl.formatMessage({
                      id: "pathology.grossing.distanceToMargins",
                      defaultMessage: "Distance to Closest Margin",
                    })}
                    value={grossingData.distanceToMargins}
                    onChange={handleGrossingInputChange}
                    placeholder="e.g., 0.3 cm from posterior"
                  />
                </Column>

                {/* Free Text Gross Description */}
                <Column lg={16} md={8} sm={4}>
                  <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
                    <FormattedMessage
                      id="pathology.grossing.freeText"
                      defaultMessage="Gross Description (Free Text) *"
                    />
                  </h5>
                </Column>

                <Column lg={16} md={8} sm={4}>
                  <TextArea
                    id="grossDescription"
                    name="grossDescription"
                    labelText={intl.formatMessage({
                      id: "pathology.grossing.grossDescriptionLabel",
                      defaultMessage: "Complete Gross Description",
                    })}
                    value={grossingData.grossDescription}
                    onChange={handleGrossingInputChange}
                    rows={6}
                    placeholder="Enter complete gross description including specimen type, laterality, dimensions, appearance, lesion characteristics, and sections submitted..."
                  />
                </Column>

                {/* Staff & Timing */}
                <Column lg={16} md={8} sm={4}>
                  <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
                    <FormattedMessage
                      id="pathology.grossing.staff"
                      defaultMessage="Examiner Information"
                    />
                  </h5>
                </Column>

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
                  />
                </Column>

                <Column lg={2} md={2} sm={2}>
                  <TextInput
                    id="examinerInitials"
                    name="examinerInitials"
                    labelText={intl.formatMessage({
                      id: "pathology.grossing.examinerInitials",
                      defaultMessage: "Initials *",
                    })}
                    value={grossingData.examinerInitials}
                    onChange={handleGrossingInputChange}
                    maxLength={4}
                  />
                </Column>

                <Column lg={3} md={2} sm={2}>
                  <TextInput
                    id="grossingDate"
                    name="grossingDate"
                    labelText={intl.formatMessage({
                      id: "pathology.grossing.date",
                      defaultMessage: "Date",
                    })}
                    value={grossingData.grossingDate}
                    onChange={handleGrossingInputChange}
                    type="date"
                  />
                </Column>

                <Column lg={3} md={2} sm={2}>
                  <TextInput
                    id="grossingStartTime"
                    name="grossingStartTime"
                    labelText={intl.formatMessage({
                      id: "pathology.grossing.startTime",
                      defaultMessage: "Start Time",
                    })}
                    value={grossingData.grossingStartTime}
                    onChange={handleGrossingInputChange}
                    type="time"
                  />
                </Column>

                <Column lg={3} md={2} sm={2}>
                  <TextInput
                    id="grossingEndTime"
                    name="grossingEndTime"
                    labelText={intl.formatMessage({
                      id: "pathology.grossing.endTime",
                      defaultMessage: "End Time",
                    })}
                    value={grossingData.grossingEndTime}
                    onChange={handleGrossingInputChange}
                    type="time"
                  />
                </Column>
              </Grid>
            </TabPanel>

            {/* Tab 2: Images */}
            <TabPanel>
              <div style={{ padding: "1rem 0" }}>
                {/* Image Upload Area */}
                <div style={{ marginBottom: "1.5rem" }}>
                  <h5 style={{ marginBottom: "0.5rem" }}>
                    <FormattedMessage
                      id="pathology.grossing.uploadImages"
                      defaultMessage="Upload Gross Images"
                    />
                  </h5>
                  <p
                    style={{
                      color: "#6f6f6f",
                      fontSize: "0.875rem",
                      marginBottom: "1rem",
                    }}
                  >
                    <FormattedMessage
                      id="pathology.grossing.imageNamingInfo"
                      defaultMessage="Images will be automatically renamed using standardized convention: {AccessionNumber}_{Part}_{Number}_{View}.jpg"
                    />
                  </p>

                  <FileUploaderDropContainer
                    accept={[".jpg", ".jpeg", ".png", ".tiff", ".bmp"]}
                    labelText={intl.formatMessage({
                      id: "pathology.grossing.dropzone",
                      defaultMessage:
                        "Drag and drop images here or click to upload (max 96 images)",
                    })}
                    multiple
                    onAddFiles={handleGrossImageUpload}
                    disabled={grossImages.length >= 96}
                  />

                  <div
                    style={{
                      marginTop: "0.5rem",
                      display: "flex",
                      justifyContent: "space-between",
                    }}
                  >
                    <span style={{ color: "#6f6f6f", fontSize: "0.875rem" }}>
                      {grossImages.length} / 96 images
                    </span>
                    {grossImages.length > 0 && (
                      <Button
                        kind="danger--ghost"
                        size="sm"
                        onClick={() => setGrossImages([])}
                      >
                        <FormattedMessage
                          id="pathology.grossing.clearAll"
                          defaultMessage="Clear All Images"
                        />
                      </Button>
                    )}
                  </div>
                </div>

                {/* Image Grid */}
                {grossImages.length > 0 && (
                  <div
                    style={{
                      display: "grid",
                      gridTemplateColumns:
                        "repeat(auto-fill, minmax(280px, 1fr))",
                      gap: "1rem",
                      maxHeight: "500px",
                      overflowY: "auto",
                      padding: "0.5rem",
                    }}
                  >
                    {grossImages.map((img, index) => (
                      <Tile
                        key={img.id}
                        style={{
                          padding: "0.75rem",
                          display: "flex",
                          flexDirection: "column",
                          gap: "0.5rem",
                        }}
                      >
                        {/* Image Preview */}
                        <div
                          style={{
                            width: "100%",
                            height: "150px",
                            backgroundColor: "#f4f4f4",
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            overflow: "hidden",
                            borderRadius: "4px",
                          }}
                        >
                          {img.preview ? (
                            <img
                              src={img.preview}
                              alt={`Gross image ${index + 1}`}
                              style={{
                                maxWidth: "100%",
                                maxHeight: "100%",
                                objectFit: "contain",
                              }}
                            />
                          ) : (
                            <Camera size={32} style={{ color: "#8d8d8d" }} />
                          )}
                        </div>

                        {/* Image Info */}
                        <div style={{ fontSize: "0.75rem", color: "#6f6f6f" }}>
                          Image #{index + 1} •{" "}
                          {(img.fileSize / 1024).toFixed(1)} KB
                        </div>

                        {/* Specimen Part */}
                        <Select
                          id={`img-part-${img.id}`}
                          size="sm"
                          labelText="Specimen Part"
                          value={img.specimenPart}
                          onChange={(e) =>
                            handleUpdateImageMetadata(
                              img.id,
                              "specimenPart",
                              e.target.value,
                            )
                          }
                        >
                          {specimenPartOptions.map((opt) => (
                            <SelectItem
                              key={opt.id}
                              value={opt.id}
                              text={opt.text}
                            />
                          ))}
                        </Select>

                        {/* View Description */}
                        <Select
                          id={`img-view-${img.id}`}
                          size="sm"
                          labelText="View Description"
                          value={img.viewDescription}
                          onChange={(e) =>
                            handleUpdateImageMetadata(
                              img.id,
                              "viewDescription",
                              e.target.value,
                            )
                          }
                        >
                          <SelectItem value="" text="Select view..." />
                          {viewDescriptionPresets.map((view) => (
                            <SelectItem key={view} value={view} text={view} />
                          ))}
                        </Select>

                        {/* Notes */}
                        <TextInput
                          id={`img-notes-${img.id}`}
                          size="sm"
                          labelText="Notes"
                          value={img.notes}
                          onChange={(e) =>
                            handleUpdateImageMetadata(
                              img.id,
                              "notes",
                              e.target.value,
                            )
                          }
                          placeholder="Optional notes"
                        />

                        {/* Remove Button */}
                        <Button
                          kind="danger--ghost"
                          size="sm"
                          renderIcon={TrashCan}
                          onClick={() => handleRemoveGrossImage(img.id)}
                        >
                          Remove
                        </Button>
                      </Tile>
                    ))}
                  </div>
                )}

                {/* Empty State */}
                {grossImages.length === 0 && (
                  <div
                    style={{
                      textAlign: "center",
                      padding: "3rem",
                      color: "#6f6f6f",
                    }}
                  >
                    <Camera
                      size={48}
                      style={{ marginBottom: "1rem", opacity: 0.5 }}
                    />
                    <p>
                      <FormattedMessage
                        id="pathology.grossing.noImages"
                        defaultMessage="No images uploaded yet. Drag and drop images above or click to select files."
                      />
                    </p>
                  </div>
                )}
              </div>
            </TabPanel>

            {/* Tab 3: Sectioning Plan */}
            <TabPanel>
              <Grid fullWidth style={{ padding: "1rem 0" }}>
                <Column lg={16} md={8} sm={4}>
                  <h5 style={{ marginBottom: "1rem" }}>
                    <FormattedMessage
                      id="pathology.grossing.sectioningPlan"
                      defaultMessage="Sectioning Plan"
                    />
                  </h5>
                </Column>

                <Column lg={4} md={4} sm={4}>
                  <NumberInput
                    id="numberOfSections"
                    name="numberOfSections"
                    label={intl.formatMessage({
                      id: "pathology.grossing.numberOfSections",
                      defaultMessage: "Number of Sections",
                    })}
                    value={grossingData.numberOfSections}
                    onChange={(e, { value }) =>
                      setGrossingData((prev) => ({
                        ...prev,
                        numberOfSections: value,
                      }))
                    }
                    min={1}
                    max={100}
                  />
                </Column>

                <Column lg={6} md={4} sm={4}>
                  <Select
                    id="sectioningMethod"
                    name="sectioningMethod"
                    labelText={intl.formatMessage({
                      id: "pathology.grossing.sectioningMethod",
                      defaultMessage: "Sectioning Method",
                    })}
                    value={grossingData.sectioningMethod}
                    onChange={handleGrossingInputChange}
                  >
                    <SelectItem value="" text="Select method..." />
                    {sectioningMethodOptions.map((opt) => (
                      <SelectItem key={opt.id} value={opt.id} text={opt.text} />
                    ))}
                  </Select>
                </Column>

                <Column lg={3} md={2} sm={2}>
                  <Checkbox
                    id="representativeSections"
                    name="representativeSections"
                    labelText={intl.formatMessage({
                      id: "pathology.grossing.representativeSections",
                      defaultMessage: "Representative Sections",
                    })}
                    checked={grossingData.representativeSections}
                    onChange={handleGrossingInputChange}
                  />
                </Column>

                <Column lg={3} md={2} sm={2}>
                  <Checkbox
                    id="entirelySubmitted"
                    name="entirelySubmitted"
                    labelText={intl.formatMessage({
                      id: "pathology.grossing.entirelySubmitted",
                      defaultMessage: "Entirely Submitted",
                    })}
                    checked={grossingData.entirelySubmitted}
                    onChange={handleGrossingInputChange}
                  />
                </Column>

                <Column lg={16} md={8} sm={4}>
                  <TextArea
                    id="sectionsToSubmit"
                    name="sectionsToSubmit"
                    labelText={intl.formatMessage({
                      id: "pathology.grossing.sectionsToSubmit",
                      defaultMessage:
                        "Sections Submitted (Cassette Designations)",
                    })}
                    value={grossingData.sectionsToSubmit}
                    onChange={handleGrossingInputChange}
                    rows={4}
                    placeholder={`A1: Lesion with closest margin (posterior)
A2-A3: Lesion, serial sections
A4: Uninvolved breast tissue
A5: Skin ellipse`}
                  />
                </Column>
              </Grid>
            </TabPanel>
          </TabPanels>
        </Tabs>
      </Modal>
    </div>
  );
}

export default PathologySampleProcessingPage;
