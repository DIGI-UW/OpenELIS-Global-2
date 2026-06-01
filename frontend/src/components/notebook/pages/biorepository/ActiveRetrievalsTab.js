import React, { useState, useCallback, useEffect } from "react";
import {
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
  TableToolbar,
  TableToolbarContent,
  TableBatchActions,
  TableBatchAction,
  TableSelectAll,
  TableSelectRow,
  Button,
  Modal,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  NumberInput,
  Checkbox,
  Tag,
  InlineNotification,
  Loading,
  ExpandableSearch,
  Pagination,
} from "@carbon/react";
import { Renew, Catalog, Checkmark } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import PropTypes from "prop-types";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import BiorepositoryLifecycleModal from "./BiorepositoryLifecycleModal";
import AttachSamplePanel, { buildFiltersFromReferenceItem } from "./AttachSamplePanel";
import FulfillmentReviewBlock from "./FulfillmentReviewBlock";
import { formatQuantityWithUnit } from "./biorepositoryQuantityHelpers";
import { formatBrf02SamplePath } from "./biorepositorySamplePathHelpers";
import { formatRequestedReferenceSummary } from "../common/biorepoRequestReferenceHelpers";
import {
  buildFulfillmentSearchQuery,
  hasActiveSearchFilters,
} from "../common/biorepoSampleSearchHelpers";
import {
  buildBulkSuggestionRequest,
  buildSuggestionSummaryFromResults,
  formatSamplePath,
  formatTopCandidateIdentity,
  getSuggestionStatusDefaultMessage,
  getSuggestionStatusMessageId,
  getSuggestionSummary,
  getSuggestionTagProps,
  getTopCandidate,
  hasTypeMismatch,
  hasUsableTopSuggestion,
  isValidSuggestionApiMap,
  lookupSuggestion,
  mapApiSuggestionResponse,
  mergeSuggestionMaps,
  normalizeRetrievalItemId,
  SUGGESTION_STATUS,
  SUGGESTIONS_LOAD_STATE,
  resolveDefaultAttachQuantity,
  validateAttachQuantity,
} from "./biorepoFulfillmentSuggestionHelpers";
import {
  countUnresolvedReferenceRows,
  findNextAttachTarget,
  getItemDisplayStatus,
  getRequestCompletionBlockReason,
  getRequestDisplayStatus,
  getRequestLineCount,
  getUnresolvedReferenceItems,
  isUnresolvedReferenceItem,
  resolveItemStatus,
} from "./biorepoRetrievalStatusHelpers";

/**
 * ActiveRetrievalsTab - Track and manage checked-out samples
 *
 * Features:
 * - View approved requests with work orders
 * - Record physical retrieval from storage
 * - Release samples to requester
 * - Process sample returns
 * - Flag overdue items
 */
