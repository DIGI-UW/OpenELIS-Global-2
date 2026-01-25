import React, {
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
  Tile,
  Tag,
  Modal,
  Dropdown,
  TextArea,
  Loading,
  Checkbox,
} from "@carbon/react";
import {
  Renew,
  CheckmarkFilled,
  Edit,
  Archive,
  Pending,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import { usePermissions } from "../../../../hooks/usePermissions";
import { useTMMRDPermissions } from "../../../../hooks/useTMMRDPermissions";
import AccessDeniedMessage from "../../../common/AccessDeniedMessage";
import "../../workflow/NotebookWorkflow.css";

/**
 * TraditionalMedicineTestingPage - Page 6 of the Traditional Medicine workflow.
 *
 * SRS Requirements - STAGE 7: Product Development & Testing
 * - Phytochemical screening (alkaloids, flavonoids, tannins, saponins, terpenoids, glycosides)
 * - Safety/Toxicity study
 * - Efficacy testing (antimicrobial, antioxidant, anti-inflammatory, anticancer)
 * - Three-way approval system
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 */
function TraditionalMedicineTestingPage({
  entryId,
  pageData,
  progress: _progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const componentMounted = useRef(false);
  const { hasAnyRole } = usePermissions();

  // TMMRD permissions per SRS Section 11
  const { getPagePermissionLevel, canSaveData, canAccessStage5to6 } =
    useTMMRDPermissions();

  // STAGE 5-6 allowed roles per TMMRD SRS Section 11 - Researchers lead analytics
  const allowedRoles = [
    "Researcher",
    "Pharmacognosist",
    "Lab Manager",
    "Principal Investigator",
  ];

  const canAccessPage = hasAnyRole(allowedRoles);

  // Check page access - show access denied if user lacks required roles
  if (!canAccessPage) {
    return (
      <AccessDeniedMessage
        page="Testing & Analytics"
        reason="This page requires specific Traditional Medicine testing roles to access."
        requiredRoles={allowedRoles}
      />
    );
  }

  // Get user's action-level permission for this page
  const pagePermissionLevel = getPagePermissionLevel("Testing & Analytics");
  const canEditData = canSaveData(pagePermissionLevel);

  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);

  const [isApplying, setIsApplying] = useState(false);
  const [isCompleting, setIsCompleting] = useState(false);

  const [assignedTests, setAssignedTests] = useState([]);
  const [testAssignmentModal, setTestAssignmentModal] = useState(false);
  const [assignmentData, setAssignmentData] = useState({
    category: "",
    subcategory: "",
    specificTest: "",
    methodology: "",
    expectedResults: "",
    acceptanceCriteria: "",
  });

  // TMMRD Test Catalog Hierarchy - Based on SRS Sections 4-5
  const tmmrdTestCategories = [
    { id: "BOTANICAL", text: "Botanical Authentication" },
    { id: "PHYTOCHEMICAL", text: "Phytochemical Screening" },
    { id: "ANALYTICAL", text: "Analytical Techniques" },
    { id: "BIOLOGICAL", text: "Biological Activity Assays" },
    { id: "SAFETY", text: "Safety & Toxicity Studies" },
    { id: "PRODUCT_QC", text: "Product Quality Control" },
    { id: "FORMULATION", text: "Formulation Development" },
  ];

  const getTMMRDSubcategories = (categoryId) => {
    switch (categoryId) {
      case "BOTANICAL":
        return [
          { id: "MORPHOLOGICAL", text: "Morphological Authentication" },
          { id: "MICROSCOPIC", text: "Microscopic Authentication" },
          { id: "DNA_BARCODING", text: "DNA Barcoding" },
          { id: "PROTEIN_PROFILING", text: "Protein Profiling" },
        ];
      case "PHYTOCHEMICAL":
        return [
          { id: "TLC_SCREENING", text: "TLC Screening Methods" },
          { id: "CHEMICAL_TESTS", text: "Chemical Tests" },
          { id: "QUANTITATIVE", text: "Quantitative Analysis" },
        ];
      case "ANALYTICAL":
        return [
          { id: "CHROMATOGRAPHY", text: "Chromatographic Methods" },
          { id: "SPECTROSCOPY", text: "Spectroscopic Methods" },
          { id: "MASS_SPECTROMETRY", text: "Mass Spectrometry" },
        ];
      case "BIOLOGICAL":
        return [
          { id: "ANTIMICROBIAL", text: "Antimicrobial Assays" },
          { id: "ANTIOXIDANT", text: "Antioxidant Activity" },
          { id: "ANTI_INFLAMMATORY", text: "Anti-inflammatory Activity" },
          { id: "ANTICANCER", text: "Anticancer Activity" },
        ];
      case "SAFETY":
        return [
          { id: "ACUTE_TOXICITY", text: "Acute Toxicity" },
          { id: "CYTOTOXICITY", text: "Cytotoxicity" },
          { id: "GENOTOXICITY", text: "Genotoxicity" },
        ];
      case "PRODUCT_QC":
        return [
          { id: "CONTAMINATION", text: "Contamination Testing" },
          { id: "STABILITY", text: "Stability Studies" },
          { id: "PURITY", text: "Purity Analysis" },
        ];
      case "FORMULATION":
        return [
          { id: "DEVELOPMENT", text: "Formulation Development" },
          { id: "COMPATIBILITY", text: "Excipient Compatibility" },
          { id: "BIOAVAILABILITY", text: "Bioavailability Studies" },
        ];
      default:
        return [];
    }
  };

  const getTMMRDSpecificTests = (subcategoryId) => {
    switch (subcategoryId) {
      case "TLC_SCREENING":
        return [
          { id: "TLC_ALKALOID", text: "TLC Alkaloid Screening", unit: "" },
          { id: "TLC_FLAVONOID", text: "TLC Flavonoid Screening", unit: "" },
          { id: "TLC_TANNIN", text: "TLC Tannin Screening", unit: "" },
          { id: "TLC_SAPONIN", text: "TLC Saponin Screening", unit: "" },
          { id: "TLC_PHENOLIC", text: "TLC Phenolic Screening", unit: "" },
          { id: "TLC_TERPENOID", text: "TLC Terpenoid Screening", unit: "" },
          { id: "TLC_STEROID", text: "TLC Steroid Screening", unit: "" },
        ];
      case "CHROMATOGRAPHY":
        return [
          {
            id: "HPLC_QUANTITATIVE",
            text: "HPLC Quantitative Analysis",
            unit: "mg/kg",
          },
          { id: "HPLC_FINGERPRINTING", text: "HPLC Fingerprinting", unit: "" },
          { id: "GC_MS", text: "GC-MS Analysis", unit: "mg/kg" },
          { id: "LC_MS", text: "LC-MS Analysis", unit: "mg/kg" },
        ];
      case "ANTIMICROBIAL":
        return [
          {
            id: "ANTIMICROBIAL_ECOLI",
            text: "Antimicrobial Assay E.coli",
            unit: "mm",
          },
          {
            id: "ANTIMICROBIAL_SAUREUS",
            text: "Antimicrobial Assay S.aureus",
            unit: "mm",
          },
          {
            id: "ANTIMICROBIAL_PAERUGINOSA",
            text: "Antimicrobial Assay P.aeruginosa",
            unit: "mm",
          },
          {
            id: "ANTIMICROBIAL_CALBICANS",
            text: "Antimicrobial Assay C.albicans",
            unit: "mm",
          },
        ];
      case "ANTIOXIDANT":
        return [
          {
            id: "DPPH_ASSAY",
            text: "DPPH Antioxidant Assay",
            unit: "IC50 μg/mL",
          },
          {
            id: "ABTS_ASSAY",
            text: "ABTS Antioxidant Assay",
            unit: "IC50 μg/mL",
          },
        ];
      case "CONTAMINATION":
        return [
          { id: "HEAVY_METALS_LEAD", text: "Heavy Metals Lead", unit: "ppm" },
          {
            id: "HEAVY_METALS_MERCURY",
            text: "Heavy Metals Mercury",
            unit: "ppm",
          },
          {
            id: "PESTICIDE_RESIDUE",
            text: "Pesticide Residue Screen",
            unit: "ppm",
          },
          {
            id: "MICROBIAL_TOTAL",
            text: "Total Microbial Count",
            unit: "CFU/g",
          },
          { id: "AFLATOXIN_B1", text: "Aflatoxin B1 Analysis", unit: "ppb" },
        ];
      case "ACUTE_TOXICITY":
        return [
          {
            id: "LD50_ORAL",
            text: "Acute Toxicity LD50 (Oral)",
            unit: "mg/kg",
          },
          {
            id: "LD50_DERMAL",
            text: "Acute Toxicity LD50 (Dermal)",
            unit: "mg/kg",
          },
        ];
      default:
        return [];
    }
  };

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
              ? samplesToProcess.map((s) => ({
                  id: String(s.id || s.sampleItemId),
                  externalId: s.externalId,
                  accessionNumber: s.accessionNumber,
                  status: s.pageStatus || s.status || "PENDING",
                  localName: s.data?.localName,
                  assignedTests: s.data?.assignedTests || [],
                  testingStatus:
                    s.data?.assignedTests?.length > 0 ? "ASSIGNED" : "PENDING",
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

  const resetAssignmentForm = useCallback(() => {
    setAssignmentData({
      category: "",
      subcategory: "",
      specificTest: "",
      methodology: "",
      expectedResults: "",
      acceptanceCriteria: "",
    });
  }, []);

  const openTestAssignmentModal = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      });
      return;
    }
    resetAssignmentForm();
    setTestAssignmentModal(true);
  }, [selectedSampleIds, intl, resetAssignmentForm, notify]);

  const handleCategoryChange = useCallback(({ selectedItem }) => {
    setAssignmentData((prev) => ({
      ...prev,
      category: selectedItem?.id || "",
      subcategory: "", // Reset child selections
      specificTest: "",
    }));
  }, []);

  const handleSubcategoryChange = useCallback(({ selectedItem }) => {
    setAssignmentData((prev) => ({
      ...prev,
      subcategory: selectedItem?.id || "",
      specificTest: "", // Reset child selection
    }));
  }, []);

  const handleSpecificTestChange = useCallback(({ selectedItem }) => {
    setAssignmentData((prev) => ({
      ...prev,
      specificTest: selectedItem?.id || "",
    }));
  }, []);

  const assignTestsToSamples = useCallback(() => {
    if (!assignmentData.specificTest) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.testing.error.testRequired",
          defaultMessage: "Please select a specific test.",
        }),
      });
      return;
    }

    if (!hasRealPageId) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.error.noPage",
          defaultMessage: "Cannot assign tests: Page not properly initialized.",
        }),
      });
      return;
    }

    setIsApplying(true);

    const sampleIds = selectedSampleIds.map((id) => parseInt(id, 10));
    const selectedTest = getTMMRDSpecificTests(assignmentData.subcategory).find(
      (test) => test.id === assignmentData.specificTest,
    );

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds,
        data: {
          assignedTests: [
            ...assignedTests,
            {
              testId: assignmentData.specificTest,
              testName: selectedTest?.text,
              category: assignmentData.category,
              subcategory: assignmentData.subcategory,
              unit: selectedTest?.unit,
              methodology: assignmentData.methodology,
              expectedResults: assignmentData.expectedResults,
              acceptanceCriteria: assignmentData.acceptanceCriteria,
              status: "ASSIGNED",
              assignedAt: new Date().toISOString(),
            },
          ],
        },
      }),
      (response) => {
        setIsApplying(false);
        if (response?.success !== false) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage(
              {
                id: "notebook.page.tradmed.testing.assignSuccess",
                defaultMessage:
                  "Assigned test {testName} to {count} sample(s).",
              },
              {
                testName: selectedTest?.text,
                count: selectedSampleIds.length,
              },
            ),
          });
          setTestAssignmentModal(false);
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          notify({
            kind: NotificationKinds.error,
            title: response?.error || "Test assignment failed",
          });
        }
      },
    );
  }, [
    assignmentData,
    assignedTests,
    hasRealPageId,
    pageData?.id,
    selectedSampleIds,
    intl,
    loadPageSamples,
    onProgressUpdate,
    notify,
  ]);

  // Handle marking tested samples complete (moving to next page)
  const handleMarkComplete = useCallback(() => {
    // Filter samples that can be marked complete: selected, have tests assigned, and not already completed
    const samplesToComplete = samples.filter(
      (s) =>
        selectedSampleIds.includes(s.id) &&
        s.assignedTests &&
        s.assignedTests.length > 0 &&
        s.status !== "COMPLETED",
    );

    if (samplesToComplete.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.tradmed.testing.noEligibleSamples",
          defaultMessage:
            "Selected samples must have tests assigned before completing.",
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
                id: "notebook.tradmed.testing.completeSuccess",
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
                id: "notebook.tradmed.testing.completeFailed",
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

  const pendingSamples = useMemo(
    () => samples.filter((s) => s.testingStatus === "PENDING"),
    [samples],
  );
  const assignedInProgressSamples = useMemo(
    () =>
      samples.filter(
        (s) => s.testingStatus === "ASSIGNED" && s.status !== "COMPLETED",
      ),
    [samples],
  );
  const assignedCompletedSamples = useMemo(
    () =>
      samples.filter(
        (s) => s.testingStatus === "ASSIGNED" && s.status === "COMPLETED",
      ),
    [samples],
  );

  // Helper to render sample status - simple status display matching API response
  const renderStatus = (sample) => {
    const status = sample.status || "PENDING";

    switch (status.toUpperCase()) {
      case "COMPLETED":
        return (
          <Tag type="green" size="sm" renderIcon={CheckmarkFilled}>
            <FormattedMessage
              id="notebook.tradmed.status.completed"
              defaultMessage="Completed"
            />
          </Tag>
        );
      case "IN_PROGRESS":
        return (
          <Tag type="blue" size="sm" renderIcon={Archive}>
            <FormattedMessage
              id="notebook.tradmed.status.inProgress"
              defaultMessage="In Progress"
            />
          </Tag>
        );
      case "SKIPPED":
        return (
          <Tag type="gray" size="sm">
            <FormattedMessage
              id="notebook.tradmed.status.skipped"
              defaultMessage="Skipped"
            />
          </Tag>
        );
      default:
        return (
          <Tag type="gray" size="sm" renderIcon={Pending}>
            <FormattedMessage
              id="notebook.tradmed.status.pending"
              defaultMessage="Pending"
            />
          </Tag>
        );
    }
  };

  return (
    <div className="tradmed-testing-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.tradmed.testing.title"
            defaultMessage="Product Development & Testing"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.tradmed.testing.description"
            defaultMessage="Perform phytochemical screening, safety/toxicity, and efficacy testing with approval workflow."
          />
        </p>
      </div>

      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.testing.pending"
                  defaultMessage="Awaiting Test Assignment"
                />
              </span>
              <span className="progress-value">{pendingSamples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.testing.assigned"
                  defaultMessage="Tests Assigned"
                />
              </span>
              <span className="progress-value">
                {assignedInProgressSamples.length}
              </span>
            </Tile>
          </div>
        </Column>
      </Grid>

      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Edit}
          onClick={openTestAssignmentModal}
          disabled={selectedSampleIds.length === 0 || !hasRealPageId}
        >
          <FormattedMessage
            id="notebook.page.tradmed.testing.assignTests"
            defaultMessage="Assign Tests ({count})"
            values={{ count: selectedSampleIds.length }}
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
            id="notebook.page.tradmed.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.tradmed.testing.pending.title"
              defaultMessage="Samples Awaiting Test Assignment"
            />
            <Tag type="blue" size="sm" className="count-tag">
              {pendingSamples.length}
            </Tag>
          </h5>
        </div>
        <div className="sample-grid-container">
          {!loading && pendingSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.tradmed.testing.pending.empty"
                  defaultMessage="No samples awaiting test assignment."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="pending-testing"
              samples={pendingSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              showSelection={true}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "localName", header: "Local Name" },
                {
                  key: "status",
                  header: intl.formatMessage({
                    id: "notebook.tradmed.column.status",
                    defaultMessage: "Status",
                  }),
                  render: (_value, sample) => renderStatus(sample),
                },
              ]}
            />
          )}
        </div>
      </div>

      {/* Assigned Tests Section - IN PROGRESS */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              width: "100%",
            }}
          >
            <div>
              <h5>
                <FormattedMessage
                  id="notebook.page.tradmed.testing.assigned.inProgress.title"
                  defaultMessage="Tests Assigned (Pending Completion)"
                />
                <Tag type="blue" size="sm" className="count-tag">
                  {assignedInProgressSamples.length}
                </Tag>
              </h5>
            </div>
            {selectedSampleIds.length > 0 && (
              <Button
                kind="tertiary"
                size="sm"
                renderIcon={CheckmarkFilled}
                onClick={handleMarkComplete}
                disabled={isCompleting || !pageData?.id}
              >
                <FormattedMessage
                  id="notebook.tradmed.testing.markComplete"
                  defaultMessage="Mark Complete ({count})"
                  values={{ count: selectedSampleIds.length }}
                />
              </Button>
            )}
          </div>
        </div>
        <div className="sample-grid-container">
          {!loading && assignedInProgressSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.tradmed.testing.assigned.empty"
                  defaultMessage="No tests have been assigned yet."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="assigned-in-progress-samples"
              samples={assignedInProgressSamples}
              onSelectionChange={setSelectedSampleIds}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "localName", header: "Local Name" },
                {
                  key: "status",
                  header: intl.formatMessage({
                    id: "notebook.tradmed.column.status",
                    defaultMessage: "Status",
                  }),
                  render: (_value, sample) => renderStatus(sample),
                },
                {
                  key: "assignedTests",
                  header: "Assigned Tests",
                  render: (sample) => (
                    <div>
                      {sample.assignedTests.map((test, idx) => (
                        <Tag
                          key={idx}
                          type="blue"
                          size="sm"
                          style={{ marginRight: "4px", marginBottom: "2px" }}
                        >
                          {test.testName}
                        </Tag>
                      ))}
                    </div>
                  ),
                },
              ]}
            />
          )}
        </div>
      </div>

      {/* Assigned Tests Section - COMPLETED */}
      {assignedCompletedSamples.length > 0 && (
        <div className="sample-table-section">
          <div className="table-section-header">
            <h5>
              <FormattedMessage
                id="notebook.page.tradmed.testing.assigned.completed.title"
                defaultMessage="Testing Completion Finalized"
              />
              <Tag type="green" size="sm" className="count-tag">
                {assignedCompletedSamples.length}
              </Tag>
            </h5>
          </div>
          <div className="sample-grid-container">
            <SampleGrid
              gridId="assigned-completed-samples"
              samples={assignedCompletedSamples}
              showSelection={false}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "localName", header: "Local Name" },
                {
                  key: "status",
                  header: intl.formatMessage({
                    id: "notebook.tradmed.column.status",
                    defaultMessage: "Status",
                  }),
                  render: (_value, sample) => renderStatus(sample),
                },
                {
                  key: "assignedTests",
                  header: "Assigned Tests",
                  render: (sample) => (
                    <div>
                      {sample.assignedTests.map((test, idx) => (
                        <Tag
                          key={idx}
                          type="blue"
                          size="sm"
                          style={{ marginRight: "4px", marginBottom: "2px" }}
                        >
                          {test.testName}
                        </Tag>
                      ))}
                    </div>
                  ),
                },
              ]}
            />
          </div>
        </div>
      )}

      <Modal
        open={testAssignmentModal}
        onRequestClose={() => setTestAssignmentModal(false)}
        onRequestSubmit={assignTestsToSamples}
        modalHeading={intl.formatMessage({
          id: "notebook.page.tradmed.testing.modal.assign.title",
          defaultMessage: "Assign TMMRD Tests to Samples",
        })}
        primaryButtonText={
          isApplying
            ? intl.formatMessage({
                id: "label.assigning",
                defaultMessage: "Assigning...",
              })
            : intl.formatMessage({
                id: "notebook.page.tradmed.testing.modal.assign.button",
                defaultMessage: "Assign Tests",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        primaryButtonDisabled={isApplying || !assignmentData.specificTest}
        size="lg"
      >
        {isApplying && <Loading withOverlay={false} small />}

        <Grid narrow>
          <Column lg={8} md={4} sm={2}>
            <Dropdown
              id="test-category"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.testing.modal.category",
                defaultMessage: "Test Category *",
              })}
              label="Select category..."
              items={tmmrdTestCategories}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={tmmrdTestCategories.find(
                (c) => c.id === assignmentData.category,
              )}
              onChange={handleCategoryChange}
            />
          </Column>

          <Column lg={8} md={4} sm={2}>
            {assignmentData.category &&
              getTMMRDSubcategories(assignmentData.category).length > 0 && (
                <Dropdown
                  id="test-subcategory"
                  titleText={intl.formatMessage({
                    id: "notebook.page.tradmed.testing.modal.subcategory",
                    defaultMessage: "Test Subcategory *",
                  })}
                  label="Select subcategory..."
                  items={getTMMRDSubcategories(assignmentData.category)}
                  itemToString={(item) => (item ? item.text : "")}
                  selectedItem={getTMMRDSubcategories(
                    assignmentData.category,
                  ).find((sc) => sc.id === assignmentData.subcategory)}
                  onChange={handleSubcategoryChange}
                />
              )}
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            {assignmentData.subcategory &&
              getTMMRDSpecificTests(assignmentData.subcategory).length > 0 && (
                <Dropdown
                  id="specific-test"
                  titleText={intl.formatMessage({
                    id: "notebook.page.tradmed.testing.modal.specificTest",
                    defaultMessage: "Specific Test *",
                  })}
                  label="Select specific test..."
                  items={getTMMRDSpecificTests(assignmentData.subcategory)}
                  itemToString={(item) =>
                    item
                      ? `${item.text}${item.unit ? ` (${item.unit})` : ""}`
                      : ""
                  }
                  selectedItem={getTMMRDSpecificTests(
                    assignmentData.subcategory,
                  ).find((st) => st.id === assignmentData.specificTest)}
                  onChange={handleSpecificTestChange}
                />
              )}
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="methodology"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.testing.modal.methodology",
                defaultMessage: "Test Methodology",
              })}
              value={assignmentData.methodology}
              onChange={(e) =>
                setAssignmentData((prev) => ({
                  ...prev,
                  methodology: e.target.value,
                }))
              }
              rows={3}
              placeholder="Specify the test methodology, protocols, or procedures to be followed..."
            />
          </Column>

          <Column lg={8} md={4} sm={2}>
            <TextArea
              id="expected-results"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.testing.modal.expectedResults",
                defaultMessage: "Expected Results",
              })}
              value={assignmentData.expectedResults}
              onChange={(e) =>
                setAssignmentData((prev) => ({
                  ...prev,
                  expectedResults: e.target.value,
                }))
              }
              rows={3}
              placeholder="Describe expected results or outcome ranges..."
            />
          </Column>

          <Column lg={8} md={4} sm={2}>
            <TextArea
              id="acceptance-criteria"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.testing.modal.acceptanceCriteria",
                defaultMessage: "Acceptance Criteria",
              })}
              value={assignmentData.acceptanceCriteria}
              onChange={(e) =>
                setAssignmentData((prev) => ({
                  ...prev,
                  acceptanceCriteria: e.target.value,
                }))
              }
              rows={3}
              placeholder="Define pass/fail criteria and quality standards..."
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default TraditionalMedicineTestingPage;
