import React, { useEffect, useState } from "react";
import {
  Button,
  InlineNotification,
  ListItem,
  Modal,
  NumberInput,
  Tile,
  UnorderedList,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { putToOpenElisServer } from "../../utils/Utils";
import {
  useInvalidateServerData,
  useServerData,
} from "../../utils/useServerData";
import { qualityWeightsUrl } from "./managementApi";
import { scaleToOne } from "./weightsScaling";
import "./CompositeWeightsPanel.css";

const FIELDS = [
  { key: "qc", labelKey: "label.mgmt.weights.qc" },
  { key: "eqa", labelKey: "label.mgmt.weights.eqa" },
  { key: "nceInverse", labelKey: "label.mgmt.weights.nceInverse" },
  { key: "amendmentInverse", labelKey: "label.mgmt.weights.amendmentInverse" },
];

const sumOf = (weights) =>
  FIELDS.reduce((total, { key }) => total + (Number(weights[key]) || 0), 0);

const enteredOk = (weights) =>
  FIELDS.every(({ key }) => {
    const value = weights[key];
    return value !== "" && value != null && value >= 0 && value <= 1;
  }) && sumOf(weights) > 0;

const CompositeWeightsPanel = () => {
  const intl = useIntl();
  const query = useServerData(qualityWeightsUrl());
  const invalidateServerData = useInvalidateServerData();
  const [weights, setWeights] = useState(null);
  const [saving, setSaving] = useState(false);
  const [outcome, setOutcome] = useState(null);
  const [scaled, setScaled] = useState(null);

  useEffect(() => {
    if (query.data) {
      setWeights(query.data);
    }
  }, [query.data]);

  // The panel renders after its data loads, too late for the browser's own
  // jump to #composite-weights from the dashboard's "Edit weights" link.
  useEffect(() => {
    if (weights && window.location.hash === "#composite-weights") {
      document.getElementById("composite-weights")?.scrollIntoView();
    }
  }, [weights != null]);

  if (!weights) {
    return null;
  }

  const sum = sumOf(weights);
  const entered = enteredOk(weights);
  const sumsToOne = entered && Math.abs(sum - 1) <= 0.01 + 1e-9;

  const put = (values) => {
    setSaving(true);
    setOutcome(null);
    putToOpenElisServer(
      qualityWeightsUrl(),
      JSON.stringify(values),
      (status) => {
        setSaving(false);
        setOutcome(status === 204 ? "saved" : "saveFailed");
        if (status === 204) {
          invalidateServerData();
        }
      },
    );
  };

  const save = () => {
    const values = Object.fromEntries(
      FIELDS.map(({ key }) => [key, Number(weights[key])]),
    );
    if (sumsToOne) {
      put(values);
    } else {
      setScaled(scaleToOne(values));
    }
  };

  return (
    <Tile
      id="composite-weights"
      className="mgmt-weights"
      data-testid="composite-weights"
    >
      <h3>
        <FormattedMessage id="label.mgmt.weights.title" />
      </h3>
      <p className="mgmt-weights__help">
        <FormattedMessage id="label.mgmt.weights.help" />
      </p>
      <div className="mgmt-weights__fields">
        {FIELDS.map(({ key, labelKey }) => (
          <NumberInput
            key={key}
            id={`mgmt-weight-${key}`}
            label={intl.formatMessage({ id: labelKey })}
            min={0}
            max={1}
            step={0.05}
            allowEmpty
            value={weights[key] ?? ""}
            onChange={(event, { value }) => {
              setOutcome(null);
              setWeights((current) => ({ ...current, [key]: value }));
            }}
          />
        ))}
      </div>
      <p
        className={`mgmt-weights__total${entered ? "" : " mgmt-weights__total--invalid"}`}
        data-testid="composite-weights-total"
        role={entered ? undefined : "alert"}
      >
        <FormattedMessage
          id={
            !entered
              ? "label.mgmt.weights.invalid"
              : sumsToOne
                ? "label.mgmt.weights.total"
                : "label.mgmt.weights.offTotal"
          }
          values={{ 0: sum.toFixed(2) }}
        />
      </p>
      {outcome && (
        <InlineNotification
          kind={outcome === "saved" ? "success" : "error"}
          lowContrast
          hideCloseButton
          title={intl.formatMessage({ id: `label.mgmt.weights.${outcome}` })}
        />
      )}
      <Button
        size="sm"
        disabled={!entered || saving}
        onClick={save}
        data-testid="composite-weights-save"
      >
        <FormattedMessage id="label.button.save" />
      </Button>
      {scaled && (
        <Modal
          open
          modalHeading={intl.formatMessage({
            id: "label.mgmt.weights.scaleTitle",
          })}
          primaryButtonText={intl.formatMessage({
            id: "label.mgmt.weights.scaleConfirm",
          })}
          secondaryButtonText={intl.formatMessage({
            id: "label.button.cancel",
          })}
          onRequestClose={() => setScaled(null)}
          onRequestSubmit={() => {
            setWeights(scaled);
            setScaled(null);
            put(scaled);
          }}
          data-testid="composite-weights-scale"
        >
          <p>
            <FormattedMessage
              id="label.mgmt.weights.scaleBody"
              values={{ 0: sum.toFixed(2) }}
            />
          </p>
          <UnorderedList>
            {FIELDS.map(({ key, labelKey }) => (
              <ListItem key={key}>
                {intl.formatMessage({ id: labelKey })}: {scaled[key].toFixed(2)}
              </ListItem>
            ))}
          </UnorderedList>
        </Modal>
      )}
    </Tile>
  );
};

export default CompositeWeightsPanel;
