import React, { useState, useEffect } from "react";
import {
  Button,
  Form,
  Select,
  SelectItem,
  TextInput,
  Column,
  Grid,
  TableContainer,
  Table,
  TableRow,
  TableHeader,
  TableBody,
  TableHead,
  TableCell,
  DataTable,
  TableSelectRow,
  Pagination,
} from "@carbon/react";
import PatientEditByProjectForm from "./patientEdit.js";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";
import { useIntl, FormattedMessage } from "react-intl";

let breadcrumbs = [{ label: "home.label", link: "/" }];

const PatientEditByProject = () => {
  const intl = useIntl();

  const [projectData, setProjectData] = useState({
    subjectNumber: "",
    firstName: "",
    lastName: "",
    gender: "",
  });
  const [data, setData] = useState(null);
  const [selectedCriteria, setSelectedCriteria] = useState("");
  const [inputValue, setInputValue] = useState("");
  const [error, setError] = useState("");
  const [rows, setRows] = useState([]);
  const [selectedRowId, setSelectedRowId] = useState(null);
  const [selectedForm, setSelectedForm] = useState("");
  const [formData, setFormData] = useState({});

  const endpoint = "/rest/PatientEditByProject";

  const headers = [
    { id: 0, header: "ID" },
    { id: 1, header: "Last Name", key: "lastName" },
    { id: 2, header: "First Name", key: "firstName" },
    { id: 3, header: "Gender", key: "gender" },
    { id: 4, header: "Date Of Birth", key: "birthdate" },
    { id: 5, header: "Unique Health ID", key: "subjectNumber" },
    { id: 6, header: "National ID", key: "nationalId" },
  ];

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = () => {
    getFromOpenElisServer(endpoint, (response) => {
      if (response?.patientSearch?.searchCriteria) {
        setData(response);
      } else {
        console.error("Unexpected API response:", response);
        setError("Failed to load search criteria.");
      }
    });
  };

  const handleSelect = (e) => {
    let criteria = e.target.value;
    if (criteria == 2) {
      setSelectedCriteria("lastName");
    } else if (criteria == 1) {
      setSelectedCriteria("firstName");
    } else if (criteria == 4) {
      setSelectedCriteria("subjectNumber");
    } else if (criteria == 5) {
      setSelectedCriteria("labNo");
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const searchEndpoint = `/rest/patient-search?${selectedCriteria}=${encodeURIComponent(inputValue)}`;
    console.log(`Fetching data with endpoint: ${searchEndpoint}`);
    getFromOpenElisServer(searchEndpoint, (response) => {
      if (response) {
        const formattedRows = response.map((patient, index) => ({
          id: index + 1,
          lastName: patient.lastName || "N/A",
          firstName: patient.firstName || "N/A",
          gender: patient.gender || "N/A",
          birthdate: patient.dob || "N/A",
          subjectNumber: patient.subjectNumber || "N/A",
          nationalId: patient.nationalId || "N/A",
        }));
        setRows(formattedRows);
      } else {
        setRows([]);
      }
    });
  };

  const handleRowSelect = (id) => {
    const selectedRow = rows.find((row) => row.id === id);

    // Only update initial values in projectData
    setProjectData({
      subjectNumber: selectedRow.subjectNumber || "",
      firstName: selectedRow.firstName || "",
      lastName: selectedRow.lastName || "",
      gender: selectedRow.gender || "",
    });
    setSelectedRowId(id);
  };

  const handleSelectForm = (e) => {
    const selectedValue = e.target.value;
    setSelectedForm(selectedValue);
  };

  const handleProject = () => {
    console.log("Final project data to submit:", projectData);

    // Build the request body with observations separate from other data
    const requestBody = {
      ...projectData,
      observations: {
        projectFormName: selectedForm,
      },
    };

    // Remove the 'id' attribute from the requestBody
    delete requestBody.id;

    console.log("Request Body (without id):", requestBody);

    // Send the requestBody to the server
    postToOpenElisServerJsonResponse(
      endpoint,
      JSON.stringify(requestBody),
      (response) => {
        if (response) {
          setFormData(response);
        } else {
          console.error("Unexpected API response:", response);
          setError("An error occurred while submitting the data.");
        }
      },
    );
  };

  if (!data) {
    return (
      <div style={{ padding: "1rem" }}>
        <h2>No data available to display</h2>
      </div>
    );
  }

  return (
    <div style={{ padding: "1rem" }}>
      <breadcrumbs breadcrumbs={breadcrumbs} />
      <h1>
        <b>
          <FormattedMessage id="project.edit.title" />{" "}
        </b>
      </h1>

      <h2>
        <b>
          <FormattedMessage id="patient.label.info" />{" "}
        </b>
      </h2>

      <Form onSubmit={handleSubmit}>
        <Grid>
          <Column lg={4} md={8} sm={4}>
            <Select
              id="search-criteria"
              labelText="Search Criteria"
              onChange={handleSelect}
            >
              <SelectItem text="Select Criteria" value={selectedCriteria} />
              {data.patientSearch.searchCriteria.map((item) => (
                <SelectItem key={item.id} value={item.id} text={item.value} />
              ))}
            </Select>
          </Column>
          <Column lg={6} md={2} sm={1}>
            <TextInput
              id="search-query"
              labelText={`Enter ${selectedCriteria}`}
              placeholder={`${selectedCriteria}`}
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
            />
          </Column>
          <Column lg={2} md={1} sm={1}>
            <Button type="submit">Search</Button>
          </Column>
        </Grid>
      </Form>

      <div style={{ marginTop: "2rem" }}>
        <DataTable
          rows={rows}
          headers={headers}
          render={({ rows, headers, getHeaderProps, getRowProps }) => (
            <TableContainer title="Search Results">
              <Table>
                <TableHead>
                  <TableRow>
                    {headers.map((header, index) => (
                      <TableHeader
                        key={header.id || index} // Ensure a unique key is used for each TableHeader
                        {...getHeaderProps({ header })}
                        style={{ padding: "0.5rem", textAlign: "center" }}
                      >
                        {header.header}
                      </TableHeader>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {rows.map((row) => (
                    <TableRow key={row.id} {...getRowProps({ row })}>
                      <TableSelectRow
                        id={row.id}
                        name="patient-selection"
                        onChange={() => handleRowSelect(row.id)}
                      />
                      {row.cells.map((cell) => (
                        <TableCell
                          key={cell.id}
                          style={{ padding: "0.25rem", textAlign: "center" }}
                        >
                          {cell.value || "-"}
                        </TableCell>
                      ))}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        />
      </div>

      <Grid>
        <Column lg={4} md={4} sm={4}>
          <Select id="additional-project" onChange={handleSelectForm}>
            <SelectItem
              text={intl.formatMessage({ id: "project.ARVStudy.name" })}
              value="InitialARV_Id"
            />
            <SelectItem
              text={intl.formatMessage({ id: "project.ARVFollowupStudy.name" })}
              value="FollowUpARV_Id"
            />
            <SelectItem
              text={intl.formatMessage({ id: "project.RTNStudy.name" })}
              value="RTN_Id"
            />
            <SelectItem
              text={intl.formatMessage({
                id: "banner.menu.resultvalidation.viralload",
              })}
              value="VL_Id"
            />
            <SelectItem
              text={intl.formatMessage({ id: "project.EIDStudy.name" })}
              value="EID_Id"
            />
            <SelectItem
              text={intl.formatMessage({ id: "project.Recency.name" })}
              value="Recency_Id"
            />
          </Select>
        </Column>
      </Grid>

      <Button onClick={handleProject}>ADD</Button>
      <hr />
      <PatientEditByProjectForm formData={formData} projectData={projectData} />
    </div>
  );
};

export default PatientEditByProject;
