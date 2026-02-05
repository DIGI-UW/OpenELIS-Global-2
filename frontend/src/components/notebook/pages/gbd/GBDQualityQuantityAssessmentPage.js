import {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  useRef,
} from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Button,
  Modal,
  TextInput,
  NumberInput,
  DatePickerInput,
  Grid,
  Column,
  Tile,
  TextArea,
  Tag,
  Loading,
  InlineNotification,
} from "@carbon/react";
import { Renew, CheckmarkFilled, Chemistry } from "@carbon/react/icons";
import useGBDPermissions from "../../../../hooks/useGBDPermissions";
import { usePermissions } from "../../../../hooks/usePermissions";
import { NotificationContext } from "../../../layout/Layout";
import {
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
  getFromOpenElisServer,
} from "../../../utils/Utils";
import { NotificationKinds } from "../../../../components/common/CustomNotification";
import AccessDeniedMessage from "../../../common/AccessDeniedMessage";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * GBDQualityQuantityAssessmentPage - Page 3: Quality & Quantity Assessment
 *
 * Performs comprehensive QC assessment on extracted nucleic acids using TMMRD design pattern:
 * - Section-based layout (not tabs)
 * - Action buttons bar with Primary/Tertiary/Ghost buttons
 * - Progress tiles for workflow tracking
 * - Nanodrop: A260/280, A260/230 ratios, concentration
 * - Qubit: Concentration measurement
 * - Bioanalyzer: RNA Integrity Number (RIN), fragment size
 * - Auto-calculation of pass/fail with color-coded status
 *
 * Data stored in sample.data JSONB:
 * {
 *   qc: {
 *     nanodrop: { concentration, a260_280, a260_230, operator, dateTime },
 *     qubit: { concentration, operator, dateTime },
 *     bioanalyzer: { rin, fragmentSize, operator, dateTime },
 *     notes: "Overall QC assessment notes"
 *   }
 * }
 */
