import React, { useState } from "react";
import {
  Button,
  Heading,
  InlineNotification,
  Section,
  Stack,
  TextInput,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import IdentifiedRow, { ListToolbar } from "../IdentifiedRow";
import {
  blockIdentifier,
  cassetteStateBadge,
  deactivatedCount,
  embeddingSummary,
  isDeactivatedRow,
  isEmbeddedRow,
  isUnsavedRow,
  rowListEditors,
} from "../pathologyRows";
import { embeddingOutstandingLabel } from "../pathologySections";
import "../pathologyCaseView.scss";

/**
 * FR-6.1 to FR-6.3: each cassette cut at grossing is embedded into a block,
 * one at a time, where the bench stores it, and the section names whichever
 * cassettes are still outstanding. Only saved cassettes are listed: a row
 * added at grossing is not cut until the save that names it.
 */
const EmbeddingSection = ({
  caseInfo,
  updateCase,
  readOnly,
  dirty,
  saving,
  embeddingId,
  onEmbed,
}) => {
  const intl = useIntl();
  const [showDeactivated, setShowDeactivated] = useState(false);

  const blocks = caseInfo.blocks ?? [];
  const summary = embeddingSummary(blocks);
  const { patchRow } = rowListEditors("blocks", updateCase);

  return (
    <Stack gap={6}>
      {summary.total === 0 && (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({ id: "pathology.empty.noCassettes" })}
        />
      )}
      {summary.total > 0 && !summary.complete && (
        <InlineNotification
          kind="warning"
          lowContrast
          hideCloseButton
          title={intl.formatMessage(
            { id: "pathology.badge.embeddedOf" },
            { embedded: summary.embedded, total: summary.total },
          )}
          subtitle={embeddingOutstandingLabel(intl, summary.outstanding).full}
        />
      )}
      <div>
        <Section>
          <Heading className="pathology-case-view__heading">
            <FormattedMessage id="pathology.label.cassettes" />
          </Heading>
        </Section>
        {blocks.map((block, index) =>
          isUnsavedRow(block) ? null : (
            <IdentifiedRow
              key={block.id}
              kind="block"
              row={block}
              readOnly={readOnly}
              showDeactivated={showDeactivated}
              stateBadge={cassetteStateBadge(block)}
              withLifecycleAction={false}
              actions={({ disabled }) =>
                isDeactivatedRow(block) || isEmbeddedRow(block) ? null : (
                  <Button
                    kind="tertiary"
                    size="md"
                    id={"embedCassette-" + block.id}
                    aria-label={intl.formatMessage(
                      { id: "pathology.action.embedCassette" },
                      { designation: blockIdentifier(block) },
                    )}
                    disabled={
                      disabled || dirty || saving || embeddingId !== null
                    }
                    title={
                      dirty
                        ? intl.formatMessage({
                            id: "pathology.locked.saveBeforeEmbed",
                          })
                        : undefined
                    }
                    onClick={() => onEmbed(block)}
                  >
                    <FormattedMessage id="pathology.action.embed" />
                  </Button>
                )
              }
            >
              {({ hiddenName, disabled }) => (
                <>
                  <div className="pathology-case-view__row-meta">
                    <span className="pathology-case-view__row-meta-label">
                      <FormattedMessage id="pathology.label.tissueType" />
                    </span>
                    <span>
                      {caseInfo.tissueTypeNames?.[block.tissueTypeId] ?? (
                        <FormattedMessage id="caseView.label.notRecorded" />
                      )}
                    </span>
                  </div>
                  <div className="pathology-case-view__row-field">
                    <TextInput
                      id={"embeddingLocation" + index}
                      disabled={disabled}
                      labelText={
                        <>
                          {intl.formatMessage({
                            id: "common.storageLocation",
                          })}
                          {hiddenName}
                        </>
                      }
                      helperText={intl.formatMessage({
                        id: "pathology.label.storageLocationInterim",
                      })}
                      value={block.location ?? ""}
                      // The row's place in the case's own list, not in the
                      // saved rows shown here, so the edit lands on this row.
                      onChange={(e) =>
                        patchRow(index, { location: e.target.value })
                      }
                    />
                  </div>
                </>
              )}
            </IdentifiedRow>
          ),
        )}
        <ListToolbar
          deactivatedCount={deactivatedCount(blocks)}
          showDeactivated={showDeactivated}
          onToggleDeactivated={setShowDeactivated}
          toggleId="showDeactivatedEmbedding"
        />
      </div>
    </Stack>
  );
};

export default EmbeddingSection;
