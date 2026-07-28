import React, { useEffect, useMemo, useState } from "react";
import {
  Button,
  Column,
  Dropdown,
  Grid,
  InlineNotification,
  Loading,
  Search,
  Tag,
} from "@carbon/react";
import { ArrowRight } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { useHistory, useLocation } from "react-router-dom";
import { getFromOpenElisServer } from "../../utils/Utils";
import PageHeader from "../../common/PageHeader/PageHeader";
import AnalyzerConfigTable from "../AnalyzerConfigTable/AnalyzerConfigTable";
import {
  buildAnalyzerSetupUrl,
  buildProfileCatalogUrl,
  parseProfileCatalogQuery,
} from "../analyzerRoutes";
import "./AnalyzerTypeManagement.css";

const profileDomId = (id) => String(id || "").replace(/[^a-zA-Z0-9_-]/g, "-");

const readinessTagType = (status) => {
  if (status === "READY") {
    return "green";
  }
  if (status === "PENDING") {
    return "blue";
  }
  return "gray";
};

const profileReadinessStates = ["", "DRAFT", "PENDING", "READY"];
const profileCategoryMessageIds = new Set([
  "analyzer.type.chemistry",
  "analyzer.type.coagulation",
  "analyzer.type.hematology",
  "analyzer.type.immunology",
  "analyzer.type.microbiology",
  "analyzer.type.molecular",
]);

