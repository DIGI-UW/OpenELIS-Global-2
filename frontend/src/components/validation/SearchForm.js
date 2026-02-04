import React, { useState, useEffect, useContext } from "react";
import {
  Button,
  Column,
  Grid,
  Section,
  Select,
  SelectItem,
  TextInput,
  DatePicker,
  DatePickerInput,
  Checkbox,
  Stack,
} from "@carbon/react";
import { Filter, Search, Close } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";
import { NotificationContext } from "../layout/Layout";
import { NotificationKinds } from "../common/CustomNotification";

const SearchForm = ({ setResults, setIsLoading, setSearchParams }) => {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  const [searchQuery, setSearchQuery] = useState("");
  const [selectedLabUnit, setSelectedLabUnit] = useState("");
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(false);

  const [labNumberFrom, setLabNumberFrom] = useState("");
  const [labNumberTo, setLabNumberTo] = useState("");
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");
  const [testSection, setTestSection] = useState("");
  const [analyzer, setAnalyzer] = useState("");
  const [enteredBy, setEnteredBy] = useState("");
  const [showFlaggedOnly, setShowFlaggedOnly] = useState(false);
  const [showNormalOnly, setShowNormalOnly] = useState(false);
  const [showAbnormalOnly, setShowAbnormalOnly] = useState(false);

  const [labUnits, setLabUnits] = useState([]);
  const [testSections, setTestSections] = useState([]);
  const [analyzers] = useState([
    "Sysmex XN-L",
    "Sysmex XS-1000i",
    "Cobas c 501",
    "Cobas e 411",
    "Manual",
  ]);
  const [users] = useState([
    "J. Smith",
    "M. Johnson",
    "K. Davis",
    "A. Williams",
  ]);

  useEffect(() => {
    getFromOpenElisServer("/rest/user-test-sections/Validation", (data) => {
      setLabUnits(data || []);
      if (data && data.length > 0) {
        setTestSections(data.map((unit) => unit.value));
      }
    });
  }, []);

  useEffect(() => {
    const urlParams = new URLSearchParams(window.location.search);
    const typeParam = urlParams.get("type");
    const testParam = urlParams.get("test");
    const accessionNumberParam = urlParams.get("accessionNumber");

    if (accessionNumberParam && accessionNumberParam.trim()) {
      setSearchQuery(accessionNumberParam);
      setIsLoading(true);
      const params = new URLSearchParams();
      params.append("q", accessionNumberParam);
      params.append("doRange", "false"); // Direct sample lookup by accession number
      if (setSearchParams) {
        setSearchParams(params.toString());
      }
      getFromOpenElisServer(
        `/rest/AccessionValidation?${params.toString()}`,
        (data) => {
          setIsLoading(false);
          if (data && data.resultList && data.resultList.length > 0) {
            setResults(data);
          } else {
            setResults({ resultList: [] });
            addNotification({
              kind: NotificationKinds.info,
              title: intl.formatMessage({ id: "notification.title" }),
              message: intl.formatMessage({
                id: "validation.search.noresults",
              }),
            });
            setNotificationVisible(true);
          }
        },
      );
      return;
    }

    if (typeParam) {
      if (labUnits.length > 0) {
        const matchingUnit = labUnits.find(
          (unit) =>
            unit.value.toLowerCase() === typeParam.toLowerCase() ||
            unit.label?.toLowerCase() === typeParam.toLowerCase(),
        );
        if (matchingUnit) {
          setSelectedLabUnit(matchingUnit.id);
          setIsLoading(true);
          const searchParams = new URLSearchParams();
          searchParams.append("labUnit", matchingUnit.id);
          if (setSearchParams) {
            setSearchParams(searchParams.toString());
          }
          getFromOpenElisServer(
            `/rest/AccessionValidation?${searchParams.toString()}`,
            (data) => {
              setIsLoading(false);
              if (data && data.resultList && data.resultList.length > 0) {
                setResults(data);
              } else {
                setResults({ resultList: [] });
              }
            },
          );
        }
      }
    }

    if (testParam && testParam.trim()) {
      setSearchQuery(testParam);
    }
  }, [labUnits]);

  const handleSearch = (e) => {
    if (e) e.preventDefault();

    if (!selectedLabUnit && !searchQuery) {
      addNotification({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({
          id: "validation.search.required",
        }),
      });
      setNotificationVisible(true);
      return;
    }

    setIsLoading(true);

    const params = new URLSearchParams();

    if (searchQuery) {
      params.append("q", searchQuery);
      // When searching by accession number without lab unit, use doRange=false
      // to trigger direct sample lookup instead of range-based search
      if (!selectedLabUnit) {
        params.append("doRange", "false");
      }
    }
    if (selectedLabUnit) {
      params.append("labUnit", selectedLabUnit);
    }
    if (labNumberFrom) {
      params.append("labNumberFrom", labNumberFrom);
    }
    if (labNumberTo) {
      params.append("labNumberTo", labNumberTo);
    }
    if (dateFrom) {
      params.append("dateFrom", dateFrom);
    }
    if (dateTo) {
      params.append("dateTo", dateTo);
    }
    if (testSection) {
      params.append("testSection", testSection);
    }
    if (analyzer) {
      params.append("analyzer", analyzer);
    }
    if (enteredBy) {
      params.append("enteredBy", enteredBy);
    }
    if (showFlaggedOnly) {
      params.append("flagged", "true");
    }
    if (showNormalOnly) {
      params.append("normal", "true");
    }
    if (showAbnormalOnly) {
      params.append("normal", "false");
    }

    if (setSearchParams) {
      setSearchParams(params.toString());
    }

    getFromOpenElisServer(
      `/rest/AccessionValidation?${params.toString()}`,
      (data) => {
        setIsLoading(false);
        if (data && data.resultList && data.resultList.length > 0) {
          setResults(data);
        } else {
          setResults({ resultList: [] });
          addNotification({
            kind: NotificationKinds.info,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "validation.search.noresults",
            }),
          });
          setNotificationVisible(true);
        }
      },
    );
  };

  const handleClear = () => {
    setSearchQuery("");
    setSelectedLabUnit("");
    setLabNumberFrom("");
    setLabNumberTo("");
    setDateFrom("");
    setDateTo("");
    setTestSection("");
    setAnalyzer("");
    setEnteredBy("");
    setShowFlaggedOnly(false);
    setShowNormalOnly(false);
    setShowAbnormalOnly(false);
    setResults({ resultList: [] });
  };

  const handleLabUnitChange = (e) => {
    const value = e.target.value;
    setSelectedLabUnit(value);
    if (value) {
      triggerSearchWithLabUnit(value);
    }
  };

  const triggerSearchWithLabUnit = (labUnitValue) => {
    setIsLoading(true);

    const params = new URLSearchParams();
    params.append("labUnit", labUnitValue);

    if (setSearchParams) {
      setSearchParams(params.toString());
    }

    getFromOpenElisServer(
      `/rest/AccessionValidation?${params.toString()}`,
      (data) => {
        setIsLoading(false);
        if (data && data.resultList && data.resultList.length > 0) {
          setResults(data);
        } else {
          setResults({ resultList: [] });
          addNotification({
            kind: NotificationKinds.info,
            title: intl.formatMessage({ id: "notification.title" }),
            message: intl.formatMessage({
              id: "validation.search.noresults",
            }),
          });
          setNotificationVisible(true);
        }
      },
    );
  };

  const handleQuickFilterChange = (filterType) => {
    if (filterType === "flagged") {
      setShowFlaggedOnly(!showFlaggedOnly);
      setShowNormalOnly(false);
      setShowAbnormalOnly(false);
    } else if (filterType === "normal") {
      setShowNormalOnly(!showNormalOnly);
      setShowFlaggedOnly(false);
      setShowAbnormalOnly(false);
    } else if (filterType === "abnormal") {
      setShowAbnormalOnly(!showAbnormalOnly);
      setShowFlaggedOnly(false);
      setShowNormalOnly(false);
    }
  };

  return (
    <Section className="validation-search-section">
      <Grid className="validation-search-grid">
        <Column lg={16}>
          <form onSubmit={handleSearch}>
            <Stack gap={4}>
              <Grid>
                <Column lg={4} md={4} sm={4}>
                  <Select
                    id="lab-unit-select"
                    labelText={intl.formatMessage({
                      id: "validation.search.labunit",
                    })}
                    value={selectedLabUnit}
                    onChange={handleLabUnitChange}
                  >
                    <SelectItem
                      text={intl.formatMessage({
                        id: "validation.search.labunit.placeholder",
                      })}
                      value=""
                    />
                    {labUnits.map((unit) => (
                      <SelectItem
                        key={unit.id}
                        text={unit.value}
                        value={unit.id}
                      />
                    ))}
                  </Select>
                </Column>

                <Column lg={1} md={1} sm={1} className="or-separator">
                  <div style={{ textAlign: "center", paddingTop: "2rem" }}>
                    <FormattedMessage id="label.or" />
                  </div>
                </Column>

                <Column lg={7} md={7} sm={7}>
                  <TextInput
                    id="search-query"
                    labelText={intl.formatMessage({
                      id: "validation.search.query",
                    })}
                    placeholder={intl.formatMessage({
                      id: "validation.search.query.placeholder",
                    })}
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                  />
                </Column>

                <Column lg={2} md={2} sm={2}>
                  <Button
                    type="submit"
                    style={{ marginTop: "1.5rem" }}
                    renderIcon={Search}
                    disabled={!selectedLabUnit && !searchQuery}
                  >
                    <FormattedMessage id="label.button.search" />
                  </Button>
                </Column>

                <Column lg={2} md={2} sm={2}>
                  <Button
                    kind={showAdvancedFilters ? "primary" : "secondary"}
                    style={{ marginTop: "1.5rem" }}
                    renderIcon={Filter}
                    onClick={() => setShowAdvancedFilters(!showAdvancedFilters)}
                  >
                    <FormattedMessage id="validation.filters.toggle" />
                  </Button>
                </Column>
              </Grid>

              {showAdvancedFilters && (
                <Grid className="advanced-filters-panel">
                  <Column lg={16}>
                    <h4>
                      <FormattedMessage id="validation.filters.advanced" />
                    </h4>
                  </Column>

                  <Column lg={4} md={4} sm={4}>
                    <TextInput
                      id="lab-number-from"
                      labelText={intl.formatMessage({
                        id: "validation.filter.labnumber.from",
                      })}
                      placeholder="DEV0125000"
                      value={labNumberFrom}
                      onChange={(e) => setLabNumberFrom(e.target.value)}
                    />
                  </Column>

                  <Column lg={4} md={4} sm={4}>
                    <TextInput
                      id="lab-number-to"
                      labelText={intl.formatMessage({
                        id: "validation.filter.labnumber.to",
                      })}
                      placeholder="DEV0125999"
                      value={labNumberTo}
                      onChange={(e) => setLabNumberTo(e.target.value)}
                    />
                  </Column>

                  <Column lg={4} md={4} sm={4}>
                    <DatePicker
                      datePickerType="single"
                      onChange={(dates) => {
                        if (dates && dates.length > 0) {
                          setDateFrom(dates[0].toISOString().split("T")[0]);
                        }
                      }}
                    >
                      <DatePickerInput
                        id="date-from"
                        labelText={intl.formatMessage({
                          id: "validation.filter.date.from",
                        })}
                        placeholder="mm/dd/yyyy"
                      />
                    </DatePicker>
                  </Column>

                  <Column lg={4} md={4} sm={4}>
                    <DatePicker
                      datePickerType="single"
                      onChange={(dates) => {
                        if (dates && dates.length > 0) {
                          setDateTo(dates[0].toISOString().split("T")[0]);
                        }
                      }}
                    >
                      <DatePickerInput
                        id="date-to"
                        labelText={intl.formatMessage({
                          id: "validation.filter.date.to",
                        })}
                        placeholder="mm/dd/yyyy"
                      />
                    </DatePicker>
                  </Column>

                  <Column lg={4} md={4} sm={4}>
                    <Select
                      id="test-section"
                      labelText={intl.formatMessage({
                        id: "validation.filter.testsection",
                      })}
                      value={testSection}
                      onChange={(e) => setTestSection(e.target.value)}
                    >
                      <SelectItem
                        text={intl.formatMessage({
                          id: "validation.filter.all",
                        })}
                        value=""
                      />
                      {testSections.map((section) => (
                        <SelectItem
                          key={section}
                          text={section}
                          value={section}
                        />
                      ))}
                    </Select>
                  </Column>

                  <Column lg={4} md={4} sm={4}>
                    <Select
                      id="analyzer"
                      labelText={intl.formatMessage({
                        id: "validation.filter.analyzer",
                      })}
                      value={analyzer}
                      onChange={(e) => setAnalyzer(e.target.value)}
                    >
                      <SelectItem
                        text={intl.formatMessage({
                          id: "validation.filter.all",
                        })}
                        value=""
                      />
                      {analyzers.map((ana) => (
                        <SelectItem key={ana} text={ana} value={ana} />
                      ))}
                    </Select>
                  </Column>

                  <Column lg={4} md={4} sm={4}>
                    <Select
                      id="entered-by"
                      labelText={intl.formatMessage({
                        id: "validation.filter.enteredby",
                      })}
                      value={enteredBy}
                      onChange={(e) => setEnteredBy(e.target.value)}
                    >
                      <SelectItem
                        text={intl.formatMessage({
                          id: "validation.filter.all",
                        })}
                        value=""
                      />
                      {users.map((user) => (
                        <SelectItem key={user} text={user} value={user} />
                      ))}
                    </Select>
                  </Column>

                  <Column lg={4} md={4} sm={4}>
                    <fieldset className="cds--fieldset">
                      <legend className="cds--label">
                        <FormattedMessage id="validation.filter.quickfilters" />
                      </legend>
                      <Checkbox
                        id="filter-flagged"
                        labelText={intl.formatMessage({
                          id: "validation.filter.flagged",
                        })}
                        checked={showFlaggedOnly}
                        onChange={() => handleQuickFilterChange("flagged")}
                      />
                      <Checkbox
                        id="filter-normal"
                        labelText={intl.formatMessage({
                          id: "validation.filter.normal",
                        })}
                        checked={showNormalOnly}
                        onChange={() => handleQuickFilterChange("normal")}
                      />
                      <Checkbox
                        id="filter-abnormal"
                        labelText={intl.formatMessage({
                          id: "validation.filter.abnormal",
                        })}
                        checked={showAbnormalOnly}
                        onChange={() => handleQuickFilterChange("abnormal")}
                      />
                    </fieldset>
                  </Column>

                  <Column lg={16}>
                    <div
                      style={{
                        display: "flex",
                        justifyContent: "flex-end",
                        gap: "1rem",
                        marginTop: "1rem",
                      }}
                    >
                      <Button
                        kind="secondary"
                        renderIcon={Close}
                        onClick={handleClear}
                      >
                        <FormattedMessage id="label.button.clear" />
                      </Button>
                      <Button
                        type="button"
                        onClick={handleSearch}
                        disabled={!selectedLabUnit && !searchQuery}
                      >
                        <FormattedMessage id="validation.filter.apply" />
                      </Button>
                    </div>
                  </Column>
                </Grid>
              )}
            </Stack>
          </form>
        </Column>
      </Grid>
    </Section>
  );
};

export default SearchForm;
