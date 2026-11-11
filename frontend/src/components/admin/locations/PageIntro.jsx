import React from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Accordion,
  AccordionItem,
  Column,
  Grid,
  Link,
  Stack,
} from "@carbon/react";

const INTRO = {
  organizations: "help.locations.page.organizations",
  sites: "help.locations.page.sites",
  areas: "help.locations.page.areas",
  import: "help.locations.page.import",
};

/**
 * FR-B0: one plain paragraph under each page title, and on Organizations and
 * Sampling Sites a collapsed panel that compares the two side by side.
 */
const PageIntro = ({ view, go }) => {
  const intl = useIntl();
  const compare = view === "organizations" || view === "sites";
  const other = view === "organizations" ? "sites" : "organizations";
  return (
    <Stack gap={3} className="locationsIntro">
      <p className="cds--body-compact-01" data-testid="locations-page-intro">
        <FormattedMessage id={INTRO[view]} />
      </p>
      {compare && (
        <Accordion size="sm">
          <AccordionItem
            title={intl.formatMessage({ id: "label.locations.page.compare" })}
          >
            <Grid condensed>
              <Column lg={8} md={4} sm={4}>
                <h6>
                  <FormattedMessage id="label.locations.page.compare.orgTitle" />
                </h6>
                <p>
                  <FormattedMessage id="help.locations.page.compare.org" />
                </p>
                <p className="cds--label">
                  <FormattedMessage id="label.locations.page.compare.orgExamples" />
                </p>
              </Column>
              <Column lg={8} md={4} sm={4}>
                <h6>
                  <FormattedMessage id="label.locations.page.compare.siteTitle" />
                </h6>
                <p>
                  <FormattedMessage id="help.locations.page.compare.site" />
                </p>
                <p className="cds--label">
                  <FormattedMessage id="label.locations.page.compare.siteExamples" />
                </p>
              </Column>
            </Grid>
            <p className="cds--label">
              <FormattedMessage id="help.locations.page.compare.tip" />{" "}
              <Link
                href="#"
                onClick={(e) => {
                  e.preventDefault();
                  go(other);
                }}
              >
                <FormattedMessage
                  id={
                    other === "sites"
                      ? "sidenav.label.admin.locations.sites"
                      : "sidenav.label.admin.locations.organizations"
                  }
                />
              </Link>
            </p>
          </AccordionItem>
        </Accordion>
      )}
    </Stack>
  );
};

export default PageIntro;
