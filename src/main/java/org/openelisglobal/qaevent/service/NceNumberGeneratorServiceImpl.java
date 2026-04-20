package org.openelisglobal.qaevent.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for generating unique NCE numbers.
 *
 * <p>
 * Uses synchronized method to ensure thread safety within the single JVM. A
 * unique constraint on nc_event.nce_number (added via Liquibase migration
 * nce-016) provides an additional safety net against duplicates.
 */
@Service
public class NceNumberGeneratorServiceImpl implements NceNumberGeneratorService {

    private static final String NCE_NUMBER_PREFIX = "NCE";
    private static final String NCE_NUMBER_FORMAT = "%s-%d-%05d";
    private static final Pattern NCE_NUMBER_PATTERN = Pattern.compile("^NCE-(\\d{4})-(\\d{5})$");

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public String generateNceNumber() {
        return generateNceNumber(LocalDate.now().getYear());
    }

    @Override
    @Transactional
    public synchronized String generateNceNumber(int year) {
        try {
            int nextSequence = getNextSequenceForYear(year);
            return String.format(NCE_NUMBER_FORMAT, NCE_NUMBER_PREFIX, year, nextSequence);
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error generating NCE number", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int getCurrentSequenceForYear(int year) {
        try {
            String yearPrefix = String.format("%s-%d-", NCE_NUMBER_PREFIX, year);
            String sql = "SELECT MAX(CAST(SUBSTRING(nce_number, 10) AS INTEGER)) "
                    + "FROM clinlims.nc_event WHERE nce_number LIKE :yearPrefix";
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("yearPrefix", yearPrefix + "%");
            Object result = query.getSingleResult();
            if (result == null) {
                return 0;
            }
            if (result instanceof Number) {
                return ((Number) result).intValue();
            }
            return Integer.parseInt(result.toString());
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error getting current sequence for year", e);
        }
    }

    private int getNextSequenceForYear(int year) {
        return getCurrentSequenceForYear(year) + 1;
    }

    @Override
    public boolean isValidFormat(String nceNumber) {
        if (nceNumber == null || nceNumber.isEmpty()) {
            return false;
        }
        return NCE_NUMBER_PATTERN.matcher(nceNumber).matches();
    }

    @Override
    public int parseYear(String nceNumber) {
        Matcher matcher = NCE_NUMBER_PATTERN.matcher(nceNumber);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid NCE number format: " + nceNumber);
        }
        return Integer.parseInt(matcher.group(1));
    }

    @Override
    public int parseSequence(String nceNumber) {
        Matcher matcher = NCE_NUMBER_PATTERN.matcher(nceNumber);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid NCE number format: " + nceNumber);
        }
        return Integer.parseInt(matcher.group(2));
    }
}
