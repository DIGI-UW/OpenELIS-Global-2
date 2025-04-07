import React from "react";
import { useState, useEffect, useContext } from "react";
import { Grid, Column, Button, Tile, Form, Search } from "@carbon/react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer, postToOpenElisServer } from "../utils/Utils";
import { ConfigurationContext } from "../layout/Layout";
import UserSessionDetailsContext from "../../UserSessionDetailsContext";
import { Building, ArrowRight } from "@carbon/react/icons";

const LandingPage: React.FC = () => {
  const intl = useIntl();
  const [departments, setDepartments] = useState([]);
  const [filteredDepartments, setFilteredDepartments] = useState([]);
  const [selectedDepartment, setSelectedDepartment] = useState("");
  const [rememberChoice, setRememberChoice] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [loading, setLoading] = useState(true);
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
      setLoading(false);
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
        backgroundColor: "#f4f7fb",
        backgroundImage: "linear-gradient(to bottom right, #f4f7fb, #e0e4e8)",
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
            padding: "2.5rem",
            textAlign: "center",
            width: "100%",
            maxWidth: "500px",
            backgroundColor: "white",
            borderRadius: "12px",
            boxShadow: "0 8px 24px rgba(0, 0, 0, 0.12)",
            border: "none",
          }}
        >
          <div style={{ marginBottom: "1.5rem" }}>
            <Building size={48} style={{ color: "#0f62fe" }} />
            <h2
              style={{
                fontSize: "1.75rem",
                fontWeight: "600",
                marginTop: "1rem",
                color: "#161616",
              }}
            >
              Welcome!
            </h2>
            <p
              style={{
                fontSize: "1rem",
                color: "#6f6f6f",
                marginTop: "0.5rem",
              }}
            >
              Please select a unit to continue
            </p>
          </div>
          <Form>
            <Search
              id="department-search"
              labelText="Search for a department"
              placeholder="Search departments..."
              value={searchTerm}
              onChange={handleSearch}
              size="lg"
              style={{ marginBottom: "1.5rem" }}
            />
            <div
              className="department-list"
              style={{
                maxHeight: "300px",
                overflowY: "auto",
                marginBottom: "1.5rem",
                textAlign: "left",
                border: "1px solid #e0e0e0",
                borderRadius: "8px",
                padding: "0.5rem",
                backgroundColor: "#f9f9f9",
              }}
            >
              {loading ? (
                <div
                  style={{
                    padding: "2rem",
                    textAlign: "center",
                    color: "#6f6f6f",
                  }}
                >
                  Loading departments...
                </div>
              ) : filteredDepartments.length === 0 ? (
                <div
                  style={{
                    padding: "2rem",
                    textAlign: "center",
                    color: "#6f6f6f",
                  }}
                >
                  No departments found
                </div>
              ) : (
                filteredDepartments?.map((dept) => (
                  <div
                    key={dept.id}
                    className={`department-item ${selectedDepartment === dept.id ? "selected" : ""}`}
                    style={{
                      padding: "0.85rem 1rem",
                      cursor: "pointer",
                      borderRadius: "6px",
                      marginBottom: "0.5rem",
                      backgroundColor:
                        selectedDepartment === dept.id ? "#e8f1ff" : "white",
                      border: "1px solid",
                      borderColor:
                        selectedDepartment === dept.id ? "#0f62fe" : "#e0e0e0",
                      transition: "all 0.2s ease",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "space-between",
                    }}
                    onClick={() => handleDepartmentSelect(dept.id)}
                  >
                    <span
                      style={{
                        fontWeight:
                          selectedDepartment === dept.id ? "600" : "400",
                        color:
                          selectedDepartment === dept.id
                            ? "#0f62fe"
                            : "#161616",
                      }}
                    >
                      {dept.value}
                    </span>
                    {selectedDepartment === dept.id && (
                      <ArrowRight size={16} style={{ color: "#0f62fe" }} />
                    )}
                  </div>
                ))
              )}
            </div>
            {/* <Checkbox
              id="remember-choice"
              labelText="Remember my choice"
              checked={rememberChoice}
              onChange={(e) => setRememberChoice(e.target.checked)}
            /> */}
            <Button
              onClick={handleContinue}
              disabled={!selectedDepartment || loading}
              style={{
                marginTop: "1rem",
                width: "100%",
                maxWidth: "100%",
                height: "48px",
                fontSize: "1rem",
              }}
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
