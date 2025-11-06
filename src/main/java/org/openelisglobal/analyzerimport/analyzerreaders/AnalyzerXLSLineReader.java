package org.openelisglobal.analyzerimport.analyzerreaders;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;
import org.openelisglobal.spring.util.SpringContext;

public class AnalyzerXLSLineReader extends AnalyzerReader {

    private List<String> lines;
    private AnalyzerLineInserter inserter;
    private String error;

    @Override
    public boolean readStream(InputStream stream) {
        error = null;
        inserter = null;
        lines = new ArrayList<>();
        POIFSFileSystem fs = null;
        HSSFWorkbook wb = null;
        try {
            fs = new POIFSFileSystem(stream);
            wb = new HSSFWorkbook(fs);
            HSSFSheet sheet = wb.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.rowIterator();
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Iterator<Cell> cellIterator = row.cellIterator();
                String line = "";
                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    String value = "";
                    switch (cell.getCellType()) {
                    case BLANK:
                    case STRING:
                        value = cell.getStringCellValue();
                        break;
                    case NUMERIC:
                        value = Double.toString(cell.getNumericCellValue());
                        break;
                    default:
                    }
                    line += value.replaceAll("\t", "\\t") + "\t";
                }
                if (line.endsWith("\t")) {
                    line = line.substring(0, line.length() - 1);
                }
                lines.add(line);
            }
        } catch (IOException e) {
            error = "Unable to read file";
            return false;
        }

        if (!lines.isEmpty()) {
            setInserter();
            if (inserter == null) {
                error = "Unable to understand which analyzer sent the file";
                return false;
            }
            return true;
        } else {
            error = "Empty file";
            return false;
        }
    }

    private void setInserter() {
        for (AnalyzerImporterPlugin plugin : SpringContext.getBean(PluginAnalyzerService.class).getAnalyzerPlugins()) {
            try {
                if (plugin.isTargetAnalyzer(lines)) {
                    inserter = plugin.getAnalyzerLineInserter();
                    return;
                }
            } catch (RuntimeException e) {
                LogEvent.logError(e);
            }
        }
    }

    /*
     * For testing purposes only
     */
    public void insertTestLines(List<String> testLines) {
        lines = testLines;
    }

    @Override
    public boolean insertAnalyzerData(String systemUserId) {
        if (inserter == null) {
            error = "Unable to understand which analyzer sent the file";
            return false;
        } else {
            boolean success = inserter.insert(lines, systemUserId);
            if (!success) {
                error = inserter.getError();
            }

            return success;
        }
    }

    @Override
    public String getError() {
        return error;
    }
}
