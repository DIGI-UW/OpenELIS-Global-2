import React, {
  useState,
  useEffect,
  useRef,
  useCallback,
  useMemo,
} from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Tag,
  Modal,
  RadioButtonGroup,
  RadioButton,
  TextArea,
  Select,
  SelectItem,
} from "@carbon/react";
import { Checkmark, Close, WarningAlt } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * BacteriologySampleQualityAssessmentPage - Page 2 of the Bacteriology workflow.
 * Handles sample quality assessment after reception verification.
 * Assesses:
 * - Sample integrity (container intact, proper labeling)
 * - Temperature compliance (maintained cold chain)
 * - Volume adequacy
 * - Visual inspection (hemolysis, lipemia, contamination)
 * - Documentation completeness
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function BacteriologySampleQualityAssessmentPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State for samples
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Assessment modal state
  const [assessmentModalOpen, setAssessmentModalOpen] = useState(false);
  const [currentAssessment, setCurrentAssessment] = useState({
    sampleId: null,
    containerIntegrity: "pass",
    labelingCorrect: "pass",
    temperatureCompliant: "pass",
    volumeAdequate: "pass",
    visualInspection: "acceptable",
    overallAssessment: "pass",
    rejectionReason: "",
    notes: "",
  });

  // Rejection reasons for dropdown
  const REJECTION_REASONS = [
    "Container damaged/leaking",
    "Improper labeling",
    "Temperature excursion",
    "Insufficient volume",
    "Hemolyzed sample",
    "Lipemic sample",
    "Contaminated sample",
    "Clotted sample",
    "Expired sample",
    "Missing documentation",
    "Other",
  ];

  // Load samples for this page
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
              collectionDate: sample.collectionDate,
              status: sample.pageStatus || "PENDING",
              // Quality assessment data
              containerIntegrity: sample.data?.containerIntegrity,
              labelingCorrect: sample.data?.labelingCorrect,
              temperatureCompliant: sample.data?.temperatureCompliant,
              volumeAdequate: sample.data?.volumeAdequate,
              visualInspection: sample.data?.visualInspection,
              overallAssessment: sample.data?.overallAssessment,
              rejectionReason: sample.data?.rejectionReason,
              assessmentNotes: sample.data?.assessmentNotes,
              // Reception data for reference
              sampleOrigin: sample.data?.sampleOrigin,
              projectName: sample.data?.projectName,
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

  // Open assessment modal for a sample
  const handleOpenAssessment = useCallback(
    (sampleId) => {
      const sample = samples.find((s) => s.id === sampleId);
      setCurrentAssessment({
        sampleId,
        containerIntegrity: sample?.containerIntegrity || "pass",
        labelingCorrect: sample?.labelingCorrect || "pass",
        temperatureCompliant: sample?.temperatureCompliant || "pass",
        volumeAdequate: sample?.volumeAdequate || "pass",
        visualInspection: sample?.visualInspection || "acceptable",
        overallAssessment: sample?.overallAssessment || "pass",
        rejectionReason: sample?.rejectionReason || "",
        notes: sample?.assessmentNotes || "",
      });
      setAssessmentModalOpen(true);
    },
    [samples],
  );

  // Handle bulk pass assessment
  const handleBulkPassAssessment = useCallback(() => {
    if (selectedSampleIds.length === 0) return;

    const hasRealPageId =
      pageData?.id && !String(pageData.id).startsWith("default-");
    if (!hasRealPageId) {
      setError("Cannot update samples: Page not properly initialized.");
      return;
    }

    // Update page sample data with pass assessment
    const assessmentData = {
      containerIntegrity: "pass",
      labelingCorrect: "pass",
      temperatureCompliant: "pass",
      volumeAdequate: "pass",
      visualInspection: "acceptable",
      overallAssessment: "pass",
    };

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        status: "COMPLETED",
        data: assessmentData,
      }),
      (status) => {
        if (status === 200) {
          loadPageSamples();
          setSelectedSampleIds([]);
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError("Failed to update sample status.");
        }
      },
    );
  }, [selectedSampleIds, pageData?.id, loadPageSamples, onProgressUpdate]);

  // Submit individual assessment
  const handleSubmitAssessment = useCallback(() => {
    if (!currentAssessment.sampleId) return;

    const hasRealPageId =
      pageData?.id && !String(pageData.id).startsWith("default-");
    if (!hasRealPageId) {
      setError("Cannot update samples: Page not properly initialized.");
      return;
    }

    const status =
      currentAssessment.overallAssessment === "pass" ? "COMPLETED" : "REJECTED";
    const assessmentData = {
      containerIntegrity: currentAssessment.containerIntegrity,
      labelingCorrect: currentAssessment.labelingCorrect,
      temperatureCompliant: currentAssessment.temperatureCompliant,
      volumeAdequate: currentAssessment.volumeAdequate,
      visualInspection: currentAssessment.visualInspection,
      overallAssessment: currentAssessment.overallAssessment,
      rejectionReason: currentAssessment.rejectionReason,
      assessmentNotes: currentAssessment.notes,
    };

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: [parseInt(currentAssessment.sampleId, 10)],
        status,
        data: assessmentData,
      }),
      (responseStatus) => {
        if (responseStatus === 200) {
          loadPageSamples();
          setAssessmentModalOpen(false);
          setCurrentAssessment({
            sampleId: null,
            containerIntegrity: "pass",
            labelingCorrect: "pass",
            temperatureCompliant: "pass",
            volumeAdequate: "pass",
            visualInspection: "acceptable",
            overallAssessment: "pass",
            rejectionReason: "",
            notes: "",
          });
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError("Failed to submit assessment.");
        }
      },
    );
  }, [currentAssessment, pageData?.id, loadPageSamples, onProgressUpdate]);

  // Split samples by status
  const pendingSamples = useMemo(
    () => samples.filter((s) => s.status === "PENDING"),
    [samples],
  );
  const passedSamples = useMemo(
    () => samples.filter((s) => s.status === "COMPLETED"),
    [samples],
  );
  const rejectedSamples = useMemo(
    () => samples.filter((s) => s.status === "REJECTED"),
    [samples],
  );

  // Get assessment status badge
  const getAssessmentBadge = (assessment) => {
    if (assessment === "pass") {
      return (
        <Tag type="green" size="sm">
          Pass
        </Tag>
      );
    } else if (assessment === "fail") {
      return (
        <Tag type="red" size="sm">
          Fail
        </Tag>
      );
    }
    return (
      <Tag type="gray" size="sm">
        Pending
      </Tag>
    );
  };

  return (
    <div className="bacteriology-quality-assessment-page pharma-sample-creation-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.bacteriology.qualityAssessment.title"
            defaultMessage="Sample Quality Assessment"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.bacteriology.qualityAssessment.description"
            defaultMessage="Assess sample quality for each received sample. Check container integrity, labeling, temperature compliance, volume adequacy, and visual appearance. Reject samples that do not meet quality standards."
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
                  id="notebook.page.bacteriology.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.bacteriology.passed"
                  defaultMessage="Passed QA"
                />
              </span>
              <span className="progress-value">{passedSamples.length}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.bacteriology.pendingAssessment"
                  defaultMessage="Pending Assessment"
                />
              </span>
              <span className="progress-value">{pendingSamples.length}</span>
            </Tile>
            <Tile className="progress-tile rejected">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.bacteriology.rejected"
                  defaultMessage="Rejected"
                />
              </span>
              <span className="progress-value">{rejectedSamples.length}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        {selectedSampleIds.length > 0 && (
          <Button
            kind="primary"
            size="sm"
            renderIcon={Checkmark}
            onClick={handleBulkPassAssessment}
          >
            <FormattedMessage
              id="notebook.page.bacteriology.bulkPassQA"
              defaultMessage="Pass QA ({count})"
              values={{ count: selectedSampleIds.length }}
            />
          </Button>
        )}
      </div>

      {/* Error Display */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          hideCloseButton
          lowContrast
        />
      )}

      {/* Pending Assessment Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.bacteriology.pendingAssessment.title"
              defaultMessage="Pending Quality Assessment"
            />
            <Tag type="gray" size="sm" className="count-tag">
              {pendingSamples.length}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.bacteriology.pendingAssessment.description"
              defaultMessage="Samples awaiting quality assessment. Click on a sample to perform individual assessment or select multiple samples for bulk pass."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          {!loading && pendingSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.bacteriology.pendingAssessment.empty"
                  defaultMessage="No samples pending quality assessment."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="pending-assessment-samples"
              samples={pendingSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              showSelection={true}
              loading={loading}
              onRowClick={(sample) => handleOpenAssessment(sample.id)}
              additionalColumns={[
                {
                  key: "sampleType",
                  header: intl.formatMessage({
                    id: "notebook.sample.sampleType",
                    defaultMessage: "Sample Type",
                  }),
                  render: (sample) => sample.sampleType || "-",
                },
                {
                  key: "sampleOrigin",
                  header: intl.formatMessage({
                    id: "notebook.sample.sampleOrigin",
                    defaultMessage: "Origin",
                  }),
                  render: (sample) => sample.sampleOrigin || "-",
                },
                {
                  key: "projectName",
                  header: intl.formatMessage({
                    id: "notebook.sample.projectName",
                    defaultMessage: "Project",
                  }),
                  render: (sample) => sample.projectName || "-",
                },
                {
                  key: "action",
                  header: intl.formatMessage({
                    id: "notebook.sample.action",
                    defaultMessage: "Action",
                  }),
                  render: (sample) => (
                    <Button
                      kind="ghost"
                      size="sm"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleOpenAssessment(sample.id);
                      }}
                    >
                      Assess
                    </Button>
                  ),
                },
              ]}
            />
          )}
        </div>
      </div>

      {/* Passed Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.bacteriology.passedSamples.title"
              defaultMessage="Passed Quality Assessment"
            />
            <Tag type="green" size="sm" className="count-tag">
              {passedSamples.length}
            </Tag>
          </h5>
        </div>
        <div className="sample-grid-container">
          {!loading && passedSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.bacteriology.passedSamples.empty"
                  defaultMessage="No samples have passed quality assessment yet."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="passed-samples"
              samples={passedSamples}
              showSelection={false}
              loading={loading}
              additionalColumns={[
                {
                  key: "sampleType",
                  header: intl.formatMessage({
                    id: "notebook.sample.sampleType",
                    defaultMessage: "Sample Type",
                  }),
                  render: (sample) => sample.sampleType || "-",
                },
                {
                  key: "overallAssessment",
                  header: intl.formatMessage({
                    id: "notebook.sample.qaStatus",
                    defaultMessage: "QA Status",
                  }),
                  render: (sample) =>
                    getAssessmentBadge(sample.overallAssessment),
                },
                {
                  key: "projectName",
                  header: intl.formatMessage({
                    id: "notebook.sample.projectName",
                    defaultMessage: "Project",
                  }),
                  render: (sample) => sample.projectName || "-",
                },
              ]}
            />
          )}
        </div>
      </div>

      {/* Rejected Samples Table */}
      {rejectedSamples.length > 0 && (
        <div className="sample-table-section">
          <div className="table-section-header">
            <h5>
              <FormattedMessage
                id="notebook.page.bacteriology.rejectedSamples.title"
                defaultMessage="Rejected Samples"
              />
              <Tag type="red" size="sm" className="count-tag">
                {rejectedSamples.length}
              </Tag>
            </h5>
          </div>
          <div className="sample-grid-container">
            <SampleGrid
              gridId="rejected-samples"
              samples={rejectedSamples}
              showSelection={false}
              loading={loading}
              additionalColumns={[
                {
                  key: "sampleType",
                  header: intl.formatMessage({
                    id: "notebook.sample.sampleType",
                    defaultMessage: "Sample Type",
                  }),
                  render: (sample) => sample.sampleType || "-",
                },
                {
                  key: "rejectionReason",
                  header: intl.formatMessage({
                    id: "notebook.sample.rejectionReason",
                    defaultMessage: "Rejection Reason",
                  }),
                  render: (sample) => sample.rejectionReason || "-",
                },
                {
                  key: "projectName",
                  header: intl.formatMessage({
                    id: "notebook.sample.projectName",
                    defaultMessage: "Project",
                  }),
                  render: (sample) => sample.projectName || "-",
                },
              ]}
            />
          </div>
        </div>
      )}

      {/* Assessment Modal */}
      <Modal
        open={assessmentModalOpen}
        onRequestClose={() => setAssessmentModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.bacteriology.assessment.title",
          defaultMessage: "Sample Quality Assessment",
        })}
        primaryButtonText={intl.formatMessage({
          id: "label.button.submit",
          defaultMessage: "Submit",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.button.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleSubmitAssessment}
        size="md"
      >
        <div className="assessment-form">
          <Grid fullWidth>
            <Column lg={8} md={4} sm={4}>
              <div className="assessment-field">
                <label>Container Integrity</label>
                <RadioButtonGroup
                  name="containerIntegrity"
                  valueSelected={currentAssessment.containerIntegrity}
                  onChange={(value) =>
                    setCurrentAssessment((prev) => ({
                      ...prev,
                      containerIntegrity: value,
                    }))
                  }
                >
                  <RadioButton labelText="Pass" value="pass" />
                  <RadioButton labelText="Fail" value="fail" />
                </RadioButtonGroup>
              </div>
            </Column>

            <Column lg={8} md={4} sm={4}>
              <div className="assessment-field">
                <label>Labeling Correct</label>
                <RadioButtonGroup
                  name="labelingCorrect"
                  valueSelected={currentAssessment.labelingCorrect}
                  onChange={(value) =>
                    setCurrentAssessment((prev) => ({
                      ...prev,
                      labelingCorrect: value,
                    }))
                  }
                >
                  <RadioButton labelText="Pass" value="pass" />
                  <RadioButton labelText="Fail" value="fail" />
                </RadioButtonGroup>
              </div>
            </Column>

            <Column lg={8} md={4} sm={4}>
              <div className="assessment-field">
                <label>Temperature Compliant</label>
                <RadioButtonGroup
                  name="temperatureCompliant"
                  valueSelected={currentAssessment.temperatureCompliant}
                  onChange={(value) =>
                    setCurrentAssessment((prev) => ({
                      ...prev,
                      temperatureCompliant: value,
                    }))
                  }
                >
                  <RadioButton labelText="Pass" value="pass" />
                  <RadioButton labelText="Fail" value="fail" />
                </RadioButtonGroup>
              </div>
            </Column>

            <Column lg={8} md={4} sm={4}>
              <div className="assessment-field">
                <label>Volume Adequate</label>
                <RadioButtonGroup
                  name="volumeAdequate"
                  valueSelected={currentAssessment.volumeAdequate}
                  onChange={(value) =>
                    setCurrentAssessment((prev) => ({
                      ...prev,
                      volumeAdequate: value,
                    }))
                  }
                >
                  <RadioButton labelText="Pass" value="pass" />
                  <RadioButton labelText="Fail" value="fail" />
                </RadioButtonGroup>
              </div>
            </Column>

            <Column lg={16} md={8} sm={4}>
              <div className="assessment-field">
                <label>Visual Inspection</label>
                <RadioButtonGroup
                  name="visualInspection"
                  valueSelected={currentAssessment.visualInspection}
                  onChange={(value) =>
                    setCurrentAssessment((prev) => ({
                      ...prev,
                      visualInspection: value,
                    }))
                  }
                  orientation="horizontal"
                >
                  <RadioButton labelText="Acceptable" value="acceptable" />
                  <RadioButton labelText="Hemolyzed" value="hemolyzed" />
                  <RadioButton labelText="Lipemic" value="lipemic" />
                  <RadioButton labelText="Contaminated" value="contaminated" />
                  <RadioButton labelText="Other Issue" value="other" />
                </RadioButtonGroup>
              </div>
            </Column>

            <Column lg={16} md={8} sm={4}>
              <div className="assessment-field">
                <label>Overall Assessment</label>
                <RadioButtonGroup
                  name="overallAssessment"
                  valueSelected={currentAssessment.overallAssessment}
                  onChange={(value) =>
                    setCurrentAssessment((prev) => ({
                      ...prev,
                      overallAssessment: value,
                    }))
                  }
                >
                  <RadioButton
                    labelText="Pass - Sample meets quality standards"
                    value="pass"
                  />
                  <RadioButton labelText="Fail - Reject sample" value="fail" />
                </RadioButtonGroup>
              </div>
            </Column>

            {currentAssessment.overallAssessment === "fail" && (
              <Column lg={16} md={8} sm={4}>
                <Select
                  id="rejectionReason"
                  labelText="Rejection Reason"
                  value={currentAssessment.rejectionReason}
                  onChange={(e) =>
                    setCurrentAssessment((prev) => ({
                      ...prev,
                      rejectionReason: e.target.value,
                    }))
                  }
                >
                  <SelectItem value="" text="Select reason..." />
                  {REJECTION_REASONS.map((reason) => (
                    <SelectItem key={reason} value={reason} text={reason} />
                  ))}
                </Select>
              </Column>
            )}

            <Column lg={16} md={8} sm={4}>
              <TextArea
                id="assessmentNotes"
                labelText="Notes"
                placeholder="Enter any additional notes about this assessment..."
                value={currentAssessment.notes}
                onChange={(e) =>
                  setCurrentAssessment((prev) => ({
                    ...prev,
                    notes: e.target.value,
                  }))
                }
                rows={3}
              />
            </Column>
          </Grid>
        </div>
      </Modal>
    </div>
  );
}

export default BacteriologySampleQualityAssessmentPage;
