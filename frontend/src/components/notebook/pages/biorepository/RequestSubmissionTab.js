import React, { useState, useCallback, useEffect } from "react";
import {
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableSelectAll,
  TableSelectRow,
  TableContainer,
  TableToolbar,
  TableToolbarContent,
  TableToolbarSearch,
  Button,
  Form,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  DatePicker,
  DatePickerInput,
  InlineNotification,
  Tag,
  Loading,
  Modal,
} from "@carbon/react";
import { Add, Search, SendAlt, TrashCan } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import PropTypes from "prop-types";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";

/**
 * RequestSubmissionTab - Create new sample retrieval requests
 *
 * Workflow:
 * 1. Search and select samples from available inventory
 * 2. Fill in request details (purpose, destination, priority)
 * 3. Submit for supervisor approval
 */
function RequestSubmissionTab({ onRequestCreated }) {
  const intl = useIntl();

  // Form state
  const [requestPurpose, setRequestPurpose] = useState("");
  const [destinationType, setDestinationType] = useState("INTERNAL_LAB");
  const [destinationDetails, setDestinationDetails] = useState("");
  const [priorityLevel, setPriorityLevel] = useState("NORMAL");
  const [ethicsApprovalRef, setEthicsApprovalRef] = useState("");
  const [requiredByDate, setRequiredByDate] = useState(null);
  const [projectId, setProjectId] = useState("");

  // Selected samples state
  const [selectedSamples, setSelectedSamples] = useState([]);

  // Sample search state
  const [searchModalOpen, setSearchModalOpen] = useState(false);
  const [searchType, setSearchType] = useState("barcode"); // barcode, originLab, projectId
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [searchSelectedRows, setSearchSelectedRows] = useState([]);

  // Projects for dropdown
  const [projects, setProjects] = useState([]);

  // Submission state
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState(null);
  const [submitSuccess, setSubmitSuccess] = useState(false);

  // Load available projects
  useEffect(() => {
    getFromOpenElisServer("/rest/projects", (data) => {
      if (data && Array.isArray(data)) {
        setProjects(data);
      }
    });
  }, []);

  // Search for available samples
  const handleSearchSamples = useCallback(() => {
    if (!searchQuery.trim()) return;

    setSearchLoading(true);
    // Build search URL based on selected search type
    const searchParam = `${searchType}=${encodeURIComponent(searchQuery.trim())}`;
    getFromOpenElisServer(
      `/rest/biorepository/sample/search?${searchParam}&status=STORED`,
      (data) => {
        setSearchLoading(false);
        if (data && Array.isArray(data)) {
          // Filter out already selected samples
          const selectedIds = selectedSamples.map((s) => s.id);
          const filtered = data.filter((s) => !selectedIds.includes(s.id));
          setSearchResults(filtered);
        } else {
          setSearchResults([]);
        }
      },
    );
  }, [searchQuery, searchType, selectedSamples]);

  // Add selected samples from search modal
  const handleAddSelectedSamples = useCallback(() => {
    const samplesToAdd = searchResults.filter((s) =>
      searchSelectedRows.includes(s.id.toString()),
    );
    setSelectedSamples((prev) => [...prev, ...samplesToAdd]);
    setSearchSelectedRows([]);
    setSearchResults([]);
    setSearchQuery("");
    setSearchType("barcode");
    setSearchModalOpen(false);
  }, [searchResults, searchSelectedRows]);

  // Remove sample from selection
  const handleRemoveSample = useCallback((sampleId) => {
    setSelectedSamples((prev) => prev.filter((s) => s.id !== sampleId));
  }, []);

  // Submit the request
  const handleSubmit = useCallback(
    (e) => {
      e.preventDefault();

      if (selectedSamples.length === 0) {
        setSubmitError(
          intl.formatMessage({
            id: "biorepository.retrieval.error.noSamples",
            defaultMessage: "Please select at least one sample",
          }),
        );
        return;
      }

      if (!requestPurpose.trim()) {
        setSubmitError(
          intl.formatMessage({
            id: "biorepository.retrieval.error.noPurpose",
            defaultMessage: "Please provide a request purpose",
          }),
        );
        return;
      }

      setSubmitting(true);
      setSubmitError(null);

      const requestBody = {
        requestPurpose,
        bioSampleIds: selectedSamples.map((s) => s.id),
        projectId: projectId || null,
        ethicsApprovalRef: ethicsApprovalRef || null,
        destinationType,
        destinationDetails: destinationDetails || null,
        priorityLevel,
        requiredByDate: requiredByDate || null,
      };

      postToOpenElisServerJsonResponse(
        "/rest/biorepository/retrieval/requests",
        JSON.stringify(requestBody),
        (data) => {
          setSubmitting(false);

          if (data && data.error) {
            setSubmitError(data.error);
            return;
          }

          if (data && data.id) {
            // Submit for approval immediately after creation
            postToOpenElisServerJsonResponse(
              `/rest/biorepository/retrieval/requests/${data.id}/submit`,
              "{}",
              (submitData) => {
                if (submitData && submitData.error) {
                  setSubmitError(submitData.error);
                  return;
                }

                setSubmitSuccess(true);
                // Reset form
                setRequestPurpose("");
                setDestinationType("INTERNAL_LAB");
                setDestinationDetails("");
                setPriorityLevel("NORMAL");
                setEthicsApprovalRef("");
                setRequiredByDate(null);
                setProjectId("");
                setSelectedSamples([]);

                if (onRequestCreated) {
                  onRequestCreated(submitData);
                }
              },
            );
          }
        },
      );
    },
    [
      selectedSamples,
      requestPurpose,
      projectId,
      ethicsApprovalRef,
      destinationType,
      destinationDetails,
      priorityLevel,
      requiredByDate,
      intl,
      onRequestCreated,
    ],
  );

  // Selected samples table headers
  const selectedHeaders = [
    {
      key: "sampleNumber",
      header: intl.formatMessage({
        id: "biorepository.sample.number",
        defaultMessage: "Sample Number",
      }),
    },
    {
      key: "sampleType",
      header: intl.formatMessage({
        id: "biorepository.sample.type",
        defaultMessage: "Sample Type",
      }),
    },
    {
      key: "storageLocation",
      header: intl.formatMessage({
        id: "biorepository.sample.storageLocation",
        defaultMessage: "Storage Location",
      }),
    },
    {
      key: "actions",
      header: intl.formatMessage({
        id: "label.actions",
        defaultMessage: "Actions",
      }),
    },
  ];

  // Search results table headers
  const searchHeaders = [
    {
      key: "sampleNumber",
      header: intl.formatMessage({
        id: "biorepository.sample.number",
        defaultMessage: "Sample Number",
      }),
    },
    {
      key: "sampleType",
      header: intl.formatMessage({
        id: "biorepository.sample.type",
        defaultMessage: "Sample Type",
      }),
    },
    {
      key: "collectionDate",
      header: intl.formatMessage({
        id: "biorepository.sample.collectionDate",
        defaultMessage: "Collection Date",
      }),
    },
    {
      key: "storageLocation",
      header: intl.formatMessage({
        id: "biorepository.sample.storageLocation",
        defaultMessage: "Storage Location",
      }),
    },
  ];

  return (
    <div className="request-submission-tab" style={{ padding: "1rem 0" }}>
      {submitSuccess && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({
            id: "biorepository.retrieval.success.title",
            defaultMessage: "Request Submitted",
          })}
          subtitle={intl.formatMessage({
            id: "biorepository.retrieval.success.description",
            defaultMessage:
              "Your retrieval request has been submitted for approval.",
          })}
          lowContrast
          onCloseButtonClick={() => setSubmitSuccess(false)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {submitError && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "biorepository.retrieval.error.title",
            defaultMessage: "Submission Error",
          })}
          subtitle={submitError}
          lowContrast
          onCloseButtonClick={() => setSubmitError(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      <Form onSubmit={handleSubmit}>
        {/* Section 1: Selected Samples */}
        <div style={{ marginBottom: "2rem" }}>
          <h4 style={{ marginBottom: "1rem" }}>
            <FormattedMessage
              id="biorepository.retrieval.selectedSamples"
              defaultMessage="Selected Samples"
            />
            {selectedSamples.length > 0 && (
              <Tag type="blue" size="sm" style={{ marginLeft: "0.5rem" }}>
                {selectedSamples.length}
              </Tag>
            )}
          </h4>

          <Button
            kind="tertiary"
            size="sm"
            renderIcon={Add}
            onClick={() => setSearchModalOpen(true)}
            style={{ marginBottom: "1rem" }}
          >
            <FormattedMessage
              id="biorepository.retrieval.addSamples"
              defaultMessage="Add Samples"
            />
          </Button>

          {selectedSamples.length > 0 ? (
            <DataTable
              rows={selectedSamples.map((s) => ({
                id: (s.id || s.sampleItemId).toString(),
                sampleNumber:
                  s.barcode ||
                  s.sampleNumber ||
                  `BIO-${s.id || s.sampleItemId}`,
                sampleType:
                  s.sampleType?.description || s.sampleTypeName || "N/A",
                storageLocation:
                  s.storageLocationName || s.storageLocation || "N/A",
              }))}
              headers={selectedHeaders}
              size="sm"
            >
              {({
                rows,
                headers,
                getTableProps,
                getHeaderProps,
                getRowProps,
              }) => (
                <TableContainer>
                  <Table {...getTableProps()}>
                    <TableHead>
                      <TableRow>
                        {headers.map((header) => (
                          <TableHeader
                            {...getHeaderProps({ header })}
                            key={header.key}
                          >
                            {header.header}
                          </TableHeader>
                        ))}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {rows.map((row) => (
                        <TableRow {...getRowProps({ row })} key={row.id}>
                          {row.cells.map((cell) => (
                            <TableCell key={cell.id}>
                              {cell.info.header === "actions" ? (
                                <Button
                                  kind="ghost"
                                  size="sm"
                                  renderIcon={TrashCan}
                                  iconDescription={intl.formatMessage({
                                    id: "label.remove",
                                    defaultMessage: "Remove",
                                  })}
                                  hasIconOnly
                                  onClick={() =>
                                    handleRemoveSample(parseInt(row.id))
                                  }
                                />
                              ) : (
                                cell.value
                              )}
                            </TableCell>
                          ))}
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </DataTable>
          ) : (
            <p style={{ color: "#525252", fontStyle: "italic" }}>
              <FormattedMessage
                id="biorepository.retrieval.noSamplesSelected"
                defaultMessage="No samples selected. Click 'Add Samples' to search and select samples."
              />
            </p>
          )}
        </div>

        {/* Section 2: Request Details */}
        <div style={{ marginBottom: "2rem" }}>
          <h4 style={{ marginBottom: "1rem" }}>
            <FormattedMessage
              id="biorepository.retrieval.requestDetails"
              defaultMessage="Request Details"
            />
          </h4>

          <div
            style={{
              display: "grid",
              gap: "1rem",
              gridTemplateColumns: "1fr 1fr",
            }}
          >
            <TextArea
              id="requestPurpose"
              labelText={intl.formatMessage({
                id: "biorepository.retrieval.purpose",
                defaultMessage: "Request Purpose",
              })}
              placeholder={intl.formatMessage({
                id: "biorepository.retrieval.purpose.placeholder",
                defaultMessage:
                  "Describe the purpose of this sample retrieval...",
              })}
              value={requestPurpose}
              onChange={(e) => setRequestPurpose(e.target.value)}
              required
              style={{ gridColumn: "1 / -1" }}
            />

            <Select
              id="destinationType"
              labelText={intl.formatMessage({
                id: "biorepository.retrieval.destinationType",
                defaultMessage: "Destination Type",
              })}
              value={destinationType}
              onChange={(e) => setDestinationType(e.target.value)}
            >
              <SelectItem
                value="INTERNAL_LAB"
                text={intl.formatMessage({
                  id: "biorepository.retrieval.destination.internal",
                  defaultMessage: "Internal Laboratory",
                })}
              />
              <SelectItem
                value="EXTERNAL_LAB"
                text={intl.formatMessage({
                  id: "biorepository.retrieval.destination.external",
                  defaultMessage: "External Laboratory",
                })}
              />
              <SelectItem
                value="ANALYSIS_RETURN"
                text={intl.formatMessage({
                  id: "biorepository.retrieval.destination.analysisReturn",
                  defaultMessage: "Analysis & Return",
                })}
              />
            </Select>

            <Select
              id="priorityLevel"
              labelText={intl.formatMessage({
                id: "biorepository.retrieval.priority",
                defaultMessage: "Priority Level",
              })}
              value={priorityLevel}
              onChange={(e) => setPriorityLevel(e.target.value)}
            >
              <SelectItem
                value="NORMAL"
                text={intl.formatMessage({
                  id: "biorepository.retrieval.priority.normal",
                  defaultMessage: "Normal",
                })}
              />
              <SelectItem
                value="URGENT"
                text={intl.formatMessage({
                  id: "biorepository.retrieval.priority.urgent",
                  defaultMessage: "Urgent",
                })}
              />
              <SelectItem
                value="CRITICAL"
                text={intl.formatMessage({
                  id: "biorepository.retrieval.priority.critical",
                  defaultMessage: "Critical",
                })}
              />
            </Select>

            <TextInput
              id="destinationDetails"
              labelText={intl.formatMessage({
                id: "biorepository.retrieval.destinationDetails",
                defaultMessage: "Destination Details",
              })}
              placeholder={intl.formatMessage({
                id: "biorepository.retrieval.destinationDetails.placeholder",
                defaultMessage: "Lab name, building, room number...",
              })}
              value={destinationDetails}
              onChange={(e) => setDestinationDetails(e.target.value)}
            />

            <Select
              id="projectId"
              labelText={intl.formatMessage({
                id: "biorepository.retrieval.project",
                defaultMessage: "Project (Optional)",
              })}
              value={projectId}
              onChange={(e) => setProjectId(e.target.value)}
            >
              <SelectItem
                value=""
                text={intl.formatMessage({
                  id: "label.select",
                  defaultMessage: "Select...",
                })}
              />
              {projects.map((project) => (
                <SelectItem
                  key={project.id}
                  value={project.id.toString()}
                  text={project.value || project.projectName}
                />
              ))}
            </Select>

            <TextInput
              id="ethicsApprovalRef"
              labelText={intl.formatMessage({
                id: "biorepository.retrieval.ethicsRef",
                defaultMessage: "Ethics Approval Reference (if applicable)",
              })}
              placeholder={intl.formatMessage({
                id: "biorepository.retrieval.ethicsRef.placeholder",
                defaultMessage: "IRB-2024-001",
              })}
              value={ethicsApprovalRef}
              onChange={(e) => setEthicsApprovalRef(e.target.value)}
            />

            <DatePicker
              datePickerType="single"
              dateFormat="Y-m-d"
              value={requiredByDate}
              onChange={(dates) => setRequiredByDate(dates[0] || null)}
            >
              <DatePickerInput
                id="requiredByDate"
                labelText={intl.formatMessage({
                  id: "biorepository.retrieval.requiredBy",
                  defaultMessage: "Required By Date (Optional)",
                })}
                placeholder="YYYY-MM-DD"
              />
            </DatePicker>
          </div>
        </div>

        {/* Submit Button */}
        <Button
          type="submit"
          kind="primary"
          renderIcon={SendAlt}
          disabled={submitting || selectedSamples.length === 0}
        >
          {submitting ? (
            <FormattedMessage
              id="biorepository.retrieval.submitting"
              defaultMessage="Submitting..."
            />
          ) : (
            <FormattedMessage
              id="biorepository.retrieval.submitRequest"
              defaultMessage="Submit Request for Approval"
            />
          )}
        </Button>
      </Form>

      {/* Sample Search Modal */}
      <Modal
        open={searchModalOpen}
        modalHeading={intl.formatMessage({
          id: "biorepository.retrieval.searchSamples",
          defaultMessage: "Search Available Samples",
        })}
        primaryButtonText={intl.formatMessage({
          id: "biorepository.retrieval.addSelected",
          defaultMessage: "Add Selected",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setSearchModalOpen(false);
          setSearchType("barcode");
          setSearchQuery("");
          setSearchResults([]);
          setSearchSelectedRows([]);
        }}
        onRequestSubmit={handleAddSelectedSamples}
        primaryButtonDisabled={searchSelectedRows.length === 0}
        size="lg"
      >
        <div style={{ marginBottom: "1rem" }}>
          <div
            style={{ display: "flex", gap: "0.5rem", alignItems: "flex-end" }}
          >
            <Select
              id="searchType"
              labelText={intl.formatMessage({
                id: "biorepository.retrieval.search.type",
                defaultMessage: "Search By",
              })}
              value={searchType}
              onChange={(e) => setSearchType(e.target.value)}
              style={{ minWidth: "180px" }}
            >
              <SelectItem
                value="barcode"
                text={intl.formatMessage({
                  id: "biorepository.retrieval.search.type.barcode",
                  defaultMessage: "Barcode",
                })}
              />
              <SelectItem
                value="originLab"
                text={intl.formatMessage({
                  id: "biorepository.retrieval.search.type.originLab",
                  defaultMessage: "Origin Lab",
                })}
              />
              <SelectItem
                value="projectId"
                text={intl.formatMessage({
                  id: "biorepository.retrieval.search.type.projectId",
                  defaultMessage: "Project ID",
                })}
              />
            </Select>
            <TextInput
              id="searchQuery"
              labelText={intl.formatMessage({
                id: "biorepository.retrieval.search.value",
                defaultMessage: "Search Value",
              })}
              placeholder={intl.formatMessage(
                {
                  id: "biorepository.retrieval.search.placeholder.dynamic",
                  defaultMessage: "Enter {searchType}...",
                },
                {
                  searchType:
                    searchType === "barcode"
                      ? intl.formatMessage({
                          id: "biorepository.retrieval.search.type.barcode",
                          defaultMessage: "barcode",
                        })
                      : searchType === "originLab"
                        ? intl.formatMessage({
                            id: "biorepository.retrieval.search.type.originLab",
                            defaultMessage: "origin lab",
                          })
                        : intl.formatMessage({
                            id: "biorepository.retrieval.search.type.projectId",
                            defaultMessage: "project ID",
                          }),
                },
              )}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  e.preventDefault();
                  handleSearchSamples();
                }
              }}
              style={{ flex: 1 }}
            />
            <Button
              kind="primary"
              size="md"
              renderIcon={Search}
              onClick={handleSearchSamples}
              disabled={searchLoading || !searchQuery.trim()}
            >
              <FormattedMessage id="label.search" defaultMessage="Search" />
            </Button>
          </div>
        </div>

        {searchLoading && <Loading small withOverlay={false} />}

        {!searchLoading && searchResults.length > 0 && (
          <DataTable
            rows={searchResults.map((s) => ({
              id: (s.id || s.sampleItemId).toString(),
              sampleNumber:
                s.barcode || s.sampleNumber || `BIO-${s.id || s.sampleItemId}`,
              sampleType:
                s.sampleType?.description || s.sampleTypeName || "N/A",
              collectionDate: s.collectionDate || "N/A",
              storageLocation:
                s.storageLocationName || s.storageLocation || "N/A",
            }))}
            headers={searchHeaders}
            size="sm"
          >
            {({
              rows,
              headers,
              getTableProps,
              getHeaderProps,
              getRowProps,
              getSelectionProps,
              selectedRows,
            }) => {
              // Sync selection state
              if (selectedRows.length !== searchSelectedRows.length) {
                const newSelection = selectedRows.map((r) => r.id);
                if (
                  JSON.stringify(newSelection) !==
                  JSON.stringify(searchSelectedRows)
                ) {
                  setTimeout(() => setSearchSelectedRows(newSelection), 0);
                }
              }
              return (
                <TableContainer>
                  <Table {...getTableProps()}>
                    <TableHead>
                      <TableRow>
                        <TableSelectAll {...getSelectionProps()} />
                        {headers.map((header) => (
                          <TableHeader
                            {...getHeaderProps({ header })}
                            key={header.key}
                          >
                            {header.header}
                          </TableHeader>
                        ))}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {rows.map((row) => (
                        <TableRow {...getRowProps({ row })} key={row.id}>
                          <TableSelectRow {...getSelectionProps({ row })} />
                          {row.cells.map((cell) => (
                            <TableCell key={cell.id}>{cell.value}</TableCell>
                          ))}
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              );
            }}
          </DataTable>
        )}

        {!searchLoading && searchQuery && searchResults.length === 0 && (
          <p
            style={{ color: "#525252", fontStyle: "italic", marginTop: "1rem" }}
          >
            <FormattedMessage
              id="biorepository.retrieval.search.noResults"
              defaultMessage="No available samples found matching your search."
            />
          </p>
        )}
      </Modal>
    </div>
  );
}

RequestSubmissionTab.propTypes = {
  onRequestCreated: PropTypes.func,
};

export default RequestSubmissionTab;
