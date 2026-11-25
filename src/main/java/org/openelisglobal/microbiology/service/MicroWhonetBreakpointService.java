package org.openelisglobal.microbiology.service;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.configuration.service.CsvRow;
import org.openelisglobal.configuration.service.LoadedRow;
import org.openelisglobal.microbiology.dao.MicroWhonetBreakpointDAO;
import org.openelisglobal.microbiology.valueholder.MicroWhonetBreakpoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MicroWhonetBreakpointService {
    static final String[] REQUIRED = { "GUIDELINES", "YEAR", "TEST_METHOD", "ORGANISM_CODE", "BREAKPOINT_TYPE" };

    private final MicroWhonetBreakpointDAO whonetBreakpoints;

    public MicroWhonetBreakpointService(MicroWhonetBreakpointDAO whonetBreakpoints) {
        this.whonetBreakpoints = whonetBreakpoints;
    }

    /** The table is the file: every row is replaced, so every row is created. */
    public List<LoadedRow<String>> load(List<CsvRow> rows) {
        List<MicroWhonetBreakpoint> breakpoints = new ArrayList<>();
        for (CsvRow row : rows) {
            for (String column : REQUIRED) {
                if (row.isBlank(column)) {
                    throw new IllegalArgumentException(column + " is required at line " + row.lineNumber());
                }
            }
            breakpoints.add(of(row));
        }
        whonetBreakpoints.deleteAllRows();
        whonetBreakpoints.insertAll(breakpoints);
        return breakpoints.stream().map(breakpoint -> LoadedRow.created(breakpoint.getId())).toList();
    }

    private static MicroWhonetBreakpoint of(CsvRow row) {
        MicroWhonetBreakpoint breakpoint = new MicroWhonetBreakpoint();
        breakpoint.setGuidelines(value(row, "GUIDELINES"));
        breakpoint.setYear(value(row, "YEAR"));
        breakpoint.setTestMethod(value(row, "TEST_METHOD"));
        breakpoint.setPotency(value(row, "POTENCY"));
        breakpoint.setOrganismCode(value(row, "ORGANISM_CODE"));
        breakpoint.setOrganismCodeType(value(row, "ORGANISM_CODE_TYPE"));
        breakpoint.setBreakpointType(value(row, "BREAKPOINT_TYPE"));
        breakpoint.setHost(value(row, "HOST"));
        breakpoint.setSiteOfInfection(value(row, "SITE_OF_INFECTION"));
        breakpoint.setReferenceTable(value(row, "REFERENCE_TABLE"));
        breakpoint.setReferenceSequence(value(row, "REFERENCE_SEQUENCE"));
        breakpoint.setWhonetAbxCode(value(row, "WHONET_ABX_CODE"));
        breakpoint.setWhonetTest(value(row, "WHONET_TEST"));
        breakpoint.setR(value(row, "R"));
        breakpoint.setI(value(row, "I"));
        breakpoint.setSdd(value(row, "SDD"));
        breakpoint.setS(value(row, "S"));
        breakpoint.setEcvEcoff(value(row, "ECV_ECOFF"));
        breakpoint.setEcvEcoffTentative(value(row, "ECV_ECOFF_TENTATIVE"));
        breakpoint.setDateEntered(value(row, "DATE_ENTERED"));
        breakpoint.setDateModified(value(row, "DATE_MODIFIED"));
        breakpoint.setComments(value(row, "COMMENTS"));
        return breakpoint;
    }

    private static String value(CsvRow row, String column) {
        String value = row.get(column);
        return value.isEmpty() ? null : value;
    }
}
