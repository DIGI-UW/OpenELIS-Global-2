import React from "react";
import {
  Column,
  DataTable,
  Grid,
  InlineNotification,
  Section,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../components/common/PageBreadCrumb";
import { useModuleHost } from "./ModuleHostProvider";

const STATE_KIND = {
  STARTED: "green",
  MIGRATED: "teal",
  VALIDATED: "teal",
  DISCOVERED: "gray",
  DISABLED: "gray",
  STOPPED: "gray",
  INVALID: "red",
  INCOMPATIBLE: "magenta",
  FAILED: "red",
};

/**
 * Read-only view of the module registry for administrators: what is
 * installed, of which kind and version, whether it started and, if not, why,
 * plus the contract versions this installation runs. Installing, disabling
 * and removing stay file operations in the deployment (docs/modules.md).
 */
export default function ModulesAdminPage() {
  const intl = useIntl();
  const host = useModuleHost();
  const registry = host.registry;
  const modules = registry ? registry.modules : [];
  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "breadcrums.admin.managment", link: "/admin" },
    { label: "modules.admin.title", link: "/admin/modules" },
  ];
  const headers = [
    {
      key: "id",
      header: intl.formatMessage({ id: "modules.admin.column.id" }),
    },
    {
      key: "name",
      header: intl.formatMessage({ id: "modules.admin.column.name" }),
    },
    {
      key: "version",
      header: intl.formatMessage({ id: "modules.admin.column.version" }),
    },
    {
      key: "kind",
      header: intl.formatMessage({ id: "modules.admin.column.kind" }),
    },
    {
      key: "state",
      header: intl.formatMessage({ id: "modules.admin.column.state" }),
    },
    {
      key: "reason",
      header: intl.formatMessage({ id: "modules.admin.column.reason" }),
    },
  ];
  const rows = modules.map((module) => ({
    id: module.id,
    name: module.name,
    version: module.version || "",
    kind: module.kind,
    state: module.state,
    reason:
      module.reason ||
      (host.skipped.find((m) => m.id === module.id) || {}).frontendReason ||
      "",
  }));

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <h1 data-testid="modules-admin-title">
              <FormattedMessage id="modules.admin.title" />
            </h1>
            <p>
              <FormattedMessage id="modules.admin.description" />
            </p>
          </Section>
        </Column>
        <Column lg={16} md={8} sm={4}>
          {host.status === "error" && (
            <InlineNotification
              kind="error"
              lowContrast
              hideCloseButton
              title={intl.formatMessage({ id: "modules.admin.loadError" })}
            />
          )}
          {registry && (
            <p data-testid="modules-admin-versions">
              <FormattedMessage
                id="modules.admin.versions"
                values={{
                  openelis: registry.openelisVersion,
                  coreApi: registry.coreApiVersion,
                  spi: registry.spiVersion,
                  frontend: registry.frontendContractVersion,
                }}
              />
            </p>
          )}
          {registry && modules.length === 0 && (
            <p data-testid="modules-admin-empty">
              <FormattedMessage id="modules.admin.empty" />
            </p>
          )}
          {modules.length > 0 && (
            <DataTable rows={rows} headers={headers} isSortable>
              {({
                rows: tableRows,
                headers: tableHeaders,
                getHeaderProps,
                getRowProps,
                getTableProps,
              }) => (
                <TableContainer>
                  <Table {...getTableProps()} size="md">
                    <TableHead>
                      <TableRow>
                        {tableHeaders.map((header) => (
                          <TableHeader
                            {...getHeaderProps({ header })}
                            key={header.key}
                          >
                            {header.header}
                          </TableHeader>
                        ))}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {tableRows.map((row) => (
                        <TableRow
                          {...getRowProps({ row })}
                          key={row.id}
                          data-testid={`module-row-${row.id}`}
                        >
                          {row.cells.map((cell) => (
                            <TableCell key={cell.id}>
                              {cell.info.header === "state" ? (
                                <Tag
                                  type={STATE_KIND[cell.value] || "gray"}
                                  size="sm"
                                >
                                  {cell.value}
                                </Tag>
                              ) : (
                                cell.value
                              )}
                            </TableCell>
                          ))}
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </DataTable>
          )}
        </Column>
      </Grid>
    </>
  );
}
