import React from "react";
import { useIntl } from "react-intl";
import ReflexRule from "./ReflexRuleForm";
import { Grid, Column, Section, Heading } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "sidenav.label.admin.testmgt.reflex",
    link: "/MasterListsPage/reflex",
  },
];

function ReflexTestManagement() {
  const intl = useIntl();
  return (
    <>
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth={true}>
          <Column lg={16}>
            <Section>
              <Heading>
                <FormattedMessage id="sidenav.label.admin.testmgt.reflex" />
              </Heading>
            </Section>
          </Column>
        </Grid>
        <ReflexRule />
      </div>
    </>
  );
}

export default ReflexTestManagement;
