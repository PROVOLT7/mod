package dev.vexvol.minigames.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {
    private static final Pattern PATTERN = Pattern.compile("^(\\d+)(s|m|h|d|w)$", Pattern.CASE_INSENSITIVE);

    private DurationParser() {}

    public static long parseMillis(String input) {
        if (input == null) return -1L;
        Matcher matcher = PATTERN.matcher(input.trim());
        if (!matcher.matches()) return -1L;
        long value;
        try {
            value = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return -1L;
        }
        String unit = matcher.group(2).toLowerCase(Locale.ROOT);
        try {
            return switch (unit) {
                case "s" -> Math.multiplyExact(value, 1_000L);
                case "m" -> Math.multiplyExact(value, 60_000L);
                case "h" -> Math.multiplyExact(value, 3_600_000L);
                case "d" -> Math.multiplyExact(value, 86_400_000L);
                case "w" -> Math.multiplyExact(value, 604_800_000L);
                default -> -1L;
            };
        } catch (ArithmeticException ignored) {
            return -1L;
        }
    }
}
