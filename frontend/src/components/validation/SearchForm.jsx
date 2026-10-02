import React, { useState, useEffect, useRef, useContext } from "react";
import {
  Button,
  Column,
  DatePicker,
  DatePickerInput,
  Grid,
  InlineNotification,
  Loading,
  Search,
  Select,
  SelectItem,
  Tag,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer, Roles } from "../utils/Utils";
import { format, isValid, parse } from "date-fns";
import SearchPatientForm from "../patient/SearchPatientForm";
import { ConfigurationContext, NotificationContext } from "../layout/Layout";
import { NotificationKinds } from "../common/CustomNotification";
import {
  isEmptySearch,
  searchFromLocation,
  searchQueryString,
  validationEndpoint,
} from "./validationSearch";
import "../resultPage/unified/unified-results.scss";
import "./validation-search.scss";

const patientDisplayName = (patient) =>
  [patient?.firstName, patient?.lastName].filter(Boolean).join(" ") +
  (patient?.subjectNumber ? ` (${patient.subjectNumber})` : "");

const replaceValidationUrl = (search) => {
  window.history.replaceState(
    window.history.state,
    "",
    `/validation${searchQueryString(search)}`,
  );
};

/**
 * OGC-1418 — Validation's one search, laid out like Results Entry: a search
 * box for a lab number or a lab number range, a Lab Unit (only units the user
 * validates), a date range and a patient, all combined, and kept in the
 * address so a refresh or a bookmark reopens the same queue. The old menu
 * entries' addresses open this page with the equivalent filter.
 */
