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
  Dropdown,
  DatePickerInput,
  Grid,
  Column,
  Tile,
  TextArea,
  Tag,
  Loading,
} from "@carbon/react";
import { Chemistry, Renew, CheckmarkFilled } from "@carbon/react/icons";
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
 * GBDDNARNAExtractionPage - Page 2: DNA/RNA Extraction
 *
 * Manages the extraction of nucleic acids from samples using TMMRD design pattern:
 * - Section-based layout (not tabs)
 * - Action buttons bar with Primary/Tertiary/Ghost buttons
 * - Progress tiles for workflow tracking
 * - Records extraction method/kit, lot numbers, operator, date, and notes
 * - Tracks sample progression to QC Assessment (Page 3)
 *
 * Data stored in sample.data JSONB:
 * {
 *   extraction: {
 *     methodKit: "Qiagen DNeasy|Trizol|Phenol/Chloroform|Other",
 *     lotNumbers: "LOT123",
 *     operator: "John Doe",
 *     dateTime: "2024-01-27",
 *     notes: "Extraction completed successfully"
 *   }
 * }
 */
export const GBDDNARNAExtractionPage = ({
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
    canAccessExtraction,
    GBD_ROLES,
    GBD_PAGES,
  } = useGBDPermissions();

  // Page access check
  const canAccessPage = canAccessExtraction();

  // Get user's action-level permission for this page
  const pagePermissionLevel = getPagePermissionLevel(GBD_PAGES.EXTRACTION);

  // Function-level permissions per permission matrix
  // Matrix: Lab Technicians (Yes), Bioinformaticians (No), Lab Manager (Full), Principal Investigator (View), Data Managers (No)
  const canPerformExtraction = canPerformWork(pagePermissionLevel); // Lab Technicians (Yes), Lab Manager (Full)
  const canModifyData = canSaveData(pagePermissionLevel);
  const canMarkComplete = canPerformWork(pagePermissionLevel);
  const isViewOnly = isReadOnly(pagePermissionLevel); // Principal Investigator (View)

  const componentMounted = useRef(false);
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);

  const [extractionModalOpen, setExtractionModalOpen] = useState(false);
  const [isApplyingExtraction, setIsApplyingExtraction] = useState(false);
  const [isCompleting, setIsCompleting] = useState(false);

  const [methodKit, setMethodKit] = useState(null);
  const [lotNumbers, setLotNumbers] = useState("");
  const [operator, setOperator] = useState("");
  const [dateTime, setDateTime] = useState(
    new Date().toISOString().split("T")[0],
  );
  const [extractionTime, setExtractionTime] = useState("09:00");
  const [notes, setNotes] = useState("");

  const methodKitOptions = [
    { id: "Qiagen DNeasy", label: "Qiagen DNeasy" },
    { id: "Trizol", label: "Trizol" },
    { id: "Phenol/Chloroform", label: "Phenol/Chloroform" },
    { id: "Other", label: "Other" },
  ];

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

          if (response) {
            if (Array.isArray(response)) {
              samplesToProcess = response;
            } else if (response.samples && Array.isArray(response.samples)) {
              samplesToProcess = response.samples;
            }
          }

          setSamples(
            samplesToProcess.length > 0
              ? samplesToProcess.map((s) => ({
                  id: String(s.id || s.sampleItemId),
                  externalId: s.externalId,
                  accessionNumber: s.accessionNumber,
                  status: s.pageStatus || s.status || "PENDING",
                  sampleType: s.data?.sampleType,
                  collectionDate: s.data?.collectionDate,
                  source: s.data?.source,
                  volumeConcentration: s.data?.volumeConcentration,
                  projectStudyAssociation: s.data?.projectStudyAssociation,
                  methodKit: s.data?.extraction?.methodKit,
                  lotNumbers: s.data?.extraction?.lotNumbers,
                  operator: s.data?.extraction?.operator,
                  dateTime: s.data?.extraction?.dateTime,
                  notes: s.data?.extraction?.notes,
                }))
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
    setMethodKit(null);
    setLotNumbers("");
    setOperator("");
    setDateTime(new Date().toISOString().split("T")[0]);
    setExtractionTime("09:00");
    setNotes("");
  }, []);

  const openModal = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.gbd.extraction.error.noSample",
          defaultMessage: "Please select at least one sample.",
        }),
      });
      return;
    }
    resetForm();
    setExtractionModalOpen(true);
  }, [selectedSampleIds, intl, resetForm, notify]);

  const applyExtraction = useCallback(() => {
    // Validation
    if (!methodKit) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.gbd.extraction.error.requiredFields",
          defaultMessage: "Required Field Missing",
        }),
        message: intl.formatMessage({
          id: "notebook.gbd.extraction.error.methodKitRequired",
          defaultMessage: "Extraction method/kit is required",
        }),
      });
      return;
    }

    if (!lotNumbers || lotNumbers.trim() === "") {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.gbd.extraction.error.requiredFields",
          defaultMessage: "Required Field Missing",
        }),
        message: intl.formatMessage({
          id: "notebook.gbd.extraction.error.lotNumbersRequired",
          defaultMessage: "Lot numbers are required for traceability",
        }),
      });
      return;
    }

    if (!operator || operator.trim() === "") {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.gbd.extraction.error.requiredFields",
          defaultMessage: "Required Field Missing",
        }),
        message: intl.formatMessage({
          id: "notebook.gbd.extraction.error.operatorRequired",
          defaultMessage: "Operator name is required",
        }),
      });
      return;
    }

    if (!hasRealPageId) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.gbd.extraction.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      });
      return;
    }

    setIsApplyingExtraction(true);

    const sampleIds = selectedSampleIds.map((id) => parseInt(id, 10));

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds,
        data: {
          extraction: {
            methodKit: methodKit.id,
            lotNumbers,
            operator,
            dateTime:
              dateTime && extractionTime
                ? `${dateTime}T${extractionTime}:00Z`
                : null,
            notes,
          },
        },
      }),
      (response) => {
        setIsApplyingExtraction(false);
        if (response?.success) {
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
                      id: "notebook.gbd.extraction.success",
                      defaultMessage: "Extracted {count} sample(s).",
                    },
                    {
                      count: response.updatedCount || selectedSampleIds.length,
                    },
                  ),
                });
                setExtractionModalOpen(false);
                setSelectedSampleIds([]);
                loadPageSamples();
                if (onProgressUpdate) onProgressUpdate();
              } else {
                notify({
                  kind: NotificationKinds.error,
                  title: intl.formatMessage({
                    id: "notebook.gbd.extraction.error.statusUpdate",
                    defaultMessage:
                      "Extraction recorded but failed to update sample status.",
                  }),
                });
              }
            },
          );
        } else {
          notify({
            kind: NotificationKinds.error,
            title: response?.error || "Extraction failed",
          });
        }
      },
    );
  }, [
    methodKit,
    lotNumbers,
    operator,
    dateTime,
    extractionTime,
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
          id: "notebook.gbd.extraction.noEligibleSamples",
          defaultMessage:
            "Selected samples must have extraction recorded (status: In Progress) before completing.",
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
                id: "notebook.gbd.extraction.completeSuccess",
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
                id: "notebook.gbd.extraction.completeFailed",
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

  const readyForExtractionSamples = useMemo(
    () =>
      samples.filter(
        (s) =>
          s.status === "PENDING" ||
          s.status === "IN_PROGRESS" ||
          s.status === "AWAITING",
      ),
    [samples],
  );

  const extractionCompletedSamples = useMemo(
    () => samples.filter((s) => s.status === "COMPLETED"),
    [samples],
  );

  const eligibleForCompletionCount = useMemo(
    () =>
      samples.filter(
        (s) => selectedSampleIds.includes(s.id) && s.status === "IN_PROGRESS",
      ).length,
    [samples, selectedSampleIds],
  );

  if (!canAccessPage) {
    return (
      <AccessDeniedMessage
        page="DNA/RNA Extraction"
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

  return (
    <div className="gbd-dna-rna-extraction-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.gbd.extraction.title"
            defaultMessage="DNA/RNA Extraction"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.gbd.extraction.description"
            defaultMessage="Record DNA/RNA extraction methods, operators, and procedures."
          />
        </p>
      </div>

      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.gbd.extraction.ready"
                  defaultMessage="Ready for Extraction"
                />
              </span>
              <span className="progress-value">
                {readyForExtractionSamples.length}
              </span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.gbd.extraction.extracted"
                  defaultMessage="Extracted"
                />
              </span>
              <span className="progress-value">
                {extractionCompletedSamples.length}
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
            !canPerformExtraction ||
            isViewOnly
          }
          title={
            !canPerformExtraction
              ? intl.formatMessage({
                  id: "notebook.gbd.extraction.insufficientPermissions.record",
                  defaultMessage:
                    "Insufficient permissions to record extraction. Only Lab Technicians and Lab Manager (with appropriate permissions) can perform extractions.",
                })
              : isViewOnly
                ? intl.formatMessage({
                    id: "notebook.gbd.extraction.viewOnlyAccess",
                    defaultMessage: "You have view-only access to this page.",
                  })
                : undefined
          }
        >
          <FormattedMessage
            id="notebook.gbd.recordExtraction"
            defaultMessage="Record Extraction ({count})"
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
                  id: "notebook.gbd.extraction.insufficientPermissions.complete",
                  defaultMessage:
                    "Insufficient permissions to mark samples complete. Only users with work permissions can complete samples.",
                })
              : isViewOnly
                ? intl.formatMessage({
                    id: "notebook.gbd.extraction.viewOnlyAccess",
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
              id="notebook.gbd.extraction.ready.title"
              defaultMessage="Samples Ready for Extraction"
            />
            <Tag type="blue" size="sm" className="count-tag">
              {readyForExtractionSamples.length}
            </Tag>
          </h5>
        </div>
        <div className="sample-grid-container">
          {!loading && readyForExtractionSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.gbd.extraction.ready.empty"
                  defaultMessage="No samples ready for extraction."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="gbd-extraction-ready"
              samples={readyForExtractionSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              showSelection={canPerformExtraction}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "sampleType", header: "Sample Type" },
                { key: "collectionDate", header: "Collection Date" },
                { key: "source", header: "Source" },
                { key: "volumeConcentration", header: "Volume/Conc (ng/µL)" },
                { key: "methodKit", header: "Extraction Method/Kit" },
                { key: "operator", header: "Extraction Operator" },
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

      {/* Extraction Completed Section */}
      {extractionCompletedSamples.length > 0 && (
        <div className="sample-table-section">
          <div className="table-section-header">
            <h5>
              <FormattedMessage
                id="notebook.gbd.extraction.completed.title"
                defaultMessage="Extraction Completed"
              />
              <Tag type="green" size="sm" className="count-tag">
                {extractionCompletedSamples.length}
              </Tag>
            </h5>
          </div>
          <div className="sample-grid-container">
            <SampleGrid
              gridId="gbd-extraction-completed"
              samples={extractionCompletedSamples}
              showSelection={false}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "sampleType", header: "Sample Type" },
                { key: "collectionDate", header: "Collection Date" },
                { key: "methodKit", header: "Extraction Method/Kit" },
                { key: "operator", header: "Extraction Operator" },
                { key: "lotNumbers", header: "Lot Numbers" },
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
          </div>
        </div>
      )}

      <Modal
        open={extractionModalOpen}
        onRequestClose={() => setExtractionModalOpen(false)}
        onRequestSubmit={applyExtraction}
        modalHeading={intl.formatMessage({
          id: "notebook.gbd.extraction.modal.title",
          defaultMessage: "Record DNA/RNA Extraction",
        })}
        primaryButtonText={
          isApplyingExtraction
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
        primaryButtonDisabled={
          isApplyingExtraction || !canModifyData || isViewOnly
        }
        size="lg"
      >
        {isApplyingExtraction && <Loading withOverlay={false} small />}

        <Grid narrow>
          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="methodKit"
              titleText={intl.formatMessage({
                id: "notebook.gbd.extraction.methodKit",
                defaultMessage: "Extraction Method/Kit *",
              })}
              label="Select..."
              items={methodKitOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={methodKit}
              onChange={({ selectedItem }) => setMethodKit(selectedItem)}
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="lotNumbers"
              labelText={intl.formatMessage({
                id: "notebook.gbd.extraction.lotNumbers",
                defaultMessage: "Lot Numbers *",
              })}
              value={lotNumbers}
              onChange={(e) => setLotNumbers(e.target.value)}
              placeholder="e.g., LOT123456"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="operator"
              labelText={intl.formatMessage({
                id: "notebook.gbd.extraction.operator",
                defaultMessage: "Operator Name *",
              })}
              value={operator}
              onChange={(e) => setOperator(e.target.value)}
              placeholder="Full name of technician"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <DatePickerInput
              id="dateTime"
              labelText={intl.formatMessage({
                id: "notebook.gbd.extraction.dateTime",
                defaultMessage: "Extraction Date",
              })}
              value={dateTime}
              onChange={(e) => setDateTime(e.target.value)}
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="notes"
              labelText={intl.formatMessage({
                id: "notebook.gbd.extraction.notes",
                defaultMessage: "Notes/Observations",
              })}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={2}
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
};

export default GBDDNARNAExtractionPage;
