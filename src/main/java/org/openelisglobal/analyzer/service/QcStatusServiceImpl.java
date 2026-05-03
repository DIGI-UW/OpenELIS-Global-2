package org.openelisglobal.analyzer.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class QcStatusServiceImpl implements QcStatusService {

    private final QcStatusDataProvider dataProvider;
    private Clock clock = Clock.systemDefaultZone();

    @Autowired
    public QcStatusServiceImpl(QcStatusDataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

    // For unit testing deterministic time behavior.
    void setClock(Clock clock) {
        this.clock = clock;
    }

    @Override
    public QcStatus resolveStatus(String testId, String instrumentId) {
        if (isBlank(testId) || isBlank(instrumentId)) {
            throw new IllegalArgumentException("testId and instrumentId are required");
        }

        Optional<QcControlLotSnapshot> controlLotOptional = dataProvider.findControlLot(testId, instrumentId);
        if (controlLotOptional.isEmpty()) {
            return QcStatus.NOT_RUN;
        }

        QcControlLotSnapshot controlLot = controlLotOptional.get();
        Optional<QcResultSnapshot> latestResultOptional = dataProvider.findLatestResult(controlLot.getControlLotId());

        if (latestResultOptional.isEmpty()) {
            return QcStatus.NOT_RUN;
        }

        if (!controlLot.isActive()) {
            return QcStatus.FAILED;
        }

        QcResultSnapshot latestResult = latestResultOptional.get();
        if (latestResult.getResultStatus() == QcResultStatus.REJECTED) {
            return QcStatus.FAILED;
        }

        return isFrequencyWindowValid(controlLot) ? QcStatus.PASS : QcStatus.OVERDUE;
    }

    private boolean isFrequencyWindowValid(QcControlLotSnapshot controlLot) {
        Optional<QcResultSnapshot> latestAcceptedOptional = dataProvider
                .findLatestAcceptedResult(controlLot.getControlLotId());
        if (latestAcceptedOptional.isEmpty()) {
            return false;
        }

        LocalDateTime acceptedAt = latestAcceptedOptional.get().getResultDateTime();
        if (acceptedAt == null || controlLot.getFrequencyType() == null) {
            return false;
        }

        ZonedDateTime now = ZonedDateTime.now(clock);
        ZoneId zoneId = now.getZone();
        ZonedDateTime acceptedZoned = acceptedAt.atZone(zoneId);

        switch (controlLot.getFrequencyType()) {
        case DAILY:
            return LocalDate.from(acceptedZoned).equals(LocalDate.from(now));
        case PER_SHIFT:
            return isAcceptedInCurrentShift(controlLot, acceptedZoned, now);
        case CUSTOM_HOURS:
            return isWithinCustomHours(controlLot, acceptedZoned, now);
        default:
            return false;
        }
    }

    private boolean isAcceptedInCurrentShift(QcControlLotSnapshot controlLot, ZonedDateTime acceptedAt, ZonedDateTime now) {
        LocalTime shiftStart = controlLot.getShiftStart();
        LocalTime shiftEnd = controlLot.getShiftEnd();
        if (shiftStart == null || shiftEnd == null) {
            return false;
        }

        ShiftWindow currentWindow = resolveCurrentShiftWindow(now, shiftStart, shiftEnd);
        if (currentWindow == null) {
            return false;
        }

        return !acceptedAt.isBefore(currentWindow.start()) && acceptedAt.isBefore(currentWindow.end());
    }

    private boolean isWithinCustomHours(QcControlLotSnapshot controlLot, ZonedDateTime acceptedAt, ZonedDateTime now) {
        Integer customHours = controlLot.getCustomHours();
        if (customHours == null || customHours <= 0) {
            return false;
        }
        long elapsedHours = Duration.between(acceptedAt, now).toHours();
        return elapsedHours <= customHours;
    }

    private ShiftWindow resolveCurrentShiftWindow(ZonedDateTime now, LocalTime shiftStart, LocalTime shiftEnd) {
        LocalDate today = now.toLocalDate();
        LocalTime nowTime = now.toLocalTime();

        if (shiftStart.equals(shiftEnd)) {
            ZonedDateTime start = ZonedDateTime.of(today, shiftStart, now.getZone());
            return new ShiftWindow(start, start.plusDays(1));
        }

        if (shiftStart.isBefore(shiftEnd)) {
            if (nowTime.isBefore(shiftStart) || !nowTime.isBefore(shiftEnd)) {
                return null;
            }
            return new ShiftWindow(ZonedDateTime.of(today, shiftStart, now.getZone()),
                    ZonedDateTime.of(today, shiftEnd, now.getZone()));
        }

        if (!nowTime.isBefore(shiftStart)) {
            return new ShiftWindow(ZonedDateTime.of(today, shiftStart, now.getZone()),
                    ZonedDateTime.of(today.plusDays(1), shiftEnd, now.getZone()));
        }
        return new ShiftWindow(ZonedDateTime.of(today.minusDays(1), shiftStart, now.getZone()),
                ZonedDateTime.of(today, shiftEnd, now.getZone()));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record ShiftWindow(ZonedDateTime start, ZonedDateTime end) {
    }
}
