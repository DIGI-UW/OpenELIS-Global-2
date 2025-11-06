/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) The Minnesota Department of Health.  All Rights Reserved.
 *
 * Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.reports.action.implementation.reportBeans;

import static org.apache.commons.validator.GenericValidator.isBlankOrNull;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.DICT;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.TEST_RESULT;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.jdbc.ReturningWork;
import org.openelisglobal.analyte.service.AnalyteService;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.observationhistorytype.service.ObservationHistoryTypeService;
import org.openelisglobal.observationhistorytype.valueholder.ObservationHistoryType;
import org.openelisglobal.project.service.ProjectService;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl;

/**
 * @author pahill (pahill@uw.edu)
 * @since Mar 18, 2011
 */
public abstract class CSVColumnBuilder {

    // these are used so we are not passing around strings in the methods that are
    // appended to sql
    // this is to cover any potential sql injection that could be introduced by a
    // developer
    protected enum SQLConstant {
        ARV_TREATMENT_ADV_EFF_GRD("arvTreatmentAdvEffGrd"), ARV_TREATMENT_ADV_EFF_TYPE("arvTreatmentAdvEffType"),
        COTRIMOXAZOLE_TREAT_ADV_EFF_GRD("cotrimoxazoleTreatAdvEffGrd"),
        COTRIMOXAZOLE_TREAT_ADV_EFF_TYPE("cotrimoxazoleTreatAdvEffType"),
        CURRENT_ARV_TREATMENT_INNS("currentARVTreatmentINNs"), CURRENT_DISEASE_OTHER("currentDiseaseOther"),
        CURRENT_DISEASES("currentDiseases"), DEMO("demo"), FUTURE_ARV_TREATMENT_INNS("futureARVTreatmentINNs"),
        ORGANIZATION("organization"), PRIOR_ARV_TREATMENT_INNS("priorARVTreatmentINNs"),
        PRIOR_DISEASE_OTHER("priorDiseaseOther"), PRIOR_DISEASES("priorDiseases"), RESULT("result");

        private final String nameInSql;

        private SQLConstant(String name) {
            nameInSql = name;
        }

        @Override
        public String toString() {
            return nameInSql;
        }
    }

    /** */
    public CSVColumnBuilder(StatusService.AnalysisStatus validStatusFilter) {
        // we'll round up everything via hibernate first.
        ResourceTranslator.DictionaryTranslator.getInstance();
        ResourceTranslator.GenderTranslator.getInstance();

        if (validStatusFilter != null) {
            validStatusId = SpringContext.getBean(IStatusService.class).getStatusID(validStatusFilter);
        }
    }

    /**
     * The list of all columns that can be exported for lookup when report is
     * generated
     */
    private final Map<String, CSVColumn> columnsByDbName = new HashMap<>();

    /**
     * The list of columns in order of definition, so that we can generate
     * JasperReports XML in order for display
     */
    private final List<CSVColumn> columnsInOrder = new ArrayList<>();

    /**
     * In order as postgres would order varchar columns (alphabetical ignoring
     * case?).
     */
    protected List<ObservationHistoryType> allObHistoryTypes;

    /** All possible tests, so we can have 1 result per test. */
    protected List<Test> allTests;

    /**
     * This table provide a mapping from test name (CSV column heading also) to the
     * TestResult record so we can look at the result type (Dictionary vs. constant
     * etc.)
     */
    protected Map<String, TestResult> testResultsByTestName;

    protected String validStatusId;

    protected static final SimpleDateFormat postgresDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat postgresDateTime = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    protected ResultSet resultSet;

    protected String eol = System.getProperty("line.separator");

    private static ProjectService projectService = SpringContext.getBean(ProjectService.class);

    protected ResultService resultService = SpringContext.getBean(ResultService.class);
    private ObservationHistoryTypeService ohtService = SpringContext.getBean(ObservationHistoryTypeService.class);
    private AnalyteService analyteService = SpringContext.getBean(AnalyteService.class);
    private TestService testService = SpringContext.getBean(TestService.class);
    private TestResultService testResultService = SpringContext.getBean(TestResultService.class);

