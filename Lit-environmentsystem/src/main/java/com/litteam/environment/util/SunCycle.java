package com.litteam.environment.util;

import java.time.LocalTime;

/**
 * Maps the real-world clock onto Minecraft's 24000 tick day.
 *
 * Sunrise is tick 0 and sunset is tick 12000, so the daylight hours are
 * stretched or squeezed to fit whatever sunrise and sunset times are
 * configured. With 06:00 and 18:00 it works out to 06:00 = tick 0,
 * 12:00 = tick 6000, 18:00 = tick 12000 and 00:00 = tick 18000.
 */
public record SunCycle(LocalTime sunrise, LocalTime sunset) {

    public static final SunCycle DEFAULT = new SunCycle(LocalTime.of(6, 0), LocalTime.of(18, 0));

    private static final double DAY_SECONDS = 86400.0;
    private static final double HALF_DAY_TICKS = 12000.0;
    private static final double FULL_DAY_TICKS = 24000.0;

    public SunCycle {
        if (!sunset.isAfter(sunrise)) {
            throw new IllegalArgumentException("sunset must be after sunrise");
        }
    }

    /** Fractional tick for a wall clock time, so the sky can follow it smoothly. */
    public double ticksAt(LocalTime time) {
        double now = time.toNanoOfDay() / 1_000_000_000.0;
        double dayLength = dayLength();
        double sinceSunrise = wrap(now - sunrise.toSecondOfDay(), DAY_SECONDS);

        if (sinceSunrise < dayLength) {
            return sinceSunrise / dayLength * HALF_DAY_TICKS;
        }
        double nightLength = DAY_SECONDS - dayLength;
        return HALF_DAY_TICKS + (sinceSunrise - dayLength) / nightLength * HALF_DAY_TICKS;
    }

    /** The wall clock time a given world tick corresponds to. */
    public LocalTime clockAt(long ticks) {
        double tick = wrap(ticks, FULL_DAY_TICKS);
        double dayLength = dayLength();
        double nightLength = DAY_SECONDS - dayLength;

        double sinceSunrise;
        if (tick < HALF_DAY_TICKS) {
            sinceSunrise = tick / HALF_DAY_TICKS * dayLength;
        } else {
            sinceSunrise = dayLength + (tick - HALF_DAY_TICKS) / HALF_DAY_TICKS * nightLength;
        }

        long second = Math.round(wrap(sunrise.toSecondOfDay() + sinceSunrise, DAY_SECONDS)) % 86400L;
        return LocalTime.ofSecondOfDay(second);
    }

    private double dayLength() {
        return sunset.toSecondOfDay() - sunrise.toSecondOfDay();
    }

    private static double wrap(double value, double range) {
        double result = value % range;
        return result < 0 ? result + range : result;
    }
}
