import React from "react";
import { Button, Select, SelectItem, Tag } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";

const TAG_TYPE = {
  RESOLVED: "green",
  RETEST_CHOICE: "magenta",
  MULTI_TUBE: "red",
  UNORDERED_ONE_FITS: "blue",
  UNORDERED_MANY_FIT: "blue",
  UNORDERED_NONE_FIT: "blue",
  NEW_SAMPLE: "purple",
};

const tubeName = (intl, placement, tubeId) => {
  const tube = (placement.tubes || []).find(
    (candidate) => candidate.sampleItemId === tubeId,
  );
  return (
    tube?.externalId ||
    intl.formatMessage(
      { id: "analyzer.placement.tubeUnlabelled" },
      { id: tubeId },
    )
  );
};

/** The placement state as a tag, on its own. */
export const PlacementStateTag = ({ placement }) => (
  <Tag type={TAG_TYPE[placement.state] || "gray"} size="sm">
    <FormattedMessage id={`analyzer.placement.state.${placement.state}`} />
  </Tag>
);

/**
 * The choice a result with several matching analyses asks of the reviewer:
 * which analysis it belongs to.
 */
export const AnalysisChooser = ({ row, onChooseAnalysis }) => {
  const intl = useIntl();
  const placement = row.placement;
  const candidates = placement?.analyses || [];
  if (placement?.state !== "MULTI_TUBE" || candidates.length === 0) {
    return null;
  }
  return (
    <Select
      id={`resultList${row.id}.chosenAnalysisId`}
      size="sm"
      labelText={intl.formatMessage({
        id: "analyzer.placement.chooseAnalysis",
      })}
      defaultValue={row.chosenAnalysisId || ""}
      onChange={(event) => onChooseAnalysis?.(event.target.value, row.id)}
    >
      <SelectItem
        value=""
        text={intl.formatMessage({
          id: "analyzer.placement.chooseAnalysis.none",
        })}
      />
      {candidates.map((candidate) => (
        <SelectItem
          key={candidate.analysisId}
          value={candidate.analysisId}
          text={intl.formatMessage(
            { id: "analyzer.placement.option" },
            {
              tube: tubeName(intl, placement, candidate.sampleItemId),
              status: intl.formatMessage({
                id: candidate.awaitingResult
                  ? "analyzer.placement.option.awaiting"
                  : "analyzer.placement.option.hasResult",
              }),
            },
          )}
        />
      ))}
    </Select>
  );
};

/**
 * What a result will do when it is saved, and why, for one review row. A result
 * with a single waiting analysis only says so; every other state explains
 * itself, and several matching analyses ask the reviewer to choose one. The
 * parts can be left out when another row of the same sample already shows them.
 */
const PlacementNotice = ({
  row,
  onChooseAnalysis,
  onViewBundle,
  showChooser = true,
  showState = true,
  showPatient = true,
  showBundle = true,
}) => {
  const intl = useIntl();
  const placement = row.placement;
  if (!placement) {
    return null;
  }
  const patient = placement.patient;
  const instrumentPatient = patient?.instrumentId
    ? patient.instrumentName
      ? `${patient.instrumentId} (${patient.instrumentName})`
      : patient.instrumentId
    : "";
  const candidates = placement.analyses || [];
  const ambiguous = placement.state === "MULTI_TUBE";
  const reasonId =
    ambiguous && candidates.length === 0
      ? "analyzer.placement.reason.MULTI_TUBE_NO_ANALYSIS"
      : `analyzer.placement.reason.${placement.state}`;
  return (
    <div className="placementNotice" data-testid={`placement-${row.id}`}>
      {showState && (
        <div className="placementNotice__state">
          <PlacementStateTag placement={placement} />
          <span>
            <FormattedMessage
              id={reasonId}
              values={{
                tube: placement.proposedSampleItemId
                  ? tubeName(intl, placement, placement.proposedSampleItemId)
                  : "",
              }}
            />
          </span>
        </div>
      )}
      {showPatient && patient?.status === "MATCH" && (
        <p>
          <FormattedMessage
            id="analyzer.placement.patient.match"
            values={{ instrument: instrumentPatient }}
          />
        </p>
      )}
      {showPatient && patient?.status === "MISMATCH" && (
        <div className="placementNotice__state">
          <Tag type="red" size="sm">
            <FormattedMessage id="analyzer.placement.patient.mismatch.tag" />
          </Tag>
          <span>
            <FormattedMessage
              id="analyzer.placement.patient.mismatch"
              values={{
                instrument: instrumentPatient,
                order: patient.orderName || "",
              }}
            />
          </span>
        </div>
      )}
      {showPatient && patient?.status === "NO_ORDER_PATIENT" && (
        <p>
          <FormattedMessage
            id="analyzer.placement.patient.noOrderPatient"
            values={{ instrument: instrumentPatient }}
          />
        </p>
      )}
      {showChooser && (
        <AnalysisChooser row={row} onChooseAnalysis={onChooseAnalysis} />
      )}
      {showBundle && row.deliveryReceiptId && (
        <Button
          kind="ghost"
          size="sm"
          onClick={() => onViewBundle?.(row.deliveryReceiptId)}
        >
          <FormattedMessage id="analyzer.placement.viewBundle" />
        </Button>
      )}
    </div>
  );
};

export default PlacementNotice;
