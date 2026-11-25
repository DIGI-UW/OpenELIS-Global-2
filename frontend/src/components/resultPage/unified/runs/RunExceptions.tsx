import React, { useContext, useEffect, useState } from "react";
import {
  Button,
  ComboBox,
  InlineNotification,
  Link as CarbonLink,
  Tag,
  TextArea,
  Tile,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { Link as RouterLink } from "react-router-dom";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
import { RequiredMarker, requiredProps } from "../../../common/RequiredMarker";
import useSiteDateFormat from "../../../common/useSiteDateFormat";
import { formatRunTime } from "./runFormat";

export type RunExceptionKind =
  | "UNMATCHED"
  | "UNORDERED"
  | "RESULTED"
  | "UNMAPPED";

export interface RunExceptionRecord {
  key: string;
  kind: RunExceptionKind;
  accessionNumber?: string | null;
  testId?: string | null;
  componentId?: string | null;
  testName?: string | null;
  code?: string | null;
  value?: string | null;
  issue?: string | null;
  reportedPatient?: string | null;
  rowIds: string[];
  mapping?: {
    analyzerId: string;
    focusTest: string;
    focusValue?: string | null;
  } | null;
  hold?: {
    reason: string;
    heldBy?: string | null;
    heldAt?: string | null;
  } | null;
}

interface RunOrderMatch {
  accessionNumber: string;
  patientName?: string | null;
  receivedDate?: string | null;
  test: "AWAITING" | "RESULTED" | "NOT_ORDERED";
}

const ORDER_STATE_KEYS: Record<RunOrderMatch["test"], string> = {
  AWAITING: "run.exception.order.testOrdered",
  RESULTED: "run.exception.order.testResulted",
  NOT_ORDERED: "run.exception.order.testNotOrdered",
};

type Action = "match" | "hold" | "ignore";

const REASON_MAX = 200;
const QUERY_MIN = 3;
const VALUE_MAPPING_ISSUES = [
  "unknown_analyzer_result_value",
  "result_mapping_not_ready",
  "invalid_result_mapping",
  "conflicting_answer_mapping",
];

/** Where Map now opens the analyzer's mapping, coming back to this run. */
export const mapNowUrl = (
  runId: number,
  mapping: NonNullable<RunExceptionRecord["mapping"]>,
): string => {
  const query = new URLSearchParams({
    returnTo: `/Results?run=${runId}`,
    focusTest: mapping.focusTest,
  });
  if (mapping.focusValue) {
    query.set("focusValue", mapping.focusValue);
  }
  return `/analyzers/${encodeURIComponent(mapping.analyzerId)}/mapping?${query.toString()}`;
};

interface ExceptionLineProps {
  runId: number;
  record: RunExceptionRecord;
  onResolved: () => void;
}

const ExceptionLine: React.FC<ExceptionLineProps> = ({
  runId,
  record,
  onResolved,
}) => {
  const intl = useIntl();
  const { pattern } = useSiteDateFormat();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);
  const [action, setAction] = useState<Action | null>(null);
  const [reason, setReason] = useState("");
  const [query, setQuery] = useState("");
  const [orders, setOrders] = useState<RunOrderMatch[]>([]);
  const [order, setOrder] = useState<RunOrderMatch | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const lineId = record.rowIds[0];

  useEffect(() => {
    if (action !== "match" || query.trim().length < QUERY_MIN) {
      setOrders([]);
      return;
    }
    const controller = new AbortController();
    const timer = window.setTimeout(() => {
      const params = new URLSearchParams({ q: query.trim() });
      if (record.testId) {
        params.set("testId", record.testId);
      }
      if (record.componentId) {
        params.set("componentId", record.componentId);
      }
      getFromOpenElisServer(
        `/rest/result-runs/${runId}/exceptions/orders?${params.toString()}`,
        (response?: RunOrderMatch[]) => {
          setOrders(Array.isArray(response) ? response : []);
        },
        controller.signal,
      );
    }, 300);
    return () => {
      window.clearTimeout(timer);
      controller.abort();
    };
  }, [action, query, record.testId, record.componentId, runId]);

  const count = record.rowIds.length;
  const detail = () => {
    switch (record.kind) {
      case "UNMATCHED":
        return intl.formatMessage(
          { id: "run.exception.detail.unmatched" },
          { labNumber: record.accessionNumber || "—", count },
        );
      case "UNORDERED":
      case "RESULTED":
        return intl.formatMessage(
          {
            id:
              record.kind === "RESULTED"
                ? "run.exception.detail.resulted"
                : "run.exception.detail.unordered",
          },
          {
            test: record.testName || record.code || "—",
            labNumber: record.accessionNumber || "—",
            count,
          },
        );
      default:
        return intl.formatMessage(
          {
            id: VALUE_MAPPING_ISSUES.includes(record.issue ?? "")
              ? "run.exception.detail.unmappedValue"
              : "run.exception.detail.unmappedCode",
          },
          { code: record.code || "—", value: record.value || "—", count },
        );
    }
  };

  const orderLabel = (candidate: RunOrderMatch | null) =>
    candidate
      ? [
          candidate.accessionNumber,
          candidate.patientName,
          candidate.receivedDate,
          intl.formatMessage({
            id:
              ORDER_STATE_KEYS[candidate.test] ??
              "run.exception.order.testNotOrdered",
          }),
        ]
          .filter(Boolean)
          .join(" · ")
      : "";

  const close = () => {
    setAction(null);
    setReason("");
    setQuery("");
    setOrder(null);
    setError(null);
  };

  const confirm = () => {
    if (!action) {
      return;
    }
    if (action === "match" && !order) {
      setError(intl.formatMessage({ id: "run.exception.orderRequired" }));
      return;
    }
    if (!reason.trim()) {
      setError(intl.formatMessage({ id: "run.exception.reasonRequired" }));
      return;
    }
    setBusy(true);
    setError(null);
    postToOpenElisServerJsonResponse(
      `/rest/result-runs/${runId}/exceptions/${action}`,
      JSON.stringify({
        rowIds: record.rowIds,
        key: record.key,
        reason: reason.trim(),
        accessionNumber: action === "match" ? order?.accessionNumber : null,
      }),
      (response?: {
        exceptions?: unknown[];
        message?: string;
        stale?: boolean;
      }) => {
        setBusy(false);
        if (response?.stale) {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "run.header.exceptions" }),
            message:
              response.message ||
              intl.formatMessage({ id: "run.exception.failed" }),
          });
          setNotificationVisible(true);
          close();
          onResolved();
          return;
        }
        if (!response || !Array.isArray(response.exceptions)) {
          setError(
            response?.message ||
              intl.formatMessage({ id: "run.exception.failed" }),
          );
          return;
        }
        addNotification({
          kind: NotificationKinds.success,
          title: intl.formatMessage({ id: "run.header.exceptions" }),
          message: intl.formatMessage(
            { id: `run.toast.${action}` },
            { labNumber: order?.accessionNumber ?? "", count },
          ),
        });
        setNotificationVisible(true);
        close();
        onResolved();
      },
    );
  };

  return (
    <li className="runException" data-testid={`run-exception-${lineId}`}>
      <div className="runExceptionLine">
        <Tag type="purple" size="sm">
          <FormattedMessage id={`run.exception.${record.kind}`} />
        </Tag>{" "}
        <span data-testid={`run-exception-detail-${lineId}`}>{detail()}</span>
      </div>
      {record.hold && (
        <p
          className="unifiedFieldHint"
          data-testid={`run-exception-hold-${lineId}`}
        >
          <FormattedMessage
            id="run.exception.held"
            values={{
              reason: record.hold.reason,
              by: record.hold.heldBy || "—",
              at: record.hold.heldAt
                ? formatRunTime(intl, record.hold.heldAt, pattern)
                : "—",
            }}
          />
        </p>
      )}
      {!action && (
        <div className="runExceptionActions">
          {record.kind === "UNMAPPED" ? (
            record.mapping && (
              <CarbonLink
                as={RouterLink}
                to={mapNowUrl(runId, record.mapping)}
                data-testid={`run-exception-map-${lineId}`}
              >
                <FormattedMessage id="run.action.mapNow" />
              </CarbonLink>
            )
          ) : (
            <Button
              kind="tertiary"
              size="sm"
              onClick={() => setAction("match")}
              data-testid={`run-exception-match-${lineId}`}
            >
              <FormattedMessage id="run.action.match" />
            </Button>
          )}
          <Button
            kind="ghost"
            size="sm"
            onClick={() => setAction("hold")}
            data-testid={`run-exception-hold-open-${lineId}`}
          >
            <FormattedMessage id="run.action.holdException" />
          </Button>
          <Button
            kind="ghost"
            size="sm"
            onClick={() => setAction("ignore")}
            data-testid={`run-exception-ignore-open-${lineId}`}
          >
            <FormattedMessage id="run.action.ignoreException" />
          </Button>
        </div>
      )}
      {action && (
        <div className="runExceptionForm">
          <p>
            {intl.formatMessage(
              { id: `run.exception.explain.${action}` },
              { count },
            )}
          </p>
          {action === "match" && record.reportedPatient && (
            <p data-testid={`run-exception-reported-patient-${lineId}`}>
              <FormattedMessage
                id="run.exception.reportedPatient"
                values={{ patient: record.reportedPatient }}
              />
            </p>
          )}
          {action === "match" && (
            <ComboBox
              id={`run-${runId}-exception-order-${lineId}`}
              titleText={
                <>
                  {intl.formatMessage({ id: "run.exception.order" })}
                  <RequiredMarker />
                </>
              }
              helperText={intl.formatMessage({
                id: "run.exception.orderHelp",
              })}
              items={orders}
              itemToString={orderLabel}
              selectedItem={order}
              shouldFilterItem={() => true}
              onInputChange={(text: string) => {
                setQuery(text ?? "");
              }}
              onChange={({
                selectedItem,
              }: {
                selectedItem?: RunOrderMatch | null;
              }) => setOrder(selectedItem ?? null)}
            />
          )}
          <TextArea
            id={`run-${runId}-exception-reason-${lineId}`}
            labelText={
              <>
                {intl.formatMessage({ id: "run.exception.reason" })}
                <RequiredMarker />
              </>
            }
            {...requiredProps()}
            value={reason}
            maxCount={REASON_MAX}
            enableCounter
            rows={2}
            onChange={(event) => setReason(event.target.value)}
          />
          {error && (
            <InlineNotification
              kind="error"
              lowContrast
              hideCloseButton
              title={error}
            />
          )}
          <div className="runQcFormActions">
            <Button
              size="sm"
              kind={action === "ignore" ? "danger" : "primary"}
              disabled={busy}
              onClick={confirm}
              data-testid={`run-exception-confirm-${lineId}`}
            >
              <FormattedMessage
                id={
                  action === "match"
                    ? "run.action.matchConfirm"
                    : action === "hold"
                      ? "run.action.holdException"
                      : "run.action.ignoreException"
                }
              />
            </Button>
            <Button size="sm" kind="ghost" onClick={close}>
              <FormattedMessage id="label.button.cancel" />
            </Button>
          </div>
        </div>
      )}
    </li>
  );
};

interface RunExceptionsProps {
  runId: number;
  exceptions?: RunExceptionRecord[] | null;
  onResolved: () => void;
}

/**
 * OGC-1200 FR-E4: the run's records that cannot be accepted until someone
 * resolves them, a lab number that names no order, a test not on its order or
 * already resulted there, or an analyzer code with no mapping. Each is resolved here: found on another
 * order, mapped, held or ignored with a reason, and only its own values are
 * touched. Hidden when the run has none.
 */
const RunExceptions: React.FC<RunExceptionsProps> = ({
  runId,
  exceptions,
  onResolved,
}) => {
  if (!exceptions || exceptions.length === 0) {
    return null;
  }
  return (
    <Tile className="runHeaderTile" data-testid="run-exceptions">
      <h4>
        <FormattedMessage
          id="run.header.exceptionsCount"
          values={{ count: exceptions.length }}
        />
      </h4>
      <ul className="runExceptions">
        {exceptions.map((record) => (
          <ExceptionLine
            key={record.key}
            runId={runId}
            record={record}
            onResolved={onResolved}
          />
        ))}
      </ul>
    </Tile>
  );
};

export default RunExceptions;
