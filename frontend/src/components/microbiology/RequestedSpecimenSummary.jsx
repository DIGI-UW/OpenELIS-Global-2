import React from "react";
import { Tag } from "@carbon/react";
import { useIntl } from "react-intl";

const RequestedSpecimenSummary = ({ specimens = [] }) => {
  const intl = useIntl();
  if (!specimens.length) return null;
  return (
    <section
      aria-label={intl.formatMessage({
        id: "microbiology.case.requestedSpecimens",
      })}
    >
      <h2>
        {intl.formatMessage({ id: "microbiology.case.requestedSpecimens" })}
      </h2>
      <ul>
        {specimens.map((specimen) => (
          <li key={specimen.requestId}>
            <Tag type="gray">
              {intl.formatMessage({
                id: "microbiology.case.awaitingCollection",
              })}
            </Tag>
            {[specimen.specimenType, specimen.containerType, specimen.bodySite]
              .filter(Boolean)
              .join(" · ")}
            {specimen.collectedInSets && specimen.cultureSetNumber != null && (
              <span>
                {" · "}
                {intl.formatMessage(
                  { id: "microbiology.sets.number" },
                  { number: specimen.cultureSetNumber },
                )}
              </span>
            )}
          </li>
        ))}
      </ul>
    </section>
  );
};

export default RequestedSpecimenSummary;