function ActiveRetrievalsTab({ onActionComplete, refreshToken }) {
  const intl = useIntl();

  // Data state
  const [activeRequests, setActiveRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Pagination
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [searchTerm, setSearchTerm] = useState("");

  // Modal state
  const [selectedItem, setSelectedItem] = useState(null);
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [retrieveModalOpen, setRetrieveModalOpen] = useState(false);
  const [releaseModalOpen, setReleaseModalOpen] = useState(false);
  const [returnModalOpen, setReturnModalOpen] = useState(false);
  const [workOrderModalOpen, setWorkOrderModalOpen] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [workOrderLoading, setWorkOrderLoading] = useState(false);
  const [lifecycleModalOpen, setLifecycleModalOpen] = useState(false);
  const [lifecycleContext, setLifecycleContext] = useState(null);

  // Batch selection state
  const [selectedRows, setSelectedRows] = useState([]);

  const [attachTargetItem, setAttachTargetItem] = useState(null);
  const [suggestionsByItemId, setSuggestionsByItemId] = useState({});
  const [suggestionsLoading, setSuggestionsLoading] = useState(false);
  const [suggestionsLoadState, setSuggestionsLoadState] = useState(
    SUGGESTIONS_LOAD_STATE.IDLE,
  );
  const [suggestionsError, setSuggestionsError] = useState(null);
  const [suggestionsUsedFallback, setSuggestionsUsedFallback] = useState(false);
  const [highlightedItemId, setHighlightedItemId] = useState(null);
  const [quickAttachTarget, setQuickAttachTarget] = useState(null);
  const [quickAttachQuantity, setQuickAttachQuantity] = useState("");
  const [quickAttachError, setQuickAttachError] = useState(null);
  const [attachSuccessMessage, setAttachSuccessMessage] = useState(null);
  const [quickAttachLoading, setQuickAttachLoading] = useState(false);

  // Retrieve form state
  const [conditionAtRelease, setConditionAtRelease] = useState("Good");
  const [conditionNotes, setConditionNotes] = useState("");
  const [temperatureAtRetrieval, setTemperatureAtRetrieval] = useState("");
  const [quantityReleased, setQuantityReleased] = useState("");
  const [receivedByName, setReceivedByName] = useState("");

  // Return form state
  const [returnedCondition, setReturnedCondition] = useState("Good");
  const [returnNotes, setReturnNotes] = useState("");
  const [fullyConsumed, setFullyConsumed] = useState(false);

  // Load active data
  const loadData = useCallback(() => {
    setLoading(true);
    setError(null);

    // Load approved/in-progress requests
    getFromOpenElisServer(
      "/rest/biorepository/retrieval/requests?status=APPROVED,IN_PROGRESS,PARTIALLY_COMPLETED",
      (data) => {
        if (data && Array.isArray(data)) {
          setActiveRequests(data);
        } else if (data && !data.error) {
          setActiveRequests([]);
        }
        setLoading(false);
      },
    );
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData, refreshToken]);

  // Filter data
  const filteredRequests = activeRequests.filter((r) => {
    if (!searchTerm) return true;
    const term = searchTerm.toLowerCase();
    return (
      (r.requestNumber && r.requestNumber.toLowerCase().includes(term)) ||
      (r.requestedByName && r.requestedByName.toLowerCase().includes(term)) ||
      (r.workOrderNumber && r.workOrderNumber.toLowerCase().includes(term))
    );
  });

  // Paginate
  const paginatedData = filteredRequests.slice(
    (page - 1) * pageSize,
    page * pageSize,
  );

  const resetRetrieveForm = () => {
    setConditionAtRelease("Good");
    setConditionNotes("");
    setTemperatureAtRetrieval("");
    setQuantityReleased("");
  };

  const resetReturnForm = () => {
    setReturnedCondition("Good");
    setReturnNotes("");
    setFullyConsumed(false);
  };

  const loadSuggestionsViaGet = useCallback(
    (unresolvedItems) =>
      Promise.all(
        unresolvedItems.map(
          (item) =>
            new Promise((resolve) => {
              const key = normalizeRetrievalItemId(item.id);
              const filters = buildFiltersFromReferenceItem(item);
              if (!hasActiveSearchFilters(filters)) {
                resolve([
                  key,
                  {
                    status: SUGGESTION_STATUS.NO_CRITERIA,
                    results: [],
                    topSuggestion: null,
                    exactMatchFound: false,
                    fallbackUsed: false,
                    noExactMatch: false,
                  },
                ]);
                return;
              }
              const query = buildFulfillmentSearchQuery(filters, { status: "STORED" });
              getFromOpenElisServer(
                `/rest/biorepository/sample/search?${query}&context=fulfillment`,
                (data) => {
                  resolve([key, buildSuggestionSummaryFromResults(data)]);
                },
              );
            }),
        ),
      ).then((entries) => Object.fromEntries(entries)),
    [],
  );

  const buildSyntheticSuggestionMap = useCallback(
    (unresolvedItems, existingMap = {}) =>
      Object.fromEntries(
        unresolvedItems.map((item) => {
          const key = normalizeRetrievalItemId(item.id);
          const existing = key ? existingMap[key] : null;
          if (existing) {
            return [key, existing];
          }

          const filters = buildFiltersFromReferenceItem(item);
          const hasCriteria = hasActiveSearchFilters(filters);

          return [
            key,
            {
              status: hasCriteria
                ? SUGGESTION_STATUS.NO_CANDIDATE
                : SUGGESTION_STATUS.NO_CRITERIA,
              results: [],
              topSuggestion: null,
              exactMatchFound: false,
              fallbackUsed: false,
              noExactMatch: false,
              summary: null,
            },
          ];
        }),
      ),
    [],
  );

  const loadSuggestionReview = useCallback(
    (requestData, onComplete) => {
      const unresolvedItems = getUnresolvedReferenceItems(requestData);

      if (unresolvedItems.length === 0) {
        setSuggestionsByItemId({});
        setSuggestionsLoading(false);
        setSuggestionsLoadState(SUGGESTIONS_LOAD_STATE.LOADED);
        setSuggestionsError(null);
        setSuggestionsUsedFallback(false);
        return;
      }

      setSuggestionsLoading(true);
      setSuggestionsLoadState(SUGGESTIONS_LOAD_STATE.LOADING);
      setSuggestionsError(null);
      setSuggestionsUsedFallback(false);

      const finishLoad = (
        merged,
        { usedFallback = false, errorMessage = null, onComplete = null } = {},
      ) => {
        const normalizedMap = buildSyntheticSuggestionMap(unresolvedItems, merged);
        setSuggestionsByItemId(normalizedMap);
        setSuggestionsLoading(false);
        setSuggestionsLoadState(
          errorMessage && Object.keys(normalizedMap).length === 0
            ? SUGGESTIONS_LOAD_STATE.ERROR
            : SUGGESTIONS_LOAD_STATE.LOADED,
        );
        setSuggestionsError(errorMessage);
        setSuggestionsUsedFallback(usedFallback);
        if (onComplete) {
          onComplete(normalizedMap);
        }
      };

      postToOpenElisServerJsonResponse(
        "/rest/biorepository/retrieval/items/suggestions",
        JSON.stringify(
          buildBulkSuggestionRequest(unresolvedItems.map((item) => item.id)),
        ),
        async (data) => {
          const postSucceeded = isValidSuggestionApiMap(data);
          const apiErrorDetail =
            !postSucceeded && data && typeof data.error === "string"
              ? data.error
              : null;
          if (!postSucceeded && process.env.NODE_ENV !== "production") {
            // eslint-disable-next-line no-console
            console.warn("Bulk fulfillment suggestions POST failed or returned invalid payload:", data);
          }
          const postMap = postSucceeded ? mapApiSuggestionResponse(data) : {};

          if (postSucceeded && Object.keys(postMap).length >= unresolvedItems.length) {
            finishLoad(postMap, { onComplete });
            return;
          }

          try {
            const fallbackMap = await loadSuggestionsViaGet(unresolvedItems);
            const merged = mergeSuggestionMaps(postMap, fallbackMap);
            const usedFallback = !postSucceeded || Object.keys(postMap).length === 0;
            const errorMessage = usedFallback
              ? apiErrorDetail
                ? intl.formatMessage(
                    {
                      id: "biorepository.retrieval.workbench.suggestions.apiError",
                      defaultMessage: "Suggestions failed: {detail}",
                    },
                    { detail: apiErrorDetail },
                  )
                : postSucceeded
                  ? intl.formatMessage({
                      id: "biorepository.retrieval.workbench.suggestions.partialPost",
                      defaultMessage:
                        "Some suggestion data was loaded using individual search. Retry if results look incomplete.",
                    })
                  : intl.formatMessage({
                      id: "biorepository.retrieval.workbench.suggestions.fallback",
                      defaultMessage:
                        "Bulk suggestions unavailable. Showing results from individual sample search.",
                    })
              : null;
            finishLoad(merged, { usedFallback, errorMessage, onComplete });
          } catch (fetchError) {
            finishLoad(postMap, {
              errorMessage:
                fetchError?.message ||
                intl.formatMessage({
                  id: "biorepository.retrieval.workbench.suggestions.error",
                  defaultMessage: "Could not load sample suggestions.",
                }),
              onComplete,
            });
          }
        },
      );
    },
    [buildSyntheticSuggestionMap, intl, loadSuggestionsViaGet],
  );

  const refreshSelectedRequest = useCallback((requestId) => {
    if (!requestId) return;
    getFromOpenElisServer(
      `/rest/biorepository/retrieval/requests/${requestId}`,
      (data) => {
        if (data && !data.error) {
          setSelectedRequest(data);
          setSuggestionsLoading(true);
          loadSuggestionReview(data);
        }
      },
    );
  }, [loadSuggestionReview]);

  const afterItemAction = useCallback(
    (requestId) => {
      loadData();
      if (requestId) {
        refreshSelectedRequest(requestId);
      }
      if (onActionComplete) onActionComplete();
    },
    [loadData, refreshSelectedRequest, onActionComplete],
  );

  const openReviewAlternatives = useCallback((item) => {
    setQuickAttachTarget(null);
    setQuickAttachError(null);
    setAttachTargetItem(item);
  }, []);

  const openQuickAttach = useCallback((referenceItem, bioSample) => {
    setAttachTargetItem(null);
    setQuickAttachTarget({ referenceItem, bioSample });
    setQuickAttachError(null);
    const defaultQty = resolveDefaultAttachQuantity(referenceItem, bioSample);
    setQuickAttachQuantity(defaultQty != null ? String(defaultQty) : "");
  }, []);

  const handleAttachSuccess = useCallback(
    (attachResult, errorMessage) => {
      if (errorMessage) {
        setError(errorMessage);
        setAttachSuccessMessage(null);
        return;
      }
      const requestId = selectedRequest?.id;
      const currentItemId =
        attachTargetItem?.id ?? quickAttachTarget?.referenceItem?.id;
      const bioSample = attachResult?.bioSample;
      const updatedRequest = attachResult?.request;

      setQuickAttachTarget(null);
      setAttachTargetItem(null);
      setQuickAttachError(null);

      if (bioSample) {
        setAttachSuccessMessage(
          intl.formatMessage(
            {
              id: "biorepository.retrieval.attach.success",
              defaultMessage: "Attached {identity} to request line.",
            },
            {
              identity:
                bioSample.accessionNumber ||
                bioSample.externalId ||
                bioSample.barcode ||
                String(bioSample.id),
            },
          ),
        );
        setError(null);
      }

      const applyRequestRefresh = (requestData) => {
        if (!requestData || requestData.error) {
          return;
        }
        setSelectedRequest(requestData);
        loadSuggestionReview(requestData, (merged) => {
          const next = findNextAttachTarget(requestData, currentItemId);
          setHighlightedItemId(next?.id ?? null);
          if (next) {
            const nextSuggestion = lookupSuggestion(merged, next.id);
            if (
              nextSuggestion?.status === SUGGESTION_STATUS.EXACT_MATCH &&
              hasUsableTopSuggestion(nextSuggestion)
            ) {
              openQuickAttach(next, getTopCandidate(nextSuggestion));
            }
          }
        });
      };

      loadData();
      if (updatedRequest) {
        applyRequestRefresh(updatedRequest);
      } else if (requestId) {
        getFromOpenElisServer(
          `/rest/biorepository/retrieval/requests/${requestId}`,
          applyRequestRefresh,
        );
      }
      if (onActionComplete) onActionComplete();
    },
    [
      selectedRequest?.id,
      attachTargetItem?.id,
      quickAttachTarget?.referenceItem?.id,
      intl,
      loadData,
      loadSuggestionReview,
      openQuickAttach,
      onActionComplete,
    ],
  );

  const handleQuickAttachConfirm = useCallback(() => {
    if (!quickAttachTarget?.referenceItem?.id || !quickAttachTarget?.bioSample?.id) {
      return;
    }
    const { referenceItem, bioSample } = quickAttachTarget;
    const validation = validateAttachQuantity(
      referenceItem.quantityRequested,
      null,
      quickAttachQuantity,
      { bioSample },
    );
    if (!validation.valid) {
      setQuickAttachError(
        intl.formatMessage({
          id: validation.errorKey,
          defaultMessage: "Invalid quantity for attach",
        }),
      );
      return;
    }

    setQuickAttachLoading(true);
    setQuickAttachError(null);
    postToOpenElisServerJsonResponse(
      `/rest/biorepository/retrieval/items/${referenceItem.id}/attach`,
      JSON.stringify({
        bioSampleId: bioSample.id,
        quantityRequested: validation.quantity,
      }),
      (data) => {
        setQuickAttachLoading(false);
        if (data && data.error) {
          setQuickAttachError(data.error);
          return;
        }
        handleAttachSuccess({
          bioSample,
          request: data.request,
          referenceItem: data.referenceItem,
          fulfillmentItem: data.fulfillmentItem,
        });
      },
    );
  }, [quickAttachTarget, quickAttachQuantity, intl, handleAttachSuccess]);

  // Handle retrieve action
  const handleRetrieve = useCallback(() => {
    if (!selectedItem) return;

    setActionLoading(true);
    postToOpenElisServerJsonResponse(
      `/rest/biorepository/retrieval/items/${selectedItem.id}/retrieve`,
      JSON.stringify({
        conditionAtRelease,
        conditionNotes: conditionNotes || null,
        temperatureAtRetrieval: temperatureAtRetrieval
          ? parseFloat(temperatureAtRetrieval)
          : null,
        quantityReleased: quantityReleased ? parseFloat(quantityReleased) : null,
      }),
      (data) => {
        setActionLoading(false);
        if (data && data.error) {
          setError(data.error);
          return;
        }
        const requestId = selectedItem.retrievalRequestId;
        setRetrieveModalOpen(false);
        setSelectedItem(null);
        resetRetrieveForm();
        afterItemAction(requestId);
      },
    );
  }, [
    selectedItem,
    conditionAtRelease,
    conditionNotes,
    temperatureAtRetrieval,
    quantityReleased,
    afterItemAction,
  ]);

  // Handle release action
  const handleRelease = useCallback(() => {
    if (!selectedItem) return;

    setActionLoading(true);
    postToOpenElisServerJsonResponse(
      `/rest/biorepository/retrieval/items/${selectedItem.id}/release`,
      JSON.stringify({
        receivedByName: receivedByName || null,
      }),
      (data) => {
        setActionLoading(false);
        if (data && data.error) {
          setError(data.error);
          return;
        }
        const requestId = selectedItem.retrievalRequestId;
        setReleaseModalOpen(false);
        setSelectedItem(null);
        setReceivedByName("");
        afterItemAction(requestId);
      },
    );
  }, [selectedItem, receivedByName, afterItemAction]);

  // Handle return action
  const handleReturn = useCallback(() => {
    if (!selectedItem) return;

    setActionLoading(true);
    postToOpenElisServerJsonResponse(
      `/rest/biorepository/retrieval/items/${selectedItem.id}/return`,
      JSON.stringify({
        returnedCondition,
        returnNotes: returnNotes || null,
        fullyConsumed,
      }),
      (data) => {
        setActionLoading(false);
        if (data && data.error) {
          setError(data.error);
          return;
        }
        const requestId = selectedItem.retrievalRequestId;
        setReturnModalOpen(false);
        setSelectedItem(null);
        resetReturnForm();
        afterItemAction(requestId);
      },
    );
  }, [
    selectedItem,
    returnedCondition,
    returnNotes,
    fullyConsumed,
    afterItemAction,
  ]);

  const getItemLabel = (item) =>
    item?.sampleNumber ||
    item?.bioSampleExternalId ||
    (item?.sampleItemId ? `Item-${item.sampleItemId}` : `Item-${item?.id}`);

  const openRetrieveModal = useCallback((item) => {
    if (!item) return;
    setSelectedItem(item);
    setConditionAtRelease("Good");
    setConditionNotes("");
    setTemperatureAtRetrieval("");
    setQuantityReleased(
      item.quantityRequested != null ? String(item.quantityRequested) : "",
    );
    setRetrieveModalOpen(true);
  }, []);

  const openReleaseModal = useCallback((item) => {
    if (!item) return;
    setSelectedItem(item);
    setReceivedByName(item.receivedByName || "");
    setReleaseModalOpen(true);
  }, []);

  const openReturnModal = useCallback((item) => {
    if (!item) return;
    setSelectedItem(item);
    setReturnedCondition("Good");
    setReturnNotes("");
    setFullyConsumed(false);
    setReturnModalOpen(true);
  }, []);

  const openLifecycle = useCallback((item, referenceItem) => {
    if (!item) return;
    setLifecycleContext({
      sampleItemId: item.sampleItemId,
      bioSampleId: item.bioSampleId,
      sampleLabel:
        item.sampleNumber ||
        item.bioSampleExternalId ||
        item.externalId ||
        item.barcode ||
        (item.sampleItemId ? `Item-${item.sampleItemId}` : ""),
      retrievalContext: referenceItem
        ? {
            requestedReference: formatRequestedReferenceSummary(referenceItem),
            requestedQuantity: referenceItem.quantityRequested,
            requestedUnit: referenceItem.unitOfMeasure,
            fulfilledSample:
              item.externalId ||
              item.barcode ||
              item.sampleNumber ||
              item.bioSampleExternalId,
            storagePath: item.sourceStoragePath || item.storageLocation,
            quantityReleased: item.quantityReleased,
          }
        : null,
    });
    setLifecycleModalOpen(true);
  }, []);

  const getSuggestionReview = useCallback(
    (item) => lookupSuggestion(suggestionsByItemId, item?.id),
    [suggestionsByItemId],
  );

  const isRowSuggestionPending = useCallback(
    (item) =>
      suggestionsLoadState === SUGGESTIONS_LOAD_STATE.LOADING &&
      !lookupSuggestion(suggestionsByItemId, item?.id),
    [suggestionsByItemId, suggestionsLoadState],
  );

  const renderSuggestionIdentityCell = useCallback(
    (item, suggestion, top) => {
      const summary = getSuggestionSummary(suggestion);
      if (top) {
        return summary?.sampleIdentity || formatTopCandidateIdentity(top) || "—";
      }
      if (isRowSuggestionPending(item)) {
        return intl.formatMessage({
          id: "biorepository.retrieval.workbench.searching",
          defaultMessage: "Searching…",
        });
      }
      if (suggestion?.status === SUGGESTION_STATUS.NO_CANDIDATE) {
        return intl.formatMessage({
          id: "biorepository.retrieval.workbench.suggestionState.noCandidate",
          defaultMessage: "No candidate found",
        });
      }
      if (suggestion?.status === SUGGESTION_STATUS.NO_CRITERIA) {
        return intl.formatMessage({
          id: "biorepository.retrieval.workbench.suggestionState.noCriteria",
          defaultMessage: "Add accession, barcode, type, origin, or project",
        });
      }
      return "—";
    },
    [intl, isRowSuggestionPending],
  );

  const renderItemActions = useCallback(
    (item, referenceItem) => {
      if (!item) return null;
      const status = resolveItemStatus(item);
      const enrichedItem = {
        ...item,
        retrievalRequestId: selectedRequest?.id,
      };
      const suggestion = getSuggestionReview(item);
      const topSuggestion = getTopCandidate(suggestion);
      const unresolved = isUnresolvedReferenceItem(item);

      return (
        <div style={{ display: "flex", gap: "0.25rem", flexWrap: "wrap" }}>
          {unresolved && hasUsableTopSuggestion(suggestion) && (
            <Button
              kind="primary"
              size="sm"
              onClick={() => openQuickAttach(item, topSuggestion)}
            >
              <FormattedMessage
                id="biorepository.retrieval.workbench.row.useThisSample"
                defaultMessage="Use this sample"
              />
            </Button>
          )}
          {unresolved && (
            <Button
              kind={hasUsableTopSuggestion(suggestion) ? "tertiary" : "primary"}
              size="sm"
              onClick={() => openReviewAlternatives(item)}
            >
              <FormattedMessage
                id="biorepository.retrieval.workbench.row.reviewAlternatives"
                defaultMessage="Review alternatives"
              />
            </Button>
          )}
          {status === "PENDING" && (
            <Button
              kind="primary"
              size="sm"
              onClick={() => openRetrieveModal(enrichedItem)}
            >
              <FormattedMessage
                id="biorepository.retrieval.retrieve"
                defaultMessage="Retrieve"
              />
            </Button>
          )}
          {status === "RETRIEVED" && (
            <Button
              kind="primary"
              size="sm"
              onClick={() => openReleaseModal(enrichedItem)}
            >
              <FormattedMessage
                id="biorepository.retrieval.release"
                defaultMessage="Release"
              />
            </Button>
          )}
          {status === "IN_ANALYSIS" && item.returnExpected !== false && (
            <Button
              kind="secondary"
              size="sm"
              onClick={() => openReturnModal(enrichedItem)}
            >
              <FormattedMessage
                id="biorepository.retrieval.processReturn"
                defaultMessage="Process Return"
              />
            </Button>
          )}
          <Button
            kind="ghost"
            size="sm"
            onClick={() => openLifecycle(item, referenceItem)}
          >
            <FormattedMessage
              id="biorepository.lifecycle.view"
              defaultMessage="Lifecycle"
            />
          </Button>
        </div>
      );
    },
    [
      selectedRequest?.id,
      getSuggestionReview,
      openQuickAttach,
      openReviewAlternatives,
      openRetrieveModal,
      openReleaseModal,
      openReturnModal,
      openLifecycle,
    ],
  );

  const renderSuggestionTag = useCallback(
    (item) => {
      const suggestion = getSuggestionReview(item);
      if (!suggestion?.status) {
        if (isRowSuggestionPending(item)) {
          return (
            <Tag type="gray" size="sm">
              <FormattedMessage
                id="biorepository.retrieval.workbench.searching"
                defaultMessage="Searching…"
              />
            </Tag>
          );
        }
        return (
          <Tag type="gray" size="sm">
            <FormattedMessage
              id="biorepository.retrieval.workbench.suggestionState.unavailable"
              defaultMessage="Suggestions unavailable"
            />
          </Tag>
        );
      }
      const tagProps = getSuggestionTagProps(suggestion.status);
      return (
        <Tag {...tagProps} size="sm">
          {intl.formatMessage(
            {
              id: getSuggestionStatusMessageId(suggestion.status),
              defaultMessage: getSuggestionStatusDefaultMessage(
                suggestion.status,
                suggestion.results?.length || 0,
              ),
            },
            { count: suggestion.results?.length || 0 },
          )}
        </Tag>
      );
    },
    [getSuggestionReview, isRowSuggestionPending, intl],
  );

  // Load full request details with items
  const loadRequestDetails = useCallback((requestId) => {
    setWorkOrderLoading(true);
    setAttachTargetItem(null);
    setQuickAttachTarget(null);
    setHighlightedItemId(null);
    setAttachSuccessMessage(null);
    getFromOpenElisServer(
      `/rest/biorepository/retrieval/requests/${requestId}`,
      (data) => {
        setWorkOrderLoading(false);
        if (data && !data.error) {
          setSelectedRequest(data);
          setWorkOrderModalOpen(true);
          loadSuggestionReview(data);
        } else {
          setSuggestionsLoading(false);
          setError(data?.error || "Failed to load request details");
        }
      },
    );
  }, [loadSuggestionReview]);

  const completeRequestAfterRelease = useCallback(
    (requestId) =>
      new Promise((resolve) => {
        getFromOpenElisServer(
          `/rest/biorepository/retrieval/requests/${requestId}`,
          (data) => {
            if (!data || data.error) {
              resolve({ error: data?.error || "Failed to load request" });
              return;
            }

            const blockReason = getRequestCompletionBlockReason(data.items || []);
            if (blockReason) {
              resolve({ error: blockReason });
              return;
            }

            postToOpenElisServerJsonResponse(
              `/rest/biorepository/retrieval/requests/${requestId}/complete`,
              "{}",
              (completeData) => {
                if (completeData && completeData.error) {
                  resolve({ error: completeData.error });
                  return;
                }
                if (data.notebookId) {
                  postToOpenElisServerJsonResponse(
                    `/rest/biorepository/retrieval/requests/${requestId}/link-to-notebook`,
                    JSON.stringify({ notebookId: data.notebookId }),
                    () => resolve({ success: true }),
                  );
                } else {
                  resolve({ success: true });
                }
              },
            );
          },
        );
      }),
    [],
  );

  // Handle bulk complete
  const handleBulkComplete = useCallback(() => {
    if (selectedRows.length === 0) return;

    setActionLoading(true);
    let completed = 0;
    let failed = 0;

    selectedRows.forEach((rowId) => {
      const request = activeRequests.find((r) => r.id.toString() === rowId);
      if (!request) return;

      completeRequestAfterRelease(request.id).then((result) => {
        if (result.error) {
          failed++;
        } else {
          completed++;
        }

        if (completed + failed === selectedRows.length) {
          setActionLoading(false);
          setSelectedRows([]);
          loadData();
          if (failed > 0) {
            setError(`Completed ${completed} request(s). ${failed} failed.`);
          }
          if (onActionComplete) onActionComplete();
        }
      });
    });
  }, [
    selectedRows,
    activeRequests,
    loadData,
    completeRequestAfterRelease,
    onActionComplete,
  ]);

  // Get item status tag (used in work order modal)
  const getItemStatusTag = (item) => {
    const display = getItemDisplayStatus(item, intl);
    return (
      <Tag type={display.tagType} size="sm">
        {display.label}
      </Tag>
    );
  };

  // Request table headers
  const requestHeaders = [
    {
      key: "requestNumber",
      header: intl.formatMessage({
        id: "biorepository.retrieval.requestNumber",
        defaultMessage: "Request #",
      }),
    },
    {
      key: "workOrderNumber",
      header: intl.formatMessage({
        id: "biorepository.retrieval.workOrder",
        defaultMessage: "Work Order",
      }),
    },
    {
      key: "requestedBy",
      header: intl.formatMessage({
        id: "biorepository.retrieval.requestedBy",
        defaultMessage: "Requested By",
      }),
    },
    {
      key: "itemCount",
      header: intl.formatMessage({
        id: "biorepository.retrieval.items",
        defaultMessage: "Items",
      }),
    },
    {
      key: "status",
      header: intl.formatMessage({
        id: "biorepository.retrieval.status",
        defaultMessage: "Status",
      }),
    },
    {
      key: "actions",
      header: intl.formatMessage({
        id: "label.actions",
        defaultMessage: "Actions",
      }),
    },
  ];

  if (loading) {
    return (
      <div style={{ padding: "2rem", textAlign: "center" }}>
        <Loading withOverlay={false} />
      </div>
    );
  }

  return (
    <div className="active-retrievals-tab" style={{ padding: "1rem 0" }}>
      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "error.title",
            defaultMessage: "Error",
          })}
          subtitle={error}
          lowContrast
          onCloseButtonClick={() => setError(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Data Table */}
      <DataTable
        rows={paginatedData.map((r) => {
          const displayStatus = getRequestDisplayStatus(r, intl);
          return {
            id: r.id.toString(),
            requestNumber: r.requestNumber || `REQ-${r.id}`,
            workOrderNumber: r.workOrderNumber || "N/A",
            requestedBy: r.requestedByName || "Unknown",
            itemCount: getRequestLineCount(r),
            status: displayStatus.label,
            statusTagType: displayStatus.tagType,
            _raw: r,
          };
        })}
        headers={requestHeaders}
        size="md"
        radio={false}
        isSortable={false}
      >
        {({
          rows,
          headers,
          getTableProps,
          getHeaderProps,
          getRowProps,
          getSelectionProps,
          getBatchActionProps,
          selectedRows,
        }) => {
          const selectedRowIds = selectedRows.map((row) => row.id);
          return (
            <TableContainer>
              <TableToolbar>
                <TableBatchActions {...getBatchActionProps()}>
                  <TableBatchAction
                    renderIcon={Checkmark}
                    iconDescription={intl.formatMessage({
                      id: "biorepository.retrieval.markComplete",
                      defaultMessage: "Complete After Release",
                    })}
                    onClick={() => {
                      handleBulkComplete();
                      setSelectedRows(selectedRowIds);
                    }}
                  >
                    <FormattedMessage
                      id="biorepository.retrieval.markComplete"
                      defaultMessage="Complete After Release"
                    />
                  </TableBatchAction>
                </TableBatchActions>
                <TableToolbarContent>
                  <ExpandableSearch
                    labelText={intl.formatMessage({
                      id: "label.search",
                      defaultMessage: "Search",
                    })}
                    placeholder={intl.formatMessage({
                      id: "biorepository.retrieval.search.active",
                      defaultMessage: "Search...",
                    })}
                    value={searchTerm}
                    onChange={(e) => {
                      setSearchTerm(e.target.value);
                      setPage(1);
                    }}
                  />
                  <Button
                    kind="ghost"
                    size="sm"
                    renderIcon={Renew}
                    iconDescription={intl.formatMessage({
                      id: "label.refresh",
                      defaultMessage: "Refresh",
                    })}
                    hasIconOnly
                    onClick={loadData}
                  />
                </TableToolbarContent>
              </TableToolbar>
              <Table {...getTableProps()}>
                <TableHead>
                  <TableRow>
                    <TableSelectAll {...getSelectionProps()} />
                    {headers.map((header) => (
                      <TableHeader
                        {...getHeaderProps({ header })}
                        key={header.key}
                      >
                        {header.header}
                      </TableHeader>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {rows.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={headers.length + 1}>
                        <div
                          style={{
                            textAlign: "center",
                            padding: "2rem",
                            color: "#525252",
                          }}
                        >
                          <FormattedMessage
                            id="biorepository.retrieval.noActiveRetrievals"
                            defaultMessage="No active retrievals"
                          />
                        </div>
                      </TableCell>
                    </TableRow>
                  ) : (
                    rows.map((row) => {
                      // Get raw data from paginatedData
                      const rawData = paginatedData.find(
                        (r) => r.id.toString() === row.id,
                      );

                      if (!rawData) {
                        console.error("Missing rawData for row:", row.id);
                        console.error(
                          "paginatedData IDs:",
                          paginatedData.map((d) => d.id),
                        );
                        console.error("row:", row);
                        return null;
                      }

                      return (
                        <TableRow {...getRowProps({ row })} key={row.id}>
                          <TableSelectRow {...getSelectionProps({ row })} />
                          {row.cells.map((cell) => (
                            <TableCell key={cell.id}>
                              {cell.info.header === "status" ? (
                                <Tag
                                  type={rawData ? getRequestDisplayStatus(rawData, intl).tagType : "gray"}
                                  size="sm"
                                >
                                  {cell.value}
                                </Tag>
                              ) : cell.info.header === "actions" ? (
                                <div
                                  style={{ display: "flex", gap: "0.25rem" }}
                                >
                                  <Button
                                    kind="ghost"
                                    size="sm"
                                    renderIcon={Catalog}
                                    iconDescription={intl.formatMessage({
                                      id: "biorepository.retrieval.viewWorkOrder",
                                      defaultMessage: "View Work Order",
                                    })}
                                    hasIconOnly
                                    data-testid="view-work-order-button"
                                    onClick={() =>
                                      loadRequestDetails(rawData.id)
                                    }
                                  />
                                  <Button
                                    kind="ghost"
                                    size="sm"
                                    data-testid="view-lifecycle-button"
                                    onClick={() =>
                                      openLifecycle(rawData.items?.[0])
                                    }
                                  >
                                    <FormattedMessage
                                      id="biorepository.lifecycle.view"
                                      defaultMessage="View Lifecycle"
                                    />
                                  </Button>
                                </div>
                              ) : (
                                cell.value
                              )}
                            </TableCell>
                          ))}
                        </TableRow>
                      );
                    })
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          );
        }}
      </DataTable>

      {filteredRequests.length > pageSize && (
        <Pagination
          page={page}
          pageSize={pageSize}
          pageSizes={[10, 20, 50]}
          totalItems={filteredRequests.length}
          onChange={({ page: newPage, pageSize: newPageSize }) => {
            setPage(newPage);
            setPageSize(newPageSize);
          }}
        />
      )}

      {/* Work Order Modal */}
      <Modal
        open={workOrderModalOpen}
        modalHeading={intl.formatMessage({
          id: "biorepository.retrieval.workOrderDetails",
          defaultMessage: "Work Order Details",
        })}
        passiveModal
        onRequestClose={() => {
          setWorkOrderModalOpen(false);
          setSelectedRequest(null);
          setAttachTargetItem(null);
          setQuickAttachTarget(null);
          setHighlightedItemId(null);
          setSuggestionsByItemId({});
          setSuggestionsLoading(false);
          setSuggestionsLoadState(SUGGESTIONS_LOAD_STATE.IDLE);
          setSuggestionsError(null);
          setSuggestionsUsedFallback(false);
        }}
        size="lg"
        className="fulfillment-workbench-modal"
      >
        {workOrderLoading ? (
          <div style={{ padding: "2rem", textAlign: "center" }}>
            <Loading withOverlay={false} small />
          </div>
        ) : (
          selectedRequest && (
            <div style={{ display: "grid", gap: "1rem" }}>
              <div
                style={{
                  display: "grid",
                  gap: "0.5rem",
                  gridTemplateColumns: "1fr 1fr",
                }}
              >
                <div>
                  <strong>
                    <FormattedMessage
                      id="biorepository.retrieval.workOrder"
                      defaultMessage="Work Order"
                    />
                    :
                  </strong>{" "}
                  {selectedRequest.workOrderNumber || "N/A"}
                </div>
                <div>
                  <strong>
                    <FormattedMessage
                      id="biorepository.retrieval.requestNumber"
                      defaultMessage="Request #"
                    />
                    :
                  </strong>{" "}
                  {selectedRequest.requestNumber}
                </div>
                <div>
                  <strong>
                    <FormattedMessage
                      id="biorepository.retrieval.approvedBy"
                      defaultMessage="Approved By"
                    />
                    :
                  </strong>{" "}
                  {selectedRequest.approvedByName || "N/A"}
                </div>
                <div>
                  <strong>
                    <FormattedMessage
                      id="biorepository.retrieval.approvedAt"
                      defaultMessage="Approved At"
                    />
                    :
                  </strong>{" "}
                  {selectedRequest.approvedTimestamp
                    ? new Date(
                        selectedRequest.approvedTimestamp,
                      ).toLocaleString()
                    : "N/A"}
                </div>
              </div>

              {selectedRequest.items && selectedRequest.items.length > 0 && (
                <InlineNotification
                  kind="info"
                  lowContrast
                  hideCloseButton
                  title={intl.formatMessage(
                    {
                      id: "biorepository.retrieval.workOrder.unresolvedBanner",
                      defaultMessage:
                        "{unresolvedCount} of {requestLineCount} rows awaiting sample match",
                    },
                    {
                      unresolvedCount: countUnresolvedReferenceRows(
                        selectedRequest.items,
                      ),
                      requestLineCount: getRequestLineCount(selectedRequest),
                    },
                  )}
                />
              )}

              {attachSuccessMessage && (
                <InlineNotification
                  kind="success"
                  lowContrast
                  onCloseButtonClick={() => setAttachSuccessMessage(null)}
                  title={attachSuccessMessage}
                />
              )}

              {(suggestionsError || suggestionsUsedFallback) && (
                <InlineNotification
                  kind={suggestionsError ? "error" : "warning"}
                  lowContrast
                  hideCloseButton
                  title={suggestionsError || ""}
                  subtitle={
                    suggestionsUsedFallback && !suggestionsError
                      ? intl.formatMessage({
                          id: "biorepository.retrieval.workbench.suggestions.fallback",
                          defaultMessage:
                            "Bulk suggestions unavailable. Showing results from individual sample search.",
                        })
                      : undefined
                  }
                />
              )}

              <FulfillmentReviewBlock
                unresolvedItems={getUnresolvedReferenceItems(selectedRequest)}
                suggestionsByItemId={suggestionsByItemId}
                suggestionsLoadState={suggestionsLoadState}
                suggestionsError={suggestionsError}
                highlightedItemId={
                  highlightedItemId ||
                  attachTargetItem?.id ||
                  quickAttachTarget?.referenceItem?.id ||
                  null
                }
                onUseSample={openQuickAttach}
                onReviewAlternatives={openReviewAlternatives}
                onRetry={() => loadSuggestionReview(selectedRequest)}
                quickAttachTarget={quickAttachTarget}
                quickAttachQuantity={quickAttachQuantity}
                quickAttachError={quickAttachError}
                quickAttachLoading={quickAttachLoading}
                onQuickAttachQuantityChange={setQuickAttachQuantity}
                onQuickAttachConfirm={handleQuickAttachConfirm}
                onQuickAttachCancel={() => {
                  setQuickAttachTarget(null);
                  setQuickAttachError(null);
                }}
              />

              <div>
                <strong>
                  <FormattedMessage
                    id="biorepository.retrieval.workOrder.items"
                    defaultMessage="Request Items"
                  />
                  :
                </strong>
                {selectedRequest.items && selectedRequest.items.length > 0 ? (
                  <Table size="sm" style={{ marginTop: "0.5rem" }}>
                    <TableHead>
                      <TableRow>
                        <TableHeader>
                          <FormattedMessage
                            id="biorepository.retrieval.workOrder.requestedReference"
                            defaultMessage="Requested Reference"
                          />
                        </TableHeader>
                        <TableHeader>
                          <FormattedMessage
                            id="biorepository.retrieval.workOrder.qtyRequested"
                            defaultMessage="Qty Requested"
                          />
                        </TableHeader>
                        <TableHeader>
                          <FormattedMessage
                            id="biorepository.retrieval.workbench.sampleIdentity"
                            defaultMessage="Sample identity"
                          />
                        </TableHeader>
                        <TableHeader>
                          <FormattedMessage
                            id="biorepo.import.field.availableQuantity"
                            defaultMessage="Available"
                          />
                        </TableHeader>
                        <TableHeader>
                          <FormattedMessage
                            id="biorepo.import.field.samplePath"
                            defaultMessage="Sample Path (Storage Location)"
                          />
                        </TableHeader>
                        <TableHeader>
                          <FormattedMessage
                            id="biorepository.retrieval.workbench.matchState"
                            defaultMessage="Match"
                          />
                        </TableHeader>
                        <TableHeader>
                          <FormattedMessage
                            id="biorepository.retrieval.status"
                            defaultMessage="Status"
                          />
                        </TableHeader>
                        <TableHeader>
                          <FormattedMessage
                            id="label.actions"
                            defaultMessage="Actions"
                          />
                        </TableHeader>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {selectedRequest.items.flatMap((item) => {
                        const rows = [
                          { item, isFulfillment: false, referenceItem: null },
                        ];
                        (item.fulfillments || []).forEach((fulfillment) => {
                          rows.push({
                            item: fulfillment,
                            isFulfillment: true,
                            referenceItem: item,
                          });
                        });
                        return rows;
                      }).map(({ item, isFulfillment, referenceItem }, idx) => {
                        const unresolved = !isFulfillment && isUnresolvedReferenceItem(item);
                        const suggestion = unresolved ? getSuggestionReview(item) : null;
                        const top = getTopCandidate(suggestion);
                        const summary = getSuggestionSummary(suggestion);
                        const rowHighlighted =
                          String(highlightedItemId) === String(item.id) ||
                          quickAttachTarget?.referenceItem?.id === item.id ||
                          attachTargetItem?.id === item.id;

                        return (
                        <TableRow
                          key={`${item.id}-${idx}`}
                          style={
                            rowHighlighted
                              ? { backgroundColor: "#edf5ff" }
                              : undefined
                          }
                        >
                          <TableCell>
                            {isFulfillment ? (
                              <span
                                style={{
                                  paddingLeft: "1rem",
                                  color: "#525252",
                                  fontStyle: "italic",
                                }}
                              >
                                <FormattedMessage
                                  id="biorepository.retrieval.workOrder.fulfillsRowAbove"
                                  defaultMessage="(fulfills row above)"
                                />
                              </span>
                            ) : (
                              formatRequestedReferenceSummary(item)
                            )}
                          </TableCell>
                          <TableCell>
                            {!isFulfillment && item.quantityRequested != null
                              ? formatQuantityWithUnit(
                                  item.quantityRequested,
                                  item.unitOfMeasure,
                                )
                              : "—"}
                          </TableCell>
                          <TableCell>
                            {isFulfillment
                              ? item.externalId ||
                                item.accessionNumber ||
                                item.barcode ||
                                item.sampleNumber ||
                                item.bioSampleExternalId ||
                                "—"
                              : item.fulfillments?.length > 0
                                ? item.fulfillments
                                    .map(
                                      (f) =>
                                        f.externalId ||
                                        f.accessionNumber ||
                                        f.barcode ||
                                        f.sampleNumber ||
                                        f.bioSampleExternalId,
                                    )
                                    .filter(Boolean)
                                    .join(", ") || "—"
                                : unresolved
                                  ? renderSuggestionIdentityCell(item, suggestion, top)
                                  : "—"}
                          </TableCell>
                          <TableCell>
                            {isFulfillment
                              ? formatQuantityWithUnit(
                                  item.quantityReleased ?? item.quantityRequested,
                                  item.unitOfMeasure,
                                )
                              : top
                                ? formatQuantityWithUnit(
                                    summary?.availableQuantity ??
                                      top.remainingQuantity ??
                                      top.quantity,
                                    summary?.availableUnitOfMeasure ||
                                      top.unitOfMeasure,
                                  )
                                : unresolved && isRowSuggestionPending(item)
                                  ? intl.formatMessage({
                                      id: "biorepository.retrieval.workbench.searching",
                                      defaultMessage: "Searching…",
                                    })
                                  : unresolved &&
                                      suggestion?.status === SUGGESTION_STATUS.NO_CANDIDATE
                                    ? "—"
                                    : unresolved &&
                                        suggestion?.status === SUGGESTION_STATUS.NO_CRITERIA
                                      ? "—"
                                      : "—"}
                          </TableCell>
                          <TableCell>
                            {isFulfillment
                              ? formatBrf02SamplePath(item) ||
                                item.sourceStoragePath ||
                                item.storageLocation ||
                                "—"
                              : top
                                ? summary?.samplePath ||
                                  formatBrf02SamplePath(top) ||
                                  formatSamplePath(top) ||
                                  "—"
                                : unresolved && isRowSuggestionPending(item)
                                  ? intl.formatMessage({
                                      id: "biorepository.retrieval.workbench.searching",
                                      defaultMessage: "Searching…",
                                    })
                                  : "—"}
                          </TableCell>
                          <TableCell>
                            {unresolved ? (
                              <div>
                                {renderSuggestionTag(item)}
                                {(summary?.matchReason || top?.matchReason) && (
                                  <div
                                    style={{
                                      marginTop: "0.25rem",
                                      fontSize: "0.75rem",
                                      color: "#525252",
                                    }}
                                  >
                                    <FormattedMessage
                                      id={`biorepository.retrieval.attach.matchReason.${summary?.matchReason || top.matchReason}`}
                                      defaultMessage={summary?.matchReason || top.matchReason}
                                    />
                                  </div>
                                )}
                                {suggestion?.noExactMatch && (
                                  <div
                                    style={{
                                      marginTop: "0.25rem",
                                      fontSize: "0.75rem",
                                      color: "#8a3ffc",
                                    }}
                                  >
                                    <FormattedMessage
                                      id="biorepository.retrieval.workbench.suggestionState.noExactMatch"
                                      defaultMessage="No exact match"
                                    />
                                  </div>
                                )}
                                {hasTypeMismatch(suggestion) && (
                                  <div
                                    style={{
                                      marginTop: "0.25rem",
                                      fontSize: "0.75rem",
                                      color: "#a56eff",
                                    }}
                                  >
                                    <FormattedMessage
                                      id="biorepository.retrieval.workbench.typeMismatch.short"
                                      defaultMessage="Type mismatch"
                                    />
                                  </div>
                                )}
                              </div>
                            ) : (
                              "—"
                            )}
                          </TableCell>
                          <TableCell>{getItemStatusTag(item)}</TableCell>
                          <TableCell>
                            {renderItemActions(item, referenceItem)}
                          </TableCell>
                        </TableRow>
                        );
                      })}
                    </TableBody>
                  </Table>
                ) : (
                  <p
                    style={{
                      color: "#525252",
                      fontStyle: "italic",
                      marginTop: "0.5rem",
                    }}
                  >
                    <FormattedMessage
                      id="biorepository.retrieval.workOrder.noItems"
                      defaultMessage="No items available"
                    />
                  </p>
                )}
              </div>

              {attachTargetItem && (
                <AttachSamplePanel
                  referenceItem={attachTargetItem}
                  initialResults={getSuggestionReview(attachTargetItem)?.results || []}
                  suggestionSummary={getSuggestionReview(attachTargetItem)}
                  onAttachSuccess={handleAttachSuccess}
                  onCancel={() => setAttachTargetItem(null)}
                />
              )}
            </div>
          )
        )}
      </Modal>

      {/* Retrieve Modal */}
      <Modal
        open={retrieveModalOpen}
        modalHeading={intl.formatMessage({
          id: "biorepository.retrieval.retrieveSample",
          defaultMessage: "Record Sample Retrieval",
        })}
        primaryButtonText={
          actionLoading
            ? intl.formatMessage({
                id: "label.processing",
                defaultMessage: "Processing...",
              })
            : intl.formatMessage({
                id: "biorepository.retrieval.retrieve",
                defaultMessage: "Retrieve",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setRetrieveModalOpen(false);
          setSelectedItem(null);
          resetRetrieveForm();
        }}
        onRequestSubmit={handleRetrieve}
        primaryButtonDisabled={actionLoading}
      >
        {selectedItem && (
          <div style={{ display: "grid", gap: "1rem" }}>
            <p>
              <FormattedMessage
                id="biorepository.retrieval.retrieve.description"
                defaultMessage="Record physical retrieval of sample {sampleNumber} from storage."
                values={{
                  sampleNumber:
                    selectedItem.sampleNumber ||
                    selectedItem.bioSampleExternalId ||
                    `Item-${selectedItem.id}`,
                }}
              />
            </p>

            {(selectedItem.quantityRequested != null ||
              selectedItem.availableQuantity != null) && (
              <p style={{ color: "#525252", fontSize: "0.875rem" }}>
                Requested:{" "}
                {formatQuantityWithUnit(
                  selectedItem.quantityRequested,
                  selectedItem.unitOfMeasure,
                )}
                {selectedItem.availableQuantity != null &&
                  ` — Available: ${formatQuantityWithUnit(
                    selectedItem.availableQuantity,
                    selectedItem.unitOfMeasure,
                  )}`}
              </p>
            )}

            <NumberInput
              id="quantityReleased"
              label={intl.formatMessage({
                id: "biorepository.retrieval.quantityReleased",
                defaultMessage: "Quantity to release",
              })}
              helperText={intl.formatMessage({
                id: "biorepository.retrieval.quantityReleased.helper",
                defaultMessage: "Defaults to requested quantity if left blank",
              })}
              value={quantityReleased}
              onChange={(e, { value }) => setQuantityReleased(value)}
              min={0}
              step={0.0001}
              allowEmpty
            />

            <Select
              id="conditionAtRelease"
              labelText={intl.formatMessage({
                id: "biorepository.retrieval.conditionAtRelease",
                defaultMessage: "Condition at Retrieval",
              })}
              value={conditionAtRelease}
              onChange={(e) => setConditionAtRelease(e.target.value)}
            >
              <SelectItem value="Good" text="Good" />
              <SelectItem value="Thawed" text="Thawed" />
              <SelectItem value="Partially Thawed" text="Partially Thawed" />
              <SelectItem value="Damaged" text="Damaged" />
              <SelectItem value="Other" text="Other" />
            </Select>

            <TextArea
              id="conditionNotes"
              labelText={intl.formatMessage({
                id: "biorepository.retrieval.conditionNotes",
                defaultMessage: "Condition Notes (Optional)",
              })}
              value={conditionNotes}
              onChange={(e) => setConditionNotes(e.target.value)}
            />

            <NumberInput
              id="temperatureAtRetrieval"
              label={intl.formatMessage({
                id: "biorepository.retrieval.temperature",
                defaultMessage: "Temperature at Retrieval (°C, Optional)",
              })}
              value={temperatureAtRetrieval}
              onChange={(e, { value }) => setTemperatureAtRetrieval(value)}
              min={-200}
              max={50}
              step={0.1}
              allowEmpty
            />
          </div>
        )}
      </Modal>

      {/* Release Modal */}
      <Modal
        open={releaseModalOpen}
        modalHeading={intl.formatMessage({
          id: "biorepository.retrieval.releaseSample",
          defaultMessage: "Release Sample to Requester",
        })}
        primaryButtonText={
          actionLoading
            ? intl.formatMessage({
                id: "label.processing",
                defaultMessage: "Processing...",
              })
            : intl.formatMessage({
                id: "biorepository.retrieval.release",
                defaultMessage: "Release",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setReleaseModalOpen(false);
          setSelectedItem(null);
          setReceivedByName("");
        }}
        onRequestSubmit={handleRelease}
        primaryButtonDisabled={actionLoading}
      >
        {selectedItem && (
          <div style={{ display: "grid", gap: "1rem" }}>
            <p>
              <FormattedMessage
                id="biorepository.retrieval.release.confirmation"
                defaultMessage="Confirm release of sample {sampleNumber} to requester. The sample will be marked as 'In Analysis'."
                values={{
                  sampleNumber:
                    selectedItem.sampleNumber ||
                    selectedItem.bioSampleExternalId ||
                    `Item-${selectedItem.id}`,
                }}
              />
            </p>
            <TextInput
              id="receivedByName"
              labelText={intl.formatMessage({
                id: "biorepository.retrieval.receivedByName",
                defaultMessage: "Received by",
              })}
              value={receivedByName}
              onChange={(e) => setReceivedByName(e.target.value)}
              placeholder={intl.formatMessage({
                id: "biorepository.retrieval.receivedByName.placeholder",
                defaultMessage: "Enter receiver name",
              })}
            />
          </div>
        )}
      </Modal>

      {/* Return Modal */}
      <Modal
        open={returnModalOpen}
        modalHeading={intl.formatMessage({
          id: "biorepository.retrieval.returnSample",
          defaultMessage: "Process Sample Return",
        })}
        primaryButtonText={
          actionLoading
            ? intl.formatMessage({
                id: "label.processing",
                defaultMessage: "Processing...",
              })
            : intl.formatMessage({
                id: "biorepository.retrieval.processReturn",
                defaultMessage: "Process Return",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setReturnModalOpen(false);
          setSelectedItem(null);
          resetReturnForm();
        }}
        onRequestSubmit={handleReturn}
        primaryButtonDisabled={actionLoading}
      >
        {selectedItem && (
          <div style={{ display: "grid", gap: "1rem" }}>
            <p>
              <FormattedMessage
                id="biorepository.retrieval.return.description"
                defaultMessage="Process return of sample {sampleNumber}."
                values={{
                  sampleNumber:
                    selectedItem.sampleNumber ||
                    selectedItem.bioSampleExternalId ||
                    `Item-${selectedItem.id}`,
                }}
              />
            </p>

            <Select
              id="returnedCondition"
              labelText={intl.formatMessage({
                id: "biorepository.retrieval.returnedCondition",
                defaultMessage: "Returned Condition",
              })}
              value={returnedCondition}
              onChange={(e) => setReturnedCondition(e.target.value)}
            >
              <SelectItem value="Good" text="Good" />
              <SelectItem value="Reduced Volume" text="Reduced Volume" />
              <SelectItem value="Thawed" text="Thawed" />
              <SelectItem value="Damaged" text="Damaged" />
              <SelectItem value="Other" text="Other" />
            </Select>

            <TextArea
              id="returnNotes"
              labelText={intl.formatMessage({
                id: "biorepository.retrieval.returnNotes",
                defaultMessage: "Return Notes (Optional)",
              })}
              value={returnNotes}
              onChange={(e) => setReturnNotes(e.target.value)}
            />

            <Checkbox
              id="fullyConsumed"
              labelText={intl.formatMessage({
                id: "biorepository.retrieval.fullyConsumed",
                defaultMessage:
                  "Sample was fully consumed (will not be returned to storage)",
              })}
              checked={fullyConsumed}
              onChange={(_, { checked }) => setFullyConsumed(checked)}
            />
          </div>
        )}
      </Modal>

      <BiorepositoryLifecycleModal
        open={lifecycleModalOpen}
        onClose={() => {
          setLifecycleModalOpen(false);
          setLifecycleContext(null);
        }}
        sampleItemId={lifecycleContext?.sampleItemId}
        bioSampleId={lifecycleContext?.bioSampleId}
        sampleLabel={lifecycleContext?.sampleLabel}
        retrievalContext={lifecycleContext?.retrievalContext}
      />
    </div>
  );
}

ActiveRetrievalsTab.propTypes = {
  onActionComplete: PropTypes.func,
  refreshToken: PropTypes.number,
};

export default ActiveRetrievalsTab;
