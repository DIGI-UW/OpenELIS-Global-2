import React, { useState } from "react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import { InlineNotification, Breadcrumb, BreadcrumbItem } from "@carbon/react";
import SampleSearch from "./SampleSearch";
import SampleResultsTable from "./SampleResultsTable";

/**
 * SampleManagement - Main container component for Sample Management feature.
 *
 * Features:
 * - Integrates SampleSearch and SampleResultsTable components
 * - Manages search results state
 * - Handles API errors with inline notifications
 * - Provides breadcrumb navigation
 * - Displays search metadata (accession number, result count)
 *
 * This component serves as the entry point for User Story 1: Search for sample
 * items by accession number and view results with hierarchy information.
 *
 * Related: Feature 001-sample-management, User Story 1, Task T035
 */
function SampleManagement() {
  const intl = useIntl();

  // Search results state
  const [searchResponse, setSearchResponse] = useState(null);
  const [searchError, setSearchError] = useState(null);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);

  /**
   * Handle search results callback from SampleSearch component.
   *
   * @param {Object} response - SearchSamplesResponse from backend
   * @param {Object} error - Error object if search failed
   */
  const handleSearchResults = (response, error) => {
    setSearchResponse(response);
    setSearchError(error);

    // Clear selection when new search results arrive
    setSelectedSampleIds([]);
  };

  /**
   * Handle row selection changes from SampleResultsTable component.
   *
   * @param {Array<string>} selectedIds - Array of selected sample item IDs
   */
  const handleSelectionChange = (selectedIds) => {
    setSelectedSampleIds(selectedIds);
  };

  /**
   * Clear error notification.
   */
  const handleDismissError = () => {
    setSearchError(null);
  };

  return (
    <div className="sample-management-container">
      {/* Breadcrumb Navigation */}
      <Breadcrumb>
        <BreadcrumbItem href="/">
          <FormattedMessage id="home.label" />
        </BreadcrumbItem>
        <BreadcrumbItem isCurrentPage>
          <FormattedMessage id="sample.management.breadcrumb.title" />
        </BreadcrumbItem>
      </Breadcrumb>

      {/* Page Header */}
      <div style={{ marginTop: "1rem", marginBottom: "2rem" }}>
        <h1>
          <FormattedMessage id="sample.management.title" />
        </h1>
        <p style={{ color: "#6f6f6f", marginTop: "0.5rem" }}>
          <FormattedMessage id="sample.management.description" />
        </p>
      </div>

      {/* Error Notification */}
      {searchError && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "sample.management.error.title",
          })}
          subtitle={searchError.message}
          onClose={handleDismissError}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Search Component */}
      <div style={{ marginBottom: "2rem" }}>
        <SampleSearch
          onSearchResults={handleSearchResults}
          includeTests={false}
        />
      </div>

      {/* Search Results Metadata */}
      {searchResponse && searchResponse.sampleItems.length > 0 && (
        <div
          style={{
            marginBottom: "1rem",
            padding: "0.75rem",
            backgroundColor: "#f4f4f4",
            borderRadius: "4px",
          }}
        >
          <strong>
            <FormattedMessage id="sample.management.results.accessionNumber" />:
          </strong>{" "}
          {searchResponse.accessionNumber}
          <span style={{ marginLeft: "2rem" }}>
            <strong>
              <FormattedMessage id="sample.management.results.totalCount" />:
            </strong>{" "}
            {searchResponse.totalCount}{" "}
            {searchResponse.totalCount === 1 ? (
              <FormattedMessage id="sample.management.results.item" />
            ) : (
              <FormattedMessage id="sample.management.results.items" />
            )}
          </span>
          {selectedSampleIds.length > 0 && (
            <span style={{ marginLeft: "2rem" }}>
              <strong>
                <FormattedMessage id="sample.management.results.selected" />:
              </strong>{" "}
              {selectedSampleIds.length}
            </span>
          )}
        </div>
      )}

      {/* Empty State (when search has been performed but no results) */}
      {searchResponse && searchResponse.sampleItems.length === 0 && (
        <InlineNotification
          kind="info"
          title={intl.formatMessage({
            id: "sample.management.noResults.title",
          })}
          subtitle={intl.formatMessage(
            { id: "sample.management.noResults.subtitle" },
            { accessionNumber: searchResponse.accessionNumber },
          )}
          hideCloseButton
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Results Table */}
      {searchResponse && searchResponse.sampleItems.length > 0 && (
        <div style={{ marginBottom: "2rem" }}>
          <SampleResultsTable
            sampleItems={searchResponse.sampleItems}
            selectedRows={selectedSampleIds}
            onSelectionChange={handleSelectionChange}
          />
        </div>
      )}

      {/* Future: Action buttons for selected samples will go here */}
      {/* This will be implemented in Phase 4-6 for aliquoting and test management */}
    </div>
  );
}

export default injectIntl(SampleManagement);
