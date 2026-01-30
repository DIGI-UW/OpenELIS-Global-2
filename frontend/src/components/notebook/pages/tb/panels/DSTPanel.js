import {
  useState,
  useEffect,
  useRef,
  useCallback,
  useMemo,
  useContext,
} from "react";
import {
  Grid,
  Column,
  Button,
  InlineNotification,
  Select,
  SelectItem,
  TextArea,
  Tile,
  Modal,
  Tag,
  RadioButtonGroup,
  RadioButton,
} from "@carbon/react";
import { Add, Renew } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  convertToISODate,
} from "../../../../utils/Utils";
import SampleGrid from "../../../workflow/SampleGrid";
import CustomDatePicker from "../../../../common/CustomDatePicker";
import { ConfigurationContext } from "../../../../layout/Layout";

/**
 * DSTPanel - Drug Susceptibility Testing results panel.
 *
 * DST Methods:
 * - PHENOTYPIC_1ST: Phenotypic DST for 1st line drugs (INH, RMP, STM, EMB, PZA)
 * - PHENOTYPIC_2ND: Phenotypic DST for 2nd line drugs (FLQ, KAN, AMK, CAP, VIO)
 * - MOLECULAR_1ST: Molecular DST using Line Probe Assay (LPA)
 *
 * Drug Susceptibility Results per drug:
 * - S: Sensitive (drug is effective)
 * - R: Resistant (drug resistance detected)
 * - INVALID: Invalid result
 *
 * Special Classifications:
 * - MDR-TB: Multi-Drug Resistant (INH + RIF resistant)
 * - XDR-TB: Extensively Drug Resistant (MDR + FLQ + injectable resistant)
 */