const SearchForm = (props) => {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext) || {};
  // The server reads dates in the site's format, as the old By Date search
  // sent them. The calendar keeps the handlers it was created with, which can
  // be before the site's format has loaded, so it is remounted when it loads.
  const dayFirst = configurationProperties?.DEFAULT_DATE_LOCALE === "fr-FR";
  const displayFormat = dayFirst ? "dd/MM/yyyy" : "MM/dd/yyyy";
  const parseDisplayDate = (text) => {
    const parsed = parse(text, displayFormat, new Date());
    return isValid(parsed) ? parsed : undefined;
  };
  const formatDate = (date) => (date ? format(date, displayFormat) : "");

  const [search, setSearch] = useState(() =>
    searchFromLocation(window.location.pathname, window.location.search),
  );
  const [testSections, setTestSections] = useState([]);
  const [selectedPatient, setSelectedPatient] = useState(null);
  const [showPatientSearch, setShowPatientSearch] = useState(false);
  const [searchResults, setSearchResults] = useState();
  const [isLoading, setIsLoading] = useState(false);
  const [loadError, setLoadError] = useState(false);
  const [url, setUrl] = useState("");
  const searchRef = useRef(search);

  const validationResults = (data, announceEmpty = true) => {
    setIsLoading(false);
    if (!data || (typeof data.status === "number" && data.status >= 400)) {
      setLoadError(true);
      setSearchResults({ resultList: [], searched: true });
      return;
    }
    setLoadError(false);
    const resultList = (data.resultList || []).map((row, id) => ({
      ...row,
      id,
    }));
    setSearchResults({ ...data, resultList, searched: true });
    if (announceEmpty && resultList.length === 0) {
      addNotification({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({ id: "notification.title" }),
        message: intl.formatMessage({ id: "validation.search.noresult" }),
      });
      setNotificationVisible(true);
    }
  };

  useEffect(() => {
    // OGC-654: server's GET response omits a `note` key on each row.
    // jpSet (utils/JsonPath.js) silently no-ops when the JSONPath query
    // returns 0 matches, so handleChange's `jpSet(form, "resultList[N].note", value)`
    // never reaches the form state when typing into the Notes column. Pre-init
    // each row's note to "" so the path exists and the mutation succeeds.
    if (searchResults?.resultList) {
      for (const row of searchResults.resultList) {
        if (row && row.note === undefined) row.note = "";
      }
    }
    if (searchResults) {
      props.setResults(searchResults);
    }
  }, [searchResults]);

  /**
   * The queue behind this form re-runs the current search after a write. The
   * registration is keyed on the endpoint so a later search supersedes it.
   */
  useEffect(() => {
    if (!props.registerRefresh) {
      return;
    }
    props.registerRefresh(url ? refreshResults : null);
    props.registerPageLoader?.(url ? loadResultsPage : null);
  }, [url, props.registerRefresh, props.registerPageLoader]);

  const loadQueue = (next = searchRef.current) => {
    replaceValidationUrl(next);
    props.setParams(searchQueryString(next));
    if (isEmptySearch(next)) {
      setUrl("");
      setLoadError(false);
      setSearchResults({ resultList: [], searched: false });
      return;
    }
    const endpoint = validationEndpoint(next);
    setUrl(endpoint);
    setIsLoading(true);
    getFromOpenElisServer(endpoint, validationResults);
  };

  const updateSearch = (changes, load = false) => {
    const next = { ...searchRef.current, ...changes };
    searchRef.current = next;
    setSearch(next);
    if (load) {
      loadQueue(next);
    }
  };

  /** One server page, the same request for the arrows and for Carbon. */
  const loadResultsPage = (pageNumber) => {
    setIsLoading(true);
    getFromOpenElisServer(url + "&page=" + pageNumber, validationResults);
  };

  /**
   * Re-runs the search, so the server rebuilds its pages, and reopens the page
   * the user was on when the rebuilt queue still has it.
   */
  const refreshResults = (pageToReopen) => {
    setIsLoading(true);
    getFromOpenElisServer(url, (data) => {
      const totalPages = Number(data?.paging?.totalPages) || 1;
      if (pageToReopen > 1 && pageToReopen <= totalPages) {
        getFromOpenElisServer(url + "&page=" + pageToReopen, (pageData) =>
          validationResults(pageData, false),
        );
      } else {
        validationResults(data, false);
      }
    });
  };

  /** The patient form auto-selects any ?patientId= it finds, so it goes first. */
  const openPatientSearch = () => {
    const urlState = new URLSearchParams(window.location.search);
    urlState.delete("patientId");
    const query = urlState.toString();
    window.history.replaceState(
      window.history.state,
      "",
      query ? `/validation?${query}` : "/validation",
    );
    setShowPatientSearch(true);
  };

  const selectPatient = (patient) => {
    setSelectedPatient(patient);
    setShowPatientSearch(false);
    updateSearch({ patientId: patient?.patientPK || "" }, true);
  };

  const clearPatient = () => {
    setSelectedPatient(null);
    setShowPatientSearch(false);
    updateSearch({ patientId: "" }, true);
  };

  useEffect(() => {
    getFromOpenElisServer(
      "/rest/user-test-sections/" + Roles.VALIDATION,
      (sections) => {
        setTestSections(Array.isArray(sections) ? sections : []);
        const unit = searchRef.current.testSectionId;
        if (
          Array.isArray(sections) &&
          unit &&
          !sections.some((section) => String(section.id) === String(unit))
        ) {
          updateSearch({ testSectionId: "" }, true);
        }
      },
    );
    const initial = searchRef.current;
    if (initial.patientId) {
      getFromOpenElisServer(
        "/rest/patient-details?patientID=" +
          encodeURIComponent(initial.patientId),
        (details) => {
          if (details?.patientPK) {
            setSelectedPatient(details);
          }
        },
      );
    }
    if (isEmptySearch(initial)) {
      replaceValidationUrl(initial);
    } else {
      loadQueue(initial);
    }
  }, []);

  return (
    <>
      {isLoading && (
        <Loading
          description={intl.formatMessage({ id: "label.results.loading" })}
        />
      )}
      <Grid
        fullWidth
        className="unifiedResultsPage validationSearchArea"
        data-testid="validation-search-area"
      >
        <Column
          max={3}
          xlg={3}
          lg={4}
          md={4}
          sm={4}
          className="unifiedResultsToolbarColumn"
        >
          <div className="cds--label">
            <FormattedMessage id="label.button.search" />
          </div>
          <Search
            id="validationSearch"
            labelText={intl.formatMessage({
              id: "label.validation.search.box",
            })}
            placeholder={intl.formatMessage({
              id: "label.validation.search.box",
            })}
            value={search.labNumber}
            onChange={(e) => updateSearch({ labNumber: e.target.value })}
            onClear={() => updateSearch({ labNumber: "" }, true)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                loadQueue();
              }
            }}
          />
          <div className="cds--form__helper-text">
            <FormattedMessage id="label.validation.search.rangeHint" />
          </div>
        </Column>
        <Column
          max={3}
          xlg={3}
          lg={4}
          md={4}
          sm={4}
          className="unifiedResultsToolbarColumn"
        >
          <Select
            id="validationLabUnit"
            labelText={intl.formatMessage({ id: "label.results.labUnit" })}
            value={search.testSectionId}
            onChange={(e) =>
              updateSearch({ testSectionId: e.target.value }, true)
            }
          >
            <SelectItem text="" value="" />
            {testSections.map((unit) => (
              <SelectItem text={unit.value} value={unit.id} key={unit.id} />
            ))}
          </Select>
        </Column>
        <Column
          max={4}
          xlg={4}
          lg={8}
          md={8}
          sm={4}
          className="unifiedResultsToolbarColumn"
        >
          <DatePicker
            key={dayFirst ? "day-first" : "month-first"}
            datePickerType="range"
            dateFormat={dayFirst ? "d/m/Y" : "m/d/Y"}
            parseDate={parseDisplayDate}
            value={[
              parseDisplayDate(search.fromDate),
              parseDisplayDate(search.toDate),
            ].filter(Boolean)}
            onChange={(dates) =>
              updateSearch({
                fromDate: formatDate(dates?.[0]),
                toDate: formatDate(dates?.[1]),
              })
            }
          >
            <DatePickerInput
              id="validationFromDate"
              labelText={intl.formatMessage({
                id: "label.validation.search.fromDate",
              })}
              placeholder={dayFirst ? "dd/mm/yyyy" : "mm/dd/yyyy"}
            />
            <DatePickerInput
              id="validationToDate"
              labelText={intl.formatMessage({
                id: "label.validation.search.toDate",
              })}
              placeholder={dayFirst ? "dd/mm/yyyy" : "mm/dd/yyyy"}
            />
          </DatePicker>
        </Column>
        <Column
          max={3}
          xlg={3}
          lg={4}
          md={4}
          sm={4}
          className="unifiedResultsToolbarColumn unifiedResultsPatientColumn"
        >
          <div className="cds--label">&nbsp;</div>
          <Button
            kind="tertiary"
            size="md"
            data-testid="validation-search-by-patient"
            onClick={() =>
              showPatientSearch
                ? setShowPatientSearch(false)
                : openPatientSearch()
            }
            disabled={isLoading}
          >
            <FormattedMessage id="label.results.searchByPatient" />
          </Button>
        </Column>
        <Column
          max={3}
          xlg={3}
          lg={4}
          md={4}
          sm={4}
          className="unifiedResultsToolbarColumn unifiedResultsLoadColumn"
        >
          <div className="cds--label">&nbsp;</div>
          <Button
            size="md"
            data-testid="validation-load"
            onClick={() => loadQueue()}
            disabled={isLoading}
          >
            <FormattedMessage id="label.results.load" />
          </Button>
        </Column>

        {(showPatientSearch || selectedPatient || search.patientId) && (
          <Column lg={16} md={8} sm={4}>
            <div
              className="bordered-section-panel unifiedResultsPatientPanel"
              data-testid="validation-patient-panel"
            >
              <div className="unifiedResultsPatientPanelHeader">
                <Tag
                  type={search.patientId ? "blue" : "gray"}
                  data-testid="validation-selected-patient"
                >
                  <FormattedMessage id="label.results.selectedPatient" />:{" "}
                  {search.patientId
                    ? patientDisplayName(selectedPatient) || search.patientId
                    : intl.formatMessage({
                        id: "label.results.selectedPatient.none",
                      })}
                </Tag>
                {search.patientId && (
                  <Button
                    kind="ghost"
                    size="sm"
                    data-testid="validation-clear-patient"
                    onClick={clearPatient}
                  >
                    <FormattedMessage id="label.button.clear" />
                  </Button>
                )}
              </div>
              {showPatientSearch && (
                <div className="unifiedResultsPatientSearch">
                  <SearchPatientForm getSelectedPatient={selectPatient} />
                </div>
              )}
            </div>
          </Column>
        )}

        {loadError && (
          <Column lg={16} md={8} sm={4}>
            <InlineNotification
              kind="error"
              lowContrast
              hideCloseButton
              title={intl.formatMessage({ id: "notification.title" })}
              subtitle={intl.formatMessage({
                id: "label.validation.search.loadError",
              })}
            />
            <Button kind="ghost" size="sm" onClick={() => loadQueue()}>
              <FormattedMessage id="label.results.refresh" />
            </Button>
          </Column>
        )}
      </Grid>
    </>
  );
};

export default SearchForm;
