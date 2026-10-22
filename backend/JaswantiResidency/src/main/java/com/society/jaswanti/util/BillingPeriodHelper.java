package com.society.jaswanti.util;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class BillingPeriodHelper {

    private static final Pattern CANONICAL_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    private static final DateTimeFormatter CANONICAL_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM");

    private static final DateTimeFormatter FULL_MONTH_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("MMMM")
            .appendLiteral(' ')
            .appendValue(ChronoField.YEAR, 4)
            .toFormatter(Locale.ENGLISH);

    private static final DateTimeFormatter SHORT_MONTH_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("MMM")
            .appendLiteral(' ')
            .appendValue(ChronoField.YEAR, 4)
            .toFormatter(Locale.ENGLISH);

    private BillingPeriodHelper() {}

    /**
     * Validates that the provided billing period is strictly in canonical YYYY-MM format.
     * Throws IllegalArgumentException if null, blank, or non-canonical.
     */
    public static YearMonth validateCanonical(String billingMonth) {
        if (billingMonth == null || billingMonth.isBlank()) {
            throw new IllegalArgumentException("Billing month must be provided and in canonical format YYYY-MM (e.g. 2026-10)");
        }
        String trimmed = billingMonth.trim();
        if (!CANONICAL_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Billing month '" + billingMonth + "' is not in canonical format YYYY-MM (e.g. 2026-10)");
        }
        try {
            return YearMonth.parse(trimmed, CANONICAL_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Billing month '" + billingMonth + "' is invalid");
        }
    }

    /**
     * Safely attempts to parse any known format (canonical YYYY-MM, 'October 2026', 'Oct 2026').
     * Returns null if unparseable.
     */
    public static YearMonth parsePeriodSafe(String billingMonth) {
        if (billingMonth == null || billingMonth.isBlank()) {
            return null;
        }
        String trimmed = billingMonth.trim();
        try {
            if (CANONICAL_PATTERN.matcher(trimmed).matches()) {
                return YearMonth.parse(trimmed, CANONICAL_FORMATTER);
            }
        } catch (Exception ignored) {}

        try {
            return YearMonth.parse(trimmed, FULL_MONTH_FORMATTER);
        } catch (Exception ignored) {}

        try {
            return YearMonth.parse(trimmed, SHORT_MONTH_FORMATTER);
        } catch (Exception ignored) {}

        return null;
    }

    /**
     * Returns all known representations of this YearMonth (canonical '2026-10', 'October 2026', 'Oct 2026').
     */
    public static Set<String> getEquivalentRepresentations(YearMonth ym) {
        Set<String> set = new LinkedHashSet<>();
        if (ym == null) return set;
        set.add(ym.format(CANONICAL_FORMATTER));
        set.add(ym.format(FULL_MONTH_FORMATTER));
        set.add(ym.format(SHORT_MONTH_FORMATTER));
        return set;
    }
}
