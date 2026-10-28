import React from "react";
import { InlineNotification, Link, SkeletonText, Tile } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import ServerDataState from "../../utils/ServerDataState";
import { useServerData } from "../../utils/useServerData";
import { stockUrl } from "./managementApi";
import "../../qa/common/QAStyles.css";
import "./StockSection.css";

const METER_TONE = { REORDER_NOW: "red", REORDER_SOON: "amber" };

const Card = ({ titleKey, emptyKey, children }) => (
  <Tile className="mgmt-stock__card">
    <h4>
      <FormattedMessage id={titleKey} />
    </h4>
    {React.Children.count(children) > 0 ? (
      children
    ) : (
      <p className="mgmt-stock__none">
        <FormattedMessage id={emptyKey} />
      </p>
    )}
  </Tile>
);

const StockSection = ({ features }) => {
  const intl = useIntl();
  const query = useServerData(features?.inventory ? stockUrl() : null);

  if (!features?.inventory) return null;

  const number = (value) =>
    intl.formatNumber(value, { maximumFractionDigits: 1 });
  const date = (iso) =>
    intl.formatDate(`${iso}T00:00:00`, {
      day: "2-digit",
      month: "short",
      year: "numeric",
    });
  const data = query.data;

  const body = () => {
    if (query.isError) return <ServerDataState query={query} />;
    if (!data) return <SkeletonText paragraph lineCount={3} />;
    if (data.state === "EMPTY") {
      return (
        <p className="mgmt-stock__none">
          <FormattedMessage id="label.mgmt.stock.emptyState" />
        </p>
      );
    }
    return (
      <div className="mgmt-stock__cards">
        <Card
          titleKey="label.mgmt.stock.criticalReagents"
          emptyKey="label.mgmt.stock.noneCritical"
        >
          {data.criticalReagents.map((reagent) => {
            const pct = Math.round((reagent.onHand / reagent.threshold) * 100);
            return (
              <div key={reagent.itemId} className="mgmt-stock__reagent">
                <div>
                  <div>{reagent.name}</div>
                  {reagent.lot && (
                    <div className="mgmt-stock__meta">
                      <FormattedMessage
                        id="label.mgmt.stock.lotExpiry"
                        values={{
                          0: reagent.lot,
                          1: reagent.expiry ? date(reagent.expiry) : "—",
                        }}
                      />
                    </div>
                  )}
                </div>
                <div
                  className={`mgmt-meter mgmt-meter--${
                    METER_TONE[reagent.status] || "green"
                  }`}
                >
                  <span style={{ width: `${Math.min(pct, 100)}%` }} />
                </div>
                <div className="mgmt-stock__amount">
                  <div>
                    {number(reagent.onHand)} / {number(reagent.threshold)}
                  </div>
                  <div className="mgmt-stock__meta">
                    {intl.formatNumber(pct / 100, { style: "percent" })}
                  </div>
                </div>
              </div>
            );
          })}
        </Card>
        <Card
          titleKey="label.mgmt.stock.expiringIn30"
          emptyKey="label.mgmt.stock.noneExpiring"
        >
          {data.expiring.map((lot) => (
            <InlineNotification
              key={`${lot.name}-${lot.lot}`}
              kind={lot.expired ? "error" : "warning"}
              lowContrast
              hideCloseButton
              title={intl.formatMessage(
                {
                  id: lot.expired
                    ? "label.mgmt.stock.expired"
                    : "label.mgmt.stock.expiresIn",
                },
                { days: lot.daysLeft },
              )}
              subtitle={intl.formatMessage(
                { id: "label.mgmt.stock.lotRemaining" },
                { 0: lot.name, 1: lot.lot, 2: number(lot.remaining) },
              )}
            />
          ))}
        </Card>
        <Card
          titleKey="label.mgmt.stock.reorderAlerts"
          emptyKey="label.mgmt.stock.noneReorder"
        >
          {data.reorder.map((item) => {
            const below = item.kind === "BELOW";
            const on = below ? item.orderedOn : item.orderBy;
            const subtitle = intl.formatMessage(
              { id: "label.mgmt.stock.onHandReorderAt" },
              {
                0: item.name,
                1: number(item.onHand),
                2: number(item.threshold ?? 0),
              },
            );
            return (
              <InlineNotification
                key={item.itemId}
                kind={below ? "error" : "warning"}
                lowContrast
                hideCloseButton
                title={intl.formatMessage({
                  id: below
                    ? "label.mgmt.stock.belowReorder"
                    : "label.mgmt.stock.approachingReorder",
                })}
                subtitle={
                  on
                    ? `${subtitle} · ${intl.formatMessage(
                        {
                          id: below
                            ? "label.mgmt.stock.orderPlaced"
                            : "label.mgmt.stock.orderBy",
                        },
                        { 0: date(on) },
                      )}`
                    : subtitle
                }
              />
            );
          })}
        </Card>
      </div>
    );
  };

  return (
    <section className="mgmt-stock" data-testid="mgmt-stock">
      <div className="mgmt-stock__header">
        <h3>
          <FormattedMessage id="label.mgmt.stock.title" />
        </h3>
        <Link href="/inventory">
          <FormattedMessage id="label.mgmt.stock.inventoryLink" />
        </Link>
      </div>
      {body()}
    </section>
  );
};

export default StockSection;
