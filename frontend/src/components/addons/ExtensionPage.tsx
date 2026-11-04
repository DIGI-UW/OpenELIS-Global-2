import React from "react";
import { useParams } from "react-router-dom";
import { FormattedMessage } from "react-intl";
import { Column, Grid, Heading, Section } from "@carbon/react";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { useSlot } from "./ExtensionSlot";
import AddonFrame from "./AddonFrame";
import { SLOT_APP_MENU } from "./slots";

const breadcrumbs = [{ label: "home.label", link: "/" }];

/** The page behind a menu entry an add-on contributes (app-menu-slot). */
export default function ExtensionPage() {
  const { addonId, extension } = useParams<{
    addonId: string;
    extension: string;
  }>();
  const found = useSlot(SLOT_APP_MENU).find(
    (e) => e.addonId === addonId && e.name === extension,
  );
  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>{found ? found.title : addonId}</Heading>
          </Section>
          {found ? (
            <AddonFrame
              extension={found}
              state={{ page: "extension-page" }}
              fill
            />
          ) : (
            <p data-testid="addon-page-missing">
              <FormattedMessage id="addon.page.missing" />
            </p>
          )}
        </Column>
      </Grid>
    </>
  );
}
