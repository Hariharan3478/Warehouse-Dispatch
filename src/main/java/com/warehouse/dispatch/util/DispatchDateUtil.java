package com.warehouse.dispatch.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

public final class DispatchDateUtil {

    public static final ZoneId KOLKATA_ZONE = ZoneId.of("Asia/Kolkata");

    private DispatchDateUtil() {}

    /**
     * Checks if a given date is a business day:
     * - Sunday is NOT a business day
     * - Dates in HOLIDAY are NOT business days
     * - Saturday IS a business day
     */
    public static boolean isBusinessDay(LocalDate date, Set<LocalDate> holidays) {
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return false;
        }
        return holidays == null || !holidays.contains(date);
    }

    /**
     * Advances to the next business day strictly after the given date.
     */
    public static LocalDate getNextBusinessDay(LocalDate date, Set<LocalDate> holidays) {
        LocalDate next = date.plusDays(1);
        while (!isBusinessDay(next, holidays)) {
            next = next.plusDays(1);
        }
        return next;
    }

    /**
     * T5: Effective dispatch date
     * requestedAt (converted to Asia/Kolkata) after warehouse.cutoffTime -> next business day after dispatchDate;
     * otherwise dispatchDate.
     */
    public static LocalDate calculateEffectiveDispatchDate(
            ZonedDateTime requestedAt,
            String cutoffTimeStr,
            LocalDate dispatchDate,
            Set<LocalDate> holidays) {

        ZonedDateTime kolkataTime = requestedAt.withZoneSameInstant(KOLKATA_ZONE);
        LocalTime requestedLocalTime = kolkataTime.toLocalTime();
        LocalTime cutoffTime = LocalTime.parse(cutoffTimeStr);

        if (requestedLocalTime.isAfter(cutoffTime)) {
            return getNextBusinessDay(dispatchDate, holidays);
        }
        return dispatchDate;
    }

    /**
     * T10: ETA date = effective dispatch date plus carrier transit_days, counting only business days.
     */
    public static LocalDate calculateEta(LocalDate effectiveDispatchDate, int transitDays, Set<LocalDate> holidays) {
        LocalDate current = effectiveDispatchDate;
        int remainingDays = transitDays;

        while (remainingDays > 0) {
            current = current.plusDays(1);
            if (isBusinessDay(current, holidays)) {
                remainingDays--;
            }
        }
        return current;
    }
}
