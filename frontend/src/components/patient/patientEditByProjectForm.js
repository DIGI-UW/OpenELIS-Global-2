import React, { useState, useEffect } from "react";
import {
  Button,
  Form,
  Select,
  SelectItem,
  TextInput,
  Column,
  Grid,
} from "@carbon/react";
import { getFromOpenElisServer, postToOpenElisServer2 } from "../utils/Utils";
import { useIntl, FormattedMessage } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb.js";
import PatientEditByProjectForm from "./patientEdit.js";

let breadcrumbs = [{ label: "home.label", link: "/" }];

const PatientEditByProject = () => {
  const intl = useIntl();
  const [info, setInfo] = useState({});
  const [selectedForm, setSelectedForm] = useState("");
  const [data, setData] = useState(null);
  const [selectedCriteria, setSelectedCriteria] = useState("");
  const [inputValue, setInputValue] = useState("");
  const [loading, setLoading] = useState(false); // Added loading state
  const [error, setError] = useState(""); // Added error state
  const endpoint = "/rest/PatientEditByProject";

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = () => {
    setLoading(true); // Set loading to true when fetching
    getFromOpenElisServer(endpoint, (response) => {
      setLoading(false); // Set loading to false after data is fetched
      if (response?.patientSearch?.searchCriteria) {
        setData(response);
      } else {
        console.error("Unexpected API response:", response);
        setError("Failed to load search criteria.");
      }
    });
  };

  const handleSubmit = (e) => {
    e.preventDefault();

    if (!selectedCriteria || !inputValue) {
      alert("Please select a search criteria and enter a value.");
      return;
    }

    let field = "";
    switch (selectedCriteria) {
      case "1":
        field = "firstName";
        break;
      case "2":
        field = "lastName";
        break;
      case "3":
        field = "centerName";
        break;
      case "4":
        field = "subjectNumber";
        break;
      case "5":
        field = "LabNo";
        break;
      default:
        alert("Invalid criteria");
        return;
    }

    const requestBody = {
      [field]: inputValue,
      observations: {
        projectFormName: selectedForm,
      },
    };

    setLoading(true); // Show loading spinner while submitting
    postToOpenElisServer2(endpoint, JSON.stringify(requestBody), (response) => {
      setLoading(false); // Hide loading spinner after response
      if (response) {
        console.log(response);
        setInfo(response);
      } else {
        console.error("Unexpected API response:", response);
        setError("An error occurred while submitting the data.");
      }
    });
  };

  if (loading) {
    return (
      <div style={{ padding: "1rem" }}>
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
      </div>
    );
  }

  if (error) {
    return (
      <div style={{ padding: "1rem" }}>
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <p style={{ color: "red" }}>{error}</p>
      </div>
    );
  }

  if (!data) {
    return (
      <div style={{ padding: "1rem" }}>
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <h2>No data available to display</h2>
      </div>
    );
  }

  return (
    <div style={{ padding: "1rem" }}>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
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
              onChange={(e) => setSelectedCriteria(e.target.value)}
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
              onChange={(e) => setInputValue(e.target.value)}
            />
          </Column>
          <Column lg={2} md={2} sm={1}>
            <Button type="submit" disabled={loading}>
              Search
            </Button>
          </Column>
        </Grid>
      </Form>
      <Grid>
        <Column lg={4} md={4} sm={4}>
          <Select
            id="additional-project"
            labelText={intl.formatMessage({ id: "project.select.label" })}
            onChange={(e) => setSelectedForm(e.target.value)}
          >
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
      <hr />
      {info && <PatientEditByProjectForm formData={info} />}
    </div>
  );
};

export default PatientEditByProject;