function DSTPanel({ pageData, onProgressUpdate, cultureSamples = [] }) {
  const intl = useIntl();
  const { configurationProperties } = useContext(ConfigurationContext);
  const componentMounted = useRef(false);

  const [pageSamples, setPageSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);

  // Drug definitions
  const firstLineDrugs = useMemo(
    () => [
      { code: "INH", name: "Isoniazid" },
      { code: "RMP", name: "Rifampicin" },
      { code: "STM", name: "Streptomycin" },
      { code: "EMB", name: "Ethambutol" },
      { code: "PZA", name: "Pyrazinamide" },
    ],
    [],
  );

  const secondLineDrugs = useMemo(
    () => [
      { code: "FLQ", name: "Fluoroquinolones" },
      { code: "KAN", name: "Kanamycin" },
      { code: "AMK", name: "Amikacin" },
      { code: "CAP", name: "Capreomycin" },
      { code: "VIO", name: "Viomycin" },
    ],
    [],
  );

  const molecularDrugs = useMemo(
    () => [
      { code: "INH", name: "Isoniazid" },
      { code: "RMP", name: "Rifampicin" },
    ],
    [],
  );

  // DST should only show culture-positive samples (phenotypic DST requires viable bacteria)
  const samples = useMemo(() => {
    // Transform culture-positive samples
    const culturePositiveSamples = cultureSamples
      .filter((s) => s.cultureResult === "POSITIVE")
      .map((sample) => ({
        id: sample.sampleItemId,
        externalId: sample.externalId,
        accessionNumber: sample.accessionNumber,
        sampleType: null,
        specimenType: null,
        status: "PENDING",
        source: "culture",
        cultureResult: sample.cultureResult,
        positiveWeek: sample.positiveWeek,
        // DST fields
        dstMethod: null,
        dstResults: null,
        dstClassification: null,
        resultDate: null,
        testedBy: null,
        reviewedBy: null,
        notes: null,
      }));

    // Only include page samples that have DST results (completed samples)
    // These should also be culture-positive but may have been processed
    const pageSamplesWithDst = pageSamples.filter((s) => s.dstResults);

    // Merge: page samples with DST results take precedence
    const pageSampleIds = new Set(pageSamplesWithDst.map((s) => s.id));
    const uniqueCultureSamples = culturePositiveSamples.filter(
      (s) => !pageSampleIds.has(s.id),
    );

    return [...pageSamplesWithDst, ...uniqueCultureSamples];
  }, [pageSamples, cultureSamples]);

  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Result entry modal state
  const [resultModalOpen, setResultModalOpen] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  // DST result form state
  const [resultData, setResultData] = useState({
    method: "PHENOTYPIC_1ST",
    drugResults: {},
    resultDate: configurationProperties?.currentDateAsText || "",
    testedBy: "",
    reviewedBy: "",
    notes: "",
  });

  // Method options
  const methodOptions = [
    {
      value: "PHENOTYPIC_1ST",
      label: intl.formatMessage({
        id: "notebook.tb.dst.method.phenotypic1st",
        defaultMessage: "Phenotypic DST - 1st Line Drugs",
      }),
    },
    {
      value: "PHENOTYPIC_2ND",
      label: intl.formatMessage({
        id: "notebook.tb.dst.method.phenotypic2nd",
        defaultMessage: "Phenotypic DST - 2nd Line Drugs",
      }),
    },
    {
      value: "MOLECULAR_1ST",
      label: intl.formatMessage({
        id: "notebook.tb.dst.method.molecular1st",
        defaultMessage: "Molecular DST (LPA) - 1st Line",
      }),
    },
  ];

  // Get drugs based on selected method
  const getDrugsForMethod = useCallback(
    (method) => {
      switch (method) {
        case "PHENOTYPIC_1ST":
          return firstLineDrugs;
        case "PHENOTYPIC_2ND":
          return secondLineDrugs;
        case "MOLECULAR_1ST":
          return molecularDrugs;
        default:
          return firstLineDrugs;
      }
    },
    [firstLineDrugs, secondLineDrugs, molecularDrugs],
  );

  // Initialize drug results when method changes
  useEffect(() => {
    const drugs = getDrugsForMethod(resultData.method);
    const initialResults = {};
    drugs.forEach((drug) => {
      initialResults[drug.code] =
        resultData.drugResults[drug.code] || "PENDING";
    });
    setResultData((prev) => ({
      ...prev,
      drugResults: initialResults,
    }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [resultData.method, getDrugsForMethod]);

  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    return () => {
      componentMounted.current = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pageData?.id]);

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
    setSuccessMessage(null);

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
              specimenType: sample.data?.specimenType,
              status: sample.pageStatus || sample.status || "PENDING",
              // DST result fields
              dstMethod: sample.data?.dstMethod,
              dstResults: sample.data?.dstResults,
              dstClassification: sample.data?.dstClassification,
              resultDate: sample.data?.dstResultDate,
              testedBy: sample.data?.dstTestedBy,
              reviewedBy: sample.data?.dstReviewedBy,
              notes: sample.data?.dstNotes,
            }));
            setPageSamples(transformedSamples);
          } else {
            setPageSamples([]);
          }
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Reset result form
  const resetResultData = () => {
    const drugs = getDrugsForMethod("PHENOTYPIC_1ST");
    const initialResults = {};
    drugs.forEach((drug) => {
      initialResults[drug.code] = "PENDING";
    });
    setResultData({
      method: "PHENOTYPIC_1ST",
      drugResults: initialResults,
      resultDate: configurationProperties?.currentDateAsText || "",
      testedBy: "",
      reviewedBy: "",
      notes: "",
    });
  };

  // Determine DST classification from results
  const classifyDstResults = useCallback((method, drugResults) => {
    const isResistant = (code) => drugResults[code] === "R";

    const inhResistant = isResistant("INH");
    const rmpResistant = isResistant("RMP");

    // MDR-TB: Resistant to both INH and RMP
    const isMdr = inhResistant && rmpResistant;

    if (method === "PHENOTYPIC_2ND" || method === "MOLECULAR_2ND") {
      // Check for XDR-TB if 2nd line testing
      const flqResistant = isResistant("FLQ");
      const injectableResistant =
        isResistant("KAN") || isResistant("AMK") || isResistant("CAP");

      if (isMdr && flqResistant && injectableResistant) {
        return "XDR";
      }
      // Pre-XDR if MDR + FLQ or injectable
      if (isMdr && (flqResistant || injectableResistant)) {
        return "PRE_XDR";
      }
    }

    if (isMdr) {
      return "MDR";
    }

    // Any resistance (mono or poly)
    const hasAnyResistance = Object.values(drugResults).some((r) => r === "R");
    if (hasAnyResistance) {
      if (inhResistant && !rmpResistant) {
        return "INH_MONO";
      }
      if (rmpResistant && !inhResistant) {
        return "RMP_MONO";
      }
      return "OTHER_RESISTANCE";
    }

    // All sensitive
    const allTested = Object.values(drugResults).every((r) => r !== "PENDING");
    const allSensitive = Object.values(drugResults).every((r) => r === "S");
    if (allTested && allSensitive) {
      return "FULLY_SENSITIVE";
    }

    return "PENDING";
  }, []);

  // Calculate stats
  const stats = useMemo(() => {
    const tested = samples.filter((s) => s.dstResults).length;
    const fullySensitive = samples.filter(
      (s) => s.dstClassification === "FULLY_SENSITIVE",
    ).length;
    const anyResistance = samples.filter(
      (s) =>
        s.dstClassification &&
        s.dstClassification !== "FULLY_SENSITIVE" &&
        s.dstClassification !== "PENDING",
    ).length;
    const mdr = samples.filter(
      (s) =>
        s.dstClassification === "MDR" ||
        s.dstClassification === "PRE_XDR" ||
        s.dstClassification === "XDR",
    ).length;
    const xdr = samples.filter((s) => s.dstClassification === "XDR").length;
    const pending = samples.filter((s) => !s.dstResults).length;

    return {
      total: samples.length,
      tested,
      fullySensitive,
      anyResistance,
      mdr,
      xdr,
      pending,
    };
  }, [samples]);

  // Handle opening result modal
  const handleOpenResultModal = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tb.dst.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    setResultModalOpen(true);
  }, [selectedSampleIds, intl]);

  // Handle saving DST result
  const handleSaveResult = useCallback(() => {
    // Validate at least one drug has a result
    const drugs = getDrugsForMethod(resultData.method);
    const hasResults = drugs.some(
      (d) =>
        resultData.drugResults[d.code] === "S" ||
        resultData.drugResults[d.code] === "R" ||
        resultData.drugResults[d.code] === "INVALID",
    );

    if (!hasResults) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tb.dst.error.noResults",
          defaultMessage: "Please enter at least one drug result.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tb.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    setIsSaving(true);
    setError(null);

    // Calculate classification
    const classification = classifyDstResults(
      resultData.method,
      resultData.drugResults,
    );

    const data = {
      dstMethod: resultData.method,
      dstResults: resultData.drugResults,
      dstClassification: classification,
      dstResultDate: convertToISODate(resultData.resultDate),
      dstTestedBy: resultData.testedBy || null,
      dstReviewedBy: resultData.reviewedBy || null,
      dstNotes: resultData.notes,
    };

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        data,
      }),
      (status) => {
        if (status === 200) {
          // Update status to COMPLETED
          postToOpenElisServer(
            `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
            JSON.stringify({
              sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
              status: "COMPLETED",
            }),
            () => {
              setIsSaving(false);
              const isMdr =
                classification === "MDR" ||
                classification === "PRE_XDR" ||
                classification === "XDR";
              setSuccessMessage(
                intl.formatMessage(
                  {
                    id: isMdr
                      ? "notebook.page.tb.dst.resultSaved.mdr"
                      : "notebook.page.tb.dst.resultSaved",
                    defaultMessage: isMdr
                      ? "MDR-TB detected! DST result recorded for {count} sample(s). Refer patient for specialist care."
                      : "DST result recorded for {count} sample(s).",
                  },
                  { count: selectedSampleIds.length },
                ),
              );
              setResultModalOpen(false);
              setSelectedSampleIds([]);
              resetResultData();
              loadPageSamples();
              if (onProgressUpdate) {
                onProgressUpdate();
              }
            },
          );
        } else {
          setIsSaving(false);
          setError(
            intl.formatMessage({
              id: "notebook.page.tb.dst.error.save",
              defaultMessage: "Failed to save result. Please try again.",
            }),
          );
        }
      },
    );
  }, [
    resultData,
    selectedSampleIds,
    hasRealPageId,
    intl,
    loadPageSamples,
    onProgressUpdate,
    pageData?.id,
    getDrugsForMethod,
    classifyDstResults,
  ]);

  // Get classification tag
  const getClassificationTag = (classification) => {
    if (!classification) return <Tag type="gray">Pending</Tag>;
    const config = {
      FULLY_SENSITIVE: { type: "green", label: "Fully Sensitive" },
      INH_MONO: { type: "purple", label: "INH Mono-R" },
      RMP_MONO: { type: "purple", label: "RMP Mono-R" },
      OTHER_RESISTANCE: { type: "purple", label: "Resistant" },
      MDR: { type: "red", label: "MDR-TB" },
      PRE_XDR: { type: "magenta", label: "Pre-XDR" },
      XDR: { type: "magenta", label: "XDR-TB" },
      PENDING: { type: "gray", label: "Pending" },
    };
    const cfg = config[classification] || {
      type: "gray",
      label: classification,
    };
    return <Tag type={cfg.type}>{cfg.label}</Tag>;
  };

  // Get method tag
  const getMethodTag = (method) => {
    if (!method) return null;
    const methodLabels = {
      PHENOTYPIC_1ST: { type: "blue", label: "1st Line" },
      PHENOTYPIC_2ND: { type: "teal", label: "2nd Line" },
      MOLECULAR_1ST: { type: "cyan", label: "LPA" },
    };
    const config = methodLabels[method] || { type: "gray", label: method };
    return <Tag type={config.type}>{config.label}</Tag>;
  };

  // Render drug result summary
  const renderDrugResultSummary = (dstResults) => {
    if (!dstResults) return "-";
    const resistant = Object.entries(dstResults)
      .filter(([, val]) => val === "R")
      .map(([code]) => code);
    if (resistant.length === 0) return "All S";
    return resistant.join(", ") + " R";
  };

  // Handle drug result change
  const handleDrugResultChange = (drugCode, value) => {
    setResultData((prev) => ({
      ...prev,
      drugResults: {
        ...prev.drugResults,
        [drugCode]: value,
      },
    }));
  };

  return (
    <div className="dst-panel">
      {/* Section Header */}
      <div className="panel-section-header">
        <h5>
          <FormattedMessage
            id="notebook.page.tb.dst.title"
            defaultMessage="Drug Susceptibility Testing (DST)"
          />
        </h5>
        <p className="panel-description">
          <FormattedMessage
            id="notebook.page.tb.dst.description"
            defaultMessage="Record phenotypic and molecular drug susceptibility testing results for 1st and 2nd line anti-TB drugs."
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
                  id="notebook.page.tb.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{stats.total}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tb.dst.fullySensitive"
                  defaultMessage="Fully Sensitive"
                />
              </span>
              <span className="progress-value">{stats.fullySensitive}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tb.dst.anyResistance"
                  defaultMessage="Any Resistance"
                />
              </span>
              <span className="progress-value">{stats.anyResistance}</span>
            </Tile>
            <Tile className="progress-tile rejected">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tb.dst.mdr"
                  defaultMessage="MDR-TB"
                />
              </span>
              <span className="progress-value">{stats.mdr}</span>
            </Tile>
            <Tile className="progress-tile error">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tb.dst.xdr"
                  defaultMessage="XDR-TB"
                />
              </span>
              <span className="progress-value">{stats.xdr}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Add}
          onClick={handleOpenResultModal}
          disabled={selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="notebook.page.tb.dst.addResult"
            defaultMessage="Enter DST Result ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="ghost"
          size="sm"
          renderIcon={Renew}
          onClick={loadPageSamples}
        >
          <FormattedMessage
            id="notebook.page.tb.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      {/* Messages */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          onClose={() => setError(null)}
          lowContrast
        />
      )}
      {successMessage && (
        <InlineNotification
          kind="success"
          title={successMessage}
          onClose={() => setSuccessMessage(null)}
          lowContrast
        />
      )}

      {/* Pending Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.tb.dst.pendingTable.title"
              defaultMessage="Samples Pending DST"
            />
            <Tag type="gray" className="count-tag">
              {samples.filter((s) => !s.dstResults).length}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.tb.dst.pendingTable.description"
              defaultMessage="Select samples and enter drug susceptibility testing results."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          <SampleGrid
            gridId="dst-pending"
            samples={samples.filter((s) => !s.dstResults)}
            selectedIds={selectedSampleIds}
            onSelectionChange={setSelectedSampleIds}
            statusFilter={statusFilter}
            onStatusFilterChange={setStatusFilter}
            showSelection={true}
            loading={loading}
            columns={[
              {
                key: "accessionNumber",
                header: intl.formatMessage({
                  id: "notebook.grid.accessionNumber",
                  defaultMessage: "Accession Number",
                }),
              },
              {
                key: "externalId",
                header: intl.formatMessage({
                  id: "notebook.grid.sampleId",
                  defaultMessage: "Sample ID",
                }),
              },
              {
                key: "cultureResult",
                header: intl.formatMessage({
                  id: "notebook.grid.cultureResult",
                  defaultMessage: "Culture",
                }),
                render: (value) =>
                  value ? (
                    <Tag type="red" size="sm">
                      Culture+
                    </Tag>
                  ) : null,
              },
              {
                key: "dstClassification",
                header: intl.formatMessage({
                  id: "notebook.grid.dstClassification",
                  defaultMessage: "Classification",
                }),
                render: (value) => getClassificationTag(value),
              },
            ]}
          />
        </div>
        {!loading && samples.filter((s) => !s.dstResults).length === 0 && (
          <div className="empty-table-state">
            <p>
              <FormattedMessage
                id="notebook.page.tb.dst.pendingTable.empty"
                defaultMessage="No samples pending DST testing."
              />
            </p>
          </div>
        )}
      </div>

      {/* Completed Results Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.tb.dst.completedTable.title"
              defaultMessage="Completed DST Results"
            />
            <Tag type="green" className="count-tag">
              {samples.filter((s) => s.dstResults).length}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.tb.dst.completedTable.description"
              defaultMessage="Samples with completed DST results."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          <SampleGrid
            gridId="dst-completed"
            samples={samples.filter((s) => s.dstResults)}
            selectedIds={[]}
            showSelection={false}
            loading={loading}
            columns={[
              {
                key: "accessionNumber",
                header: intl.formatMessage({
                  id: "notebook.grid.accessionNumber",
                  defaultMessage: "Accession Number",
                }),
              },
              {
                key: "externalId",
                header: intl.formatMessage({
                  id: "notebook.grid.sampleId",
                  defaultMessage: "Sample ID",
                }),
              },
              {
                key: "dstMethod",
                header: intl.formatMessage({
                  id: "notebook.grid.dstMethod",
                  defaultMessage: "Method",
                }),
                render: (value) => getMethodTag(value),
              },
              {
                key: "dstClassification",
                header: intl.formatMessage({
                  id: "notebook.grid.dstClassification",
                  defaultMessage: "Classification",
                }),
                render: (value) => getClassificationTag(value),
              },
              {
                key: "dstResults",
                header: intl.formatMessage({
                  id: "notebook.grid.dstResults",
                  defaultMessage: "Drug Results",
                }),
                render: (value) => renderDrugResultSummary(value),
              },
              {
                key: "resultDate",
                header: intl.formatMessage({
                  id: "notebook.grid.resultDate",
                  defaultMessage: "Result Date",
                }),
              },
            ]}
          />
        </div>
        {!loading && samples.filter((s) => s.dstResults).length === 0 && (
          <div className="empty-table-state">
            <p>
              <FormattedMessage
                id="notebook.page.tb.dst.completedTable.empty"
                defaultMessage="No completed DST results yet."
              />
            </p>
          </div>
        )}
      </div>

      {/* Global empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state global-empty">
          <p>
            <FormattedMessage
              id="notebook.page.tb.dst.empty"
              defaultMessage="No samples available for DST testing. Culture-positive samples will appear here."
            />
          </p>
        </div>
      )}

      {/* DST Result Modal */}
      <Modal
        open={resultModalOpen}
        onRequestClose={() => setResultModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.tb.dst.modal.title",
          defaultMessage: "Enter DST Result",
        })}
        primaryButtonText={
          isSaving
            ? intl.formatMessage({
                id: "label.saving",
                defaultMessage: "Saving...",
              })
            : intl.formatMessage({
                id: "label.save",
                defaultMessage: "Save Result",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleSaveResult}
        onSecondarySubmit={() => setResultModalOpen(false)}
        size="lg"
        primaryButtonDisabled={isSaving}
      >
        <div className="dst-result-modal">
          <p className="modal-description">
            <FormattedMessage
              id="notebook.tb.dst.modal.description"
              defaultMessage="Record drug susceptibility results for {count} selected sample(s)."
              values={{ count: selectedSampleIds.length }}
            />
          </p>

          <Grid fullWidth>
            {/* Method Selection */}
            <Column lg={8} md={4} sm={4}>
              <Select
                id="dstMethod"
                labelText={intl.formatMessage({
                  id: "notebook.tb.dst.method",
                  defaultMessage: "DST Method",
                })}
                value={resultData.method}
                onChange={(e) =>
                  setResultData((prev) => ({
                    ...prev,
                    method: e.target.value,
                  }))
                }
              >
                {methodOptions.map((option) => (
                  <SelectItem
                    key={option.value}
                    value={option.value}
                    text={option.label}
                  />
                ))}
              </Select>
            </Column>

            {/* Result Date */}
            <Column lg={8} md={4} sm={4}>
              <CustomDatePicker
                id="resultDate"
                labelText={intl.formatMessage({
                  id: "notebook.tb.dst.resultDate",
                  defaultMessage: "Result Date",
                })}
                value={resultData.resultDate}
                onChange={(date) =>
                  setResultData((prev) => ({
                    ...prev,
                    resultDate: date,
                  }))
                }
              />
            </Column>

            {/* Drug Results Section */}
            <Column lg={16} md={8} sm={4}>
              <div
                style={{
                  marginTop: "1rem",
                  marginBottom: "0.5rem",
                  fontWeight: "600",
                }}
              >
                <FormattedMessage
                  id="notebook.tb.dst.drugResults"
                  defaultMessage="Drug Susceptibility Results"
                />
              </div>
              <p
                style={{
                  fontSize: "0.875rem",
                  color: "#525252",
                  marginBottom: "1rem",
                }}
              >
                <FormattedMessage
                  id="notebook.tb.dst.drugResults.hint"
                  defaultMessage="S = Sensitive (effective), R = Resistant, I = Invalid"
                />
              </p>
            </Column>

            {/* Drug Result Entries */}
            {getDrugsForMethod(resultData.method).map((drug) => (
              <Column key={drug.code} lg={8} md={4} sm={4}>
                <div
                  style={{
                    padding: "0.75rem",
                    backgroundColor: "#f4f4f4",
                    borderRadius: "4px",
                    marginBottom: "0.5rem",
                  }}
                >
                  <div
                    style={{
                      fontWeight: "500",
                      marginBottom: "0.5rem",
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                    }}
                  >
                    <span>
                      {drug.code} - {drug.name}
                    </span>
                    {resultData.drugResults[drug.code] === "R" && (
                      <Tag type="red" size="sm">
                        Resistant
                      </Tag>
                    )}
                    {resultData.drugResults[drug.code] === "S" && (
                      <Tag type="green" size="sm">
                        Sensitive
                      </Tag>
                    )}
                  </div>
                  <RadioButtonGroup
                    name={`drug-${drug.code}`}
                    valueSelected={
                      resultData.drugResults[drug.code] || "PENDING"
                    }
                    onChange={(value) =>
                      handleDrugResultChange(drug.code, value)
                    }
                    orientation="horizontal"
                  >
                    <RadioButton
                      id={`${drug.code}-pending`}
                      value="PENDING"
                      labelText="—"
                    />
                    <RadioButton
                      id={`${drug.code}-sensitive`}
                      value="S"
                      labelText="S"
                    />
                    <RadioButton
                      id={`${drug.code}-resistant`}
                      value="R"
                      labelText="R"
                    />
                    <RadioButton
                      id={`${drug.code}-invalid`}
                      value="INVALID"
                      labelText="I"
                    />
                  </RadioButtonGroup>
                </div>
              </Column>
            ))}

            {/* Notes */}
            <Column lg={16} md={8} sm={4}>
              <TextArea
                id="notes"
                labelText={intl.formatMessage({
                  id: "notebook.tb.dst.notes",
                  defaultMessage: "Notes",
                })}
                value={resultData.notes}
                onChange={(e) =>
                  setResultData((prev) => ({
                    ...prev,
                    notes: e.target.value,
                  }))
                }
                placeholder={intl.formatMessage({
                  id: "notebook.tb.dst.notes.placeholder",
                  defaultMessage:
                    "Add any observations about the DST results...",
                })}
                rows={3}
              />
            </Column>
          </Grid>

          {/* MDR-TB Warning */}
          {resultData.drugResults["INH"] === "R" &&
            resultData.drugResults["RMP"] === "R" && (
              <InlineNotification
                kind="error"
                title={intl.formatMessage({
                  id: "notebook.tb.dst.mdr.title",
                  defaultMessage: "MDR-TB Detected",
                })}
                subtitle={intl.formatMessage({
                  id: "notebook.tb.dst.mdr.subtitle",
                  defaultMessage:
                    "Patient is resistant to both Isoniazid (INH) and Rifampicin (RMP). This indicates Multi-Drug Resistant TB. Refer for specialist care and second-line DST.",
                })}
                hideCloseButton
                lowContrast
                style={{ marginTop: "1rem" }}
              />
            )}

          {/* RMP Mono-resistance Warning */}
          {resultData.drugResults["RMP"] === "R" &&
            resultData.drugResults["INH"] !== "R" && (
              <InlineNotification
                kind="warning"
                title={intl.formatMessage({
                  id: "notebook.tb.dst.rmpMono.title",
                  defaultMessage: "Rifampicin Mono-Resistance",
                })}
                subtitle={intl.formatMessage({
                  id: "notebook.tb.dst.rmpMono.subtitle",
                  defaultMessage:
                    "Rifampicin resistance detected without INH resistance. Consider extended DST and specialist consultation.",
                })}
                hideCloseButton
                lowContrast
                style={{ marginTop: "1rem" }}
              />
            )}

          {/* INH Mono-resistance Info */}
          {resultData.drugResults["INH"] === "R" &&
            resultData.drugResults["RMP"] !== "R" && (
              <InlineNotification
                kind="warning"
                title={intl.formatMessage({
                  id: "notebook.tb.dst.inhMono.title",
                  defaultMessage: "Isoniazid Mono-Resistance",
                })}
                subtitle={intl.formatMessage({
                  id: "notebook.tb.dst.inhMono.subtitle",
                  defaultMessage:
                    "Isoniazid resistance detected. Modify treatment regimen accordingly.",
                })}
                hideCloseButton
                lowContrast
                style={{ marginTop: "1rem" }}
              />
            )}

          {/* Drug Legend */}
          <div
            style={{
              marginTop: "1.5rem",
              padding: "1rem",
              backgroundColor: "#e5f6ff",
              borderRadius: "4px",
              borderLeft: "4px solid #0f62fe",
            }}
          >
            <strong>
              <FormattedMessage
                id="notebook.tb.dst.legend.title"
                defaultMessage="Classification Guide:"
              />
            </strong>
            <ul
              style={{
                margin: "0.5rem 0 0 1rem",
                fontSize: "0.875rem",
                color: "#525252",
              }}
            >
              <li>
                <strong>MDR-TB:</strong>{" "}
                <FormattedMessage
                  id="notebook.tb.dst.legend.mdr"
                  defaultMessage="Resistant to both INH and RMP"
                />
              </li>
              <li>
                <strong>Pre-XDR:</strong>{" "}
                <FormattedMessage
                  id="notebook.tb.dst.legend.prexdr"
                  defaultMessage="MDR + resistance to fluoroquinolone OR injectable"
                />
              </li>
              <li>
                <strong>XDR-TB:</strong>{" "}
                <FormattedMessage
                  id="notebook.tb.dst.legend.xdr"
                  defaultMessage="MDR + resistance to fluoroquinolone AND injectable"
                />
              </li>
            </ul>
          </div>
        </div>
      </Modal>
    </div>
  );
}

export default DSTPanel;
