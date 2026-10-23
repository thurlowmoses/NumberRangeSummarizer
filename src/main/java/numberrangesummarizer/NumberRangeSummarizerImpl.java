package numberrangesummarizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Takes a list of numbers and squashes any runs of consecutive numbers into ranges.
 *
 * <p>For example, {@code "1,3,6,7,8,12,13,14,15,21,22,23,24,31"} becomes
 * {@code "1, 3, 6-8, 12-15, 21-24, 31"}.
 *
 * <p>The brief didn't say what to do in a few situations, so I made these choices
 * (there's a test for each one):
 * <ul>
 *   <li>The numbers can come in any order. I sort them first.</li>
 *   <li>Duplicates are ignored, so {@code 1,1,2} gives {@code "1-2"}.</li>
 *   <li>Two numbers in a row already count as a range, so {@code 1,2} gives {@code "1-2"}.</li>
 *   <li>Null or blank input just gives an empty result instead of crashing.</li>
 *   <li>Extra spaces and empty entries like {@code "1,,2"} are skipped.</li>
 *   <li>Anything that isn't a whole number throws an error that says which value was wrong.</li>
 *   <li>The collection you pass in is never changed.</li>
 * </ul>
 *
 * <p>This class handles the logic (parse, sort, group, join). How each range is written
 * out is handed off to a {@link RangeFormatter}, so the output format can be changed
 * without touching this class. It doesn't keep any state between calls, so it's safe
 * to share one instance.
 *
 * <p>How this came together, roughly in order:
 * <ol>
 *   <li>Started from the brief's own example and wrote that as a failing test first,
 *       against a stub that just threw. Once that failed for the right reason (not a
 *       typo), there was something real to build towards.</li>
 *   <li>{@code collect()} is the easy half: split, trim, drop blanks, parse. It's a
 *       straight pipeline, which is why it reads well as a stream.</li>
 *   <li>{@code summarizeCollection()} is the actual problem: you can't tell what's
 *       "consecutive" in an unordered bag of numbers, so sorting (and dropping
 *       duplicates) comes first. After that it's one pass with two pointers:
 *       where the current run started, and the last number seen.</li>
 *   <li>Once the grouping worked, the only opinion left in the method was how a
 *       range gets printed. That's a different concern to grouping, and the brief
 *       could change it without changing the algorithm — so it was pulled out into
 *       {@link RangeFormatter} and injected rather than hard-coded.</li>
 * </ol>
 */
public class NumberRangeSummarizerImpl implements NumberRangeSummarizer {

    private static final String INPUT_SEPARATOR = ",";
    private static final String OUTPUT_SEPARATOR = ", ";

    private final RangeFormatter formatter;

    /** Uses the normal format, e.g. {@code "6-8"}. */
    public NumberRangeSummarizerImpl() {
        this(new DefaultRangeFormatter());
    }

    /**
     * Lets the caller choose how ranges are written.
     *
     * @param formatter writes out each range
     * @throws NullPointerException if {@code formatter} is null
     */
    public NumberRangeSummarizerImpl(RangeFormatter formatter) {
        this.formatter = Objects.requireNonNull(formatter, "formatter must not be null");
    }

    /**
     * Reads a comma separated string and turns it into a list of numbers.
     * The order and any duplicates are kept as they are.
     *
     * @param input something like {@code "1, 3,6"}; can be null or blank
     * @return the numbers, or an empty list if there was nothing to read
     * @throws IllegalArgumentException if one of the values isn't a valid {@code int}
     */
    @Override
    public Collection<Integer> collect(String input) {
        // Nothing to read, so hand back an empty list rather than null.
        if (input == null || input.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(input.split(INPUT_SEPARATOR))
                .map(String::trim)                              // " 3 " becomes "3"
                .filter(token -> !token.isEmpty())              // skip the gap in "1,,2"
                .map(NumberRangeSummarizerImpl::parseInteger)   // "3" becomes 3
                .collect(Collectors.toList());
    }

    /**
     * Builds the summary string, e.g. {@code "1, 3, 6-8"}.
     *
     * @param input the numbers in any order; can be null or contain nulls
     * @return the summary, or an empty string if there were no numbers
     */
    @Override
    public String summarizeCollection(Collection<Integer> input) {
        List<Integer> numbers = sortedUniqueNumbers(input);
        if (numbers.isEmpty()) {
            return "";
        }

        // rangeStart is where the run we're currently building began.
        // previous is the last number we looked at, so we can tell if the next
        // one carries the run on or breaks it.
        List<String> parts = new ArrayList<>();
        int rangeStart = numbers.get(0);
        int previous = rangeStart;

        for (int i = 1; i < numbers.size(); i++) {
            int current = numbers.get(i);
            if (!isNextNumber(previous, current)) {
                // The run has been broken, so save it and start a new one here.
                parts.add(formatter.format(rangeStart, previous));
                rangeStart = current;
            }
            previous = current;
        }

        // The loop only closes a run when it sees a break, and there's no break
        // after the last number - the loop just ends. So the final run has to be
        // closed off here, or it would silently go missing from the output.
        parts.add(formatter.format(rangeStart, previous));

        return String.join(OUTPUT_SEPARATOR, parts);
    }

    /** Drops nulls and duplicates and sorts what's left, without touching the original. */
    private static List<Integer> sortedUniqueNumbers(Collection<Integer> input) {
        if (input == null) {
            return Collections.emptyList();
        }
        return input.stream()
                .filter(Objects::nonNull)   // a null can't be turned into an int
                .distinct()                 // treat 1,1,2 the same as 1,2
                .sorted()                   // neighbours need to sit next to each other
                .collect(Collectors.toList());
    }

    /**
     * Checks if {@code current} comes straight after {@code previous}.
     *
     * <p>The subtraction is done as a {@code long}, not an {@code int}. If
     * {@code previous} is {@code Integer.MAX_VALUE} and {@code current} is
     * {@code Integer.MIN_VALUE}, int arithmetic would wrap the result around
     * to {@code 1} and wrongly call them consecutive. Doing the maths as a
     * {@code long} first avoids that wraparound.
     */
    private static boolean isNextNumber(int previous, int current) {
        return (long) current - previous == 1;
    }

    private static Integer parseInteger(String token) {
        try {
            return Integer.valueOf(token);
        } catch (NumberFormatException e) {
            // Say which value was bad, and keep the original error attached.
            throw new IllegalArgumentException("Not a valid integer: '" + token + "'", e);
        }
    }
}
