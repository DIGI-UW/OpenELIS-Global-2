import React, { useEffect, useState } from "react";
import { ClickableTile, SkeletonText } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { useHistory } from "react-router-dom";
import { formatTat } from "../../reports/tat/tatUtils";
import ComingSoon from "./ComingSoon";
import {
  countCriticalPending,
  countInCorrectiveAction,
  fetchNceList,
} from "./nceOverview";
import { fetchOverviewSummary, fetchTatRollup } from "./overviewData";

export const STATUS_ICON = { green: "✓", amber: "⚠", red: "✗" };

const PillarChip = ({ titleKey, status, text, loading, onClick }) => (
  <ClickableTile
    className={`qa-cs-tile qa-pillar-chip qa-pillar-${status || "none"}`}
    onClick={onClick}
  >
    <div className="qa-cs-title">
      {status ? (
        <span className={`qa-pillar-icon qa-live-${status}`} aria-hidden="true">
          {STATUS_ICON[status]}{" "}
        </span>
      ) : null}
      <FormattedMessage id={titleKey} />
    </div>
    {loading ? (
      <SkeletonText width="70%" />
    ) : (
      <div className="qa-live-caption">{text}</div>
    )}
  </ClickableTile>
);

// Exported so InspectorReadiness Q1 derives the same status as the QC chip.
export const qcPillar = (intl, summary) => {
  if (!summary) return { status: null, text: "—" };
  const {
    compliantInstruments,
    warningInstruments,
    nonCompliantInstruments,
    totalInstruments,
  } = summary.qc;
  // Zero instruments means "no QC data" (unconfigured lab or a swallowed
  // backend failure) — never a healthy green.
  if (totalInstruments === 0) {
    return {
      status: null,
      text: intl.formatMessage({ id: "qa.overview.pillar.qc.noData" }),
    };
  }
  const status =
    nonCompliantInstruments > 0
      ? "red"
      : warningInstruments > 0
        ? "amber"
        : "green";
  return {
    status,
    text: intl.formatMessage(
      { id: "qa.overview.pillar.qc.text" },
      { inControl: compliantInstruments, total: totalInstruments },
    ),
  };
};

const qiPillar = (intl, tat) => {
  if (!tat) {
    return {
      status: null,
      text: intl.formatMessage({ id: "qa.overview.pillar.qi.noData" }),
    };
  }
  const delta = tat.text ? ` ${tat.arrow} ${tat.text}` : "";
  return {
    status: tat.tone === "bad" ? "amber" : "green",
    text:
      intl.formatMessage(
        { id: "qa.overview.pillar.qi.text" },
        { tat: formatTat(tat.mean) },
      ) + delta,
  };
};

const qmsPillar = (intl, nceList) => {
  if (!nceList) return { status: null, text: "—" };
  const critical = countCriticalPending(nceList);
  const capa = countInCorrectiveAction(nceList);
  return {
    status: critical > 0 ? "red" : capa > 0 ? "amber" : "green",
    text: intl.formatMessage(
      { id: "qa.overview.pillar.qms.text" },
      { critical, capa },
    ),
  };
};

/**
 * Pillar Status chips (OGC-694 WS-F): QC and QMS light up from live rollups,
 * QI from the shared TAT rollup; EQA stays a placeholder until Phase E
 * (OGC-721).
 */
const PillarStatus = () => {
  const intl = useIntl();
  const history = useHistory();
  const title = intl.formatMessage({ id: "qa.overview.section.pillars" });
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

  const qc = qcPillar(intl, summary);
  const qi = qiPillar(intl, tat);
  const qms = qmsPillar(intl, nceList);

  return (
    <section className="qa-overview-section" aria-label={title}>
      <div className="qa-sec-head">
        <h3>{title}</h3>
      </div>
      <div className="qa-cs-grid qa-cs-grid-pillars">
        <PillarChip
          titleKey="sideNav.label.qa.qc"
          loading={summary === undefined}
          status={qc.status}
          text={qc.text}
          onClick={() => history.push("/qa/qc/dashboard")}
        />
        <ComingSoon titleKey="banner.menu.eqa" ticket="OGC-721" />
        <PillarChip
          titleKey="sideNav.label.qa.qi"
          loading={tat === undefined}
          status={qi.status}
          text={qi.text}
          onClick={() => history.push("/qa/qi/dashboard")}
        />
        <PillarChip
          titleKey="sideNav.label.qa.qms"
          loading={nceList === undefined}
          status={qms.status}
          text={qms.text}
          onClick={() => history.push("/NceDashboard")}
        />
      </div>
    </section>
  );
};

export default PillarStatus;
