/**
 * FieldMapping Component
 *
 * Dual-panel interface for mapping analyzer fields to OpenELIS fields
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
  InlineNotification,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { useParams, useHistory, useLocation } from "react-router-dom";
import * as analyzerService from "../../../services/analyzerService";
import FieldMappingPanel from "./FieldMappingPanel";
import MappingPanel from "./MappingPanel";
import QueryStatusModal from "./QueryStatusModal";
import TestMappingModal from "./TestMappingModal";
import ValidationDashboard from "./ValidationDashboard";
import PageTitle from "../../common/PageTitle/PageTitle";
import "./FieldMapping.css";

// Helper function to extract mappings from API response
const extractMappings = (mappingsData) => {
  if (!mappingsData) return [];
  if (Array.isArray(mappingsData)) return mappingsData;
  if (mappingsData.data) {
    if (Array.isArray(mappingsData.data.content))
      return mappingsData.data.content;
    if (Array.isArray(mappingsData.data)) return mappingsData.data;
  }
  return [];
};

const FieldMapping = () => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const { id: analyzerId } = useParams();

  const [analyzer, setAnalyzer] = useState(null);
  const [fields, setFields] = useState([]);
  const [mappings, setMappings] = useState([]);
  const [selectedField, setSelectedField] = useState(null);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [queryModalOpen, setQueryModalOpen] = useState(false);
  const [queryJobId, setQueryJobId] = useState(null);
  const [testMappingModalOpen, setTestMappingModalOpen] = useState(false);
  const [errorNotification, setErrorNotification] = useState(null);

  useEffect(() => {
    if (!analyzerId) {
      return;
    }

    const params = new URLSearchParams(location.search);
    const initialSearch = params.get("search") || "";
    const initialSelectedFieldId = params.get("selectedField") || "";

    setSearchTerm(initialSearch);

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

    const storedSelectedField = sessionStorage.getItem(
      `fieldMapping.${analyzerId}.selectedField`,
    );
    const fieldIdToRestore = initialSelectedFieldId || storedSelectedField;

    setLoading(true);

    analyzerService.getAnalyzer(analyzerId, (analyzerData) => {
      if (analyzerData) {
        setAnalyzer(analyzerData);
      }
    });

    analyzerService.getFields(analyzerId, (fieldsData) => {
      if (fieldsData && Array.isArray(fieldsData)) {
        setFields(fieldsData);

        const fieldId = fieldIdToRestore;
        if (fieldId) {
          const fieldToSelect = fieldsData.find((f) => f.id === fieldId);
          if (fieldToSelect) {
            setSelectedField(fieldToSelect);
          }
        }
      }
    });

    analyzerService.getMappings(analyzerId, (mappingsData) => {
      const mappings = extractMappings(mappingsData);
      setMappings(mappings);
      setLoading(false);
    });

    // Note: Initial query is removed - fields are loaded from database on page load
    // User must explicitly click "Query Analyzer" button to trigger a new query

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

  const handleFieldSelect = (field) => {
    setSelectedField(field);

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

  const handleCreateMapping = (mappingData) => {
    analyzerService.createMapping(
      analyzerId,
      mappingData,
      (response, error) => {
        if (!error && !(response && response.error)) {
          analyzerService.getMappings(analyzerId, (mappingsData) => {
            const mappings = extractMappings(mappingsData);
            setMappings(mappings);
          });
        }
      },
    );
  };

  const filteredFields = fields.filter((field) => {
    if (!searchTerm) return true;
    const searchLower = searchTerm.toLowerCase();
    return (
      (field.fieldName &&
        field.fieldName.toLowerCase().includes(searchLower)) ||
      (field.astmRef && field.astmRef.toLowerCase().includes(searchLower))
    );
  });

  const selectedFieldMapping = selectedField
    ? mappings.find((m) => m.analyzerFieldId === selectedField.id)
    : null;

  const requiredMappings = mappings.filter((m) => m.isRequired).length;
  const unmappedFieldsCount = fields.filter(
    (f) => !mappings.some((m) => m.analyzerFieldId === f.id),
  ).length;

  const requiredFieldTypes = ["sampleId", "testCode", "resultValue"];
  const hasUnmappedRequired = requiredFieldTypes.some(
    (type) => !mappings.some((m) => m.mappingType === type),
  );

  return (
    <div className="field-mapping" data-testid="field-mapping">
      {/* Hierarchical Page Title with Back Arrow */}
      <div className="field-mapping-header">
        <div className="field-mapping-header-title">
          <PageTitle
            breadcrumbs={[
              {
                label: intl.formatMessage({
                  id: "analyzer.page.hierarchy.root",
                }),
                link: "/analyzers",
              },
              {
                label: intl.formatMessage({
                  id: "analyzer.page.hierarchy.mappings",
                }),
              },
              {
                label:
                  analyzer?.name ||
                  intl.formatMessage({
                    id: "analyzer.fieldMapping.page.title",
                  }),
              },
            ]}
            showBackArrow={true}
            onBack={() => history.push("/analyzers")}
            subtitle={intl.formatMessage({
              id: "analyzer.fieldMapping.page.subtitle",
            })}
          />
        </div>
      </div>

      {errorNotification && (
        <Grid>
          <Column lg={16} md={8} sm={4}>
            <InlineNotification
              kind={errorNotification.kind}
              title={errorNotification.title}
              subtitle={errorNotification.subtitle}
              lowContrast={errorNotification.kind === "warning"}
              onClose={() => setErrorNotification(null)}
              data-testid="field-mapping-error-notification"
            />
          </Column>
        </Grid>
      )}

      {hasUnmappedRequired && (
        <Grid>
          <Column lg={16} md={8} sm={4}>
            <InlineNotification
              kind="warning"
              title={intl.formatMessage({
                id: "analyzer.fieldMapping.warning.missingRequired",
              })}
              subtitle={intl.formatMessage({
                id: "analyzer.fieldMapping.warning.missingRequired.detail",
              })}
              lowContrast
              hideCloseButton
              data-testid="field-mapping-warning"
            />
          </Column>
        </Grid>
      )}

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
              {intl.formatMessage({
                id: "analyzer.fieldMapping.stats.required",
              })}
            </div>
            <div className="stat-value">{requiredMappings}</div>
          </Tile>
        </Column>
        <Column lg={5} md={4} sm={4}>
          <Tile data-testid="stat-unmapped-fields">
            <div className="stat-label">
              {intl.formatMessage({
                id: "analyzer.fieldMapping.stats.unmapped",
              })}
            </div>
            <div className="stat-value">{unmappedFieldsCount}</div>
          </Tile>
        </Column>
      </Grid>

      {/* Validation Dashboard - Only shown in VALIDATION stage */}
      {analyzer?.status === "VALIDATION" && (
        <ValidationDashboard analyzerId={analyzerId} status={analyzer.status} />
      )}

      <div
        className="field-mapping-actions"
        data-testid="field-mapping-actions"
      >
        <Button
          kind="tertiary"
          data-testid="field-mapping-query-button"
          onClick={() => {
            analyzerService.queryAnalyzer(analyzerId, (resp, error) => {
              if (error) {
                setQueryModalOpen(true);
                return;
              }

              if (resp && resp.jobId) {
                setQueryJobId(resp.jobId);
                setQueryModalOpen(true);
              } else if (
                resp &&
                Array.isArray(resp.fields) &&
                resp.fields.length > 0
              ) {
                setFields(resp.fields);
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

      <Grid className="field-mapping-grid">
        <Column lg={8} md={8} sm={4}>
          <FieldMappingPanel
            fields={filteredFields}
            selectedField={selectedField}
            onFieldSelect={handleFieldSelect}
            searchTerm={searchTerm}
            onSearchChange={(value) => {
              setSearchTerm(value);

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
        onClose={() => {
          setQueryModalOpen(false);
          setErrorNotification(null);
        }}
        analyzerId={analyzerId}
        jobId={queryJobId}
        onCompleted={(data) => {
          if (data && data.state === "completed") {
            if (data.error) {
              setErrorNotification({
                kind: "error",
                title: "Query Failed",
                subtitle:
                  data.error ||
                  "Failed to query analyzer fields. Please try again.",
              });
            } else {
              if (
                data.fields &&
                Array.isArray(data.fields) &&
                data.fields.length > 0
              ) {
                // Check if fields have IDs (from database) or are just parsed (no IDs)
                const hasIds = data.fields.every((f) => f.id != null);

                if (hasIds) {
                  setFields(data.fields);
                  setErrorNotification(null);
                } else {
                  analyzerService.getFields(analyzerId, (fieldsData) => {
                    if (fieldsData && Array.isArray(fieldsData)) {
                      setFields(fieldsData);
                      setErrorNotification(null);
                    } else {
                      setErrorNotification({
                        kind: "error",
                        title: "Failed to Load Fields",
                        subtitle:
                          "Query completed but could not reload fields from database.",
                      });
                    }
                  });
                }
              } else {
                analyzerService.getFields(analyzerId, (fieldsData) => {
                  if (fieldsData && Array.isArray(fieldsData)) {
                    if (fieldsData.length === 0) {
                      setErrorNotification({
                        kind: "warning",
                        title: "No Fields Retrieved",
                        subtitle:
                          "The query completed but no fields were saved. Check backend logs for errors.",
                      });
                    } else {
                      setFields(fieldsData);
                      setErrorNotification(null);
                    }
                  } else {
                    setErrorNotification({
                      kind: "error",
                      title: "Failed to Load Fields",
                      subtitle: "Query completed but could not load fields.",
                    });
                  }
                });
              }
            }
          } else if (data && data.state === "failed") {
            setErrorNotification({
              kind: "error",
              title: "Query Failed",
              subtitle:
                data.error ||
                "Failed to query analyzer. Please check the analyzer connection and try again.",
            });
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
