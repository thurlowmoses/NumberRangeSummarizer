package numberrangesummarizer;

import java.util.Objects;

/**
 * The standard formatter. A single number is written on its own ({@code "31"}),
 * and a run is written as start, dash, end ({@code "6-8"}).
 */
public class DefaultRangeFormatter implements RangeFormatter {

    private static final String DEFAULT_RANGE_SEPARATOR = "-";

    private final String rangeSeparator;

    /** Puts a dash between the start and end, e.g. {@code "6-8"}. */
    public DefaultRangeFormatter() {
        this(DEFAULT_RANGE_SEPARATOR);
    }

    /**
     * @param rangeSeparator what goes between the start and end, e.g. {@code ".."}
     * @throws NullPointerException if {@code rangeSeparator} is null
     */
    public DefaultRangeFormatter(String rangeSeparator) {
        // Catch a null here, otherwise it would quietly print "6null8" later on.
        this.rangeSeparator = Objects.requireNonNull(rangeSeparator, "rangeSeparator must not be null");
    }

    @Override
    public String format(int start, int end) {
        // Just one number, so write "5" rather than "5-5".
        if (start == end) {
            return String.valueOf(start);
        }
        return start + rangeSeparator + end;
    }
}
