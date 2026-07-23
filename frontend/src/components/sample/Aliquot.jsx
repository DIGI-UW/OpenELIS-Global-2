import React from "react";
import "../Style.css";
import { injectIntl, FormattedMessage, useIntl } from "react-intl";
import AliquotPage from "./AliquotForm";
import { Heading, Grid, Column, Section } from "@carbon/react";
import PageBreadCrumb from "../common/PageBreadCrumb";

function Aliquot() {
  return (
    <>
      <PageBreadCrumb breadcrumbs={[{ label: "home.label", link: "/" }]} />

      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage id="aliquot" />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      <div className="orderLegendBody">
        <AliquotPage />
      </div>
    </>
  );
}

export default injectIntl(Aliquot);
