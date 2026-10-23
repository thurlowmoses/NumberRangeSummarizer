package numberrangesummarizer;

import java.util.Objects;

/**
 * Default {@link RangeFormatter}: a single number is written on its own ({@code "31"})
 * and a longer run as {@code start-end} ({@code "6-8"}).
 */
public class DefaultRangeFormatter implements RangeFormatter {

    private static final String DEFAULT_RANGE_SEPARATOR = "-";

    private final String rangeSeparator;

    /** Uses {@code "-"} between the start and end of a range. */
    public DefaultRangeFormatter() {
        this(DEFAULT_RANGE_SEPARATOR);
    }

    /**
     * @param rangeSeparator text placed between the start and end, e.g. {@code ".."}
     * @throws NullPointerException if {@code rangeSeparator} is null
     */
    public DefaultRangeFormatter(String rangeSeparator) {
        // Fail fast: a null separator would otherwise print "6null8" much later.
        this.rangeSeparator = Objects.requireNonNull(rangeSeparator, "rangeSeparator must not be null");
    }

    @Override
    public String format(int start, int end) {
        // A run of one number prints as just that number, never as "5-5".
        if (start == end) {
            return String.valueOf(start);
        }
        return start + rangeSeparator + end;
    }
}
