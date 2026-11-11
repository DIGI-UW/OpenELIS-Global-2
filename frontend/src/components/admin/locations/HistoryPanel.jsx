import React, { useEffect, useState } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Select,
  SelectItem,
  Stack,
  StructuredListBody,
  StructuredListCell,
  StructuredListRow,
  StructuredListWrapper,
  Tile,
} from "@carbon/react";
import { getHistory } from "./locationsApi";

const ACTION_LABELS = {
  CREATED: "label.locations.history.action.created",
  EDITED: "label.locations.history.action.edited",
  DEACTIVATED: "label.locations.history.action.deactivated",
  REACTIVATED: "label.locations.history.action.reactivated",
  MOVED: "label.locations.history.action.moved",
  IDENTIFIERS: "label.locations.history.action.identifiers",
  REFERRAL_REVIEW: "label.locations.history.action.referralReview",
  IMPORTED: "label.locations.history.action.imported",
  REGISTRY_SYNC: "label.locations.history.action.registrySync",
};

/**
 * Section K: the record's change history, newest first, filterable by action.
 * Each entry names who, when, what, and every field's old and new value.
 */
const HistoryPanel = ({ id, name }) => {
  const intl = useIntl();
  const [entries, setEntries] = useState(null);
  const [filter, setFilter] = useState("all");

  useEffect(() => {
    let current = true;
    getHistory(id)
      .then((loaded) => current && setEntries(loaded || []))
      .catch(() => current && setEntries([]));
    return () => {
      current = false;
    };
  }, [id]);

  const actionLabel = (action) =>
    ACTION_LABELS[action]
      ? intl.formatMessage({ id: ACTION_LABELS[action] })
      : action;
  const actions = [...new Set((entries || []).map((e) => e.action))];
  const shown = (entries || []).filter(
    (e) => filter === "all" || e.action === filter,
  );

  return (
    <Tile className="locationsHistory" data-testid="locations-history">
      <Stack orientation="horizontal" gap={5}>
        <h5>
          <FormattedMessage
            id="label.locations.history.title"
            values={{ name }}
          />
        </h5>
        <Select
          id={`history-filter-${id}`}
          size="sm"
          labelText={intl.formatMessage({
            id: "label.locations.history.filter",
          })}
          hideLabel
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
        >
          <SelectItem
            value="all"
            text={intl.formatMessage({
              id: "label.locations.history.allChanges",
            })}
          />
          {actions.map((action) => (
            <SelectItem
              key={action}
              value={action}
              text={actionLabel(action)}
            />
          ))}
        </Select>
        <span className="cds--label">
          <FormattedMessage id="help.locations.history.readOnly" />
        </span>
      </Stack>
      {entries === null ? (
        <p className="cds--label">
          <FormattedMessage id="label.loading" />
        </p>
      ) : (
        <StructuredListWrapper isCondensed>
          <StructuredListBody>
            {shown.map((entry) => (
              <StructuredListRow key={entry.id}>
                <StructuredListCell noWrap>
                  <strong>{actionLabel(entry.action)}</strong>
                  <div className="cds--label">
                    {entry.when} · {entry.user}
                  </div>
                </StructuredListCell>
                <StructuredListCell>
                  {(entry.changes || []).map((change, index) => (
                    <div key={index}>
                      {change.field}:{" "}
                      <s>
                        {change.oldValue ||
                          intl.formatMessage({ id: "label.locations.empty" })}
                      </s>{" "}
                      <strong>
                        {change.newValue ||
                          intl.formatMessage({ id: "label.locations.empty" })}
                      </strong>
                    </div>
                  ))}
                </StructuredListCell>
              </StructuredListRow>
            ))}
          </StructuredListBody>
        </StructuredListWrapper>
      )}
    </Tile>
  );
};

export default HistoryPanel;
