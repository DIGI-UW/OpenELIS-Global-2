import React from "react";
import { SkeletonText, Tag, Tile } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import ServerDataState from "../../utils/ServerDataState";
import { useServerData } from "../../utils/useServerData";
import { equipmentUrl } from "./managementApi";
import "../../qa/common/QAStyles.css";
import "./EquipmentSection.css";

const STATUS = {
  ACTIVE: { type: "green", labelKey: "label.mgmt.equipment.status.active" },
  QUIET: {
    type: "gray",
    className: "qi-rate-tag--amber",
    labelKey: "label.mgmt.equipment.status.quiet",
  },
  NO_ACTIVITY: {
    type: "red",
    labelKey: "label.mgmt.equipment.status.noActivity",
  },
};

export const timeAgo = (iso, locale, now = Date.now()) => {
  const minutes = Math.round((new Date(iso).getTime() - now) / 60000);
  const format = new Intl.RelativeTimeFormat(locale, { style: "narrow" });
  if (minutes > -60) return format.format(minutes, "minute");
  if (minutes > -24 * 60)
    return format.format(Math.round(minutes / 60), "hour");
  return format.format(Math.round(minutes / (24 * 60)), "day");
};

const EquipmentSection = ({ scope, scopeLabel, features }) => {
  const intl = useIntl();
  const query = useServerData(features?.analyzers ? equipmentUrl(scope) : null);

  if (!features?.analyzers) return null;

  return (
    <section className="mgmt-equipment" data-testid="mgmt-equipment">
      <div className="mgmt-equipment__header">
        <h3>
          <FormattedMessage id="label.mgmt.equipment.title" />
        </h3>
        <p className="mgmt-equipment__subtitle">
          <FormattedMessage
            id="label.mgmt.equipment.subtitle"
            values={{ 0: scopeLabel }}
          />
        </p>
      </div>
      {query.isError ? (
        <ServerDataState query={query} />
      ) : !query.data ? (
        <SkeletonText paragraph lineCount={3} />
      ) : (
        <div className="mgmt-equipment__grid">
          {query.data.map((analyzer) => {
            const status = STATUS[analyzer.status];
            return (
              <Tile
                key={analyzer.id}
                className="mgmt-equipment__card"
                data-testid="mgmt-equipment-card"
              >
                <div className="mgmt-equipment__card-head">
                  <h4>{analyzer.name}</h4>
                  <Tag
                    size="sm"
                    type={status.type}
                    className={status.className}
                  >
                    <FormattedMessage id={status.labelKey} />
                  </Tag>
                </div>
                <p>
                  <FormattedMessage
                    id="label.mgmt.equipment.samples"
                    values={{
                      0: (
                        <span className="mgmt-equipment__count">
                          {intl.formatNumber(analyzer.samples)}
                        </span>
                      ),
                      1: scopeLabel,
                      count: analyzer.samples,
                    }}
                  />
                </p>
                {analyzer.section && (
                  <p className="mgmt-equipment__meta">{analyzer.section}</p>
                )}
                <p className="mgmt-equipment__meta">
                  <FormattedMessage
                    id="label.mgmt.equipment.lastActivity"
                    values={{
                      0: analyzer.lastActivityAt
                        ? timeAgo(analyzer.lastActivityAt, intl.locale)
                        : "—",
                    }}
                  />
                </p>
              </Tile>
            );
          })}
        </div>
      )}
    </section>
  );
};

export default EquipmentSection;
