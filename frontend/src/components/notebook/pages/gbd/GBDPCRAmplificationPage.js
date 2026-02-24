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
  NumberInput,
  DatePickerInput,
  Grid,
  Column,
  Tile,
  TextArea,
  Tag,
  Loading,
  Checkbox,
} from "@carbon/react";
import { Renew, CheckmarkFilled, Chemistry } from "@carbon/react/icons";
import { NotificationContext } from "../../../layout/Layout";
import {
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
  getFromOpenElisServer,
} from "../../../utils/Utils";
import { NotificationKinds } from "../../../../components/common/CustomNotification";
import { Permissions } from "../../../../constants/roles";
import PermissionGate from "../../../security/PermissionGate";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * GBDPCRAmplificationPage - Page 4: PCR Amplification
 *
 * Manages PCR amplification process for nucleic acids using TMMRD design pattern:
 * - Section-based layout (not tabs)
 * - Action buttons bar with Primary/Tertiary/Ghost buttons
 * - Progress tiles for workflow tracking
 * - Supports conventional PCR and qPCR
 * - Records primers, protocols, cycling conditions, and expected band sizes
 * - Tracks sample progression to Gel Electrophoresis (Page 5)
 *
 * Data stored in sample.data JSONB:
 * {
 *   pcr: {
 *     type: "conventional|qpcr",
 *     primers: "Forward/Reverse sequences or primer set name",
 *     protocol: "Protocol reference or description",
 *     cyclingConditions: "Detailed cycling parameters",
 *     expectedBandSize: "Expected PCR product size (bp)",
 *     operator: "Name of technician",
 *     dateTime: "2024-01-27",
 *     notes: "PCR observations or results"
 *   }
 * }
 */
