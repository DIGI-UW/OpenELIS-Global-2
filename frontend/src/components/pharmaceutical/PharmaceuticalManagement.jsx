import React, { useState } from "react";
import {
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  Grid,
  Column,
} from "@carbon/react";
import { FormattedMessage } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
import SampleAccession from "./SampleAccession";
import AliquotManagement from "./AliquotManagement";
import AssayRunList from "./AssayRunList";
import DisposalWorkflow from "./DisposalWorkflow";
import EnvironmentalExcursions from "./EnvironmentalExcursions";
import "./Pharmaceutical.css";

const breadcrumbs = [
  { label: "home.label", link: "/", defaultMessage: "Home" },
  {
    label: "sidenav.label.pharmaceutical",
    link: "/pharmaceutical",
    defaultMessage: "Pharmaceutical Lab",
  },
];

const PharmaceuticalManagement = () => {
  const [selectedTab, setSelectedTab] = useState(0);

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <div className="pharmaceutical-container">
            <h2>
              <FormattedMessage id="pharmaceutical.title" />
            </h2>

            <Tabs
              selectedIndex={selectedTab}
              onChange={({ selectedIndex }) => setSelectedTab(selectedIndex)}
            >
              <TabList aria-label="Pharmaceutical management tabs" contained>
                <Tab>
                  <FormattedMessage id="pharmaceutical.tab.samples" />
                </Tab>
                <Tab>
                  <FormattedMessage id="pharmaceutical.tab.aliquots" />
                </Tab>
                <Tab>
                  <FormattedMessage id="pharmaceutical.tab.assays" />
                </Tab>
                <Tab>
                  <FormattedMessage id="pharmaceutical.tab.disposal" />
                </Tab>
                <Tab>
                  <FormattedMessage id="pharmaceutical.tab.excursions" />
                </Tab>
              </TabList>

              <TabPanels>
                <TabPanel>
                  <SampleAccession />
                </TabPanel>

                <TabPanel>
                  <AliquotManagement />
                </TabPanel>

                <TabPanel>
                  <AssayRunList />
                </TabPanel>

                <TabPanel>
                  <DisposalWorkflow />
                </TabPanel>

                <TabPanel>
                  <EnvironmentalExcursions />
                </TabPanel>
              </TabPanels>
            </Tabs>
          </div>
        </Column>
      </Grid>
    </>
  );
};

export default PharmaceuticalManagement;
