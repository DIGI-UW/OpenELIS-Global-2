import React, { useContext } from "react";
import { Link } from "react-router-dom";
import { Tile } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import QITile from "../../qa/qi/QITile";
import { windowDelta } from "../../reports/tat/tatUtils";
import ServerDataState from "../../utils/ServerDataState";
import { useServerData } from "../../utils/useServerData";
import { hasPermissionOrGlobalAdmin, hasRole, Roles } from "../../utils/Utils";
import UserSessionDetailsContext from "../../../UserSessionDetailsContext";
import { qualityUrl } from "./managementApi";
import "../../qa/common/QAStyles.css";
import "./QualitySection.css";

const LABELS = {
  qcPass: "label.mgmt.quality.qcPass",
  eqa: "label.mgmt.quality.eqaPerf",
  nceInverse: "label.mgmt.quality.nceInverse",
  amendmentInverse: "label.mgmt.quality.amendmentInverse",
};

// The QI Dashboard route's own role list.
const QI_DASHBOARD_ROLES = [
  Roles.RECEPTION,
  Roles.RESULTS,
  Roles.VALIDATION,
  Roles.GLOBAL_ADMIN,
];

const score = (value) => (value != null ? value.toFixed(1) : "—");

const QualitySection = ({ scope, scopeLabel }) => {
  const intl = useIntl();
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
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
  const canEditWeights = hasPermissionOrGlobalAdmin(
    userSessionDetails,
    "qa.manage.qi",
  );
  const canOpenQiDashboard = QI_DASHBOARD_ROLES.some((role) =>
    hasRole(userSessionDetails, role),
  );

  const row = (key, labelKey, value, weight) => (
    <li key={key} data-testid={`mgmt-quality-${key}`}>
      <span className="mgmt-quality__name">
        <FormattedMessage id={labelKey} />
      </span>
      <span className="mgmt-meter" aria-hidden="true">
        <span style={{ width: `${value ?? 0}%` }} />
      </span>
      <span className="mgmt-quality__value">{score(value)}</span>
      <span className="mgmt-quality__note">
        {weight != null && (
          <FormattedMessage
            id="label.mgmt.quality.weight"
            values={{ 0: weight.toFixed(2) }}
          />
        )}
      </span>
    </li>
  );

  return (
    <section className="mgmt-quality" data-testid="mgmt-quality">
      <h3>
        <FormattedMessage id="label.mgmt.quality.title" />
      </h3>
      <div className="mgmt-quality__cards">
        <QITile
          testId="mgmt-quality-composite"
          titleKey="label.mgmt.quality.composite"
          primary={score(rollup.composite)}
          delta={delta}
          targetLine={intl.formatMessage({ id: "label.mgmt.kpi.vsPrior" })}
          secondary={scopeLabel}
        />
        <Tile className="mgmt-quality__components">
          <p className="mgmt-quality__label">
            <FormattedMessage id="label.mgmt.quality.components" />
          </p>
          <ul>
            {rollup.components.map((c) =>
              row(c.key, LABELS[c.key], c.value, c.weight),
            )}
            {row(
              "total",
              "label.mgmt.quality.composite",
              rollup.composite,
              null,
            )}
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
          {(canEditWeights || canOpenQiDashboard) && (
            <p className="mgmt-quality__note">
              {canEditWeights && (
                <>
                  <FormattedMessage id="label.mgmt.quality.weightsConfigurable" />{" "}
                  <Link to="/qa/qi/config#composite-weights">
                    <FormattedMessage id="label.mgmt.quality.editWeights" />
                  </Link>
                </>
              )}
              {canEditWeights && canOpenQiDashboard && " · "}
              {canOpenQiDashboard && (
                <Link to="/qa/qi/dashboard">
                  <FormattedMessage id="label.mgmt.quality.qiDashboard" />
                </Link>
              )}
            </p>
          )}
        </Tile>
      </div>
    </section>
  );
};

export default QualitySection;
