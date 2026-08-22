import React from "react";
import { ListItem, UnorderedList } from "@carbon/react";
import { FormattedMessage } from "react-intl";

const AffectedAnalyzerList = ({ analyzers = [] }) => {
  if (analyzers.length === 0) {
    return null;
  }

  return (
    <section
      className="analyzer-type-affected"
      aria-labelledby="analyzer-type-affected-heading"
    >
      <h3 id="analyzer-type-affected-heading">
        <FormattedMessage
          id="analyzerType.affectedAnalyzers.heading"
          values={{ count: analyzers.length }}
        />
      </h3>
      <UnorderedList>
        {analyzers.map((analyzer) => (
          <ListItem key={analyzer.id}>{analyzer.name}</ListItem>
        ))}
      </UnorderedList>
    </section>
  );
};

export default AffectedAnalyzerList;
