package numberrangesummarizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Turns a comma separated string of integers into a summary where runs of
 * consecutive numbers are written as ranges.
 *
 * <p>Example: {@code "1,3,6,7,8,12,13,14,15,21,22,23,24,31"} is summarized as
 * {@code "1, 3, 6-8, 12-15, 21-24, 31"}.
 *
 * <p>Assumptions (each one is covered by a unit test):
 * <ul>
 *   <li>Input does not need to be sorted; numbers are sorted before grouping.</li>
 *   <li>Duplicates are ignored: {@code 1,1,2} gives {@code "1-2"}.</li>
 *   <li>Two consecutive numbers already form a range: {@code 1,2} gives {@code "1-2"}.</li>
 *   <li>Null or blank input gives an empty result, never an exception.</li>
 *   <li>Whitespace around numbers and empty entries ({@code "1,,2"}) are ignored.</li>
 *   <li>A value that is not a whole {@code int} is rejected with an exception naming it.</li>
 *   <li>The caller's collection is never modified.</li>
 * </ul>
 *
 * <p>Design: this class owns the algorithm (parse, sort, group, join). How each run is
 * written is delegated to an injected {@link RangeFormatter} (composition over
 * inheritance), so the output format can change without editing this class.
 * The class has no mutable state, so one instance can safely be shared between threads.
 */
public class NumberRangeSummarizerImpl implements NumberRangeSummarizer {

    private static final String INPUT_SEPARATOR = ",";
    private static final String OUTPUT_SEPARATOR = ", ";

    private final RangeFormatter formatter;

    /** Creates a summarizer that writes ranges in the default {@code "6-8"} format. */
    public NumberRangeSummarizerImpl() {
        this(new DefaultRangeFormatter());
    }

    /**
     * Creates a summarizer with a custom output format (constructor injection).
     *
     * @param formatter decides how each run of numbers is written
     * @throws NullPointerException if {@code formatter} is null
     */
    public NumberRangeSummarizerImpl(RangeFormatter formatter) {
        this.formatter = Objects.requireNonNull(formatter, "formatter must not be null");
    }

    /**
     * Parses a comma separated string into integers, keeping their order and duplicates.
     *
     * @param input e.g. {@code "1, 3,6"}; may be null or blank
     * @return the parsed numbers, or an empty collection for null or blank input
     * @throws IllegalArgumentException if a value is not a valid {@code int}
     */
    @Override
    public Collection<Integer> collect(String input) {
        // Return an empty list rather than null, so callers never need a null check.
        if (input == null || input.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(input.split(INPUT_SEPARATOR))
                .map(String::trim)                              // " 3 " -> "3"
                .filter(token -> !token.isEmpty())              // "1,,2" -> skip the empty entry
                .map(NumberRangeSummarizerImpl::parseInteger)   // "3" -> 3, or a clear error
                .collect(Collectors.toList());
    }

    /**
     * Summarizes the numbers as a comma delimited string, grouping consecutive
     * numbers into ranges such as {@code "6-8"}.
     *
     * @param input the numbers to summarize, in any order; may be null or contain nulls
     * @return e.g. {@code "1, 3, 6-8"}, or an empty string if there is nothing to summarize
     */
    @Override
    public String summarizeCollection(Collection<Integer> input) {
        List<Integer> numbers = sortedUniqueNumbers(input);
        if (numbers.isEmpty()) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        int rangeStart = numbers.get(0);
        int previous = rangeStart;

        for (int i = 1; i < numbers.size(); i++) {
            int current = numbers.get(i);
            if (!isNextNumber(previous, current)) {
                // Gap found: the run that started at rangeStart is finished.
                parts.add(formatter.format(rangeStart, previous));
                rangeStart = current;
            }
            previous = current;
        }

        // The loop only closes a run when it meets a gap, so the last run must be added here.
        parts.add(formatter.format(rangeStart, previous));

        return String.join(OUTPUT_SEPARATOR, parts);
    }

    /** Returns the non-null values, without duplicates and in ascending order, as a new list. */
    private static List<Integer> sortedUniqueNumbers(Collection<Integer> input) {
        if (input == null) {
            return Collections.emptyList();
        }
        return input.stream()
                .filter(Objects::nonNull)   // a null cannot be turned into an int
                .distinct()                 // 1,1,2 behaves like 1,2
                .sorted()                   // consecutive numbers must sit next to each other
                .collect(Collectors.toList());
    }

    /**
     * True when {@code current} is exactly one more than {@code previous}.
     * Done in {@code long} so {@code Integer.MAX_VALUE + 1} cannot overflow.
     */
    private static boolean isNextNumber(int previous, int current) {
        return (long) current - previous == 1;
    }

    private static Integer parseInteger(String token) {
        try {
            return Integer.valueOf(token);
        } catch (NumberFormatException e) {
            // Name the bad value and keep the original exception as the cause.
            throw new IllegalArgumentException("Not a valid integer: '" + token + "'", e);
        }
    }
}
