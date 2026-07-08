import React, { useEffect, useState } from "react";
import { Accordion, AccordionItem } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { useHistory } from "react-router-dom";
import { formatTat } from "../../reports/tat/tatUtils";
import ComingSoon from "./ComingSoon";
import {
  NCE_DRILL_URL,
  countCriticalPending,
  fetchNceList,
} from "./nceOverview";
import { fetchOverviewSummary, fetchTatRollup } from "./overviewData";
import { STATUS_ICON, qcPillar } from "./PillarStatus";

const STORAGE_KEY = "qa.overview.inspectorOpen";

const AnswerRow = ({ titleKey, status, answer, loading, onClick }) => (
  <button type="button" className="qa-live-row" onClick={onClick}>
    <span
      className={"qa-pillar-icon" + (status ? ` qa-live-${status}` : "")}
      aria-hidden="true"
    >
      {loading ? "…" : STATUS_ICON[status] || "—"}
    </span>
    <span className="qa-cs-body">
      <span className="qa-cs-title">
        <FormattedMessage id={titleKey} />
      </span>
      <span className="qa-live-caption">{loading ? "…" : answer}</span>
    </span>
    <span className="qa-live-arrow" aria-hidden="true">
      →
    </span>
  </button>
);

/**
 * Inspector readiness Q&A (OGC-694 WS-F): Q1 answers from the QC instrument
 * rollup, Q3 from the TAT rollup, Q4 from the NCE register; Q2 (EQA, OGC-721)
 * and Q5 (accreditation, OGC-716) stay placeholders.
 */
const InspectorReadiness = () => {
  const intl = useIntl();
  const history = useHistory();
  const title = intl.formatMessage({ id: "qa.overview.section.inspector" });
  // Collapsed by default; open state is sticky per browser session (OGC-694).
  const [open, setOpen] = useState(
    () => sessionStorage.getItem(STORAGE_KEY) === "1",
  );
  // undefined = loading, null = fetch yielded no data
  const [summary, setSummary] = useState();
  const [nceList, setNceList] = useState();
  const [tat, setTat] = useState();

  useEffect(() => {
    let mounted = true;
    fetchOverviewSummary((data) => mounted && setSummary(data));
    fetchNceList((list) => mounted && setNceList(list));
    fetchTatRollup((data) => mounted && setTat(data));
    return () => {
      mounted = false;
    };
  }, []);

  const handleHeadingClick = ({ isOpen }) => {
    setOpen(isOpen);
    sessionStorage.setItem(STORAGE_KEY, isOpen ? "1" : "0");
  };

  const qc = summary ? summary.qc : null;
  const q1Status = qcPillar(intl, summary).status;
  const critical = nceList ? countCriticalPending(nceList) : null;

  return (
    <section className="qa-overview-section" aria-label={title}>
      <Accordion>
        <AccordionItem
          title={title}
          open={open}
          onHeadingClick={handleHeadingClick}
        >
          <div className="qa-cs-rows">
            <AnswerRow
              titleKey="qa.overview.inspector.q1"
              loading={summary === undefined}
              status={q1Status}
              answer={
                qc && qc.totalInstruments > 0
                  ? intl.formatMessage(
                      { id: "qa.overview.inspector.q1.answer" },
                      {
                        inControl: qc.compliantInstruments,
                        total: qc.totalInstruments,
                      },
                    )
                  : intl.formatMessage({ id: "qa.overview.pillar.qc.noData" })
              }
              onClick={() => history.push("/qa/qc/dashboard")}
            />
            <ComingSoon
              variant="row"
              titleKey="qa.overview.inspector.q2"
              ticket="OGC-721"
            />
            <AnswerRow
              titleKey="qa.overview.inspector.q3"
              loading={tat === undefined}
              status={tat ? (tat.tone === "bad" ? "amber" : "green") : null}
              answer={
                tat
                  ? intl.formatMessage(
                      { id: "qa.overview.inspector.q3.answer" },
                      { tat: formatTat(tat.mean) },
                    )
                  : intl.formatMessage({ id: "qa.overview.pillar.qi.noData" })
              }
              onClick={() => history.push("/qa/qi/dashboard")}
            />
            <AnswerRow
              titleKey="qa.overview.inspector.q4"
              loading={nceList === undefined}
              status={critical == null ? null : critical > 0 ? "red" : "green"}
              answer={
                critical == null
                  ? "—"
                  : intl.formatMessage(
                      { id: "qa.overview.inspector.q4.answer" },
                      { count: critical },
                    )
              }
              onClick={() => history.push(NCE_DRILL_URL)}
            />
            <ComingSoon
              variant="row"
              titleKey="qa.overview.inspector.q5"
              ticket="OGC-716"
            />
          </div>
        </AccordionItem>
      </Accordion>
    </section>
  );
};

export default InspectorReadiness;
