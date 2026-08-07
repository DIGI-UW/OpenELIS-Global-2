import React, { useCallback, useEffect, useState } from "react";
import { Pagination, Tag } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import ReferenceSection from "./ReferenceSection";
import { getFromOpenElisServer } from "../../utils/Utils";

/**
 * OGC-1022 (R3, FR-H1/H2) — "History (this analysis)": an inline,
 * default-collapsed reference section, paginated 25/50/100, fed by
 * GET /rest/results-entry/analysis/{id}/history. This analysis's own events
 * only — patient-longitudinal trends and Westgard rules live elsewhere (D7).
 * Events load lazily on first open so collapsed rows cost nothing.
 */
export interface TimelineEvent {
  type?: string;
  when?: string;
  detail?: string;
  by?: string;
}

interface HistoryResponse {
  events?: TimelineEvent[];
  total?: number;
  page?: number;
  pageSize?: number;
}

const EVENT_TAG_TYPE: Record<string, string> = {
  CREATED: "blue",
  STATUS: "teal",
  RESULT: "green",
  NOTE: "gray",
  RETEST: "magenta",
  REFLEX: "purple",
};

interface HistorySectionProps {
  analysisId?: string;
  open: boolean;
  onToggle: (open: boolean) => void;
}

const HistorySection: React.FC<HistorySectionProps> = ({
  analysisId,
  open,
  onToggle,
}) => {
  const intl = useIntl();
  const [events, setEvents] = useState<TimelineEvent[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);
  const [loaded, setLoaded] = useState(false);

  const fetchPage = useCallback(
    (nextPage: number, nextPageSize: number) => {
      if (!analysisId) {
        return;
      }
      getFromOpenElisServer(
        `/rest/results-entry/analysis/${analysisId}/history?page=${nextPage}&pageSize=${nextPageSize}`,
        (body: HistoryResponse) => {
          setEvents(body?.events || []);
          setTotal(body?.total || 0);
          setPage(body?.page || nextPage);
          setPageSize(body?.pageSize || nextPageSize);
          setLoaded(true);
        },
      );
    },
    [analysisId],
  );

  useEffect(() => {
    if (open && !loaded) {
      fetchPage(page, pageSize);
    }
  }, [open, loaded, fetchPage, page, pageSize]);

  const summary = loaded
    ? intl.formatMessage({ id: "label.results.history.summary" }, { 0: total })
    : intl.formatMessage({ id: "label.results.history.summary.unloaded" });

  return (
    <ReferenceSection
      sectionId="history"
      title={<FormattedMessage id="label.results.section.history" />}
      summary={summary}
      open={open}
      onToggle={onToggle}
    >
      {loaded && events.length === 0 && (
        <div className="unifiedHistoryEmpty">
          <FormattedMessage id="label.results.history.empty" />
        </div>
      )}
      {events.length > 0 && (
        <div className="unifiedHistoryTable" data-testid="history-table">
          <div className="unifiedHistoryRow unifiedHistoryHead">
            <span className="unifiedHistoryWhen">
              <FormattedMessage id="label.results.history.when" />
            </span>
            <span className="unifiedHistoryEvent">
              <FormattedMessage id="label.results.history.event" />
            </span>
            <span className="unifiedHistoryDetail">
              <FormattedMessage id="label.results.history.detail" />
            </span>
            <span className="unifiedHistoryBy">
              <FormattedMessage id="label.results.history.by" />
            </span>
          </div>
          {events.map((event, index) => (
            <div className="unifiedHistoryRow" key={index}>
              <span className="unifiedHistoryWhen">{event.when}</span>
              <span className="unifiedHistoryEvent">
                <Tag
                  size="sm"
                  type={EVENT_TAG_TYPE[event.type || ""] || "gray"}
                >
                  <FormattedMessage
                    id={`label.results.history.event.${event.type || "OTHER"}`}
                    defaultMessage={event.type}
                  />
                </Tag>
              </span>
              <span className="unifiedHistoryDetail">{event.detail}</span>
              <span className="unifiedHistoryBy">{event.by}</span>
            </div>
          ))}
        </div>
      )}
      {loaded && total > 0 && (
        <Pagination
          page={page}
          pageSize={pageSize}
          pageSizes={[25, 50, 100]}
          totalItems={total}
          size="sm"
          onChange={({
            page: nextPage,
            pageSize: nextPageSize,
          }: {
            page: number;
            pageSize: number;
          }) => fetchPage(nextPage, nextPageSize)}
        />
      )}
      <div className="unifiedHistoryFootnote">
        <FormattedMessage id="label.results.history.footnote" />
      </div>
    </ReferenceSection>
  );
};

export default HistorySection;
