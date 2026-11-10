/** The dictionary category that lists why an order or a test is cancelled. */
export const CANCEL_REASON_CATEGORY = "Order cancel reasons";

/** The entry value that opens the free-text reason. */
export const OTHER_REASON = "__other__";

/** The reason to send: the chosen entry, or the free text under Other. */
export const cancelReasonText = (choice, other) =>
  choice === OTHER_REASON ? (other || "").trim() : choice;