const AnalyzerTypeManagement = () => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();

  const [profiles, setProfiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [notification, setNotification] = useState(null);
  const routeState = useMemo(
    () => parseProfileCatalogQuery(location.search),
    [location.search],
  );

  useEffect(() => {
    getFromOpenElisServer("/rest/analyzer/profiles", (data) => {
      setLoading(false);
      if (Array.isArray(data)) {
        setProfiles(data);
      } else {
        setProfiles([]);
        setNotification({
          kind: "error",
          title: intl.formatMessage({
            id: "analyzerType.notification.profileLoadError",
          }),
          subtitle: data?.error || "",
        });
      }
    });
  }, [intl]);

  const filteredProfiles = useMemo(() => {
    const term = routeState.search.trim().toLowerCase();
    return profiles.filter((profile) => {
      const matchesSearch =
        !term ||
        [
          profile.displayName,
          profile.analyzerName,
          profile.id,
          profile.protocol,
          profile.category,
          profile.manufacturer,
          profile.readinessStatus,
        ]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(term));
      const matchesProtocol =
        !routeState.protocol || profile.protocol === routeState.protocol;
      const matchesReadiness =
        !routeState.readiness ||
        profile.readinessStatus === routeState.readiness;
      return matchesSearch && matchesProtocol && matchesReadiness;
    });
  }, [profiles, routeState]);

  const headers = [
    {
      key: "displayName",
      header: intl.formatMessage({ id: "analyzerType.column.profile" }),
    },
    {
      key: "protocol",
      header: intl.formatMessage({ id: "analyzerType.column.protocol" }),
    },
    {
      key: "category",
      header: intl.formatMessage({ id: "analyzerType.column.category" }),
    },
    {
      key: "supportedConnectionMode",
      header: intl.formatMessage({ id: "analyzerType.column.connectionMode" }),
    },
    {
      key: "testMappingCount",
      header: intl.formatMessage({ id: "analyzerType.column.testMappings" }),
    },
    {
      key: "qcRuleCount",
      header: intl.formatMessage({ id: "analyzerType.column.qcRules" }),
    },
    {
      key: "resultValueMappingCount",
      header: intl.formatMessage({ id: "analyzerType.column.resultValues" }),
    },
    {
      key: "readinessStatus",
      header: intl.formatMessage({ id: "analyzerType.column.readiness" }),
    },
    {
      key: "actions",
      header: intl.formatMessage({ id: "analyzerType.column.actions" }),
    },
  ];

  const rows = filteredProfiles.map((profile) => ({
    id: profile.id,
    displayName: profile.displayName || profile.analyzerName || profile.id,
    protocol: profile.protocol || "",
    category: profile.category || "",
    supportedConnectionMode: profile.supportedConnectionMode || "",
    testMappingCount: profile.testMappingCount ?? 0,
    qcRuleCount: profile.qcRuleCount ?? 0,
    resultValueMappingCount: profile.resultValueMappingCount ?? 0,
    readinessStatus: profile.readinessStatus || "DRAFT",
    actions: profile.id,
  }));

  const handleSetup = (profileId) => {
    history.push(
      buildAnalyzerSetupUrl("instrument", {
        profileId,
        returnTo: `${location.pathname}${location.search}`,
      }),
    );
  };

  const updateRouteState = (changes) => {
    history.replace(buildProfileCatalogUrl({ ...routeState, ...changes }));
  };

  const readinessLabel = (status) =>
    status
      ? intl.formatMessage({
          id: `analyzerType.readiness.${status.toLowerCase()}`,
        })
      : intl.formatMessage({ id: "analyzerType.filter.allReadiness" });

  const categoryLabel = (category) => {
    const messageId = `analyzer.type.${String(category || "").toLowerCase()}`;
    return profileCategoryMessageIds.has(messageId)
      ? intl.formatMessage({ id: messageId })
      : category;
  };

  const renderProfileCell = (cell, row) => {
    const domId = profileDomId(row.id);
    if (cell.info.header === "readinessStatus") {
      return (
        <Tag type={readinessTagType(cell.value)}>
          {readinessLabel(cell.value)}
        </Tag>
      );
    }
    if (cell.info.header === "category") {
      return categoryLabel(cell.value);
    }
    if (cell.info.header === "testMappingCount") {
      return (
        <span data-testid={`profile-test-mapping-count-${domId}`}>
          {cell.value}
        </span>
      );
    }
    if (cell.info.header === "qcRuleCount") {
      return (
        <span data-testid={`profile-qc-rule-count-${domId}`}>{cell.value}</span>
      );
    }
    if (cell.info.header === "actions") {
      return (
        <Button
          kind="ghost"
          size="sm"
          renderIcon={ArrowRight}
          data-testid={`profile-setup-${domId}`}
          onClick={() => handleSetup(cell.value)}
        >
          <FormattedMessage id="analyzerType.button.setup" />
        </Button>
      );
    }
    return cell.value;
  };

  return (
    <Grid fullWidth>
      <Column lg={16} md={8} sm={4}>
        <PageHeader
          breadcrumbs={[
            {
              label: intl.formatMessage({
                id: "analyzer.page.hierarchy.list",
              }),
              link: "/analyzers",
            },
            {
              label: intl.formatMessage({ id: "analyzerType.page.title" }),
            },
          ]}
        />

        {notification && (
          <InlineNotification
            kind={notification.kind}
            title={notification.title}
            subtitle={notification.subtitle}
            onCloseButtonClick={() => setNotification(null)}
            style={{ marginBottom: "1rem" }}
          />
        )}

        <Grid narrow className="analyzer-type-filters">
          <Column lg={8} md={4} sm={4}>
            <Search
              size="lg"
              placeholder={intl.formatMessage({
                id: "analyzerType.search.placeholder",
              })}
              labelText={intl.formatMessage({
                id: "analyzerType.search.label",
              })}
              value={routeState.search}
              onChange={(event) =>
                updateRouteState({ search: event.target.value })
              }
            />
          </Column>
          <Column lg={4} md={2} sm={4}>
            <Dropdown
              id="analyzer-type-protocol-filter"
              titleText={intl.formatMessage({
                id: "analyzerType.column.protocol",
              })}
              label={intl.formatMessage({
                id: "analyzerType.filter.allProtocols",
              })}
              items={["", "ASTM", "HL7", "FILE"]}
              selectedItem={routeState.protocol}
              itemToString={(item) =>
                item ||
                intl.formatMessage({
                  id: "analyzerType.filter.allProtocols",
                })
              }
              onChange={({ selectedItem }) =>
                updateRouteState({ protocol: selectedItem || "" })
              }
            />
          </Column>
          <Column lg={4} md={2} sm={4}>
            <Dropdown
              id="analyzer-type-readiness-filter"
              titleText={intl.formatMessage({
                id: "analyzerType.column.readiness",
              })}
              label={intl.formatMessage({
                id: "analyzerType.filter.allReadiness",
              })}
              items={profileReadinessStates}
              selectedItem={routeState.readiness}
              itemToString={readinessLabel}
              onChange={({ selectedItem }) =>
                updateRouteState({ readiness: selectedItem || "" })
              }
            />
          </Column>
        </Grid>

        {loading ? (
          <Loading withOverlay={false} />
        ) : (
          <AnalyzerConfigTable
            headers={headers}
            rows={rows}
            tableLabel={intl.formatMessage({ id: "analyzerType.page.title" })}
            testId="analyzer-profile-table"
            getRowTestId={(row) => `profile-row-${profileDomId(row.id)}`}
            renderCell={renderProfileCell}
          />
        )}
      </Column>
    </Grid>
  );
};

export default AnalyzerTypeManagement;
