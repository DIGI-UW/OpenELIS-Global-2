import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  Button,
  Checkbox,
  Column,
  Grid,
  Heading,
  InlineNotification,
  Pagination,
  Section,
  Tab,
  TabList,
  TabPanel,
  TabPanels,
  Tabs,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
} from "@carbon/react";
import { Add, Archive, Checkmark, Play, Renew } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
import {
  convertAlphaNumLabNumForDisplay,
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
  putToOpenElisServerFullResponse,
} from "../utils/Utils";
import "./wpStyle.css";

const BATCH_ENDPOINT = "/rest/batch-workplans";

const statusTagType = (status) => {
  switch (status) {
    case "ACTIVE":
      return "blue";
    case "COMPLETED":
      return "green";
    case "ARCHIVED":
      return "gray";
    case "DRAFT":
    default:
      return "purple";
  }
};

const parseJson = async (response) => {
  if (!response) {
    return null;
  }
  try {
    return await response.json();
  } catch (_error) {
    return null;
  }
};

export default function BatchWorkplan() {
  const intl = useIntl();
  const [pendingTests, setPendingTests] = useState([]);
  const [batches, setBatches] = useState([]);
  const [selectedIds, setSelectedIds] = useState([]);
  const [pendingPage, setPendingPage] = useState(1);
  const [pendingPageSize, setPendingPageSize] = useState(20);
  const [batchPage, setBatchPage] = useState(1);
  const [batchPageSize, setBatchPageSize] = useState(10);
  const [loading, setLoading] = useState(false);
  const [notification, setNotification] = useState(null);

  const breadcrumbs = useMemo(
    () => [
      { label: "home.label", link: "/" },
      { label: "workplan.batch.title", link: "/Workplan" },
    ],
    [],
  );

  const selectedSet = useMemo(() => new Set(selectedIds), [selectedIds]);

  const pagedPendingTests = useMemo(
    () =>
      pendingTests.slice(
        (pendingPage - 1) * pendingPageSize,
        pendingPage * pendingPageSize,
      ),
    [pendingTests, pendingPage, pendingPageSize],
  );

  const pagedBatches = useMemo(
    () =>
      batches.slice((batchPage - 1) * batchPageSize, batchPage * batchPageSize),
    [batches, batchPage, batchPageSize],
  );

  const loadPendingTests = useCallback(() => {
    getFromOpenElisServer(
      `${BATCH_ENDPOINT}/pending-tests?limit=500`,
      (res) => {
        setPendingTests(Array.isArray(res) ? res : []);
      },
    );
  }, []);

  const loadBatches = useCallback(() => {
    getFromOpenElisServer(`${BATCH_ENDPOINT}/batches`, (res) => {
      setBatches(Array.isArray(res) ? res : []);
    });
  }, []);

  const reload = useCallback(() => {
    setLoading(true);
    loadPendingTests();
    loadBatches();
    setLoading(false);
  }, [loadBatches, loadPendingTests]);

  useEffect(() => {
    reload();
  }, [reload]);

  const showNotification = (kind, messageId) => {
    setNotification({
      kind,
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage({ id: messageId }),
    });
  };

  const toggleSelected = (analysisId, checked) => {
    setSelectedIds((current) => {
      if (checked) {
        return current.includes(analysisId)
          ? current
          : [...current, analysisId];
      }
      return current.filter((id) => id !== analysisId);
    });
  };

  const toggleVisible = (checked) => {
    const visibleIds = pagedPendingTests.map((test) => test.analysisId);
    setSelectedIds((current) => {
      if (checked) {
        return Array.from(new Set([...current, ...visibleIds]));
      }
      return current.filter((id) => !visibleIds.includes(id));
    });
  };

  const visibleSelected =
    pagedPendingTests.length > 0 &&
    pagedPendingTests.every((test) => selectedSet.has(test.analysisId));

  const createBatch = () => {
    if (selectedIds.length === 0) {
      return;
    }
    setLoading(true);
    postToOpenElisServerFullResponse(
      `${BATCH_ENDPOINT}/batches`,
      JSON.stringify({ analysisIds: selectedIds }),
      async (response) => {
        setLoading(false);
        if (response?.ok) {
          setSelectedIds([]);
          showNotification("success", "workplan.batch.create.success");
          loadPendingTests();
          loadBatches();
          return;
        }
        const body = await parseJson(response);
        setNotification({
          kind: "error",
          title: intl.formatMessage({ id: "notification.title" }),
          message:
            body?.message ||
            intl.formatMessage({ id: "workplan.batch.create.error" }),
        });
      },
    );
  };

  const transitionBatch = (batch, status) => {
    setLoading(true);
    putToOpenElisServerFullResponse(
      `${BATCH_ENDPOINT}/batches/${batch.id}/status`,
      JSON.stringify({ status }),
      async (response) => {
        setLoading(false);
        if (response?.ok) {
          showNotification("success", "workplan.batch.status.success");
          loadPendingTests();
          loadBatches();
          return;
        }
        const body = await parseJson(response);
        setNotification({
          kind: "error",
          title: intl.formatMessage({ id: "notification.title" }),
          message:
            body?.message ||
            intl.formatMessage({ id: "workplan.batch.status.error" }),
        });
      },
    );
  };

  const renderBatchActions = (batch) => {
    if (batch.status === "DRAFT") {
      return (
        <>
          <Button
            kind="ghost"
            size="sm"
            renderIcon={Play}
            onClick={() => transitionBatch(batch, "ACTIVE")}
          >
            <FormattedMessage id="workplan.batch.action.activate" />
          </Button>
          <Button
            kind="ghost"
            size="sm"
            renderIcon={Archive}
            onClick={() => transitionBatch(batch, "ARCHIVED")}
          >
            <FormattedMessage id="workplan.batch.action.archive" />
          </Button>
        </>
      );
    }
    if (batch.status === "ACTIVE") {
      return (
        <>
          <Button
            kind="ghost"
            size="sm"
            renderIcon={Checkmark}
            onClick={() => transitionBatch(batch, "COMPLETED")}
          >
            <FormattedMessage id="workplan.batch.action.complete" />
          </Button>
          <Button
            kind="ghost"
            size="sm"
            renderIcon={Archive}
            onClick={() => transitionBatch(batch, "ARCHIVED")}
          >
            <FormattedMessage id="workplan.batch.action.archive" />
          </Button>
        </>
      );
    }
    if (batch.status === "COMPLETED") {
      return (
        <Button
          kind="ghost"
          size="sm"
          renderIcon={Archive}
          onClick={() => transitionBatch(batch, "ARCHIVED")}
        >
          <FormattedMessage id="workplan.batch.action.archive" />
        </Button>
      );
    }
    return null;
  };

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth className="batch-workplan">
        <Column lg={16} md={8} sm={4}>
          <div className="batch-workplan__header">
            <Section>
              <Heading>
                <FormattedMessage id="workplan.batch.title" />
              </Heading>
            </Section>
            <div className="batch-workplan__actions">
              <Button
                kind="ghost"
                size="sm"
                renderIcon={Renew}
                onClick={reload}
                disabled={loading}
              >
                <FormattedMessage id="workplan.batch.refresh" />
              </Button>
              <Button
                size="sm"
                renderIcon={Add}
                onClick={createBatch}
                disabled={selectedIds.length === 0 || loading}
              >
                <FormattedMessage
                  id="workplan.batch.create"
                  values={{ count: selectedIds.length }}
                />
              </Button>
            </div>
          </div>

          {notification && (
            <InlineNotification
              lowContrast
              kind={notification.kind}
              title={notification.title}
              subtitle={notification.message}
              onCloseButtonClick={() => setNotification(null)}
            />
          )}

          <Tabs>
            <TabList
              aria-label={intl.formatMessage({ id: "workplan.batch.tabs" })}
            >
              <Tab>
                <FormattedMessage id="workplan.batch.pendingTests" />
              </Tab>
              <Tab>
                <FormattedMessage id="workplan.batch.batches" />
              </Tab>
            </TabList>
            <TabPanels>
              <TabPanel>
                <Table size="sm" useZebraStyles>
                  <TableHead>
                    <TableRow>
                      <TableHeader className="batch-workplan__select-col">
                        <Checkbox
                          id="batch-workplan-select-page"
                          hideLabel
                          labelText={intl.formatMessage({
                            id: "workplan.batch.selectPage",
                          })}
                          checked={visibleSelected}
                          onChange={(_, { checked }) => toggleVisible(checked)}
                        />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="quick.entry.accession.number" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="workplan.batch.column.test" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="workplan.batch.column.unit" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="workplan.batch.column.method" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="workplan.batch.column.sampleType" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="sample.receivedDate" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="workplan.batch.column.status" />
                      </TableHeader>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {pagedPendingTests.map((test) => (
                      <TableRow key={test.analysisId}>
                        <TableCell>
                          <Checkbox
                            id={`batch-workplan-${test.analysisId}`}
                            hideLabel
                            labelText={intl.formatMessage(
                              { id: "workplan.batch.selectTest" },
                              { accessionNumber: test.accessionNumber },
                            )}
                            checked={selectedSet.has(test.analysisId)}
                            onChange={(_, { checked }) =>
                              toggleSelected(test.analysisId, checked)
                            }
                          />
                        </TableCell>
                        <TableCell>
                          {convertAlphaNumLabNumForDisplay(
                            test.accessionNumber,
                          )}
                        </TableCell>
                        <TableCell>{test.testName}</TableCell>
                        <TableCell>{test.testSectionName}</TableCell>
                        <TableCell>{test.methodName}</TableCell>
                        <TableCell>{test.sampleType}</TableCell>
                        <TableCell>{test.receivedDate}</TableCell>
                        <TableCell>
                          <Tag
                            type={test.nonconforming ? "red" : "gray"}
                            size="sm"
                          >
                            {test.statusName}
                          </Tag>
                        </TableCell>
                      </TableRow>
                    ))}
                    {pagedPendingTests.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={8}>
                          <FormattedMessage id="workplan.batch.pending.empty" />
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
                <Pagination
                  onChange={({ page, pageSize }) => {
                    setPendingPage(page);
                    setPendingPageSize(pageSize);
                  }}
                  page={pendingPage}
                  pageSize={pendingPageSize}
                  pageSizes={[10, 20, 50, 100]}
                  totalItems={pendingTests.length}
                  forwardText={intl.formatMessage({
                    id: "pagination.forward",
                  })}
                  backwardText={intl.formatMessage({
                    id: "pagination.backward",
                  })}
                  itemsPerPageText={intl.formatMessage({
                    id: "pagination.items-per-page",
                  })}
                  pageNumberText={intl.formatMessage({
                    id: "pagination.page-number",
                  })}
                />
              </TabPanel>
              <TabPanel>
                <Table size="sm" useZebraStyles>
                  <TableHead>
                    <TableRow>
                      <TableHeader>
                        <FormattedMessage id="workplan.batch.column.name" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="workplan.batch.column.status" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="workplan.batch.column.count" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="workplan.batch.column.created" />
                      </TableHeader>
                      <TableHeader>
                        <FormattedMessage id="workplan.batch.column.actions" />
                      </TableHeader>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {pagedBatches.map((batch) => (
                      <TableRow key={batch.id}>
                        <TableCell>{batch.name}</TableCell>
                        <TableCell>
                          <Tag type={statusTagType(batch.status)} size="sm">
                            <FormattedMessage
                              id={`workplan.batch.status.${batch.status}`}
                            />
                          </Tag>
                        </TableCell>
                        <TableCell>{batch.itemCount}</TableCell>
                        <TableCell>
                          {batch.createdAt
                            ? intl.formatDate(new Date(batch.createdAt), {
                                year: "numeric",
                                month: "short",
                                day: "2-digit",
                                hour: "2-digit",
                                minute: "2-digit",
                              })
                            : ""}
                        </TableCell>
                        <TableCell>
                          <div className="batch-workplan__row-actions">
                            {renderBatchActions(batch)}
                          </div>
                        </TableCell>
                      </TableRow>
                    ))}
                    {pagedBatches.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={5}>
                          <FormattedMessage id="workplan.batch.batches.empty" />
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
                <Pagination
                  onChange={({ page, pageSize }) => {
                    setBatchPage(page);
                    setBatchPageSize(pageSize);
                  }}
                  page={batchPage}
                  pageSize={batchPageSize}
                  pageSizes={[10, 20, 50]}
                  totalItems={batches.length}
                  forwardText={intl.formatMessage({
                    id: "pagination.forward",
                  })}
                  backwardText={intl.formatMessage({
                    id: "pagination.backward",
                  })}
                  itemsPerPageText={intl.formatMessage({
                    id: "pagination.items-per-page",
                  })}
                  pageNumberText={intl.formatMessage({
                    id: "pagination.page-number",
                  })}
                />
              </TabPanel>
            </TabPanels>
          </Tabs>
        </Column>
      </Grid>
    </>
  );
}
