import React, { useContext, useState } from "react";
import { useIntl, FormattedMessage } from "react-intl";
import {
  Button,
  InlineNotification,
  Modal,
  StructuredListBody,
  StructuredListCell,
  StructuredListRow,
  StructuredListWrapper,
  Tag,
} from "@carbon/react";
import { ConfigurationContext } from "../../layout/Layout";
import { formatIsoDateForBackend } from "../dateUtils";
import { RECORD_KIND } from "../api/orderEntryCleanupApi";

const MATCHED_FIELD_IDS = {
  name: "search.possibleMatches.field.name",
  dateOfBirth: "search.possibleMatches.field.dateOfBirth",
  identifier: "search.possibleMatches.field.identifier",
  code: "search.possibleMatches.field.code",
};

const Highlighted = ({ when, children }) =>
  when ? (
    <mark className="possible-match-highlight">{children}</mark>
  ) : (
    children
  );

/**
 * One possible match: the record's own fields, with the ones it matched on
 * highlighted (FR-B6a).
 */
export const PossibleMatchSummary = ({ kind, match, dateLocale }) => {
  const matched = new Set(match.matchedOn || []);
  if (kind === RECORD_KIND.FACILITY) {
    return (
      <>
        <strong>
          <Highlighted when={matched.has("name")}>{match.name}</Highlighted>
        </strong>
        {match.code && (
          <>
            {" · "}
            <Highlighted when={matched.has("code")}>{match.code}</Highlighted>
          </>
        )}
        {match.city && ` · ${match.city}`}
        {match.active === false && (
          <Tag type="gray" size="sm">
            <FormattedMessage id="search.possibleMatches.inactive" />
          </Tag>
        )}
      </>
    );
  }
  const name =
    kind === RECORD_KIND.PROVIDER
      ? [match.title, match.firstName, match.lastName].filter(Boolean).join(" ")
      : [match.lastName, match.firstName].filter(Boolean).join(", ");
  return (
    <>
      <strong>
        <Highlighted when={matched.has("name")}>{name}</Highlighted>
      </strong>
      {match.birthDate && (
        <>
          {" · "}
          <Highlighted when={matched.has("dateOfBirth")}>
            {formatIsoDateForBackend(match.birthDate, dateLocale)}
          </Highlighted>
        </>
      )}
      {match.identifier && (
        <>
          {" · "}
          <Highlighted when={matched.has("identifier")}>
            {match.identifier}
          </Highlighted>
        </>
      )}
    </>
  );
};

/**
 * Possible matches before Create (FRS clinical order entry v4, FR-B6a, D-216).
 * Lists up to five existing records with Use this one on each; Create new
 * anyway asks for confirmation first. When the check could not run, it says so
 * and still lets the user create the record.
 *
 * Props: open, kind, matches, failed, recording, recordError, onUse(match),
 * onCreateAnyway(), onCancel().
 */
const PossibleMatchesDialog = ({
  open,
  kind,
  matches = [],
  failed = false,
  recording = false,
  recordError = "",
  onUse,
  onCreateAnyway,
  onCancel,
}) => {
  const intl = useIntl();
  const { configurationProperties = {} } =
    useContext(ConfigurationContext) || {};
  const dateLocale = configurationProperties.DEFAULT_DATE_LOCALE || "en-US";
  const [confirming, setConfirming] = useState(false);

  const close = () => {
    setConfirming(false);
    onCancel();
  };

  const primary = () => {
    if (failed || confirming) {
      onCreateAnyway();
      return;
    }
    setConfirming(true);
  };

  return (
    <Modal
      open={open}
      size="md"
      data-testid="possible-matches-dialog"
      modalHeading={intl.formatMessage({ id: "search.possibleMatches.title" })}
      primaryButtonText={intl.formatMessage({
        id: confirming
          ? "search.possibleMatches.confirm.button"
          : "search.possibleMatches.createAnyway",
      })}
      secondaryButtonText={intl.formatMessage({
        id: confirming ? "search.possibleMatches.back" : "label.button.cancel",
      })}
      primaryButtonDisabled={recording}
      onRequestSubmit={primary}
      onSecondarySubmit={() => (confirming ? setConfirming(false) : close())}
      onRequestClose={close}
    >
      {failed ? (
        <InlineNotification
          kind="warning"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({ id: "search.possibleMatches.failed" })}
        />
      ) : confirming ? (
        <p data-testid="possible-matches-confirm">
          <FormattedMessage
            id="search.possibleMatches.confirm"
            values={{ count: matches.length }}
          />
        </p>
      ) : (
        <>
          <p className="helper-text">
            <FormattedMessage id="search.possibleMatches.intro" />
          </p>
          <StructuredListWrapper isCondensed aria-label="possible matches">
            <StructuredListBody>
              {matches.map((match) => (
                <StructuredListRow
                  key={match.id}
                  data-testid={`possible-match-${match.id}`}
                >
                  <StructuredListCell>
                    <PossibleMatchSummary
                      kind={kind}
                      match={match}
                      dateLocale={dateLocale}
                    />
                    <div className="helper-text">
                      <FormattedMessage
                        id="search.possibleMatches.matched"
                        values={{
                          fields: (match.matchedOn || [])
                            .map((field) =>
                              MATCHED_FIELD_IDS[field]
                                ? intl.formatMessage({
                                    id: MATCHED_FIELD_IDS[field],
                                  })
                                : field,
                            )
                            .join(", "),
                        }}
                      />
                    </div>
                  </StructuredListCell>
                  <StructuredListCell>
                    <Button
                      kind="tertiary"
                      size="sm"
                      onClick={() => {
                        setConfirming(false);
                        onUse(match);
                      }}
                      data-testid={`possible-match-use-${match.id}`}
                    >
                      <FormattedMessage id="search.possibleMatches.use" />
                    </Button>
                  </StructuredListCell>
                </StructuredListRow>
              ))}
            </StructuredListBody>
          </StructuredListWrapper>
        </>
      )}
      {recordError && (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={recordError}
        />
      )}
    </Modal>
  );
};

export default PossibleMatchesDialog;