export const GBDQualityQuantityAssessmentPage = ({
  entryId,
  pageData = {},
  onProgressUpdate,
}) => {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const {
    getPagePermissionLevel,
    canSaveData,
    canPerformWork,
    hasFullControl,
    isReadOnly,
    canAccessQC,
    GBD_ROLES,
    GBD_PAGES,
  } = useGBDPermissions();

  // Page access check
  const canAccessPage = canAccessQC();

  // Get user's action-level permission for this page
  const pagePermissionLevel = getPagePermissionLevel(GBD_PAGES.QC);

  // Function-level permissions per permission matrix
  // Matrix: Lab Technicians (Yes), Bioinformaticians (No), Lab Manager (Full), Principal Investigator (View), Data Managers (No)
  const canPerformQC = canPerformWork(pagePermissionLevel); // Lab Technicians (Yes), Lab Manager (Full)
  const canModifyData = canSaveData(pagePermissionLevel);
  const canMarkComplete = canPerformWork(pagePermissionLevel);
  const isViewOnly = isReadOnly(pagePermissionLevel); // Principal Investigator (View)

  const componentMounted = useRef(false);
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);

  const [qcModalOpen, setQcModalOpen] = useState(false);
  const [isApplyingQC, setIsApplyingQC] = useState(false);
  const [isCompleting, setIsCompleting] = useState(false);

  // Form state for QC data
  const [nanodropConcentration, setNanodropConcentration] = useState("");
  const [a260_280, setA260_280] = useState("");
  const [a260_230, setA260_230] = useState("");
  const [qubitConcentration, setQubitConcentration] = useState("");
  const [rin, setRin] = useState("");
  const [fragmentSize, setFragmentSize] = useState("");
  const [operator, setOperator] = useState("");
  const [dateTime, setDateTime] = useState(
    new Date().toISOString().split("T")[0],
  );
  const [qcTime, setQcTime] = useState("09:00");
  const [notes, setNotes] = useState("");

  // QC Thresholds per GBD Implementation Guide - STAGE 3: Quality & Quantity Assessment
  // Based on Nanodrop, Qubit, and Bioanalyzer instruments
  const QC_THRESHOLDS = {
    dnaConcentration: { min: 10 }, // ng/µL - minimum DNA concentration
    dnaA260_280: { min: 1.8, max: 2.0 }, // DNA acceptable purity range
    rnaA260_280: { min: 2.0, max: 2.2 }, // RNA acceptable purity range
    a260_230: { min: 1.8 }, // minimum purity ratio (applies to both DNA and RNA)
    rnaRin: { min: 7 }, // minimum RNA Integrity Number
  };

  // Calculate QC status (PASS/FAIL) based on recorded measurements
  const calculateQCStatus = useCallback((qcData, sampleType) => {
    if (!qcData) return null;

    const { nanodrop, qubit, bioanalyzer } = qcData;
    const issues = [];
    const isSampleRNA = sampleType && sampleType.toUpperCase().includes("RNA");

    // Nanodrop purity checks
    if (nanodrop) {
      const conc = parseFloat(nanodrop.concentration);
      if (!isNaN(conc) && conc < QC_THRESHOLDS.dnaConcentration.min) {
        issues.push("Low concentration");
      }

      const a280 = parseFloat(nanodrop.a260_280);
      if (!isNaN(a280)) {
        // Apply different thresholds based on sample type
        const threshold = isSampleRNA
          ? QC_THRESHOLDS.rnaA260_280
          : QC_THRESHOLDS.dnaA260_280;

        if (a280 < threshold.min || a280 > threshold.max) {
          issues.push(
            `A260/280 out of range (${isSampleRNA ? "RNA" : "DNA"}: ${threshold.min}-${threshold.max})`,
          );
        }
      }

      const a230 = parseFloat(nanodrop.a260_230);
      if (!isNaN(a230) && a230 < QC_THRESHOLDS.a260_230.min) {
        issues.push("A260/230 below threshold");
      }
    }

    // Bioanalyzer RIN check for RNA
    if (bioanalyzer) {
      const rinValue = parseFloat(bioanalyzer.rin);
      if (!isNaN(rinValue) && rinValue < QC_THRESHOLDS.rnaRin.min) {
        issues.push("RIN below threshold");
      }
    }

    return issues.length === 0 ? "PASS" : "FAIL";
  }, []);

  // Notification callback
  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, message }) => {
      setNotificationVisible(true);
      addNotification({ kind, title, message });
    },
    [addNotification, setNotificationVisible],
  );

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

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
          let samplesToProcess = [];

          // Handle both array and object responses from API
          if (response) {
            if (Array.isArray(response)) {
              samplesToProcess = response;
            } else if (response.samples && Array.isArray(response.samples)) {
              samplesToProcess = response.samples;
            }
          }

          setSamples(
            samplesToProcess.length > 0
              ? samplesToProcess.map((s) => {
                  const qcData = s.data?.qc;
                  return {
                    id: String(s.id || s.sampleItemId),
                    externalId: s.externalId,
                    accessionNumber: s.accessionNumber,
                    status: s.pageStatus || s.status || "PENDING",
                    sampleType: s.data?.sampleType,
                    collectionDate: s.data?.collectionDate,
                    source: s.data?.source,
                    // Nanodrop measurements
                    nanodropConcentration: s.data?.qc?.nanodrop?.concentration,
                    a260_280: s.data?.qc?.nanodrop?.a260_280,
                    a260_230: s.data?.qc?.nanodrop?.a260_230,
                    // Qubit measurements
                    qubitConcentration: s.data?.qc?.qubit?.concentration,
                    // Bioanalyzer measurements
                    rin: s.data?.qc?.bioanalyzer?.rin,
                    fragmentSize: s.data?.qc?.bioanalyzer?.fragmentSize,
                    // QC metadata
                    qcOperator:
                      s.data?.qc?.nanodrop?.operator ||
                      s.data?.qc?.qubit?.operator ||
                      s.data?.qc?.bioanalyzer?.operator,
                    qcDateTime:
                      s.data?.qc?.nanodrop?.dateTime ||
                      s.data?.qc?.qubit?.dateTime ||
                      s.data?.qc?.bioanalyzer?.dateTime,
                    qcNotes: s.data?.qc?.notes,
                    // Calculated QC status
                    qcStatus: calculateQCStatus(qcData, s.data?.sampleType),
                  };
                })
              : [],
          );
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id, loadPageSamples]);

  const resetForm = useCallback(() => {
    setNanodropConcentration("");
    setA260_280("");
    setA260_230("");
    setQubitConcentration("");
    setRin("");
    setFragmentSize("");
    setOperator("");
    setDateTime(new Date().toISOString().split("T")[0]);
    setQcTime("09:00");
    setNotes("");
  }, []);

  const openModal = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.gbd.qc.error.noSample",
          defaultMessage: "Please select at least one sample.",
        }),
      });
      return;
    }
    resetForm();
    setQcModalOpen(true);
  }, [selectedSampleIds, intl, resetForm, notify]);

  const applyQC = useCallback(() => {
    // Basic validation - at least one QC measurement required
    if (
      !nanodropConcentration &&
      !qubitConcentration &&
      !rin &&
      !fragmentSize
    ) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.gbd.qc.error.requiredFields",
          defaultMessage: "Required Field Missing",
        }),
        message: intl.formatMessage({
          id: "notebook.gbd.qc.error.atLeastOneQC",
          defaultMessage:
            "At least one QC measurement (Nanodrop, Qubit, or Bioanalyzer) is required",
        }),
      });
      return;
    }

    if (!hasRealPageId) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.gbd.qc.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      });
      return;
    }

    setIsApplyingQC(true);

    const sampleIds = selectedSampleIds.map((id) => parseInt(id, 10));

    // Build QC data object only with provided values
    const qcData = {
      notes,
    };

    if (nanodropConcentration || a260_280 || a260_230) {
      qcData.nanodrop = {
        concentration: nanodropConcentration
          ? parseFloat(nanodropConcentration)
          : null,
        a260_280: a260_280 ? parseFloat(a260_280) : null,
        a260_230: a260_230 ? parseFloat(a260_230) : null,
        operator,
        dateTime: dateTime ? `${dateTime}T${qcTime}:00Z` : null,
      };
    }

    if (qubitConcentration) {
      qcData.qubit = {
        concentration: parseFloat(qubitConcentration),
        operator,
        dateTime: dateTime ? `${dateTime}T00:00:00Z` : null,
      };
    }

    if (rin || fragmentSize) {
      qcData.bioanalyzer = {
        rin: rin ? parseFloat(rin) : null,
        fragmentSize: fragmentSize ? parseFloat(fragmentSize) : null,
        operator,
        dateTime: dateTime ? `${dateTime}T00:00:00Z` : null,
      };
    }

    // Use the bulk apply endpoint to save QC data
    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds,
        data: {
          qc: qcData,
        },
      }),
      (response) => {
        setIsApplyingQC(false);
        if (response?.success) {
          // Update sample status to IN_PROGRESS after QC recording
          postToOpenElisServer(
            `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
            JSON.stringify({
              sampleIds,
              status: "IN_PROGRESS",
            }),
            (statusCode) => {
              if (statusCode === 200) {
                notify({
                  kind: NotificationKinds.success,
                  title: intl.formatMessage(
                    {
                      id: "notebook.gbd.qc.success",
                      defaultMessage:
                        "QC assessment recorded for {count} sample(s).",
                    },
                    {
                      count: response.updatedCount || selectedSampleIds.length,
                    },
                  ),
                });
                setQcModalOpen(false);
                setSelectedSampleIds([]);
                loadPageSamples();
                if (onProgressUpdate) onProgressUpdate();
              } else {
                notify({
                  kind: NotificationKinds.error,
                  title: intl.formatMessage({
                    id: "notebook.gbd.qc.error.statusUpdate",
                    defaultMessage:
                      "QC recorded but failed to update sample status.",
                  }),
                });
              }
            },
          );
        } else {
          notify({
            kind: NotificationKinds.error,
            title: response?.error || "QC assessment failed",
          });
        }
      },
    );
  }, [
    nanodropConcentration,
    a260_280,
    a260_230,
    qubitConcentration,
    rin,
    fragmentSize,
    operator,
    dateTime,
    notes,
    hasRealPageId,
    pageData?.id,
    selectedSampleIds,
    intl,
    loadPageSamples,
    onProgressUpdate,
    notify,
  ]);

  const handleMarkComplete = useCallback(() => {
    const samplesToComplete = samples.filter(
      (s) => selectedSampleIds.includes(s.id) && s.status === "IN_PROGRESS",
    );

    if (samplesToComplete.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.gbd.qc.noEligibleSamples",
          defaultMessage:
            "Selected samples must have QC assessment recorded (status: In Progress) before completing.",
        }),
      });
      return;
    }

    setIsCompleting(true);

    const sampleIds = samplesToComplete.map((s) => parseInt(s.id, 10));

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({ sampleIds: sampleIds, status: "COMPLETED" }),
      (response) => {
        setIsCompleting(false);

        if (response && response.success) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage(
              {
                id: "notebook.gbd.qc.completeSuccess",
                defaultMessage:
                  "Successfully marked {count} samples as complete.",
              },
              { count: response.updatedCount || sampleIds.length },
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
            title:
              response?.error ||
              intl.formatMessage({
                id: "notebook.gbd.qc.completeFailed",
                defaultMessage: "Failed to mark samples complete.",
              }),
          });
        }
      },
    );
  }, [
    selectedSampleIds,
    samples,
    pageData?.id,
    intl,
    notify,
    loadPageSamples,
    onProgressUpdate,
  ]);

  const eligibleForCompletionCount = useMemo(
    () =>
      samples.filter(
        (s) => selectedSampleIds.includes(s.id) && s.status === "IN_PROGRESS",
      ).length,
    [samples, selectedSampleIds],
  );

  const readyForQCSamples = useMemo(
    () =>
      samples.filter(
        (s) =>
          s.status === "PENDING" ||
          s.status === "IN_PROGRESS" ||
          s.status === "AWAITING",
      ),
    [samples],
  );

  const qcCompletedSamples = useMemo(
    () => samples.filter((s) => s.status === "COMPLETED"),
    [samples],
  );

  if (!canAccessPage) {
    return (
      <AccessDeniedMessage
        page="Quality & Quantity Assessment"
        reason="This page requires specific GBD laboratory roles to access."
        requiredRoles={[
          GBD_ROLES.LAB_TECHNICIAN,
          GBD_ROLES.MANAGER,
          GBD_ROLES.PRINCIPAL_INVESTIGATOR,
        ]}
      />
    );
  }

  const renderStatus = (sample) => {
    const status = sample.status || "PENDING";

    switch (status.toUpperCase()) {
      case "COMPLETED":
        return (
          <Tag type="green" size="sm" renderIcon={CheckmarkFilled}>
            <FormattedMessage
              id="notebook.gbd.status.completed"
              defaultMessage="Completed"
            />
          </Tag>
        );
      case "IN_PROGRESS":
        return (
          <Tag type="blue" size="sm">
            <FormattedMessage
              id="notebook.gbd.status.inProgress"
              defaultMessage="In Progress"
            />
          </Tag>
        );
      default:
        return (
          <Tag type="gray" size="sm">
            <FormattedMessage
              id="notebook.gbd.status.pending"
              defaultMessage="Pending"
            />
          </Tag>
        );
    }
  };

  // Helper to render QC result status (PASS/FAIL)
  const renderQCStatus = (sample) => {
    const qcStatus = sample.qcStatus;

    if (!qcStatus) {
      return (
        <Tag type="gray" size="sm">
          <FormattedMessage
            id="notebook.gbd.qc.status.notRecorded"
            defaultMessage="Not Recorded"
          />
        </Tag>
      );
    }

    if (qcStatus === "PASS") {
      return (
        <Tag type="green" size="sm" renderIcon={CheckmarkFilled}>
          <FormattedMessage
            id="notebook.gbd.qc.status.pass"
            defaultMessage="PASS"
          />
        </Tag>
      );
    }

    return (
      <Tag type="red" size="sm">
        <FormattedMessage
          id="notebook.gbd.qc.status.fail"
          defaultMessage="FAIL"
        />
      </Tag>
    );
  };

  return (
    <div className="gbd-qc-assessment-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.gbd.qc.title"
            defaultMessage="Quality & Quantity Assessment"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.gbd.qc.description"
            defaultMessage="Perform comprehensive quality control assessment on extracted nucleic acids using Nanodrop, Qubit, and Bioanalyzer measurements."
          />
        </p>
      </div>

      {/* QC Criteria Information */}
      <InlineNotification
        kind="info"
        title={intl.formatMessage({
          id: "notebook.gbd.qc.criteria.title",
          defaultMessage: "QC Pass/Fail Criteria",
        })}
        subtitle={intl.formatMessage({
          id: "notebook.gbd.qc.criteria.explanation",
          defaultMessage:
            "A sample PASSES if all measurements meet thresholds: Nanodrop Conc ≥10 ng/µL | A260/280 (DNA: 1.8-2.0, RNA: 2.0-2.2) | A260/230 ≥1.8 | RIN ≥7 (RNA only). Any measurement below threshold results in FAIL.",
        })}
        lowContrast
        hideCloseButton
        style={{ marginBottom: "2rem" }}
      />

      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.gbd.qc.ready"
                  defaultMessage="Ready for QC"
                />
              </span>
              <span className="progress-value">{readyForQCSamples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.gbd.qc.assessed"
                  defaultMessage="QC Assessed"
                />
              </span>
              <span className="progress-value">
                {qcCompletedSamples.length}
              </span>
            </Tile>
          </div>
        </Column>
      </Grid>

      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Chemistry}
          onClick={openModal}
          disabled={
            selectedSampleIds.length === 0 ||
            !hasRealPageId ||
            !canPerformQC ||
            isViewOnly
          }
          title={
            !canPerformQC
              ? intl.formatMessage({
                  id: "notebook.gbd.qc.insufficientPermissions.record",
                  defaultMessage:
                    "Insufficient permissions to record QC data. Only Lab Technicians and Lab Manager (with appropriate permissions) can perform QC.",
                })
              : isViewOnly
                ? intl.formatMessage({
                    id: "notebook.gbd.qc.viewOnlyAccess",
                    defaultMessage: "You have view-only access to this page.",
                  })
                : undefined
          }
        >
          <FormattedMessage
            id="notebook.gbd.recordQC"
            defaultMessage="Record QC ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={CheckmarkFilled}
          onClick={handleMarkComplete}
          disabled={
            eligibleForCompletionCount === 0 ||
            isCompleting ||
            !hasRealPageId ||
            !canMarkComplete ||
            isViewOnly
          }
          title={
            !canMarkComplete
              ? intl.formatMessage({
                  id: "notebook.gbd.qc.insufficientPermissions.complete",
                  defaultMessage:
                    "Insufficient permissions to mark samples complete. Only users with work permissions can complete samples.",
                })
              : isViewOnly
                ? intl.formatMessage({
                    id: "notebook.gbd.qc.viewOnlyAccess",
                    defaultMessage: "You have view-only access to this page.",
                  })
                : undefined
          }
        >
          <FormattedMessage
            id="notebook.gbd.markComplete"
            defaultMessage="Mark Complete ({count})"
            values={{ count: eligibleForCompletionCount }}
          />
        </Button>

        <Button
          kind="ghost"
          size="sm"
          renderIcon={Renew}
          onClick={loadPageSamples}
          disabled={loading}
        >
          <FormattedMessage
            id="notebook.gbd.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.gbd.qc.ready.title"
              defaultMessage="Samples Ready for QC Assessment"
            />
            <Tag type="blue" size="sm" className="count-tag">
              {readyForQCSamples.length}
            </Tag>
          </h5>
        </div>
        <div className="sample-grid-container">
          {!loading && readyForQCSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.gbd.qc.ready.empty"
                  defaultMessage="No samples ready for QC assessment."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="gbd-qc-ready"
              samples={readyForQCSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              showSelection={canPerformQC}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "sampleType", header: "Sample Type" },
                { key: "collectionDate", header: "Collection Date" },
                { key: "source", header: "Source" },
                {
                  key: "qcOperator",
                  header: "QC Operator",
                  render: (_value, sample) => sample.qcOperator || "-",
                },
                {
                  key: "nanodropConcentration",
                  header: "Nanodrop\n(ng/µL)",
                  render: (_value, sample) =>
                    sample.nanodropConcentration || "-",
                },
                {
                  key: "a260_280",
                  header: "A260/280\nRatio",
                  render: (_value, sample) => sample.a260_280 || "-",
                },
                {
                  key: "a260_230",
                  header: "A260/230\nRatio",
                  render: (_value, sample) => sample.a260_230 || "-",
                },
                {
                  key: "qubitConcentration",
                  header: "Qubit\n(ng/µL)",
                  render: (_value, sample) => sample.qubitConcentration || "-",
                },
                {
                  key: "rin",
                  header: "RIN\n(RNA)",
                  render: (_value, sample) => sample.rin || "-",
                },
                {
                  key: "fragmentSize",
                  header: "Fragment\nSize (bp)",
                  render: (_value, sample) => sample.fragmentSize || "-",
                },
                {
                  key: "qcStatus",
                  header: intl.formatMessage({
                    id: "notebook.gbd.qc.column.qcStatus",
                    defaultMessage: "QC Result",
                  }),
                  render: (_value, sample) => renderQCStatus(sample),
                },
                {
                  key: "status",
                  header: intl.formatMessage({
                    id: "notebook.gbd.column.status",
                    defaultMessage: "Status",
                  }),
                  render: (_value, sample) => renderStatus(sample),
                },
              ]}
            />
          )}
        </div>
      </div>

      {/* QC Completed Section */}
      {qcCompletedSamples.length > 0 && (
        <div className="sample-table-section">
          <div className="table-section-header">
            <h5>
              <FormattedMessage
                id="notebook.gbd.qc.completed.title"
                defaultMessage="QC Assessment Completed"
              />
              <Tag type="green" size="sm" className="count-tag">
                {qcCompletedSamples.length}
              </Tag>
            </h5>
          </div>
          <div className="sample-grid-container">
            <SampleGrid
              gridId="gbd-qc-completed"
              samples={qcCompletedSamples}
              showSelection={false}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "sampleType", header: "Sample Type" },
                { key: "collectionDate", header: "Collection Date" },
                {
                  key: "nanodropConcentration",
                  header: "Nanodrop\n(ng/µL)",
                  render: (_value, sample) =>
                    sample.nanodropConcentration || "-",
                },
                {
                  key: "a260_280",
                  header: "A260/280\nRatio",
                  render: (_value, sample) => sample.a260_280 || "-",
                },
                {
                  key: "a260_230",
                  header: "A260/230\nRatio",
                  render: (_value, sample) => sample.a260_230 || "-",
                },
                {
                  key: "qubitConcentration",
                  header: "Qubit\n(ng/µL)",
                  render: (_value, sample) => sample.qubitConcentration || "-",
                },
                {
                  key: "rin",
                  header: "RIN\n(RNA)",
                  render: (_value, sample) => sample.rin || "-",
                },
                {
                  key: "fragmentSize",
                  header: "Fragment\nSize (bp)",
                  render: (_value, sample) => sample.fragmentSize || "-",
                },
                {
                  key: "qcStatus",
                  header: intl.formatMessage({
                    id: "notebook.gbd.qc.column.qcStatus",
                    defaultMessage: "QC Result",
                  }),
                  render: (_value, sample) => renderQCStatus(sample),
                },
                {
                  key: "status",
                  header: intl.formatMessage({
                    id: "notebook.gbd.column.status",
                    defaultMessage: "Page Status",
                  }),
                  render: (_value, sample) => renderStatus(sample),
                },
              ]}
            />
          </div>
        </div>
      )}

      <Modal
        open={qcModalOpen}
        onRequestClose={() => setQcModalOpen(false)}
        onRequestSubmit={applyQC}
        modalHeading={intl.formatMessage({
          id: "notebook.gbd.qc.modal.title",
          defaultMessage: "Record QC Assessment",
        })}
        primaryButtonText={
          isApplyingQC
            ? intl.formatMessage({
                id: "label.recording",
                defaultMessage: "Recording...",
              })
            : intl.formatMessage({
                id: "notebook.gbd.save",
                defaultMessage: "Save",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        primaryButtonDisabled={isApplyingQC || !canModifyData || isViewOnly}
        size="lg"
      >
        {isApplyingQC && <Loading withOverlay={false} small />}

        <Grid narrow>
          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1.5rem" }}>
            <div
              style={{
                padding: "0.75rem",
                backgroundColor: "#e8f4f8",
                borderRadius: "4px",
              }}
            >
              <p style={{ margin: 0, fontSize: "0.875rem", color: "#333" }}>
                <strong>
                  <FormattedMessage
                    id="notebook.gbd.qc.instructions"
                    defaultMessage="STAGE 3: Quality & Quantity Assessment"
                  />
                </strong>
                <br />
                <FormattedMessage
                  id="notebook.gbd.qc.description.detailed"
                  defaultMessage="Record measurements from three instruments: Nanodrop (concentration & purity), Qubit (fluorometric quantification), and Bioanalyzer (fragment distribution & RIN). Pass/Fail determination is based on threshold values: Concentration ≥10 ng/µL, A260/280 1.8-2.0, A260/230 ≥1.8, RIN ≥7."
                />
              </p>
            </div>
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <h6>
              <FormattedMessage
                id="notebook.gbd.qc.section.nanodrop"
                defaultMessage="Nanodrop Measurements"
              />
            </h6>
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <NumberInput
              id="nanodrop-concentration"
              label={intl.formatMessage({
                id: "notebook.gbd.qc.nanodrop.concentration",
                defaultMessage: "Concentration (ng/µL)",
              })}
              value={nanodropConcentration}
              onChange={(e) =>
                setNanodropConcentration(
                  e.imaginaryTarget?.value || e.target?.value || "",
                )
              }
              step={0.1}
              min={0}
              max={3000}
              placeholder="50"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <NumberInput
              id="a260-280"
              label={intl.formatMessage({
                id: "notebook.gbd.qc.a260.280",
                defaultMessage: "A260/280 Ratio",
              })}
              value={a260_280}
              onChange={(e) =>
                setA260_280(e.imaginaryTarget?.value || e.target?.value || "")
              }
              step={0.01}
              min={0.5}
              max={3.0}
              placeholder="1.9"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <NumberInput
              id="a260-230"
              label={intl.formatMessage({
                id: "notebook.gbd.qc.a260.230",
                defaultMessage: "A260/230 Ratio",
              })}
              value={a260_230}
              onChange={(e) =>
                setA260_230(e.imaginaryTarget?.value || e.target?.value || "")
              }
              step={0.01}
              min={0.5}
              max={3.0}
              placeholder="2.0"
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <h6>
              <FormattedMessage
                id="notebook.gbd.qc.section.qubit"
                defaultMessage="Qubit Measurements"
              />
            </h6>
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <NumberInput
              id="qubit-concentration"
              label={intl.formatMessage({
                id: "notebook.gbd.qc.qubit.concentration",
                defaultMessage: "Concentration (ng/µL)",
              })}
              value={qubitConcentration}
              onChange={(e) =>
                setQubitConcentration(
                  e.imaginaryTarget?.value || e.target?.value || "",
                )
              }
              step={0.1}
              min={0}
              max={1000}
              placeholder="25"
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <h6>
              <FormattedMessage
                id="notebook.gbd.qc.section.bioanalyzer"
                defaultMessage="Bioanalyzer Measurements"
              />
            </h6>
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <NumberInput
              id="rin"
              label={intl.formatMessage({
                id: "notebook.gbd.qc.bioanalyzer.rin",
                defaultMessage: "RIN (RNA Integrity Number)",
              })}
              value={rin}
              onChange={(e) =>
                setRin(e.imaginaryTarget?.value || e.target?.value || "")
              }
              step={0.1}
              min={0}
              max={10}
              placeholder="8"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <NumberInput
              id="fragment-size"
              label={intl.formatMessage({
                id: "notebook.gbd.qc.bioanalyzer.fragmentSize",
                defaultMessage: "Fragment Size (bp)",
              })}
              value={fragmentSize}
              onChange={(e) =>
                setFragmentSize(
                  e.imaginaryTarget?.value || e.target?.value || "",
                )
              }
              step={10}
              min={100}
              max={10000}
              placeholder="500"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="operator"
              labelText={intl.formatMessage({
                id: "notebook.gbd.qc.operator",
                defaultMessage: "QC Operator Name",
              })}
              value={operator}
              onChange={(e) => setOperator(e.target.value)}
              placeholder="Name of person performing QC"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <DatePickerInput
              id="qc-dateTime"
              labelText={intl.formatMessage({
                id: "notebook.gbd.qc.dateTime",
                defaultMessage: "QC Date",
              })}
              value={dateTime}
              onChange={(e) => setDateTime(e.target.value)}
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="qc-notes"
              labelText={intl.formatMessage({
                id: "notebook.gbd.qc.notes",
                defaultMessage: "QC Notes/Observations",
              })}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={2}
              placeholder="Additional QC assessment observations or concerns"
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
};

export default GBDQualityQuantityAssessmentPage;
