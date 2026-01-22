import React, { useState, useEffect, useRef, useCallback, useMemo, useContext } from "react";
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
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import NotificationContext, { NotificationKinds } from "../../layout/Layout";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
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
  const { setNotificationVisible, addNotification } = useContext(NotificationContext);
  const componentMounted = useRef(false);

  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);

  const [testingModalOpen, setTestingModalOpen] = useState(false);
  const [isApplying, setIsApplying] = useState(false);

  const [phytochemicalResults, setPhytochemicalResults] = useState({});
  const [testingNotes, setTestingNotes] = useState("");
  const [approvalStatus, setApprovalStatus] = useState(null);

  const phytochemicalOptions = [
    "Alkaloids",
    "Flavonoids",
    "Tannins",
    "Saponins",
    "Terpenoids",
    "Glycosides",
  ];

  const approvalOptions = [
    { id: "approve", label: "Approve for Production" },
    { id: "further_testing", label: "Requires Further Testing" },
    { id: "reject", label: "Reject - Not Suitable" },
  ];

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
          setSamples(
            response && Array.isArray(response)
              ? response.map((s) => ({
                  id: String(s.id || s.sampleItemId),
                  externalId: s.externalId,
                  accessionNumber: s.accessionNumber,
                  status: s.pageStatus || s.status || "PENDING",
                  localName: s.data?.localName,
                  approvalStatus: s.data?.approvalStatus,
                  testingNotes: s.data?.testingNotes,
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
    setPhytochemicalResults({});
    setTestingNotes("");
    setApprovalStatus(null);
  }, []);

  const openModal = useCallback(() => {
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
    resetForm();
    setTestingModalOpen(true);
  }, [selectedSampleIds, intl, resetForm, notify]);

  const applyTesting = useCallback(() => {
    if (!approvalStatus) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.testing.error.approvalRequired",
          defaultMessage: "Please select approval status.",
        }),
      });
      return;
    }

    if (!hasRealPageId) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      });
      return;
    }

    setIsApplying(true);

    const sampleIds = selectedSampleIds.map((id) => parseInt(id, 10));

    postToOpenElisServerJsonResponse(
      `/rest/notebook/tradmed/page/${pageData.id}/testing`,
      JSON.stringify({
        sampleIds,
        phytochemicalResults,
        testingNotes,
        approvalStatus: approvalStatus.id,
        approvalStatusLabel: approvalStatus.label,
      }),
      (response) => {
        setIsApplying(false);
        if (response?.success) {
          // Update sample status using bulk endpoint after testing
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
                  title: response.message ||
                    intl.formatMessage(
                      {
                        id: "notebook.page.tradmed.testing.success",
                        defaultMessage: "Updated testing results for {count} sample(s).",
                      },
                      { count: response.updatedCount || selectedSampleIds.length },
                    ),
                });
                setTestingModalOpen(false);
                setSelectedSampleIds([]);
                loadPageSamples();
                if (onProgressUpdate) onProgressUpdate();
              } else {
                notify({
                  kind: NotificationKinds.error,
                  title: intl.formatMessage({
                    id: "notebook.page.tradmed.error.statusUpdate",
                    defaultMessage: "Testing recorded but failed to update sample status.",
                  }),
                });
              }
            }
          );
        } else {
          notify({
            kind: NotificationKinds.error,
            title: response?.error || "Operation failed",
          });
        }
      },
    );
  }, [
    phytochemicalResults,
    testingNotes,
    approvalStatus,
    hasRealPageId,
    pageData?.id,
    selectedSampleIds,
    intl,
    loadPageSamples,
    onProgressUpdate,
    notify,
  ]);

  const pendingSamples = useMemo(
    () => samples.filter((s) => !s.approvalStatus),
    [samples],
  );
  const testedSamples = useMemo(
    () =>
      samples.filter(
        (s) => s.approvalStatus && s.status === "COMPLETED",
      ),
    [samples],
  );

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
                  defaultMessage="Awaiting Testing"
                />
              </span>
              <span className="progress-value">{pendingSamples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.testing.tested"
                  defaultMessage="Tested"
                />
              </span>
              <span className="progress-value">{testedSamples.length}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Edit}
          onClick={openModal}
          disabled={selectedSampleIds.length === 0 || !hasRealPageId}
        >
          <FormattedMessage
            id="notebook.page.tradmed.testing.recordResults"
            defaultMessage="Record Testing Results ({count})"
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
              defaultMessage="Samples Awaiting Testing"
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
                  defaultMessage="No samples awaiting testing."
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
              ]}
            />
          )}
        </div>
      </div>

      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.tradmed.testing.tested.title"
              defaultMessage="Tested Samples"
            />
            <Tag type="green" size="sm" className="count-tag">
              {testedSamples.length}
            </Tag>
          </h5>
        </div>
        <div className="sample-grid-container">
          {!loading && testedSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.tradmed.testing.tested.empty"
                  defaultMessage="No samples tested yet."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="tested-samples"
              samples={testedSamples}
              showSelection={false}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "localName", header: "Local Name" },
                { key: "approvalStatus", header: "Approval Status" },
              ]}
            />
          )}
        </div>
      </div>

      <Modal
        open={testingModalOpen}
        onRequestClose={() => setTestingModalOpen(false)}
        onRequestSubmit={applyTesting}
        modalHeading={intl.formatMessage({
          id: "notebook.page.tradmed.testing.modal.title",
          defaultMessage: "Record Testing Results & Approval",
        })}
        primaryButtonText={
          isApplying
            ? intl.formatMessage({
                id: "label.recording",
                defaultMessage: "Recording...",
              })
            : intl.formatMessage({
                id: "notebook.page.tradmed.testing.modal.record",
                defaultMessage: "Record Results",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        primaryButtonDisabled={isApplying}
        size="lg"
      >
        {isApplying && <Loading withOverlay={false} small />}

        <Grid fullWidth narrow>
          <Column lg={16} md={8} sm={4}>
            <div
              style={{
                marginBottom: "1rem",
                padding: "0.5rem",
                backgroundColor: "#f4f4f4",
                borderRadius: "4px",
              }}
            >
              <h5
                style={{ margin: "0 0 0.5rem 0" }}
              >
                <FormattedMessage
                  id="notebook.page.tradmed.testing.modal.phytochemical"
                  defaultMessage="Phytochemical Screening Results"
                />
              </h5>
              {phytochemicalOptions.map((option) => (
                <Checkbox
                  key={option}
                  id={option}
                  labelText={option}
                  checked={phytochemicalResults[option] || false}
                  onChange={(e) =>
                    setPhytochemicalResults({
                      ...phytochemicalResults,
                      [option]: e.target.checked,
                    })
                  }
                />
              ))}
            </div>
          </Column>

          <Column lg={16} md={8} sm={4}>
            <Dropdown
              id="approval"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.testing.modal.approval",
                defaultMessage: "Approval Status *",
              })}
              label="Select..."
              items={approvalOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={approvalStatus}
              onChange={({ selectedItem }) => setApprovalStatus(selectedItem)}
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="testing-notes"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.testing.modal.notes",
                defaultMessage: "Testing Notes & Results",
              })}
              value={testingNotes}
              onChange={(e) => setTestingNotes(e.target.value)}
              rows={4}
              placeholder="Document testing results, interpretation, and rationale for approval decision"
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default TraditionalMedicineTestingPage;
