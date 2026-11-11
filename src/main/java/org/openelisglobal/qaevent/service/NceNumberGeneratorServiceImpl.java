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
 * Number allocation is serialized with a transaction-scoped PostgreSQL advisory
 * lock: {@code MAX(sequence)+1} is otherwise a read-committed race — two
 * transactions read the same max before either inserts, so concurrent NCE
 * creation (e.g. one QC run failing several analytes at once) collides on the
 * nce_number unique constraint. The lock is held until the caller's transaction
 * commits the insert, so it also holds across app nodes (unlike a JVM
 * {@code synchronized}). The unique constraint remains as a final backstop.
 */
@Service
public class NceNumberGeneratorServiceImpl implements NceNumberGeneratorService {

    private static final String NCE_NUMBER_PREFIX = "NCE";
    private static final String NCE_NUMBER_FORMAT = "%s-%d-%05d";
    private static final Pattern NCE_NUMBER_PATTERN = Pattern.compile("^NCE-(\\d{4})-(\\d{5})$");

    /** Stable key for the NCE-number allocation advisory lock. */
    private static final long NCE_NUMBER_LOCK_KEY = 730_701L;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public String generateNceNumber() {
        return generateNceNumber(LocalDate.now().getYear());
    }

    @Override
    @Transactional
    public String generateNceNumber(int year) {
        try {
            // Serialize allocation until this transaction commits the insert.
            // Select a constant from the lock function (in FROM) so Hibernate maps
            // an int, not the void return — and avoid "::" which its parser reads
            // as a named parameter.
            entityManager.createNativeQuery("SELECT 1 FROM pg_advisory_xact_lock(:key)")
                    .setParameter("key", NCE_NUMBER_LOCK_KEY).getResultList();
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
