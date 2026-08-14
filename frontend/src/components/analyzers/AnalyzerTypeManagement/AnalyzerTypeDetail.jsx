import React, { useEffect, useMemo, useState } from "react";
import {
  Button,
  Column,
  ComposedModal,
  DataTable,
  Grid,
  InlineNotification,
  Loading,
  ModalBody,
  ModalFooter,
  ModalHeader,
  Tab,
  TabList,
  TabPanel,
  TabPanels,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tabs,
  Tag,
  TextInput,
} from "@carbon/react";
import { Fork, Power, Renew } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import { useHistory, useLocation, useParams } from "react-router-dom";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
import {
  AnalyzerTypeMappingProgress,
  AnalyzerTypeSourceTag,
  AnalyzerTypeStatusTag,
  profileMetadata,
} from "./AnalyzerTypePresentation";
import "./AnalyzerTypeManagement.scss";

const PROFILE_ID_PATTERN = /^[a-z0-9][a-z0-9.-]*$/;

const AnalyzerTypeDetail = () => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const { profileId } = useParams();
  const query = useMemo(
    () => new URLSearchParams(location.search),
    [location.search],
  );
  const revision = query.get("revision");
  const view = query.get("view") === "history" ? "history" : "overview";
  const requestedAction = ["fork", "deactivate", "reactivate"].includes(
    query.get("action"),
  )
    ? query.get("action")
    : null;

  const [profile, setProfile] = useState(null);
  const [historyEntries, setHistoryEntries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [error, setError] = useState(null);
  const [notification, setNotification] = useState(null);
  const [forkOpen, setForkOpen] = useState(false);
  const [lifecycleOpen, setLifecycleOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [forkForm, setForkForm] = useState({ profileId: "", displayName: "" });
  const [forkErrors, setForkErrors] = useState({});

  useEffect(() => {
    const canonical = new URLSearchParams();
    if (revision) canonical.set("revision", revision);
    if (view === "history") canonical.set("view", "history");
    if (requestedAction) canonical.set("action", requestedAction);
    history.replace({
      pathname: location.pathname,
      search: canonical.toString(),
    });
  }, [history, location.pathname, requestedAction, revision, view]);

  useEffect(() => {
    if (!profile || !requestedAction) return;
    if (requestedAction === "fork") {
      setForkOpen(true);
      return;
    }
    const actionMatchesStatus =
      (requestedAction === "deactivate" && profile.status === "ACTIVE") ||
      (requestedAction === "reactivate" && profile.status === "INACTIVE");
    if (actionMatchesStatus) setLifecycleOpen(true);
  }, [profile, requestedAction]);

  useEffect(() => {
    const controller = new AbortController();
    const encodedProfileId = encodeURIComponent(profileId);
    const suffix = revision ? `?revision=${encodeURIComponent(revision)}` : "";
    setLoading(true);
    setError(null);
    getFromOpenElisServer(
      `/rest/analyzer/types/${encodedProfileId}${suffix}`,
      (response) => {
        setLoading(false);
        if (response && !response.error && response.profileId) {
          setProfile(response);
          return;
        }
        setProfile(null);
        setError(
          response?.error ||
            intl.formatMessage({ id: "analyzerType.error.loadDetail" }),
        );
      },
      controller.signal,
    );
    return () => controller.abort();
  }, [intl, profileId, revision]);

  useEffect(() => {
    if (view !== "history") return undefined;
    const controller = new AbortController();
    setHistoryLoading(true);
    getFromOpenElisServer(
      `/rest/analyzer/types/${encodeURIComponent(profileId)}/history`,
      (response) => {
        setHistoryLoading(false);
        if (Array.isArray(response)) {
          setHistoryEntries(response);
        } else {
          setNotification({
            kind: "error",
            title: intl.formatMessage({
              id: "analyzerType.error.historyTitle",
            }),
            subtitle:
              response?.error ||
              intl.formatMessage({ id: "analyzerType.error.loadDetail" }),
          });
        }
      },
      controller.signal,
    );
    return () => controller.abort();
  }, [intl, profileId, view]);

  const selectView = (selectedIndex) => {
    const params = new URLSearchParams();
    if (revision) params.set("revision", revision);
    if (selectedIndex === 1) params.set("view", "history");
    history.replace({ pathname: location.pathname, search: params.toString() });
  };

  const closeAction = () => {
    const params = new URLSearchParams();
    if (revision) params.set("revision", revision);
    if (view === "history") params.set("view", "history");
    history.replace({ pathname: location.pathname, search: params.toString() });
    setForkOpen(false);
    setLifecycleOpen(false);
  };

  const validateFork = () => {
    const nextErrors = {};
    if (!forkForm.displayName.trim()) {
      nextErrors.displayName = intl.formatMessage({
        id: "analyzerType.fork.nameRequired",
      });
    }
    if (!PROFILE_ID_PATTERN.test(forkForm.profileId.trim())) {
      nextErrors.profileId = intl.formatMessage({
        id: "analyzerType.fork.profileIdInvalid",
      });
    }
    setForkErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const submitFork = () => {
    if (!validateFork()) return;
    setSubmitting(true);
    postToOpenElisServerJsonResponse(
      `/rest/analyzer/types/${encodeURIComponent(profile.profileId)}/fork`,
      JSON.stringify({
        sourceRevision: profile.revision,
        profileId: forkForm.profileId.trim(),
        displayName: forkForm.displayName.trim(),
      }),
      (response) => {
        setSubmitting(false);
        if (!response || response.error) {
          setNotification({
            kind: "error",
            title: intl.formatMessage({ id: "analyzerType.fork.error" }),
            subtitle:
              response?.error ||
              intl.formatMessage({ id: "analyzerType.error.loadDetail" }),
          });
          return;
        }
        history.push(
          `/analyzers/types/${encodeURIComponent(
            response.profileId,
          )}?revision=${response.revision}`,
        );
      },
    );
  };

  const submitLifecycleChange = () => {
    const action = profile.status === "ACTIVE" ? "deactivate" : "reactivate";
    setSubmitting(true);
    postToOpenElisServerJsonResponse(
      `/rest/analyzer/types/${encodeURIComponent(profile.profileId)}/${action}`,
      JSON.stringify({}),
      (response) => {
        setSubmitting(false);
        if (!response || response.error) {
          setNotification({
            kind: "error",
            title: intl.formatMessage({
              id: "analyzerType.lifecycle.error",
            }),
            subtitle:
              response?.error ||
              intl.formatMessage({ id: "analyzerType.error.loadDetail" }),
          });
          return;
        }
        setProfile(response);
        setLifecycleOpen(false);
        setNotification({
          kind: "success",
          title: intl.formatMessage({
            id:
              response.status === "ACTIVE"
                ? "analyzerType.lifecycle.reactivated"
                : "analyzerType.lifecycle.deactivated",
          }),
          subtitle: "",
        });
      },
    );
  };

  if (loading) {
    return (
      <div className="analyzer-type-page-loading">
        <Loading withOverlay={false} />
      </div>
    );
  }

  if (error || !profile) {
    return (
      <Grid fullWidth className="analyzer-type-catalog">
        <Column lg={16} md={8} sm={4}>
          <InlineNotification
            kind="error"
            hideCloseButton
            title={intl.formatMessage({ id: "analyzerType.error.loadTitle" })}
            subtitle={error}
          />
        </Column>
      </Grid>
    );
  }

  const historyHeaders = [
    {
      key: "revision",
      header: intl.formatMessage({ id: "analyzerType.history.revision" }),
    },
    {
      key: "action",
      header: intl.formatMessage({ id: "analyzerType.history.change" }),
    },
    {
      key: "actor",
      header: intl.formatMessage({ id: "analyzerType.history.actor" }),
    },
    {
      key: "markedAt",
      header: intl.formatMessage({ id: "analyzerType.history.markedAt" }),
    },
  ];
  const historyRows = historyEntries.map((entry) => ({
    id: String(entry.profile.revision),
    revision: String(entry.profile.revision),
    action: intl.formatMessage({
      id: `analyzerType.history.action.${entry.audit.action.toLowerCase()}`,
    }),
    actor: entry.audit.actor,
    markedAt: `${intl.formatDate(entry.audit.markedAt)} ${intl.formatTime(
      entry.audit.markedAt,
    )}`,
  }));

  return (
    <>
      <PageBreadCrumb
        breadcrumbs={[
          { label: "home.label", link: "/" },
          { label: "analyzer.page.hierarchy.root", link: "/analyzers" },
          { label: "analyzerType.page.title", link: "/analyzers/types" },
          { label: profile.displayName, isCurrentPage: true },
        ]}
      />
      <Grid fullWidth className="analyzer-type-catalog analyzer-type-detail">
        <Column lg={16} md={8} sm={4}>
          <div className="analyzer-type-detail__heading">
            <div>
              <h1 className="analyzer-type-heading">{profile.displayName}</h1>
              <div className="analyzer-type-detail__tags">
                <AnalyzerTypeSourceTag source={profile.source} />
                <AnalyzerTypeStatusTag status={profile.status} />
                <Tag type="cool-gray">
                  {intl.formatMessage(
                    { id: "analyzerType.revision" },
                    { revision: profile.revision },
                  )}
                </Tag>
              </div>
            </div>
            <div className="analyzer-type-detail__actions">
              <Button
                kind="tertiary"
                renderIcon={Fork}
                onClick={() => setForkOpen(true)}
              >
                {intl.formatMessage({ id: "analyzerType.action.fork" })}
              </Button>
              <Button
                kind={
                  profile.status === "ACTIVE" ? "danger--tertiary" : "tertiary"
                }
                renderIcon={profile.status === "ACTIVE" ? Power : Renew}
                onClick={() => setLifecycleOpen(true)}
              >
                {intl.formatMessage({
                  id:
                    profile.status === "ACTIVE"
                      ? "analyzerType.action.deactivate"
                      : "analyzerType.action.reactivate",
                })}
              </Button>
            </div>
          </div>

          {notification && (
            <InlineNotification
              kind={notification.kind}
              title={notification.title}
              subtitle={notification.subtitle}
              onCloseButtonClick={() => setNotification(null)}
            />
          )}

          <Tabs
            selectedIndex={view === "history" ? 1 : 0}
            onChange={({ selectedIndex }) => selectView(selectedIndex)}
          >
            <TabList
              aria-label={intl.formatMessage({
                id: "analyzerType.detail.views",
              })}
              contained
            >
              <Tab>
                {intl.formatMessage({ id: "analyzerType.detail.overview" })}
              </Tab>
              <Tab>
                {intl.formatMessage({ id: "analyzerType.detail.history" })}
              </Tab>
            </TabList>
            <TabPanels>
              <TabPanel>
                <div className="analyzer-type-detail__summary">
                  <div>
                    <span>
                      {intl.formatMessage({
                        id: "analyzerType.detail.instrument",
                      })}
                    </span>
                    <strong>{profileMetadata(profile)}</strong>
                  </div>
                  <div>
                    <span>
                      {intl.formatMessage({
                        id: "analyzerType.column.protocol",
                      })}
                    </span>
                    <strong>{profile.protocol}</strong>
                  </div>
                  <div>
                    <span>
                      {intl.formatMessage({
                        id: "analyzerType.detail.category",
                      })}
                    </span>
                    <strong>{profile.category}</strong>
                  </div>
                  <div>
                    <span>
                      {intl.formatMessage({ id: "analyzerType.column.usedBy" })}
                    </span>
                    <strong>
                      {intl.formatMessage(
                        { id: "analyzerType.usedBy" },
                        { count: profile.analyzerCount },
                      )}
                    </strong>
                  </div>
                  <div>
                    <span>
                      {intl.formatMessage({
                        id: "analyzerType.column.testsMapped",
                      })}
                    </span>
                    <strong>
                      <AnalyzerTypeMappingProgress
                        mapping={profile.testMappings}
                      />
                    </strong>
                  </div>
                  <div>
                    <span>
                      {intl.formatMessage({
                        id: "analyzerType.column.resultsMapped",
                      })}
                    </span>
                    <strong>
                      <AnalyzerTypeMappingProgress
                        mapping={profile.resultValueMappings}
                      />
                    </strong>
                  </div>
                  <div>
                    <span>
                      {intl.formatMessage({
                        id: "analyzerType.detail.qcIdentification",
                      })}
                    </span>
                    <strong>
                      {intl.formatMessage(
                        { id: "analyzerType.detail.rules" },
                        { count: profile.qcIdentificationRuleCount },
                      )}
                    </strong>
                  </div>
                  <div>
                    <span>
                      {intl.formatMessage({
                        id: "analyzerType.detail.connectionTest",
                      })}
                    </span>
                    <strong>
                      {intl.formatMessage({
                        id: profile.connectionTestSupported
                          ? "analyzerType.detail.supported"
                          : "analyzerType.detail.notSupported",
                      })}
                    </strong>
                  </div>
                </div>

                {profile.parentProfileId && (
                  <div className="analyzer-type-detail__lineage">
                    <span>
                      {intl.formatMessage({
                        id: "analyzerType.detail.forkedFrom",
                      })}
                    </span>
                    <strong>
                      {profile.parentProfileId}
                      {" \u00b7 "}
                      {intl.formatMessage(
                        { id: "analyzerType.revision" },
                        { revision: profile.parentRevision },
                      )}
                    </strong>
                  </div>
                )}

                {profile.attentionCodes.length > 0 && (
                  <InlineNotification
                    kind="warning"
                    lowContrast
                    hideCloseButton
                    title={intl.formatMessage({
                      id: "analyzerType.attention.title",
                    })}
                    subtitle={profile.attentionCodes
                      .map((code) =>
                        intl.formatMessage({
                          id: `analyzerType.attention.${code.toLowerCase()}`,
                        }),
                      )
                      .join("; ")}
                  />
                )}
              </TabPanel>
              <TabPanel>
                {historyLoading ? (
                  <div className="analyzer-type-loading">
                    <Loading withOverlay={false} small />
                  </div>
                ) : (
                  <DataTable
                    rows={historyRows}
                    headers={historyHeaders}
                    size="md"
                  >
                    {({
                      rows,
                      headers,
                      getHeaderProps,
                      getRowProps,
                      getTableProps,
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
                              <TableRow key={row.id} {...getRowProps({ row })}>
                                {row.cells.map((cell) => (
                                  <TableCell key={cell.id}>
                                    {cell.value}
                                  </TableCell>
                                ))}
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
                      </TableContainer>
                    )}
                  </DataTable>
                )}
              </TabPanel>
            </TabPanels>
          </Tabs>
        </Column>
      </Grid>

      <ComposedModal open={forkOpen} onClose={closeAction}>
        <ModalHeader
          title={intl.formatMessage({ id: "analyzerType.fork.title" })}
        />
        <ModalBody>
          <TextInput
            id="analyzer-type-fork-name"
            labelText={intl.formatMessage({ id: "analyzerType.fork.name" })}
            value={forkForm.displayName}
            invalid={Boolean(forkErrors.displayName)}
            invalidText={forkErrors.displayName}
            onChange={(event) =>
              setForkForm({ ...forkForm, displayName: event.target.value })
            }
          />
          <TextInput
            id="analyzer-type-fork-id"
            labelText={intl.formatMessage({
              id: "analyzerType.fork.profileId",
            })}
            value={forkForm.profileId}
            invalid={Boolean(forkErrors.profileId)}
            invalidText={forkErrors.profileId}
            onChange={(event) =>
              setForkForm({ ...forkForm, profileId: event.target.value })
            }
          />
        </ModalBody>
        <ModalFooter
          secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
          primaryButtonText={intl.formatMessage({
            id: "analyzerType.fork.submit",
          })}
          primaryButtonDisabled={submitting}
          onRequestSubmit={submitFork}
        />
      </ComposedModal>

      <ComposedModal open={lifecycleOpen} onClose={closeAction}>
        <ModalHeader
          title={intl.formatMessage({
            id:
              profile.status === "ACTIVE"
                ? "analyzerType.lifecycle.deactivateTitle"
                : "analyzerType.lifecycle.reactivateTitle",
          })}
        />
        <ModalBody>
          <p>
            {intl.formatMessage(
              {
                id:
                  profile.status === "ACTIVE"
                    ? "analyzerType.lifecycle.deactivateDetail"
                    : "analyzerType.lifecycle.reactivateDetail",
              },
              { count: profile.analyzerCount },
            )}
          </p>
        </ModalBody>
        <ModalFooter
          secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
          primaryButtonText={intl.formatMessage({
            id:
              profile.status === "ACTIVE"
                ? "analyzerType.action.deactivate"
                : "analyzerType.action.reactivate",
          })}
          primaryButtonDisabled={submitting}
          danger={profile.status === "ACTIVE"}
          onRequestSubmit={submitLifecycleChange}
        />
      </ComposedModal>
    </>
  );
};

export default AnalyzerTypeDetail;
