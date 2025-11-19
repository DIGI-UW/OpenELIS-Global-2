/**
 * FieldMapping Component
 *
 * Dual-panel interface for mapping analyzer fields to OpenELIS fields
 * Task Reference: T059
 *
 * Features:
 * - 50/50 split layout using Carbon Grid
 * - Left panel: Analyzer fields table
 * - Right panel: Mapping panel (View/Edit mode)
 * - Field selection opens mapping panel
 */

import React, { useState, useEffect, useRef } from "react";
import { Grid, Column, Button, Search } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { useParams, useHistory, useLocation } from "react-router-dom";
import * as analyzerService from "../../../services/analyzerService";
import FieldMappingPanel from "./FieldMappingPanel";
import MappingPanel from "./MappingPanel";
import QueryStatusModal from "./QueryStatusModal";
import "./FieldMapping.css";

const FieldMapping = () => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const { id: analyzerId } = useParams();

  // State
  const [analyzer, setAnalyzer] = useState(null);
  const [fields, setFields] = useState([]);
  const [mappings, setMappings] = useState([]);
  const [selectedField, setSelectedField] = useState(null);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [queryModalOpen, setQueryModalOpen] = useState(false);
  const [queryJobId, setQueryJobId] = useState(null);

  // Restore state from URL query parameters and sessionStorage
  useEffect(() => {
    if (!analyzerId) {
      return;
    }

    // Restore from URL query parameters
    const params = new URLSearchParams(location.search);
    const initialSearch = params.get("search") || "";
    const initialSelectedFieldId = params.get("selectedField") || "";

    setSearchTerm(initialSearch);

    // Restore scroll position from sessionStorage
    const storageKey = `fieldMapping.${analyzerId}.scrollY`;
    const storedScrollY = sessionStorage.getItem(storageKey);
    if (storedScrollY) {
      try {
        setTimeout(() => {
          window.scrollTo(0, parseInt(storedScrollY, 10));
        }, 100);
      } catch (_) {
        // ignore
      }
    }

    // Restore selected field from URL or sessionStorage
    const storedSelectedField = sessionStorage.getItem(
      `fieldMapping.${analyzerId}.selectedField`,
    );
    const fieldIdToRestore = initialSelectedFieldId || storedSelectedField;

    setLoading(true);

    // Load analyzer
    analyzerService.getAnalyzer(analyzerId, (analyzerData) => {
      if (analyzerData) {
        setAnalyzer(analyzerData);
      }
    });

    // Load mappings
    analyzerService.getMappings(analyzerId, (mappingsData) => {
      if (Array.isArray(mappingsData)) {
        setMappings(mappingsData);
      }
      setLoading(false);
    });

    // Optionally kick off an initial query (skeleton). If fields are returned immediately (tests/mocks), use them.
    analyzerService.queryAnalyzer(analyzerId, (response) => {
      if (
        response &&
        Array.isArray(response.fields) &&
        response.fields.length > 0
      ) {
        setFields(response.fields);

        // Restore selected field after fields are loaded
        const fieldId = fieldIdToRestore;
        if (fieldId) {
          const fieldToSelect = response.fields.find((f) => f.id === fieldId);
          if (fieldToSelect) {
            setSelectedField(fieldToSelect);
          }
        }
      }
      if (response && response.jobId) {
        setQueryJobId(response.jobId);
      }
    });

    // Persist scroll position on unload
    const onBeforeUnload = () => {
      sessionStorage.setItem(
        `fieldMapping.${analyzerId}.scrollY`,
        String(window.scrollY),
      );
    };
    window.addEventListener("beforeunload", onBeforeUnload);
    return () => {
      window.removeEventListener("beforeunload", onBeforeUnload);
      sessionStorage.setItem(
        `fieldMapping.${analyzerId}.scrollY`,
        String(window.scrollY),
      );
    };
  }, [analyzerId, location.search]);

  // Handle field selection - update URL and sessionStorage
  const handleFieldSelect = (field) => {
    setSelectedField(field);

    // Update URL query parameters
    const params = new URLSearchParams(location.search);
    if (field && field.id) {
      params.set("selectedField", field.id);
      sessionStorage.setItem(
        `fieldMapping.${analyzerId}.selectedField`,
        field.id,
      );
    } else {
      params.delete("selectedField");
      sessionStorage.removeItem(`fieldMapping.${analyzerId}.selectedField`);
    }
    history.replace({
      pathname: location.pathname,
      search: params.toString(),
    });
  };

  // Handle mapping creation
  const handleCreateMapping = (mappingData) => {
    analyzerService.createMapping(
      analyzerId,
      mappingData,
      (response, error) => {
        if (error || (response && response.error)) {
          // Handle error
          console.error("Failed to create mapping:", error || response?.error);
        } else {
          // Reload mappings
          analyzerService.getMappings(analyzerId, (mappingsData) => {
            if (Array.isArray(mappingsData)) {
              setMappings(mappingsData);
            }
          });
          // Keep field selected to show the new mapping
        }
      },
    );
  };

  // Filter fields by search term
  const filteredFields = fields.filter((field) => {
    if (!searchTerm) return true;
    const searchLower = searchTerm.toLowerCase();
    return (
      (field.fieldName &&
        field.fieldName.toLowerCase().includes(searchLower)) ||
      (field.astmRef && field.astmRef.toLowerCase().includes(searchLower))
    );
  });

  // Get mapping for selected field
  const selectedFieldMapping = selectedField
    ? mappings.find((m) => m.analyzerFieldId === selectedField.id)
    : null;

  return (
    <div className="field-mapping" data-testid="field-mapping">
      {/* Page Header */}
      <div className="field-mapping-header" data-testid="field-mapping-header">
        <Button
          kind="ghost"
          onClick={() => history.push("/analyzers")}
          data-testid="field-mapping-back-button"
        >
          <FormattedMessage id="analyzer.fieldMapping.back" />
        </Button>
        <h1 data-testid="field-mapping-title">
          {analyzer
            ? analyzer.name
            : intl.formatMessage({ id: "analyzer.fieldMapping.page.title" })}
        </h1>
        <Button
          kind="ghost"
          data-testid="field-mapping-query-button"
          onClick={() => {
            analyzerService.queryAnalyzer(analyzerId, (resp) => {
              if (resp && resp.jobId) {
                setQueryJobId(resp.jobId);
                setQueryModalOpen(true);
              } else {
                setQueryModalOpen(true);
              }
            });
          }}
          style={{ marginRight: "0.5rem" }}
        >
          <FormattedMessage id="analyzer.fieldMapping.queryAnalyzer" />
        </Button>
        <Button kind="primary" data-testid="field-mapping-save-button">
          <FormattedMessage id="analyzer.fieldMapping.save" />
        </Button>
      </div>

      {/* Dual Panel Layout */}
      <Grid className="field-mapping-grid">
        {/* Left Panel: Analyzer Fields */}
        <Column lg={8} md={8} sm={4}>
          <FieldMappingPanel
            fields={filteredFields}
            selectedField={selectedField}
            onFieldSelect={handleFieldSelect}
            searchTerm={searchTerm}
            onSearchChange={(value) => {
              setSearchTerm(value);

              // Update URL query parameters
              const params = new URLSearchParams(location.search);
              if (value) {
                params.set("search", value);
              } else {
                params.delete("search");
              }
              history.replace({
                pathname: location.pathname,
                search: params.toString(),
              });
            }}
            mappings={mappings}
          />
        </Column>

        {/* Right Panel: Mapping Panel */}
        <Column lg={8} md={8} sm={4}>
          {selectedField ? (
            <MappingPanel
              field={selectedField}
              mapping={selectedFieldMapping}
              onCreateMapping={handleCreateMapping}
              onUpdateMapping={(mappingId, mappingData) => {
                analyzerService.updateMapping(
                  analyzerId,
                  mappingId,
                  mappingData,
                  (response, error) => {
                    if (!error && !response?.error) {
                      analyzerService.getMappings(
                        analyzerId,
                        (mappingsData) => {
                          if (Array.isArray(mappingsData)) {
                            setMappings(mappingsData);
                          }
                        },
                      );
                    }
                  },
                );
              }}
              analyzerName={analyzer?.name || ""}
              analyzerIsActive={analyzer?.active || false}
            />
          ) : (
            <div
              className="mapping-panel-placeholder"
              data-testid="mapping-panel-placeholder"
            >
              <p>
                <FormattedMessage id="analyzer.fieldMapping.panel.target.summary" />
              </p>
              <p>
                Select a field from the left panel to view or create mappings.
              </p>
            </div>
          )}
        </Column>
      </Grid>
      <QueryStatusModal
        open={queryModalOpen}
        onClose={() => setQueryModalOpen(false)}
        analyzerId={analyzerId}
        jobId={queryJobId}
        onCompleted={(data) => {
          if (data && Array.isArray(data.fields)) {
            setFields(data.fields);
          }
        }}
      />
    </div>
  );
};

export default FieldMapping;
