package org.openelisglobal.reports.action.implementation.reportBeans;

import static org.openelisglobal.reports.action.implementation.reportBeans.CSVRoutineColumnBuilder.Strategy.AGE_MONTHS;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVRoutineColumnBuilder.Strategy.AGE_WEEKS;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVRoutineColumnBuilder.Strategy.AGE_YEARS;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVRoutineColumnBuilder.Strategy.DATE;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVRoutineColumnBuilder.Strategy.DATE_TIME;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVRoutineColumnBuilder.Strategy.NONE;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVRoutineColumnBuilder.Strategy.PROGRAM;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVRoutineColumnBuilder.Strategy.SAMPLE_STATUS;

import java.sql.Date;
import org.openelisglobal.common.services.StatusService;
import org.openelisglobal.reports.action.implementation.Report.DateRange;

/**
 * If we had a big resultSet with various columns for CSV export, we need a few
 * things defined so we can translate to a useable CSV file values. This class
 * builds the SQL for the one row per CSV output and maps everything to the
 * output including looking up resource names. This class also can print out
 * just the XML needed for the Jasper report which helps make the CSV file (see
 * the call to generateJasperXML).
 *
 * @author Paul A. Hill (pahill@uw.edu)
 * @since Jan 28, 2011
 */
public abstract class CIRoutineColumnBuilder extends CSVRoutineColumnBuilder {

    /**
     * The basic SQL SELECT to get start on finding a sample, sample_item, patient
     * and organization
     */
    protected static final String SELECT_SAMPLE_PATIENT_ORGANIZATION = "SELECT DISTINCT s.id as sample_id, s.accession_number, s.entered_date, s.received_date,"
            + " s.collection_date, s.status_id \n"
            + ", pat.national_id, pat.external_id, pat.birth_date, per.first_name, per.last_name," + " pat.gender \n"
            + ", o.short_name as organization_code, o.name AS organization_name \n" + " ";
    /**
     * The column select which puts all demographic and result columns in the result
     * set.
     */
    protected static final String SELECT_ALL_DEMOGRAPHIC_AND_RESULTS = "\n, demo.*, result.*" + "\n ";
    /**
     * the basic SQL FROM clause for the selection from basic lab tables for sample,
     * sample_item, patient & organization
     */
    protected static final String FROM_SAMPLE_PATIENT_ORGANIZATION = " FROM sample as s, patient as pat, person as per, sample_human as sh, requester_type AS rq,"
            + " sample_requester AS sq, organization AS o \n" + " ";

    protected DateRange dateRange;
    // protected String projectStr;

    /** */
    public CIRoutineColumnBuilder(DateRange dateRange) {
        super(StatusService.AnalysisStatus.Finalized);
        this.dateRange = dateRange;
        // this.projectStr = projectStr;
        defineAllObservationHistoryTypes();
        defineAllTestsAndResults();
        // defineAllProjectTags();
        defineAllReportColumns();
    }

    protected abstract void defineAllReportColumns();

    @Override
    public abstract void makeSQL();

    /**
     * Useful when building the SQL String
     *
     * @param lowDate
     * @param highDate
     * @return String starting "WHERE ..." joining patient, sample, organization
     */
    protected String buildWhereSamplePatienOrgSQL(Date lowDate, Date highDate) {
        String WHERE_SAMPLE_PATIENT_ORG = " WHERE " + "\n pat.id = sh.patient_id " + "\n AND sh.samp_id = s.id "
                + "\n AND s.entered_date >= '" + formatDateForDatabaseSql(lowDate) + "'" + "\n AND s.entered_date <= '"
                + formatDateForDatabaseSql(highDate) + "'" + "\n " + "\n AND sq.requester_type_id = rq.id "
                + "\n AND sq.sample_id= s.id " + "\n AND pat.person_id = per.id " + "\n AND o.id = sq.requester_id   "
        // + ((GenericValidator.isBlankOrNull(projectStr))?"": " AND sp.proj_id = " +
        // projectStr)
        ;
        return WHERE_SAMPLE_PATIENT_ORG;
    }

