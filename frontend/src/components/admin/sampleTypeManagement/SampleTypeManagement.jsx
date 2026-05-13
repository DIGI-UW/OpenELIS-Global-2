import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Form,
  Heading,
  Button,
  Loading,
  Grid,
  Column,
  Section,
  UnorderedList,
  ListItem,
  ClickableTile,
} from "@carbon/react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "configuration.sampleType.manage",
    link: "/MasterListsPage/SampleTypeManagement",
  },
];

function SampleTypeManagement() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const componentMounted = useRef(false);

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                <FormattedMessage id="configuration.sampleType.manage" />
              </Heading>
            </Section>
          </Column>
        </Grid>
        <br />
        <div className="orderLegendBody">
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Section>
                  <Section>
                    <Heading>
                      <FormattedMessage id="configuration.sampleType.organization" />
                    </Heading>
                  </Section>
                </Section>
              </Section>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <UnorderedList>
                <ClickableTile
                  href="/MasterListsPage/SampleTypeRenameEntry"
                  id="SampleTypeRenameEntry"
                >
                  <FormattedMessage id="configuration.type.rename" />
                  <UnorderedList nested>
                    <ListItem>
                      <FormattedMessage id="configuration.type.rename.explain" />
                    </ListItem>
                  </UnorderedList>
                </ClickableTile>
                <br />
                <ClickableTile
                  href="/MasterListsPage/SampleTypeAdd"
                  id="SampleTypeAdd"
                >
                  <FormattedMessage id="configuration.sampleType.create" />
                  <UnorderedList nested>
                    <ListItem>
                      <FormattedMessage id="configuration.sampleType.confirmation.explain" />
                    </ListItem>
                  </UnorderedList>
                </ClickableTile>
                <br />
                <ClickableTile
                  href="/MasterListsPage/SampleTypeActivation"
                  id="SampleTypeActivation"
                >
                  <FormattedMessage id="configuration.sampleType.activate" />
                  <UnorderedList nested>
                    <ListItem>
                      <FormattedMessage id="configuration.sampleType.confirmation.explain" />
                    </ListItem>
                  </UnorderedList>
                </ClickableTile>
                <br />
                <ClickableTile
                  href="/MasterListsPage/SampleTypeBulkImport"
                  id="SampleTypeBulkImport"
                >
                  <FormattedMessage id="eqa.results.batch.import" />
                  <UnorderedList nested>
                    <ListItem>
                      <FormattedMessage id="eqa.results.import.upload" />
                    </ListItem>
                  </UnorderedList>
                </ClickableTile>
                <br />
                <ClickableTile
                  href="/MasterListsPage/SampleTypeTestAssignment"
                  id="SampleTypeTestAssignment"
                >
                  <FormattedMessage id="configuration.panel.assign" />
                  <UnorderedList nested>
                    <ListItem>
                      <FormattedMessage id="configuration.sampleType.assign.explain" />
                    </ListItem>
                  </UnorderedList>
                </ClickableTile>
              </UnorderedList>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
        </div>
      </div>
    </>
  );
}

export default injectIntl(SampleTypeManagement);