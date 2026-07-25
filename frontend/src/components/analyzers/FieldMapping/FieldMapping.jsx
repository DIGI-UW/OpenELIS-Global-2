import React, { useEffect, useState } from "react";
import { Column, Grid, InlineNotification, Tile } from "@carbon/react";
import { useIntl } from "react-intl";
import { useHistory, useParams } from "react-router-dom";
import * as analyzerService from "../../../services/analyzerService";
import PageTitle from "../../common/PageTitle/PageTitle";
import PendingCodesPanel from "./PendingCodesPanel";
import ResultValueMappingsPanel from "./ResultValueMappingsPanel";
import SetupVerificationPanel from "./SetupVerificationPanel";
import "./FieldMapping.css";

const extractMappings = (response) => {
  if (!response) return [];
  if (Array.isArray(response)) return response;
  if (Array.isArray(response.data?.content)) return response.data.content;
  if (Array.isArray(response.data)) return response.data;
  return [];
};

const asArray = (value) => {
  if (!value) return [];
  if (Array.isArray(value)) return value;
  if (typeof value !== "object") return [];
  return Object.entries(value).map(([key, entry]) => ({
    analyzer_code: key,
    ...(entry && typeof entry === "object" ? entry : { openelis_test: entry }),
  }));
};

const testIdPart = (value) =>
  String(value || "unknown")
    .replace(/[^a-zA-Z0-9_-]+/g, "-")
    .replace(/^-+|-+$/g, "") || "unknown";

const extractReviewMappings = (pluginConfig, mappings, fields) => {
  const config = pluginConfig?.config || pluginConfig || {};
  const profileMappings = asArray(
    config.default_test_mappings ||
      config.defaultTestMappings ||
      config.test_mappings ||
      config.testMappings,
  );

  if (profileMappings.length > 0) {
    return profileMappings.map((mapping, index) => {
      const analyzerCode =
        mapping.analyzer_code ||
        mapping.analyzerCode ||
        mapping.code ||
        mapping.testCode ||
        mapping.test_code ||
        `mapping-${index + 1}`;
      return {
        id: testIdPart(analyzerCode),
        analyzerCode,
        openelisTest:
          mapping.test_name_hint ||
          mapping.openelis_test ||
          mapping.openelisTest ||
          mapping.testName ||
          mapping.test_name ||
          mapping.test_code ||
          mapping.loinc ||
          "-",
        loinc: mapping.loinc || mapping.loincCode || "-",
        status: "Profile",
      };
    });
  }

  return mappings.map((mapping, index) => {
    const field = fields.find((item) => item.id === mapping.analyzerFieldId);
    const analyzerCode =
      mapping.analyzerCode ||
      mapping.analyzerFieldName ||
      field?.fieldName ||
      field?.astmRef ||
      `mapping-${index + 1}`;
    return {
      id: testIdPart(analyzerCode),
      analyzerCode,
      openelisTest:
        mapping.openelisFieldName ||
        mapping.openelisField ||
        mapping.openelisTestName ||
        mapping.mappingType ||
        "-",
      loinc: mapping.loinc || mapping.loincCode || "-",
      status: mapping.isActive === false ? "Draft" : "Active",
    };
  });
};

