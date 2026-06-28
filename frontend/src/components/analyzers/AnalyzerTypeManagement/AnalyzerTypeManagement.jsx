import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  Button,
  Column,
  DataTable,
  Grid,
  InlineNotification,
  Loading,
  Search,
  Tag,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
} from "@carbon/react";
import { Launch } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { useHistory } from "react-router-dom";
import { getFromOpenElisServer } from "../../utils/Utils";

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

const AnalyzerTypeManagement = () => {
  const intl = useIntl();
  const history = useHistory();

  const [profiles, setProfiles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [notification, setNotification] = useState(null);

  const loadProfiles = useCallback(() => {
    setLoading(true);
    setNotification(null);
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

  useEffect(() => {
    loadProfiles();
  }, [loadProfiles]);

  const filteredProfiles = useMemo(() => {
    const term = searchTerm.trim().toLowerCase();
    if (!term) {
      return profiles;
    }
    return profiles.filter((profile) =>
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
        .some((value) => String(value).toLowerCase().includes(term)),
    );
  }, [profiles, searchTerm]);

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
    history.push(`/analyzers?add=1&profile=${encodeURIComponent(profileId)}`);
  };

  return (
    <Grid fullWidth>
      <Column lg={16} md={8} sm={4}>
        <h2>
          <FormattedMessage id="analyzerType.page.title" />
        </h2>

        {notification && (
          <InlineNotification
            kind={notification.kind}
            title={notification.title}
            subtitle={notification.subtitle}
            onCloseButtonClick={() => setNotification(null)}
            style={{ marginBottom: "1rem" }}
          />
        )}

        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: "1rem",
          }}
        >
          <Search
            size="lg"
            placeholder={intl.formatMessage({
              id: "analyzerType.search.placeholder",
            })}
            labelText={intl.formatMessage({ id: "analyzerType.search.label" })}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={{ maxWidth: "400px" }}
          />
        </div>

        {loading ? (
          <Loading withOverlay={false} />
        ) : (
          <DataTable rows={rows} headers={headers}>
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
                    {rows.map((row) => (
                      <TableRow
                        key={row.id}
                        {...getRowProps({ row })}
                        data-testid={`profile-row-${profileDomId(row.id)}`}
                      >
                        {row.cells.map((cell) => {
                          const domId = profileDomId(row.id);
                          if (cell.info.header === "readinessStatus") {
                            return (
                              <TableCell key={cell.id}>
                                <Tag type={readinessTagType(cell.value)}>
                                  {cell.value}
                                </Tag>
                              </TableCell>
                            );
                          }
                          if (cell.info.header === "testMappingCount") {
                            return (
                              <TableCell
                                key={cell.id}
                                data-testid={`profile-test-mapping-count-${domId}`}
                              >
                                {cell.value}
                              </TableCell>
                            );
                          }
                          if (cell.info.header === "qcRuleCount") {
                            return (
                              <TableCell
                                key={cell.id}
                                data-testid={`profile-qc-rule-count-${domId}`}
                              >
                                {cell.value}
                              </TableCell>
                            );
                          }
                          if (cell.info.header === "actions") {
                            return (
                              <TableCell key={cell.id}>
                                <Button
                                  kind="ghost"
                                  size="sm"
                                  renderIcon={Launch}
                                  data-testid={`profile-setup-${domId}`}
                                  onClick={() => handleSetup(cell.value)}
                                >
                                  <FormattedMessage id="analyzerType.button.setup" />
                                </Button>
                              </TableCell>
                            );
                          }
                          return (
                            <TableCell key={cell.id}>{cell.value}</TableCell>
                          );
                        })}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
        )}
      </Column>
    </Grid>
  );
};

export default AnalyzerTypeManagement;