    // This is the largest value possible for a postgres column name. The code will
    // convert the
    // test description to a column name so we need to truncate
    // It's actually 63 but UTF_8 makes 59 safer.
    private static final int MAX_POSTGRES_COL_NAME = 55;

    /**
     * the test have to be sorted by the name, because they have to match the pivot
     * table order in the results
     */
    @SuppressWarnings("unchecked")
    protected void defineAllTestsAndResults() {
        if (allTests == null) {
            allTests = testService.getAllOrderBy("name");
        }
        if (testResultsByTestName == null) {
            testResultsByTestName = new HashMap<>();
            List<TestResult> allTestResults = testResultService.getAllTestResults();
            for (TestResult testResult : allTestResults) {
                String key = TestServiceImpl.getLocalizedTestNameWithType(testResult.getTest());
                testResultsByTestName.put(key, testResult);
            }
        }
    }

    /** map to provide appropriate tag to identify the project. */
    static Map<String /* project Id */, String /* project tag */> projectTagById = new HashMap<>();

    static {
        defineAllProjectTags();
    }

    public static void defineAllProjectTags() {
        List<Project> allProjects = projectService.getAllProjects();
        for (Project project : allProjects) {
            String projectTag;
            // Watch the order on these 1st 2!
            if (project.getNameKey().contains("ARVF")) {
                projectTag = "ARVS";
            } else if (project.getNameKey().contains("ARV")) {
                projectTag = "ARVB";
            } else if (project.getNameKey().contains("VLS")) {
                projectTag = "VLS";
            } else if (project.getNameKey().contains("Recency")) {
                projectTag = "RTRI";
            } else if (project.getNameKey().contains("HPV")) {
                projectTag = "HPV";
            } else {
                // otherwise we use the letters from the Sample ID prefix, which
                // at some locations for some projects is undefined.
                String code = project.getProgramCode();
                projectTag = (code == null) ? "" : code.substring(1); // drop the L
            }
            projectTagById.put(project.getId(), projectTag);
        }
    }

    protected StringBuilder query;

    private String gendCD4CountAnalyteId;

    /**
     * The various ways that columns are converted for CSV export
     *
     * @author Paul A. Hill (pahill@uw.edu)
     * @since Feb 1, 2011
     */
    public enum Strategy {
        ANALYSIS_STATUS, DICT, // dictionary localized value
        DICT_PLUS, // dictionary localized value or a string constant
        DICT_RAW, // dictionary localized value, no attempts at trimming to show just code number.
        DATE, // date (i.e. 01/01/2013)
        DATE_TIME, // date with time (i.e. 01/01/2013 12:12:00)
        NONE, GENDER, DROP_ZERO, TEST_RESULT, GEND_CD4, SAMPLE_STATUS, PROJECT, LOG, // results is a real number, but
        // display the log of it.
        AGE_YEARS, AGE_MONTHS, AGE_WEEKS, DEBUG, CUSTOM, // special handling which is encapsulated in an instance of
        // ICSVColumnCustomStrategy
        BLANK // Will always be an empty string. Used when column is wanted but data is not
    }

    public void buildDataSource() throws SQLException {
        buildResultSet();
    }

