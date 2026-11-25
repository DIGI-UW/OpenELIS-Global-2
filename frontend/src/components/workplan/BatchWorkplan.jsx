import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import {
  Button,
  Column,
  Grid,
  Heading,
  InlineNotification,
  Modal,
  Section,
  Tab,
  TabList,
  TabPanel,
  TabPanels,
  Tabs,
} from "@carbon/react";
import { Renew, TrashCan } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import {
  getFromOpenElisServer,
  hasPermissionOrGlobalAdmin,
  postToOpenElisServerFullResponse,
  putToOpenElisServerFullResponse,
} from "../utils/Utils";
import BatchTile from "./batch/BatchTile";
import PendingTestsPanel from "./batch/PendingTestsPanel";
import {
  AUTO_EXPAND_QC,
  BATCH_ENDPOINT,
  QC_OVERRIDE_PERMISSION,
  parseJson,
} from "./batch/batchWorkplanUtils";
import "./wpStyle.css";

/**
 * The batch workplan (OGC-427): Pending Tests to group work into batches, and
 * Batches to set each one up with a reagent lot, check its QC and release it
 * for printing.
 */
export default function BatchWorkplan() {
  const intl = useIntl();
  const { userSessionDetails } = useContext(UserSessionDetailsContext) || {};
  const canOverride = hasPermissionOrGlobalAdmin(
    userSessionDetails,
    QC_OVERRIDE_PERMISSION,
  );

  const [pendingTests, setPendingTests] = useState([]);
  const [batches, setBatches] = useState([]);
  const [loading, setLoading] = useState(false);
  const [notification, setNotification] = useState(null);
  const [tab, setTab] = useState(0);
  const [expanded, setExpanded] = useState(() => new Set());
  const [confirmClear, setConfirmClear] = useState(false);
  const autoExpanded = useRef(new Set());

  const breadcrumbs = useMemo(
    () => [
      { label: "home.label", link: "/" },
      { label: "workplan.batch.title", link: "/Workplan" },
    ],
    [],
  );

  const loadPendingTests = useCallback((onDone) => {
    getFromOpenElisServer(
      `${BATCH_ENDPOINT}/pending-tests?limit=500`,
      (res) => {
        setPendingTests(Array.isArray(res) ? res : []);
        if (onDone) onDone();
      },
    );
  }, []);

  const loadBatches = useCallback((onDone) => {
    getFromOpenElisServer(`${BATCH_ENDPOINT}/batches`, (res) => {
      const list = Array.isArray(res) ? res : [];
      setBatches(list);
      // FR-QC-008: a batch whose QC needs attention opens by itself, once, so
      // a user who collapses it is not overruled by the next refresh.
      setExpanded((current) => {
        const next = new Set(current);
        list.forEach((batch) => {
          if (
            AUTO_EXPAND_QC.includes(batch.qc?.status) &&
            !autoExpanded.current.has(batch.id)
          ) {
            autoExpanded.current.add(batch.id);
            next.add(batch.id);
          }
        });
        return next;
      });
      if (onDone) onDone();
    });
  }, []);

  const reload = useCallback(() => {
    setLoading(true);
    let outstanding = 2;
    const settle = () => {
      outstanding -= 1;
      if (outstanding === 0) setLoading(false);
    };
    loadPendingTests(settle);
    loadBatches(settle);
  }, [loadBatches, loadPendingTests]);

  useEffect(() => {
    reload();
  }, [reload]);

  const notify = useCallback(
    (kind, message) =>
      setNotification({
        kind,
        title: intl.formatMessage({ id: "notification.title" }),
        message,
      }),
    [intl],
  );

  const toggle = (id) =>
    setExpanded((current) => {
      const next = new Set(current);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });

  const createBatch = (name, analysisIds, onCreated) => {
    setLoading(true);
    postToOpenElisServerFullResponse(
      `${BATCH_ENDPOINT}/batches`,
      JSON.stringify({ name: name || null, analysisIds }),
      async (response) => {
        setLoading(false);
        const body = await parseJson(response);
        if (response?.ok) {
          onCreated();
          notify(
            "success",
            intl.formatMessage(
              { id: "workplan.batch.create.success.named" },
              { name: body?.name || "" },
            ),
          );
          if (body?.id) {
            setExpanded((current) => new Set(current).add(body.id));
          }
          setTab(1);
          reload();
          return;
        }
        notify(
          "error",
          body?.message ||
            intl.formatMessage({ id: "workplan.batch.create.error" }),
        );
      },
    );
  };

  const addToBatch = (batchId, analysisIds, onAdded) => {
    const batch = batches.find((candidate) => candidate.id === batchId);
    if (!batch) return;
    setLoading(true);
    putToOpenElisServerFullResponse(
      `${BATCH_ENDPOINT}/batches/${batchId}`,
      JSON.stringify({
        analysisIds: [
          ...(batch.items || []).map((item) => item.analysisId),
          ...analysisIds,
        ],
      }),
      async (response) => {
        setLoading(false);
        if (response?.ok) {
          onAdded();
          notify(
            "success",
            intl.formatMessage(
              { id: "workplan.batch.addTo.success" },
              { name: batch.name },
            ),
          );
          setExpanded((current) => new Set(current).add(batchId));
          setTab(1);
          reload();
          return;
        }
        const body = await parseJson(response);
        notify(
          "error",
          body?.message ||
            intl.formatMessage({ id: "workplan.batch.tests.updateFailed" }),
        );
      },
    );
  };

  const clearAll = () => {
    setConfirmClear(false);
    setLoading(true);
    postToOpenElisServerFullResponse(
      `${BATCH_ENDPOINT}/batches/archive-all`,
      JSON.stringify({}),
      async (response) => {
        setLoading(false);
        if (response?.ok) {
          notify(
            "success",
            intl.formatMessage({ id: "workplan.batch.cleared" }),
          );
          reload();
          return;
        }
        const body = await parseJson(response);
        notify(
          "error",
          body?.message ||
            intl.formatMessage({ id: "workplan.batch.clear.failed" }),
        );
      },
    );
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

          <Tabs
            selectedIndex={tab}
            onChange={({ selectedIndex }) => setTab(selectedIndex)}
          >
            <TabList
              aria-label={intl.formatMessage({ id: "workplan.batch.tabs" })}
            >
              <Tab>
                <FormattedMessage id="workplan.batch.pendingTests" />
              </Tab>
              <Tab>
                <FormattedMessage
                  id="workplan.batch.batches.count"
                  values={{ count: batches.length }}
                />
              </Tab>
            </TabList>
            <TabPanels>
              <TabPanel>
                <PendingTestsPanel
                  tests={pendingTests}
                  loading={loading}
                  canCreate
                  onCreateBatch={createBatch}
                  draftBatches={batches.filter(
                    (batch) => batch.status === "DRAFT",
                  )}
                  onAddToBatch={addToBatch}
                />
              </TabPanel>
              <TabPanel>
                <div className="batch-workplan__batches-toolbar">
                  <Button
                    kind="danger--ghost"
                    size="sm"
                    renderIcon={TrashCan}
                    disabled={loading || batches.length === 0}
                    onClick={() => setConfirmClear(true)}
                  >
                    <FormattedMessage id="workplan.batch.clearAll" />
                  </Button>
                </div>
                {batches.length === 0 && (
                  <p className="batch-workplan__muted">
                    <FormattedMessage id="workplan.batch.batches.empty" />
                  </p>
                )}
                {batches.map((batch) => (
                  <BatchTile
                    key={batch.id}
                    batch={batch}
                    expanded={expanded.has(batch.id)}
                    onToggle={toggle}
                    onChanged={reload}
                    notify={notify}
                    canOverride={canOverride}
                  />
                ))}
              </TabPanel>
            </TabPanels>
          </Tabs>
        </Column>
      </Grid>
      <Modal
        open={confirmClear}
        danger
        size="xs"
        modalHeading={intl.formatMessage({ id: "workplan.batch.clearAll" })}
        primaryButtonText={intl.formatMessage({
          id: "workplan.batch.clearAll.confirm",
        })}
        secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
        onRequestSubmit={clearAll}
        onRequestClose={() => setConfirmClear(false)}
      >
        <p>
          <FormattedMessage id="workplan.batch.clearAll.body" />
        </p>
      </Modal>
    </>
  );
}
