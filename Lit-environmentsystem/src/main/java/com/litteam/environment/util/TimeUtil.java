package com.litteam.environment.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class TimeUtil {

    public static final long DAY_TICKS = 24000L;

    // "H:mm" so both 6:30 and 06:30 are accepted when someone types a time
    private static final DateTimeFormatter INPUT = DateTimeFormatter.ofPattern("H:mm");
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter LONG = DateTimeFormatter.ofPattern("HH:mm:ss");

    private TimeUtil() {
    }

    public static LocalTime parse(String value) throws DateTimeParseException {
        return LocalTime.parse(value.trim(), INPUT);
    }

    public static String format(LocalTime time) {
        return time.format(SHORT);
    }

    public static String formatSeconds(LocalTime time) {
        return time.format(LONG);
    }

    public static String dayOrNight(long ticks) {
        return Math.floorMod(ticks, DAY_TICKS) < 12000L ? "DAY" : "NIGHT";
    }
}
