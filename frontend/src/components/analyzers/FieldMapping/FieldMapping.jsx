import React, { useEffect, useState } from "react";
import { Button, Column, Grid, InlineNotification, Tile } from "@carbon/react";
import { ArrowRight } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import { useHistory, useLocation, useParams } from "react-router-dom";
import * as analyzerService from "../../../services/analyzerService";
import PageHeader from "../../common/PageHeader/PageHeader";
import AnalyzerConfigTable from "../AnalyzerConfigTable/AnalyzerConfigTable";
import AnalyzerSetupProgress from "../AnalyzerSetupProgress/AnalyzerSetupProgress";
import {
  buildAnalyzerSetupUrl,
  resolveAnalyzerReturnTo,
} from "../analyzerRoutes";
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
  const location = useLocation();
  const { id: analyzerId } = useParams();
  const setupParams = new URLSearchParams(location.search || "");
  const isGuidedSetup =
    setupParams.get("setup") === "1" && setupParams.get("step") === "verify";
  const setupProfileId = setupParams.get("profile") || undefined;
  const setupReturnTo = resolveAnalyzerReturnTo(setupParams.get("returnTo"));
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
  const profileMappingHeaders = [
    {
      key: "analyzerCode",
      header: intl.formatMessage({
        id: "analyzer.fieldMapping.profileApplied.analyzerCode",
      }),
    },
    {
      key: "openelisTest",
      header: intl.formatMessage({
        id: "analyzer.fieldMapping.profileApplied.openelisTest",
      }),
    },
    {
      key: "loinc",
      header: intl.formatMessage({
        id: "analyzer.fieldMapping.profileApplied.loinc",
      }),
    },
    {
      key: "status",
      header: intl.formatMessage({
        id: "analyzer.fieldMapping.profileApplied.status",
      }),
    },
  ];

  return (
    <div className="field-mapping" data-testid="field-mapping">
      <div className="field-mapping-header">
        <div className="field-mapping-header-title">
          <PageHeader
            breadcrumbs={[
              {
                label: intl.formatMessage({
                  id: "analyzer.page.hierarchy.root",
                }),
                link: "/analyzers",
              },
              {
                label:
                  analyzer?.name ||
                  intl.formatMessage({
                    id: "analyzer.fieldMapping.page.title",
                  }),
                link: `/analyzers/${analyzerId}/edit`,
              },
              {
                label: isGuidedSetup
                  ? intl.formatMessage({ id: "analyzer.setup.step.verify" })
                  : intl.formatMessage({
                      id: "analyzer.page.hierarchy.mappings",
                    }),
              },
            ]}
            showBackArrow
            onBack={() => history.push(setupReturnTo)}
            subtitle={intl.formatMessage({
              id: "analyzer.fieldMapping.page.subtitle",
            })}
          />
          {isGuidedSetup && <AnalyzerSetupProgress currentStep="verify" />}
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
              <AnalyzerConfigTable
                headers={profileMappingHeaders}
                rows={reviewMappings}
                tableLabel={intl.formatMessage({
                  id: "analyzer.fieldMapping.profileApplied.title",
                })}
                testId="profile-applied-mappings-table"
                getRowTestId={(row) => `profile-applied-mapping-row-${row.id}`}
                renderCell={(cell) =>
                  cell.info.header === "status"
                    ? intl.formatMessage({
                        id: `analyzer.fieldMapping.profileApplied.status.${String(
                          cell.value,
                        ).toLowerCase()}`,
                        defaultMessage: cell.value,
                      })
                    : cell.value
                }
              />
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

      {isGuidedSetup && (
        <div className="field-mapping-setup-actions">
          <Button
            kind="primary"
            renderIcon={ArrowRight}
            data-testid="analyzer-setup-verify-continue"
            onClick={() =>
              history.push(
                buildAnalyzerSetupUrl("connect", {
                  analyzerId,
                  profileId: setupProfileId,
                  returnTo: setupReturnTo,
                }),
              )
            }
          >
            {intl.formatMessage({ id: "analyzer.setup.action.continue" })}
          </Button>
        </div>
      )}
    </div>
  );
};

export default FieldMapping;
