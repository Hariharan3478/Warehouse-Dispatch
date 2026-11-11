package com.warehouse.dispatch.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DispatchDateUtilTest {

    private final Set<LocalDate> holidays = Set.of(
            LocalDate.of(2026, 11, 9),  // Company Holiday (Monday)
            LocalDate.of(2026, 12, 25)  // Christmas
    );

    @Test
    @DisplayName("T5: requestedAt 11:20 is before cutoff 16:00 -> effective dispatch date stays 2026-11-06")
    void testCutoffBefore() {
        ZonedDateTime requestedAt = ZonedDateTime.parse("2026-11-06T11:20:00+05:30");
        LocalDate dispatchDate = LocalDate.of(2026, 11, 6);

        LocalDate effective = DispatchDateUtil.calculateEffectiveDispatchDate(
                requestedAt, "16:00", dispatchDate, holidays);

        assertEquals(LocalDate.of(2026, 11, 6), effective);
    }

    @Test
    @DisplayName("T5: requestedAt 16:05 is after cutoff 16:00 -> effective dispatch date moves to Saturday 2026-11-07")
    void testCutoffAfter() {
        ZonedDateTime requestedAt = ZonedDateTime.parse("2026-11-06T16:05:00+05:30");
        LocalDate dispatchDate = LocalDate.of(2026, 11, 6);

        LocalDate effective = DispatchDateUtil.calculateEffectiveDispatchDate(
                requestedAt, "16:00", dispatchDate, holidays);

        assertEquals(LocalDate.of(2026, 11, 7), effective);
    }

    @Test
    @DisplayName("T10: Section 8 ETA check - SWIFTLINE (1 day transit) from Friday 2026-11-06 -> Saturday 2026-11-07")
    void testSwiftlineEta() {
        LocalDate dispatchDate = LocalDate.of(2026, 11, 6);
        LocalDate eta = DispatchDateUtil.calculateEta(dispatchDate, 1, holidays);
        assertEquals(LocalDate.of(2026, 11, 7), eta);
    }

    @Test
    @DisplayName("T10: Section 8 ETA check - ROADKING (3 days transit) from Friday 2026-11-06 skips Sun 11-08 and Mon 11-09 holiday -> Wed 2026-11-11")
    void testRoadkingEtaWithSundayAndHoliday() {
        LocalDate dispatchDate = LocalDate.of(2026, 11, 6);
        LocalDate eta = DispatchDateUtil.calculateEta(dispatchDate, 3, holidays);
        assertEquals(LocalDate.of(2026, 11, 11), eta);
    }

    @Test
    @DisplayName("T10: After cutoff scenario (Section 8) - dispatched Saturday 11-07: EXPRESS (1 day) -> Tue 11-10; STANDARD (3 days) -> Thu 11-12")
    void testEtaAfterCutoff() {
        LocalDate saturdayDispatch = LocalDate.of(2026, 11, 7);

        LocalDate expressEta = DispatchDateUtil.calculateEta(saturdayDispatch, 1, holidays);
        assertEquals(LocalDate.of(2026, 11, 10), expressEta);

        LocalDate standardEta = DispatchDateUtil.calculateEta(saturdayDispatch, 3, holidays);
        assertEquals(LocalDate.of(2026, 11, 12), standardEta);
    }
}