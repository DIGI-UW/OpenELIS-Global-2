import React from "react";
import { FormattedMessage } from "react-intl";

/**
 * What the instrument reported about a result, shown as sent and labelled
 * instrument-reported. OpenELIS does not interpret the flag.
 */
const InstrumentReported = ({ row }) => {
  const { instrumentFlags, assayName, assayVersion, instrumentOperator } = row;
  if (!instrumentFlags && !assayName && !instrumentOperator) {
    return null;
  }
  return (
    <div
      className="instrumentReported"
      data-testid={`instrument-reported-${row.id}`}
    >
      <div className="instrumentReported__label">
        <FormattedMessage id="analyzer.results.instrumentReported" />
      </div>
      {instrumentFlags && (
        <div>
          <FormattedMessage
            id="analyzer.results.instrumentReported.flag"
            values={{ flag: instrumentFlags }}
          />
        </div>
      )}
      {assayName && (
        <div>
          <FormattedMessage
            id={
              assayVersion
                ? "analyzer.results.instrumentReported.assayVersion"
                : "analyzer.results.instrumentReported.assay"
            }
            values={{ assay: assayName, version: assayVersion }}
          />
        </div>
      )}
      {instrumentOperator && (
        <div>
          <FormattedMessage
            id="analyzer.results.instrumentReported.operator"
            values={{ operator: instrumentOperator }}
          />
        </div>
      )}
    </div>
  );
};

export default InstrumentReported;
