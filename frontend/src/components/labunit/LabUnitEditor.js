import React, { useState } from "react";
import {
  Grid,
  Column,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  Button,
  Breadcrumb,
  BreadcrumbItem,
} from "@carbon/react";
import {
  ArrowLeft,
  Document,
  Branch,
  Chemistry,
  Layers,
  Report,
  FolderDetails,
  ContainerSoftware,
} from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import BasicInfoTab from "./tabs/BasicInfoTab.js";
import WorkflowsTab from "./tabs/WorkflowsTab.js";
import TestsTab from "./tabs/TestsTab.js";
import PanelsTab from "./tabs/PanelsTab.js";
import ProgramsTab from "./tabs/ProgramsTab.js";
import ProjectsTab from "./tabs/ProjectsTab.js";
import ImportExportTab from "./tabs/ImportExportTab.js";

export default function LabUnitEditor({
  unit,
  initialTab = "basic",
  onBack,
  onSave,
}) {
  const intl = useIntl();
  const [activeTab, setActiveTab] = useState(initialTab);
  const tabs = [
    {
      id: "basic",
      label: intl.formatMessage({ id: "labunit.tabs.basic.info" }),
      icon: Document,
      group: "configuration",
    },
    {
      id: "workflows",
      label: intl.formatMessage({ id: "labunit.tabs.workflows" }),
      icon: Branch,
      group: "configuration",
    },
    {
      id: "tests",
      label: intl.formatMessage({ id: "labunit.tabs.tests" }),
      icon: Chemistry,
      group: "assignments",
    },
    {
      id: "panels",
      label: intl.formatMessage({ id: "labunit.tabs.panels" }),
      icon: Layers,
      group: "assignments",
    },
    {
      id: "programs",
      label: intl.formatMessage({ id: "labunit.tabs.programs" }),
      icon: Report,
      group: "assignments",
    },
    {
      id: "projects",
      label: intl.formatMessage({ id: "labunit.tabs.projects" }),
      icon: FolderDetails,
      group: "assignments",
    },
    {
      id: "import-export",
      label: intl.formatMessage({ id: "labunit.tabs.import.export" }),
      icon: ContainerSoftware,
      group: "data",
    },
  ];
  const handleTabChange = ({ tabIndex }) => {
    setActiveTab(tabIndex);
  };
  const getTabIcon = (tab) => {
    const IconComponent = tab.icon;
    return <IconComponent size={20} />;
  };
  return (
    <>
      <Grid fullWidth>
        <Column lg={16}>
          {/* Header */}
          <div style={{ marginBottom: "2rem" }}>
            <div style={{ marginBottom: "1.5rem" }}>
              <Breadcrumb>
                <BreadcrumbItem>
                  <a href="/admin">
                    {intl.formatMessage({ id: "breadcrumb.admin" })}
                  </a>
                </BreadcrumbItem>
                <BreadcrumbItem>
                  <a href="/admin/lab-units">
                    {intl.formatMessage({ id: "breadcrumb.lab.units" })}
                  </a>
                </BreadcrumbItem>
                <BreadcrumbItem isActive>
                  {unit
                    ? intl.formatMessage(
                        { id: "breadcrumb.edit.lab.unit" },
                        { name: unit.name },
                      )
                    : intl.formatMessage({ id: "breadcrumb.add.lab.unit" })}
                </BreadcrumbItem>
              </Breadcrumb>
            </div>
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "flex-start",
                gap: "1rem",
              }}
            >
              <div
                style={{
                  display: "flex",
                  alignItems: "flex-start",
                  gap: "1rem",
                }}
              >
                <h1 style={{ marginBottom: "0.5rem" }}>
                  {unit
                    ? intl.formatMessage(
                        { id: "labunit.edit.title" },
                        { name: unit.name },
                      )
                    : intl.formatMessage({ id: "labunit.add.title" })}
                </h1>
                <p style={{ color: "#525252", margin: 0 }}>
                  {intl.formatMessage({ id: "labunit.configure.subtitle" })}
                </p>
              </div>
              <div
                style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}
              >
                <Button kind="secondary" onClick={onBack}>
                  {intl.formatMessage({ id: "button.cancel" })}
                </Button>
                <Button kind="primary" onClick={() => onSave && onSave()}>
                  {intl.formatMessage({ id: "button.save.lab.unit" })}
                </Button>
              </div>
            </div>
          </div>
          {/* Main Content with Vertical Tabs */}
          <div style={{ display: "flex", minHeight: "600px" }}>
            <Tabs
              selectedIndex={tabs.findIndex((tab) => tab.id === activeTab)}
              onChange={handleTabChange}
            >
              <TabList
                style={{
                  display: "flex",
                  flexDirection: "column",
                  minWidth: "250px",
                  borderRight: "1px solid #e0e0e0",
                  paddingRight: "1rem",
                  marginRight: "2rem",
                }}
              >
                {/* Configuration Section */}
                <div style={{ marginBottom: "2rem" }}>
                  <div
                    style={{
                      fontSize: "0.75rem",
                      fontWeight: "600",
                      textTransform: "uppercase",
                      color: "#525252",
                      marginBottom: "0.5rem",
                      letterSpacing: "0.32px",
                    }}
                  >
                    {intl.formatMessage({
                      id: "labunit.section.configuration",
                    })}
                  </div>
                  {tabs.slice(0, 2).map((tab, index) => (
                    <Tab
                      key={tab.id}
                      id={tab.id}
                      disabled={!unit && tab.id !== "basic"}
                      style={{
                        display: "flex",
                        alignItems: "center",
                        padding: "0.75rem",
                        marginBottom: "0.25rem",
                        borderRadius: "4px",
                        border: "none",
                        background: "transparent",
                        cursor: "pointer",
                        transition: "background-color 0.15s ease",
                      }}
                    >
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: "0.75rem",
                        }}
                      >
                        {getTabIcon(tab)}
                        <span>{tab.label}</span>
                      </div>
                    </Tab>
                  ))}
                </div>
                {/* Assignments Section */}
                <div style={{ marginBottom: "2rem" }}>
                  <div
                    style={{
                      fontSize: "0.75rem",
                      fontWeight: "600",
                      textTransform: "uppercase",
                      color: "#525252",
                      marginBottom: "0.5rem",
                      letterSpacing: "0.32px",
                    }}
                  >
                    {intl.formatMessage({ id: "labunit.section.assignments" })}
                  </div>
                  {tabs.slice(2, 5).map((tab, index) => (
                    <Tab
                      key={tab.id}
                      id={tab.id}
                      disabled={!unit}
                      style={{
                        display: "flex",
                        alignItems: "center",
                        padding: "0.75rem",
                        marginBottom: "0.25rem",
                        borderRadius: "4px",
                        border: "none",
                        background: "transparent",
                        cursor: "pointer",
                        transition: "background-color 0.15s ease",
                      }}
                    >
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: "0.75rem",
                        }}
                      >
                        {getTabIcon(tab)}
                        <span>{tab.label}</span>
                      </div>
                    </Tab>
                  ))}
                </div>
                {/* Data Section */}
                <div style={{ marginBottom: "2rem" }}>
                  <div
                    style={{
                      fontSize: "0.75rem",
                      fontWeight: "600",
                      textTransform: "uppercase",
                      color: "#525252",
                      marginBottom: "0.5rem",
                      letterSpacing: "0.32px",
                    }}
                  >
                    {intl.formatMessage({ id: "labunit.section.data" })}
                  </div>
                  {tabs.slice(5, 6).map((tab, index) => (
                    <Tab
                      key={tab.id}
                      id={tab.id}
                      disabled={!unit}
                      style={{
                        display: "flex",
                        alignItems: "center",
                        padding: "0.75rem",
                        marginBottom: "0.25rem",
                        borderRadius: "4px",
                        border: "none",
                        background: "transparent",
                        cursor: "pointer",
                        transition: "background-color 0.15s ease",
                      }}
                    >
                      <div
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: "0.75rem",
                        }}
                      >
                        {getTabIcon(tab)}
                        <span>{tab.label}</span>
                      </div>
                    </Tab>
                  ))}
                </div>
              </TabList>
              <TabPanels style={{ flex: 1 }}>
                {tabs.map((tab) => (
                  <TabPanel key={tab.id}>
                    {tab.id === "basic" && <BasicInfoTab unit={unit} />}
                    {tab.id === "workflows" && <WorkflowsTab unit={unit} />}
                    {tab.id === "tests" && <TestsTab unit={unit} />}
                    {tab.id === "panels" && <PanelsTab unit={unit} />}
                    {tab.id === "programs" && <ProgramsTab unit={unit} />}
                    {tab.id === "projects" && <ProjectsTab unit={unit} />}
                    {tab.id === "import-export" && (
                      <ImportExportTab unit={unit} />
                    )}
                  </TabPanel>
                ))}
              </TabPanels>
            </Tabs>
          </div>
        </Column>
      </Grid>
    </>
  );
}
