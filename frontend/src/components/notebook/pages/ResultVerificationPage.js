import React, {
  useState,
  useEffect,
  useRef,
  useCallback,
  useContext,
} from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Loading,
  Modal,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Tag,
  TextArea,
} from "@carbon/react";
import { Checkmark, Close } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer, postToOpenElisServer } from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";
import { ESignatureModal, SignatureMeaning, useESign } from "../../esignature";
import PermissionGate from "../../security/PermissionGate";
import { Permissions } from "../../../constants/roles";
import "../workflow/NotebookWorkflow.css";

/**
 * ResultVerificationPage - Page 5 of the MedLab workflow.
 * Handles verification (approval/rejection) of entered test results.
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function ResultVerificationPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  // State for samples
  const [samples, setSamples] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Rejection modal state
  const [rejectModalOpen, setRejectModalOpen] = useState(false);
  const [selectedSample, setSelectedSample] = useState(null);
  const [selectedTest, setSelectedTest] = useState(null);
  const [rejectionComments, setRejectionComments] = useState("");

  // E-signature pending approval context (Pattern C: per-row inline button)
  const [pendingApproval, setPendingApproval] = useState(null);

  // Load results for verification
  const loadResultsForVerification = useCallback(() => {
    if (!entryId) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    const url = `/rest/medlab/entry/${entryId}/results-for-verification`;

    getFromOpenElisServer(url, (response) => {
      if (componentMounted.current) {
        if (response && Array.isArray(response)) {
          setSamples(response);
        } else {
          setSamples([]);
        }
        setLoading(false);
      }
    });
  }, [entryId]);

  // Load data on mount
  useEffect(() => {
    componentMounted.current = true;
    loadResultsForVerification();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id, loadResultsForVerification]);

  // Approve a result
  const handleApproveResult = useCallback(
    (sample, test) => {
      const verifyData = {
        labNo: sample.labNo,
        testId: test.testId,
        approved: true,
        notebookPageId: pageData?.id,
      };

      postToOpenElisServer(
        "/rest/medlab/verify-result",
        JSON.stringify(verifyData),
        (status) => {
          if (status === 200) {
            addNotification({
              title: intl.formatMessage({ id: "notification.title" }),
              message: intl.formatMessage({
                id: "medlab.verification.approve.success",
                defaultMessage: "Result verified successfully",
              }),
              kind: NotificationKinds.success,
            });
            setNotificationVisible(true);
            loadResultsForVerification();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            addNotification({
              title: intl.formatMessage({ id: "notification.title" }),
              message: intl.formatMessage({
                id: "medlab.verification.approve.error",
                defaultMessage: "Error verifying result",
              }),
              kind: NotificationKinds.error,
            });
            setNotificationVisible(true);
          }
        },
      );
    },
    [
      pageData,
      intl,
      addNotification,
      setNotificationVisible,
      loadResultsForVerification,
      onProgressUpdate,
    ],
  );

  // Open rejection modal
  const handleOpenRejectModal = useCallback((sample, test) => {
    setSelectedSample(sample);
    setSelectedTest(test);
    setRejectionComments("");
    setRejectModalOpen(true);
  }, []);

  // Submit rejection
  const handleSubmitRejection = useCallback(() => {
    if (!selectedSample || !selectedTest) return;

    const verifyData = {
      labNo: selectedSample.labNo,
      testId: selectedTest.testId,
      approved: false,
      comments: rejectionComments,
      notebookPageId: pageData?.id,
    };

    postToOpenElisServer(
      "/rest/medlab/verify-result",
      JSON.stringify(verifyData),
      (status) => {
        if (status === 200) {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "medlab.verification.reject.success",
              defaultMessage: "Result rejected - needs re-entry",
            }),
            kind: NotificationKinds.success,
          });
          setNotificationVisible(true);
          setRejectModalOpen(false);
          loadResultsForVerification();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          addNotification({
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "medlab.verification.reject.error",
              defaultMessage: "Error rejecting result",
            }),
            kind: NotificationKinds.error,
          });
          setNotificationVisible(true);
        }
      },
    );
  }, [
    selectedSample,
    selectedTest,
    rejectionComments,
    pageData,
    intl,
    addNotification,
    setNotificationVisible,
    loadResultsForVerification,
    onProgressUpdate,
  ]);

  // ==========================================
  // E-Signature Integration (21 CFR Part 11)
  // ==========================================

  // VALIDATED_AND_RELEASED callback: approve with the pending sample/test
  const handleSignAndApprove = useCallback(
    // eslint-disable-next-line no-unused-vars
    (signature) => {
      if (pendingApproval) {
        handleApproveResult(pendingApproval.sample, pendingApproval.test);
        setPendingApproval(null);
      }
    },
    [pendingApproval, handleApproveResult],
  );

  // Clear pending approval on cancel so a future open starts clean
  const handleApproveSignCancelled = useCallback(() => {
    setPendingApproval(null);
  }, []);

  // REJECTED callback: submit the rejection after signing
  const handleSignAndReject = useCallback(
    // eslint-disable-next-line no-unused-vars
    (signature) => {
      handleSubmitRejection();
    },
    [handleSubmitRejection],
  );

  // Reopen the rejection modal if the user cancels the e-sig flow
  const handleRejectSignCancelled = useCallback(() => {
    setRejectModalOpen(true);
  }, []);

  // Hook 1: VALIDATED_AND_RELEASED (per-row approve)
  const {
    openSignatureModal: openApproveSignatureModal,
    signatureModalProps: approveSignatureModalProps,
  } = useESign({
    meaning: SignatureMeaning.VALIDATED_AND_RELEASED,
    context: intl.formatMessage({
      id: "medlab.verification.esig.approveContext",
      defaultMessage: "Validate and release result as approved",
    }),
    recordType: "NOTEBOOK_PAGE_SAMPLE",
    recordId: pageData?.id || 0,
    onSuccess: handleSignAndApprove,
    onCancel: handleApproveSignCancelled,
  });

  // Hook 2: REJECTED (rejection modal)
  const {
    openSignatureModal: openRejectSignatureModal,
    signatureModalProps: rejectSignatureModalProps,
  } = useESign({
    meaning: SignatureMeaning.REJECTED,
    context: intl.formatMessage({
      id: "medlab.verification.esig.rejectContext",
      defaultMessage: "Sign rejection of result",
    }),
    recordType: "NOTEBOOK_PAGE_SAMPLE",
    recordId: pageData?.id || 0,
    onSuccess: handleSignAndReject,
    onCancel: handleRejectSignCancelled,
  });

  // Trigger for per-row approve: stash context in state then open e-sig
  const handleTriggerApprove = useCallback(
    (sample, test) => {
      setPendingApproval({ sample, test });
      openApproveSignatureModal();
    },
    [openApproveSignatureModal],
  );

  // Trigger for reject modal Save button: close modal then open REJECTED e-sig
  const handleTriggerReject = useCallback(() => {
    setRejectModalOpen(false);
    openRejectSignatureModal();
  }, [openRejectSignatureModal]);

  // Calculate stats
  const totalSamples = samples.length;
  const pendingVerification = samples.reduce(
    (sum, s) => sum + (s.pendingVerification || 0),
    0,
  );
  const verified = samples.reduce((sum, s) => sum + (s.verified || 0), 0);
  const totalTests = pendingVerification + verified;

  // Get status tag type
  const getStatusTagType = (status) => {
    switch (status) {
      case "VERIFIED":
        return "green";
      case "PENDING_VERIFICATION":
        return "blue";
      case "REJECTED":
        return "red";
      default:
        return "gray";
    }
  };

  return (
    <div className="result-verification-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="medlab.page.resultVerification.title"
            defaultMessage="Result Verification"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="medlab.page.resultVerification.description"
            defaultMessage="Review and verify entered test results before reporting."
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
                  id="medlab.page.resultVerification.totalResults"
                  defaultMessage="Total Results"
                />
              </span>
              <span className="progress-value">{totalTests}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="medlab.page.resultVerification.verified"
                  defaultMessage="Verified"
                />
              </span>
              <span className="progress-value">{verified}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="medlab.page.resultVerification.pending"
                  defaultMessage="Pending Verification"
                />
              </span>
              <span className="progress-value">{pendingVerification}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="medlab.page.resultVerification.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{totalSamples}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Error Display */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          hideCloseButton
          lowContrast
        />
      )}

      {/* Loading State */}
      {loading && <Loading />}

      {/* Samples with Results for Verification */}
      {!loading && samples.length > 0 && (
        <div className="orders-section">
          <h5>
            <FormattedMessage
              id="medlab.page.resultVerification.resultsForVerification"
              defaultMessage="Results Pending Verification"
            />
          </h5>
          {samples.map((sample) => (
            <div
              key={sample.id}
              className="sample-card"
              style={{ marginBottom: "1rem" }}
            >
              <Tile style={{ padding: "1rem" }}>
                <Grid>
                  <Column lg={4} md={2} sm={2}>
                    <strong>
                      <FormattedMessage id="sample.label.labnumber" />:
                    </strong>{" "}
                    {sample.labNo}
                  </Column>
                  <Column lg={4} md={2} sm={2}>
                    <strong>
                      <FormattedMessage id="patient.label" />:
                    </strong>{" "}
                    {sample.patientName}
                  </Column>
                  <Column lg={4} md={2} sm={2}>
                    <strong>
                      <FormattedMessage
                        id="sample.sampleType"
                        defaultMessage="Sample Type"
                      />
                      :
                    </strong>{" "}
                    {sample.sampleType}
                  </Column>
                  <Column lg={4} md={2} sm={2}>
                    <Tag type={getStatusTagType(sample.verificationStatus)}>
                      {sample.verified || 0}/{(sample.tests || []).length}{" "}
                      <FormattedMessage
                        id="medlab.verification.verified"
                        defaultMessage="verified"
                      />
                    </Tag>
                  </Column>
                </Grid>

                {/* Tests Table */}
                <div style={{ marginTop: "1rem" }}>
                  <DataTable
                    rows={(sample.tests || []).map((t, idx) => ({
                      ...t,
                      id: `${sample.id}-${t.testId}-${idx}`,
                    }))}
                    headers={[
                      {
                        key: "testName",
                        header: intl.formatMessage({
                          id: "test.testName",
                          defaultMessage: "Test",
                        }),
                      },
                      {
                        key: "resultValue",
                        header: intl.formatMessage({
                          id: "medlab.result.value",
                          defaultMessage: "Result",
                        }),
                      },
                      {
                        key: "verificationStatus",
                        header: intl.formatMessage({
                          id: "medlab.verification.status",
                          defaultMessage: "Status",
                        }),
                      },
                      {
                        key: "actions",
                        header: intl.formatMessage({
                          id: "label.button.actions",
                        }),
                      },
                    ]}
                    size="sm"
                  >
                    {({ rows, headers, getHeaderProps, getTableProps }) => (
                      <TableContainer>
                        <Table {...getTableProps()} size="sm">
                          <TableHead>
                            <TableRow>
                              {headers.map((header) => (
                                <TableHeader
                                  key={header.key}
                                  {...getHeaderProps({ header })}
                                >
                                  {header.header}
                                </TableHeader>
                              ))}
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {rows.map((row) => {
                              const test =
                                sample.tests.find(
                                  (t) =>
                                    `${sample.id}-${t.testId}` ===
                                    row.id.substring(
                                      0,
                                      row.id.lastIndexOf("-"),
                                    ),
                                ) || sample.tests[rows.indexOf(row)];
                              return (
                                <TableRow key={row.id}>
                                  {row.cells.map((cell) => (
                                    <TableCell key={cell.id}>
                                      {cell.info.header === "actions" ? (
                                        test?.verificationStatus ===
                                        "PENDING_VERIFICATION" ? (
                                          <div
                                            style={{
                                              display: "flex",
                                              gap: "0.5rem",
                                            }}
                                          >
                                            <PermissionGate
                                              roles={
                                                Permissions.VALIDATE_RESULTS
                                              }
                                              disabledTooltip="You need validation permission to approve results"
                                            >
                                              <Button
                                                kind="primary"
                                                size="sm"
                                                renderIcon={Checkmark}
                                                onClick={() =>
                                                  handleTriggerApprove(
                                                    sample,
                                                    test,
                                                  )
                                                }
                                              >
                                                <FormattedMessage
                                                  id="medlab.verification.approve"
                                                  defaultMessage="Approve"
                                                />
                                              </Button>
                                            </PermissionGate>
                                            <PermissionGate
                                              roles={
                                                Permissions.VALIDATE_RESULTS
                                              }
                                              disabledTooltip="You need validation permission to reject results"
                                            >
                                              <Button
                                                kind="danger"
                                                size="sm"
                                                renderIcon={Close}
                                                onClick={() =>
                                                  handleOpenRejectModal(
                                                    sample,
                                                    test,
                                                  )
                                                }
                                              >
                                                <FormattedMessage
                                                  id="medlab.verification.reject"
                                                  defaultMessage="Reject"
                                                />
                                              </Button>
                                            </PermissionGate>
                                          </div>
                                        ) : (
                                          <span>-</span>
                                        )
                                      ) : cell.info.header ===
                                        "verificationStatus" ? (
                                        <Tag
                                          type={getStatusTagType(cell.value)}
                                          size="sm"
                                        >
                                          {cell.value}
                                        </Tag>
                                      ) : (
                                        cell.value
                                      )}
                                    </TableCell>
                                  ))}
                                </TableRow>
                              );
                            })}
                          </TableBody>
                        </Table>
                      </TableContainer>
                    )}
                  </DataTable>
                </div>
              </Tile>
            </div>
          ))}
        </div>
      )}

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="medlab.page.resultVerification.empty"
              defaultMessage="No results available for verification. Enter results on the Result Entry page first."
            />
          </p>
        </div>
      )}

      {/* Rejection Modal */}
      <Modal
        open={rejectModalOpen}
        onRequestClose={() => setRejectModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "medlab.page.resultVerification.rejectModal",
          defaultMessage: "Reject Result",
        })}
        passiveModal
        danger
        size="sm"
      >
        <Grid>
          {selectedSample && selectedTest && (
            <>
              <Column lg={16} md={8} sm={4}>
                <Tile
                  className="order-info-tile"
                  style={{ marginBottom: "1rem" }}
                >
                  <strong>
                    <FormattedMessage id="sample.label.labnumber" />:
                  </strong>{" "}
                  {selectedSample.labNo}
                  <br />
                  <strong>
                    <FormattedMessage
                      id="test.testName"
                      defaultMessage="Test"
                    />
                    :
                  </strong>{" "}
                  {selectedTest.testName}
                  <br />
                  <strong>
                    <FormattedMessage
                      id="medlab.result.value"
                      defaultMessage="Result"
                    />
                    :
                  </strong>{" "}
                  {selectedTest.resultValue}
                </Tile>
              </Column>
              <Column lg={16} md={8} sm={4}>
                <TextArea
                  id="rejection-comments"
                  labelText={intl.formatMessage({
                    id: "medlab.verification.comments",
                    defaultMessage: "Rejection Comments",
                  })}
                  value={rejectionComments}
                  onChange={(e) => setRejectionComments(e.target.value)}
                  rows={3}
                  placeholder={intl.formatMessage({
                    id: "medlab.verification.comments.placeholder",
                    defaultMessage:
                      "Enter reason for rejection (e.g., value out of range, re-test required)",
                  })}
                />
              </Column>
            </>
          )}
        </Grid>

        {/* Custom footer with E-Signature trigger */}
        <div
          style={{
            display: "flex",
            justifyContent: "flex-end",
            gap: "1rem",
            marginTop: "1rem",
            paddingTop: "1rem",
            borderTop: "1px solid #e0e0e0",
          }}
        >
          <Button kind="secondary" onClick={() => setRejectModalOpen(false)}>
            <FormattedMessage id="label.button.cancel" />
          </Button>
          <Button kind="danger" onClick={handleTriggerReject}>
            <FormattedMessage
              id="medlab.verification.reject"
              defaultMessage="Reject"
            />
          </Button>
        </div>
      </Modal>

      {/* E-Signature Modal for Approve (VALIDATED_AND_RELEASED) */}
      <ESignatureModal {...approveSignatureModalProps} />

      {/* E-Signature Modal for Reject (REJECTED) */}
      <ESignatureModal {...rejectSignatureModalProps} />
    </div>
  );
}

export default ResultVerificationPage;
