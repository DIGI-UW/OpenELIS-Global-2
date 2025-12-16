import React, { useContext, useState, useEffect, useRef } from "react";
import {
  Form,
  Heading,
  Button,
  Loading,
  Grid,
  Column,
  Section,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableBody,
  TableHeader,
  TableCell,
  TableSelectRow,
  TableSelectAll,
  TableContainer,
  Pagination,
  Search,
  Select,
  SelectItem,
  Stack,
  UnorderedList,
  ListItem,
  ClickableTile,
} from "@carbon/react";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerFormData,
  postToOpenElisServerFullResponse,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils.js";
import { NotificationContext } from "../../layout/Layout.js";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification.js";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb.js";
import CustomCheckBox from "../../common/CustomCheckBox.js";
import ActionPaginationButtonType from "../../common/ActionPaginationButtonType.js";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "master.lists.page.test.management",
    link: "/MasterListsPage#testManagementConfigMenu",
  },
];

function TestManagementConfigMenu() {
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
                <FormattedMessage id="master.lists.page.test.management" />
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
                      <FormattedMessage id="configuration.test.management.spelling" />
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
                  href="/admin#TestRenameEntry"
                  id="TestRenameEntry"
                >
                  <FormattedMessage id="configuration.test.rename" />
                  <UnorderedList nested>
                    <ListItem>
                      <FormattedMessage id="configuration.test.rename.explain" />
                    </ListItem>
                  </UnorderedList>
                </ClickableTile>
                <br />
                <ClickableTile
                  href="/admin#PanelRenameEntry"
                  id="PanelRenameEntry"
                >
                  <FormattedMessage id="configuration.panel.rename" />
                  <UnorderedList nested>
                    <ListItem>
                      <FormattedMessage id="configuration.panel.rename.explain" />
                    </ListItem>
                  </UnorderedList>
                </ClickableTile>
                <br />
                <ClickableTile
                  href="/admin#SampleTypeRenameEntry"
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
                  href="/admin#TestSectionRenameEntry"
                  id="TestSectionRenameEntry"
                >
                  <FormattedMessage id="configuration.testSection.rename" />
                  <UnorderedList nested>
                    <ListItem>
                      <FormattedMessage id="configuration.testSection.rename.explain" />
                    </ListItem>
                  </UnorderedList>
                </ClickableTile>
                <br />
                <ClickableTile href="/admin#UomRenameEntry" id="UomRenameEntry">
                  <FormattedMessage id="configuration.uom.rename" />
                  <UnorderedList nested>
                    <ListItem>
                      <FormattedMessage id="configuration.uom.rename.explain" />
                    </ListItem>
                  </UnorderedList>
                </ClickableTile>
                <br />
                <ClickableTile
                  href="/admin#SelectListRenameEntry"
                  id="SelectListRenameEntry"
                >
                  <FormattedMessage id="configuration.selectList.rename" />
                  <UnorderedList nested>
                    <ListItem>
                      <FormattedMessage id="configuration.selectList.rename.explain" />
                    </ListItem>
                  </UnorderedList>
                </ClickableTile>
                <br />
                <ClickableTile
                  href="/admin#MethodRenameEntry"
                  id="MethodRenameEntry"
                >
                  <FormattedMessage id="configuration.method.rename" />
                  <UnorderedList nested>
                    <ListItem>
                      <FormattedMessage id="configuration.method.rename.explain" />
                    </ListItem>
                  </UnorderedList>
                </ClickableTile>
              </UnorderedList>
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Section>
                  <Section>
                    <Heading>
                      <FormattedMessage id="configuration.test.management.organization" />
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
                <ClickableTile href="/admin#TestCatalog" id="TestCatalog">
                  <FormattedMessage id="configuration.test.catalog" />
                  <UnorderedList nested>
                    <ListItem>
                      <FormattedMessage id="configuration.test.catalog.explain" />
                    </ListItem>
                  </UnorderedList>
                </ClickableTile>
                <br />
                <ClickableTile
                  href="/admin#MethodManagement"
                  id="MethodManagement"
                >
                  <FormattedMessage id="configuration.method" />
                  <UnorderedList nested>
                    <ListItem>
                      <FormattedMessage id="configuration.method.explain" />
                    </ListItem>
                  </UnorderedList>
                </ClickableTile>
                <br />
                <ClickableTile href="/admin#TestAdd" id="TestAdd">
                  <FormattedMessage id="configuration.test.add" />
                  <UnorderedList nested>
                    <ListItem>
                      <FormattedMessage id="configuration.test.add.explain" />
                    </ListItem>
                  </UnorderedList>
                </ClickableTile>
                <br />
                <ClickableTile
                  href="/admin#TestModifyEntry"
                  id="TestModifyEntry"
                >
                  <FormattedMessage id="configuration.test.modify" />
                  <UnorderedList nested>
                    <ListItem>
                      <FormattedMessage id="configuration.test.modify.explain" />
                    </ListItem>
                  </UnorderedList>
                </ClickableTile>
                <br />
                <ClickableTile href="/admin#TestActivation" id="TestActivation">
                  <FormattedMessage id="configuration.test.activate" />
                  <UnorderedList nested>
                    <ListItem>
                      <FormattedMessage id="configuration.test.activate.explain" />
                    </ListItem>
                  </UnorderedList>
                </ClickableTile>
                <br />
                <ClickableTile
                  href="/admin#TestOrderability"
                  id="TestOrderability"
                >
                  <FormattedMessage id="configuration.test.orderable" />
                  <UnorderedList nested>
                    <ListItem>
                      <FormattedMessage id="configuration.test.orderable.explain" />
                    </ListItem>
                  </UnorderedList>
                </ClickableTile>
                <br />
                <ClickableTile
                  href="/admin#TestSectionManagement"
                  id="TestSectionManagement"
                >
                  <FormattedMessage id="configuration.testUnit.manage" />
                  <UnorderedList nested>
                    <ListItem>
                      <FormattedMessage id="configuration.testUnit.manage.explain" />
                    </ListItem>
                  </UnorderedList>
                </ClickableTile>
                <br />
                <ClickableTile
                  href="/admin#SampleTypeManagement"
                  id="SampleTypeManagement"
                >
                  <FormattedMessage id="configuration.sampleType.manage" />
                  <UnorderedList nested>
                    <ListItem>
                      <FormattedMessage id="configuration.sampleType.manage.explain" />
                    </ListItem>
                  </UnorderedList>
                </ClickableTile>
                <br />
                <ClickableTile href="/admin#UomManagement" id="UomManagement">
                  <FormattedMessage id="configuration.uom.manage" />
                  <UnorderedList nested>
                    <ListItem>
                      <FormattedMessage id="configuration.uom.manage.explain" />
                    </ListItem>
                  </UnorderedList>
                </ClickableTile>
                <br />
                <ClickableTile
                  href="/admin#PanelManagement"
                  id="PanelManagement"
                >
                  <FormattedMessage id="configuration.panel.manage" />
                  <UnorderedList nested>
                    <ListItem>
                      <FormattedMessage id="configuration.panel.manage.explain" />
                    </ListItem>
                  </UnorderedList>
                </ClickableTile>
                <br />
                <ClickableTile
                  href="/admin#ResultSelectListAdd"
                  id="ResultSelectListAdd"
                >
                  <FormattedMessage id="configuration.selectList.add" />
                  <UnorderedList nested>
                    <ListItem>
                      <FormattedMessage id="configuration.selectList.add.explain" />
                    </ListItem>
                    <ListItem>
                      <FormattedMessage id="configuration.selectList.add.alert" />
                    </ListItem>
                  </UnorderedList>
                </ClickableTile>
                <br />
                <ClickableTile href="#reflex" id="reflex">
                  <FormattedMessage id="sidenav.label.admin.testmgt.reflex" />
                </ClickableTile>
                <br />
                <ClickableTile href="#calculatedValue" id="calculatedValue">
                  <FormattedMessage id="sidenav.label.admin.testmgt.calculated" />
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

export default injectIntl(TestManagementConfigMenu);
