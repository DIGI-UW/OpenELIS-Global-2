import React, {
  useState,
  useEffect,
  useCallback,
  useRef,
  useContext,
} from "react";
import {
  Loading,
  Button,
  InlineNotification,
  Select,
  SelectItem,
  Grid,
  Column,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
  Tag,
} from "@carbon/react";
import { SendAlt, Download } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import UserSessionDetailsContext from "../../../../UserSessionDetailsContext";
import RequestorDetailsSection from "./sections/RequestorDetailsSection";
import SampleSelectionSection from "./sections/SampleSelectionSection";
import {
  formatQuantityWithUnit,
} from "../biorepository/biorepositoryQuantityHelpers";
import {
  buildReferenceItemsPayload,
  formatRequestedReferenceSummary,
} from "./biorepoRequestReferenceHelpers";
import {
  getApiErrorMessage,
  getDepartmentItemStatusDisplay,
  isApiErrorResponse,
} from "../biorepository/biorepoRetrievalStatusHelpers";
import {
  buildRequestorSessionDefaults,
  deriveDestinationType,
  mergePrefillFields,
  resolveRequesterLabUnit,
  validateBrf02RequestForm,
} from "./biorepoImportFormHelpers";
import IntendedUseSection from "./sections/IntendedUseSection";

/**
 * BiorepoSampleImportPage - Generic Biorepository Sample Request Form
 * (AHRI BR-F-02)
 *
 * Used in notebook workflows to import samples from the biorepository.
 *
 * Two independent sections:
 * 1. Request Form — always editable, creates new retrieval requests, clears after submission
 * 2. Requested Samples Table — always visible, shows all requests and their item statuses,
 *    with a "Collect" action for retrieved samples
 */
