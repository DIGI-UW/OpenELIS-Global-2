import React, { useState, useEffect, useCallback } from "react";
import {
  Grid,
  Column,
  ClickableTile,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Button,
  Tag,
  Section,
  Heading,
} from "@carbon/react";
import {
  Add,
  Edit,
  TrashCan,
  DataCheck,
  GroupPresentation,
  ChartBar,
  Settings,
} from "@carbon/icons-react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import ProgramForm from "./ProgramForm";
import ParticipantsTab from "./ParticipantsTab";
import SystemSettingsTab from "./SystemSettingsTab";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  {
    label: "sidenav.label.admin.eqaProgram",
    link: "/MasterListsPage/eqaProgram",
  },
];

const CATEGORY_TAG_TYPE = {
  Microbiology: "purple",
  Serology: "teal",
  Chemistry: "blue",
  Hematology: "magenta",
};

const ProgramManagement = () => {
  const intl = useIntl();
  const [programs, setPrograms] = useState([]);
  const [showForm, setShowForm] = useState(false);
  const [editingProgram, setEditingProgram] = useState(null);

  const fetchPrograms = useCallback(() => {
    getFromOpenElisServer("/rest/eqa/programs", (data) => {
      if (data && Array.isArray(data)) {
        setPrograms(data);
      }
    });
  }, []);

  useEffect(() => {
    fetchPrograms();
  }, [fetchPrograms]);

  const handleCreate = () => {
    setEditingProgram(null);
    setShowForm(true);
  };

  const handleEdit = (program) => {
    setEditingProgram(program);
    setShowForm(true);
  };

  const handleFormClose = () => {
    setShowForm(false);
    setEditingProgram(null);
    fetchPrograms();
  };

  const activeCount = programs.filter((p) => p.isActive).length;

  const headers = [
    {
      key: "name",
      header: intl.formatMessage({ id: "eqa.admin.col.programName" }),
    },
    {
      key: "providerName",
      header: intl.formatMessage({ id: "eqa.admin.col.provider" }),
    },
    {
      key: "category",
      header: intl.formatMessage({ id: "eqa.admin.col.category" }),
    },
    {
      key: "frequency",
      header: intl.formatMessage({ id: "eqa.admin.col.frequency" }),
    },
    {
      key: "status",
      header: intl.formatMessage({ id: "eqa.program.status" }),
    },
    {
      key: "actions",
      header: intl.formatMessage({ id: "eqa.admin.col.actions" }),
    },
  ];

  const rows = programs.map((p) => ({
    id: String(p.id),
    name: p.name,
    providerName: p.providerName || "",
    category: p.category || "",
    frequency: p.frequency || "",
    status: p.isActive
      ? intl.formatMessage({ id: "eqa.program.active" })
      : intl.formatMessage({ id: "eqa.program.inactive" }),
    isActive: p.isActive,
    _raw: p,
  }));

  return (
    <div className="adminPageContent">
      <PageBreadCrumb breadcrumbs={breadcrumbs} />

      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>{intl.formatMessage({ id: "eqa.admin.title" })}</Heading>
            <p style={{ color: "#525252", marginBottom: "1.5rem" }}>
              {intl.formatMessage({ id: "eqa.admin.subtitle" })}
            </p>
          </Section>
        </Column>
      </Grid>

      {/* Summary Tiles */}
      <Grid condensed style={{ marginBottom: "1.5rem" }}>
        <Column lg={5} md={4} sm={4}>
          <ClickableTile
            style={{
              backgroundColor: "#f0fdf4",
              borderRadius: "8px",
              border: "1px solid #a7f3d0",
              padding: "1rem",
              minHeight: "120px",
            }}
          >
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: "0.5rem",
                color: "#198038",
                fontWeight: 600,
                fontSize: "0.875rem",
              }}
            >
              <DataCheck size={16} />
              {intl.formatMessage({ id: "eqa.admin.tile.activePrograms" })}
            </div>
            <div
              style={{ fontSize: "2rem", fontWeight: 700, margin: "0.25rem 0" }}
            >
              {activeCount}
            </div>
            <div style={{ fontSize: "0.75rem", color: "#198038" }}>
              {intl.formatMessage(
                { id: "eqa.admin.tile.totalPrograms" },
                { count: programs.length },
              )}
            </div>
          </ClickableTile>
        </Column>
        <Column lg={5} md={4} sm={4}>
          <ClickableTile
            style={{
              backgroundColor: "#f0f4ff",
              borderRadius: "8px",
              border: "1px solid #bfdbfe",
              padding: "1rem",
              minHeight: "120px",
            }}
          >
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: "0.5rem",
                color: "#0043ce",
                fontWeight: 600,
                fontSize: "0.875rem",
              }}
            >
              <GroupPresentation size={16} />
              {intl.formatMessage({
                id: "eqa.admin.tile.enrolledParticipants",
              })}
            </div>
            <div
              style={{ fontSize: "2rem", fontWeight: 700, margin: "0.25rem 0" }}
            >
              —
            </div>
            <div style={{ fontSize: "0.75rem", color: "#0043ce" }}>
              {intl.formatMessage({ id: "eqa.admin.tile.acrossAllPrograms" })}
            </div>
          </ClickableTile>
        </Column>
        <Column lg={5} md={4} sm={4}>
          <ClickableTile
            style={{
              backgroundColor: "#fdf4ff",
              borderRadius: "8px",
              border: "1px solid #e9d5ff",
              padding: "1rem",
              minHeight: "120px",
            }}
          >
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: "0.5rem",
                color: "#7e22ce",
                fontWeight: 600,
                fontSize: "0.875rem",
              }}
            >
              <ChartBar size={16} />
              {intl.formatMessage({ id: "eqa.admin.tile.totalParticipants" })}
            </div>
            <div
              style={{ fontSize: "2rem", fontWeight: 700, margin: "0.25rem 0" }}
            >
              —
            </div>
            <div style={{ fontSize: "0.75rem", color: "#7e22ce" }}>
              {intl.formatMessage({ id: "eqa.admin.tile.acrossAllPrograms" })}
            </div>
          </ClickableTile>
        </Column>
      </Grid>

      {/* Tabs */}
      <div
        style={{
          backgroundColor: "#fff",
          borderRadius: "8px",
          border: "1px solid #e0e0e0",
          padding: "1rem",
        }}
      >
        <Tabs>
          <TabList aria-label="EQA Admin tabs">
            <Tab renderIcon={DataCheck}>
              {intl.formatMessage({ id: "eqa.admin.tab.programs" })}
            </Tab>
            <Tab renderIcon={GroupPresentation}>
              {intl.formatMessage({ id: "eqa.admin.tab.participants" })}
            </Tab>
            <Tab renderIcon={Settings}>
              {intl.formatMessage({ id: "eqa.admin.tab.systemSettings" })}
            </Tab>
          </TabList>
          <TabPanels>
            {/* EQA Programs Tab */}
            <TabPanel>
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  marginBottom: "1rem",
                  marginTop: "1rem",
                }}
              >
                <div>
                  <h4>
                    {intl.formatMessage({ id: "eqa.admin.programs.title" })}
                  </h4>
                  <p
                    style={{
                      color: "#525252",
                      fontSize: "0.875rem",
                      marginTop: "0.25rem",
                    }}
                  >
                    {intl.formatMessage({ id: "eqa.admin.programs.subtitle" })}
                  </p>
                </div>
                <Button renderIcon={Add} onClick={handleCreate}>
                  {intl.formatMessage({ id: "eqa.admin.addProgram" })}
                </Button>
              </div>

              {programs.length === 0 ? (
                <p style={{ color: "#525252", padding: "2rem 0" }}>
                  {intl.formatMessage({ id: "eqa.program.empty" })}
                </p>
              ) : (
                <DataTable rows={rows} headers={headers}>
                  {({
                    rows: tableRows,
                    headers: tableHeaders,
                    getTableProps,
                    getHeaderProps,
                    getRowProps,
                  }) => (
                    <Table {...getTableProps()}>
                      <TableHead>
                        <TableRow>
                          {tableHeaders.map((header) => (
                            <TableHeader
                              key={header.key}
                              {...getHeaderProps({ header })}
                            >
                              {header.header}
                            </TableHeader>
                          ))}
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {tableRows.map((row) => {
                          const rawProgram = programs.find(
                            (p) => String(p.id) === row.id,
                          );
                          return (
                            <TableRow key={row.id} {...getRowProps({ row })}>
                              {row.cells.map((cell) => {
                                if (cell.info.header === "category") {
                                  const tagType =
                                    CATEGORY_TAG_TYPE[cell.value] || "gray";
                                  return (
                                    <TableCell key={cell.id}>
                                      {cell.value ? (
                                        <Tag type={tagType} size="sm">
                                          {cell.value}
                                        </Tag>
                                      ) : (
                                        ""
                                      )}
                                    </TableCell>
                                  );
                                }
                                if (cell.info.header === "status") {
                                  return (
                                    <TableCell key={cell.id}>
                                      <Tag
                                        type={
                                          rawProgram?.isActive
                                            ? "green"
                                            : "gray"
                                        }
                                        size="sm"
                                      >
                                        {cell.value}
                                      </Tag>
                                    </TableCell>
                                  );
                                }
                                if (cell.info.header === "actions") {
                                  return (
                                    <TableCell key={cell.id}>
                                      <div
                                        style={{
                                          display: "flex",
                                          gap: "0.5rem",
                                        }}
                                      >
                                        <Button
                                          kind="ghost"
                                          size="sm"
                                          hasIconOnly
                                          iconDescription={intl.formatMessage({
                                            id: "eqa.program.edit",
                                          })}
                                          renderIcon={Edit}
                                          onClick={() => handleEdit(rawProgram)}
                                        />
                                        <Button
                                          kind="ghost"
                                          size="sm"
                                          hasIconOnly
                                          iconDescription={intl.formatMessage({
                                            id: "eqa.admin.delete",
                                          })}
                                          renderIcon={TrashCan}
                                        />
                                      </div>
                                    </TableCell>
                                  );
                                }
                                return (
                                  <TableCell key={cell.id}>
                                    {cell.value}
                                  </TableCell>
                                );
                              })}
                            </TableRow>
                          );
                        })}
                      </TableBody>
                    </Table>
                  )}
                </DataTable>
              )}
            </TabPanel>

            {/* Participants Tab */}
            <TabPanel>
              <ParticipantsTab programs={programs} />
            </TabPanel>

            {/* System Settings Tab */}
            <TabPanel>
              <SystemSettingsTab />
            </TabPanel>
          </TabPanels>
        </Tabs>
      </div>

      {showForm && (
        <ProgramForm program={editingProgram} onClose={handleFormClose} />
      )}
    </div>
  );
};

export default ProgramManagement;
