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

class NumberRangeSummarizerImplTest {

    private NumberRangeSummarizer summarizer;

    // Runs before every test, so each test gets a fresh object and tests cannot affect each other.
    @BeforeEach
    void setUp() {
        summarizer = new NumberRangeSummarizerImpl();
    }

    // ------------------------------------------------------------------ collect

    @Test
    void collect_parsesCommaSeparatedNumbersInOrder() {
        Collection<Integer> result = summarizer.collect("1,3,6,7,8");

        assertEquals(Arrays.asList(1, 3, 6, 7, 8), result);
    }

    @Test
    void collect_trimsWhitespaceAroundValues() {
        assertEquals(Arrays.asList(1, 2, 3), summarizer.collect("  1 ,2,   3 "));
    }

    // Assumption: null or blank input is "nothing to parse", not an error.
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void collect_returnsEmptyCollectionForNullOrBlankInput(String input) {
        assertTrue(summarizer.collect(input).isEmpty());
    }

    // Assumption: empty entries such as "1,,2," are harmless formatting noise and are skipped.
    @Test
    void collect_ignoresEmptyEntries() {
        assertEquals(Arrays.asList(1, 2), summarizer.collect("1,,2,"));
    }

    // collect only parses; sorting and removing duplicates is summarizeCollection's job.
    @Test
    void collect_keepsDuplicatesAndOrder() {
        assertEquals(Arrays.asList(5, 1, 5), summarizer.collect("5,1,5"));
    }

    @Test
    void collect_handlesNegativeNumbers() {
        assertEquals(Arrays.asList(-3, -2, 4), summarizer.collect("-3,-2,4"));
    }

    // Assumption: anything that is not a whole int is rejected (letters, decimals,
    // a different separator, and a value just above Integer.MAX_VALUE).
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

    // ------------------------------------------------------------ summarize

    @Test
    void summarize_matchesTheExampleFromTheBrief() {
        Collection<Integer> input = summarizer.collect("1,3,6,7,8,12,13,14,15,21,22,23,24,31");

        assertEquals("1, 3, 6-8, 12-15, 21-24, 31", summarizer.summarizeCollection(input));
    }

    // One assertion, many rows: like [Theory] + [MemberData] in xUnit.
    // Each row's input and expected result show up in the test report.
    @ParameterizedTest(name = "{0} -> \"{1}\"")
    @MethodSource("summarizeCases")
    void summarize_groupsConsecutiveNumbersIntoRanges(List<Integer> input, String expected) {
        assertEquals(expected, summarizer.summarizeCollection(input));
    }

    static Stream<Arguments> summarizeCases() {
        return Stream.of(
                arguments(Arrays.asList(5), "5"),                         // single number, not "5-5"
                arguments(Arrays.asList(1, 2), "1-2"),                    // assumption: two numbers form a range
                arguments(Arrays.asList(1, 2, 3, 4, 5), "1-5"),           // all consecutive: last run is added
                arguments(Arrays.asList(2, 4, 6, 8), "2, 4, 6, 8"),       // none consecutive
                arguments(Arrays.asList(1, 2, 4, 5, 7), "1-2, 4-5, 7"),   // mixed
                arguments(Arrays.asList(9, 7, 8, 1), "1, 7-9"),           // assumption: input may be unsorted
                arguments(Arrays.asList(1, 1, 2, 2, 3), "1-3"),           // assumption: duplicates are ignored
                arguments(Arrays.asList(-2, -1, 0, 1), "-2-1"),           // negatives
                arguments(Arrays.asList(Integer.MAX_VALUE - 1, Integer.MAX_VALUE),
                        "2147483646-2147483647"),                         // top of the int range
                arguments(Arrays.asList(Integer.MAX_VALUE, Integer.MIN_VALUE),
                        "-2147483648, 2147483647"));                      // no overflow: MAX+1 is not MIN
    }

    // Assumption: null or empty input is "nothing to summarize", not an error.
    @Test
    void summarize_returnsEmptyStringForNullInput() {
        assertEquals("", summarizer.summarizeCollection(null));
    }

    @Test
    void summarize_returnsEmptyStringForEmptyInput() {
        assertEquals("", summarizer.summarizeCollection(Collections.<Integer>emptyList()));
    }

    // Assumption: null elements are skipped (a null cannot be turned into an int).
    @Test
    void summarize_ignoresNullElements() {
        assertEquals("1-2, 5", summarizer.summarizeCollection(Arrays.asList(1, null, 2, null, 5)));
    }

    // The interface accepts any Collection, and a HashSet has no guaranteed order.
    @Test
    void summarize_acceptsAnyCollectionNotJustLists() {
        Collection<Integer> set = new HashSet<>(Arrays.asList(4, 2, 3, 9));

        assertEquals("2-4, 9", summarizer.summarizeCollection(set));
    }

    // Assumption: the caller's collection is never changed (we sort a copy, not the original).
    @Test
    void summarize_doesNotModifyTheCallersCollection() {
        List<Integer> input = new ArrayList<>(Arrays.asList(3, 1, 2, 2));

        summarizer.summarizeCollection(input);

        assertEquals(Arrays.asList(3, 1, 2, 2), input);
    }

    // ---------------------------------------------------------- design (OOP)

    @Test
    void constructor_rejectsANullFormatter() {
        assertThrows(NullPointerException.class, () -> new NumberRangeSummarizerImpl(null));
    }

    @Test
    void summarize_usesTheInjectedFormatter() {
        // A lambda is enough here because RangeFormatter has a single abstract method.
        RangeFormatter brackets = (start, end) -> "[" + start + ".." + end + "]";
        NumberRangeSummarizer custom = new NumberRangeSummarizerImpl(brackets);

        assertEquals("[1..3], [7..7]", custom.summarizeCollection(Arrays.asList(1, 2, 3, 7)));
    }

    // ----------------------------------------------------------- end to end

    @Test
    void collectThenSummarize_worksTogether() {
        String result = summarizer.summarizeCollection(summarizer.collect(" 10, 8 ,9, 20,3,4,5 "));

        assertEquals("3-5, 8-10, 20", result);
    }
}
