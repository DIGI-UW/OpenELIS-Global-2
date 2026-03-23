import React, { useState, useEffect } from "react";
import {
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableBody,
  TableHeader,
  TableCell,
  TableContainer,
  Loading,
} from "@carbon/react";
import { getFromOpenElisServer } from "../../utils/Utils.js";
import PageBreadCrumb from "../../common/PageBreadCrumb.js";
import { FormattedMessage, useIntl } from "react-intl";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "sidenav.label.admin.pluginFile",
    link: "/MasterListsPage/PluginFile",
  },
];

function PluginList() {
  const intl = useIntl();
  const [plugins, setPlugins] = useState([]);
  const [loading, setLoading] = useState(true);

  const handlePlugins = (res) => {
    if (res) {
      setPlugins(res.pluginList || []);
    }
    setLoading(false);
  };

  useEffect(() => {
    getFromOpenElisServer("/rest/ListPlugins", handlePlugins);
  }, []);

  const headers = [
    {
      key: "pluginName",
      header: intl.formatMessage({ id: "plugin.name" }),
    },
  ];

  const rows = plugins.map((plugin, index) => ({
    id: String(index),
    pluginName: plugin,
  }));

  if (loading) {
    return <Loading />;
  }

  return (
    <div className="adminPageContent">
      <PageBreadCrumb breadcrumbs={breadcrumbs} />

      <div className="orderLegendBody">
        <TableContainer title={intl.formatMessage({ id: "plugin.files" })}>
          {plugins.length === 0 ? (
            <p>
              <FormattedMessage id="message.noPluginFound" />
            </p>
          ) : (
            <DataTable rows={rows} headers={headers}>
              {({ rows, headers, getHeaderProps, getTableProps }) => (
                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
                      {headers.map((header) => (
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
                    {rows.map((row) => (
                      <TableRow key={row.id}>
                        {row.cells.map((cell) => (
                          <TableCell key={cell.id}>{cell.value}</TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </DataTable>
          )}
        </TableContainer>
      </div>
    </div>
  );
}

export default PluginList;
