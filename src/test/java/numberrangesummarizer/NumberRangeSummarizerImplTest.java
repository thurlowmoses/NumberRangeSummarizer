package numberrangesummarizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * These tests are grouped the same way the solution was actually built:
 * <ol>
 *   <li>collect() on its own, since parsing is a separate concern to grouping;</li>
 *   <li>summarizeCollection() on its own, including the brief's own example and
 *       the edge cases (unsorted input, duplicates, negatives, int overflow);</li>
 *   <li>the injected {@link RangeFormatter}, to prove the formatting can be swapped
 *       out without changing the grouping logic;</li>
 *   <li>one end-to-end test that runs collect() then summarizeCollection() together,
 *       since the brief is ultimately used that way.</li>
 * </ol>
 */
class NumberRangeSummarizerImplTest {

    private NumberRangeSummarizer summarizer;

    // A fresh summarizer for every test, so no test can affect another.
    @BeforeEach
    void setUp() {
        summarizer = new NumberRangeSummarizerImpl();
    }

    // --- collect() ---

    @Test
    void collect_parsesCommaSeparatedNumbersInOrder() {
        Collection<Integer> result = summarizer.collect("1,3,6,7,8");

        assertEquals(Arrays.asList(1, 3, 6, 7, 8), result);
    }

    @Test
    void collect_trimsWhitespaceAroundValues() {
        assertEquals(Arrays.asList(1, 2, 3), summarizer.collect("  1 ,2,   3 "));
    }

    // Null or blank input just means there's nothing to read, so it shouldn't blow up.
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void collect_returnsEmptyCollectionForNullOrBlankInput(String input) {
        assertTrue(summarizer.collect(input).isEmpty());
    }

    // A stray comma like in "1,,2," is probably just a typo, so skip it.
    @Test
    void collect_ignoresEmptyEntries() {
        assertEquals(Arrays.asList(1, 2), summarizer.collect("1,,2,"));
    }

    // collect() only reads the numbers. Sorting and removing duplicates happens later.
    @Test
    void collect_keepsDuplicatesAndOrder() {
        assertEquals(Arrays.asList(5, 1, 5), summarizer.collect("5,1,5"));
    }

    @Test
    void collect_handlesNegativeNumbers() {
        assertEquals(Arrays.asList(-3, -2, 4), summarizer.collect("-3,-2,4"));
    }

    // Anything that isn't a whole int should be rejected: letters, decimals,
    // the wrong separator, and a number one bigger than Integer.MAX_VALUE.
    @ParameterizedTest
    @ValueSource(strings = {"1,abc,3", "1.5", "1;2", "2147483648"})
    void collect_throwsForValuesThatAreNotValidIntegers(String input) {
        assertThrows(IllegalArgumentException.class, () -> summarizer.collect(input));
    }

    @Test
    void collect_errorMessageNamesTheBadValue() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> summarizer.collect("1,abc,3"));

        assertTrue(e.getMessage().contains("abc"));
    }

    // --- summarizeCollection() ---

    @Test
    void summarize_matchesTheExampleFromTheBrief() {
        Collection<Integer> input = summarizer.collect("1,3,6,7,8,12,13,14,15,21,22,23,24,31");

        assertEquals("1, 3, 6-8, 12-15, 21-24, 31", summarizer.summarizeCollection(input));
    }

    // The same check run against lots of inputs. Each row shows up on its own in the report.
    @ParameterizedTest(name = "{0} -> \"{1}\"")
    @MethodSource("summarizeCases")
    void summarize_groupsConsecutiveNumbersIntoRanges(List<Integer> input, String expected) {
        assertEquals(expected, summarizer.summarizeCollection(input));
    }

    static Stream<Arguments> summarizeCases() {
        return Stream.of(
                arguments(Arrays.asList(5), "5"),                         // one number, not "5-5"
                arguments(Arrays.asList(1, 2), "1-2"),                    // two in a row is a range
                arguments(Arrays.asList(1, 2, 3, 4, 5), "1-5"),           // the whole list is one run
                arguments(Arrays.asList(2, 4, 6, 8), "2, 4, 6, 8"),       // no runs at all
                arguments(Arrays.asList(1, 2, 4, 5, 7), "1-2, 4-5, 7"),   // a mix of both
                arguments(Arrays.asList(9, 7, 8, 1), "1, 7-9"),           // out of order
                arguments(Arrays.asList(1, 1, 2, 2, 3), "1-3"),           // duplicates
                arguments(Arrays.asList(-2, -1, 0, 1), "-2-1"),           // negative numbers
                arguments(Arrays.asList(Integer.MAX_VALUE - 1, Integer.MAX_VALUE),
                        "2147483646-2147483647"),                         // the biggest ints
                arguments(Arrays.asList(Integer.MAX_VALUE, Integer.MIN_VALUE),
                        "-2147483648, 2147483647"));                      // MAX + 1 wraps to MIN, but they aren't neighbours
    }

    // No numbers means an empty summary, not a crash.
    @Test
    void summarize_returnsEmptyStringForNullInput() {
        assertEquals("", summarizer.summarizeCollection(null));
    }

    @Test
    void summarize_returnsEmptyStringForEmptyInput() {
        assertEquals("", summarizer.summarizeCollection(Collections.<Integer>emptyList()));
    }

    // Nulls inside the list are skipped, since they can't be turned into numbers.
    @Test
    void summarize_ignoresNullElements() {
        assertEquals("1-2, 5", summarizer.summarizeCollection(Arrays.asList(1, null, 2, null, 5)));
    }

    // The interface takes any Collection, and a HashSet doesn't keep things in order.
    @Test
    void summarize_acceptsAnyCollectionNotJustLists() {
        Collection<Integer> set = new HashSet<>(Arrays.asList(4, 2, 3, 9));

        assertEquals("2-4, 9", summarizer.summarizeCollection(set));
    }

    // We sort a copy, so the list that was passed in should come back untouched.
    @Test
    void summarize_doesNotModifyTheCallersCollection() {
        List<Integer> input = new ArrayList<>(Arrays.asList(3, 1, 2, 2));

        summarizer.summarizeCollection(input);

        assertEquals(Arrays.asList(3, 1, 2, 2), input);
    }

    // --- custom formatter ---

    @Test
    void constructor_rejectsANullFormatter() {
        assertThrows(NullPointerException.class, () -> new NumberRangeSummarizerImpl(null));
    }

    @Test
    void summarize_usesTheInjectedFormatter() {
        // RangeFormatter only has one method, so a lambda works fine here.
        RangeFormatter brackets = (start, end) -> "[" + start + ".." + end + "]";
        NumberRangeSummarizer custom = new NumberRangeSummarizerImpl(brackets);

        assertEquals("[1..3], [7..7]", custom.summarizeCollection(Arrays.asList(1, 2, 3, 7)));
    }

    // --- both methods together ---

    @Test
    void collectThenSummarize_worksTogether() {
        String result = summarizer.summarizeCollection(summarizer.collect(" 10, 8 ,9, 20,3,4,5 "));

        assertEquals("3-5, 8-10, 20", result);
    }
}
