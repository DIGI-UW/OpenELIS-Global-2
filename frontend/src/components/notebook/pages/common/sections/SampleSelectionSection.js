import React, { useState, useCallback } from "react";
import {
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
  Button,
  TextInput,
  Select,
  SelectItem,
  Loading,
  Tag,
} from "@carbon/react";
import { Add, Search, TrashCan } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../../../utils/Utils";

/**
 * Section B: Sample Details / Selection (AHRI BR-F-02)
 * Inline search and select samples from the biorepository.
 */
function SampleSelectionSection({
  selectedSamples,
  onSamplesChange,
  readOnly,
}) {
  const intl = useIntl();

  // Inline search state
  const [searchType, setSearchType] = useState("barcode");
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);

  const handleSearchSamples = useCallback(() => {
    if (!searchQuery.trim()) return;
    setSearchLoading(true);
    setHasSearched(true);
    const searchParam = `${searchType}=${encodeURIComponent(searchQuery.trim())}`;
    getFromOpenElisServer(
      `/rest/biorepository/sample/search?${searchParam}&status=STORED`,
      (data) => {
        setSearchLoading(false);
        if (data && Array.isArray(data)) {
          const selectedIds = selectedSamples.map((s) => s.id);
          setSearchResults(data.filter((s) => !selectedIds.includes(s.id)));
        } else {
          setSearchResults([]);
        }
      },
    );
  }, [searchQuery, searchType, selectedSamples]);

  const handleAddSample = useCallback(
    (sample) => {
      onSamplesChange([...selectedSamples, { ...sample, remark: "" }]);
      setSearchResults((prev) => prev.filter((s) => s.id !== sample.id));
    },
    [selectedSamples, onSamplesChange],
  );

  const handleRemoveSample = useCallback(
    (sampleId) => {
      onSamplesChange(selectedSamples.filter((s) => s.id !== sampleId));
    },
    [selectedSamples, onSamplesChange],
  );

  const handleFieldChange = useCallback(
    (sampleId, field, value) => {
      onSamplesChange(
        selectedSamples.map((s) =>
          s.id === sampleId ? { ...s, [field]: value } : s,
        ),
      );
    },
    [selectedSamples, onSamplesChange],
  );

  const getSampleTypeLabel = (s) => {
    if (s.sampleTypeName) return s.sampleTypeName;
    if (s.sampleType && typeof s.sampleType === "object")
      return s.sampleType.description || s.sampleType.name || "-";
    if (typeof s.sampleType === "string") return s.sampleType;
    return "-";
  };

  const selectedHeaders = [
    {
      key: "sampleNumber",
      header: intl.formatMessage({
        id: "biorepo.import.field.batchNo",
        defaultMessage: "Batch No. / Barcode",
      }),
    },
    {
      key: "sampleType",
      header: intl.formatMessage({
        id: "biorepo.import.field.sampleType",
        defaultMessage: "Sample Type",
      }),
    },
    {
      key: "remark",
      header: intl.formatMessage({
        id: "biorepo.import.field.remark",
        defaultMessage: "Remark",
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

  const selectedRows = selectedSamples.map((s) => ({
    id: s.id.toString(),
    sampleNumber: s.barcode || s.sampleNumber || s.externalId || "-",
    sampleType: getSampleTypeLabel(s),
    remark: s.remark || "",
  }));

  return (
    <div className="biorepo-section" style={{ marginBottom: "2rem" }}>
      <h4 style={{ marginBottom: "1rem" }}>
        <FormattedMessage
          id="biorepo.import.section.samples"
          defaultMessage="Section B: Sample Details"
        />
      </h4>

      {/* Inline search */}
      {!readOnly && (
        <div
          style={{
            display: "flex",
            gap: "0.5rem",
            alignItems: "flex-end",
            marginBottom: "1rem",
          }}
        >
          <Select
            id="searchType"
            labelText={intl.formatMessage({
              id: "biorepo.import.searchModal.searchBy",
              defaultMessage: "Search By",
            })}
            value={searchType}
            onChange={(e) => setSearchType(e.target.value)}
            size="sm"
            style={{ minWidth: "140px" }}
          >
            <SelectItem value="barcode" text="Barcode" />
            <SelectItem value="originLab" text="Origin Lab" />
            <SelectItem value="projectId" text="Project ID" />
          </Select>
          <TextInput
            id="searchQuery"
            labelText={intl.formatMessage({
              id: "biorepo.import.searchModal.query",
              defaultMessage: "Search Value",
            })}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handleSearchSamples()}
            size="sm"
          />
          <Button
            kind="primary"
            size="sm"
            renderIcon={Search}
            onClick={handleSearchSamples}
            disabled={searchLoading || !searchQuery.trim()}
          >
            <FormattedMessage
              id="label.button.search"
              defaultMessage="Search"
            />
          </Button>
        </div>
      )}

      {/* Search results */}
      {searchLoading && <Loading withOverlay={false} small />}
      {!searchLoading && searchResults.length > 0 && (
        <div style={{ marginBottom: "1rem" }}>
          <DataTable
            rows={searchResults.map((s) => ({
              id: s.id.toString(),
              sampleNumber: s.barcode || s.sampleNumber || s.externalId || "-",
              sampleType: getSampleTypeLabel(s),
              storageLocation: s.storageLocation || "-",
            }))}
            headers={[
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
              { key: "add", header: "" },
            ]}
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
                          key={header.key}
                          {...getHeaderProps({ header })}
                        >
                          {header.header}
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {rows.map((row) => {
                      const sample = searchResults.find(
                        (s) => s.id.toString() === row.id,
                      );
                      return (
                        <TableRow key={row.id} {...getRowProps({ row })}>
                          {row.cells
                            .filter((c) => c.info.header !== "add")
                            .map((cell) => (
                              <TableCell key={cell.id}>{cell.value}</TableCell>
                            ))}
                          <TableCell>
                            <Button
                              kind="ghost"
                              size="sm"
                              renderIcon={Add}
                              iconDescription="Add"
                              hasIconOnly
                              onClick={() => handleAddSample(sample)}
                            />
                          </TableCell>
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
        </div>
      )}
      {!searchLoading &&
        hasSearched &&
        searchResults.length === 0 &&
        searchQuery.trim() && (
          <p
            style={{
              color: "#525252",
              fontSize: "0.875rem",
              marginBottom: "1rem",
            }}
          >
            <FormattedMessage
              id="biorepo.import.noResults"
              defaultMessage="No samples found matching your search."
            />
          </p>
        )}

      {/* Selected samples */}
      <h5 style={{ marginBottom: "0.5rem", marginTop: "0.5rem" }}>
        <FormattedMessage
          id="biorepo.import.selectedSamples"
          defaultMessage="Selected Samples"
        />
      </h5>
      {selectedSamples.length > 0 ? (
        <DataTable rows={selectedRows} headers={selectedHeaders} size="sm">
          {({ rows, headers, getTableProps, getHeaderProps, getRowProps }) => (
            <TableContainer>
              <Table {...getTableProps()}>
                <TableHead>
                  <TableRow>
                    {headers.map((header) => (
                      <TableHeader
                        key={header.key}
                        {...getHeaderProps({ header })}
                      >
                        {header.header}
                      </TableHeader>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {rows.map((row) => {
                    const sample = selectedSamples.find(
                      (s) => s.id.toString() === row.id,
                    );
                    return (
                      <TableRow key={row.id} {...getRowProps({ row })}>
                        <TableCell>{row.cells[0].value}</TableCell>
                        <TableCell>{row.cells[1].value}</TableCell>
                        <TableCell>
                          {readOnly ? (
                            sample?.remark || ""
                          ) : (
                            <TextInput
                              id={`remark-${row.id}`}
                              labelText=""
                              value={sample?.remark || ""}
                              onChange={(e) =>
                                handleFieldChange(
                                  sample?.id,
                                  "remark",
                                  e.target.value,
                                )
                              }
                              size="sm"
                            />
                          )}
                        </TableCell>
                        <TableCell>
                          {!readOnly && (
                            <Button
                              kind="ghost"
                              size="sm"
                              renderIcon={TrashCan}
                              iconDescription="Remove"
                              hasIconOnly
                              onClick={() => handleRemoveSample(sample?.id)}
                            />
                          )}
                        </TableCell>
                      </TableRow>
                    );
                  })}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </DataTable>
      ) : (
        <Tag type="cool-gray">
          <FormattedMessage
            id="biorepo.import.noSamples"
            defaultMessage="No samples selected"
          />
        </Tag>
      )}
    </div>
  );
}

export default SampleSelectionSection;
