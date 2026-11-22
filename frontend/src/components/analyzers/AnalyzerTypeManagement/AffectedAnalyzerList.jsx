import React from "react";
import { ListItem, Tag, UnorderedList } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import { Link } from "react-router-dom";

const AffectedAnalyzerList = ({ analyzers = [], revision }) => {
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
          <ListItem key={analyzer.id}>
            <span className="analyzer-type-affected__item">
              <span>{analyzer.name}</span>
              {analyzer.mappingConfirmed &&
              !analyzer.newerProfileRevision &&
              !analyzer.newerMappingRevision ? (
                <Tag type="green" size="sm">
                  <FormattedMessage id="analyzerType.affectedAnalyzers.current" />
                </Tag>
              ) : (
                <Tag type="warm-gray" size="sm">
                  <FormattedMessage id="analyzerType.affectedAnalyzers.behind" />
                </Tag>
              )}
              {analyzer.newerProfileRevision && (
                <Link
                  to={`/analyzers/${analyzer.id}/adoption?revision=${revision}`}
                >
                  <FormattedMessage
                    id="analyzerType.affectedAnalyzers.adopt"
                    values={{ revision }}
                  />
                </Link>
              )}
              {analyzer.newerMappingRevision && (
                <Link to={`/analyzers/${analyzer.id}/mapping`}>
                  <FormattedMessage id="analyzerType.affectedAnalyzers.verify" />
                </Link>
              )}
            </span>
          </ListItem>
        ))}
      </UnorderedList>
    </section>
  );
};

export default AffectedAnalyzerList;
