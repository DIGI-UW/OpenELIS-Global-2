import React, { useState, useMemo } from "react";
import {
  Grid,
  Column,
  Section,
  Heading,
  InlineNotification,
  Button,
  Tag,
} from "@carbon/react";
import { Add, Chemistry, CheckboxChecked, Printer } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
import SampleSearch from "./SampleSearch";
import SampleResultsTable from "./SampleResultsTable";
import CreateAliquotModal from "./CreateAliquotModal";
import AddTestsModal from "./AddTestsModal";
import config from "../../config.json";

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

  // Modal state for aliquoting
  const [isAliquotModalOpen, setIsAliquotModalOpen] = useState(false);

  // Modal state for adding tests
  const [isAddTestsModalOpen, setIsAddTestsModalOpen] = useState(false);

  // Get selected sample for aliquoting (single selection only)
  const selectedSample =
    selectedSampleIds.length === 1
      ? searchResponse?.sampleItems?.find(
          (item) => item.id === selectedSampleIds[0],
        )
      : null;

  // Compute aliquot-related statistics for bulk operations
  const aliquotStats = useMemo(() => {
    if (!searchResponse?.sampleItems) {
      return { aliquots: [], parents: [], aliquotCount: 0, parentCount: 0 };
    }

    const aliquots = searchResponse.sampleItems.filter(
      (item) => item.isAliquot,
    );
    const parents = searchResponse.sampleItems.filter(
      (item) => !item.isAliquot,
    );

    const selectedAliquots = selectedSampleIds.filter((id) =>
      aliquots.some((a) => a.id === id),
    );
    const selectedParents = selectedSampleIds.filter((id) =>
      parents.some((p) => p.id === id),
    );

    return {
      aliquots,
      parents,
      aliquotCount: aliquots.length,
      parentCount: parents.length,
      selectedAliquotCount: selectedAliquots.length,
      selectedParentCount: selectedParents.length,
    };
  }, [searchResponse?.sampleItems, selectedSampleIds]);

  /**
   * Select all aliquots in the search results.
   */
  const handleSelectAllAliquots = () => {
    const aliquotIds = aliquotStats.aliquots.map((a) => a.id);
    setSelectedSampleIds(aliquotIds);
  };

  /**
   * Select all parent samples (non-aliquots) in the search results.
   */
  const handleSelectAllParents = () => {
    const parentIds = aliquotStats.parents.map((p) => p.id);
    setSelectedSampleIds(parentIds);
  };

  /**
   * Clear all selections.
   */
  const handleClearSelection = () => {
    setSelectedSampleIds([]);
  };

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

  /**
   * Open aliquot modal for the selected sample.
   */
  const handleOpenAliquotModal = () => {
    setIsAliquotModalOpen(true);
  };

  /**
   * Close aliquot modal.
   */
  const handleCloseAliquotModal = () => {
    setIsAliquotModalOpen(false);
  };

  /**
   * Handle successful aliquot creation.
   */
  const handleAliquotSuccess = (response) => {
    // Refresh search results to show the new aliquot(s)
    if (searchResponse && searchResponse.accessionNumber) {
      handleSearchResults(null, null); // Clear current results
      // Trigger a re-search by calling the search API directly
      // Note: In production, you might want to add a refresh mechanism
      // For now, we'll just show a success message and user can re-search

      // Handle both single and multiple aliquot creation
      const aliquotCount = response.aliquotCount || 1;
      let message;
      if (aliquotCount > 1) {
        // Multiple aliquots created
        const externalIds = response.aliquots
          .map((a) => a.externalId)
          .join(", ");
        message = intl.formatMessage(
          { id: "sample.management.aliquot.successMultiple" },
          { count: aliquotCount, externalIds: externalIds },
        );
      } else {
        // Single aliquot created
        message = intl.formatMessage(
          { id: "sample.management.aliquot.success" },
          { externalId: response.aliquot.externalId },
        );
      }

      setSearchError({
        message: message,
        kind: "success",
      });
    }
  };

  /**
   * Open add tests modal.
   */
  const handleOpenAddTestsModal = () => {
    setIsAddTestsModalOpen(true);
  };

  /**
   * Close add tests modal.
   */
  const handleCloseAddTestsModal = () => {
    setIsAddTestsModalOpen(false);
  };

  /**
   * Handle successful test addition.
   * Shows detailed per-sample/aliquot breakdown in the success message.
   */
  const handleAddTestsSuccess = (response) => {
    // Calculate totals
    const totalSkipped = response.results.reduce(
      (sum, r) => sum + (r.skippedTestIds ? r.skippedTestIds.length : 0),
      0,
    );

    // Check if we have multiple samples with detailed results
    const hasDetailedResults = response.results && response.results.length > 1;

    // Build detailed per-sample breakdown for multiple samples
    let detailedBreakdown = "";
    if (hasDetailedResults) {
      const sampleResults = response.results.map((result) => {
        const addedCount = result.addedTestIds ? result.addedTestIds.length : 0;
        const skippedCount = result.skippedTestIds
          ? result.skippedTestIds.length
          : 0;

        // Use external ID for display, fallback to sample item ID
        const displayId = result.sampleItemExternalId || result.sampleItemId;

        if (skippedCount > 0) {
          return intl.formatMessage(
            { id: "sample.management.addTests.resultItem.withSkipped" },
            { sampleId: displayId, added: addedCount, skipped: skippedCount },
          );
        } else {
          return intl.formatMessage(
            { id: "sample.management.addTests.resultItem" },
            { sampleId: displayId, added: addedCount },
          );
        }
      });

      detailedBreakdown = sampleResults.join("; ");
    }

    // Show success message
    let message;
    if (hasDetailedResults) {
      // Detailed message with per-sample breakdown
      if (totalSkipped > 0) {
        message = intl.formatMessage(
          { id: "sample.management.addTests.successDetailedWithSkipped" },
          {
            added: response.successCount,
            skipped: totalSkipped,
            samples: response.results.length,
            details: detailedBreakdown,
          },
        );
      } else {
        message = intl.formatMessage(
          { id: "sample.management.addTests.successDetailed" },
          {
            count: response.successCount,
            samples: response.results.length,
            details: detailedBreakdown,
          },
        );
      }
    } else {
      // Simple message for single sample
      if (totalSkipped > 0) {
        message = intl.formatMessage(
          { id: "sample.management.addTests.successWithSkipped" },
          { added: response.successCount, skipped: totalSkipped },
        );
      } else {
        message = intl.formatMessage(
          { id: "sample.management.addTests.success" },
          { count: response.successCount },
        );
      }
    }

    setSearchError({
      message: message,
      kind: "success",
    });

    // Clear selection after adding tests
    setSelectedSampleIds([]);
  };

  /**
   * Handle printing barcode for the sample.
   * Uses the accession number from the search response.
   */
  const handlePrintBarCode = () => {
    if (searchResponse && searchResponse.accessionNumber) {
      const barcodesPdf =
        config.serverBaseUrl +
        `/LabelMakerServlet?labNo=${searchResponse.accessionNumber}`;
      window.open(barcodesPdf);
    }
  };

  /**
   * Handle test removal/cancellation from expanded row.
   * Updates local state to remove the test from the sample item.
   */
  const handleTestRemoved = (sampleItemId, analysisId, testName) => {
    // Update local state to remove the cancelled test
    if (searchResponse && searchResponse.sampleItems) {
      const updatedSampleItems = searchResponse.sampleItems.map((item) => {
        if (item.id === sampleItemId) {
          return {
            ...item,
            orderedTests: item.orderedTests.filter(
              (test) => test.analysisId !== analysisId,
            ),
          };
        }
        return item;
      });

      setSearchResponse({
        ...searchResponse,
        sampleItems: updatedSampleItems,
      });
    }

    // Show success notification
    setSearchError({
      message: intl.formatMessage(
        { id: "sample.management.cancelTest.success" },
        { testName: testName },
      ),
      kind: "success",
    });
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

      <div className="orderLegendBody">
        {/* Notification (Error or Success) */}
        {searchError && (
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <InlineNotification
                kind={searchError.kind || "error"}
                title={intl.formatMessage({
                  id:
                    searchError.kind === "success"
                      ? "sample.management.success.title"
                      : "sample.management.error.title",
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
              includeTests={true}
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
                  <div
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                      flexWrap: "wrap",
                      gap: "0.5rem",
                    }}
                  >
                    {/* Left side: Summary info */}
                    <div
                      style={{
                        display: "flex",
                        gap: "1.5rem",
                        flexWrap: "wrap",
                      }}
                    >
                      <span>
                        <strong>
                          <FormattedMessage id="sample.management.results.accessionNumber" />
                          :
                        </strong>{" "}
                        {searchResponse.accessionNumber}
                      </span>
                      <span>
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
                      {aliquotStats.aliquotCount > 0 && (
                        <span>
                          <Tag type="blue" size="sm">
                            {aliquotStats.aliquotCount}{" "}
                            <FormattedMessage id="sample.management.results.aliquots" />
                          </Tag>
                        </span>
                      )}
                      {selectedSampleIds.length > 0 && (
                        <span>
                          <strong>
                            <FormattedMessage id="sample.management.results.selected" />
                            :
                          </strong>{" "}
                          {selectedSampleIds.length}
                          {aliquotStats.selectedAliquotCount > 0 && (
                            <span
                              style={{
                                marginLeft: "0.25rem",
                                color: "#0f62fe",
                              }}
                            >
                              ({aliquotStats.selectedAliquotCount}{" "}
                              <FormattedMessage id="sample.management.results.aliquotsSelected" />
                              )
                            </span>
                          )}
                        </span>
                      )}
                    </div>

                    {/* Right side: Quick selection buttons */}
                    <div style={{ display: "flex", gap: "0.5rem" }}>
                      {aliquotStats.aliquotCount > 0 && (
                        <Button
                          kind="ghost"
                          size="sm"
                          renderIcon={CheckboxChecked}
                          onClick={handleSelectAllAliquots}
                          disabled={
                            aliquotStats.selectedAliquotCount ===
                            aliquotStats.aliquotCount
                          }
                        >
                          <FormattedMessage id="sample.management.action.selectAllAliquots" />
                        </Button>
                      )}
                      {aliquotStats.parentCount > 0 &&
                        aliquotStats.aliquotCount > 0 && (
                          <Button
                            kind="ghost"
                            size="sm"
                            onClick={handleSelectAllParents}
                            disabled={
                              aliquotStats.selectedParentCount ===
                              aliquotStats.parentCount
                            }
                          >
                            <FormattedMessage id="sample.management.action.selectAllParents" />
                          </Button>
                        )}
                      {selectedSampleIds.length > 0 && (
                        <Button
                          kind="ghost"
                          size="sm"
                          onClick={handleClearSelection}
                        >
                          <FormattedMessage id="sample.management.action.clearSelection" />
                        </Button>
                      )}
                    </div>
                  </div>
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

        {/* Action Buttons */}
        {searchResponse &&
          searchResponse.sampleItems &&
          searchResponse.sampleItems.length > 0 &&
          selectedSampleIds.length > 0 && (
            <Grid fullWidth={true}>
              <Column lg={16} md={8} sm={4}>
                <div
                  style={{
                    marginTop: "1rem",
                    marginBottom: "1rem",
                    display: "flex",
                    gap: "1rem",
                  }}
                >
                  {/* Create Aliquot Button (only when single sample selected) */}
                  {selectedSampleIds.length === 1 && selectedSample && (
                    <Button
                      kind="primary"
                      renderIcon={Add}
                      onClick={handleOpenAliquotModal}
                      disabled={!selectedSample.hasRemainingQuantity}
                    >
                      <FormattedMessage
                        id="sample.management.action.createAliquot"
                        defaultMessage="Create Aliquot"
                      />
                    </Button>
                  )}

                  {/* Add Tests Button (available when any samples are selected) */}
                  <Button
                    kind="secondary"
                    renderIcon={Chemistry}
                    onClick={handleOpenAddTestsModal}
                  >
                    <FormattedMessage
                      id="sample.management.addTests.button"
                      defaultMessage="Add Tests"
                    />
                  </Button>

                  {/* Print Barcode Button */}
                  <Button
                    kind="tertiary"
                    renderIcon={Printer}
                    onClick={handlePrintBarCode}
                  >
                    <FormattedMessage id="print.barcode" />
                  </Button>
                </div>
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
                    onSelectionChange={handleSelectionChange}
                    onTestRemoved={handleTestRemoved}
                  />
                </Column>
              </Grid>
            </>
          )}
      </div>

      {/* Create Aliquot Modal */}
      {selectedSample && (
        <CreateAliquotModal
          open={isAliquotModalOpen}
          onClose={handleCloseAliquotModal}
          parentSample={selectedSample}
          onSuccess={handleAliquotSuccess}
        />
      )}

      {/* Add Tests Modal */}
      <AddTestsModal
        open={isAddTestsModalOpen}
        onClose={handleCloseAddTestsModal}
        selectedSampleIds={selectedSampleIds}
        selectedSamples={
          searchResponse?.sampleItems?.filter((item) =>
            selectedSampleIds.includes(item.id),
          ) || []
        }
        onSuccess={handleAddTestsSuccess}
      />
    </>
  );
}
