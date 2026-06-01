import React, { useState, useCallback, useEffect } from "react";
import {
  Button,
  TextInput,
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
  buildSampleSearchQuery,
  hasActiveSearchFilters,
} from "../common/biorepoSampleSearchHelpers";

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
  const [searchRan, setSearchRan] = useState(false);

  useEffect(() => {
    setFilters(buildFiltersFromReferenceItem(referenceItem));
    setSearchResults([]);
    setSearchRan(false);
  }, [referenceItem?.id]);

  const runSearch = useCallback(() => {
    if (!hasActiveSearchFilters(filters)) {
      return;
    }
    setSearchLoading(true);
    setSearchRan(true);
    const query = buildSampleSearchQuery(filters, { status: "STORED" });
    getFromOpenElisServer(
      `/rest/biorepository/sample/search?${query}`,
      (data) => {
        setSearchLoading(false);
        setSearchResults(Array.isArray(data) ? data : []);
      },
    );
  }, [filters]);

  useEffect(() => {
    const prefill = buildFiltersFromReferenceItem(referenceItem);
    if (hasActiveSearchFilters(prefill)) {
      runSearch();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [referenceItem?.id]);

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
    setAttachLoading(true);
    postToOpenElisServerJsonResponse(
      `/rest/biorepository/retrieval/items/${referenceItem.id}/attach`,
      JSON.stringify({
        bioSampleId: bioSample.id,
        quantityRequested: referenceItem.quantityRequested || null,
      }),
      (data) => {
        setAttachLoading(false);
        if (data && data.error) {
          onAttachSuccess(null, data.error);
          return;
        }
        onAttachSuccess(bioSample);
      },
    );
  };

  return (
    <div
      style={{
        marginTop: "1rem",
        padding: "1rem",
        border: "1px solid #e0e0e0",
        borderRadius: "4px",
        backgroundColor: "#f4f4f4",
      }}
    >
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
              id="biorepository.retrieval.attachSample"
              defaultMessage="Attach Sample"
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
          gridTemplateColumns: "repeat(2, 1fr)",
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
            defaultMessage: "Accession / Lab Number",
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
            defaultMessage="Search Inventory"
          />
        </Button>
      </div>

      {searchLoading && (
        <div style={{ marginTop: "0.75rem" }}>
          <Loading withOverlay={false} small />
        </div>
      )}

      {!searchLoading && searchRan && searchResults.length === 0 && (
        <p style={{ marginTop: "0.75rem", color: "#525252", fontSize: "0.875rem" }}>
          <FormattedMessage
            id="biorepository.retrieval.attach.noResults"
            defaultMessage="No stored samples matched your search."
          />
        </p>
      )}

      {searchResults.length > 0 && (
        <Table size="sm" style={{ marginTop: "0.75rem" }}>
          <TableHead>
            <TableRow>
              <TableHeader>
                <FormattedMessage
                  id="biorepo.import.searchModal.accessionLabNumber"
                  defaultMessage="Accession / Lab Number"
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
  onAttachSuccess: PropTypes.func.isRequired,
  onCancel: PropTypes.func.isRequired,
};

export default AttachSamplePanel;
