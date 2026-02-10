import React, { useState, useEffect, useCallback } from "react";
import {
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Button,
  Tag,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import ProgramForm from "./ProgramForm";

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

  const headers = [
    { key: "name", header: intl.formatMessage({ id: "eqa.program.name" }) },
    {
      key: "description",
      header: intl.formatMessage({ id: "eqa.program.description" }),
    },
    { key: "status", header: intl.formatMessage({ id: "eqa.program.status" }) },
    { key: "actions", header: "" },
  ];

  const rows = programs.map((p) => ({
    id: String(p.id),
    name: p.name,
    description: p.description || "",
    status: p.isActive
      ? intl.formatMessage({ id: "eqa.program.active" })
      : intl.formatMessage({ id: "eqa.program.inactive" }),
    isActive: p.isActive,
    _raw: p,
  }));

  return (
    <div>
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: "1rem",
        }}
      >
        <h3>{intl.formatMessage({ id: "eqa.program.management.title" })}</h3>
        <Button onClick={handleCreate}>
          {intl.formatMessage({ id: "eqa.program.create" })}
        </Button>
      </div>

      {programs.length === 0 ? (
        <p>{intl.formatMessage({ id: "eqa.program.empty" })}</p>
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
                        if (cell.info.header === "status") {
                          return (
                            <TableCell key={cell.id}>
                              <Tag
                                type={rawProgram?.isActive ? "green" : "gray"}
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
                              <Button
                                kind="ghost"
                                size="sm"
                                onClick={() => handleEdit(rawProgram)}
                              >
                                {intl.formatMessage({ id: "eqa.program.edit" })}
                              </Button>
                            </TableCell>
                          );
                        }
                        return (
                          <TableCell key={cell.id}>{cell.value}</TableCell>
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

      {showForm && (
        <ProgramForm program={editingProgram} onClose={handleFormClose} />
      )}
    </div>
  );
};

export default ProgramManagement;
