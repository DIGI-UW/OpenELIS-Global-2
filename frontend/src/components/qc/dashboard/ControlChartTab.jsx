import React, { useState, useEffect } from "react";
import { Tag } from "@carbon/react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import { ControlLotChart } from "../charts/ControlChartDetail";

const InstrumentControlChart = ({ instrumentId }) => {
  const intl = useIntl();
  const [selectedLot, setSelectedLot] = useState(null);
  const [activeRules, setActiveRules] = useState([]);

  useEffect(() => {
    setActiveRules([]);
    if (!selectedLot?.testId) return undefined;
    const controller = new AbortController();
    getFromOpenElisServer(
      `/rest/qc/ruleConfig/enabled?testId=${selectedLot.testId}&instrumentId=${instrumentId}`,
      (response) => {
        if (controller.signal.aborted) return;
        const rules = Array.isArray(response) ? response : response?.data || [];
        setActiveRules(rules.filter((rule) => rule.enabled));
      },
      controller.signal,
    );
    return () => controller.abort();
  }, [instrumentId, selectedLot]);

  return (
    <>
      <ControlLotChart
        analyzerId={instrumentId}
        showHeader={false}
        onLotChange={setSelectedLot}
      />
      {/* Active QC Rules */}
      {selectedLot && (
        <div className="instrument-detail-chart__rules-section">
          <h5>
            {intl.formatMessage({
              id: "qc.instrumentDetail.chart.activeRules",
            })}
          </h5>
          {activeRules.length > 0 ? (
            <div className="instrument-detail-chart__rules-tags">
              {activeRules.map((rule) => (
                <Tag
                  key={rule.id || rule.ruleCode}
                  type={rule.severity === "REJECTION" ? "red" : "teal"}
                  size="sm"
                >
                  {rule.ruleCode}
                </Tag>
              ))}
            </div>
          ) : (
            <p className="instrument-detail-chart__no-rules">
              {intl.formatMessage({
                id: "qc.instrumentDetail.chart.noRules",
              })}
            </p>
          )}
        </div>
      )}
    </>
  );
};

const ControlChartTab = ({ instrument, active }) =>
  active && instrument?.instrumentId ? (
    <InstrumentControlChart
      key={instrument.instrumentId}
      instrumentId={instrument.instrumentId}
    />
  ) : null;

export default ControlChartTab;
