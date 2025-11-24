import React, { useState } from "react";
import {
  Grid,
  Column,
  Section,
  Heading,
  InlineNotification,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
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
export default function SampleManagement() {
  const intl = useIntl();

  // Breadcrumb navigation
  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "menu.genericSample" },
    { label: "banner.menu.sampleManagement" },
  ];

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
    <>
      {/* Breadcrumb Navigation */}
      <PageBreadCrumb breadcrumbs={breadcrumbs} />

      {/* Page Header */}
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              <FormattedMessage
                id="sample.management.title"
                defaultMessage="Sample Management"
              />
            </Heading>
          </Section>
        </Column>
      </Grid>

      {/* Error Notification */}
      {searchError && (
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <InlineNotification
              kind="error"
              title={intl.formatMessage({
                id: "sample.management.error.title",
              })}
              subtitle={searchError.message}
              onClose={handleDismissError}
            />
          </Column>
        </Grid>
      )}

      {/* Search Section */}
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              <FormattedMessage
                id="sample.management.search.title"
                defaultMessage="Search Samples"
              />
            </Heading>
          </Section>
        </Column>
      </Grid>

      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <SampleSearch
            onSearchResults={handleSearchResults}
            includeTests={false}
          />
        </Column>
      </Grid>

      {/* Search Results Metadata */}
      {searchResponse &&
        searchResponse.sampleItems &&
        searchResponse.sampleItems.length > 0 && (
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <div
                style={{
                  marginTop: "1rem",
                  marginBottom: "1rem",
                  padding: "0.75rem",
                  backgroundColor: "#f4f4f4",
                  borderRadius: "4px",
                }}
              >
                <strong>
                  <FormattedMessage id="sample.management.results.accessionNumber" />
                  :
                </strong>{" "}
                {searchResponse.accessionNumber}
                <span style={{ marginLeft: "2rem" }}>
                  <strong>
                    <FormattedMessage id="sample.management.results.totalCount" />
                    :
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
                      <FormattedMessage id="sample.management.results.selected" />
                      :
                    </strong>{" "}
                    {selectedSampleIds.length}
                  </span>
                )}
              </div>
            </Column>
          </Grid>
        )}

      {/* Empty State (when search has been performed but no results) */}
      {searchResponse &&
        searchResponse.sampleItems &&
        searchResponse.sampleItems.length === 0 && (
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
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
              />
            </Column>
          </Grid>
        )}

      {/* Results Table Section */}
      {searchResponse &&
        searchResponse.sampleItems &&
        searchResponse.sampleItems.length > 0 && (
          <>
            <Grid fullWidth={true}>
              <Column lg={16} md={8} sm={4}>
                <Section>
                  <Heading>
                    <FormattedMessage
                      id="sample.management.results.title"
                      defaultMessage="Sample Items"
                    />
                  </Heading>
                </Section>
              </Column>
            </Grid>

            <Grid fullWidth={true}>
              <Column lg={16} md={8} sm={4}>
                <SampleResultsTable
                  sampleItems={searchResponse.sampleItems}
                  selectedRows={selectedSampleIds}
                  onSelectionChange={handleSelectionChange}
                />
              </Column>
            </Grid>
          </>
        )}

      {/* Future: Action buttons for selected samples will go here */}
      {/* This will be implemented in Phase 4-6 for aliquoting and test management */}
    </>
  );
}
