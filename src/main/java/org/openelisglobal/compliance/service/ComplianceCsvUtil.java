package org.openelisglobal.compliance.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import org.openelisglobal.common.util.CsvParsingUtil;

/**
 * Compliance-specific CSV reading helpers.
 *
 * <p>
 * The field-level parsing lives in {@link CsvParsingUtil}, which is the single
 * canonical implementation shared with the other configuration handlers; the
 * methods below only forward to it. What remains local is
 * {@link #readHeaderLine}, whose "tolerate comments before the header" contract
 * is specific to the compliance seed-CSV authoring rules.
 */
final class ComplianceCsvUtil {

    private ComplianceCsvUtil() {
    }

    static String[] parseCsvLine(String line) {
        return CsvParsingUtil.parseCsvLine(line);
    }

    static Map<String, Integer> createColumnMap(String[] headers) {
        return CsvParsingUtil.createColumnMap(headers);
    }

    static String getValueOrEmpty(String[] values, Integer index) {
        return CsvParsingUtil.getValueOrEmpty(values, index);
    }

    static boolean isSkippableLine(String line) {
        return CsvParsingUtil.isSkippableLine(line);
    }

    /**
     * Reads forward until the first non-skippable line and returns it as the
     * header. Blank lines and {@code #}-prefixed comments before the header are
     * tolerated so the seed-CSV authoring rules in the README ("blank lines and #
     * comment lines are skipped") apply uniformly to the header position too.
     *
     * <p>
     * Throws {@link IllegalArgumentException} if the stream contains no header row.
     * The returned {@link HeaderRead#lineNumber} reflects the 1-based source-file
     * position of the header row, so callers can carry it forward as the running
     * line counter and keep error messages accurate.
     */
    static HeaderRead readHeaderLine(BufferedReader reader, String fileName, String emptyMessagePrefix)
            throws IOException {
        int lineNumber = 0;
        String line;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (!isSkippableLine(line)) {
                return new HeaderRead(line, lineNumber);
            }
        }
        throw new IllegalArgumentException(emptyMessagePrefix + " " + fileName + " is empty");
    }

    /** Header line + 1-based source-file line number it was read from. */
    static final class HeaderRead {
        final String line;
        final int lineNumber;

        HeaderRead(String line, int lineNumber) {
            this.line = line;
            this.lineNumber = lineNumber;
        }
    }
}
