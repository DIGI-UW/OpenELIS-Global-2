import {
  findNextAttachTarget,
  getItemDisplayStatus,
  getRequestCompletionBlockReason,
  getRequestDisplayStatus,
  getRequestLineCount,
  RELEASED_OR_DONE_STATUSES,
} from "./biorepoRetrievalStatusHelpers";

const intl = {
  formatMessage: ({ defaultMessage }) => defaultMessage,
};

describe("biorepoRetrievalStatusHelpers", () => {
  test("getItemDisplayStatus maps fulfillment PENDING as attached pending retrieval", () => {
    const display = getItemDisplayStatus(
      { id: 1, itemRole: "FULFILLMENT", status: "PENDING" },
      intl,
    );
    expect(display.label).toBe("Attached – Pending Retrieval");
  });

  test("getItemDisplayStatus maps reference awaiting fulfillment", () => {
    const display = getItemDisplayStatus(
      {
        id: 2,
        itemRole: "REFERENCE",
        status: "AWAITING_FULFILLMENT",
        fulfillments: [],
      },
      intl,
    );
    expect(display.label).toBe("Awaiting Fulfillment");
  });

  test("getRequestLineCount prefers requestLineCount over totalItemCount", () => {
    expect(getRequestLineCount({ requestLineCount: 3, totalItemCount: 0 })).toBe(3);
  });

  test("getRequestDisplayStatus maps approved awaiting fulfillment", () => {
    const display = getRequestDisplayStatus(
      {
        status: "APPROVED",
        awaitingFulfillmentItemCount: 2,
        totalItemCount: 0,
        retrievedItemCount: 0,
      },
      intl,
    );
    expect(display.label).toBe("Approved – Awaiting Fulfillment");
    expect(display.tagType).toBe("purple");
  });

  test("getRequestDisplayStatus distinguishes draft from pending approval", () => {
    expect(getRequestDisplayStatus({ status: "DRAFT" }, intl).label).toBe(
      "Draft – Not Submitted",
    );
    expect(getRequestDisplayStatus({ status: "PENDING_APPROVAL" }, intl).label).toBe(
      "Pending Approval",
    );
  });

  test("getRequestCompletionBlockReason blocks unmatched reference lines", () => {
    const reason = getRequestCompletionBlockReason([
      {
        itemRole: "REFERENCE",
        status: "AWAITING_FULFILLMENT",
        fulfillments: [],
      },
    ]);
    expect(reason).toMatch(/attached samples/i);
  });

  test("getRequestCompletionBlockReason allows released fulfillments", () => {
    const reason = getRequestCompletionBlockReason([
      {
        itemRole: "REFERENCE",
        status: "AWAITING_FULFILLMENT",
        fulfillments: [{ status: "IN_ANALYSIS", bioSampleId: 10 }],
      },
    ]);
    expect(reason).toBeNull();
  });

  test("RELEASED_OR_DONE_STATUSES includes release outcomes", () => {
    expect(RELEASED_OR_DONE_STATUSES.has("IN_ANALYSIS")).toBe(true);
    expect(RELEASED_OR_DONE_STATUSES.has("PENDING")).toBe(false);
  });

  test("findNextAttachTarget returns remaining awaiting fulfillment row", () => {
    const onlyAwaiting = { id: 10, status: "AWAITING_FULFILLMENT", itemRole: "REFERENCE" };
    const request = { items: [onlyAwaiting] };
    expect(findNextAttachTarget(request, 10)).toEqual(onlyAwaiting);
  });
});
