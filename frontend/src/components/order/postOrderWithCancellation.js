import { postToOpenElisServerFullResponse } from "../utils/Utils";

// The server rolls back the whole step before requesting case-specific consent.
export const postOrderWithCancellation = (
  endpoint,
  body,
  callback,
  askConfirmation,
) => {
  const send = (payload) =>
    postToOpenElisServerFullResponse(
      endpoint,
      JSON.stringify(payload),
      async (response) => {
        if (response?.status === 409) {
          const detail = await response
            .clone()
            .json()
            .catch(() => null);
          if (detail?.code === "MICRO_CASE_CANCELLATION_REQUIRED") {
            const decision = await askConfirmation(detail.cases);
            if (decision) {
              send({
                ...payload,
                microCaseCancellationIds: decision.caseIds,
                microCaseCancellationReason: decision.reason,
              });
              return;
            }
          }
        }
        callback(response);
      },
    );
  send(JSON.parse(body));
};
