import React, { useEffect, useState, useRef, useContext } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../config.json";
import {
  Grid,
  Column,
  Tile,
  Button,
  Section,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Select,
  SelectItem,
  Heading,
  Loading,
  Pagination,
} from "@carbon/react";
import { Search } from "@carbon/react";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { NotificationContext } from "../layout/Layout";
import { AlertDialog } from "../common/CustomNotification";
import { getFromOpenElisServer } from "../utils/Utils";
import "./../pathology/PathologyDashboard.css";

function GeneralProgrammeDashboard() {
  const [programmes, setProgrammes] = useState([]);
  const [filteredProgrammes, setFilteredProgrammes] = useState([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const componentMounted = useRef(false);
  const { notificationVisible } = useContext(NotificationContext);
  const intl = useIntl();

  useEffect(() => {
    componentMounted.current = true;

    getFromOpenElisServer("/rest/programs/general", (data) => {
      if (componentMounted.current) {
        if (data && Array.isArray(data)) {
          const programmesWithIds = data.map((program) => ({
            ...program,
            id: program.id.toString(),
            displayName:
              program.name ||
              program.programName ||
              program.displayName ||
              "Unknown Program",
          }));
          setProgrammes(programmesWithIds);
          setFilteredProgrammes(programmesWithIds);
        } else {
          console.error("Invalid data received from API:", data);
          setProgrammes([]);
          setFilteredProgrammes([]);
        }
        setLoading(false);
      }
    });

    return () => {
      componentMounted.current = false;
    };
  }, []);

  useEffect(() => {
    if (searchTerm) {
      const filtered = programmes.filter((program) =>
        program.displayName.toLowerCase().includes(searchTerm.toLowerCase()),
      );
      setFilteredProgrammes(filtered);
    } else {
      setFilteredProgrammes(programmes);
    }
  }, [searchTerm, programmes]);

  const handleProgrammeClick = (programme) => {
    // Navigate to the case view for this programme
    window.location.href = `/GeneralProgrammeCaseView/${programme.id}`;
  };

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    {
      label: "banner.menu.generalProgramme",
      link: "/GeneralProgrammeDashboard",
    },
  ];

  const tileList = [
    {
      title: intl.formatMessage({ id: "banner.menu.generalProgramme" }),
      count: programmes.length,
    },
    {
      title: intl.formatMessage({ id: "label.displayed.programmes" }),
      count: filteredProgrammes.length,
    },
  ];

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      {loading && <Loading description="Loading Dashboard..." />}
      <PageBreadCrumb breadcrumbs={breadcrumbs} />

      <Grid fullWidth={true}>
        <Column lg={16}>
          <Section>
            <Heading>
              <FormattedMessage id="banner.menu.generalProgramme" />
            </Heading>
          </Section>
        </Column>
      </Grid>

      <div className="dashboard-container">
        {tileList.map((tile, index) => (
          <Tile key={index} className="dashboard-tile">
            <h3 className="tile-title">{tile.title}</h3>
            <p className="tile-value">{tile.count}</p>
          </Tile>
        ))}
      </div>

      <div className="orderLegendBody">
        <Grid fullWidth={true} className="gridBoundary">
          <Column lg={8} md={4} sm={2}>
            <Search
              size="sm"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder={intl.formatMessage({ id: "label.search.programme" })}
              labelText={intl.formatMessage({ id: "label.search.programme" })}
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <DataTable
              rows={filteredProgrammes.slice(
                (page - 1) * pageSize,
                page * pageSize,
              )}
              headers={[
                {
                  key: "displayName",
                  header: intl.formatMessage({ id: "programme.name" }),
                },
                {
                  key: "description",
                  header: intl.formatMessage({ id: "programme.description" }),
                },
              ]}
              isSortable
            >
              {({ rows, headers, getHeaderProps, getTableProps }) => (
                <TableContainer title="" description="">
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
                        <TableHeader>
                          <FormattedMessage id="label.button.view" />
                        </TableHeader>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {rows.map((row) => (
                        <TableRow key={row.id}>
                          {row.cells.map((cell) => (
                            <TableCell key={cell.id}>{cell.value}</TableCell>
                          ))}
                          <TableCell>
                            <Button
                              kind="primary"
                              size="sm"
                              onClick={() => handleProgrammeClick(row)}
                            >
                              <FormattedMessage id="label.button.view" />
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </DataTable>

            <Pagination
              onChange={({ page, pageSize }) => {
                setPage(page);
                setPageSize(pageSize);
              }}
              page={page}
              pageSize={pageSize}
              pageSizes={[10, 20, 30, 50]}
              totalItems={filteredProgrammes.length}
              forwardText={intl.formatMessage({ id: "pagination.forward" })}
              backwardText={intl.formatMessage({ id: "pagination.backward" })}
            />
          </Column>
        </Grid>
      </div>
    </>
  );
}

export default GeneralProgrammeDashboard;
