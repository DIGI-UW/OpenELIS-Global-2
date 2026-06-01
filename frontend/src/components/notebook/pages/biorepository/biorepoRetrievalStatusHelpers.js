export const RELEASED_OR_DONE_STATUSES = new Set([
  "IN_ANALYSIS",
  "RETURNED",
  "CONSUMED",
  "PARTIALLY_USED",
]);

export const resolveItemStatus = (item) => item?.itemStatus || item?.status;

export const getRequestLineCount = (request) =>
  request?.requestLineCount ??
  (request?.items
    ? request.items.filter(
        (item) => item.itemRole !== "FULFILLMENT" && !item.fulfillsItemId,
      ).length
    : request?.totalItemCount ?? 0);

export const getRequestDisplayStatus = (request, intl) => {
  const status = request?.status || request?.requestStatus;
  const awaitingFulfillment = Number(request?.awaitingFulfillmentItemCount ?? 0);
  const totalWorkflow = Number(request?.totalItemCount ?? 0);
  const retrieved = Number(request?.retrievedItemCount ?? 0);

  if (status === "DRAFT") {
    return {
      key: "DRAFT",
      label: intl.formatMessage({
        id: "biorepository.retrieval.status.draftNotSubmitted",
        defaultMessage: "Draft – Not Submitted",
      }),
      tagType: "red",
    };
  }

  if (status === "PENDING_APPROVAL") {
    return {
      key: "PENDING_APPROVAL",
      label: intl.formatMessage({
        id: "biorepository.retrieval.status.pendingApproval",
        defaultMessage: "Pending Approval",
      }),
      tagType: "cool-gray",
    };
  }

  if (
    status === "APPROVED" &&
    awaitingFulfillment > 0 &&
    totalWorkflow === 0 &&
    retrieved === 0
  ) {
    return {
      key: "APPROVED_AWAITING_FULFILLMENT",
      label: intl.formatMessage({
        id: "biorepository.retrieval.status.approvedAwaitingFulfillment",
        defaultMessage: "Approved – Awaiting Fulfillment",
      }),
      tagType: "purple",
    };
  }

  if (status === "PARTIALLY_COMPLETED") {
    return {
      key: "PARTIALLY_FULFILLED",
      label: intl.formatMessage({
        id: "biorepository.retrieval.status.partiallyFulfilled",
        defaultMessage: "Partially Fulfilled",
      }),
      tagType: "blue",
    };
  }

  if (status === "IN_PROGRESS") {
    if (awaitingFulfillment > 0) {
      return {
        key: "PARTIALLY_FULFILLED",
        label: intl.formatMessage({
          id: "biorepository.retrieval.status.partiallyFulfilled",
          defaultMessage: "Partially Fulfilled",
        }),
        tagType: "blue",
      };
    }
    return {
      key: "RELEASED",
      label: intl.formatMessage({
        id: "biorepository.retrieval.status.released",
        defaultMessage: "Released",
      }),
      tagType: "blue",
    };
  }

  if (status === "COMPLETED") {
    return {
      key: "COMPLETED",
      label: intl.formatMessage({
        id: "biorepository.retrieval.status.completed",
        defaultMessage: "Completed",
      }),
      tagType: "green",
    };
  }

  if (status === "REJECTED") {
    return {
      key: "REJECTED",
      label: intl.formatMessage({
        id: "biorepository.retrieval.status.rejected",
        defaultMessage: "Rejected",
      }),
      tagType: "red",
    };
  }

  if (status === "CANCELLED") {
    return {
      key: "CANCELLED",
      label: intl.formatMessage({
        id: "biorepository.retrieval.status.cancelled",
        defaultMessage: "Cancelled",
      }),
      tagType: "gray",
    };
  }

  if (status === "APPROVED") {
    return {
      key: "APPROVED",
      label: intl.formatMessage({
        id: "biorepository.retrieval.status.approved",
        defaultMessage: "Approved",
      }),
      tagType: "green",
    };
  }

  return {
    key: status || "UNKNOWN",
    label: status || "-",
    tagType: "gray",
  };
};

export const getDepartmentItemStatusDisplay = (item, requestStatus, intl) => {
  if (requestStatus === "DRAFT") {
    return {
      label: intl.formatMessage({
        id: "biorepository.retrieval.status.draftNotSubmitted",
        defaultMessage: "Draft – Not Submitted",
      }),
      tagType: "red",
    };
  }

  if (requestStatus === "PENDING_APPROVAL") {
    return {
      label: intl.formatMessage({
        id: "biorepository.retrieval.status.pendingApproval",
        defaultMessage: "Pending Approval",
      }),
      tagType: "cool-gray",
    };
  }

  switch (resolveItemStatus(item)) {
    case "AWAITING_FULFILLMENT":
      return {
        label: intl.formatMessage({
          id: "biorepo.import.itemStatus.awaitingFulfillment",
          defaultMessage: "Awaiting Match",
        }),
        tagType: "purple",
      };
    case "PENDING":
      return {
        label: intl.formatMessage({
          id: "biorepo.import.itemStatus.approved",
          defaultMessage: "Approved",
        }),
        tagType: "blue",
      };
    case "RETRIEVED":
      return {
        label: intl.formatMessage({
          id: "biorepo.import.itemStatus.retrieved",
          defaultMessage: "Retrieved",
        }),
        tagType: "teal",
      };
    case "IN_ANALYSIS":
    case "RETURNED":
    case "PARTIALLY_USED":
    case "CONSUMED":
      return {
        label: intl.formatMessage({
          id: "biorepo.import.itemStatus.collected",
          defaultMessage: "Collected",
        }),
        tagType: "green",
      };
    default:
      return { label: resolveItemStatus(item) || "-", tagType: "cool-gray" };
  }
};

