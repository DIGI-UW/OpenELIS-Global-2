import React, { useContext, useEffect, useState } from "react";
import {
  Button,
  Checkbox,
  Column,
  FileUploaderDropContainer,
  Grid,
  Heading,
  InlineNotification,
  Section,
  Select,
  SelectItem,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import {
  getFromOpenElisServer,
  postToOpenElisServerFormDataJsonResponse,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  { label: "catalog.import.title", link: "/MasterListsPage/CatalogImport" },
];

/** The domain a file name starts with; the longest match wins. */
const inferDomain = (fileName, domains) =>
  domains
    .filter((domain) => fileName.toLowerCase().startsWith(domain))
    .sort((a, b) => b.length - a.length)[0] || "";

/**
 * Admin → Import Catalog (CSV): drop the catalog files, see what they would do
 * (dry run, nothing kept), apply them, and settle the names the import could
 * not resolve. Applying keeps the files in the configuration tree, so the next
 * start-up loads exactly what was applied.
 */
const CatalogImport = () => {
  const intl = useIntl();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const [domains, setDomains] = useState([]);
  const [staged, setStaged] = useState([]);
  const [plan, setPlan] = useState(null);
  const [applied, setApplied] = useState(false);
  const [failure, setFailure] = useState(null);
  const [busy, setBusy] = useState(false);
  const [unresolved, setUnresolved] = useState([]);
  const [candidates, setCandidates] = useState({});
  const [decisions, setDecisions] = useState({});

  useEffect(() => {
    getFromOpenElisServer("/rest/configuration/import/domains", (res) => {
      setDomains(Array.isArray(res) ? res : []);
    });
  }, []);

  const loadUnresolved = () => {
    getFromOpenElisServer("/rest/configuration/unresolved", (res) => {
      const items = Array.isArray(res) ? res : [];
      setUnresolved(items);
      [...new Set(items.map((item) => item.referenceType))].forEach((type) => {
        getFromOpenElisServer(
          `/rest/configuration/unresolved/candidates?referenceType=${encodeURIComponent(type)}`,
          (options) => {
            setCandidates((previous) => ({
              ...previous,
              [type]: Array.isArray(options) ? options : [],
            }));
          },
        );
      });
    });
  };

  useEffect(loadUnresolved, []);

  const addFiles = (addedFiles) => {
    setPlan(null);
    setApplied(false);
    setFailure(null);
    setStaged((previous) => [
      ...previous,
      ...addedFiles.map((file) => ({
        file,
        name: file.name,
        domain: inferDomain(file.name, domains),
      })),
    ]);
  };

  const removeFile = (name) => {
    setPlan(null);
    setApplied(false);
    setFailure(null);
    setStaged((previous) => previous.filter((entry) => entry.name !== name));
  };

  const setDomainOf = (name, domain) => {
    setStaged((previous) =>
      previous.map((entry) =>
        entry.name === name ? { ...entry, domain } : entry,
      ),
    );
  };

  const notify = (kind, messageId) => {
    addNotification({
      kind,
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage({ id: messageId }),
    });
    setNotificationVisible(true);
  };

  /**
   * The server's own reason for a refused batch: the file errors of a 422, the
   * status text of anything else.
   */
  const reasonOf = (response) => {
    const errors = (response?.files || [])
      .map((file) => file.error)
      .filter(Boolean);
    if (errors.length > 0) {
      return errors.join("; ");
    }
    if (response?.error) {
      return response.error;
    }
    return response?.status ? `HTTP ${response.status}` : "";
  };

  /**
   * Sends the staged files. A response without a file list, or with an error
   * status, is a refused batch: nothing was kept, so a preview shows no plan and
   * an apply keeps the preview it had, and the reason is shown on the page.
   */
  const send = (endpoint, isApply) => {
    const formData = new FormData();
    staged.forEach((entry) => {
      formData.append("files", entry.file, entry.name);
      formData.append("domains", entry.domain || "");
    });
    setBusy(true);
    setFailure(null);
    postToOpenElisServerFormDataJsonResponse(endpoint, formData, (response) => {
      setBusy(false);
      const refused =
        !response || response.status >= 400 || !Array.isArray(response.files);
      if (refused) {
        const messageId = isApply
          ? "catalog.import.apply.failed"
          : "catalog.import.failed";
        if (!isApply) {
          setPlan(null);
        }
        setFailure({
          title: intl.formatMessage({ id: messageId }),
          reason: reasonOf(response),
        });
        notify(NotificationKinds.error, messageId);
        return;
      }
      setPlan(response);
      setApplied(isApply);
      loadUnresolved();
      if (isApply) {
        const partial = response.files.some((file) => file.error);
        notify(
          partial ? NotificationKinds.error : NotificationKinds.success,
          partial ? "catalog.import.applied.partial" : "catalog.import.applied",
        );
      }
    });
  };

  const resolve = (item, action) => {
    const decision = decisions[item.id] || {};
    const body = JSON.stringify({
      action,
      targetId: decision.targetId || null,
    });
    postToOpenElisServerJsonResponse(
      `/rest/configuration/unresolved/${item.id}/resolve`,
      body,
      (response) => {
        if (response && response.error) {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.title" }),
            message: response.error,
          });
          setNotificationVisible(true);
          return;
        }
        loadUnresolved();
      },
    );
  };

  const canSend = staged.length > 0 && !busy;
  const hasErrors = (plan?.files || []).some((file) => file.error);

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                <FormattedMessage id="catalog.import.title" />
              </Heading>
            </Section>
          </Column>
        </Grid>
        <div className="orderLegendBody">
          <Grid>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <FormattedMessage id="catalog.import.description" />
              </Section>
              <br />
            </Column>

            <Column lg={16} md={8} sm={4}>
              <FileUploaderDropContainer
                accept={[".csv"]}
                multiple
                labelText={intl.formatMessage({
                  id: "catalog.import.dropzone",
                })}
                onAddFiles={(_event, { addedFiles }) => addFiles(addedFiles)}
              />
              <br />
            </Column>

            {staged.length > 0 && (
              <Column lg={16} md={8} sm={4}>
                <Table size="sm" data-testid="catalog-import-files">
                  <TableHead>
                    <TableRow>
                      <TableHeader>
                        <FormattedMessage id="catalog.import.column.file" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="catalog.import.column.domain" />
                      </TableHeader>
                      <TableHeader />
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {staged.map((entry) => (
                      <TableRow key={entry.name}>
                        <TableCell>{entry.name}</TableCell>
                        <TableCell>
                          <Select
                            id={`domain-${entry.name}`}
                            labelText={intl.formatMessage({
                              id: "catalog.import.column.domain",
                            })}
                            hideLabel
                            value={entry.domain}
                            onChange={(e) =>
                              setDomainOf(entry.name, e.target.value)
                            }
                          >
                            <SelectItem value="" text="--" />
                            {domains.map((domain) => (
                              <SelectItem
                                key={domain}
                                value={domain}
                                text={domain}
                              />
                            ))}
                          </Select>
                        </TableCell>
                        <TableCell>
                          <Button
                            kind="ghost"
                            size="sm"
                            onClick={() => removeFile(entry.name)}
                          >
                            <FormattedMessage id="catalog.import.remove" />
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
                <br />
              </Column>
            )}

            <Column lg={16} md={8} sm={4}>
              <Button
                disabled={!canSend}
                onClick={() =>
                  send("/rest/configuration/import/preview", false)
                }
              >
                <FormattedMessage id="catalog.import.preview" />
              </Button>
              &nbsp;
              <Button
                kind="secondary"
                disabled={!canSend || !plan || hasErrors}
                onClick={() => send("/rest/configuration/import/apply", true)}
              >
                <FormattedMessage id="catalog.import.apply" />
              </Button>
              {staged.length > 0 && !plan && (
                <p data-testid="catalog-import-apply-hint">
                  <FormattedMessage id="catalog.import.apply.hint" />
                </p>
              )}
              <br />
              <br />
            </Column>

            {failure && (
              <Column lg={16} md={8} sm={4}>
                <InlineNotification
                  kind="error"
                  lowContrast
                  hideCloseButton
                  title={failure.title}
                  subtitle={failure.reason}
                  data-testid="catalog-import-failure"
                />
                <br />
              </Column>
            )}

            {plan && (
              <Column lg={16} md={8} sm={4}>
                <Section>
                  <Heading>
                    <FormattedMessage
                      id={
                        applied
                          ? "catalog.import.result.title"
                          : "catalog.import.plan.title"
                      }
                    />
                  </Heading>
                </Section>
                <Table size="sm" data-testid="catalog-import-plan">
                  <TableHead>
                    <TableRow>
                      <TableHeader>
                        <FormattedMessage id="catalog.import.column.file" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="catalog.import.column.created" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="catalog.import.column.updated" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="catalog.import.column.skipped" />
                      </TableHeader>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {(plan.files || []).map((file, index) => (
                      <TableRow key={`${file.fileName}-${index}`}>
                        <TableCell>
                          {file.fileName}
                          {file.error && (
                            <InlineNotification
                              kind="error"
                              lowContrast
                              hideCloseButton
                              title={intl.formatMessage({
                                id: "catalog.import.file.error",
                              })}
                              subtitle={file.error}
                            />
                          )}
                        </TableCell>
                        <TableCell>{file.created}</TableCell>
                        <TableCell>{file.updated}</TableCell>
                        <TableCell>{file.skipped}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
                <br />
                {(plan.files || [])
                  .flatMap((file) =>
                    (file.rows || [])
                      .filter((row) => row.outcome === "SKIPPED")
                      .map((row) => ({ file: file.fileName, ...row })),
                  )
                  .map((row) => (
                    <div key={`${row.file}-${row.lineNumber}`}>
                      <Tag type="red" size="sm">
                        {row.file}:{row.lineNumber}
                      </Tag>
                      {row.reason}
                    </div>
                  ))}
                <br />
              </Column>
            )}

            <Column lg={16} md={8} sm={4}>
              <Section>
                <Heading>
                  <FormattedMessage id="catalog.import.unresolved.title" />
                </Heading>
              </Section>
              <Section>
                <FormattedMessage id="catalog.import.unresolved.description" />
              </Section>
              <br />
              {unresolved.length === 0 ? (
                <p data-testid="catalog-import-unresolved-empty">
                  <FormattedMessage id="catalog.import.unresolved.empty" />
                </p>
              ) : (
                <Table size="sm" data-testid="catalog-import-unresolved">
                  <TableHead>
                    <TableRow>
                      <TableHeader>
                        <FormattedMessage id="catalog.import.column.name" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="catalog.import.column.where" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="catalog.import.column.use" />
                      </TableHeader>
                      <TableHeader />
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {unresolved.map((item) => (
                      <TableRow key={item.id}>
                        <TableCell>
                          <Tag type="warm-gray" size="sm">
                            {item.referenceType}
                          </Tag>
                          {item.referenceValue}
                        </TableCell>
                        <TableCell>{item.context}</TableCell>
                        <TableCell>
                          <Select
                            id={`target-${item.id}`}
                            labelText={intl.formatMessage({
                              id: "catalog.import.column.use",
                            })}
                            hideLabel
                            value={decisions[item.id]?.targetId || ""}
                            onChange={(e) =>
                              setDecisions((previous) => ({
                                ...previous,
                                [item.id]: {
                                  ...previous[item.id],
                                  targetId: e.target.value,
                                },
                              }))
                            }
                          >
                            <SelectItem value="" text="--" />
                            {(candidates[item.referenceType] || []).map(
                              (candidate) => (
                                <SelectItem
                                  key={candidate.id}
                                  value={candidate.id}
                                  text={candidate.name}
                                />
                              ),
                            )}
                          </Select>
                          <Checkbox
                            id={`remember-${item.id}`}
                            labelText={intl.formatMessage({
                              id: "catalog.import.remember",
                            })}
                            checked={decisions[item.id]?.remember || false}
                            onChange={(_e, { checked }) =>
                              setDecisions((previous) => ({
                                ...previous,
                                [item.id]: {
                                  ...previous[item.id],
                                  remember: checked,
                                },
                              }))
                            }
                          />
                        </TableCell>
                        <TableCell>
                          <Button
                            size="sm"
                            disabled={!decisions[item.id]?.targetId}
                            onClick={() =>
                              resolve(
                                item,
                                decisions[item.id]?.remember
                                  ? "ALIAS"
                                  : "USE_EXISTING",
                              )
                            }
                          >
                            <FormattedMessage id="catalog.import.resolve" />
                          </Button>
                          &nbsp;
                          <Button
                            kind="ghost"
                            size="sm"
                            onClick={() => resolve(item, "SKIP")}
                          >
                            <FormattedMessage id="catalog.import.skip" />
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </Column>
          </Grid>
        </div>
      </div>
    </>
  );
};

export default CatalogImport;
