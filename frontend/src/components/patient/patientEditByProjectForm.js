import React, { useState, useEffect } from 'react';
import {
  Button,
  Form,
  Select,
  SelectItem,
  TextInput,
  DataTable,
  Table,
  TableHead,
  TableBody,
  TableRow,
  TableHeader,
  TableContainer,
  TableCell,
  Column,
  Grid
} from "@carbon/react";
import { getFromOpenElisServer } from '../utils/Utils';
import { useIntl ,FormattedMessage } from 'react-intl';
import PageBreadCrumb from "../common/PageBreadCrumb.js";

let breadcrumbs = [{ label: "home.label", link: "/" }];

const PatientEditByProject = () => {
  const intl = useIntl();
  const [scan, setScan] = useState(false); // Scan state
  const [data, setData] = useState(null); // Initialize as null to handle asynchronous data loading
  const [selectedCriteria, setSelectedCriteria] = useState(""); // Track selected criteria
  const [inputValue, setInputValue] = useState(""); // Track text input value
  const [rows, setRows] = useState([]); // For table rows
  const endpoint = "/rest/PatientEditByProject";

  // Fetch initial data for search criteria
  const fetchData = () => {
    getFromOpenElisServer(endpoint, (response) => {
      if (response?.patientSearch?.searchCriteria) {
        setData(response);
      } else {
        console.error("Unexpected API response:", response);
      }
    });
  };

  useEffect(() => {
    fetchData(); // Fetch data on component mount
  }, []);

  // Handle form submission
  const handleSubmit = (e) => {
    e.preventDefault(); // Prevent page reload

    if (!selectedCriteria || !inputValue) {
      alert("Please select a search criteria and enter a value.");
      return;
    }

    const queryParams = new URLSearchParams({
      criteria: selectedCriteria,
      value: inputValue,
    }).toString();

    getFromOpenElisServer(`${endpoint}?${queryParams}`, (response) => {
      if (response?.patientSearch?.results) {
        const transformedRows = response.patientSearch.results.map((patient, index) => ({
          id: index.toString(),
          firstName: patient.firstName,
          lastName: patient.lastName,
          gender: patient.gender,
          birthdate: patient.birthdate,
          subjectNumber: patient.subjectNumber,
          nationalId: patient.nationalId,
        }));
        setRows(transformedRows);
      } else {
        console.error("Unexpected API response:", response);
        setRows([]);
      }
    });
  };

  if (!data) {
    // Show a loading state until data is fetched
    return <div>Loading...</div>;
  }

  // Table headers
  const headers = [
    { key: "firstName", header: "First Name" },
    { key: "lastName", header: "Last Name" },
    { key: "gender", header: "Gender" },
    { key: "birthdate", header: "Date of Birth" },
    { key: "subjectNumber", header: "Subject Number" },
    { key: "nationalId", header: "National ID" },
  ];

  return (
    <div style={{ padding: "1rem" }}>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <h1><b>
        <FormattedMessage id="banner.menu.patientConsult" />{" "}
        <FormattedMessage id="banner.menu.patient" /></b></h1>

<h2><b><FormattedMessage id="patient.label.info" /> </b></h2>
      <Form onSubmit={handleSubmit}>
      <Grid>
      <Column lg={4} md={8} sm={4}>
        <Select
          id="search-criteria"
          labelText="Search Criteria"
          style={{ marginBottom: "10px" }}
          onChange={(e) => {
            const value = e.target.value;
            setSelectedCriteria(value);
            setScan(value === "patient.lab.no"); // Dynamically set scan state
          }}
        >
          <SelectItem text="Select Criteria" value="" />
          {data.patientSearch.searchCriteria.map((item) => (
            <SelectItem key={item.id} value={item.id} text={item.value} />
          ))}
        </Select>
        </Column>
        <Column lg={6} md={4} sm={2}>
              <TextInput
                id="search-input"
                labelText="Search Input"
                placeholder="Enter your search value"
                value={inputValue}
                onChange={(e) => {
                  const value = e.target.value;
                  setInputValue(value);
                  if (selectedCriteria === "patient.lab.no") {
                    setScan(true);
                  } else {
                    setScan(false);
                  }
                }}
                style={{ marginBottom: "10px" }}
              />
      </Column>
        <Column lg={2} md={2} sm={1}>
                <Button type="submit">Search</Button>
                  
        </Column>
          {scan && (
             <Column lg={3} md={2} sm={1}>
             <p  style={{margin:"10px"}} ><b><FormattedMessage id="referral.input" /> </b></p>
           </Column>)}
           </Grid>
      </Form>

      {/* Render DataTable */}
      <DataTable rows={rows} headers={headers}>
        {({ rows, headers, getHeaderProps, getRowProps }) => (
          <TableContainer title="Search Results">
            <Table>
              <TableHead>
                <TableRow>
                  {headers.map((header) => (
                    <TableHeader key={header.key} {...getHeaderProps({ header })}>
                      {header.header}
                    </TableHeader>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((row) => (
                  <TableRow key={row.id} {...getRowProps({ row })}>
                    {row.cells.map((cell) => (
                      <TableCell key={cell.id}>{cell.value}</TableCell>
                    ))}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </DataTable>

      <Select id="additional-project">
        <SelectItem text={intl.formatMessage({ id: "project.ARVStudy.name" })} />
        <SelectItem text={intl.formatMessage({ id: "project.ARVFollowupStudy.name" })} />
        <SelectItem text={intl.formatMessage({ id: "project.RTNStudy.name" })} />
        <SelectItem text={intl.formatMessage({ id: "banner.menu.resultvalidation.viralload" })} />
        <SelectItem text={intl.formatMessage({ id: "project.EIDStudy.name" })} />
        <SelectItem text={intl.formatMessage({ id: "project.Recency.name" })} />
      </Select>
    </div>
  );
};

export default PatientEditByProject;
