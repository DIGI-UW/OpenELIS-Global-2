import React, { useState, useEffect, useCallback } from 'react';
import {
  Grid,
  Column,
  Tile,
  Button,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  ProgressIndicator,
  ProgressStep,
  Modal,
  Form,
  FormGroup,
  TextInput,
  Select,
  SelectItem,
  TextArea,
  Tag,
  InlineNotification,
  Loading,
  Tabs,
  Tab,
  TabList,
  TabPanels,
  TabPanel,
  OverflowMenu,
  OverflowMenuItem,
  IconButton,
} from '@carbon/react';
import {
  Add,
  View,
  Play,
  Checkmark,
  Warning,
  Error,
  Time,
  Microscope,
  Chemistry,
  Temperature,
  CheckmarkFilled,
} from '@carbon/react/icons';
import { FormattedMessage, useIntl } from 'react-intl';
import { getFromOpenElisServer, postToOpenElisServer } from '../../../utils/Utils';
import { NotificationContext } from '../../../layout/Layout';

/**
 * Stage 2: Virus Culture Growth Workflow Page
 * Implements the 9-step virus culture process from media preparation to packaging
 */
const VirusCultureWorkflowPage = ({ entryId, pageData, progress, onProgressUpdate, notebookEntry }) => {
  const intl = useIntl();
  const [loading, setLoading] = useState(false);
  const [cultureBatches, setCultureBatches] = useState([]);
  const [selectedBatch, setSelectedBatch] = useState(null);
  const [workflowStatus, setWorkflowStatus] = useState([]);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showStepModal, setShowStepModal] = useState(false);
  const [currentStep, setCurrentStep] = useState(null);
  const [dashboardData, setDashboardData] = useState({});
  const [activeTab, setActiveTab] = useState(0);

  // Form states
  const [createBatchForm, setCreateBatchForm] = useState({
    notebookPageSampleId: null,
    virusStrain: '',
    cellLine: '',
    notes: ''
  });

  const [stepForm, setStepForm] = useState({
    qualityResult: 'NOT_APPLICABLE',
    notes: ''
  });

  // Available options (would normally come from API)
  const virusStrains = [
    'SARS-CoV-2',
    'Influenza A (H1N1)',
    'Influenza A (H3N2)',
    'Influenza B',
    'Respiratory Syncytial Virus (RSV)',
    'Adenovirus',
    'Human Metapneumovirus',
    'Parainfluenza Virus',
  ];

  const cellLines = [
    'Vero E6',
    'Vero CCL-81',
    'MDCK',
    'A549',
    'HEp-2',
    'LLC-MK2',
    'HELA',
    'BHK-21',
  ];

  // Workflow steps configuration
  const workflowSteps = [
    {
      name: 'MEDIA_PREPARATION',
      label: 'Media Preparation',
      description: 'Select and prepare culture media, reagents, and equipment',
      icon: Chemistry,
      order: 1
    },
    {
      name: 'STERILIZATION',
      label: 'Sterilization',
      description: 'Autoclave and filter sterilization procedures',
      icon: Temperature,
      order: 2
    },
    {
      name: 'CELL_CULTURE',
      label: 'Cell Culture',
      description: 'Grow host cells to appropriate confluence',
      icon: Microscope,
      order: 3
    },
    {
      name: 'QUALITY_CONTROL',
      label: 'Quality Control',
      description: 'Validate cell viability and sterility',
      icon: CheckmarkFilled,
      order: 4
    },
    {
      name: 'VIRUS_CULTURE',
      label: 'Virus Culture',
      description: 'Inoculate cells with virus samples',
      icon: Chemistry,
      order: 5
    },
    {
      name: 'DARK_ROOM_IMAGING',
      label: 'Dark Room Imaging',
      description: 'Imaging and fluorescence analysis',
      icon: View,
      order: 6
    },
    {
      name: 'FORMULATION',
      label: 'Formulation',
      description: 'Prepare viral product with stabilizers',
      icon: Chemistry,
      order: 7
    },
    {
      name: 'FEEDING',
      label: 'Feeding',
      description: 'Maintain culture with regular feeding',
      icon: Time,
      order: 8
    },
    {
      name: 'PACKAGING',
      label: 'Packaging',
      description: 'Final product packaging and labeling',
      icon: Checkmark,
      order: 9
    }
  ];

  // Load data on component mount
  useEffect(() => {
    loadDashboardData();
    loadCultureBatches();
  }, [entryId, notebookEntry]);

  // Load notebook page samples and auto-create virus culture batches
  useEffect(() => {
    if (pageData?.id && !String(pageData.id).startsWith("default-")) {
      loadNotebookPageSamples();
    }
  }, [pageData?.id]);

  const loadNotebookPageSamples = useCallback(() => {
    setLoading(true);

    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (response && response.samples) {
          console.log(`Loaded ${response.samples.length} samples for virus culture workflow page`);

          // Auto-create virus culture batches for samples that don't have them
          response.samples.forEach(sample => {
            if (sample.status === 'PENDING' || sample.status === 'IN_PROGRESS') {
              autoCreateVirusCultureBatch(sample);
            }
          });
        }
        setLoading(false);
      },
      (error) => {
        console.error('Failed to load notebook page samples:', error);
        setLoading(false);
      }
    );
  }, [pageData?.id]);

  const autoCreateVirusCultureBatch = useCallback((sample) => {
    // Check if a batch already exists for this sample
    getFromOpenElisServer(
      `/rest/notebook/virology/culture/batches/sample/${sample.sampleItemId}`,
      (response) => {
        if (response && response.batches && response.batches.length === 0) {
          // No existing batch, create one automatically
          const batchRequest = {
            notebookPageSampleId: sample.id,
            virusStrain: 'Pending Assignment',
            cellLine: 'Vero E6',
            notes: `Auto-created batch for sample ${sample.accessionNumber || sample.sampleItemId}`
          };

          postToOpenElisServer(
            '/rest/notebook/virology/culture/batch',
            batchRequest,
            (createResponse) => {
              if (createResponse && createResponse.success) {
                console.log(`Auto-created virus culture batch ${createResponse.batchNumber} for sample ${sample.sampleItemId}`);
                // Reload batches to show the new one
                loadCultureBatches();
              } else {
                console.error('Failed to auto-create virus culture batch:', createResponse);
              }
            },
            (error) => {
              console.error('Error auto-creating virus culture batch:', error);
            }
          );
        }
      },
      (error) => {
        console.error('Error checking existing batches for sample:', error);
      }
    );
  }, []);

  const loadDashboardData = useCallback(() => {
    getFromOpenElisServer(
      '/rest/notebook/virology/culture/dashboard',
      (response) => {
        if (response && response.success) {
          setDashboardData(response);
        }
      }
    );
  }, []);

  const loadCultureBatches = useCallback(() => {
    if (!entryId) return;

    setLoading(true);
    getFromOpenElisServer(
      '/rest/notebook/virology/culture/batches/active',
      (response) => {
        setLoading(false);
        if (response && response.success) {
          setCultureBatches(response.batches || []);
        } else {
          console.error('Failed to load virus culture batches');
        }
      }
    );
  }, [entryId]);

  const loadBatchDetails = useCallback((batchId) => {
    setLoading(true);
    getFromOpenElisServer(
      `/rest/notebook/virology/culture/batch/${batchId}`,
      (response) => {
        setLoading(false);
        if (response && response.success) {
          setSelectedBatch(response.batch);
          setWorkflowStatus(response.workflowStatus || []);
        } else {
          console.error('Failed to load batch details');
        }
      }
    );
  }, []);

  const handleCreateBatch = () => {
    setLoading(true);
    postToOpenElisServer(
      '/rest/notebook/virology/culture/batch',
      JSON.stringify(createBatchForm),
      (response) => {
        setLoading(false);
        if (response && response.success) {
          console.log(`Virus culture batch ${response.batchNumber} created successfully`);
          setShowCreateModal(false);
          setCreateBatchForm({
            notebookPageSampleId: null,
            virusStrain: '',
            cellLine: '',
            notes: ''
          });
          loadCultureBatches();
        } else {
          console.error(response?.message || 'Failed to create virus culture batch');
        }
      }
    );
  };

  const handleStartStep = (batchId, stepName) => {
    setLoading(true);
    postToOpenElisServer(
      `/rest/notebook/virology/culture/batch/${batchId}/step/${stepName}/start`,
      '{}',
      (response) => {
        setLoading(false);
        if (response && response.success) {
          console.log(`${getStepLabel(stepName)} started successfully`);
          loadBatchDetails(batchId);
        } else {
          console.error(response?.message || 'Failed to start step');
        }
      }
    );
  };

  const handleCompleteStep = () => {
    if (!currentStep || !selectedBatch) return;

    setLoading(true);
    postToOpenElisServer(
      `/rest/notebook/virology/culture/batch/${selectedBatch.id}/step/${currentStep.stepName}/complete`,
      JSON.stringify(stepForm),
      (response) => {
        setLoading(false);
        if (response && response.success) {
          console.log(`${getStepLabel(currentStep.stepName)} completed successfully`);
          setShowStepModal(false);
          setStepForm({ qualityResult: 'NOT_APPLICABLE', notes: '' });
          loadBatchDetails(selectedBatch.id);

          // If this was the final step (PACKAGING), mark the notebook page sample as completed
          if (currentStep.stepName === 'PACKAGING') {
            updateNotebookPageSampleStatus(selectedBatch.notebookPageSampleId, 'COMPLETED');
          }
        } else {
          console.error(response?.message || 'Failed to complete step');
        }
      }
    );
  };

  const updateNotebookPageSampleStatus = useCallback((notebookPageSampleId, status) => {
    if (!notebookPageSampleId || !pageData?.id) return;

    console.log(`Updating notebook page sample ${notebookPageSampleId} status to ${status}`);

    // Update the notebook page sample status to sync with virus culture workflow progress
    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: [notebookPageSampleId],
        status: status
      }),
      (response) => {
        if (response && response.success) {
          console.log(`Successfully updated notebook page sample status to ${status}`);
          // Notify parent component about progress update
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          console.error('Failed to update notebook page sample status:', response);
        }
      },
      (error) => {
        console.error('Error updating notebook page sample status:', error);
      }
    );
  }, [pageData?.id, onProgressUpdate]);

  const getStepLabel = (stepName) => {
    const step = workflowSteps.find(s => s.name === stepName);
    return step ? step.label : stepName;
  };

  const formatDate = (dateString) => {
    if (!dateString) return '—';
    try {
      return new Date(dateString).toLocaleDateString();
    } catch (error) {
      return '—';
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'COMPLETED':
        return <Checkmark size={16} className="status-completed" />;
      case 'IN_PROGRESS':
        return <Time size={16} className="status-in-progress" />;
      case 'FAILED':
        return <Error size={16} className="status-failed" />;
      case 'ON_HOLD':
        return <Warning size={16} className="status-warning" />;
      default:
        return <Time size={16} className="status-pending" />;
    }
  };

  const getStatusTag = (status) => {
    const statusMap = {
      'PENDING': { type: 'gray', text: 'Pending' },
      'IN_PROGRESS': { type: 'blue', text: 'In Progress' },
      'COMPLETED': { type: 'green', text: 'Completed' },
      'FAILED': { type: 'red', text: 'Failed' },
      'ON_HOLD': { type: 'yellow', text: 'On Hold' },
      'SKIPPED': { type: 'gray', text: 'Skipped' }
    };

    const config = statusMap[status] || statusMap['PENDING'];
    return <Tag type={config.type}>{config.text}</Tag>;
  };

  const batchTableHeaders = [
    { key: 'batchId', header: 'Batch ID' },
    { key: 'virusStrain', header: 'Virus Strain' },
    { key: 'cellLine', header: 'Cell Line' },
    { key: 'status', header: 'Status' },
    { key: 'createdDate', header: 'Created' },
    { key: 'progress', header: 'Progress' },
    { key: 'actions', header: 'Actions' }
  ];

  const renderBatchRow = (batch) => ({
    id: batch.id,
    batchId: batch.batchId,
    virusStrain: batch.virusStrain || '—',
    cellLine: batch.cellLineUsed || '—',
    status: getStatusTag(batch.status),
    createdDate: formatDate(batch.createdDate),
    progress: `${batch.completedSteps || 0}/9 steps`,
    actions: (
      <OverflowMenu>
        <OverflowMenuItem itemText="View Details" onClick={() => loadBatchDetails(batch.id)} />
        <OverflowMenuItem itemText="Continue Workflow" onClick={() => loadBatchDetails(batch.id)} />
      </OverflowMenu>
    )
  });

  const renderWorkflowProgress = () => {
    if (!workflowStatus.length) return null;

    const currentStepIndex = workflowStatus.findIndex(step => step.status === 'IN_PROGRESS');
    const completedSteps = workflowStatus.filter(step => step.status === 'COMPLETED').length;

    return (
      <div className="workflow-progress">
        <ProgressIndicator currentIndex={currentStepIndex >= 0 ? currentStepIndex : completedSteps}>
          {workflowSteps.map((step, index) => {
            const stepStatus = workflowStatus.find(ws => ws.stepName === step.name);
            const status = stepStatus ? stepStatus.status : 'PENDING';

            return (
              <ProgressStep
                key={step.name}
                label={step.label}
                description={step.description}
                complete={status === 'COMPLETED'}
                current={status === 'IN_PROGRESS'}
                invalid={status === 'FAILED'}
                disabled={status === 'PENDING'}
              />
            );
          })}
        </ProgressIndicator>
      </div>
    );
  };

  const renderWorkflowSteps = () => {
    if (!selectedBatch || !workflowStatus.length) return null;

    return (
      <div className="workflow-steps">
        <h4>Workflow Steps</h4>
        <DataTable
          rows={workflowStatus.map(step => ({
            id: step.id,
            stepName: getStepLabel(step.stepName),
            status: getStatusTag(step.status),
            assignedTo: step.assignedTo ? step.assignedTo.login : '—',
            startedDate: step.startedDate ? formatDate(step.startedDate) : '—',
            completedDate: step.completedDate ? formatDate(step.completedDate) : '—',
            actions: step.status === 'PENDING' ? (
              <Button
                kind="primary"
                size="sm"
                renderIcon={Play}
                onClick={() => handleStartStep(selectedBatch.id, step.stepName)}
              >
                Start
              </Button>
            ) : step.status === 'IN_PROGRESS' ? (
              <Button
                kind="secondary"
                size="sm"
                renderIcon={Checkmark}
                onClick={() => {
                  setCurrentStep(step);
                  setShowStepModal(true);
                }}
              >
                Complete
              </Button>
            ) : null
          }))}
          headers={[
            { key: 'stepName', header: 'Step' },
            { key: 'status', header: 'Status' },
            { key: 'assignedTo', header: 'Assigned To' },
            { key: 'startedDate', header: 'Started' },
            { key: 'completedDate', header: 'Completed' },
            { key: 'actions', header: 'Actions' }
          ]}
        >
          {({ rows, headers, getTableProps, getHeaderProps, getRowProps }) => (
            <TableContainer title="Workflow Steps">
              <Table {...getTableProps()}>
                <TableHead>
                  <TableRow>
                    {headers.map((header) => (
                      <TableHeader key={header.key} {...getHeaderProps({ header })}>
                        {header.header}
                      </TableHeader>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {rows.map((row) => (
                    <TableRow key={row.id} {...getRowProps({ row })}>
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
      </div>
    );
  };

  const renderDashboard = () => (
    <Grid>
      <Column lg={3} md={2} sm={2}>
        <Tile className="dashboard-tile">
          <h4>Active Batches</h4>
          <div className="metric-value">{dashboardData.activeBatches || 0}</div>
        </Tile>
      </Column>
      <Column lg={3} md={2} sm={2}>
        <Tile className="dashboard-tile">
          <h4>Completed Today</h4>
          <div className="metric-value">{dashboardData.completedToday || 0}</div>
        </Tile>
      </Column>
      <Column lg={3} md={2} sm={2}>
        <Tile className="dashboard-tile">
          <h4>Requiring Attention</h4>
          <div className="metric-value">{dashboardData.requiresAttention || 0}</div>
        </Tile>
      </Column>
      <Column lg={3} md={2} sm={2}>
        <Tile className="dashboard-tile">
          <h4>Success Rate</h4>
          <div className="metric-value">{dashboardData.successRate || 0}%</div>
        </Tile>
      </Column>
    </Grid>
  );

  const renderBatchDetails = () => {
    if (!selectedBatch) return null;

    return (
      <div className="batch-details">
        <h3>Batch Details: {selectedBatch.batchId}</h3>

        <Grid>
          <Column lg={8} md={4} sm={4}>
            <div className="batch-info">
              <h4>Batch Information</h4>
              <p><strong>Virus Strain:</strong> {selectedBatch.virusStrain}</p>
              <p><strong>Cell Line:</strong> {selectedBatch.cellLineUsed}</p>
              <p><strong>Passage Number:</strong> {selectedBatch.passageNumber}</p>
              <p><strong>Status:</strong> {getStatusTag(selectedBatch.status)}</p>
              <p><strong>Created:</strong> {formatDate(selectedBatch.createdDate)}</p>
              {selectedBatch.cultureStartDate && (
                <p><strong>Culture Started:</strong> {formatDate(selectedBatch.cultureStartDate)}</p>
              )}
              {selectedBatch.temperatureCelsius && (
                <p><strong>Temperature:</strong> {selectedBatch.temperatureCelsius}°C</p>
              )}
              {selectedBatch.co2Percentage && (
                <p><strong>CO₂:</strong> {selectedBatch.co2Percentage}%</p>
              )}
            </div>
          </Column>
          <Column lg={8} md={4} sm={4}>
            {renderWorkflowProgress()}
          </Column>
        </Grid>

        {renderWorkflowSteps()}
      </div>
    );
  };

  return (
    <div className="virus-culture-workflow-page">
      <div className="page-header">
        <h2>
          <FormattedMessage
            id="virology.culture.workflow.title"
            defaultMessage="Virus Culture Growth Workflow"
          />
        </h2>
        <Button
          kind="primary"
          renderIcon={Add}
          onClick={() => setShowCreateModal(true)}
        >
          <FormattedMessage
            id="virology.culture.workflow.createBatch"
            defaultMessage="Create Culture Batch"
          />
        </Button>
      </div>

      {loading && <Loading />}

      <Tabs selectedIndex={activeTab} onChange={({ selectedIndex }) => setActiveTab(selectedIndex)}>
        <TabList aria-label="Virus Culture Workflow Tabs">
          <Tab>Dashboard</Tab>
          <Tab>Active Batches</Tab>
          <Tab>Batch Details</Tab>
        </TabList>
        <TabPanels>
          <TabPanel>{renderDashboard()}</TabPanel>
          <TabPanel>
            <DataTable
              rows={cultureBatches.map(renderBatchRow)}
              headers={batchTableHeaders}
            >
              {({ rows, headers, getTableProps, getHeaderProps, getRowProps }) => (
                <TableContainer title="Active Culture Batches">
                  <Table {...getTableProps()}>
                    <TableHead>
                      <TableRow>
                        {headers.map((header) => (
                          <TableHeader key={header.key} {...getHeaderProps({ header })}>
                            {header.header}
                          </TableHeader>
                        ))}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {rows.map((row) => (
                        <TableRow key={row.id} {...getRowProps({ row })}>
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
          </TabPanel>
          <TabPanel>
            {selectedBatch ? (
              renderBatchDetails()
            ) : (
              <Tile>
                <p>Select a batch from the Active Batches tab to view details.</p>
              </Tile>
            )}
          </TabPanel>
        </TabPanels>
      </Tabs>

      {/* Create Batch Modal */}
      <Modal
        open={showCreateModal}
        onRequestClose={() => setShowCreateModal(false)}
        modalHeading="Create New Virus Culture Batch"
        primaryButtonText="Create Batch"
        secondaryButtonText="Cancel"
        onRequestSubmit={handleCreateBatch}
      >
        <Form>
          <FormGroup legendText="Virus Strain">
            <Select
              id="virus-strain-select"
              value={createBatchForm.virusStrain}
              onChange={(e) => setCreateBatchForm({
                ...createBatchForm,
                virusStrain: e.target.value
              })}
            >
              <SelectItem value="" text="Select virus strain..." />
              {virusStrains.map(strain => (
                <SelectItem key={strain} value={strain} text={strain} />
              ))}
            </Select>
          </FormGroup>

          <FormGroup legendText="Cell Line">
            <Select
              id="cell-line-select"
              value={createBatchForm.cellLine}
              onChange={(e) => setCreateBatchForm({
                ...createBatchForm,
                cellLine: e.target.value
              })}
            >
              <SelectItem value="" text="Select cell line..." />
              {cellLines.map(cellLine => (
                <SelectItem key={cellLine} value={cellLine} text={cellLine} />
              ))}
            </Select>
          </FormGroup>

          <FormGroup legendText="Notes">
            <TextArea
              id="batch-notes"
              labelText="Batch Notes"
              rows={3}
              placeholder="Enter any notes about this batch..."
              value={createBatchForm.notes}
              onChange={(e) => setCreateBatchForm({
                ...createBatchForm,
                notes: e.target.value
              })}
            />
          </FormGroup>
        </Form>
      </Modal>

      {/* Complete Step Modal */}
      <Modal
        open={showStepModal}
        onRequestClose={() => setShowStepModal(false)}
        modalHeading={`Complete Step: ${currentStep ? getStepLabel(currentStep.stepName) : ''}`}
        primaryButtonText="Complete Step"
        secondaryButtonText="Cancel"
        onRequestSubmit={handleCompleteStep}
      >
        <Form>
          <FormGroup legendText="Quality Check Result">
            <Select
              id="quality-result-select"
              value={stepForm.qualityResult}
              onChange={(e) => setStepForm({
                ...stepForm,
                qualityResult: e.target.value
              })}
            >
              <SelectItem value="NOT_APPLICABLE" text="Not Applicable" />
              <SelectItem value="PASSED" text="Passed" />
              <SelectItem value="FAILED" text="Failed" />
              <SelectItem value="CONDITIONAL_PASS" text="Conditional Pass" />
            </Select>
          </FormGroup>

          <FormGroup legendText="Completion Notes">
            <TextArea
              id="step-notes"
              labelText="Step Completion Notes"
              rows={3}
              placeholder="Enter any notes about completing this step..."
              value={stepForm.notes}
              onChange={(e) => setStepForm({
                ...stepForm,
                notes: e.target.value
              })}
            />
          </FormGroup>
        </Form>
      </Modal>

    </div>
  );
};

export default VirusCultureWorkflowPage;