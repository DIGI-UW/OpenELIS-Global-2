import React, { useEffect, useState } from "react";
import {
  Form,
  Checkbox,
  Grid,
  Column,
  Section,
  Button,
  Loading,
  Dropdown,
  Heading,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer, Roles } from "../../utils/Utils";
import "../../Style.css";
import { AlertDialog } from "../../common/CustomNotification";
import config from "../../../config.json";
import PageBreadCrumb from "../../common/PageBreadCrumb";

const timeFrames = [
  {
    id: "NORMAL_WORK_HOURS",
    description: "Normal Work hours (reception time 9h-15h30)",
  },
  {
    id: "OUT_OF_NORMAL_WORK_HOURS",
    description: "Out of Normal Work Hours (15h31-8h59)",
  },
];

const StatisticsReport = () => {
  const intl = useIntl();
  const [labUnits, setLabUnits] = useState([]);
  const [priorities, setPriorities] = useState([]);
  const [selectedLabUnits, setSelectedLabUnits] = useState([]);
  const [selectedPriorities, setSelectedPriorities] = useState([]);
  const [selectedTimeFrames, setSelectedTimeFrames] = useState([]);
  const [selectedYear, setSelectedYear] = useState({
    value: new Date().getFullYear(),
    label: new Date().getFullYear(),
  });

  const [loading, setLoading] = useState(false);
  const [notificationVisible, setNotificationVisible] = useState(false);

  useEffect(() => {
    getFromOpenElisServer(
      "/rest/user-test-sections/" + Roles.REPORTS,
      (fetchedTestSections) => {
        setLabUnits(fetchedTestSections);
      },
    );
    getFromOpenElisServer("/rest/priorities", (fetchedPriorities) => {
      setPriorities(fetchedPriorities);
    });
  }, []);

  const handleSubmit = () => {
    setLoading(true);

    // Constructing URL based on selected values
    const baseParams = "report=statisticsReport&type=indicator";
    const labUnitsParams = selectedLabUnits
      .map((unit) => `labSections=${encodeURIComponent(unit)}`)
      .join("&");
    const prioritiesParams = selectedPriorities
      .map((priority) => `priority=${encodeURIComponent(priority)}`)
      .join("&");
    const timeFramesParams = selectedTimeFrames
      .map((frame) => `receptionTime=${encodeURIComponent(frame)}`)
      .join("&");
    const yearParam = `upperYear=${encodeURIComponent(selectedYear.value)}`;

    // Constructing the base URL
    const baseUrl = `${config.serverBaseUrl}/ReportPrint`;

    // Constructing the query string
    const queryParams = `${baseParams}&${labUnitsParams}&${prioritiesParams}&${timeFramesParams}&${yearParam}`;

    // Constructing the final URL
    const url = `${baseUrl}?${queryParams}`;

    // Redirect to the constructed URL
    window.open(url, "_blank");

    setLoading(false);
    setNotificationVisible(true);
  };

  const handleYearChange = (year) => {
    setSelectedYear({ value: year.value, label: year.label });
  };

  const handleSelectAllLabUnits = (isChecked) => {
    setSelectedLabUnits(isChecked ? labUnits.map((unit) => unit.id) : []);
  };

  const handleSelectAllPriorities = (isChecked) => {
    setSelectedPriorities(
      isChecked ? priorities.map((priority) => priority.id) : [],
    );
  };

  const handleSelectAllTimeFrames = (isChecked) => {
    setSelectedTimeFrames(isChecked ? timeFrames.map((frame) => frame.id) : []);
  };

  const currentYear = new Date().getFullYear();
  const years = Array.from({ length: currentYear - 2008 }, (_, index) => ({
    value: currentYear - index,
    label: (currentYear - index).toString(),
  }));

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "routine.reports", link: "/RoutineReports" },
    {
      label: "openreports.stat.aggregate",
      link: "/RoutineReport?type=indicator&report=statisticsReport",
    },
  ];

  return (
    <>
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage id="openreports.stat.aggregate" />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Form>
            <Grid fullWidth={true}>
              <Column lg={16} md={8} sm={4}>
                <Section>
                  <br />
                  <br />
                  <h5>
                    <FormattedMessage id="select.labUnits" />
                  </h5>
                </Section>
                <div>
                  <Checkbox
                    labelText="All"
                    id="select-all-lab-units"
                    checked={selectedLabUnits.length === labUnits.length}
                    onChange={(event) =>
                      handleSelectAllLabUnits(event.target.checked)
                    }
                  />
                  {labUnits.map((unit) => (
                    <Checkbox
                      key={unit.id}
                      labelText={intl.formatMessage({
                        id: unit.value,
                        defaultMessage: unit.value,
                      })}
                      id={unit.id}
                      checked={selectedLabUnits.includes(unit.id)}
                      onChange={() => {
                        setSelectedLabUnits((prev) => {
                          if (prev.includes(unit.id)) {
                            return prev.filter((item) => item !== unit.id);
                          } else {
                            return [...prev, unit.id];
                          }
                        });
                      }}
                    />
                  ))}
                </div>
              </Column>
            </Grid>
            <Grid fullWidth={true}>
              <Column lg={16} md={8} sm={4}>
                <Section>
                  <br />
                  <h5>
                    <FormattedMessage id="select.priority.tests" />
                  </h5>
                </Section>
                <div className="inlineDiv">
                  <Checkbox
                    labelText="All"
                    id="select-all-priorities"
                    checked={selectedPriorities.length === priorities.length}
                    onChange={(event) =>
                      handleSelectAllPriorities(event.target.checked)
                    }
                  />
                  {priorities.map((priority) => (
                    <Checkbox
                      key={priority.id}
                      labelText={intl.formatMessage({
                        id: priority.value,
                        defaultMessage: priority.value,
                      })}
                      id={priority.id}
                      checked={selectedPriorities.includes(priority.id)}
                      onChange={() => {
                        setSelectedPriorities((prev) => {
                          if (prev.includes(priority.id)) {
                            return prev.filter((item) => item !== priority.id);
                          } else {
                            return [...prev, priority.id];
                          }
                        });
                      }}
                    />
                  ))}
                </div>
              </Column>
            </Grid>
            <Grid fullWidth={true}>
              <Column lg={16} md={8} sm={4}>
                <Section>
                  <br />
                  <h5>
                    <FormattedMessage id="select.timeFrame" />
                  </h5>
                  <br />
                  <h7>
                    <FormattedMessage id="select.timeFrame.Note" />
                  </h7>
                </Section>
                <div>
                  <Checkbox
                    labelText="All"
                    id="select-all-time-frames"
                    checked={selectedTimeFrames.length === timeFrames.length}
                    onChange={(event) =>
                      handleSelectAllTimeFrames(event.target.checked)
                    }
                  />
                  {timeFrames.map((frame) => (
                    <Checkbox
                      key={frame.id}
                      id={frame.id}
                      labelText={intl.formatMessage({
                        id: frame.description,
                        defaultMessage: frame.description,
                      })}
                      checked={selectedTimeFrames.includes(frame.id)}
                      onChange={() => {
                        setSelectedTimeFrames((prev) => {
                          if (prev.includes(frame.id)) {
                            return prev.filter((item) => item !== frame.id);
                          } else {
                            return [...prev, frame.id];
                          }
                        });
                      }}
                    />
                  ))}
                </div>
              </Column>
            </Grid>
            <Grid fullWidth={true}>
              <Column lg={16} md={8} sm={4}>
                <Section>
                  <br />
                  <h5>
                    <FormattedMessage id="select.year.report" />
                  </h5>
                </Section>
              </Column>
              <Column lg={2} md={2} sm={2}>
                <Dropdown
                  id="year-picker"
                  label="Select Year"
                  selectedItem={selectedYear}
                  onChange={({ selectedItem }) =>
                    handleYearChange(selectedItem)
                  }
                  items={years.map((year) => ({
                    value: year.value,
                    label: year.label,
                  }))}
                />
              </Column>{" "}
            </Grid>
            <br />
            <Section>
              <br />
              <Button type="button" onClick={handleSubmit}>
                <FormattedMessage
                  id="label.button.generatePrintableVersion"
                  defaultMessage="Generate printable version"
                />
              </Button>
            </Section>
          </Form>
        </Column>
      </Grid>
    </>
  );
};

export default StatisticsReport;