    protected void appendRepeatingObservation(SQLConstant aOhTypeName, int maxCols, Date lowDate, Date highDate) {
        appendCrosstabPreamble(aOhTypeName);

        query.append(" crosstab( ' SELECT s.id as s_id, type, value FROM Sample AS s  LEFT JOIN ( SELECT"
                + " DISTINCT s.id as s_id , oh.observation_history_type_id AS type, oh.value AS value,"
                + " oh.id  FROM Sample as s, Observation_History AS oh WHERE oh.sample_id = s.id AND"
                + " s.entered_date >= date(''" + formatDateForDatabaseSql(lowDate) + "'') "
                + " AND s.entered_date <= date(''" + formatDateForDatabaseSql(highDate)
                + "'') AND oh.observation_history_type_id = (select id FROM observation_history_type"
                + " WHERE type_name = ''" + aOhTypeName + "'')  ORDER by 1,2, oh.id desc ) AS repeatCols"
                + " ON s.id = repeatCols.s_id" + " WHERE s.entered_date >= date(''" + formatDateForDatabaseSql(lowDate)
                + "'') " + " AND s.entered_date <= date(''" + formatDateForDatabaseSql(highDate) + "'')" + "' )"
                + " AS " + aOhTypeName + " ( s_id NUMERIC(10) ");
        for (int col = 1; col <= maxCols; col++) {
            query.append(", \"").append(aOhTypeName).append(col).append("\" VARCHAR(100)");
        }
        query.append(" )\n ");
        appendCrosstabPostfix(lowDate, highDate, aOhTypeName);
    }

    protected void appendOtherDiseaseCrosstab(Date lowDate, Date highDate, SQLConstant diseaseListName,
            SQLConstant otherColumnName) {
        appendCrosstabPreamble(otherColumnName);
        query.append(" crosstab( "
                + " 'SELECT DISTINCT s.id, oh.observation_history_type_id AS ohType, oh.value AS value "
                + " FROM  Sample as s, Observation_History AS oh, Observation_history_type as oht "
                + " WHERE s.entered_date >= ''" + formatDateForDatabaseSql(lowDate) + "''"
                + "   AND s.entered_date <= ''" + formatDateForDatabaseSql(highDate)
                + "''   AND s.id = oh.sample_id    AND oh.observation_history_type_id = oht.id    AND"
                + " oh.observation_history_type_id = (select id FROM observation_history_type WHERE" + " type_name = ''"
                + diseaseListName + "'') " + "   AND oh.value !~ ''^[0-9]+$'' " + " ORDER by 1,2,3' ) AS "
                + otherColumnName + " ( s_id NUMERIC(10), " + otherColumnName + " varChar(100) ) " + "\n");
        appendCrosstabPostfix(lowDate, highDate, otherColumnName);
    }

    protected void defineBasicColumns() {
        add("accession_number", "LABNO", NONE);
        add("lab_unit", "LAB_UNIT", NONE);
        add("national_id", "IDENTIFIER", NONE);
        add("gender", "SEX", NONE);
        add("birth_date", "BIRTHDATE", DATE);
        add("entered_date", "AGEYEARS", AGE_YEARS);
        add("entered_date", "AGEMONTHS", AGE_MONTHS);
        add("entered_date", "AGEWEEKS", AGE_WEEKS);
        add("received_date", "DATERECPT", DATE_TIME); // reception date
        add("entered_date", "DATEENTERED", DATE_TIME); // interview date
        add("collection_date", "DATECOLLECT", DATE_TIME); // collection date
        // add("released_date", "DATEVALIDATION", DATE_TIME ); // validation date
        add("organization_code", "CODEREFERER", NONE);
        add("organization_name", "REFERER", NONE);
        add("program", "PROGRAM", PROGRAM);
        add("status_id", "STATUS", SAMPLE_STATUS);
        // add("external_id", "SUJETSIT", NONE);
        // add("last_name", "NOM", NONE);
        // add("first_name", "PRENOM", NONE);

    }

    @Override
    protected void addAllResultsColumns() {
        super.addAllResultsColumns();
    }
}
