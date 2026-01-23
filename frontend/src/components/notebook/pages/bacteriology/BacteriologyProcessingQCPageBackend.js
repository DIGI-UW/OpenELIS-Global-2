import React, { useState, useEffect, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  SkeletonText,
} from "@carbon/react";
import {
  CheckmarkFilled,
  ChevronLeft,
  ChevronRight,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import BacteriologyQCBackend from "./BacteriologyQCBackend";
import "./BacteriologyQCBackend.css";
import "../../workflow/NotebookWorkflow.css";

/**
 * BacteriologyProcessingQCPageBackend - Simplified backend-powered QC management
 * Replaces the complex frontend logic with API calls to MediaQCRestController
 *
 * This version focuses on:
 * - Simple media selection
 * - Backend-powered QC testing via REST APIs
 * - Automatic quarantine management
 * - Clean separation of concerns
 */

const BacteriologyProcessingQCPageBackend = ({
  pageData,
  samples: propSamples,
  entryId,
  onDataChange,
  onSampleDataChange,
  notebookType,
}) => {
  const intl = useIntl();

  // State Management
  const [samples, setSamples] = useState(propSamples || []);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [notification, setNotification] = useState(null);

  // Load samples for this page
  useEffect(() => {
    if (pageData?.id && !String(pageData.id).startsWith("default-")) {
      loadPageSamples();
    } else {
      setSamples(propSamples || []);
      setLoading(false);
    }
  }, [pageData?.id, propSamples]);

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (response && Array.isArray(response)) {
          const transformedSamples = response.map((sample) => ({
            id: String(sample.id || sample.sampleItemId),
            externalId: sample.externalId,
            accessionNumber: sample.accessionNumber,
            sampleType: sample.sampleType || sample.typeOfSample?.description,
            collectionDate: sample.collectionDate,
            status: sample.pageStatus || "PENDING",
            patientName: sample.patientName,
            // Additional sample metadata
            sampleOrigin: sample.data?.sampleOrigin,
            projectName: sample.data?.projectName,
            // QC and processing status will be managed by backend
            qcStatus: sample.data?.qcStatus || "PENDING",
            quarantineStatus: sample.data?.quarantineStatus || null,
          }));
          setSamples(transformedSamples);
        } else {
          setSamples([]);
        }
        setLoading(false);
      },
      (error) => {
        console.error("Failed to load page samples:", error);
        setError("Failed to load samples for this page");
        setLoading(false);
      },
    );
  }, [pageData?.id]);

  // Handle page data changes from QC component
  const handlePageDataChange = useCallback(
    (updatedData) => {
      if (onDataChange) {
        onDataChange(updatedData);
      }
    },
    [onDataChange],
  );

  // Handle sample data changes
  const handleSampleDataChange = useCallback(
    (updatedSamples) => {
      setSamples(updatedSamples);
      if (onSampleDataChange) {
        onSampleDataChange(updatedSamples);
      }
    },
    [onSampleDataChange],
  );

  // Handle workflow navigation
  const canProceedToNext = useCallback(() => {
    // This would be determined by backend QC status
    // For now, allow navigation (backend will handle blocking)
    return true;
  }, []);

  if (loading) {
    return (
      <div className="bacteriology-qc-loading">
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <Tile>
              <SkeletonText paragraph lineCount={3} />
            </Tile>
          </Column>
        </Grid>
      </div>
    );
  }

  return (
    <div className="bacteriology-processing-qc-page">
      {/* Page Header */}
      <Grid fullWidth className="page-header">
        <Column lg={16} md={8} sm={4}>
          <div className="page-title-container">
            <h2 className="page-title">
              <FormattedMessage
                id="notebook.bacteriology.processingQC.title"
                defaultMessage="Processing & Quality Control"
              />
            </h2>
            <p className="page-subtitle">
              <FormattedMessage
                id="notebook.bacteriology.processingQC.subtitle"
                defaultMessage="Media preparation, quality control testing, and sample processing"
              />
            </p>
          </div>
        </Column>
      </Grid>

      {/* Error Notification */}
      {error && (
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <InlineNotification
              kind="error"
              title="Error"
              subtitle={error}
              onCloseButtonClick={() => setError(null)}
            />
          </Column>
        </Grid>
      )}

      {/* Success Notification */}
      {notification && (
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <InlineNotification
              kind={notification.kind}
              title={notification.title}
              subtitle={notification.subtitle}
              onCloseButtonClick={() => setNotification(null)}
            />
          </Column>
        </Grid>
      )}

      {/* Backend-Powered QC Component */}
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <BacteriologyQCBackend
            pageData={pageData}
            onPageDataChange={handlePageDataChange}
            notebookEntryId={entryId}
            pageId={pageData?.id}
          />
        </Column>
      </Grid>

      {/* Sample Grid - Shows samples with QC status */}
      <Grid fullWidth className="sample-grid-section">
        <Column lg={16} md={8} sm={4}>
          <Tile className="sample-grid-tile">
            <div className="sample-grid-header">
              <h3>
                <FormattedMessage
                  id="notebook.samples.title"
                  defaultMessage="Samples"
                />
              </h3>
              <span className="sample-count">
                {samples.length}{" "}
                <FormattedMessage
                  id="notebook.samples.count"
                  defaultMessage="samples"
                />
              </span>
            </div>

            <SampleGrid
              samples={samples}
              onSampleSelectionChange={() => {}} // Simplified - backend handles QC logic
              selectedSamples={[]} // Simplified - no need for complex selection logic
              enableSelection={false} // Disable selection for now - focus on QC workflow
              showAdditionalColumns={[
                {
                  key: "qcStatus",
                  header: intl.formatMessage({
                    id: "notebook.sample.qcStatus",
                    defaultMessage: "QC Status",
                  }),
                  render: (sample) => {
                    const status = sample.qcStatus || "PENDING";
                    const colorMap = {
                      PASSED: "green",
                      FAILED: "red",
                      PENDING: "gray",
                      QUARANTINED: "red",
                    };
                    return (
                      <span
                        className={`status-indicator status-${status.toLowerCase()}`}
                        style={{
                          color: colorMap[status] || "#6f6f6f",
                          fontWeight: "600",
                        }}
                      >
                        {status}
                      </span>
                    );
                  },
                },
              ]}
            />
          </Tile>
        </Column>
      </Grid>

      {/* Page Instructions */}
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <Tile className="instruction-tile">
            <h4>
              <FormattedMessage
                id="notebook.bacteriology.processingQC.instructions.title"
                defaultMessage="Instructions"
              />
            </h4>
            <ol>
              <li>
                <FormattedMessage
                  id="notebook.bacteriology.processingQC.instructions.step1"
                  defaultMessage="Select the culture, biochemical, and enrichment media to be used for processing"
                />
              </li>
              <li>
                <FormattedMessage
                  id="notebook.bacteriology.processingQC.instructions.step2"
                  defaultMessage="Perform quality control tests on each selected media type"
                />
              </li>
              <li>
                <FormattedMessage
                  id="notebook.bacteriology.processingQC.instructions.step3"
                  defaultMessage="Review QC results - samples using failed media will be automatically quarantined"
                />
              </li>
              <li>
                <FormattedMessage
                  id="notebook.bacteriology.processingQC.instructions.step4"
                  defaultMessage="Proceed to next page when all QC testing is complete and passed"
                />
              </li>
            </ol>
          </Tile>
        </Column>
      </Grid>

      {/* Navigation */}
      <Grid fullWidth className="navigation-section">
        <Column lg={8} md={4} sm={2}>
          <Button
            kind="secondary"
            renderIcon={ChevronLeft}
            onClick={() => {
              // Navigate to previous page
              if (window.history && window.history.length > 1) {
                window.history.back();
              }
            }}
          >
            <FormattedMessage
              id="notebook.navigation.previous"
              defaultMessage="Previous"
            />
          </Button>
        </Column>
        <Column lg={8} md={4} sm={2} className="next-button-column">
          <Button
            kind="primary"
            renderIcon={ChevronRight}
            iconDescription="Next"
            disabled={!canProceedToNext()}
            onClick={() => {
              // Navigate to next page
              setNotification({
                kind: "info",
                title: "Navigation",
                subtitle: "This would navigate to the next workflow page",
              });
            }}
          >
            <FormattedMessage
              id="notebook.navigation.next"
              defaultMessage="Next"
            />
          </Button>
        </Column>
      </Grid>
    </div>
  );
};

export default BacteriologyProcessingQCPageBackend;