function BiorepoSampleImportPage({
  entryId,
  notebookId,
  pageData,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(true);
  const { userSessionDetails } = useContext(UserSessionDetailsContext);

  // Loading state
  const [loading, setLoading] = useState(true);

  // --- Request Form State (independent) ---
  const [formData, setFormData] = useState({
    requestorName: "",
    requesterLabUnit: "",
    principalInvestigator: "",
    projectTitle: "",
    requesterContactInfo: "",
    intendedUseDescription: "",
    samplesWillBeDestroyed: null,
    estimatedReturnDate: null,
    destinationType: "ANALYSIS_RETURN",
    priorityLevel: "NORMAL",
  });
  const [selectedSamples, setSelectedSamples] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(null);
  const [submitSuccess, setSubmitSuccess] = useState(false);
  const [showDestroyValidation, setShowDestroyValidation] = useState(false);
  const [showReturnDateValidation, setShowReturnDateValidation] = useState(false);
  const notebookDefaultsRef = useRef({
    principalInvestigator: "",
    projectTitle: "",
    requesterLabUnit: "",
  });

  // --- Requested Samples Table State (independent) ---
  const [existingRequests, setExistingRequests] = useState([]);
  const [collecting, setCollecting] = useState(false);

  const applyNotebookPrefill = useCallback(
    (entryData, notebookData) => {
      const entryTitle = entryData?.title || "";
      const requesterLabUnit = resolveRequesterLabUnit({
        entryData,
        notebookData,
        pageData,
        session: userSessionDetails,
      });

      const notebookPrefill = {
        principalInvestigator: notebookData?.principalInvestigator || "",
        projectTitle:
          entryTitle ||
          notebookData?.title ||
          notebookData?.notebookName ||
          "",
        requesterLabUnit,
      };

      notebookDefaultsRef.current = {
        principalInvestigator: notebookPrefill.principalInvestigator,
        projectTitle: notebookPrefill.projectTitle,
        requesterLabUnit: notebookPrefill.requesterLabUnit,
      };

      setFormData((prev) => {
        const withNotebook = mergePrefillFields(prev, notebookPrefill);
        return mergePrefillFields(
          withNotebook,
          buildRequestorSessionDefaults(
            userSessionDetails,
            notebookPrefill.requesterLabUnit || withNotebook.requesterLabUnit,
          ),
        );
      });
    },
    [pageData, userSessionDetails],
  );

  // Prefill requestor name/contact from session; lab unit from notebook when available
  useEffect(() => {
    if (!userSessionDetails) {
      return;
    }
    const labUnit =
      notebookDefaultsRef.current.requesterLabUnit ||
      resolveRequesterLabUnit({ pageData, session: userSessionDetails });
    setFormData((prev) =>
      mergePrefillFields(
        prev,
        buildRequestorSessionDefaults(userSessionDetails, labUnit),
      ),
    );
  }, [userSessionDetails, pageData]);

  // Prefill PI / project / lab unit from notebook entry and template
  useEffect(() => {
    if (!entryId) {
      return;
    }

    getFromOpenElisServer(`/rest/notebook-entry/${entryId}`, (entryData) => {
      if (!componentMounted.current || !entryData) {
        return;
      }

      const resolvedNotebookId = notebookId || entryData.notebook?.id;
      if (!resolvedNotebookId) {
        applyNotebookPrefill(entryData, null);
        return;
      }

      getFromOpenElisServer(
        `/rest/notebook/view/${resolvedNotebookId}`,
        (notebookData) => {
          if (!componentMounted.current) {
            return;
          }
          applyNotebookPrefill(entryData, notebookData || null);
        },
      );
    });
  }, [entryId, notebookId, applyNotebookPrefill]);

  // Load all existing retrieval requests for this notebook entry
  const loadExistingRequests = useCallback(() => {
    if (!entryId) {
      setLoading(false);
      return;
    }
    getFromOpenElisServer(
      `/rest/biorepository/retrieval/by-entry/${entryId}`,
      (data) => {
        if (!componentMounted.current) return;
        setLoading(false);
        if (data && Array.isArray(data)) {
          setExistingRequests(
            data.filter(
              (r) => r.status !== "CANCELLED" && r.status !== "REJECTED",
            ),
          );
        } else {
          setExistingRequests([]);
        }
      },
    );
  }, [entryId]);

  useEffect(() => {
    loadExistingRequests();
    return () => {
      componentMounted.current = false;
    };
  }, [loadExistingRequests]);

  const handleFormChange = useCallback((field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    if (field === "samplesWillBeDestroyed") {
      setShowDestroyValidation(false);
      if (value === true) {
        setShowReturnDateValidation(false);
      }
    }
    if (field === "estimatedReturnDate") {
      setShowReturnDateValidation(false);
    }
  }, []);

  // Reset form to initial state while retaining session/notebook prefills
  const resetForm = useCallback(() => {
    const requesterLabUnit =
      notebookDefaultsRef.current.requesterLabUnit ||
      resolveRequesterLabUnit({ pageData, session: userSessionDetails });
    const sessionDefaults = buildRequestorSessionDefaults(
      userSessionDetails || {},
      requesterLabUnit,
    );
    setFormData((prev) => ({
      ...prev,
      ...sessionDefaults,
      requesterLabUnit:
        notebookDefaultsRef.current.requesterLabUnit ||
        sessionDefaults.requesterLabUnit,
      principalInvestigator:
        notebookDefaultsRef.current.principalInvestigator ||
        prev.principalInvestigator,
      projectTitle:
        notebookDefaultsRef.current.projectTitle || prev.projectTitle,
      intendedUseDescription: "",
      samplesWillBeDestroyed: null,
      estimatedReturnDate: null,
      destinationType: "ANALYSIS_RETURN",
      priorityLevel: "NORMAL",
    }));
    setSelectedSamples([]);
    setSubmitError(null);
    setShowDestroyValidation(false);
    setShowReturnDateValidation(false);
  }, [userSessionDetails, pageData]);

  // Submit a new request
  const handleSubmit = useCallback(() => {
    const validationErrors = validateBrf02RequestForm(formData, selectedSamples);
    const destroyMissing =
      formData.samplesWillBeDestroyed !== true &&
      formData.samplesWillBeDestroyed !== false;
    const returnDateMissing =
      formData.samplesWillBeDestroyed === false && !formData.estimatedReturnDate;

    setShowDestroyValidation(destroyMissing);
    setShowReturnDateValidation(returnDateMissing);

    if (validationErrors.length > 0) {
      setSubmitError(validationErrors[0]);
      return;
    }

    setSubmitting(true);
    setSubmitError(null);

    const destinationType = deriveDestinationType(formData.samplesWillBeDestroyed);

    const requestBody = {
      requestPurpose: formData.intendedUseDescription,
      items: buildReferenceItemsPayload(selectedSamples),
      projectId: null,
      ethicsApprovalRef: null,
      destinationType,
      destinationDetails: null,
      priorityLevel: formData.priorityLevel,
      requiredByDate: null,
      notebookEntryId: entryId,
      requestorName: formData.requestorName || null,
      requesterLabUnit: formData.requesterLabUnit || null,
      principalInvestigator: formData.principalInvestigator || null,
      projectTitle: formData.projectTitle || null,
      requesterContactInfo: formData.requesterContactInfo || null,
      intendedUseDescription: formData.intendedUseDescription || null,
      samplesWillBeDestroyed: formData.samplesWillBeDestroyed,
      estimatedReturnDate: formData.estimatedReturnDate
        ? formatDate(formData.estimatedReturnDate)
        : null,
    };

    postToOpenElisServerJsonResponse(
      "/rest/biorepository/retrieval/requests",
      JSON.stringify(requestBody),
      (data) => {
        if (!componentMounted.current) return;

        if (isApiErrorResponse(data)) {
          setSubmitting(false);
          setSubmitError(
            getApiErrorMessage(data, "Failed to create sample request."),
          );
          return;
        }

        if (data && data.id) {
          postToOpenElisServerJsonResponse(
            `/rest/biorepository/retrieval/requests/${data.id}/submit`,
            "{}",
            (submitData) => {
              if (!componentMounted.current) return;
              setSubmitting(false);

              if (isApiErrorResponse(submitData)) {
                setSubmitError(
                  getApiErrorMessage(
                    submitData,
                    "Request was saved but could not be submitted for approval. Please contact Biorepository support.",
                  ),
                );
                loadExistingRequests();
                return;
              }

              if (submitData?.status !== "PENDING_APPROVAL") {
                setSubmitError(
                  "Request was saved but is not pending approval. Please contact Biorepository support.",
                );
                loadExistingRequests();
                return;
              }

              setSubmitSuccess(true);
              resetForm();
              loadExistingRequests();
            },
          );
        } else {
          setSubmitting(false);
          setSubmitError("Failed to create sample request. No request ID returned.");
        }
      },
    );
  }, [
    selectedSamples,
    formData,
    entryId,
    intl,
    resetForm,
    loadExistingRequests,
  ]);

  // Collect retrieved samples — link them to the notebook, then close modal
  const handleCollect = useCallback(
    (request) => {
      const targetNotebookId = request.notebookId || notebookId;
      if (!targetNotebookId) return;

      setCollecting(true);
      postToOpenElisServerJsonResponse(
        `/rest/biorepository/retrieval/requests/${request.id}/link-to-notebook`,
        JSON.stringify({ notebookId: targetNotebookId }),
        (data) => {
          if (!componentMounted.current) return;
          setCollecting(false);
          if (data && data.error) {
            setSubmitError(data.error);
            return;
          }
          // Close modal and refresh parent page
          if (onProgressUpdate) onProgressUpdate();
        },
      );
    },
    [notebookId, onProgressUpdate],
  );

  const getItemStatusDisplay = useCallback(
    (item, requestStatus) =>
      getDepartmentItemStatusDisplay(item, requestStatus, intl),
    [intl],
  );

  // Flatten all items from all requests for the table
  const allRequestedItems = existingRequests.flatMap((req) =>
    (req.items || [])
      .filter((item) => item.itemRole !== "FULFILLMENT" && !item.fulfillsItemId)
      .map((item) => ({
        ...item,
        requestNumber: req.requestNumber,
        requestStatus: req.status,
        requestId: req.id,
        request: req,
      })),
  );

  if (loading) {
    return <Loading withOverlay={false} />;
  }

  return (
    <div
      className="biorepo-sample-import-page"
      style={{ padding: "1rem 0", maxWidth: "1200px" }}
    >
      {/* ===== REQUESTED SAMPLES TABLE (always visible) ===== */}
      {allRequestedItems.length > 0 && (
        <div style={{ marginBottom: "2rem" }}>
          <h4 style={{ marginBottom: "1rem" }}>
            <FormattedMessage
              id="biorepo.import.section.requestedSamples"
              defaultMessage="Requested Samples"
            />
          </h4>
          <DataTable
            rows={allRequestedItems.map((item, idx) => ({
              id: item.id?.toString() || idx.toString(),
              reference: formatRequestedReferenceSummary(item),
              sampleType:
                item.requestedSampleType || item.sampleType || "-",
              requestNumber: item.requestNumber || "-",
              quantityRequested: formatQuantityWithUnit(
                item.quantityRequested,
                item.unitOfMeasure,
              ),
              quantityReleased: formatQuantityWithUnit(
                item.quantityReleased ??
                  (item.fulfillments || []).reduce(
                    (sum, f) => sum + (Number(f.quantityReleased) || 0),
                    0,
                  ),
                item.unitOfMeasure,
              ),
              fulfilledSample:
                item.externalId ||
                item.barcode ||
                (item.fulfillments || [])
                  .map((f) => f.externalId || f.barcode)
                  .filter(Boolean)
                  .join(", ") ||
                "-",
              status: item.status,
            }))}
            headers={[
              {
                key: "reference",
                header: intl.formatMessage({
                  id: "biorepo.import.reference.requested",
                  defaultMessage: "Requested Reference",
                }),
              },
              {
                key: "sampleType",
                header: intl.formatMessage({
                  id: "biorepo.import.field.sampleType",
                  defaultMessage: "Sample Type",
                }),
              },
              {
                key: "requestNumber",
                header: intl.formatMessage({
                  id: "biorepo.import.field.requestNumber",
                  defaultMessage: "Request #",
                }),
              },
              {
                key: "quantityRequested",
                header: intl.formatMessage({
                  id: "biorepo.import.field.quantityRequested",
                  defaultMessage: "Requested",
                }),
              },
              {
                key: "fulfilledSample",
                header: intl.formatMessage({
                  id: "biorepo.import.reference.fulfilledSample",
                  defaultMessage: "Fulfilled Sample",
                }),
              },
              {
                key: "quantityReleased",
                header: intl.formatMessage({
                  id: "biorepo.import.field.quantityReleased",
                  defaultMessage: "Released",
                }),
              },
              {
                key: "status",
                header: intl.formatMessage({
                  id: "biorepo.import.field.status",
                  defaultMessage: "Status",
                }),
              },
            ]}
            size="sm"
          >
            {({
              rows,
              headers,
              getTableProps,
              getHeaderProps,
              getRowProps,
            }) => (
              <TableContainer>
                <Table {...getTableProps()}>
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
                      const item = allRequestedItems.find(
                        (i) => (i.id?.toString() || "") === row.id,
                      );
                      const statusDisplay = getItemStatusDisplay(
                        item || {},
                        item?.requestStatus,
                      );
                      return (
                        <TableRow key={row.id} {...getRowProps({ row })}>
                          <TableCell>{row.cells[0].value}</TableCell>
                          <TableCell>{row.cells[1].value}</TableCell>
                          <TableCell>{row.cells[2].value}</TableCell>
                          <TableCell>{row.cells[3].value}</TableCell>
                          <TableCell>{row.cells[4].value}</TableCell>
                          <TableCell>{row.cells[5].value}</TableCell>
                          <TableCell>
                            <Tag type={statusDisplay.tagType} size="sm">
                              {statusDisplay.label}
                            </Tag>
                          </TableCell>
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
          <div style={{ marginTop: "1rem" }}>
            <Button
              kind="primary"
              size="sm"
              renderIcon={Download}
              onClick={() => {
                // Collect from all requests that have retrievable items
                const requestsToCollect = existingRequests.filter((req) =>
                  (req.items || []).some((item) => item.status === "RETRIEVED"),
                );
                if (requestsToCollect.length === 0) return;
                setCollecting(true);
                let completed = 0;
                requestsToCollect.forEach((req) => {
                  handleCollect(req);
                  completed++;
                  if (completed === requestsToCollect.length) {
                    setCollecting(false);
                  }
                });
              }}
              disabled={collecting}
            >
              <FormattedMessage
                id="biorepo.import.collectAll"
                defaultMessage="Collect All Retrieved Samples"
              />
            </Button>
          </div>
        </div>
      )}

      {/* ===== NEW REQUEST FORM (always editable) ===== */}
      <h3 style={{ marginBottom: "0.5rem" }}>
        <FormattedMessage
          id="biorepo.import.newRequest.title"
          defaultMessage="New Sample Request"
        />
      </h3>
      <p style={{ marginBottom: "1.5rem", color: "#525252" }}>
        <FormattedMessage
          id="biorepo.import.newRequest.description"
          defaultMessage="Submit a new request for biorepository samples. After submission, the request will be reviewed and approved by a biorepository supervisor. You can track the status of your requests in the table above."
        />
      </p>

      {submitSuccess && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({
            id: "biorepo.import.success.title",
            defaultMessage: "Request Submitted",
          })}
          subtitle={intl.formatMessage({
            id: "biorepo.import.success.description",
            defaultMessage:
              "Your sample request has been submitted for approval. The biorepository supervisor will review it.",
          })}
          lowContrast
          style={{ marginBottom: "1rem" }}
          onClose={() => setSubmitSuccess(false)}
        />
      )}

      {submitError && (
        <InlineNotification
          kind="error"
          title={submitError}
          lowContrast
          style={{ marginBottom: "1rem" }}
          onClose={() => setSubmitError(null)}
        />
      )}

      {/* Section A: Requestor Details */}
      <RequestorDetailsSection
        formData={formData}
        onChange={handleFormChange}
        readOnly={false}
      />

      {/* Section B: Sample Selection */}
      <SampleSelectionSection
        selectedSamples={selectedSamples}
        onSamplesChange={setSelectedSamples}
        readOnly={false}
      />

      {/* Section C: Intended Use */}
      <IntendedUseSection
        formData={formData}
        onChange={handleFormChange}
        readOnly={false}
        showDestroyValidation={showDestroyValidation}
        showReturnDateValidation={showReturnDateValidation}
      />

      {/* Request configuration (priority only; destination derived on submit) */}
      <Grid condensed style={{ marginBottom: "2rem" }}>
        <Column lg={8} md={4} sm={4}>
          <Select
            id="priorityLevel"
            labelText={intl.formatMessage({
              id: "biorepo.import.field.priorityLevel",
              defaultMessage: "Priority Level",
            })}
            value={formData.priorityLevel}
            onChange={(e) => handleFormChange("priorityLevel", e.target.value)}
          >
            <SelectItem value="NORMAL" text="Normal" />
            <SelectItem value="URGENT" text="Urgent" />
            <SelectItem value="CRITICAL" text="Critical" />
          </Select>
        </Column>
      </Grid>

      {/* Submit button */}
      <Button
        kind="primary"
        renderIcon={SendAlt}
        onClick={handleSubmit}
        disabled={submitting || selectedSamples.length === 0}
      >
        {submitting ? (
          <FormattedMessage
            id="biorepo.import.submitting"
            defaultMessage="Submitting..."
          />
        ) : (
          <FormattedMessage
            id="biorepo.import.submit"
            defaultMessage="Submit Request for Approval"
          />
        )}
      </Button>
    </div>
  );
}

function formatDate(date) {
  if (!date) return null;
  if (typeof date === "string") return date;
  const d = new Date(date);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

export default BiorepoSampleImportPage;
