import React from "react";
import { FormattedMessage } from "react-intl";

/**
 * The row of count tiles a QA register opens with — blue/green/amber/red
 * accents over a label and a number. `tiles` is [{ labelKey, value, accent }].
 */
const QAStatTiles = ({ tiles }) => (
  <div className="qi-dashboard__tiles">
    {tiles.map(({ labelKey, value, accent }) => (
      <div className={`qi-tile qi-tile--${accent}`} key={labelKey}>
        <div className="qi-tile__title">
          <FormattedMessage id={labelKey} />
        </div>
        <div className="qi-tile__value">{value}</div>
      </div>
    ))}
  </div>
);

export default QAStatTiles;
