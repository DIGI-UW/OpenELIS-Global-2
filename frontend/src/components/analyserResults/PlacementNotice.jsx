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

/**
 * What a result will do when it is saved, and why, for one review row. A result
 * with a single waiting analysis only says so; every other state explains
 * itself, and several matching analyses ask the reviewer to choose one.
 */
const PlacementNotice = ({
  row,
  onChooseAnalysis,
  onViewBundle,
  showChooser = true,
}) => {
  const intl = useIntl();
  const placement = row.placement;
  if (!placement) {
    return null;
  }
  const tubeName = (tubeId) => {
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
      <Tag type={TAG_TYPE[placement.state] || "gray"} size="sm">
        <FormattedMessage id={`analyzer.placement.state.${placement.state}`} />
      </Tag>
      <p>
        <FormattedMessage
          id={reasonId}
          values={{
            tube: placement.proposedSampleItemId
              ? tubeName(placement.proposedSampleItemId)
              : "",
          }}
        />
      </p>
      {patient?.status === "MATCH" && (
        <p>
          <FormattedMessage
            id="analyzer.placement.patient.match"
            values={{ instrument: instrumentPatient }}
          />
        </p>
      )}
      {patient?.status === "MISMATCH" && (
        <>
          <Tag type="red" size="sm">
            <FormattedMessage id="analyzer.placement.patient.mismatch.tag" />
          </Tag>
          <p>
            <FormattedMessage
              id="analyzer.placement.patient.mismatch"
              values={{
                instrument: instrumentPatient,
                order: patient.orderName || "",
              }}
            />
          </p>
        </>
      )}
      {patient?.status === "NO_ORDER_PATIENT" && (
        <p>
          <FormattedMessage
            id="analyzer.placement.patient.noOrderPatient"
            values={{ instrument: instrumentPatient }}
          />
        </p>
      )}
      {ambiguous && candidates.length > 0 && showChooser && (
        <Select
          id={`resultList${row.id}.chosenAnalysisId`}
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
                  tube: tubeName(candidate.sampleItemId),
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
      )}
      {row.deliveryReceiptId && (
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
