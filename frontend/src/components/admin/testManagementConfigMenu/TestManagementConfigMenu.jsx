import React, { useContext } from "react";
import {
  Heading,
  Grid,
  Column,
  Section,
  UnorderedList,
  ListItem,
  ClickableTile,
} from "@carbon/react";
import { NotificationContext } from "../../layout/Layout";
import { AlertDialog } from "../../common/CustomNotification";
import { FormattedMessage, injectIntl } from "react-intl";
import useInAppNavigation from "../../common/useInAppNavigation";
import PageBreadCrumb from "../../common/PageBreadCrumb";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "master.lists.page.test.management",
    link: "/MasterListsPage/testManagementConfigMenu",
  },
];

/**
 * The catalogue editors: each one creates, edits, renames, orders, activates
 * and assigns its entity in one place, so there is one tile per entity.
 */
const CATALOGUE_TILES = [
  {
    id: "TestCatalogList",
    href: "/MasterListsPage/TestCatalogList",
    label: "sidenav.label.admin.testmgt.testCatalogEditor",
    explain: "configuration.testCatalog.tests.explain",
  },
  {
    id: "PanelsList",
    href: "/MasterListsPage/TestCatalogList?entity=panels",
    label: "label.testCatalog.entity.panels",
    explain: "configuration.testCatalog.panels.explain",
  },
  {
    id: "SampleTypeEditor",
    href: "/MasterListsPage/SampleTypeEditor",
    label: "sidenav.label.admin.sampleTypeManagement",
    explain: "configuration.testCatalog.sampleTypes.explain",
  },
  {
    id: "LabUnitManagement",
    href: "/MasterListsPage/LabUnitManagement",
    label: "sidenav.label.admin.labUnitManagement",
    explain: "configuration.testCatalog.labUnits.explain",
  },
  {
    id: "TestCatalog",
    href: "/MasterListsPage/TestCatalog",
    label: "configuration.test.catalog",
    explain: "configuration.test.catalog.explain",
  },
  {
    id: "TestActivation",
    href: "/MasterListsPage/TestActivation",
    label: "configuration.test.activate",
    explain: "configuration.test.activate.explain",
  },
  {
    id: "TestOrderability",
    href: "/MasterListsPage/TestOrderability",
    label: "configuration.test.orderable",
    explain: "configuration.test.orderable.explain",
  },
  {
    id: "PanelOrder",
    href: "/MasterListsPage/PanelOrder",
    label: "configuration.panel.order",
    explain: "configuration.panel.order.explain",
  },
  {
    id: "MethodManagement",
    href: "/MasterListsPage/MethodManagement",
    label: "configuration.method",
    explain: "configuration.testCatalog.methods.explain",
  },
  {
    id: "UnitsOfMeasure",
    href: "/MasterListsPage/UnitsOfMeasure",
    label: "configuration.uom.title",
    explain: "configuration.testCatalog.uom.explain",
  },
  {
    id: "ResultSelectListAdd",
    href: "/MasterListsPage/ResultSelectListAdd",
    label: "configuration.selectList.add",
    explain: "configuration.selectList.add.explain",
  },
  {
    id: "SelectListRenameEntry",
    href: "/MasterListsPage/SelectListRenameEntry",
    label: "configuration.selectList.rename",
    explain: "configuration.selectList.rename.explain",
  },
  {
    id: "CatalogImport",
    href: "/MasterListsPage/CatalogImport",
    label: "sidenav.label.admin.catalogImport",
    explain: "configuration.testCatalog.import.explain",
  },
];

const RULE_TILES = [
  {
    id: "reflex",
    href: "/MasterListsPage/reflex",
    label: "sidenav.label.admin.testmgt.reflex",
  },
  {
    id: "calculatedValue",
    href: "/MasterListsPage/calculatedValue",
    label: "sidenav.label.admin.testmgt.calculated",
  },
  {
    id: "programs",
    href: "/MasterListsPage/program",
    label: "admin.programs.title",
    explain: "admin.programs.subtitle",
  },
  {
    id: "ComplianceStandardsAdmin",
    href: "/MasterListsPage/ComplianceStandardsAdmin",
    label: "compliance.admin.title",
    explain: "compliance.admin.tile.explain",
  },
];

const Tiles = ({ tiles }) => {
  const navigate = useInAppNavigation();
  return (
    <UnorderedList>
      {tiles.map((tile, index) => (
        <React.Fragment key={tile.id}>
          {index > 0 && <br />}
          <ClickableTile
            href={tile.href}
            id={tile.id}
            onClick={navigate(tile.href)}
          >
            <FormattedMessage id={tile.label} />
            {tile.explain && (
              <UnorderedList nested>
                <ListItem>
                  <FormattedMessage id={tile.explain} />
                </ListItem>
              </UnorderedList>
            )}
          </ClickableTile>
        </React.Fragment>
      ))}
    </UnorderedList>
  );
};

const SectionHeading = ({ id }) => (
  <Grid fullWidth={true}>
    <Column lg={16} md={8} sm={4}>
      <Section>
        <Section>
          <Section>
            <Heading>
              <FormattedMessage id={id} />
            </Heading>
          </Section>
        </Section>
      </Section>
    </Column>
  </Grid>
);

function TestManagementConfigMenu() {
  const { notificationVisible } = useContext(NotificationContext);

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
          <SectionHeading id="sidenav.label.admin.testCatalog" />
          <br />
          <hr />
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Tiles tiles={CATALOGUE_TILES} />
            </Column>
          </Grid>
          <br />
          <hr />
          <br />
          <SectionHeading id="configuration.test.management.organization" />
          <br />
          <hr />
          <br />
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Tiles tiles={RULE_TILES} />
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
