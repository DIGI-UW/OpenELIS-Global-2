import React from "react";
import { Link } from "react-router-dom";
import { Tile } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { windowDelta } from "../../reports/tat/tatUtils";
import ServerDataState from "../../utils/ServerDataState";
import { useServerData } from "../../utils/useServerData";
import { qualityUrl } from "./managementApi";
import "../../qa/common/QAStyles.css";
import "./QualitySection.css";

const LABELS = {
  qcPass: "label.mgmt.quality.qcPass",
  eqa: "label.mgmt.quality.eqaPerf",
  nceInverse: "label.mgmt.quality.nceInverse",
  amendmentInverse: "label.mgmt.quality.amendmentInverse",
};

const QualitySection = ({ scope, scopeLabel }) => {
  const intl = useIntl();
  const query = useServerData(qualityUrl(scope));
  const rollup = query.data;

  if (!rollup) {
    return <ServerDataState query={query} />;
  }

  const delta =
    rollup.composite != null && rollup.prior != null
      ? windowDelta(
          rollup.composite - rollup.prior,
          0.05,
          (d) => d.toFixed(1),
          true,
        )
      : null;
  const missing = rollup.components.filter((c) => c.value == null);

  return (
    <section className="mgmt-quality" data-testid="mgmt-quality">
      <h3>
        <FormattedMessage id="label.mgmt.quality.title" />
      </h3>
      <div className="mgmt-quality__cards">
        <Tile className="mgmt-quality__score">
          <p className="mgmt-quality__label">
            <FormattedMessage id="label.mgmt.quality.composite" />
          </p>
          <p
            className="mgmt-quality__number"
            data-testid="mgmt-quality-composite"
          >
            {rollup.composite != null ? rollup.composite.toFixed(1) : "—"}
          </p>
          {delta && (
            <p className={`qi-tile__delta qi-tile__delta--${delta.tone}`}>
              {delta.arrow} {delta.text}{" "}
              <FormattedMessage id="label.mgmt.kpi.vsPrior" />
            </p>
          )}
          <p className="mgmt-quality__note">{scopeLabel}</p>
        </Tile>
        <Tile className="mgmt-quality__components">
          <p className="mgmt-quality__label">
            <FormattedMessage id="label.mgmt.quality.components" />
          </p>
          <ul>
            {rollup.components.map((c) => (
              <li key={c.key} data-testid={`mgmt-quality-${c.key}`}>
                <span className="mgmt-quality__name">
                  <FormattedMessage id={LABELS[c.key]} />
                </span>
                <span className="mgmt-quality__meter" aria-hidden="true">
                  <span style={{ width: `${c.value ?? 0}%` }} />
                </span>
                <span className="mgmt-quality__value">
                  {c.value != null ? c.value.toFixed(1) : "—"}
                </span>
                <span className="mgmt-quality__weight">
                  <FormattedMessage
                    id="label.mgmt.quality.weight"
                    values={{ 0: c.weight.toFixed(2) }}
                  />
                </span>
              </li>
            ))}
          </ul>
          {missing.length > 0 && (
            <p
              className="mgmt-quality__note"
              data-testid="mgmt-quality-missing"
            >
              <FormattedMessage
                id="label.mgmt.quality.noData"
                values={{
                  0: missing
                    .map((c) => intl.formatMessage({ id: LABELS[c.key] }))
                    .join(", "),
                }}
              />
            </p>
          )}
          <p className="mgmt-quality__note">
            <FormattedMessage id="label.mgmt.quality.weightsConfigurable" />{" "}
            <Link to="/qa/qi/config#composite-weights">
              <FormattedMessage id="label.mgmt.quality.editWeights" />
            </Link>{" "}
            ·{" "}
            <Link to="/qa/qi/dashboard">
              <FormattedMessage id="label.mgmt.quality.qiDashboard" />
            </Link>
          </p>
        </Tile>
      </div>
    </section>
  );
};

export default QualitySection;
