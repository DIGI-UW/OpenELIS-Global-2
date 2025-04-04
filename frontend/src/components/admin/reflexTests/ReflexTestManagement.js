import React from "react";
import { injectIntl, FormattedMessage } from "react-intl";
import ReflexRule from "./ReflexRuleForm";
import { Grid, Column, Section, Heading } from "@carbon/react";
import PageBreadCrumb from "../../common/PageBreadCrumb";

// Breadcrumbs setup
const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "sidenav.label.admin.testmgt.reflex",
    link: "/MasterListsPage#reflex",
  },
];

function ReflexTestManagement() {
  return (
    <div className="adminPageContent">
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              <FormattedMessage id="sidenav.label.admin.testmgt.reflex" />
            </Heading>
          </Section>
        </Column>
      </Grid>

      {/* ReflexRule component should internally handle validation */}
      <ReflexRule />
    </div>
  );
}

export default injectIntl(ReflexTestManagement);
