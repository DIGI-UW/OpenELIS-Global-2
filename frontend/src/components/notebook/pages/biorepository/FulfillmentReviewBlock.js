import React from "react";
import { Button, Tag, InlineLoading, NumberInput, InlineNotification } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import PropTypes from "prop-types";
import { formatRequestedReferenceSummary } from "../common/biorepoRequestReferenceHelpers";
import { formatQuantityWithUnit } from "./biorepositoryQuantityHelpers";
import {
  formatTopCandidateIdentity,
  getSuggestionStatusDefaultMessage,
  getSuggestionStatusMessageId,
  getSuggestionSummary,
  getSuggestionTagProps,
  getTopCandidate,
  hasTypeMismatch,
  hasUsableTopSuggestion,
  lookupSuggestion,
  SUGGESTION_STATUS,
  SUGGESTIONS_LOAD_STATE,
} from "./biorepoFulfillmentSuggestionHelpers";

function FulfillmentReviewBlock({
  unresolvedItems,
  suggestionsByItemId,
  suggestionsLoadState,
  highlightedItemId,
  quickAttachTarget,
  quickAttachQuantity,
  quickAttachError,
  quickAttachLoading,
  onQuickAttachQuantityChange,
  onQuickAttachConfirm,
  onQuickAttachCancel,
  onUseSample,
  onReviewAlternatives,
  onRetry,
}) {
  const intl = useIntl();

  if (!unresolvedItems || unresolvedItems.length === 0) {
    return null;
  }

  return (
    <section
      style={{
        marginTop: "1rem",
        padding: "1rem",
        border: "1px solid #c6c6c6",
        borderRadius: "4px",
        backgroundColor: "#ffffff",
      }}
      data-testid="fulfillment-review-block"
    >
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: "0.75rem",
          flexWrap: "wrap",
          gap: "0.5rem",
        }}
      >
        <div>
          <h4 style={{ margin: 0, fontSize: "1rem", fontWeight: 600 }}>
            <FormattedMessage
              id="biorepository.retrieval.workbench.reviewSuggestions.title"
              defaultMessage="Review suggestions"
            />
          </h4>
          <span style={{ color: "#525252", fontSize: "0.875rem" }}>
            <FormattedMessage
              id="biorepository.retrieval.workbench.reviewSuggestions.unresolvedCount"
              defaultMessage="{count} unresolved"
              values={{ count: unresolvedItems.length }}
            />
          </span>
        </div>
        {onRetry && (
          <Button kind="ghost" size="sm" onClick={onRetry}>
            <FormattedMessage
              id="biorepository.retrieval.workbench.retrySuggestions"
              defaultMessage="Retry suggestions"
            />
          </Button>
        )}
      </div>

      <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}>
        {unresolvedItems.map((item) => {
          const suggestion = lookupSuggestion(suggestionsByItemId, item.id);
          const top = getTopCandidate(suggestion);
          const summary = getSuggestionSummary(suggestion);
          const status = suggestion?.status;
          const rowPending =
            suggestionsLoadState === SUGGESTIONS_LOAD_STATE.LOADING && !suggestion;
          const tagProps = status ? getSuggestionTagProps(status) : { type: "gray" };
          const isHighlighted = String(highlightedItemId) === String(item.id);
          const rowQuickAttach =
            String(quickAttachTarget?.referenceItem?.id) === String(item.id);

          return (
            <div
              key={item.id}
              data-testid={`fulfillment-review-row-${item.id}`}
              style={{
                border: isHighlighted ? "2px solid #0f62fe" : "1px solid #e0e0e0",
                borderRadius: "4px",
                padding: "0.75rem",
                backgroundColor: isHighlighted ? "#edf5ff" : "#f4f4f4",
              }}
            >
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  gap: "1rem",
                  flexWrap: "wrap",
                  alignItems: "flex-start",
                }}
              >
                <div style={{ flex: "1 1 240px" }}>
                  <div style={{ fontWeight: 600, marginBottom: "0.25rem" }}>
                    {formatRequestedReferenceSummary(item)}
                  </div>
                  {item.quantityRequested != null && (
                    <div style={{ fontSize: "0.875rem", color: "#525252" }}>
                      <FormattedMessage
                        id="biorepository.retrieval.workbench.requestedQty"
                        defaultMessage="Requested: {qty}"
                        values={{
                          qty: formatQuantityWithUnit(
                            item.quantityRequested,
                            item.unitOfMeasure,
                          ),
                        }}
                      />
                    </div>
                  )}
                </div>

                <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
                  {rowPending ? (
                    <InlineLoading
                      description={intl.formatMessage({
                        id: "biorepository.retrieval.workbench.searching",
                        defaultMessage: "Searching…",
                      })}
                    />
                  ) : status ? (
                    <Tag {...tagProps} size="md">
                      {intl.formatMessage(
                        {
                          id: getSuggestionStatusMessageId(status),
                          defaultMessage: getSuggestionStatusDefaultMessage(
                            status,
                            suggestion?.results?.length || 0,
                          ),
                        },
                        { count: suggestion?.results?.length || 0 },
                      )}
                    </Tag>
                  ) : (
                    <Tag type="gray" size="md">
                      <FormattedMessage
                        id="biorepository.retrieval.workbench.suggestionState.unavailable"
                        defaultMessage="Suggestions unavailable"
                      />
                    </Tag>
                  )}
                </div>
              </div>

              {status === SUGGESTION_STATUS.NO_CRITERIA && (
                <p style={{ margin: "0.5rem 0 0", color: "#525252", fontSize: "0.875rem" }}>
                  <FormattedMessage
                    id="biorepository.retrieval.workbench.suggestionState.noCriteria"
                    defaultMessage="Add accession, barcode, type, origin, or project to search"
                  />
                </p>
              )}

              {status === SUGGESTION_STATUS.NO_CANDIDATE && (
                <p style={{ margin: "0.5rem 0 0", color: "#525252", fontSize: "0.875rem" }}>
                  <FormattedMessage
                    id="biorepository.retrieval.workbench.noStoredSample"
                    defaultMessage="No stored sample found for this request line."
                  />
                </p>
              )}

              {hasTypeMismatch(suggestion) && (
                <InlineNotification
                  kind="warning"
                  lowContrast
                  hideCloseButton
                  style={{ marginTop: "0.5rem" }}
                  title={intl.formatMessage({
                    id: "biorepository.retrieval.workbench.typeMismatch.title",
                    defaultMessage: "Type mismatch",
                  })}
                  subtitle={intl.formatMessage({
                    id: "biorepository.retrieval.workbench.typeMismatch.body",
                    defaultMessage:
                      "This exact sample identity was found, but its stored sample type differs from the requested reference.",
                  })}
                />
              )}

              <div
                style={{
                  marginTop: "0.75rem",
                  display: "grid",
                  gridTemplateColumns: "minmax(0, 1.3fr) minmax(0, 0.8fr) minmax(0, 1.2fr) minmax(0, 0.9fr)",
                  gap: "0.75rem",
                  alignItems: "start",
                }}
              >
                <div>
                  <div style={{ fontSize: "0.75rem", color: "#525252", marginBottom: "0.2rem" }}>
                    <FormattedMessage
                      id="biorepository.retrieval.workbench.topSuggestion"
                      defaultMessage="Best match"
                    />
                  </div>
                  <div style={{ fontWeight: 500, fontSize: "0.875rem" }}>
                    {rowPending
                      ? intl.formatMessage({
                          id: "biorepository.retrieval.workbench.searching",
                          defaultMessage: "Searching…",
                        })
                      : summary?.sampleIdentity || "—"}
                  </div>
                </div>
                <div>
                  <div style={{ fontSize: "0.75rem", color: "#525252", marginBottom: "0.2rem" }}>
                    <FormattedMessage
                      id="biorepository.retrieval.workbench.availableQty"
                      defaultMessage="Available"
                    />
                  </div>
                  <div style={{ fontWeight: 500, fontSize: "0.875rem" }}>
                    {rowPending
                      ? intl.formatMessage({
                          id: "biorepository.retrieval.workbench.searching",
                          defaultMessage: "Searching…",
                        })
                      : summary?.availableQuantity != null
                        ? formatQuantityWithUnit(
                            summary.availableQuantity,
                            summary.availableUnitOfMeasure,
                          )
                        : "—"}
                  </div>
                </div>
                <div>
                  <div style={{ fontSize: "0.75rem", color: "#525252", marginBottom: "0.2rem" }}>
                    <FormattedMessage
                      id="biorepo.import.field.samplePath"
                      defaultMessage="Sample Path (Storage Location)"
                    />
                  </div>
                  <div style={{ fontWeight: 500, fontSize: "0.875rem", wordBreak: "break-word" }}>
                    {rowPending
                      ? intl.formatMessage({
                          id: "biorepository.retrieval.workbench.searching",
                          defaultMessage: "Searching…",
                        })
                      : summary?.samplePath || "—"}
                  </div>
                </div>
                <div>
                  <div style={{ fontSize: "0.75rem", color: "#525252", marginBottom: "0.2rem" }}>
                    <FormattedMessage
                      id="biorepository.retrieval.workbench.matchState"
                      defaultMessage="Match"
                    />
                  </div>
                  <div style={{ fontWeight: 500, fontSize: "0.875rem" }}>
                    {rowPending ? (
                      intl.formatMessage({
                        id: "biorepository.retrieval.workbench.searching",
                        defaultMessage: "Searching…",
                      })
                    ) : summary?.matchReason ? (
                      <FormattedMessage
                        id={`biorepository.retrieval.attach.matchReason.${summary.matchReason}`}
                        defaultMessage={summary.matchReason}
                      />
                    ) : status === SUGGESTION_STATUS.NO_CANDIDATE ? (
                      <FormattedMessage
                        id="biorepository.retrieval.workbench.suggestionState.noCandidate"
                        defaultMessage="No candidate found"
                      />
                    ) : status === SUGGESTION_STATUS.NO_CRITERIA ? (
                      <FormattedMessage
                        id="biorepository.retrieval.workbench.suggestionState.noCriteria"
                        defaultMessage="Add accession, barcode, type, origin, or project"
                      />
                    ) : (
                      "—"
                    )}
                  </div>
                </div>
              </div>

              {status === SUGGESTION_STATUS.REVIEW_SUGGESTIONS && suggestion?.noExactMatch && (
                <p style={{ margin: "0.35rem 0 0", color: "#8a3ffc", fontSize: "0.8125rem" }}>
                  <FormattedMessage
                    id="biorepository.retrieval.workbench.suggestionState.noExactMatch"
                    defaultMessage="No exact match — broader suggestions to review"
                  />
                </p>
              )}

              <div style={{ marginTop: "0.5rem", display: "flex", gap: "0.5rem", flexWrap: "wrap" }}>
                {hasUsableTopSuggestion(suggestion) && (
                  <Button kind="primary" size="sm" onClick={() => onUseSample(item, top)}>
                    <FormattedMessage
                      id="biorepository.retrieval.workbench.row.useThisSample"
                      defaultMessage="Use this sample"
                    />
                  </Button>
                )}
                {(hasUsableTopSuggestion(suggestion) ||
                  (suggestion?.results?.length || 0) > 0 ||
                  status === SUGGESTION_STATUS.NO_CRITERIA) && (
                  <Button kind="tertiary" size="sm" onClick={() => onReviewAlternatives(item)}>
                    <FormattedMessage
                      id="biorepository.retrieval.workbench.row.reviewAlternatives"
                      defaultMessage="Review alternatives"
                    />
                  </Button>
                )}
              </div>

              {rowQuickAttach && (
                <div
                  style={{
                    marginTop: "0.75rem",
                    padding: "0.75rem",
                    borderRadius: "4px",
                    border: "1px solid #0f62fe",
                    backgroundColor: "#edf5ff",
                  }}
                >
                  <div style={{ fontWeight: 600, marginBottom: "0.5rem" }}>
                    <FormattedMessage
                      id="biorepository.retrieval.workbench.row.confirmAttach"
                      defaultMessage="Confirm attach"
                    />
                  </div>
                  <div style={{ fontSize: "0.875rem", marginBottom: "0.5rem" }}>
                    {summary?.sampleIdentity || formatTopCandidateIdentity(top)}{" "}
                    {summary?.samplePath ? `· ${summary.samplePath}` : ""}
                  </div>
                  {quickAttachError && (
                    <InlineNotification
                      kind="error"
                      lowContrast
                      hideCloseButton
                      title={quickAttachError}
                      style={{ marginBottom: "0.5rem" }}
                    />
                  )}
                  <div style={{ display: "flex", gap: "0.75rem", alignItems: "flex-end", flexWrap: "wrap" }}>
                    <NumberInput
                      id={`quick-attach-quantity-${item.id}`}
                      label={intl.formatMessage({
                        id: "biorepository.retrieval.attach.quantityToAttach",
                        defaultMessage: "Quantity to attach",
                      })}
                      hideSteppers={false}
                      allowEmpty
                      min={0}
                      step={0.0001}
                      value={quickAttachQuantity}
                      onChange={(e, { value }) => onQuickAttachQuantityChange(value)}
                    />
                    <Button
                      kind="primary"
                      size="sm"
                      disabled={quickAttachLoading}
                      onClick={onQuickAttachConfirm}
                    >
                      <FormattedMessage
                        id="biorepository.retrieval.workbench.row.confirmAttach"
                        defaultMessage="Confirm attach"
                      />
                    </Button>
                    <Button kind="ghost" size="sm" onClick={onQuickAttachCancel}>
                      <FormattedMessage id="label.cancel" defaultMessage="Cancel" />
                    </Button>
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </section>
  );
}

FulfillmentReviewBlock.propTypes = {
  unresolvedItems: PropTypes.arrayOf(PropTypes.object).isRequired,
  suggestionsByItemId: PropTypes.object.isRequired,
  suggestionsLoadState: PropTypes.string,
  highlightedItemId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  quickAttachTarget: PropTypes.shape({
    referenceItem: PropTypes.object,
    bioSample: PropTypes.object,
  }),
  quickAttachQuantity: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  quickAttachError: PropTypes.string,
  quickAttachLoading: PropTypes.bool,
  onQuickAttachQuantityChange: PropTypes.func,
  onQuickAttachConfirm: PropTypes.func,
  onQuickAttachCancel: PropTypes.func,
  onUseSample: PropTypes.func.isRequired,
  onReviewAlternatives: PropTypes.func.isRequired,
  onRetry: PropTypes.func,
};

FulfillmentReviewBlock.defaultProps = {
  suggestionsLoadState: SUGGESTIONS_LOAD_STATE.IDLE,
  suggestionsError: null,
  highlightedItemId: null,
  onRetry: null,
};

export default FulfillmentReviewBlock;