const FieldMapping = () => {
  const intl = useIntl();
  const history = useHistory();
  const { id: analyzerId } = useParams();
  const [analyzer, setAnalyzer] = useState(null);
  const [fields, setFields] = useState([]);
  const [mappings, setMappings] = useState([]);
  const [pendingCodes, setPendingCodes] = useState([]);
  const [pluginConfig, setPluginConfig] = useState(null);
  const [resultValueMappings, setResultValueMappings] = useState([]);
  const [pendingResultValues, setPendingResultValues] = useState([]);

  useEffect(() => {
    if (!analyzerId) return;

    analyzerService.getAnalyzer(analyzerId, (response) => {
      setAnalyzer(response || null);
    });
    analyzerService.getFields(analyzerId, (response) => {
      setFields(Array.isArray(response) ? response : []);
    });
    analyzerService.getMappings(analyzerId, (response) => {
      setMappings(extractMappings(response));
    });
    analyzerService.getPendingCodes(analyzerId, (response) => {
      setPendingCodes(Array.isArray(response) ? response : []);
    });
    analyzerService.getPluginConfig(analyzerId, (response) => {
      setPluginConfig(
        response && typeof response === "object" ? response : null,
      );
    });
    analyzerService.getResultValueMappings(analyzerId, (response) => {
      setResultValueMappings(Array.isArray(response) ? response : []);
    });
    analyzerService.getPendingResultValues(analyzerId, (response) => {
      setPendingResultValues(Array.isArray(response) ? response : []);
    });
  }, [analyzerId]);

  const refreshPendingCodes = () => {
    analyzerService.getPendingCodes(analyzerId, (response) => {
      setPendingCodes(Array.isArray(response) ? response : []);
    });
  };

  const refreshResultValues = () => {
    analyzerService.getResultValueMappings(analyzerId, (response) => {
      setResultValueMappings(Array.isArray(response) ? response : []);
    });
    analyzerService.getPendingResultValues(analyzerId, (response) => {
      setPendingResultValues(Array.isArray(response) ? response : []);
    });
    analyzerService.getPluginConfig(analyzerId, (response) => {
      setPluginConfig(
        response && typeof response === "object" ? response : null,
      );
    });
  };

  const reviewMappings = extractReviewMappings(pluginConfig, mappings, fields);

  return (
    <div className="field-mapping" data-testid="field-mapping">
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
            showBackArrow
            onBack={() => history.push("/analyzers")}
            subtitle={intl.formatMessage({
              id: "analyzer.fieldMapping.page.subtitle",
            })}
          />
        </div>
      </div>

      <Grid className="field-mapping-profile-context">
        <Column lg={16} md={8} sm={4}>
          <InlineNotification
            kind="info"
            title={intl.formatMessage({
              id: "analyzer.fieldMapping.profileContext.title",
              defaultMessage: "Profile mapping review",
            })}
            subtitle={intl.formatMessage({
              id: "analyzer.fieldMapping.profileContext.subtitle",
              defaultMessage:
                "Review profile-applied test mappings, pending analyzer codes, and qualitative result values in one workflow.",
            })}
            lowContrast
            hideCloseButton
            data-testid="field-mapping-profile-context"
          />
        </Column>
      </Grid>

      <Grid className="field-mapping-profile-review">
        <Column lg={16} md={8} sm={4}>
          <Tile data-testid="profile-applied-mappings-panel">
            <div className="profile-applied-mappings-header">
              <h4>
                {intl.formatMessage({
                  id: "analyzer.fieldMapping.profileApplied.title",
                  defaultMessage: "Profile-Applied Test Mappings",
                })}
              </h4>
              <p>
                {intl.formatMessage({
                  id: "analyzer.fieldMapping.profileApplied.subtitle",
                  defaultMessage:
                    "Confirm analyzer test codes are linked to the intended OpenELIS tests before activation.",
                })}
              </p>
            </div>
            {reviewMappings.length > 0 ? (
              <table className="profile-applied-mappings-table">
                <thead>
                  <tr>
                    <th>
                      {intl.formatMessage({
                        id: "analyzer.fieldMapping.profileApplied.analyzerCode",
                        defaultMessage: "Analyzer Code",
                      })}
                    </th>
                    <th>
                      {intl.formatMessage({
                        id: "analyzer.fieldMapping.profileApplied.openelisTest",
                        defaultMessage: "OpenELIS Test",
                      })}
                    </th>
                    <th>
                      {intl.formatMessage({
                        id: "analyzer.fieldMapping.profileApplied.loinc",
                        defaultMessage: "LOINC",
                      })}
                    </th>
                    <th>
                      {intl.formatMessage({
                        id: "analyzer.fieldMapping.profileApplied.status",
                        defaultMessage: "Status",
                      })}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {reviewMappings.map((mapping) => (
                    <tr
                      key={`${mapping.id}-${mapping.analyzerCode}`}
                      data-testid={`profile-applied-mapping-row-${mapping.id}`}
                    >
                      <td>{mapping.analyzerCode}</td>
                      <td>{mapping.openelisTest}</td>
                      <td>{mapping.loinc}</td>
                      <td>
                        {intl.formatMessage({
                          id: `analyzer.fieldMapping.profileApplied.status.${mapping.status.toLowerCase()}`,
                          defaultMessage: mapping.status,
                        })}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <p
                className="profile-applied-mappings-empty"
                data-testid="profile-applied-mappings-empty"
              >
                {intl.formatMessage({
                  id: "analyzer.fieldMapping.profileApplied.empty",
                  defaultMessage:
                    "No profile-applied test mappings are available yet.",
                })}
              </p>
            )}
          </Tile>
        </Column>
      </Grid>

      <Grid className="field-mapping-pending-codes">
        <Column lg={16} md={8} sm={4}>
          <Tile>
            <PendingCodesPanel
              analyzerId={analyzerId}
              pendingCodes={pendingCodes}
              onUpdated={refreshPendingCodes}
            />
          </Tile>
        </Column>
      </Grid>

      <Grid className="field-mapping-result-values">
        <Column lg={16} md={8} sm={4}>
          <Tile>
            <ResultValueMappingsPanel
              analyzerId={analyzerId}
              mappings={resultValueMappings}
              pendingValues={pendingResultValues}
              onUpdated={refreshResultValues}
            />
          </Tile>
        </Column>
      </Grid>

      <Grid className="field-mapping-setup-verification">
        <Column lg={16} md={8} sm={4}>
          <Tile>
            <SetupVerificationPanel analyzerId={analyzerId} />
          </Tile>
        </Column>
      </Grid>
    </div>
  );
};

export default FieldMapping;
