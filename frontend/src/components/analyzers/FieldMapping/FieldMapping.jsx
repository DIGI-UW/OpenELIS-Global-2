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
import { 
  Grid, 
  Column, 
  Button, 
  Search, 
  Tile,
  InlineNotification 
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { useParams, useHistory, useLocation } from "react-router-dom";
import * as analyzerService from "../../../services/analyzerService";
import FieldMappingPanel from "./FieldMappingPanel";
import MappingPanel from "./MappingPanel";
import QueryStatusModal from "./QueryStatusModal";
import TestMappingModal from "./TestMappingModal";
import PageTitle from "../../common/PageTitle/PageTitle";
import "./FieldMapping.css";

// Helper function to extract mappings from API response
const extractMappings = (mappingsData) => {
  if (!mappingsData) return [];
  if (Array.isArray(mappingsData)) return mappingsData;
  if (mappingsData.data) {
    if (Array.isArray(mappingsData.data.content)) return mappingsData.data.content;
    if (Array.isArray(mappingsData.data)) return mappingsData.data;
  }
  return [];
};

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
  const [testMappingModalOpen, setTestMappingModalOpen] = useState(false);

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
      const mappings = extractMappings(mappingsData);
      setMappings(mappings);
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
            const mappings = extractMappings(mappingsData);
            setMappings(mappings);
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

  // Calculate statistics for stats cards
  const requiredMappings = mappings.filter((m) => m.isRequired).length;
  const unmappedFieldsCount = fields.filter(
    (f) => !mappings.some((m) => m.analyzerFieldId === f.id)
  ).length;
  
  // Check if required mappings are missing
  const requiredFieldTypes = ["sampleId", "testCode", "resultValue"];
  const hasUnmappedRequired = requiredFieldTypes.some(
    (type) => !mappings.some((m) => m.mappingType === type)
  );

  return (
    <div className="field-mapping" data-testid="field-mapping">
      {/* Hierarchical Page Title with Back Arrow */}
      <div className="field-mapping-header">
        <div className="field-mapping-header-title">
          <PageTitle
            breadcrumbs={[
              { label: intl.formatMessage({ id: "analyzer.page.hierarchy.root" }), link: "/analyzers" },
              { label: intl.formatMessage({ id: "analyzer.page.hierarchy.mappings" }) },
              { label: analyzer?.name || intl.formatMessage({ id: "analyzer.fieldMapping.page.title" }) }
            ]}
            showBackArrow={true}
            onBack={() => history.push("/analyzers")}
            subtitle={intl.formatMessage({ id: "analyzer.fieldMapping.page.subtitle" })}
          />
        </div>
      </div>

      {/* Warning Banner - Conditional */}
      {hasUnmappedRequired && (
        <Grid>
          <Column lg={16} md={8} sm={4}>
            <InlineNotification
              kind="warning"
              title={intl.formatMessage({ id: "analyzer.fieldMapping.warning.missingRequired" })}
              subtitle={intl.formatMessage({ id: "analyzer.fieldMapping.warning.missingRequired.detail" })}
              lowContrast
              hideCloseButton
              data-testid="field-mapping-warning"
            />
          </Column>
        </Grid>
      )}

      {/* Statistics Cards */}
      <Grid className="field-mapping-stats" data-testid="field-mapping-stats">
        <Column lg={5} md={4} sm={4}>
          <Tile data-testid="stat-total-mappings">
            <div className="stat-label">
              {intl.formatMessage({ id: "analyzer.fieldMapping.stats.total" })}
            </div>
            <div className="stat-value">{mappings.length}</div>
          </Tile>
        </Column>
        <Column lg={6} md={4} sm={4}>
          <Tile data-testid="stat-required-mappings">
            <div className="stat-label">
              {intl.formatMessage({ id: "analyzer.fieldMapping.stats.required" })}
            </div>
            <div className="stat-value">{requiredMappings}</div>
          </Tile>
        </Column>
        <Column lg={5} md={4} sm={4}>
          <Tile data-testid="stat-unmapped-fields">
            <div className="stat-label">
              {intl.formatMessage({ id: "analyzer.fieldMapping.stats.unmapped" })}
            </div>
            <div className="stat-value">{unmappedFieldsCount}</div>
          </Tile>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="field-mapping-actions" data-testid="field-mapping-actions">
        <Button
          kind="tertiary"
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
        >
          <FormattedMessage id="analyzer.fieldMapping.queryAnalyzer" />
        </Button>
        <Button
          kind="ghost"
          data-testid="field-mapping-test-button"
          onClick={() => setTestMappingModalOpen(true)}
        >
          <FormattedMessage id="analyzer.fieldMapping.testMapping" />
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
                          const mappings = extractMappings(mappingsData);
                          setMappings(mappings);
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
      <TestMappingModal
        open={testMappingModalOpen}
        onClose={() => setTestMappingModalOpen(false)}
        analyzerId={analyzerId}
        analyzerName={analyzer?.name}
        analyzerType={analyzer?.analyzerType}
        activeMappingsCount={mappings.filter((m) => m.isActive).length}
      />
    </div>
  );
};

export default FieldMapping;