    protected void buildResultSet() throws SQLException {
        makeSQL();
        String sql = query.toString();
        // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown",
        // "===1===\n" +
        // sql.substring(0, 7000)); // the SQL is
        // chunked out only because Eclipse thinks printing really big strings to the
        // console must be wrong, so it truncates them
        // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown",
        // "===2===\n" +
        // sql.substring(7000));
        // Session session =
        // HibernateUtil.getSession().getSessionFactory().openSession();
        // PreparedStatement stmt = session.connection().prepareStatement(sql,
        // ResultSet.TYPE_SCROLL_SENSITIVE,
        // ResultSet.CONCUR_READ_ONLY);
        // resultSet = stmt.executeQuery();
        Session session = SpringContext.getBean(SessionFactory.class).getCurrentSession();
        session.beginTransaction();
        resultSet = session.doReturningWork(new ReturningWork<ResultSet>() {

            @Override
            public ResultSet execute(Connection connection) throws SQLException {
                return connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY)
                        .executeQuery();
            }
        });
    }

    protected synchronized String formatDateForDatabaseSql(Date date) {
        // SimpleDateFormat is not thread safe
        return postgresDateFormat.format(date);
    }

    private synchronized String formatDateTimeForDatabaseSql(Date date) {
        // SimpleDateFormat is not thread safe
        return postgresDateTime.format(date);
    }

    protected synchronized java.util.Date parseDateForDatabaseSql(String date) throws ParseException {
        // SimpleDateFormat is not thread safe
        return postgresDateFormat.parse(date);
    }

    private synchronized java.util.Date parseDateTimeForDatabaseSql(String date) throws ParseException {
        // SimpleDateFormat is not thread safe
        return postgresDateTime.parse(date);
    }

    /**
     * @param value
     * @return
     */
    protected String datetimeToLocalDate(String value) {
        try {
            Date date = new java.sql.Date(parseDateTimeForDatabaseSql(value).getTime());
            return DateUtil.convertSqlDateToStringDate(date);
        } catch (ParseException e) {
            return value;
        }
    }

    protected String datetimeToLocalDateTime(String value) {
        try {
            return DateUtil.formatDateTimeAsText(parseDateTimeForDatabaseSql(value));
        } catch (ParseException e) {
            return value;
        }
    }

    public String getValue(CSVColumn column, String accessionNumber) throws SQLException, ParseException {
        String value;
        // look in the data source for a value
        try {
            value = resultSet.getString(trimToPostgresMaxColumnName(column.dbName));
        } catch (RuntimeException e) {
            // if you end up where it is because the result set doesn't return a
            // column of the right name
            // Check MAX_POSTGRES_COL_NAME if this fails on a long name
            LogEvent.logInfo(this.getClass().getSimpleName(), "getValue",
                    "Internal Error: Unable to find db column \"" + column.dbName + "\" in data.");
            return "?" + column.csvName + "?";
        }

        String result = column.translate(value, accessionNumber);
        // translate should never return null, "" is better while it is doing
        // translation.
        if (result == null) {
            LogEvent.logInfo(this.getClass().getSimpleName(), "getValue", "A null found " + column.dbName);
        }
        return result;
    }

    protected String prepareColumnName(String columnName) {
        // trim and escape the column name so it is more safe from sql injection
        if (!columnName.matches("(?i)[a-zàâçéèêëîïôûùüÿñæœ0-9_ ()%/\\[\\]+\\-]+")) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "prepareColumnName",
                    "potentially dangerous character detected in '" + columnName + "'");
        }
        return "\"" + trimToPostgresMaxColumnName(columnName = columnName.replace("\"", "\\\"")) + "\"";
    }

    private String trimToPostgresMaxColumnName(String name) {
        if (name.length() <= MAX_POSTGRES_COL_NAME) {
            return name;
        } else {
            return name.substring(0, MAX_POSTGRES_COL_NAME);
        }
    }

    public void add(String dbName, String csvTitle) {
        add(dbName, csvTitle, DICT);
    }

    public void add(String dbName, String csvTitle, Strategy strat) {
        CSVColumn col = new CSVColumn(dbName, csvTitle, strat);
        columnsByDbName.put(dbName, col);
        columnsInOrder.add(col);
    }

    public void add(String dbName, String csvTitle, ICSVColumnCustomStrategy customStrategy) {
        CSVColumn col = new CSVColumn(dbName, csvTitle, Strategy.CUSTOM, customStrategy);
        columnsByDbName.put(dbName, col);
        columnsInOrder.add(col);
    }

    /**
     * A utility routine for finding the project short tag (used in exporting etc.)
     * from a projectId.
     */
    public static String translateProjectId(String projectId) {
        return (projectId == null) ? null : projectTagById.get(projectId);
    }

    public class CSVColumn {
        public String csvName;
        public String dbName;
        public Strategy strategy;
        public ICSVColumnCustomStrategy customStrategy;

        public CSVColumn(String dbName, String csvName, Strategy strategy) {
            this.csvName = csvName;
            this.dbName = dbName;
            this.strategy = strategy;
        }

        public CSVColumn(String dbName, String csvName, Strategy strategy, ICSVColumnCustomStrategy customStrategy) {
            this.csvName = csvName;
            this.dbName = dbName;
            this.strategy = strategy;
            this.customStrategy = customStrategy;
        }

        public String translate(String value, String accessionNumber) throws SQLException, ParseException {
            switch (strategy) {
            case CUSTOM:
                return customStrategy.translate(value, accessionNumber, csvName, dbName);
            case DICT_RAW:
                return isBlankOrNull(value) ? ""
                        : ResourceTranslator.DictionaryTranslator.getInstance().translateRaw(value);
            case DICT_PLUS:
                return isBlankOrNull(value) ? ""
                        : ResourceTranslator.DictionaryTranslator.getInstance().translateOrNot(value);
            case DICT:
                return isBlankOrNull(value) ? ""
                        : ResourceTranslator.DictionaryTranslator.getInstance().translate(value);
            case DATE:
                return isBlankOrNull(value) ? "" : datetimeToLocalDate(value);
            case DATE_TIME:
                return isBlankOrNull(value) ? "" : datetimeToLocalDateTime(value);
            case AGE_YEARS:
            case AGE_MONTHS:
            case AGE_WEEKS:
                return isBlankOrNull(value) ? "" : translateAge(strategy, value);
            // PK: Query result already return M/F so no need to translate
            // case GENDER:
            // return isBlankOrNull(value) ? "" :
            // ResourceTranslator.GenderTranslator.getInstance().translate(value);
            case DROP_ZERO:
                return ("0".equals(value) || value == null) ? "" : value;
            case TEST_RESULT:
                return isBlankOrNull(value) ? "" : translateTestResult(this.csvName, value);
            case GEND_CD4:
                return isBlankOrNull(value) ? "" : translateGendResult(getGendCD4CountAnalyteId(), value);
            case LOG:
                return isBlankOrNull(value) ? "" : translateLog(value);
            case SAMPLE_STATUS:
                OrderStatus orderStatus = SpringContext.getBean(IStatusService.class).getOrderStatusForID(value);
                if (orderStatus == null) {
                    return "?";
                }
                switch (orderStatus) {
                case Entered:
                    return "E"; // entered, entree
                case Started:
                    return "C"; // commenced, commence
                case Finished:
                    return "F"; // Finished, Finale
                case NonConforming_depricated:
                    return "N"; // Non-conforming, Non-conformes
                }
            case ANALYSIS_STATUS:
                AnalysisStatus analysisStatus = StatusService.getInstance().getAnalysisStatusForID(value);
                if (analysisStatus == null)
                    return "?";
                switch (analysisStatus) {
                case SampleRejected:
                    return "Reject"; // rejété, entr�e
                case NotStarted:
                    return "Not_Started"; // entered, entr�e
                case Canceled:
                    return "Canceled"; // commenced, commenc�
                case TechnicalAcceptance:
                    return "Validation_Technique"; // Finished, Finale
                case TechnicalRejected:
                    return "Rejet - Technique"; // Non-conforming, Non-conformes
                case BiologistRejected:
                    return "Rejet - Biologie"; // entered, entr�e
                case NonConforming_depricated:
                    return "Non Conforme"; // commenced, commenc�
                case Finalized:
                    return "Validation_Biologique"; // Finished, Finale
                }
            case PROJECT:
                return translateProjectId(value);
            case DEBUG:
                LogEvent.logInfo(this.getClass().getSimpleName(), "translate",
                        "Processing Column Value: " + csvName + " \"" + value + "\"");
            case BLANK:
                return "";
            default:
                return isBlankOrNull(value) ? "" : value;
            }
        }

        private String translateGendResult(String gendResultAnalyteId, String sampleItemId) {
            // 'generated CD4 Count'
            Result gendCD4Result = resultService.getResultForAnalyteAndSampleItem(gendResultAnalyteId, sampleItemId);
            if (gendCD4Result == null) {
                return "";
            }
            String value = gendCD4Result.getValue(true);
            return (value == null) ? "" : value;
        }

        /**
         * The log value of the given value as a floating-point value.
         *
         * @param value
         * @return The log value
         */
        private String translateLog(String value) {
            try {
                double d = Double.parseDouble(value);
                return String.valueOf(Math.log10(d));
            } catch (NumberFormatException e) {
                return "";
            }
        }

        public String translateAge(Strategy strategy, String end) throws SQLException, ParseException {
            java.util.Date birthday = resultSet.getDate("birth_date");
            java.util.Date endDate = parseDateTimeForDatabaseSql(end);
            switch (strategy) {
            case AGE_YEARS:
                return String.valueOf(DateUtil.getAgeInYears(birthday, endDate));
            case AGE_MONTHS:
                return String.valueOf(DateUtil.getAgeInMonths(birthday, endDate));
            case AGE_WEEKS:
                return String.valueOf(DateUtil.getAgeInWeeks(birthday, endDate));
            }
            return "";
        }

        /**
         * @param value
         * @return @
         * @throws SQLException
         */
        public String translateTestResult(String testName, String value) throws SQLException {
            TestResult testResult = testResultsByTestName.get(testName);
            if (testName.equalsIgnoreCase("DNA PCR")) { // need fix: testName is different from map key
                return ResourceTranslator.DictionaryTranslator.getInstance().translateRaw(value);
            }
            // if it is not in the table then its just a value in the result
            // that was NOT selected from a list, thus no translation
            if (testResult == null) {
                return value;
            }
            String type = testResult.getTestResultType();
            // if it is in the table D have to be translated through the
            // dictionary, otherwise don't
            if (TypeOfTestResultServiceImpl.ResultType.DICTIONARY.getCharacterValue().equals(type)) {
                return ResourceTranslator.DictionaryTranslator.getInstance().translateRaw(value);
            } else if (TypeOfTestResultServiceImpl.ResultType.MULTISELECT.getCharacterValue().equals(type)) {
                return findMultiSelectItemsForTest(testResult.getTest().getId());
            }
            return value;
        }

        /**
         * @param testId
         * @return
         * @throws SQLException
         */
        private String findMultiSelectItemsForTest(String testId) throws SQLException {
            String sampleId = resultSet.getString("sample_id");
            List<Result> results = resultService.getResultsForTestAndSample(sampleId, testId);
            StringBuilder multi = new StringBuilder();
            for (Result result : results) {
                multi.append(ResourceTranslator.DictionaryTranslator.getInstance().translateRaw(result.getValue()));
                multi.append(",");
            }

            if (multi.length() > 0) {
                multi.setLength(multi.length() - 1);
            }

            return multi.toString();
        }
    }

    public abstract void makeSQL();

    protected void defineAllObservationHistoryTypes() {
        allObHistoryTypes = ohtService.getAllOrdered("typeName", false);
    }

    /**
     * For every sample, one row per sample item, one column per test result for
     * that sample item.
     *
     * @param lowDatePostgres
     * @param highDatePostgres
     */
    protected void appendResultCrosstab(Date lowDate, Date highDate, String byDate) {
        // A list of analytes which should not show up in the regular results,
        // because they are not the primary results, but, for example, is a
        // conclusion.
        // String excludeAnalytes = getExcludedAnalytesSet();
        SQLConstant listName = SQLConstant.RESULT;
        query.append(", \n\n ( SELECT si.samp_id, si.id AS sampleItem_id, si.sort_order AS sampleItemNo, " + listName
                + ".* " + " FROM sample_item AS si LEFT JOIN \n ");

        // Begin cross tab / pivot table
        query.append(" crosstab( \n" + " 'SELECT si.id, t.description, r.value \n"
                + " FROM clinlims.result AS r, clinlims.analysis AS a, clinlims.sample_item AS si,"
                + " clinlims.sample AS s, clinlims.test AS t, clinlims.test_result AS tr \n" + " WHERE \n"
                + " s.id = si.samp_id AND " + byDate + " >= date(''" + formatDateForDatabaseSql(lowDate) + "'')  AND "
                + byDate + " <= date(''" + formatDateForDatabaseSql(highDate) + " '') " + "\n AND s.id = si.samp_id "
                + "\n AND si.id = a.sampitem_id "
                // sql injection safe as user cannot overwrite validStatusId in database
                + ((validStatusId == null) ? "" : " AND a.status_id = " + validStatusId)
                + "\n AND a.id = r.analysis_id " + "\n AND r.test_result_id = tr.id" + "\n AND tr.test_id = t.id       "
                // + (( excludeAnalytes == null)?"":
                // " AND r.analyte_id NOT IN ( " + excludeAnalytes) + ")"
                // + " AND a.test_id = t.id "
                + "\n ORDER BY 1, 2 " + "\n ', 'SELECT description FROM test where is_active = ''Y'' ORDER BY 1' ) ");
        // end of cross tab

        // Name the test pivot table columns . We'll name them all after the
        // resource name, because some tests have fancy characters in them and
        // somewhere
        // between iReport, Java and postgres a complex name (e.g. one including
        // a beta, " HCG Quant") get messed up and isn't found.
        query.append("\n as " + listName + " ( " // inner use of the list name
                + "\"si_id\" numeric(10) ");
        for (Test col : allTests) {
            String testName = TestServiceImpl.getLocalizedTestNameWithType(col);
            // sql injection safe as it is escaped for
            query.append("\n, " + prepareColumnName(testName) + " varchar(200) ");
        }
        query.append(" ) \n");
        // left join all sample Items from the right sample range to the results table.
        query.append("\n ON si.id = " + listName + ".si_id " // the inner use a few lines above
                + "\n ORDER BY si.samp_id, si.id " + "\n) AS " + listName + "\n "); // outer re-use the list name to
        // name this sparse matrix of
        // results.
    }

    /**
     * @return
     */
    /*
     * private String getExcludedAnalytesSet() { String[] excludedAnalytes = new
     * String[] { getGendCD4CountAnalyteId() }; StringBuilder sb = new
     * StringBuilder(); for (String a : excludedAnalytes) {
     * sb.append(a).append(","); } sb.setLength(sb.length() - 1); return
     * sb.toString(); }
     */

    protected void appendObservationHistoryCrosstab(Date lowDate, Date highDate, String byDate) {
        SQLConstant listName = SQLConstant.DEMO;
        appendCrosstabPreamble(listName);
        query.append( // any Observation History items
                "\n crosstab( " + "\n 'SELECT DISTINCT oh.sample_id as samp_id, oht.type_name, value "
                        + "\n FROM observation_history AS oh, sample AS s, observation_history_type AS oht "
                        + "\n WHERE s.collection_date >= date(''" + formatDateForDatabaseSql(lowDate) + "'') "
                        + "\n AND s.collection_date <= date(''" + formatDateForDatabaseSql(highDate) + "'')"
                        + "\n AND s.id = oh.sample_id AND oh.observation_history_type_id = oht.id order by 1;' "
                        + "\n , "
                        + "\n 'SELECT DISTINCT oht.type_name FROM observation_history_type AS oht ORDER BY 1;' " + // must
                                                                                                                   // be
                                                                                                                   // the
                                                                                                                   // same
                                                                                                                   // list
                                                                                                                   // as
                                                                                                                   // the
                                                                                                                   // column
                                                                                                                   // definition
                                                                                                                   // for
                                                                                                                   // the
                                                                                                                   // demo
                                                                                                                   // table
                                                                                                                   // below.
                        "\n ) \n ");

        // in the following list of observation history items, all valid values
        // are listed in alphabetical order (since that is how crosstab lists
        // them), so that that the queries constructed don't have to filter the
        // exceptional ones.
        // It is the column name list defined in the constructor of this class
        // which picks the right column to display (and in what order). That
        // list is also used to generated the JasperReport XML
        // That list skips the OH types which actually repeat (priorDiseases,
        // priorARVTreatmentINNs) which are returned as as crosstab set of
        // columns either specially named columns (disease acronyms for each
        // diseases) or with with n after their name (priorARVTreatmentINNs1, 2,
        // ... )
        query.append(" as demo ( " + " \"s_id\"                           numeric(10) ");
        for (ObservationHistoryType oht : allObHistoryTypes) {
            // this is sql injection safe as users currently have no way of modifying fields
            // in ObservationHistoryTypes
            query.append("\n," + oht.getTypeName() + " varchar(100) ");
        }
        query.append(" ) \n");
        appendCrosstabPostfix(lowDate, highDate, listName);
    }

    protected void appendCrosstabPreamble(SQLConstant listName) {
        query.append(", \n\n ( SELECT s.id AS samp_id, " + listName + ".* " + " FROM sample AS s LEFT JOIN \n ");
    }

    protected void appendCrosstabPostfix(Date lowDate, Date highDate, SQLConstant listName) {
        query.append("\n ON s.id = " + listName + ".s_id " + " AND s.collection_date >= '"
                + formatDateForDatabaseSql(lowDate) + "'" + " AND s.collection_date <= '"
                + formatDateForDatabaseSql(highDate) + "'" + " ORDER BY 1 " + "\n) AS " + listName + "\n ");
    }

    protected Object pad20(Object translate) {
        // uncomment the following lines to help make everything line up when
        // debugging output.
        // String strIn = (String)translate;
        // int len = strIn.length();
        // len = ( len > 30 )? 30: len;
        // translate = translate + " ".substring(len);
        return translate;
    }

    /** Generate a column to the list of all columns. One for each possible test. */
    protected void addAllResultsColumns() {
        for (Test test : allTests) {
            String testTag = TestServiceImpl.getLocalizedTestNameWithType(test);
            add(testTag, TestServiceImpl.getLocalizedTestNameWithType(test), TEST_RESULT);
        }
    }

    /**
     * Useful for the 1st line of a CSV files. This produces a completely escaped
     * for MSExcel comma separated list of columns.
     *
     * @return one string with all names.
     */
    public String getColumnNamesLine() {
        StringBuilder line = new StringBuilder();
        for (CSVColumn column : columnsInOrder) {
            line.append(StringUtil.escapeCSVValue(column.csvName));
            line.append(",");
        }
        line.setLength(line.length() - 1);
        line.append(eol);
        return line.toString();
    }

    /**
     * @return @
     * @throws ParseException
     * @throws SQLException
     */
    public String nextLine() throws SQLException, ParseException {
        StringBuilder line = new StringBuilder();
        String accessionNumber = null;
        for (CSVColumn column : columnsInOrder) {
            // OK this is a little hocky. Some of the custom translation strategies need the
            // accession number and MOST of the time
            // they are called after the accession number column. Not sure what to do if
            // they are ever called before.
            String translatedValue = StringUtil.escapeCSVValue(getValue(column, accessionNumber));
            if (accessionNumber == null && "accession_number".equals(column.dbName)) {
                accessionNumber = translatedValue;
            }
            line.append(translatedValue);
            line.append(",");
        }
        line.setLength(line.length() - 1);
        line.append(eol);
        return line.toString();
    }

    public boolean next() throws SQLException {
        return resultSet.next();
    }

    /**
     * @throws SQLException
     */
    public void closeResultSet() throws SQLException {
        resultSet.close();
        resultSet = null;
    }

    protected String getGendCD4CountAnalyteId() {
        if (gendCD4CountAnalyteId == null) {
            Analyte a = new Analyte();
            a.setAnalyteName("generated CD4 Count");
            gendCD4CountAnalyteId = analyteService.getAnalyteByName(a, false).getId();
        }
        return gendCD4CountAnalyteId;
    }
}
