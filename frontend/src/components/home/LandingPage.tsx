import React, { useState, useEffect, useContext } from "react";
import {
  Grid,
  Column,
  TextInput,
  Button,
  Checkbox,
  Tile,
  Form,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer, postToOpenElisServer } from "../utils/Utils";
import { ConfigurationContext } from "../layout/Layout";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";

const LandingPage: React.FC = () => {
  const intl = useIntl();
  const [departments, setDepartments] = useState([]);
  const [filteredDepartments, setFilteredDepartments] = useState([]);
  const [selectedDepartment, setSelectedDepartment] = useState("");
  const [rememberChoice, setRememberChoice] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const { configurationProperties } =
    useContext<ConfigurationContext>(ConfigurationContext);
  const { userSessionDetails } = useContext<UserSessionDetailsContext>(
    UserSessionDetailsContext,
  );

  interface UserSessionDetailsContext {
    userSessionDetails: any;
  }

  interface ConfigurationContext {
    configurationProperties: any;
  }

  useEffect(() => {
    if (
      configurationProperties.REQUIRE_LAB_UNIT_AT_LOGIN === "false" ||
      userSessionDetails.loginLabUnit
    ) {
      const refererUrl = document.referrer;
      if (refererUrl.endsWith("/landing")) {
        window.location.href = "/";
      } else {
        window.location.href = refererUrl;
      }
    }
    getFromOpenElisServer("/rest/user-test-sections/ALL", (response) => {
      setDepartments(response);
      setFilteredDepartments(response);
    });
  }, []);

  const handleSearch = (event) => {
    const term = event.target.value.toLowerCase();
    setSearchTerm(term);
    setFilteredDepartments(
      departments.filter((dept) => dept.value.toLowerCase().includes(term)),
    );
  };

  const handleDepartmentSelect = (departmentId) => {
    setSelectedDepartment(departmentId);
  };

  const handleContinue = () => {
    if (selectedDepartment) {
      // if(rememberChoice){
      //   localStorage.setItem("selectedDepartment", selectedDepartment);
      // }
      postToOpenElisServer(
        "/rest/setUserLoginLabUnit/" + selectedDepartment,
        {},
        handlePostLabUbit,
      );
    }
    const refererUrl = document.referrer;
    if (refererUrl.endsWith("/landing")) {
      window.location.href = "/";
    } else {
      window.location.href = refererUrl;
    }
  };

  const handlePostLabUbit = (status) => {};

  return (
    <Grid
      fullWidth
      className="landing-page"
      style={{
        minHeight: "100vh",
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        padding: "1rem",
        backgroundColor: "#f0f4f8",
      }}
    >
      <Column
        lg={8}
        md={6}
        sm={4}
        className="landing-page-column"
        style={{
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          maxWidth: "500px",
          width: "100%",
        }}
      >
        <Tile
          style={{
            padding: "2rem",
            textAlign: "center",
            width: "100%",
            maxWidth: "500px",
            backgroundColor: "white",
            borderRadius: "12px",
            boxShadow: "0 4px 12px rgba(0, 0, 0, 0.1)",
          }}
        >
          <h2>Welcome!</h2>
          <p>Please select a unit to continue</p>
          <Form>
            <TextInput
              id="department-search"
              labelText="Search for a department"
              value={searchTerm}
              onChange={handleSearch}
              light
            />
            <div
              className="department-list"
              style={{
                maxHeight: "400px",
                overflowY: "auto",
                marginTop: "1rem",
                marginBottom: "1rem",
                textAlign: "left",
                border: "1px solid #e0e0e0",
                borderRadius: "4px",
                padding: "0.5rem",
                backgroundColor: "#f9f9f9",
              }}
            >
              {filteredDepartments?.map((dept) => (
                <div
                  key={dept.id}
                  className={`department-item ${
                    selectedDepartment === dept.id ? "selected" : ""
                  }`}
                  style={{
                    padding: "0.75rem",
                    cursor: "pointer",
                    borderRadius: "4px",
                    marginBottom: "0.5rem",
                    backgroundColor:
                      selectedDepartment === dept.id ? "#c6c6c6" : "inherit",
                    borderColor:
                      selectedDepartment === dept.id
                        ? "#0f62fe"
                        : "transparent",
                    transition: "background-color 0.3s, border-color 0.3s",
                  }}
                  onClick={() => handleDepartmentSelect(dept.id)}
                >
                  {dept.value}
                </div>
              ))}
            </div>
            {/* <Checkbox
              id="remember-choice"
              labelText="Remember my choice"
              checked={rememberChoice}
              onChange={(e) => setRememberChoice(e.target.checked)}
            /> */}
            <Button
              onClick={handleContinue}
              disabled={!selectedDepartment}
              style={{ marginTop: "1rem", width: "100%", maxWidth: "100%" }}
            >
              Continue
            </Button>
          </Form>
        </Tile>
      </Column>
    </Grid>
  );
};

export default LandingPage;
