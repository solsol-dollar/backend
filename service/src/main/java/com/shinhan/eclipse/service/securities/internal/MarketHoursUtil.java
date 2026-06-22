package com.shinhan.eclipse.service.securities.internal;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

final class MarketHoursUtil {

    private static final ZoneId    ET_ZONE    = ZoneId.of("America/New_York");
    private static final LocalTime OPEN_TIME  = LocalTime.of(9, 30);
    private static final LocalTime CLOSE_TIME = LocalTime.of(16, 0);

    private MarketHoursUtil() {}

    static boolean isUsMarketOpen() {
        ZonedDateTime now = ZonedDateTime.now(ET_ZONE);
        DayOfWeek day = now.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) return false;
        LocalTime t = now.toLocalTime();
        return !t.isBefore(OPEN_TIME) && t.isBefore(CLOSE_TIME);
    }
}
