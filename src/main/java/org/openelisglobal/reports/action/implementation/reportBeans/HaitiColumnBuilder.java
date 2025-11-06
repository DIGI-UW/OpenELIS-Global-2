package org.openelisglobal.reports.action.implementation.reportBeans;

import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.DATE;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.DROP_ZERO;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.NONE;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.SAMPLE_STATUS;

import java.sql.Date;
import org.openelisglobal.reports.action.implementation.Report.DateRange;
import org.openelisglobal.reports.form.ReportForm.DateType;

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
public class HaitiColumnBuilder extends CSVColumnBuilder {

    private DateRange dateRange;
    private DateType dateType;

    /** */
    public HaitiColumnBuilder(DateRange dateRange) {
        super(null);
        this.dateRange = dateRange;
        defineAllObservationHistoryTypes();
        defineAllTestsAndResults();
        defineAllColumns();
        // insert this next call to generate just what you need to past into the
        // JasperReport to update it to include all the columns you've defined above.
        // generateJasperXML();
    }

    /**
     * This is the order we want them in the CSV file, but if you change this you
     * have to rerun generateJasperXML (see commented out call in c'tor above) and
     * paste each chunk into the JasperReports XML.
     */
    private void defineAllColumns() {
        add("accession_number", "LABNO", NONE);
        add("status_id", "ECHSTAT", SAMPLE_STATUS);
        add("national_id", "SUJETNO", NONE);
        add("received_date", "DRCPT", DATE); // reception date
        add("collection_date", "DINTV", DATE); // interview date
        add("organization_name", "SERVICE", NONE);
        add("last_name", "NOM", NONE);
        add("first_name", "PRENOM", NONE);
        add("gender", "SEXE", DROP_ZERO);
        add("birth_date", "DATENAIS", DATE);
        addAllResultsColumns();
    }

    /**
     * @return the SQL for one big row for each sample item in the date range for
     *         the particular project.
     */
    @Override
    public void makeSQL() {
        // Switch date column according to selected DateType: PK
        String dateColumn = "s.entered_date ";
        switch (dateType) {
        case ORDER_DATE:
            dateColumn = "s.entered_date ";
            break;
        case RESULT_DATE:
            dateColumn = "a.released_date ";
            break;
        case PRINT_DATE:
            dateColumn = "dt.report_generation_time ";
        default:
            break;
        }
        query = new StringBuilder();
        Date lowDate = dateRange.getLowDate();
        Date highDate = dateRange.getHighDate();
        query.append("SELECT s.id as sample_id, s.accession_number, s.entered_date, s.received_date,"
                + " s.collection_date, s.status_id , pat.national_id, pat.birth_date, per.first_name,"
                + " per.last_name, pat.gender  ");

        // all crosstab or sub-select generated tables need to be listed in the
        // following list and in the WHERE clause at the bottom
        query.append("\n, demo.*" + ", result.* " + ", organization.* " + "\n ");

        // ordinary lab (sample and patient) tables
        query.append("\n FROM sample as s, patient as pat, person as per, sample_human as sh \n ");

        // all observation history values
        appendOrganization(lowDate, highDate);
        appendObservationHistoryCrosstab(lowDate, highDate, dateColumn);
        appendResultCrosstab(lowDate, highDate, dateColumn);

        // and finally the join that puts these all together. Each cross table should be
        // listed here otherwise it's not in the result and you'll get a full join
        query.append("\n WHERE " + "\n pat.id = sh.patient_id " + "\n AND sh.samp_id = s.id "
                + "\n AND s.collection_date >= '" + formatDateForDatabaseSql(lowDate) + "'"
                + "\n AND s.collection_date <= '" + formatDateForDatabaseSql(highDate) + "'"
                + "\n AND pat.person_id = per.id " + "\n AND s.id = demo.samp_id " + "\n AND s.id = result.samp_id "
                + "\n AND s.id = organization.samp_id " + " ORDER BY s.accession_number ");
        // no don't insert another crosstab or table here, go up before the main WHERE
        // clause
        return;
    }

    /**
     * @param lowDatePostgres
     * @param highDatePostgres
     */
    private void appendOrganization(Date lowDate, Date highDate) {
        appendCrosstabPreamble(SQLConstant.ORGANIZATION);
        query.append("\n" + "( SELECT s.id as s_id, o.name AS organization_name FROM organization AS o, sample AS"
                + " s, sample_organization as so \n" + "     WHERE s.collection_date >= date('"
                + formatDateForDatabaseSql(lowDate) + "') " + "\n     AND   s.collection_date <= date('"
                + formatDateForDatabaseSql(highDate) + "') "
                + "\n     AND   s.id = so.samp_id AND so.org_id = o.id ) AS ORGANIZATION " + "\n  ");
        appendCrosstabPostfix(lowDate, highDate, SQLConstant.ORGANIZATION);
    }
}
