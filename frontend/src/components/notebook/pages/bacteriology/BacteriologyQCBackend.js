import React, { useState, useEffect, useCallback, useMemo } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  TextInput,
  Dropdown,
  MultiSelect,
  RadioButtonGroup,
  RadioButton,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeaderCell,
  TableBody,
  TableCell,
  Tag,
  Modal,
  TextArea,
} from "@carbon/react";
import {
  CheckmarkFilled,
  WarningAlt,
  Chemistry,
  Renew,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  putToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";

/**
 * BacteriologyQCBackend - Backend-powered QC management for bacteriology workflow
 * Replaces complex frontend logic with API calls to MediaQCRestController
 */

// Media Types Configuration
const MEDIA_TYPES = {
  CULTURE: [
    { id: "BLOOD_AGAR", text: "Blood Agar" },
    { id: "MACCONKEY", text: "MacConkey Agar" },
    { id: "CHOCOLATE_AGAR", text: "Chocolate Agar" },
    { id: "MUELLER_HINTON", text: "Mueller-Hinton Agar" },
    { id: "EMB", text: "EMB Agar" },
    { id: "OTHER", text: "Other (specify)" },
  ],
  BIOCHEMICAL: [
    { id: "TSI", text: "Triple Sugar Iron (TSI)" },
    { id: "UREASE", text: "Urease Test" },
    { id: "CITRATE", text: "Citrate Test" },
    { id: "INDOLE", text: "Indole Test" },
    { id: "OXIDASE", text: "Oxidase Test" },
    { id: "OTHER", text: "Other (specify)" },
  ],
  ENRICHMENT: [
    { id: "SELENITE_F", text: "Selenite F Broth" },
    { id: "TETRATHIONATE", text: "Tetrathionate Broth" },
    { id: "BHI", text: "Brain Heart Infusion" },
    { id: "THIOGLYCOLLATE", text: "Thioglycollate Broth" },
    { id: "OTHER", text: "Other (specify)" },
  ],
};

const BacteriologyQCBackend = ({
  pageData,
  onPageDataChange,
  notebookEntryId,
  pageId,
}) => {
  const intl = useIntl();

  // State Management
  const [selectedMedia, setSelectedMedia] = useState({});
  const [qcResults, setQCResults] = useState([]);
  const [quarantinedSamples, setQuarantinedSamples] = useState([]);
  const [qcSummary, setQCSummary] = useState({});
  const [loading, setLoading] = useState(false);
  const [notification, setNotification] = useState(null);
  const [blockingNotification, setBlockingNotification] = useState(null);
  const [qcTestTemplates, setQCTestTemplates] = useState({});

  // Load QC data on component mount and when pageId changes
  useEffect(() => {
    if (pageId) {
      loadQCData();
      loadQuarantinedSamples();
    }
  }, [pageId]);

  // Load initial media selections from pageData
  useEffect(() => {
    if (pageData?.mediaSelections) {
      setSelectedMedia(pageData.mediaSelections);
    }
  }, [pageData]);

  // API Calls

  /**
   * Load QC results from backend
   */
  const loadQCData = useCallback(async () => {
    if (!pageId) return;

    try {
      setLoading(true);
      getFromOpenElisServer(
        `/rest/notebook/qc/media/${pageId}`,
        (response) => {
          if (response.success) {
            setQCResults(response.qcResults || []);
            setQCSummary(response.summary || {});

            // Check for blocking QC results
            if (response.blockingResults?.length > 0) {
              setBlockingNotification({
                kind: "error",
                title: intl.formatMessage({
                  id: "notebook.qc.blocking.title",
                  defaultMessage: "QC Failures Block Processing",
                }),
                subtitle: intl.formatMessage({
                  id: "notebook.qc.blocking.message",
                  defaultMessage: `${response.blockingResults.length} media have failed QC. Samples using these media are quarantined.`,
                }),
              });
            } else {
              setBlockingNotification(null);
            }
          }
        },
        (error) => {
          console.error("Failed to load QC data:", error);
          setNotification({
            kind: "error",
            title: "Error loading QC data",
            subtitle: error.message || "Failed to load QC data",
          });
        },
      );
    } finally {
      setLoading(false);
    }
  }, [pageId, intl]);

  /**
   * Load quarantined samples from backend
   */
  const loadQuarantinedSamples = useCallback(async () => {
    if (!pageId) return;

    getFromOpenElisServer(
      `/rest/notebook/qc/quarantine/${pageId}`,
      (response) => {
        if (response.success) {
          setQuarantinedSamples(response.quarantinedSamples || []);
        }
      },
      (error) => {
        console.error("Failed to load quarantined samples:", error);
      },
    );
  }, [pageId]);

  /**
   * Load QC test template for media type
   */
  const loadQCTestTemplate = useCallback(
    async (mediaType) => {
      if (qcTestTemplates[mediaType]) return qcTestTemplates[mediaType];

      getFromOpenElisServer(
        `/rest/notebook/qc/template/${mediaType}`,
        (response) => {
          if (response.success) {
            setQCTestTemplates((prev) => ({
              ...prev,
              [mediaType]: response.template,
            }));
          }
        },
        (error) => {
          console.error(`Failed to load QC template for ${mediaType}:`, error);
        },
      );
    },
    [qcTestTemplates],
  );

  /**
   * Perform QC for a media
   */
  const performMediaQC = useCallback(
    async (mediaId, mediaName, mediaType, qcTests) => {
      if (!pageId) return;

      const qcData = {
        mediaId,
        mediaName,
        mediaType,
        qcTests,
      };

      setLoading(true);

      postToOpenElisServerJsonResponse(
        `/rest/notebook/qc/media/${pageId}`,
        JSON.stringify(qcData),
        (response) => {
          if (response.success) {
            // Refresh QC data
            loadQCData();
            loadQuarantinedSamples();

            setNotification({
              kind:
                response.qcResult.overallStatus === "PASSED"
                  ? "success"
                  : "warning",
              title: `QC ${response.qcResult.overallStatus} for ${mediaName}`,
              subtitle: response.workflow?.action || "QC completed",
            });
          } else {
            setNotification({
              kind: "error",
              title: "QC Submission Failed",
              subtitle: response.error || "Failed to submit QC results",
            });
          }
        },
        null,
      );

      setLoading(false);
    },
    [pageId, loadQCData, loadQuarantinedSamples],
  );

  /**
   * Handle media selection changes
   */
  const handleMediaSelectionChange = useCallback(
    (mediaType, selections) => {
      const updatedSelections = {
        ...selectedMedia,
        [mediaType]: selections,
      };

      setSelectedMedia(updatedSelections);

      // Save to page data
      if (onPageDataChange) {
        onPageDataChange({
          ...pageData,
          mediaSelections: updatedSelections,
        });
      }

      // Load QC templates for selected media types
      selections.forEach((mediaId) => {
        loadQCTestTemplate(mediaType);
      });
    },
    [selectedMedia, pageData, onPageDataChange, loadQCTestTemplate],
  );

  // UI Components

  /**
   * Media Selection Component
   */
  const MediaSelectionSection = ({ mediaType, title }) => {
    const mediaOptions = MEDIA_TYPES[mediaType] || [];
    const selections = selectedMedia[mediaType] || [];

    return (
      <Tile className="media-selection-tile">
        <h4>{title}</h4>
        <MultiSelect
          id={`${mediaType}-media-select`}
          titleText={intl.formatMessage({
            id: `notebook.media.${mediaType.toLowerCase()}.label`,
            defaultMessage: `Select ${title}`,
          })}
          items={mediaOptions}
          itemToString={(item) => (item ? item.text : "")}
          selectedItems={selections
            .map((id) => mediaOptions.find((m) => m.id === id))
            .filter(Boolean)}
          onChange={(changes) => {
            const selectedIds = changes.selectedItems.map((item) => item.id);
            handleMediaSelectionChange(mediaType, selectedIds);
          }}
        />
      </Tile>
    );
  };

  /**
   * QC Section for each selected media
   */
  const MediaQCSection = ({ mediaId, mediaName, mediaType }) => {
    const [qcTests, setQCTests] = useState({});
    const [testTemplate, setTestTemplate] = useState({});

    // Load existing QC results for this media
    useEffect(() => {
      const existingQC = qcResults.find((qc) => qc.mediaId === mediaId);
      if (existingQC) {
        setQCTests(existingQC.qcTests || {});
      }
    }, [mediaId, qcResults]);

    // Load test template
    useEffect(() => {
      if (qcTestTemplates[mediaType]) {
        setTestTemplate(qcTestTemplates[mediaType]);
      }
    }, [mediaType, qcTestTemplates]);

    const handleQCTestChange = (testName, value) => {
      setQCTests((prev) => ({
        ...prev,
        [testName]: value,
      }));
    };

    const handleQCSubmit = () => {
      performMediaQC(mediaId, mediaName, mediaType, qcTests);
    };

    const existingQCResult = qcResults.find((qc) => qc.mediaId === mediaId);
    const qcStatus = existingQCResult?.overallStatus;

    return (
      <Tile className="media-qc-tile">
        <div className="media-qc-header">
          <h5>{mediaName} Quality Control</h5>
          {qcStatus && (
            <Tag
              type={
                qcStatus === "PASSED"
                  ? "green"
                  : qcStatus === "FAILED"
                    ? "red"
                    : "gray"
              }
            >
              {qcStatus}
            </Tag>
          )}
        </div>

        {Object.entries(testTemplate).map(([testName, testConfig]) => (
          <div key={testName} className="qc-test-item">
            <RadioButtonGroup
              legendText={testConfig.name}
              name={`${mediaId}-${testName}`}
              valueSelected={qcTests[testName] || ""}
              onChange={(value) => handleQCTestChange(testName, value)}
            >
              {testConfig.options?.map((option) => (
                <RadioButton
                  key={option}
                  labelText={option}
                  value={option}
                  id={`${mediaId}-${testName}-${option}`}
                />
              ))}
            </RadioButtonGroup>
          </div>
        ))}

        <Button
          kind="primary"
          size="sm"
          onClick={handleQCSubmit}
          disabled={loading}
          renderIcon={Chemistry}
        >
          <FormattedMessage
            id="notebook.qc.submit"
            defaultMessage="Submit QC Results"
          />
        </Button>
      </Tile>
    );
  };

  /**
   * QC Summary Statistics
   */
  const QCSummaryTile = () => {
    const totalQC = Object.values(qcSummary).reduce(
      (sum, count) => sum + count,
      0,
    );
    const passedCount = qcSummary.PASSED || 0;
    const failedCount = qcSummary.FAILED || 0;
    const pendingCount = qcSummary.PENDING || 0;

    return (
      <Tile className="qc-summary-tile">
        <h4>
          <FormattedMessage
            id="notebook.qc.summary.title"
            defaultMessage="QC Summary"
          />
        </h4>
        <div className="qc-summary-stats">
          <div className="qc-stat">
            <span className="qc-stat-label">Total:</span>
            <span className="qc-stat-value">{totalQC}</span>
          </div>
          <div className="qc-stat qc-stat-passed">
            <span className="qc-stat-label">Passed:</span>
            <span className="qc-stat-value">{passedCount}</span>
          </div>
          <div className="qc-stat qc-stat-failed">
            <span className="qc-stat-label">Failed:</span>
            <span className="qc-stat-value">{failedCount}</span>
          </div>
          <div className="qc-stat qc-stat-pending">
            <span className="qc-stat-label">Pending:</span>
            <span className="qc-stat-value">{pendingCount}</span>
          </div>
        </div>
      </Tile>
    );
  };

  /**
   * Quarantined Samples Table
   */
  const QuarantinedSamplesTable = () => {
    const headers = [
      { key: "labNumber", header: "Lab Number" },
      { key: "sampleId", header: "Sample ID" },
      { key: "failedMediaName", header: "Failed Media" },
      { key: "quarantineStatus", header: "Status" },
      { key: "quarantineDate", header: "Quarantine Date" },
    ];

    const rows = quarantinedSamples.map((sample, index) => ({
      id: index.toString(),
      labNumber: sample.labNumber,
      sampleId: sample.sampleId,
      failedMediaName: sample.failedMediaName,
      quarantineStatus: sample.quarantineStatus,
      quarantineDate: new Date(sample.quarantineDate).toLocaleDateString(),
    }));

    if (quarantinedSamples.length === 0) {
      return null;
    }

    return (
      <Tile className="quarantined-samples-tile">
        <h4>
          <FormattedMessage
            id="notebook.qc.quarantined.title"
            defaultMessage="Quarantined Samples"
          />
        </h4>
        <DataTable rows={rows} headers={headers}>
          {({ rows, headers, getTableProps, getHeaderProps, getRowProps }) => (
            <TableContainer>
              <Table {...getTableProps()}>
                <TableHead>
                  <TableRow>
                    {headers.map((header) => (
                      <TableHeaderCell {...getHeaderProps({ header })}>
                        {header.header}
                      </TableHeaderCell>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {rows.map((row) => (
                    <TableRow {...getRowProps({ row })}>
                      {row.cells.map((cell) => (
                        <TableCell key={cell.id}>{cell.value}</TableCell>
                      ))}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </DataTable>
      </Tile>
    );
  };

  // Generate QC sections for selected media
  const qcSections = useMemo(() => {
    const sections = [];

    Object.entries(selectedMedia).forEach(([mediaType, mediaIds]) => {
      mediaIds.forEach((mediaId) => {
        const mediaInfo = MEDIA_TYPES[mediaType].find((m) => m.id === mediaId);
        if (mediaInfo) {
          sections.push(
            <MediaQCSection
              key={`${mediaType}-${mediaId}`}
              mediaId={`${mediaType}-${mediaId}`}
              mediaName={mediaInfo.text}
              mediaType={mediaType}
            />,
          );
        }
      });
    });

    return sections;
  }, [selectedMedia, qcResults, qcTestTemplates]);

  return (
    <div className="bacteriology-qc-backend">
      {/* Notifications */}
      {notification && (
        <InlineNotification
          kind={notification.kind}
          title={notification.title}
          subtitle={notification.subtitle}
          onCloseButtonClick={() => setNotification(null)}
          className="qc-notification"
        />
      )}

      {blockingNotification && (
        <InlineNotification
          kind={blockingNotification.kind}
          title={blockingNotification.title}
          subtitle={blockingNotification.subtitle}
          hideCloseButton
          className="qc-blocking-notification"
        />
      )}

      <Grid fullWidth>
        {/* Media Selection */}
        <Column lg={16} md={8} sm={4}>
          <h3>
            <FormattedMessage
              id="notebook.qc.media.selection.title"
              defaultMessage="Media Selection"
            />
          </h3>
        </Column>

        <Column lg={5} md={8} sm={4}>
          <MediaSelectionSection
            mediaType="CULTURE"
            title={intl.formatMessage({
              id: "notebook.media.culture.title",
              defaultMessage: "Culture Media",
            })}
          />
        </Column>

        <Column lg={5} md={8} sm={4}>
          <MediaSelectionSection
            mediaType="BIOCHEMICAL"
            title={intl.formatMessage({
              id: "notebook.media.biochemical.title",
              defaultMessage: "Biochemical Media",
            })}
          />
        </Column>

        <Column lg={6} md={8} sm={4}>
          <MediaSelectionSection
            mediaType="ENRICHMENT"
            title={intl.formatMessage({
              id: "notebook.media.enrichment.title",
              defaultMessage: "Enrichment Media",
            })}
          />
        </Column>

        {/* QC Summary */}
        <Column lg={16} md={8} sm={4}>
          <QCSummaryTile />
        </Column>

        {/* QC Sections */}
        {qcSections.length > 0 && (
          <Column lg={16} md={8} sm={4}>
            <h3>
              <FormattedMessage
                id="notebook.qc.testing.title"
                defaultMessage="Quality Control Testing"
              />
            </h3>
            <div className="qc-sections-container">{qcSections}</div>
          </Column>
        )}

        {/* Quarantined Samples */}
        <Column lg={16} md={8} sm={4}>
          <QuarantinedSamplesTable />
        </Column>
      </Grid>
    </div>
  );
};

export default BacteriologyQCBackend;
