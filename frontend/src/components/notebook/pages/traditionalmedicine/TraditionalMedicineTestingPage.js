import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Modal,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  Checkbox,
  Tag,
} from "@carbon/react";
import { Checkmark, Close, Edit, Chemistry } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * TraditionalMedicineTestingPage - Page 7 of the Traditional Medicine workflow.
 * Assess safety, efficacy, and readiness for formulation through phytochemical screening,
 * toxicity studies, and efficacy testing.
 */
function TraditionalMedicineTestingPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Bulk Apply Modal State
  const [bulkModalOpen, setBulkModalOpen] = useState(false);
  const [bulkFormData, setBulkFormData] = useState({
    // Phytochemical screening
    phytochemicalAlkaloids: false,
    phytochemicalFlavonoids: false,
    phytochemicalTannins: false,
    phytochemicalSaponins: false,
    phytochemicalGlycosides: false,
    phytochemicalTerpenoids: false,
    phytochemicalSteroids: false,
    phytochemicalPhenols: false,
    // Safety/Toxicity
    safetyStudyType: "",
    toxicityModel: "",
    toxicityOutcome: "",
    // Efficacy
    biologicalAssayType: "",
    assayProtocol: "",
    efficacyOutcome: "",
    // General
    testedBy: "",
    testDate: "",
    testingNotes: "",
  });

  // Safety study type options
  const safetyStudyTypes = [
    {
      value: "",
      label: intl.formatMessage({
        id: "common.select",
        defaultMessage: "Select...",
      }),
    },
    { value: "In Vitro", label: "In Vitro (Cell-based)" },
    { value: "In Vivo - Acute", label: "In Vivo - Acute Toxicity" },
    { value: "In Vivo - Subacute", label: "In Vivo - Subacute Toxicity" },
    { value: "In Vivo - Chronic", label: "In Vivo - Chronic Toxicity" },
    { value: "Genotoxicity", label: "Genotoxicity Study" },
    { value: "Cytotoxicity", label: "Cytotoxicity Assay" },
    { value: "Other", label: "Other" },
  ];

  // Toxicity model options
  const toxicityModels = [
    {
      value: "",
      label: intl.formatMessage({
        id: "common.select",
        defaultMessage: "Select...",
      }),
    },
    { value: "Cell Line", label: "Cell Line" },
    { value: "Rodent - Mouse", label: "Rodent (Mouse)" },
    { value: "Rodent - Rat", label: "Rodent (Rat)" },
    { value: "Zebrafish", label: "Zebrafish" },
    { value: "Brine Shrimp", label: "Brine Shrimp" },
    { value: "Other", label: "Other" },
  ];

  // Toxicity outcome options
  const toxicityOutcomes = [
    {
      value: "",
      label: intl.formatMessage({
        id: "common.select",
        defaultMessage: "Select...",
      }),
    },
    { value: "Safe", label: "Safe - No toxicity observed" },
    { value: "Low Toxicity", label: "Low Toxicity" },
    { value: "Moderate Toxicity", label: "Moderate Toxicity" },
    { value: "High Toxicity", label: "High Toxicity" },
    { value: "Inconclusive", label: "Inconclusive - Further testing needed" },
  ];

  // Biological assay type options
  const biologicalAssayTypes = [
    {
      value: "",
      label: intl.formatMessage({
        id: "common.select",
        defaultMessage: "Select...",
      }),
    },
    { value: "Antimicrobial", label: "Antimicrobial Activity" },
    { value: "Antioxidant", label: "Antioxidant Activity" },
    { value: "Anti-inflammatory", label: "Anti-inflammatory Activity" },
    { value: "Antidiabetic", label: "Antidiabetic Activity" },
    { value: "Anticancer", label: "Anticancer/Cytotoxic Activity" },
    { value: "Analgesic", label: "Analgesic Activity" },
    { value: "Hepatoprotective", label: "Hepatoprotective Activity" },
    { value: "Immunomodulatory", label: "Immunomodulatory Activity" },
    { value: "Wound Healing", label: "Wound Healing Activity" },
    { value: "Other", label: "Other" },
  ];

  // Efficacy outcome options
  const efficacyOutcomes = [
    {
      value: "",
      label: intl.formatMessage({
        id: "common.select",
        defaultMessage: "Select...",
      }),
    },
    { value: "Highly Effective", label: "Highly Effective" },
    { value: "Moderately Effective", label: "Moderately Effective" },
    { value: "Weakly Effective", label: "Weakly Effective" },
    { value: "Not Effective", label: "Not Effective" },
    { value: "Inconclusive", label: "Inconclusive" },
  ];

  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    return () => {
      componentMounted.current = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [entryId, pageData?.id]);

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
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
              status: sample.pageStatus || sample.status || "PENDING",
              localName: sample.data?.localName,
              extractId: sample.data?.extractId,
              // Phytochemical screening
              phytochemicalAlkaloids: sample.data?.phytochemicalAlkaloids,
              phytochemicalFlavonoids: sample.data?.phytochemicalFlavonoids,
              phytochemicalTannins: sample.data?.phytochemicalTannins,
              phytochemicalSaponins: sample.data?.phytochemicalSaponins,
              phytochemicalGlycosides: sample.data?.phytochemicalGlycosides,
              phytochemicalTerpenoids: sample.data?.phytochemicalTerpenoids,
              phytochemicalSteroids: sample.data?.phytochemicalSteroids,
              phytochemicalPhenols: sample.data?.phytochemicalPhenols,
              testsPerformed: sample.data?.testsPerformed,
              // Safety/Toxicity
              safetyStudyType: sample.data?.safetyStudyType,
              toxicityModel: sample.data?.toxicityModel,
              toxicityOutcome: sample.data?.toxicityOutcome,
              // Efficacy
              biologicalAssayType: sample.data?.biologicalAssayType,
              assayProtocol: sample.data?.assayProtocol,
              efficacyOutcome: sample.data?.efficacyOutcome,
              // General
              testedBy: sample.data?.testedBy,
              testDate: sample.data?.testDate,
              testingNotes: sample.data?.testingNotes,
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

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Open bulk apply modal
  const openBulkModal = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    setBulkFormData({
      phytochemicalAlkaloids: false,
      phytochemicalFlavonoids: false,
      phytochemicalTannins: false,
      phytochemicalSaponins: false,
      phytochemicalGlycosides: false,
      phytochemicalTerpenoids: false,
      phytochemicalSteroids: false,
      phytochemicalPhenols: false,
      safetyStudyType: "",
      toxicityModel: "",
      toxicityOutcome: "",
      biologicalAssayType: "",
      assayProtocol: "",
      efficacyOutcome: "",
      testedBy: "",
      testDate: "",
      testingNotes: "",
    });
    setBulkModalOpen(true);
  }, [selectedSampleIds, intl]);

  // Apply bulk testing data
  const applyBulkTesting = useCallback(() => {
    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    // Build phytochemical tests list
    const phytochemicalTests = [];
    if (bulkFormData.phytochemicalAlkaloids)
      phytochemicalTests.push("Alkaloids");
    if (bulkFormData.phytochemicalFlavonoids)
      phytochemicalTests.push("Flavonoids");
    if (bulkFormData.phytochemicalTannins) phytochemicalTests.push("Tannins");
    if (bulkFormData.phytochemicalSaponins) phytochemicalTests.push("Saponins");
    if (bulkFormData.phytochemicalGlycosides)
      phytochemicalTests.push("Glycosides");
    if (bulkFormData.phytochemicalTerpenoids)
      phytochemicalTests.push("Terpenoids");
    if (bulkFormData.phytochemicalSteroids) phytochemicalTests.push("Steroids");
    if (bulkFormData.phytochemicalPhenols) phytochemicalTests.push("Phenols");

    setError(null);
    setSuccessMessage(null);

    const apiRequestData = {
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      // Phytochemical screening
      detectedPhytochemicals: phytochemicalTests,
      // Safety/Toxicity
      safetyStudyType: bulkFormData.safetyStudyType || null,
      toxicityModel: bulkFormData.toxicityModel || null,
      toxicityOutcome: bulkFormData.toxicityOutcome || null,
      // Efficacy
      biologicalAssayType: bulkFormData.biologicalAssayType || null,
      assayProtocol: bulkFormData.assayProtocol || null,
      efficacyOutcome: bulkFormData.efficacyOutcome || null,
      // General
      testedBy: bulkFormData.testedBy || null,
      testDate: bulkFormData.testDate || null,
      notes: bulkFormData.testingNotes || null,
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/tradmed/page/${pageData.id}/testing`,
      JSON.stringify(apiRequestData),
      (response) => {
        if (response && response.success) {
          setSuccessMessage(
            response.message ||
              intl.formatMessage(
                {
                  id: "notebook.page.tradmed.success.testingApplied",
                  defaultMessage: "Applied testing data to {count} sample(s).",
                },
                { count: response.updatedCount || selectedSampleIds.length },
              ),
          );
          setBulkModalOpen(false);
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          setError(
            response?.error ||
              intl.formatMessage({
                id: "notebook.page.tradmed.error.bulkApply",
                defaultMessage:
                  "Failed to apply testing data. Please try again.",
              }),
          );
        }
      },
    );
  }, [
    hasRealPageId,
    bulkFormData,
    selectedSampleIds,
    intl,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Mark as approved for formulation
  const markAsApproved = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    // Validate selected samples have testing data
    const selectedSamples = samples.filter((s) =>
      selectedSampleIds.includes(s.id),
    );
    const incompleteCount = selectedSamples.filter(
      (s) => !s.toxicityOutcome && !s.efficacyOutcome,
    ).length;
    if (incompleteCount > 0) {
      setError(
        intl.formatMessage(
          {
            id: "notebook.page.tradmed.error.incompleteTesting",
            defaultMessage:
              "{count} sample(s) missing testing results. Apply testing data first.",
          },
          { count: incompleteCount },
        ),
      );
      return;
    }

    setError(null);
    setSuccessMessage(null);

    // Optimistic UI update
    setSamples((prevSamples) =>
      prevSamples.map((sample) =>
        selectedSampleIds.includes(sample.id)
          ? { ...sample, status: "COMPLETED" }
          : sample,
      ),
    );

    postToOpenElisServerJsonResponse(
      `/rest/notebook/tradmed/page/${pageData.id}/testing/approve`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      }),
      (response) => {
        if (response && response.success) {
          setSuccessMessage(
            response.message ||
              intl.formatMessage(
                {
                  id: "notebook.page.tradmed.success.approved",
                  defaultMessage: "Approved {count} sample(s) for formulation.",
                },
                { count: response.approvedCount || selectedSampleIds.length },
              ),
          );
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          // Revert optimistic update on error
          loadPageSamples();
          setError(
            response?.error ||
              intl.formatMessage({
                id: "notebook.page.tradmed.error.status",
                defaultMessage: "Failed to update samples. Please try again.",
              }),
          );
        }
      },
    );
  }, [
    selectedSampleIds,
    hasRealPageId,
    samples,
    intl,
    loadPageSamples,
    onProgressUpdate,
    pageData?.id,
  ]);

  // Mark as requiring further testing
  const markAsFurtherTesting = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    setError(null);
    setSuccessMessage(null);

    postToOpenElisServerJsonResponse(
      `/rest/notebook/tradmed/page/${pageData.id}/testing/further`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      }),
      (response) => {
        if (response && response.success) {
          setSuccessMessage(
            response.message ||
              intl.formatMessage(
                {
                  id: "notebook.page.tradmed.success.furtherTesting",
                  defaultMessage:
                    "Marked {count} sample(s) for further testing.",
                },
                { count: response.updatedCount || selectedSampleIds.length },
              ),
          );
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          setError(
            response?.error ||
              intl.formatMessage({
                id: "notebook.page.tradmed.error.status",
                defaultMessage: "Failed to update samples. Please try again.",
              }),
          );
        }
      },
    );
  }, [
    selectedSampleIds,
    hasRealPageId,
    intl,
    loadPageSamples,
    onProgressUpdate,
    pageData?.id,
  ]);

  // Mark as rejected (failed testing)
  const markAsRejected = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    setError(null);
    setSuccessMessage(null);

    postToOpenElisServerJsonResponse(
      `/rest/notebook/tradmed/page/${pageData.id}/testing/reject`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      }),
      (response) => {
        if (response && response.success) {
          setSuccessMessage(
            response.message ||
              intl.formatMessage(
                {
                  id: "notebook.page.tradmed.success.rejected",
                  defaultMessage:
                    "Rejected {count} sample(s) - failed safety/efficacy testing.",
                },
                { count: response.rejectedCount || selectedSampleIds.length },
              ),
          );
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          setError(
            response?.error ||
              intl.formatMessage({
                id: "notebook.page.tradmed.error.status",
                defaultMessage: "Failed to update samples. Please try again.",
              }),
          );
        }
      },
    );
  }, [
    selectedSampleIds,
    hasRealPageId,
    intl,
    loadPageSamples,
    onProgressUpdate,
    pageData?.id,
  ]);

  const pendingCount = samples.filter((s) => s.status === "PENDING").length;
  const completedCount = samples.filter((s) => s.status === "COMPLETED").length;
  const inProgressCount = samples.filter(
    (s) => s.status === "IN_PROGRESS",
  ).length;
  const rejectedCount = samples.filter((s) => s.status === "REJECTED").length;

  // Count samples without testing results (cannot be approved)
  const incompleteTestingCount = samples.filter(
    (s) =>
      s.status !== "COMPLETED" &&
      s.status !== "REJECTED" &&
      (!s.toxicityOutcome || s.toxicityOutcome === "") &&
      (!s.efficacyOutcome || s.efficacyOutcome === ""),
  ).length;

  // Render testing info as tags
  const renderTestingInfo = (sample) => {
    const tags = [];
    if (sample.toxicityOutcome) {
      const toxColor =
        sample.toxicityOutcome === "Safe"
          ? "green"
          : sample.toxicityOutcome === "Low Toxicity"
            ? "teal"
            : sample.toxicityOutcome === "Moderate Toxicity"
              ? "orange"
              : sample.toxicityOutcome === "High Toxicity"
                ? "red"
                : "gray";
      tags.push(
        <Tag key="tox" type={toxColor} size="sm">
          {sample.toxicityOutcome}
        </Tag>,
      );
    }
    if (sample.efficacyOutcome) {
      const effColor =
        sample.efficacyOutcome === "Highly Effective"
          ? "green"
          : sample.efficacyOutcome === "Moderately Effective"
            ? "teal"
            : sample.efficacyOutcome === "Weakly Effective"
              ? "orange"
              : sample.efficacyOutcome === "Not Effective"
                ? "red"
                : "gray";
      tags.push(
        <Tag key="eff" type={effColor} size="sm">
          {sample.efficacyOutcome}
        </Tag>,
      );
    }
    if (sample.biologicalAssayType) {
      tags.push(
        <Tag key="assay" type="blue" size="sm">
          {sample.biologicalAssayType}
        </Tag>,
      );
    }
    return tags.length > 0 ? tags : "-";
  };

  // Custom column renderer for testing data
  const enhancedSamples = samples.map((sample) => ({
    ...sample,
    testingInfo: renderTestingInfo(sample),
  }));

  return (
    <div className="tradmed-testing-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.tradmed.testing.title"
            defaultMessage="Product Development &amp; Testing"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.tradmed.testing.description"
            defaultMessage="Assess safety, efficacy, and readiness for formulation through phytochemical screening, toxicity studies, and efficacy testing."
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
                  id="notebook.page.tradmed.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.pendingTesting"
                  defaultMessage="Pending Testing"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
            <Tile className="progress-tile in-progress">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.furtherTesting"
                  defaultMessage="Further Testing"
                />
              </span>
              <span className="progress-value">{inProgressCount}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.approved"
                  defaultMessage="Approved"
                />
              </span>
              <span className="progress-value">{completedCount}</span>
            </Tile>
            <Tile className="progress-tile rejected">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.rejected"
                  defaultMessage="Rejected"
                />
              </span>
              <span className="progress-value">{rejectedCount}</span>
            </Tile>
            {incompleteTestingCount > 0 && (
              <Tile
                className="progress-tile"
                style={{
                  backgroundColor: "#fff1f1",
                  border: "1px solid #da1e28",
                }}
              >
                <span className="progress-label" style={{ color: "#da1e28" }}>
                  <FormattedMessage
                    id="notebook.page.tradmed.incompleteTesting"
                    defaultMessage="Missing Test Results"
                  />
                </span>
                <span className="progress-value" style={{ color: "#da1e28" }}>
                  {incompleteTestingCount}
                </span>
              </Tile>
            )}
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        {selectedSampleIds.length > 0 && (
          <>
            <Button
              kind="tertiary"
              size="sm"
              renderIcon={Edit}
              onClick={openBulkModal}
            >
              <FormattedMessage
                id="notebook.page.tradmed.applyTesting"
                defaultMessage="Apply Testing Data ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>
            <Button
              kind="primary"
              size="sm"
              renderIcon={Checkmark}
              onClick={markAsApproved}
            >
              <FormattedMessage
                id="notebook.page.tradmed.markApproved"
                defaultMessage="Approve for Formulation ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>
            <Button
              kind="secondary"
              size="sm"
              renderIcon={Chemistry}
              onClick={markAsFurtherTesting}
            >
              <FormattedMessage
                id="notebook.page.tradmed.markFurtherTesting"
                defaultMessage="Further Testing ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>
            <Button
              kind="danger--ghost"
              size="sm"
              renderIcon={Close}
              onClick={markAsRejected}
            >
              <FormattedMessage
                id="notebook.page.tradmed.markRejected"
                defaultMessage="Reject ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>
          </>
        )}
      </div>

      {/* Errors / Success */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          hideCloseButton
          lowContrast
        />
      )}
      {successMessage && (
        <InlineNotification
          kind="success"
          title={successMessage}
          hideCloseButton
          lowContrast
        />
      )}

      {/* Sample Grid */}
      <div className="sample-grid-container">
        <SampleGrid
          samples={enhancedSamples}
          selectedIds={selectedSampleIds}
          onSelectionChange={setSelectedSampleIds}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          showSelection={true}
          loading={loading}
          columns={[
            { key: "accessionNumber", header: "Accession Number" },
            { key: "externalId", header: "Sample ID" },
            { key: "localName", header: "Local Name" },
            { key: "testsPerformed", header: "Phytochemical Tests" },
            { key: "testingInfo", header: "Results" },
            { key: "testedBy", header: "Tested By" },
            { key: "status", header: "Status" },
          ]}
        />
      </div>

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.page.tradmed.testing.empty"
              defaultMessage="No samples pending testing. Complete extraction or analytical pathway first."
            />
          </p>
        </div>
      )}

      {/* Bulk Apply Modal */}
      <Modal
        open={bulkModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.page.tradmed.testing.modal.title",
          defaultMessage: "Apply Testing Data",
        })}
        primaryButtonText={intl.formatMessage({
          id: "common.apply",
          defaultMessage: "Apply",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setBulkModalOpen(false)}
        onRequestSubmit={applyBulkTesting}
        size="lg"
      >
        <p className="modal-description">
          <FormattedMessage
            id="notebook.page.tradmed.testing.modal.description"
            defaultMessage="Apply testing data to {count} selected sample(s)."
            values={{ count: selectedSampleIds.length }}
          />
        </p>

        <Grid fullWidth narrow className="modal-form-grid">
          {/* Phytochemical Screening Section */}
          <Column lg={16} md={8} sm={4}>
            <div className="checkbox-group">
              <span className="checkbox-group-label">
                <FormattedMessage
                  id="notebook.page.tradmed.testing.phytochemicalScreening"
                  defaultMessage="Phytochemical Screening (Select detected compounds)"
                />
              </span>
              <div className="checkbox-group-items checkbox-grid">
                <Checkbox
                  id="phytochemicalAlkaloids"
                  labelText="Alkaloids"
                  checked={bulkFormData.phytochemicalAlkaloids}
                  onChange={(_, { checked }) =>
                    setBulkFormData({
                      ...bulkFormData,
                      phytochemicalAlkaloids: checked,
                    })
                  }
                />
                <Checkbox
                  id="phytochemicalFlavonoids"
                  labelText="Flavonoids"
                  checked={bulkFormData.phytochemicalFlavonoids}
                  onChange={(_, { checked }) =>
                    setBulkFormData({
                      ...bulkFormData,
                      phytochemicalFlavonoids: checked,
                    })
                  }
                />
                <Checkbox
                  id="phytochemicalTannins"
                  labelText="Tannins"
                  checked={bulkFormData.phytochemicalTannins}
                  onChange={(_, { checked }) =>
                    setBulkFormData({
                      ...bulkFormData,
                      phytochemicalTannins: checked,
                    })
                  }
                />
                <Checkbox
                  id="phytochemicalSaponins"
                  labelText="Saponins"
                  checked={bulkFormData.phytochemicalSaponins}
                  onChange={(_, { checked }) =>
                    setBulkFormData({
                      ...bulkFormData,
                      phytochemicalSaponins: checked,
                    })
                  }
                />
                <Checkbox
                  id="phytochemicalGlycosides"
                  labelText="Glycosides"
                  checked={bulkFormData.phytochemicalGlycosides}
                  onChange={(_, { checked }) =>
                    setBulkFormData({
                      ...bulkFormData,
                      phytochemicalGlycosides: checked,
                    })
                  }
                />
                <Checkbox
                  id="phytochemicalTerpenoids"
                  labelText="Terpenoids"
                  checked={bulkFormData.phytochemicalTerpenoids}
                  onChange={(_, { checked }) =>
                    setBulkFormData({
                      ...bulkFormData,
                      phytochemicalTerpenoids: checked,
                    })
                  }
                />
                <Checkbox
                  id="phytochemicalSteroids"
                  labelText="Steroids"
                  checked={bulkFormData.phytochemicalSteroids}
                  onChange={(_, { checked }) =>
                    setBulkFormData({
                      ...bulkFormData,
                      phytochemicalSteroids: checked,
                    })
                  }
                />
                <Checkbox
                  id="phytochemicalPhenols"
                  labelText="Phenols"
                  checked={bulkFormData.phytochemicalPhenols}
                  onChange={(_, { checked }) =>
                    setBulkFormData({
                      ...bulkFormData,
                      phytochemicalPhenols: checked,
                    })
                  }
                />
              </div>
            </div>
          </Column>

          {/* Safety/Toxicity Section */}
          <Column lg={16} md={8} sm={4}>
            <h5 className="modal-section-title">
              <FormattedMessage
                id="notebook.page.tradmed.testing.safetySection"
                defaultMessage="Safety &amp; Toxicity Study"
              />
            </h5>
          </Column>
          <Column lg={5} md={4} sm={4}>
            <Select
              id="safetyStudyType"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.testing.safetyStudyType",
                defaultMessage: "Study Type",
              })}
              value={bulkFormData.safetyStudyType}
              onChange={(e) =>
                setBulkFormData({
                  ...bulkFormData,
                  safetyStudyType: e.target.value,
                })
              }
            >
              {safetyStudyTypes.map((opt) => (
                <SelectItem
                  key={opt.value}
                  value={opt.value}
                  text={opt.label}
                />
              ))}
            </Select>
          </Column>
          <Column lg={5} md={4} sm={4}>
            <Select
              id="toxicityModel"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.testing.toxicityModel",
                defaultMessage: "Model/System",
              })}
              value={bulkFormData.toxicityModel}
              onChange={(e) =>
                setBulkFormData({
                  ...bulkFormData,
                  toxicityModel: e.target.value,
                })
              }
            >
              {toxicityModels.map((opt) => (
                <SelectItem
                  key={opt.value}
                  value={opt.value}
                  text={opt.label}
                />
              ))}
            </Select>
          </Column>
          <Column lg={6} md={4} sm={4}>
            <Select
              id="toxicityOutcome"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.testing.toxicityOutcome",
                defaultMessage: "Toxicity Outcome",
              })}
              value={bulkFormData.toxicityOutcome}
              onChange={(e) =>
                setBulkFormData({
                  ...bulkFormData,
                  toxicityOutcome: e.target.value,
                })
              }
            >
              {toxicityOutcomes.map((opt) => (
                <SelectItem
                  key={opt.value}
                  value={opt.value}
                  text={opt.label}
                />
              ))}
            </Select>
          </Column>

          {/* Efficacy Section */}
          <Column lg={16} md={8} sm={4}>
            <h5 className="modal-section-title">
              <FormattedMessage
                id="notebook.page.tradmed.testing.efficacySection"
                defaultMessage="Efficacy Testing"
              />
            </h5>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="biologicalAssayType"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.testing.biologicalAssayType",
                defaultMessage: "Biological Assay Type",
              })}
              value={bulkFormData.biologicalAssayType}
              onChange={(e) =>
                setBulkFormData({
                  ...bulkFormData,
                  biologicalAssayType: e.target.value,
                })
              }
            >
              {biologicalAssayTypes.map((opt) => (
                <SelectItem
                  key={opt.value}
                  value={opt.value}
                  text={opt.label}
                />
              ))}
            </Select>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="efficacyOutcome"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.testing.efficacyOutcome",
                defaultMessage: "Efficacy Outcome",
              })}
              value={bulkFormData.efficacyOutcome}
              onChange={(e) =>
                setBulkFormData({
                  ...bulkFormData,
                  efficacyOutcome: e.target.value,
                })
              }
            >
              {efficacyOutcomes.map((opt) => (
                <SelectItem
                  key={opt.value}
                  value={opt.value}
                  text={opt.label}
                />
              ))}
            </Select>
          </Column>
          <Column lg={16} md={8} sm={4}>
            <TextInput
              id="assayProtocol"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.testing.assayProtocol",
                defaultMessage: "Assay Protocol Reference",
              })}
              placeholder={intl.formatMessage({
                id: "notebook.page.tradmed.testing.assayProtocolPlaceholder",
                defaultMessage: "Protocol ID or reference",
              })}
              value={bulkFormData.assayProtocol}
              onChange={(e) =>
                setBulkFormData({
                  ...bulkFormData,
                  assayProtocol: e.target.value,
                })
              }
            />
          </Column>

          {/* General Section */}
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="testedBy"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.testing.testedBy",
                defaultMessage: "Tested By",
              })}
              value={bulkFormData.testedBy}
              onChange={(e) =>
                setBulkFormData({ ...bulkFormData, testedBy: e.target.value })
              }
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="testDate"
              type="date"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.testing.testDate",
                defaultMessage: "Test Date",
              })}
              value={bulkFormData.testDate}
              onChange={(e) =>
                setBulkFormData({ ...bulkFormData, testDate: e.target.value })
              }
            />
          </Column>

          {/* Notes */}
          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="testingNotes"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.testing.notes",
                defaultMessage: "Testing Notes",
              })}
              value={bulkFormData.testingNotes}
              onChange={(e) =>
                setBulkFormData({
                  ...bulkFormData,
                  testingNotes: e.target.value,
                })
              }
              rows={3}
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default TraditionalMedicineTestingPage;
