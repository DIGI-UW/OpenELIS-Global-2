/**
 * QAPlaceholder Component
 *
 * Static placeholder pages for the QA menu (QA v0.5 IA rehome):
 * - FUTURE features (Reagent QC, Analyzer Manual QC) — design exists, build not
 *   scheduled; cross-links to the public design doc so UAT can reference it.
 */

import React from "react";
import { Grid, Column, Button, InlineNotification, Tile } from "@carbon/react";
import { Launch } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";

const DESIGN_DOC_BASE =
  "https://github.com/DIGI-UW/openelis-work/blob/main/designs/quality";

const CONTENT = {
  "reagent-qc": {
    titleKey: "sideNav.label.qa.qc.reagentQc",
    future: true,
    questionKey: "qa.future.reagentQc.question",
    summaryKey: "qa.future.reagentQc.summary",
    whyKey: "qa.future.reagentQc.why",
    docUrl: `${DESIGN_DOC_BASE}/batch-workplan-reagent-qc.md`,
  },
  "manual-qc": {
    titleKey: "sideNav.label.qa.qc.manualQc",
    future: true,
    questionKey: "qa.future.manualQc.question",
    summaryKey: "qa.future.manualQc.summary",
    whyKey: "qa.future.manualQc.why",
    docUrl: `${DESIGN_DOC_BASE}/analyzer-manual-qc.md`,
  },
};

const QAPlaceholder = ({ feature }) => {
  const intl = useIntl();
  const content = CONTENT[feature];

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "sideNav.label.qa", link: "" },
    { label: content.titleKey, link: "" },
  ];

  return (
    <div className="adminPageContent">
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <h2>
            <FormattedMessage id={content.titleKey} />
          </h2>
          <InlineNotification
            kind="info"
            hideCloseButton
            lowContrast
            title={intl.formatMessage({
              id: content.future
                ? "qa.future.subtitle"
                : "qa.comingSoon.subtitle",
            })}
            subtitle={
              content.future
                ? intl.formatMessage({ id: content.summaryKey })
                : intl.formatMessage({ id: content.bodyKey })
            }
          />
          {content.future && (
            <Tile style={{ marginTop: "1rem" }}>
              <h4>
                <FormattedMessage id="qa.future.questionLabel" />
              </h4>
              <p>
                <FormattedMessage id={content.questionKey} />
              </p>
              <h4 style={{ marginTop: "1rem" }}>
                <FormattedMessage id="qa.future.whyLabel" />
              </h4>
              <p>
                <FormattedMessage id={content.whyKey} />
              </p>
              <Button
                kind="tertiary"
                renderIcon={Launch}
                href={content.docUrl}
                target="_blank"
                rel="noopener noreferrer"
                style={{ marginTop: "1rem" }}
              >
                <FormattedMessage id="qa.future.readDesignDoc" />
              </Button>
            </Tile>
          )}
        </Column>
      </Grid>
    </div>
  );
};

export default QAPlaceholder;
