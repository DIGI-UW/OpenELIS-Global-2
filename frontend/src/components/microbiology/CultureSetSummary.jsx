import React from "react";
import { Tag } from "@carbon/react";
import { useIntl } from "react-intl";

const CultureSetSummary = ({
  specimens = [],
  requestedSpecimens = [],
  warnings = [],
}) => {
  const intl = useIntl();
  const bottles = [...specimens, ...requestedSpecimens].filter(
    (specimen) => specimen.collectedInSets,
  );
  if (!bottles.length) return null;
  const sets = new Map();
  bottles.forEach((bottle) => {
    const key = bottle.cultureSetNumber ?? null;
    sets.set(key, [...(sets.get(key) || []), bottle]);
  });
  const unique = (values) => [...new Set(values.filter(Boolean))].join(", ");
  return (
    <section aria-label={intl.formatMessage({ id: "microbiology.sets.title" })}>
      <strong>
        {intl.formatMessage(
          { id: "microbiology.sets.count" },
          {
            sets: [...sets.keys()].filter((key) => key !== null).length,
            bottles: bottles.length,
          },
        )}
      </strong>
      <ul>
        {[...sets.entries()]
          .sort(([a], [b]) => (a ?? Infinity) - (b ?? Infinity))
          .map(([number, members]) => (
            <li key={number ?? "unassigned"}>
              {warnings
                .filter((warning) => warning.setNumber === number)
                .map((warning) => (
                  <Tag type="warm-gray" key={warning.code}>
                    {intl.formatMessage(
                      { id: `microbiology.sets.warning.${warning.code}` },
                      { minutes: warning.intervalMinutes },
                    )}
                  </Tag>
                ))}
              {number == null
                ? intl.formatMessage({ id: "microbiology.sets.unassigned" })
                : intl.formatMessage(
                    { id: "microbiology.sets.number" },
                    { number },
                  )}
              {": "}
              {[
                unique(members.map((member) => member.bodySite)),
                unique(
                  members.map(
                    (member) =>
                      member.collectionDate &&
                      intl.formatTime(member.collectionDate),
                  ),
                ),
                unique(
                  members.map(
                    (member) => member.containerType || member.specimenType,
                  ),
                ),
              ]
                .filter(Boolean)
                .join("; ")}
            </li>
          ))}
      </ul>
    </section>
  );
};

export default CultureSetSummary;