export const getItemDisplayStatus = (item, intl) => {
  const status = resolveItemStatus(item);
  const isReference =
    item?.itemRole === "REFERENCE" ||
    (status === "AWAITING_FULFILLMENT" && !item?.fulfillsItemId);

  if (isReference && status === "AWAITING_FULFILLMENT") {
    if (item?.fulfillments?.length > 0) {
      return {
        label: intl.formatMessage({
          id: "biorepository.retrieval.itemStatus.partiallyMatched",
          defaultMessage: "Partially Matched",
        }),
        tagType: "blue",
      };
    }
    return {
      label: intl.formatMessage({
        id: "biorepository.retrieval.itemStatus.awaitingFulfillment",
        defaultMessage: "Awaiting Fulfillment",
      }),
      tagType: "purple",
    };
  }

  switch (status) {
    case "PENDING":
      return {
        label: intl.formatMessage({
          id: "biorepository.retrieval.itemStatus.attachedPendingRetrieval",
          defaultMessage: "Attached – Pending Retrieval",
        }),
        tagType: "gray",
      };
    case "RETRIEVED":
      return {
        label: intl.formatMessage({
          id: "biorepo.import.itemStatus.retrieved",
          defaultMessage: "Retrieved",
        }),
        tagType: "blue",
      };
    case "IN_ANALYSIS":
      return {
        label: intl.formatMessage({
          id: "biorepository.retrieval.status.released",
          defaultMessage: "Released",
        }),
        tagType: "purple",
      };
    case "RETURNED":
      return {
        label: intl.formatMessage({
          id: "biorepository.retrieval.status.returned",
          defaultMessage: "Returned",
        }),
        tagType: "green",
      };
    case "CONSUMED":
      return {
        label: intl.formatMessage({
          id: "biorepository.retrieval.status.consumed",
          defaultMessage: "Consumed",
        }),
        tagType: "teal",
      };
    case "PARTIALLY_USED":
      return {
        label: intl.formatMessage({
          id: "biorepository.retrieval.status.partiallyUsed",
          defaultMessage: "Partially Used",
        }),
        tagType: "teal",
      };
    default:
      return { label: status || "-", tagType: "gray" };
  }
};

export const countUnresolvedReferenceRows = (items) =>
  (items || []).filter(
    (item) =>
      item.itemRole !== "FULFILLMENT" &&
      !item.fulfillsItemId &&
      resolveItemStatus(item) === "AWAITING_FULFILLMENT" &&
      !(item.fulfillments?.length > 0),
  ).length;

export const findNextAttachTarget = (request, currentItemId) => {
  const topLevel = (request?.items || []).filter(
    (item) => item.itemRole !== "FULFILLMENT" && !item.fulfillsItemId,
  );
  const awaiting = topLevel.filter(
    (item) => resolveItemStatus(item) === "AWAITING_FULFILLMENT",
  );
  if (awaiting.length === 0) {
    return null;
  }
  const currentIndex = awaiting.findIndex(
    (item) => String(item.id) === String(currentItemId),
  );
  const nextIndex =
    currentIndex >= 0 ? (currentIndex + 1) % awaiting.length : 0;
  return awaiting[nextIndex];
};

export const isApiErrorResponse = (data) =>
  Boolean(data?.error || (data?.statusCode && data.statusCode >= 400));

export const getApiErrorMessage = (data, fallback) =>
  data?.error || data?.statusText || fallback;

export const collectWorkflowItems = (items) =>
  (items || []).flatMap((item) => {
    if (item.fulfillments?.length) {
      return item.fulfillments;
    }
    if (item.bioSampleId) {
      return [item];
    }
    return [];
  });

export const getRequestCompletionBlockReason = (items) => {
  const topLevelItems = (items || []).filter(
    (item) => item.itemRole !== "FULFILLMENT" && !item.fulfillsItemId,
  );

  const unmatchedReferences = topLevelItems.filter(
    (item) =>
      (item.itemRole === "REFERENCE" || item.status === "AWAITING_FULFILLMENT") &&
      !(item.fulfillments?.length > 0),
  );
  if (unmatchedReferences.length > 0) {
    return "All reference lines must have attached samples before completing the request.";
  }

  const workflowItems = collectWorkflowItems(items);
  if (workflowItems.length === 0) {
    return "No fulfilled samples are ready to complete.";
  }

  const notReleased = workflowItems.filter(
    (item) => !RELEASED_OR_DONE_STATUSES.has(resolveItemStatus(item)),
  );
  if (notReleased.length > 0) {
    return "All samples must be retrieved and released before completing the request.";
  }

  return null;
};