export const GBDPCRAmplificationPage = ({
  entryId,
  pageData = {},
  onProgressUpdate,
}) => {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const componentMounted = useRef(false);
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);

  const [pcrModalOpen, setPcrModalOpen] = useState(false);
  const [isApplyingPCR, setIsApplyingPCR] = useState(false);
  const [isCompleting, setIsCompleting] = useState(false);

  const [pcrType, setPcrType] = useState(null);
  const [primers, setPrimers] = useState("");
  const [protocol, setProtocol] = useState("");
  const [cyclingConditions, setCyclingConditions] = useState("");
  const [expectedBandSize, setExpectedBandSize] = useState("");
  const [positiveControlPresent, setPositiveControlPresent] = useState(false);
  const [positiveControlBandSize, setPositiveControlBandSize] = useState("");
  const [positiveControlNotes, setPositiveControlNotes] = useState("");
  const [negativeControlPresent, setNegativeControlPresent] = useState(false);
  const [negativeControlResult, setNegativeControlResult] = useState(null);
  const [negativeControlNotes, setNegativeControlNotes] = useState("");
  const [operator, setOperator] = useState("");
  const [dateTime, setDateTime] = useState(
    new Date().toISOString().split("T")[0],
  );
  const [pcrTime, setPcrTime] = useState("09:00");
  const [notes, setNotes] = useState("");

  const pcrTypeOptions = [
    { id: "conventional", label: "Conventional PCR" },
    { id: "qpcr", label: "qPCR (Real-time PCR)" },
    { id: "digital", label: "Digital PCR" },
  ];

  const pcrProtocolOptions = [
    { id: "standard_gotaq", label: "Standard GoTaq" },
    { id: "kapa_hifi", label: "KAPA HiFi" },
    { id: "phusion", label: "Phusion" },
    { id: "taq_polymerase", label: "Taq Polymerase" },
    { id: "pfu", label: "Pfu Polymerase" },
    { id: "dreamtaq", label: "DreamTaq" },
    { id: "q5", label: "Q5 High-Fidelity" },
    { id: "iproof", label: "iProof High-Fidelity" },
    { id: "custom", label: "Custom Protocol" },
  ];

  const primerOptions = [
    { id: "16s_universal_f", label: "16S rRNA F (AGAGTTTGATCCTGGCTCAG)" },
    { id: "16s_universal_r", label: "16S rRNA R (TACGGYTACCTTGTTACGACTT)" },
    { id: "18s_universal_f", label: "18S rRNA F (GTACACACCGCCCGTC)" },
    { id: "18s_universal_r", label: "18S rRNA R (CTGAGCCAGTCAGTGT)" },
    { id: "its1_f", label: "ITS1 F (TCCGTAGGTGAACCTGCGG)" },
    { id: "its1_r", label: "ITS1 R (TCCTCCGCTTATTGATATGC)" },
    { id: "coi_f", label: "COI F (GGWACWGGWTGAACWGTWTAYCCYCC)" },
    { id: "coi_r", label: "COI R (TANACYTCNGGRTGNCCRAARAAYCA)" },
    { id: "rbcl_f", label: "rbcL F (ATGTCACCACAAACAGAGACTAAAGC)" },
    { id: "rbcl_r", label: "rbcL R (GTAAAATCAAGTCCACCRCG)" },
    { id: "its2_f", label: "ITS2 F (GCATCGATGAAGAACGCAGC)" },
    { id: "its2_r", label: "ITS2 R (TCCTCCGCTTATTGATATGC)" },
    { id: "custom", label: "Custom Primers" },
  ];

  const negativeControlResultOptions = [
    { id: "no_band", label: "No Band (Expected)" },
    { id: "unexpected_band", label: "Unexpected Band" },
    { id: "weak_band", label: "Weak Band" },
    { id: "contamination", label: "Contamination" },
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
                  pcrType: s.data?.pcr?.type,
                  primers: s.data?.pcr?.primers,
                  protocol: s.data?.pcr?.protocol,
                  cyclingConditions: s.data?.pcr?.cyclingConditions,
                  expectedBandSize: s.data?.pcr?.expectedBandSize,
                  pcrOperator: s.data?.pcr?.operator,
                  pcrDateTime: s.data?.pcr?.dateTime,
                  pcrNotes: s.data?.pcr?.notes,
                  positiveControl: s.data?.pcr?.positiveControl,
                  negativeControl: s.data?.pcr?.negativeControl,
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
    setPcrType(null);
    setPrimers("");
    setProtocol("");
    setCyclingConditions("");
    setExpectedBandSize("");
    setPositiveControlPresent(false);
    setPositiveControlBandSize("");
    setPositiveControlNotes("");
    setNegativeControlPresent(false);
    setNegativeControlResult(null);
    setNegativeControlNotes("");
    setOperator("");
    setDateTime(new Date().toISOString().split("T")[0]);
    setPcrTime("09:00");
    setNotes("");
  }, []);

  const openModal = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.gbd.pcr.error.noSample",
          defaultMessage: "Please select at least one sample.",
        }),
      });
      return;
    }
    resetForm();
    setPcrModalOpen(true);
  }, [selectedSampleIds, intl, resetForm, notify]);

  const applyPCR = useCallback(() => {
    if (!pcrType) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.gbd.pcr.error.requiredFields",
          defaultMessage: "Required Field Missing",
        }),
        message: intl.formatMessage({
          id: "notebook.gbd.pcr.error.typeRequired",
          defaultMessage: "PCR type is required",
        }),
      });
      return;
    }

    if (!hasRealPageId) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.gbd.pcr.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      });
      return;
    }

    setIsApplyingPCR(true);

    const sampleIds = selectedSampleIds.map((id) => parseInt(id, 10));

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds,
        data: {
          pcr: {
            type: pcrType.id,
            primers,
            protocol,
            cyclingConditions,
            expectedBandSize: expectedBandSize
              ? parseInt(expectedBandSize, 10)
              : null,
            positiveControl: {
              present: positiveControlPresent,
              bandSize: positiveControlBandSize
                ? parseInt(positiveControlBandSize, 10)
                : null,
              notes: positiveControlNotes || null,
            },
            negativeControl: {
              present: negativeControlPresent,
              result: negativeControlResult?.id || null,
              notes: negativeControlNotes || null,
            },
            operator,
            dateTime: dateTime && pcrTime ? `${dateTime}T${pcrTime}:00Z` : null,
            notes,
          },
        },
      }),
      (response) => {
        setIsApplyingPCR(false);
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
                      id: "notebook.gbd.pcr.success",
                      defaultMessage:
                        "PCR amplification recorded for {count} sample(s).",
                    },
                    {
                      count: response.updatedCount || selectedSampleIds.length,
                    },
                  ),
                });
                setPcrModalOpen(false);
                setSelectedSampleIds([]);
                loadPageSamples();
                if (onProgressUpdate) onProgressUpdate();
              } else {
                notify({
                  kind: NotificationKinds.error,
                  title: intl.formatMessage({
                    id: "notebook.gbd.pcr.error.statusUpdate",
                    defaultMessage:
                      "PCR recorded but failed to update sample status.",
                  }),
                });
              }
            },
          );
        } else {
          notify({
            kind: NotificationKinds.error,
            title: response?.error || "PCR amplification failed",
          });
        }
      },
    );
  }, [
    pcrType,
    primers,
    protocol,
    cyclingConditions,
    expectedBandSize,
    positiveControlPresent,
    positiveControlBandSize,
    positiveControlNotes,
    negativeControlPresent,
    negativeControlResult,
    negativeControlNotes,
    operator,
    dateTime,
    pcrTime,
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
          id: "notebook.gbd.pcr.noEligibleSamples",
          defaultMessage:
            "Selected samples must have PCR amplification recorded (status: In Progress) before completing.",
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
                id: "notebook.gbd.pcr.completeSuccess",
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
                id: "notebook.gbd.pcr.completeFailed",
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

  const readyForPCRSamples = useMemo(
    () =>
      samples.filter(
        (s) =>
          s.status === "PENDING" ||
          s.status === "IN_PROGRESS" ||
          s.status === "AWAITING",
      ),
    [samples],
  );

  const pcrCompletedSamples = useMemo(
    () => samples.filter((s) => s.status === "COMPLETED"),
    [samples],
  );

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
    <div className="gbd-pcr-amplification-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.gbd.pcr.title"
            defaultMessage="PCR Amplification"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.gbd.pcr.description"
            defaultMessage="Perform PCR amplification of target DNA sequences. Record PCR type, primers, protocols, cycling conditions, and expected product sizes."
          />
        </p>
      </div>

      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.gbd.pcr.ready"
                  defaultMessage="Ready for PCR"
                />
              </span>
              <span className="progress-value">
                {readyForPCRSamples.length}
              </span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.gbd.pcr.amplified"
                  defaultMessage="PCR Completed"
                />
              </span>
              <span className="progress-value">
                {pcrCompletedSamples.length}
              </span>
            </Tile>
          </div>
        </Column>
      </Grid>

      <div className="page-actions-bar">
        <PermissionGate permission={Permissions.UPDATE_SAMPLES}>
          <Button
            kind="primary"
            size="sm"
            renderIcon={Chemistry}
            onClick={openModal}
            disabled={selectedSampleIds.length === 0 || !hasRealPageId}
          >
            <FormattedMessage
              id="notebook.gbd.recordPCR"
              defaultMessage="Record PCR ({count})"
              values={{ count: selectedSampleIds.length }}
            />
          </Button>
        </PermissionGate>

        <PermissionGate permission={Permissions.PROCESS_SAMPLES}>
          <Button
            kind="tertiary"
            size="sm"
            renderIcon={CheckmarkFilled}
            onClick={handleMarkComplete}
            disabled={
              eligibleForCompletionCount === 0 || isCompleting || !hasRealPageId
            }
          >
            <FormattedMessage
              id="notebook.gbd.markComplete"
              defaultMessage="Mark Complete ({count})"
              values={{ count: eligibleForCompletionCount }}
            />
          </Button>
        </PermissionGate>

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
              id="notebook.gbd.pcr.ready.title"
              defaultMessage="Samples Ready for PCR Amplification"
            />
            <Tag type="blue" size="sm" className="count-tag">
              {readyForPCRSamples.length}
            </Tag>
          </h5>
        </div>
        <div className="sample-grid-container">
          {!loading && readyForPCRSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.gbd.pcr.ready.empty"
                  defaultMessage="No samples ready for PCR amplification."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="gbd-pcr-ready"
              samples={readyForPCRSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              showSelection={true}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "sampleType", header: "Sample Type" },
                { key: "collectionDate", header: "Collection Date" },
                { key: "source", header: "Source" },
                {
                  key: "positiveControl",
                  header: "Positive\nControl",
                  render: (_value, sample) => {
                    if (!sample.positiveControl?.present) return "-";
                    return (
                      <Tag
                        type="green"
                        size="sm"
                        title={sample.positiveControl.notes || ""}
                      >
                        ✓
                      </Tag>
                    );
                  },
                },
                {
                  key: "negativeControl",
                  header: "Negative\nControl",
                  render: (_value, sample) => {
                    if (!sample.negativeControl?.present) return "-";

                    const result = sample.negativeControl.result;
                    let tagType = "green";
                    let displayText = "✓";

                    if (
                      result === "unexpected_band" ||
                      result === "contamination"
                    ) {
                      tagType = "red";
                      displayText = "⚠";
                    } else if (result === "weak_band") {
                      tagType = "orange";
                      displayText = "⚠";
                    }

                    return (
                      <Tag
                        type={tagType}
                        size="sm"
                        title={sample.negativeControl.notes || ""}
                      >
                        {displayText}
                      </Tag>
                    );
                  },
                },
                {
                  key: "pcrOperator",
                  header: "PCR Operator",
                  render: (_value, sample) => sample.pcrOperator || "-",
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

      {/* PCR Completed Section */}
      {pcrCompletedSamples.length > 0 && (
        <div className="sample-table-section">
          <div className="table-section-header">
            <h5>
              <FormattedMessage
                id="notebook.gbd.pcr.completed.title"
                defaultMessage="PCR Amplification Completed"
              />
              <Tag type="green" size="sm" className="count-tag">
                {pcrCompletedSamples.length}
              </Tag>
            </h5>
          </div>
          <div className="sample-grid-container">
            <SampleGrid
              gridId="gbd-pcr-completed"
              samples={pcrCompletedSamples}
              showSelection={false}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "sampleType", header: "Sample Type" },
                { key: "collectionDate", header: "Collection Date" },
                {
                  key: "pcrType",
                  header: "PCR Type",
                  render: (_value, sample) => {
                    if (sample.pcrType === "qpcr") return "qPCR";
                    if (sample.pcrType === "digital") return "Digital PCR";
                    return "Conventional PCR";
                  },
                },
                {
                  key: "expectedBandSize",
                  header: "Expected\nBand Size (bp)",
                  render: (_value, sample) => sample.expectedBandSize || "-",
                },
                {
                  key: "primers",
                  header: "Primers",
                  render: (_value, sample) =>
                    sample.primers
                      ? sample.primers.length > 20
                        ? sample.primers.substring(0, 20) + "..."
                        : sample.primers
                      : "-",
                },
                {
                  key: "positiveControl",
                  header: "Positive\nControl",
                  render: (_value, sample) => {
                    if (!sample.positiveControl?.present) return "-";
                    return (
                      <Tag
                        type="green"
                        size="sm"
                        title={sample.positiveControl.notes || ""}
                      >
                        ✓{" "}
                        {sample.positiveControl.bandSize
                          ? `${sample.positiveControl.bandSize}bp`
                          : ""}
                      </Tag>
                    );
                  },
                },
                {
                  key: "negativeControl",
                  header: "Negative\nControl",
                  render: (_value, sample) => {
                    if (!sample.negativeControl?.present) return "-";

                    const result = sample.negativeControl.result;
                    let tagType = "green";
                    let displayText = "✓";

                    if (result === "no_band") {
                      tagType = "green";
                      displayText = "✓ No Band";
                    } else if (result === "unexpected_band") {
                      tagType = "red";
                      displayText = "⚠ Band";
                    } else if (result === "weak_band") {
                      tagType = "orange";
                      displayText = "⚠ Weak";
                    } else if (result === "contamination") {
                      tagType = "red";
                      displayText = "⚠ Contam";
                    }

                    return (
                      <Tag
                        type={tagType}
                        size="sm"
                        title={sample.negativeControl.notes || ""}
                      >
                        {displayText}
                      </Tag>
                    );
                  },
                },
                {
                  key: "pcrOperator",
                  header: "PCR Operator",
                  render: (_value, sample) => sample.pcrOperator || "-",
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
          </div>
        </div>
      )}

      <Modal
        open={pcrModalOpen}
        onRequestClose={() => setPcrModalOpen(false)}
        onRequestSubmit={applyPCR}
        modalHeading={intl.formatMessage({
          id: "notebook.gbd.pcr.modal.title",
          defaultMessage: "Record PCR Amplification",
        })}
        primaryButtonText={
          isApplyingPCR
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
        primaryButtonDisabled={isApplyingPCR}
        size="lg"
      >
        {isApplyingPCR && <Loading withOverlay={false} small />}

        <Grid narrow>
          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="pcr-type"
              titleText={intl.formatMessage({
                id: "notebook.gbd.pcr.type",
                defaultMessage: "PCR Type *",
              })}
              label="Select..."
              items={pcrTypeOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={pcrType}
              onChange={({ selectedItem }) => setPcrType(selectedItem)}
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="expected-band-size"
              labelText={intl.formatMessage({
                id: "notebook.gbd.pcr.expectedBandSize",
                defaultMessage: "Expected Band Size (bp)",
              })}
              value={expectedBandSize}
              onChange={(e) => setExpectedBandSize(e.target.value)}
              type="number"
              step="10"
              min="50"
              max="15000"
              placeholder="500"
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="primers"
              titleText={intl.formatMessage({
                id: "notebook.gbd.pcr.primers",
                defaultMessage: "Primers (Select from list or enter custom)",
              })}
              label="Select primer set"
              items={primerOptions}
              selectedItem={
                primerOptions.find((p) => p.label === primers) || null
              }
              onChange={({ selectedItem }) => {
                setPrimers(selectedItem?.label || "");
              }}
              itemToString={(item) => item?.label || ""}
            />
            {primers && !primerOptions.find((p) => p.label === primers) && (
              <div
                style={{
                  marginTop: "0.5rem",
                  fontSize: "0.875rem",
                  color: "#525252",
                }}
              >
                Custom: {primers}
              </div>
            )}
            {!primers && (
              <TextInput
                id="custom-primers-input"
                labelText="Or enter custom primers"
                value={primers}
                onChange={(e) => setPrimers(e.target.value)}
                placeholder="e.g., Forward: 5'-ATGC...-3' / Reverse: 5'-CGTA...-3'"
                style={{ marginTop: "0.5rem" }}
              />
            )}
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="protocol"
              titleText={intl.formatMessage({
                id: "notebook.gbd.pcr.protocol",
                defaultMessage: "PCR Protocol/Reference",
              })}
              label="Select PCR protocol"
              items={pcrProtocolOptions}
              selectedItem={
                pcrProtocolOptions.find((p) => p.label === protocol) || null
              }
              onChange={({ selectedItem }) => {
                setProtocol(selectedItem?.label || "");
              }}
              itemToString={(item) => item?.label || ""}
            />
            {protocol &&
              !pcrProtocolOptions.find((p) => p.label === protocol) && (
                <div
                  style={{
                    marginTop: "0.5rem",
                    fontSize: "0.875rem",
                    color: "#525252",
                  }}
                >
                  Custom: {protocol}
                </div>
              )}
            {!protocol && (
              <TextInput
                id="custom-protocol-input"
                labelText="Or enter custom protocol"
                value={protocol}
                onChange={(e) => setProtocol(e.target.value)}
                placeholder="e.g., Custom GoTaq variant, Lab protocol reference"
                style={{ marginTop: "0.5rem" }}
              />
            )}
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="cycling-conditions"
              labelText={intl.formatMessage({
                id: "notebook.gbd.pcr.cyclingConditions",
                defaultMessage: "Cycling Conditions",
              })}
              value={cyclingConditions}
              onChange={(e) => setCyclingConditions(e.target.value)}
              rows={3}
              placeholder="e.g., Initial denaturation: 95°C 5 min; 35 cycles: 95°C 30s, 60°C 30s, 72°C 1 min; Final extension: 72°C 5 min"
            />
          </Column>

          {/* PCR Controls Section */}
          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <div
              style={{
                border: "1px solid #ccc",
                padding: "1rem",
                borderRadius: "4px",
                backgroundColor: "#f9f9f9",
              }}
            >
              <h5
                style={{ marginTop: 0, marginBottom: "1rem", color: "#161616" }}
              >
                <FormattedMessage
                  id="notebook.gbd.pcr.controls"
                  defaultMessage="PCR Controls"
                />
              </h5>

              <div style={{ marginBottom: "1rem" }}>
                <Checkbox
                  id="positive-control-present"
                  labelText={intl.formatMessage({
                    id: "notebook.gbd.pcr.positiveControlPresent",
                    defaultMessage: "Positive Control Present",
                  })}
                  checked={positiveControlPresent}
                  onChange={(e) => setPositiveControlPresent(e.target.checked)}
                />
              </div>

              {positiveControlPresent && (
                <>
                  <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
                    <TextInput
                      id="positive-control-band-size"
                      labelText={intl.formatMessage({
                        id: "notebook.gbd.pcr.positiveControlBandSize",
                        defaultMessage: "Positive Control Band Size (bp)",
                      })}
                      value={positiveControlBandSize}
                      onChange={(e) =>
                        setPositiveControlBandSize(e.target.value)
                      }
                      type="number"
                      step="10"
                      min="50"
                      max="15000"
                      placeholder="500"
                    />
                  </Column>

                  <Column
                    lg={16}
                    md={16}
                    sm={4}
                    style={{ marginBottom: "1rem" }}
                  >
                    <TextArea
                      id="positive-control-notes"
                      labelText={intl.formatMessage({
                        id: "notebook.gbd.pcr.positiveControlNotes",
                        defaultMessage: "Positive Control Notes",
                      })}
                      value={positiveControlNotes}
                      onChange={(e) => setPositiveControlNotes(e.target.value)}
                      rows={2}
                      placeholder="e.g., Control worked as expected"
                    />
                  </Column>
                </>
              )}

              <div style={{ marginBottom: "1rem" }}>
                <Checkbox
                  id="negative-control-present"
                  labelText={intl.formatMessage({
                    id: "notebook.gbd.pcr.negativeControlPresent",
                    defaultMessage: "Negative Control Present",
                  })}
                  checked={negativeControlPresent}
                  onChange={(e) => setNegativeControlPresent(e.target.checked)}
                />
              </div>

              {negativeControlPresent && (
                <>
                  <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
                    <Dropdown
                      id="negative-control-result"
                      titleText={intl.formatMessage({
                        id: "notebook.gbd.pcr.negativeControlResult",
                        defaultMessage: "Negative Control Result",
                      })}
                      label="Select..."
                      items={negativeControlResultOptions}
                      itemToString={(item) => (item ? item.label : "")}
                      selectedItem={negativeControlResult}
                      onChange={({ selectedItem }) =>
                        setNegativeControlResult(selectedItem)
                      }
                    />
                  </Column>

                  <Column
                    lg={16}
                    md={16}
                    sm={4}
                    style={{ marginBottom: "1rem" }}
                  >
                    <TextArea
                      id="negative-control-notes"
                      labelText={intl.formatMessage({
                        id: "notebook.gbd.pcr.negativeControlNotes",
                        defaultMessage: "Negative Control Notes",
                      })}
                      value={negativeControlNotes}
                      onChange={(e) => setNegativeControlNotes(e.target.value)}
                      rows={2}
                      placeholder="e.g., Clean negative control, no visible bands"
                    />
                  </Column>
                </>
              )}
            </div>
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="operator"
              labelText={intl.formatMessage({
                id: "notebook.gbd.pcr.operator",
                defaultMessage: "PCR Operator Name",
              })}
              value={operator}
              onChange={(e) => setOperator(e.target.value)}
              placeholder="Name of person performing PCR"
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <DatePickerInput
              id="pcr-dateTime"
              labelText={intl.formatMessage({
                id: "notebook.gbd.pcr.dateTime",
                defaultMessage: "PCR Date",
              })}
              value={dateTime}
              onChange={(e) => setDateTime(e.target.value)}
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="pcr-notes"
              labelText={intl.formatMessage({
                id: "notebook.gbd.pcr.notes",
                defaultMessage: "PCR Notes/Observations",
              })}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={2}
              placeholder="Any observations or issues during PCR amplification"
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
};

export default GBDPCRAmplificationPage;
