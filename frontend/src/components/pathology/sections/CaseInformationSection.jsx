import React from "react";
import {
  Heading,
  InlineNotification,
  Section,
  Stack,
  StructuredListWrapper,
  StructuredListBody,
  StructuredListRow,
  StructuredListCell,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import QuestionnaireResponse from "../../common/QuestionnaireResponse";
import StageEventList from "./StageEventList";

/**
 * FR-1: what the laboratory already knows about the case, read from the
 * sample it was ordered on. Display-only: every one of these facts is owned by
 * order entry or by the dashboard's assignment, so offering an input here
 * would create a second place to change one of them.
 *
 * A fact nobody recorded is shown as an absence rather than as a blank, a zero
 * or the word "null", because an empty row beside a label reads as a field
 * that failed to load (S-6.3 applies the same rule to the summary panel, and
 * the hint deliberately reuses its class so the two never drift apart).
 *
 * The stage history sits here because this is the display-only section: who
 * moved the case and when is a fact the bench reads and never types.
 */
const isMissing = (value) =>
  value === null || value === undefined || value === "";

const notRecorded = () => (
  <span className="case-view__summary-label">
    <FormattedMessage id="caseView.label.notRecorded" />
  </span>
);

const CaseInformationSection = ({ caseInfo }) => {
  const intl = useIntl();
  const specimenTypes = caseInfo.specimenTypes ?? [];
  const stageEvents = caseInfo.stageEvents ?? [];

  const rows = [
    { labelKey: "sample.label.labnumber", value: caseInfo.labNumber },
    { labelKey: "sample.requestDate", value: caseInfo.requestDate },
    { labelKey: "sample.receivedDate", value: caseInfo.receivedDate },
    // Two items of one type read as that type twice: the row answers what
    // arrived, not how many distinct types the catalogue knows.
    { labelKey: "sample.type", value: specimenTypes.join(", ") },
    { labelKey: "sample.label.requester", value: caseInfo.requester },
    { labelKey: "sample.label.facility", value: caseInfo.referringFacility },
    { labelKey: "sample.label.dept", value: caseInfo.department },
    {
      labelKey: "assigned.technician.label",
      value: caseInfo.assignedTechnician,
    },
    {
      labelKey: "assigned.pathologist.label",
      value: caseInfo.assignedPathologist,
    },
  ];

  return (
    <Stack gap={5}>
      <StructuredListWrapper isCondensed>
        <StructuredListBody>
          {rows.map(({ labelKey, value }) => (
            <StructuredListRow key={labelKey}>
              <StructuredListCell>
                <FormattedMessage id={labelKey} />
              </StructuredListCell>
              <StructuredListCell>
                {isMissing(value) ? notRecorded() : value}
              </StructuredListCell>
            </StructuredListRow>
          ))}
        </StructuredListBody>
      </StructuredListWrapper>
      <div>
        <Section>
          <Heading className="pathology-case-view__heading">
            <FormattedMessage id="pathology.label.stageHistory" />
          </Heading>
        </Section>
        {stageEvents.length > 0 ? (
          <StageEventList
            events={stageEvents}
            ariaLabelKey="pathology.label.stageHistory"
            showCompletion={false}
          />
        ) : (
          <InlineNotification
            kind="info"
            lowContrast
            hideCloseButton
            title={intl.formatMessage({ id: "pathology.empty.noStageEvents" })}
          />
        )}
      </div>
      <QuestionnaireResponse
        questionnaireResponse={caseInfo.programQuestionnaireResponse}
      />
    </Stack>
  );
};

export default CaseInformationSection;
