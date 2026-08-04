import React, { useState } from "react";
import {
  Button,
  Select,
  SelectItem,
  Tag,
  TextArea,
  TextInput,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { formatMicrobiologyEnum } from "./MicrobiologyLabels";
import ReagentLotPicker from "./ReagentLotPicker";
import ReagentUsageHistory from "./ReagentUsageHistory";

const STAGE_OPTIONS = [
  {
    value: "SETUP_RECORDED",
    labelId: "microbiology.case.action.startInoculation",
  },
  { value: "INCUBATING", labelId: "microbiology.case.action.setIncubating" },
  {
    value: "GROWTH_DETECTED",
    labelId: "microbiology.case.action.recordGrowth",
  },
  {
    value: "NO_GROWTH_READY",
    labelId: "microbiology.case.action.markNoGrowth",
  },
  { value: "REJECTED", labelId: "microbiology.case.action.rejectCase" },
];

const CaseTimelinePanel = ({
  activities = [],
  onRecordActivity,
  saving,
  setupSectionId,
  timelineSectionId,
  showSetup = true,
  showTimeline = true,
  reagentRequirements = [],
  reagentUsages = [],
}) => {
  const intl = useIntl();
  const [nextStage, setNextStage] = useState("SETUP_RECORDED");
  const [note, setNote] = useState("");
  const [media, setMedia] = useState("");
  const [incubation, setIncubation] = useState("");
  const [atmosphere, setAtmosphere] = useState("");
  const [selectedLots, setSelectedLots] = useState({});

  const selectLot = (selection) => {
    const selectionKey = `${selection.analysisId}:${selection.testReagentLinkId}`;
    setSelectedLots((current) => ({
      ...current,
      [selectionKey]: selection,
    }));
  };

  const submit = () => {
    const setupDetails =
      nextStage === "SETUP_RECORDED"
        ? [
            media &&
              `${intl.formatMessage({
                id: "microbiology.case.media",
              })}: ${media}`,
            incubation &&
              `${intl.formatMessage({
                id: "microbiology.case.incubation",
              })}: ${incubation}`,
            atmosphere &&
              `${intl.formatMessage({
                id: "microbiology.case.atmosphere",
              })}: ${atmosphere}`,
          ].filter(Boolean)
        : [];
    const payload = {
      nextStage,
      note: [...setupDetails, note].filter(Boolean).join("; "),
    };
    const lotSelections = Object.values(selectedLots);
    if (nextStage === "SETUP_RECORDED" && lotSelections.length > 0) {
      payload.lotSelections = lotSelections;
    }
    onRecordActivity(payload);
    setNote("");
    setMedia("");
    setIncubation("");
    setAtmosphere("");
    setSelectedLots({});
  };
  const selectedStageOption =
    STAGE_OPTIONS.find((option) => option.value === nextStage) ||
    STAGE_OPTIONS[0];

  return (
    <>
      {showSetup && (
        <section
          id={setupSectionId}
          className="microbiology-card microbiology-card--current"
          data-testid="microbiology-setup-card"
          aria-labelledby="microbiology-setup-heading"
        >
          <div className="microbiology-card__header">
            <div>
              <h3 id="microbiology-setup-heading">
                {intl.formatMessage({ id: "microbiology.case.setup" })}
              </h3>
              <p className="microbiology-card__hint">
                {intl.formatMessage({ id: "microbiology.case.setup.hint" })}
              </p>
            </div>
          </div>
          <div className="microbiology-form-grid">
            <Select
              id="microbiology-next-stage"
              labelText={intl.formatMessage({
                id: "microbiology.case.cultureAction",
              })}
              value={nextStage}
              onChange={(event) => setNextStage(event.target.value)}
            >
              {STAGE_OPTIONS.map((stage) => (
                <SelectItem
                  key={stage.value}
                  value={stage.value}
                  text={intl.formatMessage({ id: stage.labelId })}
                />
              ))}
            </Select>
            <div />
            {nextStage === "SETUP_RECORDED" && (
              <>
                <TextInput
                  id="microbiology-setup-media"
                  labelText={intl.formatMessage({
                    id: "microbiology.case.media",
                  })}
                  value={media}
                  onChange={(event) => setMedia(event.target.value)}
                />
                <TextInput
                  id="microbiology-setup-incubation"
                  labelText={intl.formatMessage({
                    id: "microbiology.case.incubation",
                  })}
                  value={incubation}
                  onChange={(event) => setIncubation(event.target.value)}
                />
                <TextInput
                  id="microbiology-setup-atmosphere"
                  labelText={intl.formatMessage({
                    id: "microbiology.case.atmosphere",
                  })}
                  value={atmosphere}
                  onChange={(event) => setAtmosphere(event.target.value)}
                />
                <div className="microbiology-form-grid__wide">
                  <ReagentLotPicker
                    id="microbiology-culture-lots"
                    requirements={reagentRequirements}
                    selectedLots={selectedLots}
                    onChange={selectLot}
                    disabled={saving}
                  />
                </div>
              </>
            )}
            <div className="microbiology-form-grid__wide">
              <TextArea
                id="microbiology-activity-note"
                labelText={intl.formatMessage({
                  id: "microbiology.case.activityNote",
                })}
                value={note}
                onChange={(event) => setNote(event.target.value)}
              />
            </div>
            <div>
              <Button onClick={submit} disabled={saving}>
                {intl.formatMessage({ id: selectedStageOption.labelId })}
              </Button>
            </div>
            <div className="microbiology-form-grid__wide">
              <ReagentUsageHistory
                usages={reagentUsages.filter(
                  (usage) => usage.usageContext === "CULTURE_SETUP",
                )}
              />
            </div>
          </div>
        </section>
      )}

      {showTimeline && (
        <section
          id={timelineSectionId}
          className="microbiology-card"
          data-testid="microbiology-timeline-card"
          aria-labelledby="microbiology-timeline-heading"
        >
          <div className="microbiology-card__header">
            <div>
              <h3 id="microbiology-timeline-heading">
                {intl.formatMessage({ id: "microbiology.case.timeline" })}
              </h3>
              <p className="microbiology-card__hint">
                {intl.formatMessage({ id: "microbiology.case.timeline.hint" })}
              </p>
            </div>
            <Tag type="cool-gray">
              {activities.length}{" "}
              {intl.formatMessage({ id: "microbiology.case.events" })}
            </Tag>
          </div>
          {activities.length === 0 ? (
            <p>
              {intl.formatMessage({ id: "microbiology.case.timeline.empty" })}
            </p>
          ) : (
            <ol className="microbiology-list">
              {activities.map((activity) => (
                <li
                  className="microbiology-list__row"
                  key={
                    activity.id ||
                    `${activity.activityType}-${activity.occurredAt}`
                  }
                >
                  <strong>
                    {formatMicrobiologyEnum(activity.activityType)}
                  </strong>
                  {activity.note ? `: ${activity.note}` : ""}
                  {activity.occurredAt && (
                    <div className="microbiology-list__meta">
                      {activity.occurredAt}
                    </div>
                  )}
                </li>
              ))}
            </ol>
          )}
        </section>
      )}
    </>
  );
};

export default CaseTimelinePanel;
