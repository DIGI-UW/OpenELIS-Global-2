import React, {
  useState,
  useEffect,
  useRef,
  useCallback,
  useContext,
} from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Loading,
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
  TableToolbar,
  TableToolbarContent,
  TableBatchActions,
  TableBatchAction,
  Tag,
  TextArea,
  RadioButtonGroup,
  RadioButton,
  Toggle,
  Select,
  SelectItem,
  Accordion,
  AccordionItem,
  Checkbox,
  NumberInput,
} from "@carbon/react";
import {
  Checkmark,
  Close,
  CheckmarkFilled,
  CloseFilled,
  WarningAlt,
  View,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
import "../../workflow/NotebookWorkflow.css";

/**
 * QualityCheckPage - Sample Receipt & Quality Assessment
 *
 * Purpose: Accept or reject samples based on defined quality rules.
 * Who uses it: Reception staff, Lab supervisor
 *
 * Features:
 * - General QC Checks (labeling, container, matching order, temperature)
 * - Discipline-Specific QC (Chemistry, Hematology, Stool, Urine, Microbiology)
 * - Acceptance Decision (Accept/Reject)
 * - Rejection handling with reasons and corrective actions
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function MedLabQualityCheckPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  // State for samples
  const [pendingSamples, setPendingSamples] = useState([]);
  const [processedSamples, setProcessedSamples] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // QC Assessment modal state
  const [qcModalOpen, setQcModalOpen] = useState(false);
  const [selectedSample, setSelectedSample] = useState(null);
  const [selectedRows, setSelectedRows] = useState([]);

  // QC Form state - General Checks
  const [qcForm, setQcForm] = useState({
    // General QC Checks
    labelingCorrect: true,
    correctContainerForTest: true,
    matchingLabOrder: true,
    storageTemperature: "room_temp", // room_temp, 2_8c, frozen, na

    // Discipline-Specific QC (Chemistry)
    chemistryDelay: false, // delay > 1hr
    chemistryVolume: true, // volume >= 3ml
    hemolysis: "none", // none, slight, moderate, gross
    lipemia: "none", // none, slight, moderate, gross
    icterus: "none", // none, slight, moderate, gross

    // Discipline-Specific QC (Hematology)
    anticoagulantType: "edta", // edta, citrate, heparin, other
    hematologyDelay: false, // delay > 4hr
    clotting: false,
    hematologyHemolysis: false,

    // Discipline-Specific QC (Stool)
    stoolDelay: false, // delay > 30min
    stoolVolume: true,
    stoolContamination: false,

    // Discipline-Specific QC (Urine)
    urineDelay: false, // delay > 30min
    urineVolume: true, // volume >= 10ml
    urineContamination: false,

    // Discipline-Specific QC (Microbiology)
    microbiologyContamination: false,

    // Decision
    decision: "accepted", // accepted, rejected
    rejectionReason: "",
    correctiveAction: "none", // none, recollection, return_to_submitter
    notes: "",
  });

  // Bulk QC modal state
  const [bulkQcModalOpen, setBulkQcModalOpen] = useState(false);
  const [bulkQcAction, setBulkQcAction] = useState("accept");
  const [bulkRejectionReason, setBulkRejectionReason] = useState("");
  const [bulkCorrectiveAction, setBulkCorrectiveAction] = useState("none");

  // Discipline options
  const disciplines = [
    { id: "chemistry", label: "Chemistry" },
    { id: "hematology", label: "Hematology" },
    { id: "stool", label: "Stool" },
    { id: "urine", label: "Urine" },
    { id: "microbiology", label: "Microbiology" },
  ];

  // Storage temperature options
  const temperatureOptions = [
    { id: "room_temp", label: "Room Temperature" },
    { id: "2_8c", label: "2-8°C (Refrigerated)" },
    { id: "frozen", label: "Frozen" },
    { id: "na", label: "N/A" },
  ];

  // Hemolysis/Lipemia/Icterus levels
  const serumQualityLevels = [
    { id: "none", label: "None" },
    { id: "slight", label: "Slight (+)" },
    { id: "moderate", label: "Moderate (++)" },
    { id: "gross", label: "Gross (+++)" },
  ];

  // Anticoagulant types
  const anticoagulantTypes = [
    { id: "edta", label: "EDTA" },
    { id: "citrate", label: "Citrate" },
    { id: "heparin", label: "Heparin" },
    { id: "other", label: "Other" },
  ];

  // Corrective action options
  const correctiveActionOptions = [
    { id: "none", label: "None Required" },
    { id: "recollection", label: "Recollection Needed" },
    { id: "return_to_submitter", label: "Return to Submitter" },
  ];

  // Rejection reason options
  const rejectionReasons = [
    { id: "hemolysis", label: "Hemolysis" },
    { id: "lipemia", label: "Lipemia" },
    { id: "icterus", label: "Icterus" },
    { id: "clotted", label: "Clotted Sample" },
    { id: "insufficient_volume", label: "Insufficient Volume" },
    { id: "wrong_container", label: "Wrong Container" },
    { id: "labeling_error", label: "Labeling Error" },
    { id: "delayed", label: "Excessive Delay" },
    { id: "contamination", label: "Contamination" },
    { id: "wrong_temperature", label: "Wrong Storage Temperature" },
    { id: "order_mismatch", label: "Order Mismatch" },
    { id: "other", label: "Other" },
  ];

  // Determine sample discipline based on sample type
  const getSampleDiscipline = (sampleType) => {
    if (!sampleType) return "chemistry";
    const type = sampleType.toLowerCase();
    if (type.includes("stool") || type.includes("feces")) return "stool";
    if (type.includes("urine")) return "urine";
    if (
      type.includes("swab") ||
      type.includes("culture") ||
      type.includes("wound")
    )
      return "microbiology";
    if (type.includes("edta") || type.includes("cbc") || type.includes("blood"))
      return "hematology";
    return "chemistry";
  };

  // Check if QC form has any issues (auto-suggest rejection)
  const hasQcIssues = useCallback(() => {
    // General checks
    if (!qcForm.labelingCorrect) return true;
    if (!qcForm.correctContainerForTest) return true;
    if (!qcForm.matchingLabOrder) return true;

    // Chemistry issues
    if (qcForm.chemistryDelay) return true;
    if (!qcForm.chemistryVolume) return true;
    if (qcForm.hemolysis !== "none" && qcForm.hemolysis !== "slight")
      return true;
    if (qcForm.lipemia !== "none" && qcForm.lipemia !== "slight") return true;
    if (qcForm.icterus !== "none" && qcForm.icterus !== "slight") return true;

    // Hematology issues
    if (qcForm.hematologyDelay) return true;
    if (qcForm.clotting) return true;
    if (qcForm.hematologyHemolysis) return true;

    // Stool issues
    if (qcForm.stoolDelay) return true;
    if (!qcForm.stoolVolume) return true;
    if (qcForm.stoolContamination) return true;

    // Urine issues
    if (qcForm.urineDelay) return true;
    if (!qcForm.urineVolume) return true;
    if (qcForm.urineContamination) return true;

    // Microbiology issues
    if (qcForm.microbiologyContamination) return true;

    return false;
  }, [qcForm]);

  // Load samples for QC
  const loadSamplesForQC = useCallback(() => {
    if (!entryId) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    const url = `/rest/medlab/entry/${entryId}/samples-for-qc`;
    getFromOpenElisServer(url, (response) => {
      if (componentMounted.current) {
        if (response && Array.isArray(response)) {
          const pending = response.filter((s) => s.qcStatus === "PENDING_QC");
          const processed = response.filter(
            (s) =>
              s.qcStatus === "ACCEPTED" ||
              s.qcStatus === "REJECTED" ||
              s.isRejected,
          );
          setPendingSamples(pending);
          setProcessedSamples(processed);
        } else {
          setPendingSamples([]);
          setProcessedSamples([]);
        }
        setLoading(false);
      }
    });
  }, [entryId]);

  // Load data on mount
  useEffect(() => {
    componentMounted.current = true;
    loadSamplesForQC();
    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id, loadSamplesForQC]);

  // Open QC assessment modal
  const handleOpenQcModal = useCallback((sample) => {
    setSelectedSample(sample);
    const discipline = getSampleDiscipline(sample?.sampleType);

    // Reset form with defaults
    setQcForm({
      labelingCorrect: true,
      correctContainerForTest: true,
      matchingLabOrder: true,
      storageTemperature: "room_temp",
      chemistryDelay: false,
      chemistryVolume: true,
      hemolysis: "none",
      lipemia: "none",
      icterus: "none",
      anticoagulantType: "edta",
      hematologyDelay: false,
      clotting: false,
      hematologyHemolysis: false,
      stoolDelay: false,
      stoolVolume: true,
      stoolContamination: false,
      urineDelay: false,
      urineVolume: true,
      urineContamination: false,
      microbiologyContamination: false,
      decision: "accepted",
      rejectionReason: "",
      correctiveAction: "none",
      notes: "",
      discipline: discipline,
    });
    setQcModalOpen(true);
  }, []);

  // Submit QC assessment
  const handleSubmitQcAssessment = useCallback(() => {
    if (!selectedSample) return;

    const qcData = {
      labNo: selectedSample.labNo,
      accepted: qcForm.decision === "accepted",
      rejectionReason:
        qcForm.decision === "rejected" ? qcForm.rejectionReason : null,
      correctiveAction:
        qcForm.decision === "rejected" ? qcForm.correctiveAction : null,
      notebookPageId: pageData?.id,
      // Include all QC check data
      qcChecks: {
        labelingCorrect: qcForm.labelingCorrect,
        correctContainerForTest: qcForm.correctContainerForTest,
        matchingLabOrder: qcForm.matchingLabOrder,
        storageTemperature: qcForm.storageTemperature,
        hemolysis: qcForm.hemolysis,
        lipemia: qcForm.lipemia,
        icterus: qcForm.icterus,
        clotting: qcForm.clotting,
        delay:
          qcForm.chemistryDelay ||
          qcForm.hematologyDelay ||
          qcForm.stoolDelay ||
          qcForm.urineDelay,
        volumeAdequate:
          qcForm.chemistryVolume && qcForm.stoolVolume && qcForm.urineVolume,
        contamination:
          qcForm.stoolContamination ||
          qcForm.urineContamination ||
          qcForm.microbiologyContamination,
      },
      notes: qcForm.notes,
    };

    postToOpenElisServer(
      "/rest/medlab/qc-decision",
      JSON.stringify(qcData),
      (status) => {
        if (status === 200) {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id:
                qcForm.decision === "accepted"
                  ? "medlab.qc.accept.success"
                  : "medlab.qc.reject.success",
              defaultMessage:
                qcForm.decision === "accepted"
                  ? "Sample accepted"
                  : "Sample rejected",
            }),
            kind:
              qcForm.decision === "accepted"
                ? NotificationKinds.success
                : NotificationKinds.warning,
          });
          setNotificationVisible(true);
          setQcModalOpen(false);
          loadSamplesForQC();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "medlab.qc.error",
              defaultMessage: "Error processing QC decision",
            }),
            kind: NotificationKinds.error,
          });
          setNotificationVisible(true);
        }
      },
    );
  }, [
    selectedSample,
    qcForm,
    pageData,
    intl,
    addNotification,
    setNotificationVisible,
    loadSamplesForQC,
    onProgressUpdate,
  ]);

  // Quick accept sample (bypass detailed form)
  const handleQuickAccept = useCallback(
    (sample) => {
      const qcData = {
        labNo: sample.labNo,
        accepted: true,
        notebookPageId: pageData?.id,
      };

      postToOpenElisServer(
        "/rest/medlab/qc-decision",
        JSON.stringify(qcData),
        (status) => {
          if (status === 200) {
            addNotification({
              title: intl.formatMessage({ id: "notification.title" }),
              message: intl.formatMessage({
                id: "medlab.qc.accept.success",
                defaultMessage: "Sample accepted",
              }),
              kind: NotificationKinds.success,
            });
            setNotificationVisible(true);
            loadSamplesForQC();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            addNotification({
              title: intl.formatMessage({ id: "notification.title" }),
              message: intl.formatMessage({
                id: "medlab.qc.accept.error",
                defaultMessage: "Error accepting sample",
              }),
              kind: NotificationKinds.error,
            });
            setNotificationVisible(true);
          }
        },
      );
    },
    [
      pageData,
      intl,
      addNotification,
      setNotificationVisible,
      loadSamplesForQC,
      onProgressUpdate,
    ],
  );

  // Open bulk QC modal
  const handleOpenBulkQcModal = useCallback((action, selectedRowIds) => {
    setSelectedRows(selectedRowIds);
    setBulkQcAction(action);
    setBulkRejectionReason("");
    setBulkCorrectiveAction("none");
    setBulkQcModalOpen(true);
  }, []);

  // Submit bulk QC decisions
  const handleSubmitBulkQc = useCallback(() => {
    if (selectedRows.length === 0) return;

    const selectedLabNos = selectedRows
      .map((rowId) => {
        const sample = pendingSamples.find((s) => String(s.id) === rowId);
        return sample?.labNo;
      })
      .filter(Boolean);

    if (selectedLabNos.length === 0) return;

    const bulkQcData = {
      labNumbers: selectedLabNos,
      accepted: bulkQcAction === "accept",
      rejectionReason: bulkQcAction === "reject" ? bulkRejectionReason : null,
      correctiveAction: bulkQcAction === "reject" ? bulkCorrectiveAction : null,
      notebookPageId: pageData?.id,
    };

    postToOpenElisServer(
      "/rest/medlab/bulk-qc-decision",
      JSON.stringify(bulkQcData),
      (status) => {
        if (status === 200) {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage(
              {
                id:
                  bulkQcAction === "accept"
                    ? "medlab.qc.bulk.accept.success"
                    : "medlab.qc.bulk.reject.success",
                defaultMessage:
                  bulkQcAction === "accept"
                    ? "{count} samples accepted"
                    : "{count} samples rejected",
              },
              { count: selectedLabNos.length },
            ),
            kind: NotificationKinds.success,
          });
          setNotificationVisible(true);
          setBulkQcModalOpen(false);
          setSelectedRows([]);
          loadSamplesForQC();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "medlab.qc.bulk.error",
              defaultMessage: "Error processing bulk QC decision",
            }),
            kind: NotificationKinds.error,
          });
          setNotificationVisible(true);
        }
      },
    );
  }, [
    selectedRows,
    pendingSamples,
    bulkQcAction,
    bulkRejectionReason,
    bulkCorrectiveAction,
    pageData,
    intl,
    addNotification,
    setNotificationVisible,
    loadSamplesForQC,
    onProgressUpdate,
  ]);

  // Calculate stats
  const totalSamples = pendingSamples.length + processedSamples.length;
  const acceptedCount = processedSamples.filter((s) => !s.isRejected).length;
  const rejectedCount = processedSamples.filter((s) => s.isRejected).length;
  const pendingCount = pendingSamples.length;

  // Table headers for pending QC
  const pendingHeaders = [
    {
      key: "labNo",
      header: intl.formatMessage({ id: "sample.label.labnumber" }),
    },
    {
      key: "patientName",
      header: intl.formatMessage({ id: "patient.label" }),
    },
    {
      key: "sampleType",
      header: intl.formatMessage({
        id: "sample.sampleType",
        defaultMessage: "Sample Type",
      }),
    },
    {
      key: "collectionDate",
      header: intl.formatMessage({
        id: "medlab.collection.date",
        defaultMessage: "Collection Date",
      }),
    },
    {
      key: "actions",
      header: intl.formatMessage({ id: "label.button.actions" }),
    },
  ];

  // Table headers for processed samples
  const processedHeaders = [
    {
      key: "labNo",
      header: intl.formatMessage({ id: "sample.label.labnumber" }),
    },
    {
      key: "patientName",
      header: intl.formatMessage({ id: "patient.label" }),
    },
    {
      key: "sampleType",
      header: intl.formatMessage({
        id: "sample.sampleType",
        defaultMessage: "Sample Type",
      }),
    },
    {
      key: "qcStatus",
      header: intl.formatMessage({
        id: "medlab.qc.status",
        defaultMessage: "QC Status",
      }),
    },
    {
      key: "rejectionReason",
      header: intl.formatMessage({
        id: "medlab.qc.rejectionReason",
        defaultMessage: "Rejection Reason",
      }),
    },
  ];

  // Format date for display
  const formatDate = (dateStr) => {
    if (!dateStr) return "";
    try {
      const date = new Date(dateStr);
      return date.toLocaleDateString();
    } catch {
      return dateStr;
    }
  };

  // Render discipline-specific QC fields
  const renderDisciplineQcFields = () => {
    const discipline =
      qcForm.discipline || getSampleDiscipline(selectedSample?.sampleType);

    return (
      <AccordionItem
        title={intl.formatMessage({
          id: "medlab.qc.disciplineSpecific",
          defaultMessage: "Discipline-Specific QC",
        })}
        open
      >
        <Grid>
          {/* Chemistry QC */}
          {discipline === "chemistry" && (
            <>
              <Column lg={8} md={4} sm={4}>
                <Toggle
                  id="chemistry-delay"
                  labelText={intl.formatMessage({
                    id: "medlab.qc.chemistry.delay",
                    defaultMessage: "Delay > 1 hour?",
                  })}
                  labelA="No"
                  labelB="Yes"
                  toggled={qcForm.chemistryDelay}
                  onToggle={(checked) =>
                    setQcForm({ ...qcForm, chemistryDelay: checked })
                  }
                />
              </Column>
              <Column lg={8} md={4} sm={4}>
                <Toggle
                  id="chemistry-volume"
                  labelText={intl.formatMessage({
                    id: "medlab.qc.chemistry.volume",
                    defaultMessage: "Volume >= 3ml?",
                  })}
                  labelA="No"
                  labelB="Yes"
                  toggled={qcForm.chemistryVolume}
                  onToggle={(checked) =>
                    setQcForm({ ...qcForm, chemistryVolume: checked })
                  }
                />
              </Column>
              <Column lg={5} md={4} sm={4}>
                <Select
                  id="hemolysis"
                  labelText={intl.formatMessage({
                    id: "medlab.qc.hemolysis",
                    defaultMessage: "Hemolysis",
                  })}
                  value={qcForm.hemolysis}
                  onChange={(e) =>
                    setQcForm({ ...qcForm, hemolysis: e.target.value })
                  }
                >
                  {serumQualityLevels.map((level) => (
                    <SelectItem
                      key={level.id}
                      value={level.id}
                      text={level.label}
                    />
                  ))}
                </Select>
              </Column>
              <Column lg={5} md={4} sm={4}>
                <Select
                  id="lipemia"
                  labelText={intl.formatMessage({
                    id: "medlab.qc.lipemia",
                    defaultMessage: "Lipemia",
                  })}
                  value={qcForm.lipemia}
                  onChange={(e) =>
                    setQcForm({ ...qcForm, lipemia: e.target.value })
                  }
                >
                  {serumQualityLevels.map((level) => (
                    <SelectItem
                      key={level.id}
                      value={level.id}
                      text={level.label}
                    />
                  ))}
                </Select>
              </Column>
              <Column lg={6} md={4} sm={4}>
                <Select
                  id="icterus"
                  labelText={intl.formatMessage({
                    id: "medlab.qc.icterus",
                    defaultMessage: "Icterus",
                  })}
                  value={qcForm.icterus}
                  onChange={(e) =>
                    setQcForm({ ...qcForm, icterus: e.target.value })
                  }
                >
                  {serumQualityLevels.map((level) => (
                    <SelectItem
                      key={level.id}
                      value={level.id}
                      text={level.label}
                    />
                  ))}
                </Select>
              </Column>
            </>
          )}

          {/* Hematology QC */}
          {discipline === "hematology" && (
            <>
              <Column lg={8} md={4} sm={4}>
                <Select
                  id="anticoagulant-type"
                  labelText={intl.formatMessage({
                    id: "medlab.qc.anticoagulantType",
                    defaultMessage: "Anticoagulant Type",
                  })}
                  value={qcForm.anticoagulantType}
                  onChange={(e) =>
                    setQcForm({ ...qcForm, anticoagulantType: e.target.value })
                  }
                >
                  {anticoagulantTypes.map((type) => (
                    <SelectItem
                      key={type.id}
                      value={type.id}
                      text={type.label}
                    />
                  ))}
                </Select>
              </Column>
              <Column lg={8} md={4} sm={4}>
                <Toggle
                  id="hematology-delay"
                  labelText={intl.formatMessage({
                    id: "medlab.qc.hematology.delay",
                    defaultMessage: "Delay > 4 hours?",
                  })}
                  labelA="No"
                  labelB="Yes"
                  toggled={qcForm.hematologyDelay}
                  onToggle={(checked) =>
                    setQcForm({ ...qcForm, hematologyDelay: checked })
                  }
                />
              </Column>
              <Column lg={8} md={4} sm={4}>
                <Toggle
                  id="clotting"
                  labelText={intl.formatMessage({
                    id: "medlab.qc.clotting",
                    defaultMessage: "Clotting present?",
                  })}
                  labelA="No"
                  labelB="Yes"
                  toggled={qcForm.clotting}
                  onToggle={(checked) =>
                    setQcForm({ ...qcForm, clotting: checked })
                  }
                />
              </Column>
              <Column lg={8} md={4} sm={4}>
                <Toggle
                  id="hematology-hemolysis"
                  labelText={intl.formatMessage({
                    id: "medlab.qc.hemolysis",
                    defaultMessage: "Hemolysis present?",
                  })}
                  labelA="No"
                  labelB="Yes"
                  toggled={qcForm.hematologyHemolysis}
                  onToggle={(checked) =>
                    setQcForm({ ...qcForm, hematologyHemolysis: checked })
                  }
                />
              </Column>
            </>
          )}

          {/* Stool QC */}
          {discipline === "stool" && (
            <>
              <Column lg={8} md={4} sm={4}>
                <Toggle
                  id="stool-delay"
                  labelText={intl.formatMessage({
                    id: "medlab.qc.stool.delay",
                    defaultMessage: "Delay > 30 minutes?",
                  })}
                  labelA="No"
                  labelB="Yes"
                  toggled={qcForm.stoolDelay}
                  onToggle={(checked) =>
                    setQcForm({ ...qcForm, stoolDelay: checked })
                  }
                />
              </Column>
              <Column lg={8} md={4} sm={4}>
                <Toggle
                  id="stool-volume"
                  labelText={intl.formatMessage({
                    id: "medlab.qc.stool.volume",
                    defaultMessage: "Adequate volume?",
                  })}
                  labelA="No"
                  labelB="Yes"
                  toggled={qcForm.stoolVolume}
                  onToggle={(checked) =>
                    setQcForm({ ...qcForm, stoolVolume: checked })
                  }
                />
              </Column>
              <Column lg={8} md={4} sm={4}>
                <Toggle
                  id="stool-contamination"
                  labelText={intl.formatMessage({
                    id: "medlab.qc.stool.contamination",
                    defaultMessage: "Contamination?",
                  })}
                  labelA="No"
                  labelB="Yes"
                  toggled={qcForm.stoolContamination}
                  onToggle={(checked) =>
                    setQcForm({ ...qcForm, stoolContamination: checked })
                  }
                />
              </Column>
            </>
          )}

          {/* Urine QC */}
          {discipline === "urine" && (
            <>
              <Column lg={8} md={4} sm={4}>
                <Toggle
                  id="urine-delay"
                  labelText={intl.formatMessage({
                    id: "medlab.qc.urine.delay",
                    defaultMessage: "Delay > 30 minutes?",
                  })}
                  labelA="No"
                  labelB="Yes"
                  toggled={qcForm.urineDelay}
                  onToggle={(checked) =>
                    setQcForm({ ...qcForm, urineDelay: checked })
                  }
                />
              </Column>
              <Column lg={8} md={4} sm={4}>
                <Toggle
                  id="urine-volume"
                  labelText={intl.formatMessage({
                    id: "medlab.qc.urine.volume",
                    defaultMessage: "Volume >= 10ml?",
                  })}
                  labelA="No"
                  labelB="Yes"
                  toggled={qcForm.urineVolume}
                  onToggle={(checked) =>
                    setQcForm({ ...qcForm, urineVolume: checked })
                  }
                />
              </Column>
              <Column lg={8} md={4} sm={4}>
                <Toggle
                  id="urine-contamination"
                  labelText={intl.formatMessage({
                    id: "medlab.qc.urine.contamination",
                    defaultMessage: "Contamination?",
                  })}
                  labelA="No"
                  labelB="Yes"
                  toggled={qcForm.urineContamination}
                  onToggle={(checked) =>
                    setQcForm({ ...qcForm, urineContamination: checked })
                  }
                />
              </Column>
            </>
          )}

          {/* Microbiology QC */}
          {discipline === "microbiology" && (
            <Column lg={8} md={4} sm={4}>
              <Toggle
                id="microbiology-contamination"
                labelText={intl.formatMessage({
                  id: "medlab.qc.microbiology.contamination",
                  defaultMessage: "Visible contamination?",
                })}
                labelA="No"
                labelB="Yes"
                toggled={qcForm.microbiologyContamination}
                onToggle={(checked) =>
                  setQcForm({ ...qcForm, microbiologyContamination: checked })
                }
              />
            </Column>
          )}
        </Grid>
      </AccordionItem>
    );
  };

  return (
    <div className="quality-check-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="medlab.page.qualityCheck.title"
            defaultMessage="Sample Receipt & Quality Assessment"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="medlab.page.qualityCheck.description"
            defaultMessage="Accept or reject samples based on defined quality rules. Review labeling, container type, and discipline-specific criteria."
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
                  id="medlab.page.qualityCheck.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{totalSamples}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="medlab.page.qualityCheck.accepted"
                  defaultMessage="Accepted"
                />
              </span>
              <span className="progress-value">{acceptedCount}</span>
            </Tile>
            <Tile className="progress-tile rejected">
              <span className="progress-label">
                <FormattedMessage
                  id="medlab.page.qualityCheck.rejected"
                  defaultMessage="Rejected"
                />
              </span>
              <span className="progress-value">{rejectedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="medlab.page.qualityCheck.pending"
                  defaultMessage="Pending QC"
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
          hideCloseButton
          lowContrast
        />
      )}

      {/* Loading State */}
      {loading && <Loading />}

      {/* Pending QC Table */}
      {!loading && pendingSamples.length > 0 && (
        <div className="orders-section">
          <h5>
            <FormattedMessage
              id="medlab.page.qualityCheck.pendingSamples"
              defaultMessage="Samples Pending QC"
            />
          </h5>
          <DataTable
            rows={pendingSamples.map((s) => ({
              ...s,
              id: String(s.id),
              collectionDate: formatDate(s.collectionDate),
            }))}
            headers={pendingHeaders}
            isSortable
          >
            {({
              rows,
              headers,
              getHeaderProps,
              getTableProps,
              getSelectionProps,
              getRowProps,
              selectedRows,
              getBatchActionProps,
            }) => {
              const batchActionProps = getBatchActionProps();
              return (
                <TableContainer>
                  <TableToolbar>
                    <TableBatchActions {...batchActionProps}>
                      <TableBatchAction
                        tabIndex={
                          batchActionProps.shouldShowBatchActions ? 0 : -1
                        }
                        renderIcon={CheckmarkFilled}
                        onClick={() =>
                          handleOpenBulkQcModal(
                            "accept",
                            selectedRows.map((r) => r.id),
                          )
                        }
                      >
                        <FormattedMessage
                          id="medlab.qc.bulk.accept"
                          defaultMessage="Accept Selected"
                        />
                      </TableBatchAction>
                      <TableBatchAction
                        tabIndex={
                          batchActionProps.shouldShowBatchActions ? 0 : -1
                        }
                        renderIcon={CloseFilled}
                        onClick={() =>
                          handleOpenBulkQcModal(
                            "reject",
                            selectedRows.map((r) => r.id),
                          )
                        }
                      >
                        <FormattedMessage
                          id="medlab.qc.bulk.reject"
                          defaultMessage="Reject Selected"
                        />
                      </TableBatchAction>
                    </TableBatchActions>
                    <TableToolbarContent>
                      <p style={{ fontSize: "0.875rem", color: "#525252" }}>
                        <FormattedMessage
                          id="medlab.qc.bulk.hint"
                          defaultMessage="Select samples for bulk accept/reject, or click Assess for detailed QC"
                        />
                      </p>
                    </TableToolbarContent>
                  </TableToolbar>
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
                        const sample = pendingSamples.find(
                          (s) => String(s.id) === row.id,
                        );
                        return (
                          <TableRow key={row.id} {...getRowProps({ row })}>
                            <TableSelectRow {...getSelectionProps({ row })} />
                            {row.cells.map((cell) => (
                              <TableCell key={cell.id}>
                                {cell.info.header === "actions" ? (
                                  <div
                                    style={{ display: "flex", gap: "0.5rem" }}
                                  >
                                    <Button
                                      kind="ghost"
                                      size="sm"
                                      renderIcon={View}
                                      onClick={() => handleOpenQcModal(sample)}
                                    >
                                      <FormattedMessage
                                        id="medlab.qc.assess"
                                        defaultMessage="Assess"
                                      />
                                    </Button>
                                    <Button
                                      kind="primary"
                                      size="sm"
                                      renderIcon={Checkmark}
                                      onClick={() => handleQuickAccept(sample)}
                                    >
                                      <FormattedMessage
                                        id="medlab.qc.accept"
                                        defaultMessage="Accept"
                                      />
                                    </Button>
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
              );
            }}
          </DataTable>
        </div>
      )}

      {/* Processed Samples Table */}
      {!loading && processedSamples.length > 0 && (
        <div className="orders-section" style={{ marginTop: "2rem" }}>
          <h5>
            <FormattedMessage
              id="medlab.page.qualityCheck.processedSamples"
              defaultMessage="Processed Samples"
            />
          </h5>
          <DataTable
            rows={processedSamples.map((s) => ({
              ...s,
              id: String(s.id),
              qcStatus: s.isRejected ? "REJECTED" : "ACCEPTED",
              rejectionReason: s.rejectionReason || "-",
            }))}
            headers={processedHeaders}
            isSortable
          >
            {({ rows, headers, getHeaderProps, getTableProps }) => (
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
                    {rows.map((row) => (
                      <TableRow key={row.id}>
                        {row.cells.map((cell) => (
                          <TableCell key={cell.id}>
                            {cell.info.header === "qcStatus" ? (
                              <Tag
                                type={
                                  cell.value === "ACCEPTED" ? "green" : "red"
                                }
                              >
                                {cell.value === "ACCEPTED" ? (
                                  <FormattedMessage
                                    id="medlab.qc.status.accepted"
                                    defaultMessage="Accepted - Ready for Storage"
                                  />
                                ) : (
                                  <FormattedMessage
                                    id="medlab.qc.status.rejected"
                                    defaultMessage="Rejected"
                                  />
                                )}
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
          </DataTable>
        </div>
      )}

      {/* Empty state */}
      {!loading && totalSamples === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="medlab.page.qualityCheck.empty"
              defaultMessage="No samples available for quality check. Samples must pass Transport & Packaging verification first."
            />
          </p>
        </div>
      )}

      {/* QC Assessment Modal */}
      <Modal
        open={qcModalOpen}
        onRequestClose={() => setQcModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "medlab.page.qualityCheck.assessModal",
          defaultMessage: "Sample Quality Assessment",
        })}
        primaryButtonText={intl.formatMessage({
          id: "medlab.qc.submit",
          defaultMessage: "Submit Assessment",
        })}
        secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
        onRequestSubmit={handleSubmitQcAssessment}
        size="lg"
      >
        {selectedSample && (
          <Grid>
            {/* Sample Info */}
            <Column lg={16} md={8} sm={4}>
              <Tile
                className="order-info-tile"
                style={{ marginBottom: "1rem" }}
              >
                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    flexWrap: "wrap",
                  }}
                >
                  <div>
                    <strong>
                      <FormattedMessage id="sample.label.labnumber" />:
                    </strong>{" "}
                    {selectedSample.labNo}
                  </div>
                  <div>
                    <strong>
                      <FormattedMessage id="patient.label" />:
                    </strong>{" "}
                    {selectedSample.patientName}
                  </div>
                  <div>
                    <strong>
                      <FormattedMessage
                        id="sample.sampleType"
                        defaultMessage="Sample Type"
                      />
                      :
                    </strong>{" "}
                    <Tag type="blue">{selectedSample.sampleType}</Tag>
                  </div>
                </div>
              </Tile>
            </Column>

            {/* QC Issue Warning */}
            {hasQcIssues() && (
              <Column lg={16} md={8} sm={4}>
                <InlineNotification
                  kind="warning"
                  title={intl.formatMessage({
                    id: "medlab.qc.issuesDetected",
                    defaultMessage: "Quality issues detected",
                  })}
                  subtitle={intl.formatMessage({
                    id: "medlab.qc.reviewBeforeAccept",
                    defaultMessage:
                      "Review the issues below before accepting this sample.",
                  })}
                  lowContrast
                  hideCloseButton
                  style={{ marginBottom: "1rem" }}
                />
              </Column>
            )}

            <Column lg={16} md={8} sm={4}>
              <Accordion>
                {/* General QC Checks */}
                <AccordionItem
                  title={intl.formatMessage({
                    id: "medlab.qc.generalChecks",
                    defaultMessage: "General QC Checks",
                  })}
                  open
                >
                  <Grid>
                    <Column lg={8} md={4} sm={4}>
                      <Toggle
                        id="labeling-correct"
                        labelText={intl.formatMessage({
                          id: "medlab.qc.labelingCorrect",
                          defaultMessage: "Labeling Correct?",
                        })}
                        labelA="No"
                        labelB="Yes"
                        toggled={qcForm.labelingCorrect}
                        onToggle={(checked) =>
                          setQcForm({ ...qcForm, labelingCorrect: checked })
                        }
                      />
                    </Column>
                    <Column lg={8} md={4} sm={4}>
                      <Toggle
                        id="correct-container"
                        labelText={intl.formatMessage({
                          id: "medlab.qc.correctContainer",
                          defaultMessage: "Correct Container for Test?",
                        })}
                        labelA="No"
                        labelB="Yes"
                        toggled={qcForm.correctContainerForTest}
                        onToggle={(checked) =>
                          setQcForm({
                            ...qcForm,
                            correctContainerForTest: checked,
                          })
                        }
                      />
                    </Column>
                    <Column lg={8} md={4} sm={4}>
                      <Toggle
                        id="matching-order"
                        labelText={intl.formatMessage({
                          id: "medlab.qc.matchingOrder",
                          defaultMessage: "Matching Lab Order?",
                        })}
                        labelA="No"
                        labelB="Yes"
                        toggled={qcForm.matchingLabOrder}
                        onToggle={(checked) =>
                          setQcForm({ ...qcForm, matchingLabOrder: checked })
                        }
                      />
                    </Column>
                    <Column lg={8} md={4} sm={4}>
                      <Select
                        id="storage-temperature"
                        labelText={intl.formatMessage({
                          id: "medlab.qc.storageTemperature",
                          defaultMessage: "Storage Temperature at Collection",
                        })}
                        value={qcForm.storageTemperature}
                        onChange={(e) =>
                          setQcForm({
                            ...qcForm,
                            storageTemperature: e.target.value,
                          })
                        }
                      >
                        {temperatureOptions.map((temp) => (
                          <SelectItem
                            key={temp.id}
                            value={temp.id}
                            text={temp.label}
                          />
                        ))}
                      </Select>
                    </Column>
                  </Grid>
                </AccordionItem>

                {/* Discipline-Specific QC */}
                {renderDisciplineQcFields()}

                {/* Acceptance Decision */}
                <AccordionItem
                  title={intl.formatMessage({
                    id: "medlab.qc.decision",
                    defaultMessage: "Acceptance Decision",
                  })}
                  open
                >
                  <Grid>
                    <Column
                      lg={16}
                      md={8}
                      sm={4}
                      style={{ marginBottom: "1rem" }}
                    >
                      <RadioButtonGroup
                        legendText={intl.formatMessage({
                          id: "medlab.qc.decision.label",
                          defaultMessage: "Decision",
                        })}
                        name="qc-decision"
                        valueSelected={qcForm.decision}
                        onChange={(value) =>
                          setQcForm({ ...qcForm, decision: value })
                        }
                        orientation="horizontal"
                      >
                        <RadioButton
                          id="qc-accepted"
                          labelText={intl.formatMessage({
                            id: "medlab.qc.decision.accepted",
                            defaultMessage: "Accepted (A)",
                          })}
                          value="accepted"
                        />
                        <RadioButton
                          id="qc-rejected"
                          labelText={intl.formatMessage({
                            id: "medlab.qc.decision.rejected",
                            defaultMessage: "Rejected (R)",
                          })}
                          value="rejected"
                        />
                      </RadioButtonGroup>
                    </Column>

                    {qcForm.decision === "rejected" && (
                      <>
                        <Column lg={8} md={4} sm={4}>
                          <Select
                            id="rejection-reason"
                            labelText={intl.formatMessage({
                              id: "medlab.qc.rejectionReason",
                              defaultMessage: "Reason for Rejection",
                            })}
                            value={qcForm.rejectionReason}
                            onChange={(e) =>
                              setQcForm({
                                ...qcForm,
                                rejectionReason: e.target.value,
                              })
                            }
                          >
                            <SelectItem value="" text="Select reason..." />
                            {rejectionReasons.map((reason) => (
                              <SelectItem
                                key={reason.id}
                                value={reason.id}
                                text={reason.label}
                              />
                            ))}
                          </Select>
                        </Column>
                        <Column lg={8} md={4} sm={4}>
                          <Select
                            id="corrective-action"
                            labelText={intl.formatMessage({
                              id: "medlab.qc.correctiveAction",
                              defaultMessage: "Corrective Action",
                            })}
                            value={qcForm.correctiveAction}
                            onChange={(e) =>
                              setQcForm({
                                ...qcForm,
                                correctiveAction: e.target.value,
                              })
                            }
                          >
                            {correctiveActionOptions.map((action) => (
                              <SelectItem
                                key={action.id}
                                value={action.id}
                                text={action.label}
                              />
                            ))}
                          </Select>
                        </Column>
                      </>
                    )}

                    <Column lg={16} md={8} sm={4}>
                      <TextArea
                        id="qc-notes"
                        labelText={intl.formatMessage({
                          id: "medlab.qc.notes",
                          defaultMessage: "Additional Notes",
                        })}
                        value={qcForm.notes}
                        onChange={(e) =>
                          setQcForm({ ...qcForm, notes: e.target.value })
                        }
                        rows={2}
                        placeholder={intl.formatMessage({
                          id: "medlab.qc.notes.placeholder",
                          defaultMessage:
                            "Enter any additional observations...",
                        })}
                      />
                    </Column>
                  </Grid>
                </AccordionItem>
              </Accordion>
            </Column>
          </Grid>
        )}
      </Modal>

      {/* Bulk QC Modal */}
      <Modal
        open={bulkQcModalOpen}
        onRequestClose={() => setBulkQcModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "medlab.page.qualityCheck.bulkQcModal",
          defaultMessage: "Bulk QC Decision",
        })}
        primaryButtonText={intl.formatMessage({
          id:
            bulkQcAction === "accept"
              ? "medlab.qc.bulk.accept"
              : "medlab.qc.bulk.reject",
          defaultMessage:
            bulkQcAction === "accept" ? "Accept Selected" : "Reject Selected",
        })}
        secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
        onRequestSubmit={handleSubmitBulkQc}
        danger={bulkQcAction === "reject"}
        size="md"
      >
        <Grid>
          <Column lg={16} md={8} sm={4}>
            <Tile className="order-info-tile" style={{ marginBottom: "1rem" }}>
              <Tag type="blue">
                <FormattedMessage
                  id="medlab.qc.bulk.selectedCount"
                  defaultMessage="{count} samples selected"
                  values={{ count: selectedRows.length }}
                />
              </Tag>
            </Tile>
          </Column>
          <Column lg={16} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <RadioButtonGroup
              legendText={intl.formatMessage({
                id: "medlab.qc.bulk.action",
                defaultMessage: "QC Action",
              })}
              name="bulk-qc-action"
              valueSelected={bulkQcAction}
              onChange={(value) => setBulkQcAction(value)}
              orientation="horizontal"
            >
              <RadioButton
                id="bulk-qc-accept"
                labelText={intl.formatMessage({
                  id: "medlab.qc.accept",
                  defaultMessage: "Accept",
                })}
                value="accept"
              />
              <RadioButton
                id="bulk-qc-reject"
                labelText={intl.formatMessage({
                  id: "medlab.qc.reject",
                  defaultMessage: "Reject",
                })}
                value="reject"
              />
            </RadioButtonGroup>
          </Column>
          {bulkQcAction === "reject" && (
            <>
              <Column lg={8} md={4} sm={4}>
                <Select
                  id="bulk-rejection-reason"
                  labelText={intl.formatMessage({
                    id: "medlab.qc.rejectionReason",
                    defaultMessage: "Rejection Reason",
                  })}
                  value={bulkRejectionReason}
                  onChange={(e) => setBulkRejectionReason(e.target.value)}
                >
                  <SelectItem value="" text="Select reason..." />
                  {rejectionReasons.map((reason) => (
                    <SelectItem
                      key={reason.id}
                      value={reason.id}
                      text={reason.label}
                    />
                  ))}
                </Select>
              </Column>
              <Column lg={8} md={4} sm={4}>
                <Select
                  id="bulk-corrective-action"
                  labelText={intl.formatMessage({
                    id: "medlab.qc.correctiveAction",
                    defaultMessage: "Corrective Action",
                  })}
                  value={bulkCorrectiveAction}
                  onChange={(e) => setBulkCorrectiveAction(e.target.value)}
                >
                  {correctiveActionOptions.map((action) => (
                    <SelectItem
                      key={action.id}
                      value={action.id}
                      text={action.label}
                    />
                  ))}
                </Select>
              </Column>
            </>
          )}
        </Grid>
      </Modal>
    </div>
  );
}

export default MedLabQualityCheckPage;
