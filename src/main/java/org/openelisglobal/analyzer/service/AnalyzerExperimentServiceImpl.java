package org.openelisglobal.analyzer.service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.openelisglobal.analyzer.dao.AnalyzerExperimentDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerExperiment;
import org.openelisglobal.common.exception.LIMSException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnalyzerExperimentServiceImpl extends AuditableBaseObjectServiceImpl<AnalyzerExperiment, Integer>
        implements AnalyzerExperimentService {

    @Autowired
    private AnalyzerExperimentDAO baseDAO;

    public AnalyzerExperimentServiceImpl() {
        super(AnalyzerExperiment.class);
    }

    @Override
    protected AnalyzerExperimentDAO getBaseObjectDAO() {
        return baseDAO;
    }

    @Override
    public Map<String, String> getWellValuesForId(Integer id) throws IOException {
        Map<String, String> wellValues = new HashMap<>();
        AnalyzerExperiment analyzerExperiment = getBaseObjectDAO().get(id).orElse(null);
        if (analyzerExperiment == null) {
            return wellValues;
        }
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(analyzerExperiment.getFile())));
        String[] columns = reader.readLine().split(",");
        while (reader.ready()) {
            String[] pair = reader.readLine().split(",");
            wellValues.put(pair[0], pair.length == 2 ? pair[1] : "");
        }
        return wellValues;
    }

    @Override
    public Integer saveMapAsCSVFile(String filename, Map<String, String> wellValues) throws LIMSException {
        AnalyzerExperiment analyzerExperiment = new AnalyzerExperiment();
        analyzerExperiment.setName(filename);
        analyzerExperiment.setFile(generateCSV(wellValues));
        return baseDAO.insert(analyzerExperiment);
    }

    private byte[] generateCSV(Map<String, String> wellValues) throws LIMSException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (Writer writer = new PrintWriter(outputStream)) {
            writer.append("well").append(',').append("Sample Name").append('\n');
            List<Entry<String, String>> entries = wellValues.entrySet().stream().collect(Collectors.toList());
            Collections.sort(entries, new WellValueComparator());
            for (Entry<String, String> entry : entries) {
                writer.append(entry.getKey()).append(',').append(entry.getValue()).append('\n');
            }
        } catch (IOException e) {
            LogEvent.logError(e);
            throw new LIMSException("could not generate the csv");
        }
        return outputStream.toByteArray();
    }

    public class WellValueComparator implements Comparator<Entry<String, String>> {

        @Override
        public int compare(Entry<String, String> firstEntry, Entry<String, String> secondEntry) {
            Pattern pattern = Pattern.compile("([A-Z]+)(\\d+)");
            Matcher matcher = pattern.matcher(firstEntry.getKey());
            matcher.find();
            String firstAlpha = matcher.group(1);
            String firstNum = matcher.group(2);

            matcher = pattern.matcher(secondEntry.getKey());
            matcher.find();
            String secondAlpha = matcher.group(1);
            String secondNum = matcher.group(2);

            int compareVal = compareLengthAware(firstAlpha, secondAlpha);
            if (compareVal == 0) {
                compareVal = compareLengthAware(firstNum, secondNum);
            }

            return compareVal;
        }

        private int compareLengthAware(String first, String second) {
            if (first.length() > second.length()) {
                return 1;
            } else if (first.length() < second.length()) {
                return -1;
            } else if (first.compareTo(second) > 0) {
                return 1;
            } else if (first.compareTo(second) < 0) {
                return -1;
            } else {
                return 0;
            }
        }
    }
}
