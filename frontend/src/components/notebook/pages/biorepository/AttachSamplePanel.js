import React, { useState, useCallback, useEffect } from "react";
import {
  Button,
  TextInput,
  NumberInput,
  InlineNotification,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Loading,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import PropTypes from "prop-types";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import { formatQuantityWithUnit } from "./biorepositoryQuantityHelpers";
import { formatBrf02SamplePath } from "./biorepositorySamplePathHelpers";
import { formatRequestedReferenceSummary } from "../common/biorepoRequestReferenceHelpers";
import {
  EMPTY_SAMPLE_SEARCH_FILTERS,
  buildFulfillmentSearchQuery,
  hasActiveSearchFilters,
} from "../common/biorepoSampleSearchHelpers";
import {
  resolveDefaultAttachQuantity,
  validateAttachQuantity,
} from "./biorepoFulfillmentSuggestionHelpers";

export const buildFiltersFromReferenceItem = (referenceItem) => ({
  ...EMPTY_SAMPLE_SEARCH_FILTERS,
  sampleType: referenceItem?.requestedSampleType || "",
  originLab: referenceItem?.requestedOriginLab || "",
  projectId: referenceItem?.requestedProjectId || "",
  accessionNumber: referenceItem?.requestedAccessionNumber || "",
  barcode: referenceItem?.requestedBarcode || "",
});

function AttachSamplePanel({
  referenceItem,
  initialResults,
  suggestionSummary,
  onAttachSuccess,
  onCancel,
}) {
  const intl = useIntl();
  const [filters, setFilters] = useState(() =>
    buildFiltersFromReferenceItem(referenceItem),
  );
  const [searchResults, setSearchResults] = useState([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [attachLoading, setAttachLoading] = useState(false);
  const [attachError, setAttachError] = useState(null);
  const [searchRan, setSearchRan] = useState(false);
  const [attachQuantity, setAttachQuantity] = useState(
    referenceItem?.quantityRequested != null
      ? String(referenceItem.quantityRequested)
      : "",
  );

  useEffect(() => {
    setFilters(buildFiltersFromReferenceItem(referenceItem));
    setSearchResults(Array.isArray(initialResults) ? initialResults : []);
    setSearchRan(Array.isArray(initialResults) && initialResults.length > 0);
    setAttachQuantity(
      referenceItem?.quantityRequested != null
        ? String(referenceItem.quantityRequested)
        : "",
    );
  }, [referenceItem?.id, initialResults]);

  const runSearch = useCallback(() => {
    if (!hasActiveSearchFilters(filters)) {
      return;
    }
    setSearchLoading(true);
    setSearchRan(true);
    const query = buildFulfillmentSearchQuery(filters, { status: "STORED" });
    getFromOpenElisServer(
      `/rest/biorepository/sample/search?${query}&context=fulfillment`,
      (data) => {
        setSearchLoading(false);
        setSearchResults(Array.isArray(data) ? data : []);
      },
    );
  }, [filters]);

  useEffect(() => {
    const prefill = buildFiltersFromReferenceItem(referenceItem);
    if (
      hasActiveSearchFilters(prefill) &&
      (!Array.isArray(initialResults) || initialResults.length === 0)
    ) {
      runSearch();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [referenceItem?.id, initialResults]);

  const updateFilter = (key, value) => {
    setFilters((prev) => ({ ...prev, [key]: value }));
  };

  const handleKeyDown = (event) => {
    if (event.key === "Enter") {
      event.preventDefault();
      runSearch();
    }
  };

  const handleAttach = (bioSample) => {
    if (!referenceItem?.id || !bioSample?.id) return;

    const validation = validateAttachQuantity(
      referenceItem.quantityRequested,
      null,
      attachQuantity,
      { bioSample },
    );
    if (!validation.valid) {
      onAttachSuccess(
        null,
        intl.formatMessage({
          id: validation.errorKey,
          defaultMessage: "Invalid quantity for attach",
        }),
      );
      return;
    }

    setAttachLoading(true);
    setAttachError(null);
    postToOpenElisServerJsonResponse(
      `/rest/biorepository/retrieval/items/${referenceItem.id}/attach`,
      JSON.stringify({
        bioSampleId: bioSample.id,
        quantityRequested: validation.quantity,
      }),
      (data) => {
        setAttachLoading(false);
        if (data && data.error) {
          setAttachError(data.error);
          onAttachSuccess(null, data.error);
          return;
        }
        onAttachSuccess({
          bioSample,
          request: data.request,
          referenceItem: data.referenceItem,
          fulfillmentItem: data.fulfillmentItem,
        });
      },
    );
  };

  const hasExactIdentityInput =
    Boolean(filters.accessionNumber?.trim()) || Boolean(filters.barcode?.trim());
  const hasExactIdentityMatch = searchResults.some(
    (sample) => sample?.exactIdentityMatch,
  );
  const showingFallbackSuggestions =
    searchResults.some((sample) => sample?.fallbackUsed) ||
    Boolean(suggestionSummary?.fallbackUsed);
  const panelNoExactMatch =
    Boolean(suggestionSummary?.noExactMatch) ||
    (hasExactIdentityInput && !hasExactIdentityMatch && searchResults.length > 0);

  return (
    <div
      style={{
        marginTop: "1rem",
        padding: "1rem",
        border: "1px dashed #c6c6c6",
        borderRadius: "4px",
        backgroundColor: "#fafafa",
      }}
      data-testid="attach-sample-panel-secondary"
    >
      {attachError && (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          style={{ marginBottom: "0.75rem" }}
          title={attachError}
        />
      )}
      <p
        style={{
          margin: "0 0 0.5rem",
          fontSize: "0.75rem",
          fontWeight: 600,
          letterSpacing: "0.02em",
          textTransform: "uppercase",
          color: "#525252",
        }}
      >
        <FormattedMessage
          id="biorepository.retrieval.workbench.attach.optionalRefine"
          defaultMessage="Refine search (optional)"
        />
      </p>
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "flex-start",
          marginBottom: "0.75rem",
        }}
      >
        <div>
          <strong>
            <FormattedMessage
              id="biorepository.retrieval.workbench.attach.confirmTitle"
              defaultMessage="Confirm or refine match"
            />
          </strong>
          <p style={{ margin: "0.25rem 0 0", color: "#525252", fontSize: "0.875rem" }}>
            <FormattedMessage
              id="biorepository.retrieval.attach.forReference"
              defaultMessage="Requested:"
            />{" "}
            {formatRequestedReferenceSummary(referenceItem)}
            {referenceItem?.quantityRequested != null &&
              ` — ${formatQuantityWithUnit(
                referenceItem.quantityRequested,
                referenceItem.unitOfMeasure,
              )}`}
          </p>
        </div>
        <Button kind="ghost" size="sm" onClick={onCancel}>
          <FormattedMessage id="label.cancel" defaultMessage="Cancel" />
        </Button>
      </div>

      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(3, minmax(0, 1fr))",
          gap: "0.75rem",
        }}
      >
        <TextInput
          id={`attach-sampleType-${referenceItem?.id}`}
          labelText={intl.formatMessage({
            id: "biorepo.import.field.sampleType",
            defaultMessage: "Sample Type",
          })}
          size="sm"
          value={filters.sampleType}
          onChange={(e) => updateFilter("sampleType", e.target.value)}
          onKeyDown={handleKeyDown}
        />
        <TextInput
          id={`attach-accession-${referenceItem?.id}`}
          labelText={intl.formatMessage({
            id: "biorepo.import.searchModal.accessionLabNumber",
            defaultMessage: "Accession / Sample ID / Barcode",
          })}
          size="sm"
          value={filters.accessionNumber}
          onChange={(e) => updateFilter("accessionNumber", e.target.value)}
          onKeyDown={handleKeyDown}
        />
        <TextInput
          id={`attach-barcode-${referenceItem?.id}`}
          labelText={intl.formatMessage({
            id: "biorepo.import.field.batchNo",
            defaultMessage: "Batch No. / Barcode",
          })}
          size="sm"
          value={filters.barcode}
          onChange={(e) => updateFilter("barcode", e.target.value)}
          onKeyDown={handleKeyDown}
        />
        <TextInput
          id={`attach-originLab-${referenceItem?.id}`}
          labelText={intl.formatMessage({
            id: "biorepo.import.searchModal.originLab",
            defaultMessage: "Origin Lab",
          })}
          size="sm"
          value={filters.originLab}
          onChange={(e) => updateFilter("originLab", e.target.value)}
          onKeyDown={handleKeyDown}
        />
        <TextInput
          id={`attach-project-${referenceItem?.id}`}
          labelText={intl.formatMessage({
            id: "biorepo.import.searchModal.project",
            defaultMessage: "Project",
          })}
          size="sm"
          value={filters.projectId}
          onChange={(e) => updateFilter("projectId", e.target.value)}
          onKeyDown={handleKeyDown}
        />
        <NumberInput
          id={`attach-quantity-${referenceItem?.id}`}
          label={intl.formatMessage({
            id: "biorepository.retrieval.attach.quantityToAttach",
            defaultMessage: "Quantity to attach",
          })}
          hideSteppers={false}
          allowEmpty
          min={0}
          step={0.0001}
          value={attachQuantity}
          onChange={(e, { value }) => setAttachQuantity(value)}
        />
      </div>

      <div style={{ marginTop: "0.75rem", display: "flex", gap: "0.5rem" }}>
        <Button
          kind="secondary"
          size="sm"
          onClick={runSearch}
          disabled={searchLoading || !hasActiveSearchFilters(filters)}
        >
          <FormattedMessage
            id="biorepository.retrieval.searchInventory"
            defaultMessage="Find matching stored sample"
          />
        </Button>
      </div>

      <div style={{ marginTop: "0.75rem" }}>
        <strong>
          {hasExactIdentityMatch ? (
            <FormattedMessage
              id="biorepository.retrieval.attach.exactMatch"
              defaultMessage="Exact match"
            />
          ) : (
            <FormattedMessage
              id="biorepository.retrieval.attach.suggestedMatches"
              defaultMessage="Suggested stored samples"
            />
          )}
        </strong>
        <p style={{ margin: "0.25rem 0 0", color: "#525252", fontSize: "0.875rem" }}>
          {hasExactIdentityMatch ? (
            <FormattedMessage
              id="biorepository.retrieval.attach.exactMatchHelp"
              defaultMessage="An exact accession or barcode match was found for this request line."
            />
          ) : (
            <FormattedMessage
              id="biorepository.retrieval.attach.refineSearch"
              defaultMessage="Refine search if the suggested matches do not show the right stored sample."
            />
          )}
        </p>
      </div>

      {(panelNoExactMatch || showingFallbackSuggestions) &&
        searchResults.length > 0 && (
          <InlineNotification
            kind="warning"
            lowContrast
            hideCloseButton
            style={{ marginTop: "0.75rem" }}
            title={intl.formatMessage({
              id: "biorepository.retrieval.workbench.suggestionState.noExactMatch",
              defaultMessage: "No exact match — broader suggestions to review",
            })}
          />
        )}

      {searchLoading && (
        <div style={{ marginTop: "0.75rem" }}>
          <Loading withOverlay={false} small />
        </div>
      )}

      {!searchLoading && searchRan && searchResults.length === 0 && (
        <p style={{ marginTop: "0.75rem", color: "#525252", fontSize: "0.875rem" }}>
          {hasExactIdentityInput ? (
            <FormattedMessage
              id="biorepository.retrieval.attach.noExactOrFallbackResults"
              defaultMessage="No exact match or broader suggestion was found for this search."
            />
          ) : (
            <FormattedMessage
              id="biorepository.retrieval.attach.noResults"
              defaultMessage="No stored samples matched your search."
            />
          )}
        </p>
      )}

      {searchResults.length > 0 && (
        <Table size="sm" style={{ marginTop: "0.75rem" }}>
          <TableHead>
            <TableRow>
                <TableHeader>
                  <FormattedMessage
                    id="biorepo.import.searchModal.accessionLabNumber"
                    defaultMessage="Accession / Sample ID / Barcode"
                  />
                </TableHeader>
              <TableHeader>
                <FormattedMessage
                  id="biorepo.import.field.batchNo"
                  defaultMessage="Batch No. / Barcode"
                />
              </TableHeader>
              <TableHeader>
                <FormattedMessage
                  id="biorepo.import.field.sampleType"
                  defaultMessage="Sample Type"
                />
              </TableHeader>
              <TableHeader>
                <FormattedMessage
                  id="biorepo.import.searchModal.originLab"
                  defaultMessage="Origin Lab"
                />
              </TableHeader>
              <TableHeader>
                <FormattedMessage
                  id="biorepository.retrieval.attach.matchReason"
                  defaultMessage="Why this match"
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
              <TableHeader />
            </TableRow>
          </TableHead>
          <TableBody>
            {searchResults.map((sample) => (
              <TableRow key={sample.id}>
                <TableCell>{sample.accessionNumber || "-"}</TableCell>
                <TableCell>{sample.barcode || "-"}</TableCell>
                <TableCell>
                  {sample.sampleType?.description || sample.sampleTypeName || "-"}
                </TableCell>
                <TableCell>{sample.originLab || "-"}</TableCell>
                <TableCell>
                  {sample.matchReason ? (
                    <FormattedMessage
                      id={`biorepository.retrieval.attach.matchReason.${sample.matchReason}`}
                      defaultMessage={sample.matchReason}
                    />
                  ) : (
                    "—"
                  )}
                </TableCell>
                <TableCell>
                  {formatQuantityWithUnit(
                    sample.remainingQuantity ?? sample.quantity,
                    sample.unitOfMeasure,
                  )}
                </TableCell>
                <TableCell>{formatBrf02SamplePath(sample) || "-"}</TableCell>
                <TableCell>
                  <Button
                    kind="primary"
                    size="sm"
                    disabled={attachLoading}
                    onClick={() => handleAttach(sample)}
                  >
                    <FormattedMessage
                      id="biorepository.retrieval.attach.action"
                      defaultMessage="Attach"
                    />
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  );
}

AttachSamplePanel.propTypes = {
  referenceItem: PropTypes.shape({
    id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    requestedSampleType: PropTypes.string,
    requestedOriginLab: PropTypes.string,
    requestedProjectId: PropTypes.string,
    requestedAccessionNumber: PropTypes.string,
    requestedBarcode: PropTypes.string,
    quantityRequested: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    unitOfMeasure: PropTypes.string,
  }).isRequired,
  initialResults: PropTypes.arrayOf(PropTypes.shape({
    id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    exactIdentityMatch: PropTypes.bool,
    fallbackUsed: PropTypes.bool,
  })),
  suggestionSummary: PropTypes.shape({
    status: PropTypes.string,
    fallbackUsed: PropTypes.bool,
    noExactMatch: PropTypes.bool,
  }),
  onAttachSuccess: PropTypes.func.isRequired,
  onCancel: PropTypes.func.isRequired,
};

AttachSamplePanel.defaultProps = {
  initialResults: [],
  suggestionSummary: null,
};

export default AttachSamplePanel;
