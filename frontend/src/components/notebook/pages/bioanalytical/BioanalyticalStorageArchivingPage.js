import React, { useState, useCallback, useMemo, useContext } from "react";
import config from "../../../../config.json";
import {
  Grid,
  Column,
  Button,
  Loading,
  Tag,
  Modal,
  ComboBox,
  TextArea,
  FormLabel,
  Stack,
  Tooltip,
  FileUploader,
  Checkbox,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { usePermissions } from "../../../../hooks/usePermissions";
import { useBioanalyticalPermissions } from "../../../../hooks/useBioanalyticalPermissions";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
import AccessDeniedMessage from "../../../common/AccessDeniedMessage";
import SampleGrid from "../../workflow/SampleGrid";
import StorageHierarchySelector from "../../workflow/StorageHierarchySelector";
import "./BioanalyticalPages.css";

/**
 * BioanalyticalStorageArchivingPage - Stage 5 of bioanalytical workflow.
 *
 * Features:
 * - Sample storage location and condition tracking
 * - Retention period management per regulatory requirements
 * - Long-term archival and retrieval planning
 * - Disposal scheduling and compliance documentation
 * - Final sample/data disposition tracking
 *
 * @param {Object} props
 * @param {number} props.entryId - Notebook entry ID
 * @param {Object} props.pageData - Page configuration
 */
function BioanalyticalStorageArchivingPage({ entryId, pageData }) {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { hasAnyRole } = usePermissions();

  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, message }) => {
      setNotificationVisible(true);
      addNotification({ kind, title, message });
    },
    [addNotification, setNotificationVisible],
  );
  const {
    BIOANALYTICAL_ROLES,
    getPagePermissionLevel,
    canApproveData,
    canModify,
    canAccessStorageArchiving,
  } = useBioanalyticalPermissions();

  const allowedRoles = [
    BIOANALYTICAL_ROLES.LAB_SUPERVISOR,
    BIOANALYTICAL_ROLES.STUDY_DIRECTOR,
  ];

  const canAccessPage = canAccessStorageArchiving();

  const pagePermissionLevel = getPagePermissionLevel("Storage & Archiving");
  const canApproveStorage = canApproveData(pagePermissionLevel);
  const canModifyStorage = canModify(pagePermissionLevel);

  const [isLoading, setIsLoading] = useState(false);
  const [storageSamples, setStorageSamples] = useState([]);
  const [selectedSamples, setSelectedSamples] = useState(new Set());

  const [disposalModalOpen, setDisposalModalOpen] = useState(false);
  const [disposalReason, setDisposalReason] = useState("");
  const [disposalMethod, setDisposalMethod] = useState("");
  const [disposalNotes, setDisposalNotes] = useState("");
  const [supervisorApproval, setSupervisorApproval] = useState("");

  const [viewDetailsModalOpen, setViewDetailsModalOpen] = useState(false);
  const [selectedSampleDetail, setSelectedSampleDetail] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);

  const [biorepositoryModalOpen, setBiorepositoryModalOpen] = useState(false);
  const [retentionStorageModalOpen, setRetentionStorageModalOpen] =
    useState(false);
  const [storageLocation, setStorageLocation] = useState("");
  const [storageTemperature, setStorageTemperature] = useState("");
  const [retentionPeriod, setRetentionPeriod] = useState("");
  const [storageNotes, setStorageNotes] = useState("");

  const temperatureOptions = [
    { id: "-80C", text: "-80°C (Ultra-low freezer)" },
    { id: "-20C", text: "-20°C (Freezer)" },
    { id: "4C", text: "4°C (Refrigerated)" },
    { id: "RT", text: "Room Temperature (15-25°C)" },
    { id: "-196C", text: "-196°C (Liquid nitrogen)" },
    { id: "DRY_ICE", text: "Dry ice (-78°C)" },
  ];
  const [selectedStorageHierarchy, setSelectedStorageHierarchy] = useState({
    room: null,
    device: null,
    shelf: null,
    rack: null,
    box: null,
  });

  const [storageSelection, setStorageSelection] = useState({
    room: null,
    device: null,
    shelf: null,
    rack: null,
    box: null,
  });

  const [archivalModalOpen, setArchivalModalOpen] = useState(false);
  const [archiveRecords, setArchiveRecords] = useState([]);
  const [archiveType, setArchiveType] = useState("");
  const [archiveLocation, setArchiveLocation] = useState("");
  const [retentionYears, setRetentionYears] = useState("");
  const [archiveNotes, setArchiveNotes] = useState("");
  const [archiveFiles, setArchiveFiles] = useState([]);
  const [linkedSamples, setLinkedSamples] = useState([]);
  const [archiveMetadata, setArchiveMetadata] = useState({
    instrumentId: "",
    batchNumber: "",
    analystName: "",
    studyId: "",
    complianceNotes: "",
  });

  const disposalMethods = [
    {
      id: "autoclave_incineration",
      label: "Autoclaving + Incineration (Biological Samples)",
    },
    {
      id: "chemical_incineration",
      label: "Chemical Treatment + Incineration (Pharmaceutical)",
    },
    {
      id: "licensed_facility",
      label: "Licensed/Accredited Disposal Facility",
    },
    { id: "return_sponsor", label: "Return to Sponsor" },
    { id: "research_transfer", label: "Transfer to Research (with approval)" },
  ];

  const disposalReasons = [
    { id: "exhausted", label: "Sample Exhausted" },
    { id: "retention_expired", label: "Retention Period Expired" },
    { id: "failed_qc", label: "Failed QC (Unusable)" },
    { id: "safety_concerns", label: "Safety Concerns" },
    { id: "legal_hold_completed", label: "Legal Hold Period Completed" },
    { id: "study_terminated", label: "Study Terminated" },
  ];

  const archiveTypes = [
    {
      id: "raw_data",
      label: "Raw Data Files",
      retention: "Permanent",
      description:
        "Chromatograms, spectra, instrument data files (.mzml, .cdf, .raw)",
      fileTypes: [".mzml", ".cdf", ".raw", ".pdf", ".txt"],
    },
    {
      id: "analytical_reports",
      label: "Analytical Reports",
      retention: "5-10 years",
      description: "Final analytical result reports and certificates",
      fileTypes: [".pdf", ".docx", ".xlsx"],
    },
    {
      id: "calibration_records",
      label: "Calibration Records",
      retention: "5-10 years",
      description: "Calibration curves, instrument calibration certificates",
      fileTypes: [".pdf", ".csv", ".xlsx", ".txt"],
    },
    {
      id: "study_data",
      label: "Bioequivalence Study Data",
      retention: "2 years minimum post-completion",
      description:
        "Complete study datasets, pharmacokinetic data, statistical analyses",
      fileTypes: [".sas7bdat", ".csv", ".xlsx", ".pdf"],
    },
    {
      id: "qc_records",
      label: "QC Performance Records",
      retention: "5 years",
      description:
        "QC sample results, trending charts, system suitability data",
      fileTypes: [".pdf", ".csv", ".xlsx"],
    },
    {
      id: "method_validation",
      label: "Method Validation Documentation",
      retention: "Life of method + 5 years",
      description: "Method validation protocols, reports, and amendments",
      fileTypes: [".pdf", ".docx"],
    },
  ];

  const loadStorageSamples = useCallback(async () => {
    if (!entryId || String(entryId).startsWith("default-")) {
      setStorageSamples([]);
      return;
    }

    setIsLoading(true);
    try {
      const response = await fetch(
        `${config.serverBaseUrl}/rest/notebook/page/${pageData.id}/samples`,
        {
          method: "GET",
          credentials: "include",
          headers: {
            "X-CSRF-Token": localStorage.getItem("CSRF"),
            "Content-Type": "application/json",
          },
        },
      );

      if (response.ok) {
        const data = await response.json();
        const allSamples = Array.isArray(data) ? data : data.samples || [];

        let stage4CompletedSamples = allSamples.filter((sample) => {
          return (
            sample.data &&
            sample.data.executionStatus === "EXECUTED" &&
            sample.data.resultsApproved &&
            (sample.data.submissionStatus === "SUBMITTED" ||
              sample.data.submissionStatus ===
                "QA_APPROVED_READY_FOR_STORAGE" ||
              sample.data.exportStatus === "EXPORTED")
          );
        });

        if (stage4CompletedSamples.length === 0 && allSamples.length > 0) {
          console.log(
            "No QA-approved samples found, showing all samples in Stage 5",
          );
          stage4CompletedSamples = allSamples;
        }

        const transformedSamples = stage4CompletedSamples.map((sample) => ({
          id: sample.id,
          sampleId:
            sample.accessionNumber || sample.externalId || `S${sample.id}`,
          type: sample.sampleType || "Bioanalytical Sample",
          volume: sample.data?.sampleVolume || "5.0 mL",
          location: sample.data?.storageLocation || "Not Assigned",
          storageTemp: sample.data?.storageTemperature || "Pending Assignment",
          status: sample.data?.storageStatus || "READY_FOR_STORAGE",
          disposalStatus: sample.data?.disposalStatus || "-",
          disposalReason: sample.data?.disposalReason || "-",
          disposalMethod: sample.data?.disposalMethod || "-",
          disposalDate: sample.data?.disposalDate || "-",
          disposalApprovedBy: sample.data?.disposalApprovedBy || "-",
          retentionPeriod: sample.data?.retentionPeriod || "-",
          retentionExpiryDate: sample.data?.retentionExpiryDate || "-",
        }));

        setStorageSamples(transformedSamples);
      } else {
        setStorageSamples([]);
      }
    } catch (error) {
      setStorageSamples([]);
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.bioanalytical.storage.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "notebook.bioanalytical.storage.loadError",
          defaultMessage: "Failed to load samples. Please refresh the page.",
        }),
      });
    } finally {
      setIsLoading(false);
    }
  }, [entryId, intl, pageData.id]);

  /*
   * Backend API Endpoints Needed:
   *
   * GET /rest/notebook/page/{pageId}/archives
   * - Returns: List of archive records for the page
   *
   * POST /rest/notebook/page/{pageId}/archives
   * - Body: Archive record object with metadata, files, linked samples
   * - Returns: Created archive record with ID
   *
   * POST /rest/notebook/page/{pageId}/archives/files
   * - Body: FormData with files, archiveType, pageId
   * - Returns: List of uploaded file metadata
   */

  // Load archive records from backend
  const loadArchiveRecords = useCallback(async () => {
    if (!pageData?.id) {
      return;
    }

    try {
      const response = await fetch(
        `${config.serverBaseUrl}/rest/notebook/page/${pageData.id}/archives`,
        {
          method: "GET",
          credentials: "include",
          headers: {
            "X-CSRF-Token": localStorage.getItem("CSRF"),
            "Content-Type": "application/json",
          },
        },
      );

      if (response.ok) {
        const data = await response.json();
        setArchiveRecords(data || []);
      } else {
        setArchiveRecords([]);
      }
    } catch (error) {
      setArchiveRecords([]);
    }
  }, [pageData.id]);

  React.useEffect(() => {
    loadStorageSamples();
    loadArchiveRecords();
  }, [loadStorageSamples, loadArchiveRecords]);

  // Stage 5 Handler: Sample Disposal Management (via bulk apply endpoint)
  const handleSampleDisposal = useCallback(async () => {
    if (selectedSamples.size === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.bioanalytical.storage.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "notebook.bioanalytical.storage.selectSamplesFirst",
          defaultMessage: "Please select samples for disposal",
        }),
      });
      return;
    }

    if (!disposalReason) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.bioanalytical.storage.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "notebook.bioanalytical.storage.selectDisposalReason",
          defaultMessage: "Please select disposal reason",
        }),
      });
      return;
    }

    if (!disposalMethod) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.bioanalytical.storage.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "notebook.bioanalytical.storage.selectDisposalMethod",
          defaultMessage: "Please select disposal method",
        }),
      });
      return;
    }

    if (!supervisorApproval) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.bioanalytical.storage.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "notebook.bioanalytical.storage.supervisorApprovalRequired",
          defaultMessage: "Supervisor approval is required for sample disposal",
        }),
      });
      return;
    }

    setIsLoading(true);
    try {
      // Use bulk apply endpoint with disposal fields in JSONB data
      const bulkRequest = {
        sampleIds: Array.from(selectedSamples).map((id) => parseInt(id, 10)),
        data: {
          disposalStatus: "SCHEDULED",
          disposalReason: disposalReason,
          disposalMethod: disposalMethod,
          disposalDate: new Date().toISOString().split("T")[0],
          disposalApprovedBy: supervisorApproval,
          disposalNotes: disposalNotes,
          storageStatus: "DISPOSED",
        },
      };

      const response = await fetch(
        `${config.serverBaseUrl}/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
        {
          method: "POST",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
          body: JSON.stringify(bulkRequest),
        },
      );

      if (response.ok) {
        const result = await response.json();
        notify({
          kind: NotificationKinds.success,
          title: intl.formatMessage({
            id: "notebook.bioanalytical.storage.success",
            defaultMessage: "Success",
          }),
          message: intl.formatMessage(
            {
              id: "notebook.bioanalytical.storage.disposalSuccess",
              defaultMessage:
                "{count} samples scheduled for disposal. Method: {method}. Supervisor: {supervisor}",
            },
            {
              count: result.updatedCount || selectedSamples.size,
              method: disposalMethods.find((m) => m.id === disposalMethod)
                ?.label,
              supervisor: supervisorApproval,
            },
          ),
        });

        setDisposalModalOpen(false);
        setSelectedSamples(new Set());
        setDisposalReason("");
        setDisposalMethod("");
        setDisposalNotes("");
        setSupervisorApproval("");
        loadStorageSamples();
      } else {
        throw new Error("Failed to schedule disposal");
      }
    } catch (error) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.bioanalytical.storage.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "notebook.bioanalytical.storage.disposalError",
          defaultMessage: "Failed to schedule sample disposal",
        }),
      });
    } finally {
      setIsLoading(false);
    }
  }, [
    selectedSamples,
    disposalReason,
    disposalMethod,
    supervisorApproval,
    disposalNotes,
    intl,
    loadStorageSamples,
    disposalMethods,
    pageData?.id,
  ]);

  const handleSampleDetailsClick = useCallback(async (sample) => {
    setViewDetailsModalOpen(true);
    setSelectedSampleDetail(sample);
    setDetailLoading(true);

    try {
      // Fetch the full sample record from the backend
      const response = await fetch(
        `${config.serverBaseUrl}/rest/sample/${sample.id}`,
        {
          method: "GET",
          credentials: "include",
          headers: {
            "X-CSRF-Token": localStorage.getItem("CSRF"),
            "Content-Type": "application/json",
          },
        },
      );

      if (response.ok) {
        const fullSample = await response.json();
        setSelectedSampleDetail(fullSample);
      }
    } catch (error) {
      // Keep the basic sample data if fetch fails
    } finally {
      setDetailLoading(false);
    }
  }, []);

  // Helper function to clear biorepository modal state
  const clearBiorepositoryModalState = useCallback(() => {
    setBiorepositoryModalOpen(false);
    setStorageLocation("");
    setStorageTemperature("");
    setStorageNotes("");
    setSelectedStorageHierarchy({
      room: null,
      device: null,
      shelf: null,
      rack: null,
      box: null,
    });
  }, []);

  // Handle biorepository transfer
  const handleBiorepositoryTransfer = useCallback(async () => {
    if (selectedSamples.size === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.bioanalytical.storage.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "notebook.bioanalytical.storage.selectSamplesFirst",
          defaultMessage: "Please select samples for biorepository transfer",
        }),
      });
      return;
    }

    setIsLoading(true);
    try {
      // Step 1: Create transfer request using the biorepository transfer API
      const transferRequest = {
        sourceLab: "Bioanalytical Laboratory",
        sampleItemIds: Array.from(selectedSamples).map((id) =>
          parseInt(id, 10),
        ),
        requestNotes: `Bioanalytical transfer to biorepository. Storage Location: ${storageLocation || "Biorepository"}. Temperature: ${storageTemperature || "-80°C"}. Notes: ${storageNotes || "Standard bioanalytical completion transfer"}`,
      };

      const transferResponse = await fetch(
        `${config.serverBaseUrl}/rest/biorepository/transfer`,
        {
          method: "POST",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
          body: JSON.stringify(transferRequest),
        },
      );

      if (!transferResponse.ok) {
        let errorMessage = "Failed to create transfer request";
        try {
          const errorData = await transferResponse.json();
          errorMessage = errorData.error || errorData.message || errorMessage;
        } catch (parseError) {
          // If JSON parsing fails, try to get text response
          try {
            const errorText = await transferResponse.text();
            errorMessage = errorText || errorMessage;
          } catch (textError) {
            // Keep default error message
          }
        }
        throw new Error(errorMessage);
      }

      const transferResult = await transferResponse.json();

      const bulkRequest = {
        sampleIds: Array.from(selectedSamples).map((id) => parseInt(id, 10)),
        data: {
          storageStatus: "BIOREPOSITORY_TRANSFER",
          storageLocation: storageLocation || "Biorepository",
          storageTemperature: storageTemperature || "-80°C",
          storageNotes: `${storageNotes || ""}. Transfer Request ID: ${transferResult.id}`,
          transferRequestId: transferResult.id,
        },
      };

      const statusResponse = await fetch(
        `${config.serverBaseUrl}/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
        {
          method: "POST",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
          body: JSON.stringify(bulkRequest),
        },
      );

      if (statusResponse.ok) {
        notify({
          kind: NotificationKinds.success,
          title: intl.formatMessage({
            id: "notebook.bioanalytical.storage.success",
            defaultMessage: "Success",
          }),
          message: intl.formatMessage(
            {
              id: "notebook.bioanalytical.storage.biorepositorySuccess",
              defaultMessage:
                "{count} samples transferred to biorepository at {location} ({temperature}). Transfer #{transferId}",
            },
            {
              count: selectedSamples.size,
              location: storageLocation || "Biorepository",
              temperature: storageTemperature || "-80°C",
              transferId: transferResult.id,
            },
          ),
        });
        clearBiorepositoryModalState();
        setSelectedSamples(new Set());
        loadStorageSamples();
      } else {
        throw new Error(
          "Transfer request created but failed to update sample status",
        );
      }
    } catch (error) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.bioanalytical.storage.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "notebook.bioanalytical.storage.biorepositoryError",
          defaultMessage:
            "Failed to transfer samples to biorepository: {error}",
          values: { error: error.message },
        }),
      });
    } finally {
      setIsLoading(false);
    }
  }, [
    selectedSamples,
    storageLocation,
    storageTemperature,
    storageNotes,
    intl,
    loadStorageSamples,
    pageData?.id,
    clearBiorepositoryModalState,
  ]);

  // Handle retention storage
  const handleRetentionStorage = useCallback(async () => {
    if (selectedSamples.size === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.bioanalytical.storage.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "notebook.bioanalytical.storage.selectSamplesFirst",
          defaultMessage: "Please select samples for retention storage",
        }),
      });
      return;
    }

    if (!retentionPeriod) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.bioanalytical.storage.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "notebook.bioanalytical.storage.selectRetentionPeriod",
          defaultMessage: "Please select retention period",
        }),
      });
      return;
    }

    setIsLoading(true);
    try {
      const bulkRequest = {
        sampleIds: Array.from(selectedSamples).map((id) => parseInt(id, 10)),
        data: {
          storageStatus: "RETENTION_STORAGE",
          storageLocation: storageLocation || "Retention Freezer",
          storageTemperature: storageTemperature || "-80°C",
          retentionPeriod: retentionPeriod,
          retentionExpiryDate: calculateRetentionExpiryDate(retentionPeriod),
          storageNotes: storageNotes,
        },
      };

      const response = await fetch(
        `${config.serverBaseUrl}/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
        {
          method: "POST",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
          body: JSON.stringify(bulkRequest),
        },
      );

      if (response.ok) {
        notify({
          kind: NotificationKinds.success,
          title: intl.formatMessage({
            id: "notebook.bioanalytical.storage.success",
            defaultMessage: "Success",
          }),
          message: intl.formatMessage({
            id: "notebook.bioanalytical.storage.retentionSuccess",
            defaultMessage:
              "{count} samples placed in retention storage for {period}",
            values: { count: selectedSamples.size, period: retentionPeriod },
          }),
        });
        setRetentionStorageModalOpen(false);
        setSelectedSamples(new Set());
        setStorageLocation("");
        setStorageTemperature("");
        setRetentionPeriod("");
        setStorageNotes("");
        loadStorageSamples();
      } else {
        throw new Error("Failed to place samples in retention storage");
      }
    } catch (error) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.bioanalytical.storage.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "notebook.bioanalytical.storage.retentionError",
          defaultMessage: "Failed to place samples in retention storage",
        }),
      });
    } finally {
      setIsLoading(false);
    }
  }, [
    selectedSamples,
    storageLocation,
    storageTemperature,
    retentionPeriod,
    storageNotes,
    intl,
    loadStorageSamples,
    pageData?.id,
  ]);

  // Helper function to build storage path from selection object
  const buildStoragePathFromSelection = (selection) => {
    const parts = [];
    if (selection.room) parts.push(selection.room.label);
    if (selection.device) parts.push(selection.device.label);
    if (selection.shelf) parts.push(selection.shelf.label);
    if (selection.rack) parts.push(selection.rack.label);
    if (selection.box) parts.push(selection.box.label);
    return parts.join(" > ");
  };

  const calculateRetentionExpiryDate = (period) => {
    const now = new Date();
    const months = parseInt(period.split(" ")[0]) || 24;
    now.setMonth(now.getMonth() + months);
    return now.toISOString().split("T")[0];
  };

  const handleAddArchiveRecord = useCallback(async () => {
    if (!archiveType) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.bioanalytical.storage.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "notebook.bioanalytical.storage.selectArchiveType",
          defaultMessage: "Please select archive type",
        }),
      });
      return;
    }

    if (!archiveLocation) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.bioanalytical.storage.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "notebook.bioanalytical.storage.enterArchiveLocation",
          defaultMessage: "Please enter archive location/system",
        }),
      });
      return;
    }

    if (!retentionYears) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.bioanalytical.storage.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "notebook.bioanalytical.storage.selectRetentionYears",
          defaultMessage: "Please select retention period",
        }),
      });
      return;
    }

    const selectedType = archiveTypes.find((t) => t.id === archiveType);
    const newRecord = {
      id: `archive-${Date.now()}`,
      archiveType: archiveType,
      archiveTypeLabel: selectedType?.label,
      archiveDescription: selectedType?.description,
      archiveLocation: archiveLocation,
      retentionYears: retentionYears,
      archivalDate: new Date().toISOString().split("T")[0],
      purgeDate: calculateArchivePurgeDate(retentionYears),
      notes: archiveNotes,
      // Enhanced data
      files: archiveFiles.map((file) => ({
        name: file.name,
        size: file.size,
        type: file.type,
        uploadDate: new Date().toISOString(),
      })),
      linkedSamples: linkedSamples.map((sampleId) => {
        const sample = storageSamples.find((s) => s.id === sampleId);
        return {
          sampleId: sampleId,
          sampleLabel: sample?.sampleId || `S${sampleId}`,
          sampleType: sample?.type || "Unknown",
        };
      }),
      metadata: {
        ...archiveMetadata,
        archivedBy: "CURRENT_USER", // This would come from user session
        totalFiles: archiveFiles.length,
        totalSize: archiveFiles.reduce((sum, file) => sum + file.size, 0),
      },
    };

    try {
      // Archive selected samples with storage information
      const archiveRequest = {
        sampleIds: linkedSamples.map((id) => String(id)),
        storageLocation: archiveLocation,
        archiveReason: archiveType,
        retentionPeriod: retentionYears,
        archiveDate: new Date().toISOString().split("T")[0],
        archivedBy: "CURRENT_USER",
        notes: archiveNotes,
      };

      const archiveResponse = await fetch(
        `${config.serverBaseUrl}/rest/notebook/bulk/page/${pageData.id}/samples/archive`,
        {
          method: "POST",
          credentials: "include",
          headers: {
            "X-CSRF-Token": localStorage.getItem("CSRF"),
            "Content-Type": "application/json",
          },
          body: JSON.stringify(archiveRequest),
        },
      );

      if (archiveResponse.ok) {
        notify({
          kind: NotificationKinds.success,
          title: intl.formatMessage({
            id: "notebook.bioanalytical.storage.success",
            defaultMessage: "Success",
          }),
          message: intl.formatMessage({
            id: "notebook.bioanalytical.storage.archiveRecordAdded",
            defaultMessage: "Archive record added: {type}",
            values: { type: newRecord.archiveTypeLabel },
          }),
        });
      } else {
        const errorText = await archiveResponse.text();
        throw new Error(`Archive failed: ${errorText}`);
      }
    } catch (error) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.bioanalytical.storage.error",
          defaultMessage: "Error",
        }),
        message: intl.formatMessage({
          id: "notebook.bioanalytical.storage.archiveRecordError",
          defaultMessage: "Failed to archive samples. Please try again.",
        }),
      });
      return; // Don't reset form on error
    }

    // Reset form
    setArchiveType("");
    setArchiveLocation("");
    setRetentionYears("");
    setArchiveNotes("");
    setArchiveFiles([]);
    setLinkedSamples([]);
    setArchiveMetadata({
      instrumentId: "",
      batchNumber: "",
      analystName: "",
      studyId: "",
      complianceNotes: "",
    });
    setArchivalModalOpen(false);
  }, [
    archiveType,
    archiveLocation,
    retentionYears,
    archiveNotes,
    archiveTypes,
    archiveFiles,
    linkedSamples,
    archiveMetadata,
    storageSamples,
    pageData?.id,
    loadArchiveRecords,
    intl,
  ]);

  const calculateArchivePurgeDate = (retentionPeriod) => {
    const now = new Date();
    const years = parseInt(retentionPeriod) || 5;
    now.setFullYear(now.getFullYear() + years);
    return now.toISOString().split("T")[0];
  };

  const getStatusTag = (status) => {
    let type = "blue";
    let label = status;

    switch (status) {
      case "READY_FOR_STORAGE":
        type = "blue";
        label = intl.formatMessage({
          id: "notebook.bioanalytical.storage.status.ready",
          defaultMessage: "Ready for Storage",
        });
        break;
      case "BIOREPOSITORY_TRANSFER":
        type = "green";
        label = intl.formatMessage({
          id: "notebook.bioanalytical.storage.status.biorepository",
          defaultMessage: "Biorepository Transfer",
        });
        break;
      case "RETENTION_STORAGE":
        type = "purple";
        label = intl.formatMessage({
          id: "notebook.bioanalytical.storage.status.retention",
          defaultMessage: "Retention Storage",
        });
        break;
      case "DISPOSED":
        type = "red";
        label = intl.formatMessage({
          id: "notebook.bioanalytical.storage.status.disposed",
          defaultMessage: "Disposed",
        });
        break;
      case "ARCHIVED":
        type = "cyan";
        label = intl.formatMessage({
          id: "notebook.bioanalytical.storage.status.archived",
          defaultMessage: "Archived",
        });
        break;
      default:
        type = "gray";
    }

    return <Tag type={type}>{label}</Tag>;
  };

  const getDisposalStatusTag = (status) => {
    let type = "gray";
    let label = status;

    switch (status) {
      case "SCHEDULED":
        type = "green";
        label = intl.formatMessage({
          id: "notebook.bioanalytical.storage.disposalStatus.scheduled",
          defaultMessage: "Scheduled",
        });
        break;
      case "PENDING":
        type = "blue";
        label = intl.formatMessage({
          id: "notebook.bioanalytical.storage.disposalStatus.pending",
          defaultMessage: "Pending",
        });
        break;
      case "COMPLETED":
        type = "teal";
        label = intl.formatMessage({
          id: "notebook.bioanalytical.storage.disposalStatus.completed",
          defaultMessage: "Completed",
        });
        break;
      case "CANCELLED":
        type = "red";
        label = intl.formatMessage({
          id: "notebook.bioanalytical.storage.disposalStatus.cancelled",
          defaultMessage: "Cancelled",
        });
        break;
      case "-":
        return "-";
      default:
        return <Tag type={type}>{status}</Tag>;
    }

    return <Tag type={type}>{label}</Tag>;
  };

  const getStorageLocationTag = (location) => {
    let type = "green";
    let fullLocation = location || "Not Assigned";

    // Create a shortened version for display
    let displayText = fullLocation;

    // If location contains hierarchy separators, show only the last 2 parts
    if (fullLocation.includes(" > ")) {
      const parts = fullLocation.split(" > ");
      if (parts.length > 2) {
        // Show last 2 parts with ellipsis
        displayText = `...${parts.slice(-2).join(" > ")}`;
      }
    }

    // If still too long, truncate to 25 characters
    if (displayText.length > 25) {
      displayText = displayText.substring(0, 22) + "...";
    }

    // Different colors based on location type
    if (
      fullLocation === "Not Assigned" ||
      fullLocation === "Pending Assignment"
    ) {
      type = "gray";
    } else if (
      fullLocation.includes("Freezer") ||
      fullLocation.includes("-80°C") ||
      fullLocation.includes("-20°C")
    ) {
      type = "blue";
    } else if (fullLocation.includes("Biorepository")) {
      type = "purple";
    } else if (
      fullLocation.includes("Room") ||
      fullLocation.includes("Shelf")
    ) {
      type = "green";
    } else {
      type = "cyan";
    }

    // If location was truncated, wrap in tooltip
    if (displayText !== fullLocation) {
      return (
        <Tooltip label={fullLocation} align="bottom">
          <Tag type={type}>{displayText}</Tag>
        </Tooltip>
      );
    }

    return <Tag type={type}>{displayText}</Tag>;
  };

  // Helper function to get actionable samples (not transferred or disposed)
  const getActionableSamples = useCallback(() => {
    return storageSamples.filter(
      (sample) =>
        sample.status !== "DISPOSED" &&
        sample.status !== "BIOREPOSITORY_TRANSFER",
    );
  }, [storageSamples]);

  // Helper function to check if selected samples can be used for biorepository transfer
  const hasActionableSelectedSamples = useMemo(() => {
    if (selectedSamples.size === 0) return false;
    const actionableSamples = getActionableSamples();
    const selectedIds = Array.from(selectedSamples);
    return selectedIds.some((id) =>
      actionableSamples.find((sample) => sample.id === id),
    );
  }, [selectedSamples, getActionableSamples]);

  // Helper to get counts of selected sample statuses
  const selectedSampleStatuses = useMemo(() => {
    const selectedIds = Array.from(selectedSamples);
    const transferredCount = selectedIds.filter((id) => {
      const sample = storageSamples.find((s) => s.id === id);
      return sample && sample.status === "BIOREPOSITORY_TRANSFER";
    }).length;
    const disposedCount = selectedIds.filter((id) => {
      const sample = storageSamples.find((s) => s.id === id);
      return sample && sample.status === "DISPOSED";
    }).length;
    const actionableCount = selectedIds.filter((id) => {
      const sample = storageSamples.find((s) => s.id === id);
      return (
        sample &&
        sample.status !== "BIOREPOSITORY_TRANSFER" &&
        sample.status !== "DISPOSED"
      );
    }).length;

    return { transferredCount, disposedCount, actionableCount };
  }, [selectedSamples, storageSamples]);

  const headers = [
    { key: "sampleId", header: "Sample ID" },
    { key: "type", header: "Type" },
    { key: "storageTemp", header: "Temperature" },
    { key: "retentionPeriod", header: "Retention Period" },
    { key: "retentionExpiryDate", header: "Expiry Date" },
    { key: "disposalReason", header: "Disposal Reason" },
    { key: "disposalMethod", header: "Disposal Method" },
    { key: "disposalDate", header: "Disposal Date" },
    { key: "disposalApprovedBy", header: "Approved By" },
  ];

  const additionalColumns = useMemo(
    () => [
      {
        key: "location",
        header: "Storage Location",
        render: (value) => getStorageLocationTag(value),
      },
      {
        key: "status",
        header: "Storage Status",
        render: (value) => getStatusTag(value),
      },
      {
        key: "disposalStatus",
        header: "Disposal Status",
        render: (value) => getDisposalStatusTag(value),
      },
    ],
    [],
  );

  if (!canAccessPage) {
    return (
      <AccessDeniedMessage
        page="Sample Storage & Archival"
        reason="This page requires specific bioanalytical laboratory roles to access."
        requiredRoles={allowedRoles}
      />
    );
  }

  return (
    <div className="bioanalytical-page">
      <div className="page-instructions">
        <h3>
          <FormattedMessage
            id="notebook.bioanalytical.storage.title"
            defaultMessage="Sample Storage & Archival"
          />
        </h3>
        <p>
          <FormattedMessage
            id="notebook.bioanalytical.storage.description"
            defaultMessage="Document sample storage locations and conditions, establish retention periods per regulatory requirements, plan long-term archival, and schedule final disposal or archival transfers."
          />
        </p>
      </div>

      <div style={{ paddingTop: "1.5rem" }}>
        <Grid>
          <Column lg={16} md={8} sm={4}>
            <div className="section-header">
              <h4>
                <FormattedMessage
                  id="notebook.bioanalytical.storage.inventorySection"
                  defaultMessage="Stage 4 Completed Samples - Ready for Storage & Archival"
                />
              </h4>
              <p>
                <FormattedMessage
                  id="notebook.bioanalytical.storage.inventoryHelp"
                  defaultMessage="Samples that have completed Stage 4 (QA approved and submitted/exported) are ready for biorepository transfer, retention storage at -80°C (2 years for bioequivalence), and data archival."
                />
              </p>

              {isLoading ? (
                <Loading description="Loading sample inventory..." />
              ) : storageSamples.length > 0 ? (
                <div style={{ marginTop: "1.5rem" }}>
                  <div
                    style={{
                      marginBottom: "1rem",
                      padding: "0.75rem",
                      backgroundColor: "#f4f4f4",
                      borderRadius: "4px",
                    }}
                  >
                    <p style={{ fontSize: "0.875rem", margin: 0 }}>
                      <strong>
                        <FormattedMessage
                          id="notebook.bioanalytical.storage.totalSamples"
                          defaultMessage="Total Samples:"
                        />
                      </strong>{" "}
                      {storageSamples.length}{" "}
                      <span style={{ color: "#525252" }}>
                        (
                        <FormattedMessage
                          id="notebook.bioanalytical.storage.actionableSamples"
                          defaultMessage="{actionable} actionable, {transferred} transferred"
                          values={{
                            actionable: getActionableSamples().length,
                            transferred: storageSamples.filter(
                              (s) =>
                                s.status === "BIOREPOSITORY_TRANSFER" ||
                                s.status === "DISPOSED",
                            ).length,
                          }}
                        />
                        )
                      </span>
                      {selectedSamples.size > 0 && (
                        <span style={{ marginLeft: "1rem", color: "#0043ce" }}>
                          |{" "}
                          <FormattedMessage
                            id="notebook.bioanalytical.storage.samplesSelected"
                            defaultMessage="{count} selected"
                            values={{ count: selectedSamples.size }}
                          />
                          {!hasActionableSelectedSamples && (
                            <span
                              style={{ color: "#fa4d56", marginLeft: "0.5rem" }}
                            >
                              (already processed)
                            </span>
                          )}
                        </span>
                      )}
                    </p>
                  </div>

                  {selectedSamples.size > 0 && (
                    <div
                      style={{
                        marginBottom: "1rem",
                        display: "flex",
                        gap: "1rem",
                        flexWrap: "wrap",
                      }}
                    >
                      {/* Biorepository Transfer - Now Enabled */}
                      <Button
                        kind="primary"
                        onClick={() => setBiorepositoryModalOpen(true)}
                        disabled={!hasActionableSelectedSamples}
                        title={
                          selectedSamples.size === 0
                            ? "Select samples to transfer to biorepository"
                            : !hasActionableSelectedSamples
                              ? "Selected samples have already been transferred or disposed"
                              : "Transfer selected samples to biorepository for long-term storage"
                        }
                      >
                        {selectedSampleStatuses.transferredCount > 0 &&
                        selectedSampleStatuses.actionableCount === 0 ? (
                          <FormattedMessage
                            id="notebook.bioanalytical.storage.samplesAlreadyTransferred"
                            defaultMessage="{count} samples already transferred"
                            values={{
                              count: selectedSampleStatuses.transferredCount,
                            }}
                          />
                        ) : selectedSampleStatuses.transferredCount > 0 ? (
                          <FormattedMessage
                            id="notebook.bioanalytical.storage.transferRemaining"
                            defaultMessage="Transfer {actionable} remaining ({transferred} already transferred)"
                            values={{
                              actionable:
                                selectedSampleStatuses.actionableCount,
                              transferred:
                                selectedSampleStatuses.transferredCount,
                            }}
                          />
                        ) : (
                          <FormattedMessage
                            id="notebook.bioanalytical.storage.transferBiorepository"
                            defaultMessage="Transfer to Biorepository"
                          />
                        )}
                      </Button>
                      <Button
                        kind="secondary"
                        onClick={() => setRetentionStorageModalOpen(true)}
                        disabled={
                          isLoading ||
                          !(canModifyStorage || canApproveStorage) ||
                          !hasActionableSelectedSamples
                        }
                        title={
                          !hasActionableSelectedSamples &&
                          selectedSamples.size > 0
                            ? "Selected samples have already been transferred or disposed"
                            : "Place selected samples in retention storage"
                        }
                      >
                        <FormattedMessage
                          id="notebook.bioanalytical.storage.retentionStorage"
                          defaultMessage="Retention Storage"
                        />
                      </Button>
                      <Button
                        kind="danger--tertiary"
                        onClick={() => setDisposalModalOpen(true)}
                        disabled={
                          isLoading ||
                          !canApproveStorage ||
                          !hasActionableSelectedSamples
                        }
                        title={
                          !hasActionableSelectedSamples &&
                          selectedSamples.size > 0
                            ? "Selected samples have already been transferred or disposed"
                            : "Schedule selected samples for disposal"
                        }
                      >
                        <FormattedMessage
                          id="notebook.bioanalytical.storage.manageSampleDisposal"
                          defaultMessage="Manage Sample Disposal"
                        />
                      </Button>
                    </div>
                  )}

                  <SampleGrid
                    gridId="storage-samples"
                    className="compact-storage-table"
                    samples={storageSamples}
                    selectedIds={Array.from(selectedSamples)}
                    onSelectionChange={(ids) => {
                      // Only allow selection of samples that haven't been transferred or disposed
                      const selectableSamples = ids.filter((id) => {
                        const sample = storageSamples.find((s) => s.id === id);
                        return (
                          sample &&
                          sample.status !== "DISPOSED" &&
                          sample.status !== "BIOREPOSITORY_TRANSFER"
                        );
                      });
                      setSelectedSamples(new Set(selectableSamples));
                    }}
                    onSampleClick={handleSampleDetailsClick}
                    showSelection={true}
                    loading={isLoading}
                    columns={headers}
                    additionalColumns={additionalColumns}
                  />
                </div>
              ) : (
                <div
                  style={{
                    marginTop: "1.5rem",
                    padding: "1rem",
                    backgroundColor: "#f4f4f4",
                    borderRadius: "4px",
                    textAlign: "center",
                  }}
                >
                  <p style={{ color: "#525252" }}>
                    <FormattedMessage
                      id="notebook.bioanalytical.storage.noSamples"
                      defaultMessage="No samples in storage"
                    />
                  </p>
                </div>
              )}
            </div>
          </Column>
        </Grid>
      </div>

      {/* Data Archival Section */}
      <div style={{ paddingTop: "1.5rem" }}>
        <Grid>
          <Column lg={16} md={8} sm={4}>
            <div className="section-header">
              <h4>
                <FormattedMessage
                  id="notebook.bioanalytical.storage.archivalSection"
                  defaultMessage="Data Archival & Records Management"
                />
              </h4>
              <p>
                <FormattedMessage
                  id="notebook.bioanalytical.storage.archivalHelp"
                  defaultMessage="Archive and track retention of analytical data, reports, calibration records, and study data per regulatory requirements."
                />
              </p>

              <div style={{ marginTop: "1.5rem" }}>
                <Button
                  kind="secondary"
                  onClick={() => setArchivalModalOpen(true)}
                  disabled={isLoading}
                >
                  <FormattedMessage
                    id="notebook.bioanalytical.storage.addArchiveRecord"
                    defaultMessage="Add Archive Record"
                  />
                </Button>
              </div>

              {archiveRecords.length > 0 ? (
                <div style={{ marginTop: "1.5rem" }}>
                  <div style={{ overflowX: "auto" }}>
                    <table
                      style={{
                        width: "100%",
                        borderCollapse: "collapse",
                        fontSize: "0.875rem",
                      }}
                    >
                      <thead>
                        <tr
                          style={{
                            backgroundColor: "#f4f4f4",
                            borderBottom: "1px solid #ccc",
                          }}
                        >
                          <th
                            style={{
                              padding: "0.75rem",
                              textAlign: "left",
                              fontWeight: "600",
                            }}
                          >
                            <FormattedMessage
                              id="notebook.bioanalytical.storage.archiveType"
                              defaultMessage="Archive Type"
                            />
                          </th>
                          <th
                            style={{
                              padding: "0.75rem",
                              textAlign: "left",
                              fontWeight: "600",
                            }}
                          >
                            <FormattedMessage
                              id="notebook.bioanalytical.storage.archiveLocation"
                              defaultMessage="Location/System"
                            />
                          </th>
                          <th
                            style={{
                              padding: "0.75rem",
                              textAlign: "left",
                              fontWeight: "600",
                            }}
                          >
                            <FormattedMessage
                              id="notebook.bioanalytical.storage.archiveDate"
                              defaultMessage="Archival Date"
                            />
                          </th>
                          <th
                            style={{
                              padding: "0.75rem",
                              textAlign: "left",
                              fontWeight: "600",
                            }}
                          >
                            <FormattedMessage
                              id="notebook.bioanalytical.storage.purgeDate"
                              defaultMessage="Purge/Disposal Date"
                            />
                          </th>
                          <th
                            style={{
                              padding: "0.75rem",
                              textAlign: "left",
                              fontWeight: "600",
                            }}
                          >
                            Files
                          </th>
                          <th
                            style={{
                              padding: "0.75rem",
                              textAlign: "left",
                              fontWeight: "600",
                            }}
                          >
                            Linked Samples
                          </th>
                          <th
                            style={{
                              padding: "0.75rem",
                              textAlign: "left",
                              fontWeight: "600",
                            }}
                          >
                            <FormattedMessage
                              id="notebook.bioanalytical.storage.notes"
                              defaultMessage="Notes"
                            />
                          </th>
                        </tr>
                      </thead>
                      <tbody>
                        {archiveRecords.map((record) => (
                          <tr
                            key={record.id}
                            style={{
                              borderBottom: "1px solid #e0e0e0",
                            }}
                          >
                            <td
                              style={{
                                padding: "0.75rem",
                                color: "#161616",
                              }}
                            >
                              <strong>{record.archiveTypeLabel}</strong>
                            </td>
                            <td
                              style={{
                                padding: "0.75rem",
                                color: "#525252",
                              }}
                            >
                              {record.archiveLocation}
                            </td>
                            <td
                              style={{
                                padding: "0.75rem",
                                color: "#525252",
                              }}
                            >
                              {record.archivalDate}
                            </td>
                            <td
                              style={{
                                padding: "0.75rem",
                                color: "#525252",
                              }}
                            >
                              {record.purgeDate}
                            </td>
                            <td
                              style={{
                                padding: "0.75rem",
                                color: "#525252",
                                fontSize: "0.75rem",
                              }}
                            >
                              {record.files?.length > 0 ? (
                                <div>
                                  <Tag type="blue" size="sm">
                                    {record.files.length} file
                                    {record.files.length > 1 ? "s" : ""}
                                  </Tag>
                                  <br />
                                  <span
                                    style={{
                                      fontSize: "0.65rem",
                                      color: "#8d8d8d",
                                    }}
                                  >
                                    {(
                                      record.metadata?.totalSize /
                                      1024 /
                                      1024
                                    )?.toFixed(1) || "0"}{" "}
                                    MB
                                  </span>
                                </div>
                              ) : (
                                <span style={{ color: "#8d8d8d" }}>
                                  No files
                                </span>
                              )}
                            </td>
                            <td
                              style={{
                                padding: "0.75rem",
                                color: "#525252",
                                fontSize: "0.75rem",
                              }}
                            >
                              {record.linkedSamples?.length > 0 ? (
                                <div>
                                  <Tag type="green" size="sm">
                                    {record.linkedSamples.length} sample
                                    {record.linkedSamples.length > 1 ? "s" : ""}
                                  </Tag>
                                  <br />
                                  <span
                                    style={{
                                      fontSize: "0.65rem",
                                      color: "#8d8d8d",
                                    }}
                                  >
                                    {record.linkedSamples
                                      .slice(0, 2)
                                      .map((s) => s.sampleLabel)
                                      .join(", ")}
                                    {record.linkedSamples.length > 2
                                      ? "..."
                                      : ""}
                                  </span>
                                </div>
                              ) : (
                                <span style={{ color: "#8d8d8d" }}>
                                  Not linked
                                </span>
                              )}
                            </td>
                            <td
                              style={{
                                padding: "0.75rem",
                                color: "#525252",
                                fontSize: "0.75rem",
                              }}
                            >
                              {record.notes || "-"}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              ) : (
                <div
                  style={{
                    marginTop: "1.5rem",
                    padding: "1rem",
                    backgroundColor: "#f4f4f4",
                    borderRadius: "4px",
                    textAlign: "center",
                  }}
                >
                  <p style={{ color: "#525252" }}>
                    <FormattedMessage
                      id="notebook.bioanalytical.storage.noArchiveRecords"
                      defaultMessage="No archive records added yet"
                    />
                  </p>
                </div>
              )}
            </div>
          </Column>
        </Grid>
      </div>

      {/* Sample Disposal Modal */}
      <Modal
        open={disposalModalOpen}
        onRequestClose={() => setDisposalModalOpen(false)}
        modalHeading="Sample Disposal Management"
        primaryButtonText="Schedule Disposal"
        secondaryButtonText="Cancel"
        onRequestSubmit={handleSampleDisposal}
        preventCloseOnClickOutside
        size="lg"
      >
        <div style={{ marginBottom: "1.5rem" }}>
          <p style={{ marginBottom: "1rem", color: "#525252" }}>
            Schedule disposal for selected bioequivalence study samples after
            retention period completion.
          </p>

          <div style={{ display: "flex", gap: "1rem", marginBottom: "1rem" }}>
            <div style={{ flex: 1 }}>
              <ComboBox
                id="disposal-reason"
                items={disposalReasons}
                itemToString={(item) => (item ? item.label : "")}
                selectedItem={
                  disposalReasons.find((r) => r.id === disposalReason) || null
                }
                onChange={({ selectedItem }) =>
                  setDisposalReason(selectedItem?.id || "")
                }
                placeholder="Select a reason"
                titleText="Disposal Reason"
              />
            </div>

            <div style={{ flex: 1 }}>
              <ComboBox
                id="disposal-method"
                items={disposalMethods}
                itemToString={(item) => (item ? item.label : "")}
                selectedItem={
                  disposalMethods.find((m) => m.id === disposalMethod) || null
                }
                onChange={({ selectedItem }) =>
                  setDisposalMethod(selectedItem?.id || "")
                }
                placeholder="Select a method"
                titleText="Disposal Method"
              />
            </div>
          </div>

          <div style={{ marginBottom: "1rem" }}>
            <label
              style={{
                display: "block",
                marginBottom: "0.5rem",
                fontWeight: "600",
                fontSize: "0.875rem",
              }}
            >
              Supervisor Approval (Name)
            </label>
            <input
              type="text"
              value={supervisorApproval}
              onChange={(e) => setSupervisorApproval(e.target.value)}
              placeholder="Enter supervisor name for approval"
              style={{
                width: "100%",
                padding: "0.75rem",
                border: "1px solid #ccc",
                borderRadius: "4px",
                fontSize: "0.875rem",
              }}
            />
          </div>

          <div style={{ marginBottom: "1rem" }}>
            <TextArea
              id="disposal-notes"
              labelText="Notes"
              value={disposalNotes}
              onChange={(e) => setDisposalNotes(e.target.value)}
              placeholder="Add any additional notes about this disposal"
            />
          </div>

          {selectedSamples.size > 0 && (
            <div
              style={{
                backgroundColor: "#fef3c7",
                padding: "1rem",
                borderRadius: "4px",
                border: "1px solid #f59e0b",
              }}
            >
              <p
                style={{
                  fontSize: "0.875rem",
                  margin: 0,
                  color: "#92400e",
                  fontWeight: "600",
                }}
              >
                ⚠️ Disposal Warning
              </p>
              <p
                style={{
                  fontSize: "0.875rem",
                  margin: "0.5rem 0 0 0",
                  color: "#92400e",
                }}
              >
                <strong>Samples to dispose:</strong> {selectedSamples.size}{" "}
                samples
              </p>
              <p
                style={{
                  fontSize: "0.75rem",
                  margin: "0.5rem 0 0 0",
                  color: "#92400e",
                }}
              >
                This action will permanently schedule samples for disposal and
                cannot be undone.
              </p>
            </div>
          )}
        </div>
      </Modal>

      {/* Sample Details View Modal */}
      <Modal
        open={viewDetailsModalOpen}
        onRequestClose={() => setViewDetailsModalOpen(false)}
        modalHeading={
          selectedSampleDetail
            ? `Sample Details - ${selectedSampleDetail.sampleId || selectedSampleDetail.accessionNumber || `S${selectedSampleDetail.id}`}`
            : "Sample Details"
        }
        secondaryButtonText="Close"
        primaryButtonText=""
        size="lg"
      >
        {detailLoading ? (
          <div style={{ textAlign: "center", padding: "2rem" }}>
            <Loading description="Loading sample details..." />
          </div>
        ) : selectedSampleDetail ? (
          <div style={{ marginBottom: "1.5rem" }}>
            <Grid>
              <Column lg={8} md={4} sm={4} style={{ marginBottom: "1.5rem" }}>
                <div>
                  <h5 style={{ marginBottom: "0.5rem", color: "#161616" }}>
                    <FormattedMessage
                      id="notebook.bioanalytical.storage.sampleId"
                      defaultMessage="Sample ID"
                    />
                  </h5>
                  <p style={{ margin: 0, color: "#525252" }}>
                    {selectedSampleDetail.sampleId ||
                      selectedSampleDetail.accessionNumber ||
                      `S${selectedSampleDetail.id}`}
                  </p>
                </div>
              </Column>

              <Column lg={8} md={4} sm={4} style={{ marginBottom: "1.5rem" }}>
                <div>
                  <h5 style={{ marginBottom: "0.5rem", color: "#161616" }}>
                    <FormattedMessage
                      id="notebook.bioanalytical.storage.sampleType"
                      defaultMessage="Sample Type"
                    />
                  </h5>
                  <p style={{ margin: 0, color: "#525252" }}>
                    {selectedSampleDetail.type ||
                      selectedSampleDetail.sampleType ||
                      "-"}
                  </p>
                </div>
              </Column>

              <Column lg={8} md={4} sm={4} style={{ marginBottom: "1.5rem" }}>
                <div>
                  <h5 style={{ marginBottom: "0.5rem", color: "#161616" }}>
                    <FormattedMessage
                      id="notebook.bioanalytical.storage.volume"
                      defaultMessage="Volume"
                    />
                  </h5>
                  <p style={{ margin: 0, color: "#525252" }}>
                    {selectedSampleDetail.volume ||
                      selectedSampleDetail.data?.sampleVolume ||
                      "-"}
                  </p>
                </div>
              </Column>

              <Column lg={8} md={4} sm={4} style={{ marginBottom: "1.5rem" }}>
                <div>
                  <h5 style={{ marginBottom: "0.5rem", color: "#161616" }}>
                    <FormattedMessage
                      id="notebook.bioanalytical.storage.storageLocation"
                      defaultMessage="Storage Location"
                    />
                  </h5>
                  <p style={{ margin: 0, color: "#525252" }}>
                    {selectedSampleDetail.location ||
                      selectedSampleDetail.data?.storageLocation ||
                      "-"}
                  </p>
                </div>
              </Column>

              <Column lg={8} md={4} sm={4} style={{ marginBottom: "1.5rem" }}>
                <div>
                  <h5 style={{ marginBottom: "0.5rem", color: "#161616" }}>
                    <FormattedMessage
                      id="notebook.bioanalytical.storage.temperature"
                      defaultMessage="Storage Temperature"
                    />
                  </h5>
                  <p style={{ margin: 0, color: "#525252" }}>
                    {selectedSampleDetail.storageTemp ||
                      selectedSampleDetail.data?.storageTemperature ||
                      "-"}
                  </p>
                </div>
              </Column>

              <Column lg={8} md={4} sm={4} style={{ marginBottom: "1.5rem" }}>
                <div>
                  <h5 style={{ marginBottom: "0.5rem", color: "#161616" }}>
                    <FormattedMessage
                      id="notebook.bioanalytical.storage.status"
                      defaultMessage="Status"
                    />
                  </h5>
                  <div style={{ margin: 0 }}>
                    {getStatusTag(
                      selectedSampleDetail.status ||
                        selectedSampleDetail.data?.storageStatus,
                    )}
                  </div>
                </div>
              </Column>

              {selectedSampleDetail.data?.disposalStatus && (
                <>
                  <Column
                    lg={16}
                    md={8}
                    sm={4}
                    style={{
                      marginBottom: "1rem",
                      marginTop: "1rem",
                      paddingTop: "1rem",
                      borderTop: "1px solid #e0e0e0",
                    }}
                  >
                    <h5 style={{ color: "#161616", marginBottom: "1rem" }}>
                      <FormattedMessage
                        id="notebook.bioanalytical.storage.disposalInfo"
                        defaultMessage="Disposal Information"
                      />
                    </h5>
                  </Column>

                  <Column
                    lg={8}
                    md={4}
                    sm={4}
                    style={{ marginBottom: "1.5rem" }}
                  >
                    <div>
                      <h5 style={{ marginBottom: "0.5rem", color: "#161616" }}>
                        <FormattedMessage
                          id="notebook.bioanalytical.storage.disposalReason"
                          defaultMessage="Disposal Reason"
                        />
                      </h5>
                      <p style={{ margin: 0, color: "#525252" }}>
                        {selectedSampleDetail.data?.disposalReason || "-"}
                      </p>
                    </div>
                  </Column>

                  <Column
                    lg={8}
                    md={4}
                    sm={4}
                    style={{ marginBottom: "1.5rem" }}
                  >
                    <div>
                      <h5 style={{ marginBottom: "0.5rem", color: "#161616" }}>
                        <FormattedMessage
                          id="notebook.bioanalytical.storage.disposalMethod"
                          defaultMessage="Disposal Method"
                        />
                      </h5>
                      <p style={{ margin: 0, color: "#525252" }}>
                        {selectedSampleDetail.data?.disposalMethod || "-"}
                      </p>
                    </div>
                  </Column>

                  <Column
                    lg={8}
                    md={4}
                    sm={4}
                    style={{ marginBottom: "1.5rem" }}
                  >
                    <div>
                      <h5 style={{ marginBottom: "0.5rem", color: "#161616" }}>
                        <FormattedMessage
                          id="notebook.bioanalytical.storage.disposalDate"
                          defaultMessage="Disposal Date"
                        />
                      </h5>
                      <p style={{ margin: 0, color: "#525252" }}>
                        {selectedSampleDetail.data?.disposalDate || "-"}
                      </p>
                    </div>
                  </Column>

                  <Column
                    lg={8}
                    md={4}
                    sm={4}
                    style={{ marginBottom: "1.5rem" }}
                  >
                    <div>
                      <h5 style={{ marginBottom: "0.5rem", color: "#161616" }}>
                        <FormattedMessage
                          id="notebook.bioanalytical.storage.approvedBy"
                          defaultMessage="Approved By"
                        />
                      </h5>
                      <p style={{ margin: 0, color: "#525252" }}>
                        {selectedSampleDetail.data?.disposalApprovedBy || "-"}
                      </p>
                    </div>
                  </Column>

                  {selectedSampleDetail.data?.disposalNotes && (
                    <Column
                      lg={16}
                      md={8}
                      sm={4}
                      style={{ marginBottom: "1.5rem" }}
                    >
                      <div>
                        <h5
                          style={{ marginBottom: "0.5rem", color: "#161616" }}
                        >
                          <FormattedMessage
                            id="notebook.bioanalytical.storage.disposalNotes"
                            defaultMessage="Disposal Notes"
                          />
                        </h5>
                        <p
                          style={{
                            margin: 0,
                            color: "#525252",
                            whiteSpace: "pre-wrap",
                          }}
                        >
                          {selectedSampleDetail.data.disposalNotes}
                        </p>
                      </div>
                    </Column>
                  )}
                </>
              )}
            </Grid>
          </div>
        ) : (
          <p>
            <FormattedMessage
              id="notebook.bioanalytical.storage.noDetailsAvailable"
              defaultMessage="No sample details available"
            />
          </p>
        )}
      </Modal>

      {/* Biorepository Transfer Modal */}
      <Modal
        open={biorepositoryModalOpen}
        onRequestClose={clearBiorepositoryModalState}
        modalHeading="Transfer to Biorepository"
        primaryButtonText="Confirm Transfer"
        secondaryButtonText="Cancel"
        onRequestSubmit={handleBiorepositoryTransfer}
        preventCloseOnClickOutside
        size="lg"
      >
        <div style={{ marginBottom: "1.5rem" }}>
          <p style={{ marginBottom: "1rem", color: "#525252" }}>
            Transfer selected samples to the biorepository for long-term storage
            and preservation. This will create a formal transfer request that
            biorepository staff can review and accept.
          </p>
          <p
            style={{
              marginBottom: "1rem",
              color: "#525252",
              fontSize: "0.875rem",
            }}
          >
            Selected samples: <strong>{selectedSamples.size}</strong>
          </p>

          <div style={{ marginBottom: "1rem" }}>
            <div>
              <label
                style={{
                  display: "block",
                  marginBottom: "0.5rem",
                  fontWeight: "600",
                  fontSize: "0.875rem",
                }}
              >
                Biorepository Storage Location
              </label>
              <p
                style={{
                  marginBottom: "1rem",
                  color: "#525252",
                  fontSize: "0.875rem",
                }}
              >
                Select the specific storage location in the biorepository where
                samples will be placed. You can assign at any level.
              </p>
              <StorageHierarchySelector
                onSelectionChange={(selection) => {
                  setSelectedStorageHierarchy(selection);
                  const locationParts = [];
                  if (selection.room?.name)
                    locationParts.push(`Room: ${selection.room.name}`);
                  if (selection.device?.name)
                    locationParts.push(`Device: ${selection.device.name}`);
                  if (selection.shelf?.name)
                    locationParts.push(`Shelf: ${selection.shelf.name}`);
                  if (selection.rack?.name)
                    locationParts.push(`Rack: ${selection.rack.name}`);
                  if (selection.box?.name)
                    locationParts.push(`Box: ${selection.box.name}`);
                  setStorageLocation(
                    locationParts.join(", ") || "Biorepository",
                  );
                }}
                initialSelection={selectedStorageHierarchy}
                boxRequired={true}
                showPath={true}
              />
            </div>
          </div>

          <div style={{ marginBottom: "1rem" }}>
            <div>
              <label
                style={{
                  display: "block",
                  marginBottom: "0.5rem",
                  fontWeight: "600",
                  fontSize: "0.875rem",
                }}
              >
                Storage Temperature
              </label>
              <ComboBox
                id="storage-temperature-selector"
                items={temperatureOptions}
                itemToString={(item) => (item ? item.text : "")}
                selectedItem={
                  storageTemperature
                    ? temperatureOptions.find(
                        (item) => item.id === storageTemperature,
                      )
                    : null
                }
                onChange={({ selectedItem }) => {
                  setStorageTemperature(selectedItem ? selectedItem.id : "");
                }}
                placeholder="Select storage temperature"
                helperText="Select the required storage temperature for the samples"
              />
            </div>
          </div>

          <div style={{ marginBottom: "1rem" }}>
            <TextArea
              id="biorepository-notes"
              labelText="Notes"
              value={storageNotes}
              onChange={(e) => setStorageNotes(e.target.value)}
              placeholder="Add any additional notes about biorepository transfer"
            />
          </div>

          {selectedSamples.size > 0 && (
            <div
              style={{
                backgroundColor: "#d0f0ff",
                padding: "1rem",
                borderRadius: "4px",
                border: "1px solid #0f62fe",
              }}
            >
              <p
                style={{
                  fontSize: "0.875rem",
                  margin: 0,
                  color: "#0043ce",
                  fontWeight: "600",
                }}
              >
                ℹ️ Transfer Information
              </p>
              <p
                style={{
                  fontSize: "0.875rem",
                  margin: "0.5rem 0 0 0",
                  color: "#0043ce",
                }}
              >
                <strong>Samples to transfer:</strong> {selectedSamples.size}{" "}
                samples
              </p>
            </div>
          )}
        </div>
      </Modal>

      {/* Retention Storage Modal */}
      <Modal
        open={retentionStorageModalOpen}
        onRequestClose={() => setRetentionStorageModalOpen(false)}
        modalHeading="Place in Retention Storage"
        primaryButtonText="Confirm Storage"
        secondaryButtonText="Cancel"
        onRequestSubmit={handleRetentionStorage}
        preventCloseOnClickOutside
        size="lg"
      >
        <div style={{ marginBottom: "1.5rem" }}>
          <p style={{ marginBottom: "1rem", color: "#525252" }}>
            Place selected samples in retention storage per regulatory
            requirements (typically -80°C for 2 years in bioequivalence
            studies).
          </p>

          {/* Hierarchical Storage Location Selector */}
          <div
            style={{
              backgroundColor: "#f4f4f4",
              padding: "1rem",
              borderRadius: "4px",
              border: "1px solid #e0e0e0",
              marginBottom: "1rem",
            }}
          >
            <FormLabel
              style={{
                marginBottom: "0.75rem",
                display: "block",
                fontWeight: "600",
              }}
            >
              <FormattedMessage
                id="notebook.bioanalytical.storage.location"
                defaultMessage="Storage Location"
              />
              <span style={{ color: "#da1e28" }}> *</span>
            </FormLabel>
            <p
              style={{
                fontSize: "0.75rem",
                color: "#525252",
                marginBottom: "1rem",
              }}
            >
              <FormattedMessage
                id="notebook.bioanalytical.storage.location.helper"
                defaultMessage="Select the storage hierarchy where samples will be retained. You can assign at any level (room, device, shelf, rack, or box)."
              />
            </p>
            <StorageHierarchySelector
              onSelectionChange={(selection) => {
                setStorageSelection(selection);
                // Auto-populate storage location from hierarchy
                const storagePath = buildStoragePathFromSelection(selection);
                setStorageLocation(storagePath);
              }}
              boxRequired={false}
              showPath={true}
            />
          </div>

          <div style={{ marginBottom: "1rem" }}>
            <label
              style={{
                display: "block",
                marginBottom: "0.5rem",
                fontWeight: "600",
                fontSize: "0.875rem",
              }}
            >
              Storage Temperature
            </label>
            <input
              type="text"
              value={storageTemperature}
              onChange={(e) => setStorageTemperature(e.target.value)}
              placeholder="e.g., -80°C"
              style={{
                width: "100%",
                padding: "0.75rem",
                border: "1px solid #ccc",
                borderRadius: "4px",
                fontSize: "0.875rem",
              }}
            />
          </div>

          <div style={{ marginBottom: "1rem" }}>
            <label
              style={{
                display: "block",
                marginBottom: "0.5rem",
                fontWeight: "600",
                fontSize: "0.875rem",
              }}
            >
              Retention Period
            </label>
            <select
              value={retentionPeriod}
              onChange={(e) => setRetentionPeriod(e.target.value)}
              style={{
                width: "100%",
                padding: "0.75rem",
                border: "1px solid #ccc",
                borderRadius: "4px",
                fontSize: "0.875rem",
              }}
            >
              <option value="">Select retention period...</option>
              <option value="6 months">6 months</option>
              <option value="1 year">1 year</option>
              <option value="2 years">2 years (Bioequivalence Standard)</option>
              <option value="3 years">3 years</option>
              <option value="5 years">5 years</option>
              <option value="10 years">10 years</option>
            </select>
          </div>

          <div style={{ marginBottom: "1rem" }}>
            <TextArea
              id="retention-notes"
              labelText="Notes"
              value={storageNotes}
              onChange={(e) => setStorageNotes(e.target.value)}
              placeholder="Add any additional notes about retention storage"
            />
          </div>

          {selectedSamples.size > 0 && (
            <div
              style={{
                backgroundColor: "#f0f3ff",
                padding: "1rem",
                borderRadius: "4px",
                border: "1px solid #7f10f0",
              }}
            >
              <p
                style={{
                  fontSize: "0.875rem",
                  margin: 0,
                  color: "#7f10f0",
                  fontWeight: "600",
                }}
              >
                ℹ️ Retention Information
              </p>
              <p
                style={{
                  fontSize: "0.875rem",
                  margin: "0.5rem 0 0 0",
                  color: "#7f10f0",
                }}
              >
                <strong>Samples to store:</strong> {selectedSamples.size}{" "}
                samples
              </p>
              {retentionPeriod && (
                <p
                  style={{
                    fontSize: "0.875rem",
                    margin: "0.5rem 0 0 0",
                    color: "#7f10f0",
                  }}
                >
                  <strong>Expiry Date:</strong>{" "}
                  {calculateRetentionExpiryDate(retentionPeriod)}
                </p>
              )}
            </div>
          )}
        </div>
      </Modal>

      {/* Data Archival Modal */}
      <Modal
        open={archivalModalOpen}
        onRequestClose={() => setArchivalModalOpen(false)}
        modalHeading="Add Data Archive Record"
        primaryButtonText="Add Record"
        secondaryButtonText="Cancel"
        onRequestSubmit={handleAddArchiveRecord}
        preventCloseOnClickOutside
        size="lg"
      >
        <div style={{ marginBottom: "1.5rem" }}>
          <p style={{ marginBottom: "1rem", color: "#525252" }}>
            Archive analytical data, reports, calibration records, and study
            data per regulatory retention requirements. Track archival location
            and scheduled purge/disposal dates.
          </p>

          <div style={{ marginBottom: "1rem" }}>
            <label
              style={{
                display: "block",
                marginBottom: "0.5rem",
                fontWeight: "600",
                fontSize: "0.875rem",
              }}
            >
              Archive Type
            </label>
            <select
              value={archiveType}
              onChange={(e) => setArchiveType(e.target.value)}
              style={{
                width: "100%",
                padding: "0.75rem",
                border: "1px solid #ccc",
                borderRadius: "4px",
                fontSize: "0.875rem",
              }}
            >
              <option value="">Select archive type...</option>
              {archiveTypes.map((type) => (
                <option key={type.id} value={type.id}>
                  {type.label} (Retention: {type.retention})
                </option>
              ))}
            </select>
            {archiveType && (
              <p
                style={{
                  fontSize: "0.75rem",
                  color: "#525252",
                  margin: "0.5rem 0 0 0",
                  fontStyle: "italic",
                }}
              >
                {archiveTypes.find((t) => t.id === archiveType)?.description}
              </p>
            )}
          </div>

          {/* File Upload Section */}
          {archiveType && (
            <div style={{ marginBottom: "1.5rem" }}>
              <label
                style={{
                  display: "block",
                  marginBottom: "0.5rem",
                  fontWeight: "600",
                  fontSize: "0.875rem",
                }}
              >
                Archive Files
              </label>
              <FileUploader
                labelTitle="Upload Files"
                labelDescription={`Accepted formats: ${archiveTypes.find((t) => t.id === archiveType)?.fileTypes?.join(", ") || "All formats"}`}
                buttonLabel="Select files"
                buttonKind="primary"
                size="md"
                filenameStatus="edit"
                accept={
                  archiveTypes.find((t) => t.id === archiveType)?.fileTypes
                }
                multiple={true}
                disabled={false}
                iconDescription="Delete file"
                onChange={(e) => {
                  const files = Array.from(e.target.files);
                  setArchiveFiles(files);
                }}
              />
              {archiveFiles.length > 0 && (
                <div
                  style={{
                    marginTop: "0.5rem",
                    fontSize: "0.75rem",
                    color: "#525252",
                  }}
                >
                  {archiveFiles.length} file(s) selected (
                  {(
                    archiveFiles.reduce((sum, f) => sum + f.size, 0) /
                    1024 /
                    1024
                  ).toFixed(2)}{" "}
                  MB)
                </div>
              )}
            </div>
          )}

          {/* Sample Linking Section */}
          {selectedSamples.size > 0 && (
            <div style={{ marginBottom: "1rem" }}>
              <label
                style={{
                  display: "block",
                  marginBottom: "0.5rem",
                  fontWeight: "600",
                  fontSize: "0.875rem",
                }}
              >
                Link to Samples
              </label>
              <div
                style={{
                  maxHeight: "120px",
                  overflowY: "auto",
                  border: "1px solid #e0e0e0",
                  borderRadius: "4px",
                  padding: "0.5rem",
                }}
              >
                {Array.from(selectedSamples).map((sampleId) => {
                  const sample = storageSamples.find((s) => s.id === sampleId);
                  return (
                    <Checkbox
                      key={sampleId}
                      id={`link-sample-${sampleId}`}
                      labelText={`${sample?.sampleId || `S${sampleId}`} - ${sample?.type || "Unknown"}`}
                      checked={linkedSamples.includes(sampleId)}
                      onChange={(checked) => {
                        if (checked) {
                          setLinkedSamples((prev) => [...prev, sampleId]);
                        } else {
                          setLinkedSamples((prev) =>
                            prev.filter((id) => id !== sampleId),
                          );
                        }
                      }}
                    />
                  );
                })}
              </div>
              {linkedSamples.length > 0 && (
                <p
                  style={{
                    fontSize: "0.75rem",
                    color: "#0f62fe",
                    margin: "0.5rem 0 0 0",
                  }}
                >
                  {linkedSamples.length} sample(s) will be linked to this
                  archive
                </p>
              )}
            </div>
          )}

          <div style={{ marginBottom: "1rem" }}>
            <label
              style={{
                display: "block",
                marginBottom: "0.5rem",
                fontWeight: "600",
                fontSize: "0.875rem",
              }}
            >
              Archive Location / System
            </label>
            <input
              type="text"
              value={archiveLocation}
              onChange={(e) => setArchiveLocation(e.target.value)}
              placeholder="e.g., LIMS Server, Cloud Storage, File Room A3"
              style={{
                width: "100%",
                padding: "0.75rem",
                border: "1px solid #ccc",
                borderRadius: "4px",
                fontSize: "0.875rem",
              }}
            />
          </div>

          {/* Enhanced Metadata Section */}
          <div
            style={{
              marginBottom: "1.5rem",
              border: "1px solid #e0e0e0",
              borderRadius: "4px",
              padding: "1rem",
              backgroundColor: "#f9f9f9",
            }}
          >
            <h5
              style={{
                margin: "0 0 1rem 0",
                fontSize: "0.875rem",
                fontWeight: "600",
              }}
            >
              Archive Metadata
            </h5>

            <div
              style={{
                display: "grid",
                gridTemplateColumns: "1fr 1fr",
                gap: "1rem",
              }}
            >
              <div>
                <label
                  style={{
                    display: "block",
                    marginBottom: "0.5rem",
                    fontSize: "0.75rem",
                    fontWeight: "600",
                  }}
                >
                  Instrument ID
                </label>
                <input
                  type="text"
                  value={archiveMetadata.instrumentId}
                  onChange={(e) =>
                    setArchiveMetadata((prev) => ({
                      ...prev,
                      instrumentId: e.target.value,
                    }))
                  }
                  placeholder="e.g., LC-MS-001, HPLC-UV-002"
                  style={{
                    width: "100%",
                    padding: "0.5rem",
                    border: "1px solid #ccc",
                    borderRadius: "4px",
                    fontSize: "0.75rem",
                  }}
                />
              </div>

              <div>
                <label
                  style={{
                    display: "block",
                    marginBottom: "0.5rem",
                    fontSize: "0.75rem",
                    fontWeight: "600",
                  }}
                >
                  Batch/Run Number
                </label>
                <input
                  type="text"
                  value={archiveMetadata.batchNumber}
                  onChange={(e) =>
                    setArchiveMetadata((prev) => ({
                      ...prev,
                      batchNumber: e.target.value,
                    }))
                  }
                  placeholder="e.g., BATCH-2024-001"
                  style={{
                    width: "100%",
                    padding: "0.5rem",
                    border: "1px solid #ccc",
                    borderRadius: "4px",
                    fontSize: "0.75rem",
                  }}
                />
              </div>

              <div>
                <label
                  style={{
                    display: "block",
                    marginBottom: "0.5rem",
                    fontSize: "0.75rem",
                    fontWeight: "600",
                  }}
                >
                  Analyst Name
                </label>
                <input
                  type="text"
                  value={archiveMetadata.analystName}
                  onChange={(e) =>
                    setArchiveMetadata((prev) => ({
                      ...prev,
                      analystName: e.target.value,
                    }))
                  }
                  placeholder="Analyst who generated data"
                  style={{
                    width: "100%",
                    padding: "0.5rem",
                    border: "1px solid #ccc",
                    borderRadius: "4px",
                    fontSize: "0.75rem",
                  }}
                />
              </div>

              <div>
                <label
                  style={{
                    display: "block",
                    marginBottom: "0.5rem",
                    fontSize: "0.75rem",
                    fontWeight: "600",
                  }}
                >
                  Study ID
                </label>
                <input
                  type="text"
                  value={archiveMetadata.studyId}
                  onChange={(e) =>
                    setArchiveMetadata((prev) => ({
                      ...prev,
                      studyId: e.target.value,
                    }))
                  }
                  placeholder="e.g., BE-STUDY-2024-001"
                  style={{
                    width: "100%",
                    padding: "0.5rem",
                    border: "1px solid #ccc",
                    borderRadius: "4px",
                    fontSize: "0.75rem",
                  }}
                />
              </div>
            </div>

            <div style={{ marginTop: "1rem" }}>
              <label
                style={{
                  display: "block",
                  marginBottom: "0.5rem",
                  fontSize: "0.75rem",
                  fontWeight: "600",
                }}
              >
                Compliance Notes
              </label>
              <textarea
                value={archiveMetadata.complianceNotes}
                onChange={(e) =>
                  setArchiveMetadata((prev) => ({
                    ...prev,
                    complianceNotes: e.target.value,
                  }))
                }
                placeholder="Regulatory compliance notes, special handling requirements, etc."
                style={{
                  width: "100%",
                  padding: "0.5rem",
                  border: "1px solid #ccc",
                  borderRadius: "4px",
                  fontSize: "0.75rem",
                  minHeight: "60px",
                  resize: "vertical",
                }}
              />
            </div>
          </div>

          <div style={{ marginBottom: "1rem" }}>
            <label
              style={{
                display: "block",
                marginBottom: "0.5rem",
                fontWeight: "600",
                fontSize: "0.875rem",
              }}
            >
              Retention Period (Years)
            </label>
            <select
              value={retentionYears}
              onChange={(e) => setRetentionYears(e.target.value)}
              style={{
                width: "100%",
                padding: "0.75rem",
                border: "1px solid #ccc",
                borderRadius: "4px",
                fontSize: "0.875rem",
              }}
            >
              <option value="">Select retention period...</option>
              <option value="0">Permanent</option>
              <option value="2">2 years (Bioequivalence)</option>
              <option value="5">5 years</option>
              <option value="10">10 years</option>
              <option value="15">15 years</option>
              <option value="20">20 years</option>
            </select>
          </div>

          <div style={{ marginBottom: "1rem" }}>
            <TextArea
              id="archive-notes"
              labelText="Notes"
              value={archiveNotes}
              onChange={(e) => setArchiveNotes(e.target.value)}
              placeholder="Add any additional notes about this archive (backup location, contact info, etc.)"
            />
          </div>

          {archiveType && retentionYears && (
            <div
              style={{
                backgroundColor: "#f0f3ff",
                padding: "1rem",
                borderRadius: "4px",
                border: "1px solid #7f10f0",
              }}
            >
              <p
                style={{
                  fontSize: "0.875rem",
                  margin: 0,
                  color: "#7f10f0",
                  fontWeight: "600",
                }}
              >
                ℹ️ Archive Information
              </p>
              <p
                style={{
                  fontSize: "0.875rem",
                  margin: "0.5rem 0 0 0",
                  color: "#7f10f0",
                }}
              >
                <strong>Archive Type:</strong>{" "}
                {archiveTypes.find((t) => t.id === archiveType)?.label}
              </p>
              <p
                style={{
                  fontSize: "0.875rem",
                  margin: "0.5rem 0 0 0",
                  color: "#7f10f0",
                }}
              >
                <strong>Retention:</strong>{" "}
                {retentionYears === "0"
                  ? "Permanent"
                  : `${retentionYears} years`}
              </p>
              <p
                style={{
                  fontSize: "0.875rem",
                  margin: "0.5rem 0 0 0",
                  color: "#7f10f0",
                }}
              >
                <strong>Archival Date:</strong>{" "}
                {new Date().toISOString().split("T")[0]}
              </p>
              {retentionYears !== "0" && (
                <p
                  style={{
                    fontSize: "0.875rem",
                    margin: "0.5rem 0 0 0",
                    color: "#7f10f0",
                  }}
                >
                  <strong>Purge/Disposal Date:</strong>{" "}
                  {calculateArchivePurgeDate(retentionYears)}
                </p>
              )}
            </div>
          )}
        </div>
      </Modal>
    </div>
  );
}

export default BioanalyticalStorageArchivingPage;
